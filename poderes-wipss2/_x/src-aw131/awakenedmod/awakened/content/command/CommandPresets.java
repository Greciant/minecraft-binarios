package com.awakenedmod.awakened.content.command;

import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.Target;
import com.awakenedmod.awakened.content.command.vocab.Trigger;
import java.util.List;
import net.minecraft.network.chat.Component;

public final class CommandPresets {
   private static final List<CommandPresets.Starter> STARTERS = List.of(
      new CommandPresets.Starter("night_eyes", new Rule("me", "sees_in_the_dark", 1, 10, "always")),
      new CommandPresets.Starter("sprint", new Rule("me", "moves_at", 2, 10, "while_sprinting")),
      new CommandPresets.Starter("bleed", new Rule("what_i_hit", "bleeds", 2, 10, "always")),
      new CommandPresets.Starter("freeze_on_hit", new Rule("what_i_hit", "cannot_move", 1, 3, "always")),
      new CommandPresets.Starter("push_back", new Rule("what_hits_me", "is_pushed", 2, 10, "always")),
      new CommandPresets.Starter("minefield", new Rule("mobs_near_me", "explodes", 4, 4, 10, "always"))
   );

   private CommandPresets() {
   }

   public static List<CommandPresets.Starter> all() {
      return STARTERS;
   }

   public static record Starter(String id, Rule rule) {
      public Component displayName() {
         return Component.translatable("awakened.command.preset." + this.id);
      }

      public int unlockLevel() {
         Target target = this.rule.target();
         Effect effect = this.rule.effect();
         Trigger trigger = this.rule.trigger();
         if (target != null && effect != null && trigger != null) {
            int needed = Math.max(target.unlockLevel(), Math.max(effect.unlockLevel(), trigger.unlockLevel()));
            needed = Math.max(needed, CommandCost.masteryForDuration(this.rule.seconds()));
            if (this.rule.levelled()) {
               needed = Math.max(needed, CommandCost.masteryForLevel(this.rule.level()));
            }

            if (this.rule.radial()) {
               needed = Math.max(needed, CommandCost.masteryForLevel(this.rule.radius()));
            }

            return needed;
         } else {
            return Integer.MAX_VALUE;
         }
      }
   }
}
