package com.devilfruits.fruits.doru_doru.atelier;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class UndoHistory {
   private static final Map<UUID, Deque<UndoHistory.Change>> UNDO = new HashMap<>();
   private static final Map<UUID, Deque<UndoHistory.Change>> REDO = new HashMap<>();

   public static void push(UUID player, UndoHistory.Change change, int depth) {
      if (!change.isEmpty() && depth > 0) {
         Deque<UndoHistory.Change> undo = UNDO.computeIfAbsent(player, k -> new ArrayDeque<>());
         undo.push(change);

         while (undo.size() > depth) {
            undo.removeLast();
         }

         REDO.remove(player);
      }
   }

   public static UndoHistory.Change popUndo(UUID player) {
      Deque<UndoHistory.Change> d = UNDO.get(player);
      return d != null && !d.isEmpty() ? d.pop() : null;
   }

   public static UndoHistory.Change popRedo(UUID player) {
      Deque<UndoHistory.Change> d = REDO.get(player);
      return d != null && !d.isEmpty() ? d.pop() : null;
   }

   public static void pushRedo(UUID player, UndoHistory.Change change) {
      REDO.computeIfAbsent(player, k -> new ArrayDeque<>()).push(change);
   }

   public static void pushUndoKeepRedo(UUID player, UndoHistory.Change change, int depth) {
      Deque<UndoHistory.Change> undo = UNDO.computeIfAbsent(player, k -> new ArrayDeque<>());
      undo.push(change);

      while (undo.size() > depth) {
         undo.removeLast();
      }
   }

   public static int undoCount(UUID player) {
      Deque<UndoHistory.Change> d = UNDO.get(player);
      return d == null ? 0 : d.size();
   }

   public static int redoCount(UUID player) {
      Deque<UndoHistory.Change> d = REDO.get(player);
      return d == null ? 0 : d.size();
   }

   public static void forget(UUID player) {
      UNDO.remove(player);
      REDO.remove(player);
   }

   private UndoHistory() {
   }

   public static record Change(
      ResourceKey<Level> level,
      Long2LongOpenHashMap before,
      Long2IntOpenHashMap beforeColor,
      Long2LongOpenHashMap after,
      Long2IntOpenHashMap afterColor,
      float energy
   ) {
      public boolean isEmpty() {
         return this.before.isEmpty();
      }
   }
}
