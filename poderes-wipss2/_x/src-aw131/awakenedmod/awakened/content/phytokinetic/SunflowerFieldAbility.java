package com.awakenedmod.awakened.content.phytokinetic;

import com.awakenedmod.awakened.content.vfx.BlockFxEntity;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TorchflowerCropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SunflowerFieldAbility extends Ability {
   public static final String ID = "sunflower_field";
   private static final float COST = 20.0F;
   private static final int UNLOCK = 3;
   private static final double PLANT_RANGE = 12.0;
   private static final double RADIUS = 4.0;
   private static final int LOB_TICKS = 6;
   private static final int CHARGE_TICKS = 25;
   private static final int BLOOM_TICKS = 180;
   private static final int INNER_FLOWERS = 4;
   private static final int OUTER_FLOWERS = 6;
   private static final int GRASS_TUFTS = 8;
   private static final double MAX_GROUND_DROP = 3.0;
   private static final int WAVE_TICKS = 14;
   private static final int RISE_TICKS = 6;
   private static final int HEAD_LAG = 3;
   private static final float MOTHER_SCALE = 1.3F;
   private static final float YAW_JITTER = 16.0F;
   private static final int WILT_JITTER = 16;
   private static final int STEM_LAG = 5;
   private static final int DEAD_BUSH_TICKS = 22;
   private static final int HEAL_INTERVAL = 10;
   private static final float HEAL_PER_TICK = 1.0F;
   private static final double HEAL_HEIGHT = 2.5;
   private static final ItemParticleOption SEED_CRUMBS = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.WHEAT_SEEDS));
   private final Map<UUID, SunflowerFieldAbility.Field> fields = this.playerState();

   public SunflowerFieldAbility() {
      super("sunflower_field", 20.0F, 3);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().healing(1.0).radius(4.0).build();
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.fields.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.feedback.sunflower_field_active", new Object[0]);
      } else {
         ServerLevel level = player.serverLevel();
         Vec3 base = AimUtil.groundedPositionIfFound(level, AimUtil.aimedPosition(player, 12.0)).orElse(null);
         if (base == null) {
            return this.fail(player, "awakened.feedback.sunflower_field_no_ground", new Object[0]);
         } else {
            List<SunflowerFieldAbility.Plant> plants = plan(level, base);
            if (plants.isEmpty()) {
               return this.fail(player, "awakened.feedback.sunflower_field_no_ground", new Object[0]);
            } else {
               Vec3 hand = AimUtil.handPosition(player);
               this.fields.put(player.getUUID(), new SunflowerFieldAbility.Field(base, hand, plants));
               Fx.burst(level, PhytoFx.LEAF_DUST, hand, 6, 0.2, 0.03);
               Fx.sound(level, hand, SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.6F, 1.4F);
               return true;
            }
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      SunflowerFieldAbility.Field field = this.fields.get(player.getUUID());
      if (field != null) {
         ServerLevel level = player.serverLevel();
         field.tick++;
         if (field.tick <= 6) {
            this.tickSeedLob(level, field);
         } else {
            int bloomStart = 31;
            if (field.tick < bloomStart) {
               this.tickSprouting(level, field);
            } else {
               if (field.tick == bloomStart) {
                  this.startBloom(level, field);
               }

               this.tickBloomWave(level, field);
               this.tickWiltChoreo(level, field);
               this.tickHealing(level, field, player);
               if (field.tick - bloomStart >= 180) {
                  this.fields.remove(player.getUUID());
                  this.expire(level, field);
               } else {
                  this.tickAmbient(level, field);
               }
            }
         }
      }
   }

   private void tickSeedLob(ServerLevel level, SunflowerFieldAbility.Field field) {
      double t = (double)field.tick / 6.0;
      Vec3 p = field.hand.add(field.base.add(0.0, 0.2, 0.0).subtract(field.hand).scale(t)).add(0.0, 4.4 * t * (1.0 - t), 0.0);
      Fx.burst(level, SEED_CRUMBS, p, 2, 0.05, 0.02);
      if (field.tick == 6) {
         Fx.burst(level, ParticleTypes.COMPOSTER, field.base.add(0.0, 0.2, 0.0), 8, 0.3, 0.05);
         Fx.sound(level, field.base, SoundEvents.CROP_PLANTED, SoundSource.PLAYERS, 1.0F, 0.9F);
         Fx.sound(level, field.base, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.PLAYERS, 0.8F, 1.1F);
      }
   }

   private void tickSprouting(ServerLevel level, SunflowerFieldAbility.Field field) {
      RandomSource random = level.getRandom();
      int chargeTick = field.tick - 6;

      BlockState stage = switch (chargeTick / 8) {
         case 0 -> Blocks.TORCHFLOWER_CROP.defaultBlockState();
         case 1 -> (BlockState)Blocks.TORCHFLOWER_CROP.defaultBlockState().setValue(TorchflowerCropBlock.AGE, 1);
         default -> Blocks.SUNFLOWER.defaultBlockState();
      };
      if (chargeTick % 8 == 1) {
         if (field.seedling != null) {
            field.seedling.discard();
         }

         field.seedling = Vfx.blockState(level, field.base, stage, 1.0, 1.0, 1.0, 35, 0.0F, 3, true);
         float progress = (float)chargeTick / 25.0F;
         Fx.burst(level, PhytoFx.LEAF_DUST, field.base.add(0.0, 0.4, 0.0), 5, 0.2, 0.04);
         Fx.sound(level, field.base, SoundEvents.AZALEA_LEAVES_PLACE, SoundSource.PLAYERS, 0.8F, 0.8F + progress * 0.6F);
      }

      for (int i = 0; i < 3; i++) {
         double angle = random.nextDouble() * Math.PI * 2.0;
         double dist = 1.2 + random.nextDouble() * 1.4;
         Vec3 from = field.base.add(Math.cos(angle) * dist, 0.2 + random.nextDouble() * 0.8, Math.sin(angle) * dist);
         Vec3 vel = field.base.add(0.0, 0.4, 0.0).subtract(from).scale(0.22);
         level.sendParticles(ParticleTypes.COMPOSTER, from.x, from.y, from.z, 0, vel.x, vel.y, vel.z, 1.0);
      }
   }

   private void startBloom(ServerLevel level, SunflowerFieldAbility.Field field) {
      if (field.seedling != null) {
         field.seedling.discard();
         field.seedling = null;
      }

      Vfx.effect(VfxEntity.Kind.DOME, field.base).scale(4.5).color(16765498, 16774064).lifetime(16).intensity(0.7F).spawn(level);
      Vfx.ring(level, field.base.add(0.0, 0.1, 0.0), 5.0, 16765498, 16);
      Fx.burst(level, PhytoFx.POLLEN_DUST, field.base.add(0.0, 1.0, 0.0), 30, 2.4, 0.08);
      PhytoFx.leafBurst(level, field.base.add(0.0, 0.5, 0.0), 12);
      Fx.sound(level, field.base, SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 1.2F, 0.9F);
      Fx.sound(level, field.base, SoundEvents.AZALEA_PLACE, SoundSource.PLAYERS, 1.1F, 0.8F);
      Fx.sound(level, field.base, SoundEvents.CHERRY_LEAVES_PLACE, SoundSource.PLAYERS, 1.0F, 0.7F);
   }

   private void tickBloomWave(ServerLevel level, SunflowerFieldAbility.Field field) {
      BlockState lowerState = Blocks.SUNFLOWER.defaultBlockState();
      BlockState upperState = (BlockState)lowerState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);

      for (SunflowerFieldAbility.Plant plant : field.plants) {
         if (field.tick == plant.bloomAt) {
            int lifetime = remainingLife(field);
            if (plant.grass) {
               plant.lower = Vfx.blockState(
                  level,
                  plant.spot,
                  Blocks.SHORT_GRASS.defaultBlockState(),
                  (double)plant.scale,
                  (double)plant.scale,
                  (double)plant.scale,
                  lifetime,
                  plant.yaw,
                  6,
                  true
               );
            } else {
               plant.lower = Vfx.blockState(
                  level, plant.spot, lowerState, (double)plant.scale, (double)plant.scale, (double)plant.scale, lifetime, plant.yaw, 6, true
               );
               PhytoFx.leafBurst(level, plant.spot.add(0.0, 0.4, 0.0), 3);
               float wave = Math.min(1.0F, (float)(plant.bloomAt - 6 - 25) / 14.0F);
               Fx.sound(level, plant.spot, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.PLAYERS, 0.55F, 0.8F + 0.6F * wave);
            }
         } else if (!plant.grass && field.tick == plant.bloomAt + 3) {
            plant.upper = Vfx.blockState(
               level,
               plant.spot.add(0.0, (double)plant.scale, 0.0),
               upperState,
               (double)plant.scale,
               (double)plant.scale,
               (double)plant.scale,
               remainingLife(field),
               plant.yaw,
               5,
               true
            );
            Fx.burst(level, PhytoFx.POLLEN_DUST, plant.spot.add(0.0, (double)plant.scale + 0.7, 0.0), 3, 0.2, 0.03);
         }
      }
   }

   private static int remainingLife(SunflowerFieldAbility.Field field) {
      return 211 - field.tick + 40;
   }

   private void tickWiltChoreo(ServerLevel level, SunflowerFieldAbility.Field field) {
      for (SunflowerFieldAbility.Plant plant : field.plants) {
         if (field.tick == plant.wiltAt && plant.lower != null) {
            if (plant.grass) {
               plant.lower.discard();
               plant.lower = null;
               Fx.burst(level, PhytoFx.LEAF_DUST, plant.spot.add(0.0, 0.3, 0.0), 3, 0.2, 0.02);
            } else {
               if (plant.upper != null) {
                  plant.upper.discard();
                  plant.upper = null;
               }

               Vec3 head = plant.spot.add(0.0, (double)plant.scale + 0.6, 0.0);
               Fx.burst(level, PhytoFx.BARK_DUST, head, 6, 0.3, 0.04);
               PhytoFx.fallingPetals(level, head, -9747419, 4);
               Fx.sound(level, plant.spot, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.PLAYERS, 0.5F, 0.55F);
            }
         } else if (field.tick == plant.wiltAt + 5 && !plant.grass && plant.lower != null) {
            plant.lower.discard();
            plant.lower = Vfx.blockState(
               level, plant.spot, Blocks.DEAD_BUSH.defaultBlockState(), (double)plant.scale, (double)plant.scale, (double)plant.scale, 22, plant.yaw, 2, true
            );
            Fx.burst(level, PhytoFx.BARK_DUST, plant.spot.add(0.0, 0.4, 0.0), 3, 0.2, 0.03);
            Fx.sound(level, plant.spot, SoundEvents.GRASS_BREAK, SoundSource.PLAYERS, 0.5F, 0.7F);
         }
      }
   }

   private void tickAmbient(ServerLevel level, SunflowerFieldAbility.Field field) {
      RandomSource random = level.getRandom();
      if (field.tick % 16 == 0) {
         Vfx.effect(VfxEntity.Kind.HALO, field.base.add(0.0, 0.12, 0.0)).scale(4.0).color(9234544, 16765498).lifetime(22).intensity(0.55F).spawn(level);
      }

      for (int i = 0; i < 3; i++) {
         Vec3 drift = PhytoFx.inDisc(random, field.base, 4.0).add(0.0, 0.3 + random.nextDouble() * 1.6, 0.0);
         Fx.burst(level, PhytoFx.POLLEN_DUST, drift, 1, 0.1, 0.01);
      }

      Vec3 spore = PhytoFx.inDisc(random, field.base, 3.6).add(0.0, 1.0 + random.nextDouble(), 0.0);
      Fx.burst(level, ParticleTypes.SPORE_BLOSSOM_AIR, spore, 1, 0.2, 0.0);
      if (field.tick % 4 == 0 && !field.plants.isEmpty()) {
         SunflowerFieldAbility.Plant plant = field.plants.get(random.nextInt(field.plants.size()));
         if (!plant.grass && plant.upper != null) {
            Fx.burst(level, ParticleTypes.HAPPY_VILLAGER, plant.spot.add(0.0, (double)plant.scale + 0.8, 0.0), 1, 0.2, 0.02);
            if (random.nextInt(3) == 0) {
               PhytoFx.fallingPetals(level, plant.spot.add(0.0, (double)plant.scale + 0.6, 0.0), -11718, 1);
            }
         }
      }
   }

   private void tickHealing(ServerLevel level, SunflowerFieldAbility.Field field, ServerPlayer caster) {
      if (field.tick % 10 == 0) {
         float heal = MasteryScaling.scaled(1.0F, caster);
         AABB volume = new AABB(field.base, field.base).inflate(4.0, 2.5, 4.0);
         boolean healedAlly = false;

         for (Player p : level.getEntitiesOfClass(Player.class, volume, LivingEntity::isAlive)) {
            double dx = p.getX() - field.base.x;
            double dz = p.getZ() - field.base.z;
            if (!(dx * dx + dz * dz > 16.0) && !(p.getHealth() >= p.getMaxHealth())) {
               p.heal(heal);
               Fx.burst(level, ParticleTypes.HEART, p.position().add(0.0, (double)p.getBbHeight() + 0.2, 0.0), 1, 0.2, 0.01);
               if (p != caster) {
                  healedAlly = true;
               }
            }
         }

         if (healedAlly) {
            MasterySystem.award(caster, MasteryReason.HIT);
         }
      }
   }

   private void expire(ServerLevel level, SunflowerFieldAbility.Field field) {
      discardModels(field);
      Fx.burst(level, PhytoFx.BARK_DUST, field.base.add(0.0, 0.5, 0.0), 12, 2.0, 0.04);
      Fx.sound(level, field.base, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.PLAYERS, 0.8F, 0.7F);
   }

   private static List<SunflowerFieldAbility.Plant> plan(ServerLevel level, Vec3 base) {
      RandomSource random = level.getRandom();
      List<Vec3> flowerSpots = new ArrayList<>();
      addSpot(level, base, base, flowerSpots);
      addRing(level, base, 1.8, 4, random, flowerSpots);
      addRing(level, base, 3.4, 6, random, flowerSpots);
      int fieldEnd = 211;
      List<SunflowerFieldAbility.Plant> plants = new ArrayList<>();
      boolean mother = true;

      for (Vec3 spot : flowerSpots) {
         plants.add(
            new SunflowerFieldAbility.Plant(
               spot, waveArrival(base, spot, random), fieldEnd - 5 - 22 - random.nextInt(16), (random.nextFloat() - 0.5F) * 16.0F, mother ? 1.3F : 1.0F, false
            )
         );
         mother = false;
      }

      for (int i = 0; i < 8; i++) {
         List<Vec3> one = new ArrayList<>(1);
         addSpot(level, base, PhytoFx.inDisc(random, base, 3.8), one);
         if (!one.isEmpty()) {
            plants.add(
               new SunflowerFieldAbility.Plant(
                  one.get(0), waveArrival(base, one.get(0), random), fieldEnd - 5 - random.nextInt(16), random.nextFloat() * 360.0F, 0.9F, true
               )
            );
         }
      }

      return plants;
   }

   private static int waveArrival(Vec3 base, Vec3 spot, RandomSource random) {
      double fraction = Math.min(1.0, spot.distanceTo(base) / 4.0);
      return 31 + (int)Math.round(fraction * 14.0) + random.nextInt(3);
   }

   private static void addRing(ServerLevel level, Vec3 base, double radius, int count, RandomSource random, List<Vec3> spots) {
      double startAngle = random.nextDouble() * Math.PI * 2.0;

      for (int i = 0; i < count; i++) {
         double angle = startAngle + (Math.PI * 2) * (double)i / (double)count;
         Vec3 raw = base.add(Math.cos(angle) * radius + (random.nextDouble() - 0.5) * 0.8, 0.0, Math.sin(angle) * radius + (random.nextDouble() - 0.5) * 0.8);
         addSpot(level, base, raw, spots);
      }
   }

   private static void addSpot(ServerLevel level, Vec3 base, Vec3 raw, List<Vec3> spots) {
      AimUtil.groundedPositionIfFound(level, raw.add(0.0, 1.0, 0.0)).filter(grounded -> Math.abs(grounded.y - base.y) <= 3.0).ifPresent(spots::add);
   }

   private static void discardModels(SunflowerFieldAbility.Field field) {
      for (SunflowerFieldAbility.Plant plant : field.plants) {
         if (plant.lower != null) {
            plant.lower.discard();
         }

         if (plant.upper != null) {
            plant.upper.discard();
         }
      }

      if (field.seedling != null) {
         field.seedling.discard();
         field.seedling = null;
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      SunflowerFieldAbility.Field field = this.fields.get(player.getUUID());
      if (field != null) {
         discardModels(field);
      }

      super.clearPlayer(player);
   }

   private static final class Field {
      final Vec3 base;
      final Vec3 hand;
      final List<SunflowerFieldAbility.Plant> plants;
      int tick;
      BlockFxEntity seedling;

      Field(Vec3 base, Vec3 hand, List<SunflowerFieldAbility.Plant> plants) {
         this.base = base;
         this.hand = hand;
         this.plants = plants;
      }
   }

   private static final class Plant {
      final Vec3 spot;
      final int bloomAt;
      final int wiltAt;
      final float yaw;
      final float scale;
      final boolean grass;
      BlockFxEntity lower;
      BlockFxEntity upper;

      Plant(Vec3 spot, int bloomAt, int wiltAt, float yaw, float scale, boolean grass) {
         this.spot = spot;
         this.bloomAt = bloomAt;
         this.wiltAt = wiltAt;
         this.yaw = yaw;
         this.scale = scale;
         this.grass = grass;
      }
   }
}
