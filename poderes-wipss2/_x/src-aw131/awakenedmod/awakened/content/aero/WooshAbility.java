package com.awakenedmod.awakened.content.aero;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class WooshAbility extends Ability {
   private static final float COST = 8.0F;
   private static final double RANGE = 6.0;
   private static final double HALF_ANGLE_DEG = 30.0;
   private static final double FRONT_DOT = Math.cos(Math.toRadians(30.0));
   private static final float BASE_DAMAGE = 2.0F;
   private static final double KNOCKBACK = 2.8;
   private static final double KNOCKUP = 0.5;
   private static final double POINT_BLANK = 1.5;
   private static final double DEFLECT_SPEED = 1.1;

   public WooshAbility() {
      super("aero_woosh", 8.0F, 0);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(2.0).knockback(2.8).build();
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return "aero_shove";
   }

   @Override
   public boolean activate(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 origin = AimUtil.castOrigin(player);
      Vec3 apex = player.getEyePosition();
      Vec3 dir = player.getLookAngle();
      float damage = MasteryScaling.scaledDamage(2.0F, player, this.balanceKey());
      double kb = MasteryScaling.scaled(2.8, player);
      List<LivingEntity> displaced = this.shove(player, level, apex, dir, damage, kb);
      List<LivingEntity> shooters = this.deflectProjectiles(player, level, apex, dir);
      AeroVfx.gustCone(level, origin, dir, (float)(kb / 2.8));
      List<LivingEntity> shovedFoes = displaced.stream().filter(e -> Foes.counts(player, e)).toList();
      List<LivingEntity> credited = new ArrayList<>(shooters);
      if (shovedFoes.size() >= 2) {
         credited.addAll(shovedFoes);
      }

      MasterySystem.award(player, MasteryReason.HIT, credited);
      if (shooters.stream().anyMatch(shooter -> Foes.counts(player, shooter))) {
         QuestSystem.feat(player, "aero_deflect");
      }

      return true;
   }

   private List<LivingEntity> shove(ServerPlayer player, ServerLevel level, Vec3 origin, Vec3 dir, float damage, double kb) {
      List<LivingEntity> hits = new ArrayList<>();

      for (LivingEntity victim : Harm.only(player, AimUtil.livingAround(player, origin, 6.0))) {
         Vec3 to = victim.getBoundingBox().getCenter().subtract(origin);
         double dist = to.length();
         if (!(dist < 0.01)) {
            Vec3 toN = to.scale(1.0 / dist);
            if (!(toN.dot(dir) < (dist <= 1.5 ? 0.0 : FRONT_DOT))) {
               Dmg.hurt(victim, level, Dmg.by(player, DamageTypes.PLAYER_ATTACK), damage);
               victim.push(toN.x * kb, toN.y * kb + 0.5 * (1.0 - Math.abs(dir.y)), toN.z * kb);
               victim.hurtMarked = true;
               hits.add(victim);
            }
         }
      }

      return hits;
   }

   private List<LivingEntity> deflectProjectiles(ServerPlayer player, ServerLevel level, Vec3 origin, Vec3 dir) {
      List<LivingEntity> shooters = new ArrayList<>();

      for (Projectile proj : level.getEntitiesOfClass(
         Projectile.class, new AABB(origin, origin).inflate(6.0), p -> p.isAlive() && p.getOwner() != player && p.getDeltaMovement().lengthSqr() > 1.0E-4
      )) {
         Vec3 to = proj.position().subtract(origin);
         double dist = to.length();
         if (!(dist < 0.01)) {
            Vec3 toN = to.scale(1.0 / dist);
            if (!(toN.dot(dir) < (dist <= 1.5 ? 0.0 : FRONT_DOT))) {
               RandomSource rand = level.random;
               Vec3 scatter = new Vec3(rand.nextGaussian() * 0.4, rand.nextGaussian() * 0.25, rand.nextGaussian() * 0.4);
               proj.setDeltaMovement(toN.scale(1.1).add(scatter));
               proj.hurtMarked = true;
               Entity var15 = proj.getOwner();
               if (var15 instanceof LivingEntity) {
                  LivingEntity owner = (LivingEntity)var15;
                  if (Harm.allowed(player, owner)) {
                     shooters.add(owner);
                  }
               }
            }
         }
      }

      return shooters;
   }
}
