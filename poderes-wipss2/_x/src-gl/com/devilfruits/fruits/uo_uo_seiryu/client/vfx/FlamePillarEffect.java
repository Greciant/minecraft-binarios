package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.VfxRenderTypes;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuParticles;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class FlamePillarEffect extends VfxEffect {
   private static final ResourceLocation STRIP = DevilFruits.id("textures/misc/uo_uo_seiryu/breath_beam.png");
   private final double x;
   private final double y;
   private final double z;
   private final float height;
   private final float width;
   private final int duration;

   public FlamePillarEffect(Vec3 base, float height, float width, int duration) {
      this.x = base.x;
      this.y = base.y;
      this.z = base.z;
      this.height = height;
      this.width = width;
      this.duration = Math.max(4, duration);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age > this.duration) {
         this.finish();
      } else {
         float top = this.height * Math.min(1.0F, (float)this.age / 4.0F);
         if (Fx.random().nextFloat() < 0.7F) {
            Fx.spawn(
               (ParticleOptions)SeiryuParticles.FLAME.get(),
               this.x + Fx.spread((double)this.width),
               this.y + (double)top,
               this.z + Fx.spread((double)this.width),
               Fx.spread(0.04),
               0.08,
               Fx.spread(0.04)
            );
         }
      }
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      float t = ((float)this.age + ctx.partialTick) / (float)this.duration;
      if (!(t >= 1.0F)) {
         float rise = Math.min(1.0F, ((float)this.age + ctx.partialTick) / 4.0F);
         float bottom = t > 0.5F ? (t - 0.5F) * 2.0F * this.height : 0.0F;
         float alpha = (t < 0.7F ? 1.0F : (1.0F - t) / 0.3F) * (0.85F + 0.15F * Mth.sin(((float)this.age + ctx.partialTick) * 1.7F));
         float ax = ctx.rx(this.x);
         float ay = ctx.ry(this.y + (double)bottom);
         float az = ctx.rz(this.z);
         float by = ctx.ry(this.y + (double)(this.height * rise));
         float scroll = ((float)this.age + ctx.partialTick) * 0.35F;
         VertexConsumer vc = ctx.buffers.getBuffer(VfxRenderTypes.glowTextured(STRIP));
         SeiryuDraw.texturedBeam(vc, ctx.matrix, ax, ay, az, ax, by, az, this.width, this.width * 0.35F, scroll, 1.0F, 15217182, 0.6F * alpha);
         SeiryuDraw.texturedBeam(vc, ctx.matrix, ax, ay, az, ax, by, az, this.width * 0.55F, this.width * 0.18F, scroll * 1.5F, 1.0F, 16742938, 0.85F * alpha);
         SeiryuDraw.texturedBeam(vc, ctx.matrix, ax, ay, az, ax, by, az, this.width * 0.2F, this.width * 0.06F, scroll * 2.0F, 1.0F, 16774338, alpha);
      }
   }

   @Override
   public double x() {
      return this.x;
   }

   @Override
   public double y() {
      return this.y + (double)this.height * 0.5;
   }

   @Override
   public double z() {
      return this.z;
   }
}
