package com.awakenedmod.awakened.client.screen.card;

public interface CardFace {
   int height(CardContext var1, int var2, boolean var3);

   void draw(CardGfx var1, CardContext var2, int var3, int var4, int var5, boolean var6);

   default boolean scrolled(CardContext ctx, double amount) {
      return false;
   }
}
