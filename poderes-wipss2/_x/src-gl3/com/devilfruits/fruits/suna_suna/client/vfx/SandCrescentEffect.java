package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.fruits.suna_suna.SunaParticles;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Quaternionf;

public class SandCrescentEffect extends VfxEffect {
   private static final int SEGMENTS = 24;
   private static final int FLOW = 9;
   private static final int TRAIL = 7;
   private static final float ARC = 1.08F;
   public final long seed;
   private final Vec3 velocity;
   private final Vec3 dir;
   private final Vec3 side;
   private final Vec3 normal;
   private final float half;
   private final int lifetime;
   private double x;
   private double y;
   private double z;
   private double px;
   private double py;
   private double pz;
   private int stoppedAt = -1;
   private final Vec3[] path = new Vec3[Math.max(9, 7)];
   private final Vec3[] across = new Vec3[Math.max(9, 7)];
   private final float[] widths = new float[Math.max(9, 7)];
   private final float[] alphas = new float[Math.max(9, 7)];

   public SandCrescentEffect(Vec3 start, Vec3 velocity, float span, int lifetime, long seed) {
      this.seed = seed;
      this.velocity = velocity;
      this.dir = velocity.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : velocity.normalize();
      Vec3 flat = new Vec3(this.dir.x, 0.0, this.dir.z);
      this.side = flat.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(-flat.z, 0.0, flat.x).normalize();
      this.normal = this.side.cross(this.dir).normalize();
      this.half = span * 0.5F;
      this.lifetime = lifetime;
      this.x = this.px = start.x;
      this.y = this.py = start.y;
      this.z = this.pz = start.z;
   }

   public void stop(Vec3 at) {
      if (this.stoppedAt < 0) {
         this.stoppedAt = this.age;
         this.x = this.px = at.x;
         this.y = this.py = at.y;
         this.z = this.pz = at.z;
         this.burst();
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      this.px = this.x;
      this.py = this.y;
      this.pz = this.z;
      if (this.stoppedAt < 0 && this.age <= this.lifetime + 10) {
         Vec3 from = new Vec3(this.x, this.y, this.z);
         Vec3 to = from.add(this.velocity);
         if (PhysicsWorld.clip(level, new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, CollisionContext.empty())).getType() != Type.MISS) {
            this.stop(from);
         } else {
            this.x = to.x;
            this.y = to.y;
            this.z = to.z;
            RandomSource r = Fx.random();
            Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            float spread = spread((float)this.age);
            int n = Fx.count(9);

            for (int i = 0; i < n; i++) {
               double s = r.nextDouble() * 2.0 - 1.0;
               Vec3 p = this.back(s, this.x, this.y, this.z, spread).add(this.dir.scale(r.nextDouble() * this.depth(s, spread)));
               if (!(p.distanceToSqr(camera) < 4.0)) {
                  Fx.spawn(
                     r.nextInt(5) == 0 ? (ParticleOptions)SunaParticles.DUST.get() : (ParticleOptions)SunaParticles.GRAIN.get(),
                     p.x,
                     p.y,
                     p.z,
                     this.velocity.x * 0.25 + Fx.spread(0.05),
                     -0.04,
                     this.velocity.z * 0.25 + Fx.spread(0.05)
                  );
               }
            }
         }
      } else {
         this.finish();
      }
   }

   private static float spread(float t) {
      return 0.35F + 0.65F * Math.min(1.0F, t / 3.0F);
   }

   private Vec3 lead(double s, double cx, double cy, double cz, float spread) {
      double h = (double)(this.half * spread);
      double trail = 0.45 * h * s * s;
      return new Vec3(
         cx + this.side.x * s * h - this.dir.x * trail, cy + this.side.y * s * h - this.dir.y * trail, cz + this.side.z * s * h - this.dir.z * trail
      );
   }

   private double depth(double s, float spread) {
      return 0.45 * (double)this.half * (double)spread * Math.pow(Math.max(0.0, 1.0 - s * s), 0.75);
   }

   private Vec3 back(double s, double cx, double cy, double cz, float spread) {
      return this.lead(s, cx, cy, cz, spread).subtract(this.dir.scale(this.depth(s, spread)));
   }

   private Vec3 across(double s, float spread) {
      double h = (double)(this.half * spread);
      Vec3 tangent = this.side.scale(h).subtract(this.dir.scale(0.9 * h * s));
      Vec3 a = this.normal.cross(tangent);
      a = a.lengthSqr() < 1.0E-8 ? this.dir : a.normalize();
      return a.dot(this.dir) < 0.0 ? a.reverse() : a;
   }

   private void burst() {
      RandomSource r = Fx.random();
      float spread = spread((float)this.age);
      int n = Fx.count(30);

      for (int i = 0; i < n; i++) {
         Vec3 p = this.lead(r.nextDouble() * 2.0 - 1.0, this.x, this.y, this.z, spread);
         Fx.spawn(
            r.nextInt(3) == 0 ? (ParticleOptions)SunaParticles.CLUMP.get() : (ParticleOptions)SunaParticles.GRAIN.get(),
            p.x,
            p.y,
            p.z,
            Fx.spread(0.2) + this.dir.x * 0.12,
            0.06 + r.nextDouble() * 0.14,
            Fx.spread(0.2) + this.dir.z * 0.12
         );
      }

      for (int i = 0; i < Fx.count(6); i++) {
         Vec3 p = this.lead(r.nextDouble() * 2.0 - 1.0, this.x, this.y, this.z, spread);
         Fx.spawn((ParticleOptions)SunaParticles.DUST.get(), p.x, p.y, p.z, Fx.spread(0.05), 0.02, Fx.spread(0.05));
      }
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      if (this.stoppedAt < 0) {
         float pt = ctx.partialTick;
         double cx = Mth.lerp((double)pt, this.px, this.x);
         double cy = Mth.lerp((double)pt, this.py, this.y);
         double cz = Mth.lerp((double)pt, this.pz, this.z);
         float near = ctx.proximity(cx, cy, cz, 1.0F, 2.4F, 0.0F);
         if (!(near <= 0.0F)) {
            float t = (float)this.age + pt;
            float spread = spread(t);
            int light = SandArt.light(cx, cy, cz);
            float arc = this.half * spread * 1.08F;
            int frame = this.age / 2 % 4;
            float v0 = (float)frame / 4.0F + 0.002F;
            float v1 = (float)(frame + 1) / 4.0F - 0.002F;
            VertexConsumer body = SandArt.wispy(ctx, SandArt.BARJAN);

            for (int layer = -1; layer <= 1; layer++) {
               Vec3 shift = this.normal.scale((double)layer * 0.09).subtract(this.dir.scale((double)Math.abs(layer) * 0.06));
               int color = SandArt.argb(16777215, (layer == 0 ? 1.0F : 0.7F) * near);

               for (int i = 0; i < 24; i++) {
                  double s0 = (double)i / 24.0 * 2.0 - 1.0;
                  double s1 = (double)(i + 1) / 24.0 * 2.0 - 1.0;
                  float u0 = (float)Math.abs(s0) * arc / 2.0F;
                  float u1 = (float)Math.abs(s1) * arc / 2.0F;
                  Vec3 a0 = this.lead(s0, cx, cy, cz, spread).add(shift);
                  Vec3 a1 = this.lead(s1, cx, cy, cz, spread).add(shift);
                  Vec3 b0 = this.back(s0, cx, cy, cz, spread).add(shift);
                  Vec3 b1 = this.back(s1, cx, cy, cz, spread).add(shift);
                  SandArt.vertex(ctx, body, a0, u0, v0, color, light);
                  SandArt.vertex(ctx, body, a1, u1, v0, color, light);
                  SandArt.vertex(ctx, body, b1, u1, v1, color, light);
                  SandArt.vertex(ctx, body, b0, u0, v1, color, light);
               }
            }

            VertexConsumer stream = SandArt.wispy(ctx, SandArt.STREAM);

            for (int horn = -1; horn <= 1; horn += 2) {
               for (int i = 0; i < 9; i++) {
                  double s = (double)(horn * i) / 8.0 * 0.95;
                  this.path[i] = this.lead(s, cx, cy, cz, spread).subtract(this.dir.scale(this.depth(s, spread) * 0.4)).add(this.normal.scale(0.13));
                  this.across[i] = this.across(s, spread);
                  this.widths[i] = (float)(this.depth(s, spread) * 0.3 + 0.05);
                  this.alphas[i] = (float)(0.75 * (1.0 - s * s)) * near;
               }

               SandArt.ribbon(ctx, stream, this.path, this.across, this.widths, this.alphas, 9, 0.5F, -t * 0.5F, 16777215, light);
            }

            for (int i = 0; i < 9; i++) {
               double s = ((double)i / 8.0 * 2.0 - 1.0) * 0.85;
               this.path[i] = this.back(s, cx, cy, cz, spread)
                  .add(this.dir.scale(0.1))
                  .add(this.normal.scale(0.1 + 0.06 * (double)Mth.sin(t * 0.9F + (float)i)));
               this.across[i] = this.normal;
               this.widths[i] = (float)(0.2 * (1.0 - s * s) + 0.04);
               this.alphas[i] = (float)(0.5 * (1.0 - s * s)) * near;
            }

            SandArt.ribbon(ctx, stream, this.path, this.across, this.widths, this.alphas, 9, 0.5F, t * 0.3F, 16777215, light);

            for (int horn = -1; horn <= 1; horn += 2) {
               Vec3 tip = this.lead((double)horn * 0.97, cx, cy, cz, spread);
               Vec3 flat = this.side.scale((double)horn * 0.8).add(this.normal.scale(0.6)).normalize();

               for (int i = 0; i < 7; i++) {
                  float f = (float)i / 6.0F;
                  this.path[i] = tip.subtract(this.dir.scale((double)f * 2.6))
                     .add(this.side.scale((double)((float)horn * f) * 0.35))
                     .add(this.normal.scale((double)(Mth.sin(t * 0.7F + f * 4.0F + (float)horn) * 0.1F)));
                  this.across[i] = flat;
                  this.widths[i] = 0.12F + 0.14F * f;
                  this.alphas[i] = (1.0F - f) * 0.8F * near;
               }

               SandArt.ribbon(ctx, stream, this.path, this.across, this.widths, this.alphas, 7, 0.5F, -t * 0.4F, 16777215, light);
            }

            if (!(near < 0.6F)) {
               VertexConsumer rim = SandArt.solid(ctx, SandArt.PACKED);

               for (int i = 0; i < 24; i++) {
                  double s0 = ((double)i / 24.0 * 2.0 - 1.0) * 0.93;
                  double s1 = ((double)(i + 1) / 24.0 * 2.0 - 1.0) * 0.93;
                  double w0 = 0.05 + 0.06 * (1.0 - Math.abs(s0));
                  double w1 = 0.05 + 0.06 * (1.0 - Math.abs(s1));
                  Vec3 a0 = this.lead(s0, cx, cy, cz, spread);
                  Vec3 a1 = this.lead(s1, cx, cy, cz, spread);
                  Vec3 c0 = this.across(s0, spread);
                  Vec3 c1 = this.across(s1, spread);
                  float u0 = (float)(s0 * (double)this.half * (double)spread);
                  float u1 = (float)(s1 * (double)this.half * (double)spread);
                  SandArt.quad(
                     ctx,
                     rim,
                     a0.add(c0.scale(w0)),
                     a1.add(c1.scale(w1)),
                     a1.subtract(c1.scale(w1 * 2.0)),
                     a0.subtract(c0.scale(w0 * 2.0)),
                     u0,
                     0.0F,
                     u1,
                     0.2F,
                     -1,
                     light
                  );
                  SandArt.quad(
                     ctx,
                     rim,
                     a0.add(this.normal.scale(w0)),
                     a1.add(this.normal.scale(w1)),
                     a1.subtract(this.normal.scale(w1)),
                     a0.subtract(this.normal.scale(w0)),
                     u0,
                     0.5F,
                     u1,
                     0.65F,
                     -1,
                     light
                  );
               }

               VertexConsumer lumps = SandArt.solid(ctx, SandArt.PACKED);

               for (int i = 0; i < 7; i++) {
                  double s = (((double)i + 0.5) / 7.0 * 2.0 - 1.0) * 0.8;
                  Vec3 p = this.lead(s, cx, cy, cz, spread)
                     .subtract(this.dir.scale(this.depth(s, spread) * 0.45))
                     .add(this.normal.scale(0.05 * (double)Mth.sin(t * 0.6F + (float)i)));
                  float size = (float)(0.08 + 0.07 * (1.0 - Math.abs(s)));
                  Quaternionf rot = new Quaternionf().rotateY(t * 0.5F + (float)i).rotateX(t * 0.37F + (float)i * 1.7F);
                  SandArt.chunk(ctx, lumps, p.x, p.y, p.z, size, size, size, rot, i + (int)this.seed, light);
               }
            }
         }
      }
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
}
