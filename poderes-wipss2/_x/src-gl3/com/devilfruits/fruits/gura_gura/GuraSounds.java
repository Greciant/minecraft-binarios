package com.devilfruits.fruits.gura_gura;

import com.devilfruits.registry.DFSounds;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;

public final class GuraSounds {
   public static final Supplier<SoundEvent> KAISHIN_CHARGE = DFSounds.register("gura_gura.ability.kaishin.charge");
   public static final Supplier<SoundEvent> KAISHIN_RELEASE = DFSounds.register("gura_gura.ability.kaishin.release");
   public static final Supplier<SoundEvent> GEKISHIN = DFSounds.register("gura_gura.ability.gekishin");
   public static final Supplier<SoundEvent> KABUTOWARI = DFSounds.register("gura_gura.ability.kabutowari");
   public static final Supplier<SoundEvent> SHIMA_YURASHI = DFSounds.register("gura_gura.ability.shima_yurashi");
   public static final Supplier<SoundEvent> SHIMA_YURASHI_TREMOR = DFSounds.register("gura_gura.ability.shima_yurashi.tremor");
   public static final Supplier<SoundEvent> KAISHO = DFSounds.register("gura_gura.ability.kaisho");
   public static final Supplier<SoundEvent> AURA_LOOP = DFSounds.register("gura_gura.ability.aura.loop");
   public static final Supplier<SoundEvent> AURA_DEFLECT = DFSounds.register("gura_gura.ability.aura.deflect");
   public static final Supplier<SoundEvent> WEAPON_SLASH = DFSounds.register("gura_gura.ability.weapon_slash");
   public static final Supplier<SoundEvent> LAYER_SHATTER = DFSounds.register("gura_gura.layer.shatter");
   public static final Supplier<SoundEvent> LAYER_SHATTER_HIGH = DFSounds.register("gura_gura.layer.shatter_high");
   public static final Supplier<SoundEvent> LAYER_CRACK = DFSounds.register("gura_gura.layer.crack");

   static void init() {
   }

   private GuraSounds() {
   }
}
