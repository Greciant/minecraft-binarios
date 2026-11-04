package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class CommandQuests {
   private static final String POWER = "command";

   private CommandQuests() {
   }

   public static void register() {
      Quest.trial("command_1", QuestTier.I)
         .power("command")
         .trigger(QuestTrigger.TRAVEL_DISTANCE, 4000)
         .mastery(55)
         .xp(110)
         .reward(QuestRewards.item(() -> Items.EMERALD, 3))
         .register();
      Quest.trial("command_2", QuestTier.I)
         .power("command")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 15)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.EXPERIENCE_BOTTLE, 4))
         .register();
      Quest.trial("command_3", QuestTier.I)
         .power("command")
         .trigger(QuestTrigger.MINE_BLOCKS, 256)
         .mastery(55)
         .xp(110)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("command_4", QuestTier.I)
         .power("command")
         .feat("command_held_kill", 5)
         .needsMastery(2)
         .mastery(65)
         .xp(130)
         .reward(QuestRewards.item(() -> Items.GOLDEN_CARROT, 6))
         .register();
      Quest.trial("command_6", QuestTier.II)
         .power("command")
         .trigger(QuestTrigger.LAND_HITS, 60)
         .needsMastery(2)
         .mastery(105)
         .xp(220)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("command_7", QuestTier.II)
         .power("command")
         .feat("command_held_kill", 20)
         .needsMastery(2)
         .mastery(115)
         .xp(250)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("command_8", QuestTier.II)
         .power("command")
         .feat("command_counter", 10)
         .needsMastery(5)
         .mastery(115)
         .xp(250)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("command_9", QuestTier.II)
         .power("command")
         .kill("minecraft:witch", 8)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("command_10", QuestTier.II)
         .power("command")
         .place("structure/minecraft:pillager_outpost")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("command_11", QuestTier.III)
         .power("command")
         .feat("command_crowd", 8)
         .needsMastery(8)
         .mastery(175)
         .xp(420)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("command_12", QuestTier.III)
         .power("command")
         .kill("minecraft:evoker", 5)
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("command_13", QuestTier.III)
         .power("command")
         .place("structure/minecraft:mansion")
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("command_14", QuestTier.III)
         .power("command")
         .feat("command_held_kill", 60)
         .needsMastery(2)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.goal("command_5", QuestTier.III)
         .power("command")
         .trigger(QuestTrigger.REACH_MASTERY, 9)
         .xp(350)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("command_15", QuestTier.III)
         .power("command")
         .feat("command_bind", 1)
         .xp(300)
         .after("command_5")
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
   }
}
