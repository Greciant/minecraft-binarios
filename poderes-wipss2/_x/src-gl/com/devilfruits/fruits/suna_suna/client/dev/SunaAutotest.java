package com.devilfruits.fruits.suna_suna.client.dev;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.core.logia.CounterTrigger;
import com.devilfruits.core.logia.DamageCategory;
import com.devilfruits.core.logia.HitRule;
import com.devilfruits.core.logia.LogiaDamage;
import com.devilfruits.core.logia.LogiaDash;
import com.devilfruits.core.logia.LogiaHits;
import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.world.BlockRestoreData;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.fruits.suna_suna.Desiccation;
import com.devilfruits.fruits.suna_suna.SunaAbilities;
import com.devilfruits.fruits.suna_suna.SunaSuna;
import com.devilfruits.fruits.suna_suna.client.vfx.SandBallEffect;
import com.devilfruits.fruits.suna_suna.client.vfx.SandBladeEffect;
import com.devilfruits.fruits.suna_suna.client.vfx.SandCrescentEffect;
import com.devilfruits.fruits.suna_suna.client.vfx.SandGripEffect;
import com.devilfruits.fruits.suna_suna.client.vfx.SandSpearEffect;
import com.devilfruits.fruits.suna_suna.client.vfx.SandWaveEffect;
import com.devilfruits.fruits.suna_suna.client.vfx.SandstormEffect;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SunaAutotest implements AutoTest.Script {
   @Override
   public int write(AutoTest s, int start) {
      s.add(start, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_FRONT);
         s.refresh(mc, SunaSuna.ID);
      });
      s.add(start + 8, mc -> s.server(mc, p -> FruitRules.grantPower(p, SunaSuna.fruit(), true)));
      s.add(start + 16, mc -> s.shot(mc, "suna_00_awaken"));
      s.add(start + 60, mc -> s.shot(mc, "suna_01_sand_body_idle"));
      s.add(start + 64, mc -> s.server(mc, p -> LogiaManager.partial(p, 12, 40)));
      s.add(start + 76, mc -> s.shot(mc, "suna_02_sand_arms"));
      s.add(start + 110, mc -> s.server(mc, SunaAutotest::punch));
      s.add(start + 116, mc -> s.shot(mc, "suna_03_punch_through"));
      s.add(start + 140, mc -> s.server(mc, p -> LogiaManager.setElemental(p, true)));
      s.add(start + 156, mc -> s.shot(mc, "suna_04_all_sand"));
      s.add(start + 162, mc -> s.server(mc, p -> LogiaManager.setElemental(p, false)));
      s.add(start + 168, mc -> s.shot(mc, "suna_05_reforming"));
      s.add(start + 186, mc -> s.camera(mc, CameraType.THIRD_PERSON_BACK));
      s.add(start + 190, mc -> s.server(mc, p -> LogiaDash.dash(p, p.getEyePosition(), new Vec3(0.6, 0.0, -0.8), 7.0)));
      s.add(start + 194, mc -> s.shot(mc, "suna_06_reform_step_flowing"));
      s.add(start + 199, mc -> s.shot(mc, "suna_06b_reform_step_arriving"));
      s.add(start + 200, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
         s.refresh(mc, SunaSuna.ID);
      });
      s.add(
         start + 201,
         mc -> VfxManager.add(new SandBallEffect(Vec3.atBottomCenterOf(s.origin()).add(0.9, 2.1, 2.6), new Vec3(5.0E-4, 0.0, 0.0), 1.2F, 30, 4242L))
      );
      s.add(start + 205, mc -> s.shot(mc, "suna_06c_pesado_ball_model"));
      s.add(start + 206, mc -> s.tap(SunaAbilities.SABLES_PESADO));
      s.add(start + 213, mc -> s.shot(mc, "suna_07_pesado_blast"));
      s.add(start + 250, mc -> s.press(SunaAbilities.SABLES_PESADO));
      s.add(start + 270, mc -> s.shot(mc, "suna_08_pesado_charging"));
      s.add(start + 282, mc -> s.release(SunaAbilities.SABLES_PESADO));
      s.add(start + 288, mc -> s.shot(mc, "suna_09_pesado_ball"));
      s.add(start + 294, mc -> s.shot(mc, "suna_10_pesado_impact"));
      s.add(start + 310, mc -> {
         s.camera(mc, CameraType.FIRST_PERSON);
         s.refresh(mc, SunaSuna.ID);
      });
      s.add(start + 314, mc -> s.press(SunaAbilities.SABLES_PESADO));
      s.add(start + 330, mc -> s.shot(mc, "suna_11_pesado_first_person"));
      s.add(start + 334, mc -> s.release(SunaAbilities.SABLES_PESADO));
      s.add(start + 340, mc -> s.shot(mc, "suna_12_pesado_first_person_throw"));
      s.add(start + 360, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
         s.refresh(mc, SunaSuna.ID);
         s.server(mc, SunaAutotest::garden);
      });
      s.add(start + 372, mc -> s.shot(mc, "suna_13_before_drying"));
      s.add(
         start + 376,
         mc -> s.server(
               mc,
               p -> Desiccation.dry(
                     p.serverLevel(), p, Vec3.atBottomCenterOf(s.origin().offset(0, 0, 1)), 4.5, Desiccation.Options.SURFACE, new CastBudget(400, 0)
                  )
            )
      );
      s.add(start + 386, mc -> s.shot(mc, "suna_14_dried_land"));
      s.add(start + 392, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
         s.refresh(mc, SunaSuna.ID);
      });
      s.add(start + 396, mc -> s.server(mc, p -> {
            BlockPos o = s.origin();
            p.teleportTo(p.serverLevel(), 0.5, (double)o.getY(), -2.5, 180.0F, 12.0F);

            for (int i = 0; i < 5; i++) {
               Pig pig = (Pig)EntityType.PIG.create(p.serverLevel());
               if (pig != null) {
                  double a = (double)i * 1.26;
                  pig.moveTo(0.5 + Math.cos(a) * 5.5, (double)o.getY(), -10.0 + Math.sin(a) * 5.5, (float)(i * 70), 0.0F);
                  pig.setPersistenceRequired();
                  p.serverLevel().addFreshEntity(pig);
               }
            }
         }));
      s.add(start + 402, mc -> s.press(SunaAbilities.DESERT_GIRASOLE));
      s.add(start + 412, mc -> s.shot(mc, "suna_15_girasole_opening"));
      s.add(start + 440, mc -> s.shot(mc, "suna_16_girasole_whirl"));
      s.add(start + 444, mc -> s.camera(mc, CameraType.THIRD_PERSON_FRONT));
      s.add(start + 452, mc -> s.shot(mc, "suna_16b_girasole_front"));
      s.add(start + 456, mc -> s.camera(mc, CameraType.THIRD_PERSON_BACK));
      s.add(start + 490, mc -> s.shot(mc, "suna_17_girasole_swallowing"));
      s.add(start + 494, mc -> s.camera(mc, CameraType.FIRST_PERSON));
      s.add(start + 504, mc -> s.shot(mc, "suna_18_girasole_first_person"));
      s.add(start + 508, mc -> s.release(SunaAbilities.DESERT_GIRASOLE));
      s.add(start + 512, mc -> s.camera(mc, CameraType.THIRD_PERSON_BACK));
      s.add(start + 520, mc -> s.shot(mc, "suna_19_girasole_closed"));
      int c = start + 530;
      s.add(c, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
         s.refresh(mc, SunaSuna.ID);
      });
      s.add(c + 4, mc -> s.server(mc, p -> {
            north(s, p);
            targets(s, p, -9, true);
            walls(s, p, true);
         }));
      s.add(c + 6, mc -> {
         Vec3 o = Vec3.atBottomCenterOf(s.origin());
         VfxManager.add(new SandBladeEffect(o.add(-1.6, 0.0, -6.5), new Vec3(0.004, 0.0, 0.0), 2.0F, 6, 501L));
         VfxManager.add(new SandCrescentEffect(o.add(1.4, 0.7, -6.0), new Vec3(0.0, 0.0, -0.004), 6.0F, 6, 502L));
         VfxManager.add(new SandSpearEffect(o.add(2.4, 1.9, -5.0), new Vec3(0.004, 0.0, 0.0), 6, 503L));
      });
      s.add(c + 9, mc -> s.shot(mc, "suna_20b_models_blade_crescent_spear"));
      s.add(c + 10, mc -> s.server(mc, p -> p.teleportTo(p.serverLevel(), p.getX(), p.getY(), p.getZ(), 180.0F, 24.0F)));
      s.add(c + 12, mc -> s.press(SunaAbilities.DESERT_SPADA));
      s.add(c + 15, mc -> s.shot(mc, "suna_21_spada_running"));
      s.add(c + 18, mc -> s.shot(mc, "suna_22_spada_cut"));
      s.add(c + 28, mc -> s.shot(mc, "suna_22b_spada_tunnel"));
      s.add(c + 32, mc -> s.server(mc, p -> {
            walls(s, p, false);
            north(s, p);
         }));
      s.add(c + 40, mc -> s.press(SunaAbilities.BARJAN));
      s.add(c + 43, mc -> s.shot(mc, "suna_23_barjan"));
      s.add(c + 43, mc -> s.server(mc, p -> p.teleportTo(p.serverLevel(), p.getX(), p.getY(), p.getZ(), 180.0F, 38.0F)));
      s.add(c + 45, mc -> s.shot(mc, "suna_24_barjan_above"));
      s.add(c + 48, mc -> s.shot(mc, "suna_24b_barjan_far"));
      s.add(c + 52, mc -> s.server(mc, p -> north(s, p)));
      s.add(c + 70, mc -> s.press(SunaAbilities.DESERT_LA_SPADA));
      s.add(c + 78, mc -> s.shot(mc, "suna_25_la_spada"));
      s.add(c + 84, mc -> s.shot(mc, "suna_26_la_spada_volley"));
      s.add(c + 100, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
         s.refresh(mc, SunaSuna.ID);
         s.server(mc, p -> north(s, p));
      });
      s.add(c + 106, mc -> s.press(SunaAbilities.GROUND_SECCO));
      s.add(c + 140, mc -> s.shot(mc, "suna_27_ground_secco"));
      s.add(c + 142, mc -> s.camera(mc, CameraType.THIRD_PERSON_FRONT));
      s.add(c + 147, mc -> s.shot(mc, "suna_27b_ground_secco_front"));
      s.add(c + 150, mc -> s.camera(mc, CameraType.FIRST_PERSON));
      s.add(c + 162, mc -> s.shot(mc, "suna_28_ground_secco_first_person"));
      s.add(c + 166, mc -> {
         s.release(SunaAbilities.GROUND_SECCO);
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
      });
      s.add(c + 180, mc -> s.server(mc, p -> {
            p.teleportTo(p.serverLevel(), 4.5, (double)s.origin().getY(), -2.5, 180.0F, 14.0F);
            Husk husk = (Husk)EntityType.HUSK.create(p.serverLevel());
            if (husk != null) {
               husk.moveTo(4.5, (double)s.origin().getY(), -4.6, 0.0F, 0.0F);
               husk.setPersistenceRequired();
               p.serverLevel().addFreshEntity(husk);
            }
         }));
      s.add(c + 186, mc -> s.press(SunaAbilities.DESERT_ENCIERRO));
      s.add(c + 206, mc -> s.shot(mc, "suna_29_encierro"));
      s.add(c + 214, mc -> s.release(SunaAbilities.DESERT_ENCIERRO));
      s.add(c + 230, mc -> {
         s.refresh(mc, SunaSuna.ID);
         s.server(mc, p -> {
            north(s, p);
            targets(s, p, -12, true);
         });
      });
      s.add(c + 236, mc -> s.press(SunaAbilities.SAND_TSUNAMI));
      s.add(c + 262, mc -> s.shot(mc, "suna_30_tsunami_charging"));
      s.add(c + 278, mc -> s.release(SunaAbilities.SAND_TSUNAMI));
      s.add(c + 286, mc -> s.shot(mc, "suna_31_tsunami_rolling"));
      s.add(c + 296, mc -> s.shot(mc, "suna_32_tsunami_far"));
      s.add(c + 330, mc -> {
         s.refresh(mc, SunaSuna.ID);
         s.server(mc, p -> {
            north(s, p);
            targets(s, p, -12, false);
         });
      });
      s.add(c + 336, mc -> s.press(SunaAbilities.SABLES));
      s.add(c + 360, mc -> s.shot(mc, "suna_33_sables_rising"));
      s.add(c + 400, mc -> s.shot(mc, "suna_34_sables_raging"));
      s.add(c + 450, mc -> s.shot(mc, "suna_35_sables_wandering"));
      int g = c + 520;
      s.add(g, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
         s.server(mc, p -> p.teleportTo(p.serverLevel(), 60.5, (double)s.origin().getY(), 0.5, 180.0F, 10.0F));
      });
      s.add(g + 6, mc -> {
         Vec3 o = new Vec3(60.5, (double)s.origin().getY(), 0.5);
         VfxManager.add(new SandBladeEffect(o.add(-2.4, 0.0, -4.2), new Vec3(0.004, 0.0, 0.0), 2.0F, 40, 601L));
         VfxManager.add(new SandCrescentEffect(o.add(1.0, 0.6, -6.0), new Vec3(-0.002, 0.0, -0.004), 7.0F, 40, 602L));
         VfxManager.add(new SandSpearEffect(o.add(1.9, 1.3, -2.6), new Vec3(0.004, 0.001, -0.001), 40, 603L));
      });
      s.add(g + 12, mc -> s.shot(mc, "suna_36_gallery_fin_crescent_blade"));
      s.add(g + 13, mc -> s.server(mc, p -> p.teleportTo(p.serverLevel(), 60.5, (double)s.origin().getY(), 0.5, 180.0F, 45.0F)));
      s.add(g + 15, mc -> s.shot(mc, "suna_36b_gallery_from_above"));
      s.add(g + 16, mc -> s.server(mc, p -> p.teleportTo(p.serverLevel(), 60.5, (double)s.origin().getY(), 0.5, 180.0F, 10.0F)));
      s.add(g + 16, mc -> {
         Vec3 o = new Vec3(60.5, (double)s.origin().getY(), 0.5);
         VfxManager.add(new SandWaveEffect(mc.level, o.add(0.0, 0.0, -22.0), new Vec3(0.0, 0.0, 0.6), 13.0F, 970, 604L));
      });
      s.add(g + 38, mc -> s.shot(mc, "suna_37_gallery_wave_front"));
      s.add(g + 60, mc -> {
         Vec3 o = new Vec3(60.5, (double)s.origin().getY(), 0.5);
         VfxManager.add(new SandstormEffect(o.add(0.0, 0.0, -13.0), new Vec3(0.0, 0.0, 1.0E-4), 5.0F, 200, 605L));
      });
      s.add(g + 110, mc -> s.shot(mc, "suna_38_gallery_storm"));
      s.add(g + 108, mc -> DevilFruits.LOGGER.info("[devilfruits autotest] fps with 1 sandstorm: {}", mc.getFps()));
      s.add(g + 113, mc -> {
         Vec3 o = new Vec3(60.5, (double)s.origin().getY(), 0.5);

         for (int i = 0; i < 4; i++) {
            VfxManager.add(new SandstormEffect(o.add((double)(-12 + i * 8), 0.0, -20.0), new Vec3(0.0, 0.0, 1.0E-4), 5.0F, 40, 700L + (long)i));
         }

         for (int i = 0; i < 3; i++) {
            VfxManager.add(new SandWaveEffect(mc.level, o.add((double)(-10 + i * 10), 0.0, -30.0), new Vec3(0.0, 0.0, 0.6), 13.0F, 40, 710L + (long)i));
         }
      });
      s.add(g + 130, mc -> DevilFruits.LOGGER.info("[devilfruits autotest] fps with 5 sandstorms and 3 sand waves: {}", mc.getFps()));
      s.add(g + 112, mc -> {
         SandstormEffect storm = VfxManager.find(SandstormEffect.class, e -> e.seed == 605L);
         if (storm != null) {
            storm.finish();
         }
      });
      s.add(g + 114, mc -> s.server(mc, p -> {
            Husk husk = (Husk)EntityType.HUSK.create(p.serverLevel());
            if (husk != null) {
               husk.moveTo(60.5, (double)s.origin().getY() + 0.4, -3.0, 0.0F, 0.0F);
               husk.setNoAi(true);
               husk.setPersistenceRequired();
               p.serverLevel().addFreshEntity(husk);
            }
         }));
      s.add(
         g + 120,
         mc -> {
            Husk husk = (Husk)mc.level
               .getEntitiesOfClass(Husk.class, new AABB(58.0, (double)(s.origin().getY() - 2), -6.0, 63.0, (double)(s.origin().getY() + 3), 0.0))
               .stream()
               .findFirst()
               .orElse(null);
            if (husk != null && mc.player != null) {
               VfxManager.add(new SandGripEffect(mc.player, husk.getId(), 100, 606L));
            }
         }
      );
      s.add(g + 134, mc -> s.shot(mc, "suna_39_gallery_prison"));
      s.add(g + 150, mc -> s.server(mc, p -> LogiaManager.trigger(p, CounterTrigger.Kind.WATER_BUCKET, null)));
      s.add(g + 157, mc -> s.tap(SunaAbilities.SABLES_PESADO));
      s.add(g + 162, mc -> s.shot(mc, "suna_40_wet"));
      int w = g + 270;
      s.add(w, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
         s.refresh(mc, SunaSuna.ID);
         s.server(mc, p -> {
            BlockPos o = s.origin();

            for (int x = -4; x <= 4; x++) {
               for (int z = -12; z <= 2; z++) {
                  p.serverLevel().setBlockAndUpdate(new BlockPos(60 + x, o.getY() - 1, z), Blocks.SAND.defaultBlockState());
               }
            }

            p.teleportTo(p.serverLevel(), 60.5, (double)o.getY(), 0.5, 180.0F, 15.0F);
         });
      });
      s.add(w + 4, mc -> s.press(SunaAbilities.SAND_CLOUD));
      s.add(w + 14, mc -> s.shot(mc, "suna_41_sand_cloud"));
      s.add(w + 16, mc -> s.press(SunaAbilities.SAND_CLOUD));
      return w + 30;
   }

   private static void north(AutoTest s, ServerPlayer p) {
      BlockPos o = s.origin();
      p.teleportTo(p.serverLevel(), 0.5, (double)o.getY(), -2.5, 180.0F, 8.0F);
   }

   private static void walls(AutoTest s, ServerPlayer p, boolean build) {
      BlockPos o = s.origin();
      ServerLevel level = p.serverLevel();
      if (!build) {
         BlockRestoreData.get(level)
            .restoreInside(
               level,
               new AABB(
                  (double)(o.getX() - 4), (double)(o.getY() - 2), (double)(o.getZ() - 28), (double)(o.getX() + 5), (double)(o.getY() + 4), (double)o.getZ()
               )
            );
      }

      for (int x = -2; x <= 2; x++) {
         for (int y = 0; y < 3; y++) {
            level.setBlockAndUpdate(o.offset(x, y, -15), build ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(o.offset(x, y, -19), build ? Blocks.OAK_LOG.defaultBlockState() : Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(o.offset(x, y, -25), build ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState());
         }
      }
   }

   private static void targets(AutoTest s, ServerPlayer p, int z, boolean husks) {
      for (int i = -2; i <= 2; i++) {
         Mob mob = husks ? (Mob)EntityType.HUSK.create(p.serverLevel()) : (Mob)EntityType.PIG.create(p.serverLevel());
         if (mob != null) {
            mob.moveTo((double)i * 1.8 + 0.5, (double)s.origin().getY(), (double)z + 0.5, 0.0F, 0.0F);
            mob.setNoAi(husks);
            mob.setPersistenceRequired();
            p.serverLevel().addFreshEntity(mob);
         }
      }
   }

   private static void punch(ServerPlayer p) {
      LogiaDamage.Decision pass = new LogiaDamage.Decision(HitRule.PASS, DamageCategory.MELEE, 0.0F);
      Vec3 feet = p.position();
      Vec3 front = Vec3.directionFromRotation(0.0F, p.yBodyRot);
      LogiaHits.add(p, feet.add(front.scale(2.0)).add(0.0, 1.2, 0.0), front.scale(-1.0), pass, -1);
      LogiaHits.add(p, feet.add(front.scale(2.0)).add(0.35, 1.6, 0.0), front.scale(-1.0), pass, -1);
      LogiaHits.add(p, feet.add(front.scale(2.0)).add(-0.4, 0.5, 0.0), front.scale(-1.0), pass, -1);
   }

   private static void garden(ServerPlayer p) {
      ServerLevel level = p.serverLevel();
      BlockPos o = level.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.ZERO);
      int y = o.getY();

      for (int x = -4; x <= 4; x++) {
         for (int z = 1; z <= 2; z++) {
            if ((x + z) % 3 == 0) {
               level.setBlockAndUpdate(new BlockPos(x, y - 1, z), Blocks.FARMLAND.defaultBlockState());
               level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.WHEAT.defaultBlockState());
            } else if ((x + z) % 3 == 1) {
               level.setBlockAndUpdate(new BlockPos(x, y, z), (x % 2 == 0 ? Blocks.POPPY : Blocks.DANDELION).defaultBlockState());
            } else {
               level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.SHORT_GRASS.defaultBlockState());
            }
         }
      }
   }
}
