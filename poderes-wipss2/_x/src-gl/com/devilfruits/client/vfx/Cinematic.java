package com.devilfruits.client.vfx;

import net.minecraft.client.multiplayer.ClientLevel;

public abstract class Cinematic {
   protected final VfxAnchor anchor;
   protected final VfxParams params;
   protected int age;
   private boolean done;

   protected Cinematic(VfxAnchor anchor, VfxParams params) {
      this.anchor = anchor;
      this.params = params;
   }

   public void tick(ClientLevel level) {
      this.age++;
   }

   public void render(VfxFrame frame) {
   }

   public void removed() {
   }

   public abstract double x();

   public abstract double y();

   public abstract double z();

   public double cullDistance() {
      return 96.0;
   }

   public final boolean isDone() {
      return this.done;
   }

   public final void finish() {
      this.done = true;
   }

   public final int age() {
      return this.age;
   }

   public final VfxAnchor anchor() {
      return this.anchor;
   }
}
