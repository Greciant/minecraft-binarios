package com.awakenedmod.awakened.content.earth;

import com.awakenedmod.awakened.core.Harm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public enum EarthMaterial {
   STONE(Items.COBBLESTONE, Blocks.COBBLESTONE.defaultBlockState(), 8.0F, 2.0F, 2.0F, 0.95F, 1.7, 14, 0, 0, 1.0),
   GRAVEL(Items.GRAVEL, Blocks.GRAVEL.defaultBlockState(), 6.0F, 3.0F, 3.2F, 0.5F, 1.7, 6, 0, 6, 0.85),
   SAND(Items.SAND, Blocks.SAND.defaultBlockState(), 5.0F, 2.0F, 3.0F, 0.5F, 1.6, 8, 70, 0, 0.85),
   DIRT(Items.DIRT, Blocks.DIRT.defaultBlockState(), 4.0F, 1.0F, 1.6F, 0.4F, 2.4, 4, 0, 0, 0.65),
   NETHERRACK(STONE, Blocks.NETHERRACK, Blocks.NETHER_BRICKS),
   CRIMSON_NYLIUM(STONE, Blocks.CRIMSON_NYLIUM, Blocks.NETHER_WART_BLOCK),
   WARPED_NYLIUM(STONE, Blocks.WARPED_NYLIUM, Blocks.WARPED_WART_BLOCK),
   BASALT(STONE, Blocks.BASALT, Blocks.POLISHED_BASALT),
   SOUL_SAND(SAND, Blocks.SOUL_SAND, Blocks.SOUL_SOIL),
   SOUL_SOIL(DIRT, Blocks.SOUL_SOIL, Blocks.SOUL_SAND),
   END_STONE(STONE, Blocks.END_STONE, Blocks.END_STONE_BRICKS);

   private final EarthMaterial kind;
   private final Block accent;
   private final Item item;
   private final BlockState state;
   private final float damage;
   private final float aoeDamage;
   private final float aoeRadius;
   private final float knockback;
   private final double launchSpeed;
   private final int staggerTicks;
   private final int blindTicks;
   private final int pellets;
   private final double liftFactor;

   private EarthMaterial(
      Item item,
      BlockState state,
      float damage,
      float aoeDamage,
      float aoeRadius,
      float knockback,
      double launchSpeed,
      int staggerTicks,
      int blindTicks,
      int pellets,
      double liftFactor
   ) {
      this(null, item, state, damage, aoeDamage, aoeRadius, knockback, launchSpeed, staggerTicks, blindTicks, pellets, liftFactor, null);
   }

   private EarthMaterial(EarthMaterial kind, Block look, Block accent) {
      this(
         kind,
         look.asItem(),
         look.defaultBlockState(),
         kind.damage,
         kind.aoeDamage,
         kind.aoeRadius,
         kind.knockback,
         kind.launchSpeed,
         kind.staggerTicks,
         kind.blindTicks,
         kind.pellets,
         kind.liftFactor,
         accent
      );
   }

   private EarthMaterial(
      EarthMaterial kind,
      Item item,
      BlockState state,
      float damage,
      float aoeDamage,
      float aoeRadius,
      float knockback,
      double launchSpeed,
      int staggerTicks,
      int blindTicks,
      int pellets,
      double liftFactor,
      Block accent
   ) {
      this.kind = kind;
      this.accent = accent;
      this.item = item;
      this.state = state;
      this.damage = damage;
      this.aoeDamage = aoeDamage;
      this.aoeRadius = aoeRadius;
      this.knockback = knockback;
      this.launchSpeed = launchSpeed;
      this.staggerTicks = staggerTicks;
      this.blindTicks = blindTicks;
      this.pellets = pellets;
      this.liftFactor = liftFactor;
   }

   public EarthMaterial kind() {
      return this.kind == null ? this : this.kind;
   }

   public Block accent() {
      if (this.accent != null) {
         return this.accent;
      } else {
         return switch (this) {
            case GRAVEL -> Blocks.TUFF;
            case SAND -> Blocks.SMOOTH_SANDSTONE;
            case DIRT -> Blocks.COARSE_DIRT;
            default -> Blocks.MOSSY_COBBLESTONE;
         };
      }
   }

   public Item item() {
      return this.item;
   }

   public BlockState state() {
      return this.state;
   }

   public float damage() {
      return this.damage;
   }

   public float aoeDamage() {
      return this.aoeDamage;
   }

   public float aoeRadius() {
      return this.aoeRadius;
   }

   public float knockback() {
      return this.knockback;
   }

   public double launchSpeed() {
      return this.launchSpeed;
   }

   public int staggerTicks() {
      return this.staggerTicks;
   }

   public int blindTicks() {
      return this.blindTicks;
   }

   public int pellets() {
      return this.pellets;
   }

   public boolean blinds() {
      return this.blindTicks > 0;
   }

   public double liftFactor() {
      return this.liftFactor;
   }

   public void stagger(Entity source, LivingEntity foe) {
      if (this.staggerTicks > 0) {
         Harm.effect(source, foe, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, this.staggerTicks, 3, false, true));
      }

      if (this.blindTicks > 0) {
         Harm.effect(source, foe, new MobEffectInstance(MobEffects.BLINDNESS, this.blindTicks, 0));
      }
   }

   public static EarthMaterial under(ServerPlayer player) {
      return under((Entity)player);
   }

   public static EarthMaterial under(Entity entity) {
      Level level = entity.level();
      BlockPos foot = entity.blockPosition();
      BlockState bs = level.getBlockState(foot.below());
      if (bs.isAir()) {
         bs = level.getBlockState(foot);
      }

      return of(bs, level);
   }

   public static EarthMaterial of(BlockState bs, Level level) {
      if (bs.is(Blocks.NETHERRACK)) {
         return NETHERRACK;
      } else if (bs.is(Blocks.CRIMSON_NYLIUM)) {
         return CRIMSON_NYLIUM;
      } else if (bs.is(Blocks.WARPED_NYLIUM)) {
         return WARPED_NYLIUM;
      } else if (bs.is(Blocks.BASALT) || bs.is(Blocks.POLISHED_BASALT) || bs.is(Blocks.SMOOTH_BASALT)) {
         return BASALT;
      } else if (bs.is(Blocks.SOUL_SAND)) {
         return SOUL_SAND;
      } else if (bs.is(Blocks.SOUL_SOIL)) {
         return SOUL_SOIL;
      } else if (bs.is(Blocks.END_STONE)) {
         return END_STONE;
      } else if (bs.is(Blocks.GRAVEL)) {
         return GRAVEL;
      } else if (bs.is(BlockTags.SAND)) {
         return SAND;
      } else if (bs.is(BlockTags.DIRT)) {
         return DIRT;
      } else if (level.dimension() == Level.NETHER) {
         return NETHERRACK;
      } else {
         return level.dimension() == Level.END ? END_STONE : STONE;
      }
   }

   public static boolean standingOnEarth(ServerPlayer player) {
      if (!player.onGround()) {
         return false;
      } else {
         BlockPos below = BlockPos.containing(player.getX(), player.getBoundingBox().minY - 0.02, player.getZ());
         BlockState bs = player.serverLevel().getBlockState(below);
         return bs.isFaceSturdy(player.serverLevel(), below, Direction.UP) && !bs.is(BlockTags.LEAVES) && !bs.is(BlockTags.LOGS);
      }
   }
}
