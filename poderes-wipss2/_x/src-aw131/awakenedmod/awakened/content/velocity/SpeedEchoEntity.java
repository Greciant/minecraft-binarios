package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.content.vfx.AbstractFxEntity;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SpeedEchoEntity extends AbstractFxEntity {
   private UUID playerId = new UUID(0L, 0L);
   private float yawDeg;
   private float limbPhase;
   private float limbAmount;
   private float velX;
   private float velY;
   private float velZ;
   private int gear;
   private int colorId = VelocityPalette.DEFAULT_DYE.getId();

   public SpeedEchoEntity(EntityType<? extends SpeedEchoEntity> type, Level level) {
      super(type, level);
   }

   public void configure(UUID playerId, float yawDeg, float limbPhase, float limbAmount, Vec3 velocity, int gear, int colorId, int lifetime) {
      this.playerId = playerId != null ? playerId : new UUID(0L, 0L);
      this.yawDeg = yawDeg;
      this.limbPhase = limbPhase;
      this.limbAmount = limbAmount;
      this.velX = (float)velocity.x;
      this.velY = (float)velocity.y;
      this.velZ = (float)velocity.z;
      this.gear = gear;
      this.colorId = colorId;
      this.lifetime = lifetime;
   }

   public UUID playerId() {
      return this.playerId;
   }

   public float yawDeg() {
      return this.yawDeg;
   }

   public float limbPhase() {
      return this.limbPhase;
   }

   public float limbAmount() {
      return this.limbAmount;
   }

   public Vec3 velocity() {
      return new Vec3((double)this.velX, (double)this.velY, (double)this.velZ);
   }

   public int gear() {
      return this.gear;
   }

   public int colorId() {
      return this.colorId;
   }

   @Override
   protected void writeExtraSpawnData(RegistryFriendlyByteBuf buf) {
      buf.writeUUID(this.playerId);
      buf.writeFloat(this.yawDeg);
      buf.writeFloat(this.limbPhase);
      buf.writeFloat(this.limbAmount);
      buf.writeFloat(this.velX);
      buf.writeFloat(this.velY);
      buf.writeFloat(this.velZ);
      buf.writeVarInt(this.gear);
      buf.writeVarInt(this.colorId);
   }

   @Override
   protected void readExtraSpawnData(RegistryFriendlyByteBuf buf) {
      this.playerId = buf.readUUID();
      this.yawDeg = buf.readFloat();
      this.limbPhase = buf.readFloat();
      this.limbAmount = buf.readFloat();
      this.velX = buf.readFloat();
      this.velY = buf.readFloat();
      this.velZ = buf.readFloat();
      this.gear = buf.readVarInt();
      this.colorId = buf.readVarInt();
   }
}
