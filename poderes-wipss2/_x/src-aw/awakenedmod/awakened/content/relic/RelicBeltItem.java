package com.awakenedmod.awakened.content.relic;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.core.ItemGrant;
import com.awakenedmod.awakened.core.relic.Relic;
import com.awakenedmod.awakened.core.relic.RelicRegistry;
import com.awakenedmod.awakened.core.relic.RelicSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class RelicBeltItem extends Item implements ICurioItem {
   private static final String RELICS_KEY = "relics";

   public RelicBeltItem(Properties properties) {
      super(properties);
   }

   public static List<String> relicIds(ItemStack stack) {
      CustomData data = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      if (data == null) {
         return List.of();
      } else {
         String joined = data.copyTag().getString("relics");
         if (joined.isEmpty()) {
            return List.of();
         } else {
            List<String> ids = new ArrayList<>();

            for (String id : joined.split(",")) {
               if (!id.isEmpty()) {
                  ids.add(id);
               }
            }

            return ids;
         }
      }
   }

   public static ItemStack withRelics(List<String> ids) {
      ItemStack stack = new ItemStack((ItemLike)AwakenedItems.RELIC_BELT.get());
      if (ids != null && !ids.isEmpty()) {
         CompoundTag tag = new CompoundTag();
         tag.putString("relics", String.join(",", ids));
         stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      }

      return stack;
   }

   public static ItemStack takeStack(Player player) {
      Inventory inv = player.getInventory();

      for (int i = 0; i < inv.getContainerSize(); i++) {
         ItemStack s = inv.getItem(i);
         if (s.getItem() instanceof RelicBeltItem) {
            ItemStack one = s.copyWithCount(1);
            s.shrink(1);
            return one;
         }
      }

      return ItemStack.EMPTY;
   }

   public static void give(Player player, List<String> ids) {
      ItemGrant.to(player, withRelics(ids));
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack held = player.getItemInHand(hand);
      if (player instanceof ServerPlayer serverPlayer) {
         RelicSystem.openBelt(serverPlayer);
         return InteractionResultHolder.success(held);
      } else {
         return InteractionResultHolder.success(held);
      }
   }

   public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
      refresh(slotContext);
   }

   public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
      refresh(slotContext);
   }

   private static void refresh(SlotContext slotContext) {
      if (slotContext.entity() instanceof ServerPlayer player) {
         RelicSystem.refresh(player);
      }
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      List<String> ids = relicIds(stack);
      if (ids.isEmpty()) {
         tooltip.add(Component.translatable("item.awakened.relic_belt.empty").withStyle(ChatFormatting.DARK_GRAY));
      } else {
         for (String id : ids) {
            Relic relic = RelicRegistry.get(id);
            Component name = relic != null
               ? relic.displayName().copy().withStyle(s -> s.withColor(relic.themeColor()))
               : Component.literal(id).withStyle(ChatFormatting.GRAY);
            tooltip.add(name);
         }
      }
   }
}
