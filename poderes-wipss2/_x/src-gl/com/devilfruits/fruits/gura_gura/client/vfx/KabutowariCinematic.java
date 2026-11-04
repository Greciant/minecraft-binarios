package com.devilfruits.fruits.gura_gura.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.GlassCrack;
import com.devilfruits.client.vfx.fx.GroundWave;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.fruits.gura_gura.quake.FissurePattern;
import com.mojang.blaze3d.vertex.BufferBuilder;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class KabutowariCinematic extends QuakeCinematic {
   private static final float SPREAD = 1.6F;
   private static final int GLOW = 26;
   private static final int FADE = 40;
   private final double ex;
   private final double ey;
   private final double ez;
   private final float[][] lx;
   private final float[][] ly;
   private final float[][] lz;
   private final float[][] dist;
   private final float[] widths;
   private final int[] dust;
   private float maxDist;
   private final float[] sx = new float[64];
   private final float[] sy = new float[64];
   private final float[] sz = new float[64];
   private final float[] sw = new float[64];
   private final float[] sh = new float[64];
   private final float[] sa = new float[64];

   public KabutowariCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      Vec3 at = params.pos();
      this.ex = at.x;
      this.ey = at.y;
      this.ez = at.z;
      float length = Math.max(2.0F, params.power());
      List<FissurePattern.Line> pattern = FissurePattern.generate(params.seed(), Math.max(1, params.extra()), length);
      int n = pattern.size();
      this.lx = new float[n][];
      this.ly = new float[n][];
      this.lz = new float[n][];
      this.dist = new float[n][];
      this.widths = new float[n];
      this.dust = new int[n];
      MutableBlockPos pos = new MutableBlockPos();

      for (int i = 0; i < n; i++) {
         FissurePattern.Line line = pattern.get(i);
         int size = Math.min(64, line.size());
         this.lx[i] = new float[size];
         this.ly[i] = new float[size];
         this.lz[i] = new float[size];
         this.dist[i] = new float[size];
         this.widths[i] = line.width();

         for (int k = 0; k < size; k++) {
            double wx = this.ex + (double)line.x()[k];
            double wz = this.ez + (double)line.z()[k];
            BlockState state = Fx.groundAt(level, wx, this.ey - 1.0, wz, pos);
            double top = state != null ? (double)pos.getY() + 1.0 : this.ey;
            this.lx[i][k] = (float)(wx - this.ex);
            this.ly[i][k] = (float)(top + 0.04 - this.ey);
            this.lz[i][k] = (float)(wz - this.ez);
            this.dist[i][k] = line.dist()[k];
            this.maxDist = Math.max(this.maxDist, this.dist[i][k]);
         }

         this.dust[i] = QuakeCinematic.dust(level, this.ex + (double)this.lx[i][0], this.ey - 1.0, this.ez + (double)this.lz[i][0]);
      }

      this.slam(level, length);
   }

   private void slam(ClientLevel level, float length) {
      Entity e = this.caster(level);
      Vec3 g = new Vec3(this.ex, this.ey + 0.05, this.ez);
      GlassCrack.at(g, new Vec3(0.0, 1.0, 0.0), 3.4F, this.seed ^ 81L)
         .pattern(15, 4)
         .grow(2)
         .hold(6)
         .shatter(new Vec3(0.0, 0.24, 0.0))
         .refraction(1.0F)
         .lines(0.02F)
         .tint(15398655)
         .spawn();
      int floor = QuakeCinematic.dust(level, this.ex, this.ey - 1.0, this.ez);
      GroundWave.at(this.ex, this.ey, this.ez, length * 0.95F, 16)
         .strength(1.4F)
         .ring(15398655, 0.35F)
         .dust(22, floor, 0.4F, 0.55F, 0.9F, this.seed ^ 82L)
         .air(this.ey + 1.5, length * 0.6F)
         .spawn();
      ShockRing.at(g.add(0.0, 0.6, 0.0), new Vec3(0.0, 1.0, 0.0))
         .radius(0.5F, 5.0F)
         .life(9)
         .move(new Vec3(0.0, 0.25, 0.0))
         .strength(1.2F)
         .band(0.35F)
         .rim(0.2F, 15398655)
         .spawn();
      VfxParticles.emit(VfxPresets.GLOW, this.ex, this.ey + 0.5, this.ez, 0.0, 0.0, 0.0).size(4.0F).tint(15398655).alpha(0.4F).life(1.3F);

      for (int i = 0; i < VfxSettings.count(30.0F); i++) {
         VfxParticles.emit(
               i % 3 == 0 ? VfxPresets.GLASS_DUST : VfxPresets.GLASS_GLINT,
               this.ex + this.spread(1.5),
               this.ey + 0.3,
               this.ez + this.spread(1.5),
               this.spread(0.35),
               0.15 + this.random.nextDouble() * 0.35,
               this.spread(0.35)
            )
            .tint(14217983);
      }

      float x0 = this.exposure(g);
      if (x0 > 0.0F) {
         float soft = this.firstPersonCaster() ? 0.5F : 1.0F;
         CameraDirector.shake(0.8F * x0, 0.06F, 4.0F, 3.0F);
         CameraDirector.fovPunch(0.09F * x0);
         VfxPost.chromatic(0.45F * x0 * soft);
         VfxPost.radialBlur(g, 0.18F * x0 * soft);
         VfxPost.flash(15398655, 0.15F * x0 * soft);
         if (x0 > 0.5F) {
            VfxClock.hitStop(3);
         }
      }
   }

   private int growTicks() {
      return Mth.ceil(this.maxDist / 1.6F);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age > this.growTicks() + 26 + 40) {
         this.finish();
      } else {
         float reach = (float)this.age * 1.6F;
         float last = (float)(this.age - 1) * 1.6F;
         if (!(reach > this.maxDist + 1.6F)) {
            for (int i = 0; i < this.lx.length; i++) {
               for (int k = 0; k < this.lx[i].length; k++) {
                  if (this.dist[i][k] > last && this.dist[i][k] <= reach) {
                     double wx = this.ex + (double)this.lx[i][k];
                     double wy = this.ey + (double)this.ly[i][k];
                     double wz = this.ez + (double)this.lz[i][k];

                     for (int q = 0; q < VfxSettings.count(1.2F); q++) {
                        VfxParticles.emit(
                              VfxPresets.DUST,
                              wx + this.spread(0.4),
                              wy + 0.2,
                              wz + this.spread(0.4),
                              this.spread(0.04),
                              0.06 + this.random.nextDouble() * 0.05,
                              this.spread(0.04)
                           )
                           .tint(this.dust[i])
                           .size(0.6F * this.widths[i] + 0.35F)
                           .alpha(0.7F);
                     }

                     if (this.random.nextFloat() < 0.4F) {
                        VfxParticles.emit(VfxPresets.GLASS_GLINT, wx, wy + 0.1, wz, this.spread(0.1), 0.15 + this.random.nextDouble() * 0.15, this.spread(0.1))
                           .tint(14217983);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      if (f.allowMesh()) {
         float t = (float)this.age + f.partial;
         float reach = t * 1.6F;
         float glow = 1.0F - Mth.clamp((t - (float)this.growTicks()) / 26.0F, 0.0F, 1.0F);
         float alpha = 1.0F - Mth.clamp((t - (float)this.growTicks() - 26.0F) / 40.0F, 0.0F, 1.0F);
         if (!(alpha <= 0.01F)) {
            float ox = f.rx(this.ex);
            float oy = f.ry(this.ey);
            float oz = f.rz(this.ez);
            BufferBuilder lance = f.mesh(MeshTex.LANCE);
            BufferBuilder d = f.distortion();
            VfxSprites.Sprite haze = VfxSprites.GLOW;

            for (int i = 0; i < this.lx.length; i++) {
               int n = 0;

               for (int k = 0; k < this.lx[i].length; k++) {
                  float dk = this.dist[i][k];
                  if (dk > reach) {
                     if (k > 0) {
                        float d0 = this.dist[i][k - 1];
                        float e = (reach - d0) / Math.max(0.01F, dk - d0);
                        this.sx[n] = ox + Mth.lerp(e, this.lx[i][k - 1], this.lx[i][k]);
                        this.sy[n] = oy + Mth.lerp(e, this.ly[i][k - 1], this.ly[i][k]);
                        this.sz[n] = oz + Mth.lerp(e, this.lz[i][k - 1], this.lz[i][k]);
                        n++;
                     }
                     break;
                  }

                  this.sx[n] = ox + this.lx[i][k];
                  this.sy[n] = oy + this.ly[i][k];
                  this.sz[n] = oz + this.lz[i][k];
                  n++;
               }

               if (n >= 2) {
                  float w = 0.22F + this.widths[i] * 0.3F;

                  for (int q = 0; q < n; q++) {
                     float tip = 1.0F - (float)q / (float)Math.max(1, this.lx[i].length - 1);
                     this.sw[q] = w * (0.5F + 0.5F * tip);
                     this.sh[q] = -1.0F;
                     this.sa[q] = 0.88F * alpha;
                  }

                  MeshWriter.strip(lance, n, this.sx, this.sy, this.sz, this.sw, this.sh, this.sa, 0.0F, 0.4F, 461068, 0, 0.0F);

                  for (int q = 0; q < n; q++) {
                     this.sw[q] = this.sw[q] * 0.35F;
                     this.sa[q] = alpha * (0.35F + 0.65F * glow);
                  }

                  MeshWriter.strip(lance, n, this.sx, this.sy, this.sz, this.sw, this.sh, this.sa, 0.0F, 0.4F, 15398655, 0, 0.6F + 2.0F * glow);
                  if (d != null && glow > 0.05F) {
                     for (int q = 2; q < n; q += 5) {
                        MeshWriter.distortQuad(
                           d,
                           this.sx[q],
                           this.sy[q] + 0.7F,
                           this.sz[q],
                           0.7F + w,
                           0.22F * glow,
                           1,
                           false,
                           f.left.x,
                           f.left.y,
                           f.left.z,
                           f.up.x,
                           f.up.y,
                           f.up.z,
                           haze.u0(),
                           haze.v0(),
                           haze.u1(),
                           haze.v1(),
                           (float)(i * 7 + q) * 0.13F + f.time * 0.01F,
                           0.4F
                        );
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public double x() {
      return this.ex;
   }

   @Override
   public double y() {
      return this.ey + 1.0;
   }

   @Override
   public double z() {
      return this.ez;
   }

   @Override
   public double cullDistance() {
      return 128.0;
   }
}
