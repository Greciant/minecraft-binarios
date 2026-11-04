package com.awakenedmod.awakened.core.terrain;

import com.awakenedmod.awakened.Awakened;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

final class TempTerrainData extends SavedData {
   private static final String DATA_ID = "awakened_temp_terrain";
   static final Codec<TempTerrainData> CODEC = RecordCodecBuilder.create(
      i -> i.group(
               Lenient.list(TempTerrainData.Job.CODEC, "temporary terrain index entry")
                  .optionalFieldOf("jobs", List.of())
                  .forGetter(d -> new ArrayList(d.jobs.values()))
            )
            .apply(i, TempTerrainData::new)
   );
   private final Long2ObjectLinkedOpenHashMap<TempTerrainData.Job> jobs = new Long2ObjectLinkedOpenHashMap();
   private static final Factory<TempTerrainData> FACTORY = new Factory(TempTerrainData::new, TempTerrainData::load, null);

   TempTerrainData() {
   }

   private TempTerrainData(List<TempTerrainData.Job> read) {
      read.forEach(job -> this.jobs.put(job.id, job));
   }

   private static TempTerrainData load(CompoundTag tag, Provider provider) {
      return CODEC.parse(NbtOps.INSTANCE, tag.get("terrain")).resultOrPartial(Awakened.LOGGER::error).orElseGet(TempTerrainData::new);
   }

   public CompoundTag save(CompoundTag tag, Provider provider) {
      CODEC.encodeStart(NbtOps.INSTANCE, this).resultOrPartial(Awakened.LOGGER::error).ifPresent(encoded -> tag.put("terrain", encoded));
      return tag;
   }

   static TempTerrainData of(ServerLevel level) {
      return (TempTerrainData)level.getDataStorage().computeIfAbsent(FACTORY, "awakened_temp_terrain");
   }

   @Nullable
   static TempTerrainData peek(ServerLevel level) {
      return (TempTerrainData)level.getDataStorage().get(FACTORY, "awakened_temp_terrain");
   }

   Collection<TempTerrainData.Job> jobs() {
      return this.jobs.values();
   }

   @Nullable
   TempTerrainData.Job job(long id) {
      return (TempTerrainData.Job)this.jobs.get(id);
   }

   TempTerrainData.Job jobFor(JobClock clock) {
      TempTerrainData.Job job = (TempTerrainData.Job)this.jobs.get(clock.id);
      if (job == null) {
         job = new TempTerrainData.Job(clock.id, clock.ticketChunk, clock.ticketRadius, LongStream.empty());
         this.jobs.put(clock.id, job);
         this.setDirty();
      }

      return job;
   }

   void addChunk(TempTerrainData.Job job, long chunk) {
      if (job.chunks.add(chunk)) {
         this.setDirty();
      }
   }

   void finish(TempTerrainData.Job job) {
      this.jobs.remove(job.id);
      this.setDirty();
   }

   static final class Job {
      static final Codec<TempTerrainData.Job> CODEC = RecordCodecBuilder.create(
         i -> i.group(
                  Codec.LONG.fieldOf("id").forGetter(j -> j.id),
                  Codec.LONG.fieldOf("ticket_chunk").forGetter(j -> j.ticketChunk),
                  Codec.INT.fieldOf("ticket_radius").forGetter(j -> j.ticketRadius),
                  Codec.LONG_STREAM.fieldOf("chunks").forGetter(j -> LongStream.of(j.chunks.toLongArray()))
               )
               .apply(i, TempTerrainData.Job::new)
      );
      final long id;
      final long ticketChunk;
      final int ticketRadius;
      final LongOpenHashSet chunks = new LongOpenHashSet();

      private Job(long id, long ticketChunk, int ticketRadius, LongStream chunks) {
         this.id = id;
         this.ticketChunk = ticketChunk;
         this.ticketRadius = ticketRadius;
         chunks.forEach(this.chunks::add);
      }
   }
}
