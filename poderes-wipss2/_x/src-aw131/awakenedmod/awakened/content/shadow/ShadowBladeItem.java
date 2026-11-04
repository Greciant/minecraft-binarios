package com.awakenedmod.awakened.content.shadow;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.AbilityFeedbackPayload;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ShadowBladeItem extends Item {
   private static final float SLASH_COST = 7.0F;

   public ShadowBladeItem(Properties properties) {
      super(properties);
   }

   public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
      return false;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack held = player.getItemInHand(hand);
      if (player instanceof ServerPlayer serverPlayer) {
         PowerData data = PowerService.data(serverPlayer);
         if (!data.awakened() || !"shadow".equals(data.powerId())) {
            return InteractionResultHolder.pass(held);
         } else if (PowerService.refusedBySuppression(serverPlayer)) {
            return InteractionResultHolder.fail(held);
         } else {
            float cost = slashCost();
            if (!data.infiniteStamina() && !StaminaSystem.canAfford(data, cost)) {
               PacketDistributor.sendToPlayer(serverPlayer, new AbilityFeedbackPayload(0), new CustomPacketPayload[0]);
               return InteractionResultHolder.fail(held);
            } else {
               ShadowSlash.shoot(serverPlayer);
               if (!data.infiniteStamina()) {
                  data.setStamina(data.stamina() - cost);
                  data.setLastSpendTime(level.getGameTime());
               }

               PowerService.sync(serverPlayer);
               return InteractionResultHolder.success(held);
            }
         }
      } else {
         return InteractionResultHolder.success(held);
      }
   }

   private static float slashCost() {
      return StaminaSystem.effectiveCost(7.0F, ShadowBladeAbility.class.getSimpleName());
   }

   public static boolean powerExists() {
      return PowerRegistry.get("shadow") != null;
   }

   public static ItemStack stack() {
      return new ItemStack((ItemLike)AwakenedItems.SHADOW_BLADE.get());
   }

   public static void setTier(ItemStack stack, int tier) {
      CustomModelData cmd = (CustomModelData)stack.get(DataComponents.CUSTOM_MODEL_DATA);
      if (cmd == null || cmd.value() != tier) {
         stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(tier));
      }
   }
}
