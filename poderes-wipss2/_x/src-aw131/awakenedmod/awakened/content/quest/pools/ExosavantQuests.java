package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class ExosavantQuests {
   private ExosavantQuests() {
   }

   public static void register() {
      Quest.trial("exosavant_1", QuestTier.I)
         .power("exosavant")
         .trigger(QuestTrigger.LAND_HITS, 25)
         .mastery(55)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.REDSTONE, 24))
         .register();
      Quest.trial("exosavant_2", QuestTier.I)
         .power("exosavant")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 15)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 6))
         .register();
      Quest.trial("exosavant_3", QuestTier.I)
         .power("exosavant")
         .obtain("minecraft:gunpowder", 8)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.IRON_BLOCK, 2))
         .register();
      Quest.trial("exosavant_4", QuestTier.I)
         .power("exosavant")
         .obtain("awakened:exosavant_gauntlets", 1)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.COPPER_INGOT, 16))
         .register();
      Quest.trial("exosavant_5", QuestTier.II)
         .power("exosavant")
         .trigger(QuestTrigger.CLUTCH_SURVIVE, 6)
         .mastery(95)
         .xp(190)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("exosavant_6", QuestTier.II)
         .power("exosavant")
         .trigger(QuestTrigger.USE_SIGNATURE, 24)
         .mastery(100)
         .xp(210)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("exosavant_7", QuestTier.II)
         .power("exosavant")
         .kill("minecraft:blaze", 12)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("exosavant_8", QuestTier.II)
         .power("exosavant")
         .place("structure/minecraft:fortress")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("exosavant_9", QuestTier.II)
         .power("exosavant")
         .feat("exo_marked_kill", 6)
         .needsMastery(7)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("exosavant_10", QuestTier.II)
         .power("exosavant")
         .obtain("minecraft:amethyst_shard", 12)
         .mastery(100)
         .xp(210)
         .reward(QuestRewards.item(AwakenedItems.RAW_ADAMANTIUM, 3))
         .register();
      Quest.trial("exosavant_11", QuestTier.III)
         .power("exosavant")
         .kill("awakened:archevoker", 1)
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("exosavant_12", QuestTier.III)
         .power("exosavant")
         .place("structure/minecraft:bastion_remnant")
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("exosavant_13", QuestTier.III)
         .power("exosavant")
         .feat("exo_air_kill", 15)
         .needsMastery(8)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.trial("exosavant_14", QuestTier.III)
         .power("exosavant")
         .obtain("minecraft:netherite_ingot", 2)
         .mastery(160)
         .xp(380)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("exosavant_15", QuestTier.II)
         .power("exosavant")
         .trigger(QuestTrigger.REACH_MASTERY, 8)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.goal("exosavant_16", QuestTier.III)
         .power("exosavant")
         .trigger(QuestTrigger.REACH_MASTERY, 13)
         .xp(380)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .after("exosavant_15")
         .register();
   }
}
