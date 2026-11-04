package com.awakenedmod.awakened.client.render.limitless.red;

import com.awakenedmod.awakened.client.render.limitless.Ease;
import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.LimitlessMesh;
import com.awakenedmod.awakened.client.render.limitless.LimitlessShapes;
import com.awakenedmod.awakened.core.Lab;
import java.util.Arrays;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class RedBlast {
   private static final String K = "limitless.red.";
   public static final Lab.Value STREAK_W = Lab.CLIENT.def("limitless.red.streak_width", 0.045, 0.005, 0.3, 0.005);
   public static final Lab.Value STREAK_FADE = Lab.CLIENT.def("limitless.red.streak_fade", 1.4, 0.2, 8.0, 0.1);
   public static final Lab.Value GRAVITY = Lab.CLIENT.def("limitless.red.gravity", 0.05, 0.005, 0.2, 0.005);
   public static final Lab.Value BITS = Lab.CLIENT.def("limitless.red.bits", 0.0, 0.0, 150.0, 1.0);
   public static final float GROUND_REACH = 4.0F;
   static final float TWO_PI = (float) (Math.PI * 2);
   static final int DUST = 12167308;
   static final int DUST_SHADE = 7233612;
   static final int[] RUBBLE = new int[]{7032373, 8157824, 6184034, 8609854, 4864554};
   private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();

   private RedBlast() {
   }

   public static void streak(LimitlessMesh m, boolean body, RedBlast.Shot sh) {
      if (!(sh.b < 0.0F) && !(sh.head <= 0.05F)) {
         float w = STREAK_W.getF();
         float fade = STREAK_FADE.getF();
         float vis = 1.0F - Ease.ramp(sh.tb, 0.5F, 3.0F);
         if (!(vis <= 0.01F)) {
            int n = 14;
            Vector3f[] pts = new Vector3f[n];
            float[] wd = new float[n];
            int[] col = new int[n];

            for (int pass = 0; pass < (body ? 2 : 1); pass++) {
               for (int i = 0; i < n; i++) {
                  float t = (float)i / (float)(n - 1);
                  float x = sh.head * t;
                  float since = Math.max(0.0F, sh.b - x / Math.max(0.01F, sh.speed));
                  float life = (float)Math.exp((double)(-since / fade)) * vis * Ease.ramp(x, 0.0F, 0.4F);
                  float near = 0.25F + 0.75F * t * t;
                  pts[i] = new Vector3f(sh.fwd).mul(x).add(sh.p);
                  if (body && pass == 0) {
                     wd[i] = w * near;
                     col[i] = LimitlessMesh.rgba(RedOrb.rgb(5636104, 13633052, life), 0.95F * life);
                  } else if (body) {
                     wd[i] = w * 0.35F * near;
                     col[i] = LimitlessMesh.rgba(16772578, 0.95F * life * t);
                  } else {
                     wd[i] = w * 3.0F * near;
                     col[i] = LimitlessMesh.rgba(16716806, 0.5F * life);
                  }
               }

               m.ribbon(pts, wd, col, n);
            }
         }
      }
   }

   public static void glow(LimitlessMesh m, boolean body, Vector3f c, float tb, float r, float rise, float end, float strength, long seed, float age) {
      float a = strength * Ease.envelope(tb + 0.3F, rise, end, end * 0.65F);
      if (!(a <= 0.01F)) {
         if (body) {
            RedOrb.glow(m, c, r * 0.55F, 13633052, 5636104, 0.75F * a, 1.6F, 0.1F, seed, age);
            RedOrb.glow(m, c, r * 0.3F, 16772578, 16726544, 0.95F * a, 1.0F, 0.12F, seed + 1L, age);
         } else {
            RedOrb.glow(m, c, r, 16716806, 16716806, 0.4F * a, 2.0F, 0.08F, seed, age);
            m.disc(c, r * 0.25F, 16, LimitlessMesh.rgba(16758934, 0.6F * a), LimitlessMesh.rgba(16716806, 0.0F));
         }
      }
   }

   public static RedBlast.Painter painter() {
      return new RedBlast.Painter();
   }

   public static void rubble(RedBlast.Painter out, RedBlast.Shot sh, int n, float speed, float size, float floorShare, long salt) {
      if (!(sh.tb < 0.0F) && n > 0) {
         float g = GRAVITY.getF();
         float force = 1.0F / (1.0F + 0.15F * sh.height());
         Vector3f gnd = sh.ground();
         Vector3f c = new Vector3f();
         float fade = 1.0F - Ease.ramp(sh.tb, 34.0F, 8.0F);

         for (int j = 0; j < n; j++) {
            long sj = sh.seed * 53L + (long)j + salt;
            float a = (float) (Math.PI * 2) * ((float)j + 0.8F * RedOrb.h(sj, 1)) / (float)n;
            float dx = (float)Math.cos((double)a);
            float dz = (float)Math.sin((double)a);
            float d0 = 0.15F + 0.9F * RedOrb.h(sj, 2);
            float t = sh.tb - 0.25F * d0;
            if (!(t < 0.0F)) {
               float vh = speed * force * (0.35F + 0.65F * RedOrb.h(sj, 3));
               float vu = (0.2F + 0.4F * RedOrb.h(sj, 4)) * force;
               float s = size * (0.35F + 0.65F * RedOrb.h(sj, 5) * RedOrb.h(sj, 6) + 0.2F * RedOrb.h(sj, 7));
               float y0 = 0.1F;
               float land = (vu + (float)Math.sqrt((double)(vu * vu + 2.0F * g * y0))) / g;
               float x;
               float y;
               float turn;
               if (t < land) {
                  x = d0 + vh * t;
                  y = y0 + vu * t - 0.5F * g * t * t;
                  turn = t * (0.35F + 0.4F * RedOrb.h(sj, 8));
               } else {
                  float skid = 1.0F - (float)Math.exp((double)(-(t - land) / 2.5F));
                  x = d0 + vh * land + vh * 2.5F * 0.45F * skid;
                  y = 0.0F;
                  turn = land * (0.35F + 0.4F * RedOrb.h(sj, 8)) + 0.8F * skid;
               }

               c.set(gnd.x + dx * x, gnd.y + Math.max(y, 0.0F) + s * 0.32F, gnd.z + dz * x);
               int rgb = RedOrb.h(sj, 9) < floorShare ? darker(sh.floorRgb, 0.6F + 0.25F * RedOrb.h(sj, 10)) : RUBBLE[j % RUBBLE.length];
               out.chunk(c, s * fade, turn, sh.litRgb(rgb), sj);
            }
         }
      }
   }

   public static void dustRing(RedBlast.Painter out, RedBlast.Shot sh, int n, float reach, float time, float wall, float alpha, long salt) {
      if (!(sh.tb < 0.0F) && n > 0) {
         Vector3f gnd = sh.ground();
         Vector3f c = new Vector3f();
         int dustIn = sh.litRgb(RedOrb.rgb(12167308, sh.floorRgb, 0.25F));
         int dustOut = sh.litRgb(7233612);

         for (int j = 0; j < n; j++) {
            long sj = sh.seed * 61L + (long)j + salt;
            float t = sh.tb - 0.6F * RedOrb.h(sj, 1);
            if (!(t < 0.0F)) {
               float a = (float) (Math.PI * 2) * ((float)j + 0.7F * RedOrb.h(sj, 2)) / (float)n;
               float row = RedOrb.h(sj, 3);
               float front = reach * Ease.out(t / time, 3.0F) * (0.78F + 0.22F * row) + 0.012F * t;
               float r = wall * (0.55F + 0.5F * RedOrb.h(sj, 4)) * (0.35F + 0.65F * Ease.out(t / 3.0F, 2.0F)) * (1.0F + 0.03F * t);
               c.set(
                  gnd.x + (float)Math.cos((double)a) * (0.3F + front),
                  gnd.y + r * 0.55F + 0.015F * t * row,
                  gnd.z + (float)Math.sin((double)a) * (0.3F + front)
               );
               float hold = 5.0F + 6.0F * RedOrb.h(sj, 5);
               float fall = 12.0F + 10.0F * RedOrb.h(sj, 6);
               float k = alpha * Ease.ramp(t, 0.0F, 1.2F) * (1.0F - Ease.smooth((t - hold) / fall)) * (0.6F + 0.4F * row);
               out.puff(c, r, dustIn, dustOut, k, sj);
            }
         }
      }
   }

   public static void flatDisc(LimitlessMesh m, Vector3f c, float r, int segs, int inner, int outer, long seed) {
      Vector3f prev = null;

      for (int i = 0; i <= segs; i++) {
         float a = (float) (Math.PI * 2) * (float)i / (float)segs;
         float rr = r * (0.8F + 0.35F * RedOrb.h(seed, i % segs));
         Vector3f q = new Vector3f(c.x + (float)Math.cos((double)a) * rr, c.y, c.z + (float)Math.sin((double)a) * rr);
         if (prev != null) {
            m.tri(c, inner, prev, outer, q, outer);
         }

         prev = q;
      }
   }

   public static int darker(int rgb, float k) {
      return LimitlessMesh.argb(255, Math.round((float)(rgb >> 16 & 0xFF) * k), Math.round((float)(rgb >> 8 & 0xFF) * k), Math.round((float)(rgb & 0xFF) * k))
         & 16777215;
   }

   public static void groundBits(ClientLevel level, RedCue cue, int age, float power) {
      int tb = age - cue.burstTick();
      if (tb >= 0 && tb <= 1) {
         Vec3 c = cue.burstPoint();
         double floor = cue.floorY();
         if (!(c.y - floor > 4.0)) {
            BlockState st = level.getBlockState(BlockPos.containing(c.x, floor - 0.5, c.z));
            if (!st.isAir()) {
               int n = Math.round((float)BITS.getI() * power);

               for (int i = 0; i < n; i++) {
                  double a = (Math.PI * 2) * ((double)i + level.random.nextDouble()) / (double)Math.max(1, n);
                  double cx = Math.cos(a);
                  double cz = Math.sin(a);
                  double d = 0.3 + 1.4 * level.random.nextDouble();
                  double sp = (0.45 + 0.6 * level.random.nextDouble()) * (double)power * (tb == 0 ? 1.0 : 0.6);
                  BlockState bit = level.random.nextInt(3) == 0 ? DIRT : st;
                  level.addParticle(
                     new BlockParticleOption(ParticleTypes.BLOCK, bit),
                     c.x + cx * d,
                     floor + 0.15,
                     c.z + cz * d,
                     cx * sp,
                     0.5 + 0.4 * level.random.nextDouble() * (double)power,
                     cz * sp
                  );
                  level.addParticle(
                     new BlockParticleOption(ParticleTypes.DUST_PILLAR, st), c.x + cx * d, floor + 0.1, c.z + cz * d, cx * sp * 0.6, 0.1, cz * sp * 0.6
                  );
               }
            }
         }
      }
   }

   public static final class Painter {
      private static final int MAX = 400;
      private final Vector3f[] pos = new Vector3f[400];
      private final float[] size = new float[400];
      private final float[] alpha = new float[400];
      private final float[] spin = new float[400];
      private final int[] in = new int[400];
      private final int[] out = new int[400];
      private final boolean[] chunk = new boolean[400];
      private final long[] seeds = new long[400];
      private final Integer[] order = new Integer[400];
      private final Quaternionf rot = new Quaternionf();
      private int n;

      private Painter() {
         for (int i = 0; i < 400; i++) {
            this.pos[i] = new Vector3f();
         }
      }

      public void clear() {
         this.n = 0;
      }

      public void puff(Vector3f c, float r, int rgbIn, int rgbOut, float a, long seed) {
         if (this.n < 400 && !(a <= 0.01F) && !(r <= 0.01F)) {
            this.pos[this.n].set(c);
            this.size[this.n] = r;
            this.alpha[this.n] = a;
            this.in[this.n] = rgbIn;
            this.out[this.n] = rgbOut;
            this.chunk[this.n] = false;
            this.seeds[this.n] = seed;
            this.n++;
         }
      }

      public void chunk(Vector3f c, float s, float turn, int rgb, long seed) {
         if (this.n < 400 && !(s <= 0.01F)) {
            this.pos[this.n].set(c);
            this.size[this.n] = s;
            this.spin[this.n] = turn;
            this.in[this.n] = rgb;
            this.chunk[this.n] = true;
            this.seeds[this.n] = seed;
            this.n++;
         }
      }

      public void draw(LimitlessMesh m, float age) {
         for (int i = 0; i < this.n; i++) {
            this.order[i] = i;
         }

         Arrays.sort(this.order, 0, this.n, (x, y) -> Float.compare(this.pos[y].lengthSquared(), this.pos[x].lengthSquared()));

         for (int k = 0; k < this.n; k++) {
            int i = this.order[k];
            if (this.chunk[i]) {
               float t = this.spin[i];
               this.rot
                  .identity()
                  .rotateXYZ(
                     t * (0.6F + RedOrb.h(this.seeds[i], 1)) + (float) (Math.PI * 2) * RedOrb.h(this.seeds[i], 4),
                     t * (0.4F + RedOrb.h(this.seeds[i], 2)),
                     (float) (Math.PI * 2) * RedOrb.h(this.seeds[i], 3) + t * 0.3F
                  );
               LimitlessShapes.chunk(m, this.pos[i], this.size[i], this.rot, this.seeds[i], LimitlessMesh.rgba(this.in[i], 1.0F));
            } else {
               RedOrb.glow(m, this.pos[i], this.size[i], this.in[i], this.out[i], this.alpha[i], 0.55F, 0.28F, this.seeds[i], age);
            }
         }
      }
   }

   public static final class Shot {
      public final Vector3f p;
      public final Vector3f fwd;
      public final Vector3f burst;
      public final Vector3f orbAt;
      public final float age;
      public final float floorY;
      public final float flight;
      public final float speed;
      public final float dist;
      public final float b;
      public final float tb;
      public final float head;
      public final float grow;
      public final float pre;
      public final float fly;
      public final float orbVis;
      public final float lit;
      public final long seed;
      public final int floorRgb;

      public Shot(LimitlessFrame f, RedCue c, float age) {
         this.age = age;
         this.seed = c.seed();
         this.p = f.rel(c.fireFrom());
         Vec3 fw = c.fwd();
         this.fwd = new Vector3f((float)fw.x, (float)fw.y, (float)fw.z).normalize();
         Vec3 wb = c.burstPoint();
         this.burst = f.rel(wb);
         float charge = 14.0F;
         this.flight = (float)Math.max(1, c.burstTick() - 14);
         this.dist = new Vector3f(this.burst).sub(this.p).length();
         this.speed = this.dist / this.flight;
         this.b = age - charge;
         this.tb = this.b - this.flight;
         this.floorY = (float)(c.floorY() - f.cam.y);
         this.grow = Ease.smooth(age / (charge - 3.0F));
         this.pre = Ease.ramp(age, charge - 5.0F, 5.0F);
         this.fly = this.b < 0.0F ? 0.0F : Ease.ramp(this.b, 0.0F, 1.0F);
         this.orbVis = this.grow * (1.0F - Ease.ramp(this.tb, -0.4F, 1.0F));
         this.head = Math.max(0.0F, Math.min(this.dist, this.speed * this.b));
         this.orbAt = new Vector3f(this.fwd).mul(this.head).add(this.p);
         Vec3 ground = new Vec3(wb.x, c.floorY(), wb.z);
         this.lit = 0.22F + 0.78F * (float)f.level.getMaxLocalRawBrightness(BlockPos.containing(ground.add(0.0, 0.5, 0.0))) / 15.0F;
         BlockState st = f.level.getBlockState(BlockPos.containing(ground.x, ground.y - 0.5, ground.z));
         this.floorRgb = st.isAir() ? RedBlast.RUBBLE[0] : st.getMapColor(f.level, BlockPos.containing(ground.x, ground.y - 0.5, ground.z)).col;
      }

      public Vector3f ground() {
         return new Vector3f(this.burst.x, this.floorY, this.burst.z);
      }

      public float height() {
         return Math.max(0.0F, this.burst.y - this.floorY);
      }

      public boolean grounded() {
         return this.height() <= 4.0F;
      }

      public int litRgb(int rgb) {
         return LimitlessMesh.argb(
               255,
               Math.round((float)(rgb >> 16 & 0xFF) * this.lit),
               Math.round((float)(rgb >> 8 & 0xFF) * this.lit),
               Math.round((float)(rgb & 0xFF) * this.lit)
            )
            & 16777215;
      }

      public void orb(LimitlessMesh m, boolean body, float vis) {
         RedOrb.orb(m, body, this.orbAt, this.fwd, this.age, this.grow, this.pre, this.fly, vis, this.seed);
      }
   }
}
