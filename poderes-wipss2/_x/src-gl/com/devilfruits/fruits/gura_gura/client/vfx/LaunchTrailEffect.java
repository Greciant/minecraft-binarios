package com.devilfruits.fruits.gura_gura.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.VfxDraw;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.core.util.DeterministicRandom;
import com.devilfruits.fruits.gura_gura.GuraParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

public class LaunchTrailEffect extends VfxEffect {
   private static final int HISTORY = 12;
   private final int entityId;
   private final int duration;
   private final double[] hx = new double[12];
   private final double[] hy = new double[12];
   private final double[] hz = new double[12];
   private final float[] jitter = new float[36];
   private int count;
   private double x;
   private double y;
   private double z;
   private static final Vector3f A = new Vector3f();
   private static final Vector3f B = new Vector3f();
   private static final Vector3f D = new Vector3f();
   private static final Vector3f W = new Vector3f();

   public LaunchTrailEffect(Entity entity, int duration, long seed) {
      this.entityId = entity.getId();
      this.duration = duration;
      DeterministicRandom r = new DeterministicRandom(seed);

      for (int i = 0; i < this.jitter.length; i++) {
         this.jitter[i] = r.signed(0.35F);
      }

      this.x = entity.getX();
      this.y = entity.getY();
      this.z = entity.getZ();
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity entity = level.getEntity(this.entityId);
      if (entity != null && this.age <= this.duration + 12) {
         if (this.age <= this.duration) {
            for (int i = 11; i > 0; i--) {
               this.hx[i] = this.hx[i - 1];
               this.hy[i] = this.hy[i - 1];
               this.hz[i] = this.hz[i - 1];
            }

            this.x = entity.getX();
            this.y = entity.getY() + (double)entity.getBbHeight() * 0.5;
            this.z = entity.getZ();
            this.hx[0] = this.x;
            this.hy[0] = this.y;
            this.hz[0] = this.z;
            this.count = Math.min(12, this.count + 1);
            int n = Fx.count(3);

            for (int i = 0; i < n; i++) {
               Fx.spawn(
                  i % 2 == 0 ? (ParticleOptions)GuraParticles.GLASS_SHARD.get() : (ParticleOptions)GuraParticles.CRACK_SPARK.get(),
                  this.x + Fx.spread(0.4),
                  this.y + Fx.spread(0.4),
                  this.z + Fx.spread(0.4),
                  Fx.spread(0.05),
                  Fx.spread(0.05),
                  Fx.spread(0.05)
               );
            }
         } else {
            this.count = Math.max(0, this.count - 1);
         }
      } else {
         this.finish();
      }
   }

   @Override
   public void renderGlow(VfxContext ctx) {
      if (this.count >= 2 && Minecraft.getInstance().level != null) {
         for (int i = 1; i < this.count; i++) {
            float fade = 1.0F - (float)i / 12.0F;
            float j0 = i == 1 ? 0.0F : 1.0F;
            A.set(
               ctx.rx(this.hx[i - 1]) + this.jitter[(i - 1) * 3] * j0,
               ctx.ry(this.hy[i - 1]) + this.jitter[(i - 1) * 3 + 1] * j0,
               ctx.rz(this.hz[i - 1]) + this.jitter[(i - 1) * 3 + 2] * j0
            );
            B.set(ctx.rx(this.hx[i]) + this.jitter[i * 3], ctx.ry(this.hy[i]) + this.jitter[i * 3 + 1], ctx.rz(this.hz[i]) + this.jitter[i * 3 + 2]);
            D.set(B).sub(A);
            W.set(D).cross(A.x + B.x, A.y + B.y, A.z + B.z);
            if (!(W.lengthSquared() < 1.0E-8F)) {
               W.normalize();
               float flicker = 0.75F + 0.25F * Mth.sin(ctx.time * 3.0F + (float)i);
               VfxDraw.ribbon(ctx.buffer, ctx.matrix, A, B, W, 0.22F * fade, 120, 214, 255, (int)(110.0F * fade * flicker));
               VfxDraw.ribbon(ctx.buffer, ctx.matrix, A, B, W, 0.05F * fade + 0.015F, 255, 255, 255, (int)(255.0F * fade * flicker));
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
