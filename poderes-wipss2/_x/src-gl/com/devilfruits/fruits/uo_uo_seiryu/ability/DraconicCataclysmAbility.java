package com.devilfruits.fruits.uo_uo_seiryu.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuFx;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuSounds;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuTriggers;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class DraconicCataclysmAbility extends Ability {
   public static final int LEAP_TICK = 8;
   public static final int SLAM_TICK = 29;
   private static final int MAX_TICKS = 80;

   public DraconicCataclysmAbility() {
      super(
         UoUoSeiryu.id("draconic_cataclysm"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(60)
            .cooldown(400)
            .mastery(5)
            .damage(4.0)
            .range(8.0)
            .knockback(3.0)
            .blockDamage(true)
            .extra("leapHeight", 1.6, 0.2, 4.0, "Upward velocity of the leap.")
            .extra("hangTicks", 10.0, 0.0, 40.0, "Ticks spent spinning at the top of the leap.")
            .extra("craterBlocks", 40.0, 0.0, 256.0, "Blocks torn out of the ground by the impact (they heal).")
            .extra("stunTicks", 30.0, 0.0, 200.0, "How long the impact stuns what it hits (ticks).")
            .extra("clubBonus", 1.25, 1.0, 5.0, "Damage multiplier while holding a club (Hassaikai).")
            .build()
      );
   }

   @Override
   public boolean coatable() {
      return true;
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults())
         .impact(PhysicsProfile.Impact.RADIAL)
         .force(1.6)
         .reaction(PhysicsProfile.Reaction.LANDING)
         .blockDamage(0.6)
         .manual()
         .build();
   }

   @Nullable
   @Override
   public ResourceKey<DamageType> damageType() {
      return UoUoSeiryu.DRAGON_DAMAGE;
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      boolean veiled = HybridCombat.veiled(player);
      AbilityManager.setBusy(player, 35);
      ServerFx.animate(player, UoUoSeiryu.anim("hybrid/draconic_cataclysm"));
      ServerFx.send(player, SeiryuFx.CATACLYSM_LEAP, player.position(), Vec3.ZERO, veiled ? 1.0F : 0.0F, ctx.seed(), 8);
      double leap = ctx.extra("leapHeight");
      int hang = (int)ctx.extra("hangTicks");
      Vec3 flat = ctx.horizontalLook();
      ServerScheduler.schedule(level, 1, age -> {
         if (player.isAlive() && !player.isRemoved() && player.level() == level) {
            player.fallDistance = 0.0F;
            if (age < 8) {
               Targeting.moveSelf(player, new Vec3(0.0, Math.min(0.0, player.getDeltaMovement().y), 0.0));
               return false;
            } else if (age == 8) {
               Targeting.moveSelf(player, flat.scale(0.35).add(0.0, leap, 0.0));
               level.playSound(null, player.getX(), player.getY(), player.getZ(), SeiryuSounds.RAGNARAKU_LEAP.get(), SoundSource.PLAYERS, 1.4F, 0.9F);
               return false;
            } else {
               int hangStart = 29 - hang;
               if (age >= hangStart && age < 29) {
                  Vec3 v = player.getDeltaMovement();
                  Targeting.moveSelf(player, new Vec3(v.x * 0.6, Math.max(-0.02, Math.min(v.y, 0.05)), v.z * 0.6));
                  return false;
               } else if (age == 29) {
                  Targeting.moveSelf(player, flat.scale(0.25).add(0.0, -3.2, 0.0));
                  return false;
               } else if (age <= 29 || !player.onGround() && !player.isInWater() && age < 80) {
                  if (age > 29) {
                     Vec3 v = player.getDeltaMovement();
                     Targeting.moveSelf(player, new Vec3(v.x, Math.min(v.y, -2.4), v.z));
                  }

                  return false;
               } else {
                  this.impact(ctx, veiled);
                  return true;
               }
            }
         } else {
            return true;
         }
      });
   }

   private void impact(AbilityContext ctx, boolean veiled) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      BlockPos ground = TerrainDestruction.findSurface(level, Mth.floor(player.getX()), Mth.floor(player.getY()), Mth.floor(player.getZ()), 1, 4);
      Vec3 at = ground != null ? Vec3.atBottomCenterOf(ground.above()) : player.position();
      float radius = ctx.range() * Math.max(1.0F, player.getScale() * 0.7F);
      float damage = HybridCombat.hit(ctx) * SeiryuCombat.clubFactor(player, ctx.extra("clubBonus"));

      for (Entity e : Targeting.inRadius(level, at, (double)radius, x -> Targeting.canHurt(player, x))) {
         double dist = e.position().distanceTo(at);
         float falloff = (float)Mth.clamp(1.0 - dist / (double)radius * 0.5, 0.5, 1.0);
         if (HybridCombat.strike(ctx, e, damage * falloff)) {
            if (e instanceof LivingEntity living) {
               HybridStun.apply(living, (int)ctx.extra("stunTicks"));
            }

            Vec3 out = e.position().subtract(at);
            Vec3 dir = new Vec3(out.x, 0.0, out.z).lengthSqr() < 1.0E-4 ? ctx.horizontalLook() : new Vec3(out.x, 0.0, out.z).normalize();
            Targeting.launch(e, dir.scale((double)(ctx.knockback() * falloff)).add(0.0, 0.9 * (double)falloff + 0.3, 0.0));
         }
      }

      for (Entity ex : Targeting.inRadius(level, at, (double)radius * 1.6, x -> Targeting.canPush(player, x) && x.position().distanceTo(at) > (double)radius)) {
         Targeting.push(ex, ex.position().subtract(at).normalize().scale(0.8).add(0.0, 0.4, 0.0));
      }

      BlockImpactService.slam(ctx, at, (double)radius, 1.0);
      SeiryuCombat.crater(ctx, at.add(0.0, -0.5, 0.0), (double)radius * 0.6, (int)ctx.extra("craterBlocks"), 0.75);
      ServerFx.send(player, SeiryuFx.CATACLYSM, at, ctx.horizontalLook(), radius, ctx.seed(), veiled ? 1 : 0);
      ServerFx.shake(level, at, 1.2F, 64);
      SeiryuTriggers.USED_RAGNARAKU.get().trigger(player);
   }
}
