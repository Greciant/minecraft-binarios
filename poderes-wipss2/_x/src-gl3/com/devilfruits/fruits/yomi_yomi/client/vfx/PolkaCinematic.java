package com.devilfruits.fruits.yomi_yomi.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.post.VfxPost;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class PolkaCinematic extends YomiCinematic {
   private static final int SMEAR = 3;
   private final List<PolkaCinematic.Smear> smears = new ArrayList<>();
   private final double halfCone;
   private final int thrusts;
   private int done;
   private int stopAt = -1;

   public PolkaCinematic(Entity user, Vec3 at, float coneDegrees, int thrusts, long seed) {
      super(VfxAnchor.of(user, at), at, seed);
      this.halfCone = Math.toRadians((double)coneDegrees) * 0.5;
      this.thrusts = thrusts;
   }

   public void stop() {
      if (this.stopAt < 0) {
         this.stopAt = this.age;
         this.accent();
      }
   }

   public int entityId() {
      return this.anchor.entityId();
   }

   @Override
   protected int length() {
      int end = this.stopAt >= 0 ? this.stopAt : 81;
      return end + 3 + 2;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity user = this.anchorEntity();
      if (user != null) {
         if (this.age <= 4) {
            Vec3 feet = user.position();
            SoulFlames.wispTrail(this.random, feet.add(0.0, 0.1, 0.0), feet.add(0.0, 0.4, 0.0), 3.0F, 0.7F);
         }

         if (this.stopAt < 0) {
            int due = this.age < 4 ? 0 : Math.min(this.thrusts, (this.age - 4) / 2 + 1);

            while (this.done < due) {
               this.done++;
               this.thrust(user);
               if (this.done == this.thrusts) {
                  this.stopAt = this.age;
                  this.accent();
               }
            }
         }
      }
   }

   private void thrust(Entity user) {
      Vec3 eye = user.getEyePosition();
      Vec3 look = user.getViewVector(1.0F);
      Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0));
      right = right.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
      Vec3 up = right.cross(look).normalize();
      Vec3 hand = eye.add(0.0, -0.35, 0.0).add(right.scale(0.3)).add(look.scale(0.5));

      for (int k = 0; k < 3; k++) {
         double ang = this.random.nextDouble() * Math.PI * 2.0;
         double spread = Math.sqrt(this.random.nextDouble()) * this.halfCone * 0.85;
         Vec3 dir = look.scale(Math.cos(spread)).add(right.scale(Math.sin(spread) * Math.cos(ang))).add(up.scale(Math.sin(spread) * Math.sin(ang))).normalize();
         Vec3 from = hand.add(dir.scale(0.3 + this.random.nextDouble() * 0.5));
         Vec3 to = from.add(dir.scale(1.3 + this.random.nextDouble() * 1.5));
         this.smears.add(new PolkaCinematic.Smear(from, to, (float)this.age, 0.16F + this.random.nextFloat() * 0.1F));
         VfxParticles.emit(
            YomiLook.STREAK, to.x, to.y, to.z, dir.x * 0.22 + this.random.nextGaussian() * 0.05, 0.04, dir.z * 0.22 + this.random.nextGaussian() * 0.05
         );
         if (k == 0) {
            VfxParticles.emit(YomiLook.STREAK, to.x, to.y, to.z, dir.x * 0.3, dir.y * 0.3, dir.z * 0.3);
         }
      }

      this.smears.removeIf(s -> (float)this.age - s.born > 4.0F);
   }

   private void accent() {
      Entity user = this.anchorEntity();
      if (user != null) {
         Vec3 c = user.getEyePosition().add(user.getViewVector(1.0F).scale(2.6)).add(0.0, -0.3, 0.0);
         VfxParticles.emit(YomiLook.SOUL_FLASH, c.x, c.y, c.z, 0.0, 0.0, 0.0).size(1.9F);
         SoulFlames.burst(this.random, c, 1.3F);
         if (VfxWorld.local(user.getId())) {
            VfxPost.flash(14483400, 0.22F);
            VfxPost.chromatic(0.25F);
            CameraDirector.shake(0.22F, 0.08F, 1.2F, 16.0F);
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      float t = (float)this.age + f.partial;

      for (PolkaCinematic.Smear s : this.smears) {
         float life = t - s.born;
         if (!(life < 0.0F) && !(life > 3.0F)) {
            float k = 1.0F - life / 3.0F;
            Vec3 tip = s.a.lerp(s.b, (double)Math.min(1.0F, life * 1.6F + 0.4F));
            SoulCuts.soulThrust(f, s.a, tip, s.width, 0.7F + 0.5F * k, k, -t);
         }
      }
   }

   public static void hit(@Nullable Entity target, Vec3 point, Vec3 dir, boolean beat, long seed) {
      RandomSource r = RandomSource.create(seed);
      VfxParticles.emit(YomiLook.SOUL_FLASH, point.x, point.y, point.z, 0.0, 0.0, 0.0).size(beat ? 0.85F : 0.55F);

      for (int i = 0; i < VfxSettings.count(beat ? 6.0F : 3.0F); i++) {
         VfxParticles.emit(
            YomiLook.STREAK,
            point.x,
            point.y,
            point.z,
            dir.x * 0.25 + r.nextGaussian() * 0.08,
            0.05 + r.nextDouble() * 0.08,
            dir.z * 0.25 + r.nextGaussian() * 0.08
         );
      }

      if (beat && VfxSettings.hitFlashes() && target != null) {
         VfxClock.hitStop(1);
      }
   }

   @Override
   public double y() {
      return this.at.y + 1.0;
   }

   private static record Smear(Vec3 a, Vec3 b, float born, float width) {
   }
}
