package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class SummoningQuests {
   private SummoningQuests() {
   }

   public static void register() {
      Quest.trial("summoning_1", QuestTier.I)
         .power("summoning")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 15)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("summoning_2", QuestTier.I)
         .power("summoning")
         .trigger(QuestTrigger.USE_SIGNATURE, 12)
         .mastery(55)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.EXPERIENCE_BOTTLE, 6))
         .register();
      Quest.trial("summoning_6", QuestTier.I)
         .power("summoning")
         .kill("minecraft:zombie", 15)
         .mastery(55)
         .xp(110)
         .reward(QuestRewards.item(() -> Items.NAME_TAG, 2))
         .register();
      Quest.trial("summoning_5", QuestTier.I)
         .power("summoning")
         .obtain("minecraft:rotten_flesh", 32)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.GOLDEN_APPLE, 1))
         .register();
      Quest.trial("summoning_3", QuestTier.II)
         .power("summoning")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 40)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("summoning_7", QuestTier.II)
         .power("summoning")
         .feat("summon_minion_held", 12)
         .mastery(100)
         .xp(210)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("summoning_8", QuestTier.II)
         .power("summoning")
         .kill("minecraft:evoker", 4)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("summoning_9", QuestTier.II)
         .power("summoning")
         .place("structure/minecraft:pillager_outpost")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("summoning_11", QuestTier.II)
         .power("summoning")
         .obtain("minecraft:soul_sand", 32)
         .mastery(95)
         .xp(200)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 2))
         .register();
      Quest.trial("summoning_10", QuestTier.II)
         .power("summoning")
         .feat("summon_host_kill", 10)
         .needsMastery(3)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("summoning_12", QuestTier.III)
         .power("summoning")
         .place("structure/minecraft:monument")
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("summoning_13", QuestTier.III)
         .power("summoning")
         .feat("summon_host_kill", 30)
         .needsMastery(3)
         .mastery(175)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("summoning_14", QuestTier.III)
         .power("summoning")
         .feat("summon_pilot_kill", 10)
         .needsMastery(10)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.goal("summoning_4", QuestTier.III)
         .power("summoning")
         .trigger(QuestTrigger.REACH_MASTERY, 10)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("summoning_15", QuestTier.III).power("summoning").feat("summon_apex_raise", 1).xp(400).reward(QuestRewards.catalyst()).register();
   }
}
