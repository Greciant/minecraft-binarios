package com.awakenedmod.awakened.content.scaler;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.PowerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingVisibilityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;

@EventBusSubscriber(
   modid = "awakened"
)
public final class ScalerEvents {
   private ScalerEvents() {
   }

   @SubscribeEvent
   public static void onPlayerTick(Post event) {
      if (event.getEntity() instanceof ServerPlayer sp) {
         JarControl.tick(sp);
      }
   }

   @SubscribeEvent
   public static void onVisibility(LivingVisibilityEvent event) {
      LivingEntity seen = event.getEntity();
      if (Scaling.isSpeck(seen)) {
         event.modifyVisibility(0.0);
      } else if (Scaling.isMite(seen)) {
         event.modifyVisibility(0.3);
      }
   }

   @SubscribeEvent
   public static void onIncomingDamage(LivingIncomingDamageEvent event) {
      float mult = RayEffects.damageTakenMult(event.getEntity());
      if (mult != 1.0F) {
         event.setAmount(event.getAmount() * mult);
      }
   }

   @SubscribeEvent
   public static void onKnockback(LivingKnockBackEvent event) {
      if (event.getEntity() instanceof ServerPlayer victim) {
         PowerData var4 = AwakenedAttachments.get(victim);
         if ("scaler".equals(var4.powerId())) {
            float mult = Scaling.stage(var4).knockbackTakenMult();
            if (mult != 1.0F) {
               event.setStrength(event.getStrength() * mult);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onRightClickBlock(RightClickBlock event) {
      Player p = event.getEntity();
      if (!(p.getScale() > 0.15F)) {
         BlockState state = event.getLevel().getBlockState(event.getPos());
         if (isMechanismOutOfReach(state)) {
            event.setCanceled(true);
            if (p instanceof ServerPlayer sp) {
               sp.displayClientMessage(Component.translatable("awakened.scaler.too_small"), true);
            }
         }
      }
   }

   private static boolean isMechanismOutOfReach(BlockState state) {
      return state.is(BlockTags.BUTTONS)
         || state.is(Blocks.LEVER)
         || state.is(BlockTags.DOORS)
         || state.is(BlockTags.TRAPDOORS)
         || state.is(BlockTags.FENCE_GATES);
   }
}
