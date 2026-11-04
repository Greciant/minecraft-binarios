package com.devilfruits.client;

import com.devilfruits.client.hud.FruitInfoScreen;
import com.devilfruits.core.fruit.FruitRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class ClientHooks {
   public static void openEncyclopedia(ResourceLocation fruit) {
      Minecraft.getInstance().setScreen(new FruitInfoScreen(FruitRegistry.get(fruit)));
   }

   private ClientHooks() {
   }
}
