package com.devilfruits.fruits.horo_horo.client.dev;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureRig;
import com.devilfruits.client.dev.CaptureStage;
import com.devilfruits.client.dev.VfxCapture;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.fruits.horo_horo.HoroHoro;
import com.devilfruits.fruits.horo_horo.client.SwallowedView;
import com.devilfruits.fruits.horo_horo.client.vfx.HoroCinematic;
import com.devilfruits.fruits.horo_horo.entity.GhostBody;
import com.devilfruits.fruits.horo_horo.ghost.GhostForm;
import com.devilfruits.fruits.horo_horo.negative.Negative;
import com.devilfruits.mobs.pirate.entity.PirateFighter;
import com.devilfruits.registry.DFAttachments;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class HoroCapture extends CaptureStage {
   private static final int RATE = 8;
   private static final int CLIP_RATE = 5;
   private static final String NEGATIVE = "devilfruits:horo_horo/negative_hollow";
   private static final String MINI = "devilfruits:horo_horo/mini_hollow";
   private static final String GHOST = "devilfruits:horo_horo/ghost_form";
   private static final String SPECIAL = "devilfruits:horo_horo/special_hollow";
   private final HoroCapture.Stage stage;

   public HoroCapture(String prefix) {
      this(prefix, HoroCapture.Stage.M4);
   }

   public HoroCapture(String prefix, HoroCapture.Stage stage) {
      super(prefix);
      this.stage = stage;
   }

   @Override
   protected ResourceLocation fruit() {
      return HoroHoro.ID;
   }

   @Override
   protected void onReset(ServerPlayer p) {
      command(p, "time set 6000");
      command(p, "gamerule doMobSpawning false");
      double y = p.getY();
      p.serverLevel()
         .getEntitiesOfClass(Monster.class, new AABB(-40.0, y - 10.0, -50.0, 40.0, y + 30.0, 30.0), e -> !(e instanceof PirateFighter))
         .forEach(Entity::discard);
      PlayerFruitData data = DFAttachments.data(p);
      data.clearCooldowns();
      data.setEnergy(1000.0F);
      data.setMastery(10);
      p.removeEffect(Negative.NEGATIVE);
      p.removeEffect(MobEffects.WEAKNESS);
      p.removeEffect(MobEffects.DIG_SLOWDOWN);
      p.removeData(Negative.VIEW);
      if (this.stage == HoroCapture.Stage.M6) {
         refloor(p);
      }

      if (GhostForm.active(p)) {
         GhostForm.finish(p, true);
      }

      Minecraft.getInstance().execute(() -> {
         for (int i = 0; i < 64; i++) {
            HoroCinematic c = VfxDirector.find(HoroCinematic.class, k -> !k.isDone());
            if (c == null) {
               break;
            }

            c.finish();
         }

         VfxParticles.clear();
      });
   }

   private static Consumer<Minecraft> onDummy(double dist, float yaw, float pitch, double lift) {
      return mc -> {
         if (mc.level != null) {
            for (Entity e : mc.level.entitiesForRendering()) {
               if (e instanceof PirateFighter) {
                  mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                  CaptureRig.follow(e, dist, yaw, pitch, lift);
                  return;
               }
            }
         }
      };
   }

   private static Consumer<Minecraft> firstPersonGui() {
      return mc -> {
         CaptureRig.off();
         mc.options.setCameraType(CameraType.FIRST_PERSON);
         mc.options.hideGui = false;
      };
   }

   private static Consumer<Minecraft> guiOff() {
      return mc -> mc.options.hideGui = true;
   }

   private static void glide(AutoTest s, int from, int ticks, double x, double y, double z) {
      for (int k = 0; k < ticks; k++) {
         s.add(from + k, mc -> {
            if (mc.player != null) {
               mc.player.setDeltaMovement(x, y, z);
            }
         });
      }
   }

   private static Consumer<ServerPlayer> possess() {
      return p -> {
         GhostBody body = GhostForm.bodyEntity(p);
         if (body != null) {
            GhostForm.requestPossession(p, body);
         }
      };
   }

   private int m5(AutoTest s, int t) {
      t = this.take(s, t, 8, none(), 24, p -> castAbility(p, "devilfruits:horo_horo/ghost_form", 1), rig(4.6, 140.0F, 8.0F, 0.6), 30, c -> {
         for (int k : new int[]{4, 6, 8, 11, 15, 24}) {
            this.shot(s, c + k, "exit_" + k);
         }
      });
      t = this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:horo_horo/ghost_form", 1), rig(7.5, 100.0F, 10.0F, 0.8), 60, c -> {
         glide(s, c + 12, 22, 0.0, 0.08, -0.42);

         for (int k : new int[]{16, 22, 28, 36, 46, 58}) {
            this.shot(s, c + k, "fly_" + k);
         }
      });
      t = this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:horo_horo/ghost_form", 1), rig(9.0, 20.0F, 12.0F, 1.0), 40, c -> {
         s.add(c + 6, mc -> s.server(mc, p -> p.teleportTo(p.serverLevel(), p.getX(), p.getY() + 4.0, p.getZ() - 54.0, 180.0F, 10.0F)));

         for (int k : new int[]{14, 24, 34}) {
            this.shot(s, c + k, "leash_" + k);
         }
      });
      t = this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:horo_horo/ghost_form", 1), rig(4.8, 125.0F, 8.0F, 0.6), 64, c -> {
         glide(s, c + 8, 10, 0.0, 0.05, -0.3);
         glide(s, c + 22, 10, 0.0, -0.05, 0.28);
         s.add(c + 34, mc -> s.server(mc, possess()));

         for (int k : new int[]{20, 33, 36, 39, 42, 45, 48, 54, 62}) {
            this.shot(s, c + k, "possess_" + k);
         }
      });
      t = this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:horo_horo/ghost_form", 1), firstPersonGui(), 44, c -> {
         glide(s, c + 6, 14, 0.0, 0.0, -0.4);
         this.shot(s, c + 24, "view_24");
         s.add(c + 26, mc -> {
            if (mc.player != null) {
               mc.player.setYRot(0.0F);
               mc.player.setXRot(25.0F);
            }
         });
         glide(s, c + 27, 12, 0.0, 0.0, 0.42);
         this.shot(s, c + 42, "view_42");
         s.add(c + 43, guiOff());
      });
      return this.take(s, t, 5, none(), 0, p -> castAbility(p, "devilfruits:horo_horo/ghost_form", 1), rig(8.0, 115.0F, 10.0F, 0.8), 120, c -> {
         s.add(c - 1, mc -> VfxCapture.clip(this.prefix + "clip_ghost", 160, 0.6666666666666666));
         glide(s, c + 14, 24, 0.0, 0.07, -0.4);
         glide(s, c + 50, 26, 0.0, -0.06, 0.36);
         s.add(c + 86, mc -> s.server(mc, possess()));
      });
   }

   private static void refloor(ServerPlayer p) {
      ServerLevel level = p.serverLevel();
      int y = (int)Math.floor(p.getY());

      for (int x = -12; x <= 12; x++) {
         for (int z = -24; z <= 4; z++) {
            for (int dy = -4; dy <= 8; dy++) {
               BlockPos at = new BlockPos(x, y + dy, z);
               boolean stage = Math.abs(x) <= 7 && z >= -16 && z <= 2;
               BlockState state = dy >= 0
                  ? Blocks.AIR.defaultBlockState()
                  : (
                     dy == -1 && stage
                        ? (Math.abs(x) != 7 && z != -16 && z != 2 ? Blocks.STONE_BRICKS : Blocks.POLISHED_ANDESITE).defaultBlockState()
                        : (dy == -1 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState())
                  );
               if (level.getBlockState(at) != state) {
                  level.setBlock(at, state, 2);
               }
            }
         }
      }
   }

   private int m6(AutoTest s, int t) {
      t = this.take(s, t, 8, none(), 24, p -> castAbility(p, "devilfruits:horo_horo/special_hollow", 1), rig(9.0, 150.0F, 6.0F, 2.2), 40, c -> {
         for (int k : new int[]{3, 8, 13, 18, 24, 30}) {
            this.shot(s, c + k, "rise_" + k);
         }
      });
      t = this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:horo_horo/special_hollow", 1), rig(16.0, 95.0F, 12.0F, 2.0), 140, c -> {
         for (int k : new int[]{34, 42, 50, 56, 64, 74, 84, 92, 96, 98, 100, 102, 105, 110, 120, 134}) {
            this.shot(s, c + k, "blast_" + k);
         }
      });
      t = this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:horo_horo/special_hollow", 1), onDummy(8.0, 140.0F, 8.0F, 1.5), 110, c -> {
         for (int k : new int[]{52, 62, 72, 82, 90, 96}) {
            this.shot(s, c + k, "swell_" + k);
         }
      });
      t = this.take(
         s,
         t,
         8,
         p -> command(p, "time set 18000"),
         24,
         p -> castAbility(p, "devilfruits:horo_horo/special_hollow", 1),
         rig(18.0, 110.0F, 10.0F, 2.0),
         130,
         c -> {
            for (int k : new int[]{60, 94, 98, 100, 103, 108, 118}) {
               this.shot(s, c + k, "night_" + k);
            }
         }
      );
      t = this.take(s, t, 8, none(), 0, p -> {
      }, firstPersonGui(), 64, c -> {
         s.add(c, mc -> SwallowedView.begin(50));

         for (int k : new int[]{6, 26, 46}) {
            this.shot(s, c + k, "inside_" + k);
         }

         s.add(c + 62, guiOff());
      });
      return this.take(
         s,
         t,
         5,
         none(),
         0,
         p -> castAbility(p, "devilfruits:horo_horo/special_hollow", 1),
         rig(16.0, 100.0F, 11.0F, 2.0),
         140,
         c -> s.add(c - 1, mc -> VfxCapture.clip(this.prefix + "clip_special", 210, 0.6666666666666666))
      );
   }

   @Override
   protected int takes(AutoTest s, int t) {
      if (this.stage == HoroCapture.Stage.M5) {
         return this.m5(s, t);
      } else if (this.stage == HoroCapture.Stage.M6) {
         return this.m6(s, t);
      } else {
         t = this.take(
            s,
            t,
            8,
            p -> command(p, "time set 18000"),
            0,
            p -> castAbility(p, "devilfruits:horo_horo/negative_hollow", 1),
            rig(3.2, 35.0F, 4.0F, 0.5),
            22,
            c -> {
               for (int k : new int[]{3, 5, 7, 9, 11, 13}) {
                  this.shot(s, c + k, "neg_emerge_" + k);
               }
            }
         );
         t = this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:horo_horo/negative_hollow", 1), rig(9.5, 95.0F, 8.0F, 1.0), 44, c -> {
            for (int k : new int[]{6, 8, 10, 12, 13, 15, 18, 22, 28}) {
               this.shot(s, c + k, "neg_fly_" + k);
            }
         });
         t = this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:horo_horo/negative_hollow", 1), onDummy(4.2, 125.0F, 10.0F, 0.6), 60, c -> {
            for (int k : new int[]{14, 20, 30, 45, 58}) {
               this.shot(s, c + k, "neg_look_" + k);
            }
         });
         t = this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:horo_horo/mini_hollow", 1), rig(8.5, 100.0F, 10.0F, 1.0), 30, c -> {
            for (int k : new int[]{2, 4, 6, 8, 11, 14, 20}) {
               this.shot(s, c + k, "mini_fly_" + k);
            }

            s.add(c + 28, mc -> s.server(mc, p -> castAbility(p, "devilfruits:horo_horo/mini_hollow", 1)));
         });
         t = this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:horo_horo/mini_hollow", 1), onDummy(3.8, 150.0F, 6.0F, 0.5), 60, c -> {
            s.add(c + 34, mc -> s.server(mc, p -> castAbility(p, "devilfruits:horo_horo/mini_hollow", 1)));

            for (int k : new int[]{16, 24, 32, 36, 38, 39, 40, 41, 42, 44, 48}) {
               this.shot(s, c + k, "mini_pop_" + k);
            }
         });
         t = this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:horo_horo/mini_hollow", 1), rig(4.0, 140.0F, 6.0F, 0.6), 50, c -> {
            s.add(c + 26, mc -> s.server(mc, p -> castAbility(p, "devilfruits:horo_horo/mini_hollow", 1)));

            for (int k : new int[]{27, 29, 31, 32, 33}) {
               this.shot(s, c + k, "clap_" + k);
            }
         });
         t = this.take(s, t, 8, none(), 0, p -> Negative.apply(p, 200, null), rig(3.6, 150.0F, 10.0F, 0.3), 40, c -> {
            for (int k : new int[]{4, 12, 30}) {
               this.shot(s, c + k, "victim_" + k);
            }
         });
         t = this.take(s, t, 8, none(), 0, p -> Negative.apply(p, 200, null), firstPersonGui(), 30, c -> {
            this.shot(s, c + 20, "victim_fp_20");
            s.add(c + 26, guiOff());
         });
         return this.take(s, t, 5, none(), 0, p -> castAbility(p, "devilfruits:horo_horo/negative_hollow", 1), rig(8.5, 105.0F, 10.0F, 1.0), 130, c -> {
            s.add(c - 1, mc -> VfxCapture.clip(this.prefix + "clip_skills", 150, 0.6666666666666666));
            s.add(c + 45, mc -> s.server(mc, p -> {
                  DFAttachments.data(p).clearCooldowns();
                  castAbility(p, "devilfruits:horo_horo/mini_hollow", 1);
               }));
            s.add(c + 80, mc -> s.server(mc, p -> castAbility(p, "devilfruits:horo_horo/mini_hollow", 1)));
         });
      }
   }

   public static enum Stage {
      M4,
      M5,
      M6;
   }
}
