package com.devilfruits.fruits.kage_kage.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class CollectCinematic extends KageCinematic {
   private static final KageModel SCISSORS = new KageModel("moria_scissors");
   private static final int PEEL = 2;
   private static final int STAND = 8;
   private static final int WRITHE = 16;
   private static final int ABSORB = 28;
   private final Player user;
   private final int targetId;
   @Nullable
   private Entity target;
   private Vec3 feet;
   private float yaw;
   private int snipAt = -1;
   private int cancelAt = -1;
   private final ShadowPool pool;
   private Vec3 from = Vec3.ZERO;
   private Vec3 bend = Vec3.ZERO;

   public CollectCinematic(Player user, int targetId, Vec3 feet, long seed) {
      super(VfxAnchor.of(user, feet), seed);
      this.user = user;
      this.targetId = targetId;
      this.feet = feet;
      this.target = user.level().getEntity(targetId);
      this.aim();
      this.pool = new ShadowPool(this.floor(), 1.0F, 1.2F, seed);
      this.pools.add(this.pool);
      KageLook.sound((ClientLevel)user.level(), feet, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6F, 0.6F);
   }

   @Nullable
   public static CollectCinematic of(Player user, int targetId) {
      return VfxDirector.find(CollectCinematic.class, c -> c.user == user && c.targetId == targetId && c.snipAt < 0 && c.cancelAt < 0);
   }

   private void aim() {
      Vec3 d = this.feet.subtract(this.user.position()).multiply(1.0, 0.0, 1.0);
      d = d.lengthSqr() < 1.0E-4 ? this.user.getLookAngle().multiply(1.0, 0.0, 1.0) : d.normalize();
      this.yaw = (float)Mth.atan2(-d.x, -d.z);
   }

   private Vec3 floor() {
      return new Vec3(this.feet.x, VfxWorld.ground(this.user.level(), this.feet.x, this.feet.y + 0.5, this.feet.z, 3), this.feet.z);
   }

   public void snip() {
      this.snipAt = this.age;
      if (this.target != null) {
         this.from = this.floor();
         Vec3 chest = this.user.position().add(0.0, (double)this.user.getBbHeight() * 0.62, 0.0);
         this.bend = this.from.lerp(chest, 0.5).add(0.0, 1.6, 0.0);
      }

      ClientLevel level = (ClientLevel)this.user.level();
      KageLook.sound(level, this.feet, SoundEvents.BAT_TAKEOFF, 0.6F, 0.5F);
      this.pool.pulse(0.8F);
      this.flash(level, this.floor().add(0.0, 0.2, 0.0), 1.0F, 0.6F);
      if (VfxWorld.local(this.user.getId())) {
         VfxClock.hitStop(2);
      }
   }

   public void cancel() {
      this.cancelAt = this.age;
      this.pool.sink();
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.snipAt < 0 && this.cancelAt < 0) {
         Entity t = this.user.level().getEntity(this.targetId);
         if (t != null) {
            this.target = t;
            this.feet = t.position();
            this.aim();
            this.pool.centre = this.floor();
         }

         if (this.age > 300) {
            this.cancel();
         }
      } else if (this.cancelAt >= 0) {
         if (this.age - this.cancelAt > 8 && this.pool.gone()) {
            this.finish();
         }
      } else {
         int tx = this.age - this.snipAt;
         if (tx == 8) {
            this.pool.sink();
         }

         if (tx > 8 && tx < 16 && this.random.nextFloat() < 0.5F) {
            this.motes(level, this.from.add(this.jit(0.3), 0.3 + this.random.nextDouble() * 1.2, this.jit(0.3)), 1, 0.02);
         }

         if (tx == 28) {
            Vec3 chest = this.user.position().add(0.0, (double)this.user.getBbHeight() * 0.62, 0.0);
            this.flash(level, chest, 0.9F, 0.7F);
            this.motes(level, chest, 8, 0.06);
            KageLook.sound(level, chest, (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), 1.0F, 0.8F);
         }

         if (tx > 32 && this.pool.gone() && this.flakes.isEmpty()) {
            this.finish();
         }
      }
   }

   @Override
   protected void drawModels(boolean glow, PoseStack ps, MultiBufferSource buffers, Vec3 cam, float partial, float entityPartial) {
      float t = (float)this.age + partial;
      Vec3 ground = this.floor();
      int light = light(ground.add(0.0, 1.0, 0.0));
      float since = this.snipAt >= 0 ? t - (float)this.snipAt : -1.0F;
      float alpha = this.cancelAt >= 0
         ? Mth.clamp(1.0F - (t - (float)this.cancelAt) / 6.0F, 0.0F, 1.0F)
         : (since > 10.0F ? Mth.clamp(1.0F - (since - 10.0F) / 8.0F, 0.0F, 1.0F) : Mth.clamp(t / 3.0F, 0.0F, 1.0F));
      if (alpha > 0.01F && !glow && SCISSORS.ready()) {
         SCISSORS.pose();
         if (since >= 0.0F) {
            SCISSORS.play("snip", since, false);
         } else if (t < 8.0F) {
            SCISSORS.play("open", t, false);
         } else {
            SCISSORS.play("open_idle", t - 8.0F, true);
         }

         ps.pushPose();
         Vec3 pivot = ground.add(new Vec3((double)Mth.sin(this.yaw), 0.0, (double)Mth.cos(this.yaw)).scale(0.9));
         ps.translate(pivot.x - cam.x, pivot.y - cam.y + 0.06, pivot.z - cam.z);
         ps.mulPose(Axis.YP.rotation(this.yaw));
         ps.mulPose(Axis.XP.rotationDegrees(-90.0F));
         ps.scale(0.0625F, 0.0625F, 0.0625F);
         SCISSORS.draw(false, ps, buffers, light, alpha, 0.0F, 0.0F, this.rim);
         ps.popPose();
      }

      if (!(since < 2.0F) && this.target != null && !(since > 28.0F)) {
         float stand = Mth.clamp((since - 2.0F) / 6.0F, 0.0F, 1.0F);
         stand = 1.0F - (1.0F - stand) * (1.0F - stand);
         float fly = Mth.clamp((since - 16.0F) / 12.0F, 0.0F, 1.0F);
         Vec3 chest = this.user.getPosition(entityPartial).add(0.0, (double)this.user.getBbHeight() * 0.62, 0.0);
         Vec3 at = fly <= 0.0F ? this.from : bezier(this.from, this.bend, chest.add(0.0, 0.6, 0.0), chest, fly * fly);
         float shrink = 1.0F - 0.85F * fly;
         float writhe = since > 8.0F ? Mth.sin(since * 0.9F) * 0.18F * (1.0F - fly) : 0.0F;
         ps.pushPose();
         ps.translate(at.x - cam.x, at.y - cam.y + 0.02, at.z - cam.z);
         ps.mulPose(Axis.YP.rotation(this.yaw + (float) Math.PI));
         ps.mulPose(Axis.XP.rotationDegrees(-90.0F * (1.0F - stand)));
         ps.mulPose(Axis.ZP.rotation(writhe));
         ps.scale(shrink, shrink * (1.0F + 0.08F * Mth.sin(since * 1.3F)), shrink);
         if (fly > 0.0F) {
            ps.translate(0.0F, -this.target.getBbHeight() * 0.5F, 0.0F);
         }

         if (!glow) {
            ps.translate(0.0F, 0.0F, -0.02F);
            Silhouette.draw(this.target, ps, buffers, entityPartial, true);
            ps.translate(0.0F, 0.0F, 0.02F);
            Silhouette.draw(this.target, ps, buffers, entityPartial, false);
            ps.translate(0.0F, 0.0F, -0.04F);
            Silhouette.draw(this.target, ps, buffers, entityPartial, false);
         }

         ps.popPose();
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
