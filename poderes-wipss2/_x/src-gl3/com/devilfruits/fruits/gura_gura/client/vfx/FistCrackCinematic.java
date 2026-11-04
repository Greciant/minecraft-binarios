package com.devilfruits.fruits.gura_gura.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.GlassCrack;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.mojang.blaze3d.vertex.BufferBuilder;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class FistCrackCinematic extends QuakeCinematic {
   private final float scale;
   private final int charge;
   @Nullable
   private GlassCrack crack;
   private double fx;
   private double fy;
   private double fz;
   private float r;
   private boolean ended;

   public FistCrackCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.scale = Math.max(0.2F, params.power());
      this.charge = Math.max(1, params.extra());
      Entity e = this.caster(level);
      Vec3 f = e != null ? fist(e, 1.0F, 1.0F) : new Vec3(this.x, this.y + 1.3, this.z);
      this.fx = f.x;
      this.fy = f.y;
      this.fz = f.z;
      this.crack = GlassCrack.at(f, new Vec3(0.0, 0.0, 1.0), 0.1F, this.seed)
         .billboard()
         .pattern(10, 3)
         .grow(this.charge)
         .hold(600)
         .refraction(0.75F)
         .lines(0.03F)
         .sheen(1.4F)
         .tint(15398655)
         .spawn();
   }

   @Nullable
   public static FistCrackCinematic of(Entity e) {
      return VfxDirector.find(FistCrackCinematic.class, c -> c.follows(e) && !c.ended);
   }

   private float progress(float t) {
      return Math.min(1.0F, t / (float)this.charge);
   }

   public void release(Vec3 dir) {
      if (this.crack != null) {
         this.crack.moveTo(this.fx, this.fy, this.fz);
         this.crack.shatterNow(dir.scale(0.5));
      }

      this.ended = true;
      this.finish();
   }

   public void stop() {
      if (this.crack != null) {
         this.crack.release();
      }

      this.ended = true;
      this.finish();
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = this.caster(level);
      if (e != null && this.age <= this.charge + 400) {
         Vec3 f = fist(e, 1.0F, 1.0F);
         this.fx = f.x;
         this.fy = f.y;
         this.fz = f.z;
         float p = this.progress((float)this.age);
         this.r = (0.14F + 0.46F * p) * this.scale * (this.firstPersonCaster() ? 0.42F : 1.0F);
         if (this.crack != null) {
            this.crack.moveTo(this.fx, this.fy, this.fz);
            this.crack.radius(this.r);
         }

         for (int i = 0; i < VfxSettings.count(1.0F + 3.0F * p); i++) {
            double ox = this.spread(1.4 * (double)this.scale);
            double oy = this.spread(1.2 * (double)this.scale);
            double oz = this.spread(1.4 * (double)this.scale);
            VfxParticles.emit(
                  i % 2 == 0 ? VfxPresets.GLASS_DUST : VfxPresets.GLASS_GLINT, this.fx + ox, this.fy + oy, this.fz + oz, -ox * 0.14, -oy * 0.14, -oz * 0.14
               )
               .tint(14217983)
               .life(0.5F);
         }

         if (this.local() && p >= 1.0F) {
            if (this.age % 4 == 0) {
               CameraDirector.shake(0.05F, 0.2F, 0.5F, 7.0F);
            }

            VfxPost.chromatic(0.04F);
         }
      } else {
         this.stop();
      }
   }

   @Override
   public void render(VfxFrame f) {
      Entity e = this.caster();
      if (e != null) {
         Vec3 p = fist(e, f.entityPartial, 1.0F);
         this.fx = p.x;
         this.fy = p.y;
         this.fz = p.z;
         if (this.crack != null) {
            this.crack.moveTo(this.fx, this.fy, this.fz);
         }
      }

      float p = this.progress((float)this.age + f.partial);
      float cx = f.rx(this.fx);
      float cy = f.ry(this.fy);
      float cz = f.rz(this.fz);
      BufferBuilder d = f.distortion();
      if (d != null) {
         VfxSprites.Sprite ring = VfxSprites.RING_NORMAL;
         float fp = this.firstPersonCaster() ? 0.4F : 1.0F;
         MeshWriter.distortQuad(
            d,
            cx,
            cy,
            cz,
            this.r * (fp < 1.0F ? 1.1F : 1.35F),
            (0.5F + 0.6F * p) * fp,
            0,
            false,
            f.left.x,
            f.left.y,
            f.left.z,
            f.up.x,
            f.up.y,
            f.up.z,
            ring.u0(),
            ring.v0(),
            ring.u1(),
            ring.v1(),
            0.0F,
            0.0F
         );
      }

      if (f.allowMesh()) {
         MeshWriter.ring(
            f.mesh(MeshTex.LANCE),
            cx,
            cy,
            cz,
            -f.left.x,
            -f.left.y,
            -f.left.z,
            f.up.x,
            f.up.y,
            f.up.z,
            this.r,
            Math.max(0.015F, this.r * 0.05F),
            32,
            0.0F,
            1.0F,
            MeshWriter.argb(15398655, 0.25F + 0.35F * p),
            -1.0F,
            0,
            1.6F,
            0.0F
         );
      }
   }

   @Override
   public double x() {
      return this.fx;
   }

   @Override
   public double y() {
      return this.fy;
   }

   @Override
   public double z() {
      return this.fz;
   }
}
