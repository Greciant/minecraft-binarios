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
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshTex;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DragonDashCinematic extends HybridCinematic {
   private static final int COIL = 9;
   private static final int SUB = 3;
   private final int dash;
   private final Vec3 start;
   private final Vec3 end;
   private final float dx;
   private final float dy;
   private final float dz;
   private final float p1x;
   private final float p1y;
   private final float p1z;
   private final float p2x;
   private final float p2y;
   private final float p2z;
   @Nullable
   private final Ribbon body;
   @Nullable
   private final Ribbon core;
   private final IntSet struck = new IntOpenHashSet();
   private double cx;
   private double cy;
   private double cz;
   private double pcx;
   private double pcy;
   private double pcz;
   private double hx;
   private double hy;
   private double hz;
   private boolean coiled;

   public DragonDashCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.dash = Math.max(1, params.extra() - 8);
      this.start = params.pos();
      this.end = params.dir();
      Vec3 d = this.end.subtract(this.start);
      if (d.lengthSqr() < 1.0E-4) {
         d = new Vec3(0.0, 0.0, 1.0);
      }

      d = d.normalize();
      this.dx = (float)d.x;
      this.dy = (float)d.y;
      this.dz = (float)d.z;
      Vec3 a = Math.abs(d.y) > 0.95 ? new Vec3(1.0, 0.0, 0.0) : d.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
      Vec3 b = a.cross(d).normalize();
      this.p1x = (float)a.x;
      this.p1y = (float)a.y;
      this.p1z = (float)a.z;
      this.p2x = (float)b.x;
      this.p2y = (float)b.y;
      this.p2z = (float)b.z;
      this.body = Ribbon.create(36);
      if (this.body != null) {
         this.body.look(0.9F, 12.0F, 1.35F, 0.55F, 2.2F, 1).texture(MeshTex.CRACKLE, 0.3F);
      }

      this.core = Ribbon.create(36);
      if (this.core != null) {
         this.core.look(0.22F, 10.0F, 1.7F, 0.9F, 3.6F, 1).texture(MeshTex.FLAME_RIBBON, 0.35F);
      }

      this.cx = this.pcx = this.start.x;
      this.cy = this.pcy = this.start.y + (double)this.height * 0.55;
      this.cz = this.pcz = this.start.z;
      this.thunderclap(level);
   }

   private void thunderclap(ClientLevel level) {
      Vec3 c = this.start.add(0.0, (double)this.height * 0.55, 0.0);
      Entity e = this.caster(level);
      Impact.at(level, c, this.seed ^ 209L).caster(e).thunder().flash(0.65F).glow(11955455).sparks(22, 0.5F).embers(14).play();
      double gy = VfxWorld.ground(level, this.start.x, this.start.y + 0.5, this.start.z, 4);
      GroundWave w = GroundWave.at(this.start.x, Double.isNaN(gy) ? this.start.y : gy, this.start.z, 2.8F, 10).strength(1.1F).air(c.y, 2.2F);
      if (!Double.isNaN(gy) && this.start.y - gy < 2.5) {
         w.ring(11955455, 0.45F).dust(12, this.shade(VfxWorld.groundColour(level, this.start.x, gy - 0.5, this.start.z)), 0.6F, 0.32F, 0.55F, this.seed ^ 210L);
      }

      w.spawn();

      for (int i = 0; i < 3; i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         BoltStrike.between(c, c.add(Math.cos(a) * 1.6 - (double)this.dx, this.spread(1.0), Math.sin(a) * 1.6 - (double)this.dz), this.seed + (long)i * 71L)
            .width(0.08F)
            .life(4)
            .forks(2)
            .rough(0.25F)
            .endGlow(0.6F)
            .spawn();
      }

      float ex = this.exposure();
      if (ex > 0.0F) {
         float soft = this.firstPersonCaster() ? 0.5F : 1.0F;
         CameraDirector.snapDolly();
         CameraDirector.fovPunch(0.11F * ex);
         CameraDirector.shake(0.22F * ex, 0.12F, 1.4F, 3.0F);
         VfxPost.radialBlur(this.end.add(0.0, 1.0, 0.0), 0.2F * ex * soft);
         VfxPost.chromatic(0.25F * ex * soft);
         VfxPost.flash(11955455, 0.1F * ex * soft);
      }
   }

   private void headAt(float time, float s, double[] out) {
      if (time <= (float)(this.dash + 1)) {
         double px = Mth.lerp((double)s, this.pcx, this.cx);
         double py = Mth.lerp((double)s, this.pcy, this.cy);
         double pz = Mth.lerp((double)s, this.pcz, this.cz);
         float ang = time * 1.7F;
         float r = 1.0F * this.height / 1.8F;
         float c = Mth.cos(ang) * r;
         float sn = Mth.sin(ang) * r;
         out[0] = px + (double)this.dx * 0.9 + (double)(this.p1x * c) + (double)(this.p2x * sn);
         out[1] = py + (double)this.dy * 0.9 + (double)(this.p1y * c) + (double)(this.p2y * sn);
         out[2] = pz + (double)this.dz * 0.9 + (double)(this.p1z * c) + (double)(this.p2z * sn);
      } else {
         float k = Math.min(1.0F, (time - (float)this.dash - 1.0F) / 9.0F);
         float ang = time * 1.25F;
         float r = (0.9F + 1.0F * k) * this.height / 1.8F;
         out[0] = this.cx + (double)(Mth.cos(ang) * r);
         out[1] = this.cy - 0.2 + (double)k * 3.2 * (double)this.height / 1.8;
         out[2] = this.cz + (double)(Mth.sin(ang) * r);
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = this.caster(level);
      this.pcx = this.cx;
      this.pcy = this.cy;
      this.pcz = this.cz;
      if (e != null && this.age <= this.dash + 1) {
         this.cx = e.getX();
         this.cy = e.getY() + (double)this.height * 0.55;
         this.cz = e.getZ();
      }

      float t = VfxClock.time();
      double[] h = new double[3];
      boolean alive = this.age <= this.dash + 1 + 9;
      if (alive) {
         for (int k = 1; k <= 3; k++) {
            float s = (float)k / 3.0F;
            this.headAt((float)(this.age - 1) + s, s, h);
            if (this.body != null) {
               this.body.push(h[0], h[1], h[2], t - 1.0F + s);
            }

            if (this.core != null) {
               this.core.push(h[0], h[1], h[2], t - 1.0F + s);
            }

            VfxParticles.emit(VfxPresets.THUNDER_PUFF, h[0], h[1], h[2], this.spread(0.03), this.spread(0.03), this.spread(0.03)).size(1.5F).life(1.3F);
            if (this.random.nextFloat() < 0.5F) {
               VfxParticles.emit(VfxPresets.THUNDER_SHARD, h[0], h[1], h[2], this.spread(0.25), this.spread(0.25), this.spread(0.25));
            }
         }

         this.hx = h[0];
         this.hy = h[1];
         this.hz = h[2];
         this.head(level, t);
      }

      if (this.age <= this.dash + 1) {
         this.tickLunge(level, e);
      } else if (!this.coiled) {
         this.coiled = true;
         ArcBurst.at(new Vec3(this.cx, this.cy - (double)this.height * 0.2, this.cz), this.seed ^ 211L)
            .follow(e, 0.45F)
            .arcs(3)
            .radius(0.9F, 1.8F)
            .life(14)
            .spin(0.9F)
            .arc((float) (Math.PI * 7.0 / 5.0))
            .look(MeshTex.CRACKLE, 1, 0.3F, 1.35F, 2.6F)
            .tilt(0.1F, 0.5F)
            .spawn();

         for (int i = 0; i < VfxSettings.count(16.0F); i++) {
            VfxParticles.emit(
               VfxPresets.THUNDER_SPARK,
               this.cx,
               this.cy,
               this.cz,
               this.spread(0.4) + (double)this.dx * 0.2,
               this.random.nextDouble() * 0.3,
               this.spread(0.4) + (double)this.dz * 0.2
            );
         }
      }

      if (this.age == this.dash + 1 + 9) {
         Vec3 top = new Vec3(this.hx, this.hy, this.hz);
         Impact.at(level, top, this.seed ^ 212L).thunder().flash(1.2F).glow(11955455).sparks(26, 0.55F).embers(18).play();

         for (int i = 0; i < 3; i++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            BoltStrike.between(top, top.add(Math.cos(a) * 2.0, -0.5 - this.random.nextDouble() * 1.5, Math.sin(a) * 2.0), this.seed + (long)i * 83L)
               .width(0.09F)
               .life(4)
               .forks(2)
               .endGlow(0.6F)
               .spawn();
         }
      }

      int after = this.age - this.dash - 1;
      if (after > 0 && after < 14 && this.random.nextFloat() < 0.8F * (1.0F - (float)after / 14.0F)) {
         double u0 = this.random.nextDouble();
         double u1 = Math.min(1.0, u0 + 0.12 + this.random.nextDouble() * 0.15);
         Vec3 a = this.start.lerp(this.end, u0).add(this.spread(0.5), (double)this.height * 0.5 + this.spread(0.5), this.spread(0.5));
         Vec3 b = this.start.lerp(this.end, u1).add(this.spread(0.5), (double)this.height * 0.5 + this.spread(0.5), this.spread(0.5));
         BoltStrike.between(a, b, this.seed + (long)this.age * 59L).width(0.06F).life(3).forks(1).rough(0.25F).endGlow(0.3F).spawn();
      }

      if (this.age > this.dash + 1 + 9 + 16) {
         this.finish();
      }
   }

   private void head(ClientLevel level, float t) {
      Vec3 h = new Vec3(this.hx, this.hy, this.hz);
      VfxParticles.emit(VfxPresets.THUNDER_FLARE, h.x, h.y, h.z, 0.0, 0.0, 0.0).size(0.8F).rotation(this.random.nextFloat() * (float) (Math.PI * 2)).life(0.5F);
      VfxParticles.emit(VfxPresets.GLOW, h.x, h.y, h.z, 0.0, 0.0, 0.0).size(3.2F).tint(11955455).alpha(0.75F);
      double bx = (double)(-this.dx);
      double by = (double)(-this.dy);
      double bz = (double)(-this.dz);

      for (int side = -1; side <= 1; side += 2) {
         Vec3 horn = h.add(
            bx * 0.6 + (double)(this.p1x * (float)side) * 0.35 + (double)this.p2x * 0.55,
            by * 0.6 + (double)(this.p1y * (float)side) * 0.35 + (double)this.p2y * 0.55 + 0.25,
            bz * 0.6 + (double)(this.p1z * (float)side) * 0.35 + (double)this.p2z * 0.55
         );
         BoltStrike.between(h, horn, this.seed + (long)this.age * 7L + (long)side).width(0.05F).life(1).forks(1).rough(0.2F).endGlow(0.0F).spawn();
      }

      for (int i = 0; i < VfxSettings.count(4.0F); i++) {
         VfxParticles.emit(VfxPresets.THUNDER_SPARK, h.x, h.y, h.z, bx * 0.3 + this.spread(0.15), by * 0.3 + this.spread(0.15), bz * 0.3 + this.spread(0.15));
      }

      VfxParticles.emit(VfxPresets.THUNDER_PUFF, h.x, h.y, h.z, bx * 0.08, by * 0.08, bz * 0.08).size(0.9F);
   }

   private void tickLunge(ClientLevel level, @Nullable Entity e) {
      Vec3 a = new Vec3(this.pcx, this.pcy, this.pcz);
      Vec3 b = new Vec3(this.cx, this.cy, this.cz);
      if (this.age > 1 && a.distanceToSqr(b) > 0.25) {
         BoltStrike.between(
               a.add(this.spread(0.3), this.spread(0.3), this.spread(0.3)),
               b.add(this.spread(0.3), this.spread(0.3), this.spread(0.3)),
               this.seed + (long)this.age * 97L
            )
            .width(0.13F)
            .life(5)
            .forks(2)
            .rough(0.16F)
            .endGlow(0.0F)
            .spawn();
      }

      for (int i = 0; i < VfxSettings.count(6.0F); i++) {
         double u = this.random.nextDouble();
         VfxParticles.emit(
            VfxPresets.THUNDER_MOTE,
            Mth.lerp(u, a.x, b.x) + this.spread(0.5),
            Mth.lerp(u, a.y, b.y) + this.spread(0.6),
            Mth.lerp(u, a.z, b.z) + this.spread(0.5),
            this.spread(0.04),
            this.spread(0.04),
            this.spread(0.04)
         );
      }

      if (this.local()) {
         for (int i = 0; i < VfxSettings.count(6.0F); i++) {
            double ang = this.random.nextDouble() * (float) (Math.PI * 2);
            double px = this.cx + (double)(this.dx * 3.0F) + ((double)this.p1x * Math.cos(ang) + (double)this.p2x * Math.sin(ang)) * 1.7;
            double py = this.cy + (double)(this.dy * 3.0F) + ((double)this.p1y * Math.cos(ang) + (double)this.p2y * Math.sin(ang)) * 1.7;
            double pz = this.cz + (double)(this.dz * 3.0F) + ((double)this.p1z * Math.cos(ang) + (double)this.p2z * Math.sin(ang)) * 1.7;
            VfxParticles.emit(VfxPresets.SPEED_LINE, px, py, pz, (double)(-this.dx) * 1.6, (double)(-this.dy) * 1.6, (double)(-this.dz) * 1.6)
               .tint(14207231)
               .alpha(0.35F);
         }

         float ex = this.exposure();
         if (ex > 0.0F) {
            VfxPost.grade(10259664, 0.18F * ex);
         }
      }

      if (e != null) {
         double r = 1.4 * (double)Math.max(1.0F, this.height / 1.8F);

         for (Entity hit : level.getEntities(
            e, new AABB(this.cx - r - 1.5, this.cy - r - 1.0, this.cz - r - 1.5, this.cx + r + 1.5, this.cy + r + 1.0, this.cz + r + 1.5)
         )) {
            if (hit instanceof LivingEntity && !this.struck.contains(hit.getId())) {
               Vec3 at = hit.getBoundingBox().getCenter();
               double seg = segmentDistanceSqr(at, a, b);
               if (!(seg > r * r)) {
                  this.struck.add(hit.getId());
                  Impact.at(level, at, this.seed ^ (long)hit.getId())
                     .caster(e)
                     .thunder()
                     .flash(0.9F)
                     .glow(11955455)
                     .sparks(26, 0.55F)
                     .embers(14)
                     .trauma(0.36F, 0.11F, 2.2F)
                     .hitStop(2)
                     .lens(0.3F)
                     .play();
                  Impact.at(level, at, this.seed ^ (long)hit.getId() ^ 90L).flash(0.4F).glow(16742938).sparks(10, 0.45F).play();
                  ArcBurst.at(at, this.seed ^ (long)hit.getId() ^ 119L)
                     .follow(hit, 0.5F)
                     .arcs(3)
                     .radius(0.9F, 1.7F)
                     .life(12)
                     .spin(1.0F)
                     .arc((float) (Math.PI * 3.0 / 2.0))
                     .look(MeshTex.CRACKLE, 1, 0.22F, 1.35F, 2.6F)
                     .tilt(0.6F, 1.4F)
                     .spawn();
               }
            }
         }
      }
   }

   private static double segmentDistanceSqr(Vec3 p, Vec3 a, Vec3 b) {
      Vec3 ab = b.subtract(a);
      double l = ab.lengthSqr();
      double u = l < 1.0E-6 ? 0.0 : Mth.clamp(p.subtract(a).dot(ab) / l, 0.0, 1.0);
      return p.distanceToSqr(a.add(ab.scale(u)));
   }

   @Override
   public void render(VfxFrame f) {
      float t = (float)this.age + f.partial;
      float fade = t <= (float)(this.dash + 1 + 9) ? 1.0F : Math.max(0.0F, 1.0F - (t - (float)this.dash - 1.0F - 9.0F) / 4.0F);
      if (!(fade <= 0.01F)) {
         if (this.body != null) {
            this.body.render(f, this.hx, this.hy, this.hz, fade);
         }

         if (this.core != null) {
            this.core.render(f, this.hx, this.hy, this.hz, fade);
         }
      }
   }

   @Override
   public void removed() {
      if (this.body != null) {
         this.body.release();
      }

      if (this.core != null) {
         this.core.release();
      }
   }

   @Override
   public double x() {
      return this.cx;
   }

   @Override
   public double y() {
      return this.cy;
   }

   @Override
   public double z() {
      return this.cz;
   }
}
