package com.awakenedmod.awakened.content.kineticcharger;

import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

final class TimedBlows {
   private final Map<UUID, List<TimedBlows.Wave>> waves;

   static boolean stillLegal(ServerPlayer player, ServerLevel level, LivingEntity target) {
      return target.isAlive() && target.level() == level && Harm.allowed(player, target) && !Foes.fightsFor(player, target);
   }

   TimedBlows(Map<UUID, List<TimedBlows.Wave>> state) {
      this.waves = state;
   }

   void launch(ServerPlayer player, List<TimedBlows.Blow> blows, TimedBlows.Landing landing) {
      this.launch(player, blows, landing, null);
   }

   void launch(ServerPlayer player, List<TimedBlows.Blow> blows, TimedBlows.Landing landing, @Nullable Consumer<List<LivingEntity>> finished) {
      if (!blows.isEmpty()) {
         TimedBlows.Wave wave = new TimedBlows.Wave(landing, finished);
         wave.pending.addAll(blows);
         if (!advance(player, wave)) {
            this.waves.computeIfAbsent(player.getUUID(), id -> new ArrayList<>()).add(wave);
         }
      }
   }

   void tick(ServerPlayer player) {
      List<TimedBlows.Wave> live = this.waves.get(player.getUUID());
      if (live != null) {
         Iterator<TimedBlows.Wave> it = live.iterator();

         while (it.hasNext()) {
            TimedBlows.Wave wave = it.next();
            wave.age++;
            if (advance(player, wave)) {
               it.remove();
            }
         }

         if (live.isEmpty()) {
            this.waves.remove(player.getUUID());
         }
      }
   }

   private static boolean advance(ServerPlayer player, TimedBlows.Wave wave) {
      ServerLevel level = player.serverLevel();
      wave.pending.removeIf(blow -> {
         if (blow.delay() > wave.age) {
            return false;
         } else {
            if (wave.landing.land(player, level, blow.target(), blow.dist())) {
               wave.struck.add(blow.target());
            }

            return true;
         }
      });
      if (!wave.pending.isEmpty()) {
         return false;
      } else {
         if (!wave.struck.isEmpty()) {
            MasterySystem.award(player, MasteryReason.HIT, wave.struck);
         }

         if (wave.finished != null) {
            wave.finished.accept(wave.struck);
         }

         return true;
      }
   }

   static record Blow(LivingEntity target, int delay, double dist) {
   }

   @FunctionalInterface
   interface Landing {
      boolean land(ServerPlayer var1, ServerLevel var2, LivingEntity var3, double var4);
   }

   static final class Wave {
      private final TimedBlows.Landing landing;
      @Nullable
      private final Consumer<List<LivingEntity>> finished;
      private final List<TimedBlows.Blow> pending = new ArrayList<>();
      private final List<LivingEntity> struck = new ArrayList<>();
      private int age;

      private Wave(TimedBlows.Landing landing, @Nullable Consumer<List<LivingEntity>> finished) {
         this.landing = landing;
         this.finished = finished;
      }
   }
}
