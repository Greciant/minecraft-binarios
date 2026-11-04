package com.devilfruits.client.camera;

import com.devilfruits.client.ClientFruitState;
import com.devilfruits.client.dev.CaptureRig;
import com.devilfruits.client.input.DFKeys;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.network.AimPayload;
import com.devilfruits.core.physics.PhysicsWorld;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ClientTickEvent.Pre;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeFov;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(
   modid = "devilfruits",
   value = {Dist.CLIENT}
)
public final class ShoulderCamera {
   private static final ResourceLocation CROSSHAIR = ResourceLocation.withDefaultNamespace("hud/crosshair");
   private static final ResourceLocation ATTACK_FULL = ResourceLocation.withDefaultNamespace("hud/crosshair_attack_indicator_full");
   private static final ResourceLocation ATTACK_BACKGROUND = ResourceLocation.withDefaultNamespace("hud/crosshair_attack_indicator_background");
   private static final ResourceLocation ATTACK_PROGRESS = ResourceLocation.withDefaultNamespace("hud/crosshair_attack_indicator_progress");
   private static final double AIM_RANGE = 128.0;
   private static final double NEAR = 0.1;
   private static final float RELEASE = 4.0F;
   private static final int AIM_KEEPALIVE = 10;
   private static boolean on;
   private static boolean leaving;
   private static boolean primed;
   private static float blend;
   private static float blendO;
   private static float side = 1.0F;
   private static float sideO = 1.0F;
   private static float dist;
   private static float distO;
   private static float horiz;
   private static float horizO;
   private static float vert;
   private static float vertO;
   private static float pull = 1.0F;
   private static long lastFrame;
   private static boolean hideModel;
   private static boolean aimSent;
   private static Vec3 aimPoint = Vec3.ZERO;
   private static int aimAge;

   public static boolean isOn() {
      return on;
   }

   public static String describe() {
      return Minecraft.getInstance().options.getCameraType() + (on ? (leaving ? " + shoulder (leaving)" : " + shoulder") : "");
   }

   private static boolean right() {
      return CoreClientConfig.SHOULDER_SIDE.get() == CoreClientConfig.ShoulderSide.RIGHT;
   }

   private static boolean available() {
      FruitForm form = ClientFruitState.form();
      return form == null || form.cameraDistance() <= 0.0F;
   }

   private static boolean applies(LocalPlayer player) {
      Minecraft mc = Minecraft.getInstance();
      return on
         && available()
         && !player.isSleeping()
         && !CaptureRig.active()
         && mc.getCameraEntity() == player
         && mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK;
   }

   @SubscribeEvent(
      priority = EventPriority.HIGH
   )
   public static void onClientTick(Pre event) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null) {
         boolean enabled = (Boolean)CoreClientConfig.SHOULDER_ENABLED.get();
         if (enabled) {
            while (ownsNextStep(mc) && mc.options.keyTogglePerspective.consumeClick()) {
               cycle(mc);
            }
         }

         while (DFKeys.SWAP_SHOULDER.consumeClick()) {
            CoreClientConfig.SHOULDER_SIDE.set(right() ? CoreClientConfig.ShoulderSide.LEFT : CoreClientConfig.ShoulderSide.RIGHT);
         }

         if (on && (!enabled || mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK)) {
            on = false;
            leaving = false;
         }

         smooth(player);
         if (leaving && blend <= 0.0F) {
            finishLeaving(mc);
         }

         sendAim(mc, player);
      }
   }

   private static boolean ownsNextStep(Minecraft mc) {
      return on || mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT && available();
   }

   private static void cycle(Minecraft mc) {
      if (leaving) {
         finishLeaving(mc);
         setType(mc, CameraType.THIRD_PERSON_BACK);
      } else if (!on) {
         setType(mc, CameraType.THIRD_PERSON_BACK);
         on = true;
         blendO = 0.0F;
         blend = 0.0F;
         pull = 1.0F;
      } else {
         leaving = true;
         if ((Integer)CoreClientConfig.SHOULDER_SMOOTHING.get() == 0 || mc.player == null || !applies(mc.player)) {
            finishLeaving(mc);
         }
      }
   }

   private static void finishLeaving(Minecraft mc) {
      on = false;
      leaving = false;
      blendO = 0.0F;
      blend = 0.0F;
      setType(mc, CameraType.FIRST_PERSON);
   }

   private static void setType(Minecraft mc, CameraType type) {
      CameraType old = mc.options.getCameraType();
      mc.options.setCameraType(type);
      if (old.isFirstPerson() != type.isFirstPerson()) {
         mc.gameRenderer.checkEntityPostEffect(type.isFirstPerson() ? mc.getCameraEntity() : null);
      }

      mc.levelRenderer.needsUpdate();
   }

   private static void smooth(LocalPlayer player) {
      int n = (Integer)CoreClientConfig.SHOULDER_SMOOTHING.get();
      float step = n <= 0 ? 1.0F : 1.0F / (float)n;
      float follow = n <= 0 ? 1.0F : (float)(1.0 - Math.pow(0.05, 1.0 / (double)n));
      double[] frame = framing(player);
      blendO = blend;
      sideO = side;
      distO = dist;
      horizO = horiz;
      vertO = vert;
      blend = approach(blend, on && !leaving ? 1.0F : 0.0F, step);
      side = approach(side, right() ? 1.0F : -1.0F, 2.0F * step);
      if (!primed) {
         dist = distO = (float)frame[0];
         horiz = horizO = (float)frame[1];
         vert = vertO = (float)frame[2];
         side = sideO = right() ? 1.0F : -1.0F;
         primed = true;
      } else {
         dist = dist + ((float)frame[0] - dist) * follow;
         horiz = horiz + ((float)frame[1] - horiz) * follow;
         vert = vert + ((float)frame[2] - vert) * follow;
      }
   }

   private static float approach(float value, float target, float step) {
      return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
   }

   private static double[] framing(LocalPlayer player) {
      double[] f = formOverride(ClientFruitState.form());
      if (f == null) {
         f = new double[]{
            (Double)CoreClientConfig.SHOULDER_DISTANCE.get(),
            (Double)CoreClientConfig.SHOULDER_HORIZONTAL.get(),
            (Double)CoreClientConfig.SHOULDER_VERTICAL.get()
         };
      }

      double lateral = !player.isVisuallySwimming() && !player.isFallFlying() ? 1.0 : 0.6;
      double s = CoreClientConfig.SHOULDER_SCALE.get() ? (double)player.getScale() : 1.0;
      return new double[]{f[0] * s, f[1] * lateral * s, f[2] * s};
   }

   @Nullable
   private static double[] formOverride(@Nullable FruitForm form) {
      if (form == null) {
         return null;
      } else {
         for (String entry : (List)CoreClientConfig.SHOULDER_FORMS.get()) {
            int eq = entry.indexOf(61);
            String key = entry.substring(0, Math.max(0, eq)).trim();
            if (key.equals(form.name()) || key.equals(form.id().toString())) {
               String[] v = entry.substring(eq + 1).split(",");
               if (v.length == 3) {
                  try {
                     return new double[]{
                        Mth.clamp(Double.parseDouble(v[0].trim()), 1.0, 8.0), Double.parseDouble(v[1].trim()), Double.parseDouble(v[2].trim())
                     };
                  } catch (NumberFormatException var7) {
                  }
               }
            }
         }

         return null;
      }
   }

   private static float ease(float t) {
      return t * t * (3.0F - 2.0F * t);
   }

   @Nullable
   public static Vec3 place(Entity entity, Vec3 pivot, Vec3 vanilla, boolean reverse, float partialTick) {
      hideModel = false;
      if (!reverse && entity instanceof LocalPlayer player && player == Minecraft.getInstance().player && applies(player)) {
         float b = ease(Mth.lerp(partialTick, blendO, blend));
         float yaw = entity.getViewYRot(partialTick);
         Vec3 forward = Vec3.directionFromRotation(entity.getViewXRot(partialTick), yaw);
         double yr = Math.toRadians((double)yaw);
         Vec3 right = new Vec3(-Math.cos(yr), 0.0, -Math.sin(yr));
         Vec3 up = right.cross(forward);
         Vec3 shoulder = forward.scale((double)(-Mth.lerp(partialTick, distO, dist)))
            .add(right.scale((double)(Mth.lerp(partialTick, horizO, horiz) * Mth.lerp(partialTick, sideO, side))))
            .add(up.scale((double)Mth.lerp(partialTick, vertO, vert)));
         Vec3 offset = (leaving ? Vec3.ZERO : vanilla.subtract(pivot)).lerp(shoulder, (double)b);
         long now = Util.getNanos();
         float dt = lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(now - lastFrame) / 1.0E9F);
         lastFrame = now;
         float fit = CoreClientConfig.SHOULDER_COLLISION.get() ? (float)fit(entity, pivot, offset) : 1.0F;
         pull = fit < pull ? fit : Math.min(fit, pull + 4.0F * dt);
         hideModel = offset.length() * (double)pull < (Double)CoreClientConfig.SHOULDER_HIDE_BELOW.get() * (double)player.getScale();
         return pivot.add(offset.scale((double)pull));
      }

      return null;
   }

   public static boolean hideModel() {
      return hideModel;
   }

   private static double fit(Entity entity, Vec3 from, Vec3 offset) {
      double length = offset.length();
      if (length < 1.0E-4) {
         return 1.0;
      } else {
         double free = length;

         for (int i = 0; i < 8; i++) {
            Vec3 a = from.add((double)((i & 1) * 2 - 1) * 0.1, (double)((i >> 1 & 1) * 2 - 1) * 0.1, (double)((i >> 2 & 1) * 2 - 1) * 0.1);
            HitResult hit = PhysicsWorld.clip(entity.level(), new ClipContext(a, a.add(offset), Block.VISUAL, Fluid.NONE, entity));
            if (hit.getType() != Type.MISS) {
               free = Math.min(free, hit.getLocation().distanceTo(a));
            }
         }

         return free / length;
      }
   }

   @SubscribeEvent
   public static void onFov(ComputeFov event) {
      Minecraft mc = Minecraft.getInstance();
      double multiplier = (Double)CoreClientConfig.SHOULDER_FOV.get();
      if (multiplier != 1.0 && event.usedConfiguredFov() && mc.player != null && applies(mc.player)) {
         float b = ease(Mth.lerp((float)event.getPartialTick(), blendO, blend));
         event.setFOV(event.getFOV() * Mth.lerp((double)b, 1.0, multiplier));
      }
   }

   public static void renderCrosshair(GuiGraphics g, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null
         && !mc.options.hideGui
         && !leaving
         && !(blend < 0.5F)
         && applies(player)
         && mc.gameMode != null
         && mc.gameMode.getPlayerMode() != GameType.SPECTATOR) {
         RenderSystem.enableBlend();
         RenderSystem.blendFuncSeparate(SourceFactor.ONE_MINUS_DST_COLOR, DestFactor.ONE_MINUS_SRC_COLOR, SourceFactor.ONE, DestFactor.ZERO);
         g.blitSprite(CROSSHAIR, (g.guiWidth() - 15) / 2, (g.guiHeight() - 15) / 2, 15, 15);
         if (mc.options.attackIndicator().get() == AttackIndicatorStatus.CROSSHAIR) {
            float strength;
            boolean var10000;
            label31: {
               strength = player.getAttackStrengthScale(0.0F);
               if (mc.crosshairPickEntity instanceof LivingEntity target
                  && target.isAlive()
                  && strength >= 1.0F
                  && player.getCurrentItemAttackStrengthDelay() > 5.0F) {
                  var10000 = true;
                  break label31;
               }

               var10000 = false;
            }

            boolean full = var10000;
            int x = g.guiWidth() / 2 - 8;
            int y = g.guiHeight() / 2 - 7 + 16;
            if (full) {
               g.blitSprite(ATTACK_FULL, x, y, 16, 16);
            } else if (strength < 1.0F) {
               g.blitSprite(ATTACK_BACKGROUND, x, y, 16, 4);
               g.blitSprite(ATTACK_PROGRESS, 16, 4, 0, 0, x, y, (int)(strength * 17.0F), 4);
            }
         }

         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
      }
   }

   private static void sendAim(Minecraft mc, LocalPlayer player) {
      if (mc.getConnection() != null) {
         if ((Boolean)CoreClientConfig.SHOULDER_AIM.get() && !leaving && !(blend < 0.5F) && applies(player)) {
            Vec3 point = crosshairPoint(mc, player);
            aimAge++;
            if (!aimSent || aimAge >= 10 || point.distanceToSqr(aimPoint) > 0.0025) {
               PacketDistributor.sendToServer(new AimPayload(true, point), new CustomPacketPayload[0]);
               aimSent = true;
               aimPoint = point;
               aimAge = 0;
            }
         } else {
            if (aimSent) {
               PacketDistributor.sendToServer(AimPayload.none(), new CustomPacketPayload[0]);
               aimSent = false;
            }
         }
      }
   }

   public static Vec3 crosshairPoint(Minecraft mc, LocalPlayer player) {
      Vec3 dir = player.getViewVector(1.0F);
      Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
      Vec3 start = camera.add(dir.scale(Math.max(0.0, player.getEyePosition(1.0F).subtract(camera).dot(dir))));
      Vec3 end = start.add(dir.scale(128.0));
      HitResult block = PhysicsWorld.clip(player.level(), new ClipContext(start, end, Block.OUTLINE, Fluid.NONE, player));
      if (block.getType() != Type.MISS) {
         end = block.getLocation();
      }

      Entity root = player.getRootVehicle();
      EntityHitResult hit = ProjectileUtil.getEntityHitResult(
         player, start, end, new AABB(start, end).inflate(1.0), e -> !e.isSpectator() && e.isPickable() && e.getRootVehicle() != root, start.distanceToSqr(end)
      );
      return hit != null ? hit.getLocation() : end;
   }

   @SubscribeEvent
   public static void onLogout(LoggingOut event) {
      on = false;
      leaving = false;
      primed = false;
      aimSent = false;
      blendO = 0.0F;
      blend = 0.0F;
   }

   private ShoulderCamera() {
   }
}
