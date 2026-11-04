package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class ChronokineticQuests {
   private ChronokineticQuests() {
   }

   public static void register() {
      Quest.trial("chronokinetic_1", QuestTier.I)
         .power("chronokinetic")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 15)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.GOLD_INGOT, 6))
         .register();
      Quest.trial("chronokinetic_6", QuestTier.I)
         .power("chronokinetic")
         .obtain("minecraft:clock", 2)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.EXPERIENCE_BOTTLE, 6))
         .register();
      Quest.trial("chronokinetic_7", QuestTier.I)
         .power("chronokinetic")
         .trigger(QuestTrigger.LAND_HITS, 20)
         .needsMastery(5)
         .mastery(65)
         .xp(130)
         .reward(QuestRewards.item(() -> Items.GOLDEN_CARROT, 6))
         .register();
      Quest.trial("chronokinetic_8", QuestTier.II)
         .power("chronokinetic")
         .kill("minecraft:skeleton", 20)
         .mastery(105)
         .xp(230)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("chronokinetic_2", QuestTier.II)
         .power("chronokinetic")
         .trigger(QuestTrigger.CLUTCH_SURVIVE, 6)
         .needsMastery(3)
         .mastery(95)
         .xp(190)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("chronokinetic_3", QuestTier.II)
         .power("chronokinetic")
         .trigger(QuestTrigger.LAND_HITS, 50)
         .needsMastery(5)
         .mastery(105)
         .xp(220)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("chronokinetic_9", QuestTier.II)
         .power("chronokinetic")
         .kill("minecraft:vex", 12)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("chronokinetic_10", QuestTier.II)
         .power("chronokinetic")
         .place("biome/minecraft:old_growth_pine_taiga")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("chronokinetic_11", QuestTier.II)
         .power("chronokinetic")
         .feat("chrono_stasis_kill", 6)
         .needsMastery(5)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("chronokinetic_12", QuestTier.III)
         .power("chronokinetic")
         .kill("awakened:arbiter", 1)
         .mastery(175)
         .xp(410)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("chronokinetic_13", QuestTier.III)
         .power("chronokinetic")
         .place("structure/minecraft:trail_ruins")
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("chronokinetic_14", QuestTier.III)
         .power("chronokinetic")
         .feat("chrono_stasis_kill", 20)
         .needsMastery(5)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.trial("chronokinetic_15", QuestTier.III)
         .power("chronokinetic")
         .feat("chrono_stopped_execution", 6)
         .needsMastery(10)
         .mastery(175)
         .xp(420)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("chronokinetic_4", QuestTier.III)
         .power("chronokinetic")
         .trigger(QuestTrigger.REACH_MASTERY, 10)
         .xp(350)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("chronokinetic_5", QuestTier.III)
         .power("chronokinetic")
         .feat("chrono_stopped_execution", 1)
         .xp(300)
         .after("chronokinetic_4")
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
   }
}
