package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.ArcBurst;
import com.devilfruits.client.vfx.fx.ArcStrip;
import com.devilfruits.client.vfx.fx.BoltStrike;
import com.devilfruits.client.vfx.fx.GroundWave;
import com.devilfruits.client.vfx.fx.Impact;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.fruits.uo_uo_seiryu.client.render.HybridAnchors;
import com.mojang.blaze3d.vertex.BufferBuilder;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class BaguaCinematic extends HybridCinematic {
   static final int VIOLET = 11955455;
   static final int LIGHT = 15984895;
   private static final int DARK_GRADE = 8023198;
   private static final float SWING_FROM = -0.55F;
   private static final float SWING_TO = 2.35F;
   private static final int AFTER = 34;
   private final int windup;
   private final int swingStart;
   private float k;
   private boolean hybrid;
   private int struck = -1;
   private Vec3 at = Vec3.ZERO;
   private float dirX;
   private float dirZ = 1.0F;
   private float radius = 4.5F;
   private float ax;
   private float ay;
   private float az;
   private float fx;
   private float fz;
   private boolean planeSet;

   public BaguaCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.windup = Math.max(1, params.extra());
      this.swingStart = Math.max(0, Math.min(7, this.windup - 1));
      this.hybrid = params.power() > 0.0F;
      this.k = this.hybrid ? 1.7F : 1.0F;
   }

   @Nullable
   public static BaguaCinematic of(Entity e) {
      return VfxDirector.find(BaguaCinematic.class, c -> c.follows(e) && c.struck < 0);
   }

   private void plane(Entity e) {
      float yaw = (e instanceof LivingEntity l ? l.yBodyRot : e.getYRot()) * (float) (Math.PI / 180.0);
      this.fx = -Mth.sin(yaw);
      this.fz = Mth.cos(yaw);
      float rx = -this.fz;
      float rz = this.fx;
      this.ax = rx * 0.6F;
      this.ay = 0.8F;
      this.az = rz * 0.6F;
      this.planeSet = true;
   }

   private float swing(float t) {
      if (t <= (float)this.swingStart) {
         return -0.55F;
      } else {
         float s = Mth.clamp((t - (float)this.swingStart) / 2.0F, 0.0F, 1.0F);
         float e = 1.0F - (float)Math.pow(2.0, (double)(-8.0F * s));
         return -0.55F + 2.8999999F * e;
      }
   }

   private double reach() {
      return 2.4 * (double)this.height / 1.8;
   }

   private double chestY() {
      return this.y + (double)this.height * 0.72;
   }

   private Vec3 head(float a) {
      double r = this.reach();
      double c = Math.cos((double)a);
      double s = Math.sin((double)a);
      return new Vec3(
         this.x + ((double)this.ax * c + (double)this.fx * s) * r,
         this.chestY() + (double)this.ay * c * r,
         this.z + ((double)this.az * c + (double)this.fz * s) * r
      );
   }

   private Vec3 raised(float p) {
      double sc = (double)this.height / 1.8;
      float rx = -this.fz;
      float rz = this.fx;
      return new Vec3(
         this.x + ((double)rx * 0.5 - (double)this.fx * 0.25) * sc,
         this.y + (1.35 + 0.55 * (double)p) * sc,
         this.z + ((double)rz * 0.5 - (double)this.fz * 0.25) * sc
      );
   }

   public void strike(ClientLevel level, Vec3 impact, Vec3 dir, float power, boolean hybrid) {
      this.struck = this.age;
      this.hybrid = hybrid;
      this.at = impact;
      double dl = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
      if (dl > 1.0E-4) {
         this.dirX = (float)(dir.x / dl);
         this.dirZ = (float)(dir.z / dl);
      }

      this.k = hybrid ? 1.7F : 1.0F;
      this.radius = Math.max(2.0F, power);
      Entity e = this.caster(level);
      if (!this.planeSet && e != null) {
         this.plane(e);
      }

      float ex = this.exposure();
      Vec3 c = this.at.add(0.0, 0.35, 0.0);
      Impact.at(level, c, this.seed ^ 177L)
         .caster(e)
         .thunder()
         .flash(1.5F * this.k)
         .glow(11955455)
         .wave(this.radius * 1.2F * this.k, 14, 11955455)
         .dust(Math.round(18.0F * this.k))
         .sparks(Math.round(40.0F * this.k), 0.62F)
         .embers(Math.round(26.0F * this.k))
         .smoke(Math.round(12.0F * this.k), 1708080, 1.25F * this.k)
         .debris(Math.round(7.0F * this.k), 0.24F)
         .decal(this.radius * 0.9F * this.k)
         .trauma(0.55F * this.k, 0.085F, 3.2F)
         .hitStop(hybrid ? 3 : 2)
         .fov(0.07F)
         .lens(0.45F)
         .screenFlash(0.32F)
         .play();
      Impact.at(level, c, this.seed ^ 178L)
         .flash(0.7F * this.k)
         .glow(16742938)
         .sparks(Math.round(18.0F * this.k), 0.5F)
         .embers(Math.round(12.0F * this.k))
         .play();
      double floor = VfxWorld.ground(level, this.at.x, this.at.y + 0.5, this.at.z, 4);
      double fy = Double.isNaN(floor) ? this.at.y : floor;
      ArcBurst.at(new Vec3(this.at.x, fy + 0.18, this.at.z), this.seed ^ 179L)
         .arcs(4)
         .radius(0.5F, this.radius * 1.45F * this.k)
         .life(12)
         .spin(0.32F)
         .arc(2.670354F)
         .look(MeshTex.CRACKLE, 1, 0.38F * this.k, 1.35F, 2.6F)
         .tilt(0.0F, 0.06F)
         .fadeIn(0.05F)
         .spawn();
      ArcBurst.at(new Vec3(this.at.x, fy + 0.12, this.at.z), this.seed ^ 180L)
         .arcs(3)
         .radius(0.3F, this.radius * 1.0F * this.k)
         .life(9)
         .spin(-0.45F)
         .arc(1.8849558F)
         .look(MeshTex.FLAME_RIBBON, 1, 0.16F * this.k, 1.6F, 3.2F)
         .tilt(0.0F, 0.05F)
         .fadeIn(0.05F)
         .spawn();
      GroundWave.at(this.at.x, fy, this.at.z, this.radius * 0.9F * this.k, 10).strength(1.2F).air(c.y + 0.6, this.radius * 0.9F * this.k).spawn();
      this.fan(level, 4, 0);
      BoltStrike.between(c, c.add(this.spread(1.2), (double)(9.0F * this.k), this.spread(1.2)), this.seed ^ 181L)
         .width(0.45F * this.k)
         .life(9)
         .forks(4)
         .rough(0.1F)
         .heat(1.25F)
         .endGlow(0.0F)
         .spawn();
      BoltStrike.between(c, c.add(this.spread(2.5), (double)(5.0F * this.k), this.spread(2.5)), this.seed ^ 182L)
         .width(0.16F * this.k)
         .life(6)
         .forks(2)
         .rough(0.16F)
         .endGlow(0.0F)
         .spawn();
      if (ex > 0.0F) {
         VfxPost.flash(11955455, 0.22F * ex * (this.firstPersonCaster() ? 0.5F : 1.0F));
         CameraDirector.fovPunch(0.05F * ex);
      }
   }

   private void fan(ClientLevel level, int n, int first) {
      Vec3 c = this.at.add(0.0, 0.45, 0.0);
      int slots = this.hybridSlots();

      for (int i = first; i < first + n && i < slots; i++) {
         float off = ((float)i / (float)(slots - 1) - 0.5F) * 1.25F + (float)this.spread(0.08);
         float cs = Mth.cos(off);
         float sn = Mth.sin(off);
         double bx = (double)(this.dirX * cs - this.dirZ * sn);
         double bz = (double)(this.dirZ * cs + this.dirX * sn);
         double r = (double)this.radius * (1.05 + this.random.nextDouble() * 0.45) * (double)this.k;
         Vec3 end = c.add(bx * r, (i & 1) == 0 ? this.spread(0.3) : 0.6 + this.random.nextDouble() * 1.6, bz * r);
         BoltStrike.between(c, end, this.seed + (long)i * 977L)
            .width((0.22F + this.random.nextFloat() * 0.1F) * this.k)
            .life(8 + this.random.nextInt(3))
            .forks(3)
            .rough(0.17F)
            .heat(1.15F)
            .endGlow(1.1F)
            .spawn();
      }
   }

   private int hybridSlots() {
      return this.k > 1.2F ? 10 : 7;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = this.caster(level);
      if (e != null && !this.planeSet) {
         this.plane(e);
      }

      if (this.struck < 0) {
         this.tickWindup(level, e);
         if (this.age > this.windup + 12) {
            this.finish();
         }
      } else {
         int s = this.age - this.struck;
         if (s == 1) {
            this.fan(level, 3, 4);
         } else if (s == 2 && this.hybridSlots() > 7) {
            this.fan(level, 3, 7);
         }

         if (this.hybrid && (s == 2 || s == 4 || s == 6)) {
            double a = Math.atan2((double)this.dirZ, (double)this.dirX) + (double)(s - 4) * 0.9 + this.spread(0.3);
            double r = (double)this.radius * (0.7 + this.random.nextDouble() * 0.4) * (double)this.k * 0.8;
            double floor = VfxWorld.ground(level, this.at.x, this.at.y + 0.5, this.at.z, 4);
            Vec3 g = new Vec3(this.at.x + Math.cos(a) * r, Double.isNaN(floor) ? this.at.y : floor, this.at.z + Math.sin(a) * r);
            BoltStrike.between(g.add(this.spread(3.0), 16.0, this.spread(3.0)), g, this.seed + (long)s * 211L)
               .width(0.4F)
               .life(8)
               .forks(4)
               .rough(0.1F)
               .heat(1.25F)
               .endGlow(1.8F)
               .spawn();
            Impact.at(level, g.add(0.0, 0.3, 0.0), this.seed + (long)s * 223L)
               .caster(this.caster(level))
               .thunder()
               .flash(0.9F)
               .glow(11955455)
               .wave(3.0F, 10, 11955455)
               .sparks(18, 0.5F)
               .debris(4, 0.2F)
               .decal(2.2F)
               .trauma(0.2F, 0.1F, 1.5F)
               .play();
         }

         this.tickAfter(level, s);
         if (s > 34) {
            this.finish();
         }
      }
   }

   private void tickWindup(ClientLevel level, @Nullable Entity e) {
      float t = VfxClock.time();
      float p = Math.min(1.0F, (float)this.age / (float)this.windup);
      Vec3 head = this.raised(p);
      Vec3 hands = new Vec3(
         this.x + (double)this.fx * 0.35 * (double)this.height / 1.8, this.chestY() - 0.1, this.z + (double)this.fz * 0.35 * (double)this.height / 1.8
      );
      if (this.hybrid && e != null) {
         HybridAnchors.get(e, 0, 1.0F, this.anchor3);
         double wx = this.anchor3.x;
         double wy = this.anchor3.y;
         double wz = this.anchor3.z;
         HybridAnchors.get(e, 1, 1.0F, this.anchor3);
         hands = new Vec3((wx + this.anchor3.x) * 0.5, (wy + this.anchor3.y) * 0.5, (wz + this.anchor3.z) * 0.5);
         head = hands.add(0.0, (0.4 + 0.5 * (double)p) * (double)this.height / 1.8 * 0.6, 0.0);
      }

      if (this.random.nextFloat() < 0.45F + 0.5F * p) {
         BoltStrike.between(hands, head.add(this.spread(0.25), this.spread(0.25), this.spread(0.25)), this.seed + (long)this.age * 31L)
            .width(0.06F + 0.05F * p)
            .life(2)
            .forks(2)
            .rough(0.3F)
            .rejitter(1)
            .endGlow(0.5F + 0.6F * p)
            .spawn();
      }

      if (this.random.nextFloat() < 0.5F * p) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         BoltStrike.between(head, head.add(Math.cos(a) * 0.9, this.spread(0.7), Math.sin(a) * 0.9), this.seed + (long)this.age * 37L)
            .width(0.05F)
            .life(2)
            .forks(1)
            .rough(0.35F)
            .endGlow(0.3F)
            .spawn();
      }

      for (int i = 0; i < VfxSettings.count(2.0F + 4.0F * p); i++) {
         double ox = this.spread(1.6);
         double oy = this.spread(1.2);
         double oz = this.spread(1.6);
         VfxParticles.emit(VfxPresets.THUNDER_MOTE, head.x + ox, head.y + oy, head.z + oz, -ox * 0.16, -oy * 0.16, -oz * 0.16).life(0.5F);
      }

      VfxParticles.emit(VfxPresets.GLOW, head.x, head.y, head.z, 0.0, 0.0, 0.0).size(0.7F + 1.1F * p).tint(11955455).alpha(0.45F + 0.3F * p);
      if (this.age % 3 == 0) {
         VfxParticles.emit(VfxPresets.THUNDER_FLARE, head.x, head.y, head.z, 0.0, 0.0, 0.0)
            .size(0.25F + 0.25F * p)
            .rotation(this.random.nextFloat() * (float) (Math.PI * 2));
      }

      if (this.onFloor(1.5) && this.random.nextFloat() < 0.6F * p) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double r = 0.8 + this.random.nextDouble() * 0.8;
         Vec3 f0 = new Vec3(this.x + Math.cos(a) * 0.4, this.groundY + 0.08, this.z + Math.sin(a) * 0.4);
         BoltStrike.between(f0, new Vec3(this.x + Math.cos(a) * r, this.groundY + 0.08, this.z + Math.sin(a) * r), this.seed + (long)this.age * 41L)
            .width(0.05F)
            .life(2)
            .forks(1)
            .rough(0.3F)
            .endGlow(0.0F)
            .spawn();
      }

      float ex = this.exposure();
      if (this.local() && ex > 0.0F) {
         VfxPost.grade(8023198, 0.35F * p * ex);
         VfxPost.vignette(0.22F * p * ex);
      }

      if (this.age == this.swingStart && ex > 0.0F) {
         CameraDirector.shake(0.12F * ex, 0.12F, 1.0F, 2.5F);
      }
   }

   private void tickAfter(ClientLevel level, int s) {
      float fade = 1.0F - (float)s / 34.0F;
      double floor = VfxWorld.ground(level, this.at.x, this.at.y + 0.5, this.at.z, 4);
      double fy = Double.isNaN(floor) ? this.at.y : floor;
      if (s > 2 && this.random.nextFloat() < 0.75F * fade) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double r0 = this.random.nextDouble() * (double)this.radius * 0.5 * (double)this.k;
         double r1 = r0 + 0.8 + this.random.nextDouble() * 1.4 * (double)this.k;
         double b = a + this.spread(0.6);
         BoltStrike.between(
               new Vec3(this.at.x + Math.cos(a) * r0, fy + 0.1, this.at.z + Math.sin(a) * r0),
               new Vec3(this.at.x + Math.cos(b) * r1, fy + 0.1, this.at.z + Math.sin(b) * r1),
               this.seed + (long)s * 53L
            )
            .width(0.05F + 0.04F * fade)
            .life(3)
            .forks(1)
            .rough(0.28F)
            .endGlow(0.4F * fade)
            .spawn();
      }

      for (int i = 0; i < VfxSettings.count(3.0F * fade); i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double r = this.random.nextDouble() * (double)this.radius * 0.8 * (double)this.k;
         VfxParticles.emit(
            VfxPresets.THUNDER_MOTE,
            this.at.x + Math.cos(a) * r,
            fy + 0.1,
            this.at.z + Math.sin(a) * r,
            this.spread(0.02),
            0.03 + this.random.nextDouble() * 0.05,
            this.spread(0.02)
         );
      }

      if (s < 14) {
         VfxParticles.emit(VfxPresets.GROUND_GLOW, this.at.x, fy + 0.05, this.at.z, 0.0, 0.0, 0.0)
            .size(this.radius * (this.hybrid ? 1.1F : 1.4F) * this.k)
            .tint(11955455)
            .alpha((this.hybrid ? 0.45F : 0.7F) * (1.0F - (float)s / 14.0F));
      }

      if (s % 4 == 2 && s < 24) {
         VfxParticles.emit(
               VfxPresets.SMOKE,
               this.at.x + this.spread((double)this.radius * 0.4),
               fy + 0.4,
               this.at.z + this.spread((double)this.radius * 0.4),
               this.spread(0.04),
               0.05,
               this.spread(0.04)
            )
            .tint(this.shade(1708080))
            .size(1.2F * this.k)
            .alpha(0.7F);
      }
   }

   @Override
   public void render(VfxFrame f) {
      float t = (float)this.age + f.partial;
      if (this.planeSet && !this.firstPersonCaster()) {
         this.renderSwing(f, t);
      }

      if (this.struck >= 0) {
         this.renderCone(f, t - (float)this.struck);
      }

      BufferBuilder d = f.distortion();
      if (d != null && this.struck < 0 && this.age > 1) {
         Vec3 h = this.raised(Math.min(1.0F, t / (float)this.windup));
         VfxSprites.Sprite glow = VfxSprites.GLOW;
         MeshWriter.distortQuad(
            d,
            f.rx(h.x),
            f.ry(h.y),
            f.rz(h.z),
            0.9F,
            0.45F,
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
            f.time * 0.006F,
            0.2F
         );
      }
   }

   private void renderSwing(VfxFrame f, float t) {
      float a = this.swing(t);
      float arc = a - -0.55F;
      if (!(arc < 0.05F)) {
         float end = (float)this.swingStart + 2.0F;
         float fade = t <= end + 1.0F ? 1.0F : Math.max(0.0F, 1.0F - (t - end - 1.0F) / 5.0F);
         if (!(fade <= 0.01F)) {
            float r = (float)this.reach();
            float cx = f.rx(this.x);
            float cy = f.ry(this.chestY());
            float cz = f.rz(this.z);
            ArcStrip.draw(
               f,
               MeshTex.CRACKLE,
               cx,
               cy,
               cz,
               this.ax,
               this.ay,
               this.az,
               this.fx,
               0.0F,
               this.fz,
               r,
               a,
               arc,
               0.78F * this.k,
               1.3F,
               fade,
               1,
               2.4F,
               16777215,
               -t * 0.3F,
               40
            );
            ArcStrip.draw(
               f,
               MeshTex.FLAME_RIBBON,
               cx,
               cy,
               cz,
               this.ax,
               this.ay,
               this.az,
               this.fx,
               0.0F,
               this.fz,
               r * 0.97F,
               a,
               arc * 0.75F,
               0.2F * this.k,
               1.65F,
               fade,
               1,
               3.6F,
               16777215,
               -t * 0.45F,
               32
            );
            ArcStrip.draw(
               f,
               MeshTex.FLAME_RIBBON,
               cx,
               cy,
               cz,
               this.ax,
               this.ay,
               this.az,
               this.fx,
               0.0F,
               this.fz,
               r * 0.8F,
               a,
               arc * 0.9F,
               0.4F * this.k,
               1.0F,
               fade * 0.55F,
               1,
               1.6F,
               16777215,
               -t * 0.2F,
               32
            );
         }
      }
   }

   private void renderCone(VfxFrame f, float s) {
      if (!(s > 9.0F) && f.allowMesh()) {
         float g = 1.0F - (1.0F - Math.min(1.0F, s / 4.0F)) * (1.0F - Math.min(1.0F, s / 4.0F));
         float alpha = s < 3.0F ? 1.0F : Math.max(0.0F, 1.0F - (s - 3.0F) / 6.0F);
         float bx = f.rx(this.at.x);
         float by = f.ry(this.at.y + 0.5);
         float bz = f.rz(this.at.z);
         float dy = 0.12F;
         float dl = Mth.sqrt(1.0F + dy * dy);
         float len = this.radius * 1.5F * this.k * g;
         MeshWriter.cone(
            f.mesh(MeshTex.CRACKLE),
            bx,
            by,
            bz,
            this.dirX / dl,
            dy / dl,
            this.dirZ / dl,
            len,
            0.3F * this.k,
            this.radius * 0.78F * this.k * g,
            28,
            0.0F,
            3.0F,
            -s * 0.35F,
            1.2F,
            MeshWriter.argb(16777215, 0.9F * alpha),
            MeshWriter.argb(16777215, 0.15F * alpha),
            1.45F,
            0.85F,
            1,
            2.6F,
            0.0F,
            0.45F
         );
         MeshWriter.cone(
            f.mesh(MeshTex.NOISE),
            bx,
            by,
            bz,
            this.dirX / dl,
            dy / dl,
            this.dirZ / dl,
            len * 0.8F,
            0.2F * this.k,
            this.radius * 0.5F * this.k * g,
            24,
            s * 0.02F,
            2.0F,
            -s * 0.2F,
            1.5F,
            MeshWriter.argb(16777215, 0.55F * alpha),
            MeshWriter.argb(16777215, 0.05F * alpha),
            1.5F,
            1.0F,
            1,
            1.8F,
            0.0F,
            0.3F
         );
      }
   }

   @Override
   public double y() {
      return this.struck >= 0 ? this.at.y + 1.0 : super.y();
   }
}
