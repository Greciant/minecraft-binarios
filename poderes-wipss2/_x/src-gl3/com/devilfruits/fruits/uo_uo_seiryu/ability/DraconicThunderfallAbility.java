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
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuFx;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class DraconicThunderfallAbility extends Ability {
   public static final int RELEASE_TICKS = 4;
   public static final int VOLLEY_INTERVAL = 2;
   public static final int MARK_LEAD = 3;
   private static final Map<UUID, DraconicThunderfallAbility.State> STATES = new HashMap<>();

   public DraconicThunderfallAbility() {
      super(
         UoUoSeiryu.id("draconic_thunderfall"),
         CastType.HELD,
         AbilityStats.builder()
            .energy(40)
            .cooldown(300)
            .mastery(6)
            .damage(1.5)
            .range(9.0)
            .knockback(0.5)
            .blockDamage(true)
            .extra("minStrikes", 6.0, 1.0, 64.0, "Strikes even a short press calls down.")
            .extra("maxStrikes", 18.0, 1.0, 64.0, "Strikes of a press held to the end.")
            .extra("strikeInterval", 4.0, 1.0, 40.0, "Ticks between strikes while the key is held.")
            .extra("strikeRadius", 2.2, 0.5, 8.0, "Radius each strike hurts round its point.")
            .extra("craterBlocks", 3.0, 0.0, 32.0, "Floor blocks one strike tears out (they heal).")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults())
         .impact(PhysicsProfile.Impact.RADIAL)
         .force(4.0)
         .reaction(PhysicsProfile.Reaction.NONE)
         .blockDamage(0.5)
         .manual()
         .build();
   }

   @Nullable
   @Override
   public ResourceKey<DamageType> damageType() {
      return UoUoSeiryu.THUNDER_DAMAGE;
   }

   @Override
   public int maxChannelTicks() {
      return (int)(this.config().extra("maxStrikes") * this.config().extra("strikeInterval")) + 1;
   }

   @Override
   public void onChannelStart(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      DraconicThunderfallAbility.State st = new DraconicThunderfallAbility.State();
      st.seed = ctx.seed();
      st.next = 2;
      STATES.put(player.getUUID(), st);
      HybridCombat.root(player, this.maxChannelTicks() + 2);
      ServerFx.animate(player, UoUoSeiryu.anim("hybrid/thunderfall"));
      ServerFx.send(player, SeiryuFx.THUNDERFALL, player.position(), Vec3.ZERO, ctx.range(), st.seed, this.maxChannelTicks());
      this.mark(ctx, st, st.next);
   }

   @Override
   public boolean channelTick(AbilityContext ctx, int ticks) {
      DraconicThunderfallAbility.State st = STATES.get(ctx.player().getUUID());
      if (st == null) {
         return false;
      } else {
         if (ticks >= st.next && st.strikes < this.maxStrikes(ctx)) {
            this.strike(ctx, st);
            st.next = ticks + (int)ctx.extra("strikeInterval");
         } else if (ticks == st.next - 3 && st.strikes < this.maxStrikes(ctx)) {
            this.mark(ctx, st, 3);
         }

         return st.strikes < this.maxStrikes(ctx);
      }
   }

   @Override
   public void onChannelEnd(AbilityContext ctx, int ticks, boolean interrupted) {
      ServerPlayer player = ctx.player();
      DraconicThunderfallAbility.State st = STATES.remove(player.getUUID());
      HybridCombat.unroot(player);
      if (st != null) {
         if (interrupted) {
            ServerFx.send(player, SeiryuFx.THUNDERFALL_END, player.position(), Vec3.ZERO, 0.0F, st.seed, st.strikes);
         } else {
            int owed = Math.max(0, (int)ctx.extra("minStrikes") - st.strikes);
            int total = 4 + owed * 2;
            AbilityManager.setBusy(player, total + 2);
            ServerFx.animate(player, UoUoSeiryu.anim("hybrid/thunderfall_release"));
            ServerScheduler.schedule(ctx.level(), 1, age -> {
               if (player.isAlive() && !player.isRemoved() && player.level() == ctx.level()) {
                  if (age >= 4 && st.strikes < (int)ctx.extra("minStrikes") && (age - 4) % 2 == 0) {
                     this.strike(ctx, st);
                  }

                  if (age >= total) {
                     ServerFx.send(player, SeiryuFx.THUNDERFALL_END, player.position(), Vec3.ZERO, ctx.range(), st.seed, st.strikes);
                     ServerFx.shake(ctx.level(), player.position(), 0.7F, 40);
                     return true;
                  } else {
                     return false;
                  }
               } else {
                  return true;
               }
            });
         }
      }
   }

   private int maxStrikes(AbilityContext ctx) {
      return Math.max((int)ctx.extra("minStrikes"), (int)ctx.extra("maxStrikes"));
   }

   public int strikes(ServerPlayer player) {
      DraconicThunderfallAbility.State st = STATES.get(player.getUUID());
      return st == null ? -1 : st.strikes;
   }

   private DraconicThunderfallAbility.Aim aim(AbilityContext ctx, DraconicThunderfallAbility.State st) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      Vec3 center = player.position();
      float range = ctx.range();
      List<Entity> targets = Targeting.inRadius(level, center.add(0.0, 1.0, 0.0), (double)range, x -> Targeting.canHurt(player, x));
      targets.sort((ax, b) -> Double.compare(ax.distanceToSqr(player), b.distanceToSqr(player)));
      Entity aimed = targets.isEmpty() ? null : targets.get(st.strikes % targets.size());
      Vec3 point;
      if (aimed != null) {
         point = aimed.position();
      } else {
         long h = st.seed + (long)st.strikes * -7046029254386353131L;
         h = (h ^ h >>> 31) * -4658895280553007687L;
         double a = (double)(h >>> 11 & 65535L) / 65536.0 * Math.PI * 2.0;
         double r = 3.0 + (double)(h >>> 29 & 65535L) / 65536.0 * Math.max(0.5, (double)range - 3.0);
         point = center.add(Math.cos(a) * r, 0.0, Math.sin(a) * r);
      }

      BlockPos ground = TerrainDestruction.findSurface(level, Mth.floor(point.x), Mth.floor(point.y) + 2, Mth.floor(point.z), 4, 8);
      return new DraconicThunderfallAbility.Aim(ground != null ? Vec3.atBottomCenterOf(ground.above()) : point, aimed);
   }

   private void mark(AbilityContext ctx, DraconicThunderfallAbility.State st, int lead) {
      ServerFx.send(ctx.player(), SeiryuFx.THUNDERFALL_MARK, this.aim(ctx, st).at(), Vec3.ZERO, (float)lead, st.seed + (long)st.strikes + 1L, st.strikes + 1);
   }

   private void strike(AbilityContext ctx, DraconicThunderfallAbility.State st) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      Vec3 center = player.position();
      DraconicThunderfallAbility.Aim aimHere = this.aim(ctx, st);
      Vec3 at = aimHere.at();
      Entity aimed = aimHere.aimed();
      st.strikes++;
      float damage = HybridCombat.hit(ctx);
      double radius = ctx.extra("strikeRadius");
      List<Entity> hit = Targeting.inRadius(level, at.add(0.0, 1.0, 0.0), radius, x -> Targeting.canHurt(player, x) && x != aimed);
      if (aimed != null) {
         hit.add(0, aimed);
      }

      for (Entity e : hit) {
         if (HybridCombat.strike(ctx, e, damage)) {
            Vec3 out = e.position().subtract(at);
            Vec3 flat = new Vec3(out.x, 0.0, out.z);
            Vec3 dir = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize();
            Targeting.launch(e, dir.scale((double)ctx.knockback()).add(0.0, 0.45, 0.0));
         }
      }

      BlockImpactService.push(ctx, ImpactShape.sphere(at, radius), new Vec3(0.0, -1.0, 0.0), 0.6, PhysicsProfile.Impact.RADIAL);
      SeiryuCombat.crater(ctx, at.add(0.0, -0.5, 0.0), 1.2, (int)ctx.extra("craterBlocks"), 0.7);
      ServerFx.send(player, SeiryuFx.THUNDERFALL_STRIKE, at, center, (float)st.strikes, st.seed + (long)st.strikes, this.maxStrikes(ctx));
   }

   private static record Aim(Vec3 at, @Nullable Entity aimed) {
   }

   private static final class State {
      int strikes;
      int next;
      long seed;
   }
}
