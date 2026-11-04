package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.ArcBurst;
import com.devilfruits.client.vfx.fx.BoltStrike;
import com.devilfruits.client.vfx.fx.GroundWave;
import com.devilfruits.client.vfx.fx.Impact;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.Ribbon;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.core.body.SerpentBody;
import com.devilfruits.fruits.uo_uo_seiryu.client.render.DragonRenderer;
import com.devilfruits.fruits.uo_uo_seiryu.entity.SeiryuDragonEntity;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class TailCinematic extends HybridCinematic {
   private static final float[] AT = new float[]{0.97F, 0.86F};
   private final boolean crush;
   private final int ticks;
   private final Vec3 centre;
   private final float coil;
   @Nullable
   private final Ribbon fire;
   @Nullable
   private final Ribbon bolt;
   private final double[] p = new double[3];
   private final double[] q = new double[3];
   private double hx;
   private double hy;
   private double hz;
   private double kx;
   private double ky;
   private double kz;
   private float thick = 2.0F;

   public TailCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.crush = Math.round(params.dir().x) == 2L;
      this.ticks = Math.max(4, params.extra());
      this.centre = params.pos();
      this.coil = (float)Math.max(2.0, params.dir().y);
      SerpentBody body = this.body(level);
      if (body != null) {
         this.thick = Math.max(1.0F, body.radiusAt(0.9F));
      }

      this.fire = this.crush ? null : Ribbon.create(22);
      if (this.fire != null) {
         this.fire.look(this.thick * 1.3F, 12.0F, 1.35F, 0.45F, 2.0F, 0).texture(MeshTex.FLAME_RIBBON, 0.22F);
      }

      this.bolt = this.crush ? null : Ribbon.create(22);
      if (this.bolt != null) {
         this.bolt.look(this.thick * 0.6F, 10.0F, 1.4F, 0.6F, 2.4F, 1).texture(MeshTex.CRACKLE, 0.3F);
      }
   }

   @Nullable
   private SerpentBody body(ClientLevel level) {
      Entity e = this.caster(level);
      SeiryuDragonEntity shell = e == null ? null : DragonRenderer.shellOf(e);
      return shell == null ? null : shell.body();
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.crush) {
         this.tickCrush(level);
      } else {
         this.tickSweep(level);
      }
   }

   private void tickSweep(ClientLevel level) {
      SerpentBody body = this.body(level);
      if (body != null && this.age <= this.ticks + 16) {
         boolean swinging = body.action() == 1 && this.age <= this.ticks;
         if (swinging) {
            float t = VfxClock.time();
            body.point((double)(body.neck + body.length * AT[0]), 1.0F, this.p);
            body.point((double)(body.neck + body.length * AT[1]), 1.0F, this.q);
            this.hx = this.p[0];
            this.hy = this.p[1];
            this.hz = this.p[2];
            this.kx = this.q[0];
            this.ky = this.q[1];
            this.kz = this.q[2];
            if (this.fire != null) {
               this.fire.push(this.hx, this.hy, this.hz, t);
            }

            if (this.bolt != null) {
               this.bolt.push(this.kx, this.ky + (double)this.thick * 0.5, this.kz, t);
            }

            if (this.random.nextFloat() < 0.7F) {
               BoltStrike.between(
                     new Vec3(this.kx, this.ky + (double)this.thick, this.kz),
                     new Vec3(this.hx + this.spread(1.0), this.hy + (double)this.thick * 0.8, this.hz + this.spread(1.0)),
                     this.seed + (long)this.age * 37L
                  )
                  .width(0.3F)
                  .life(3)
                  .forks(2)
                  .rough(0.2F)
                  .endGlow(1.0F)
                  .spawn();
            }

            for (int i = 0; i < VfxSettings.count(4.0F); i++) {
               VfxParticles.emit(
                  i % 2 == 0 ? VfxPresets.SPARK : VfxPresets.THUNDER_SPARK,
                  this.hx + this.spread((double)this.thick),
                  this.hy + this.spread((double)this.thick),
                  this.hz + this.spread((double)this.thick),
                  this.spread(0.4),
                  this.random.nextDouble() * 0.3,
                  this.spread(0.4)
               );
            }

            VfxParticles.emit(VfxPresets.FIRE_PUFF, this.hx, this.hy, this.hz, this.spread(0.1), 0.05, this.spread(0.1)).size(2.6F * this.thick / 2.0F);
            double g = VfxWorld.ground(level, this.hx, this.hy, this.hz, 14);
            if (!Double.isNaN(g) && this.hy - g < 12.0) {
               int dust = this.shade(VfxWorld.groundColour(level, this.hx, g - 0.5, this.hz));

               for (int i = 0; i < VfxSettings.count(5.0F); i++) {
                  VfxParticles.emit(VfxPresets.DUST, this.hx + this.spread(3.0), g + 0.4, this.hz + this.spread(3.0), this.spread(0.3), 0.1, this.spread(0.3))
                     .tint(dust)
                     .size(2.2F);
               }
            }

            if (this.age == 10) {
               Impact.at(level, new Vec3(this.hx, this.hy, this.hz), this.seed ^ 62L)
                  .caster(this.caster(level))
                  .flash(1.8F)
                  .glow(16742938)
                  .sparks(40, 0.8F)
                  .embers(24)
                  .trauma(0.45F, 0.08F, 3.0F)
                  .lens(0.3F)
                  .play();
               if (!Double.isNaN(g) && this.hy - g < 12.0) {
                  GroundWave.at(this.hx, g, this.hz, 7.0F, 12)
                     .strength(1.3F)
                     .dust(20, this.shade(VfxWorld.groundColour(level, this.hx, g - 0.5, this.hz)), 0.5F, 0.45F, 1.2F, this.seed ^ 63L)
                     .spawn();
               }
            }
         }
      } else {
         this.finish();
      }
   }

   private void tickCrush(ClientLevel level) {
      if (this.age > this.ticks + 20) {
         this.finish();
      } else {
         Entity e = this.caster(level);
         double gy = VfxWorld.ground(level, this.centre.x, this.centre.y + 1.0, this.centre.z, 6);
         double floor = Double.isNaN(gy) ? this.centre.y : gy;
         if (this.age <= this.ticks) {
            for (int k = 0; k < 3; k++) {
               if (this.random.nextFloat() < 0.8F) {
                  double a = this.random.nextDouble() * (float) (Math.PI * 2);
                  double b = a + 0.6 + this.random.nextDouble() * 1.2;
                  double r = (double)this.coil + 1.2;
                  Vec3 p0 = new Vec3(this.centre.x + Math.cos(a) * r, floor + 0.5 + this.random.nextDouble() * 4.0, this.centre.z + Math.sin(a) * r);
                  Vec3 p1 = new Vec3(this.centre.x + Math.cos(b) * r, floor + 0.5 + this.random.nextDouble() * 4.0, this.centre.z + Math.sin(b) * r);
                  BoltStrike.between(p0, p1, this.seed + (long)this.age * 97L + (long)k).width(0.42F).life(4).forks(3).rough(0.2F).endGlow(1.2F).spawn();
               }
            }

            VfxParticles.emit(VfxPresets.GLOW, this.centre.x, floor + 2.0, this.centre.z, 0.0, 0.0, 0.0).size(this.coil * 5.0F).tint(11955455).alpha(0.6F);
            if (this.age % 12 == 1) {
               ArcBurst.at(new Vec3(this.centre.x, floor + 1.5, this.centre.z), this.seed + (long)this.age)
                  .arcs(3)
                  .radius(this.coil + 1.5F, this.coil + 2.2F)
                  .life(12)
                  .spin(0.6F)
                  .arc(4.08407F)
                  .look(MeshTex.CRACKLE, 1, 0.35F, 1.35F, 2.6F)
                  .tilt(0.0F, 0.5F)
                  .spawn();
            }

            if (this.age % 10 == 5) {
               Impact.at(level, new Vec3(this.centre.x, floor + 2.0, this.centre.z), this.seed + (long)this.age)
                  .caster(e)
                  .thunder()
                  .flash(1.2F)
                  .glow(11955455)
                  .sparks(26, 0.6F)
                  .embers(14)
                  .trauma(0.2F, 0.1F, 1.6F)
                  .play();
               GroundWave.at(this.centre.x, floor, this.centre.z, this.coil + 3.0F, 10)
                  .strength(1.0F)
                  .dust(14, this.shade(VfxWorld.groundColour(level, this.centre.x, floor - 0.5, this.centre.z)), 0.6F, 0.35F, 1.0F, this.seed + (long)this.age)
                  .spawn();
            }
         } else if (this.age == this.ticks + 1) {
            Vec3 c = new Vec3(this.centre.x, floor + 1.0, this.centre.z);
            BoltStrike.between(c, c.add(this.spread(2.0), 16.0, this.spread(2.0)), this.seed ^ 113L)
               .width(0.8F)
               .life(9)
               .forks(4)
               .rough(0.1F)
               .endGlow(0.0F)
               .spawn();
            Impact.at(level, c, this.seed ^ 114L)
               .caster(e)
               .thunder()
               .flash(2.0F)
               .glow(11955455)
               .wave(this.coil + 5.0F, 14, 11955455)
               .dust(24)
               .sparks(50, 0.8F)
               .embers(30)
               .smoke(10, 1708080, 1.6F)
               .debris(10, 0.3F)
               .decal(this.coil + 2.0F)
               .trauma(0.5F, 0.08F, 3.0F)
               .lens(0.35F)
               .play();
            float ex = this.exposure();
            if (ex > 0.0F) {
               CameraDirector.fovPunch(0.06F * ex);
            }
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      if (!this.crush) {
         float fade = this.age <= this.ticks ? 1.0F : Math.max(0.0F, 1.0F - ((float)(this.age - this.ticks) + f.partial) / 8.0F);
         if (this.fire != null) {
            this.fire.render(f, this.hx, this.hy, this.hz, fade);
         }

         if (this.bolt != null) {
            this.bolt.render(f, this.kx, this.ky + (double)this.thick * 0.5, this.kz, fade);
         }
      }
   }

   @Override
   public void removed() {
      if (this.fire != null) {
         this.fire.release();
      }

      if (this.bolt != null) {
         this.bolt.release();
      }
   }

   @Override
   public double x() {
      return this.crush ? this.centre.x : this.hx;
   }

   @Override
   public double y() {
      return this.crush ? this.centre.y : this.hy;
   }

   @Override
   public double z() {
      return this.crush ? this.centre.z : this.hz;
   }

   @Override
   public double cullDistance() {
      return 192.0;
   }
}
