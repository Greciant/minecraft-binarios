package com.devilfruits.fruits.yomi_yomi.client.dev;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureRig;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.fruits.yomi_yomi.Vessel;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.fruits.yomi_yomi.client.VesselScreen;
import com.devilfruits.fruits.yomi_yomi.entity.PlayerCorpse;
import com.devilfruits.fruits.yomi_yomi.soul.SoulPhase;
import com.devilfruits.fruits.yomi_yomi.soul.YomiSoul;
import com.devilfruits.registry.DFAttachments;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class YomiAutotest implements AutoTest.Script {
   private static void command(ServerPlayer p, String cmd) {
      p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), cmd);
   }

   @Override
   public int write(AutoTest steps, int start) {
      steps.add(start, mc -> steps.refresh(mc, YomiYomi.ID));
      steps.add(start + 5, mc -> steps.server(mc, p -> {
            p.serverLevel().setDayTime(6000L);
            DFAttachments.data(p).setHome("");
            p.setData(YomiSoul.ATTACHMENT, YomiSoul.NONE);
            FormManager.revert(p, FormManager.Reason.COMMAND);
         }));
      steps.add(start + 10, mc -> steps.server(mc, p -> p.hurt(p.damageSources().genericKill(), Float.MAX_VALUE)));
      steps.add(start + 40, mc -> {
         if (mc.player != null) {
            mc.player.respawn();
         }
      });
      steps.add(start + 60, mc -> {
         mc.setScreen(null);
         steps.camera(mc, CameraType.THIRD_PERSON_FRONT);
         steps.server(mc, p -> p.setXRot(-10.0F));
      });
      steps.add(start + 80, mc -> steps.shot(mc, "yomi_m3_soul_sky"));
      steps.add(start + 85, mc -> steps.server(mc, p -> {
            YomiSoul soul = YomiSoul.of(p);
            p.getAbilities().flying = true;
            p.teleportTo(p.serverLevel(), soul.body().x + 0.5, soul.body().y + 1.4, soul.body().z + 3.2, 180.0F, 20.0F);
         }));
      steps.add(start + 100, mc -> steps.shot(mc, "yomi_m3_soul_near"));
      steps.add(start + 105, mc -> steps.server(mc, p -> {
            if (p.serverLevel().getEntity(YomiSoul.of(p).corpseId()) instanceof PlayerCorpse corpse) {
               SoulPhase.requestPossession(p, corpse);
            }
         }));
      steps.add(start + 125, mc -> steps.shot(mc, "yomi_m3_menu"));
      steps.add(start + 128, mc -> {
         if (mc.screen instanceof VesselScreen sx) {
            sx.autotestPick(Vessel.SKELETON);
         }
      });
      steps.add(start + 140, mc -> steps.shot(mc, "yomi_m3_menu_picked"));
      steps.add(start + 142, mc -> {
         mc.gui.getChat().clearMessages(false);
         steps.camera(mc, CameraType.THIRD_PERSON_FRONT);
         steps.server(mc, p -> command(p, "tick rate 10"));
      });
      steps.add(start + 145, mc -> {
         if (mc.screen instanceof VesselScreen sx) {
            sx.autotestConfirm();
         }
      });
      int f0 = start + 146;

      for (int i = 0; i < 72; i++) {
         int n = i;
         steps.add(f0 + i, mc -> steps.shot(mc, String.format("yomi_m3_pos_%03d", n)));
      }

      int s = f0 + 74;
      steps.add(s, mc -> {
         steps.server(mc, p -> command(p, "tick rate 20"));
         steps.camera(mc, CameraType.THIRD_PERSON_FRONT);
      });
      steps.add(s + 15, mc -> steps.shot(mc, "yomi_m3_skeleton_front"));
      steps.add(s + 16, mc -> CaptureRig.follow(mc.player, 3.2, 40.0F, 12.0F, 0.0));
      steps.add(s + 24, mc -> steps.shot(mc, "yomi_m3_skeleton_34"));
      steps.add(s + 25, mc -> CaptureRig.follow(mc.player, 3.2, 95.0F, 8.0F, 0.0));
      steps.add(s + 32, mc -> steps.shot(mc, "yomi_m3_skeleton_side"));
      steps.add(s + 33, mc -> {
         CaptureRig.off();
         steps.camera(mc, CameraType.THIRD_PERSON_BACK);
         steps.server(mc, p -> p.teleportTo(p.serverLevel(), 0.5, (double)steps.origin().getY(), -2.5, 0.0F, 10.0F));
      });
      steps.add(s + 42, mc -> steps.shot(mc, "yomi_m3_skeleton_back"));
      steps.add(s + 45, mc -> {
         steps.camera(mc, CameraType.FIRST_PERSON);
         steps.server(mc, p -> p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
      });
      steps.add(s + 58, mc -> steps.shot(mc, "yomi_m3_skeleton_fp"));
      steps.add(s + 60, mc -> mc.setScreen(new InventoryScreen(mc.player)));
      steps.add(s + 70, mc -> steps.shot(mc, "yomi_m3_skeleton_inv"));
      steps.add(s + 72, mc -> {
         mc.setScreen(null);
         steps.camera(mc, CameraType.THIRD_PERSON_FRONT);
         steps.server(mc, p -> SoulPhase.becomeVessel(p, Vessel.CORPSE));
      });
      steps.add(s + 88, mc -> steps.shot(mc, "yomi_m3_corpse_front"));
      steps.add(s + 90, mc -> steps.server(mc, p -> {
            SoulPhase.becomeVessel(p, Vessel.SKELETON);
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            p.teleportTo(p.serverLevel(), -4.5, (double)steps.origin().getY(), 1.2, 0.0F, 15.0F);
         }));
      steps.add(s + 100, mc -> {
         steps.camera(mc, CameraType.THIRD_PERSON_BACK);
         steps.server(mc, p -> command(p, "tick rate 10"));
      });
      steps.add(s + 104, mc -> {
         mc.gui.getChat().clearMessages(false);
         mc.options.keyUp.setDown(true);
         mc.options.keySprint.setDown(true);
      });
      int r0 = s + 106;

      for (int i = 0; i < 60; i++) {
         int n = i;
         steps.add(r0 + i, mc -> {
            if (mc.player != null) {
               mc.player.setSprinting(true);
            }

            steps.shot(mc, String.format("yomi_m3_run_%03d", n));
         });
      }

      int e = r0 + 62;
      steps.add(e, mc -> {
         mc.options.keyUp.setDown(false);
         mc.options.keySprint.setDown(false);
         steps.server(mc, p -> command(p, "tick rate 20"));
         steps.camera(mc, CameraType.FIRST_PERSON);
      });
      return e + 5;
   }
}
