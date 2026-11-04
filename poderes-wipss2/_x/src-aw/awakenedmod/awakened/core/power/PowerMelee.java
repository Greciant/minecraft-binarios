package com.awakenedmod.awakened.core.power;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerSuppression;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.PowerData;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(
   modid = "awakened"
)
public final class PowerMelee {
   private static final Map<UUID, Long> LAST_SWING = new ConcurrentHashMap<>();
   private static final Map<UUID, Map<Integer, Long>> LAST_HIT = new ConcurrentHashMap<>();
   private static final double SEPARATION_GAIN = 0.25;
   private static final double SEPARATION_MAX = 0.3;
   private static final long HIT_WINDOW_FORGET_TICKS = 100L;

   private PowerMelee() {
   }

   public static MeleeProfile profileOf(Player player) {
      if (player == null) {
         return null;
      } else {
         PowerData data = AwakenedAttachments.get(player);
         if (data.hasPower() && data.awakened() && !PowerSuppression.isSuppressed(player)) {
            Power power = PowerRegistry.get(data.powerId());
            return power == null ? null : power.meleeProfile(player);
         } else {
            return null;
         }
      }
   }

   public static void swing(ServerPlayer player) {
      if (player != null && !player.isSpectator()) {
         MeleeProfile profile = profileOf(player);
         if (profile != null) {
            if (!profile.requiresEmptyHand() || player.getMainHandItem().isEmpty()) {
               long now = player.serverLevel().getGameTime();
               Long last = LAST_SWING.get(player.getUUID());
               if (last == null || now - last >= (long)profile.cadenceTicks(player)) {
                  LAST_SWING.put(player.getUUID(), now);
                  LivingEntity target = AimUtil.aimedLivingSoft(
                     player,
                     profile.reach(player),
                     profile.coneHalfDegrees(player),
                     e -> e != player && e.isAlive() && !e.isSpectator() && profile.accepts(player, e)
                  );
                  if (target == null) {
                     profile.onMiss(player);
                  } else {
                     boolean dealt = strike(player, target, profile, now);
                     profile.onHit(player, target, dealt);
                  }
               }
            }
         }
      }
   }

   public static long lastSwingTick(Player player) {
      Long last = LAST_SWING.get(player.getUUID());
      return last == null ? -1L : last;
   }

   private static boolean strike(ServerPlayer player, LivingEntity target, MeleeProfile profile, long now) {
      float damage = profile.damage(player, target);
      boolean allowed = damage > 0.0F && Harm.allowed(player, target);
      boolean dealt = false;
      boolean reopened = false;
      int hadIFrames = 0;
      if (allowed) {
         int reopen = profile.reopenTicks(player);
         if (reopen > 0 && now - lastHit(player, target) >= (long)reopen) {
            hadIFrames = target.invulnerableTime;
            reopened = true;
            target.invulnerableTime = 0;
         }

         ServerLevel level = player.serverLevel();
         dealt = Dmg.hurtNoKnockback(target, level, Dmg.by(player, DamageTypes.PLAYER_ATTACK), damage);
      }

      if (dealt) {
         LAST_HIT.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>()).put(target.getId(), now);
         prune(player.getUUID(), now);
      } else if (reopened) {
         target.invulnerableTime = Math.max(target.invulnerableTime, hadIFrames);
      }

      double knockback = profile.knockback(player, target);
      if (knockback > 0.0) {
         Harm.knockback(player, target, knockback, player.getX(), player.getZ());
      }

      separate(player, target, profile.separation(player, target));
      return dealt;
   }

   private static void separate(ServerPlayer player, LivingEntity target, double separation) {
      if (!(separation <= 0.0) && !(target instanceof Player)) {
         Vec3 away = new Vec3(target.getX() - player.getX(), 0.0, target.getZ() - player.getZ());
         double dist = away.length();
         if (!(dist >= separation)) {
            Vec3 dir = dist < 1.0E-4 ? AimUtil.lookFlat(player) : away.scale(1.0 / dist);
            double push = Math.min(0.3, (separation - dist) * 0.25);
            Harm.push(player, target, dir.x * push, 0.0, dir.z * push);
         }
      }
   }

   private static long lastHit(ServerPlayer player, LivingEntity target) {
      Map<Integer, Long> hits = LAST_HIT.get(player.getUUID());
      Long tick = hits == null ? null : hits.get(target.getId());
      return tick == null ? -4611686018427387904L : tick;
   }

   private static void prune(UUID attacker, long now) {
      Map<Integer, Long> hits = LAST_HIT.get(attacker);
      if (hits != null) {
         hits.values().removeIf(tick -> now - tick > 100L);
      }
   }

   public static void clearPlayer(Player player) {
      if (player != null) {
         LAST_SWING.remove(player.getUUID());
         LAST_HIT.remove(player.getUUID());
      }
   }

   public static void clearAll() {
      LAST_SWING.clear();
      LAST_HIT.clear();
   }

   @SubscribeEvent
   public static void onLogout(PlayerLoggedOutEvent event) {
      clearPlayer(event.getEntity());
   }

   @SubscribeEvent
   public static void onRespawn(PlayerRespawnEvent event) {
      clearPlayer(event.getEntity());
   }

   @SubscribeEvent
   public static void onChangedDimension(PlayerChangedDimensionEvent event) {
      clearPlayer(event.getEntity());
   }

   @SubscribeEvent
   public static void onDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof Player dead) {
         clearPlayer(dead);
      }
   }

   @SubscribeEvent
   public static void onServerStopped(ServerStoppedEvent event) {
      clearAll();
   }
}
