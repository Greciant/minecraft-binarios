package com.devilfruits.fruits.doru_doru.atelier;

import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.fruits.doru_doru.DoruAbilities;
import com.devilfruits.fruits.doru_doru.DoruSounds;
import com.devilfruits.fruits.doru_doru.DoruTriggers;
import com.devilfruits.fruits.doru_doru.ability.WaxAtelierAbility;
import com.devilfruits.fruits.doru_doru.wax.WaxBlockEntity;
import com.devilfruits.fruits.doru_doru.wax.WaxOwners;
import com.devilfruits.fruits.doru_doru.wax.WaxPalette;
import com.devilfruits.fruits.doru_doru.wax.WaxWorld;
import com.devilfruits.registry.DFAttachments;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AtelierTools {
   static void handle(ServerPlayer player, SculptRequest r) {
      ServerLevel level = player.serverLevel();
      if (r.tool() == SculptRequest.Tool.CAST && r.param(3) >= 0.5F) {
         capture(player, r);
      } else if (BuildEngine.busy(player)) {
         BuildEngine.refuse(player, "busy");
      } else {
         double reach = WaxAtelierAbility.reach(DFAttachments.data(player).mastery()) + 1.5;

         for (Vec3 p : r.points()) {
            if (p.distanceTo(player.getEyePosition()) > reach) {
               BuildEngine.refuse(player, "too_far");
               return;
            }
         }

         AtelierGen.Result result = WaxGen.generate(r, WaxGen.context(level, CastSlots.of(player)));
         if (result == null) {
            BuildEngine.refuse(player, r.tool() == SculptRequest.Tool.CAST ? "no_cast" : "too_big");
         } else {
            switch (r.tool()) {
               case BRUSH:
                  switch (r.kind()) {
                     case 0:
                        BuildEngine.submit(player, r, result.set(), BuildEngine.Spec.atelier());
                        return;
                     case 1:
                        BuildEngine.submit(player, r, result.set(), BuildEngine.Spec.atelier().carve());
                        return;
                     case 2:
                        smooth(player, r, result.set());
                        return;
                     default:
                        dye(player, r, result.set());
                        return;
                  }
               case DISSOLVE:
                  BuildEngine.submit(player, r, result.set(), BuildEngine.Spec.atelier().carve());
                  break;
               case CAST:
                  BuildEngine.submit(player, r, result.set(), BuildEngine.Spec.atelier().colors(result.colors()));
            }
         }
      }
   }

   static void capture(ServerPlayer player, SculptRequest r) {
      if (r.points().size() >= 2) {
         ServerLevel level = player.serverLevel();
         BlockPos a = BlockPos.containing((Position)r.points().get(0));
         BlockPos b = BlockPos.containing((Position)r.points().get(1));
         int x0 = Math.min(a.getX(), b.getX());
         int y0 = Math.min(a.getY(), b.getY());
         int z0 = Math.min(a.getZ(), b.getZ());
         int sx = Math.abs(a.getX() - b.getX()) + 1;
         int sy = Math.abs(a.getY() - b.getY()) + 1;
         int sz = Math.abs(a.getZ() - b.getZ()) + 1;
         if (sx <= 32 && sy <= 32 && sz <= 32) {
            double reach = WaxAtelierAbility.reach(DFAttachments.data(player).mastery()) + 1.5;
            if (!(r.points().get(0).distanceTo(player.getEyePosition()) > reach) && !(r.points().get(1).distanceTo(player.getEyePosition()) > reach)) {
               byte[] cells = new byte[sx * sy * sz];
               LongArrayList masks = new LongArrayList();
               int solid = 0;
               MutableBlockPos m = new MutableBlockPos();

               for (int y = 0; y < sy; y++) {
                  for (int z = 0; z < sz; z++) {
                     for (int x = 0; x < sx; x++) {
                        m.set(x0 + x, y0 + y, z0 + z);
                        if (level.isLoaded(m)) {
                           int i = x + sx * (z + sz * y);
                           BlockEntity var22 = level.getBlockEntity(m);
                           if (var22 instanceof WaxBlockEntity) {
                              WaxBlockEntity be = (WaxBlockEntity)var22;
                              if (be.mask() == -1L) {
                                 cells[i] = (byte)(1 + be.color());
                              } else {
                                 cells[i] = (byte)(17 + be.color());
                                 masks.add(be.mask());
                              }

                              solid++;
                           } else {
                              BlockState state = level.getBlockState(m);
                              if (!state.isAir() && !state.getCollisionShape(level, m).isEmpty()) {
                                 cells[i] = (byte)(1 + WaxPalette.nearest(state.getMapColor(level, m).col));
                                 solid++;
                              }
                           }
                        }
                     }
                  }
               }

               if (solid == 0) {
                  BuildEngine.refuse(player, "cast_empty");
               } else {
                  int slot = Math.floorMod(r.kind(), 3);
                  CastSlots.of(player).set(slot, CastData.pack(sx, sy, sz, cells, masks.toLongArray()));
                  player.syncData(CastSlots.TYPE);
                  player.displayClientMessage(
                     Component.translatable("message.devilfruits.doru_doru.sculpt.cast_saved", new Object[]{slot + 1, sx, sy, sz, solid})
                        .withStyle(ChatFormatting.YELLOW),
                     true
                  );
                  level.playSound(null, player.getX(), player.getY(), player.getZ(), DoruSounds.SET.get(), SoundSource.PLAYERS, 0.7F, 1.4F);
                  DoruTriggers.CAST_MOLD.get().trigger(player);
               }
            } else {
               BuildEngine.refuse(player, "too_far");
            }
         } else {
            BuildEngine.refuse(player, "cast_too_big", 32);
         }
      }
   }

   static void smooth(ServerPlayer player, SculptRequest r, VoxelSet region) {
      ServerLevel level = player.serverLevel();
      int owner = WaxOwners.get(level.getServer()).index(player.getUUID());
      AtelierGen.Context ctx = WaxGen.context(level, null);
      Long2LongOpenHashMap before = new Long2LongOpenHashMap();
      Long2IntOpenHashMap beforeColor = new Long2IntOpenHashMap();
      Long2LongOpenHashMap after = new Long2LongOpenHashMap();
      MutableBlockPos m = new MutableBlockPos();
      ObjectIterator afterColor = region.blocks().long2LongEntrySet().iterator();

      while (afterColor.hasNext()) {
         Entry e = (Entry)afterColor.next();
         long key = e.getLongKey();
         m.set(key);
         BlockEntity mask = level.getBlockEntity(m);
         if (mask instanceof WaxBlockEntity) {
            WaxBlockEntity be = (WaxBlockEntity)mask;
            if (be.owner() == owner && level.mayInteract(player, m)) {
               long maskx = be.mask();
               long out = maskx;
               int bx = m.getX() * 4;
               int by = m.getY() * 4;
               int bz = m.getZ() * 4;
               long area = e.getLongValue();

               while (area != 0L) {
                  int c = Long.numberOfTrailingZeros(area);
                  area &= area - 1L;
                  int cx = bx + (c & 3);
                  int cy = by + (c >> 4);
                  int cz = bz + (c >> 2 & 3);
                  int n = 0;

                  for (int dy = -1; dy <= 1; dy++) {
                     for (int dz = -1; dz <= 1; dz++) {
                        for (int dx = -1; dx <= 1; dx++) {
                           if (ctx.solid(cx + dx, cy + dy, cz + dz)) {
                              n++;
                           }
                        }
                     }
                  }

                  out = n >= 14 ? out | 1L << c : out & ~(1L << c);
               }

               if (out != maskx) {
                  before.put(key, maskx);
                  beforeColor.put(key, be.color());
                  after.put(key, out);
               }
            }
         }
      }

      if (after.isEmpty()) {
         BuildEngine.refuse(player, "nothing_to_smooth");
      } else {
         Long2IntOpenHashMap afterColorx = new Long2IntOpenHashMap();
         ObjectIterator var33 = after.long2LongEntrySet().iterator();

         while (var33.hasNext()) {
            Entry e = (Entry)var33.next();
            m.set(e.getLongKey());
            if (level.getBlockEntity(m) instanceof WaxBlockEntity be) {
               afterColorx.put(e.getLongKey(), be.color());
               WaxWorld.setMask(level, m.immutable(), be, e.getLongValue());
            }
         }

         UndoHistory.push(
            player.getUUID(),
            new UndoHistory.Change(level.dimension(), before, beforeColor, after, afterColorx, 0.0F),
            (int)DoruAbilities.ATELIER.config().extra("undoDepth")
         );
         level.playSound(null, player.getX(), player.getY(), player.getZ(), DoruSounds.SET.get(), SoundSource.PLAYERS, 0.6F, 1.25F);
         sendStroke(player, r);
      }
   }

   static void dye(ServerPlayer player, SculptRequest r, VoxelSet region) {
      ServerLevel level = player.serverLevel();
      int owner = WaxOwners.get(level.getServer()).index(player.getUUID());
      int color = Math.floorMod(r.color(), 16);
      Long2LongOpenHashMap masks = new Long2LongOpenHashMap();
      Long2IntOpenHashMap beforeColor = new Long2IntOpenHashMap();
      Long2IntOpenHashMap afterColor = new Long2IntOpenHashMap();
      MutableBlockPos m = new MutableBlockPos();
      LongIterator var10 = region.blocks().keySet().iterator();

      while (var10.hasNext()) {
         long key = (Long)var10.next();
         m.set(key);
         BlockEntity var14 = level.getBlockEntity(m);
         if (var14 instanceof WaxBlockEntity) {
            WaxBlockEntity be = (WaxBlockEntity)var14;
            if (be.owner() == owner && be.color() != color && level.mayInteract(player, m)) {
               masks.put(key, be.mask());
               beforeColor.put(key, be.color());
               afterColor.put(key, color);
               be.setColor(color);
               WaxWorld.sync(level, m.immutable());
            }
         }
      }

      if (masks.isEmpty()) {
         BuildEngine.refuse(player, "nothing_to_dye");
      } else {
         UndoHistory.push(
            player.getUUID(),
            new UndoHistory.Change(level.dimension(), masks, beforeColor, new Long2LongOpenHashMap(masks), afterColor, 0.0F),
            (int)DoruAbilities.ATELIER.config().extra("undoDepth")
         );
         level.playSound(null, player.getX(), player.getY(), player.getZ(), DoruSounds.COAT.get(), SoundSource.PLAYERS, 0.6F, 1.3F);
         sendStroke(player, r);
      }
   }

   private static void sendStroke(ServerPlayer player, SculptRequest r) {
      long[] path = new long[Math.min(96, r.points().size())];

      for (int i = 0; i < path.length; i++) {
         Vec3 p = r.points().get(i);
         path[i] = BlockPos.asLong((int)Math.floor(p.x * 4.0), (int)Math.floor(p.y * 4.0), (int)Math.floor(p.z * 4.0));
      }

      if (path.length != 0) {
         PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            player,
            new DoruNetwork.PourPayload(
               player.getId(),
               BuildEngine.hands(player),
               Math.max(1, path.length / 8),
               player.level().getGameTime(),
               path.length,
               r.color(),
               path,
               false,
               false
            ),
            new CustomPacketPayload[0]
         );
      }
   }

   static void undo(ServerPlayer player) {
      if (BuildEngine.busy(player)) {
         BuildEngine.refuse(player, "busy");
      } else {
         UndoHistory.Change change = UndoHistory.popUndo(player.getUUID());
         if (change != null && change.level() == player.level().dimension()) {
            int n = apply(player, change.after(), change.before(), change.beforeColor());
            PlayerFruitData data = DFAttachments.data(player);
            if (!AbilityManager.freeEnergy(player)) {
               data.setEnergy(Math.min((float)Mastery.maxEnergy(data), data.energy() + change.energy()));
            }

            UndoHistory.pushRedo(player.getUUID(), change);
            feedback(player, "undone", n);
         } else {
            if (change != null) {
               UndoHistory.pushRedo(player.getUUID(), change);
            }

            BuildEngine.refuse(player, "nothing_to_undo");
         }
      }
   }

   static void redo(ServerPlayer player) {
      if (BuildEngine.busy(player)) {
         BuildEngine.refuse(player, "busy");
      } else {
         UndoHistory.Change change = UndoHistory.popRedo(player.getUUID());
         if (change != null && change.level() == player.level().dimension()) {
            PlayerFruitData data = DFAttachments.data(player);
            boolean free = AbilityManager.freeEnergy(player);
            if (!free && data.energy() < change.energy()) {
               UndoHistory.pushRedo(player.getUUID(), change);
               BuildEngine.refuse(player, "no_energy", String.format("%.1f", change.energy()));
            } else {
               if (!free) {
                  data.setEnergy(data.energy() - change.energy());
               }

               int n = apply(player, change.before(), change.after(), change.afterColor());
               UndoHistory.pushUndoKeepRedo(player.getUUID(), change, (int)DoruAbilities.ATELIER.config().extra("undoDepth"));
               feedback(player, "redone", n);
            }
         } else {
            BuildEngine.refuse(player, "nothing_to_redo");
         }
      }
   }

   private static int apply(ServerPlayer player, Long2LongMap expect, Long2LongMap target, Long2IntMap colors) {
      ServerLevel level = player.serverLevel();
      int owner = WaxOwners.get(level.getServer()).index(player.getUUID());
      int n = 0;
      MutableBlockPos m = new MutableBlockPos();
      ObjectIterator var8 = target.long2LongEntrySet().iterator();

      while (var8.hasNext()) {
         Entry e = (Entry)var8.next();
         long key = e.getLongKey();
         m.set(key);
         if (level.isLoaded(m)) {
            WaxBlockEntity be = level.getBlockEntity(m) instanceof WaxBlockEntity w ? w : null;
            long now = be == null ? 0L : be.mask();
            if (now == expect.get(key) && (be == null || be.owner() == owner)) {
               long want = e.getLongValue();
               int color = colors.getOrDefault(key, -1);
               if (want == 0L) {
                  if (be != null) {
                     WaxWorld.remove(level, m.immutable());
                  }
               } else if (be == null) {
                  WaxWorld.pour(level, m.immutable(), want, Math.max(0, color), owner, 0, 0L);
               } else {
                  if (color >= 0) {
                     be.setColor(color);
                  }

                  WaxWorld.setMask(level, m.immutable(), be, want);
               }

               n++;
            }
         }
      }

      return n;
   }

   private static void feedback(ServerPlayer player, String key, int blocks) {
      player.displayClientMessage(
         Component.translatable("message.devilfruits.doru_doru.sculpt." + key, new Object[]{blocks}).withStyle(ChatFormatting.YELLOW), true
      );
      player.level().playSound(null, player.getX(), player.getY(), player.getZ(), DoruSounds.DISSOLVE.get(), SoundSource.PLAYERS, 0.6F, 1.1F);
   }

   private AtelierTools() {
   }
}
