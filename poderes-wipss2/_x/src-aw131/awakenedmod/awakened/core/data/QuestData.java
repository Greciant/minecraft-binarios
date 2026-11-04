package com.awakenedmod.awakened.core.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class QuestData {
   public static final int SLOTS = 4;
   public static final int MAX_REROLLS = 3;
   public static final int SCHEMA_PRE_REWORK = 0;
   public static final String EMPTY_SLOT = "";
   public static final MapCodec<QuestData> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
               Codec.unboundedMap(Codec.STRING, QuestData.State.CODEC).optionalFieldOf("quests", Map.of()).forGetter(d -> d.states),
               Codec.STRING.listOf().optionalFieldOf("xpClaimed", List.of()).forGetter(d -> List.copyOf(d.xpClaimed)),
               Codec.STRING.listOf().optionalFieldOf("craftedItems", List.of()).forGetter(d -> List.copyOf(d.craftedItems)),
               Codec.STRING.listOf().optionalFieldOf("slate", List.of()).forGetter(d -> List.copyOf(d.slate)),
               Codec.INT.optionalFieldOf("rerolls", 3).forGetter(d -> d.rerolls),
               Codec.STRING.optionalFieldOf("pinned", "").forGetter(d -> d.pinned),
               Codec.INT.optionalFieldOf("schema", 0).forGetter(d -> d.schema),
               Codec.STRING.listOf().optionalFieldOf("itemsClaimed", List.of()).forGetter(d -> List.copyOf(d.itemsClaimed))
            )
            .apply(i, QuestData::new)
   );
   private static final int MAX_TRACKED_CRAFTS = 128;
   private final Map<String, QuestData.State> states;
   private final Set<String> xpClaimed;
   private final Set<String> craftedItems;
   private final Set<String> itemsClaimed;
   private final List<String> slate;
   private int rerolls;
   private String pinned;
   private int schema;

   public QuestData() {
      this.states = new HashMap<>();
      this.xpClaimed = new HashSet<>();
      this.craftedItems = new HashSet<>();
      this.itemsClaimed = new HashSet<>();
      this.slate = emptySlate();
      this.rerolls = 3;
      this.pinned = "";
      this.schema = 0;
   }

   private QuestData(
      Map<String, QuestData.State> states,
      List<String> xpClaimed,
      List<String> craftedItems,
      List<String> slate,
      int rerolls,
      String pinned,
      int schema,
      List<String> itemsClaimed
   ) {
      this.states = new HashMap<>(states);
      this.xpClaimed = new HashSet<>(xpClaimed);
      this.craftedItems = new HashSet<>(craftedItems);
      this.itemsClaimed = new HashSet<>(itemsClaimed);
      this.slate = emptySlate();

      for (int s = 0; s < 4 && s < slate.size(); s++) {
         String id = slate.get(s);
         this.slate.set(s, id == null ? "" : id);
      }

      this.rerolls = Math.max(0, Math.min(3, rerolls));
      this.pinned = pinned == null ? "" : pinned;
      this.schema = schema;
   }

   private static List<String> emptySlate() {
      List<String> fresh = new ArrayList<>(4);

      for (int s = 0; s < 4; s++) {
         fresh.add("");
      }

      return fresh;
   }

   public int progress(String questId) {
      QuestData.State s = this.states.get(questId);
      return s == null ? 0 : s.progress();
   }

   public boolean isComplete(String questId) {
      QuestData.State s = this.states.get(questId);
      return s != null && s.complete();
   }

   public int timesCompleted(String questId) {
      QuestData.State s = this.states.get(questId);
      return s == null ? 0 : s.timesCompleted();
   }

   public void setProgress(String questId, int progress) {
      this.states.put(questId, new QuestData.State(progress, this.isComplete(questId), this.timesCompleted(questId)));
   }

   public void recordCompletion(String questId, int progress) {
      this.states.put(questId, new QuestData.State(progress, true, this.timesCompleted(questId) + 1));
   }

   public boolean hasClaimedXp(String questId) {
      return this.xpClaimed.contains(questId);
   }

   public void markXpClaimed(String questId) {
      this.xpClaimed.add(questId);
   }

   public void clearMatching(Predicate<String> drop) {
      this.states.keySet().removeIf(drop);
   }

   public boolean recordCraftedItem(String itemId) {
      return this.craftedItems.size() < 128 && this.craftedItems.add(itemId);
   }

   public int uniqueCraftCount() {
      return this.craftedItems.size();
   }

   public void clearCraftedItems() {
      this.craftedItems.clear();
   }

   public List<String> slate() {
      return Collections.unmodifiableList(this.slate);
   }

   public String slot(int index) {
      return index >= 0 && index < 4 ? this.slate.get(index) : "";
   }

   public void setSlot(int index, String questId) {
      if (index >= 0 && index < 4) {
         this.slate.set(index, questId == null ? "" : questId);
      }
   }

   public int slotOf(String questId) {
      return questId != null && !questId.isEmpty() ? this.slate.indexOf(questId) : -1;
   }

   public void clearSlate() {
      for (int s = 0; s < 4; s++) {
         this.slate.set(s, "");
      }
   }

   public int rerolls() {
      return this.rerolls;
   }

   public void setRerolls(int value) {
      this.rerolls = Math.max(0, Math.min(3, value));
   }

   public boolean spendReroll() {
      if (this.rerolls <= 0) {
         return false;
      } else {
         this.rerolls--;
         return true;
      }
   }

   public void refillRerolls() {
      this.rerolls = 3;
   }

   public String pinned() {
      return this.pinned;
   }

   public void setPinned(String questId) {
      this.pinned = questId == null ? "" : questId;
   }

   public int schema() {
      return this.schema;
   }

   public void setSchema(int value) {
      this.schema = value;
   }

   public boolean hasClaimedItems(String questId) {
      return this.itemsClaimed.contains(questId);
   }

   public void markItemsClaimed(String questId) {
      this.itemsClaimed.add(questId);
   }

   public void reopen(String questId, int required) {
      QuestData.State s = this.states.get(questId);
      if (s != null) {
         if (s.complete() || s.progress() >= required) {
            this.states.put(questId, new QuestData.State(0, false, s.timesCompleted()));
         }
      }
   }

   public void clampProgress(String questId, int max) {
      QuestData.State s = this.states.get(questId);
      if (s != null && s.progress() > max) {
         this.states.put(questId, new QuestData.State(Math.max(0, max), s.complete(), s.timesCompleted()));
      }
   }

   public void clearClaimedMatching(Predicate<String> drop) {
      this.itemsClaimed.removeIf(drop);
   }

   public void pruneUnknownXpClaimed(Predicate<String> unknown) {
      this.xpClaimed.removeIf(unknown);
   }

   public static record State(int progress, boolean complete, int timesCompleted) {
      public static final Codec<QuestData.State> CODEC = RecordCodecBuilder.create(
         i -> i.group(
                  Codec.INT.optionalFieldOf("progress", 0).forGetter(QuestData.State::progress),
                  Codec.BOOL.optionalFieldOf("complete", false).forGetter(QuestData.State::complete),
                  Codec.INT.optionalFieldOf("timesCompleted", 0).forGetter(QuestData.State::timesCompleted)
               )
               .apply(i, QuestData.State::new)
      );
   }
}
