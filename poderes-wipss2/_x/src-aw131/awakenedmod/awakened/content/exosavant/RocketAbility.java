package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class RocketAbility extends Ability {
   private static final int UNLOCK = 0;
   private static final float COST = 12.0F;
   private static final float IMPACT_DAMAGE = 7.0F;
   private static final float AOE_DAMAGE = 4.0F;
   private static final float AOE_RADIUS = 2.6F;
   private static final float KNOCKBACK = 1.7F;
   private static final double VELOCITY = 2.6;
   private static final double GRAVITY = 0.03;
   private static final double CONTACT_RADIUS = 0.5;
   private static final double HOMING_STRENGTH = 0.18;
   private static final float HOMING_INACCURACY = 0.25F;
   private static final int LAUNCH_WINDUP_TICKS = 5;
   private static final int REFIRE_LOCK_TICKS = 22;
   private final Map<UUID, Long> pendingShotAt = this.playerState();
   private final Map<UUID, Long> refireLockUntil = this.playerState();

   public RocketAbility() {
      super("rocket", 12.0F, 0);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(7.0).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (!ExoLoadout.usable(player, ExoPiece.GAUNTLETS)) {
         return false;
      } else if (this.pendingShotAt.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.exosavant.reloading", new Object[0]);
      } else {
         long now = player.serverLevel().getGameTime();
         Long lockUntil = this.refireLockUntil.get(player.getUUID());
         if (lockUntil != null && now < lockUntil) {
            return this.fail(player, "awakened.exosavant.reloading", new Object[0]);
         } else {
            ExoDeploy.deployHandCannon(player);
            if (!ExoFlight.isJet(player)) {
               PowerService.playAnimation(player, "rocket_fire");
            }

            player.swing(InteractionHand.MAIN_HAND);
            this.pendingShotAt.put(player.getUUID(), now + 5L);
            this.refireLockUntil.put(player.getUUID(), now + 22L);
            return true;
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      Long fireAt = this.pendingShotAt.get(player.getUUID());
      if (fireAt != null) {
         if (!ExoLoadout.usable(player, ExoPiece.GAUNTLETS)) {
            this.pendingShotAt.remove(player.getUUID());
         } else if (player.serverLevel().getGameTime() >= fireAt) {
            this.pendingShotAt.remove(player.getUUID());
            this.launchRocket(player);
         }
      }
   }

   private void launchRocket(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 origin = AimUtil.handPosition(player);
      Vec3 dir = player.getLookAngle();
      LivingEntity locked = ExoTags.primaryTarget(player);
      float impact = MasteryScaling.scaledDamage(7.0F, player, this.balanceKey());
      float aoe = MasteryScaling.scaledDamage(4.0F, player, this.balanceKey());
      ExoProjectiles.spawnShell(
         AwakenedEntities.EXO_MISSILE.get(),
         player,
         level,
         Items.FIRE_CHARGE,
         origin,
         dir,
         2.6,
         locked != null ? 0.25F : 1.0F,
         impact,
         aoe,
         2.6F,
         1.7F,
         0.03,
         0.5,
         locked,
         0.18,
         p -> ExoFx.rocketBlast(level, p, 2.6F)
      );
      Fx.sound(level, origin, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
      Fx.sound(level, origin, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.35F, 1.9F);
      Fx.burst(level, ParticleTypes.SMOKE, origin, 6, 0.1, 0.02);
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      ExoSuit.clear(player);
      ExoFlight.clear(player);
      ExoDeploy.clear(player);
      ExoVisor.clear(player);
      ExoTags.clear(player);
      ScanReveal.clear(player);
      ExoAnalysis.clear(player);
      super.clearPlayer(player);
   }
}
