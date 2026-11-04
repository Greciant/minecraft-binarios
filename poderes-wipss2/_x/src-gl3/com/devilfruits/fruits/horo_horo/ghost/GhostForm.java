package com.devilfruits.fruits.horo_horo.ghost;

import com.devilfruits.core.CoreEvents;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.horo_horo.HoroAbilities;
import com.devilfruits.fruits.horo_horo.HoroEntities;
import com.devilfruits.fruits.horo_horo.HoroFx;
import com.devilfruits.fruits.horo_horo.HoroHoro;
import com.devilfruits.fruits.horo_horo.HoroSounds;
import com.devilfruits.fruits.horo_horo.HoroTriggers;
import com.devilfruits.fruits.horo_horo.entity.GhostBody;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFTags;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.FakePlayer;

public final class GhostForm {
   public static final int OUT = FormFlags.fruit(0);
   public static final int POSSESSING = FormFlags.fruit(1);
   public static final int RECALLING = FormFlags.fruit(2);
   public static final int POSSESS_TICKS = 12;
   private static final double RECALL_SPEED = 0.55;
   private static final TicketType<ChunkPos> TICKET = TicketType.create("devilfruits_horo_body", Comparator.comparingLong(ChunkPos::toLong));
   private static final Codec<Vec3> VEC = Codec.DOUBLE
      .listOf(3, 3)
      .xmap(l -> new Vec3((Double)l.get(0), (Double)l.get(1), (Double)l.get(2)), v -> List.of(v.x, v.y, v.z));
   public static final Supplier<AttachmentType<GhostForm.Anchor>> ANCHOR = DFAttachments.ATTACHMENTS
      .register(
         "horo_horo/ghost",
         () -> AttachmentType.builder(() -> GhostForm.Anchor.NONE)
               .serialize(GhostForm.Anchor.CODEC)
               .sync((holder, to) -> holder == to, GhostForm.Anchor.STREAM_CODEC)
               .build()
      );
   private static final Map<UUID, GhostForm.Session> SESSIONS = new HashMap<>();
   private static boolean forwarding;

   public static void init() {
   }

   public static boolean active(Player player) {
      return player.level().isClientSide() ? FormFlags.has(player, HoroHoro.ID, OUT) : SESSIONS.containsKey(player.getUUID());
   }

   public static boolean forwarding() {
      return forwarding;
   }

   public static boolean ownsBody(ServerLevel level, GhostBody body) {
      UUID owner = body.ownerId();
      GhostForm.Session s = owner == null ? null : SESSIONS.get(owner);
      return s != null && s.body.equals(body.getUUID());
   }

   @Nullable
   public static Vec3 body(Player player) {
      GhostForm.Session s = SESSIONS.get(player.getUUID());
      return s == null ? null : s.bodyPos;
   }

   @Nullable
   public static GhostBody bodyEntity(ServerPlayer player) {
      GhostForm.Session s = SESSIONS.get(player.getUUID());
      return s != null && player.serverLevel().getEntity(s.body) instanceof GhostBody b ? b : null;
   }

   public static boolean recalling(Player player) {
      GhostForm.Session s = player.level().isClientSide() ? null : SESSIONS.get(player.getUUID());
      return s != null ? s.recalling : FormFlags.has(player, HoroHoro.ID, RECALLING);
   }

   public static boolean possessing(Player player) {
      GhostForm.Session s = player.level().isClientSide() ? null : SESSIONS.get(player.getUUID());
      return s != null ? s.possessAt >= 0L : FormFlags.has(player, HoroHoro.ID, POSSESSING);
   }

   public static boolean ticketed(ServerPlayer player) {
      GhostForm.Session s = SESSIONS.get(player.getUUID());
      return s != null && s.ticket != null;
   }

   public static boolean start(ServerPlayer player) {
      if (!SESSIONS.containsKey(player.getUUID()) && HoroHoro.isUser(player)) {
         ServerLevel level = player.serverLevel();
         GhostBody body = (GhostBody)HoroEntities.GHOST_BODY.get().create(level);
         if (body == null) {
            return false;
         } else {
            body.setup(player);
            body.moveTo(player.getX(), player.getY(), player.getZ(), player.yBodyRot, 0.0F);
            level.addFreshEntity(body);
            GhostForm.Session session = new GhostForm.Session(player, body.getUUID(), player.position(), player.yBodyRot, level.dimension());
            SESSIONS.put(player.getUUID(), session);
            if (FormManager.request(player, "ghost", FormManager.Reason.COMMAND) != FormManager.Result.SWITCHED) {
               SESSIONS.remove(player.getUUID());
               body.discard();
               return false;
            } else {
               player.setData(ANCHOR, new GhostForm.Anchor(true, session.bodyPos, session.yaw, level.dimension()));
               FormFlags.set(player, OUT, true);
               holdChunk(level, session);
               move(player, player.position().add(0.0, 0.4, 0.0));
               player.getAbilities().flying = true;
               player.onUpdateAbilities();
               ServerFx.send(player, HoroFx.GHOST_EXIT, session.bodyPos, Vec3.ZERO, 1.0F, level.getRandom().nextLong(), body.getId());
               level.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(), HoroSounds.GHOST_EXIT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
               HoroTriggers.GHOST_FORM.get().trigger(player);
               CoreEvents.syncNow(player);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   public static void beginHold(ServerPlayer player) {
      GhostForm.Session s = SESSIONS.get(player.getUUID());
      if (s != null && !s.recalling && s.possessAt < 0L && s.holdStart < 0L) {
         s.holdStart = player.level().getGameTime();
         ServerFx.chargeStart(player, HoroAbilities.GHOST_FORM, (float)(HoroHoro.setting("recallHoldSeconds") * 20.0), 0L);
      }
   }

   public static void endHold(ServerPlayer player) {
      GhostForm.Session s = SESSIONS.get(player.getUUID());
      if (s != null && s.holdStart >= 0L) {
         s.holdStart = -1L;
         ServerFx.chargeStop(player, HoroAbilities.GHOST_FORM, false);
      }
   }

   public static void beginRecall(ServerPlayer player) {
      GhostForm.Session s = SESSIONS.get(player.getUUID());
      if (s != null && !s.recalling && s.possessAt < 0L) {
         s.recalling = true;
         s.holdStart = -1L;
         FormFlags.set(player, RECALLING, true);
         CoreEvents.syncNow(player);
      }
   }

   public static boolean requestPossession(ServerPlayer player, GhostBody body) {
      GhostForm.Session s = SESSIONS.get(player.getUUID());
      if (s != null
         && s.body.equals(body.getUUID())
         && s.possessAt < 0L
         && !(player.position().distanceTo(body.position()) > HoroHoro.setting("possessReach") + 1.0)) {
         beginPossession(player, s);
         return true;
      } else {
         return false;
      }
   }

   private static void beginPossession(ServerPlayer player, GhostForm.Session s) {
      s.possessAt = player.level().getGameTime() + 12L;
      s.recalling = false;
      s.holdStart = -1L;
      FormFlags.set(player, POSSESSING, true);
      FormFlags.set(player, RECALLING, false);
      player.setDeltaMovement(Vec3.ZERO);
      player.hurtMarked = true;
      ServerFx.send(player, HoroFx.POSSESS, s.bodyPos, player.position(), 1.0F, 0L, 12);
      player.serverLevel().playSound(null, s.bodyPos.x, s.bodyPos.y + 1.0, s.bodyPos.z, HoroSounds.POSSESS.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
      CoreEvents.syncNow(player);
   }

   public static void tick(ServerPlayer player) {
      GhostForm.Session s = SESSIONS.get(player.getUUID());
      if (s == null) {
         if (((GhostForm.Anchor)player.getData(ANCHOR)).active()) {
            finish(player, true);
         }
      } else {
         ServerLevel level = player.serverLevel();
         long now = level.getGameTime();
         player.noPhysics = true;
         player.fallDistance = 0.0F;
         if (!player.getAbilities().flying) {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
         }

         Entity body = level.getEntity(s.body);
         if (body != null && !body.isRemoved() && HoroHoro.isUser(player) && HoroHoro.isGhost(player)) {
            s.bodyPos = body.position();
            holdChunk(level, s);
            if (bodySuppressed(level, body) || Weakness.isSuppressed(player)) {
               finish(player, true);
            } else if (s.possessAt >= 0L) {
               player.setDeltaMovement(Vec3.ZERO);
               if (now >= s.possessAt) {
                  finish(player, true);
                  ServerFx.animate(player, HoroHoro.id("possess"));
               }
            } else if (s.recalling) {
               Vec3 to = s.bodyPos.add(0.0, 0.6, 0.0).subtract(player.position());
               if (to.length() <= 1.15) {
                  beginPossession(player, s);
               } else {
                  move(player, player.position().add(to.normalize().scale(0.55)));
                  player.setDeltaMovement(Vec3.ZERO);
                  player.hurtMarked = true;
               }
            } else if (s.holdStart >= 0L && (double)(now - s.holdStart) >= HoroHoro.setting("recallHoldSeconds") * 20.0) {
               ServerFx.chargeStop(player, HoroAbilities.GHOST_FORM, true);
               beginRecall(player);
            } else {
               validate(player);
               leash(player, s);
            }
         } else {
            finish(player, true);
         }
      }
   }

   private static void leash(ServerPlayer player, GhostForm.Session s) {
      double leash = HoroHoro.setting("ghostLeash");
      Vec3 off = player.position().subtract(s.bodyPos);
      double d = off.length();
      if (!(d <= leash)) {
         Vec3 home = off.scale(-1.0 / d);
         double pull = Math.min(0.8, (d - leash) * 0.12);
         Vec3 v = player.getDeltaMovement();
         double outward = -v.dot(home);
         if (outward > 0.0) {
            v = v.add(home.scale(outward));
         }

         player.setDeltaMovement(v.add(home.scale(pull)));
         player.hurtMarked = true;
         if (d > leash + 8.0) {
            move(player, s.bodyPos.add(off.scale((leash + 8.0) / d)));
         }
      }
   }

   public static boolean validate(ServerPlayer player) {
      GhostForm.Session s = SESSIONS.get(player.getUUID());
      if (s == null) {
         return false;
      } else if (!touchesSeastone(player.level(), player.getBoundingBox().deflate(0.001))) {
         s.lastFree = player.position();
         return false;
      } else {
         move(player, s.lastFree);
         player.setDeltaMovement(Vec3.ZERO);
         player.hurtMarked = true;
         return true;
      }
   }

   public static boolean touchesSeastone(Level level, AABB box) {
      for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
         if (level.getBlockState(pos).is(DFTags.SEASTONE_BLOCKS)) {
            return true;
         }
      }

      return false;
   }

   private static boolean bodySuppressed(ServerLevel level, Entity body) {
      return touchesSeastone(level, body.getBoundingBox().inflate(0.05)) || level.getFluidState(BlockPos.containing(body.getEyePosition())).is(FluidTags.WATER);
   }

   private static void holdChunk(ServerLevel level, GhostForm.Session s) {
      ChunkPos at = new ChunkPos(BlockPos.containing(s.bodyPos));
      if (!at.equals(s.ticket)) {
         if (s.ticket != null) {
            level.getChunkSource().removeRegionTicket(TICKET, s.ticket, 2, s.ticket);
         }

         level.getChunkSource().addRegionTicket(TICKET, at, 2, at);
         s.ticket = at;
      }
   }

   public static void finish(ServerPlayer player, boolean toBody) {
      GhostForm.Session s = SESSIONS.remove(player.getUUID());
      GhostForm.Anchor anchor = (GhostForm.Anchor)player.getData(ANCHOR);
      player.setData(ANCHOR, GhostForm.Anchor.NONE);
      ServerLevel bodyLevel = player.server.getLevel(s != null ? s.dim : anchor.dimension());
      if (s != null && bodyLevel != null) {
         Entity body = bodyLevel.getEntity(s.body);
         if (body != null) {
            s.bodyPos = body.position();
            body.discard();
         }

         if (s.ticket != null) {
            bodyLevel.getChunkSource().removeRegionTicket(TICKET, s.ticket, 2, s.ticket);
         }

         if (s.holdStart >= 0L) {
            ServerFx.chargeStop(player, HoroAbilities.GHOST_FORM, false);
         }
      }

      player.noPhysics = false;
      FormFlags.set(player, OUT, false);
      FormFlags.set(player, POSSESSING, false);
      FormFlags.set(player, RECALLING, false);
      AbilityManager.stopToggle(player, HoroAbilities.GHOST_FORM);
      if (toBody) {
         Vec3 bodyx = s != null ? s.bodyPos : (anchor.active() ? anchor.body() : null);
         float yaw = s != null ? s.yaw : anchor.yaw();
         if (bodyx != null && bodyLevel != null) {
            if (player instanceof FakePlayer && bodyLevel == player.level()) {
               player.moveTo(bodyx.x, bodyx.y, bodyx.z, yaw, 0.0F);
            } else {
               player.teleportTo(bodyLevel, bodyx.x, bodyx.y, bodyx.z, yaw, 0.0F);
            }
         }
      }

      if (HoroHoro.isGhost(player)) {
         FormManager.revert(player, FormManager.Reason.COMMAND);
      }

      if (!player.isCreative() && !player.isSpectator()) {
         player.getAbilities().flying = false;
      }

      player.onUpdateAbilities();
      player.fallDistance = 0.0F;
      player.setDeltaMovement(Vec3.ZERO);
      player.hurtMarked = true;
      CoreEvents.syncNow(player);
   }

   public static float forward(GhostBody body, DamageSource source, float amount) {
      UUID ownerId = body.ownerId();
      if (ownerId != null && body.level() instanceof ServerLevel level && !(amount <= 0.0F)) {
         GhostForm.Session session = SESSIONS.get(ownerId);
         ServerPlayer owner = session != null ? session.player : level.getServer().getPlayerList().getPlayer(ownerId);
         if (owner == null || source.getEntity() == owner) {
            return 0.0F;
         } else {
            if (source.getEntity() instanceof ServerPlayer attacker && !Targeting.canHurt(attacker, owner)) {
               return 0.0F;
            }

            boolean lethal = amount >= owner.getHealth() + owner.getAbsorptionAmount();
            if (lethal) {
               finish(owner, true);
            }

            Vec3 v = owner.getDeltaMovement();
            forwarding = true;

            try {
               owner.invulnerableTime = 0;
               owner.hurt(source, amount);
            } finally {
               forwarding = false;
            }

            if (active(owner)) {
               owner.setDeltaMovement(v);
               owner.hurtMarked = true;
            }

            return amount;
         }
      } else {
         return 0.0F;
      }
   }

   private static void move(ServerPlayer player, Vec3 at) {
      if (player instanceof FakePlayer) {
         player.moveTo(at.x, at.y, at.z, player.getYRot(), player.getXRot());
      } else {
         player.teleportTo(at.x, at.y, at.z);
      }
   }

   public static void register(IEventBus bus) {
      bus.addListener(e -> {
         if (e.getEntity() instanceof ServerPlayer p) {
            tick(p);
         }
      });
      bus.addListener(e -> {
         if (e.getEntity() instanceof ServerPlayer p && SESSIONS.containsKey(p.getUUID())) {
            finish(p, true);
         }
      });
      bus.addListener(e -> {
         if (e.getEntity() instanceof ServerPlayer p && SESSIONS.containsKey(p.getUUID())) {
            finish(p, true);
         }
      });
      bus.addListener(e -> {
         if (!e.isCanceled() && e.getEntity() instanceof ServerPlayer p && SESSIONS.containsKey(p.getUUID())) {
            finish(p, false);
         }
      });
   }

   public static void onPowerRemoved(ServerPlayer player) {
      if (SESSIONS.containsKey(player.getUUID()) || ((GhostForm.Anchor)player.getData(ANCHOR)).active()) {
         finish(player, true);
      }
   }

   public static void rewindHold(ServerPlayer player, int ticks) {
      GhostForm.Session s = SESSIONS.get(player.getUUID());
      if (s != null && s.holdStart >= 0L) {
         s.holdStart -= (long)ticks;
      }
   }

   private GhostForm() {
   }

   public static record Anchor(boolean active, Vec3 body, float yaw, ResourceKey<Level> dimension) {
      public static final GhostForm.Anchor NONE = new GhostForm.Anchor(false, Vec3.ZERO, 0.0F, Level.OVERWORLD);
      public static final Codec<GhostForm.Anchor> CODEC = RecordCodecBuilder.create(
         i -> i.group(
                  Codec.BOOL.fieldOf("active").forGetter(GhostForm.Anchor::active),
                  GhostForm.VEC.fieldOf("body").forGetter(GhostForm.Anchor::body),
                  Codec.FLOAT.fieldOf("yaw").forGetter(GhostForm.Anchor::yaw),
                  ResourceKey.codec(Registries.DIMENSION).fieldOf("dim").forGetter(GhostForm.Anchor::dimension)
               )
               .apply(i, GhostForm.Anchor::new)
      );
      public static final StreamCodec<ByteBuf, GhostForm.Anchor> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.BOOL,
         GhostForm.Anchor::active,
         ByteBufCodecs.DOUBLE,
         a -> a.body().x,
         ByteBufCodecs.DOUBLE,
         a -> a.body().y,
         ByteBufCodecs.DOUBLE,
         a -> a.body().z,
         ByteBufCodecs.FLOAT,
         GhostForm.Anchor::yaw,
         ResourceKey.streamCodec(Registries.DIMENSION),
         GhostForm.Anchor::dimension,
         (active, x, y, z, yaw, dim) -> new GhostForm.Anchor(active, new Vec3(x, y, z), yaw, dim)
      );
   }

   private static final class Session {
      final ServerPlayer player;
      final UUID body;
      final float yaw;
      final ResourceKey<Level> dim;
      Vec3 bodyPos;
      Vec3 lastFree;
      long holdStart = -1L;
      boolean recalling;
      long possessAt = -1L;
      ChunkPos ticket;

      Session(ServerPlayer player, UUID body, Vec3 bodyPos, float yaw, ResourceKey<Level> dim) {
         this.player = player;
         this.dim = dim;
         this.body = body;
         this.bodyPos = bodyPos;
         this.yaw = yaw;
         this.lastFree = bodyPos;
      }
   }
}
