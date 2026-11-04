package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.Awakened;
import com.awakenedmod.awakened.core.AwId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;

public final class PostEffectSlot {
   private final ResourceLocation chain;
   private final String label;
   private boolean applied;
   private boolean loadFailed;
   private boolean warned;

   public PostEffectSlot(String name, String label) {
      this.chain = AwId.of("shaders/post/" + name + ".json");
      this.label = label;
   }

   public boolean active() {
      return this.applied;
   }

   public boolean failed() {
      return this.loadFailed;
   }

   public void want(boolean on) {
      if (this.applied) {
         if (this.mine()) {
            if (!on) {
               this.restore();
            }

            return;
         }

         this.applied = false;
      }

      if (on && !this.loadFailed && !occupied()) {
         this.applied = this.load();
         if (!this.applied) {
            this.loadFailed = true;
            this.warnOnce();
         }
      }
   }

   public void clear() {
      if (this.applied) {
         this.restore();
      }

      this.loadFailed = false;
   }

   private void restore() {
      Minecraft mc = Minecraft.getInstance();
      if (this.mine()) {
         mc.gameRenderer.checkEntityPostEffect(mc.getCameraEntity());
      }

      this.applied = false;
   }

   private void warnOnce() {
      if (!this.warned) {
         this.warned = true;
         Awakened.LOGGER.warn("Failed to load {} post effect {}", this.label, this.chain);
      }
   }

   private boolean load() {
      Minecraft mc = Minecraft.getInstance();
      mc.gameRenderer.loadEffect(this.chain);
      return this.mine();
   }

   private static boolean occupied() {
      Minecraft mc = Minecraft.getInstance();
      return mc.gameRenderer.currentEffect() != null;
   }

   private boolean mine() {
      Minecraft mc = Minecraft.getInstance();
      PostChain current = mc.gameRenderer.currentEffect();
      return current != null && this.chain.toString().equals(current.getName());
   }
}
