package com.impactframe.client;

public final class ImpactFeedbackBridge {
   private ImpactFeedbackBridge() {
   }

   public static void onHighDamageAttack(int targetId, float damage) {
      ImpactFrameClient.onHighDamageAttack(targetId, damage);
   }
}
