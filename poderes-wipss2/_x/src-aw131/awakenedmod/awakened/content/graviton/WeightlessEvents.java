package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.core.Fx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(
   modid = "awakened"
)
public final class WeightlessEvents {
   private WeightlessEvents() {
   }

   @SubscribeEvent(
      priority = EventPriority.HIGH
   )
   public static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (WeightlessField.isFloating(event.getSource().getEntity())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onAttack(AttackEntityEvent event) {
      if (WeightlessField.isFloating(event.getEntity())) {
         event.setCanceled(true);
         if (event.getEntity().level() instanceof ServerLevel level) {
            Fx.sound(level, event.getEntity().position(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 0.8F, 1.0F);
         }
      }
   }

   @SubscribeEvent
   public static void onServerStopped(ServerStoppedEvent event) {
      WeightlessField.forgetAll();
      HeavyHandBodies.forgetAll();
   }
}
