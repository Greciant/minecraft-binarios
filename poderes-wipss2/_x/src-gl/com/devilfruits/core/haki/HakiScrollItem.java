package com.devilfruits.core.haki;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.fx.ServerFx;
import java.util.List;
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
import net.minecraft.world.phys.Vec3;

public class HakiScrollItem extends Item {
   private final boolean master;

   public HakiScrollItem(boolean master, Properties properties) {
      super(properties);
      this.master = master;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!(player instanceof ServerPlayer sp)) {
         return InteractionResultHolder.success(stack);
      } else {
         boolean changed;
         if (this.master) {
            changed = HakiManager.level(sp) < 10;
            if (changed) {
               HakiManager.setLevel(sp, 10);
               sp.sendSystemMessage(Component.translatable("haki.devilfruits.message.mastered").withStyle(ChatFormatting.GRAY));
            }
         } else if (HakiManager.unlocked(sp)) {
            float xp = ((Double)HakiConfig.SCROLL_REPEAT_XP.get()).floatValue();
            changed = HakiManager.level(sp) < 10 && xp > 0.0F;
            if (changed) {
               HakiManager.addXp(sp, xp);
               HakiManager.sync(sp);
               sp.displayClientMessage(Component.translatable("haki.devilfruits.message.scroll_xp", new Object[]{Math.round(xp)}), true);
            }
         } else {
            changed = HakiManager.unlock(sp);
         }

         if (!changed) {
            sp.displayClientMessage(Component.translatable("haki.devilfruits.message.already"), true);
            return InteractionResultHolder.fail(stack);
         } else {
            ServerFx.animate(sp, DevilFruits.id("haki/scroll"));
            ServerFx.sendNear(sp.serverLevel(), HakiFx.UNLOCK, sp.getId(), sp.position(), Vec3.ZERO, this.master ? 2.0F : 1.0F, 0L, 1, 48.0);
            stack.consume(1, player);
            return InteractionResultHolder.consume(stack);
         }
      }
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable(this.getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
   }
}
