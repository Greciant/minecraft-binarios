package com.awakenedmod.awakened.content.brawler;

import com.awakenedmod.awakened.content.nuclear.NuclearFx;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxBolt;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class BrawlerFx {
   private static final int GUARD_WHITE = 16777215;
   static final VfxBolt.Look RETURN_BOLT = VfxBolt.Look.of(16777215, 0.32).core(16751150).motes(ParticleTypes.END_ROD);
   private static final int DOJO_VIOLET = 11820287;
   public static final int NUKE_GOLD = 16762170;
   public static final VfxBolt.Look DOJO_BOLT = VfxBolt.Look.of(11820287, 0.35).motes(Fx.dust(11820287, 1.1F));

   private BrawlerFx() {
   }

   public static int tierColor(int tier) {
      return tier <= 0 ? 16751150 : BrawlerTuning.TIER_COLOR[tier];
   }

   private static Vec3 chest(LivingEntity e) {
      return e.position().add(0.0, (double)e.getBbHeight() * 0.55, 0.0);
   }

   private static void soundForOthers(ServerLevel level, ServerPlayer owner, Vec3 pos, SoundEvent sound, float ownerVolume, float pitch) {
      level.playSound(owner, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, ownerVolume * 0.5F, pitch);
   }

   private static void shake(ServerLevel level, Vec3 pos, float degrees, int ticks) {
      Fx.shake(level, pos, 16.0, BrawlerTuning.shakeMagnitude(degrees), 0, ticks);
   }

   public static void comboHit(ServerLevel level, ServerPlayer player, LivingEntity target, int count, int tier) {
      Vec3 hit = chest(target);
      Fx.burst(level, ParticleTypes.CRIT, hit, 4, 0.25, 0.1);
      if (tier >= 2) {
         Vfx.effect(VfxEntity.Kind.RING, hit).scale(0.7).color(tierColor(tier)).lifetime(4).orient(-player.getYRot(), 90.0F).spawn(level);
      }

      if (count > 0 && count % 10 == 0) {
         Vfx.ring(level, player.position().add(0.0, 0.15, 0.0), 3.0, tierColor(tier), 8);
         Fx.burst(level, ParticleTypes.POOF, player.position().add(0.0, 0.5, 0.0), 8, 0.6, 0.05);
      }
   }

   public static void tierUp(ServerLevel level, ServerPlayer player, int tier) {
      Vec3 c = chest(player);
      int color = tierColor(tier);
      Vfx.sphere(level, c, 0.9, color, 6, 1.3F);
      Fx.burst(level, ParticleTypes.POOF, c, 10, 0.7, 0.04);
      Vec3 pos = player.position();
      switch (tier) {
         case 2:
            soundForOthers(level, player, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.4F);
            soundForOthers(level, player, pos, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.9F);
            break;
         case 3:
            soundForOthers(level, player, pos, SoundEvents.PLAYER_LEVELUP, 0.6F, 1.6F);
            soundForOthers(level, player, pos, SoundEvents.ANVIL_LAND, 0.4F, 1.5F);
            break;
         case 4:
            soundForOthers(level, player, pos, SoundEvents.ANVIL_LAND, 0.7F, 1.1F);
            soundForOthers(level, player, pos, (SoundEvent)SoundEvents.TRIDENT_THUNDER.value(), 0.4F, 1.5F);
            break;
         case 5:
            soundForOthers(level, player, pos, SoundEvents.TOTEM_USE, 0.7F, 1.3F);
            soundForOthers(level, player, pos, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5F, 1.4F);
      }
   }

   public static void unchainedFists(ServerLevel level, ServerPlayer player) {
      double yaw = Math.toRadians((double)player.yBodyRot);
      Vec3 right = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
      Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
      Vec3 base = player.position().add(0.0, (double)player.getBbHeight() * 0.42, 0.0).add(forward.scale(0.25));
      Fx.burst(level, ParticleTypes.CRIT, base.add(right.scale(0.38)), 2, 0.08, 0.02);
      Fx.burst(level, ParticleTypes.CRIT, base.add(right.scale(-0.38)), 2, 0.08, 0.02);
   }

   public static void markLanded(ServerLevel level, ServerPlayer player, LivingEntity target, Mark mark, Mark.Flavour flavour) {
      Vec3 hit = chest(target);
      switch (mark) {
         case AIRBORNE:
            Fx.burst(level, ParticleTypes.CRIT, hit, 8, 0.3, 0.15);
            Vfx.effect(VfxEntity.Kind.RING, hit).scale(0.6).color(16751150, 16771504).lifetime(6).orient(0.0F, 0.0F).spawn(level);
            Vfx.pillar(level, target.position(), 0.35, 2.0, 16751150, 6);
            Fx.sound(level, hit, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 0.8F, 0.8F);
            Fx.sound(level, hit, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 1.2F);
            shake(level, player.position(), 0.6F, 4);
            break;
         case GROUNDED:
            Vec3 feet = target.position();
            Vfx.effect(VfxEntity.Kind.RING, feet.add(0.0, 0.15, 0.0)).scale(1.6).color(16751150).lifetime(6).spawn(level);
            groundDust(level, feet, 16);
            Fx.sound(level, feet, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 0.8F, 1.1F);
            shake(level, feet, 1.2F, 6);
            break;
         default:
            int color = flavour == Mark.Flavour.WALL ? 16771504 : 16751150;
            Fx.burst(level, ParticleTypes.CRIT, hit, 8, 0.3, 0.12);
            Vfx.effect(VfxEntity.Kind.RING, hit).scale(0.8).color(color).lifetime(6).orient(-player.getYRot(), 90.0F).spawn(level);
            Fx.sound(level, hit, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 0.7F, 0.9F);
            if (flavour == Mark.Flavour.WALL) {
               Fx.sound(level, hit, SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.4F, 1.5F);
            }

            shake(level, player.position(), 0.6F, 4);
      }
   }

   public static void markTell(ServerLevel level, LivingEntity target, Mark mark, Mark.Flavour flavour, int phase) {
      switch (mark) {
         case AIRBORNE:
            Vec3 c = chest(target);
            Fx.burst(level, ParticleTypes.CRIT, c, 2, (double)target.getBbWidth() * 0.5, 0.02);
            if (!(target instanceof Player)) {
               float yaw = target.getYRot() + 7.0F;
               target.setYRot(yaw);
               target.setYBodyRot(yaw);
               target.setYHeadRot(yaw);
            }
            break;
         case GROUNDED:
            groundDust(level, target.position(), 2);
            break;
         default:
            double a = (double)phase * 0.5;
            double r = Math.max(0.4, (double)target.getBbWidth() * 0.7);
            Vec3 p = target.position().add(Math.cos(a) * r, (double)target.getBbHeight() * 0.5, Math.sin(a) * r);
            Fx.burst(level, ParticleTypes.CRIT, p, 1, 0.0, 0.0);
            if (flavour == Mark.Flavour.WALL && phase % 6 == 0) {
               Fx.burst(level, ParticleTypes.CRIT, chest(target), 2, 0.2, 0.05);
            }
      }
   }

   public static void markBounce(ServerLevel level, LivingEntity target) {
      groundDust(level, target.position(), 8);
      Fx.sound(level, target.position(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 0.5F, 0.7F);
   }

   public static void ceilingSlam(ServerLevel level, LivingEntity target) {
      Vec3 top = target.position().add(0.0, (double)target.getBbHeight(), 0.0);
      Fx.burst(level, ParticleTypes.CRIT, top, 10, 0.4, 0.2);
      Vfx.ring(level, top, 1.0, 16771504, 6, 0.0F, 0.0F);
      BlockState above = level.getBlockState(BlockPos.containing(top.x, top.y + 0.6, top.z));
      if (!above.isAir()) {
         Fx.burst(level, new BlockParticleOption(ParticleTypes.BLOCK, above), top, 12, 0.5, 0.15);
      }

      Fx.sound(level, top, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5F, 1.4F);
   }

   private static void groundDust(ServerLevel level, Vec3 feet, int count) {
      BlockState ground = level.getBlockState(BlockPos.containing(feet.x, feet.y - 0.4, feet.z));
      if (!ground.isAir()) {
         Fx.burst(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), feet.add(0.0, 0.2, 0.0), count, 0.6, 0.1);
      }
   }

   public static void punch(ServerLevel level, LivingEntity target, Mark mark) {
      Vec3 hit = chest(target);
      Fx.sound(level, hit, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.9F, 1.0F);
      Fx.burst(level, ParticleTypes.CRIT, hit, mark == null ? 2 : 3, 0.2, 0.05);
      if (mark == Mark.GROUNDED) {
         groundDust(level, target.position(), 3);
      }
   }

   public static void hookStart(ServerLevel level, ServerPlayer player) {
      Vec3 c = chest(player);
      Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5F, 0.7F);
      Fx.burst(level, ParticleTypes.CRIT, c, 3, 0.25, 0.03);
   }

   public static void hookHit(ServerLevel level, LivingEntity target) {
      Vec3 feet = target.position();
      double height = (double)target.getBbHeight();

      for (int i = 0; i <= 3; i++) {
         Fx.burst(level, ParticleTypes.CRIT, feet.add(0.0, height * (0.2 + (double)i * 0.35), 0.0), 2, 0.2, 0.06);
      }

      Vfx.pillar(level, feet, Math.max(0.3, (double)target.getBbWidth() * 0.5), height * 1.4, 16771504, 5);
      Fx.sound(level, chest(target), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.9F, 0.8F);
   }

   public static void sledgeStart(ServerLevel level, ServerPlayer player) {
      Vec3 c = chest(player);
      Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6F, 0.5F);
      Fx.burst(level, ParticleTypes.CRIT, c.add(0.0, 0.4, 0.0), 4, 0.3, 0.04);
   }

   public static void sledgeContact(ServerLevel level, LivingEntity target) {
      Vec3 c = chest(target);
      Fx.burst(level, ParticleTypes.CRIT, c, 6, 0.25, 0.05);
      Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 0.7F, 1.1F);
   }

   public static void sledgeImpact(ServerLevel level, Vec3 ground, double ringRadius, boolean crater) {
      Vec3 floor = ground.add(0.0, 0.15, 0.0);
      Vfx.effect(VfxEntity.Kind.RING, floor).scale(ringRadius).color(crater ? 16771504 : 16751150).lifetime(8).spawn(level);
      Vfx.effect(VfxEntity.Kind.RING, floor).scale(ringRadius * 1.45).color(16771504).lifetime(5).spawn(level);
      Vfx.dustDome(level, floor, ringRadius * 0.55, 10);
      Fx.groundShockwave(level, ground.add(0.0, 0.1, 0.0), 0.6, ringRadius, 5, false, 16751150);
      groundDust(level, ground, crater ? 28 : 16);
      Fx.sound(level, ground, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.0F, crater ? 0.7F : 1.0F);
      if (crater) {
         Fx.sound(level, ground, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6F, 0.8F);
      }

      shake(level, ground, 1.2F, 6);
   }

   public static void sledgeOwnerKick(ServerPlayer player, boolean crater) {
      float degrees = crater ? 0.9F : 0.5F;
      Fx.shakeFor(player, player.getEyePosition(), 16.0, BrawlerTuning.shakeMagnitude(degrees), 5, 5);
   }

   public static void sledgeSpike(ServerLevel level, LivingEntity target) {
      Vec3 feet = target.position();
      Vfx.effect(VfxEntity.Kind.RING, feet.add(0.0, 0.15, 0.0)).scale(1.8).color(16771504).lifetime(7).spawn(level);
      groundDust(level, feet, 20);
      Fx.burst(level, ParticleTypes.CRIT, chest(target), 10, 0.3, 0.2);
      Fx.sound(level, feet, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.6F);
   }

   public static void bargeStart(ServerLevel level, ServerPlayer player, boolean air) {
      Vec3 c = chest(player);
      Fx.burst(level, ParticleTypes.CRIT, c, 4, 0.3, 0.03);
      if (air) {
         Fx.burst(level, ParticleTypes.CLOUD, c.add(0.0, -0.4, 0.0), 4, 0.3, 0.02);
      } else {
         groundDust(level, player.position(), 6);
      }
   }

   public static void bargeLaunch(ServerLevel level, ServerPlayer player, Vec3 dir, boolean air) {
      Vec3 c = chest(player);
      Vec3 behind = c.subtract(dir.scale(0.45));
      Vfx.effect(VfxEntity.Kind.RING, behind)
         .scale(0.9)
         .color(16751150, 16771504)
         .lifetime(6)
         .intensity(1.2F)
         .orient(Vfx.yawAlong(dir), ringPitchOf(dir))
         .spawn(level);
      Vec3 puff = air ? behind : player.position().add(0.0, 0.2, 0.0).subtract(dir.scale(0.3));
      Fx.burst(level, ParticleTypes.CLOUD, puff, 8, 0.25, 0.08);
      Fx.burst(level, ParticleTypes.POOF, puff, 4, 0.2, 0.04);
      if (!air) {
         groundDust(level, player.position(), 8);
      }

      float pitch = 1.26F;
      Fx.sound(level, c, SoundEvents.WIND_CHARGE_THROW, SoundSource.PLAYERS, 0.9F, pitch);
      Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7F, air ? 1.5F : 1.2F);
      Fx.shakeFor(player, player.getEyePosition(), 16.0, BrawlerTuning.shakeMagnitude(0.4F), 0, 4);
   }

   public static void bargeTrail(ServerLevel level, ServerPlayer player, Vec3 from, Vec3 dir, boolean air, int tick) {
      Vec3 c = chest(player);
      if (from.distanceToSqr(c) > 0.04) {
         Vfx.beam(level, from, c, 0.13, 16751150, 4);
      }

      if (tick % 2 == 0) {
         Vfx.effect(VfxEntity.Kind.RING, c.subtract(dir.scale(0.3)))
            .scale(0.55)
            .color(16751150)
            .lifetime(5)
            .intensity(0.8F)
            .orient(Vfx.yawAlong(dir), ringPitchOf(dir))
            .spawn(level);
      }

      Fx.burst(level, Fx.dust(16751150, 1.1F), c.subtract(dir.scale(0.5)), 2, 0.25, 0.01);
      Fx.burst(level, ParticleTypes.CLOUD, c.subtract(dir.scale(0.6)).add(0.0, -0.3, 0.0), 1, 0.15, 0.01);
      if (tick % 3 == 1) {
         Fx.burst(level, ParticleTypes.SWEEP_ATTACK, c.add(dir.scale(0.4)), 1, 0.0, 0.0);
      }

      if (!air) {
         groundDust(level, player.position(), 2);
      }

      if (tick >= 6 && tick % 3 == 0) {
         Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.35F, 1.3F + 0.05F * (float)(tick % 4));
      }
   }

   public static void bargeContact(ServerLevel level, ServerPlayer player, LivingEntity target) {
      Vec3 hit = chest(target);
      Vfx.sphere(level, hit, 0.45, 16771504, 5, 1.3F);
      Fx.burst(level, ParticleTypes.CRIT, hit, 8, 0.3, 0.12);
      Fx.sound(level, hit, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 0.8F, 0.9F);
      Fx.shakeFor(player, player.getEyePosition(), 16.0, BrawlerTuning.shakeMagnitude(0.5F), 0, 3);
   }

   public static void bargeEnd(ServerLevel level, ServerPlayer player, boolean air) {
      Vec3 feet = player.position().add(0.0, 0.2, 0.0);
      Fx.burst(level, ParticleTypes.CLOUD, air ? chest(player) : feet, 5, 0.3, 0.05);
      if (!air) {
         groundDust(level, player.position(), 6);
      }
   }

   public static void bargeImpact(ServerLevel level, LivingEntity target, boolean wall) {
      Vec3 hit = chest(target);
      if (wall) {
         Fx.burst(level, ParticleTypes.CRIT, hit, 12, 0.35, 0.15);
         Vfx.effect(VfxEntity.Kind.RING, hit).scale(1.0).color(16771504).lifetime(7).orient(-target.getYRot(), 90.0F).spawn(level);
         Fx.sound(level, hit, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7F, 1.0F);
      } else {
         groundDust(level, target.position(), 14);
         Fx.sound(level, target.position(), SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 0.7F, 1.3F);
      }

      shake(level, target.position(), 1.2F, 6);
   }

   private static float ringPitchOf(Vec3 n) {
      return 90.0F - (float)Math.toDegrees(Math.asin(Mth.clamp(n.y, -1.0, 1.0)));
   }

   private static Vec3 hand(ServerPlayer player) {
      return AimUtil.handPosition(player);
   }

   public static void reelStart(ServerLevel level, ServerPlayer player) {
      Vec3 c = chest(player);
      Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.4F, 0.9F);
      Fx.burst(level, ParticleTypes.CRIT, c, 3, 0.3, 0.03);
   }

   public static void reelArmOut(ServerLevel level, ServerPlayer player) {
      Fx.sound(level, hand(player), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.7F);
   }

   public static void reelHook(ServerLevel level, ServerPlayer player, List<? extends LivingEntity> caught) {
      for (LivingEntity target : caught) {
         Vec3 hit = chest(target);
         Vfx.effect(VfxEntity.Kind.RING, hit).scale(0.7).color(16751150).lifetime(5).orient(-player.getYRot(), 90.0F).spawn(level);
         Fx.burst(level, ParticleTypes.CRIT, hit, 8, 0.3, 0.12);
      }

      if (!caught.isEmpty()) {
         Vec3 at = chest(caught.get(0));
         Fx.sound(level, at, SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.0F, 0.85F);
         Fx.sound(level, at, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 0.5F + 0.15F * (float)Math.min(2, caught.size()), 1.4F);
      }
   }

   public static void reelTrail(ServerLevel level, ServerPlayer player, LivingEntity target) {
      Vec3 from = hand(player);
      Vec3 to = chest(target);
      Vec3 away = to.subtract(from);
      double len = away.length();
      Vec3 behind = len < 0.001 ? to : to.add(away.scale(0.35 / len));
      Fx.burst(level, ParticleTypes.CRIT, behind, 1, 0.1, 0.02);
      Fx.burst(level, ParticleTypes.CLOUD, behind.add(0.0, -0.3, 0.0), 1, 0.1, 0.0);
   }

   public static void reelStrain(ServerLevel level, ServerPlayer player, int tick) {
      if (tick % 4 == 0) {
         Fx.sound(level, chest(player), SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 0.35F, 0.55F);
      }
   }

   static Vec3 guardPoint(ServerPlayer player) {
      return player.getEyePosition().add(player.getLookAngle().scale(0.55)).add(0.0, -0.2, 0.0);
   }

   public static void counterGuard(ServerLevel level, ServerPlayer player) {
      Vec3 c = chest(player);
      Vfx.effect(VfxEntity.Kind.RING, c).scale(0.8).color(16777215).lifetime(5).orient(-player.getYRot(), 90.0F).spawn(level);
      Fx.sound(level, player.position(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.35F, 1.8F);
   }

   private static double parryRadius(float damage) {
      return Mth.clamp(1.0 + (double)damage * 0.15, 1.0, 8.0);
   }

   private static int parryRingTicks(double radius) {
      double t = (radius - 1.0) / 7.0;
      return 6 + (int)Math.round(t * 4.0);
   }

   public static void parryBeat(ServerLevel level, ServerPlayer player, LivingEntity attacker, float damage, boolean split) {
      Vec3 guard = guardPoint(player);
      Vec3 c = chest(player);
      Vec3 look = player.getLookAngle();
      float yaw = -player.getYRot();
      double radius = parryRadius(damage);
      int ticks = parryRingTicks(radius);
      Vfx.sphere(level, guard, 0.55, 16777215, 3, 1.6F);
      Vfx.effect(VfxEntity.Kind.RING, c).scale(radius).color(16777215).lifetime(ticks).orient(yaw, 90.0F).spawn(level);
      if (split) {
         Vec3 behind = c.subtract(look.scale(0.6));
         Vfx.effect(VfxEntity.Kind.RING, behind).scale(radius).color(16777215).lifetime(ticks).orient(yaw, 90.0F).delay(2).spawn(level);
         Vfx.effect(VfxEntity.Kind.DOME, player.position().subtract(look.scale(0.4)))
            .scale(radius * 0.45)
            .color(16777215)
            .lifetime(ticks)
            .intensity(0.7F)
            .delay(2)
            .spawn(level);
      }

      if (damage >= 20.0F) {
         Vfx.dome(level, c, radius * 0.6, 16777215, 10);
         Vfx.effect(VfxEntity.Kind.RING, c).scale(radius).color(16777215).lifetime(ticks).orient(yaw, 90.0F).delay(3).spawn(level);
      }

      Fx.burst(level, ParticleTypes.END_ROD, guard, 5, 0.25, 0.08);
      Fx.burst(level, ParticleTypes.CRIT, guard, 8, 0.35, 0.18);
      Fx.sound(level, guard, SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.7F, 1.6F);
      Fx.sound(level, guard, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.45F, 1.7F);
      Fx.sound(level, guard, (SoundEvent)SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.75F, 0.55F);
      float kick = Math.min(1.8F, 0.6F + damage * 0.06F);
      Fx.shakeFor(player, player.getEyePosition(), 6.0, BrawlerTuning.shakeMagnitude(kick), 2, 4);
      shake(level, c, 0.6F, 4);
      if (attacker != null) {
         Fx.burst(level, ParticleTypes.CRIT, chest(attacker), 6, 0.3, 0.15);
      }
   }

   public static void counterReflect(ServerLevel level, LivingEntity attacker) {
      Vec3 hit = chest(attacker);
      Fx.burst(level, ParticleTypes.CRIT, hit, 8, 0.3, 0.2);
      Fx.sound(level, hit, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.9F, 0.9F);
   }

   private static void shotRing(ServerLevel level, Vec3 at, Vec3 dir) {
      Vfx.effect(VfxEntity.Kind.RING, at).scale(0.5).color(16777215).lifetime(5).orient(Vfx.yawAlong(dir), ringPitchOf(dir)).spawn(level);
   }

   public static void counterReturn(ServerLevel level, Vec3 met, Vec3 dir) {
      shotRing(level, met, dir);
      Vfx.beam(level, met, met.add(dir.scale(1.5)), 0.08, 16777215, 4);
      Fx.sound(level, met, SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.6F, 1.6F);
   }

   public static void counterNegate(ServerLevel level, Vec3 at, Vec3 dir) {
      shotRing(level, at, dir);
      Vfx.sphere(level, at, 0.5, 16777215, 4, 1.2F);
      Fx.burst(level, ParticleTypes.POOF, at, 6, 0.2, 0.02);
   }

   public static void dojoWindup(ServerLevel level, LivingEntity dummy, Vec3 hands) {
      Fx.burst(level, ParticleTypes.CRIT, hands, 6, 0.2, 0.1);
      Fx.sound(level, dummy.position(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.5F, 0.7F);
   }

   public static void dojoBlastCharge(ServerLevel level, Vec3 hands, double frac, boolean humTick) {
      Vfx.effect(VfxEntity.Kind.SPHERE, hands).scale(0.15 + 0.4 * frac).color(11820287, 16777215).lifetime(3).intensity(0.9F + (float)frac * 0.6F).spawn(level);
      Fx.burst(level, ParticleTypes.WITCH, hands, 1, 0.3, 0.02);
      if (humTick) {
         Fx.sound(level, hands, SoundEvents.CONDUIT_AMBIENT, SoundSource.HOSTILE, 0.5F, 0.6F + (float)frac * 0.9F);
      }
   }

   public static void dojoBlastLaunch(ServerLevel level, Vec3 hands) {
      Vfx.sphere(level, hands, 0.6, 11820287, 4, 1.4F);
      Fx.sound(level, hands, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.8F, 1.3F);
   }

   public static void dojoBlastDetonate(ServerLevel level, Vec3 at, double radius, int ticks) {
      VfxBolt.blast(level, at, radius, ticks, 11820287);
      Vfx.sphere(level, at, 1.2, 11820287, 6, 1.5F);
      Fx.burst(level, ParticleTypes.EXPLOSION_EMITTER, at, 1, 0.0, 0.0);
      Fx.burst(level, ParticleTypes.EXPLOSION, at, 8, radius * 0.4, 0.02);
      Fx.ring(level, ParticleTypes.EXPLOSION, at.add(0.0, 0.2, 0.0), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), radius * 0.8, 12, 0.0);
      Fx.sound(level, at, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2F, 1.0F);
      shake(level, at, 1.2F, 6);
   }

   public static void dojoBlastBurst(ServerLevel level, Vec3 at) {
      Vfx.sphere(level, at, 0.9, 11820287, 5, 1.2F);
      Fx.burst(level, ParticleTypes.EXPLOSION, at, 1, 0.0, 0.0);
      Fx.sound(level, at, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8F, 1.2F);
   }

   public static void dojoNukeCharge(ServerLevel level, Vec3 hands, double frac, boolean humTick) {
      NuclearFx.gather(level, hands, frac, 1.4);
      if (humTick) {
         Fx.sound(level, hands, SoundEvents.CONDUIT_AMBIENT, SoundSource.HOSTILE, 0.7F, 0.4F + (float)frac * 0.9F);
      }
   }

   public static void dojoNukeDetonate(ServerLevel level, Vec3 feet, double radius) {
      NuclearFx.boom(level, feet, radius, NuclearFx.Kind.MUSHROOM);
      NuclearFx.screenShake(level, feet, 24.0, 12);
   }

   public static void dojoNukeArrive(ServerLevel level, Vec3 at) {
      Vfx.sphere(level, at, 1.4, 16762170, 6, 1.1F);
      Fx.burst(level, ParticleTypes.EXPLOSION, at, 1, 0.0, 0.0);
      Fx.burst(level, ParticleTypes.FLAME, at, 10, 0.5, 0.08);
      Fx.sound(level, at, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0F, 0.8F);
   }

   public static void whiff(ServerLevel level, ServerPlayer player) {
      Fx.sound(level, player.position(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 0.8F, 1.1F);
   }
}
