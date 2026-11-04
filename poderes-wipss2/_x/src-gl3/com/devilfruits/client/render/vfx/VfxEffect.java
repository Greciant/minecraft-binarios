package com.devilfruits.client.render.vfx;

import net.minecraft.client.multiplayer.ClientLevel;

public abstract class VfxEffect {
   protected int age;
   private boolean done;

   public void tick(ClientLevel level) {
      this.age++;
   }

   public void renderShell(VfxContext ctx) {
   }

   public void renderGlow(VfxContext ctx) {
   }

   public void renderTextured(VfxContext ctx) {
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
}
