package com.devilfruits.fruits.kage_kage.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.combat.Stun;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.kage_kage.KageAim;
import com.devilfruits.fruits.kage_kage.KageFx;
import com.devilfruits.fruits.kage_kage.KageKage;
import com.devilfruits.fruits.kage_kage.KageTiming;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.common.Tags.EntityTypes;

public class ShadowTrapAbility extends Ability {
   public ShadowTrapAbility() {
      super(
         KageKage.id("shadow_trap"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(30)
            .cooldown(320)
            .mastery(4)
            .damage(18.0)
            .range(24.0)
            .knockback(0.9)
            .extra("radius", 2.5, 1.0, 8.0, "With nothing aimed at, the casket takes the nearest creature within this radius of the spot.")
            .extra("holdSeconds", 2.5, 0.5, 10.0, "Seconds the swallowed stay shut in the casket.")
            .extra("pulses", 3.0, 1.0, 10.0, "Hits while it is shut in (the damage is shared between them).")
            .extra("bossHoldFactor", 0.4, 0.0, 1.0, "Bosses are not swallowed: they are held beside it for this fraction of the time.")
            .extra("aimConeDegrees", 6.0, 0.0, 45.0, "Aim assist: a creature this close to the crosshair is the one taken.")
            .build()
      );
   }

   @Override
   public boolean coatable() {
      return true;
   }

   @Nullable
   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      return KageAim.groundSpot(player, this.config().range(), this.config().extra("aimConeDegrees")) == null
         ? Component.translatable("message.devilfruits.kage_kage.no_ground")
         : null;
   }

   public static List<LivingEntity> inPool(ServerPlayer user, Vec3 centre, double radius) {
      AABB box = new AABB(centre, centre).inflate(radius, 2.0, radius);
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : user.level().getEntitiesOfClass(LivingEntity.class, box, ex -> ex.isAlive() && Targeting.canHurt(user, ex))) {
         double dx = e.getX() - centre.x;
         double dz = e.getZ() - centre.z;
         if (dx * dx + dz * dz <= radius * radius) {
            out.add(e);
         }
      }

      return out;
   }

   public static Vec3 casketSpot(ServerPlayer user, Vec3 spot) {
      Vec3 away = spot.subtract(user.position()).multiply(1.0, 0.0, 1.0);
      away = away.lengthSqr() < 1.0E-4 ? user.getLookAngle().multiply(1.0, 0.0, 1.0).normalize() : away.normalize();
      Vec3 side = new Vec3(-away.z, 0.0, away.x);

      for (Vec3 off : new Vec3[]{away.scale(1.5), side.scale(1.5), side.scale(-1.5), away.scale(-1.5)}) {
         Vec3 at = spot.add(off);
         HitResult floor = user.level().clip(new ClipContext(at.add(0.0, 1.0, 0.0), at.add(0.0, -2.5, 0.0), Block.COLLIDER, Fluid.NONE, user));
         if (floor.getType() != Type.MISS) {
            Vec3 f = floor.getLocation();
            if (user.level().noCollision(new AABB(f.x - 0.77, f.y + 0.05, f.z - 0.77, f.x + 0.77, f.y + 3.3, f.z + 0.77))) {
               return f;
            }
         }
      }

      return spot;
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      Vec3 spot = KageAim.groundSpot(player, (double)ctx.range(), ctx.extra("aimConeDegrees"));
      if (spot != null) {
         LivingEntity aimed = KageAim.aimed(player, (double)ctx.range(), ctx.extra("aimConeDegrees"));
         Vec3 casket = casketSpot(player, spot);
         Vec3 toUser = player.position().subtract(casket).multiply(1.0, 0.0, 1.0);
         Vec3 front = toUser.lengthSqr() < 1.0E-4 ? player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(-1.0) : toUser.normalize();
         float yaw = (float)Mth.atan2(-front.x, -front.z);
         int run = KageTiming.batRun(Math.sqrt(player.distanceToSqr(casket)));
         int hold = (int)Math.round(ctx.extra("holdSeconds") * 20.0);
         ServerLevel level = ctx.level();
         ServerFx.send(player, KageFx.TRAP, casket, new Vec3((double)run, (double)hold, (double)yaw), 1.0F, ctx.seed(), aimed != null ? aimed.getId() : -1);
         ServerFx.animate(player, ctx.animation(KageKage.id("palm_press")));
         if (aimed != null) {
            aimed.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, KageTiming.trapPull(run) + 4, 3, false, false, false));
         }

         ServerScheduler.later(level, KageTiming.trapPull(run), () -> swallow(ctx, aimed, spot, casket, front, hold));
      }
   }

   @Nullable
   private static LivingEntity catchOf(AbilityContext ctx, @Nullable LivingEntity aimed, Vec3 spot, Vec3 casket) {
      ServerPlayer player = ctx.player();
      return aimed != null
            && aimed.isAlive()
            && !aimed.isRemoved()
            && aimed.level() == player.level()
            && aimed.distanceToSqr(casket) < 20.25
            && Targeting.canHurt(player, aimed)
         ? aimed
         : inPool(player, spot, ctx.extra("radius")).stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(spot))).orElse(null);
   }

   private static void swallow(AbilityContext ctx, @Nullable LivingEntity aimed, Vec3 spot, Vec3 casket, Vec3 front, int hold) {
      ServerPlayer player = ctx.player();
      if (!player.isRemoved()) {
         LivingEntity victim = catchOf(ctx, aimed, spot, casket);
         if (victim != null) {
            int pulses = Math.max(1, (int)ctx.extra("pulses"));
            float each = ctx.damage() / (float)pulses;
            boolean boss = victim.getType().is(EntityTypes.BOSSES);
            int shutAfter = 9;
            int spitAfter = shutAfter + hold + 7;
            if (boss) {
               Stun.apply(victim, (int)Math.round((double)hold * ctx.extra("bossHoldFactor")) + shutAfter);
            } else {
               Stun.apply(victim, spitAfter + 2);
               ServerFx.send(victim, KageFx.TRAP_SWALLOW, casket, Vec3.ZERO, 1.0F, ctx.seed(), hold);
            }

            Vec3 start = victim.position();
            ServerScheduler.schedule(ctx.level(), 0, age -> {
               if (victim.isAlive() && !victim.isRemoved() && victim.level() == player.level()) {
                  if (!boss && age <= spitAfter) {
                     float k = Mth.clamp((float)age / 6.0F, 0.0F, 1.0F);
                     Vec3 at = start.lerp(casket, (double)(k * k * (3.0F - 2.0F * k)));
                     if (victim.position().distanceToSqr(at) > 0.0025) {
                        victim.teleportTo(at.x, at.y, at.z);
                     }

                     victim.setDeltaMovement(Vec3.ZERO);
                  }

                  int inside = age - shutAfter;
                  if (inside >= 0 && inside < hold && inside % Math.max(1, hold / pulses) == 0 && inside / Math.max(1, hold / pulses) < pulses) {
                     victim.invulnerableTime = 0;
                     ctx.hurt(victim, each);
                  }

                  if (age == spitAfter) {
                     Stun.release(victim);
                     if (!boss) {
                        Targeting.push(victim, front.scale((double)ctx.knockback()).add(0.0, 0.35, 0.0));
                     }

                     return true;
                  } else {
                     return false;
                  }
               } else {
                  return true;
               }
            });
         }
      }
   }
}
