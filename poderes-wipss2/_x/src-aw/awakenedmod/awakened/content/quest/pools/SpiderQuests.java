package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class SpiderQuests {
   private SpiderQuests() {
   }

   public static void register() {
      Quest.trial("spider_1", QuestTier.I)
         .power("spider")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 12)
         .mastery(55)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.STRING, 16))
         .register();
      Quest.trial("spider_2", QuestTier.I)
         .power("spider")
         .obtain("minecraft:string", 32)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.SLIME_BALL, 3))
         .register();
      Quest.trial("spider_3", QuestTier.I)
         .power("spider")
         .trigger(QuestTrigger.TRAVEL_DISTANCE, 3000)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("spider_4", QuestTier.I)
         .power("spider")
         .kill("minecraft:spider", 10)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.REDSTONE, 12))
         .register();
      Quest.trial("spider_5", QuestTier.II)
         .power("spider")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 60)
         .mastery(140)
         .xp(300)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("spider_6", QuestTier.II)
         .power("spider")
         .trigger(QuestTrigger.LAND_HITS, 20)
         .needsMastery(7)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("spider_7", QuestTier.II)
         .power("spider")
         .kill("awakened:suppressor", 8)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("spider_8", QuestTier.II)
         .power("spider")
         .place("biome/minecraft:jungle")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("spider_9", QuestTier.II)
         .power("spider")
         .obtain("awakened:web_gauntlets", 1)
         .mastery(120)
         .xp(250)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("spider_10", QuestTier.II)
         .power("spider")
         .feat("web_momentum_strike", 6)
         .needsMastery(7)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("spider_11", QuestTier.II)
         .power("spider")
         .feat("web_full_wrap", 10)
         .needsMastery(3)
         .mastery(115)
         .xp(240)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("spider_12", QuestTier.III)
         .power("spider")
         .kill("awakened:arbiter", 1)
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("spider_13", QuestTier.III)
         .power("spider")
         .feat("web_full_wrap", 35)
         .needsMastery(3)
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("spider_14", QuestTier.III)
         .power("spider")
         .feat("web_momentum_strike", 30)
         .needsMastery(7)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.goal("spider_15", QuestTier.II)
         .power("spider")
         .trigger(QuestTrigger.REACH_MASTERY, 4)
         .xp(200)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.goal("spider_16", QuestTier.III)
         .power("spider")
         .trigger(QuestTrigger.REACH_MASTERY, 10)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
   }
}
