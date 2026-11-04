package com.awakenedmod.awakened.content.item;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.compat.BetterCombatCompat;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;

@EventBusSubscriber(
   modid = "awakened"
)
public final class ReaperScytheAnimator {
   private static final String ANIM_GRIP = "reaper_scythe_grip";
   private static final String ANIM_REAP = "reaper_scythe_reap";
   private static final int REAP_TICKS = 14;
   private static final int REAP_THROTTLE = 9;
   private static final double SWEEP_FX_SPREAD = 0.9;
   private static final double SWEEP_RANGE = 4.0;
   private static final double SWEEP_ARC_HALF = 70.0;
   private static final double SWEEP_KNOCKBACK = 0.4;
   private static final float SWEEP_CHARGE = 0.9F;
   static final String REAP_BALANCE_KEY = "ReaperScytheReap";
   private static final Set<UUID> POSED = ConcurrentHashMap.newKeySet();
   private static final Map<UUID, Long> REAP_UNTIL = new ConcurrentHashMap<>();

   private ReaperScytheAnimator() {
   }

   static void onSwing(ServerPlayer player) {
      if (!BetterCombatCompat.ownsSwing()) {
         UUID id = player.getUUID();
         long now = player.serverLevel().getGameTime();
         long until = REAP_UNTIL.getOrDefault(id, 0L);
         if (until <= now || until - now <= 5L) {
            REAP_UNTIL.put(id, now + 14L);
            POSED.remove(id);
            PowerService.stopAnimation(player);
            PowerService.playAnimation(player, "reaper_scythe_reap");
         }
      }
   }

   static void onHit(ServerPlayer player, LivingEntity primary) {
      if (!BetterCombatCompat.ownsSwing() && !(player.getAttackStrengthScale(0.5F) < 0.9F)) {
         ServerLevel level = player.serverLevel();
         Vec3 eye = player.getEyePosition();
         Vec3 look = player.getLookAngle();
         sweep(player, level, eye, look, primary);
         sweepFx(player, level);
         Fx.sound(level, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 0.8F);
      }
   }

   private static void sweepFx(ServerPlayer player, ServerLevel level) {
      double yaw = Math.toRadians((double)player.getYRot());
      double fx = -Math.sin(yaw);
      double fz = Math.cos(yaw);
      double px = -fz;
      double pz = fx;
      double y = player.getY(0.65);

      for (double o : new double[]{-0.9, 0.0, 0.9}) {
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + fx + px * o, y, player.getZ() + fz + pz * o, 0, fx, 0.0, fz, 0.0);
      }
   }

   private static void sweep(ServerPlayer player, ServerLevel level, Vec3 eye, Vec3 look, LivingEntity primary) {
      double cosHalf = Math.cos(Math.toRadians(70.0));
      DamageSource src = level.damageSources().playerAttack(player);
      float damage = MasteryScaling.scaledDamage(
         Dmg.weaponBonus(level, player.getMainHandItem(), primary, src, (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE)), player, "ReaperScytheReap"
      );
      AABB box = player.getBoundingBox().inflate(4.0);

      for (LivingEntity victim : level.getEntitiesOfClass(
         LivingEntity.class, box, e -> e != player && e != primary && e.isAlive() && e.attackable() && !e.isAlliedTo(player) && Harm.allowed(player, e)
      )) {
         Vec3 to = victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0).subtract(eye);
         double dist = to.length();
         if (!(dist > 4.0) && !(dist < 0.001) && !(to.scale(1.0 / dist).dot(look) < cosHalf)) {
            boolean hit = Dmg.weaponHurt(victim, level, src, damage);
            if (hit) {
               victim.knockback(0.4, eye.x - victim.getX(), eye.z - victim.getZ());
               Fx.burst(level, ParticleTypes.CRIT, victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0), 6, 0.3, 0.1);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onTick(Post event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         UUID var7 = player.getUUID();
         boolean holding = player.getMainHandItem().getItem() instanceof ReaperScytheItem && !BetterCombatCompat.ownsSwing();
         long now = player.serverLevel().getGameTime();
         boolean reaping = REAP_UNTIL.getOrDefault(var7, 0L) > now;
         if (holding && !reaping) {
            if (POSED.add(var7)) {
               PowerService.playAnimation(player, "reaper_scythe_grip");
            }
         } else if (!holding) {
            if (POSED.remove(var7)) {
               PowerService.stopAnimation(player);
            }

            REAP_UNTIL.remove(var7);
         }
      }
   }

   @SubscribeEvent
   public static void onLogout(PlayerLoggedOutEvent event) {
      clear(event.getEntity().getUUID());
   }

   @SubscribeEvent
   public static void onClone(Clone event) {
      clear(event.getEntity().getUUID());
   }

   private static void clear(UUID id) {
      POSED.remove(id);
      REAP_UNTIL.remove(id);
   }
}
