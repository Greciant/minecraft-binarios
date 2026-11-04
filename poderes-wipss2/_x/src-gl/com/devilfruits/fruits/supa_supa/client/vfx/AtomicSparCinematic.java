package com.devilfruits.fruits.supa_supa.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.core.config.CoreClientConfig;
import com.mojang.blaze3d.vertex.BufferBuilder;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class AtomicSparCinematic extends Cinematic {
   private static final int FLY = 4;
   private static final int LIFE = 16;
   private final Vec3 centre;
   private final Vec3 look;
   private final Vec3 side;
   private final Vec3 up;
   private final float range;
   private final float half;
   private final RandomSource random;

   public AtomicSparCinematic(@Nullable Entity user, Vec3 centre, Vec3 look, float range, float halfWidth, long seed) {
      super(new VfxAnchor(user != null ? user.getId() : -1, centre), VfxParams.NONE);
      this.centre = centre;
      this.look = look.normalize();
      Vec3 flat = new Vec3(look.x, 0.0, look.z);
      flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
      this.side = new Vec3(-flat.z, 0.0, flat.x);
      this.up = this.side.cross(this.look).normalize();
      this.range = range;
      this.half = halfWidth;
      this.random = RandomSource.create(seed);
      if (user != null && VfxWorld.local(user.getId())) {
         VfxClock.hitStop(2);
         CameraDirector.shake(0.3F, 0.1F, 1.4F, 11.0F);
         CameraDirector.fovPunch(0.035F);
      }
   }

   private float distance(float t) {
      float s = Mth.clamp(t / 4.0F, 0.0F, 1.0F);
      float e = 1.0F - (1.0F - s) * (1.0F - s);
      return 0.6F + (this.range - 0.6F) * e;
   }

   private Vec3 at(float d, int diag, float k) {
      return this.centre
         .add(this.look.scale((double)d))
         .add(this.side.scale((double)(this.half * k)))
         .add(this.up.scale((double)(this.half * k * (float)diag)));
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      boolean fancy = VfxSettings.tier() != CoreClientConfig.CinematicQuality.LOW;
      if (fancy && this.age <= 5) {
         float d = this.distance((float)this.age);

         for (int diag = -1; diag <= 1; diag += 2) {
            for (int i = 0; i < VfxSettings.count(4.0F); i++) {
               float k = this.random.nextFloat() * 2.0F - 1.0F;
               Vec3 p = this.at(d, diag, k);
               VfxParticles.emit(SupaLook.AIR_SLIT, p.x, p.y, p.z, this.look.x * 0.6, this.look.y * 0.6, this.look.z * 0.6).alpha(0.6F);
            }

            if (this.age == 4) {
               for (int end = -1; end <= 1; end += 2) {
                  Vec3 p = this.at(d, diag, (float)end);

                  for (int i = 0; i < VfxSettings.count(4.0F); i++) {
                     VfxParticles.emit(
                        SupaLook.FILING,
                        p.x,
                        p.y,
                        p.z,
                        (double)(this.random.nextFloat() - 0.5F) * 0.2,
                        0.05 + (double)this.random.nextFloat() * 0.12,
                        (double)(this.random.nextFloat() - 0.5F) * 0.2
                     );
                  }
               }
            }
         }

         if (this.age == 4) {
            Vec3 c = this.centre.add(this.look.scale((double)d));
            VfxParticles.emit(SupaLook.GLINT, c.x, c.y, c.z, 0.0, 0.0, 0.0).size(2.2F);
         }
      }

      if (this.age > 16) {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      float t = (float)this.age + f.partial;
      float d = this.distance(t);
      float fade = t <= 6.0F ? 1.0F : Math.max(0.0F, 1.0F - (t - 4.0F - 2.0F) / 10.0F);
      float heat = t <= 6.0F ? 1.1F : 0.6F + 0.5F * fade;

      for (int diag = -1; diag <= 1; diag += 2) {
         for (int g = 0; g < 3; g++) {
            float dg = d - (float)g * 0.9F;
            float a = fade * (g == 0 ? 1.0F : (g == 1 ? 0.45F : 0.2F)) * (!(t <= 4.0F) && g != 0 ? 0.0F : 1.0F);
            if (!(a <= 0.01F) && !(dg < 0.4F)) {
               Vec3 p0 = this.at(dg, diag, -1.05F);
               Vec3 p1 = this.at(dg, diag, 1.05F);
               Cuts.line(f, p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, 0.36F * (g == 0 ? 1.0F : 0.75F), heat, a, f.time * 0.5F + (float)diag * 0.37F);
            }
         }
      }

      BufferBuilder dist = f.distortion();
      if (dist != null && fade > 0.05F) {
         VfxSprites.Sprite glow = VfxSprites.GLOW;

         for (int diag = -1; diag <= 1; diag += 2) {
            for (int i = 0; i <= 6; i++) {
               Vec3 p = this.at(Math.max(0.5F, d - 0.3F), diag, -1.0F + (float)i / 3.0F);
               MeshWriter.distortQuad(
                  dist,
                  f.rx(p.x),
                  f.ry(p.y),
                  f.rz(p.z),
                  0.55F,
                  0.7F * fade,
                  1,
                  false,
                  f.left.x,
                  f.left.y,
                  f.left.z,
                  f.up.x,
                  f.up.y,
                  f.up.z,
                  glow.u0(),
                  glow.v0(),
                  glow.u1(),
                  glow.v1(),
                  f.time * 0.05F + (float)i * 0.3F,
                  0.7F
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
