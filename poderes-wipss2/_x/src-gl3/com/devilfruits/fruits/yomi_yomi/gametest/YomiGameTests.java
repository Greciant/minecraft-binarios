package com.devilfruits.fruits.yomi_yomi.gametest;

import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.CastResult;
import com.devilfruits.core.combat.Stun;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.core.rules.WorldFruitData;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.kilo_kilo.weight.WeightCurves;
import com.devilfruits.fruits.yomi_yomi.SecondWind;
import com.devilfruits.fruits.yomi_yomi.Vessel;
import com.devilfruits.fruits.yomi_yomi.WaterRun;
import com.devilfruits.fruits.yomi_yomi.YomiAbilities;
import com.devilfruits.fruits.yomi_yomi.YomiEvents;
import com.devilfruits.fruits.yomi_yomi.YomiItems;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.fruits.yomi_yomi.ability.EarthCrushingAbility;
import com.devilfruits.fruits.yomi_yomi.ability.FubukiGiriAbility;
import com.devilfruits.fruits.yomi_yomi.ability.GavotteAbility;
import com.devilfruits.fruits.yomi_yomi.ability.PolkaRemiseAbility;
import com.devilfruits.fruits.yomi_yomi.ability.YomiTiming;
import com.devilfruits.fruits.yomi_yomi.entity.PlayerCorpse;
import com.devilfruits.fruits.yomi_yomi.entity.SoulShell;
import com.devilfruits.fruits.yomi_yomi.frost.YomiFrost;
import com.devilfruits.fruits.yomi_yomi.item.SoulCaneItem;
import com.devilfruits.fruits.yomi_yomi.soul.SoulPhase;
import com.devilfruits.fruits.yomi_yomi.soul.SoulProjection;
import com.devilfruits.fruits.yomi_yomi.soul.YomiSoul;
import com.devilfruits.fruits.yomi_yomi.soulking.SoulKing;
import com.devilfruits.registry.DFAttachments;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("devilfruits")
@PrefixGameTestTemplate(false)
public final class YomiGameTests {
   private static final String PLATFORM = "platform";

   private static FakePlayer user(GameTestHelper helper, BlockPos relative) {
      FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "yomi_test"));
      BlockPos pos = helper.absolutePos(relative);
      player.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
      player.setDeltaMovement(Vec3.ZERO);
      player.setHealth(player.getMaxHealth());
      player.setData(YomiSoul.ATTACHMENT, YomiSoul.NONE);
      WorldFruitData.get(helper.getLevel().getServer()).setFree(YomiYomi.fruit());
      FruitRules.grantPower(player, YomiYomi.fruit(), false);
      DFAttachments.data(player).setEnergy(1000.0F);
      DFAttachments.data(player).setMastery(10);
      return player;
   }

   private static void done(GameTestHelper helper, FakePlayer player) {
      AbilityManager.cancelAll(player);
      AbilityManager.forget(player.getUUID());
      player.getInventory().clearContent();
      if (YomiYomi.isUser(player)) {
         FruitRules.removePower(player);
      }

      player.setData(YomiSoul.ATTACHMENT, YomiSoul.NONE);
      player.getAbilities().flying = false;
      WorldFruitData.get(helper.getLevel().getServer()).setFree(YomiYomi.fruit());
      helper.succeed();
   }

   private static void die(GameTestHelper helper, FakePlayer player) {
      NeoForge.EVENT_BUS.post(new LivingDeathEvent(player, helper.getLevel().damageSources().generic()));
   }

   @Nullable
   private static PlayerCorpse corpse(GameTestHelper helper, FakePlayer player) {
      UUID id = YomiSoul.of(player).corpseId();
      return id == null ? null : (helper.getLevel().getEntity(id) instanceof PlayerCorpse c ? c : null);
   }

   private static PlayerCorpse soulAtBody(GameTestHelper helper, FakePlayer player) {
      die(helper, player);
      PlayerCorpse body = corpse(helper, player);
      SoulPhase.spawnSoul(player);
      player.moveTo(body.getX() + 1.0, body.getY(), body.getZ());
      return body;
   }

   @GameTest(
      template = "platform"
   )
   public static void eatingGivesNothing(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));
      PlayerFruitData data = DFAttachments.data(player);
      helper.assertTrue(YomiYomi.isUser(player) && data.form().isBase() && data.form().abilities().isEmpty(), "the empty base form");
      helper.assertTrue(data.homeName().isEmpty() && YomiYomi.vessel(player) == null, "no vessel yet");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void firstDeathLeavesBody(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      Vec3 at = player.position();
      die(helper, player);
      PlayerCorpse body = corpse(helper, player);
      helper.assertTrue(body != null && body.position().distanceTo(at) < 0.01, "a body lies where the user died");
      helper.assertTrue(player.getUUID().equals(body.ownerId()), "it is the user's");
      helper.assertTrue(YomiYomi.isUser(player), "the power is kept");
      YomiSoul soul = YomiSoul.of(player);
      helper.assertTrue(
         soul.active() && soul.pending() && soul.ticksLeft() == (int)(YomiYomi.setting("soulSeconds") * 20.0), "the death phase waits for the respawn"
      );
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void soulInTheSky(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      die(helper, player);
      PlayerCorpse body = corpse(helper, player);
      SoulPhase.spawnSoul(player);
      helper.assertTrue(Math.abs(player.getY() - (body.getY() + YomiYomi.setting("soulHeight"))) < 0.01, "soulHeight blocks above the body");
      helper.assertTrue(YomiYomi.isSoul(player) && player.getAbilities().flying && !YomiSoul.of(player).pending(), "a flying soul");
      LivingIncomingDamageEvent blow = new LivingIncomingDamageEvent(player, new DamageContainer(helper.getLevel().damageSources().generic(), 5.0F));
      helper.assertTrue(((LivingIncomingDamageEvent)NeoForge.EVENT_BUS.post(blow)).isCanceled(), "nothing hurts it");
      LivingIncomingDamageEvent kill = new LivingIncomingDamageEvent(player, new DamageContainer(helper.getLevel().damageSources().fellOutOfWorld(), 5.0F));
      helper.assertTrue(!((LivingIncomingDamageEvent)NeoForge.EVENT_BUS.post(kill)).isCanceled(), "but the void does");
      helper.assertTrue(!YomiYomi.isUser(player) || DFAttachments.data(player).form().find(YomiAbilities.SOUL_DASH.id()) != null, "the soul form's bar");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void soulTimeout(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      die(helper, player);
      PlayerCorpse body = corpse(helper, player);
      SoulPhase.spawnSoul(player);
      player.setData(YomiSoul.ATTACHMENT, YomiSoul.of(player).withTicks(10));
      player.tickCount = 20;
      SoulPhase.tick(player, YomiSoul.of(player));
      helper.assertTrue(!YomiYomi.isUser(player) && !YomiSoul.of(player).active(), "the power is lost");
      helper.assertTrue(body.isRemoved(), "the body is gone");
      helper.assertTrue(
         WorldFruitData.get(helper.getLevel().getServer()).owner(YomiYomi.fruit()).filter(player.getUUID()::equals).isEmpty(), "no longer the owner"
      );
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void bodyDestroyed(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      FakePlayer other = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "yomi_other"));
      die(helper, player);
      PlayerCorpse body = corpse(helper, player);
      SoulPhase.spawnSoul(player);
      helper.assertTrue(!body.hurt(helper.getLevel().damageSources().playerAttack(player), 100.0F), "the soul cannot break its own body");
      body.hurt(helper.getLevel().damageSources().playerAttack(other), 100.0F);
      player.tickCount = 40;
      SoulPhase.tick(player, YomiSoul.of(player));
      helper.assertTrue(body.isRemoved() && !YomiYomi.isUser(player) && !YomiSoul.of(player).active(), "destroyed: the power is lost");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void possessEachVessel(GameTestHelper helper) {
      int[] bars = new int[]{1, 3, 6};

      for (Vessel vessel : Vessel.values()) {
         FakePlayer player = user(helper, new BlockPos(2, 1, 2));
         PlayerCorpse body = soulAtBody(helper, player);
         helper.assertTrue(SoulPhase.possess(player, body, vessel), "possessed as " + vessel);
         PlayerFruitData data = DFAttachments.data(player);
         helper.assertTrue(vessel.formName().equals(data.homeName()) && vessel.formName().equals(data.formName()), vessel + " is home");
         helper.assertTrue(data.form().isHome(data) && data.form().abilities().size() == bars[vessel.ordinal()], vessel + "'s bar");
         helper.assertTrue(body.isRemoved() && !YomiSoul.of(player).active() && !player.getAbilities().flying, "back in the body");
         helper.assertTrue(player.getInventory().contains(new ItemStack((ItemLike)YomiItems.SOUL_CANE.get())), "a Soul Cane");
         helper.assertTrue(FormManager.revert(player, FormManager.Reason.WATER) == FormManager.Result.ALREADY, "the sea leaves the vessel as it is");
         AbilityManager.cancelAll(player);
         player.getInventory().clearContent();
         FruitRules.removePower(player);
      }

      done(helper, user(helper, new BlockPos(1, 1, 1)));
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 60
   )
   public static void possessionDivesIn(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      PlayerCorpse body = soulAtBody(helper, player);
      helper.assertTrue(SoulPhase.beginPossession(player, body, Vessel.SKELETON), "the dive begins");
      helper.assertTrue(FormFlags.has(player, YomiYomi.POSSESSING) && YomiYomi.isSoul(player) && !body.isRemoved(), "still a soul, held, the body still there");
      helper.assertTrue(!SoulPhase.beginPossession(player, body, Vessel.HUMAN), "one dive at a time");
      helper.runAfterDelay(17L, () -> {
         helper.assertTrue(YomiYomi.vessel(player) == Vessel.SKELETON && body.isRemoved(), "risen as a Skeleton");
         helper.assertTrue(!FormFlags.has(player, YomiYomi.POSSESSING), "no longer held");
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform"
   )
   public static void waterRunRules(GameTestHelper helper) {
      BlockPos water = new BlockPos(3, 0, 3);
      helper.setBlock(water, Blocks.WATER);
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));
      SoulPhase.becomeVessel(player, Vessel.SKELETON);
      BlockPos abs = helper.absolutePos(water);
      player.moveTo((double)abs.getX() + 0.5, (double)abs.getY() + 0.9, (double)abs.getZ() + 0.5);
      float energy = DFAttachments.data(player).energy();
      player.setSprinting(false);
      WaterRun.tick(player);
      helper.assertTrue(!FormFlags.has(player, WaterRun.FLAG), "walking: no run");
      player.setSprinting(true);
      player.fallDistance = 5.0F;
      WaterRun.tick(player);
      helper.assertTrue(FormFlags.has(player, WaterRun.FLAG), "sprinting on the surface: running");
      helper.assertTrue(DFAttachments.data(player).energy() < energy && player.fallDistance == 0.0F, "energy paid, no fall");
      player.moveTo((double)abs.getX() + 0.5, (double)(abs.getY() + 3), (double)abs.getZ() + 0.5);
      WaterRun.tick(player);
      helper.assertTrue(!FormFlags.has(player, WaterRun.FLAG), "high above the water: no run");
      SoulPhase.becomeVessel(player, Vessel.HUMAN);
      player.moveTo((double)abs.getX() + 0.5, (double)abs.getY() + 0.9, (double)abs.getZ() + 0.5);
      WaterRun.tick(player);
      helper.assertTrue(!FormFlags.has(player, WaterRun.FLAG), "a Human sinks");
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 60
   )
   public static void runningOnWaterIsNotInIt(GameTestHelper helper) {
      BlockPos deep = new BlockPos(3, 0, 3);
      helper.setBlock(deep, Blocks.WATER);
      helper.setBlock(deep.above(), Blocks.WATER);
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));
      SoulPhase.becomeVessel(player, Vessel.SKELETON);
      BlockPos abs = helper.absolutePos(deep);
      player.moveTo((double)abs.getX() + 0.5, (double)abs.getY() + 1.9, (double)abs.getZ() + 0.5);
      player.setSprinting(true);
      WaterRun.tick(player);
      helper.assertTrue(FormFlags.has(player, WaterRun.FLAG), "running on the deep water");
      player.moveTo((double)abs.getX() + 0.5, (double)abs.getY() + 0.5, (double)abs.getZ() + 0.5);
      DevilFruit fruit = DFAttachments.data(player).fruit();
      helper.assertTrue(Weakness.isSubmerged(player), "the feet are in deep water");
      helper.assertTrue(!Weakness.drownedPowers(player, fruit), "no weakness while running on it");
      player.setSprinting(false);
      WaterRun.tick(player);
      helper.assertTrue(!Weakness.drownedPowers(player, fruit), "nor right after the last stride");
      helper.runAfterDelay(12L, () -> {
         helper.assertTrue(Weakness.drownedPowers(player, fruit), "stopped running: the sea takes it");
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform"
   )
   public static void possessionNeedsReach(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      PlayerCorpse body = soulAtBody(helper, player);
      player.moveTo(body.getX() + YomiYomi.setting("possessReach") + 6.0, body.getY() + 4.0, body.getZ());
      helper.assertTrue(!SoulPhase.possess(player, body, Vessel.SKELETON) && YomiSoul.of(player).active(), "too far: nothing happens");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void laterDeathLosesPower(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      SoulPhase.possess(player, soulAtBody(helper, player), Vessel.HUMAN);
      die(helper, player);
      helper.assertTrue(!YomiYomi.isUser(player) && !YomiSoul.of(player).active(), "the power is lost");
      List<PlayerCorpse> bodies = helper.getLevel().getEntitiesOfClass(PlayerCorpse.class, player.getBoundingBox().inflate(4.0));
      helper.assertTrue(bodies.isEmpty(), "no second body");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void soulKilledForGood(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      PlayerCorpse body = soulAtBody(helper, player);
      die(helper, player);
      helper.assertTrue(!YomiYomi.isUser(player) && body.isRemoved() && !YomiSoul.of(player).active(), "power and body gone");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void vesselCommandAndLock(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      SoulPhase.becomeVessel(player, Vessel.CORPSE);
      helper.assertTrue(YomiYomi.vessel(player) == Vessel.CORPSE && "corpse".equals(DFAttachments.data(player).formName()), "set to the Living Corpse");
      helper.assertTrue(FormManager.request(player, "skeleton", FormManager.Reason.PLAYER) == FormManager.Result.CONDITION, "another vessel is refused");
      helper.assertTrue(FormManager.request(player, "soul", FormManager.Reason.PLAYER) == FormManager.Result.CONDITION, "the soul form needs a soul");
      die(helper, player);
      helper.assertTrue(!YomiYomi.isUser(player), "the first death no longer applies once a vessel is set");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void livingCorpseRules(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));
      SoulPhase.becomeVessel(player, Vessel.CORPSE);
      Zombie zombie = (Zombie)helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 1, 3));
      zombie.setTarget(player);
      helper.assertTrue(zombie.getTarget() == null, "the zombie leaves the corpse alone");
      zombie.setLastHurtByMob(player);
      zombie.setTarget(player);
      helper.assertTrue(zombie.getTarget() == player, "unless it struck the zombie first");
      IronGolem golem = (IronGolem)helper.spawn(EntityType.IRON_GOLEM, new BlockPos(4, 1, 1));
      YomiEvents.scareTheLiving(player);
      helper.assertTrue(golem.getTarget() == player && player.getUUID().equals(golem.getPersistentAngerTarget()), "the golem attacks it");
      zombie.discard();
      golem.discard();
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void skeletonJump(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));
      double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
      SoulPhase.becomeVessel(player, Vessel.SKELETON);
      double apex = WeightCurves.apex(player.getAttributeValue(Attributes.JUMP_STRENGTH), 0.08);
      helper.assertTrue(Math.abs(apex - 3.0) < 0.05, "apex " + apex);
      helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) > speed * 1.15, "faster");
      SoulPhase.becomeVessel(player, Vessel.HUMAN);
      helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.JUMP_STRENGTH) - 0.42) < 1.0E-6, "a Human jumps like anyone");
      done(helper, player);
   }

   private static FakePlayer swordsman(GameTestHelper helper, BlockPos relative) {
      FakePlayer player = user(helper, relative);
      SoulPhase.becomeVessel(player, Vessel.SKELETON);
      ItemStack cane = new ItemStack((ItemLike)YomiItems.SOUL_CANE.get());
      cane.set((DataComponentType)YomiItems.DRAWN.get(), true);
      player.setItemInHand(InteractionHand.MAIN_HAND, cane);
      player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack((ItemLike)YomiItems.SCABBARD.get()));
      return player;
   }

   private static Husk husk(GameTestHelper helper, BlockPos relative, float health) {
      Husk husk = (Husk)helper.spawn(EntityType.HUSK, relative);
      husk.setNoAi(true);
      husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue((double)health);
      husk.setHealth(health);
      return husk;
   }

   @GameTest(
      template = "platform"
   )
   public static void caneDrawAndSheathe(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      player.getInventory().clearContent();
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)YomiItems.SOUL_CANE.get()));
      player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH, 5));
      helper.assertTrue(SoulCaneItem.draw(player) == null, "drawn");
      helper.assertTrue(SoulCaneItem.isDrawn(player.getMainHandItem()), "the blade in the main hand");
      helper.assertTrue(player.getOffhandItem().is((Item)YomiItems.SCABBARD.get()), "the scabbard in the off hand");
      helper.assertTrue(player.getInventory().countItem(Items.TORCH) == 5, "the torches moved into the inventory");
      helper.assertTrue(SoulCaneItem.ready(player), "ready for the techniques");
      helper.assertTrue(SoulCaneItem.draw(player) != null, "already drawn");
      helper.assertTrue(SoulCaneItem.sheathe(player) == null, "sheathed");
      helper.assertTrue(player.getMainHandItem().is((Item)YomiItems.SOUL_CANE.get()) && !SoulCaneItem.isDrawn(player.getMainHandItem()), "a cane again");
      helper.assertTrue(player.getOffhandItem().isEmpty(), "the scabbard went back on the blade");
      SoulCaneItem.draw(player);
      player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
      helper.assertTrue(SoulCaneItem.sheathe(player) != null && SoulCaneItem.isDrawn(player.getMainHandItem()), "no scabbard, no sheathing");
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)YomiItems.SOUL_CANE.get()));

      for (int i = 0; i < 36; i++) {
         if (i != player.getInventory().selected) {
            player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
         }
      }

      player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH));
      helper.assertTrue(SoulCaneItem.draw(player) != null && !SoulCaneItem.isDrawn(player.getMainHandItem()), "refused without room");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void bladeTechniquesNeedTheCane(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      SoulPhase.becomeVessel(player, Vessel.SKELETON);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)YomiItems.SOUL_CANE.get()));
      helper.assertTrue(AbilityManager.press(player, YomiAbilities.AUBADE) == CastResult.REQUIREMENT, "a sheathed cane");
      player.getMainHandItem().set((DataComponentType)YomiItems.DRAWN.get(), true);
      helper.assertTrue(AbilityManager.press(player, YomiAbilities.AUBADE) == CastResult.REQUIREMENT, "no scabbard");
      player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack((ItemLike)YomiItems.SCABBARD.get()));
      helper.assertTrue(AbilityManager.press(player, YomiAbilities.AUBADE) == CastResult.SUCCESS, "drawn: it goes");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void aubadePierces(GameTestHelper helper) {
      FakePlayer player = swordsman(helper, new BlockPos(2, 1, 0));
      Husk near = husk(helper, new BlockPos(2, 1, 2), 100.0F);
      Husk behind = husk(helper, new BlockPos(2, 1, 4), 100.0F);
      helper.setBlock(new BlockPos(2, 2, 3), Blocks.STONE);
      helper.assertTrue(AbilityManager.press(player, YomiAbilities.AUBADE) == CastResult.SUCCESS, "cast");
      helper.runAfterDelay(5L, () -> helper.assertTrue(near.getHealth() == 100.0F, "nothing before the thrust frame"));
      helper.runAfterDelay(9L, () -> {
         helper.assertTrue(near.getHealth() < 100.0F, "pierced");
         helper.assertTrue(near.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "slowed");
         helper.assertTrue(behind.getHealth() == 100.0F, "the wall stopped the thrust");
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform"
   )
   public static void gavotteLeapsAndSweeps(GameTestHelper helper) {
      Vec3 v = GavotteAbility.leap(new Vec3(0.0, 0.0, 1.0), 10.0);
      double x = 0.0;
      double vx = v.z;

      for (int i = 0; i < 19; i++) {
         x += vx;
         vx *= 0.91;
      }

      helper.assertTrue(Math.abs(x - 10.0) < 0.8, "the arc covers " + x);
      helper.assertTrue(v.y > 0.6, "up and forward");
      FakePlayer player = swordsman(helper, new BlockPos(2, 1, 1));
      Husk husk = husk(helper, new BlockPos(2, 1, 3), 100.0F);
      double y0 = husk.getY();
      helper.assertTrue(AbilityManager.press(player, YomiAbilities.GAVOTTE) == CastResult.SUCCESS, "cast");
      helper.runAfterDelay(18L, () -> {
         helper.assertTrue(husk.getHealth() < 100.0F, "cut");
         helper.assertTrue(husk.getDeltaMovement().y > 0.05 || husk.getY() > y0 + 0.2, "launched");
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform"
   )
   public static void fubukiGiriFreezes(GameTestHelper helper) {
      FakePlayer player = swordsman(helper, new BlockPos(2, 1, 0));
      helper.setBlock(new BlockPos(1, 1, 4), Blocks.STONE);
      helper.setBlock(new BlockPos(1, 2, 4), Blocks.STONE);
      helper.setBlock(new BlockPos(1, 3, 4), Blocks.STONE);
      Vec3 lane = helper.absoluteVec(new Vec3(1.5, 1.0, 0.5));
      player.moveTo(lane.x, lane.y, lane.z, 0.0F, 0.0F);
      Vec3[] walled = FubukiGiriAbility.path(player, new Vec3(0.0, 0.0, 1.0), 12.0);
      double run = walled[1].z - walled[0].z;
      helper.assertTrue(run < 3.3 && run > 2.5, "stopped before the wall: " + run);
      Vec3 start = helper.absoluteVec(new Vec3(2.5, 1.0, 0.5));
      player.moveTo(start.x, start.y, start.z, 0.0F, 0.0F);
      Vec3[] open = FubukiGiriAbility.path(player, new Vec3(0.0, 0.0, 1.0), 3.5);
      helper.assertTrue(Math.abs(open[1].z - open[0].z - 3.5) < 0.001, "the whole range in the open");
      Husk husk = husk(helper, new BlockPos(2, 1, 2), 100.0F);
      helper.assertTrue(AbilityManager.press(player, YomiAbilities.FUBUKI_GIRI) == CastResult.SUCCESS, "cast");
      helper.runAfterDelay(25L, () -> {
         helper.assertTrue(husk.getHealth() == 100.0F, "the cut comes with the click");
         helper.assertTrue(player.getZ() > start.z + 2.0, "dashed through");
      });
      helper.runAfterDelay(29L, () -> {
         helper.assertTrue(husk.getHealth() < 100.0F, "cut");
         helper.assertTrue(YomiFrost.frozen(husk) && Stun.stunned(husk), "frozen solid");
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 140
   )
   public static void polkaLocks(GameTestHelper helper) {
      FakePlayer player = swordsman(helper, new BlockPos(2, 1, 0));
      Husk husk = husk(helper, new BlockPos(2, 1, 3), 300.0F);
      helper.assertTrue(AbilityManager.press(player, YomiAbilities.POLKA) == CastResult.CHANNELLING, "channelling");

      for (int i = 1; i <= 82; i++) {
         helper.runAfterDelay((long)i, () -> AbilityManager.tickPlayer(player, DFAttachments.data(player)));
      }

      helper.runAfterDelay(9L, () -> {
         helper.assertTrue(husk.getHealth() < 300.0F && Stun.stunned(husk), "struck and held");
         helper.assertTrue(YomiFrost.chill(husk) == 0, "no chill");
      });
      helper.runAfterDelay(83L, () -> {
         helper.assertTrue(PolkaRemiseAbility.thrusts(player) == -1 && AbilityManager.channellingAbility(player) == null, "over");
         helper.assertTrue(!husk.hasEffect(YomiFrost.FROSTBITE) && YomiFrost.chill(husk) == 0 && !YomiFrost.frozen(husk), "never frozen");
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform"
   )
   public static void stunInterruptsChannel(GameTestHelper helper) {
      FakePlayer player = swordsman(helper, new BlockPos(2, 1, 2));
      helper.assertTrue(AbilityManager.press(player, YomiAbilities.POLKA) == CastResult.CHANNELLING, "channelling");
      Stun.apply(player, 10);
      helper.assertTrue(AbilityManager.channellingAbility(player) == null, "interrupted");
      Stun.release(player);
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void coldRules(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(0, 1, 0));
      Husk a = husk(helper, new BlockPos(2, 1, 2), 100.0F);
      Husk b = husk(helper, new BlockPos(3, 1, 2), 100.0F);
      a.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));

      for (int i = 1; i <= 3; i++) {
         helper.assertTrue(YomiFrost.addChill(a, player, 10.0F, 70) == i, "stack " + i);
      }

      helper.assertTrue(YomiFrost.addChill(a, player, 10.0F, 70) == 0, "burst");
      helper.assertTrue(a.getHealth() <= 90.01F, "pure damage through netherite: " + a.getHealth());
      helper.assertTrue(a.hasEffect(YomiFrost.FROSTBITE), "frostbite");
      long now = helper.getLevel().getGameTime();
      YomiFrost.Data melted = new YomiFrost.Data(3, now - 80L - 60L - 1L, 0L, 0L);
      helper.assertTrue(melted.chillAt(now) == 0, "melted: " + melted.chillAt(now));
      helper.assertTrue(new YomiFrost.Data(3, now, 0L, 0L).chillAt(now + 10L) == 3, "held for a while");
      YomiFrost.freeze(a, 40);
      float before = b.getHealth();
      a.doHurtTarget(b);
      helper.assertTrue(b.getHealth() == before, "a frozen body strikes no blow");
      done(helper, player);
   }

   private static FakePlayer projected(GameTestHelper helper, BlockPos relative) {
      FakePlayer player = user(helper, relative);
      SoulPhase.becomeVessel(player, Vessel.SKELETON);
      helper.assertTrue(AbilityManager.press(player, YomiAbilities.SOUL_PROJECTION) == CastResult.TOGGLED, "projected");
      return player;
   }

   private static List<SoulShell> shells(GameTestHelper helper) {
      return helper.getLevel().getEntitiesOfClass(SoulShell.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12.0));
   }

   @GameTest(
      template = "platform"
   )
   public static void projectionOutAndBack(GameTestHelper helper) {
      FakePlayer player = projected(helper, new BlockPos(2, 1, 1));
      helper.assertTrue(YomiYomi.isSoul(player) && SoulProjection.active(player), "a soul");
      helper.assertTrue(shells(helper).size() == 1, "one kneeling body");
      Vec3 body = SoulProjection.body(player);
      player.moveTo(body.x, body.y + 1.0, body.z + 2.5, 0.0F, 0.0F);
      helper.assertTrue(AbilityManager.press(player, YomiAbilities.SOUL_PROJECTION) == CastResult.TOGGLED, "pressed again");
      helper.assertTrue(SoulProjection.recoiling(player), "pulled back");

      for (int i = 1; i <= 10; i++) {
         helper.runAfterDelay((long)i, () -> SoulProjection.tick(player));
      }

      helper.runAfterDelay(12L, () -> {
         helper.assertTrue(!SoulProjection.active(player) && !YomiYomi.isSoul(player), "back in the body");
         helper.assertTrue(Vessel.SKELETON.formName().equals(DFAttachments.data(player).formName()), "the Skeleton again");
         helper.assertTrue(player.position().distanceTo(body) < 0.1, "where the body knelt");
         helper.assertTrue(shells(helper).isEmpty(), "no body left behind");
         helper.assertTrue(DFAttachments.data(player).isOnCooldown(YomiAbilities.SOUL_PROJECTION.id(), helper.getLevel().getGameTime()), "cooldown");
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform"
   )
   public static void projectionLeashNoTimeLimit(GameTestHelper helper) {
      FakePlayer player = projected(helper, new BlockPos(2, 1, 2));
      Vec3 body = SoulProjection.body(player);
      double leash = YomiYomi.setting("soulLeash");
      player.moveTo(body.x, body.y + 2.0, body.z + 200.0, 0.0F, 0.0F);
      SoulProjection.tick(player);
      helper.assertTrue(player.position().distanceTo(body) <= leash + 8.01, "far beyond: drawn to the edge");
      player.moveTo(body.x, body.y + 2.0, body.z + leash + 3.0, 0.0F, 0.0F);
      player.setDeltaMovement(0.0, 0.0, 0.5);
      SoulProjection.tick(player);
      helper.assertTrue(player.getDeltaMovement().z < 0.0, "past the leash: no further out, pulled home");
      SoulProjection.rewind(player, 36000);
      SoulProjection.tick(player);
      helper.assertTrue(SoulProjection.active(player) && !SoulProjection.recoiling(player), "half an hour out: still out");
      SoulProjection.finish(player, true);
      done(helper, player);
   }

   private static void standOnFloor(FakePlayer player) {
      for (int i = 0; i < 4 && !player.level().noCollision(player, player.getBoundingBox().deflate(0.001)); i++) {
         player.moveTo(player.getX(), Math.floor(player.getY()) + 1.0, player.getZ(), player.getYRot(), player.getXRot());
      }
   }

   private static Husk huskAt(GameTestHelper helper, Vec3 at, float health) {
      Husk husk = (Husk)EntityType.HUSK.create(helper.getLevel());
      husk.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
      husk.setNoAi(true);
      husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue((double)health);
      husk.setHealth(health);
      helper.getLevel().addFreshEntity(husk);
      return husk;
   }

   @GameTest(
      template = "platform"
   )
   public static void projectionThinWallsOnly(GameTestHelper helper) {
      FakePlayer player = projected(helper, new BlockPos(2, 1, 2));
      standOnFloor(player);
      SoulProjection.validate(player);
      BlockPos feet = player.blockPosition();
      Vec3 c = player.position();

      for (int y = 0; y <= 2; y++) {
         helper.getLevel().setBlockAndUpdate(feet.offset(0, y, 1), Blocks.STONE.defaultBlockState());
         helper.getLevel().setBlockAndUpdate(feet.offset(0, y, -1), Blocks.STONE.defaultBlockState());
         helper.getLevel().setBlockAndUpdate(feet.offset(0, y, -2), Blocks.STONE.defaultBlockState());
      }

      boolean snapped = false;
      String where = "";

      for (double z = 0.0; z <= 2.0; z += 0.25) {
         player.moveTo(c.x, c.y, c.z + z, 0.0F, 0.0F);
         if (SoulProjection.validate(player) && !snapped) {
            snapped = true;
            where = "at " + z;
         }
      }

      helper.assertTrue(!snapped, "through the thin wall " + where);
      player.moveTo(c.x, c.y, c.z, 0.0F, 0.0F);
      SoulProjection.validate(player);

      for (double zx = 0.0; zx >= -2.5 && !snapped; zx -= 0.25) {
         player.moveTo(c.x, c.y, c.z + zx, 0.0F, 0.0F);
         snapped = SoulProjection.validate(player);
      }

      helper.assertTrue(snapped, "put back out of the thick wall");
      helper.assertTrue(player.getZ() > c.z - 0.21, "where it was free: " + (player.getZ() - c.z));
      SoulProjection.finish(player, true);
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void bodyPassesBlowsOn(GameTestHelper helper) {
      FakePlayer player = projected(helper, new BlockPos(2, 1, 2));
      SoulShell shell = shells(helper).get(0);
      float dealt = SoulProjection.forward(shell, helper.getLevel().damageSources().generic(), 8.0F);
      helper.assertTrue((double)Math.abs(dealt - 8.0F * (1.0F - (float)YomiYomi.setting("shellResistance"))) < 0.001, "half passed on: " + dealt);
      helper.assertTrue(SoulProjection.forward(shell, helper.getLevel().damageSources().playerAttack(player), 8.0F) == 0.0F, "not by its own soul");
      SoulProjection.finish(player, true);
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 60
   )
   public static void soulSkills(GameTestHelper helper) {
      FakePlayer skeleton = user(helper, new BlockPos(2, 1, 2));
      SoulPhase.becomeVessel(skeleton, Vessel.SKELETON);
      helper.assertTrue(!AbilityManager.press(skeleton, YomiAbilities.SOUL_DASH).succeeded(), "not without the soul out");
      AbilityManager.press(skeleton, YomiAbilities.SOUL_PROJECTION);
      standOnFloor(skeleton);
      Vec3 from = skeleton.position();
      Husk ahead = huskAt(helper, from.add(0.0, 0.0, 1.5), 100.0F);
      Husk near = huskAt(helper, from.add(1.5, 0.0, 0.0), 100.0F);
      skeleton.moveTo(from.x, from.y, from.z, 0.0F, 0.0F);
      helper.assertTrue(AbilityManager.press(skeleton, YomiAbilities.SOUL_DASH) == CastResult.SUCCESS, "Soul Dash");
      helper.runAfterDelay(
         12L,
         () -> {
            helper.assertTrue(
               ahead.getHealth() < 100.0F && YomiFrost.chill(ahead) >= 1,
               "chilled on the way: hp " + ahead.getHealth() + " moved " + skeleton.position().distanceTo(from)
            );
            skeleton.moveTo(from.x, from.y, from.z, 0.0F, 0.0F);
            helper.assertTrue(AbilityManager.press(skeleton, YomiAbilities.COLD_SOUL) == CastResult.SUCCESS, "Cold Soul");
         }
      );
      helper.runAfterDelay((long)(14 + YomiTiming.COLD_SOUL_PULSES[0]), () -> {
         MobEffectInstance bite = near.getEffect(YomiFrost.FROSTBITE);
         helper.assertTrue(near.getHealth() < 100.0F && bite != null && bite.getAmplifier() == 5, "60 % slower");
         SoulProjection.finish(skeleton, true);
         ahead.discard();
         near.discard();
         done(helper, skeleton);
      });
   }

   @GameTest(
      template = "platform"
   )
   public static void secondWind(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      SoulPhase.becomeVessel(player, Vessel.SKELETON);
      Husk husk = husk(helper, new BlockPos(2, 1, 4), 100.0F);
      helper.assertTrue(!SecondWind.ready(player, helper.getLevel().damageSources().genericKill()), "not against /kill");
      player.setHealth(1.0F);
      LivingDeathEvent death = new LivingDeathEvent(player, helper.getLevel().damageSources().generic());
      NeoForge.EVENT_BUS.post(death);
      helper.assertTrue(death.isCanceled(), "saved");
      helper.assertTrue(player.getHealth() == player.getMaxHealth(), "whole again");
      helper.assertTrue(SecondWind.inGrace(player), "untouchable a moment");
      helper.assertTrue(husk.getHealth() < 100.0F, "the shockwave struck");
      helper.assertTrue(!SecondWind.ready(player, helper.getLevel().damageSources().generic()), "once in a while");
      SoulPhase.becomeVessel(player, Vessel.HUMAN);
      DFAttachments.data(player).clearCooldowns();
      helper.assertTrue(!SecondWind.ready(player, helper.getLevel().damageSources().generic()), "a Skeleton's");
      done(helper, player);
   }

   private static FakePlayer king(GameTestHelper helper, BlockPos relative, BlockPos facing) {
      FakePlayer player = user(helper, relative);
      SoulPhase.becomeVessel(player, Vessel.SKELETON);
      standOnFloor(player);
      Vec3 to = Vec3.atBottomCenterOf(helper.absolutePos(facing));
      float yaw = (float)Math.toDegrees(Math.atan2(-(to.x - player.getX()), to.z - player.getZ()));
      player.moveTo(player.getX(), player.getY(), player.getZ(), yaw, 0.0F);
      player.setYHeadRot(yaw);
      helper.assertTrue(FormManager.request(player, "soul_king", FormManager.Reason.COMMAND) == FormManager.Result.SWITCHED, "the Soul King");
      return player;
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 80
   )
   public static void soulKingNeedsSkeletonAndGuitar(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      SoulPhase.becomeVessel(player, Vessel.HUMAN);
      helper.assertTrue(FormManager.check(player, "soul_king") == FormManager.Result.CONDITION, "not a Human");
      SoulPhase.becomeVessel(player, Vessel.SKELETON);
      helper.assertTrue(FormManager.check(player, "soul_king") == FormManager.Result.CONDITION, "not without the guitar");
      helper.assertTrue(AbilityManager.press(player, YomiAbilities.KING_STRIKE) == CastResult.NOT_IN_FORM, "no ghost's blows as a plain Skeleton");
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)YomiItems.SOUL_GUITAR.get()));
      helper.assertTrue(FormManager.check(player, "soul_king") == FormManager.Result.SWITCHED, "a Skeleton with the guitar");
      helper.assertTrue(FormManager.request(player, "soul_king", FormManager.Reason.PLAYER) == FormManager.Result.WINDUP, "the riff first");
      helper.onEachTick(() -> FormManager.tick(player, DFAttachments.data(player)));
      helper.runAfterDelay(
         33L,
         () -> {
            helper.assertTrue(SoulKing.active(player), "the ghost rises after the riff");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            PlayerFruitData data = DFAttachments.data(player);
            helper.assertTrue(data.form().condition().check(player, data) == null, "it stands without the guitar");
            helper.assertTrue(
               data.form().find(YomiAbilities.KING_STRIKE.id()) != null && data.form().indexOf(YomiAbilities.KING_STRIKE.id()) < 0,
               "M1 is the ghost's, not on the bar"
            );
            FormManager.revert(player, FormManager.Reason.COMMAND);
            done(helper, player);
         }
      );
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 80
   )
   public static void soulKingRisesAndEnds(GameTestHelper helper) {
      FakePlayer player = king(helper, new BlockPos(2, 1, 2), new BlockPos(2, 1, 4));
      helper.assertTrue(!SoulKing.ready(player), "still rising");
      helper.assertTrue(!AbilityManager.press(player, YomiAbilities.EARTH_CRUSHING).succeeded(), "nothing while it rises");
      helper.runAfterDelay(41L, () -> {
         helper.assertTrue(SoulKing.ready(player), "risen");
         FormManager.revert(player, FormManager.Reason.DEPLETED);
         PlayerFruitData data = DFAttachments.data(player);
         long left = (long)data.cooldownTicksLeft(PlayerFruitData.FORM_COOLDOWN, helper.getLevel().getGameTime());
         helper.assertTrue(left > 1700L, "90 s from the end, the bar run dry too: " + left);
         helper.assertTrue(!SoulKing.active(player), "gone");
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 140
   )
   public static void ghostStrikeCombo(GameTestHelper helper) {
      FakePlayer player = king(helper, new BlockPos(2, 1, 1), new BlockPos(2, 1, 4));
      Vec3 dir = player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
      Vec3 right = new Vec3(-dir.z, 0.0, dir.x);
      Husk[] targets = new Husk[3];
      float[] first = new float[1];
      Husk beside = husk(helper, new BlockPos(4, 1, 1), 200.0F);
      int s = 41;
      int[] press = new int[]{s, s + YomiTiming.KING_STRIKE_HIT[0] + 5, s + YomiTiming.KING_STRIKE_HIT[0] + YomiTiming.KING_STRIKE_HIT[1] + 10};

      for (int i = 0; i < 3; i++) {
         int step = i;
         helper.runAfterDelay((long)press[i], () -> {
            targets[step] = husk(helper, new BlockPos(2, 1, 3), 200.0F);
            beside.setHealth(200.0F);
            CastResult r = AbilityManager.press(player, YomiAbilities.KING_STRIKE);
            helper.assertTrue(r == (step < 2 ? CastResult.COMBO_STEP : CastResult.SUCCESS), "blow " + step + ": " + r);
            if (step == 0) {
               helper.assertTrue(AbilityManager.press(player, YomiAbilities.KING_STRIKE) == CastResult.BUSY, "a click mid-blow is dropped");
            }
         });
         helper.runAfterDelay(
            (long)(press[i] + YomiTiming.KING_STRIKE_HIT[step] - 1),
            () -> helper.assertTrue(
                  targets[step].getHealth() == 200.0F,
                  "nothing before blow " + step + "'s hit frame: " + targets[step].getHealth() + " " + targets[step].getLastDamageSource()
               )
         );
         helper.runAfterDelay((long)(press[i] + YomiTiming.KING_STRIKE_HIT[step] + 1), () -> {
            Husk h = targets[step];
            float lost = 200.0F - h.getHealth();
            helper.assertTrue(lost > 0.0F, "blow " + step + " struck");
            Vec3 v = h.getDeltaMovement();
            if (step == 0) {
               first[0] = lost;
               helper.assertTrue(v.dot(right) > 0.4, "flung along the arc, to the right: " + v);
            } else if (step == 1) {
               helper.assertTrue(Stun.stunned(h), "pinned");
            } else {
               helper.assertTrue(v.dot(dir) > 0.5 && lost > first[0] * 1.5F, "thrown away, double damage: " + v + " " + lost + " vs " + first[0]);
               helper.assertTrue(beside.getHealth() == 200.0F, "the push only goes ahead");
            }

            h.discard();
         });
      }

      helper.runAfterDelay((long)(press[2] + YomiTiming.KING_STRIKE_HIT[2] + 3), () -> {
         beside.discard();
         FormManager.revert(player, FormManager.Reason.COMMAND);
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 140
   )
   public static void earthCrushingWave(GameTestHelper helper) {
      double range = YomiAbilities.EARTH_CRUSHING.config().range() * (double)Mastery.rangeMultiplier(10);
      helper.assertTrue(
         EarthCrushingAbility.band(range, 0) < 2.5 && Math.abs(EarthCrushingAbility.band(range, 8) - range) < 0.001,
         "the wave races from the slam out to the full radius"
      );
      FakePlayer player = king(helper, new BlockPos(2, 1, 1), new BlockPos(2, 1, 4));
      Husk near = husk(helper, new BlockPos(1, 1, 4), 200.0F);
      Husk side = husk(helper, new BlockPos(4, 1, 2), 200.0F);
      BlockPos glass = new BlockPos(2, 1, 4);
      helper.setBlock(glass, Blocks.GLASS);
      helper.runAfterDelay(41L, () -> helper.assertTrue(AbilityManager.press(player, YomiAbilities.EARTH_CRUSHING) == CastResult.SUCCESS, "cast"));
      helper.runAfterDelay(65L, () -> helper.assertTrue(near.getHealth() == 200.0F, "nothing before the slam"));
      helper.runAfterDelay(68L, () -> {
         Vec3 apex = player.position().add(player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(1.5));

         for (Husk h : List.of(near, side)) {
            Vec3 v = h.getDeltaMovement();
            Vec3 away = new Vec3(h.getX() - apex.x, 0.0, h.getZ() - apex.z).normalize();
            helper.assertTrue(v.y > 0.2 && v.x * away.x + v.z * away.z > 0.3, "thrown away from the slam " + v);
         }
      });
      helper.runAfterDelay(
         97L,
         () -> {
            for (Husk h : List.of(near, side)) {
               helper.assertTrue(h.getHealth() < 200.0F, "struck: " + h.getHealth());
               helper.assertTrue(Stun.stunned(h), "stunned once down");
               helper.assertTrue(
                  !YomiFrost.get(h).cold(helper.getLevel().getGameTime()) && !h.hasEffect(YomiFrost.FROSTBITE) && YomiFrost.chill(h) == 0, "no cold"
               );
            }

            if (TerrainDestruction.destructionAllowed(helper.getLevel())) {
               helper.assertTrue(helper.getBlockState(glass).isAir(), "the glass shattered");
            }

            near.discard();
            side.discard();
            helper.setBlock(glass, Blocks.AIR);
            FormManager.revert(player, FormManager.Reason.COMMAND);
            done(helper, player);
         }
      );
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 120
   )
   public static void roarStuns(GameTestHelper helper) {
      FakePlayer player = king(helper, new BlockPos(2, 1, 2), new BlockPos(2, 1, 4));
      Husk naked = husk(helper, new BlockPos(4, 1, 2), 500.0F);
      Husk armoured = husk(helper, new BlockPos(0, 1, 2), 500.0F);
      armoured.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
      armoured.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
      armoured.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
      armoured.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
      helper.runAfterDelay(41L, () -> helper.assertTrue(AbilityManager.press(player, YomiAbilities.SOUL_KING_ROAR) == CastResult.SUCCESS, "cast"));
      helper.runAfterDelay(65L, () -> helper.assertTrue(naked.getHealth() == 500.0F, "only the scream strikes"));
      helper.runAfterDelay(68L, () -> {
         float bare = 500.0F - naked.getHealth();
         float clad = 500.0F - armoured.getHealth();
         helper.assertTrue(bare > 0.0F && Stun.stunned(naked) && Stun.stunned(armoured), "struck and stunned");
         helper.assertTrue(clad < bare && clad > bare * 0.4F * 0.95F, "armour stops only part of it: " + clad + " of " + bare);
         naked.discard();
         armoured.discard();
         FormManager.revert(player, FormManager.Reason.COMMAND);
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 80
   )
   public static void soulKingAura(GameTestHelper helper) {
      FakePlayer player = king(helper, new BlockPos(2, 1, 2), new BlockPos(2, 1, 4));
      Husk close = husk(helper, new BlockPos(3, 1, 2), 100.0F);
      helper.runAfterDelay(41L, () -> {
         SoulKing.aura(player);
         MobEffectInstance slow = close.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
         helper.assertTrue(slow != null && slow.getAmplifier() == 0, "slightly slowed");
         close.discard();
         FormManager.revert(player, FormManager.Reason.COMMAND);
         done(helper, player);
      });
   }
}
