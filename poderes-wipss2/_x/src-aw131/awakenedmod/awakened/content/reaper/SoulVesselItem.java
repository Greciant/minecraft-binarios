package com.awakenedmod.awakened.content.reaper;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SoulVesselItem extends Item {
   public static final long SOUL_COST = 500L;
   private static final int BAR_COLOR = 6277344;
   private final boolean filled;

   public SoulVesselItem(Properties properties, boolean filled) {
      super(properties);
      this.filled = filled;
   }

   public boolean isFilled() {
      return this.filled;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (player instanceof ServerPlayer serverPlayer) {
         ItemStack held = this.run(serverPlayer, stack);
         return held == null ? InteractionResultHolder.fail(stack) : InteractionResultHolder.success(held);
      } else {
         return InteractionResultHolder.success(stack);
      }
   }

   private ItemStack run(ServerPlayer player, ItemStack stack) {
      if (!"reaper".equals(PowerService.data(player).powerId())) {
         return fail(player, "item.awakened.soul_vessel.not_reaper");
      } else {
         return this.filled ? this.withdraw(player, stack) : this.fill(player, stack);
      }
   }

   private ItemStack fill(ServerPlayer player, ItemStack stack) {
      ReaperSoulData bank = ReaperAttachments.souls(player);
      if (bank.souls() < 500L) {
         return fail(player, "item.awakened.soul_vessel.not_enough");
      } else {
         ReaperAttachments.withdraw(player, 500L);
         PowerService.sync(player);
         feedback(player, (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), 0.7F, "item.awakened.soul_vessel.filled");
         return ItemUtils.createFilledResult(stack, player, new ItemStack((ItemLike)AwakenedItems.SOUL_CORE.get()));
      }
   }

   private ItemStack withdraw(ServerPlayer player, ItemStack stack) {
      if (!player.isShiftKeyDown()) {
         player.displayClientMessage(Component.translatable("item.awakened.soul_core.shift_hint").withStyle(ChatFormatting.DARK_GRAY), true);
         return stack;
      } else {
         ReaperAttachments.add(player, 500L);
         PowerService.sync(player);
         feedback(player, (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), 1.2F, "item.awakened.soul_core.emptied");
         return ItemUtils.createFilledResult(stack, player, new ItemStack((ItemLike)AwakenedItems.SOUL_VESSEL.get()));
      }
   }

   private static void feedback(ServerPlayer player, SoundEvent sound, float pitch, String key) {
      Vec3 at = player.position();
      Fx.sound(player.serverLevel(), at, sound, SoundSource.PLAYERS, 0.9F, pitch);
      player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.AQUA), true);
   }

   private static ItemStack fail(ServerPlayer player, String key) {
      player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
      return null;
   }

   public boolean isBarVisible(ItemStack stack) {
      return this.filled;
   }

   public int getBarWidth(ItemStack stack) {
      return this.filled ? 13 : 0;
   }

   public int getBarColor(ItemStack stack) {
      return 6277344;
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      this.addTooltip(tooltip::add);
   }

   private void addTooltip(Consumer<Component> adder) {
      if (this.filled) {
         adder.accept(Component.translatable("item.awakened.soul_core.souls", new Object[]{500L}).withStyle(ChatFormatting.AQUA));
         adder.accept(Component.translatable("item.awakened.soul_core.shift_hint").withStyle(ChatFormatting.DARK_GRAY));
      } else {
         adder.accept(Component.translatable("item.awakened.soul_vessel.fill", new Object[]{500L}).withStyle(ChatFormatting.DARK_GRAY));
      }
   }
}
