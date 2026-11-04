package com.awakenedmod.awakened.content.limitless;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.LoopSound;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class LimitlessFx {
   private static final String KEY_ORB_HUM = "limitless.blue.hum";
   private static final String KEY_PULL = "limitless.blue.pull";
   private static final String KEY_RED_GATHER = "limitless.red.gather";
   private static final String KEY_RCT_HUM = "limitless.rct.hum";

   private LimitlessFx() {
   }

   public static void shotStopped(ServerLevel level, Vec3 at) {
      Fx.sound(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.45F, 1.7F);
   }

   public static void meleeStopped(ServerLevel level, Vec3 at) {
      Fx.sound(level, at, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8F, 0.55F);
   }

   public static void orbOpened(ServerLevel level, Vec3 at, boolean held) {
      Fx.sound(level, at, SoundEvents.BREEZE_INHALE, SoundSource.PLAYERS, 1.0F, 0.7F);
      if (!held) {
         Fx.sound(level, at, SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.6F);
      }
   }

   public static void blueSlam(ServerLevel level, Vec3 at) {
      Fx.sound(level, at, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.6F);
      Fx.sound(level, at, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.PLAYERS, 0.5F, 0.8F);
      Fx.burst(level, ParticleTypes.CRIT, at, 8, 0.3, 0.25);
   }

   public static void blueSnap(ServerLevel level, Vec3 at) {
      Fx.sound(level, at, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.5F);
      Fx.sound(level, at, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.8F, 1.4F);
   }

   public static void blueImploded(ServerLevel level, Vec3 at) {
      Fx.sound(level, at, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.2F, 0.55F);
      Fx.sound(level, at, (SoundEvent)SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 0.9F, 0.6F);
      Fx.shake(level, at, 16.0, 0.12F, 1, 8);
   }

   public static void bluePopped(ServerLevel level, Vec3 at) {
      Fx.sound(level, at, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.7F, 1.5F);
      Fx.sound(level, at, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.8F);
   }

   public static void orbHum(Entity orb, float presence) {
      LoopSound.keep(orb, "limitless.blue.hum", SoundEvents.CONDUIT_AMBIENT, SoundSource.PLAYERS, 0.35F + 0.55F * presence, 1.35F);
   }

   public static void orbCollapsed(ServerLevel level, Entity orb) {
      LoopSound.stop(orb, "limitless.blue.hum");
      Fx.sound(level, orb.position(), SoundEvents.CONDUIT_DEACTIVATE, SoundSource.PLAYERS, 0.8F, 1.5F);
   }

   public static void pullStarted(ServerLevel level, Entity caster) {
      Fx.sound(level, caster.position(), SoundEvents.BREEZE_CHARGE, SoundSource.PLAYERS, 0.9F, 1.1F);
   }

   public static void pulling(Entity caster, double speed) {
      float s = (float)Math.min(1.0, speed / 1.6);
      LoopSound.keep(caster, "limitless.blue.pull", SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.15F + 0.5F * s, 0.9F + 0.4F * s);
   }

   public static void pullEnded(Entity caster) {
      LoopSound.stop(caster, "limitless.blue.pull");
   }

   public static void redGathering(Entity caster, float u) {
      LoopSound.keep(caster, "limitless.red.gather", SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.35F + 0.45F * u, 1.2F + 0.8F * u);
   }

   public static void redGatherEnded(Entity caster) {
      LoopSound.stop(caster, "limitless.red.gather");
   }

   public static void redFired(ServerLevel level, Vec3 from) {
      Fx.sound(level, from, SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 0.9F, 1.6F);
   }

   public static void redBurst(ServerLevel level, Vec3 at) {
      Fx.sound(level, at, (SoundEvent)SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 2.0F, 0.5F);
      Fx.sound(level, at, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.6F, 0.55F);
      Fx.sound(level, at, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.2F, 0.5F);
      Fx.shake(level, at, 20.0, 0.22F, 2, 10);
   }

   public static void redSlam(ServerLevel level, Vec3 at) {
      Fx.sound(level, at, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.PLAYERS, 0.7F, 0.7F);
      Fx.burst(level, ParticleTypes.POOF, at, 6, 0.15, 0.02);
   }

   public static void rctStarted(ServerLevel level, Entity caster) {
      Fx.sound(level, caster.position(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6F, 1.3F);
   }

   public static void rctHum(Entity caster) {
      LoopSound.keep(caster, "limitless.rct.hum", SoundEvents.CONDUIT_AMBIENT_SHORT, SoundSource.PLAYERS, 0.35F, 1.8F);
   }

   public static void rctEnded(Entity caster) {
      LoopSound.stop(caster, "limitless.rct.hum");
   }

   public static void rctSwingRefused(ServerLevel level, Entity caster) {
      Fx.sound(level, caster.position(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 0.8F, 1.0F);
   }
}
