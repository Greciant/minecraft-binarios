package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class HydrokineticQuests {
   private HydrokineticQuests() {
   }

   public static void register() {
      Quest.trial("hydrokinetic_1", QuestTier.I)
         .power("hydrokinetic")
         .trigger(QuestTrigger.LAND_HITS, 25)
         .mastery(55)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.WATER_BUCKET, 2))
         .register();
      Quest.trial("hydrokinetic_2", QuestTier.I)
         .power("hydrokinetic")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 18)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("hydrokinetic_3", QuestTier.I)
         .power("hydrokinetic")
         .place("biome/minecraft:river")
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.EXPERIENCE_BOTTLE, 8))
         .register();
      Quest.trial("hydrokinetic_6", QuestTier.I)
         .power("hydrokinetic")
         .obtain("awakened:water_canteen", 1)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.PRISMARINE_SHARD, 4))
         .register();
      Quest.trial("hydrokinetic_4", QuestTier.II)
         .power("hydrokinetic")
         .trigger(QuestTrigger.LAND_HITS, 60)
         .mastery(105)
         .xp(220)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("hydrokinetic_7", QuestTier.II)
         .power("hydrokinetic")
         .trigger(QuestTrigger.USE_SIGNATURE, 24)
         .mastery(100)
         .xp(210)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("hydrokinetic_8", QuestTier.II)
         .power("hydrokinetic")
         .kill("minecraft:skeleton", 25)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("hydrokinetic_9", QuestTier.II)
         .power("hydrokinetic")
         .place("biome/minecraft:warm_ocean")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("hydrokinetic_10", QuestTier.II)
         .power("hydrokinetic")
         .feat("hydro_skewer", 4)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("hydrokinetic_11", QuestTier.II)
         .power("hydrokinetic")
         .feat("hydro_drown_hold", 5)
         .needsMastery(7)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("hydrokinetic_12", QuestTier.III)
         .power("hydrokinetic")
         .place("structure/minecraft:monument")
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("hydrokinetic_13", QuestTier.III)
         .power("hydrokinetic")
         .kill("minecraft:guardian", 15)
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("hydrokinetic_14", QuestTier.III)
         .power("hydrokinetic")
         .feat("hydro_skewer", 15)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.goal("hydrokinetic_5", QuestTier.III)
         .power("hydrokinetic")
         .trigger(QuestTrigger.REACH_MASTERY, 9)
         .xp(350)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("hydrokinetic_15", QuestTier.III)
         .power("hydrokinetic")
         .feat("hydro_wave_rout", 3)
         .xp(400)
         .after("hydrokinetic_5")
         .reward(QuestRewards.catalyst())
         .register();
   }
}
