package com.devilfruits.fruits.uo_uo_seiryu.client.dev;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.VfxCapture;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuFx;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.RushCinematic;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class StressCapture extends GpoCapture {
   public StressCapture() {
      super("vfx_st_");
   }

   private static void finisher(Minecraft mc, AutoTest s) {
      if (mc.level != null && mc.player != null) {
         Vec3 at = new Vec3(0.5, (double)s.origin().getY(), -10.5);
         RushCinematic c = (RushCinematic)VfxDirector.play(
            SeiryuFx.RUSH, new VfxAnchor(-1, at), new VfxParams(at, new Vec3(0.0, 0.0, -1.0), 1.0F, mc.level.getGameTime(), 0)
         );
         if (c != null) {
            c.finisher(mc.level, at, 3.0F);
         }
      }
   }

   private int pass(AutoTest s, int t, CoreClientConfig.CinematicQuality q, String label) {
      s.add(t, mc -> quality(q));
      Consumer<Minecraft> wide = rig(19.0, 150.0F, 11.0F, 1.6);
      return this.take(s, t + 2, 20, p -> veil(p, true), 45, p -> cast(p, "draconic_thunderfall", 72), wide, 110, c -> {
         for (int k = 10; k <= 80; k += 20) {
            s.add(c + k, mc -> finisher(mc, s));
         }

         s.add(c + 20, mc -> VfxCapture.fps(label));
         s.add(c + 74, mc -> VfxCapture.fpsEnd());
         s.add(c + 80, mc -> s.shot(mc, this.prefix + label.toLowerCase(Locale.ROOT).replace(' ', '_')));
      });
   }

   @Override
   protected int takes(AutoTest s, int t) {
      t = this.pass(s, t, CoreClientConfig.CinematicQuality.ULTRA, "ULTRA stress");
      t = this.pass(s, t, CoreClientConfig.CinematicQuality.HIGH, "HIGH stress");
      t = this.pass(s, t, CoreClientConfig.CinematicQuality.MEDIUM, "MEDIUM stress");
      t = this.pass(s, t, CoreClientConfig.CinematicQuality.LOW, "LOW stress");
      s.add(t, mc -> CoreClientConfig.ADAPTIVE_LOAD.set(false));
      t = this.pass(s, t, CoreClientConfig.CinematicQuality.ULTRA, "ULTRA governor off");
      s.add(t, mc -> CoreClientConfig.ADAPTIVE_LOAD.set(true));
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.ULTRA));
      return t + 2;
   }
}
