package com.awakenedmod.awakened.content;

import com.awakenedmod.awakened.Awakened;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.net.OpenCatalystSelectPayload;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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
import net.neoforged.neoforge.network.PacketDistributor;

public final class FocusedCatalystItem extends Item {
   private final IntSupplier picks;
   private final IntSupplier xpCost;

   public FocusedCatalystItem(Properties properties, IntSupplier picks, IntSupplier xpCost) {
      super(properties);
      this.picks = picks;
      this.xpCost = xpCost;
   }

   public int picks(ServerPlayer player) {
      return Math.max(1, Math.min(this.picks.getAsInt(), selectable(player).size()));
   }

   public int xpCost() {
      return this.xpCost.getAsInt();
   }

   public static void handleConfirm(ServerPlayer player, boolean offHand, List<String> powerIds) {
      if (player != null) {
         InteractionHand hand = offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
         ItemStack stack = player.getItemInHand(hand);
         if (stack.getItem() instanceof FocusedCatalystItem catalyst) {
            List<Power> pool = resolvePool(powerIds, catalyst.picks(player));
            if (pool == null) {
               Awakened.LOGGER.warn("Invalid Focused Catalyst selection from {}: {}", player.getName().getString(), powerIds);
               player.displayClientMessage(Component.translatable("item.awakened.catalyst.stale"), true);
               PacketDistributor.sendToPlayer(player, catalyst.openPayload(player, hand), new CustomPacketPayload[0]);
            } else {
               Set<String> locked = lockedPicks(player);
               if (pool.stream().anyMatch(p -> locked.contains(p.id()))) {
                  player.displayClientMessage(Component.translatable("item.awakened.catalyst.taken"), true);
               } else if (CatalystItem.canUse(player, catalyst.xpCost(), stack)) {
                  PowerService.reroll(player, pool.size() > 1, pool);
                  CatalystItem.charge(player, catalyst.xpCost(), stack);
               }
            }
         }
      }
   }

   private static List<Power> selectable(ServerPlayer player) {
      List<Power> rollable = PowerRegistry.rollable();
      List<Power> free = PowerService.unclaimed(player, rollable);
      if (free.isEmpty()) {
         free = rollable;
      }

      String current = PowerService.data(player).powerId();
      List<Power> changed = free.stream().filter(p -> !p.id().equals(current)).toList();
      return changed.isEmpty() ? free : changed;
   }

   private static Set<String> lockedPicks(ServerPlayer player) {
      Set<String> free = selectable(player).stream().map(Power::id).collect(Collectors.toSet());
      return PowerRegistry.rollable().stream().map(Power::id).filter(id -> !free.contains(id)).collect(Collectors.toSet());
   }

   private OpenCatalystSelectPayload openPayload(ServerPlayer player, InteractionHand hand) {
      return new OpenCatalystSelectPayload(this.picks(player), this.xpCost(), hand == InteractionHand.OFF_HAND, List.copyOf(lockedPicks(player)), unavailable());
   }

   private static List<String> unavailable() {
      return PowerRegistry.all().stream().filter(p -> p.rollable() && !PowerRegistry.obtainable(p)).map(Power::id).toList();
   }

   private static List<Power> resolvePool(List<String> powerIds, int picks) {
      if (powerIds.size() == picks && powerIds.stream().distinct().count() == (long)picks) {
         List<Power> pool = new ArrayList<>(picks);

         for (String id : powerIds) {
            Power power = PowerRegistry.get(id);
            if (power == null || !PowerRegistry.obtainable(power)) {
               return null;
            }

            pool.add(power);
         }

         return pool;
      } else {
         return null;
      }
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      this.addTooltip(tooltip::add);
   }

   private void addTooltip(Consumer<Component> adder) {
      int picks = Math.max(1, this.picks.getAsInt());
      int cost = this.xpCost();
      Component body;
      if (picks == 1) {
         body = cost > 0
            ? Component.translatable("item.awakened.catalyst.tooltip.exact", new Object[]{cost})
            : Component.translatable("item.awakened.catalyst.tooltip.exact.free");
      } else {
         body = cost > 0
            ? Component.translatable("item.awakened.catalyst.tooltip.focused", new Object[]{picks, cost})
            : Component.translatable("item.awakened.catalyst.tooltip.focused.free", new Object[]{picks});
      }

      adder.accept(body.copy().withStyle(ChatFormatting.GRAY));
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (player instanceof ServerPlayer serverPlayer) {
         if (!CatalystItem.canUse(serverPlayer, this.xpCost(), stack)) {
            return InteractionResultHolder.fail(stack);
         } else {
            PacketDistributor.sendToPlayer(serverPlayer, this.openPayload(serverPlayer, hand), new CustomPacketPayload[0]);
            return InteractionResultHolder.success(stack);
         }
      } else {
         return InteractionResultHolder.success(stack);
      }
   }
}
