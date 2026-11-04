package com.devilfruits.fruits.gomu_gomu;

import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.fruits.gomu_gomu.ability.GatlingAbility;
import com.devilfruits.fruits.gomu_gomu.ability.StrikeAbility;
import com.devilfruits.fruits.gomu_gomu.rubber.BulletCatch;
import com.devilfruits.fruits.gomu_gomu.rubber.RubberBody;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

final class GomuEvents {
   private static final Map<UUID, Long> LAST_THUD = new HashMap<>();

   static void register(IEventBus bus) {
      bus.addListener(EventPriority.HIGH, GomuEvents::onIncomingDamage);
      bus.addListener(GomuEvents::onFall);
      bus.addListener(GomuEvents::onLightning);
      bus.addListener(EventPriority.HIGH, GomuEvents::onProjectileImpact);
      bus.addListener(e -> BulletCatch.tick(e.getServer()));
      bus.addListener(e -> {
         if (e.getEntity() instanceof ServerPlayer player) {
            BulletCatch.release(player);
            LAST_THUD.remove(player.getUUID());
            StrikeAbility.forget(player.getUUID());
            GatlingAbility.forget(player.getUUID());
         }
      });
      bus.addListener(e -> BulletCatch.clear());
   }

   private static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         RubberBody.Shrug shrug = RubberBody.shrugs(player, event.getSource());
         if (shrug != null) {
            event.setCanceled(true);
            long now = player.level().getGameTime();
            if (shrug == RubberBody.Shrug.BLUNT && event.getSource().getEntity() != null && now - LAST_THUD.getOrDefault(player.getUUID(), 0L) >= 5L) {
               LAST_THUD.put(player.getUUID(), now);
               GomuTriggers.BLUNT.get().trigger(player);
               player.level()
                  .playSound(
                     null,
                     player.getX(),
                     player.getY() + 1.0,
                     player.getZ(),
                     GomuSounds.BLUNT_ABSORB.get(),
                     SoundSource.PLAYERS,
                     0.8F,
                     0.9F + player.getRandom().nextFloat() * 0.25F
                  );
            }
         }
      }
   }

   private static void onFall(LivingFallEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && GomuGomu.canUse(player) && GomuGomu.flag("fallImmunity")) {
         event.setDamageMultiplier(0.0F);
         if (event.getDistance() >= 40.0F) {
            GomuTriggers.HIGH_FALL_SURVIVED.get().trigger(player);
         }

         if ((double)event.getDistance() >= GomuGomu.setting("rubberLandingHeight") && !player.isFallFlying()) {
            ServerFx.animate(player, GomuGomu.id("rubber_land"));
            player.level()
               .playSound(
                  null,
                  player.getX(),
                  player.getY(),
                  player.getZ(),
                  GomuSounds.RUBBER_LAND.get(),
                  SoundSource.PLAYERS,
                  Math.min(1.2F, 0.5F + event.getDistance() / 30.0F),
                  0.85F + player.getRandom().nextFloat() * 0.15F
               );
         }

         return;
      }
   }

   private static void onLightning(EntityStruckByLightningEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && GomuGomu.canUse(player) && GomuGomu.flag("lightningImmunity")) {
         event.setCanceled(true);
      }
   }

   private static void onProjectileImpact(ProjectileImpactEvent event) {
      Projectile projectile = event.getProjectile();
      if (!projectile.level().isClientSide() && event.getRayTraceResult() instanceof EntityHitResult hit) {
         if (hit.getEntity() instanceof ServerPlayer player && BulletCatch.catches(player, projectile)) {
            event.setCanceled(true);
            BulletCatch.absorb(player, projectile, hit.getLocation());
         }
      }
   }

   private GomuEvents() {
   }
}
