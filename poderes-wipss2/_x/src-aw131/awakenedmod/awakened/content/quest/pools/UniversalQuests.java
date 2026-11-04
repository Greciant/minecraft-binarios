package com.awakenedmod.awakened.content.quest.pools;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.quest.QuestRewards;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestTier;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class UniversalQuests {
   private UniversalQuests() {
   }

   public static void register() {
      Quest.trial("first_blood", QuestTier.I)
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 5)
         .mastery(60)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 4))
         .register();
      Quest.trial("deep_delver", QuestTier.I)
         .trigger(QuestTrigger.MINE_BLOCKS, 400)
         .mastery(60)
         .xp(140)
         .reward(QuestRewards.item(() -> Items.EXPERIENCE_BOTTLE, 4))
         .register();
      Quest.trial("forge_hand", QuestTier.I)
         .trigger(QuestTrigger.CRAFT_ITEMS, 10)
         .mastery(50)
         .xp(120)
         .reward(QuestRewards.item(() -> Items.IRON_INGOT, 6))
         .register();
      Quest.trial("far_walker", QuestTier.I)
         .trigger(QuestTrigger.TRAVEL_DISTANCE, 6000)
         .mastery(70)
         .xp(160)
         .reward(QuestRewards.item(() -> Items.GOLDEN_CARROT, 8))
         .register();
      Quest.trial("long_dark", QuestTier.I)
         .trigger(QuestTrigger.SURVIVE_NIGHTS, 2)
         .mastery(60)
         .xp(130)
         .reward(QuestRewards.item(() -> Items.GOLDEN_APPLE, 1))
         .register();
      Quest.trial("well_provisioned", QuestTier.I)
         .obtain("minecraft:bread", 16)
         .mastery(50)
         .xp(100)
         .reward(QuestRewards.item(() -> Items.GOLDEN_CARROT, 6))
         .register();
      Quest.trial("iron_stock", QuestTier.I)
         .obtain("minecraft:iron_ingot", 32)
         .mastery(70)
         .xp(150)
         .reward(QuestRewards.item(() -> Items.EXPERIENCE_BOTTLE, 8))
         .register();
      Quest.trial("executioner", QuestTier.II)
         .trigger(QuestTrigger.DEFEAT_ENEMIES, 150)
         .mastery(140)
         .xp(320)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("mother_lode", QuestTier.II)
         .trigger(QuestTrigger.MINE_BLOCKS, 1500)
         .mastery(110)
         .xp(300)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 4))
         .register();
      Quest.trial("artisan", QuestTier.II)
         .trigger(QuestTrigger.CRAFT_ITEMS, 30)
         .mastery(90)
         .xp(220)
         .reward(QuestRewards.item(AwakenedItems.RAW_ADAMANTIUM, 3))
         .register();
      Quest.trial("globetrotter", QuestTier.II)
         .trigger(QuestTrigger.TRAVEL_DISTANCE, 20000)
         .mastery(120)
         .xp(300)
         .reward(QuestRewards.item(AwakenedItems.TRACKER, 1))
         .register();
      Quest.trial("night_warden", QuestTier.II)
         .trigger(QuestTrigger.SURVIVE_NIGHTS, 5)
         .mastery(100)
         .xp(240)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("cordon_cull", QuestTier.II)
         .kill("awakened:stalker", 10)
         .mastery(120)
         .xp(280)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("line_breaker", QuestTier.II)
         .kill("awakened:enforcer", 5)
         .mastery(130)
         .xp(300)
         .reward(QuestRewards.item(AwakenedItems.ADAMANTIUM_INGOT, 2))
         .register();
      Quest.trial("ashen_road", QuestTier.II)
         .place("dimension/minecraft:the_nether")
         .mastery(110)
         .xp(260)
         .reward(QuestRewards.item(() -> Items.DIAMOND, 2))
         .register();
      Quest.trial("the_deep_dark", QuestTier.II)
         .place("biome/minecraft:deep_dark")
         .mastery(130)
         .xp(300)
         .reward(QuestRewards.relicBand(Rarity.COMMON, Rarity.RARE, false))
         .register();
      Quest.trial("master_miner", QuestTier.III)
         .trigger(QuestTrigger.MINE_BLOCKS, 4000)
         .mastery(150)
         .xp(360)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("spire_seeker", QuestTier.III)
         .place("structure/awakened:evoker_spire")
         .mastery(160)
         .xp(380)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("court_of_judgment", QuestTier.III)
         .place("structure/awakened:arbiter_arena")
         .mastery(160)
         .xp(380)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.trial("far_gate", QuestTier.III).place("dimension/minecraft:the_end").mastery(170).xp(420).reward(QuestRewards.catalyst()).register();
      Quest.trial("overseer_hunt", QuestTier.III)
         .kill("awakened:overseer", 1)
         .mastery(170)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
      Quest.goal("nemesis", QuestTier.III)
         .trigger(QuestTrigger.DEFEAT_BOSS, 1)
         .mastery(150)
         .xp(400)
         .reward(QuestRewards.relicBand(Rarity.RARE, Rarity.EPIC, true))
         .register();
   }
}
