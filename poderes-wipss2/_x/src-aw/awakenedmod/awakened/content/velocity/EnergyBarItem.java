package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.content.AwakenedEffects;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties.Builder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class EnergyBarItem extends Item {
   static final int NUTRITION = 10;
   static final float SATURATION_MODIFIER = 1.0F;
   static final float EAT_SECONDS = 0.8F;
   static final int ENERGIZED_TICKS = 1200;
   static final int NAUSEA_TICKS = 200;
   static final int POISON_TICKS = 100;

   public EnergyBarItem(Properties properties) {
      super(properties);
   }

   public static Properties food(Properties properties) {
      return properties.food(new Builder().nutrition(10).saturationModifier(1.0F).fast().build());
   }

   public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
      if (entity instanceof ServerPlayer player && !isSpeedster(player)) {
         player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200));
         player.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
         stack.consume(1, player);
         return stack;
      }

      if (entity instanceof ServerPlayer player) {
         player.addEffect(new MobEffectInstance(AwakenedEffects.ENERGIZED, 1200));
      }

      return super.finishUsingItem(stack, level, entity);
   }

   private static boolean isSpeedster(ServerPlayer player) {
      PowerData data = PowerService.data(player);
      return data.hasPower() && "velocity".equals(data.powerId());
   }
}
