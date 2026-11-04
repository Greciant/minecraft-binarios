package com.awakenedmod.awakened.core.stamina;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.PowerResource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

public final class ChargeBill {
   private static final int SYNC_INTERVAL = 4;
   private final float min;
   private final float max;
   private float paid;
   private boolean dry;

   private ChargeBill(float min, float max) {
      this.min = min;
      this.max = max;
      this.paid = min;
   }

   public static ChargeBill opened(float min, float max) {
      return new ChargeBill(min, max);
   }

   public void payTo(ServerPlayer player, PowerResource resource, float f) {
      float owed = Mth.lerp(Mth.clamp(f, 0.0F, 1.0F), this.min, this.max) - this.paid;
      if (!(owed <= 0.0F) && !this.dry) {
         float pay = Math.min(owed, resource.current(player));
         if (pay > 0.0F) {
            resource.sustain(player, pay);
            this.paid += pay;
         }

         this.dry = pay < owed;
         if (this.dry || player.tickCount % 4 == 0) {
            PowerService.sync(player);
         }
      }
   }

   public float paidCharge() {
      return this.max <= this.min ? 1.0F : Mth.clamp((this.paid - this.min) / (this.max - this.min), 0.0F, 1.0F);
   }

   public void refund(ServerPlayer player, PowerResource resource) {
      resource.refund(player, this.paid);
      this.paid = 0.0F;
      PowerService.sync(player);
   }
}
