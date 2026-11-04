package com.devilfruits.fruits.bomu_bomu.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.animation.AnimationController;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.fx.ChargeVisuals;
import com.devilfruits.fruits.bomu_bomu.BomuAbilities;
import com.devilfruits.fruits.bomu_bomu.BomuBomu;
import com.devilfruits.fruits.bomu_bomu.BomuEntities;
import com.devilfruits.fruits.bomu_bomu.BomuItems;
import com.devilfruits.fruits.bomu_bomu.client.dev.BomuCapture;
import com.devilfruits.fruits.bomu_bomu.item.BombPistolItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

public final class BomuClient {
   public static void init(IEventBus modBus) {
      BomuClientFx.register();
      ChargeVisuals.register(BomuAbilities.SELF_DESTRUCT.id(), BomuClientEvents.SELF_DESTRUCT);
      AnimationController.registerUseAnimation(s -> s.getItem() instanceof BombPistolItem, BomuBomu.id("pistol_load"), BomuBomu.id("pistol_load_left"));
      modBus.addListener(e -> e.registerAbove(VanillaGuiLayers.CROSSHAIR, BomuBomu.id("status"), BomuHud::render));
      modBus.addListener(e -> {
         e.registerEntityRenderer(BomuEntities.BOOGER.get(), BomuRenderers.Booger::new);
         e.registerEntityRenderer(BomuEntities.BULLET.get(), BomuRenderers.Bullet::new);
      });
      modBus.addListener(e -> e.registerItem(BombPistolClient.EXTENSIONS, new Item[]{(Item)BomuItems.BOMB_PISTOL.get()}));
      modBus.addListener(e -> e.enqueueWork(BombPistolClient::registerProperties));
      NeoForge.EVENT_BUS.addListener(BomuClientEvents::onClientTick);
      NeoForge.EVENT_BUS.addListener(BomuClientEvents::onJoin);
      NeoForge.EVENT_BUS.addListener(BomuClientEvents::onRenderPlayer);
      NeoForge.EVENT_BUS.addListener(BomuClientEvents::onRenderHand);
      NeoForge.EVENT_BUS.addListener(BomuClientEvents::onFov);
      NeoForge.EVENT_BUS.addListener(e -> BomuClientEvents.onLogout());
      AutoTest.addScript(BomuBomu.ID, new BomuCapture(false));
      AutoTest.addScript(DevilFruits.id("vfx_bomu"), new BomuCapture(true));
      AutoTest.addScript(DevilFruits.id("vfx_bomu_pistol"), new BomuCapture(true, true));
   }

   private BomuClient() {
   }
}
