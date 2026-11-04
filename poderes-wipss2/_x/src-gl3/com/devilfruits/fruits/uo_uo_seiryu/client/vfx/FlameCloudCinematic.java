package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxDirector;
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
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.mojang.blaze3d.vertex.BufferBuilder;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class FlameCloudCinematic extends HybridCinematic {
   private final int dash;
   private final float cloud;
   private final Vec3 start;
   private float dirX;
   private float dirY;
   private float dirZ = 1.0F;
   @Nullable
   private final Ribbon ribbonA;
   @Nullable
   private final Ribbon ribbonB;
   private final double[] heads = new double[6];
   private final IntSet struck = new IntOpenHashSet();
   private int trailAge = -1;
   private int linger;
   private Vec3 a = Vec3.ZERO;
   private Vec3 b = Vec3.ZERO;
   private float trailWidth = 1.3F;

   public FlameCloudCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.dash = Math.max(1, params.extra());
      this.cloud = Math.max(0.8F, params.power());
      this.start = params.pos();
      Vec3 d = params.dir();
      if (d.lengthSqr() > 1.0E-6) {
         Vec3 n = d.normalize();
         this.dirX = (float)n.x;
         this.dirY = (float)n.y;
         this.dirZ = (float)n.z;
      }

      this.ribbonA = Ribbon.create(18);
      if (this.ribbonA != null) {
         this.ribbonA.look(0.42F, 9.0F, 1.3F, 0.45F, 1.8F, 0).texture(MeshTex.FLAME_RIBBON, 0.24F);
      }

      this.ribbonB = Ribbon.create(18);
      if (this.ribbonB != null) {
         this.ribbonB.look(0.3F, 8.0F, 1.4F, 0.5F, 2.2F, 0).texture(MeshTex.FLAME_RIBBON, 0.28F);
      }

      this.pushOff(level, params.pos());
   }

   @Nullable
   public static FlameCloudCinematic riding(Entity e) {
      return VfxDirector.find(FlameCloudCinematic.class, c -> c.follows(e) && c.trailAge < 0);
   }

   private void pushOff(ClientLevel level, Vec3 from) {
      double gy = VfxWorld.ground(level, from.x, from.y + 0.5, from.z, 4);
      Vec3 c = from.add(0.0, 0.5, 0.0);
      VfxParticles.emit(VfxPresets.FLARE, c.x, c.y, c.z, 0.0, 0.0, 0.0).size(0.75F).rotation(this.random.nextFloat() * (float) (Math.PI * 2));
      VfxParticles.emit(VfxPresets.GLOW, c.x, c.y, c.z, 0.0, 0.0, 0.0).size(3.0F).tint(15217182).alpha(0.6F).life(1.4F);

      for (int i = 0; i < VfxSettings.count(18.0F); i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         VfxParticles.emit(
               VfxPresets.FIRE_PUFF,
               c.x,
               c.y - 0.2,
               c.z,
               Math.cos(a) * 0.22 - (double)this.dirX * 0.1,
               0.03 + this.random.nextDouble() * 0.08,
               Math.sin(a) * 0.22 - (double)this.dirZ * 0.1
            )
            .size(1.3F);
      }

      for (int i = 0; i < VfxSettings.count(8.0F); i++) {
         VfxParticles.emit(
               VfxPresets.SMOKE,
               c.x + this.spread(0.6),
               c.y,
               c.z + this.spread(0.6),
               this.spread(0.08) - (double)this.dirX * 0.06,
               0.04,
               this.spread(0.08) - (double)this.dirZ * 0.06
            )
            .tint(this.shade(3811876))
            .size(1.2F)
            .alpha(0.8F);
      }

      if (!Double.isNaN(gy) && from.y - gy < 2.5) {
         GroundWave.at(from.x, gy, from.z, 2.6F, 10)
            .strength(1.0F)
            .ring(16767136, 0.6F)
            .dust(14, this.shade(VfxWorld.groundColour(level, from.x, gy - 0.5, from.z)), 0.6F, 0.32F, 0.6F, this.seed ^ 193L)
            .spawn();
         ArcBurst.at(new Vec3(from.x, gy + 0.2, from.z), this.seed ^ 194L)
            .arcs(3)
            .radius(0.4F, 2.4F)
            .life(10)
            .spin(0.5F)
            .arc((float) Math.PI * 11.0F / 10.0F)
            .look(MeshTex.FLAME_RIBBON, 0, 0.35F, 1.3F, 2.0F)
            .tilt(0.0F, 0.08F)
            .spawn();
      }

      float ex = this.exposure();
      if (ex > 0.0F) {
         float soft = this.firstPersonCaster() ? 0.5F : 1.0F;
         CameraDirector.fovPunch(0.08F * ex);
         CameraDirector.shake(0.18F * ex, 0.12F, 1.2F, 3.0F);
         VfxPost.radialBlur(from.add((double)(this.dirX * 6.0F), 1.0, (double)(this.dirZ * 6.0F)), 0.14F * ex * soft);
      }
   }

   public void trail(ClientLevel level, Vec3 from, Vec3 to, float width, int ticks) {
      this.trailAge = 0;
      this.a = from.add(0.0, -0.6, 0.0);
      this.b = to.add(0.0, -0.6, 0.0);
      this.trailWidth = Math.max(0.8F, width);
      this.linger = Math.max(10, ticks);
      Vec3 c = to.add(0.0, 0.4, 0.0);

      for (int i = 0; i < VfxSettings.count(16.0F); i++) {
         double ang = this.random.nextDouble() * (float) (Math.PI * 2);
         VfxParticles.emit(
               VfxPresets.FIRE_PUFF,
               c.x,
               c.y,
               c.z,
               Math.cos(ang) * 0.2 + (double)this.dirX * 0.15,
               0.04 + this.random.nextDouble() * 0.1,
               Math.sin(ang) * 0.2 + (double)this.dirZ * 0.15
            )
            .size(1.4F);
      }

      for (int i = 0; i < VfxSettings.count(8.0F); i++) {
         VfxParticles.emit(
               VfxPresets.SMOKE,
               c.x + this.spread(0.8),
               c.y + 0.3,
               c.z + this.spread(0.8),
               this.spread(0.06) + (double)this.dirX * 0.05,
               0.05,
               this.spread(0.06) + (double)this.dirZ * 0.05
            )
            .tint(this.shade(3811876))
            .size(1.4F)
            .alpha(0.75F);
      }

      double len = from.distanceTo(to);
      int marks = Math.max(2, (int)(len / 1.6));

      for (int i = 0; i <= marks; i++) {
         Vec3 p = this.a.lerp(this.b, (double)i / (double)marks);
         double gy = VfxWorld.ground(level, p.x, p.y + 1.2, p.z, 4);
         if (!Double.isNaN(gy)) {
            float rot = this.random.nextFloat() * (float) (Math.PI * 2);
            VfxParticles.emit(VfxPresets.SCORCH, p.x, gy + 0.03, p.z, 0.0, 0.0, 0.0).size(this.trailWidth * 1.6F).rotation(rot).life(1.4F);
            VfxParticles.emit(VfxPresets.SCORCH_GLOW, p.x, gy + 0.035, p.z, 0.0, 0.0, 0.0).size(this.trailWidth * 1.5F).rotation(rot).life(1.2F);
         }
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = this.caster(level);
      if (this.trailAge < 0) {
         if (e != null) {
            this.tickRide(level, e);
         }

         if (this.age > this.dash + 12) {
            this.trail(level, this.start.add(0.0, 0.6, 0.0), new Vec3(this.x, this.y + 0.6, this.z), this.cloud, 20);
         }
      } else {
         this.trailAge++;
         this.tickTrail(level);
         if (this.trailAge > this.linger + 30) {
            this.finish();
         }
      }
   }

   private void tickRide(ClientLevel level, Entity e) {
      float t = VfxClock.time();
      double cy = this.y + (double)this.height * 0.35;

      for (int i = 0; i < VfxSettings.count(7.0F); i++) {
         double px = this.x + this.spread((double)this.cloud * 0.7);
         double py = this.y + this.random.nextDouble() * (double)this.height * 0.55;
         double pz = this.z + this.spread((double)this.cloud * 0.7);
         VfxParticles.emit(
               i % 3 == 0 ? VfxPresets.FIRE_TONGUE : VfxPresets.FIRE_PUFF,
               px,
               py,
               pz,
               (double)(-this.dirX) * 0.18 + this.spread(0.05),
               0.02 + this.random.nextDouble() * 0.05,
               (double)(-this.dirZ) * 0.18 + this.spread(0.05)
            )
            .size(1.2F + this.random.nextFloat() * 0.7F);
      }

      for (int i = 0; i < VfxSettings.count(6.0F); i++) {
         VfxParticles.emit(
               VfxPresets.SMOKE,
               this.x + this.spread((double)this.cloud * 0.6),
               this.y + this.random.nextDouble() * 0.6,
               this.z + this.spread((double)this.cloud * 0.6),
               (double)(-this.dirX) * 0.1 + this.spread(0.04),
               0.03,
               (double)(-this.dirZ) * 0.1 + this.spread(0.04)
            )
            .tint(this.shade(4599850))
            .size(1.1F + this.random.nextFloat() * 0.5F)
            .alpha(0.75F);
      }

      for (int i = 0; i < VfxSettings.count(3.0F); i++) {
         VfxParticles.emit(
            VfxPresets.EMBER,
            this.x + this.spread(0.8),
            cy + this.spread(0.5),
            this.z + this.spread(0.8),
            (double)(-this.dirX) * 0.2 + this.spread(0.1),
            0.06,
            (double)(-this.dirZ) * 0.2 + this.spread(0.1)
         );
      }

      if (this.random.nextFloat() < 0.6F) {
         Vec3 c = new Vec3(this.x + this.spread(0.6), this.y + 0.3 + this.random.nextDouble() * 0.6, this.z + this.spread(0.6));
         double ang = this.random.nextDouble() * (float) (Math.PI * 2);
         BoltStrike.between(c, c.add(Math.cos(ang) * 1.1, this.spread(0.5), Math.sin(ang) * 1.1), this.seed + (long)this.age * 43L)
            .width(0.06F)
            .life(2)
            .forks(1)
            .rough(0.33F)
            .endGlow(0.4F)
            .spawn();
      }

      for (int k = 0; k < 2; k++) {
         float ang = t * 0.9F + (float)k * (float) Math.PI;
         double r = 0.55 * (double)this.height / 1.8 * (double)this.cloud / 1.3;
         this.heads[k * 3] = this.x + Math.cos((double)ang) * r - (double)this.dirX * 0.3;
         this.heads[k * 3 + 1] = this.y + (double)this.height * (0.25 + 0.25 * (double)k) + (double)Mth.sin(ang * 0.7F) * 0.15;
         this.heads[k * 3 + 2] = this.z + Math.sin((double)ang) * r - (double)this.dirZ * 0.3;
         Ribbon rb = k == 0 ? this.ribbonA : this.ribbonB;
         if (rb != null) {
            rb.push(this.heads[k * 3], this.heads[k * 3 + 1], this.heads[k * 3 + 2], t);
         }
      }

      if (this.local()) {
         for (int i = 0; i < VfxSettings.count(4.0F); i++) {
            double ang = this.random.nextDouble() * (float) (Math.PI * 2);
            double px = this.x + (double)(this.dirX * 3.0F) + Math.cos(ang) * 1.6;
            double py = this.y + 1.0 + this.spread(1.1);
            double pz = this.z + (double)(this.dirZ * 3.0F) + Math.sin(ang) * 1.6;
            VfxParticles.emit(VfxPresets.SPEED_LINE, px, py, pz, (double)(-this.dirX) * 1.2, (double)(-this.dirY) * 1.2, (double)(-this.dirZ) * 1.2)
               .tint(16769216)
               .alpha(0.3F);
         }
      }

      double r = (double)this.cloud + 0.3;

      for (Entity hit : level.getEntities(e, new AABB(this.x - r, this.y - 1.0, this.z - r, this.x + r, this.y + (double)this.height + 1.0, this.z + r))) {
         if (hit instanceof LivingEntity && !this.struck.contains(hit.getId()) && !(hit.distanceToSqr(e) > (r + 0.5) * (r + 0.5))) {
            this.struck.add(hit.getId());
            Vec3 at = hit.getBoundingBox().getCenter();
            Impact.at(level, at, this.seed ^ (long)hit.getId())
               .caster(e)
               .flash(0.8F)
               .glow(16742938)
               .sparks(24, 0.5F)
               .embers(16)
               .smoke(5, 2761760, 1.0F)
               .trauma(0.32F, 0.11F, 2.0F)
               .hitStop(1)
               .lens(0.2F)
               .play();

            for (int i = 0; i < VfxSettings.count(10.0F); i++) {
               VfxParticles.emit(
                     VfxPresets.FIRE_PUFF,
                     at.x + this.spread(0.4),
                     at.y + this.spread(0.6),
                     at.z + this.spread(0.4),
                     (double)this.dirX * 0.12 + this.spread(0.08),
                     0.06,
                     (double)this.dirZ * 0.12 + this.spread(0.08)
                  )
                  .size(1.1F);
            }
         }
      }
   }

   private void tickTrail(ClientLevel level) {
      float fade = this.trailAge < this.linger ? 1.0F : Math.max(0.0F, 1.0F - (float)(this.trailAge - this.linger) / 20.0F);
      if (!(fade <= 0.0F)) {
         double len = this.a.distanceTo(this.b);
         int n = VfxSettings.count((float)(1.2 + len * 0.55) * fade);

         for (int i = 0; i < n; i++) {
            double u = this.random.nextDouble();
            double px = Mth.lerp(u, this.a.x, this.b.x) + this.spread((double)this.trailWidth * 0.55);
            double pz = Mth.lerp(u, this.a.z, this.b.z) + this.spread((double)this.trailWidth * 0.55);
            double py = Mth.lerp(u, this.a.y, this.b.y);
            double gy = VfxWorld.ground(level, px, py + 1.2, pz, 4);
            double fy = Double.isNaN(gy) ? py : gy;
            VfxParticles.emit(
                  i % 2 == 0 ? VfxPresets.FIRE_TONGUE : VfxPresets.FIRE_PUFF,
                  px,
                  fy + 0.1,
                  pz,
                  this.spread(0.02),
                  0.06 + this.random.nextDouble() * 0.06,
                  this.spread(0.02)
               )
               .size((0.8F + this.random.nextFloat() * 0.6F) * (0.5F + 0.5F * fade));
            if (this.random.nextFloat() < 0.3F) {
               VfxParticles.emit(VfxPresets.EMBER, px, fy + 0.3, pz, this.spread(0.04), 0.08 + this.random.nextDouble() * 0.05, this.spread(0.04));
            }
         }

         if (this.trailAge % 3 == 0) {
            double u = this.random.nextDouble();
            Vec3 p = this.a.lerp(this.b, u);
            VfxParticles.emit(VfxPresets.SMOKE, p.x + this.spread(0.4), p.y + 1.0, p.z + this.spread(0.4), this.spread(0.02), 0.05, this.spread(0.02))
               .tint(this.shade(3024418))
               .size(1.1F)
               .alpha(0.6F * fade);
         }

         int glows = Math.max(2, (int)(len / 2.5));

         for (int ix = 0; ix <= glows; ix++) {
            Vec3 p = this.a.lerp(this.b, (double)ix / (double)glows);
            VfxParticles.emit(VfxPresets.GROUND_GLOW, p.x, p.y + 0.08, p.z, 0.0, 0.0, 0.0).size(this.trailWidth * 2.6F).tint(16742938).alpha(0.55F * fade);
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      float t = (float)this.age + f.partial;
      float fade = this.trailAge < 0 ? 1.0F : Math.max(0.0F, 1.0F - ((float)this.trailAge + f.partial) / 8.0F);
      if (fade > 0.01F && !this.firstPersonCaster()) {
         if (this.ribbonA != null) {
            this.ribbonA.render(f, this.heads[0], this.heads[1], this.heads[2], fade);
         }

         if (this.ribbonB != null) {
            this.ribbonB.render(f, this.heads[3], this.heads[4], this.heads[5], fade);
         }
      }

      BufferBuilder d = f.distortion();
      if (d != null) {
         VfxSprites.Sprite glow = VfxSprites.GLOW;
         if (this.trailAge < 0) {
            float p = f.entityPartial;
            MeshWriter.distortQuad(
               d,
               f.rx(this.rx(p)),
               f.ry(this.ry(p) + (double)this.height * 0.4),
               f.rz(this.rz(p)),
               this.cloud * 1.6F,
               0.6F,
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
               0.3F
            );
         } else {
            float trail = this.trailAge < this.linger ? 1.0F : Math.max(0.0F, 1.0F - (float)(this.trailAge - this.linger) / 20.0F);
            int n = Math.max(2, (int)(this.a.distanceTo(this.b) / 2.2));

            for (int i = 0; i <= n && trail > 0.02F; i++) {
               Vec3 p = this.a.lerp(this.b, (double)i / (double)n);
               MeshWriter.distortQuad(
                  d,
                  f.rx(p.x),
                  f.ry(p.y + 1.0),
                  f.rz(p.z),
                  1.3F,
                  0.5F * trail,
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
                  (float)i * 0.19F + f.time * 0.005F,
                  0.4F
               );
            }
         }
      }
   }

   @Override
   public void removed() {
      if (this.ribbonA != null) {
         this.ribbonA.release();
      }

      if (this.ribbonB != null) {
         this.ribbonB.release();
      }
   }

   @Override
   public double x() {
      return this.trailAge < 0 ? this.x : (this.a.x + this.b.x) * 0.5;
   }

   @Override
   public double y() {
      return this.trailAge < 0 ? this.y + 1.0 : (this.a.y + this.b.y) * 0.5 + 1.0;
   }

   @Override
   public double z() {
      return this.trailAge < 0 ? this.z : (this.a.z + this.b.z) * 0.5;
   }
}
