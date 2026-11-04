package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.voidview.VoidIntroMode;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class ClientConfig {
   private static final Builder BUILDER = new Builder();
   public static final BooleanValue QUICK_KEY_MODE = BUILDER.comment("Slot keys activate the ability as well as select it.")
      .define("input.quickKeyMode", false);
   public static final BooleanValue ABILITY_WHEEL = BUILDER.comment("Hold Activate to open the ability wheel and release to fire the chosen slot.")
      .define("input.abilityWheel", false);
   public static final BooleanValue SCREEN_SHAKE = BUILDER.comment("Camera shake from boss impacts and big ability hits.").define("fx.screenShake", true);
   public static final DoubleValue SCREEN_SHAKE_INTENSITY = BUILDER.comment("Camera shake strength. 0 = off, 1 = default, 2 = double.")
      .defineInRange("fx.screenShakeIntensity", 1.0, 0.0, 2.0);
   public static final EnumValue<VoidIntroMode> VOID_INTRO = BUILDER.comment(
         "How the Infinite Void intro plays when one catches you: FULL, SHORT or NO_CAMERA (your own view)."
      )
      .defineEnum("fx.voidIntro", VoidIntroMode.FULL);
   public static final BooleanValue CATALYST_SPINNER = BUILDER.comment("Play the spinner animation on a Catalyst roll. Off = instant reveal.")
      .define("fx.catalystSpinner", true);
   public static final BooleanValue BOSS_NOTE_MUSIC = BUILDER.comment("Play note-block boss themes. Off = vanilla music.").define("fx.bossNoteMusic", true);
   public static final BooleanValue BRAWLER_COMBO_SOUNDS = BUILDER.comment("Play the Brawler combo sounds only you hear.")
      .define("fx.brawlerComboSounds", true);
   public static final BooleanValue SPIDER_SENSE = BUILDER.comment("Show the Spider-Sense screen ripple and sound when a threat targets you.")
      .define("fx.spiderSense", true);
   public static final BooleanValue REAPER_AURA_SELF_FIRST = BUILDER.comment("Show your own Reaper aura in first person. Affects only your view.")
      .define("fx.reaperAuraSelfFirstPerson", true);
   public static final BooleanValue REAPER_AURA_SELF_THIRD = BUILDER.comment("Show your own Reaper aura in third person. Affects only your view.")
      .define("fx.reaperAuraSelfThirdPerson", true);
   public static final BooleanValue HUD_ABILITY_NUMBER_ONLY = BUILDER.comment("Show only the slot number above the ability tray, not the ability name.")
      .define("hud.abilityNumberOnly", false);
   public static final BooleanValue HUD_HIDE_STAMINA_WHEN_FULL = BUILDER.comment("Hide the stamina bar while it is full.")
      .define("hud.hideStaminaWhenFull", false);
   public static final BooleanValue HUD_ICONS_OFF = BUILDER.comment("Hide the ability tray and show only the current ability's icon beside the hotbar.")
      .define("hud.iconsOff", false);
   public static final BooleanValue HUD_MOVE_TOGETHER = BUILDER.comment("In the HUD layout editor, moving one part of a multi-part element moves the rest too.")
      .define("hud.moveTogether", true);
   public static final HudSettings HUD_STAMINA = HudSettings.define(BUILDER, "powerHud", "the stamina bar (with a power's resource bar under it)", true);
   public static final HudSettings HUD_ABILITY_BAR = HudSettings.define(BUILDER, "abilityHud", "the ability tray and the ability's name above it", true);
   public static final HudSettings HUD_ABILITY_ICON = HudSettings.define(
      BUILDER, "abilityIcon", "the selected ability's icon beside the hotbar (shown while the ability tray is off)", true
   );
   public static final HudSettings HUD_RESOURCE_GAUGE = HudSettings.define(BUILDER, "resourceGauge", "the tall power gauge on the right edge", true);
   public static final HudSettings HUD_QUEST_TRACKER = HudSettings.define(BUILDER, "questTracker", "the pinned-quest tracker", true, true);
   public static final HudSettings HUD_GAMMA_RAGE = HudSettings.define(BUILDER, "gammaRage", "the Gamma rage meter", true);
   public static final HudSettings HUD_BRAWLER_COMBO = HudSettings.define(BUILDER, "brawlerCombo", "the Brawler combo counter", true);
   public static final HudSettings HUD_SHADOW_DARKNESS = HudSettings.define(BUILDER, "shadowDarkness", "the Shadow darkness gauge", true);
   public static final HudSettings HUD_SOUL_CHIP = HudSettings.define(BUILDER, "soulChip", "the Summoning active-soul chip", true);
   public static final HudSettings HUD_SOUL_ROSTER = HudSettings.define(BUILDER, "soulRoster", "the Summoning army roster", true);
   public static final HudSettings HUD_SPIDER_WEB = HudSettings.define(BUILDER, "spiderWeb", "the Spider loaded-web chip", true);
   public static final HudSettings HUD_SENTINEL_ARSENAL = HudSettings.define(BUILDER, "sentinelArsenal", "the Sentinel's absorbed-power arsenal readout", true);
   public static final HudSettings HUD_POSSESS_JUMP = HudSettings.define(BUILDER, "possessJump", "the jump-charge bar shown while possessing a horse", true);
   public static final HudSettings HUD_STONE_MECH_CONTROLS = HudSettings.define(
      BUILDER, "stoneMechControls", "the Stone Mech controls box shown while piloting", true
   );
   public static final BooleanValue EXO_STATUS_CLUSTER = BUILDER.comment("Show the Exosavant visor status cluster.").define("exosavant.hudStatusCluster", true);
   public static final BooleanValue EXO_TARGET_DOSSIER = BUILDER.comment("Show the Exosavant visor target panel while a foe is locked.")
      .define("exosavant.hudTargetDossier", true);
   public static final BooleanValue EXO_FLIGHT_INSTRUMENTS = BUILDER.comment(
         "Show the Exosavant visor flight instruments, horizon and speed streaks while flying."
      )
      .define("exosavant.hudFlightInstruments", true);
   public static final EnumValue<ExoCorner> EXO_STATUS_CLUSTER_CORNER = BUILDER.comment("Screen corner for the Exosavant status cluster.")
      .defineEnum("exosavant.hudStatusClusterCorner", ExoCorner.TOP_LEFT);
   public static final EnumValue<ExoCorner> EXO_TARGET_DOSSIER_CORNER = BUILDER.comment("Screen corner for the Exosavant target panel.")
      .defineEnum("exosavant.hudTargetDossierCorner", ExoCorner.BOTTOM_RIGHT);
   public static final EnumValue<ExoCorner> EXO_FLIGHT_INSTRUMENTS_CORNER = BUILDER.comment("Screen corner for the Exosavant flight instruments.")
      .defineEnum("exosavant.hudFlightInstrumentsCorner", ExoCorner.TOP_RIGHT);
   public static final BooleanValue AWAKEN_CONTROLS_CHAT = BUILDER.comment(
         "Show the controls reminder in chat when you join a world. Also set by /awakened controlshint."
      )
      .define("hud.awakenControlsChat", true);
   public static final IntValue EXO_FLIGHT_HINTS_SHOWN = BUILDER.comment("Internal: times the Exosavant flight tip has been shown. Set to 0 to see it again.")
      .defineInRange("exosavant.flightHintsShown", 0, 0, 99);
   public static final BooleanValue AERO_FLIGHT_HINT_SHOWN = BUILDER.comment(
         "Internal: whether the Aerokinetic flight tip has been shown. Set to false to see it again."
      )
      .define("aero.flightHintShown", false);
   public static final IntValue VELOCITY_GEAR_HINTS_SHOWN = BUILDER.comment("Internal: times the Velocity gear tip has been shown. Set to 0 to see it again.")
      .defineInRange("velocity.gearHintsShown", 0, 0, 99);
   public static final BooleanValue PYRO_OVERDRIVE_HINT_SHOWN = BUILDER.comment(
         "Internal: whether the Pyrokinetic overdrive tip has been shown. Set to false to see it again."
      )
      .define("pyrokinetic.overdriveHintShown", false);
   public static final BooleanValue PYRO_BLUE_HINT_SHOWN = BUILDER.comment(
         "Internal: whether the Pyrokinetic blue fire tip has been shown. Set to false to see it again."
      )
      .define("pyrokinetic.blueHintShown", false);
   public static final BooleanValue COMMAND_RULES_HINT_SHOWN = BUILDER.comment(
         "Internal: whether the Command first-rule tip has been shown. Set to false to see it again."
      )
      .define("command.rulesHintShown", false);
   public static final BooleanValue NOMINATION_REMINDER = BUILDER.comment("Show the Mod of the Year nomination reminder (ends November 9, 2026).")
      .define("contest.nominationReminder", true);
   public static final ModConfigSpec SPEC = BUILDER.build();

   public static void save() {
      if (SPEC.isLoaded()) {
         SPEC.save();
      }
   }

   private ClientConfig() {
   }
}
