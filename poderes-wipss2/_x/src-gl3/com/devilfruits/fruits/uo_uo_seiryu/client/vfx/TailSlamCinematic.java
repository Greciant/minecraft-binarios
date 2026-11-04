package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.model.geo.GeoAnimation;
import com.devilfruits.client.model.geo.GeoResources;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.ArcStrip;
import com.devilfruits.client.vfx.fx.Impact;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.Ribbon;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuSounds;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.fruits.uo_uo_seiryu.client.render.HybridAnchors;
import com.devilfruits.registry.DFSounds;
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

public class TailSlamCinematic extends HybridCinematic {
   private final int sweepStart;
   private final int sweepEnd;
   private final float radius;
   private final double base;
   private final double half;
   private final double cx;
   private final double cz;
   @Nullable
   private final Ribbon tail;
   private final IntSet struck = new IntOpenHashSet();
   private int waveAge = -1;

   public TailSlamCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      GeoAnimation clip = GeoResources.animation(UoUoSeiryu.anim("hybrid/draconic_tail_slam"));
      this.sweepStart = Math.round(clip != null ? clip.marker("sweep_start", 3.0F) : 3.0F);
      this.sweepEnd = Math.max(this.sweepStart + 1, Math.round(clip != null ? clip.marker("sweep_end", 8.0F) : 8.0F));
      this.radius = Math.max(2.0F, params.power());
      Vec3 d = params.dir();
      this.base = Math.atan2(d.z, d.x);
      this.half = Math.toRadians((double)Math.max(30, params.extra()) * 0.5);
      this.cx = this.x;
      this.cz = this.z;
      this.tail = Ribbon.create(16);
      if (this.tail != null) {
         this.tail.look(0.5F, 8.0F, 1.35F, 0.45F, 1.9F, 0).texture(MeshTex.FLAME_RIBBON, 0.25F);
      }

      SoundDesign.play(
         new Vec3(this.x, this.y + 1.0, this.z),
         SeiryuSounds.TAIL_SLAM,
         1.4F,
         1.0F,
         SoundDesign.layer(DFSounds.LAYER_WIND, 1.0F, 0.9F),
         SoundDesign.layer(SeiryuSounds.LAYER_FLAME, 0.8F, 1.0F)
      );
   }

   private float sweep(float t) {
      return Mth.clamp((t - (float)this.sweepStart + 1.0F) / (float)(this.sweepEnd - this.sweepStart + 1), 0.0F, 1.0F);
   }

   private double bearing(float s) {
      return this.base - this.half + 2.0 * this.half * (double)s;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = this.caster(level);
      float t = VfxClock.time();
      if (e != null) {
         HybridAnchors.get(e, 2, 1.0F, this.anchor3);
         if (this.tail != null && this.age <= this.sweepEnd + 2) {
            this.tail.push(this.anchor3.x, this.anchor3.y, this.anchor3.z, t);
         }

         if (this.age < this.sweepStart) {
            for (int i = 0; i < VfxSettings.count(3.0F); i++) {
               VfxParticles.emit(
                     VfxPresets.FIRE_TONGUE,
                     this.anchor3.x + this.spread(0.15),
                     this.anchor3.y + this.spread(0.15),
                     this.anchor3.z + this.spread(0.15),
                     this.spread(0.03),
                     0.05,
                     this.spread(0.03)
                  )
                  .size(1.2F);
            }

            VfxParticles.emit(VfxPresets.GLOW, this.anchor3.x, this.anchor3.y, this.anchor3.z, 0.0, 0.0, 0.0).size(1.6F).tint(16742938).alpha(0.7F);
         }
      }

      if (this.age >= this.sweepStart && this.age <= this.sweepEnd + 1) {
         if (this.age == this.sweepStart) {
            this.waveAge = 0;
            float ex = this.exposure();
            if (ex > 0.0F) {
               CameraDirector.shake(0.3F * ex, 0.1F, 1.8F, 3.0F);
               CameraDirector.fovPunch(0.04F * ex);
            }
         }

         float s = this.sweep((float)this.age);
         double a = this.bearing(s);
         double floor = this.onFloor(2.5) ? this.groundY : this.y;
         int dust = this.shade(VfxWorld.groundColour(level, this.x, floor - 0.5, this.z));

         for (int i = 0; i < VfxSettings.count(4.0F); i++) {
            double aa = a - this.random.nextDouble() * 0.5 * Math.signum(this.half);
            double r = (double)this.radius * (0.6 + this.random.nextDouble() * 0.5);
            double px = this.cx + Math.cos(aa) * r;
            double pz = this.cz + Math.sin(aa) * r;
            VfxParticles.emit(VfxPresets.DUST, px, floor + 0.25, pz, Math.cos(aa) * 0.2 - Math.sin(aa) * 0.12, 0.03, Math.sin(aa) * 0.2 + Math.cos(aa) * 0.12)
               .tint(dust)
               .size(0.5F);
         }

         for (int i = 0; i < VfxSettings.count(3.0F); i++) {
            double r = (double)this.radius * (0.5 + this.random.nextDouble() * 0.45);
            VfxParticles.emit(
                  VfxPresets.FIRE_TONGUE,
                  this.cx + Math.cos(a) * r,
                  this.y + 0.8 + this.spread(0.3),
                  this.cz + Math.sin(a) * r,
                  -Math.sin(a) * 0.2,
                  0.04,
                  Math.cos(a) * 0.2
               )
               .size(1.4F);
         }

         for (Entity hit : level.getEntities(
            this.caster(level),
            new AABB(
               this.cx - (double)this.radius,
               this.y - 1.0,
               this.cz - (double)this.radius,
               this.cx + (double)this.radius,
               this.y + 4.0,
               this.cz + (double)this.radius
            )
         )) {
            if (hit instanceof LivingEntity && !this.struck.contains(hit.getId())) {
               double bx = hit.getX() - this.cx;
               double bz = hit.getZ() - this.cz;
               if (!(bx * bx + bz * bz > (double)(this.radius * this.radius))) {
                  double off = Mth.wrapDegrees(Math.toDegrees(Math.atan2(bz, bx) - this.base));
                  double halfDeg = Math.toDegrees(this.half);
                  if (Math.abs(off) <= halfDeg && off + halfDeg <= 2.0 * halfDeg * (double)s + 0.001) {
                     this.struck.add(hit.getId());
                     Vec3 at = hit.getBoundingBox().getCenter();
                     Impact.at(level, at, this.seed ^ (long)hit.getId())
                        .caster(this.caster(level))
                        .flash(0.65F)
                        .glow(16742938)
                        .sparks(24, 0.6F)
                        .embers(10)
                        .trauma(0.3F, 0.12F, 1.6F)
                        .play();
                     VeilAuraCinematic.pulseOf(this.caster(level));
                     SoundDesign.layerOnly(at, DFSounds.LAYER_IMPACT, 0.9F, 1.1F);
                  }
               }
            }
         }
      }

      if (this.waveAge >= 0 && ++this.waveAge > 16) {
         this.waveAge = -1;
      }

      if (this.age > this.sweepEnd + 30) {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      float t = (float)this.age + f.partial;
      float s = this.sweep(t);
      float rx = f.rx(this.cx);
      float rz = f.rz(this.cz);
      double floor = this.onFloor(2.5) ? this.groundY : this.y;
      float fade = t < (float)this.sweepStart
         ? 0.0F
         : (t <= (float)(this.sweepEnd + 1) ? 1.0F : Math.max(0.0F, 1.0F - (t - (float)this.sweepEnd - 1.0F) / 6.0F));
      if (fade > 0.01F && s > 0.02F) {
         float head = (float)this.bearing(s);
         float arc = (float)(2.0 * this.half * (double)s);
         ArcStrip.draw(
            f,
            MeshTex.FLAME_RIBBON,
            rx,
            f.ry(this.y + 0.9),
            rz,
            1.0F,
            0.0F,
            0.0F,
            0.0F,
            0.0F,
            1.0F,
            this.radius * 0.82F,
            head,
            arc,
            1.1F,
            1.3F,
            fade,
            0,
            2.0F,
            16777215,
            -t * 0.2F,
            40
         );
         ArcStrip.draw(
            f,
            MeshTex.FLAME_RIBBON,
            rx,
            f.ry(this.y + 0.9),
            rz,
            1.0F,
            0.0F,
            0.0F,
            0.0F,
            0.0F,
            1.0F,
            this.radius * 0.82F,
            head,
            arc * 0.6F,
            0.16F,
            1.5F,
            fade,
            0,
            3.2F,
            16777215,
            -t * 0.3F,
            30
         );
         float w = this.waveAge >= 0 ? Mth.clamp(((float)this.waveAge + f.partial) / 16.0F, 0.0F, 1.0F) : 1.0F;
         ArcStrip.draw(
            f,
            MeshTex.FLAME_RIBBON,
            rx,
            f.ry(floor + 0.15),
            rz,
            1.0F,
            0.0F,
            0.0F,
            0.0F,
            0.0F,
            1.0F,
            this.radius * (0.8F + 0.55F * w),
            head,
            arc,
            0.6F,
            -1.0F,
            0.75F * fade * (1.0F - w * 0.6F),
            0,
            0.4F,
            this.shade(15262426),
            -t * 0.15F,
            36
         );
      }

      Entity e = this.caster();
      if (this.tail != null && e != null && t >= (float)(this.sweepStart - 1)) {
         HybridAnchors.get(e, 2, f.entityPartial, this.anchor3);
         this.tail.render(f, this.anchor3.x, this.anchor3.y, this.anchor3.z, Math.max(0.0F, 1.0F - Math.max(0.0F, t - (float)this.sweepEnd - 2.0F) / 6.0F));
      }

      BufferBuilder d = f.distortion();
      if (d != null && this.waveAge >= 0) {
         float p = Mth.clamp(((float)this.waveAge + f.partial) / 16.0F, 0.0F, 1.0F);
         VfxSprites.Sprite ring = VfxSprites.RING_NORMAL;
         float r = this.radius * (0.85F + 0.6F * p);

         for (int k = 0; k <= 6; k++) {
            double a = this.bearing((float)k / 6.0F * s);
            MeshWriter.distortQuad(
               d,
               rx + (float)Math.cos(a) * r,
               f.ry(floor + 0.05),
               rz + (float)Math.sin(a) * r,
               1.6F,
               1.1F * (1.0F - p),
               0,
               true,
               0.0F,
               0.0F,
               0.0F,
               0.0F,
               0.0F,
               0.0F,
               ring.u0(),
               ring.v0(),
               ring.u1(),
               ring.v1(),
               0.0F,
               0.0F
            );
         }
      }
   }

   @Override
   public void removed() {
      if (this.tail != null) {
         this.tail.release();
      }
   }
}
