package com.devilfruits.fruits.uo_uo_seiryu.client.dev;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureRig;
import com.devilfruits.client.dev.VfxCapture;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.mobs.pirate.Pirates;
import com.devilfruits.mobs.pirate.entity.PirateFighter;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class HellfireCaptureScript implements AutoTest.Script {
   private static final double START_Z = -2.5;
   private static final double DUMMY_Z = -10.5;
   private static final int FULL = 78;
   private static final String P = "vfx_hr_";

   private static void command(ServerPlayer p, String cmd) {
      p.getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), cmd);
   }

   private static void arena(AutoTest s, ServerPlayer p) {
      ServerLevel level = p.serverLevel();
      int y = s.origin().getY();

      for (int x = -7; x <= 7; x++) {
         for (int z = -16; z <= 2; z++) {
            boolean edge = x == -7 || x == 7 || z == -16 || z == 2;
            level.setBlockAndUpdate(new BlockPos(x, y - 1, z), edge ? Blocks.POLISHED_ANDESITE.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState());
         }
      }

      command(p, "weather clear");
      command(p, "gamerule doDaylightCycle false");
      command(p, "gamerule mobGriefing false");
   }

   private static void reset(AutoTest s, ServerPlayer p) {
      ServerLevel level = p.serverLevel();
      double y = (double)s.origin().getY();
      AABB area = new AABB(-30.0, y - 10.0, -40.0, 30.0, y + 30.0, 20.0);
      level.getEntitiesOfClass(PirateFighter.class, area).forEach(e -> e.discard());
      level.getEntitiesOfClass(Husk.class, area).forEach(e -> e.discard());
      PirateFighter dummy = (PirateFighter)Pirates.DECKHAND.get().create(level);
      if (dummy != null) {
         dummy.moveTo(0.5, y, -10.5, 0.0F, 0.0F);
         dummy.setYHeadRot(0.0F);
         dummy.setYBodyRot(0.0F);
         dummy.setNoAi(true);
         dummy.setPersistenceRequired();
         dummy.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack((ItemLike)Pirates.CUTLASS.get()));
         dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(4000.0);
         dummy.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
         dummy.setHealth(4000.0F);
         level.addFreshEntity(dummy);
      }

      p.teleportTo(level, 0.5, y, -2.5, 180.0F, 8.0F);
      p.setDeltaMovement(Vec3.ZERO);
   }

   private static void quality(CoreClientConfig.CinematicQuality q) {
      CoreClientConfig.CINEMATIC_QUALITY.set(q);
   }

   private static int take(AutoTest s, int t, int rate, int hold, Consumer<Minecraft> camera, HellfireCaptureScript.Beats beats) {
      s.add(t, mc -> s.server(mc, p -> {
            reset(s, p);
            command(p, "tick rate " + rate);
         }));
      s.add(t + 3, camera);
      int c = t + 8;
      s.add(c, mc -> s.server(mc, p -> command(p, "devilfruit cast devilfruits:uo_uo_seiryu/hellfire_ray " + hold)));
      beats.add(c);
      return c + hold + 50;
   }

   private static Consumer<Minecraft> rig(double dist, float yaw, float pitch, double lift) {
      return mc -> {
         mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
         if (mc.player != null) {
            CaptureRig.follow(mc.player, dist, yaw, pitch, lift);
         }
      };
   }

   private static Consumer<Minecraft> firstPerson() {
      return mc -> {
         CaptureRig.off();
         mc.options.setCameraType(CameraType.FIRST_PERSON);
      };
   }

   @Override
   public int write(AutoTest s, int start) {
      s.add(start, mc -> {
         VfxCapture.exactWindow(1920, 1080);
         mc.options.hideGui = true;
         quality(CoreClientConfig.CinematicQuality.ULTRA);
         CoreClientConfig.REDUCE_FLASHING.set(false);
         s.refresh(mc, UoUoSeiryu.ID);
      });
      s.add(start + 4, mc -> s.server(mc, p -> {
            arena(s, p);
            command(p, "time set 15000");
            FormManager.request(p, "hybrid", FormManager.Reason.COMMAND);
         }));
      int t = start + 70;
      Consumer<Minecraft> wide = rig(16.0, 148.0F, 15.0F, 1.0);
      Consumer<Minecraft> close = rig(8.5, 206.0F, 9.0F, 0.6);
      Consumer<Minecraft> low = rig(10.0, 96.0F, -5.0F, -0.4);
      t = take(s, t, 5, 78, wide, c -> {
         s.add(c + 10, mc -> s.shot(mc, "vfx_hr_02_release_flash"));
         s.add(c + 26, mc -> s.shot(mc, "vfx_hr_03_sustain_wide"));
         s.add(c + 84, mc -> s.shot(mc, "vfx_hr_09_explosion_wide"));
         s.add(c + 104, mc -> s.shot(mc, "vfx_hr_10_aftermath"));
      });
      t = take(s, t, 5, 78, close, c -> {
         s.add(c + 5, mc -> s.shot(mc, "vfx_hr_01_anticipation"));
         s.add(c + 20, mc -> s.shot(mc, "vfx_hr_04_sustain_close"));
         s.add(c + 35, mc -> s.shot(mc, "vfx_hr_06_hit_on_target"));
         s.add(c + 81, mc -> s.shot(mc, "vfx_hr_07_collapse"));
         s.add(c + 85, mc -> s.shot(mc, "vfx_hr_08_explosion"));
      });
      t = take(s, t, 5, 78, low, c -> s.add(c + 30, mc -> s.shot(mc, "vfx_hr_05_sustain_low_angle")));
      t = take(s, t, 5, 78, firstPerson(), c -> {
         s.add(c + 10, mc -> s.shot(mc, "vfx_hr_11_first_person_release"));
         s.add(c + 26, mc -> s.shot(mc, "vfx_hr_12_first_person_sustain"));
      });
      t = take(s, t, 3, 40, rig(13.0, 160.0F, 13.0F, 0.8), c -> s.add(c - 1, mc -> VfxCapture.clip("vfx_hr_clip", 138, 0.6666666666666666)));
      t += 10;
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.LOW));
      t = take(s, t + 2, 5, 78, close, c -> {
         s.add(c + 26, mc -> s.shot(mc, "vfx_hr_cmp_low_sustain"));
         s.add(c + 85, mc -> s.shot(mc, "vfx_hr_cmp_low_explosion"));
      });
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.ULTRA));
      t = take(s, t + 2, 5, 78, close, c -> {
         s.add(c + 26, mc -> s.shot(mc, "vfx_hr_cmp_ultra_sustain"));
         s.add(c + 85, mc -> s.shot(mc, "vfx_hr_cmp_ultra_explosion"));
      });
      s.add(t, mc -> s.server(mc, p -> command(p, "time set 6000")));
      t = take(s, t + 2, 5, 78, wide, c -> {
         s.add(c + 26, mc -> s.shot(mc, "vfx_hr_13_day_sustain"));
         s.add(c + 85, mc -> s.shot(mc, "vfx_hr_14_day_explosion"));
      });
      s.add(t, mc -> s.server(mc, p -> command(p, "time set 15000")));
      t = take(s, t + 2, 20, 78, wide, c -> {
         s.add(c + 10, mc -> VfxCapture.fps("ULTRA"));
         s.add(c + 78, mc -> VfxCapture.fpsEnd());
      });
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.LOW));
      t = take(s, t + 2, 20, 78, wide, c -> {
         s.add(c + 10, mc -> VfxCapture.fps("LOW"));
         s.add(c + 78, mc -> VfxCapture.fpsEnd());
      });
      s.add(t, mc -> {
         quality(CoreClientConfig.CinematicQuality.ULTRA);
         CaptureRig.off();
         mc.options.hideGui = false;
         mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
      });
      return t + 5;
   }

   @FunctionalInterface
   private interface Beats {
      void add(int var1);
   }
}
