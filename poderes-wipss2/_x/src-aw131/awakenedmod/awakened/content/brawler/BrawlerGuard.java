package com.awakenedmod.awakened.content.brawler;

import com.awakenedmod.awakened.content.vfx.VfxBolt;
import com.awakenedmod.awakened.core.DeflectedShots;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.HitKind;
import com.awakenedmod.awakened.core.OwnedProjectile;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.phys.Vec3;

public final class BrawlerGuard {
   static final String ABILITY_ID = "brawler_full_counter";
   private static final String FEAT_PARRY = "brawler_parry";
   private static final String FEAT_PARRY_SHOT = "brawler_parry_shot";
   private static final String FEAT_PARRY_BLAST = "brawler_parry_blast";
   private static final String FEAT_COUNTER_SHAPE = "brawler_counter_shape";
   private static final Map<UUID, BrawlerGuard.Guard> GUARDS = new ConcurrentHashMap<>();
   private static final Map<UUID, BrawlerGuard.Strip> STRIPS = new ConcurrentHashMap<>();

   private BrawlerGuard() {
   }

   static void begin(ServerPlayer player) {
      BrawlerGuard.Guard guard = new BrawlerGuard.Guard();
      guard.until = player.serverLevel().getGameTime() + 12L + 2L;
      GUARDS.put(player.getUUID(), guard);
      PowerService.playAnimation(player, "brawler_full_counter");
      BrawlerFx.counterGuard(player.serverLevel(), player);
   }

   public static boolean up(ServerPlayer player) {
      BrawlerGuard.Guard guard = GUARDS.get(player.getUUID());
      if (guard == null) {
         return false;
      } else if (player.serverLevel().getGameTime() <= guard.until) {
         return true;
      } else {
         end(player);
         return false;
      }
   }

   public static void end(ServerPlayer player) {
      BrawlerGuard.Guard guard = GUARDS.remove(player.getUUID());
      if (guard != null) {
         if (player.serverLevel().getGameTime() >= guard.clipTick + 5L) {
            PowerService.stopAnimation(player);
         }
      }
   }

   public static boolean parriedThisTick(ServerPlayer player) {
      BrawlerGuard.Guard guard = GUARDS.get(player.getUUID());
      return guard != null && guard.parriedTick == player.serverLevel().getGameTime();
   }

   public static boolean onIncomingDamage(ServerPlayer player, DamageSource source, float amount) {
      if (!up(player)) {
         return false;
      } else {
         Entity direct = source.getDirectEntity();
         if (direct != null && DeflectedShots.turnedBy(direct, player.getUUID())) {
            return true;
         } else {
            LivingEntity attacker = responsible(player, source);
            if (attacker == null) {
               return false;
            } else {
               ServerLevel level = player.serverLevel();
               BrawlerGuard.Bucket bucket = bucket(player, source);
               if (bucket == BrawlerGuard.Bucket.PROJECTILE) {
                  if (returnShot(player, direct, attacker)) {
                     answer(player, attacker, amount, BrawlerGuard.Bucket.PROJECTILE);
                     return true;
                  }

                  BrawlerFx.counterNegate(level, direct.position(), shotLine(player, direct));
                  direct.discard();
                  bucket = BrawlerGuard.Bucket.ABILITY;
               }

               if (bucket == BrawlerGuard.Bucket.MELEE) {
                  grant(player, attacker, amount, BrawlerGuard.Bucket.MELEE);
                  Marks.apply(player, attacker, Mark.PINNED, 44, Mark.Flavour.NONE, "brawler_full_counter");
                  reflect(player, level, attacker, amount * 2.0F, meleeCap(attacker));
                  beat(player, attacker, amount, BrawlerGuard.Bucket.MELEE);
                  return true;
               } else {
                  throwBack(player, level, attacker, amount);
                  STRIPS.computeIfAbsent(player.getUUID(), k -> new BrawlerGuard.Strip(player));
                  answer(player, attacker, amount, BrawlerGuard.Bucket.ABILITY);
                  return true;
               }
            }
         }
      }
   }

   public static boolean onProjectileImpact(ServerPlayer player, Projectile shot) {
      if (up(player) && shot != null && shot.getOwner() != player) {
         if (shot.getOwner() instanceof LivingEntity shooter && Harm.allowed(player, shooter)) {
            float amount = shotValue(shot);
            BrawlerGuard.Bucket bucket = BrawlerGuard.Bucket.PROJECTILE;
            if (!returnShot(player, shot, shooter)) {
               BrawlerFx.counterNegate(player.serverLevel(), shot.position(), shotLine(player, shot));
               shot.discard();
               throwBack(player, player.serverLevel(), shooter, amount);
               STRIPS.computeIfAbsent(player.getUUID(), k -> new BrawlerGuard.Strip(player));
               bucket = BrawlerGuard.Bucket.ABILITY;
            }

            answer(player, shooter, amount, bucket);
            return true;
         }

         return false;
      } else {
         return false;
      }
   }

   private static float shotValue(Entity shot) {
      if (shot instanceof OwnedProjectile owned) {
         return Math.max(0.0F, owned.shotDamage());
      } else if (shot instanceof AbstractArrow) {
         double speed = shot.getDeltaMovement().length();
         return (float)Math.ceil(speed * 2.0);
      } else {
         return shot instanceof ThrowableProjectile ? 0.0F : 6.0F;
      }
   }

   private static LivingEntity responsible(ServerPlayer player, DamageSource source) {
      return HitKind.responsible(player, source);
   }

   private static BrawlerGuard.Bucket bucket(ServerPlayer player, DamageSource source) {
      return switch (HitKind.shape(source)) {
         case PROJECTILE -> BrawlerGuard.Bucket.PROJECTILE;
         case BODY -> BrawlerGuard.Bucket.MELEE;
         case FIST -> HitKind.inReach(player, source.getEntity(), 5.5) ? BrawlerGuard.Bucket.MELEE : BrawlerGuard.Bucket.ABILITY;
         case EXPLOSION, OTHER -> BrawlerGuard.Bucket.ABILITY;
      };
   }

   private static boolean returnShot(ServerPlayer player, Entity shot, LivingEntity shooter) {
      if (shot != null && shot.isAlive() && !shot.isRemoved() && !DeflectedShots.turned(shot)) {
         Vec3 back = shooter == null ? player.getLookAngle() : shooter.getEyePosition().subtract(player.getEyePosition()).normalize();
         Vec3 met = shot.position();
         double speed = Math.max(shot.getDeltaMovement().length(), 1.2) * 1.5;
         if (!turn(player, shot, back, speed)) {
            return false;
         } else {
            DeflectedShots.mark(shot, player.getUUID(), 200);
            BrawlerFx.counterReturn(player.serverLevel(), met, back);
            return true;
         }
      } else {
         return false;
      }
   }

   private static boolean turn(ServerPlayer player, Entity shot, Vec3 back, double speed) {
      if (shot instanceof Projectile projectile) {
         if (!(shot instanceof AbstractArrow) && !(shot instanceof ThrowableProjectile)) {
            return false;
         } else {
            projectile.setPos(player.getEyePosition().add(back));
            projectile.setDeltaMovement(back.scale(speed));
            if (!(shot instanceof ThrownTrident)) {
               projectile.setOwner(player);
            }

            projectile.hurtMarked = true;
            return true;
         }
      } else {
         if (shot instanceof OwnedProjectile owned && owned.sendBack(back, speed, player)) {
            return true;
         }

         return false;
      }
   }

   private static Vec3 shotLine(ServerPlayer player, Entity shot) {
      Vec3 v = shot.getDeltaMovement();
      return v.lengthSqr() > 1.0E-4 ? v.normalize() : player.getLookAngle().scale(-1.0);
   }

   private static void reflect(ServerPlayer player, ServerLevel level, LivingEntity attacker, float raw, float cap) {
      float damage = charge(player, raw, cap);
      if (damage > 0.0F) {
         land(player, level, attacker, damage);
      }
   }

   private static void throwBack(ServerPlayer player, ServerLevel level, LivingEntity attacker, float amount) {
      float damage = charge(player, amount * 0.5F, abilityCap(attacker));
      if (!(damage <= 0.0F)) {
         Vec3 from = BrawlerFx.guardPoint(player);
         VfxBolt.fire(level, from, VfxBolt.chestOf(attacker, level, from, 48.0), 7, BrawlerFx.RETURN_BOLT, at -> land(player, level, attacker, damage));
      }
   }

   private static float charge(ServerPlayer player, float raw, float cap) {
      BrawlerGuard.Guard guard = GUARDS.get(player.getUUID());
      float budget = guard == null ? cap : Math.max(0.0F, cap - guard.returned);
      float damage = Math.min(budget, Math.max(0.0F, raw));
      if (damage > 0.0F && guard != null) {
         guard.returned += damage;
      }

      return damage;
   }

   private static void land(ServerPlayer player, ServerLevel level, LivingEntity attacker, float damage) {
      Dmg.hurtThrough(
         attacker, level, player.damageSources().thorns(player), MasteryScaling.configScaledDamage(damage, FullCounterAbility.class.getSimpleName())
      );
      BrawlerFx.counterReflect(level, attacker);
   }

   private static float meleeCap(LivingEntity attacker) {
      if (attacker instanceof Player) {
         return 12.0F;
      } else {
         return BrawlerTargets.unliftable(attacker) ? 20.0F : 30.0F;
      }
   }

   private static float abilityCap(LivingEntity attacker) {
      return attacker instanceof Player ? 8.0F : 20.0F;
   }

   private static void answer(ServerPlayer player, LivingEntity attacker, float amount, BrawlerGuard.Bucket bucket) {
      grant(player, attacker, amount, bucket);
      beat(player, attacker, amount, bucket);
   }

   private static void grant(ServerPlayer player, LivingEntity attacker, float amount, BrawlerGuard.Bucket bucket) {
      if (!(amount <= 0.0F)) {
         BrawlerGuard.Guard guard = GUARDS.get(player.getUUID());
         if (guard != null && !guard.paid) {
            guard.paid = true;
            boolean transition = Marks.consumeTransition(player, attacker, "brawler_full_counter");
            BrawlerCombo.add(player, transition ? 3 : 2);
            QuestSystem.feat(player, "brawler_parry");
            if (bucket == BrawlerGuard.Bucket.PROJECTILE) {
               QuestSystem.feat(player, "brawler_parry_shot");
            } else if (bucket == BrawlerGuard.Bucket.ABILITY) {
               QuestSystem.feat(player, "brawler_parry_blast");
            }

            if (transition) {
               QuestSystem.feat(player, "brawler_counter_shape");
            }
         }
      }
   }

   private static void beat(ServerPlayer player, LivingEntity attacker, float amount, BrawlerGuard.Bucket bucket) {
      BrawlerHitStop.apply(attacker, 2);
      BrawlerGuard.Guard guard = GUARDS.get(player.getUUID());
      if (guard != null) {
         guard.parriedTick = player.serverLevel().getGameTime();
      }

      if (beatAllowed(player)) {
         BrawlerFx.parryBeat(player.serverLevel(), player, attacker, amount, bucket == BrawlerGuard.Bucket.ABILITY);
         PowerService.playAnimation(player, "brawler_full_counter_success");
         if (guard != null) {
            guard.clipTick = player.serverLevel().getGameTime();
         }
      }
   }

   private static boolean beatAllowed(ServerPlayer player) {
      BrawlerGuard.Guard guard = GUARDS.get(player.getUUID());
      if (guard == null) {
         return true;
      } else {
         long now = player.serverLevel().getGameTime();
         if (guard.beatTick != now) {
            guard.beatTick = now;
            guard.beatsThisTick = 0;
         }

         return ++guard.beatsThisTick <= 2;
      }
   }

   public static void tickPost(MinecraftServer server) {
      if (!STRIPS.isEmpty()) {
         for (BrawlerGuard.Strip strip : STRIPS.values()) {
            revert(strip);
         }

         STRIPS.clear();
      }
   }

   private static void revert(BrawlerGuard.Strip strip) {
      ServerPlayer player = strip.player;
      if (player.isAlive() && !player.isRemoved()) {
         if (player.getRemainingFireTicks() > strip.fireTicks) {
            player.setRemainingFireTicks(strip.fireTicks);
         }

         for (MobEffectInstance now : List.copyOf(player.getActiveEffects())) {
            if (!((MobEffect)now.getEffect().value()).isBeneficial()) {
               MobEffectInstance before = strip.effects.get(now.getEffect());
               if (before == null) {
                  player.removeEffect(now.getEffect());
               } else if (now.getAmplifier() > before.getAmplifier() || now.getDuration() > before.getDuration()) {
                  player.removeEffect(now.getEffect());
                  player.addEffect(new MobEffectInstance(before));
               }
            }
         }

         revertPush(player, strip.motion);
      }
   }

   private static void revertPush(ServerPlayer player, Vec3 before) {
      Vec3 now = player.getDeltaMovement();
      Vec3 diff = now.subtract(before);
      double up = Math.max(0.0, diff.y);
      double shove = Math.sqrt(diff.x * diff.x + diff.z * diff.z + up * up);
      if (!(shove <= 0.15)) {
         player.setDeltaMovement(now.x - diff.x, now.y - up, now.z - diff.z);
         player.hurtMarked = true;
      }
   }

   public static void clearPlayer(ServerPlayer player) {
      end(player);
      STRIPS.remove(player.getUUID());
   }

   public static void clearAll() {
      GUARDS.clear();
      DeflectedShots.clearAll();
      STRIPS.clear();
   }

   public static enum Bucket {
      PROJECTILE,
      MELEE,
      ABILITY;
   }

   private static final class Guard {
      long until;
      boolean paid;
      long beatTick = Long.MIN_VALUE;
      int beatsThisTick;
      float returned;
      long parriedTick = Long.MIN_VALUE;
      long clipTick = Long.MIN_VALUE;
   }

   private static final class Strip {
      final ServerPlayer player;
      final int fireTicks;
      final Map<Holder<MobEffect>, MobEffectInstance> effects = new HashMap<>();
      final Vec3 motion;

      Strip(ServerPlayer player) {
         this.player = player;
         this.fireTicks = player.getRemainingFireTicks();
         this.motion = player.getDeltaMovement();

         for (MobEffectInstance instance : player.getActiveEffects()) {
            this.effects.put(instance.getEffect(), new MobEffectInstance(instance));
         }
      }
   }
}
