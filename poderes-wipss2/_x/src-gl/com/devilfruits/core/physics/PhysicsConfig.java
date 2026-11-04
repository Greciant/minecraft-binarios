package com.devilfruits.core.physics;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class PhysicsConfig {
   public static final ModConfigSpec SPEC;
   public static final BooleanValue ENABLED;
   public static final ConfigValue<List<? extends String>> DISABLED_FRUITS;
   public static final BooleanValue OTHER_PLAYERS_STRUCTURES;
   public static final BooleanValue BLOCK_DAMAGE;
   public static final BooleanValue IMPULSES;
   public static final BooleanValue REACTIONS;
   public static final BooleanValue LANDINGS;
   public static final BooleanValue PLAIN_JUMPS;
   public static final BooleanValue DRAG_EFFECTS;
   public static final BooleanValue INHERIT_VELOCITY;
   public static final BooleanValue SAND_ON_STRUCTURES;
   public static final BooleanValue EMPOWERED_MELEE;
   public static final DoubleValue IMPULSE_MULTIPLIER;
   public static final DoubleValue REACTION_MULTIPLIER;
   public static final DoubleValue BLOCK_DAMAGE_MULTIPLIER;
   public static final DoubleValue ARMAMENT_BLOCK_BONUS;
   public static final DoubleValue MAX_IMPULSE_PER_HIT;
   public static final DoubleValue MAX_VELOCITY_CHANGE_PER_TICK;
   public static final DoubleValue IMPULSE_BUDGET_PER_SECOND;
   public static final IntValue MAX_BLOCKS_PER_CAST;
   public static final IntValue MAX_BLOCKS_PER_TICK;
   public static final IntValue MAX_SCAN_VOLUME;
   public static final DoubleValue MIN_MASS_FOR_REACTION;
   public static final DoubleValue SELF_HIT_FACTOR;
   public static final BooleanValue SELF_HIT_CONSERVES_MOMENTUM;
   public static final IntValue MAX_SELF_HITS_PER_SECOND;
   public static final DoubleValue PLAYER_MASS;
   public static final DoubleValue LANDING_FACTOR;
   public static final DoubleValue LANDING_MIN_FALL;
   public static final DoubleValue DRAGON_SEGMENT_WEIGHT;
   public static final DoubleValue MASS_SCALE;
   public static final DoubleValue MAX_LOAD_ACCELERATION;

   public static boolean enabledFor(ResourceLocation fruit) {
      return !ENABLED.get() ? false : fruit == null || !((List)DISABLED_FRUITS.get()).contains(fruit.toString());
   }

   private PhysicsConfig() {
   }

   static {
      Builder b = new Builder();
      b.comment("Physics structures (Sable sub-levels). Only used when Sable is installed.").push("general");
      ENABLED = b.comment("Master switch: abilities break, push and ride physics structures.").define("enabled", true);
      DISABLED_FRUITS = b.comment("Fruit ids whose abilities leave physics structures alone (e.g. \"devilfruits:gura_gura\").")
         .defineListAllowEmpty("disabledFruits", List.of(), () -> "devilfruits:gura_gura", o -> {
            if (o instanceof String s && s.contains(":")) {
               return true;
            }

            return false;
         });
      OTHER_PLAYERS_STRUCTURES = b.comment(
            new String[]{
               "Abilities affect any structure. Off: a player only affects the structure they stand on",
               "(Sable keeps no owner of a structure, so 'aboard' is the only safe test)."
            }
         )
         .define("allowOnOtherPlayersStructures", true);
      b.pop();
      b.comment("Categories.").push("categories");
      BLOCK_DAMAGE = b.comment("Destructive abilities break structure blocks (permanently: structures never heal).").define("blockDamage", true);
      IMPULSES = b.comment("Strikes shove and spin structures.").define("impulses", true);
      REACTIONS = b.comment("Jumps, leaps and dashes push back on the structure under the caster.").define("reactionForces", true);
      LANDINGS = b.comment("Landing on a structure pushes it down (mass x impact speed).").define("landings", true);
      PLAIN_JUMPS = b.comment("Ordinary jumps push back too (gently).").define("plainJumps", true);
      DRAG_EFFECTS = b.comment("Vortices, storms and sand whirls drag and spin light structures.").define("dragEffects", true);
      INHERIT_VELOCITY = b.comment("Projectiles and waves launched from a moving structure keep its velocity.").define("inheritVelocity", true);
      SAND_ON_STRUCTURES = b.comment("Sand blocks of structures count as sand for the Suna Suna (its sand source).").define("sandOnStructures", false);
      EMPOWERED_MELEE = b.comment("Haki-coated or quake-infused bare hands shove (and with Haki chip) the structure block they punch.")
         .define("empoweredMelee", true);
      b.pop();
      b.comment("Multipliers.").push("multipliers");
      IMPULSE_MULTIPLIER = b.comment("All strike impulses.").defineInRange("impulseMultiplier", 1.0, 0.0, 100.0);
      REACTION_MULTIPLIER = b.comment("Jump / dash / landing reaction impulses.").defineInRange("reactionMultiplier", 1.0, 0.0, 100.0);
      BLOCK_DAMAGE_MULTIPLIER = b.comment("Chance factor of a structure block inside an attack to break (1 = like terrain).")
         .defineInRange("blockDamageMultiplier", 1.0, 0.0, 10.0);
      ARMAMENT_BLOCK_BONUS = b.comment("Extra block damage of Haki-coated hits on structures (0.25 = +25%).")
         .defineInRange("armamentBlockBonus", 0.25, 0.0, 10.0);
      b.pop();
      b.comment("Limits (anti-lag, anti-exploit).").push("limits");
      MAX_IMPULSE_PER_HIT = b.comment("Largest impulse one hit may apply (mass units x blocks/s).").defineInRange("maxImpulsePerHit", 25000.0, 0.0, 1.0E7);
      MAX_VELOCITY_CHANGE_PER_TICK = b.comment("Largest velocity change (blocks/s) all hits together may give one structure in one tick.")
         .defineInRange("maxVelocityChangePerTick", 14.0, 0.0, 1000.0);
      IMPULSE_BUDGET_PER_SECOND = b.comment("Velocity change budget (blocks/s) of one structure per second; further hits that second do not push.")
         .defineInRange("impulseBudgetPerSecond", 40.0, 0.0, 10000.0);
      MAX_BLOCKS_PER_CAST = b.comment("Structure blocks one cast may break.").defineInRange("maxBlocksPerCast", 96, 0, 4096);
      MAX_BLOCKS_PER_TICK = b.comment("Structure blocks broken per structure per tick (the rest follows on the next ticks).")
         .defineInRange("maxBlocksPerTick", 48, 1, 4096);
      MAX_SCAN_VOLUME = b.comment("Block cells scanned per structure per area query (larger areas are sampled).")
         .defineInRange("maxScanVolume", 24576, 512, 1048576);
      MIN_MASS_FOR_REACTION = b.comment("Structures lighter than this get no reaction forces (a lone block under the feet is not a launch pad).")
         .defineInRange("minStructureMassForReaction", 4.0, 0.0, 1000000.0);
      SELF_HIT_FACTOR = b.comment("Strikes on the structure the attacker stands on hit with this factor.").defineInRange("selfHitFactor", 0.5, 0.0, 1.0);
      SELF_HIT_CONSERVES_MOMENTUM = b.comment(
            new String[]{
               "A strike on your own structure also pushes back through your feet: no net push, only spin", "(no flying a ship by punching it from aboard)."
            }
         )
         .define("selfHitConservesMomentum", true);
      MAX_SELF_HITS_PER_SECOND = b.comment("Strikes per second on the attacker's own structure that push at all.")
         .defineInRange("maxSelfHitsPerSecond", 3, 0, 100);
      PLAYER_MASS = b.comment("Mass of a player for reaction / landing forces (a block weighs about 1).").defineInRange("playerMass", 2.0, 0.0, 1000.0);
      LANDING_FACTOR = b.comment("Landing impulse = playerMass x impact speed x this.").defineInRange("landingFactor", 0.6, 0.0, 100.0);
      LANDING_MIN_FALL = b.comment("Shortest fall (blocks) that pushes the structure when landing.").defineInRange("landingMinFall", 1.2, 0.0, 64.0);
      DRAGON_SEGMENT_WEIGHT = b.comment("Weight of one dragon body segment resting on a structure (mass units).")
         .defineInRange("dragonSegmentWeight", 0.6, 0.0, 100.0);
      MASS_SCALE = b.comment("Bodies whose weight a fruit changes (Kilo Kilo): their mass in structure units = playerMass x kg / 70 x this.")
         .defineInRange("massScale", 1.0, 0.0, 100.0);
      MAX_LOAD_ACCELERATION = b.comment("Largest extra downward acceleration (blocks/s^2) a body's standing weight may give a structure.")
         .defineInRange("maxLoadAcceleration", 25.0, 0.0, 1000.0);
      b.pop();
      SPEC = b.build();
   }
}
