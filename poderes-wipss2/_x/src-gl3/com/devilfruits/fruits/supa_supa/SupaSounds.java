package com.devilfruits.fruits.supa_supa;

import com.devilfruits.registry.DFSounds;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;

public final class SupaSounds {
   public static final Supplier<SoundEvent> BLADE_GROW = DFSounds.register("supa_supa.blade.grow");
   public static final Supplier<SoundEvent> BLADE_RETRACT = DFSounds.register("supa_supa.blade.retract");
   public static final Supplier<SoundEvent> CLAW_SWING = DFSounds.register("supa_supa.claw.swing");
   public static final Supplier<SoundEvent> CLAW_HIT = DFSounds.register("supa_supa.claw.hit");
   public static final Supplier<SoundEvent> ATOMIC_SPAR = DFSounds.register("supa_supa.atomic_spar");
   public static final Supplier<SoundEvent> STEEL_ON = DFSounds.register("supa_supa.steel.on");
   public static final Supplier<SoundEvent> STEEL_OFF = DFSounds.register("supa_supa.steel.off");
   public static final Supplier<SoundEvent> STEEL_CLANG = DFSounds.register("supa_supa.steel.clang");
   public static final Supplier<SoundEvent> SKATES_UNFOLD = DFSounds.register("supa_supa.skates.unfold");
   public static final Supplier<SoundEvent> SKATES_SCRAPE = DFSounds.register("supa_supa.skates.scrape");
   public static final Supplier<SoundEvent> SKATES_BRAKE = DFSounds.register("supa_supa.skates.brake");
   public static final Supplier<SoundEvent> SKATES_LUNGE = DFSounds.register("supa_supa.skates.lunge");
   public static final Supplier<SoundEvent> SPIRAL_SPIN = DFSounds.register("supa_supa.spiral.spin");
   public static final Supplier<SoundEvent> SPIRAL_GRIND = DFSounds.register("supa_supa.spiral.grind");
   public static final Supplier<SoundEvent> SLASH_CHARGE = DFSounds.register("supa_supa.slash.charge");
   public static final Supplier<SoundEvent> SLASH_RELEASE = DFSounds.register("supa_supa.slash.release");
   public static final Supplier<SoundEvent> SLASH_FLY = DFSounds.register("supa_supa.slash.fly");
   public static final Supplier<SoundEvent> SLASH_BURST = DFSounds.register("supa_supa.slash.burst");
   public static final Supplier<SoundEvent> SEAM_BITE = DFSounds.register("supa_supa.seam.bite");
   public static final Supplier<SoundEvent> SEAM_CUT = DFSounds.register("supa_supa.seam.cut");
   public static final Supplier<SoundEvent> SEAM_EXIT = DFSounds.register("supa_supa.seam.exit");
   public static final Supplier<SoundEvent> SEAM_TENSION = DFSounds.register("supa_supa.seam.tension");
   public static final Supplier<SoundEvent> SEAM_SPLIT = DFSounds.register("supa_supa.seam.split");

   static void init() {
   }

   private SupaSounds() {
   }
}
