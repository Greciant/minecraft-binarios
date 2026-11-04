package com.devilfruits.fruits.bomu_bomu.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.fx.ExplosionProfile;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.bomu_bomu.BomuAbilities;
import com.devilfruits.fruits.bomu_bomu.BomuBomu;
import com.devilfruits.fruits.bomu_bomu.BomuFx;
import com.devilfruits.fruits.bomu_bomu.explosion.BombExplosionService;
import com.devilfruits.fruits.bomu_bomu.explosion.ExplosionSpec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ExplosivePunchAbility extends Ability {
   private static final Map<UUID, Long> ARMED_UNTIL = new HashMap<>();

   public ExplosivePunchAbility() {
      super(
         BomuBomu.id("explosive_punch"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(14)
            .cooldown(30)
            .mastery(1)
            .damage(10.0)
            .range(2.8)
            .knockback(1.1)
            .blockDamage(true)
            .extra("armSeconds", 8.0, 1.0, 60.0, "Seconds the fist stays armed, waiting for a blow to land.")
            .extra("maxRadius", 4.2, 1.0, 16.0, "Cap of the blast radius (blocks); the radius is the range, which grows with mastery.")
            .extra("targetKnockback", 0.9, 0.0, 6.0, "Extra shove of the punched target away from the fist (blocks/tick).")
            .extra("maxPlayerDamage", 14.0, 0.0, 200.0, "Most damage the blast deals to a player (PvP cap).")
            .build()
      );
   }

   @Override
   public boolean coatable() {
      return true;
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.RADIAL).force(1.0).blockDamage(0.6).manual().build();
   }

   @Nullable
   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      return armed(player) ? Component.translatable("ability.devilfruits.bomu_bomu.explosive_punch.already_armed") : null;
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ARMED_UNTIL.put(player.getUUID(), player.level().getGameTime() + Math.round(ctx.extra("armSeconds") * 20.0));
      FormFlags.set(player, BomuBomu.ARMED, true);
      ServerFx.send(player, BomuFx.PUNCH_ARM, player.position(), ctx.look(), 1.0F, ctx.seed(), 0);
      ServerFx.animate(player, ctx.animation(BomuBomu.id("punch_arm")));
   }

   public static boolean armed(ServerPlayer player) {
      Long until = ARMED_UNTIL.get(player.getUUID());
      return until != null && player.level().getGameTime() <= until;
   }

   public static ExplosionSpec spec(AbilityContext ctx) {
      float radius = (float)Math.min((double)ctx.range(), ctx.extra("maxRadius"));
      return new ExplosionSpec(
         radius,
         radius,
         0.8F,
         ctx.damage(),
         ctx.knockback() * 0.7F,
         0.0F,
         radius >= 3.6F ? ExplosionProfile.MEDIUM : ExplosionProfile.SMALL,
         BomuAbilities.PUNCH.id()
      );
   }

   public static boolean detonate(ServerPlayer player, Vec3 at, Vec3 normal, @Nullable Entity target) {
      if (!armed(player)) {
         return false;
      } else {
         disarm(player);
         AbilityContext ctx = BomuBomu.context(player, BomuAbilities.PUNCH);
         if (ctx == null) {
            return false;
         } else {
            BombExplosionService.detonate(player.serverLevel(), at, normal, spec(ctx), player);
            if (target instanceof LivingEntity living && living.isAlive() && Targeting.canPush(player, living)) {
               Vec3 away = living.position().subtract(player.position());
               Vec3 flat = new Vec3(away.x, 0.0, away.z);
               Vec3 dir = flat.lengthSqr() < 1.0E-4 ? ctx.horizontalLook() : flat.normalize();
               Targeting.push(living, dir.scale(ctx.extra("targetKnockback")).add(0.0, 0.35, 0.0));
            }

            ServerFx.animate(player, ctx.animation(BomuBomu.id("punch_hit")));
            return true;
         }
      }
   }

   public static void tick(ServerPlayer player) {
      Long until = ARMED_UNTIL.get(player.getUUID());
      if (until != null) {
         if (player.level().getGameTime() > until || Weakness.isSuppressed(player) || !player.isAlive()) {
            disarm(player);
            ServerFx.send(player, BomuFx.PUNCH_FIZZLE, player.position(), Vec3.ZERO, 1.0F, 0L, 0);
         }
      }
   }

   public static void disarm(ServerPlayer player) {
      ARMED_UNTIL.remove(player.getUUID());
      FormFlags.set(player, BomuBomu.ARMED, false);
   }

   public static void forget(UUID player) {
      ARMED_UNTIL.remove(player);
   }

   public static void clear() {
      ARMED_UNTIL.clear();
   }
}
