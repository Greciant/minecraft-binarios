package com.awakenedmod.awakened.core.team;

import com.awakenedmod.awakened.Config;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;

public final class TeamEvents {
   private TeamEvents() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(EventPriority.HIGHEST, TeamEvents::onIncomingDamage);
      gameBus.addListener(EventPriority.HIGHEST, TeamEvents::onProjectileImpact);
      gameBus.addListener(TeamEvents::onServerTick);
      gameBus.addListener(TeamEvents::onServerStopped);
   }

   private static void onServerTick(Post event) {
      if ((Boolean)Config.TEAMS_ENABLED.get() && event.getServer().getTickCount() % 20 == 0) {
         TeamActions.tickInvites(event.getServer());
      }
   }

   private static void onServerStopped(ServerStoppedEvent event) {
      TeamService.clearInvites();
   }

   private static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (Teams.harmForbidden(event.getSource().getEntity(), event.getEntity())) {
         event.setCanceled(true);
         TeamFeedback.blocked(event.getSource().getEntity(), event.getEntity());
      }
   }

   private static void onProjectileImpact(ProjectileImpactEvent event) {
      if (event.getRayTraceResult() instanceof EntityHitResult hit && Teams.harmForbidden(event.getProjectile(), hit.getEntity())) {
         event.setCanceled(true);
      }
   }
}
