package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class RiftQuests {
   private RiftQuests() {
   }

   public static void register() {
      Quest.trial("pocket_1", QuestTier.I)
         .power("pocket")
         .place("dimension/awakened:pocket")
         .mastery(55)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.EXPERIENCE_BOTTLE, 8))
         .register();
      Quest.trial("pocket_2", QuestTier.I)
         .power("pocket")
         .trigger(QuestTrigger.LAND_HITS, 15)
         .mastery(55)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("pocket_3", QuestTier.I)
         .power("pocket")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 18)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.GOLDEN_APPLE, 1))
         .register();
      Quest.trial("pocket_6", QuestTier.I)
         .power("pocket")
         .obtain("minecraft:ender_pearl", 12)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.CHORUS_FRUIT, 8))
         .register();
      Quest.trial("pocket_7", QuestTier.II)
         .power("pocket")
         .trigger(QuestTrigger.LAND_HITS, 40)
         .mastery(105)
         .xp(220)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("pocket_4", QuestTier.II)
         .power("pocket")
         .trigger(QuestTrigger.CLUTCH_SURVIVE, 6)
         .needsMastery(3)
         .mastery(95)
         .xp(190)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("pocket_8", QuestTier.II)
         .power("pocket")
         .kill("minecraft:zombified_piglin", 16)
         .mastery(110)
         .xp(240)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("pocket_9", QuestTier.II)
         .power("pocket")
         .place("structure/minecraft:ruined_portal")
         .mastery(100)
         .xp(220)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("pocket_10", QuestTier.II)
         .power("pocket")
         .obtain("minecraft:ender_eye", 6)
         .mastery(100)
         .xp(210)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("pocket_11", QuestTier.II)
         .power("pocket")
         .feat("rift_exile", 6)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("pocket_12", QuestTier.II)
         .power("pocket")
         .feat("rift_dropped_out", 8)
         .needsMastery(6)
         .mastery(115)
         .xp(250)
         .reward(QuestRewards.item(AwakenedItems.RAW_ADAMANTIUM, 3))
         .register();
      Quest.trial("pocket_13", QuestTier.III)
         .power("pocket")
         .kill("awakened:archevoker", 1)
         .mastery(175)
         .xp(410)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("pocket_14", QuestTier.III)
         .power("pocket")
         .feat("rift_exile", 12)
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("pocket_15", QuestTier.III)
         .power("pocket")
         .feat("rift_dropped_out", 25)
         .needsMastery(6)
         .mastery(180)
         .xp(420)
         .reward(QuestRewards.catalyst())
         .register();
      Quest.goal("pocket_5", QuestTier.III)
         .power("pocket")
         .trigger(QuestTrigger.REACH_MASTERY, 11)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("pocket_16", QuestTier.III).power("pocket").feat("rift_domain_seal", 3).after("pocket_5").xp(420).reward(QuestRewards.catalyst()).register();
   }
}
