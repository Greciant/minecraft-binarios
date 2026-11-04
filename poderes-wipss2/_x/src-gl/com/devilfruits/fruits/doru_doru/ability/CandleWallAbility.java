package com.devilfruits.fruits.doru_doru.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.fruits.doru_doru.DoruAbilities;
import com.devilfruits.fruits.doru_doru.DoruDoru;
import com.devilfruits.fruits.doru_doru.DoruFx;
import com.devilfruits.fruits.doru_doru.DoruSounds;
import com.devilfruits.fruits.doru_doru.atelier.BuildEngine;
import com.devilfruits.fruits.doru_doru.atelier.SculptRequest;
import com.devilfruits.fruits.doru_doru.atelier.TechniqueGen;
import com.devilfruits.fruits.doru_doru.atelier.VoxelSet;
import com.devilfruits.fruits.doru_doru.wax.WaxBlockEntity;
import com.devilfruits.fruits.doru_doru.wax.WaxHeatService;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongListIterator;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class CandleWallAbility extends Ability {
   private static final Map<UUID, Deque<CandleWallAbility.Wall>> WALLS = new HashMap<>();

   public CandleWallAbility() {
      super(
         DoruDoru.id("candle_wall"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(18)
            .cooldown(60)
            .mastery(3)
            .damage(0.0)
            .range(12.0)
            .extra("width", 7.0, 1.0, 24.0, "Width of the wall in blocks.")
            .extra("height", 5.0, 1.0, 16.0, "Height of the wall in blocks.")
            .extra("thickness", 1.5, 0.25, 6.0, "Thickness of the wall in blocks.")
            .extra("bow", 1.0, 0.0, 6.0, "How far the wall's middle bows towards the enemy.")
            .extra("domeRadius", 3.0, 1.0, 10.0, "Radius of the dome raised while crouching.")
            .extra("lifetimeSeconds", 45.0, 1.0, 3600.0, "Seconds a wall stands before it sags and melts.")
            .extra("maxWalls", 3.0, 1.0, 32.0, "Walls one user keeps at once (a new one melts the oldest).")
            .extra("riseTicks", 6.0, 1.0, 60.0, "Ticks the wall takes to rise.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).manual().build();
   }

   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      return DoruDoru.heatBlock(player);
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      boolean dome = player.isCrouching();
      Vec3 facing = ctx.horizontalLook();
      Vec3 base;
      VoxelSet set;
      if (dome) {
         base = player.position();
         set = TechniqueGen.dome(base, ctx.extra("domeRadius"), 0.75);
      } else {
         Vec3 eye = player.getEyePosition();
         BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(ctx.look().scale((double)ctx.range())), Block.COLLIDER, Fluid.NONE, player));
         if (hit.getType() != Type.BLOCK) {
            Vec3 far = eye.add(ctx.look().scale((double)Math.min(ctx.range(), 6.0F)));
            hit = level.clip(new ClipContext(far, far.add(0.0, -8.0, 0.0), Block.COLLIDER, Fluid.NONE, player));
            if (hit.getType() != Type.BLOCK) {
               player.displayClientMessage(Component.translatable("message.devilfruits.doru_doru.no_ground"), true);
               return;
            }
         }

         BlockPos ground = hit.getBlockPos();
         base = new Vec3(hit.getLocation().x, (double)(ground.getY() + (hit.getDirection() == Direction.UP ? 1 : 0)), hit.getLocation().z);
         set = TechniqueGen.wall(base, facing, ctx.extra("width"), ctx.extra("height"), ctx.extra("thickness"), ctx.extra("bow"));
      }

      SculptRequest r = new SculptRequest(
         SculptRequest.Tool.SHAPE, 0, List.of(base), new float[4], 0.0F, SculptRequest.Symmetry.NONE, 2, Vec3.ZERO, DoruAbilities.color(player), 0.0F
      );
      BuildEngine.Spec spec = BuildEngine.Spec.technique(this.id().getPath(), 1, (int)ctx.extra("riseTicks"))
         .lifetime((long)(ctx.extra("lifetimeSeconds") * 20.0))
         .integrity(false)
         .hands(dome ? player.position() : base);
      if (BuildEngine.submit(player, r, set, spec)) {
         remember(player, level, set, (int)ctx.extra("maxWalls"));
         level.playSound(null, base.x, base.y, base.z, DoruSounds.WALL_RISE.get(), SoundSource.PLAYERS, 1.2F, dome ? 1.1F : 0.9F);
         level.playSound(null, player.getX(), player.getY(), player.getZ(), DoruSounds.SLAM.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
         ServerFx.send(player, DoruFx.WALL_RISE, base, facing, (float)(dome ? ctx.extra("domeRadius") : ctx.extra("width")), ctx.seed(), dome ? 1 : 0);
         ServerFx.animate(player, ctx.animation(DoruDoru.id(dome ? "dome_raise" : "wall_slam")));
      }
   }

   private static void remember(ServerPlayer player, ServerLevel level, VoxelSet set, int max) {
      Deque<CandleWallAbility.Wall> walls = WALLS.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());
      walls.addLast(new CandleWallAbility.Wall(level.dimension(), new LongArrayList(set.blocks().keySet())));

      while (walls.size() > max) {
         CandleWallAbility.Wall old = walls.removeFirst();
         ServerLevel l = player.getServer().getLevel(old.level());
         if (l != null) {
            long now = l.getGameTime();
            LongListIterator var9 = old.blocks().iterator();

            while (var9.hasNext()) {
               long key = (Long)var9.next();
               BlockPos pos = BlockPos.of(key);
               if (l.isLoaded(pos)) {
                  BlockEntity var14 = l.getBlockEntity(pos);
                  if (var14 instanceof WaxBlockEntity) {
                     WaxBlockEntity be = (WaxBlockEntity)var14;
                     if (be.kind() == 1) {
                        be.setExpires(now);
                        WaxHeatService.poke(l, pos);
                     }
                  }
               }
            }
         }
      }
   }

   public static void forget(UUID player) {
      WALLS.remove(player);
   }

   private static record Wall(ResourceKey<Level> level, LongArrayList blocks) {
   }
}
