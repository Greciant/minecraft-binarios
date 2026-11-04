package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.exosavant.WarhammerThrowEntity;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;

public final class WarhammerThrowRenderer extends ThrownItemRenderer<WarhammerThrowEntity> {
   public WarhammerThrowRenderer(Context context) {
      super(context, 1.5F, false);
   }
}
