package com.devilfruits.fruits.uo_uo_seiryu.client.dev;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.VfxCapture;
import com.devilfruits.core.config.CoreClientConfig;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

public final class RushCapture extends GpoCapture {
   private static final int VEIL_SETTLE = 45;

   public RushCapture() {
      super("vfx_tr_");
   }

   private static Consumer<ServerPlayer> rush() {
      return p -> cast(p, "thunderclaw_rush", 1);
   }

   private static Consumer<ServerPlayer> veiled() {
      return p -> veil(p, true);
   }

   @Override
   protected int takes(AutoTest s, int t) {
      Consumer<Minecraft> side = rig(13.0, 118.0F, 12.0F, 0.4);
      Consumer<Minecraft> behind = rig(8.5, 196.0F, 11.0F, 0.7);
      t = this.take(s, t, 5, veiled(), 45, rush(), side, 60, c -> {
         this.shot(s, c + 3, "01_anticipation");
         this.shot(s, c + 7, "02_dash");
         this.shot(s, c + 11, "03_hit1");
         this.shot(s, c + 17, "04_hit2");
         this.shot(s, c + 23, "05_hit3");
         this.shot(s, c + 31, "06_finisher");
         this.shot(s, c + 47, "07_end");
      });
      t = this.take(s, t, 5, veiled(), 45, rush(), behind, 60, c -> {
         for (int k = 1; k <= 40; k++) {
            this.shot(s, c + k, String.format("seq_%02d", k));
         }
      });
      s.add(t, mc -> s.server(mc, p -> cave(s, p, true)));
      t = this.take(s, t + 2, 5, veiled(), 45, rush(), side, 50, c -> {
         this.shot(s, c + 17, "18_cave_hit2");
         this.shot(s, c + 31, "19_cave_finisher");
         this.shot(s, c + 45, "20_cave_end");
      });
      s.add(t, mc -> s.server(mc, p -> cave(s, p, false)));
      t = this.take(s, t + 2, 5, none(), 0, rush(), behind, 40, c -> {
         this.shot(s, c + 7, "21_fire_dash");
         this.shot(s, c + 11, "22_fire_hit1");
         this.shot(s, c + 17, "23_fire_hit2");
         this.shot(s, c + 23, "24_fire_hit3");
      });
      t = this.take(s, t, 5, veiled(), 45, rush(), firstPerson(), 50, c -> {
         this.shot(s, c + 2, "25_first_person_anticipation");
         this.shot(s, c + 7, "26_first_person_dash");
         this.shot(s, c + 17, "27_first_person_hit2");
         this.shot(s, c + 31, "28_first_person_finisher");
      });
      t = this.take(s, t, 3, veiled(), 45, rush(), rig(11.0, 150.0F, 12.0F, 0.5), 90, c -> this.clip(s, c - 1, 120));
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.LOW));
      t = this.take(s, t + 2, 5, veiled(), 45, rush(), side, 40, c -> this.shot(s, c + 31, "cmp_low_finisher"));
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.ULTRA));
      t = this.take(s, t + 2, 5, veiled(), 45, rush(), side, 40, c -> this.shot(s, c + 31, "cmp_ultra_finisher"));
      return this.take(s, t, 20, veiled(), 45, rush(), side, 60, c -> {
         s.add(c, mc -> VfxCapture.fps("ULTRA rush + finisher + veil aura"));
         s.add(c + 50, mc -> VfxCapture.fpsEnd());
      });
   }
}
