package com.devilfruits.compat.sable.mixin;

import com.devilfruits.fruits.suke_suke.clear.ShipView;
import com.devilfruits.fruits.suke_suke.client.ShipLook;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.dispatcher.FancySubLevelRenderDispatcher;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import dev.ryanhcode.sable.sublevel.render.dispatcher.VanillaSubLevelRenderDispatcher;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {VanillaSubLevelRenderDispatcher.class, FancySubLevelRenderDispatcher.class},
   remap = false
)
public abstract class SubLevelDispatcherMixin {
   @Unique
   private static boolean devilfruits$splitting;

   @Inject(
      method = {"renderSectionLayer"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void devilfruits$hiddenShips(
      Iterable<ClientSubLevel> sublevels,
      RenderType renderType,
      ShaderInstance shader,
      double camX,
      double camY,
      double camZ,
      Matrix4f modelView,
      Matrix4f projection,
      float partial,
      CallbackInfo ci
   ) {
      if (!devilfruits$splitting && ShipView.any()) {
         List<ClientSubLevel> usual = null;
         List<ClientSubLevel> glass = null;
         List<Float> alphas = null;

         for (ClientSubLevel s : sublevels) {
            float a = ShipLook.alpha(s.getUniqueId(), partial);
            if (usual != null || !(a < 0.0F)) {
               if (usual == null) {
                  usual = new ArrayList<>();
                  glass = new ArrayList<>();
                  alphas = new ArrayList<>();

                  for (ClientSubLevel before : sublevels) {
                     if (before == s) {
                        break;
                     }

                     usual.add(before);
                  }
               }

               if (a < 0.0F || a >= 0.999F) {
                  usual.add(s);
               } else if (a > 0.004F) {
                  glass.add(s);
                  alphas.add(a);
               }
            }
         }

         if (usual != null) {
            ci.cancel();
            SubLevelRenderDispatcher self = (SubLevelRenderDispatcher)this;
            devilfruits$splitting = true;

            try {
               if (!usual.isEmpty()) {
                  self.renderSectionLayer(usual, renderType, shader, camX, camY, camZ, modelView, projection, partial);
               }

               boolean blended = renderType == RenderType.translucent();

               for (int i = 0; i < glass.size(); i++) {
                  float a = alphas.get(i);
                  RenderSystem.enableBlend();
                  RenderSystem.defaultBlendFunc();
                  devilfruits$modulate(shader, 0.82F, 0.97F, 0.92F, a);
                  self.renderSectionLayer(List.of(glass.get(i)), renderType, shader, camX, camY, camZ, modelView, projection, partial);
               }

               if (!glass.isEmpty()) {
                  devilfruits$modulate(shader, 1.0F, 1.0F, 1.0F, 1.0F);
                  if (!blended) {
                     RenderSystem.disableBlend();
                  }
               }
            } finally {
               devilfruits$splitting = false;
            }
         }
      }
   }

   @ModifyVariable(
      method = {"renderAfterSections"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private Iterable<ClientSubLevel> devilfruits$hiddenSingles(Iterable<ClientSubLevel> sublevels) {
      if (!ShipView.any()) {
         return sublevels;
      } else {
         List<ClientSubLevel> shown = new ArrayList<>();

         for (ClientSubLevel s : sublevels) {
            if (ShipLook.alpha(s.getUniqueId(), 1.0F) != 0.0F) {
               shown.add(s);
            }
         }

         return shown;
      }
   }

   @Unique
   private static void devilfruits$modulate(ShaderInstance shader, float r, float g, float b, float a) {
      RenderSystem.setShaderColor(r, g, b, a);
      if (shader != null && shader.COLOR_MODULATOR != null) {
         shader.COLOR_MODULATOR.set(r, g, b, a);
         shader.COLOR_MODULATOR.upload();
      }
   }
}
