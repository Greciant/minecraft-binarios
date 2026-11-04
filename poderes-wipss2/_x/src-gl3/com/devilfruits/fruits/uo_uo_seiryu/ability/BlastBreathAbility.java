package com.devilfruits.fruits.uo_uo_seiryu.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.uo_uo_seiryu.DragonHead;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuFx;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class BlastBreathAbility extends Ability {
   public BlastBreathAbility() {
      super(
         UoUoSeiryu.id("blast_breath"),
         CastType.CHARGED,
         AbilityStats.builder()
            .energy(18)
            .cooldown(140)
            .mastery(1)
            .damage(9.0)
            .range(9.0)
            .knockback(0.8)
            .blockDamage(true)
            .extra("minCharge", 0.35, 0.0, 1.0, "Charge used by a quick tap.")
            .extra("width", 1.0, 0.2, 6.0, "Half-width of the breath in blocks (x form range factor).")
            .extra("burnSeconds", 4.0, 0.0, 30.0, "Seconds targets keep burning.")
            .extra("hybridBeamTicks", 30.0, 5.0, 200.0, "Hybrid: beam duration at full charge.")
            .extra("dragonBeamTicks", 120.0, 5.0, 300.0, "Dragon: beam duration at full charge.")
            .extra("dragonBeamLength", 200.0, 10.0, 400.0, "Dragon: beam length in blocks at full charge.")
            .extra("beamBlocksPerTick", 2.0, 0.0, 16.0, "Hybrid: blocks torn up at the beam's end each tick.")
            .extra("dragonBlastRadius", 3.0, 1.0, 64.0, "Dragon: radius of the burnt crater where the beam lands (3: six blocks across).")
            .extra("dragonBlastDepth", 3.0, 1.0, 32.0, "Dragon: depth of that crater at its centre.")
            .extra("dragonBlastBlocksPerTick", 24.0, 1.0, 1024.0, "Dragon: blocks the crater takes each tick.")
            .extra("dragonBlastBlocks", 2000.0, 0.0, 100000.0, "Dragon: most blocks one breath may tear up (its own limit instead of maxBlocksPerCast).")
            .extra("groundFireChance", 0.35, 0.0, 1.0, "Chance for each scorched spot to catch fire (0: the breath sets no fire).")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.SUSTAINED).force(0.6).blockDamage(0.5).manual().build();
   }

   @Nullable
   @Override
   public ResourceKey<DamageType> damageType() {
      return UoUoSeiryu.FLAME_DAMAGE;
   }

   @Override
   public int maxChargeTicks() {
      return 30;
   }

   @Override
   public float energyCost(float charge) {
      return (float)this.config().energyCost() * (1.0F + charge);
   }

   @Override
   public void onChargeStart(AbilityContext ctx) {
      ServerFx.animate(ctx.player(), ctx.animation(UoUoSeiryu.anim("blast_breath")));
   }

   @Override
   public void release(AbilityContext ctx) {
      float charge = Math.max(ctx.charge(), (float)ctx.extra("minCharge"));
      ServerFx.animate(ctx.player(), ctx.animation(UoUoSeiryu.anim("blast_breath")).withSuffix("_release"));
      if (ctx.variant().isEmpty()) {
         this.burst(ctx, charge);
      } else {
         this.beam(ctx, charge, "dragon".equals(ctx.variant()));
      }
   }

   private void burst(AbilityContext ctx, float charge) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      Vec3 from = SeiryuCombat.mouth(player);
      double length = (double)ctx.range() * (0.5 + 0.5 * (double)charge);
      Vec3 dir = DragonHead.aim(level, player, from, length, 1.0F);
      Vec3 end = SeiryuCombat.rayEnd(level, player, from, dir, length);
      double width = ctx.extra("width") * (0.6 + 0.4 * (double)charge);

      for (Entity e : SeiryuCombat.inCapsule(level, player, from, end, width)) {
         if (ctx.hurt(e, ctx.damage() * (0.4F + 0.6F * charge))) {
            e.igniteForSeconds((float)ctx.extra("burnSeconds"));
            Targeting.launch(e, dir.scale((double)(ctx.knockback() * charge)).add(0.0, 0.2, 0.0));
         }
      }

      BlockImpactService.push(ctx, ImpactShape.capsule(from, end, width), dir, 0.4 + 0.6 * (double)charge);
      ServerFx.send(player, SeiryuFx.BREATH, from, dir, (float)from.distanceTo(end), ctx.seed(), 0);
      ServerFx.send(player, SeiryuFx.BREATH_IMPACT, end, dir, 0.6F + charge, ctx.seed(), 0);
      if (charge >= 0.8F && !onWater(level, end)) {
         SeiryuCombat.crater(ctx, end, 1.2, 2, 0.3);
      }

      if (!onWater(level, end)) {
         SeiryuCombat.scorch(ctx, end, 1.5 + (double)charge, 4, ctx.extra("groundFireChance"), 40);
      }
   }

   private void beam(AbilityContext ctx, float charge, boolean dragon) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      int duration = Math.max(8, Math.round((float)ctx.extra(dragon ? "dragonBeamTicks" : "hybridBeamTicks") * charge));
      double length = (dragon ? ctx.extra("dragonBeamLength") : (double)ctx.range()) * (0.6 + 0.4 * (double)charge);
      double width = ctx.extra("width") * (dragon ? 2.2 : 1.4);
      double blastRadius = ctx.extra("dragonBlastRadius") * (0.5 + 0.5 * (double)charge);
      double blastDepth = ctx.extra("dragonBlastDepth") * (0.5 + 0.5 * (double)charge);
      int blastPerTick = (int)ctx.extra("dragonBlastBlocksPerTick");
      Long2IntOpenHashMap ground = new Long2IntOpenHashMap();
      double fireChance = ctx.extra("groundFireChance");
      if (dragon) {
         ctx.budget((int)ctx.extra("dragonBlastBlocks"));
      }

      float tickDamage = ctx.damage() * 0.3F;
      int blocksPerTick = (int)ctx.extra("beamBlocksPerTick") * (dragon ? 2 : 1);
      AbilityManager.setBusy(player, duration + 2);
      Vec3 mouth = SeiryuCombat.mouth(player);
      ServerFx.send(player, SeiryuFx.BREATH, mouth, DragonHead.aim(level, player, mouth, length, 1.0F), (float)length, ctx.seed(), duration);
      Map<Entity, Integer> lastHit = new HashMap<>();
      ServerScheduler.schedule(level, 1, age -> {
         if (player.isAlive() && !player.isRemoved() && player.level() == level && age <= duration) {
            Vec3 from = SeiryuCombat.mouth(player);
            Vec3 dir = DragonHead.aim(level, player, from, length, 1.0F);
            Vec3 end = SeiryuCombat.rayEnd(level, player, from, dir, length);

            for (Entity e : SeiryuCombat.inCapsule(level, player, from, end, width)) {
               Integer last = lastHit.get(e);
               if (last == null || age - last >= 4) {
                  lastHit.put(e, age);
                  if (ctx.hurt(e, tickDamage)) {
                     e.igniteForSeconds((float)ctx.extra("burnSeconds"));
                     Targeting.push(e, dir.scale((double)ctx.knockback() * 0.15));
                  }
               }
            }

            BlockImpactService.push(ctx, ImpactShape.capsule(from, end, width), dir, dragon ? 0.08 : 0.05);
            if (end.distanceToSqr(from) < length * length - 0.5) {
               double radius = dragon ? blastRadius : 1.4;
               if (!onWater(level, end)) {
                  if (dragon) {
                     SeiryuCombat.blast(ctx, end, radius, blastDepth, blastPerTick, 0.6, ground, 200);
                  } else {
                     SeiryuCombat.crater(ctx, end, 1.4, blocksPerTick, 0.45);
                  }

                  SeiryuCombat.scorch(ctx, end, radius + 1.0, dragon ? 3 : 2, fireChance, dragon ? 200 : 40);
               }

               if (age % 4 == 0) {
                  float size = dragon ? 2.2F + (float)radius * 0.12F : 1.4F;
                  ServerFx.sendNear(level, SeiryuFx.BREATH_IMPACT, player.getId(), end, dir, size, ctx.seed() + (long)age, 1, dragon ? 256.0 : 96.0);
               }
            }

            if (age % 10 == 0) {
               ServerFx.shake(level, end, dragon ? 0.5F : 0.3F, 24);
            }

            return false;
         } else {
            if (dragon && age > duration) {
               ServerFx.stopAnimation(player);
            }

            return true;
         }
      });
   }

   private static boolean onWater(ServerLevel level, Vec3 at) {
      return !level.getFluidState(BlockPos.containing(at.x, at.y - 0.05, at.z)).isEmpty();
   }
}
