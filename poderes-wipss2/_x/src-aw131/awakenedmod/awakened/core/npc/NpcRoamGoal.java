package com.awakenedmod.awakened.core.npc;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class NpcRoamGoal extends Goal {
   private static final int WALK_MIN = 16;
   private static final int WALK_REACH = 32;
   private static final int WALK_VERTICAL = 10;
   private static final int CANDIDATES = 10;
   private static final double HEIGHT_PULL = 0.35;
   private static final double PATH_PULL = 1.5;
   private static final double JITTER = 2.0;
   private static final double PACE = 0.8;
   private static final double WATER_PACE = 1.25;
   private static final int WALK_TIMEOUT = 600;
   private static final int PAUSE_MIN = 60;
   private static final int PAUSE_SPAN = 140;
   private static final int RETRY_TICKS = 40;
   private static final int FLOURISH_MIN = 100;
   private static final int FLOURISH_SPAN = 100;
   private static final int WATER_REPATH = 20;
   private static final int WATER_REACH = 12;
   private final PoweredNpc npc;
   private boolean walking;
   private int walkUntil;
   private int pauseUntil;
   private int flourishAt;
   private int waterRepathAt;
   @Nullable
   private Vec3 interest;
   @Nullable
   private Vec3 land;

   public NpcRoamGoal(PoweredNpc npc) {
      this.npc = npc;
      this.setFlags(EnumSet.of(Flag.MOVE));
   }

   public boolean canUse() {
      return this.npc.getTarget() == null;
   }

   public boolean canContinueToUse() {
      return this.npc.getTarget() == null;
   }

   public boolean requiresUpdateEveryTick() {
      return true;
   }

   public void start() {
      this.walking = false;
      this.interest = null;
      this.pauseUntil = this.npc.tickCount + 20 + this.npc.getRandom().nextInt(40);
      this.flourishAt = this.npc.tickCount + 100 + this.npc.getRandom().nextInt(100);
   }

   public void stop() {
      this.npc.getNavigation().stop();
      this.walking = false;
   }

   public void tick() {
      int now = this.npc.tickCount;
      if (now >= this.flourishAt) {
         this.flourishAt = now + 100 + this.npc.getRandom().nextInt(100);
         this.npc.kit().idleFlourish(this.npc.caster());
      }

      if (this.npc.kit().avoidsWater() && NpcNav.wet(this.npc)) {
         if (now >= this.waterRepathAt) {
            this.waterRepathAt = now + 20;
            this.land = NpcNav.dryLand(this.npc, 12);
         }

         if (this.land != null) {
            NpcFootwork.wadeTo(this.npc, this.land, 1.25);
         }

         this.walking = false;
         this.pauseUntil = now;
      } else if (!this.walking) {
         if (this.interest != null) {
            this.npc.getLookControl().setLookAt(this.interest.x, this.interest.y, this.interest.z);
         }

         if (now >= this.pauseUntil) {
            Vec3 dest = this.pickDestination();
            if (dest == null) {
               this.pauseUntil = now + 40;
            } else {
               this.interest = null;
               this.walking = this.npc.getNavigation().moveTo(dest.x, dest.y, dest.z, 0.8);
               this.walkUntil = now + 600;
               if (!this.walking) {
                  this.pauseUntil = now + 40;
               }
            }
         }
      } else {
         if (this.npc.getNavigation().isDone() || now >= this.walkUntil) {
            this.walking = false;
            this.pauseUntil = now + 60 + this.npc.getRandom().nextInt(140);
            this.interest = this.npc.kit().pointOfInterest(this.npc.caster());
         }
      }
   }

   @Nullable
   private Vec3 pickDestination() {
      Vec3 best = null;
      double bestScore = Double.NEGATIVE_INFINITY;

      for (int i = 0; i < 10; i++) {
         Vec3 c = LandRandomPos.getPos(this.npc, 32, 10);
         if (c != null && !(NpcNav.flat(this.npc.position(), c) < 16.0)) {
            double score = (c.y - this.npc.getY()) * 0.35 + this.npc.getRandom().nextDouble() * 2.0;
            if (this.npc.level().getBlockState(BlockPos.containing(c).below()).is(Blocks.DIRT_PATH)) {
               score++;
            }

            if (score > bestScore) {
               bestScore = score;
               best = c;
            }
         }
      }

      return best;
   }
}
