package com.awakenedmod.awakened.content.relic;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.core.ItemGrant;
import com.awakenedmod.awakened.core.relic.Relic;
import com.awakenedmod.awakened.core.relic.RelicRegistry;
import com.awakenedmod.awakened.core.relic.RelicSystem;
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
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public final class RelicItem extends Item {
   private static final String RELIC_KEY = "relic";
   private final String boundRelicId;

   public RelicItem(Properties properties) {
      this(properties, null);
   }

   public RelicItem(Properties properties, String boundRelicId) {
      super(properties);
      this.boundRelicId = boundRelicId;
   }

   public String boundRelicId() {
      return this.boundRelicId;
   }

   public static ItemStack of(String relicId) {
      ItemStack stack = RelicItems.stack(relicId);
      return stack.isEmpty() ? legacy(relicId) : stack;
   }

   public static ItemStack legacy(String relicId) {
      ItemStack stack = new ItemStack((ItemLike)AwakenedItems.RELIC.get());
      CompoundTag tag = new CompoundTag();
      tag.putString("relic", relicId);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      Relic relic = RelicRegistry.get(relicId);
      if (relic != null) {
         stack.set(DataComponents.DYED_COLOR, new DyedItemColor(relic.themeColor() & 16777215, false));
      }

      return stack;
   }

   public static String relicId(ItemStack stack) {
      if (stack.getItem() instanceof RelicItem relicItem && relicItem.boundRelicId != null) {
         return relicItem.boundRelicId;
      }

      CustomData data = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      if (data == null) {
         return null;
      } else {
         String id = data.copyTag().getString("relic");
         return id.isEmpty() ? null : id;
      }
   }

   public static int count(Player player, String relicId) {
      Inventory inv = player.getInventory();
      int n = 0;

      for (int i = 0; i < inv.getContainerSize(); i++) {
         ItemStack s = inv.getItem(i);
         if (s.getItem() instanceof RelicItem && relicId.equals(relicId(s))) {
            n += s.getCount();
         }
      }

      return n;
   }

   public static boolean take(Player player, String relicId) {
      Inventory inv = player.getInventory();

      for (int i = 0; i < inv.getContainerSize(); i++) {
         ItemStack s = inv.getItem(i);
         if (s.getItem() instanceof RelicItem && relicId.equals(relicId(s))) {
            s.shrink(1);
            return true;
         }
      }

      return false;
   }

   public static void give(Player player, String relicId) {
      ItemGrant.to(player, of(relicId));
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack held = player.getItemInHand(hand);
      if (player instanceof ServerPlayer serverPlayer) {
         String id = relicId(held);
         if (id != null && RelicRegistry.get(id) != null) {
            RelicSystem.openBelt(serverPlayer);
            return InteractionResultHolder.success(held);
         } else {
            return InteractionResultHolder.fail(held);
         }
      } else {
         return InteractionResultHolder.success(held);
      }
   }

   public Component getName(ItemStack stack) {
      Relic relic = RelicRegistry.get(relicId(stack));
      return (Component)(relic == null ? super.getName(stack) : relic.displayName().copy().withStyle(s -> s.withColor(relic.themeColor())));
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      Relic relic = RelicRegistry.get(relicId(stack));
      if (relic != null) {
         tooltip.add(relic.description().copy().withStyle(ChatFormatting.GRAY));

         for (Component line : RelicTooltip.effectLines(relic)) {
            tooltip.add(line);
         }
      }
   }
}
