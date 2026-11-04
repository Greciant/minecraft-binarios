package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class RegeneratorQuests {
   private RegeneratorQuests() {
   }

   public static void register() {
      Quest.trial("regenerator_1", QuestTier.I)
         .power("regenerator")
         .trigger(QuestTrigger.LAND_HITS, 25)
         .mastery(55)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.COOKED_BEEF, 12))
         .register();
      Quest.trial("regenerator_2", QuestTier.I)
         .power("regenerator")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 20)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("regenerator_6", QuestTier.I)
         .power("regenerator")
         .trigger(QuestTrigger.USE_SIGNATURE, 12)
         .mastery(55)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.GOLDEN_APPLE, 1))
         .register();
      Quest.trial("regenerator_7", QuestTier.I)
         .power("regenerator")
         .obtain("minecraft:rotten_flesh", 24)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.EXPERIENCE_BOTTLE, 4))
         .register();
      Quest.trial("regenerator_3", QuestTier.II)
         .power("regenerator")
         .trigger(QuestTrigger.USE_SIGNATURE, 30)
         .mastery(100)
         .xp(210)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("regenerator_4", QuestTier.II)
         .power("regenerator")
         .trigger(QuestTrigger.CLUTCH_SURVIVE, 30)
         .needsMastery(9)
         .mastery(95)
         .xp(190)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("regenerator_8", QuestTier.II)
         .power("regenerator")
         .kill("minecraft:witch", 10)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("regenerator_9", QuestTier.II)
         .power("regenerator")
         .place("structure/minecraft:mineshaft")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("regenerator_10", QuestTier.II)
         .power("regenerator")
         .obtain("awakened:raw_adamantium", 6)
         .mastery(105)
         .xp(230)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("regenerator_11", QuestTier.II)
         .power("regenerator")
         .feat("regen_bleed_out", 8)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("regenerator_12", QuestTier.II)
         .power("regenerator")
         .feat("regen_pin_kill", 5)
         .needsMastery(7)
         .mastery(115)
         .xp(250)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("regenerator_13", QuestTier.III)
         .power("regenerator")
         .kill("minecraft:elder_guardian", 1)
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("regenerator_14", QuestTier.III)
         .power("regenerator")
         .place("structure/minecraft:trial_chambers")
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("regenerator_15", QuestTier.III)
         .power("regenerator")
         .obtain("awakened:adamantium_ingot", 8)
         .mastery(160)
         .xp(380)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("regenerator_16", QuestTier.III)
         .power("regenerator")
         .feat("regen_bleed_out", 30)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.goal("regenerator_5", QuestTier.III)
         .power("regenerator")
         .trigger(QuestTrigger.REACH_MASTERY, 9)
         .xp(350)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("regenerator_17", QuestTier.III).power("regenerator").feat("regen_bonded", 1).xp(400).reward(QuestRewards.catalyst()).register();
   }
}
