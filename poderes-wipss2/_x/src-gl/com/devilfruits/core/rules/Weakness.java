package com.devilfruits.core.rules;

import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.core.poneglyph.PoneglyphConfig;
import com.devilfruits.core.seastone.SeastoneCombat;
import com.devilfruits.core.seastone.SeastoneGear;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFTags;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class Weakness {
   public static boolean isSubmerged(Player player) {
      Level level = player.level();
      BlockPos feet = BlockPos.containing(player.getX(), player.getY() + 0.1, player.getZ());
      return level.getFluidState(feet).is(FluidTags.WATER) && level.getFluidState(feet.above()).is(FluidTags.WATER);
   }

   public static boolean touchesSeastone(Player player) {
      if (!(Boolean)CoreConfig.SEASTONE_WEAKNESS.get()) {
         return false;
      } else if (suppressesHolder(player.getMainHandItem()) || suppressesHolder(player.getOffhandItem()) || player.hasEffect(SeastoneGear.SUPPRESSED)) {
         return true;
      } else {
         return PoneglyphConfig.ARMOR_SUPPRESSES_FRUIT_USER.get() && SeastoneCombat.armorPieces(player) > 0
            ? true
            : player.getBlockStateOn().is(DFTags.SEASTONE_BLOCKS) || player.level().getBlockState(player.blockPosition()).is(DFTags.SEASTONE_BLOCKS);
      }
   }

   private static boolean suppressesHolder(ItemStack stack) {
      return stack.is(DFTags.SEASTONE_ITEMS)
         && !stack.is((Item)SeastoneGear.JITTE.get())
         && (!stack.is(SeastoneGear.GEAR) || (Boolean)PoneglyphConfig.GEAR_SUPPRESSES_HOLDER.get());
   }

   public static boolean drownedPowers(Player player, DevilFruit fruit) {
      return (Boolean)CoreConfig.WATER_WEAKNESS.get() && fruit.waterWeakness() && isSubmerged(player);
   }

   public static boolean isSuppressed(Player player) {
      return seaSuppressed(player) || LogiaManager.abilitiesBlocked(player);
   }

   public static boolean seaSuppressed(Player player) {
      if (player.isSpectator()) {
         return false;
      } else {
         DevilFruit fruit = DFAttachments.data(player).fruit();
         return fruit != null && (drownedPowers(player, fruit) || touchesSeastone(player));
      }
   }

   public static void tickServer(Player player, PlayerFruitData data) {
      boolean suppressed = isSuppressed(player);
      data.setSuppressed(suppressed);
      if (suppressed && player.tickCount % 10 == 0) {
         player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, true, false, true));
         player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, true, false, true));
         player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 1, true, false, true));
      }
   }

   public static void applySinking(Player player) {
      if (!player.getAbilities().flying) {
         DevilFruit fruit = DFAttachments.data(player).fruit();
         if (fruit != null && !player.isSpectator() && drownedPowers(player, fruit)) {
            Vec3 motion = player.getDeltaMovement();
            double sink = (Double)CoreConfig.SINK_SPEED.get();
            player.setDeltaMovement(motion.x * 0.6, Math.min(motion.y, -sink), motion.z * 0.6);
            player.setSwimming(false);
            player.setSprinting(false);
         }
      }
   }

   private Weakness() {
   }
}
