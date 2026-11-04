package com.devilfruits.fruits.bomu_bomu.client.dev;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureRig;
import com.devilfruits.core.fx.ExplosionProfile;
import com.devilfruits.core.physics.PhysicsStructure;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.fruits.bomu_bomu.BomuAbilities;
import com.devilfruits.fruits.bomu_bomu.BomuBomu;
import com.devilfruits.fruits.bomu_bomu.BomuItems;
import com.devilfruits.fruits.bomu_bomu.ability.ExplosivePunchAbility;
import com.devilfruits.fruits.bomu_bomu.ability.SelfDestructAbility;
import com.devilfruits.fruits.bomu_bomu.explosion.BombExplosionService;
import com.devilfruits.fruits.bomu_bomu.explosion.ExplosionSpec;
import com.devilfruits.fruits.bomu_bomu.item.BombPistolItem;
import com.devilfruits.registry.DFAttachments;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class BomuCapture implements AutoTest.Script {
   private final boolean wide;
   private final boolean pistolOnly;

   public BomuCapture(boolean wide) {
      this(wide, false);
   }

   public BomuCapture(boolean wide, boolean pistolOnly) {
      this.wide = wide;
      this.pistolOnly = pistolOnly;
   }

   private static void drill(AutoTest s, int at, String pre, String name) {
      s.add(at, mc -> {
         KeyMapping.click(mc.options.keyUse.getKey());
         mc.options.keyUse.setDown(true);
      });
      s.add(at + 10, mc -> s.shot(mc, pre + name + "_spin"));
      s.add(at + 26, mc -> s.shot(mc, pre + name + "_blow"));
      s.add(
         at + 44,
         mc -> {
            mc.options.keyUse.setDown(false);
            s.server(
               mc,
               p -> log("drill {}: {} rounds, bomb breath {}", name, BombPistolItem.rounds(p.getMainHandItem()), BombPistolItem.bombBreath(p.getMainHandItem()))
            );
         }
      );
   }

   private static void log(String text, Object... args) {
      DevilFruits.LOGGER.info("[devilfruits autotest] bomu " + text, args);
   }

   private static void command(ServerPlayer p, String command) {
      p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withSuppressedOutput().withPermission(4), command);
   }

   private static void refill(ServerPlayer p) {
      DFAttachments.data(p).clearCooldowns();
      DFAttachments.data(p).setEnergy(10000.0F);
   }

   @Override
   public int write(AutoTest s, int start) {
      String pre = this.wide ? "vfx_bomu_" : "bomu_";
      s.add(start, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_FRONT);
         s.refresh(mc, BomuBomu.ID);
      });
      s.add(
         start + 4,
         mc -> s.server(
               mc,
               p -> {
                  BlockPos o = s.origin();
                  ServerLevel level = p.serverLevel();
                  command(p, "time set 6000");
                  command(p, "weather clear");
                  command(p, "gamerule mobGriefing true");

                  for (int x = 10; x <= 22; x++) {
                     for (int z = -5; z <= 5; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, o.getY() - 1, z), Blocks.STONE.defaultBlockState());

                        for (int y = 0; y < 5; y++) {
                           level.setBlockAndUpdate(
                              new BlockPos(x, o.getY() + y, z),
                              x == 20 && Math.abs(z) <= 2 && y < 4 ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.AIR.defaultBlockState()
                           );
                        }
                     }
                  }

                  p.teleportTo(level, 12.5, (double)o.getY(), 0.5, -90.0F, 6.0F);
                  Husk z = (Husk)EntityType.HUSK.create(level);
                  if (z != null) {
                     z.moveTo(15.5, (double)o.getY(), 0.5, 90.0F, 0.0F);
                     z.setNoAi(true);
                     z.setPersistenceRequired();
                     level.addFreshEntity(z);
                  }
               }
            )
      );
      s.add(start + 10, mc -> s.server(mc, p -> FruitRules.grantPower(p, BomuBomu.fruit(), true)));
      s.add(start + 18, mc -> s.shot(mc, pre + "00_awaken"));
      if (!this.pistolOnly) {
         s.add(start + 30, mc -> {
            s.server(mc, BomuCapture::refill);
            s.tap(BomuAbilities.PUNCH);
         });
         s.add(start + 40, mc -> s.shot(mc, pre + "01_punch_armed_front"));
         s.add(start + 42, mc -> s.camera(mc, CameraType.FIRST_PERSON));
         s.add(start + 48, mc -> s.shot(mc, pre + "02_punch_armed_fp"));
         s.add(start + 50, mc -> s.camera(mc, CameraType.THIRD_PERSON_BACK));
         s.add(
            start + 56,
            mc -> s.server(
                  mc,
                  p -> {
                     Husk z = (Husk)p.serverLevel()
                        .getNearestEntity(Husk.class, TargetingConditions.forNonCombat(), p, p.getX(), p.getY(), p.getZ(), p.getBoundingBox().inflate(8.0));
                     if (z != null) {
                        float before = z.getHealth();
                        p.attack(z);
                        log("punch: husk health {} -> {}, still armed {}", before, z.getHealth(), ExplosivePunchAbility.armed(p));
                     }
                  }
               )
         );
         s.add(start + 57, mc -> s.shot(mc, pre + "03_punch_blast_1"));
         s.add(start + 59, mc -> s.shot(mc, pre + "04_punch_blast_2"));
         s.add(start + 64, mc -> s.shot(mc, pre + "05_punch_blast_3"));
         s.add(start + 82, mc -> {
            s.shot(mc, pre + "06_punch_smoke");
            s.server(mc, p -> p.serverLevel().getEntitiesOfClass(Husk.class, p.getBoundingBox().inflate(16.0)).forEach(Entity::discard));
         });
         s.add(start + 92, mc -> {
            s.server(mc, p -> {
               refill(p);
               p.teleportTo(p.serverLevel(), 13.5, (double)s.origin().getY(), 0.5, -90.0F, 4.0F);
            });
            s.tap(BomuAbilities.CANNON);
         });
         s.add(start + 96, mc -> s.shot(mc, pre + "07_booger_flying"));
         s.add(start + 101, mc -> s.shot(mc, pre + "08_booger_stuck"));
         s.add(start + 106, mc -> s.shot(mc, pre + "09_booger_blasts"));
         s.add(start + 124, mc -> s.shot(mc, pre + "10_booger_after"));
      } else {
         s.add(start + 12, mc -> s.server(mc, p -> p.serverLevel().getEntitiesOfClass(Husk.class, p.getBoundingBox().inflate(16.0)).forEach(Entity::discard)));
      }

      int ps = this.pistolOnly ? start + 24 : start + 132;
      s.add(ps, mc -> s.server(mc, p -> {
            refill(p);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)BomuItems.BOMB_PISTOL.get()));
            p.teleportTo(p.serverLevel(), 13.5, (double)s.origin().getY(), 0.5, -90.0F, 4.0F);
         }));
      s.add(ps + 4, mc -> s.camera(mc, CameraType.THIRD_PERSON_FRONT));
      drill(s, ps + 8, pre, "11_drill_front");
      s.add(ps + 54, mc -> s.camera(mc, CameraType.FIRST_PERSON));
      s.add(ps + 56, mc -> s.server(mc, p -> BombPistolItem.setRounds(p.getMainHandItem(), 0)));
      drill(s, ps + 60, pre, "13_drill_fp");
      s.add(ps + 103, mc -> mc.options.hideGui = false);
      s.add(ps + 105, mc -> {
         s.shot(mc, pre + "10_pistol_hotbar");
         mc.options.hideGui = true;
      });
      s.add(ps + 106, mc -> {
         s.shot(mc, pre + "15_pistol_loaded_fp");
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
         s.server(mc, p -> {
            refill(p);
            BlockPos o = s.origin();

            for (int z = -2; z <= 2; z++) {
               for (int y = 0; y < 4; y++) {
                  p.serverLevel().setBlockAndUpdate(new BlockPos(20, o.getY() + y, z), Blocks.STONE_BRICKS.defaultBlockState());
               }
            }
         });
      });
      s.add(ps + 108, mc -> s.server(mc, p -> {
            BlockPos o = s.origin();

            for (int y = 0; y < 4; y++) {
               for (int x = 12; x <= 14; x++) {
                  p.serverLevel().setBlockAndUpdate(new BlockPos(x, o.getY() + y, -30), Blocks.STONE_BRICKS.defaultBlockState());
               }
            }

            p.teleportTo(p.serverLevel(), 13.5, (double)o.getY(), -4.5, 180.0F, 1.0F);
         }));
      s.add(ps + 110, mc -> {
         mc.player.setYBodyRot(mc.player.getYRot());
         mc.player.yBodyRotO = mc.player.getYRot();
         CaptureRig.follow(mc.player, 11.0, 90.0F, 10.0F, 1.2);
      });
      s.add(ps + 112, mc -> s.tap(BomuAbilities.SHOT));
      s.add(ps + 113, mc -> s.shot(mc, pre + "16_shot_air_1"));
      s.add(ps + 114, mc -> s.shot(mc, pre + "17_shot_air_2"));
      s.add(ps + 115, mc -> s.shot(mc, pre + "17_shot_air_3"));
      s.add(ps + 118, mc -> s.shot(mc, pre + "18_shot_blast"));
      s.add(ps + 124, mc -> s.shot(mc, pre + "19_shot_fireball"));
      s.add(ps + 145, mc -> {
         s.shot(mc, pre + "19_shot_smoke");
         CaptureRig.off();
      });
      if (this.pistolOnly) {
         return ps + 150;
      } else {
         ExplosionProfile[] profiles = ExplosionProfile.values();
         float[] power = new float[]{2.2F, 4.0F, 8.0F, 12.0F};
         double[] at = new double[]{24.0, 28.0, 40.0, 58.0};
         int[] beats = new int[]{2, 6, 14, 40};
         int p0 = start + 290;

         for (int i = 0; i < profiles.length; i++) {
            int base = p0 + i * 60;
            int k = i;
            s.add(base, mc -> s.server(mc, p -> {
                  refill(p);
                  p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                  p.teleportTo(p.serverLevel(), 12.5, (double)s.origin().getY(), 0.5, -90.0F, k == 3 ? -12.0F : -2.0F);
                  BombExplosionService.forget(p.getUUID());
                  ExplosionSpec spec = new ExplosionSpec(power[k], power[k], 0.0F, 0.0F, 0.0F, 0.0F, profiles[k], BomuAbilities.SELF_DESTRUCT.id());
                  BombExplosionService.detonate(p.serverLevel(), new Vec3(at[k], (double)s.origin().getY() + 0.5, 0.5), spec, p);
               }));

            for (int b : beats) {
               s.add(base + b, mc -> s.shot(mc, pre + "20_profile_" + profiles[k].name().toLowerCase() + "_" + String.format("%02d", b)));
            }
         }

         int tn = p0 + profiles.length * 60 + 10;
         s.add(tn, mc -> {
            s.camera(mc, CameraType.THIRD_PERSON_FRONT);
            s.server(mc, p -> {
               PrimedTnt tnt = new PrimedTnt(p.serverLevel(), p.getX() + 1.5, p.getY(), p.getZ(), null);
               tnt.setFuse(12);
               p.serverLevel().addFreshEntity(tnt);
               log("tnt: health {} before", p.getHealth());
            });
         });
         s.add(tn + 13, mc -> s.shot(mc, pre + "21_tnt_blast"));
         s.add(tn + 18, mc -> {
            s.shot(mc, pre + "22_tnt_absorbed");
            s.server(mc, p -> log("tnt: health {} after, velocity {}", p.getHealth(), p.getDeltaMovement()));
         });
         int sd = tn + 40;
         s.add(sd, mc -> {
            s.server(mc, p -> {
               refill(p);
               p.teleportTo(p.serverLevel(), 14.5, (double)s.origin().getY(), 0.5, -90.0F, 0.0F);
            });
            s.camera(mc, CameraType.THIRD_PERSON_FRONT);
         });
         s.add(sd + 4, mc -> s.press(BomuAbilities.SELF_DESTRUCT));
         s.add(sd + 18, mc -> s.shot(mc, pre + "23_sd_charge_1"));
         s.add(sd + 36, mc -> s.shot(mc, pre + "24_sd_charge_2"));
         s.add(sd + 54, mc -> s.shot(mc, pre + "25_sd_charge_full"));
         s.add(sd + 56, mc -> s.release(BomuAbilities.SELF_DESTRUCT));
         s.add(sd + 58, mc -> s.shot(mc, pre + "26_sd_blast_1"));
         s.add(sd + 61, mc -> s.shot(mc, pre + "27_sd_blast_2"));
         s.add(sd + 66, mc -> s.shot(mc, pre + "28_sd_vanished"));
         s.add(sd + 90, mc -> s.shot(mc, pre + "29_sd_reformed"));
         s.add(sd + 92, mc -> s.camera(mc, CameraType.THIRD_PERSON_BACK));
         s.add(sd + 130, mc -> {
            s.shot(mc, pre + "30_sd_crater");
            s.server(mc, p -> log("self-destruct: health {}, vanished {}", p.getHealth(), SelfDestructAbility.vanished(p)));
         });
         int sb = sd + 140;
         s.add(sb, mc -> s.server(mc, p -> {
               if (!PhysicsWorld.active()) {
                  log("sable not installed: raft skipped");
               } else {
                  BlockPos o = s.origin();
                  ServerLevel level = p.serverLevel();

                  for (int x = -2; x <= 2; x++) {
                     for (int z = 9; z <= 13; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, o.getY() + 2, z), Blocks.OAK_PLANKS.defaultBlockState());
                     }
                  }

                  command(p, String.format("sable assemble area %d %d %d %d %d %d", -2, o.getY() + 2, 9, 2, o.getY() + 2, 13));
                  p.getAbilities().flying = true;
                  p.onUpdateAbilities();
                  p.teleportTo(level, 0.5, (double)(o.getY() + 5), 2.5, 0.0F, 38.0F);
               }
            }));
         s.add(sb + 30, mc -> {
            s.camera(mc, CameraType.FIRST_PERSON);
            s.server(mc, p -> {
               PhysicsStructure raft = PhysicsWorld.active() ? nearest(p) : null;
               log("raft before: {} mass {}", raft != null, raft != null ? raft.mass() : 0.0);
            });
         });
         s.add(sb + 34, mc -> s.shot(mc, pre + "31_raft_before"));
         s.add(sb + 36, mc -> s.server(mc, p -> {
               refill(p);
               BombExplosionService.forget(p.getUUID());
               command(p, "devilfruit sable test blast 4");
            }));
         s.add(sb + 39, mc -> s.shot(mc, pre + "32_raft_blast"));
         s.add(sb + 70, mc -> {
            s.shot(mc, pre + "33_raft_after");
            s.server(mc, p -> {
               PhysicsStructure raft = PhysicsWorld.active() ? nearest(p) : null;
               log("raft after: {} mass {}", raft != null, raft != null ? raft.mass() : 0.0);
            });
         });
         return sb + 80;
      }
   }

   private static PhysicsStructure nearest(ServerPlayer p) {
      List<PhysicsStructure> list = PhysicsWorld.bridge().structuresIn(p.level(), p.getBoundingBox().inflate(32.0));
      return list.isEmpty() ? null : list.iterator().next();
   }
}
