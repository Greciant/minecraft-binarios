package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class KineticQuests {
   private static final String POWER = "kinetic_charger";

   private KineticQuests() {
   }

   public static void register() {
      Quest.trial("kinetic_1", QuestTier.I)
         .power("kinetic_charger")
         .trigger(QuestTrigger.LAND_HITS, 25)
         .mastery(55)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("kinetic_2", QuestTier.I)
         .power("kinetic_charger")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 15)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.EXPERIENCE_BOTTLE, 4))
         .register();
      Quest.trial("kinetic_3", QuestTier.I)
         .power("kinetic_charger")
         .obtain("minecraft:dirt", 64)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.GOLDEN_CARROT, 6))
         .register();
      Quest.trial("kinetic_4", QuestTier.I)
         .power("kinetic_charger")
         .trigger(QuestTrigger.TRAVEL_DISTANCE, 3000)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.EMERALD, 3))
         .register();
      Quest.trial("kinetic_5", QuestTier.II)
         .power("kinetic_charger")
         .trigger(QuestTrigger.LAND_HITS, 60)
         .mastery(105)
         .xp(220)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("kinetic_6", QuestTier.II)
         .power("kinetic_charger")
         .trigger(QuestTrigger.USE_SIGNATURE, 24)
         .mastery(100)
         .xp(210)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("kinetic_7", QuestTier.II)
         .power("kinetic_charger")
         .kill("minecraft:pillager", 15)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("kinetic_8", QuestTier.II)
         .power("kinetic_charger")
         .place("biome/minecraft:savanna")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("kinetic_9", QuestTier.II)
         .power("kinetic_charger")
         .feat("kinetic_full_hurl", 8)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("kinetic_10", QuestTier.II)
         .power("kinetic_charger")
         .feat("kinetic_rupture_launch", 3)
         .needsMastery(7)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("kinetic_11", QuestTier.III)
         .power("kinetic_charger")
         .kill("minecraft:breeze", 10)
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("kinetic_12", QuestTier.III)
         .power("kinetic_charger")
         .place("structure/minecraft:ancient_city")
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("kinetic_13", QuestTier.III)
         .power("kinetic_charger")
         .feat("kinetic_full_hurl", 30)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.trial("kinetic_14", QuestTier.III)
         .power("kinetic_charger")
         .feat("kinetic_rupture_launch", 8)
         .needsMastery(7)
         .mastery(175)
         .xp(420)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("kinetic_15", QuestTier.III)
         .power("kinetic_charger")
         .trigger(QuestTrigger.REACH_MASTERY, 9)
         .xp(350)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("kinetic_16", QuestTier.III)
         .power("kinetic_charger")
         .feat("kinetic_rupture_launch", 1)
         .xp(300)
         .after("kinetic_15")
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
   }
}
