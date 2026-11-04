package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class StormCallerQuests {
   private StormCallerQuests() {
   }

   public static void register() {
      Quest.trial("storm_caller_1", QuestTier.I)
         .power("storm_caller")
         .trigger(QuestTrigger.LAND_HITS, 25)
         .mastery(55)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.LIGHTNING_ROD, 1))
         .register();
      Quest.trial("storm_caller_2", QuestTier.I)
         .power("storm_caller")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 15)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("storm_caller_6", QuestTier.I)
         .power("storm_caller")
         .obtain("minecraft:copper_ingot", 16)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.EXPERIENCE_BOTTLE, 6))
         .register();
      Quest.trial("storm_caller_7", QuestTier.I)
         .power("storm_caller")
         .trigger(QuestTrigger.USE_SIGNATURE, 10)
         .needsMastery(3)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.GOLDEN_APPLE, 1))
         .register();
      Quest.trial("storm_caller_3", QuestTier.II)
         .power("storm_caller")
         .trigger(QuestTrigger.CLUTCH_SURVIVE, 6)
         .needsMastery(5)
         .mastery(95)
         .xp(190)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("storm_caller_4", QuestTier.II)
         .power("storm_caller")
         .trigger(QuestTrigger.USE_SIGNATURE, 24)
         .needsMastery(3)
         .mastery(100)
         .xp(210)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("storm_caller_8", QuestTier.II)
         .power("storm_caller")
         .kill("minecraft:phantom", 8)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("storm_caller_9", QuestTier.II)
         .power("storm_caller")
         .place("biome/minecraft:jagged_peaks")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("storm_caller_12", QuestTier.II)
         .power("storm_caller")
         .obtain("minecraft:lightning_rod", 8)
         .mastery(95)
         .xp(200)
         .reward(QuestRewards.item(AwakenedItems.RAW_ADAMANTIUM, 3))
         .register();
      Quest.trial("storm_caller_11", QuestTier.II)
         .power("storm_caller")
         .trigger(QuestTrigger.LAND_HITS, 60)
         .mastery(105)
         .xp(220)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("storm_caller_10", QuestTier.II)
         .power("storm_caller")
         .feat("storm_thunderhead", 5)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("storm_caller_13", QuestTier.III)
         .power("storm_caller")
         .kill("awakened:archevoker", 1)
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("storm_caller_14", QuestTier.III)
         .power("storm_caller")
         .kill("minecraft:ender_dragon", 1)
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("storm_caller_15", QuestTier.III)
         .power("storm_caller")
         .feat("storm_thunderhead", 20)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.goal("storm_caller_5", QuestTier.III)
         .power("storm_caller")
         .trigger(QuestTrigger.REACH_MASTERY, 9)
         .xp(350)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("storm_caller_16", QuestTier.III)
         .power("storm_caller")
         .feat("storm_airstrike", 3)
         .xp(400)
         .after("storm_caller_5")
         .reward(QuestRewards.catalyst())
         .register();
   }
}
