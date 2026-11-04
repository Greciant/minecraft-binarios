package com.devilfruits.fruits.gura_gura.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsConfig;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.physics.PhysicsStructure;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.physics.StructureForces;
import com.devilfruits.core.util.DeterministicRandom;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.gura_gura.GuraFx;
import com.devilfruits.fruits.gura_gura.GuraGura;
import com.devilfruits.fruits.gura_gura.GuraTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ShimaYurashiAbility extends Ability {
   public ShimaYurashiAbility() {
      super(
         GuraGura.id("shima_yurashi"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(80)
            .cooldown(1200)
            .mastery(5)
            .damage(7.0)
            .range(28.0)
            .knockback(0.7)
            .extra("tremorSeconds", 5.0, 1.0, 30.0, "Duration of the tremor.")
            .extra("pulseInterval", 10.0, 2.0, 100.0, "Ticks between damage / push pulses.")
            .extra("crackDistance", 14.0, 4.0, 64.0, "Distance of the sky crack in front of the user.")
            .extra("crackRadius", 9.0, 2.0, 40.0, "Radius of the sky crack.")
            .extra("debrisPerPulse", 7.0, 0.0, 64.0, "Blocks torn out per pulse.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.TANGENTIAL).force(1.5).blockDamage(0.08).manual().build();
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      AbilityManager.setBusy(player, 32);
      ServerFx.animate(player, ctx.animation(GuraGura.anim("shima_yurashi")));
      GuraTriggers.USED_SHIMA_YURASHI.get().trigger(player);
      Vec3 flat = ctx.horizontalLook();
      float radius = ctx.range();
      int duration = (int)(ctx.extra("tremorSeconds") * 20.0);
      int interval = (int)ctx.extra("pulseInterval");
      long seed = ctx.seed();
      ServerScheduler.later(level, 8, () -> {
         Vec3 crack = player.getEyePosition().add(flat.scale(ctx.extra("crackDistance"))).add(0.0, 3.5, 0.0);
         ServerFx.sendNear(level, GuraFx.SHIMA_GRAB, player.getId(), crack, flat.scale(-1.0), (float)ctx.extra("crackRadius"), seed, duration, 192.0);
      });
      ServerScheduler.schedule(
         level,
         14,
         age -> {
            if (age == 0) {
               float swayYaw = (float)Mth.atan2(flat.z, flat.x);
               ServerFx.sendNear(
                  level,
                  GuraFx.SHIMA_TREMOR,
                  player.getId(),
                  player.position(),
                  new Vec3((double)swayYaw, (double)interval, 0.0),
                  radius,
                  seed,
                  duration,
                  (double)radius * 2.5
               );
            }

            if (age % interval == 0) {
               this.pulse(ctx, level, player, flat, radius, age / interval);
            }

            return age >= duration || !player.isAlive();
         }
      );
   }

   private void pulse(AbilityContext ctx, ServerLevel level, ServerPlayer player, Vec3 flat, float radius, int index) {
      Vec3 center = player.position();
      QuakeDeflect.sweep(player, center.add(0.0, 1.0, 0.0), (double)radius);
      Vec3 perp = new Vec3(-flat.z, 0.0, flat.x);
      double side = index % 2 == 0 ? 1.0 : -1.0;
      DeterministicRandom random = new DeterministicRandom(ctx.seed() + (long)index * 7919L);

      for (Entity e : Targeting.inRadius(level, center, (double)radius, xx -> Targeting.canPush(player, xx))) {
         double falloff = 1.0 - 0.6 * (double)e.distanceTo(player) / (double)radius;
         ctx.hurt(e, ctx.damage() * (float)falloff);
         Vec3 sway = perp.scale(side * (double)ctx.knockback() * falloff)
            .add((double)random.signed(0.15F), 0.18 + (double)random.nextFloat() * 0.2, (double)random.signed(0.15F));
         Targeting.push(e, sway);
         if (e instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2, false, false, true));
         }
      }

      this.shakeStructures(ctx, level, player, center, perp.scale(side), radius);
      if (TerrainDestruction.destructionAllowed(level)) {
         float power = GuraGura.destruction(ctx.mastery());
         ctx.budget(GuraGura.blockBudget(power));
         int count = Math.round((float)ctx.extra("debrisPerPulse") * power);

         for (int i = 0; i < count && ctx.budget().hasBlocks(); i++) {
            double r = 4.0 + Math.sqrt((double)random.nextFloat()) * (double)(radius - 4.0F);
            double a = (double)random.nextFloat() * Math.PI * 2.0;
            int x = Mth.floor(center.x + Math.cos(a) * r);
            int z = Mth.floor(center.z + Math.sin(a) * r);
            BlockPos surface = TerrainDestruction.findSurface(level, x, player.getBlockY(), z, 6, 8);
            if (surface != null) {
               Vec3 velocity = new Vec3((double)random.signed(0.15F), 0.5 + (double)random.nextFloat() * 0.45, (double)random.signed(0.15F));
               TerrainDestruction.tearOut(level, player, surface, velocity, ctx.budget(), random.nextInt(200), true);
            }
         }
      }
   }

   private void shakeStructures(AbilityContext ctx, ServerLevel level, ServerPlayer player, Vec3 center, Vec3 sway, float radius) {
      if (PhysicsWorld.active() && PhysicsConfig.enabledFor(ctx.fruit().id())) {
         double strength = this.physics().strength((double)ctx.damage(), (double)ctx.knockback());

         for (PhysicsStructure s : PhysicsWorld.bridge().structuresIn(level, new AABB(center, center).inflate((double)radius))) {
            if (BlockImpactService.allowed(player, s)) {
               StructureForces.shake(player, s, sway, strength);
            }
         }

         BlockImpactService.damage(ctx, ImpactShape.sphere(center, (double)radius), new Vec3(0.0, 1.0, 0.0), 1.0, false, ctx.budget());
      }
   }
}
