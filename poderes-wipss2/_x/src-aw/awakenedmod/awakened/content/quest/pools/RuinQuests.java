package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class RuinQuests {
   private RuinQuests() {
   }

   public static void register() {
      Quest.trial("ruin_1", QuestTier.I)
         .power("ruin")
         .trigger(QuestTrigger.LAND_HITS, 25)
         .mastery(55)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.ENDER_PEARL, 8))
         .register();
      Quest.trial("ruin_2", QuestTier.I)
         .power("ruin")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 18)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("ruin_3", QuestTier.I)
         .power("ruin")
         .obtain("minecraft:rotten_flesh", 64)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.GOLDEN_APPLE, 1))
         .register();
      Quest.trial("ruin_4", QuestTier.I)
         .power("ruin")
         .feat("ruin_deep", 5)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.EXPERIENCE_BOTTLE, 8))
         .register();
      Quest.trial("ruin_5", QuestTier.II)
         .power("ruin")
         .trigger(QuestTrigger.USE_SIGNATURE, 24)
         .mastery(100)
         .xp(210)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("ruin_6", QuestTier.II)
         .power("ruin")
         .trigger(QuestTrigger.LAND_HITS, 60)
         .mastery(105)
         .xp(220)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("ruin_7", QuestTier.II)
         .power("ruin")
         .kill("minecraft:husk", 12)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("ruin_8", QuestTier.II)
         .power("ruin")
         .kill("minecraft:wither_skeleton", 12)
         .mastery(115)
         .xp(250)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("ruin_9", QuestTier.II)
         .power("ruin")
         .place("biome/minecraft:eroded_badlands")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("ruin_10", QuestTier.II)
         .power("ruin")
         .feat("ruin_slam", 10)
         .needsMastery(5)
         .mastery(115)
         .xp(250)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("ruin_11", QuestTier.II)
         .power("ruin")
         .feat("ruin_rot", 15)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("ruin_12", QuestTier.III)
         .power("ruin")
         .kill("minecraft:wither", 1)
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("ruin_13", QuestTier.III)
         .power("ruin")
         .place("structure/minecraft:trail_ruins")
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("ruin_14", QuestTier.III).power("ruin").feat("ruin_rot", 50).mastery(180).xp(420).reward(QuestRewards.catalyst()).register();
      Quest.goal("ruin_15", QuestTier.III)
         .power("ruin")
         .trigger(QuestTrigger.REACH_MASTERY, 10)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("ruin_16", QuestTier.III).power("ruin").feat("ruin_collapse", 3).xp(400).after("ruin_15").reward(QuestRewards.catalyst()).register();
   }
}
