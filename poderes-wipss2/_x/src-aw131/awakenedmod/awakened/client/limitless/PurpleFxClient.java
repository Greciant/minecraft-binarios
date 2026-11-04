package com.awakenedmod.awakened.client.limitless;

import com.awakenedmod.awakened.client.ScreenShakeFx;
import com.awakenedmod.awakened.client.render.limitless.LimitlessComfort;
import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.purple.PurpleBallLook;
import com.awakenedmod.awakened.client.render.limitless.purple.PurpleCue;
import com.awakenedmod.awakened.client.render.limitless.purple.PurpleRadiant;
import com.awakenedmod.awakened.content.limitless.LimitlessCast;
import com.awakenedmod.awakened.content.limitless.LimitlessHands;
import com.awakenedmod.awakened.content.limitless.purple.PurpleCast;
import com.awakenedmod.awakened.content.limitless.purple.PurpleSphereEntity;
import com.awakenedmod.awakened.content.limitless.purple.PurpleStrength;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Lab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

final class PurpleFxClient {
   private static final PurpleBallLook LOOK = new PurpleRadiant();
   private static final float CHARGE_END = 49.999F;
   private static final CastHandover HANDOVER = new CastHandover();
   private static final Lab.Value SHAKE = Lab.CLIENT.def("limitless.purple.shake", 0.04, 0.0, 0.3, 0.005);
   private static final Lab.Value SHAKE_RANGE = Lab.CLIENT.def("limitless.purple.shake_range", 16.0, 2.0, 48.0, 1.0);
   private static final Lab.Value OVER_SHAKE = Lab.CLIENT.def("limitless.purple.over_shake", 0.03, 0.0, 0.3, 0.005);

   private PurpleFxClient() {
   }

   static void begin() {
      HANDOVER.clear();
   }

   static void drawSphere(LimitlessFrame f, PurpleSphereEntity sphere) {
      HANDOVER.arrived(sphere.casterId(), sphere.releaseTick());
      float age = 50.0F + Math.max(0.0F, f.since((long)sphere.releaseTick()));
      if (age <= (float)(sphere.gone() + sphere.strength().afterLife())) {
         PurpleCrumbs.draw(f, sphere);
         LOOK.draw(f, sphere(sphere, f.partialTick), age);
      }
   }

   static void tickSphere(ClientLevel level, PurpleSphereEntity sphere) {
      PurpleCrumbs.tick(level, sphere);
      float age = sphere.age(0.0F);
      LOOK.tick(level, sphere(sphere, 0.0F), (int)age);
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && !(age >= (float)sphere.gone())) {
         double d = mc.player.getEyePosition().distanceTo(sphere.centre(0.0F)) - sphere.strength().radius((double)age);
         shake(mc, SHAKE.getF(), d);
      }
   }

   static void tickCasts(ClientLevel level) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         long now = level.getGameTime();

         for (Player p : level.players()) {
            PurpleCast cast = PurpleCast.of(p);
            if (cast.live() && cast.over()) {
               float k = cast.beats((double)now) / 4.0F * (1.0F - cast.pullIn((double)now));
               if (k > 0.0F) {
                  shake(mc, OVER_SHAKE.getF() * k, mc.player.getEyePosition().distanceTo(p.getEyePosition()));
               }
            }
         }
      }
   }

   private static void shake(Minecraft mc, float magnitude, double distance) {
      double range = SHAKE_RANGE.get();
      double d = Math.max(0.0, distance);
      if (d < range) {
         float k = (float)(1.0 - d / range);
         ScreenShakeFx.local(magnitude * k * k * LimitlessComfort.distortion(mc), 0, 3);
      }
   }

   static void endTick() {
      PurpleCrumbs.endTick();
   }

   static void clear() {
      PurpleCrumbs.clear();
   }

   static void drawCasts(LimitlessFrame f) {
      for (Player p : f.level.players()) {
         PurpleCast purple = PurpleCast.of(p);
         LimitlessCast cast = purple.cast();
         float presence;
         if (cast.live()) {
            presence = 1.0F;
         } else if (cast.state() == 3) {
            presence = 1.0F - f.since((long)cast.end()) / 6.0F;
         } else {
            if (!HANDOVER.waiting(f, p, cast)) {
               continue;
            }

            presence = 1.0F;
         }

         if (presence > 0.0F) {
            float age = Math.min(Math.max(0.0F, f.since((long)cast.start())), 49.999F);
            LOOK.draw(f, charge(f, p, purple, presence), age);
         }
      }
   }

   private static PurpleCue charge(LimitlessFrame f, Player p, PurpleCast cast, final float presence) {
      float pt = f.partialTick;
      float yaw = p.getViewYRot(pt);
      float pitch = p.getViewXRot(pt);
      Vec3 eye = p.getEyePosition(pt);
      boolean fp = LimitlessArms.firstPerson(p);
      float arm = LimitlessArms.armPitch(p, pt, fp);
      final Vec3 left = LimitlessHands.purpleHand(eye, (double)yaw, (double)arm, -1, fp);
      final Vec3 right = LimitlessHands.purpleHand(eye, (double)yaw, (double)arm, 1, fp);
      final Vec3 merge = LimitlessHands.purpleMerge(eye, (double)yaw, (double)pitch);
      final Vec3 fwd = AimUtil.direction((double)yaw, (double)pitch);
      final long seed = LimitlessCast.seed(p.getId(), cast.start());
      double now = (double)((float)f.gameTime + pt);
      final float motion = f.since((long)cast.start());
      final PurpleStrength strength = cast.strength();
      final float overcharge = cast.beats(now);
      final float pullIn = cast.pullIn(now);
      return new PurpleCue() {
         @Override
         public Vec3 left() {
            return left;
         }

         @Override
         public Vec3 right() {
            return right;
         }

         @Override
         public Vec3 merge() {
            return merge;
         }

         @Override
         public Vec3 fwd() {
            return fwd;
         }

         @Override
         public long seed() {
            return seed;
         }

         @Override
         public PurpleStrength strength() {
            return strength;
         }

         @Override
         public float presence() {
            return presence;
         }

         @Override
         public float overcharge() {
            return overcharge;
         }

         @Override
         public float pullIn() {
            return pullIn;
         }

         @Override
         public float motion(float age) {
            return motion;
         }
      };
   }

   private static PurpleCue sphere(final PurpleSphereEntity sphere, final float pt) {
      return new PurpleCue() {
         @Override
         public Vec3 left() {
            return sphere.merge();
         }

         @Override
         public Vec3 right() {
            return sphere.merge();
         }

         @Override
         public Vec3 merge() {
            return sphere.merge();
         }

         @Override
         public Vec3 fwd() {
            return sphere.fwd();
         }

         @Override
         public long seed() {
            return sphere.seed();
         }

         @Override
         public PurpleStrength strength() {
            return sphere.strength();
         }

         @Override
         public int gone() {
            return sphere.gone();
         }

         @Override
         public float motion(float age) {
            return sphere.motion(pt);
         }
      };
   }
}
