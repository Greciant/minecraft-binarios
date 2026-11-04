package com.devilfruits.fruits.gomu_gomu.client.dev;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureRig;
import com.devilfruits.client.dev.CaptureStage;
import com.devilfruits.client.hud.GearWheelScreen;
import com.devilfruits.fruits.gomu_gomu.GomuGomu;
import com.devilfruits.mobs.pirate.entity.MusketBall;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GomuCapture extends CaptureStage {
   private static final int RATE = 8;
   private final String stage;

   public GomuCapture(String stage) {
      super("vfx_gomu_" + stage + "_");
      this.stage = stage;
   }

   @Override
   protected ResourceLocation fruit() {
      return GomuGomu.ID;
   }

   @Override
   protected void setup(ServerPlayer p) {
      command(p, "time set 6000");
      command(p, "devilfruit setmastery @s 10");
   }

   @Override
   protected void onReset(ServerPlayer p) {
      command(p, "gamemode creative");
      command(p, "clear @s");
      command(p, "kill @e[type=item]");
   }

   @Override
   protected int takes(AutoTest s, int t) {
      if (!"m2".equals(this.stage)) {
         t = this.take(s, t, 20, none(), 24, p -> castAbility(p, "devilfruits:gomu_gomu/pistol", 0), rig(6.0, 90.0F, 6.0F, 0.4), 30, c -> {
         });
      }

      String var3 = this.stage;

      return switch (var3) {
         case "m3" -> this.strikes(s, t);
         case "m4" -> this.rocket(s, t);
         case "m5" -> this.axe(s, t);
         case "m6" -> this.gatling(s, t);
         default -> this.m2(s, t);
      };
   }

   private static void pillar(AutoTest s, ServerPlayer p, double z, boolean build) {
      int y = s.origin().getY();
      command(p, "fill 0 " + y + " " + (int)Math.floor(z) + " 0 " + (y + 4) + " " + (int)Math.floor(z) + (build ? " minecraft:stone" : " minecraft:air"));
   }

   private int rocket(AutoTest s, int t) {
      t = this.take(s, t, 8, p -> {
         dummyTo(s, p, -30.0);
         pillar(s, p, -15.0, true);
         command(p, "tp @s 0.5 " + s.origin().getY() + " -2.5 180 -8");
      }, 6, p -> castAbility(p, "devilfruits:gomu_gomu/rocket", 0), rig(9.0, 90.0F, 6.0F, 0.8), 40, c -> {
         for (int b : new int[]{3, 6, 8, 10, 12, 14, 16, 19, 23, 28}) {
            this.shot(s, c + b, String.format("rocket_wall_%02d", b));
         }
      });
      t = this.take(s, t, 8, p -> {
         pillar(s, p, -15.0, false);
         dummyTo(s, p, -12.5);
         command(p, "tp @s 0.5 " + s.origin().getY() + " -2.5 180 -2");
      }, 6, p -> castAbility(p, "devilfruits:gomu_gomu/rocket", 0), rig(8.0, 90.0F, 6.0F, 0.6), 34, c -> {
         for (int b : new int[]{7, 9, 11, 13, 16}) {
            this.shot(s, c + b, String.format("rocket_body_%02d", b));
         }
      });
      return this.take(s, t, 8, p -> {
         dummyTo(s, p, -30.0);
         pillar(s, p, -15.0, true);
         command(p, "tp @s 0.5 " + s.origin().getY() + " -2.5 180 -8");
      }, 6, p -> castAbility(p, "devilfruits:gomu_gomu/rocket", 0), firstPerson(), 30, c -> {
         for (int b : new int[]{8, 11, 14}) {
            this.shot(s, c + b, String.format("rocket_fp_%02d", b));
         }
      });
   }

   private int axe(AutoTest s, int t) {
      t = this.take(s, t, 8, p -> {
         pillar(s, p, -15.0, false);
         dummyTo(s, p, -9.5);
         command(p, "tp @s 0.5 " + s.origin().getY() + " -2.5 180 30");
      }, 6, p -> castAbility(p, "devilfruits:gomu_gomu/axe", 0), rig(9.0, 90.0F, 6.0F, 2.0), 30, c -> {
         for (int b : new int[]{3, 6, 8, 11, 12, 13, 14, 16, 19, 23}) {
            this.shot(s, c + b, String.format("axe_side_%02d", b));
         }
      });
      return this.take(s, t, 8, p -> {
         dummyTo(s, p, -9.5);
         command(p, "tp @s 0.5 " + s.origin().getY() + " -2.5 180 30");
      }, 6, p -> castAbility(p, "devilfruits:gomu_gomu/axe", 0), rig(11.0, 35.0F, 28.0F, 0.0), 30, c -> {
         for (int b : new int[]{9, 13, 14, 15, 18}) {
            this.shot(s, c + b, String.format("axe_wide_%02d", b));
         }
      });
   }

   private int gatling(AutoTest s, int t) {
      t = this.take(s, t, 8, p -> dummyTo(s, p, -6.5), 6, p -> castAbility(p, "devilfruits:gomu_gomu/gatling", 40), rig(6.0, 70.0F, 6.0F, 0.5), 56, c -> {
         for (int b : new int[]{4, 8, 12, 16, 24, 32, 41, 43, 46}) {
            this.shot(s, c + b, String.format("gatling_side_%02d", b));
         }
      });
      t = this.take(s, t, 8, p -> dummyTo(s, p, -6.5), 6, p -> castAbility(p, "devilfruits:gomu_gomu/gatling", 30), rig(4.5, 20.0F, 8.0F, 0.6), 46, c -> {
         for (int b : new int[]{10, 20, 31, 33}) {
            this.shot(s, c + b, String.format("gatling_front_%02d", b));
         }
      });
      return this.take(s, t, 8, p -> dummyTo(s, p, -6.5), 6, p -> castAbility(p, "devilfruits:gomu_gomu/gatling", 30), firstPerson(), 44, c -> {
         for (int b : new int[]{10, 20}) {
            this.shot(s, c + b, String.format("gatling_fp_%02d", b));
         }
      });
   }

   private int strikes(AutoTest s, int t) {
      t = this.strike(s, t, "pistol", -10.5, rig(6.5, 90.0F, 6.0F, 0.4), new int[]{3, 6, 8, 9, 10, 12, 15}, "side");
      t = this.strike(s, t, "pistol", -10.5, firstPerson(), new int[]{9, 11, 12, 14}, "fp");
      t = this.strike(s, t, "bazooka", -8.5, rig(6.0, 90.0F, 6.0F, 0.4), new int[]{4, 8, 10, 11, 13, 17}, "side");
      t = this.strike(s, t, "bazooka", -8.5, rig(4.0, 25.0F, 8.0F, 0.6), new int[]{10, 11, 12}, "front");
      t = this.strike(s, t, "rifle", -10.5, rig(6.5, 90.0F, 6.0F, 0.4), new int[]{4, 8, 10, 11, 12, 14, 17}, "side");
      t = this.strike(s, t, "rifle", -10.5, rig(3.2, 60.0F, 4.0F, 0.5), new int[]{7, 9, 10, 11}, "close");
      t = this.wall(s, t, "bazooka", "minecraft:oak_planks", "wood", new int[]{8, 10, 11, 13, 17});
      return this.wall(s, t, "rifle", "minecraft:stone", "stone", new int[]{8, 10, 11, 12, 14});
   }

   private int wall(AutoTest s, int t, String ability, String block, String name, int[] beats) {
      return this.take(s, t, 8, p -> {
         dummyTo(s, p, -30.0);
         int y = s.origin().getY();
         int z = (int)Math.floor(-6.5);
         command(p, "fill 5 " + y + " " + z + " 7 " + (y + 2) + " " + z + " " + block);
         command(p, "tp @s 6.5 " + y + " -2.5 180 0");
         command(p, "gamerule mobGriefing true");
      }, 4, p -> castAbility(p, "devilfruits:gomu_gomu/" + ability, 0), rig(9.0, 12.0F, 6.0F, 0.6), 26, c -> {
         for (int b : beats) {
            this.shot(s, c + b, ability + "_" + name + String.format("_%02d", b));
         }

         s.add(c + 25, mc -> s.server(mc, p -> {
               int y = s.origin().getY();
               int z = (int)Math.floor(-6.5);
               command(p, "fill 5 " + y + " " + z + " 7 " + (y + 2) + " " + z + " minecraft:air");
               command(p, "gamerule mobGriefing false");
            }));
      });
   }

   private int strike(AutoTest s, int t, String ability, double dummyZ, Consumer<Minecraft> camera, int[] beats, String view) {
      return this.take(s, t, 8, p -> dummyTo(s, p, dummyZ), 4, p -> castAbility(p, "devilfruits:gomu_gomu/" + ability, 0), camera, 26, c -> {
         for (int b : beats) {
            this.shot(s, c + b, ability + "_" + view + String.format("_%02d", b));
         }
      });
   }

   private int m2(AutoTest s, int t) {
      t = this.take(s, t, 20, p -> {
         command(p, "tp @s 0.5 " + s.origin().getY() + " -2.5 180 55");
         command(p, "summon item 0.5 " + s.origin().getY() + " -3.6 {Item:{id:\"devilfruits:gomu_gomu_no_mi\",count:1},PickupDelay:32767}");
      }, 20, none(), firstPerson(), 6, c -> this.shot(s, c + 3, "fruit_ground"));
      t = this.take(
         s,
         t,
         20,
         p -> command(p, "item replace entity @s weapon.mainhand with devilfruits:gomu_gomu_no_mi"),
         4,
         none(),
         rig(2.2, 30.0F, 10.0F, 0.3),
         6,
         c -> this.shot(s, c + 3, "fruit_tp")
      );
      t = this.wheel(s, t, 0, -1, "wheel_g1");
      t = this.wheel(s, t, 3, -1, "wheel_g4");
      t = this.wheel(s, t, 3, 0, "wheel_bounce");
      t = this.dent(s, t, new Vec3(0.0, 1.62, 0.0), rig(1.7, 40.0F, 4.0F, 0.6), "head");
      t = this.dent(s, t, new Vec3(0.05, 1.05, 0.0), rig(1.9, 45.0F, 6.0F, 0.1), "body");
      t = this.dent(s, t, new Vec3(0.26, 1.25, 0.0), rig(1.8, -40.0F, 6.0F, 0.2), "arm");
      t = this.dent(s, t, new Vec3(0.12, 0.45, 0.0), rig(1.9, 50.0F, 2.0F, -0.5), "leg");
      return this.landing(s, t);
   }

   private int wheel(AutoTest s, int t, int gear, int sub, String name) {
      s.add(t, mc -> {
         CaptureRig.off();
         mc.options.setCameraType(CameraType.FIRST_PERSON);
         mc.options.hideGui = false;
         GearWheelScreen.capturePointer = new int[]{gear, sub};
         mc.setScreen(new GearWheelScreen(GomuGomu.fruit(), false));
      });
      s.add(t + 6, mc -> s.shot(mc, this.prefix + name));
      s.add(t + 8, mc -> {
         GearWheelScreen.capturePointer = null;
         mc.setScreen(null);
         mc.options.hideGui = true;
      });
      return t + 10;
   }

   private int dent(AutoTest s, int t, Vec3 at, Consumer<Minecraft> camera, String part) {
      return this.take(
         s,
         t,
         8,
         none(),
         4,
         p -> {
            LivingEntity dummy = (LivingEntity)p.serverLevel()
               .getEntitiesOfClass(LivingEntity.class, new AABB(p.blockPosition()).inflate(20.0), e -> e != p)
               .stream()
               .findFirst()
               .orElse(null);
            if (dummy != null) {
               Vec3 muzzle = dummy.getEyePosition().add(0.0, -0.2, 0.6);
               Vec3 target = p.position().add(at);
               MusketBall.fire(dummy, muzzle, target.subtract(muzzle), 3.0F, 0.0F, 6.0F, 0.25F, false);
            }
         },
         camera,
         40,
         c -> {
            this.shot(s, c + 12, "dent_" + part + "_in");
            this.shot(s, c + 24, "dent_" + part + "_deep");
            this.shot(s, c + 31, "dent_" + part + "_out");
            this.shot(s, c + 33, "dent_" + part + "_bulge");
         }
      );
   }

   private int landing(AutoTest s, int t) {
      return this.take(
         s, t, 5, p -> command(p, "gamemode survival"), 4, p -> p.teleportTo(p.getX(), p.getY() + 12.0, p.getZ()), rig(3.0, 70.0F, 4.0F, 0.0), 44, c -> {
            for (int i = 14; i < 40; i++) {
               this.shot(s, c + i, String.format("land_%02d", i));
            }
         }
      );
   }
}
