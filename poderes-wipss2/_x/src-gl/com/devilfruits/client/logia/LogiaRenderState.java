package com.devilfruits.client.logia;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.logia.LogiaData;
import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.core.logia.LogiaState;
import com.devilfruits.core.network.LogiaHitPayload;
import com.devilfruits.registry.DFAttachments;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class LogiaRenderState {
   private static final Int2ObjectMap<LogiaRenderState> STATES = new Int2ObjectOpenHashMap();
   private static final int MAX_HOLES = 8;
   private static final int RELEASE_AFTER = 200;
   private static final double RANGE = 96.0;
   private static int clock;
   private static int serials;
   private final float[] level = new float[6];
   private final float[] target = new float[6];
   private final int[] delay = new int[6];
   private final float[] stepped = new float[6];
   private final int[] contact = new int[6];
   public static ToIntFunction<Player> solidLimbs = p -> 0;
   public static BiConsumer<Player, LogiaHitPayload.Hit> hakiContact = (p, h) -> {
   };
   private final LogiaRenderState.Hole[] holes = new LogiaRenderState.Hole[8];
   private int holeCount;
   private final List<LogiaRenderState.Tunnel> tunnels = new ArrayList<>();
   private final boolean[] held = new boolean[6];
   private LogiaState state = LogiaState.SOLID;
   private int limbs;
   private boolean seen;
   @Nullable
   private LogiaSkinMask mask;
   private int maskRetry;
   private int solidTicks;
   private int lastTick;

   private LogiaRenderState() {
      for (int i = 0; i < 8; i++) {
         this.holes[i] = new LogiaRenderState.Hole();
      }
   }

   public static LogiaRenderState of(Player player) {
      return (LogiaRenderState)STATES.computeIfAbsent(player.getId(), k -> new LogiaRenderState());
   }

   @Nullable
   public static LogiaRenderState get(Player player) {
      return (LogiaRenderState)STATES.get(player.getId());
   }

   public static void clear() {
      ObjectIterator var0 = STATES.values().iterator();

      while (var0.hasNext()) {
         LogiaRenderState s = (LogiaRenderState)var0.next();
         s.release();
      }

      STATES.clear();
      LogiaBodyShader.forgetAll();
   }

   public LogiaState state() {
      return this.state;
   }

   public float level(int limbIndex) {
      return this.stepped[limbIndex];
   }

   public float bodyLevel(int limbIndex) {
      return this.held[limbIndex] ? 0.0F : this.level[limbIndex];
   }

   public List<LogiaRenderState.Tunnel> tunnels() {
      return this.tunnels;
   }

   void bindTunnels(Vec3 cam, Matrix4f[] limbs, LogiaBodyShader.Uniforms u) {
      for (LogiaRenderState.Tunnel t : this.tunnels) {
         if (t.limb < 0) {
            Vector3f a = new Vector3f((float)(t.worldA.x - cam.x), (float)(t.worldA.y - cam.y), (float)(t.worldA.z - cam.z));
            Vector3f b = new Vector3f((float)(t.worldB.x - cam.x), (float)(t.worldB.y - cam.y), (float)(t.worldB.z - cam.z));
            int best = 1;
            double bestD = Double.MAX_VALUE;

            for (int i = 0; i < 6; i++) {
               Vector3f r = new Vector3f(u.root[i * 4], u.root[i * 4 + 1], u.root[i * 4 + 2]);
               Vector3f tip = new Vector3f(u.tip[i * 4], u.tip[i * 4 + 1], u.tip[i * 4 + 2]);
               double d = Double.MAX_VALUE;

               for (int k = 0; k <= 8; k++) {
                  Vector3f p = new Vector3f(a).lerp(b, (float)k / 8.0F);
                  d = Math.min(d, segment(p, r, tip));
               }

               if (d < bestD) {
                  bestD = d;
                  best = i;
               }
            }

            Matrix4f inv = new Matrix4f(limbs[best]).invert();
            inv.transformPosition(a, t.localA);
            inv.transformPosition(b, t.localB);
            t.limb = best;
         }
      }
   }

   private static double segment(Vector3f p, Vector3f a, Vector3f b) {
      Vector3f ab = new Vector3f(b).sub(a);
      float len = ab.lengthSquared();
      float t = len < 1.0E-8F ? 0.0F : Mth.clamp(new Vector3f(p).sub(a).dot(ab) / len, 0.0F, 1.0F);
      return (double)new Vector3f(a).fma(t, ab).distance(p);
   }

   public boolean hidden() {
      for (float v : this.stepped) {
         if (v < 1.0F) {
            return false;
         }
      }

      return true;
   }

   public boolean visiblyChanged() {
      if (this.holeCount <= 0 && this.tunnels.isEmpty()) {
         for (float v : this.stepped) {
            if (v > 0.0F) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public static void tickAll() {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      if (level == null) {
         if (!STATES.isEmpty()) {
            clear();
         }
      } else {
         clock++;
         Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();

         for (AbstractClientPlayer player : level.players()) {
            if (LogiaManager.isLogia(player) && (player == mc.player || !(player.distanceToSqr(camera) > 9216.0))) {
               LogiaRenderState s = of(player);
               s.lastTick = clock;
               s.tick(player, mc);
            }
         }

         ObjectIterator<Entry<LogiaRenderState>> it = STATES.int2ObjectEntrySet().iterator();

         while (it.hasNext()) {
            Entry<LogiaRenderState> entry = (Entry<LogiaRenderState>)it.next();
            LogiaRenderState s = (LogiaRenderState)entry.getValue();
            if (s.lastTick != clock) {
               s.release();
               LogiaBodyShader.forget(entry.getIntKey());
               it.remove();
            }
         }
      }
   }

   private void tick(AbstractClientPlayer player, Minecraft mc) {
      LogiaTheme theme = LogiaThemes.current(player);
      this.observe(player);

      for (int i = 0; i < 6; i++) {
         if (this.delay[i] > 0) {
            this.delay[i]--;
         } else {
            float rate = this.target[i] > this.level[i] ? theme.dissolveRate() : theme.reformRate();
            this.level[i] = this.level[i] < this.target[i] ? Math.min(this.target[i], this.level[i] + rate) : Math.max(this.target[i], this.level[i] - rate);
         }
      }

      int steps = steps();
      int solid = solidLimbs.applyAsInt(player);

      for (int ix = 0; ix < 6; ix++) {
         this.stepped[ix] = quantise(this.level[ix], steps);
         if (this.contact[ix] > 0) {
            this.contact[ix]--;
            this.stepped[ix] = 0.0F;
         } else if ((solid & 1 << ix) != 0) {
            this.stepped[ix] = 0.0F;
         }

         this.held[ix] = this.contact[ix] > 0 || (solid & 1 << ix) != 0;
      }

      Iterator<LogiaRenderState.Tunnel> ti = this.tunnels.iterator();

      while (ti.hasNext()) {
         LogiaRenderState.Tunnel t = ti.next();
         if (++t.age >= t.maxAge || t.limb < 0 && t.age > 12) {
            ti.remove();
         }
      }

      int ix = 0;

      while (ix < this.holeCount) {
         LogiaRenderState.Hole h = this.holes[ix];
         if (++h.age >= h.maxAge) {
            this.holes[ix] = this.holes[--this.holeCount];
            this.holes[this.holeCount] = h;
         } else {
            ix++;
         }
      }

      if (this.visiblyChanged()) {
         this.solidTicks = 0;
      } else if (++this.solidTicks > 200 && this.mask != null) {
         this.mask.close();
         this.mask = null;
      }

      if (this.maskRetry > 0) {
         this.maskRetry--;
      }

      if (!player.isInvisible() && !player.isSpectator() && (Boolean)CoreClientConfig.LOGIA_SHEDDING.get()) {
         this.shed(player, mc, theme);
      }

      theme.hooks().tick(player, this);
   }

   void observe(Player player) {
      if (player.hasData(DFAttachments.PLAYER_DATA)) {
         LogiaData data = DFAttachments.data(player).logia();
         LogiaTheme theme = LogiaThemes.current(player);
         if (!this.seen) {
            this.seen = true;
            this.state = data.state();
            this.limbs = data.limbs();
            this.retarget(theme);
            System.arraycopy(this.target, 0, this.level, 0, 6);
            Arrays.fill(this.delay, 0);
         } else if (data.state() != this.state || data.limbs() != this.limbs) {
            LogiaState before = this.state;
            this.state = data.state();
            this.limbs = data.limbs();
            this.retarget(theme);
            this.transitionFx(player, theme, before, this.state);
            theme.hooks().onState(player, before, this.state);
         }
      }
   }

   private void retarget(LogiaTheme theme) {
      int[] order = theme.dissolveOrder();
      Arrays.fill(this.delay, 0);
      switch (this.state) {
         case ELEMENTAL_BODY:
            int i = 0;

            while (i < order.length) {
               int limb = LogiaSkinLayout.index(order[i]);
               this.target[limb] = 1.0F;
               this.delay[limb] = i++;
            }
            break;
         case REFORMING:
            if (max(this.level) < 0.5F) {
               Arrays.fill(this.level, 1.0F);
            }

            for (int i = 0; i < order.length; i++) {
               int limb = LogiaSkinLayout.index(order[i]);
               this.target[limb] = 0.0F;
               this.delay[limb] = order.length - 1 - i;
            }
            break;
         case PARTIAL:
            for (int i = 0; i < 6; i++) {
               this.target[i] = (this.limbs & LogiaSkinLayout.bit(i)) != 0 ? theme.partialLevel() : 0.0F;
            }
            break;
         default:
            Arrays.fill(this.target, 0.0F);
      }
   }

   void dashed() {
      Arrays.fill(this.level, 1.0F);
      Arrays.fill(this.stepped, 1.0F);
   }

   void onHits(Player player, List<LogiaHitPayload.Hit> hits) {
      LogiaTheme theme = LogiaThemes.current(player);
      boolean passed = false;

      for (LogiaHitPayload.Hit hit : hits) {
         if (hit.haki()) {
            this.contact(player, hit, theme);
         } else {
            passed = true;
            if (LogiaBodyShader.active()) {
               this.addTunnel(player, hit, theme);
            } else if (CoreClientConfig.LOGIA_BODY_QUALITY.get() != CoreClientConfig.VfxQuality.LOW) {
               this.addHole(player, hit, theme);
            }

            punchThrough(player, hit, theme);
            theme.hooks().onHit(player, hit);
         }
      }

      if (passed) {
         Vec3 at = player.position().add(0.0, (double)player.getBbHeight() * 0.6, 0.0);
         SoundDesign.play(at, theme.passSound(), 0.7F, 0.95F + Fx.random().nextFloat() * 0.15F, theme.passLayers());
      }
   }

   private void contact(Player player, LogiaHitPayload.Hit hit, LogiaTheme theme) {
      double yaw = Math.toRadians((double)player.yBodyRot);
      float ws = player.getBbWidth() / 0.6F;
      float hs = player.getBbHeight() / 1.8F;
      float lateral = (float)((hit.pos().x - player.getX()) * -Math.cos(yaw) + (hit.pos().z - player.getZ()) * -Math.sin(yaw)) / ws;
      float height = (float)(hit.pos().y - player.getY()) / hs;
      int limb = LogiaSkinLayout.limbAt(lateral, height);
      this.contact[limb] = 10;
      RandomSource r = Fx.random();
      int n = Fx.count(10);

      for (int i = 0; i < n; i++) {
         Fx.spawn(
            theme.clumps(r),
            hit.pos().x + Fx.spread(0.2),
            hit.pos().y + Fx.spread(0.2),
            hit.pos().z + Fx.spread(0.2),
            hit.dir().x * 0.06 + Fx.spread(0.08),
            0.05 + r.nextDouble() * 0.08,
            hit.dir().z * 0.06 + Fx.spread(0.08)
         );
      }

      hakiContact.accept(player, hit);
   }

   private void addTunnel(Player player, LogiaHitPayload.Hit hit, LogiaTheme theme) {
      Vec3 dir = hit.dir().lengthSqr() < 1.0E-6 ? player.getLookAngle().reverse() : hit.dir().normalize();
      LogiaRenderState.Tunnel t = new LogiaRenderState.Tunnel();
      t.worldA = hit.pos().subtract(dir.scale(0.3));
      t.worldB = hit.pos().add(dir.scale((double)player.getBbWidth() + 0.4));
      t.radius0 = Mth.clamp(0.06F + hit.size() * 0.3F, 0.1F, 1.6F) * (hit.partial() ? 0.7F : 1.0F);
      t.maxAge = (int)((float)theme.holeTicks() * (1.0F + Math.min(2.0F, hit.size() * 0.6F)));
      if (this.tunnels.size() >= 8) {
         this.tunnels.remove(0);
      }

      this.tunnels.add(t);
   }

   private void addHole(Player player, LogiaHitPayload.Hit hit, LogiaTheme theme) {
      Vec3 p = hit.pos().add(hit.dir().scale(0.3));
      double yaw = Math.toRadians((double)player.yBodyRot);
      double rx = -Math.cos(yaw);
      double rz = -Math.sin(yaw);
      float ws = player.getBbWidth() / 0.6F;
      float hs = player.getBbHeight() / 1.8F;
      float lateral = (float)((p.x - player.getX()) * rx + (p.z - player.getZ()) * rz) / ws;
      float height = (float)(p.y - player.getY()) / hs;
      int limb = LogiaSkinLayout.limbAt(lateral, height);
      LogiaRenderState.Hole h;
      if (this.holeCount < 8) {
         h = this.holes[this.holeCount++];
      } else {
         h = this.holes[0];

         for (int i = 1; i < 8; i++) {
            if (this.holes[i].age > h.age) {
               h = this.holes[i];
            }
         }
      }

      h.limb = limb;
      h.across = LogiaSkinLayout.across(limb, lateral);
      h.down = LogiaSkinLayout.down(limb, height);
      h.radius0 = hit.partial() ? 1.5F : 2.5F;
      h.age = 0;
      h.maxAge = theme.holeTicks();
      h.serial = ++serials;
   }

   private static void punchThrough(Player player, LogiaHitPayload.Hit hit, LogiaTheme theme) {
      RandomSource r = Fx.random();
      Vec3 dir = hit.dir();
      Vec3 exit = hit.pos().add(dir.scale((double)player.getBbWidth()));
      int n = Fx.count(hit.partial() ? 6 : 12);

      for (int i = 0; i < n; i++) {
         double speed = 0.14 + r.nextDouble() * 0.16;
         Fx.spawn(
            theme.burst(r),
            exit.x + Fx.spread(0.1),
            exit.y + Fx.spread(0.1),
            exit.z + Fx.spread(0.1),
            dir.x * speed + Fx.spread(0.05),
            dir.y * speed + Fx.spread(0.05) + 0.02,
            dir.z * speed + Fx.spread(0.05)
         );
      }

      int back = Fx.count(4);

      for (int i = 0; i < back; i++) {
         Fx.spawn(theme.shed(r), hit.pos().x, hit.pos().y, hit.pos().z, -dir.x * 0.04 + Fx.spread(0.03), Fx.spread(0.03), -dir.z * 0.04 + Fx.spread(0.03));
      }
   }

   private void transitionFx(Player player, LogiaTheme theme, LogiaState before, LogiaState after) {
      Vec3 feet = player.position();
      Vec3 centre = feet.add(0.0, (double)player.getBbHeight() * 0.55, 0.0);
      if (after == LogiaState.ELEMENTAL_BODY && before != LogiaState.ELEMENTAL_BODY) {
         burst(theme, feet, player.getBbWidth(), player.getBbHeight(), Fx.count(22), 0.12);
         SoundDesign.play(centre, theme.dissolveSound(), 0.8F, 1.0F);
      } else if (after == LogiaState.REFORMING || before == LogiaState.ELEMENTAL_BODY) {
         converge(theme, feet, player.getBbWidth(), player.getBbHeight(), Fx.count(18));
         SoundDesign.play(centre, theme.reformSound(), 0.7F, 1.0F);
      } else if (after == LogiaState.PARTIAL && before != LogiaState.PARTIAL) {
         for (int i = 0; i < 6; i++) {
            if ((this.limbs & LogiaSkinLayout.bit(i)) != 0) {
               limbBurst(player, theme, i, Fx.count(6));
            }
         }

         SoundDesign.play(centre, theme.dissolveSound(), 0.4F, 1.3F);
      }

      if (after == LogiaState.SUPPRESSED && before != LogiaState.SUPPRESSED) {
         int n = Fx.count(8);

         for (int ix = 0; ix < n; ix++) {
            Fx.spawn(theme.burst(Fx.random()), feet.x + Fx.spread(0.35), centre.y + Fx.spread(0.6), feet.z + Fx.spread(0.35), 0.0, -0.02, 0.0);
         }

         SoundDesign.play(centre, theme.suppressSound(), 0.6F, 1.0F);
      }
   }

   static void fizzle(Player player) {
      LogiaTheme theme = LogiaThemes.current(player);
      RandomSource r = Fx.random();
      Vec3 feet = player.position();
      int n = Fx.count(8);

      for (int i = 0; i < n; i++) {
         double a = r.nextDouble() * Math.PI * 2.0;
         double y = (double)player.getBbHeight() * (0.35 + r.nextDouble() * 0.45);
         Fx.spawn(theme.burst(r), feet.x + Math.cos(a) * 0.3, feet.y + y, feet.z + Math.sin(a) * 0.3, Math.cos(a) * 0.03, -0.02, Math.sin(a) * 0.03);
      }

      SoundDesign.play(feet.add(0.0, (double)player.getBbHeight() * 0.5, 0.0), theme.suppressSound(), 0.5F, 1.1F);
   }

   static void burst(LogiaTheme theme, Vec3 feet, float width, float height, int count, double speed) {
      RandomSource r = Fx.random();

      for (int i = 0; i < count; i++) {
         double a = r.nextDouble() * Math.PI * 2.0;
         double ox = Math.cos(a);
         double oz = Math.sin(a);
         double y = r.nextDouble() * (double)height;
         double s = speed * (0.5 + r.nextDouble());
         Fx.spawn(theme.burst(r), feet.x + ox * (double)width * 0.4, feet.y + y, feet.z + oz * (double)width * 0.4, ox * s, (r.nextDouble() - 0.3) * s, oz * s);
      }
   }

   static void converge(LogiaTheme theme, Vec3 feet, float width, float height, int count) {
      RandomSource r = Fx.random();

      for (int i = 0; i < count; i++) {
         double a = r.nextDouble() * Math.PI * 2.0;
         double ox = Math.cos(a);
         double oz = Math.sin(a);
         double dist = (double)width * 0.6 + 0.6 + r.nextDouble() * 0.5;
         double y = r.nextDouble() * (double)height;
         Fx.spawn(theme.shed(r), feet.x + ox * dist, feet.y + y, feet.z + oz * dist, -ox * dist * 0.1, 0.0, -oz * dist * 0.1);
      }
   }

   private static void limbBurst(Player player, LogiaTheme theme, int limb, int count) {
      RandomSource r = Fx.random();

      for (int i = 0; i < count; i++) {
         Vec3 p = limbPoint(player, limb, r);
         Fx.spawn(theme.burst(r), p.x, p.y, p.z, Fx.spread(0.06), r.nextDouble() * 0.04, Fx.spread(0.06));
      }
   }

   static Vec3 limbPoint(Player player, int limb, RandomSource r) {
      float[] e = LogiaSkinLayout.extents(limb);
      double yaw = Math.toRadians((double)player.yBodyRot);
      double rx = -Math.cos(yaw);
      double rz = -Math.sin(yaw);
      double fx = -Math.sin(yaw);
      double fz = Math.cos(yaw);
      double ws = (double)player.getBbWidth() / 0.6;
      double hs = (double)player.getBbHeight() / 1.8;
      double lateral = Mth.lerp(r.nextDouble(), (double)e[0], (double)e[1]) * ws;
      double height = Mth.lerp(r.nextDouble(), (double)e[2], (double)e[3]) * hs;
      double depth = (r.nextDouble() - 0.5) * 0.25 * ws;
      return new Vec3(player.getX() + rx * lateral + fx * depth, player.getY() + height, player.getZ() + rz * lateral + fz * depth);
   }

   private void shed(AbstractClientPlayer player, Minecraft mc, LogiaTheme theme) {
      RandomSource r = Fx.random();
      boolean ownFirstPerson = player == mc.player && mc.options.getCameraType().isFirstPerson();
      double dx = player.getX() - player.xo;
      double dz = player.getZ() - player.zo;
      if (this.state == LogiaState.ELEMENTAL_BODY) {
         double speed = Math.sqrt(dx * dx + dz * dz);
         int n = Fx.count(theme.elementalDensity() + theme.trail() * (float)speed);
         double top = ownFirstPerson ? 1.0 : (double)player.getBbHeight();

         for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2.0;
            double rad = 0.15 + r.nextDouble() * 0.35;
            double y = r.nextDouble() * top;
            Fx.spawn(
               theme.shed(r),
               player.getX() + Math.cos(a) * rad,
               player.getY() + y,
               player.getZ() + Math.sin(a) * rad,
               -Math.sin(a) * 0.08 + dx * 0.5,
               0.01,
               Math.cos(a) * 0.08 + dz * 0.5
            );
         }
      } else {
         for (int i = 0; i < 6; i++) {
            if (this.stepped[i] > 0.0F && (!ownFirstPerson || i >= 4)) {
               int m = Fx.count(this.stepped[i] * 0.6F);

               for (int k = 0; k < m; k++) {
                  Vec3 p = limbPoint(player, i, r);
                  Fx.spawn(theme.shed(r), p.x, p.y, p.z, Fx.spread(0.02), -0.03, Fx.spread(0.02));
               }
            }
         }
      }
   }

   @Nullable
   ResourceLocation maskedSkin(AbstractClientPlayer player) {
      if (this.visiblyChanged() && !LogiaBodyShader.active()) {
         PlayerSkin skin = player.getSkin();
         boolean slim = skin.model() == Model.SLIM;
         if (this.mask == null || !this.mask.skin.equals(skin.texture()) || this.mask.slim != slim) {
            if (this.mask != null) {
               this.mask.close();
               this.mask = null;
            }

            if (this.maskRetry > 0) {
               return null;
            }

            this.mask = LogiaSkinMask.create(skin.texture(), slim);
            if (this.mask == null) {
               this.maskRetry = 40;
               return null;
            }
         }

         CoreClientConfig.VfxQuality quality = (CoreClientConfig.VfxQuality)CoreClientConfig.LOGIA_BODY_QUALITY.get();

         float band = switch (quality) {
            case LOW -> 0.0F;
            case MEDIUM -> 0.08F;
            case HIGH -> 0.13F;
         };
         int edge = LogiaThemes.current(player).edgeColor();
         this.mask.update(this.signature(quality) * 31L + (long)edge, this.stepped, band, edge, this.holes, this.holeCount);
         return this.mask.location;
      } else {
         return null;
      }
   }

   private long signature(CoreClientConfig.VfxQuality quality) {
      long sig = (long)quality.ordinal();

      for (float v : this.stepped) {
         sig = sig * 9L + (long)((int)(v * 8.0F));
      }

      for (int i = 0; i < this.holeCount; i++) {
         sig = sig * 31L + (long)this.holes[i].serial * 64L + (long)((int)(this.holes[i].radius() * 2.0F));
      }

      return sig;
   }

   private void release() {
      if (this.mask != null) {
         this.mask.close();
         this.mask = null;
      }
   }

   private static int steps() {
      return switch ((CoreClientConfig.VfxQuality)CoreClientConfig.LOGIA_BODY_QUALITY.get()) {
         case LOW -> 1;
         case MEDIUM -> 4;
         case HIGH -> 8;
      };
   }

   private static float quantise(float v, int steps) {
      if (steps == 1) {
         return v >= 0.5F ? 1.0F : 0.0F;
      } else {
         return (float)Mth.floor(v * (float)steps + 1.0E-4F) / (float)steps;
      }
   }

   private static float max(float[] values) {
      float m = 0.0F;

      for (float v : values) {
         m = Math.max(m, v);
      }

      return m;
   }

   public static final class Hole {
      int limb;
      float across;
      float down;
      float radius0;
      int age;
      int maxAge;
      int serial;

      public float radius() {
         float r = this.radius0 * (1.0F - (float)this.age / (float)Math.max(1, this.maxAge));
         return Math.max(0.0F, (float)Mth.ceil(r * 2.0F) / 2.0F);
      }
   }

   public static final class Tunnel {
      int limb = -1;
      Vec3 worldA = Vec3.ZERO;
      Vec3 worldB = Vec3.ZERO;
      final Vector3f localA = new Vector3f();
      final Vector3f localB = new Vector3f();
      float radius0;
      int age;
      int maxAge;

      public float radius() {
         float k = Mth.clamp(((float)this.age / (float)Math.max(1, this.maxAge) - 0.4F) / 0.6F, 0.0F, 1.0F);
         return this.radius0 * (1.0F - k * k * (3.0F - 2.0F * k));
      }
   }
}
