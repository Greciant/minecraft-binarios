package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuParticles;
import com.devilfruits.registry.DFSounds;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class StormCloudEffect extends VfxEffect {
   private static final int SEGMENTS = 28;
   private static final int RINGS = 4;
   private final int casterId;
   private final float radius;
   private final int duration;
   private final double up;
   private int stoppedAt = -1;
   private double cx;
   private double cz;
   private double cy;
   private double pcx;
   private double pcz;
   private double pcy;
   private int flashAge = 100;
   private double fx;
   private double fz;

   public StormCloudEffect(Entity caster, Vec3 cloud, float radius, int duration) {
      this.casterId = caster.getId();
      this.radius = radius;
      this.duration = duration;
      this.up = cloud.y - caster.getY();
      this.cx = this.pcx = cloud.x;
      this.cz = this.pcz = cloud.z;
      this.cy = this.pcy = cloud.y;
   }

   public int casterId() {
      return this.casterId;
   }

   public void stop() {
      if (this.stoppedAt < 0) {
         this.stoppedAt = this.age;
      }
   }

   public void flash(Vec3 at) {
      this.flashAge = 0;
      this.fx = at.x;
      this.fz = at.z;
   }

   private float cover(float t) {
      float in = Math.min(1.0F, t / 16.0F);
      float end = this.stoppedAt >= 0 ? (float)this.stoppedAt : (float)this.duration;
      return in * Mth.clamp(1.0F - (t - end) / 40.0F, 0.0F, 1.0F);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      int end = this.stoppedAt >= 0 ? this.stoppedAt : this.duration;
      if (this.age > end + 45) {
         this.finish();
      } else {
         this.pcx = this.cx;
         this.pcz = this.cz;
         this.pcy = this.cy;
         this.flashAge++;
         Entity caster = level.getEntity(this.casterId);
         if (caster != null && this.age <= end) {
            this.cx = this.cx + (caster.getX() - this.cx) * 0.08;
            this.cz = this.cz + (caster.getZ() - this.cz) * 0.08;
            this.cy = this.cy + (caster.getY() + this.up - this.cy) * 0.1;
         }

         if (this.age <= end - 30) {
            int n = Fx.count(this.age < 16 ? 6 : 3);

            for (int i = 0; i < n; i++) {
               float a = Fx.random().nextFloat() * (float) (Math.PI * 2);
               double r = (double)this.radius * 1.1 * Math.sqrt((double)Fx.random().nextFloat());
               double px = this.cx + (double)Mth.cos(a) * r;
               double pz = this.cz + (double)Mth.sin(a) * r;
               Fx.spawnFar(
                  (ParticleOptions)SeiryuParticles.STORM_CLOUD.get(),
                  px,
                  this.cy + Fx.spread(1.6),
                  pz,
                  (double)(-Mth.sin(a)) * 0.04,
                  0.0,
                  (double)Mth.cos(a) * 0.04
               );
            }

            if (Fx.random().nextInt(14) == 0) {
               this.flashAge = 0;
               this.fx = this.cx + Fx.spread((double)this.radius);
               this.fz = this.cz + Fx.spread((double)this.radius);
            }

            if (this.age % 35 == 12) {
               SoundDesign.layerOnly(new Vec3(this.cx, this.cy, this.cz), DFSounds.LAYER_THUNDER, 0.7F, 0.55F + Fx.random().nextFloat() * 0.2F);
            }
         }
      }
   }

   @Override
   public void renderShell(VfxContext ctx) {
      float t = (float)this.age + ctx.partialTick;
      float cover = this.cover(t);
      if (!(cover <= 0.01F)) {
         float x = ctx.rx(Mth.lerp((double)ctx.partialTick, this.pcx, this.cx));
         float y = ctx.ry(Mth.lerp((double)ctx.partialTick, this.pcy, this.cy) - 1.4);
         float z = ctx.rz(Mth.lerp((double)ctx.partialTick, this.pcz, this.cz));
         VertexConsumer vc = ctx.buffer;
         Matrix4f m = ctx.matrix;
         float turn = t * 0.004F;

         for (int ring = 0; ring < 4; ring++) {
            float r0 = this.radius * 1.15F * (float)ring / 4.0F;
            float r1 = this.radius * 1.15F * (float)(ring + 1) / 4.0F;
            int a0 = (int)(cover * 120.0F * (1.0F - (float)ring / 4.0F));
            int a1 = (int)(cover * 120.0F * (1.0F - (float)(ring + 1) / 4.0F));

            for (int i = 0; i < 28; i++) {
               float b0 = turn + (float) (Math.PI * 2) * (float)i / 28.0F;
               float b1 = turn + (float) (Math.PI * 2) * (float)(i + 1) / 28.0F;
               float c0 = Mth.cos(b0);
               float s0 = Mth.sin(b0);
               float c1 = Mth.cos(b1);
               float s1 = Mth.sin(b1);
               vc.addVertex(m, x + c0 * r0, y, z + s0 * r0).setColor(34, 38, 51, a0);
               vc.addVertex(m, x + c0 * r1, y, z + s0 * r1).setColor(34, 38, 51, a1);
               vc.addVertex(m, x + c1 * r1, y, z + s1 * r1).setColor(34, 38, 51, a1);
               vc.addVertex(m, x + c1 * r0, y, z + s1 * r0).setColor(34, 38, 51, a0);
            }
         }
      }
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      if (this.flashAge <= 5) {
         float f = 1.0F - ((float)this.flashAge + ctx.partialTick) / 6.0F;
         float cover = this.cover((float)this.age + ctx.partialTick);
         if (!(f <= 0.0F) && !(cover <= 0.01F)) {
            float y = ctx.ry(Mth.lerp((double)ctx.partialTick, this.pcy, this.cy) - 0.5);
            SeiryuDraw.flare(ctx, ctx.rx(this.fx), y, ctx.rz(this.fz), this.radius * 0.5F, 16777215, 11955455, 0.5F * f * cover);
         }
      }
   }

   @Override
   public double x() {
      return this.cx;
   }

   @Override
   public double y() {
      return this.cy;
   }

   @Override
   public double z() {
      return this.cz;
   }

   @Override
   public double cullDistance() {
      return 256.0;
   }
}
