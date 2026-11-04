package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class GravitonQuests {
   private GravitonQuests() {
   }

   public static void register() {
      Quest.trial("graviton_1", QuestTier.I)
         .power("graviton")
         .trigger(QuestTrigger.LAND_HITS, 25)
         .mastery(55)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.ENDER_PEARL, 8))
         .register();
      Quest.trial("graviton_2", QuestTier.I)
         .power("graviton")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 18)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("graviton_6", QuestTier.I)
         .power("graviton")
         .obtain("minecraft:gravel", 64)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.GOLDEN_APPLE, 1))
         .register();
      Quest.trial("graviton_17", QuestTier.I)
         .power("graviton")
         .feat("graviton_chain", 3)
         .needsMastery(3)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.EXPERIENCE_BOTTLE, 8))
         .register();
      Quest.trial("graviton_3", QuestTier.II)
         .power("graviton")
         .trigger(QuestTrigger.USE_SIGNATURE, 24)
         .mastery(100)
         .xp(210)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("graviton_8", QuestTier.II)
         .power("graviton")
         .trigger(QuestTrigger.LAND_HITS, 60)
         .mastery(105)
         .xp(220)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("graviton_9", QuestTier.II)
         .power("graviton")
         .kill("minecraft:phantom", 8)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("graviton_10", QuestTier.II)
         .power("graviton")
         .kill("minecraft:ghast", 6)
         .mastery(115)
         .xp(250)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("graviton_11", QuestTier.II)
         .power("graviton")
         .place("biome/minecraft:jagged_peaks")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("graviton_4", QuestTier.II)
         .power("graviton")
         .trigger(QuestTrigger.CLUTCH_SURVIVE, 6)
         .needsMastery(7)
         .mastery(95)
         .xp(190)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("graviton_18", QuestTier.II)
         .power("graviton")
         .feat("graviton_drop", 8)
         .needsMastery(5)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("graviton_13", QuestTier.III)
         .power("graviton")
         .kill("minecraft:shulker", 12)
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("graviton_14", QuestTier.III)
         .power("graviton")
         .place("structure/minecraft:end_city")
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("graviton_15", QuestTier.III).power("graviton").feat("graviton_crush", 25).mastery(180).xp(420).reward(QuestRewards.catalyst()).register();
      Quest.goal("graviton_5", QuestTier.III)
         .power("graviton")
         .trigger(QuestTrigger.REACH_MASTERY, 10)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("graviton_16", QuestTier.III)
         .power("graviton")
         .feat("graviton_collapse", 3)
         .xp(400)
         .after("graviton_5")
         .reward(QuestRewards.catalyst())
         .register();
   }
}
