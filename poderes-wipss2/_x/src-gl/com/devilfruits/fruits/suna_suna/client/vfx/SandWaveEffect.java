package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.CameraEffects;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.fruits.suna_suna.SunaGround;
import com.devilfruits.fruits.suna_suna.SunaParticles;
import com.devilfruits.fruits.suna_suna.SunaSounds;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class SandWaveEffect extends VfxEffect {
   private static final float STEP = 0.5F;
   private static final int CRASH_TICKS = 8;
   private static final int FRONT_ROWS = 9;
   private static final int BACK_ROWS = 6;
   private static final float BACK_SLOPE = 1.6F;
   public final long seed;
   private final Vec3 velocity;
   private final Vec3 dir;
   private final Vec3 side;
   private final float width;
   private final float height;
   private final int lifetime;
   private final int columns;
   private final float yaw;
   private final double[] base;
   private final double[] prevBase;
   private double x;
   private double y;
   private double z;
   private double px;
   private double py;
   private double pz;
   private int crashedAt = -1;

   public SandWaveEffect(ClientLevel level, Vec3 start, Vec3 velocity, float width, int packed, long seed) {
      this.seed = seed;
      this.velocity = velocity;
      this.dir = velocity.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : velocity.normalize();
      this.side = new Vec3(-this.dir.z, 0.0, this.dir.x);
      this.width = width;
      this.height = (float)Math.max(1, packed & 15) / 2.0F;
      this.lifetime = packed >> 4;
      this.columns = Math.max(3, Mth.ceil(width / 0.5F));
      this.yaw = (float)Math.atan2(this.dir.x, this.dir.z);
      this.base = new double[this.columns];
      this.prevBase = new double[this.columns];
      this.x = this.px = start.x;
      this.y = this.py = start.y;
      this.z = this.pz = start.z;
      this.ground(level);
      System.arraycopy(this.base, 0, this.prevBase, 0, this.columns);
   }

   public void crash() {
      if (this.crashedAt < 0) {
         this.crashedAt = this.age;
         RandomSource r = Fx.random();
         int n = Fx.count(this.width * 4.0F);

         for (int i = 0; i < n; i++) {
            double s = r.nextDouble() * 2.0 - 1.0;
            Vec3 p = new Vec3(this.x, this.y, this.z).add(this.side.scale(s * (double)this.width * 0.5));
            Fx.spawn(
               r.nextInt(2) == 0 ? (ParticleOptions)SunaParticles.CLUMP.get() : (ParticleOptions)SunaParticles.DUST.get(),
               p.x,
               p.y + r.nextDouble() * (double)this.height,
               p.z,
               this.dir.x * 0.15 + Fx.spread(0.1),
               0.05,
               this.dir.z * 0.15 + Fx.spread(0.1)
            );
         }

         SoundDesign.play(new Vec3(this.x, this.y + 1.0, this.z), SunaSounds.TSUNAMI_CRASH, 1.6F, 0.8F, SoundDesign.layer(SunaSounds.LAYER_SAND, 1.0F, 0.6F));
      }
   }

   private double reach() {
      return (double)this.height;
   }

   private void ground(ClientLevel level) {
      int up = Mth.ceil(this.reach() * 0.6);

      for (int c = 0; c < this.columns; c++) {
         double s = ((double)c + 0.5) / (double)this.columns * 2.0 - 1.0;
         double cx = this.x + this.side.x * s * (double)this.width * 0.5;
         double cz = this.z + this.side.z * s * (double)this.width * 0.5;
         double g = SunaGround.surface(level, cx, this.y, cz, up, 4);
         this.base[c] = Double.isNaN(g) ? this.y : g;
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      this.px = this.x;
      this.py = this.y;
      this.pz = this.z;
      System.arraycopy(this.base, 0, this.prevBase, 0, this.columns);
      if (this.crashedAt >= 0) {
         if (this.age - this.crashedAt > 8) {
            this.finish();
         }
      } else if (this.age > this.lifetime + 20) {
         this.crash();
      } else {
         double nx = this.x + this.velocity.x;
         double nz = this.z + this.velocity.z;
         double g = SunaGround.surface(level, nx, this.y, nz, Mth.ceil(this.reach() * 0.6), 4);
         if (Double.isNaN(g)) {
            this.crash();
         } else {
            this.x = nx;
            this.y = g;
            this.z = nz;
            this.ground(level);
            RandomSource r = Fx.random();
            int n = Fx.count(this.width * 0.9F);

            for (int i = 0; i < n; i++) {
               int c = r.nextInt(this.columns);
               double s = ((double)c + 0.5) / (double)this.columns * 2.0 - 1.0;
               double h = (double)this.columnHeight(s, (float)this.age);
               Vec3 p = new Vec3(this.x, this.base[c], this.z).add(this.side.scale(s * (double)this.width * 0.5)).add(this.dir.scale((double)lean(1.0F) * h));
               Fx.spawn(
                  r.nextInt(4) == 0 ? (ParticleOptions)SunaParticles.CLUMP.get() : (ParticleOptions)SunaParticles.GRAIN.get(),
                  p.x,
                  p.y + h,
                  p.z,
                  this.dir.x * 0.22 + Fx.spread(0.05),
                  0.08 + r.nextDouble() * 0.1,
                  this.dir.z * 0.22 + Fx.spread(0.05)
               );
               if (i % 3 == 0) {
                  Fx.spawn(
                     (ParticleOptions)SunaParticles.DUST.get(),
                     p.x + this.dir.x * 0.6,
                     this.base[c] + 0.2,
                     p.z + this.dir.z * 0.6,
                     this.dir.x * 0.08,
                     0.02,
                     this.dir.z * 0.08
                  );
               }
            }

            if (this.age % 8 == 1) {
               SoundDesign.play(new Vec3(this.x, this.y + 1.0, this.z), SunaSounds.TSUNAMI_LOOP, 1.4F, 0.7F + r.nextFloat() * 0.1F);
               CameraEffects.shakeAt(new Vec3(this.x, this.y, this.z), 0.12F, (double)(this.width + 6.0F));
            }
         }
      }
   }

   private float columnHeight(double s, float t) {
      double a2 = s * s;
      double profile = 1.0 - 0.85 * a2 * a2 * a2;
      double ripple = 1.0 + 0.06 * Math.sin((double)t * 0.31 + s * 11.0) + 0.03 * Math.sin((double)t * 0.57 + s * 23.0);
      return (float)((double)this.height * profile * ripple);
   }

   private static float lean(float h) {
      float l = (float)Math.pow((double)h, 2.4) * 0.4F;
      return h > 0.75F ? l + (float)Math.pow((double)(h - 0.75F), 1.5) * 1.5F : l;
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      float pt = ctx.partialTick;
      float t = (float)this.age + pt;
      float shrink = this.crashedAt < 0 ? Math.min(1.0F, t / 6.0F) : Math.max(0.0F, 1.0F - ((float)(this.age - this.crashedAt) + pt) / 8.0F);
      if (!(shrink <= 0.02F)) {
         double cx = Mth.lerp((double)pt, this.px, this.x);
         double cz = Mth.lerp((double)pt, this.pz, this.z);
         int light = SandArt.light(cx, Mth.lerp((double)pt, this.py, this.y) + 1.0, cz);
         float topple = this.crashedAt < 0 ? 0.0F : 1.0F - shrink;
         int n = this.columns + 1;
         Vec3[] feet = new Vec3[n];
         float[] heights = new float[n];

         for (int c = 0; c < n; c++) {
            double s = (double)c / (double)this.columns * 2.0 - 1.0;
            feet[c] = new Vec3(cx + this.side.x * s * (double)this.width * 0.5, this.foot(c, pt), cz + this.side.z * s * (double)this.width * 0.5);
            heights[c] = this.columnHeight(s, t) * shrink;
         }

         float fnx = (float)this.dir.x * 0.75F;
         float fnz = (float)this.dir.z * 0.75F;
         VertexConsumer face = SandArt.solid(ctx, SandArt.WAVE);

         for (int c = 0; c < this.columns; c++) {
            float u0 = (float)c / (float)this.columns * this.width;
            float u1 = (float)(c + 1) / (float)this.columns * this.width;

            for (int r = 0; r < 9; r++) {
               float f0 = (float)r / 9.0F;
               float f1 = (float)(r + 1) / 9.0F;
               Vec3 a = this.front(feet[c], heights[c], f0, topple);
               Vec3 b = this.front(feet[c + 1], heights[c + 1], f0, topple);
               Vec3 cc = this.front(feet[c + 1], heights[c + 1], f1, topple);
               Vec3 d = this.front(feet[c], heights[c], f1, topple);
               float va = (1.0F - f0) * heights[c] * 0.5F;
               float vb = (1.0F - f0) * heights[c + 1] * 0.5F;
               float vc2 = (1.0F - f1) * heights[c + 1] * 0.5F;
               float vd = (1.0F - f1) * heights[c] * 0.5F;
               float ny = 0.35F + 0.5F * f1;
               SandArt.vertex(ctx, face, a, u0, va, -1, light, fnx, ny, fnz);
               SandArt.vertex(ctx, face, b, u1, vb, -1, light, fnx, ny, fnz);
               SandArt.vertex(ctx, face, cc, u1, vc2, -1, light, fnx, ny, fnz);
               SandArt.vertex(ctx, face, d, u0, vd, -1, light, fnx, ny, fnz);
            }
         }

         VertexConsumer back = SandArt.solid(ctx, SandArt.WAVE);

         for (int c = 0; c < this.columns; c++) {
            float u0 = (float)c / (float)this.columns * this.width;
            float u1 = (float)(c + 1) / (float)this.columns * this.width;

            for (int r = 0; r < 6; r++) {
               float b0 = (float)r / 6.0F;
               float b1 = (float)(r + 1) / 6.0F;
               Vec3 a = this.backPoint(feet[c], heights[c], b0, topple);
               Vec3 b = this.backPoint(feet[c + 1], heights[c + 1], b0, topple);
               Vec3 cc = this.backPoint(feet[c + 1], heights[c + 1], b1, topple);
               Vec3 d = this.backPoint(feet[c], heights[c], b1, topple);
               float va = b0 * heights[c] * 1.6F * 0.5F;
               float vd = b1 * heights[c] * 1.6F * 0.5F;
               SandArt.vertex(ctx, back, a, u0, va, -1, light, -fnx * 0.6F, 0.8F, -fnz * 0.6F);
               SandArt.vertex(ctx, back, b, u1, b0 * heights[c + 1] * 1.6F * 0.5F, -1, light, -fnx * 0.6F, 0.8F, -fnz * 0.6F);
               SandArt.vertex(ctx, back, cc, u1, b1 * heights[c + 1] * 1.6F * 0.5F, -1, light, -fnx * 0.6F, 0.8F, -fnz * 0.6F);
               SandArt.vertex(ctx, back, d, u0, vd, -1, light, -fnx * 0.6F, 0.8F, -fnz * 0.6F);
            }
         }

         Vec3[] crest = new Vec3[n];
         Vec3[] up = new Vec3[n];
         float[] w = new float[n];
         float[] alpha = new float[n];

         for (int c = 0; c < n; c++) {
            crest[c] = this.front(feet[c], heights[c], 1.0F, topple)
               .add(this.dir.scale(0.15))
               .add(0.0, 0.22 + 0.06 * Math.sin((double)t * 0.9 + (double)c), 0.0);
            up[c] = new Vec3(0.0, 1.0, 0.0).add(this.dir.scale(-0.4)).normalize();
            w[c] = 0.3F * shrink;
            alpha[c] = 0.85F * Math.min(1.0F, heights[c] / Math.max(0.1F, this.height) * 1.6F);
         }

         SandArt.ribbon(ctx, SandArt.wispy(ctx, SandArt.STREAM), crest, up, w, alpha, n, 0.5F, t * 0.3F, 16777215, light);
         VertexConsumer lumps = SandArt.solid(ctx, SandArt.PACKED);

         for (int c = 1; c < this.columns; c += 3) {
            Vec3 p = this.front(feet[c], heights[c], 0.88F, topple).add(this.dir.scale(0.2));
            float size = 0.12F + 0.05F * (float)(c % 2);
            Quaternionf rot = new Quaternionf().rotateY(this.yaw).rotateX(t * 0.45F + (float)c).rotateZ(t * 0.21F + (float)c);
            SandArt.chunk(ctx, lumps, p.x, p.y, p.z, size, size, size, rot, c * 7, light);
         }
      }
   }

   private double foot(int c, float pt) {
      int a = Math.max(0, c - 1);
      int b = Math.min(this.columns - 1, c);
      return (Mth.lerp((double)pt, this.prevBase[a], this.base[a]) + Mth.lerp((double)pt, this.prevBase[b], this.base[b])) * 0.5;
   }

   private Vec3 front(Vec3 foot, float h, float f, float topple) {
      double y = (double)(f * h - (f > 0.9F ? (f - 0.9F) * h * 0.6F : 0.0F));
      double fwd = (double)(lean(f) * h) + (double)topple * 1.5 * (double)f;
      return foot.add(this.dir.scale(fwd)).add(0.0, y, 0.0);
   }

   private Vec3 backPoint(Vec3 foot, float h, float b, float topple) {
      double y = (double)h * 0.82 * (double)(1.0F - b * b * (3.0F - 2.0F * b));
      double fwd = (double)(lean(0.8F) * h - b * h * 1.6F) + (double)topple * 1.2 * (double)(1.0F - b);
      return foot.add(this.dir.scale(fwd)).add(0.0, y, 0.0);
   }

   @Override
   public double x() {
      return this.x;
   }

   @Override
   public double y() {
      return this.y;
   }

   @Override
   public double z() {
      return this.z;
   }

   @Override
   public double cullDistance() {
      return 128.0;
   }
}
