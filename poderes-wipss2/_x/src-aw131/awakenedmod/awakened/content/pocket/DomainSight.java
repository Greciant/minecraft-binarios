package com.awakenedmod.awakened.content.pocket;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post;

public final class DomainSight {
   static final double SIGHT = 7.0;
   private static final double SCAN = 64.0;
   private static boolean ambient;

   private DomainSight() {
   }

   static void ambient(Runnable hits) {
      boolean outer = ambient;
      ambient = true;

      try {
         hits.run();
      } finally {
         ambient = outer;
      }
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(DomainSight::onChangeTarget);
      gameBus.addListener(DomainSight::onDamaged);
   }

   private static void onChangeTarget(LivingChangeTargetEvent event) {
      if (event.getEntity() instanceof Mob mob && hidden(mob, event.getNewAboutToBeSetTarget())) {
         event.setCanceled(true);
      }
   }

   private static void onDamaged(Post event) {
      if (!ambient && event.getEntity() instanceof Mob mob && event.getSource().getEntity() instanceof ServerPlayer caster) {
         DomainManager.reveal(caster.getUUID(), mob.getUUID());
      }
   }

   static void dropFar(ServerPlayer caster, ServerLevel level) {
      for (Mob mob : level.getEntitiesOfClass(Mob.class, caster.getBoundingBox().inflate(64.0), m -> m.isAlive() && hidden(m, caster))) {
         if (mob.getTarget() == caster) {
            mob.setTarget(null);
         }

         if (mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET) && mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) == caster) {
            mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
         }
      }
   }

   private static boolean hidden(Mob mob, LivingEntity target) {
      if (target instanceof ServerPlayer caster
         && DomainManager.hasDome(caster)
         && mob.distanceToSqr(caster) > 49.0
         && !DomainManager.revealed(caster.getUUID(), mob.getUUID())) {
         return true;
      }

      return false;
   }
}
