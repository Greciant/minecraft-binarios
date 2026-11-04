package com.awakenedmod.awakened.content;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AwakenedSounds {
   private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, "awakened");
   public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_BATTLE = SOUNDS.register(
      "music.sentinel_battle", () -> SoundEvent.createVariableRangeEvent(AwId.of("music.sentinel_battle"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> FLAME_BEAM_LOOP = sound("pyro.flame_beam.loop");
   public static final DeferredHolder<SoundEvent, SoundEvent> FLAME_BEAM_FULL = sound("pyro.flame_beam.full");
   public static final DeferredHolder<SoundEvent, SoundEvent> FLAME_BEAM_BLUE = sound("pyro.flame_beam.blue");
   public static final DeferredHolder<SoundEvent, SoundEvent> FLAME_BEAM_IGNITE = sound("pyro.flame_beam.ignite");
   public static final DeferredHolder<SoundEvent, SoundEvent> FLAME_BEAM_SHUTDOWN = sound("pyro.flame_beam.shutdown");
   public static final DeferredHolder<SoundEvent, SoundEvent> FLAME_BEAM_SPUTTER = sound("pyro.flame_beam.sputter");
   public static final DeferredHolder<SoundEvent, SoundEvent> FLAME_BEAM_IMPACT = sound("pyro.flame_beam.impact");
   public static final DeferredHolder<SoundEvent, SoundEvent> FLAME_BEAM_CRACKLE = sound("pyro.flame_beam.crackle");
   public static final DeferredHolder<SoundEvent, SoundEvent> FLAME_BEAM_GOUT = sound("pyro.flame_beam.gout");
   public static final DeferredHolder<SoundEvent, SoundEvent> FLAME_BEAM_THROB = sound("pyro.flame_beam.throb");
   public static final DeferredHolder<SoundEvent, SoundEvent> ARBITER_GAVEL_CRACK = sound("arbiter.gavel.crack", 64.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> ARBITER_CHAIN_RATTLE = sound("arbiter.chain.rattle");
   public static final DeferredHolder<SoundEvent, SoundEvent> ARBITER_SEAL_SHATTER = sound("arbiter.seal.shatter", 40.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> ARBITER_COURT_COLLAPSE = sound("arbiter.court.collapse", 64.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> ARBITER_KNEEL_DRONE = sound("arbiter.court.kneel");
   public static final DeferredHolder<SoundEvent, SoundEvent> ARBITER_RISE_TOLL = sound("arbiter.rise.toll", 64.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> TESLA_CRACKLE = sound("stormcaller.tesla.crackle");
   public static final DeferredHolder<SoundEvent, SoundEvent> TESLA_BUZZ = sound("stormcaller.tesla.buzz");
   public static final DeferredHolder<SoundEvent, SoundEvent> TESLA_SNAP = sound("stormcaller.tesla.snap");
   public static final DeferredHolder<SoundEvent, SoundEvent> TESLA_IGNITE = sound("stormcaller.tesla.ignite");
   public static final DeferredHolder<SoundEvent, SoundEvent> TESLA_FIZZLE = sound("stormcaller.tesla.fizzle");
   public static final DeferredHolder<SoundEvent, SoundEvent> CHRONO_STOP = sound("chrono.stop", 48.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> CHRONO_TICK_TOWER = sound("chrono.tick_tower", 48.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> CHRONO_RESUME_SWELL = sound("chrono.resume_swell", 48.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> CHRONO_STOP_BURN = sound("chrono.stop_burn", 48.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> CHRONO_HELD_HITS = sound("chrono.held_hits", 48.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> CHRONO_TICK_ESCAPE = sound("chrono.tick_escape");
   public static final DeferredHolder<SoundEvent, SoundEvent> CHRONO_STASIS_KEY_WIND = sound("chrono.stasis_key_wind");
   public static final DeferredHolder<SoundEvent, SoundEvent> CHRONO_STASIS_KEY_TURN = sound("chrono.stasis_key_turn");
   public static final DeferredHolder<SoundEvent, SoundEvent> CHRONO_STASIS_KEY_RELEASE = sound("chrono.stasis_key_release");
   public static final DeferredHolder<SoundEvent, SoundEvent> CHRONO_REWIND = sound("chrono.rewind");
   public static final DeferredHolder<SoundEvent, SoundEvent> CHRONO_SKIP = sound("chrono.skip");
   public static final DeferredHolder<SoundEvent, SoundEvent> CHRONO_WATCH_LOOP = sound("chrono.watch_loop");
   public static final DeferredHolder<SoundEvent, SoundEvent> HORIZON_RUMBLE = sound("graviton.horizon.rumble", 96.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> HORIZON_WHINE = sound("graviton.horizon.whine", 96.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> HORIZON_OPEN = sound("graviton.horizon.open", 128.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> HORIZON_COLLAPSE = sound("graviton.horizon.collapse", 160.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> DENSE_PLANT = sound("graviton.dense.plant", 24.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> DENSE_LOAD = sound("graviton.dense.load");
   public static final DeferredHolder<SoundEvent, SoundEvent> DENSE_ABSORB = sound("graviton.dense.absorb");
   public static final DeferredHolder<SoundEvent, SoundEvent> DENSE_RELEASE = sound("graviton.dense.release", 48.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> DENSE_RUMBLE = sound("graviton.dense.rumble", 48.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> DENSE_PUFF = sound("graviton.dense.puff");
   public static final DeferredHolder<SoundEvent, SoundEvent> WELL_OPEN = sound("graviton.well.open", 48.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> WELL_SNAP = sound("graviton.well.snap", 48.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> WELL_HUM = sound("graviton.well.hum", 32.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> WELL_RUSH = sound("graviton.well.rush");
   public static final DeferredHolder<SoundEvent, SoundEvent> HAND_GRIP = sound("graviton.hand.grip", 32.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> HAND_BEND = sound("graviton.hand.bend", 32.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> HAND_YANK = sound("graviton.hand.yank", 32.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> HAND_SHOVE = sound("graviton.hand.shove", 32.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> HAND_CRUSH = sound("graviton.hand.crush", 32.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> HAND_LAUNCH = sound("graviton.hand.launch", 32.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> PULL = sound("graviton.pull", 32.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> SHEET_RELEASE = sound("graviton.sheet.release");
   public static final DeferredHolder<SoundEvent, SoundEvent> STONE_CREAK = sound("graviton.stone.creak");
   public static final DeferredHolder<SoundEvent, SoundEvent> MASS_HEAVIER = sound("graviton.mass.heavier");
   public static final DeferredHolder<SoundEvent, SoundEvent> MASS_LIGHTER = sound("graviton.mass.lighter");
   public static final DeferredHolder<SoundEvent, SoundEvent> MASS_RESET = sound("graviton.mass.reset");
   public static final DeferredHolder<SoundEvent, SoundEvent> MASS_LAND = sound("graviton.mass.land", 32.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> WEIGHTLESS_CAST = sound("graviton.weightless.cast", 24.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> WEIGHTLESS_ZONE = sound("graviton.weightless.zone", 24.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> WEIGHTLESS_DROP = sound("graviton.weightless.drop", 40.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> AWAKEN = sound("graviton.awaken", 32.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> RUIN_RUMBLE = sound("ruin.rumble", 96.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> RUIN_GROAN = sound("ruin.groan");
   public static final DeferredHolder<SoundEvent, SoundEvent> RUIN_GRIND = sound("ruin.grind");
   public static final DeferredHolder<SoundEvent, SoundEvent> PURPLE_CHARGE = sound("limitless.purple.charge", 48.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> PURPLE_READY = sound("limitless.purple.ready", 64.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> PURPLE_RELEASE = sound("limitless.purple.release", 160.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> PURPLE_ROAR = sound("limitless.purple.roar", 128.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> PURPLE_ERASE = sound("limitless.purple.erase", 64.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> PURPLE_END = sound("limitless.purple.end", 96.0F);
   public static final DeferredHolder<SoundEvent, SoundEvent> PURPLE_AFTERMATH = sound("limitless.purple.aftermath", 160.0F);

   private AwakenedSounds() {
   }

   private static DeferredHolder<SoundEvent, SoundEvent> sound(String id) {
      return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(AwId.of(id)));
   }

   private static DeferredHolder<SoundEvent, SoundEvent> sound(String id, float range) {
      return SOUNDS.register(id, () -> SoundEvent.createFixedRangeEvent(AwId.of(id), range));
   }

   public static void register(IEventBus modEventBus) {
      SOUNDS.register(modEventBus);
   }
}
