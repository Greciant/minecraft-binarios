package com.devilfruits.fruits.gura_gura.gametest;

import com.devilfruits.core.CoreEvents;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.CastResult;
import com.devilfruits.core.combat.AbilityProjectiles;
import com.devilfruits.core.combat.Stun;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.core.rules.WorldFruitData;
import com.devilfruits.core.seastone.SeastoneCombat;
import com.devilfruits.core.seastone.SeastoneGear;
import com.devilfruits.fruits.gura_gura.GuraAbilities;
import com.devilfruits.fruits.gura_gura.GuraGura;
import com.devilfruits.fruits.gura_gura.GuraItems;
import com.devilfruits.fruits.gura_gura.ability.QuakeDeflect;
import com.devilfruits.fruits.suna_suna.SunaAbilities;
import com.devilfruits.fruits.suna_suna.SunaSuna;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFDataComponents;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@GameTestHolder("devilfruits")
@PrefixGameTestTemplate(false)
public final class GuraGameTests {
   private static final String PLATFORM = "platform";

   private static DevilFruit fruit() {
      return GuraGura.fruit();
   }

   private static FakePlayer player(GameTestHelper helper, BlockPos relative) {
      FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "gura_test"));
      BlockPos pos = helper.absolutePos(relative);
      player.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
      return player;
   }

   private static void freeWorldFruit(GameTestHelper helper) {
      WorldFruitData.get(helper.getLevel().getServer()).setFree(fruit());
   }

   private static void cleanup(GameTestHelper helper, FakePlayer... players) {
      for (FakePlayer p : players) {
         AbilityManager.cancelAll(p);
         AbilityManager.forget(p.getUUID());
         FruitRules.removePower(p);
      }

      freeWorldFruit(helper);
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 40
   )
   public static void quakeBatsProjectilesAwayAndCrumblesSand(GameTestHelper helper) {
      FakePlayer gura = player(helper, new BlockPos(2, 2, 2));
      ServerLevel level = helper.getLevel();
      Arrow arrow = (Arrow)EntityType.ARROW.create(level);
      Vec3 eye = gura.getEyePosition();
      arrow.moveTo(eye.x, eye.y, eye.z + 2.5);
      arrow.setDeltaMovement(0.0, 0.0, -1.5);
      level.addFreshEntity(arrow);
      QuakeDeflect.sweep(gura, ImpactShape.cone(eye, new Vec3(0.0, 0.0, 1.0), 6.0, Math.toRadians(40.0)), new Vec3(0.0, 0.0, 1.0));
      helper.assertTrue(
         arrow.getDeltaMovement().z > 1.0 && arrow.getOwner() == gura, "the arrow flies back along the blow, now the user's: " + arrow.getDeltaMovement()
      );
      arrow.discard();
      FakePlayer suna = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "suna_thrower"));
      BlockPos at = helper.absolutePos(new BlockPos(0, 2, 0));
      suna.moveTo((double)at.getX() + 0.5, (double)at.getY(), (double)at.getZ() + 0.5, 0.0F, 0.0F);
      FruitRules.grantPower(suna, SunaSuna.fruit(), false);
      Husk husk = (Husk)helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(0, 2, 4));
      float health = husk.getHealth();
      AbilityManager.press(suna, SunaAbilities.SABLES_PESADO);
      AbilityManager.release(suna, SunaAbilities.SABLES_PESADO);
      List<AbilityProjectiles.Flight> flying = AbilityProjectiles.active(level);
      helper.assertTrue(flying.size() == 1, "the ball of sand is in the air: " + flying.size());
      int broken = QuakeDeflect.sweep(gura, ImpactShape.sphere(flying.get(0).position(), 2.0), new Vec3(-1.0, 0.0, 0.0));
      helper.assertTrue(broken == 1 && AbilityProjectiles.active(level).isEmpty(), "the quake crumbles it");
      helper.runAfterDelay(10L, () -> {
         helper.assertTrue(husk.getHealth() == health, "so it never lands");
         husk.discard();
         cleanup(helper, gura, suna);
         WorldFruitData.get(level.getServer()).setFree(SunaSuna.fruit());
         helper.succeed();
      });
   }

   @GameTest(
      template = "platform"
   )
   public static void jitteStunsAFruitUserButSparesItsHolder(GameTestHelper helper) {
      freeWorldFruit(helper);
      FakePlayer user = player(helper, new BlockPos(1, 2, 1));
      FruitRules.grantPower(user, fruit(), false);
      user.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)SeastoneGear.JITTE.get()));
      helper.assertFalse(Weakness.touchesSeastone(user), "a fruit user can hold the jitte");
      user.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)SeastoneGear.SWORD.get()));
      helper.assertTrue(Weakness.touchesSeastone(user), "but a seastone sword weakens them");
      user.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      FakePlayer marine = player(helper, new BlockPos(3, 2, 1));
      marine.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)SeastoneGear.JITTE.get()));
      SeastoneCombat.hit(user, marine.damageSources().playerAttack(marine));
      helper.assertTrue(Stun.stunned(user), "the jitte's blow stuns a fruit user");
      helper.assertTrue(user.hasEffect(SeastoneGear.SUPPRESSED), "and suppresses their powers");
      Stun.release(user);
      cleanup(helper, user, marine);
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void eatingTheFruitGrantsThePower(GameTestHelper helper) {
      freeWorldFruit(helper);
      FakePlayer player = player(helper, new BlockPos(1, 1, 1));
      ItemStack stack = FruitRules.createWorldFruit(helper.getLevel().getServer(), fruit());
      helper.assertTrue(!stack.isEmpty(), "a free world must be able to create the fruit");
      FruitRules.EatOutcome outcome = FruitRules.eat(player, fruit(), stack);
      PlayerFruitData data = DFAttachments.data(player);
      helper.assertTrue(outcome == FruitRules.EatOutcome.GRANTED, "expected GRANTED but was " + outcome);
      helper.assertTrue(GuraGura.ID.equals(data.fruitId()), "power must be granted");
      helper.assertTrue(data.energy() == (float)Mastery.maxEnergy(data), "energy must start full");
      helper.assertTrue(WorldFruitData.get(helper.getLevel().getServer()).state(fruit()) == WorldFruitData.State.EATEN, "world fruit must be marked eaten");
      cleanup(helper, player);
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void secondDevilFruitKills(GameTestHelper helper) {
      freeWorldFruit(helper);
      FakePlayer player = player(helper, new BlockPos(1, 1, 1));
      FruitRules.grantPower(player, fruit(), false);
      FruitRules.EatOutcome outcome = FruitRules.eat(player, fruit(), new ItemStack((ItemLike)GuraItems.FRUIT.get()));
      helper.assertTrue(outcome == FruitRules.EatOutcome.KILLED, "second fruit must kill, was " + outcome);
      cleanup(helper, player);
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void onlyOneFruitPerWorld(GameTestHelper helper) {
      freeWorldFruit(helper);
      FakePlayer first = player(helper, new BlockPos(1, 1, 1));
      FakePlayer second = player(helper, new BlockPos(3, 1, 3));
      FruitRules.eat(first, fruit(), FruitRules.createWorldFruit(helper.getLevel().getServer(), fruit()));
      helper.assertTrue(FruitRules.createWorldFruit(helper.getLevel().getServer(), fruit()).isEmpty(), "no second fruit while one is eaten");
      FruitRules.EatOutcome outcome = FruitRules.eat(second, fruit(), new ItemStack((ItemLike)GuraItems.FRUIT.get()));
      helper.assertTrue(outcome == FruitRules.EatOutcome.POWER_TAKEN, "power must already be taken, was " + outcome);
      helper.assertTrue(!DFAttachments.data(second).hasFruit(), "second eater must not gain the power");
      cleanup(helper, first, second);
      helper.succeed();
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 200
   )
   public static void cooldownBlocksRecast(GameTestHelper helper) {
      freeWorldFruit(helper);
      FakePlayer player = player(helper, new BlockPos(2, 1, 2));
      FruitRules.grantPower(player, fruit(), false);
      helper.assertTrue(AbilityManager.press(player, GuraAbilities.KAISHIN) == CastResult.CHARGING, "Kaishin must start charging");
      helper.assertTrue(AbilityManager.release(player, GuraAbilities.KAISHIN) == CastResult.SUCCESS, "tap release must fire");
      PlayerFruitData data = DFAttachments.data(player);
      int left = data.cooldownTicksLeft(GuraAbilities.KAISHIN.id(), helper.getLevel().getGameTime());
      helper.assertTrue(left > 0, "cooldown must be running");
      helper.assertTrue(AbilityManager.press(player, GuraAbilities.KAISHIN) == CastResult.COOLDOWN, "recast must be blocked by the cooldown");
      helper.runAfterDelay((long)(left + 1), () -> {
         helper.assertTrue(AbilityManager.press(player, GuraAbilities.KAISHIN) == CastResult.CHARGING, "cast must work again after the cooldown");
         cleanup(helper, player);
         helper.succeed();
      });
   }

   @GameTest(
      template = "platform"
   )
   public static void energyIsRequiredAndRegenerates(GameTestHelper helper) {
      freeWorldFruit(helper);
      FakePlayer player = player(helper, new BlockPos(2, 1, 2));
      FruitRules.grantPower(player, fruit(), false);
      PlayerFruitData data = DFAttachments.data(player);
      data.setEnergy(0.0F);
      data.setLastCombatTick(-100000L);
      helper.assertTrue(AbilityManager.press(player, GuraAbilities.GEKISHIN) == CastResult.NO_ENERGY, "cast without energy must fail");

      for (int i = 0; i < 40; i++) {
         CoreEvents.tickServer(player, data);
      }

      helper.assertTrue(data.energy() > 1.0F, "energy must regenerate, was " + data.energy());
      cleanup(helper, player);
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void waterSuppressesThePowers(GameTestHelper helper) {
      freeWorldFruit(helper);
      helper.setBlock(new BlockPos(1, 1, 1), Blocks.WATER);
      helper.setBlock(new BlockPos(1, 2, 1), Blocks.WATER);
      FakePlayer wet = player(helper, new BlockPos(1, 1, 1));
      FruitRules.grantPower(wet, fruit(), false);
      helper.assertTrue(Weakness.isSubmerged(wet), "player must count as submerged");
      helper.assertTrue(Weakness.isSuppressed(wet), "powers must be suppressed in deep water");
      helper.assertTrue(AbilityManager.press(wet, GuraAbilities.GEKISHIN) == CastResult.SUPPRESSED, "casting must be blocked in water");
      wet.moveTo(helper.absoluteVec(new Vec3(3.5, 1.0, 3.5)));
      helper.assertTrue(!Weakness.isSuppressed(wet), "dry land must lift the suppression");
      cleanup(helper, wet);
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void legacyIdsResolve(GameTestHelper helper) {
      ResourceLocation oldFruit = ResourceLocation.fromNamespaceAndPath("gura_gura", "gura_gura_no_mi");
      helper.assertTrue(BuiltInRegistries.ITEM.get(oldFruit) == GuraItems.FRUIT.get(), "gura_gura:gura_gura_no_mi must resolve to the new item");
      helper.assertTrue(
         NeoForgeRegistries.ATTACHMENT_TYPES.get(ResourceLocation.fromNamespaceAndPath("gura_gura", "power")) == DFAttachments.PLAYER_DATA.get(),
         "gura_gura:power must resolve to the new attachment"
      );
      UUID instance = UUID.randomUUID();
      CompoundTag components = new CompoundTag();
      components.put("gura_gura:fruit_instance", new IntArrayTag(UUIDUtil.uuidToIntArray(instance)));
      CompoundTag stackTag = new CompoundTag();
      stackTag.putString("id", "gura_gura:gura_gura_no_mi");
      stackTag.putInt("count", 1);
      stackTag.put("components", components);
      ItemStack stack = ItemStack.parseOptional(helper.getLevel().registryAccess(), stackTag);
      helper.assertTrue(stack.is((Item)GuraItems.FRUIT.get()), "old stack must load as the new fruit item, was " + stack);
      helper.assertTrue(instance.equals(stack.get((DataComponentType)DFDataComponents.FRUIT_INSTANCE.get())), "old fruit_instance component must survive");
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void legacyPowerDataDecodes(GameTestHelper helper) {
      CompoundTag old = new CompoundTag();
      old.putBoolean("has_fruit", true);
      old.putInt("mastery", 4);
      old.putFloat("energy", 55.0F);
      old.putBoolean("aura", true);
      PlayerFruitData data = (PlayerFruitData)PlayerFruitData.CODEC.parse(NbtOps.INSTANCE, old).getOrThrow();
      helper.assertTrue(GuraGura.ID.equals(data.fruitId()), "has_fruit must mean the Gura Gura no Mi, was " + data.fruitId());
      helper.assertTrue(data.mastery() == 4 && data.energy() == 55.0F, "mastery and energy must survive");
      helper.assertTrue(!data.hasActiveToggles(), "the old aura flag is not restored (auras reset on login)");
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void masteryLevelsUp(GameTestHelper helper) {
      freeWorldFruit(helper);
      FakePlayer player = player(helper, new BlockPos(2, 1, 2));
      FruitRules.grantPower(player, fruit(), false);
      PlayerFruitData data = DFAttachments.data(player);
      int before = data.mastery();
      int maxBefore = Mastery.maxEnergy(data);
      Mastery.addXp(player, data, 1000.0F);
      helper.assertTrue(data.mastery() > before, "mastery must increase");
      helper.assertTrue(Mastery.maxEnergy(data) > maxBefore, "max energy must grow with mastery");
      cleanup(helper, player);
      helper.succeed();
   }
}
