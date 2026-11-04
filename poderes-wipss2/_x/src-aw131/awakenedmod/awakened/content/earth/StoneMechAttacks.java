package com.awakenedmod.awakened.content.earth;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class StoneMechAttacks {
   private static final String STONE_MECH_KEY = StoneMechAbility.class.getSimpleName();
   private static final int HAMMER_DURATION = 16;
   private static final int HAMMER_STRIKE = 8;
   private static final int HAMMER_COOLDOWN = 16;
   private static final float HAMMER_DAMAGE = 20.0F;
   private static final double HAMMER_REACH = 4.5;
   private static final double HAMMER_WIDTH = 1.5;
   private static final int BOULDER_DURATION = 14;
   private static final int BOULDER_STRIKE = 7;
   private static final int BOULDER_COOLDOWN = 40;
   private static final float BOULDER_DAMAGE = 24.0F;
   private static final float BOULDER_SIZE = 1.4F;
   private static final float POUND_DAMAGE = 14.0F;
   private static final double POUND_RADIUS = 5.0;
   private static final int POUND_RECOVER = 6;
   private static final int POUND_AIR_FAILSAFE = 80;

   private StoneMechAttacks() {
   }

   static void hammer(ServerPlayer player, StoneMechControl.Pilot p) {
      if (p.hammerCd <= 0 && p.action == 0) {
         p.hammerCd = 16;
         StoneMechControl.startAction(p, p.mech, (byte)2, 16, 8);
         if (p.mech.level() instanceof ServerLevel level) {
            Fx.sound(level, p.mech.position(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1.2F, 0.6F);
         }
      }
   }

   static void boulder(ServerPlayer player, StoneMechControl.Pilot p) {
      if (p.boulderCd <= 0 && p.action == 0) {
         p.boulderCd = 40;
         StoneMechControl.startAction(p, p.mech, (byte)4, 14, 7);
      }
   }

   static void onStrike(ServerPlayer player, StoneMechControl.Pilot p, StoneMechEntity mech, byte action) {
      if (mech.level() instanceof ServerLevel level) {
         switch (action) {
            case 2:
               hammerStrike(player, mech, level);
               break;
            case 4:
               boulderStrike(player, mech, level);
         }
      }
   }

   private static void hammerStrike(ServerPlayer player, StoneMechEntity mech, ServerLevel level) {
      Vec3 look = mech.getLookAngle();
      AABB box = mech.getBoundingBox().expandTowards(look.scale(4.5)).inflate(1.5);
      DamageSource src = Dmg.by(player, DamageTypes.FALLING_BLOCK);
      float dmg = MasteryScaling.scaledDamage(20.0F, player, STONE_MECH_KEY);
      Vec3 center = mech.position();

      for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, t -> t.isAlive() && t != mech && t != player && Harm.allowed(player, t))) {
         if (Dmg.hurt(e, level, src, dmg)) {
            Vec3 dir = e.position().subtract(center);
            dir = dir.lengthSqr() < 1.0E-4 ? look : dir.normalize();
            e.push(dir.x * 1.4, 0.45, dir.z * 1.4);
            e.hurtMarked = true;
            Harm.effect(player, e, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 0));
            MasterySystem.award(player, MasteryReason.HIT, e);
            creditRout(player, e);
         }
      }

      Vec3 impact = mech.position().add(look.x * 2.5, 0.0, look.z * 2.5);
      Fx.groundImpact(level, impact, 2.5, true);
      Fx.groundShockwave(level, impact, 0.5, 3.0, 6, true);
      Fx.shake(level, impact, 22.0, 0.3F, 2, 14);
      Fx.burst(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()), impact.add(0.0, 0.5, 0.0), 18, 0.6, 0.12);
   }

   private static void boulderStrike(ServerPlayer player, StoneMechEntity mech, ServerLevel level) {
      EarthMaterial mat = EarthMaterial.under(mech);
      Vec3 look = mech.getLookAngle();
      Vec3 spawnPos = mech.getEyePosition().add(look.scale(2.5));
      HoverRockEntity rock = HoverRockEntity.spawn(level, spawnPos, spawnPos, mat, 1.4F, player.getUUID());
      rock.launch(look, MasteryScaling.scaledDamage(24.0F, player, STONE_MECH_KEY), STONE_MECH_KEY);
   }

   static void tickGroundPound(ServerPlayer player, StoneMechControl.Pilot p, StoneMechEntity mech, boolean leapThisTick, boolean wasOnGround) {
      if (leapThisTick) {
         p.poundCd = 50;
         p.poundAirborne = true;
         p.poundAirTicks = 0;
         StoneMechControl.startAction(p, mech, (byte)3, 1, -1);
         if (mech.level() instanceof ServerLevel level) {
            Fx.groundImpact(level, mech.position(), 1.5, false);
            Fx.sound(level, mech.position(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1.0F, 0.8F);
         }
      }

      if (p.action == 3) {
         if (p.poundAirborne) {
            mech.fallDistance = 0.0F;
            mech.setActionProgress(0.35F);
            boolean landed = !wasOnGround && mech.onGround();
            if (landed || ++p.poundAirTicks >= 80) {
               p.poundAirborne = false;
               p.poundRecover = 6;
               groundPoundSlam(player, mech);
            }
         } else if (p.poundRecover > 0) {
            float t = 1.0F - (float)p.poundRecover / 6.0F;
            mech.setActionProgress(0.55F + 0.45F * t);
            if (--p.poundRecover <= 0) {
               p.action = 0;
               mech.setAction((byte)0);
               mech.setActionProgress(0.0F);
            }
         }
      }
   }

   private static void creditRout(ServerPlayer player, LivingEntity foe) {
      if (Foes.defeatedBy(foe, player)) {
         QuestSystem.feat(player, "geo_mech_rout");
      }
   }

   private static void groundPoundSlam(ServerPlayer player, StoneMechEntity mech) {
      if (mech.level() instanceof ServerLevel level) {
         Vec3 var10 = mech.position();
         DamageSource src = Dmg.by(player, DamageTypes.FALLING_BLOCK);
         float dmg = MasteryScaling.scaledDamage(14.0F, player, STONE_MECH_KEY);
         AABB area = new AABB(var10, var10).inflate(5.0, 3.0, 5.0);

         for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, t -> t.isAlive() && t != mech && t != player && Harm.allowed(player, t))) {
            if (!(e.distanceToSqr(var10) > 25.0) && Dmg.hurt(e, level, src, dmg)) {
               Vec3 push = e.position().subtract(var10);
               push = push.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 1.0, 0.0) : push.normalize();
               e.push(push.x * 0.9, 0.7, push.z * 0.9);
               e.hurtMarked = true;
               MasterySystem.award(player, MasteryReason.HIT, e);
               creditRout(player, e);
            }
         }

         Fx.groundShockwave(level, var10, 1.0, 6.0, 12, true);
         Fx.groundImpact(level, var10, 3.0, true);
         Fx.shake(level, var10, 26.0, 0.4F, 4, 16);
         Fx.screenShake(level, var10, 22.0, 12, 10520680, true);
      }
   }
}
