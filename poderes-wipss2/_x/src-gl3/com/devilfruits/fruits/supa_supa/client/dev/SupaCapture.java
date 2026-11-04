package com.devilfruits.fruits.supa_supa.client.dev;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureRig;
import com.devilfruits.client.dev.CaptureStage;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.PhysicsStructure;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.fruits.supa_supa.BladeLimbs;
import com.devilfruits.fruits.supa_supa.SupaAbilities;
import com.devilfruits.fruits.supa_supa.SupaSupa;
import com.devilfruits.registry.DFAttachments;
import com.mojang.authlib.GameProfile;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SupaCapture extends CaptureStage {
   private static final int RATE = 8;
   private static final int CLIP_RATE = 5;
   private static final int FAKE_ID = 9400000;
   private final String stage;
   private static final String SLASH = "devilfruits:supa_supa/super_slash";

   public SupaCapture(String prefix) {
      this(prefix, "look");
   }

   public SupaCapture(String prefix, String stage) {
      super(prefix);
      this.stage = stage;
   }

   @Override
   protected ResourceLocation fruit() {
      return SupaSupa.ID;
   }

   private static Consumer<ServerPlayer> day() {
      return p -> command(p, "time set 6000");
   }

   @Override
   protected void onReset(ServerPlayer p) {
      AbilityManager.cancelAll(p);
      if (DFAttachments.data(p).isToggleActive(SupaAbilities.BLADE_SKATES.id())) {
         AbilityManager.stopToggle(p, SupaAbilities.BLADE_SKATES);
      }

      BladeLimbs.clear(p);
      DFAttachments.data(p).clearCooldowns();
      command(p, "time set 15000");
   }

   private static Consumer<ServerPlayer> blades(BladeLimbs.ArmMode mode, boolean right, boolean left) {
      return p -> BladeLimbs.arms(p, mode, right, left, 0);
   }

   private static Consumer<ServerPlayer> cast(String ability) {
      return p -> castAbility(p, "devilfruits:supa_supa/" + ability, 0);
   }

   private void stills(AutoTest s, int c, String name, int... ticks) {
      for (int i : ticks) {
         this.shot(s, c + i, name + "_" + (i < 10 ? "0" : "") + i);
      }
   }

   @Override
   protected int takes(AutoTest s, int t) {
      String var3 = this.stage;

      return switch (var3) {
         case "m3" -> this.steelAndSkates(s, t);
         case "m4" -> this.spiral(s, t);
         case "m5" -> this.superSlash(s, t);
         case "m6" -> this.sableCut(s, t);
         case "m6b" -> this.aimedCuts(s, t);
         case "hull" -> this.hullWaves(s, t);
         default -> this.blades(s, t);
      };
   }

   private static void raft(AutoTest s, ServerPlayer p) {
      int y = s.origin().getY();
      if (!p.level().getBlockState(new BlockPos(0, y - 3, -10)).is(Blocks.WATER)) {
         command(p, String.format("fill -6 %d -16 6 %d -4 minecraft:stone", y - 4, y - 1));
         command(p, String.format("fill -5 %d -15 5 %d -5 minecraft:water", y - 3, y - 1));
      }

      command(p, String.format("fill -5 %d -15 5 %d -5 minecraft:water replace minecraft:air", y - 3, y - 1));
      command(p, String.format("fill -2 %d -13 2 %d -7 minecraft:oak_planks", y - 1, y - 1));
      command(p, String.format("fill -2 %d -13 -2 %d -7 minecraft:oak_fence", y, y));
      command(p, String.format("fill 2 %d -13 2 %d -7 minecraft:oak_fence", y, y));
      command(p, String.format("fill -2 %d -7 2 %d -7 minecraft:oak_fence", y, y));
      command(p, String.format("fill -2 %d -13 2 %d -13 minecraft:oak_fence", y, y));
      command(p, String.format("fill -2 %d -10 2 %d -10 minecraft:white_wool", y + 1, y + 3));
      command(p, String.format("fill 0 %d -10 0 %d -10 minecraft:oak_log", y, y + 4));
      command(p, String.format("sable assemble area -2 %d -13 2 %d -7", y - 1, y + 4));
   }

   private static void airship(AutoTest s, ServerPlayer p, float roll) {
      int y = s.origin().getY();
      command(p, String.format("fill -2 %d -13 2 %d -7 aeronautics:levitite", y + 2, y + 2));
      command(p, String.format("fill -2 %d -13 2 %d -7 minecraft:spruce_planks", y + 3, y + 3));
      command(p, String.format("fill -1 %d -12 1 %d -8 minecraft:air", y + 3, y + 3));
      command(p, String.format("fill -2 %d -10 2 %d -10 minecraft:white_wool", y + 5, y + 7));
      command(p, String.format("fill 0 %d -10 0 %d -10 minecraft:spruce_log", y + 3, y + 8));
      command(p, String.format("sable assemble area -2 %d -13 2 %d -7", y + 2, y + 8));
      if (roll != 0.0F) {
         command(p, String.format(Locale.ROOT, "sable physics rotation @n set axis 0 0 1 %.1f", roll));
      }
   }

   private static void push(ServerPlayer p, Vec3 dv) {
      AABB area = p.getBoundingBox().inflate(40.0);

      for (PhysicsStructure st : PhysicsWorld.bridge().structuresIn(p.level(), area)) {
         Vec3 c = st.centerOfMassWorld();
         st.applyImpulse(c != null ? c : st.worldBounds().getCenter(), dv.scale(st.mass()));
      }
   }

   private static Consumer<ServerPlayer> later(Consumer<ServerPlayer> build) {
      return p -> ServerScheduler.later(p.serverLevel(), 10, () -> build.accept(p));
   }

   private static Consumer<ServerPlayer> clear(AutoTest s) {
      return p -> {
         dummyTo(s, p, 1.5);
         command(p, "gamerule mobGriefing true");
         command(p, "sable remove @e");
      };
   }

   private static void aimAt(ServerPlayer p, Vec3 target) {
      Vec3 d = target.subtract(p.getEyePosition());
      float yaw = (float)Math.toDegrees(Math.atan2(-d.x, d.z));
      float pitch = (float)(-Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z))));
      p.teleportTo(p.serverLevel(), p.getX(), p.getY(), p.getZ(), yaw, pitch);
   }

   private static void cube(AutoTest s, ServerPlayer p) {
      int y = s.origin().getY();
      command(p, String.format("fill -2 %d -14 3 %d -9 minecraft:oak_planks", y, y + 5));
      command(p, String.format("sable assemble area -2 %d -14 3 %d -9", y, y + 5));
   }

   @Nullable
   private static Vec3 heaviest(ServerPlayer p) {
      PhysicsStructure best = null;

      for (PhysicsStructure st : PhysicsWorld.bridge().structuresIn(p.level(), p.getBoundingBox().inflate(40.0))) {
         if (best == null || st.mass() > best.mass()) {
            best = st;
         }
      }

      if (best == null) {
         return null;
      } else {
         Vec3 c = best.centerOfMassWorld();
         return c != null ? c : best.worldBounds().getCenter();
      }
   }

   private int aimedCuts(AutoTest s, int t) {
      t += 40;
      Consumer<ServerPlayer> high = clear(s).andThen(later(p -> {
         int y = s.origin().getY();
         command(p, String.format("fill -2 %d -14 2 %d -8 aeronautics:levitite", y + 8, y + 8));
         command(p, String.format("fill -2 %d -14 2 %d -8 minecraft:spruce_planks", y + 9, y + 9));
         command(p, String.format("fill -1 %d -13 1 %d -9 minecraft:air", y + 9, y + 9));
         command(p, String.format("fill 0 %d -11 0 %d -11 minecraft:spruce_log", y + 9, y + 13));
         command(p, String.format("sable assemble area -2 %d -14 2 %d -8", y + 8, y + 13));
      }));
      Consumer<ServerPlayer> look = p -> {
         aimAt(p, new Vec3(0.5, (double)s.origin().getY() + 8.5, -11.0));
         castAbility(p, "devilfruits:supa_supa/super_slash", 32);
      };
      t = this.take(s, t, 8, high, 40, look, rig(9.0, 215.0F, -12.0F, 1.5), 165, c -> this.stills(s, c, "aim_up", 20, 36, 40, 48, 60, 75, 96, 110, 140));
      Consumer<ServerPlayer> cube = clear(s).andThen(later(p -> cube(s, p)));
      Consumer<ServerPlayer> first = p -> castAbility(p, "devilfruits:supa_supa/super_slash", 32);
      return this.take(s, t, 8, cube, 40, first, rig(10.0, 205.0F, 22.0F, 1.6), 300, c -> {
         this.stills(s, c, "cube", 40, 70, 96, 110, 140);
         s.add(c + 145, mc -> s.server(mc, p -> {
               DFAttachments.data(p).clearCooldowns();
               DFAttachments.data(p).setEnergy(1000.0F);
               Vec3 half = heaviest(p);
               if (half != null) {
                  aimAt(p, half);
               }

               castAbility(p, "devilfruits:supa_supa/super_slash", 32);
            }));
         this.stills(s, c, "cube", 180, 190, 210, 240, 260, 290);
      });
   }

   private int hullWaves(AutoTest s, int t) {
      t += 40;
      Consumer<ServerPlayer> deck = clear(s).andThen(later(p -> {
         int y = s.origin().getY();
         if (!p.level().getBlockState(new BlockPos(0, y - 3, -10)).is(Blocks.WATER)) {
            command(p, String.format("fill -6 %d -16 6 %d -4 minecraft:stone", y - 4, y - 1));
            command(p, String.format("fill -5 %d -15 5 %d -5 minecraft:water", y - 3, y - 1));
         }

         command(p, String.format("fill -3 %d -13 3 %d -7 minecraft:spruce_planks", y - 1, y - 1));
         command(p, String.format("sable assemble area -3 %d -13 3 %d -7", y - 1, y - 1));
      }));
      Consumer<ServerPlayer> blow = p -> {
         for (PhysicsStructure st : PhysicsWorld.bridge().structuresIn(p.level(), p.getBoundingBox().inflate(30.0))) {
            AABB b = st.localBounds();
            if (b != null) {
               BlockPos mid = BlockPos.containing(b.getCenter().x, b.maxY - 0.5, b.getCenter().z);
               BlockImpactService.decal(p.serverLevel(), st, mid, st.dirToLocal(new Vec3(0.0, 1.0, 0.0)), BlockImpactService.Decal.CRACK, 2.5F);
            }
         }
      };
      t = this.take(s, t, 8, deck, 60, blow, rig(6.0, 195.0F, 38.0F, 1.0), 30, c -> this.stills(s, c, "hull", 1, 2, 3, 5, 7, 9, 12, 16));
      return this.take(s, t, 5, deck, 60, blow, rig(6.0, 165.0F, 30.0F, 1.0), 30, c -> this.clip(s, c - 2, 45));
   }

   private int sableCut(AutoTest s, int t) {
      t += 40;
      Consumer<ServerPlayer> clear = clear(s);
      Consumer<ServerPlayer> raft = clear.andThen(later(p -> raft(s, p)));
      Consumer<ServerPlayer> tilted = clear.andThen(later(p -> airship(s, p, 18.0F)));
      Consumer<ServerPlayer> level = clear.andThen(later(p -> airship(s, p, 0.0F)));
      Consumer<ServerPlayer> slash = p -> castAbility(p, "devilfruits:supa_supa/super_slash", 32);
      t = this.take(
         s,
         t,
         8,
         raft,
         40,
         slash,
         rig(7.0, 200.0F, 24.0F, 1.4),
         190,
         c -> this.stills(s, c, "raft_back", 36, 40, 50, 62, 68, 75, 85, 93, 96, 100, 110, 130, 155)
      );
      t = this.take(s, t, 8, raft, 40, slash, rig(12.0, 165.0F, 42.0F, 1.0), 165, c -> this.stills(s, c, "raft_high", 40, 55, 68, 85, 96, 105, 130, 155));
      t = this.take(
         s, t, 8, tilted, 40, slash, rig(8.0, 205.0F, 10.0F, 2.2), 190, c -> this.stills(s, c, "air_tilt", 36, 42, 55, 65, 75, 85, 96, 105, 130, 155)
      );
      t = this.take(s, t, 8, level, 40, slash, rig(10.0, 140.0F, 8.0F, 2.0), 190, c -> {
         for (int k = 20; k < 140; k += 4) {
            s.add(c + k, mc -> s.server(mc, p -> push(p, new Vec3(0.35, 0.0, 0.0))));
         }

         this.stills(s, c, "air_move", 40, 55, 65, 75, 85, 96, 105, 130, 155);
      });
      return this.take(s, t, 5, tilted, 40, slash, rig(9.0, 150.0F, 12.0F, 1.8), 170, c -> this.clip(s, c + 26, 210));
   }

   private int superSlash(AutoTest s, int t) {
      t += 40;
      Consumer<ServerPlayer> wall = p -> {
         dummyTo(s, p, -7.5);
         int y = s.origin().getY();
         command(p, String.format("fill -3 %d -13 3 %d -13 minecraft:stone_bricks", y, y + 5));
         command(p, "gamerule mobGriefing true");
      };
      t = this.take(s, t, 8, wall, 4, p -> castAbility(p, "devilfruits:supa_supa/super_slash", 32), rig(3.4, 25.0F, 6.0F, 0.6), 60, c -> {
         this.stills(s, c, "slash_charge", 4, 10, 20, 30);
         this.stills(s, c, "slash_release", 33, 34, 35, 36, 38, 41, 45, 52);
      });
      t = this.take(
         s,
         t,
         8,
         wall,
         4,
         p -> castAbility(p, "devilfruits:supa_supa/super_slash", 32),
         rig(7.0, 215.0F, 14.0F, 1.2),
         60,
         c -> this.stills(s, c, "slash_back", 33, 34, 35, 36, 38, 41, 46)
      );
      t = this.take(
         s,
         t,
         8,
         wall,
         4,
         p -> castAbility(p, "devilfruits:supa_supa/super_slash", 32),
         rig(9.0, 120.0F, 8.0F, 1.6),
         60,
         c -> this.stills(s, c, "slash_side", 33, 35, 37, 40, 44)
      );
      return this.take(
         s, t, 5, wall, 4, p -> castAbility(p, "devilfruits:supa_supa/super_slash", 32), rig(6.0, 200.0F, 12.0F, 1.0), 60, c -> this.clip(s, c - 1, 90)
      );
   }

   private int spiral(AutoTest s, int t) {
      t += 40;
      t = this.take(
         s,
         t,
         8,
         day(),
         0,
         p -> castAbility(p, "devilfruits:supa_supa/spiral_hollow", 80),
         rig(2.2, 120.0F, 4.0F, 0.15),
         30,
         c -> this.stills(s, c, "spiral_grow", 1, 2, 3, 4, 6, 9, 14, 20)
      );
      t = this.take(
         s,
         t,
         8,
         p -> {
            dummyTo(s, p, -5.5);
            int y = s.origin().getY();
            command(p, String.format("fill -2 %d -8 3 %d -8 minecraft:stone", y, y + 3));
         },
         4,
         p -> castAbility(p, "devilfruits:supa_supa/spiral_hollow", 80),
         rig(4.2, 215.0F, 12.0F, 0.5),
         84,
         c -> this.stills(s, c, "spiral_drill", 4, 8, 12, 20, 30, 40, 55, 70, 80)
      );
      t = this.take(s, t, 8, day(), 0, p -> castAbility(p, "devilfruits:supa_supa/spiral_hollow", 60), mc -> {
         firstPerson().accept(mc);
         mc.options.hideGui = false;
      }, 40, c -> {
         this.stills(s, c, "spiral_fp", 4, 10, 20);
         s.add(c + 30, mc -> mc.options.hideGui = true);
      });
      return this.take(
         s,
         t,
         5,
         p -> dummyTo(s, p, -5.5),
         2,
         p -> castAbility(p, "devilfruits:supa_supa/spiral_hollow", 80),
         rig(4.0, 140.0F, 8.0F, 0.4),
         90,
         c -> this.clip(s, c - 1, 90)
      );
   }

   private static Consumer<ServerPlayer> skateStage(AutoTest s) {
      return p -> {
         day().accept(p);
         dummyTo(s, p, -14.5);
         int y = s.origin().getY();
         command(p, String.format("fill -3 %d -6 -1 %d -2 minecraft:packed_ice", y - 1, y - 1));
         command(p, String.format("fill 5 %d -6 6 %d -2 minecraft:snow_block", y - 1, y - 1));
         p.teleportTo(p.serverLevel(), -1.5, (double)y, -4.0, 180.0F, 8.0F);
      };
   }

   private static void circle(AutoTest s, int from, int ticks) {
      for (int i = 0; i < ticks; i++) {
         s.add(
            from + i,
            mc -> {
               if (mc.player != null) {
                  double v = Math.sqrt(
                     mc.player.getDeltaMovement().x * mc.player.getDeltaMovement().x + mc.player.getDeltaMovement().z * mc.player.getDeltaMovement().z
                  );
                  mc.player.setYRot(mc.player.getYRot() + (float)Math.toDegrees(Math.max(0.03, v) / 4.0));
               }
            }
         );
      }
   }

   private static void keys(Minecraft mc, boolean forward, boolean sneak) {
      mc.options.keyUp.setDown(forward);
      mc.options.keyShift.setDown(sneak);
   }

   private int steelAndSkates(AutoTest s, int t) {
      t += 40;
      t = this.take(
         s,
         t,
         8,
         day(),
         0,
         p -> castAbility(p, "devilfruits:supa_supa/steel_body", 140),
         rig(3.0, 20.0F, 8.0F, 0.25),
         60,
         c -> {
            this.stills(s, c, "steel_sweep", 2, 4, 6, 8, 10, 14);
            s.add(c + 20, mc -> s.server(mc, p -> command(p, "damage @s 14 minecraft:mob_attack by @e[type=!player,limit=1,sort=nearest]")));
            this.stills(s, c, "steel_clang", 21, 22, 24);
            s.add(
               c + 30,
               mc -> s.server(
                     mc,
                     p -> command(
                           p, String.format(Locale.ROOT, "summon arrow %.2f %.2f %.2f {Motion:[0.0,0.0,1.6d]}", p.getX(), p.getY() + 1.3, p.getZ() - 4.5)
                        )
                  )
            );
            this.stills(s, c, "steel_arrow", 32, 33, 34, 36);
            s.add(c + 44, mc -> s.server(mc, p -> command(p, "damage @s 30 minecraft:mob_attack by @e[type=!player,limit=1,sort=nearest]")));
            this.stills(s, c, "steel_heavy", 45, 46, 48);
         }
      );
      t = this.take(
         s,
         t,
         8,
         skateStage(s),
         4,
         p -> castAbility(p, "devilfruits:supa_supa/blade_skates", 0),
         rig(1.8, 150.0F, 20.0F, -0.55),
         14,
         c -> this.stills(s, c, "skate_unfold", 2, 3, 4, 5, 6, 9)
      );
      t = this.take(s, t, 8, skateStage(s), 4, p -> castAbility(p, "devilfruits:supa_supa/blade_skates", 0), rig(5.0, 200.0F, 22.0F, 0.6), 110, c -> {
         s.add(c + 8, mc -> keys(mc, true, false));
         circle(s, c + 10, 80);
         this.stills(s, c, "skate_ride", 16, 22, 28, 34, 40, 46, 52, 58);
         s.add(c + 62, mc -> KeyMapping.click(mc.options.keyAttack.getKey()));
         this.stills(s, c, "skate_lunge", 63, 64, 66);
         s.add(c + 90, mc -> keys(mc, false, true));
         this.stills(s, c, "skate_brake", 91, 93, 96);
         s.add(c + 104, mc -> keys(mc, false, false));
      });
      t = this.take(s, t, 8, day(), 0, p -> castAbility(p, "devilfruits:supa_supa/blade_skates", 0), mc -> {
         firstPerson().accept(mc);
         mc.options.hideGui = false;
      }, 40, c -> {
         s.add(c + 8, mc -> keys(mc, true, false));
         this.stills(s, c, "skate_fp", 16, 24, 30);
         s.add(c + 32, mc -> {
            keys(mc, false, false);
            mc.options.hideGui = true;
         });
      });
      return this.take(s, t, 5, day(), 0, p -> castAbility(p, "devilfruits:supa_supa/steel_body", 40), rig(3.4, 30.0F, 10.0F, 0.3), 110, c -> {
         this.clip(s, c - 1, 120);
         s.add(c + 20, mc -> s.server(mc, p -> command(p, "damage @s 18 minecraft:mob_attack by @e[type=!player,limit=1,sort=nearest]")));
         s.add(c + 46, mc -> s.server(mc, p -> {
               skateStage(s).accept(p);
               castAbility(p, "devilfruits:supa_supa/blade_skates", 0);
            }));
         s.add(c + 56, mc -> keys(mc, true, false));
         circle(s, c + 58, 40);
         s.add(c + 98, mc -> keys(mc, false, true));
         s.add(c + 108, mc -> keys(mc, false, false));
      });
   }

   private int blades(AutoTest s, int t) {
      t = this.take(s, t, 8, day(), 0, blades(BladeLimbs.ArmMode.FOREARM, true, true), rig(3.0, 150.0F, 10.0F, 0.25), 34, c -> {
         this.stills(s, c, "meta_tp", 0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 14);
         s.add(c + 22, mc -> s.server(mc, p -> BladeLimbs.arms(p, BladeLimbs.ArmMode.NONE, false, false, 0)));
         this.stills(s, c, "fold_tp", 23, 24, 25, 27);
      });
      t = this.take(
         s,
         t,
         8,
         day(),
         0,
         blades(BladeLimbs.ArmMode.FOREARM, true, false),
         rig(1.7, 125.0F, 4.0F, 0.1),
         16,
         c -> this.stills(s, c, "meta_close", 1, 2, 3, 4, 5, 6, 8, 12)
      );
      t = this.take(s, t, 8, none(), 0, blades(BladeLimbs.ArmMode.FOREARM, true, false), mc -> {
         firstPerson().accept(mc);
         mc.options.hideGui = false;
      }, 16, c -> {
         this.stills(s, c, "meta_fp", 1, 2, 3, 4, 5, 6, 8, 12);
         s.add(c + 14, mc -> mc.options.hideGui = true);
      });
      t = this.take(s, t, 8, none(), 0, none(), mc -> {
         RemotePlayer slim = slimBody(mc);
         if (slim != null) {
            CaptureRig.follow(slim, 2.4, 50.0F, 8.0F, 0.2);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
         }
      }, 20, c -> {
         s.add(c, mc -> {
            Entity e = mc.level == null ? null : mc.level.getEntity(9400000);
            if (e != null) {
               DFAttachments.data((RemotePlayer)e).setFruit(SupaSupa.ID);
               DFAttachments.data((RemotePlayer)e).setFlags(BladeLimbs.flags(BladeLimbs.ArmMode.FOREARM, true, true, false, false));
            }
         });
         this.stills(s, c, "meta_slim", 2, 4, 6, 9, 14);
         s.add(c + 19, mc -> {
            if (mc.level != null) {
               mc.level.removeEntity(9400000, RemovalReason.DISCARDED);
            }

            CaptureRig.off();
         });
      });
      t = this.take(s, t, 8, p -> dummyTo(s, p, -5.0), 2, cast("spar_claw"), rig(4.5, 215.0F, 12.0F, 0.5), 24, c -> {
         this.stills(s, c, "claw", 1, 2, 3, 4, 5, 6, 8);
         s.add(c + 10, mc -> s.server(mc, cast("spar_claw")));
         this.stills(s, c, "claw", 12, 13, 14, 15, 17);
      });

      for (String view : new String[]{"back", "side"}) {
         t = this.take(
            s,
            t,
            8,
            p -> dummyTo(s, p, -7.0),
            2,
            cast("spar_claw"),
            view.equals("back") ? rig(4.0, 205.0F, 14.0F, 0.6) : rig(5.5, 140.0F, 12.0F, 0.8),
            40,
            c -> {
               s.add(c + 9, mc -> s.server(mc, cast("spar_claw")));
               s.add(c + 18, mc -> s.server(mc, cast("spar_claw")));
               this.stills(s, c, "atomic_" + view, 19, 20, 21, 22, 23, 24, 25, 26, 28, 31);
            }
         );
      }

      return this.take(s, t, 5, p -> dummyTo(s, p, -7.0), 2, blades(BladeLimbs.ArmMode.FOREARM, true, true), rig(4.2, 150.0F, 8.0F, 0.4), 70, c -> {
         this.clip(s, c - 1, 72);
         s.add(c + 22, mc -> s.server(mc, cast("spar_claw")));
         s.add(c + 31, mc -> s.server(mc, cast("spar_claw")));
         s.add(c + 40, mc -> s.server(mc, cast("spar_claw")));
      });
   }

   private static RemotePlayer slimBody(Minecraft mc) {
      if (mc.level != null && mc.player != null) {
         UUID id = null;

         for (int i = 0; i < 200; i++) {
            UUID u = UUID.nameUUIDFromBytes(("supa_slim_" + i).getBytes());
            if (DefaultPlayerSkin.get(u).model() == Model.SLIM) {
               id = u;
               break;
            }
         }

         if (id == null) {
            return null;
         } else {
            RemotePlayer body = new RemotePlayer(mc.level, new GameProfile(id, "Slim"));
            body.setId(9400000);
            body.moveTo(mc.player.getX() - 2.5, mc.player.getY(), mc.player.getZ(), 180.0F, 8.0F);
            body.setYHeadRot(180.0F);
            body.yBodyRot = 180.0F;
            body.yBodyRotO = 180.0F;
            mc.level.addEntity(body);
            return body;
         }
      } else {
         return null;
      }
   }
}
