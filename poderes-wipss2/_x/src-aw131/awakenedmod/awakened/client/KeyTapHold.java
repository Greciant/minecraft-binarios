package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.screen.RadialWheelScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class KeyTapHold {
   private final KeyMapping key;
   private final int holdTicks;
   private int heldTicks;
   private boolean wasDown;
   private boolean spent;
   private boolean shiftAtPress;
   private boolean rawWasDown;

   public KeyTapHold(KeyMapping key, int holdTicks) {
      this.key = key;
      this.holdTicks = holdTicks;
   }

   public void reset() {
      this.heldTicks = 0;
      this.wasDown = false;
      this.spent = false;
      this.shiftAtPress = false;
      this.rawWasDown = false;
   }

   public boolean pressedWithShift() {
      return this.shiftAtPress;
   }

   public float heldProgress() {
      return this.wasDown && !this.spent ? Math.min(1.0F, (float)this.heldTicks / (float)this.holdTicks) : 0.0F;
   }

   public void tick(Minecraft mc, Runnable onTap, Runnable onHold) {
      boolean rawDown = RadialWheelScreen.keyHeld(this.key);
      boolean clicked = false;

      while (this.key.consumeClick()) {
         clicked = true;
      }

      boolean freshClick = clicked && !this.rawWasDown;
      this.rawWasDown = rawDown;
      if (!rawDown) {
         this.spent = false;
      }

      boolean down = rawDown && mc.screen == null && !this.spent;
      if (down) {
         if (!this.wasDown) {
            this.shiftAtPress = mc.options.keyShift.isDown();
         }

         if (++this.heldTicks == this.holdTicks) {
            this.spent = true;
            onHold.run();
         }

         this.wasDown = true;
      } else {
         if (mc.screen == null) {
            if (this.wasDown && this.heldTicks < this.holdTicks) {
               onTap.run();
            } else if (!this.wasDown && !rawDown && freshClick) {
               this.shiftAtPress = mc.options.keyShift.isDown();
               onTap.run();
            }
         }

         this.heldTicks = 0;
         this.wasDown = false;
      }
   }
}
