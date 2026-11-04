package com.awakenedmod.awakened.content.item;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.exosavant.ExoSuit;
import com.awakenedmod.awakened.content.exosavant.ExoTags;
import com.awakenedmod.awakened.content.exosavant.WarhammerThrowEntity;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.compat.BetterCombatCompat;
import com.awakenedmod.awakened.core.progression.BalanceKeyed;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ExosavantWarhammerItem extends Item implements BalanceKeyed {
   private static final int USE_DURATION = 72000;
   private static final int MIN_CHARGE_TICKS = 6;
   private static final int MAX_CHARGE_TICKS = 30;
   private static final int SLAM_COOLDOWN = 30;
   private static final float SLAM_BASE_DMG = 6.0F;
   private static final float SLAM_CHARGE_DMG = 8.0F;
   private static final double SLAM_BASE_RADIUS = 2.2;
   private static final double SLAM_CHARGE_RADIUS = 2.8;
   private static final double SLAM_UNSUITED_RADIUS = 2.0;
   private static final double SLAM_KNOCKBACK = 1.4;
   private static final double SLAM_KNOCKUP = 0.75;
   private static final double FALL_CAP = 24.0;
   private static final double FALL_DIVISOR = 16.0;
   private static final double RADIUS_AMP_CAP = 1.4;
   private static final String SLAM_BALANCE_KEY = "ExosavantWarhammerSlam";
   private static final double THROW_LAUNCH_SPEED = 1.5;
   private static final int THROW_COOLDOWN = 30;
   private static final double CLEAVE_RANGE = 3.5;
   private static final double CLEAVE_ARC_HALF = 60.0;
   private static final int CLEAVE_MAX_TARGETS = 3;
   private static final float CLEAVE_DMG_FRACTION = 0.7F;
   private static final String CLEAVE_BALANCE_KEY = "ExosavantWarhammerCleave";
   private static final double CLEAVE_KNOCKBACK = 0.6;
   private static final float CLEAVE_CHARGE = 0.9F;

   public ExosavantWarhammerItem(Properties properties) {
      super(properties);
   }

   @Override
   public List<String> balanceKeys() {
      return List.of("ExosavantWarhammerCleave", "ExosavantWarhammerSlam");
   }

   private static void onHit(LivingEntity target, LivingEntity attacker) {
      if (attacker instanceof ServerPlayer player && target.isAlive()) {
         cleave(player, target);
      }
   }

   private static void cleave(ServerPlayer player, LivingEntity primary) {
      ServerLevel level = player.serverLevel();
      float attack = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE);
      ExoTags.applyTaggedBonus(player, primary, level, MasteryScaling.configScaledDamage(attack, "ExosavantWarhammerCleave"));
      if (!BetterCombatCompat.ownsSwing() && !(player.getAttackStrengthScale(0.5F) < 0.9F)) {
         Vec3 eye = player.getEyePosition();
         Vec3 look = player.getLookAngle();
         double cosHalf = Math.cos(Math.toRadians(60.0));
         float dmg = MasteryScaling.scaledDamage(attack * 0.7F, player, "ExosavantWarhammerCleave");
         AABB box = player.getBoundingBox().inflate(3.5);
         int hit = 0;

         for (LivingEntity victim : level.getEntitiesOfClass(
            LivingEntity.class, box, e -> e != player && e != primary && e.isAlive() && e.attackable() && !e.isAlliedTo(player) && Harm.allowed(player, e)
         )) {
            if (hit >= 3) {
               break;
            }

            Vec3 to = victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0).subtract(eye);
            double dist = to.length();
            if (!(dist > 3.5)
               && !(dist < 0.001)
               && !(to.scale(1.0 / dist).dot(look) < cosHalf)
               && Dmg.weaponHurt(victim, level, level.damageSources().playerAttack(player), dmg)) {
               victim.knockback(0.6, eye.x - victim.getX(), eye.z - victim.getZ());
               victim.hurtMarked = true;
               ExoTags.applyTaggedBonus(player, victim, level, dmg);
               Fx.burst(level, ParticleTypes.CRIT, victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0), 6, 0.3, 0.1);
               hit++;
            }
         }

         Fx.sound(level, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.6F);
      }
   }

   public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      onHit(target, attacker);
      return true;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack held = player.getItemInHand(hand);
      if (player.isShiftKeyDown()) {
         if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
            throwHammer(server, sp, hand);
         }

         return InteractionResultHolder.consume(held);
      } else {
         player.startUsingItem(hand);
         return InteractionResultHolder.consume(held);
      }
   }

   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return 72000;
   }

   public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
      release(stack, level, entity, timeLeft);
   }

   private static void release(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
      if (level instanceof ServerLevel server && entity instanceof ServerPlayer player) {
         int charge = 72000 - timeLeft;
         if (charge < 6) {
            return;
         }

         slam(server, player, stack, charge);
         return;
      }
   }

   private static void slam(ServerLevel server, ServerPlayer player, ItemStack stack, int charge) {
      float chargeFrac = Mth.clamp((float)(charge - 6) / 24.0F, 0.0F, 1.0F);
      float heightAmp = 1.0F + (float)Math.min((double)player.fallDistance, 24.0) / 16.0F;
      boolean suited = ExoSuit.isSuited(player);
      Vec3 c = player.position();
      DamageSource src = Dmg.by(player, DamageTypes.PLAYER_ATTACK);
      double radius = suited ? (2.2 + (double)chargeFrac * 2.8) * Math.min((double)heightAmp, 1.4) : 2.0;
      float dmg = MasteryScaling.scaledDamage((6.0F + chargeFrac * 8.0F) * heightAmp, player, "ExosavantWarhammerSlam");
      AABB area = new AABB(c, c).inflate(radius, 3.0, radius);

      for (LivingEntity e : server.getEntitiesOfClass(
         LivingEntity.class, area, t -> t != player && t.isAlive() && t.attackable() && !t.isAlliedTo(player) && Harm.allowed(player, t)
      )) {
         if (!(e.distanceToSqr(c) > radius * radius) && Dmg.weaponHurt(e, server, src, dmg)) {
            Vec3 push = e.position().subtract(c);
            push = push.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 1.0, 0.0) : push.normalize();
            double kb = suited ? 1.4 : 0.5599999999999999;
            double up = suited ? 0.75 : 0.3;
            e.push(push.x * kb, up, push.z * kb);
            e.hurtMarked = true;
            ExoTags.applyTaggedBonus(player, e, server, dmg);
         }
      }

      if (suited) {
         Fx.groundShockwave(server, c, 0.6, radius, Math.min((int)(radius * 2.2), 20), true, 10475775);
         Vfx.ring(server, c.add(0.0, 0.12, 0.0), radius * 0.6, 15792895, 12);
         Fx.shake(server, c, radius * 4.0, 0.35F, 4, 14);
         Fx.screenShake(server, c, radius * 3.5, 12, 10475775, true);
         Fx.sound(server, c, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 0.7F);
      } else {
         Fx.groundImpact(server, c, radius, true);
      }

      Fx.sound(server, c, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.9F, 0.6F);
      player.fallDistance = 0.0F;
      player.getCooldowns().addCooldown(stack.getItem(), 30);
   }

   private static void throwHammer(ServerLevel server, ServerPlayer player, InteractionHand hand) {
      ItemStack held = player.getItemInHand(hand);
      if (!held.isEmpty()) {
         Vec3 look = player.getLookAngle();
         Vec3 spawn = player.getEyePosition().add(look.scale(0.6));
         WarhammerThrowEntity hammer = new WarhammerThrowEntity(AwakenedEntities.EXO_WARHAMMER.get(), player, server, held.copy());
         hammer.setPos(spawn.x, spawn.y, spawn.z);
         hammer.shoot(look.x, look.y, look.z, 1.5F, 0.0F);
         server.addFreshEntity(hammer);
         player.getCooldowns().addCooldown(held.getItem(), 30);
         player.setItemInHand(hand, ItemStack.EMPTY);
         Fx.sound(server, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.1F, 0.7F);
         Fx.sound(server, player.position(), SoundEvents.NETHERITE_BLOCK_BREAK, SoundSource.PLAYERS, 0.6F, 1.3F);
      }
   }
}
