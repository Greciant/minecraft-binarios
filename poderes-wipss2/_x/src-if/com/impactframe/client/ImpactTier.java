package com.impactframe.client;

public enum ImpactTier {
   LIGHT(2, 0.1F, 3, 0.55F, 0.015, 1.3F, 0.6F, 1),
   NORMAL(3, 0.16F, 7, 0.95F, 0.03, 1.05F, 0.8F, 1),
   HEAVY(5, 0.24F, 27, 1.65F, 0.06, 0.9F, 1.0F, 1),
   SMASH(8, 0.38F, 183, 3.0F, 0.11, 0.75F, 1.0F, 2);

   private static final float FRAME_SECONDS = 0.016666668F;
   public final int holdFrames;
   public final float releaseSeconds;
   public final int invertMask;
   public final float shake;
   public final double fovPunch;
   public final float soundPitch;
   public final float soundVolume;
   public final int soundLayers;

   private ImpactTier(int holdFrames, float releaseSeconds, int invertMask, float shake, double fovPunch, float soundPitch, float soundVolume, int soundLayers) {
      this.holdFrames = holdFrames;
      this.releaseSeconds = releaseSeconds;
      this.invertMask = invertMask;
      this.shake = shake;
      this.fovPunch = fovPunch;
      this.soundPitch = soundPitch;
      this.soundVolume = soundVolume;
      this.soundLayers = soundLayers;
   }

   public float holdSeconds() {
      return (float)this.holdFrames * 0.016666668F;
   }

   public float totalSeconds() {
      return this.holdSeconds() + this.releaseSeconds;
   }
}
