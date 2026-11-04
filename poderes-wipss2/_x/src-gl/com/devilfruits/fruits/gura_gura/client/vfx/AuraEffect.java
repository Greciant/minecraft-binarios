package com.devilfruits.fruits.gura_gura.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.CrackGeometry;
import com.devilfruits.client.render.VfxDraw;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.client.sound.LoopSound;
import com.devilfruits.fruits.gura_gura.GuraParticles;
import com.devilfruits.fruits.gura_gura.GuraSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

public class AuraEffect extends VfxEffect {
   private static final int PATCHES = 6;
   private final int entityId;
   private final float[][] burstShapes = new float[4][];
   private final float[] patchDirX = new float[6];
   private final float[] patchDirY = new float[6];
   private final float[] patchDirZ = new float[6];
   private final int[] patchBirth = new int[6];
   private final int[] patchShape = new int[6];
   private boolean stopped;
   private int stopAge;
   private double x;
   private double y;
   private double z;
   private static final Vector3f U = new Vector3f(1.0F, 0.0F, 0.0F);
   private static final Vector3f V = new Vector3f(0.0F, 0.0F, 1.0F);
   private static final Vector3f N = new Vector3f();
   private static final Vector3f AU = new Vector3f();
   private static final Vector3f AV = new Vector3f();
   private static final Vector3f AX = new Vector3f();
   private static final Vector3f AY = new Vector3f();
   private static final Vector3f AZ = new Vector3f();

   public AuraEffect(Entity entity) {
      this.entityId = entity.getId();

      for (int i = 0; i < this.burstShapes.length; i++) {
         this.burstShapes[i] = CrackGeometry.burst((long)entity.getId() * 31L + (long)i, 4 + i, 0.3F);
      }

      for (int i = 0; i < 6; i++) {
         this.patchBirth[i] = -100;
      }

      this.x = entity.getX();
      this.y = entity.getY();
      this.z = entity.getZ();
      Minecraft.getInstance()
         .getSoundManager()
         .play(new LoopSound(GuraSounds.AURA_LOOP.get(), entity, entity.position(), 0.55F, 1.0F, 0, () -> !this.isDone() && !this.stopped));
   }

   public int entityId() {
      return this.entityId;
   }

   public void stop() {
      if (!this.stopped) {
         this.stopped = true;
         this.stopAge = this.age;
      }
   }

   private float alpha(float partial) {
      float a = Mth.clamp(((float)this.age + partial) / 6.0F, 0.0F, 1.0F);
      if (this.stopped) {
         a *= 1.0F - Mth.clamp(((float)(this.age - this.stopAge) + partial) / 6.0F, 0.0F, 1.0F);
      }

      return a;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity entity = level.getEntity(this.entityId);
      if (entity != null && (!this.stopped || this.age - this.stopAge <= 6)) {
         this.x = entity.getX();
         this.y = entity.getY();
         this.z = entity.getZ();
         if (!this.stopped) {
            if (this.age % 3 == 0) {
               int slot = this.age / 3 % 6;
               float theta = Fx.random().nextFloat() * (float) (Math.PI * 2);
               float h = Fx.random().nextFloat() * 2.0F - 1.0F;
               float s = (float)Math.sqrt((double)(1.0F - h * h));
               this.patchDirX[slot] = s * Mth.cos(theta);
               this.patchDirY[slot] = h;
               this.patchDirZ[slot] = s * Mth.sin(theta);
               this.patchBirth[slot] = this.age;
               this.patchShape[slot] = Fx.random().nextInt(this.burstShapes.length);
            }

            int n = Fx.count(2);

            for (int i = 0; i < n; i++) {
               double a = Fx.random().nextDouble() * Math.PI * 2.0;
               double r = 0.6 + Fx.random().nextDouble() * 0.3;
               Fx.spawn(
                  (ParticleOptions)GuraParticles.CRACK_SPARK.get(),
                  this.x + Math.cos(a) * r,
                  this.y + Fx.random().nextDouble() * (double)entity.getBbHeight(),
                  this.z + Math.sin(a) * r,
                  Math.cos(a) * 0.02,
                  0.01,
                  Math.sin(a) * 0.02
               );
            }
         }
      } else {
         this.finish();
      }
   }

   private boolean hiddenFor(Entity entity) {
      Minecraft mc = Minecraft.getInstance();
      return entity == mc.player && !mc.gameRenderer.getMainCamera().isDetached();
   }

   @Override
   public void renderShell(VfxContext ctx) {
      Entity entity = Minecraft.getInstance().level.getEntity(this.entityId);
      if (entity != null && !this.hiddenFor(entity)) {
         double px = Mth.lerp((double)ctx.partialTick, entity.xo, entity.getX());
         double py = Mth.lerp((double)ctx.partialTick, entity.yo, entity.getY());
         double pz = Mth.lerp((double)ctx.partialTick, entity.zo, entity.getZ());
         float h = entity.getBbHeight();
         float a = this.alpha(ctx.partialTick);
         float vib = 1.0F + 0.035F * Mth.sin(ctx.time * 9.1F) + 0.02F * Mth.sin(ctx.time * 23.7F);
         float cx = ctx.rx(px);
         float cy = ctx.ry(py + (double)h * 0.5);
         float cz = ctx.rz(pz);
         VfxDraw.shell(ctx, cx, cy, cz, 0.78F * vib, h * 0.62F * vib, 0.78F * vib, 8, 14, 195, 235, 255, 0.015F * a, 0.2F * a);

         for (int k = 0; k < 3; k++) {
            float ry = ctx.ry(py) + h * (0.08F + 0.42F * (float)k);
            float jitter = 0.04F * Mth.sin(ctx.time * (11.0F + (float)k * 3.0F) + (float)k);
            VfxDraw.ring(ctx, cx, ry, cz, U, V, 0.9F + jitter, 0.22F, 32, 225, 245, 255, 0.28F * a);
         }
      }
   }

   @Override
   public void renderGlow(VfxContext ctx) {
      Entity entity = Minecraft.getInstance().level.getEntity(this.entityId);
      if (entity != null && !this.hiddenFor(entity)) {
         double px = Mth.lerp((double)ctx.partialTick, entity.xo, entity.getX());
         double py = Mth.lerp((double)ctx.partialTick, entity.yo, entity.getY());
         double pz = Mth.lerp((double)ctx.partialTick, entity.zo, entity.getZ());
         float h = entity.getBbHeight();
         float a = this.alpha(ctx.partialTick);
         float cx = ctx.rx(px);
         float cy = ctx.ry(py + (double)h * 0.5);
         float cz = ctx.rz(pz);

         for (int k = 0; k < 3; k++) {
            float ry = ctx.ry(py) + h * (0.08F + 0.42F * (float)k);
            float jitter = 0.04F * Mth.sin(ctx.time * (11.0F + (float)k * 3.0F) + (float)k);
            VfxDraw.ring(ctx, cx, ry, cz, U, V, 0.9F + jitter, 0.05F, 32, 120, 214, 255, 0.35F * a);
         }

         for (int i = 0; i < 6; i++) {
            float life = ((float)this.age + ctx.partialTick - (float)this.patchBirth[i]) / 8.0F;
            if (!(life < 0.0F) && !(life > 1.0F)) {
               N.set(this.patchDirX[i], this.patchDirY[i], this.patchDirZ[i]);
               VfxDraw.basis(N, AU, AV);
               float qx = cx + this.patchDirX[i] * 0.78F;
               float qy = cy + this.patchDirY[i] * h * 0.62F;
               float qz = cz + this.patchDirZ[i] * 0.78F;
               float size = 0.28F;
               AX.set(AU).mul(size);
               AY.set(AV).mul(size);
               AZ.set(N).mul(size);
               QuakeDraw.cracks(
                  ctx,
                  this.burstShapes[this.patchShape[i]],
                  qx,
                  qy,
                  qz,
                  AX,
                  AY,
                  AZ,
                  QuakeDraw.Orientation.PLANE,
                  Math.min(1.0F, life * 3.0F),
                  0.012F,
                  0.05F,
                  a * (1.0F - life)
               );
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
