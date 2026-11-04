package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.terrain.RemovedBlock;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.Nullable;

public final class RuinBreachAbility extends RuinAbility {
   public static final String ID = "ruin_breach";
   private static final String CLIP_HOLD = "ruin_breach_hold";
   private static final String CLIP_RELEASE = "ruin_grip_release";
   private static final int ARM_HEAL_TICKS = 20;
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
   private static final int HALF = 1;
   static final AbilityScaling.Entry PIT_STACKS = new AbilityScaling.Entry(
      "ruin_pit_stacks", AbilityScaling.Unit.COUNT, AbilityScaling.Kind.PER_POINT, 2.0, 4, 0.1
   );
   static final AbilityScaling.Entry DEPTH = new AbilityScaling.Entry(
      "ruin_hole_depth", AbilityScaling.Unit.BLOCKS, AbilityScaling.Kind.PER_POINT, 8.0, 16, 0.4
   );
   static final AbilityScaling.Entry BORE_LENGTH = new AbilityScaling.Entry(
      "ruin_tunnel_length", AbilityScaling.Unit.BLOCKS, AbilityScaling.Kind.PER_POINT, 40.0, 80, 2.0
   );
   private final Map<UUID, RuinBreachAbility.Press> presses = this.activeState();

   public RuinBreachAbility() {
      super("ruin_breach", 18.0F, 7);
   }

   @Override
   public String costLabel() {
      return holdCostLabel(this.billed(18.0F), this.billed(0.5F));
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().add(PIT_STACKS).add(DEPTH).add(BORE_LENGTH).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (busy(player)) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         ServerLevel level = player.serverLevel();
         RuinBreachAbility.Plan plan = plan(player, level);
         if (plan == null) {
            return this.fail(player, "awakened.ruin.breach_no_surface", new Object[0]);
         } else {
            RuinCarve.Run tap = carve(level, player, 6, plan.blocks(), p -> false, null);
            if (tap.planned() == 0) {
               return this.fail(player, "awakened.ruin.nothing_to_rot", new Object[0]);
            } else {
               RuinBreachFx.crackAhead(level, plan.blocks(), 6);
               RuinCues.tense(level, plan.face());
               this.presses.put(player.getUUID(), new RuinBreachAbility.Press(plan, tap, level.getGameTime() + 6L));
               return true;
            }
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      RuinBreachAbility.Press press = this.presses.get(player.getUUID());
      if (press != null) {
         press.released = true;
         if (press.bore != null) {
            this.endBore(player, press);
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      RuinBreachAbility.Press press = this.presses.get(player.getUUID());
      if (press != null) {
         if (press.bore != null) {
            this.tickBore(player, press);
         } else if (player.serverLevel().getGameTime() >= press.contactAt) {
            this.contact(player, press);
         }
      }
   }

   private void contact(ServerPlayer player, RuinBreachAbility.Press press) {
      ServerLevel level = player.serverLevel();
      RuinBreachAbility.Plan plan = press.plan;
      RuinBreachFx.press(level, player, plan.face(), plan.normal());
      LivingEntity target = plan.target();
      if (target != null
         && target.isAlive()
         && target.level() == level
         && RuinStacks.add(target, poweredCount(PIT_STACKS, player), player, RuinStacks.touchPoint(player, target))) {
         RuinCues.stackLanded(level, target, plan.face());
      }

      Vec3 through = plan.through();
      if (through != null) {
         Vec3 into = plan.normal().scale(-1.0);
         BlockState last = level.getBlockState(BlockPos.containing(plan.face().add(into.scale(0.5))));
         RuinFxClock.after(level, 1, () -> RuinBreachFx.breakthrough(level, through, into, last));
      }

      if (!press.released && plan.canBore() && this.drainTick(player, 0.5F, 0)) {
         press.bore = new RuinBore(level, player, plan.boreStart(), plan.boreFloor(), plan.blocks(), powered(BORE_LENGTH, player));
         RuinArm.set(player, 0.4F);
         PowerService.playAnimation(player, "ruin_breach_hold", true);
      } else {
         this.presses.remove(player.getUUID());
      }
   }

   private void tickBore(ServerPlayer player, RuinBreachAbility.Press press) {
      RuinBore bore = press.bore;
      if (bore != null) {
         if (!this.drainTick(player, 0.5F, ++press.ticks)) {
            this.endBore(player, press);
            ranDry(player);
         } else {
            float climb = Math.min(1.0F, (float)press.ticks / 60.0F);
            RuinArm.set(player, Mth.lerp(climb, 0.4F, 0.9F));
            if (!bore.tick()) {
               this.endBore(player, press);
            }
         }
      }
   }

   private void endBore(ServerPlayer player, RuinBreachAbility.Press press) {
      if (press.bore != null && this.presses.remove(player.getUUID()) != null) {
         press.bore.finish();
         RuinArm.pulse(player, RuinArm.of(player).amount(), 0, 20);
         PowerService.stopAnimation(player);
         PowerService.playAnimation(player, "ruin_grip_release");
         PowerService.sync(player);
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      RuinBreachAbility.Press press = this.presses.get(player.getUUID());
      if (press != null) {
         if (press.bore != null) {
            press.bore.stop();
            PowerService.stopAnimation(player);
            RuinArm.off(player);
         } else {
            press.tap.cancel();
         }
      }

      super.clearPlayer(player);
   }

   static RuinCarve.Run carve(
      final ServerLevel level, ServerPlayer player, int at, List<BlockPos> blocks, final Predicate<BlockPos> carving, @Nullable final LongSet boreRim
   ) {
      return new RuinCarve().wave(at, blocks).drops().hold(200).rise(6).start(level, player, new RuinCarve.Listener() {
         @Override
         public void wave(List<RemovedBlock> removed) {
            RuinBreachFx.opened(level, removed, 200, carving, boreRim);
         }

         @Override
         public void layer(int y, List<RemovedBlock> restored) {
            RuinBreachFx.restored(level, restored);
         }
      });
   }

   @Nullable
   private static RuinBreachAbility.Plan plan(ServerPlayer player, ServerLevel level) {
      Vec3 eye = player.getEyePosition();
      Vec3 end = eye.add(player.getLookAngle().scale(6.0));
      BlockHitResult hit = AimUtil.clipBlocks(level, player, eye, end);
      boolean struck = hit.getType() == Type.BLOCK;
      double wallDistance = struck ? eye.distanceTo(hit.getLocation()) : 6.0;
      LivingEntity body = AimUtil.aimedLivingEntity(player, 6.0, e -> sinkable(player, e));
      if (body != null && rayDistance(body, eye, end) <= wallDistance) {
         return pitUnder(body, body.position(), UP);
      } else if (!struck) {
         return null;
      } else {
         Direction face = hit.getDirection();
         BlockPos pos = hit.getBlockPos();
         Vec3 normal = new Vec3((double)face.getStepX(), (double)face.getStepY(), (double)face.getStepZ());
         Vec3 start = hit.getLocation().subtract(normal.scale(0.05));
         int feet = player.blockPosition().getY();
         if (face == Direction.UP) {
            if (pos.getY() == feet - 1 && underFeet(player, pos)) {
               List<BlockPos> shaft = square(player.blockPosition().below(), 4);
               Vec3 bottom = start.add(0.0, -3.0, 0.0);
               return new RuinBreachAbility.Plan(shaft, hit.getLocation(), normal, null, bottom, Mth.floor(bottom.y) - 1, null);
            }

            LivingEntity standing = standingOn(level, player, pos);
            if (standing != null) {
               return pitUnder(standing, hit.getLocation(), normal);
            }
         }

         return opening(level, hit, face, normal, start, feet, poweredCount(DEPTH, player));
      }
   }

   private static boolean underFeet(ServerPlayer player, BlockPos pos) {
      for (long col : footprint(player.getBoundingBox())) {
         if (Math.abs(BlockPos.getX(col) - pos.getX()) <= 1 && Math.abs(BlockPos.getZ(col) - pos.getZ()) <= 1) {
            return true;
         }
      }

      return false;
   }

   private static boolean sinkable(ServerPlayer player, LivingEntity e) {
      return e != player && e.onGround() && RuinGripAbility.grabbable(player, e);
   }

   private static double rayDistance(LivingEntity body, Vec3 eye, Vec3 end) {
      Optional<Vec3> at = body.getBoundingBox().inflate(0.3).clip(eye, end);
      return at.<Double>map(eye::distanceTo).orElse(Double.MAX_VALUE);
   }

   @Nullable
   private static LivingEntity standingOn(ServerLevel level, ServerPlayer player, BlockPos floor) {
      AABB top = new AABB(
         (double)floor.getX(),
         (double)(floor.getY() + 1),
         (double)floor.getZ(),
         (double)(floor.getX() + 1),
         (double)floor.getY() + 1.5,
         (double)(floor.getZ() + 1)
      );
      return level.getEntitiesOfClass(LivingEntity.class, top, e -> sinkable(player, e))
         .stream()
         .min(Comparator.comparingDouble(e -> e.distanceToSqr(Vec3.atCenterOf(floor))))
         .orElse(null);
   }

   private static RuinBreachAbility.Plan pitUnder(LivingEntity body, Vec3 face, Vec3 normal) {
      int top = floorY(body);
      Set<BlockPos> blocks = new LinkedHashSet<>(square(body.blockPosition().atY(top), 4));

      for (long col : footprint(body.getBoundingBox())) {
         BlockPos c = BlockPos.of(col).atY(top);

         for (int i = 0; i < 4; i++) {
            blocks.add(c.below(i));
         }
      }

      Vec3 press = new Vec3(face.x, (double)top + 1.0, face.z);
      return new RuinBreachAbility.Plan(new ArrayList<>(blocks), press, normal, body, press, top, null);
   }

   private static List<BlockPos> square(BlockPos centre, int depth) {
      int half = 1;
      List<BlockPos> out = new ArrayList<>();

      for (int d = 0; d < depth; d++) {
         for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
               out.add(centre.offset(dx, -d, dz));
            }
         }
      }

      return out;
   }

   private static RuinBreachAbility.Plan opening(ServerLevel level, BlockHitResult hit, Direction face, Vec3 normal, Vec3 start, int feet, int maxDepth) {
      Direction into = face.getOpposite();
      BlockPos pos = hit.getBlockPos();
      boolean wall = face.getAxis().isHorizontal();
      boolean onFeet = wall && Math.abs(pos.getY() - feet) <= 2;
      BlockPos centre = onFeet ? pos.atY(feet + 1) : pos;
      Direction across = wall ? face.getClockWise() : Direction.EAST;
      Direction upward = wall ? Direction.UP : Direction.SOUTH;
      List<BlockPos> blocks = new ArrayList<>();
      Vec3 through = null;
      int depth = 0;

      for (int d = 0; d < maxDepth; d++) {
         List<BlockPos> slice = new ArrayList<>();
         boolean open = true;

         for (int w = -1; w <= 1; w++) {
            for (int h = -1; h <= 1; h++) {
               BlockPos p = centre.relative(into, d).relative(across, w).relative(upward, h);
               slice.add(p);
               open &= level.getBlockState(p).getCollisionShape(level, p).isEmpty();
            }
         }

         if (open && d > 0) {
            through = Vec3.atCenterOf(centre.relative(into, d));
            break;
         }

         blocks.addAll(slice);
         depth = d + 1;
      }

      Vec3 press = Vec3.atCenterOf(centre.relative(face)).subtract(normal.scale(0.5));
      Vec3 farEnd = start.subtract(normal.scale((double)Math.max(0, depth - 1)));
      int boreFloor = onFeet ? feet : Mth.floor(farEnd.y - 0.1) - 1;
      return new RuinBreachAbility.Plan(blocks, press, normal, null, farEnd, boreFloor, through);
   }

   private static int floorY(LivingEntity body) {
      return Mth.floor(body.getY() - 0.01);
   }

   private static List<Long> footprint(AABB box) {
      List<Long> out = new ArrayList<>();
      int x0 = Mth.floor(box.minX + 0.01);
      int x1 = Mth.floor(box.maxX - 0.01);
      int z0 = Mth.floor(box.minZ + 0.01);
      int z1 = Mth.floor(box.maxZ - 0.01);

      for (int x = x0; x <= x1; x++) {
         for (int z = z0; z <= z1; z++) {
            out.add(BlockPos.asLong(x, 0, z));
         }
      }

      return out;
   }

   private static record Plan(
      List<BlockPos> blocks, Vec3 face, Vec3 normal, @Nullable LivingEntity target, Vec3 boreStart, int boreFloor, @Nullable Vec3 through
   ) {
      boolean canBore() {
         return this.target == null;
      }
   }

   private static final class Press {
      final RuinBreachAbility.Plan plan;
      final RuinCarve.Run tap;
      final long contactAt;
      int ticks;
      boolean released;
      @Nullable
      RuinBore bore;

      Press(RuinBreachAbility.Plan plan, RuinCarve.Run tap, long contactAt) {
         this.plan = plan;
         this.tap = tap;
         this.contactAt = contactAt;
      }
   }
}
