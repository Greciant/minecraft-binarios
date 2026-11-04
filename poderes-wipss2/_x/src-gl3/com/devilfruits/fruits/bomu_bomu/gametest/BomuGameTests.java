package com.devilfruits.fruits.bomu_bomu.gametest;

import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.CastResult;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.fx.ExplosionProfile;
import com.devilfruits.core.rules.DFGameRules;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.rules.WorldFruitData;
import com.devilfruits.core.world.BlockRestoreData;
import com.devilfruits.fruits.bomu_bomu.BomuAbilities;
import com.devilfruits.fruits.bomu_bomu.BomuBomu;
import com.devilfruits.fruits.bomu_bomu.BomuEntities;
import com.devilfruits.fruits.bomu_bomu.BomuItems;
import com.devilfruits.fruits.bomu_bomu.ability.ExplosivePunchAbility;
import com.devilfruits.fruits.bomu_bomu.ability.SelfDestructAbility;
import com.devilfruits.fruits.bomu_bomu.entity.BombBullet;
import com.devilfruits.fruits.bomu_bomu.entity.BoogerBomb;
import com.devilfruits.fruits.bomu_bomu.explosion.BombExplosionService;
import com.devilfruits.fruits.bomu_bomu.explosion.ExplosionSpec;
import com.devilfruits.fruits.bomu_bomu.item.BombPistolItem;
import com.devilfruits.fruits.bomu_bomu.trade.PistolTrade;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFDamageTypes;
import com.devilfruits.registry.DFItems;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent.Detonate;
import net.neoforged.neoforge.event.level.ExplosionEvent.Start;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("devilfruits")
@PrefixGameTestTemplate(false)
public final class BomuGameTests {
   private static final String PLATFORM = "platform";

   private static FakePlayer user(GameTestHelper helper, BlockPos relative) {
      FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "bomu_test"));
      BlockPos pos = helper.absolutePos(relative);
      player.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
      player.setDeltaMovement(Vec3.ZERO);
      WorldFruitData.get(helper.getLevel().getServer()).setFree(BomuBomu.fruit());
      FruitRules.grantPower(player, BomuBomu.fruit(), false);
      DFAttachments.data(player).setEnergy(1000.0F);
      return player;
   }

   private static void done(GameTestHelper helper, FakePlayer player) {
      AbilityManager.cancelAll(player);
      AbilityManager.forget(player.getUUID());
      ExplosivePunchAbility.forget(player.getUUID());
      SelfDestructAbility.forget(player);
      BombExplosionService.forget(player.getUUID());
      player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      FruitRules.removePower(player);
      WorldFruitData.get(helper.getLevel().getServer()).setFree(BomuBomu.fruit());
      helper.succeed();
   }

   private static boolean blocked(FakePlayer player, DamageSource source) {
      return ((LivingIncomingDamageEvent)NeoForge.EVENT_BUS.post(new LivingIncomingDamageEvent(player, new DamageContainer(source, 10.0F)))).isCanceled();
   }

   private static ExplosionSpec spec(float radius, float damage, ExplosionProfile profile) {
      return new ExplosionSpec(radius, radius, 1.0F, damage, 1.0F, 0.0F, profile, BomuAbilities.SHOT.id());
   }

   @GameTest(
      template = "platform"
   )
   public static void immuneToEveryExplosionSource(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      Level level = helper.getLevel();
      Creeper creeper = (Creeper)helper.spawn(EntityType.CREEPER, new BlockPos(4, 1, 4));
      LargeFireball fireball = new LargeFireball(EntityType.FIREBALL, level);
      WitherSkull skull = new WitherSkull(EntityType.WITHER_SKULL, level);
      FakePlayer other = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "bomu_other"));

      for (DamageSource s : List.of(
         level.damageSources().explosion(null, null),
         level.damageSources().explosion(creeper, creeper),
         level.damageSources().fireball(fireball, null),
         level.damageSources().witherSkull(skull, creeper),
         level.damageSources().fireworks(null, null),
         level.damageSources().badRespawnPointExplosion(Vec3.ZERO),
         DFDamageTypes.source(level, BomuBomu.EXPLOSION_DAMAGE, other)
      )) {
         helper.assertTrue(blocked(player, s), "immune to " + s.getMsgId());
      }

      helper.assertTrue(
         !blocked(player, level.damageSources().generic()) && !blocked(player, level.damageSources().playerAttack(other)), "other damage still hurts"
      );
      creeper.discard();
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      batch = "bomu_blast"
   )
   public static void noKnockbackUnlessNullified(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      Vec3 c = player.position().add(1.5, 0.5, 0.0);
      Explosion blast = new Explosion(
         helper.getLevel(),
         null,
         null,
         null,
         c.x,
         c.y,
         c.z,
         3.0F,
         false,
         BlockInteraction.KEEP,
         ParticleTypes.EXPLOSION,
         ParticleTypes.EXPLOSION_EMITTER,
         SoundEvents.GENERIC_EXPLODE
      );
      List<Entity> list = new ArrayList<>(List.of(player));
      NeoForge.EVENT_BUS.post(new Detonate(helper.getLevel(), blast, list));
      helper.assertTrue(list.isEmpty(), "the user left the blast's list");
      ExplosionKnockbackEvent push = (ExplosionKnockbackEvent)NeoForge.EVENT_BUS
         .post(new ExplosionKnockbackEvent(helper.getLevel(), blast, player, new Vec3(1.0, 0.5, 0.0)));
      helper.assertTrue(push.getKnockbackVelocity().lengthSqr() == 0.0, "no knockback");
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)DFItems.SEASTONE_INGOT.get()));
      helper.assertTrue(!BomuBomu.immune(player) && !blocked(player, helper.getLevel().damageSources().explosion(null, null)), "seastone: immunity lost");
      list.add(player);
      NeoForge.EVENT_BUS.post(new Detonate(helper.getLevel(), blast, list));
      push = (ExplosionKnockbackEvent)NeoForge.EVENT_BUS.post(new ExplosionKnockbackEvent(helper.getLevel(), blast, player, new Vec3(1.0, 0.5, 0.0)));
      helper.assertTrue(list.size() == 1 && push.getKnockbackVelocity().lengthSqr() > 0.0, "nullified: hit and thrown like anyone");
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      batch = "bomu_blast"
   )
   public static void punchArmsAndIsSpentByABlow(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 1, 2));
      helper.assertTrue(AbilityManager.press(player, BomuAbilities.PUNCH) == CastResult.SUCCESS && ExplosivePunchAbility.armed(player), "armed");
      player.swing(InteractionHand.MAIN_HAND);
      helper.assertTrue(ExplosivePunchAbility.armed(player), "a swing at the air keeps it armed");
      helper.assertTrue(AbilityManager.press(player, BomuAbilities.PUNCH) != CastResult.SUCCESS, "cannot arm twice");
      Zombie zombie = (Zombie)helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 1, 2));
      float before = zombie.getHealth();
      player.attack(zombie);
      helper.assertTrue(!ExplosivePunchAbility.armed(player), "the blow spent it");
      helper.assertTrue(zombie.getHealth() < before - 4.0F, "the fist went off on the zombie: " + zombie.getHealth());
      zombie.discard();
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      batch = "bomu_booger",
      timeoutTicks = 60
   )
   public static void boogerSticksThenDetonates(GameTestHelper helper) {
      for (int y = 1; y <= 3; y++) {
         for (int z = 1; z <= 3; z++) {
            helper.setBlock(5, y, z, Blocks.STONE);
         }
      }

      FakePlayer player = user(helper, new BlockPos(1, 1, 2));
      Zombie zombie = (Zombie)helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 1, 1));
      float before = zombie.getHealth();
      Vec3 from = player.getEyePosition();
      BoogerBomb booger = BoogerBomb.shoot(player, from, new Vec3(1.5, 0.0, 0.0), spec(1.8F, 6.0F, ExplosionProfile.SMALL), 4);
      boolean[] stuck = new boolean[]{false};
      helper.onEachTick(() -> stuck[0] |= booger.stuck());
      helper.succeedWhen(() -> {
         helper.assertTrue(booger.isRemoved(), "still clinging");
         helper.assertTrue(stuck[0], "it stuck before it went off");
         helper.assertTrue(zombie.getHealth() < before, "the blast hurt the zombie");
         zombie.discard();
         FruitRules.removePower(player);
      });
   }

   @GameTest(
      template = "platform",
      batch = "bomu_shot"
   )
   public static void pistolFiresBreath(GameTestHelper helper) {
      helper.assertTrue(
         BomuAbilities.CANNON.config().cooldownTicks() == 300 && BomuAbilities.CANNON.castType() == CastType.INSTANT,
         "Nose Fancy Cannon: one shot, then a 15 s cooldown"
      );
      FakePlayer player = user(helper, new BlockPos(1, 1, 2));
      helper.assertTrue(AbilityManager.press(player, BomuAbilities.SHOT) == CastResult.LOCKED, "unlocked at mastery 4");
      Mastery.setLevel(player, DFAttachments.data(player), 10, false);
      DFAttachments.data(player).setEnergy(1000.0F);
      helper.assertTrue(AbilityManager.press(player, BomuAbilities.SHOT) == CastResult.REQUIREMENT, "no pistol");
      ItemStack pistol = new ItemStack((ItemLike)BomuItems.BOMB_PISTOL.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, pistol);
      helper.assertTrue(AbilityManager.press(player, BomuAbilities.SHOT) == CastResult.REQUIREMENT, "an empty cylinder");
      BombPistolItem.fill(pistol, player);
      int cap = BombPistolItem.capacity();
      helper.assertTrue(BombPistolItem.rounds(pistol) == cap && BombPistolItem.bombBreath(pistol), "blown full of bomb breath");
      helper.assertTrue(
         AbilityManager.press(player, BomuAbilities.SHOT) == CastResult.SUCCESS && BombPistolItem.rounds(pistol) == cap - 1, "fired, one round spent"
      );
      helper.assertTrue(DFAttachments.data(player).isOnCooldown(BomuAbilities.SHOT.id(), helper.getLevel().getGameTime()), "the shot's cooldown runs");
      List<BombBullet> shots = helper.getLevel().getEntities((EntityTypeTest)BomuEntities.BULLET.get(), player.getBoundingBox().inflate(16.0), b -> true);
      helper.assertTrue(shots.size() == 1 && !shots.get(0).isGust(), "a shot of bomb breath flies: " + shots.size());
      shots.forEach(Entity::discard);
      DFAttachments.data(player).clearCooldowns();
      BombPistolItem.setBombBreath(pistol, false);
      helper.assertTrue(AbilityManager.press(player, BomuAbilities.SHOT) == CastResult.REQUIREMENT, "plain air is no bomb");
      FakePlayer plain = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "bomu_plain"));
      plain.moveTo(player.getX(), player.getY(), player.getZ() + 1.0, 0.0F, 0.0F);
      ItemStack other = new ItemStack((ItemLike)BomuItems.BOMB_PISTOL.get());
      plain.setItemInHand(InteractionHand.MAIN_HAND, other);
      BombPistolItem.fill(other, plain);
      helper.assertTrue(BombPistolItem.rounds(other) == cap && !BombPistolItem.bombBreath(other), "anyone else blows plain air");
      helper.assertTrue(AbilityManager.press(plain, BomuAbilities.SHOT) == CastResult.NO_FRUIT, "no fruit, no bomb");
      BombPistolItem.fire(plain, InteractionHand.MAIN_HAND, other);
      List<BombBullet> gusts = helper.getLevel().getEntities((EntityTypeTest)BomuEntities.BULLET.get(), plain.getBoundingBox().inflate(16.0), b -> true);
      helper.assertTrue(gusts.size() == 1 && gusts.get(0).isGust() && BombPistolItem.rounds(other) == cap - 1, "a gust of plain air, one round spent");
      gusts.forEach(Entity::discard);
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      batch = "bomu_self",
      timeoutTicks = 120
   )
   public static void selfDestructTiersAndInvulnerability(GameTestHelper helper) {
      SelfDestructAbility sd = BomuAbilities.SELF_DESTRUCT;
      helper.assertTrue(sd.tier(0.05F) == 0 && sd.tier(0.2F) == 1 && sd.tier(0.6F) == 2 && sd.tier(1.0F) == 3, "tiers by charge");
      helper.assertTrue(
         sd.energyCost(0.05F) == 0.0F && sd.cooldownTicks(0.05F) == 0 && sd.energyCost(1.0F) > sd.energyCost(0.6F) && sd.energyCost(0.6F) > sd.energyCost(0.2F),
         "cost by tier, none for a fizzle"
      );
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      Mastery.setLevel(player, DFAttachments.data(player), 10, false);
      DFAttachments.data(player).setEnergy(1000.0F);
      BooleanValue griefing = (BooleanValue)helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
      boolean was = griefing.get();
      griefing.set(false, helper.getLevel().getServer());
      helper.onEachTick(() -> SelfDestructAbility.tick(player));
      AbilityManager.press(player, sd);
      int full = sd.maxChargeTicks();
      helper.runAfterDelay((long)(full + 2), () -> {
         helper.assertTrue(AbilityManager.release(player, sd) == CastResult.SUCCESS, "released at full charge");
         helper.assertTrue(SelfDestructAbility.vanished(player) && SelfDestructAbility.invulnerable(player), "the body is gone, untouchable");
         helper.assertTrue(blocked(player, helper.getLevel().damageSources().generic()), "nothing hurts it in the first 0.6 s");
         helper.assertTrue(DFAttachments.data(player).isOnCooldown(sd.id(), helper.getLevel().getGameTime()), "the long cooldown runs");
      });
      helper.runAfterDelay(
         (long)(full + 2 + 16),
         () -> helper.assertTrue(SelfDestructAbility.vanished(player) && !SelfDestructAbility.invulnerable(player), "still gone, no longer invulnerable")
      );
      helper.runAfterDelay((long)(full + 2 + 34), () -> {
         helper.assertTrue(!SelfDestructAbility.vanished(player), "reformed");
         griefing.set(was, helper.getLevel().getServer());
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      batch = "bomu_blast"
   )
   public static void capsGriefingAndHealingCraters(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(0, 1, 0));
      int cap = (int)BomuBomu.setting("maxExplosionsPerTickPerPlayer");
      int went = 0;

      for (int i = 0; i < cap + 3; i++) {
         if (!BombExplosionService.detonate(helper.getLevel(), player.position().add(0.0, 6.0, 0.0), spec(0.5F, 0.0F, ExplosionProfile.SMALL), player)
            .cancelled()) {
            went++;
         }
      }

      helper.assertTrue(went == cap, "capped at " + cap + " a tick: " + went);
      helper.setBlock(3, 1, 3, Blocks.DIRT);
      Vec3 at = Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 2, 3)));
      BooleanValue griefing = (BooleanValue)helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
      boolean was = griefing.get();
      griefing.set(false, helper.getLevel().getServer());
      BombExplosionService.forget(player.getUUID());
      BombExplosionService.Result off = BombExplosionService.detonate(helper.getLevel(), at, spec(2.0F, 0.0F, ExplosionProfile.SMALL), player);
      helper.assertTrue(off.blocks() == 0, "mobGriefing off: nothing breaks");
      helper.assertBlockPresent(Blocks.DIRT, 3, 1, 3);
      griefing.set(true, helper.getLevel().getServer());
      BombExplosionService.Result on = BombExplosionService.detonate(helper.getLevel(), at, spec(2.0F, 0.0F, ExplosionProfile.SMALL), player);
      griefing.set(was, helper.getLevel().getServer());
      helper.assertTrue(on.blocks() > 0, "a crater: " + on.blocks());
      helper.assertBlockPresent(Blocks.AIR, 3, 1, 3);
      boolean heals = CoreConfig.DEBRIS_MODE.get() == CoreConfig.DebrisMode.TEMPORARY;
      helper.assertTrue(
         BlockRestoreData.get(helper.getLevel()).pending(helper.absolutePos(new BlockPos(3, 1, 3))) != null == heals,
         "queued for restoring like every fruit's damage"
      );
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void pistolTradeChanceIsStablePerVillager(GameTestHelper helper) {
      double chance = BomuBomu.setting("pistolTradeChance");
      Random r = new Random(7L);
      int yes = 0;
      int n = 4000;

      for (int i = 0; i < n; i++) {
         UUID id = new UUID(r.nextLong(), r.nextLong());
         boolean a = PistolTrade.chosen(id, chance);
         helper.assertTrue(a == PistolTrade.chosen(id, chance), "stable per UUID");
         yes += a ? 1 : 0;
      }

      helper.assertTrue(Math.abs((double)yes / (double)n - chance) < 0.03, "about " + chance + ": " + (double)yes / (double)n);
      Villager villager = (Villager)helper.spawn(EntityType.VILLAGER, new BlockPos(2, 1, 2));
      MerchantOffer offer = new PistolTrade().getOffer(villager, villager.getRandom());
      boolean chosen = PistolTrade.chosen(villager.getUUID(), chance);
      helper.assertTrue(
         chosen ? offer != null && offer.getResult().is((Item)BomuItems.BOMB_PISTOL.get()) && offer.getMaxUses() == 1 : offer == null,
         "the listing follows the villager: chosen " + chosen + ", offer " + offer
      );
      helper.assertTrue(offer == null == (new PistolTrade().getOffer(villager, villager.getRandom()) == null), "asking again changes nothing");
      villager.discard();
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void keepFruitKeepsTheImmunity(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      BooleanValue rule = (BooleanValue)helper.getLevel().getGameRules().getRule(DFGameRules.KEEP_FRUIT);
      boolean was = rule.get();
      rule.set(true, helper.getLevel().getServer());
      AbilityManager.press(player, BomuAbilities.PUNCH);
      FruitRules.keepOnDeath(player);
      helper.assertTrue(
         BomuBomu.isUser(player) && BomuBomu.immune(player) && blocked(player, helper.getLevel().damageSources().explosion(null, null)),
         "power and immunity kept"
      );
      rule.set(was, helper.getLevel().getServer());
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      batch = "bomu_blast"
   )
   public static void blastsPostTheExplosionEvents(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(0, 1, 0));
      boolean[] seen = new boolean[]{false, false};
      Consumer<Start> start = e -> {
         if (e.getExplosion().getDirectSourceEntity() == player) {
            seen[0] = true;
            e.setCanceled(true);
         }
      };
      NeoForge.EVENT_BUS.addListener(start);
      BombExplosionService.Result r = BombExplosionService.detonate(
         helper.getLevel(), player.position().add(2.0, 1.0, 2.0), spec(2.0F, 5.0F, ExplosionProfile.SMALL), player
      );
      NeoForge.EVENT_BUS.unregister(start);
      helper.assertTrue(seen[0] && r.cancelled(), "ExplosionEvent.Start fired and its cancel honoured");
      done(helper, player);
   }
}
