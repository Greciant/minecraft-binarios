package com.devilfruits.core.poneglyph;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.network.DFNetwork;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

public final class RubbingService {
   public static final ResourceLocation ANIMATION = DevilFruits.id("poneglyph/rub");
   static final int HEARTBEAT_GRACE = 6;
   private static final int RESUME_WINDOW = 40;
   private static final double MAX_DISTANCE = 5.0;
   private static final DustParticleOptions INK = new DustParticleOptions(new Vector3f(0.06F, 0.05F, 0.08F), 0.8F);
   private static final Map<UUID, RubbingService.Session> SESSIONS = new HashMap<>();
   private static final Map<UUID, RubbingService.Session> PAUSED = new HashMap<>();

   public static boolean holdsTools(Player player) {
      return player.getMainHandItem().is(Items.PAPER) && player.getOffhandItem().is(Items.INK_SAC);
   }

   public static boolean rubbing(Player player) {
      return SESSIONS.containsKey(player.getUUID());
   }

   public static void heartbeat(ServerPlayer player, BlockPos pos) {
      long now = (long)player.server.getTickCount();
      RubbingService.Session session = SESSIONS.get(player.getUUID());
      if (session != null && session.pos.equals(pos)) {
         session.lastBeat = now;
      } else {
         if (session != null) {
            stop(player, session, 2);
         }

         tryStart(player, pos, now);
      }
   }

   private static void tryStart(ServerPlayer player, BlockPos pos, long now) {
      ServerLevel level = player.serverLevel();
      if (level.isLoaded(pos)
         && !player.isSpectator()
         && holdsTools(player)
         && !(player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > 25.0)
         && level.mayInteract(player, pos)
         && !player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())) {
         BlockState state = level.getBlockState(pos);
         if (state.is((Block)Poneglyphs.PONEGLYPH_FACE.get()) && lookingAt(player, pos)) {
            ResourceLocation id = PoneglyphFaceBlock.poneglyphAt(level, pos, state);
            PoneglyphData.Poneglyph def = id == null ? null : PoneglyphData.poneglyph(id);
            if (def == null) {
               player.displayClientMessage(Component.translatable("poneglyph.devilfruits.message.unreadable").withStyle(ChatFormatting.GRAY), true);
            } else if (Knowledge.knows(player, def.knowledge())) {
               player.displayClientMessage(
                  Component.translatable("poneglyph.devilfruits.message.already_known", new Object[]{Knowledge.name(def.knowledge())})
                     .withStyle(ChatFormatting.GRAY),
                  true
               );
            } else {
               int literacy = Literacy.level(player);
               if (literacy < def.minLiteracy()) {
                  player.displayClientMessage(
                     Component.translatable("poneglyph.devilfruits.message.too_ancient", new Object[]{def.minLiteracy()}).withStyle(ChatFormatting.RED), true
                  );
               } else {
                  int cell = (Integer)state.getValue(PoneglyphFaceBlock.CELL);
                  long wait = ((PoneglyphPlayerData.RubbingProgress)player.getData(Poneglyphs.RUBBING_PROGRESS)).readyAt(id, cell)
                     - player.server.overworld().getGameTime();
                  if (wait > 0L && !player.isCreative()) {
                     player.displayClientMessage(
                        Component.translatable("poneglyph.devilfruits.message.cooldown", new Object[]{(wait + 19L) / 20L}).withStyle(ChatFormatting.GRAY), true
                     );
                  } else {
                     int duration = Math.max(5, (int)Math.round((double)((Integer)PoneglyphConfig.RUB_TICKS.get()).intValue() * Literacy.rubFactor(literacy)));
                     RubbingService.Session session = new RubbingService.Session(pos.immutable(), id, cell, duration, player.position(), now);
                     RubbingService.Session paused = PAUSED.remove(player.getUUID());
                     if (paused != null && paused.pos.equals(pos) && now - paused.lastBeat <= 40L) {
                        session.ticks = Math.min(paused.ticks, duration - 1);
                     }

                     SESSIONS.put(player.getUUID(), session);
                     DFNetwork.sendTo(
                        player,
                        new PoneglyphNetwork.RubStatePayload(pos, cell, (Integer)state.getValue(PoneglyphFaceBlock.CARVING), duration - session.ticks, 0)
                     );
                     ServerFx.animate(player, ANIMATION);
                     level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.7F, 0.8F);
                  }
               }
            }
         }
      }
   }

   public static void tick(MinecraftServer server) {
      if (!SESSIONS.isEmpty()) {
         long now = (long)server.getTickCount();
         Iterator<Entry<UUID, RubbingService.Session>> it = SESSIONS.entrySet().iterator();

         while (it.hasNext()) {
            Entry<UUID, RubbingService.Session> e = it.next();
            RubbingService.Session s = e.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
            if (player == null) {
               it.remove();
            } else {
               boolean lost = now - s.lastBeat > 6L;
               if (!lost && valid(player, s)) {
                  s.ticks++;
                  feedback(player, s);
                  if (s.ticks >= s.duration) {
                     it.remove();
                     complete(player, s);
                  }
               } else {
                  it.remove();
                  if (lost) {
                     PAUSED.put(e.getKey(), s);
                  }

                  send(player, s, 2);
               }
            }
         }
      }
   }

   private static boolean valid(ServerPlayer player, RubbingService.Session s) {
      return player.isAlive()
         && holdsTools(player)
         && player.level().getBlockState(s.pos).is((Block)Poneglyphs.PONEGLYPH_FACE.get())
         && player.position().distanceToSqr(s.start) < 1.0
         && lookingAt(player, s.pos);
   }

   private static boolean lookingAt(ServerPlayer player, BlockPos pos) {
      if (player.pick(5.0, 1.0F, false) instanceof BlockHitResult bhr && bhr.getType() == Type.BLOCK && bhr.getBlockPos().equals(pos)) {
         return true;
      }

      return false;
   }

   private static void feedback(ServerPlayer player, RubbingService.Session s) {
      ServerLevel level = player.serverLevel();
      if (s.ticks % 2 == 0) {
         Direction facing = (Direction)level.getBlockState(s.pos).getValue(PoneglyphFaceBlock.FACING);
         Vec3 contact = Vec3.atCenterOf(s.pos).add((double)facing.getStepX() * 0.52, 0.0, (double)facing.getStepZ() * 0.52);
         level.sendParticles(INK, contact.x, contact.y + (level.random.nextDouble() - 0.5) * 0.5, contact.z, 2, 0.15, 0.15, 0.15, 0.0);
      }

      if (s.ticks % 6 == 1) {
         float progress = (float)s.ticks / (float)s.duration;
         level.playSound(null, s.pos, Poneglyphs.SOUND_RUB.get(), SoundSource.PLAYERS, 0.6F, 0.8F + 0.5F * progress);
      }
   }

   private static void complete(ServerPlayer player, RubbingService.Session s) {
      if (!player.isCreative()) {
         if ((Boolean)PoneglyphConfig.CONSUME_PAPER.get()) {
            player.getItemInHand(InteractionHand.MAIN_HAND).shrink(1);
         }

         if ((Boolean)PoneglyphConfig.CONSUME_INK.get()) {
            player.getItemInHand(InteractionHand.OFF_HAND).shrink(1);
         }
      }

      ItemStack rubbing = rubbing(s.poneglyph, s.cell);
      if (!player.getInventory().add(rubbing)) {
         player.drop(rubbing, false);
      }

      long ready = player.server.overworld().getGameTime() + (long)((Integer)PoneglyphConfig.CELL_COOLDOWN_SECONDS.get()).intValue() * 20L;
      player.setData(
         Poneglyphs.RUBBING_PROGRESS, ((PoneglyphPlayerData.RubbingProgress)player.getData(Poneglyphs.RUBBING_PROGRESS)).with(s.poneglyph, s.cell, ready)
      );
      Literacy.addXp(player, (Integer)PoneglyphConfig.XP_RUB.get());
      player.level().playSound(null, s.pos, Poneglyphs.SOUND_RUB_DONE.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
      Poneglyphs.FIRST_RUBBING.get().trigger(player);
      stop(player, s, 1);
      NeoForge.EVENT_BUS.post(new PoneglyphApiEvents.PoneglyphRubbedEvent(player, s.pos, s.poneglyph, s.cell));
   }

   public static ItemStack rubbing(ResourceLocation poneglyph, int cell) {
      ItemStack stack = new ItemStack((ItemLike)Poneglyphs.RUBBING_ITEM.get());
      stack.set((DataComponentType)Poneglyphs.RUBBING.get(), new BlueprintData.Rubbing(poneglyph, cell));
      stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(cell + 1));
      return stack;
   }

   public static void cancel(ServerPlayer player) {
      PAUSED.remove(player.getUUID());
      RubbingService.Session s = SESSIONS.remove(player.getUUID());
      if (s != null) {
         send(player, s, 2);
      }
   }

   private static void stop(ServerPlayer player, RubbingService.Session s, int state) {
      SESSIONS.remove(player.getUUID());
      send(player, s, state);
   }

   private static void send(ServerPlayer player, RubbingService.Session s, int state) {
      DFNetwork.sendTo(player, new PoneglyphNetwork.RubStatePayload(s.pos, s.cell, 0, s.duration, state));
      ServerFx.stopAnimation(player);
   }

   private RubbingService() {
   }

   private static final class Session {
      final BlockPos pos;
      final ResourceLocation poneglyph;
      final int cell;
      final int duration;
      final Vec3 start;
      int ticks;
      long lastBeat;

      Session(BlockPos pos, ResourceLocation poneglyph, int cell, int duration, Vec3 start, long now) {
         this.pos = pos;
         this.poneglyph = poneglyph;
         this.cell = cell;
         this.duration = duration;
         this.start = start;
         this.lastBeat = now;
      }
   }
}
