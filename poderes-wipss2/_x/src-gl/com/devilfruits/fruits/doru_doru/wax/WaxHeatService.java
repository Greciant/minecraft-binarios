package com.devilfruits.fruits.doru_doru.wax;

import com.devilfruits.fruits.doru_doru.DoruDoru;
import com.devilfruits.fruits.doru_doru.DoruItems;
import com.devilfruits.fruits.doru_doru.DoruTags;
import com.devilfruits.fruits.doru_doru.DoruTriggers;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public final class WaxHeatService {
   public static final float SOFT_AT = 0.3F;
   public static final float MELT_AT = 0.7F;
   private static final int CHECK = 10;
   private static final int MELT_CHECK = 6;
   private static final int RING = 32;
   private static final int RADIUS = 4;
   private static final float REACH = 4.5F;
   private static final Map<ResourceKey<Level>, WaxHeatService.State> STATES = new HashMap<>();

   private static WaxHeatService.State state(ServerLevel level) {
      return STATES.computeIfAbsent(level.dimension(), k -> new WaxHeatService.State());
   }

   public static boolean tooHot(Level level) {
      return level.dimensionTypeRegistration().is(DoruTags.TOO_HOT);
   }

   public static float sourceWeight(BlockState state) {
      if (state.isAir() || !state.is(DoruTags.WAX_HEAT)) {
         return 0.0F;
      } else if (state.hasProperty(BlockStateProperties.LIT) && !(Boolean)state.getValue(BlockStateProperties.LIT)) {
         return 0.0F;
      } else {
         Block b = state.getBlock();
         if (b == Blocks.LAVA || b == Blocks.FIRE || b == Blocks.SOUL_FIRE) {
            return 1.0F;
         } else if (b == Blocks.LAVA_CAULDRON || state.is(BlockTags.CAMPFIRES)) {
            return 0.8F;
         } else if (b == Blocks.MAGMA_BLOCK) {
            return 0.6F;
         } else {
            return !state.is(BlockTags.CANDLES) && !state.is(BlockTags.CANDLE_CAKES) ? 0.5F : 0.12F;
         }
      }
   }

   public static void poke(ServerLevel level, BlockPos pos) {
      schedule(level, pos, 1);
   }

   public static void schedule(ServerLevel level, BlockPos pos, int delay) {
      WaxHeatService.State st = state(level);
      long key = pos.asLong();
      if (st.queued.add(key)) {
         st.ring[(int)((st.tick + (long)Math.max(1, Math.min(31, delay))) % 32L)].add(key);
      }
   }

   public static void pokeAround(ServerLevel level, BlockPos center, int radius) {
      MutableBlockPos m = new MutableBlockPos();

      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            if (level.hasChunk(center.getX() + dx >> 4, center.getZ() + dz >> 4)) {
               for (int dy = -radius; dy <= radius; dy++) {
                  m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                  if (level.getBlockState(m).getBlock() instanceof WaxBlock) {
                     poke(level, m);
                  }
               }
            }
         }
      }
   }

   public static void heatBurst(ServerLevel level, Vec3 pos, float weight, int ticks) {
      WaxHeatService.State st = state(level);
      st.points.add(new WaxHeatService.Hot(pos, weight, level.getGameTime() + (long)ticks));
      pokeAround(level, BlockPos.containing(pos), 4);
   }

   public static void sear(ServerLevel level, Vec3 at, double radius, @Nullable Entity source) {
      int r = Mth.ceil(radius) + 2;
      double melt = (radius + 0.5) * (radius + 0.5);
      BlockPos c = BlockPos.containing(at);
      MutableBlockPos m = new MutableBlockPos();
      ItemParticleOption drops = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack((ItemLike)DoruItems.WAX_LUMP.get()));
      int melted = 0;
      boolean near = false;

      for (int dx = -r; dx <= r; dx++) {
         for (int dz = -r; dz <= r; dz++) {
            if (level.hasChunk(c.getX() + dx >> 4, c.getZ() + dz >> 4)) {
               for (int dy = -r; dy <= r; dy++) {
                  m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                  if (level.getBlockState(m).getBlock() instanceof WaxBlock) {
                     near = true;
                     Vec3 b = Vec3.atCenterOf(m);
                     if (!(b.distanceToSqr(at) > melt)) {
                        if (source instanceof ServerPlayer) {
                           ServerPlayer p = (ServerPlayer)source;
                           if (!level.mayInteract(p, m)) {
                              continue;
                           }
                        }

                        level.sendParticles(drops, b.x, b.y, b.z, 4, 0.3, 0.3, 0.3, 0.06);
                        level.sendParticles(ParticleTypes.CLOUD, b.x, b.y + 0.3, b.z, 2, 0.25, 0.2, 0.25, 0.02);
                        WaxWorld.remove(level, m.immutable());
                        melted++;
                     }
                  }
               }
            }
         }
      }

      if (melted > 0) {
         level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.9F, 0.7F + level.random.nextFloat() * 0.3F);
         level.playSound(null, at.x, at.y, at.z, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 0.7F, 0.6F);
      }

      if (near) {
         heatBurst(level, at, 3.0F, 40);
      }
   }

   public static boolean nearHeat(ServerLevel level, BlockPos pos) {
      for (Direction d : Direction.values()) {
         if (sourceWeight(level.getBlockState(pos.relative(d))) > 0.0F) {
            return true;
         }
      }

      return false;
   }

   public static boolean cooled(ServerLevel level, BlockPos pos) {
      for (Direction d : Direction.values()) {
         if (d != Direction.DOWN && level.getFluidState(pos.relative(d)).is(FluidTags.WATER)) {
            return true;
         }
      }

      return level.isRainingAt(pos.above());
   }

   public static float heatAt(ServerLevel level, BlockPos pos) {
      if (tooHot(level)) {
         return 1.5F;
      } else {
         float heat = 0.0F;
         MutableBlockPos m = new MutableBlockPos();

         for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
               if (level.hasChunk(pos.getX() + dx >> 4, pos.getZ() + dz >> 4)) {
                  for (int dy = -4; dy <= 4; dy++) {
                     int d2 = dx * dx + dy * dy + dz * dz;
                     if (!((float)d2 > 20.25F) && d2 != 0) {
                        BlockState s = level.getBlockState(m.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz));
                        float w = sourceWeight(s);
                        if (w > 0.0F) {
                           heat += w * falloff((float)Math.sqrt((double)d2));
                        }
                     }
                  }
               }
            }
         }

         WaxHeatService.State st = STATES.get(level.dimension());
         if (st != null && !st.points.isEmpty()) {
            Vec3 c = Vec3.atCenterOf(pos);

            for (WaxHeatService.Hot h : st.points) {
               heat += h.weight * falloff((float)h.pos.distanceTo(c));
            }
         }

         return Math.min(3.0F, heat);
      }
   }

   private static float falloff(float d) {
      return d <= 1.0F ? 1.0F : Math.max(0.0F, 1.0F - (d - 1.0F) / 3.5F);
   }

   public static void tick(ServerLevel level) {
      WaxHeatService.State st = STATES.get(level.dimension());
      if (st != null) {
         st.tick++;
         long now = level.getGameTime();
         st.points.removeIf(h -> h.until < now);
         LongArrayList due = st.ring[(int)(st.tick % 32L)];
         if (!due.isEmpty()) {
            st.ring[(int)(st.tick % 32L)] = new LongArrayList();
            int cap = (int)DoruDoru.setting("maxHeatChecksPerTick");
            int done = 0;

            int i;
            for (i = 0; i < due.size() && done < cap; i++) {
               long key = due.getLong(i);
               st.queued.remove(key);
               BlockPos pos = BlockPos.of(key);
               if (level.isLoaded(pos)) {
                  BlockEntity next = level.getBlockEntity(pos);
                  if (next instanceof WaxBlockEntity) {
                     WaxBlockEntity be = (WaxBlockEntity)next;
                     done++;
                     int nextx = step(level, pos, be);
                     if (nextx > 0) {
                        schedule(level, pos, nextx);
                     }
                  }
               }
            }

            while (i < due.size()) {
               long key = due.getLong(i);
               st.queued.remove(key);
               schedule(level, BlockPos.of(key), 1);
               i++;
            }
         }
      }
   }

   private static int step(ServerLevel level, BlockPos pos, WaxBlockEntity be) {
      long now = level.getGameTime();
      boolean doomed = be.expires() != 0L && now >= be.expires();
      float heat = doomed ? 1.0F : heatAt(level, pos);
      boolean cool = !doomed && cooled(level, pos);
      float p = be.heat();
      if (cool) {
         p = Math.max(0.0F, p - 0.25F);
      } else if (heat > 0.0F) {
         float biome = ((Biome)level.getBiome(pos).value()).getBaseTemperature() >= 1.5F ? (float)DoruDoru.setting("hotBiomeMeltMultiplier") : 1.0F;
         p = Math.min(1.5F, p + heat * (float)DoruDoru.setting("meltRate") * biome * (doomed ? 2.5F : 1.0F));
      } else {
         p = Math.max(0.0F, p - 0.04F);
      }

      int melt = p >= 0.7F && !cool ? 2 : (p >= 0.3F ? 1 : 0);
      boolean changed = melt != be.melt();
      be.setHeat(p);
      be.setMelt(melt);
      if (melt == 2) {
         long mask = drain(be.mask(), pos, now);
         if (mask == 0L) {
            if (!doomed && be.kind() == 1) {
               wallMelted(level, pos, be.owner());
            }

            gone(level, pos, doomed && (be.kind() == 0 || be.kind() == 4));
            return 0;
         } else {
            WaxWorld.setMask(level, pos, be, mask);
            if (level.random.nextInt(4) == 0) {
               level.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 0.35F, 0.6F + level.random.nextFloat() * 0.3F);
            }

            return 6;
         }
      } else {
         if (changed) {
            WaxWorld.sync(level, pos);
         }

         return !(p > 0.0F) && !(heat > 0.0F) ? 0 : 10;
      }
   }

   static long drain(long mask, BlockPos pos, long now) {
      int top = WaxMask.top(mask);
      if (top < 0) {
         return 0L;
      } else {
         long seed = pos.asLong() * -7046029254386353131L ^ now * -4417276706812531889L;
         long out = mask;
         boolean removed = false;

         for (int i = 0; i < 16; i++) {
            int x = i & 3;
            int z = i >> 2;
            if (WaxMask.has(out, x, top, z)) {
               seed = seed * 6364136223846793005L + 1442695040888963407L;
               int r = (int)(seed >>> 59);
               if (r < 18) {
                  out = WaxMask.without(out, x, top, z);
                  removed = true;
                  if (r < 6) {
                     for (int y = 0; y < top; y++) {
                        if (!WaxMask.has(out, x, y, z)) {
                           out = WaxMask.with(out, x, y, z);
                           break;
                        }
                     }
                  }
               }
            }
         }

         if (!removed) {
            out = WaxMask.withLayer(out, top, 0);
         }

         return out;
      }
   }

   private static void gone(ServerLevel level, BlockPos pos, boolean doomed) {
      Vec3 c = Vec3.atBottomCenterOf(pos);
      level.sendParticles(
         new ItemParticleOption(ParticleTypes.ITEM, new ItemStack((ItemLike)DoruItems.WAX_LUMP.get())), c.x, c.y + 0.2, c.z, 6, 0.3, 0.1, 0.3, 0.02
      );
      level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.25F, 1.4F + level.random.nextFloat() * 0.4F);
      WaxWorld.remove(level, pos);
      if (doomed && level.random.nextFloat() < 0.35F) {
         ItemEntity lump = new ItemEntity(level, c.x, c.y + 0.1, c.z, new ItemStack((ItemLike)DoruItems.WAX_LUMP.get()));
         lump.setDefaultPickUpDelay();
         level.addFreshEntity(lump);
      }
   }

   private static void wallMelted(ServerLevel level, BlockPos pos, int owner) {
      if (owner >= 0 && !BlockPos.betweenClosedStream(pos.offset(-2, -2, -2), pos.offset(2, 2, 2)).noneMatch(p -> level.getFluidState(p).is(FluidTags.LAVA))) {
         UUID id = WaxOwners.get(level.getServer()).owner(owner);
         ServerPlayer player = id == null ? null : level.getServer().getPlayerList().getPlayer(id);
         if (player != null) {
            DoruTriggers.MELT_WALL.get().trigger(player);
         }
      }
   }

   public static void clear(ServerLevel level) {
      STATES.remove(level.dimension());
   }

   public static int queued(ServerLevel level) {
      WaxHeatService.State st = STATES.get(level.dimension());
      return st == null ? 0 : st.queued.size();
   }

   static Iterator<ResourceKey<Level>> levels() {
      return STATES.keySet().iterator();
   }

   private WaxHeatService() {
   }

   private static record Hot(Vec3 pos, float weight, long until) {
   }

   private static final class State {
      final LongArrayList[] ring = new LongArrayList[32];
      final LongOpenHashSet queued = new LongOpenHashSet();
      final List<WaxHeatService.Hot> points = new ArrayList<>();
      long tick;

      State() {
         for (int i = 0; i < 32; i++) {
            this.ring[i] = new LongArrayList();
         }
      }
   }
}
