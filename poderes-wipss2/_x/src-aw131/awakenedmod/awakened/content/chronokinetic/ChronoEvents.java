package com.awakenedmod.awakened.content.chronokinetic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post;

public final class ChronoEvents {
   private ChronoEvents() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ChronoEvents::onDamaged);
   }

   private static void onDamaged(Post event) {
      LivingEntity body = event.getEntity();
      if (!(event.getNewDamage() <= 0.0F) && body.level() instanceof ServerLevel level && ChronoHeld.isHeld(body)) {
         Entity attacker = event.getSource().getEntity();
         Vec3 from = attacker != null ? attacker.getEyePosition() : event.getSource().getSourcePosition();
         Vec3 chest = ChronoFx.chest(body);
         if (from == null) {
            from = chest.add(0.0, 0.0, 1.0);
         }

         Vec3 toward = from.subtract(chest);
         Vec3 hit = toward.lengthSqr() < 1.0E-6 ? chest : chest.add(toward.normalize().scale((double)body.getBbWidth() * 0.5));
         ChronoFx.heldImpact(level, body, hit, from);
      }
   }
}
