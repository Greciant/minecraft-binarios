package com.awakenedmod.awakened.core;

import com.awakenedmod.awakened.Awakened;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;

public final class ConfigOverrides<V> {
   private final String label;
   private final Supplier<List<? extends String>> source;
   private final Function<String, V> parser;
   private volatile ConfigOverrides.Cache<V> cache = new ConfigOverrides.Cache<>(null, Map.of());

   public ConfigOverrides(String label, Supplier<List<? extends String>> source, Function<String, V> parser) {
      this.label = label;
      this.source = source;
      this.parser = parser;
      AwakenedConfigs.check(label, this::accepts);
      AwakenedConfigs.keyed(label);
   }

   public ConfigOverrides(String label, Supplier<List<? extends String>> source, Function<String, V> parser, Supplier<Stream<String>> names) {
      this(label, source, parser);
      AwakenedConfigs.keyChoices(label, (Function<CommandSourceStack, Stream<String>>)(commandSource -> names.get()));
   }

   public boolean accepts(String entry) {
      int split = entry.indexOf(61);
      return split > 0 && !entry.substring(0, split).isBlank() && this.value(entry, split) != null;
   }

   public Map<String, V> map() {
      List<? extends String> raw = this.source.get();
      ConfigOverrides.Cache<V> current = this.cache;
      if (raw != current.raw()) {
         current = new ConfigOverrides.Cache<>(raw, Map.copyOf(this.parse(raw)));
         this.cache = current;
      }

      return current.map();
   }

   public V get(String key, V fallback) {
      return key == null ? fallback : this.map().getOrDefault(key, fallback);
   }

   public boolean has(String key) {
      return this.map().containsKey(key);
   }

   private Map<String, V> parse(List<? extends String> raw) {
      Map<String, V> map = new HashMap<>();

      for (String entry : raw) {
         int split = entry.indexOf(61);
         if (split <= 0) {
            this.warn(entry);
         } else {
            V value = this.value(entry, split);
            if (value == null) {
               this.warn(entry);
            } else {
               map.put(entry.substring(0, split).trim(), value);
            }
         }
      }

      return map;
   }

   private V value(String entry, int split) {
      try {
         return this.parser.apply(entry.substring(split + 1).trim());
      } catch (NumberFormatException var4) {
         return null;
      }
   }

   private void warn(String entry) {
      Awakened.LOGGER.warn("Ignoring malformed {} config entry '{}'", this.label, entry);
   }

   private static record Cache<V>(List<? extends String> raw, Map<String, V> map) {
   }
}
