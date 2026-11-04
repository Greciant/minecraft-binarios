package com.devilfruits.fruits.doru_doru.client;

import com.devilfruits.client.ClientFruitState;
import com.devilfruits.client.input.DFKeys;
import com.devilfruits.core.form.FormAbility;
import com.devilfruits.fruits.doru_doru.DoruAbilities;
import com.devilfruits.fruits.doru_doru.DoruItems;
import com.devilfruits.fruits.doru_doru.atelier.DoruNetwork;
import com.devilfruits.fruits.doru_doru.client.render.WaxCoatingLayer;
import com.devilfruits.fruits.doru_doru.coating.WaxCoating;
import com.devilfruits.fruits.doru_doru.item.WaxTools;
import com.devilfruits.fruits.doru_doru.wax.WaxPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.ClientTickEvent.Pre;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item;
import net.neoforged.neoforge.network.PacketDistributor;

public final class DoruClientEvents {
   private static final int SHAKE_WINDOW = 10;
   private static boolean useWasDown;
   private static int struggleCooldown;
   private static int lastSide;
   private static int sideTicks;

   static void onClientTickEnd(Post event) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player == null) {
         useWasDown = false;
      } else {
         boolean use = DFKeys.USE.isDown();
         if (use && !useWasDown && mc.screen == null) {
            FormAbility selected = ClientFruitState.selectedAbility();
            if (selected != null
               && selected.ability() == DoruAbilities.TOOLS
               && !WaxTools.isWaxTool(player.getMainHandItem())
               && !ClientFruitState.isLocked(DoruAbilities.TOOLS)) {
               mc.setScreen(new WaxToolWheelScreen());
            }
         }

         useWasDown = use;
      }
   }

   static void onClientTick(Pre event) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null) {
         if (struggleCooldown > 0) {
            struggleCooldown--;
         }

         shake(player, WaxCoating.get(player));
         if (WaxCoating.get(player).encased(player.level().getGameTime())) {
            boolean struggled = false;

            while (mc.options.keyJump.consumeClick()) {
               struggled = true;
            }

            while (mc.options.keyAttack.consumeClick()) {
               struggled = true;
            }

            if (struggled && struggleCooldown == 0) {
               PacketDistributor.sendToServer(new DoruNetwork.StrugglePayload(), new CustomPacketPayload[0]);
               struggleCooldown = 4;
               player.swing(InteractionHand.MAIN_HAND);
            }
         }
      }
   }

   private static void shake(LocalPlayer player, WaxCoating.Data d) {
      if ((d.stacks() > 0 || d.encased(player.level().getGameTime())) && Minecraft.getInstance().screen == null) {
         float strafe = player.input.leftImpulse;
         int side = strafe > 0.3F ? 1 : (strafe < -0.3F ? -1 : 0);
         sideTicks++;
         if (side != 0) {
            if (lastSide == -side && sideTicks <= 10) {
               PacketDistributor.sendToServer(new DoruNetwork.ShakePayload(), new CustomPacketPayload[0]);
            }

            lastSide = side;
            sideTicks = 0;
         }
      } else {
         lastSide = 0;
      }
   }

   static void onItemColors(Item event) {
      event.register(
         (stack, tint) -> {
            if (tint != 0) {
               return -1;
            } else {
               WaxTools.Bound b = (WaxTools.Bound)stack.get((DataComponentType)DoruItems.BOUND.get());
               return 0xFF000000 | WaxPalette.tint(b == null ? 0 : b.color(), false);
            }
         },
         new ItemLike[]{
            (ItemLike)DoruItems.WAX_PICKAXE.get(),
            (ItemLike)DoruItems.WAX_SHOVEL.get(),
            (ItemLike)DoruItems.WAX_AXE.get(),
            (ItemLike)DoruItems.WAX_HOE.get(),
            (ItemLike)DoruItems.WAX_SWORD.get(),
            (ItemLike)DoruItems.WAX_LANCE.get(),
            (ItemLike)DoruItems.WAX_KEY.get()
         }
      );
   }

   static void addLayers(AddLayers event) {
      for (Model skin : event.getSkins()) {
         if (event.getSkin(skin) instanceof LivingEntityRenderer r) {
            r.addLayer(new WaxCoatingLayer(r));
         }
      }

      for (EntityType<?> type : event.getEntityTypes()) {
         if (event.getRenderer(type) instanceof LivingEntityRenderer r) {
            r.addLayer(new WaxCoatingLayer(r));
         }
      }
   }

   private DoruClientEvents() {
   }
}
