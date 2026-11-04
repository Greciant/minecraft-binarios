package com.devilfruits.fruits.suna_suna.client.dev;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureRig;
import com.devilfruits.client.dev.CaptureStage;
import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.fruits.suna_suna.SunaSuna;
import com.devilfruits.registry.DFAttachments;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.CameraType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public final class SandBlowCheck extends CaptureStage {
   public SandBlowCheck() {
      super("vfx_sbl_");
   }

   @Override
   protected ResourceLocation fruit() {
      return SunaSuna.ID;
   }

   @Override
   protected void setup(ServerPlayer p) {
      DFAttachments.data(p).setMastery(10);
      command(p, "time set 6000");
      command(p, "gamemode survival");
      command(p, "difficulty normal");
   }

   private static void still(AutoTest s, int tick, String name) {
      s.add(tick, mc -> s.shot(mc, "vfx_sbl_" + name));
   }

   @Override
   protected int takes(AutoTest s, int t) {
      t += 40;
      int c = this.take(s, t, 20, p -> {
         LogiaManager.setIntangible(p, true);
         dummyTo(s, p, -40.0);
      }, 10, p -> {
      }, rig(4.2, 140.0F, 8.0F, 0.4), 200, cc -> {
      });
      int start = c - 200;

      for (int k = 0; k < 3; k++) {
         int at = start + 5 + k * 14;
         s.add(at, mc -> s.server(mc, p -> {
               ServerPlayer fake = FakePlayerFactory.get(p.serverLevel(), new GameProfile(UUID.nameUUIDFromBytes("sand_blows".getBytes()), "swordsman"));
               fake.moveTo(p.getX(), p.getY(), p.getZ() - 1.4, 0.0F, 0.0F);
               fake.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
               fake.resetAttackStrengthTicker();
               fake.attack(p);
               DevilFruits.LOGGER.info("[blow] swordsman struck: intangible {} health {}", DFAttachments.data(p).logia().intangible(), p.getHealth());
            }));
         still(s, at + 1, "sword_" + k + "_a");
         still(s, at + 3, "sword_" + k + "_b");
      }

      s.add(start + 60, mc -> s.server(mc, p -> {
            Zombie z = (Zombie)EntityType.ZOMBIE.create(p.serverLevel());
            if (z != null) {
               z.moveTo(p.getX() + 1.2, p.getY(), p.getZ() - 0.4, 90.0F, 0.0F);
               z.setTarget(p);
               p.serverLevel().addFreshEntity(z);
            }
         }));

      for (int k = 0; k < 8; k++) {
         still(s, start + 80 + k * 6, "zombie_" + k);
      }

      s.add(start + 140, mc -> s.server(mc, p -> command(p, "kill @e[type=zombie]")));
      s.add(
         start + 145,
         mc -> s.server(mc, p -> command(p, "summon arrow " + p.getX() + " " + (p.getY() + 1.3) + " " + (p.getZ() - 6.0) + " {Motion:[0.0,0.0,1.5]}"))
      );
      still(s, start + 148, "arrow_a");
      still(s, start + 150, "arrow_b");
      s.add(c, mc -> {
         CaptureRig.off();
         mc.options.setCameraType(CameraType.FIRST_PERSON);
      });
      return c + 5;
   }
}
