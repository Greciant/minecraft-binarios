package com.devilfruits.fruits.suna_suna.gametest;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.CoreEvents;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.CastResult;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitRegistry;
import com.devilfruits.core.gacha.CasketLootModifier;
import com.devilfruits.core.gacha.GachaTier;
import com.devilfruits.core.logia.CounterTrigger;
import com.devilfruits.core.logia.HitRule;
import com.devilfruits.core.logia.LogiaDamage;
import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.core.logia.LogiaMovement;
import com.devilfruits.core.logia.LogiaState;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.rules.WorldFruitData;
import com.devilfruits.core.world.BlockRestoreData;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.fruits.suna_suna.Desiccation;
import com.devilfruits.fruits.suna_suna.SandFunnel;
import com.devilfruits.fruits.suna_suna.SandStorm;
import com.devilfruits.fruits.suna_suna.SandTravel;
import com.devilfruits.fruits.suna_suna.SunaAbilities;
import com.devilfruits.fruits.suna_suna.SunaBlocks;
import com.devilfruits.fruits.suna_suna.SunaEffects;
import com.devilfruits.fruits.suna_suna.SunaSuna;
import com.devilfruits.fruits.suna_suna.ability.DesertEncierroAbility;
import com.devilfruits.fruits.suna_suna.ability.DesertLaSpadaAbility;
import com.devilfruits.fruits.suna_suna.block.QuicksandBlock;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFItems;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContext.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("devilfruits")
@PrefixGameTestTemplate(false)
public final class SunaGameTests {
   private static final String PLATFORM = "platform";

   private static FakePlayer user(GameTestHelper helper, BlockPos relative) {
      FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "suna_test")) {
         public boolean isInvulnerableTo(DamageSource source) {
            return false;
         }
      };
      player.setGameMode(GameType.SURVIVAL);
      ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, player, 0, "spawnInvulnerableTime");
      BlockPos pos = helper.absolutePos(relative);
      player.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
      FruitRules.grantPower(player, SunaSuna.fruit(), false);
      DFAttachments.data(player).setEnergy(100.0F);
      return player;
   }

   private static void tick(FakePlayer player, int ticks) {
      PlayerFruitData data = DFAttachments.data(player);

      for (int i = 0; i < ticks; i++) {
         player.tickCount++;
         CoreEvents.tickServer(player, data);
      }
   }

   private static void done(GameTestHelper helper, FakePlayer... players) {
      for (FakePlayer p : players) {
         AbilityManager.cancelAll(p);
         AbilityManager.forget(p.getUUID());
         LogiaManager.forget(p);
         FruitRules.removePower(p);
      }

      for (DevilFruit fruit : FruitRegistry.all()) {
         WorldFruitData.get(helper.getLevel().getServer()).setFree(fruit);
      }

      helper.succeed();
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 80
   )
   public static void eatingTheFruitTurnsTheBodyToSandAndBack(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 2));
      FruitRules.grantPower(player, SunaSuna.fruit(), true);
      helper.assertTrue(LogiaManager.isLogia(player) && SunaSuna.ELEMENT.equals(LogiaManager.profile(player).element()), "a sand Logia");
      helper.assertTrue(LogiaManager.state(player) == LogiaState.ELEMENTAL_BODY, "the new body bursts into sand");
      helper.runAfterDelay(32L, () -> {
         helper.assertTrue(LogiaManager.state(player) != LogiaState.ELEMENTAL_BODY, "and gathers back: " + LogiaManager.state(player));
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform"
   )
   public static void wetSandIsSolidAndWeak(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 2));
      helper.assertTrue(LogiaDamage.decide(player, player.damageSources().fall()).rule() == HitRule.PASS, "a dry sand body lets a fall through");
      LogiaManager.trigger(player, CounterTrigger.Kind.WATER_BUCKET, null);
      tick(player, 1);
      helper.assertTrue(LogiaManager.counterActive(player, "wet"), "a bucket of water wets the sand");
      helper.assertTrue(LogiaDamage.decide(player, player.damageSources().fall()).rule() == HitRule.CONNECT, "wet sand is hit");
      helper.assertTrue(LogiaManager.abilityPower(player) < 1.0F, "and its techniques are weaker: " + LogiaManager.abilityPower(player));
      helper.assertFalse(LogiaManager.abilitiesBlocked(player), "but still usable");
      helper.assertFalse(LogiaManager.setElemental(player, true), "wet sand cannot become the whole sand body");
      helper.assertTrue(AbilityManager.press(player, SunaAbilities.SAND_CLOUD) != CastResult.TOGGLED, "nor fly as a cloud");
      tick(player, 105);
      helper.assertFalse(LogiaManager.counterActive(player, "wet"), "it dries after five seconds");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void dryingTurnsTheLandToSandAndItHeals(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 2));
      helper.setBlock(1, 1, 1, Blocks.GRASS_BLOCK);
      helper.setBlock(1, 2, 1, Blocks.POPPY);
      helper.setBlock(2, 1, 1, Blocks.FARMLAND);
      helper.setBlock(2, 2, 1, Blocks.WHEAT);
      helper.setBlock(3, 1, 1, Blocks.WATER);
      helper.setBlock(1, 1, 2, Blocks.DIRT);
      helper.setBlock(3, 1, 2, Blocks.CLAY);
      helper.setBlock(1, 1, 3, Blocks.GRASS_BLOCK);
      helper.setBlock(1, 2, 3, (BlockState)Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
      helper.setBlock(1, 3, 3, (BlockState)Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
      Vec3 centre = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)));
      int changed = Desiccation.dry(helper.getLevel(), player, centre, 2.5, Desiccation.Options.SURFACE, new CastBudget(200, 0));
      helper.assertBlockPresent(Blocks.SAND, new BlockPos(1, 1, 1));
      helper.assertBlockPresent(Blocks.DEAD_BUSH, new BlockPos(1, 2, 1));
      helper.assertBlockPresent(Blocks.SAND, new BlockPos(2, 1, 1));
      helper.assertBlockPresent(Blocks.DEAD_BUSH, new BlockPos(2, 2, 1));
      helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, 1, 1));
      helper.assertBlockPresent(Blocks.SAND, new BlockPos(1, 1, 2));
      helper.assertBlockPresent(Blocks.SAND, new BlockPos(3, 1, 2));
      helper.assertBlockPresent(Blocks.DEAD_BUSH, new BlockPos(1, 2, 3));
      helper.assertBlockPresent(Blocks.AIR, new BlockPos(1, 3, 3));
      helper.assertBlockPresent(Blocks.STONE, new BlockPos(3, 1, 3));
      helper.assertTrue(changed >= 10, "blocks changed: " + changed);
      AABB box = new AABB(helper.absolutePos(BlockPos.ZERO).getCenter(), helper.absolutePos(new BlockPos(4, 4, 4)).getCenter()).inflate(0.6);
      BlockRestoreData.get(helper.getLevel()).restoreInside(helper.getLevel(), box);
      helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(1, 1, 1));
      helper.assertBlockPresent(Blocks.POPPY, new BlockPos(1, 2, 1));
      helper.assertBlockPresent(Blocks.FARMLAND, new BlockPos(2, 1, 1));
      helper.assertBlockPresent(Blocks.WHEAT, new BlockPos(2, 2, 1));
      helper.assertBlockPresent(Blocks.WATER, new BlockPos(3, 1, 1));
      helper.assertBlockPresent(Blocks.CLAY, new BlockPos(3, 1, 2));
      helper.assertBlockPresent(Blocks.TALL_GRASS, new BlockPos(1, 2, 3));
      helper.assertBlockPresent(Blocks.TALL_GRASS, new BlockPos(1, 3, 3));
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void sandThatWasDugAwayDoesNotComeBackAsGrass(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 2));
      helper.setBlock(2, 1, 1, Blocks.GRASS_BLOCK);
      Desiccation.dry(
         helper.getLevel(), player, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 1))), 0.5, Desiccation.Options.SURFACE, new CastBudget(10, 0)
      );
      helper.assertBlockPresent(Blocks.SAND, new BlockPos(2, 1, 1));
      helper.setBlock(2, 1, 1, Blocks.COBBLESTONE);
      AABB box = new AABB(helper.absolutePos(new BlockPos(2, 1, 1))).inflate(0.1);
      BlockRestoreData.get(helper.getLevel()).restoreInside(helper.getLevel(), box);
      helper.assertBlockPresent(Blocks.COBBLESTONE, new BlockPos(2, 1, 1));
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      batch = "suna_no_griefing"
   )
   public static void dryingRespectsMobGriefing(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 2));
      BooleanValue griefing = (BooleanValue)helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
      boolean before = griefing.get();
      griefing.set(false, helper.getLevel().getServer());

      try {
         helper.setBlock(2, 1, 1, Blocks.GRASS_BLOCK);
         int changed = Desiccation.dry(
            helper.getLevel(),
            player,
            Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 1))),
            1.5,
            Desiccation.Options.SURFACE,
            new CastBudget(50, 0)
         );
         helper.assertTrue(changed == 0, "nothing changes without mobGriefing: " + changed);
         helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(2, 1, 1));
      } finally {
         griefing.set(before, helper.getLevel().getServer());
      }

      done(helper, player);
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 120
   )
   public static void quicksandSwallowsButNotTheSand(GameTestHelper helper) {
      BlockState quicksand = ((QuicksandBlock)SunaBlocks.QUICKSAND.get()).defaultBlockState();

      for (int y = 2; y <= 3; y++) {
         for (int x : new int[]{1, 3}) {
            helper.setBlock(x, y, 2, quicksand);
            helper.setBlock(x, y, 1, Blocks.GLASS);
            helper.setBlock(x, y, 3, Blocks.GLASS);
         }

         helper.setBlock(0, y, 2, Blocks.GLASS);
         helper.setBlock(2, y, 2, Blocks.GLASS);
         helper.setBlock(4, y, 2, Blocks.GLASS);
      }

      FakePlayer player = user(helper, new BlockPos(3, 4, 2));
      player.move(MoverType.SELF, new Vec3(0.0, -1.0, 0.0));
      helper.assertTrue(player.getY() >= (double)helper.absolutePos(new BlockPos(3, 4, 2)).getY() - 0.01, "the sand holds its user: y " + player.getY());
      Pig pig = (Pig)helper.spawn(EntityType.PIG, new BlockPos(1, 4, 2));
      float health = pig.getHealth();
      double top = pig.getY();
      helper.runAfterDelay(80L, () -> {
         helper.assertTrue(pig.getY() < top - 1.0, "the pig sank: " + pig.getY() + " from " + top);
         helper.assertTrue(pig.getHealth() < health, "and chokes under the sand");
         pig.discard();
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 180,
      batch = "suna_girasole"
   )
   public static void girasoleDragsIntoItsThroatThenSettles(GameTestHelper helper) {
      for (int x = 0; x < 5; x++) {
         for (int z = 0; z < 5; z++) {
            helper.setBlock(x, 2, z, Blocks.SAND);
         }
      }

      FakePlayer player = user(helper, new BlockPos(0, 3, 0));
      Vec3 centre = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 3, 2)));
      Pig pig = (Pig)helper.spawn(EntityType.PIG, new BlockPos(4, 3, 2));
      double before = pig.position().subtract(centre).horizontalDistance();
      SandFunnel funnel = SandFunnel.open(helper.getLevel(), player, centre, new SandFunnel.Shape(2.5, 1.0, 1, 0.08, 4), new CastBudget(100, 0), 7L, 200);
      helper.onEachTick(() -> funnel.tick(0.0F, 0));
      BlockPos throat = new BlockPos(2, 2, 2);
      helper.runAfterDelay(130L, () -> {
         helper.assertBlockPresent((Block)SunaBlocks.QUICKSAND.get(), throat);
         helper.assertBlockPresent(Blocks.SAND, new BlockPos(0, 2, 2));
         double now = pig.position().subtract(centre).horizontalDistance();
         helper.assertTrue(now < before - 0.8, "the pig is dragged towards the centre: " + now + " from " + before);
         helper.assertTrue(pig.getY() < centre.y - 0.3, "and sinks into the throat: y " + pig.getY() + ", surface " + centre.y);
         funnel.close();
         helper.assertBlockPresent(Blocks.SAND, throat);
         helper.assertTrue(BlockRestoreData.get(helper.getLevel()).pending(helper.absolutePos(throat)) == null, "nothing left to heal");
         helper.assertTrue(pig.getY() >= centre.y - 0.01, "the closing throat throws the pig up: y " + pig.getY());
         pig.discard();
         done(helper, player);
      });
   }

   private static FakePlayer master(GameTestHelper helper, BlockPos relative, int mastery) {
      FakePlayer player = user(helper, relative);
      PlayerFruitData data = DFAttachments.data(player);
      Mastery.setLevel(player, data, mastery, false);
      data.setEnergy((float)Mastery.maxEnergy(data));
      return player;
   }

   private static void soil(GameTestHelper helper) {
      for (int x = 0; x < 5; x++) {
         for (int z = 0; z < 5; z++) {
            helper.setBlock(x, 2, z, Blocks.GRASS_BLOCK);
         }
      }
   }

   private static void wall(GameTestHelper helper, int z) {
      wall(helper, z, Blocks.STONE);
   }

   private static void wall(GameTestHelper helper, int z, Block block) {
      for (int x = 0; x < 5; x++) {
         for (int y = 2; y <= 4; y++) {
            helper.setBlock(x, y, z, block);
         }
      }
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 60,
      batch = "suna_spada"
   )
   public static void spadaCutsThroughWallsButNotObsidian(GameTestHelper helper) {
      FakePlayer player = master(helper, new BlockPos(2, 2, 0), 2);
      wall(helper, 3, Blocks.STONE);
      wall(helper, 4, Blocks.OBSIDIAN);
      BlockState door = Blocks.OAK_DOOR.defaultBlockState();
      helper.setBlock(new BlockPos(1, 2, 3), (BlockState)door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
      helper.setBlock(new BlockPos(1, 3, 3), (BlockState)door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
      Husk husk = (Husk)helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 2));
      float health = husk.getHealth();
      helper.assertTrue(AbilityManager.press(player, SunaAbilities.DESERT_SPADA) == CastResult.SUCCESS, "the hand strikes the ground");
      helper.runAfterDelay(
         8L,
         () -> {
            helper.assertTrue(husk.getHealth() < health, "the blade cut the husk");
            BlockRestoreData restore = BlockRestoreData.get(helper.getLevel());
            boolean split = restore.pending(helper.absolutePos(new BlockPos(2, 1, 2))) != null
               && restore.pending(helper.absolutePos(new BlockPos(2, 1, 3))) != null;
            helper.assertTrue(split, "split the ground open behind it, block after block");

            for (int x = 1; x <= 3; x++) {
               for (int y = 2; y <= 4; y++) {
                  helper.assertTrue(restore.pending(helper.absolutePos(new BlockPos(x, y, 3))) != null, "cut through the stone wall, three wide and three high");
               }
            }

            helper.assertBlockPresent(Blocks.STONE, new BlockPos(0, 2, 3));
            helper.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(2, 2, 4));
            helper.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(2, 3, 4));
            AABB area = new AABB(helper.absolutePos(BlockPos.ZERO).getCenter(), helper.absolutePos(new BlockPos(4, 4, 4)).getCenter()).inflate(0.6);
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, area.inflate(3.0)).isEmpty(), "the door did not pop off as an item");
            restore.restoreInside(helper.getLevel(), area);
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(2, 3, 3));
            helper.assertBlockPresent(Blocks.OAK_DOOR, new BlockPos(1, 2, 3));
            helper.assertBlockPresent(Blocks.OAK_DOOR, new BlockPos(1, 3, 3));
            husk.discard();
            done(helper, player);
         }
      );
   }

   @GameTest(
      template = "platform"
   )
   public static void sandCloudFliesUntilSwitchedOff(GameTestHelper helper) {
      FakePlayer player = master(helper, new BlockPos(2, 2, 2), 2);
      helper.assertTrue(AbilityManager.press(player, SunaAbilities.SAND_CLOUD) == CastResult.TOGGLED, "the body bursts into a cloud");
      LogiaMovement.Session session = LogiaMovement.session(player);
      helper.assertTrue(session != null && session.mode == SandTravel.CLOUD, "flying as a cloud");
      helper.assertTrue(player.mayFly(), "allowed to fly (no floating kick)");
      float energy = DFAttachments.data(player).energy();
      tick(player, 10);
      helper.assertTrue(LogiaMovement.session(player) != null && DFAttachments.data(player).energy() < energy, "it keeps flying and drains energy");
      helper.assertTrue(AbilityManager.press(player, SunaAbilities.SAND_CLOUD) == CastResult.TOGGLED, "switched off");
      tick(player, 2);
      helper.assertTrue(LogiaMovement.session(player) == null, "the cloud gathers back into a body");
      helper.assertFalse(player.mayFly(), "and may not fly any more");
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 40,
      batch = "suna_barjan"
   )
   public static void barjanDriesOutWhatItHits(GameTestHelper helper) {
      FakePlayer player = master(helper, new BlockPos(2, 2, 0), 3);
      wall(helper, 4);
      Husk husk = (Husk)helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 3));
      float health = husk.getHealth();
      helper.assertTrue(AbilityManager.press(player, SunaAbilities.BARJAN) == CastResult.SUCCESS, "the crescent flies");
      helper.runAfterDelay(8L, () -> {
         helper.assertTrue(husk.getHealth() < health, "the crescent struck the husk");
         helper.assertTrue(husk.hasEffect(SunaEffects.DEHYDRATED), "and dried it out");
         husk.discard();
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 100,
      batch = "suna_techniques"
   )
   public static void groundSeccoDriesTheLandAndWhoStandsOnIt(GameTestHelper helper) {
      soil(helper);
      FakePlayer player = master(helper, new BlockPos(2, 3, 2), 3);
      Pig pig = (Pig)helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(0, 3, 2));
      helper.assertTrue(AbilityManager.press(player, SunaAbilities.GROUND_SECCO) == CastResult.CHANNELLING, "the palm goes down");
      helper.onEachTick(() -> tick(player, 1));
      helper.runAfterDelay(50L, () -> {
         helper.assertBlockPresent(Blocks.SAND, new BlockPos(2, 2, 2));
         helper.assertBlockPresent(Blocks.SAND, new BlockPos(0, 2, 2));
         helper.assertTrue(pig.hasEffect(SunaEffects.DEHYDRATED), "the pig standing on it dries out");
         AbilityManager.release(player, SunaAbilities.GROUND_SECCO);
         AABB box = new AABB(helper.absolutePos(BlockPos.ZERO).getCenter(), helper.absolutePos(new BlockPos(4, 4, 4)).getCenter()).inflate(0.6);
         BlockRestoreData.get(helper.getLevel()).restoreInside(helper.getLevel(), box);
         helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(2, 2, 2));
         pig.discard();
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 60,
      batch = "suna_techniques"
   )
   public static void laSpadaFlingsMoreBladesWithMastery(GameTestHelper helper) {
      helper.assertTrue(
         DesertLaSpadaAbility.blades(3, 5) == 3 && DesertLaSpadaAbility.blades(3, 7) == 4 && DesertLaSpadaAbility.blades(3, 10) == 5,
         "3 blades at mastery 5, 4 at 7, 5 at 10"
      );
      FakePlayer player = master(helper, new BlockPos(2, 2, 0), 5);
      wall(helper, 4);
      Husk husk = (Husk)helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 3));
      float health = husk.getHealth();
      helper.assertTrue(AbilityManager.press(player, SunaAbilities.DESERT_LA_SPADA) == CastResult.SUCCESS, "the blades form");
      float[] afterFirst = new float[]{0.0F};
      helper.runAfterDelay(7L, () -> afterFirst[0] = husk.getHealth());
      helper.runAfterDelay(18L, () -> {
         helper.assertTrue(afterFirst[0] < health, "the first blade struck");
         helper.assertTrue(husk.getHealth() < afterFirst[0], "and the next ones too, each a blow of its own");
         husk.discard();
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 80,
      batch = "suna_techniques"
   )
   public static void encierroHoldsAndDrains(GameTestHelper helper) {
      FakePlayer player = master(helper, new BlockPos(2, 2, 0), 6);
      Husk husk = (Husk)helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 2));
      float health = husk.getHealth();
      helper.assertTrue(AbilityManager.press(player, SunaAbilities.DESERT_ENCIERRO) == CastResult.CHANNELLING, "the sand hand seizes");
      helper.assertTrue(DesertEncierroAbility.victimId(player) == husk.getId(), "the husk in front");
      helper.onEachTick(() -> tick(player, 1));
      helper.runAfterDelay(45L, () -> {
         helper.assertTrue(husk.getHealth() < health, "its moisture is drawn out");
         MobEffectInstance dry = husk.getEffect(SunaEffects.DEHYDRATED);
         helper.assertTrue(dry != null && dry.getAmplifier() >= 1, "Dehydrated stacks while held: " + dry);
         AbilityManager.release(player, SunaAbilities.DESERT_ENCIERRO);
         helper.assertTrue(DesertEncierroAbility.victimId(player) == -1, "letting go frees it");
         husk.discard();
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 100,
      batch = "suna_wide_tsunami"
   )
   public static void tsunamiSweepsAndDriesTheLand(GameTestHelper helper) {
      soil(helper);
      FakePlayer player = master(helper, new BlockPos(2, 3, 0), 7);
      Husk husk = (Husk)helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 3, 3));
      float health = husk.getHealth();
      helper.assertTrue(AbilityManager.press(player, SunaAbilities.SAND_TSUNAMI) == CastResult.CHARGING, "the land heaves");
      CastResult[] released = new CastResult[]{null};
      helper.runAfterDelay(42L, () -> released[0] = AbilityManager.release(player, SunaAbilities.SAND_TSUNAMI));
      helper.runAfterDelay(52L, () -> {
         helper.assertTrue(released[0] == CastResult.SUCCESS, "the wave rolls: " + released[0]);
         helper.assertTrue(husk.getHealth() < health, "the wave struck the husk");
         helper.assertTrue(husk.hasEffect(SunaEffects.DEHYDRATED), "and dried it out");
         helper.assertBlockPresent(Blocks.SAND, new BlockPos(2, 2, 3));
         AABB box = new AABB(helper.absolutePos(BlockPos.ZERO).getCenter(), helper.absolutePos(new BlockPos(4, 4, 4)).getCenter()).inflate(12.0, 2.0, 0.6);
         BlockRestoreData.get(helper.getLevel()).restoreInside(helper.getLevel(), box);
         husk.discard();
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 60,
      batch = "suna_wide_sables_pull"
   )
   public static void sablesDragsInFromAfar(GameTestHelper helper) {
      FakePlayer player = master(helper, new BlockPos(4, 2, 0), 8);
      Pig pig = (Pig)helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 2, 4));
      Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(0, 2, 0)));
      double before = Math.sqrt(pig.position().subtract(at).horizontalDistanceSqr());
      SandStorm.Power power = new SandStorm.Power(2.5, 50, 0.0, 0.3, 0.16, 1.0F, 10, 0.0, 2, 1.0, false, 8.0);
      SandStorm storm = new SandStorm(helper.getLevel(), player, at, 0.0, power, new CastBudget(0, 0), 9L);
      int[] age = new int[]{0};
      helper.onEachTick(() -> storm.tick((int)(age[0]++)));
      helper.runAfterDelay(30L, () -> {
         double after = Math.sqrt(pig.position().subtract(at).horizontalDistanceSqr());
         helper.assertTrue(after < before - 1.0, "the storm dragged the pig in from " + before + " to " + after);
         pig.discard();
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 80,
      batch = "suna_wide_sables"
   )
   public static void sablesStormLiftsAndBlowsApart(GameTestHelper helper) {
      FakePlayer player = master(helper, new BlockPos(0, 2, 0), 8);
      Pig pig = (Pig)helper.spawn(EntityType.PIG, new BlockPos(3, 2, 2));
      float health = pig.getHealth();
      double floor = pig.getY();
      Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)));
      SandStorm.Power power = new SandStorm.Power(2.5, 40, 0.0, 0.3, 0.16, 1.0F, 10, 0.0, 2, 1.0, false);
      SandStorm storm = new SandStorm(helper.getLevel(), player, at, 0.0, power, new CastBudget(0, 0), 9L);
      int[] age = new int[]{0};
      boolean[] over = new boolean[]{false};
      double[] highest = new double[]{floor};
      helper.onEachTick(() -> {
         if (!over[0]) {
            over[0] = storm.tick((int)(age[0]++));
            highest[0] = Math.max(highest[0], pig.getY());
         }
      });
      helper.runAfterDelay(
         30L,
         () -> {
            helper.assertTrue(
               highest[0] > floor + 0.5,
               "the storm lifted the pig: "
                  + highest[0]
                  + " from "
                  + floor
                  + ", it holds "
                  + storm.held()
                  + " at "
                  + storm.position()
                  + ", pig at "
                  + pig.position()
            );
            helper.assertTrue(pig.getHealth() < health, "and tore at it");
            helper.assertTrue(pig.hasEffect(SunaEffects.DEHYDRATED), "drying it out");
         }
      );
      helper.runAfterDelay(45L, () -> {
         helper.assertTrue(over[0], "the storm blew apart when its time was up");
         pig.discard();
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 80,
      batch = "suna_pesado"
   )
   public static void pesadoBallBurstsAndDriesOut(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 0));
      Husk husk = (Husk)helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 4));
      float health = husk.getHealth();
      helper.assertTrue(AbilityManager.press(player, SunaAbilities.SABLES_PESADO) == CastResult.CHARGING, "the sand gathers");
      helper.runAfterDelay(32L, () -> {
         AbilityManager.release(player, SunaAbilities.SABLES_PESADO);
         helper.runAfterDelay(12L, () -> {
            helper.assertTrue(husk.getHealth() < health, "the ball burst on the husk");
            helper.assertTrue(husk.hasEffect(SunaEffects.DEHYDRATED), "and dried it out");
            husk.discard();
            done(helper, player);
         });
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 40
   )
   public static void pesadoTapIsAQuickBlast(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 0));
      Husk husk = (Husk)helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 4));
      float health = husk.getHealth();
      AbilityManager.press(player, SunaAbilities.SABLES_PESADO);
      AbilityManager.release(player, SunaAbilities.SABLES_PESADO);
      helper.runAfterDelay(8L, () -> {
         helper.assertTrue(husk.getHealth() < health, "the blast hit");
         helper.assertFalse(husk.hasEffect(SunaEffects.DEHYDRATED), "a quick blast does not dry anybody out");
         husk.discard();
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform"
   )
   public static void dehydrationStacks(GameTestHelper helper) {
      Husk husk = (Husk)helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 2));
      SunaEffects.dehydrate(husk, 10, 1, 4, null);
      SunaEffects.dehydrate(husk, 10, 1, 4, null);
      MobEffectInstance effect = husk.getEffect(SunaEffects.DEHYDRATED);
      helper.assertTrue(effect != null && effect.getAmplifier() == 1, "two doses: level II");
      SunaEffects.dehydrate(husk, 2, 9, 4, null);
      effect = husk.getEffect(SunaEffects.DEHYDRATED);
      helper.assertTrue(effect.getAmplifier() == 4, "capped at level 5");
      helper.assertTrue(effect.getDuration() > 150, "and the longer duration is kept");
      husk.discard();
      helper.succeed();
   }

   @GameTest(
      template = "platform",
      batch = "suna_loot"
   )
   public static void suspiciousSandHandsOutACasketInsteadOfItsItem(GameTestHelper helper) {
      helper.assertTrue(SunaSuna.fruit().defaultGachaTier() == GachaTier.RARE, "a Logia is a rare casket fruit");
      CasketLootModifier modifier = new CasketLootModifier(new LootItemCondition[0], 1.0F, true);
      LootContext context = new Builder(new net.minecraft.world.level.storage.loot.LootParams.Builder(helper.getLevel()).create(LootContextParamSets.EMPTY))
         .withQueriedLootTableId(DevilFruits.id("test/archaeology"))
         .create(Optional.empty());
      ObjectArrayList<ItemStack> loot = new ObjectArrayList();
      loot.add(new ItemStack(Items.EMERALD));
      loot = modifier.apply(loot, context);
      helper.assertTrue(loot.size() == 1 && ((ItemStack)loot.get(0)).is((Item)DFItems.CASKET.get()), "one item, a casket: " + loot);
      helper.succeed();
   }
}
