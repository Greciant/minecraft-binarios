package com.awakenedmod.awakened.client.limitless.domain;

import com.awakenedmod.awakened.client.ClientConfig;
import com.awakenedmod.awakened.client.ForcedCamera;
import com.awakenedmod.awakened.client.voidview.VoidIntroMode;
import com.awakenedmod.awakened.content.limitless.domain.VoidAnchorEntity;
import com.awakenedmod.awakened.content.limitless.domain.VoidBody;
import com.awakenedmod.awakened.core.AwId;
import javax.annotation.Nullable;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class VoidIntro {
   public static final ResourceLocation LAYER = AwId.of("limitless_void_intro");
   private static CameraType saved;
   private static boolean forced;
   private static int toned = -1;

   private VoidIntro() {
   }

   static void register(IEventBus gameBus) {
      gameBus.addListener(VoidIntro::onTick);
      gameBus.addListener(VoidIntro::onGuiLayer);
      gameBus.addListener(VoidIntro::onLoggingOut);
   }

   public static int length(VoidIntroMode mode) {
      return mode == VoidIntroMode.SHORT ? 30 : 44;
   }

   @Nullable
   public static VoidIntro.State current(float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer local = mc.player;
      if (local != null && !VoidClient.anchors().isEmpty()) {
         VoidBody body = VoidBody.of(local);
         if (!body.inVoid()) {
            return null;
         } else {
            VoidAnchorEntity a = VoidClient.anchor(body.anchor());
            if (a == null) {
               return null;
            } else {
               VoidIntroMode mode = (VoidIntroMode)ClientConfig.VOID_INTRO.get();
               float t = a.since(partialTick);
               return t >= 0.0F && t < (float)length(mode) ? new VoidIntro.State(a, t, mode) : null;
            }
         }
      } else {
         return null;
      }
   }

   private static void onTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      VoidIntro.State s = mc.player == null ? null : current(0.0F);
      if (s != null && toned != s.anchor().getId()) {
         toned = s.anchor().getId();
         VoidTone.play(mc, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 0.5F);
      }

      boolean shot = s != null && s.camera() && !ForcedCamera.active();
      if (shot) {
         if (!forced) {
            saved = mc.options.getCameraType();
            forced = true;
         }

         if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
         }
      } else {
         restore(mc);
      }
   }

   private static void restore(Minecraft mc) {
      if (forced) {
         forced = false;
         if (saved != null) {
            mc.options.setCameraType(saved);
         }

         saved = null;
      }
   }

   static boolean shotActive() {
      return forced;
   }

   private static void onGuiLayer(Pre event) {
      ResourceLocation name = event.getName();
      if (!name.equals(LAYER) && !name.equals(VanillaGuiLayers.CHAT)) {
         if (current(0.0F) != null) {
            event.setCanceled(true);
         }
      }
   }

   private static void onLoggingOut(LoggingOut event) {
      restore(Minecraft.getInstance());
      toned = -1;
   }

   public static record State(VoidAnchorEntity anchor, float t, VoidIntroMode mode) {
      public int length() {
         return VoidIntro.length(this.mode);
      }

      public boolean camera() {
         return this.mode.movesCamera();
      }
   }
}
