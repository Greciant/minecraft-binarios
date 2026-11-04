package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class NuclearQuests {
   private NuclearQuests() {
   }

   public static void register() {
      Quest.trial("nuclear_1", QuestTier.I)
         .power("nuclear")
         .trigger(QuestTrigger.LAND_HITS, 25)
         .mastery(55)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.GUNPOWDER, 12))
         .register();
      Quest.trial("nuclear_2", QuestTier.I)
         .power("nuclear")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 18)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("nuclear_6", QuestTier.I)
         .power("nuclear")
         .obtain("minecraft:tnt", 2)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.EXPERIENCE_BOTTLE, 8))
         .register();
      Quest.trial("nuclear_7", QuestTier.I)
         .power("nuclear")
         .trigger(QuestTrigger.USE_SIGNATURE, 10)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.GOLDEN_APPLE, 1))
         .register();
      Quest.trial("nuclear_3", QuestTier.II)
         .power("nuclear")
         .trigger(QuestTrigger.USE_SIGNATURE, 24)
         .mastery(100)
         .xp(210)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("nuclear_4", QuestTier.II)
         .power("nuclear")
         .trigger(QuestTrigger.LAND_HITS, 60)
         .mastery(105)
         .xp(220)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("nuclear_8", QuestTier.II)
         .power("nuclear")
         .kill("minecraft:magma_cube", 20)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("nuclear_9", QuestTier.II)
         .power("nuclear")
         .place("biome/minecraft:eroded_badlands")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("nuclear_10", QuestTier.II)
         .power("nuclear")
         .feat("nuclear_cluster_sweep", 5)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("nuclear_11", QuestTier.II)
         .power("nuclear")
         .obtain("minecraft:end_crystal", 2)
         .mastery(105)
         .xp(230)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("nuclear_12", QuestTier.III)
         .power("nuclear")
         .kill("minecraft:wither", 1)
         .mastery(175)
         .xp(410)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("nuclear_13", QuestTier.III)
         .power("nuclear")
         .feat("nuclear_cluster_sweep", 25)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.trial("nuclear_14", QuestTier.III)
         .power("nuclear")
         .feat("nuclear_direct_hit", 12)
         .needsMastery(6)
         .mastery(175)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("nuclear_15", QuestTier.III)
         .power("nuclear")
         .place("biome/minecraft:mushroom_fields")
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("nuclear_5", QuestTier.III)
         .power("nuclear")
         .trigger(QuestTrigger.REACH_MASTERY, 11)
         .xp(370)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("nuclear_16", QuestTier.III)
         .power("nuclear")
         .feat("nuclear_ground_zero", 1)
         .xp(400)
         .after("nuclear_5")
         .reward(QuestRewards.catalyst())
         .register();
   }
}
