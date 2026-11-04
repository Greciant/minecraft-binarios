package com.devilfruits.fruits.uo_uo_seiryu.client.dev;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.VfxCapture;
import com.devilfruits.core.config.CoreClientConfig;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

public final class VeilCapture extends GpoCapture {
   public VeilCapture() {
      super("vfx_tv_");
   }

   private static Consumer<ServerPlayer> veilCast() {
      return p -> cast(p, "thunderclad_veil", 1);
   }

   @Override
   protected int takes(AutoTest s, int t) {
      Consumer<Minecraft> wide = rig(12.0, 150.0F, 12.0F, 0.6);
      Consumer<Minecraft> close = rig(7.0, 205.0F, 6.0F, 0.2);
      t = this.take(s, t, 5, none(), 0, veilCast(), wide, 70, c -> {
         this.shot(s, c + 5, "01_anticipation");
         this.shot(s, c + 11, "02_release");
         this.shot(s, c + 18, "03_cocoon");
         this.shot(s, c + 26, "04_burst");
         this.shot(s, c + 38, "05_aura");
         s.add(c + 44, mc -> s.server(mc, p -> command(p, "damage @s 1")));
         this.shot(s, c + 45, "06_aura_pulse");
      });
      t = this.take(s, t, 5, none(), 0, veilCast(), close, 50, c -> {
         this.shot(s, c + 6, "07_anticipation_close");
         this.shot(s, c + 18, "08_cocoon_close");
         this.shot(s, c + 26, "09_burst_close");
         this.shot(s, c + 45, "10_aura_close");
      });
      s.add(t, mc -> s.server(mc, p -> cave(s, p, true)));
      t = this.take(s, t + 2, 5, none(), 0, veilCast(), wide, 50, c -> {
         this.shot(s, c + 18, "11_cave_cocoon");
         this.shot(s, c + 25, "12_cave_burst");
         this.shot(s, c + 45, "13_cave_aura");
      });
      s.add(t, mc -> s.server(mc, p -> cave(s, p, false)));
      t = this.take(s, t + 2, 5, none(), 0, veilCast(), firstPerson(), 50, c -> {
         this.shot(s, c + 6, "14_first_person_gather");
         this.shot(s, c + 18, "15_first_person_cocoon");
         this.shot(s, c + 45, "16_first_person_aura");
      });
      t = this.take(s, t, 3, none(), 0, veilCast(), rig(10.0, 165.0F, 10.0F, 0.5), 90, c -> this.clip(s, c - 1, 120));
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.LOW));
      t = this.take(s, t + 2, 5, none(), 0, veilCast(), close, 40, c -> this.shot(s, c + 18, "cmp_low_cocoon"));
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.ULTRA));
      t = this.take(s, t + 2, 5, none(), 0, veilCast(), close, 40, c -> this.shot(s, c + 18, "cmp_ultra_cocoon"));
      return this.take(s, t, 20, none(), 0, veilCast(), wide, 90, c -> {
         s.add(c + 2, mc -> VfxCapture.fps("ULTRA veil cast + aura"));
         s.add(c + 80, mc -> VfxCapture.fpsEnd());
      });
   }
}
