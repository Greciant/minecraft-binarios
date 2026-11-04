package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.content.AwakenedEffects;
import com.awakenedmod.awakened.content.item.PrismItem;
import com.awakenedmod.awakened.core.FluidFooting;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.CraftRecipe;
import com.awakenedmod.awakened.core.power.PassiveToggle;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerProgress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class VelocityPower extends Power {
   public static final String ID = "velocity";
   private static final int MOMENTUM_SYNC_INTERVAL = 4;
   private static final Set<UUID> MOMENTUM_DIRTY = ConcurrentHashMap.newKeySet();
   private static final String PREV_LEVEL_KEY = "velocity_prev_level";
   private static final float VANILLA_SPRINT_EXHAUSTION = 0.1F;
   private static final float SPRINT_EXHAUSTION_EXTRA = 1.5F;
   private static final int WISP_INTERVAL = 2;
   private static final int WISP_MIN = 1;
   private static final int WISP_MAX = 2;
   private static final double WISP_HEIGHT = 0.1;
   private static final double WISP_SPREAD = 0.25;
   private static final double WISP_SPEED = 0.01;
   private static final String VAR_PREFIX = "velocity_";
   private static final float GEAR_DIGIT_SCALE = 2.0F;
   private static final int GEAR_DIGIT_COLOR = -1;
   private static final float[] GEAR_TICKS = gearTicks();
   private static final String SECONDARY_KEY = "key.awakened.secondary_ability";

   public VelocityPower() {
      super("velocity", List.of(new OverdriveAbility(), new PhasingAbility(), new StaticBoltAbility(), new TimelessAbility(), new SpeedTunnelAbility()));
      FluidFooting.register((body, fluid) -> {
         if (body instanceof Player runner && OverdriveControl.canSurfaceRun(runner, fluid)) {
            return true;
         }

         return false;
      });
   }

   public static boolean owns(ServerPlayer player) {
      return player != null && "velocity".equals(PowerService.data(player).powerId());
   }

   @Override
   public int themeColor() {
      return -1560004;
   }

   @Override
   public int signatureSlot() {
      return 0;
   }

   @Override
   public List<PassiveToggle> passiveToggles() {
      return List.of(OverdriveAbility.TOGGLE, Gearbox.AUTO, VelocityVisualState.TRAIL, EchoCadence.ECHOES);
   }

   @Override
   public List<CraftRecipe> recipes() {
      List<ItemStack> grid = new ArrayList<>(9);

      for (int i = 0; i < 9; i++) {
         grid.add(new ItemStack(i == 4 ? Items.RED_DYE : Items.QUARTZ));
      }

      return List.of(new CraftRecipe(PrismItem.of(DyeColor.RED), 3, 3, grid, ItemStack.EMPTY, 0));
   }

   @Override
   public void passiveTick(ServerPlayer player, PowerData data) {
      super.passiveTick(player, data);
      gearLock(player, data);
      boolean gaining = Momentum.runTick(player, data);
      boolean changed = Momentum.tick(player, data, gaining);
      Gearbox.autoTick(player, data, gaining);
      Gearbox.forcingTick(player, data);
      Gearbox.topGearTick(player, data);
      Gearbox.boomTick(player, data);
      Gearbox.redlineTick(player, data);
      OverdriveControl.tick(player, data);
      metabolism(player, gaining);
      VelocityVisualState.sync(player);
      EchoCadence.tick(player, data);
      if (changed) {
         MOMENTUM_DIRTY.add(player.getUUID());
      }

      if (player.tickCount % 4 == 0 && MOMENTUM_DIRTY.remove(player.getUUID())) {
         PowerService.sync(player);
      }

      gearZeroWisps(player, data, gaining);
   }

   private static void gearLock(ServerPlayer player, PowerData data) {
      if (Momentum.reconcileLock(data)) {
         VelocityVisualState.sync(player);
         PowerService.sync(player);
      }

      int level = data.masteryLevel();
      int previous = Math.round(data.var("velocity_prev_level", -1.0F));
      if (previous != level) {
         data.setVar("velocity_prev_level", (float)level);
         if (previous >= 0) {
            for (int gear = 1; gear <= 5; gear++) {
               int needs = Momentum.unlockLevel(gear);
               if (needs > previous && needs <= level) {
                  PowerService.announceUnlock(
                     player, Component.translatable("awakened.velocity.gear_unlocked", new Object[]{gear}), PowerService.Unlock.FEATURE
                  );
               }
            }
         }
      }
   }

   public static void momentumSpent(ServerPlayer player) {
      if (player != null) {
         MOMENTUM_DIRTY.add(player.getUUID());
      }
   }

   static void forgetMomentumSync(ServerPlayer player) {
      if (player != null) {
         MOMENTUM_DIRTY.remove(player.getUUID());
      }
   }

   private static void metabolism(ServerPlayer player, boolean running) {
      if (running && !player.hasEffect(AwakenedEffects.ENERGIZED)) {
         double blocks = (double)Momentum.travelThisTick(player);
         if (blocks > 0.0) {
            player.causeFoodExhaustion((float)blocks * 0.1F * 0.5F);
         }
      }
   }

   @Override
   public float modifyBreakSpeed(Player player, float speed) {
      VelocityVisual visual = VelocityVisualState.of(player);
      return visual.overdrive() && visual.gear() > 0 ? speed * (float)(1.0 + (double)visual.gear() * 0.2) : speed;
   }

   private static void gearZeroWisps(ServerPlayer player, PowerData data, boolean running) {
      if (running && Gearbox.effectiveGear(player, data) == 0 && player.tickCount % 2 == 0) {
         int count = 1 + player.getRandom().nextInt(2);
         Fx.burst(player.serverLevel(), ParticleTypes.CLOUD, player.position().add(0.0, 0.1, 0.0), count, 0.25, 0.01);
      }
   }

   @Override
   public void onReplaced(ServerPlayer player) {
      OverdriveControl.clear(player);
      Gearbox.clearDemands(player);
      VelocityVisualState.clear(player);
      EchoCadence.forget(player);
      MOMENTUM_DIRTY.remove(player.getUUID());
      PowerData data = PowerService.data(player);
      data.removeVars("velocity_");
      data.removeVar(Gearbox.AUTO.var());
      data.removeVar(OverdriveAbility.TOGGLE.var());
   }

   @Override
   public boolean hasResourceBar() {
      return true;
   }

   @Override
   public int resourceBarColor() {
      return -1560004;
   }

   @Override
   public int resourceBarColor(Player viewer) {
      return VelocityPalette.of(viewer).bodyArgb();
   }

   @Override
   public Component resourceBarLabel() {
      return Component.translatable("awakened.velocity.momentum");
   }

   @Override
   public float resourceBarFraction(ServerPlayer player, PowerData data) {
      return Momentum.fraction(data);
   }

   @Override
   public int resourceBarTier(ServerPlayer player, PowerData data) {
      return Gearbox.effectiveGear(player, data);
   }

   @Override
   public float resourceBarMark(ServerPlayer player, PowerData data) {
      return 0.0F;
   }

   @Override
   public String resourceBarTierLabel(int tier) {
      return Integer.toString(Math.max(0, tier));
   }

   @Override
   public String resourceBarTierRoom() {
      return "0";
   }

   @Override
   public float resourceBarTierScale() {
      return 2.0F;
   }

   @Override
   public int resourceBarTierColor(Player player) {
      if (!VelocityVisualState.of(player).forcing()) {
         return -1;
      } else {
         VelocityPalette palette = VelocityPalette.of(player);
         return 0xFF000000 | (hudBlinkOn() ? palette.hot() : palette.ember());
      }
   }

   @Override
   public float[] resourceBarTicks() {
      return GEAR_TICKS;
   }

   @Override
   public float resourceBarLockedFrom(Player viewer) {
      int unlocked = VelocityVisualState.of(viewer).unlocked();
      return unlocked >= 5 ? 1.0F : Momentum.thresholdFraction(unlocked);
   }

   @Override
   public PowerProgress progress(Player player, int masteryLevel) {
      List<PowerProgress.Stat> cells = new ArrayList<>();
      int open = 0;
      int next = 0;

      for (int gear = 1; gear <= 5; gear++) {
         int needs = Momentum.unlockLevel(gear);
         boolean unlocked = masteryLevel >= needs;
         if (unlocked) {
            open++;
         } else if (next == 0) {
            next = gear;
         }

         cells.add(
            new PowerProgress.Stat(
               Component.translatable("awakened.velocity.gear", new Object[]{gear}),
               unlocked
                  ? Component.translatable("awakened.velocity.progress.gear_open")
                  : Component.translatable("awakened.velocity.progress.gear_locked", new Object[]{needs})
            )
         );
      }

      Component count = Component.literal(open + " / 5");
      if (next == 0) {
         return new PowerProgress(1.0F, Component.translatable("awakened.velocity.progress.all_open"), count, cells);
      } else {
         int needs = Momentum.unlockLevel(next);
         int from = Momentum.unlockLevel(next - 1);
         float fraction = needs <= from ? 1.0F : Math.max(0.0F, Math.min(1.0F, (float)(masteryLevel - from) / (float)(needs - from)));
         return new PowerProgress(
            fraction,
            Component.translatable("awakened.velocity.progress.next", new Object[]{next}),
            Component.translatable("awakened.velocity.progress.to_go", new Object[]{needs, needs - masteryLevel}),
            cells
         );
      }
   }

   @Override
   public boolean resourceBarFlashing(Player player) {
      return VelocityVisualState.of(player).forcing();
   }

   @Override
   public String staminaDrainLabel(Player viewer) {
      float perSecond = this.drainPerSecond(viewer);
      return perSecond <= 0.0F ? null : "-" + Math.round(perSecond) + "/s";
   }

   @Override
   public boolean staminaDrainArmed(Player viewer) {
      VelocityVisual visual = VelocityVisualState.of(viewer);
      return visual.billing() || this.holdDrain(viewer, visual) > 0.0F;
   }

   private float drainPerSecond(Player viewer) {
      VelocityVisual visual = VelocityVisualState.of(viewer);
      float perSecond = (float)Math.max(0, visual.gear() - visual.earnedGear()) * 5.0F;
      if (visual.overdrive()) {
         perSecond += Gearbox.topGearCost(visual.chosen());
      }

      return perSecond + this.holdDrain(viewer, visual);
   }

   private float holdDrain(Player viewer, VelocityVisual visual) {
      float perSecond = 0.0F;

      for (Ability ability : this.abilities()) {
         if (ability instanceof VelocityHoldAbility hold) {
            perSecond += hold.viewerDrain(viewer, visual);
         }
      }

      return perSecond;
   }

   @Override
   public void onRespawn(ServerPlayer player, PowerData data) {
      Momentum.reset(data);
      Gearbox.pinAfterSpawn(player);
   }

   @Override
   public void onLogin(ServerPlayer player, PowerData data) {
      Gearbox.pinAfterSpawn(player);
      VelocityColor.rollIfUnset(player, data);
   }

   private static float[] gearTicks() {
      float[] ticks = new float[4];

      for (int gear = 1; gear < 5; gear++) {
         ticks[gear - 1] = Momentum.thresholdFraction(gear);
      }

      return ticks;
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      VelocityPalette palette = VelocityPalette.of(VelocityColor.roll(player));
      Fx.burst(level, VelocityFx.dust(palette), player.position().add(0.0, 1.0, 0.0), 70, 0.9, 0.3);
      Fx.burst(level, ParticleTypes.ELECTRIC_SPARK, player.position().add(0.0, 1.0, 0.0), 30, 0.7, 0.2);
      Fx.sound(level, player.position(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.3F);
      player.displayClientMessage(
         Component.translatable("awakened.velocity.hint", new Object[]{Component.keybind("key.awakened.secondary_ability")}).withStyle(ChatFormatting.RED),
         false
      );
   }
}
