package com.awakenedmod.awakened.client.render.limitless;

import net.minecraft.client.multiplayer.ClientLevel;

public interface LimitlessLook<C> {
   void draw(LimitlessFrame var1, C var2, float var3);

   default void tick(ClientLevel level, C cue, int age) {
   }
}
