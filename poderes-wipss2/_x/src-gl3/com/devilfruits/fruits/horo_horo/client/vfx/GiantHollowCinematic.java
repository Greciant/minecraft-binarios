package com.devilfruits.fruits.horo_horo.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.Impact;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.particle.Ribbon;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.fruits.horo_horo.HollowFlight;
import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class GiantHollowCinematic extends HoroCinematic {
   private static final float UNIT = 0.0625F;
   private static final int BURST_LEAD = 4;
   @Nullable
   private final Player user;
   private final int targetId;
   private final HollowFlight flight;
   private final HollowGhost ghost;
   @Nullable
   private final Ribbon wake;
   private int absorbAge = -1;
   private int swellTicks = 50;
   private int endAge = -1;

   public GiantHollowCinematic(@Nullable Entity user, Vec3 start, Vec3 look, double speed, int targetId, long seed) {
      super(VfxAnchor.of(user, start), seed);
      this.user = user instanceof Player p ? p : null;
      this.targetId = targetId;
      Vec3 flat = new Vec3(look.x, 0.0, look.z);
      this.flight = new HollowFlight(start, flat, speed, 0.1);
      this.ghost = new HollowGhost(HoroModel.GIANT, 0.0625F, 54.0F, start, 0.0F);
      this.ghost.face(flat);
      this.ghost.play("rise", false);
      this.ghost.alpha = 0.94F;
      this.wake = mistRibbon(24, 1.1F, 16.0F, 13219583);
      squareUp(user);
   }

   public int targetId() {
      return this.targetId;
   }

   public boolean absorbed() {
      return this.absorbAge >= 0;
   }

   public boolean endPending() {
      return this.endAge < 0;
   }

   private boolean local() {
      return this.user != null && this.user == Minecraft.getInstance().player;
   }

   public void onAbsorb(ClientLevel level, Vec3 at, int swell) {
      if (this.absorbAge < 0) {
         this.absorbAge = this.age;
         this.swellTicks = Math.max(5, swell);
         this.flight.pos = at;
         this.ghost.moveTo(at);
         this.ghost.play("envelop", false);
         flash(at, 1.6F, 16775163, 0.6F);
         this.puffs(level, HoroLook.PUFF_BIG, at, 8, 0.12, 1.4F, new int[]{16775163, 16758741, 13219583});
         ShockRing.at(at, new Vec3(0.0, 1.0, 0.0)).radius(3.5F, 1.2F).life(8).strength(0.35F).band(0.4F).rim(0.3F, 16758741).spawn();
      }
   }

   public void onEnd(ClientLevel level, Vec3 at, boolean harmless) {
      if (this.endAge < 0) {
         this.endAge = this.age;
         this.flight.pos = at;
         if (!this.ghost.clip().equals("burst")) {
            this.ghost.play("burst", false);
         }

         if (harmless) {
            this.puffs(level, HoroLook.PUFF_BIG, at, 12, 0.18, 1.3F, new int[]{16775163, 13219583});
            this.sparkles(level, at, 10, 1.2, 0.06, new int[]{16775163, 13219583});
            ShockRing.at(at, new Vec3(0.0, 1.0, 0.0)).radius(0.5F, 3.0F).life(8).strength(0.3F).band(0.4F).rim(0.25F, 16775163).spawn();
         } else {
            this.blast(level, at);
         }
      }
   }

   private void blast(ClientLevel level, Vec3 at) {
      float exposure = this.user != null
         ? CameraDirector.exposure(this.user, at, 10.0, 70.0)
         : CameraDirector.exposure(Minecraft.getInstance().player, at, 10.0, 70.0);
      if (exposure > 0.2F) {
         VfxClock.slowMo(16, 0.45F);
      }

      flash(at, 7.0F, 16775163, 1.0F);
      flash(at, 11.0F, 16758741, 0.8F);
      if (!VfxSettings.legacy()) {
         VfxParticles.emit(HoroLook.HALO, at.x, at.y, at.z, 0.0, 0.0, 0.0).size(16.0F).tint(16758741).alpha(0.55F * VfxSettings.flashCap()).life(4.0F);
         VfxParticles.emit(VfxPresets.BLAST_RING, at.x, at.y, at.z, 0.0, 0.0, 0.0).size(42.0F).alpha(0.35F).tint(16770288);
      }

      ShockRing.at(at, new Vec3(0.0, 1.0, 0.0)).radius(1.5F, 13.0F).life(16).strength(1.0F).band(0.32F).rim(0.4F, 16740264).spawn();
      Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
      ShockRing.at(at, cam.subtract(at)).radius(1.0F, 9.0F).life(12).strength(0.6F).band(0.4F).rim(0.3F, 16775163).spawn();
      Impact.at(level, at, this.random.nextLong())
         .caster(this.user)
         .glow(16769775)
         .wave(11.0F, 18, 16766698)
         .dust(48)
         .debris(26, 0.65F)
         .trauma(0.7F * Math.max(0.3F, exposure), 0.04F, 3.5F)
         .hitStop(2)
         .fov(0.06F)
         .lens(0.6F)
         .screenFlash(0.3F)
         .play();
      this.puffs(level, HoroLook.PUFF_BIG, at, 34, 1.4, 3.4F, new int[]{16758741, 16775163, 13219583, 16740264, 16775163});
      this.puffs(level, HoroLook.PUFF, at, 30, 1.8, 1.8F, new int[]{16775163, 16758741});
      int[] dome = new int[]{16775163, 16758741, 13219583, 16775163, 16740264};
      int count = VfxSettings.legacy() ? 10 : VfxSettings.count(30.0F);

      for (int i = 0; i < count; i++) {
         Vec3 d = new Vec3(this.jit(1.0), this.random.nextDouble() * 0.8, this.jit(1.0));
         d = d.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 1.0, 0.0) : d.normalize();
         double reach = 1.5 + this.random.nextDouble() * 4.5;
         this.emit(
            level, HoroLook.PUFF_BIG, at.add(d.scale(reach)), d.scale(0.06).add(0.0, 0.02, 0.0), dome[i % dome.length], 3.6F + this.random.nextFloat() * 1.8F
         );
      }

      this.sparkles(level, at, 46, 4.5, 0.32, new int[]{16758741, 16740264, 13219583, 16775163});
      if (!VfxSettings.legacy()) {
         for (int i = 0; i < VfxSettings.count(16.0F); i++) {
            Vec3 v = new Vec3(this.jit(1.0), 0.4 + this.random.nextDouble() * 0.6, this.jit(1.0)).normalize().scale(0.25 + this.random.nextDouble() * 0.3);
            VfxParticles.emit(HoroLook.GIGGLE, at.x, at.y, at.z, v.x, v.y, v.z).size(1.4F + this.random.nextFloat()).life(1.6F).unflipped();
         }
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      this.ghost.tick();
      int rise = 24;
      Entity target = level.getEntity(this.targetId);
      if (this.endAge >= 0) {
         this.ghost.alpha = Math.max(0.0F, this.ghost.alpha - 0.3F);
         if (this.age > this.endAge + 30) {
            this.finish();
         }
      } else if (this.absorbAge >= 0) {
         int held = this.age - this.absorbAge;
         if (this.ghost.clip().equals("envelop") && this.ghost.clipTime() >= 10.0F) {
            this.ghost.play("swell", false);
         }

         if (held == this.swellTicks - 4) {
            this.ghost.play("burst", false);
         }

         this.ghost.alpha = Mth.lerp(Math.min(1.0F, (float)held / 8.0F), 0.94F, 0.66F);
         float k = Mth.clamp((float)held / (float)this.swellTicks, 0.0F, 1.0F);
         float shake = 0.03F + 0.16F * k * k;
         this.ghost.pos = this.flight.pos.add(this.jit((double)shake), this.jit((double)shake), this.jit((double)shake));
         this.ghost.lean = 0.0F;
         float pulse = 0.5F + 0.5F * Mth.sin((float)held * (0.4F + k * 0.9F));
         halo(this.flight.pos, 3.2F + 1.2F * k, 16758741, (0.25F + 0.35F * k) * pulse);
         if (held % Math.max(1, 6 - (int)(k * 5.0F)) == 0) {
            this.sparkles(level, this.flight.pos, 2, 2.0, 0.08, new int[]{16758741, 16775163, 16740264});
         }

         if (this.local() || target != null && target == Minecraft.getInstance().player) {
            CameraDirector.shake(7, 0.12F + 0.25F * k, 0.2F, 0.6F + 1.2F * k, 3.0F);
         }
      } else if (this.age <= rise) {
         Vec3 hands = this.user != null ? this.user.position().add(0.0, 2.1, 0.0) : this.flight.pos.subtract(0.0, 2.4, 0.0);
         if (this.age % 2 == 0) {
            Vec3 p = hands.add(this.jit(0.4), this.jit(0.2), this.jit(0.4));
            Vec3 v = this.flight.pos.subtract(p).scale(0.08);
            this.emit(level, HoroLook.MIST, p, v, this.random.nextBoolean() ? 16758741 : 16775163, 1.4F);
            this.sparkles(level, p, 1, 0.3, 0.03, new int[]{16758741, 16775163});
         }

         halo(this.flight.pos, 2.5F * Math.min(1.0F, (float)this.age / 10.0F), 13219583, 0.25F);
         if (this.local()) {
            CameraDirector.dolly(1.1F, 0.06F);
         }

         if (this.age == rise - 2) {
            this.puffs(level, HoroLook.PUFF_BIG, this.flight.pos, 6, 0.1, 1.6F, new int[]{16775163, 13219583});
         }
      } else {
         if (this.ghost.clip().equals("rise")) {
            this.ghost.play("drift", true);
         }

         if (target != null && target.isAlive()) {
            this.flight.step(HollowFlight.aimPoint(target));
         } else {
            this.flight.pos = this.flight.pos.add(this.flight.heading.scale(this.flight.speed * 0.3));
         }

         this.ghost.moveTo(this.flight.pos);
         this.ghost.lean = 0.15F;
         Vec3 back = this.flight.pos.subtract(this.flight.heading.scale(1.6)).add(this.jit(0.8), this.jit(0.6) - 0.6, this.jit(0.8));
         this.emit(level, HoroLook.MIST, back, new Vec3(this.jit(0.01), 0.004, this.jit(0.01)), this.random.nextInt(3) == 0 ? 16758741 : 16775163, 3.0F);
         if (this.age % 3 == 0) {
            this.sparkles(level, back, 1, 0.8, 0.02, new int[]{16775163, 13219583});
         }

         if (this.wake != null) {
            Vec3 w = this.flight.pos.add(0.0, -0.6, 0.0);
            this.wake.push(w.x, w.y, w.z, VfxClock.time());
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      if (this.wake != null && this.absorbAge < 0 && this.endAge < 0) {
         Vec3 p = this.ghost.position(f.partial).add(0.0, -0.6, 0.0);
         this.wake.render(f, p.x, p.y, p.z, 0.7F);
      }
   }

   @Override
   public void removed() {
      super.removed();
      if (this.wake != null) {
         this.wake.release();
      }
   }

   @Override
   public void draw(PoseStack ps, MultiBufferSource buffers, Vec3 cam, float partial) {
      if (!this.isDone()) {
         this.ghost.draw(ps, buffers, cam, partial);
      }
   }

   @Override
   public double cullDistance() {
      return 160.0;
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
