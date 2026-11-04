package com.awakenedmod.awakened.content.command;

import com.awakenedmod.awakened.content.command.vocab.TargetKind;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class CommandFeats {
   public static final String HELD_KILL = "command_held_kill";
   public static final String COUNTER = "command_counter";
   public static final String CROWD = "command_crowd";
   public static final String BIND = "command_bind";
   public static final int CROWD_SIZE = 5;
   private static final Map<UUID, long[]> CROWD_REPORTED = new ConcurrentHashMap<>();

   private CommandFeats() {
   }

   static void landed(ServerPlayer caster, Rule rule, LivingEntity body, boolean bound) {
      if (bound) {
         QuestSystem.feat(caster, "command_bind");
      }

      if (rule.target() != null && rule.target().kind() == TargetKind.HITS_ME && Foes.counts(caster, body)) {
         QuestSystem.feat(caster, "command_counter");
      }
   }

   static void killed(ServerPlayer caster, LivingEntity victim) {
      if (Foes.countsAsKill(caster, victim)) {
         if (RuleEngine.holds(caster, victim.getUUID())) {
            QuestSystem.feat(caster, "command_held_kill");
         }
      }
   }

   static void aura(ServerPlayer caster, int slot, long armedAt, int foes) {
      if (foes >= 5) {
         long[] reported = CROWD_REPORTED.computeIfAbsent(caster.getUUID(), id -> {
            long[] fresh = new long[5];
            Arrays.fill(fresh, Long.MIN_VALUE);
            return fresh;
         });
         if (reported[slot] != armedAt) {
            reported[slot] = armedAt;
            QuestSystem.feat(caster, "command_crowd");
         }
      }
   }

   static void forget(UUID caster) {
      CROWD_REPORTED.remove(caster);
   }

   static void forgetAll() {
      CROWD_REPORTED.clear();
   }
}
