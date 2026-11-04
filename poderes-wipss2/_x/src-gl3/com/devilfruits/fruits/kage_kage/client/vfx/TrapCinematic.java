package com.devilfruits.fruits.kage_kage.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.fruits.kage_kage.KageTiming;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class TrapCinematic extends KageCinematic {
   private static final KageModel CASKET = new KageModel("shadow_casket").silhouetteRim();
   private static final float SCALE = 0.07801419F;
   private static final int BATS = 5;
   private static final int PULSES = 3;
   private final Player user;
   private final Vec3 casket;
   private final float yaw;
   private final Vec3 front;
   private final int run;
   private final int hold;
   private final int rise;
   private final int open;
   private final int pull;
   private final int close;
   private final int shut;
   private final int reopen;
   private final int spit;
   private final int sink;
   private final BatSwarm.Bat[] bats = new BatSwarm.Bat[5];
   private final Vec3[] from = new Vec3[5];
   private final Vec3[] bend = new Vec3[5];
   private final float[] orbit = new float[5];
   private final List<BatSwarm.Bat> leaving = new ArrayList<>();
   @Nullable
   private Entity victim;
   private ShadowPool pool;
   private float heat;
   private float prevHeat;
   private float jolt;
   private float prevJolt;

   public TrapCinematic(Player user, Vec3 casket, int run, int hold, float yaw, int aimedId, long seed) {
      super(VfxAnchor.of(user, casket), seed);
      this.user = user;
      this.casket = casket;
      this.yaw = yaw;
      this.front = new Vec3((double)(-Mth.sin(yaw)), 0.0, (double)(-Mth.cos(yaw)));
      this.run = Math.max(2, run);
      this.hold = Math.max(10, hold);
      this.rise = this.run + 4;
      this.open = this.run + 24;
      this.pull = KageTiming.trapPull(this.run);
      this.close = this.pull + 6;
      this.shut = KageTiming.trapShut(this.run);
      this.reopen = this.shut + this.hold;
      this.spit = KageTiming.trapSpit(this.run, this.hold);
      this.sink = this.spit + 6;
      this.victim = aimedId >= 0 ? user.level().getEntity(aimedId) : null;
      Vec3 start = user.position().add(0.0, 1.4, 0.0);

      for (int i = 0; i < 5; i++) {
         this.from[i] = start.add(this.jit(0.5), this.jit(0.3), this.jit(0.5));
         this.bend[i] = start.lerp(casket, 0.5).add(this.jit(3.0), 2.5 + this.random.nextDouble() * 2.0, this.jit(3.0));
         this.orbit[i] = (float) (Math.PI * 2) * (float)i / 5.0F + this.random.nextFloat();
         this.bats[i] = new BatSwarm.Bat(this.from[i], this.random.nextFloat() * 6.0F);
         this.bats[i].scale = 0.0F;
         this.bats[i].flapSpeed = 1.6F;
      }

      KageLook.sound((ClientLevel)user.level(), start, SoundEvents.BAT_TAKEOFF, 0.8F, 0.75F);
   }

   @Nullable
   public static TrapCinematic at(Vec3 at) {
      return VfxDirector.find(TrapCinematic.class, c -> c.casket.distanceToSqr(at) < 0.25);
   }

   public void swallow(Entity e) {
      this.victim = e;
   }

   private Vec3 circling(int i) {
      float a = this.orbit[i] + (float)this.age * 0.12F;
      return this.casket.add((double)Mth.cos(a) * 1.75, 3.5 + 0.4 * (double)Mth.sin((float)this.age * 0.2F + (float)i * 1.7F), (double)Mth.sin(a) * 1.75);
   }

   private Vec3 mouth() {
      return this.casket.add(this.front.scale(0.385)).add(0.0, 1.54, 0.0);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      int t = this.age;
      this.prevHeat = this.heat;
      this.heat = Math.max(0.0F, this.heat - 0.18F);
      this.prevJolt = this.jolt;
      this.jolt *= 0.6F;

      for (int i = 0; i < 5; i++) {
         BatSwarm.Bat b = this.bats[i];
         b.tick();
         if (t <= this.run) {
            float u = Mth.clamp((float)t / (float)this.run, 0.0F, 1.0F);
            b.moveTo(bezier(this.from[i], this.bend[i], this.circling(i).add(0.0, 1.5, 0.0), this.circling(i), u * u * (3.0F - 2.0F * u)));
            b.scale = Math.min(1.0F, b.scale + 0.25F);
         } else if (t < this.sink) {
            b.moveTo(b.pos.lerp(this.circling(i), 0.35));
         } else {
            Vec3 away = b.pos.subtract(this.casket).multiply(1.0, 0.0, 1.0);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
            b.moveTo(b.pos.add(away.scale(0.45)).add(0.0, 0.18, 0.0));
            b.alpha = Math.max(0.0F, b.alpha - 0.06F);
         }
      }

      if (t == this.run - 1) {
         this.pool = new ShadowPool(this.casket, 1.3F, 1.5F, this.random.nextLong());
         this.pools.add(this.pool);
         KageLook.sound(level, this.casket, SoundEvents.SOUL_SAND_PLACE, 1.0F, 0.55F);
      }

      if (t == this.rise) {
         this.pool.pulse(0.6F);
         KageLook.sound(level, this.casket, SoundEvents.ENDERMAN_TELEPORT, 0.6F, 0.45F);
      }

      if (t == this.rise + 12) {
         for (int ix = 0; ix < 8; ix++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            this.flakes.burst(this.casket.add(Math.cos(a) * 0.8, 0.2, Math.sin(a) * 0.8), new Vec3(Math.cos(a) * 0.1, 0.12, Math.sin(a) * 0.1), 0.07F, 14);
         }
      }

      if (t == this.open || t == this.reopen) {
         this.heat = 1.2F;
         KageLook.sound(level, this.casket, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 0.5F);
         KageLook.sound(level, this.casket, (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), 1.0F, 0.6F);
      }

      if (t == this.pull) {
         KageLook.sound(level, this.casket, SoundEvents.BAT_TAKEOFF, 0.8F, 0.6F);
      }

      if (t == this.shut) {
         this.heat = 1.6F;
         this.jolt = 1.0F;
         this.pool.pulse(1.0F);
         if (!VfxSettings.legacy()) {
            VfxParticles.emit(KageLook.RIPPLE, this.casket.x, this.casket.y + 0.05, this.casket.z, 0.0, 0.0, 0.0).size(0.5F).tint(12868576).alpha(0.9F);
         }

         for (int ix = 0; ix < 10; ix++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            this.flakes
               .burst(
                  this.mouth(),
                  new Vec3(Math.cos(a) * 0.14, 0.05 + this.random.nextDouble() * 0.12, Math.sin(a) * 0.14),
                  0.06F + this.random.nextFloat() * 0.05F,
                  14 + this.random.nextInt(8)
               );
         }

         KageLook.sound(level, this.casket, SoundEvents.WITHER_BREAK_BLOCK, 0.5F, 1.2F);
         KageLook.sound(level, this.casket, SoundEvents.AMETHYST_BLOCK_BREAK, 1.0F, 0.5F);
         Player me = Minecraft.getInstance().player;
         if (me != null && me != this.victim && me.distanceToSqr(this.casket) < 100.0) {
            CameraDirector.shake(0.25F, 0.08F, 2.0F, 18.0F);
         }
      }

      int step = Math.max(1, this.hold / 3);
      if (t >= this.shut && t < this.reopen && (t - this.shut) % step == 0 && (t - this.shut) / step < 3) {
         this.jolt = 1.0F;
         this.heat = Math.max(this.heat, 1.1F);
         this.flash(level, this.mouth(), 0.9F, 0.55F);
         this.motes(level, this.mouth(), 4, 0.05);
         KageLook.sound(level, this.casket, SoundEvents.AMETHYST_BLOCK_HIT, 0.9F, 0.5F);
         KageLook.sound(level, this.casket, SoundEvents.SOUL_SAND_BREAK, 0.8F, 0.6F);
      }

      if (t == this.spit) {
         this.heat = 1.8F;
         Vec3 m = this.mouth();

         for (int ix = 0; ix < 14; ix++) {
            this.flakes
               .burst(
                  m,
                  this.front.scale(0.15 + this.random.nextDouble() * 0.12).add(this.jit(0.08), 0.06 + this.random.nextDouble() * 0.1, this.jit(0.08)),
                  0.06F + this.random.nextFloat() * 0.05F,
                  14 + this.random.nextInt(8)
               );
         }

         this.motes(level, m, 8, 0.08);
         KageLook.sound(level, this.casket, SoundEvents.BAT_TAKEOFF, 0.9F, 0.7F);
         KageLook.sound(level, this.casket, SoundEvents.ENDERMAN_TELEPORT, 0.6F, 0.7F);
      }

      if (t == this.sink) {
         this.pool.sink();

         for (int ix = 0; ix < 5; ix++) {
            BatSwarm.Bat b = new BatSwarm.Bat(this.mouth().add(this.jit(0.4), this.jit(0.4), this.jit(0.4)), this.random.nextFloat() * 6.0F);
            b.swoopTime = 0.0F;
            b.heading = new Vec3(this.jit(1.0), 0.6 + this.random.nextDouble() * 0.5, this.jit(1.0)).normalize();
            this.leaving.add(b);
         }

         KageLook.sound(level, this.casket, SoundEvents.SOUL_SAND_BREAK, 0.9F, 0.6F);
      }

      for (BatSwarm.Bat b : this.leaving) {
         b.tick();
         b.moveTo(b.pos.add(b.heading.scale(0.3)).add(0.0, 0.03, 0.0));
         b.alpha = Math.max(0.0F, b.alpha - 0.05F);
         if (b.swoopTime > 8.0F) {
            b.swoopTime = -1.0F;
            b.flapSpeed = 1.8F;
         }
      }

      if (t > this.sink + 22 && (this.pool == null || this.pool.gone()) && this.flakes.isEmpty()) {
         this.finish();
      }
   }

   @Override
   protected void drawModels(boolean glow, PoseStack ps, MultiBufferSource buffers, Vec3 cam, float partial, float entityPartial) {
      int light = light(this.casket.add(0.0, 1.5, 0.0));

      for (BatSwarm.Bat b : this.bats) {
         BatSwarm.draw(b, glow, ps, buffers, cam, partial, light, this.rim);
      }

      for (BatSwarm.Bat b : this.leaving) {
         BatSwarm.draw(b, glow, ps, buffers, cam, partial, light, this.rim);
      }

      float t = (float)this.age + partial;
      if (this.victim != null && t >= (float)(this.pull - 2) && t < (float)(this.close + 1)) {
         float reach = t < (float)this.pull
            ? Mth.clamp((t - (float)this.pull + 2.0F) / 2.0F, 0.0F, 1.0F)
            : Mth.clamp(1.0F - (t - (float)this.pull) / ((float)(this.close - this.pull) + 1.0F), 0.15F, 1.0F);
         Vec3 body = this.victim.getPosition(entityPartial).add(0.0, (double)this.victim.getBbHeight() * 0.5, 0.0);

         for (int k = 0; k < 3; k++) {
            Vec3 root = this.mouth().add(0.0, (double)(k - 1) * 0.5, 0.0);
            Vec3 bend = root.lerp(body, 0.5).add((double)(k - 1) * 0.6, 0.6, 0.0);
            Tendrils.draw(
               glow, ps, buffers, cam, root, bend, body.add(0.0, (double)(k - 1) * 0.35, 0.0), reach, t + (float)(k * 3), 0.2F, light, (long)k * 977L
            );
         }
      }

      if (!(t < (float)this.rise) && CASKET.ready()) {
         float alpha = t < (float)this.sink ? 1.0F : Mth.clamp(1.0F - (t - (float)this.sink) / 16.0F, 0.0F, 1.0F);
         float h = Mth.lerp(partial, this.prevHeat, this.heat);
         if (!(alpha <= 0.01F)) {
            CASKET.pose();
            if (t < (float)this.open) {
               CASKET.play("rise", t - (float)this.rise, false);
            } else if (t < (float)this.close) {
               CASKET.play("open", t - (float)this.open, false);
            } else if (t < (float)this.reopen) {
               CASKET.play("slam", t - (float)this.close, false);
            } else if (t < (float)this.sink) {
               CASKET.play("open", t - (float)this.reopen, false);
            } else {
               CASKET.play("open", 10.0F, false);
               CASKET.play("sink", t - (float)this.sink, false);
            }

            float j = Mth.lerp(partial, this.prevJolt, this.jolt);
            ps.pushPose();
            ps.translate(this.casket.x - cam.x, this.casket.y - cam.y + 0.01, this.casket.z - cam.z);
            ps.mulPose(Axis.YP.rotation(this.yaw));
            ps.mulPose(Axis.ZP.rotationDegrees(4.0F * j * Mth.sin(t * 2.3F)));
            ps.scale(0.07801419F * (1.0F + 0.04F * j), 0.07801419F * (1.0F - 0.05F * j), 0.07801419F * (1.0F + 0.04F * j));
            CASKET.draw(glow, ps, buffers, light, alpha, h, 0.04F, this.rim);
            ps.popPose();
         }
      }
   }

   @Override
   public double x() {
      return this.casket.x;
   }

   @Override
   public double y() {
      return this.casket.y;
   }

   @Override
   public double z() {
      return this.casket.z;
   }
}
