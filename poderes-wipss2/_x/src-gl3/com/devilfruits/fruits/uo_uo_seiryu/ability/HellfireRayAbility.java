package com.devilfruits.fruits.uo_uo_seiryu.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuFx;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class HellfireRayAbility extends Ability {
   private static final Map<UUID, HellfireRayAbility.State> STATES = new HashMap<>();
   public static final int WINDUP_TICKS = 8;
   public static final int EXPLODE_TICKS = 4;
   private static final int MIN_RAY_TICKS = 10;

   public HellfireRayAbility() {
      super(
         UoUoSeiryu.id("hellfire_ray"),
         CastType.HELD,
         AbilityStats.builder()
            .energy(20)
            .cooldown(240)
            .mastery(4)
            .damage(0.25)
            .range(14.0)
            .knockback(0.6)
            .blockDamage(true)
            .extra("maxHits", 49.0, 1.0, 200.0, "Hits of a channel held to the end.")
            .extra("maxSeconds", 3.5, 0.5, 10.0, "Longest channel (seconds).")
            .extra("ringRadius", 2.5, 0.5, 6.0, "Radius of the fire rings: what is inside is caught and hit.")
            .extra("chaseSpeed", 1.35, 0.0, 4.0, "How fast the spinning body flies after its target (blocks per tick).")
            .extra("hoverHeight", 1.4, 0.0, 4.0, "How high the body hovers over the target's head.")
            .extra("burnPerSecond", 6.0, 0.0, 100.0, "Extra energy the channel burns per second.")
            .flag("igniteBlocks", true, "The fire can set flammable blocks alight (with the block damage switch and mobGriefing).")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults())
         .impact(PhysicsProfile.Impact.SUSTAINED)
         .force(6.0)
         .reaction(PhysicsProfile.Reaction.NONE)
         .blockDamage(0.0)
         .manual()
         .build();
   }

   @Nullable
   @Override
   public ResourceKey<DamageType> damageType() {
      return UoUoSeiryu.FLAME_DAMAGE;
   }

   @Override
   public boolean coatable() {
      return true;
   }

   public int rayTicks() {
      return (int)Math.round(this.config().extra("maxSeconds") * 20.0);
   }

   @Override
   public int maxChannelTicks() {
      return 8 + this.rayTicks();
   }

   @Override
   public int minChannelTicks() {
      return 18;
   }

   @Override
   public void onChannelStart(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      HellfireRayAbility.State st = new HellfireRayAbility.State();
      st.seed = ctx.seed();
      st.target = HybridCombat.aimTarget(player, (double)ctx.range(), 45.0);
      st.drift = ctx.horizontalLook();
      STATES.put(player.getUUID(), st);
      ServerFx.animate(player, UoUoSeiryu.anim("hybrid/hellfire_ray_windup"));
      ServerFx.send(
         player,
         SeiryuFx.HELLFIRE_WINDUP,
         player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0),
         ctx.horizontalLook(),
         (float)ctx.extra("ringRadius"),
         st.seed,
         8
      );
   }

   @Override
   public boolean channelTick(AbilityContext ctx, int ticks) {
      ServerPlayer player = ctx.player();
      HellfireRayAbility.State st = STATES.get(player.getUUID());
      if (st == null) {
         return false;
      } else {
         player.fallDistance = 0.0F;
         PlayerFruitData data = ctx.data();
         if (!AbilityManager.freeCast(player, data)) {
            data.setEnergy(Math.max(0.0F, data.energy() - (float)ctx.extra("burnPerSecond") / 20.0F));
            if (data.energy() <= 0.0F) {
               return false;
            }
         }

         if (ticks < 8) {
            return true;
         } else {
            if (ticks == 8) {
               ServerFx.animate(player, UoUoSeiryu.anim("hybrid/hellfire_ray"));
               ServerFx.send(
                  player,
                  SeiryuFx.HELLFIRE,
                  player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0),
                  st.drift,
                  (float)ctx.extra("ringRadius"),
                  st.seed,
                  this.rayTicks()
               );
            }

            this.move(ctx, st);
            Vec3 center = player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);
            double radius = ctx.extra("ringRadius");
            int maxHits = (int)ctx.extra("maxHits");
            int due = Math.min(maxHits, (int)((long)(ticks - 8 + 1) * (long)maxHits / (long)Math.max(1, this.rayTicks())));
            float damage = HybridCombat.hit(ctx);
            Entity struck = null;
            int struckCount = 0;

            while (st.hits < due) {
               st.hits++;

               for (Entity e : Targeting.inRadius(ctx.level(), center, radius, x -> Targeting.canHurt(player, x))) {
                  if (HybridCombat.strike(ctx, e, damage) && e instanceof LivingEntity) {
                     LivingEntity living = (LivingEntity)e;
                     if (struck == null) {
                        struck = living;
                     }

                     struckCount++;
                     if (st.hits % 4 == 1 || !HybridStun.stunned(living)) {
                        HybridStun.apply(living, 12);
                     }

                     Vec3 in = center.subtract(living.getBoundingBox().getCenter());
                     if (in.lengthSqr() > 1.0) {
                        Targeting.launch(living, in.normalize().scale(0.15).add(0.0, 0.04, 0.0));
                     }
                  }
               }
            }

            if (struck != null) {
               ServerFx.sendNear(
                  ctx.level(),
                  SeiryuFx.HELLFIRE_HIT,
                  struck.getId(),
                  struck.getBoundingBox().getCenter(),
                  center,
                  (float)struckCount,
                  st.seed + (long)st.hits,
                  st.hits,
                  64.0
               );
            }

            if (ticks % 5 == 0) {
               BlockImpactService.push(ctx, ImpactShape.sphere(center, radius), st.drift, 0.25, PhysicsProfile.Impact.SUSTAINED);
            }

            if (ticks % 10 == 5 && ctx.flag("igniteBlocks")) {
               SeiryuCombat.scorch(ctx, center.add(0.0, (double)(-player.getBbHeight()) * 0.5, 0.0), radius, 3, 0.35, 200);
            }

            return true;
         }
      }
   }

   private void move(AbilityContext ctx, HellfireRayAbility.State st) {
      ServerPlayer player = ctx.player();
      LivingEntity target = st.target != null && st.target.isAlive() && st.target.level() == player.level() ? st.target : null;
      double speed = ctx.extra("chaseSpeed");
      double climb = speed * 0.9;
      Vec3 v;
      if (target != null) {
         Vec3 to = target.position().subtract(player.position());
         Vec3 flat = new Vec3(to.x, 0.0, to.z);
         double d = flat.length();
         if (d > 0.001) {
            st.drift = flat.scale(1.0 / d);
         }

         double step = Math.min(speed, Math.max(0.0, d - 0.4));
         double hover = aboveWater(ctx, target.getY() + (double)target.getBbHeight() + ctx.extra("hoverHeight") - (double)player.getBbHeight() * 0.35);
         v = st.drift.scale(step).add(0.0, Mth.clamp((hover - player.getY()) * 0.5, -climb, climb), 0.0);
      } else {
         BlockPos ground = TerrainDestruction.findSurface(ctx.level(), Mth.floor(player.getX()), Mth.floor(player.getY()), Mth.floor(player.getZ()), 2, 6);
         double hover = aboveWater(ctx, (ground != null ? (double)(ground.getY() + 1) : player.getY()) + ctx.extra("hoverHeight"));
         v = st.drift.scale(speed * 0.6).add(0.0, Mth.clamp((hover - player.getY()) * 0.5, -climb, climb), 0.0);
      }

      Targeting.moveSelf(player, v);
   }

   private static double aboveWater(AbilityContext ctx, double y) {
      ServerPlayer player = ctx.player();
      MutableBlockPos pos = new MutableBlockPos();

      for (int i = 0; i < 8; i++) {
         pos.set(player.getX(), y, player.getZ());
         if (ctx.level().getFluidState(pos).isEmpty()) {
            break;
         }

         y = Math.floor(y) + 1.2;
      }

      return y;
   }

   @Override
   public void onChannelEnd(AbilityContext ctx, int ticks, boolean interrupted) {
      ServerPlayer player = ctx.player();
      HellfireRayAbility.State st = STATES.remove(player.getUUID());
      if (st != null) {
         Vec3 center = player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);
         ServerLevel level = ctx.level();
         Targeting.moveSelf(player, new Vec3(0.0, -0.6, 0.0));
         if (!interrupted) {
            ServerFx.animate(player, UoUoSeiryu.anim("hybrid/hellfire_ray_end"));
         }

         ServerFx.send(player, SeiryuFx.HELLFIRE_END, center, st.drift, (float)ctx.extra("ringRadius"), st.seed, st.hits);
         double radius = ctx.extra("ringRadius") * 1.4;
         double knockback = (double)ctx.knockback();
         Vec3 drift = st.drift;
         ServerScheduler.later(level, 4, () -> {
            if (player.isAlive() && player.level() == level) {
               Vec3 at = player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);

               for (Entity e : Targeting.inRadius(level, at, radius, x -> Targeting.canPush(player, x))) {
                  Vec3 out = e.position().subtract(player.position());
                  Vec3 flat = new Vec3(out.x, 0.0, out.z);
                  Targeting.push(e, (flat.lengthSqr() < 1.0E-4 ? drift : flat.normalize()).scale(knockback).add(0.0, 0.3, 0.0));
               }

               BlockImpactService.push(ctx, ImpactShape.sphere(at, radius), new Vec3(0.0, -1.0, 0.0), 0.6, PhysicsProfile.Impact.RADIAL);
               ServerFx.shake(level, at, 0.5F, 32);
            }
         });
      }
   }

   public int hits(ServerPlayer player) {
      HellfireRayAbility.State st = STATES.get(player.getUUID());
      return st == null ? -1 : st.hits;
   }

   private static final class State {
      @Nullable
      LivingEntity target;
      Vec3 drift = Vec3.ZERO;
      int hits;
      long seed;
   }
}
