package com.devilfruits.fruits.yomi_yomi.item;

import com.devilfruits.fruits.yomi_yomi.YomiItems;
import com.devilfruits.fruits.yomi_yomi.YomiSounds;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public class SoulCaneItem extends SwordItem {
   private final ItemAttributeModifiers sheathed = createAttributes(Tiers.IRON, 3.0F - Tiers.IRON.getAttackDamageBonus(), -2.6F);
   private final ItemAttributeModifiers drawn = createAttributes(Tiers.IRON, 5.0F - Tiers.IRON.getAttackDamageBonus(), -2.0F);

   public SoulCaneItem(Properties properties) {
      super(Tiers.IRON, properties.attributes(createAttributes(Tiers.IRON, 3.0F - Tiers.IRON.getAttackDamageBonus(), -2.6F)));
   }

   public static boolean isDrawn(ItemStack stack) {
      return stack.getItem() instanceof SoulCaneItem && Boolean.TRUE.equals(stack.get((DataComponentType)YomiItems.DRAWN.get()));
   }

   public static boolean ready(Player player) {
      return isDrawn(player.getMainHandItem()) && player.getOffhandItem().is((Item)YomiItems.SCABBARD.get());
   }

   public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
      return isDrawn(stack) ? this.drawn : this.sheathed;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (hand == InteractionHand.MAIN_HAND && player.isSecondaryUseActive()) {
         if (player instanceof ServerPlayer sp) {
            Component refusal = isDrawn(stack) ? sheathe(sp) : draw(sp);
            if (refusal != null) {
               sp.displayClientMessage(refusal.copy().withStyle(ChatFormatting.RED), true);
               return InteractionResultHolder.fail(stack);
            }
         }

         return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
      } else {
         return super.use(level, player, hand);
      }
   }

   @Nullable
   public static Component draw(ServerPlayer player) {
      ItemStack stack = player.getMainHandItem();
      if (stack.getItem() instanceof SoulCaneItem && !isDrawn(stack)) {
         ItemStack off = player.getOffhandItem();
         if (!off.isEmpty()) {
            if (player.getInventory().getFreeSlot() < 0) {
               return Component.translatable("message.devilfruits.yomi_yomi.cane_no_room");
            }

            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            player.getInventory().add(off);
         }

         player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack((ItemLike)YomiItems.SCABBARD.get()));
         stack.set((DataComponentType)YomiItems.DRAWN.get(), true);
         player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), YomiSounds.CANE_DRAW.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
         return null;
      } else {
         return Component.translatable("message.devilfruits.yomi_yomi.cane_not_sheathed");
      }
   }

   @Nullable
   public static Component sheathe(ServerPlayer player) {
      ItemStack stack = player.getMainHandItem();
      if (!isDrawn(stack)) {
         return Component.translatable("message.devilfruits.yomi_yomi.cane_not_drawn");
      } else if (!player.getOffhandItem().is((Item)YomiItems.SCABBARD.get())) {
         return Component.translatable("message.devilfruits.yomi_yomi.cane_no_scabbard");
      } else {
         player.getOffhandItem().shrink(1);
         stack.remove((DataComponentType)YomiItems.DRAWN.get());
         player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), YomiSounds.CANE_SHEATHE.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
         return null;
      }
   }

   public Component getName(ItemStack stack) {
      return (Component)(isDrawn(stack) ? Component.translatable("item.devilfruits.soul_solid.drawn") : super.getName(stack));
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(
         Component.translatable(isDrawn(stack) ? "item.devilfruits.soul_solid.hint_drawn" : "item.devilfruits.soul_solid.hint_sheathed")
            .withStyle(ChatFormatting.GRAY)
      );
   }
}
