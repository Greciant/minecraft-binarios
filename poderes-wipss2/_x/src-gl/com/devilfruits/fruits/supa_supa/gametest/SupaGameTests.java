package com.devilfruits.fruits.supa_supa.gametest;

import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.CastResult;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.WorldFruitData;
import com.devilfruits.fruits.supa_supa.SkateMotion;
import com.devilfruits.fruits.supa_supa.SupaAbilities;
import com.devilfruits.fruits.supa_supa.SupaSupa;
import com.devilfruits.fruits.supa_supa.ability.SteelBodyAbility;
import com.devilfruits.registry.DFAttachments;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("devilfruits")
@PrefixGameTestTemplate(false)
public final class SupaGameTests {
   private static final String PLATFORM = "platform";

   static FakePlayer user(GameTestHelper helper, BlockPos relative) {
      FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "supa_test"));
      BlockPos pos = helper.absolutePos(relative);
      player.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
      player.setDeltaMovement(Vec3.ZERO);
      WorldFruitData.get(helper.getLevel().getServer()).setFree(SupaSupa.fruit());
      FruitRules.grantPower(player, SupaSupa.fruit(), false);
      DFAttachments.data(player).setEnergy(1000.0F);
      return player;
   }

   static void done(GameTestHelper helper, FakePlayer player) {
      AbilityManager.cancelAll(player);
      AbilityManager.forget(player.getUUID());
      FruitRules.removePower(player);
      WorldFruitData.get(helper.getLevel().getServer()).setFree(SupaSupa.fruit());
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void supaUnlockGating(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      DFAttachments.data(player).setMastery(1);
      helper.assertTrue(AbilityManager.press(player, SupaAbilities.STEEL_BODY) == CastResult.LOCKED, "Steel Body at mastery 2");
      helper.assertTrue(AbilityManager.press(player, SupaAbilities.BLADE_SKATES) == CastResult.LOCKED, "Blade Skates at mastery 3");
      helper.assertTrue(AbilityManager.press(player, SupaAbilities.SPIRAL_HOLLOW) == CastResult.LOCKED, "Spiral Hollow at mastery 5");
      helper.assertTrue(AbilityManager.press(player, SupaAbilities.SUPER_SLASH) == CastResult.LOCKED, "Super Slash at mastery 8");
      DFAttachments.data(player).setMastery(8);
      helper.assertTrue(AbilityManager.press(player, SupaAbilities.SUPER_SLASH) == CastResult.CHARGING, "Super Slash charges at mastery 8");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void supaSteelBodyBlows(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      DFAttachments.data(player).setMastery(2);
      helper.assertTrue(AbilityManager.press(player, SupaAbilities.STEEL_BODY) == CastResult.CHANNELLING, "Steel Body holds");
      helper.assertTrue(SteelBodyAbility.active(player), "the body is steel");
      Zombie zombie = (Zombie)helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 1, 2));
      DamageSources damage = helper.getLevel().damageSources();
      float energy = DFAttachments.data(player).energy();
      float fist = SteelBodyAbility.absorb(player, damage.mobAttack(zombie), 20.0F);
      helper.assertTrue(Math.abs(fist - 1.0F) < 0.001F, "a bare blow shrinks to 5 %: " + fist);
      helper.assertTrue(DFAttachments.data(player).energy() < energy, "turning a blow aside costs energy");
      zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
      float sword = SteelBodyAbility.absorb(player, damage.mobAttack(zombie), 20.0F);
      helper.assertTrue(sword == 0.0F, "a blade does nothing to steel: " + sword);
      Arrow arrow = new Arrow(helper.getLevel(), zombie, new ItemStack(Items.ARROW), null);
      float shot = SteelBodyAbility.absorb(player, damage.arrow(arrow, zombie), 20.0F);
      helper.assertTrue(shot == 0.0F, "an arrow does nothing to steel: " + shot);
      float voidHit = SteelBodyAbility.absorb(player, damage.fellOutOfWorld(), 20.0F);
      helper.assertTrue(voidHit == 20.0F, "the void passes every defence");
      DFAttachments.data(player).setEnergy(0.0F);
      AbilityManager.tickPlayer(player, DFAttachments.data(player));
      helper.assertFalse(SteelBodyAbility.active(player), "Steel Body ends at 0 energy");
      zombie.discard();
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void supaSkateSpeedCap(GameTestHelper helper) {
      SkateMotion.Params p = SupaAbilities.BLADE_SKATES.params();
      Vec3 v = Vec3.ZERO;
      double top = 0.0;

      for (int i = 0; i < 400; i++) {
         v = SkateMotion.step(v, 1.0, i % 40 < 20 ? 1.0 : -1.0, (float)i * 3.0F, i % 50 != 0, false, p);
         top = Math.max(top, v.length());
      }

      helper.assertTrue(top <= p.maxSpeed() + 1.0E-6, "never past the top speed: " + top);
      helper.assertTrue(top > p.maxSpeed() * 0.8, "pushing on reaches near the top speed: " + top);
      Vec3 braked = v;

      for (int i = 0; i < 20; i++) {
         braked = SkateMotion.step(braked, 0.0, 0.0, 0.0F, true, true, p);
      }

      helper.assertTrue(braked.length() < 0.1, "sneaking brakes hard: " + braked.length());
      helper.assertTrue(p.maxSpeed() < 10.0, "far below the vanilla moved-too-quickly limit");
      helper.assertTrue(!SkateMotion.tooFast(p.maxSpeed(), p.maxSpeed()), "top speed is fine");
      helper.assertTrue(SkateMotion.tooFast(p.maxSpeed() * 2.0, p.maxSpeed()), "twice the top speed is caught");
      helper.succeed();
   }

   private SupaGameTests() {
   }
}
