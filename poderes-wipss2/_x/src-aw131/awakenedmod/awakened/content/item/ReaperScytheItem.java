package com.awakenedmod.awakened.content.item;

import com.awakenedmod.awakened.core.progression.BalanceKeyed;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;

public final class ReaperScytheItem extends Item implements BalanceKeyed {
   public ReaperScytheItem(Properties properties) {
      super(properties);
   }

   @Override
   public List<String> balanceKeys() {
      return List.of("ReaperScytheReap");
   }

   private static void swing(LivingEntity entity) {
      if (entity instanceof ServerPlayer player) {
         ReaperScytheAnimator.onSwing(player);
      }
   }

   private static void hit(LivingEntity target, LivingEntity attacker) {
      if (attacker instanceof ServerPlayer player) {
         ReaperScytheAnimator.onHit(player, target);
      }
   }

   public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
      swing(entity);
      return false;
   }

   public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      hit(target, attacker);
      return true;
   }
}
