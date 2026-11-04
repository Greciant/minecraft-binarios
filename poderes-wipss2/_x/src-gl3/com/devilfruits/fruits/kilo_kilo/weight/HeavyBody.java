package com.devilfruits.fruits.kilo_kilo.weight;

import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsConfig;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.physics.PhysicsStructure;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.physics.StructureBlock;
import com.devilfruits.core.physics.StructureForces;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.kilo_kilo.KiloAbilities;
import com.devilfruits.fruits.kilo_kilo.KiloFx;
import com.devilfruits.fruits.kilo_kilo.KiloKilo;
import com.devilfruits.fruits.kilo_kilo.KiloSounds;
import com.devilfruits.fruits.kilo_kilo.KiloTriggers;
import com.devilfruits.registry.DFAttachments;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class HeavyBody {
   public static final ResourceLocation COMBO = KiloKilo.id("press_combo");
   private static final double STRIDE = 1.6;
   private static final double LOOKAHEAD_TICKS = 2.0;
   private static final Map<UUID, HeavyBody.State> STATES = new HashMap<>();
   private static int crackIds;

   private static void endRun(HeavyBody.State st) {
      st.runStart = null;
      st.runEnergy = -1.0;
      st.burst = false;
   }

   private static HeavyBody.State state(ServerPlayer player) {
      return STATES.computeIfAbsent(player.getUUID(), k -> new HeavyBody.State());
   }

   public static void forget(ServerPlayer player) {
      STATES.remove(player.getUUID());
   }

   public static boolean plunging(ServerPlayer player) {
      HeavyBody.State st = STATES.get(player.getUUID());
      return st != null && st.plunge >= 0;
   }

   static void tick(ServerPlayer player, BodyWeight w, WeightCurves.Tuning t) {
      HeavyBody.State st = state(player);
      Vec3 pos = player.position();
      if (!st.init) {
         st.init = true;
         st.lastX = pos.x;
         st.lastY = pos.y;
         st.lastZ = pos.z;
      }

      Vec3 moved = new Vec3(pos.x - st.lastX, pos.y - st.lastY, pos.z - st.lastZ);
      st.lastX = pos.x;
      st.lastY = pos.y;
      st.lastZ = pos.z;
      if (moved.lengthSqr() > 64.0) {
         moved = Vec3.ZERO;
      }

      st.velocity = st.velocity.scale(0.5).add(moved.scale(0.5));
      long now = player.level().getGameTime();
      if (st.tick != now) {
         st.tick = now;
         st.blocksThisTick = 0;
      }

      double kg = w.kg();
      boolean ground = player.onGround();
      st.groundTicks = ground ? st.groundTicks + 1 : 0;
      if (!ground) {
         st.airFall = Math.max(st.airFall, (double)player.fallDistance);
      } else if (!st.wasGround && !player.isSpectator()) {
         onLand(player, st.airFall);
         st.airFall = 0.0;
      }

      st.wasGround = ground;
      thinFloor(player, kg, st, ground && !player.isSpectator() && !player.getAbilities().flying);
      if (!player.isSpectator() && !player.getAbilities().flying) {
         if (!Double.isNaN(st.jumpStartY)) {
            st.jumpPeak = Math.max(st.jumpPeak, pos.y);
            if (ground && now % 2L == 0L && st.jumpPeak - st.jumpStartY > 0.5) {
               if (st.jumpPeak - st.jumpStartY >= t.lightJumpBlocks() * 0.95) {
                  KiloTriggers.FEATHER_JUMP.get().trigger(player);
               }

               st.jumpStartY = Double.NaN;
            }
         }

         if (ground && kg >= WeightManager.setting("footstepMinKg", 1000.0)) {
            st.stride = st.stride + Math.sqrt(moved.x * moved.x + moved.z * moved.z);
            if (st.stride >= 1.6) {
               st.stride = 0.0;
               ServerFx.send(player, KiloFx.FOOTSTEP, pos, Vec3.ZERO, (float)WeightCurves.heaviness(kg), player.getRandom().nextLong(), 0);
            }
         } else if (!ground) {
            st.stride = 0.96;
         }

         double cap = WeightCurves.fallCap(kg, t);
         if (st.plunge >= 0) {
            st.plunge++;
            if (!ground && !player.isInWater() && !player.isPassenger() && st.plunge <= 200) {
               setMotion(player, new Vec3(0.0, -cap, 0.0));
            } else if (!ground) {
               endPlunge(player, w, st);
            }
         }

         double gravity = WeightCurves.gravity(kg, t);
         if (!ground && !player.isInWater()) {
            double down = st.plunge >= 0
               ? cap
               : Math.max(st.carry, st.velocity.y < -0.05 ? WeightCurves.fallSpeed((double)player.fallDistance, gravity, cap) : 0.0);
            if (down > 0.3) {
               HeavyBody.Smash s = smash(player, kg, new Vec3(0.0, -1.0, 0.0), down, down * 2.0 + 0.6, false);
               if (s.broken > 0) {
                  st.blocksThisFall = st.blocksThisFall + s.broken;
                  double left = WeightCurves.speedFor(kg, s.energyLeft);
                  st.carry = left;
                  if (st.plunge >= 0) {
                     st.plunge = -1;
                     w.setPlunging(false);
                     WeightManager.sync(player);
                  }

                  setMotion(player, new Vec3(player.getDeltaMovement().x * 0.5, -left, player.getDeltaMovement().z * 0.5));
                  wreckingBall(player, kg, st);
               }
            }
         }

         double flat = Math.sqrt(st.velocity.x * st.velocity.x + st.velocity.z * st.velocity.z);
         if (flat > 0.55 && WeightCurves.heaviness(kg) > 0.3) {
            Vec3 dir = new Vec3(st.velocity.x, 0.0, st.velocity.z).normalize();
            HeavyBody.Smash s = smash(player, kg, dir, flat, flat * 2.0 + 0.4, true);
            if (s.broken > 0) {
               st.blocksThisFall = st.blocksThisFall + s.broken;
               double left = WeightCurves.speedFor(kg, s.energyLeft);
               setMotion(player, new Vec3(dir.x * left, st.velocity.y, dir.z * left));
               wreckingBall(player, kg, st);
            }
         }

         double fall = ground || player.isInWater()
            ? 0.0
            : (st.plunge >= 0 ? cap : Math.max(st.carry, st.velocity.y < -0.05 ? WeightCurves.fallSpeed((double)player.fallDistance, gravity, cap) : 0.0));
         contact(player, kg, st, moved, fall, now);
         if (st.groundTicks > 5 && st.plunge < 0) {
            st.carry = 0.0;
            st.blocksThisFall = 0;
            endRun(st);
         }

         standingLoad(player, kg, st, ground, now);
      } else {
         endPlunge(player, w, st);
      }
   }

   private static void contact(ServerPlayer player, double kg, HeavyBody.State st, Vec3 moved, double fall, long now) {
      int cooldown = (int)WeightManager.setting("contactCooldownTicks", 10.0);
      st.contacts.values().removeIf(at -> now - at > (long)cooldown || now < at);
      Vec3 vel = new Vec3(st.velocity.x, -Math.max(fall, -moved.y), st.velocity.z);
      double energy = WeightCurves.kineticEnergy(kg, vel.length());
      if (!(energy < WeightManager.setting("contactMinEnergyKJ", 40.0) * 1000.0)) {
         ServerLevel level = player.serverLevel();
         AABB box = player.getBoundingBox();
         Vec3 from = box.getCenter().subtract(moved);
         Vec3 to = box.getCenter().add(vel.scale(2.0));
         HitResult wall = PhysicsWorld.clip(level, new ClipContext(box.getCenter(), to, Block.COLLIDER, Fluid.NONE, player));
         if (wall.getType() != Type.MISS) {
            to = wall.getLocation();
         }

         AABB swept = box.expandTowards(moved.scale(-1.0)).expandTowards(to.subtract(box.getCenter())).inflate(0.05);
         double hx = box.getXsize() * 0.5;
         double hy = box.getYsize() * 0.5;
         double hz = box.getZsize() * 0.5;
         double flat = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
         double power = WeightCurves.impactPower(energy);
         float mastery = Mastery.damageMultiplier(DFAttachments.data(player).mastery());
         boolean press = KiloKilo.weight(player).mode() == 2;

         for (Entity e : Targeting.inBox(
            level,
            player,
            swept,
            x -> x instanceof LivingEntity && Targeting.canHurt(player, x) && x != player.getVehicle() && !x.isPassengerOfSameVehicle(player)
         )) {
            if (!st.contacts.containsKey(e.getId())) {
               AABB reach = e.getBoundingBox().inflate(hx, hy, hz);
               if (reach.contains(from) || !reach.clip(from, to).isEmpty()) {
                  st.contacts.put(e.getId(), now);
                  boolean fromAbove = -vel.y > flat && e.getBoundingBox().getCenter().y < box.getCenter().y - 0.3;
                  double damage = (
                        fromAbove ? WeightCurves.crushDamage(energy, WeightManager.setting("crushDamageCap", 60.0)) : WeightCurves.impactDamage(energy)
                     )
                     * (double)mastery;
                  Targeting.hurt(player, e, (float)damage, KiloKilo.PRESS_DAMAGE);
                  if (!fromAbove && flat > 0.001 && Targeting.canPush(player, e)) {
                     Targeting.launch(e, new Vec3(vel.x / flat, 0.0, vel.z / flat).scale(0.3 + 0.4 * power).add(0.0, 0.2 + 0.1 * power, 0.0));
                  }

                  float k = (float)Math.min(1.0, power / 2.6);
                  level.playSound(null, e.getX(), e.getY(), e.getZ(), KiloSounds.HEAVY_STEP.get(), SoundSource.PLAYERS, 0.7F + 0.5F * k, 0.75F - 0.25F * k);
                  if (press && !st.burst) {
                     pressBurst(player, e.position(), energy);
                  }
               }
            }
         }
      }
   }

   private static void pressBurst(ServerPlayer player, Vec3 at, double energy) {
      HeavyBody.State st = state(player);
      st.burst = true;
      double radius = WeightManager.setting("pressBurstRadius", 3.0);
      if (!(radius <= 0.0)) {
         ServerLevel level = player.serverLevel();
         double power = WeightCurves.impactPower(energy);
         double damage = WeightCurves.impactDamage(energy) * (double)Mastery.damageMultiplier(DFAttachments.data(player).mastery());

         for (Entity e : Targeting.inRadius(level, at, radius, x -> x != player)) {
            double falloff = Mth.clamp(1.0 - e.position().distanceTo(at) / radius, 0.0, 1.0);
            Targeting.hurt(player, e, (float)(damage * (0.4 + 0.6 * falloff)), KiloKilo.PRESS_DAMAGE);
            if (Targeting.canPush(player, e)) {
               Vec3 away = e.position().subtract(at).multiply(1.0, 0.0, 1.0);
               away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
               Targeting.launch(e, away.scale((0.2 + 0.3 * power) * falloff).add(0.0, 0.15 + 0.15 * power * falloff, 0.0));
            }
         }

         ServerFx.sendNear(
            level,
            KiloFx.IMPACT,
            player.getId(),
            at,
            new Vec3(0.0, 1.0, 0.0),
            (float)Math.min(power, 1.0),
            player.getRandom().nextLong(),
            (int)Math.round(radius * 16.0),
            96.0
         );
      }
   }

   private static void wreckingBall(ServerPlayer player, double kg, HeavyBody.State st) {
      if (st.blocksThisFall >= 12 && kg >= 9000.0) {
         KiloTriggers.WRECKING_BALL.get().trigger(player);
      }
   }

   private static void standingLoad(ServerPlayer player, double kg, HeavyBody.State st, boolean ground, long now) {
      if (ground && PhysicsWorld.active() && PhysicsConfig.enabledFor(KiloKilo.ID)) {
         PhysicsStructure s = PhysicsWorld.structureUnder(player);
         if (s != null && BlockImpactService.allowed(player, s)) {
            double mass = StructureForces.massUnits(kg) * (Double)PhysicsConfig.MASS_SCALE.get();
            StructureForces.load(s, player.position(), mass);
            double h = WeightCurves.heaviness(kg);
            if (h > 0.45 && ++st.groan >= 50) {
               st.groan = player.getRandom().nextInt(20);
               float relative = (float)Math.min(1.0, mass / Math.max(1.0, s.mass()) * 4.0);
               player.level()
                  .playSound(
                     null,
                     player.getX(),
                     player.getY(),
                     player.getZ(),
                     KiloSounds.HULL_GROAN.get(),
                     SoundSource.PLAYERS,
                     0.4F + relative * 0.8F,
                     1.1F - (float)h * 0.5F
                  );
            }

            if (!s.id().equals(st.loadStructure)) {
               st.loadStructure = s.id();
               st.loadStartY = player.getY();
               st.loadStart = now;
            } else if (kg >= 5000.0 && s.mass() < mass && now - st.loadStart > 40L && st.loadStartY - player.getY() > 2.0 && player.isInWater()) {
               KiloTriggers.SANK_RAFT.get().trigger(player);
               st.loadStart = now + 1000000L;
            }
         } else {
            st.loadStructure = null;
         }
      } else {
         st.loadStructure = null;
      }
   }

   private static int thinFloor(ServerPlayer player, double kg, HeavyBody.State st, boolean standing) {
      ServerLevel level = player.serverLevel();
      int maxBlocks = (int)Math.round(WeightManager.setting("thinFloorMaxBlocks", 1.0));
      double minKg = WeightManager.setting("thinFloorMinKg", 5000.0);
      if (standing && maxBlocks > 0 && !(kg < minKg) && TerrainDestruction.destructionAllowed(level) && KiloAbilities.PRESS.config().blockDamage()) {
         Map<BlockPos, HeavyBody.Floor> under = thinFloors(player, maxBlocks);
         Iterator<Entry<BlockPos, HeavyBody.Creak>> it = st.creaks.entrySet().iterator();

         while (it.hasNext()) {
            Entry<BlockPos, HeavyBody.Creak> e = it.next();
            if (!under.containsKey(e.getKey())) {
               level.destroyBlockProgress(e.getValue().crackId, e.getKey(), -1);
               it.remove();
            }
         }

         double seconds = WeightManager.setting("thinFloorSeconds", 0.4);
         int gave = 0;

         for (HeavyBody.Floor f : under.values()) {
            BlockState state = level.getBlockState(f.top());
            HeavyBody.Creak c = st.creaks
               .computeIfAbsent(f.top(), k -> new HeavyBody.Creak(WeightCurves.creakTicks(seconds, (double)state.getDestroySpeed(level, f.top()), kg, minKg)));
            c.ticks++;
            if (c.ticks == 1 && f.structure() != null) {
               level.playSound(null, player.getX(), player.getY(), player.getZ(), KiloSounds.HULL_GROAN.get(), SoundSource.BLOCKS, 0.9F, 0.7F);
            }

            if (c.ticks % 4 == 1) {
               SoundType sound = state.getSoundType(level, f.top(), player);
               Vec3 at = f.structure() != null ? f.structure().toWorld(Vec3.atCenterOf(f.top())) : Vec3.atCenterOf(f.top());
               level.playSound(null, at.x, at.y, at.z, sound.getHitSound(), SoundSource.BLOCKS, sound.getVolume() * 0.6F, sound.getPitch() * 0.6F);
            }

            int stage = Math.min(9, c.ticks * 10 / c.need);
            if (stage != c.stage) {
               c.stage = stage;
               level.destroyBlockProgress(c.crackId, f.top(), stage);
            }

            if (c.ticks >= c.need) {
               level.destroyBlockProgress(c.crackId, f.top(), -1);
               st.creaks.remove(f.top());
               if (giveWay(player, f, state)) {
                  gave++;
               }
            }
         }

         return gave;
      } else {
         if (!st.creaks.isEmpty()) {
            st.creaks.forEach((pos, cx) -> level.destroyBlockProgress(cx.crackId, pos, -1));
            st.creaks.clear();
         }

         return 0;
      }
   }

   static Map<BlockPos, HeavyBody.Floor> thinFloors(ServerPlayer player, int maxBlocks) {
      ServerLevel level = player.serverLevel();
      AABB box = player.getBoundingBox();
      Map<BlockPos, HeavyBody.Floor> out = new LinkedHashMap<>();
      AABB feet = new AABB(box.minX + 0.001, box.minY - 0.1, box.minZ + 0.001, box.maxX - 0.001, box.minY + 0.01, box.maxZ - 0.001);
      MutableBlockPos m = new MutableBlockPos();

      for (int x = Mth.floor(feet.minX); x <= Mth.floor(feet.maxX); x++) {
         for (int y = Mth.floor(feet.minY); y <= Mth.floor(feet.maxY); y++) {
            for (int z = Mth.floor(feet.minZ); z <= Mth.floor(feet.maxZ); z++) {
               m.set(x, y, z);
               VoxelShape shape = level.getBlockState(m).getCollisionShape(level, m);
               if (!shape.isEmpty() && shape.bounds().move(m).intersects(feet)) {
                  column(level, m.immutable(), Direction.DOWN, maxBlocks, null, out);
               }
            }
         }
      }

      PhysicsStructure s = PhysicsWorld.active() && PhysicsConfig.enabledFor(KiloKilo.ID) ? PhysicsWorld.structureUnder(player) : null;
      if (s != null && BlockImpactService.allowed(player, s)) {
         Vec3 d = s.dirToLocal(new Vec3(0.0, -1.0, 0.0));
         Direction down = Direction.getNearest(d.x, d.y, d.z);
         double[] xs = new double[]{box.minX + 0.05, (box.minX + box.maxX) * 0.5, box.maxX - 0.05};
         double[] zs = new double[]{box.minZ + 0.05, (box.minZ + box.maxZ) * 0.5, box.maxZ - 0.05};

         for (double x : xs) {
            for (double zx : zs) {
               BlockPos local = BlockPos.containing(s.toLocal(new Vec3(x, box.minY - 0.08, zx)));
               if (!out.containsKey(local) && !level.getBlockState(local).getCollisionShape(level, local).isEmpty()) {
                  column(level, local, down, maxBlocks, s, out);
               }
            }
         }
      }

      return out;
   }

   private static void column(ServerLevel level, BlockPos top, Direction down, int maxBlocks, @Nullable PhysicsStructure s, Map<BlockPos, HeavyBody.Floor> out) {
      List<BlockPos> column = new ArrayList<>();
      MutableBlockPos m = top.mutable();

      for (int i = 0; i <= maxBlocks; i++) {
         if (level.getBlockState(m).getCollisionShape(level, m).isEmpty()) {
            if (i > 0) {
               out.putIfAbsent(top, new HeavyBody.Floor(top, column, s));
            }

            return;
         }

         column.add(m.immutable());
         m.move(down);
      }
   }

   private static boolean giveWay(ServerPlayer player, HeavyBody.Floor f, BlockState topState) {
      ServerLevel level = player.serverLevel();
      CastBudget budget = new CastBudget(f.column().size(), 2);
      boolean broke = false;

      for (BlockPos pos : f.column()) {
         if (!TerrainDestruction.tearOut(level, player, pos, new Vec3(0.0, -0.15, 0.0), budget, player.getRandom().nextInt(40), true, false)) {
            break;
         }

         broke = true;
      }

      if (broke) {
         Vec3 at = f.structure() != null ? f.structure().toWorld(Vec3.atCenterOf(f.top())) : Vec3.atCenterOf(f.top());
         ServerFx.sendNear(
            level,
            KiloFx.PUNCH,
            player.getId(),
            at,
            new Vec3(0.0, -1.0, 0.0),
            1.0F,
            player.getRandom().nextLong(),
            net.minecraft.world.level.block.Block.getId(topState),
            64.0
         );
      }

      return broke;
   }

   public static int pressFloor(ServerPlayer player, double kg) {
      return thinFloor(player, kg, state(player), true);
   }

   public static void startPlunge(ServerPlayer player, BodyWeight w) {
      HeavyBody.State st = state(player);
      st.plunge = 0;
      st.blocksThisFall = 0;
      st.carry = 0.0;
      endRun(st);
      w.setPlunging(true);
      player.fallDistance = Math.max(player.fallDistance, 1.0F);
      setMotion(player, new Vec3(0.0, -1.0, 0.0));
      ServerFx.send(player, KiloFx.PRESS_START, player.position(), Vec3.ZERO, 1.0F, player.getRandom().nextLong(), 0);
      ServerFx.animate(player, KiloKilo.id("press_dive"));
   }

   private static void endPlunge(ServerPlayer player, BodyWeight w, HeavyBody.State st) {
      if (st.plunge >= 0 || w.plunging()) {
         st.plunge = -1;
         w.setPlunging(false);
         WeightManager.sync(player);
      }
   }

   public static void onLand(ServerPlayer player, double distance) {
      BodyWeight w = KiloKilo.weight(player);
      HeavyBody.State st = state(player);
      WeightCurves.Tuning t = WeightManager.tuning();
      double kg = w.kg();
      boolean press = st.plunge >= 0 || w.plunging();
      double cap = WeightCurves.fallCap(kg, t);
      double speed = press ? cap : Math.max(st.carry, WeightCurves.fallSpeed(distance, WeightCurves.gravity(kg, t), cap));
      double energy = WeightCurves.kineticEnergy(kg, speed);
      HeavyBody.Smash s = smash(player, kg, new Vec3(0.0, -1.0, 0.0), speed, 1.2, false);
      if (s.broken > 0) {
         st.blocksThisFall = st.blocksThisFall + s.broken;
         wreckingBall(player, kg, st);
      }

      if (s.broken > 0 && !s.stopped && s.energyLeft > 0.0) {
         st.carry = WeightCurves.speedFor(kg, s.energyLeft);
         setMotion(player, new Vec3(0.0, -st.carry, 0.0));
      } else {
         energy = s.broken > 0 ? s.energyLeft : (st.runEnergy >= 0.0 ? Math.min(energy, st.runEnergy) : energy);
         st.carry = 0.0;
         endRun(st);
         endPlunge(player, w, st);
         double light = WeightCurves.lightness(kg);
         if (light > 0.3 && distance > 1.0) {
            ServerFx.animate(player, KiloKilo.id("light_land"));
         }

         boolean big = (press || distance >= WeightManager.setting("impactMinFall", 2.5))
            && energy >= WeightManager.setting("impactMinEnergyKJ", 250.0) * 1000.0;
         if (big) {
            impact(player, energy, press);
         } else if (WeightCurves.heaviness(kg) > 0.35 && distance > 0.6) {
            ServerFx.send(
               player, KiloFx.FOOTSTEP, player.position(), Vec3.ZERO, (float)Math.min(1.0, WeightCurves.heaviness(kg) * 1.2), player.getRandom().nextLong(), 1
            );
         }

         st.blocksThisFall = 0;
      }
   }

   public static void impact(ServerPlayer player, double energy, boolean press) {
      ServerLevel level = player.serverLevel();
      Vec3 at = player.position();
      double power = WeightCurves.impactPower(energy);
      double radius = WeightCurves.impactRadius(energy);
      if (KiloKilo.weight(player).mode() == 2) {
         radius = Math.max(radius, WeightManager.setting("pressBurstRadius", 3.0));
      }

      float mastery = Mastery.damageMultiplier(DFAttachments.data(player).mastery());
      double centre = WeightCurves.impactDamage(energy) * (double)mastery;
      double crush = WeightCurves.crushDamage(energy, WeightManager.setting("crushDamageCap", 60.0)) * (double)mastery;
      AABB footprint = player.getBoundingBox().inflate(0.35, 0.0, 0.35).expandTowards(0.0, -1.6, 0.0);

      for (Entity e : Targeting.inRadius(level, at, radius, x -> x != player)) {
         double dist = e.position().distanceTo(at);
         double falloff = Mth.clamp(1.0 - dist / radius, 0.0, 1.0);
         boolean crushed = footprint.intersects(e.getBoundingBox());
         double damage = crushed ? crush : centre * (0.3 + 0.7 * falloff);
         Targeting.hurt(player, e, (float)damage, KiloKilo.PRESS_DAMAGE);
         if (Targeting.canPush(player, e) && !crushed) {
            Vec3 away = e.position().subtract(at).multiply(1.0, 0.0, 1.0);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
            double kb = (0.2 + 0.4 * power) * falloff;
            Targeting.launch(e, away.scale(kb).add(0.0, 0.18 + 0.2 * power * falloff, 0.0));
         }
      }

      if (PhysicsWorld.active() && PhysicsConfig.enabledFor(KiloKilo.ID)) {
         BlockImpactService.pushFrom(player, ImpactShape.sphere(at, radius), new Vec3(0.0, -1.0, 0.0), 60.0 * power * power, PhysicsProfile.Impact.RADIAL);
      }

      ServerFx.sendNear(
         level,
         KiloFx.IMPACT,
         player.getId(),
         at,
         new Vec3(0.0, press ? 1.0 : 0.0, 0.0),
         (float)power,
         player.getRandom().nextLong(),
         (int)Math.round(radius * 16.0),
         96.0
      );
      if (press) {
         ServerFx.animate(player, KiloKilo.id("press_slam"));
      }
   }

   public static HeavyBody.Smash smash(ServerPlayer player, double kg, Vec3 dir, double speed, double reach, boolean ramOnStop) {
      ServerLevel level = player.serverLevel();
      HeavyBody.State st = state(player);
      double energy = WeightCurves.kineticEnergy(kg, speed);
      if (st.runEnergy >= 0.0) {
         energy = Math.min(energy, st.runEnergy);
      }

      double base = WeightManager.setting("blockEnergyKJ", 120.0) * 1000.0;
      if (energy < base * 0.6) {
         return new HeavyBody.Smash(0, false, energy);
      } else {
         double maxDepth = WeightManager.setting("maxSmashDepth", 10.0);
         boolean allowed = TerrainDestruction.destructionAllowed(level) && KiloAbilities.PRESS.config().blockDamage();
         int perTick = (int)KiloAbilities.PRESS.config().extra("maxBlocksPerTick");
         int perCast = (int)KiloAbilities.PRESS.config().extra("maxBlocksPerCast");
         AABB box = player.getBoundingBox();
         AABB swept = box.expandTowards(dir.scale(reach));
         Vec3 origin = box.getCenter();
         List<HeavyBody.Candidate> found = new ArrayList<>();
         MutableBlockPos cursor = new MutableBlockPos();

         for (int x = Mth.floor(swept.minX); x <= Mth.floor(swept.maxX - 1.0E-7); x++) {
            for (int y = Mth.floor(swept.minY); y <= Mth.floor(swept.maxY - 1.0E-7); y++) {
               for (int z = Mth.floor(swept.minZ); z <= Mth.floor(swept.maxZ - 1.0E-7); z++) {
                  cursor.set(x, y, z);
                  BlockState state = level.getBlockState(cursor);
                  if (!state.isAir()) {
                     VoxelShape shape = state.getCollisionShape(level, cursor);
                     if (!shape.isEmpty()) {
                        AABB bb = shape.bounds().move(cursor);
                        if (bb.intersects(swept) && !bb.intersects(box.deflate(0.05))) {
                           Vec3 c = bb.getCenter();
                           found.add(new HeavyBody.Candidate(cursor.immutable(), state, null, c, c.subtract(origin).dot(dir)));
                        }
                     }
                  }
               }
            }
         }

         if (PhysicsWorld.active() && PhysicsConfig.enabledFor(KiloKilo.ID)) {
            Vec3 to = origin.add(dir.scale(reach + box.getYsize() * 0.5 * Math.abs(dir.y)));

            for (StructureBlock b : PhysicsWorld.blocksIn(level, ImpactShape.capsule(origin, to, 0.55), s -> BlockImpactService.allowed(player, s), 64)) {
               found.add(new HeavyBody.Candidate(b.localPos(), b.state(), b.structure(), b.worldCenter(), b.worldCenter().subtract(origin).dot(dir)));
            }
         }

         if (found.isEmpty()) {
            return new HeavyBody.Smash(0, false, energy);
         } else {
            found.sort(Comparator.comparingDouble(HeavyBody.Candidate::along));
            double startEnergy = energy;
            boolean press = KiloKilo.weight(player).mode() == 2;
            CastBudget budget = new CastBudget(Math.max(0, Math.min(perTick - st.blocksThisTick, perCast - st.blocksThisFall)), 6);
            double massUnits = StructureForces.massUnits(kg) * (Double)PhysicsConfig.MASS_SCALE.get();
            int broken = 0;
            boolean stopped = false;
            HeavyBody.Candidate first = null;

            for (HeavyBody.Candidate c : found) {
               BlockPos pos = c.pos();
               if (st.runStart != null && WeightCurves.smashDepth(st.runStart, st.runDir, c.center()) > maxDepth + 1.0E-6) {
                  stopped = true;
                  break;
               }

               double cost = WeightCurves.blockCost(
                  (double)c.state().getDestroySpeed(level, pos), (double)c.state().getExplosionResistance(level, pos, null), base
               );
               double before = WeightCurves.speedFor(kg, energy);
               if (!allowed || energy < cost || !budget.hasBlocks()) {
                  stopped = true;
                  if (ramOnStop) {
                     ram(player, c, dir, massUnits * before * 20.0);
                  }
                  break;
               }

               Vec3 fling = dir.scale(0.35 * Math.min(1.0, before)).add(0.0, 0.15, 0.0);
               if (!TerrainDestruction.tearOut(level, player, pos, fling, budget, player.getRandom().nextInt(40), true, false)) {
                  stopped = true;
                  if (ramOnStop) {
                     ram(player, c, dir, massUnits * before * 20.0);
                  }
                  break;
               }

               energy -= cost;
               broken++;
               if (first == null) {
                  first = c;
               }

               if (st.runStart == null) {
                  st.runStart = c.center();
                  st.runDir = dir;
                  if (press && !st.burst) {
                     pressBurst(player, c.center().subtract(dir.scale(0.5)), startEnergy);
                  }
               }

               double after = WeightCurves.speedFor(kg, energy);
               ram(player, c, dir, massUnits * (before - after) * 20.0);
               if (c.structure() != null) {
                  BlockImpactService.decal(level, c.structure(), pos, c.structure().dirToLocal(dir.scale(-1.0)), BlockImpactService.Decal.DENT, 1.4F);
               }
            }

            st.blocksThisTick += broken;
            if (st.runStart != null) {
               st.runEnergy = energy;
            }

            if (first != null) {
               Vec3 at = first.center();
               float left = (float)Math.min(1.0, WeightCurves.speedFor(kg, energy) / Math.max(0.01, speed));
               ServerFx.sendNear(
                  level,
                  KiloFx.PUNCH,
                  player.getId(),
                  at,
                  dir,
                  left,
                  player.getRandom().nextLong(),
                  net.minecraft.world.level.block.Block.getId(first.state()),
                  64.0
               );
            }

            return new HeavyBody.Smash(broken, stopped, energy);
         }
      }
   }

   private static void ram(ServerPlayer player, HeavyBody.Candidate c, Vec3 dir, double impulse) {
      if (c.structure() != null && impulse > 0.0) {
         StructureForces.ram(player, c.structure(), c.center().subtract(dir.scale(0.5)), dir.scale(impulse));
      }
   }

   public static double heightAboveGround(ServerPlayer player, double max) {
      Vec3 from = player.position();
      HitResult hit = PhysicsWorld.clip(player.level(), new ClipContext(from, from.add(0.0, -max, 0.0), Block.COLLIDER, Fluid.ANY, player));
      return hit.getType() == Type.MISS ? max : from.y - hit.getLocation().y;
   }

   static void setMotion(ServerPlayer player, Vec3 velocity) {
      player.setDeltaMovement(velocity);
      player.hasImpulse = true;
      player.connection.send(new ClientboundSetEntityMotionPacket(player));
   }

   public static void onJump(ServerPlayer player) {
      BodyWeight w = KiloKilo.weight(player);
      double light = WeightCurves.lightness(w.kg());
      if (!(light <= 0.05)) {
         HeavyBody.State st = state(player);
         st.jumpStartY = player.getY();
         st.jumpPeak = player.getY();
         double height = WeightCurves.jumpHeight(w.kg(), WeightManager.tuning());
         ServerFx.send(player, KiloFx.LIGHT_JUMP, player.position(), Vec3.ZERO, (float)height, player.getRandom().nextLong(), 0);
      }
   }

   private HeavyBody() {
   }

   private static record Candidate(BlockPos pos, BlockState state, @Nullable PhysicsStructure structure, Vec3 center, double along) {
   }

   static final class Creak {
      final int need;
      final int crackId;
      int ticks;
      int stage = -1;

      Creak(int need) {
         this.need = need;
         this.crackId = -1275068416 - (HeavyBody.crackIds++ & 1048575);
      }
   }

   static record Floor(BlockPos top, List<BlockPos> column, @Nullable PhysicsStructure structure) {
   }

   public static record Smash(int broken, boolean stopped, double energyLeft) {
   }

   static final class State {
      double lastX;
      double lastY;
      double lastZ;
      boolean init;
      Vec3 velocity = Vec3.ZERO;
      double stride;
      int plunge = -1;
      double carry;
      int blocksThisFall;
      int blocksThisTick;
      long tick = -1L;
      double jumpStartY = Double.NaN;
      double jumpPeak;
      @Nullable
      UUID loadStructure;
      double loadStartY;
      long loadStart;
      int groan;
      int groundTicks;
      @Nullable
      Vec3 runStart;
      Vec3 runDir = Vec3.ZERO;
      double runEnergy = -1.0;
      boolean wasGround = true;
      double airFall;
      final Map<BlockPos, HeavyBody.Creak> creaks = new HashMap<>();
      final Map<Integer, Long> contacts = new HashMap<>();
      boolean burst;
   }
}
