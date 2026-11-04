package com.devilfruits.client.animation;

public interface AnimationClip {
   float length();

   boolean loop();

   default float loopFrom() {
      return 0.0F;
   }

   float fadeIn();

   float fadeOut();
}
