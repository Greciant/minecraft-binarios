package com.awakenedmod.awakened.content.command.vocab.target;

import com.awakenedmod.awakened.content.command.vocab.Target;
import com.awakenedmod.awakened.content.command.vocab.TargetKind;

public final class WhatHitsMeTarget implements Target {
   @Override
   public String id() {
      return "what_hits_me";
   }

   @Override
   public int unlockLevel() {
      return 5;
   }

   @Override
   public double defaultWeight() {
      return 1.2;
   }

   @Override
   public TargetKind kind() {
      return TargetKind.HITS_ME;
   }
}
