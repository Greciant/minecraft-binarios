package com.devilfruits.fruits.doru_doru.client.atelier;

import com.devilfruits.DevilFruits;
import com.devilfruits.fruits.doru_doru.atelier.SculptRequest;
import com.devilfruits.fruits.doru_doru.wax.WaxPalette;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class SculptHud {
   static final ResourceLocation CURSOR = DevilFruits.id("hud/doru_doru/sculpt_cursor");
   static final ResourceLocation CURSOR_BAD = DevilFruits.id("hud/doru_doru/sculpt_cursor_bad");

   public static void onLayer(Pre event) {
      if (event.getName().equals(VanillaGuiLayers.CROSSHAIR) && SculptClient.active()) {
         event.setCanceled(true);
      }
   }

   public static void render(GuiGraphics gg, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      if (SculptClient.active() && !mc.options.hideGui && mc.screen == null) {
         int cx = gg.guiWidth() / 2;
         int cy = gg.guiHeight() / 2;
         SculptClient.Ghost g = SculptClient.ghost;
         boolean bad = g != null && !g.ok();
         gg.blitSprite(bad ? CURSOR_BAD : CURSOR, cx - 7, cy - 7, 15, 15);
         Font font = mc.font;
         int x = cx + 14;
         int y = cy - 22;

         MutableComponent title = switch (SculptClient.tool) {
            case SHAPE -> Component.translatable("hud.devilfruits.doru_doru.shape." + SculptClient.kind.key());
            case BRUSH -> Component.translatable("hud.devilfruits.doru_doru.brush." + SculptClient.brushMode);
            case CAST -> Component.translatable(
            SculptClient.castCapture ? "hud.devilfruits.doru_doru.cast.capture" : "hud.devilfruits.doru_doru.cast.place",
            new Object[]{SculptClient.castSlot + 1}
         );
            case DISSOLVE -> Component.translatable("hud.devilfruits.doru_doru.tool.dissolve");
         };
         title.withStyle(ChatFormatting.WHITE);
         int needed = SculptClient.needed();
         if (needed > 1) {
            title.append(Component.literal("  " + SculptClient.points.size() + "/" + needed).withStyle(ChatFormatting.GRAY));
         }

         gg.drawString(font, title, x, y, 16777215, true);
         y += 10;
         MutableComponent params = Component.empty();
         List<String> names = SculptClient.paramNames();

         for (int i = 0; i < names.size(); i++) {
            float v = SculptClient.paramValue(i);
            String shown = names.get(i).equals("mirror") ? (v >= 0.5F ? "✔" : "✘") : (names.get(i).equals("turn") ? Math.round(v) + "°" : fmt(v));
            MutableComponent one = Component.translatable("hud.devilfruits.doru_doru.param." + names.get(i)).append(" " + shown);
            params.append(
               i == SculptClient.param ? Component.literal("[").append(one).append("]").withStyle(ChatFormatting.YELLOW) : one.withStyle(ChatFormatting.GRAY)
            );
            params.append("  ");
         }

         if (SculptClient.tool == SculptRequest.Tool.BRUSH && SculptClient.viscosity > 0.0F) {
            params.append(
               Component.translatable("hud.devilfruits.doru_doru.viscosity", new Object[]{Math.round(SculptClient.viscosity * 100.0F)})
                  .withStyle(ChatFormatting.AQUA)
            );
         }

         gg.drawString(font, params, x, y, 16777215, true);
         y += 10;
         MutableComponent flags = Component.empty();
         if (SculptClient.hollow) {
            flags.append(Component.translatable("hud.devilfruits.doru_doru.flag.hollow").withStyle(ChatFormatting.AQUA)).append("  ");
         }

         if (SculptClient.symmetry != SculptRequest.Symmetry.NONE) {
            Component sym = SculptClient.symmetry == SculptRequest.Symmetry.RADIAL
               ? Component.translatable("hud.devilfruits.doru_doru.symmetry.radial", new Object[]{SculptClient.folds})
               : Component.translatable("hud.devilfruits.doru_doru.symmetry." + SculptClient.symmetry.name().toLowerCase(Locale.ROOT));
            flags.append(sym.copy().withStyle(ChatFormatting.AQUA)).append("  ");
         }

         flags.append(
            Component.translatable("hud.devilfruits.doru_doru.snap." + SculptClient.snap.name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.DARK_GRAY)
         );
         gg.drawString(font, flags, x + 10, y, 16777215, true);
         int dye = WaxPalette.tint(SculptClient.color, false);
         gg.fill(x, y, x + 7, y + 7, 0xFF000000 | dye);
         gg.renderOutline(x - 1, y - 1, 9, 9, -12700380);
         y += 10;
         if (g != null) {
            Component line;
            if (g.ok() && g.mode() != SculptClient.Mode.ADD) {
               line = Component.translatable("hud.devilfruits.doru_doru.affects", new Object[]{String.format(Locale.ROOT, "%.1f", g.blocks())})
                  .withStyle(ChatFormatting.GREEN);
            } else if (g.ok()) {
               line = Component.translatable(
                     "hud.devilfruits.doru_doru.cost",
                     new Object[]{String.format(Locale.ROOT, "%.1f", g.cost()), String.format(Locale.ROOT, "%.1f", g.blocks())}
                  )
                  .withStyle(ChatFormatting.GREEN);
            } else {
               line = Component.translatable("hud.devilfruits.doru_doru.problem." + g.problem(), new Object[]{g.unsupported()}).withStyle(ChatFormatting.RED);
            }

            gg.drawString(font, line, x, y, 16777215, true);
         }
      }
   }

   private static String fmt(float v) {
      return v == (float)Math.round(v) ? String.valueOf(Math.round(v)) : String.format(Locale.ROOT, "%.2f", v).replaceAll("0+$", "");
   }

   private SculptHud() {
   }
}
