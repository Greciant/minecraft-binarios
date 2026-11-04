package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.compat.BetterCombatCompat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;

public final class ClickClaims {
   private static final List<ClickClaims.Claim> CLAIMS = new CopyOnWriteArrayList<>();

   private ClickClaims() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(EventPriority.HIGH, ClickClaims::onInteract);
   }

   public static void add(BooleanSupplier wants, Runnable take) {
      CLAIMS.add(new ClickClaims.Claim(wants, take));
   }

   private static ClickClaims.Claim wanting() {
      for (ClickClaims.Claim claim : CLAIMS) {
         if (claim.wants().getAsBoolean()) {
            return claim;
         }
      }

      return null;
   }

   private static void onInteract(InteractionKeyMappingTriggered event) {
      if (event.isAttack()) {
         ClickClaims.Claim claim = wanting();
         if (claim != null) {
            event.setCanceled(true);
            event.setSwingHand(false);
            claim.take().run();
         }
      }
   }

   public static boolean takeFirst() {
      if (!BetterCombatCompat.ownsSwing()) {
         return false;
      } else {
         ClickClaims.Claim claim = wanting();
         if (claim == null) {
            return false;
         } else {
            claim.take().run();
            return true;
         }
      }
   }

   public static boolean holdClaimed() {
      return BetterCombatCompat.ownsSwing() && wanting() != null;
   }

   private static record Claim(BooleanSupplier wants, Runnable take) {
   }
}
