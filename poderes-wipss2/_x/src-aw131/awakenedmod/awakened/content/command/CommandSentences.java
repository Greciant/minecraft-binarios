package com.awakenedmod.awakened.content.command;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

public final class CommandSentences {
   private static final String[] NUMERAL = new String[]{"I", "II", "III", "IV", "V"};

   private CommandSentences() {
   }

   public static String numeral(int slotIndex) {
      return slotIndex >= 0 && slotIndex < NUMERAL.length ? NUMERAL[slotIndex] : String.valueOf(slotIndex + 1);
   }

   public static List<Component> onSentences(Player caster) {
      CommandData rules = CommandData.of(caster);
      List<Component> lines = new ArrayList<>(5);

      for (int i = 0; i < 5; i++) {
         RuleSlot slot = rules.slot(i);
         if (slot.on() && slot.rule().resolves()) {
            String name = rules.name(i);
            MutableComponent line = Component.literal(name.isEmpty() ? numeral(i) + ". " : name + ": ").append(slot.rule().describe(slot.secondsLeft()));
            if (slot.bound() && slot.boundDormant()) {
               line.append(" ").append(Component.translatable("awakened.command.bound_waiting"));
            }

            lines.add(line);
         }
      }

      return lines;
   }

   public static String onSlotLabel(Player caster) {
      CommandData rules = CommandData.of(caster);
      StringBuilder label = new StringBuilder(10);

      for (int i = 0; i < 5; i++) {
         if (rules.slot(i).on()) {
            if (label.length() > 0) {
               label.append(' ');
            }

            label.append(i + 1);
         }
      }

      return label.toString();
   }

   public static List<String> returnLabels(Player caster) {
      CommandData rules = CommandData.of(caster);
      int soonest = Integer.MAX_VALUE;
      float soonestHeld = 0.0F;
      float endless = 0.0F;

      for (int i = 0; i < 5; i++) {
         RuleSlot slot = rules.slot(i);
         if (slot.on() && !(slot.held() <= 0.5F)) {
            if (slot.holdEndsIn() < 0) {
               endless = Math.max(endless, slot.held());
            } else if (slot.holdEndsIn() > 0 && slot.holdEndsIn() < soonest) {
               soonest = slot.holdEndsIn();
               soonestHeld = slot.held();
            }
         }
      }

      if (soonest != Integer.MAX_VALUE) {
         String seconds = seconds(soonest);
         return List.of("+" + Math.round(soonestHeld) + " in " + seconds, seconds);
      } else {
         return endless > 0.0F ? List.of("+" + Math.round(endless) + " held") : List.of();
      }
   }

   public static String seconds(int ticks) {
      return Math.max(1, Math.round((float)ticks / 20.0F)) + "s";
   }

   public static boolean anyLive(Player caster) {
      CommandData rules = CommandData.of(caster);

      for (int i = 0; i < 5; i++) {
         RuleSlot slot = rules.slot(i);
         if (slot.on() && slot.rule().resolves() && slot.rule().trigger().liveOnClient(caster)) {
            return true;
         }
      }

      return false;
   }
}
