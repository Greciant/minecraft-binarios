package com.awakenedmod.awakened.client.chrono.stasis;

public record StasisProp(float width, float height, float bodyYaw, float age, int hold, float sinceRelease) {
   public boolean released() {
      return this.sinceRelease >= 0.0F;
   }
}
