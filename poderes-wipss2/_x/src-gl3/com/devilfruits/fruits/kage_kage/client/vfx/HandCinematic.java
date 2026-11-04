package com.devilfruits.fruits.kage_kage.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.fruits.kage_kage.KageTiming;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class HandCinematic extends KageCinematic {
   private static final KageModel HAND = new KageModel("shadow_hand").silhouetteRim();
   private static final float UNITS = 56.0F;
   private final Player user;
   private final Vec3 spot;
   private final Vec3 base;
   private final float scale;
   private final float yaw;
   private final int run;
   private final int rise;
   private final int draw;
   private final int slap;
   private final int impact;
   private final int sink;
   private final BatSwarm.Bat[] bats;
   private final Vec3[] from;
   private final Vec3[] bend;
   private ShadowPool pool;
   private float heat;
   private float prevHeat;

   public HandCinematic(Player user, Vec3 spot, Vec3 fall, int bats, int run, long seed) {
      super(VfxAnchor.of(user, spot), seed);
      this.user = user;
      this.spot = spot;
      float height = (float)Math.max(2.0, fall.length());
      Vec3 dir = fall.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, -1.0) : fall.normalize();
      this.scale = height / 56.0F;
      this.yaw = (float)Mth.atan2(-dir.x, -dir.z);
      Vec3 b = spot.subtract(dir.scale((double)(height * 0.66071427F)));
      this.base = new Vec3(b.x, VfxWorld.ground(user.level(), b.x, b.y + 1.5, b.z, 4), b.z);
      this.run = Math.max(2, run);
      this.rise = this.run + 3;
      this.draw = this.run + 27;
      this.slap = this.run + 36;
      this.impact = KageTiming.handImpact(this.run);
      this.sink = this.impact + 14;
      this.bats = new BatSwarm.Bat[Math.max(0, bats)];
      this.from = new Vec3[this.bats.length];
      this.bend = new Vec3[this.bats.length];
      Vec3 start = user.position().add(0.0, 1.5, 0.0);

      for (int i = 0; i < this.bats.length; i++) {
         this.from[i] = start.add(this.jit(0.6), this.jit(0.3), this.jit(0.6));
         this.bend[i] = start.lerp(this.base, 0.5).add(this.jit(3.0), 3.0 + this.random.nextDouble() * 2.5, this.jit(3.0));
         this.bats[i] = new BatSwarm.Bat(this.from[i], this.random.nextFloat() * 6.0F);
         this.bats[i].scale = 0.0F;
         this.bats[i].flapSpeed = 1.7F;
      }

      KageLook.sound((ClientLevel)user.level(), start, SoundEvents.BAT_TAKEOFF, 0.9F, 0.7F);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      int t = this.age;
      this.prevHeat = this.heat;
      this.heat = Math.max(0.0F, this.heat - 0.16F);

      for (int i = 0; i < this.bats.length; i++) {
         BatSwarm.Bat b = this.bats[i];
         b.tick();
         float u = Mth.clamp((float)(t - i) / (float)Math.max(1, this.run - i), 0.0F, 1.0F);
         if (t >= i) {
            Vec3 into = this.base.add(0.0, 0.1, 0.0);
            Vec3 before = b.pos;
            b.moveTo(bezier(this.from[i], this.bend[i], into.add(0.0, 2.5, 0.0), into, u * u));
            b.scale = u >= 1.0F ? Math.max(0.0F, b.scale - 0.5F) : Math.min(1.0F, b.scale + 0.25F);
            if (u > 0.7F && b.swoopTime < 0.0F) {
               b.swoopTime = 0.0F;
            }

            if (u > 0.0F && u < 1.0F) {
               this.flakes.mote(before, new Vec3(this.jit(0.008), 0.003, this.jit(0.008)), 0.045F, 8);
            }
         }
      }

      if (t == this.run - 1) {
         this.pool = new ShadowPool(this.base, 1.9F, 2.2F, this.random.nextLong());
         this.pools.add(this.pool);
         KageLook.sound(level, this.base, SoundEvents.SOUL_SAND_PLACE, 1.0F, 0.5F);
      }

      if (t == this.rise) {
         this.pool.pulse(0.7F);
         KageLook.sound(level, this.base, SoundEvents.ENDERMAN_TELEPORT, 0.7F, 0.4F);
         KageLook.sound(level, this.base, (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), 1.0F, 0.5F);
      }

      if (t == this.draw) {
         KageLook.sound(level, this.base.add(0.0, 4.0, 0.0), SoundEvents.ENDERMAN_TELEPORT, 0.5F, 0.7F);
      }

      if (t == this.slap) {
         KageLook.sound(level, this.spot, SoundEvents.BAT_TAKEOFF, 0.7F, 0.5F);
      }

      if (t == this.impact) {
         this.heat = 2.0F;
         ShadowPool splash = new ShadowPool(this.spot, 2.6F, 3.0F, this.random.nextLong());
         splash.pulse(1.0F);
         this.pools.add(splash);
         this.pool.pulse(0.6F);
         this.flash(level, this.spot.add(0.0, 0.4, 0.0), 1.6F, 0.6F);
         this.motes(level, this.spot.add(0.0, 0.3, 0.0), 10, 0.1);
         if (!VfxSettings.legacy()) {
            VfxParticles.emit(KageLook.RIPPLE, this.spot.x, this.spot.y + 0.05, this.spot.z, 0.0, 0.0, 0.0).size(1.0F).tint(12868576).alpha(0.9F);
            VfxParticles.emit(KageLook.RIPPLE, this.spot.x, this.spot.y + 0.04, this.spot.z, 0.0, 0.0, 0.0).size(0.6F).tint(8007103).alpha(0.8F).life(1.6F);
         }

         for (int ix = 0; ix < 16; ix++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            double r = this.random.nextDouble() * 2.2;
            this.flakes
               .burst(
                  this.spot.add(Math.cos(a) * r, 0.2, Math.sin(a) * r),
                  new Vec3(Math.cos(a) * 0.12, 0.12 + this.random.nextDouble() * 0.16, Math.sin(a) * 0.12),
                  0.06F + this.random.nextFloat() * 0.06F,
                  14 + this.random.nextInt(8)
               );
         }

         KageLook.sound(level, this.spot, SoundEvents.WITHER_BREAK_BLOCK, 0.8F, 0.6F);
         KageLook.sound(level, this.spot, SoundEvents.AMETHYST_BLOCK_BREAK, 1.0F, 0.45F);
         KageLook.sound(level, this.spot, SoundEvents.SOUL_SAND_BREAK, 1.0F, 0.6F);
         Player me = Minecraft.getInstance().player;
         if (me != null) {
            double d = Math.sqrt(me.distanceToSqr(this.spot));
            if (d < 14.0) {
               CameraDirector.shake((float)(0.55 * (1.0 - d / 14.0)), 0.06F, 3.5F, 16.0F);
            }

            if (d < 6.0 || VfxWorld.local(this.user.getId())) {
               VfxClock.hitStop(2);
            }
         }
      }

      if (t == this.impact + 10) {
         this.pools.get(this.pools.size() - 1).sink();
      }

      if (t == this.sink) {
         this.pool.sink();
         KageLook.sound(level, this.base, SoundEvents.SOUL_SAND_BREAK, 0.8F, 0.5F);
      }

      if (t > this.sink + 20 && this.pools.stream().allMatch(ShadowPool::gone) && this.flakes.isEmpty()) {
         this.finish();
      }
   }

   @Override
   protected void drawModels(boolean glow, PoseStack ps, MultiBufferSource buffers, Vec3 cam, float partial, float entityPartial) {
      int light = light(this.base.add(0.0, 2.0, 0.0));

      for (BatSwarm.Bat b : this.bats) {
         BatSwarm.draw(b, glow, ps, buffers, cam, partial, light, this.rim);
      }

      float t = (float)this.age + partial;
      if (!(t < (float)this.rise) && HAND.ready()) {
         float alpha = t < (float)this.sink ? 1.0F : Mth.clamp(1.0F - (t - (float)this.sink) / 16.0F, 0.0F, 1.0F);
         float h = Mth.lerp(partial, this.prevHeat, this.heat);
         if (!(alpha <= 0.01F)) {
            HAND.pose();
            if (t < (float)this.draw) {
               HAND.play("rise", t - (float)this.rise, false);
            } else if (t < (float)this.slap) {
               HAND.play("draw_back", t - (float)this.draw, false);
            } else if (t < (float)(this.impact + 6)) {
               HAND.play("slap", t - (float)this.slap, false);
            } else if (t < (float)this.sink) {
               HAND.play("press", t - (float)this.impact - 6.0F, true);
            } else {
               HAND.play("sink", t - (float)this.sink, false);
            }

            ps.pushPose();
            ps.translate(this.base.x - cam.x, this.base.y - cam.y + 0.02, this.base.z - cam.z);
            ps.mulPose(Axis.YP.rotation(this.yaw));
            ps.scale(this.scale, this.scale, this.scale);
            HAND.draw(glow, ps, buffers, light, alpha, h, 0.04F, this.rim);
            ps.popPose();
         }
      }
   }

   @Override
   public double x() {
      return this.spot.x;
   }

   @Override
   public double y() {
      return this.spot.y;
   }

   @Override
   public double z() {
      return this.spot.z;
   }
}
