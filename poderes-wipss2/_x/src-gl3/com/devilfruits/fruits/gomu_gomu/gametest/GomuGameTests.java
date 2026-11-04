package com.devilfruits.fruits.gomu_gomu.gametest;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormAbility;
import com.devilfruits.core.gear.GearDefinition;
import com.devilfruits.core.gear.GearRegistry;
import com.devilfruits.core.gear.GearState;
import com.devilfruits.core.gear.GearSwitch;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.WorldFruitData;
import com.devilfruits.fruits.gomu_gomu.GomuAbilities;
import com.devilfruits.fruits.gomu_gomu.GomuGomu;
import com.devilfruits.fruits.gomu_gomu.ability.AxeAbility;
import com.devilfruits.fruits.gomu_gomu.ability.GatlingAbility;
import com.devilfruits.fruits.gomu_gomu.ability.StrikeAbility;
import com.devilfruits.fruits.gomu_gomu.rubber.BulletCatch;
import com.devilfruits.fruits.gomu_gomu.strike.Strike;
import com.devilfruits.fruits.gomu_gomu.strike.StrikeKind;
import com.devilfruits.fruits.gomu_gomu.strike.StrikePath;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.mobs.pirate.entity.MusketBall;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFDamageTypes;
import com.devilfruits.registry.DFItems;
import com.devilfruits.registry.DFMobEffects;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("devilfruits")
@PrefixGameTestTemplate(false)
public final class GomuGameTests {
   private static final String PLATFORM = "platform";

   private static FakePlayer user(GameTestHelper helper, BlockPos relative) {
      FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "gomu_test"));
      BlockPos pos = helper.absolutePos(relative);
      player.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
      player.setDeltaMovement(Vec3.ZERO);
      WorldFruitData.get(helper.getLevel().getServer()).setFree(GomuGomu.fruit());
      FruitRules.grantPower(player, GomuGomu.fruit(), false);
      DFAttachments.data(player).setEnergy(1000.0F);
      return player;
   }

   private static void done(GameTestHelper helper, FakePlayer player) {
      BulletCatch.release(player);
      player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      FruitRules.removePower(player);
      WorldFruitData.get(helper.getLevel().getServer()).setFree(GomuGomu.fruit());
      helper.succeed();
   }

   private static boolean blocked(FakePlayer player, DamageSource source) {
      return ((LivingIncomingDamageEvent)NeoForge.EVENT_BUS.post(new LivingIncomingDamageEvent(player, new DamageContainer(source, 10.0F)))).isCanceled();
   }

   private static FakePlayer attacker(GameTestHelper helper, ItemStack weapon) {
      FakePlayer other = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "gomu_attacker"));
      other.setItemInHand(InteractionHand.MAIN_HAND, weapon);
      return other;
   }

   private static <T extends Mob> T mob(GameTestHelper helper, EntityType<T> type, ItemStack held) {
      T mob = (T)helper.spawn(type, new BlockPos(4, 1, 4));
      mob.setItemInHand(InteractionHand.MAIN_HAND, held);
      return mob;
   }

   @GameTest(
      template = "platform"
   )
   public static void bluntBlowsDoNothing(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));
      Level level = helper.getLevel();
      DamageSources sources = level.damageSources();

      for (ItemStack weapon : List.of(
         ItemStack.EMPTY, new ItemStack(Items.MACE), new ItemStack(Items.STICK), new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.COBBLESTONE)
      )) {
         helper.assertTrue(blocked(player, sources.playerAttack(attacker(helper, weapon))), "a blow with '" + weapon + "' does nothing");
      }

      for (ItemStack weapon : List.of(
         new ItemStack(Items.IRON_SWORD), new ItemStack(Items.DIAMOND_AXE), new ItemStack(Items.TRIDENT), new ItemStack(Items.IRON_PICKAXE)
      )) {
         helper.assertFalse(blocked(player, sources.playerAttack(attacker(helper, weapon))), weapon + " cuts rubber");
      }

      FakePlayer coated = attacker(helper, ItemStack.EMPTY);
      coated.addEffect(new MobEffectInstance(DFMobEffects.ARMAMENT_COATING, 200));
      helper.assertFalse(blocked(player, sources.playerAttack(coated)), "an Armament-coated fist hurts a rubber body");
      Zombie zombie = mob(helper, EntityType.ZOMBIE, ItemStack.EMPTY);
      helper.assertTrue(blocked(player, sources.mobAttack(zombie)), "a bare-handed zombie's blow does nothing");
      zombie.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
      helper.assertFalse(blocked(player, sources.mobAttack(zombie)), "a zombie with a sword cuts");
      IronGolem golem = mob(helper, EntityType.IRON_GOLEM, ItemStack.EMPTY);
      helper.assertTrue(blocked(player, sources.mobAttack(golem)), "an iron golem's blow does nothing");
      Spider spider = mob(helper, EntityType.SPIDER, ItemStack.EMPTY);
      helper.assertFalse(blocked(player, sources.mobAttack(spider)), "a spider's bite connects");
      Vindicator vindicator = mob(helper, EntityType.VINDICATOR, new ItemStack(Items.IRON_AXE));
      helper.assertFalse(blocked(player, sources.mobAttack(vindicator)), "a vindicator's axe cuts");

      for (DamageSource s : List.of(sources.fall(), sources.flyIntoWall(), sources.fallingBlock(null), sources.anvil(null), sources.cramming())) {
         helper.assertTrue(blocked(player, s), s.getMsgId() + " does nothing");
      }

      for (DamageSource s : List.of(sources.generic(), sources.stalagmite(), sources.explosion(null, null), sources.inFire(), sources.magic())) {
         helper.assertFalse(blocked(player, s), s.getMsgId() + " still hurts");
      }

      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)DFItems.SEASTONE_INGOT.get()));
      helper.assertFalse(blocked(player, sources.playerAttack(attacker(helper, ItemStack.EMPTY))), "seastone takes the rubber away");
      helper.assertFalse(blocked(player, sources.fall()), "seastone takes the fall immunity away");
      List.of(zombie, golem, spider, vindicator).forEach(Entity::discard);
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void fallsDoNothing(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));

      for (float height : new float[]{4.0F, 40.0F, 400.0F}) {
         LivingFallEvent event = (LivingFallEvent)NeoForge.EVENT_BUS.post(new LivingFallEvent(player, height, 1.0F));
         helper.assertTrue(event.isCanceled() || event.getDamageMultiplier() == 0.0F, "a " + height + "-block fall does nothing");
      }

      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void lightningDoesNothing(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));
      Level level = helper.getLevel();
      FakePlayer dragon = attacker(helper, ItemStack.EMPTY);
      helper.assertTrue(blocked(player, level.damageSources().lightningBolt()), "a lightning bolt does nothing");
      helper.assertTrue(blocked(player, DFDamageTypes.source(level, UoUoSeiryu.THUNDER_DAMAGE, dragon)), "the Seiryu's thunder does nothing");
      LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
      helper.assertTrue(
         ((EntityStruckByLightningEvent)NeoForge.EVENT_BUS.post(new EntityStruckByLightningEvent(player, bolt))).isCanceled(),
         "the strike is cancelled (no fire)"
      );
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      batch = "gomu_bullet"
   )
   public static void bulletIsCaughtAndReturned(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      Level level = helper.getLevel();
      Zombie shooter = (Zombie)helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 6));
      shooter.setNoAi(true);
      MusketBall ball = new MusketBall(level, shooter);
      Vec3 incoming = new Vec3(0.0, 0.0, -1.0);
      ball.setPos(player.getX(), player.getY() + 1.2, player.getZ() + 1.0);
      ball.shoot(incoming.x, incoming.y, incoming.z, 3.0F, 0.0F);
      level.addFreshEntity(ball);
      helper.assertTrue(BulletCatch.catches(player, ball), "a musket ball is a bullet the rubber catches");
      ProjectileImpactEvent impact = (ProjectileImpactEvent)NeoForge.EVENT_BUS
         .post(new ProjectileImpactEvent(ball, new EntityHitResult(player, player.position().add(0.0, 1.2, 0.3))));
      helper.assertTrue(impact.isCanceled(), "the impact is cancelled: no damage, no knockback");
      helper.assertTrue(ball.isRemoved(), "the ball leaves the world while it is held");
      helper.assertTrue(BulletCatch.holding(player) == 1, "the ball is held in a dent");
      BulletCatch.ejectNow(player);
      helper.assertTrue(BulletCatch.holding(player) == 0, "the ball is shot back once");
      AABB around = player.getBoundingBox().inflate(3.0);
      List<MusketBall> back = level.getEntitiesOfClass(MusketBall.class, around, b -> b.getPersistentData().getBoolean("devilfruits_gomu_returned"));
      helper.assertTrue(back.size() == 1, "one ball comes back, found " + back.size());
      MusketBall returned = back.get(0);
      helper.assertTrue(returned.getOwner() == player, "the returned ball is the user's shot");
      helper.assertTrue(returned.getDeltaMovement().normalize().dot(incoming) < -0.95, "it flies back the way it came");
      helper.assertTrue(
         Math.abs(returned.getDeltaMovement().length() - 3.0 * GomuGomu.setting("returnedBulletSpeed")) < 0.05, "at the configured share of its speed"
      );
      helper.assertFalse(BulletCatch.catches(player, returned), "the user's own returned ball is never caught again");
      returned.discard();
      shooter.discard();
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      batch = "gomu_bullet"
   )
   public static void bulletHoldTime(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      Level level = helper.getLevel();
      MusketBall ball = new MusketBall(level, player);
      ball.setOwner(null);
      ball.setPos(player.getX(), player.getY() + 1.0, player.getZ() + 1.0);
      ball.shoot(0.0, 0.0, -1.0, 3.0F, 0.0F);
      long eject = BulletCatch.absorb(player, ball, null);
      helper.assertTrue(eject == level.getGameTime() + (long)GomuGomu.setting("bulletHoldTicks"), "held for bulletHoldTicks");
      done(helper, player);
   }

   private static AbilityContext context(FakePlayer player, StrikeAbility ability) {
      FormAbility entry = GomuGomu.fruit().baseForm().find(ability.id());
      return new AbilityContext(entry, player, DFAttachments.data(player), 0.0F);
   }

   private static Strike run(FakePlayer player, StrikeAbility ability) {
      Strike strike = ability.prepare(context(player, ability), 1);
      int t = 1;

      while (t < 60 && !strike.tick(t)) {
         t++;
      }

      return strike;
   }

   @GameTest(
      template = "platform"
   )
   public static void strikeRatiosAndReach(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));

      for (int mastery : new int[]{1, 5, 10}) {
         DFAttachments.data(player).setMastery(mastery);
         float pistol = GomuAbilities.PISTOL.damage(context(player, (StrikeAbility)GomuAbilities.PISTOL));
         float bazooka = GomuAbilities.BAZOOKA.damage(context(player, (StrikeAbility)GomuAbilities.BAZOOKA));
         float rifle = GomuAbilities.RIFLE.damage(context(player, (StrikeAbility)GomuAbilities.RIFLE));
         helper.assertTrue(
            (double)Math.abs(bazooka - 3.0F * pistol) < 0.001 && (double)Math.abs(rifle - 2.0F * bazooka) < 0.001,
            "ratios at mastery " + mastery + ": " + pistol + " / " + bazooka + " / " + rifle
         );
      }

      helper.assertTrue(
         GomuAbilities.PISTOL.config().range() == 10.0 && GomuAbilities.BAZOOKA.config().range() == 8.0 && GomuAbilities.RIFLE.config().range() == 12.0,
         "reach 10 / 8 / 12"
      );
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      batch = "gomu_strike"
   )
   public static void pistolStopsAtTheFirstBody(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 0));
      player.moveTo(player.getX(), player.getY(), player.getZ(), 0.0F, 0.0F);
      Zombie first = (Zombie)helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
      Zombie second = (Zombie)helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 5));
      List.of(first, second).forEach(z -> z.setNoAi(true));
      Strike strike = run(player, GomuAbilities.PISTOL);
      helper.assertTrue(first.getHealth() < first.getMaxHealth(), "the first zombie is struck");
      helper.assertTrue(second.getHealth() == second.getMaxHealth(), "the one behind is not");
      helper.assertTrue(strike.end(1) < 4.0, "the shot ends at the first body, not " + strike.end(1));
      List.of(first, second).forEach(Entity::discard);
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      batch = "gomu_strike"
   )
   public static void wallsStopRiflesPierce(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 0));
      player.moveTo(player.getX(), player.getY(), player.getZ(), 0.0F, 0.0F);
      helper.setBlock(new BlockPos(2, 2, 4), Blocks.STONE);
      Strike pistol = run(player, GomuAbilities.PISTOL);
      helper.assertTrue(pistol.end(1) > 2.0 && pistol.end(1) < 4.2, "the wall stops the Pistol, not at " + pistol.end(1));
      helper.setBlock(new BlockPos(2, 2, 4), Blocks.AIR);
      DFAttachments.data(player).setMastery(10);
      List<Zombie> row = List.of(
         (Zombie)helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 1)),
         (Zombie)helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2)),
         (Zombie)helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 3))
      );
      row.forEach(z -> {
         z.setNoAi(true);
         z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400.0);
         z.setHealth(400.0F);
      });
      Strike rifle = run(player, GomuAbilities.RIFLE);
      helper.assertTrue(
         row.stream().allMatch(z -> z.getHealth() < 400.0F),
         "the Rifle bores through all three: "
            + row.stream().map(z -> z.getHealth() + "@" + String.format("%.2f", z.distanceTo(player))).toList()
            + ", ended at "
            + rifle.end(1)
      );
      helper.assertTrue(rifle.end(1) > 3.4, "and flies on past them, ended at " + rifle.end(1));
      row.forEach(Entity::discard);
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      batch = "gomu_strike"
   )
   public static void bazookaBreaksWoodRifleStone(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 0));
      player.moveTo(player.getX(), player.getY(), player.getZ(), 0.0F, 0.0F);
      BlockPos wall = new BlockPos(2, 2, 3);
      helper.setBlock(wall, Blocks.OAK_PLANKS);
      Strike bazooka = run(player, GomuAbilities.BAZOOKA);
      helper.assertTrue(
         helper.getBlockState(wall).isAir() && bazooka.broken() > 0 && bazooka.end(1) > 3.5,
         "the Bazooka smashes the planks and flies on: " + helper.getBlockState(wall) + ", ended at " + bazooka.end(1)
      );
      helper.setBlock(wall, Blocks.STONE);
      Strike stopped = run(player, GomuAbilities.BAZOOKA);
      helper.assertTrue(helper.getBlockState(wall).is(Blocks.STONE) && stopped.end(1) < 3.5, "stone stops the Bazooka at " + stopped.end(1));
      run(player, GomuAbilities.RIFLE);
      helper.assertTrue(helper.getBlockState(wall).isAir(), "the Rifle bores through stone");
      helper.setBlock(wall, Blocks.OBSIDIAN);
      Strike blocked = run(player, GomuAbilities.RIFLE);
      helper.assertTrue(helper.getBlockState(wall).is(Blocks.OBSIDIAN) && blocked.end(1) < 3.5, "obsidian stops the Rifle at " + blocked.end(1));
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      batch = "gomu_rocket",
      timeoutTicks = 80
   )
   public static void rocketMissHalvesTheCooldown(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));
      player.moveTo(player.getX(), player.getY() + 6.0, player.getZ(), 0.0F, -80.0F);
      FormAbility entry = GomuGomu.fruit().baseForm().find(GomuAbilities.ROCKET.id());
      PlayerFruitData data = DFAttachments.data(player);
      data.setMastery(5);
      int full = AbilityManager.cooldown(entry, data, 1.0F);
      data.startCooldown(GomuAbilities.ROCKET.id(), helper.getLevel().getGameTime(), full);
      GomuAbilities.ROCKET.activate(new AbilityContext(entry, player, data, 0.0F));
      int wait = (int)Math.ceil(StrikePath.endTick(StrikeKind.ROCKET, GomuAbilities.ROCKET.config().range())) + 4;
      helper.runAfterDelay((long)wait, () -> {
         int left = data.cooldownTicksLeft(GomuAbilities.ROCKET.id(), helper.getLevel().getGameTime());
         int expected = Math.round((float)full * 0.5F);
         helper.assertTrue(left <= expected && left >= expected - 8, "a miss leaves half of " + full + " (" + expected + "), left " + left);
         done(helper, player);
      });
   }

   private static AbilityContext context(FakePlayer player, Ability ability) {
      return new AbilityContext(GomuGomu.fruit().baseForm().find(ability.id()), player, DFAttachments.data(player), 0.0F);
   }

   @GameTest(
      template = "platform",
      batch = "gomu_axe"
   )
   public static void axeFalloffAndHardness(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(0, 1, 0));
      AbilityContext ctx = context(player, GomuAbilities.AXE);
      Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2)));
      List<Zombie> zombies = List.of(
         (Zombie)helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 3)), (Zombie)helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 7))
      );
      zombies.forEach(z -> {
         z.setNoAi(true);
         z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400.0);
         z.setHealth(400.0F);
      });
      helper.setBlock(new BlockPos(2, 0, 2), Blocks.OBSIDIAN);
      helper.setBlock(new BlockPos(3, 0, 2), Blocks.STONE);
      AxeAbility.impact(ctx, at);
      float near = 400.0F - zombies.get(0).getHealth();
      float far = 400.0F - zombies.get(1).getHealth();
      helper.assertTrue(near > far && far > 0.0F, "falloff: near " + near + ", far " + far);
      helper.assertTrue(helper.getBlockState(new BlockPos(2, 0, 2)).is(Blocks.OBSIDIAN), "obsidian is harder than the cap");
      helper.assertTrue(helper.getBlockState(new BlockPos(3, 0, 2)).isAir(), "the crater breaks stone");
      zombies.forEach(Entity::discard);
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      batch = "gomu_axe"
   )
   public static void axeFallsStraightInFront(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(3, 1, 1));
      AbilityContext ctx = context(player, GomuAbilities.AXE);
      Vec3 at = AxeAbility.target(ctx);
      Vec3 ahead = player.position().add(ctx.horizontalLook().scale(ctx.extra("axeDistance")));
      helper.assertTrue(Math.abs(at.x - ahead.x) < 0.001 && Math.abs(at.z - ahead.z) < 0.001, "the foot comes down straight in front: " + at);

      for (int z = 0; z <= 5; z++) {
         helper.setBlock(new BlockPos(3, 0, z), Blocks.STONE);
      }

      AxeAbility.impact(ctx, at);
      helper.assertTrue(helper.getBlockState(new BlockPos(3, 0, 1)).is(Blocks.STONE), "the ground under the user stays");
      helper.assertTrue(helper.getBlockState(new BlockPos(3, 0, 4)).isAir(), "the crater is in front");
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      batch = "gomu_strike"
   )
   public static void fistBackSnapsOnTheReturn(GameTestHelper helper) {
      for (StrikeKind k : StrikeKind.values()) {
         for (double end : new double[]{1.5, 3.0, 8.0, 12.0, 20.0}) {
            double back = StrikePath.returnedTick(k, end);
            double length = StrikePath.distance(k, back, end);
            helper.assertTrue(Math.abs(length - 0.7) < 0.01, k + " " + end + ": the arm is " + length + " at the return");
            long start = Math.round(back - 2.0);
            helper.assertTrue(Math.abs((double)(start + 2L) - back) <= 0.5, k + " " + end + ": the snap is off the return");
            helper.assertTrue((double)start >= Math.ceil(StrikePath.endTick(k, end)), k + " " + end + ": the sound would be due before the shot ends");
         }
      }

      helper.succeed();
   }

   @GameTest(
      template = "platform",
      batch = "gomu_gatling"
   )
   public static void gatlingBarrageAndFinisher(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 0));
      player.moveTo(player.getX(), player.getY(), player.getZ(), 0.0F, 0.0F);
      Zombie target = (Zombie)helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
      target.setNoAi(true);
      target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400.0);
      target.setHealth(400.0F);
      AbilityContext ctx = context(player, GomuAbilities.GATLING);
      GomuAbilities.GATLING.onChannelStart(ctx);
      int hits = 0;
      float last = target.getHealth();

      for (int t = 1; t <= 20; t++) {
         GomuAbilities.GATLING.channelTick(ctx, t);
         if (target.getHealth() < last) {
            hits++;
            last = target.getHealth();
         }
      }

      helper.assertTrue(hits >= 15, "the barrage keeps landing (hurt-time reset): " + hits + " of 18 ticks");
      Vec3 before = target.getDeltaMovement();
      GomuAbilities.GATLING.onChannelEnd(ctx, 20, false);
      helper.assertTrue(target.getDeltaMovement().subtract(before).length() > 0.5, "the finisher knocks the target away");
      helper.assertFalse(GatlingAbility.channeling(player.getUUID()), "the barrage is over");
      target.discard();
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void lockedGearsAreRefused(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));
      List<GearDefinition> gears = GearRegistry.of(GomuGomu.fruit());
      helper.assertTrue(gears.size() == 5 && gears.get(0).name().equals("gear_1") && gears.get(0).available(), "Gear 1 first and available");
      helper.assertTrue(gears.stream().skip(1L).noneMatch(GearDefinition::available), "Gears 2-5 are placeholders");
      helper.assertTrue(GearRegistry.find(GomuGomu.fruit(), "gear_4").subGears().size() == 2, "Gear 4 has Bounce Man and Snake Man");
      GearState state = GearState.of(player);
      helper.assertTrue(state != null && state.gear().name().equals("gear_1"), "the user starts in Gear 1");
      helper.assertTrue(GearSwitch.request(player, "gear_1", "") == GearSwitch.Result.ALREADY, "already in Gear 1");

      for (String[] pick : new String[][]{{"gear_2", ""}, {"gear_3", ""}, {"gear_4", ""}, {"gear_4", "bounce_man"}, {"gear_4", "snake_man"}, {"gear_5", ""}}) {
         helper.assertTrue(GearSwitch.request(player, pick[0], pick[1]) == GearSwitch.Result.UNAVAILABLE, pick[0] + "/" + pick[1] + " is refused");
      }

      helper.assertTrue(GearSwitch.request(player, "gear_9", "") == GearSwitch.Result.UNKNOWN, "an unknown gear is refused");
      helper.assertTrue(GearState.of(player).gear().name().equals("gear_1"), "still in Gear 1");
      done(helper, player);
   }
}
