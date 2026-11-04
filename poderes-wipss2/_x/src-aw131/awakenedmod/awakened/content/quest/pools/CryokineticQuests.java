package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class CryokineticQuests {
   private CryokineticQuests() {
   }

   public static void register() {
      Quest.trial("cryokinetic_1", QuestTier.I)
         .power("cryokinetic")
         .trigger(QuestTrigger.LAND_HITS, 25)
         .mastery(55)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.SNOWBALL, 16))
         .register();
      Quest.trial("cryokinetic_2", QuestTier.I)
         .power("cryokinetic")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 15)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("cryokinetic_6", QuestTier.I)
         .power("cryokinetic")
         .obtain("minecraft:snowball", 32)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.POWDER_SNOW_BUCKET, 1))
         .register();
      Quest.trial("cryokinetic_3", QuestTier.II)
         .power("cryokinetic")
         .trigger(QuestTrigger.LAND_HITS, 60)
         .mastery(105)
         .xp(220)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("cryokinetic_7", QuestTier.II)
         .power("cryokinetic")
         .trigger(QuestTrigger.USE_SIGNATURE, 10)
         .needsMastery(5)
         .mastery(90)
         .xp(180)
         .reward(QuestRewards.item(() -> Items.ICE, 16))
         .register();
      Quest.trial("cryokinetic_4", QuestTier.II)
         .power("cryokinetic")
         .trigger(QuestTrigger.USE_SIGNATURE, 24)
         .needsMastery(5)
         .mastery(100)
         .xp(210)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("cryokinetic_8", QuestTier.II)
         .power("cryokinetic")
         .kill("awakened:suppressor", 8)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("cryokinetic_9", QuestTier.II)
         .power("cryokinetic")
         .place("biome/minecraft:ice_spikes")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("cryokinetic_10", QuestTier.II)
         .power("cryokinetic")
         .feat("cryo_shatter", 5)
         .needsMastery(5)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("cryokinetic_11", QuestTier.III)
         .power("cryokinetic")
         .kill("awakened:arbiter", 1)
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("cryokinetic_12", QuestTier.III)
         .power("cryokinetic")
         .place("structure/awakened:sunken_vault")
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("cryokinetic_13", QuestTier.III)
         .power("cryokinetic")
         .feat("cryo_shatter", 25)
         .needsMastery(5)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.goal("cryokinetic_5", QuestTier.III)
         .power("cryokinetic")
         .trigger(QuestTrigger.REACH_MASTERY, 10)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
   }
}
