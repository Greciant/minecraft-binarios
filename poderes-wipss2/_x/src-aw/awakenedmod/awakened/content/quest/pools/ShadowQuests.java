package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class ShadowQuests {
   private ShadowQuests() {
   }

   public static void register() {
      Quest.trial("shadow_1", QuestTier.I)
         .power("shadow")
         .trigger(QuestTrigger.LAND_HITS, 25)
         .mastery(55)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("shadow_2", QuestTier.I)
         .power("shadow")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 20)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.ENDER_PEARL, 3))
         .register();
      Quest.trial("shadow_6", QuestTier.I)
         .power("shadow")
         .trigger(QuestTrigger.USE_SIGNATURE, 12)
         .mastery(55)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.GOLDEN_APPLE, 1))
         .register();
      Quest.trial("shadow_7", QuestTier.I)
         .power("shadow")
         .obtain("minecraft:coal", 32)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.OBSIDIAN, 6))
         .register();
      Quest.trial("shadow_3", QuestTier.II)
         .power("shadow")
         .trigger(QuestTrigger.USE_SIGNATURE, 30)
         .mastery(100)
         .xp(210)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("shadow_4", QuestTier.II)
         .power("shadow")
         .trigger(QuestTrigger.LAND_HITS, 60)
         .mastery(105)
         .xp(220)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("shadow_8", QuestTier.II)
         .power("shadow")
         .kill("minecraft:enderman", 10)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("shadow_9", QuestTier.II)
         .power("shadow")
         .place("biome/minecraft:dark_forest")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("shadow_10", QuestTier.II)
         .power("shadow")
         .obtain("minecraft:obsidian", 12)
         .mastery(95)
         .xp(200)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("shadow_11", QuestTier.II)
         .power("shadow")
         .feat("shadow_choke_kill", 6)
         .needsMastery(3)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("shadow_12", QuestTier.II)
         .power("shadow")
         .feat("shadow_ambush", 12)
         .needsMastery(5)
         .mastery(115)
         .xp(250)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("shadow_13", QuestTier.III)
         .power("shadow")
         .place("structure/minecraft:ancient_city")
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("shadow_14", QuestTier.III)
         .power("shadow")
         .feat("shadow_choke_kill", 25)
         .needsMastery(3)
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("shadow_15", QuestTier.III)
         .power("shadow")
         .feat("shadow_devour_kill", 10)
         .needsMastery(10)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.goal("shadow_5", QuestTier.III)
         .power("shadow")
         .trigger(QuestTrigger.REACH_MASTERY, 10)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("shadow_16", QuestTier.III)
         .power("shadow")
         .trigger(QuestTrigger.REACH_MASTERY, 12)
         .after("shadow_5")
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
   }
}
