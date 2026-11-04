package com.devilfruits.fruits.gomu_gomu;

import com.devilfruits.registry.DFSounds;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;

public final class GomuSounds {
   public static final Supplier<SoundEvent> RUBBER_LAND = DFSounds.register("gomu_gomu.rubber_land");
   public static final Supplier<SoundEvent> BLUNT_ABSORB = DFSounds.register("gomu_gomu.blunt_absorb");
   public static final Supplier<SoundEvent> BULLET_ABSORB = DFSounds.register("gomu_gomu.bullet_absorb");
   public static final Supplier<SoundEvent> BULLET_RETURN = DFSounds.register("gomu_gomu.bullet_return");
   public static final Supplier<SoundEvent> STRETCH = DFSounds.register("gomu_gomu.stretch");
   public static final Supplier<SoundEvent> SNAP = DFSounds.register("gomu_gomu.snap");
   public static final Supplier<SoundEvent> FIST_BACK = DFSounds.register("gomu_gomu.fist_back");
   public static final int FIST_BACK_SNAP = 2;
   public static final Supplier<SoundEvent> PUNCH_LAUNCH = DFSounds.register("gomu_gomu.punch_launch");
   public static final Supplier<SoundEvent> BAZOOKA_LAUNCH = DFSounds.register("gomu_gomu.bazooka_launch");
   public static final Supplier<SoundEvent> RIFLE_LAUNCH = DFSounds.register("gomu_gomu.rifle_launch");
   public static final Supplier<SoundEvent> PUNCH_HIT = DFSounds.register("gomu_gomu.punch_hit");
   public static final Supplier<SoundEvent> BAZOOKA_HIT = DFSounds.register("gomu_gomu.bazooka_hit");
   public static final Supplier<SoundEvent> RIFLE_TWIST = DFSounds.register("gomu_gomu.rifle_twist");
   public static final Supplier<SoundEvent> RIFLE_HIT = DFSounds.register("gomu_gomu.rifle_hit");
   public static final Supplier<SoundEvent> AXE_IMPACT = DFSounds.register("gomu_gomu.axe_impact");
   public static final Supplier<SoundEvent> GATLING = DFSounds.register("gomu_gomu.gatling");
   public static final int GATLING_LENGTH = 18;

   static void init() {
   }

   private GomuSounds() {
   }
}
