package com.devilfruits.client.dev;

import com.devilfruits.core.gametest.fixture.DebugLogia;
import com.devilfruits.core.logia.CounterTrigger;
import com.devilfruits.core.logia.DamageCategory;
import com.devilfruits.core.logia.HitRule;
import com.devilfruits.core.logia.LogiaDamage;
import com.devilfruits.core.logia.LogiaDash;
import com.devilfruits.core.logia.LogiaHits;
import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.core.logia.LogiaState;
import net.minecraft.client.CameraType;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class DebugLogiaAutotest implements AutoTest.Script {
   public static void register() {
      AutoTest.addScript(DebugLogia.ID, new DebugLogiaAutotest());
   }

   @Override
   public int write(AutoTest s, int start) {
      int t = start;
      s.add(start, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_FRONT);
         s.refresh(mc, DebugLogia.ID);
      });
      s.add(start + 10, mc -> s.shot(mc, "logia_00_solid"));
      s.add(start + 12, mc -> s.tap(DebugLogia.PULSE));
      s.add(start + 30, mc -> s.server(mc, p -> LogiaManager.partial(p, 12, 40)));
      s.add(start + 42, mc -> s.shot(mc, "logia_01_partial_arms"));
      s.add(start + 76, mc -> s.server(mc, p -> LogiaManager.partial(p, 62, 40)));
      s.add(start + 88, mc -> s.shot(mc, "logia_02_partial_body"));
      s.add(start + 124, mc -> s.server(mc, DebugLogiaAutotest::punch));
      s.add(start + 130, mc -> s.shot(mc, "logia_03_hit_holes"));
      s.add(start + 146, mc -> s.shot(mc, "logia_04_holes_refilling"));
      s.add(start + 170, mc -> s.server(mc, p -> LogiaManager.setElemental(p, true)));
      s.add(start + 178, mc -> s.shot(mc, "logia_05_dissolving"));
      s.add(start + 190, mc -> s.shot(mc, "logia_06_elemental_body"));
      s.add(start + 196, mc -> s.server(mc, p -> LogiaManager.setElemental(p, false)));
      s.add(start + 204, mc -> s.shot(mc, "logia_07_reforming"));
      s.add(start + 220, mc -> s.server(mc, p -> LogiaDash.dash(p, p.getEyePosition(), new Vec3(0.0, 0.0, -1.0), 6.0)));
      s.add(start + 225, mc -> s.shot(mc, "logia_08_dash"));
      s.add(start + 236, mc -> s.shot(mc, "logia_09_dash_reformed"));
      s.add(start + 240, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
         s.refresh(mc, DebugLogia.ID);
      });
      s.add(start + 250, mc -> {
         mc.getTutorial().setStep(TutorialSteps.NONE);
         mc.getToasts().clear();
         mc.options.guiScale().set(1);
         mc.resizeDisplay();
         s.server(mc, p -> LogiaManager.trigger(p, CounterTrigger.Kind.WATER_BUCKET, null));
      });
      s.add(start + 260, mc -> s.shot(mc, "logia_10_wet_hud"));
      s.add(start + 262, mc -> {
         mc.options.guiScale().set(0);
         mc.resizeDisplay();
      });
      s.add(start + 320, mc -> {
         s.camera(mc, CameraType.FIRST_PERSON);
         s.server(mc, p -> LogiaManager.forceState(p, LogiaState.SOLID, 0));
      });
      s.add(start + 326, mc -> s.shot(mc, "logia_11_first_person_solid"));
      s.add(start + 328, mc -> s.server(mc, p -> LogiaManager.partial(p, 12, 40)));

      for (int i = 2; i <= 8; i += 2) {
         int at = i;
         s.add(t + 328 + at, mc -> s.shot(mc, "logia_12_first_person_arm_" + at));
      }

      s.add(t + 340, mc -> s.camera(mc, CameraType.THIRD_PERSON_BACK));
      return t + 342;
   }

   private static void punch(ServerPlayer p) {
      LogiaDamage.Decision pass = new LogiaDamage.Decision(HitRule.PASS, DamageCategory.MELEE, 0.0F);
      Vec3 feet = p.position();
      Vec3 front = Vec3.directionFromRotation(0.0F, p.yBodyRot);
      LogiaHits.add(p, feet.add(front.scale(2.0)).add(0.0, 1.2, 0.0), front.scale(-1.0), pass, -1);
      LogiaHits.add(p, feet.add(front.scale(2.0)).add(0.35, 1.7, 0.0), front.scale(-1.0), pass, -1);
      LogiaHits.add(
         p, feet.add(front.scale(2.0)).add(-0.4, 1.1, 0.0), front.scale(-1.0), new LogiaDamage.Decision(HitRule.PARTIAL, DamageCategory.FIRE, 0.5F), -1
      );
   }
}
