package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.MoveKeys;
import com.awakenedmod.awakened.core.net.MoveKeysPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(
   modid = "awakened",
   value = {Dist.CLIENT}
)
public final class MoveKeysRelay {
   private static final int REMIND_TICKS = 20;
   private static MoveKeys.Keys lastSent = MoveKeys.Keys.NONE;
   private static Player lastPlayer;
   private static int sinceSent;

   private MoveKeysRelay() {
   }

   @SubscribeEvent
   public static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      ClientPacketListener connection = mc.getConnection();
      if (mc.player != null && connection != null && connection.hasChannel(MoveKeysPayload.TYPE)) {
         if (mc.player != lastPlayer) {
            lastPlayer = mc.player;
            lastSent = MoveKeys.Keys.NONE;
         }

         MoveKeys.Keys now = new MoveKeys.Keys(
            mc.options.keyUp.isDown(), mc.options.keyDown.isDown(), mc.options.keyLeft.isDown(), mc.options.keyRight.isDown(), mc.options.keyJump.isDown()
         );
         sinceSent++;
         boolean held = now.moving() || now.jump();
         if (!now.equals(lastSent) || held && sinceSent >= 20) {
            PacketDistributor.sendToServer(new MoveKeysPayload(now), new CustomPacketPayload[0]);
            lastSent = now;
            sinceSent = 0;
         }
      }
   }
}
