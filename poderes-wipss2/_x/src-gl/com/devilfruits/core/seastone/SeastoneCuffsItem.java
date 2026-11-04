package com.devilfruits.core.seastone;

import com.devilfruits.core.poneglyph.PoneglyphConfig;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;

public class SeastoneCuffsItem extends Item {
   public SeastoneCuffsItem(Properties properties) {
      super(properties);
   }

   public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
      if (player.level().isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         int ticks = (Integer)PoneglyphConfig.CUFFS_TICKS.get();
         target.addEffect(new MobEffectInstance(SeastoneGear.SUPPRESSED, ticks, 0, false, true, true), player);
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2, false, false, false), player);
         target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1, false, false, false), player);
         target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 1, false, false, false), player);
         player.level().playSound(null, target.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 0.7F);
         player.level().playSound(null, target.blockPosition(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 0.8F, 1.3F);
         stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
         return InteractionResult.CONSUME;
      }
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.devilfruits.seastone_cuffs.desc").withStyle(ChatFormatting.GRAY));
   }
}
