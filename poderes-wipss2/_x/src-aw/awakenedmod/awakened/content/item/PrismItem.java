package com.awakenedmod.awakened.content.item;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.velocity.VelocityColor;
import com.awakenedmod.awakened.content.velocity.VelocityPalette;
import com.awakenedmod.awakened.content.velocity.VelocityPower;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.power.AbilityFeedback;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public final class PrismItem extends Item {
   public PrismItem(Properties properties) {
      super(properties);
   }

   public static ItemStack of(DyeColor dye) {
      ItemStack stack = new ItemStack((ItemLike)AwakenedItems.PRISM.get());
      stack.set((DataComponentType)PrismComponents.LIGHTNING_COLOR.get(), dye.getId());
      int body = VelocityPalette.of(dye).body();
      stack.set(DataComponents.DYED_COLOR, new DyedItemColor(body, false));
      return stack;
   }

   public static DyeColor dyeOf(ItemStack stack) {
      Integer id = (Integer)stack.get((DataComponentType)PrismComponents.LIGHTNING_COLOR.get());
      return id != null && id >= 0 && id < DyeColor.values().length ? DyeColor.byId(id) : null;
   }

   public static int tint(ItemStack stack) {
      DyeColor dye = dyeOf(stack);
      return dye == null ? -1 : VelocityPalette.of(dye).bodyArgb();
   }

   private static boolean spend(ServerPlayer player, ItemStack held) {
      DyeColor dye = dyeOf(held);
      if (dye != null && VelocityPower.owns(player)) {
         VelocityColor.set(player, dye);
         if (!player.getAbilities().instabuild) {
            held.shrink(1);
         }

         player.displayClientMessage(VelocityColor.recoloured(dye), true);
         Fx.sound(player.serverLevel(), player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.4F);
         return true;
      } else {
         AbilityFeedback.fail(player, "awakened.feedback.prism_no_velocity");
         return false;
      }
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack held = player.getItemInHand(hand);
      if (player instanceof ServerPlayer server) {
         return spend(server, held) ? InteractionResultHolder.success(held) : InteractionResultHolder.fail(held);
      } else {
         return InteractionResultHolder.success(held);
      }
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      addTooltip(stack, tooltip::add);
   }

   private static void addTooltip(ItemStack stack, Consumer<Component> adder) {
      DyeColor dye = dyeOf(stack);
      if (dye != null) {
         adder.accept(
            Component.translatable("item.awakened.prism.color", new Object[]{Component.translatable("color.minecraft." + dye.getSerializedName())})
               .withStyle(ChatFormatting.GRAY)
         );
      }

      adder.accept(Component.translatable("item.awakened.prism.tooltip").withStyle(ChatFormatting.DARK_GRAY));
   }
}
