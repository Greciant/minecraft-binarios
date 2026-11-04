package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class RuinFeats {
   public static final String ROT_FEAT = "ruin_rot";
   public static final String DEEP_FEAT = "ruin_deep";
   public static final String SLAM_FEAT = "ruin_slam";
   public static final String COLLAPSE_FEAT = "ruin_collapse";

   private RuinFeats() {
   }

   static void died(LivingEntity body, DamageSource source) {
      if (source.is(RuinStacks.DECAY_TYPE) && source.getEntity() instanceof ServerPlayer caster && Foes.defeatedBy(body, caster)) {
         QuestSystem.feat(caster, "ruin_rot");
      }
   }

   static void stacked(ServerPlayer caster, LivingEntity body, int before, int after) {
      if (before < 20 && after >= 20 && Foes.countsAsKill(caster, body)) {
         QuestSystem.feat(caster, "ruin_deep");
      }
   }

   static void slammed(ServerPlayer caster, LivingEntity body) {
      if (Foes.countsAsKill(caster, body)) {
         QuestSystem.feat(caster, "ruin_slam");
      }
   }

   static void collapsed(ServerPlayer caster, List<LivingEntity> caught) {
      if (caught.stream().filter(b -> Foes.countsAsKill(caster, b)).count() >= 3L) {
         QuestSystem.feat(caster, "ruin_collapse");
      }
   }
}
