package com.awakenedmod.awakened.content.kineticcharger;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.PowerService;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class ImpactStrikeAbility extends KineticChargedStrike {
   public static final String ID = "impact_strike";
   private static final int UNLOCK = 5;
   private static final float COST = 10.0F;
   private static final int WINDUP_TICKS = 6;
   private static final int SAFETY_TICKS = 1200;
   private static final String CHARGE_CLIP = "impact_strike_charge";
   private static final String PUNCH_CLIP = "impact_strike";
   private static final float MAX_SPEND = 40.0F;
   private static final float BASE_DAMAGE = 6.0F;
   private static final float DAMAGE_PER_CHARGE = 0.5F;
   public static final Lab.Value REACH = Lab.SERVER.def("kinetic.ring.reach", 10.5, 2.0, 30.0, 0.5);
   public static final Lab.Value RADIUS_NEAR = Lab.SERVER.def("kinetic.ring.near", 0.25, 0.1, 12.0, 0.05);
   public static final Lab.Value RADIUS_FAR = Lab.SERVER.def("kinetic.ring.far", 3.0, 0.1, 12.0, 0.05);
   public static final Lab.Value REACH_MIN = Lab.SERVER.def("kinetic.ring.reach_min", 6.0, 1.0, 30.0, 0.5);
   public static final Lab.Value RADIUS_FAR_MIN = Lab.SERVER.def("kinetic.ring.far_min", 1.2, 0.1, 12.0, 0.05);
   private static final double FULL_DAMAGE_SHARE = 0.15;
   private static final float FAR_DAMAGE = 0.4F;
   private static final double KNOCK_OUT_BASE = 0.8;
   private static final double KNOCK_OUT_SPENT = 1.4;
   private static final double KNOCK_UP_BASE = 0.3;
   private static final double KNOCK_UP_SPENT = 0.4;
   private static final double BLOCKS_PER_TICK = 3.0;
   private static final double LEAD = 2.0;

   public static double reachFor(float fill) {
      return Mth.lerp((double)Mth.clamp(fill, 0.0F, 1.0F), REACH_MIN.get(), REACH.get());
   }

   public static double farFor(float fill) {
      return Mth.lerp((double)Mth.clamp(fill, 0.0F, 1.0F), RADIUS_FAR_MIN.get(), RADIUS_FAR.get());
   }

   public ImpactStrikeAbility() {
      super("impact_strike", 10.0F, 5, 6, 40.0F, 1200);
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return "impact_strike_charge";
   }

   @Override
   protected void windupFx(ServerPlayer player, float progress) {
      KineticFx.strikeWindup(player, progress);
   }

   @Override
   protected void pourFx(ServerPlayer player, int ticks, float fill) {
      KineticFx.strikePour(player, ticks, fill);
   }

   @Override
   protected void releaseFx(ServerPlayer player) {
      player.swing(InteractionHand.MAIN_HAND);
      PowerService.playAnimation(player, "impact_strike");
      KineticFx.strikeSling(player);
   }

   @Override
   protected void strike(ServerPlayer player, float spent) {
      Vec3 origin = KineticFx.fist(player);
      Vec3 dir = player.getLookAngle().normalize();
      float fill = this.fillOf(spent);
      double reach = reachFor(fill);
      double far = farFor(fill);
      List<LivingEntity> caught = inFront(player, origin, dir, reach, far);
      KineticFx.strikeWave(player, origin, dir, reach, fill);
      if (!caught.isEmpty()) {
         float damage = KineticBank.empoweredDamage(player, 6.0F + spent * 0.5F, this.balanceKey());
         double out = KineticBank.empowered(player, 0.8 + (double)fill * 1.4);
         double up = KineticBank.empowered(player, 0.3 + (double)fill * 0.4);
         List<TimedBlows.Blow> pending = new ArrayList<>(caught.size());

         for (LivingEntity target : caught) {
            double dist = origin.distanceTo(target.getBoundingBox().getCenter());
            pending.add(new TimedBlows.Blow(target, arrivalDelay(dist), dist));
         }

         this.blows
            .launch(player, pending, (thrower, level, targetx, distx) -> land(thrower, level, origin, targetx, damage * falloff(distx, reach), out, up, fill));
      }
   }

   private static boolean land(ServerPlayer player, ServerLevel level, Vec3 origin, LivingEntity target, float damage, double out, double up, float fill) {
      if (!TimedBlows.stillLegal(player, level, target)) {
         return false;
      } else {
         Vec3 away = outward(player, origin, target);
         Dmg.hurtNoKnockback(target, level, player.damageSources().playerAttack(player), damage);
         Harm.push(player, target, away.x * out, up, away.z * out);
         KineticFx.strikeWaveHit(level, target, origin, away, fill);
         return true;
      }
   }

   private static int arrivalDelay(double dist) {
      return (int)Math.ceil(Math.max(0.0, dist - 2.0) / 3.0);
   }

   private static float falloff(double dist, double reach) {
      double full = reach * 0.15;
      return dist <= full ? 1.0F : Mth.lerp((float)Mth.clamp((dist - full) / Math.max(1.0E-6, reach - full), 0.0, 1.0), 1.0F, 0.4F);
   }

   private static List<LivingEntity> inFront(ServerPlayer player, Vec3 fist, Vec3 dir, double reach, double far) {
      Vec3 eye = player.getEyePosition();
      double lead = Math.max(0.0, fist.subtract(eye).dot(dir));
      return Harm.hostilesOnly(player, AimUtil.livingInFrustum(player, eye, dir, reach + lead, RADIUS_NEAR.get(), far));
   }
}
