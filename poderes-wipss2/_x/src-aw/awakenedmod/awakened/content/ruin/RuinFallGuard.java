package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

final class RuinFallGuard {
   private static final Map<UUID, RuinFallGuard.Guard> GUARDS = new ConcurrentHashMap<>();

   private RuinFallGuard() {
   }

   static void register(IEventBus gameBus) {
      gameBus.addListener(RuinFallGuard::onIncomingDamage);
      gameBus.addListener(event -> GUARDS.clear());
   }

   static void hold(ServerPlayer caster, ServerLevel level, AABB area, long until) {
      GUARDS.put(caster.getUUID(), new RuinFallGuard.Guard(level, area, until));
   }

   static void extend(ServerPlayer caster, long until) {
      GUARDS.computeIfPresent(caster.getUUID(), (id, g) -> until > g.until() ? new RuinFallGuard.Guard(g.level(), g.area(), until) : g);
   }

   static void drop(ServerPlayer caster) {
      GUARDS.remove(caster.getUUID());
   }

   private static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (!GUARDS.isEmpty() && event.getSource().is(DamageTypes.FALL) && event.getEntity().level() instanceof ServerLevel level) {
         LivingEntity var9 = event.getEntity();
         long now = level.getGameTime();

         for (Entry<UUID, RuinFallGuard.Guard> entry : GUARDS.entrySet()) {
            RuinFallGuard.Guard guard = entry.getValue();
            if (now > guard.until()) {
               GUARDS.remove(entry.getKey(), guard);
            } else if (guard.level() == level && guard.area().contains(var9.position())) {
               ServerPlayer caster = level.getServer().getPlayerList().getPlayer(entry.getKey());
               if (caster != null && (Foes.fightsFor(caster, var9) || !Harm.allowed(caster, var9))) {
                  event.setCanceled(true);
                  return;
               }
            }
         }
      }
   }

   private static record Guard(ServerLevel level, AABB area, long until) {
   }
}
