package com.devilfruits.fruits.uo_uo_seiryu.client.dev;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureStage;
import com.devilfruits.client.dev.VfxCapture;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.registry.DFAttachments;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

public final class JumpCapture extends CaptureStage {
   private static final int STILL_RATE = 8;
   private static final int CLIP_RATE = 5;
   private static final int RELEASE = 33;
   private final boolean look;

   public JumpCapture(boolean look) {
      super("vfx_j_");
      this.look = look;
   }

   @Override
   protected ResourceLocation fruit() {
      return UoUoSeiryu.ID;
   }

   @Override
   protected void setup(ServerPlayer p) {
      DFAttachments.data(p).setMastery(10);
      FormManager.request(p, "hybrid", FormManager.Reason.COMMAND);
      command(p, "time set 6000");
   }

   @Override
   protected void onReset(ServerPlayer p) {
      PlayerFruitData data = DFAttachments.data(p);
      data.clearCooldowns();
      data.setEnergy((float)Mastery.maxEnergy(data));
      if (!UoUoSeiryu.isIn(p, "hybrid")) {
         FormManager.request(p, "hybrid", FormManager.Reason.COMMAND);
      }

      p.clearFire();
      Minecraft.getInstance().execute(JumpCapture::keysUp);
   }

   private static void keysUp() {
      Minecraft mc = Minecraft.getInstance();
      mc.options.keyShift.setDown(false);
      mc.options.keyJump.setDown(false);
      mc.options.keyUp.setDown(false);
   }

   private static void keys(AutoTest s, int c, boolean forward) {
      s.add(c, mc -> {
         mc.options.keyShift.setDown(true);
         mc.options.keyJump.setDown(true);
      });
      if (forward) {
         s.add(c + 33 - 2, mc -> mc.options.keyUp.setDown(true));
      }

      s.add(c + 33, mc -> mc.options.keyJump.setDown(false));
      s.add(c + 33 + 1, mc -> mc.options.keyShift.setDown(false));
      if (forward) {
         s.add(c + 33 + 12, mc -> mc.options.keyUp.setDown(false));
      }
   }

   private static void fire(AutoTest s, ServerPlayer p) {
      ServerLevel level = p.serverLevel();
      int y = s.origin().getY();

      for (int x = -1; x <= 1; x++) {
         for (int z = -4; z <= -2; z++) {
            level.setBlock(new BlockPos(x, y - 1, z), Blocks.NETHERRACK.defaultBlockState(), 2);
            level.setBlock(new BlockPos(x, y, z), Blocks.FIRE.defaultBlockState(), 2);
         }
      }
   }

   private static void unfire(AutoTest s, ServerPlayer p) {
      ServerLevel level = p.serverLevel();
      int y = s.origin().getY();

      for (int x = -1; x <= 1; x++) {
         for (int z = -4; z <= -2; z++) {
            level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(new BlockPos(x, y - 1, z), Blocks.STONE_BRICKS.defaultBlockState(), 2);
         }
      }
   }

   private void still(AutoTest s, int tick, String name) {
      s.add(tick, mc -> s.shot(mc, name));
   }

   @Override
   protected int takes(AutoTest s, int t) {
      Consumer<ServerPlayer> away = p -> dummyTo(s, p, -40.0);
      Consumer<ServerPlayer> none = p -> {
      };
      Consumer<Minecraft> wide = rig(17.0, 125.0F, 6.0F, 0.5);
      Consumer<Minecraft> side = rig(15.0, 95.0F, 6.0F, 0.5);
      int length = 103;
      t += 40;
      t = this.take(s, t, 8, away, 0, none, wide, length, c -> {
         keys(s, c, false);

         for (int k = 0; k <= length; k += 3) {
            this.still(s, c + k, String.format("vfx_jup_seq_%03d", k));
         }
      });
      t = this.take(s, t, 8, away, 0, none, side, length, c -> {
         keys(s, c, true);

         for (int k = 0; k <= length; k += 3) {
            this.still(s, c + k, String.format("vfx_jfw_seq_%03d", k));
         }
      });
      t = this.take(s, t, 8, away, 0, none, firstPerson(), length, c -> {
         keys(s, c, true);

         for (int k : new int[]{10, 28, 34, 37, 45, 57, 73}) {
            this.still(s, c + k, String.format("vfx_jfw_fp_%03d", k));
         }
      });
      s.add(t, mc -> s.server(mc, p -> fire(s, p)));
      t = this.take(s, t + 2, 20, away, 0, none, rig(6.0, 140.0F, 8.0F, 0.4), 50, c -> {
         this.still(s, c + 20, "vfx_jfi_third");
         s.add(c + 26, mc -> firstPerson().accept(mc));
         this.still(s, c + 34, "vfx_jfi_first");
      });
      s.add(t, mc -> s.server(mc, p -> unfire(s, p)));
      t += 2;
      if (this.look) {
         return t;
      } else {
         t = this.take(s, t, 5, away, 0, none, side, length + 20, c -> {
            keys(s, c, true);
            s.add(c + 2, mc -> VfxCapture.clip("vfx_jfw_clip", 160, 0.6666666666666666));
         });
         t = this.take(s, t, 5, away, 0, none, wide, length + 20, c -> {
            keys(s, c, false);
            s.add(c + 2, mc -> VfxCapture.clip("vfx_jup_clip", 160, 0.6666666666666666));
         });
         s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.LOW));
         t = this.take(s, t + 2, 8, away, 0, none, side, length, c -> {
            keys(s, c, true);

            for (int k : new int[]{20, 34, 45, 73}) {
               this.still(s, c + k, String.format("vfx_jfw_cmp_low_%03d", k));
            }
         });
         t = this.take(s, t + 2, 20, away, 0, none, side, length + 10, c -> {
            keys(s, c, true);
            s.add(c, mc -> VfxCapture.fps("LOW hybrid charged jump"));
            s.add(c + length, mc -> VfxCapture.fpsEnd());
         });
         s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.ULTRA));
         return this.take(s, t + 2, 20, away, 0, none, side, length + 10, c -> {
            keys(s, c, true);
            s.add(c, mc -> VfxCapture.fps("ULTRA hybrid charged jump"));
            s.add(c + length, mc -> VfxCapture.fpsEnd());
         });
      }
   }
}
