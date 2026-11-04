package com.devilfruits.fruits.horo_horo.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.combat.Stun;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.fruits.horo_horo.HollowFlight;
import com.devilfruits.fruits.horo_horo.HoroFx;
import com.devilfruits.fruits.horo_horo.HoroHoro;
import com.devilfruits.fruits.horo_horo.HoroSounds;
import com.devilfruits.fruits.horo_horo.HoroTriggers;
import com.devilfruits.fruits.horo_horo.blast.HollowBlast;
import com.devilfruits.fruits.kage_kage.KageAim;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags.EntityTypes;

public class SpecialHollowAbility extends Ability {
   public static final int RISE_TICKS = 24;
   public static final double RISE_HEIGHT = 4.5;
   private static final int MAX_FLIGHT = 600;
   private static final Map<UUID, Vec3> SWALLOWED = new HashMap<>();

   public SpecialHollowAbility() {
      super(
         HoroHoro.id("special_hollow"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(60)
            .cooldown(1200)
            .mastery(8)
            .damage(32.0)
            .range(32.0)
            .knockback(2.4)
            .extra("speed", 0.3, 0.05, 2.0, "Blocks per tick the giant drifts (the slowest Horo flight).")
            .extra("swellSeconds", 2.5, 0.5, 10.0, "Seconds the giant swells with its target inside before it explodes.")
            .extra("radius", 8.5, 2.0, 20.0, "Blast radius in blocks (damage and knockback fall to zero here).")
            .extra("blockPower", 6.0, 0.0, 16.0, "Blast power on terrain (vanilla scale, TNT = 4); 0 = no block damage.")
            .extra("maxBlocks", 260.0, 0.0, 2000.0, "Most blocks one blast may break.")
            .extra("maxPlayerDamage", 26.0, 0.0, 1000.0, "Most damage the blast deals one player.")
            .extra("aimConeDegrees", 10.0, 0.0, 45.0, "Aim assist: half-angle of the cone round the crosshair a target may stand in.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.RADIAL).force(2.0).blockDamage(1.0).manual().build();
   }

   public static boolean swallowed(Entity entity) {
      return SWALLOWED.containsKey(entity.getUUID());
   }

   @Nullable
   public static Component castBlocked(ServerPlayer player) {
      return swallowed(player) ? Component.translatable("message.devilfruits.horo_horo.swallowed") : null;
   }

   @Nullable
   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      double range = this.config().range() * (double)Mastery.rangeMultiplier(data.mastery());
      return KageAim.aimed(player, range, this.config().extra("aimConeDegrees")) == null
         ? Component.translatable("message.devilfruits.horo_horo.no_target")
         : null;
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      LivingEntity target = KageAim.aimed(player, (double)ctx.range(), ctx.extra("aimConeDegrees"));
      if (target != null) {
         ServerLevel level = ctx.level();
         Vec3 start = player.position().add(0.0, 4.5, 0.0);
         double speed = ctx.extra("speed");
         HollowFlight flight = new HollowFlight(start, ctx.horizontalLook(), speed, 0.1);
         HollowBlast.Spec spec = new HollowBlast.Spec(
            (float)ctx.extra("radius"),
            (float)ctx.extra("blockPower"),
            ctx.damage(),
            ctx.knockback(),
            (int)ctx.extra("maxBlocks"),
            (float)ctx.extra("maxPlayerDamage"),
            ctx.blockDamageAllowed()
         );
         int swell = (int)Math.round(ctx.extra("swellSeconds") * 20.0);
         ServerFx.animate(player, HoroHoro.id("kamikaze_raise"));
         ServerFx.send(player, HoroFx.GIANT_LAUNCH, start, ctx.look(), (float)speed, ctx.seed(), target.getId());
         level.playSound(null, start.x, start.y, start.z, HoroSounds.GIANT_RISE.get(), SoundSource.PLAYERS, 1.2F, 0.8F);
         long[] swallowedAt = new long[]{-1L};
         ServerScheduler.schedule(level, 24, age -> {
            if (age == 0 && player.isAlive()) {
               ServerFx.animate(player, HoroHoro.id("kamikaze_release"));
            }

            if (swallowedAt[0] < 0L) {
               if (target.isAlive() && !target.isRemoved() && target.level() == level && age <= 600 && player.isAlive()) {
                  if (flight.step(HollowFlight.aimPoint(target)) > (double)target.getBbWidth() * 0.5 + 1.2) {
                     return false;
                  } else {
                     swallowedAt[0] = (long)age;
                     flight.pos = HollowFlight.aimPoint(target);
                     SWALLOWED.put(target.getUUID(), flight.pos);
                     boolean boss = target.getType().is(EntityTypes.BOSSES);
                     Stun.apply(target, boss ? (int)Math.round((double)swell * HoroHoro.setting("bossNegativeFactor")) : swell + 2, false);
                     ServerFx.send(target, HoroFx.GIANT_ABSORB, flight.pos, Vec3.ZERO, 1.0F, 0L, swell);
                     level.playSound(null, flight.pos.x, flight.pos.y, flight.pos.z, HoroSounds.GIANT_GULP.get(), SoundSource.PLAYERS, 1.2F, 0.8F);
                     level.playSound(null, flight.pos.x, flight.pos.y, flight.pos.z, HoroSounds.GIANT_SWELL.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
                     return false;
                  }
               } else {
                  fizzle(level, flight.pos);
                  return true;
               }
            } else {
               int held = age - (int)swallowedAt[0];
               if (target.isAlive() && !target.isRemoved() && target.level() == level) {
                  Vec3 c = flight.pos;
                  target.teleportTo(c.x, c.y - (double)target.getBbHeight() * 0.55, c.z);
                  target.setDeltaMovement(Vec3.ZERO);
                  target.fallDistance = 0.0F;
                  target.hurtMarked = true;
               } else if (held < swell) {
                  SWALLOWED.remove(target.getUUID());
                  fizzle(level, flight.pos);
                  return true;
               }

               if (held < swell) {
                  return false;
               } else {
                  SWALLOWED.remove(target.getUUID());
                  Stun.release(target);
                  HollowBlast.detonate(level, flight.pos, spec, player.isAlive() ? player : null);
                  ServerFx.sendNear(level, HoroFx.GIANT_END, -1, flight.pos, Vec3.ZERO, 1.0F, 0L, 0, 128.0);
                  if (player.isAlive()) {
                     HoroTriggers.KAMIKAZE.get().trigger(player);
                  }

                  return true;
               }
            }
         });
      }
   }

   private static void fizzle(ServerLevel level, Vec3 at) {
      ServerFx.sendNear(level, HoroFx.GIANT_END, -1, at, Vec3.ZERO, 1.0F, 0L, 1, 128.0);
      level.playSound(null, at.x, at.y, at.z, HoroSounds.POP.get(), SoundSource.PLAYERS, 1.4F, 0.5F);
   }

   public static void clear() {
      SWALLOWED.clear();
   }
}
