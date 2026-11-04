package com.devilfruits.fruits.supa_supa.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.supa_supa.BladeLimbs;
import com.devilfruits.fruits.supa_supa.SupaFx;
import com.devilfruits.fruits.supa_supa.SupaSounds;
import com.devilfruits.fruits.supa_supa.SupaSupa;
import com.devilfruits.registry.DFTags;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SpiralHollowAbility extends Ability {
   private static final Map<UUID, CastBudget> BUDGETS = new HashMap<>();

   public SpiralHollowAbility() {
      super(
         SupaSupa.id("spiral_hollow"),
         CastType.HELD,
         AbilityStats.builder()
            .energy(16)
            .cooldown(120)
            .mastery(5)
            .damage(2.6)
            .range(4.2)
            .knockback(0.12)
            .blockDamage(true)
            .extra("maxSeconds", 4.0, 0.5, 20.0, "Longest Spiral Hollow spins in one go, seconds.")
            .extra("hitIntervalTicks", 3.0, 1.0, 20.0, "Ticks between two hits of the saw rings.")
            .extra("coneDegrees", 32.0, 5.0, 90.0, "Half-angle of the cone the rings cut, degrees.")
            .extra("advanceSpeed", 0.14, 0.0, 1.0, "How fast the user bores forward while spinning, blocks per tick.")
            .extra("drainPerSecond", 6.0, 0.0, 100.0, "Energy Spiral Hollow drains every second.")
            .extra("blocksPerTick", 3.0, 0.0, 64.0, "Most blocks the rings shred per tick.")
            .extra("maxBlocks", 80.0, 0.0, 1024.0, "Most blocks one Spiral Hollow shreds.")
            .build()
      );
   }

   @Override
   public boolean coatable() {
      return true;
   }

   @Override
   public int maxChannelTicks() {
      return (int)(this.config().extra("maxSeconds") * 20.0);
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.DIRECTIONAL).force(0.15).blockDamage(1.0).manual().build();
   }

   @Override
   public void onChannelStart(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      BladeLimbs.arms(player, BladeLimbs.ArmMode.SAW, true, true, 0);
      ServerFx.animate(player, ctx.animation(SupaSupa.id("saw_stance")));
      BUDGETS.put(player.getUUID(), new CastBudget((int)ctx.extra("maxBlocks"), (Integer)CoreConfig.MAX_DEBRIS_ENTITIES_PER_CAST.get()));
      ServerFx.sendNear(ctx.level(), SupaFx.SPIRAL, player.getId(), ctx.eye(), ctx.look(), ctx.range(), ctx.seed(), this.maxChannelTicks(), 64.0);
   }

   @Override
   public boolean channelTick(AbilityContext ctx, int ticks) {
      ServerPlayer player = ctx.player();
      PlayerFruitData data = ctx.data();
      if (!AbilityManager.freeEnergy(player)) {
         data.setEnergy(Math.max(0.0F, data.energy() - (float)ctx.extra("drainPerSecond") / 20.0F));
         if (data.energy() <= 0.0F) {
            return false;
         }
      }

      ServerLevel level = ctx.level();
      Vec3 look = ctx.look();
      Vec3 flat = new Vec3(look.x, 0.0, look.z);
      flat = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize();
      double advance = ctx.extra("advanceSpeed");
      if (advance > 0.0 && player.onGround() && ticks > 4) {
         Vec3 v = player.getDeltaMovement();
         Targeting.moveSelf(player, new Vec3(flat.x * advance, v.y, flat.z * advance));
      }

      int interval = Math.max(1, (int)ctx.extra("hitIntervalTicks"));
      Vec3 from = ctx.eye().add(0.0, -0.35, 0.0);
      if (ticks % interval == 0) {
         double half = Math.toRadians(ctx.extra("coneDegrees"));

         for (Entity e : Targeting.inCone(level, from, look, (double)ctx.range(), half, x -> Targeting.canHurt(player, x))) {
            e.invulnerableTime = 0;
            if (ctx.hurt(e, ctx.damage())) {
               Targeting.launch(e, e.getDeltaMovement().scale(0.3).add(look.scale((double)ctx.knockback())).add(0.0, 0.04, 0.0));
               ServerFx.sendNear(level, SupaFx.SPIRAL_BITE, e.getId(), e.getBoundingBox().getCenter(), look, 0.0F, (long)e.getId(), 0, 48.0);
            }
         }

         if (ticks % (interval * 3) == 0) {
            level.playSound(
               null,
               player.getX(),
               player.getY() + 1.0,
               player.getZ(),
               SupaSounds.SPIRAL_GRIND.get(),
               SoundSource.PLAYERS,
               0.5F,
               0.9F + player.getRandom().nextFloat() * 0.2F
            );
         }
      }

      if (ctx.blockDamageAllowed()) {
         CastBudget budget = BUDGETS.computeIfAbsent(
            player.getUUID(), k -> new CastBudget((int)ctx.extra("maxBlocks"), (Integer)CoreConfig.MAX_DEBRIS_ENTITIES_PER_CAST.get())
         );
         int shredded = shred(level, player, from, look, Math.min(2.6, (double)ctx.range() * 0.6), (int)ctx.extra("blocksPerTick"), budget);
         if (ticks % interval == 0) {
            ImpactShape cone = ImpactShape.cone(from, look, Math.min(2.6, (double)ctx.range() * 0.6), Math.toRadians(30.0));
            BlockImpactService.damage(ctx, cone, look, 0.5, true, budget);
            BlockImpactService.push(ctx, cone, look, 0.05);
         }

         if (shredded > 0 && ticks % 2 == 0) {
            Vec3 at = from.add(look.scale(1.4));
            ServerFx.sendNear(level, SupaFx.SPIRAL_BITE, player.getId(), at, look, 1.0F, ctx.seed() + (long)ticks, shredded, 48.0);
         }
      }

      return true;
   }

   public static int shred(ServerLevel level, ServerPlayer player, Vec3 from, Vec3 look, double reach, int perTick, CastBudget budget) {
      if (perTick > 0 && budget.hasBlocks()) {
         List<BlockPos> candidates = new ArrayList<>();
         MutableBlockPos pos = new MutableBlockPos();
         int r = Mth.ceil(reach);
         BlockPos base = BlockPos.containing(from);

         for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
               for (int dz = -r; dz <= r; dz++) {
                  pos.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                  Vec3 c = Vec3.atCenterOf(pos).subtract(from);
                  double along = c.dot(look);
                  if (!(along < 0.3) && !(along > reach)) {
                     double off = c.subtract(look.scale(along)).length();
                     if (!(off > 0.55 + along * 0.35)) {
                        BlockState state = level.getBlockState(pos);
                        if (!state.isAir() && !state.is(DFTags.RESISTS_TECHNIQUES) && !state.getCollisionShape(level, pos).isEmpty()) {
                           candidates.add(pos.immutable());
                        }
                     }
                  }
               }
            }
         }

         candidates.sort(Comparator.comparingDouble(px -> Vec3.atCenterOf(px).distanceToSqr(from)));
         int done = 0;

         for (BlockPos p : candidates) {
            if (done >= perTick || !budget.hasBlocks()) {
               break;
            }

            Vec3 fling = look.scale(-0.12).add((level.random.nextDouble() - 0.5) * 0.3, 0.2, (level.random.nextDouble() - 0.5) * 0.3);
            if (TerrainDestruction.tearOut(level, player, p, fling, budget, 4 + done, true, false)) {
               done++;
            }
         }

         return done;
      } else {
         return 0;
      }
   }

   @Override
   public void onChannelEnd(AbilityContext ctx, int ticks, boolean interrupted) {
      ServerPlayer player = ctx.player();
      BladeLimbs.clearArms(player, BladeLimbs.ArmMode.SAW);
      ServerFx.stopAnimation(player);
      BUDGETS.remove(player.getUUID());
   }

   public static void forget(UUID player) {
      BUDGETS.remove(player);
   }
}
