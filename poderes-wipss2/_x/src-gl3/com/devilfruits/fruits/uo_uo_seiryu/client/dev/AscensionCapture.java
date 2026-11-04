package com.devilfruits.fruits.uo_uo_seiryu.client.dev;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.VfxCapture;
import com.devilfruits.core.config.CoreClientConfig;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

public final class AscensionCapture extends GpoCapture {
   public AscensionCapture() {
      super("vfx_da_");
   }

   private static Consumer<ServerPlayer> dive() {
      return p -> cast(p, "draconic_ascension", 1);
   }

   @Override
   protected int takes(AutoTest s, int t) {
      Consumer<Minecraft> behind = rig(10.0, 196.0F, 14.0F, 0.8);
      Consumer<Minecraft> side = rig(15.0, 112.0F, 10.0F, 1.0);
      t = this.take(s, t, 5, none(), 0, dive(), behind, 50, c -> {
         for (int k = 1; k <= 40; k++) {
            this.shot(s, c + k, String.format("seq_%02d", k));
         }
      });
      t = this.take(s, t, 5, none(), 0, dive(), side, 50, c -> {
         this.shot(s, c + 6, "01_anticipation");
         this.shot(s, c + 16, "02_launch_side");
         this.shot(s, c + 25, "03_impact_side");
         this.shot(s, c + 34, "04_after_side");
      });
      s.add(t, mc -> s.server(mc, p -> cave(s, p, true)));
      t = this.take(s, t + 2, 5, none(), 0, dive(), side, 50, c -> {
         this.shot(s, c + 16, "05_cave_launch");
         this.shot(s, c + 25, "06_cave_impact");
      });
      s.add(t, mc -> s.server(mc, p -> cave(s, p, false)));
      t = this.take(s, t + 2, 5, none(), 0, dive(), firstPerson(), 50, c -> {
         this.shot(s, c + 6, "07_first_person_anticipation");
         this.shot(s, c + 25, "08_first_person_impact");
      });
      t = this.take(s, t, 3, none(), 0, dive(), rig(13.0, 150.0F, 12.0F, 0.8), 90, c -> this.clip(s, c - 1, 120));
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.LOW));
      t = this.take(s, t + 2, 5, none(), 0, dive(), side, 40, c -> this.shot(s, c + 25, "cmp_low_impact"));
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.ULTRA));
      t = this.take(s, t + 2, 5, none(), 0, dive(), side, 40, c -> this.shot(s, c + 25, "cmp_ultra_impact"));
      return this.take(s, t, 20, none(), 0, dive(), side, 60, c -> {
         s.add(c, mc -> VfxCapture.fps("ULTRA ascension"));
         s.add(c + 50, mc -> VfxCapture.fpsEnd());
      });
   }
}
