package com.devilfruits.fruits.kilo_kilo;

import com.devilfruits.registry.DFSounds;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;

public final class KiloSounds {
   public static final Supplier<SoundEvent> WEIGHT_DOWN = DFSounds.register("kilo_kilo.weight.down");
   public static final Supplier<SoundEvent> WEIGHT_UP = DFSounds.register("kilo_kilo.weight.up");
   public static final Supplier<SoundEvent> RAMP_DONE = DFSounds.register("kilo_kilo.weight.ramp_done");
   public static final Supplier<SoundEvent> LIGHT_JUMP = DFSounds.register("kilo_kilo.light.jump");
   public static final Supplier<SoundEvent> GLIDE_LOOP = DFSounds.register("kilo_kilo.light.glide");
   public static final Supplier<SoundEvent> HEAVY_STEP = DFSounds.register("kilo_kilo.heavy.step");
   public static final Supplier<SoundEvent> FALL_RUSH = DFSounds.register("kilo_kilo.heavy.fall_rush");
   public static final Supplier<SoundEvent> PRESS_IMPACT = DFSounds.register("kilo_kilo.press.impact");
   public static final Supplier<SoundEvent> PUNCH_THROUGH = DFSounds.register("kilo_kilo.press.punch_through");
   public static final Supplier<SoundEvent> HULL_GROAN = DFSounds.register("kilo_kilo.heavy.hull_groan");

   static void init() {
   }

   private KiloSounds() {
   }
}
