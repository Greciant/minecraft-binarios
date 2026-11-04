package com.devilfruits.fruits.horo_horo.gametest;

import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.CastResult;
import com.devilfruits.core.combat.Stun;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.WorldFruitData;
import com.devilfruits.fruits.horo_horo.HoroAbilities;
import com.devilfruits.fruits.horo_horo.HoroHoro;
import com.devilfruits.fruits.horo_horo.ability.MiniHollowAbility;
import com.devilfruits.fruits.horo_horo.ability.SpecialHollowAbility;
import com.devilfruits.fruits.horo_horo.entity.GhostBody;
import com.devilfruits.fruits.horo_horo.ghost.GhostForm;
import com.devilfruits.fruits.horo_horo.negative.Negative;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFBlocks;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("devilfruits")
@PrefixGameTestTemplate(false)
public final class HoroGameTests {
   private static final String PLATFORM = "platform";

   private static FakePlayer user(GameTestHelper helper, BlockPos relative) {
      FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "horo_test"));
      BlockPos pos = helper.absolutePos(relative);
      player.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
      player.setDeltaMovement(Vec3.ZERO);
      player.setHealth(player.getMaxHealth());
      WorldFruitData.get(helper.getLevel().getServer()).setFree(HoroHoro.fruit());
      FruitRules.grantPower(player, HoroHoro.fruit(), false);
      DFAttachments.data(player).setEnergy(1000.0F);
      DFAttachments.data(player).setMastery(10);
      return player;
   }

   private static void done(GameTestHelper helper, FakePlayer player) {
      if (GhostForm.active(player)) {
         GhostForm.finish(player, true);
      }

      AbilityManager.cancelAll(player);
      AbilityManager.forget(player.getUUID());
      MiniHollowAbility.forget(player.getUUID());
      player.getInventory().clearContent();
      player.removeAllEffects();
      if (HoroHoro.isUser(player)) {
         FruitRules.removePower(player);
      }

      player.getAbilities().flying = false;
      WorldFruitData.get(helper.getLevel().getServer()).setFree(HoroHoro.fruit());
      helper.succeed();
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

   private static void lookAt(FakePlayer player, Entity target) {
      player.lookAt(Anchor.EYES, target.getBoundingBox().getCenter());
   }

   private static Husk huskAhead(GameTestHelper helper, float health) {
      BlockPos at = helper.absolutePos(new BlockPos(2, 2, 3));
      return huskAt(helper, new Vec3((double)at.getX() + 0.5, (double)at.getY(), (double)at.getZ() + 0.5), health);
   }

   @GameTest(
      template = "platform"
   )
   public static void negativeRestrictions(GameTestHelper helper) {
      FakePlayer victim = user(helper, new BlockPos(2, 1, 2));
      double speed = victim.getAttributeValue(Attributes.MOVEMENT_SPEED);
      Negative.apply(victim, 80, null);
      helper.assertTrue(Negative.in(victim), "in Negative");
      helper.assertTrue(Negative.view(victim).on(helper.getLevel().getGameTime()), "the clients see it (the look)");
      helper.assertTrue(victim.getAttributeValue(Attributes.MOVEMENT_SPEED) < speed * 0.2, "barely moves");
      helper.assertTrue(victim.getAttributeValue(Attributes.JUMP_STRENGTH) <= 1.0E-6, "no jumping");
      Zombie zombie = (Zombie)EntityType.ZOMBIE.create(helper.getLevel());
      zombie.moveTo(victim.getX() + 1.0, victim.getY(), victim.getZ());
      zombie.setNoAi(true);
      helper.getLevel().addFreshEntity(zombie);
      AttackEntityEvent attack = (AttackEntityEvent)NeoForge.EVENT_BUS.post(new AttackEntityEvent(victim, zombie));
      helper.assertTrue(attack.isCanceled(), "no blows");
      float before = zombie.getHealth();
      zombie.hurt(helper.getLevel().damageSources().playerAttack(victim), 4.0F);
      helper.assertTrue(zombie.getHealth() == before, "nothing it is the source of hurts");
      BlockPos at = victim.blockPosition().above(2);
      BreakEvent breaking = (BreakEvent)NeoForge.EVENT_BUS.post(new BreakEvent(helper.getLevel(), at, Blocks.STONE.defaultBlockState(), victim));
      helper.assertTrue(breaking.isCanceled(), "no breaking");
      helper.assertTrue(AbilityManager.press(victim, HoroAbilities.NEGATIVE_HOLLOW) == CastResult.REQUIREMENT, "no casts");
      Negative.apply(victim, 1, null);
      victim.removeEffect(Negative.NEGATIVE);
      helper.assertTrue(!Negative.in(victim), "over");
      helper.assertTrue(!victim.hasData(Negative.VIEW), "the look ends with it");
      zombie.discard();
      done(helper, victim);
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 80,
      batch = "horo_negative"
   )
   public static void negativeHollowLands(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 1));
      player.moveTo(player.getX(), player.getY(), player.getZ(), 180.0F, -80.0F);
      float energy = DFAttachments.data(player).energy();
      helper.assertTrue(AbilityManager.press(player, HoroAbilities.NEGATIVE_HOLLOW) == CastResult.REQUIREMENT, "no target: refused");
      helper.assertTrue(DFAttachments.data(player).energy() == energy, "nothing spent");
      Husk husk = huskAhead(helper, 40.0F);
      lookAt(player, husk);
      helper.assertTrue(AbilityManager.press(player, HoroAbilities.NEGATIVE_HOLLOW) == CastResult.SUCCESS, "cast");
      helper.runAfterDelay(18L, () -> {
         helper.assertTrue(Negative.in(husk), "the husk sank into Negative");
         int left = husk.getEffect(Negative.NEGATIVE).getDuration();
         helper.assertTrue(left > 60 && left <= 107, "about four seconds, a little longer with mastery: " + left);
         husk.discard();
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 100,
      batch = "horo_mini"
   )
   public static void miniHollowClap(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 1));
      Husk husk = huskAhead(helper, 100.0F);
      lookAt(player, husk);
      helper.assertTrue(AbilityManager.press(player, HoroAbilities.MINI_HOLLOW) == CastResult.TOGGLED, "first press");
      helper.assertTrue(MiniHollowAbility.rap(player) != null, "ghosts out");
      helper.runAfterDelay(
         25L,
         () -> {
            helper.assertTrue(MiniHollowAbility.rap(player).clinging() == 4, "all four cling");
            helper.assertTrue(!DFAttachments.data(player).isOnCooldown(HoroAbilities.MINI_HOLLOW.id(), helper.getLevel().getGameTime()), "no cooldown yet");
            AbilityManager.press(player, HoroAbilities.MINI_HOLLOW);
            helper.runAfterDelay(
               9L,
               () -> {
                  helper.assertTrue(MiniHollowAbility.rap(player) == null, "all popped");
                  helper.assertTrue(husk.getHealth() < 90.0F, "four pops hurt: " + husk.getHealth());
                  helper.assertTrue(
                     DFAttachments.data(player).isOnCooldown(HoroAbilities.MINI_HOLLOW.id(), helper.getLevel().getGameTime()), "cooldown from the pop"
                  );
                  husk.discard();
                  done(helper, player);
               }
            );
         }
      );
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 100,
      batch = "horo_mini_timeout"
   )
   public static void miniHollowTimeoutAndDeath(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 1));
      Husk husk = huskAhead(helper, 100.0F);
      lookAt(player, husk);
      AbilityManager.press(player, HoroAbilities.MINI_HOLLOW);
      helper.runAfterDelay(25L, () -> {
         MiniHollowAbility.rewind(player, (int)(HoroAbilities.MINI_HOLLOW.config().extra("attachSeconds") * 20.0));
         helper.runAfterDelay(3L, () -> {
            helper.assertTrue(MiniHollowAbility.rap(player) == null, "popped by themselves");
            float lost = 100.0F - husk.getHealth();
            helper.assertTrue(lost > 0.0F && (double)lost < HoroAbilities.MINI_HOLLOW.config().damage() * 2.2F * 0.75, "for less: " + lost);
            DFAttachments.data(player).clearCooldowns();
            Husk second = huskAhead(helper, 100.0F);
            lookAt(player, second);
            husk.discard();
            AbilityManager.press(player, HoroAbilities.MINI_HOLLOW);
            helper.runAfterDelay(25L, () -> {
               second.kill();
               helper.runAfterDelay(2L, () -> {
                  helper.assertTrue(MiniHollowAbility.rap(player) == null, "harmless pops");
                  done(helper, player);
               });
            });
         });
      });
   }

   private static FakePlayer ghost(GameTestHelper helper, BlockPos relative) {
      FakePlayer player = user(helper, relative);
      helper.assertTrue(AbilityManager.press(player, HoroAbilities.GHOST_FORM) == CastResult.TOGGLED, "out");
      helper.assertTrue(HoroHoro.isGhost(player) && GhostForm.active(player), "a ghost");
      return player;
   }

   private static List<GhostBody> bodies(GameTestHelper helper) {
      return helper.getLevel().getEntitiesOfClass(GhostBody.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16.0));
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 60
   )
   public static void ghostOutAndBack(GameTestHelper helper) {
      FakePlayer player = ghost(helper, new BlockPos(2, 1, 2));
      Vec3 body = GhostForm.body(player);
      helper.assertTrue(bodies(helper).size() == 1, "one body");
      helper.assertTrue(GhostForm.ticketed(player), "its chunk held");
      float health = player.getHealth();
      player.hurt(helper.getLevel().damageSources().generic(), 5.0F);
      helper.assertTrue(player.getHealth() == health, "the ghost takes no blows");
      Zombie zombie = (Zombie)EntityType.ZOMBIE.create(helper.getLevel());
      zombie.moveTo(body.x + 2.0, body.y, body.z);
      zombie.setNoAi(true);
      helper.getLevel().addFreshEntity(zombie);
      helper.assertTrue(((AttackEntityEvent)NeoForge.EVENT_BUS.post(new AttackEntityEvent(player, zombie))).isCanceled(), "no blows from the ghost");
      helper.assertTrue(
         ((BreakEvent)NeoForge.EVENT_BUS.post(new BreakEvent(helper.getLevel(), BlockPos.containing(body).above(3), Blocks.STONE.defaultBlockState(), player)))
            .isCanceled(),
         "no breaking"
      );
      zombie.setTarget(player);
      helper.assertTrue(zombie.getTarget() != player, "mobs ignore it");
      ItemEntity item = new ItemEntity(helper.getLevel(), player.getX(), player.getY(), player.getZ(), new ItemStack(Items.APPLE));
      helper.getLevel().addFreshEntity(item);
      item.playerTouch(player);
      helper.assertTrue(!player.getInventory().contains(new ItemStack(Items.APPLE)), "no pickups");
      item.discard();
      zombie.discard();
      GhostBody entity = bodies(helper).get(0);
      player.moveTo(body.x + 1.0, body.y + 0.5, body.z, 0.0F, 0.0F);
      entity.interact(player, InteractionHand.MAIN_HAND);
      helper.assertTrue(GhostForm.possessing(player), "sinking in");

      for (int i = 1; i <= 13; i++) {
         helper.runAfterDelay((long)i, () -> GhostForm.tick(player));
      }

      helper.runAfterDelay(15L, () -> {
         helper.assertTrue(!GhostForm.active(player) && !HoroHoro.isGhost(player), "back in the body");
         helper.assertTrue(player.position().distanceTo(body) < 1.5, "where the body lay (it settled on the floor)");
         helper.assertTrue(bodies(helper).isEmpty(), "no body left");
         helper.assertTrue(DFAttachments.data(player).isOnCooldown(HoroAbilities.GHOST_FORM.id(), helper.getLevel().getGameTime()), "cooldown");
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform"
   )
   public static void ghostSeastoneAndLeash(GameTestHelper helper) {
      FakePlayer player = ghost(helper, new BlockPos(2, 1, 2));
      Vec3 c = player.position().add(0.0, 1.2, 0.0);
      player.moveTo(c.x, c.y, c.z, 0.0F, 0.0F);
      GhostForm.validate(player);
      BlockPos stone = BlockPos.containing(c.x, c.y, c.z + 1.5);
      helper.getLevel().setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
      helper.getLevel().setBlockAndUpdate(stone.above(), Blocks.STONE.defaultBlockState());
      player.moveTo(c.x, c.y, (double)stone.getZ() + 0.5, 0.0F, 0.0F);
      helper.assertTrue(!GhostForm.validate(player), "through stone");
      BlockPos sea = stone.offset(0, 0, 2);
      helper.getLevel().setBlockAndUpdate(sea, ((Block)DFBlocks.SEASTONE_BLOCK.get()).defaultBlockState());
      helper.getLevel().setBlockAndUpdate(sea.above(), ((Block)DFBlocks.SEASTONE_BLOCK.get()).defaultBlockState());
      player.moveTo(c.x, c.y, (double)sea.getZ() + 0.5, 0.0F, 0.0F);
      helper.assertTrue(GhostForm.validate(player), "Seastone: put back");
      helper.assertTrue(!GhostForm.touchesSeastone(helper.getLevel(), player.getBoundingBox()), "out of the Seastone");
      Vec3 body = GhostForm.body(player);
      double leash = HoroHoro.setting("ghostLeash");
      player.moveTo(body.x + leash + 4.0, body.y + 2.0, body.z, 0.0F, 0.0F);
      player.setDeltaMovement(new Vec3(0.5, 0.0, 0.0));
      GhostForm.tick(player);
      helper.assertTrue(player.getDeltaMovement().x < 0.0, "pulled home past the leash");
      player.moveTo(body.x + leash + 40.0, body.y + 2.0, body.z, 0.0F, 0.0F);
      GhostForm.tick(player);
      helper.assertTrue(player.position().distanceTo(body) <= leash + 8.05, "drawn to the edge");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void ghostBodyTakesBlows(GameTestHelper helper) {
      FakePlayer player = ghost(helper, new BlockPos(2, 1, 2));
      GhostBody body = bodies(helper).get(0);
      helper.assertTrue(GhostForm.forward(body, helper.getLevel().damageSources().generic(), 6.0F) == 6.0F, "in full");
      helper.assertTrue(GhostForm.active(player), "still out");
      helper.assertTrue(GhostForm.forward(body, helper.getLevel().damageSources().playerAttack(player), 6.0F) == 0.0F, "its own ghost cannot strike it");
      Vec3 at = body.position();
      player.moveTo(at.x + 5.0, at.y + 2.0, at.z, 0.0F, 0.0F);
      GhostForm.forward(body, helper.getLevel().damageSources().generic(), 1000.0F);
      helper.assertTrue(!GhostForm.active(player), "a lethal blow pulls the ghost back");
      helper.assertTrue(player.position().distanceTo(at) < 0.3, "it dies in the body");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void ghostCastsAndLogsOut(GameTestHelper helper) {
      FakePlayer player = ghost(helper, new BlockPos(2, 1, 1));
      Vec3 body = GhostForm.body(player);
      Husk husk = huskAhead(helper, 100.0F);
      lookAt(player, husk);
      helper.assertTrue(AbilityManager.press(player, HoroAbilities.NEGATIVE_HOLLOW) == CastResult.SUCCESS, "a fruit skill from the ghost");
      NeoForge.EVENT_BUS.post(new PlayerLoggedOutEvent(player));
      helper.assertTrue(!GhostForm.active(player) && !HoroHoro.isGhost(player), "logged out: back in the body");
      helper.assertTrue(player.position().distanceTo(body) < 0.3, "where the body lay");
      helper.assertTrue(bodies(helper).isEmpty(), "no body left");
      husk.discard();
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 80
   )
   public static void ghostRecallAndYank(GameTestHelper helper) {
      FakePlayer player = ghost(helper, new BlockPos(2, 1, 2));
      Vec3 body = GhostForm.body(player);
      player.moveTo(body.x, body.y + 2.0, body.z + 4.0, 0.0F, 0.0F);
      AbilityManager.press(player, HoroAbilities.GHOST_FORM);
      AbilityManager.release(player, HoroAbilities.GHOST_FORM);
      GhostForm.tick(player);
      helper.assertTrue(GhostForm.active(player) && !GhostForm.recalling(player), "a short press does nothing");
      AbilityManager.press(player, HoroAbilities.GHOST_FORM);
      GhostForm.rewindHold(player, (int)(HoroHoro.setting("recallHoldSeconds") * 20.0));
      GhostForm.tick(player);
      helper.assertTrue(GhostForm.recalling(player), "held: recalled");

      for (int i = 1; i <= 30; i++) {
         helper.runAfterDelay((long)i, () -> {
            if (GhostForm.active(player)) {
               GhostForm.tick(player);
            }
         });
      }

      helper.runAfterDelay(32L, () -> {
         helper.assertTrue(!GhostForm.active(player), "home");
         DFAttachments.data(player).clearCooldowns();
         AbilityManager.press(player, HoroAbilities.GHOST_FORM);
         GhostBody b = bodies(helper).get(0);
         helper.getLevel().setBlockAndUpdate(b.blockPosition().below(), ((Block)DFBlocks.SEASTONE_BLOCK.get()).defaultBlockState());
         player.moveTo(b.getX() + 6.0, b.getY() + 2.0, b.getZ(), 0.0F, 0.0F);
         GhostForm.tick(player);
         helper.assertTrue(!GhostForm.active(player), "yanked back by the Seastone under the body");
         helper.assertTrue(player.position().distanceTo(b.position()) < 0.3, "into the body");
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 200,
      batch = "horo_special"
   )
   public static void specialHollowSwallowsAndBlasts(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 1));
      Husk husk = huskAhead(helper, 200.0F);
      lookAt(player, husk);
      helper.assertTrue(AbilityManager.press(player, HoroAbilities.SPECIAL_HOLLOW) == CastResult.SUCCESS, "cast");
      double speed = HoroAbilities.SPECIAL_HOLLOW.config().extra("speed");
      helper.runAfterDelay(26L, () -> helper.assertTrue(!SpecialHollowAbility.swallowed(husk), "slow: not there yet"));
      boolean[] seen = new boolean[]{false};

      for (int i = 27; i <= 24 + (int)Math.ceil(12.0 / speed); i++) {
         helper.runAfterDelay((long)i, () -> {
            if (!seen[0] && SpecialHollowAbility.swallowed(husk)) {
               seen[0] = true;
               helper.assertTrue(Stun.stunned(husk), "held fast");
               float before = husk.getHealth();
               int swell = (int)Math.round(HoroAbilities.SPECIAL_HOLLOW.config().extra("swellSeconds") * 20.0);
               helper.runAfterDelay((long)(swell - 3), () -> helper.assertTrue(husk.getHealth() == before, "no blast before the swell is over"));
               helper.runAfterDelay((long)(swell + 2), () -> {
                  helper.assertTrue(!SpecialHollowAbility.swallowed(husk), "let go");
                  helper.assertTrue(husk.getHealth() < before - 20.0F, "a heavy blast: " + husk.getHealth());
                  helper.assertTrue(player.getHealth() == player.getMaxHealth(), "the user is spared");
                  husk.discard();
                  done(helper, player);
               });
            }
         });
      }
   }
}
