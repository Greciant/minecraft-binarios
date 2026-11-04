package com.devilfruits.fruits.doru_doru.atelier;

import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.fruits.doru_doru.DoruAbilities;
import com.devilfruits.fruits.doru_doru.DoruDoru;
import com.devilfruits.fruits.doru_doru.DoruSounds;
import com.devilfruits.fruits.doru_doru.DoruTriggers;
import com.devilfruits.fruits.doru_doru.ability.WaxAtelierAbility;
import com.devilfruits.fruits.doru_doru.wax.WaxBlockEntity;
import com.devilfruits.fruits.doru_doru.wax.WaxHeatService;
import com.devilfruits.fruits.doru_doru.wax.WaxMask;
import com.devilfruits.fruits.doru_doru.wax.WaxOwners;
import com.devilfruits.fruits.doru_doru.wax.WaxWorld;
import com.devilfruits.registry.DFAttachments;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class BuildEngine {
   public static final String ATELIER = "atelier";
   private static final List<BuildEngine.Job> JOBS = new ArrayList<>();

   public static void handle(ServerPlayer player, SculptRequest r) {
      if (r.sane() && !r.points().isEmpty()) {
         if (DFAttachments.data(player).isToggleActive(DoruAbilities.ATELIER.id())) {
            DoruAbilities.setColor(player, r.color());
            switch (r.tool()) {
               case SHAPE:
                  VoxelSet set = ShapeGen.generate(r);
                  if (set == null) {
                     refuse(player, "too_big");
                     return;
                  }

                  submit(player, r, set, BuildEngine.Spec.atelier());
                  break;
               default:
                  AtelierTools.handle(player, r);
            }
         }
      }
   }

   public static void command(ServerPlayer player, int action) {
      switch (action) {
         case 0:
            AtelierTools.undo(player);
            break;
         case 1:
            AtelierTools.redo(player);
            break;
         case 2:
            cancel(player, true);
      }
   }

   public static boolean busy(ServerPlayer player) {
      for (BuildEngine.Job j : JOBS) {
         if (j.player.equals(player.getUUID()) && j.spec.slot.equals("atelier")) {
            return true;
         }
      }

      return false;
   }

   static void refuse(ServerPlayer player, String reason, Object... args) {
      player.displayClientMessage(Component.translatable("message.devilfruits.doru_doru.sculpt." + reason, args).withStyle(ChatFormatting.GOLD), true);
      player.level().playSound(null, player.getX(), player.getY(), player.getZ(), DoruSounds.SCULPT_DENY.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
   }

   public static boolean submit(ServerPlayer player, SculptRequest r, VoxelSet set, BuildEngine.Spec spec) {
      ServerLevel level = player.serverLevel();
      PlayerFruitData data = DFAttachments.data(player);
      if (!DoruDoru.canWax(player)) {
         refuse(player, WaxHeatService.tooHot(level) ? "too_hot" : "cannot");
         return false;
      } else if (spec.slot.equals("atelier") && busy(player)) {
         refuse(player, "busy");
         return false;
      } else {
         int mastery = data.mastery();
         if (spec.reach) {
            double reach = WaxAtelierAbility.reach(mastery) + 1.5;
            Vec3 eye = player.getEyePosition();

            for (Vec3 p : r.points()) {
               if (p.distanceTo(eye) > reach) {
                  refuse(player, "too_far");
                  return false;
               }
            }
         }

         WaxOwners owners = WaxOwners.get(level.getServer());
         int owner = owners.index(player.getUUID());
         Long2LongOpenHashMap accepted = new Long2LongOpenHashMap();
         int newBlocks = 0;
         int cellsTotal = 0;
         MutableBlockPos m = new MutableBlockPos();
         ObjectIterator blocks = set.blocks().long2LongEntrySet().iterator();

         while (blocks.hasNext()) {
            Entry e = (Entry)blocks.next();
            long key = e.getLongKey();
            m.set(key);
            if (level.isLoaded(m) && !level.isOutsideBuildHeight(m) && level.mayInteract(player, m)) {
               long want = e.getLongValue();
               BlockEntity job = level.getBlockEntity(m);
               long cells;
               if (job instanceof WaxBlockEntity) {
                  WaxBlockEntity be = (WaxBlockEntity)job;
                  if (be.owner() != owner) {
                     continue;
                  }

                  cells = spec.carve ? want & be.mask() : want & ~be.mask();
               } else {
                  if (spec.carve || !WaxWorld.replaceable(level.getBlockState(m))) {
                     continue;
                  }

                  cells = want;
                  newBlocks++;
               }

               if (cells != 0L) {
                  accepted.put(key, cells);
                  cellsTotal += WaxMask.count(cells);
               }
            }
         }

         if (cellsTotal == 0) {
            refuse(player, spec.carve ? "nothing_to_carve" : "no_room");
            return false;
         } else {
            float blocksx = (float)cellsTotal / 64.0F;
            if (spec.limits && (double)blocksx > WaxAtelierAbility.jobSize(mastery) + 0.001) {
               refuse(player, "too_big_mastery", String.format("%.0f", blocksx), String.format("%.0f", WaxAtelierAbility.jobSize(mastery)));
               return false;
            } else if (!spec.carve && (double)(owners.count(player.getUUID()) + newBlocks) > DoruDoru.setting("maxWaxBlocksPerPlayer")) {
               refuse(player, "block_cap", (int)DoruDoru.setting("maxWaxBlocksPerPlayer"));
               return false;
            } else {
               LongIterator var28 = accepted.keySet().iterator();

               while (var28.hasNext()) {
                  long key = (Long)var28.next();
                  m.set(key);
                  BlockState state = level.getBlockState(m);
                  if (spec.carve) {
                     if (((BreakEvent)NeoForge.EVENT_BUS.post(new BreakEvent(level, m.immutable(), state, player))).isCanceled()) {
                        refuse(player, "protected");
                        return false;
                     }
                  } else if (!(level.getBlockEntity(m) instanceof WaxBlockEntity)) {
                     BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, m.immutable());
                     if (((EntityPlaceEvent)NeoForge.EVENT_BUS.post(new EntityPlaceEvent(snapshot, level.getBlockState(m.below()), player))).isCanceled()) {
                        refuse(player, "protected");
                        return false;
                     }
                  }
               }

               if (spec.integrity && !spec.carve) {
                  WaxIntegrity.Result result = WaxIntegrity.check(WaxViews.of(level, accepted, null), accepted.keySet(), WaxAtelierAbility.rules());
                  if (result.anyUnsupported()) {
                     refuse(player, "unsupported", result.count(WaxIntegrity.Stress.UNSUPPORTED));
                     return false;
                  }
               }

               float cost = spec.energy >= 0.0F ? spec.energy : (float)((double)blocksx * DoruDoru.blockCost(mastery));
               if (spec.carve) {
                  cost = 0.0F;
               }

               boolean free = AbilityManager.freeEnergy(player);
               if (!free && cost > data.energy() + 0.001F) {
                  refuse(player, "no_energy", String.format("%.1f", cost));
                  return false;
               } else {
                  if (!free && cost > 0.0F) {
                     data.setEnergy(data.energy() - cost);
                  }

                  Vec3 hands = spec.hands != null ? spec.hands : hands(player);
                  BuildEngine.Ordered ordered = order(accepted, hands);
                  long[] keys = ordered.keys();
                  byte[] cellsx = ordered.cells();
                  int rate;
                  if (spec.duration > 0) {
                     rate = Math.max(1, (int)Math.ceil((double)keys.length / (double)spec.duration));
                  } else {
                     int fast = WaxAtelierAbility.voxelsPerTick(mastery);
                     rate = Math.min(fast, (int)Math.ceil((double)keys.length / (double)WaxAtelierAbility.minBuildTicks()));
                     rate = Math.max(Math.max(1, rate), (int)Math.ceil((double)keys.length / (double)WaxAtelierAbility.maxBuildTicks()));
                  }

                  int color = Math.floorMod(r.color(), 16);
                  BuildEngine.Job jobx = new BuildEngine.Job(
                     player.getUUID(), level, spec, r, keys, cellsx, rate, color, owner, free ? 0.0F : cost, hands, level.getGameTime()
                  );
                  jobx.entityId = player.getId();
                  JOBS.add(jobx);
                  PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                     player,
                     new DoruNetwork.PourPayload(player.getId(), hands, rate, jobx.start, jobx.total, color, path(keys, cellsx), false, spec.carve),
                     new CustomPacketPayload[0]
                  );
                  if (spec.slot.equals("atelier")) {
                     ServerFx.animate(player, DoruDoru.id(spec.carve ? "atelier_carve" : "atelier_pour"));
                     if (!spec.carve && r.tool() == SculptRequest.Tool.SHAPE && r.shape() == ShapeKind.ARCH) {
                        DoruTriggers.FIRST_ARCH.get().trigger(player);
                     }

                     if (!spec.carve && r.symmetry() != SculptRequest.Symmetry.NONE) {
                        DoruTriggers.SYMMETRY.get().trigger(player);
                     }
                  }

                  return true;
               }
            }
         }
      }
   }

   public static Vec3 hands(ServerPlayer player) {
      Vec3 look = player.getLookAngle();
      return player.getEyePosition().add(look.x * 0.55, look.y * 0.55 - 0.45, look.z * 0.55);
   }

   static long[] path(long[] keys, byte[] cells) {
      int n = Math.min(96, keys.length);
      long[] out = new long[n];

      for (int i = 0; i < n; i++) {
         int idx = n == 1 ? 0 : (int)((long)i * (long)(keys.length - 1) / (long)(n - 1));
         long key = keys[idx];
         int c = cells[idx];
         out[i] = BlockPos.asLong(BlockPos.getX(key) * 4 + (c & 3), BlockPos.getY(key) * 4 + (c >> 4), BlockPos.getZ(key) * 4 + (c >> 2 & 3));
      }

      return out;
   }

   public static BuildEngine.Ordered order(Long2LongMap set, Vec3 from) {
      int n = 0;
      LongIterator keys = set.values().iterator();

      while (keys.hasNext()) {
         long mask = (Long)keys.next();
         n += WaxMask.count(mask);
      }

      long[] keysx = new long[n];
      byte[] cells = new byte[n];
      long[] sort = new long[n];
      int i = 0;
      ObjectIterator outKeys = set.long2LongEntrySet().iterator();

      while (outKeys.hasNext()) {
         Entry e = (Entry)outKeys.next();
         long key = e.getLongKey();
         int bx = BlockPos.getX(key);
         int by = BlockPos.getY(key);
         int bz = BlockPos.getZ(key);

         for (long mask = e.getLongValue(); mask != 0L; i++) {
            int c = Long.numberOfTrailingZeros(mask);
            mask &= mask - 1L;
            int cx = c & 3;
            int cz = c >> 2 & 3;
            int cy = c >> 4;
            double x = (double)bx + ((double)cx + 0.5) / 4.0;
            double z = (double)bz + ((double)cz + 0.5) / 4.0;
            int sy = by * 4 + cy;
            double d = Math.sqrt((x - from.x) * (x - from.x) + (z - from.z) * (z - from.z));
            long k = (long)(sy + 4096) << 16 | (long)Math.min(65535, (int)(d * 64.0));
            keysx[i] = key;
            cells[i] = (byte)c;
            sort[i] = k << 24 | (long)i;
         }
      }

      Arrays.sort(sort);
      long[] outKeysx = new long[n];
      byte[] outCells = new byte[n];

      for (int j = 0; j < n; j++) {
         int idx = (int)(sort[j] & 16777215L);
         outKeysx[j] = keysx[idx];
         outCells[j] = cells[idx];
      }

      return new BuildEngine.Ordered(outKeysx, outCells);
   }

   public static void tick(MinecraftServer server) {
      if (!JOBS.isEmpty()) {
         Iterator<BuildEngine.Job> it = JOBS.iterator();
         List<BuildEngine.Job> finished = new ArrayList<>();

         while (it.hasNext()) {
            BuildEngine.Job job = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(job.player);
            if (player != null && player.isAlive() && player.serverLevel() == job.level && !Weakness.isSuppressed(player) && !WaxHeatService.tooHot(job.level)) {
               step(job);
               if (job.done()) {
                  it.remove();
                  finished.add(job);
               }
            } else {
               it.remove();
               stop(job, player, true);
            }
         }

         for (BuildEngine.Job job : finished) {
            stop(job, server.getPlayerList().getPlayer(job.player), false);
         }
      }
   }

   private static void step(BuildEngine.Job job) {
      ServerLevel level = job.level;
      Long2LongOpenHashMap batch = new Long2LongOpenHashMap();
      int end = Math.min(job.total, job.cursor + job.rate);

      for (int i = job.cursor; i < end; i++) {
         long key = job.keys[i];
         batch.put(key, batch.get(key) | 1L << job.cells[i]);
      }

      job.cursor = end;
      MutableBlockPos m = new MutableBlockPos();
      double minX = Double.MAX_VALUE;
      double minY = minX;
      double minZ = minX;
      double maxX = -minX;
      double maxY = maxX;
      double maxZ = maxX;
      long expires = job.spec.lifetime > 0L ? level.getGameTime() + job.spec.lifetime : 0L;
      ObjectIterator key = batch.long2LongEntrySet().iterator();

      while (key.hasNext()) {
         Entry e = (Entry)key.next();
         long keyx = e.getLongKey();
         m.set(keyx);
         BlockState state = level.getBlockState(m);
         WaxBlockEntity be = level.getBlockEntity(m) instanceof WaxBlockEntity w ? w : null;
         if (!job.before.containsKey(keyx)) {
            job.before.put(keyx, be == null ? 0L : be.mask());
            job.beforeColor.put(keyx, be == null ? -1 : be.color());
         }

         if (job.spec.carve) {
            if (be != null && be.owner() == job.owner) {
               WaxWorld.setMask(level, m.immutable(), be, be.mask() & ~e.getLongValue());
            }
         } else {
            if (be != null && be.owner() != job.owner || be == null && !WaxWorld.replaceable(state)) {
               continue;
            }

            int dye = job.spec.colors == null ? job.color : job.spec.colors.getOrDefault(keyx, job.color);
            WaxWorld.pour(level, m.immutable(), e.getLongValue(), dye, job.owner, job.spec.kind, expires);
         }

         minX = Math.min(minX, (double)m.getX());
         minY = Math.min(minY, (double)m.getY());
         minZ = Math.min(minZ, (double)m.getZ());
         maxX = Math.max(maxX, (double)(m.getX() + 1));
         maxY = Math.max(maxY, (double)(m.getY() + 1));
         maxZ = Math.max(maxZ, (double)(m.getZ() + 1));
      }

      if (!job.spec.carve && minX != Double.MAX_VALUE) {
         liftOut(level, new AABB(minX, minY, minZ, maxX, maxY, maxZ));
      }

      if (job.cursor % Math.max(1, job.rate * 5) < job.rate && end > 0) {
         long keyxx = job.keys[end - 1];
         level.playSound(
            null,
            (double)BlockPos.getX(keyxx) + 0.5,
            (double)BlockPos.getY(keyxx) + 0.5,
            (double)BlockPos.getZ(keyxx) + 0.5,
            job.spec.carve ? DoruSounds.DISSOLVE.get() : DoruSounds.POUR.get(),
            SoundSource.PLAYERS,
            0.55F,
            0.85F + level.random.nextFloat() * 0.3F
         );
      }
   }

   private static void liftOut(ServerLevel level, AABB box) {
      for (Entity e : level.getEntities((Entity)null, box.inflate(0.05), x -> !x.isSpectator() && (x instanceof LivingEntity || x instanceof ItemEntity))) {
         AABB bb = e.getBoundingBox();
         if (!level.noCollision(e, bb)) {
            for (double dy = 0.25; dy <= 6.0; dy += 0.25) {
               if (level.noCollision(e, bb.move(0.0, dy, 0.0))) {
                  if (e instanceof ServerPlayer p) {
                     p.teleportTo(p.getX(), p.getY() + dy, p.getZ());
                  } else {
                     e.setPos(e.getX(), e.getY() + dy, e.getZ());
                  }
                  break;
               }
            }
         }
      }
   }

   private static void stop(BuildEngine.Job job, @Nullable ServerPlayer player, boolean cancelled) {
      if (player != null) {
         PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            player, new DoruNetwork.PourPayload(player.getId(), Vec3.ZERO, 0, 0L, 0, 0, new long[0], true, job.spec.carve), new CustomPacketPayload[0]
         );
         if (job.spec.slot.equals("atelier")) {
            ServerFx.stopAnimation(player);
         }

         float paid = job.energy;
         if (cancelled && paid > 0.0F && job.total > 0) {
            float back = paid * (1.0F - (float)job.cursor / (float)job.total);
            PlayerFruitData data = DFAttachments.data(player);
            data.setEnergy(data.energy() + back);
            paid -= back;
         }

         if (job.spec.carve && job.cursor > 0) {
            float share = (float)DoruAbilities.ATELIER.config().extra("carveRefundShare");
            float back = (float)((double)job.cursor / 64.0 * DoruDoru.blockCost(DFAttachments.data(player).mastery()) * (double)share);
            PlayerFruitData data = DFAttachments.data(player);
            data.setEnergy(Math.min((float)Mastery.maxEnergy(data), data.energy() + back));
         }

         if (!cancelled && job.total > 0) {
            long key = job.keys[job.total - 1];
            job.level
               .playSound(
                  null,
                  (double)BlockPos.getX(key) + 0.5,
                  (double)BlockPos.getY(key) + 0.5,
                  (double)BlockPos.getZ(key) + 0.5,
                  DoruSounds.SET.get(),
                  SoundSource.PLAYERS,
                  0.8F,
                  0.9F + job.level.random.nextFloat() * 0.2F
               );
         }

         if (job.spec.undoable) {
            UndoHistory.push(player.getUUID(), change(job, paid), (int)DoruAbilities.ATELIER.config().extra("undoDepth"));
         }
      }
   }

   private static UndoHistory.Change change(BuildEngine.Job job, float energy) {
      Long2LongOpenHashMap after = new Long2LongOpenHashMap();
      Long2IntOpenHashMap afterColor = new Long2IntOpenHashMap();
      MutableBlockPos m = new MutableBlockPos();
      LongIterator var5 = job.before.keySet().iterator();

      while (var5.hasNext()) {
         long key = (Long)var5.next();
         m.set(key);
         WaxBlockEntity be = job.level.getBlockEntity(m) instanceof WaxBlockEntity w ? w : null;
         after.put(key, be == null ? 0L : be.mask());
         afterColor.put(key, be == null ? -1 : be.color());
      }

      return new UndoHistory.Change(job.level.dimension(), job.before, job.beforeColor, after, afterColor, energy);
   }

   public static void cancel(ServerPlayer player, boolean atelierOnly) {
      Iterator<BuildEngine.Job> it = JOBS.iterator();

      while (it.hasNext()) {
         BuildEngine.Job job = it.next();
         if (job.player.equals(player.getUUID()) && (!atelierOnly || job.spec.slot.equals("atelier"))) {
            it.remove();
            stop(job, player, true);
         }
      }
   }

   public static int running() {
      return JOBS.size();
   }

   public static void flush(MinecraftServer server) {
      for (int i = 0; i < 100000 && !JOBS.isEmpty(); i++) {
         tick(server);
      }
   }

   public static void clear() {
      JOBS.clear();
   }

   public static LongOpenHashSet unsupported(ServerLevel level, VoxelSet set) {
      WaxIntegrity.Result result = WaxIntegrity.check(WaxViews.of(level, set.blocks(), null), set.blocks().keySet(), WaxAtelierAbility.rules());
      LongOpenHashSet out = new LongOpenHashSet();
      ObjectIterator var4 = result.stress().long2IntEntrySet().iterator();

      while (var4.hasNext()) {
         it.unimi.dsi.fastutil.longs.Long2IntMap.Entry e = (it.unimi.dsi.fastutil.longs.Long2IntMap.Entry)var4.next();
         if (e.getIntValue() == WaxIntegrity.Stress.UNSUPPORTED.ordinal()) {
            out.add(e.getLongKey());
         }
      }

      return out;
   }

   private BuildEngine() {
   }

   static final class Job {
      final UUID player;
      final ServerLevel level;
      final BuildEngine.Spec spec;
      final SculptRequest request;
      final long[] keys;
      final byte[] cells;
      final int total;
      final int rate;
      final int color;
      final int owner;
      final float energy;
      final Vec3 hands;
      final long start;
      final Long2LongOpenHashMap before = new Long2LongOpenHashMap();
      final Long2IntOpenHashMap beforeColor = new Long2IntOpenHashMap();
      int cursor;
      int entityId;

      Job(
         UUID player,
         ServerLevel level,
         BuildEngine.Spec spec,
         SculptRequest request,
         long[] keys,
         byte[] cells,
         int rate,
         int color,
         int owner,
         float energy,
         Vec3 hands,
         long start
      ) {
         this.player = player;
         this.level = level;
         this.spec = spec;
         this.request = request;
         this.keys = keys;
         this.cells = cells;
         this.total = keys.length;
         this.rate = rate;
         this.color = color;
         this.owner = owner;
         this.energy = energy;
         this.hands = hands;
         this.start = start;
      }

      boolean done() {
         return this.cursor >= this.total;
      }
   }

   public static record Ordered(long[] keys, byte[] cells) {
   }

   public static final class Spec {
      String slot = "atelier";
      int kind = 0;
      long lifetime;
      int duration;
      float energy = -1.0F;
      boolean integrity = true;
      boolean undoable = true;
      boolean reach = true;
      boolean limits = true;
      boolean carve;
      @Nullable
      Vec3 hands;
      @Nullable
      Long2IntOpenHashMap colors;

      public static BuildEngine.Spec atelier() {
         return new BuildEngine.Spec();
      }

      public static BuildEngine.Spec technique(String slot, int kind, int duration) {
         BuildEngine.Spec s = new BuildEngine.Spec();
         s.slot = slot;
         s.kind = kind;
         s.duration = duration;
         s.energy = 0.0F;
         s.reach = false;
         s.limits = false;
         s.undoable = false;
         return s;
      }

      public BuildEngine.Spec lifetime(long ticks) {
         this.lifetime = ticks;
         return this;
      }

      public BuildEngine.Spec integrity(boolean on) {
         this.integrity = on;
         return this;
      }

      public BuildEngine.Spec carve() {
         this.carve = true;
         return this;
      }

      public BuildEngine.Spec hands(Vec3 hands) {
         this.hands = hands;
         return this;
      }

      public BuildEngine.Spec duration(int ticks) {
         this.duration = ticks;
         return this;
      }

      public BuildEngine.Spec colors(@Nullable Long2IntOpenHashMap colors) {
         this.colors = colors;
         return this;
      }
   }
}
