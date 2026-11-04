package com.devilfruits.fruits.kage_kage.item;

import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.fruits.kage_kage.KageItems;
import com.devilfruits.fruits.kage_kage.KageKage;
import com.devilfruits.fruits.kage_kage.KageSounds;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public class MoriaScissorsItem extends SwordItem {
   private final ItemAttributeModifiers joined;
   private final ItemAttributeModifiers split;
   private static final Map<LivingEntity, Boolean> CLIENT_TURN = Collections.synchronizedMap(new WeakHashMap<>());
   private static final Map<LivingEntity, Boolean> SERVER_TURN = Collections.synchronizedMap(new WeakHashMap<>());

   public MoriaScissorsItem(Tier tier, Properties properties) {
      super(tier, properties);
      this.joined = createAttributes(tier, 6.0F - tier.getAttackDamageBonus(), -2.8F);
      this.split = createAttributes(tier, 3.0F - tier.getAttackDamageBonus(), -0.70000005F);
   }

   public static boolean isSplit(ItemStack stack) {
      return (Boolean)stack.getOrDefault((DataComponentType)KageItems.SPLIT.get(), false);
   }

   public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
      return isSplit(stack) ? this.split : this.joined;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
         return InteractionResultHolder.pass(stack);
      } else if (isSplit(stack)) {
         if (player instanceof ServerPlayer sp) {
            join(sp);
         }

         return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
      } else if (!player.getOffhandItem().isEmpty()) {
         if (!level.isClientSide()) {
            player.displayClientMessage(Component.translatable("message.devilfruits.kage_kage.scissors.offhand_busy"), true);
         }

         return InteractionResultHolder.fail(stack);
      } else {
         if (player instanceof ServerPlayer sp) {
            split(sp);
         }

         return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
      }
   }

   public static boolean split(ServerPlayer player) {
      ItemStack stack = player.getMainHandItem();
      if (stack.getItem() instanceof MoriaScissorsItem && !isSplit(stack) && player.getOffhandItem().isEmpty()) {
         stack.set((DataComponentType)KageItems.SPLIT.get(), true);
         stack.remove((DataComponentType)KageItems.OFF_HAND_NEXT.get());
         player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack((ItemLike)KageItems.MORIA_BLADE.get()));
         player.level().playSound(null, player.getX(), player.getY(), player.getZ(), KageSounds.SCISSORS_SPLIT.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
         return true;
      } else {
         return false;
      }
   }

   public static void join(ServerPlayer player) {
      boolean any = false;
      Inventory inv = player.getInventory();

      for (int i = 0; i < inv.getContainerSize(); i++) {
         ItemStack s = inv.getItem(i);
         if (s.getItem() instanceof MoriaBladeItem) {
            inv.setItem(i, ItemStack.EMPTY);
            any = true;
         } else if (s.getItem() instanceof MoriaScissorsItem && isSplit(s)) {
            s.remove((DataComponentType)KageItems.SPLIT.get());
            s.remove((DataComponentType)KageItems.OFF_HAND_NEXT.get());
            any = true;
         }
      }

      if (player.containerMenu.getCarried().getItem() instanceof MoriaBladeItem) {
         player.containerMenu.setCarried(ItemStack.EMPTY);
         any = true;
      }

      if (any) {
         player.level().playSound(null, player.getX(), player.getY(), player.getZ(), KageSounds.SCISSORS_JOIN.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
      }
   }

   public static InteractionHand nextHand(LivingEntity entity) {
      Map<LivingEntity, Boolean> turns = entity.level().isClientSide() ? CLIENT_TURN : SERVER_TURN;
      boolean off = turns.getOrDefault(entity, false);
      turns.put(entity, !off);
      return off ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
   }

   public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
      if (hand == InteractionHand.MAIN_HAND
         && isSplit(stack)
         && entity.getOffhandItem().getItem() instanceof MoriaBladeItem
         && (!entity.level().isClientSide() || entity instanceof Player p && p.isLocalPlayer())
         && (!entity.swinging || entity.swingTime < 0 || entity.swingTime >= 3)) {
         InteractionHand turn = nextHand(entity);
         if (entity instanceof ServerPlayer player) {
            ServerFx.animate(player, KageKage.id(turn == InteractionHand.OFF_HAND ? "dual_swing_left" : "dual_swing_right"));
         }

         if (turn == InteractionHand.MAIN_HAND) {
            return false;
         } else {
            entity.swing(InteractionHand.OFF_HAND, false);
            return true;
         }
      } else {
         return false;
      }
   }

   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
      if (!level.isClientSide()
         && isSplit(stack)
         && entity instanceof ServerPlayer player
         && (!selected || player.getMainHandItem() != stack || !(player.getOffhandItem().getItem() instanceof MoriaBladeItem))) {
         join(player);
      }
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(
         Component.translatable(isSplit(stack) ? "item.devilfruits.moria_scissors.split" : "item.devilfruits.moria_scissors.joined")
            .withStyle(ChatFormatting.GRAY)
      );
      tooltip.add(Component.translatable("item.devilfruits.moria_scissors.hint").withStyle(ChatFormatting.DARK_PURPLE));
   }
}
