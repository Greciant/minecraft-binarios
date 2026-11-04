package com.devilfruits.core.gacha;

import com.devilfruits.core.config.CoreConfig;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;

public class CasketItem extends Item {
   public CasketItem(Properties properties) {
      super(properties);
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (hand != InteractionHand.MAIN_HAND) {
         return InteractionResultHolder.pass(stack);
      } else if (!CasketOpening.isKey(player.getOffhandItem())) {
         if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.devilfruits.casket.need_key").withStyle(ChatFormatting.GRAY), true);
         }

         return InteractionResultHolder.fail(stack);
      } else {
         player.startUsingItem(hand);
         if (player instanceof ServerPlayer server) {
            Component refusal = CasketOpening.refusal(server);
            if (refusal != null) {
               server.displayClientMessage(refusal, true);
               server.stopUsingItem();
               return InteractionResultHolder.fail(stack);
            }
         }

         return InteractionResultHolder.consume(stack);
      }
   }

   public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
      if (!level.isClientSide && !CasketOpening.isKey(entity.getOffhandItem())) {
         entity.stopUsingItem();
      }
   }

   public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
      if (entity instanceof ServerPlayer player && entity.getUsedItemHand() == InteractionHand.MAIN_HAND) {
         CasketOpening.open(player, stack);
      }

      return stack;
   }

   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return useTicks();
   }

   public static int useTicks() {
      return Math.max(1, (int)Math.round((Double)CoreConfig.CASKET_OPEN_SECONDS.get() * 20.0));
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.NONE;
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.devilfruits.devil_fruit_casket.desc").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("item.devilfruits.devil_fruit_casket.how").withStyle(ChatFormatting.DARK_GRAY));
   }
}
