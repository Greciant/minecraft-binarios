package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.ArbiterEntity;
import com.awakenedmod.awakened.content.entity.ArbiterSealEntity;
import com.awakenedmod.awakened.content.entity.ArbiterWhipEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;

public final class ArbiterChainRenderer {
   private static final float SAG_PER_BLOCK = 0.073275864F;
   private static final double SEAL_CHAIN_CLEARANCE = 1.0;
   private static final float SAG_PEAK = 1.5F;
   private static final double YOKE_OUT = 0.55;
   private static final double ROOT_BURIED = 6.0;
   private static final float WEAPON_THICKNESS = 1.3F;
   private static final float ROOT_THICKNESS = 1.45F;
   private static final double ROOT_LEAN = 0.32;
   private static final float ROOT_WAVER = 0.03F;
   private static final float ROOT_CLIMB_TICKS = 6.0F;
   private static final float ROOT_FLOOR_T = 0.51666665F;
   private static final float SEAL_TAUT = 0.55F;
   private static final float SEAL_GROW = 10.0F;
   private static final float SEAL_STAGGER = 1.5F;
   private static final float CUE_SPEED = 0.6F;
   private static final float CUE_LINGER = 4.0F;
   private static final float SHATTER_TICKS = 10.0F;
   private static final float SEAL_SHATTER_TICKS = 16.0F;
   private static final float RETRACT_TICKS = 5.0F;
   private static final float PENDULUM_RETRACT_TICKS = 10.0F;
   private static final double PENDULUM_ANCHOR_OUT = 1.5;
   private static final float PENDULUM_SLOW = 1.2F;
   private static final float PENDULUM_FAST = 3.0F;
   private static final float PIN_DRIVE_TICKS = 5.0F;
   private static final float PIN_STAGGER = 1.1F;
   private static final double PIN_LIFT = 1.7;
   private static final double PIN_STAND = 0.16;
   private static final float PIN_SAG = 0.055F;
   private static final float PIN_THICKNESS = 1.15F;
   private static final float PIN_CHARGE = 0.12F;
   private static final float DEATH_SLACK_TICKS = 40.0F;
   private static final float PIN_SAG_DEAD = 0.42F;
   private static final double[][] PINS = new double[][]{
      {0.0, 0.0, 0.82, 2.55, 52.0},
      {0.0, 0.0, -0.82, 2.55, -52.0},
      {0.0, 0.0, 0.0, 1.85, 104.0},
      {0.0, 0.0, 0.0, 1.85, -104.0},
      {3.05, 1.35, -0.05, 3.05, 6.0},
      {1.55, 0.3, 0.05, 2.35, 176.0}
   };
   private static final ArbiterChainRenderer.PinAnchor[] PIN_ANCHORS = new ArbiterChainRenderer.PinAnchor[]{
      ArbiterChainRenderer.PinAnchor.YOKE,
      ArbiterChainRenderer.PinAnchor.YOKE,
      ArbiterChainRenderer.PinAnchor.RIGHT_HAND,
      ArbiterChainRenderer.PinAnchor.LEFT_HAND,
      ArbiterChainRenderer.PinAnchor.SILHOUETTE,
      ArbiterChainRenderer.PinAnchor.SILHOUETTE
   };
   private static final int KNEEL_CHAINS = PINS.length;
   private static final float SPILL_FALL_TICKS = 12.0F;
   private static final float SPILL_STAGGER = 1.5F;
   private static final double SPILL_ANCHOR_OUT = 1.5;
   private static final double SPILL_LIE = 0.14;
   private static final float SPILL_DROP_SAG = 0.34F;
   private static final float SPILL_RUN_SAG = 0.05F;
   private static final float SPILL_WAVER = 0.05F;
   private static final float SPILL_CHARGE = 0.1F;
   private static final int TRAIL_LEN = 10;
   private static final float TRAIL_HALF_HEIGHT = 0.26F;
   private static final Map<Integer, ArbiterChainRenderer.Seen> SEEN = new HashMap<>();
   private static final List<ArbiterChainRenderer.Ghost> GHOSTS = new ArrayList<>();
   private static final Map<Integer, ArbiterChainRenderer.Court> COURTS = new HashMap<>();
   private static final Map<Integer, ArbiterChainRenderer.Trail> TRAILS = new HashMap<>();
   private static final List<ChainLinks.Chain> POOL = new ArrayList<>();
   private static final List<ChainLinks.Chain> FRAME = new ArrayList<>();
   private static ArbiterChainRenderer.Demo demo;
   private static Object memoryLevel;
   private static final float WINDUP_DROP = 2.5F;
   private static final float WINDUP_GROW = 12.0F;
   private static final float REEL_WINDUP_DROP = 3.4F;
   private static final float REEL_WINDUP_GROW = 24.0F;

   private ArbiterChainRenderer() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(event -> {
         if (event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
            draw(event);
         }
      });
   }

   public static void startDemo(Vec3 origin, float yawDeg, int ticks) {
      Minecraft mc = Minecraft.getInstance();
      long now = mc.level != null ? mc.level.getGameTime() : 0L;
      Vec3 fwd = Vec3.directionFromRotation(0.0F, yawDeg);
      Vec3 flat = new Vec3(fwd.x, 0.0, fwd.z);
      demo = new ArbiterChainRenderer.Demo(origin, flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize(), now, ticks);
   }

   public static void stopDemo() {
      demo = null;
   }

   private static void draw(RenderLevelStageEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         memoryLevel = null;
      } else {
         float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
         Vec3 cam = event.getCamera().getPosition();
         long now = mc.level.getGameTime();
         float time = (float)(now % 100000L) + pt;
         if (memoryLevel != mc.level) {
            memoryLevel = mc.level;
            SEEN.clear();
            GHOSTS.clear();
            COURTS.clear();
            TRAILS.clear();
            demo = null;
         }

         FRAME.clear();

         for (ArbiterChainRenderer.Seen s : SEEN.values()) {
            s.seenThisFrame = false;
         }

         for (ArbiterChainRenderer.Trail t : TRAILS.values()) {
            t.seenThisFrame = false;
         }

         for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ArbiterEntity boss) {
               court(boss, now, pt);
            } else if (entity instanceof ArbiterSealEntity seal) {
               seal(seal, now, pt);
            } else if (entity instanceof ArbiterWhipEntity whip) {
               whip(whip, now, pt);
            }
         }

         Iterator<Entry<Integer, ArbiterChainRenderer.Seen>> it = SEEN.entrySet().iterator();

         while (it.hasNext()) {
            ArbiterChainRenderer.Seen s = it.next().getValue();
            if (!s.seenThisFrame) {
               it.remove();
               if (s.kind != ArbiterChainRenderer.Kind.ROOT) {
                  Entity owner = mc.level.getEntity(s.ownerId);
                  Vec3 end = new Vec3(s.ex, s.ey, s.ez);
                  Vec3 anchor = owner instanceof ArbiterEntity boss ? anchor(s, boss, pt, end) : end.add(0.0, 2.0, 0.0);
                  GHOSTS.add(new ArbiterChainRenderer.Ghost(s, now, s.kind == ArbiterChainRenderer.Kind.SEAL, anchor));
               }
            }
         }

         ghosts(mc, now, pt);
         TRAILS.values().removeIf(t -> !t.seenThisFrame);
         COURTS.values().removeIf(c -> now - c.lastSeen > 60L);
         if (demo != null) {
            if (now - demo.born > (long)demo.ticks) {
               demo = null;
            } else {
               demo(now, pt);
            }
         }

         PoseStack ps = event.getPoseStack();
         BufferSource buffers = mc.renderBuffers().bufferSource();
         ChainLinks.draw(ps, buffers, cam, FRAME, time);
         trails(ps, buffers, cam);
      }
   }

   private static void court(ArbiterEntity boss, long now, float pt) {
      ArbiterChainRenderer.Court c = COURTS.computeIfAbsent(boss.getId(), id -> new ArbiterChainRenderer.Court());
      c.lastSeen = now;
      byte phase = boss.getPhase();
      boolean kneeling = boss.isKneeling();
      byte cue = boss.guardCue();
      if (!c.primed) {
         c.primed = true;
         c.phase = phase;
         c.phaseSince = now - 100L;
         c.kneeling = kneeling;
         c.cue = cue;
         if (kneeling) {
            c.kneelSince = now - 100L;
            pinSnapshot(boss, c, now - 100L, pt);
         }
      }

      if (phase != c.phase) {
         if (c.phase == 6 || c.phase == 8) {
            windupGhost(boss, now, pt);
         }

         c.prevPhase = c.phase;
         c.phase = phase;
         c.phaseSince = now;
      }

      if (kneeling != c.kneeling) {
         c.kneeling = kneeling;
         if (kneeling) {
            c.kneelSince = now;
            pinSnapshot(boss, c, now, pt);
         } else {
            c.riseSince = now;
         }
      }

      if (cue != c.cue) {
         c.cue = cue;
         if ((float)(now - c.cueSince) >= c.cueSpan) {
            c.cueSince = now;
         }
      }

      if (phase == 6 && !kneeling) {
         windup(boss, c, now, pt, 1, 2.5F, 12.0F);
      }

      if (phase == 8 && !kneeling) {
         windup(boss, c, now, pt, 2, 3.4F, 24.0F);
      }

      if (kneeling) {
         kneelPins(boss, c, now, pt, false);
         kneelSpill(boss, c, now, pt, false);
      } else if ((float)(now - c.riseSince) < 10.0F) {
         kneelPins(boss, c, now, pt, true);
         kneelSpill(boss, c, now, pt, true);
      }
   }

   private static void windup(ArbiterEntity boss, ArbiterChainRenderer.Court c, long now, float pt, int head, float drop, float grow) {
      float age = (float)(now - c.phaseSince) + pt;
      Vec3 look = boss.getViewVector(pt);
      Vec3 flat = new Vec3(look.x, 0.0, look.z);
      flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
      Vec3 from = ArbiterAnchors.scepterHand(boss, pt);
      Vec3 to = from.add(flat.scale(0.9)).add(0.0, (double)(-drop), 0.0);
      ChainLinks.Chain ch = next().from(from).to(to);
      ch.thickness = 1.3F;
      ch.head = head;
      ch.seed = boss.getId() * 7 + 3;
      ch.sag = 0.35F;
      ch.sway = 0.2F;
      ch.growth = AnimEnvelopes.easeOutCubic(ChainLinks.span(age, 0.0F, grow));
      ch.charge = 0.3F + 0.6F * ChainLinks.span(age, 0.0F, 22.0F);
      ch.brightness = light(to);
      remember(SEEN.computeIfAbsent(windupKey(boss), id -> new ArbiterChainRenderer.Seen()), ArbiterChainRenderer.Kind.WINDUP, boss.getId(), to, ch);
   }

   private static void windupGhost(ArbiterEntity boss, long now, float pt) {
      ArbiterChainRenderer.Seen s = SEEN.remove(windupKey(boss));
      if (s != null) {
         GHOSTS.add(new ArbiterChainRenderer.Ghost(s, now, false, ArbiterAnchors.scepterHand(boss, pt)));
      }
   }

   private static int windupKey(ArbiterEntity boss) {
      return -boss.getId() - 1;
   }

   private static void pinSnapshot(ArbiterEntity boss, ArbiterChainRenderer.Court c, long now, float pt) {
      c.kneelX = boss.getX();
      c.kneelY = boss.getY();
      c.kneelZ = boss.getZ();
      c.kneelClock = now;
      c.spillDir = ArbiterSealEntity.orbitDirection(boss.getEscalation());
      c.kneelYaw = ArbiterAnchors.pinnedYawOrBody(boss, pt);

      for (int i = 0; i < KNEEL_CHAINS; i++) {
         double bearing = (double)c.kneelYaw + PINS[i][4] + 18.0 * ((double)ChainLinks.noise(boss.getId(), i, 41) - 0.5);
         double r = PINS[i][3] + 0.5 * ((double)ChainLinks.noise(boss.getId(), i, 42) - 0.5);
         double rad = Math.toRadians(bearing);
         c.stakes[i * 3] = c.kneelX - Math.sin(rad) * r;
         c.stakes[i * 3 + 1] = c.kneelY + 0.16;
         c.stakes[i * 3 + 2] = c.kneelZ + Math.cos(rad) * r;
      }
   }

   private static void kneelPins(ArbiterEntity boss, ArbiterChainRenderer.Court c, long now, float pt, boolean rising) {
      float kneelAge = (float)(now - c.kneelSince) + pt;
      float riseAge = (float)(now - c.riseSince) + pt;
      float shatter = rising ? ChainLinks.span(riseAge, 0.0F, 10.0F) : 0.0F;
      float snap = rising ? 1.0F - ChainLinks.span(riseAge, 0.0F, 4.0F) : 0.0F;
      float stir = ChainLinks.span(kneelAge, 140.0F, 180.0F);
      float dead = boss.deathTime > 0 ? ChainLinks.span((float)boss.deathTime + pt, 50.0F, 90.0F) : 0.0F;
      Vec3 feet = boss.getPosition(pt);

      for (int i = 0; i < KNEEL_CHAINS; i++) {
         Vec3 body = pinBody(boss, c, feet, pt, i);
         double sx = c.stakes[i * 3];
         double sy = c.stakes[i * 3 + 1];
         double sz = c.stakes[i * 3 + 2];
         float start = (float)i * 1.1F;
         float drive = ChainLinks.span(kneelAge, start, start + 5.0F);
         float fall = drive * drive;
         float since = kneelAge - (start + 5.0F);
         float bite = since >= 0.0F ? 1.0F - ChainLinks.span(since, 0.0F, 6.0F) : 0.0F;
         Vec3 stake = new Vec3(sx, Mth.lerp((double)fall, sy + 1.7, sy), sz);
         ChainLinks.Chain ch = next().from(body).to(stake);
         ch.seed = boss.getId() * 13 + i;
         ch.thickness = 1.15F;
         ch.head = 3;
         ch.growth = AnimEnvelopes.easeOutCubic(ChainLinks.span(kneelAge, start, start + 4.0F));
         ch.sag = Mth.lerp(dead, Mth.lerp(fall, 0.4F, 0.055F), 0.42F);
         ch.settle = ChainLinks.settle(since) * 0.5F;
         ch.tremble = drive * (0.018F + 0.05F * stir + 0.05F * snap) * (1.0F - dead);
         ch.charge = Mth.clamp(drive * (0.12F + 0.55F * bite + 0.45F * stir) + 0.7F * snap, 0.0F, 1.0F) * (1.0F - dead);
         ch.flash = Math.max(bite, snap);
         ch.shatter = shatter;
         ch.brightness = light(stake);
      }
   }

   private static void kneelSpill(ArbiterEntity boss, ArbiterChainRenderer.Court c, long now, float pt, boolean rising) {
      float kneelAge = (float)(now - c.kneelSince) + pt;
      float shatter = rising ? ChainLinks.span((float)(now - c.riseSince) + pt, 0.0F, 10.0F) : 0.0F;
      float dead = boss.deathTime > 0 ? ChainLinks.span((float)boss.deathTime + pt, 50.0F, 90.0F) : 0.0F;
      Vec3 yoke = yokeAt(boss, pt);
      double floorY = c.kneelY + 0.14;

      for (int i = 0; i < 6; i++) {
         double angle = ArbiterSealEntity.orbitAngle(i, c.kneelClock, c.spillDir);
         double dx = Math.cos(angle);
         double dz = Math.sin(angle);
         float start = (float)i * 1.5F;
         float fall = AnimEnvelopes.easeOutCubic(ChainLinks.span(kneelAge, start, start + 12.0F));
         float since = kneelAge - (start + 12.0F);
         float ember = (0.45F * (1.0F - ChainLinks.span(kneelAge, start, start + 24.0F)) + 0.1F) * (1.0F - dead) * fall;
         double reach = ArbiterSealEntity.orbitRadius(i);
         Vec3 anchor = new Vec3(c.kneelX + dx * 1.5, floorY, c.kneelZ + dz * 1.5);
         Vec3 far = new Vec3(c.kneelX + dx * reach, floorY, c.kneelZ + dz * reach);
         ChainLinks.Chain drop = next().from(yoke).to(anchor);
         drop.seed = boss.getId() * 23 + i;
         drop.sag = floorSafeSag(yoke, anchor, Mth.lerp(fall, 0.1F, 0.34F), floorY);
         drop.growth = fall;
         drop.charge = ember;
         drop.shatter = shatter;
         drop.brightness = light(anchor);
         ChainLinks.Chain run = next().from(anchor).to(far);
         run.seed = boss.getId() * 23 + i + 101;
         run.sag = 0.05F;
         run.waver = 0.05F;
         run.growth = fall;
         run.settle = ChainLinks.settle(since) * 0.35F;
         run.charge = ember * 0.8F;
         run.shatter = shatter;
         run.brightness = light(far);
      }
   }

   private static Vec3 pinBody(ArbiterEntity boss, ArbiterChainRenderer.Court c, Vec3 feet, float pt, int i) {
      double[] pin = PINS[i];

      return switch (PIN_ANCHORS[i]) {
         case YOKE -> bodyPoint(yokeAt(boss, pt), c.kneelYaw, pin[0], pin[1], pin[2]);
         case RIGHT_HAND -> ArbiterAnchors.hand(boss, pt, true);
         case LEFT_HAND -> ArbiterAnchors.hand(boss, pt, false);
         case SILHOUETTE -> bodyPoint(feet, c.kneelYaw, pin[0], pin[1], pin[2]);
      };
   }

   private static Vec3 bodyPoint(Vec3 origin, float yawDeg, double up, double fwd, double side) {
      double rad = Math.toRadians((double)yawDeg);
      double fx = -Math.sin(rad);
      double fz = Math.cos(rad);
      return origin.add(fx * fwd - fz * side, up, fz * fwd + fx * side);
   }

   private static void seal(ArbiterSealEntity seal, long now, float pt) {
      ArbiterEntity owner = seal.owner();
      if (owner != null && owner.isAlive()) {
         ArbiterChainRenderer.Court c = COURTS.get(owner.getId());
         Vec3 end = seal.getPosition(pt).add(0.0, (double)seal.getBbHeight() * 0.5, 0.0);
         Vec3 from = yoke(owner, pt, end);
         int idx = seal.orbitIndex();
         float age = (float)seal.tickCount + pt;
         float start = (float)idx * 1.5F;
         double floor = owner.getY() + 1.0;
         float slack = floorSafeSag(from, end, (float)from.distanceTo(end) * 0.073275864F, floor);
         ChainLinks.Chain ch = next().from(from).to(end);
         ch.seed = seal.getId();
         ch.sag = slack;
         ch.sway = 0.1F;
         ch.charge = 0.22F;
         ch.growth = AnimEnvelopes.easeOutCubic(ChainLinks.span(age, start, start + 10.0F));
         ch.settle = ChainLinks.settle(age - (start + 10.0F));
         if (c != null) {
            if (c.phase != 0) {
               ch.charge += 0.12F;
               ch.sag = slack * 0.55F;
            }

            float travel = Math.max(1.0F, (float)from.distanceTo(end) / 0.6F);
            float cueEnd = travel + 4.0F;
            c.cueSpan = Math.max(c.cueSpan, cueEnd);
            float cueAge = (float)(now - c.cueSince) + pt;
            if (cueAge >= 0.0F && cueAge < cueEnd) {
               ch.pulse = cueAge / travel;
               ch.pulseWidth = 0.14F;
               ch.charge += 0.25F;
               ch.flash = cueAge < travel - 0.5F
                  ? ChainLinks.span(cueAge, travel - 1.5F, travel - 0.5F)
                  : 1.0F - ChainLinks.span(cueAge, travel - 0.5F, cueEnd);
            }
         }

         ch.brightness = light(end);
         remember(SEEN.computeIfAbsent(seal.getId(), id -> new ArbiterChainRenderer.Seen()), ArbiterChainRenderer.Kind.SEAL, owner.getId(), end, ch);
      }
   }

   private static void whip(ArbiterWhipEntity whip, long now, float pt) {
      ArbiterEntity owner = whip.owner();
      if (owner != null && (owner.isAlive() || whip.chainMode() == 1)) {
         Vec3 end = whip.renderPosition(pt);
         float age = (float)whip.tickCount + pt;
         byte mode = whip.chainMode();
         boolean rightHand = true;
         ChainLinks.Chain ch;
         ArbiterChainRenderer.Kind kind;
         switch (mode) {
            case 1:
               kind = ArbiterChainRenderer.Kind.ROOT;
               ch = next();
               ch.seed = whip.getId();
               rootChain(ch, end);
               float breach = 2.0F;
               float rise = 4.0F;
               float settled = rise + 3.0F;
               float sinkAt = rise + (float)whip.rootStandTicks();
               float ignite = ChainLinks.span(age, breach, rise);
               float cool = ChainLinks.span(age, settled, settled + 18.0F);
               float drain = ChainLinks.span(age, sinkAt, sinkAt + 2.0F);
               ch.charge = (0.3F + 0.7F * ignite) * (1.0F - 0.72F * cool) * (1.0F - 0.85F * drain);
               ch.chargeFront = Mth.lerp(ChainLinks.span(age, breach, breach + 6.0F), 0.51666665F, 1.2F);
               if (age < rise) {
                  ch.pulse = ChainLinks.span(age, 0.0F, rise);
                  ch.pulseWidth = 0.1F;
               } else {
                  float run = (age - rise) / 9.0F;
                  ch.pulse = run - (float)Math.floor((double)run);
                  ch.pulseWidth = 0.09F;
               }

               ch.sag = 0.05F;
               ch.settle = ChainLinks.settle(age - rise);
               ch.flash = age < rise ? ChainLinks.span(age, rise - 1.0F, rise) : 1.0F - ChainLinks.span(age, rise, rise + 5.0F);
               float yank = ChainLinks.span(age, sinkAt - 1.0F, sinkAt) * (1.0F - ChainLinks.span(age, sinkAt + 2.0F, sinkAt + 4.0F));
               float rising = age < rise ? 1.0F : 0.0F;
               ch.tremble = 0.02F + 0.05F * (1.0F - ChainLinks.span(age, settled, settled + 12.0F)) + 0.05F * rising + 0.07F * yank;
               break;
            case 2:
            case 4:
               boolean high = mode == 4;
               kind = high ? ArbiterChainRenderer.Kind.SWEEP_HIGH : ArbiterChainRenderer.Kind.PENDULUM;
               float speed = trail(whip, end, now);
               rightHand = hoistHand(whip, owner, end, pt);
               Vec3 hand = ArbiterAnchors.hand(owner, pt, rightHand);
               float tell = (float)whip.scriptTicks();
               float lit = ChainLinks.span(age, tell - 1.0F, tell + 5.0F);
               float drive = Mth.clamp((speed - 1.2F) / 1.8F, 0.0F, 1.0F);
               float grow = AnimEnvelopes.easeOutCubic(ChainLinks.span(age, 0.0F, 7.0F));
               if (high) {
                  ch = next().from(hand).to(end);
                  ch.thickness = 1.3F;
                  ch.head = 1;
                  ch.growth = grow;
                  ch.sag = Mth.lerp(lit, 0.22F, 0.07F);
                  ch.charge = 0.04F + lit * (0.45F + 0.55F * drive);
                  ch.tremble = lit * (0.035F + 0.05F * drive);
               } else {
                  Vec3 court = owner.getPosition(pt);
                  Vec3 flat = new Vec3(end.x - court.x, 0.0, end.z - court.z);
                  flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
                  Vec3 anchor = new Vec3(court.x, end.y, court.z).add(flat.scale(1.5));
                  ChainLinks.Chain drop = next().from(hand).to(anchor);
                  drop.thickness = 1.3F;
                  drop.seed = whip.getId() * 31 + 7;
                  drop.sag = 0.28F;
                  drop.growth = grow;
                  drop.charge = 0.04F + lit * (0.35F + 0.3F * drive);
                  drop.tremble = 0.035F * lit;
                  drop.brightness = light(anchor);
                  ch = next().from(anchor).to(end);
                  ch.thickness = 1.3F;
                  ch.head = 1;
                  ch.growth = grow;
                  ch.sag = Mth.lerp(lit, 0.3F, 0.04F);
                  ch.charge = 0.04F + lit * (0.45F + 0.55F * drive);
                  ch.tremble = lit * (0.03F + 0.045F * drive);
               }
               break;
            case 3:
               kind = ArbiterChainRenderer.Kind.REEL;
               ch = next().from(ArbiterAnchors.scepterHand(owner, pt)).to(end);
               ch.thickness = 1.2F;
               ch.head = 2;
               float fly = (float)whip.scriptTicks();
               if (age < fly) {
                  ch.growth = Math.max(0.15F, ChainLinks.span(age, 0.0F, fly));
                  ch.charge = 0.6F;
                  ch.sag = 0.06F;
                  ch.tremble = 0.02F;
               } else {
                  ch.charge = 1.0F;
                  ch.sag = 0.05F;
                  ch.tremble = 0.03F;
                  ch.flash = age < fly + 1.0F ? ChainLinks.span(age, fly, fly + 1.0F) : 1.0F - ChainLinks.span(age, fly + 1.0F, fly + 6.0F);
                  float run = (age - fly) / 7.0F;
                  ch.pulse = run - (float)Math.floor((double)run);
                  ch.pulseWidth = 0.12F;
               }
               break;
            default:
               kind = ArbiterChainRenderer.Kind.WHIP;
               ch = next().from(ArbiterAnchors.scepterHand(owner, pt)).to(end);
               ch.thickness = 1.3F;
               ch.head = 1;
               float travel = 6.0F;
               ch.growth = AnimEnvelopes.easeOutCubic(ChainLinks.span(age, 0.0F, 3.0F));
               if (age < travel) {
                  ch.charge = 0.45F + 0.3F * age / travel;
                  ch.sag = 0.12F;
                  ch.tremble = 0.02F;
               } else {
                  ch.charge = 1.0F - 0.65F * ChainLinks.span(age, travel, travel + 14.0F);
                  ch.sag = Mth.lerp(ChainLinks.span(age, travel, travel + 4.0F), 0.12F, 0.3F);
                  ch.settle = ChainLinks.settle(age - travel);
                  ch.tremble = 0.045F * (1.0F - ChainLinks.span(age, travel, travel + 8.0F));
               }
         }

         ch.seed = whip.getId();
         ch.brightness = light(end);
         ArbiterChainRenderer.Seen seen = SEEN.computeIfAbsent(whip.getId(), id -> new ArbiterChainRenderer.Seen());
         seen.rightHand = rightHand;
         remember(seen, kind, owner.getId(), end, ch);
      }
   }

   private static boolean hoistHand(ArbiterWhipEntity whip, ArbiterEntity owner, Vec3 end, float pt) {
      ArbiterChainRenderer.Trail t = TRAILS.computeIfAbsent(whip.getId(), id -> new ArbiterChainRenderer.Trail());
      if (!t.handChosen) {
         t.handChosen = true;
         t.ownerId = owner.getId();
         Vec3 look = owner.getViewVector(pt);
         Vec3 out = end.subtract(owner.getPosition(pt));
         double side = -look.z * out.x + look.x * out.z;
         double ahead = look.x * out.x + look.z * out.z;
         t.rightHand = (Math.abs(side) >= Math.abs(ahead) ? side : ahead) >= 0.0;

         for (ArbiterChainRenderer.Trail other : TRAILS.values()) {
            if (other != t && other.handChosen && other.ownerId == t.ownerId) {
               t.rightHand = !other.rightHand;
               break;
            }
         }
      }

      return t.rightHand;
   }

   private static float trail(ArbiterWhipEntity whip, Vec3 end, long now) {
      ArbiterChainRenderer.Trail t = TRAILS.computeIfAbsent(whip.getId(), id -> new ArbiterChainRenderer.Trail());
      t.seenThisFrame = true;
      t.sample(end.x, end.y, end.z, now);
      return t.speed();
   }

   private static void ghosts(Minecraft mc, long now, float pt) {
      Iterator<ArbiterChainRenderer.Ghost> it = GHOSTS.iterator();

      while (it.hasNext()) {
         ArbiterChainRenderer.Ghost g = it.next();
         float age = (float)(now - g.born) + pt;
         ArbiterChainRenderer.Seen s = g.seen;
         float life = g.shatter ? 16.0F : (s.kind != ArbiterChainRenderer.Kind.PENDULUM && s.kind != ArbiterChainRenderer.Kind.SWEEP_HIGH ? 5.0F : 10.0F);
         if (age >= life) {
            it.remove();
         } else {
            Vec3 end = new Vec3(s.ex, s.ey, s.ez);
            Entity ch = mc.level.getEntity(s.ownerId);
            if (ch instanceof ArbiterEntity) {
               ArbiterEntity boss = (ArbiterEntity)ch;
               if (boss.isAlive()) {
                  Vec3 y = anchor(s, boss, pt, end);
                  g.yx = y.x;
                  g.yy = y.y;
                  g.yz = y.z;
               }
            }

            Vec3 start = s.kind == ArbiterChainRenderer.Kind.PENDULUM ? new Vec3(s.sx, s.sy, s.sz) : new Vec3(g.yx, g.yy, g.yz);
            ChainLinks.Chain chx = next().from(start).to(end);
            chx.thickness = s.thickness;
            chx.seed = s.seed;
            chx.sag = s.sag;
            chx.brightness = light(end);
            if (g.shatter) {
               chx.shatter = age / 16.0F;
               chx.charge = s.charge * (1.0F - chx.shatter) + 0.6F * (1.0F - ChainLinks.span(age, 0.0F, 3.0F));
               chx.head = s.head;
            } else {
               float t = age / life;
               chx.growth = 1.0F - AnimEnvelopes.easeOutCubic(t);
               chx.charge = s.charge * 0.6F * (1.0F - t);
            }
         }
      }
   }

   private static float floorSafeSag(Vec3 from, Vec3 to, float wanted, double minY) {
      if (!(from.y < minY) && !(to.y < minY)) {
         double dy = to.y - from.y;
         double quarter = Math.abs(dy) * 0.25;
         double a = from.y + dy * 0.5 - minY;
         double disc = a * a - dy * dy * 0.25;
         double max = Math.max(quarter, disc <= 0.0 ? quarter : 0.5 * (a + Math.sqrt(disc)));
         return (float)Math.min((double)wanted, max / 1.5);
      } else {
         return wanted;
      }
   }

   private static void rootChain(ChainLinks.Chain ch, Vec3 tip) {
      double lean = (double)ChainLinks.noise(ch.seed, 0, 7) * Math.PI * 2.0;
      ch.from(tip.add(Math.cos(lean) * 0.32, -6.0, Math.sin(lean) * 0.32)).to(tip);
      ch.thickness = 1.45F;
      ch.head = 3;
      ch.waver = 0.03F;
   }

   private static void demo(long now, float pt) {
      ArbiterChainRenderer.Demo d = demo;
      float age = (float)(now - d.born) + pt;
      Vec3 o = d.origin;
      Vec3 f = d.fwd;
      Vec3 r = d.right;
      float lightHere = light(o);

      for (int i = 0; i < 8; i++) {
         double side = ((double)i - 3.5) * 1.6;
         Vec3 top = o.add(f.scale(4.0)).add(r.scale(side)).add(0.0, 3.2, 0.0);
         Vec3 bottom = o.add(f.scale(7.0)).add(r.scale(side * 1.25)).add(0.0, 0.6, 0.0);
         ChainLinks.Chain ch = next().from(top).to(bottom);
         ch.seed = 1000 + i;
         ch.brightness = lightHere;
         switch (i) {
            case 0:
               ch.sag = (float)top.distanceTo(bottom) * 0.073275864F;
               ch.sway = 0.12F;
               ch.charge = 0.22F;
               break;
            case 1:
               ch.thickness = 1.3F;
               ch.head = 1;
               ch.sag = 0.04F;
               ch.tremble = 0.03F;
               ch.charge = 0.4F;
               break;
            case 2:
               ch.thickness = 1.3F;
               ch.head = 1;
               ch.sag = 0.1F;
               ch.charge = 1.0F;
               ch.tremble = 0.02F;
               break;
            case 3: {
               ch.sag = 0.25F;
               ch.charge = 0.35F;
               float run = age / 14.0F;
               ch.pulse = run - (float)Math.floor((double)run);
               ch.pulseWidth = 0.12F;
               ch.flash = ch.pulse > 0.85F ? (ch.pulse - 0.85F) / 0.15F : Math.max(0.0F, 1.0F - ch.pulse / 0.25F);
               break;
            }
            case 4:
               float cyclex = age % 60.0F;
               ch.sag = 0.4F;
               ch.charge = 0.5F;
               ch.growth = AnimEnvelopes.easeOutCubic(ChainLinks.span(cyclex, 0.0F, 12.0F));
               ch.settle = ChainLinks.settle(cyclex - 12.0F);
               ch.shatter = ChainLinks.span(cyclex, 40.0F, 50.0F);
               if (cyclex >= 50.0F) {
                  ch.growth = 0.0F;
               }
               break;
            case 5: {
               ch.thickness = 1.2F;
               ch.head = 2;
               ch.sag = 0.05F;
               ch.charge = 1.0F;
               ch.tremble = 0.03F;
               float run = age / 7.0F;
               ch.pulse = run - (float)Math.floor((double)run);
               break;
            }
            case 6:
               float cycle = age % 56.0F;
               float rise = 4.0F;
               float breach = 2.0F;
               float settled = rise + 3.0F;
               float sinkAt = rise + 24.0F;
               float sinkEnd = sinkAt + 6.0F;
               double height;
               if (cycle < rise) {
                  height = Mth.lerp((double)AnimEnvelopes.easeOutCubic(cycle / rise), -2.2, 3.35);
               } else if (cycle < settled) {
                  height = Mth.lerp((double)AnimEnvelopes.easeOutCubic((cycle - rise) / (settled - rise)), 3.35, 2.9);
               } else if (cycle < sinkAt) {
                  height = 2.9;
               } else if (cycle < sinkEnd) {
                  float k = (cycle - sinkAt) / (sinkEnd - sinkAt);
                  height = Mth.lerp((double)(k * k), 2.9, -2.2);
               } else {
                  ch.growth = 0.0F;
                  height = -2.2;
               }

               Vec3 tip = o.add(f.scale(6.0)).add(r.scale(side)).add(0.0, height, 0.0);
               rootChain(ch, tip);
               float ignite = ChainLinks.span(cycle, breach, rise);
               float cool = ChainLinks.span(cycle, settled, settled + 18.0F);
               float drain = ChainLinks.span(cycle, sinkAt, sinkAt + 2.0F);
               ch.charge = (0.3F + 0.7F * ignite) * (1.0F - 0.72F * cool) * (1.0F - 0.85F * drain);
               ch.chargeFront = Mth.lerp(ChainLinks.span(cycle, breach, breach + 6.0F), 0.51666665F, 1.2F);
               if (cycle < rise) {
                  ch.pulse = ChainLinks.span(cycle, 0.0F, rise);
                  ch.pulseWidth = 0.1F;
               } else {
                  float runx = (cycle - rise) / 9.0F;
                  ch.pulse = runx - (float)Math.floor((double)runx);
                  ch.pulseWidth = 0.09F;
               }

               ch.sag = 0.05F;
               ch.settle = ChainLinks.settle(cycle - rise);
               ch.flash = cycle < rise ? ChainLinks.span(cycle, rise - 1.0F, rise) : 1.0F - ChainLinks.span(cycle, rise, rise + 5.0F);
               float yank = ChainLinks.span(cycle, sinkAt - 1.0F, sinkAt) * (1.0F - ChainLinks.span(cycle, sinkAt + 2.0F, sinkAt + 4.0F));
               ch.tremble = 0.02F + 0.05F * (1.0F - ChainLinks.span(cycle, settled, settled + 12.0F)) + 0.05F * (cycle < rise ? 1.0F : 0.0F) + 0.07F * yank;
               break;
            default: {
               Vec3 near = o.add(f.scale(3.5)).add(r.scale(side)).add(0.0, 1.5, 0.0);
               Vec3 far = o.add(f.scale(8.5)).add(r.scale(side)).add(0.0, 1.5, 0.0);
               ch.from(near).to(far);
               ch.thickness = 1.3F;
               ch.head = 1;
               ch.sag = 0.06F;
               ch.charge = 1.0F;
               ch.tremble = 0.02F;
               float run = age / 12.0F;
               ch.pulse = run - (float)Math.floor((double)run);
               ch.pulseWidth = 0.1F;
            }
         }
      }
   }

   private static void trails(PoseStack ps, BufferSource buffers, Vec3 cam) {
      if (!TRAILS.isEmpty()) {
         ps.pushPose();
         ps.translate(-cam.x, -cam.y, -cam.z);
         Pose pose = ps.last();
         VertexConsumer body = FxLayers.begin(buffers, FxLayer.PAINT);

         for (ArbiterChainRenderer.Trail t : TRAILS.values()) {
            t.ribbon(pose, body, 0.26F, 70, 6171526);
         }

         FxLayers.end(buffers, FxLayer.PAINT);
         VertexConsumer glow = FxLayers.begin(buffers, FxLayer.LIGHT);

         for (ArbiterChainRenderer.Trail t : TRAILS.values()) {
            t.ribbon(pose, glow, 0.26F, 130, 11544319);
         }

         FxLayers.end(buffers, FxLayer.LIGHT);
         ps.popPose();
      }
   }

   private static ChainLinks.Chain next() {
      int i = FRAME.size();
      if (i == POOL.size()) {
         POOL.add(new ChainLinks.Chain());
      }

      ChainLinks.Chain c = POOL.get(i).reset();
      FRAME.add(c);
      return c;
   }

   private static void remember(ArbiterChainRenderer.Seen s, ArbiterChainRenderer.Kind kind, int ownerId, Vec3 end, ChainLinks.Chain ch) {
      s.kind = kind;
      s.ownerId = ownerId;
      s.sx = ch.ax;
      s.sy = ch.ay;
      s.sz = ch.az;
      s.ex = end.x;
      s.ey = end.y;
      s.ez = end.z;
      s.thickness = ch.thickness;
      s.head = ch.head;
      s.seed = ch.seed;
      s.sag = ch.sag;
      s.charge = ch.charge;
      s.seenThisFrame = true;
   }

   private static Vec3 anchor(ArbiterChainRenderer.Seen s, ArbiterEntity boss, float pt, Vec3 end) {
      return switch (s.kind) {
         case SEAL -> yoke(boss, pt, end);
         default -> ArbiterAnchors.hand(boss, pt, s.rightHand);
      };
   }

   private static Vec3 yoke(ArbiterEntity boss, float pt, Vec3 end) {
      Vec3 centre = yokeAt(boss, pt);
      Vec3 toEnd = end.subtract(centre);
      Vec3 flat = new Vec3(toEnd.x, 0.0, toEnd.z);
      Vec3 out = flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : flat.normalize().scale(0.55);
      return centre.add(out);
   }

   private static Vec3 yokeAt(ArbiterEntity boss, float pt) {
      return ArbiterAnchors.yoke(boss, pt);
   }

   private static float light(Vec3 at) {
      Minecraft mc = Minecraft.getInstance();
      return (float)mc.level.getMaxLocalRawBrightness(BlockPos.containing(at)) / 15.0F;
   }

   private static final class Court {
      byte phase;
      byte prevPhase;
      long phaseSince;
      boolean kneeling;
      long kneelSince = -4611686018427387904L;
      long riseSince = -4611686018427387904L;
      byte cue;
      long cueSince = -4611686018427387904L;
      float cueSpan;
      long lastSeen;
      boolean primed;
      double kneelX;
      double kneelY;
      double kneelZ;
      float kneelYaw;
      final double[] stakes = new double[ArbiterChainRenderer.KNEEL_CHAINS * 3];
      long kneelClock;
      int spillDir = 1;
   }

   private static final class Demo {
      final Vec3 origin;
      final Vec3 fwd;
      final Vec3 right;
      final long born;
      final int ticks;

      Demo(Vec3 origin, Vec3 fwd, long born, int ticks) {
         this.origin = origin;
         this.fwd = fwd;
         this.right = new Vec3(-fwd.z, 0.0, fwd.x);
         this.born = born;
         this.ticks = ticks;
      }
   }

   private static final class Ghost {
      final ArbiterChainRenderer.Seen seen;
      final long born;
      final boolean shatter;
      double yx;
      double yy;
      double yz;

      Ghost(ArbiterChainRenderer.Seen seen, long born, boolean shatter, Vec3 yoke) {
         this.seen = seen;
         this.born = born;
         this.shatter = shatter;
         this.yx = yoke.x;
         this.yy = yoke.y;
         this.yz = yoke.z;
      }
   }

   private static enum Kind {
      SEAL,
      WHIP,
      PENDULUM,
      SWEEP_HIGH,
      REEL,
      ROOT,
      WINDUP;
   }

   private static enum PinAnchor {
      YOKE,
      RIGHT_HAND,
      LEFT_HAND,
      SILHOUETTE;
   }

   private static final class Seen {
      ArbiterChainRenderer.Kind kind;
      int ownerId;
      boolean rightHand = true;
      double sx;
      double sy;
      double sz;
      double ex;
      double ey;
      double ez;
      float thickness;
      int head;
      int seed;
      float sag;
      float charge;
      boolean seenThisFrame;
   }

   private static final class Trail extends ChainLinks.Trail {
      boolean seenThisFrame;
      boolean rightHand;
      boolean handChosen;
      int ownerId;

      Trail() {
         super(10);
      }
   }
}
