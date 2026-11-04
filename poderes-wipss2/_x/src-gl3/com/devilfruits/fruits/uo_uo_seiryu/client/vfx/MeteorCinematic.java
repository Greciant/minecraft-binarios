package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.VfxSound;
import com.devilfruits.client.vfx.fx.ArcBurst;
import com.devilfruits.client.vfx.fx.BoltStrike;
import com.devilfruits.client.vfx.fx.Impact;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.Ribbon;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuSounds;
import com.mojang.blaze3d.vertex.BufferBuilder;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class MeteorCinematic extends HybridCinematic {
   private static final double GRAVITY = 0.025;
   private static final int AFTER = 50;
   private final float size;
   private final int lifetime;
   private final boolean meteor;
   private double px;
   private double py;
   private double pz;
   private double opx;
   private double opy;
   private double opz;
   private double vx;
   private double vy;
   private double vz;
   @Nullable
   private final Ribbon trail;
   @Nullable
   private Vec3 landing;
   private int landingTick = -1;
   private int hitAge = -1;
   private Vec3 hit = Vec3.ZERO;
   @Nullable
   private VfxSound roar;

   public MeteorCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.size = Math.max(0.5F, params.power());
      this.lifetime = Math.max(2, params.extra());
      Vec3 p = params.pos();
      this.px = this.opx = p.x;
      this.py = this.opy = p.y;
      this.pz = this.opz = p.z;
      this.vx = params.dir().x;
      this.vy = params.dir().y;
      this.vz = params.dir().z;
      this.meteor = this.vy < 0.0;
      this.trail = Ribbon.create(20);
      if (this.trail != null) {
         this.trail.look(0.42F * this.size, 10.0F, 1.4F, 0.4F, 2.0F, 0).texture(MeshTex.FLAME_RIBBON, 0.3F);
      }

      if (this.meteor) {
         double sx = this.px;
         double sy = this.py;
         double sz = this.pz;
         double svx = this.vx;
         double svy = this.vy;
         double svz = this.vz;

         for (int k = 1; k <= this.lifetime; k++) {
            sx += svx;
            sy += svy;
            sz += svz;
            svy -= 0.025;
            double g = VfxWorld.ground(level, sx, sy + 1.0, sz, 3);
            if (!Double.isNaN(g) && sy <= g + 0.2) {
               this.landing = new Vec3(sx, g, sz);
               this.landingTick = k;
               break;
            }
         }
      }

      if (this.meteor || Math.abs(this.vy) < 1.0) {
         int fall = this.landingTick > 0 ? this.landingTick : this.lifetime;
         this.roar = VfxSound.tracking(
            SeiryuSounds.METEOR_FALL.get(),
            () -> new Vec3(this.px, this.py, this.pz),
            true,
            a -> this.meteor ? 0.35F + 0.85F * (float)Math.pow((double)Math.min(1.0F, (float)a / (float)fall), 1.5) : 0.8F * Math.min(1.0F, this.size),
            a -> this.meteor ? 1.25F - 0.4F * Math.min(1.0F, (float)a / (float)fall) : 1.1F,
            this.lifetime + 6,
            () -> !this.isDone() && this.hitAge < 0
         );
      }
   }

   @Nullable
   public static MeteorCinematic of(long seed) {
      return VfxDirector.find(MeteorCinematic.class, c -> c.seed == seed && c.hitAge < 0);
   }

   public void impact(ClientLevel level, Vec3 at, float power) {
      this.hitAge = 0;
      if (this.roar != null) {
         this.roar.end();
      }

      this.hit = at;
      float k = Mth.clamp(power, 0.6F, 3.0F);
      if (!level.getFluidState(BlockPos.containing(at.x, at.y - 0.05, at.z)).isEmpty()) {
         for (int i = 0; i < VfxSettings.count(16.0F * k); i++) {
            VfxParticles.emit(
                  VfxPresets.SMOKE,
                  at.x + this.spread(1.2 * (double)k),
                  at.y + 0.2,
                  at.z + this.spread(1.2 * (double)k),
                  this.spread(0.05),
                  0.1 + this.random.nextDouble() * 0.08,
                  this.spread(0.05)
               )
               .tint(14210256)
               .size(1.4F * k)
               .alpha(0.6F);
         }
      } else {
         Entity e = this.caster(level);
         boolean big = this.meteor && k >= 1.5F;
         Vec3 c = at.add(0.0, 0.4, 0.0);
         Impact.at(level, c, this.seed ^ 241L)
            .caster(e)
            .flash(1.4F * k)
            .glow(16742938)
            .wave(2.4F * k, 13, 16774338)
            .dust(Math.round(8.0F * k))
            .sparks(Math.round(20.0F * k), 0.6F)
            .embers(Math.round(12.0F * k))
            .smoke(Math.round(5.0F * k), 2366488, 1.6F * k)
            .debris(big ? Math.round(4.0F * k) : 0, 0.26F)
            .decal(2.2F * k)
            .trauma(big ? 0.3F : 0.15F, 0.1F, 2.0F)
            .fov(big ? 0.03F : 0.0F)
            .lens(0.15F)
            .screenFlash(big ? 0.06F : 0.0F)
            .play();
         VfxParticles.emit(VfxPresets.FIREBALL, c.x, c.y + 0.3, c.z, 0.0, 0.05, 0.0).size(0.65F * k);

         for (int i = 0; i < VfxSettings.count(7.0F * k); i++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            VfxParticles.emit(
                  VfxPresets.FIRE_PUFF, c.x, c.y, c.z, Math.cos(a) * 0.32 * (double)k, 0.06 + this.random.nextDouble() * 0.16, Math.sin(a) * 0.32 * (double)k
               )
               .size(1.2F * k);
         }

         double g = VfxWorld.ground(level, at.x, at.y + 1.0, at.z, 3);
         if (!Double.isNaN(g)) {
            ArcBurst.at(new Vec3(at.x, g + 0.25, at.z), this.seed ^ 242L)
               .arcs(3)
               .radius(0.5F, 2.8F * k)
               .life(11)
               .spin(0.45F)
               .arc(3.7699115F)
               .look(MeshTex.FLAME_RIBBON, 0, 0.45F * k, 1.35F, 2.2F)
               .tilt(0.0F, 0.08F)
               .spawn();
         }

         for (int i = 0; i < 2; i++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            BoltStrike.between(
                  c,
                  c.add(Math.cos(a) * 2.0 * (double)k, 0.5 + this.random.nextDouble() * 1.5 * (double)k, Math.sin(a) * 2.0 * (double)k),
                  this.seed + (long)i * 17L
               )
               .width(0.08F * k)
               .life(4)
               .forks(2)
               .rough(0.28F)
               .endGlow(0.6F)
               .spawn();
         }
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.hitAge >= 0) {
         this.hitAge++;
         this.tickAfter(level);
         if (this.hitAge > (this.meteor ? 50 : 14)) {
            this.finish();
         }
      } else if (this.age > this.lifetime + 4) {
         this.impact(level, new Vec3(this.px, this.py, this.pz), this.size);
      } else {
         this.opx = this.px;
         this.opy = this.py;
         this.opz = this.pz;
         this.px = this.px + this.vx;
         this.py = this.py + this.vy;
         this.pz = this.pz + this.vz;
         this.vy -= 0.025;
         float t = VfxClock.time();
         if (this.trail != null) {
            this.trail.push(this.px, this.py, this.pz, t);
         }

         VfxParticles.emit(VfxPresets.FIREBALL, this.px, this.py, this.pz, this.vx * 0.3, this.vy * 0.3, this.vz * 0.3)
            .size(0.22F * this.size)
            .life(0.35F)
            .rotation(this.random.nextFloat() * (float) (Math.PI * 2));

         for (int i = 0; i < VfxSettings.count(1.5F * this.size); i++) {
            VfxParticles.emit(
                  VfxPresets.FIRE_PUFF,
                  this.px + this.spread(0.4 * (double)this.size),
                  this.py + this.spread(0.4 * (double)this.size),
                  this.pz + this.spread(0.4 * (double)this.size),
                  -this.vx * 0.15,
                  -this.vy * 0.15 + 0.02,
                  -this.vz * 0.15
               )
               .size(1.1F * this.size)
               .life(0.6F);
         }

         for (int i = 0; i < VfxSettings.count(0.8F * this.size); i++) {
            VfxParticles.emit(
                  VfxPresets.SMOKE,
                  this.px - this.vx * 0.8 + this.spread(0.3),
                  this.py - this.vy * 0.8 + this.spread(0.3),
                  this.pz - this.vz * 0.8 + this.spread(0.3),
                  this.spread(0.02),
                  0.02,
                  this.spread(0.02)
               )
               .tint(this.shade(1972244))
               .size(0.8F * this.size)
               .alpha(0.75F)
               .life(0.7F);
         }

         if (this.random.nextFloat() < 0.6F) {
            VfxParticles.emit(
               VfxPresets.EMBER,
               this.px,
               this.py,
               this.pz,
               -this.vx * 0.2 + this.spread(0.08),
               -this.vy * 0.2 + this.spread(0.08),
               -this.vz * 0.2 + this.spread(0.08)
            );
         }

         VfxParticles.emit(VfxPresets.GLOW, this.px, this.py, this.pz, 0.0, 0.0, 0.0).size(3.2F * this.size).tint(16742938).alpha(0.7F);
         if (this.landing != null && this.age < this.landingTick) {
            float p = (float)this.age / (float)this.landingTick;
            float r = (3.2F - 1.6F * p) * this.size;
            VfxParticles.emit(VfxPresets.TELEGRAPH, this.landing.x, this.landing.y + 0.06, this.landing.z, 0.0, 0.0, 0.0)
               .size(r)
               .tint(15217182)
               .alpha(0.4F + 0.5F * p);
            VfxParticles.emit(VfxPresets.GROUND_GLOW, this.landing.x, this.landing.y + 0.05, this.landing.z, 0.0, 0.0, 0.0)
               .size((1.5F + 3.0F * p) * this.size)
               .tint(16742938)
               .alpha(0.25F + 0.5F * p);
         }
      }
   }

   private void tickAfter(ClientLevel level) {
      if (this.meteor) {
         float fade = 1.0F - (float)this.hitAge / 50.0F;
         if (this.hitAge % 3 == 0 && this.hitAge < 40) {
            VfxParticles.emit(
                  VfxPresets.SMOKE,
                  this.hit.x + this.spread(0.8),
                  this.hit.y + 1.0 + (double)this.hitAge * 0.12,
                  this.hit.z + this.spread(0.8),
                  this.spread(0.03),
                  0.12,
                  this.spread(0.03)
               )
               .tint(this.shade(1840916))
               .size(1.7F * this.size)
               .alpha(0.8F * fade);
         }

         for (int i = 0; i < VfxSettings.count(3.0F * fade); i++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            double r = this.random.nextDouble() * 2.0 * (double)this.size;
            VfxParticles.emit(
                  VfxPresets.FIRE_TONGUE,
                  this.hit.x + Math.cos(a) * r,
                  this.hit.y + 0.15,
                  this.hit.z + Math.sin(a) * r,
                  this.spread(0.02),
                  0.07,
                  this.spread(0.02)
               )
               .size(1.1F * this.size * fade);
         }

         VfxParticles.emit(VfxPresets.GROUND_GLOW, this.hit.x, this.hit.y + 0.05, this.hit.z, 0.0, 0.0, 0.0)
            .size(4.5F * this.size)
            .tint(16742938)
            .alpha(0.6F * fade);
      }
   }

   @Override
   public void render(VfxFrame f) {
      if (this.hitAge < 0 && f.allowMesh()) {
         float p = f.partial;
         double x0 = Mth.lerp((double)p, this.opx, this.px);
         double y0 = Mth.lerp((double)p, this.opy, this.py);
         double z0 = Mth.lerp((double)p, this.opz, this.pz);
         float t = (float)this.age + p;
         float cx = f.rx(x0);
         float cy = f.ry(y0);
         float cz = f.rz(z0);
         if (this.trail != null) {
            this.trail.render(f, x0, y0, z0, 1.0F);
         }

         BufferBuilder noise = f.mesh(MeshTex.NOISE);
         MeshWriter.sphere(
            noise, cx, cy, cz, 0.62F * this.size, 8, 12, t * 0.05F, -t * 0.09F, 2.0F, MeshWriter.argb(16777215, 0.85F), 1.15F, 0, 1.6F, 0.0F, 0.6F
         );
         MeshWriter.sphere(
            noise, cx, cy, cz, 0.36F * this.size, 8, 12, -t * 0.07F, t * 0.05F, 2.0F, MeshWriter.argb(16777215, 0.95F), 1.6F, 0, 3.0F, 0.0F, 0.3F
         );
         BufferBuilder d = f.distortion();
         if (d != null) {
            VfxSprites.Sprite glow = VfxSprites.GLOW;
            MeshWriter.distortQuad(
               d,
               cx,
               cy,
               cz,
               1.6F * this.size,
               0.8F,
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
               f.time * 0.008F,
               0.3F
            );
         }
      } else {
         if (this.trail != null && this.hitAge >= 0 && this.hitAge < 8) {
            this.trail.render(f, this.hit.x, this.hit.y, this.hit.z, 1.0F - (float)this.hitAge / 8.0F);
         }
      }
   }

   @Override
   public void removed() {
      if (this.trail != null) {
         this.trail.release();
      }
   }

   @Override
   public double x() {
      return this.hitAge >= 0 ? this.hit.x : this.px;
   }

   @Override
   public double y() {
      return this.hitAge >= 0 ? this.hit.y : this.py;
   }

   @Override
   public double z() {
      return this.hitAge >= 0 ? this.hit.z : this.pz;
   }

   @Override
   public double cullDistance() {
      return 160.0;
   }
}
