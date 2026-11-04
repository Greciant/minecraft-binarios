package com.awakenedmod.awakened.core.quest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

public final class QuestRegistry {
   private static final Map<String, Quest> BY_ID = new LinkedHashMap<>();
   private static final Set<String> SEEN_THIS_PASS = new HashSet<>();

   private QuestRegistry() {
   }

   public static void beginRegistration() {
      SEEN_THIS_PASS.clear();
   }

   public static void register(Quest quest) {
      if (!SEEN_THIS_PASS.add(quest.id())) {
         Quest existing = BY_ID.get(quest.id());
         if (existing != null && !existing.equals(quest)) {
            throw new IllegalStateException("Duplicate quest id: " + quest.id());
         }
      }

      BY_ID.put(quest.id(), quest);
   }

   @Nullable
   public static Quest byId(String id) {
      return BY_ID.get(id);
   }

   public static List<Quest> all() {
      return List.copyOf(BY_ID.values());
   }

   public static List<Quest> goalsFor(@Nullable String powerId) {
      return section(powerId, true);
   }

   public static List<Quest> trialsFor(@Nullable String powerId) {
      return section(powerId, false);
   }

   private static List<Quest> section(@Nullable String powerId, boolean goals) {
      List<Quest> out = new ArrayList<>();

      for (Quest q : BY_ID.values()) {
         if (q.goal() == goals && q.availableTo(powerId)) {
            out.add(q);
         }
      }

      return out;
   }
}
