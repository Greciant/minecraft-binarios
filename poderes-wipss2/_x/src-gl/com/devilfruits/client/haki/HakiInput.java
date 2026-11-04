package com.devilfruits.client.haki;

import com.devilfruits.client.input.DFKeys;
import com.devilfruits.core.haki.HakiManager;
import com.devilfruits.core.network.HakiInputPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Pre;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(
   modid = "devilfruits",
   value = {Dist.CLIENT}
)
public final class HakiInput {
   private static boolean ryuoHeld;

   @SubscribeEvent
   public static void onClientTick(Pre event) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player == null) {
         ryuoHeld = false;
      } else {
         while (DFKeys.HAKI_COAT.consumeClick()) {
            if (player.isShiftKeyDown()) {
               mc.setScreen(new HakiScreen());
            } else if (HakiManager.unlocked(player)) {
               PacketDistributor.sendToServer(HakiInputPayload.of(HakiInputPayload.Action.TOGGLE), new CustomPacketPayload[0]);
            } else {
               player.displayClientMessage(Component.translatable("haki.devilfruits.message.not_unlocked"), true);
            }
         }

         boolean down = DFKeys.HAKI_RYUO.isDown() && mc.screen == null && HakiManager.unlocked(player);
         if (down != ryuoHeld) {
            ryuoHeld = down;
            if (down && !HakiManager.data(player).tier().ryuo()) {
               player.displayClientMessage(Component.translatable("haki.devilfruits.message.ryuo_locked"), true);
            } else {
               PacketDistributor.sendToServer(
                  HakiInputPayload.of(down ? HakiInputPayload.Action.RYUO_START : HakiInputPayload.Action.RYUO_RELEASE), new CustomPacketPayload[0]
               );
            }
         }
      }
   }

   private HakiInput() {
   }
}
