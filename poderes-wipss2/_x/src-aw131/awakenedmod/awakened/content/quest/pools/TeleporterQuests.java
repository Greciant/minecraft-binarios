package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class TeleporterQuests {
   private TeleporterQuests() {
   }

   public static void register() {
      Quest.trial("teleporter_1", QuestTier.I)
         .power("teleporter")
         .trigger(QuestTrigger.USE_SIGNATURE, 15)
         .mastery(55)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.ENDER_PEARL, 8))
         .register();
      Quest.trial("teleporter_2", QuestTier.I)
         .power("teleporter")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 20)
         .mastery(75)
         .xp(140)
         .reward(QuestRewards.item(() -> Items.GOLDEN_APPLE, 1))
         .register();
      Quest.trial("teleporter_3", QuestTier.II)
         .power("teleporter")
         .trigger(QuestTrigger.LAND_HITS, 40)
         .needsMastery(3)
         .mastery(90)
         .xp(160)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("teleporter_4", QuestTier.II)
         .power("teleporter")
         .trigger(QuestTrigger.CLUTCH_SURVIVE, 6)
         .mastery(100)
         .xp(190)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.goal("teleporter_5", QuestTier.III)
         .power("teleporter")
         .trigger(QuestTrigger.REACH_MASTERY, 8)
         .xp(340)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
   }
}
