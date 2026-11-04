package com.devilfruits.client.haki;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.core.haki.HakiManager;
import com.devilfruits.core.rules.FruitRules;
import net.minecraft.client.CameraType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;

final class HakiAutotest implements AutoTest.Script {
   @Override
   public int write(AutoTest s, int t) {
      s.add(t, mc -> s.server(mc, p -> {
            FruitRules.removePower(p);
            HakiManager.setLevel(p, 10);
            HakiManager.select(p, 511);
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            p.teleportTo(p.serverLevel(), 0.5, (double)s.origin().getY(), -8.5, 0.0F, 8.0F);
         }));
      int u = t + 45;
      s.add(u + 2, mc -> s.camera(mc, CameraType.THIRD_PERSON_FRONT));
      s.add(u + 8, mc -> s.server(mc, HakiManager::coatOn));
      s.add(u + 11, mc -> s.shot(mc, "haki_01_coat_spreading"));
      s.add(u + 20, mc -> s.shot(mc, "haki_02_full_body_front"));
      s.add(u + 21, mc -> s.camera(mc, CameraType.THIRD_PERSON_BACK));
      s.add(u + 25, mc -> s.shot(mc, "haki_03_full_body_back"));
      s.add(u + 26, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_FRONT);
         s.server(mc, p -> p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD)));
      });
      s.add(u + 32, mc -> s.shot(mc, "haki_04_coated_sword"));
      s.add(u + 33, mc -> s.camera(mc, CameraType.FIRST_PERSON));
      s.add(u + 48, mc -> s.shot(mc, "haki_05_first_person_sword"));
      s.add(u + 49, mc -> s.server(mc, p -> p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY)));
      s.add(u + 54, mc -> s.shot(mc, "haki_06_first_person_fist"));
      s.add(u + 55, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
         s.server(mc, p -> {
            Husk husk = (Husk)EntityType.HUSK.create(p.serverLevel());
            if (husk != null) {
               husk.moveTo(0.5, (double)s.origin().getY(), -6.4, 180.0F, 0.0F);
               husk.setNoAi(true);
               p.serverLevel().addFreshEntity(husk);
            }
         });
      });
      s.add(u + 58, mc -> s.server(mc, HakiManager::ryuoStart));
      s.add(u + 80, mc -> s.shot(mc, "haki_07_ryuo_charging"));
      s.add(u + 90, mc -> s.server(mc, HakiManager::ryuoRelease));
      s.add(u + 92, mc -> s.shot(mc, "haki_08_ryuo_strike"));
      s.add(u + 94, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_FRONT);
         s.server(mc, p -> {
            p.serverLevel().getEntitiesOfClass(Husk.class, p.getBoundingBox().inflate(4.0)).forEach(h -> h.discard());
            ItemStack crossbow = new ItemStack(Items.CROSSBOW);
            crossbow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(new ItemStack(Items.ARROW)));
            p.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
         });
      });
      s.add(u + 100, mc -> s.shot(mc, "haki_10_loaded_crossbow"));
      s.add(u + 101, mc -> s.server(mc, p -> {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            p.setItemInHand(InteractionHand.MAIN_HAND, sword);
         }));
      s.add(u + 106, mc -> s.shot(mc, "haki_11_enchanted_sword"));
      s.add(u + 107, mc -> s.camera(mc, CameraType.FIRST_PERSON));
      s.add(u + 122, mc -> s.shot(mc, "haki_12_first_person_enchanted"));
      s.add(u + 123, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
         s.server(mc, p -> p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
      });
      s.add(u + 128, mc -> s.server(mc, p -> HakiManager.coatOff(p, false)));
      s.add(u + 145, mc -> s.shot(mc, "haki_09_coat_off"));
      s.add(u + 146, mc -> s.server(mc, p -> HakiManager.select(p, 267)));
      s.add(u + 150, mc -> mc.setScreen(new HakiScreen()));
      s.add(u + 166, mc -> s.shot(mc, "haki_13_screen_master"));
      s.add(u + 167, mc -> {
         mc.setScreen(null);
         s.server(mc, p -> HakiManager.setLevel(p, 2));
      });
      s.add(u + 172, mc -> mc.setScreen(new HakiScreen()));
      s.add(u + 188, mc -> s.shot(mc, "haki_14_screen_novice"));
      s.add(u + 189, mc -> {
         mc.setScreen(null);
         s.server(mc, p -> HakiManager.setLevel(p, 10));
      });
      return u + 192;
   }
}
