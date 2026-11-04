package com.devilfruits.fruits.supa_supa.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.ArcStrip;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.core.config.CoreClientConfig;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class ClawRakeCinematic extends Cinematic {
   private static final int SWEEP = 3;
   private static final int LIFE = 11;
   private final Entity user;
   private final Vec3 look;
   private final Vec3 u;
   private final Vec3 v;
   private final Vec3 n;
   private final float radius;
   private final boolean through;
   private final RandomSource random;
   private Vec3 centre;

   public ClawRakeCinematic(@Nullable Entity user, Vec3 eye, Vec3 look, float reach, boolean left, boolean through, long seed) {
      super(new VfxAnchor(user != null ? user.getId() : -1, eye), VfxParams.NONE);
      this.user = user;
      this.look = look.normalize();
      this.through = through;
      this.random = RandomSource.create(seed);
      Vec3 flat = new Vec3(look.x, 0.0, look.z);
      flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
      Vec3 side = new Vec3(-flat.z, 0.0, flat.x).scale(left ? -1.0 : 1.0);
      Vec3 up = new Vec3(0.0, 1.0, 0.0);
      Vec3 diag = up.add(side.scale(1.1)).normalize();
      diag = diag.subtract(this.look.scale(diag.dot(this.look))).normalize();
      this.u = this.look;
      this.v = diag;
      this.n = this.u.cross(this.v).normalize();
      this.radius = Math.max(1.2F, reach * 0.42F) * (through ? 1.3F : 1.0F);
      this.centre = eye.add(0.0, -0.3, 0.0);
   }

   private float head(float t) {
      float s = Mth.clamp(t / 3.0F, 0.0F, 1.0F);
      float e = 1.0F - (1.0F - s) * (1.0F - s) * (1.0F - s);
      return (float)Math.toRadians((double)(72.0F - 150.0F * e));
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.user != null && !this.user.isRemoved()) {
         this.centre = this.user.getEyePosition().add(0.0, -0.3, 0.0);
      }

      if (this.age <= 3 && VfxSettings.tier() != CoreClientConfig.CinematicQuality.LOW) {
         float a = this.head((float)this.age);
         Vec3 p = this.point(a, this.radius);
         Vec3 tangent = this.u.scale((double)(-Mth.sin(a))).add(this.v.scale((double)Mth.cos(a))).scale(-1.0);

         for (int i = 0; i < VfxSettings.count(3.0F); i++) {
            Vec3 q = p.add(this.n.scale((double)((this.random.nextFloat() - 0.5F) * 0.4F)));
            VfxParticles.emit(SupaLook.AIR_SLIT, q.x, q.y, q.z, tangent.x * 0.5, tangent.y * 0.5, tangent.z * 0.5).alpha(0.7F);
         }

         if (this.age == 3) {
            for (int i = 0; i < VfxSettings.count(5.0F); i++) {
               VfxParticles.emit(
                  SupaLook.FILING,
                  p.x,
                  p.y,
                  p.z,
                  tangent.x * 0.25 + (double)(this.random.nextFloat() - 0.5F) * 0.15,
                  0.08 + (double)this.random.nextFloat() * 0.1,
                  tangent.z * 0.25 + (double)(this.random.nextFloat() - 0.5F) * 0.15
               );
            }
         }
      }

      if (this.age > 11) {
         this.finish();
      }
   }

   private Vec3 point(float a, float r) {
      return this.centre.add(this.u.scale((double)(Mth.cos(a) * r))).add(this.v.scale((double)(Mth.sin(a) * r)));
   }

   @Override
   public void render(VfxFrame f) {
      float t = (float)this.age + f.partial;
      float head = this.head(t);
      float travelled = this.head(0.0F) - head;
      float fade = t <= 3.0F ? 1.0F : Math.max(0.0F, 1.0F - (t - 3.0F) / 8.0F);
      float cool = t <= 3.0F ? 1.0F : 0.55F + 0.45F * fade;
      float trail = Math.min(travelled, (float)Math.toRadians(150.0));
      float cx = f.rx(this.centre.x);
      float cy = f.ry(this.centre.y);
      float cz = f.rz(this.centre.z);

      for (int k = -1; k <= 1; k++) {
         float off = (float)k * 0.16F;
         float ox = (float)(this.n.x * (double)off);
         float oy = (float)(this.n.y * (double)off);
         float oz = (float)(this.n.z * (double)off);
         float r = this.radius + (float)k * 0.05F;

         for (int g = 0; g < 3; g++) {
            float lag = (float)Math.toRadians((double)(14 * g));
            float alpha = fade * (g == 0 ? 1.0F : (g == 1 ? 0.42F : 0.18F)) * (!(t <= 3.0F) && g != 0 ? 0.0F : 1.0F);
            if (!(alpha <= 0.01F) && !(trail - lag <= 0.02F)) {
               ArcStrip.draw(
                  f,
                  MeshTex.CUT_RIBBON,
                  cx + ox,
                  cy + oy,
                  cz + oz,
                  (float)this.u.x,
                  (float)this.u.y,
                  (float)this.u.z,
                  (float)this.v.x,
                  (float)this.v.y,
                  (float)this.v.z,
                  r,
                  head + lag,
                  -(trail - lag),
                  0.46F * (g == 0 ? 1.0F : 0.8F),
                  1.05F * cool,
                  alpha,
                  5,
                  2.2F,
                  16777215,
                  f.time * 0.4F + (float)k * 0.31F,
                  28
               );
            }
         }
      }
   }

   @Override
   public double x() {
      return this.centre.x;
   }

   @Override
   public double y() {
      return this.centre.y;
   }

   @Override
   public double z() {
      return this.centre.z;
   }
}
