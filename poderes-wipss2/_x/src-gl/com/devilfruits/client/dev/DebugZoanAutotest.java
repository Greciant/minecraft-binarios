package com.devilfruits.client.dev;

import com.devilfruits.client.hud.FruitInfoScreen;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.core.gametest.fixture.DebugZoan;
import net.minecraft.client.CameraType;

public final class DebugZoanAutotest implements AutoTest.Script {
   public static void register() {
      AutoTest.addScript(DebugZoan.ID, new DebugZoanAutotest());
   }

   @Override
   public int write(AutoTest s, int start) {
      s.add(start, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
         s.refresh(mc, DebugZoan.ID);
      });
      s.add(start + 10, mc -> s.shot(mc, "debug_00_base"));
      s.add(start + 12, mc -> s.tap(DebugZoan.TRANSFORM));
      s.add(start + 24, mc -> s.shot(mc, "debug_01_beast"));
      s.add(start + 26, mc -> s.press(DebugZoan.BREATH));
      s.add(start + 36, mc -> s.shot(mc, "debug_02_channel"));
      s.add(start + 40, mc -> s.release(DebugZoan.BREATH));
      s.add(start + 44, mc -> s.tap(DebugZoan.FLURRY));
      s.add(start + 46, mc -> s.tap(DebugZoan.FLURRY));
      s.add(start + 48, mc -> s.tap(DebugZoan.FLURRY));
      s.add(start + 52, mc -> s.shot(mc, "debug_03_combo_cooldown"));
      s.add(start + 56, mc -> s.server(mc, p -> FormManager.request(p, "winged", FormManager.Reason.COMMAND)));
      s.add(start + 64, mc -> s.shot(mc, "debug_04_winged"));
      s.add(start + 66, mc -> mc.setScreen(new FruitInfoScreen()));
      s.add(start + 71, mc -> s.shot(mc, "debug_05_info"));
      s.add(start + 74, mc -> mc.setScreen(null));
      s.add(start + 76, mc -> s.server(mc, p -> FormManager.revert(p, FormManager.Reason.COMMAND)));
      s.add(start + 84, mc -> s.shot(mc, "debug_06_reverted"));
      return start + 84;
   }
}
