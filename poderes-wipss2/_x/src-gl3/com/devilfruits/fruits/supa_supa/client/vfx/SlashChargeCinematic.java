package com.devilfruits.fruits.supa_supa.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.core.config.CoreClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class SlashChargeCinematic extends Cinematic {
   private final Player user;
   private final float duration;
   private final RandomSource random;
   private boolean stopped;
   private boolean released;

   public SlashChargeCinematic(Player user, float duration, long seed) {
      super(VfxAnchor.of(user, user.position()), VfxParams.NONE);
      this.user = user;
      this.duration = Math.max(1.0F, duration);
      this.random = RandomSource.create(seed);
   }

   public Player user() {
      return this.user;
   }

   public void stop(boolean released) {
      this.stopped = true;
      this.released = released;
      if (released && VfxWorld.local(this.user.getId())) {
         CameraDirector.snapDolly();
      }
   }

   private float progress() {
      return Mth.clamp((float)this.age / this.duration, 0.0F, 1.0F);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (!this.stopped && !this.user.isRemoved()) {
         float k = this.progress();
         if (VfxWorld.local(this.user.getId())) {
            CameraDirector.dolly(1.0F - 0.07F * k, 0.15F);
         }

         double dist = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceTo(this.user.position());
         if (dist < 40.0) {
            float near = (float)Mth.clamp(1.0 - dist / 40.0, 0.0, 1.0);
            VfxPost.grade(922664, 0.22F * k * near);
            VfxPost.vignette(0.25F * k * near);
         }

         if (VfxSettings.tier() != CoreClientConfig.CinematicQuality.LOW) {
            Vec3 look = this.user.getLookAngle();
            Vec3 side = new Vec3(-look.z, 0.0, look.x);
            side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();

            for (int arm = -1; arm <= 1; arm += 2) {
               Vec3 shoulder = this.user.position().add(0.0, 1.4, 0.0).add(side.scale(0.33 * (double)arm));
               Vec3 tip = shoulder.add(0.0, 1.55, 0.0).add(side.scale(0.12 * (double)arm));

               for (int i = 0; i < VfxSettings.count(2.0F + 3.0F * k); i++) {
                  double t = this.random.nextDouble();
                  Vec3 p = shoulder.add(tip.subtract(shoulder).scale(t));
                  VfxParticles.emit(SupaLook.FILING, p.x, p.y, p.z, 0.0, 0.05 + 0.08 * (double)k, 0.0).alpha(0.7F).size(0.6F);
               }

               if (this.age % 3 == 0) {
                  VfxParticles.emit(SupaLook.GLINT, tip.x, tip.y, tip.z, 0.0, 0.0, 0.0).size(0.5F + 1.2F * k);
               }
            }
         }
      } else {
         this.finish();
      }
   }

   @Override
   public void removed() {
      if (!this.released && VfxWorld.local(this.user.getId())) {
         CameraDirector.snapDolly();
      }
   }

   @Override
   public double x() {
      return this.user.getX();
   }

   @Override
   public double y() {
      return this.user.getY();
   }

   @Override
   public double z() {
      return this.user.getZ();
   }
}
