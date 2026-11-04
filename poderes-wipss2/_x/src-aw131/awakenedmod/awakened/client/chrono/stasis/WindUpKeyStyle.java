package com.awakenedmod.awakened.client.chrono.stasis;

import com.awakenedmod.awakened.content.chronokinetic.ChronoSounds;
import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class WindUpKeyStyle extends StasisStyle {
   private static final String KEY = "chrono.stasis.key.";
   static final Lab.Value SIZE = Lab.CLIENT.def("chrono.stasis.key.size", 1.2, 0.2, 4.0, 0.05);
   static final Lab.Value SIZE_MIN = Lab.CLIENT.def("chrono.stasis.key.size_min", 0.45, 0.1, 2.0, 0.05);
   static final Lab.Value SIZE_MAX = Lab.CLIENT.def("chrono.stasis.key.size_max", 1.2, 0.2, 3.0, 0.05);
   static final Lab.Value UPRIGHT = Lab.CLIENT.def("chrono.stasis.key.upright", 2.0, 0.5, 5.0, 0.1);
   static final Lab.Value BACK_Y = Lab.CLIENT.def("chrono.stasis.key.back_y", 0.62, 0.0, 1.0, 0.01);
   static final Lab.Value BACK_Z = Lab.CLIENT.def("chrono.stasis.key.back_z", 0.2, 0.0, 1.0, 0.01);
   static final Lab.Value BACK_TILT = Lab.CLIENT.def("chrono.stasis.key.back_tilt", 15.0, -45.0, 60.0, 1.0);
   static final Lab.Value TOP_Y = Lab.CLIENT.def("chrono.stasis.key.top_y", 0.85, 0.0, 1.2, 0.01);
   static final Lab.Value TOP_Z = Lab.CLIENT.def("chrono.stasis.key.top_z", 0.1, -0.5, 0.5, 0.01);
   static final Lab.Value TOP_TILT = Lab.CLIENT.def("chrono.stasis.key.top_tilt", 20.0, -45.0, 60.0, 1.0);
   static final Lab.Value EMBED = Lab.CLIENT.def("chrono.stasis.key.embed", 0.12, 0.0, 0.5, 0.01);
   static final Lab.Value SHAFT = Lab.CLIENT.def("chrono.stasis.key.shaft", 0.42, 0.1, 0.8, 0.01);
   static final Lab.Value THICK = Lab.CLIENT.def("chrono.stasis.key.thick", 0.06, 0.02, 0.2, 0.005);
   static final Lab.Value LOOP_W = Lab.CLIENT.def("chrono.stasis.key.loop_w", 0.3, 0.1, 0.6, 0.01);
   static final Lab.Value BOW_H = Lab.CLIENT.def("chrono.stasis.key.bow_h", 0.5, 0.2, 0.9, 0.01);
   static final Lab.Value BAR = Lab.CLIENT.def("chrono.stasis.key.bar", 0.065, 0.02, 0.2, 0.005);
   static final Lab.Value ROLL = Lab.CLIENT.def("chrono.stasis.key.roll", 0.0, 0.0, 180.0, 5.0);
   static final Lab.Value APPEAR = Lab.CLIENT.def("chrono.stasis.key.appear", 2.0, 0.0, 10.0, 1.0);
   static final Lab.Value WIND_STEPS = Lab.CLIENT.def("chrono.stasis.key.wind_steps", 4.0, 0.0, 8.0, 1.0);
   static final Lab.Value WIND_GAP = Lab.CLIENT.def("chrono.stasis.key.wind_gap", 1.5999999F, 0.5, 5.0, 0.1);
   static final Lab.Value WIND_DEG = Lab.CLIENT.def("chrono.stasis.key.wind_deg", 90.0, 0.0, 360.0, 5.0);
   static final Lab.Value TURN_TICKS = Lab.CLIENT.def("chrono.stasis.key.turn_ticks", 3.0, 1.0, 10.0, 0.5);
   static final Lab.Value TURN_LEAD = Lab.CLIENT.def("chrono.stasis.key.turn_lead", 1.5, 0.0, 10.0, 0.5);
   static final Lab.Value OVERSHOOT = Lab.CLIENT.def("chrono.stasis.key.overshoot", 1.2, 0.0, 4.0, 0.1);
   static final Lab.Value CRANK_TICKS = Lab.CLIENT.def("chrono.stasis.key.crank_ticks", 3.0, 1.0, 10.0, 1.0);
   static final Lab.Value OUT = Lab.CLIENT.def("chrono.stasis.key.out", 0.12, 0.0, 1.0, 0.01);
   static final Lab.Value UP = Lab.CLIENT.def("chrono.stasis.key.up", 0.1, 0.0, 1.0, 0.01);
   static final Lab.Value BACK = Lab.CLIENT.def("chrono.stasis.key.back", 0.03, -0.5, 0.5, 0.01);
   static final Lab.Value GRAVITY = Lab.CLIENT.def("chrono.stasis.key.gravity", 0.06, 0.01, 0.3, 0.005);
   static final Lab.Value TUMBLES = Lab.CLIENT.def("chrono.stasis.key.tumbles", 1.0, 0.0, 4.0, 1.0);
   static final Lab.Value LIE_TICKS = Lab.CLIENT.def("chrono.stasis.key.lie_ticks", 8.0, 0.0, 40.0, 1.0);
   static final Lab.Value SHRINK_TICKS = Lab.CLIENT.def("chrono.stasis.key.shrink_ticks", 4.0, 1.0, 20.0, 1.0);
   static final Lab.Value CHIPS = Lab.CLIENT.def("chrono.stasis.key.chips", 6.0, 0.0, 30.0, 1.0);
   private static final int FLY_MAX = 20;
   private static final BlockState IRON = Blocks.IRON_BLOCK.defaultBlockState();
   private static final BlockState GOLD = Blocks.GOLD_BLOCK.defaultBlockState();

   WindUpKeyStyle() {
   }

   @Override
   public int releaseTicks() {
      return CRANK_TICKS.getI() + 20 + LIE_TICKS.getI() + SHRINK_TICKS.getI();
   }

   @Override
   public void draw(StasisProp prop, PropCanvas canvas) {
      WindUpKeyStyle.Shape s = WindUpKeyStyle.Shape.of(prop.width(), prop.height());
      float sr = prop.sinceRelease();
      if (!prop.released() || !(sr >= s.gone())) {
         float heldAge = prop.released() ? prop.age() - sr : prop.age();
         float spin = s.spin(heldAge, prop.hold(), Math.max(0.0F, sr));
         float fly = prop.released() ? Math.max(0.0F, sr - s.crank) : 0.0F;
         float t = Math.min(fly, s.landT);
         float f = s.landT > 0.0F ? t / s.landT : 1.0F;
         PoseStack pose = canvas.pose();
         pose.pushPose();
         canvas.bodyFrame(prop.bodyYaw());
         pose.translate(s.cx0, s.cy0 + s.vy * t - 0.5F * s.g * t * t, s.cz0 + s.vz * t);
         pose.mulPose(new Quaternionf().rotationX(-s.phi + s.tumble * f));
         if (fly > 0.0F) {
            spin = Mth.lerp(f, spin, (float)Math.round(spin / 180.0F) * 180.0F);
         }

         pose.mulPose(new Quaternionf().rotationY(spin * (float) (Math.PI / 180.0)));
         float shrink = 1.0F - Mth.clamp((fly - s.landT - s.lie) / s.shrink, 0.0F, 1.0F);
         if (shrink < 1.0F) {
            pose.scale(shrink, shrink, shrink);
         }

         pose.translate(0.0F, -s.mid, 0.0F);
         float appear = s.appear > 0.0F ? easeOut(heldAge / s.appear) : 1.0F;
         if (appear < 1.0F) {
            pose.scale(appear, appear, appear);
         }

         drawKey(canvas, s);
         pose.popPose();
      }
   }

   private static void drawKey(PropCanvas c, WindUpKeyStyle.Shape s) {
      c.column(IRON, 0.0F, 0.0F, s.t, s.t, -s.embed, s.shaftTop);
      c.column(GOLD, 0.0F, 0.0F, s.t * 2.0F, s.t * 2.0F, 0.0F, s.L * 0.05F);
      c.column(GOLD, 0.0F, 0.0F, s.t * 1.6F, s.t * 1.6F, s.shaftTop - s.L * 0.08F, s.shaftTop - s.L * 0.04F);
      c.column(GOLD, 0.0F, 0.0F, s.spine, s.depth, s.shaftTop, s.bowTop);
      loop(c, s, 1.0F);
      loop(c, s, -1.0F);
   }

   private static void loop(PropCanvas c, WindUpKeyStyle.Shape s, float side) {
      float b = s.bar;
      float xi = s.spine;
      float xo = s.spine + s.loopW;
      float y0 = s.bowBottom;
      float y1 = s.bowTop;
      bar(c, s, side, xi, xo - b * 1.25F, y1 - b, y1);
      bar(c, s, side, xi, xo - b * 1.25F, y0, y0 + b);
      bar(c, s, side, xo - b, xo, y0 + b * 1.25F, y1 - b * 1.25F);
      bar(c, s, side, xo - b * 1.7F, xo - b * 0.45F, y1 - b * 1.7F, y1 - b * 0.45F);
      bar(c, s, side, xo - b * 1.7F, xo - b * 0.45F, y0 + b * 0.45F, y0 + b * 1.7F);
   }

   private static void bar(PropCanvas c, WindUpKeyStyle.Shape s, float side, float x0, float x1, float y0, float y1) {
      float a = side > 0.0F ? x0 : -x1;
      float z = side > 0.0F ? x1 : -x0;
      c.box(GOLD, a, y0, -s.depth, z, y1, s.depth);
   }

   @Override
   public void cast(StasisCue cue) {
      cue.play(ChronoSounds.stasisKeyWind());
   }

   @Override
   public void tick(StasisCue cue) {
      if (!cue.released()) {
         int mark = cue.secondsLeftMark();
         if (mark > 0 && cue.leads()) {
            cue.play(mark <= 2 ? ChronoSounds.tickEscape() : ChronoSounds.stasisKeyTurn());
         }
      } else {
         WindUpKeyStyle.Shape s = WindUpKeyStyle.Shape.of(cue.width(), cue.height());
         if ((float)cue.sinceRelease() == s.crank + (float)Mth.ceil(s.landT)) {
            chips(cue, s);
         }
      }
   }

   @Override
   public void release(StasisCue cue) {
      cue.play(ChronoSounds.stasisKeyRelease());
   }

   private static void chips(StasisCue cue, WindUpKeyStyle.Shape s) {
      float t = s.landT;
      Vec3 at = cue.local((double)s.cx0, (double)(s.cy0 + s.vy * t - 0.5F * s.g * t * t), (double)(s.cz0 + s.vz * t));
      BlockParticleOption iron = new BlockParticleOption(ParticleTypes.BLOCK, IRON);
      BlockParticleOption gold = new BlockParticleOption(ParticleTypes.BLOCK, GOLD);
      int count = CHIPS.getI();
      RandomSource random = cue.level().random;

      for (int i = 0; i < count; i++) {
         Vec3 off = new Vec3(random.nextGaussian(), 0.0, random.nextGaussian()).scale((double)s.L * 0.2);
         cue.particle((i & 1) == 0 ? gold : iron, at.add(off), new Vec3(off.x * 0.3, 0.08, off.z * 0.3));
      }
   }

   private static final class Shape {
      final float L;
      final float t;
      final float embed;
      final float shaftTop;
      final float spine;
      final float loopW;
      final float bowBottom;
      final float bowTop;
      final float bar;
      final float depth;
      final float mid;
      final float phi;
      final float cx0;
      final float cy0;
      final float cz0;
      final float vy;
      final float vz;
      final float g;
      final float landT;
      final float tumble;
      final float appear;
      final float crank;
      final float lie;
      final float shrink;
      final float turnT;
      final float turnLead;
      final float overshoot;
      final int windSteps;
      final float windGap;
      final float windDeg;
      final float roll;

      private Shape(float width, float height) {
         this.L = StasisStyle.sized(width, WindUpKeyStyle.SIZE.getF(), WindUpKeyStyle.SIZE_MIN.getF(), WindUpKeyStyle.SIZE_MAX.getF());
         this.t = this.L * WindUpKeyStyle.THICK.getF() * 0.5F;
         this.embed = this.L * WindUpKeyStyle.EMBED.getF();
         this.shaftTop = this.L * WindUpKeyStyle.SHAFT.getF();
         this.spine = this.t * 1.6F;
         this.loopW = this.L * WindUpKeyStyle.LOOP_W.getF();
         this.bowTop = this.shaftTop + this.L * WindUpKeyStyle.BOW_H.getF();
         this.bowBottom = this.shaftTop + this.L * WindUpKeyStyle.BOW_H.getF() * 0.12F;
         this.bar = this.L * WindUpKeyStyle.BAR.getF();
         this.depth = Math.max(this.t, this.bar * 0.5F);
         this.mid = this.bowTop * 0.5F;
         boolean upright = height >= width * WindUpKeyStyle.UPRIGHT.getF();
         float my;
         float mz;
         if (upright) {
            this.phi = (90.0F - WindUpKeyStyle.BACK_TILT.getF()) * (float) (Math.PI / 180.0);
            my = height * WindUpKeyStyle.BACK_Y.getF();
            mz = -width * WindUpKeyStyle.BACK_Z.getF();
         } else {
            this.phi = WindUpKeyStyle.TOP_TILT.getF() * (float) (Math.PI / 180.0);
            my = height * WindUpKeyStyle.TOP_Y.getF();
            mz = -width * WindUpKeyStyle.TOP_Z.getF();
         }

         float mx = 0.0F;
         float ay = Mth.cos(this.phi);
         float az = -Mth.sin(this.phi);
         this.cx0 = mx;
         this.cy0 = my + ay * this.mid;
         this.cz0 = mz + az * this.mid;
         float out = WindUpKeyStyle.OUT.getF();
         this.vy = ay * out + WindUpKeyStyle.UP.getF();
         this.vz = az * out - WindUpKeyStyle.BACK.getF();
         this.g = WindUpKeyStyle.GRAVITY.getF();
         float rest = this.depth;
         float drop = Math.max(0.0F, this.cy0 - rest);
         this.landT = Mth.clamp((this.vy + Mth.sqrt(this.vy * this.vy + 2.0F * this.g * drop)) / this.g, 1.0F, 20.0F);
         this.tumble = this.phi - (90.0F + 360.0F * (float)WindUpKeyStyle.TUMBLES.getI()) * (float) (Math.PI / 180.0);
         this.appear = WindUpKeyStyle.APPEAR.getF();
         this.crank = (float)Math.max(1, WindUpKeyStyle.CRANK_TICKS.getI());
         this.lie = WindUpKeyStyle.LIE_TICKS.getF();
         this.shrink = Math.max(1.0F, WindUpKeyStyle.SHRINK_TICKS.getF());
         this.turnT = Math.max(0.5F, WindUpKeyStyle.TURN_TICKS.getF());
         this.turnLead = WindUpKeyStyle.TURN_LEAD.getF();
         this.overshoot = WindUpKeyStyle.OVERSHOOT.getF();
         this.windSteps = WindUpKeyStyle.WIND_STEPS.getI();
         this.windGap = WindUpKeyStyle.WIND_GAP.getF();
         this.windDeg = WindUpKeyStyle.WIND_DEG.getF();
         this.roll = WindUpKeyStyle.ROLL.getF();
      }

      static WindUpKeyStyle.Shape of(float width, float height) {
         return new WindUpKeyStyle.Shape(width, height);
      }

      float gone() {
         return this.crank + this.landT + this.lie + this.shrink;
      }

      float spin(float heldAge, int hold, float sinceRelease) {
         float wind = 0.0F;

         for (int k = 0; k < this.windSteps; k++) {
            wind += StasisStyle.easeOut(heldAge - (float)k * this.windGap);
         }

         float quarters = 0.0F;

         for (int m = 1; hold - 20 * m > 0; m++) {
            float x = (heldAge - ((float)(hold - 20 * m) - this.turnLead)) / this.turnT;
            if (x > 0.0F) {
               quarters += x >= 1.0F ? 1.0F : backOut(x, this.overshoot);
            }
         }

         float crankTurn = StasisStyle.easeIn(sinceRelease / this.crank);
         return this.roll - (this.windSteps > 0 ? wind / (float)this.windSteps * this.windDeg : 0.0F) + quarters * 90.0F + crankTurn * 360.0F;
      }

      private static float backOut(float x, float c) {
         float u = x - 1.0F;
         return 1.0F + (c + 1.0F) * u * u * u + c * u * u;
      }
   }
}
