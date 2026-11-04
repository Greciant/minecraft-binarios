package com.awakenedmod.awakened.content.command;

import com.awakenedmod.awakened.Awakened;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record RuleSlot(
   Rule rule,
   boolean on,
   long armedAt,
   int ticksLeft,
   @Nullable UUID boundTarget,
   boolean boundDormant,
   float locked,
   float held,
   int heldBodies,
   int holdEndsIn
) {
   public static final RuleSlot EMPTY = new RuleSlot(Rule.EMPTY, false, 0L, -1, null, false, 0.0F, 0.0F, 0, 0);
   public static final MapCodec<RuleSlot> MAP_CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
               Rule.MAP_CODEC.forGetter(RuleSlot::rule),
               Codec.BOOL.optionalFieldOf("on", false).forGetter(RuleSlot::on),
               Codec.LONG.optionalFieldOf("armedAt", 0L).forGetter(RuleSlot::armedAt),
               Codec.INT.optionalFieldOf("ticksLeft", -1).forGetter(RuleSlot::ticksLeft),
               UUIDUtil.CODEC.optionalFieldOf("bound").forGetter(s -> Optional.ofNullable(s.boundTarget)),
               Codec.BOOL.optionalFieldOf("dormant", false).forGetter(RuleSlot::boundDormant),
               Codec.FLOAT.optionalFieldOf("locked", 0.0F).forGetter(RuleSlot::locked)
            )
            .apply(instance, RuleSlot::of)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, RuleSlot> STREAM_CODEC = StreamCodec.of(RuleSlot::encode, RuleSlot::decode);

   private static void encode(RegistryFriendlyByteBuf buf, RuleSlot slot) {
      Rule.STREAM_CODEC.encode(buf, slot.rule);
      buf.writeBoolean(slot.on);
      buf.writeVarLong(slot.armedAt);
      buf.writeInt(slot.ticksLeft);
      buf.writeBoolean(slot.boundTarget != null);
      if (slot.boundTarget != null) {
         buf.writeUUID(slot.boundTarget);
      }

      buf.writeBoolean(slot.boundDormant);
      buf.writeFloat(slot.locked);
      buf.writeFloat(slot.held);
      buf.writeVarInt(slot.heldBodies);
      buf.writeInt(slot.holdEndsIn);
   }

   private static RuleSlot decode(RegistryFriendlyByteBuf buf) {
      Rule rule = (Rule)Rule.STREAM_CODEC.decode(buf);
      boolean on = buf.readBoolean();
      long armedAt = buf.readVarLong();
      int ticksLeft = buf.readInt();
      UUID bound = buf.readBoolean() ? buf.readUUID() : null;
      boolean dormant = buf.readBoolean();
      float locked = buf.readFloat();
      float held = buf.readFloat();
      int heldBodies = buf.readVarInt();
      int holdEndsIn = buf.readInt();
      return new RuleSlot(rule, on, armedAt, ticksLeft, bound, dormant, locked, held, heldBodies, holdEndsIn);
   }

   private static RuleSlot of(Rule rule, boolean on, long armedAt, int ticksLeft, Optional<UUID> bound, boolean dormant, float locked) {
      if (!rule.isEmpty() && !rule.resolves()) {
         Awakened.LOGGER
            .warn(
               "Dropping saved Command rule with unknown words (target '{}', effect '{}', trigger '{}')",
               new Object[]{rule.targetId(), rule.effectId(), rule.triggerId()}
            );
         return EMPTY;
      } else {
         return new RuleSlot(rule, on, armedAt, ticksLeft, bound.orElse(null), dormant, locked, 0.0F, 0, 0);
      }
   }

   public static RuleSlot written(Rule rule, long gameTime) {
      return new RuleSlot(rule, false, gameTime, -1, null, false, 0.0F, 0.0F, 0, 0);
   }

   public boolean isEmpty() {
      return this.rule.isEmpty();
   }

   public int secondsLeft() {
      if (this.on && !this.rule.infinite()) {
         int ticks = this.ticksLeft > 0 ? this.ticksLeft : this.holdEndsIn;
         return ticks > 0 ? (ticks + 19) / 20 : -1;
      } else {
         return -1;
      }
   }

   public boolean bound() {
      return this.boundTarget != null;
   }

   public RuleSlot withRule(Rule newRule, long gameTime) {
      return written(newRule, gameTime);
   }

   public RuleSlot switchedOn(long gameTime, int ticks, float lockedPoints) {
      return new RuleSlot(this.rule, true, gameTime, ticks, null, false, lockedPoints, lockedPoints, 0, lockedPoints > 0.0F ? ticks : 0);
   }

   public RuleSlot switchedOff(long gameTime) {
      return new RuleSlot(this.rule, false, gameTime, -1, null, false, 0.0F, 0.0F, 0, 0);
   }

   public RuleSlot withTicksLeft(int ticks) {
      return new RuleSlot(
         this.rule, this.on, this.armedAt, ticks, this.boundTarget, this.boundDormant, this.locked, this.held, this.heldBodies, this.holdEndsIn
      );
   }

   public RuleSlot withBind(@Nullable UUID target, boolean dormant) {
      return new RuleSlot(this.rule, this.on, this.armedAt, this.ticksLeft, target, dormant, this.locked, this.held, this.heldBodies, this.holdEndsIn);
   }

   public RuleSlot withHold(float heldNow, int bodies, int endsIn) {
      return new RuleSlot(this.rule, this.on, this.armedAt, this.ticksLeft, this.boundTarget, this.boundDormant, this.locked, heldNow, bodies, endsIn);
   }
}
