package com.awakenedmod.awakened.client.screen.card.faces;

import com.awakenedmod.awakened.client.screen.card.CardContext;
import com.awakenedmod.awakened.client.screen.card.CardFace;
import com.awakenedmod.awakened.client.screen.card.CardGfx;
import com.awakenedmod.awakened.client.screen.card.CardStyle;
import com.awakenedmod.awakened.content.CatalystItem;
import com.awakenedmod.awakened.content.CommonRecipes;
import com.awakenedmod.awakened.content.FocusedCatalystItem;
import com.awakenedmod.awakened.content.relic.RelicItem;
import com.awakenedmod.awakened.core.power.CraftRecipe;
import com.awakenedmod.awakened.core.power.Power;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;

public final class RecipesFace implements CardFace {
   private static final int COLS = 8;
   private static final int SLOT = 18;
   private static final int HEAD = 12;
   private static final int GAP = 5;
   private static final int GAP_COMPACT = 2;
   private static final int GRID = 56;
   private static final int CELL = 20;
   private static final int DIM = -1725027544;
   private static final String BOOK = "recipes/book";
   private static final String PICKED = "recipes/picked";

   @Override
   public int height(CardContext ctx, int w, boolean compact) {
      RecipesFace.Book book = book(ctx);
      int rw = w - (bookW() + 14);
      int detail = 0;

      for (RecipesFace.Entry e : book.all()) {
         detail = Math.max(detail, detailH(ctx, e, rw));
      }

      return Math.max(bookH(book, compact), detail);
   }

   @Override
   public void draw(CardGfx gfx, CardContext ctx, int x, int y, int w, boolean compact) {
      RecipesFace.Book book = book(ctx);
      if (!book.all().isEmpty()) {
         int picked = ctx.get("recipes/picked", 0);
         int cur = Math.min(picked, book.all().size() - 1);

         for (int i = 0; i < book.all().size(); i++) {
            if (gfx.hover("recipe/" + i)) {
               cur = i;
            }
         }

         this.drawBook(gfx, ctx, book, cur, x, y, compact);
         int rx = x + bookW() + 14;
         int h = this.height(ctx, w, compact);
         gfx.fill(rx - 8, y, 1, h, -12238791);
         this.detail(gfx, ctx, book.all().get(cur), rx, y, x + w - rx, y + h);
      }
   }

   private static int bookW() {
      return 144;
   }

   private static int rows(RecipesFace.Group g) {
      return (g.list().size() + 8 - 1) / 8;
   }

   private static int bookH(RecipesFace.Book book, boolean compact) {
      int gap = compact ? 2 : 5;
      int h = 0;

      for (RecipesFace.Group g : book.groups()) {
         h += 12 + rows(g) * 18 + gap;
      }

      return Math.max(0, h - gap);
   }

   private void drawBook(CardGfx gfx, CardContext ctx, RecipesFace.Book book, int cur, int x, int y, boolean compact) {
      int yy = y;
      int idx = 0;

      for (RecipesFace.Group g : book.groups()) {
         gfx.textBold(g.name(), x, yy, -1185569);
         yy += 12;

         for (int i = 0; i < g.list().size(); idx++) {
            CraftRecipe r = g.list().get(i);
            int sx = x + i % 8 * 18;
            int sy = yy + i / 8 * 18;
            int pick = idx;
            gfx.slot(sx, sy, 18, idx == cur ? CardStyle.ring(gfx.band) : 0);
            gfx.item(r.result(), sx + 1, sy + 1);
            if (locked(ctx, r)) {
               gfx.fill(sx + 1, sy + 1, 16, 16, -1725027544);
            }

            gfx.hit(sx, sy, 18, 18, "recipe/" + idx, () -> ctx.put("recipes/picked", pick));
            i++;
         }

         yy += rows(g) * 18 + (compact ? 2 : 5);
      }
   }

   private static int detailH(CardContext ctx, RecipesFace.Entry e, int rw) {
      int h = ctx.lines(CardGfx.bold(e.recipe().result().getHoverName()), rw) * 10 + 3 + 56 + 6;
      if (e.recipe().unlockLevel() > 0) {
         h += 10;
      }

      int tip = 0;

      for (Component c : e.tip()) {
         tip += ctx.lines(c, rw);
      }

      return h + tip * 9;
   }

   private void detail(CardGfx gfx, CardContext ctx, RecipesFace.Entry e, int x, int y, int w, int bottom) {
      CraftRecipe r = e.recipe();
      boolean locked = locked(ctx, r);
      int ry = y;

      for (String line : gfx.wrap(CardGfx.bold(r.result().getHoverName()), w)) {
         gfx.textBold(line, x, ry, -1185569);
         ry += 10;
      }

      ry += 3;
      int ax = x + grid(gfx, r, x, ry) + 8;
      int mid = ry + 28;
      if (r.hasStation()) {
         ax = arrowTo(gfx, r.station(), ax, mid, "recipes/station", false) + 6;
      }

      arrowTo(gfx, r.result(), ax, mid, "recipes/result", locked);
      ry += 62;
      if (locked) {
         gfx.text(Component.translatable("awakened.screen.recipe_requires", new Object[]{r.unlockLevel()}), x, ry, -6054250);
         ry += 10;
      }

      tip(gfx, e, x, ry, w, (bottom - ry) / 9);
   }

   private static int grid(CardGfx gfx, CraftRecipe r, int x, int y) {
      gfx.fill(x, y, 56, 56, -15461872);

      for (int i = 0; i < 9; i++) {
         int col = i % 3;
         int row = i / 3;
         int cx = x + 2 + col * 18;
         int cy = y + 2 + row * 18;
         gfx.fill(cx, cy, 16, 16, -13751512);
         if (col < r.width() && row < r.height()) {
            itemWithTooltip(gfx, r.grid().get(row * r.width() + col), cx, cy, "recipes/cell/" + i);
         }
      }

      return 56;
   }

   private static int arrowTo(CardGfx gfx, ItemStack stack, int ax, int mid, String key, boolean dim) {
      gfx.sprite("container/villager/trade_arrow", ax, mid - 5, 10, 9);
      int cx = ax + 16;
      int cy = mid - 10;
      gfx.fill(cx, cy, 20, 20, -15461872);
      gfx.fill(cx + 2, cy + 2, 16, 16, -13751512);
      itemWithTooltip(gfx, stack, cx + 2, cy + 2, key);
      if (dim) {
         gfx.fill(cx + 2, cy + 2, 16, 16, -1725027544);
      }

      return cx + 20;
   }

   private static void itemWithTooltip(CardGfx gfx, ItemStack stack, int x, int y, String key) {
      if (!stack.isEmpty()) {
         gfx.item(stack, x, y);
         gfx.hit(x, y, 16, 16, key);
         if (gfx.hover(key)) {
            gfx.tooltip(stack);
         }
      }
   }

   private static void tip(CardGfx gfx, RecipesFace.Entry e, int x, int y, int w, int maxLines) {
      List<String> lines = new ArrayList<>();

      for (Component c : e.tip()) {
         lines.addAll(gfx.wrap(c, w));
      }

      int n = Math.min(lines.size(), Math.max(0, maxLines));

      for (int i = 0; i < n; i++) {
         String s = lines.get(i);
         gfx.text(i == n - 1 && n < lines.size() ? gfx.clip(s + "...", w) : s, x, y + i * 9, -3554376);
      }
   }

   private static boolean locked(CardContext ctx, CraftRecipe r) {
      return r.unlockLevel() > ctx.state.masteryLevel();
   }

   private static RecipesFace.Book book(CardContext ctx) {
      RecipesFace.Book book = ctx.get("recipes/book", null);
      if (book == null || book.power() != ctx.power) {
         book = build(ctx);
         ctx.put("recipes/book", book);
      }

      return book;
   }

   private static RecipesFace.Book build(CardContext ctx) {
      List<CraftRecipe> catalysts = new ArrayList<>();
      List<CraftRecipe> relics = new ArrayList<>();
      List<CraftRecipe> gear = new ArrayList<>();

      for (CraftRecipe r : CommonRecipes.list()) {
         Item item = r.result().getItem();
         if (item instanceof CatalystItem || item instanceof FocusedCatalystItem) {
            catalysts.add(r);
         } else if (item instanceof RelicItem) {
            relics.add(r);
         } else {
            gear.add(r);
         }
      }

      List<RecipesFace.Group> groups = new ArrayList<>();
      addGroup(groups, ctx.power.displayName(), ctx.power.recipes());
      addGroup(groups, Component.translatableWithFallback("awakened.card.recipes.catalysts", "Catalysts"), catalysts);
      addGroup(groups, Component.translatable("awakened.screen.tab_relics"), relics);
      addGroup(groups, Component.translatableWithFallback("awakened.card.recipes.gear", "Gear"), gear);
      List<RecipesFace.Entry> all = new ArrayList<>();

      for (RecipesFace.Group g : groups) {
         for (CraftRecipe rx : g.list()) {
            all.add(new RecipesFace.Entry(rx, tooltip(ctx, rx.result())));
         }
      }

      return new RecipesFace.Book(ctx.power, groups, all);
   }

   private static void addGroup(List<RecipesFace.Group> groups, Component name, List<CraftRecipe> list) {
      if (!list.isEmpty()) {
         groups.add(new RecipesFace.Group(name, list));
      }
   }

   private static List<Component> tooltip(CardContext ctx, ItemStack stack) {
      List<Component> lines = stack.getTooltipLines(TooltipContext.of(ctx.mc.level), ctx.mc.player, TooltipFlag.NORMAL);
      List<Component> out = new ArrayList<>();

      for (int i = 1; i < lines.size(); i++) {
         if (!lines.get(i).getString().isBlank()) {
            out.add(lines.get(i));
         }
      }

      return out;
   }

   private static record Book(Power power, List<RecipesFace.Group> groups, List<RecipesFace.Entry> all) {
   }

   private static record Entry(CraftRecipe recipe, List<Component> tip) {
   }

   private static record Group(Component name, List<CraftRecipe> list) {
   }
}
