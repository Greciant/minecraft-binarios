package com.awakenedmod.awakened.core.terrain;

final class TerrainLimits {
   static final int WRITES_PER_TICK = 2048;
   static final int VOID_MARGIN = 4;
   static final int VOID_SCAN = 4;
   static final int RESUME_MAX_DELAY_TICKS = 400;
   static final int MAX_PLAN_BLOCKS = 262144;
   static final int TICKET_REFRESH_TICKS = 100;
   static final double UNBURY_SEARCH_RADIUS = 4.0;
   static final int UNBURY_MAX_DROP = 3;

   private TerrainLimits() {
   }
}
