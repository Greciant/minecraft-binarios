package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class LimitlessQuests {
   private LimitlessQuests() {
   }

   public static void register() {
      Quest.trial("limitless_1", QuestTier.I)
         .power("limitless")
         .trigger(QuestTrigger.LAND_HITS, 25)
         .mastery(55)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.ENDER_PEARL, 8))
         .register();
      Quest.trial("limitless_2", QuestTier.I)
         .power("limitless")
         .trigger(QuestTrigger.CLUTCH_SURVIVE, 6)
         .needsToggle("limitless_infinity")
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("limitless_3", QuestTier.I)
         .power("limitless")
         .obtain("minecraft:lapis_lazuli", 32)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.GOLDEN_APPLE, 1))
         .register();
      Quest.trial("limitless_4", QuestTier.I)
         .power("limitless")
         .kill("minecraft:zombie", 15)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.EXPERIENCE_BOTTLE, 8))
         .register();
      Quest.trial("limitless_5", QuestTier.II)
         .power("limitless")
         .trigger(QuestTrigger.USE_SIGNATURE, 24)
         .mastery(100)
         .xp(210)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("limitless_6", QuestTier.II)
         .power("limitless")
         .trigger(QuestTrigger.LAND_HITS, 60)
         .mastery(105)
         .xp(220)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("limitless_7", QuestTier.II)
         .power("limitless")
         .kill("minecraft:skeleton", 15)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("limitless_8", QuestTier.II)
         .power("limitless")
         .kill("minecraft:enderman", 10)
         .mastery(115)
         .xp(250)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("limitless_9", QuestTier.II)
         .power("limitless")
         .place("biome/minecraft:cherry_grove")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("limitless_10", QuestTier.II)
         .power("limitless")
         .feat("limitless_red_combo", 5)
         .needsMastery(2)
         .mastery(115)
         .xp(250)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("limitless_11", QuestTier.II)
         .power("limitless")
         .feat("limitless_purple_crowd", 4)
         .needsMastery(5)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("limitless_12", QuestTier.III)
         .power("limitless")
         .place("structure/minecraft:ancient_city")
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("limitless_13", QuestTier.III)
         .power("limitless")
         .feat("limitless_purple_crowd", 10)
         .needsMastery(5)
         .mastery(175)
         .xp(410)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("limitless_14", QuestTier.III)
         .power("limitless")
         .feat("limitless_red_combo", 20)
         .needsMastery(2)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.goal("limitless_15", QuestTier.II)
         .power("limitless")
         .trigger(QuestTrigger.REACH_MASTERY, 5)
         .xp(200)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.goal("limitless_16", QuestTier.III)
         .power("limitless")
         .trigger(QuestTrigger.REACH_MASTERY, 10)
         .xp(360)
         .after("limitless_15")
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
   }
}
