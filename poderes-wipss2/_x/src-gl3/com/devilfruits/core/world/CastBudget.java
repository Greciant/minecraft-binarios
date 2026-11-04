package com.devilfruits.core.world;

import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.physics.PhysicsConfig;

public final class CastBudget {
   private int blocks;
   private int debris;
   private int structureBlocks = -1;

   public CastBudget(int blocks, int debris) {
      this.blocks = blocks;
      this.debris = debris;
   }

   public static CastBudget fromConfig() {
      return new CastBudget((Integer)CoreConfig.MAX_BLOCKS_PER_CAST.get(), (Integer)CoreConfig.MAX_DEBRIS_ENTITIES_PER_CAST.get());
   }

   public boolean takeBlock() {
      if (this.blocks <= 0) {
         return false;
      } else {
         this.blocks--;
         return true;
      }
   }

   public boolean takeDebris() {
      if (this.debris <= 0) {
         return false;
      } else {
         this.debris--;
         return true;
      }
   }

   public boolean takeStructureBlock() {
      if (this.structureBlocks < 0) {
         this.structureBlocks = (Integer)PhysicsConfig.MAX_BLOCKS_PER_CAST.get();
      }

      if (this.structureBlocks <= 0) {
         return false;
      } else {
         this.structureBlocks--;
         return true;
      }
   }

   public void atLeast(int blocks) {
      this.blocks = Math.max(this.blocks, blocks);
   }

   public boolean hasBlocks() {
      return this.blocks > 0;
   }
}
