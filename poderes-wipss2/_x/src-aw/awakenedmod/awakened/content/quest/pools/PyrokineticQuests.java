package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class PyrokineticQuests {
   private PyrokineticQuests() {
   }

   public static void register() {
      Quest.trial("pyrokinetic_1", QuestTier.I)
         .power("pyrokinetic")
         .trigger(QuestTrigger.LAND_HITS, 25)
         .mastery(55)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.COAL, 16))
         .register();
      Quest.trial("pyrokinetic_2", QuestTier.I)
         .power("pyrokinetic")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 15)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("pyrokinetic_6", QuestTier.I)
         .power("pyrokinetic")
         .obtain("minecraft:coal", 32)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.LAVA_BUCKET, 1))
         .register();
      Quest.trial("pyrokinetic_7", QuestTier.I)
         .power("pyrokinetic")
         .kill("minecraft:creeper", 10)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.TNT, 2))
         .register();
      Quest.trial("pyrokinetic_3", QuestTier.II)
         .power("pyrokinetic")
         .trigger(QuestTrigger.LAND_HITS, 60)
         .mastery(105)
         .xp(220)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("pyrokinetic_4", QuestTier.II)
         .power("pyrokinetic")
         .trigger(QuestTrigger.USE_SIGNATURE, 20)
         .needsMastery(3)
         .mastery(100)
         .xp(210)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("pyrokinetic_8", QuestTier.II)
         .power("pyrokinetic")
         .kill("awakened:suppressor", 8)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("pyrokinetic_9", QuestTier.II)
         .power("pyrokinetic")
         .place("biome/minecraft:basalt_deltas")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("pyrokinetic_11", QuestTier.II)
         .power("pyrokinetic")
         .obtain("minecraft:blaze_rod", 4)
         .mastery(95)
         .xp(200)
         .reward(QuestRewards.item(AwakenedItems.RAW_ADAMANTIUM, 3))
         .register();
      Quest.trial("pyrokinetic_10", QuestTier.II)
         .power("pyrokinetic")
         .feat("pyro_full_burn", 6)
         .needsMastery(3)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("pyrokinetic_12", QuestTier.III)
         .power("pyrokinetic")
         .place("structure/minecraft:mansion")
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("pyrokinetic_13", QuestTier.III)
         .power("pyrokinetic")
         .place("structure/minecraft:bastion_remnant")
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("pyrokinetic_16", QuestTier.III)
         .power("pyrokinetic")
         .feat("pyro_backdraft_blast", 6)
         .needsMastery(10)
         .mastery(175)
         .xp(410)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("pyrokinetic_14", QuestTier.III)
         .power("pyrokinetic")
         .feat("pyro_full_burn", 20)
         .needsMastery(3)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.goal("pyrokinetic_15", QuestTier.II)
         .power("pyrokinetic")
         .trigger(QuestTrigger.REACH_MASTERY, 5)
         .xp(200)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.goal("pyrokinetic_5", QuestTier.III)
         .power("pyrokinetic")
         .trigger(QuestTrigger.REACH_MASTERY, 10)
         .xp(360)
         .after("pyrokinetic_15")
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
   }
}
