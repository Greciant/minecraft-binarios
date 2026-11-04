package com.devilfruits.fruits.suke_suke.gametest;

import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.CastResult;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.rules.WorldFruitData;
import com.devilfruits.fruits.suke_suke.SukeAbilities;
import com.devilfruits.fruits.suke_suke.SukeSuke;
import com.devilfruits.fruits.suke_suke.clear.Clear;
import com.devilfruits.fruits.suke_suke.clear.Cloak;
import com.devilfruits.fruits.suke_suke.clear.ShipCloak;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFBlocks;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("devilfruits")
@PrefixGameTestTemplate(false)
public final class SukeGameTests {
   private static final String PLATFORM = "platform";

   private static FakePlayer player(GameTestHelper helper, BlockPos relative, String name) {
      FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
      BlockPos pos = helper.absolutePos(relative);
      player.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
      player.setDeltaMovement(Vec3.ZERO);
      return player;
   }

   private static FakePlayer user(GameTestHelper helper, BlockPos relative, int mastery) {
      FakePlayer player = player(helper, relative, "suke_test");
      WorldFruitData.get(helper.getLevel().getServer()).setFree(SukeSuke.fruit());
      FruitRules.grantPower(player, SukeSuke.fruit(), false);
      PlayerFruitData data = DFAttachments.data(player);
      data.setMastery(mastery);
      data.setEnergy((float)Mastery.maxEnergy(data));
      return player;
   }

   private static void tick(FakePlayer player) {
      AbilityManager.tickPlayer(player, DFAttachments.data(player));
   }

   private static void done(GameTestHelper helper, FakePlayer... players) {
      for (FakePlayer p : players) {
         Cloak.end(p);
         Cloak.leave(p);
         AbilityManager.cancelAll(p);
         AbilityManager.forget(p.getUUID());
         p.removeData(Clear.STATE);
         if (SukeSuke.isUser(p)) {
            FruitRules.removePower(p);
         }
      }

      Cloak.resetNearbyPlayers();
      ShipCloak.world(null);
      WorldFruitData.get(helper.getLevel().getServer()).setFree(SukeSuke.fruit());
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void invisibilityFlagAndMobs(GameTestHelper helper) {
      FakePlayer user = user(helper, new BlockPos(1, 1, 1), 1);
      FakePlayer stranger = player(helper, new BlockPos(3, 1, 3), "stranger");
      BlockPos at = helper.absolutePos(new BlockPos(3, 1, 1));
      Husk husk = (Husk)EntityType.HUSK.create(helper.getLevel());
      husk.moveTo((double)at.getX() + 0.5, (double)at.getY(), (double)at.getZ() + 0.5, 0.0F, 0.0F);
      husk.setNoAi(true);
      helper.getLevel().addFreshEntity(husk);
      helper.assertTrue(user.getVisibilityPercent(husk) > 0.5, "visible before");
      helper.assertTrue(AbilityManager.press(user, SukeAbilities.INVISIBILITY) == CastResult.TOGGLED, "invisibility toggles on");
      helper.assertTrue(Clear.invisible(user) && user.isInvisible(), "flagged and isInvisible");
      helper.assertTrue(user.isInvisibleTo(stranger), "a stranger cannot see the user");
      helper.assertFalse(user.isInvisibleTo(user), "the user sees themself");
      double v = user.getVisibilityPercent(husk);
      helper.assertTrue(Math.abs(v - SukeSuke.setting("mobVisibility")) < 1.0E-4, "mob visibility is the fruit's factor, was " + v);
      tick(user);
      helper.assertTrue(user.isInvisible(), "still invisible after a tick (vanilla rewrites the shared flag)");
      AbilityManager.press(user, SukeAbilities.INVISIBILITY);
      helper.assertFalse(Clear.invisible(user) || user.isInvisible(), "visible again when switched off");
      husk.discard();
      done(helper, user, stranger);
   }

   @GameTest(
      template = "platform"
   )
   public static void energyCalibration(GameTestHelper helper) {
      helper.assertTrue(Cloak.soloSeconds(10) == 300.0 && Cloak.soloSeconds(1) == 60.0, "60 s at mastery 1, 300 s at 10");

      for (int m = 1; m <= 10; m++) {
         int max = Mastery.maxEnergy(SukeSuke.fruit(), m);

         for (int n = 1; n <= 4; n++) {
            double seconds = (double)max / ((double)Cloak.drainPerTick(max, m, n) * 20.0);
            double expected = Cloak.soloSeconds(m) / (double)n;
            helper.assertTrue(Math.abs(seconds - expected) < 0.01, "mastery " + m + ", " + n + " people: " + seconds + " s, expected " + expected);
         }
      }

      helper.assertTrue(
         Math.abs((double)Mastery.maxEnergy(SukeSuke.fruit(), 10) / ((double)Cloak.drainPerTick(Mastery.maxEnergy(SukeSuke.fruit(), 10), 10, 2) * 20.0) - 150.0)
            < 0.01,
         "two people at mastery 10: 150 s"
      );
      FakePlayer user = user(helper, new BlockPos(1, 1, 1), 10);
      PlayerFruitData data = DFAttachments.data(user);
      float full = data.energy();
      AbilityManager.press(user, SukeAbilities.INVISIBILITY);

      for (int i = 0; i < 100; i++) {
         tick(user);
      }

      float spent = full - data.energy();
      float expected = full / 300.0F * 5.0F;
      helper.assertTrue(Math.abs(spent - expected) < 0.05F, "5 s at mastery 10 spends a 60th of the bar: " + spent + " vs " + expected);
      data.setEnergy(Cloak.drainPerTick(Mastery.maxEnergy(data), 10, 1) * 3.0F);

      for (int i = 0; i < 6; i++) {
         tick(user);
      }

      helper.assertFalse(data.isToggleActive(SukeAbilities.INVISIBILITY.id()) || Clear.invisible(user), "out of energy ends it");
      done(helper, user);
   }

   @GameTest(
      template = "platform",
      batch = "suke_group"
   )
   public static void groupMembership(GameTestHelper helper) {
      FakePlayer user = user(helper, new BlockPos(2, 1, 2), 10);
      FakePlayer near1 = player(helper, new BlockPos(3, 1, 2), "near1");
      FakePlayer near2 = player(helper, new BlockPos(2, 1, 4), "near2");
      FakePlayer far = player(helper, new BlockPos(2, 1, 2), "far");
      far.moveTo(user.getX() + 8.0, user.getY(), user.getZ());
      Cloak.nearbyPlayers(owner -> List.of(near1, near2, far, owner));
      PlayerFruitData data = DFAttachments.data(user);
      helper.assertTrue(AbilityManager.press(user, SukeAbilities.GROUP_INVISIBILITY) == CastResult.TOGGLED, "shared invisibility on");
      helper.assertTrue(Cloak.headcount(user) == 3, "the user and the two within reach, was " + Cloak.headcount(user));
      helper.assertTrue(Clear.invisible(near1) && Clear.invisible(near2) && !Clear.invisible(far), "the members are clear, the far one is not");
      helper.assertFalse(near1.isInvisibleTo(near2), "members see each other");
      helper.assertTrue(near1.isInvisibleTo(far), "a stranger does not see the members");
      float before = data.energy();
      tick(user);
      float drain = before - data.energy();
      float solo = Cloak.drainPerTick(Mastery.maxEnergy(data), 10, 1);
      helper.assertTrue(Math.abs(drain - solo * 3.0F) < 1.0E-4F, "three people drain three times: " + drain + " vs " + solo * 3.0F);
      near2.moveTo(user.getX() + SukeSuke.setting("groupRadius") + 0.2, user.getY(), user.getZ());
      tick(user);
      helper.assertTrue(Cloak.headcount(user) == 2 && !Clear.invisible(near2), "out of the radius the member shows at once");
      near2.moveTo(user.getX() + 1.0, user.getY(), user.getZ());
      tick(user);
      helper.assertTrue(Cloak.headcount(user) == 2 && !Clear.invisible(near2), "coming back does not rejoin");
      AbilityManager.press(user, SukeAbilities.INVISIBILITY);
      helper.assertTrue(Clear.invisible(user) && !Clear.invisible(near1) && Cloak.headcount(user) == 1, "the handover keeps only the user");
      helper.assertFalse(data.isOnCooldown(SukeAbilities.GROUP_INVISIBILITY.id(), helper.getLevel().getGameTime()), "a handover starts no cooldown");
      near2.moveTo(user.getX() + 1.0, user.getY(), user.getZ() + 1.0);
      AbilityManager.press(user, SukeAbilities.GROUP_INVISIBILITY);
      helper.assertTrue(Cloak.headcount(user) == 3, "the group again");
      AbilityManager.press(user, SukeAbilities.GROUP_INVISIBILITY);
      helper.assertFalse(Clear.invisible(user) || Clear.invisible(near1) || Clear.invisible(near2), "the owner's end ends it for all");
      done(helper, user, near1, near2, far);
   }

   @GameTest(
      template = "platform",
      batch = "suke_group"
   )
   public static void groupNobodyNearAndOwnerEnds(GameTestHelper helper) {
      FakePlayer user = user(helper, new BlockPos(2, 1, 2), 10);
      Cloak.nearbyPlayers(owner -> List.of(owner));
      PlayerFruitData data = DFAttachments.data(user);
      float full = data.energy();
      helper.assertTrue(AbilityManager.press(user, SukeAbilities.GROUP_INVISIBILITY) == CastResult.REQUIREMENT, "nobody in reach: refused");
      helper.assertTrue(data.energy() == full && !Clear.invisible(user), "and nothing spent");
      FakePlayer member = player(helper, new BlockPos(3, 1, 3), "member");
      Cloak.nearbyPlayers(owner -> List.of(member, owner));
      AbilityManager.press(user, SukeAbilities.GROUP_INVISIBILITY);
      helper.assertTrue(Clear.invisible(member), "the member joined");
      Cloak.end(user);
      helper.assertFalse(Clear.invisible(member) || Clear.invisible(user), "the owner's end shows everyone");
      AbilityManager.press(user, SukeAbilities.GROUP_INVISIBILITY);
      AbilityManager.press(user, SukeAbilities.GROUP_INVISIBILITY);
      data.clearCooldowns();
      AbilityManager.press(user, SukeAbilities.GROUP_INVISIBILITY);
      Cloak.leave(member);
      helper.assertTrue(!Clear.invisible(member) && Clear.invisible(user) && Cloak.headcount(user) == 1, "a member leaving drops only itself");
      done(helper, user, member);
   }

   @GameTest(
      template = "platform"
   )
   public static void seastoneEndsIt(GameTestHelper helper) {
      FakePlayer user = user(helper, new BlockPos(2, 1, 2), 5);
      AbilityManager.press(user, SukeAbilities.INVISIBILITY);
      helper.assertTrue(Clear.invisible(user), "on");
      helper.setBlock(new BlockPos(2, 0, 2), (Block)DFBlocks.SEASTONE_BLOCK.get());
      tick(user);
      helper.assertFalse(Clear.invisible(user) || user.isInvisible(), "Seastone underfoot ends the invisibility at once");
      helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
      done(helper, user);
   }

   @GameTest(
      template = "platform"
   )
   public static void hiddenWeaponStacks(GameTestHelper helper) {
      FakePlayer user = user(helper, new BlockPos(2, 1, 2), 5);
      AbilityManager.press(user, SukeAbilities.HIDDEN_WEAPON);
      helper.assertTrue(Clear.hiddenItems(user) && !Clear.invisible(user), "the items hide while visible");
      AbilityManager.press(user, SukeAbilities.INVISIBILITY);
      helper.assertTrue(Clear.hiddenItems(user) && Clear.invisible(user), "both at once");
      AbilityManager.press(user, SukeAbilities.INVISIBILITY);
      helper.assertTrue(Clear.hiddenItems(user) && !Clear.invisible(user), "the invisibility's end keeps the hidden items");
      AbilityManager.press(user, SukeAbilities.HIDDEN_WEAPON);
      helper.assertFalse(Clear.hiddenItems(user), "and off");
      done(helper, user);
   }

   @GameTest(
      template = "platform",
      batch = "suke_ship"
   )
   public static void shipGatedWithoutSable(GameTestHelper helper) {
      FakePlayer user = user(helper, new BlockPos(2, 1, 2), 10);
      helper.assertFalse(ShipCloak.world().available(), "no Sable in the test server");
      helper.assertTrue(AbilityManager.press(user, SukeAbilities.SHIP_INVISIBILITY) == CastResult.REQUIREMENT, "refused without it");
      done(helper, user);
   }

   @GameTest(
      template = "platform",
      batch = "suke_ship",
      timeoutTicks = 200
   )
   public static void shipRidersAndExpiry(GameTestHelper helper) {
      helper.assertTrue(ShipCloak.seconds(8) == 30.0 && ShipCloak.seconds(9) == 45.0 && ShipCloak.seconds(10) == 60.0, "30 s at 8, 45 at 9, 60 at 10");
      FakePlayer user = user(helper, new BlockPos(2, 1, 2), 10);
      FakePlayer rider = player(helper, new BlockPos(3, 1, 3), "rider");
      SukeGameTests.FakeShips fake = new SukeGameTests.FakeShips(new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(4.0));
      ShipCloak.world(fake);
      helper.assertTrue(AbilityManager.press(user, SukeAbilities.SHIP_INVISIBILITY) == CastResult.REQUIREMENT, "not standing on nor aiming at a ship");
      fake.aboard.add(user);
      PlayerFruitData data = DFAttachments.data(user);
      float before = data.energy();
      helper.assertTrue(
         AbilityManager.press(user, SukeAbilities.SHIP_INVISIBILITY) == CastResult.TOGGLED && ShipCloak.hidden(fake.id), "pressed: the ship hides"
      );
      helper.assertTrue(
         data.energy() < before && !data.isOnCooldown(SukeAbilities.SHIP_INVISIBILITY.id(), helper.getLevel().getGameTime()),
         "the cost paid, no cooldown while it is hidden"
      );
      AbilityManager.press(user, SukeAbilities.SHIP_INVISIBILITY);
      helper.assertTrue(!ShipCloak.hidden(fake.id) && !Clear.invisible(user), "pressed again: shown at once");
      helper.assertTrue(data.isOnCooldown(SukeAbilities.SHIP_INVISIBILITY.id(), helper.getLevel().getGameTime()), "the cooldown starts when it shows");
      data.clearCooldowns();
      fake.aboard.add(rider);
      ShipCloak.start(user, fake.ship, 40);
      helper.assertTrue(ShipCloak.hidden(fake.id) && ShipCloak.endOf(fake.id) == helper.getLevel().getGameTime() + 40L, "hidden for its time");
      helper.assertTrue(Clear.invisible(user) && Clear.invisible(rider) && Clear.of(rider).has(4), "everyone aboard is clear");
      helper.assertFalse(rider.isInvisibleTo(user), "the riders and the caster see each other");
      fake.aboard.remove(rider);
      ShipCloak.tick(helper.getLevel().getServer());
      helper.assertFalse(Clear.invisible(rider), "stepping off shows the rider");
      fake.aboard.add(rider);
      ShipCloak.tick(helper.getLevel().getServer());
      helper.assertTrue(Clear.invisible(rider), "stepping on hides it again");
      helper.runAfterDelay(45L, () -> {
         ShipCloak.tick(helper.getLevel().getServer());
         helper.assertFalse(ShipCloak.hidden(fake.id), "expired");
         helper.assertFalse(Clear.invisible(user) || Clear.invisible(rider), "everyone aboard shows again");
         done(helper, user, rider);
      });
   }

   private static final class FakeShips implements ShipCloak.World {
      final UUID id = UUID.randomUUID();
      final List<Entity> aboard = new ArrayList<>();
      final AABB bounds;
      boolean valid = true;
      final ShipCloak.Ship ship = new ShipCloak.Ship() {
         @Override
         public UUID id() {
            return FakeShips.this.id;
         }

         @Override
         public boolean valid() {
            return FakeShips.this.valid;
         }

         @Override
         public AABB worldBounds() {
            return FakeShips.this.bounds;
         }
      };

      FakeShips(AABB bounds) {
         this.bounds = bounds;
      }

      @Override
      public boolean available() {
         return true;
      }

      @Nullable
      @Override
      public ShipCloak.Ship under(Entity entity) {
         return this.aboard.contains(entity) ? this.ship : null;
      }

      @Nullable
      @Override
      public ShipCloak.Ship aimed(ServerPlayer player, double reach) {
         return null;
      }

      @Override
      public boolean on(Entity entity, ShipCloak.Ship s) {
         return this.aboard.contains(entity);
      }

      @Override
      public List<Entity> nearby(ServerLevel level, ShipCloak.Ship s) {
         return List.copyOf(this.aboard);
      }
   }
}
