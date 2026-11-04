package com.awakenedmod.awakened.core.terrain;

import com.awakenedmod.awakened.Awakened;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

final class Lenient {
   private Lenient() {
   }

   static <T> Codec<Optional<T>> optional(Codec<T> codec, String what) {
      return Codec.PASSTHROUGH
         .xmap(
            dynamic -> codec.parse(dynamic).resultOrPartial(error -> Awakened.LOGGER.warn("Awakened: skipping an unreadable saved {}: {}", what, error)),
            value -> encode(codec, value.orElseThrow())
         );
   }

   static <T> Codec<List<T>> list(Codec<T> codec, String what) {
      return optional(codec, what).listOf().xmap(read -> {
         List<T> out = new ArrayList<>(read.size());
         read.forEach(value -> value.ifPresent(out::add));
         return out;
      }, list -> list.stream().map(Optional::of).toList());
   }

   private static <T> Dynamic<?> encode(Codec<T> codec, T value) {
      return new Dynamic(NbtOps.INSTANCE, (Tag)codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow());
   }
}
