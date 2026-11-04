package com.devilfruits.fruits.suna_suna.ability;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.AimHelper;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.suna_suna.SunaEffects;
import com.devilfruits.fruits.suna_suna.SunaFx;
import com.devilfruits.fruits.suna_suna.SunaSuna;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class SablesPesadoAbility extends SandTechnique {
   public static final float TAP = 0.3F;

   public SablesPesadoAbility() {
      super(
         "sables_pesado",
         CastType.CHARGED,
         AbilityStats.builder()
            .energy(12)
            .cooldown(30)
            .mastery(1)
            .damage(7.0)
            .range(48.0)
            .knockback(1.2)
            .extra("blastSpeed", 2.2, 0.4, 4.0, "Speed of the quick blast (tap), blocks per tick.")
            .extra("ballSpeed", 1.4, 0.3, 3.0, "Speed of the heavy ball (hold), blocks per tick.")
            .extra("shockRadius", 4.0, 1.0, 12.0, "Radius of the ball's shockwave at full charge.")
            .extra("dehydrateSeconds", 6.0, 0.0, 60.0, "Seconds of Dehydrated the shockwave leaves (level I, II at full charge).")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.RADIAL).force(1.0).blockDamage(0.4).manual().build();
   }

   @Override
   public int maxChargeTicks() {
      return 30;
   }

   @Override
   public void onChargeStart(AbilityContext ctx) {
      ServerFx.animate(ctx.player(), ctx.animation(SunaSuna.id("sables_pesado")));
      sandArm(ctx.player(), 15);
   }

   @Override
   public void onChargeTick(AbilityContext ctx, int ticks) {
      if (ticks % 10 == 0) {
         sandArm(ctx.player(), 15);
      }
   }

   @Override
   public void release(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      float charge = ctx.charge();
      boolean heavy = charge >= 0.3F;
      ServerFx.animate(player, ctx.animation(SunaSuna.id(heavy ? "sables_pesado_release" : "sables_pesado_tap")));
      sandArm(player, heavy ? 12 : 8);
      double speed = ctx.extra(heavy ? "ballSpeed" : "blastSpeed");
      float size = heavy ? 0.55F + 0.65F * charge : 0.3F;
      Vec3 start = hand(player);
      Vec3 velocity = AimHelper.look(player).scale(speed).add(PhysicsWorld.carrierVelocity(player));
      int lifetime = Mth.ceil((double)ctx.range() / speed);
      this.launch(ctx, start, velocity, size, heavy, charge, ctx.seed(), lifetime);
   }

   public static Vec3 hand(ServerPlayer player) {
      Vec3 look = AimHelper.look(player);
      Vec3 right = new Vec3(-look.z, 0.0, look.x);
      right = right.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
      return player.getEyePosition().add(look.scale(0.55)).add(right.scale(0.32)).add(0.0, -0.3, 0.0);
   }

   private void launch(AbilityContext ctx, Vec3 start, Vec3 velocity, float size, boolean heavy, float charge, long seed, int lifetime) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      ServerFx.sendNear(level, SunaFx.PESADO, player.getId(), start, velocity, size, seed, lifetime * (heavy ? 1 : -1), 128.0);
      Vec3[] state = new Vec3[]{start, velocity};
      double gravity = heavy ? 0.003 : 0.0;
      SandFlight flight = SandFlight.track(level, player, start, 0.45 * (double)size + 0.2, seed, size, 0);
      ServerScheduler.schedule(level, 1, age -> {
         if (!player.isRemoved() && player.level() == level && !flight.broken()) {
            Vec3 pos = state[0];
            Vec3 vel = state[1];
            Vec3 next = pos.add(vel);
            BlockHitResult block = PhysicsWorld.clip(level, new ClipContext(pos, next, Block.COLLIDER, Fluid.ANY, player));
            Vec3 end = block.getType() == Type.MISS ? next : block.getLocation();
            List<Entity> hit = Targeting.inCapsule(level, player, pos, end, 0.45 * (double)size + 0.2);
            if (hit.isEmpty() && block.getType() == Type.MISS && age < lifetime) {
               state[0] = next;
               flight.moveTo(next);
               state[1] = vel.add(0.0, -gravity, 0.0);
               return false;
            } else {
               Vec3 at = hit.isEmpty() ? end : hit.get(0).getBoundingBox().getCenter();
               flight.end();
               this.impact(ctx, at, vel, size, heavy, charge, seed, hit.isEmpty() ? null : hit.get(0));
               return true;
            }
         } else {
            flight.end();
            return true;
         }
      });
   }

   private void impact(AbilityContext ctx, Vec3 at, Vec3 vel, float size, boolean heavy, float charge, long seed, Entity struck) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      Vec3 dir = vel.lengthSqr() < 1.0E-6 ? AimHelper.look(player) : vel.normalize();
      if (!heavy) {
         if (struck != null && ctx.hurt(struck, ctx.damage() * 0.55F)) {
            Targeting.launch(struck, dir.scale((double)ctx.knockback() * 0.5).add(0.0, 0.15, 0.0));
         }

         BlockImpactService.push(ctx, ImpactShape.sphere(at, 1.0), dir, 0.3, PhysicsProfile.Impact.DIRECTIONAL);
         ServerFx.sendNear(level, SunaFx.PESADO_IMPACT, player.getId(), at, vel, 0.8F, seed, 0, 96.0);
      } else {
         double radius = ctx.extra("shockRadius") * (0.45 + 0.55 * (double)charge);
         int seconds = (int)ctx.extra("dehydrateSeconds");
         int levels = charge >= 0.9F ? 2 : 1;

         for (Entity e : Targeting.inRadius(level, at, radius, x -> Targeting.canHurt(player, x))) {
            float falloff = (float)Mth.clamp(1.0 - e.getBoundingBox().getCenter().distanceTo(at) / (radius * 1.4), 0.35, 1.0);
            if (ctx.hurt(e, ctx.damage() * (0.6F + 0.6F * charge) * falloff)) {
               Vec3 out = e.position().subtract(at);
               Vec3 push = out.horizontalDistanceSqr() < 1.0E-4 ? dir : new Vec3(out.x, 0.0, out.z).normalize();
               Targeting.launch(e, push.scale((double)ctx.knockback() * (1.0 + (double)charge) * (double)falloff).add(0.0, 0.35 * (double)falloff, 0.0));
               if (seconds > 0 && e instanceof LivingEntity living) {
                  SunaEffects.dehydrate(living, seconds, levels, 4, player);
               }
            }
         }

         BlockImpactService.strike(ctx, ImpactShape.sphere(at, radius), dir, 0.6 + 0.6 * (double)charge);
         ServerFx.shake(level, at, 0.2F + 0.3F * charge, 24);
         ServerFx.sendNear(level, SunaFx.PESADO_IMPACT, player.getId(), at, vel, (float)radius, seed, 1, 96.0);
      }
   }
}
