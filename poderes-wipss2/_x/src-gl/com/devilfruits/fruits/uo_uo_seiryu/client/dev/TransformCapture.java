package com.devilfruits.fruits.uo_uo_seiryu.client.dev;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.VfxCapture;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.form.FormManager;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

public final class TransformCapture extends GpoCapture {
   public TransformCapture() {
      super("vfx_tx_");
   }

   private static Consumer<ServerPlayer> human() {
      return p -> FormManager.revert(p, FormManager.Reason.COMMAND);
   }

   private static Consumer<ServerPlayer> transform() {
      return p -> cast(p, "transform_hybrid", 1);
   }

   @Override
   protected int takes(AutoTest s, int t) {
      Consumer<Minecraft> front = rig(7.5, 0.0F, 6.0F, 0.4);
      Consumer<Minecraft> wide = rig(12.0, 150.0F, 10.0F, 0.6);
      t = this.take(s, t, 5, human(), 30, transform(), front, 80, c -> {
         for (int k = 1; k <= 70; k++) {
            this.shot(s, c + k, String.format("seq_%02d", k));
         }
      });
      s.add(t, mc -> s.server(mc, p -> cave(s, p, true)));
      t = this.take(s, t + 2, 5, human(), 30, transform(), wide, 80, c -> {
         for (int k = 10; k <= 60; k += 10) {
            this.shot(s, c + k, String.format("cave_%02d", k));
         }
      });
      s.add(t, mc -> s.server(mc, p -> cave(s, p, false)));
      t = this.take(s, t + 2, 5, human(), 30, transform(), firstPerson(), 80, c -> {
         for (int k = 10; k <= 60; k += 10) {
            this.shot(s, c + k, String.format("first_person_%02d", k));
         }
      });
      t = this.take(s, t, 3, human(), 30, transform(), rig(9.0, 20.0F, 6.0F, 0.4), 90, c -> this.clip(s, c - 1, 120));
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.LOW));
      t = this.take(s, t + 2, 5, human(), 30, transform(), wide, 80, c -> {
         for (int k = 10; k <= 60; k += 10) {
            this.shot(s, c + k, String.format("low_%02d", k));
         }
      });
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.ULTRA));
      return this.take(s, t, 20, human(), 30, transform(), wide, 90, c -> {
         s.add(c, mc -> VfxCapture.fps("ULTRA transformation"));
         s.add(c + 80, mc -> VfxCapture.fpsEnd());
      });
   }
}
