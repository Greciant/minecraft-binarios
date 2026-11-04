package com.awakenedmod.awakened.content.command;

import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.Trigger;
import com.awakenedmod.awakened.content.command.vocab.Vocabulary;
import com.awakenedmod.awakened.content.command.vocab.effect.ReturnsHereEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.SwapsWithMeEffect;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;

public final class CommandState {
   private static final List<CommandState.Holder> HOLDERS = List.of(
      new CommandState.Holder("engine", (server, id) -> RuleEngine.forgetCaster(id), server -> RuleEngine.forgetAll()),
      new CommandState.Holder("landings", Landings::clearCaster, Landings::clearAll),
      new CommandState.Holder("sealed", (server, id) -> SealedRules.forget(id), server -> SealedRules.forgetAll()),
      new CommandState.Holder("feats", (server, id) -> CommandFeats.forget(id), server -> CommandFeats.forgetAll()),
      new CommandState.Holder("trace", (server, id) -> CommandTrace.forget(id), server -> CommandTrace.forgetAll()),
      new CommandState.Holder(
         "trigger_words", (server, id) -> Vocabulary.triggers().forEach(word -> word.forget(id)), server -> Vocabulary.triggers().forEach(Trigger::forgetAll)
      ),
      new CommandState.Holder("effect_words", (server, id) -> {
      }, server -> Vocabulary.effects().forEach(Effect::forgetAllBodies)),
      new CommandState.Holder("returns_here", (server, id) -> ReturnsHereEffect.forgetCaster(id), server -> ReturnsHereEffect.forgetAll()),
      new CommandState.Holder("swaps_with_me", (server, id) -> {
      }, server -> SwapsWithMeEffect.forgetAll())
   );

   private CommandState() {
   }

   public static void clearCaster(@Nullable MinecraftServer server, UUID casterId) {
      for (CommandState.Holder holder : HOLDERS) {
         holder.forCaster().accept(server, casterId);
      }
   }

   public static void clearAll(@Nullable MinecraftServer server) {
      for (CommandState.Holder holder : HOLDERS) {
         holder.forAll().accept(server);
      }
   }

   public static List<String> names() {
      return HOLDERS.stream().map(CommandState.Holder::name).toList();
   }

   private static record Holder(String name, BiConsumer<MinecraftServer, UUID> forCaster, Consumer<MinecraftServer> forAll) {
   }
}
