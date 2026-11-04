package com.awakenedmod.awakened.content.entity;

import java.util.Locale;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public final class ArbiterRootVariants {
   public static final int STAND_TICKS = 120;
   public static final int STAND_CAP = 9;
   public static final int SOLID_AFTER_BURST_TICKS = 2;
   public static final float PILLAR_WIDTH = 0.45F;
   public static final float PILLAR_HEIGHT = 3.2F;
   public static final double CAGE_RADIUS = 3.0;
   public static final int CAGE_SLOTS = 8;
   public static final int CAGE_ROOTS = 6;
   public static final double CAGE_MIN_SPACING = 1.5;
   public static final float CAGE_VOLUME = 0.7F;
   public static final int COURT_ROOTS_T2 = 6;
   public static final int COURT_ROOTS_T3 = 10;
   public static final double COURT_MIN_RADIUS = 4.0;
   public static final double COURT_MAX_RADIUS = 16.0;
   public static final double COURT_MIN_SPACING = 3.0;
   public static final int COURT_TRIES = 8;
   public static final float COURT_VOLUME = 0.7F;
   public static final int COURT_GAVEL_MIN = 2;
   public static final int COURT_GAVEL_MAX = 3;
   public static final int COURT_STIR_MIN = 1;
   public static final int COURT_STIR_MAX = 2;
   public static final int FOREST_TIER = 2;
   public static final int FOREST_STAND_TICKS = 12000;
   private static final String DATA_ID = "awakened_arbiter_roots";

   private ArbiterRootVariants() {
   }

   public static int courtRoots(int tier, int defendants, boolean caged) {
      int base = tier >= 3 ? 10 : 6;
      int share = Math.max(1, base / Math.max(1, defendants));
      return caged ? Math.max(1, share / 2) : share;
   }

   private static ArbiterRootVariants.State state(MinecraftServer server) {
      return (ArbiterRootVariants.State)server.overworld()
         .getDataStorage()
         .computeIfAbsent(new Factory(ArbiterRootVariants.State::new, ArbiterRootVariants.State::load, null), "awakened_arbiter_roots");
   }

   public static ArbiterRootVariants.Mode mode(MinecraftServer server) {
      return server == null ? ArbiterRootVariants.Mode.CLASSIC : state(server).mode;
   }

   public static boolean forest(MinecraftServer server) {
      return server != null && state(server).forest;
   }

   public static void setMode(MinecraftServer server, ArbiterRootVariants.Mode mode) {
      ArbiterRootVariants.State state = state(server);
      if (state.mode != mode) {
         state.mode = mode;
         state.setDirty();
      }
   }

   public static void setForest(MinecraftServer server, boolean forest) {
      ArbiterRootVariants.State state = state(server);
      if (state.forest != forest) {
         state.forest = forest;
         state.setDirty();
      }
   }

   public static enum Mode {
      CLASSIC(false, false),
      PILLARS(false, true),
      CAGE(true, false),
      CAGE_PILLARS(true, true);

      private final boolean cage;
      private final boolean stand;

      private Mode(boolean cage, boolean stand) {
         this.cage = cage;
         this.stand = stand;
      }

      public boolean cage() {
         return this.cage;
      }

      public boolean stand() {
         return this.stand;
      }

      public String id() {
         return this.name().toLowerCase(Locale.ROOT);
      }

      static ArbiterRootVariants.Mode byId(String id) {
         for (ArbiterRootVariants.Mode mode : values()) {
            if (mode.id().equals(id)) {
               return mode;
            }
         }

         return CLASSIC;
      }
   }

   public static final class State extends SavedData {
      private ArbiterRootVariants.Mode mode = ArbiterRootVariants.Mode.CLASSIC;
      private boolean forest;

      State() {
      }

      State(String mode, boolean forest) {
         this.mode = ArbiterRootVariants.Mode.byId(mode);
         this.forest = forest;
      }

      static ArbiterRootVariants.State load(CompoundTag tag, Provider provider) {
         return new ArbiterRootVariants.State(tag.getString("mode"), tag.getBoolean("forest"));
      }

      public CompoundTag save(CompoundTag tag, Provider provider) {
         tag.putString("mode", this.mode.id());
         tag.putBoolean("forest", this.forest);
         return tag;
      }
   }
}
