package com.awakenedmod.awakened.core.command;

import com.awakenedmod.awakened.core.AwakenedConfigs;
import com.awakenedmod.awakened.core.ChatFx;
import com.awakenedmod.awakened.core.PowerLimits;
import com.awakenedmod.awakened.core.progression.AbilityBalance;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
import net.neoforged.neoforge.common.ModConfigSpec.LongValue;
import net.neoforged.neoforge.common.ModConfigSpec.Range;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ConfigCommand {
   private static final int BIG_SECTION = 10;
   private static final List<String> LIST_OPS = List.of("add", "remove", "clear");
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_KEYS = (ctx, builder) -> {
      Set<String> out = new LinkedHashSet<>();

      for (AwakenedConfigs.Entry entry : AwakenedConfigs.listed()) {
         String key = entry.key();

         for (int dot = key.indexOf(46); dot > 0; dot = key.indexOf(46, dot + 1)) {
            out.add(key.substring(0, dot));
         }

         out.add(key);
      }

      return SharedSuggestionProvider.suggest(out, builder);
   };
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_VALUES = (ctx, builder) -> {
      AwakenedConfigs.Entry entry = AwakenedConfigs.find(key(ctx));
      if (entry == null) {
         return builder.buildFuture();
      } else if (isList(entry)) {
         return suggestList(entry, (CommandSourceStack)ctx.getSource(), builder);
      } else {
         List<String> allowed = allowedWords(entry);
         return allowed != null
            ? SharedSuggestionProvider.suggest(allowed, builder)
            : SharedSuggestionProvider.suggest(List.of(plain(entry.value().getDefault())), builder);
      }
   };

   private ConfigCommand() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ConfigCommand::onRegister);
   }

   private static void onRegister(RegisterCommandsEvent event) {
      for (Class<?> holder : List.of(AbilityBalance.class, PowerLimits.class)) {
         try {
            Class.forName(holder.getName(), true, holder.getClassLoader());
         } catch (ClassNotFoundException var4) {
            throw new IllegalStateException(var4);
         }
      }

      register(event.getDispatcher());
   }

   private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("awakened")
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("config").requires(RealisticMovementCommand::isOp))
                     .executes(ctx -> sections((CommandSourceStack)ctx.getSource())))
                  .then(
                     ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("key", StringArgumentType.word())
                              .suggests(SUGGEST_KEYS)
                              .executes(ctx -> show((CommandSourceStack)ctx.getSource(), key(ctx))))
                           .then(Commands.literal("reset").executes(ctx -> reset((CommandSourceStack)ctx.getSource(), key(ctx)))))
                        .then(
                           Commands.argument("value", StringArgumentType.greedyString())
                              .suggests(SUGGEST_VALUES)
                              .executes(ctx -> change((CommandSourceStack)ctx.getSource(), key(ctx), StringArgumentType.getString(ctx, "value")))
                        )
                  )
            )
      );
   }

   private static String key(CommandContext<CommandSourceStack> ctx) {
      return StringArgumentType.getString(ctx, "key");
   }

   private static int sections(CommandSourceStack source) {
      Set<String> names = new LinkedHashSet<>();

      for (AwakenedConfigs.Entry entry : AwakenedConfigs.listed()) {
         int dot = entry.key().indexOf(46);
         names.add(dot > 0 ? entry.key().substring(0, dot) : entry.key());
      }

      MutableComponent line = Component.empty();
      boolean first = true;

      for (String name : names) {
         if (!first) {
            line.append(", ");
         }

         first = false;
         line.append(openable(name, name));
      }

      source.sendSuccess(() -> Component.translatable("commands.awakened.config.sections", new Object[]{line}), false);
      return names.size();
   }

   private static int show(CommandSourceStack source, String key) {
      if (retired(source, key)) {
         return 0;
      } else {
         AwakenedConfigs.Entry entry = AwakenedConfigs.find(key);
         if (entry == null) {
            return showSection(source, key);
         } else if (!loaded(source, entry)) {
            return 0;
         } else {
            ValueSpec spec = entry.value().getSpec();
            source.sendSuccess(
               () -> Component.translatable(
                     "commands.awakened.config.value",
                     new Object[]{
                        Component.literal(entry.key()).withStyle(ChatFormatting.YELLOW), display(entry.value().get()), plain(entry.value().getDefault())
                     }
                  ),
               false
            );
            Range<?> range = spec.getRange();
            List<String> allowed = entry.value() instanceof BooleanValue ? null : allowedWords(entry);
            if (range != null) {
               source.sendSuccess(
                  () -> Component.translatable("commands.awakened.config.range", new Object[]{plain(range.getMin()), plain(range.getMax())})
                        .withStyle(ChatFormatting.GRAY),
                  false
               );
            } else if (allowed != null) {
               source.sendSuccess(
                  () -> Component.translatable("commands.awakened.config.allowed", new Object[]{String.join(", ", allowed)}).withStyle(ChatFormatting.GRAY),
                  false
               );
            }

            String comment = spec.getComment();
            if (comment != null) {
               String text = comment.lines()
                  .map(String::strip)
                  .filter(l -> !l.isEmpty() && !l.startsWith("Default:") && !l.startsWith("Range:") && !l.startsWith("Allowed Values:"))
                  .collect(Collectors.joining(" "));
               if (!text.isEmpty()) {
                  source.sendSuccess(() -> Component.literal(text).withStyle(ChatFormatting.GRAY), false);
               }
            }

            return 1;
         }
      }
   }

   private static int showSection(CommandSourceStack source, String typed) {
      String section = sectionName(typed);
      if (section == null) {
         source.sendFailure(Component.translatable("commands.awakened.config.unknown", new Object[]{typed}));
         return 0;
      } else {
         String prefix = section + ".";
         List<AwakenedConfigs.Entry> inside = AwakenedConfigs.listed().stream().filter(e -> e.key().startsWith(prefix)).toList();
         Map<String, Integer> parts = new LinkedHashMap<>();
         List<AwakenedConfigs.Entry> direct = new ArrayList<>();

         for (AwakenedConfigs.Entry entry : inside) {
            String rest = entry.key().substring(prefix.length());
            int dot = rest.indexOf(46);
            if (dot > 0) {
               parts.merge(rest.substring(0, dot), 1, Integer::sum);
            } else {
               direct.add(entry);
            }
         }

         boolean grouped = inside.size() > 10 && !parts.isEmpty();
         source.sendSuccess(
            () -> Component.translatable(
                  grouped ? "commands.awakened.config.section_parts" : "commands.awakened.config.section",
                  new Object[]{Component.literal(section).withStyle(ChatFormatting.YELLOW)}
               ),
            false
         );

         for (AwakenedConfigs.Entry entryx : grouped ? direct : inside) {
            Component value = (Component)(entryx.spec().isLoaded() ? display(entryx.value().get()) : Component.literal("?"));
            source.sendSuccess(
               () -> Component.literal("  ").append(fillable(entry.key().substring(prefix.length()), entry.key())).append(" = ").append(value), false
            );
         }

         if (grouped) {
            parts.forEach(
               (part, count) -> source.sendSuccess(
                     () -> Component.literal("  ")
                           .append(openable(part, prefix + part))
                           .append(Component.translatable("commands.awakened.config.part_size", new Object[]{count}).withStyle(ChatFormatting.GRAY)),
                     false
                  )
            );
         }

         return inside.size();
      }
   }

   private static int change(CommandSourceStack source, String key, String raw) {
      AwakenedConfigs.Entry entry = find(source, key);
      if (entry == null) {
         return 0;
      } else {
         String typed = raw.trim();
         if (isList(entry)) {
            String[] words = typed.split("\\s+", 2);
            String op = words[0].toLowerCase(Locale.ROOT);
            String item = words.length > 1 ? words[1].trim() : "";
            if (op.equals("clear") && item.isEmpty()) {
               return write(source, entry, new ArrayList(), "[]");
            }

            if (op.equals("add") || op.equals("remove")) {
               if (item.isEmpty()) {
                  source.sendFailure(Component.translatable("commands.awakened.config.missing_entry", new Object[]{op}));
                  return 0;
               }

               return op.equals("add") ? add(source, entry, item) : remove(source, entry, item);
            }
         }

         return set(source, entry, typed);
      }
   }

   private static int set(CommandSourceStack source, AwakenedConfigs.Entry entry, String raw) {
      Object parsed = parse(entry, raw);
      if (parsed == null) {
         invalid(source, entry, raw);
         return 0;
      } else {
         if (parsed instanceof List) {
            for (Object item : (List)parsed) {
               if (!knownEntry(source, entry.key(), String.valueOf(item))) {
                  return 0;
               }
            }
         }

         return write(source, entry, parsed, raw);
      }
   }

   private static int reset(CommandSourceStack source, String key) {
      AwakenedConfigs.Entry entry = find(source, key);
      return entry == null ? 0 : write(source, entry, entry.value().getDefault(), plain(entry.value().getDefault()));
   }

   private static int add(CommandSourceStack source, AwakenedConfigs.Entry entry, String item) {
      List<String> current = list(entry);
      if (current.contains(item)) {
         source.sendFailure(Component.translatable("commands.awakened.config.list_has", new Object[]{entry.key(), item}));
         return 0;
      } else if (!knownEntry(source, entry.key(), item)) {
         return 0;
      } else {
         List<String> next = new ArrayList<>(current);
         if (AwakenedConfigs.isKeyed(entry.key())) {
            String name = AwakenedConfigs.nameOf(item);
            next.removeIf(e -> name != null && name.equals(AwakenedConfigs.nameOf(e)));
         }

         next.add(item);
         return write(source, entry, next, item);
      }
   }

   private static int remove(CommandSourceStack source, AwakenedConfigs.Entry entry, String item) {
      List<String> next = new ArrayList<>(list(entry));
      boolean byName = AwakenedConfigs.isKeyed(entry.key()) && item.indexOf(61) < 0;
      boolean removed = byName ? next.removeIf(e -> item.equals(AwakenedConfigs.nameOf(e))) : next.remove(item);
      if (!removed) {
         source.sendFailure(Component.translatable("commands.awakened.config.list_missing", new Object[]{entry.key(), item}));
         return 0;
      } else {
         return write(source, entry, next, item);
      }
   }

   private static int write(CommandSourceStack source, AwakenedConfigs.Entry entry, Object value, String shown) {
      if (!entry.value().getSpec().test(value)) {
         invalid(source, entry, shown);
         return 0;
      } else {
         AwakenedConfigs.Saved where = AwakenedConfigs.write((ConfigValue<Object>)entry.value(), value, source.getServer());
         if (ConfigFeedback.failed(
            source,
            where,
            () -> Component.translatable(
                  "commands.awakened.config.already", new Object[]{Component.literal(entry.key()).withStyle(ChatFormatting.YELLOW), display(value)}
               )
         )) {
            return 0;
         } else {
            source.sendSuccess(
               () -> Component.translatable(
                     "commands.awakened.config.set", new Object[]{Component.literal(entry.key()).withStyle(ChatFormatting.YELLOW), display(value)}
                  ),
               true
            );
            ConfigFeedback.scope(source, where, entry.value());
            return 1;
         }
      }
   }

   private static void invalid(CommandSourceStack source, AwakenedConfigs.Entry entry, String raw) {
      Range<?> range = entry.value().getSpec().getRange();
      List<String> allowed = allowedWords(entry);
      Component message;
      if (range != null) {
         message = Component.translatable(
            "commands.awakened.config.invalid_range", new Object[]{raw, entry.key(), plain(range.getMin()), plain(range.getMax())}
         );
      } else if (allowed != null) {
         message = Component.translatable("commands.awakened.config.invalid_choice", new Object[]{raw, entry.key(), String.join(", ", allowed)});
      } else if (entry.value().getDefault() instanceof Number) {
         message = Component.translatable("commands.awakened.config.invalid_number", new Object[]{raw, entry.key()});
      } else if (AwakenedConfigs.isKeyed(entry.key())) {
         message = Component.translatable("commands.awakened.config.invalid_pair", new Object[]{raw, entry.key()});
      } else {
         message = Component.translatable("commands.awakened.config.invalid", new Object[]{raw, entry.key()});
      }

      source.sendFailure(message);
   }

   private static boolean knownEntry(CommandSourceStack source, String key, String item) {
      if (AwakenedConfigs.accepts(key, item, source)) {
         return true;
      } else if (!AwakenedConfigs.isKeyed(key)) {
         boolean hasChoices = AwakenedConfigs.choices(key, source) != null;
         source.sendFailure(
            Component.translatable(hasChoices ? "commands.awakened.config.not_known" : "commands.awakened.config.invalid", new Object[]{item, key})
         );
         return false;
      } else {
         String name = AwakenedConfigs.nameOf(item);
         boolean unknownName = name != null && !AwakenedConfigs.knownName(key, name, source);
         source.sendFailure(
            unknownName
               ? Component.translatable("commands.awakened.config.not_known", new Object[]{name, key})
               : Component.translatable("commands.awakened.config.invalid_pair", new Object[]{item, key})
         );
         return false;
      }
   }

   private static boolean retired(CommandSourceStack source, String key) {
      String instead = AwakenedConfigs.replacementOf(key);
      if (instead == null) {
         return false;
      } else {
         source.sendFailure(Component.translatable("commands.awakened.config.retired", new Object[]{key, instead}));
         return true;
      }
   }

   @Nullable
   private static AwakenedConfigs.Entry find(CommandSourceStack source, String key) {
      if (retired(source, key)) {
         return null;
      } else {
         AwakenedConfigs.Entry entry = AwakenedConfigs.find(key);
         if (entry == null) {
            source.sendFailure(
               Component.translatable(sectionName(key) != null ? "commands.awakened.config.is_section" : "commands.awakened.config.unknown", new Object[]{key})
            );
            return null;
         } else {
            return loaded(source, entry) ? entry : null;
         }
      }
   }

   private static boolean loaded(CommandSourceStack source, AwakenedConfigs.Entry entry) {
      if (!entry.spec().isLoaded()) {
         source.sendFailure(Component.translatable("commands.awakened.config.unavailable"));
         return false;
      } else {
         return true;
      }
   }

   private static CompletableFuture<Suggestions> suggestList(AwakenedConfigs.Entry entry, CommandSourceStack source, SuggestionsBuilder builder) {
      String typed = builder.getRemaining();
      String lower = typed.toLowerCase(Locale.ROOT);

      for (String op : List.of("add ", "remove ")) {
         if (lower.startsWith(op)) {
            SuggestionsBuilder after = builder.createOffset(builder.getStart() + skipSpaces(typed, op.length()));
            Stream<String> options = op.equals("add ") ? addable(entry.key(), source) : (entry.spec().isLoaded() ? list(entry).stream() : Stream.empty());
            return SharedSuggestionProvider.suggest(options, after);
         }
      }

      int comma = typed.lastIndexOf(44);
      if (comma >= 0) {
         SuggestionsBuilder after = builder.createOffset(builder.getStart() + skipSpaces(typed, comma + 1));
         return SharedSuggestionProvider.suggest(addable(entry.key(), source), after);
      } else {
         return SharedSuggestionProvider.suggest(Stream.concat(LIST_OPS.stream(), addable(entry.key(), source)), builder);
      }
   }

   private static Stream<String> addable(String key, CommandSourceStack source) {
      Stream<String> names = AwakenedConfigs.keyChoices(key, source);
      if (names != null) {
         return names.map(name -> name + "=");
      } else {
         Stream<String> choices = AwakenedConfigs.choices(key, source);
         return choices == null ? Stream.empty() : choices;
      }
   }

   private static int skipSpaces(String text, int from) {
      int at = from;

      while (at < text.length() && text.charAt(at) == ' ') {
         at++;
      }

      return at;
   }

   private static boolean isList(AwakenedConfigs.Entry entry) {
      return entry.value().getDefault() instanceof List;
   }

   private static List<String> list(AwakenedConfigs.Entry entry) {
      return !entry.spec().isLoaded() ? List.of() : ((List)entry.value().get()).stream().map(String::valueOf).toList();
   }

   @Nullable
   private static List<String> allowedWords(AwakenedConfigs.Entry entry) {
      // $VF: Couldn't be decompiled
      // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
      // java.lang.NullPointerException: Cannot invoke "org.jetbrains.java.decompiler.struct.gen.VarType.isGeneric()" because "newRet" is null
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent.getInferredExprType(InvocationExprent.java:634)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent.getInferredExprType(InvocationExprent.java:503)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent.getInferredExprType(InvocationExprent.java:339)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent.getInferredExprType(InvocationExprent.java:339)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.getInferredExprType(FunctionExprent.java:287)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:966)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:895)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:90)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:833)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:261)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.RootStatement.toJava(RootStatement.java:36)
      //   at org.jetbrains.java.decompiler.main.ClassWriter.writeMethod(ClassWriter.java:1283)
      //
      // Bytecode:
      // 00: aload 0
      // 01: invokevirtual com/awakenedmod/awakened/core/AwakenedConfigs$Entry.value ()Lnet/neoforged/neoforge/common/ModConfigSpec$ConfigValue;
      // 04: instanceof net/neoforged/neoforge/common/ModConfigSpec$BooleanValue
      // 07: ifeq 14
      // 0a: ldc_w "on"
      // 0d: ldc_w "off"
      // 10: invokestatic java/util/List.of (Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;
      // 13: areturn
      // 14: aload 0
      // 15: invokevirtual com/awakenedmod/awakened/core/AwakenedConfigs$Entry.value ()Lnet/neoforged/neoforge/common/ModConfigSpec$ConfigValue;
      // 18: invokevirtual net/neoforged/neoforge/common/ModConfigSpec$ConfigValue.getSpec ()Lnet/neoforged/neoforge/common/ModConfigSpec$ValueSpec;
      // 1b: invokevirtual net/neoforged/neoforge/common/ModConfigSpec$ValueSpec.getClazz ()Ljava/lang/Class;
      // 1e: astore 1
      // 1f: aload 1
      // 20: ifnull 41
      // 23: aload 1
      // 24: invokevirtual java/lang/Class.isEnum ()Z
      // 27: ifeq 41
      // 2a: aload 1
      // 2b: invokevirtual java/lang/Class.getEnumConstants ()[Ljava/lang/Object;
      // 2e: invokestatic java/util/stream/Stream.of ([Ljava/lang/Object;)Ljava/util/stream/Stream;
      // 31: invokedynamic apply ()Ljava/util/function/Function; bsm=java/lang/invoke/LambdaMetafactory.metafactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[ (Ljava/lang/Object;)Ljava/lang/Object;, com/awakenedmod/awakened/core/command/ConfigCommand.lambda$allowedWords$22 (Ljava/lang/Object;)Ljava/lang/String;, (Ljava/lang/Object;)Ljava/lang/String; ]
      // 36: invokeinterface java/util/stream/Stream.map (Ljava/util/function/Function;)Ljava/util/stream/Stream; 2
      // 3b: invokeinterface java/util/stream/Stream.toList ()Ljava/util/List; 1
      // 40: areturn
      // 41: aconst_null
      // 42: areturn
   }

   @Nullable
   private static String sectionName(String typed) {
      String prefix = typed.toLowerCase(Locale.ROOT) + ".";

      for (AwakenedConfigs.Entry entry : AwakenedConfigs.listed()) {
         if (entry.key().toLowerCase(Locale.ROOT).startsWith(prefix)) {
            return entry.key().substring(0, typed.length());
         }
      }

      return null;
   }

   @Nullable
   private static Object parse(AwakenedConfigs.Entry entry, String raw) {
      // $VF: Couldn't be decompiled
      // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
      // java.lang.NullPointerException: Cannot invoke "org.jetbrains.java.decompiler.struct.gen.VarType.isGeneric()" because "newRet" is null
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent.getInferredExprType(InvocationExprent.java:634)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.DoStatement.toJava(DoStatement.java:146)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:833)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:833)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:241)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:833)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.RootStatement.toJava(RootStatement.java:36)
      //   at org.jetbrains.java.decompiler.main.ClassWriter.writeMethod(ClassWriter.java:1283)
      //
      // Bytecode:
      // 000: aload 0
      // 001: invokevirtual com/awakenedmod/awakened/core/AwakenedConfigs$Entry.value ()Lnet/neoforged/neoforge/common/ModConfigSpec$ConfigValue;
      // 004: astore 2
      // 005: aload 2
      // 006: instanceof net/neoforged/neoforge/common/ModConfigSpec$BooleanValue
      // 009: ifeq 0e6
      // 00c: aload 1
      // 00d: getstatic java/util/Locale.ROOT Ljava/util/Locale;
      // 010: invokevirtual java/lang/String.toLowerCase (Ljava/util/Locale;)Ljava/lang/String;
      // 013: astore 3
      // 014: bipush -1
      // 015: istore 4
      // 017: aload 3
      // 018: invokevirtual java/lang/String.hashCode ()I
      // 01b: lookupswitch 150 6 3521 137 3551 73 109935 121 119527 89 3569038 57 97196323 105
      // 054: aload 3
      // 055: ldc_w "true"
      // 058: invokevirtual java/lang/String.equals (Ljava/lang/Object;)Z
      // 05b: ifeq 0b1
      // 05e: bipush 0
      // 05f: istore 4
      // 061: goto 0b1
      // 064: aload 3
      // 065: ldc_w "on"
      // 068: invokevirtual java/lang/String.equals (Ljava/lang/Object;)Z
      // 06b: ifeq 0b1
      // 06e: bipush 1
      // 06f: istore 4
      // 071: goto 0b1
      // 074: aload 3
      // 075: ldc_w "yes"
      // 078: invokevirtual java/lang/String.equals (Ljava/lang/Object;)Z
      // 07b: ifeq 0b1
      // 07e: bipush 2
      // 07f: istore 4
      // 081: goto 0b1
      // 084: aload 3
      // 085: ldc_w "false"
      // 088: invokevirtual java/lang/String.equals (Ljava/lang/Object;)Z
      // 08b: ifeq 0b1
      // 08e: bipush 3
      // 08f: istore 4
      // 091: goto 0b1
      // 094: aload 3
      // 095: ldc_w "off"
      // 098: invokevirtual java/lang/String.equals (Ljava/lang/Object;)Z
      // 09b: ifeq 0b1
      // 09e: bipush 4
      // 09f: istore 4
      // 0a1: goto 0b1
      // 0a4: aload 3
      // 0a5: ldc_w "no"
      // 0a8: invokevirtual java/lang/String.equals (Ljava/lang/Object;)Z
      // 0ab: ifeq 0b1
      // 0ae: bipush 5
      // 0af: istore 4
      // 0b1: iload 4
      // 0b3: tableswitch 49 0 5 37 37 37 43 43 43
      // 0d8: getstatic java/lang/Boolean.TRUE Ljava/lang/Boolean;
      // 0db: goto 0e5
      // 0de: getstatic java/lang/Boolean.FALSE Ljava/lang/Boolean;
      // 0e1: goto 0e5
      // 0e4: aconst_null
      // 0e5: areturn
      // 0e6: aload 2
      // 0e7: instanceof net/neoforged/neoforge/common/ModConfigSpec$IntValue
      // 0ea: ifeq 0f5
      // 0ed: aload 1
      // 0ee: invokestatic java/lang/Integer.parseInt (Ljava/lang/String;)I
      // 0f1: invokestatic java/lang/Integer.valueOf (I)Ljava/lang/Integer;
      // 0f4: areturn
      // 0f5: aload 2
      // 0f6: instanceof net/neoforged/neoforge/common/ModConfigSpec$LongValue
      // 0f9: ifeq 104
      // 0fc: aload 1
      // 0fd: invokestatic java/lang/Long.parseLong (Ljava/lang/String;)J
      // 100: invokestatic java/lang/Long.valueOf (J)Ljava/lang/Long;
      // 103: areturn
      // 104: aload 2
      // 105: instanceof net/neoforged/neoforge/common/ModConfigSpec$DoubleValue
      // 108: ifeq 120
      // 10b: aload 1
      // 10c: invokestatic java/lang/Double.parseDouble (Ljava/lang/String;)D
      // 10f: dstore 3
      // 110: dload 3
      // 111: invokestatic java/lang/Double.isFinite (D)Z
      // 114: ifeq 11e
      // 117: dload 3
      // 118: invokestatic java/lang/Double.valueOf (D)Ljava/lang/Double;
      // 11b: goto 11f
      // 11e: aconst_null
      // 11f: areturn
      // 120: goto 126
      // 123: astore 3
      // 124: aconst_null
      // 125: areturn
      // 126: aload 2
      // 127: invokevirtual net/neoforged/neoforge/common/ModConfigSpec$ConfigValue.getSpec ()Lnet/neoforged/neoforge/common/ModConfigSpec$ValueSpec;
      // 12a: invokevirtual net/neoforged/neoforge/common/ModConfigSpec$ValueSpec.getClazz ()Ljava/lang/Class;
      // 12d: astore 3
      // 12e: aload 3
      // 12f: ifnull 16f
      // 132: aload 3
      // 133: invokevirtual java/lang/Class.isEnum ()Z
      // 136: ifeq 16f
      // 139: aload 3
      // 13a: invokevirtual java/lang/Class.getEnumConstants ()[Ljava/lang/Object;
      // 13d: astore 4
      // 13f: aload 4
      // 141: arraylength
      // 142: istore 5
      // 144: bipush 0
      // 145: istore 6
      // 147: iload 6
      // 149: iload 5
      // 14b: if_icmpge 16d
      // 14e: aload 4
      // 150: iload 6
      // 152: aaload
      // 153: astore 7
      // 155: aload 7
      // 157: checkcast java/lang/Enum
      // 15a: invokevirtual java/lang/Enum.name ()Ljava/lang/String;
      // 15d: aload 1
      // 15e: invokevirtual java/lang/String.equalsIgnoreCase (Ljava/lang/String;)Z
      // 161: ifeq 167
      // 164: aload 7
      // 166: areturn
      // 167: iinc 6 1
      // 16a: goto 147
      // 16d: aconst_null
      // 16e: areturn
      // 16f: aload 2
      // 170: invokevirtual net/neoforged/neoforge/common/ModConfigSpec$ConfigValue.getDefault ()Ljava/lang/Object;
      // 173: instanceof java/util/List
      // 176: ifeq 1da
      // 179: aload 1
      // 17a: ldc_w "["
      // 17d: invokevirtual java/lang/String.startsWith (Ljava/lang/String;)Z
      // 180: ifeq 19b
      // 183: aload 1
      // 184: ldc_w "]"
      // 187: invokevirtual java/lang/String.endsWith (Ljava/lang/String;)Z
      // 18a: ifeq 19b
      // 18d: aload 1
      // 18e: bipush 1
      // 18f: aload 1
      // 190: invokevirtual java/lang/String.length ()I
      // 193: bipush 1
      // 194: isub
      // 195: invokevirtual java/lang/String.substring (II)Ljava/lang/String;
      // 198: goto 19c
      // 19b: aload 1
      // 19c: astore 4
      // 19e: aload 4
      // 1a0: ldc_w "none"
      // 1a3: invokevirtual java/lang/String.equalsIgnoreCase (Ljava/lang/String;)Z
      // 1a6: ifeq 1ae
      // 1a9: ldc_w ""
      // 1ac: astore 4
      // 1ae: new java/util/ArrayList
      // 1b1: dup
      // 1b2: aload 4
      // 1b4: ldc_w ","
      // 1b7: invokevirtual java/lang/String.split (Ljava/lang/String;)[Ljava/lang/String;
      // 1ba: invokestatic java/util/stream/Stream.of ([Ljava/lang/Object;)Ljava/util/stream/Stream;
      // 1bd: invokedynamic apply ()Ljava/util/function/Function; bsm=java/lang/invoke/LambdaMetafactory.metafactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[ (Ljava/lang/Object;)Ljava/lang/Object;, java/lang/String.trim ()Ljava/lang/String;, (Ljava/lang/String;)Ljava/lang/String; ]
      // 1c2: invokeinterface java/util/stream/Stream.map (Ljava/util/function/Function;)Ljava/util/stream/Stream; 2
      // 1c7: invokedynamic test ()Ljava/util/function/Predicate; bsm=java/lang/invoke/LambdaMetafactory.metafactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[ (Ljava/lang/Object;)Z, com/awakenedmod/awakened/core/command/ConfigCommand.lambda$parse$23 (Ljava/lang/String;)Z, (Ljava/lang/String;)Z ]
      // 1cc: invokeinterface java/util/stream/Stream.filter (Ljava/util/function/Predicate;)Ljava/util/stream/Stream; 2
      // 1d1: invokeinterface java/util/stream/Stream.toList ()Ljava/util/List; 1
      // 1d6: invokespecial java/util/ArrayList.<init> (Ljava/util/Collection;)V
      // 1d9: areturn
      // 1da: aload 2
      // 1db: invokevirtual net/neoforged/neoforge/common/ModConfigSpec$ConfigValue.getDefault ()Ljava/lang/Object;
      // 1de: instanceof java/lang/String
      // 1e1: ifeq 1e8
      // 1e4: aload 1
      // 1e5: goto 1e9
      // 1e8: aconst_null
      // 1e9: areturn
   }

   private static String plain(Object value) {
      if (value instanceof Boolean on) {
         return on ? "on" : "off";
      } else if (value instanceof Enum<?> constant) {
         return constant.name();
      } else {
         if (value instanceof Double number && number == Math.rint(number) && Math.abs(number) < 1.0E15) {
            return String.valueOf(number.longValue());
         }

         if (value instanceof List<?> list) {
            return list.isEmpty() ? "[]" : "[" + String.join(", ", list.stream().map(String::valueOf).toList()) + "]";
         } else {
            return String.valueOf(value);
         }
      }
   }

   private static Component display(Object value) {
      return (Component)(value instanceof Boolean on ? OnOffCommand.state(on) : Component.literal(plain(value)).withStyle(ChatFormatting.GREEN));
   }

   private static MutableComponent fillable(String label, String key) {
      return Component.literal(label)
         .withStyle(
            s -> s.withColor(ChatFormatting.YELLOW)
                  .withClickEvent(ChatFx.suggestCommand("/awakened config " + key + " "))
                  .withHoverEvent(ChatFx.showText(Component.translatable("commands.awakened.config.hover_fill")))
         );
   }

   private static MutableComponent openable(String label, String section) {
      return Component.literal(label)
         .withStyle(
            s -> s.withColor(ChatFormatting.YELLOW)
                  .withClickEvent(ChatFx.runCommand("awakened config " + section))
                  .withHoverEvent(ChatFx.showText(Component.translatable("commands.awakened.config.hover_open")))
         );
   }
}
