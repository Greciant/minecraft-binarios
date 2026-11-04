package com.devilfruits.fruits.doru_doru.wax;

import com.devilfruits.fruits.doru_doru.DoruBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WaxBlockEntity extends BlockEntity {
   public static final int SOLID = 0;
   public static final int SOFT = 1;
   public static final int MELTING = 2;
   public static final int KIND_SCULPT = 0;
   public static final int KIND_WALL = 1;
   public static final int KIND_LANCE = 2;
   public static final int KIND_CANDLE = 3;
   public static final int KIND_CAST = 4;
   private long mask = -1L;
   private int color;
   private int melt = 0;
   private float heat;
   private int owner = -1;
   private long placed;
   private boolean cured = true;
   private int kind = 0;
   private long expires;

   public WaxBlockEntity(BlockPos pos, BlockState state) {
      super(DoruBlocks.WAX_BE.get(), pos, state);
   }

   public long mask() {
      return this.mask;
   }

   public int color() {
      return this.color;
   }

   public int melt() {
      return this.melt;
   }

   public float heat() {
      return this.heat;
   }

   public int owner() {
      return this.owner;
   }

   public long placed() {
      return this.placed;
   }

   public boolean cured() {
      return this.cured;
   }

   public int kind() {
      return this.kind;
   }

   public long expires() {
      return this.expires;
   }

   public void init(long mask, int color, int owner, int kind, long now, long expires) {
      this.mask = mask;
      this.color = color & 15;
      this.owner = owner;
      this.kind = kind;
      this.placed = now;
      this.expires = expires;
      this.cured = false;
      this.melt = 0;
      this.heat = 0.0F;
      this.setChanged();
   }

   public void setMask(long mask) {
      if (this.mask != mask) {
         this.mask = mask;
         this.setChanged();
      }
   }

   public void setColor(int color) {
      if (this.color != (color & 15)) {
         this.color = color & 15;
         this.setChanged();
      }
   }

   public void setMelt(int melt) {
      if (this.melt != melt) {
         this.melt = melt;
         this.setChanged();
      }
   }

   public void setHeat(float heat) {
      this.heat = heat;
      this.setChanged();
   }

   public void setCured(boolean cured) {
      if (this.cured != cured) {
         this.cured = cured;
         this.setChanged();
      }
   }

   public void setExpires(long expires) {
      this.expires = expires;
      this.setChanged();
   }

   public void setOwner(int owner) {
      this.owner = owner;
      this.setChanged();
   }

   public void onLoad() {
      super.onLoad();
      if (this.level instanceof ServerLevel server && (this.heat > 0.0F || this.expires != 0L && server.getGameTime() >= this.expires)) {
         WaxHeatService.schedule(server, this.worldPosition, 20);
      }
   }

   protected void saveAdditional(CompoundTag tag, Provider registries) {
      super.saveAdditional(tag, registries);
      this.writeLook(tag);
      tag.putFloat("heat", this.heat);
      tag.putInt("owner", this.owner);
      tag.putLong("placed", this.placed);
      tag.putByte("kind", (byte)this.kind);
      if (this.expires != 0L) {
         tag.putLong("expires", this.expires);
      }
   }

   protected void loadAdditional(CompoundTag tag, Provider registries) {
      super.loadAdditional(tag, registries);
      this.readLook(tag);
      this.heat = tag.getFloat("heat");
      this.owner = tag.contains("owner") ? tag.getInt("owner") : -1;
      this.placed = tag.getLong("placed");
      this.kind = tag.getByte("kind");
      this.expires = tag.getLong("expires");
   }

   private void writeLook(CompoundTag tag) {
      tag.putLong("m", this.mask);
      tag.putByte("c", (byte)this.color);
      tag.putByte("s", (byte)this.melt);
      tag.putBoolean("k", this.cured);
   }

   private void readLook(CompoundTag tag) {
      this.mask = tag.contains("m") ? tag.getLong("m") : -1L;
      this.color = tag.getByte("c") & 15;
      this.melt = tag.getByte("s");
      this.cured = !tag.contains("k") || tag.getBoolean("k");
   }

   public CompoundTag getUpdateTag(Provider registries) {
      CompoundTag tag = new CompoundTag();
      this.writeLook(tag);
      return tag;
   }

   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public void handleUpdateTag(CompoundTag tag, Provider registries) {
      this.readLook(tag);
      this.rerender();
   }

   public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet, Provider registries) {
      CompoundTag tag = packet.getTag();
      if (!tag.isEmpty()) {
         this.readLook(tag);
         this.rerender();
      }
   }

   private void rerender() {
      if (this.level != null && this.level.isClientSide) {
         BlockState state = this.getBlockState();
         this.level.sendBlockUpdated(this.worldPosition, state, state, 0);
      }
   }
}
