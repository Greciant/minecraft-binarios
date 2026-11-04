package com.devilfruits.fruits.horo_horo.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.particle.Ribbon;
import com.devilfruits.fruits.horo_horo.HollowFlight;
import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class NegativeHollowCinematic extends HoroCinematic {
   private static final float UNIT = 0.029411765F;
   private static final int PHASE_TICKS = 8;
   private static final int DISSOLVE_TICKS = 12;
   @Nullable
   private final Player user;
   private final int targetId;
   private final HollowFlight flight;
   private final HollowGhost ghost;
   @Nullable
   private final Ribbon trail;
   private int hitAge = -1;
   private Vec3 drift = Vec3.ZERO;
   private boolean lost;
   private final Vec3 handOffset;

   public NegativeHollowCinematic(@Nullable Entity user, Vec3 hand, Vec3 dir, double speed, int targetId, long seed) {
      super(VfxAnchor.of(user, hand), seed);
      this.user = user instanceof Player p ? p : null;
      this.targetId = targetId;
      this.flight = new HollowFlight(hand, dir, speed, 0.22);
      this.ghost = new HollowGhost(HoroModel.LONG, 0.029411765F, 10.0F, hand, 0.0F);
      this.ghost.face(dir);
      this.ghost.play("emerge", false);
      this.trail = mistRibbon(18, 0.2F, 10.0F, 16775163);
      this.handOffset = user != null ? hand.subtract(user.position()) : Vec3.ZERO;
      squareUp(user);
   }

   public int targetId() {
      return this.targetId;
   }

   public boolean hit() {
      return this.hitAge >= 0;
   }

   private Vec3 hand() {
      return this.user != null && !this.user.isRemoved() ? this.user.position().add(this.handOffset) : this.flight.pos;
   }

   private boolean local() {
      return this.user != null && this.user == Minecraft.getInstance().player;
   }

   public void onHit(ClientLevel level, Vec3 at, Vec3 dir, @Nullable Entity target) {
      if (this.hitAge < 0) {
         this.hitAge = this.age;
         this.flight.pos = at;
         this.drift = (dir.lengthSqr() < 1.0E-6 ? this.flight.heading : dir.normalize()).scale(this.flight.speed * 0.35);
         this.ghost.play("phase", false);
         Vec3 c = target != null ? target.getBoundingBox().getCenter() : at;
         ShockRing.at(c, this.drift).radius(0.25F, 1.9F).life(9).strength(0.5F).band(0.4F).rim(0.35F, 10132184).spawn();
         ShockRing.at(c.add(0.0, 0.3, 0.0), new Vec3(0.0, 1.0, 0.0)).radius(0.2F, 1.3F).life(7).strength(0.3F).band(0.5F).rim(0.25F, 13219583).spawn();
         flash(c, 0.9F, 16775163, 0.7F);
         this.puffs(level, HoroLook.PUFF, c, 8, 0.12, 0.9F, new int[]{10132184, 13219583, 16775163});
         this.sparkles(level, c, 9, 0.5, 0.06, new int[]{13219583, 16775163, 10132184});
         Minecraft mc = Minecraft.getInstance();
         if (target != null && target == mc.player) {
            CameraDirector.shake(0.55F, 0.07F, 2.2F, 3.2F);
         }

         if (this.local()) {
            VfxClock.hitStop(2);
            CameraDirector.shake(0.2F, 0.1F, 0.8F, 2.0F);
         }
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      this.ghost.tick();
      int t = this.age;
      int emerge = 6;
      if (this.hitAge >= 0 || this.lost) {
         int since = t - this.hitAge;
         this.flight.pos = this.flight.pos.add(this.drift);
         this.ghost.moveTo(this.flight.pos);
         this.drift = this.drift.scale(0.86);
         if (!this.lost && since == 8) {
            this.ghost.play("dissolve", false);
         }

         int dissolveAt = this.lost ? 0 : 8;
         if (since >= dissolveAt + 6) {
            this.ghost.alpha = Math.max(0.0F, 0.92F * (1.0F - (float)(since - dissolveAt - 6) / 6.0F));
         }

         if (since >= dissolveAt && since < dissolveAt + 10 && since % 2 == 0) {
            Vec3 p = this.flight.pos.add(0.0, 0.2, 0.0);
            this.puffs(level, HoroLook.MIST, p, 2, 0.04, 1.1F, new int[]{16775163, 13219583});
            this.sparkles(level, p, 1, 0.35, 0.03, new int[]{16775163});
         }

         if (since > dissolveAt + 12 + 12) {
            this.finish();
         }
      } else if (t <= emerge) {
         Vec3 h = this.hand();
         this.ghost.moveTo(h);
         this.ghost.face(this.flight.heading);
         this.ghost.lean = 0.25F;
         this.flight.pos = h;
         if (t == 1) {
            this.puffs(level, HoroLook.PUFF, h, 3, 0.05, 0.6F, new int[]{16775163, 16758741});
         }

         if (t == emerge) {
            flash(h, 0.6F, 16758741, 0.6F);
            this.puffs(level, HoroLook.PUFF, h, 4, 0.1, 0.7F, new int[]{16775163, 16758741, 13219583});
            if (this.local()) {
               CameraDirector.fovPunch(0.035F);
            }
         }
      } else {
         Entity target = level.getEntity(this.targetId);
         if (target != null && target.isAlive() && t <= emerge + 80) {
            this.flight.step(HollowFlight.aimPoint(target));
            this.ghost.moveTo(this.flight.pos);
            if (this.ghost.clip().equals("emerge") && this.ghost.clipTime() >= 14.0F) {
               this.ghost.play("fly", true);
            }

            if (this.local()) {
               CameraDirector.dolly(0.965F, 0.12F);
            }
         } else {
            this.lost = true;
            this.hitAge = t;
            this.drift = this.flight.heading.scale(0.05);
            this.ghost.play("dissolve", false);
         }
      }

      if (t <= emerge + 3) {
         Vec3 hx = this.hand();
         float k = t <= emerge ? 1.0F : 1.0F - (float)(t - emerge) / 3.0F;
         halo(hx, 0.9F + 0.15F * (float)Math.sin((double)((float)t * 1.3F)), 16758741, 0.65F * k);
         halo(hx, 0.45F, 16775163, 0.8F * k);
         if (t % 2 == 0) {
            this.sparkles(level, hx, 1, 0.25, 0.02, new int[]{16758741, 16775163});
         }
      }

      if (this.ghost.alpha > 0.2F && t > 2) {
         Vec3 tail = this.tail(this.ghost.pos);
         if (this.trail != null) {
            this.trail.push(tail.x, tail.y, tail.z, VfxClock.time());
         }

         if (t > emerge) {
            this.emit(
               level,
               HoroLook.MIST,
               tail.add(this.jit(0.1), this.jit(0.1), this.jit(0.1)),
               new Vec3(this.jit(0.01), 0.008, this.jit(0.01)),
               this.random.nextBoolean() ? 16775163 : 16758741,
               0.8F + this.random.nextFloat() * 0.4F
            );
            if (t % 4 == 0) {
               this.sparkles(level, tail, 1, 0.2, 0.01, new int[]{16775163, 16758741});
            }
         }
      }
   }

   private Vec3 tail(Vec3 p) {
      return p.subtract(this.ghost.facing.scale(0.35)).add(0.0, -0.45, 0.0);
   }

   @Override
   public void render(VfxFrame f) {
      if (this.trail != null) {
         Vec3 tail = this.tail(this.ghost.position(f.partial));
         this.trail.render(f, tail.x, tail.y, tail.z, Math.min(1.0F, this.ghost.alpha));
      }
   }

   @Override
   public void removed() {
      super.removed();
      if (this.trail != null) {
         this.trail.release();
      }
   }

   @Override
   public void draw(PoseStack ps, MultiBufferSource buffers, Vec3 cam, float partial) {
      if (!this.isDone()) {
         this.ghost.draw(ps, buffers, cam, partial);
      }
   }

   @Override
   public double x() {
      return this.flight.pos.x;
   }

   @Override
   public double y() {
      return this.flight.pos.y;
   }

   @Override
   public double z() {
      return this.flight.pos.z;
   }
}
