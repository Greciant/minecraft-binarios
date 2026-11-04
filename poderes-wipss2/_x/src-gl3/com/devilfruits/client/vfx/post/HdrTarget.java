package com.devilfruits.client.vfx.post;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;

public final class HdrTarget extends RenderTarget {
   public HdrTarget(int width, int height) {
      super(false);
      this.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      this.resize(Math.max(1, width), Math.max(1, height), Minecraft.ON_OSX);
   }

   public void createBuffers(int width, int height, boolean clearError) {
      super.createBuffers(width, height, clearError);
      GlStateManager._bindTexture(this.colorTextureId);
      GlStateManager._texImage2D(3553, 0, 34842, this.width, this.height, 0, 6408, 5126, null);
      this.setFilterMode(9729);
      this.clear(clearError);
   }
}
