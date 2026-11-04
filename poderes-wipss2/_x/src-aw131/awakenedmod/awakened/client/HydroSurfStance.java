package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.Lab;
import com.zigythebird.playeranimcore.animation.layered.modifier.AdjustmentModifier.PartModifier;
import com.zigythebird.playeranimcore.math.Vec3f;
import java.util.Optional;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;

final class HydroSurfStance {
   static final Lab.Value TURN = def("stance_turn", 80.0, -100.0, 100.0);
   static final Lab.Value WIDTH = def("stance_width", 24.0, 0.0, 60.0);
   static final Lab.Value CROUCH = def("crouch", 12.0, 0.0, 60.0);
   static final Lab.Value HIP_PITCH = def("hip_pitch", 8.0, -20.0, 70.0);
   static final Lab.Value TORSO_TWIST = def("torso_twist", 15.0, -45.0, 45.0);
   static final Lab.Value HEAD_LOOK = def("head_look", 70.0, 0.0, 110.0);
   static final Lab.Value HEAD_PITCH = def("head_pitch", 0.0, -40.0, 40.0);
   static final Lab.Value LEAD_ARM_PITCH = def("lead_arm_pitch", 78.0, 0.0, 180.0);
   static final Lab.Value LEAD_ARM_YAW = def("lead_arm_yaw", 38.0, -90.0, 90.0);
   static final Lab.Value TRAIL_ARM_PITCH = def("trail_arm_pitch", 62.0, 0.0, 180.0);
   static final Lab.Value TRAIL_ARM_YAW = def("trail_arm_yaw", 18.0, -90.0, 90.0);
   static final Lab.Value CARVE_HINGE = def("carve_hinge", 20.0, 0.0, 70.0);
   static final Lab.Value CARVE_CROUCH = def("carve_crouch", 6.0, 0.0, 40.0);
   static final Lab.Value CARVE_ARMS = def("carve_arms", 25.0, 0.0, 60.0);
   private static final float LEG = 12.0F;

   private static Lab.Value def(String key, double fallback, double min, double max) {
      return Lab.CLIENT.def("hydro.surf.pose_" + key, fallback, min, max, 1.0);
   }

   private HydroSurfStance() {
   }

   static Optional<PartModifier> bone(String bone, float carve, float headYaw, float headPitch) {
      HydroSurfStance.Angles live = HydroSurfStance.Angles.of(false, carve);
      HydroSurfStance.Angles base = HydroSurfStance.Angles.DEFAULTS;

      return switch (bone) {
         case "body" -> Optional.of(
         new PartModifier(
            new Vec3f((live.hip - base.hip) * (float) (Math.PI / 180.0), (live.rootYaw() - base.rootYaw()) * (float) (Math.PI / 180.0), 0.0F),
            new Vec3f(0.0F, base.drop() - live.drop(), 0.0F)
         )
      );
         case "right_leg", "left_leg" -> offset(live.leg(bone.startsWith("right")), base.leg(bone.startsWith("right")));
         case "right_arm", "left_arm" -> offset(live.arm(bone.startsWith("right")), base.arm(bone.startsWith("right")));
         case "head" -> {
            float yaw = headYaw * (float) (Math.PI / 180.0);
            float pitch = headPitch * (float) (Math.PI / 180.0);
            yield offset(live.head(yaw, pitch), new Vec3f(pitch, yaw, 0.0F));
         }
         default -> Optional.empty();
      };
   }

   private static Optional<PartModifier> offset(Vec3f live, Vec3f base) {
      return Optional.of(new PartModifier(new Vec3f(wrap(live.x() - base.x()), wrap(live.y() - base.y()), wrap(live.z() - base.z())), Vec3f.ZERO));
   }

   private static float wrap(float rad) {
      return Mth.wrapDegrees(rad * (180.0F / (float)Math.PI)) * (float) (Math.PI / 180.0);
   }

   private static Vec3f euler(Matrix3f m) {
      float y = (float)Math.asin((double)Mth.clamp(-m.m02(), -1.0F, 1.0F));
      float x = (float)Math.atan2((double)m.m12(), (double)m.m22());
      float z = (float)Math.atan2((double)m.m01(), (double)m.m00());
      return new Vec3f(x, y, z);
   }

   private static record Angles(
      float side,
      float turn,
      float width,
      float crouch,
      float hip,
      float twist,
      float look,
      float headPitch,
      float leadPitch,
      float leadYaw,
      float trailPitch,
      float trailYaw
   ) {
      static final HydroSurfStance.Angles DEFAULTS = of(true, 0.0F);

      static HydroSurfStance.Angles of(boolean defaults, float carve) {
         float turn = v(HydroSurfStance.TURN, defaults);
         float side = turn >= 0.0F ? 1.0F : -1.0F;
         float toes = carve * side;
         float arms = v(HydroSurfStance.CARVE_ARMS, defaults);
         float counter = -toes * arms;
         float lift = Math.abs(toes) * arms * 0.5F;
         return new HydroSurfStance.Angles(
            side,
            turn,
            v(HydroSurfStance.WIDTH, defaults),
            v(HydroSurfStance.CROUCH, defaults) + Math.abs(toes) * v(HydroSurfStance.CARVE_CROUCH, defaults),
            v(HydroSurfStance.HIP_PITCH, defaults) + toes * (toes > 0.0F ? 0.5F : 1.0F) * v(HydroSurfStance.CARVE_HINGE, defaults),
            v(HydroSurfStance.TORSO_TWIST, defaults),
            v(HydroSurfStance.HEAD_LOOK, defaults),
            v(HydroSurfStance.HEAD_PITCH, defaults),
            v(HydroSurfStance.LEAD_ARM_PITCH, defaults) + lift,
            v(HydroSurfStance.LEAD_ARM_YAW, defaults) + counter,
            v(HydroSurfStance.TRAIL_ARM_PITCH, defaults) + lift,
            v(HydroSurfStance.TRAIL_ARM_YAW, defaults) + counter
         );
      }

      private static float v(Lab.Value value, boolean defaults) {
         return defaults ? (float)value.defaultValue() : value.getF();
      }

      float rootYaw() {
         return this.turn - this.side * this.twist;
      }

      float drop() {
         return 12.0F * (1.0F - Mth.cos(this.width * (float) (Math.PI / 180.0)) * Mth.cos(this.crouch * (float) (Math.PI / 180.0)));
      }

      private Matrix3f unleaned() {
         return new Matrix3f().rotateX(-this.hip * (float) (Math.PI / 180.0));
      }

      Vec3f leg(boolean right) {
         return HydroSurfStance.euler(
            this.unleaned()
               .rotateY(this.side * this.twist * (float) (Math.PI / 180.0))
               .rotateZ((right ? this.width : -this.width) * (float) (Math.PI / 180.0))
               .rotateX(-this.crouch * (float) (Math.PI / 180.0))
         );
      }

      Vec3f arm(boolean right) {
         boolean lead = right == this.side < 0.0F;
         float pitch = lead ? this.leadPitch : this.trailPitch;
         float yaw = lead ? this.leadYaw : this.trailYaw;
         return HydroSurfStance.euler(
            this.unleaned().rotateY((right ? -yaw : yaw) * (float) (Math.PI / 180.0)).rotateZ((right ? pitch : -pitch) * (float) (Math.PI / 180.0))
         );
      }

      Vec3f head(float yaw, float pitch) {
         return HydroSurfStance.euler(
            this.unleaned().rotateY(yaw - this.side * this.look * (float) (Math.PI / 180.0)).rotateX(pitch + this.headPitch * (float) (Math.PI / 180.0))
         );
      }
   }
}
