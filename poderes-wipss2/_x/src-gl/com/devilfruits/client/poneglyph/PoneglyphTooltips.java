package com.devilfruits.client.poneglyph;

import com.devilfruits.core.poneglyph.BlueprintData;
import com.devilfruits.core.poneglyph.PoneglyphData;
import com.devilfruits.core.poneglyph.PoneglyphNetwork;
import com.devilfruits.core.poneglyph.Poneglyphs;
import com.devilfruits.core.seastone.SeastoneGear;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class PoneglyphTooltips {
   private static final String[] RU = new String[]{
      "a",
      "b",
      "v",
      "g",
      "d",
      "e",
      "zh",
      "z",
      "i",
      "i",
      "k",
      "l",
      "m",
      "n",
      "o",
      "p",
      "r",
      "s",
      "t",
      "u",
      "f",
      "h",
      "c",
      "ch",
      "sh",
      "sh",
      "",
      "y",
      "",
      "e",
      "yu",
      "ya"
   };

   static void onTooltip(ItemTooltipEvent event) {
      ItemStack stack = event.getItemStack();
      List<Component> tip = event.getToolTip();
      boolean readable = ClientPoneglyphState.readable(event.getEntity());
      int at = Math.min(1, tip.size());
      BlueprintData.Rubbing rubbing = (BlueprintData.Rubbing)stack.get((DataComponentType)Poneglyphs.RUBBING.get());
      BlueprintData.Encrypted encrypted = (BlueprintData.Encrypted)stack.get((DataComponentType)Poneglyphs.ENCRYPTED.get());
      BlueprintData.Deciphered deciphered = (BlueprintData.Deciphered)stack.get((DataComponentType)Poneglyphs.DECIPHERED.get());
      if (deciphered != null) {
         tip.add(
            at,
            ancient(
               Component.translatable("poneglyph.devilfruits.tooltip.subject", new Object[]{Component.translatable(knowledgeKey(deciphered.knowledge()))}),
               readable,
               ChatFormatting.GOLD
            )
         );
      } else if (encrypted != null || rubbing != null) {
         ResourceLocation id = encrypted != null ? encrypted.poneglyph() : rubbing.poneglyph();
         PoneglyphNetwork.PoneglyphInfo info = ClientPoneglyphState.poneglyph(id);
         if (info != null) {
            if (encrypted != null) {
               tip.add(
                  at,
                  ancient(
                     Component.translatable("poneglyph.devilfruits.tooltip.subject", new Object[]{Component.translatable(knowledgeKey(info.knowledge()))}),
                     readable,
                     ChatFormatting.DARK_AQUA
                  )
               );
            }

            tip.add(at, ancient(Component.translatable(info.loreKey()), readable, ChatFormatting.GRAY));
         }
      }

      if (stack.is(SeastoneGear.GEAR)) {
         tip.add(Component.translatable("item.devilfruits.seastone_gear.lore").withStyle(ChatFormatting.DARK_AQUA));
      }

      if ((rubbing != null || encrypted != null || deciphered != null) && !readable) {
         tip.add(
            Component.translatable("poneglyph.devilfruits.tooltip.need_primer")
               .withStyle(new ChatFormatting[]{ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC})
         );
      }
   }

   private static String knowledgeKey(ResourceLocation id) {
      return PoneglyphData.Knowledge.nameKey(id.getNamespace(), id.getPath());
   }

   public static Component ancient(Component text, boolean readable, ChatFormatting color) {
      return readable
         ? text.copy().withStyle(new ChatFormatting[]{color, ChatFormatting.ITALIC})
         : ClientPoneglyphState.glyphs(latin(text.getString())).withStyle(color);
   }

   public static String latin(@Nullable String text) {
      if (text == null) {
         return "";
      } else {
         StringBuilder sb = new StringBuilder();

         for (char c : text.toLowerCase(Locale.ROOT).toCharArray()) {
            if ((c < 'a' || c > 'z') && c != ' ') {
               if (c >= 1072 && c <= 1103) {
                  sb.append(RU[c - 1072]);
               } else if (c == 1105) {
                  sb.append('e');
               }
            } else {
               sb.append(c);
            }
         }

         return sb.toString().replaceAll(" +", " ").trim();
      }
   }

   static String localized(String key) {
      return I18n.get(key, new Object[0]);
   }

   private PoneglyphTooltips() {
   }
}
