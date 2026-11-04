package com.devilfruits.fruits.kage_kage.gametest;

import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.CastResult;
import com.devilfruits.core.combat.Stun;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.WorldFruitData;
import com.devilfruits.fruits.kage_kage.KageAbilities;
import com.devilfruits.fruits.kage_kage.KageAim;
import com.devilfruits.fruits.kage_kage.KageEntities;
import com.devilfruits.fruits.kage_kage.KageEvents;
import com.devilfruits.fruits.kage_kage.KageItems;
import com.devilfruits.fruits.kage_kage.KageKage;
import com.devilfruits.fruits.kage_kage.KageTags;
import com.devilfruits.fruits.kage_kage.KageTiming;
import com.devilfruits.fruits.kage_kage.ability.BrickBatAbility;
import com.devilfruits.fruits.kage_kage.ability.DoppelmanAbility;
import com.devilfruits.fruits.kage_kage.ability.ShadowTrapAbility;
import com.devilfruits.fruits.kage_kage.entity.Doppelman;
import com.devilfruits.fruits.kage_kage.entity.HollowCorpse;
import com.devilfruits.fruits.kage_kage.entity.ShadowZombie;
import com.devilfruits.fruits.kage_kage.item.MoriaBladeItem;
import com.devilfruits.fruits.kage_kage.item.MoriaScissorsItem;
import com.devilfruits.fruits.kage_kage.shadow.ShadowVictims;
import com.devilfruits.fruits.kage_kage.shadow.Shadows;
import com.devilfruits.fruits.kage_kage.shadow.StolenShadow;
import com.devilfruits.fruits.kage_kage.shadow.Sunburn;
import com.devilfruits.mobs.pirate.Pirates;
import com.devilfruits.registry.DFAttachments;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("devilfruits")
@PrefixGameTestTemplate(false)
public final class KageGameTests {
   private static final String PLATFORM = "platform";

   private static FakePlayer user(GameTestHelper helper, BlockPos relative) {
      FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "kage_test"));
      BlockPos pos = helper.absolutePos(relative);
      player.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
      player.setDeltaMovement(Vec3.ZERO);
      WorldFruitData.get(helper.getLevel().getServer()).setFree(KageKage.fruit());
      FruitRules.grantPower(player, KageKage.fruit(), false);
      DFAttachments.data(player).setEnergy(1000.0F);
      DFAttachments.data(player).setMastery(8);
      return player;
   }

   private static void done(GameTestHelper helper, FakePlayer player) {
      AbilityManager.cancelAll(player);
      AbilityManager.forget(player.getUUID());
      Shadows.releaseAll(player, Shadows.Release.COMMAND);
      player.getInventory().clearContent();
      FruitRules.removePower(player);
      WorldFruitData.get(helper.getLevel().getServer()).setFree(KageKage.fruit());
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void takeShadow(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));
      Villager villager = (Villager)helper.spawn(EntityType.VILLAGER, new BlockPos(3, 1, 3));
      StolenShadow shadow = Shadows.take(player, villager);
      helper.assertTrue(shadow != null, "the villager's shadow is taken");
      helper.assertTrue(Shadows.isShadowless(villager), "the villager is shadowless");
      helper.assertTrue(player.getUUID().equals(ShadowVictims.get(helper.getLevel().getServer()).owner(villager.getUUID())), "saved under the user");
      helper.assertTrue(Shadows.count(player) == 1 && shadow.type().equals(EntityType.getKey(EntityType.VILLAGER)), "held: one villager shadow");
      helper.assertTrue(Shadows.take(player, villager) == null && Shadows.count(player) == 1, "a shadowless body has nothing left to take");
      villager.discard();
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void noShadowTypes(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));
      Zombie zombie = (Zombie)helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 1, 3));
      ArmorStand stand = (ArmorStand)helper.spawn(EntityType.ARMOR_STAND, new BlockPos(3, 1, 1));
      helper.assertTrue(zombie.getType().is(KageTags.NO_SHADOW) && stand.getType().is(KageTags.NO_SHADOW), "tagged");
      helper.assertTrue(Shadows.take(player, zombie) == null && Shadows.take(player, stand) == null && Shadows.take(player, player) == null, "nothing to take");
      helper.assertTrue(Shadows.count(player) == 0 && !Shadows.isShadowless(zombie), "nothing changed");
      zombie.discard();
      stand.discard();
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void shadowsReturnOnDeath(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));
      Villager villager = (Villager)helper.spawn(EntityType.VILLAGER, new BlockPos(3, 1, 3));
      Shadows.take(player, villager);
      NeoForge.EVENT_BUS.post(new LivingDeathEvent(player, helper.getLevel().damageSources().generic()));
      helper.assertTrue(!Shadows.isShadowless(villager), "the villager has its shadow back");
      helper.assertTrue(Shadows.count(player) == 0 && !ShadowVictims.get(helper.getLevel().getServer()).isVictim(villager.getUUID()), "nothing held");
      villager.discard();
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void shadowsReturnOnPowerLost(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));
      Villager villager = (Villager)helper.spawn(EntityType.VILLAGER, new BlockPos(3, 1, 3));
      Shadows.take(player, villager);
      FruitRules.removePower(player);
      helper.assertTrue(!Shadows.isShadowless(villager) && Shadows.count(player) == 0, "returned with the power");
      villager.discard();
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void lazyReturnOnJoin(GameTestHelper helper) {
      Villager freed = (Villager)helper.spawn(EntityType.VILLAGER, new BlockPos(3, 1, 3));
      Villager stolen = (Villager)helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 3));
      freed.setData(Shadows.SHADOWLESS, Boolean.TRUE);
      ShadowVictims victims = ShadowVictims.get(helper.getLevel().getServer());
      victims.put(stolen.getUUID(), UUID.randomUUID());
      Shadows.onJoin(freed);
      Shadows.onJoin(stolen);
      helper.assertTrue(!Shadows.isShadowless(freed), "not in the table: the shadow is back");
      helper.assertTrue(Shadows.isShadowless(stolen), "in the table: shadowless again");
      victims.remove(stolen.getUUID());
      freed.discard();
      stolen.discard();
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void capAndBonuses(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));
      double base = player.getAttributeBaseValue(Attributes.MAX_HEALTH);
      Villager a = (Villager)helper.spawn(EntityType.VILLAGER, new BlockPos(3, 1, 3));
      Villager b = (Villager)helper.spawn(EntityType.VILLAGER, new BlockPos(3, 1, 1));
      Shadows.take(player, a);
      Shadows.take(player, b);
      double health = player.getAttributeValue(Attributes.MAX_HEALTH);
      helper.assertTrue(Math.abs(health - base - 2.0 * KageKage.setting("shadowHealthBonus")) < 1.0E-6, "max health +2 shadows: " + health);
      helper.assertTrue(Math.abs((double)Shadows.damageMultiplier(player) - (1.0 + 2.0 * KageKage.setting("shadowDamageBonus"))) < 1.0E-5, "damage bonus");
      List<StolenShadow> full = new ArrayList<>();

      for (int i = 0; i < (int)KageKage.setting("maxShadows"); i++) {
         full.add(new StolenShadow(UUID.randomUUID(), EntityType.getKey(EntityType.PIG), 10.0F, 1.0F, "pig"));
      }

      player.setData(Shadows.HELD, new Shadows.Held(full));
      Villager c = (Villager)helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 3));
      helper.assertTrue(Shadows.refusal(player, c) != null && Shadows.take(player, c) == null, "a full collection takes no more");
      Shadows.releaseAll(player, Shadows.Release.COMMAND);
      helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MAX_HEALTH) - base) < 1.0E-6, "the bonus goes with the shadows");
      a.discard();
      b.discard();
      c.discard();
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void spendStrongest(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));
      Villager weak = (Villager)helper.spawn(EntityType.VILLAGER, new BlockPos(3, 1, 3));
      LivingEntity strong = (LivingEntity)helper.spawn(Pirates.CAPTAIN.get(), new BlockPos(3, 1, 1));
      Shadows.take(player, weak);
      Shadows.take(player, strong);
      StolenShadow spent = Shadows.spendStrongest(player);
      helper.assertTrue(spent != null && spent.victim().equals(strong.getUUID()), "the captain's shadow is the strongest");
      helper.assertTrue(Shadows.count(player) == 1 && Shadows.isShadowless(strong), "one left; the captain stays shadowless");
      weak.discard();
      strong.discard();
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void sunburnRule(GameTestHelper helper) {
      helper.assertTrue(Sunburn.exposed(true, true, false, 0.9F), "full sun burns");
      helper.assertTrue(!Sunburn.exposed(false, true, false, 0.9F), "night spares");
      helper.assertTrue(!Sunburn.exposed(true, false, false, 0.9F), "a roof spares");
      helper.assertTrue(!Sunburn.exposed(true, true, true, 0.9F), "rain and water spare");
      helper.assertTrue(!Sunburn.exposed(true, true, false, 0.4F), "shade spares");
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void collectionNeedsScissors(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));
      helper.assertTrue(KageAbilities.COLLECTION.checkRequirements(player, DFAttachments.data(player)) != null, "no scissors: refused");
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)KageItems.MORIA_SCISSORS.get()));
      helper.assertTrue(KageAbilities.COLLECTION.checkRequirements(player, DFAttachments.data(player)) != null, "nothing in reach: refused");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void scissorsSplitAndJoin(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));
      ItemStack scissors = new ItemStack((ItemLike)KageItems.MORIA_SCISSORS.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, scissors);
      helper.assertTrue(
         Math.abs(stat(scissors, Attributes.ATTACK_DAMAGE, 1.0) - 7.0) < 1.0E-6 && Math.abs(stat(scissors, Attributes.ATTACK_SPEED, 4.0) - 1.2) < 1.0E-6,
         "joined: 7 damage, 1.2 a second"
      );
      player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH));
      helper.assertTrue(!MoriaScissorsItem.split(player), "a busy off hand cannot take the blade");
      player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
      helper.assertTrue(MoriaScissorsItem.split(player), "split");
      helper.assertTrue(player.getOffhandItem().getItem() instanceof MoriaBladeItem && MoriaScissorsItem.isSplit(player.getMainHandItem()), "a blade per hand");
      helper.assertTrue(
         Math.abs(stat(scissors, Attributes.ATTACK_DAMAGE, 1.0) - 4.0) < 1.0E-6 && Math.abs(stat(scissors, Attributes.ATTACK_SPEED, 4.0) - 3.3) < 1.0E-6,
         "split: 4 damage, 3.3 a second"
      );
      InteractionHand[] arms = new InteractionHand[3];

      for (int i = 0; i < arms.length; i++) {
         player.swing(InteractionHand.MAIN_HAND);
         arms[i] = player.swingingArm;
      }

      helper.assertTrue(
         arms[0] == InteractionHand.MAIN_HAND && arms[1] == InteractionHand.OFF_HAND && arms[2] == InteractionHand.MAIN_HAND, "the hands take turns"
      );
      MoriaScissorsItem.join(player);
      helper.assertTrue(player.getOffhandItem().isEmpty() && !MoriaScissorsItem.isSplit(player.getMainHandItem()), "joined again");
      done(helper, player);
   }

   private static double stat(ItemStack stack, Holder<Attribute> attribute, double base) {
      double[] v = new double[]{base};
      stack.getAttributeModifiers().forEach(EquipmentSlot.MAINHAND, (a, m) -> {
         if (a.equals(attribute)) {
            v[0] += m.amount();
         }
      });
      return v[0];
   }

   @GameTest(
      template = "platform"
   )
   public static void bladeJoinsWhenUnpaired(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));
      player.getInventory().selected = 0;
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)KageItems.MORIA_SCISSORS.get()));
      MoriaScissorsItem.split(player);
      player.getInventory().selected = 1;
      player.getInventory().tick();
      helper.assertTrue(player.getOffhandItem().isEmpty(), "the blade is gone");
      helper.assertTrue(!MoriaScissorsItem.isSplit(player.getInventory().getItem(0)), "the scissors are whole");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void cadaverFlesh(GameTestHelper helper) {
      Villager villager = (Villager)helper.spawn(EntityType.VILLAGER, new BlockPos(3, 1, 3));
      LivingEntity captain = (LivingEntity)helper.spawn(Pirates.CAPTAIN.get(), new BlockPos(1, 1, 3));
      ItemStack scissors = new ItemStack((ItemLike)KageItems.MORIA_SCISSORS.get());
      helper.assertTrue(villager.getType().is(KageTags.FLESHY) && captain.getType().is(KageTags.FLESHY), "fleshy");
      int base = (int)KageKage.setting("fleshPerKill");
      helper.assertTrue(KageEvents.fleshCount(helper.getLevel(), villager, scissors) == base, "a villager: the base count");
      helper.assertTrue(KageEvents.fleshCount(helper.getLevel(), captain, scissors) == base + (int)KageKage.setting("captainExtraFlesh"), "a captain: more");
      villager.discard();
      captain.discard();
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void recipes(GameTestHelper helper) {
      for (String name : new String[]{"moria_scissors", "cadaver_arm", "cadaver_leg", "cadaver_head", "cadaver_torso", "hollow_corpse"}) {
         Optional<RecipeHolder<?>> recipe = helper.getLevel().getRecipeManager().byKey(KageKage.id(name));
         helper.assertTrue(recipe.isPresent(), "recipe " + name);
      }

      RecipeHolder<?> corpse = (RecipeHolder<?>)helper.getLevel().getRecipeManager().byKey(KageKage.id("hollow_corpse")).orElseThrow();
      helper.assertTrue(corpse.value().getResultItem(helper.getLevel().registryAccess()).is((Item)KageItems.HOLLOW_CORPSE.get()), "makes a Hollow Corpse");
      helper.succeed();
   }

   private static Villager villager(GameTestHelper helper, BlockPos relative) {
      Villager v = (Villager)helper.spawnWithNoFreeWill(EntityType.VILLAGER, relative);
      v.setInvulnerable(false);
      return v;
   }

   @GameTest(
      template = "platform"
   )
   public static void aegisIntercepts(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));
      Vec3 near = player.position().add(0.0, 1.0, 1.5);
      Arrow before = new Arrow(helper.getLevel(), near.x, near.y, near.z, new ItemStack(Items.ARROW), null);
      helper.getLevel().addFreshEntity(before);
      helper.assertTrue(!BrickBatAbility.intercept(player, before), "Aegis down: nothing taken");
      helper.assertTrue(AbilityManager.press(player, KageAbilities.BRICK_BAT) == CastResult.TOGGLED && BrickBatAbility.raised(player), "Aegis up");
      float energy = DFAttachments.data(player).energy();
      helper.assertTrue(BrickBatAbility.intercept(player, before) && before.isRemoved(), "an arrow breaks on it");
      helper.assertTrue(
         Math.abs((double)(energy - DFAttachments.data(player).energy()) - KageAbilities.BRICK_BAT.config().extra("interceptEnergy")) < 0.001, "paid per catch"
      );
      SmallFireball ball = new SmallFireball(helper.getLevel(), near.x, near.y, near.z, new Vec3(0.0, 0.0, -0.5));
      helper.getLevel().addFreshEntity(ball);
      helper.assertTrue(
         BrickBatAbility.intercept(player, ball) && !ball.isRemoved() && ball.getOwner() == player && ball.getDeltaMovement().z > 0.0, "a fireball goes back"
      );
      ball.discard();
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void aegisLimit(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));
      AbilityManager.press(player, KageAbilities.BRICK_BAT);
      int cap = (int)KageAbilities.BRICK_BAT.config().extra("interceptsPerSecond");
      Vec3 near = player.position().add(0.0, 1.0, 1.5);
      int taken = 0;

      for (int i = 0; i < cap + 2; i++) {
         Arrow a = new Arrow(helper.getLevel(), near.x, near.y, near.z, new ItemStack(Items.ARROW), null);
         helper.getLevel().addFreshEntity(a);
         if (BrickBatAbility.intercept(player, a)) {
            taken++;
         } else {
            a.discard();
         }
      }

      helper.assertTrue(taken == cap, "the cap per second: " + taken + " of " + cap);
      helper.runAfterDelay(21L, () -> {
         DFAttachments.data(player).setEnergy(1.0F);
         Arrow ax = new Arrow(helper.getLevel(), near.x, near.y, near.z, new ItemStack(Items.ARROW), null);
         helper.getLevel().addFreshEntity(ax);
         helper.assertTrue(!BrickBatAbility.intercept(player, ax), "no energy: it flies through");
         ax.discard();
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform"
   )
   public static void swarmTarget(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 2, 0));
      Villager aimed = villager(helper, new BlockPos(1, 2, 3));
      Villager near = villager(helper, new BlockPos(3, 2, 1));
      player.lookAt(Anchor.EYES, aimed.getBoundingBox().getCenter());
      helper.assertTrue(BrickBatAbility.swarmTarget(player, KageAbilities.BRICK_BAT.config()) == aimed, "the aimed one");
      aimed.discard();
      helper.assertTrue(BrickBatAbility.swarmTarget(player, KageAbilities.BRICK_BAT.config()) == near, "else the nearest");
      near.discard();
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 120
   )
   public static void spearStuns(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 0));
      helper.assertTrue(AbilityManager.press(player, KageAbilities.SPEAR) != CastResult.SUCCESS, "nothing aimed at: refused");
      Villager target = villager(helper, new BlockPos(2, 2, 3));
      player.lookAt(Anchor.EYES, target.getBoundingBox().getCenter());
      helper.assertTrue(AbilityManager.press(player, KageAbilities.SPEAR) == CastResult.SUCCESS, "cast");
      float health = target.getHealth();
      int pierce = KageTiming.spearPierce(KageTiming.spearRun(Math.sqrt(player.distanceToSqr(target))));
      helper.runAfterDelay((long)(pierce - 1), () -> helper.assertTrue(target.getHealth() == health && !Stun.stunned(target), "nothing before the pierce"));
      helper.runAfterDelay((long)(pierce + 2), () -> {
         helper.assertTrue(target.getHealth() < health && Stun.stunned(target), "hurt and stunned on the pierce");
         target.discard();
         done(helper, player);
      });
   }

   private static Villager tough(GameTestHelper helper, BlockPos relative) {
      Villager v = villager(helper, relative);
      v.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
      v.setHealth(200.0F);
      return v;
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 200
   )
   public static void trapSwallows(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 0));
      Villager target = tough(helper, new BlockPos(2, 2, 3));
      player.lookAt(Anchor.EYES, target.getBoundingBox().getCenter());
      Vec3 spot = KageAim.groundSpot(player, KageAbilities.TRAP.config().range(), KageAbilities.TRAP.config().extra("aimConeDegrees"));
      Vec3 casket = ShadowTrapAbility.casketSpot(player, spot);
      helper.assertTrue(AbilityManager.press(player, KageAbilities.TRAP) == CastResult.SUCCESS, "cast");
      int run = KageTiming.batRun(Math.sqrt(player.distanceToSqr(casket)));
      int hold = (int)Math.round(KageAbilities.TRAP.config().extra("holdSeconds") * 20.0);
      helper.runAfterDelay((long)(KageTiming.trapPull(run) - 1), () -> helper.assertTrue(!Stun.stunned(target), "free until the pull"));
      helper.runAfterDelay(
         (long)(KageTiming.trapShut(run) + 2),
         () -> helper.assertTrue(
               Stun.stunned(target) && target.position().distanceTo(casket) < 0.6, "pulled into the casket and held: " + target.position().distanceTo(casket)
            )
      );
      helper.runAfterDelay(
         (long)(KageTiming.trapSpit(run, hold) + 3),
         () -> {
            helper.assertTrue(
               !Stun.stunned(target) && (double)target.getHealth() < 200.0 - KageAbilities.TRAP.config().damage() * 0.9,
               "spat out, hurt while inside: " + target.getHealth()
            );
            target.discard();
            done(helper, player);
         }
      );
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 160
   )
   public static void handSlaps(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 0));
      Villager target = tough(helper, new BlockPos(2, 2, 4));
      Villager far = tough(helper, new BlockPos(0, 2, 0));
      player.lookAt(Anchor.EYES, target.getBoundingBox().getCenter());
      helper.assertTrue(AbilityManager.press(player, KageAbilities.HAND) == CastResult.SUCCESS, "cast");
      int impact = KageTiming.handImpact(KageTiming.batRun(Math.sqrt(player.distanceToSqr(target.position()))));
      helper.runAfterDelay((long)(impact - 1), () -> helper.assertTrue(target.getHealth() == 200.0F, "nothing before the slap"));
      helper.runAfterDelay((long)(impact + 2), () -> {
         helper.assertTrue(target.getHealth() < 200.0F && target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "slapped and slowed");
         helper.assertTrue(far.getHealth() == 200.0F, "beyond the radius: untouched");
         target.discard();
         far.discard();
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 120
   )
   public static void doppelmanSummon(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 2));
      float energy = DFAttachments.data(player).energy();
      helper.assertTrue(AbilityManager.press(player, KageAbilities.DOPPELMAN) == CastResult.SUCCESS, "cast");
      Doppelman d = DoppelmanAbility.doubleOf(player);
      helper.assertTrue(
         d != null
            && Math.abs((double)d.getMaxHealth() - KageAbilities.DOPPELMAN.config().extra("health")) < 0.001
            && Math.abs(d.getAttributeValue(Attributes.ATTACK_DAMAGE) - KageAbilities.DOPPELMAN.config().damage()) < 0.5,
         "the double, its health and damage"
      );
      helper.assertTrue(
         (double)Math.abs(energy - DFAttachments.data(player).energy() - (float)KageAbilities.DOPPELMAN.config().energyCost()) < 0.001, "the summon is paid"
      );
      helper.assertTrue(!DFAttachments.data(player).isOnCooldown(KageAbilities.DOPPELMAN.id(), helper.getLevel().getGameTime()), "no cooldown for a summon");
      helper.assertTrue(!d.hurt(helper.getLevel().damageSources().generic(), 5.0F) && d.state() == 1, "untouchable while it rises");
      helper.runAfterDelay(42L, () -> {
         helper.assertTrue(d.isAlive() && d.state() == 0, "standing after the rise");
         helper.assertTrue(!d.mayFight(player) && !d.hurt(helper.getLevel().damageSources().playerAttack(player), 5.0F), "never against its user");
         d.discard();
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 160
   )
   public static void doppelmanOrders(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 0));
      AbilityManager.press(player, KageAbilities.DOPPELMAN);
      Doppelman d = DoppelmanAbility.doubleOf(player);
      helper.assertTrue(d != null, "summoned");
      Villager target = tough(helper, new BlockPos(2, 2, 4));
      helper.runAfterDelay(42L, () -> {
         player.lookAt(Anchor.EYES, target.getBoundingBox().getCenter());
         helper.assertTrue(AbilityManager.press(player, KageAbilities.DOPPELMAN) == CastResult.SUCCESS && d.getTarget() == target, "attack order");
         player.setXRot(-80.0F);
         AbilityManager.press(player, KageAbilities.DOPPELMAN);
         helper.assertTrue(d.state() == 2, "recall: it sinks");
      });
      helper.runAfterDelay(
         66L,
         () -> {
            helper.assertTrue(d.isRemoved() && DoppelmanAbility.doubleOf(player) == null, "gone after the sink");
            target.discard();
            AbilityManager.press(player, KageAbilities.DOPPELMAN);
            Doppelman again = DoppelmanAbility.doubleOf(player);
            helper.assertTrue(again != null, "a new one");
            again.kill();
            helper.assertTrue(
               DFAttachments.data(player).isOnCooldown(KageAbilities.DOPPELMAN.id(), helper.getLevel().getGameTime()), "its death starts the cooldown"
            );
            again.discard();
            done(helper, player);
         }
      );
   }

   private static HollowCorpse corpse(GameTestHelper helper, BlockPos relative) {
      HollowCorpse c = (HollowCorpse)KageEntities.HOLLOW_CORPSE.get().create(helper.getLevel());
      Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(relative));
      c.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
      helper.getLevel().addFreshEntity(c);
      return c;
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 100
   )
   public static void zombieRises(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 0));
      Villager donor = villager(helper, new BlockPos(0, 2, 3));
      StolenShadow shadow = Shadows.take(player, donor);
      helper.assertTrue(shadow != null && Shadows.count(player) == 1, "a shadow held");
      HollowCorpse body = corpse(helper, new BlockPos(2, 2, 2));
      helper.assertTrue(body.inject(player) && Shadows.count(player) == 0 && body.injected(), "spent into the corpse");
      helper.assertTrue(Shadows.isShadowless(donor), "the donor stays shadowless");
      helper.runAfterDelay(
         26L,
         () -> {
            List<ShadowZombie> zs = helper.getLevel().getEntitiesOfClass(ShadowZombie.class, body.getBoundingBox().inflate(3.0));
            helper.assertTrue(zs.size() == 1 && body.isRemoved(), "a zombie got up out of it");
            ShadowZombie z = zs.get(0);
            float[] stats = ShadowZombie.stats(shadow);
            helper.assertTrue(
               player.getUUID().equals(z.ownerId())
                  && (double)Math.abs(z.getMaxHealth() - stats[0]) < 0.001
                  && Math.abs(z.getAttributeValue(Attributes.ATTACK_DAMAGE) - (double)stats[1]) < 0.001,
               "the donor's strength, its user's"
            );
            helper.assertTrue(ShadowVictims.get(helper.getLevel().getServer()).zombies(player.getUUID()) == 1, "counted");
            Shadows.releaseAll(player, Shadows.Release.COMMAND);
            helper.assertTrue(
               !z.isAlive() && !Shadows.isShadowless(donor) && ShadowVictims.get(helper.getLevel().getServer()).zombies(player.getUUID()) == 0,
               "the shadows went back: it fell"
            );
            z.discard();
            donor.discard();
            done(helper, player);
         }
      );
   }

   @GameTest(
      template = "platform"
   )
   public static void zombieRules(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 0));
      helper.assertTrue(HollowCorpse.refusal(player) != null, "no shadow held: refused");
      ShadowVictims victims = ShadowVictims.get(helper.getLevel().getServer());
      int max = (int)KageKage.setting("maxZombies");
      List<UUID> fake = new ArrayList<>();

      for (int i = 0; i < max; i++) {
         UUID id = UUID.randomUUID();
         fake.add(id);
         victims.putZombie(id, player.getUUID());
      }

      player.setData(
         Shadows.HELD,
         new Shadows.Held(List.of(new StolenShadow(UUID.randomUUID(), EntityType.VILLAGER.builtInRegistryHolder().key().location(), 20.0F, 1.0F, "v")))
      );
      helper.assertTrue(HollowCorpse.refusal(player) != null, "at the cap: refused");
      fake.forEach(victims::removeZombie);
      helper.assertTrue(HollowCorpse.refusal(player) == null, "below the cap, a shadow held: allowed");
      float[] weak = ShadowZombie.stats(new StolenShadow(UUID.randomUUID(), EntityType.VILLAGER.builtInRegistryHolder().key().location(), 20.0F, 0.0F, "a"));
      float[] strong = ShadowZombie.stats(
         new StolenShadow(UUID.randomUUID(), EntityType.IRON_GOLEM.builtInRegistryHolder().key().location(), 100.0F, 15.0F, "b")
      );
      helper.assertTrue(strong[0] > weak[0] && strong[1] > weak[1], "a stronger shadow, a stronger zombie");
      ShadowZombie z = (ShadowZombie)KageEntities.SHADOW_ZOMBIE.get().create(helper.getLevel());
      StolenShadow saved = new StolenShadow(UUID.randomUUID(), EntityType.VILLAGER.builtInRegistryHolder().key().location(), 20.0F, 1.0F, "kept");
      z.raise(player, saved);
      CompoundTag tag = new CompoundTag();
      z.saveWithoutId(tag);
      ShadowZombie loaded = (ShadowZombie)KageEntities.SHADOW_ZOMBIE.get().create(helper.getLevel());
      loaded.load(tag);
      helper.assertTrue(
         saved.equals(loaded.shadow()) && player.getUUID().equals(loaded.ownerId()) && !loaded.rising(), "saved and loaded with its shadow and user"
      );
      victims.removeZombie(z.getUUID());
      player.setData(Shadows.HELD, Shadows.Held.NONE);
      done(helper, player);
   }

   private static void steal(FakePlayer user, FakePlayer victim) {
      List<StolenShadow> held = new ArrayList<>(Shadows.held(user));
      held.add(new StolenShadow(victim.getUUID(), EntityType.getKey(EntityType.PLAYER), victim.getMaxHealth(), 1.0F, victim.getName().getString()));
      user.setData(Shadows.HELD, new Shadows.Held(List.copyOf(held)));
      ShadowVictims.get(user.server).put(victim.getUUID(), user.getUUID());
      victim.setData(Shadows.SHADOWLESS, Boolean.TRUE);
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 100
   )
   public static void playerShadowComesHome(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 0));
      FakePlayer victim = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "kage_victim"));
      BlockPos at = helper.absolutePos(new BlockPos(2, 2, 4));
      victim.moveTo((double)at.getX() + 0.5, (double)at.getY(), (double)at.getZ() + 0.5, 180.0F, 0.0F);
      victim.setGameMode(GameType.SURVIVAL);
      ShadowVictims victims = ShadowVictims.get(helper.getLevel().getServer());
      steal(player, victim);
      helper.assertTrue(Shadows.isShadowless(victim) && Shadows.count(player) == 1, "a player's shadow taken");
      NeoForge.EVENT_BUS.post(new LivingDeathEvent(victim, helper.getLevel().damageSources().generic()));
      helper.assertTrue(
         Shadows.isShadowless(victim) && victims.isVictim(victim.getUUID()) && Shadows.count(player) == 1, "the player's own death gives nothing back"
      );
      HollowCorpse body = corpse(helper, new BlockPos(2, 2, 2));
      helper.assertTrue(body.inject(player), "into a corpse");
      helper.runAfterDelay(26L, () -> {
         List<ShadowZombie> zs = helper.getLevel().getEntitiesOfClass(ShadowZombie.class, body.getBoundingBox().inflate(3.0));
         helper.assertTrue(zs.size() == 1 && Shadows.isShadowless(victim), "the zombie walks with it, the player still without");
         zs.get(0).kill();
         helper.assertTrue(!victims.isVictim(victim.getUUID()), "the zombie fell: the shadow is home");
         Shadows.onJoin(victim);
         helper.assertTrue(!Shadows.isShadowless(victim), "the player has its shadow again");
         steal(player, victim);
         helper.assertTrue(Shadows.isShadowless(victim) && Shadows.count(player) == 1, "taken again");
         NeoForge.EVENT_BUS.post(new LivingDeathEvent(player, helper.getLevel().damageSources().generic()));
         helper.assertTrue(!victims.isVictim(victim.getUUID()) && Shadows.count(player) == 0, "the thief died: home, the count at zero");
         Shadows.onJoin(victim);
         helper.assertTrue(!Shadows.isShadowless(victim), "the player has its shadow again");
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 120
   )
   public static void doppelmanSwap(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 2));
      helper.assertTrue(!DoppelmanAbility.swap(player), "no double: nothing to swap with");
      AbilityManager.press(player, KageAbilities.DOPPELMAN);
      Doppelman d = DoppelmanAbility.doubleOf(player);
      helper.assertTrue(d != null && !DoppelmanAbility.swap(player), "not while it rises");
      helper.runAfterDelay(
         42L,
         () -> {
            Vec3 mine = player.position();
            float energy = DFAttachments.data(player).energy();
            helper.assertTrue(DoppelmanAbility.swap(player) && d.position().distanceTo(mine) < 0.001, "swapped: the double where the user stood");
            helper.assertTrue(
               Math.abs((double)(energy - DFAttachments.data(player).energy()) - KageAbilities.DOPPELMAN.config().extra("swapEnergy")) < 0.001, "paid"
            );
            helper.assertTrue(!DoppelmanAbility.swap(player), "not again so soon");
            d.discard();
            done(helper, player);
         }
      );
   }
}
