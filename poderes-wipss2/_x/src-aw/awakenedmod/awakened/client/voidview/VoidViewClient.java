package com.awakenedmod.awakened.client.voidview;

import com.awakenedmod.awakened.client.compat.ShaderPacks;
import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.core.voidview.VoidOccluderEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent.OverlayType;
import net.neoforged.neoforge.client.event.RenderHighlightEvent.Block;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeFogColor;
import net.neoforged.neoforge.client.event.ViewportEvent.RenderFog;
import net.neoforged.neoforge.common.util.TriState;

public final class VoidViewClient {
   private static final float FOG_FAR = 1024.0F;
   private static ClientLevel level;
   private static final List<VoidOccluderEntity> OCCLUDERS = new ArrayList<>();
   private static final int OCCLUDER_ID = -6320128;
   private static boolean culledOff;
   private static boolean shadowsOff;

   private VoidViewClient() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(VoidViewClient::onTick);
      gameBus.addListener(EventPriority.LOWEST, VoidViewClient::onFog);
      gameBus.addListener(EventPriority.LOWEST, VoidViewClient::onFogColor);
      gameBus.addListener(VoidViewClient::onScreenEffect);
      gameBus.addListener(VoidViewClient::onHighlight);
      gameBus.addListener(VoidViewClient::onStage);
      gameBus.addListener(VoidViewClient::onNameTag);
      VoidViewRenderer.register(gameBus);
   }

   private static void onTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != level) {
         VoidView.clear();
         OCCLUDERS.clear();
         level = mc.level;
      }

      occluders(mc);
      boolean hide = VoidView.hidesWorld();
      if (hide && mc.smartCull) {
         mc.smartCull = false;
         culledOff = true;
         if (mc.levelRenderer != null) {
            mc.levelRenderer.needsUpdate();
         }
      } else if (!hide && culledOff) {
         mc.smartCull = true;
         culledOff = false;
         if (mc.levelRenderer != null) {
            mc.levelRenderer.needsUpdate();
         }
      }
   }

   private static void occluders(Minecraft mc) {
      List<VoidView.Sphere> spheres = (List<VoidView.Sphere>)(mc.level != null && ShaderPacks.active() ? new ArrayList<>(VoidView.spheres()) : List.of());

      while (OCCLUDERS.size() > spheres.size()) {
         VoidOccluderEntity gone = OCCLUDERS.remove(OCCLUDERS.size() - 1);
         if (mc.level != null) {
            mc.level.removeEntity(gone.getId(), RemovalReason.DISCARDED);
         }
      }

      for (int i = 0; i < spheres.size(); i++) {
         VoidView.Sphere s = spheres.get(i);
         if (i == OCCLUDERS.size()) {
            VoidOccluderEntity e = new VoidOccluderEntity(AwakenedEntities.VOID_OCCLUDER.get(), mc.level);
            e.setId(-6320128 - i);
            e.setPos(s.centre().x, s.centre().y, s.centre().z);
            e.setOldPosAndRot();
            mc.level.addEntity(e);
            OCCLUDERS.add(e);
         }

         VoidOccluderEntity e = OCCLUDERS.get(i);
         e.setRadius((float)s.radius());
         if (e.position().distanceToSqr(s.centre()) > 1.0E-6) {
            e.setPos(s.centre().x, s.centre().y, s.centre().z);
            e.setOldPosAndRot();
         }
      }
   }

   private static void onFog(RenderFog event) {
      if (VoidView.hidesWorld()) {
         event.setFarPlaneDistance(1024.0F);
         event.setNearPlaneDistance(1023.0F);
         event.setCanceled(true);
      }
   }

   private static void onFogColor(ComputeFogColor event) {
      if (VoidView.hidesWorld()) {
         event.setRed(0.0F);
         event.setGreen(0.0F);
         event.setBlue(0.0F);
      }
   }

   private static void onScreenEffect(RenderBlockScreenEffectEvent event) {
      if (event.getOverlayType() != OverlayType.FIRE && VoidView.hidesWorld()) {
         event.setCanceled(true);
      }
   }

   private static void onNameTag(RenderNameTagEvent event) {
      Entity e = event.getEntity();
      if (VoidView.any() && VoidView.separated(VoidView.cameraPos(), e.getX(), e.getY(0.5), e.getZ())) {
         event.setCanRender(TriState.FALSE);
      }
   }

   private static void onHighlight(Block event) {
      if (VoidView.hidesWorld()) {
         event.setCanceled(true);
      }
   }

   private static void onStage(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_SKY) {
         Minecraft mc = Minecraft.getInstance();
         boolean hide = VoidView.hidesWorld();
         if (hide) {
            mc.getEntityRenderDispatcher().setRenderShadow(false);
            shadowsOff = true;
         } else if (shadowsOff) {
            mc.getEntityRenderDispatcher().setRenderShadow(true);
            shadowsOff = false;
         }
      }
   }
}
