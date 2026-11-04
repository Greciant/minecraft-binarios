package com.awakenedmod.awakened.client.limitless;

import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.red.RedCue;
import com.awakenedmod.awakened.client.render.limitless.red.RedShove;
import com.awakenedmod.awakened.content.limitless.LimitlessCast;
import com.awakenedmod.awakened.content.limitless.LimitlessHands;
import com.awakenedmod.awakened.content.limitless.red.RedBlastEntity;
import com.awakenedmod.awakened.core.AimUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

final class RedFxClient {
   private static final RedShove LOOK = new RedShove();
   private static final float GATHER_END = 13.999F;
   private static final CastHandover HANDOVER = new CastHandover();

   private RedFxClient() {
   }

   static void begin() {
      HANDOVER.clear();
   }

   static void drawShot(LimitlessFrame f, RedBlastEntity shot) {
      HANDOVER.arrived(shot.casterId(), shot.fireTick());
      float age = 14.0F + Math.max(0.0F, f.since((long)shot.fireTick()));
      LOOK.draw(f, shot(shot), age);
   }

   static void drawCasts(LimitlessFrame f) {
      for (Player p : f.level.players()) {
         LimitlessCast cast = LimitlessCast.of(p, LimitlessCast.RED);
         if (cast.live() || HANDOVER.waiting(f, p, cast)) {
            float age = Math.min(Math.max(0.0F, f.since((long)cast.start())), 13.999F);
            LOOK.draw(f, gather(p, cast, f.partialTick), age);
         }
      }
   }

   static void tickShot(ClientLevel level, RedBlastEntity shot) {
      LOOK.tick(level, shot(shot), 14 + (int)(level.getGameTime() - (long)shot.fireTick()));
   }

   private static RedCue gather(Player p, LimitlessCast cast, float pt) {
      float yaw = p.getViewYRot(pt);
      float pitch = p.getViewXRot(pt);
      boolean fp = LimitlessArms.firstPerson(p);
      final Vec3 tip = LimitlessHands.redTip(p.getEyePosition(pt), (double)yaw, (double)LimitlessArms.armPitch(p, pt, fp), fp);
      final Vec3 fwd = AimUtil.direction((double)yaw, (double)pitch);
      final long seed = LimitlessCast.seed(p.getId(), cast.start());
      return new RedCue() {
         @Override
         public Vec3 fireFrom() {
            return tip;
         }

         @Override
         public Vec3 fwd() {
            return fwd;
         }

         @Override
         public Vec3 burst() {
            return null;
         }

         @Override
         public int hitTick() {
            return -1;
         }

         @Override
         public double floorY() {
            return tip.y - 1.35;
         }

         @Override
         public long seed() {
            return seed;
         }
      };
   }

   private static RedCue shot(final RedBlastEntity shot) {
      return new RedCue() {
         @Override
         public Vec3 fireFrom() {
            return shot.from();
         }

         @Override
         public Vec3 fwd() {
            return shot.fwd();
         }

         @Override
         public Vec3 burst() {
            return shot.burst();
         }

         @Override
         public int hitTick() {
            return shot.hitAge();
         }

         @Override
         public double floorY() {
            return shot.floorY();
         }

         @Override
         public long seed() {
            return shot.seed();
         }
      };
   }
}
