package com.awakenedmod.awakened.client.screen.card.faces;

import com.awakenedmod.awakened.client.screen.RelicBeltScreen;
import com.awakenedmod.awakened.client.screen.card.CardContext;
import com.awakenedmod.awakened.client.screen.card.CardFace;
import com.awakenedmod.awakened.client.screen.card.CardGfx;
import com.awakenedmod.awakened.client.screen.card.CardStyle;
import com.awakenedmod.awakened.content.relic.RelicBeltItem;
import com.awakenedmod.awakened.content.relic.RelicItem;
import com.awakenedmod.awakened.content.relic.RelicTooltip;
import com.awakenedmod.awakened.core.net.OpenBeltPayload;
import com.awakenedmod.awakened.core.relic.Condition;
import com.awakenedmod.awakened.core.relic.Relic;
import com.awakenedmod.awakened.core.relic.RelicContext;
import com.awakenedmod.awakened.core.relic.RelicEffect;
import com.awakenedmod.awakened.core.relic.RelicRegistry;
import com.awakenedmod.awakened.core.skill.Stat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RelicsFace implements CardFace {
   private static final int SLOT = 22;
   private static final int ROW = 28;
   private static final int BOX = 48;
   private static final int CARRY_PITCH = 25;
   private static final int BELT_H = 78;
   private static final int HEALTHY_PCT = Math.round(75.0F);
   private static final Map<String, String> COND_FALLBACK = Map.of(
      "while_healthy", "at %s%% health or more", "when_wounded", "near death", "while_in_water", "in water", "while_sprinting", "while sprinting"
   );

   @Override
   public int height(CardContext ctx, int w, boolean compact) {
      if (!ctx.state.hasRelicBelt()) {
         return 14 + ctx.lines(noBeltHint(ctx), w) * 9 + 4 + (carryingBelt(ctx) ? 12 : 0);
      } else {
         int carriedRows = rows(carried(ctx).size(), w);
         return 100 + (combines(worn(ctx)) ? 18 : 0) + 12 + carriedRows * 22 + (carriedRows - 1) * 3 + 8 + 6 + 11 + detailReserve(ctx, w, compact) * 10;
      }
   }

   private static int perRow(int w) {
      return Math.max(1, (w + 25 - 22) / 25);
   }

   private static int rows(int n, int w) {
      return Math.max(1, (n + perRow(w) - 1) / perRow(w));
   }

   private static int detailReserve(CardContext ctx, int w, boolean compact) {
      int most = 0;

      for (Relic r : RelicRegistry.all()) {
         most = Math.max(most, loreLines(ctx, r, w, compact) + r.effects().size());
      }

      return most;
   }

   private static int loreLines(CardContext ctx, Relic r, int w, boolean compact) {
      return !compact && r.isSignature() ? ctx.lines(r.description(), w) : 0;
   }

   @Override
   public void draw(CardGfx gfx, CardContext ctx, int x, int y, int w, boolean compact) {
      gfx.textBold(Component.translatable("awakened.screen.relics_belt"), x, y, -1185569);
      if (!ctx.state.hasRelicBelt()) {
         this.noBelt(gfx, ctx, x, y + 14, w);
      } else {
         gfx.chip(
            x + w, y - 2, Component.translatableWithFallback("awakened.card.relics.open", "Open belt").getString(), true, "relics/open", RelicsFace::openBelt
         );
         RelicContext live = live(ctx);
         List<String> worn = worn(ctx);
         List<RelicsFace.Carried> carried = carried(ctx);
         int hovBelt = hoveredBelt(gfx, worn);
         int hovCarry = hoveredCarried(gfx, carried.size());
         int top = y + 14;
         this.portrait(gfx, ctx, x, top);
         this.belt(gfx, live, worn, hovBelt, hovCarry < 0, x + 48 + 8, top, w - 48 - 8);
         int yy = top + 78 + 8;
         if (combines(worn)) {
            this.total(gfx, worn, x, yy);
            yy += 18;
         }

         yy = this.carriedRow(gfx, carried, hovCarry, x, yy, w) + 8;
         String focus = hovBelt >= 0
            ? worn.get(hovBelt)
            : (hovCarry >= 0 ? carried.get(hovCarry).id() : (!worn.isEmpty() ? worn.get(0) : (carried.isEmpty() ? null : carried.get(0).id())));
         gfx.rule(x, yy, w);
         this.detail(gfx, ctx, live, RelicRegistry.get(focus), x, yy + 6, w, compact);
      }
   }

   private void noBelt(CardGfx gfx, CardContext ctx, int x, int y, int w) {
      int yy = y + gfx.paragraph(noBeltHint(ctx), x, y, w, -6054250, 8) * 9 + 4;
      if (carryingBelt(ctx)) {
         gfx.chip(x, yy, Component.translatable("awakened.screen.relics_belt_equip").getString(), false, "relics/equip", RelicsFace::openBelt);
      }
   }

   private static Component noBeltHint(CardContext ctx) {
      return Component.translatable(carryingBelt(ctx) ? "awakened.screen.relics_equip_hint" : "awakened.screen.relics_get_hint");
   }

   private void portrait(CardGfx gfx, CardContext ctx, int x, int y) {
      gfx.fill(x, y, 48, 78, -15461872);
      gfx.fill(x + 1, y + 1, 46, 76, -12238791);
      if (ctx.mc.player != null) {
         gfx.entity(ctx.mc.player, x + 2, y + 2, x + 48 - 2, y + 78 - 2, 30);
      }
   }

   private void belt(CardGfx gfx, RelicContext live, List<String> worn, int hovBelt, boolean noCarryHover, int rx, int top, int rw) {
      for (int i = 0; i < 3; i++) {
         int ry = top + i * 28;
         Relic r = i < worn.size() ? RelicRegistry.get(worn.get(i)) : null;
         boolean focus = r != null && (hovBelt == i || hovBelt < 0 && noCarryHover && i == 0);
         gfx.slot(rx, ry, 22, focus ? CardStyle.ring(gfx.band) : 0);
         gfx.hit(rx, ry, rw, 22, "relics/belt/" + i, RelicsFace::openBelt);
         if (r == null) {
            gfx.text(Component.translatableWithFallback("awakened.card.relics.empty", "Empty"), rx + 28, ry + 7, -6054250);
         } else {
            gfx.item(RelicItem.of(r.id()), rx + 3, ry + 3);
            if (hovBelt == i) {
               gfx.slotHover(rx, ry, 22);
            }

            gfx.textBold(r.displayName(), rx + 28, ry + 1, rarityColor(r));
            this.effectLine(gfx, live, headline(r), rx + 28, ry + 12, rw - 28);
         }
      }
   }

   private void total(CardGfx gfx, List<String> worn, int x, int y) {
      int lx = x + gfx.text(Component.translatableWithFallback("awakened.card.relics.total", "Total:"), x, y, -3554376) + 5;
      Map<Stat, Float> sums = plainSums(worn);
      int k = 0;

      for (Entry<Stat, Float> e : sums.entrySet()) {
         Component t = Component.literal(RelicTooltip.value(e.getKey(), e.getValue()) + " ").append(e.getKey().displayName());
         lx += gfx.text(t, lx, y, e.getValue() < 0.0F ? -43691 : -11141291);
         if (++k < sums.size()) {
            lx += gfx.text(", ", lx, y, -3554376);
         }
      }
   }

   private int carriedRow(CardGfx gfx, List<RelicsFace.Carried> carried, int hov, int x, int y, int w) {
      gfx.textBold(Component.translatable("awakened.screen.relics_carried"), x, y, -1185569);
      int yy = y + 12;
      if (carried.isEmpty()) {
         gfx.text(Component.translatable("awakened.screen.relics_none"), x, yy + 7, -6054250);
         return yy + 22;
      } else {
         gfx.textRight(Component.translatableWithFallback("awakened.card.relics.carried_hint", "Click one to open the belt"), x + w, y, -6054250);
         int per = perRow(w);

         for (int i = 0; i < carried.size(); i++) {
            int sx = x + i % per * 25;
            int sy = yy + i / per * 25;
            RelicsFace.Carried c = carried.get(i);
            gfx.slot(sx, sy, 22, hov == i ? CardStyle.ring(gfx.band) : 0);
            gfx.itemWithCount(RelicItem.of(c.id()).copyWithCount(c.count()), sx + 3, sy + 3);
            if (hov == i) {
               gfx.slotHover(sx, sy, 22);
            }

            gfx.hit(sx, sy, 22, 22, "relics/carried/" + i, RelicsFace::openBelt);
         }

         int rows = rows(carried.size(), w);
         return yy + rows * 22 + (rows - 1) * 3;
      }
   }

   private void detail(CardGfx gfx, CardContext ctx, RelicContext live, Relic r, int x, int y, int w, boolean compact) {
      if (r != null) {
         gfx.textBold(r.displayName(), x, y, rarityColor(r));
         gfx.textRight(rarityName(r.rarity()), x + w, y, -3554376);
         int yy = y + 11;
         int lore = loreLines(ctx, r, w, compact);
         if (lore > 0) {
            List<String> lines = gfx.wrap(r.description(), w);

            for (int i = 0; i < lore; i++) {
               boolean cut = i == lore - 1 && lines.size() > lore;
               gfx.text(cut ? gfx.clip(lines.get(i) + "...", w) : lines.get(i), x, yy, -3554376);
               yy += 10;
            }
         }

         for (RelicEffect e : r.effects()) {
            this.effectLine(gfx, live, e, x, yy, w);
            yy += 10;
         }
      }
   }

   private void effectLine(CardGfx gfx, RelicContext live, RelicEffect e, int x, int y, int maxW) {
      Condition cond = e.condition();
      float factor = live == null ? 0.0F : cond.factor(live);
      boolean off = !cond.isAlways() && factor <= 0.0F;
      MutableComponent t = Component.literal(RelicTooltip.value(e.stat(), e.baseAmount()) + " ").append(e.stat().displayName());
      if (!cond.isAlways()) {
         t.append(" ").append(conditionLabel(cond));
         if (off) {
            t.append(" ").append(Component.translatableWithFallback("awakened.card.relics.inactive", "(inactive)"));
         } else if (factor < 1.0F) {
            t.append(" ")
               .append(
                  Component.translatableWithFallback(
                     "awakened.card.relics.now", "(%s now)", new Object[]{RelicTooltip.value(e.stat(), e.baseAmount() * factor)}
                  )
               );
         }
      }

      int col = off ? -6054250 : (e.baseAmount() < 0.0F ? -43691 : -11141291);
      gfx.text(gfx.clip(t.getString(), maxW), x, y, col);
   }

   private static void openBelt() {
      RelicBeltScreen.returnToCharacterScreen = true;
      PacketDistributor.sendToServer(OpenBeltPayload.INSTANCE, new CustomPacketPayload[0]);
   }

   private static List<String> worn(CardContext ctx) {
      List<String> out = new ArrayList<>();

      for (String id : ctx.state.attunedRelics()) {
         if (RelicRegistry.get(id) != null && out.size() < 3) {
            out.add(id);
         }
      }

      return out;
   }

   private static List<RelicsFace.Carried> carried(CardContext ctx) {
      Map<String, Integer> counts = new LinkedHashMap<>();

      for (ItemStack s : inventory(ctx)) {
         String id = s.getItem() instanceof RelicItem ? RelicItem.relicId(s) : null;
         if (RelicRegistry.get(id) != null) {
            counts.merge(id, s.getCount(), Integer::sum);
         }
      }

      List<RelicsFace.Carried> out = new ArrayList<>(counts.size());
      counts.forEach((idx, n) -> out.add(new RelicsFace.Carried(idx, n)));
      return out;
   }

   private static boolean carryingBelt(CardContext ctx) {
      for (ItemStack s : inventory(ctx)) {
         if (s.getItem() instanceof RelicBeltItem) {
            return true;
         }
      }

      return false;
   }

   private static List<ItemStack> inventory(CardContext ctx) {
      List<ItemStack> out = new ArrayList<>();
      Player p = ctx.mc.player;
      if (p != null) {
         Inventory inv = p.getInventory();

         for (int i = 0; i < inv.getContainerSize(); i++) {
            if (!inv.getItem(i).isEmpty()) {
               out.add(inv.getItem(i));
            }
         }
      }

      return out;
   }

   private static RelicContext live(CardContext ctx) {
      return ctx.mc.player == null ? null : new RelicContext(ctx.mc.player);
   }

   private static int hoveredBelt(CardGfx gfx, List<String> worn) {
      for (int i = 0; i < worn.size(); i++) {
         if (gfx.hover("relics/belt/" + i)) {
            return i;
         }
      }

      return -1;
   }

   private static int hoveredCarried(CardGfx gfx, int n) {
      for (int i = 0; i < n; i++) {
         if (gfx.hover("relics/carried/" + i)) {
            return i;
         }
      }

      return -1;
   }

   private static RelicEffect headline(Relic r) {
      for (RelicEffect e : r.effects()) {
         if (!e.condition().isAlways()) {
            return e;
         }
      }

      return r.effects().get(0);
   }

   private static Map<Stat, Float> plainSums(List<String> worn) {
      Map<Stat, Float> sums = new EnumMap<>(Stat.class);

      for (String id : worn) {
         for (RelicEffect e : RelicRegistry.get(id).effects()) {
            if (e.condition().isAlways()) {
               sums.merge(e.stat(), e.baseAmount(), Float::sum);
            }
         }
      }

      return sums;
   }

   private static boolean combines(List<String> worn) {
      Map<Stat, Integer> n = new EnumMap<>(Stat.class);

      for (String id : worn) {
         for (RelicEffect e : RelicRegistry.get(id).effects()) {
            if (e.condition().isAlways() && n.merge(e.stat(), 1, Integer::sum) > 1) {
               return true;
            }
         }
      }

      return false;
   }

   private static Component conditionLabel(Condition cond) {
      Component tip = cond.describe();
      String id = cond.id() != null ? cond.id() : "";
      String fallback = COND_FALLBACK.get(id);
      if (fallback == null) {
         return (Component)(tip != null ? tip : Component.empty());
      } else {
         return Component.translatableWithFallback("awakened.card.relics.cond." + id, fallback, new Object[]{HEALTHY_PCT});
      }
   }

   private static int rarityColor(Relic r) {
      Integer c = r.rarity().color().getColor();
      return c == null ? -1185569 : CardStyle.opaque(c);
   }

   private static Component rarityName(Rarity rarity) {
      String n = rarity.getSerializedName();
      return Component.translatableWithFallback("awakened.card.relics.rarity." + n, n.isEmpty() ? n : Character.toUpperCase(n.charAt(0)) + n.substring(1));
   }

   private static record Carried(String id, int count) {
   }
}
