package com.awakenedmod.awakened.client.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.slf4j.Logger;

public final class NoteScore {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<String, Holder<SoundEvent>> INSTRUMENTS = new HashMap<>();
   private static final int DEFAULT_STEPS_PER_BAR = 32;
   private static final NotePattern EMPTY = NotePattern.builder(1).build();
   private final String path;
   private final Map<String, NotePattern> patterns = new HashMap<>();
   private final Map<String, NoteScore.Steps> stepPatterns = new HashMap<>();
   private final Map<String, NotePattern> quantized = new HashMap<>();
   private int stepsPerBar = 32;

   private NoteScore(String path) {
      this.path = path;
   }

   public static NoteScore load(String name) {
      String path = "/assets/awakened/music/" + name + ".json";
      NoteScore score = new NoteScore(path);

      try {
         label71: {
            NoteScore var11;
            try (InputStream in = NoteScore.class.getResourceAsStream(path)) {
               if (in != null) {
                  JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                  if (root.has("steps_per_bar")) {
                     score.stepsPerBar = Math.max(1, root.get("steps_per_bar").getAsInt());
                  }

                  for (Entry<String, JsonElement> entry : root.getAsJsonObject("patterns").entrySet()) {
                     JsonObject json = entry.getValue().getAsJsonObject();
                     if (json.has("bars")) {
                        score.stepPatterns.put(entry.getKey(), parseSteps(json, path));
                     } else {
                        score.patterns.put(entry.getKey(), parsePattern(json, path));
                     }
                  }
                  break label71;
               }

               LOGGER.error("Missing boss theme score {}", path);
               var11 = score;
            }

            return var11;
         }
      } catch (Exception var10) {
         LOGGER.error("Failed to load boss theme score {}", path, var10);
      }

      return score;
   }

   private static NotePattern parsePattern(JsonObject json, String path) {
      NotePattern.Builder b = NotePattern.builder(Math.max(1, json.get("length").getAsInt()));

      for (JsonElement element : json.getAsJsonArray("notes")) {
         JsonArray note = element.getAsJsonArray();
         String name = note.get(1).getAsString();
         Holder<SoundEvent> instrument = INSTRUMENTS.get(name);
         if (instrument == null) {
            LOGGER.warn("Boss theme score {}: unknown instrument '{}' skipped", path, name);
         } else {
            int semitone = Math.max(0, Math.min(24, note.get(2).getAsInt()));
            b.add(note.get(0).getAsInt(), instrument, semitone, note.get(3).getAsFloat());
         }
      }

      return b.build();
   }

   private static NoteScore.Steps parseSteps(JsonObject json, String path) {
      JsonArray raw = json.getAsJsonArray("notes");
      int n = raw.size();
      int[] positions = new int[n];
      Holder<SoundEvent>[] instruments = new Holder[n];
      int[] notes = new int[n];
      float[] volumes = new float[n];
      int kept = 0;

      for (JsonElement element : raw) {
         JsonArray note = element.getAsJsonArray();
         String name = note.get(1).getAsString();
         Holder<SoundEvent> instrument = INSTRUMENTS.get(name);
         if (instrument == null) {
            LOGGER.warn("Boss theme score {}: unknown instrument '{}' skipped", path, name);
         } else {
            positions[kept] = note.get(0).getAsInt();
            instruments[kept] = instrument;
            notes[kept] = Math.max(0, Math.min(24, note.get(2).getAsInt()));
            volumes[kept] = note.get(3).getAsFloat();
            kept++;
         }
      }

      return new NoteScore.Steps(
         Math.max(1, json.get("bars").getAsInt()),
         Arrays.copyOf(positions, kept),
         Arrays.copyOf(instruments, kept),
         Arrays.copyOf(notes, kept),
         Arrays.copyOf(volumes, kept)
      );
   }

   public NotePattern barPattern(String name, int barTicks) {
      int bar = Math.max(1, barTicks);
      String key = name + "@" + bar;
      NotePattern cached = this.quantized.get(key);
      if (cached != null) {
         return cached;
      } else {
         NoteScore.Steps steps = this.stepPatterns.get(name);
         if (steps == null) {
            LOGGER.warn("Boss theme score {} has no bar pattern '{}'", this.path, name);
            this.quantized.put(key, EMPTY);
            return EMPTY;
         } else {
            NotePattern.Builder b = NotePattern.builder(steps.bars() * bar);

            for (int i = 0; i < steps.positions().length; i++) {
               int pos = steps.positions()[i];
               int barIndex = pos / this.stepsPerBar;
               int step = pos % this.stepsPerBar;
               int tick = barIndex * bar + (step * bar + this.stepsPerBar / 2) / this.stepsPerBar;
               b.add(tick, steps.instruments()[i], steps.notes()[i], steps.volumes()[i]);
            }

            NotePattern built = b.build();
            this.quantized.put(key, built);
            return built;
         }
      }
   }

   public NotePattern pattern(String name) {
      NotePattern p = this.patterns.get(name);
      if (p == null) {
         LOGGER.warn("Boss theme score {} has no pattern '{}'", this.path, name);
         this.patterns.put(name, EMPTY);
         return EMPTY;
      } else {
         return p;
      }
   }

   static {
      INSTRUMENTS.put("bass", SoundEvents.NOTE_BLOCK_BASS);
      INSTRUMENTS.put("basedrum", SoundEvents.NOTE_BLOCK_BASEDRUM);
      INSTRUMENTS.put("snare", SoundEvents.NOTE_BLOCK_SNARE);
      INSTRUMENTS.put("hat", SoundEvents.NOTE_BLOCK_HAT);
      INSTRUMENTS.put("harp", SoundEvents.NOTE_BLOCK_HARP);
      INSTRUMENTS.put("pling", SoundEvents.NOTE_BLOCK_PLING);
      INSTRUMENTS.put("bell", SoundEvents.NOTE_BLOCK_BELL);
      INSTRUMENTS.put("chime", SoundEvents.NOTE_BLOCK_CHIME);
      INSTRUMENTS.put("bit", SoundEvents.NOTE_BLOCK_BIT);
      INSTRUMENTS.put("iron_xylophone", SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE);
      INSTRUMENTS.put("xylophone", SoundEvents.NOTE_BLOCK_XYLOPHONE);
      INSTRUMENTS.put("didgeridoo", SoundEvents.NOTE_BLOCK_DIDGERIDOO);
      INSTRUMENTS.put("flute", SoundEvents.NOTE_BLOCK_FLUTE);
      INSTRUMENTS.put("guitar", SoundEvents.NOTE_BLOCK_GUITAR);
      INSTRUMENTS.put("banjo", SoundEvents.NOTE_BLOCK_BANJO);
      INSTRUMENTS.put("cow_bell", SoundEvents.NOTE_BLOCK_COW_BELL);
   }

   private static record Steps(int bars, int[] positions, Holder<SoundEvent>[] instruments, int[] notes, float[] volumes) {
   }
}
