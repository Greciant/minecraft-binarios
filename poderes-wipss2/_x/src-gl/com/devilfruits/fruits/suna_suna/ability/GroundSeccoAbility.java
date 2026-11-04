package com.devilfruits.fruits.suna_suna.ability;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.fruits.suna_suna.Desiccation;
import com.devilfruits.fruits.suna_suna.SunaEffects;
import com.devilfruits.fruits.suna_suna.SunaFx;
import com.devilfruits.fruits.suna_suna.SunaGround;
import com.devilfruits.fruits.suna_suna.SunaSuna;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class GroundSeccoAbility extends SandTechnique {
   private static final Map<UUID, GroundSeccoAbility.Patch> PATCHES = new HashMap<>();

   public GroundSeccoAbility() {
      super(
         "ground_secco",
         CastType.HELD,
         AbilityStats.builder()
            .energy(16)
            .cooldown(160)
            .mastery(3)
            .damage(2.0)
            .range(14.0)
            .knockback(0.0)
            .blockDamage(true)
            .extra("growSeconds", 3.0, 0.1, 20.0, "Seconds the dry patch takes to spread out to its full radius.")
            .extra("maxSeconds", 8.0, 1.0, 60.0, "Longest time the hand can stay on the ground.")
            .extra("drainPerSecond", 4.0, 0.0, 100.0, "Energy drained per second while held.")
            .extra("dehydrateSeconds", 4.0, 0.0, 60.0, "Seconds of Dehydrated added every second to whatever stands on the patch.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults())
         .impact(PhysicsProfile.Impact.NONE)
         .reaction(PhysicsProfile.Reaction.NONE)
         .blockDamage(0.0)
         .manual()
         .build();
   }

   @Override
   public int maxChannelTicks() {
      return (int)(this.config().extra("maxSeconds") * 20.0);
   }

   private int growTicks(AbilityContext ctx) {
      return Math.max(1, (int)(ctx.extra("growSeconds") * 20.0));
   }

   @Override
   public void onChannelStart(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      double ground = SunaGround.surface(level, player.getX(), player.getY(), player.getZ(), 0, 3);
      Vec3 centre = new Vec3(player.getX(), Double.isNaN(ground) ? player.getY() : ground, player.getZ());
      PATCHES.put(player.getUUID(), new GroundSeccoAbility.Patch(centre, ctx.seed()));
      ServerFx.animate(player, ctx.animation(SunaSuna.id("ground_secco")));
      sandArm(player, 20);
      ServerFx.sendNear(
         level,
         SunaFx.SECCO,
         player.getId(),
         centre,
         new Vec3((double)this.growTicks(ctx), 0.0, 0.0),
         ctx.range(),
         ctx.seed(),
         this.maxChannelTicks(),
         (double)(64.0F + ctx.range())
      );
   }

   @Override
   public boolean channelTick(AbilityContext ctx, int ticks) {
      ServerPlayer player = ctx.player();
      GroundSeccoAbility.Patch patch = PATCHES.get(player.getUUID());
      if (patch != null && !(player.position().distanceToSqr(patch.centre) > 6.25)) {
         PlayerFruitData data = ctx.data();
         if (!AbilityManager.freeEnergy(player)) {
            data.setEnergy(Math.max(0.0F, data.energy() - (float)ctx.extra("drainPerSecond") / 20.0F));
            if (data.energy() <= 0.0F) {
               return false;
            }
         }

         if (ticks % 10 == 0) {
            sandArm(player, 20);
         }

         ServerLevel level = ctx.level();
         double r = radius((double)ctx.range(), ticks, this.growTicks(ctx));
         if (r > patch.dried + 0.4 || r >= (double)ctx.range() && patch.dried < (double)ctx.range()) {
            Desiccation.dry(level, player, patch.centre, r, patch.dried, Desiccation.Options.SURFACE, patch.budget);
            patch.dried = r;
         }

         if (ticks > 0 && ticks % 20 == 0) {
            int seconds = (int)ctx.extra("dehydrateSeconds");
            AABB box = new AABB(patch.centre, patch.centre).inflate(r, 1.5, r).expandTowards(0.0, 1.0, 0.0);

            for (Entity e : Targeting.inBox(level, player, box, x -> Targeting.canHurt(player, x))) {
               if (e instanceof LivingEntity) {
                  LivingEntity living = (LivingEntity)e;
                  if (!(Mth.square(e.getX() - patch.centre.x) + Mth.square(e.getZ() - patch.centre.z) > r * r)
                     && (e.onGround() || !(e.getY() > patch.centre.y + 0.6))) {
                     Targeting.hurt(player, e, ctx.damage(), SunaSuna.DEHYDRATION_DAMAGE);
                     if (seconds > 0) {
                        SunaEffects.dehydrate(living, seconds, 1, 4, player);
                     }
                  }
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static double radius(double range, int ticks, int growTicks) {
      double t = Mth.clamp((double)ticks / (double)growTicks, 0.0, 1.0);
      return range * (1.0 - (1.0 - t) * (1.0 - t));
   }

   @Override
   public void onChannelEnd(AbilityContext ctx, int ticks, boolean interrupted) {
      ServerPlayer player = ctx.player();
      GroundSeccoAbility.Patch patch = PATCHES.remove(player.getUUID());
      if (patch != null) {
         ServerFx.sendNear(ctx.level(), SunaFx.SECCO_END, player.getId(), patch.centre, Vec3.ZERO, ctx.range(), patch.seed, 0, (double)(64.0F + ctx.range()));
      }

      ServerFx.stopAnimation(player);
   }

   @Nullable
   public static Vec3 centre(ServerPlayer player) {
      GroundSeccoAbility.Patch patch = PATCHES.get(player.getUUID());
      return patch == null ? null : patch.centre;
   }

   private static final class Patch {
      final Vec3 centre;
      final long seed;
      final CastBudget budget = Desiccation.budget();
      double dried;

      Patch(Vec3 centre, long seed) {
         this.centre = centre;
         this.seed = seed;
      }
   }
}
