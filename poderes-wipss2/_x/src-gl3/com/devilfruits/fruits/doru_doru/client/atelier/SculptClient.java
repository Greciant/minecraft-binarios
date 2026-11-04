package com.devilfruits.fruits.doru_doru.client.atelier;

import com.devilfruits.client.ClientFruitState;
import com.devilfruits.client.input.DFKeys;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.fruits.doru_doru.DoruDoru;
import com.devilfruits.fruits.doru_doru.DoruSounds;
import com.devilfruits.fruits.doru_doru.ability.WaxAtelierAbility;
import com.devilfruits.fruits.doru_doru.atelier.AtelierGen;
import com.devilfruits.fruits.doru_doru.atelier.CastSlots;
import com.devilfruits.fruits.doru_doru.atelier.DoruNetwork;
import com.devilfruits.fruits.doru_doru.atelier.SculptRequest;
import com.devilfruits.fruits.doru_doru.atelier.ShapeKind;
import com.devilfruits.fruits.doru_doru.atelier.VoxelSet;
import com.devilfruits.fruits.doru_doru.atelier.WaxGen;
import com.devilfruits.fruits.doru_doru.atelier.WaxIntegrity;
import com.devilfruits.fruits.doru_doru.atelier.WaxViews;
import com.devilfruits.fruits.doru_doru.client.vfx.PourCinematic;
import com.devilfruits.fruits.doru_doru.wax.WaxBlockEntity;
import com.devilfruits.fruits.doru_doru.wax.WaxMask;
import com.devilfruits.fruits.doru_doru.wax.WaxWorld;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.ClientTickEvent.Pre;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SculptClient {
   static SculptRequest.Tool tool = SculptRequest.Tool.SHAPE;
   static ShapeKind kind = ShapeKind.DOME;
   static final float[][] PARAMS = new float[ShapeKind.values().length][4];
   static int param;
   static boolean hollow;
   static float shell = 0.5F;
   static SculptRequest.Symmetry symmetry = SculptRequest.Symmetry.NONE;
   static int folds = 4;
   static Vec3 origin = Vec3.ZERO;
   static int color;
   static SculptClient.Snap snap = SculptClient.Snap.BLOCK;
   static float airDistance = 6.0F;
   static float viscosity;
   static int brushMode = 0;
   static float brushRadius = 1.0F;
   static final List<Vec3> stroke = new ArrayList<>();
   static boolean stroking;
   static int castSlot;
   static boolean castCapture;
   static final float[] CAST = new float[]{0.0F, 0.0F, 2.0F};
   static float dissolveRadius = 2.5F;
   private static int holdCooldown;
   static final List<Vec3> points = new ArrayList<>();
   @Nullable
   static Vec3 target;
   static boolean onSurface;
   static Direction face = Direction.UP;
   @Nullable
   static SculptClient.Ghost ghost;
   private static int ghostKey;
   private static long ghostAt;
   private static boolean altDown;
   private static boolean altUsed;
   @Nullable
   private static Vec3 forcedTarget;

   public static void init() {
      NeoForge.EVENT_BUS.addListener(SculptClient::onTickPre);
      NeoForge.EVENT_BUS.addListener(SculptClient::onTickPost);
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, SculptClient::onScroll);
      NeoForge.EVENT_BUS.addListener(SculptClient::onInteraction);
   }

   public static boolean active() {
      Minecraft mc = Minecraft.getInstance();
      return mc.player != null
         && FormFlags.has(mc.player, DoruDoru.SCULPTING)
         && ClientFruitState.fruit() != null
         && DoruDoru.ID.equals(ClientFruitState.fruit().id());
   }

   static int mastery() {
      return ClientFruitState.data() == null ? 1 : ClientFruitState.data().mastery();
   }

   private static void onTickPre(Pre event) {
      Minecraft mc = Minecraft.getInstance();
      if (!active()) {
         if (!points.isEmpty() || ghost != null || stroking) {
            points.clear();
            stroke.clear();
            stroking = false;
            ghost = null;
         }

         altDown = false;
      } else if (mc.screen == null) {
         while (mc.options.keyAttack.consumeClick()) {
            place();
         }

         while (mc.options.keyUse.consumeClick()) {
            takeBack();
         }

         mc.options.keyAttack.setDown(false);
         mc.options.keyUse.setDown(false);
         boolean held = mc.mouseHandler.isLeftPressed();
         if (stroking) {
            if (held && target != null) {
               Vec3 last = stroke.isEmpty() ? null : stroke.get(stroke.size() - 1);
               if ((last == null || last.distanceTo(target) >= Math.max(0.25, (double)brushRadius * 0.5)) && stroke.size() < 64) {
                  stroke.add(target);
                  ghostAt = 0L;
               }
            } else {
               finishStroke();
            }
         }

         if (holdCooldown > 0) {
            holdCooldown--;
         } else if (tool == SculptRequest.Tool.DISSOLVE && held && target != null) {
            send(request(List.of(target)));
            holdCooldown = 18;
         }

         boolean alt = DFKeys.SELECT_MODIFIER.isDown();

         for (int i = 0; i < Math.min(4, mc.options.keyHotbarSlots.length); i++) {
            while (!alt && mc.options.keyHotbarSlots[i].consumeClick()) {
               setTool(SculptRequest.Tool.values()[i]);
            }
         }

         if (alt && !altDown) {
            altDown = true;
            altUsed = false;
         } else if (!alt && altDown) {
            altDown = false;
            if (!altUsed) {
               snap = SculptClient.Snap.values()[(snap.ordinal() + 1) % SculptClient.Snap.values().length];
               feedback(Component.translatable("hud.devilfruits.doru_doru.snap." + snap.name().toLowerCase(Locale.ROOT)));
            }
         }

         if (alt) {
            for (KeyMapping k : mc.options.keyHotbarSlots) {
               if (k.isDown()) {
                  altUsed = true;
               }
            }
         }

         if (SculptKeys.WHEEL.consumeClick()) {
            mc.setScreen(new SculptWheelScreen());
         } else {
            while (SculptKeys.PARAM.consumeClick()) {
               param = (param + 1) % Math.max(1, paramCount());
               click(1.2F);
            }

            while (SculptKeys.MIRROR.consumeClick()) {
               cycleSymmetry();
            }

            while (SculptKeys.HOLLOW.consumeClick()) {
               hollow = !hollow;
               param = Math.min(param, Math.max(0, paramCount() - 1));
               feedback(Component.translatable(hollow ? "hud.devilfruits.doru_doru.hollow_on" : "hud.devilfruits.doru_doru.hollow_off"));
            }

            while (SculptKeys.VISCOSITY.consumeClick()) {
               viscosity = viscosity >= 0.99F ? 0.0F : Math.min(1.0F, viscosity + 0.25F);
               feedback(Component.translatable("hud.devilfruits.doru_doru.viscosity", new Object[]{Math.round(viscosity * 100.0F)}));
            }

            while (SculptKeys.UNDO.consumeClick()) {
               PacketDistributor.sendToServer(new DoruNetwork.CommandPayload(0), new CustomPacketPayload[0]);
            }

            while (SculptKeys.REDO.consumeClick()) {
               PacketDistributor.sendToServer(new DoruNetwork.CommandPayload(1), new CustomPacketPayload[0]);
            }
         }
      }
   }

   private static void onTickPost(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (active() && mc.player != null && mc.level != null) {
         updateTarget(mc.player);
         refreshGhost(mc.level, false);
      }
   }

   private static void onScroll(MouseScrollingEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (active() && mc.screen == null && event.getScrollDeltaY() != 0.0) {
         if (DFKeys.SELECT_MODIFIER.isDown()) {
            altUsed = true;
         } else {
            int dir = event.getScrollDeltaY() > 0.0 ? 1 : -1;
            if (Screen.hasControlDown()) {
               airDistance = Math.max(2.0F, Math.min((float)WaxAtelierAbility.reach(mastery()), airDistance + (float)dir));
            } else {
               nudge(dir);
            }

            click(1.5F + 0.1F * (float)dir);
            event.setCanceled(true);
         }
      }
   }

   private static void onInteraction(InteractionKeyMappingTriggered event) {
      if (active() && (event.isAttack() || event.isUseItem())) {
         event.setCanceled(true);
         event.setSwingHand(false);
      }
   }

   static List<String> paramNames() {
      return (List<String>)(switch (tool) {
         case SHAPE -> {
            List<String> n = new ArrayList<>();

            for (ShapeKind.Param p : kind.params()) {
               n.add(p.name());
            }

            if (hollow) {
               n.add("shell");
            }

            yield n;
         }
         case BRUSH -> List.of("radius");
         case CAST -> castCapture ? List.of() : List.of("turn", "mirror", "scale");
         case DISSOLVE -> List.of("radius");
      });
   }

   static float paramValue(int i) {
      return switch (tool) {
         case SHAPE -> i < kind.params().length ? PARAMS[kind.ordinal()][i] : shell;
         case BRUSH -> brushRadius;
         case CAST -> i == 2 ? AtelierGen.SCALES[Math.round(CAST[2])] : (i == 0 ? CAST[0] * 90.0F : CAST[1]);
         case DISSOLVE -> dissolveRadius;
      };
   }

   static int paramCount() {
      return paramNames().size();
   }

   static void nudge(int dir) {
      switch (tool) {
         case SHAPE:
            ShapeKind.Param[] ps = kind.params();
            if (param < ps.length) {
               ShapeKind.Param p = ps[param];
               float v = PARAMS[kind.ordinal()][param];
               float step = p.step(dir > 0 ? v : v - 0.001F);
               PARAMS[kind.ordinal()][param] = p.clamp((float)Math.round((v + (float)dir * step) / step) * step);
            } else {
               shell = Math.max(0.25F, Math.min(4.0F, shell + (float)dir * 0.25F));
            }
            break;
         case BRUSH:
            brushRadius = Math.max(0.25F, Math.min(4.0F, brushRadius + (float)dir * 0.25F));
            break;
         case CAST:
            int i = Math.min(param, 2);
            CAST[i] = i == 0
               ? (float)Math.floorMod(Math.round(CAST[0]) + dir, 4)
               : (i == 1 ? 1.0F - CAST[1] : (float)Math.max(0, Math.min(3, Math.round(CAST[2]) + dir)));
            break;
         case DISSOLVE:
            dissolveRadius = Math.max(0.5F, Math.min(8.0F, dissolveRadius + (float)dir * 0.5F));
      }
   }

   static void setTool(SculptRequest.Tool t) {
      tool = t;
      points.clear();
      stroke.clear();
      stroking = false;
      param = 0;
      click(1.3F);
      feedback(Component.translatable("hud.devilfruits.doru_doru.tool." + t.name().toLowerCase(Locale.ROOT)));
   }

   static void setKind(ShapeKind k) {
      tool = SculptRequest.Tool.SHAPE;
      if (kind != k) {
         kind = k;
         points.clear();
         param = 0;
      }
   }

   static void setBrush(int mode) {
      tool = SculptRequest.Tool.BRUSH;
      brushMode = mode;
      param = 0;
   }

   static void setCast(int slot, boolean capture) {
      tool = SculptRequest.Tool.CAST;
      castSlot = slot;
      castCapture = capture;
      points.clear();
      param = 0;
   }

   static void cycleSymmetry() {
      int nextFolds = folds;
      SculptRequest.Symmetry next;
      switch (symmetry) {
         case NONE:
            next = SculptRequest.Symmetry.MIRROR_X;
            break;
         case MIRROR_X:
            next = SculptRequest.Symmetry.MIRROR_Z;
            break;
         case MIRROR_Z:
            next = SculptRequest.Symmetry.MIRROR_XZ;
            break;
         case MIRROR_XZ:
            next = SculptRequest.Symmetry.RADIAL;
            nextFolds = 3;
            break;
         default:
            if (folds < 12) {
               next = SculptRequest.Symmetry.RADIAL;
               nextFolds = folds < 4 ? 4 : (folds < 6 ? 6 : (folds < 8 ? 8 : 12));
            } else {
               next = SculptRequest.Symmetry.NONE;
            }
      }

      if (symmetry == SculptRequest.Symmetry.NONE && target != null) {
         origin = target;
      }

      symmetry = next;
      folds = nextFolds;
      feedback(
         symmetry == SculptRequest.Symmetry.RADIAL
            ? Component.translatable("hud.devilfruits.doru_doru.symmetry.radial", new Object[]{folds})
            : Component.translatable("hud.devilfruits.doru_doru.symmetry." + symmetry.name().toLowerCase(Locale.ROOT))
      );
   }

   static int needed() {
      return switch (tool) {
         case SHAPE -> kind.points();
         case CAST -> castCapture ? 2 : 1;
         default -> 1;
      };
   }

   private static void place() {
      if (target == null) {
         deny(null);
      } else if (tool == SculptRequest.Tool.BRUSH) {
         stroke.clear();
         stroke.add(target);
         stroking = true;
         click(1.1F);
      } else if (tool == SculptRequest.Tool.DISSOLVE) {
         if (holdCooldown == 0) {
            send(request(List.of(target)));
            holdCooldown = 18;
         }
      } else {
         points.add(target);
         if (points.size() < needed()) {
            click(1.0F + 0.15F * (float)points.size());
            ghostAt = 0L;
         } else {
            List<Vec3> pts = new ArrayList<>(points);
            if (tool != SculptRequest.Tool.CAST || !castCapture) {
               ClientLevel level = Minecraft.getInstance().level;
               SculptClient.Ghost g = level == null ? null : compute(level, pts);
               if (g == null || !g.ok()) {
                  points.remove(points.size() - 1);
                  deny(g == null ? (tool == SculptRequest.Tool.CAST ? "no_cast" : "too_big") : g.problem());
                  return;
               }
            }

            send(request(pts));
            points.clear();
         }
      }
   }

   private static void finishStroke() {
      stroking = false;
      if (!stroke.isEmpty()) {
         List<Vec3> pts = new ArrayList<>(stroke);
         stroke.clear();
         ClientLevel level = Minecraft.getInstance().level;
         SculptClient.Ghost g = level == null ? null : compute(level, pts);
         if (g != null && g.ok()) {
            send(request(pts));
         } else {
            deny(g == null ? "too_big" : g.problem());
         }
      }
   }

   private static void send(SculptRequest r) {
      PacketDistributor.sendToServer(new DoruNetwork.SculptPayload(r), new CustomPacketPayload[0]);
      ghost = null;
      SculptGhost.dirty();
      click(0.8F);
   }

   private static void takeBack() {
      if (!points.isEmpty()) {
         points.remove(points.size() - 1);
         ghostAt = 0L;
         click(0.7F);
      }
   }

   private static void updateTarget(LocalPlayer p) {
      if (forcedTarget != null) {
         target = forcedTarget;
         onSurface = true;
         face = Direction.UP;
      } else {
         double reach = WaxAtelierAbility.reach(mastery());
         Vec3 eye = p.getEyePosition();
         Vec3 look = p.getViewVector(1.0F);
         BlockHitResult hit = p.level().clip(new ClipContext(eye, eye.add(look.scale(reach)), Block.OUTLINE, Fluid.NONE, p));
         Vec3 t;
         if (hit.getType() == Type.BLOCK && hit.getLocation().distanceTo(eye) <= reach + 0.5) {
            t = hit.getLocation();
            face = hit.getDirection();
            onSurface = true;
            if (tool == SculptRequest.Tool.DISSOLVE || tool == SculptRequest.Tool.BRUSH && brushMode != 0 || tool == SculptRequest.Tool.CAST && castCapture) {
               t = t.subtract((double)face.getStepX() * 0.2, (double)face.getStepY() * 0.2, (double)face.getStepZ() * 0.2);
               onSurface = false;
            }
         } else {
            t = eye.add(look.scale(Math.min((double)airDistance, reach)));
            face = Direction.UP;
            onSurface = false;
         }

         t = snap(t);
         if (p.isShiftKeyDown() && !points.isEmpty()) {
            Vec3 last = points.get(points.size() - 1);
            Vec3 d = t.subtract(last);
            double ax = Math.abs(d.x);
            double ay = Math.abs(d.y);
            double az = Math.abs(d.z);
            t = ax >= ay && ax >= az ? new Vec3(t.x, last.y, last.z) : (ay >= az ? new Vec3(last.x, t.y, last.z) : new Vec3(last.x, last.y, t.z));
         }

         target = t;
      }
   }

   private static Vec3 snap(Vec3 t) {
      if (snap == SculptClient.Snap.SURFACE) {
         return t;
      } else {
         double g = snap == SculptClient.Snap.SUB ? 0.25 : (snap == SculptClient.Snap.BLOCK ? 1.0 : 2.0);
         boolean centre = snap != SculptClient.Snap.SUB;
         Axis axis = onSurface ? face.getAxis() : null;
         Vec3 q = onSurface ? t.add((double)face.getStepX() * 0.01, (double)face.getStepY() * 0.01, (double)face.getStepZ() * 0.01) : t;
         double x = axis == Axis.X ? t.x : grid(q.x, g, centre);
         double y = axis == Axis.Y ? t.y : (!onSurface && centre ? Math.floor(q.y / g) * g : grid(q.y, g, centre));
         double z = axis == Axis.Z ? t.z : grid(q.z, g, centre);
         return new Vec3(x, y, z);
      }
   }

   private static double grid(double v, double g, boolean centre) {
      return centre ? Math.floor(v / g) * g + g / 2.0 : (double)Math.round(v / g) * g;
   }

   static SculptRequest request(List<Vec3> pts) {
      return switch (tool) {
         case SHAPE -> new SculptRequest(
         tool, kind.ordinal(), pts, (float[])PARAMS[kind.ordinal()].clone(), hollow ? shell : 0.0F, symmetry, folds, origin, color, viscosity
      );
         case BRUSH -> new SculptRequest(tool, brushMode, pts, new float[]{brushRadius, 0.0F, 0.0F, 0.0F}, 0.0F, symmetry, folds, origin, color, viscosity);
         case CAST -> new SculptRequest(
         tool, castSlot, pts, new float[]{CAST[0], CAST[1], CAST[2], castCapture ? 1.0F : 0.0F}, 0.0F, SculptRequest.Symmetry.NONE, folds, origin, color, 0.0F
      );
         case DISSOLVE -> new SculptRequest(
         tool, 0, pts, new float[]{dissolveRadius, 0.0F, 0.0F, 0.0F}, 0.0F, SculptRequest.Symmetry.NONE, folds, origin, color, 0.0F
      );
      };
   }

   static SculptClient.Mode mode() {
      return switch (tool) {
         case SHAPE, CAST -> SculptClient.Mode.ADD;
         case BRUSH -> brushMode == 0
         ? SculptClient.Mode.ADD
         : (brushMode == 1 ? SculptClient.Mode.REMOVE : (brushMode == 2 ? SculptClient.Mode.RESHAPE : SculptClient.Mode.PAINT));
         case DISSOLVE -> SculptClient.Mode.REMOVE;
      };
   }

   @Nullable
   static List<Vec3> ghostPoints() {
      if (tool == SculptRequest.Tool.CAST && castCapture) {
         return null;
      } else {
         List<Vec3> pts = new ArrayList<>(tool == SculptRequest.Tool.BRUSH && stroking ? stroke : points);
         if (target != null && (pts.size() < needed() || stroking)) {
            pts.add(target);
         }

         if (tool == SculptRequest.Tool.SHAPE && kind == ShapeKind.BRIDGE && pts.size() == 2) {
            pts.add(pts.get(0).add(pts.get(1)).scale(0.5));
         }

         return pts.size() >= needed() ? pts : null;
      }
   }

   private static void refreshGhost(ClientLevel level, boolean now) {
      List<Vec3> pts = ghostPoints();
      Minecraft mc = Minecraft.getInstance();
      boolean pouring = mc.player != null && VfxDirector.find(PourCinematic.class, c -> c.entityId() == mc.player.getId()) != null;
      if (pts != null && (!pouring || !points.isEmpty() || stroking)) {
         int key = Objects.hash(
            pts,
            tool,
            kind,
            brushMode,
            castSlot,
            Arrays.hashCode(CAST),
            Arrays.hashCode(PARAMS[kind.ordinal()]),
            brushRadius,
            dissolveRadius,
            hollow,
            shell,
            symmetry,
            folds,
            origin,
            color,
            viscosity,
            mastery()
         );
         long time = System.currentTimeMillis();
         if (now || ghost == null || key != ghostKey && time - ghostAt >= 100L) {
            ghostKey = key;
            ghostAt = time;
            ghost = compute(level, pts);
            SculptGhost.dirty();
         }
      } else {
         if (ghost != null) {
            ghost = null;
            SculptGhost.dirty();
         }
      }
   }

   @Nullable
   static SculptClient.Ghost compute(ClientLevel level, List<Vec3> pts) {
      Minecraft mc = Minecraft.getInstance();
      SculptRequest r = request(pts);
      AtelierGen.Result result = WaxGen.generate(r, WaxGen.context(level, mc.player == null ? null : CastSlots.of(mc.player)));
      if (result == null) {
         return null;
      } else {
         VoxelSet set = result.set();
         SculptClient.Mode mode = mode();
         Long2LongOpenHashMap accepted = new Long2LongOpenHashMap();
         int cells = 0;
         MutableBlockPos m = new MutableBlockPos();
         ObjectIterator mastery = set.blocks().long2LongEntrySet().iterator();

         while (mastery.hasNext()) {
            Entry e = (Entry)mastery.next();
            m.set(e.getLongKey());
            if (level.isLoaded(m) && !level.isOutsideBuildHeight(m)) {
               WaxBlockEntity be = level.getBlockEntity(m) instanceof WaxBlockEntity w ? w : null;
               long c;
               if (mode == SculptClient.Mode.ADD) {
                  c = be != null ? e.getLongValue() & ~be.mask() : (WaxWorld.replaceable(level.getBlockState(m)) ? e.getLongValue() : 0L);
               } else if (mode == SculptClient.Mode.REMOVE) {
                  c = be != null ? e.getLongValue() & be.mask() : 0L;
               } else {
                  c = be != null ? be.mask() & (mode == SculptClient.Mode.PAINT ? -1L : e.getLongValue() | be.mask()) : 0L;
               }

               if (c != 0L) {
                  accepted.put(e.getLongKey(), c);
                  cells += WaxMask.count(c);
               }
            }
         }

         int masteryx = mastery();
         float blocks = (float)cells / 64.0F;
         float cost = mode == SculptClient.Mode.ADD ? (float)((double)blocks * DoruDoru.blockCost(masteryx)) : 0.0F;
         String problem = null;
         double reach = WaxAtelierAbility.reach(masteryx) + 1.5;

         for (Vec3 p : pts) {
            if (mc.player != null && p.distanceTo(mc.player.getEyePosition()) > reach) {
               problem = "too_far";
            }
         }

         if (problem == null && cells == 0) {
            problem = mode == SculptClient.Mode.ADD ? "no_room" : "no_wax";
         }

         if (problem == null && mode == SculptClient.Mode.ADD && (double)blocks > WaxAtelierAbility.jobSize(masteryx) + 0.001) {
            problem = "too_big_mastery";
         }

         WaxIntegrity.Result stress = null;
         int unsupported = 0;
         if (problem == null && mode == SculptClient.Mode.ADD && accepted.size() <= 20000) {
            stress = WaxIntegrity.check(WaxViews.of(level, accepted, null), accepted.keySet(), WaxAtelierAbility.rules());
            unsupported = stress.count(WaxIntegrity.Stress.UNSUPPORTED);
            if (unsupported > 0) {
               problem = "unsupported";
            }
         }

         boolean free = mc.player != null && (mc.player.isCreative() || mc.player.isSpectator());
         if (problem == null && !free && ClientFruitState.data() != null && cost > ClientFruitState.data().energy() + 0.001F) {
            problem = "no_energy";
         }

         return new SculptClient.Ghost(set, accepted, stress, blocks, cost, problem, unsupported, mode);
      }
   }

   public static void debugDesign(ShapeKind k, float[] values, List<Vec3> pts, float hollowShell, SculptRequest.Symmetry sym, int nFolds, Vec3 o, int dye) {
      tool = SculptRequest.Tool.SHAPE;
      kind = k;
      System.arraycopy(values, 0, PARAMS[k.ordinal()], 0, Math.min(values.length, 4));
      hollow = hollowShell > 0.0F;
      shell = hollowShell > 0.0F ? hollowShell : shell;
      symmetry = sym;
      folds = nFolds;
      origin = o;
      color = dye;
      points.clear();
      points.addAll(pts.subList(0, pts.size() - 1));
      forcedTarget = pts.get(pts.size() - 1);
      target = forcedTarget;
      ghostAt = 0L;
      ghostKey = 0;
   }

   public static void debugPour() {
      place();
      forcedTarget = null;
   }

   public static void debugStroke(int mode, float radius, float viscosity, List<Vec3> pts, int dye) {
      tool = SculptRequest.Tool.BRUSH;
      brushMode = mode;
      brushRadius = radius;
      SculptClient.viscosity = viscosity;
      color = dye;
      symmetry = SculptRequest.Symmetry.NONE;
      stroke.clear();
      stroke.addAll(pts);
      stroking = false;
      ClientLevel level = Minecraft.getInstance().level;
      ghost = level == null ? null : compute(level, new ArrayList<>(pts));
      SculptGhost.dirty();
      send(request(new ArrayList<>(pts)));
      stroke.clear();
   }

   public static void debugCast(int slot, Vec3 a, @Nullable Vec3 b, int turns, boolean mirror, int scaleIndex) {
      setCast(slot, b != null);
      CAST[0] = (float)turns;
      CAST[1] = mirror ? 1.0F : 0.0F;
      CAST[2] = (float)scaleIndex;
      send(request(b != null ? List.of(a, b) : List.of(a)));
   }

   public static void debugCastGhost(int slot, Vec3 at, int turns, boolean mirror, int scaleIndex) {
      setCast(slot, false);
      CAST[0] = (float)turns;
      CAST[1] = mirror ? 1.0F : 0.0F;
      CAST[2] = (float)scaleIndex;
      forcedTarget = at;
      target = at;
      ghostAt = 0L;
      ghostKey = 0;
   }

   public static void debugRelease() {
      forcedTarget = null;
      points.clear();
      ghost = null;
      tool = SculptRequest.Tool.SHAPE;
   }

   @Nullable
   public static SculptClient.Ghost debugGhost() {
      return ghost;
   }

   static void click(float pitch) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.player.playSound(DoruSounds.SCULPT_CLICK.get(), 0.5F, pitch);
      }
   }

   static void deny(@Nullable String reason) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.player.playSound(DoruSounds.SCULPT_DENY.get(), 0.6F, 1.0F);
         if (reason != null) {
            mc.player
               .displayClientMessage(
                  Component.translatable("hud.devilfruits.doru_doru.problem." + reason, new Object[]{0}).withStyle(ChatFormatting.GOLD), true
               );
         }
      }
   }

   static void feedback(Component text) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.player.displayClientMessage(text, true);
      }

      click(1.4F);
   }

   private SculptClient() {
   }

   static {
      for (ShapeKind k : ShapeKind.values()) {
         ShapeKind.Param[] ps = k.params();

         for (int i = 0; i < ps.length; i++) {
            PARAMS[k.ordinal()][i] = ps[i].def();
         }
      }
   }

   public static record Ghost(
      VoxelSet set,
      Long2LongOpenHashMap accepted,
      @Nullable WaxIntegrity.Result stress,
      float blocks,
      float cost,
      @Nullable String problem,
      int unsupported,
      SculptClient.Mode mode
   ) {
      boolean ok() {
         return this.problem == null;
      }
   }

   public static enum Mode {
      ADD,
      REMOVE,
      RESHAPE,
      PAINT;
   }

   public static enum Snap {
      SUB,
      BLOCK,
      TWO_BLOCKS,
      SURFACE;
   }
}
