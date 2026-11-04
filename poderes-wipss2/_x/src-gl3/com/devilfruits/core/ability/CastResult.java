package com.devilfruits.core.ability;

public enum CastResult {
   SUCCESS(null),
   CHARGING(null),
   CHANNELLING(null),
   TOGGLED(null),
   COMBO_STEP(null),
   NO_FRUIT(null),
   NOT_IN_FORM(null),
   SUPPRESSED("message.devilfruits.suppressed"),
   DISABLED("message.devilfruits.disabled"),
   LOCKED("message.devilfruits.locked"),
   COOLDOWN("message.devilfruits.cooldown"),
   NO_ENERGY(null),
   REQUIREMENT(null),
   BUSY(null);

   public final String messageKey;

   private CastResult(String messageKey) {
      this.messageKey = messageKey;
   }

   public boolean succeeded() {
      return this == SUCCESS || this == CHARGING || this == CHANNELLING || this == TOGGLED || this == COMBO_STEP;
   }
}
