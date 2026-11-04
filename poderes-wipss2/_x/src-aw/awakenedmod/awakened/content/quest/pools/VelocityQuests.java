package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class VelocityQuests {
   private VelocityQuests() {
   }

   public static void register() {
      Quest.trial("velocity_1", QuestTier.I)
         .power("velocity")
         .trigger(QuestTrigger.TRAVEL_DISTANCE, 5000)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.EMERALD, 3))
         .register();
      Quest.trial("velocity_2", QuestTier.I)
         .power("velocity")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 15)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.EXPERIENCE_BOTTLE, 4))
         .register();
      Quest.trial("velocity_6", QuestTier.I)
         .power("velocity")
         .obtain("minecraft:cooked_beef", 64)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.GOLDEN_CARROT, 6))
         .register();
      Quest.trial("velocity_7", QuestTier.I)
         .power("velocity")
         .feat("velocity_redline_10", 3)
         .needsMastery(8)
         .mastery(65)
         .xp(130)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("velocity_3", QuestTier.II)
         .power("velocity")
         .trigger(QuestTrigger.TRAVEL_DISTANCE, 25000)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("velocity_4", QuestTier.II)
         .power("velocity")
         .trigger(QuestTrigger.LAND_HITS, 40)
         .needsMastery(5)
         .mastery(105)
         .xp(220)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("velocity_8", QuestTier.II)
         .power("velocity")
         .kill("minecraft:pillager", 15)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("velocity_9", QuestTier.II)
         .power("velocity")
         .place("biome/minecraft:savanna")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("velocity_10", QuestTier.II)
         .power("velocity")
         .feat("velocity_redline_10", 12)
         .needsMastery(8)
         .mastery(115)
         .xp(250)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("velocity_11", QuestTier.II)
         .power("velocity")
         .feat("velocity_full_bolt", 8)
         .needsMastery(5)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("velocity_12", QuestTier.III)
         .power("velocity")
         .kill("minecraft:breeze", 10)
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("velocity_13", QuestTier.III)
         .power("velocity")
         .place("structure/minecraft:ancient_city")
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("velocity_14", QuestTier.III)
         .power("velocity")
         .feat("velocity_full_bolt", 30)
         .needsMastery(5)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.trial("velocity_15", QuestTier.III)
         .power("velocity")
         .feat("velocity_tunnel_300", 12)
         .needsMastery(9)
         .mastery(175)
         .xp(420)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("velocity_5", QuestTier.III)
         .power("velocity")
         .trigger(QuestTrigger.REACH_MASTERY, 9)
         .xp(350)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("velocity_16", QuestTier.III)
         .power("velocity")
         .feat("velocity_tunnel_300", 1)
         .xp(300)
         .after("velocity_5")
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
   }
}
