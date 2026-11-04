package com.devilfruits.client.poneglyph;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.core.poneglyph.BlueprintData;
import com.devilfruits.core.poneglyph.DecipherDeskBlock;
import com.devilfruits.core.poneglyph.DecipherDeskBlockEntity;
import com.devilfruits.core.poneglyph.DecipherDeskMenu;
import com.devilfruits.core.poneglyph.Knowledge;
import com.devilfruits.core.poneglyph.PoneglyphFaceBlock;
import com.devilfruits.core.poneglyph.PoneglyphFaceBlockEntity;
import com.devilfruits.core.poneglyph.PoneglyphNetwork;
import com.devilfruits.core.poneglyph.PoneglyphRuins;
import com.devilfruits.core.poneglyph.Poneglyphs;
import com.devilfruits.core.poneglyph.RubbingService;
import com.devilfruits.core.seastone.SeastoneGear;
import com.devilfruits.registry.DFBlocks;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Rotations;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

final class PoneglyphAutotest implements AutoTest.Script {
   private static final double X = 60.5;
   private static final ResourceLocation ID = DevilFruits.id("seastone_refining");
   private static ArmorStand STAND;

   static void register() {
      AutoTest.addScript(DevilFruits.id("poneglyph"), new PoneglyphAutotest());
   }

   private static BlockPos face(AutoTest s, int dx, int dy) {
      return new BlockPos(60 + dx, s.origin().getY() + 1 + dy, 4);
   }

   @Override
   public int write(AutoTest s, int start) {
      int t = start;
      s.add(
         start,
         mc -> {
            s.camera(mc, CameraType.FIRST_PERSON);
            s.server(
               mc,
               p -> {
                  ServerLevel level = p.serverLevel();
                  int oy = s.origin().getY();

                  for (int dx = -1; dx <= 1; dx++) {
                     for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = 0; dz <= 2; dz++) {
                           BlockPos pos = new BlockPos(60 + dx, oy + 1 + dy, 4 + dz);
                           if (dz == 0) {
                              int cell = (1 - dy) * 3 + (1 - dx);
                              level.setBlock(
                                 pos,
                                 (BlockState)((BlockState)((PoneglyphFaceBlock)Poneglyphs.PONEGLYPH_FACE.get())
                                       .defaultBlockState()
                                       .setValue(PoneglyphFaceBlock.FACING, Direction.NORTH))
                                    .setValue(PoneglyphFaceBlock.CELL, cell),
                                 3
                              );
                              if (level.getBlockEntity(pos) instanceof PoneglyphFaceBlockEntity be) {
                                 be.setPoneglyph(ID);
                              }
                           } else {
                              level.setBlock(pos, ((Block)Poneglyphs.PONEGLYPH_STONE.get()).defaultBlockState(), 3);
                           }
                        }
                     }
                  }

                  level.setBlock(
                     new BlockPos(64, oy, 2),
                     (BlockState)((DecipherDeskBlock)Poneglyphs.DECIPHER_DESK.get()).defaultBlockState().setValue(DecipherDeskBlock.FACING, Direction.WEST),
                     3
                  );
                  Knowledge.revoke(p, ID);
                  p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PAPER, 16));
                  p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.INK_SAC, 16));
                  p.teleportTo(level, 60.5, (double)oy, 1.6, 0.0F, 8.0F);
               }
            );
         }
      );
      s.add(start + 15, mc -> s.shot(mc, "poneglyph_monolith"));

      for (int i = 0; i < 64; i++) {
         s.add(t + 18 + i, mc -> s.server(mc, p -> RubbingService.heartbeat(p, face(s, 0, 0))));
      }

      s.add(t + 50, mc -> s.shot(mc, "poneglyph_rubbing_fp"));
      s.add(t + 52, mc -> s.camera(mc, CameraType.THIRD_PERSON_BACK));
      s.add(t + 64, mc -> s.shot(mc, "poneglyph_rubbing_tp"));
      s.add(
         t + 84,
         mc -> {
            s.camera(mc, CameraType.FIRST_PERSON);
            s.shot(mc, "poneglyph_rubbed");
            s.server(
               mc,
               p -> DevilFruits.LOGGER
                     .info("[devilfruits autotest] rubbing taken: {}", p.getInventory().contains(st -> st.is((Item)Poneglyphs.RUBBING_ITEM.get())))
            );
         }
      );
      t += 95;
      s.add(t, mc -> s.server(mc, p -> {
            p.getInventory().clearContent();

            for (int cell = 0; cell < 9; cell++) {
               p.getInventory().setItem(cell, RubbingService.rubbing(ID, cell));
            }

            p.getInventory().setItem(9, new ItemStack((ItemLike)Poneglyphs.ENCRYPTED_BLUEPRINT.get()));
            p.teleportTo(p.serverLevel(), 60.5, (double)s.origin().getY(), 0.5, 180.0F, 10.0F);
         }));
      s.add(t + 10, mc -> s.shot(mc, "poneglyph_hotbar_rubbings"));
      s.add(
         t + 12,
         mc -> s.server(
               mc,
               p -> {
                  ItemStack[] gear = new ItemStack[]{
                     new ItemStack((ItemLike)Poneglyphs.ENCRYPTED_BLUEPRINT.get()),
                     new ItemStack((ItemLike)Poneglyphs.DECIPHERED_BLUEPRINT.get()),
                     new ItemStack((ItemLike)Poneglyphs.PRIMER.get()),
                     new ItemStack((ItemLike)SeastoneGear.RAW_SEASTONE.get()),
                     new ItemStack((ItemLike)SeastoneGear.SWORD.get()),
                     new ItemStack((ItemLike)SeastoneGear.JITTE.get()),
                     new ItemStack((ItemLike)SeastoneGear.PICKAXE.get()),
                     new ItemStack((ItemLike)SeastoneGear.CUFFS.get()),
                     new ItemStack((ItemLike)SeastoneGear.ARROW.get())
                  };

                  for (int ixx = 0; ixx < 9; ixx++) {
                     p.getInventory().setItem(ixx, gear[ixx]);
                  }

                  p.getInventory().selected = 5;
                  Block[] row = new Block[]{
                     (Block)SeastoneGear.SEASTONE_ORE.get(),
                     (Block)SeastoneGear.DEEPSLATE_SEASTONE_ORE.get(),
                     (Block)DFBlocks.SEASTONE_BLOCK.get(),
                     (Block)Poneglyphs.PONEGLYPH_STONE.get()
                  };

                  for (int ix = 0; ix < row.length; ix++) {
                     p.serverLevel().setBlock(new BlockPos(63 + ix, s.origin().getY(), -3), row[ix].defaultBlockState(), 3);
                  }

                  p.setItemSlot(EquipmentSlot.HEAD, new ItemStack((ItemLike)SeastoneGear.HELMET.get()));
                  p.setItemSlot(EquipmentSlot.CHEST, new ItemStack((ItemLike)SeastoneGear.CHESTPLATE.get()));
                  p.setItemSlot(EquipmentSlot.LEGS, new ItemStack((ItemLike)SeastoneGear.LEGGINGS.get()));
                  p.setItemSlot(EquipmentSlot.FEET, new ItemStack((ItemLike)SeastoneGear.BOOTS.get()));
               }
            )
      );
      s.add(t + 22, mc -> s.shot(mc, "poneglyph_hotbar_gear"));
      s.add(t + 24, mc -> s.camera(mc, CameraType.THIRD_PERSON_FRONT));
      s.add(t + 36, mc -> s.shot(mc, "poneglyph_seastone_armor"));
      s.add(t + 38, mc -> s.server(mc, p -> p.teleportTo(p.serverLevel(), 60.5, (double)s.origin().getY(), 0.5, 90.0F, 10.0F)));
      s.add(t + 50, mc -> s.shot(mc, "poneglyph_seastone_armor_side"));
      s.add(t + 52, mc -> {
         s.camera(mc, CameraType.FIRST_PERSON);
         s.server(mc, p -> {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
               p.setItemSlot(slot, ItemStack.EMPTY);
            }
         });
      });
      t += 60;
      s.add(t, mc -> s.server(mc, p -> {
            ServerLevel level = p.serverLevel();
            ArmorStand stand = (ArmorStand)EntityType.ARMOR_STAND.create(level);
            if (stand != null) {
               stand.moveTo(60.5, (double)s.origin().getY(), 3.0, 180.0F, 0.0F);
               stand.setShowArms(true);
               stand.setNoBasePlate(true);
               stand.setRightArmPose(new Rotations(-20.0F, 0.0F, 10.0F));
               stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack((ItemLike)SeastoneGear.HELMET.get()));
               stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack((ItemLike)SeastoneGear.CHESTPLATE.get()));
               stand.setItemSlot(EquipmentSlot.LEGS, new ItemStack((ItemLike)SeastoneGear.LEGGINGS.get()));
               stand.setItemSlot(EquipmentSlot.FEET, new ItemStack((ItemLike)SeastoneGear.BOOTS.get()));
               stand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack((ItemLike)SeastoneGear.JITTE.get()));
               level.addFreshEntity(stand);
               STAND = stand;
               p.teleportTo(level, 60.5, (double)s.origin().getY(), 0.4, 0.0F, 12.0F);
            }
         }));
      s.add(t + 20, mc -> s.shot(mc, "poneglyph_stand_front"));
      s.add(t + 22, mc -> s.server(mc, p -> {
            STAND.setYRot(90.0F);
            STAND.setYBodyRot(90.0F);
            STAND.setYHeadRot(90.0F);
         }));
      s.add(t + 30, mc -> s.shot(mc, "poneglyph_stand_side"));
      s.add(t + 32, mc -> s.server(mc, p -> {
            STAND.setYRot(0.0F);
            STAND.setYBodyRot(0.0F);
            STAND.setYHeadRot(0.0F);
         }));
      s.add(t + 40, mc -> s.shot(mc, "poneglyph_stand_back"));
      s.add(t + 42, mc -> s.server(mc, p -> {
            STAND.discard();
            p.teleportTo(p.serverLevel(), 60.5, (double)s.origin().getY(), 0.5, 180.0F, 0.0F);
         }));
      s.add(t + 44, mc -> {
         if (mc.player != null) {
            mc.player.getInventory().selected = 5;
         }
      });
      s.add(t + 75, mc -> s.shot(mc, "poneglyph_jitte_fp"));
      t += 80;
      s.add(
         t,
         mc -> s.server(
               mc,
               p -> {
                  BlockPos desk = new BlockPos(64, s.origin().getY(), 2);
                  p.serverLevel()
                     .setBlock(
                        desk,
                        (BlockState)((DecipherDeskBlock)Poneglyphs.DECIPHER_DESK.get()).defaultBlockState().setValue(DecipherDeskBlock.FACING, Direction.WEST),
                        3
                     );
                  p.teleportTo(p.serverLevel(), 62.6, (double)s.origin().getY(), 2.5, -90.0F, 38.0F);
               }
            )
      );
      s.add(t + 12, mc -> s.shot(mc, "poneglyph_desk_empty"));
      s.add(t + 13, mc -> s.server(mc, p -> {
            if (p.serverLevel().getBlockEntity(new BlockPos(64, s.origin().getY(), 2)) instanceof DecipherDeskBlockEntity be) {
               ItemStack bp = new ItemStack((ItemLike)Poneglyphs.ENCRYPTED_BLUEPRINT.get());
               bp.set((DataComponentType)Poneglyphs.ENCRYPTED.get(), new BlueprintData.Encrypted(ID, 20261011L, BlueprintData.Progress.EMPTY));
               be.items().setItem(0, bp);
               be.items().setItem(1, new ItemStack((ItemLike)Poneglyphs.PRIMER.get()));
            }
         }));
      s.add(t + 22, mc -> s.shot(mc, "poneglyph_desk_block"));
      s.add(
         t + 24,
         mc -> s.server(
               mc,
               p -> {
                  if (p.serverLevel().getBlockEntity(new BlockPos(64, s.origin().getY(), 2)) instanceof DecipherDeskBlockEntity be) {
                     ItemStack primer = be.items().removeItemNoUpdate(1);
                     be.items().setChanged();
                     p.getInventory().add(primer);
                     p.openMenu(
                        new SimpleMenuProvider(
                           (id, inv, pl) -> new DecipherDeskMenu(id, inv, be), Component.translatable("container.devilfruits.decipher_desk")
                        ),
                        buf -> buf.writeBlockPos(be.getBlockPos())
                     );
                  }
               }
            )
      );
      s.add(t + 30, mc -> s.shot(mc, "poneglyph_desk_no_primer"));
      s.add(t + 32, mc -> s.server(mc, p -> {
            if (p.containerMenu instanceof DecipherDeskMenu menu && menu.desk() != null) {
               menu.desk().items().setItem(1, new ItemStack((ItemLike)Poneglyphs.PRIMER.get()));
            }
         }));
      s.add(t + 42, mc -> s.shot(mc, "poneglyph_desk_primer"));
      s.add(t + 44, mc -> click(mc, false));
      s.add(t + 49, mc -> s.shot(mc, "poneglyph_desk_wrong"));
      int c = t + 44 + 110;

      for (int i = 0; i < 3; i++) {
         s.add(c + i * 30, mc -> click(mc, true));
      }

      s.add(c + 60 + 4, mc -> s.shot(mc, "poneglyph_desk_seal"));
      s.add(c + 60 + 30, mc -> s.shot(mc, "poneglyph_desk_success"));
      s.add(c + 60 + 31, mc -> {
         if (mc.player != null) {
            mc.player.closeContainer();
         }
      });
      s.add(c + 60 + 33, mc -> s.shot(mc, "poneglyph_desk_sealed"));
      t = c + 60 + 36;
      s.add(
         t,
         mc -> {
            if (mc.player != null) {
               mc.player.closeContainer();
            }

            s.server(
               mc,
               p -> {
                  if (p.serverLevel().getBlockEntity(new BlockPos(64, s.origin().getY(), 2)) instanceof DecipherDeskBlockEntity be) {
                     ItemStack out = be.items().removeItemNoUpdate(2);
                     be.items().setChanged();
                     p.setItemInHand(InteractionHand.MAIN_HAND, out);
                     out.use(p.serverLevel(), p, InteractionHand.MAIN_HAND);
                     DevilFruits.LOGGER
                        .info(
                           "[devilfruits autotest] deciphered and learned refining: {}, ingot recipe in the book: {}",
                           Knowledge.knows(p, ID),
                           p.getRecipeBook().contains(DevilFruits.id("seastone/seastone_ingot"))
                        );
                  }
               }
            );
         }
      );
      s.add(t + 20, mc -> s.shot(mc, "poneglyph_learned"));
      s.add(t + 24, mc -> mc.setScreen(new PrimerScreen()));
      s.add(t + 32, mc -> s.shot(mc, "poneglyph_primer_1"));
      s.add(t + 34, mc -> {
         if (mc.screen instanceof PrimerScreen screen) {
            screen.turn(1);
         }
      });
      s.add(t + 42, mc -> s.shot(mc, "poneglyph_primer_2"));
      s.add(t + 44, mc -> mc.setScreen(null));
      t += 50;
      s.add(
         t,
         mc -> s.server(
               mc,
               p -> {
                  ServerLevel level = p.serverLevel();
                  BlockPos origin = new BlockPos(60, s.origin().getY() - 1, 40);
                  PoneglyphRuins.Piece piece = new PoneglyphRuins.Piece(origin, Rotation.NONE, DevilFruits.id("seastone_weapons"), 2);
                  piece.postProcess(
                     level,
                     level.structureManager(),
                     level.getChunkSource().getGenerator(),
                     level.random,
                     new BoundingBox(origin.getX() - 10, origin.getY() - 10, origin.getZ() - 10, origin.getX() + 10, origin.getY() + 10, origin.getZ() + 10),
                     new ChunkPos(origin),
                     origin
                  );
                  p.teleportTo(level, 60.5, (double)(s.origin().getY() + 3), 53.5, 180.0F, 14.0F);
               }
            )
      );
      s.add(t + 25, mc -> s.shot(mc, "poneglyph_ruins"));
      s.add(t + 27, mc -> s.server(mc, p -> p.teleportTo(p.serverLevel(), 60.5, (double)s.origin().getY(), 42.5, 180.0F, 6.0F)));
      s.add(t + 45, mc -> s.shot(mc, "poneglyph_ruins_monolith"));
      return t + 50;
   }

   private static void click(Minecraft mc, boolean right) {
      PoneglyphNetwork.DeskStatePayload state = ClientPoneglyphState.desk;
      if (state != null && mc.gameMode != null && !state.options().isEmpty()) {
         int correct = state.options().indexOf(state.clue());
         int pick = right ? correct : (correct + 1) % state.options().size();
         DevilFruits.LOGGER
            .info(
               "[devilfruits autotest] desk round {}/{} clue '{}' options {} pick {}",
               new Object[]{state.round() + 1, state.rounds(), state.clue(), state.options().size(), right ? "right" : "wrong"}
            );
         mc.gameMode.handleInventoryButtonClick(state.containerId(), pick);
      } else {
         DevilFruits.LOGGER.info("[devilfruits autotest] desk: no puzzle state");
      }
   }
}
