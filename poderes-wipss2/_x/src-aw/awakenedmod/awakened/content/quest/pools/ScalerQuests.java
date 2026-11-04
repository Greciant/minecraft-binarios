package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class ScalerQuests {
   private ScalerQuests() {
   }

   public static void register() {
      Quest.trial("scaler_1", QuestTier.I)
         .power("scaler")
         .trigger(QuestTrigger.USE_SIGNATURE, 15)
         .mastery(55)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.EXPERIENCE_BOTTLE, 4))
         .register();
      Quest.trial("scaler_2", QuestTier.I)
         .power("scaler")
         .trigger(QuestTrigger.LAND_HITS, 25)
         .mastery(60)
         .xp(110)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("scaler_3", QuestTier.I)
         .power("scaler")
         .trigger(QuestTrigger.CLUTCH_SURVIVE, 3)
         .mastery(65)
         .xp(130)
         .reward(QuestRewards.item(() -> Items.GOLDEN_APPLE, 1))
         .register();
      Quest.trial("scaler_6", QuestTier.I)
         .power("scaler")
         .obtain("awakened:containment_jar", 3)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.SLIME_BALL, 8))
         .register();
      Quest.trial("scaler_7", QuestTier.II)
         .power("scaler")
         .trigger(QuestTrigger.LAND_HITS, 70)
         .mastery(105)
         .xp(220)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("scaler_4", QuestTier.II)
         .power("scaler")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 40)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("scaler_8", QuestTier.II)
         .power("scaler")
         .kill("minecraft:slime", 20)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("scaler_9", QuestTier.II)
         .power("scaler")
         .place("biome/minecraft:lush_caves")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("scaler_10", QuestTier.II)
         .power("scaler")
         .feat("scaler_stomp_crowd", 6)
         .needsMastery(6)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("scaler_11", QuestTier.II)
         .power("scaler")
         .feat("scaler_shrunk_kill", 5)
         .needsMastery(9)
         .mastery(125)
         .xp(270)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("scaler_12", QuestTier.III)
         .power("scaler")
         .place("structure/minecraft:nether_fossil")
         .mastery(155)
         .xp(370)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("scaler_13", QuestTier.III)
         .power("scaler")
         .kill("minecraft:piglin_brute", 8)
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("scaler_14", QuestTier.III)
         .power("scaler")
         .feat("scaler_shrunk_kill", 25)
         .needsMastery(9)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.trial("scaler_15", QuestTier.III)
         .power("scaler")
         .feat("scaler_clamber_kill", 10)
         .needsMastery(13)
         .mastery(175)
         .xp(410)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("scaler_5", QuestTier.III)
         .power("scaler")
         .trigger(QuestTrigger.REACH_MASTERY, 9)
         .xp(350)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("scaler_16", QuestTier.III)
         .power("scaler")
         .trigger(QuestTrigger.REACH_MASTERY, 14)
         .xp(420)
         .after("scaler_5")
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
   }
}
