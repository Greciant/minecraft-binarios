package com.awakenedmod.awakened.core.trade;

import java.util.UUID;

final class PowerTrade {
   final UUID initiator;
   final UUID target;
   private final String initiatorName;
   private final String targetName;
   final String initiatorPower;
   final String targetPower;
   PowerTrade.Stage stage = PowerTrade.Stage.INVITED;
   long stageStart;
   long expires;
   private boolean initiatorReady;
   private boolean targetReady;

   PowerTrade(UUID initiator, UUID target, String initiatorName, String targetName, String initiatorPower, String targetPower) {
      this.initiator = initiator;
      this.target = target;
      this.initiatorName = initiatorName;
      this.targetName = targetName;
      this.initiatorPower = initiatorPower;
      this.targetPower = targetPower;
   }

   UUID other(UUID party) {
      return party.equals(this.initiator) ? this.target : this.initiator;
   }

   String nameOf(UUID party) {
      return party.equals(this.initiator) ? this.initiatorName : this.targetName;
   }

   String powerOf(UUID party) {
      return party.equals(this.initiator) ? this.initiatorPower : this.targetPower;
   }

   boolean ready(UUID party) {
      return party.equals(this.initiator) ? this.initiatorReady : this.targetReady;
   }

   void setReady(UUID party) {
      if (party.equals(this.initiator)) {
         this.initiatorReady = true;
      } else {
         this.targetReady = true;
      }
   }

   boolean bothReady() {
      return this.initiatorReady && this.targetReady;
   }

   void advance(PowerTrade.Stage next, long now, long lifetime) {
      this.stage = next;
      this.stageStart = now;
      this.expires = now + lifetime;
      this.initiatorReady = false;
      this.targetReady = false;
   }

   static enum Stage {
      INVITED,
      REVIEW,
      FINAL;
   }
}
