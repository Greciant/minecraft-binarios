package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.VfxRenderTypes;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuParticles;
import com.devilfruits.fruits.uo_uo_seiryu.client.render.HybridAnchors;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;

public class AnchorTrailEffect extends VfxEffect {
   private static final ResourceLocation STRIP = DevilFruits.id("textures/misc/uo_uo_seiryu/breath_beam.png");
   private static final int SAMPLES = 16;
   private final int entityId;
   private final int anchor;
   private final int duration;
   private final float width;
   private final double[] px = new double[16];
   private final double[] py = new double[16];
   private final double[] pz = new double[16];
   private int count;
   private int head = -1;
   private int length = 10;
   private final Vector3d at = new Vector3d();

   public AnchorTrailEffect(Entity entity, int anchor, int duration, float width) {
      this.entityId = entity.getId();
      this.anchor = anchor;
      this.duration = duration;
      this.width = width;
   }

   public AnchorTrailEffect length(int samples) {
      this.length = Mth.clamp(samples, 2, 16);
      return this;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = level.getEntity(this.entityId);
      if (this.age > this.duration + this.length) {
         this.finish();
      } else {
         if (e != null && this.age <= this.duration) {
            HybridAnchors.get(e, this.anchor, 1.0F, this.at);
            this.head = (this.head + 1) % 16;
            this.px[this.head] = this.at.x;
            this.py[this.head] = this.at.y;
            this.pz[this.head] = this.at.z;
            this.count = Math.min(this.length, this.count + 1);
            if (this.count > 2 && Fx.random().nextFloat() < 0.6F) {
               Fx.spawn(
                  (ParticleOptions)SeiryuParticles.EMBER.get(),
                  this.at.x + Fx.spread(0.3),
                  this.at.y + Fx.spread(0.3),
                  this.at.z + Fx.spread(0.3),
                  Fx.spread(0.05),
                  -0.02,
                  Fx.spread(0.05)
               );
            }
         } else {
            this.count = Math.max(0, this.count - 1);
         }
      }
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      if (this.count >= 2) {
         VertexConsumer vc = ctx.buffers.getBuffer(VfxRenderTypes.glowTextured(STRIP));
         float scroll = -((float)this.age + ctx.partialTick) * 0.3F;

         for (int k = 0; k + 1 < this.count; k++) {
            int i = Math.floorMod(this.head - k, 16);
            int j = Math.floorMod(this.head - k - 1, 16);
            float t0 = (float)k / (float)(this.count - 1);
            float t1 = (float)(k + 1) / (float)(this.count - 1);
            float w0 = this.width * (1.0F - t0 * 0.85F);
            float w1 = this.width * (1.0F - t1 * 0.85F);
            float ax = ctx.rx(this.px[i]);
            float ay = ctx.ry(this.py[i]);
            float az = ctx.rz(this.pz[i]);
            float bx = ctx.rx(this.px[j]);
            float by = ctx.ry(this.py[j]);
            float bz = ctx.rz(this.pz[j]);
            float a0 = 1.0F - t0;
            SeiryuDraw.texturedBeam(vc, ctx.matrix, ax, ay, az, bx, by, bz, w0, w1, scroll + (float)k * 0.2F, 0.2F, 15217182, 0.55F * a0);
            SeiryuDraw.texturedBeam(vc, ctx.matrix, ax, ay, az, bx, by, bz, w0 * 0.6F, w1 * 0.6F, scroll * 1.4F + (float)k * 0.2F, 0.2F, 16742938, 0.8F * a0);
            if (t0 < 0.5F) {
               SeiryuDraw.texturedBeam(vc, ctx.matrix, ax, ay, az, bx, by, bz, w0 * 0.25F, w1 * 0.25F, scroll * 2.0F, 0.2F, 16774338, a0);
            }
         }
      }
   }

   @Override
   public double x() {
      return this.head < 0 ? 0.0 : this.px[this.head];
   }

   @Override
   public double y() {
      return this.head < 0 ? 0.0 : this.py[this.head];
   }

   @Override
   public double z() {
      return this.head < 0 ? 0.0 : this.pz[this.head];
   }
}
