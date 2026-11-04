package com.devilfruits.fruits.kage_kage.client.vfx;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class ShadowPool {
   private static int counter;
   private final int size;
   private final DynamicTexture dark;
   private final DynamicTexture glow;
   private final ResourceLocation darkId;
   private final ResourceLocation glowId;
   private final RandomSource random;
   private final float a1;
   private final float a2;
   private final float a3;
   public Vec3 centre;
   public float target;
   private float radius;
   private float velocity;
   private float pulse;
   private boolean sinking;
   private int age;
   private final List<float[]> ripples = new ArrayList<>();
   private boolean closed;

   public ShadowPool(Vec3 centre, float target, float maxRadius, long seed) {
      this.centre = centre;
      this.target = target;
      this.random = RandomSource.create(seed);
      this.size = Mth.clamp(Mth.ceil(maxRadius * 2.6F * 16.0F) + 2, 16, 224);
      this.dark = new DynamicTexture(new NativeImage(this.size, this.size, true));
      this.glow = new DynamicTexture(new NativeImage(this.size, this.size, true));
      int n = counter++;
      this.darkId = DevilFruits.id("dynamic/kage_pool_" + n);
      this.glowId = DevilFruits.id("dynamic/kage_pool_glow_" + n);
      Minecraft.getInstance().getTextureManager().register(this.darkId, this.dark);
      Minecraft.getInstance().getTextureManager().register(this.glowId, this.glow);
      this.a1 = this.random.nextFloat() * (float) (Math.PI * 2);
      this.a2 = this.random.nextFloat() * (float) (Math.PI * 2);
      this.a3 = this.random.nextFloat() * (float) (Math.PI * 2);
   }

   public float radius() {
      return this.radius;
   }

   public boolean gone() {
      return this.sinking && this.radius <= 0.02F;
   }

   public void pulse(float strength) {
      this.pulse = Math.max(this.pulse, strength);
      this.ripples.add(new float[]{0.1F, 0.5F + strength * 0.5F});
      this.velocity += 0.06F * strength;
   }

   public void sink() {
      this.sinking = true;
   }

   public void tick() {
      this.age++;
      float goal = this.sinking ? 0.0F : this.target;
      this.velocity = this.velocity + (goal - this.radius) * (this.sinking ? 0.12F : 0.32F);
      this.velocity = this.velocity * (this.sinking ? 0.7F : 0.62F);
      this.radius = Math.max(0.0F, this.radius + this.velocity);
      this.pulse *= 0.82F;
      if (!this.sinking && this.age % 9 == 1) {
         this.ripples.add(new float[]{0.05F, 0.35F});
      }

      for (float[] r : this.ripples) {
         r[0] += 0.075F + r[1] * 0.06F;
      }

      this.ripples.removeIf(rx -> rx[0] > this.radius);
      if (!this.closed) {
         this.paint();
      }

      if (!VfxSettings.legacy() && this.radius > 0.1F) {
         VfxParticles.emit(KageLook.FLOOR_GLOW, this.centre.x, this.centre.y + 0.03, this.centre.z, 0.0, 0.0, 0.0)
            .size(this.radius * 2.6F)
            .tint(12868576)
            .alpha(0.22F + this.pulse * 0.3F);
         if (this.random.nextFloat() < this.radius * 0.5F) {
            float a = this.random.nextFloat() * (float) (Math.PI * 2);
            VfxParticles.emit(
                  KageLook.MOTE,
                  this.centre.x + (double)(Mth.cos(a) * this.radius),
                  this.centre.y + 0.05,
                  this.centre.z + (double)(Mth.sin(a) * this.radius),
                  0.0,
                  0.01,
                  0.0
               )
               .tint(12868576);
         }
      }
   }

   private float edge(float a, float t) {
      float n = 0.07F * Mth.sin(3.0F * a + this.a1 + t * 0.09F)
         + 0.05F * Mth.sin(5.0F * a + this.a2 - t * 0.13F)
         + 0.03F * Mth.sin(9.0F * a + this.a3 + t * 0.21F);
      return this.radius * (1.0F + n);
   }

   private static int abgr(int alpha, int rgb) {
      return alpha << 24 | (rgb & 0xFF) << 16 | rgb & 0xFF00 | rgb >> 16 & 0xFF;
   }

   private void paint() {
      NativeImage d = this.dark.getPixels();
      NativeImage g = this.glow.getPixels();
      if (d != null && g != null) {
         float half = (float)this.size / 2.0F;
         float t = (float)this.age;
         float beat = 0.82F + 0.18F * Mth.sin(t * 0.33F) + this.pulse * 0.9F;
         boolean simple = VfxSettings.legacy();
         float px = 0.0625F;

         for (int y = 0; y < this.size; y++) {
            for (int x = 0; x < this.size; x++) {
               float dx = ((float)x + 0.5F - half) * px;
               float dz = ((float)y + 0.5F - half) * px;
               float r = Mth.sqrt(dx * dx + dz * dz);
               if (r > this.radius * 1.25F + px) {
                  d.setPixelRGBA(x, y, 0);
                  g.setPixelRGBA(x, y, 0);
               } else {
                  float a = (float)Mth.atan2((double)dz, (double)dx);
                  float e = this.edge(a, t);
                  float in = e - r;
                  if (in < 0.0F) {
                     d.setPixelRGBA(x, y, 0);
                     g.setPixelRGBA(x, y, 0);
                  } else {
                     int alpha = in < px * 1.5F ? 255 : (int)(232.0F - 30.0F * Mth.clamp(1.0F - r / Math.max(0.1F, e), 0.0F, 1.0F));
                     d.setPixelRGBA(x, y, abgr(alpha, 459788));
                     float heat = 0.0F;
                     if (in < px * 1.01F) {
                        heat = 1.25F * beat;
                     } else if (in < px * 2.01F) {
                        heat = 0.45F * beat;
                     }

                     if (!simple) {
                        for (float[] rp : this.ripples) {
                           if (Math.abs(r - rp[0]) < px * 0.6F) {
                              heat = Math.max(heat, rp[1] * (1.0F - rp[0] / Math.max(0.1F, e)));
                           }
                        }

                        float sw = Mth.sin(3.0F * a + r * 7.0F - t * 0.22F);
                        if (sw > 0.96F && in > px * 3.0F) {
                           heat = Math.max(heat, 0.16F);
                        }
                     }

                     g.setPixelRGBA(x, y, heat <= 0.01F ? 0 : abgr(255, KageLook.scale(KageLook.rim(Math.min(2.0F, heat)), Math.min(1.0F, heat))));
                  }
               }
            }
         }

         this.dark.upload();
         this.glow.upload();
      }
   }

   public void draw(boolean glowPass, PoseStack ps, MultiBufferSource buffers, Vec3 cam, int light) {
      if (!this.closed && !(this.radius <= 0.01F)) {
         float h = (float)this.size / 32.0F;
         ps.pushPose();
         ps.translate(this.centre.x - cam.x, this.centre.y - cam.y + (glowPass ? 0.018 : 0.014), this.centre.z - cam.z);
         Matrix4f m = ps.last().pose();
         VertexConsumer vc = buffers.getBuffer(glowPass ? RenderType.eyes(this.glowId) : RenderType.entityTranslucent(this.darkId));
         int l = glowPass ? 15728880 : light;
         vertex(vc, m, -h, -h, 0.0F, 0.0F, l);
         vertex(vc, m, -h, h, 0.0F, 1.0F, l);
         vertex(vc, m, h, h, 1.0F, 1.0F, l);
         vertex(vc, m, h, -h, 1.0F, 0.0F, l);
         ps.popPose();
      }
   }

   private static void vertex(VertexConsumer vc, Matrix4f m, float x, float z, float u, float v, int light) {
      Vector3f p = m.transformPosition(x, 0.0F, z, new Vector3f());
      vc.addVertex(p.x, p.y, p.z, -1, u, v, OverlayTexture.NO_OVERLAY, light, 0.0F, 1.0F, 0.0F);
   }

   public void close() {
      if (!this.closed) {
         this.closed = true;
         Minecraft.getInstance().getTextureManager().release(this.darkId);
         Minecraft.getInstance().getTextureManager().release(this.glowId);
      }
   }
}
