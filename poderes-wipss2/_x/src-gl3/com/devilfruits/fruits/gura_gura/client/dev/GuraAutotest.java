package com.devilfruits.fruits.gura_gura.client.dev;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.hud.AbilityWheelScreen;
import com.devilfruits.client.hud.FruitInfoScreen;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.fruits.gura_gura.GuraAbilities;
import com.devilfruits.fruits.gura_gura.GuraGura;
import com.devilfruits.fruits.gura_gura.GuraItems;
import com.devilfruits.registry.DFAttachments;
import net.minecraft.client.CameraType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class GuraAutotest implements AutoTest.Script {
   @Override
   public int write(AutoTest s, int start) {
      s.add(start, mc -> s.shot(mc, "gura_00_setup"));
      int t = start + 5;
      s.add(t, mc -> s.server(mc, p -> {
            FruitRules.grantPower(p, GuraGura.fruit(), true);
            DFAttachments.data(p).setMastery(10);
         }));
      t += 6;
      s.add(t, mc -> s.shot(mc, "gura_01_awaken"));
      t += 40;
      s.add(t, mc -> s.press(GuraAbilities.KAISHIN));
      s.add(t + 30, mc -> s.shot(mc, "gura_02_kaishin_charge"));
      s.add(t + 42, mc -> s.release(GuraAbilities.KAISHIN));
      s.add(t + 44, mc -> s.shot(mc, "gura_03_kaishin_release"));
      s.add(t + 49, mc -> s.shot(mc, "gura_04_kaishin_shockwave"));
      t += 90;
      s.add(t, mc -> s.refresh(mc, GuraGura.ID));
      s.add(t + 2, mc -> s.tap(GuraAbilities.GEKISHIN));
      s.add(t + 8, mc -> s.shot(mc, "gura_05_gekishin_hit"));
      s.add(t + 14, mc -> s.shot(mc, "gura_06_gekishin_launch"));
      t += 70;
      s.add(t, mc -> s.refresh(mc, GuraGura.ID));
      s.add(t + 2, mc -> s.tap(GuraAbilities.KABUTOWARI));
      s.add(t + 14, mc -> s.shot(mc, "gura_07_kabutowari_air"));
      s.add(t + 34, mc -> s.shot(mc, "gura_08_kabutowari_fissure"));
      s.add(t + 44, mc -> s.shot(mc, "gura_09_kabutowari_fissure_late"));
      t += 100;
      s.add(t, mc -> s.refresh(mc, GuraGura.ID));
      s.add(t + 2, mc -> s.tap(GuraAbilities.KAISHO));
      s.add(t + 6, mc -> s.server(mc, p -> p.teleportTo(p.serverLevel(), 0.5, (double)s.origin().getY(), 0.5, 42.0F, 4.0F)));
      s.add(t + 12, mc -> s.shot(mc, "gura_10_kaisho_wave"));
      s.add(t + 20, mc -> s.shot(mc, "gura_11_kaisho_wave_late"));
      t += 70;
      s.add(t, mc -> s.refresh(mc, GuraGura.ID));
      s.add(t + 2, mc -> s.camera(mc, CameraType.THIRD_PERSON_FRONT));
      s.add(t + 4, mc -> s.tap(GuraAbilities.SHINDO));
      s.add(t + 10, mc -> s.shot(mc, "gura_12_shindo_pose"));
      s.add(t + 30, mc -> s.shot(mc, "gura_12b_shindo_aura"));
      s.add(t + 34, mc -> s.tap(GuraAbilities.SHINDO));
      s.add(t + 36, mc -> s.camera(mc, CameraType.THIRD_PERSON_BACK));
      t += 60;
      s.add(t, mc -> s.server(mc, p -> p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)GuraItems.BISENTO.get()))));
      s.add(t + 4, mc -> s.refresh(mc, GuraGura.ID));
      s.add(t + 6, mc -> s.tap(GuraAbilities.QUAKE_INFUSION));
      s.add(t + 10, mc -> s.shot(mc, "gura_13_quake_slash"));
      s.add(t + 16, mc -> s.shot(mc, "gura_14_quake_slash_late"));
      t += 60;
      s.add(t, mc -> s.refresh(mc, GuraGura.ID));
      s.add(t + 2, mc -> s.tap(GuraAbilities.SHIMA_YURASHI));
      s.add(t + 14, mc -> s.shot(mc, "gura_15_shima_grab"));
      s.add(t + 24, mc -> s.shot(mc, "gura_16_shima_twist"));
      s.add(t + 26, mc -> s.camera(mc, CameraType.THIRD_PERSON_FRONT));
      s.add(t + 30, mc -> s.shot(mc, "gura_17_shima_pose_front"));
      s.add(t + 31, mc -> s.camera(mc, CameraType.THIRD_PERSON_BACK));
      s.add(t + 60, mc -> s.shot(mc, "gura_18_shima_tremor"));
      t += 140;
      s.add(t, mc -> {
         s.camera(mc, CameraType.FIRST_PERSON);
         s.server(mc, p -> p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
         s.refresh(mc, GuraGura.ID);
      });
      s.add(t + 4, mc -> s.press(GuraAbilities.KAISHIN));
      s.add(t + 34, mc -> s.shot(mc, "gura_19_fp_charge_hud"));
      s.add(t + 40, mc -> s.release(GuraAbilities.KAISHIN));
      s.add(t + 42, mc -> s.shot(mc, "gura_20_fp_kaishin"));
      s.add(t + 60, mc -> s.refresh(mc, GuraGura.ID));
      s.add(t + 62, mc -> s.tap(GuraAbilities.KAISHO));
      s.add(t + 66, mc -> s.shot(mc, "gura_21_fp_kaisho_both_fists"));
      t += 100;
      s.add(t, mc -> mc.setScreen(new AbilityWheelScreen(false)));
      s.add(t + 5, mc -> s.shot(mc, "gura_22_wheel"));
      s.add(t + 8, mc -> mc.setScreen(new FruitInfoScreen()));
      s.add(t + 13, mc -> s.shot(mc, "gura_23_info"));
      s.add(t + 16, mc -> mc.setScreen(null));
      return t + 16;
   }
}
