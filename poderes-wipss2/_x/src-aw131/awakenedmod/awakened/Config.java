package com.awakenedmod.awakened;

import com.awakenedmod.awakened.core.terrain.BlockBreaking;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class Config {
   private static final Builder BUILDER = new Builder();
   public static final DoubleValue AWAKEN_HEALTH_THRESHOLD = BUILDER.comment("Health at or below which a player awakens, in half-hearts (6 = 3 hearts).")
      .defineInRange("awakening.healthThreshold", 6.0, 0.5, 20.0);
   public static final DoubleValue OLD_UNIQUE_POWERS_STALE_AFTER_DAYS = BUILDER.comment("Old setting, now in awakened-rules-server.toml.")
      .defineInRange("uniquePowers.staleAfterDays", 0.0, 0.0, 3650.0);
   public static final IntValue OLD_UNIQUE_POWERS_DEFAULT_LIMIT = BUILDER.comment("Old setting, now in awakened-rules-server.toml.")
      .defineInRange("uniquePowers.defaultLimit", 1, 0, 1000);
   public static final ConfigValue<List<? extends String>> OLD_UNIQUE_POWERS_LIMITS = BUILDER.comment("Old setting, now in awakened-rules-server.toml.")
      .defineListAllowEmpty("uniquePowers.limits", List.of(), () -> "powerId=1", o -> {
         if (o instanceof String s && s.contains("=")) {
            return true;
         }

         return false;
      });
   public static final DoubleValue OLD_STAMINA_MAX = BUILDER.comment("Old setting, now in awakened-balance-server.toml.")
      .defineInRange("stamina.max", 100.0, 1.0, 10000.0);
   public static final DoubleValue OLD_STAMINA_REGEN_PER_SECOND = BUILDER.comment("Old setting, now in awakened-balance-server.toml.")
      .defineInRange("stamina.regenPerSecond", 11.0, 0.0, 10000.0);
   public static final DoubleValue OLD_STAMINA_REGEN_DELAY_SECONDS = BUILDER.comment("Old setting, now in awakened-balance-server.toml.")
      .defineInRange("stamina.regenDelaySeconds", 1.5, 0.0, 60.0);
   public static final DoubleValue OLD_ABILITY_DAMAGE_MULTIPLIER = BUILDER.comment("Old setting, now in awakened-balance-server.toml.")
      .defineInRange("abilities.damageMultiplier", 1.0, 0.0, 100.0);
   public static final DoubleValue OLD_ABILITY_STAMINA_MULTIPLIER = BUILDER.comment("Old setting, now in awakened-balance-server.toml.")
      .defineInRange("abilities.staminaMultiplier", 1.0, 0.0, 100.0);
   public static final BooleanValue OLD_ADDITIVE_DAMAGE_STACKING = BUILDER.comment("Old setting, now in awakened-balance-server.toml.")
      .define("abilities.additiveDamageStacking", true);
   public static final ConfigValue<List<? extends String>> OLD_ABILITY_DAMAGE_OVERRIDES = BUILDER.comment("Old setting, now in awakened-balance-server.toml.")
      .defineListAllowEmpty("abilities.damageOverrides", List.of(), () -> "abilityKey=1.0", o -> {
         if (o instanceof String s && s.contains("=")) {
            return true;
         }

         return false;
      });
   public static final ConfigValue<List<? extends String>> OLD_ABILITY_STAMINA_OVERRIDES = BUILDER.comment("Old setting, now in awakened-balance-server.toml.")
      .defineListAllowEmpty("abilities.staminaOverrides", List.of(), () -> "abilityKey=1.0", o -> {
         if (o instanceof String s && s.contains("=")) {
            return true;
         }

         return false;
      });
   public static final DoubleValue NUCLEAR_OVERHEAT_COOLDOWN_SECONDS = BUILDER.comment(
         "Seconds before a Nuclear player's Overheat can explode again. 0 = no cooldown."
      )
      .defineInRange("nuclear.overheatCooldownSeconds", 60.0, 0.0, 3600.0);
   public static final EnumValue<BlockBreaking> BLOCK_BREAKING = BUILDER.comment(
         "What happens to blocks an ability breaks. RESTORE: they come back. PERMANENT: they stay broken. OFF: nothing breaks. Nuclear's Excavate ignores this."
      )
      .defineEnum("gameplay.blockBreaking", BlockBreaking.RESTORE);
   public static final BooleanValue OLD_ALLOW_TERRAIN_DAMAGE = BUILDER.comment("Old setting, replaced by gameplay.blockBreaking.")
      .define("gameplay.allowTerrainDamage", false);
   public static final BooleanValue OLD_RUIN_VISUAL_ONLY_TERRAIN = BUILDER.comment("Old setting, replaced by gameplay.blockBreaking.")
      .define("gameplay.ruinVisualOnlyTerrain", false);
   public static final BooleanValue BOSS_RANGE_IMMUNITY = BUILDER.comment("Bosses ignore hits from far away.").define("bosses.rangeImmunity", true);
   public static final BooleanValue BOSS_DAMAGE_CAP = BUILDER.comment("Bosses take limited damage from one hit.").define("bosses.damageCap", true);
   public static final BooleanValue BOSS_BLOCK_BREAKING = BUILDER.comment(
         "Bosses break the blocks around them when their target hides. Also needs the mobGriefing gamerule."
      )
      .define("bosses.blockBreaking", false);
   public static final BooleanValue BOSS_DEATH_CINEMATICS = BUILDER.comment("Bosses play a death scene before dropping loot.")
      .define("bosses.deathCinematics", true);
   public static final BooleanValue BOSS_DANGER_CLOSE_CUE = BUILDER.comment("Players hear a warning ping before a boss attack hits them.")
      .define("bosses.dangerCloseCue", true);
   public static final DoubleValue ARBITER_DAMAGE_SCALE = BUILDER.comment("Multiplier on the Arbiter's damage.")
      .defineInRange("bosses.arbiterDamageScale", 1.0, 0.0, 10.0);
   public static final DoubleValue LEVIATHAN_CANNON_HP = BUILDER.comment("Health of each Leviathan cannon.")
      .defineInRange("bosses.leviathanCannonHp", 110.0, 10.0, 2000.0);
   public static final DoubleValue LEVIATHAN_CANNON_BLEED = BUILDER.comment("Share of cannon damage that also hurts the Leviathan.")
      .defineInRange("bosses.leviathanCannonBleed", 0.5, 0.0, 1.0);
   public static final BooleanValue TEAMS_ENABLED = BUILDER.comment("Players can make teams.").define("teams.enabled", true);
   public static final BooleanValue TEAM_FRIENDLY_FIRE_DEFAULT = BUILDER.comment("New teams start with friendly fire on.")
      .define("teams.friendlyFireDefault", false);
   public static final IntValue TEAM_MAX_MEMBERS = BUILDER.comment("Most players on one team, leader included.").defineInRange("teams.maxMembers", 8, 2, 100);
   public static final IntValue TEAM_INVITE_TIMEOUT_SECONDS = BUILDER.comment("Seconds a team invite lasts.")
      .defineInRange("teams.inviteTimeoutSeconds", 60, 5, 3600);
   public static final BooleanValue DISCORD_ADVERT_ENABLED = BUILDER.comment("Show a 'join our Discord' message in chat on login.")
      .define("community.discordAdvert", true);
   public static final ConfigValue<String> DISCORD_INVITE_URL = BUILDER.comment("Discord invite link for the message.")
      .define("community.discordInviteUrl", "https://discord.gg/nmfnB8z9Ve");
   public static final DoubleValue DISCORD_ADVERT_COOLDOWN_HOURS = BUILDER.comment("Hours before the same player sees the message again.")
      .defineInRange("community.discordAdvertCooldownHours", 48.0, 0.0, 100000.0);
   public static final ModConfigSpec SPEC = BUILDER.build();

   private Config() {
   }
}
