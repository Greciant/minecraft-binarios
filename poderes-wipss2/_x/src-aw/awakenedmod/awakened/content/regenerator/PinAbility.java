package com.awakenedmod.awakened.content.regenerator;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.TickFreeze;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class PinAbility extends RegenAbility {
   private static final float COST = 14.0F;
   private static final int UNLOCK = 7;
   private static final double TACKLE_RANGE = 6.0;
   private static final double TACKLE_SPEED = 1.1;
   private static final double HOLD_AHEAD = 0.8;
   private static final double BREAK_DISTANCE = 3.2;
   private static final int GRACE_TICKS = 6;
   private static final int RAKE_INTERVAL = 20;
   private static final float TACKLE_DAMAGE = 5.0F;
   private static final float RAKE_DAMAGE = 2.0F;
   private static final int MAX_TICKS = 200;
   private final Map<UUID, PinAbility.Pin> pins = this.activeState();

   public PinAbility() {
      super("pin", 14.0F, 7);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(5.0).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.pins.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.regenerator.pin_busy", new Object[0]);
      } else {
         LivingEntity target = AimUtil.aimedLivingEntity(player, 6.0, e -> Harm.allowed(player, e));
         if (target == null) {
            return this.fail(player, "awakened.regenerator.pin_no_target", new Object[0]);
         } else if (!Harm.controls(player, target)) {
            return this.fail(player, "awakened.feedback.pvp_control_off", new Object[0]);
         } else {
            ServerLevel level = player.serverLevel();
            Vec3 dir = horizontalToward(player, target.position());
            player.setDeltaMovement(dir.x * 1.1, 0.1, dir.z * 1.1);
            player.hurtMarked = true;
            player.swing(InteractionHand.MAIN_HAND);
            clawHit(player, target, MasteryScaling.scaledDamage(5.0F, player, this.balanceKey()));
            if (!(target instanceof Player)) {
               TickFreeze.freeze(target);
            }

            this.pins.put(player.getUUID(), new PinAbility.Pin(target));
            PowerService.playAnimation(player, "pin");
            this.holdDown(player, target);
            Fx.burst(level, ParticleTypes.CRIT, target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0), 16, 0.4, 0.1);
            Fx.sound(level, player.position(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.9F, 1.5F);
            Fx.sound(level, player.position(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.8F);
            player.displayClientMessage(Component.translatable("awakened.regenerator.pinned").withStyle(ChatFormatting.RED), true);
            MasterySystem.award(player, MasteryReason.HIT, target);
            return true;
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      PinAbility.Pin pin = this.pins.get(player.getUUID());
      if (pin != null) {
         LivingEntity target = pin.target;
         if (target.isAlive() && !target.isRemoved()) {
            if (pin.ticks >= 6 && (double)player.distanceTo(target) > 3.2) {
               this.release(player, "awakened.regenerator.pin_broken");
            } else {
               this.holdDown(player, target);
               if (pin.ticks > 0 && pin.ticks % 20 == 0) {
                  clawHit(player, target, MasteryScaling.scaledDamage(2.0F, player, this.balanceKey()));
                  Fx.sound(player.serverLevel(), target.position(), SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.7F, 0.8F);
                  MasterySystem.award(player, MasteryReason.HIT, target);
               }

               if (++pin.ticks >= 200) {
                  this.release(player, "awakened.regenerator.pin_released");
               }
            }
         } else {
            if (Foes.defeatedBy(target, player)) {
               QuestSystem.feat(player, "regen_pin_kill");
            }

            this.pins.remove(player.getUUID());
            letGo(target);
            PowerService.stopAnimation(player);
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      if (this.pins.containsKey(player.getUUID())) {
         this.release(player, "awakened.regenerator.pin_released");
      }
   }

   private void holdDown(ServerPlayer player, LivingEntity target) {
      ServerLevel level = player.serverLevel();
      Vec3 ahead = horizontalToward(player, target.position());
      double hx = player.getX() + ahead.x * 0.8;
      double hz = player.getZ() + ahead.z * 0.8;
      target.setPos(hx, player.getY(), hz);
      target.setDeltaMovement(Vec3.ZERO);
      target.hurtMarked = true;
      target.resetFallDistance();
      target.setSprinting(false);
      if (target instanceof Player) {
         Harm.effect(player, target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8, 6, false, false));
         Harm.effect(player, target, new MobEffectInstance(MobEffects.WEAKNESS, 8, 2, false, false));
         Harm.effect(player, target, new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 8, 2, false, false));
      }

      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8, 2, false, false));
      Fx.burst(level, ParticleTypes.CRIT, target.position().add(0.0, (double)target.getBbHeight() * 0.6, 0.0), 1, 0.15, 0.0);
      Fx.burst(level, ParticleTypes.DAMAGE_INDICATOR, target.position().add(0.0, (double)target.getBbHeight() * 0.6, 0.0), 1, 0.1, 0.0);
   }

   private void release(ServerPlayer player, String messageKey) {
      PinAbility.Pin pin = this.pins.remove(player.getUUID());
      PowerService.stopAnimation(player);
      Fx.sound(player.serverLevel(), player.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 0.6F, 1.1F);
      if (pin != null) {
         letGo(pin.target);
         player.displayClientMessage(Component.translatable(messageKey).withStyle(ChatFormatting.RED), true);
      }
   }

   private static void letGo(LivingEntity target) {
      if (!(target instanceof Player)) {
         TickFreeze.release(target);
      }
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      PinAbility.Pin pin = this.pins.remove(player.getUUID());
      if (pin != null) {
         letGo(pin.target);
         PowerService.stopAnimation(player);
      }

      super.clearPlayer(player);
   }

   private static Vec3 horizontalToward(ServerPlayer player, Vec3 to) {
      Vec3 flat = new Vec3(to.x - player.getX(), 0.0, to.z - player.getZ());
      if (flat.lengthSqr() < 1.0E-4) {
         Vec3 look = player.getLookAngle();
         flat = new Vec3(look.x, 0.0, look.z);
      }

      return flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
   }

   private static final class Pin {
      private final LivingEntity target;
      private int ticks;

      private Pin(LivingEntity target) {
         this.target = target;
      }
   }
}
