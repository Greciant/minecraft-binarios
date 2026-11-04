package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.PassiveToggle;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerProgress;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class GravitonPower extends Power {
   public static final String ID = "graviton";
   public static final int THEME = -30147;
   private static final float[] WEIGHT_TICKS = new float[]{0.16666667F, 0.33333334F, 0.6666667F, 0.8333333F};
   private static final int LIGHT_SIDE = -7484;

   public GravitonPower() {
      super("graviton", List.of(new HeavyHandAbility(), new GravityWellAbility(), new WeightlessAbility(), new DenseAbility(), new EventHorizonAbility()));
   }

   @Override
   public int themeColor() {
      return -30147;
   }

   @Override
   public int signatureSlot() {
      return 0;
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 pos = player.position().add(0.0, 1.0, 0.0);
      Fx.burst(level, ParticleTypes.SONIC_BOOM, pos, 1, 0.0, 0.0);
      Fx.ring(level, Fx.dust(16747069, 1.4F), pos, new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), 1.6, 40, 0.0);
      Fx.burst(level, ParticleTypes.SMALL_FLAME, pos, 30, 0.8, 0.02);
      Fx.sound(level, pos, (SoundEvent)AwakenedSounds.AWAKEN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
   }

   @Override
   public boolean handlesSecondaryKey() {
      return true;
   }

   @Override
   public void onSecondaryKey(ServerPlayer player, boolean sneaking, boolean held) {
      MassControl.onKey(player, sneaking, held);
   }

   @Override
   public void passiveTick(ServerPlayer player, PowerData data) {
      super.passiveTick(player, data);
      MassControl.apply(player, data);
   }

   @Override
   protected void onAbilityStateCleared(ServerPlayer player) {
      MassControl.clear(player);
   }

   @Override
   public void onReplaced(ServerPlayer player) {
      MassControl.clear(player);
   }

   @Override
   public List<PassiveToggle> passiveToggles() {
      return List.of(MassControl.MESSAGE_TOGGLE);
   }

   @Override
   public List<String> extraBalanceKeys() {
      return List.of("MassControlShockwave");
   }

   @Override
   public boolean hasResourceBar() {
      return true;
   }

   @Override
   public int resourceBarColor() {
      return -30147;
   }

   @Override
   public int resourceBarColor(@Nullable Player viewer, float fraction) {
      return fraction < MassControl.fraction(0) - 0.001F ? -7484 : -30147;
   }

   @Override
   public Component resourceBarLabel() {
      return Component.translatable("awakened.graviton.weight_label");
   }

   @Override
   public float resourceBarFraction(ServerPlayer player, PowerData data) {
      return MassControl.fraction(MassControl.tier(data));
   }

   @Override
   public int resourceBarTier(ServerPlayer player, PowerData data) {
      return MassControl.tier(data);
   }

   @Override
   public String resourceBarTierLabel(int tier) {
      return Component.translatable(MassControl.nameKey(tier)).getString();
   }

   @Override
   public String resourceBarTierRoom() {
      String widest = "";

      for (int tier = -3; tier <= 3; tier++) {
         String label = this.resourceBarTierLabel(tier);
         if (label.length() > widest.length()) {
            widest = label;
         }
      }

      return widest;
   }

   @Override
   public float[] resourceBarTicks() {
      return WEIGHT_TICKS;
   }

   @Override
   public float resourceBarOrigin() {
      return MassControl.fraction(0);
   }

   @Override
   public boolean resourceBarHoldResets() {
      return true;
   }

   @Override
   public float resourceBarLockedFrom(@Nullable Player viewer, int masteryLevel) {
      return MassControl.fraction(MassControl.reachAt(masteryLevel));
   }

   @Override
   public float resourceBarLockedBelow(@Nullable Player viewer, int masteryLevel) {
      return MassControl.fraction(-MassControl.reachAt(masteryLevel));
   }

   @Override
   public int resourceBarTierColor(Player player) {
      return -1;
   }

   @Override
   public int resourceBarTierColor(Player player, int tier) {
      return tier < 0 ? -7484 : (tier > 0 ? -30147 : -1);
   }

   @Override
   public boolean progressOnCard() {
      return true;
   }

   @Override
   public PowerProgress progress(Player player, int masteryLevel) {
      List<PowerProgress.Stat> stats = List.of(
         stat("awakened.graviton.progress.light3_knockback", Component.literal("x" + trim(2.0))),
         stat("awakened.graviton.progress.heavy3_speed", Component.literal("x" + trim(MassControl.speedMultiplier(3)))),
         stat("awakened.graviton.progress.light3_fall", Component.translatable("awakened.graviton.progress.none")),
         stat("awakened.graviton.progress.heavy3_landing", Component.translatable("awakened.graviton.progress.blocks", new Object[]{trim(5.0)})),
         stat("awakened.graviton.progress.tier2", unlock(2, masteryLevel)),
         stat("awakened.graviton.progress.tier3", unlock(3, masteryLevel))
      );
      return new PowerProgress(-1.0F, Component.empty(), Component.empty(), stats);
   }

   private static PowerProgress.Stat stat(String labelKey, Component value) {
      return new PowerProgress.Stat(Component.translatable(labelKey), value);
   }

   private static Component unlock(int step, int masteryLevel) {
      int needs = MassControl.unlockLevel(step);
      return masteryLevel >= needs
         ? Component.translatable("awakened.graviton.progress.open")
         : Component.translatable("awakened.graviton.progress.at_level", new Object[]{needs});
   }

   private static String trim(double v) {
      String s = String.format(Locale.ROOT, "%.2f", v);
      s = s.replaceAll("0+$", "");
      return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
   }
}
