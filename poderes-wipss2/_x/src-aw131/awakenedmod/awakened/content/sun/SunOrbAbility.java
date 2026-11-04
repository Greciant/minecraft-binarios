package com.awakenedmod.awakened.content.sun;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.AwakenedParticles;
import com.awakenedmod.awakened.content.entity.projectile.KineticProjectile;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class SunOrbAbility extends Ability {
   private static final float COST = 14.0F;
   private static final float CHARGE_COST = 0.6F;
   private static final int UNLOCK = 3;
   private static final int BASE_CHARGE_TICKS = 55;
   private static final double ORB_MIN_R = 0.35;
   private static final double ORB_MAX_R = 1.5;
   private static final double MIN_VELOCITY = 0.95;
   private static final double MAX_VELOCITY = 1.7;
   private static final float MIN_DMG = 6.0F;
   private static final float MAX_DMG = 18.0F;
   private static final float MIN_AOE = 3.0F;
   private static final float MAX_AOE = 11.0F;
   private final Map<UUID, SunOrbAbility.Charge> charges = this.activeState();
   private final Map<UUID, Integer> liveOrb = this.playerState();
   private final Map<UUID, Integer> liveSun = this.playerState();

   public SunOrbAbility() {
      super("sun_orb", 14.0F, 3);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.charges.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         this.charges.put(player.getUUID(), new SunOrbAbility.Charge());
         PowerService.playAnimation(player, "sun_orb_charge");
         return true;
      }
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public void serverTick(ServerPlayer player) {
      SunOrbAbility.Charge charge = this.charges.get(player.getUUID());
      if (charge != null) {
         PowerData data = PowerService.data(player);
         long gameTime = player.serverLevel().getGameTime();
         StaminaSystem.suppressRegen(data, gameTime);
         if (!data.infiniteStamina()) {
            float chargeCost = this.billed(0.6F);
            if (data.stamina() < chargeCost) {
               this.charges.remove(player.getUUID());
               this.fire(player, charge);
               return;
            }

            StaminaSystem.drainSustained(data, gameTime, chargeCost);
         }

         float exposure = Solar.exposure(player, data);
         float rate = 0.018181818F * (0.45F + 0.9F * exposure);
         charge.progress = Math.min(1.0F, charge.progress + rate);
         double r = Mth.lerp((double)charge.progress, 0.35, 1.5);
         Vec3 c = orbCenter(player, r);
         ServerLevel level = player.serverLevel();
         if (charge.sun == null || !charge.sun.isAlive()) {
            charge.sun = new SunOrbEntity(AwakenedEntities.SUN_ORB.get(), level);
            charge.sun.configure((float)r, 205);
            charge.sun.setPos(c.x, c.y, c.z);
            level.addFreshEntity(charge.sun);
         }

         charge.sun.setPos(c.x, c.y, c.z);
         charge.sun.setRadius((float)r);
         if (player.tickCount % 3 == 0) {
            Fx.burst(level, (ParticleOptions)AwakenedParticles.SUN_MOTE.get(), c, 2, r * 0.7, 0.04);
         }

         if (player.tickCount % 4 == 0) {
            PowerService.sync(player);
         }

         if (charge.progress >= 1.0F) {
            this.charges.remove(player.getUUID());
            this.fire(player, charge);
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      SunOrbAbility.Charge charge = this.charges.remove(player.getUUID());
      if (charge != null) {
         this.fire(player, charge);
      }
   }

   private void fire(ServerPlayer player, SunOrbAbility.Charge charge) {
      ServerLevel level = player.serverLevel();
      PowerData data = PowerService.data(player);
      float f = charge.progress;
      if (charge.sun != null) {
         charge.sun.discard();
         charge.sun = null;
      }

      float m = Solar.scale(data, 0.6F, 1.6F);
      boolean axe = Solar.hasAxe(player);
      if (axe) {
         m *= 1.5F;
      }

      float ballRadius = (float)Mth.lerp((double)f, 0.35, 1.5);
      double blast = (double)ballRadius * (axe ? 3.6 : 3.0) * (0.7 + 0.5 * (double)m);
      float dmg = MasteryScaling.scaledDamage(Mth.lerp(f, 6.0F, 18.0F), player, this.balanceKey(), m - 1.0F);
      float aoe = MasteryScaling.scaledDamage(Mth.lerp(f, 3.0F, 11.0F), player, this.balanceKey(), m - 1.0F);
      float velocity = (float)Mth.lerp((double)f, 0.95, 1.7);
      Vec3 origin = orbCenter(player, (double)ballRadius);
      SunOrbEntity sun = new SunOrbEntity(AwakenedEntities.SUN_ORB.get(), level);
      sun.setPos(origin.x, origin.y, origin.z);
      sun.configure(ballRadius, 200);
      KineticProjectile orb = new KineticProjectile(AwakenedEntities.KINETIC_CHARGE.get(), player, level, ItemStack.EMPTY);
      orb.setPos(origin.x, origin.y, origin.z);
      orb.configure(dmg, aoe, (float)blast, 1.0F);
      orb.setIgnite(Math.max(1, Math.round(4.0F * m)));
      orb.setFlameAura((double)ballRadius);
      orb.setAuraParticle((ParticleOptions)AwakenedParticles.SUN_MOTE.get());
      orb.setContactRadius((double)ballRadius);
      orb.setCustomDetonation(true);
      orb.setOnTick(pos -> sun.setPos(pos.x, pos.y, pos.z));
      orb.setOnDetonate(pos -> {
         sun.discard();
         this.liveSun.remove(player.getUUID());
         this.liveOrb.remove(player.getUUID());
         SunFx.detonation(level, pos, blast);
      });
      level.addFreshEntity(sun);
      Vec3 target = player.pick(64.0, 1.0F, false).getLocation();
      Vec3 dir = target.subtract(origin);
      orb.shoot(dir.x, dir.y, dir.z, velocity, 0.3F);
      level.addFreshEntity(orb);
      this.liveOrb.put(player.getUUID(), orb.getId());
      this.liveSun.put(player.getUUID(), sun.getId());
      Fx.burst(level, (ParticleOptions)AwakenedParticles.SUN_MOTE.get(), origin, 20, (double)ballRadius * 0.6, 0.12);
      Fx.sound(level, player.position(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
      PowerService.stopAnimation(player);
      PowerService.playAnimation(player, "sun_orb_throw");
   }

   private static Vec3 orbCenter(ServerPlayer player, double radius) {
      return player.getEyePosition().add(0.0, 0.6 + radius, 0.0);
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      SunOrbAbility.Charge charge = this.charges.remove(player.getUUID());
      if (charge != null) {
         if (charge.sun != null) {
            charge.sun.discard();
         }

         PowerService.stopAnimation(player);
      }

      discardTracked(player, this.liveOrb);
      discardTracked(player, this.liveSun);
      super.clearPlayer(player);
   }

   private static void discardTracked(ServerPlayer player, Map<UUID, Integer> map) {
      Integer id = map.remove(player.getUUID());
      if (id != null) {
         Entity var4 = player.serverLevel().getEntity(id);
         if (var4 instanceof Entity) {
            var4.discard();
         }
      }
   }

   private static final class Charge {
      private float progress;
      private SunOrbEntity sun;
   }
}
