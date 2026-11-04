package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.chrono.ChronoStasis;
import com.awakenedmod.awakened.client.chrono.stasis.PropCanvas;
import com.awakenedmod.awakened.client.chrono.stasis.StasisProp;
import com.awakenedmod.awakened.client.chrono.stasis.StasisStyles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLivingEvent.Post;

public final class StasisPropRenderer {
   private StasisPropRenderer() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(StasisPropRenderer::onRenderPost);
   }

   private static void onRenderPost(Post<?, ?> event) {
      LivingEntity entity = event.getEntity();
      if (!entity.isInvisible()) {
         StasisProp prop = ChronoStasis.frame(entity);
         if (prop != null) {
            MultiBufferSource buffers = event.getMultiBufferSource();
            BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
            PropCanvas canvas = new PropCanvas(
               event.getPoseStack(),
               (pose, block, light) -> blocks.renderSingleBlock(block, pose, buffers, light, OverlayTexture.NO_OVERLAY),
               event.getPackedLight()
            );
            StasisStyles.look().draw(prop, canvas);
         }
      }
   }
}
