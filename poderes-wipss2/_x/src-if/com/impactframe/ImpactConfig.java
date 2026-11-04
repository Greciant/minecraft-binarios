package com.impactframe;

import com.impactframe.client.ImpactPreset;
import com.impactframe.client.ImpactState;
import com.impactframe.client.ImpactType;
import java.util.Arrays;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class ImpactConfig {
   private static final Builder BUILDER = new Builder();
   private static final BooleanValue ENABLED = BUILDER.translation("impactframe.configuration.enabled").define("enabled", true);
   private static final ConfigValue<String> PRESET = BUILDER.translation("impactframe.configuration.preset")
      .defineInList("preset", "normal", Arrays.asList("off", "minimal", "normal", "love", "ridiculous", "advanced"));
   private static final DoubleValue INTENSITY = BUILDER.translation("impactframe.configuration.intensity").defineInRange("intensity", 0.8, 0.0, 1.0);
   private static final IntValue COOLDOWN_TICKS = BUILDER.translation("impactframe.configuration.cooldownTicks").defineInRange("cooldownTicks", 6, 0, 100);
   private static final BooleanValue SAFE_MODE = BUILDER.translation("impactframe.configuration.safeMode").define("safeMode", false);
   private static final BooleanValue SOUND_ENABLED = BUILDER.translation("impactframe.configuration.soundEnabled").define("soundEnabled", true);
   private static final DoubleValue SOUND_VOLUME = BUILDER.translation("impactframe.configuration.soundVolume").defineInRange("soundVolume", 1.0, 0.0, 1.0);
   private static final BooleanValue SERVER_FEATURES = BUILDER.translation("impactframe.configuration.serverFeatures").define("enabled", false);
   private static final BooleanValue FREEZE_ON_IMPACT = BUILDER.translation("impactframe.configuration.freezeOnImpact").define("freezeOnImpact", true);
   private static final IntValue FREEZE_TICKS = BUILDER.translation("impactframe.configuration.freezeTicks").defineInRange("freezeTicks", 3, 1, 10);
   private static final BooleanValue LOCK_MOVEMENT = BUILDER.translation("impactframe.configuration.lockMovement").define("lockMovement", true);
   private static final BooleanValue ON_ATTACK = BUILDER.translation("impactframe.configuration.onAttack").define("onAttack", true);
   private static final BooleanValue ON_DAMAGE = BUILDER.translation("impactframe.configuration.onDamage").define("onDamage", true);
   private static final BooleanValue ON_BLOCK_BREAK = BUILDER.translation("impactframe.configuration.onBlockBreak").define("onBlockBreak", true);
   private static final BooleanValue ON_BLOCK_PLACE = BUILDER.translation("impactframe.configuration.onBlockPlace").define("onBlockPlace", true);
   private static final BooleanValue ON_FOOTSTEP = BUILDER.translation("impactframe.configuration.onFootstep").define("onFootstep", false);
   private static final BooleanValue ON_EXPLOSION = BUILDER.translation("impactframe.configuration.onExplosion").define("onExplosion", true);
   private static final BooleanValue ON_KILL = BUILDER.translation("impactframe.configuration.onKill").define("onKill", true);
   private static final BooleanValue ON_LAND = BUILDER.translation("impactframe.configuration.onLand").define("onLand", true);
   private static final BooleanValue ON_CRIT = BUILDER.translation("impactframe.configuration.onCrit").define("onCrit", true);
   private static final BooleanValue ON_MACE = BUILDER.translation("impactframe.configuration.onMace").define("onMace", true);
   private static final DoubleValue ATTACK_INTENSITY = BUILDER.translation("impactframe.configuration.attackIntensity")
      .defineInRange("attackIntensity", 0.6, 0.0, 1.0);
   private static final DoubleValue DAMAGE_INTENSITY = BUILDER.translation("impactframe.configuration.damageIntensity")
      .defineInRange("damageIntensity", 0.68, 0.0, 1.0);
   private static final DoubleValue BLOCK_BREAK_INTENSITY = BUILDER.translation("impactframe.configuration.blockBreakIntensity")
      .defineInRange("blockBreakIntensity", 0.52, 0.0, 1.0);
   private static final DoubleValue BLOCK_PLACE_INTENSITY = BUILDER.translation("impactframe.configuration.blockPlaceIntensity")
      .defineInRange("blockPlaceIntensity", 0.36, 0.0, 1.0);
   private static final DoubleValue FOOTSTEP_INTENSITY = BUILDER.translation("impactframe.configuration.footstepIntensity")
      .defineInRange("footstepIntensity", 0.36, 0.0, 1.0);
   private static final DoubleValue EXPLOSION_INTENSITY = BUILDER.translation("impactframe.configuration.explosionIntensity")
      .defineInRange("explosionIntensity", 0.76, 0.0, 1.0);
   private static final DoubleValue KILL_INTENSITY = BUILDER.translation("impactframe.configuration.killIntensity")
      .defineInRange("killIntensity", 0.8, 0.0, 1.0);
   private static final DoubleValue LAND_INTENSITY = BUILDER.translation("impactframe.configuration.landIntensity")
      .defineInRange("landIntensity", 0.6, 0.0, 1.0);
   private static final DoubleValue CRIT_INTENSITY = BUILDER.translation("impactframe.configuration.critIntensity")
      .defineInRange("critIntensity", 0.72, 0.0, 1.0);
   private static final DoubleValue MACE_INTENSITY = BUILDER.translation("impactframe.configuration.maceIntensity")
      .defineInRange("maceIntensity", 0.8, 0.0, 1.0);
   private static final DoubleValue MINIMUM_DAMAGE = BUILDER.translation("impactframe.configuration.minimumDamage")
      .defineInRange("minimumDamage", 2.0, 0.0, 10.0);
   private static final DoubleValue MINIMUM_FALL_DISTANCE = BUILDER.translation("impactframe.configuration.minimumFallDistance")
      .defineInRange("minimumFallDistance", 3.0, 0.0, 12.0);
   private static final DoubleValue MINIMUM_EXPLOSION_STRENGTH = BUILDER.translation("impactframe.configuration.minimumExplosionStrength")
      .defineInRange("minimumExplosionStrength", 0.6, 0.35, 1.0);
   private static final DoubleValue MINIMUM_BLOCK_HARDNESS = BUILDER.translation("impactframe.configuration.minimumBlockHardness")
      .defineInRange("minimumBlockHardness", 1.5, 0.0, 20.0);
   private static final BooleanValue DIRECTIONAL_STREAKS = BUILDER.translation("impactframe.configuration.directionalStreaks")
      .define("directionalStreaks", true);
   private static final BooleanValue ZOOM_ENABLED = BUILDER.translation("impactframe.configuration.zoomEnabled").define("zoomEnabled", true);
   private static final DoubleValue ZOOM_STRENGTH = BUILDER.translation("impactframe.configuration.zoomStrength").defineInRange("zoomStrength", 1.5, 0.0, 3.0);
   private static final DoubleValue HALFTONE = BUILDER.translation("impactframe.configuration.halftone").defineInRange("halftone", 0.25, 0.0, 1.0);
   private static final BooleanValue ONOMATOPOEIA = BUILDER.translation("impactframe.configuration.onomatopoeia").define("onomatopoeia", true);
   private static final DoubleValue ONOMATOPOEIA_SCALE = BUILDER.translation("impactframe.configuration.onomatopoeiaScale")
      .defineInRange("onomatopoeiaScale", 1.0, 0.5, 2.0);
   private static final BooleanValue COLOR_KEYED = BUILDER.translation("impactframe.configuration.colorKeyedFrames").define("colorKeyedFrames", false);
   private static final BooleanValue LETTERBOX = BUILDER.translation("impactframe.configuration.letterbox").define("letterbox", true);
   private static final DoubleValue LETTERBOX_SIZE = BUILDER.translation("impactframe.configuration.letterboxSize")
      .defineInRange("letterboxSize", 0.12, 0.02, 0.25);
   private static final IntValue FLASH_BUDGET = BUILDER.translation("impactframe.configuration.flashBudget").defineInRange("flashBudget", 30, 0, 60);
   private static final BooleanValue DEPTH_OUTLINES = BUILDER.translation("impactframe.configuration.depthOutlines").define("depthOutlines", true);
   private static final BooleanValue LITE_WITH_SHADER_PACKS = BUILDER.translation("impactframe.configuration.liteWithShaderPacks")
      .define("liteWithShaderPacks", true);
   public static final ModConfigSpec SPEC = BUILDER.build();

   private ImpactConfig() {
   }

   public static boolean enabled() {
      return (Boolean)ENABLED.get() && preset() != ImpactPreset.OFF;
   }

   public static boolean enabledSetting() {
      return (Boolean)ENABLED.get();
   }

   public static void setEnabled(boolean value) {
      ENABLED.set(value);
      if (!value) {
         ImpactState.reset();
      }
   }

   public static ImpactPreset preset() {
      return ImpactPreset.fromId((String)PRESET.get());
   }

   public static void setPreset(ImpactPreset preset) {
      PRESET.set(preset.id);
      SPEC.save();
      if (preset == ImpactPreset.OFF) {
         ImpactState.reset();
      }
   }

   public static double intensitySetting() {
      return (Double)INTENSITY.get();
   }

   public static void setIntensity(double value) {
      INTENSITY.set(Math.max(0.0, Math.min(1.0, value)));
   }

   public static double intensity() {
      return switch (preset()) {
         case MINIMAL -> Math.min((Double)INTENSITY.get(), 0.4);
         case I_LOVE_IMPACT_FRAMES, RIDICULOUS -> 1.0;
         default -> INTENSITY.get();
      };
   }

   public static int cooldownTicks() {
      return switch (preset()) {
         case MINIMAL -> Math.max((Integer)COOLDOWN_TICKS.get(), 12);
         case I_LOVE_IMPACT_FRAMES -> Math.min((Integer)COOLDOWN_TICKS.get(), 1);
         case RIDICULOUS -> 0;
         default -> COOLDOWN_TICKS.get();
      };
   }

   public static int cooldownTicksSetting() {
      return (Integer)COOLDOWN_TICKS.get();
   }

   public static void setCooldownTicks(int value) {
      COOLDOWN_TICKS.set(Math.max(0, Math.min(100, value)));
   }

   public static boolean safeMode() {
      return (Boolean)SAFE_MODE.get();
   }

   public static void setSafeMode(boolean value) {
      SAFE_MODE.set(value);
   }

   public static boolean soundEnabled() {
      return (Boolean)SOUND_ENABLED.get();
   }

   public static void setSoundEnabled(boolean value) {
      SOUND_ENABLED.set(value);
   }

   public static double soundVolume() {
      return (Double)SOUND_VOLUME.get();
   }

   public static void setSoundVolume(double value) {
      SOUND_VOLUME.set(Math.max(0.0, Math.min(1.0, value)));
   }

   public static boolean serverFeatures() {
      return (Boolean)SERVER_FEATURES.get();
   }

   public static void setServerFeatures(boolean value) {
      SERVER_FEATURES.set(value);
   }

   public static boolean freezeOnImpact() {
      return (Boolean)FREEZE_ON_IMPACT.get();
   }

   public static void setFreezeOnImpact(boolean value) {
      FREEZE_ON_IMPACT.set(value);
   }

   public static boolean lockMovement() {
      return (Boolean)LOCK_MOVEMENT.get();
   }

   public static void setLockMovement(boolean value) {
      LOCK_MOVEMENT.set(value);
   }

   public static int freezeTicks() {
      return (Integer)FREEZE_TICKS.get();
   }

   public static void setFreezeTicks(int value) {
      FREEZE_TICKS.set(Math.max(1, Math.min(10, value)));
   }

   public static boolean triggerEnabled(String name) {
      return switch (preset()) {
         case MINIMAL -> name.equals("attack") || name.equals("damage");
         case I_LOVE_IMPACT_FRAMES -> isGameplayTrigger(name);
         case RIDICULOUS -> isGameplayTrigger(name) || name.equals("footstep");
         case OFF -> false;
         case NORMAL -> isGameplayTrigger(name) && !name.equals("blockBreak") && !name.equals("blockPlace");
         case ADVANCED -> triggerConfigured(name);
      };
   }

   public static boolean triggerConfigured(ImpactType type) {
      return triggerConfigured(type.key);
   }

   private static boolean triggerConfigured(String name) {
      return switch (name) {
         case "attack" -> ON_ATTACK.get();
         case "damage" -> ON_DAMAGE.get();
         case "blockBreak" -> ON_BLOCK_BREAK.get();
         case "blockPlace" -> ON_BLOCK_PLACE.get();
         case "footstep" -> ON_FOOTSTEP.get();
         case "explosion" -> ON_EXPLOSION.get();
         case "kill" -> ON_KILL.get();
         case "land" -> ON_LAND.get();
         case "crit" -> ON_CRIT.get();
         case "mace" -> ON_MACE.get();
         default -> false;
      };
   }

   public static double triggerIntensity(ImpactType type) {
      return (Double)triggerIntensityValue(type).get();
   }

   public static void setTriggerIntensity(ImpactType type, double value) {
      triggerIntensityValue(type).set(Math.max(0.0, Math.min(1.0, value)));
   }

   public static boolean useMinimumImpactThresholds() {
      return switch (preset()) {
         case MINIMAL, NORMAL, ADVANCED -> true;
         default -> false;
      };
   }

   public static double minimumDamage() {
      return (Double)MINIMUM_DAMAGE.get();
   }

   public static void setMinimumDamage(double value) {
      MINIMUM_DAMAGE.set(Math.max(0.0, Math.min(10.0, value)));
   }

   public static double minimumFallDistance() {
      return (Double)MINIMUM_FALL_DISTANCE.get();
   }

   public static void setMinimumFallDistance(double value) {
      MINIMUM_FALL_DISTANCE.set(Math.max(0.0, Math.min(12.0, value)));
   }

   public static double minimumExplosionStrength() {
      return (Double)MINIMUM_EXPLOSION_STRENGTH.get();
   }

   public static void setMinimumExplosionStrength(double value) {
      MINIMUM_EXPLOSION_STRENGTH.set(Math.max(0.35, Math.min(1.0, value)));
   }

   public static double minimumBlockHardness() {
      return (Double)MINIMUM_BLOCK_HARDNESS.get();
   }

   public static void setMinimumBlockHardness(double value) {
      MINIMUM_BLOCK_HARDNESS.set(Math.max(0.0, Math.min(20.0, value)));
   }

   private static DoubleValue triggerIntensityValue(ImpactType type) {
      return switch (type) {
         case ATTACK -> ATTACK_INTENSITY;
         case DAMAGE -> DAMAGE_INTENSITY;
         case BLOCK_BREAK -> BLOCK_BREAK_INTENSITY;
         case BLOCK_PLACE -> BLOCK_PLACE_INTENSITY;
         case FOOTSTEP -> FOOTSTEP_INTENSITY;
         case EXPLOSION -> EXPLOSION_INTENSITY;
         case KILL -> KILL_INTENSITY;
         case LAND -> LAND_INTENSITY;
         case CRIT -> CRIT_INTENSITY;
         case MACE -> MACE_INTENSITY;
      };
   }

   public static void setTriggerEnabled(ImpactType type, boolean enabled) {
      switch (type) {
         case ATTACK:
            ON_ATTACK.set(enabled);
            break;
         case DAMAGE:
            ON_DAMAGE.set(enabled);
            break;
         case BLOCK_BREAK:
            ON_BLOCK_BREAK.set(enabled);
            break;
         case BLOCK_PLACE:
            ON_BLOCK_PLACE.set(enabled);
            break;
         case FOOTSTEP:
            ON_FOOTSTEP.set(enabled);
            break;
         case EXPLOSION:
            ON_EXPLOSION.set(enabled);
            break;
         case KILL:
            ON_KILL.set(enabled);
            break;
         case LAND:
            ON_LAND.set(enabled);
            break;
         case CRIT:
            ON_CRIT.set(enabled);
            break;
         case MACE:
            ON_MACE.set(enabled);
      }
   }

   public static boolean directionalStreaks() {
      return (Boolean)DIRECTIONAL_STREAKS.get();
   }

   public static void setDirectionalStreaks(boolean value) {
      DIRECTIONAL_STREAKS.set(value);
   }

   public static boolean zoomEnabled() {
      return (Boolean)ZOOM_ENABLED.get();
   }

   public static void setZoomEnabled(boolean value) {
      ZOOM_ENABLED.set(value);
   }

   public static double zoomStrength() {
      return (Double)ZOOM_STRENGTH.get();
   }

   public static void setZoomStrength(double value) {
      ZOOM_STRENGTH.set(Math.max(0.0, Math.min(3.0, value)));
   }

   public static float halftone() {
      return ((Double)HALFTONE.get()).floatValue();
   }

   public static void setHalftone(double value) {
      HALFTONE.set(Math.max(0.0, Math.min(1.0, value)));
   }

   public static boolean onomatopoeia() {
      return (Boolean)ONOMATOPOEIA.get();
   }

   public static void setOnomatopoeia(boolean value) {
      ONOMATOPOEIA.set(value);
   }

   public static float onomatopoeiaScale() {
      return ((Double)ONOMATOPOEIA_SCALE.get()).floatValue();
   }

   public static void setOnomatopoeiaScale(double value) {
      ONOMATOPOEIA_SCALE.set(Math.max(0.5, Math.min(2.0, value)));
   }

   public static boolean colorKeyed() {
      return (Boolean)COLOR_KEYED.get();
   }

   public static void setColorKeyed(boolean value) {
      COLOR_KEYED.set(value);
   }

   public static boolean letterbox() {
      return (Boolean)LETTERBOX.get();
   }

   public static void setLetterbox(boolean value) {
      LETTERBOX.set(value);
   }

   public static float letterboxSize() {
      return ((Double)LETTERBOX_SIZE.get()).floatValue();
   }

   public static void setLetterboxSize(double value) {
      LETTERBOX_SIZE.set(Math.max(0.02, Math.min(0.25, value)));
   }

   public static int flashBudget() {
      return (Integer)FLASH_BUDGET.get();
   }

   public static void setFlashBudget(int value) {
      FLASH_BUDGET.set(Math.max(0, Math.min(60, value)));
   }

   public static boolean depthOutlines() {
      return (Boolean)DEPTH_OUTLINES.get();
   }

   public static void setDepthOutlines(boolean value) {
      DEPTH_OUTLINES.set(value);
   }

   public static boolean liteWithShaderPacks() {
      return (Boolean)LITE_WITH_SHADER_PACKS.get();
   }

   public static void setLiteWithShaderPacks(boolean value) {
      LITE_WITH_SHADER_PACKS.set(value);
   }

   public static boolean useDepth() {
      if (!(Boolean)DEPTH_OUTLINES.get()) {
         return false;
      } else if (!(Boolean)LITE_WITH_SHADER_PACKS.get()) {
         return true;
      } else {
         ModList mods = ModList.get();
         return !mods.isLoaded("iris") && !mods.isLoaded("oculus");
      }
   }

   public static void save() {
      SPEC.save();
   }

   private static boolean isGameplayTrigger(String name) {
      return switch (name) {
         case "attack", "damage", "blockBreak", "blockPlace", "explosion", "kill", "land", "crit", "mace" -> true;
         default -> false;
      };
   }

   static {
      BUILDER.push("general");
      BUILDER.pop();
      BUILDER.push("serverFeatures");
      BUILDER.pop();
      BUILDER.push("triggers");
      BUILDER.pop();
      BUILDER.push("effects");
      BUILDER.pop();
      BUILDER.push("compatibility");
      BUILDER.pop();
   }
}
