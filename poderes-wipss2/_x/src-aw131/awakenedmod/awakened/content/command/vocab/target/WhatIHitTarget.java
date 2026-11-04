package com.awakenedmod.awakened.content.command.vocab.target;

import com.awakenedmod.awakened.content.command.vocab.Target;
import com.awakenedmod.awakened.content.command.vocab.TargetKind;

public final class WhatIHitTarget implements Target {
   @Override
   public String id() {
      return "what_i_hit";
   }

   @Override
   public int unlockLevel() {
      return 2;
   }

   @Override
   public double defaultWeight() {
      return 1.2;
   }

   @Override
   public TargetKind kind() {
      return TargetKind.HIT;
   }
}
