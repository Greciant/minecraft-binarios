package com.devilfruits.fruits.suna_suna.ability;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.fruits.suna_suna.SandStorm;
import com.devilfruits.fruits.suna_suna.SunaFx;
import com.devilfruits.fruits.suna_suna.SunaGround;
import com.devilfruits.fruits.suna_suna.SunaSuna;
import com.devilfruits.fruits.suna_suna.SunaTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class SablesAbility extends SandTechnique {
   public SablesAbility() {
      super(
         "sables",
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(70)
            .cooldown(900)
            .mastery(8)
            .damage(4.0)
            .range(24.0)
            .knockback(1.5)
            .blockDamage(true)
            .extra("radius", 5.0, 2.0, 16.0, "Radius of the sandstorm.")
            .extra("lifeSeconds", 12.0, 2.0, 60.0, "How long the sandstorm rages.")
            .extra("speed", 0.12, 0.0, 1.0, "How fast the sandstorm wanders, blocks per tick.")
            .extra("pull", 0.3, 0.0, 3.0, "Pull towards the sandstorm.")
            .extra("pullRadius", 8.0, 2.0, 32.0, "How far out the sandstorm drags mobs towards its column.")
            .extra("lift", 0.16, 0.0, 2.0, "Upward lift inside the sandstorm.")
            .extra("hitInterval", 10.0, 1.0, 40.0, "Ticks between the blows of the sandstorm.")
            .extra("blocksPerSecond", 4.0, 0.0, 64.0, "Blocks the sandstorm rips out of the ground per second.")
            .extra("dehydrateSeconds", 3.0, 0.0, 60.0, "Seconds of Dehydrated each blow leaves (up to level III).")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.TANGENTIAL).force(0.5).blockDamage(0.15).manual().build();
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      ServerFx.animate(player, ctx.animation(SunaSuna.id("sables")));
      SunaTriggers.USED_SABLES.get().trigger(player);
      sandArm(player, 30);
      Vec3 at = DesertGirasoleAbility.ground(player, (double)ctx.range());
      Vec3 dir = ctx.horizontalLook();
      if (at == null) {
         Vec3 ahead = player.position().add(dir.scale(6.0));
         double y = SunaGround.surface(level, ahead.x, player.getY(), ahead.z, 3, 8);
         at = new Vec3(ahead.x, Double.isNaN(y) ? player.getY() : y, ahead.z);
      }

      double heading = Math.atan2(dir.z, dir.x);
      int life = (int)(ctx.extra("lifeSeconds") * 20.0);
      SandStorm.Power power = new SandStorm.Power(
         ctx.extra("radius"),
         life,
         ctx.extra("speed"),
         ctx.extra("pull"),
         ctx.extra("lift"),
         ctx.damage(),
         (int)ctx.extra("hitInterval"),
         ctx.extra("blocksPerSecond"),
         (int)ctx.extra("dehydrateSeconds"),
         (double)ctx.knockback(),
         ctx.blockDamageAllowed(),
         ctx.extra("pullRadius")
      );
      long seed = ctx.seed();
      SandStorm storm = new SandStorm(level, player, at, heading, power, ctx.budget(), seed);
      ServerFx.sendNear(level, SunaFx.SABLES, player.getId(), at, dir.scale(power.speed()), (float)power.radius(), seed, life, 160.0);
      ServerFx.shake(level, at, 0.35F, 48);
      ServerScheduler.schedule(level, 1, storm::tick);
   }
}
