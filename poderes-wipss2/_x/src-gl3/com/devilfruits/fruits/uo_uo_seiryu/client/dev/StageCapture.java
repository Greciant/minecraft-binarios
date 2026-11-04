package com.devilfruits.fruits.uo_uo_seiryu.client.dev;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureStage;
import com.devilfruits.client.dev.VfxCapture;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.registry.DFAttachments;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

public final class StageCapture extends GpoCapture {
   private final String stage;
   @Nullable
   private final String only;

   public StageCapture(String stage, @Nullable String only) {
      super("vfx_hb_");
      this.stage = stage;
      this.only = only;
   }

   @Override
   protected String form() {
      String var1 = this.stage;

      return switch (var1) {
         case "human" -> "human";
         case "dragon" -> "dragon";
         default -> "hybrid";
      };
   }

   private static Consumer<ServerPlayer> into(String form, float pitch) {
      return into(form, pitch, 0.0);
   }

   private static Consumer<ServerPlayer> into(String form, float pitch, double lift) {
      return p -> {
         FormManager.request(p, form, FormManager.Reason.COMMAND);
         PlayerFruitData data = DFAttachments.data(p);
         data.clearCooldowns();
         data.setEnergy((float)Mastery.maxEnergy(data));
         p.teleportTo(p.serverLevel(), p.getX(), p.getY() + lift, p.getZ(), 180.0F, pitch);
      };
   }

   private void still(AutoTest s, int tick, String pre, String name) {
      s.add(tick, mc -> s.shot(mc, pre + name));
   }

   private static Consumer<ServerPlayer> look(float pitch) {
      return p -> p.teleportTo(p.serverLevel(), p.getX(), p.getY(), p.getZ(), 180.0F, pitch);
   }

   private static void sweep(AutoTest s, int c, int ticks, float pitch) {
      for (int k = 0; k <= ticks; k++) {
         float yaw = 180.0F + 28.0F * Mth.sin((float)k / (float)ticks * (float) (Math.PI * 2));
         s.add(c + k, mc -> s.server(mc, p -> p.teleportTo(p.serverLevel(), p.getX(), p.getY(), p.getZ(), yaw, pitch)));
      }
   }

   private List<StageCapture.Tech> techniques(AutoTest s) {
      Consumer<ServerPlayer> none = none();
      if (this.stage.equals("human")) {
         Consumer<ServerPlayer> near = p -> dummyTo(s, p, -6.5);
         return List.of(
            new StageCapture.Tech(
               "vfx_hb_",
               "raimei hakke",
               near,
               p -> cast(p, "thunder_bagua", 1),
               rig(6.5, 140.0F, 12.0F, 0.4),
               rig(7.0, 200.0F, 22.0F, 0.4),
               34,
               22,
               1,
               9,
               -2,
               -2,
               0,
               0
            ),
            new StageCapture.Tech(
               "vfx_hbr_",
               "bolo breath",
               none,
               p -> cast(p, "blast_breath", 30),
               rig(6.5, 120.0F, 8.0F, 0.4),
               rig(7.0, 210.0F, 14.0F, 0.4),
               50,
               40,
               1,
               32,
               18,
               18,
               0,
               0
            ),
            new StageCapture.Tech(
               "vfx_hfc_",
               "flame cloud",
               none,
               p -> cast(p, "flame_cloud", 1),
               rig(11.0, 155.0F, 24.0F, 0.4),
               rig(9.0, 205.0F, 22.0F, 0.4),
               60,
               30,
               1,
               5,
               -2,
               -2,
               0,
               0
            ),
            new StageCapture.Tech(
               "vfx_hdd_",
               "dragon dash",
               look(-3.0F),
               p -> cast(p, "dragon_dash", 1),
               rig(11.0, 120.0F, 14.0F, 0.6),
               rig(8.0, 205.0F, 18.0F, 0.6),
               36,
               24,
               1,
               4,
               -2,
               -2,
               0,
               0
            )
         );
      } else if (this.stage.equals("dragon")) {
         String d = "dragon";
         Consumer<Minecraft> wide = rig(22.0, 150.0F, 20.0F, 2.0);
         Consumer<Minecraft> side = rig(20.0, 115.0F, 12.0F, 2.0);
         Consumer<Minecraft> high = rig(32.0, 165.0F, 30.0F, 2.0);
         return List.of(
            new StageCapture.Tech(
               "vfx_dbr_",
               "dragon bolo breath",
               into(d, 30.0F, 16.0),
               p -> cast(p, "blast_breath", 30),
               side,
               null,
               160,
               156,
               6,
               70,
               28,
               34,
               34,
               110,
               20,
               -1,
               30.0F
            ),
            new StageCapture.Tech(
               "vfx_dbb_",
               "dragon bolo barrage",
               into(d, 35.0F, 14.0),
               p -> cast(p, "bolo_breath", 30),
               high,
               null,
               130,
               124,
               4,
               72,
               40,
               58,
               0,
               0,
               20,
               -1,
               22.0F
            ),
            new StageCapture.Tech(
               "vfx_dtw_", "dragon twister", into(d, 8.0F, 12.0), p -> cast(p, "dragon_twister", 80), wide, null, 110, 104, 4, 40, -2, 66, 0, 0, 20, -1, 8.0F
            ),
            new StageCapture.Tech(
               "vfx_dtl_", "tail whip and coil crush", into(d, 8.0F, 8.0), p -> cast(p, "tail_whip", 1), wide, null, 110, 104, 4, 12, -2, 0, 0, 0, 20, 18, 8.0F
            ),
            new StageCapture.Tech("vfx_dro_", "dragon roar", into(d, 12.0F, 8.0), p -> cast(p, "roar", 1), side, null, 40, 30, 1, 10, -2, 2, 0, 0, 20, -1, 8.0F),
            new StageCapture.Tech(
               "vfx_dst_",
               "thunder cloud storm",
               into(d, 8.0F, 10.0),
               p -> cast(p, "thunder_storm", 1),
               rig(36.0, 140.0F, 4.0F, 2.0),
               null,
               170,
               168,
               6,
               46,
               10,
               40,
               0,
               0,
               20,
               -1,
               8.0F
            ),
            new StageCapture.Tech(
               "vfx_dtf_",
               "transformation to the dragon",
               into("hybrid", 8.0F),
               p -> cast(p, "transform_dragon", 1),
               wide,
               null,
               110,
               105,
               3,
               52,
               20,
               42,
               0,
               0,
               40,
               -1,
               8.0F
            )
         );
      } else {
         Consumer<ServerPlayer> mid = p -> dummyTo(s, p, -8.5);
         Consumer<ServerPlayer> down = look(26.0F);
         return List.of(
            new StageCapture.Tech(
               "vfx_yb_",
               "hybrid raimei hakke",
               mid,
               p -> cast(p, "thunder_bagua", 1),
               rig(12.0, 145.0F, 14.0F, 0.4),
               rig(12.0, 200.0F, 24.0F, 0.4),
               40,
               26,
               1,
               10,
               -2,
               2,
               0,
               0
            ),
            new StageCapture.Tech(
               "vfx_ybr_",
               "hybrid bolo breath",
               down,
               p -> cast(p, "blast_breath", 30),
               rig(10.0, 125.0F, 14.0F, 0.4),
               rig(11.0, 215.0F, 20.0F, 0.4),
               72,
               68,
               2,
               44,
               16,
               30,
               32,
               30
            )
         );
      }
   }

   private int technique(AutoTest s, int t, StageCapture.Tech x) {
      if (this.only != null && !this.only.equals(x.pre())) {
         return t;
      } else if (x.cave() == null) {
         return this.dragonTechnique(s, t, x);
      } else {
         String pre = x.pre();
         float pitch = x.pitch();
         t = this.take(s, t, 5, x.prep(), 0, x.cast(), x.main(), x.length(), c -> {
            int k = 0;

            while (k <= x.seq()) {
               this.still(s, c + k, pre, String.format("seq_%02d", k));
               k += x.step();
            }

            if (x.sweepTicks() > 0) {
               sweep(s, c + x.sweepFrom(), x.sweepTicks(), pitch);
            }
         });
         s.add(t, mc -> s.server(mc, p -> cave(s, p, true)));
         t = this.take(s, t + 2, 5, x.prep(), 0, x.cast(), x.cave(), x.length(), c -> {
            this.still(s, c + x.peak(), pre, "02_cave_peak");
            this.still(s, c + x.peak() + 3, pre, "03_cave_after");
            if (x.sweepTicks() > 0) {
               sweep(s, c + x.sweepFrom(), x.sweepTicks(), pitch);
            }
         });
         s.add(t, mc -> s.server(mc, p -> cave(s, p, false)));
         t = this.take(s, t + 2, 5, x.prep(), 0, x.cast(), firstPerson(), x.length(), c -> {
            this.still(s, c + x.peak(), pre, "04_first_person");
            if (x.sweepTicks() > 0) {
               sweep(s, c + x.sweepFrom(), x.sweepTicks(), pitch);
            }
         });
         t = this.take(s, t, 3, x.prep(), 0, x.cast(), x.main(), x.length() + 40, c -> {
            s.add(c + x.clipFrom(), mc -> VfxCapture.clip(pre + "clip", 90, 0.6666666666666666));
            if (x.sweepTicks() > 0) {
               sweep(s, c + x.sweepFrom(), x.sweepTicks(), pitch);
            }
         });
         t = this.take(s, t, 3, x.prep(), 0, x.cast(), x.main(), x.length() + 40, c -> {
            s.add(c + x.slowFrom(), mc -> VfxCapture.clip(pre + "slow", 120, 0.3333333333333333));
            if (x.sweepTicks() > 0) {
               sweep(s, c + x.sweepFrom(), x.sweepTicks(), pitch);
            }
         });
         s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.LOW));
         t = this.take(s, t + 2, 5, x.prep(), 0, x.cast(), x.main(), x.length(), c -> this.still(s, c + x.peak(), pre, "cmp_low_peak"));
         s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.ULTRA));
         t = this.take(s, t + 2, 5, x.prep(), 0, x.cast(), x.main(), x.length(), c -> this.still(s, c + x.peak(), pre, "cmp_ultra_peak"));
         return this.take(s, t, 20, x.prep(), 0, x.cast(), x.main(), x.length() + 20, c -> {
            s.add(c, mc -> VfxCapture.fps("ULTRA " + x.label()));
            s.add(c + x.length(), mc -> VfxCapture.fpsEnd());
            if (x.sweepTicks() > 0) {
               sweep(s, c + x.sweepFrom(), x.sweepTicks(), pitch);
            }
         });
      }
   }

   private int dragonTechnique(AutoTest s, int t, StageCapture.Tech x) {
      String pre = x.pre();
      CaptureStage.Beats extras = c -> {
         if (x.sweepTicks() > 0) {
            sweep(s, c + x.sweepFrom(), x.sweepTicks(), x.pitch());
         }

         if (x.again() >= 0) {
            s.add(c + x.again(), mc -> s.server(mc, x.cast()));
         }
      };
      t = this.take(s, t, 5, x.prep(), x.prepTicks(), x.cast(), x.main(), x.length(), c -> {
         int k = 0;

         while (k <= x.seq()) {
            this.still(s, c + k, pre, String.format("seq_%03d", k));
            k += x.step();
         }

         extras.add(c);
      });
      t = this.take(s, t, 3, x.prep(), x.prepTicks(), x.cast(), x.main(), x.length() + 40, c -> {
         s.add(c + x.clipFrom(), mc -> VfxCapture.clip(pre + "clip", 90, 0.6666666666666666));
         extras.add(c);
      });
      t = this.take(s, t, 3, x.prep(), x.prepTicks(), x.cast(), x.main(), x.length() + 40, c -> {
         s.add(c + x.slowFrom(), mc -> VfxCapture.clip(pre + "slow", 120, 0.3333333333333333));
         extras.add(c);
      });
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.LOW));
      t = this.take(s, t + 2, 5, x.prep(), x.prepTicks(), x.cast(), x.main(), x.length(), c -> {
         this.still(s, c + x.peak(), pre, "cmp_low_peak");
         extras.add(c);
      });
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.ULTRA));
      return this.take(s, t + 2, 20, x.prep(), x.prepTicks(), x.cast(), x.main(), x.length() + 20, c -> {
         s.add(c, mc -> VfxCapture.fps("ULTRA " + x.label()));
         s.add(c + x.length(), mc -> VfxCapture.fpsEnd());
         extras.add(c);
      });
   }

   @Override
   protected int takes(AutoTest s, int t) {
      for (StageCapture.Tech x : this.techniques(s)) {
         t = this.technique(s, t, x);
      }

      return t;
   }

   private static record Tech(
      String pre,
      String label,
      Consumer<ServerPlayer> prep,
      Consumer<ServerPlayer> cast,
      Consumer<Minecraft> main,
      @Nullable Consumer<Minecraft> cave,
      int length,
      int seq,
      int step,
      int peak,
      int clipFrom,
      int slowFrom,
      int sweepFrom,
      int sweepTicks,
      int prepTicks,
      int again,
      float pitch
   ) {
      Tech(
         String pre,
         String label,
         Consumer<ServerPlayer> prep,
         Consumer<ServerPlayer> cast,
         Consumer<Minecraft> main,
         @Nullable Consumer<Minecraft> cave,
         int length,
         int seq,
         int step,
         int peak,
         int clipFrom,
         int slowFrom,
         int sweepFrom,
         int sweepTicks
      ) {
         this(pre, label, prep, cast, main, cave, length, seq, step, peak, clipFrom, slowFrom, sweepFrom, sweepTicks, 0, -1, sweepTicks > 0 ? 26.0F : 8.0F);
      }
   }
}
