package com.devilfruits.fruits.kage_kage.client;

import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.fruits.kage_kage.KageKage;
import com.devilfruits.fruits.kage_kage.KageNetwork;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;

public final class KageKeys {
   public static final KeyMapping SWAP = new KeyMapping(
      "key.devilfruits.kage_swap", KageKeys.KageKeyContext.INSTANCE, Type.KEYSYM, 89, "key.categories.devilfruits"
   );

   public static void register(RegisterKeyMappingsEvent event) {
      event.register(SWAP);
   }

   public static void tick(Post event) {
      Minecraft mc = Minecraft.getInstance();

      while (SWAP.consumeClick()) {
         if (mc.player != null && mc.screen == null && !(Boolean)CoreClientConfig.FRUIT_LOCKED.get() && KageKage.isUser(mc.player)) {
            PacketDistributor.sendToServer(new KageNetwork.SwapPayload(), new CustomPacketPayload[0]);
         }
      }
   }

   private KageKeys() {
   }

   private static enum KageKeyContext implements IKeyConflictContext {
      INSTANCE;

      public boolean isActive() {
         Minecraft mc = Minecraft.getInstance();
         return KeyConflictContext.IN_GAME.isActive() && mc.player != null && KageKage.isUser(mc.player);
      }

      public boolean conflicts(IKeyConflictContext other) {
         return other == this || other == KeyConflictContext.IN_GAME;
      }
   }
}
