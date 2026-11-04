package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.ArcBurst;
import com.devilfruits.client.vfx.fx.BoltStrike;
import com.devilfruits.client.vfx.fx.GroundWave;
import com.devilfruits.client.vfx.fx.Impact;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.core.body.SerpentBody;
import com.devilfruits.fruits.uo_uo_seiryu.client.render.DragonRenderer;
import com.devilfruits.fruits.uo_uo_seiryu.entity.SeiryuDragonEntity;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class DragonTransformCinematic extends HybridCinematic {
   private static final int DARK_GRADE = 7234192;
   private static final int AFTER = 44;
   private final int windup;
   private int changed = -1;
   private double cx;
   private double cy;
   private double cz;

   public DragonTransformCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.windup = Math.max(0, params.extra());
      this.cx = this.x;
      this.cz = this.z;
      this.cy = Double.isNaN(this.groundY) ? this.y : this.groundY;
      if (this.windup > 0) {
         ArcBurst.at(new Vec3(this.x, this.y + (double)this.height * 0.5, this.z), this.seed ^ 161L)
            .follow(this.caster(level), 0.5F)
            .arcs(3)
            .radius(1.2F, 2.0F)
            .life(Math.min(this.windup, 50))
            .spin(0.5F)
            .arc((float) (Math.PI * 7.0 / 5.0))
            .look(MeshTex.FLAME_RIBBON, 0, 0.3F, 1.3F, 2.0F)
            .tilt(0.1F, 0.4F)
            .fadeIn(0.3F)
            .spawn();
      }
   }

   @Nullable
   public static DragonTransformCinematic of(Entity e) {
      return VfxDirector.find(DragonTransformCinematic.class, c -> c.follows(e) && c.changed < 0);
   }

   public void change(ClientLevel level) {
      this.changed = this.age;
      Entity e = this.caster(level);
      Vec3 g = new Vec3(this.cx, this.cy, this.cz);
      Vec3 c = g.add(0.0, 1.5, 0.0);
      BoltStrike.between(g.add(this.spread(2.0), 34.0, this.spread(2.0)), g, this.seed ^ 162L)
         .width(1.8F)
         .life(14)
         .forks(6)
         .rough(0.07F)
         .heat(1.35F)
         .endGlow(4.0F)
         .spawn();
      BoltStrike.between(g.add(this.spread(6.0), 28.0, this.spread(6.0)), g.add(this.spread(3.0), 0.0, this.spread(3.0)), this.seed ^ 163L)
         .width(0.5F)
         .life(9)
         .forks(3)
         .endGlow(1.5F)
         .spawn();
      BoltStrike.between(g.add(this.spread(8.0), 24.0, this.spread(8.0)), g.add(this.spread(4.0), 0.0, this.spread(4.0)), this.seed ^ 164L)
         .width(0.35F)
         .life(7)
         .forks(3)
         .endGlow(1.0F)
         .spawn();
      Impact.at(level, c, this.seed ^ 165L)
         .caster(e)
         .thunder()
         .flash(3.2F)
         .glow(11955455)
         .wave(14.0F, 18, 11955455)
         .dust(40)
         .sparks(80, 1.0F)
         .embers(50)
         .smoke(18, 1708080, 2.4F)
         .debris(16, 0.34F)
         .decal(9.0F)
         .trauma(0.8F, 0.06F, 4.0F)
         .hitStop(3)
         .fov(0.1F)
         .lens(0.55F)
         .screenFlash(0.35F)
         .play();
      Impact.at(level, c, this.seed ^ 166L).flash(2.2F).glow(16742938).sparks(40, 0.9F).embers(40).play();
      VfxParticles.emit(VfxPresets.FIREBALL, c.x, c.y + 1.0, c.z, 0.0, 0.06, 0.0).size(2.2F);

      for (int i = 0; i < 3; i++) {
         ArcBurst.at(c.add(0.0, (double)i * 1.5, 0.0), this.seed ^ (long)(167 + i))
            .arcs(3)
            .radius(1.5F, 9.0F + (float)(i * 2))
            .life(14)
            .spin(0.4F + 0.15F * (float)i)
            .arc(3.7699115F)
            .look(MeshTex.FLAME_RIBBON, 0, 0.7F, 1.35F, 2.2F)
            .tilt(0.0F, 0.35F)
            .spawn();
      }

      GroundWave.at(this.cx, this.cy, this.cz, 10.0F, 16).strength(1.8F).air(this.cy + 4.0, 9.0F).spawn();
      float ex = this.exposure();
      if (ex > 0.0F) {
         VfxPost.radialBlur(c, 0.3F * ex);
         CameraDirector.fovPunch(0.12F * ex);
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.changed < 0) {
         if (this.age > this.windup + 20) {
            this.finish();
         } else {
            this.tickWindup(level);
         }
      } else {
         int s = this.age - this.changed;
         this.tickAfter(level, s);
         if (s > 44) {
            this.finish();
         }
      }
   }

   private void tickWindup(ClientLevel level) {
      this.cx = this.x;
      this.cz = this.z;
      if (!Double.isNaN(this.groundY)) {
         this.cy = this.groundY;
      }

      float p = this.windup > 0 ? Math.min(1.0F, (float)this.age / (float)this.windup) : 1.0F;
      int dust = this.shade(VfxWorld.groundColour(level, this.cx, this.cy - 0.5, this.cz));

      for (int i = 0; i < VfxSettings.count(4.0F + 6.0F * p); i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double r = (double)(2.0F + p * 6.0F) + this.random.nextDouble() * 2.0;
         VfxParticles.emit(
               VfxPresets.DUST, this.cx + Math.cos(a) * r, this.cy + 0.3 + this.random.nextDouble() * 1.5, this.cz + Math.sin(a) * r, 0.0, 0.04, 0.0
            )
            .orbit(this.cx, this.cz, 0.18F)
            .tint(dust)
            .size(1.6F);
      }

      if (this.random.nextFloat() < 0.12F + 0.45F * p) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double r = 3.0 + this.random.nextDouble() * 6.0;
         Vec3 g = new Vec3(this.cx + Math.cos(a) * r, this.cy, this.cz + Math.sin(a) * r);
         BoltStrike.between(g.add(this.spread(3.0), 22.0, this.spread(3.0)), g, this.seed + (long)this.age * 131L)
            .width(0.3F + 0.3F * p)
            .life(6)
            .forks(3)
            .rough(0.1F)
            .endGlow(1.5F)
            .spawn();
         VfxParticles.emit(VfxPresets.CRACKS_VIOLET, g.x, this.cy + 0.04, g.z, 0.0, 0.0, 0.0)
            .size(2.5F)
            .rotation(this.random.nextFloat() * (float) (Math.PI * 2))
            .life(0.6F);
      }

      if (this.random.nextFloat() < 0.6F * p) {
         Vec3 b = new Vec3(this.x + this.spread(0.6), this.y + this.random.nextDouble() * (double)this.height, this.z + this.spread(0.6));
         BoltStrike.between(b, b.add(this.spread(1.4), this.spread(1.4), this.spread(1.4)), this.seed + (long)this.age * 7L)
            .width(0.07F)
            .life(2)
            .forks(1)
            .rough(0.3F)
            .endGlow(0.4F)
            .spawn();
      }

      VfxParticles.emit(VfxPresets.GLOW, this.x, this.y + (double)this.height * 0.5, this.z, 0.0, 0.0, 0.0)
         .size(3.0F + 4.0F * p)
         .tint(11955455)
         .alpha(0.3F + 0.4F * p);
      float ex = this.exposure();
      if (ex > 0.0F) {
         VfxPost.grade(7234192, 0.4F * p * ex);
         VfxPost.vignette(0.3F * p * ex);
         if (this.age % 6 == 0) {
            CameraDirector.shake((0.05F + 0.15F * p) * ex, 0.15F, 1.0F, 4.0F);
         }
      }
   }

   private void tickAfter(ClientLevel level, int s) {
      Entity e = this.caster(level);
      SeiryuDragonEntity shell = e == null ? null : DragonRenderer.shellOf(e);
      SerpentBody body = shell == null ? null : shell.body();
      float fade = 1.0F - (float)s / 44.0F;
      if (body != null) {
         double[] a = new double[3];
         double[] b = new double[3];

         for (int k = 0; k < 2; k++) {
            if (this.random.nextFloat() < 0.8F * fade) {
               float at = this.random.nextFloat() * 0.5F;
               body.point((double)(body.neck + body.length * at), 1.0F, a);
               body.point((double)(body.neck + body.length * (at + 0.03F)), 1.0F, b);
               float r = body.radiusAt(at);
               BoltStrike.between(
                     new Vec3(a[0], a[1] + (double)r * 0.8, a[2]),
                     new Vec3(b[0] + this.spread((double)r), b[1] + (double)r, b[2] + this.spread((double)r)),
                     this.seed + (long)s * 59L + (long)k
                  )
                  .width(0.25F)
                  .life(3)
                  .forks(2)
                  .rough(0.22F)
                  .endGlow(1.0F)
                  .spawn();
            }
         }

         for (int i = 0; i < VfxSettings.count(4.0F * fade); i++) {
            body.point((double)(body.neck + body.length * this.random.nextFloat() * 0.6F), 1.0F, a);
            VfxParticles.emit(
                  VfxPresets.EMBER, a[0] + this.spread(2.0), a[1] + this.spread(2.0), a[2] + this.spread(2.0), this.spread(0.05), -0.05, this.spread(0.05)
               )
               .life(1.5F);
         }
      }
   }

   @Override
   public double x() {
      return this.cx;
   }

   @Override
   public double y() {
      return this.cy + 2.0;
   }

   @Override
   public double z() {
      return this.cz;
   }

   @Override
   public double cullDistance() {
      return 192.0;
   }
}
