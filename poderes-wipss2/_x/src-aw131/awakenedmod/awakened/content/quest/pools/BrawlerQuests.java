package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class BrawlerQuests {
   private BrawlerQuests() {
   }

   public static void register() {
      Quest.trial("brawler_1", QuestTier.I)
         .power("brawler")
         .trigger(QuestTrigger.USE_SIGNATURE, 10)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 5))
         .register();
      Quest.trial("brawler_2", QuestTier.I)
         .power("brawler")
         .trigger(QuestTrigger.LAND_HITS, 25)
         .mastery(55)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.GOLD_INGOT, 6))
         .register();
      Quest.trial("brawler_3", QuestTier.I)
         .power("brawler")
         .obtain("minecraft:leather", 16)
         .mastery(50)
         .xp(90)
         .reward(QuestRewards.item(() -> Items.GOLDEN_APPLE, 1))
         .register();
      Quest.trial("brawler_4", QuestTier.I)
         .power("brawler")
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 15)
         .mastery(60)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.EMERALD, 4))
         .register();
      Quest.trial("brawler_5", QuestTier.II)
         .power("brawler")
         .needsMastery(10)
         .feat("brawler_parry", 5)
         .mastery(95)
         .xp(190)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("brawler_6", QuestTier.II)
         .power("brawler")
         .needsMastery(10)
         .feat("brawler_parry_shot", 5)
         .mastery(100)
         .xp(200)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("brawler_7", QuestTier.II)
         .power("brawler")
         .trigger(QuestTrigger.USE_SIGNATURE, 60)
         .mastery(110)
         .xp(230)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 2))
         .register();
      Quest.trial("brawler_8", QuestTier.II)
         .power("brawler")
         .trigger(QuestTrigger.LAND_HITS, 150)
         .mastery(105)
         .xp(220)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("brawler_9", QuestTier.II)
         .power("brawler")
         .kill("minecraft:vindicator", 8)
         .mastery(115)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("brawler_10", QuestTier.II)
         .power("brawler")
         .needsMastery(10)
         .feat("brawler_counter_shape", 5)
         .mastery(120)
         .xp(260)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 3))
         .register();
      Quest.trial("brawler_11", QuestTier.III)
         .power("brawler")
         .needsMastery(10)
         .feat("brawler_parry", 40)
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("brawler_12", QuestTier.III)
         .power("brawler")
         .needsMastery(10)
         .feat("brawler_parry_blast", 10)
         .mastery(160)
         .xp(380)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("brawler_13", QuestTier.III).power("brawler").feat("brawler_unchained", 5).mastery(175).xp(410).reward(QuestRewards.catalyst()).register();
      Quest.goal("brawler_goal_1", QuestTier.III).power("brawler").trigger(QuestTrigger.REACH_MASTERY, 10).xp(320).reward(QuestRewards.catalyst()).register();
      Quest.goal("brawler_goal_2", QuestTier.III)
         .power("brawler")
         .after("brawler_goal_1")
         .feat("brawler_unchained", 1)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
   }
}
