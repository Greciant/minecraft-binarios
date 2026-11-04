package com.awakenedmod.awakened.client.render.limitless.blue;

import com.awakenedmod.awakened.client.render.FxLayer;
import com.awakenedmod.awakened.client.render.SteeredParticle;
import com.awakenedmod.awakened.client.render.limitless.Ease;
import com.awakenedmod.awakened.client.render.limitless.LimitlessComfort;
import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.LimitlessLook;
import com.awakenedmod.awakened.client.render.limitless.LimitlessMesh;
import com.awakenedmod.awakened.client.render.limitless.LimitlessShapes;
import com.awakenedmod.awakened.client.render.limitless.SpaceBend;
import com.awakenedmod.awakened.core.Lab;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class BlueLapse implements LimitlessLook<BlueCue> {
   private static final String K = "limitless.blue.";
   private static final Lab.Value CORE = Lab.CLIENT.def("limitless.blue.core", 0.5, 0.1, 1.5, 0.01);
   private static final Lab.Value GROW = Lab.CLIENT.def("limitless.blue.grow", 0.25, 0.0, 1.0, 0.01);
   private static final Lab.Value BLOOM = Lab.CLIENT.def("limitless.blue.bloom", 1.0, 0.0, 3.0, 0.05);
   private static final Lab.Value HAZE = Lab.CLIENT.def("limitless.blue.haze", 2.6, 0.0, 6.0, 0.05);
   private static final Lab.Value HAZE_ALPHA = Lab.CLIENT.def("limitless.blue.haze_alpha", 150.0, 0.0, 255.0, 1.0);
   private static final Lab.Value RIM_ALPHA = Lab.CLIENT.def("limitless.blue.rim_alpha", 235.0, 0.0, 255.0, 1.0);
   private static final Lab.Value HEART_ALPHA = Lab.CLIENT.def("limitless.blue.heart_alpha", 245.0, 0.0, 255.0, 1.0);
   private static final Lab.Value SHELLS = Lab.CLIENT.def("limitless.blue.shells", 3.0, 0.0, 6.0, 1.0);
   private static final Lab.Value SHELL_R = Lab.CLIENT.def("limitless.blue.shell_reach", 3.2, 0.5, 8.0, 0.05);
   private static final Lab.Value SHELL_ALPHA = Lab.CLIENT.def("limitless.blue.shell_alpha", 90.0, 0.0, 255.0, 1.0);
   private static final Lab.Value SHELL_PERIOD = Lab.CLIENT.def("limitless.blue.shell_period", 12.0, 4.0, 40.0, 1.0);
   private static final Lab.Value STROKES = Lab.CLIENT.def("limitless.blue.strokes", 22.0, 0.0, 60.0, 1.0);
   private static final Lab.Value STROKE_R = Lab.CLIENT.def("limitless.blue.stroke_reach", 4.2, 0.5, 10.0, 0.05);
   private static final Lab.Value STROKE_W = Lab.CLIENT.def("limitless.blue.stroke_width", 0.15, 0.02, 0.6, 0.005);
   private static final Lab.Value STROKE_LEN = Lab.CLIENT.def("limitless.blue.stroke_length", 0.55, 0.1, 1.0, 0.01);
   private static final Lab.Value STROKE_BODY = Lab.CLIENT.def("limitless.blue.stroke_body", 190.0, 0.0, 255.0, 1.0);
   private static final Lab.Value STROKE_GLOW = Lab.CLIENT.def("limitless.blue.stroke_glow", 225.0, 0.0, 255.0, 1.0);
   private static final Lab.Value LINES = Lab.CLIENT.def("limitless.blue.lines", 60.0, 0.0, 200.0, 1.0);
   private static final Lab.Value LINE_R = Lab.CLIENT.def("limitless.blue.line_reach", 6.5, 1.0, 14.0, 0.1);
   private static final Lab.Value LINE_W = Lab.CLIENT.def("limitless.blue.line_width", 0.045, 0.005, 0.2, 0.005);
   private static final Lab.Value LINE_ALPHA = Lab.CLIENT.def("limitless.blue.line_alpha", 200.0, 0.0, 255.0, 1.0);
   private static final Lab.Value TURNS = Lab.CLIENT.def("limitless.blue.turns", 0.7, 0.0, 3.0, 0.05);
   private static final Lab.Value ARCS = Lab.CLIENT.def("limitless.blue.arcs", 5.0, 0.0, 10.0, 1.0);
   private static final Lab.Value DEBRIS = Lab.CLIENT.def("limitless.blue.debris", 34.0, 0.0, 100.0, 1.0);
   private static final Lab.Value DEBRIS_SIZE = Lab.CLIENT.def("limitless.blue.debris_size", 0.3, 0.02, 1.0, 0.01);
   private static final Lab.Value MASS = Lab.CLIENT.def("limitless.blue.mass", 22.0, 0.0, 60.0, 1.0);
   private static final Lab.Value CAVE = Lab.CLIENT.def("limitless.blue.cave", 2.0, 0.0, 4.0, 1.0);
   private static final Lab.Value CAVE_R = Lab.CLIENT.def("limitless.blue.cave_reach", 7.5, 2.0, 14.0, 0.1);
   private static final Lab.Value CAVE_ALPHA = Lab.CLIENT.def("limitless.blue.cave_alpha", 60.0, 0.0, 255.0, 1.0);
   private static final Lab.Value CAVE_PERIOD = Lab.CLIENT.def("limitless.blue.cave_period", 18.0, 6.0, 60.0, 1.0);
   private static final Lab.Value GRIT = Lab.CLIENT.def("limitless.blue.grit", 6.0, 0.0, 30.0, 1.0);
   private static final Lab.Value GRIT_R = Lab.CLIENT.def("limitless.blue.grit_reach", 7.0, 1.0, 14.0, 0.1);
   private static final Lab.Value TORN = Lab.CLIENT.def("limitless.blue.torn", 16.0, 0.0, 60.0, 1.0);
   private static final int FLOOR_SCAN = 7;
   private static final int SAMPLES = 14;
   private static final float TWO_PI = (float) (Math.PI * 2);
   private static final int[] RUBBLE = new int[]{
      LimitlessMesh.argb(255, 118, 116, 122),
      LimitlessMesh.argb(255, 98, 74, 52),
      LimitlessMesh.argb(255, 136, 130, 120),
      LimitlessMesh.argb(255, 120, 86, 52),
      LimitlessMesh.argb(255, 64, 104, 48),
      LimitlessMesh.argb(255, 84, 84, 92)
   };
   private static final int ORB_LIT = LimitlessMesh.argb(255, 70, 160, 255);
   private final Vector3f[] pts = new Vector3f[14];
   private final Vector3f[] mid = new Vector3f[14];
   private final float[] widths = new float[14];
   private final float[] base = new float[14];
   private final int[] colors = new int[14];
   private float squeeze;

   public BlueLapse() {
      for (int i = 0; i < 14; i++) {
         this.pts[i] = new Vector3f();
         this.mid[i] = new Vector3f();
      }
   }

   public void draw(LimitlessFrame f, BlueCue s, float age) {
      float form = Ease.smooth(age / 8.0F);
      float release = s.release();
      float gone = age <= release ? 1.0F : 1.0F - (age - release) / BlueEnding.fadeTicks(s);
      if (!(gone <= 0.0F)) {
         float presence = form * Ease.smooth(gone);
         float hold = Ease.ramp(age, 8.0F, 32.0F);
         float size = (float)Math.pow((double)form, 0.6)
            * (float)Math.pow((double)Math.max(0.0F, gone), 0.7)
            * (1.0F + GROW.getF() * hold)
            * (1.0F + 0.04F * (float)Math.sin((double)(age * 0.6F)));
         Vector3f c = f.rel(s.orb());
         long seed = s.seed();
         this.squeeze = BlueEnding.squeeze(s, age);
         float core = CORE.getF() * size;
         float lit = 0.22F + 0.78F * (float)f.level.getMaxLocalRawBrightness(BlockPos.containing(s.orb())) / 15.0F;
         LimitlessMesh paint = f.begin(FxLayer.PAINT);
         float hazeR = HAZE.getF() * size;
         float hazeA = HAZE_ALPHA.getF() / 255.0F * presence;
         paint.disc(c, hazeR, 32, LimitlessMesh.fade(LimitlessMesh.argb(255, 14, 60, 200), hazeA * 0.3F), LimitlessMesh.argb(0, 10, 40, 170));
         paint.disc(c, hazeR * 0.6F, 28, LimitlessMesh.fade(LimitlessMesh.argb(255, 16, 78, 220), hazeA * 0.4F), LimitlessMesh.argb(0, 14, 60, 200));
         paint.disc(c, hazeR * 0.32F, 24, LimitlessMesh.fade(LimitlessMesh.argb(255, 20, 95, 235), hazeA * 0.55F), LimitlessMesh.argb(0, 20, 80, 220));
         this.strokes(paint, c, age, seed, presence, core, true);
         this.lines(paint, c, age, seed, presence, core, true);
         BlueLapse.Floor floor = floor(f.level, s.orb());
         this.cave(paint, true, c, core, age, seed, presence * LimitlessComfort.distortion(f.mc));
         this.debris(paint, c, age, seed, presence, size, lit, false);
         this.torn(paint, c, s.orb(), floor, age, seed, presence, lit, false);
         mass(paint, c, core, age, seed, presence, lit, false);
         int deep = LimitlessMesh.argb(Math.round(250.0F * presence), 16, 70 + Math.round(20.0F * (1.0F - hold)), 225);
         int face = LimitlessMesh.argb(Math.round(250.0F * presence), 70, 185, 255);
         paint.sphere(c, core, 12, 20, (n, facing) -> LimitlessMesh.mix(face, deep, (float)Math.pow((double)Math.max(0.0F, facing), 0.9)));
         this.debris(paint, c, age, seed, presence, size, lit, true);
         this.torn(paint, c, s.orb(), floor, age, seed, presence, lit, true);
         mass(paint, c, core, age, seed, presence, lit, true);
         f.end(FxLayer.PAINT);
         LimitlessMesh light = f.begin(FxLayer.LIGHT);
         float bloom = BLOOM.getF() * presence;
         light.disc(c, core * 6.5F, 36, LimitlessMesh.argb(Math.round(48.0F * bloom), 10, 70, 255), 0);
         light.disc(c, core * 3.6F, 32, LimitlessMesh.argb(Math.round(90.0F * bloom), 20, 120, 255), 0);
         light.disc(c, core * 2.0F, 28, LimitlessMesh.argb(Math.round(145.0F * bloom), 40, 150, 255), 0);
         this.shells(light, c, core, age, seed, presence * LimitlessComfort.distortion(f.mc));
         this.cave(light, false, c, core, age, seed, presence * LimitlessComfort.distortion(f.mc));
         float rimA = RIM_ALPHA.getF() * presence;
         light.sphere(c, core * 1.06F, 12, 22, (n, facing) -> {
            float edge = 1.0F - Math.max(0.0F, facing);
            return LimitlessMesh.argb(Math.round(rimA * edge * edge * edge), 150, 228, 255);
         });
         float heartA = HEART_ALPHA.getF() * presence;
         light.disc(c, core * 1.0F, 24, LimitlessMesh.argb(Math.round(heartA), 200, 240, 255), LimitlessMesh.argb(0, 60, 170, 255));
         light.disc(c, core * 0.42F, 16, LimitlessMesh.argb(Math.round(heartA), 240, 250, 255), LimitlessMesh.argb(0, 160, 225, 255));
         this.arcs(light, c, core, age, seed, presence);
         this.strokes(light, c, age, seed, presence, core, false);
         this.lines(light, c, age, seed, presence, core, false);
         f.end(FxLayer.LIGHT);
      }
   }

   private void shells(LimitlessMesh m, Vector3f c, float core, float age, long seed, float presence) {
      int n = SHELLS.getI();
      float period = SHELL_PERIOD.getF();
      float reach = this.in(SHELL_R.getF(), 0.6F);
      float alpha = SHELL_ALPHA.getF() * presence;

      for (int k = 0; k < n; k++) {
         float ph = Ease.frac((age + (float)k * period / (float)n) / period);
         float r = core * 1.3F + (reach - core * 1.3F) * (float)Math.pow((double)(1.0F - ph), 1.6);
         float a = alpha * (float)Math.sin(Math.PI * Math.pow((double)ph, 0.7));
         if (!(a < 1.0F)) {
            SpaceBend frame = SpaceBend.random(
               seed * 59L + (long)(k * 7) + (long)Math.floor((double)((age + (float)k * period / (float)n) / period)), 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F
            );
            Vector3f ax = frame.axis();
            Vector3f u = frame.start();
            Vector3f v = new Vector3f(ax).cross(u);
            float twist = 5.0F + 4.0F * ph;
            float spin = age * 0.35F * (k % 2 == 0 ? 1.0F : -1.0F);
            m.sphere(c, r, 12, 26, (nn, facing) -> {
               float body = 0.35F + 0.65F * (float)Math.sqrt((double)Math.max(0.0F, facing));
               return LimitlessMesh.argb(Math.round(a * band(nn, ax, u, v, twist, spin) * body), 60, 175, 255);
            });
         }
      }
   }

   private float in(float reach, float by) {
      return reach * (1.0F - by * this.squeeze);
   }

   static float band(Vector3f nn, Vector3f ax, Vector3f u, Vector3f v, float twist, float spin) {
      float lat = nn.dot(ax);
      float lon = (float)Math.atan2((double)nn.dot(v), (double)nn.dot(u));
      float band = 0.5F + 0.5F * (float)Math.sin((double)(3.0F * lon + twist * lat + spin));
      return band * band * band;
   }

   private void cave(LimitlessMesh m, boolean body, Vector3f c, float core, float age, long seed, float presence) {
      int n = CAVE.getI();
      float period = CAVE_PERIOD.getF();
      float reach = this.in(CAVE_R.getF(), 0.7F);
      float alpha = CAVE_ALPHA.getF() * presence * Ease.ramp(age, 2.0F, 8.0F) * (body ? 0.6F : 1.0F) * (1.0F + 0.6F * this.squeeze);
      float cam = c.length();

      for (int k = 0; k < n; k++) {
         float cycle = (age + (float)k * period / (float)n) / period;
         float ph = Ease.frac(cycle);
         float r = core * 2.0F + (reach - core * 2.0F) * (float)Math.pow((double)(1.0F - ph), 1.4);
         float a = alpha * (float)Math.sin(Math.PI * Math.pow((double)ph, 0.6)) * Ease.clamp01((cam + 1.0F - r) / 2.0F);
         if (!(a < 1.0F)) {
            SpaceBend frame = SpaceBend.random(seed * 71L + (long)(k * 13) + (long)Math.floor((double)cycle), 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F);
            Vector3f ax = frame.axis();
            Vector3f u = frame.start();
            Vector3f v = new Vector3f(ax).cross(u);
            float twist = 3.0F + 6.0F * ph;
            float spin = age * 0.2F * (k % 2 == 0 ? -1.0F : 1.0F);
            m.sphere(c, r, 14, 32, (nn, facing) -> {
               float face = 0.3F + 0.7F * (float)Math.sqrt((double)Math.max(0.0F, facing));
               int here = Math.round(a * band(nn, ax, u, v, twist, spin) * face);
               return body ? LimitlessMesh.argb(here, 10, 44, 160) : LimitlessMesh.argb(here, 70, 180, 255);
            });
         }
      }
   }

   private void arcs(LimitlessMesh m, Vector3f c, float core, float age, long seed, float presence) {
      int n = ARCS.getI();

      for (int k = 0; k < n; k++) {
         SpaceBend plane = SpaceBend.random(seed * 31L + (long)k, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 1.0F);
         Vector3f ax = plane.axis();
         Vector3f u = plane.start();
         Vector3f v = new Vector3f(ax).cross(u);
         float r = core * (1.25F + 0.12F * (float)k);
         float dir = k % 2 == 0 ? 1.0F : -1.0F;
         float head = age * (0.75F + 0.15F * (float)k) * dir;
         float sweep = 3.0F;

         for (int i = 0; i < 14; i++) {
            float t = (float)i / 13.0F;
            float a = head - (1.0F - t) * sweep * dir;
            float rr = r * (1.0F + 0.25F * (1.0F - t));
            this.pts[i].set(u).mul((float)Math.cos((double)a) * rr).add(new Vector3f(v).mul((float)Math.sin((double)a) * rr)).add(c);
            this.widths[i] = 0.015F + 0.05F * t;
            this.colors[i] = LimitlessMesh.argb(Math.round(235.0F * presence * t * t), 170, 235, 255);
         }

         m.ribbon(this.pts, this.widths, this.colors, 14);
      }
   }

   private void strokes(LimitlessMesh m, Vector3f c, float age, long seed, float presence, float core, boolean body) {
      int n = STROKES.getI();
      float reach = this.in(STROKE_R.getF(), 0.5F);
      float w = STROKE_W.getF() * (body ? 1.0F : 0.45F);
      float len = STROKE_LEN.getF();
      float turns = TURNS.getF();
      float alpha = (body ? STROKE_BODY.getF() : STROKE_GLOW.getF()) / 255.0F * presence;
      Vector3f tmp = new Vector3f();
      int tailCol = body ? LimitlessMesh.argb(255, 8, 40, 170) : LimitlessMesh.argb(255, 30, 120, 255);
      int headCol = body ? LimitlessMesh.argb(255, 25, 110, 250) : LimitlessMesh.argb(255, 175, 235, 255);

      for (int i = 0; i < n; i++) {
         long si = seed * 733L + (long)i;
         SpaceBend path = SpaceBend.random(si, reach * 0.5F, reach, turns * 0.6F, turns * 1.2F, 0.35F, 1.1F);
         float period = 10.0F + 7.0F * SpaceBend.hash(si, 11);
         float u = Ease.frac((age + SpaceBend.hash(si, 12) * period) / period);
         float headS = SpaceBend.fall(u, 1.6F);
         float tailS = Math.max(0.0F, headS - len * (0.7F + 0.5F * SpaceBend.hash(si, 13)));
         float env = Ease.ramp(u, 0.0F, 0.15F) * Math.min(1.0F, (1.0F - headS) / 0.1F);
         if (!(env <= 0.01F) && !(headS - tailS < 0.001F)) {
            for (int j = 0; j < 14; j++) {
               float t = (float)j / 13.0F;
               float sj = tailS + (headS - tailS) * t;
               path.at(sj, tmp);
               outside(tmp, core);
               this.pts[j].set(tmp).add(c);
               this.widths[j] = w * (float)Math.pow(Math.sin(Math.PI * Math.pow((double)t, 0.75)), 0.8) * (0.55F + 0.45F * (1.0F - sj));
               this.colors[j] = LimitlessMesh.fade(LimitlessMesh.mix(tailCol, headCol, 0.35F * t + 0.65F * sj), alpha * env * (0.25F + 0.75F * t));
            }

            m.ribbon(this.pts, this.widths, this.colors, 14);
         }
      }
   }

   private void lines(LimitlessMesh m, Vector3f c, float age, long seed, float presence, float core, boolean body) {
      int n = LINES.getI();
      float reach = this.in(LINE_R.getF(), 0.6F);
      float w = LINE_W.getF() * (body ? 1.5F : 0.7F);
      float alpha = (body ? 150.0F : LINE_ALPHA.getF()) / 255.0F * presence;
      float turns = TURNS.getF() * 0.6F;
      Vector3f tmp = new Vector3f();

      for (int i = 0; i < n; i++) {
         long si = seed * 977L + (long)i;
         SpaceBend path = SpaceBend.random(si, reach * 0.4F, reach, turns * 0.4F, turns * 1.3F, 0.5F, 1.0F);
         float period = 7.0F + 6.0F * SpaceBend.hash(si, 11);
         float u = Ease.frac((age + SpaceBend.hash(si, 12) * period) / period);
         float headS = SpaceBend.fall(u, 1.9F);
         float tailS = SpaceBend.fall(Math.max(0.0F, u - 0.35F), 1.9F);
         float near = Math.min(1.0F, (1.0F - headS) / 0.12F) * Ease.ramp(u, 0.0F, 0.1F);
         if (!(near <= 0.01F) && !(headS - tailS < 0.001F)) {
            for (int j = 0; j < 14; j++) {
               float t = (float)j / 13.0F;
               float sj = tailS + (headS - tailS) * t;
               path.at(sj, tmp);
               outside(tmp, core);
               this.pts[j].set(tmp).add(c);
               this.widths[j] = w * (0.15F + 0.85F * t * t);
               int col = body
                  ? LimitlessMesh.mix(LimitlessMesh.argb(255, 8, 36, 160), LimitlessMesh.argb(255, 24, 100, 245), sj)
                  : LimitlessMesh.mix(LimitlessMesh.argb(255, 40, 130, 255), LimitlessMesh.argb(255, 190, 238, 255), sj);
               this.colors[j] = LimitlessMesh.fade(col, alpha * near * t * t);
            }

            m.ribbon(this.pts, this.widths, this.colors, 14);
         }
      }
   }

   private static int rubble(int col, float lit, float glow) {
      int dim = LimitlessMesh.argb(
         255, Math.round((float)(col >> 16 & 0xFF) * lit), Math.round((float)(col >> 8 & 0xFF) * lit), Math.round((float)(col & 0xFF) * lit)
      );
      return LimitlessMesh.mix(dim, ORB_LIT, glow);
   }

   private static boolean inFront(Vector3f off, Vector3f c) {
      return off.dot(c) < 0.0F;
   }

   private static void outside(Vector3f p, float core) {
      float len = p.length();
      if (len < core * 1.05F && len > 1.0E-4F) {
         p.mul(core * 1.05F / len);
      }
   }

   private void debris(LimitlessMesh m, Vector3f c, float age, long seed, float presence, float size, float lit, boolean front) {
      int n = DEBRIS.getI();
      float ds = DEBRIS_SIZE.getF();
      Vector3f tmp = new Vector3f();
      Quaternionf rot = new Quaternionf();
      float skin = CORE.getF() * size;

      for (int i = 0; i < n; i++) {
         long si = seed * 1291L + (long)i;
         SpaceBend path = SpaceBend.random(si, 2.2F, 6.0F, 0.3F, 0.9F, 0.6F, 1.2F);
         float period = 18.0F + 12.0F * SpaceBend.hash(si, 21);
         float u = Ease.frac((age + SpaceBend.hash(si, 22) * period) / period);
         float sj = SpaceBend.fall(u, 2.2F);
         float scale = Math.min(1.0F, u / 0.1F) * (1.0F - 0.75F * sj) * Math.min(1.0F, (1.0F - sj) / 0.08F) * presence;
         if (!(scale <= 0.03F)) {
            path.at(sj, tmp);
            if (SpaceBend.hash(si, 25) < 0.6F && tmp.y > 0.0F) {
               tmp.y = -tmp.y;
            }

            tmp.mul((0.5F + 0.5F * size) * (1.0F - 0.55F * this.squeeze));
            if (inFront(tmp, c) == front) {
               tmp.add(c);
               float spin = age * (0.2F + 0.5F * SpaceBend.hash(si, 23)) * (1.0F + 2.0F * sj);
               rot.identity().rotateXYZ(spin, spin * 1.3F + (float)i, spin * 0.7F);
               float glow = 0.6F * Ease.clamp01(1.0F - (tmp.distance(c) - skin) / 2.5F);
               LimitlessShapes.chunk(m, tmp, ds * (0.45F + 1.1F * SpaceBend.hash(si, 24)) * scale, rot, si, rubble(RUBBLE[i % RUBBLE.length], lit, glow));
            }
         }
      }
   }

   private static void mass(LimitlessMesh m, Vector3f c, float core, float age, long seed, float presence, float lit, boolean front) {
      int n = MASS.getI();
      float grow = Ease.ramp(age, 8.0F, 14.0F);
      if (n > 0 && !(grow <= 0.0F)) {
         Vector3f tmp = new Vector3f();
         Quaternionf rot = new Quaternionf();
         int shown = Math.round((float)n * grow);

         for (int i = 0; i < shown; i++) {
            long si = seed * 4211L + (long)i;
            SpaceBend orbit = SpaceBend.random(si, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F);
            Vector3f ax = orbit.axis();
            Vector3f u = orbit.start();
            Vector3f v = new Vector3f(ax).cross(u);
            float a = age * (0.25F + 0.25F * SpaceBend.hash(si, 31)) * (SpaceBend.hash(si, 32) < 0.5F ? 1.0F : -1.0F) + SpaceBend.hash(si, 33) * 6.283F;
            float r = core * (1.15F + 0.45F * SpaceBend.hash(si, 34));
            tmp.set(u).mul((float)Math.cos((double)a) * r).add(new Vector3f(v).mul((float)Math.sin((double)a) * r));
            if (inFront(tmp, c) == front) {
               tmp.add(c);
               float spin = age * 0.4F + (float)i;
               rot.identity().rotateXYZ(spin, spin * 0.8F, spin * 1.2F);
               LimitlessShapes.chunk(
                  m,
                  tmp,
                  core * (0.3F + 0.35F * SpaceBend.hash(si, 35)) * presence * Math.min(1.0F, grow * 1.5F),
                  rot,
                  si,
                  rubble(RUBBLE[i * 3 % RUBBLE.length], lit, 0.55F)
               );
            }
         }
      }
   }

   @Nullable
   private static BlueLapse.Floor floor(Level level, Vec3 orb) {
      MutableBlockPos pos = BlockPos.containing(orb).mutable();
      int i = 0;

      while (i <= 7) {
         BlockState st = level.getBlockState(pos);
         if (!st.isAir() && st.getFluidState().isEmpty()) {
            VoxelShape shape = st.getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
               return new BlueLapse.Floor((double)pos.getY() + shape.max(Axis.Y), st, st.getMapColor(level, pos).col);
            }
         }

         i++;
         pos.move(0, -1, 0);
      }

      return null;
   }

   private void torn(LimitlessMesh m, Vector3f c, Vec3 orb, @Nullable BlueLapse.Floor floor, float age, long seed, float presence, float lit, boolean front) {
      int n = TORN.getI();
      if (floor != null && n > 0) {
         int col = floor.rgb() | 0xFF000000;
         float drop = (float)(orb.y - floor.top());
         float reach = GRIT_R.getF();
         float ds = DEBRIS_SIZE.getF();
         float grow = Ease.ramp(age, 2.0F, 6.0F);
         Vector3f tmp = new Vector3f();
         Quaternionf rot = new Quaternionf();

         for (int i = 0; i < n; i++) {
            long si = seed * 2833L + (long)i;
            float period = 14.0F + 10.0F * SpaceBend.hash(si, 41);
            float u = Ease.frac((age + SpaceBend.hash(si, 42) * period) / period);
            float sj = SpaceBend.fall(u, 2.0F);
            float scale = Math.min(1.0F, u / 0.12F) * (1.0F - 0.7F * sj) * Math.min(1.0F, (1.0F - sj) / 0.1F) * presence * grow;
            if (!(scale <= 0.03F)) {
               float a0 = SpaceBend.hash(si, 43) * (float) (Math.PI * 2) + sj * 2.2F * (SpaceBend.hash(si, 44) < 0.5F ? 1.0F : -1.0F);
               float r = (1.2F + (reach - 1.2F) * SpaceBend.hash(si, 45)) * (float)Math.pow((double)(1.0F - sj), 1.3) * (1.0F - 0.6F * this.squeeze);
               float y = -drop * (float)Math.pow((double)(1.0F - sj), 0.8) * (1.0F - 0.5F * this.squeeze);
               tmp.set((float)Math.cos((double)a0) * r, y, (float)Math.sin((double)a0) * r);
               if (inFront(tmp, c) == front) {
                  tmp.add(c);
                  float spin = age * (0.3F + 0.5F * SpaceBend.hash(si, 46)) * (1.0F + 2.0F * sj);
                  rot.identity().rotateXYZ(spin, spin * 1.2F + (float)i, spin * 0.6F);
                  LimitlessShapes.chunk(m, tmp, ds * (0.5F + 1.2F * SpaceBend.hash(si, 47)) * scale, rot, si, rubble(col, lit, 0.5F * sj));
               }
            }
         }
      }
   }

   public void tick(ClientLevel level, BlueCue cue, int age) {
      int n = GRIT.getI();
      if (n > 0 && age >= 2 && !((float)age > cue.release())) {
         Vec3 orb = cue.orb();
         BlueLapse.Floor floor = floor(level, orb);
         if (floor != null) {
            RandomSource r = level.random;
            double reach = GRIT_R.get();
            if (age < 7) {
               n = Math.max(1, n / 3);
            }

            for (int i = 0; i < n; i++) {
               double a = r.nextDouble() * Math.PI * 2.0;
               double d = 1.2 + r.nextDouble() * (reach - 1.2);
               double x = orb.x + Math.cos(a) * d;
               double z = orb.z + Math.sin(a) * d;
               BlockState st = level.getBlockState(BlockPos.containing(x, floor.top() - 0.5, z));
               if (!st.isAir() && st.getFluidState().isEmpty()) {
                  Vec3 at = new Vec3(x, floor.top() + 0.05, z);
                  int t = 8 + r.nextInt(8);
                  Vec3 v = orb.subtract(at).scale(1.0 / (double)t).add(0.0, 0.02 * (double)t, 0.0);
                  SteeredParticle.block(st, at, v);
                  if (r.nextInt(3) == 0) {
                     SteeredParticle.spawn(new BlockParticleOption(ParticleTypes.DUST_PILLAR, st), at, v.scale(0.6));
                  }
               }
            }
         }
      }
   }

   private static record Floor(double top, BlockState state, int rgb) {
   }
}
