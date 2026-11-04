package com.devilfruits.mobs.pirate.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.core.npc.FactionFighter;
import com.devilfruits.mobs.pirate.PirateClips;
import com.devilfruits.mobs.pirate.Pirates;
import com.devilfruits.mobs.pirate.entity.PirateFighter;
import com.devilfruits.mobs.pirate.entity.PirateGunner;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;

final class PirateAutotest implements AutoTest.Script {
   private static final double X = 20.5;
   private static final List<Mob> SPAWNED = new ArrayList<>();
   private static Vec3 chaseFrom;
   private static Mob chaser;

   @Override
   public int write(AutoTest s, int start) {
      s.add(start, mc -> {
         s.camera(mc, CameraType.FIRST_PERSON);
         s.server(mc, p -> p.teleportTo(p.serverLevel(), 20.5, (double)s.origin().getY(), 0.5, 0.0F, 0.0F));
      });
      int t = start + 10;
      t = hold(s, t, "cutlass", new ItemStack((ItemLike)Pirates.CUTLASS.get()));
      t = hold(s, t, "captain_cutlass", new ItemStack((ItemLike)Pirates.CAPTAIN_CUTLASS.get()));
      t = gun(s, t, "pistol", new ItemStack((ItemLike)Pirates.FLINTLOCK_PISTOL.get()), 40);
      t = gun(s, t, "musket", new ItemStack((ItemLike)Pirates.MUSKET.get()), 70);
      s.add(t, mc -> s.camera(mc, CameraType.THIRD_PERSON_FRONT));
      s.add(t + 12, mc -> s.shot(mc, "pirates_tp_player_musket_ready"));
      s.add(t + 14, mc -> {
         s.camera(mc, CameraType.FIRST_PERSON);
         s.server(mc, p -> p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
      });
      t += 20;
      t = drill(s, t, "musket", new ItemStack((ItemLike)Pirates.MUSKET.get()), 8, 16);
      t = drill(s, t, "pistol", new ItemStack((ItemLike)Pirates.FLINTLOCK_PISTOL.get()), 6, 12);
      s.add(t, mc -> s.server(mc, p -> {
            clear();
            spawn(p, Pirates.DECKHAND.get(), 18.1, 4.5, 180.0F, new ItemStack((ItemLike)Pirates.CUTLASS.get()));
            spawn(p, Pirates.GUNNER.get(), 20.5, 4.5, 180.0F, new ItemStack((ItemLike)Pirates.MUSKET.get()));
            spawn(p, Pirates.CAPTAIN.get(), 23.3, 5.5, 180.0F, new ItemStack((ItemLike)Pirates.CAPTAIN_CUTLASS.get()));
         }));
      s.add(t + 20, mc -> s.shot(mc, "pirates_trio_unarmed"));
      s.add(t + 22, mc -> s.server(mc, p -> SPAWNED.forEach(m -> ((FactionFighter)m).stayArmed(200))));
      s.add(t + 45, mc -> s.shot(mc, "pirates_trio_armed"));
      s.add(t + 50, mc -> s.server(mc, p -> clear()));
      t += 60;
      s.add(t, mc -> s.server(mc, p -> {
            ServerLevel level = p.serverLevel();
            Mob target = (Mob)EntityType.HUSK.create(level);
            if (target != null) {
               target.moveTo(40.5, p.getY(), 16.0, 0.0F, 0.0F);
               target.setNoAi(true);
               target.setInvulnerable(true);
               level.addFreshEntity(target);
               SPAWNED.add(target);
               chaser = spawn(p, Pirates.DECKHAND.get(), 40.5, -8.0, 0.0F, new ItemStack((ItemLike)Pirates.CUTLASS.get()));
               chaser.setNoAi(false);
               chaser.setTarget(target);
            }
         }));
      s.add(t + 20, mc -> s.server(mc, p -> chaseFrom = chaser.position()));
      s.add(t + 50, mc -> s.server(mc, p -> {
            double d = chaser.position().subtract(chaseFrom).horizontalDistance();
            DevilFruits.LOGGER.info("[devilfruits autotest] deckhand chase speed {} blocks/s (a walking player: 4.32)", String.format("%.2f", d / 1.5));
            clear();
         }));
      return t + 60;
   }

   private static int hold(AutoTest s, int t, String name, ItemStack stack) {
      s.add(t, mc -> s.server(mc, p -> p.setItemInHand(InteractionHand.MAIN_HAND, stack.copy())));
      s.add(t + 15, mc -> s.shot(mc, "pirates_fp_" + name));
      return t + 20;
   }

   private static int gun(AutoTest s, int t, String name, ItemStack stack, int reload) {
      s.add(t, mc -> s.server(mc, p -> p.setItemInHand(InteractionHand.MAIN_HAND, stack.copy())));
      s.add(t + 15, mc -> s.shot(mc, "pirates_fp_" + name + "_0_empty"));
      s.add(t + 17, mc -> {
         KeyMapping.click(mc.options.keyUse.getKey());
         mc.options.keyUse.setDown(true);
      });
      s.add(t + 17 + reload / 4, mc -> s.shot(mc, "pirates_fp_" + name + "_1_loading"));
      s.add(t + 17 + reload / 2, mc -> s.shot(mc, "pirates_fp_" + name + "_2_loading"));
      s.add(t + 17 + reload * 3 / 4, mc -> s.shot(mc, "pirates_fp_" + name + "_3_loading"));
      s.add(t + 17 + reload + 10, mc -> mc.options.keyUse.setDown(false));
      int ready = t + 17 + reload + 30;
      s.add(ready, mc -> s.shot(mc, "pirates_fp_" + name + "_4_ready"));
      s.add(ready + 2, mc -> KeyMapping.click(mc.options.keyAttack.getKey()));
      s.add(ready + 5, mc -> s.shot(mc, "pirates_fp_" + name + "_5_shot"));
      s.add(ready + 20, mc -> s.shot(mc, "pirates_fp_" + name + "_6_after"));
      return ready + 25;
   }

   private static int drill(AutoTest s, int t, String name, ItemStack gun, int action, int every) {
      s.add(
         t,
         mc -> s.server(
               mc,
               p -> {
                  clear();

                  for (Mob m : List.of(
                     (PirateGunner)spawn(p, Pirates.GUNNER.get(), 19.3, 4.0, 180.0F, gun.copy()),
                     (PirateGunner)spawn(p, Pirates.GUNNER.get(), 22.1, 4.4, 90.0F, gun.copy())
                  )) {
                     ((FactionFighter)m).stayArmed(400);
                  }
               }
            )
      );
      s.add(t + 20, mc -> s.server(mc, p -> SPAWNED.forEach(m -> {
               ((FactionFighter)m).setFlag(8, false);
               ((FactionFighter)m).playAction(action, PirateClips.LENGTH[action]);
            })));
      int length = PirateClips.LENGTH[action];
      int k = 0;

      while (k <= length) {
         String tick = String.format("%03d", k);
         s.add(t + 21 + k, mc -> s.shot(mc, "pirates_reload_" + name + "_" + tick));
         k += every;
      }

      s.add(t + 21 + length + 20, mc -> s.shot(mc, "pirates_reload_" + name + "_after"));
      s.add(t + 21 + length + 25, mc -> s.server(mc, p -> clear()));
      return t + 21 + length + 30;
   }

   private static <T extends PirateFighter> T spawn(ServerPlayer p, EntityType<T> type, double x, double z, float yaw, ItemStack hand) {
      ServerLevel level = p.serverLevel();
      T mob = (T)type.create(level);
      if (mob == null) {
         throw new IllegalStateException("no " + type);
      } else {
         mob.moveTo(x, p.getY(), z, yaw, 0.0F);
         mob.setYHeadRot(yaw);
         mob.setYBodyRot(yaw);
         mob.setNoAi(true);
         mob.setPersistenceRequired();
         mob.setItemSlot(EquipmentSlot.MAINHAND, hand);
         level.addFreshEntity(mob);
         SPAWNED.add(mob);
         return mob;
      }
   }

   private static void clear() {
      SPAWNED.forEach(Entity::discard);
      SPAWNED.clear();
   }
}
