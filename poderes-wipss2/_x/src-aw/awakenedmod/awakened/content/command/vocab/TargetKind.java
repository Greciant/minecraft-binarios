package com.awakenedmod.awakened.content.command.vocab;

public enum TargetKind {
   SELF,
   HIT,
   HITS_ME,
   GAZE,
   NEAR;

   public boolean lands() {
      return this == HIT || this == HITS_ME || this == GAZE;
   }

   public boolean landsBySelect() {
      return this == GAZE;
   }
}
