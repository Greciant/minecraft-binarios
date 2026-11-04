package com.devilfruits.fruits.uo_uo_seiryu.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.AimHelper;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.body.SerpentBody;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.uo_uo_seiryu.DragonShells;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuFx;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.fruits.uo_uo_seiryu.entity.SeiryuDragonEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class TailWhipAbility extends Ability {
   private static final int SWEEP_TICKS = 26;
   private static final int TAIL_SAMPLES = 10;
   private static final int SUB_STEPS = 4;
   private static final ResourceLocation HOLD = UoUoSeiryu.id("coil_crush_hold");

   @Override
   public boolean coatable() {
      return true;
   }

   public TailWhipAbility() {
      super(
         UoUoSeiryu.id("tail_whip"),
         CastType.COMBO,
         AbilityStats.builder()
            .energy(25)
            .cooldown(160)
            .mastery(6)
            .damage(16.0)
            .range(1.0)
            .knockback(2.5)
            .blockDamage(true)
            .extra("crushDamagePerSecond", 6.0, 0.0, 100.0, "Coil Crush damage per second.")
            .extra("crushTicks", 60.0, 10.0, 400.0, "Coil Crush duration.")
            .extra("crushReach", 14.0, 2.0, 48.0, "How far from the head the coil reaches for a victim.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.DIRECTIONAL).force(1.0).blockDamage(0.4).manual().build();
   }

   @Override
   public int maxComboSteps() {
      return 2;
   }

   @Override
   public int comboWindowTicks() {
      return 40;
   }

   @Override
   public void comboStep(AbilityContext ctx, int step) {
      SeiryuDragonEntity shell = DragonShells.get(ctx.player());
      if (shell != null && shell.body() != null) {
         if (step == 0) {
            this.sweep(ctx, shell, shell.body());
         } else {
            SerpentBody body = shell.body();
            int delay = body.action() == 1 ? Math.max(0, 14 - (int)body.actionTime(0.0F)) : 0;
            ServerScheduler.later(ctx.level(), delay, () -> this.crush(ctx, shell));
         }
      }
   }

   private void sweep(AbilityContext ctx, SeiryuDragonEntity shell, SerpentBody body) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      float side = side(ctx, player);
      body.sweep(26, side);
      ServerFx.animate(player, UoUoSeiryu.anim("dragon/tail_whip"));
      ServerFx.send(player, SeiryuFx.TAIL, player.position(), new Vec3(1.0, 0.0, (double)side), 1.0F, ctx.seed(), 26);
      Set<Entity> hit = new HashSet<>();
      double[] p = new double[3];
      double[] pivot = new double[3];
      float damage = ctx.damage();
      ServerScheduler.schedule(level, 1, age -> {
         if (player.isAlive() && !shell.isRemoved() && shell.body() == body && body.action() == 1 && age <= 14) {
            body.point((double)(body.neck + body.length * 0.35F), 1.0F, pivot);
            float from = age == 1 ? -body.actionTime(0.0F) : 0.0F;
            int steps = (int)Math.ceil((double)(4.0F * (1.0F - from)));
            int samples = samples(body);
            Vec3[][] path = new Vec3[samples][steps + 1];
            AABB swept = null;

            for (int i = 0; i < samples; i++) {
               double d = (double)(body.neck + body.length * sample(i, samples));

               for (int step = 0; step <= steps; step++) {
                  body.point(d, from + (1.0F - from) * (float)step / (float)steps, p);
                  path[i][step] = new Vec3(p[0], p[1], p[2]);
                  AABB at = new AABB(path[i][step], path[i][step]);
                  swept = swept == null ? at : swept.minmax(at);
               }
            }

            List<Entity> candidates = level.getEntities((Entity)null, swept.inflate((double)body.thickness + 2.0));
            List<Entity> found = new ArrayList<>();

            for (int i = 0; i < samples; i++) {
               double reach = (double)body.radiusAt(sample(i, samples)) + 0.6;

               for (int step = 1; step <= steps; step++) {
                  SeiryuCombat.capsuleHits(candidates, player, path[i][step - 1], path[i][step], reach, found);
               }

               if (i >= samples - 3 && (age & 1) == 0) {
                  SeiryuCombat.crater(ctx, path[i][steps], 1.2, 1, 0.6);
               }
            }

            List<ImpactShape> tail = new ArrayList<>();

            for (int i = Math.max(0, samples - 3); i < samples; i++) {
               double reach = (double)body.radiusAt(sample(i, samples)) + 0.6;
               tail.add(ImpactShape.capsule(path[i][0], path[i][steps], reach));
            }

            Vec3 tip = path[samples - 1][steps];
            double tx = tip.x - pivot[0];
            double tz = tip.z - pivot[2];
            double tl = Math.max(1.0E-4, Math.sqrt(tx * tx + tz * tz));
            BlockImpactService.push(ctx, ImpactShape.union(tail), new Vec3(tz / tl * (double)side, 0.15, -tx / tl * (double)side), 0.45);

            for (Entity e : found) {
               if (hit.add(e)) {
                  double rx = e.getX() - pivot[0];
                  double rz = e.getZ() - pivot[2];
                  double len = Math.max(1.0E-4, Math.sqrt(rx * rx + rz * rz));
                  Vec3 along = new Vec3(rz / len * (double)side, 0.0, -rx / len * (double)side);
                  if (ctx.hurt(e, damage)) {
                     Targeting.launch(e, along.scale((double)ctx.knockback()).add(0.0, 0.6, 0.0));
                  }
               }
            }

            if (age == 10) {
               ServerFx.shake(level, new Vec3(p[0], p[1], p[2]), 0.45F, 40);
            }

            return false;
         } else {
            return true;
         }
      });
   }

   private static float sample(int i, int samples) {
      return 0.35F + 0.65F * ((float)i + 0.5F) / (float)samples;
   }

   private static int samples(SerpentBody body) {
      return Math.max(10, (int)Math.ceil((double)(body.length * 0.65F / 3.9F)));
   }

   private static float side(AbilityContext ctx, ServerPlayer player) {
      Vec3 f = ctx.horizontalLook();
      double rightX = -f.z;
      double rightZ = f.x;
      int score = 0;

      for (Entity e : Targeting.inRadius(ctx.level(), player.position(), 48.0, x -> Targeting.canHurt(player, x))) {
         double dx = e.getX() - player.getX();
         double dz = e.getZ() - player.getZ();
         score += dx * rightX + dz * rightZ >= 0.0 ? 1 : -1;
      }

      return score != 0 ? Math.signum((float)score) : ((ctx.seed() & 1L) == 0L ? 1.0F : -1.0F);
   }

   private void crush(AbilityContext ctx, SeiryuDragonEntity shell) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      SerpentBody body = shell.body();
      if (player.isAlive() && !shell.isRemoved() && body != null) {
         LivingEntity victim = victim(ctx, player);
         Vec3 center;
         float radius;
         if (victim != null) {
            center = victim.position();
            radius = Math.max(0.6F, victim.getBbWidth() * 0.5F + 0.2F);
         } else {
            Vec3 ahead = SeiryuCombat.rayEnd(level, player, player.getEyePosition(), AimHelper.look(player), 10.0);
            HitResult down = PhysicsWorld.clip(level, new ClipContext(ahead, ahead.add(0.0, -16.0, 0.0), Block.COLLIDER, Fluid.NONE, player));
            center = down.getType() == Type.MISS ? ahead : down.getLocation();
            radius = 1.0F;
         }

         int ticks = (int)ctx.extra("crushTicks");
         body.crush(ticks, center.x, center.y, center.z, radius);
         ServerFx.animate(player, UoUoSeiryu.anim("dragon/coil_crush"));
         ServerFx.send(player, SeiryuFx.TAIL, center, new Vec3(2.0, (double)radius, 1.0), 1.0F, ctx.seed(), ticks);
         if (victim != null) {
            hold(victim, true);
         }

         float perSqueeze = (float)ctx.extra("crushDamagePerSecond") * 0.5F;
         int release = ticks - 8;
         ServerScheduler.schedule(
            level,
            1,
            age -> {
               boolean victimGone = victim != null && (!victim.isAlive() || victim.isRemoved() || victim.level() != level);
               boolean over = age >= release
                  || victimGone
                  || !player.isAlive()
                  || shell.isRemoved()
                  || shell.body() != body
                  || body.action() != 2
                  || player.distanceToSqr(center) > 2304.0;
               if (over) {
                  if (victim != null) {
                     hold(victim, false);
                     if (age >= release && !victimGone && ctx.hurt(victim, perSqueeze * 2.0F)) {
                        Targeting.launch(victim, new Vec3(0.0, 1.1, 0.0));
                     }
                  }

                  return true;
               } else {
                  if (victim != null) {
                     if (victim.position().distanceToSqr(center) > 0.6400000000000001) {
                        victim.teleportTo(center.x, center.y, center.z);
                     }

                     victim.setDeltaMovement(Vec3.ZERO);
                     victim.fallDistance = 0.0F;
                  }

                  if (age >= 10 && age % 10 == 0) {
                     Vec3 mid = center.add(0.0, 1.0, 0.0);

                     for (Entity e : Targeting.inRadius(level, mid, (double)radius + 1.6, x -> Targeting.canHurt(player, x))) {
                        if (ctx.hurt(e, perSqueeze) && e != victim) {
                           Targeting.push(e, e.position().subtract(center).multiply(1.0, 0.0, 1.0).normalize().scale(0.4));
                        }
                     }

                     ImpactShape coil = ImpactShape.sphere(mid, (double)radius + 1.4);
                     BlockImpactService.push(ctx, coil, new Vec3(0.0, -1.0, 0.0), 0.25);
                     BlockImpactService.damage(ctx, coil, new Vec3(0.0, -1.0, 0.0), 0.5);
                     ServerFx.send(
                        player,
                        SeiryuFx.CHAIN_ARC,
                        mid.add((double)radius + 1.5, 1.5, 0.0),
                        mid.add((double)(-radius) - 1.5, -0.5, 0.0),
                        1.0F,
                        ctx.seed() + (long)age,
                        0
                     );
                     ServerFx.shake(level, center, 0.2F, 20);
                  }

                  return false;
               }
            }
         );
      }
   }

   @Nullable
   private static LivingEntity victim(AbilityContext ctx, ServerPlayer player) {
      double reach = ctx.extra("crushReach");
      Vec3 eye = player.getEyePosition();
      Vec3 look = AimHelper.look(player);
      LivingEntity best = null;
      double bestScore = Double.MAX_VALUE;

      for (Entity e : Targeting.inRadius(ctx.level(), eye, reach, x -> x instanceof LivingEntity && Targeting.canHurt(player, x))) {
         Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
         double dist = to.length();
         double facing = dist < 1.0E-4 ? 1.0 : to.dot(look) / dist;
         double score = dist * (facing > 0.5 ? 1.0 : 2.5);
         if (score < bestScore) {
            bestScore = score;
            best = (LivingEntity)e;
         }
      }

      return best;
   }

   private static void hold(LivingEntity victim, boolean on) {
      holdAttribute(victim, Attributes.MOVEMENT_SPEED, on);
      holdAttribute(victim, Attributes.JUMP_STRENGTH, on);
      holdAttribute(victim, Attributes.FLYING_SPEED, on);
   }

   private static void holdAttribute(LivingEntity victim, Holder<Attribute> attribute, boolean on) {
      AttributeInstance instance = victim.getAttribute(attribute);
      if (instance != null) {
         if (on) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(HOLD, -1.0, Operation.ADD_MULTIPLIED_TOTAL));
         } else {
            instance.removeModifier(HOLD);
         }
      }
   }

   public static boolean isHeld(LivingEntity entity) {
      AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
      return speed != null && speed.getModifier(HOLD) != null;
   }
}
