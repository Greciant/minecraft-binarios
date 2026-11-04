package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class ReaperQuests {
   private ReaperQuests() {
   }

   public static void register() {
      Quest.trial("reaper_1", QuestTier.I)
         .power("reaper")
         .trigger(QuestTrigger.LAND_HITS, 25)
         .mastery(55)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.SOUL_TORCH, 8))
         .register();
      Quest.trial("reaper_2", QuestTier.I)
         .power("reaper")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 18)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("reaper_3", QuestTier.I)
         .power("reaper")
         .obtain("minecraft:bone", 32)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.SOUL_LANTERN, 2))
         .register();
      Quest.trial("reaper_4", QuestTier.I)
         .power("reaper")
         .trigger(QuestTrigger.USE_SIGNATURE, 15)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.BONE, 16))
         .register();
      Quest.trial("reaper_5", QuestTier.II)
         .power("reaper")
         .kill("minecraft:drowned", 20)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("reaper_6", QuestTier.II)
         .power("reaper")
         .kill("minecraft:wither_skeleton", 12)
         .mastery(115)
         .xp(250)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("reaper_7", QuestTier.II)
         .power("reaper")
         .place("biome/minecraft:soul_sand_valley")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("reaper_8", QuestTier.II)
         .power("reaper")
         .trigger(QuestTrigger.CLUTCH_SURVIVE, 6)
         .needsMastery(3)
         .mastery(95)
         .xp(190)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("reaper_9", QuestTier.II)
         .power("reaper")
         .obtain("awakened:soul_vessel", 1)
         .mastery(95)
         .xp(200)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("reaper_10", QuestTier.II)
         .power("reaper")
         .feat("reaper_drain", 6)
         .needsMastery(5)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("reaper_11", QuestTier.III).power("reaper").kill("minecraft:warden", 1).mastery(180).xp(420).reward(QuestRewards.catalyst()).register();
      Quest.trial("reaper_12", QuestTier.III)
         .power("reaper")
         .place("structure/minecraft:stronghold")
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("reaper_13", QuestTier.III)
         .power("reaper")
         .obtain("awakened:soul_core", 1)
         .mastery(160)
         .xp(380)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("reaper_14", QuestTier.III)
         .power("reaper")
         .feat("reaper_condemn", 12)
         .needsMastery(10)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.goal("reaper_15", QuestTier.III)
         .power("reaper")
         .trigger(QuestTrigger.REACH_MASTERY, 10)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("reaper_16", QuestTier.III).power("reaper").obtain("awakened:reaper_scythe", 1).xp(400).reward(QuestRewards.catalyst()).register();
   }
}
