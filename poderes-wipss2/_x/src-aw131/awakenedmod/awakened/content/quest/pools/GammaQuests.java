package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class GammaQuests {
   private GammaQuests() {
   }

   public static void register() {
      Quest.trial("gamma_1", QuestTier.I)
         .power("gamma")
         .trigger(QuestTrigger.LAND_HITS, 20)
         .needsMastery(2)
         .mastery(55)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.COOKED_BEEF, 12))
         .register();
      Quest.trial("gamma_2", QuestTier.I)
         .power("gamma")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 15)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("gamma_3", QuestTier.I)
         .power("gamma")
         .obtain("minecraft:cobblestone", 64)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.EXPERIENCE_BOTTLE, 6))
         .register();
      Quest.trial("gamma_4", QuestTier.I)
         .power("gamma")
         .trigger(QuestTrigger.USE_SIGNATURE, 8)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.GOLDEN_APPLE, 1))
         .register();
      Quest.trial("gamma_5", QuestTier.II)
         .power("gamma")
         .trigger(QuestTrigger.CLUTCH_SURVIVE, 6)
         .mastery(95)
         .xp(190)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("gamma_6", QuestTier.II)
         .power("gamma")
         .trigger(QuestTrigger.USE_SIGNATURE, 20)
         .mastery(100)
         .xp(210)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("gamma_7", QuestTier.II)
         .power("gamma")
         .kill("minecraft:hoglin", 10)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("gamma_8", QuestTier.II)
         .power("gamma")
         .place("biome/minecraft:stony_peaks")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("gamma_9", QuestTier.II)
         .power("gamma")
         .feat("gamma_ragdoll", 5)
         .needsMastery(10)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("gamma_10", QuestTier.II)
         .power("gamma")
         .feat("gamma_quake", 6)
         .needsMastery(5)
         .mastery(115)
         .xp(250)
         .reward(QuestRewards.item(AwakenedItems.RAW_ADAMANTIUM, 3))
         .register();
      Quest.trial("gamma_11", QuestTier.III)
         .power("gamma")
         .kill("minecraft:wither", 1)
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("gamma_12", QuestTier.III)
         .power("gamma")
         .place("structure/minecraft:bastion_remnant")
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("gamma_13", QuestTier.III)
         .power("gamma")
         .feat("gamma_unstoppable", 5)
         .needsToggle("rising_fury")
         .mastery(175)
         .xp(410)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("gamma_14", QuestTier.III)
         .power("gamma")
         .feat("gamma_ragdoll", 18)
         .needsMastery(10)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.goal("gamma_15", QuestTier.II)
         .power("gamma")
         .trigger(QuestTrigger.REACH_MASTERY, 5)
         .xp(200)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.goal("gamma_16", QuestTier.III)
         .power("gamma")
         .trigger(QuestTrigger.REACH_MASTERY, 10)
         .xp(360)
         .after("gamma_15")
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
   }
}
