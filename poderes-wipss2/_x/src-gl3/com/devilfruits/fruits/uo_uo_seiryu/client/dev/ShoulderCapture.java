package com.devilfruits.fruits.uo_uo_seiryu.client.dev;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.camera.ShoulderCamera;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureRig;
import com.devilfruits.client.dev.CaptureStage;
import com.devilfruits.core.ability.AimHelper;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.registry.DFAttachments;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class ShoulderCapture extends CaptureStage {
   public ShoulderCapture() {
      super("vfx_sh_");
   }

   @Override
   protected ResourceLocation fruit() {
      return UoUoSeiryu.ID;
   }

   @Override
   protected void setup(ServerPlayer p) {
      DFAttachments.data(p).setMastery(10);
      command(p, "time set 6000");
   }

   @Override
   protected void onReset(ServerPlayer p) {
      PlayerFruitData data = DFAttachments.data(p);
      data.clearCooldowns();
      data.setEnergy((float)Mastery.maxEnergy(data));
   }

   private static void log(String message) {
      DevilFruits.LOGGER.info("[devilfruits autotest] shoulder: {}", message);
   }

   private static void f5(AutoTest s, int tick) {
      s.add(tick, mc -> KeyMapping.click(mc.options.keyTogglePerspective.getKey()));
      s.add(tick + 2, mc -> log("F5 -> " + ShoulderCamera.describe()));
   }

   private static void wall(AutoTest s, ServerPlayer p, boolean side, boolean build) {
      ServerLevel level = p.serverLevel();
      int y = s.origin().getY();
      BlockState state = build ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.AIR.defaultBlockState();

      for (int h = 0; h < 4; h++) {
         for (int i = -4; i <= 4; i++) {
            level.setBlock(side ? new BlockPos(1, y + h, i - 2) : new BlockPos(i, y + h, -2), state, 2);
         }
      }
   }

   @Override
   protected int takes(AutoTest s, int t) {
      Consumer<Minecraft> keep = mc -> {
      };
      t += 20;
      s.add(t, mc -> {
         mc.options.hideGui = false;
         CaptureRig.off();
         mc.options.setCameraType(CameraType.FIRST_PERSON);
         CoreClientConfig.SHOULDER_SIDE.set(CoreClientConfig.ShoulderSide.RIGHT);
         log("start " + ShoulderCamera.describe());
      });

      for (int i = 1; i <= 7; i++) {
         f5(s, t + 10 * i);
      }

      t += 80;
      t = this.take(s, t, 20, none(), 0, none(), keep, 30, c -> this.shot(s, c + 20, "right"));
      t = this.take(s, t, 20, none(), 0, none(), keep, 30, c -> {
         s.add(c + 2, mc -> CoreClientConfig.SHOULDER_SIDE.set(CoreClientConfig.ShoulderSide.LEFT));
         this.shot(s, c + 4, "swap_mid");
         this.shot(s, c + 20, "left");
         s.add(c + 26, mc -> CoreClientConfig.SHOULDER_SIDE.set(CoreClientConfig.ShoulderSide.RIGHT));
      });
      t = this.take(s, t, 20, p -> wall(s, p, true, true), 4, none(), keep, 30, c -> this.shot(s, c + 20, "wall_side"));
      s.add(t, mc -> s.server(mc, p -> wall(s, p, true, false)));
      t = this.take(s, t + 2, 20, p -> wall(s, p, false, true), 4, none(), keep, 30, c -> {
         this.shot(s, c + 20, "wall_back");
         s.add(c + 21, mc -> log("wall_back: own model hidden = " + ShoulderCamera.hideModel()));
      });
      s.add(t, mc -> s.server(mc, p -> wall(s, p, false, false)));
      t = this.take(
         s,
         t + 2,
         20,
         none(),
         0,
         p -> {
            AimHelper.debug = true;
            castAbility(p, "devilfruits:uo_uo_seiryu/blast_breath", 10);
         },
         keep,
         40,
         c -> {
            s.add(
               c - 1,
               mc -> log(
                     "crosshair point "
                        + ShoulderCamera.crosshairPoint(mc, mc.player)
                        + ", eye "
                        + mc.player.getEyePosition()
                        + ", look "
                        + mc.player.getLookAngle()
                  )
            );
            this.shot(s, c + 11, "aim");
            this.shot(s, c + 14, "aim_late");
            s.add(c + 36, mc -> AimHelper.debug = false);
         }
      );
      s.add(t, mc -> s.server(mc, p -> FormManager.request(p, "hybrid", FormManager.Reason.COMMAND)));
      t = this.take(s, t + 60, 20, none(), 0, none(), keep, 50, c -> this.shot(s, c + 40, "hybrid"));
      s.add(t, mc -> s.server(mc, p -> FormManager.revert(p, FormManager.Reason.COMMAND)));
      t = this.take(s, t + 60, 20, none(), 0, none(), keep, 40, c -> {
         this.shot(s, c + 20, "right_again");
         s.add(c + 24, mc -> KeyMapping.click(mc.options.keyTogglePerspective.getKey()));
         this.shot(s, c + 26, "leaving");
      });
      s.add(t, mc -> log("after the slide in " + ShoulderCamera.describe()));
      return t + 2;
   }
}
