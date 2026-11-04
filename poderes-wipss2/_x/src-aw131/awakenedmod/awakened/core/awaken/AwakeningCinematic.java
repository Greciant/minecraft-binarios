package com.awakenedmod.awakened.core.awaken;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.net.AwakenScreenFxPayload;
import com.awakenedmod.awakened.core.power.Power;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AwakeningCinematic {
   private static final int BUILDUP_TICKS = 22;
   private static final int SETTLE_TICKS = 30;
   private static final int SCREEN_FX_TICKS = 24;
   private static final Map<UUID, AwakeningCinematic.Active> ACTIVE = new ConcurrentHashMap<>();

   private AwakeningCinematic() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(AwakeningCinematic::onServerTick);
      gameBus.addListener(AwakeningCinematic::onLogout);
      gameBus.addListener(AwakeningCinematic::onServerStopping);
   }

   public static Power pendingPower(UUID playerId) {
      AwakeningCinematic.Active active = ACTIVE.get(playerId);
      return active != null && active.onReveal != null ? active.power : null;
   }

   public static void flushPending(ServerPlayer player) {
      AwakeningCinematic.Active active = ACTIVE.get(player.getUUID());
      if (active != null && active.onReveal != null) {
         ACTIVE.remove(player.getUUID());
         active.reveal(player);
      }
   }

   public static void begin(ServerPlayer player, Power power) {
      beginDelayed(player, power, 0);
   }

   public static void beginDelayed(ServerPlayer player, Power power, int ticksUntilEruption) {
      beginDelayed(player, power, ticksUntilEruption, null);
   }

   public static void beginDelayed(ServerPlayer player, Power power, int ticksUntilEruption, Consumer<ServerPlayer> onReveal) {
      AwakeningCinematic.Active active = new AwakeningCinematic.Active(power, onReveal);
      active.delay = Math.max(0, ticksUntilEruption - 22);
      AwakeningCinematic.Active replaced = ACTIVE.put(player.getUUID(), active);
      if (replaced != null) {
         replaced.reveal(player);
      }

      Fx.sound(player.serverLevel(), player.position(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.8F);
   }

   public static void skipDelay(ServerPlayer player) {
      if (player != null) {
         AwakeningCinematic.Active active = ACTIVE.get(player.getUUID());
         if (active != null) {
            active.delay = 0;
            active.reveal(player);
         }
      }
   }

   private static void onServerTick(Post event) {
      if (!ACTIVE.isEmpty()) {
         ACTIVE.entrySet().removeIf(entry -> {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            return player == null ? true : advance(player, entry.getValue());
         });
      }
   }

   private static void onLogout(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         AwakeningCinematic.Active active = ACTIVE.remove(player.getUUID());
         if (active != null) {
            active.reveal(player);
         }
      }
   }

   private static void onServerStopping(ServerStoppingEvent event) {
      ACTIVE.forEach((uuid, active) -> {
         ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
         if (player != null) {
            active.reveal(player);
         }
      });
      ACTIVE.clear();
   }

   private static boolean advance(ServerPlayer player, AwakeningCinematic.Active active) {
      if (active.delay > 0) {
         active.delay--;
         return false;
      } else {
         int t = active.tick++;
         if (t < 22) {
            buildUp(player, active.power, t);
         } else if (t == 22) {
            active.reveal(player);
            erupt(player, active.power);
         } else {
            settle(player, active.power, t - 22);
         }

         return t >= 52;
      }
   }

   private static void buildUp(ServerPlayer player, Power power, int t) {
      ServerLevel level = player.serverLevel();
      DustParticleOptions dust = Fx.dust(power.themeColor() & 16777215, 1.0F);
      Vec3 base = player.position();
      float frac = (float)t / 22.0F;
      double radius = 2.4 * (1.0 - (double)frac) + 0.2;
      double height = 0.1 + (double)frac * 1.9;
      int motes = 7;

      for (int i = 0; i < motes; i++) {
         double angle = (double)t * 0.55 + (double)i * ((Math.PI * 2) / (double)motes);
         double x = base.x + Math.cos(angle) * radius;
         double z = base.z + Math.sin(angle) * radius;
         level.sendParticles(dust, x, base.y + height, z, 1, 0.0, 0.0, 0.0, 0.0);
      }

      level.sendParticles(ParticleTypes.ENCHANT, base.x, base.y + 0.2, base.z, 2, 0.25, 0.1, 0.25, 0.4);
      if (t == 11) {
         Fx.sound(level, base, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.9F, 1.4F);
      }
   }

   private static void erupt(ServerPlayer player, Power power) {
      ServerLevel level = player.serverLevel();
      Vec3 chest = player.position().add(0.0, 1.0, 0.0);
      Fx.burst(level, ParticleTypes.EXPLOSION, chest, 1, 0.0, 0.0);
      Fx.burst(level, ParticleTypes.END_ROD, chest, 80, 0.6, 0.25);
      Fx.burst(level, ParticleTypes.ENCHANT, player.position().add(0.0, 1.2, 0.0), 60, 0.8, 0.5);
      DustParticleOptions dust = Fx.dust(power.themeColor() & 16777215, 1.4F);
      ring(level, dust, player.position().add(0.0, 0.1, 0.0), 2.6, 40);
      Fx.sound(level, player.position(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
      Fx.sound(level, player.position(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.2F);
      Fx.sound(level, player.position(), (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7F, 0.7F);
      power.onAwaken(player);
      player.connection.send(new ClientboundSetTitlesAnimationPacket(8, 50, 20));
      player.connection.send(new ClientboundSetTitleTextPacket(power.displayName()));
      player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("awakened.awakening.subtitle")));
      PacketDistributor.sendToPlayer(player, new AwakenScreenFxPayload(power.themeColor() & 16777215, 24), new CustomPacketPayload[0]);
   }

   private static void settle(ServerPlayer player, Power power, int t) {
      ServerLevel level = player.serverLevel();
      DustParticleOptions dust = Fx.dust(power.themeColor() & 16777215, 0.9F);
      Vec3 base = player.position();
      int motes = Math.max(1, 5 - t / 8);
      level.sendParticles(dust, base.x, base.y + 1.1, base.z, motes, 0.6, 0.8, 0.6, 0.02);
   }

   private static void ring(ServerLevel level, DustParticleOptions dust, Vec3 center, double radius, int points) {
      for (int i = 0; i < points; i++) {
         double angle = (double)i * ((Math.PI * 2) / (double)points);
         level.sendParticles(dust, center.x + Math.cos(angle) * radius, center.y, center.z + Math.sin(angle) * radius, 1, 0.0, 0.05, 0.0, 0.0);
      }
   }

   private static final class Active {
      private final Power power;
      private int tick;
      private int delay;
      private Consumer<ServerPlayer> onReveal;

      private Active(Power power, Consumer<ServerPlayer> onReveal) {
         this.power = power;
         this.onReveal = onReveal;
      }

      private void reveal(ServerPlayer player) {
         if (this.onReveal != null) {
            Consumer<ServerPlayer> swap = this.onReveal;
            this.onReveal = null;
            swap.accept(player);
         }
      }
   }
}
