package com.awakenedmod.awakened.content;

import com.awakenedmod.awakened.core.CatalystConfig;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.awaken.RerollOffers;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;

public final class CatalystItem extends Item {
   public CatalystItem(Properties properties) {
      super(properties);
   }

   private static int xpCost() {
      return CatalystConfig.xpCost();
   }

   static boolean canUse(ServerPlayer player, int xpLevels, ItemStack stack) {
      return enabled(player, stack) && noOpenOffer(player) && offCooldown(player) && canAfford(player, xpLevels, stack);
   }

   private static boolean enabled(ServerPlayer player, ItemStack stack) {
      boolean on = stack.getItem() instanceof FocusedCatalystItem ? CatalystConfig.focusedEnabled() : CatalystConfig.enabled();
      if (!on) {
         player.displayClientMessage(Component.translatable("item.awakened.catalyst.disabled"), true);
         return false;
      } else if (PowerRegistry.rollable().isEmpty()) {
         player.displayClientMessage(Component.translatable("item.awakened.catalyst.no_powers"), true);
         return false;
      } else {
         return true;
      }
   }

   private static boolean noOpenOffer(ServerPlayer player) {
      if (!RerollOffers.has(player)) {
         return true;
      } else {
         player.displayClientMessage(Component.translatable("item.awakened.catalyst.offer_open"), true);
         return false;
      }
   }

   private static boolean offCooldown(ServerPlayer player) {
      int seconds = CatalystConfig.cooldownSeconds();
      if (seconds > 0 && !player.getAbilities().instabuild) {
         long now = player.serverLevel().getGameTime();
         long ready = PowerService.data(player).lastCatalystUse() + (long)seconds * 20L;
         if (now < ready && now >= PowerService.data(player).lastCatalystUse()) {
            player.displayClientMessage(Component.translatable("item.awakened.catalyst.cooldown", new Object[]{Math.max(1L, (ready - now + 19L) / 20L)}), true);
            return false;
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   static boolean canAfford(ServerPlayer player, int xpLevels, ItemStack stack) {
      if (!player.getAbilities().instabuild && player.experienceLevel < xpLevels) {
         player.displayClientMessage(Component.translatable("item.awakened.catalyst.no_xp", new Object[]{xpLevels, stack.getHoverName()}), true);
         return false;
      } else {
         return true;
      }
   }

   static void charge(ServerPlayer player, int xpLevels, ItemStack stack) {
      PowerService.data(player).setLastCatalystUse(player.serverLevel().getGameTime());
      if (!player.getAbilities().instabuild) {
         if (xpLevels > 0) {
            player.giveExperienceLevels(-xpLevels);
         }

         stack.shrink(1);
      }
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      this.addTooltip(tooltip::add);
   }

   private void addTooltip(Consumer<Component> adder) {
      int cost = xpCost();
      Component body = cost > 0
         ? Component.translatable("item.awakened.catalyst.tooltip", new Object[]{cost})
         : Component.translatable("item.awakened.catalyst.tooltip.free");
      adder.accept(body.copy().withStyle(ChatFormatting.GRAY));
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (player instanceof ServerPlayer serverPlayer) {
         int cost = xpCost();
         if (!canUse(serverPlayer, cost, stack)) {
            return InteractionResultHolder.fail(stack);
         } else {
            PowerService.reroll(serverPlayer, true);
            charge(serverPlayer, cost, stack);
            return InteractionResultHolder.success(stack);
         }
      } else {
         return InteractionResultHolder.success(stack);
      }
   }
}
