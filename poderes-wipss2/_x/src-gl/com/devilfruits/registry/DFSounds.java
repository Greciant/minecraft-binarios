package com.devilfruits.registry;

import com.devilfruits.DevilFruits;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DFSounds {
   public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, "devilfruits");
   public static final Supplier<SoundEvent> FRUIT_EAT = register("fruit.eat");
   public static final Supplier<SoundEvent> FRUIT_AWAKEN = register("fruit.awaken");
   public static final Supplier<SoundEvent> FRUIT_REINCARNATE = register("fruit.reincarnate");
   public static final Supplier<SoundEvent> MASTERY_LEVEL_UP = register("mastery.level_up");
   public static final Supplier<SoundEvent> POWER_SUPPRESSED = register("power.suppressed");
   public static final Supplier<SoundEvent> FORM_CHANGE = register("form.change");
   public static final Supplier<SoundEvent> LOGIA_DISSOLVE = register("logia.dissolve");
   public static final Supplier<SoundEvent> LOGIA_REFORM = register("logia.reform");
   public static final Supplier<SoundEvent> LOGIA_PASS = register("logia.pass");
   public static final Supplier<SoundEvent> LOGIA_SUPPRESS = register("logia.suppress");
   public static final Supplier<SoundEvent> LOGIA_DASH = register("logia.dash");
   public static final Supplier<SoundEvent> LAYER_BOOM = register("layer.boom");
   public static final Supplier<SoundEvent> LAYER_SONIC = register("layer.sonic");
   public static final Supplier<SoundEvent> LAYER_THUNDER = register("layer.thunder");
   public static final Supplier<SoundEvent> LAYER_IMPACT = register("layer.impact");
   public static final Supplier<SoundEvent> LAYER_ANVIL = register("layer.anvil");
   public static final Supplier<SoundEvent> LAYER_ROAR = register("layer.roar");
   public static final Supplier<SoundEvent> LAYER_WIND = register("layer.wind");
   public static final Supplier<SoundEvent> LAYER_SPLASH = register("layer.splash");
   public static final Supplier<SoundEvent> LAYER_RUMBLE = register("layer.rumble");
   public static final Supplier<SoundEvent> EXPLOSION_CRACK = register("explosion.crack");
   public static final Supplier<SoundEvent> EXPLOSION_BOOM = register("explosion.boom");
   public static final Supplier<SoundEvent> EXPLOSION_DEEP = register("explosion.deep");
   public static final Supplier<SoundEvent> EXPLOSION_DISTANT = register("explosion.distant");
   public static final Supplier<SoundEvent> EXPLOSION_DEBRIS = register("explosion.debris");
   public static final Supplier<SoundEvent> EXPLOSION_CRACKLE = register("explosion.crackle");
   public static final Supplier<SoundEvent> STRUCTURE_CREAK = register("structure.creak");
   public static final Supplier<SoundEvent> STRUCTURE_THOOM = register("structure.thoom");
   public static final Supplier<SoundEvent> HAKI_COAT = register("haki.coat");
   public static final Supplier<SoundEvent> HAKI_UNCOAT = register("haki.uncoat");
   public static final Supplier<SoundEvent> HAKI_FLICKER = register("haki.flicker");
   public static final Supplier<SoundEvent> HAKI_HIT = register("haki.hit");
   public static final Supplier<SoundEvent> HAKI_CLASH = register("haki.clash");
   public static final Supplier<SoundEvent> HAKI_RYUO_CHARGE = register("haki.ryuo_charge");
   public static final Supplier<SoundEvent> HAKI_RYUO = register("haki.ryuo");
   public static final Supplier<SoundEvent> HAKI_LEVEL_UP = register("haki.level_up");
   public static final Supplier<SoundEvent> HAKI_UNLOCK = register("haki.unlock");
   public static final Supplier<SoundEvent> HAKI_LAYER_ANVIL = register("haki.layer.anvil");
   public static final Supplier<SoundEvent> HAKI_LAYER_THUNDER = register("haki.layer.thunder");
   public static final Supplier<SoundEvent> HAKI_LAYER_SMASH = register("haki.layer.smash");

   public static Supplier<SoundEvent> register(String name) {
      return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(DevilFruits.id(name)));
   }

   private DFSounds() {
   }
}
