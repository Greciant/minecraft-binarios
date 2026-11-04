package com.awakenedmod.awakened.core.npc;

import javax.annotation.Nullable;
import net.minecraft.world.phys.Vec3;

public record NpcStep(NpcStep.Kind kind, @Nullable String move, @Nullable Vec3 dest, @Nullable NpcTravel.Purpose purpose, NpcFooting footing) {
   public static final NpcStep DONE = new NpcStep(NpcStep.Kind.DONE, null, null, null, NpcFooting.BAND);

   public static NpcStep cast(String move, NpcFooting footing) {
      return new NpcStep(NpcStep.Kind.CAST, move, null, null, footing);
   }

   public static NpcStep travel(Vec3 dest, NpcTravel.Purpose purpose) {
      return new NpcStep(NpcStep.Kind.TRAVEL, null, dest, purpose, NpcFooting.JUKE);
   }

   public static NpcStep move(NpcFooting footing) {
      return new NpcStep(NpcStep.Kind.MOVE, null, null, null, footing);
   }

   public static enum Kind {
      CAST,
      TRAVEL,
      MOVE,
      DONE;
   }
}
