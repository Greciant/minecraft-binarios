package com.awakenedmod.awakened.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class ForceBlast {
   public static final double MAX_PUSH = 2.2;

   private ForceBlast() {
   }

   public static List<LivingEntity> shockwave(ServerPlayer player, ServerLevel level, Vec3 center, double radius, float damage, double knockOut, double knockUp) {
      return shockwave(player, level, List.of(center), radius, damage, knockOut, knockUp, 2.2);
   }

   public static List<LivingEntity> shockwave(
      ServerPlayer player, ServerLevel level, Vec3 center, double radius, float damage, double knockOut, double knockUp, double maxPush
   ) {
      return shockwave(player, level, List.of(center), radius, damage, knockOut, knockUp, maxPush);
   }

   public static List<LivingEntity> shockwave(
      ServerPlayer player, ServerLevel level, List<Vec3> centers, double radius, float damagePerCenter, double knockOut, double knockUp
   ) {
      return shockwave(player, level, centers, radius, damagePerCenter, knockOut, knockUp, 2.2);
   }

   public static List<LivingEntity> shockwave(
      ServerPlayer player, ServerLevel level, List<Vec3> centers, double radius, float damagePerCenter, double knockOut, double knockUp, double maxPush
   ) {
      return shockwave(player, level, centers, radius, damagePerCenter, knockOut, knockUp, maxPush, Set.of(), false);
   }

   public static List<LivingEntity> shockwave(
      ServerPlayer player,
      ServerLevel level,
      Vec3 center,
      double radius,
      float damage,
      double knockOut,
      double knockUp,
      double maxPush,
      @Nullable LivingEntity spare,
      boolean replace
   ) {
      return shockwave(player, level, List.of(center), radius, damage, knockOut, knockUp, maxPush, spare == null ? Set.of() : Set.of(spare), replace);
   }

   public static List<LivingEntity> shockwave(
      ServerPlayer player,
      ServerLevel level,
      Vec3 center,
      double radius,
      float damage,
      double knockOut,
      double knockUp,
      double maxPush,
      Collection<? extends LivingEntity> spare,
      boolean replace
   ) {
      return shockwave(player, level, List.of(center), radius, damage, knockOut, knockUp, maxPush, spare, replace);
   }

   private static List<LivingEntity> shockwave(
      ServerPlayer player,
      ServerLevel level,
      List<Vec3> centers,
      double radius,
      float damagePerCenter,
      double knockOut,
      double knockUp,
      double maxPush,
      Collection<? extends LivingEntity> spare,
      boolean replace
   ) {
      Map<LivingEntity, Float> damage = new LinkedHashMap<>();
      Map<LivingEntity, Vec3> push = new HashMap<>();

      for (Vec3 center : centers) {
         for (LivingEntity target : Harm.only(player, AimUtil.livingAround(player, center, radius))) {
            if (!spare.contains(target) && !Foes.fightsFor(player, target)) {
               Vec3 away = target.position().subtract(center);
               Vec3 flat = new Vec3(away.x, 0.0, away.z);
               Vec3 dir = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
               double prox = 1.0 - Math.min(1.0, Math.sqrt(flat.lengthSqr()) / radius);
               double out = knockOut * (0.4 + 0.6 * prox);
               double up = knockUp * (0.5 + 0.5 * prox);
               damage.merge(target, damagePerCenter, Float::sum);
               push.merge(target, new Vec3(dir.x * out, up, dir.z * out), Vec3::add);
            }
         }
      }

      List<LivingEntity> caught = new ArrayList<>(damage.size());

      for (Entry<LivingEntity, Float> entry : damage.entrySet()) {
         LivingEntity targetx = entry.getKey();
         if (entry.getValue() > 0.0F) {
            Dmg.hurtNoKnockback(targetx, level, player.damageSources().playerAttack(player), entry.getValue());
         }

         Vec3 p = clamp(push.get(targetx), maxPush);
         if (replace) {
            targetx.setOnGround(false);
            Harm.setMotion(player, targetx, p);
         } else {
            Harm.push(player, targetx, p.x, p.y, p.z);
         }

         caught.add(targetx);
      }

      return caught;
   }

   private static Vec3 clamp(Vec3 push, double max) {
      double len = push.length();
      return len > max ? push.scale(max / len) : push;
   }
}
