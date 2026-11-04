package com.devilfruits.fruits.gura_gura.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.GlassCrack;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.registry.DFParticles;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class KaishinCinematic extends QuakeCinematic {
   private static final int LIFE = 14;
   private final Vec3 fist;
   private final Vec3 dir;
   private final float charge;
   private final float length;
   private final float spread;
   private final MutableBlockPos pos = new MutableBlockPos();

   public KaishinCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.fist = params.pos();
      Vec3 d = params.dir().lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : params.dir().normalize();
      this.dir = d;
      this.charge = Mth.clamp(params.power(), 0.0F, 1.0F);
      this.length = Math.max(2.0F, (float)params.extra() / 16.0F);
      this.spread = (float)Math.tan(Math.toRadians(28.0 * (1.0 + 0.35 * (double)this.charge)));
      float c = this.charge;
      boolean fp = this.firstPersonCaster();
      Entity e = this.caster(level);
      FistCrackCinematic bubble = e != null ? FistCrackCinematic.of(e) : null;
      if (bubble != null) {
         bubble.release(d);
      }

      Vec3 pane = this.fist.add(d.scale(fp ? 2.6 : 1.0 + 0.6 * (double)c));
      GlassCrack.at(pane, d.scale(-1.0), (0.9F + 1.7F * c) * (fp ? 0.6F : 1.0F), this.seed)
         .pattern(11 + Math.round(4.0F * c), 3)
         .grow(2)
         .hold(2 + Math.round(2.0F * c))
         .shatter(d.scale(0.35 + 0.45 * (double)c))
         .refraction(0.9F)
         .lines(0.02F)
         .tint(15398655)
         .spawn();
      VfxParticles.emit(VfxPresets.GLOW, pane.x, pane.y, pane.z, 0.0, 0.0, 0.0).size(1.5F + 1.5F * c).tint(15398655).alpha(0.35F).life(1.2F);
      int rings = 3 + Math.round(2.0F * c);

      for (int k = 0; k < rings; k++) {
         float reach = this.length * (0.55F + 0.45F * (float)k / (float)Math.max(1, rings - 1));
         int life = 7 + 2 * k;
         ShockRing.at(this.fist.add(d.scale(0.5)), d)
            .radius(0.3F + 0.15F * (float)k, 0.8F + reach * this.spread)
            .life(life)
            .move(d.scale((double)(reach / (float)life)))
            .strength(1.25F - 0.12F * (float)k)
            .band(0.32F)
            .rim(0.16F, 15398655)
            .spawn();
      }

      float ex = this.exposure(this.fist);
      if (ex > 0.0F) {
         float soft = fp ? 0.5F : 1.0F;
         CameraDirector.shake((0.25F + 0.4F * c) * ex, 0.09F, 1.5F + 2.0F * c, 3.2F);
         CameraDirector.fovPunch((0.04F + 0.06F * c) * ex);
         VfxPost.chromatic((0.15F + 0.35F * c) * ex * soft);
         VfxPost.radialBlur(pane, (0.08F + 0.12F * c) * ex * soft);
         if (c > 0.75F) {
            VfxPost.flash(15398655, 0.18F * c * ex * soft);
            if (ex > 0.5F) {
               VfxClock.hitStop(2);
            }
         }
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age > 14) {
         this.finish();
      } else {
         float front = this.length * Math.min(1.0F, (float)this.age / 6.0F);
         float back = this.length * Math.min(1.0F, (float)(this.age - 1) / 6.0F);
         if (!(front <= back) && this.age <= 7) {
            for (float t = Math.max(0.8F, back); t < front; t += 0.9F) {
               Vec3 p = this.fist.add(this.dir.scale((double)t));
               double r = (double)(t * this.spread);

               for (int i = 0; i < VfxSettings.count(2.0F + this.charge * 2.0F); i++) {
                  VfxParticles.emit(
                        VfxPresets.GLASS_GLINT,
                        p.x + this.spread(r),
                        p.y + this.spread(r),
                        p.z + this.spread(r),
                        this.dir.x * 0.35,
                        this.dir.y * 0.35 + 0.05,
                        this.dir.z * 0.35
                     )
                     .tint(14217983);
               }

               BlockState ground = Fx.groundAt(level, p.x, p.y - 1.0, p.z, this.pos);
               if (ground != null && p.y - (double)(this.pos.getY() + 1) < 2.5) {
                  int dust = QuakeCinematic.dust(level, p.x, (double)this.pos.getY(), p.z);

                  for (int i = 0; i < VfxSettings.count(1.0F + 2.0F * this.charge); i++) {
                     VfxParticles.emit(
                           VfxPresets.DUST,
                           p.x + this.spread(r),
                           (double)this.pos.getY() + 1.2,
                           p.z + this.spread(r),
                           this.dir.x * 0.18 + this.spread(0.05),
                           0.04,
                           this.dir.z * 0.18 + this.spread(0.05)
                        )
                        .tint(dust)
                        .size(1.1F + this.charge);
                  }

                  for (int i = 0; i < Fx.count(1 + Math.round(3.0F * this.charge)); i++) {
                     Fx.spawn(
                        new BlockParticleOption(DFParticles.BLOCK_DEBRIS.get(), ground),
                        p.x + this.spread(r),
                        (double)this.pos.getY() + 1.05,
                        p.z + this.spread(r),
                        this.dir.x * 0.3 + this.spread(0.1),
                        0.25 + this.random.nextDouble() * 0.3 * (double)(1.0F + this.charge),
                        this.dir.z * 0.3 + this.spread(0.1)
                     );
                  }
               }
            }
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      float t = (float)this.age + f.partial;
      float grow = Math.min(1.0F, t / 3.0F);
      float fade = Math.max(0.0F, 1.0F - t / 14.0F);
      if (!(fade <= 0.01F)) {
         float len = this.length * grow;
         float bx = f.rx(this.fist.x + this.dir.x * 0.5);
         float by = f.ry(this.fist.y + this.dir.y * 0.5);
         float bz = f.rz(this.fist.z + this.dir.z * 0.5);
         float dx = (float)this.dir.x;
         float dy = (float)this.dir.y;
         float dz = (float)this.dir.z;
         if (f.allowMesh()) {
            MeshWriter.cone(
               f.mesh(MeshTex.NOISE),
               bx,
               by,
               bz,
               dx,
               dy,
               dz,
               len,
               0.25F,
               0.3F + len * this.spread,
               28,
               t * 0.02F,
               2.0F,
               -t * 0.25F,
               1.4F,
               MeshWriter.argb(15398655, 0.11F * fade * (0.6F + 0.4F * this.charge)),
               MeshWriter.argb(15398655, 0.02F * fade),
               -1.0F,
               -1.0F,
               0,
               0.5F,
               0.0F,
               0.6F
            );
         }

         BufferBuilder d = f.distortion();
         if (d != null) {
            VfxSprites.Sprite glow = VfxSprites.GLOW;

            for (int k = 1; k <= 4; k++) {
               float u = (float)k / 4.0F * len;
               MeshWriter.distortQuad(
                  d,
                  bx + dx * u,
                  by + dy * u,
                  bz + dz * u,
                  0.5F + u * this.spread,
                  0.9F * fade,
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
                  (float)k * 0.27F + f.time * 0.01F,
                  0.3F
               );
            }
         }
      }
   }

   @Override
   public double x() {
      return this.fist.x + this.dir.x * (double)this.length * 0.5;
   }

   @Override
   public double y() {
      return this.fist.y + this.dir.y * (double)this.length * 0.5;
   }

   @Override
   public double z() {
      return this.fist.z + this.dir.z * (double)this.length * 0.5;
   }
}
