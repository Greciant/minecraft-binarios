package com.devilfruits.fruits.yomi_yomi.ability;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.combat.Stun;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.yomi_yomi.YomiFx;
import com.devilfruits.fruits.yomi_yomi.YomiSounds;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags.Blocks;

public class EarthCrushingAbility extends KingAbility {
   public EarthCrushingAbility() {
      super(
         "earth_crushing",
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(0)
            .cooldown(240)
            .mastery(9)
            .damage(18.0)
            .range(6.6)
            .knockback(0.0)
            .blockDamage(true)
            .extra("stunTicks", 60.0, 0.0, 400.0, "Ticks the shockwave stuns everything it reaches (3 s).")
            .extra("push", 0.8, 0.0, 3.0, "Speed the wave throws what it reaches away from the slam.")
            .extra("launch", 0.55, 0.0, 3.0, "Upward speed the wave throws what it reaches.")
            .build()
      );
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      AbilityManager.setBusy(player, 37);
      Vec3 dir = ctx.horizontalLook();
      Vec3 apex = player.position().add(dir.scale(1.5));
      ServerFx.send(player, YomiFx.KING_CRUSH, apex, dir, ctx.range(), ctx.seed(), 0);
      Set<UUID> struck = new HashSet<>();
      CastBudget budget = ctx.budget();
      ServerScheduler.schedule(level, 0, age -> {
         if (!present(player, level)) {
            return true;
         } else {
            int t = age - 25;
            if (t == 0) {
               level.playSound(null, apex.x, apex.y, apex.z, YomiSounds.KING_SLAM.get(), SoundSource.PLAYERS, 2.0F, 0.7F);
               level.playSound(null, apex.x, apex.y, apex.z, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.4F, 0.55F);
            }

            if (t >= 0 && t < 8) {
               wave(ctx, apex, dir, band((double)ctx.range(), t), band((double)ctx.range(), t + 1), struck, budget);
            }

            return t >= 8;
         }
      });
   }

   public static double band(double range, int t) {
      float k = Mth.clamp((float)t / 8.0F, 0.0F, 1.0F);
      return 0.5 + (range - 0.5) * (double)(1.0F - (1.0F - k) * (1.0F - k));
   }

   public static List<Entity> wave(AbilityContext ctx, Vec3 apex, Vec3 dir, double near, double far, Set<UUID> struck, CastBudget budget) {
      ServerPlayer player = ctx.player();
      List<Entity> hit = new ArrayList<>();

      for (Entity e : inRing(player, apex, near, far)) {
         if (struck.add(e.getUUID())) {
            hit.add(e);
         }
      }

      int stun = (int)ctx.extra("stunTicks");

      for (Entity ex : hit) {
         breakShield(ex);
         ex.invulnerableTime = 0;
         if (ctx.hurt(ex, ctx.damage())) {
            Targeting.launch(ex, away(apex, ex, dir).scale(ctx.extra("push")).add(0.0, ctx.extra("launch"), 0.0));
            if (ex instanceof LivingEntity) {
               LivingEntity living = (LivingEntity)ex;
               if (living.isAlive() && stun > 0) {
                  stunOnLanding(ctx, living, stun);
               }
            }
         }
      }

      if (ctx.blockDamageAllowed()) {
         shatter(ctx, apex, near, far, budget);
      }

      return hit;
   }

   private static void stunOnLanding(AbilityContext ctx, LivingEntity target, int ticks) {
      ServerScheduler.schedule(ctx.level(), 0, age -> {
         if (target.isAlive() && !target.isRemoved()) {
            if (age >= 3 && (target.onGround() || age >= 20)) {
               Stun.apply(target, ticks);
               if (target instanceof ServerPlayer victim) {
                  ServerFx.send(victim, YomiFx.KING_ROAR_STUN, victim.position(), Vec3.ZERO, (float)ticks, ctx.seed(), 0);
               }

               return true;
            } else {
               return false;
            }
         } else {
            return true;
         }
      });
   }

   public static List<Entity> inRing(ServerPlayer player, Vec3 apex, double near, double far) {
      List<Entity> out = new ArrayList<>();

      for (Entity e : Targeting.inRadius(player.serverLevel(), apex, far + 2.0, x -> x != player && Targeting.canHurt(player, x))) {
         double d = Math.sqrt((e.getX() - apex.x) * (e.getX() - apex.x) + (e.getZ() - apex.z) * (e.getZ() - apex.z));
         double r = (double)e.getBbWidth() * 0.5;
         if (!(d + r < near - 0.6) && !(d - r > far) && !(e.getY() < apex.y - 1.5) && !(e.getY() > apex.y + 3.0)) {
            out.add(e);
         }
      }

      return out;
   }

   private static void shatter(AbilityContext ctx, Vec3 apex, double near, double far, CastBudget budget) {
      ServerLevel level = ctx.level();
      ServerPlayer player = ctx.player();

      for (double d = Math.max(0.5, near); d <= far; d += 0.7) {
         int steps = Math.max(6, (int)Math.ceil(d * (float) (Math.PI * 2) / 0.7));

         for (int i = 0; i < steps; i++) {
            double a = (double)((float)i * (float) (Math.PI * 2) / (float)steps);
            Vec3 p = apex.add(Math.cos(a) * d, 0.0, Math.sin(a) * d);
            int x = Mth.floor(p.x);
            int z = Mth.floor(p.z);

            for (int y = Mth.floor(apex.y) - 1; y <= Mth.floor(apex.y) + 2; y++) {
               BlockPos pos = new BlockPos(x, y, z);
               BlockState state = level.getBlockState(pos);
               if (fragile(level, pos, state)) {
                  TerrainDestruction.tearOut(
                     level, player, pos, new Vec3(Math.cos(a) * 0.3, 0.45, Math.sin(a) * 0.3), budget, level.random.nextInt(60), false, true
                  );
               }
            }
         }
      }
   }

   public static boolean fragile(ServerLevel level, BlockPos pos, BlockState state) {
      if (state.isAir()) {
         return false;
      } else {
         float hardness = state.getDestroySpeed(level, pos);
         return state.is(Blocks.GLASS_BLOCKS)
            || state.is(Blocks.GLASS_PANES)
            || state.is(BlockTags.ICE)
            || state.is(BlockTags.LEAVES)
            || state.is(BlockTags.REPLACEABLE)
            || state.is(BlockTags.FLOWERS)
            || state.is(BlockTags.SNOW)
            || hardness >= 0.0F && hardness <= 0.3F;
      }
   }
}
