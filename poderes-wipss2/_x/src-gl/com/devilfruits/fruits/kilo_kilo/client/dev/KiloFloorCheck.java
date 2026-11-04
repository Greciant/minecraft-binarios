package com.devilfruits.fruits.kilo_kilo.client.dev;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.core.physics.PhysicsStructure;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.fruits.kilo_kilo.KiloAbilities;
import com.devilfruits.fruits.kilo_kilo.KiloKilo;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

public final class KiloFloorCheck implements AutoTest.Script {
   private static final int HANG = 6;
   private PhysicsStructure raft;
   private boolean dirtGone;
   private boolean raftGone;

   private static void log(String message, Object... args) {
      DevilFruits.LOGGER.info("[devilfruits autotest] kilo floor " + message, args);
   }

   private static void command(ServerPlayer p, String command) {
      p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withSuppressedOutput().withPermission(4), command);
   }

   @Override
   public int write(AutoTest s, int start) {
      int t = start;
      s.add(start, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
         s.refresh(mc, KiloKilo.ID);
         s.server(mc, p -> {
            BlockPos o = s.origin();
            ServerLevel level = p.serverLevel();

            for (int x = -21; x <= -19; x++) {
               for (int z = -1; z <= 1; z++) {
                  level.setBlockAndUpdate(new BlockPos(x, o.getY() + 6, z), Blocks.DIRT.defaultBlockState());
               }
            }

            for (int x = -2; x <= 2; x++) {
               for (int z = 9; z <= 13; z++) {
                  level.setBlockAndUpdate(new BlockPos(x, o.getY(), z), Blocks.OAK_PLANKS.defaultBlockState());
               }
            }

            if (PhysicsWorld.active()) {
               command(p, String.format("sable assemble area %d %d %d %d %d %d", -2, o.getY(), 9, 2, o.getY(), 13));
            }
         });
      });
      s.add(start + 20, mc -> s.server(mc, p -> {
            BlockPos o = s.origin();
            p.teleportTo(p.serverLevel(), -19.5, (double)(o.getY() + 6 + 1), 0.5, 90.0F, 30.0F);
            p.fallDistance = 0.0F;
         }));
      s.add(start + 40, mc -> {
         s.server(mc, p -> log("on the dirt floor: weight {} kg, on ground {}", KiloKilo.weight(p).kg(), p.onGround()));
         s.tap(KiloAbilities.PRESS);
      });

      for (int i = start + 42; i <= t + 110; i += 2) {
         s.add(i, mc -> s.server(mc, p -> {
               boolean gone = p.serverLevel().getBlockState(new BlockPos(-20, s.origin().getY() + 6, 0)).isAir();
               if (gone && !this.dirtGone) {
                  this.dirtGone = true;
                  log("the dirt floor gave way at {} kg", KiloKilo.weight(p).kg());
               }
            }));
      }

      s.add(t + 72, mc -> s.shot(mc, "vfx_kilo_floor_1_creak"));
      s.add(t + 100, mc -> s.shot(mc, "vfx_kilo_floor_2_through"));
      s.add(t + 112, mc -> {
         s.server(mc, p -> log("dirt floor gone {}, user {} blocks under its top", this.dirtGone, (double)(s.origin().getY() + 6 + 1) - p.getY()));
         s.tap(KiloAbilities.PRESS);
      });
      s.add(t + 180, mc -> s.server(mc, p -> {
            BlockPos o = s.origin();
            p.teleportTo(p.serverLevel(), 0.5, (double)o.getY() + 1.6, 11.5, 180.0F, 30.0F);
            p.fallDistance = 0.0F;
         }));
      s.add(
         t + 210,
         mc -> {
            s.server(
               mc,
               p -> {
                  this.raft = PhysicsWorld.structureUnder(p);
                  log(
                     "on the raft: {} (sable {}), mass {}, weight {} kg",
                     this.raft != null,
                     PhysicsWorld.active(),
                     this.raft != null ? this.raft.mass() : 0.0,
                     KiloKilo.weight(p).kg()
                  );
               }
            );
            s.tap(KiloAbilities.PRESS);
         }
      );

      for (int i = t + 212; i <= t + 290; i += 2) {
         s.add(
            i,
            mc -> s.server(
                  mc,
                  p -> {
                     if (!this.raftGone && this.raft != null && PhysicsWorld.structureUnder(p) == null) {
                        this.raftGone = true;
                        log(
                           "off the raft at {} kg: in water {}, raft mass now {} (valid {})",
                           KiloKilo.weight(p).kg(),
                           p.isInWater(),
                           this.raft.mass(),
                           this.raft.isValid()
                        );
                     }
                  }
               )
         );
      }

      s.add(t + 242, mc -> s.shot(mc, "vfx_kilo_floor_3_raft_creak"));
      s.add(t + 275, mc -> s.shot(mc, "vfx_kilo_floor_4_raft_through"));
      s.add(t + 292, mc -> {
         s.server(mc, p -> log("raft: user went through {}, in water {}, y {}", this.raftGone, p.isInWater(), p.getY() - (double)s.origin().getY()));
         s.tap(KiloAbilities.PRESS);
      });
      return t + 300;
   }
}
