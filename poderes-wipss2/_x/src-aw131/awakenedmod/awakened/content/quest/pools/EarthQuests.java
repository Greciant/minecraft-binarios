package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class EarthQuests {
   private EarthQuests() {
   }

   public static void register() {
      Quest.trial("geokinetic_1", QuestTier.I)
         .power("geokinetic")
         .trigger(QuestTrigger.USE_SIGNATURE, 12)
         .mastery(55)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 4))
         .register();
      Quest.trial("geokinetic_2", QuestTier.I)
         .power("geokinetic")
         .trigger(QuestTrigger.LAND_HITS, 25)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.EXPERIENCE_BOTTLE, 4))
         .register();
      Quest.trial("geokinetic_3", QuestTier.I)
         .power("geokinetic")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 15)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("geokinetic_4", QuestTier.I)
         .power("geokinetic")
         .obtain("minecraft:gravel", 32)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.GOLDEN_CARROT, 6))
         .register();
      Quest.trial("geokinetic_5", QuestTier.II)
         .power("geokinetic")
         .trigger(QuestTrigger.LAND_HITS, 60)
         .mastery(105)
         .xp(220)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("geokinetic_6", QuestTier.II)
         .power("geokinetic")
         .trigger(QuestTrigger.USE_SIGNATURE, 30)
         .mastery(100)
         .xp(210)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("geokinetic_8", QuestTier.II)
         .power("geokinetic")
         .place("biome/minecraft:dripstone_caves")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("geokinetic_9", QuestTier.II)
         .power("geokinetic")
         .feat("geo_crush", 4)
         .needsMastery(7)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("geokinetic_7", QuestTier.III)
         .power("geokinetic")
         .kill("minecraft:ravager", 5)
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("geokinetic_10", QuestTier.III)
         .power("geokinetic")
         .kill("awakened:archevoker", 1)
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("geokinetic_11", QuestTier.III)
         .power("geokinetic")
         .place("structure/awakened:sunken_vault")
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("geokinetic_12", QuestTier.III)
         .power("geokinetic")
         .feat("geo_crush", 20)
         .needsMastery(7)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.trial("geokinetic_13", QuestTier.III)
         .power("geokinetic")
         .feat("geo_mech_rout", 12)
         .needsMastery(10)
         .mastery(175)
         .xp(420)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("geokinetic_14", QuestTier.III)
         .power("geokinetic")
         .trigger(QuestTrigger.REACH_MASTERY, 10)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("geokinetic_15", QuestTier.III)
         .power("geokinetic")
         .feat("geo_mech_rout", 1)
         .xp(300)
         .after("geokinetic_14")
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
   }
}
