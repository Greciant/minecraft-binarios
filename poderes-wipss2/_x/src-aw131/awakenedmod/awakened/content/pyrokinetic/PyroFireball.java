package com.awakenedmod.awakened.content.pyrokinetic;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.entity.projectile.KineticProjectile;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.caster.Caster;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class PyroFireball {
   public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, AwId.of("pyro_fireball"));
   private static final double CONTACT_RADIUS = 0.35;
   public static final int STYLE_LEFT_ARM_BIT = 16;

   private PyroFireball() {
   }

   public static void launch(Caster caster, PyroFireball.Shot shot, Vec3 origin, Vec3 dir, boolean rightArm, FlameTone style) {
      launch(caster, shot, origin, dir, rightArm, style, PyroFireball.Extras.NONE);
   }

   public static void launch(Caster caster, PyroFireball.Shot shot, Vec3 origin, Vec3 dir, boolean rightArm, FlameTone style, PyroFireball.Extras extras) {
      ServerLevel level = caster.level();
      KineticProjectile ball = new KineticProjectile(AwakenedEntities.PYRO_FIREBALL.get(), caster.body(), level, ItemStack.EMPTY);
      ball.setPos(origin.x, origin.y, origin.z);
      ball.setVisualStyle(style.syncByte() | (rightArm ? 0 : 16));
      ball.configure(shot.damage(), 0.0F, 0.0F, 0.0F);
      ball.setIgnite(shot.igniteSeconds());
      ball.setDamageType(DAMAGE_TYPE);
      ball.setContactRadius(0.35);
      ball.setGravity(shot.gravity());
      ball.setCustomDetonation(true);
      ball.setOnDetonateSurface((pos, normal) -> {
         boolean direct = ball.directHit() != null;
         double radius = direct ? shot.directBurstRadius() : shot.burstRadius();
         if (extras.quietBurst()) {
            PyroFlames.burstSilent(level, pos, radius, style, normal);
         } else {
            PyroFlames.burst(level, pos, radius, style, normal);
         }

         if (direct) {
            PyroFlames.impactFlare(level, pos, style);
         }

         if (extras.onImpact() != null) {
            extras.onImpact().accept(pos, normal);
         }
      });
      ServerPlayer thrower = caster.asPlayer();
      ball.setOnDirectHit(victim -> {
         PyroFlames.sound(level, victim.position(), SoundEvents.FIRE_EXTINGUISH, 0.8F, 0.65F, style);
         PyroFlames.sound(level, victim.position(), SoundEvents.PLAYER_HURT_ON_FIRE, 0.7F, 1.0F, style);
         if (thrower != null && victim != thrower) {
            Fx.soundOnlyFor(thrower, thrower.position(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.4F, 1.7F);
         }
      });
      ball.setOnTick(pos -> {
         if (ball.tickCount > shot.maxFlightTicks()) {
            ball.discard();
         } else if (ball.isInWater()) {
            PyroFlames.sound(level, pos, SoundEvents.FIRE_EXTINGUISH, 0.9F, 1.0F, style);
            ball.discard();
         } else {
            if (ball.tickCount % shot.wakeInterval() == 0) {
               PyroFlames.wake(level, pos, ball.getDeltaMovement(), style, shot.wakeInterval());
            }

            if (extras.onFlightTick() != null) {
               extras.onFlightTick().accept(ball);
            }
         }
      });
      ball.shoot(dir.x, dir.y, dir.z, shot.velocity(), shot.spread());
      level.addFreshEntity(ball);
   }

   public static record Extras(Consumer<KineticProjectile> onFlightTick, BiConsumer<Vec3, Vec3> onImpact, boolean quietBurst) {
      public static final PyroFireball.Extras NONE = new PyroFireball.Extras(null, null, false);

      public Extras(Consumer<KineticProjectile> onFlightTick, BiConsumer<Vec3, Vec3> onImpact) {
         this(onFlightTick, onImpact, false);
      }
   }

   public static record Shot(
      float damage,
      int igniteSeconds,
      double burstRadius,
      double directBurstRadius,
      float velocity,
      double gravity,
      float spread,
      int maxFlightTicks,
      int wakeInterval
   ) {
      public Shot(
         float damage, int igniteSeconds, double burstRadius, double directBurstRadius, float velocity, double gravity, float spread, int maxFlightTicks
      ) {
         this(damage, igniteSeconds, burstRadius, directBurstRadius, velocity, gravity, spread, maxFlightTicks, 2);
      }

      public PyroFireball.Shot withDamage(float resolved) {
         return new PyroFireball.Shot(
            resolved,
            this.igniteSeconds,
            this.burstRadius,
            this.directBurstRadius,
            this.velocity,
            this.gravity,
            this.spread,
            this.maxFlightTicks,
            this.wakeInterval
         );
      }

      public PyroFireball.Shot withVelocity(float resolved) {
         return new PyroFireball.Shot(
            this.damage,
            this.igniteSeconds,
            this.burstRadius,
            this.directBurstRadius,
            resolved,
            this.gravity,
            this.spread,
            this.maxFlightTicks,
            this.wakeInterval
         );
      }

      public PyroFireball.Shot withWakeInterval(int ticks) {
         return new PyroFireball.Shot(
            this.damage, this.igniteSeconds, this.burstRadius, this.directBurstRadius, this.velocity, this.gravity, this.spread, this.maxFlightTicks, ticks
         );
      }
   }
}
