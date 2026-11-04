package com.devilfruits.core.gametest;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.poneglyph.BlueprintAssemblyRecipe;
import com.devilfruits.core.poneglyph.BlueprintData;
import com.devilfruits.core.poneglyph.Cipher;
import com.devilfruits.core.poneglyph.Knowledge;
import com.devilfruits.core.poneglyph.PoneglyphData;
import com.devilfruits.core.poneglyph.PoneglyphPlayerData;
import com.devilfruits.core.poneglyph.PoneglyphPuzzle;
import com.devilfruits.core.poneglyph.Poneglyphs;
import com.devilfruits.core.poneglyph.RubbingService;
import com.devilfruits.core.seastone.SeastoneGear;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.registry.DFItems;
import com.devilfruits.registry.DFTags;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("devilfruits")
@PrefixGameTestTemplate(false)
public final class PoneglyphGameTests {
   private static final String PLATFORM = "platform";
   private static final ResourceLocation REFINING = DevilFruits.id("seastone_refining");
   private static final ResourceLocation TOOLS = DevilFruits.id("seastone_tools");

   private static FakePlayer player(GameTestHelper helper) {
      FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "poneglyph_test"));
      player.setGameMode(GameType.SURVIVAL);
      BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
      player.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
      return player;
   }

   private static List<String> otherWords(ResourceLocation except) {
      List<String> out = new ArrayList<>();

      for (Entry<ResourceLocation, PoneglyphData.Poneglyph> e : PoneglyphData.poneglyphs().entrySet()) {
         if (!e.getKey().equals(except)) {
            out.addAll(e.getValue().words());
         }
      }

      return out;
   }

   @GameTest(
      template = "platform"
   )
   public static void rubbingsAssembleOnlyInPlace(GameTestHelper helper) {
      ResourceLocation id = REFINING;
      List<ItemStack> grid = new ArrayList<>();

      for (int cell = 0; cell < 9; cell++) {
         grid.add(RubbingService.rubbing(id, cell));
      }

      helper.assertTrue(id.equals(BlueprintAssemblyRecipe.assembled(CraftingInput.of(3, 3, grid))), "nine rubbings in place assemble");
      List<ItemStack> swapped = new ArrayList<>(grid);
      swapped.set(0, grid.get(1));
      swapped.set(1, grid.get(0));
      helper.assertTrue(BlueprintAssemblyRecipe.assembled(CraftingInput.of(3, 3, swapped)) == null, "two fragments swapped do not");
      List<ItemStack> mixed = new ArrayList<>(grid);
      mixed.set(4, RubbingService.rubbing(TOOLS, 4));
      helper.assertTrue(BlueprintAssemblyRecipe.assembled(CraftingInput.of(3, 3, mixed)) == null, "fragments of two poneglyphs do not");
      List<ItemStack> missing = new ArrayList<>(grid);
      missing.set(8, ItemStack.EMPTY);
      helper.assertTrue(BlueprintAssemblyRecipe.assembled(CraftingInput.of(3, 3, missing)) == null, "eight fragments do not");
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void puzzleIsDeterministicWithOneAnswer(GameTestHelper helper) {
      PoneglyphData.Poneglyph def = PoneglyphData.poneglyph(REFINING);
      helper.assertTrue(def != null && PoneglyphData.poneglyphs().size() >= 5, "the poneglyph data is loaded");
      List<String> others = otherWords(REFINING);
      List<PoneglyphPuzzle.Round> a = PoneglyphPuzzle.generate(42L, 7L, 0, def.words(), others, 3, 10);
      List<PoneglyphPuzzle.Round> b = PoneglyphPuzzle.generate(42L, 7L, 0, def.words(), others, 3, 10);
      helper.assertTrue(a.equals(b), "same seed, same solver, same draw: the same puzzle");
      helper.assertTrue(!a.equals(PoneglyphPuzzle.generate(42L, 7L, 1, def.words(), others, 3, 10)), "a redraw changes it");
      Random r = new Random(1L);

      for (int n = 0; n < 200; n++) {
         int options = 4 + r.nextInt(9);

         for (PoneglyphPuzzle.Round round : PoneglyphPuzzle.generate(r.nextLong(), r.nextLong(), r.nextInt(5), def.words(), others, 3, options)) {
            helper.assertTrue(round.options().size() == options, "the asked number of options");
            helper.assertTrue(new HashSet<>(round.options()).size() == options, "no duplicates: " + round.options());
            helper.assertTrue(round.options().stream().filter(round.clue()::equals).count() == 1L, "the answer exactly once");
            helper.assertTrue(round.options().get(round.correct()).equals(round.clue()), "the index points at it");
            helper.assertTrue(round.options().stream().allMatch(w -> w.matches("[a-z]+")), "lowercase letters only (the Galactic font)");
            PoneglyphPuzzle.Round shuffled = round.reshuffled(99L);
            helper.assertTrue(shuffled.options().get(shuffled.correct()).equals(round.clue()), "a reshuffle keeps the answer");
         }
      }

      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void decoysAreGood(GameTestHelper helper) {
      List<String> others = otherWords(REFINING);

      for (String word : List.of("forge", "ore", "anvil", "blaze")) {
         List<String> decoys = PoneglyphPuzzle.decoys(word, PoneglyphData.poneglyph(REFINING).words(), others, new Random((long)word.hashCode()), 11);
         helper.assertTrue(
            decoys.size() == 11 && !decoys.contains(word) && new HashSet<>(decoys).size() == 11, "11 distinct decoys without the word: " + decoys
         );
         long sameLength = decoys.stream().filter(d -> d.length() == word.length()).count();
         helper.assertTrue(sameLength >= 8L, "mostly the same length: " + decoys);
         long sharing = decoys.stream().filter(d -> d.chars().anyMatch(ch -> word.indexOf(ch) >= 0)).count();
         helper.assertTrue(sharing >= 6L, "most share letters with the word: " + decoys);
      }

      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void mistakesLockReshuffleAndRedraw(GameTestHelper helper) {
      BlueprintData.Progress p = new BlueprintData.Progress(5, 0, 1, 0, 0L, 0, 0);
      PoneglyphPuzzle.Step wrong = PoneglyphPuzzle.pick(p, false, 3, 100L, 100, 3, false, false);
      helper.assertTrue(
         wrong.outcome() == PoneglyphPuzzle.Outcome.WRONG
            && wrong.progress().mistakes() == 1
            && wrong.progress().lockUntil() == 200L
            && wrong.progress().shuffle() == 1,
         "a mistake locks and reshuffles"
      );
      helper.assertTrue(
         PoneglyphPuzzle.pick(wrong.progress(), true, 3, 150L, 100, 3, false, false).outcome() == PoneglyphPuzzle.Outcome.LOCKED,
         "nothing counts during the lockout"
      );
      PoneglyphPuzzle.Step second = PoneglyphPuzzle.pick(wrong.progress(), false, 3, 200L, 100, 3, false, false);
      PoneglyphPuzzle.Step third = PoneglyphPuzzle.pick(second.progress(), false, 3, 400L, 100, 3, false, false);
      helper.assertTrue(
         third.outcome() == PoneglyphPuzzle.Outcome.RESET && third.progress().draw() == 1 && third.progress().round() == 0 && third.progress().mistakes() == 0,
         "the third mistake redraws the words from round one"
      );
      helper.assertTrue(
         PoneglyphPuzzle.pick(second.progress(), false, 3, 400L, 100, 3, false, true).outcome() == PoneglyphPuzzle.Outcome.DESTROYED,
         "hard mode destroys the blueprint"
      );
      PoneglyphPuzzle.Step forgiven = PoneglyphPuzzle.pick(p, false, 3, 100L, 100, 3, true, false);
      helper.assertTrue(forgiven.outcome() == PoneglyphPuzzle.Outcome.FORGIVEN && forgiven.progress().mistakes() == 0, "literacy 4 forgives one");
      helper.assertTrue(PoneglyphPuzzle.pick(forgiven.progress(), false, 3, 100L, 100, 3, true, false).outcome() == PoneglyphPuzzle.Outcome.WRONG, "only one");
      helper.assertTrue(PoneglyphPuzzle.pick(p, true, 3, 100L, 100, 3, false, false).outcome() == PoneglyphPuzzle.Outcome.CORRECT, "round 2 -> 3");
      helper.assertTrue(
         PoneglyphPuzzle.pick(new BlueprintData.Progress(5, 0, 2, 0, 0L, 0, 0), true, 3, 100L, 100, 3, false, false).outcome()
            == PoneglyphPuzzle.Outcome.SOLVED,
         "the last round solves it"
      );
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void worldboundCipherIsBijective(GameTestHelper helper) {
      Random r = new Random(3L);

      for (int n = 0; n < 100; n++) {
         int[] perm = Cipher.permutation(r.nextLong());
         helper.assertTrue(Cipher.isBijective(perm), "a permutation");

         for (int i = 0; i < 26; i++) {
            helper.assertTrue(perm[i] != i, "no letter stays itself");
         }

         int[] inverse = new int[26];
         int i = 0;

         while (i < 26) {
            inverse[perm[i]] = i++;
         }

         helper.assertTrue(Cipher.encode(Cipher.encode("seastone", perm), inverse).equals("seastone"), "it decodes back");
      }

      helper.assertTrue(Arrays.equals(Cipher.permutation(12345L), Cipher.permutation(12345L)), "stable per world seed");
      helper.assertTrue(Cipher.encode("Forge!", Cipher.identity()).equals("forge!"), "standard mode is letter for letter");
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void lockedRecipeNeedsKnowledge(GameTestHelper helper) {
      FakePlayer player = player(helper);
      CraftingMenu menu = new CraftingMenu(1, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)));

      for (int slot : new int[]{1, 2, 3}) {
         menu.getSlot(slot).set(new ItemStack((ItemLike)DFItems.SEASTONE_INGOT.get()));
      }

      menu.getSlot(5).set(new ItemStack(Items.STICK));
      menu.getSlot(8).set(new ItemStack(Items.STICK));
      helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "no pickaxe without Seastone Tools");
      Knowledge.learn(player, TOOLS);
      menu.slotsChanged(menu.getSlot(1).container);
      helper.assertTrue(menu.getSlot(0).getItem().is((Item)SeastoneGear.PICKAXE.get()), "the pickaxe once learned");
      helper.assertTrue(player.getRecipeBook().contains(DevilFruits.id("seastone/seastone_pickaxe")), "and it is in the recipe book");
      Knowledge.revoke(player, TOOLS);
      menu.slotsChanged(menu.getSlot(1).container);
      helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "revoked: locked again");
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void branchesTeachRefiningAlong(GameTestHelper helper) {
      FakePlayer player = player(helper);
      helper.assertTrue(Knowledge.learn(player, TOOLS).result() == Knowledge.Result.LEARNED, "no prerequisite blocks a blueprint");
      helper.assertTrue(Knowledge.knows(player, REFINING), "refining comes along, so the ingots can be made");
      helper.assertTrue(player.getRecipeBook().contains(DevilFruits.id("seastone/seastone_ingot")), "its recipes too");
      helper.assertTrue(Knowledge.learn(player, TOOLS).result() == Knowledge.Result.ALREADY, "once");
      helper.assertTrue(Knowledge.learn(player, REFINING).result() == Knowledge.Result.ALREADY, "refining is known");
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void attachmentsSaveAndSurviveDeath(GameTestHelper helper) {
      FakePlayer player = player(helper);
      player.setData(Poneglyphs.KNOWLEDGE, PoneglyphPlayerData.PlayerKnowledge.EMPTY.plus(REFINING));
      player.setData(Poneglyphs.LITERACY, new PoneglyphPlayerData.PoneglyphLiteracy(3, 150));
      player.setData(Poneglyphs.RUBBING_PROGRESS, PoneglyphPlayerData.RubbingProgress.EMPTY.with(REFINING, 4, 12345L));
      PoneglyphPlayerData.PlayerKnowledge k = (PoneglyphPlayerData.PlayerKnowledge)PoneglyphPlayerData.PlayerKnowledge.CODEC
         .parse(
            JsonOps.INSTANCE,
            (JsonElement)PoneglyphPlayerData.PlayerKnowledge.CODEC
               .encodeStart(JsonOps.INSTANCE, (PoneglyphPlayerData.PlayerKnowledge)player.getData(Poneglyphs.KNOWLEDGE))
               .getOrThrow()
         )
         .getOrThrow();
      PoneglyphPlayerData.RubbingProgress rp = (PoneglyphPlayerData.RubbingProgress)PoneglyphPlayerData.RubbingProgress.CODEC
         .parse(
            JsonOps.INSTANCE,
            (JsonElement)PoneglyphPlayerData.RubbingProgress.CODEC
               .encodeStart(JsonOps.INSTANCE, (PoneglyphPlayerData.RubbingProgress)player.getData(Poneglyphs.RUBBING_PROGRESS))
               .getOrThrow()
         )
         .getOrThrow();
      PoneglyphPlayerData.PoneglyphLiteracy lit = (PoneglyphPlayerData.PoneglyphLiteracy)PoneglyphPlayerData.PoneglyphLiteracy.CODEC
         .parse(
            JsonOps.INSTANCE,
            (JsonElement)PoneglyphPlayerData.PoneglyphLiteracy.CODEC
               .encodeStart(JsonOps.INSTANCE, (PoneglyphPlayerData.PoneglyphLiteracy)player.getData(Poneglyphs.LITERACY))
               .getOrThrow()
         )
         .getOrThrow();
      helper.assertTrue(
         k.knows(REFINING) && rp.readyAt(REFINING, 4) == 12345L && rp.readyAt(REFINING, 3) == 0L && lit.level() == 3 && lit.xp() == 150,
         "saved and loaded the same"
      );
      FakePlayer reborn = player(helper);
      NeoForge.EVENT_BUS.post(new Clone(reborn, player, true));
      helper.assertTrue(
         Knowledge.knows(reborn, REFINING)
            && ((PoneglyphPlayerData.PoneglyphLiteracy)reborn.getData(Poneglyphs.LITERACY)).level() == 3
            && ((PoneglyphPlayerData.RubbingProgress)reborn.getData(Poneglyphs.RUBBING_PROGRESS)).readyAt(REFINING, 4) == 12345L,
         "kept through death"
      );
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void poneglyphBlocksAreImmune(GameTestHelper helper) {
      FakePlayer player = player(helper);
      BlockPos stone = new BlockPos(1, 2, 1);
      BlockPos face = new BlockPos(3, 2, 1);
      helper.setBlock(stone, (Block)Poneglyphs.PONEGLYPH_STONE.get());
      helper.setBlock(face, (Block)Poneglyphs.PONEGLYPH_FACE.get());

      for (BlockPos rel : new BlockPos[]{stone, face}) {
         BlockPos pos = helper.absolutePos(rel);
         BlockState state = helper.getLevel().getBlockState(pos);
         helper.assertTrue(state.is(Poneglyphs.UNBREAKABLE) && state.is(DFTags.DESTRUCTION_IMMUNE), "tagged immune");
         helper.assertTrue(!TerrainDestruction.canAffect(helper.getLevel(), player, pos, state, true), "no technique tears it out");
         helper.assertTrue(!TerrainDestruction.canAffect(helper.getLevel(), player, pos, state, false), "nor breaks it by hardness");
         helper.assertTrue(
            state.getBlock().getExplosionResistance() >= 3000000.0F && state.getDestroySpeed(helper.getLevel(), pos) < 0.0F, "blast-proof and unbreakable"
         );
         helper.assertTrue(state.getPistonPushReaction() == PushReaction.BLOCK, "pistons cannot move it");
      }

      helper.succeed();
   }
}
