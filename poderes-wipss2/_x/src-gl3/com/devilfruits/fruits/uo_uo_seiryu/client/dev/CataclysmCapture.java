package com.devilfruits.fruits.uo_uo_seiryu.client.dev;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.VfxCapture;
import com.devilfruits.core.config.CoreClientConfig;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

public final class CataclysmCapture extends GpoCapture {
   public CataclysmCapture() {
      super("vfx_dc_");
   }

   private static Consumer<ServerPlayer> slam() {
      return p -> cast(p, "draconic_cataclysm", 1);
   }

   @Override
   protected int takes(AutoTest s, int t) {
      Consumer<Minecraft> wide = rig(22.0, 150.0F, 10.0F, 1.5);
      Consumer<Minecraft> close = rig(12.0, 205.0F, 8.0F, 0.8);
      t = this.take(s, t, 5, none(), 0, slam(), wide, 60, c -> {
         for (int k = 1; k <= 50; k++) {
            this.shot(s, c + k, String.format("seq_%02d", k));
         }
      });
      t = this.take(s, t, 5, none(), 0, slam(), close, 60, c -> {
         this.shot(s, c + 6, "01_crouch_close");
         this.shot(s, c + 26, "02_telegraph_close");
         this.shot(s, c + 34, "03_impact_close");
         this.shot(s, c + 50, "04_after_close");
      });
      t = this.take(s, t, 5, p -> veil(p, true), 45, slam(), wide, 60, c -> {
         this.shot(s, c + 26, "05_veil_telegraph");
         this.shot(s, c + 34, "06_veil_impact");
         this.shot(s, c + 36, "07_veil_impact_late");
      });
      s.add(t, mc -> s.server(mc, p -> cave(s, p, true)));
      t = this.take(s, t + 2, 5, none(), 0, slam(), wide, 60, c -> {
         this.shot(s, c + 26, "08_cave_telegraph");
         this.shot(s, c + 34, "09_cave_impact");
      });
      s.add(t, mc -> s.server(mc, p -> cave(s, p, false)));
      t = this.take(s, t + 2, 5, none(), 0, slam(), firstPerson(), 60, c -> {
         this.shot(s, c + 20, "10_first_person_hang");
         this.shot(s, c + 34, "11_first_person_impact");
      });
      t = this.take(s, t, 3, none(), 0, slam(), rig(20.0, 160.0F, 10.0F, 1.5), 90, c -> this.clip(s, c - 1, 120));
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.LOW));
      t = this.take(s, t + 2, 5, none(), 0, slam(), wide, 50, c -> this.shot(s, c + 34, "cmp_low_impact"));
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.ULTRA));
      t = this.take(s, t + 2, 5, none(), 0, slam(), wide, 50, c -> this.shot(s, c + 34, "cmp_ultra_impact"));
      return this.take(s, t, 20, none(), 0, slam(), wide, 80, c -> {
         s.add(c, mc -> VfxCapture.fps("ULTRA cataclysm"));
         s.add(c + 70, mc -> VfxCapture.fpsEnd());
      });
   }
}
