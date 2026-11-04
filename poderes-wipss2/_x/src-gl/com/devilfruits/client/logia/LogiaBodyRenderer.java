package com.devilfruits.client.logia;

import com.devilfruits.client.render.FormRenderers;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre;

@EventBusSubscriber(
   modid = "devilfruits",
   value = {Dist.CLIENT}
)
public final class LogiaBodyRenderer {
   @SubscribeEvent
   public static void onClientTick(Post event) {
      LogiaRenderState.tickAll();
   }

   @SubscribeEvent
   public static void onLogout(LoggingOut event) {
      LogiaRenderState.clear();
   }

   @SubscribeEvent
   public static void onRenderPlayer(Pre event) {
      if (event.getEntity() instanceof AbstractClientPlayer player) {
         LogiaRenderState state = LogiaRenderState.get(player);
         if (state != null && state.hidden()) {
            event.setCanceled(true);
            FormRenderers.renderNameTag(player, event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
            LogiaThemes.of(player)
               .hooks()
               .renderElemental(player, state, event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
         }
      }
   }

   @Nullable
   public static ResourceLocation maskedSkin(AbstractClientPlayer player) {
      LogiaRenderState state = LogiaRenderState.get(player);
      return state == null ? null : state.maskedSkin(player);
   }

   @Nullable
   public static VertexConsumer firstPersonArm(AbstractClientPlayer player, MultiBufferSource buffers, boolean sleeve) {
      ResourceLocation skin = maskedSkin(player);
      return skin == null ? null : buffers.getBuffer(sleeve ? RenderType.entityTranslucent(skin) : RenderType.entityCutoutNoCull(skin));
   }

   private LogiaBodyRenderer() {
   }
}
