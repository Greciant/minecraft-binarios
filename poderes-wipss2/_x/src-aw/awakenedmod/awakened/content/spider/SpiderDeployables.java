package com.awakenedmod.awakened.content.spider;

import com.awakenedmod.awakened.content.vfx.BlockFxEntity;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(
   modid = "awakened"
)
public final class SpiderDeployables {
   private static final int BLEED_TICKS = 80;
   private static final int BLEED_INTERVAL = 15;
   private static final float BLEED_PER_TICK = 1.5F;
   static final String BLEED_BALANCE_KEY = "SpiderWebBleed";
   private static final float RAZOR_HIT_DAMAGE = 6.0F;
   static final String RAZOR_BALANCE_KEY = "SpiderRazorMine";
   private static final double IMPACT_RADIUS = 3.5;
   private static final float IMPACT_DAMAGE = 7.0F;
   static final String IMPACT_BALANCE_KEY = "SpiderWebImpact";
   private static final int IMPACT_WEB_TICKS = 80;
   private static final int IMPACT_WEB_SLOW_AMP = 5;
   private static final int WALL_TICKS = 400;
   private static final double WALL_HALF_WIDTH = 2.5;
   private static final double WALL_HEIGHT = 3.5;
   private static final double WALL_THICKNESS = 1.0;
   private static final int MAX_WALLS_PER_PLAYER = 4;
   private static final int WALL_COLS = 5;
   private static final int WALL_ROWS = 4;
   private static final double WALL_PANE_THICK = 0.35;
   private static final Map<LivingEntity, SpiderDeployables.Bleed> BLEEDING = new ConcurrentHashMap<>();
   private static final List<SpiderDeployables.Wall> WALLS = new ArrayList<>();
   private static int lastTick = -1;

   private SpiderDeployables() {
   }

   public static void razor(ServerPlayer source, ServerLevel level, @Nullable LivingEntity hit, Vec3 pos) {
      SpiderFx.razorSlice(level, pos);
      if (hit != null && hit != source && hit.isAlive() && !hit.isRemoved() && Harm.allowed(source, hit)) {
         Dmg.hurt(hit, level, Dmg.by(source, DamageTypes.PLAYER_ATTACK), MasteryScaling.scaledDamage(6.0F, source, "SpiderRazorMine"));
         Harm.effect(source, hit, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 0, false, false));
         SpiderDeployables.Bleed b = BLEEDING.computeIfAbsent(hit, t -> new SpiderDeployables.Bleed());
         b.source = source.getUUID();
         b.expiry = level.getGameTime() + 80L;
      }
   }

   public static void impact(ServerPlayer source, ServerLevel level, @Nullable LivingEntity hit, Vec3 pos) {
      boolean hitFoe = hit != null && hit != source && hit.isAlive() && !hit.isRemoved();
      Vec3 center = hitFoe ? hit.getBoundingBox().getCenter() : pos;
      SpiderFx.impactBomb(level, center);
      AABB blast = new AABB(center, center).inflate(3.5);
      double r2 = 12.25;

      for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, blast)) {
         if (t != source && !t.getUUID().equals(source.getUUID()) && t.isAlive() && !t.isRemoved() && Harm.allowed(source, t)) {
            double d2 = t.getBoundingBox().getCenter().distanceToSqr(center);
            if (!(d2 > r2)) {
               double falloff = 1.0 - Math.sqrt(d2) / 3.5 * 0.5;
               float impactDamage = MasteryScaling.scaledDamage((float)(7.0 * falloff), source, "SpiderWebImpact");
               Dmg.hurt(t, level, Dmg.by(source, DamageTypes.PLAYER_ATTACK), impactDamage);
               t.setDeltaMovement(0.0, Math.min(0.0, t.getDeltaMovement().y), 0.0);
               t.hurtMarked = true;
               Harm.effect(source, t, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 5, false, false));
               Harm.effect(source, t, new MobEffectInstance(MobEffects.WEAKNESS, 80, 2, false, false));
            }
         }
      }
   }

   public static void wall(ServerPlayer source, ServerLevel level, @Nullable LivingEntity hit, Vec3 pos) {
      Vec3 look = source.getLookAngle();
      Vec3 normal = new Vec3(look.x, 0.0, look.z);
      normal = normal.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : normal.normalize();
      Vec3 right = new Vec3(-normal.z, 0.0, normal.x).normalize();
      Vec3 center = groundedCenter(level, pos, source);
      capPerPlayer(WALLS, source.getUUID());
      SpiderDeployables.Wall w = new SpiderDeployables.Wall();
      w.source = source.getUUID();
      w.level = level;
      w.center = center;
      w.right = right;
      w.normal = normal;
      w.expiry = level.getGameTime() + 400L;
      w.blocks = raiseWebWall(level, center, right, normal);
      WALLS.add(w);
      SpiderFx.wallWeave(level, center, right, 2.5, 3.5);
   }

   private static Vec3 groundedCenter(ServerLevel level, Vec3 pos, ServerPlayer source) {
      Vec3 from = pos.add(0.0, 0.1, 0.0);
      ClipContext ctx = new ClipContext(from, pos.subtract(0.0, 3.5, 0.0), Block.COLLIDER, Fluid.NONE, source);
      HitResult hr = level.clip(ctx);
      return hr.getType() == Type.BLOCK ? new Vec3(pos.x, hr.getLocation().y + 1.75, pos.z) : pos;
   }

   private static List<BlockFxEntity> raiseWebWall(ServerLevel level, Vec3 center, Vec3 right, Vec3 normal) {
      List<BlockFxEntity> blocks = new ArrayList<>();
      float yaw = (float)Math.toDegrees(Math.atan2(normal.x, normal.z));
      double colStep = 1.0;
      double rowStep = 0.875;
      double sx = colStep + 0.12;
      double sy = rowStep + 0.12;
      int life = 405;

      for (int c = 0; c < 5; c++) {
         double along = -2.5 + colStep * ((double)c + 0.5);

         for (int r = 0; r < 4; r++) {
            double up = -1.75 + rowStep * ((double)r + 0.5);
            Vec3 cell = center.add(right.scale(along)).add(0.0, up, 0.0);
            blocks.add(Vfx.blockTracked(level, cell, Blocks.COBWEB, sx, sy, 0.35, life, yaw));
         }
      }

      return blocks;
   }

   public static void tickAll(MinecraftServer server) {
      int now = server.getTickCount();
      if (now != lastTick) {
         lastTick = now;
         tickBleed(now);
         tickWalls();
      }
   }

   @SubscribeEvent
   static void onServerTick(Post e) {
      tickAll(e.getServer());
   }

   @SubscribeEvent
   static void onServerStopped(ServerStoppedEvent e) {
      for (SpiderDeployables.Wall w : WALLS) {
         discardBlocks(w);
      }

      BLEEDING.clear();
      WALLS.clear();
      lastTick = -1;
   }

   private static void tickBleed(int now) {
      Iterator<Entry<LivingEntity, SpiderDeployables.Bleed>> it = BLEEDING.entrySet().iterator();

      while (it.hasNext()) {
         Entry<LivingEntity, SpiderDeployables.Bleed> e = it.next();
         LivingEntity t = e.getKey();
         SpiderDeployables.Bleed b = e.getValue();
         if (t.isAlive() && !t.isRemoved() && t.level() instanceof ServerLevel level && level.getGameTime() < b.expiry) {
            Vec3 c = t.position().add(0.0, (double)t.getBbHeight() * 0.5, 0.0);
            Fx.burst(level, SpiderFx.BLOOD, c, 2, (double)t.getBbWidth() * 0.4 + 0.1, 0.02);
            if (now % 15 == 0) {
               float bleedDamage = 1.5F;
               ServerPlayer src = b.source == null ? null : level.getServer().getPlayerList().getPlayer(b.source);
               if (src != null) {
                  bleedDamage = MasteryScaling.scaledDamage(bleedDamage, src, "SpiderWebBleed");
               } else {
                  bleedDamage = MasteryScaling.configScaledDamage(bleedDamage, "SpiderWebBleed");
               }

               Dmg.hurt(t, level, bleedFrom(level, b.source), bleedDamage);
            }
         } else {
            it.remove();
         }
      }
   }

   private static void tickWalls() {
      Iterator<SpiderDeployables.Wall> it = WALLS.iterator();

      while (it.hasNext()) {
         SpiderDeployables.Wall w = it.next();
         ServerLevel level = w.level;
         if (level.getGameTime() >= w.expiry) {
            discardBlocks(w);
            it.remove();
         } else {
            ServerPlayer weaver = level.getServer().getPlayerList().getPlayer(w.source);

            for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, new AABB(w.center, w.center).inflate(3.0, 3.5, 3.0))) {
               if (!t.getUUID().equals(w.source) && t.isAlive() && Harm.allowed(weaver, t)) {
                  Vec3 rel = t.position().subtract(w.center);
                  double sd = rel.dot(w.normal);
                  double alongRight = Math.abs(rel.dot(w.right));
                  double dy = rel.y;
                  if (!(Math.abs(sd) > 1.0) && !(alongRight > 2.5) && !(dy < -2.25) && !(dy > 1.75)) {
                     Harm.effect(weaver, t, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8, 8, false, false));
                     Vec3 v = t.getDeltaMovement();
                     t.setDeltaMovement(0.0, v.y, 0.0);
                     double side = sd >= 0.0 ? 1.0 : -1.0;
                     Vec3 out = t.position().add(w.normal.scale(side * 1.15 - sd));
                     if (!(t instanceof Player)) {
                        t.setPos(out.x, out.y, out.z);
                     }

                     t.hurtMarked = true;
                     if (level.getServer().getTickCount() % 6 == 0) {
                        Fx.burst(level, SpiderFx.SILK, t.position().add(0.0, (double)t.getBbHeight() * 0.5, 0.0), 2, 0.3, 0.01);
                     }
                  }
               }
            }
         }
      }
   }

   private static void discardBlocks(SpiderDeployables.Wall w) {
      for (BlockFxEntity b : w.blocks) {
         b.discard();
      }

      w.blocks.clear();
   }

   public static void clear(ServerPlayer player) {
      UUID id = player.getUUID();
      BLEEDING.entrySet().removeIf(e -> id.equals(e.getValue().source));
      WALLS.removeIf(w -> {
         boolean mine = id.equals(w.source);
         if (mine) {
            discardBlocks(w);
         }

         return mine;
      });
   }

   private static DamageSource bleedFrom(ServerLevel level, UUID source) {
      ServerPlayer src = source == null ? null : level.getServer().getPlayerList().getPlayer(source);
      return src != null ? Dmg.by(src, DamageTypes.PLAYER_ATTACK) : level.damageSources().generic();
   }

   private static void capPerPlayer(List<SpiderDeployables.Wall> list, UUID owner) {
      int count = 0;

      for (SpiderDeployables.Wall w : list) {
         if (owner.equals(w.source)) {
            count++;
         }
      }

      while (count >= 4) {
         Iterator<SpiderDeployables.Wall> it = list.iterator();
         boolean removed = false;

         while (true) {
            if (it.hasNext()) {
               if (!owner.equals(it.next().source)) {
                  continue;
               }

               it.remove();
               count--;
               removed = true;
            }

            if (!removed) {
               return;
            }
            break;
         }
      }
   }

   private static final class Bleed {
      private UUID source;
      private long expiry;
   }

   private static final class Wall {
      private UUID source;
      private ServerLevel level;
      private Vec3 center = Vec3.ZERO;
      private Vec3 right = Vec3.ZERO;
      private Vec3 normal = Vec3.ZERO;
      private long expiry;
      private List<BlockFxEntity> blocks = new ArrayList<>();
   }
}
