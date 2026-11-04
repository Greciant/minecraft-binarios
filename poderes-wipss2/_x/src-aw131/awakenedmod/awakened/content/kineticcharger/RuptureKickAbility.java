package com.awakenedmod.awakened.content.kineticcharger;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.RealisticMovement;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class RuptureKickAbility extends KineticChargedStrike {
   public static final String ID = "rupture_kick";
   private static final int UNLOCK = 7;
   private static final float COST = 14.0F;
   private static final int WINDUP_TICKS = 6;
   private static final int SAFETY_TICKS = 1200;
   private static final String CHARGE_CLIP = "rupture_kick_charge";
   private static final String KICK_CLIP = "rupture_kick";
   private static final float MAX_SPEND = 40.0F;
   private static final float BASE_DAMAGE = 5.0F;
   private static final float DAMAGE_PER_CHARGE = 0.5F;
   public static final Lab.Value LENGTH = Lab.SERVER.def("kinetic.rupture.length", 12.0, 2.0, 40.0, 0.5);
   public static final Lab.Value LENGTH_MIN = Lab.SERVER.def("kinetic.rupture.length_min", 6.0, 1.0, 40.0, 0.5);
   public static final Lab.Value WIDTH = Lab.SERVER.def("kinetic.rupture.width", 6.0, 0.1, 12.0, 0.05);
   public static final Lab.Value WIDTH_MIN = Lab.SERVER.def("kinetic.rupture.width_min", 3.0, 0.1, 12.0, 0.05);
   public static final Lab.Value NEAR = Lab.SERVER.def("kinetic.rupture.near", 0.6, 0.05, 6.0, 0.05);
   public static final Lab.Value SPEED = Lab.SERVER.def("kinetic.rupture.speed", 2.0, 0.5, 10.0, 0.1);
   private static final double GROUND_REACH = 1.5;
   private static final double FOOT_DROP = 2.0;
   private static final double LAUNCH_UP_BASE = 0.9;
   private static final double LAUNCH_UP_FILL = 0.7;
   private static final double LAUNCH_OUT = 0.3;
   private static final int FEAT_LAUNCHED = 3;
   private static final String FEAT = "kinetic_rupture_launch";

   public static double lengthFor(float fill) {
      return Mth.lerp((double)Mth.clamp(fill, 0.0F, 1.0F), LENGTH_MIN.get(), LENGTH.get());
   }

   public static double widthFor(float fill) {
      return Mth.lerp((double)Mth.clamp(fill, 0.0F, 1.0F), WIDTH_MIN.get(), WIDTH.get());
   }

   public RuptureKickAbility() {
      super("rupture_kick", 14.0F, 7, 6, 40.0F, 1200);
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return "rupture_kick_charge";
   }

   @Override
   protected boolean canBegin(ServerPlayer player) {
      return RealisticMovement.hasFooting(player) || this.fail(player, "awakened.feedback.no_footing", new Object[0]);
   }

   @Override
   protected void windupFx(ServerPlayer player, float progress) {
      KineticFx.ruptureWindup(player, progress);
   }

   @Override
   protected void pourFx(ServerPlayer player, int ticks, float fill) {
      KineticFx.rupturePour(player, ticks, fill);
   }

   @Override
   protected void releaseFx(ServerPlayer player) {
      PowerService.playAnimation(player, "rupture_kick");
      KineticFx.ruptureSling(player);
   }

   @Override
   protected void strike(ServerPlayer player, float spent) {
      ServerLevel world = player.serverLevel();
      Vec3 apex = AimUtil.groundedPositionIfFound(world, player.position()).filter(ground -> player.getY() - ground.y <= 2.0).orElse(null);
      if (apex == null) {
         this.fail(player, "awakened.feedback.no_footing", new Object[0]);
         this.refundPour(player, spent);
      } else {
         Vec3 flat = AimUtil.lookFlat(player);
         float fill = this.fillOf(spent);
         double length = lengthFor(fill);
         double halfNear = NEAR.get();
         double halfFar = widthFor(fill);
         KineticFx.ruptureKick(player, apex, flat, length, halfNear, halfFar, fill);
         List<LivingEntity> caught = onTheGround(player, apex, flat, length, halfNear, halfFar);
         if (!caught.isEmpty()) {
            float damage = KineticBank.empoweredDamage(player, 5.0F + spent * 0.5F, this.balanceKey());
            double up = KineticBank.empowered(player, 0.9 + (double)fill * 0.7);
            double out = KineticBank.empowered(player, 0.3);
            double speed = Math.max(0.1, SPEED.get());
            List<TimedBlows.Blow> pending = new ArrayList<>(caught.size());

            for (LivingEntity target : caught) {
               double dist = along(apex, flat, target);
               pending.add(new TimedBlows.Blow(target, (int)Math.ceil(dist / speed), dist));
            }

            this.blows
               .launch(
                  player, pending, (kicker, level, targetx, distx) -> land(kicker, level, apex, targetx, damage, out, up, fill), struck -> feat(player, struck)
               );
         }
      }
   }

   private static boolean land(ServerPlayer player, ServerLevel level, Vec3 apex, LivingEntity target, float damage, double out, double up, float fill) {
      if (!TimedBlows.stillLegal(player, level, target)) {
         return false;
      } else {
         Dmg.hurtNoKnockback(target, level, player.damageSources().playerAttack(player), damage);
         Vec3 away = outward(player, apex, target);
         Harm.push(player, target, away.x * out, up, away.z * out);
         KineticFx.ruptureHit(level, target, fill);
         return true;
      }
   }

   private static void feat(ServerPlayer player, List<LivingEntity> struck) {
      int threats = 0;

      for (LivingEntity victim : struck) {
         if (Foes.threat(victim, player)) {
            threats++;
         }
      }

      if (threats >= 3) {
         QuestSystem.feat(player, "kinetic_rupture_launch");
      }
   }

   private static List<LivingEntity> onTheGround(ServerPlayer player, Vec3 apex, Vec3 flat, double length, double halfNear, double halfFar) {
      return Harm.hostilesOnly(player, AimUtil.livingInGroundWedge(player, apex, flat, length, halfNear, halfFar, 1.5));
   }

   private static double along(Vec3 apex, Vec3 flat, LivingEntity target) {
      return Math.max(0.0, (target.getX() - apex.x) * flat.x + (target.getZ() - apex.z) * flat.z);
   }
}
