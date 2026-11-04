package com.devilfruits.fruits.suke_suke.clear;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class Clear {
   public static final int INVISIBLE = 1;
   public static final int HIDDEN_ITEMS = 2;
   public static final int SHIP = 4;
   public static Predicate<Entity> hiddenHere = e -> false;
   public static Entity drawnSolid;
   public static Predicate<Entity> itemsHiddenHere = e -> false;
   private static final UUID NOBODY = new UUID(0L, 0L);
   public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "devilfruits");
   public static final Supplier<AttachmentType<Clear.State>> STATE = ATTACHMENTS.register(
      "suke_suke/clear", () -> AttachmentType.builder(() -> Clear.State.NONE).sync((holder, to) -> true, Clear.State.STREAM_CODEC).build()
   );

   public static void init(IEventBus modBus) {
      ATTACHMENTS.register(modBus);
   }

   public static Clear.State of(Entity entity) {
      Clear.State s = (Clear.State)entity.getExistingDataOrNull(STATE);
      return s == null ? Clear.State.NONE : s;
   }

   public static boolean invisible(Entity entity) {
      Clear.State s = (Clear.State)entity.getExistingDataOrNull(STATE);
      return s != null && (s.flags() & 1) != 0;
   }

   public static boolean hiddenItems(Entity entity) {
      Clear.State s = (Clear.State)entity.getExistingDataOrNull(STATE);
      return s != null && (s.flags() & 2) != 0;
   }

   public static boolean sees(@Nullable Entity viewer, Entity target, boolean spectatorsSee) {
      Clear.State t = of(target);
      if (!t.invisible() || viewer == target) {
         return true;
      } else if (viewer == null) {
         return false;
      } else {
         if (spectatorsSee && viewer instanceof Player p && p.isSpectator()) {
            return true;
         }

         if (viewer.getUUID().equals(t.group())) {
            return true;
         } else {
            Clear.State v = of(viewer);
            return v.invisible() && v.group().equals(t.group());
         }
      }
   }

   public static boolean hiddenOnClient(Entity entity) {
      return entity.level().isClientSide() && invisible(entity) && hiddenHere.test(entity);
   }

   public static void vanish(Entity entity, UUID group, boolean ship) {
      Clear.State was = of(entity);
      int flags = 1 | was.flags() & 2 | (ship ? 4 : 0);
      entity.setData(STATE, new Clear.State(flags, group, was.invisible() ? was.since() : entity.level().getGameTime()));
   }

   public static void appear(Entity entity) {
      Clear.State was = of(entity);
      if (was.invisible()) {
         set(entity, new Clear.State(was.flags() & 2, was.group(), 0L));
      }
   }

   public static void hideItems(Entity entity, boolean on) {
      Clear.State was = of(entity);
      if (was.has(2) != on) {
         set(entity, was.with(2, on));
      }
   }

   private static void set(Entity entity, Clear.State s) {
      if (s.flags() == 0) {
         entity.removeData(STATE);
      } else {
         entity.setData(STATE, s);
      }
   }

   private Clear() {
   }

   public static record State(int flags, UUID group, long since) {
      public static final Clear.State NONE = new Clear.State(0, Clear.NOBODY, 0L);
      public static final StreamCodec<ByteBuf, Clear.State> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT, Clear.State::flags, UUIDUtil.STREAM_CODEC, Clear.State::group, ByteBufCodecs.VAR_LONG, Clear.State::since, Clear.State::new
      );

      public boolean has(int bit) {
         return (this.flags & bit) != 0;
      }

      public boolean invisible() {
         return this.has(1);
      }

      Clear.State with(int bit, boolean on) {
         return new Clear.State(on ? this.flags | bit : this.flags & ~bit, this.group, this.since);
      }
   }
}
