package com.devilfruits.fruits.uo_uo_seiryu.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.AimHelper;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.combat.AbilityProjectiles;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuFx;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class BoloBreathAbility extends Ability {
   private static final int[] BURSTS = new int[]{1, 5, 9};
   private static final double SPEED = 1.5;
   private static final double GRAVITY = 0.025;
   private static final float METEOR_SIZE = 1.8F;
   private static final double METEOR_HEIGHT = 40.0;
   private static final int METEOR_FLIGHT_TICKS = 24;

   public BoloBreathAbility() {
      super(
         UoUoSeiryu.id("bolo_breath"),
         CastType.CHARGED,
         AbilityStats.builder()
            .energy(40)
            .cooldown(200)
            .mastery(4)
            .damage(7.0)
            .range(24.0)
            .knockback(0.9)
            .blockDamage(true)
            .extra("fireballs", 8.0, 1.0, 40.0, "Hybrid: fireballs per full volley.")
            .extra("meteors", 14.0, 1.0, 80.0, "Dragon: meteors per full barrage.")
            .extra("impactRadius", 2.5, 0.5, 12.0, "Explosion radius of one fireball (no vanilla explosion).")
            .extra("burnSeconds", 4.0, 0.0, 30.0, "Seconds targets keep burning.")
            .extra("spreadDegrees", 14.0, 0.0, 60.0, "How far fireballs scatter around the view.")
            .extra("meteorReach", 1.4, 0.2, 4.0, "Dragon: how far away the barrage can land, times the ability's range.")
            .extra("meteorArea", 9.0, 1.0, 40.0, "Dragon: radius of the area the meteors rain on at full charge.")
            .extra("meteorCraterRadius", 2.5, 0.5, 12.0, "Dragon: radius of the crater one meteor leaves (2.5: five blocks across).")
            .extra("meteorCraterDepth", 2.5, 0.5, 12.0, "Dragon: depth of that crater at its centre.")
            .extra("groundFireChance", 0.5, 0.0, 1.0, "Chance for each scorched spot to catch fire (0: no fire).")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.RADIAL).force(0.8).blockDamage(0.6).manual().build();
   }

   @Nullable
   @Override
   public ResourceKey<DamageType> damageType() {
      return UoUoSeiryu.FLAME_DAMAGE;
   }

   @Override
   public int maxChargeTicks() {
      return 24;
   }

   @Override
   public void onChargeStart(AbilityContext ctx) {
      ServerFx.animate(ctx.player(), ctx.animation(UoUoSeiryu.anim("hybrid/bolo_breath")));
   }

   @Override
   public void release(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      float charge = Math.max(0.25F, ctx.charge());
      ServerFx.animate(player, ctx.animation(UoUoSeiryu.anim("hybrid/bolo_breath")).withSuffix("_release"));
      boolean dragon = "dragon".equals(ctx.variant());
      if (dragon) {
         double r = ctx.extra("meteorCraterRadius");
         ctx.budget((int)Math.ceil(ctx.extra("meteors") * (Math.PI * r * r * ctx.extra("meteorCraterDepth") * 0.6 + 4.0)));
         this.meteors(ctx, charge);
      } else {
         int total = Math.max(BURSTS.length, Math.round((float)ctx.extra(dragon ? "meteors" : "fireballs") * charge));
         AbilityManager.setBusy(player, BURSTS[BURSTS.length - 1] + 4);
         RandomSource random = RandomSource.create(ctx.seed());
         double spread = Math.toRadians(ctx.extra("spreadDegrees"));
         int fired = 0;

         for (int b = 0; b < BURSTS.length; b++) {
            int count = (total - fired) / (BURSTS.length - b);
            fired += count;

            for (int i = 0; i < count; i++) {
               double yaw = (random.nextDouble() * 2.0 - 1.0) * spread;
               double pitch = (random.nextDouble() * 2.0 - 1.0) * spread * 0.5;
               long seed = ctx.seed() * 31L + (long)b * 97L + (long)i;
               ServerScheduler.later(ctx.level(), BURSTS[b] + (i & 1), () -> this.fire(ctx, yaw, pitch, seed));
            }
         }
      }
   }

   private void fire(AbilityContext ctx, double yaw, double pitch, long seed) {
      ServerPlayer player = ctx.player();
      if (player.isAlive() && !player.isRemoved()) {
         Vec3 look = AimHelper.look(player);
         Vec3 dir = look.yRot((float)yaw).xRot((float)pitch).normalize();
         this.launch(
            ctx, SeiryuCombat.mouth(player), dir.scale(1.5).add(PhysicsWorld.carrierVelocity(player)), 0.8F, seed, (int)Math.ceil((double)ctx.range() / 1.5)
         );
      }
   }

   private void meteors(AbilityContext ctx, float charge) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      AbilityManager.setBusy(player, 16);
      Vec3 aim = SeiryuCombat.rayEnd(level, player, player.getEyePosition(), AimHelper.look(player), (double)ctx.range() * ctx.extra("meteorReach"));
      Vec3 target = ground(level, player, aim);
      Vec3 mouth = SeiryuCombat.mouth(player);
      RandomSource random = RandomSource.create(ctx.seed());

      for (int i = 0; i < 4; i++) {
         Vec3 up = new Vec3((random.nextDouble() - 0.5) * 0.6, 2.4, (random.nextDouble() - 0.5) * 0.6);
         ServerFx.send(player, SeiryuFx.FIREBALL, mouth, up, 1.6F, ctx.seed() ^ (long)(20973 + i), 14);
      }

      int total = Math.max(4, Math.round((float)ctx.extra("meteors") * charge));
      double area = ctx.extra("meteorArea") * (0.55 + 0.45 * (double)charge);
      Vec3 flat = ctx.horizontalLook();

      for (int i = 0; i < total; i++) {
         double a = random.nextDouble() * (float) (Math.PI * 2);
         double r = area * Math.sqrt(random.nextDouble());
         Vec3 impact = ground(level, player, new Vec3(target.x + Math.cos(a) * r, target.y + 6.0, target.z + Math.sin(a) * r));
         Vec3 start = impact.add(flat.scale(-14.0 - random.nextDouble() * 6.0))
            .add((random.nextDouble() - 0.5) * 6.0, 40.0 + random.nextDouble() * 8.0, (random.nextDouble() - 0.5) * 6.0);
         Vec3 d = impact.subtract(start);
         int flight = 24;
         Vec3 velocity = new Vec3(d.x / (double)flight, (d.y + 0.025 * (double)flight * (double)(flight - 1) / 2.0) / (double)flight, d.z / (double)flight);
         long seed = ctx.seed() * 31L + (long)i;
         int delay = 14 + i * 2 + random.nextInt(2);
         ServerScheduler.later(level, delay, () -> this.launch(ctx, start, velocity, 1.8F, seed, flight + 10));
      }
   }

   private static Vec3 ground(ServerLevel level, ServerPlayer player, Vec3 at) {
      BlockHitResult hit = PhysicsWorld.clip(level, new ClipContext(at, at.add(0.0, -32.0, 0.0), Block.COLLIDER, Fluid.ANY, player));
      return hit.getType() == Type.MISS ? at : hit.getLocation();
   }

   private void launch(AbilityContext ctx, Vec3 start, Vec3 velocity, final float size, final long seed, int lifetime) {
      final ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      ServerFx.sendNear(level, SeiryuFx.FIREBALL, player.getId(), start, velocity, size, seed, lifetime, 128.0);
      Vec3[] state = new Vec3[]{start, velocity};
      AbilityProjectiles.Flight flight = AbilityProjectiles.track(level, new AbilityProjectiles.Flight(player, start, 0.5 * (double)size) {
         @Override
         protected void shatter(ServerLevel l, Entity by, Vec3 push) {
            ServerFx.sendNear(l, SeiryuFx.FIREBALL_IMPACT, player.getId(), this.position(), push, Math.min(size, 1.0F), seed, 0, 96.0);
         }
      });
      ServerScheduler.schedule(level, 1, age -> {
         if (flight.broken()) {
            return true;
         } else {
            Vec3 pos = state[0];
            Vec3 vel = state[1];
            Vec3 next = pos.add(vel);
            BlockHitResult block = PhysicsWorld.clip(level, new ClipContext(pos, next, Block.COLLIDER, Fluid.ANY, player));
            Vec3 end = block.getType() == Type.MISS ? next : block.getLocation();
            List<Entity> hit = SeiryuCombat.inCapsule(level, player, pos, end, 0.5 * (double)size);
            if (hit.isEmpty() && block.getType() == Type.MISS && age < lifetime) {
               state[0] = next;
               flight.moveTo(next);
               state[1] = vel.add(0.0, -0.025, 0.0);
               return false;
            } else {
               Vec3 at = hit.isEmpty() ? end : hit.get(0).getBoundingBox().getCenter();
               flight.end();
               this.burst(ctx, at, vel, size, seed, block.getType() != Type.MISS);
               return true;
            }
         }
      });
   }

   private void burst(AbilityContext ctx, Vec3 at, Vec3 vel, float size, long seed, boolean onGround) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      double radius = ctx.extra("impactRadius") * (double)size / 0.8;

      for (Entity e : Targeting.inRadius(level, at, radius, x -> Targeting.canHurt(player, x))) {
         float falloff = (float)Mth.clamp(1.0 - e.position().distanceTo(at) / (radius * 1.5), 0.4, 1.0);
         if (ctx.hurt(e, ctx.damage() * falloff)) {
            e.igniteForSeconds((float)ctx.extra("burnSeconds"));
            Vec3 out = e.position().subtract(at);
            Vec3 dir = out.lengthSqr() < 1.0E-4 ? vel.normalize() : out.normalize();
            Targeting.launch(e, dir.scale((double)(ctx.knockback() * falloff)).add(0.0, 0.3, 0.0));
         }
      }

      BlockImpactService.push(ctx, ImpactShape.sphere(at, radius), vel, size >= 1.8F ? 1.6 : 0.5);
      if (onGround) {
         boolean meteor = size >= 1.8F;
         if (meteor) {
            SeiryuCombat.blast(ctx, at, ctx.extra("meteorCraterRadius"), ctx.extra("meteorCraterDepth"), Integer.MAX_VALUE, 0.55, new Long2IntOpenHashMap(), 40);
         } else {
            SeiryuCombat.crater(ctx, at, radius * 0.5, 2, 0.3);
         }

         SeiryuCombat.scorch(ctx, at, meteor ? ctx.extra("meteorCraterRadius") + 1.5 : radius * 0.6, meteor ? 10 : 3, ctx.extra("groundFireChance"), 40);
      }

      if (size >= 1.8F) {
         ServerFx.shake(level, at, 0.35F, 40);
      }

      ServerFx.sendNear(level, SeiryuFx.FIREBALL_IMPACT, player.getId(), at, vel, size, seed, 0, 96.0);
   }
}
