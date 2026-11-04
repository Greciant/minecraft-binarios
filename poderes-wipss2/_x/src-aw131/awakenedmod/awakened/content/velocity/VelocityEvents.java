package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.PowerSuppression;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import com.awakenedmod.awakened.core.power.AbilityFeedback;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Start;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable.Result;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public final class VelocityEvents {
   private static final float KNOCKBACK_CUT = 0.3F;
   private static final double MOVING_EPS = 0.01;
   private static final float SLOWNESS_SCALE = 0.5F;
   private static final String PHASING_BUSY = "awakened.feedback.velocity_phasing_busy";
   private static final String TUNNEL_BUSY = "awakened.feedback.velocity_tunnel_busy";
   private static final int BUSY_MESSAGE_TICKS = 20;
   private static final Set<UUID> TRIMMING = ConcurrentHashMap.newKeySet();
   private static final Map<UUID, Long> TOLD_BUSY = new ConcurrentHashMap<>();

   private VelocityEvents() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(VelocityEvents::onKnockback);
      gameBus.addListener(VelocityEvents::onEffectApplicable);
      gameBus.addListener(VelocityEvents::onUseItemStart);
      gameBus.addListener(VelocityEvents::onProjectileImpact);
      gameBus.addListener(VelocityEvents::onAttackEntity);
      gameBus.addListener(VelocityEvents::onLeftClickBlock);
      gameBus.addListener(VelocityEvents::onRightClickBlock);
      gameBus.addListener(VelocityEvents::onRightClickItem);
      gameBus.addListener(VelocityEvents::onEntityInteract);
      gameBus.addListener(VelocityEvents::onEntityInteractSpecific);
      gameBus.addListener(VelocityEvents::onLoggedOut);
      gameBus.addListener(VelocityEvents::onServerStopped);
      LaneTransit.register(gameBus);
   }

   private static boolean phasing(Player player) {
      return VelocityVisualState.of(player).phasing();
   }

   @Nullable
   private static String busy(Player player) {
      if (phasing(player)) {
         return "awakened.feedback.velocity_phasing_busy";
      } else {
         return SpeedTunnelAbility.running(player) ? "awakened.feedback.velocity_tunnel_busy" : null;
      }
   }

   private static void onProjectileImpact(ProjectileImpactEvent event) {
      if (event.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof Player player && busy(player) != null) {
         event.setCanceled(true);
      }
   }

   private static void onAttackEntity(AttackEntityEvent event) {
      if (refused(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   private static void onLeftClickBlock(LeftClickBlock event) {
      cancelIfRefused(event);
   }

   private static void onRightClickBlock(RightClickBlock event) {
      cancelIfRefused(event);
   }

   private static void onRightClickItem(RightClickItem event) {
      cancelIfRefused(event);
   }

   private static void onEntityInteract(EntityInteract event) {
      cancelIfRefused(event);
   }

   private static void onEntityInteractSpecific(EntityInteractSpecific event) {
      cancelIfRefused(event);
   }

   private static <E extends PlayerInteractEvent & ICancellableEvent> void cancelIfRefused(E event) {
      if (refused(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   private static boolean refused(Player player) {
      String reason = busy(player);
      if (reason == null) {
         return false;
      } else {
         tellBusy(player, reason);
         return true;
      }
   }

   private static void tellBusy(Player player, String reason) {
      if (player instanceof ServerPlayer server) {
         long now = server.level().getGameTime();
         Long last = TOLD_BUSY.get(server.getUUID());
         if (last == null || now - last >= 20L) {
            TOLD_BUSY.put(server.getUUID(), now);
            AbilityFeedback.fail(server, reason);
         }
      }
   }

   private static void onLoggedOut(PlayerLoggedOutEvent event) {
      TOLD_BUSY.remove(event.getEntity().getUUID());
   }

   private static void onServerStopped(ServerStoppedEvent event) {
      SpeedTunnelAbility.closeAllLanes();
      TOLD_BUSY.clear();
      TRIMMING.clear();
      OverdriveControl.clearAll();
   }

   private static boolean passivesLive(ServerPlayer player) {
      return VelocityPower.owns(player) && !PowerSuppression.isSuppressed(player);
   }

   private static void onKnockback(LivingKnockBackEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && passivesLive(player)) {
         PowerData data = PowerService.data(player);
         if (OverdriveAbility.isOn(data) && !(player.getKnownMovement().horizontalDistance() < 0.01)) {
            event.setStrength(event.getStrength() * 0.7F);
            return;
         }

         return;
      }
   }

   private static void onEffectApplicable(Applicable event) {
      if (event.getEntity() instanceof ServerPlayer player && passivesLive(player)) {
         MobEffectInstance applied = event.getEffectInstance();
         if (applied.is(MobEffects.MOVEMENT_SLOWDOWN) && !applied.isInfiniteDuration() && !TRIMMING.contains(player.getUUID())) {
            int halved = Math.max(1, Math.round((float)applied.getDuration() * 0.5F));
            if (halved >= applied.getDuration()) {
               return;
            }

            event.setResult(Result.DO_NOT_APPLY);
            TRIMMING.add(player.getUUID());

            try {
               player.addEffect(
                  new MobEffectInstance(
                     MobEffects.MOVEMENT_SLOWDOWN, halved, applied.getAmplifier(), applied.isAmbient(), applied.isVisible(), applied.showIcon()
                  ),
                  event.getEffectSource()
               );
            } finally {
               TRIMMING.remove(player.getUUID());
            }

            return;
         }

         return;
      }
   }

   private static void onUseItemStart(Start event) {
      if (event.getEntity() instanceof Player player) {
         VelocityVisual visual = VelocityVisualState.of(player);
         if (visual.overdrive()) {
            if (busy(player) != null) {
               event.setCanceled(true);
            } else if (event.getItem().has(DataComponents.FOOD)) {
               float speed = visual.speedMult();
               if (!(speed <= 1.0F)) {
                  event.setDuration(Math.max(1, Math.round((float)event.getDuration() / speed)));
               }
            }
         }
      }
   }
}
