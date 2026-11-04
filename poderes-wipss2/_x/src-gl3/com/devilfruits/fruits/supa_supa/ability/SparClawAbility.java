package com.devilfruits.fruits.supa_supa.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.supa_supa.BladeLimbs;
import com.devilfruits.fruits.supa_supa.SupaFx;
import com.devilfruits.fruits.supa_supa.SupaSounds;
import com.devilfruits.fruits.supa_supa.SupaSupa;
import com.devilfruits.registry.DFTags;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SparClawAbility extends Ability {
   public static final int CLAW_BEAT = 3;
   public static final int ATOMIC_BEAT = 6;
   private static final double STEP = 0.5;

   public SparClawAbility() {
      super(
         SupaSupa.id("spar_claw"),
         CastType.COMBO,
         AbilityStats.builder()
            .energy(7)
            .cooldown(14)
            .mastery(1)
            .damage(6.0)
            .range(3.4)
            .knockback(0.35)
            .blockDamage(true)
            .extra("arcDegrees", 80.0, 20.0, 180.0, "Width of a claw rake's arc in front of the user, degrees.")
            .extra("throughMastery", 6.0, 1.0, 10.0, "Mastery from which a claw rake also cuts what stands right behind its first target.")
            .extra("throughDepth", 2.5, 0.0, 8.0, "How far behind the first target the claw's cut still reaches, blocks.")
            .extra("atomicDamage", 14.0, 0.0, 200.0, "Damage of the Atomic Spar X cut (the third step), before mastery.")
            .extra("atomicRange", 6.5, 1.0, 24.0, "Reach of the Atomic Spar X cut, blocks.")
            .extra("atomicHalfWidth", 2.2, 0.5, 6.0, "Half the width (and height) of the Atomic Spar X, blocks.")
            .extra("atomicMaxBlocks", 70.0, 0.0, 1024.0, "Most blocks one Atomic Spar cuts out.")
            .build()
      );
   }

   @Override
   public boolean coatable() {
      return true;
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.DIRECTIONAL).force(0.25).blockDamage(1.0).manual().build();
   }

   @Override
   public int comboWindowTicks() {
      return 16;
   }

   @Override
   public float comboStepCost(int step) {
      return step == 2 ? this.energyCost(1.0F) * 2.4F : this.energyCost(1.0F);
   }

   @Override
   public void comboStep(AbilityContext ctx, int step) {
      ServerPlayer player = ctx.player();
      if (step < 2) {
         boolean left = step == 1;
         BladeLimbs.arms(player, BladeLimbs.ArmMode.CLAWS, true, true, this.comboWindowTicks() + 10);
         ServerFx.animate(player, ctx.animation(SupaSupa.id(left ? "claw_rake_left" : "claw_rake_right")));
         ServerScheduler.later(ctx.level(), 3, () -> {
            if (!player.isRemoved() && SupaSupa.active(player)) {
               this.rake(ctx, left);
            }
         });
      } else {
         BladeLimbs.arms(player, BladeLimbs.ArmMode.X_BLADES, true, true, 20);
         ServerFx.animate(player, ctx.animation(SupaSupa.id("atomic_spar")));
         ServerScheduler.later(ctx.level(), 6, () -> {
            if (!player.isRemoved() && SupaSupa.active(player)) {
               this.atomicSpar(ctx);
            }
         });
      }
   }

   private void rake(AbilityContext ctx, boolean left) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      Vec3 eye = ctx.eye();
      Vec3 look = ctx.look();
      double reach = (double)ctx.range();
      double half = Math.toRadians(ctx.extra("arcDegrees") * 0.5);
      List<Entity> hit = Targeting.inCone(level, eye, look, reach, half, ex -> Targeting.canHurt(player, ex));
      hit.sort(Comparator.comparingDouble(ex -> ex.distanceToSqr(eye)));
      IntSet struck = new IntOpenHashSet();
      boolean through = false;
      float damage = ctx.damage();

      for (Entity e : hit) {
         if (struck.add(e.getId()) && ctx.hurt(e, damage)) {
            Targeting.launch(e, look.scale((double)ctx.knockback()).add(0.0, 0.12, 0.0));
            cutFx(level, e, look, damage);
         }
      }

      if (!hit.isEmpty() && ctx.mastery() >= (int)ctx.extra("throughMastery")) {
         Entity first = hit.get(0);
         Vec3 from = first.getBoundingBox().getCenter();
         Vec3 to = from.add(look.scale(ctx.extra("throughDepth") + (double)first.getBbWidth() * 0.5));

         for (Entity ex : Targeting.inCapsule(level, player, from, to, 0.6)) {
            if (Targeting.canHurt(player, ex) && struck.add(ex.getId()) && ctx.hurt(ex, damage * 0.8F)) {
               through = true;
               Targeting.launch(ex, look.scale((double)ctx.knockback()).add(0.0, 0.1, 0.0));
               cutFx(level, ex, look, damage * 0.8F);
            }
         }
      }

      level.playSound(null, player.getX(), player.getY() + 1.2, player.getZ(), SupaSounds.CLAW_SWING.get(), SoundSource.PLAYERS, 0.9F, left ? 1.1F : 1.0F);
      if (!struck.isEmpty()) {
         level.playSound(null, player.getX(), player.getY() + 1.2, player.getZ(), SupaSounds.CLAW_HIT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
      }

      ServerFx.sendNear(level, SupaFx.CLAW, player.getId(), eye, look, (float)reach, ctx.seed(), (left ? 1 : 0) | (through ? 2 : 0), 64.0);
   }

   private static void cutFx(ServerLevel level, Entity e, Vec3 look, float damage) {
      ServerFx.sendNear(level, SupaFx.CUT_HIT, e.getId(), e.getBoundingBox().getCenter(), look, damage, (long)e.getId(), 0, 64.0);
   }

   private void atomicSpar(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      Vec3 look = ctx.look();
      Vec3 side = horizontalSide(look);
      Vec3 up = side.cross(look).normalize();
      double range = ctx.extra("atomicRange");
      double halfWidth = ctx.extra("atomicHalfWidth");
      Vec3 centre = player.position().add(0.0, (double)player.getBbHeight() * 0.62, 0.0).add(look.scale(0.6));
      float damage = (float)(ctx.extra("atomicDamage") * (double)ctx.damage() / Math.max(0.001, ctx.config().damage()));
      IntSet struck = new IntOpenHashSet();

      for (int i = 1; i <= Mth.ceil(range / 0.5); i++) {
         Vec3 at = centre.add(look.scale((double)i * 0.5));

         for (Entity e : Targeting.inCapsule(level, player, at.add(side.scale(-halfWidth)), at.add(side.scale(halfWidth)), halfWidth * 0.75)) {
            if (Targeting.canHurt(player, e) && struck.add(e.getId()) && ctx.hurt(e, damage)) {
               Targeting.launch(e, look.scale((double)ctx.knockback() * 1.6).add(0.0, 0.25, 0.0));
               cutFx(level, e, look, damage);
            }
         }
      }

      if (ctx.blockDamageAllowed()) {
         CastBudget budget = new CastBudget((int)ctx.extra("atomicMaxBlocks"), (Integer)CoreConfig.MAX_DEBRIS_ENTITIES_PER_CAST.get());
         cutX(level, player, centre, look, side, up, range, halfWidth, budget);

         for (int d = -1; d <= 1; d += 2) {
            Vec3 diag = side.scale(halfWidth).add(up.scale(halfWidth * (double)d));
            ImpactShape cut = ImpactShape.capsule(centre.add(diag.scale(-1.0)).add(look.scale(range * 0.5)), centre.add(diag).add(look.scale(range * 0.5)), 0.6);
            BlockImpactService.damage(ctx, cut, look, 1.0, true, budget);
         }

         BlockImpactService.push(ctx, ImpactShape.capsule(centre, centre.add(look.scale(range)), halfWidth), look, 0.25);
      }

      level.playSound(null, player.getX(), player.getY() + 1.2, player.getZ(), SupaSounds.ATOMIC_SPAR.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
      ServerFx.sendNear(level, SupaFx.ATOMIC_SPAR, player.getId(), centre, look, (float)range, ctx.seed(), (int)Math.round(halfWidth * 16.0), 96.0);
   }

   public static int cutX(ServerLevel level, ServerPlayer player, Vec3 centre, Vec3 look, Vec3 side, Vec3 up, double range, double halfWidth, CastBudget budget) {
      LongSet tried = new LongOpenHashSet();
      int cut = 0;
      int lanes = Math.max(1, (int)Math.round(halfWidth / 0.35));
      MutableBlockPos pos = new MutableBlockPos();

      for (int d = -1; d <= 1; d += 2) {
         for (int k = -lanes; k <= lanes; k++) {
            double s = halfWidth * (double)k / (double)lanes;
            Vec3 lane = centre.add(side.scale(s)).add(up.scale(s * (double)d));

            for (double t = 0.8; t <= range; t += 0.5) {
               Vec3 p = lane.add(look.scale(t));
               pos.set(Mth.floor(p.x), Mth.floor(p.y), Mth.floor(p.z));
               BlockState state = level.getBlockState(pos);
               if (!state.isAir()) {
                  if (state.is(DFTags.RESISTS_TECHNIQUES) || !TerrainDestruction.canAffect(level, player, pos, state, true)) {
                     break;
                  }

                  if (tried.add(pos.asLong())) {
                     Vec3 fling = side.scale(Math.signum(s) * 0.12).add(look.scale(0.18)).add(0.0, 0.25, 0.0);
                     if (TerrainDestruction.tearOut(level, player, pos.immutable(), fling, budget, 2 + (int)(t * 2.0), true, false)) {
                        cut++;
                     }

                     if (!budget.hasBlocks()) {
                        return cut;
                     }
                  }
               }
            }
         }
      }

      return cut;
   }

   static Vec3 horizontalSide(Vec3 look) {
      Vec3 flat = new Vec3(look.x, 0.0, look.z);
      if (flat.lengthSqr() < 1.0E-4) {
         flat = new Vec3(0.0, 0.0, 1.0);
      }

      flat = flat.normalize();
      return new Vec3(-flat.z, 0.0, flat.x);
   }
}
