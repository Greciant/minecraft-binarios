package com.awakenedmod.awakened.client.music;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

public final class NotePattern {
   private final int[] ticks;
   private final Holder<SoundEvent>[] instruments;
   private final int[] notes;
   private final float[] volumes;
   private final int length;

   private NotePattern(int[] ticks, Holder<SoundEvent>[] instruments, int[] notes, float[] volumes, int length) {
      this.ticks = ticks;
      this.instruments = instruments;
      this.notes = notes;
      this.volumes = volumes;
      this.length = length;
   }

   public int length() {
      return this.length;
   }

   public int emit(int pos, int cursor, float volMul, NotePattern.NoteSink sink) {
      while (cursor < this.ticks.length && this.ticks[cursor] == pos) {
         sink.note(this.instruments[cursor], this.notes[cursor], this.volumes[cursor] * volMul);
         cursor++;
      }

      return cursor;
   }

   public int cursorFor(int pos) {
      int i = 0;

      while (i < this.ticks.length && this.ticks[i] < pos) {
         i++;
      }

      return i;
   }

   public static NotePattern.Builder builder(int length) {
      return new NotePattern.Builder(length);
   }

   public static final class Builder {
      private final int length;
      private final List<NotePattern.Builder.Step> steps = new ArrayList<>();

      private Builder(int length) {
         this.length = length;
      }

      public NotePattern.Builder add(int tick, Holder<SoundEvent> instrument, int note, float volume) {
         this.steps.add(new NotePattern.Builder.Step(tick, instrument, note, volume));
         return this;
      }

      public NotePattern build() {
         this.steps.sort(Comparator.comparingInt(NotePattern.Builder.Step::tick));
         int n = this.steps.size();
         int[] ticks = new int[n];
         Holder<SoundEvent>[] instruments = new Holder[n];
         int[] notes = new int[n];
         float[] volumes = new float[n];

         for (int i = 0; i < n; i++) {
            NotePattern.Builder.Step s = this.steps.get(i);
            ticks[i] = s.tick;
            instruments[i] = s.instrument;
            notes[i] = s.note;
            volumes[i] = s.volume;
         }

         return new NotePattern(ticks, instruments, notes, volumes, this.length);
      }

      private static record Step(int tick, Holder<SoundEvent> instrument, int note, float volume) {
      }
   }

   public interface NoteSink {
      void note(Holder<SoundEvent> var1, int var2, float var3);
   }
}
