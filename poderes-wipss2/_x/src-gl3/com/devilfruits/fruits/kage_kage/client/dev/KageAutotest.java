package com.devilfruits.fruits.kage_kage.client.dev;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.fruits.kage_kage.KageItems;
import com.devilfruits.fruits.kage_kage.KageKage;
import com.devilfruits.fruits.kage_kage.item.MoriaScissorsItem;
import com.devilfruits.fruits.kage_kage.shadow.Shadows;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class KageAutotest implements AutoTest.Script {
   @Override
   public int write(AutoTest steps, int start) {
      steps.add(start, mc -> steps.refresh(mc, KageKage.ID));
      steps.add(start + 5, mc -> steps.server(mc, p -> {
            p.serverLevel().setDayTime(6000L);
            BlockPos o = steps.origin();
            Villager robbed = (Villager)EntityType.VILLAGER.create(p.serverLevel());
            Villager whole = (Villager)EntityType.VILLAGER.create(p.serverLevel());
            if (robbed != null && whole != null) {
               robbed.moveTo((double)o.getX() - 0.5, (double)o.getY(), (double)o.getZ() + 3.5, 180.0F, 0.0F);
               whole.moveTo((double)o.getX() + 1.5, (double)o.getY(), (double)o.getZ() + 3.5, 180.0F, 0.0F);
               robbed.setNoAi(true);
               whole.setNoAi(true);
               p.serverLevel().addFreshEntity(robbed);
               p.serverLevel().addFreshEntity(whole);
               Shadows.take(p, robbed);
               p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)KageItems.MORIA_SCISSORS.get()));
               p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
               MoriaScissorsItem.split(p);
               p.teleportTo(p.serverLevel(), (double)o.getX() + 0.5, (double)o.getY(), (double)o.getZ() - 1.5, 0.0F, 30.0F);
            }
         }));
      steps.add(start + 30, mc -> steps.camera(mc, CameraType.FIRST_PERSON));
      steps.add(start + 40, mc -> steps.shot(mc, "kage_m1_hud"));
      steps.add(start + 45, mc -> steps.camera(mc, CameraType.THIRD_PERSON_BACK));
      steps.add(start + 55, mc -> steps.shot(mc, "kage_m1_shadowless"));
      steps.add(start + 60, mc -> {
         steps.camera(mc, CameraType.FIRST_PERSON);
         steps.server(mc, p -> Shadows.releaseAll(p, Shadows.Release.COMMAND));
      });
      steps.add(start + 70, mc -> steps.shot(mc, "kage_m1_returned"));
      return start + 75;
   }
}
