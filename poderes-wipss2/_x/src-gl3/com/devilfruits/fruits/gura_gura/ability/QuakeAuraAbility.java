package com.devilfruits.fruits.gura_gura.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.combat.AbilityProjectiles;
import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.gura_gura.GuraAbilities;
import com.devilfruits.fruits.gura_gura.GuraFx;
import com.devilfruits.fruits.gura_gura.GuraGura;
import com.devilfruits.fruits.gura_gura.entity.QuakeSlashEntity;
import com.devilfruits.registry.DFAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public class QuakeAuraAbility extends Ability {
   public QuakeAuraAbility() {
      super(
         GuraGura.id("shindo"),
         CastType.TOGGLE,
         AbilityStats.builder()
            .energy(10)
            .cooldown(40)
            .mastery(3)
            .damage(5.0)
            .range(3.5)
            .knockback(0.8)
            .extra("drainPerSecond", 4.0, 0.0, 200.0, "Energy drained per second while the aura is active.")
            .extra("deflectCost", 2.0, 0.0, 200.0, "Energy per shattered / deflected projectile.")
            .extra("hitCost", 1.0, 0.0, 200.0, "Energy per empowered melee hit.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults())
         .impact(PhysicsProfile.Impact.NONE)
         .reaction(PhysicsProfile.Reaction.NONE)
         .blockDamage(0.0)
         .manual()
         .build();
   }

   @Override
   public void onToggle(AbilityContext ctx, boolean active) {
      ServerPlayer player = ctx.player();
      ServerFx.simple(player, active ? GuraFx.AURA_ON : GuraFx.AURA_OFF);
      if (active) {
         ServerFx.animate(player, ctx.animation(GuraGura.anim("shindo")));
      }
   }

   @Override
   public void onStartTracking(ServerPlayer user, ServerPlayer tracker) {
      ServerFx.sendTo(tracker, GuraFx.AURA_ON, user, user.position(), Vec3.ZERO, 1.0F, 0L, 0);
   }

   @Override
   public void toggleTick(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      PlayerFruitData data = ctx.data();
      boolean free = AbilityManager.freeEnergy(player);
      if (!free) {
         data.setEnergy(data.energy() - (float)this.config().extra("drainPerSecond") / 20.0F);
      }

      double radius = (double)ctx.range();
      Vec3 center = player.getBoundingBox().getCenter();

      for (Projectile projectile : player.level()
         .getEntitiesOfClass(
            Projectile.class,
            player.getBoundingBox().inflate(radius),
            p -> p.isAlive() && p.getOwner() != player && !(p instanceof QuakeSlashEntity) && p.position().distanceToSqr(center) <= radius * radius
         )) {
         Vec3 velocity = projectile.getDeltaMovement();
         if (!(projectile instanceof ThrownTrident) && !(projectile instanceof AbstractHurtingProjectile)) {
            projectile.discard();
         } else {
            Vec3 away = projectile.position().subtract(center).normalize();
            projectile.setDeltaMovement(away.scale(Math.max(0.8, velocity.length())));
            projectile.hasImpulse = true;
            if (projectile instanceof AbstractHurtingProjectile hurting) {
               hurting.setOwner(player);
               hurting.accelerationPower = 0.1;
            }
         }

         ServerFx.send(player, GuraFx.AURA_DEFLECT, projectile.position(), velocity, 1.0F, 0L, 0);
         if (!free) {
            data.setEnergy(data.energy() - (float)this.config().extra("deflectCost"));
         }
      }

      int broken = AbilityProjectiles.shatter(ctx.level(), player, p -> p.distanceToSqr(center) <= radius * radius, new Vec3(0.0, 1.0, 0.0));
      if (broken > 0 && !free) {
         data.setEnergy(data.energy() - (float)this.config().extra("deflectCost") * (float)broken);
      }
   }

   public static void onIncomingDamage(LivingIncomingDamageEvent event) {
      DamageSource source = event.getSource();
      if (source.getEntity() instanceof ServerPlayer attacker && source.getDirectEntity() == attacker && source.is(DamageTypes.PLAYER_ATTACK)) {
         PlayerFruitData data = DFAttachments.data(attacker);
         LivingEntity target = event.getEntity();
         if (data.isToggleActive(GuraAbilities.SHINDO.id()) && target != attacker && Targeting.canHurt(attacker, target)) {
            QuakeAuraAbility aura = GuraAbilities.SHINDO;
            float bonus = (float)aura.config().damage() * Mastery.damageMultiplier(data.mastery());
            event.setAmount(event.getAmount() + bonus);
            if (!AbilityManager.freeEnergy(attacker)) {
               data.setEnergy(data.energy() - (float)aura.config().extra("hitCost"));
            }

            data.setLastCombatTick(attacker.level().getGameTime());
            Mastery.addXp(attacker, data, ((Double)CoreConfig.XP_PER_HIT.get()).floatValue());
            Vec3 dir = target.position().subtract(attacker.position()).multiply(1.0, 0.0, 1.0);
            Vec3 push = dir.lengthSqr() > 1.0E-4 ? dir.normalize().scale(aura.config().knockback()).add(0.0, 0.2, 0.0) : new Vec3(0.0, 0.3, 0.0);
            Targeting.push(target, push);
            ServerFx.send(
               attacker, GuraFx.AURA_HIT, target.getBoundingBox().getCenter(), push.normalize(), 1.0F, attacker.getRandom().nextLong(), target.getId()
            );
            return;
         }

         return;
      }
   }
}
