package com.awakenedmod.awakened.content.earth;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.vfx.AbstractFxEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class StoneConstructEntity extends AbstractFxEntity {
   private int blockId;
   private float width = 1.0F;
   private float height = 1.0F;
   private float yawDeg;
   private boolean solid = true;

   public StoneConstructEntity(EntityType<? extends StoneConstructEntity> type, Level level) {
      super(type, level);
   }

   public void configure(Block block, float width, float height, int lifetime, float yawDeg) {
      this.configure(block, width, height, lifetime, yawDeg, true);
   }

   public void configure(Block block, float width, float height, int lifetime, float yawDeg, boolean solid) {
      this.blockId = BuiltInRegistries.BLOCK.getId(block);
      this.width = width;
      this.height = height;
      this.lifetime = lifetime;
      this.yawDeg = yawDeg;
      this.solid = solid;
      this.refreshDimensions();
   }

   public static StoneConstructEntity spawn(ServerLevel level, Vec3 feet, Block block, float width, float height, int lifetime, float yawDeg) {
      return spawn(level, feet, block, width, height, lifetime, yawDeg, true);
   }

   public static StoneConstructEntity spawn(ServerLevel level, Vec3 feet, Block block, float width, float height, int lifetime, float yawDeg, boolean solid) {
      StoneConstructEntity e = new StoneConstructEntity(AwakenedEntities.STONE_CONSTRUCT.get(), level);
      e.configure(block, width, height, lifetime, yawDeg, solid);
      e.setPos(feet.x, feet.y, feet.z);
      level.addFreshEntity(e);
      return e;
   }

   public static StoneConstructEntity collider(ServerLevel level, Vec3 feet, float width, float height, int lifetime) {
      return spawn(level, feet, Blocks.AIR, width, height, lifetime, 0.0F, true);
   }

   public boolean canBeCollidedWith() {
      return this.solid;
   }

   public boolean isPickable() {
      return this.solid;
   }

   public EntityDimensions getDimensions(Pose pose) {
      return EntityDimensions.scalable(this.width, this.height);
   }

   public int blockId() {
      return this.blockId;
   }

   public float width() {
      return this.width;
   }

   public float height() {
      return this.height;
   }

   public float yawDeg() {
      return this.yawDeg;
   }

   @Override
   protected void writeExtraSpawnData(RegistryFriendlyByteBuf buf) {
      buf.writeVarInt(this.blockId);
      buf.writeFloat(this.width);
      buf.writeFloat(this.height);
      buf.writeFloat(this.yawDeg);
      buf.writeBoolean(this.solid);
   }

   @Override
   protected void readExtraSpawnData(RegistryFriendlyByteBuf buf) {
      this.blockId = buf.readVarInt();
      this.width = buf.readFloat();
      this.height = buf.readFloat();
      this.yawDeg = buf.readFloat();
      this.solid = buf.readBoolean();
      this.refreshDimensions();
   }
}
