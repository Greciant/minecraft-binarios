package com.awakenedmod.awakened.core;

import com.awakenedmod.awakened.Awakened;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileWatcher;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.fml.config.IConfigSpec.ILoadedConfig;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.config.ModConfigEvent.Reloading;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class AwakenedConfigs {
   private static final List<ModConfigSpec> SPECS = new ArrayList<>();
   private static final Map<String, Function<CommandSourceStack, Stream<String>>> CHOICES = new ConcurrentHashMap<>();
   private static final Map<String, Predicate<String>> CHECKS = new ConcurrentHashMap<>();
   private static final Map<ModConfigSpec, List<Consumer<MinecraftServer>>> ON_RELOAD = new ConcurrentHashMap<>();
   private static final Map<String, Function<CommandSourceStack, Stream<String>>> KEY_CHOICES = new ConcurrentHashMap<>();
   private static final Set<String> KEYED = ConcurrentHashMap.newKeySet();
   private static final Map<ConfigValue<?>, ConfigValue<?>> HIDDEN = new ConcurrentHashMap<>();
   private static final Map<ConfigValue<?>, ConfigValue<?>> MOVED = new LinkedHashMap<>();
   @Nullable
   private static Method openConfig;
   private static boolean openConfigLooked;
   private static boolean reloadListener;
   private static boolean movedListener;
   private static Map<String, AwakenedConfigs.Entry> entries;
   private static List<AwakenedConfigs.Entry> all;

   private AwakenedConfigs() {
   }

   public static void register(ModContainer container, Type type, ModConfigSpec spec, @Nullable String fileName) {
      if (fileName == null) {
         container.registerConfig(type, spec);
      } else {
         container.registerConfig(type, spec, fileName);
      }

      if (type != Type.CLIENT) {
         SPECS.add(spec);
         entries = null;
         all = null;
      }

      if (!reloadListener && container.getEventBus() != null) {
         reloadListener = true;
         container.getEventBus().addListener(Reloading.class, AwakenedConfigs::onReloading);
      }
   }

   public static void onReload(ModConfigSpec spec, Consumer<MinecraftServer> hook) {
      ON_RELOAD.computeIfAbsent(spec, k -> new CopyOnWriteArrayList<>()).add(hook);
   }

   private static void onReloading(Reloading event) {
      List<Consumer<MinecraftServer>> hooks = ON_RELOAD.get(event.getConfig().getSpec());
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      if (hooks != null && server != null) {
         server.execute(() -> hooks.forEach(hook -> hook.accept(server)));
      }
   }

   public static Map<String, AwakenedConfigs.Entry> entries() {
      if (entries == null) {
         Map<String, AwakenedConfigs.Entry> byKey = new LinkedHashMap<>();

         for (AwakenedConfigs.Entry entry : all()) {
            AwakenedConfigs.Entry first = byKey.get(entry.key());
            if (first != null && (!hidden(first) || hidden(entry))) {
               if (!hidden(first) && !hidden(entry)) {
                  Awakened.LOGGER.warn("Config key {} is defined twice; /awakened config only reaches the first", entry.key());
               }
            } else {
               byKey.put(entry.key(), entry);
            }
         }

         entries = Collections.unmodifiableMap(byKey);
      }

      return entries;
   }

   private static List<AwakenedConfigs.Entry> all() {
      if (all == null) {
         List<AwakenedConfigs.Entry> list = new ArrayList<>();

         for (ModConfigSpec spec : SPECS) {
            collect(spec, spec.getValues(), "", list);
         }

         all = Collections.unmodifiableList(list);
      }

      return all;
   }

   @Nullable
   public static AwakenedConfigs.Entry get(String key) {
      return entries().get(key);
   }

   public static List<AwakenedConfigs.Entry> listed() {
      return entries().values().stream().filter(e -> !hidden(e)).toList();
   }

   @Nullable
   public static AwakenedConfigs.Entry find(String key) {
      AwakenedConfigs.Entry exact = entries().get(key);
      if (exact != null) {
         return hidden(exact) ? null : exact;
      } else {
         for (AwakenedConfigs.Entry entry : listed()) {
            if (entry.key().equalsIgnoreCase(key)) {
               return entry;
            }
         }

         return null;
      }
   }

   public static void hide(ConfigValue<?> useInstead, ConfigValue<?>... old) {
      for (ConfigValue<?> value : old) {
         HIDDEN.put(value, useInstead);
      }

      entries = null;
   }

   public static <T> void moved(ConfigValue<T> moved, ConfigValue<T> old) {
      hide(moved, old);
      MOVED.put(old, moved);
      if (!movedListener) {
         movedListener = true;
         NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, e -> {
            try {
               carryOverMoved(e.getServer());
            } catch (RuntimeException var2) {
               Awakened.LOGGER.error("Could not carry moved config settings over", var2);
            }
         });
      }
   }

   private static void carryOverMoved(MinecraftServer server) {
      MOVED.forEach((old, moved) -> carryOver(moved, old, server));
   }

   private static <T> void carryOver(ConfigValue<T> moved, ConfigValue<T> old, MinecraftServer server) {
      AwakenedConfigs.Entry from = entryOf(old);
      AwakenedConfigs.Entry to = entryOf(moved);
      if (from != null && to != null && from.spec().isLoaded() && to.spec().isLoaded() && !Objects.equals(old.get(), old.getDefault())) {
         T legacy = (T)old.get();
         if (!Objects.equals(legacy, moved.get())) {
            if (!Objects.equals(moved.get(), moved.getDefault())) {
               Awakened.LOGGER
                  .warn(
                     "Ignoring old setting {} = {} in {}: {} in {} is already set", new Object[]{from.key(), legacy, fileName(old), to.key(), fileName(moved)}
                  );
            } else if (writeShared(to, moved, legacy)) {
               Awakened.LOGGER.info("Carried {} = {} over from {} to {}", new Object[]{from.key(), legacy, fileName(old), fileName(moved)});
            }
         }
      }
   }

   private static <T> boolean writeShared(AwakenedConfigs.Entry entry, ConfigValue<T> value, T newValue) {
      ModConfig file = fileOf(entry.spec());
      if (file == null) {
         return false;
      } else {
         Path shared = FMLPaths.CONFIGDIR.get().resolve(file.getFileName());
         if (!shared.equals(file.getFullPath())) {
            try {
               CommentedFileConfig onDisk = (CommentedFileConfig)CommentedFileConfig.builder(shared, TomlFormat.instance())
                  .writingMode(WritingMode.REPLACE_ATOMIC)
                  .build();

               try {
                  if (Files.exists(shared)) {
                     onDisk.load();
                  }

                  onDisk.set(entry.key(), newValue);
                  onDisk.save();
               } catch (Throwable var9) {
                  if (onDisk != null) {
                     try {
                        onDisk.close();
                     } catch (Throwable var8) {
                        var9.addSuppressed(var8);
                     }
                  }

                  throw var9;
               }

               if (onDisk != null) {
                  onDisk.close();
               }
            } catch (RuntimeException var10) {
               Awakened.LOGGER.warn("Could not write {} to config/{}", new Object[]{entry.key(), file.getFileName(), var10});
               return false;
            }
         }

         T oldValue = (T)value.get();
         value.set(newValue);
         return save(entry, value, oldValue);
      }
   }

   private static <T> boolean save(AwakenedConfigs.Entry entry, ConfigValue<T> value, T oldValue) {
      try {
         value.save();
         return true;
      } catch (RuntimeException var6) {
         try {
            value.set(oldValue);
         } catch (RuntimeException var5) {
         }

         Awakened.LOGGER.warn("Could not save config setting {}", entry.key(), var6);
         return false;
      }
   }

   private static boolean hidden(AwakenedConfigs.Entry entry) {
      return HIDDEN.containsKey(entry.value());
   }

   @Nullable
   public static String replacementOf(String key) {
      for (AwakenedConfigs.Entry entry : entries().values()) {
         if (entry.key().equalsIgnoreCase(key) && hidden(entry)) {
            AwakenedConfigs.Entry instead = entryOf(HIDDEN.get(entry.value()));
            return instead == null ? "?" : instead.key();
         }
      }

      return null;
   }

   public static String fileName(ConfigValue<?> value) {
      AwakenedConfigs.Entry entry = entryOf(value);
      ModConfig file = entry == null ? null : fileOf(entry.spec());
      return file == null ? "?" : file.getFileName();
   }

   public static boolean reachesPlayers(ConfigValue<?> value) {
      AwakenedConfigs.Entry entry = entryOf(value);
      ModConfig file = entry == null ? null : fileOf(entry.spec());
      return file != null && file.getType() == Type.SERVER;
   }

   public static <T> AwakenedConfigs.Saved write(ConfigValue<T> value, T newValue, @Nullable MinecraftServer server) {
      AwakenedConfigs.Entry entry = entryOf(value);
      if (entry != null && entry.spec().isLoaded()) {
         T oldValue = (T)value.get();
         if (Objects.equals(oldValue, newValue)) {
            return AwakenedConfigs.Saved.UNCHANGED;
         } else {
            value.set(newValue);
            ModConfig file = fileOf(entry.spec());
            AwakenedConfigs.Saved where = AwakenedConfigs.Saved.SHARED_FILE;
            if (file != null && file.getType() == Type.SERVER && server != null && !server.isDedicatedServer()) {
               Path copy = server.getWorldPath(new LevelResource("serverconfig")).resolve(file.getFileName());
               if (copy.equals(file.getFullPath())) {
                  if (!save(entry, value, oldValue)) {
                     return AwakenedConfigs.Saved.FAILED;
                  }

                  where = AwakenedConfigs.Saved.THIS_WORLD;
               } else {
                  if (!makeWorldCopy(server, copy, file)) {
                     value.set(oldValue);
                     return AwakenedConfigs.Saved.FAILED;
                  }

                  where = AwakenedConfigs.Saved.NEW_WORLD_COPY;
               }
            } else if (!save(entry, value, oldValue)) {
               return AwakenedConfigs.Saved.FAILED;
            }

            return where;
         }
      } else {
         return AwakenedConfigs.Saved.NOT_LOADED;
      }
   }

   public static <T> void adopt(ConfigValue<T> value, @Nullable T legacy, MinecraftServer server, Runnable forget) {
      if (legacy != null && entryOf(value) != null && entryOf(value).spec().isLoaded()) {
         if (legacy.equals(value.get())) {
            forget.run();
         } else if (!Objects.equals(value.get(), value.getDefault())) {
            Awakened.LOGGER.warn("Ignoring this world's old saved value {} for {}: the config already sets it", legacy, entryOf(value).key());
         } else {
            if (write(value, legacy, server).saved()) {
               forget.run();
            }
         }
      }
   }

   @Nullable
   private static AwakenedConfigs.Entry entryOf(ConfigValue<?> value) {
      for (AwakenedConfigs.Entry entry : all()) {
         if (entry.value() == value) {
            return entry;
         }
      }

      return null;
   }

   @Nullable
   private static ModConfig fileOf(ModConfigSpec spec) {
      for (ModConfig config : ModConfigs.getModConfigs("awakened")) {
         if (config.getSpec() == spec) {
            return config;
         }
      }

      return null;
   }

   private static boolean makeWorldCopy(MinecraftServer server, Path copy, ModConfig file) {
      ILoadedConfig loaded = file.getLoadedConfig();
      if (loaded == null) {
         return false;
      } else {
         try {
            Files.createDirectories(copy.getParent());
            TomlFormat.instance().createWriter().write(loaded.config(), copy, WritingMode.REPLACE_ATOMIC);
         } catch (RuntimeException | IOException var6) {
            Awakened.LOGGER.warn("Could not write this world's copy of {}", file.getFileName(), var6);
            return false;
         }

         reopen(file, copy.getParent());

         try {
            ModList.get().getModContainerById("awakened").ifPresent(c -> c.acceptEvent(new Reloading(file)));
         } catch (RuntimeException var5) {
            Awakened.LOGGER.warn("A listener failed on the reload of {}", file.getFileName(), var5);
         }

         return true;
      }
   }

   private static void reopen(ModConfig file, Path overrideDir) {
      Method open = openConfigMethod();
      if (open != null) {
         try {
            FileWatcher.defaultInstance().removeWatch(FMLPaths.CONFIGDIR.get().resolve(file.getFileName()));
         } catch (RuntimeException var5) {
            Awakened.LOGGER.warn("Could not stop watching config/{}", file.getFileName(), var5);
         }

         try {
            open.invoke(null, file, FMLPaths.CONFIGDIR.get(), overrideDir);
         } catch (RuntimeException | LinkageError | ReflectiveOperationException var4) {
            Awakened.LOGGER.warn("Could not have FML reload {} from this world's copy; it is used from the next world start", file.getFileName(), var4);
         }
      }
   }

   @Nullable
   private static synchronized Method openConfigMethod() {
      if (!openConfigLooked) {
         openConfigLooked = true;

         try {
            Method method = ConfigTracker.class.getDeclaredMethod("openConfig", ModConfig.class, Path.class, Path.class);
            method.setAccessible(true);
            openConfig = method;
         } catch (RuntimeException | LinkageError | ReflectiveOperationException var1) {
            Awakened.LOGGER.warn("Cannot reach FML's ConfigTracker.openConfig; a new world copy of a config is used from the next world start", var1);
         }
      }

      return openConfig;
   }

   public static void choices(String key, Function<CommandSourceStack, Stream<String>> source) {
      CHOICES.put(key, source);
   }

   public static void check(String key, Predicate<String> accepts) {
      CHECKS.put(key, accepts);
   }

   public static void keyed(String key) {
      KEYED.add(key);
   }

   public static boolean isKeyed(String key) {
      return KEYED.contains(key);
   }

   public static void keyChoices(String key, Function<CommandSourceStack, Stream<String>> names) {
      keyed(key);
      KEY_CHOICES.put(key, names);
   }

   @Nullable
   public static Stream<String> keyChoices(String key, CommandSourceStack source) {
      Function<CommandSourceStack, Stream<String>> names = KEY_CHOICES.get(key);
      return names == null ? null : names.apply(source);
   }

   @Nullable
   public static String nameOf(String entry) {
      int split = entry.indexOf(61);
      String name = split > 0 ? entry.substring(0, split).trim() : "";
      return name.isEmpty() ? null : name;
   }

   @Nullable
   public static Stream<String> choices(String key, CommandSourceStack source) {
      Function<CommandSourceStack, Stream<String>> choices = CHOICES.get(key);
      return choices == null ? null : choices.apply(source);
   }

   public static boolean accepts(String key, String entry, CommandSourceStack source) {
      Predicate<String> check = CHECKS.get(key);
      if (check != null && !check.test(entry)) {
         return false;
      } else if (isKeyed(key) && !knownName(key, nameOf(entry), source)) {
         return false;
      } else {
         Stream<String> choices = choices(key, source);
         return choices == null || choices.anyMatch(entry::equals);
      }
   }

   public static boolean knownName(String key, @Nullable String name, CommandSourceStack source) {
      if (name == null) {
         return false;
      } else {
         Stream<String> names = keyChoices(key, source);
         return names == null || names.anyMatch(name::equals);
      }
   }

   private static void collect(ModConfigSpec spec, UnmodifiableConfig config, String prefix, List<AwakenedConfigs.Entry> out) {
      for (com.electronwill.nightconfig.core.UnmodifiableConfig.Entry entry : config.entrySet()) {
         String key = prefix + entry.getKey();
         Object value = entry.getRawValue();
         if (value instanceof UnmodifiableConfig section) {
            collect(spec, section, key + ".", out);
         } else if (value instanceof ConfigValue<?> handle) {
            out.add(new AwakenedConfigs.Entry(key, spec, handle));
         }
      }
   }

   public static record Entry(String key, ModConfigSpec spec, ConfigValue<?> value) {
   }

   public static enum Saved {
      SHARED_FILE,
      THIS_WORLD,
      NEW_WORLD_COPY,
      UNCHANGED,
      NOT_LOADED,
      FAILED;

      public boolean saved() {
         return this == SHARED_FILE || this == THIS_WORLD || this == NEW_WORLD_COPY;
      }
   }
}
