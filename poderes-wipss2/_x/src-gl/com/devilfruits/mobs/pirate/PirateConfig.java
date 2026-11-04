package com.devilfruits.mobs.pirate;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class PirateConfig {
   public static final ModConfigSpec SPEC;
   public static final DoubleValue SPAWN_CHANCE;
   public static final IntValue MAX_NEARBY;
   public static final BooleanValue CAPTAIN_CREW;
   public static final IntValue CREW_DECKHANDS_MIN;
   public static final IntValue CREW_DECKHANDS_MAX;
   public static final IntValue CREW_GUNNERS_MIN;
   public static final IntValue CREW_GUNNERS_MAX;
   public static final BooleanValue HUNT_VILLAGERS;
   public static final BooleanValue HUNT_GOLEMS;
   public static final BooleanValue HUNT_ILLAGERS;
   public static final BooleanValue FRIENDLY_FIRE;
   public static final IntValue TARGET_SCAN_INTERVAL;
   public static final DoubleValue GEAR_DROP_CHANCE;
   public static final DoubleValue DECKHAND_HEALTH;
   public static final DoubleValue DECKHAND_DAMAGE;
   public static final DoubleValue DECKHAND_SPEED;
   public static final DoubleValue DECKHAND_COMBO_CHANCE;
   public static final IntValue DECKHAND_BARGE_COOLDOWN;
   public static final DoubleValue DECKHAND_RETREAT_HEALTH;
   public static final DoubleValue GUNNER_HEALTH;
   public static final DoubleValue GUNNER_SPEED;
   public static final DoubleValue GUNNER_MUSKET_CHANCE;
   public static final DoubleValue GUNNER_PISTOL_DAMAGE;
   public static final DoubleValue GUNNER_MUSKET_DAMAGE;
   public static final DoubleValue GUNNER_MIN_RANGE;
   public static final DoubleValue GUNNER_MAX_RANGE;
   public static final IntValue GUNNER_AIM_TICKS;
   public static final DoubleValue GUNNER_INACCURACY;
   public static final DoubleValue CAPTAIN_HEALTH;
   public static final DoubleValue CAPTAIN_DAMAGE;
   public static final DoubleValue CAPTAIN_ARMOR;
   public static final DoubleValue CAPTAIN_SPEED;
   public static final DoubleValue CAPTAIN_SCALE;
   public static final BooleanValue CAPTAIN_BOSS_BAR;
   public static final IntValue CAPTAIN_SLAM_COOLDOWN;
   public static final IntValue CAPTAIN_LUNGE_COOLDOWN;
   public static final IntValue CAPTAIN_PISTOL_COOLDOWN;
   public static final IntValue CAPTAIN_RALLY_COOLDOWN;
   public static final DoubleValue CAPTAIN_SLAM_RADIUS;
   public static final DoubleValue CAPTAIN_SLAM_DAMAGE;
   public static final BooleanValue CAPTAIN_SLAM_BLOCK_DAMAGE;
   public static final DoubleValue CAPTAIN_PISTOL_DAMAGE;
   public static final DoubleValue CAPTAIN_ENRAGE_HEALTH;
   public static final DoubleValue CAPTAIN_ENRAGE_COOLDOWN_FACTOR;
   public static final DoubleValue RALLY_RADIUS;
   public static final IntValue RALLY_SECONDS;
   public static final DoubleValue PISTOL_DAMAGE;
   public static final DoubleValue MUSKET_DAMAGE;
   public static final IntValue PISTOL_RELOAD_TICKS;
   public static final IntValue MUSKET_RELOAD_TICKS;
   public static final IntValue PISTOL_COOLDOWN;
   public static final IntValue MUSKET_COOLDOWN;
   public static final DoubleValue PISTOL_SPREAD;
   public static final DoubleValue MUSKET_SPREAD;
   public static final DoubleValue BALL_SPEED;
   public static final DoubleValue ARMOR_PIERCING;
   public static final BooleanValue GUNPOWDER_PER_SHOT;
   public static final BooleanValue BALL_PUSHES_STRUCTURES;

   private PirateConfig() {
   }

   static {
      Builder b = new Builder();
      b.comment("Spawning and behaviour.").push("general");
      SPAWN_CHANCE = b.comment(
            "Chance a natural spawn attempt that picked a pirate goes ahead (the weights live in data/devilfruits/neoforge/biome_modifier/pirates.json)."
         )
         .defineInRange("spawnChance", 1.0, 0.0, 1.0);
      MAX_NEARBY = b.comment("No natural pirate spawns where this many pirates are already within 64 blocks.").defineInRange("maxNearby", 12, 0, 256);
      CAPTAIN_CREW = b.comment("A naturally spawned captain lands with a crew.").define("captainCrew", true);
      CREW_DECKHANDS_MIN = b.defineInRange("crewDeckhandsMin", 2, 0, 16);
      CREW_DECKHANDS_MAX = b.defineInRange("crewDeckhandsMax", 4, 0, 16);
      CREW_GUNNERS_MIN = b.defineInRange("crewGunnersMin", 1, 0, 16);
      CREW_GUNNERS_MAX = b.defineInRange("crewGunnersMax", 2, 0, 16);
      HUNT_VILLAGERS = b.comment("Pirates attack villagers and wandering traders.").define("huntVillagers", true);
      HUNT_GOLEMS = b.comment("Pirates attack iron golems.").define("huntGolems", true);
      HUNT_ILLAGERS = b.comment("Pirates attack illagers (off: they leave each other alone).").define("huntIllagers", false);
      FRIENDLY_FIRE = b.comment("Pirate shots and blows can hurt other pirates.").define("friendlyFire", false);
      TARGET_SCAN_INTERVAL = b.comment("Ticks between target scans (performance).").defineInRange("targetScanInterval", 10, 1, 100);
      GEAR_DROP_CHANCE = b.comment("Chance a pirate drops its weapon (the captain's cutlass only comes from his loot table).")
         .defineInRange("gearDropChance", 0.085, 0.0, 1.0);
      b.pop();
      b.comment("Pirate Deckhand: a fast cutlass rusher.").push("deckhand");
      DECKHAND_HEALTH = b.defineInRange("health", 24.0, 1.0, 1024.0);
      DECKHAND_DAMAGE = b.defineInRange("damage", 5.0, 0.0, 1024.0);
      DECKHAND_SPEED = b.defineInRange("speed", 0.26, 0.05, 2.0);
      DECKHAND_COMBO_CHANCE = b.comment("Chance the first slash is followed by a second.").defineInRange("comboChance", 0.6, 0.0, 1.0);
      DECKHAND_BARGE_COOLDOWN = b.defineInRange("bargeCooldownTicks", 120, 0, 2400);
      DECKHAND_RETREAT_HEALTH = b.comment("Below this share of health it backs off for a moment.").defineInRange("retreatHealth", 0.25, 0.0, 1.0);
      b.pop();
      b.comment("Pirate Gunner: a flintlock pistol or (as a variant) a musket.").push("gunner");
      GUNNER_HEALTH = b.defineInRange("health", 20.0, 1.0, 1024.0);
      GUNNER_SPEED = b.defineInRange("speed", 0.3, 0.05, 2.0);
      GUNNER_MUSKET_CHANCE = b.defineInRange("musketChance", 0.3, 0.0, 1.0);
      GUNNER_PISTOL_DAMAGE = b.defineInRange("pistolDamage", 6.0, 0.0, 1024.0);
      GUNNER_MUSKET_DAMAGE = b.defineInRange("musketDamage", 12.0, 0.0, 1024.0);
      GUNNER_MIN_RANGE = b.defineInRange("minRange", 8.0, 1.0, 64.0);
      GUNNER_MAX_RANGE = b.defineInRange("maxRange", 16.0, 2.0, 64.0);
      GUNNER_AIM_TICKS = b.comment("Aiming time before a shot (the musket takes twice as long).").defineInRange("aimTicks", 14, 0, 200);
      GUNNER_INACCURACY = b.defineInRange("inaccuracy", 2.5, 0.0, 30.0);
      b.pop();
      b.comment("Pirate Captain: the mini-boss.").push("captain");
      CAPTAIN_HEALTH = b.defineInRange("health", 150.0, 1.0, 4096.0);
      CAPTAIN_DAMAGE = b.defineInRange("damage", 10.0, 0.0, 1024.0);
      CAPTAIN_ARMOR = b.defineInRange("armor", 8.0, 0.0, 30.0);
      CAPTAIN_SPEED = b.defineInRange("speed", 0.3, 0.05, 2.0);
      CAPTAIN_SCALE = b.comment("Size of the captain (1 = an illager).").defineInRange("scale", 1.35, 0.5, 3.0);
      CAPTAIN_BOSS_BAR = b.define("bossBar", true);
      CAPTAIN_SLAM_COOLDOWN = b.defineInRange("slamCooldownTicks", 140, 0, 2400);
      CAPTAIN_LUNGE_COOLDOWN = b.defineInRange("lungeCooldownTicks", 100, 0, 2400);
      CAPTAIN_PISTOL_COOLDOWN = b.defineInRange("pistolCooldownTicks", 90, 0, 2400);
      CAPTAIN_RALLY_COOLDOWN = b.defineInRange("rallyCooldownTicks", 320, 0, 6000);
      CAPTAIN_SLAM_RADIUS = b.defineInRange("slamRadius", 4.5, 1.0, 16.0);
      CAPTAIN_SLAM_DAMAGE = b.defineInRange("slamDamage", 14.0, 0.0, 1024.0);
      CAPTAIN_SLAM_BLOCK_DAMAGE = b.comment("The slam cracks weak blocks (also needs the mobGriefing game rule).").define("slamBlockDamage", false);
      CAPTAIN_PISTOL_DAMAGE = b.defineInRange("pistolDamage", 9.0, 0.0, 1024.0);
      CAPTAIN_ENRAGE_HEALTH = b.defineInRange("enrageHealth", 0.5, 0.0, 1.0);
      CAPTAIN_ENRAGE_COOLDOWN_FACTOR = b.comment("Cooldowns run this much faster once enraged.").defineInRange("enrageCooldownSpeed", 1.6, 1.0, 10.0);
      RALLY_RADIUS = b.defineInRange("rallyRadius", 12.0, 1.0, 64.0);
      RALLY_SECONDS = b.defineInRange("rallySeconds", 6, 1, 120);
      b.pop();
      b.comment("Flintlock pistol and musket in a player's hands.").push("weapons");
      PISTOL_DAMAGE = b.defineInRange("pistolDamage", 8.0, 0.0, 1024.0);
      MUSKET_DAMAGE = b.defineInRange("musketDamage", 15.0, 0.0, 1024.0);
      PISTOL_RELOAD_TICKS = b.defineInRange("pistolReloadTicks", 40, 1, 400);
      MUSKET_RELOAD_TICKS = b.defineInRange("musketReloadTicks", 70, 1, 600);
      PISTOL_COOLDOWN = b.defineInRange("pistolCooldownTicks", 10, 0, 400);
      MUSKET_COOLDOWN = b.defineInRange("musketCooldownTicks", 20, 0, 400);
      PISTOL_SPREAD = b.defineInRange("pistolSpread", 1.5, 0.0, 30.0);
      MUSKET_SPREAD = b.defineInRange("musketSpread", 0.4, 0.0, 30.0);
      BALL_SPEED = b.comment("Muzzle speed in blocks per tick.").defineInRange("ballSpeed", 4.0, 0.5, 10.0);
      ARMOR_PIERCING = b.comment("Share of a ball's damage that ignores armour.").defineInRange("armorPiercing", 0.25, 0.0, 1.0);
      GUNPOWDER_PER_SHOT = b.comment("Reloading also uses up one gunpowder.").define("gunpowderPerShot", true);
      BALL_PUSHES_STRUCTURES = b.comment("A ball gives a physics structure (Sable) a tiny shove where it hits; never breaks blocks.")
         .define("ballPushesStructures", true);
      b.pop();
      SPEC = b.build();
   }
}
