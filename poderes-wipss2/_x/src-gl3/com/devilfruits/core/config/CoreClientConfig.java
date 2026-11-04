package com.devilfruits.core.config;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class CoreClientConfig {
   public static final ModConfigSpec SPEC;
   public static final EnumValue<CoreClientConfig.VfxQuality> VFX_QUALITY;
   public static final DoubleValue CAMERA_SHAKE;
   public static final BooleanValue CAMERA_ROLL;
   public static final BooleanValue FOV_PUNCH;
   public static final BooleanValue SCREEN_FLASH;
   public static final BooleanValue LAYERED_SOUNDS;
   public static final BooleanValue FIRST_PERSON_ANIMATIONS;
   public static final DoubleValue STRUCTURE_SHAKE;
   public static final IntValue STRUCTURE_DECALS;
   public static final BooleanValue STRUCTURE_EDGE_DUST;
   public static final BooleanValue SHOW_ABILITY_BAR;
   public static final BooleanValue HIDE_HUD;
   public static final BooleanValue FRUIT_LOCKED;
   public static final EnumValue<CoreClientConfig.HudAnchor> HUD_ANCHOR;
   public static final IntValue HUD_OFFSET_X;
   public static final IntValue HUD_OFFSET_Y;
   public static final BooleanValue SHOW_ENERGY_TEXT;
   public static final EnumValue<CoreClientConfig.KeyMode> LOGIA_ELEMENTAL_KEY;
   public static final EnumValue<CoreClientConfig.VfxQuality> LOGIA_BODY_QUALITY;
   public static final BooleanValue LOGIA_SHEDDING;
   public static final EnumValue<CoreClientConfig.CinematicQuality> CINEMATIC_QUALITY;
   public static final BooleanValue REDUCE_FLASHING;
   public static final DoubleValue BLOOM_STRENGTH;
   public static final IntValue MAX_PARTICLES;
   public static final IntValue MAX_RIBBON_POINTS;
   public static final IntValue MAX_MESHES;
   public static final BooleanValue ADAPTIVE_LOAD;
   public static final BooleanValue SHOW_ARMOR_ON_STRETCH;
   public static final IntValue MAX_LIMBS;
   public static final BooleanValue SHOULDER_ENABLED;
   public static final DoubleValue SHOULDER_DISTANCE;
   public static final DoubleValue SHOULDER_HORIZONTAL;
   public static final DoubleValue SHOULDER_VERTICAL;
   public static final EnumValue<CoreClientConfig.ShoulderSide> SHOULDER_SIDE;
   public static final IntValue SHOULDER_SMOOTHING;
   public static final BooleanValue SHOULDER_COLLISION;
   public static final BooleanValue SHOULDER_SCALE;
   public static final DoubleValue SHOULDER_FOV;
   public static final BooleanValue SHOULDER_AIM;
   public static final DoubleValue SHOULDER_HIDE_BELOW;
   public static final ConfigValue<List<? extends String>> SHOULDER_FORMS;

   private CoreClientConfig() {
   }

   static {
      Builder b = new Builder();
      b.comment("Visual effects, camera and sound.").push("effects");
      VFX_QUALITY = b.comment("Particle density and crack detail. Also scaled by the vanilla Particles setting.")
         .defineEnum("vfxQuality", CoreClientConfig.VfxQuality.HIGH);
      CAMERA_SHAKE = b.comment("Camera shake intensity multiplier (0 disables).").defineInRange("cameraShake", 1.0, 0.0, 2.0);
      CAMERA_ROLL = b.comment("Camera roll during huge area tremors (e.g. Shima Yurashi).").define("cameraRoll", true);
      FOV_PUNCH = b.comment("Short FOV kick on big impacts.").define("fovPunch", true);
      SCREEN_FLASH = b.comment("Subtle white flash at the screen edges on big impacts.").define("screenFlash", true);
      LAYERED_SOUNDS = b.comment("Stack pitched vanilla sound layers on top of each signature sound. Disable after installing custom .ogg sounds.")
         .define("layeredSounds", true);
      FIRST_PERSON_ANIMATIONS = b.comment("Animate the first-person arms during abilities.").define("firstPersonAnimations", true);
      STRUCTURE_SHAKE = b.comment("Physics structures (Sable): camera shake when the structure you stand on is struck (0 disables).")
         .defineInRange("structureShake", 1.0, 0.0, 2.0);
      STRUCTURE_DECALS = b.comment("Physics structures (Sable): most marks (cracks, scorches, lightning scars) kept on one structure.")
         .defineInRange("structureDecals", 24, 0, 256);
      STRUCTURE_EDGE_DUST = b.comment("Physics structures (Sable): dust drifting past the screen edges when your structure takes a heavy hit.")
         .define("structureEdgeDust", true);
      b.pop();
      b.comment("Heads-up display.").push("hud");
      SHOW_ABILITY_BAR = b.comment("Show the ability bar (toggle in game with V).").define("showAbilityBar", true);
      HIDE_HUD = b.comment("Hide the whole Devil Fruits interface: ability bar, energy, form, Haki, Logia and charge indicators (toggle in game with F7).")
         .define("hideHud", false);
      FRUIT_LOCKED = b.comment(
            "The fruit is switched off: techniques, transformations and the Logia keys do nothing, so nothing is cast by accident (toggle in game with O)."
         )
         .define("fruitLocked", false);
      HUD_ANCHOR = b.comment("Ability bar position. AUTO uses the space right of the hotbar and falls back to the right screen edge.")
         .defineEnum("hudAnchor", CoreClientConfig.HudAnchor.AUTO);
      HUD_OFFSET_X = b.comment("Horizontal offset of the ability bar in GUI pixels.").defineInRange("hudOffsetX", 0, -1000, 1000);
      HUD_OFFSET_Y = b.comment("Vertical offset of the ability bar in GUI pixels.").defineInRange("hudOffsetY", 0, -1000, 1000);
      SHOW_ENERGY_TEXT = b.comment("Show the energy value next to the energy bar.").define("showEnergyText", true);
      b.pop();
      b.comment("Logia bodies (every Logia fruit).").push("logia");
      LOGIA_ELEMENTAL_KEY = b.comment("Elemental body key: TOGGLE (press to switch) or HOLD (element while held).")
         .defineEnum("elementalKeyMode", CoreClientConfig.KeyMode.TOGGLE);
      LOGIA_BODY_QUALITY = b.comment(
            "Dissolving bodies: LOW (whole limbs, no hit holes), MEDIUM (4-step dither, holes) or HIGH (8-step dither, holes, element-coloured edges)."
         )
         .defineEnum("bodyQuality", CoreClientConfig.VfxQuality.HIGH);
      LOGIA_SHEDDING = b.comment(
            "Dissolved Logia bodies shed their element (crumbling limbs, the swirl and trail of an elemental body). A solid body never sheds."
         )
         .define("shedding", true);
      b.pop();
      b.comment("Cinematic layer of showcase techniques (bloom, distortion, hi-fi sprites, camera direction). See docs/VFX_AAA.md.").push("vfx");
      CINEMATIC_QUALITY = b.comment(
            "ULTRA: everything. HIGH: no screen distortion, lighter bloom. MEDIUM: additive sprites only, no post-processing. LOW: the simple vanilla-particle effects, no shaders. A shader pack (Iris) forces LOW."
         )
         .defineEnum("quality", CoreClientConfig.CinematicQuality.ULTRA);
      REDUCE_FLASHING = b.comment(
            "Photosensitivity: halves flashes, chromatic aberration and zoom blur and drops per-hit flashes. Flashes are always capped and never repeat faster than 3 times a second."
         )
         .define("reduceFlashing", false);
      BLOOM_STRENGTH = b.comment("Glow intensity of fire and lightning (0 disables bloom).").defineInRange("bloomStrength", 1.0, 0.0, 2.0);
      MAX_PARTICLES = b.comment("Most hi-fi particles alive at once.").defineInRange("maxParticles", 3000, 256, 16384);
      MAX_RIBBON_POINTS = b.comment("Most ribbon trail points alive at once.").defineInRange("maxRibbonPoints", 1024, 64, 8192);
      MAX_MESHES = b.comment("Most shader meshes (rings, cones, spirals) drawn per frame.").defineInRange("maxMeshes", 64, 8, 512);
      ADAPTIVE_LOAD = b.comment(
            "While effects pile up and frames take longer than 40 ms, render the glow buffer at a quarter of the resolution, shorten the bloom chain and spawn fewer particles; back to full once frames are under 28 ms."
         )
         .define("adaptiveLoad", true);
      b.pop();
      b.comment("Stretched limbs (Gomu Gomu no Mi), drawn with the player's own skin. See docs/GOMU_LIMBS.md.").push("limbs");
      SHOW_ARMOR_ON_STRETCH = b.comment("Keep drawing armour where a stretched limb's own part would be (it does not stretch with the limb).")
         .define("showArmorOnStretch", false);
      MAX_LIMBS = b.comment(
            "Most stretched limbs drawn at once, everyone's together (the oldest give way); halved below HIGH cinematic quality. A swarm (the Gatling's punches and afterimages) has its own budget, three times this."
         )
         .defineInRange("maxLimbs", 12, 2, 64);
      b.pop();
      b.comment(
            "Over-the-shoulder camera: the 4th F5 state (first person > back > front > shoulder). The camera looks exactly where the player looks, so the crosshair stays in the screen centre."
         )
         .push("shoulderCamera");
      SHOULDER_ENABLED = b.comment("Add the shoulder state to F5. Off: F5 cycles the three vanilla states.").define("enabled", true);
      SHOULDER_DISTANCE = b.comment("Distance behind the eyes in blocks.").defineInRange("distance", 3.2, 1.0, 8.0);
      SHOULDER_HORIZONTAL = b.comment("Sideways offset in blocks (positive = towards the chosen shoulder).").defineInRange("horizontalOffset", 0.65, -2.0, 2.0);
      SHOULDER_VERTICAL = b.comment("Height above the eyes in blocks.").defineInRange("verticalOffset", 0.15, -2.0, 2.0);
      SHOULDER_SIDE = b.comment("Shoulder side (swap in game with the \"Swap shoulder side\" key).").defineEnum("side", CoreClientConfig.ShoulderSide.RIGHT);
      SHOULDER_SMOOTHING = b.comment("Ticks a change of state, side, pose or form takes to blend (0 = instant).").defineInRange("smoothingTicks", 4, 0, 20);
      SHOULDER_COLLISION = b.comment("Pull the camera in front of blocks instead of into them.").define("collision", true);
      SHOULDER_SCALE = b.comment("Scale the offsets with the player's size (big forms keep their framing).").define("scaleWithEntity", true);
      SHOULDER_FOV = b.comment("Field of view multiplier in the shoulder state.").defineInRange("fovMultiplier", 1.0, 0.5, 1.5);
      SHOULDER_AIM = b.comment(
            "Techniques aim at what the crosshair points at (the client sends that point; the server checks it). Vanilla attacks stay vanilla."
         )
         .define("aimFromCrosshair", true);
      SHOULDER_HIDE_BELOW = b.comment("Hide your own model while the camera is pulled closer than this to the eyes (x size).")
         .defineInRange("hideModelBelowDistance", 0.6, 0.0, 3.0);
      SHOULDER_FORMS = b.comment(
            "Per-form framing replacing distance, horizontalOffset, verticalOffset: \"<form>=<distance>,<horizontal>,<vertical>\" (form name such as hybrid, or its full id). Forms with their own camera (the Azure Dragon) never use the shoulder state."
         )
         .defineListAllowEmpty("formOverrides", List.of("hybrid=2.6,0.75,0.1"), () -> "hybrid=2.6,0.75,0.1", o -> {
            if (o instanceof String str && str.indexOf(61) > 0) {
               return true;
            }

            return false;
         });
      b.pop();
      SPEC = b.build();
   }

   public static enum CinematicQuality {
      ULTRA,
      HIGH,
      MEDIUM,
      LOW;
   }

   public static enum HudAnchor {
      AUTO,
      RIGHT_OF_HOTBAR,
      LEFT_OF_HOTBAR,
      RIGHT_EDGE,
      LEFT_EDGE;
   }

   public static enum KeyMode {
      TOGGLE,
      HOLD;
   }

   public static enum ShoulderSide {
      RIGHT,
      LEFT;
   }

   public static enum VfxQuality {
      LOW(0.35F),
      MEDIUM(0.7F),
      HIGH(1.0F);

      public final float particleScale;

      private VfxQuality(float particleScale) {
         this.particleScale = particleScale;
      }
   }
}
