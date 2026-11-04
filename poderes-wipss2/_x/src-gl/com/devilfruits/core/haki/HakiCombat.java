package com.devilfruits.core.haki;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.logia.DamageCategory;
import com.devilfruits.core.logia.LogiaBypass;
import com.devilfruits.core.logia.LogiaBypassSource;
import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.registry.DFDamageTypes;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class HakiCombat {
   private static final ThreadLocal<Player> COATED_TECHNIQUE = new ThreadLocal<>();
   private static final ThreadLocal<Player> RYUO = new ThreadLocal<>();

   public static void init() {
      LogiaBypass.register((victim, source) -> {
         if (!(source.getEntity() instanceof Player attacker) || attacker == victim) {
            return null;
         }

         if (ryuo(source)) {
            return new LogiaBypass.Result(LogiaBypassSource.HAKI, 1.0F);
         } else {
            return coatedStrike(source) ? new LogiaBypass.Result(LogiaBypassSource.HAKI, HakiManager.logiaFraction(HakiManager.level(attacker))) : null;
         }
      });
   }

   public static boolean coatedHand(Player player) {
      int mask = HakiManager.mask(player);
      if (mask == 0) {
         return false;
      } else {
         return player.getMainHandItem().isEmpty() ? HakiParts.fist(mask, player.getMainArm() == HumanoidArm.RIGHT) : (mask & 256) != 0;
      }
   }

   public static boolean techniqueHurt(Player player, BooleanSupplier hurt) {
      Player before = COATED_TECHNIQUE.get();
      COATED_TECHNIQUE.set(player);

      boolean var3;
      try {
         var3 = hurt.getAsBoolean();
      } finally {
         COATED_TECHNIQUE.set(before);
      }

      return var3;
   }

   public static boolean coatsTechnique(Player player) {
      int mask = HakiManager.mask(player);
      return (mask & 319) != 0;
   }

   public static boolean coatedStrike(DamageSource source) {
      if (source.getEntity() instanceof Player attacker) {
         if (RYUO.get() == attacker) {
            return true;
         } else {
            int mask = HakiManager.mask(attacker);
            if (mask == 0) {
               return false;
            } else if (COATED_TECHNIQUE.get() == attacker) {
               return coatsTechnique(attacker);
            } else if (source.getDirectEntity() == attacker && source.is(DamageTypes.PLAYER_ATTACK)) {
               ItemStack weapon = attacker.getMainHandItem();
               return weapon.isEmpty() ? HakiParts.fist(mask, attacker.getMainArm() == HumanoidArm.RIGHT) : (mask & 256) != 0;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public static boolean ryuo(DamageSource source) {
      if (source.is(DFDamageTypes.HAKI_RYUO)) {
         return true;
      } else {
         if (source.getEntity() instanceof Player p && RYUO.get() == p) {
            return true;
         }

         return false;
      }
   }

   public static void onIncomingDamage(LivingIncomingDamageEvent event) {
      LivingEntity victim = event.getEntity();
      DamageSource source = event.getSource();
      if (victim.level() instanceof ServerLevel level && DamageCategory.of(source) != DamageCategory.ALWAYS) {
         float amount = event.getAmount();
         Player attacker = source.getEntity() instanceof Player p ? p : null;
         boolean coated = attacker != null && coatedStrike(source);
         if (coated && COATED_TECHNIQUE.get() != attacker && !source.is(DFDamageTypes.HAKI_RYUO)) {
            amount *= 1.0F + ((Double)HakiConfig.ATTACK_PER_LEVEL.get()).floatValue() * (float)HakiManager.level(attacker);
         }

         int defended = victim instanceof Player vp ? HakiManager.mask(vp) : 0;
         if (coated && defended != 0 && !source.is(DFDamageTypes.HAKI_RYUO)) {
            amount = clash(level, attacker, (Player)victim, amount);
         }

         if (defended != 0) {
            int part = hitPart(victim, source);
            float covered = part < 0 ? HakiParts.coverage(defended) / HakiParts.coverage(255) : partCovered(defended, part);
            float reduction = ((Double)HakiConfig.DEFENSE_PER_LEVEL.get()).floatValue() * (float)HakiManager.level((Player)victim) * covered;
            if (attacker != null && attacker != victim && LogiaManager.isLogia(attacker) && !source.is(DamageTypes.PLAYER_ATTACK)) {
               reduction += ((Double)HakiConfig.LOGIA_ELEMENT_REDUCTION.get()).floatValue() * covered;
            }

            amount *= 1.0F - Math.min(0.8F, reduction);
         }

         if (coated) {
            Vec3 at = contact(attacker, victim);
            Vec3 dir = victim.getBoundingBox().getCenter().subtract(attacker.getEyePosition()).normalize();
            ServerFx.sendNear(level, HakiFx.IMPACT, attacker.getId(), at, dir, amount, 0L, victim.getId(), 48.0);
         }

         event.setAmount(amount);
         return;
      }
   }

   private static float clash(ServerLevel level, Player attacker, Player victim, float amount) {
      PlayerHakiData a = HakiManager.data(attacker);
      PlayerHakiData d = HakiManager.data(victim);
      HakiClash.Result result = HakiClash.resolve(
         a.level(),
         HakiParts.coverage(a.mask()),
         RYUO.get() == attacker,
         d.level(),
         HakiParts.coverage(d.mask()),
         ((Double)HakiConfig.CLASH_JITTER.get()).floatValue(),
         attacker.getRandom()
      );
      Player loser = result.attackerWins() ? victim : attacker;
      Player winner = result.attackerWins() ? attacker : victim;
      Vec3 push = loser.position().subtract(winner.position()).multiply(1.0, 0.0, 1.0);
      push = push.lengthSqr() < 1.0E-4 ? Vec3.ZERO : push.normalize();
      Targeting.launch(loser, push.scale(0.55).add(0.0, 0.25, 0.0));
      loser.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 1, false, false));
      if (loser instanceof ServerPlayer sp) {
         ServerFx.animate(sp, DevilFruits.id("haki/clash_recoil"));
      }

      long now = level.getGameTime();
      long last = a.lastClash.getOrDefault(victim.getId(), -100L);
      if (now - last > 6L) {
         a.lastClash.put(victim.getId(), now);
         ServerFx.sendNear(
            level,
            HakiFx.CLASH,
            attacker.getId(),
            contact(attacker, victim),
            victim.getEyePosition().subtract(attacker.getEyePosition()),
            result.strength(),
            attacker.getRandom().nextLong(),
            loser.getId(),
            64.0
         );
         HakiStructures.clash(level, attacker, victim, result.strength());
      }

      return result.attackerWins()
         ? amount * (1.0F + ((Double)HakiConfig.CLASH_BONUS.get()).floatValue())
         : amount * ((Double)HakiConfig.CLASH_PENALTY.get()).floatValue();
   }

   static Vec3 contact(Entity attacker, LivingEntity victim) {
      Vec3 eye = attacker.getEyePosition();
      Optional<Vec3> hit = victim.getBoundingBox().clip(eye, eye.add(attacker.getLookAngle().scale(8.0)));
      return hit.orElseGet(() -> victim.getBoundingBox().getCenter().lerp(eye, 0.3));
   }

   static int hitPart(LivingEntity victim, DamageSource source) {
      Vec3 point;
      label50: {
         Entity direct = source.getDirectEntity();
         if (direct instanceof LivingEntity attacker && direct != victim) {
            point = contact(attacker, victim);
            break label50;
         }

         if (direct == null || direct == victim) {
            return -1;
         }

         point = direct.position();
      }

      double h = (point.y - victim.getY()) / Math.max(0.1, (double)victim.getBbHeight());
      float yaw = victim.yBodyRot * (float) (Math.PI / 180.0);
      double side = (point.x - victim.getX()) * (double)(-Mth.cos(yaw)) + (point.z - victim.getZ()) * (double)(-Mth.sin(yaw));
      boolean right = side > 0.0;
      if (h > 0.78) {
         return 128;
      } else if (h > 0.42) {
         return Math.abs(side) > (double)victim.getBbWidth() * 0.32 ? (right ? 4 : 8) : 64;
      } else {
         return right ? 16 : 32;
      }
   }

   static float partCovered(int mask, int part) {
      if ((mask & part) != 0) {
         return 1.0F;
      } else {
         return (part != 4 || (mask & 1) == 0) && (part != 8 || (mask & 2) == 0) ? 0.0F : 0.35F;
      }
   }

   static void ryuo(ServerPlayer player, PlayerHakiData data, float charge) {
      ServerLevel level = player.serverLevel();
      ServerFx.animate(player, DevilFruits.id("haki/ryuo_strike"));
      LivingEntity target = target(player);
      Vec3 look = player.getLookAngle();
      Vec3 at = target != null ? contact(player, target) : player.getEyePosition().add(look.scale(2.2));
      ServerFx.sendNear(level, HakiFx.RYUO, player.getId(), at, look, charge, player.getRandom().nextLong(), target == null ? -1 : target.getId(), 64.0);
      HakiStructures.ryuo(player, charge, target != null && Targeting.canHurt(player, target) ? target : null);
      if (target != null && Targeting.canHurt(player, target)) {
         float base = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE)
            * (1.0F + (((Double)HakiConfig.RYUO_MULTIPLIER.get()).floatValue() - 1.0F) * charge);
         float internal = base
            * (
               data.tier() == HakiTier.MASTER
                  ? ((Double)HakiConfig.RYUO_INTERNAL_MASTER.get()).floatValue()
                  : ((Double)HakiConfig.RYUO_INTERNAL.get()).floatValue()
            );
         Player before = RYUO.get();
         RYUO.set(player);

         try {
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().playerAttack(player), base);
            target.invulnerableTime = 0;
            target.hurt(DFDamageTypes.source(level, DFDamageTypes.HAKI_RYUO, player), internal);
         } finally {
            RYUO.set(before);
         }

         Targeting.launch(target, look.multiply(1.0, 0.0, 1.0).normalize().scale(0.7 + 0.7 * (double)charge).add(0.0, 0.35, 0.0));
         ServerFx.shake(level, at, 0.35F + 0.35F * charge, 20);
      }
   }

   @Nullable
   static LivingEntity target(ServerPlayer player) {
      double reach = player.entityInteractionRange() + 0.5;
      Vec3 eye = player.getEyePosition();
      Vec3 end = eye.add(player.getLookAngle().scale(reach));
      AABB box = player.getBoundingBox().expandTowards(player.getLookAngle().scale(reach)).inflate(1.0);
      EntityHitResult hit = ProjectileUtil.getEntityHitResult(
         player, eye, end, box, e -> e instanceof LivingEntity && e != player && !e.isSpectator() && e.isPickable(), reach * reach
      );
      return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
   }

   private HakiCombat() {
   }
}
