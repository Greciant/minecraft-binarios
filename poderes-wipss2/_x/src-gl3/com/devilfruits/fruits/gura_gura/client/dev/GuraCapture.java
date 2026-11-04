package com.devilfruits.fruits.gura_gura.client.dev;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureStage;
import com.devilfruits.client.dev.VfxCapture;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.fruits.gura_gura.GuraAbilities;
import com.devilfruits.fruits.gura_gura.GuraGura;
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

public final class GuraCapture extends CaptureStage {
   private final boolean baseline;
   @Nullable
   private final String only;

   public GuraCapture(boolean baseline, @Nullable String only) {
      super("vfx_g_");
      this.baseline = baseline;
      this.only = only;
   }

   @Override
   protected ResourceLocation fruit() {
      return GuraGura.ID;
   }

   @Override
   protected void setup(ServerPlayer p) {
      DFAttachments.data(p).setMastery(10);
      command(p, "item replace entity @s weapon.mainhand with devilfruits:bisento");
   }

   @Override
   protected void onReset(ServerPlayer p) {
      PlayerFruitData data = DFAttachments.data(p);
      data.clearCooldowns();
      data.setEnergy((float)Mastery.maxEnergy(data));
      AbilityManager.stopToggle(p, GuraAbilities.SHINDO);
   }

   private static Consumer<ServerPlayer> cast(String ability, int hold) {
      return p -> castAbility(p, "devilfruits:gura_gura/" + ability, hold);
   }

   private static void pool(AutoTest s, ServerPlayer p) {
      ServerLevel level = p.serverLevel();
      int y = s.origin().getY();

      for (int x = -16; x <= 16; x++) {
         for (int z = -44; z <= -8; z++) {
            for (int d = 1; d <= 3; d++) {
               level.setBlock(new BlockPos(x, y - d, z), d == 3 ? Blocks.SAND.defaultBlockState() : Blocks.WATER.defaultBlockState(), 2);
            }
         }
      }
   }

   private void still(AutoTest s, int tick, String pre, String name) {
      s.add(tick, mc -> s.shot(mc, pre + name));
   }

   private List<GuraCapture.Tech> techniques(AutoTest s) {
      Consumer<ServerPlayer> near = p -> dummyTo(s, p, -6.5);
      Consumer<ServerPlayer> none = none();
      Consumer<Minecraft> side = rig(7.0, 135.0F, 12.0F, 0.4);
      Consumer<Minecraft> wide = rig(12.0, 150.0F, 18.0F, 0.4);
      Consumer<Minecraft> high = rig(26.0, 160.0F, 30.0F, 0.4);
      CaptureStage.Beats arrows = c -> {
         for (int k = 0; k < 3; k++) {
            int dx = k - 1;
            s.add(
               c + 14 + k * 6,
               mc -> s.server(
                     mc, p -> command(p, "summon arrow " + (0.5 + (double)dx) + " " + ((double)s.origin().getY() + 1.4) + " -9.5 {Motion:[0.0,0.05,1.1]}")
                  )
            );
         }
      };
      return List.of(
         new GuraCapture.Tech("vfx_gks_", "kaishin", near, cast("kaishin", 36), side, 64, 56, 2, 38, 10, 26, true, null),
         new GuraCapture.Tech("vfx_ggk_", "gekishin", near, cast("gekishin", 1), rig(7.5, 125.0F, 10.0F, 0.4), 50, 40, 1, 6, -2, -2, true, null),
         new GuraCapture.Tech("vfx_gkb_", "kabutowari", none, cast("kabutowari", 1), wide, 80, 72, 2, 30, -2, 14, false, null),
         new GuraCapture.Tech("vfx_gkg_", "kaisho ground", none, cast("kaisho", 1), wide, 50, 40, 2, 12, -2, -2, false, null),
         new GuraCapture.Tech("vfx_gau_", "quake aura", none, cast("shindo", 1), side, 50, 44, 2, 22, -2, 10, false, arrows),
         new GuraCapture.Tech("vfx_gsl_", "quake slash", none, cast("quake_infusion", 1), rig(8.0, 130.0F, 12.0F, 0.4), 30, 24, 1, 6, -2, -2, true, null),
         new GuraCapture.Tech("vfx_gsy_", "shima yurashi", none, cast("shima_yurashi", 1), high, 120, 112, 4, 30, 2, 6, false, null),
         new GuraCapture.Tech("vfx_gkw_", "kaisho tsunami", near, cast("kaisho", 1), rig(24.0, 62.0F, 12.0F, 0.4), 50, 44, 2, 20, -2, 6, false, null)
      );
   }

   private int technique(AutoTest s, int t, GuraCapture.Tech x) {
      if (this.only != null && !this.only.contains(x.pre())) {
         return t;
      } else {
         String pre = x.pre();
         if (pre.equals("vfx_gkw_")) {
            s.add(t, mc -> s.server(mc, p -> pool(s, p)));
            t += 2;
         }

         CaptureStage.Beats extra = x.extra() != null ? x.extra() : c -> {
         };
         t = this.take(s, t, 5, x.prep(), 0, x.cast(), x.main(), x.length(), c -> {
            int k = 0;

            while (k <= x.seq()) {
               this.still(s, c + k, pre, String.format("seq_%03d", k));
               k += x.step();
            }

            extra.add(c);
         });
         if (this.baseline) {
            return t;
         } else {
            if (x.firstPerson()) {
               t = this.take(s, t, 5, x.prep(), 0, x.cast(), firstPerson(), x.length(), c -> {
                  this.still(s, c + x.peak(), pre, "04_first_person");
                  extra.add(c);
               });
            }

            t = this.take(s, t, 3, x.prep(), 0, x.cast(), x.main(), x.length() + 40, c -> {
               s.add(c + x.clipFrom(), mc -> VfxCapture.clip(pre + "clip", 90, 0.6666666666666666));
               extra.add(c);
            });
            t = this.take(s, t, 3, x.prep(), 0, x.cast(), x.main(), x.length() + 40, c -> {
               s.add(c + x.slowFrom(), mc -> VfxCapture.clip(pre + "slow", 120, 0.3333333333333333));
               extra.add(c);
            });
            s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.LOW));
            t = this.take(s, t + 2, 5, x.prep(), 0, x.cast(), x.main(), x.length(), c -> {
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
   }

   @Override
   protected int takes(AutoTest s, int t) {
      for (GuraCapture.Tech x : this.techniques(s)) {
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
