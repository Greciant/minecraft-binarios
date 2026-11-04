package com.awakenedmod.awakened.client.limitless;

import com.awakenedmod.awakened.client.ClientPowerState;
import com.awakenedmod.awakened.client.render.HeadAnchor;
import com.awakenedmod.awakened.client.render.limitless.Ease;
import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.sixeyes.SixEyesCue;
import com.awakenedmod.awakened.client.voidview.VoidView;
import com.awakenedmod.awakened.content.limitless.LimitlessPower;
import com.awakenedmod.awakened.core.PowerSuppression;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

final class SixEyesSight implements SixEyesCue {
   static final SixEyesSight INSTANCE = new SixEyesSight();
   static final double RANGE = 24.0;
   static final int CAP = 32;
   private static final float FADE = 10.0F;
   private static final long SEED = 136405L;
   private boolean on;
   private long onSince;
   private long offSince = -4611686018427387904L;
   @Nullable
   private ClientLevel in;
   private final Map<Integer, Long> firstSeen = new HashMap<>();
   private final List<LivingEntity> found = new ArrayList<>();
   private final List<LivingEntity> seen = new ArrayList<>();
   private float time;
   @Nullable
   private HeadAnchor.Head head;

   private SixEyesSight() {
   }

   private static boolean active(Minecraft mc) {
      LocalPlayer p = mc.player;
      return p != null
         && "limitless".equals(ClientPowerState.get().powerId())
         && LimitlessPower.SIX_EYES.enabled(p)
         && !PowerSuppression.isSuppressed(p)
         && !p.isSpectator();
   }

   void tick(Minecraft mc) {
      ClientLevel level = mc.level;
      if (level != null && mc.player != null) {
         if (level != this.in) {
            this.in = level;
            this.firstSeen.clear();
            this.on = false;
            this.offSince = -4611686018427387904L;
         }

         long now = level.getGameTime();
         boolean active = active(mc);
         if (active && !this.on) {
            this.onSince = now;
         } else if (!active && this.on) {
            this.offSince = now;
         }

         this.on = active;
         this.found.clear();
         if (!this.visible(now)) {
            this.firstSeen.clear();
         } else {
            LocalPlayer self = mc.player;
            AABB box = self.getBoundingBox().inflate(24.0);
            Vec3 cam = VoidView.cameraPos();

            for (LivingEntity e : level.getEntitiesOfClass(
               LivingEntity.class,
               box,
               ex -> ex != self && ex.isAlive() && !ex.isSpectator() && !(ex instanceof ArmorStand) && ex.distanceToSqr(self) <= 576.0 && !walledOff(cam, ex)
            )) {
               this.found.add(e);
            }

            this.found.sort(Comparator.comparingDouble(ex -> ex.distanceToSqr(self)));
            if (this.found.size() > 32) {
               this.found.subList(32, this.found.size()).clear();
            }

            Map<Integer, Long> keep = new HashMap<>();

            for (LivingEntity e : this.found) {
               keep.put(e.getId(), this.firstSeen.getOrDefault(e.getId(), now));
            }

            this.firstSeen.clear();
            this.firstSeen.putAll(keep);
         }
      } else {
         this.on = false;
         this.found.clear();
         this.firstSeen.clear();
      }
   }

   private static boolean walledOff(Vec3 cam, LivingEntity body) {
      return VoidView.separated(cam, body.getX(), body.getY(0.5), body.getZ());
   }

   void reset() {
      this.in = null;
      this.on = false;
      this.offSince = -4611686018427387904L;
      this.firstSeen.clear();
      this.found.clear();
      this.seen.clear();
      this.head = null;
   }

   private boolean visible(long now) {
      return this.on || (float)(now - this.offSince) < 10.0F;
   }

   boolean prepare(LimitlessFrame f) {
      if (f.level == this.in && this.visible(f.gameTime) && f.mc.player != null) {
         this.seen.clear();
         Vector3f rel = new Vector3f();

         for (LivingEntity e : this.found) {
            if (e.isAlive() && e.level() == f.level && !walledOff(f.cam, e)) {
               Vec3 c = e.getBoundingBox().getCenter();
               rel.set((float)(c.x - f.cam.x), (float)(c.y - f.cam.y), (float)(c.z - f.cam.z));
               if (!(rel.dot(f.camForward) < -(e.getBbWidth() + e.getBbHeight()))) {
                  this.seen.add(e);
               }
            }
         }

         LocalPlayer self = f.mc.player;
         boolean thirdPerson = f.mc.options.getCameraType() != CameraType.FIRST_PERSON;
         this.head = this.on && thirdPerson && !self.isInvisible() ? HeadAnchor.of(self, f.partialTick) : null;
         this.time = f.since(this.onSince);
         return true;
      } else {
         return false;
      }
   }

   float age() {
      return Math.max(0.0F, this.time);
   }

   @Override
   public List<LivingEntity> seen() {
      return this.seen;
   }

   @Override
   public float presence(LivingEntity body) {
      ClientLevel level = this.in;
      if (level == null) {
         return 0.0F;
      } else {
         long now = level.getGameTime();
         Long first = this.firstSeen.get(body.getId());
         float fadeIn = first == null ? 1.0F : Ease.ramp((float)(now - first), 0.0F, 10.0F);
         float fadeOut = this.on ? 1.0F : 1.0F - Ease.ramp((float)(now - this.offSince), 0.0F, 10.0F);
         return fadeIn * fadeOut;
      }
   }

   @Nullable
   @Override
   public HeadAnchor.Head head() {
      return this.head;
   }

   @Override
   public long seed() {
      return 136405L;
   }
}
