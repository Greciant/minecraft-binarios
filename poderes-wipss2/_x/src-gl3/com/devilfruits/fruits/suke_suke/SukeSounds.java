package com.devilfruits.fruits.suke_suke;

import com.devilfruits.registry.DFSounds;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;

public final class SukeSounds {
   public static final Supplier<SoundEvent> VANISH = DFSounds.register("suke_suke.vanish");
   public static final Supplier<SoundEvent> APPEAR = DFSounds.register("suke_suke.appear");
   public static final Supplier<SoundEvent> GROUP_CAST = DFSounds.register("suke_suke.group_cast");
   public static final Supplier<SoundEvent> SHIP_VANISH = DFSounds.register("suke_suke.ship_vanish");
   public static final Supplier<SoundEvent> SHIP_APPEAR = DFSounds.register("suke_suke.ship_appear");

   static void init() {
   }

   private SukeSounds() {
   }
}
