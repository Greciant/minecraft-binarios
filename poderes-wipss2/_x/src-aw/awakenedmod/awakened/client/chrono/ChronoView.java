package com.awakenedmod.awakened.client.chrono;

import com.awakenedmod.awakened.client.ClientFreeze;
import com.awakenedmod.awakened.client.render.NegativeLayers;
import com.awakenedmod.awakened.content.chronokinetic.ChronoHeld;
import com.awakenedmod.awakened.core.net.ChronoFxPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class ChronoView {
   private static final double ENTER_MARGIN = 0.25;
   private static final double LEAVE_MARGIN = 1.0;
   private static ChronoFxClient.Effect own;
   private static ChronoFxClient.Effect around;
   private static ChronoFxClient.Effect released;

   private ChronoView() {
   }

   public static ChronoFxClient.Effect own() {
      return own;
   }

   public static ChronoFxClient.Effect around() {
      return around;
   }

   public static ChronoFxClient.Effect governing() {
      return own != null ? own : around;
   }

   public static ChronoFxClient.Effect ending() {
      ChronoFxClient.Effect field = governing();
      if (field != null) {
         return field.resolveAge(0.0F) >= 0.0F ? field : null;
      } else {
         return released;
      }
   }

   public static boolean frozen() {
      Minecraft mc = Minecraft.getInstance();
      return ClientFreeze.isFrozen() && mc.player != null && ChronoHeld.isHeld(mc.player);
   }

   public static boolean calm() {
      return (Boolean)Minecraft.getInstance().options.hideLightningFlash().get();
   }

   public static boolean noNegative() {
      return calm() || !NegativeLayers.available();
   }

   static void tick(Minecraft mc) {
      if (mc.player == null) {
         clear();
      } else {
         int self = mc.player.getId();
         Vec3 eye = mc.player.getEyePosition();
         ChronoFxClient.Effect was = governing();
         if (was != null && was.ended()) {
            released = was;
         }

         if (released != null && !ChronoFxClient.effects().contains(released)) {
            released = null;
         }

         own = null;
         ChronoFxClient.Effect stay = null;
         ChronoFxClient.Effect enter = null;

         for (ChronoFxClient.Effect e : ChronoFxClient.effects(ChronoFxPayload.Kind.TIME_STOP)) {
            if (!e.ended()) {
               if (e.entityId == self && own == null) {
                  own = e;
               }

               double d = Math.sqrt(eye.distanceToSqr(e.pos));
               if (e == around && d < (double)e.radius + 1.0) {
                  stay = e;
               } else if (enter == null && d < (double)e.radius - 0.25) {
                  enter = e;
               }
            }
         }

         around = stay != null ? stay : enter;
      }
   }

   static void clear() {
      own = null;
      around = null;
      released = null;
   }
}
