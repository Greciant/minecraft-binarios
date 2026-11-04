package com.awakenedmod.awakened.content.limitless.red;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.limitless.LimitlessBufs;
import com.awakenedmod.awakened.content.vfx.AbstractFxEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class RedBlastEntity extends AbstractFxEntity {
   private int casterId = -1;
   private int fireTick;
   private Vec3 from = Vec3.ZERO;
   private Vec3 fwd = new Vec3(0.0, 0.0, 1.0);
   private int hitAge;
   private double floorY;
   private long seed;

   public RedBlastEntity(EntityType<? extends RedBlastEntity> type, Level level) {
      super(type, level);
   }

   public static RedBlastEntity spawn(ServerLevel level, int casterId, int fireTick, Vec3 from, Vec3 fwd, Vec3 burst, int flight, double floorY, long seed) {
      RedBlastEntity e = new RedBlastEntity(AwakenedEntities.LIMITLESS_RED_BLAST.get(), level);
      e.setPos(burst.x, burst.y, burst.z);
      e.casterId = casterId;
      e.fireTick = fireTick;
      e.from = from;
      e.fwd = fwd;
      e.hitAge = 14 + flight;
      e.floorY = floorY;
      e.seed = seed;
      e.lifetime = flight + 50;
      level.addFreshEntity(e);
      return e;
   }

   public int casterId() {
      return this.casterId;
   }

   public int fireTick() {
      return this.fireTick;
   }

   public float age(float partialTick) {
      return (float)(14 + ((int)this.level().getGameTime() - this.fireTick)) + partialTick;
   }

   public Vec3 from() {
      return this.from;
   }

   public Vec3 fwd() {
      return this.fwd;
   }

   public Vec3 burst() {
      return this.position();
   }

   public int hitAge() {
      return this.hitAge;
   }

   public double floorY() {
      return this.floorY;
   }

   public long seed() {
      return this.seed;
   }

   @Override
   protected void writeExtraSpawnData(RegistryFriendlyByteBuf buf) {
      buf.writeVarInt(this.casterId);
      buf.writeInt(this.fireTick);
      LimitlessBufs.writeVec(buf, this.from);
      LimitlessBufs.writeVec(buf, this.fwd);
      buf.writeVarInt(this.hitAge);
      buf.writeDouble(this.floorY);
      buf.writeLong(this.seed);
   }

   @Override
   protected void readExtraSpawnData(RegistryFriendlyByteBuf buf) {
      this.casterId = buf.readVarInt();
      this.fireTick = buf.readInt();
      this.from = LimitlessBufs.readVec(buf);
      this.fwd = LimitlessBufs.readVec(buf);
      this.hitAge = buf.readVarInt();
      this.floorY = buf.readDouble();
      this.seed = buf.readLong();
   }
}
