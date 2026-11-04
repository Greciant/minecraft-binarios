package com.awakenedmod.awakened.content.command.vocab;

import javax.annotation.Nullable;

public record Clash(String id) {
   public static final Clash ONLY_PLAYERS = new Clash("only_players");
   public static final Clash SNEAK_DISMOUNTS = new Clash("sneak_dismounts");
   public static final Clash NEVER_FIGHTS = new Clash("never_fights");
   public static final Clash SNEAK_SINKS = new Clash("sneak_sinks");
   public static final Clash NEEDS_MOVING = new Clash("needs_moving");
   public static final Clash STOPS_THE_FALL = new Clash("stops_the_fall");
   public static final Clash JUMPS_FROM_GROUND = new Clash("jumps_from_ground");
   public static final Clash CARRIES_YOU = new Clash("carries_you");
   private static final String PREFIX = "awakened.command.clash.";

   public String noteKey() {
      return "awakened.command.clash." + this.id;
   }

   public String sayKey() {
      return "awakened.command.clash." + this.id + ".say";
   }

   @Nullable
   public static Clash of(Target target, Effect effect, Trigger trigger) {
      if (effect.playersOnly() && !target.namesPlayers()) {
         return ONLY_PLAYERS;
      } else {
         Clash clash = target.clash(target, effect, trigger);
         if (clash == null) {
            clash = effect.clash(target, effect, trigger);
         }

         if (clash == null) {
            clash = trigger.clash(target, effect, trigger);
         }

         return clash;
      }
   }
}
