package com.devilfruits.fruits.uo_uo_seiryu.client.dev;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.VfxCapture;
import com.devilfruits.core.config.CoreClientConfig;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

public final class ThunderfallCapture extends GpoCapture {
   private static final int HOLD = 72;

   public ThunderfallCapture() {
      super("vfx_tf_");
   }

   private static Consumer<ServerPlayer> storm(int hold) {
      return p -> cast(p, "draconic_thunderfall", hold);
   }

   @Override
   protected int takes(AutoTest s, int t) {
      Consumer<Minecraft> wide = rig(19.0, 150.0F, 11.0F, 1.6);
      t = this.take(s, t, 5, none(), 0, storm(72), wide, 110, c -> {
         for (int k = 1; k <= 100; k += 2) {
            this.shot(s, c + k, String.format("seq_%03d", k));
         }
      });
      t = this.take(s, t, 5, p -> dummyTo(s, p, -30.0), 0, storm(72), rig(9.0, 205.0F, -6.0F, 1.0), 90, c -> {
         this.shot(s, c + 30, "01_storm_low");
         this.shot(s, c + 60, "02_storm_low_peak");
      });
      s.add(t, mc -> s.server(mc, p -> cave(s, p, true)));
      t = this.take(s, t + 2, 5, none(), 0, storm(72), wide, 110, c -> {
         this.shot(s, c + 40, "03_cave_storm");
         this.shot(s, c + 81, "04_cave_release");
         this.shot(s, c + 100, "05_cave_after");
      });
      s.add(t, mc -> s.server(mc, p -> cave(s, p, false)));
      t = this.take(s, t + 2, 5, none(), 0, storm(72), firstPerson(), 100, c -> {
         this.shot(s, c + 30, "06_first_person_storm");
         this.shot(s, c + 81, "07_first_person_release");
      });
      t = this.take(s, t, 3, none(), 0, storm(40), rig(17.0, 160.0F, 15.0F, 1.0), 90, c -> this.clip(s, c - 1, 120));
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.LOW));
      t = this.take(s, t + 2, 5, none(), 0, storm(72), wide, 70, c -> this.shot(s, c + 50, "cmp_low_storm"));
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.ULTRA));
      t = this.take(s, t + 2, 5, none(), 0, storm(72), wide, 70, c -> this.shot(s, c + 50, "cmp_ultra_storm"));
      t = this.take(s, t, 20, none(), 0, storm(72), wide, 110, c -> {
         s.add(c + 30, mc -> VfxCapture.fps("ULTRA thunderfall max strikes"));
         s.add(c + 74, mc -> VfxCapture.fpsEnd());
      });
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.HIGH));
      t = this.take(s, t + 2, 20, none(), 0, storm(72), wide, 110, c -> {
         s.add(c + 30, mc -> VfxCapture.fps("HIGH thunderfall max strikes"));
         s.add(c + 74, mc -> VfxCapture.fpsEnd());
      });
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.ULTRA));
      return t + 2;
   }
}
