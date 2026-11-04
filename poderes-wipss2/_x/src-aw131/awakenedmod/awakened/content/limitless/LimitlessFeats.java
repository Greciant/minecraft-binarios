package com.awakenedmod.awakened.content.limitless;

import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.Collection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class LimitlessFeats {
   public static final String RED_COMBO = "limitless_red_combo";
   public static final String PURPLE_CROWD = "limitless_purple_crowd";

   private LimitlessFeats() {
   }

   public static void comboLanded(ServerPlayer caster, Collection<LivingEntity> hit) {
      if (threats(caster, hit) >= 3) {
         QuestSystem.feat(caster, "limitless_red_combo");
      }
   }

   public static int purpleStruck(ServerPlayer caster, int before, Collection<LivingEntity> struck) {
      int after = before + threats(caster, struck);
      if (before < 3 && after >= 3) {
         QuestSystem.feat(caster, "limitless_purple_crowd");
      }

      return after;
   }

   private static int threats(ServerPlayer caster, Collection<LivingEntity> bodies) {
      int n = 0;

      for (LivingEntity body : bodies) {
         if (Foes.countsAsKill(caster, body)) {
            n++;
         }
      }

      return n;
   }
}
