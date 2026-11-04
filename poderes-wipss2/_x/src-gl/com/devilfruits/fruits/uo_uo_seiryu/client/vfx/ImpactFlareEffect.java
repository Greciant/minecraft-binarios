package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ImpactFlareEffect extends VfxEffect {
   private final double x;
   private final double y;
   private final double z;
   private final float size;
   private final int duration;
   private final int inner;
   private final int outer;

   public ImpactFlareEffect(Vec3 at, float size, int duration, int inner, int outer) {
      this.x = at.x;
      this.y = at.y;
      this.z = at.z;
      this.size = size;
      this.duration = Math.max(2, duration);
      this.inner = inner;
      this.outer = outer;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age > this.duration) {
         this.finish();
      }
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      float t = Mth.clamp(((float)this.age + ctx.partialTick) / (float)this.duration, 0.0F, 1.0F);
      float swell = 0.6F + 0.4F * Mth.sqrt(t);
      float alpha = (1.0F - t) * (1.0F - t) * ctx.proximity(this.x, this.y, this.z, 1.5F, 5.0F, 0.25F);
      SeiryuDraw.flare(ctx, ctx.rx(this.x), ctx.ry(this.y), ctx.rz(this.z), this.size * swell, this.inner, this.outer, alpha);
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
