package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.render.MeshTex;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class EncierroCinematic extends SandCinematic {
   private static final int BANDS = 4;
   private static final int STEPS = 24;
   private static final int ARM = 12;
   private final int victimId;
   private final int maxAge;
   private int releasedAt = -1;
   private double vx;
   private double vy;
   private double vz;
   private float vw = 0.6F;
   private float vh = 1.8F;
   private final double[] px = new double[24];
   private final double[] py = new double[24];
   private final double[] pz = new double[24];
   private final float[] ax = new float[24];
   private final float[] ay = new float[24];
   private final float[] az = new float[24];
   private final float[] w = new float[24];
   private final float[] a = new float[24];

   public EncierroCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.victimId = params.extra();
      this.maxAge = Math.round(params.power()) + 30;
      Entity v = level.getEntity(this.victimId);
      if (v != null) {
         this.vx = v.getX();
         this.vy = v.getY();
         this.vz = v.getZ();
         this.vw = v.getBbWidth();
         this.vh = v.getBbHeight();
         int tint = lit(level, this.vx, this.vy + 1.0, this.vz);
         burst(this.random, v.getBoundingBox().getCenter(), new Vec3(0.0, 0.3, 0.0), 1.0F, 0.25F, 14.0F, 6.0F, tint);
      }
   }

   @Nullable
   public static EncierroCinematic of(long seed) {
      return VfxDirector.find(EncierroCinematic.class, c -> c.seed == seed);
   }

   public void release() {
      if (this.releasedAt < 0) {
         this.releasedAt = this.age;
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            int tint = lit(level, this.vx, this.vy + 1.0, this.vz);

            for (int i = 0; i < VfxSettings.count(18.0F); i++) {
               double ang = this.random.nextDouble() * (float) (Math.PI * 2);
               double h = this.random.nextDouble() * (double)this.vh;
               double r = (double)this.vw * 0.5 + 0.35;
               grains(
                  this.random, this.vx + Math.cos(ang) * r, this.vy + h, this.vz + Math.sin(ang) * r, Math.cos(ang) * 0.06, -0.05, Math.sin(ang) * 0.06, tint
               );
               if (i % 3 == 0) {
                  puff(
                        this.random,
                        this.vx + Math.cos(ang) * r,
                        this.vy + h,
                        this.vz + Math.sin(ang) * r,
                        Math.cos(ang) * 0.05,
                        -0.02,
                        Math.sin(ang) * 0.05,
                        tint
                     )
                     .size(1.1F);
               }
            }

            decal(this.random, this.vx, this.vy, this.vz, 2.2F, tint);
         }
      }
   }

   @Nullable
   private Entity victim(ClientLevel level) {
      Entity e = level.getEntity(this.victimId);
      return e != null && e.isAlive() ? e : null;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity caster = this.caster(level);
      Entity victim = this.victim(level);
      if (caster != null && this.age <= this.maxAge && (this.releasedAt < 0 || this.age - this.releasedAt <= 8)) {
         if (victim == null) {
            this.release();
         } else {
            this.vx = victim.getX();
            this.vy = victim.getY();
            this.vz = victim.getZ();
            this.vw = victim.getBbWidth();
            this.vh = victim.getBbHeight();
            if (this.releasedAt < 0) {
               int tint = lit(level, this.vx, this.vy + 1.0, this.vz);
               Vec3 hand = SandGatherEffect.hand(caster, 1.0F);
               Vec3 body = victim.getBoundingBox().getCenter();
               int n = VfxSettings.count(3.0F);

               for (int i = 0; i < n; i++) {
                  double k = this.random.nextDouble() * 0.3;
                  Vec3 p = body.lerp(hand, k).add(this.spread((double)this.vw * 0.4), this.spread((double)this.vh * 0.3), this.spread((double)this.vw * 0.4));
                  Vec3 v = hand.subtract(p).scale(0.11);
                  VfxParticles.emit(VfxPresets.GLASS_GLINT, p.x, p.y, p.z, v.x, v.y + 0.02, v.z).tint(13625599).alpha(0.9F).life(0.9F);
                  if (i == 0) {
                     VfxParticles.emit(VfxPresets.SMOKE, p.x, p.y, p.z, v.x * 0.8, v.y * 0.8 + 0.01, v.z * 0.8)
                        .tint(15266036)
                        .alpha(0.14F)
                        .size(0.4F)
                        .life(0.35F);
                  }
               }

               for (int ix = 0; ix < VfxSettings.count(3.0F); ix++) {
                  double ang = this.random.nextDouble() * (float) (Math.PI * 2);
                  double h = this.random.nextDouble() * (double)this.vh;
                  double r = (double)this.vw * 0.5 + 0.45;
                  puff(
                        this.random,
                        this.vx + Math.cos(ang) * r,
                        this.vy + h,
                        this.vz + Math.sin(ang) * r,
                        -Math.sin(ang) * 0.14,
                        0.02,
                        Math.cos(ang) * 0.14,
                        tint
                     )
                     .size(1.0F);
               }

               if (this.age % 3 == 0) {
                  grains(
                     this.random,
                     this.vx + this.spread((double)this.vw * 0.5),
                     this.vy + this.random.nextDouble() * (double)this.vh,
                     this.vz + this.spread((double)this.vw * 0.5),
                     this.spread(0.03),
                     0.01,
                     this.spread(0.03),
                     scale(tint, 0.8F)
                  );
               }
            }
         }
      } else {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      ClientLevel level = Minecraft.getInstance().level;
      Entity caster = this.caster();
      Entity victim = level == null ? null : this.victim(level);
      if (level != null && caster != null && victim != null) {
         float pt = f.entityPartial;
         float t = (float)this.age + f.partial;
         float close = this.releasedAt < 0 ? Math.min(1.0F, t / 6.0F) : Math.max(0.0F, 1.0F - ((float)(this.age - this.releasedAt) + f.partial) / 6.0F);
         if (!(close <= 0.02F)) {
            double cx = Mth.lerp((double)pt, victim.xo, victim.getX());
            double cy = Mth.lerp((double)pt, victim.yo, victim.getY());
            double cz = Mth.lerp((double)pt, victim.zo, victim.getZ());
            float rw = victim.getBbWidth() * 0.5F + 0.32F;
            float h = victim.getBbHeight();
            int tint = lit(level, cx, cy + (double)h * 0.5, cz);
            float squeeze = 1.7F - 0.7F * close;
            SandDraw.sphere(
               f,
               MeshTex.SAND_STREAM,
               cx,
               cy + (double)h * 0.5,
               cz,
               rw * 1.15F * squeeze,
               0.0F,
               1.0F,
               0.0F,
               h * 0.62F / (rw * 1.15F * squeeze),
               t * 0.09F,
               3.0F,
               -t * 0.02F,
               2.0F,
               scale(tint, 1.12F),
               0.62F * close,
               7,
               18
            );

            for (int k = 0; k < 4; k++) {
               float off = (float)k * (float) (Math.PI * 2) / 4.0F;
               float spin = t * 0.2F * (float)(k % 2 == 0 ? 1 : -1);

               for (int i = 0; i < 24; i++) {
                  float q = (float)i / 23.0F;
                  float ang = off + spin + q * (float) (Math.PI * 2) * 1.3F * (float)(k % 2 == 0 ? 1 : -1);
                  float r = rw * squeeze * (0.92F + 0.12F * Mth.sin(q * 9.0F + t * 0.3F + (float)k));
                  this.px[i] = cx + (double)(Mth.cos(ang) * r);
                  this.py[i] = cy + 0.05 + (double)(q * h) * 0.98;
                  this.pz[i] = cz + (double)(Mth.sin(ang) * r);
                  this.ax[i] = 0.0F;
                  this.ay[i] = 1.0F;
                  this.az[i] = 0.0F;
                  this.w[i] = 0.26F * close;
                  this.a[i] = Math.min(1.0F, Math.min(q, 1.0F - q) * 6.0F) * close;
               }

               SandDraw.band(
                  f, MeshTex.SAND_STREAM, 24, this.px, this.py, this.pz, this.ax, this.ay, this.az, this.w, this.a, -t * 0.4F + (float)k, 0.22F, tint, true
               );
            }

            Vec3 hand = SandGatherEffect.hand(caster, pt);
            Vec3 body = new Vec3(cx, cy + (double)h * 0.55, cz);
            Vec3 along = body.subtract(hand);

            for (int i = 0; i < 12; i++) {
               float q = (float)i / 11.0F;
               float wob = Mth.sin(q * 7.0F - t * 0.5F) * 0.06F * Mth.sin(q * (float) Math.PI);
               this.px[i] = hand.x + along.x * (double)q;
               this.py[i] = hand.y + along.y * (double)q + (double)wob + (double)(Mth.sin(q * (float) Math.PI) * 0.1F);
               this.pz[i] = hand.z + along.z * (double)q;
               this.w[i] = (0.1F + 0.12F * q) * close;
               this.a[i] = 0.85F * close * Math.min(1.0F, q * 8.0F + 0.3F);
            }

            SandDraw.stream(f, 12, this.px, this.py, this.pz, this.w, this.a, -t * 0.7F, 0.3F, tint);

            for (int i = 0; i < 12; i++) {
               this.w[i] = this.w[i] * 0.45F;
               this.a[i] = this.a[i] * 0.45F;
            }

            SandDraw.stream(f, MeshTex.LANCE, 12, this.px, this.py, this.pz, this.w, this.a, t * 0.9F, 0.3F, 13034239);
         }
      }
   }

   @Override
   public double x() {
      return this.vx;
   }

   @Override
   public double y() {
      return this.vy + 1.0;
   }

   @Override
   public double z() {
      return this.vz;
   }
}
