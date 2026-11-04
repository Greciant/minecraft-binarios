package com.awakenedmod.awakened.content.vfx;

import com.awakenedmod.awakened.content.AwakenedEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class Vfx {
   private static final float GHOST_SCALE = 0.95F;
   private static final int NO_TINT = 16777215;
   private static final float SHELL_SCALE = 1.05F;
   private static final float SHELL_ALPHA = 0.3F;
   private static final int SHELL_TINT = 16722474;
   private static final Vec3 FORWARD = new Vec3(0.0, 0.0, 1.0);

   private Vfx() {
   }

   public static void sphere(ServerLevel level, Vec3 pos, double radius, int color, int lifetime) {
      sphere(level, pos, radius, color, lifetime, 1.0F);
   }

   public static void sphere(ServerLevel level, Vec3 pos, double radius, int color, int lifetime, float intensity) {
      effect(VfxEntity.Kind.SPHERE, pos).scale(radius).color(color).lifetime(lifetime).intensity(intensity).spawn(level);
   }

   public static void sphere(ServerLevel level, Vec3 pos, double radius, int color, int lifetime, float intensity, boolean noDepth, int delay) {
      effect(VfxEntity.Kind.SPHERE, pos).scale(radius).color(color).lifetime(lifetime).intensity(intensity).noDepth(noDepth).delay(delay).spawn(level);
   }

   public static void ring(ServerLevel level, Vec3 pos, double radius, int color, int lifetime) {
      effect(VfxEntity.Kind.RING, pos).scale(radius).color(color).lifetime(lifetime).spawn(level);
   }

   public static void ring(ServerLevel level, Vec3 pos, double radius, int color, int lifetime, float yaw, float pitch) {
      effect(VfxEntity.Kind.RING, pos).scale(radius).color(color).lifetime(lifetime).orient(yaw, pitch).spawn(level);
   }

   public static void dome(ServerLevel level, Vec3 pos, double radius, int color, int lifetime) {
      effect(VfxEntity.Kind.DOME, pos).scale(radius).color(color).lifetime(lifetime).spawn(level);
   }

   public static void pillar(ServerLevel level, Vec3 base, double radius, double height, int color, int lifetime) {
      effect(VfxEntity.Kind.PILLAR, base).scale(radius).secondary(new Vec3(0.0, height, 0.0)).color(color).lifetime(lifetime).spawn(level);
   }

   public static void beam(ServerLevel level, Vec3 from, Vec3 to, double width, int color, int lifetime) {
      effect(VfxEntity.Kind.BEAM, from).scale(width).secondary(to.subtract(from)).color(color).lifetime(lifetime).spawn(level);
   }

   public static void arc(ServerLevel level, Vec3 from, Vec3 to, double width, int color, int lifetime) {
      effect(VfxEntity.Kind.ARC, from).scale(width).secondary(to.subtract(from)).color(color).lifetime(lifetime).spawn(level);
   }

   public static void crescent(ServerLevel level, Vec3 pos, double radius, int color, int lifetime, float yaw, float pitch) {
      effect(VfxEntity.Kind.CRESCENT, pos).scale(radius).color(color).lifetime(lifetime).orient(yaw, pitch).spawn(level);
   }

   public static void splat(ServerLevel level, Vec3 pos, double radius, int color, int lifetime, float yaw, float pitch) {
      effect(VfxEntity.Kind.SPLAT, pos).scale(radius).color(color).lifetime(lifetime).orient(yaw, pitch).spawn(level);
   }

   public static void scorch(ServerLevel level, Vec3 pos, double radius, int color, int lifetime) {
      effect(VfxEntity.Kind.SCORCH, pos).scale(radius).color(color).lifetime(lifetime).spawn(level);
   }

   public static void pit(ServerLevel level, Vec3 pos, double radius, int color, int lifetime) {
      effect(VfxEntity.Kind.PIT, pos).scale(radius).color(color).lifetime(lifetime).spawn(level);
   }

   public static void portal(ServerLevel level, Vec3 center, double radius, int color, int lifetime, float yawDeg) {
      effect(VfxEntity.Kind.PORTAL, center).scale(radius).color(color).lifetime(lifetime).orient(yawDeg, 0.0F).spawn(level);
   }

   public static void portalFlat(ServerLevel level, Vec3 center, double radius, int color, int lifetime) {
      effect(VfxEntity.Kind.PORTAL, center).scale(radius).color(color).lifetime(lifetime).orient(0.0F, 90.0F).secondary(new Vec3(1.0, 0.0, 0.0)).spawn(level);
   }

   public static VfxEntity portalTracked(ServerLevel level, Vec3 center, double radius, int color, int lifetime, float yawDeg) {
      return effect(VfxEntity.Kind.PORTAL, center).scale(radius).color(color).lifetime(lifetime).orient(yawDeg, 0.0F).spawnTracked(level);
   }

   public static VfxEntity portalFlatTracked(ServerLevel level, Vec3 center, double radius, int color, int lifetime) {
      return effect(VfxEntity.Kind.PORTAL, center)
         .scale(radius)
         .color(color)
         .lifetime(lifetime)
         .orient(0.0F, 90.0F)
         .secondary(new Vec3(1.0, 0.0, 0.0))
         .spawnTracked(level);
   }

   public static void helix(ServerLevel level, Vec3 base, double radius, double height, double turns, int color, int lifetime) {
      effect(VfxEntity.Kind.HELIX, base).scale(radius).secondary(new Vec3(height, turns, 0.0)).color(color).lifetime(lifetime).spawn(level);
   }

   public static void halo(ServerLevel level, Vec3 pos, double radius, int color, int lifetime) {
      effect(VfxEntity.Kind.HALO, pos).scale(radius).color(color).lifetime(lifetime).spawn(level);
   }

   public static void cubeFront(ServerLevel level, Vec3 pos, double radius, double thickness, int lifetime, int color) {
      effect(VfxEntity.Kind.KINETIC_CUBE_FRONT, pos).scale(radius).secondary(new Vec3(thickness, 0.0, 0.0)).color(color).lifetime(lifetime).spawn(level);
   }

   public static void cubeFront(ServerLevel level, Vec3 pos, double radius, double thickness, int lifetime, int color, int heart) {
      effect(VfxEntity.Kind.KINETIC_CUBE_FRONT, pos).scale(radius).secondary(new Vec3(thickness, 0.0, 0.0)).color(color, heart).lifetime(lifetime).spawn(level);
   }

   public static void cubeFrontOn(ServerLevel level, Vec3 pos, Vec3 normal, double radius, double thickness, int lifetime, int color, int heart, int delay) {
      effect(VfxEntity.Kind.KINETIC_CUBE_FRONT, pos)
         .scale(radius)
         .secondary(new Vec3(thickness, 1.0, 0.0))
         .color(color, heart)
         .lifetime(lifetime)
         .along(normal)
         .delay(delay)
         .spawn(level);
   }

   public static void pixelArc(ServerLevel level, Vec3 from, Vec3 to, int lifetime, int color) {
      effect(VfxEntity.Kind.KINETIC_PIXEL_ARC, from).scale(1.0).secondary(to.subtract(from)).color(color).lifetime(lifetime).spawn(level);
   }

   public static void pixelArc(ServerLevel level, Vec3 from, Vec3 to, int lifetime, int color, int heart) {
      effect(VfxEntity.Kind.KINETIC_PIXEL_ARC, from).scale(1.0).secondary(to.subtract(from)).color(color, heart).lifetime(lifetime).spawn(level);
   }

   public static void mushroom(ServerLevel level, Vec3 pos, double radius, int lifetime) {
      effect(VfxEntity.Kind.MUSHROOM, pos).scale(radius).lifetime(lifetime).spawn(level);
   }

   public static void dustDome(ServerLevel level, Vec3 pos, double radius, int lifetime) {
      effect(VfxEntity.Kind.DUST_DOME, pos).scale(radius).lifetime(lifetime).spawn(level);
   }

   public static void block(ServerLevel level, Vec3 pos, Block block, double sx, double sy, double sz, int lifetime, float yawDeg) {
      spawnBlock(level, pos, block, sx, sy, sz, lifetime, yawDeg, 1.0F, 16777215);
   }

   public static BlockFxEntity blockTracked(ServerLevel level, Vec3 pos, Block block, double sx, double sy, double sz, int lifetime, float yawDeg) {
      return spawnBlock(level, pos, block, sx, sy, sz, lifetime, yawDeg, 1.0F, 16777215);
   }

   public static BlockFxEntity blockState(ServerLevel level, Vec3 pos, BlockState state, double sx, double sy, double sz, int lifetime, float yawDeg) {
      return blockState(level, pos, state, sx, sy, sz, lifetime, yawDeg, 0, false);
   }

   public static BlockFxEntity blockState(
      ServerLevel level, Vec3 pos, BlockState state, double sx, double sy, double sz, int lifetime, float yawDeg, int growTicks, boolean anchorBottom
   ) {
      return blockState(level, pos, state, sx, sy, sz, lifetime, yawDeg, growTicks, anchorBottom, -1);
   }

   public static BlockFxEntity blockState(
      ServerLevel level,
      Vec3 pos,
      BlockState state,
      double sx,
      double sy,
      double sz,
      int lifetime,
      float yawDeg,
      int growTicks,
      boolean anchorBottom,
      int anchorEntityId
   ) {
      BlockFxEntity e = prepared(level, pos, state, sx, sy, sz, lifetime, yawDeg, growTicks, anchorBottom, anchorEntityId);
      level.addFreshEntity(e);
      return e;
   }

   public static void blockHeave(
      ServerLevel level,
      Vec3 bottomCentre,
      BlockState state,
      double sx,
      double sy,
      double sz,
      int lifetime,
      float yawDeg,
      double rise,
      float tiltDeg,
      int delay
   ) {
      blockHeave(level, bottomCentre, state, sx, sy, sz, lifetime, yawDeg, rise, tiltDeg, delay, 0, 0.0F);
   }

   public static void blockHeave(
      ServerLevel level,
      Vec3 bottomCentre,
      BlockState state,
      double sx,
      double sy,
      double sz,
      int lifetime,
      float yawDeg,
      double rise,
      float tiltDeg,
      int delay,
      int glowRgb,
      float glowStrength
   ) {
      int held = Math.max(0, delay);
      BlockFxEntity e = prepared(level, bottomCentre, state, sx, sy, sz, lifetime + held, yawDeg, 0, true, -1);
      e.setDelay(held);
      e.setHeave((float)rise, tiltDeg);
      e.setHeaveGlow(glowRgb, glowStrength);
      level.addFreshEntity(e);
   }

   private static BlockFxEntity prepared(
      ServerLevel level,
      Vec3 pos,
      BlockState state,
      double sx,
      double sy,
      double sz,
      int lifetime,
      float yawDeg,
      int growTicks,
      boolean anchorBottom,
      int anchorEntityId
   ) {
      BlockFxEntity e = new BlockFxEntity(AwakenedEntities.BLOCK_FX.get(), level);
      e.setPos(pos.x, pos.y, pos.z);
      e.configure(state, (float)sx, (float)sy, (float)sz, lifetime, yawDeg);
      e.setGrowTicks(growTicks);
      e.setAnchorBottom(anchorBottom);
      e.setAnchorEntity(anchorEntityId);
      return e;
   }

   public static BlockFxEntity blockThrown(
      ServerLevel level,
      Vec3 pos,
      BlockState state,
      double sx,
      double sy,
      double sz,
      int lifetime,
      float yawDeg,
      Vec3 velocity,
      double restY,
      float tumbleDegPerTick
   ) {
      BlockFxEntity e = new BlockFxEntity(AwakenedEntities.BLOCK_FX.get(), level);
      e.setPos(pos.x, pos.y, pos.z);
      e.configure(state, (float)sx, (float)sy, (float)sz, lifetime, yawDeg);
      e.setLaunch(velocity, restY, tumbleDegPerTick);
      level.addFreshEntity(e);
      return e;
   }

   public static BlockFxEntity ghostBlock(ServerLevel level, Vec3 pos, Block block, float alpha, int lifetime) {
      return spawnBlock(level, pos, block, 0.95F, 0.95F, 0.95F, lifetime, 0.0F, alpha, 16777215);
   }

   public static BlockFxEntity wrongBlockShell(ServerLevel level, Vec3 pos, Block occupying, int lifetime) {
      return spawnBlock(level, pos, occupying, 1.05F, 1.05F, 1.05F, lifetime, 0.0F, 0.3F, 16722474);
   }

   private static BlockFxEntity spawnBlock(
      ServerLevel level, Vec3 pos, Block block, double sx, double sy, double sz, int lifetime, float yawDeg, float alpha, int tint
   ) {
      BlockFxEntity e = new BlockFxEntity(AwakenedEntities.BLOCK_FX.get(), level);
      e.setPos(pos.x, pos.y, pos.z);
      e.configure(block, (float)sx, (float)sy, (float)sz, lifetime, yawDeg);
      e.setAlpha(alpha);
      e.setTint(tint);
      level.addFreshEntity(e);
      return e;
   }

   public static float yawAlong(Vec3 dir) {
      return (float)Math.toDegrees(Math.atan2(dir.x, dir.z));
   }

   public static float pitchAlong(Vec3 dir) {
      return (float)Math.toDegrees(Math.atan2(-dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)));
   }

   public static Vec3 safeDir(Vec3 dir) {
      if (dir == null) {
         return FORWARD;
      } else {
         double len = dir.length();
         return len < 1.0E-6 ? FORWARD : dir.scale(1.0 / len);
      }
   }

   public static Vfx.Builder effect(VfxEntity.Kind kind, Vec3 pos) {
      return new Vfx.Builder(kind, pos);
   }

   public static void spawn(ServerLevel level, VfxEntity.Kind kind, Vec3 pos, double scale, Vec3 secondary, int lifetime, int color, float intensity) {
      effect(kind, pos).scale(scale).secondary(secondary).lifetime(lifetime).color(color).intensity(intensity).spawn(level);
   }

   public static final class Builder {
      private final VfxEntity.Kind kind;
      private final Vec3 pos;
      private double scale = 1.0;
      private Vec3 secondary = Vec3.ZERO;
      private int lifetime = 20;
      private float intensity = 1.0F;
      private int color = 16762170;
      private int color2 = -1;
      private float yaw;
      private float pitch;
      private int ownerId = -1;
      private float ownerDim = 1.0F;
      private boolean noDepth = false;
      private int delay = 0;

      Builder(VfxEntity.Kind kind, Vec3 pos) {
         this.kind = kind;
         this.pos = pos;
      }

      public Vfx.Builder scale(double scale) {
         this.scale = scale;
         return this;
      }

      public Vfx.Builder secondary(Vec3 secondary) {
         this.secondary = secondary;
         return this;
      }

      public Vfx.Builder lifetime(int ticks) {
         this.lifetime = ticks;
         return this;
      }

      public Vfx.Builder intensity(float intensity) {
         this.intensity = intensity;
         return this;
      }

      public Vfx.Builder color(int rgb) {
         this.color = rgb;
         return this;
      }

      public Vfx.Builder color(int body, int core) {
         this.color = body;
         this.color2 = core;
         return this;
      }

      public Vfx.Builder orient(float yaw, float pitch) {
         this.yaw = yaw;
         this.pitch = pitch;
         return this;
      }

      public Vfx.Builder along(Vec3 dir) {
         Vec3 n = Vfx.safeDir(dir);
         float pitch = (float)Math.toDegrees(Math.acos(Mth.clamp(n.y, -1.0, 1.0)));
         float yaw = (float)Math.toDegrees(Math.atan2(n.x, n.z));
         return this.orient(yaw, pitch);
      }

      public Vfx.Builder owner(int ownerEntityId) {
         this.ownerId = ownerEntityId;
         return this;
      }

      public Vfx.Builder ownerDim(float dim) {
         this.ownerDim = dim;
         return this;
      }

      public Vfx.Builder ownerView(int ownerEntityId, float dim) {
         return this.owner(ownerEntityId).ownerDim(dim);
      }

      public Vfx.Builder noDepth(boolean noDepth) {
         this.noDepth = noDepth;
         return this;
      }

      public Vfx.Builder delay(int ticks) {
         this.delay = Math.max(0, ticks);
         return this;
      }

      public void spawn(ServerLevel level) {
         this.spawnTracked(level);
      }

      public VfxEntity spawnTracked(ServerLevel level) {
         VfxEntity e = new VfxEntity(AwakenedEntities.VFX.get(), level);
         e.setPos(this.pos.x, this.pos.y, this.pos.z);
         e.configure(this.kind, (float)this.scale, this.secondary, this.lifetime + this.delay, this.intensity, this.color, this.color2, this.yaw, this.pitch);
         e.setOwnerId(this.ownerId);
         e.setOwnerDim(this.ownerDim);
         e.setNoDepth(this.noDepth);
         e.setDelay(this.delay);
         level.addFreshEntity(e);
         return e;
      }
   }
}
