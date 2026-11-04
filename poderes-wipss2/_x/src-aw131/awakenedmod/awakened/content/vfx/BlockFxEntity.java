package com.awakenedmod.awakened.content.vfx;

import com.awakenedmod.awakened.content.AwakenedEntities;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class BlockFxEntity extends AbstractFxEntity {
   private int stateId;
   private float sx = 1.0F;
   private float sy = 1.0F;
   private float sz = 1.0F;
   private float yawDeg;
   private float alpha = 1.0F;
   private int tint = 16777215;
   private int growTicks;
   private boolean anchorBottom;
   private int anchorId = -1;
   private float velX;
   private float velY;
   private float velZ;
   private double restY = Double.NaN;
   private float tumble;
   private int flightTicks;
   private boolean landed;
   private static final float GRAVITY = 0.052F;
   private static final float DRAG = 0.965F;
   private int delay;
   private float heaveRise;
   private float heaveTilt;
   private int glowRgb;
   private float glowStrength;
   private int floatDropAt;
   private int floatRiseTicks;
   private float floatRise;
   private float floatBob;
   private float floatPhase;
   private double floatBaseY;
   private float orbitSpeed;
   private double orbitX;
   private double orbitZ;
   private float dropVelY;

   public BlockFxEntity(EntityType<? extends BlockFxEntity> type, Level level) {
      super(type, level);
   }

   public void configure(Block block, float sx, float sy, float sz, int lifetime, float yawDeg) {
      this.configure(block.defaultBlockState(), sx, sy, sz, lifetime, yawDeg);
   }

   public void configure(BlockState state, float sx, float sy, float sz, int lifetime, float yawDeg) {
      this.stateId = Block.getId(state);
      this.sx = sx;
      this.sy = sy;
      this.sz = sz;
      this.lifetime = lifetime;
      this.yawDeg = yawDeg;
   }

   public void setAlpha(float alpha) {
      this.alpha = alpha;
   }

   public void setTint(int rgbTint) {
      this.tint = rgbTint;
   }

   public void setLaunch(Vec3 velocity, double restY, float tumbleDegPerTick) {
      this.velX = (float)velocity.x;
      this.velY = (float)velocity.y;
      this.velZ = (float)velocity.z;
      this.restY = restY;
      this.tumble = tumbleDegPerTick;
   }

   public void setFloat(
      double rise,
      int riseTicks,
      double bob,
      float spinDegPerTick,
      int dropAtTick,
      double restY,
      double dropVelY,
      double orbitX,
      double orbitZ,
      double orbitRadPerTick
   ) {
      this.floatRise = (float)rise;
      this.floatRiseTicks = Math.max(1, riseTicks);
      this.floatBob = (float)bob;
      this.floatPhase = (float)((this.getX() * 7.31 + this.getZ() * 3.17) % (Math.PI * 2));
      this.floatDropAt = Math.max(1, dropAtTick);
      this.floatBaseY = this.getY();
      this.tumble = spinDegPerTick;
      this.restY = restY;
      this.dropVelY = (float)Math.min(0.0, dropVelY);
      this.orbitX = orbitX;
      this.orbitZ = orbitZ;
      this.orbitSpeed = (float)orbitRadPerTick;
   }

   public static BlockFxEntity spawnFloating(
      ServerLevel level,
      Vec3 pos,
      BlockState state,
      double size,
      int lifetime,
      float yawDeg,
      double rise,
      int riseTicks,
      double bob,
      float spinDegPerTick,
      int dropAtTick,
      double restY,
      double dropVelY,
      double orbitX,
      double orbitZ,
      double orbitRadPerTick
   ) {
      BlockFxEntity e = new BlockFxEntity(AwakenedEntities.BLOCK_FX.get(), level);
      e.setPos(pos.x, pos.y, pos.z);
      e.configure(state, (float)size, (float)size, (float)size, lifetime, yawDeg);
      e.setFloat(rise, riseTicks, bob, spinDegPerTick, dropAtTick, restY, dropVelY, orbitX, orbitZ, orbitRadPerTick);
      level.addFreshEntity(e);
      return e;
   }

   public boolean floating() {
      return this.floatDropAt > 0;
   }

   public boolean launched() {
      return !Double.isNaN(this.restY);
   }

   public int flightTicks() {
      return this.flightTicks;
   }

   public boolean landed() {
      return this.landed;
   }

   public float tumble() {
      return this.tumble;
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.isRemoved() && this.launched() && !this.landed) {
         if (this.floating()) {
            if (this.tickCount < this.floatDropAt) {
               this.hang();
               return;
            }

            if (this.tickCount == this.floatDropAt) {
               this.velX = 0.0F;
               this.velZ = 0.0F;
               this.velY = this.dropVelY;
            }
         }

         this.velY -= 0.052F;
         this.velX *= 0.965F;
         this.velZ *= 0.965F;
         double nx = this.getX() + (double)this.velX;
         double ny = this.getY() + (double)this.velY;
         double nz = this.getZ() + (double)this.velZ;
         if (this.velY < 0.0F && ny <= this.restY) {
            ny = this.restY;
            this.landed = true;
         }

         this.setPos(nx, ny, nz);
         this.flightTicks++;
      }
   }

   private void hang() {
      float u = Math.min(1.0F, (float)this.tickCount / (float)this.floatRiseTicks);
      float ease = 1.0F - (1.0F - u) * (1.0F - u) * (1.0F - u);
      double y = this.floatBaseY
         + (double)(this.floatRise * ease)
         + (double)(this.floatBob * ease) * Math.sin((double)this.tickCount * 0.15 + (double)this.floatPhase);
      double x = this.getX();
      double z = this.getZ();
      if (this.orbitSpeed != 0.0F) {
         double dx = x - this.orbitX;
         double dz = z - this.orbitZ;
         double c = Math.cos((double)this.orbitSpeed);
         double s = Math.sin((double)this.orbitSpeed);
         x = this.orbitX + dx * c - dz * s;
         z = this.orbitZ + dx * s + dz * c;
      }

      this.setPos(x, y, z);
      this.flightTicks++;
   }

   public BlockState blockState() {
      return Block.stateById(this.stateId);
   }

   public float scaleX() {
      return this.sx;
   }

   public float scaleY() {
      return this.sy;
   }

   public float scaleZ() {
      return this.sz;
   }

   public float yawDeg() {
      return this.yawDeg;
   }

   public float alpha() {
      return this.alpha;
   }

   public int tint() {
      return this.tint;
   }

   public void setGrowTicks(int ticks) {
      this.growTicks = Math.max(0, ticks);
   }

   public void setAnchorBottom(boolean anchorBottom) {
      this.anchorBottom = anchorBottom;
   }

   public void setAnchorEntity(int entityId) {
      this.anchorId = entityId;
   }

   public int growTicks() {
      return this.growTicks;
   }

   public boolean anchorBottom() {
      return this.anchorBottom;
   }

   public int anchorId() {
      return this.anchorId;
   }

   public void setDelay(int ticks) {
      this.delay = Math.max(0, ticks);
   }

   public void setHeave(float rise, float tiltDeg) {
      this.heaveRise = rise;
      this.heaveTilt = tiltDeg;
   }

   public void setHeaveGlow(int rgb, float strength) {
      this.glowRgb = rgb;
      this.glowStrength = strength;
   }

   public int delay() {
      return this.delay;
   }

   public int glowRgb() {
      return this.glowRgb;
   }

   public float glowStrength() {
      return this.glowStrength;
   }

   public float heaveRise() {
      return this.heaveRise;
   }

   public float heaveTilt() {
      return this.heaveTilt;
   }

   @Override
   protected void writeExtraSpawnData(RegistryFriendlyByteBuf buf) {
      buf.writeVarInt(this.stateId);
      buf.writeFloat(this.sx);
      buf.writeFloat(this.sy);
      buf.writeFloat(this.sz);
      buf.writeFloat(this.yawDeg);
      buf.writeFloat(this.alpha);
      buf.writeInt(this.tint);
      buf.writeVarInt(this.growTicks);
      buf.writeBoolean(this.anchorBottom);
      buf.writeInt(this.anchorId);
      buf.writeBoolean(this.launched());
      if (this.launched()) {
         buf.writeFloat(this.velX);
         buf.writeFloat(this.velY);
         buf.writeFloat(this.velZ);
         buf.writeDouble(this.restY);
         buf.writeFloat(this.tumble);
      }

      buf.writeVarInt(this.delay);
      buf.writeFloat(this.heaveRise);
      buf.writeFloat(this.heaveTilt);
      buf.writeInt(this.glowRgb);
      buf.writeFloat(this.glowStrength);
      buf.writeVarInt(this.floatDropAt);
      if (this.floating()) {
         buf.writeVarInt(this.tickCount);
         buf.writeVarInt(this.flightTicks);
         buf.writeVarInt(this.floatRiseTicks);
         buf.writeFloat(this.floatRise);
         buf.writeFloat(this.floatBob);
         buf.writeFloat(this.floatPhase);
         buf.writeDouble(this.floatBaseY);
         buf.writeFloat(this.orbitSpeed);
         buf.writeDouble(this.orbitX);
         buf.writeDouble(this.orbitZ);
         buf.writeFloat(this.dropVelY);
      }
   }

   @Override
   protected void readExtraSpawnData(RegistryFriendlyByteBuf buf) {
      this.stateId = buf.readVarInt();
      this.sx = buf.readFloat();
      this.sy = buf.readFloat();
      this.sz = buf.readFloat();
      this.yawDeg = buf.readFloat();
      this.alpha = buf.readFloat();
      this.tint = buf.readInt();
      this.growTicks = buf.readVarInt();
      this.anchorBottom = buf.readBoolean();
      this.anchorId = buf.readInt();
      if (buf.readBoolean()) {
         this.velX = buf.readFloat();
         this.velY = buf.readFloat();
         this.velZ = buf.readFloat();
         this.restY = buf.readDouble();
         this.tumble = buf.readFloat();
      }

      this.delay = buf.readVarInt();
      this.heaveRise = buf.readFloat();
      this.heaveTilt = buf.readFloat();
      this.glowRgb = buf.readInt();
      this.glowStrength = buf.readFloat();
      this.floatDropAt = buf.readVarInt();
      if (this.floating()) {
         this.tickCount = buf.readVarInt();
         this.flightTicks = buf.readVarInt();
         this.floatRiseTicks = buf.readVarInt();
         this.floatRise = buf.readFloat();
         this.floatBob = buf.readFloat();
         this.floatPhase = buf.readFloat();
         this.floatBaseY = buf.readDouble();
         this.orbitSpeed = buf.readFloat();
         this.orbitX = buf.readDouble();
         this.orbitZ = buf.readDouble();
         this.dropVelY = buf.readFloat();
      }
   }
}
