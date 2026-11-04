package com.awakenedmod.awakened.client.limitless.domain;

import com.awakenedmod.awakened.client.AwakenedAnimations;
import com.awakenedmod.awakened.client.render.limitless.Ease;
import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.voidlook.VoidAura;
import com.awakenedmod.awakened.client.render.limitless.voidlook.VoidCue;
import com.awakenedmod.awakened.client.render.limitless.voidlook.VoidLookBase;
import com.awakenedmod.awakened.client.voidview.VoidView;
import com.awakenedmod.awakened.content.limitless.domain.VoidAnchorEntity;
import com.awakenedmod.awakened.content.limitless.domain.VoidBody;
import com.awakenedmod.awakened.content.limitless.domain.VoidTimings;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.PhaseState;
import com.awakenedmod.awakened.core.voidview.VoidZones;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.RenderFrameEvent.Pre;

public final class VoidClient {
   private static final Set<Integer> ANCHORS = new HashSet<>();
   private static final Map<Integer, List<Entity>> CAUGHT = new HashMap<>();
   private static final Set<Integer> OPEN = new HashSet<>();
   private static ClientLevel level;
   public static final String FLOAT_CLIP = "limitless_void_float";

   private VoidClient() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(VoidClient::onTick);
      gameBus.addListener(VoidClient::onFrame);
      gameBus.addListener(VoidClient::onInput);
      gameBus.addListener(VoidClient::onLoggingOut);
      PhaseState.registerClientFullSource(p -> VoidBody.of(p).member());
      AwakenedAnimations.holdLoop(AwId.of("limitless_void_float"), p -> {
         VoidBody b = VoidBody.of(p);
         return b.member() && !b.held();
      });
      AwakenedAnimations.addPoseModifier(VoidFloatPose::bone);
      VoidIntro.register(gameBus);
      VoidMuffle.register();
   }

   static Set<Integer> anchors() {
      return ANCHORS;
   }

   @Nullable
   static VoidAnchorEntity anchor(int id) {
      Minecraft mc = Minecraft.getInstance();
      return mc.level != null && mc.level.getEntity(id) instanceof VoidAnchorEntity a ? a : null;
   }

   private static void onTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != level) {
         forgetAll();
         level = mc.level;
      }

      VoidFloatPose.tick(mc);
      ANCHORS.clear();
      CAUGHT.clear();
      if (mc.level != null) {
         for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof VoidAnchorEntity) {
               ANCHORS.add(e.getId());
            }
         }

         if (ANCHORS.isEmpty()) {
            reconcile(mc, 0.0F);
         } else {
            for (Entity ex : mc.level.entitiesForRendering()) {
               VoidBody body = VoidBody.of(ex);
               if (body.held() && ANCHORS.contains(body.anchor())) {
                  CAUGHT.computeIfAbsent(body.anchor(), k -> new ArrayList<>()).add(ex);
               }
            }

            reconcile(mc, 0.0F);
         }
      }
   }

   private static void onFrame(Pre event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && (!ANCHORS.isEmpty() || !OPEN.isEmpty())) {
         reconcile(mc, event.getPartialTick().getGameTimeDeltaPartialTick(false));
      }
   }

   private static void reconcile(Minecraft mc, float partialTick) {
      Set<Integer> seen = new HashSet<>();

      for (int id : ANCHORS) {
         VoidAnchorEntity a = anchor(id);
         if (a != null) {
            float t = a.since(partialTick);
            boolean view = a.viewUp(t);
            boolean zone = a.zoneUp(t);
            if (view) {
               VoidView.open(id, a.position(), (double)Math.max(0.01F, a.radiusAt(t)), false, (double)a.radius());
            } else {
               VoidView.close(id);
            }

            if (zone) {
               VoidZones.put(true, id, new VoidZones.Zone(mc.level.dimension(), a.position(), (double)a.radius()));
            } else {
               VoidZones.remove(true, id);
            }

            if (view || zone) {
               seen.add(id);
            }
         }
      }

      for (int idx : OPEN) {
         if (!seen.contains(idx)) {
            VoidView.close(idx);
            VoidZones.remove(true, idx);
         }
      }

      OPEN.clear();
      OPEN.addAll(seen);
   }

   private static void forgetAll() {
      for (int id : OPEN) {
         VoidView.close(id);
      }

      OPEN.clear();
      ANCHORS.clear();
      CAUGHT.clear();
      VoidZones.clearClient();
   }

   private static void onLoggingOut(LoggingOut event) {
      forgetAll();
      level = null;
   }

   private static void onInput(MovementInputUpdateEvent event) {
      LocalPlayer local = Minecraft.getInstance().player;
      if (local != null && event.getEntity() == local && VoidBody.of(local).held()) {
         Input in = event.getInput();
         in.leftImpulse = 0.0F;
         in.forwardImpulse = 0.0F;
         in.up = false;
         in.down = false;
         in.left = false;
         in.right = false;
         in.jumping = false;
         in.shiftKeyDown = false;
      }
   }

   public static void draw(LimitlessFrame f, VoidAnchorEntity a, VoidLookBase look) {
      float t = a.since(f.partialTick);
      float age = VoidTimings.lookAge(t);
      if (!(age < 0.0F) && !(age > (float)(a.crackAt() - 20 + 36))) {
         final float radius = a.radius();
         final Vec3 centre = a.position();
         final Vec3 forward = a.forward();
         List<Entity> held = CAUGHT.getOrDefault(a.getId(), List.of());
         Entity viewer = f.mc.getCameraEntity();
         final List<Entity> caught = held;
         if (viewer != null && f.mc.options.getCameraType().isFirstPerson() && held.contains(viewer)) {
            caught = new ArrayList<>(held);
            caught.remove(viewer);
         }

         final float crackAt = (float)(a.crackAt() - 20);
         final long seed = a.seed();
         look.draw(f, new VoidCue() {
            @Override
            public Vec3 centre() {
               return centre;
            }

            @Override
            public float radius() {
               return radius;
            }

            @Override
            public Vec3 forward() {
               return forward;
            }

            @Override
            public long seed() {
               return seed;
            }

            @Override
            public Iterable<? extends Entity> caught() {
               return caught;
            }

            @Override
            public float crackAt() {
               return crackAt;
            }
         }, age);
         float sky = Ease.ramp(age, 0.0F, 10.0F) * (1.0F - Ease.ramp(age, crackAt, 24.0F));
         Entity caster = f.level.getEntity(a.casterId());
         if (caster != null && VoidBody.of(caster).anchor() == a.getId()) {
            VoidAura.draw(f, caster, sky);
         }
      }
   }
}
