package com.awakenedmod.awakened.client.limitless;

import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.rct.RctCue;
import com.awakenedmod.awakened.client.render.limitless.rct.RctMend;
import com.awakenedmod.awakened.content.limitless.LimitlessCast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

final class RctFxClient {
   private static final RctMend LOOK = new RctMend();
   private static final float FADE_IN = 6.0F;
   private static final float FADE_OUT = 8.0F;

   private RctFxClient() {
   }

   static void draw(LimitlessFrame f) {
      for (Player p : f.level.players()) {
         LimitlessCast cast = LimitlessCast.of(p, LimitlessCast.RCT);
         float presence;
         if (cast.live()) {
            presence = Math.min(1.0F, f.since((long)cast.start()) / 6.0F);
         } else {
            if (cast.state() != 3) {
               continue;
            }

            float held = Math.min(1.0F, (float)(cast.end() - cast.start()) / 6.0F);
            presence = held * (1.0F - f.since((long)cast.end()) / 8.0F);
         }

         if (presence > 0.0F) {
            LOOK.draw(f, cue(f, p, cast, presence), Math.max(0.0F, f.since((long)cast.start())));
         }
      }
   }

   private static RctCue cue(LimitlessFrame f, Player p, LimitlessCast cast, final float presence) {
      final Vec3 feet = p.getPosition(f.partialTick);
      final float height = p.getBbHeight();
      final boolean own = p == f.mc.player && f.mc.options.getCameraType().isFirstPerson();
      final long seed = LimitlessCast.seed(p.getId(), cast.start());
      return new RctCue() {
         @Override
         public Vec3 feet() {
            return feet;
         }

         @Override
         public float height() {
            return height;
         }

         @Override
         public float presence() {
            return presence;
         }

         @Override
         public boolean ownView() {
            return own;
         }

         @Override
         public long seed() {
            return seed;
         }
      };
   }
}
