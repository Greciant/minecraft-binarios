package com.devilfruits.core.gametest;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitRegistry;
import com.devilfruits.core.gacha.CasketItem;
import com.devilfruits.core.gacha.CasketLootModifier;
import com.devilfruits.core.gacha.CasketOpening;
import com.devilfruits.core.gacha.GachaRoll;
import com.devilfruits.core.gacha.GachaTier;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.WorldFruitData;
import com.devilfruits.registry.DFItems;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContext.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("devilfruits")
@PrefixGameTestTemplate(false)
public final class GachaGameTests {
   private static final String PLATFORM = "platform";
   private static final ToIntFunction<GachaTier> DEFAULT_WEIGHTS = GachaTier::defaultWeight;

   private static List<GachaRoll.Candidate<String>> pool(Object... tierNameWeight) {
      List<GachaRoll.Candidate<String>> out = new ArrayList<>();

      for (int i = 0; i < tierNameWeight.length; i += 3) {
         out.add(new GachaRoll.Candidate<>((String)tierNameWeight[i + 1], (GachaTier)tierNameWeight[i], (Integer)tierNameWeight[i + 2]));
      }

      return out;
   }

   @GameTest(
      template = "platform"
   )
   public static void rollFollowsTheTierWeightsThenTheFruitWeights(GameTestHelper helper) {
      List<GachaRoll.Candidate<String>> pool = pool(
         GachaTier.LEGENDARY, "l", 4, GachaTier.RARE, "r", 4, GachaTier.UNCOMMON, "u", 10, GachaTier.COMMON, "c1", 1, GachaTier.COMMON, "c3", 3
      );
      RandomSource random = RandomSource.create(20261023L);
      Map<GachaTier, Integer> tiers = new EnumMap<>(GachaTier.class);
      Map<String, Integer> names = new HashMap<>();
      int n = 40000;

      for (int i = 0; i < n; i++) {
         GachaRoll.Candidate<String> c = GachaRoll.roll(pool, DEFAULT_WEIGHTS, random).orElseThrow();
         tiers.merge(c.tier(), 1, Integer::sum);
         names.merge(c.value(), 1, Integer::sum);
      }

      for (GachaTier tier : GachaTier.values()) {
         double share = (double)tiers.getOrDefault(tier, 0).intValue() / (double)n;
         double want = (double)tier.defaultWeight() / 100.0;
         helper.assertTrue(Math.abs(share - want) < 0.012, tier + " comes up " + share + ", want " + want);
      }

      double c3 = (double)names.get("c3").intValue() / (double)tiers.get(GachaTier.COMMON).intValue();
      helper.assertTrue(Math.abs(c3 - 0.75) < 0.02, "inside a tier the fruit weights decide (3:1), got " + c3);
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void anEmptyTierIsSkippedAndTheRestRenormalised(GameTestHelper helper) {
      List<GachaRoll.Candidate<String>> pool = pool(GachaTier.RARE, "r", 1, GachaTier.COMMON, "c", 1, GachaTier.LEGENDARY, "never", 0);
      RandomSource random = RandomSource.create(7L);
      int rare = 0;
      int n = 20000;

      for (int i = 0; i < n; i++) {
         GachaRoll.Candidate<String> c = GachaRoll.roll(pool, DEFAULT_WEIGHTS, random).orElseThrow();
         helper.assertTrue(c.tier() == GachaTier.RARE || c.tier() == GachaTier.COMMON, "only tiers that hold a fruit: " + c.tier());
         rare += c.tier() == GachaTier.RARE ? 1 : 0;
      }

      double want = 0.1095890410958904;
      helper.assertTrue(Math.abs((double)rare / (double)n - want) < 0.012, "rare stays rare (8 of 73), got " + (double)rare / (double)n);
      helper.assertTrue(
         GachaRoll.roll(pool(GachaTier.COMMON, "c", 1), t -> t == GachaTier.COMMON ? 0 : 5, random).isEmpty(),
         "a tier weighted 0 never comes up, even when it is the only one"
      );
      helper.assertTrue(GachaRoll.roll(List.of(), DEFAULT_WEIGHTS, random).isEmpty(), "nothing to roll: empty");
      helper.succeed();
   }

   private static List<GachaGameTests.Saved> save(MinecraftServer server) {
      WorldFruitData data = WorldFruitData.get(server);
      List<GachaGameTests.Saved> out = new ArrayList<>();

      for (DevilFruit fruit : FruitRegistry.playable()) {
         out.add(new GachaGameTests.Saved(fruit, data.state(fruit), data.instance(fruit).orElse(null), data.owner(fruit).orElse(null), data.ownerName(fruit)));
      }

      return out;
   }

   private static void restore(MinecraftServer server, List<GachaGameTests.Saved> saved) {
      WorldFruitData data = WorldFruitData.get(server);

      for (GachaGameTests.Saved s : saved) {
         data.set(s.fruit().id(), s.state(), s.instance(), s.owner(), s.name());
      }
   }

   private static void allFree(MinecraftServer server) {
      for (DevilFruit fruit : FruitRegistry.playable()) {
         WorldFruitData.get(server).setFree(fruit);
      }
   }

   private static FakePlayer opener(GameTestHelper helper, String name) {
      FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
      BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
      player.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
      player.getInventory().clearContent();
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)DFItems.CASKET.get()));
      player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack((ItemLike)DFItems.GOLD_KEY.get(), 2));
      return player;
   }

   @Nullable
   private static DevilFruit heldFruit(FakePlayer player) {
      for (ItemStack stack : player.getInventory().items) {
         DevilFruit fruit = FruitRegistry.byItem(stack.getItem());
         if (fruit != null) {
            return fruit;
         }
      }

      return null;
   }

   @GameTest(
      template = "platform"
   )
   public static void casketsNeverHoldTheSameFruitTwice(GameTestHelper helper) {
      MinecraftServer server = helper.getLevel().getServer();
      List<GachaGameTests.Saved> saved = save(server);

      try {
         allFree(server);
         int rollable = GachaRoll.candidates(server).size();
         helper.assertTrue(rollable >= 2, "the mod ships several casket fruits, got " + rollable);
         Set<ResourceLocation> found = new HashSet<>();

         for (int i = 0; i < rollable; i++) {
            FakePlayer player = opener(helper, "casket_test_" + i);
            CasketOpening.Outcome outcome = CasketOpening.open(player, player.getMainHandItem());
            helper.assertTrue(outcome == CasketOpening.Outcome.OPENED, "casket " + i + " opens, got " + outcome);
            helper.assertTrue(player.getMainHandItem().isEmpty() && player.getOffhandItem().getCount() == 1, "one casket and one key used up");
            helper.assertTrue(CasketOpening.pending(player) && heldFruit(player) == null, "the fruit waits for the end of the reveal");
            CasketOpening.deliverNow(player);
            DevilFruit fruit = heldFruit(player);
            helper.assertTrue(fruit != null && found.add(fruit.id()), "a fresh fruit each time, got " + fruit);
            WorldFruitData data = WorldFruitData.get(server);
            helper.assertTrue(data.state(fruit) == WorldFruitData.State.ITEM, fruit.id() + " is reserved as an item");
            player.getInventory().clearContent();
         }

         FakePlayer late = opener(helper, "casket_test_late");
         helper.assertTrue(CasketOpening.refusal(late) != null, "with every fruit taken the casket refuses");
         helper.assertTrue(CasketOpening.open(late, late.getMainHandItem()) == CasketOpening.Outcome.NO_FRUIT, "and does not open");
         helper.assertTrue(late.getMainHandItem().is((Item)DFItems.CASKET.get()) && late.getOffhandItem().getCount() == 2, "nothing is used up");
         late.getInventory().clearContent();
      } finally {
         restore(server, saved);
      }

      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void aTakenFruitIsNeverRolled(GameTestHelper helper) {
      MinecraftServer server = helper.getLevel().getServer();
      List<GachaGameTests.Saved> saved = save(server);

      try {
         allFree(server);
         List<DevilFruit> fruits = GachaRoll.candidates(server).stream().map(GachaRoll.Candidate::value).toList();
         DevilFruit left = fruits.get(fruits.size() - 1);

         for (DevilFruit fruit : fruits) {
            if (fruit != left) {
               if (fruit.isLogia()) {
                  WorldFruitData.get(server).setEaten(fruit, UUID.randomUUID(), "someone");
               } else {
                  FruitRules.createWorldFruit(server, fruit);
               }
            }
         }

         RandomSource random = RandomSource.create(3L);

         for (int i = 0; i < 300; i++) {
            Optional<GachaRoll.Candidate<DevilFruit>> c = GachaRoll.roll(server, random);
            helper.assertTrue(c.isPresent() && c.get().value() == left, "only the free fruit comes up, got " + c.map(GachaRoll.Candidate::value));
         }
      } finally {
         restore(server, saved);
      }

      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void creativeOpensWithoutUsingAnythingUp(GameTestHelper helper) {
      MinecraftServer server = helper.getLevel().getServer();
      List<GachaGameTests.Saved> saved = save(server);

      try {
         allFree(server);
         FakePlayer player = opener(helper, "casket_test_creative");
         player.getAbilities().instabuild = true;
         helper.assertTrue(CasketOpening.open(player, player.getMainHandItem()) == CasketOpening.Outcome.OPENED, "it opens");
         CasketOpening.deliverNow(player);
         helper.assertTrue(player.getMainHandItem().is((Item)DFItems.CASKET.get()) && player.getOffhandItem().getCount() == 2, "casket and keys stay");
         player.getAbilities().instabuild = false;
         player.getInventory().clearContent();
      } finally {
         restore(server, saved);
      }

      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void anInterruptedUnlockUsesNothingUp(GameTestHelper helper) {
      FakePlayer player = opener(helper, "casket_test_cancel");
      ItemStack casket = player.getMainHandItem();
      player.startUsingItem(InteractionHand.MAIN_HAND);
      helper.assertTrue(player.isUsingItem() && player.getUseItemRemainingTicks() == CasketItem.useTicks(), "the unlock takes openSeconds");
      player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
      casket.getItem().onUseTick(helper.getLevel(), player, casket, 10);
      helper.assertTrue(!player.isUsingItem(), "the key left the off hand: the unlock stops");
      helper.assertTrue(player.getMainHandItem().is((Item)DFItems.CASKET.get()) && !CasketOpening.pending(player), "and nothing happened");
      player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK));
      helper.assertTrue(CasketOpening.open(player, player.getMainHandItem()) == CasketOpening.Outcome.NO_KEY, "a stick is not a key");
      helper.assertTrue(player.getMainHandItem().is((Item)DFItems.CASKET.get()) && player.getOffhandItem().is(Items.STICK), "nothing used up");
      player.getInventory().clearContent();
      helper.succeed();
   }

   private static ItemEntity drop(GameTestHelper helper, DevilFruit fruit) {
      ItemStack stack = FruitRules.createWorldFruit(helper.getLevel().getServer(), fruit);
      Vec3 at = helper.absoluteVec(new Vec3(2.5, 2.0, 2.5));
      ItemEntity entity = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, stack, 0.0, 0.0, 0.0);
      helper.getLevel().addFreshEntity(entity);
      return entity;
   }

   @GameTest(
      template = "platform"
   )
   public static void aDestroyedFruitReturnsToThePool(GameTestHelper helper) {
      MinecraftServer server = helper.getLevel().getServer();
      List<GachaGameTests.Saved> saved = save(server);

      try {
         allFree(server);
         DevilFruit fruit = GachaRoll.candidates(server).get(0).value();
         WorldFruitData data = WorldFruitData.get(server);
         ItemEntity entity = drop(helper, fruit);
         helper.assertTrue(entity.getAge() == -32768, "a world fruit never despawns (unlimited lifetime), age " + entity.getAge());

         for (DamageSource fire : List.of(helper.getLevel().damageSources().lava(), helper.getLevel().damageSources().inFire())) {
            entity.hurt(fire, 100.0F);
            helper.assertTrue(entity.isAlive() && data.state(fruit) == WorldFruitData.State.ITEM, "fire and lava cannot destroy it (fire resistant)");
         }

         entity.hurt(helper.getLevel().damageSources().cactus(), 100.0F);
         helper.assertTrue(!entity.isAlive() && data.state(fruit) == WorldFruitData.State.FREE, "a cactus destroys it: free again");
         entity = drop(helper, fruit);
         entity.hurt(helper.getLevel().damageSources().explosion(null, null), 100.0F);
         helper.assertTrue(!entity.isAlive() && data.state(fruit) == WorldFruitData.State.FREE, "an explosion destroys it: free again");
         entity = drop(helper, fruit);
         entity.setPos(entity.getX(), (double)(helper.getLevel().getMinBuildHeight() - 64), entity.getZ());
         entity.discard();
         helper.assertTrue(data.state(fruit) == WorldFruitData.State.FREE, "the void takes it: free again");
         entity = drop(helper, fruit);
         UUID other = UUID.randomUUID();
         data.setItem(fruit, other);
         entity.hurt(helper.getLevel().damageSources().cactus(), 100.0F);
         helper.assertTrue(
            data.state(fruit) == WorldFruitData.State.ITEM && data.instance(fruit).filter(other::equals).isPresent(),
            "a stale copy that is destroyed frees nothing"
         );
      } finally {
         restore(server, saved);
      }

      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void aChestHoldsAtMostOneCasket(GameTestHelper helper) {
      LootContext context = new Builder(new net.minecraft.world.level.storage.loot.LootParams.Builder(helper.getLevel()).create(LootContextParamSets.EMPTY))
         .withQueriedLootTableId(DevilFruits.id("test/chest"))
         .create(Optional.empty());
      CasketLootModifier modifier = new CasketLootModifier(new LootItemCondition[0], 1.0F, false);
      ObjectArrayList<ItemStack> loot = new ObjectArrayList();
      loot.add(new ItemStack(Items.GOLD_INGOT));
      loot = modifier.apply(loot, context);
      loot = modifier.apply(loot, context);
      long caskets = loot.stream().filter(s -> s.is((Item)DFItems.CASKET.get())).count();
      helper.assertTrue(caskets == 1L && loot.size() == 2, "the chest keeps its loot and gains one casket: " + loot);
      helper.assertTrue(loot.stream().noneMatch(s -> FruitRegistry.byItem(s.getItem()) != null), "and never a fruit itself");
      helper.assertTrue(
         CasketLootModifier.tableChances().containsKey(ResourceLocation.withDefaultNamespace("chests/ancient_city")), "the config lists the default chests"
      );
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void threeGoldNuggetsMakeAGoldenKey(GameTestHelper helper) {
      ItemStack nugget = new ItemStack(Items.GOLD_NUGGET);
      CraftingInput input = CraftingInput.of(3, 1, List.of(nugget, nugget.copy(), nugget.copy()));
      Optional<RecipeHolder<CraftingRecipe>> recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
      helper.assertTrue(recipe.isPresent(), "a shapeless recipe takes three nuggets");
      ItemStack out = ((CraftingRecipe)recipe.get().value()).assemble(input, helper.getLevel().registryAccess());
      helper.assertTrue(out.is((Item)DFItems.GOLD_KEY.get()) && out.getCount() == 1, "and gives one Golden Key, got " + out);
      helper.succeed();
   }

   private static record Saved(DevilFruit fruit, WorldFruitData.State state, @Nullable UUID instance, @Nullable UUID owner, String name) {
   }
}
