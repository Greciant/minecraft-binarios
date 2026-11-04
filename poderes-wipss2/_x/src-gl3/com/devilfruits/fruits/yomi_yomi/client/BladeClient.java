package com.devilfruits.fruits.yomi_yomi.client;

import com.devilfruits.client.fx.ChargeVisuals;
import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.model.geo.GeoOverlays;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.core.network.ChargePayload;
import com.devilfruits.fruits.yomi_yomi.YomiAbilities;
import com.devilfruits.fruits.yomi_yomi.YomiFx;
import com.devilfruits.fruits.yomi_yomi.YomiItems;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.fruits.yomi_yomi.client.vfx.Afterimages;
import com.devilfruits.fruits.yomi_yomi.client.vfx.AubadeCinematic;
import com.devilfruits.fruits.yomi_yomi.client.vfx.ColdCinematic;
import com.devilfruits.fruits.yomi_yomi.client.vfx.Frost;
import com.devilfruits.fruits.yomi_yomi.client.vfx.FubukiCinematic;
import com.devilfruits.fruits.yomi_yomi.client.vfx.GavotteCinematic;
import com.devilfruits.fruits.yomi_yomi.client.vfx.PolkaCinematic;
import com.devilfruits.fruits.yomi_yomi.client.vfx.YomiRender;
import com.devilfruits.fruits.yomi_yomi.frost.YomiFrost;
import com.devilfruits.fruits.yomi_yomi.item.SoulCaneItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;

public final class BladeClient {
   private static final double POLKA_TURN = 0.22;
   private static boolean flurry;

   static void init(IEventBus modBus) {
      modBus.addListener(e -> e.registerItem(new IClientItemExtensions() {
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
               return SoulCaneRenderer.get();
            }
         }, new Item[]{(Item)YomiItems.SOUL_CANE.get(), (Item)YomiItems.SCABBARD.get()}));
      modBus.addListener(BladeClient::addLayers);
      GeoOverlays.register(FrostLayer::geo);
      NeoForge.EVENT_BUS.addListener(e -> SoulCaneRenderer.onRenderLiving(e));
      NeoForge.EVENT_BUS.addListener(e -> SoulCaneRenderer.onRenderLivingPost(e));
      NeoForge.EVENT_BUS.addListener(e -> Afterimages.onRenderPlayer(e));
      NeoForge.EVENT_BUS.addListener(e -> YomiRender.onRenderStage(e));
      NeoForge.EVENT_BUS.addListener(BladeClient::onTurn);
      NeoForge.EVENT_BUS.addListener(BladeClient::onLevelTick);
      NeoForge.EVENT_BUS.addListener(e -> {
         SoulCaneRenderer.clear();
         Afterimages.clear();
         YomiRender.clear();
         flurry = false;
      });
      ClientFxRegistry.register(YomiFx.AUBADE, (level, entity, p) -> {
         if (entity != null) {
            VfxDirector.spawn(new AubadeCinematic(level, entity, p.pos(), p.dir(), p.power(), p.seed(), p.extra()));
         }
      });
      ClientFxRegistry.register(YomiFx.GAVOTTE_LEAP, (level, entity, p) -> {
         if (entity != null) {
            VfxDirector.spawn(GavotteCinematic.leap(entity, p.pos(), p.seed()));
         }
      });
      ClientFxRegistry.register(YomiFx.GAVOTTE_SWEEP, (level, entity, p) -> {
         if (entity != null) {
            VfxDirector.spawn(GavotteCinematic.sweep(entity, p.pos(), p.power(), p.seed()));
         }
      });
      ClientFxRegistry.register(YomiFx.FUBUKI_DASH, (level, entity, p) -> {
         if (entity != null) {
            SoulCaneRenderer.fubuki(entity.getId());
            VfxDirector.spawn(FubukiCinematic.dash(entity, p.pos(), p.dir(), p.extra(), p.seed()));
         }
      });
      ClientFxRegistry.register(YomiFx.FUBUKI_BURST, (level, entity, p) -> {
         if (entity != null) {
            VfxDirector.spawn(FubukiCinematic.burst(level, entity, p.pos(), p.dir(), p.power(), p.seed()));
         }
      });
      ClientFxRegistry.register(YomiFx.POLKA, (level, entity, p) -> {
         if (entity != null) {
            VfxDirector.spawn(new PolkaCinematic(entity, p.pos(), p.power(), p.extra(), p.seed()));
         }
      });
      ClientFxRegistry.register(YomiFx.POLKA_HIT, (level, entity, p) -> PolkaCinematic.hit(entity, p.pos(), p.dir(), p.extra() != 0, p.seed()));
      ClientFxRegistry.register(YomiFx.POLKA_END, (level, entity, p) -> stopFlurry(entity));
      ClientFxRegistry.register(YomiFx.CHILL_BURST, (level, entity, p) -> {
         if (entity != null) {
            VfxDirector.spawn(ColdCinematic.burst(level, entity, p.pos(), p.power(), p.seed()));
         }
      });
      ClientFxRegistry.register(YomiFx.FREEZE, (level, entity, p) -> {
         if (entity != null) {
            VfxDirector.spawn(ColdCinematic.freeze(level, entity, p.power(), p.extra(), p.seed()));
         }
      });
      ChargeVisuals.register(YomiAbilities.POLKA.id(), new ChargeVisuals.ChargeVisual() {
         @Override
         public void start(ClientLevel level, Entity entity, ChargePayload payload) {
            if (entity == Minecraft.getInstance().player) {
               BladeClient.flurry = true;
            }
         }

         @Override
         public void stop(ClientLevel level, Entity entity, boolean released) {
            BladeClient.stopFlurry(entity);
         }
      });
   }

   private static void stopFlurry(Entity entity) {
      if (entity != null) {
         if (entity == Minecraft.getInstance().player) {
            flurry = false;
         }

         PolkaCinematic c = VfxDirector.find(PolkaCinematic.class, k -> k.entityId() == entity.getId());
         if (c != null) {
            c.stop();
         }
      }
   }

   private static void addLayers(AddLayers event) {
      for (Model skin : event.getSkins()) {
         if (event.getSkin(skin) instanceof LivingEntityRenderer r) {
            r.addLayer(new FrostLayer(r));
         }
      }

      for (EntityType<?> type : event.getEntityTypes()) {
         if (event.getRenderer(type) instanceof LivingEntityRenderer r) {
            r.addLayer(new FrostLayer(r));
         }
      }
   }

   private static void onTurn(CalculatePlayerTurnEvent event) {
      if (flurry) {
         double d = event.getMouseSensitivity() * 0.6 + 0.2;
         double scaled = d * Math.cbrt(0.22);
         event.setMouseSensitivity((scaled - 0.2) / 0.6);
      }
   }

   private static void onLevelTick(Post event) {
      if (event.getLevel() instanceof ClientLevel level && level.getGameTime() % 4L == 0L) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null) {
            return;
         }

         long now = level.getGameTime();
         RandomSource r = level.getRandom();

         for (Entity e : level.entitiesForRendering()) {
            if (e instanceof LivingEntity) {
               LivingEntity living = (LivingEntity)e;
               if (e.hasData(YomiFrost.TYPE) && !(e.distanceToSqr(mc.player) > 1024.0)) {
                  YomiFrost.Data d = YomiFrost.get(e);
                  if ((d.cold(now) || d.chillAt(now) >= 2) && (now / 4L + (long)e.getId()) % 5L == 0L) {
                     Vec3 look = living.getLookAngle();
                     Frost.breath(r, living.getEyePosition().add(look.scale((double)living.getBbWidth() * 0.55)).add(0.0, -0.08, 0.0), look);
                  }

                  if ((d.frozen(now) || d.cold(now)) && r.nextInt(3) == 0) {
                     Frost.snow(r, living.position().add(0.0, (double)living.getBbHeight() * 0.7, 0.0), (double)living.getBbWidth() * 0.5, 1);
                  }
               }
            }
         }

         return;
      }
   }

   static void hint(GuiGraphics g, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null && !mc.options.hideGui && YomiYomi.isUser(player) && !YomiYomi.isSoul(player)) {
         String key = null;
         if (player.getMainHandItem().is((Item)YomiItems.SOUL_CANE.get()) && !SoulCaneItem.isDrawn(player.getMainHandItem())) {
            key = "hud.devilfruits.yomi_yomi.draw_hint";
         } else if (SoulCaneItem.isDrawn(player.getMainHandItem()) && !SoulCaneItem.ready(player)) {
            key = "hud.devilfruits.yomi_yomi.scabbard_hint";
         }

         if (key != null) {
            g.drawCenteredString(mc.font, Component.translatable(key), g.guiWidth() / 2, g.guiHeight() - 72, 13172586);
         }
      }
   }

   private BladeClient() {
   }
}
