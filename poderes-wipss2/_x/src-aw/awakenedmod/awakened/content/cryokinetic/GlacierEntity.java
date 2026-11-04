package com.awakenedmod.awakened.content.cryokinetic;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.vfx.AbstractFxEntity;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class GlacierEntity extends AbstractFxEntity {
   public static final Block[] KINDS = new Block[]{Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.ICE};
   public static final int MELT_TICKS = 12;
   private byte[] x = new byte[0];
   private byte[] y = new byte[0];
   private byte[] z = new byte[0];
   private byte[] kind = new byte[0];
   private byte[] delay = new byte[0];
   private boolean[] half = new boolean[0];
   private float[][] shard = new float[0][];
   private byte[] shardDelay = new byte[0];
   private boolean[] shardClear = new boolean[0];
   private int[] visible = new int[0];

   public GlacierEntity(EntityType<? extends GlacierEntity> type, Level level) {
      super(type, level);
   }

   static void spawn(ServerLevel level, GlacierShape shape, int lifetime) {
      GlacierEntity e = new GlacierEntity(AwakenedEntities.GLACIER.get(), level);
      List<GlacierShape.Voxel> v = shape.voxels;
      int n = v.size();
      e.x = new byte[n];
      e.y = new byte[n];
      e.z = new byte[n];
      e.kind = new byte[n];
      e.delay = new byte[n];
      e.half = new boolean[n];

      for (int i = 0; i < n; i++) {
         GlacierShape.Voxel p = v.get(i);
         e.x[i] = (byte)p.x();
         e.y[i] = (byte)p.y();
         e.z[i] = (byte)p.z();
         e.kind[i] = (byte)p.kind();
         e.delay[i] = (byte)Math.min(127, p.delay());
         e.half[i] = p.half();
      }

      List<GlacierShape.Shard> sh = shape.shards;
      e.shard = new float[sh.size()][];
      e.shardDelay = new byte[sh.size()];
      e.shardClear = new boolean[sh.size()];

      for (int i = 0; i < sh.size(); i++) {
         GlacierShape.Shard p = sh.get(i);
         e.shard[i] = new float[]{p.x(), p.y(), p.z(), p.dx(), p.dy(), p.dz(), p.length(), p.radius(), p.roll()};
         e.shardDelay[i] = (byte)Math.min(127, p.delay());
         e.shardClear[i] = p.clear();
      }

      e.lifetime = lifetime;
      BlockPos o = shape.origin;
      e.setPos((double)o.getX(), (double)o.getY(), (double)o.getZ());
      level.addFreshEntity(e);
   }

   public int size() {
      return this.x.length;
   }

   public int[] visible() {
      return this.visible;
   }

   public int bx(int i) {
      return this.x[i];
   }

   public int by(int i) {
      return this.y[i];
   }

   public int bz(int i) {
      return this.z[i];
   }

   public Block block(int i) {
      return KINDS[this.kind[i]];
   }

   public int delay(int i) {
      return this.delay[i];
   }

   public boolean half(int i) {
      return this.half[i];
   }

   public int shardCount() {
      return this.shard.length;
   }

   public float[] shard(int i) {
      return this.shard[i];
   }

   public int shardDelay(int i) {
      return this.shardDelay[i];
   }

   public boolean shardClear(int i) {
      return this.shardClear[i];
   }

   @Override
   protected void writeExtraSpawnData(RegistryFriendlyByteBuf buf) {
      buf.writeVarInt(this.x.length);
      buf.writeBytes(this.x);
      buf.writeBytes(this.y);
      buf.writeBytes(this.z);
      buf.writeBytes(this.kind);
      buf.writeBytes(this.delay);

      for (boolean h : this.half) {
         buf.writeBoolean(h);
      }

      buf.writeVarInt(this.shard.length);

      for (int i = 0; i < this.shard.length; i++) {
         for (float v : this.shard[i]) {
            buf.writeFloat(v);
         }

         buf.writeByte(this.shardDelay[i]);
         buf.writeBoolean(this.shardClear[i]);
      }
   }

   @Override
   protected void readExtraSpawnData(RegistryFriendlyByteBuf buf) {
      int n = buf.readVarInt();
      this.x = new byte[n];
      this.y = new byte[n];
      this.z = new byte[n];
      this.kind = new byte[n];
      this.delay = new byte[n];
      this.half = new boolean[n];
      buf.readBytes(this.x);
      buf.readBytes(this.y);
      buf.readBytes(this.z);
      buf.readBytes(this.kind);
      buf.readBytes(this.delay);

      for (int i = 0; i < n; i++) {
         this.half[i] = buf.readBoolean();
      }

      int m = buf.readVarInt();
      this.shard = new float[m][];
      this.shardDelay = new byte[m];
      this.shardClear = new boolean[m];

      for (int i = 0; i < m; i++) {
         this.shard[i] = new float[9];

         for (int k = 0; k < 9; k++) {
            this.shard[i][k] = buf.readFloat();
         }

         this.shardDelay[i] = buf.readByte();
         this.shardClear[i] = buf.readBoolean();
      }

      this.visible = this.cullHidden();
   }

   private int[] cullHidden() {
      Set<Long> solid = new HashSet<>();

      for (int i = 0; i < this.x.length; i++) {
         if (!this.half[i] && this.kind[i] != 2) {
            solid.add(pack(this.x[i], this.y[i], this.z[i]));
         }
      }

      int[] out = new int[this.x.length];
      int n = 0;

      for (int ix = 0; ix < this.x.length; ix++) {
         boolean buried = solid.contains(pack(this.x[ix] + 1, this.y[ix], this.z[ix]))
            && solid.contains(pack(this.x[ix] - 1, this.y[ix], this.z[ix]))
            && solid.contains(pack(this.x[ix], this.y[ix] + 1, this.z[ix]))
            && solid.contains(pack(this.x[ix], this.y[ix] - 1, this.z[ix]))
            && solid.contains(pack(this.x[ix], this.y[ix], this.z[ix] + 1))
            && solid.contains(pack(this.x[ix], this.y[ix], this.z[ix] - 1));
         if (!buried) {
            out[n++] = ix;
         }
      }

      return Arrays.copyOf(out, n);
   }

   private static long pack(int x, int y, int z) {
      return (long)(x + 256) << 20 | (long)(y + 256) << 10 | (long)(z + 256);
   }
}
