package com.awakenedmod.awakened.content.shadow;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PhaseState;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.TeleportUtil;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.MeldHidePayload;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ShadowMeldAbility extends Ability {
   private static final float COST = 8.0F;
   private static final float MELD_COST = 0.5F;
   private static final int UNLOCK = 5;
   private static final double AGGRO_CLEAR_RADIUS = 24.0;
   private static final long AMBUSH_TICKS = 100L;
   private final Map<UUID, Boolean> melded = this.playerState();
   private final Map<UUID, Long> surfaced = this.playerState();

   public ShadowMeldAbility() {
      super("shadow_meld", 8.0F, 5);
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return 0.0F;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.melded.containsKey(player.getUUID())) {
         this.surface(player, null);
         return true;
      } else if (lit(player)) {
         return this.fail(player, "awakened.shadow.too_bright", new Object[0]);
      } else {
         PowerData data = PowerService.data(player);
         float cost = this.billed(8.0F);
         if (!data.infiniteStamina() && data.stamina() < cost) {
            return this.fail(player, "awakened.feedback.no_stamina", new Object[0]);
         } else {
            if (!data.infiniteStamina()) {
               data.setStamina(data.stamina() - cost);
            }

            this.melded.put(player.getUUID(), Boolean.TRUE);
            PhaseState.setServer(player.getUUID(), true);
            player.setInvulnerable(true);
            broadcastHidden(player, true);
            phaseFx(player);
            this.submerge(player);
            return true;
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      if (this.melded.containsKey(player.getUUID())) {
         ServerLevel level = player.serverLevel();
         if (lit(player)) {
            this.eject(player);
         } else {
            PowerData data = PowerService.data(player);
            long gameTime = level.getGameTime();
            StaminaSystem.suppressRegen(data, gameTime);
            if (!data.infiniteStamina()) {
               float drain = this.billed(0.5F);
               if (data.stamina() < drain) {
                  this.surface(player, "awakened.shadow.meld_drained");
                  return;
               }

               StaminaSystem.drainSustained(data, gameTime, drain);
            }

            this.submerge(player);
            if (player.tickCount % 4 == 0) {
               PowerService.sync(player);
            }
         }
      }
   }

   private void submerge(ServerPlayer player) {
      PhaseState.setServer(player.getUUID(), true);
      player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 12, 0, false, false));
      player.setInvulnerable(true);
      player.resetFallDistance();
      ServerLevel level = player.serverLevel();
      if (player.tickCount % 4 == 0) {
         Vec3 c = player.position().add(0.0, (double)player.getBbHeight() * 0.45, 0.0);
         level.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y, c.z, 1, 0.25, 0.35, 0.25, 0.0);
      }

      PhaseState.dropAggro(player, 24.0);
   }

   private static void phaseFx(ServerPlayer player) {
      ServerLevel at = player.serverLevel();
      if (at instanceof ServerLevel) {
         Vec3 atx = player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);
         at.sendParticles(ParticleTypes.SQUID_INK, atx.x, atx.y, atx.z, 14, 0.3, 0.5, 0.3, 0.01);
         Fx.sound(at, atx, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 1.0F);
      }
   }

   private static void broadcastHidden(ServerPlayer player, boolean hidden) {
      PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new MeldHidePayload(player.getId(), hidden), new CustomPacketPayload[0]);
   }

   private void eject(ServerPlayer player) {
      Fx.sound(player.serverLevel(), player.position(), SoundEvents.ENDERMAN_SCREAM, SoundSource.PLAYERS, 0.7F, 1.3F);
      this.surface(player, "awakened.shadow.meld_eject");
   }

   private void surface(ServerPlayer player, String message) {
      if (this.melded.remove(player.getUUID()) != null) {
         this.surfaced.put(player.getUUID(), player.serverLevel().getGameTime());
         PhaseState.setServer(player.getUUID(), false);
         player.removeEffect(MobEffects.INVISIBILITY);
         player.setInvulnerable(false);
         broadcastHidden(player, false);
         TeleportUtil.ejectFromWall(player);
         phaseFx(player);
         if (message != null) {
            player.displayClientMessage(Component.translatable(message), true);
         }

         PowerService.sync(player);
      }
   }

   public boolean leftShadowRecently(ServerPlayer player) {
      Long at = this.surfaced.get(player.getUUID());
      return at != null && player.serverLevel().getGameTime() - at <= 100L;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.melded.remove(player.getUUID()) != null) {
         PhaseState.setServer(player.getUUID(), false);
         player.removeEffect(MobEffects.INVISIBILITY);
         player.setInvulnerable(false);
         broadcastHidden(player, false);
      }

      super.clearPlayer(player);
   }

   private static boolean lit(ServerPlayer player) {
      return ShadowDark.lightAt(player) >= 8;
   }
}
