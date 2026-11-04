package com.awakenedmod.awakened.core.admin;

public enum AdminAction {
   SET_POWER,
   REROLL,
   CLEAR,
   REFILL_STAMINA,
   TOGGLE_INFINITE_STAMINA,
   SET_MASTERY,
   GRANT_XP,
   BLEED_SELF,
   SPAWN_SUPPRESSOR,
   SPAWN_STALKER,
   SPAWN_ENFORCER,
   SPAWN_OVERSEER,
   SPAWN_ARBITER,
   SPAWN_SENTINEL,
   GRANT_RELIC,
   GRANT_BELT,
   GRANT_SOULS,
   RESET_SOULS,
   HAUNT_SELF;

   private static final AdminAction[] VALUES = values();

   public static AdminAction byId(int id) {
      return id >= 0 && id < VALUES.length ? VALUES[id] : null;
   }
}
