package com.awakenedmod.awakened.client.voidview;

public enum VoidIntroMode {
   FULL,
   SHORT,
   NO_CAMERA;

   public boolean movesCamera() {
      return this != NO_CAMERA;
   }
}
