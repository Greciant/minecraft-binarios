package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;

public class FireballEffect extends VfxEffect {
   public static final double GRAVITY = 0.025;
   private static final int TRAIL = 14;
   public final long seed;
   private final float size;
   private final int lifetime;
   private double x;
   private double y;
   private double z;
   private double px;
   private double py;
   private double pz;
   private double vx;
   private double vy;
   private double vz;
   private final float[] trail = new float[42];
   private final float[] scratch = new float[42];
   private int trailCount;
   private final int trailLength;

   public FireballEffect(Vec3 start, Vec3 velocity, float size, int lifetime, long seed) {
      this.x = this.px = start.x;
      this.y = this.py = start.y;
      this.z = this.pz = start.z;
      this.vx = velocity.x;
      this.vy = velocity.y;
      this.vz = velocity.z;
      this.size = size;
      this.lifetime = Math.max(1, lifetime);
      this.seed = seed;
      this.trailLength = size >= 1.5F ? 14 : 6;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age > this.lifetime + 1) {
         this.finish();
      } else {
         this.px = this.x;
         this.py = this.y;
         this.pz = this.z;
         Vec3 from = new Vec3(this.x, this.y, this.z);
         Vec3 to = new Vec3(this.x + this.vx, this.y + this.vy, this.z + this.vz);
         HitResult hit = PhysicsWorld.clip(level, new ClipContext(from, to, Block.COLLIDER, Fluid.ANY, CollisionContext.empty()));
         if (hit.getType() != Type.MISS) {
            this.finish();
         } else {
            this.x = to.x;
            this.y = to.y;
            this.z = to.z;
            this.vy -= 0.025;
            System.arraycopy(this.trail, 0, this.trail, 3, 39);
            this.trail[0] = (float)this.px;
            this.trail[1] = (float)this.py;
            this.trail[2] = (float)this.pz;
            this.trailCount = Math.min(this.trailLength, this.trailCount + 1);
            if (this.trailLength == 14) {
               Fx.spawnFar(
                  ParticleTypes.LARGE_SMOKE,
                  this.px + Fx.spread(0.4 * (double)this.size),
                  this.py + Fx.spread(0.4 * (double)this.size),
                  this.pz + Fx.spread(0.4 * (double)this.size),
                  0.0,
                  0.02,
                  0.0
               );
            }

            for (int i = 0; i < Fx.count(2); i++) {
               Fx.spawn(
                  (ParticleOptions)SeiryuParticles.FLAME.get(),
                  this.px + Fx.spread(0.3 * (double)this.size),
                  this.py + Fx.spread(0.3 * (double)this.size),
                  this.pz + Fx.spread(0.3 * (double)this.size),
                  0.0,
                  0.02,
                  0.0
               );
            }

            if (this.age % 2 == 0) {
               Fx.spawn((ParticleOptions)SeiryuParticles.EMBER.get(), this.px, this.py, this.pz, Fx.spread(0.05), 0.05, Fx.spread(0.05));
            }
         }
      }
   }

   @Override
   public void renderGlow(VfxContext ctx) {
      if (this.trailCount >= 1) {
         float cx = (float)Mth.lerp((double)ctx.partialTick, this.px, this.x);
         float cy = (float)Mth.lerp((double)ctx.partialTick, this.py, this.y);
         float cz = (float)Mth.lerp((double)ctx.partialTick, this.pz, this.z);
         this.scratch[0] = 0.0F;
         this.scratch[1] = 0.0F;
         this.scratch[2] = 0.0F;
         int n = 1;

         for (int i = 0; i < this.trailCount; i++) {
            this.scratch[n * 3] = this.trail[i * 3] - cx;
            this.scratch[n * 3 + 1] = this.trail[i * 3 + 1] - cy;
            this.scratch[n * 3 + 2] = this.trail[i * 3 + 2] - cz;
            if (++n >= this.trailLength) {
               break;
            }
         }

         SeiryuDraw.ribbons(
            ctx, this.scratch, n, ctx.rx((double)cx), ctx.ry((double)cy), ctx.rz((double)cz), 0.12F * this.size, 1.0F, 16776160, 16758574, 14041882, 0.8F
         );
      }
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      double cx = Mth.lerp((double)ctx.partialTick, this.px, this.x);
      double cy = Mth.lerp((double)ctx.partialTick, this.py, this.y);
      double cz = Mth.lerp((double)ctx.partialTick, this.pz, this.z);
      float flicker = 0.85F + 0.15F * Mth.sin(ctx.time * 2.3F + (float)this.seed);
      float glow = this.trailLength == 14 ? 1.5F : 0.9F;
      SeiryuDraw.flare(
         ctx, ctx.rx(cx), ctx.ry(cy), ctx.rz(cz), glow * this.size * flicker, 16776160, 16758574, 0.9F * ctx.proximity(cx, cy, cz, 1.0F, 3.0F, 0.3F)
      );
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
