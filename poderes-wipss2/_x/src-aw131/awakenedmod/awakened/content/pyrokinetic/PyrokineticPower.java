package com.awakenedmod.awakened.content.pyrokinetic;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.caster.PlayerCaster;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerProgress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class PyrokineticPower extends Power {
   public static final int CONTROL_LEVEL = 5;
   private static final int OPAQUE = -16777216;
   private static final float PULSE_SPEED = 0.15F;
   private static final float PULSE_DEPTH = 0.22F;
   private static final float OVERDRIVE_PULSE_SPEED = 0.6F;
   private static final int OVERDRIVE_RGB = 16726820;
   private static final int OVERDRIVE_HOT = 16773344;
   private static final float OVERDRIVE_LEAN = 0.35F;
   private static final float OVERDRIVE_PULSE_MIN = 0.3F;
   private static final float OVERDRIVE_PULSE_GAIN = 0.35F;

   public PyrokineticPower() {
      super("pyrokinetic", kit());
   }

   private static List<Ability> kit() {
      List<Ability> beamGuards = new ArrayList<>();
      List<Ability> jetGuards = new ArrayList<>();
      List<Ability> barrageGuards = new ArrayList<>();
      FlameBeamAbility beam = new FlameBeamAbility(beamGuards);
      FireJetsAbility jets = new FireJetsAbility(jetGuards);
      BarrageAbility barrage = new BarrageAbility(barrageGuards);
      BackdraftAbility backdraft = new BackdraftAbility(List.of(beam, jets, barrage));
      beamGuards.add(jets);
      beamGuards.add(barrage);
      beamGuards.add(backdraft);
      jetGuards.add(beam);
      jetGuards.add(barrage);
      jetGuards.add(backdraft);
      barrageGuards.add(beam);
      barrageGuards.add(jets);
      barrageGuards.add(backdraft);
      return List.of(new FireballAbility(List.of(beam, jets, barrage, backdraft)), beam, jets, barrage, backdraft);
   }

   @Override
   public int themeColor() {
      return -34261;
   }

   @Override
   public int signatureSlot() {
      return 1;
   }

   @Override
   public void passiveTick(ServerPlayer player, PowerData data) {
      PyroHeatState.tick(PlayerCaster.of(player));
      keepFireImmunity(player, data.masteryLevel());
      super.passiveTick(player, data);
   }

   public static void keepFireImmunity(LivingEntity body, int masteryLevel) {
      if (masteryLevel >= 5 && body.tickCount % 20 == 0) {
         body.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false, false));
      }
   }

   @Override
   public void onLogin(ServerPlayer player, PowerData data) {
      PyroHeatState.onLogin(player);
   }

   @Override
   public void onLogout(ServerPlayer player) {
      PyroHeatState.onLogout(player);
   }

   @Override
   public void onDeath(ServerPlayer victim, DamageSource source) {
      PyroHeatState.onDeath(victim);
   }

   @Override
   public void onSuppressed(ServerPlayer player, PowerData data) {
      PyroHeatState.onDeath(player);
   }

   @Override
   public void onReplaced(ServerPlayer player) {
      PyroHeatState.onReplaced(player);
   }

   @Override
   public boolean hasResourceBar() {
      return true;
   }

   @Override
   public int resourceBarColor() {
      return 0xFF000000 | FlameTone.ORANGE.uiColor();
   }

   @Override
   public int resourceBarColor(@Nullable Player viewer, float fraction) {
      float blue = FlameStyle.blueFraction(fraction);
      int rgb = FlameTone.uiColor(blue);
      if (blue >= 1.0F && viewer != null) {
         float pulse = 0.5F + 0.5F * Mth.sin((float)viewer.tickCount * 0.15F);
         rgb = FlameTone.mixRgb(rgb, FlameStyle.BLUE.tip(), 0.22F * pulse);
      }

      return 0xFF000000 | rgb;
   }

   @Override
   public Component resourceBarBadge(Player viewer, float fraction) {
      int percent = Math.round(PyroHeatState.heatDamageBonus(fraction) * 100.0F);
      return Component.literal("+" + percent + "%");
   }

   @Override
   public Component resourceBarLabel() {
      return Component.translatable("awakened.pyrokinetic.heat");
   }

   @Override
   public float resourceBarFraction(ServerPlayer player, PowerData data) {
      return PyroHeatState.heat(data);
   }

   @Override
   public float resourceBarCeiling(@Nullable Player viewer, int masteryLevel) {
      return PyroHeatState.cap(masteryLevel);
   }

   @Override
   public int resourceBarOverColor(@Nullable Player viewer, float fraction, float ceiling) {
      if (fraction <= ceiling) {
         return 0;
      } else {
         float over = PyroHeatState.overdrive(fraction, ceiling);
         int rgb = FlameTone.mixRgb(16726820, 16773344, 0.35F * over);
         if (viewer != null) {
            float pulse = 0.5F + 0.5F * Mth.sin((float)viewer.tickCount * 0.6F);
            rgb = FlameTone.mixRgb(rgb, 16773344, (0.3F + 0.35F * over) * pulse);
         }

         return 0xFF000000 | rgb;
      }
   }

   @Override
   public PowerProgress progress(Player player, int masteryLevel) {
      float heat = PyroHeatState.visualHeat(player);
      float cap = PyroHeatState.cap(masteryLevel);
      Component state = Component.translatable(heat >= 0.7F ? "awakened.pyrokinetic.progress.blue" : "awakened.pyrokinetic.progress.orange");
      List<PowerProgress.Stat> stats = List.of(
         new PowerProgress.Stat(Component.translatable("awakened.pyrokinetic.progress.ceiling_l"), Component.literal(percent(cap))),
         new PowerProgress.Stat(Component.translatable("awakened.pyrokinetic.progress.overdrive_l"), overdriveValue(heat, cap, masteryLevel)),
         new PowerProgress.Stat(
            Component.translatable("awakened.pyrokinetic.progress.bonus_l"), Component.literal("+" + percent(PyroHeatState.heatDamageBonus(heat)))
         ),
         new PowerProgress.Stat(Component.translatable("awakened.pyrokinetic.progress.turns_blue_l"), Component.literal(percent(0.7F))),
         new PowerProgress.Stat(
            Component.translatable("awakened.pyrokinetic.progress.immunity_l"),
            Component.translatable(
               masteryLevel >= 5 ? "awakened.pyrokinetic.progress.immunity_on" : "awakened.pyrokinetic.progress.immunity_at", new Object[]{5}
            )
         )
      );
      return new PowerProgress(heat, state, Component.literal(percent(heat)), stats);
   }

   private static Component overdriveValue(float heat, float cap, int masteryLevel) {
      if (cap >= 1.0F) {
         return Component.translatable("awakened.pyrokinetic.progress.overdrive_none");
      } else {
         float burn = PyroHeatState.overdriveBurnPerSecond(heat, masteryLevel);
         return burn > 0.0F
            ? Component.translatable("awakened.pyrokinetic.progress.overdrive_on", new Object[]{oneDecimal(burn)})
            : Component.translatable("awakened.pyrokinetic.progress.overdrive_off", new Object[]{percent(cap)});
      }
   }

   private static String percent(float fraction) {
      return Math.round(fraction * 100.0F) + "%";
   }

   private static String oneDecimal(float value) {
      return String.format(Locale.ROOT, "%.1f", value);
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      VoxelFlame.burst(level, player.position().add(0.0, 1.0, 0.0), 2.0, FlameTone.ORANGE);
      Fx.sound(level, player.position(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2F, 0.6F);
   }
}
