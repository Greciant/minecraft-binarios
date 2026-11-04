package com.awakenedmod.awakened.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.IntPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class ClipClock {
   private static final List<ClipClock> ALL = new CopyOnWriteArrayList<>();
   private final IntPredicate playing;
   private final Map<Integer, ClipClock.Run> runs = new HashMap<>();
   private Level lastLevel;

   public ClipClock(ResourceLocation trigger) {
      this((IntPredicate)(id -> AwakenedAnimations.isPlaying(id, trigger)));
   }

   public ClipClock(IntPredicate playing) {
      this.playing = playing;
      ALL.add(this);
   }

   public static void observeAll(Iterable<? extends Entity> bodies) {
      for (Entity body : bodies) {
         for (ClipClock clock : ALL) {
            clock.age(body);
         }
      }
   }

   public long age(Entity entity) {
      Level level = entity.level();
      if (level != this.lastLevel) {
         this.runs.clear();
         this.lastLevel = level;
      }

      int id = entity.getId();
      if (!this.playing.test(id)) {
         this.runs.remove(id);
         return -1L;
      } else {
         long now = entity.level().getGameTime();
         int generation = AwakenedAnimations.clipGeneration(id);
         ClipClock.Run run = this.runs.get(id);
         if (run != null && run.generation() == generation) {
            return now - run.start();
         } else {
            this.runs.put(id, new ClipClock.Run(now, generation));
            return 0L;
         }
      }
   }

   private static record Run(long start, int generation) {
   }
}
