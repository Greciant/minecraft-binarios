package com.devilfruits.fruits.yomi_yomi.ability;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.yomi_yomi.YomiFx;
import com.devilfruits.fruits.yomi_yomi.YomiSounds;
import com.devilfruits.fruits.yomi_yomi.frost.YomiFrost;
import com.devilfruits.fruits.yomi_yomi.soul.SoulProjection;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

public class SoulDashAbility extends SoulSkill {
   private static final double STEP = 0.25;

   public SoulDashAbility() {
      super(
         "soul_dash",
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(10)
            .cooldown(60)
            .mastery(1)
            .damage(6.0)
            .range(15.0)
            .knockback(0.3)
            .extra("frostbiteTicks", 40.0, 0.0, 400.0, "Ticks of frostbite (-30 %) on what the dash passed through.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.NONE).blockDamage(0.0).manual().build();
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      AbilityManager.setBusy(player, 11);
      ServerFx.send(player, YomiFx.SOUL_DASH, player.position(), ctx.look(), 0.0F, ctx.seed(), 7);
      Vec3[] path = new Vec3[2];
      Set<Entity> hit = new LinkedHashSet<>();
      ServerScheduler.schedule(level, 0, age -> {
         if (player.isAlive() && !player.isRemoved() && player.level() == level && SoulProjection.active(player) && !SoulProjection.recoiling(player)) {
            int surge = age - 7;
            if (surge == 0) {
               Vec3[] p = path(player, ctx.look(), (double)ctx.range());
               path[0] = p[0];
               path[1] = p[1];
               ServerFx.send(player, YomiFx.SOUL_DASH_SURGE, p[0], p[1], 0.0F, ctx.seed(), 3);
               level.playSound(null, player.getX(), player.getY(), player.getZ(), YomiSounds.DASH.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
            }

            if (surge >= 0 && surge < 3) {
               Vec3 from = player.position();
               Vec3 to = path[0].lerp(path[1], (double)(surge + 1) / 3.0);
               if (player instanceof FakePlayer) {
                  player.moveTo(to.x, to.y, to.z, player.getYRot(), player.getXRot());
               } else {
                  Targeting.moveSelf(player, to.subtract(from));
               }

               Vec3 mid = new Vec3(0.0, (double)player.getBbHeight() * 0.5, 0.0);

               for (Entity e : Targeting.inCapsule(level, player, from.add(mid), to.add(mid), 1.1)) {
                  if (hit.add(e)) {
                     chill(ctx, e);
                  }
               }
            }

            if (surge == 3) {
               Targeting.moveSelf(player, player.getDeltaMovement().scale(0.2));
               return true;
            } else {
               return false;
            }
         } else {
            return true;
         }
      });
   }

   private static void chill(AbilityContext ctx, Entity e) {
      e.invulnerableTime = 0;
      if (ctx.hurt(e, ctx.damage()) && e instanceof LivingEntity living && living.isAlive()) {
         YomiFrost.addChill(living, ctx.player(), 8.0F, 70);
         int ticks = (int)ctx.extra("frostbiteTicks");
         if (ticks > 0) {
            YomiFrost.cold(living, ctx.player(), ticks, 3);
         }

         ServerFx.send(e, YomiFx.SOUL_CHILL, e.getBoundingBox().getCenter(), Vec3.ZERO, e.getBbHeight(), 0L, 0);
      }
   }

   public static Vec3[] path(ServerPlayer player, Vec3 look, double range) {
      Vec3 start = player.position();
      Vec3 d = look.normalize();
      AABB box = player.getBoundingBox().deflate(0.001);
      double extent = Math.abs(d.x) * (double)player.getBbWidth() + Math.abs(d.y) * (double)player.getBbHeight() + Math.abs(d.z) * (double)player.getBbWidth();
      double allowed = 1.0 + extent + 0.35;
      Vec3 lastFree = start;
      double run = 0.0;

      for (double s = 0.25; s <= range + 1.0E-6; s += 0.25) {
         Vec3 at = start.add(d.scale(s));
         if (player.level().noCollision(player, box.move(at.subtract(start)))) {
            lastFree = at;
            run = 0.0;
         } else {
            run += 0.25;
            if (run > allowed) {
               break;
            }
         }
      }

      return new Vec3[]{start, lastFree};
   }
}
