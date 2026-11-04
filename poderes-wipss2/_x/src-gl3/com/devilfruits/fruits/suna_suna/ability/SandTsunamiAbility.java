package com.devilfruits.fruits.suna_suna.ability;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.fruits.suna_suna.Desiccation;
import com.devilfruits.fruits.suna_suna.SunaEffects;
import com.devilfruits.fruits.suna_suna.SunaFx;
import com.devilfruits.fruits.suna_suna.SunaGround;
import com.devilfruits.fruits.suna_suna.SunaSuna;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SandTsunamiAbility extends SandTechnique {
   public SandTsunamiAbility() {
      super(
         "sand_tsunami",
         CastType.CHARGED,
         AbilityStats.builder()
            .energy(50)
            .cooldown(400)
            .mastery(7)
            .damage(14.0)
            .range(20.0)
            .knockback(2.0)
            .blockDamage(true)
            .extra("speed", 0.7, 0.2, 2.0, "Speed of the wave, blocks per tick.")
            .extra("waveWidth", 13.0, 3.0, 40.0, "Width of the wave at full charge (60 % of it at the lowest).")
            .extra("waveHeight", 5.0, 1.0, 7.0, "Height of the wave at full charge (half of it at the lowest).")
            .extra("dehydrateSeconds", 6.0, 0.0, 60.0, "Seconds of Dehydrated for whatever the wave strikes.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.DIRECTIONAL).force(1.2).blockDamage(0.3).manual().build();
   }

   @Override
   public int maxChargeTicks() {
      return 40;
   }

   @Override
   public void onChargeStart(AbilityContext ctx) {
      ServerFx.animate(ctx.player(), ctx.animation(SunaSuna.id("sand_tsunami")));
      LogiaManager.partial(ctx.player(), 12, 15);
   }

   @Override
   public void onChargeTick(AbilityContext ctx, int ticks) {
      if (ticks % 10 == 0) {
         LogiaManager.partial(ctx.player(), 12, 15);
      }
   }

   @Override
   public void release(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      float charge = Math.max(0.2F, ctx.charge());
      ServerFx.animate(player, ctx.animation(SunaSuna.id("sand_tsunami_release")));
      LogiaManager.partial(player, 12, 14);
      double width = ctx.extra("waveWidth") * (0.6 + 0.4 * (double)charge);
      double height = ctx.extra("waveHeight") * (0.5 + 0.5 * (double)charge);
      Vec3 dir = ctx.horizontalLook();
      Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
      double speed = ctx.extra("speed");
      Vec3 ahead = player.position().add(dir.scale(2.0));
      double ground = SunaGround.surface(level, ahead.x, player.getY(), ahead.z, 2, 4);
      Vec3 start = new Vec3(ahead.x, Double.isNaN(ground) ? player.getY() : ground, ahead.z);
      Vec3 velocity = dir.scale(speed);
      int lifetime = Mth.ceil((double)ctx.range() / speed);
      long seed = ctx.seed();
      int packed = lifetime * 16 + Mth.clamp(Math.round((float)height * 2.0F), 1, 15);
      ServerFx.sendNear(level, SunaFx.TSUNAMI, player.getId(), start, velocity, (float)width, seed, packed, 128.0);
      ServerFx.shake(level, start, 0.3F + 0.3F * charge, 40);
      float damage = ctx.damage() * (0.5F + 0.5F * charge);
      int seconds = (int)ctx.extra("dehydrateSeconds");
      CastBudget drying = Desiccation.budget();
      Vec3[] at = new Vec3[]{start};
      IntSet struck = new IntOpenHashSet();
      Set<UUID> structures = new HashSet<>();
      double half = width * 0.5;
      double reach = (double)Math.max(1L, Math.round(height * 2.0)) / 2.0;
      ServerScheduler.schedule(
         level,
         1,
         age -> {
            if (!player.isRemoved() && player.level() == level) {
               Vec3 pos = at[0];
               Vec3 next = pos.add(velocity);
               double y = SunaGround.surface(level, next.x, pos.y, next.z, Mth.ceil(reach * 0.6), 4);
               if (Double.isNaN(y)) {
                  ServerFx.sendNear(level, SunaFx.TSUNAMI_END, player.getId(), pos, velocity, (float)width, seed, 0, 128.0);
                  return true;
               } else {
                  next = new Vec3(next.x, y, next.z);
                  AABB box = new AABB(next, next).inflate(half + 1.0, 0.0, half + 1.0).expandTowards(0.0, reach + 1.0, 0.0).move(0.0, -0.5, 0.0);

                  for (Entity e : Targeting.inBox(level, player, box, x -> Targeting.canPush(player, x))) {
                     Vec3 rel = e.position().subtract(next);
                     double along = rel.dot(dir);
                     if (!(Math.abs(rel.dot(side)) > half + (double)e.getBbWidth() * 0.5) && !(along < -2.0) && !(along > 1.0) && struck.add(e.getId())) {
                        if (e instanceof LivingEntity) {
                           LivingEntity living = (LivingEntity)e;
                           if (Targeting.canHurt(player, e)) {
                              ctx.hurt(e, damage);
                              if (seconds > 0) {
                                 SunaEffects.dehydrate(living, seconds, 1, 3, player);
                              }
                           }
                        }

                        Targeting.launch(e, dir.scale((double)ctx.knockback() * (0.6 + 0.4 * (double)charge)).add(0.0, 0.45 + 0.1 * reach, 0.0));
                     }
                  }

                  Vec3 crest = next.add(0.0, reach * 0.5, 0.0);
                  BlockImpactService.strike(
                     ctx,
                     ImpactShape.capsule(crest.add(side.scale(half)), crest.add(side.scale(-half)), reach * 0.5 + 0.8),
                     dir.add(0.0, 0.3, 0.0),
                     0.5 + 0.5 * (double)charge,
                     structures
                  );

                  for (int k = -((int)half); k <= (int)half; k += 3) {
                     Vec3 p = next.add(side.scale((double)k));
                     Desiccation.dry(level, player, p, 1.8, 0.0, Desiccation.Options.SURFACE, drying);
                  }

                  at[0] = next;
                  if (age >= lifetime) {
                     ServerFx.sendNear(level, SunaFx.TSUNAMI_END, player.getId(), next, velocity, (float)width, seed, 0, 128.0);
                     return true;
                  } else {
                     return false;
                  }
               }
            } else {
               return true;
            }
         }
      );
   }
}
