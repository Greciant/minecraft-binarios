package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class SunQuests {
   private SunQuests() {
   }

   public static void register() {
      Quest.trial("sun_1", QuestTier.I)
         .power("sun")
         .trigger(QuestTrigger.USE_SIGNATURE, 10)
         .mastery(55)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.GLOWSTONE, 4))
         .register();
      Quest.trial("sun_2", QuestTier.I)
         .power("sun")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 15)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("sun_6", QuestTier.I)
         .power("sun")
         .obtain("minecraft:gold_ingot", 16)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.GOLDEN_APPLE, 1))
         .register();
      Quest.trial("sun_7", QuestTier.I)
         .power("sun")
         .trigger(QuestTrigger.TRAVEL_DISTANCE, 3000)
         .mastery(55)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.EXPERIENCE_BOTTLE, 6))
         .register();
      Quest.trial("sun_4", QuestTier.II)
         .power("sun")
         .trigger(QuestTrigger.USE_SIGNATURE, 24)
         .mastery(100)
         .xp(210)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("sun_8", QuestTier.II)
         .power("sun")
         .obtain("minecraft:gold_block", 12)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("sun_9", QuestTier.II)
         .power("sun")
         .kill("minecraft:husk", 12)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("sun_10", QuestTier.II)
         .power("sun")
         .place("biome/minecraft:desert")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("sun_14", QuestTier.II)
         .power("sun")
         .trigger(QuestTrigger.LAND_HITS, 40)
         .needsMastery(7)
         .mastery(115)
         .xp(250)
         .reward(QuestRewards.item(AwakenedItems.RAW_ADAMANTIUM, 3))
         .register();
      Quest.trial("sun_13", QuestTier.II)
         .power("sun")
         .trigger(QuestTrigger.SURVIVE_NIGHTS, 4)
         .mastery(95)
         .xp(190)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("sun_11", QuestTier.II)
         .power("sun")
         .feat("sun_zenith", 3)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("sun_12", QuestTier.II)
         .power("sun")
         .feat("sun_pinned_down", 5)
         .needsMastery(7)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("sun_15", QuestTier.III)
         .power("sun")
         .kill("minecraft:wither", 1)
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("sun_16", QuestTier.III)
         .power("sun")
         .place("structure/minecraft:desert_pyramid")
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("sun_18", QuestTier.III)
         .power("sun")
         .feat("sun_pinned_down", 20)
         .needsMastery(7)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.goal("sun_19", QuestTier.II)
         .power("sun")
         .trigger(QuestTrigger.REACH_MASTERY, 7)
         .xp(200)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.goal("sun_5", QuestTier.III)
         .power("sun")
         .trigger(QuestTrigger.REACH_MASTERY, 10)
         .xp(360)
         .after("sun_19")
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("sun_17", QuestTier.III)
         .power("sun")
         .obtain("awakened:blank_sun_axe", 1)
         .xp(380)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("sun_20", QuestTier.III).power("sun").obtain("awakened:sun_axe", 1).xp(400).after("sun_17").reward(QuestRewards.catalyst()).register();
   }
}
