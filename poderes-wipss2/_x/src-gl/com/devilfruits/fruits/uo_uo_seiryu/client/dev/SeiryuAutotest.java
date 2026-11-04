package com.devilfruits.fruits.uo_uo_seiryu.client.dev;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuAbilities;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuItems;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.fruits.uo_uo_seiryu.client.render.DragonRenderer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class SeiryuAutotest implements AutoTest.Script {
   @Override
   public int write(AutoTest s, int start) {
      s.add(start, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
         s.refresh(mc, UoUoSeiryu.ID);
      });
      s.add(start + 12, mc -> s.shot(mc, "seiryu_00_human_back"));
      s.add(start + 14, mc -> s.camera(mc, CameraType.THIRD_PERSON_FRONT));
      s.add(start + 20, mc -> s.shot(mc, "seiryu_01_human_front"));
      s.add(start + 22, mc -> s.server(mc, p -> p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)SeiryuItems.HASSAIKAI.get()))));
      s.add(start + 28, mc -> s.shot(mc, "seiryu_02_hassaikai_front"));
      s.add(start + 30, mc -> s.camera(mc, CameraType.FIRST_PERSON));
      s.add(start + 36, mc -> s.shot(mc, "seiryu_03_hassaikai_fp"));
      s.add(start + 38, mc -> s.camera(mc, CameraType.THIRD_PERSON_BACK));
      int t = start + 40;
      s.add(t, mc -> s.tap(SeiryuAbilities.THUNDER_BAGUA));
      s.add(t + 5, mc -> s.shot(mc, "seiryu_04_bagua_windup"));
      s.add(t + 10, mc -> s.shot(mc, "seiryu_05_bagua_impact"));
      s.add(t + 16, mc -> s.shot(mc, "seiryu_06_bagua_late"));
      t += 30;
      s.add(t, mc -> {
         s.refresh(mc, UoUoSeiryu.ID);
         s.press(SeiryuAbilities.BLAST_BREATH);
      });
      s.add(t + 18, mc -> s.shot(mc, "seiryu_07_breath_charge"));
      s.add(t + 32, mc -> s.release(SeiryuAbilities.BLAST_BREATH));
      s.add(t + 34, mc -> s.shot(mc, "seiryu_08_breath_burst"));
      t += 50;
      s.add(t, mc -> s.refresh(mc, UoUoSeiryu.ID));
      s.add(t + 2, mc -> s.tap(SeiryuAbilities.FLAME_CLOUD));
      s.add(t + 5, mc -> s.shot(mc, "seiryu_09_flame_cloud"));
      s.add(t + 14, mc -> s.shot(mc, "seiryu_10_flame_trail"));
      s.add(t + 30, mc -> s.refresh(mc, UoUoSeiryu.ID));
      s.add(t + 32, mc -> s.tap(SeiryuAbilities.DRAGON_DASH));
      s.add(t + 35, mc -> s.shot(mc, "seiryu_11_dragon_dash"));
      t += 50;
      s.add(t, mc -> {
         s.refresh(mc, UoUoSeiryu.ID);
         s.camera(mc, CameraType.FIRST_PERSON);
      });
      s.add(t + 4, mc -> s.tap(SeiryuAbilities.THUNDER_BAGUA));
      s.add(t + 7, mc -> s.shot(mc, "seiryu_12_fp_bagua_windup"));
      s.add(t + 10, mc -> s.shot(mc, "seiryu_13_fp_bagua_impact"));
      s.add(t + 30, mc -> s.press(SeiryuAbilities.BLAST_BREATH));
      s.add(t + 50, mc -> s.release(SeiryuAbilities.BLAST_BREATH));
      s.add(t + 52, mc -> s.shot(mc, "seiryu_14_fp_breath"));
      s.add(t + 60, mc -> s.camera(mc, CameraType.THIRD_PERSON_BACK));
      t += 70;
      s.add(t, mc -> {
         s.refresh(mc, UoUoSeiryu.ID);
         s.tap(SeiryuAbilities.TRANSFORM_HYBRID);
      });
      s.add(t + 15, mc -> s.shot(mc, "seiryu_15_hybrid_windup"));
      s.add(t + 40, mc -> s.shot(mc, "seiryu_16_hybrid"));
      t = hybrid(s, t + 64);
      s.add(t, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
         s.refresh(mc, UoUoSeiryu.ID);
      });
      s.add(t + 2, mc -> s.tap(SeiryuAbilities.TRANSFORM_DRAGON));
      s.add(t + 28, mc -> s.shot(mc, "seiryu_17_dragon_windup"));
      s.add(t + 58, mc -> s.shot(mc, "seiryu_18_dragon"));
      t = dragon(s, t + 60);
      s.add(t, mc -> s.server(mc, p -> FormManager.revert(p, FormManager.Reason.COMMAND)));
      s.add(t + 8, mc -> s.shot(mc, "seiryu_19_reverted"));
      return t + 10;
   }

   private static int dragon(AutoTest s, int start) {
      s.add(start, mc -> look(mc, -12.0F));
      s.add(start + 2, mc -> mc.options.keyUp.setDown(true));
      s.add(start + 30, mc -> s.shot(mc, "seiryu_d00_flying_back"));
      s.add(start + 32, mc -> look(mc, 8.0F));
      s.add(start + 44, mc -> s.shot(mc, "seiryu_d01_flying_turn"));
      s.add(start + 46, mc -> s.camera(mc, CameraType.THIRD_PERSON_FRONT));
      s.add(start + 56, mc -> s.shot(mc, "seiryu_d02_flying_front"));
      s.add(start + 58, mc -> mc.options.keyUp.setDown(false));
      int t = start + 60;
      s.add(t + 95, mc -> s.shot(mc, "seiryu_d03_coiled_front"));
      s.add(t + 97, mc -> s.camera(mc, CameraType.THIRD_PERSON_BACK));
      s.add(t + 99, mc -> look(mc, 35.0F));
      s.add(t + 104, mc -> s.shot(mc, "seiryu_d04_coiled_back"));
      s.add(t + 106, mc -> {
         s.camera(mc, CameraType.FIRST_PERSON);
         look(mc, 55.0F);
      });
      s.add(t + 112, mc -> s.shot(mc, "seiryu_d05_first_person"));
      t += 114;
      s.add(t, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
         look(mc, 10.0F);
         DragonRenderer.debugCopies = 7;
         mc.options.framerateLimit().set(260);
         mc.options.enableVsync().set(false);
         mc.getWindow().updateVsync(false);
      });
      s.add(t + 40, mc -> {
         DevilFruits.LOGGER.info("[devilfruits autotest] fps with {} dragon bodies: {}", DragonRenderer.count() * 8, mc.getFps());
         s.shot(mc, "seiryu_d06_eight_dragons");
      });
      s.add(t + 42, mc -> DragonRenderer.debugCopies = 0);
      s.add(t + 82, mc -> DevilFruits.LOGGER.info("[devilfruits autotest] fps with {} dragon body: {}", DragonRenderer.count(), mc.getFps()));
      t += 84;
      s.add(t, mc -> s.tap(SeiryuAbilities.SERPENTINE_FLIGHT));
      s.add(t + 30, mc -> {
         look(mc, 15.0F);
         mc.options.keyUp.setDown(true);
      });
      s.add(t + 60, mc -> s.shot(mc, "seiryu_d07_slither"));
      s.add(t + 62, mc -> mc.options.keyUp.setDown(false));
      s.add(t + 64, mc -> s.tap(SeiryuAbilities.SERPENTINE_FLIGHT));
      return techniques(s, t + 70);
   }

   private static int techniques(AutoTest s, int start) {
      s.add(start, mc -> {
         s.refresh(mc, UoUoSeiryu.ID);
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
      });
      s.add(start + 3, mc -> look(mc, 2.0F));
      s.add(start + 4, mc -> s.press(SeiryuAbilities.BLAST_BREATH));
      s.add(start + 24, mc -> s.shot(mc, "seiryu_d08_breath_charge"));
      s.add(start + 34, mc -> s.release(SeiryuAbilities.BLAST_BREATH));
      s.add(start + 46, mc -> s.shot(mc, "seiryu_d09_breath_beam"));
      s.add(start + 48, mc -> s.camera(mc, CameraType.THIRD_PERSON_FRONT));
      s.add(start + 58, mc -> s.shot(mc, "seiryu_d10_breath_front"));
      int t = start + 100;
      s.add(t, mc -> {
         s.refresh(mc, UoUoSeiryu.ID);
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
      });
      s.add(t + 3, mc -> look(mc, 10.0F));
      s.add(t + 2, mc -> s.press(SeiryuAbilities.BOLO_BREATH));
      s.add(t + 26, mc -> s.release(SeiryuAbilities.BOLO_BREATH));
      s.add(t + 30, mc -> s.shot(mc, "seiryu_d11_meteor_spit"));
      s.add(t + 32, mc -> look(mc, -8.0F));
      s.add(t + 54, mc -> s.shot(mc, "seiryu_d12_meteors_falling"));
      s.add(t + 70, mc -> s.shot(mc, "seiryu_d13_meteors_impact"));
      t += 110;
      s.add(t, mc -> s.refresh(mc, UoUoSeiryu.ID));
      s.add(t + 3, mc -> look(mc, 4.0F));
      s.add(t + 2, mc -> s.tap(SeiryuAbilities.ROAR));
      s.add(t + 12, mc -> s.shot(mc, "seiryu_d14_roar"));
      t += 40;
      s.add(t, mc -> s.refresh(mc, UoUoSeiryu.ID));
      s.add(t + 3, mc -> look(mc, 55.0F));
      s.add(t + 2, mc -> s.tap(SeiryuAbilities.TAIL_WHIP));
      s.add(t + 7, mc -> s.shot(mc, "seiryu_d15_tail_whip"));
      s.add(t + 13, mc -> s.shot(mc, "seiryu_d16_tail_whip_end"));
      s.add(t + 60, mc -> s.server(mc, p -> {
            Husk husk = (Husk)EntityType.HUSK.create(p.serverLevel());
            if (husk != null) {
               husk.moveTo(p.getX(), p.getY(), p.getZ() + 7.0, 180.0F, 0.0F);
               husk.setNoAi(true);
               husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400.0);
               husk.setHealth(400.0F);
               husk.addTag("devilfruits_autotest");
               p.serverLevel().addFreshEntity(husk);
            }
         }));
      s.add(t + 62, mc -> s.refresh(mc, UoUoSeiryu.ID));
      s.add(t + 65, mc -> look(mc, 20.0F));
      s.add(t + 64, mc -> s.tap(SeiryuAbilities.TAIL_WHIP));
      s.add(t + 66, mc -> s.tap(SeiryuAbilities.TAIL_WHIP));
      s.add(t + 98, mc -> s.shot(mc, "seiryu_d17_coil_crush"));
      s.add(t + 100, mc -> s.camera(mc, CameraType.THIRD_PERSON_FRONT));
      s.add(t + 106, mc -> s.shot(mc, "seiryu_d18_coil_crush_front"));
      s.add(
         t + 150,
         mc -> s.server(mc, p -> p.serverLevel().getEntities(EntityType.HUSK, h -> h.getTags().contains("devilfruits_autotest")).forEach(Entity::discard))
      );
      t += 152;
      s.add(t, mc -> {
         s.refresh(mc, UoUoSeiryu.ID);
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
         s.server(mc, p -> p.teleportTo(p.getX(), p.getY() + 9.0, p.getZ()));
      });
      s.add(t + 3, mc -> look(mc, 18.0F));
      s.add(t + 4, mc -> s.press(SeiryuAbilities.DRAGON_TWISTER));
      s.add(t + 30, mc -> s.shot(mc, "seiryu_d19_twister"));
      s.add(t + 32, mc -> s.camera(mc, CameraType.THIRD_PERSON_FRONT));
      s.add(t + 50, mc -> s.shot(mc, "seiryu_d20_twister_front"));
      s.add(t + 60, mc -> s.release(SeiryuAbilities.DRAGON_TWISTER));
      t += 80;
      s.add(t, mc -> {
         s.refresh(mc, UoUoSeiryu.ID);
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
      });
      s.add(t + 3, mc -> {
         look(mc, -12.0F);
         mc.options.keyUp.setDown(true);
      });
      s.add(t + 40, mc -> {
         mc.options.keyUp.setDown(false);
         look(mc, -14.0F);
      });
      s.add(t + 44, mc -> s.tap(SeiryuAbilities.THUNDER_STORM));
      s.add(t + 54, mc -> s.shot(mc, "seiryu_d21_storm_call"));
      s.add(t + 82, mc -> s.shot(mc, "seiryu_d22_storm"));
      s.add(t + 84, mc -> look(mc, 12.0F));
      s.add(t + 117, mc -> s.shot(mc, "seiryu_d23_storm_strikes"));
      return t + 210;
   }

   private static void look(Minecraft mc, float pitch) {
      if (mc.player != null) {
         mc.player.setXRot(pitch);
         mc.player.xRotO = pitch;
      }
   }

   private static int hybrid(AutoTest s, int start) {
      s.add(start, mc -> {
         s.refresh(mc, UoUoSeiryu.ID);
         s.camera(mc, CameraType.THIRD_PERSON_FRONT);
      });
      s.add(start + 10, mc -> s.shot(mc, "seiryu_h00_front"));
      s.add(start + 12, mc -> turnBody(mc, 70.0F));
      s.add(start + 16, mc -> s.shot(mc, "seiryu_h01_side"));
      s.add(start + 18, mc -> turnBody(mc, -70.0F));
      s.add(start + 22, mc -> s.shot(mc, "seiryu_h02_other_side"));
      s.add(start + 24, mc -> {
         turnBody(mc, 0.0F);
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
      });
      s.add(start + 30, mc -> s.shot(mc, "seiryu_h03_back"));
      int t = start + 34;
      s.add(t, mc -> {
         s.refresh(mc, UoUoSeiryu.ID);
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
      });
      s.add(t + 28, mc -> {
         s.refresh(mc, UoUoSeiryu.ID);
         s.server(mc, p -> p.teleportTo(p.serverLevel(), p.getX(), p.getY(), p.getZ() - 2.0, 180.0F, 12.0F));
      });
      s.add(t + 30, mc -> s.tap(SeiryuAbilities.DRACONIC_CATACLYSM));
      s.add(t + 46, mc -> s.shot(mc, "seiryu_h09_cataclysm_air"));
      s.add(t + 62, mc -> s.shot(mc, "seiryu_h10_cataclysm_smash"));
      t += 90;
      s.add(t - 4, mc -> s.server(mc, p -> FormManager.request(p, "hybrid", FormManager.Reason.COMMAND)));
      s.add(t, mc -> {
         s.refresh(mc, UoUoSeiryu.ID);
         s.camera(mc, CameraType.FIRST_PERSON);
         s.server(mc, p -> p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
      });
      s.add(t + 8, mc -> s.shot(mc, "seiryu_h13_fp_claw"));
      s.add(t + 10, mc -> {
         if (mc.player != null) {
            mc.player.swing(InteractionHand.MAIN_HAND);
         }
      });
      s.add(t + 13, mc -> s.shot(mc, "seiryu_h14_fp_claw_swing"));
      s.add(t + 16, mc -> s.server(mc, p -> p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)SeiryuItems.HASSAIKAI.get()))));
      s.add(t + 24, mc -> s.shot(mc, "seiryu_h15_fp_club"));
      return t + 26;
   }

   private static void turnBody(Minecraft mc, float offset) {
      if (mc.player != null) {
         mc.player.yBodyRot = mc.player.getYRot() + offset;
         mc.player.yBodyRotO = mc.player.yBodyRot;
      }
   }
}
