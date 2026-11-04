package com.devilfruits.core.physics;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.fx.CoreFx;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.haki.HakiCombat;
import com.devilfruits.core.poneglyph.Poneglyphs;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.core.world.DebrisEntity;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.registry.DFTags;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;

public final class BlockImpactService {
   private static final Map<ServerLevel, Map<UUID, ArrayDeque<BlockImpactService.PendingBreak>>> PENDING = new HashMap<>();
   private static final Map<UUID, long[]> UNBREAKABLE_CACHE = new ConcurrentHashMap<>();

   public static int strike(AbilityContext ctx, ImpactShape shape, Vec3 dir) {
      return strike(ctx, shape, dir, 1.0);
   }

   public static int strike(AbilityContext ctx, ImpactShape shape, Vec3 dir, double power) {
      return strike(ctx, shape, dir, power, null);
   }

   public static int strike(AbilityContext ctx, ImpactShape shape, Vec3 dir, double power, @Nullable Set<UUID> once) {
      if (!PhysicsWorld.active()) {
         return 0;
      } else {
         PhysicsProfile profile = ctx.ability().physics();
         boolean coated = coat(ctx) > 1.0;
         double strength = profile.strength((double)ctx.damage(), (double)ctx.knockback()) * power * coat(ctx);
         double chance = ctx.blockDamageAllowed()
            ? profile.blockDamage() * Math.min(1.0, power) * (coated ? 1.0 + (Double)PhysicsConfig.ARMAMENT_BLOCK_BONUS.get() : 1.0)
            : 0.0;
         return apply(
            new BlockImpactService.Strike(ctx.player(), ctx.fruit().id(), profile.impact(), strength, chance, true, ctx.budget(), decalOf(profile)),
            shape,
            dir,
            once
         );
      }
   }

   public static int strike(
      ServerPlayer caster, Ability ability, ImpactShape shape, Vec3 dir, double strength, double blockChance, CastBudget budget, @Nullable Set<UUID> once
   ) {
      if (!PhysicsWorld.active()) {
         return 0;
      } else {
         PhysicsProfile profile = ability.physics();
         double chance = ability.config().blockDamage() && TerrainDestruction.destructionAllowed(caster.serverLevel()) ? blockChance : 0.0;
         return apply(
            new BlockImpactService.Strike(caster, ability.fruit().id(), profile.impact(), strength, chance, true, budget, decalOf(profile)), shape, dir, once
         );
      }
   }

   public static int push(AbilityContext ctx, ImpactShape shape, Vec3 dir, double power) {
      return push(ctx, shape, dir, power, ctx.ability().physics().impact());
   }

   public static int push(AbilityContext ctx, ImpactShape shape, Vec3 dir, double power, PhysicsProfile.Impact impact) {
      if (PhysicsWorld.active()
         && impact != PhysicsProfile.Impact.NONE
         && (impact != PhysicsProfile.Impact.TANGENTIAL || (Boolean)PhysicsConfig.DRAG_EFFECTS.get())) {
         double strength = ctx.ability().physics().strength((double)ctx.damage(), (double)ctx.knockback()) * power * coat(ctx);
         return apply(new BlockImpactService.Strike(ctx.player(), ctx.fruit().id(), impact, strength, 0.0, true, ctx.budget(), decalOf(impact)), shape, dir);
      } else {
         return 0;
      }
   }

   public static int damage(AbilityContext ctx, ImpactShape shape, Vec3 dir, double power) {
      return damage(ctx, shape, dir, power, true, ctx.budget());
   }

   public static int damage(AbilityContext ctx, ImpactShape shape, Vec3 dir, double power, boolean anyHardness, CastBudget budget) {
      if (PhysicsWorld.active() && ctx.blockDamageAllowed()) {
         double chance = ctx.ability().physics().blockDamage()
            * Math.min(1.0, power)
            * (coat(ctx) > 1.0 ? 1.0 + (Double)PhysicsConfig.ARMAMENT_BLOCK_BONUS.get() : 1.0);
         return chance <= 0.0
            ? 0
            : apply(
               new BlockImpactService.Strike(ctx.player(), ctx.fruit().id(), PhysicsProfile.Impact.NONE, 0.0, chance, anyHardness, budget, null), shape, dir
            );
      } else {
         return 0;
      }
   }

   public static int ignite(AbilityContext ctx, ImpactShape shape, double chance) {
      return PhysicsWorld.active() && ctx.blockDamageAllowed() ? ignite(ctx.player(), ctx.fruit().id(), shape, chance, ctx.budget()) : 0;
   }

   public static void slam(AbilityContext ctx, Vec3 at, double radius, double power) {
      if (PhysicsWorld.active()) {
         push(ctx, ImpactShape.sphere(at, radius), new Vec3(0.0, -1.0, 0.0), power, PhysicsProfile.Impact.RADIAL);
         ServerPlayer player = ctx.player();
         PhysicsStructure under = PhysicsWorld.structureUnder(player);
         if (under != null && (Boolean)PhysicsConfig.IMPULSES.get() && PhysicsConfig.enabledFor(ctx.fruit().id()) && allowed(player, under)) {
            double j = ctx.ability().physics().strength((double)ctx.damage(), (double)ctx.knockback())
               * 0.5
               * power
               * (Double)PhysicsConfig.IMPULSE_MULTIPLIER.get();
            Vec3 feet = player.position();
            double got = StructureForces.apply(under, feet, new Vec3(0.0, -j, 0.0));
            BlockPos local = BlockPos.containing(under.toLocal(feet.add(0.0, -0.3, 0.0)));
            if (got > 0.0) {
               StructureForces.impactFx(player.serverLevel(), under, local, feet, new Vec3(0.0, -1.0, 0.0), got);
            }

            decal(
               player.serverLevel(),
               under,
               local,
               under.dirToLocal(new Vec3(0.0, 1.0, 0.0)),
               BlockImpactService.Decal.DENT,
               (float)Math.min(4.0, 1.0 + radius * 0.3)
            );
         }
      }
   }

   public static void mark(ServerPlayer caster, ImpactShape shape, Vec3 facing, BlockImpactService.Decal kind, float size) {
      if (PhysicsWorld.active()) {
         List<StructureBlock> top = PhysicsWorld.blocksIn(caster.serverLevel(), shape, null, 1);
         if (!top.isEmpty()) {
            StructureBlock b = top.get(0);
            decal(caster.serverLevel(), b.structure(), b.localPos(), b.structure().dirToLocal(facing), kind, size);
         }
      }
   }

   private static double coat(AbilityContext ctx) {
      return ctx.ability().coatable() && HakiCombat.coatsTechnique(ctx.player()) ? 1.3 : 1.0;
   }

   @Nullable
   private static BlockImpactService.Decal decalOf(PhysicsProfile profile) {
      return decalOf(profile.impact());
   }

   @Nullable
   private static BlockImpactService.Decal decalOf(PhysicsProfile.Impact impact) {
      return switch (impact) {
         case RADIAL -> BlockImpactService.Decal.RING;
         case DIRECTIONAL -> BlockImpactService.Decal.CRACK;
         default -> null;
      };
   }

   public static int apply(BlockImpactService.Strike s, ImpactShape shape, Vec3 dir) {
      return apply(s, shape, dir, null);
   }

   public static int apply(BlockImpactService.Strike s, ImpactShape shape, Vec3 dir, @Nullable Set<UUID> once) {
      if (PhysicsWorld.active() && PhysicsConfig.enabledFor(s.fruit())) {
         ServerPlayer caster = s.caster();
         ServerLevel level = caster.serverLevel();
         List<StructureBlock> blocks = PhysicsWorld.blocksIn(level, shape, st -> allowed(caster, st), 4096);
         if (blocks.isEmpty()) {
            return 0;
         } else {
            Map<UUID, List<StructureBlock>> byStructure = new LinkedHashMap<>();

            for (StructureBlock b : blocks) {
               byStructure.computeIfAbsent(b.structure().id(), k -> new ArrayList<>()).add(b);
            }

            for (List<StructureBlock> part : byStructure.values()) {
               PhysicsStructure structure = part.get(0).structure();
               if (once == null || once.add(structure.id())) {
                  StructureForces.strike(caster, structure, part, shape, dir, s.impact(), s.strength());
                  if (s.decal() != null) {
                     StructureBlock top = part.get(0);
                     decal(
                        level,
                        structure,
                        top.localPos(),
                        structure.dirToLocal(dir.scale(-1.0)),
                        s.decal(),
                        (float)Math.min(4.0, 0.8 + (double)part.size() * 0.05)
                     );
                  }
               }
            }

            if (s.blockChance() > 0.0 && (Boolean)PhysicsConfig.BLOCK_DAMAGE.get()) {
               double factor = s.blockChance() * (Double)PhysicsConfig.BLOCK_DAMAGE_MULTIPLIER.get();
               Vec3 d = dir.lengthSqr() < 1.0E-8 ? Vec3.ZERO : dir.normalize();

               for (StructureBlock b : blocks) {
                  if (caster.getRandom().nextDouble() < factor * b.power()) {
                     Vec3 burst = d.scale(0.25 + 0.25 * b.power()).add(0.0, 0.2, 0.0);
                     queueBreak(level, caster, b.structure(), b.localPos(), burst, s.anyHardness(), s.budget());
                  }
               }
            }

            return blocks.size();
         }
      } else {
         return 0;
      }
   }

   public static int pushFrom(Entity source, ImpactShape shape, Vec3 dir, double strength, PhysicsProfile.Impact impact) {
      if (PhysicsWorld.active() && PhysicsConfig.enabledFor(null) && !(strength <= 0.0) && impact != PhysicsProfile.Impact.NONE) {
         List<StructureBlock> blocks = PhysicsWorld.blocksIn(source.level(), shape, st -> allowed(source, st), 4096);
         Map<UUID, List<StructureBlock>> byStructure = new LinkedHashMap<>();

         for (StructureBlock blk : blocks) {
            byStructure.computeIfAbsent(blk.structure().id(), k -> new ArrayList<>()).add(blk);
         }

         for (List<StructureBlock> part : byStructure.values()) {
            StructureForces.strike(source, part.get(0).structure(), part, shape, dir, impact, strength);
         }

         return blocks.size();
      } else {
         return 0;
      }
   }

   public static boolean allowed(Entity caster, PhysicsStructure s) {
      if (holdsUnbreakable(s)) {
         return false;
      } else if ((Boolean)PhysicsConfig.OTHER_PLAYERS_STRUCTURES.get()) {
         return true;
      } else {
         PhysicsStructure under = PhysicsWorld.structureUnder(caster);
         return under != null && under.id().equals(s.id());
      }
   }

   public static boolean holdsUnbreakable(PhysicsStructure s) {
      long now = s.level().getGameTime();
      long[] cached = UNBREAKABLE_CACHE.get(s.id());
      if (cached != null && now - cached[0] < 200L) {
         return cached[1] != 0L;
      } else {
         AABB b = s.localBounds();
         boolean found = false;
         if ((b.getXsize() + 1.0) * (b.getYsize() + 1.0) * (b.getZsize() + 1.0) <= 65536.0) {
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(b.minX, b.minY, b.minZ), BlockPos.containing(b.maxX, b.maxY, b.maxZ))) {
               if (s.level().getBlockState(pos).is(Poneglyphs.UNBREAKABLE)) {
                  found = true;
                  break;
               }
            }
         }

         UNBREAKABLE_CACHE.put(s.id(), new long[]{now, found ? 1L : 0L});
         return found;
      }
   }

   public static boolean breakStructureBlock(
      ServerLevel level, ServerPlayer caster, PhysicsStructure s, BlockPos pos, @Nullable Vec3 velocity, CastBudget budget, boolean anyHardness
   ) {
      return PhysicsConfig.enabledFor(null) && PhysicsConfig.BLOCK_DAMAGE.get() && allowed(caster, s)
         ? queueBreak(level, caster, s, pos, velocity != null ? velocity : new Vec3(0.0, 0.2, 0.0), anyHardness, budget)
         : false;
   }

   public static boolean queueBreak(
      ServerLevel level, ServerPlayer caster, PhysicsStructure s, BlockPos pos, Vec3 burst, boolean anyHardness, CastBudget budget
   ) {
      BlockState state = level.getBlockState(pos);
      if (canBreak(level, caster, s, pos, state, anyHardness) && budget.hasBlocks() && budget.takeStructureBlock() && budget.takeBlock()) {
         boolean debris = budget.takeDebris();
         PENDING.computeIfAbsent(level, k -> new HashMap<>())
            .computeIfAbsent(s.id(), k -> new ArrayDeque<>())
            .add(new BlockImpactService.PendingBreak(caster, pos.immutable(), state, burst, debris));
         return true;
      } else {
         return false;
      }
   }

   public static boolean mayAlter(ServerLevel level, ServerPlayer caster, PhysicsStructure s, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      if (state.is(Poneglyphs.UNBREAKABLE)) {
         return false;
      } else if (!level.mayInteract(caster, BlockPos.containing(s.toWorld(Vec3.atCenterOf(pos))))) {
         return false;
      } else {
         BreakEvent event = new BreakEvent(level, pos, state, caster);
         NeoForge.EVENT_BUS.post(event);
         return !event.isCanceled();
      }
   }

   public static boolean canBreak(ServerLevel level, ServerPlayer caster, PhysicsStructure s, BlockPos pos, BlockState state, boolean anyHardness) {
      if (!state.isAir()
         && state.getFluidState().isEmpty()
         && !state.hasBlockEntity()
         && !state.is(DFTags.DESTRUCTION_IMMUNE)
         && !state.is(Poneglyphs.UNBREAKABLE)) {
         float hardness = state.getDestroySpeed(level, pos);
         if (!(hardness < 0.0F) && (anyHardness ? !state.is(DFTags.RESISTS_TECHNIQUES) : !((double)hardness > (Double)CoreConfig.MAX_BLOCK_HARDNESS.get()))) {
            if (!level.mayInteract(caster, BlockPos.containing(s.toWorld(Vec3.atCenterOf(pos))))) {
               return false;
            } else {
               BreakEvent event = new BreakEvent(level, pos, state, caster);
               NeoForge.EVENT_BUS.post(event);
               return !event.isCanceled();
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static int ignite(ServerPlayer caster, @Nullable ResourceLocation fruit, ImpactShape shape, double chance, CastBudget budget) {
      if (PhysicsWorld.active()
         && PhysicsConfig.enabledFor(fruit)
         && (Boolean)PhysicsConfig.BLOCK_DAMAGE.get()
         && TerrainDestruction.destructionAllowed(caster.serverLevel())) {
         ServerLevel level = caster.serverLevel();
         int lit = 0;

         for (StructureBlock b : PhysicsWorld.blocksIn(level, shape, st -> allowed(caster, st), 256)) {
            if (!(caster.getRandom().nextDouble() >= chance * b.power())
               && b.state().isFlammable(level, b.localPos(), Direction.UP)
               && igniteNear(level, caster, b.structure(), b.localPos(), budget)) {
               lit++;
            }
         }

         return lit;
      } else {
         return 0;
      }
   }

   public static boolean igniteNear(ServerLevel level, ServerPlayer caster, PhysicsStructure s, BlockPos pos, CastBudget budget) {
      for (Direction d : new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
         BlockPos at = pos.relative(d);
         if (level.getBlockState(at).isAir()) {
            BlockState fire = BaseFireBlock.getState(level, at);
            if (fire.canSurvive(level, at) && level.mayInteract(caster, BlockPos.containing(s.toWorld(Vec3.atCenterOf(at))))) {
               if (!EventHooks.onBlockPlace(caster, BlockSnapshot.create(level.dimension(), level, at), Direction.UP) && budget.takeBlock()) {
                  level.setBlock(at, fire, 3);
                  decal(level, s, pos, Vec3.atLowerCornerOf(d.getNormal()), BlockImpactService.Decal.SCORCH, 1.2F);
                  return true;
               }

               return false;
            }
         }
      }

      return false;
   }

   public static void decal(ServerLevel level, PhysicsStructure s, BlockPos localBlock, Vec3 localNormal, BlockImpactService.Decal kind, float size) {
      Vec3 n = localNormal.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : localNormal.normalize();
      Vec3 local = Vec3.atCenterOf(localBlock).add(n.scale(0.51));
      ServerFx.sendAround(level, s.toWorld(local), 80.0, CoreFx.STRUCTURE_DECAL, -1, local, n, size, level.random.nextLong(), kind.ordinal());
   }

   public static void flush(ServerLevel level) {
      Map<UUID, ArrayDeque<BlockImpactService.PendingBreak>> queues = PENDING.get(level);
      if (queues != null && !queues.isEmpty()) {
         int perTick = (Integer)PhysicsConfig.MAX_BLOCKS_PER_TICK.get();
         Iterator<ArrayDeque<BlockImpactService.PendingBreak>> it = queues.values().iterator();

         while (it.hasNext()) {
            ArrayDeque<BlockImpactService.PendingBreak> queue = it.next();

            for (int i = 0; i < perTick && !queue.isEmpty(); i++) {
               apply(level, queue.poll());
            }

            if (queue.isEmpty()) {
               it.remove();
            }
         }
      }
   }

   private static void apply(ServerLevel level, BlockImpactService.PendingBreak b) {
      if (level.getBlockState(b.pos()) == b.state()) {
         PhysicsStructure s = PhysicsWorld.structureAt(level, b.pos());
         Vec3 world = s != null ? s.toWorld(Vec3.atCenterOf(b.pos())) : Vec3.atCenterOf(b.pos());
         Vec3 carried = s != null ? s.velocityAt(world).scale(0.05) : Vec3.ZERO;
         ServerPlayer caster = b.caster() != null && !b.caster().isRemoved() ? b.caster() : null;
         BlockState state = b.state();
         BlockPos otherPos = null;
         if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            BlockPos o = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? b.pos().above() : b.pos().below();
            if (level.getBlockState(o).is(state.getBlock())) {
               otherPos = o;
            }
         }

         if ((Boolean)CoreConfig.DROP_BLOCKS.get()) {
            boolean lowerHere = otherPos == null || state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
            BlockPos dropPos = lowerHere ? b.pos() : otherPos;
            Block.dropResources(level.getBlockState(dropPos), level, dropPos, null, caster, ItemStack.EMPTY);
         }

         if (otherPos != null) {
            level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), 18);
         }

         level.setBlock(b.pos(), Blocks.AIR.defaultBlockState(), 3);
         Vec3 fly = carried.add(b.burst());
         if (b.debris()) {
            level.addFreshEntity(new DebrisEntity(level, world.x, world.y - 0.5, world.z, state, fly));
         } else {
            ServerFx.blockBurst(level, world, state, 0.6F, carried);
         }
      }
   }

   public static void clear() {
      PENDING.clear();
   }

   private BlockImpactService() {
   }

   public static enum Decal {
      CRACK,
      SCORCH,
      LIGHTNING,
      SAND,
      HAKI,
      RING,
      DENT;
   }

   private static record PendingBreak(@Nullable ServerPlayer caster, BlockPos pos, BlockState state, Vec3 burst, boolean debris) {
   }

   public static record Strike(
      ServerPlayer caster,
      @Nullable ResourceLocation fruit,
      PhysicsProfile.Impact impact,
      double strength,
      double blockChance,
      boolean anyHardness,
      CastBudget budget,
      @Nullable BlockImpactService.Decal decal
   ) {
   }
}
