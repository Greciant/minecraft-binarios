package com.awakenedmod.awakened.content.item;

import com.awakenedmod.awakened.core.progression.MasteryScaling;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(
   modid = "awakened"
)
public final class ArchEvokerStaffFangs {
   static final String BALANCE_KEY = "ArchEvokerStaffFangs";

   private ArchEvokerStaffFangs() {
   }

   @SubscribeEvent
   public static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (event.getSource().getDirectEntity() instanceof EvokerFangs fang && fang.getOwner() instanceof Player) {
         event.setAmount(MasteryScaling.configScaledDamage(event.getAmount(), "ArchEvokerStaffFangs"));
      }
   }
}
