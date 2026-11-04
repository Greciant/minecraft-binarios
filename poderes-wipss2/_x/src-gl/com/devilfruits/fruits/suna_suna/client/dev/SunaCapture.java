package com.devilfruits.fruits.suna_suna.client.dev;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureStage;
import com.devilfruits.client.dev.VfxCapture;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.fruits.suna_suna.SunaAbilities;
import com.devilfruits.fruits.suna_suna.SunaSuna;
import com.devilfruits.fruits.suna_suna.client.vfx.LaSpadaCinematic;
import com.devilfruits.registry.DFAttachments;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class SunaCapture extends CaptureStage {
   private static final int STILL_RATE = 8;
   private static final int CLIP_RATE = 5;
   private final boolean baseline;
   private final boolean look;
   @Nullable
   private final String only;
   @Nullable
   private AutoTest stage;

   public SunaCapture(boolean baseline, @Nullable String only) {
      this(baseline, false, only);
   }

   public SunaCapture(boolean baseline, boolean look, @Nullable String only) {
      super("vfx_s_");
      this.baseline = baseline;
      this.look = look;
      this.only = only;
   }

   @Override
   protected ResourceLocation fruit() {
      return SunaSuna.ID;
   }

   @Override
   protected void setup(ServerPlayer p) {
      DFAttachments.data(p).setMastery(10);
      desert(p, this.stage != null ? this.stage.origin().getY() : p.blockPosition().getY());
      command(p, "time set 3000");
   }

   @Override
   protected void onReset(ServerPlayer p) {
      PlayerFruitData data = DFAttachments.data(p);
      data.clearCooldowns();
      data.setEnergy((float)Mastery.maxEnergy(data));
      AbilityManager.stopToggle(p, SunaAbilities.SAND_CLOUD);
      p.getAbilities().flying = false;
      p.onUpdateAbilities();
      Minecraft.getInstance().execute(() -> Minecraft.getInstance().options.keyUp.setDown(false));
   }

   private static void desert(ServerPlayer p, int y) {
      ServerLevel level = p.serverLevel();
      BlockState sand = Blocks.SAND.defaultBlockState();
      BlockState stone = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
      BlockState air = Blocks.AIR.defaultBlockState();

      for (int x = -34; x <= 34; x++) {
         for (int z = -64; z <= 14; z++) {
            boolean edge = x == -34 || x == 34 || z == -64 || z == 14;
            level.setBlock(new BlockPos(x, y - 2, z), stone, 2);
            level.setBlock(new BlockPos(x, y - 1, z), edge ? stone : sand, 2);

            for (int h = 0; h < 24; h++) {
               if (!level.getBlockState(new BlockPos(x, y + h, z)).isAir()) {
                  level.setBlock(new BlockPos(x, y + h, z), air, 2);
               }
            }
         }
      }
   }

   private static Consumer<ServerPlayer> cast(String ability, int hold) {
      return p -> castAbility(p, "devilfruits:suna_suna/" + ability, hold);
   }

   private void still(AutoTest s, int tick, String pre, String name) {
      s.add(tick, mc -> s.shot(mc, pre + name));
   }

   private List<SunaCapture.Tech> techniques(AutoTest s) {
      Consumer<ServerPlayer> near = p -> dummyTo(s, p, -5.5);
      Consumer<ServerPlayer> none = none();
      Consumer<Minecraft> side = rig(7.0, 135.0F, 12.0F, 0.4);
      Consumer<Minecraft> wide = rig(13.0, 150.0F, 18.0F, 0.4);
      Consumer<Minecraft> high = rig(16.0, 160.0F, 34.0F, 0.4);
      CaptureStage.Beats arrows = c -> {
         double[][] at = new double[][]{{0.5, 1.25}, {0.87, 1.15}, {0.5, 1.65}};

         for (int k = 0; k < 3; k++) {
            double[] a = at[k];
            s.add(
               c + 2 + k * 8,
               mc -> s.server(mc, p -> command(p, "summon arrow " + a[0] + " " + ((double)s.origin().getY() + a[1]) + " -9.5 {Motion:[0.0,0.05,1.4]}"))
            );
         }

         s.add(
            c + 30,
            mc -> s.server(
                  mc, p -> command(p, "summon fireball 0.5 " + ((double)s.origin().getY() + 1.0) + " -8.5 {Motion:[0.0,0.0,0.9],acceleration_power:0.0d}")
               )
         );
      };
      CaptureStage.Beats fly = c -> {
         s.add(c + 8, mc -> mc.options.keyUp.setDown(true));
         s.add(c + 38, mc -> mc.options.keyUp.setDown(false));
      };
      return List.of(
         new SunaCapture.Tech("vfx_spb_", "pesado ball", none, cast("sables_pesado", 30), side, 64, 56, 2, 38, 4, 22, true, null),
         new SunaCapture.Tech("vfx_spt_", "pesado blast", none, cast("sables_pesado", 1), side, 24, 20, 1, 4, -2, -2, true, null),
         new SunaCapture.Tech("vfx_ssp_", "desert spada", none, cast("desert_spada", 1), rig(9.0, 145.0F, 15.0F, 0.4), 40, 32, 2, 8, -2, -2, false, null),
         new SunaCapture.Tech("vfx_scl_", "sand cloud", none, cast("sand_cloud", 1), rig(8.0, 140.0F, 14.0F, 0.2), 60, 56, 4, 24, -2, 6, false, fly),
         new SunaCapture.Tech("vfx_sbj_", "barjan", none, cast("barjan", 1), rig(8.0, 150.0F, 32.0F, 0.4), 30, 24, 1, 6, -2, -2, true, null),
         new SunaCapture.Tech("vfx_sgs_", "ground secco", none, cast("ground_secco", 70), high, 90, 80, 4, 50, 2, 10, false, null),
         new SunaCapture.Tech(
            "vfx_sgr_", "desert girasole", none, cast("desert_girasole", 100), rig(15.0, 165.0F, 40.0F, 0.4), 130, 120, 6, 60, 2, 10, false, null
         ),
         new SunaCapture.Tech("vfx_sls_", "desert la spada", none, cast("desert_la_spada", 1), rig(6.5, 112.0F, 6.0F, 0.6), 30, 24, 1, 8, -2, -2, true, null),
         new SunaCapture.Tech("vfx_sen_", "desert encierro", near, cast("desert_encierro", 60), side, 80, 72, 4, 30, 2, 10, true, null),
         new SunaCapture.Tech("vfx_sts_", "sand tsunami", none, cast("sand_tsunami", 40), rig(24.0, 25.0F, 14.0F, 0.4), 90, 84, 3, 56, 10, 34, false, null),
         new SunaCapture.Tech("vfx_ssb_", "sables", none, cast("sables", 1), rig(30.0, 160.0F, 22.0F, 0.4), 270, 264, 12, 80, 4, 20, false, null),
         new SunaCapture.Tech(
            "vfx_sld_",
            "la spada close",
            none,
            p -> {
            },
            rig(3.2, 100.0F, 4.0F, 0.6),
            40,
            36,
            2,
            10,
            -2,
            -2,
            false,
            c -> {
               for (int k = 0; k < 3; k++) {
                  int kk = k;
                  s.add(
                     c + k * 3,
                     mc -> {
                        if (mc.level != null && mc.player != null) {
                           Vec3 at = mc.player.position().add(-1.6 + (double)kk * 0.2, 1.1 + (double)kk * 0.45, -2.2 - (double)kk * 0.4);
                           VfxDirector.spawn(
                              new LaSpadaCinematic(mc.level, VfxAnchor.of(null, at), new VfxParams(at, new Vec3(0.09, 0.0, -0.02), 1.0F, 77L + (long)kk, 60))
                           );
                        }
                     }
                  );
               }
            }
         ),
         new SunaCapture.Tech("vfx_slg_", "sand body", none, p -> {
         }, rig(3.6, 125.0F, 6.0F, 0.2), 48, 46, 1, 6, -2, -2, false, arrows)
      );
   }

   private int technique(AutoTest s, int t, SunaCapture.Tech x) {
      if (this.only != null && !this.only.contains(x.pre())) {
         return t;
      } else if (!x.pre().equals("vfx_sld_") || this.only != null && this.only.contains("vfx_sld_")) {
         String pre = x.pre();
         CaptureStage.Beats extra = x.extra() != null ? x.extra() : c -> {
         };
         if (this.baseline) {
            return this.take(s, t, 20, x.prep(), 0, x.cast(), x.main(), x.length(), c -> {
               int k = 0;

               while (k <= x.seq()) {
                  this.still(s, c + k, pre, String.format("seq_%03d", k));
                  k += Math.max(2, x.step());
               }

               extra.add(c);
            });
         } else {
            t = this.take(s, t, 8, x.prep(), 0, x.cast(), x.main(), x.length(), c -> {
               int k = 0;

               while (k <= x.seq()) {
                  this.still(s, c + k, pre, String.format("seq_%03d", k));
                  k += x.step();
               }

               extra.add(c);
            });
            if (this.look) {
               return t;
            } else {
               if (x.firstPerson()) {
                  t = this.take(s, t, 8, x.prep(), 0, x.cast(), firstPerson(), x.length(), c -> {
                     this.still(s, c + x.peak(), pre, "04_first_person");
                     extra.add(c);
                  });
               }

               t = this.take(s, t, 5, x.prep(), 0, x.cast(), x.main(), x.length() + 40, c -> {
                  s.add(c + x.clipFrom(), mc -> VfxCapture.clip(pre + "clip", Math.min(180, (x.length() + 10) * 3 / 2), 0.6666666666666666));
                  extra.add(c);
               });
               t = this.take(s, t, 5, x.prep(), 0, x.cast(), x.main(), x.length() + 40, c -> {
                  s.add(c + x.slowFrom(), mc -> VfxCapture.clip(pre + "slow", 90, 0.3333333333333333));
                  extra.add(c);
               });
               s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.LOW));
               t = this.take(s, t + 2, 8, x.prep(), 0, x.cast(), x.main(), x.length(), c -> {
                  this.still(s, c + x.peak(), pre, "cmp_low_peak");
                  extra.add(c);
               });
               s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.ULTRA));
               return this.take(s, t + 2, 20, x.prep(), 0, x.cast(), x.main(), x.length() + 20, c -> {
                  s.add(c, mc -> VfxCapture.fps("ULTRA " + x.label()));
                  s.add(c + x.length(), mc -> VfxCapture.fpsEnd());
                  extra.add(c);
               });
            }
         }
      } else {
         return t;
      }
   }

   @Override
   protected int takes(AutoTest s, int t) {
      this.stage = s;
      t += 200;

      for (SunaCapture.Tech x : this.techniques(s)) {
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
      int length,
      int seq,
      int step,
      int peak,
      int clipFrom,
      int slowFrom,
      boolean firstPerson,
      @Nullable CaptureStage.Beats extra
   ) {
   }
}
