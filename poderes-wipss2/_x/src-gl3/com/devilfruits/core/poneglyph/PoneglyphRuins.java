package com.devilfruits.core.poneglyph;

import com.devilfruits.DevilFruits;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.Random;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationStub;
import net.minecraft.world.level.levelgen.structure.Structure.StructureSettings;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

public class PoneglyphRuins extends Structure {
   public static final MapCodec<PoneglyphRuins> CODEC = simpleCodec(PoneglyphRuins::new);
   public static final ResourceKey<LootTable> CHEST = ResourceKey.create(Registries.LOOT_TABLE, DevilFruits.id("chests/poneglyph_ruins"));
   static final int RADIUS = 6;

   public PoneglyphRuins(StructureSettings settings) {
      super(settings);
   }

   public StructureType<?> type() {
      return Poneglyphs.RUINS.get();
   }

   protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
      if (ctx.random().nextDouble() >= (Double)PoneglyphConfig.RUINS_CHANCE.get()) {
         return Optional.empty();
      } else {
         ChunkPos chunk = ctx.chunkPos();
         int x = chunk.getMiddleBlockX();
         int z = chunk.getMiddleBlockZ();
         ChunkGenerator gen = ctx.chunkGenerator();
         LevelHeightAccessor heights = ctx.heightAccessor();
         RandomState rs = ctx.randomState();
         int center = gen.getFirstOccupiedHeight(x, z, Types.WORLD_SURFACE_WG, heights, rs);
         int ground = gen.getFirstOccupiedHeight(x, z, Types.OCEAN_FLOOR_WG, heights, rs);
         if (center == ground && center >= gen.getSeaLevel()) {
            int min = center;
            int max = center;

            for (int dx = -6; dx <= 6; dx += 12) {
               for (int dz = -6; dz <= 6; dz += 12) {
                  int y = gen.getFirstOccupiedHeight(x + dx, z + dz, Types.OCEAN_FLOOR_WG, heights, rs);
                  min = Math.min(min, y);
                  max = Math.max(max, y);
               }
            }

            if (max - min > 6) {
               return Optional.empty();
            } else {
               ResourceLocation id = pick(ctx.seed() ^ BlockPos.asLong(x, 0, z));
               if (id == null) {
                  return Optional.empty();
               } else {
                  PoneglyphData.Poneglyph def = PoneglyphData.poneglyph(id);
                  Rotation rotation = Rotation.getRandom(ctx.random());
                  BlockPos origin = new BlockPos(x, center, z);
                  return Optional.of(
                     new GenerationStub(origin, b -> b.addPiece(new PoneglyphRuins.Piece(origin, rotation, id, def == null ? 0 : def.carvingVariant())))
                  );
               }
            }
         } else {
            return Optional.empty();
         }
      }
   }

   @Nullable
   public static ResourceLocation pick(long seed) {
      int total = 0;

      for (PoneglyphData.Poneglyph p : PoneglyphData.poneglyphs().values()) {
         total += p.weight();
      }

      if (total <= 0) {
         return null;
      } else {
         int roll = new Random(seed).nextInt(total);

         for (Entry<ResourceLocation, PoneglyphData.Poneglyph> e : PoneglyphData.poneglyphs().entrySet()) {
            roll -= e.getValue().weight();
            if (roll < 0) {
               return e.getKey();
            }
         }

         return null;
      }
   }

   public static class Piece extends StructurePiece {
      private final BlockPos origin;
      private final Rotation rotation;
      private final ResourceLocation poneglyph;
      private final int carving;

      public Piece(BlockPos origin, Rotation rotation, ResourceLocation poneglyph, int carving) {
         super(
            Poneglyphs.RUINS_PIECE.get(),
            0,
            new BoundingBox(origin.getX() - 6 - 1, origin.getY() - 9, origin.getZ() - 6 - 1, origin.getX() + 6 + 1, origin.getY() + 8, origin.getZ() + 6 + 1)
         );
         this.origin = origin;
         this.rotation = rotation;
         this.poneglyph = poneglyph;
         this.carving = carving;
         this.setOrientation(null);
      }

      public Piece(CompoundTag tag) {
         super(Poneglyphs.RUINS_PIECE.get(), tag);
         this.origin = new BlockPos(tag.getInt("OX"), tag.getInt("OY"), tag.getInt("OZ"));
         this.rotation = Rotation.values()[Math.floorMod(tag.getInt("Rot"), 4)];
         ResourceLocation id = ResourceLocation.tryParse(tag.getString("Poneglyph"));
         this.poneglyph = id != null ? id : DevilFruits.id("seastone_refining");
         this.carving = tag.getInt("Carving");
         this.setOrientation(null);
      }

      protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
         tag.putInt("OX", this.origin.getX());
         tag.putInt("OY", this.origin.getY());
         tag.putInt("OZ", this.origin.getZ());
         tag.putInt("Rot", this.rotation.ordinal());
         tag.putString("Poneglyph", this.poneglyph.toString());
         tag.putInt("Carving", this.carving);
      }

      private BlockPos world(int lx, int ly, int lz) {
         return this.origin.offset(new BlockPos(lx, ly, lz).rotate(this.rotation));
      }

      private int noise(int lx, int ly, int lz, int salt) {
         long h = this.origin.asLong() * -7046029254386353131L + (long)lx * 73856093L
            ^ (long)ly * 19349663L
            ^ (long)lz * 83492791L
            ^ (long)salt * 2685821657736338717L;
         h ^= h >>> 29;
         h *= -4658895280553007687L;
         h ^= h >>> 32;
         return (int)(h & 2147483647L);
      }

      private void put(WorldGenLevel level, BoundingBox box, int lx, int ly, int lz, BlockState state) {
         BlockPos pos = this.world(lx, ly, lz);
         if (box.isInside(pos)) {
            level.setBlock(pos, state.rotate(this.rotation), 2);
         }
      }

      private BlockState brick(int lx, int ly, int lz) {
         int n = this.noise(lx, ly, lz, 1) % 100;
         return n < 45
            ? Blocks.STONE_BRICKS.defaultBlockState()
            : (
               n < 78
                  ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
                  : (n < 94 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.MOSSY_COBBLESTONE.defaultBlockState())
            );
      }

      public void postProcess(
         WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot
      ) {
         int r = 6;

         for (int lx = -r; lx <= r; lx++) {
            for (int lz = -r; lz <= r; lz++) {
               for (int ly = 1; ly <= 7; ly++) {
                  this.put(level, box, lx, ly, lz, Blocks.AIR.defaultBlockState());
               }

               int n = this.noise(lx, 0, lz, 2) % 100;
               this.put(
                  level,
                  box,
                  lx,
                  0,
                  lz,
                  n < 8 ? Blocks.MOSS_BLOCK.defaultBlockState() : (n < 14 ? Blocks.GRASS_BLOCK.defaultBlockState() : this.brick(lx, 0, lz))
               );

               for (int ly = -1; ly >= -8; ly--) {
                  BlockPos pos = this.world(lx, ly, lz);
                  if (box.isInside(pos)) {
                     BlockState there = level.getBlockState(pos);
                     if (!there.isAir() && there.getFluidState().isEmpty() && !there.canBeReplaced()) {
                        break;
                     }

                     level.setBlock(
                        pos, this.noise(lx, ly, lz, 3) % 3 == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState(), 2
                     );
                  }
               }
            }
         }

         for (int lx = -r; lx <= r; lx++) {
            for (int lz = -r; lz <= r; lz++) {
               boolean back = lz == -r;
               boolean front = lz == r;
               boolean side = Math.abs(lx) == r;
               if ((back || front || side) && (!front || Math.abs(lx) > 1)) {
                  int height = back ? 3 + this.noise(lx, 0, lz, 4) % 3 : (front ? this.noise(lx, 0, lz, 4) % 3 : this.noise(lx, 0, lz, 4) % 4);

                  for (int lyx = 1; lyx <= height; lyx++) {
                     this.put(level, box, lx, lyx, lz, this.brick(lx, lyx, lz));
                  }

                  if (height > 0 && this.noise(lx, height, lz, 5) % 3 == 0) {
                     this.put(level, box, lx, height + 1, lz, Blocks.MOSSY_STONE_BRICK_SLAB.defaultBlockState());
                  }

                  if (height > 1 && this.noise(lx, 1, lz, 6) % 3 == 0) {
                     Direction out = side ? (lx > 0 ? Direction.EAST : Direction.WEST) : (back ? Direction.NORTH : Direction.SOUTH);
                     int lxOut = lx + out.getStepX();
                     int lzOut = lz + out.getStepZ();

                     for (int lyx = height; lyx >= 1 && this.noise(lx, lyx, lz, 7) % 5 != 0; lyx--) {
                        this.put(
                           level,
                           box,
                           lxOut,
                           lyx,
                           lzOut,
                           (BlockState)Blocks.VINE.defaultBlockState().setValue((Property)VineBlock.PROPERTY_BY_DIRECTION.get(out.getOpposite()), true)
                        );
                     }
                  }
               }
            }
         }

         for (int px : new int[]{-3, 3}) {
            for (int pz : new int[]{-5, -1}) {
               for (int lyx = 1; lyx <= 4; lyx++) {
                  this.put(level, box, px, lyx, pz, lyx == 4 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : this.brick(px, lyx, pz));
               }
            }
         }

         for (int lx = -3; lx <= 3; lx++) {
            for (int lzx = -5; lzx <= -1; lzx++) {
               if (this.noise(lx, 5, lzx, 8) % 9 != 0) {
                  BlockState roof = lzx == -1
                     ? (BlockState)Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH)
                     : (BlockState)(this.noise(lx, 5, lzx, 9) % 3 == 0 ? Blocks.MOSSY_STONE_BRICK_SLAB : Blocks.STONE_BRICK_SLAB)
                        .defaultBlockState()
                        .setValue(SlabBlock.TYPE, SlabType.BOTTOM);
                  this.put(level, box, lx, 5, lzx, roof);
                  if (this.noise(lx, 6, lzx, 10) % 4 == 0) {
                     this.put(level, box, lx, 6, lzx, (BlockState)Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
                  }
               }
            }
         }

         for (int lx = -1; lx <= 1; lx++) {
            for (int lyx = 1; lyx <= 3; lyx++) {
               for (int lzxx = -4; lzxx <= -2; lzxx++) {
                  if (lzxx == -2) {
                     int cell = (3 - lyx) * 3 + lx + 1;
                     this.placeFace(level, box, lx, lyx, lzxx, cell);
                  } else {
                     this.put(level, box, lx, lyx, lzxx, ((Block)Poneglyphs.PONEGLYPH_STONE.get()).defaultBlockState());
                  }
               }
            }
         }

         for (int lx = -1; lx <= 1; lx++) {
            for (int lzxxx = -4; lzxxx <= -2; lzxxx++) {
               if (this.noise(lx, 4, lzxxx, 12) % 3 != 0) {
                  this.put(level, box, lx, 4, lzxxx, Blocks.MOSS_CARPET.defaultBlockState());
               }
            }
         }

         for (int side : new int[]{-2, 2}) {
            Direction toward = side < 0 ? Direction.EAST : Direction.WEST;

            for (int lzxxxx = -4; lzxxxx <= -2; lzxxxx++) {
               for (int lyx = 3; lyx >= 1 && this.noise(side, lyx, lzxxxx, 13) % 3 != 0; lyx--) {
                  this.put(
                     level,
                     box,
                     side,
                     lyx,
                     lzxxxx,
                     (BlockState)Blocks.VINE.defaultBlockState().setValue((Property)VineBlock.PROPERTY_BY_DIRECTION.get(toward), true)
                  );
               }
            }
         }

         for (int lx = -r + 1; lx <= r - 1; lx++) {
            for (int lzxxxx = -r + 1; lzxxxx <= r - 1; lzxxxx++) {
               boolean monolith = Math.abs(lx) <= 1 && lzxxxx >= -4 && lzxxxx <= -2;
               boolean pillar = Math.abs(lx) == 3 && (lzxxxx == -5 || lzxxxx == -1);
               if (!monolith && !pillar) {
                  int n = this.noise(lx, 1, lzxxxx, 11) % 100;
                  BlockState plant = n < 14
                     ? Blocks.MOSS_CARPET.defaultBlockState()
                     : (n < 20 ? Blocks.SHORT_GRASS.defaultBlockState() : (n < 23 ? Blocks.FERN.defaultBlockState() : null));
                  if (plant != null) {
                     this.put(level, box, lx, 1, lzxxxx, plant);
                  }
               }
            }
         }

         this.put(level, box, -2, 4, -4, Blocks.COBWEB.defaultBlockState());
         this.put(level, box, 2, 4, -2, Blocks.COBWEB.defaultBlockState());
         BlockPos chest = this.world(4, 1, -4);
         if (box.isInside(chest)) {
            this.createChest(
               level,
               box,
               random,
               chest,
               PoneglyphRuins.CHEST,
               (BlockState)Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, this.rotation.rotate(Direction.WEST))
            );
         }
      }

      private void placeFace(WorldGenLevel level, BoundingBox box, int lx, int ly, int lz, int cell) {
         BlockPos pos = this.world(lx, ly, lz);
         if (box.isInside(pos)) {
            BlockState state = (BlockState)((BlockState)((BlockState)((PoneglyphFaceBlock)Poneglyphs.PONEGLYPH_FACE.get())
                     .defaultBlockState()
                     .setValue(PoneglyphFaceBlock.FACING, this.rotation.rotate(Direction.SOUTH)))
                  .setValue(PoneglyphFaceBlock.CELL, cell))
               .setValue(PoneglyphFaceBlock.CARVING, Math.max(0, Math.min(3, this.carving)));
            level.setBlock(pos, state, 2);
            if (level.getBlockEntity(pos) instanceof PoneglyphFaceBlockEntity be) {
               be.setPoneglyph(this.poneglyph);
            }
         }
      }
   }
}
