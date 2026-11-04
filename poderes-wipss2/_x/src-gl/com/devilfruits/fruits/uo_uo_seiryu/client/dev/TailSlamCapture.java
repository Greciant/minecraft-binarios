package com.devilfruits.fruits.uo_uo_seiryu.client.dev;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.VfxCapture;
import com.devilfruits.core.config.CoreClientConfig;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

public final class TailSlamCapture extends GpoCapture {
   public TailSlamCapture() {
      super("vfx_ts_");
   }

   private static Consumer<ServerPlayer> sweep() {
      return p -> cast(p, "draconic_tail_slam", 1);
   }

   @Override
   protected int takes(AutoTest s, int t) {
      Consumer<ServerPlayer> near = p -> dummyTo(s, p, -6.5);
      Consumer<Minecraft> above = rig(11.0, 196.0F, 34.0F, 0.6);
      Consumer<Minecraft> side = rig(10.0, 120.0F, 12.0F, 0.4);
      t = this.take(s, t, 5, near, 0, sweep(), above, 30, c -> {
         for (int k = 1; k <= 24; k++) {
            this.shot(s, c + k, String.format("seq_%02d", k));
         }
      });
      t = this.take(s, t, 5, near, 0, sweep(), side, 30, c -> {
         this.shot(s, c + 6, "01_sweep_side");
         this.shot(s, c + 9, "02_sweep_end_side");
      });
      s.add(t, mc -> s.server(mc, p -> cave(s, p, true)));
      t = this.take(s, t + 2, 5, near, 0, sweep(), above, 30, c -> this.shot(s, c + 7, "03_cave_sweep"));
      s.add(t, mc -> s.server(mc, p -> cave(s, p, false)));
      t = this.take(s, t + 2, 5, near, 0, sweep(), firstPerson(), 30, c -> this.shot(s, c + 7, "04_first_person_sweep"));
      t = this.take(s, t, 3, near, 0, sweep(), rig(11.0, 170.0F, 25.0F, 0.6), 90, c -> this.clip(s, c - 1, 120));
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.LOW));
      t = this.take(s, t + 2, 5, near, 0, sweep(), above, 30, c -> this.shot(s, c + 7, "cmp_low_sweep"));
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.ULTRA));
      t = this.take(s, t + 2, 5, near, 0, sweep(), above, 30, c -> this.shot(s, c + 7, "cmp_ultra_sweep"));
      return this.take(s, t, 20, near, 0, sweep(), side, 40, c -> {
         s.add(c, mc -> VfxCapture.fps("ULTRA tail slam"));
         s.add(c + 30, mc -> VfxCapture.fpsEnd());
      });
   }
}
