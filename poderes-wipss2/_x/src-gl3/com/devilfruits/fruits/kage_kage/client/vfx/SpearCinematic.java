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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SpearCinematic extends KageCinematic {
   private static final KageModel SPEAR = new KageModel("shadow_spear").silhouetteRim();
   private final Player user;
   private final int targetId;
   private Vec3 feet;
   private final int run;
   private final int erupt;
   private final int pierce;
   private final int dissolve;
   private final BatSwarm.Bat[] runners;
   private final Vec3[] start;
   private final float[] weave;
   private final List<BatSwarm.Bat> leaving = new ArrayList<>();
   private ShadowPool pool;
   private final float yaw;
   private float heat = 1.0F;
   private float prevHeat = 1.0F;

   public SpearCinematic(Player user, int targetId, Vec3 feet, int run, int stun, int bats, long seed) {
      super(VfxAnchor.of(user, feet), seed);
      this.user = user;
      this.targetId = targetId;
      this.feet = feet;
      this.run = Math.max(2, run);
      this.erupt = this.run + 6;
      this.pierce = KageTiming.spearPierce(this.run);
      this.dissolve = this.pierce + Math.max(10, stun);
      this.yaw = this.random.nextFloat() * (float) (Math.PI * 2);
      this.runners = new BatSwarm.Bat[Math.max(0, bats)];
      this.start = new Vec3[this.runners.length];
      this.weave = new float[this.runners.length];
      Vec3 from = user.position();

      for (int i = 0; i < this.runners.length; i++) {
         this.start[i] = from.add(this.jit(0.6), 0.3 + this.random.nextDouble() * 0.4, this.jit(0.6));
         this.runners[i] = new BatSwarm.Bat(this.start[i], this.random.nextFloat() * 6.0F);
         this.runners[i].scale = 0.0F;
         this.runners[i].flapSpeed = 2.0F;
         this.weave[i] = this.random.nextFloat() * (float) (Math.PI * 2);
      }

      KageLook.sound((ClientLevel)user.level(), from, SoundEvents.BAT_TAKEOFF, 0.7F, 0.85F);
   }

   private Vec3 floor(Vec3 p) {
      return new Vec3(p.x, VfxWorld.ground(this.user.level(), p.x, p.y + 0.5, p.z, 4), p.z);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      int t = this.age;
      this.prevHeat = this.heat;
      Entity target = this.user.level().getEntity(this.targetId);
      if (t <= this.pierce && target != null && target.isAlive()) {
         this.feet = target.position();
      }

      Vec3 ground = this.floor(this.feet);

      for (int i = 0; i < this.runners.length; i++) {
         BatSwarm.Bat b = this.runners[i];
         b.tick();
         float u = Mth.clamp((float)(t - i) / (float)Math.max(1, this.run - i), 0.0F, 1.0F);
         if (t >= i && (!(u >= 1.0F) || !(b.scale <= 0.0F))) {
            float e = u * u * (3.0F - 2.0F * u);
            Vec3 line = this.start[i].lerp(ground, (double)e);
            Vec3 dir = ground.subtract(this.start[i]).multiply(1.0, 0.0, 1.0);
            Vec3 side = dir.lengthSqr() < 1.0E-4 ? Vec3.ZERO : new Vec3(-dir.z, 0.0, dir.x).normalize();
            double lift = u < 0.85F ? 0.35 + 0.2 * (double)Mth.sin(u * (float) Math.PI) : 0.35 * (double)(1.0F - u) / 0.15;
            Vec3 p = new Vec3(line.x, ground.y + lift, line.z)
               .add(side.scale(0.4 * (double)Mth.sin(u * (float) (Math.PI * 2) * 1.5F + this.weave[i]) * (double)(1.0F - u)));
            Vec3 before = b.pos;
            b.moveTo(p);
            b.scale = u >= 1.0F ? Math.max(0.0F, b.scale - 0.5F) : Math.min(1.0F, b.scale + 0.34F);
            if (u > 0.0F && u < 1.0F) {
               this.flakes.mote(before, new Vec3(this.jit(0.008), 0.003, this.jit(0.008)), 0.045F, 7 + this.random.nextInt(3));
            }
         }
      }

      if (t == this.run - 1) {
         this.pool = new ShadowPool(ground, 1.25F, 1.5F, this.random.nextLong());
         this.pools.add(this.pool);
         KageLook.sound(level, ground, SoundEvents.SOUL_SAND_PLACE, 0.9F, 0.6F);
         KageLook.sound(level, ground, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5F, 0.6F);
      }

      if (this.pool != null && t <= this.pierce) {
         this.pool.centre = ground;
      }

      if (t == this.erupt) {
         this.pool.pulse(0.6F);
         KageLook.sound(level, ground, SoundEvents.ENDERMAN_TELEPORT, 0.6F, 0.55F);
      }

      if (t == this.pierce) {
         Vec3 hit = target != null ? target.getBoundingBox().getCenter() : ground.add(0.0, 1.0, 0.0);
         this.heat = 2.2F;
         this.pool.pulse(1.0F);
         this.flash(level, hit, 1.6F, 0.85F);
         this.motes(level, hit, 8, 0.08);

         for (int ix = 0; ix < 10; ix++) {
            this.flakes
               .burst(
                  ground.add(this.jit(0.3), 0.2 + this.random.nextDouble() * 1.2, this.jit(0.3)),
                  new Vec3(this.jit(0.16), 0.08 + this.random.nextDouble() * 0.16, this.jit(0.16)),
                  0.05F + this.random.nextFloat() * 0.06F,
                  14 + this.random.nextInt(8)
               );
         }

         if (!VfxSettings.legacy()) {
            VfxParticles.emit(KageLook.RIPPLE, ground.x, ground.y + 0.05, ground.z, 0.0, 0.0, 0.0).size(0.7F).tint(12868576).alpha(0.8F);
         }

         KageLook.sound(level, hit, SoundEvents.WITHER_SHOOT, 0.45F, 1.5F);
         KageLook.sound(level, hit, SoundEvents.AMETHYST_BLOCK_BREAK, 1.0F, 0.55F);
         KageLook.sound(level, hit, SoundEvents.BAT_HURT, 0.5F, 0.5F);
         boolean onTarget = target != null && VfxWorld.local(target.getId());
         if (onTarget || VfxWorld.local(this.user.getId())) {
            VfxClock.hitStop(3);
            CameraDirector.shake(onTarget ? 0.45F : 0.18F, 0.08F, onTarget ? 3.0F : 1.5F, 20.0F);
         }
      }

      if (t > this.pierce) {
         this.heat = Math.max(1.0F, this.heat - 0.2F);
      }

      if (t == this.dissolve) {
         this.pool.sink();

         for (int ix = 0; ix < 3; ix++) {
            BatSwarm.Bat b = new BatSwarm.Bat(ground.add(this.jit(0.2), 0.8 + (double)ix * 0.5, this.jit(0.2)), this.random.nextFloat() * 6.0F);
            b.swoopTime = 0.0F;
            b.heading = new Vec3(this.jit(1.0), 0.8, this.jit(1.0)).normalize();
            this.leaving.add(b);
         }

         KageLook.sound(level, ground, SoundEvents.BAT_TAKEOFF, 0.7F, 0.9F);
         KageLook.sound(level, ground, SoundEvents.SOUL_SAND_BREAK, 0.8F, 0.7F);
      }

      for (BatSwarm.Bat b : this.leaving) {
         b.tick();
         b.moveTo(b.pos.add(b.heading.scale(0.32)).add(0.0, 0.04, 0.0));
         b.alpha = Math.max(0.0F, b.alpha - 0.07F);
         if (b.swoopTime > 8.0F) {
            b.swoopTime = -1.0F;
            b.flapSpeed = 1.8F;
         }
      }

      if (t > this.dissolve && t % 2 == 0 && t < this.dissolve + 12) {
         this.motes(level, ground.add(this.jit(0.2), 0.4 + this.random.nextDouble() * 1.8, this.jit(0.2)), 1, 0.02);
      }

      if (t > this.dissolve + 12 + 4 && (this.pool == null || this.pool.gone()) && this.flakes.isEmpty()) {
         this.finish();
      }
   }

   @Override
   protected void drawModels(boolean glow, PoseStack ps, MultiBufferSource buffers, Vec3 cam, float partial, float entityPartial) {
      Vec3 ground = this.floor(this.feet);
      int light = light(ground.add(0.0, 1.0, 0.0));

      for (BatSwarm.Bat b : this.runners) {
         BatSwarm.draw(b, glow, ps, buffers, cam, partial, light, this.rim);
      }

      for (BatSwarm.Bat b : this.leaving) {
         BatSwarm.draw(b, glow, ps, buffers, cam, partial, light, this.rim);
      }

      float t = (float)this.age + partial;
      if (!(t < (float)this.erupt) && SPEAR.ready()) {
         float alpha = 1.0F;
         SPEAR.pose();
         if (t < (float)(this.erupt + 12)) {
            SPEAR.play("erupt", t - (float)this.erupt, false);
         } else if (t < (float)this.dissolve) {
            SPEAR.play("hold", t - (float)this.erupt - 12.0F, true);
         } else {
            SPEAR.play("dissolve", t - (float)this.dissolve, false);
            alpha = Mth.clamp(1.0F - (t - (float)this.dissolve) / 12.0F, 0.0F, 1.0F);
         }

         float h = Mth.lerp(partial, this.prevHeat, this.heat) - 1.0F;
         if (!(alpha <= 0.01F)) {
            ps.pushPose();
            ps.translate(ground.x - cam.x, ground.y - cam.y + 0.02, ground.z - cam.z);
            ps.mulPose(Axis.YP.rotation(this.yaw));
            ps.scale(0.08125F, 0.08125F, 0.08125F);
            SPEAR.draw(glow, ps, buffers, light, alpha, 0.8F * h, 0.032F, this.rim);
            ps.popPose();
         }
      }
   }

   @Override
   public double x() {
      return this.feet.x;
   }

   @Override
   public double y() {
      return this.feet.y;
   }

   @Override
   public double z() {
      return this.feet.z;
   }
}
