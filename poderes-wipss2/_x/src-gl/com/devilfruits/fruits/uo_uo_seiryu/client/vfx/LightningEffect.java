package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.core.util.DeterministicRandom;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class LightningEffect extends VfxEffect {
   private static final int DEPTH = 4;
   private static final int BRANCH_DEPTH = 3;
   private final double sx;
   private final double sy;
   private final double sz;
   private final float dx;
   private final float dy;
   private final float dz;
   private final int duration;
   private final long seed;
   private final float width;
   private final int branchCount;
   private final float[] main = new float[SeiryuDraw.pointCount(4) * 3];
   private final float[][] branches;
   private final float[] branchOrigin;
   private boolean sparks = true;

   public LightningEffect(Vec3 from, Vec3 to, int duration, float width, int branches, long seed) {
      this.sx = from.x;
      this.sy = from.y;
      this.sz = from.z;
      this.dx = (float)(to.x - from.x);
      this.dy = (float)(to.y - from.y);
      this.dz = (float)(to.z - from.z);
      this.duration = Math.max(2, duration);
      this.width = width;
      this.seed = seed;
      this.branchCount = branches;
      this.branches = new float[branches][SeiryuDraw.pointCount(3) * 3];
      this.branchOrigin = new float[branches * 3];
      this.regenerate(0);
   }

   public LightningEffect quiet() {
      this.sparks = false;
      return this;
   }

   private void regenerate(int frame) {
      long s = this.seed + (long)frame * 7919L;
      float len = Mth.sqrt(this.dx * this.dx + this.dy * this.dy + this.dz * this.dz);
      SeiryuDraw.boltPath(this.main, this.dx, this.dy, this.dz, 4, 0.16F, s);
      DeterministicRandom r = new DeterministicRandom(s ^ 25214903917L);
      int mainPoints = SeiryuDraw.pointCount(4);

      for (int b = 0; b < this.branchCount; b++) {
         int at = 2 + r.nextInt(mainPoints - 5);
         this.branchOrigin[b * 3] = this.main[at * 3];
         this.branchOrigin[b * 3 + 1] = this.main[at * 3 + 1];
         this.branchOrigin[b * 3 + 2] = this.main[at * 3 + 2];
         float bl = len * r.range(0.2F, 0.4F);
         float ex = this.dx / len * bl + r.signed(bl * 0.8F);
         float ey = this.dy / len * bl + r.signed(bl * 0.8F);
         float ez = this.dz / len * bl + r.signed(bl * 0.8F);
         SeiryuDraw.boltPath(this.branches[b], ex, ey, ez, 3, 0.2F, s + (long)b * 31L);
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age > this.duration) {
         this.finish();
      } else {
         if (this.age % 2 == 0) {
            this.regenerate(this.age / 2);
         }

         if (this.sparks && this.age == 1) {
            int n = Fx.count(4);

            for (int i = 0; i < n; i++) {
               Fx.spawn(
                  (ParticleOptions)SeiryuParticles.VIOLET_SPARK.get(),
                  this.sx + (double)this.dx + Fx.spread(0.4),
                  this.sy + (double)this.dy + Fx.spread(0.4),
                  this.sz + (double)this.dz + Fx.spread(0.4),
                  Fx.spread(0.15),
                  Fx.spread(0.15),
                  Fx.spread(0.15)
               );
            }
         }
      }
   }

   @Override
   public void renderGlow(VfxContext ctx) {
      float life = ((float)this.age + ctx.partialTick) / (float)this.duration;
      float alpha = life < 0.25F ? 1.0F : (1.0F - life) * (0.65F + 0.35F * Mth.sin(((float)this.age + ctx.partialTick) * 2.7F + (float)this.seed));
      if (!(alpha <= 0.0F)) {
         float ox = ctx.rx(this.sx);
         float oy = ctx.ry(this.sy);
         float oz = ctx.rz(this.sz);
         int count = SeiryuDraw.pointCount(4);
         SeiryuDraw.ribbons(ctx, this.main, count, ox, oy, oz, Math.min(this.width * 1.8F, 0.22F) + 0.04F, 0.3F, 8011007, 8011007, 2757984, alpha * 0.3F);
         SeiryuDraw.ribbons(ctx, this.main, count, ox, oy, oz, this.width, 0.4F, 16777215, 11955455, 8011007, alpha);
         int bp = SeiryuDraw.pointCount(3);

         for (int b = 0; b < this.branchCount; b++) {
            SeiryuDraw.ribbons(
               ctx,
               this.branches[b],
               bp,
               ox + this.branchOrigin[b * 3],
               oy + this.branchOrigin[b * 3 + 1],
               oz + this.branchOrigin[b * 3 + 2],
               this.width * 0.6F,
               0.9F,
               16777215,
               11955455,
               8011007,
               alpha * 0.8F
            );
         }
      }
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      float life = ((float)this.age + ctx.partialTick) / (float)this.duration;
      float alpha = life < 0.25F ? 1.0F : (1.0F - life) * (0.65F + 0.35F * Mth.sin(((float)this.age + ctx.partialTick) * 2.7F + (float)this.seed));
      if (!(alpha <= 0.02F)) {
         float len = Mth.sqrt(this.dx * this.dx + this.dy * this.dy + this.dz * this.dz);
         float size = Math.min(2.4F, 0.4F + this.width * 4.0F + len * 0.03F);
         int count = SeiryuDraw.pointCount(4);
         float ox = ctx.rx(this.sx);
         float oy = ctx.ry(this.sy);
         float oz = ctx.rz(this.sz);
         float near = ctx.proximity(this.sx + (double)this.dx * 0.5, this.sy + (double)this.dy * 0.5, this.sz + (double)this.dz * 0.5, 1.5F, 5.0F, 0.3F);

         for (int k = 1; k <= 3; k++) {
            int i = k * (count - 1) / 4;
            SeiryuDraw.flare(ctx, ox + this.main[i * 3], oy + this.main[i * 3 + 1], oz + this.main[i * 3 + 2], size, 8011007, 2757984, alpha * 0.4F * near);
         }

         int e = count - 1;
         SeiryuDraw.flare(ctx, ox + this.main[e * 3], oy + this.main[e * 3 + 1], oz + this.main[e * 3 + 2], size * 1.4F, 11955455, 8011007, alpha * 0.6F * near);
      }
   }

   @Override
   public double x() {
      return this.sx + (double)this.dx * 0.5;
   }

   @Override
   public double y() {
      return this.sy + (double)this.dy * 0.5;
   }

   @Override
   public double z() {
      return this.sz + (double)this.dz * 0.5;
   }

   @Override
   public double cullDistance() {
      return 160.0;
   }
}
