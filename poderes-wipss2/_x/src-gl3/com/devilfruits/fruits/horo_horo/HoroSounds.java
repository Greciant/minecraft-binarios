package com.devilfruits.fruits.horo_horo;

import com.devilfruits.registry.DFSounds;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;

public final class HoroSounds {
   public static final Supplier<SoundEvent> GHOST_EMERGE = DFSounds.register("horo_horo.ghost_emerge");
   public static final Supplier<SoundEvent> NEGATIVE_HIT = DFSounds.register("horo_horo.negative_hit");
   public static final Supplier<SoundEvent> GIGGLE = DFSounds.register("horo_horo.giggle");
   public static final Supplier<SoundEvent> CLAP = DFSounds.register("horo_horo.clap");
   public static final Supplier<SoundEvent> POP = DFSounds.register("horo_horo.pop");
   public static final Supplier<SoundEvent> GHOST_EXIT = DFSounds.register("horo_horo.ghost_exit");
   public static final Supplier<SoundEvent> POSSESS = DFSounds.register("horo_horo.possess");
   public static final Supplier<SoundEvent> GIANT_RISE = DFSounds.register("horo_horo.giant_rise");
   public static final Supplier<SoundEvent> GIANT_GULP = DFSounds.register("horo_horo.giant_gulp");
   public static final Supplier<SoundEvent> GIANT_SWELL = DFSounds.register("horo_horo.giant_swell");
   public static final Supplier<SoundEvent> BLAST = DFSounds.register("horo_horo.blast");

   static void init() {
   }

   private HoroSounds() {
   }
}
