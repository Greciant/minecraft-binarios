package com.devilfruits.fruits.bomu_bomu;

import com.devilfruits.registry.DFSounds;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;

public final class BomuSounds {
   public static final Supplier<SoundEvent> PUNCH_ARM = DFSounds.register("bomu_bomu.punch.arm");
   public static final Supplier<SoundEvent> PUNCH_TICK = DFSounds.register("bomu_bomu.punch.tick");
   public static final Supplier<SoundEvent> PUNCH_FIZZLE = DFSounds.register("bomu_bomu.punch.fizzle");
   public static final Supplier<SoundEvent> CANNON_FLICK = DFSounds.register("bomu_bomu.cannon.flick");
   public static final Supplier<SoundEvent> CANNON_FIRE = DFSounds.register("bomu_bomu.cannon.fire");
   public static final Supplier<SoundEvent> CANNON_SPLAT = DFSounds.register("bomu_bomu.cannon.splat");
   public static final Supplier<SoundEvent> SHOT_FIRE = DFSounds.register("bomu_bomu.shot.fire");
   public static final Supplier<SoundEvent> SHOT_PLAIN = DFSounds.register("bomu_bomu.shot.plain");
   public static final Supplier<SoundEvent> SHOT_CLICK = DFSounds.register("bomu_bomu.shot.click");
   public static final Supplier<SoundEvent> SHOT_RELOAD = DFSounds.register("bomu_bomu.shot.reload");
   public static final Supplier<SoundEvent> SHOT_OPEN = DFSounds.register("bomu_bomu.shot.open");
   public static final Supplier<SoundEvent> SHOT_BLOW = DFSounds.register("bomu_bomu.shot.blow");
   public static final Supplier<SoundEvent> SHOT_CLOSE = DFSounds.register("bomu_bomu.shot.close");
   public static final Supplier<SoundEvent> SHOT_WHIZ = DFSounds.register("bomu_bomu.shot.whiz");
   public static final Supplier<SoundEvent> SELF_DESTRUCT_CHARGE = DFSounds.register("bomu_bomu.self_destruct.charge");
   public static final Supplier<SoundEvent> SELF_DESTRUCT_TIER = DFSounds.register("bomu_bomu.self_destruct.tier");
   public static final Supplier<SoundEvent> SELF_DESTRUCT_REFORM = DFSounds.register("bomu_bomu.self_destruct.reform");
   public static final Supplier<SoundEvent> ABSORB = DFSounds.register("bomu_bomu.absorb");

   static void init() {
   }

   private BomuSounds() {
   }
}
