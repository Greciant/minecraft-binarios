package com.awakenedmod.awakened.content.relic;

import com.awakenedmod.awakened.core.relic.Relic;
import com.awakenedmod.awakened.core.relic.RelicEffect;
import com.awakenedmod.awakened.core.skill.Stat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class RelicTooltip {
   private RelicTooltip() {
   }

   public static List<Component> effectLines(Relic relic) {
      List<Component> lines = new ArrayList<>(relic.effects().size());

      for (RelicEffect effect : relic.effects()) {
         lines.add(line(effect));
      }

      return lines;
   }

   private static Component line(RelicEffect effect) {
      float amount = effect.baseAmount();
      ChatFormatting color = amount < 0.0F ? ChatFormatting.RED : ChatFormatting.GREEN;
      MutableComponent line = Component.literal(value(effect.stat(), amount) + " ").append(effect.stat().displayName()).withStyle(color);
      Component when = effect.condition().describe();
      if (when != null) {
         line.append(Component.literal(" ").append(when).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}));
      }

      return line;
   }

   public static String value(Stat stat, float amount) {
      String sign = amount < 0.0F ? "" : "+";
      return stat == Stat.ABILITY_POWER
         ? sign + Math.round(amount * 100.0F) + "%"
         : sign + ((double)amount == Math.rint((double)amount) ? Integer.toString((int)amount) : Float.toString(amount));
   }
}
