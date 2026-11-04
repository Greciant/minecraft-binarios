package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class AeroQuests {
   private AeroQuests() {
   }

   public static void register() {
      Quest.trial("aerokinetic_1", QuestTier.I)
         .power("aerokinetic")
         .trigger(QuestTrigger.USE_SIGNATURE, 10)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.FEATHER, 16))
         .register();
      Quest.trial("aerokinetic_2", QuestTier.I)
         .power("aerokinetic")
         .trigger(QuestTrigger.LAND_HITS, 15)
         .mastery(55)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("aerokinetic_3", QuestTier.I)
         .power("aerokinetic")
         .obtain("minecraft:feather", 32)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.GOLDEN_APPLE, 1))
         .register();
      Quest.trial("aerokinetic_4", QuestTier.I)
         .power("aerokinetic")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 15)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.GOLD_INGOT, 6))
         .register();
      Quest.trial("aerokinetic_5", QuestTier.II)
         .power("aerokinetic")
         .feat("aero_deflect", 5)
         .mastery(95)
         .xp(190)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("aerokinetic_6", QuestTier.II)
         .power("aerokinetic")
         .kill("minecraft:phantom", 8)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("aerokinetic_7", QuestTier.II)
         .power("aerokinetic")
         .trigger(QuestTrigger.TRAVEL_DISTANCE, 12000)
         .mastery(100)
         .xp(200)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("aerokinetic_8", QuestTier.II)
         .power("aerokinetic")
         .place("biome/minecraft:windswept_hills")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("aerokinetic_9", QuestTier.II)
         .power("aerokinetic")
         .trigger(QuestTrigger.USE_SIGNATURE, 24)
         .mastery(100)
         .xp(210)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("aerokinetic_10", QuestTier.II)
         .power("aerokinetic")
         .obtain("minecraft:phantom_membrane", 8)
         .mastery(105)
         .xp(220)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("aerokinetic_11", QuestTier.II)
         .power("aerokinetic")
         .feat("aero_fling_group", 3)
         .needsMastery(7)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("aerokinetic_12", QuestTier.III)
         .power("aerokinetic")
         .kill("minecraft:breeze", 10)
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("aerokinetic_13", QuestTier.III)
         .power("aerokinetic")
         .feat("aero_suppress", 30)
         .needsMastery(10)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.trial("aerokinetic_14", QuestTier.III)
         .power("aerokinetic")
         .feat("aero_fling_group", 12)
         .needsMastery(7)
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("aerokinetic_15", QuestTier.III)
         .power("aerokinetic")
         .feat("aero_pierce", 15)
         .needsMastery(12)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("aerokinetic_16", QuestTier.III)
         .power("aerokinetic")
         .trigger(QuestTrigger.REACH_MASTERY, 10)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("aerokinetic_17", QuestTier.III)
         .power("aerokinetic")
         .trigger(QuestTrigger.REACH_MASTERY, 12)
         .xp(400)
         .after("aerokinetic_16")
         .reward(QuestRewards.catalyst())
         .register();
   }
}
