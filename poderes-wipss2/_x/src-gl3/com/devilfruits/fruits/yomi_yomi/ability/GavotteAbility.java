package com.devilfruits.fruits.yomi_yomi.ability;

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
import com.devilfruits.fruits.yomi_yomi.YomiFx;
import com.devilfruits.fruits.yomi_yomi.YomiSounds;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class GavotteAbility extends BladeAbility {
   private static final double AIR_DRAG = 0.91;
   private static final int ARC_TICKS = 19;

   public GavotteAbility() {
      super(
         "gavotte",
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(16)
            .cooldown(180)
            .mastery(3)
            .damage(8.0)
            .range(10.0)
            .knockback(1.4)
            .extra("sweepRadius", 3.5, 1.0, 8.0, "Reach of the sweep round the user in blocks.")
            .extra("launch", 0.8, 0.0, 3.0, "Upward speed the sweep gives what it cuts.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults())
         .impact(PhysicsProfile.Impact.RADIAL)
         .force(0.5)
         .reaction(PhysicsProfile.Reaction.JUMP)
         .blockDamage(0.0)
         .manual()
         .build();
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      AbilityManager.setBusy(player, 18);
      ServerFx.animate(player, ctx.animation(YomiYomi.id("gavotte")));
      ServerScheduler.schedule(level, 0, age -> {
         if (!present(player, level)) {
            return true;
         } else {
            if (age >= 5) {
               player.fallDistance = 0.0F;
            }

            if (age == 5) {
               Vec3 v = leap(ctx.look(), (double)ctx.range());
               Targeting.moveSelf(player, v);
               ServerFx.send(player, YomiFx.GAVOTTE_LEAP, player.position(), v, 1.0F, ctx.seed(), 0);
               level.playSound(null, player.getX(), player.getY(), player.getZ(), YomiSounds.LEAP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            } else if (age == 16) {
               sweep(ctx);
            }

            return age > 16 && (player.onGround() || player.isInWater() || age > 60);
         }
      });
   }

   public static Vec3 leap(Vec3 look, double range) {
      Vec3 flat = new Vec3(look.x, 0.0, look.z);
      flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
      double up = Mth.clamp(look.y, 0.0, 0.8);
      double reach = (1.0 - Math.pow(0.91, 19.0)) / 0.08999999999999997;
      double forward = range / reach * (1.0 - up * 0.45);
      return flat.scale(forward).add(0.0, 0.66 + up * 0.45, 0.0);
   }

   public static List<Entity> sweep(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      double radius = ctx.extra("sweepRadius");
      Vec3 center = player.position().add(0.0, (double)player.getBbHeight() * 0.45, 0.0);
      List<Entity> hit = new ArrayList<>();

      for (Entity e : Targeting.inRadius(level, center, radius + 2.5, x -> x != player && Targeting.canHurt(player, x))) {
         Vec3 c = e.getBoundingBox().getCenter();
         double flat = Math.hypot(c.x - center.x, c.z - center.z) - (double)e.getBbWidth() * 0.5;
         if (flat <= radius && c.y > center.y - 3.0 && c.y < center.y + 1.6) {
            hit.add(e);
         }
      }

      float damage = ctx.damage();
      double launch = ctx.extra("launch");

      for (Entity ex : hit) {
         if (ctx.hurt(ex, damage)) {
            Vec3 away = new Vec3(ex.getX() - center.x, 0.0, ex.getZ() - center.z);
            away = away.lengthSqr() < 1.0E-4 ? player.getLookAngle().multiply(1.0, 0.0, 1.0) : away.normalize();
            Targeting.launch(ex, away.scale((double)ctx.knockback() * 0.6).add(0.0, launch, 0.0));
         }
      }

      BlockImpactService.strike(ctx, ImpactShape.cylinder(center.add(0.0, -1.5, 0.0), radius, 2.5), Vec3.ZERO);
      ServerFx.send(player, YomiFx.GAVOTTE_SWEEP, center, player.getDeltaMovement(), (float)radius, ctx.seed(), hit.size());
      level.playSound(null, center.x, center.y, center.z, YomiSounds.SWEEP.get(), SoundSource.PLAYERS, 1.1F, 1.0F);
      if (!hit.isEmpty()) {
         level.playSound(null, center.x, center.y, center.z, YomiSounds.BLADE_HIT.get(), SoundSource.PLAYERS, 1.0F, 0.9F);
      }

      return hit;
   }
}
