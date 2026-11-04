package com.devilfruits.core.gametest;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.CoreEvents;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.CastResult;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitRegistry;
import com.devilfruits.core.gametest.fixture.DebugLogia;
import com.devilfruits.core.logia.CounterTrigger;
import com.devilfruits.core.logia.HitRule;
import com.devilfruits.core.logia.LogiaDamage;
import com.devilfruits.core.logia.LogiaDash;
import com.devilfruits.core.logia.LogiaInteractionRegistry;
import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.core.logia.LogiaMovement;
import com.devilfruits.core.logia.LogiaReform;
import com.devilfruits.core.logia.LogiaState;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.core.rules.WorldFruitData;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFItems;
import com.devilfruits.registry.DFMobEffects;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("devilfruits")
@PrefixGameTestTemplate(false)
public final class LogiaGameTests {
   private static final String PLATFORM = "platform";

   private static FakePlayer logia(GameTestHelper helper, BlockPos relative) {
      FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "logia_test")) {
         public boolean isInvulnerableTo(DamageSource source) {
            return false;
         }
      };
      BlockPos pos = helper.absolutePos(relative);
      player.setGameMode(GameType.SURVIVAL);
      ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, player, 0, "spawnInvulnerableTime");
      player.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
      FruitRules.grantPower(player, DebugLogia.fruit(), false);
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
      template = "platform"
   )
   public static void hitsPassThroughTheElement(GameTestHelper helper) {
      FakePlayer player = logia(helper, new BlockPos(2, 2, 2));
      PlayerFruitData data = DFAttachments.data(player);
      float health = player.getHealth();
      helper.assertFalse(player.hurt(player.damageSources().explosion(null, null), 8.0F), "an explosion goes through the element");
      helper.assertFalse(player.hurt(player.damageSources().cactus(), 2.0F), "so does a cactus");
      helper.assertFalse(player.hurt(player.damageSources().fall(), 6.0F), "and a fall");
      helper.assertTrue(player.getHealth() == health, "no health lost");
      helper.assertTrue(data.energy() == 100.0F, "and intangibility is free: no energy spent, now " + data.energy());
      Mob husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 4));
      helper.assertTrue(LogiaDamage.decide(player, player.damageSources().mobAttack(husk)).rule() == HitRule.PASS, "a blow passes");
      husk.discard();
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void intangibilityIsPassiveAndCanBeSwitchedOff(GameTestHelper helper) {
      FakePlayer player = logia(helper, new BlockPos(2, 2, 2));
      PlayerFruitData data = DFAttachments.data(player);
      data.setEnergy(0.0F);
      tick(player, 1);
      helper.assertTrue(LogiaManager.bodyReady(player), "an exhausted body is still element");
      helper.assertFalse(player.hurt(player.damageSources().fall(), 6.0F), "so hits still pass without any energy");
      data.setEnergy(0.0F);
      helper.assertFalse(LogiaManager.setElemental(player, true), "but the whole elemental body needs energy");
      LogiaManager.setIntangible(player, false);
      helper.assertTrue(LogiaDamage.decide(player, player.damageSources().fall()).rule() == HitRule.CONNECT, "switched off, hits connect");
      data.setEnergy(50.0F);
      helper.assertTrue(LogiaManager.setElemental(player, true), "the elemental body still works");
      helper.assertTrue(LogiaDamage.decide(player, player.damageSources().fall()).rule() == HitRule.PASS, "and nothing hits a body of pure element");
      LogiaManager.setElemental(player, false);
      LogiaManager.setIntangible(player, true);
      helper.assertTrue(LogiaManager.intangible(player), "switched back on");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void someHarmAlwaysConnects(GameTestHelper helper) {
      FakePlayer player = logia(helper, new BlockPos(2, 2, 2));
      DamageSource[] always = new DamageSource[]{
         player.damageSources().genericKill(),
         player.damageSources().fellOutOfWorld(),
         player.damageSources().starve(),
         player.damageSources().inWall(),
         player.damageSources().drown(),
         player.damageSources().cramming(),
         player.damageSources().outOfBorder()
      };

      for (DamageSource source : always) {
         helper.assertTrue(LogiaDamage.decide(player, source).rule() == HitRule.CONNECT, source.getMsgId() + " must connect (anti-exploit)");
      }

      float health = player.getHealth();
      helper.assertTrue(player.hurt(player.damageSources().starve(), 2.0F), "starvation hurts a Logia body");
      helper.assertTrue(player.getHealth() < health, "health went down");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void rulesSoftenOrConnectPerElement(GameTestHelper helper) {
      FakePlayer player = logia(helper, new BlockPos(2, 2, 2));
      float health = player.getHealth();
      player.hurt(player.damageSources().lightningBolt(), 10.0F);
      float afterLightning = player.getHealth();
      helper.assertTrue(Math.abs(health - afterLightning - 5.0F) < 0.01F, "lightning is softened by half: lost " + (health - afterLightning));
      player.invulnerableTime = 0;
      player.hurt(player.damageSources().inFire(), 4.0F);
      helper.assertTrue(Math.abs(afterLightning - player.getHealth() - 4.0F) < 0.01F, "this element lets fire connect in full (profile override)");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void seastoneWeaponsAndArmamentBypass(GameTestHelper helper) {
      FakePlayer player = logia(helper, new BlockPos(2, 2, 2));
      Mob husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 4));
      DamageSource blow = player.damageSources().mobAttack(husk);
      helper.assertTrue(LogiaDamage.decide(player, blow).rule() == HitRule.PASS, "a bare blow passes");
      husk.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)DFItems.SEASTONE_INGOT.get()));
      helper.assertTrue(LogiaDamage.decide(player, blow).rule() == HitRule.CONNECT, "a seastone weapon connects");
      husk.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      husk.addEffect(new MobEffectInstance(DFMobEffects.ARMAMENT_COATING, 100));
      helper.assertTrue(LogiaDamage.decide(player, blow).rule() == HitRule.CONNECT, "an Armament-coated blow connects");
      husk.discard();
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 40
   )
   public static void waterSuppressesTheBodyUntilItDries(GameTestHelper helper) {
      FakePlayer player = logia(helper, new BlockPos(2, 2, 2));
      DamageSource blast = player.damageSources().explosion(null, null);
      helper.setBlock(new BlockPos(2, 2, 2), Blocks.WATER);
      tick(player, 1);
      helper.assertTrue(LogiaManager.counterActive(player, "wet"), "standing in water makes the body wet");
      helper.assertTrue(LogiaManager.state(player) == LogiaState.SUPPRESSED, "a wet body is suppressed");
      helper.assertTrue(LogiaDamage.decide(player, blast).rule() == HitRule.CONNECT, "hits connect while wet");
      helper.assertTrue(Weakness.isSuppressed(player), "and the powers are blocked");
      helper.assertTrue(player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(DevilFruits.id("logia_counter_slow")) != null, "and the wet body is slower");
      helper.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
      tick(player, 30);
      helper.assertTrue(LogiaManager.counterActive(player, "wet"), "out of the water it takes a while to dry");
      tick(player, 40);
      helper.assertFalse(LogiaManager.counterActive(player, "wet"), "then it is dry");
      helper.assertTrue(LogiaManager.state(player) == LogiaState.SOLID, "and the body is ready again");
      helper.assertTrue(LogiaDamage.decide(player, blast).rule() == HitRule.PASS, "hits pass again");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void splashesAndBucketsWetTheBody(GameTestHelper helper) {
      FakePlayer player = logia(helper, new BlockPos(2, 2, 2));
      LogiaManager.trigger(player, CounterTrigger.Kind.SPLASH_WATER, null);
      tick(player, 1);
      helper.assertTrue(LogiaManager.counterActive(player, "wet"), "a splash of water wets the body");
      tick(player, 80);
      helper.assertFalse(LogiaManager.counterActive(player, "wet"), "and it dries");
      LogiaManager.trigger(player, CounterTrigger.Kind.WATER_BUCKET, null);
      tick(player, 1);
      helper.assertTrue(LogiaManager.counterActive(player, "wet"), "a water bucket wets it too");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void seastoneForcesTheElementBack(GameTestHelper helper) {
      FakePlayer player = logia(helper, new BlockPos(2, 2, 2));
      helper.assertTrue(LogiaManager.setElemental(player, true), "the body turns into the element");
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)DFItems.SEASTONE_INGOT.get()));
      tick(player, 1);
      helper.assertTrue(LogiaManager.state(player) != LogiaState.ELEMENTAL_BODY, "seastone forces a reform");
      tick(player, 20);
      helper.assertTrue(LogiaManager.state(player) == LogiaState.SUPPRESSED, "and holds the body: " + LogiaManager.state(player));
      helper.assertTrue(LogiaDamage.decide(player, player.damageSources().explosion(null, null)).rule() == HitRule.CONNECT, "hits connect");
      helper.assertFalse(LogiaManager.setElemental(player, true), "no elemental body while seastone holds it");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void elementalBodyDrainsThenReforms(GameTestHelper helper) {
      FakePlayer player = logia(helper, new BlockPos(2, 2, 2));
      PlayerFruitData data = DFAttachments.data(player);
      data.setEnergy(3.0F);
      helper.assertTrue(LogiaManager.setElemental(player, true), "the body turns into the element");
      helper.assertTrue(
         LogiaDamage.decide(player, player.damageSources().lightningBolt()).rule() == HitRule.PASS, "a fully elemental body lets even softened harm pass"
      );
      tick(player, 10);
      helper.assertTrue(
         LogiaManager.state(player) == LogiaState.REFORMING || LogiaManager.state(player) == LogiaState.SOLID,
         "the elemental body drains the energy and reforms when it is empty: " + LogiaManager.state(player) + ", energy " + data.energy()
      );
      helper.assertTrue(data.energy() < 3.0F, "energy was spent, now " + data.energy());
      data.setEnergy(50.0F);
      tick(player, 20);
      helper.assertTrue(LogiaManager.state(player) == LogiaState.SOLID, "and stands solid again");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void partialLimbsExpire(GameTestHelper helper) {
      FakePlayer player = logia(helper, new BlockPos(2, 2, 2));
      LogiaManager.partial(player, 12, 5);
      PlayerFruitData data = DFAttachments.data(player);
      helper.assertTrue(data.logia().state() == LogiaState.PARTIAL && data.logia().limbs() == 12, "the arms turn into the element");
      tick(player, 6);
      helper.assertTrue(data.logia().state() == LogiaState.SOLID && data.logia().limbs() == 0, "and back");
      LogiaManager.forceCounter(player, "wet", 40);
      tick(player, 1);
      LogiaManager.partial(player, 12, 5);
      helper.assertTrue(data.logia().limbs() == 0, "a wet body cannot turn its arms into the element");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void abilitiesTurnLimbsIntoTheElement(GameTestHelper helper) {
      FakePlayer player = logia(helper, new BlockPos(2, 2, 2));
      CastResult result = AbilityManager.press(player, DebugLogia.PULSE);
      helper.assertTrue(result == CastResult.SUCCESS, "pulse casts (" + result + ")");
      helper.assertTrue(DFAttachments.data(player).logia().limbs() == 12, "and turns the arms into the element");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void projectilesFlyThroughTheBody(GameTestHelper helper) {
      FakePlayer player = logia(helper, new BlockPos(2, 2, 2));
      Vec3 at = player.position().add(0.0, 1.0, -2.0);
      Arrow arrow = new Arrow(helper.getLevel(), at.x, at.y, at.z, new ItemStack(Items.ARROW), null);
      arrow.setDeltaMovement(0.0, 0.0, 1.5);
      float health = player.getHealth();
      helper.assertTrue(EventHooks.onProjectileImpact(arrow, new EntityHitResult(player)), "the arrow does not stop at the body");
      helper.assertTrue(EventHooks.onProjectileImpact(arrow, new EntityHitResult(player)), "nor on the next tick while it is still inside");
      helper.assertTrue(player.getHealth() == health, "and does no harm");
      Arrow second = new Arrow(helper.getLevel(), at.x, at.y, at.z, new ItemStack(Items.ARROW), null);
      LogiaManager.forceCounter(player, "wet", 40);
      tick(player, 1);
      helper.assertFalse(EventHooks.onProjectileImpact(second, new EntityHitResult(player)), "a wet body is hit");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void dashReformsWhereTheUserLooksButNeverThroughWalls(GameTestHelper helper) {
      FakePlayer player = logia(helper, new BlockPos(2, 2, 0));
      double z = player.getZ();
      LogiaDash.Result result = LogiaDash.dash(player, player.getEyePosition(), new Vec3(0.0, 0.0, 1.0), 3.0);
      helper.assertTrue(result == LogiaDash.Result.OK, "the dash works: " + result);
      helper.assertTrue(LogiaManager.state(player) == LogiaState.ELEMENTAL_BODY, "the body flows as its element");
      tick(player, LogiaDash.flightTicks(3.0) + 10 + 2);
      helper.assertTrue(LogiaMovement.session(player) == null, "the dash is over");
      helper.assertTrue(LogiaManager.state(player) != LogiaState.ELEMENTAL_BODY, "and the body reforms");
      helper.assertTrue(player.getZ() > z + 1.5, "ahead, z " + player.getZ() + " from " + z);
      helper.assertTrue(LogiaReform.fits(player, player.position(), false), "standing where it fits");
      helper.assertTrue(LogiaDash.dash(player, player.getEyePosition(), new Vec3(0.0, 0.0, 1.0), 3.0) == LogiaDash.Result.COOLDOWN, "then it cools down");
      DFAttachments.data(player).clearCooldowns();
      BlockPos wall = player.blockPosition().south();
      helper.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
      helper.getLevel().setBlockAndUpdate(wall.above(), Blocks.STONE.defaultBlockState());
      double before = player.getZ();
      LogiaDash.Result blocked = LogiaDash.dash(player, player.getEyePosition(), new Vec3(0.0, 0.0, 1.0), 3.0);
      tick(player, LogiaDash.flightTicks(3.0) + 10 + 2);
      helper.assertTrue(
         player.getZ() < (double)wall.getZ(),
         "never reforms behind a wall it cannot pass (" + blocked + "), z " + player.getZ() + " from " + before + ", wall " + wall.getZ()
      );
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 60
   )
   public static void movementModesEndWithASafeReform(GameTestHelper helper) {
      FakePlayer player = logia(helper, new BlockPos(2, 2, 2));
      helper.assertTrue(LogiaMovement.start(player, DebugLogia.DRIFT_MODE), "the drift starts");
      helper.assertTrue(LogiaManager.state(player) == LogiaState.ELEMENTAL_BODY, "as an elemental body");
      helper.assertTrue(DFAttachments.data(player).logia().movementMode() != null, "with its mode known");
      tick(player, 25);
      helper.assertTrue(LogiaMovement.session(player) == null, "the drift ends by itself");
      helper.assertTrue(LogiaReform.fits(player, player.position(), false), "the body stands where it fits");
      tick(player, 10);
      helper.assertTrue(LogiaManager.state(player) == LogiaState.SOLID, "and is solid again");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void aDissolvedBodyNeverResumesAfterALogin(GameTestHelper helper) {
      FakePlayer player = logia(helper, new BlockPos(2, 2, 2));
      PlayerFruitData data = DFAttachments.data(player);
      LogiaManager.partial(player, 8, 100);
      LogiaManager.forceCounter(player, "wet", 300);
      Tag tag = (Tag)PlayerFruitData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
      PlayerFruitData decoded = (PlayerFruitData)PlayerFruitData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
      helper.assertTrue(decoded.sameState(data), "the Logia state survives saving: " + tag);
      helper.assertTrue(decoded.logia().counterTimers().getOrDefault("wet", 0) == 300, "a wet user is still wet after a relog");
      LogiaManager.forceCounter(player, "wet", 0);
      tick(player, 1);
      LogiaManager.forceState(player, LogiaState.ELEMENTAL_BODY, 0);
      helper.setBlock(new BlockPos(2, 2, 2), Blocks.STONE);
      helper.setBlock(new BlockPos(2, 3, 2), Blocks.STONE);
      LogiaManager.settle(player);
      helper.assertTrue(LogiaManager.state(player) == LogiaState.SOLID, "the body is whole after a login");
      helper.assertTrue(LogiaReform.fits(player, player.position(), true), "and out of the stone, at " + player.position());
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void environmentFeedsTheElement(GameTestHelper helper) {
      FakePlayer player = logia(helper, new BlockPos(2, 2, 2));
      helper.assertTrue(LogiaManager.regenMultiplier(player) == 1.0, "plain ground: normal regeneration");
      helper.setBlock(new BlockPos(2, 1, 2), Blocks.WHITE_WOOL);
      helper.assertTrue(LogiaManager.regenMultiplier(player) == 2.0, "on the element's source (wool for the fixture): faster");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void logiaVersusLogiaFollowsTheRegistry(GameTestHelper helper) {
      FakePlayer victim = logia(helper, new BlockPos(2, 2, 2));
      FakePlayer attacker = logia(helper, new BlockPos(2, 2, 4));
      DamageSource blow = victim.damageSources().playerAttack(attacker);
      ResourceLocation element = DebugLogia.ELEMENT;
      helper.assertTrue(LogiaDamage.decide(victim, blow).rule() == HitRule.PASS, "neutral by default: the blow passes");

      try {
         LogiaInteractionRegistry.register(element, element, LogiaInteractionRegistry.Interaction.BYPASS);
         helper.assertTrue(LogiaDamage.decide(victim, blow).rule() == HitRule.CONNECT, "a declared bypass connects");
         LogiaInteractionRegistry.register(element, element, LogiaInteractionRegistry.Interaction.RESISTED);
         helper.assertTrue(LogiaDamage.decide(victim, victim.damageSources().playerAttack(attacker)).rule() == HitRule.PASS, "a resisted element passes");
      } finally {
         LogiaInteractionRegistry.unregister(element, element);
      }

      done(helper, victim, attacker);
   }

   @GameTest(
      template = "platform"
   )
   public static void losingThePowerClearsTheBody(GameTestHelper helper) {
      FakePlayer player = logia(helper, new BlockPos(2, 2, 2));
      LogiaManager.forceCounter(player, "wet", 100);
      tick(player, 1);
      helper.assertTrue(player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(DevilFruits.id("logia_counter_slow")) != null, "wet and slow");
      FruitRules.removePower(player);
      helper.assertTrue(player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(DevilFruits.id("logia_counter_slow")) == null, "no slow without the power");
      helper.assertTrue(DFAttachments.data(player).logia().isDefault(), "and no Logia state");
      done(helper, player);
   }
}
