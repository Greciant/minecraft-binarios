package com.awakenedmod.awakened.content.shadow;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

public final class ShadowOrbAbility extends Ability {
   private static final float COST = 14.0F;
   private static final float CHARGE_COST = 0.6F;
   private static final int UNLOCK = 7;
   private static final int BASE_CHARGE_TICKS = 60;
   private static final double ORB_MIN_R = 0.3;
   private static final double ORB_MAX_R = 2.5;
   private static final float SMALL = 0.34F;
   private static final float MEDIUM = 0.7F;
   private static final float MIN_DMG = 5.0F;
   private static final float MAX_DMG = 16.0F;
   private static final float MIN_AOE_DMG = 3.0F;
   private static final float MAX_AOE_DMG = 10.0F;
   private static final double MIN_VELOCITY = 1.6;
   private static final double MAX_VELOCITY = 2.6;
   private final Map<UUID, ShadowOrbAbility.Charge> charges = this.activeState();

   public ShadowOrbAbility() {
      super("shadow_orb", 14.0F, 7);
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
         this.charges.put(player.getUUID(), new ShadowOrbAbility.Charge());
         return true;
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      ShadowOrbAbility.Charge charge = this.charges.get(player.getUUID());
      if (charge != null) {
         PowerData data = PowerService.data(player);
         long gameTime = player.serverLevel().getGameTime();
         StaminaSystem.suppressRegen(data, gameTime);
         if (!data.infiniteStamina()) {
            float drain = this.billed(0.6F);
            if (data.stamina() < drain) {
               this.charges.remove(player.getUUID());
               this.fire(player, charge);
               return;
            }

            StaminaSystem.drainSustained(data, gameTime, drain);
         }

         float rate = 0.016666668F * (0.6F + 0.8F * ShadowDark.darkness(player));
         charge.progress = Math.min(1.0F, charge.progress + rate);
         player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, 250, false, false));
         player.setDeltaMovement(0.0, player.getDeltaMovement().y, 0.0);
         player.hurtMarked = true;
         double r = Mth.lerp((double)charge.progress, 0.3, 2.5);
         Vec3 c = orbCenter(player, r);
         ServerLevel level = player.serverLevel();
         if (charge.orb == null || !charge.orb.isAlive()) {
            charge.orb = new ShadowShapeEntity(AwakenedEntities.SHADOW_SHAPE.get(), level);
            charge.orb.configure(ShadowShapeEntity.Shape.SPHERE, (float)r, 788500, 205, 0);
            charge.orb.setPos(c.x, c.y, c.z);
            level.addFreshEntity(charge.orb);
         }

         charge.orb.setPos(c.x, c.y, c.z);
         charge.orb.setRadius((float)r);
         if (player.tickCount % 3 == 0) {
            level.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y, c.z, 1, r * 0.4, r * 0.4, r * 0.4, 0.0);
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
      ShadowOrbAbility.Charge charge = this.charges.remove(player.getUUID());
      if (charge != null) {
         this.fire(player, charge);
      }
   }

   private void fire(ServerPlayer player, ShadowOrbAbility.Charge charge) {
      ServerLevel level = player.serverLevel();
      float f = charge.progress;
      if (charge.orb != null) {
         charge.orb.discard();
         charge.orb = null;
      }

      double r = Mth.lerp((double)f, 0.3, 2.5);
      Vec3 origin = orbCenter(player, r);
      Vec3 target = player.pick(64.0, 1.0F, false).getLocation();
      Vec3 dir = target.subtract(origin);
      float darkMul = ShadowDark.scale(player, 0.75F, 1.3F);
      float dmg = MasteryScaling.scaledDamage(Mth.lerp(f, 5.0F, 16.0F) * darkMul, player, this.balanceKey());
      ShadowBolt orb = new ShadowBolt(AwakenedEntities.SHADOW_BOLT.get(), player, level);
      orb.setPos(origin.x, origin.y, origin.z);
      if (f < 0.34F) {
         orb.configure(dmg, 0.0F, 0.0F, Math.max(0.4, r));
         orb.setPierce(4);
      } else {
         float aoeDmg = MasteryScaling.scaledDamage(Mth.lerp(f, 3.0F, 10.0F) * darkMul, player, this.balanceKey());
         double blastRadius = r * 1.6;
         orb.configure(dmg, aoeDmg, (float)blastRadius, r);
         orb.setPierce(0);
         if (f >= 0.7F) {
            double zoneRadius = r * 1.4;
            int zoneTicks = (int)Mth.lerp(f, 60.0F, 120.0F);
            float zoneDps = 2.0F * darkMul;
            orb.setOnDetonate(pos -> ShadowZones.open(player, level, pos, zoneRadius, zoneTicks, zoneDps, false, false, false, this.balanceKey()));
         }
      }

      orb.setMaxLife(80);
      orb.shoot(dir.x, dir.y, dir.z, (float)Mth.lerp((double)f, 1.6, 2.6), 0.4F);
      level.addFreshEntity(orb);
      ShadowFx.orbBurst(level, origin, Math.max(0.6, r * 0.5));
      player.swing(InteractionHand.MAIN_HAND);
      PowerService.playAnimation(player, "shadow_orb_throw");
   }

   private static Vec3 orbCenter(ServerPlayer player, double radius) {
      return player.getEyePosition().add(0.0, 0.6 + radius, 0.0);
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      ShadowOrbAbility.Charge charge = this.charges.remove(player.getUUID());
      if (charge != null && charge.orb != null) {
         charge.orb.discard();
      }

      super.clearPlayer(player);
   }

   private static final class Charge {
      private float progress;
      private ShadowShapeEntity orb;
   }
}
