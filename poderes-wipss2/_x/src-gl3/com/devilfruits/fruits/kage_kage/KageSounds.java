package com.devilfruits.fruits.kage_kage;

import com.devilfruits.registry.DFSounds;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;

public final class KageSounds {
   public static final Supplier<SoundEvent> SNIP = DFSounds.register("kage_kage.snip");
   public static final Supplier<SoundEvent> ABSORB = DFSounds.register("kage_kage.absorb");
   public static final Supplier<SoundEvent> SHADOW_RETURN = DFSounds.register("kage_kage.shadow_return");
   public static final Supplier<SoundEvent> SCISSORS_SPLIT = DFSounds.register("kage_kage.scissors.split");
   public static final Supplier<SoundEvent> SCISSORS_JOIN = DFSounds.register("kage_kage.scissors.join");
   public static final Supplier<SoundEvent> SNIP_CHARGE = DFSounds.register("kage_kage.snip_charge");

   static void init() {
   }

   private KageSounds() {
   }
}
