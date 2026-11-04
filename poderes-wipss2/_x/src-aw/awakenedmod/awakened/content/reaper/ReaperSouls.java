package com.awakenedmod.awakened.content.reaper;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

public final class ReaperSouls {
   private static final int RESPAWN_IMMUNITY_TICKS = 1200;
   private static final long DR_DECAY_TICKS = 3600L;
   private static final long MUTUAL_KILL_WINDOW = 600L;
   private static final double SCYTHE_BONUS = 1.5;
   private static final double MARK_KILL_BONUS = 0.5;
   private static final double MARK_CHAIN_RANGE = 12.0;
   private static final long TOAST_WINDOW = 40L;
   private static final Map<UUID, Map<UUID, ReaperSouls.DrEntry>> PLAYER_DR = new ConcurrentHashMap<>();
   private static final Map<UUID, ReaperSouls.PvpKill> RECENT_PVP = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> SOULLESS_UNTIL = new ConcurrentHashMap<>();
   private static final Map<UUID, ReaperSouls.ToastAcc> SOUL_TOAST = new ConcurrentHashMap<>();

   private ReaperSouls() {
   }

   public static void armRespawnImmunity(ServerPlayer player) {
      SOULLESS_UNTIL.put(player.getUUID(), player.serverLevel().getGameTime() + 1200L);
   }

   private static long soullessUntil(UUID victim) {
      Long until = SOULLESS_UNTIL.get(victim);
      return until == null ? 0L : until;
   }

   public static void onKill(ServerPlayer killer, LivingEntity victim) {
      long value = valueOf(victim, killer);
      if (value <= 0L) {
         DeathMarkControl.clear(victim);
      } else {
         boolean marked = DeathMarkControl.isMarked(victim);
         if (marked && Foes.countsAsKill(killer, victim) && killer.getUUID().equals(DeathMarkControl.markerOf(victim))) {
            QuestSystem.feat(killer, "reaper_condemn");
         }

         if (marked) {
            value += Math.max(1L, Math.round(baseValueOf(victim, killer) * 0.5));
            if (victim instanceof ServerPlayer markedPlayer) {
               RECENT_PVP.put(killer.getUUID(), new ReaperSouls.PvpKill(markedPlayer.getUUID(), value, killer.level().getGameTime()));
            }
         }

         ReaperAttachments.add(killer, value);
         PowerService.sync(killer);
         showSoulToast(killer, value);
         ServerLevel level = Dmg.serverLevel(killer);
         Vec3 chest = killer.position().add(0.0, (double)killer.getBbHeight() * 0.6, 0.0);
         ReaperFx.harvest(level, victim.position(), chest, value);
         PowerService.playAnimation(killer, "harvest");
         if (marked) {
            chainDeathMark(killer, victim, level);
         }
      }
   }

   private static void chainDeathMark(ServerPlayer killer, LivingEntity victim, ServerLevel level) {
      DeathMarkControl.clear(victim);
      Vec3 corpse = victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0);
      LivingEntity next = null;
      double bestSq = 144.0;

      for (LivingEntity candidate : AimUtil.livingAround(killer, corpse, 12.0)) {
         if (candidate != victim && !(candidate instanceof Player) && !DeathMarkControl.isMarked(candidate) && Harm.allowed(killer, candidate)) {
            double d = candidate.distanceToSqr(corpse);
            if (d < bestSq) {
               bestSq = d;
               next = candidate;
            }
         }
      }

      if (next != null) {
         DeathMarkControl.markFor(killer.getUUID(), next, level.getGameTime(), ReaperAttachments.souls(killer).souls());
         ReaperFx.deathMarkLeap(level, corpse, next.position().add(0.0, (double)next.getBbHeight() * 0.6, 0.0));
      }
   }

   private static void showSoulToast(ServerPlayer killer, long gained) {
      long now = killer.level().getGameTime();
      ReaperSouls.ToastAcc t = SOUL_TOAST.computeIfAbsent(killer.getUUID(), k -> new ReaperSouls.ToastAcc());
      if (now > t.windowStart + 40L) {
         t.windowStart = now;
         t.delta = 0L;
      }

      t.delta += gained;
      long total = ReaperAttachments.souls(killer).souls();
      killer.displayClientMessage(Component.translatable("awakened.reaper.soul_gain", new Object[]{t.delta, total}).withStyle(ChatFormatting.AQUA), true);
   }

   public static void onPlayerDeath(ServerPlayer victim, DamageSource source) {
      if (source.getEntity() instanceof ServerPlayer slayer) {
         ReaperSouls.PvpKill var6 = RECENT_PVP.get(victim.getUUID());
         long now = victim.level().getGameTime();
         if (var6 != null && var6.victim().equals(slayer.getUUID()) && now - var6.gameTime() <= 600L) {
            ReaperAttachments.withdraw(victim, var6.souls());
            PowerService.sync(victim);
            RECENT_PVP.remove(victim.getUUID());
         }
      }
   }

   public static long valueOf(LivingEntity victim, ServerPlayer killer) {
      if (victim instanceof ServerPlayer victimPlayer) {
         return playerValue(victimPlayer, killer);
      } else {
         double base = mobBaseValue(victim);
         return Math.max(0L, Math.round(applyScythe(base, killer)));
      }
   }

   private static double baseValueOf(LivingEntity victim, ServerPlayer killer) {
      return victim instanceof ServerPlayer victimPlayer ? playerBaseValue(victimPlayer) : mobBaseValue(victim);
   }

   private static double mobBaseValue(LivingEntity victim) {
      EntityType<?> type = victim.getType();
      if (victim instanceof EnderDragon) {
         return 200.0;
      } else if (victim instanceof Warden) {
         return 250.0;
      } else if (victim instanceof WitherBoss) {
         return 150.0;
      } else if (victim instanceof ElderGuardian) {
         return 35.0;
      } else if (victim instanceof Ravager) {
         return 30.0;
      } else if (type == EntityType.EVOKER) {
         return 12.0;
      } else if (victim instanceof PiglinBrute) {
         return 8.0;
      } else if (type == EntityType.WITHER_SKELETON) {
         return 7.0;
      } else if (victim instanceof Hoglin || victim instanceof Ghast) {
         return 6.0;
      } else if (victim instanceof Blaze) {
         return 5.0;
      } else if (victim instanceof Witch
         || type == EntityType.PILLAGER
         || type == EntityType.VINDICATOR
         || type == EntityType.HUSK
         || type == EntityType.STRAY
         || type == EntityType.DROWNED
         || type == EntityType.BOGGED) {
         return 3.0;
      } else if (victim instanceof Monster) {
         return 2.0;
      } else {
         return victim instanceof Animal ? 1.0 : 1.0;
      }
   }

   private static long playerValue(ServerPlayer victim, ServerPlayer killer) {
      long now = killer.level().getGameTime();
      if (now < soullessUntil(victim.getUUID())) {
         return 0L;
      } else {
         ReaperSouls.PvpKill victimsPrior = RECENT_PVP.get(victim.getUUID());
         boolean tradeBack = victimsPrior != null && victimsPrior.victim().equals(killer.getUUID()) && now - victimsPrior.gameTime() <= 600L;
         double base = playerBaseValue(victim);
         int recentKills = recentPlayerKills(killer.getUUID(), victim.getUUID(), now);
         double drFactor = Math.max(0.1, 1.0 - 0.25 * (double)recentKills);
         recordPlayerKill(killer.getUUID(), victim.getUUID(), now);
         long awarded = tradeBack ? 0L : Math.max(0L, Math.round(applyScythe(base * drFactor, killer)));
         RECENT_PVP.put(killer.getUUID(), new ReaperSouls.PvpKill(victim.getUUID(), awarded, now));
         return awarded;
      }
   }

   private static double playerBaseValue(ServerPlayer victim) {
      long theirSouls = ReaperAttachments.souls(victim).souls();
      double armorBonus = (double)victim.getArmorValue() * 1.5;
      return Mth.clamp(10.0 + Math.min(90.0, (double)theirSouls / 55.0) + armorBonus, 10.0, 120.0);
   }

   private static double applyScythe(double value, ServerPlayer killer) {
      return killer.getMainHandItem().is((Item)AwakenedItems.REAPER_SCYTHE.get()) ? value * 1.5 : value;
   }

   private static int recentPlayerKills(UUID killer, UUID victim, long now) {
      Map<UUID, ReaperSouls.DrEntry> byVictim = PLAYER_DR.get(killer);
      if (byVictim == null) {
         return 0;
      } else {
         ReaperSouls.DrEntry e = byVictim.get(victim);
         return e == null ? 0 : decayed(e, now);
      }
   }

   private static void recordPlayerKill(UUID killer, UUID victim, long now) {
      Map<UUID, ReaperSouls.DrEntry> byVictim = PLAYER_DR.computeIfAbsent(killer, k -> new ConcurrentHashMap<>());
      ReaperSouls.DrEntry e = byVictim.computeIfAbsent(victim, v -> new ReaperSouls.DrEntry());
      e.count = decayed(e, now) + 1;
      e.lastGameTime = now;
   }

   private static int decayed(ReaperSouls.DrEntry e, long now) {
      long elapsed = now - e.lastGameTime;
      if (elapsed <= 0L) {
         return e.count;
      } else {
         int decay = (int)(elapsed / 3600L);
         return Math.max(0, e.count - decay);
      }
   }

   public static void forget(UUID player) {
      PLAYER_DR.remove(player);

      for (Map<UUID, ReaperSouls.DrEntry> byVictim : PLAYER_DR.values()) {
         byVictim.remove(player);
      }

      RECENT_PVP.remove(player);
      RECENT_PVP.entrySet().removeIf(en -> en.getValue().victim().equals(player));
      SOULLESS_UNTIL.remove(player);
      SOUL_TOAST.remove(player);
   }

   private static final class DrEntry {
      long lastGameTime;
      int count;
   }

   private static record PvpKill(UUID victim, long souls, long gameTime) {
   }

   private static final class ToastAcc {
      long windowStart = Long.MIN_VALUE;
      long delta;
   }
}
