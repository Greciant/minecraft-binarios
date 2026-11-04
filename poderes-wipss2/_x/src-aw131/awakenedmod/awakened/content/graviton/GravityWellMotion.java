package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Lab;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class GravityWellMotion {
   public static final ResourceLocation GRAVITY_ID = AwId.of("graviton_well_gravity");
   public static final Lab.Value RANGE = Lab.SERVER.def("graviton.well.range", 50.0, 10.0, 120.0, 1.0);
   public static final Lab.Value GRAVITY = Lab.SERVER.def("graviton.well.gravity", 0.03, 0.005, 0.07, 0.005);
   public static final Lab.Value LIFE = Lab.SERVER.def("graviton.well.life_ticks", 60.0, 20.0, 200.0, 1.0);
   public static final Lab.Value PULL = Lab.CLIENT.def("graviton.well.pull", 0.13, 0.02, 0.4, 0.01);
   public static final Lab.Value MAX_SPEED = Lab.CLIENT.def("graviton.well.max_speed", 2.0, 0.5, 4.0, 0.05);
   public static final Lab.Value CHAIN_BONUS = Lab.CLIENT.def("graviton.well.chain_bonus", 0.25, 0.0, 1.0, 0.05);
   public static final Lab.Value CHAIN_CAP = Lab.CLIENT.def("graviton.well.chain_cap", 3.0, 0.0, 8.0, 1.0);
   public static final Lab.Value BRAKE = Lab.CLIENT.def("graviton.well.brake", 0.22, 0.05, 0.8, 0.01);
   public static final Lab.Value ARRIVE = Lab.CLIENT.def("graviton.well.arrive", 1.2, 0.3, 4.0, 0.1);
   public static final Lab.Value ARC = Lab.CLIENT.def("graviton.well.arc", 0.12, 0.0, 0.5, 0.01);
   public static final Lab.Value KEEP = Lab.CLIENT.def("graviton.well.keep", 0.99, 0.9, 1.0, 0.005);
   public static final Lab.Value PERP_DAMP = Lab.CLIENT.def("graviton.well.perp_damp", 0.15, 0.0, 0.6, 0.01);
   public static final Lab.Value HOP = Lab.CLIENT.def("graviton.well.hop", 0.45, 0.0, 1.0, 0.05);
   public static final Lab.Value HOVER = Lab.CLIENT.def("graviton.well.hover_spring", 0.3, 0.05, 1.0, 0.05);
   private static final double AIR_FRICTION = 0.91;
   private static final double AIR_DRAG = 0.98;
   private static final double WATER_DRAG = 0.8;
   public static final double WALL_OFF = 0.45;
   public static final double BODY_HEIGHT = 1.85;

   private GravityWellMotion() {
   }

   public static Optional<GravityWellMotion.Target> target(Player body) {
      AimUtil.Stand stand = AimUtil.standPoint(body, RANGE.get(), 0.45);
      return Optional.of(new GravityWellMotion.Target(stand.point(), stand.face(), stand.surface()));
   }

   public static boolean serverLive(LivingEntity body) {
      AttributeInstance gravity = body.getAttribute(Attributes.GRAVITY);
      return gravity != null && gravity.hasModifier(GRAVITY_ID);
   }

   public static double maxSpeed(int chain) {
      return MAX_SPEED.get() + CHAIN_BONUS.get() * (double)Math.min(chain, CHAIN_CAP.getI());
   }

   public static boolean arrived(Vec3 feet, Vec3 well) {
      return feet.distanceTo(well) < ARRIVE.get();
   }

   public static Vec3 step(Vec3 feet, Vec3 v, Vec3 well, boolean onGround, boolean inWater, boolean arrived, int chain) {
      Vec3 d = well.subtract(feet);
      double dist = d.length();
      if (!arrived && !(dist < 1.0E-4)) {
         if (inWater) {
            v = v.scale(1.25);
         } else if (!onGround) {
            v = new Vec3(v.x / 0.91, v.y / 0.98, v.z / 0.91);
         }

         v = v.scale(KEEP.get());
         Vec3 aim = well.add(0.0, ARC.get() * dist, 0.0).subtract(feet).normalize();
         v = v.add(aim.scale(PULL.get()));
         Vec3 dir = d.scale(1.0 / dist);
         double brake = BRAKE.get();
         double max = maxSpeed(chain);
         double zone = max * max / (2.0 * brake) + ARRIVE.get();
         if (dist < zone) {
            Vec3 perp = v.subtract(dir.scale(v.dot(dir)));
            v = v.subtract(perp.scale(PERP_DAMP.get() * (1.0 - dist / zone)));
         }

         double allowed = Math.sqrt(2.0 * brake * Math.max(0.0, dist - ARRIVE.get() * 0.5)) + 0.1;
         double cap = Math.min(max, allowed);
         double speed = v.length();
         return speed > cap ? v.scale(cap / speed) : v;
      } else {
         return d.scale(HOVER.get());
      }
   }

   public static record Target(Vec3 point, Direction face, boolean surface) {
   }
}
