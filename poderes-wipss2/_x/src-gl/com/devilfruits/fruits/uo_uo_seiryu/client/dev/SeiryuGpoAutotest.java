package com.devilfruits.fruits.uo_uo_seiryu.client.dev;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuAbilities;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuClientConfig;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import net.minecraft.client.CameraType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;

public final class SeiryuGpoAutotest implements AutoTest.Script {
   private static final double USER_Z = -2.5;
   private static final double DUMMY_Z = -9.5;

   static void cast(ServerPlayer p, String ability, int ticks) {
      p.getServer()
         .getCommands()
         .performPrefixedCommand(p.createCommandSourceStack().withPermission(4), "devilfruit cast devilfruits:uo_uo_seiryu/" + ability + " " + ticks);
   }

   private static void stage(AutoTest s, ServerPlayer p, String ability, int ticks, boolean dummy) {
      double y = (double)s.origin().getY();
      p.teleportTo(p.serverLevel(), 0.5, y, -2.5, 180.0F, 30.0F);
      if (dummy) {
         p.serverLevel().getEntitiesOfClass(Husk.class, p.getBoundingBox().inflate(40.0)).forEach(h -> h.discard());
         Husk husk = (Husk)EntityType.HUSK.create(p.serverLevel());
         if (husk != null) {
            husk.moveTo(0.5, y, -9.5, 0.0F, 0.0F);
            husk.setNoAi(true);
            husk.setPersistenceRequired();
            husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500.0);
            husk.setHealth(500.0F);
            p.serverLevel().addFreshEntity(husk);
         }
      }

      cast(p, ability, ticks);
   }

   @Override
   public int write(AutoTest s, int start) {
      s.add(start, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_FRONT);
         s.refresh(mc, UoUoSeiryu.ID);
         SeiryuClientConfig.HIT_COUNTER.set(true);
      });
      s.add(start + 2, mc -> s.server(mc, p -> FormManager.revert(p, FormManager.Reason.COMMAND)));
      s.add(start + 8, mc -> s.tap(SeiryuAbilities.TRANSFORM_HYBRID));
      s.add(start + 22, mc -> s.shot(mc, "gpo_tf_00_windup"));
      s.add(start + 40, mc -> s.shot(mc, "gpo_tf_01_explosion"));
      s.add(start + 46, mc -> s.shot(mc, "gpo_tf_02_rings"));
      s.add(start + 64, mc -> s.shot(mc, "gpo_tf_03_afterglow"));
      int t = start + 70;
      s.add(t + 2, mc -> s.server(mc, p -> FormManager.request(p, "hybrid", FormManager.Reason.COMMAND)));
      s.add(t + 30, mc -> s.shot(mc, "gpo_00_hybrid_front"));
      s.add(t + 32, mc -> s.camera(mc, CameraType.THIRD_PERSON_BACK));
      s.add(t + 40, mc -> s.shot(mc, "gpo_01_hybrid_back"));
      t += 50;
      t = rush(s, t, "plain");
      s.add(t, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_FRONT);
         s.refresh(mc, UoUoSeiryu.ID);
      });
      s.add(t + 2, mc -> s.server(mc, p -> cast(p, "thunderclad_veil", 0)));
      s.add(t + 7, mc -> s.shot(mc, "gpo_veil_00_gather"));
      s.add(t + 16, mc -> s.shot(mc, "gpo_veil_01_cocoon"));
      s.add(t + 24, mc -> s.shot(mc, "gpo_veil_02_cocoon_late"));
      s.add(t + 29, mc -> s.shot(mc, "gpo_veil_03_burst"));
      s.add(t + 46, mc -> s.shot(mc, "gpo_veil_04_aura_front"));
      s.add(t + 48, mc -> s.camera(mc, CameraType.THIRD_PERSON_BACK));
      s.add(t + 56, mc -> s.shot(mc, "gpo_veil_05_aura_back"));
      t += 60;
      t = rush(s, t, "veiled");
      s.add(t, mc -> s.server(mc, p -> stage(s, p, "draconic_thunderfall", 50, true)));
      s.add(t + 12, mc -> s.shot(mc, "gpo_fall_00_first"));
      s.add(t + 27, mc -> s.shot(mc, "gpo_fall_01_storm"));
      s.add(t + 44, mc -> s.shot(mc, "gpo_fall_02_storm_late"));
      s.add(t + 57, mc -> s.shot(mc, "gpo_fall_03_release"));
      s.add(t + 64, mc -> s.shot(mc, "gpo_fall_04_volley"));
      s.add(t + 90, mc -> s.shot(mc, "gpo_fall_05_debris"));
      t += 100;
      s.add(t, mc -> s.server(mc, p -> stage(s, p, "hellfire_ray", 45, true)));
      s.add(t + 4, mc -> s.shot(mc, "gpo_ray_00_start"));
      s.add(t + 14, mc -> s.shot(mc, "gpo_ray_01_spin"));
      s.add(t + 28, mc -> s.shot(mc, "gpo_ray_02_caught"));
      s.add(t + 42, mc -> s.shot(mc, "gpo_ray_03_late"));
      s.add(t + 49, mc -> s.shot(mc, "gpo_ray_04_explosion"));
      s.add(t + 60, mc -> s.shot(mc, "gpo_ray_05_landing"));
      t += 70;
      s.add(t, mc -> s.server(mc, p -> stage(s, p, "draconic_ascension", 0, true)));
      s.add(t + 6, mc -> s.shot(mc, "gpo_dive_00_windup"));
      s.add(t + 12, mc -> s.shot(mc, "gpo_dive_01_launch"));
      s.add(t + 16, mc -> s.shot(mc, "gpo_dive_02_arc"));
      s.add(t + 20, mc -> s.shot(mc, "gpo_dive_03_impact"));
      s.add(t + 25, mc -> s.shot(mc, "gpo_dive_04_pillar"));
      s.add(t + 40, mc -> s.shot(mc, "gpo_dive_05_after"));
      t += 50;
      s.add(t, mc -> s.server(mc, p -> stage(s, p, "draconic_cataclysm", 0, true)));
      s.add(t + 6, mc -> s.shot(mc, "gpo_cata_00_windup"));
      s.add(t + 20, mc -> s.shot(mc, "gpo_cata_01_leap"));
      s.add(t + 36, mc -> s.shot(mc, "gpo_cata_02_smash"));
      s.add(t + 44, mc -> s.shot(mc, "gpo_cata_03_after"));
      t += 60;
      s.add(t, mc -> s.server(mc, p -> stage(s, p, "draconic_tail_slam", 0, true)));
      s.add(t + 6, mc -> s.shot(mc, "gpo_tail_00_sweep"));
      s.add(t + 10, mc -> s.shot(mc, "gpo_tail_01_late"));
      t += 30;
      s.add(t, mc -> s.server(mc, p -> stage(s, p, "thunder_bagua", 0, true)));
      s.add(t + 5, mc -> s.shot(mc, "gpo_bagua_00_swing"));
      s.add(t + 10, mc -> s.shot(mc, "gpo_bagua_01_impact"));
      t += 30;
      s.add(t, mc -> s.server(mc, p -> stage(s, p, "blast_breath", 30, true)));
      s.add(t + 20, mc -> s.shot(mc, "gpo_breath_00_charge"));
      s.add(t + 34, mc -> s.camera(mc, CameraType.THIRD_PERSON_FRONT));
      s.add(t + 40, mc -> s.shot(mc, "gpo_breath_01_beam_front"));
      s.add(t + 42, mc -> s.camera(mc, CameraType.THIRD_PERSON_BACK));
      s.add(t + 52, mc -> s.shot(mc, "gpo_breath_02_beam_back"));
      t += 70;
      s.add(t, mc -> s.camera(mc, CameraType.FIRST_PERSON));
      s.add(t + 6, mc -> s.shot(mc, "gpo_veil_06_fp_hud"));
      s.add(t + 8, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
         SeiryuClientConfig.HIT_COUNTER.set(false);
      });
      return t + 10;
   }

   private static int rush(AutoTest s, int t, String tag) {
      s.add(t, mc -> s.camera(mc, CameraType.THIRD_PERSON_BACK));
      s.add(t + 2, mc -> s.server(mc, p -> stage(s, p, "thunderclaw_rush", 0, true)));
      s.add(t + 8, mc -> s.shot(mc, "gpo_rush_" + tag + "_00_lunge"));
      s.add(t + 12, mc -> s.shot(mc, "gpo_rush_" + tag + "_01_claw1"));
      s.add(t + 20, mc -> s.shot(mc, "gpo_rush_" + tag + "_02_claw2"));
      s.add(t + 26, mc -> s.shot(mc, "gpo_rush_" + tag + "_03_claw3"));
      s.add(t + 34, mc -> s.shot(mc, "gpo_rush_" + tag + "_04_finisher"));
      s.add(t + 44, mc -> s.shot(mc, "gpo_rush_" + tag + "_05_after"));
      return t + 70;
   }
}
