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
import com.devilfruits.fruits.suna_suna.SunaFx;
import com.devilfruits.fruits.suna_suna.SunaSuna;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class DesertLaSpadaAbility extends SandTechnique {
   public DesertLaSpadaAbility() {
      super(
         "desert_la_spada",
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(28)
            .cooldown(120)
            .mastery(5)
            .damage(10.0)
            .range(18.0)
            .knockback(0.4)
            .extra("speed", 1.6, 0.4, 4.0, "Speed of the blades, blocks per tick.")
            .extra("blades", 3.0, 1.0, 12.0, "Blades at mastery 5; one more every two mastery levels above.")
            .extra("spread", 7.0, 0.0, 30.0, "Degrees between neighbouring blades of the fan.")
            .extra("bladeDamage", 0.55, 0.05, 2.0, "Damage of one blade as a share of the technique's damage.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.DIRECTIONAL).force(0.6).blockDamage(0.3).manual().build();
   }

   public static int blades(int base, int mastery) {
      return base + Math.max(0, (mastery - 5) / 2);
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      ServerFx.animate(player, ctx.animation(SunaSuna.id("desert_la_spada")));
      sandArm(player, 20);
      int count = blades((int)ctx.extra("blades"), ctx.mastery());
      double speed = ctx.extra("speed");
      double spread = ctx.extra("spread");
      int lifetime = Mth.ceil((double)ctx.range() / speed);
      long seed = ctx.seed();
      float damage = ctx.damage() * (float)ctx.extra("bladeDamage");

      for (int i = 0; i < count; i++) {
         int index = i;
         int slot = (i + 1) / 2 * (i % 2 == 0 ? -1 : 1);
         ServerScheduler.later(level, 2 + i * 2, () -> {
            if (!player.isRemoved() && player.level() == level) {
               Vec3 look = AimHelper.look(player);
               Vec3 right = new Vec3(-look.z, 0.0, look.x);
               right = right.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
               Vec3 start = player.getEyePosition().add(0.0, 0.35, 0.0).add(right.scale(0.45 + (double)slot * 0.12)).add(look.scale(0.4));
               Vec3 aim = aim(player, (double)ctx.range()).subtract(start).normalize();
               Vec3 dir = aim.yRot((float)Math.toRadians((double)(-slot) * spread)).xRot((float)Math.toRadians((double)(index % 2) * 1.5));
               fly(ctx, player, level, start, dir.scale(speed).add(PhysicsWorld.carrierVelocity(player)), lifetime, seed + (long)index, damage);
            }
         });
      }
   }

   public static Vec3 aim(ServerPlayer player, double range) {
      Vec3 eye = player.getEyePosition();
      Vec3 end = eye.add(AimHelper.look(player).scale(range));
      BlockHitResult block = PhysicsWorld.clip(player.level(), new ClipContext(eye, end, Block.COLLIDER, Fluid.NONE, player));
      if (block.getType() != Type.MISS) {
         end = block.getLocation();
      }

      EntityHitResult entity = ProjectileUtil.getEntityHitResult(
         player,
         eye,
         end,
         player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0),
         e -> e.isAlive() && e.isPickable() && !e.isSpectator(),
         eye.distanceToSqr(end)
      );
      return entity != null ? entity.getEntity().getBoundingBox().getCenter() : end;
   }

   private static void fly(AbilityContext ctx, ServerPlayer player, ServerLevel level, Vec3 start, Vec3 velocity, int lifetime, long seed, float damage) {
      ServerFx.sendNear(level, SunaFx.LA_SPADA, player.getId(), start, velocity, 1.0F, seed, lifetime, 96.0);
      Vec3[] at = new Vec3[]{start};
      SandFlight flight = SandFlight.track(level, player, start, 0.35, seed, 1.0F, 1);
      ServerScheduler.schedule(level, 1, age -> {
         if (!player.isRemoved() && player.level() == level && !flight.broken()) {
            Vec3 pos = at[0];
            Vec3 next = pos.add(velocity);
            BlockHitResult block = PhysicsWorld.clip(level, new ClipContext(pos, next, Block.COLLIDER, Fluid.NONE, player));
            Vec3 end = block.getType() == Type.MISS ? next : block.getLocation();
            List<Entity> hit = Targeting.inCapsule(level, player, pos, end, 0.35);
            if (hit.isEmpty() && block.getType() == Type.MISS && age < lifetime) {
               at[0] = next;
               flight.moveTo(next);
               return false;
            } else {
               Vec3 impact = end;
               if (!hit.isEmpty()) {
                  Entity e = hit.get(0);
                  impact = e.getBoundingBox().getCenter();
                  if (e instanceof LivingEntity living) {
                     living.invulnerableTime = 0;
                  }

                  if (ctx.hurt(e, damage)) {
                     Targeting.launch(e, velocity.normalize().scale((double)ctx.knockback()).add(0.0, 0.12, 0.0));
                  }
               }

               if (hit.isEmpty() && block.getType() != Type.MISS) {
                  BlockImpactService.strike(ctx, ImpactShape.sphere(impact, 0.9), velocity, 1.0);
               }

               flight.end();
               ServerFx.sendNear(level, SunaFx.LA_SPADA_IMPACT, player.getId(), impact, velocity, 1.0F, seed, hit.isEmpty() ? 0 : 1, 96.0);
               return true;
            }
         } else {
            flight.end();
            return true;
         }
      });
   }
}
