package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class PhytokineticQuests {
   private PhytokineticQuests() {
   }

   public static void register() {
      Quest.trial("phytokinetic_1", QuestTier.I)
         .power("phytokinetic")
         .trigger(QuestTrigger.LAND_HITS, 25)
         .mastery(55)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.BONE_MEAL, 16))
         .register();
      Quest.trial("phytokinetic_2", QuestTier.I)
         .power("phytokinetic")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 15)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("phytokinetic_3", QuestTier.I)
         .power("phytokinetic")
         .obtain("minecraft:bone_meal", 32)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.MOSS_BLOCK, 8))
         .register();
      Quest.trial("phytokinetic_4", QuestTier.I)
         .power("phytokinetic")
         .trigger(QuestTrigger.USE_SIGNATURE, 10)
         .needsMastery(3)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.GOLDEN_APPLE, 1))
         .register();
      Quest.trial("phytokinetic_5", QuestTier.II)
         .power("phytokinetic")
         .trigger(QuestTrigger.CLUTCH_SURVIVE, 6)
         .needsMastery(5)
         .mastery(95)
         .xp(190)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("phytokinetic_6", QuestTier.II)
         .power("phytokinetic")
         .trigger(QuestTrigger.USE_SIGNATURE, 24)
         .needsMastery(3)
         .mastery(100)
         .xp(210)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("phytokinetic_7", QuestTier.II)
         .power("phytokinetic")
         .kill("minecraft:bogged", 20)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("phytokinetic_8", QuestTier.II)
         .power("phytokinetic")
         .place("biome/minecraft:sunflower_plains")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("phytokinetic_9", QuestTier.II)
         .power("phytokinetic")
         .feat("phyto_root_kill", 5)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("phytokinetic_10", QuestTier.II)
         .power("phytokinetic")
         .trigger(QuestTrigger.LAND_HITS, 60)
         .mastery(105)
         .xp(220)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("phytokinetic_11", QuestTier.III)
         .power("phytokinetic")
         .kill("awakened:archevoker", 1)
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("phytokinetic_12", QuestTier.III)
         .power("phytokinetic")
         .place("structure/minecraft:jungle_pyramid")
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("phytokinetic_13", QuestTier.III)
         .power("phytokinetic")
         .feat("phyto_root_kill", 25)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.goal("phytokinetic_14", QuestTier.III)
         .power("phytokinetic")
         .trigger(QuestTrigger.REACH_MASTERY, 10)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("phytokinetic_15", QuestTier.III)
         .power("phytokinetic")
         .feat("phyto_cradle_save", 3)
         .xp(400)
         .after("phytokinetic_14")
         .reward(QuestRewards.catalyst())
         .register();
   }
}
