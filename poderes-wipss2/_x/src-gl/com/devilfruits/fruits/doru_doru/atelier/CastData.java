package com.devilfruits.fruits.doru_doru.atelier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import javax.annotation.Nullable;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CastData(int sx, int sy, int sz, byte[] packed) {
   public static final int MAX = 32;
   public static final Codec<CastData> CODEC = RecordCodecBuilder.create(
      i -> i.group(
               Codec.INT.fieldOf("sx").forGetter(CastData::sx),
               Codec.INT.fieldOf("sy").forGetter(CastData::sy),
               Codec.INT.fieldOf("sz").forGetter(CastData::sz),
               Codec.BYTE_BUFFER.fieldOf("data").forGetter(c -> ByteBuffer.wrap(c.packed))
            )
            .apply(i, (x, y, z, b) -> {
               byte[] a = new byte[b.remaining()];
               b.get(a);
               return new CastData(x, y, z, a);
            })
   );
   public static final StreamCodec<ByteBuf, CastData> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      CastData::sx,
      ByteBufCodecs.VAR_INT,
      CastData::sy,
      ByteBufCodecs.VAR_INT,
      CastData::sz,
      ByteBufCodecs.byteArray(1048576),
      CastData::packed,
      CastData::new
   );

   public static CastData pack(int sx, int sy, int sz, byte[] cells, long[] masks) {
      ByteBuffer raw = ByteBuffer.allocate(cells.length + masks.length * 8);
      raw.put(cells);

      for (long m : masks) {
         raw.putLong(m);
      }

      Deflater d = new Deflater(9);
      d.setInput(raw.array());
      d.finish();
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buf = new byte[8192];

      while (!d.finished()) {
         out.write(buf, 0, d.deflate(buf));
      }

      d.end();
      return new CastData(sx, sy, sz, out.toByteArray());
   }

   @Nullable
   public CastData.Unpacked unpack() {
      if (this.sx > 0 && this.sy > 0 && this.sz > 0 && this.sx <= 32 && this.sy <= 32 && this.sz <= 32) {
         Inflater inf = new Inflater();
         inf.setInput(this.packed);
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         byte[] buf = new byte[8192];

         try {
            while (!inf.finished()) {
               int n = inf.inflate(buf);
               if (n == 0 && (inf.needsInput() || inf.needsDictionary())) {
                  break;
               }

               out.write(buf, 0, n);
               if (out.size() > 294912) {
                  return null;
               }
            }
         } catch (DataFormatException var12) {
            return null;
         } finally {
            inf.end();
         }

         byte[] var14 = out.toByteArray();
         int var16 = this.sx * this.sy * this.sz;
         if (var14.length < var16) {
            return null;
         } else {
            byte[] cells = Arrays.copyOf(var14, var16);
            ByteBuffer rest = ByteBuffer.wrap(var14, var16, var14.length - var16);
            long[] masks = new long[rest.remaining() / 8];

            for (int i = 0; i < masks.length; i++) {
               masks[i] = rest.getLong();
            }

            return new CastData.Unpacked(this.sx, this.sy, this.sz, cells, masks);
         }
      } else {
         return null;
      }
   }

   public static record Unpacked(int sx, int sy, int sz, byte[] cells, long[] masks) {
      public long mask(int x, int y, int z, int[] maskIndex) {
         int v = this.cells[x + this.sx * (z + this.sz * y)] & 255;
         if (v == 0) {
            return 0L;
         } else {
            return v <= 16 ? -1L : this.masks[maskIndex[x + this.sx * (z + this.sz * y)]];
         }
      }

      public int color(int x, int y, int z) {
         int v = this.cells[x + this.sx * (z + this.sz * y)] & 255;
         return v == 0 ? -1 : (v <= 16 ? v - 1 : v - 17);
      }

      public int[] maskIndex() {
         int[] out = new int[this.cells.length];
         int n = 0;

         for (int i = 0; i < this.cells.length; i++) {
            if ((this.cells[i] & 255) > 16) {
               out[i] = n++;
            }
         }

         return out;
      }

      public int solidBlocks() {
         int n = 0;

         for (byte c : this.cells) {
            if (c != 0) {
               n++;
            }
         }

         return n;
      }
   }
}
