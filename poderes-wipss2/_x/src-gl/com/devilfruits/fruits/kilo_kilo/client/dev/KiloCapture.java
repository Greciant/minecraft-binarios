package com.devilfruits.fruits.kilo_kilo.client.dev;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.core.physics.PhysicsStructure;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.fruits.kilo_kilo.KiloAbilities;
import com.devilfruits.fruits.kilo_kilo.KiloKilo;
import com.devilfruits.registry.DFAttachments;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;

public final class KiloCapture implements AutoTest.Script {
   private final boolean wide;

   public KiloCapture(boolean wide) {
      this.wide = wide;
   }

   private static void log(String text, Object... args) {
      DevilFruits.LOGGER.info("[devilfruits autotest] kilo " + text, args);
   }

   private static void command(ServerPlayer p, String command) {
      p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withSuppressedOutput().withPermission(4), command);
   }

   @Override
   public int write(AutoTest s, int start) {
      String prefix = this.wide ? "vfx_kilo_" : "kilo_";
      s.add(start, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_FRONT);
         s.refresh(mc, KiloKilo.ID);
      });
      s.add(start + 6, mc -> s.server(mc, p -> FruitRules.grantPower(p, KiloKilo.fruit(), true)));
      s.add(start + 14, mc -> s.shot(mc, prefix + "00_awaken"));
      s.add(start + 40, mc -> s.tap(KiloAbilities.ONE_KILO));
      s.add(start + 52, mc -> s.shot(mc, prefix + "01_light_ramping"));
      s.add(start + 80, mc -> s.shot(mc, prefix + "02_light_front"));
      s.add(start + 84, mc -> s.camera(mc, CameraType.THIRD_PERSON_BACK));
      s.add(
         start + 88,
         mc -> {
            s.shot(mc, prefix + "03_hud_light");
            log(
               "weight {} kg, gravity {}, jump {}",
               KiloKilo.weight(mc.player).kg(),
               mc.player.getAttributeValue(Attributes.GRAVITY),
               mc.player.getAttributeValue(Attributes.JUMP_STRENGTH)
            );
         }
      );
      int[] jumpFrom = new int[]{0};
      s.add(start + 92, mc -> {
         jumpFrom[0] = (int)Math.floor(mc.player.getY());
         mc.player.jumpFromGround();
      });
      s.add(start + 98, mc -> s.shot(mc, prefix + "04_light_jump"));
      s.add(start + 150, mc -> s.shot(mc, prefix + "05_light_high"));
      s.add(start + 215, mc -> log("jump rose {} blocks so far (apex after ~120 ticks)", mc.player.getY() - (double)jumpFrom[0]));
      s.add(start + 220, mc -> mc.options.keyJump.setDown(true));
      s.add(start + 250, mc -> s.shot(mc, prefix + "06_glide"));
      s.add(start + 262, mc -> {
         mc.options.keyJump.setDown(false);
         log("glide fall speed {} blocks/tick", mc.player.getDeltaMovement().y);
      });
      s.add(start + 270, mc -> {
         s.refresh(mc, KiloKilo.ID);
         s.tap(KiloAbilities.PRESS);
      });
      s.add(start + 280, mc -> s.shot(mc, prefix + "07_heavy_ramping"));
      s.add(start + 312, mc -> s.camera(mc, CameraType.THIRD_PERSON_FRONT));
      s.add(start + 316, mc -> s.shot(mc, prefix + "08_heavy_front"));
      s.add(start + 320, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
         mc.player.setYRot(90.0F);
         mc.options.keyUp.setDown(true);
      });
      s.add(start + 330, mc -> s.shot(mc, prefix + "09_hud_heavy_walking"));
      s.add(start + 346, mc -> s.shot(mc, prefix + "10_footsteps"));
      s.add(start + 352, mc -> {
         mc.options.keyUp.setDown(false);
         s.camera(mc, CameraType.FIRST_PERSON);
      });
      s.add(start + 362, mc -> s.shot(mc, prefix + "11_first_person_heavy"));
      s.add(start + 370, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
         s.tap(KiloAbilities.PRESS);
         s.server(mc, p -> hut(p.serverLevel(), s.origin()));
      });
      s.add(start + 420, mc -> s.server(mc, p -> {
            DFAttachments.data(p).clearCooldowns();
            BlockPos o = s.origin();
            p.teleportTo(p.serverLevel(), (double)o.getX() - 13.5, (double)(o.getY() + 30), (double)o.getZ() + 0.5, -90.0F, 70.0F);
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
         }));
      s.add(start + 432, mc -> s.tap(KiloAbilities.PRESS));
      s.add(start + 435, mc -> s.shot(mc, prefix + "12_press_plunge"));
      s.add(start + 440, mc -> s.shot(mc, prefix + "13_press_falling"));
      s.add(start + 444, mc -> s.shot(mc, prefix + "14_press_through"));
      s.add(start + 448, mc -> s.shot(mc, prefix + "15_press_impact"));
      s.add(start + 456, mc -> s.shot(mc, prefix + "16_press_after"));
      s.add(start + 470, mc -> s.server(mc, p -> {
            BlockPos o = s.origin();
            int open = 0;

            for (int y = 3; y <= 9; y += 3) {
               if (p.serverLevel().getBlockState(new BlockPos(o.getX() - 14, o.getY() + y - 1, o.getZ())).isAir()) {
                  open++;
               }
            }

            log("press: {} of 3 floors broken, now {} blocks above the hut floor", open, p.getY() - (double)o.getY());
         }));
      s.add(start + 478, mc -> s.tap(KiloAbilities.PRESS));
      s.add(start + 480, mc -> s.server(mc, p -> {
            if (!PhysicsWorld.active()) {
               log("sable not installed: raft skipped");
            } else {
               BlockPos o = s.origin();
               ServerLevel level = p.serverLevel();

               for (int x = -2; x <= 2; x++) {
                  for (int z = 9; z <= 13; z++) {
                     level.setBlockAndUpdate(new BlockPos(x, o.getY(), z), Blocks.OAK_PLANKS.defaultBlockState());
                  }
               }

               command(p, String.format("sable assemble area %d %d %d %d %d %d", -2, o.getY(), 9, 2, o.getY(), 13));
            }
         }));
      s.add(start + 520, mc -> s.server(mc, p -> {
            BlockPos o = s.origin();
            p.teleportTo(p.serverLevel(), 1.6, (double)o.getY() + 1.5, 12.6, 200.0F, 35.0F);
            command(p, "devilfruit sable info");
         }));
      s.add(start + 540, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_FRONT);
         s.server(mc, p -> {
            PhysicsStructure raft = PhysicsWorld.structureUnder(p);
            log("raft under the user: {} mass {} at y {}", raft != null, raft != null ? raft.mass() : 0.0, p.getY());
         });
      });
      s.add(start + 546, mc -> s.shot(mc, prefix + "17_raft_normal"));
      s.add(start + 550, mc -> s.tap(KiloAbilities.PRESS));
      s.add(start + 600, mc -> s.shot(mc, prefix + "18_raft_10t"));
      s.add(start + 640, mc -> {
         s.shot(mc, prefix + "19_raft_sinking");
         s.server(mc, p -> {
            PhysicsStructure raft = PhysicsWorld.structureUnder(p);
            log("raft after 4 s of 10 t: under {} y {} in water {}", raft != null, p.getY(), p.isInWater());
            command(p, "devilfruit sable test jump 1");
            command(p, "devilfruit sable test jump 10000");
            command(p, "devilfruit sable test load 10000 2");
            command(p, "devilfruit sable test slam 10000 3");
         });
      });
      s.add(start + 690, mc -> {
         s.shot(mc, prefix + "20_raft_commands");
         s.tap(KiloAbilities.PRESS);
      });
      return start + 700;
   }

   private static void hut(ServerLevel level, BlockPos o) {
      for (int y = 1; y <= 9; y++) {
         for (int x = -16; x <= -12; x++) {
            for (int z = -2; z <= 2; z++) {
               boolean pillar = (x == -16 || x == -12) && (z == -2 || z == 2);
               boolean floor = y % 3 == 0;
               level.setBlockAndUpdate(
                  new BlockPos(o.getX() + x, o.getY() + y - 1, o.getZ() + z),
                  pillar ? Blocks.STONE_BRICKS.defaultBlockState() : (floor ? Blocks.OAK_PLANKS.defaultBlockState() : Blocks.AIR.defaultBlockState())
               );
            }
         }
      }
   }
}
