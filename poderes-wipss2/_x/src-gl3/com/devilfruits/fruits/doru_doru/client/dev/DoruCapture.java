package com.devilfruits.fruits.doru_doru.client.dev;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.ClientFruitState;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureStage;
import com.devilfruits.client.input.DFKeys;
import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.fruits.doru_doru.DoruAbilities;
import com.devilfruits.fruits.doru_doru.DoruDoru;
import com.devilfruits.fruits.doru_doru.DoruItems;
import com.devilfruits.fruits.doru_doru.ability.CandleServiceAbility;
import com.devilfruits.fruits.doru_doru.ability.WaxToolsAbility;
import com.devilfruits.fruits.doru_doru.atelier.BuildEngine;
import com.devilfruits.fruits.doru_doru.atelier.DoruNetwork;
import com.devilfruits.fruits.doru_doru.atelier.SculptRequest;
import com.devilfruits.fruits.doru_doru.atelier.ShapeKind;
import com.devilfruits.fruits.doru_doru.atelier.UndoHistory;
import com.devilfruits.fruits.doru_doru.atelier.VoxelSet;
import com.devilfruits.fruits.doru_doru.client.atelier.SculptClient;
import com.devilfruits.fruits.doru_doru.coating.WaxCoating;
import com.devilfruits.fruits.doru_doru.item.WaxTools;
import com.devilfruits.fruits.doru_doru.wax.WaxBlockEntity;
import com.devilfruits.fruits.doru_doru.wax.WaxOwners;
import com.devilfruits.fruits.doru_doru.wax.WaxWorld;
import com.devilfruits.registry.DFAttachments;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class DoruCapture extends CaptureStage {
   private final String mode;

   public DoruCapture(String mode) {
      super("doru_" + mode + "_");
      this.mode = mode;
   }

   static void log(String text, Object... args) {
      DevilFruits.LOGGER.info("[devilfruits autotest] doru " + text, args);
   }

   @Override
   protected ResourceLocation fruit() {
      return DoruDoru.ID;
   }

   @Override
   protected void setup(ServerPlayer p) {
      command(p, "time set 6000");
   }

   @Override
   protected int takes(AutoTest s, int t) {
      String var3 = this.mode;

      return switch (var3) {
         case "atelier" -> this.atelier(s, t);
         case "depth" -> this.depth(s, t);
         case "tools" -> this.tools(s, t);
         case "service" -> this.service(s, t);
         case "champion" -> this.champion(s, t);
         case "checks" -> this.checks(s, t);
         default -> this.wax(s, t);
      };
   }

   private int wax(AutoTest s, int t) {
      s.add(t, mc -> s.server(mc, p -> {
            ServerLevel level = p.serverLevel();
            int y = s.origin().getY();
            int owner = WaxOwners.get(level.getServer()).index(p.getUUID());
            pour(level, sphere(-4.0, (double)y, -9.0, 2.75, true), 0, owner);
            VoxelSet wall = new VoxelSet();
            box(wall, 1, y, -13, 4, y + 2, -12);
            pour(level, wall, 0, owner);
            pour(level, arch(0.0, (double)y, -15.5, 3.0, 0.5, 0.5), 0, owner);
            VoxelSet ramp = new VoxelSet();

            for (int sxxxx = 0; sxxxx < 16; sxxxx++) {
               for (int sz = 0; sz < 8; sz++) {
                  for (int sy = 0; sy <= sxxxx / 2; sy++) {
                     ramp.add(12 + sxxxx, 4 * y + sy, -36 + sz);
                  }
               }
            }

            pour(level, ramp, 0, owner);

            for (int c = 0; c < 16; c++) {
               VoxelSet slab = new VoxelSet();

               for (int sxxx = 0; sxxx < 4; sxxx++) {
                  for (int sz = 0; sz < 4; sz++) {
                     for (int sy = 0; sy < 2; sy++) {
                        slab.add(4 * (c - 8) + sxxx, 4 * y + sy, -16 + sz);
                     }
                  }
               }

               pour(level, slab, c, owner);
            }

            VoxelSet hot = new VoxelSet();
            box(hot, 5, y, -6, 6, y + 1, -5);
            pour(level, hot, 0, owner);
            level.setBlockAndUpdate(new BlockPos(6, y, -6), Blocks.LAVA_CAULDRON.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(4, y, -5), Blocks.CAMPFIRE.defaultBlockState());
            p.teleportTo(level, 0.5, (double)y, 2.5, 180.0F, 20.0F);
            log("wax blocks owned: {}", WaxOwners.get(level.getServer()).count(p.getUUID()));
         }));
      this.view(s, t + 10, 0.5, 5.0, 4.5, 180.0F, 32.0F);
      this.shot(s, t + 30, "wide");
      this.view(s, t + 32, -4.0, 2.2, -3.0, 180.0F, 18.0F);
      this.shot(s, t + 50, "dome");
      this.view(s, t + 52, 0.5, 2.0, -8.0, 180.0F, 8.0F);
      this.shot(s, t + 70, "arch");
      this.view(s, t + 72, 5.5, 2.4, -2.0, 180.0F, 30.0F);
      this.shot(s, t + 90, "melting");
      this.view(s, t + 92, 0.5, 3.0, -0.5, 180.0F, 48.0F);
      this.shot(s, t + 110, "dyes");
      this.view(s, t + 112, 6.0, 2.0, -9.0, 90.0F, 20.0F);
      this.shot(s, t + 130, "ramp");
      s.add(t + 132, mc -> s.server(mc, p -> p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)DoruItems.FRUIT.get()))));
      this.view(s, t + 134, 0.5, 0.0, 1.5, 180.0F, 10.0F);
      s.add(t + 136, mc -> mc.options.hideGui = false);
      this.shot(s, t + 150, "fruit_first_person");
      s.add(t + 152, rig(2.6, 200.0F, 10.0F, 0.2));
      this.shot(s, t + 170, "fruit_third_person");
      s.add(t + 172, mc -> mc.options.hideGui = true);
      this.view(s, t + 174, 5.5, 2.4, -2.0, 180.0F, 30.0F);
      this.shot(s, t + 190, "melted");
      s.add(
         t + 192,
         mc -> s.server(
               mc,
               p -> {
                  BlockPos at = new BlockPos(5, s.origin().getY(), -6);
                  log(
                     "melting block: {}",
                     p.serverLevel().getBlockEntity(at) instanceof WaxBlockEntity be
                        ? "mask " + Long.bitCount(be.mask()) + " cells, state " + be.melt() + ", heat " + be.heat()
                        : "gone"
                  );
               }
            )
      );
      return t + 200;
   }

   private void view(AutoTest s, int tick, double x, double dy, double z, float yaw, float pitch) {
      s.add(tick, mc -> s.server(mc, p -> {
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.teleportTo(p.serverLevel(), x, (double)s.origin().getY() + dy, z, yaw, pitch);
         }));
      s.add(tick + 1, firstPerson());
   }

   private int atelier(AutoTest s, int t) {
      s.add(t, mc -> s.server(mc, p -> {
            int y = s.origin().getY();
            p.teleportTo(p.serverLevel(), 0.5, (double)y, 0.5, 180.0F, 22.0F);
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            log("atelier: user at {} {} {}", p.getX(), p.getY(), p.getZ());
         }));
      s.add(t + 4, mc -> s.tap(DoruAbilities.ATELIER));
      s.add(t + 8, firstPerson());
      t = this.design(
         s,
         t + 12,
         "arch",
         ShapeKind.ARCH,
         new float[]{4.0F, 1.0F, 1.0F},
         y -> List.of(new Vec3(-3.5, (double)y, -6.5), new Vec3(3.5, (double)y, -6.5)),
         0.0F,
         0
      );
      t = this.design(s, t, "dome", ShapeKind.DOME, new float[]{3.5F}, y -> List.of(new Vec3(-3.5, (double)y, -12.5)), 0.75F, 3);
      t = this.design(
         s, t, "column", ShapeKind.COLUMN, new float[]{1.25F}, y -> List.of(new Vec3(4.5, (double)y, -12.5), new Vec3(4.5, (double)(y + 7), -12.5)), 0.0F, 0
      );
      t = this.design(
         s,
         t,
         "spiral",
         ShapeKind.SPIRAL,
         new float[]{3.5F, 2.0F, 5.0F},
         y -> List.of(new Vec3(4.5, (double)y, -12.5), new Vec3(4.5, (double)(y + 7), -12.5)),
         0.0F,
         0
      );
      t = this.design(
         s,
         t,
         "unsupported",
         ShapeKind.SPIRAL,
         new float[]{3.0F, 1.5F, 5.0F},
         y -> List.of(new Vec3(-4.5, (double)y, -1.5), new Vec3(-4.5, (double)(y + 8), -1.5)),
         0.0F,
         0
      );
      s.add(t, mc -> SculptClient.debugRelease());
      s.add(t + 2, rig(14.0, 165.0F, 28.0F, 3.0));
      this.shot(s, t + 20, "wide");
      s.add(
         t + 22,
         mc -> s.server(mc, p -> log("atelier: {} wax blocks owned, {} jobs running", WaxOwners.get(p.getServer()).count(p.getUUID()), BuildEngine.running()))
      );
      s.add(t + 24, mc -> s.tap(DoruAbilities.ATELIER));
      return t + 30;
   }

   private int depth(AutoTest s, int t) {
      s.add(
         t,
         mc -> s.server(
               mc,
               p -> {
                  ServerLevel level = p.serverLevel();
                  int y = s.origin().getY();
                  int owner = WaxOwners.get(level.getServer()).index(p.getUUID());
                  VoxelSet wall = new VoxelSet();
                  box(wall, -6, y, -8, -1, y + 4, -7);
                  pour(level, wall, 0, owner);
                  pour(level, sphere(-4.0, (double)y, -13.0, 3.0, true), 0, owner);

                  for (int x = 3; x <= 7; x++) {
                     for (int z = -14; z <= -10; z++) {
                        for (int h = 0; h < 4; h++) {
                           boolean wallBlock = x == 3 || x == 7 || z == -14 || z == -10;
                           BlockPos at = new BlockPos(x, y + h, z);
                           if (h == 3) {
                              level.setBlockAndUpdate(at, Blocks.SPRUCE_PLANKS.defaultBlockState());
                           } else if (wallBlock) {
                              boolean window = h == 1 && (x == 5 || z == -12);
                              boolean door = z == -10 && x == 5 && h < 2;
                              level.setBlockAndUpdate(
                                 at,
                                 door
                                    ? Blocks.AIR.defaultBlockState()
                                    : (
                                       window
                                          ? Blocks.GLASS.defaultBlockState()
                                          : (h == 0 ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState())
                                    )
                              );
                           }
                        }
                     }
                  }

                  level.setBlockAndUpdate(new BlockPos(5, y + 4, -12), Blocks.RED_WOOL.defaultBlockState());
                  p.teleportTo(level, 0.5, (double)y, 1.5, 180.0F, 20.0F);
               }
            )
      );
      s.add(t + 6, mc -> s.tap(DoruAbilities.ATELIER));
      s.add(t + 10, rig(9.0, 200.0F, 25.0F, 2.0));
      this.shot(s, t + 20, "before");
      s.add(t + 22, mc -> s.server(mc, p -> p.teleportTo(p.serverLevel(), -3.5, (double)s.origin().getY(), -2.5, 180.0F, 15.0F)));
      s.add(t + 24, mc -> SculptClient.debugStroke(0, 0.6F, 1.0F, line(s, -6.5, 3.6, -6.4, -0.5, 3.6, -6.4, 9), 0));
      s.add(t + 26, rig(4.5, 205.0F, 8.0F, 1.2));
      this.shot(s, t + 50, "drips");
      s.add(t + 54, mc -> SculptClient.debugStroke(1, 0.8F, 0.0F, line(s, -4.0, 1.6, -8.4, -4.0, 1.6, -6.6, 4), 0));
      this.shot(s, t + 80, "window");
      s.add(t + 84, mc -> SculptClient.debugStroke(3, 1.5F, 0.0F, line(s, -5.5, 3.0, -13.0, -2.5, 3.0, -13.0, 5), 14));
      s.add(t + 96, mc -> SculptClient.debugStroke(2, 1.0F, 0.0F, line(s, -6.0, 3.9, -7.5, -1.0, 3.9, -7.5, 8), 0));
      this.shot(s, t + 114, "dye_smooth");
      s.add(t + 116, mc -> s.server(mc, p -> p.teleportTo(p.serverLevel(), 0.5, (double)s.origin().getY(), 1.5, 180.0F, 20.0F)));
      s.add(t + 117, rig(9.0, 200.0F, 25.0F, 2.0));
      s.add(t + 118, mc -> SculptClient.debugCast(0, at(s, 3.5, 0.5, -13.5), at(s, 7.5, 4.5, -9.5), 0, false, 2));
      s.add(t + 124, mc -> SculptClient.debugCastGhost(0, at(s, 0.0, 0.0, -3.5), 0, false, 2));
      this.shot(s, t + 132, "cast_ghost");
      s.add(t + 134, mc -> SculptClient.debugCast(0, at(s, 0.0, 0.0, -3.5), null, 0, false, 2));
      s.add(t + 160, mc -> SculptClient.debugCast(0, at(s, 5.0, 0.0, -4.0), null, 1, true, 1));
      s.add(t + 186, rig(10.0, 160.0F, 30.0F, 2.0));
      this.shot(s, t + 200, "casts");
      s.add(
         t + 204,
         mc -> SculptClient.debugDesign(
               ShapeKind.COLUMN,
               new float[]{0.5F},
               List.of(at(s, -3.0, 0.0, 2.0), at(s, -3.0, 3.0, 2.0)),
               0.0F,
               SculptRequest.Symmetry.RADIAL,
               6,
               at(s, -3.0, 0.0, 0.0),
               2
            )
      );
      this.shot(s, t + 210, "radial_ghost");
      s.add(t + 212, mc -> SculptClient.debugPour());
      s.add(t + 216, rig(8.0, 200.0F, 35.0F, 1.5));
      this.shot(s, t + 240, "radial");
      s.add(t + 244, mc -> PacketDistributor.sendToServer(new DoruNetwork.CommandPayload(0), new CustomPacketPayload[0]));
      this.shot(s, t + 256, "undone");
      s.add(
         t + 258,
         mc -> {
            SculptClient.debugRelease();
            s.server(
               mc,
               p -> log(
                     "depth: {} wax blocks owned, undo {} redo {}",
                     WaxOwners.get(p.getServer()).count(p.getUUID()),
                     UndoHistory.undoCount(p.getUUID()),
                     UndoHistory.redoCount(p.getUUID())
                  )
            );
         }
      );
      s.add(t + 262, rig(16.0, 170.0F, 35.0F, 4.0));
      this.shot(s, t + 280, "wide");
      s.add(t + 282, mc -> s.tap(DoruAbilities.ATELIER));
      return t + 290;
   }

   private static Vec3 at(AutoTest s, double x, double dy, double z) {
      return new Vec3(x, (double)s.origin().getY() + dy, z);
   }

   private static List<Vec3> line(AutoTest s, double x0, double y0, double z0, double x1, double y1, double z1, int n) {
      List<Vec3> out = new ArrayList<>();

      for (int i = 0; i < n; i++) {
         double k = n == 1 ? 0.0 : (double)i / (double)(n - 1);
         out.add(at(s, x0 + (x1 - x0) * k, y0 + (y1 - y0) * k, z0 + (z1 - z0) * k));
      }

      return out;
   }

   private int design(AutoTest s, int t, String name, ShapeKind kind, float[] params, IntFunction<List<Vec3>> points, float shell, int dye) {
      s.add(t, mc -> SculptClient.debugDesign(kind, params, points.apply(s.origin().getY()), shell, SculptRequest.Symmetry.NONE, 4, Vec3.ZERO, dye));
      s.add(t + 2, rig(6.5, 205.0F, 18.0F, 1.2));
      s.add(
         t + 10,
         mc -> {
            SculptClient.Ghost g = SculptClient.debugGhost();
            log(
               "{} ghost: {}",
               name,
               g == null
                  ? "none"
                  : String.format(Locale.ROOT, "%.1f blocks, cost %.1f, problem %s, %d unsupported", g.blocks(), g.cost(), g.problem(), g.unsupported())
            );
         }
      );
      this.shot(s, t + 12, name + "_ghost");
      s.add(t + 14, mc -> s.server(mc, p -> command(p, "tick rate 4")));
      s.add(t + 18, mc -> SculptClient.debugPour());
      this.shot(s, t + 20, name + "_pour_a");
      this.shot(s, t + 23, name + "_pour_b");
      s.add(t + 24, firstPerson());
      this.shot(s, t + 26, name + "_pour_fp");
      s.add(t + 28, rig(6.5, 205.0F, 18.0F, 1.2));
      this.shot(s, t + 30, name + "_pour_c");
      s.add(t + 34, mc -> s.server(mc, p -> command(p, "tick rate 20")));
      this.shot(s, t + 60, name + "_set");
      return t + 64;
   }

   private static void clear(AutoTest s, ServerPlayer p) {
      int y = s.origin().getY();
      command(p, "fill -7 " + y + " -16 7 " + (y + 8) + " 2 minecraft:air");
      p.setShiftKeyDown(false);
      WaxTools.dissolveAll(p, false);
   }

   private static String id(Ability a) {
      return a.id().toString();
   }

   private int tools(AutoTest s, int t) {
      t = this.take(s, t, 20, p -> {
         clear(s, p);
         p.teleportTo(p.serverLevel(), 0.5, (double)s.origin().getY(), -2.5, 180.0F, 10.0F);
      }, 4, p -> castAbility(p, id(DoruAbilities.TOOLS), 0), mc -> {
         firstPerson().accept(mc);
         mc.options.hideGui = false;
      }, 74, c -> {
         this.shot(s, c + 4, "tool_forming");
         this.shot(s, c + 26, "pickaxe_fp");
         s.add(c + 28, mc -> s.server(mc, p -> WaxToolsAbility.choose(p, 4)));
         this.shot(s, c + 52, "sword_fp");
         s.add(c + 54, rig(3.2, 210.0F, 12.0F, 0.3));
         this.shot(s, c + 66, "sword_tp");
         s.add(c + 70, mc -> mc.options.hideGui = true);
      });
      t = this.take(s, t, 8, p -> {
         clear(s, p);
         p.teleportTo(p.serverLevel(), 0.5, (double)s.origin().getY(), -2.5, 180.0F, 24.0F);
      }, 4, p -> castAbility(p, id(DoruAbilities.WALL), 0), rig(9.0, 125.0F, 16.0F, 1.0), 70, c -> {
         this.shot(s, c + 3, "wall_rise_a");
         this.shot(s, c + 8, "wall_rise_b");
         s.add(c + 30, mc -> s.server(mc, p -> command(p, "tick rate 20")));
         this.shot(s, c + 50, "wall_set");
         s.add(c + 52, rig(7.0, 200.0F, 6.0F, 0.8));
         this.shot(s, c + 66, "wall_front");
      });
      t = this.take(s, t, 8, p -> {
         clear(s, p);
         p.teleportTo(p.serverLevel(), 0.5, (double)s.origin().getY(), -2.5, 180.0F, 10.0F);
         p.setShiftKeyDown(true);
      }, 6, p -> castAbility(p, id(DoruAbilities.WALL), 0), rig(10.0, 150.0F, 28.0F, 1.5), 60, c -> {
         this.shot(s, c + 4, "dome_rise");
         s.add(c + 20, mc -> s.server(mc, p -> {
               p.setShiftKeyDown(false);
               command(p, "tick rate 20");
            }));
         this.shot(s, c + 40, "dome_set");
         s.add(c + 42, mc -> s.server(mc, p -> log("dome: {} wax blocks owned", WaxOwners.get(p.getServer()).count(p.getUUID()))));
      });
      t = this.take(
         s,
         t,
         8,
         p -> {
            clear(s, p);
            dummyTo(s, p, -7.5);
            p.teleportTo(p.serverLevel(), 0.5, (double)s.origin().getY(), -2.5, 180.0F, 4.0F);
         },
         4,
         p -> castAbility(p, id(DoruAbilities.LANCE), 2),
         rig(6.0, 115.0F, 10.0F, 0.6),
         50,
         c -> {
            this.shot(s, c + 1, "lance_form");
            this.shot(s, c + 4, "lance_thrust");
            this.shot(s, c + 9, "lance_hit");
            s.add(c + 20, mc -> s.server(mc, p -> command(p, "tick rate 20")));
            this.shot(s, c + 36, "lance_coated");
            s.add(
               c + 40,
               mc -> s.server(
                     mc,
                     p -> p.serverLevel()
                           .getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(12.0), e -> e != p)
                           .forEach(e -> log("lance: {} coated {} stacks", e.getName().getString(), WaxCoating.get(e).stacks()))
                  )
            );
         }
      );
      t = this.take(s, t, 8, p -> {
         clear(s, p);
         dummyTo(s, p, 1.5);
         int y = s.origin().getY();
         command(p, "fill -3 " + y + " -13 3 " + (y + 5) + " -13 minecraft:stone_bricks");
         p.teleportTo(p.serverLevel(), 0.5, (double)y, -2.5, 180.0F, -4.0F);
      }, 4, p -> castAbility(p, id(DoruAbilities.LANCE), 30), rig(7.0, 120.0F, 10.0F, 1.0), 90, c -> {
         this.shot(s, c + 10, "lance_charge");
         s.add(c + 12, rig(5.0, 90.0F, 6.0F, 1.0));
         this.shot(s, c + 22, "lance_charge_side");
         s.add(c + 24, rig(7.0, 120.0F, 10.0F, 1.0));
         this.shot(s, c + 34, "lance_flight");
         s.add(c + 40, mc -> s.server(mc, p -> command(p, "tick rate 20")));
         s.add(c + 60, rig(6.0, 150.0F, 14.0F, 2.0));
         this.shot(s, c + 76, "lance_peg");
         s.add(c + 78, mc -> s.server(mc, p -> log("throw: {} wax blocks owned", WaxOwners.get(p.getServer()).count(p.getUUID()))));
      });
      t = this.take(
         s,
         t,
         20,
         p -> {
            clear(s, p);
            dummyTo(s, p, -7.5);
            p.teleportTo(p.serverLevel(), 0.5, (double)s.origin().getY(), -4.0, 180.0F, 12.0F);
         },
         4,
         p -> p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(8.0), e -> e != p).forEach(e -> WaxCoating.add(e, 10, p)),
         firstPerson(),
         80,
         c -> {
            this.shot(s, c + 3, "encase");
            this.shot(s, c + 30, "encased");
            this.shot(s, c + 62, "break_free");
            this.shot(s, c + 70, "free");
         }
      );
      s.add(t, mc -> s.server(mc, p -> clear(s, p)));
      return t + 4;
   }

   private int service(AutoTest s, int t) {
      t = this.take(
         s,
         t,
         8,
         p -> {
            clear(s, p);
            ServerLevel level = p.serverLevel();
            int y = s.origin().getY();
            p.teleportTo(level, 0.5, (double)y, 1.5, 180.0F, 10.0F);
            level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(30.0)).forEach(e -> e.moveTo(-5.5, (double)y, -5.0, 0.0F, 0.0F));
            command(p, "fill 4 " + (y + 3) + " -7 7 " + (y + 3) + " -3 minecraft:smooth_stone");
            command(p, "summon minecraft:husk 5.5 " + y + " -5.0 {NoAI:1b,PersistenceRequired:1b}");
            command(p, "summon minecraft:husk -7.0 " + y + " -8.0 {NoAI:1b,PersistenceRequired:1b}");
         },
         6,
         p -> castAbility(p, id(DoruAbilities.SERVICE), 0),
         rig(11.0, 25.0F, 24.0F, 2.5),
         420,
         c -> {
            this.shot(s, c + 6, "rise_a");
            this.shot(s, c + 18, "rise_b");
            s.add(c + 30, mc -> s.server(mc, p -> command(p, "tick rate 20")));
            this.shot(s, c + 46, "lit_wide");
            this.view(s, c + 48, 6.5, 6.0, 1.0, 135.0F, 22.0F);
            this.shot(s, c + 62, "candles");
            this.view(s, c + 64, -5.5, 1.6, -1.0, 180.0F, 8.0F);
            this.shot(s, c + 76, "dummy_coating");
            this.shot(s, c + 125, "dummy_encased");
            s.add(
               c + 127,
               mc -> s.server(
                     mc,
                     p -> p.serverLevel()
                           .getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(30.0))
                           .forEach(
                              e -> log(
                                    "service: {} at {} {} has {} stacks, encased {}",
                                    e.getName().getString(),
                                    (int)e.getX(),
                                    (int)e.getZ(),
                                    WaxCoating.get(e).stacks(),
                                    WaxCoating.get(e).encased(e.level().getGameTime())
                                 )
                           )
                  )
            );
            s.add(c + 143, mc -> s.server(mc, p -> log("service: stand integrity {}", CandleServiceAbility.running(p))));
            this.view(s, c + 144, 0.5, 9.0, 11.0, 180.0F, 30.0F);
            this.shot(s, c + 158, "wide_high");
            s.add(c + 160, mc -> s.server(mc, p -> command(p, "tick sprint 380")));
            this.view(s, c + 250, 6.5, 6.0, 1.0, 135.0F, 22.0F);
            this.shot(s, c + 264, "burnt_down");
            s.add(c + 266, mc -> s.server(mc, p -> command(p, "tick sprint 260")));
            this.view(s, c + 330, 6.5, 6.0, 3.0, 140.0F, 24.0F);
            this.shot(s, c + 344, "out");
            this.shot(s, c + 410, "melted");
            s.add(
               c + 412,
               mc -> s.server(
                     mc,
                     p -> log(
                           "service: running {} (-1 = over), {} wax blocks owned",
                           CandleServiceAbility.running(p),
                           WaxOwners.get(p.getServer()).count(p.getUUID())
                        )
                  )
            );
         }
      );
      s.add(t, mc -> s.server(mc, p -> {
            clear(s, p);
            command(p, "kill @e[type=minecraft:husk]");
         }));
      return t + 4;
   }

   private static Mob dummy(ServerPlayer p) {
      return (Mob)p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(30.0)).stream().findFirst().orElse(null);
   }

   private int champion(AutoTest s, int t) {
      t = this.take(
         s,
         t,
         20,
         p -> {
            clear(s, p);
            p.teleportTo(p.serverLevel(), 0.5, (double)s.origin().getY(), -2.5, 180.0F, 6.0F);
         },
         4,
         p -> castAbility(p, id(DoruAbilities.CHAMPION), 0),
         rig(12.6, 140.0F, 8.0F, 2.9),
         330,
         c -> {
            this.shot(s, c + 14, "windup");
            this.shot(s, c + 38, "rising");
            this.shot(s, c + 66, "suit");
            s.add(
               c + 68,
               mc -> s.server(
                     mc,
                     p -> log(
                           "champion: form {}, absorption {}, scale {}, height {}",
                           DFAttachments.data(p).formName(),
                           p.getAbsorptionAmount(),
                           p.getScale(),
                           p.getBbHeight()
                        )
                  )
            );
            s.add(c + 70, rig(12.6, 180.0F, 6.0F, 3.4));
            this.shot(s, c + 80, "front");
            s.add(c + 81, rig(5.0, 160.0F, 4.0F, 4.4));
            this.shot(s, c + 85, "head");
            s.add(c + 86, rig(12.6, 90.0F, 6.0F, 3.4));
            this.shot(s, c + 92, "side");
            s.add(c + 94, rig(14.4, 150.0F, 10.0F, 3.4));

            for (int i = 0; i < 24; i++) {
               int k = i;
               s.add(c + 96 + i, mc -> s.server(mc, p -> p.teleportTo(p.serverLevel(), 0.5, (double)s.origin().getY(), -0.5 - (double)k * 0.16, 180.0F, 6.0F)));
            }

            this.shot(s, c + 110, "walk");
            s.add(c + 124, mc -> s.server(mc, p -> {
                  int y = s.origin().getY();
                  p.teleportTo(p.serverLevel(), 0.5, (double)y, -2.5, 180.0F, 6.0F);
                  Mob d = dummy(p);
                  if (d != null) {
                     d.moveTo(0.5, (double)y, -5.1, 0.0F, 0.0F);
                  }

                  command(p, "summon minecraft:husk -1.3 " + y + " -5.5 {NoAI:1b,PersistenceRequired:1b}");
                  command(p, "summon minecraft:husk 2.3 " + y + " -5.5 {NoAI:1b,PersistenceRequired:1b}");
               }));
            s.add(c + 134, mc -> s.server(mc, p -> command(p, "tick rate 8")));
            s.add(c + 136, rig(11.7, 125.0F, 8.0F, 2.9));
            s.add(c + 140, mc -> s.server(mc, p -> {
                  Mob d = dummy(p);
                  p.swing(InteractionHand.MAIN_HAND, true);
                  if (d != null) {
                     p.attack(d);
                  }
               }));
            this.shot(s, c + 142, "punch_a");
            this.shot(s, c + 145, "punch_b");
            s.add(
               c + 150,
               mc -> s.server(
                     mc,
                     p -> {
                        command(p, "tick rate 20");
                        p.serverLevel()
                           .getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(8.0), e -> e != p)
                           .forEach(e -> log("champion punch: {} health {}", e.getName().getString(), e.getHealth()));
                        log("champion: absorption after punching {}", p.getAbsorptionAmount());
                        command(p, "kill @e[type=minecraft:husk]");
                     }
                  )
            );
            s.add(c + 160, mc -> s.server(mc, p -> {
                  Mob d = dummy(p);
                  if (d != null) {
                     d.moveTo(0.5, (double)s.origin().getY(), -6.0, 0.0F, 0.0F);
                  }

                  p.teleportTo(p.serverLevel(), 0.5, (double)(s.origin().getY() + 7), -2.5, 180.0F, 6.0F);
               }));
            s.add(c + 162, rig(18.0, 130.0F, 12.0F, 2.4));
            this.shot(s, c + 168, "stomp_fall");
            s.add(c + 174, mc -> s.server(mc, p -> command(p, "tick rate 8")));
            this.shot(s, c + 182, "stomp_a");
            this.shot(s, c + 186, "stomp_b");
            s.add(c + 192, mc -> s.server(mc, p -> command(p, "tick rate 20")));
            s.add(c + 200, firstPerson());
            s.add(c + 201, mc -> mc.options.hideGui = false);
            this.shot(s, c + 214, "fp_fist");
            s.add(c + 216, mc -> s.server(mc, p -> p.swing(InteractionHand.MAIN_HAND, true)));
            this.shot(s, c + 219, "fp_punch");
            s.add(c + 224, mc -> mc.options.hideGui = true);
            s.add(c + 226, rig(12.6, 150.0F, 10.0F, 2.9));
            s.add(c + 240, mc -> s.server(mc, p -> castAbility(p, id(DoruAbilities.CHAMPION), 0)));
            this.shot(s, c + 250, "melting");
            this.shot(s, c + 268, "melted");
            s.add(
               c + 272,
               mc -> s.server(
                     mc,
                     p -> log(
                           "champion: after revert form '{}', absorption {}, scale {}", DFAttachments.data(p).formName(), p.getAbsorptionAmount(), p.getScale()
                        )
                  )
            );
         }
      );
      s.add(t, mc -> s.server(mc, p -> clear(s, p)));
      return t + 4;
   }

   private int checks(AutoTest s, int t) {
      s.add(t, mc -> s.server(mc, p -> {
            clear(s, p);
            p.teleportTo(p.serverLevel(), 0.5, (double)s.origin().getY(), -2.5, 180.0F, 6.0F);
            castAbility(p, id(DoruAbilities.TOOLS), 0);
            WaxTools.dissolveAll(p, false);
         }));
      s.add(t + 6, mc -> {
         FruitForm form = ClientFruitState.form();
         if (form != null) {
            ClientFruitState.select(form.indexOf(DoruAbilities.TOOLS.id()));
         }

         KeyMapping.set(DFKeys.USE.getKey(), true);
      });
      s.add(t + 10, mc -> {
         log("wheel: screen {}", mc.screen == null ? "none" : mc.screen.getClass().getSimpleName());
         KeyMapping.set(DFKeys.USE.getKey(), false);
      });
      s.add(t + 14, mc -> s.server(mc, p -> log("wheel: in hand after the press {}", p.getMainHandItem().getItem())));
      s.add(t + 16, mc -> PacketDistributor.sendToServer(new DoruNetwork.ToolChoicePayload(4), new CustomPacketPayload[0]));
      s.add(t + 20, mc -> s.server(mc, p -> log("wheel: in hand after picking the sword {}", p.getMainHandItem().getItem())));
      s.add(t + 22, mc -> {
         if (mc.screen != null) {
            mc.setScreen(null);
         }

         s.server(mc, p -> {
            WaxTools.dissolveAll(p, false);
            castAbility(p, id(DoruAbilities.CHAMPION), 0);
         });
      });
      s.add(t + 70, rig(4.5, 160.0F, 2.0F, 4.6));
      this.shot(s, t + 80, "head_front");
      s.add(t + 82, rig(8.0, 140.0F, 8.0F, 3.0));
      this.shot(s, t + 92, "head_wide");
      s.add(t + 94, mc -> s.server(mc, p -> castAbility(p, id(DoruAbilities.CHAMPION), 0)));
      return t + 130;
   }

   static void pour(ServerLevel level, VoxelSet set, int color, int owner) {
      ObjectIterator var4 = set.blocks().long2LongEntrySet().iterator();

      while (var4.hasNext()) {
         Entry e = (Entry)var4.next();
         WaxWorld.pour(level, BlockPos.of(e.getLongKey()), e.getLongValue(), color, owner, 0, 0L);
      }
   }

   static void box(VoxelSet set, int x0, int y0, int z0, int x1, int y1, int z1) {
      for (int x = x0 * 4; x < x1 * 4; x++) {
         for (int y = y0 * 4; y < y1 * 4; y++) {
            for (int z = z0 * 4; z < z1 * 4; z++) {
               set.add(x, y, z);
            }
         }
      }
   }

   static VoxelSet sphere(double cx, double cy, double cz, double r, boolean upperHalf) {
      VoxelSet set = new VoxelSet();
      int n = (int)Math.ceil(r * 4.0) + 1;

      for (int dx = -n; dx <= n; dx++) {
         for (int dy = upperHalf ? 0 : -n; dy <= n; dy++) {
            for (int dz = -n; dz <= n; dz++) {
               double x = ((double)dx + 0.5) / 4.0;
               double y = ((double)dy + 0.5) / 4.0;
               double z = ((double)dz + 0.5) / 4.0;
               if (x * x + y * y + z * z <= r * r) {
                  set.add((int)Math.floor(cx * 4.0) + dx, (int)Math.floor(cy * 4.0) + dy, (int)Math.floor(cz * 4.0) + dz);
               }
            }
         }
      }

      return set;
   }

   static VoxelSet arch(double cx, double cy, double cz, double r, double halfThick, double halfDepth) {
      VoxelSet set = new VoxelSet();
      int n = (int)Math.ceil((r + halfThick) * 4.0) + 1;
      int nd = (int)Math.ceil(halfDepth * 4.0);

      for (int dx = -n; dx <= n; dx++) {
         for (int dy = 0; dy <= n; dy++) {
            for (int dz = -nd; dz < nd; dz++) {
               double x = ((double)dx + 0.5) / 4.0;
               double y = ((double)dy + 0.5) / 4.0;
               double d = Math.sqrt(x * x + y * y);
               if (Math.abs(d - r) <= halfThick) {
                  set.add((int)Math.floor(cx * 4.0) + dx, (int)Math.floor(cy * 4.0) + dy, (int)Math.floor(cz * 4.0) + dz);
               }
            }
         }
      }

      return set;
   }
}
