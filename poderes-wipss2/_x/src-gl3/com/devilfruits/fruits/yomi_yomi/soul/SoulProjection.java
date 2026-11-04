package com.devilfruits.fruits.yomi_yomi.soul;

import com.devilfruits.core.CoreEvents;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.yomi_yomi.Vessel;
import com.devilfruits.fruits.yomi_yomi.YomiAbilities;
import com.devilfruits.fruits.yomi_yomi.YomiEntities;
import com.devilfruits.fruits.yomi_yomi.YomiFx;
import com.devilfruits.fruits.yomi_yomi.YomiSounds;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.fruits.yomi_yomi.entity.SoulShell;
import com.devilfruits.registry.DFAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.FakePlayer;

public final class SoulProjection {
   public static final int PROJECTING = FormFlags.fruit(2);
   public static final int RECOILING = FormFlags.fruit(3);
   public static final int RECOIL_SNAP = 9;
   private static final int INSIDE_TICKS = 40;
   private static final Codec<Vec3> VEC = Codec.DOUBLE
      .listOf(3, 3)
      .xmap(l -> new Vec3((Double)l.get(0), (Double)l.get(1), (Double)l.get(2)), v -> List.of(v.x, v.y, v.z));
   public static final Supplier<AttachmentType<SoulProjection.Anchor>> ANCHOR = DFAttachments.ATTACHMENTS
      .register(
         "yomi_yomi/projection",
         () -> AttachmentType.builder(() -> SoulProjection.Anchor.NONE)
               .serialize(SoulProjection.Anchor.CODEC)
               .sync((holder, to) -> holder == to, SoulProjection.Anchor.STREAM_CODEC)
               .build()
      );
   private static final Map<UUID, SoulProjection.Session> SESSIONS = new HashMap<>();
   private static boolean forwarding;

   public static void init() {
   }

   public static boolean active(Player player) {
      return player.level().isClientSide() ? FormFlags.has(player, YomiYomi.ID, PROJECTING) : SESSIONS.containsKey(player.getUUID());
   }

   public static boolean recoiling(Player player) {
      SoulProjection.Session s = player.level().isClientSide() ? null : SESSIONS.get(player.getUUID());
      return s != null ? s.recoilAt >= 0L : FormFlags.has(player, YomiYomi.ID, RECOILING);
   }

   public static boolean forwarding() {
      return forwarding;
   }

   public static boolean ownsShell(ServerLevel level, SoulShell shell) {
      UUID owner = shell.ownerId();
      SoulProjection.Session s = owner == null ? null : SESSIONS.get(owner);
      return s != null && s.shell.equals(shell.getUUID());
   }

   @Nullable
   public static Vec3 body(Player player) {
      SoulProjection.Session s = SESSIONS.get(player.getUUID());
      return s == null ? null : s.body;
   }

   public static int age(ServerPlayer player) {
      SoulProjection.Session s = SESSIONS.get(player.getUUID());
      return s == null ? -1 : (int)(player.level().getGameTime() - s.started);
   }

   public static boolean start(ServerPlayer player) {
      Vessel vessel = YomiYomi.vessel(player);
      if (!SESSIONS.containsKey(player.getUUID()) && vessel != null && !SoulPhase.deathPhase(player)) {
         ServerLevel level = player.serverLevel();
         SoulShell shell = (SoulShell)YomiEntities.SOUL_SHELL.get().create(level);
         if (shell == null) {
            return false;
         } else {
            shell.setup(player, vessel);
            shell.moveTo(player.getX(), player.getY(), player.getZ(), player.yBodyRot, 0.0F);
            level.addFreshEntity(shell);
            SoulProjection.Session session = new SoulProjection.Session(player, shell.getUUID(), player.position(), player.yBodyRot, level.getGameTime());
            SESSIONS.put(player.getUUID(), session);
            if (FormManager.request(player, "soul", FormManager.Reason.COMMAND) != FormManager.Result.SWITCHED) {
               SESSIONS.remove(player.getUUID());
               shell.discard();
               return false;
            } else {
               player.setData(ANCHOR, new SoulProjection.Anchor(true, session.body, session.yaw, level.dimension()));
               FormFlags.set(player, PROJECTING, true);
               if (player instanceof FakePlayer) {
                  player.moveTo(player.getX(), player.getY() + 0.4, player.getZ(), player.getYRot(), player.getXRot());
               } else {
                  player.teleportTo(player.getX(), player.getY() + 0.4, player.getZ());
               }

               player.getAbilities().flying = true;
               player.onUpdateAbilities();
               ServerFx.send(player, YomiFx.PROJECT, session.body, Vec3.ZERO, 0.0F, level.getRandom().nextLong(), shell.getId());
               level.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(), YomiSounds.SOUL_DEPART.get(), SoundSource.PLAYERS, 0.7F, 1.25F);
               CoreEvents.syncNow(player);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   public static void beginRecoil(ServerPlayer player) {
      SoulProjection.Session s = SESSIONS.get(player.getUUID());
      if (s != null && s.recoilAt < 0L) {
         s.recoilAt = player.level().getGameTime();
         FormFlags.set(player, RECOILING, true);
         player.setDeltaMovement(Vec3.ZERO);
         player.hurtMarked = true;
         ServerFx.send(player, YomiFx.SOUL_RECOIL, player.position(), s.body, 0.0F, 0L, 9);
      }
   }

   public static void tick(ServerPlayer player) {
      SoulProjection.Session s = SESSIONS.get(player.getUUID());
      if (s == null) {
         if (((SoulProjection.Anchor)player.getData(ANCHOR)).active()) {
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

         Entity shell = level.getEntity(s.shell);
         if (shell == null || shell.isRemoved() || !YomiYomi.isUser(player)) {
            snap(player);
         } else if (s.recoilAt >= 0L) {
            player.setDeltaMovement(Vec3.ZERO);
            if (now - s.recoilAt >= 9L) {
               snap(player);
            }
         } else {
            validate(player);
            leash(player, s);
         }
      }
   }

   private static void leash(ServerPlayer player, SoulProjection.Session s) {
      double leash = YomiYomi.setting("soulLeash");
      Vec3 off = player.position().subtract(s.body);
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
            move(player, s.body.add(off.scale((leash + 8.0) / d)));
         }
      }
   }

   public static boolean validate(ServerPlayer player) {
      SoulProjection.Session s = SESSIONS.get(player.getUUID());
      if (s == null) {
         return false;
      } else {
         AABB box = player.getBoundingBox().deflate(0.001);
         boolean inside = !player.level().noCollision(player, box);
         if (!inside) {
            s.lastFree = player.position();
            s.entered = null;
            s.inside = 0;
            return false;
         } else {
            if (s.entered == null) {
               s.entered = s.lastFree;
            }

            s.inside++;
            Vec3 way = player.position().subtract(s.entered);
            double len = way.length();
            Vec3 dir = len < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : way.scale(1.0 / len);
            double extent = Math.abs(dir.x) * (double)player.getBbWidth()
               + Math.abs(dir.y) * (double)player.getBbHeight()
               + Math.abs(dir.z) * (double)player.getBbWidth();
            if (!(len > 1.0 + extent + 0.35) && s.inside <= 40) {
               return false;
            } else {
               move(player, s.lastFree);
               player.setDeltaMovement(Vec3.ZERO);
               player.hurtMarked = true;
               s.entered = null;
               s.inside = 0;
               return true;
            }
         }
      }
   }

   public static void snap(ServerPlayer player) {
      SoulProjection.Session s = SESSIONS.get(player.getUUID());
      if (s != null) {
         ServerFx.send(player, YomiFx.SOUL_SNAP, s.body, player.position(), 0.0F, 0L, 0);
         player.serverLevel().playSound(null, s.body.x, s.body.y + 1.0, s.body.z, YomiSounds.POSSESS.get(), SoundSource.PLAYERS, 0.6F, 1.4F);
         finish(player, true);
         ServerFx.animate(player, YomiYomi.id("soul_return"));
      }
   }

   public static void finish(ServerPlayer player, boolean toBody) {
      SoulProjection.Session s = SESSIONS.remove(player.getUUID());
      SoulProjection.Anchor anchor = (SoulProjection.Anchor)player.getData(ANCHOR);
      player.setData(ANCHOR, SoulProjection.Anchor.NONE);
      if (s != null) {
         Entity shell = player.serverLevel().getEntity(s.shell);
         if (shell != null) {
            shell.discard();
         }
      }

      player.noPhysics = false;
      FormFlags.set(player, PROJECTING, false);
      FormFlags.set(player, RECOILING, false);
      AbilityManager.stopToggle(player, YomiAbilities.SOUL_PROJECTION);
      if (toBody) {
         Vec3 body = s != null ? s.body : (anchor.active() ? anchor.body() : null);
         float yaw = s != null ? s.yaw : anchor.yaw();
         ServerLevel level = s != null ? player.serverLevel() : player.server.getLevel(anchor.dimension());
         if (body != null && level != null) {
            if (player instanceof FakePlayer && level == player.level()) {
               player.moveTo(body.x, body.y, body.z, yaw, 0.0F);
            } else {
               player.teleportTo(level, body.x, body.y, body.z, yaw, 0.0F);
            }
         }
      }

      if (YomiYomi.isSoul(player) && !SoulPhase.deathPhase(player)) {
         FormManager.revert(player, FormManager.Reason.COMMAND);
      }

      player.getAbilities().flying = false;
      player.onUpdateAbilities();
      player.fallDistance = 0.0F;
      player.setDeltaMovement(Vec3.ZERO);
      player.hurtMarked = true;
      CoreEvents.syncNow(player);
   }

   public static float forward(SoulShell shell, DamageSource source, float amount) {
      UUID ownerId = shell.ownerId();
      if (ownerId != null && shell.level() instanceof ServerLevel level) {
         SoulProjection.Session session = SESSIONS.get(ownerId);
         ServerPlayer owner = session != null ? session.player : level.getServer().getPlayerList().getPlayer(ownerId);
         if (owner == null || source.getEntity() == owner) {
            return 0.0F;
         } else {
            if (source.getEntity() instanceof ServerPlayer attacker && !Targeting.canHurt(attacker, owner)) {
               return 0.0F;
            }

            float dealt = amount * (float)(1.0 - YomiYomi.setting("shellResistance"));
            if (dealt <= 0.0F) {
               return 0.0F;
            } else {
               if (dealt >= owner.getHealth() + owner.getAbsorptionAmount()) {
                  snap(owner);
               }

               Vec3 v = owner.getDeltaMovement();
               forwarding = true;

               try {
                  owner.invulnerableTime = 0;
                  owner.hurt(source, dealt);
               } finally {
                  forwarding = false;
               }

               if (active(owner)) {
                  owner.setDeltaMovement(v);
                  owner.hurtMarked = true;
               }

               return dealt;
            }
         }
      } else {
         return 0.0F;
      }
   }

   public static void rewind(ServerPlayer player, int ticks) {
      SoulProjection.Session s = SESSIONS.get(player.getUUID());
      if (s != null) {
         s.started -= (long)ticks;
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
            finish(p, false);
         }
      });
      bus.addListener(e -> {
         if (!e.isCanceled() && e.getEntity() instanceof ServerPlayer p && SESSIONS.containsKey(p.getUUID())) {
            finish(p, false);
         }
      });
   }

   public static void onPowerRemoved(ServerPlayer player) {
      if (SESSIONS.containsKey(player.getUUID()) || ((SoulProjection.Anchor)player.getData(ANCHOR)).active()) {
         finish(player, true);
      }
   }

   private SoulProjection() {
   }

   public static record Anchor(boolean active, Vec3 body, float yaw, ResourceKey<Level> dimension) {
      public static final SoulProjection.Anchor NONE = new SoulProjection.Anchor(false, Vec3.ZERO, 0.0F, Level.OVERWORLD);
      public static final Codec<SoulProjection.Anchor> CODEC = RecordCodecBuilder.create(
         i -> i.group(
                  Codec.BOOL.fieldOf("active").forGetter(SoulProjection.Anchor::active),
                  SoulProjection.VEC.fieldOf("body").forGetter(SoulProjection.Anchor::body),
                  Codec.FLOAT.fieldOf("yaw").forGetter(SoulProjection.Anchor::yaw),
                  ResourceKey.codec(Registries.DIMENSION).fieldOf("dim").forGetter(SoulProjection.Anchor::dimension)
               )
               .apply(i, SoulProjection.Anchor::new)
      );
      public static final StreamCodec<ByteBuf, SoulProjection.Anchor> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.BOOL,
         SoulProjection.Anchor::active,
         ByteBufCodecs.DOUBLE,
         a -> a.body().x,
         ByteBufCodecs.DOUBLE,
         a -> a.body().y,
         ByteBufCodecs.DOUBLE,
         a -> a.body().z,
         ByteBufCodecs.FLOAT,
         SoulProjection.Anchor::yaw,
         ResourceKey.streamCodec(Registries.DIMENSION),
         SoulProjection.Anchor::dimension,
         (active, x, y, z, yaw, dim) -> new SoulProjection.Anchor(active, new Vec3(x, y, z), yaw, dim)
      );
   }

   private static final class Session {
      final ServerPlayer player;
      final UUID shell;
      final Vec3 body;
      final float yaw;
      long started;
      long recoilAt = -1L;
      Vec3 lastFree;
      Vec3 entered;
      int inside;

      Session(ServerPlayer player, UUID shell, Vec3 body, float yaw, long started) {
         this.player = player;
         this.shell = shell;
         this.body = body;
         this.yaw = yaw;
         this.started = started;
         this.lastFree = body;
      }
   }
}
