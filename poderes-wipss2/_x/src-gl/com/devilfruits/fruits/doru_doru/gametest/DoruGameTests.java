package com.devilfruits.fruits.doru_doru.gametest;

import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitRegistry;
import com.devilfruits.core.gacha.CasketOpening;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.WorldFruitData;
import com.devilfruits.fruits.doru_doru.DoruBlocks;
import com.devilfruits.fruits.doru_doru.DoruDoru;
import com.devilfruits.fruits.doru_doru.DoruItems;
import com.devilfruits.fruits.doru_doru.ability.ChampionForm;
import com.devilfruits.fruits.doru_doru.ability.WaxAtelierAbility;
import com.devilfruits.fruits.doru_doru.ability.WaxToolsAbility;
import com.devilfruits.fruits.doru_doru.atelier.BuildEngine;
import com.devilfruits.fruits.doru_doru.atelier.TechniqueGen;
import com.devilfruits.fruits.doru_doru.atelier.VoxelSet;
import com.devilfruits.fruits.doru_doru.atelier.WaxIntegrity;
import com.devilfruits.fruits.doru_doru.coating.WaxCoating;
import com.devilfruits.fruits.doru_doru.item.WaxTools;
import com.devilfruits.fruits.doru_doru.wax.WaxBlock;
import com.devilfruits.fruits.doru_doru.wax.WaxBlockEntity;
import com.devilfruits.fruits.doru_doru.wax.WaxHeatService;
import com.devilfruits.fruits.doru_doru.wax.WaxMask;
import com.devilfruits.fruits.doru_doru.wax.WaxOwners;
import com.devilfruits.fruits.doru_doru.wax.WaxWorld;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFItems;
import com.devilfruits.registry.DFTags;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("devilfruits")
@PrefixGameTestTemplate(false)
public final class DoruGameTests {
   private static final String PLATFORM = "platform";

   private static FakePlayer user(GameTestHelper helper, BlockPos relative) {
      FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "doru_test"));
      BlockPos pos = helper.absolutePos(relative);
      player.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
      player.setDeltaMovement(Vec3.ZERO);
      WorldFruitData.get(helper.getLevel().getServer()).setFree(DoruDoru.fruit());
      FruitRules.grantPower(player, DoruDoru.fruit(), false);
      DFAttachments.data(player).setEnergy(1000.0F);
      return player;
   }

   private static void done(GameTestHelper helper, FakePlayer player) {
      AbilityManager.cancelAll(player);
      AbilityManager.forget(player.getUUID());
      WaxTools.dissolveAll(player, false);
      BuildEngine.cancel(player, false);
      FruitRules.removePower(player);
      WorldFruitData.get(helper.getLevel().getServer()).setFree(DoruDoru.fruit());
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void maskBits(GameTestHelper helper) {
      long m = WaxMask.with(WaxMask.with(0L, 0, 0, 0), 3, 2, 1);
      helper.assertTrue(WaxMask.count(m) == 2 && WaxMask.has(m, 3, 2, 1) && !WaxMask.has(m, 1, 1, 1), "two cells set");
      helper.assertTrue(WaxMask.top(m) == 2, "top layer is y 2");
      long turned = m;

      for (int i = 0; i < 4; i++) {
         turned = WaxMask.rotateY(turned, 1);
      }

      helper.assertTrue(turned == m, "four quarter turns are the identity");
      helper.assertTrue(WaxMask.mirrorX(WaxMask.mirrorX(m)) == m && WaxMask.count(-1L) == 64, "mirror twice, full count");
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void pourAndRemove(GameTestHelper helper) {
      ServerLevel level = helper.getLevel();
      UUID id = UUID.randomUUID();
      WaxOwners owners = WaxOwners.get(level.getServer());
      int owner = owners.index(id);
      BlockPos full = helper.absolutePos(new BlockPos(1, 2, 1));
      BlockPos part = helper.absolutePos(new BlockPos(3, 2, 1));
      WaxWorld.pour(level, full, -1L, 0, owner, 0, 0L);
      WaxWorld.pour(level, part, WaxMask.with(0L, 1, 0, 1), 3, owner, 0, 0L);
      helper.assertTrue(
         level.getBlockState(full).is((Block)DoruBlocks.WAX.get()) && (Boolean)level.getBlockState(full).getValue(WaxBlock.FULL), "a full wax block"
      );
      helper.assertTrue(
         level.getBlockState(part).is((Block)DoruBlocks.WAX.get()) && !(Boolean)level.getBlockState(part).getValue(WaxBlock.FULL), "a partial one"
      );
      helper.assertTrue(owners.count(id) == 2, "two blocks counted, got " + owners.count(id));
      if (level.getBlockEntity(part) instanceof WaxBlockEntity be) {
         helper.assertTrue(be.color() == 3 && WaxMask.count(be.mask()) == 1, "dye and mask kept");
         WaxWorld.setMask(level, part, be, 0L);
      }

      helper.assertTrue(level.getBlockState(part).isAir() && owners.count(id) == 1, "the empty mask removed it");
      WaxWorld.remove(level, full);
      helper.succeed();
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 400
   )
   public static void lavaMeltsWax(GameTestHelper helper) {
      ServerLevel level = helper.getLevel();
      BlockPos lava = helper.absolutePos(new BlockPos(0, 2, 0));
      BlockPos near = lava.east();
      BlockPos far = helper.absolutePos(new BlockPos(4, 2, 4));
      level.setBlockAndUpdate(lava, Blocks.LAVA_CAULDRON.defaultBlockState());
      int owner = WaxOwners.get(level.getServer()).index(UUID.randomUUID());
      int poured = WaxWorld.pour(level, near, -1L, 0, owner, 1, 0L) + WaxWorld.pour(level, far, -1L, 0, owner, 1, 0L);
      helper.assertTrue(poured == 128, "both blocks poured");
      helper.assertTrue(WaxHeatService.nearHeat(level, near) && !WaxHeatService.nearHeat(level, far), "heat seen only next to the lava");
      helper.succeedWhen(() -> {
         boolean var10000;
         label23: {
            if (level.getBlockEntity(near) instanceof WaxBlockEntity be && WaxMask.count(be.mask()) >= 64) {
               var10000 = false;
               break label23;
            }

            var10000 = true;
         }

         boolean var10001;
         label18: {
            boolean melting = var10000;
            helper.assertTrue(melting, "the wax by the lava is running");
            if (level.getBlockEntity(far) instanceof WaxBlockEntity f && f.mask() == -1L && f.melt() == 0) {
               var10001 = true;
               break label18;
            }

            var10001 = false;
         }

         helper.assertTrue(var10001, "the far wax stands solid");
         level.setBlockAndUpdate(lava, Blocks.AIR.defaultBlockState());
         WaxWorld.remove(level, far);
      });
   }

   @GameTest(
      template = "platform"
   )
   public static void structureRule(GameTestHelper helper) {
      Long2LongOpenHashMap ledge = new Long2LongOpenHashMap();
      Long2LongOpenHashMap beam = new Long2LongOpenHashMap();

      for (int y = 0; y < 3; y++) {
         ledge.put(BlockPos.asLong(0, y, 0), -1L);
         beam.put(BlockPos.asLong(0, y, 0), -1L);
      }

      ledge.put(BlockPos.asLong(1, 2, 0), -1L);
      long rod = 0L;

      for (int c = 0; c < 4; c++) {
         rod = WaxMask.with(rod, c, 3, 1);
      }

      for (int x = 1; x <= 20; x++) {
         beam.put(BlockPos.asLong(x, 2, 0), rod);
      }

      WaxIntegrity.Rules rules = WaxAtelierAbility.rules();
      WaxIntegrity.Result held = WaxIntegrity.check(view(ledge), ledge.keySet(), rules);
      WaxIntegrity.Result fell = WaxIntegrity.check(view(beam), beam.keySet(), rules);
      helper.assertTrue(!held.anyUnsupported(), "a one-block ledge stands");
      helper.assertTrue(
         fell.anyUnsupported() && fell.of(BlockPos.asLong(20, 2, 0)) == WaxIntegrity.Stress.UNSUPPORTED, "the far end of a 20-block thin beam is unsupported"
      );
      helper.assertTrue(held.of(BlockPos.asLong(0, 0, 0)) != WaxIntegrity.Stress.UNSUPPORTED, "the post is anchored");
      helper.succeed();
   }

   private static WaxIntegrity.View view(final Long2LongOpenHashMap wax) {
      return new WaxIntegrity.View() {
         @Override
         public long mask(long pos) {
            return wax.get(pos);
         }

         @Override
         public boolean anchor(long pos, Direction face) {
            return BlockPos.getY(pos) < 0 && face == Direction.UP;
         }
      };
   }

   @GameTest(
      template = "platform"
   )
   public static void techniqueShapes(GameTestHelper helper) {
      Vec3 base = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2)));
      VoxelSet wall = TechniqueGen.wall(base, new Vec3(0.0, 0.0, 1.0), 7.0, 5.0, 1.5, 1.0);
      helper.assertTrue(wall.blockEquivalents() > 20.0F && wall.blockEquivalents() < 120.0F, "a wall of sensible size: " + wall.blockEquivalents());
      VoxelSet dome = TechniqueGen.dome(base, 3.0, 0.75);
      helper.assertTrue(!dome.has((int)Math.floor(base.x * 4.0), (int)Math.floor(base.y * 4.0) + 4, (int)Math.floor(base.z * 4.0)), "the dome is hollow");
      TechniqueGen.Service s = TechniqueGen.service(base, 8, 7L);
      helper.assertTrue(s.candles().size() == 9 && !s.stand().isEmpty(), "eight ring candles, the middle one, a stand");
      TechniqueGen.Candle middle = s.candles().get(8);
      helper.assertTrue(middle.height() == (int)Math.round(12.0), "the middle candle's height");
      VoxelSet layer = new VoxelSet();
      middle.layer(middle.top(), layer);
      helper.assertTrue(!layer.isEmpty() && s.all().size() > s.stand().size(), "a candle layer, the candles in the whole");
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void coatingStacksAndEncases(GameTestHelper helper) {
      Zombie zombie = (Zombie)helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
      zombie.setNoAi(true);
      WaxCoating.expose(zombie, 5, null);
      WaxCoating.tick(zombie);
      helper.assertTrue(zombie.hasData(WaxCoating.TYPE) && WaxCoating.get(zombie).progress() > 0.0F, "partial progress kept");
      WaxCoating.expose(zombie, 5, null);
      helper.assertTrue(WaxCoating.get(zombie).stacks() >= 1, "a stack after ten still ticks");
      WaxCoating.add(zombie, 10, null);
      long now = helper.getLevel().getGameTime();
      helper.assertTrue(WaxCoating.get(zombie).encased(now), "ten stacks encase");
      helper.assertTrue(WaxCoating.get(zombie).immuneUntil() > WaxCoating.get(zombie).encasedUntil(), "immunity after the shell");
      zombie.discard();
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void coatingShakesOffPlayers(GameTestHelper helper) {
      FakePlayer victim = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "doru_victim"));
      BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
      victim.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
      WaxCoating.add(victim, 3, null);
      helper.assertTrue(WaxCoating.get(victim).stacks() == 3, "three stacks on the player");
      int perStack = (int)Math.ceil(DoruDoru.setting("coatingShakesPerStack"));

      for (int i = 0; i < perStack; i++) {
         WaxCoating.forget(victim.getUUID());
         WaxCoating.shake(victim);
      }

      helper.assertTrue(WaxCoating.get(victim).stacks() == 2, "a stack shaken off");
      WaxCoating.add(victim, 10, null);
      long now = helper.getLevel().getGameTime();
      helper.assertTrue(WaxCoating.get(victim).encasedUntil() == Long.MAX_VALUE, "the player's shell has no end time");

      for (int i = 0; i < (int)Math.ceil(DoruDoru.setting("encaseShakes")); i++) {
         WaxCoating.forget(victim.getUUID());
         WaxCoating.shake(victim);
      }

      WaxCoating.Data d = WaxCoating.get(victim);
      helper.assertTrue(!d.encased(now) && d.stacks() == 0 && d.immuneUntil() > now, "shaken out of the shell, immune for a while");
      victim.removeData(WaxCoating.TYPE);
      WaxCoating.forget(victim.getUUID());
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void waxToolGivesStackBack(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE, 3));
      WaxTools.form(player, WaxTools.Kind.PICKAXE, 0);
      ItemStack held = player.getMainHandItem();
      helper.assertTrue(held.is((Item)DoruItems.WAX_PICKAXE.get()), "a wax pickaxe in hand");
      WaxTools.Bound b = (WaxTools.Bound)held.get((DataComponentType)DoruItems.BOUND.get());
      helper.assertTrue(b != null && b.original().is(Items.APPLE) && b.original().getCount() == 3, "the apples kept on the tool");
      WaxTools.dissolveAll(player, false);
      helper.assertTrue(player.getMainHandItem().is(Items.APPLE) && player.getMainHandItem().getCount() == 3, "the apples are back");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void championNeedsTheForm(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      helper.assertTrue(!ChampionForm.in(player), "the base form is not the Champion");
      helper.assertTrue(DoruDoru.fruit().form("champion") != null, "the Champion form is registered");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void waxKeyNeedsAGoldenKey(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      player.getInventory().clearContent();
      player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH, 5));
      helper.assertTrue(!WaxTools.hasMold(player), "no Golden Key: no Wax Key on the wheel");
      WaxToolsAbility.choose(player, WaxTools.KEY_CHOICE);
      helper.assertTrue(player.getOffhandItem().is(Items.TORCH), "and picking it forms nothing");
      player.getInventory().setItem(9, new ItemStack((ItemLike)DFItems.GOLD_KEY.get()));
      helper.assertTrue(WaxTools.hasMold(player), "a Golden Key in the inventory offers it");
      float energy = DFAttachments.data(player).energy();
      WaxToolsAbility.choose(player, WaxTools.KEY_CHOICE);
      ItemStack key = player.getOffhandItem();
      helper.assertTrue(key.is((Item)DoruItems.WAX_KEY.get()), "the Wax Key sets in the off hand");
      WaxTools.Bound b = (WaxTools.Bound)key.get((DataComponentType)DoruItems.BOUND.get());
      helper.assertTrue(b != null && b.original().is(Items.TORCH) && b.original().getCount() == 5, "the torches wait inside it");
      helper.assertTrue(DFAttachments.data(player).energy() < energy, "it costs energy");
      helper.assertTrue(key.is(DFTags.CASKET_KEYS), "it is a casket key");
      WaxTools.dissolveAll(player, false);
      helper.assertTrue(player.getOffhandItem().is(Items.TORCH) && player.getOffhandItem().getCount() == 5, "the torches come back");
      player.getInventory().clearContent();
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void waxKeyOpensACasketAndKeepsTheGoldenKey(GameTestHelper helper) {
      MinecraftServer server = helper.getLevel().getServer();
      WorldFruitData world = WorldFruitData.get(server);
      List<Object[]> saved = new ArrayList<>();

      for (DevilFruit f : FruitRegistry.playable()) {
         if (f != DoruDoru.fruit()) {
            saved.add(new Object[]{f, world.state(f), world.instance(f).orElse(null), world.owner(f).orElse(null), world.ownerName(f)});
            world.setFree(f);
         }
      }

      FakePlayer player = user(helper, new BlockPos(2, 1, 2));

      try {
         player.getInventory().clearContent();
         player.getInventory().setItem(9, new ItemStack((ItemLike)DFItems.GOLD_KEY.get()));
         player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH));
         helper.assertTrue(WaxTools.formKey(player, 0), "the Wax Key forms");
         player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)DFItems.CASKET.get()));
         CasketOpening.Outcome outcome = CasketOpening.open(player, player.getMainHandItem());
         helper.assertTrue(outcome == CasketOpening.Outcome.OPENED, "the casket opens, got " + outcome);
         helper.assertTrue(player.getInventory().getItem(9).is((Item)DFItems.GOLD_KEY.get()), "the Golden Key is not used up");
         helper.assertTrue(player.getOffhandItem().is(Items.TORCH), "the Wax Key melted back, the torch is in the off hand again");
         CasketOpening.deliverNow(player);
      } finally {
         for (Object[] e : saved) {
            world.set(((DevilFruit)e[0]).id(), (WorldFruitData.State)e[1], (UUID)e[2], (UUID)e[3], (String)e[4]);
         }

         player.getInventory().clearContent();
      }

      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void waxKeyMeltsAndKeepsToTheOffHand(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      player.getInventory().clearContent();
      player.getInventory().setItem(9, new ItemStack((ItemLike)DFItems.GOLD_KEY.get()));
      player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH));
      WaxTools.formKey(player, 0);
      ItemStack key = player.getOffhandItem();
      key.setDamageValue(key.getMaxDamage() - 1);
      player.setRemainingFireTicks(100);
      player.tickCount = 40;
      key.inventoryTick(helper.getLevel(), player, 0, false);
      helper.assertTrue(player.getOffhandItem().is(Items.TORCH), "on fire, the soft key melts and the torch is back");
      player.clearFire();
      WaxTools.formKey(player, 0);
      ItemStack again = player.getOffhandItem();
      helper.assertTrue(again.is((Item)DoruItems.WAX_KEY.get()), "a second key forms");
      player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
      player.getInventory().setItem(3, again);
      again.inventoryTick(helper.getLevel(), player, 3, false);
      helper.assertTrue(!player.getInventory().getItem(3).is((Item)DoruItems.WAX_KEY.get()), "put away from the off hand, it dissolves");
      helper.assertTrue(player.getInventory().getItem(3).is(Items.TORCH), "and the torch it held comes back where it was (like a moved tool's stack)");
      helper.assertTrue(player.getInventory().getItem(9).is((Item)DFItems.GOLD_KEY.get()), "the Golden Key stays");
      player.getInventory().clearContent();
      done(helper, player);
   }
}
