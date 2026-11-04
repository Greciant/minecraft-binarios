package com.devilfruits.client.render.vfx;

import com.devilfruits.client.render.VfxRenderTypes;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class GroundDecalEffect extends VfxEffect {
   private final double x;
   private final double y;
   private final double z;
   private final ResourceLocation texture;
   private final float size;
   private final float angle;
   private final int life;
   private float scaleFrom = 0.6F;
   private float scaleTo = 1.0F;
   private int changeAge;
   private int changeTicks = 2;
   private float glowAlpha;
   private ResourceLocation glowTexture;

   public GroundDecalEffect(Vec3 at, ResourceLocation texture, float size, float angle, int life) {
      this.x = at.x;
      this.y = at.y + 0.02;
      this.z = at.z;
      this.texture = texture;
      this.size = size;
      this.angle = angle;
      this.life = Math.max(10, life);
   }

   public GroundDecalEffect growIn(float startScale, int ticks) {
      this.scaleFrom = startScale;
      this.scaleTo = 1.0F;
      this.changeAge = this.age;
      this.changeTicks = Math.max(1, ticks);
      return this;
   }

   public GroundDecalEffect growTo(float scale) {
      this.scaleFrom = this.currentScale(0.0F);
      this.scaleTo = scale;
      this.changeAge = this.age;
      this.changeTicks = 4;
      return this;
   }

   public GroundDecalEffect glow(ResourceLocation texture, float alpha) {
      this.glowTexture = texture;
      this.glowAlpha = alpha;
      return this;
   }

   private float currentScale(float partial) {
      float t = Mth.clamp(((float)(this.age - this.changeAge) + partial) / (float)this.changeTicks, 0.0F, 1.0F);
      return this.scaleFrom + (this.scaleTo - this.scaleFrom) * (1.0F - (1.0F - t) * (1.0F - t));
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age > this.life) {
         this.finish();
      }
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      float fade = Mth.clamp(((float)(this.life - this.age) - ctx.partialTick) / 30.0F, 0.0F, 1.0F);
      if (!(fade <= 0.0F)) {
         float half = this.size * 0.5F * this.currentScale(ctx.partialTick);
         this.quad(ctx.buffers.getBuffer(VfxRenderTypes.shadeTextured(this.texture)), ctx, half, fade);
         if (this.glowTexture != null) {
            float g = this.glowAlpha * Mth.clamp(1.0F - ((float)this.age + ctx.partialTick) / ((float)this.life * 0.5F), 0.0F, 1.0F);
            if (g > 0.01F) {
               this.quad(ctx.buffers.getBuffer(VfxRenderTypes.glowTextured(this.glowTexture)), ctx, half, g);
            }
         }
      }
   }

   private void quad(VertexConsumer vc, VfxContext ctx, float half, float alpha) {
      Matrix4f m = ctx.matrix;
      float cx = ctx.rx(this.x);
      float cy = ctx.ry(this.y);
      float cz = ctx.rz(this.z);
      float ca = Mth.cos(this.angle) * half;
      float sa = Mth.sin(this.angle) * half;
      int a = Mth.clamp((int)(alpha * 255.0F), 0, 255);
      vc.addVertex(m, cx - ca + sa, cy, cz - sa - ca).setUv(0.0F, 0.0F).setColor(255, 255, 255, a);
      vc.addVertex(m, cx - ca - sa, cy, cz - sa + ca).setUv(0.0F, 1.0F).setColor(255, 255, 255, a);
      vc.addVertex(m, cx + ca - sa, cy, cz + sa + ca).setUv(1.0F, 1.0F).setColor(255, 255, 255, a);
      vc.addVertex(m, cx + ca + sa, cy, cz + sa - ca).setUv(1.0F, 0.0F).setColor(255, 255, 255, a);
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
      return 80.0;
   }
}
