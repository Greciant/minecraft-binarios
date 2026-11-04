package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.VfxDraw;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class TwisterEffect extends VfxEffect {
   private static final int STRANDS = 6;
   private static final int STEPS = 30;
   private static final double ABOVE = 6.0;
   private static final double BELOW = 28.0;
   private static final Vector3f A = new Vector3f();
   private static final Vector3f B = new Vector3f();
   private static final Vector3f W = new Vector3f();
   private final int casterId;
   private final float radius;
   private final int maxTicks;
   private final long seed;
   private int stoppedAt = -1;
   private double x;
   private double y;
   private double z;
   private double px;
   private double py;
   private double pz;
   private double floor;
   private double prevFloor;

   public TwisterEffect(Entity caster, float radius, int maxTicks, long seed) {
      this.casterId = caster.getId();
      this.radius = radius;
      this.maxTicks = maxTicks;
      this.seed = seed;
      this.x = this.px = caster.getX();
      this.y = this.py = caster.getY();
      this.z = this.pz = caster.getZ();
      this.floor = this.prevFloor = this.y - 28.0;
   }

   public int casterId() {
      return this.casterId;
   }

   public void stop() {
      if (this.stoppedAt < 0) {
         this.stoppedAt = this.age;
      }
   }

   private float strength(float t) {
      float in = Math.min(1.0F, t / 10.0F);
      return this.stoppedAt < 0 ? in : in * Math.max(0.0F, 1.0F - (t - (float)this.stoppedAt) / 12.0F);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity caster = level.getEntity(this.casterId);
      if (caster != null && (this.stoppedAt < 0 || this.age - this.stoppedAt <= 12) && this.age <= this.maxTicks + 40) {
         this.px = this.x;
         this.py = this.y;
         this.pz = this.z;
         this.prevFloor = this.floor;
         this.x = caster.getX();
         this.y = caster.getY();
         this.z = caster.getZ();
         this.floor = Math.max(this.y - 28.0, (double)level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(this.x), Mth.floor(this.z)));
         float s = this.strength((float)this.age);
         if (!(s <= 0.05F)) {
            double top = this.y + 6.0;
            int n = Fx.count(3);

            for (int i = 0; i < n; i++) {
               float f = Fx.random().nextFloat();
               double h = Mth.lerp((double)f, this.floor, top);
               double r = (double)this.funnel(f) * (0.8 + (double)Fx.random().nextFloat() * 0.3);
               float a = Fx.random().nextFloat() * (float) (Math.PI * 2);
               double c = (double)Mth.cos(a);
               double sn = (double)Mth.sin(a);
               Fx.spawnFar((ParticleOptions)SeiryuParticles.CLOUD.get(), this.x + c * r, h, this.z + sn * r, -sn * 0.45, 0.12, c * 0.45);
            }

            if (this.floor > this.y - 28.0 + 0.5) {
               for (int i = 0; i < Fx.count(3); i++) {
                  float a = Fx.random().nextFloat() * (float) (Math.PI * 2);
                  double r = (double)(this.funnel(0.0F) * (1.0F + Fx.random().nextFloat()));
                  Fx.spawnFar(
                     (ParticleOptions)SeiryuParticles.CLOUD.get(),
                     this.x + (double)Mth.cos(a) * r,
                     this.floor + 0.2,
                     this.z + (double)Mth.sin(a) * r,
                     (double)(-Mth.sin(a)) * 0.35 + (double)Mth.cos(a) * 0.1,
                     0.05,
                     (double)Mth.cos(a) * 0.35 + (double)Mth.sin(a) * 0.1
                  );
               }
            }

            if (this.age % 5 == 0) {
               float a0 = Fx.random().nextFloat() * (float) (Math.PI * 2);
               float f0 = Fx.random().nextFloat();
               float f1 = Mth.clamp(f0 + 0.2F + Fx.random().nextFloat() * 0.2F, 0.0F, 1.0F);
               double r0 = (double)this.funnel(f0) * 0.8;
               double r1 = (double)this.funnel(f1) * 0.8;
               VfxManager.add(
                  new LightningEffect(
                        new Vec3(this.x + (double)Mth.cos(a0) * r0, Mth.lerp((double)f0, this.floor, top), this.z + (double)Mth.sin(a0) * r0),
                        new Vec3(this.x + (double)Mth.cos(a0 + 1.4F) * r1, Mth.lerp((double)f1, this.floor, top), this.z + (double)Mth.sin(a0 + 1.4F) * r1),
                        6,
                        0.07F,
                        1,
                        this.seed + (long)this.age
                     )
                     .quiet()
               );
            }
         }
      } else {
         this.finish();
      }
   }

   private float funnel(float f) {
      return this.radius * (0.3F + 0.7F * f * f);
   }

   @Override
   public void renderGlow(VfxContext ctx) {
      float t = (float)this.age + ctx.partialTick;
      float s = this.strength(t);
      if (!(s <= 0.01F)) {
         float cx = ctx.rx(Mth.lerp((double)ctx.partialTick, this.px, this.x));
         float cz = ctx.rz(Mth.lerp((double)ctx.partialTick, this.pz, this.z));
         float bottom = ctx.ry(Mth.lerp((double)ctx.partialTick, this.prevFloor, this.floor));
         float top = ctx.ry(Mth.lerp((double)ctx.partialTick, this.py, this.y) + 6.0);
         float spin = t * 0.45F;

         for (int k = 0; k < 6; k++) {
            float offset = (float)k * (float) (Math.PI * 2) / 6.0F;
            float rk = 0.85F + 0.3F * (float)(k * 37 % 7) / 7.0F;

            for (int i = 0; i < 30; i++) {
               float f0 = (float)i / 30.0F;
               float f1 = (float)(i + 1) / 30.0F;
               this.point(A, cx, cz, bottom, top, f0, spin + offset, rk);
               this.point(B, cx, cz, bottom, top, f1, spin + offset, rk);
               W.set(B).sub(A).cross(A.x + B.x, A.y + B.y, A.z + B.z);
               if (!(W.lengthSquared() < 1.0E-8F)) {
                  W.normalize();
                  float along = Mth.sin(f0 * (float) Math.PI) * (0.6F + 0.4F * Mth.sin(f0 * 9.0F - t * 0.6F + (float)k));
                  int alpha = (int)(Mth.clamp(along, 0.0F, 1.0F) * s * 70.0F);
                  if (alpha > 2) {
                     float w = 0.25F + 0.5F * f0;
                     VfxDraw.ribbon(ctx.buffer, ctx.matrix, A, B, W, w, 200, 228, 255, alpha);
                     VfxDraw.ribbon(ctx.buffer, ctx.matrix, A, B, W, w * 0.3F, 255, 255, 255, alpha);
                  }
               }
            }
         }
      }
   }

   private void point(Vector3f out, float cx, float cz, float bottom, float top, float f, float spin, float rk) {
      float angle = spin + f * (float) Math.PI * 3.5F;
      float r = this.funnel(f) * rk;
      out.set(cx + Mth.cos(angle) * r, Mth.lerp(f, bottom, top), cz + Mth.sin(angle) * r);
   }

   @Override
   public double x() {
      return this.x;
   }

   @Override
   public double y() {
      return this.y - 11.200000000000001;
   }

   @Override
   public double z() {
      return this.z;
   }

   @Override
   public double cullDistance() {
      return 192.0;
   }
}
