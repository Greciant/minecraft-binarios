package com.devilfruits.fruits.uo_uo_seiryu.client.render;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.animation.AnimationClip;
import com.devilfruits.client.animation.AnimationController;
import com.devilfruits.client.haki.CoatGeometry;
import com.devilfruits.client.haki.HakiClientState;
import com.devilfruits.client.haki.HakiCoatRenderer;
import com.devilfruits.client.haki.HakiCoatable;
import com.devilfruits.client.haki.HakiRenderTypes;
import com.devilfruits.client.model.geo.GeoAnimation;
import com.devilfruits.client.model.geo.GeoModel;
import com.devilfruits.client.model.geo.GeoPose;
import com.devilfruits.client.model.geo.GeoRenderer;
import com.devilfruits.client.model.geo.GeoResources;
import com.devilfruits.client.render.FormRenderers;
import com.devilfruits.client.vfx.render.DissolveRenderType;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.haki.HakiFormParts;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.fruits.uo_uo_seiryu.ability.ThundercladVeilAbility;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.TransformCinematic;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.VeilAuraCinematic;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class HybridRenderer implements FormRenderers.FormRenderer, HakiCoatable {
   public static final ResourceLocation MODEL = UoUoSeiryu.id("hybrid");
   private static final ResourceLocation TEXTURE = DevilFruits.id("textures/entity/uo_uo_seiryu/hybrid.png");
   private static final ResourceLocation EMISSIVE = DevilFruits.id("textures/entity/uo_uo_seiryu/hybrid_emissive.png");
   private static final String CLIP_PREFIX = "uo_uo_seiryu/hybrid/";
   private static final float MODEL_SCALE = 0.0525F;
   private static final float WALK_STRIDE = 16.0F;
   private static final float RUN_STRIDE = 13.0F;
   private static final float FIRST_PERSON_SCALE = 0.85F;
   private static final float DEG = (float) (Math.PI / 180.0);
   private final Int2ObjectMap<HybridRenderer.State> states = new Int2ObjectOpenHashMap();
   private final AnimationController.ClipState clipState = new AnimationController.ClipState();
   private final Map<ResourceLocation, GeoAnimation> mapped = new HashMap<>();
   private int generation = -1;
   @Nullable
   private GeoModel model;
   private GeoRenderer geo;
   private GeoPose pose;
   private GeoPose fpPose;
   private GeoAnimation idle;
   private GeoAnimation walk;
   private GeoAnimation run;
   private GeoAnimation crouch;
   private GeoAnimation fall;
   private GeoAnimation glide;
   private GeoAnimation swim;
   private GeoAnimation sit;
   private GeoAnimation death;
   private int head;
   private int neck;
   private int body;
   private int jaw;
   private int rightArm;
   private int leftArm;
   private int rightForearm;
   private int leftForearm;
   private int rightHand;
   private int leftHand;
   private int rightItem;
   private int leftItem;
   private int rightLeg;
   private int leftLeg;
   private int crown;
   private int[] tail;
   private int[] whiskers;
   private int[] hair;
   private int[] beard;
   private final Vector3f scratch = new Vector3f();

   @Override
   public int coatSupport() {
      return HakiFormParts.support(UoUoSeiryu.ID, "hybrid");
   }

   private boolean ensureModel() {
      int gen = GeoResources.MODELS.generation();
      if (gen == this.generation) {
         return this.model != null;
      } else {
         this.generation = gen;
         this.mapped.clear();
         this.states.clear();
         this.model = GeoResources.model(MODEL);
         if (this.model == null) {
            DevilFruits.LOGGER.error("Seiryu hybrid model {} is missing, drawing the vanilla player", MODEL);
            return false;
         } else {
            this.geo = new GeoRenderer(this.model);
            this.pose = new GeoPose(this.model);
            this.fpPose = new GeoPose(this.model);
            this.idle = clip("idle");
            this.walk = clip("walk");
            this.run = clip("run");
            this.crouch = clip("crouch");
            this.fall = clip("fall");
            this.glide = clip("glide");
            this.swim = clip("swim");
            this.sit = clip("sit");
            this.death = clip("death");
            this.head = this.model.bone("head");
            this.neck = this.model.bone("neck");
            this.body = this.model.bone("body");
            this.jaw = this.model.bone("jaw");
            this.rightArm = this.model.bone("right_arm");
            this.leftArm = this.model.bone("left_arm");
            this.rightForearm = this.model.bone("right_forearm");
            this.leftForearm = this.model.bone("left_forearm");
            this.rightHand = this.model.bone("right_hand");
            this.leftHand = this.model.bone("left_hand");
            this.rightItem = this.model.bone("right_item");
            this.leftItem = this.model.bone("left_item");
            this.rightLeg = this.model.bone("right_leg");
            this.leftLeg = this.model.bone("left_leg");
            this.crown = this.model.bone("crown");
            this.tail = this.bonesWithPrefix("tail_");
            this.whiskers = this.bonesWithPrefix("whisker_");
            this.hair = this.bonesWithPrefix("hair_");
            this.beard = this.bonesWithPrefix("beard_");
            return true;
         }
      }
   }

   @Nullable
   private static GeoAnimation clip(String name) {
      return GeoResources.animation(UoUoSeiryu.id("hybrid/" + name));
   }

   private int[] bonesWithPrefix(String prefix) {
      IntArrayList out = new IntArrayList();

      for (GeoModel.Bone bone : this.model.bones) {
         if (bone.name.startsWith(prefix)) {
            out.add(bone.index);
         }
      }

      return out.toIntArray();
   }

   @Nullable
   private GeoAnimation resolve(@Nullable ResourceLocation id, @Nullable AnimationClip clip) {
      if (clip instanceof GeoAnimation g && g.id.getPath().startsWith("uo_uo_seiryu/hybrid/")) {
         return g;
      }

      if (id == null) {
         return null;
      } else {
         GeoAnimation g = this.mapped.get(id);
         if (g == null && !this.mapped.containsKey(id)) {
            String path = id.getPath();
            g = clip(path.substring(path.lastIndexOf(47) + 1));
            this.mapped.put(id, g);
         }

         return g;
      }
   }

   @Override
   public boolean render(AbstractClientPlayer player, FruitForm form, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      if (!player.isSpectator() && this.ensureModel()) {
         float age = (float)player.tickCount + partialTick;
         float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
         float headYaw = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
         float netHeadYaw = Mth.wrapDegrees(headYaw - bodyYaw);
         float headPitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());
         HybridRenderer.State st = (HybridRenderer.State)this.states.computeIfAbsent(player.getId(), k -> new HybridRenderer.State());
         update(st, player, age, bodyYaw);
         Matrix4f entry = poseStack.last().pose();
         boolean world = Math.abs(entry.m00() - 1.0F) < 0.001F && Math.abs(entry.m11() - 1.0F) < 0.001F && Math.abs(entry.m22() - 1.0F) < 0.001F;
         float ex = entry.m30();
         float ey = entry.m31();
         float ez = entry.m32();
         poseStack.pushPose();
         boolean deathClip = this.death != null;
         setupRotations(player, poseStack, bodyYaw, partialTick, deathClip);
         poseStack.scale(0.0525F, 0.0525F, 0.0525F);
         this.pose.reset();
         this.animate(player, st, age, partialTick, netHeadYaw, headPitch);
         this.geo.pose(poseStack.last(), this.pose);
         Minecraft mc = Minecraft.getInstance();
         boolean visible = !player.isInvisible();
         boolean translucent = !visible && !player.isInvisibleTo(mc.player);
         if (visible || translucent) {
            int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
            RenderType type = translucent ? RenderType.itemEntityTranslucentCull(TEXTURE) : RenderType.entityCutoutNoCull(TEXTURE);
            float reveal = visible ? TransformCinematic.reveal(player, partialTick) : 1.0F;
            RenderType dissolve = reveal < 0.999F ? DissolveRenderType.of(TEXTURE) : null;
            if (dissolve != null) {
               this.geo.render(buffers.getBuffer(dissolve), this.pose, packedLight, overlay, DissolveRenderType.colour(reveal));
            } else {
               this.geo.render(buffers.getBuffer(type), this.pose, packedLight, overlay, translucent ? 654311423 : -1);
            }

            if (visible && this.head >= 0 && !this.pose.hidden[this.head] && reveal > 0.7F) {
               this.geo.renderBone(buffers.getBuffer(RenderType.eyes(EMISSIVE)), this.model.bones[this.head], packedLight, OverlayTexture.NO_OVERLAY, -1);
            }

            float sheen = visible && FormFlags.has(player, ThundercladVeilAbility.FLAG) ? VeilAuraCinematic.sheen(player, partialTick) : 0.0F;
            if (sheen > 0.01F) {
               int c = (int)(Math.min(1.0F, sheen) * 255.0F);
               float scroll = age * 0.011F;
               this.geo
                  .render(
                     buffers.getBuffer(RenderType.energySwirl(VfxSprites.VEIL_SHEEN, scroll % 1.0F, scroll * 0.73F % 1.0F)),
                     this.pose,
                     15728880,
                     OverlayTexture.NO_OVERLAY,
                     0xFF000000 | c << 16 | c << 8 | c
                  );
            }

            int coat = visible && !HakiClientState.blinkedOut(player) ? HakiClientState.shown(player) : 0;
            if (coat != 0) {
               CoatGeometry g = HakiCoatRenderer.begin();
               HakiCoatRenderer.geo(g, this.geo, this.pose, UoUoSeiryu.ID, "hybrid", coat, packedLight);
               HakiCoatRenderer.passes(
                  buffers, g, HakiRenderTypes.COAT_TILE, HakiRenderTypes.OUTLINE_TILE, HakiClientState.progress(player, partialTick), packedLight, 0.0F, 0.035F
               );
            }
         }

         if (world) {
            this.recordAnchors(player, ex, ey, ez);
         }

         if (visible) {
            this.renderHeld(player, player.getMainHandItem(), player.getMainArm(), poseStack, buffers, packedLight);
            this.renderHeld(player, player.getOffhandItem(), player.getMainArm().getOpposite(), poseStack, buffers, packedLight);
         }

         poseStack.popPose();
         HybridGhosts.Trail trail = HybridGhosts.get(player);
         if (trail != null && trail.count > 1 && visible) {
            this.renderGhosts(player, trail, poseStack, buffers, partialTick);
         }

         FormRenderers.renderNameTag(player, partialTick, poseStack, buffers, packedLight);
         return true;
      } else {
         return false;
      }
   }

   private void renderGhosts(AbstractClientPlayer player, HybridGhosts.Trail trail, PoseStack poseStack, MultiBufferSource buffers, float partialTick) {
      double lx = Mth.lerp((double)partialTick, player.xOld, player.getX());
      double ly = Mth.lerp((double)partialTick, player.yOld, player.getY());
      double lz = Mth.lerp((double)partialTick, player.zOld, player.getZ());
      VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));

      for (int k = 1; k < trail.count; k++) {
         int i = Math.floorMod(trail.head - k, 5);
         double dx = trail.x[i] - lx;
         double dy = trail.y[i] - ly;
         double dz = trail.z[i] - lz;
         if (!(dx * dx + dy * dy + dz * dz < 0.36)) {
            float fade = 1.0F - (float)k / 5.0F;
            int alpha = (int)((float)(trail.tint >>> 24) * fade);
            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);
            setupRotations(player, poseStack, trail.yaw[i], partialTick, false);
            poseStack.scale(0.0525F, 0.0525F, 0.0525F);
            this.geo.pose(poseStack.last(), this.pose);
            this.geo.render(vc, this.pose, 15728880, OverlayTexture.NO_OVERLAY, alpha << 24 | trail.tint & 16777215);
            poseStack.popPose();
         }
      }
   }

   private Vector3f bonePoint(int bone, float x, float y, float z, Vector3f out) {
      return this.geo.boneMatrix(bone).transformPosition(x, y, z, out);
   }

   private void recordAnchors(AbstractClientPlayer player, float ex, float ey, float ez) {
      anchor(player, 0, this.wrist(this.rightForearm, this.rightHand, this.scratch), ex, ey, ez);
      anchor(player, 1, this.wrist(this.leftForearm, this.leftHand, this.scratch), ex, ey, ez);
      if (this.tail.length > 1) {
         anchor(player, 2, this.tailTip(this.scratch), ex, ey, ez);
      }

      if (this.crown >= 0) {
         GeoModel.Bone c = this.model.bones[this.crown];
         anchor(player, 3, this.bonePoint(this.crown, c.pivotX, c.pivotY + 2.4F, c.pivotZ, this.scratch), ex, ey, ez);
      }

      if (this.body >= 0) {
         anchor(player, 4, this.bonePoint(this.body, 0.0F, 24.5F, -3.8F, this.scratch), ex, ey, ez);
      }
   }

   private static void anchor(AbstractClientPlayer player, int anchor, Vector3f p, float ex, float ey, float ez) {
      HybridAnchors.record(player, anchor, (double)(p.x - ex), (double)(p.y - ey), (double)(p.z - ez));
   }

   private Vector3f wrist(int forearm, int hand, Vector3f out) {
      GeoModel.Bone f = this.model.bones[forearm];
      GeoModel.Bone h = this.model.bones[hand];
      return this.bonePoint(forearm, Mth.lerp(0.8F, f.pivotX, h.pivotX), Mth.lerp(0.8F, f.pivotY, h.pivotY), Mth.lerp(0.8F, f.pivotZ, h.pivotZ), out);
   }

   private Vector3f tailTip(Vector3f out) {
      GeoModel.Bone last = this.model.bones[this.tail[this.tail.length - 1]];
      GeoModel.Bone prev = this.model.bones[this.tail[this.tail.length - 2]];
      return this.bonePoint(last.index, 2.0F * last.pivotX - prev.pivotX, 2.0F * last.pivotY - prev.pivotY, 2.0F * last.pivotZ - prev.pivotZ, out);
   }

   private static void setupRotations(AbstractClientPlayer player, PoseStack poseStack, float bodyYaw, float partialTick, boolean deathClip) {
      boolean sleeping = player.hasPose(Pose.SLEEPING);
      float scale = player.getScale();
      if (sleeping) {
         Direction bed = player.getBedOrientation();
         if (bed != null) {
            float eye = player.getEyeHeight(Pose.STANDING) - 0.1F;
            poseStack.translate((float)(-bed.getStepX()) * eye, 0.0F, (float)(-bed.getStepZ()) * eye);
         }
      }

      poseStack.scale(scale, scale, scale);
      if (!sleeping) {
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
      }

      if (player.deathTime > 0 && !deathClip) {
         float f = Math.min(1.0F, Mth.sqrt(((float)player.deathTime + partialTick - 1.0F) / 20.0F * 1.6F));
         poseStack.mulPose(Axis.ZP.rotationDegrees(f * 90.0F));
      } else if (player.isAutoSpinAttack()) {
         poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F - player.getXRot()));
         poseStack.mulPose(Axis.YP.rotationDegrees(((float)player.tickCount + partialTick) * -75.0F));
      } else if (sleeping) {
         Direction bed = player.getBedOrientation();
         float var10000;
         if (bed != null) {
            switch (bed) {
               case SOUTH:
                  var10000 = 90.0F;
                  break;
               case NORTH:
                  var10000 = 270.0F;
                  break;
               case EAST:
                  var10000 = 180.0F;
                  break;
               default:
                  var10000 = 0.0F;
            }
         } else {
            var10000 = bodyYaw;
         }

         float rot = var10000;
         poseStack.mulPose(Axis.YP.rotationDegrees(rot));
         poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
         poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
      }

      float pitch = player.getViewXRot(partialTick);
      float swim = player.getSwimAmount(partialTick);
      if (player.isFallFlying()) {
         float t = (float)player.getFallFlyingTicks() + partialTick;
         float f = Mth.clamp(t * t / 100.0F, 0.0F, 1.0F);
         if (!player.isAutoSpinAttack()) {
            poseStack.mulPose(Axis.XP.rotationDegrees(f * (-90.0F - pitch)));
         }

         Vec3 view = player.getViewVector(partialTick);
         Vec3 motion = player.getDeltaMovementLerped(partialTick);
         double m = motion.horizontalDistanceSqr();
         double v = view.horizontalDistanceSqr();
         if (m > 0.0 && v > 0.0) {
            double dot = (motion.x * view.x + motion.z * view.z) / Math.sqrt(m * v);
            double cross = motion.x * view.z - motion.z * view.x;
            poseStack.mulPose(Axis.YP.rotation((float)(Math.signum(cross) * Math.acos(Mth.clamp(dot, -1.0, 1.0)))));
         }
      } else if (swim > 0.0F) {
         boolean inFluid = player.isInWater() || player.isInLava();
         poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(swim, 0.0F, inFluid ? -90.0F - player.getXRot() : -90.0F)));
         if (player.isVisuallySwimming()) {
            poseStack.translate(0.0F, -1.0F, 0.3F);
         }
      }
   }

   private static void update(HybridRenderer.State st, AbstractClientPlayer player, float age, float bodyYaw) {
      st.lastSeen = player.tickCount;
      float dt = age - st.lastAge;
      if (Float.isNaN(st.lastAge) || dt < 0.0F || dt > 10.0F) {
         st.lastAge = age;
         st.lastBodyYaw = bodyYaw;
      } else if (dt != 0.0F) {
         st.lastAge = age;
         float turn = Mth.wrapDegrees(bodyYaw - st.lastBodyYaw) / dt;
         st.lastBodyYaw = bodyYaw;
         st.turn = st.turn + (turn - st.turn) * Math.min(1.0F, 0.35F * dt);
         float k = Math.min(1.0F, 0.25F * dt);
         boolean airborne = !player.onGround() && !player.isInWater() && !player.isPassenger() && !player.getAbilities().flying;
         st.air = st.air + ((airborne ? 1.0F : 0.0F) - st.air) * k;
         st.sprint = st.sprint + ((player.isSprinting() ? 1.0F : 0.0F) - st.sprint) * k;
         st.crouch = st.crouch + ((player.isCrouching() ? 1.0F : 0.0F) - st.crouch) * Math.min(1.0F, 0.45F * dt);
         st.glide = st.glide + ((FormFlags.has(player, 2) && airborne ? 1.0F : 0.0F) - st.glide) * Math.min(1.0F, 0.2F * dt);
         Vec3 motion = player.getDeltaMovement();
         float speed = (float)motion.horizontalDistance();
         float vy = (float)motion.y;
         spring(st, 0, -Mth.clamp(st.turn * 2.4F, -45.0F, 45.0F), dt);
         spring(st, 1, Mth.clamp(speed * 55.0F - vy * 20.0F, -20.0F, 30.0F), dt);
         spring(st, 2, Mth.clamp(speed * 120.0F + Math.max(0.0F, vy) * 40.0F - Math.min(0.0F, vy) * 60.0F, 0.0F, 60.0F), dt);
      }
   }

   private static void spring(HybridRenderer.State st, int which, float target, float dt) {
      float stiffness = 0.22F;
      float damping = 0.42F;

      for (int i = 0; i < 2; i++) {
         float h = dt * 0.5F;
         switch (which) {
            case 0:
               st.tailYawV = st.tailYawV + (stiffness * (target - st.tailYaw) - damping * st.tailYawV) * h;
               st.tailYaw = st.tailYaw + st.tailYawV * h;
               break;
            case 1:
               st.tailLiftV = st.tailLiftV + (stiffness * (target - st.tailLift) - damping * st.tailLiftV) * h;
               st.tailLift = st.tailLift + st.tailLiftV * h;
               break;
            default:
               st.dragV = st.dragV + (stiffness * (target - st.drag) - damping * st.dragV) * h;
               st.drag = st.drag + st.dragV * h;
         }
      }
   }

   private void animate(AbstractClientPlayer player, HybridRenderer.State st, float age, float partialTick, float netHeadYaw, float headPitch) {
      GeoModel m = this.model;
      boolean riding = player.isPassenger();
      float speed = !riding && player.isAlive() ? Math.min(1.0F, player.walkAnimation.speed(partialTick)) : 0.0F;
      float position = player.walkAnimation.position(partialTick);
      float moving = Mth.clamp(speed * 1.6F, 0.0F, 1.0F);
      float ground = 1.0F - Math.max(st.air, st.glide);
      if (this.idle != null) {
         this.idle.add(m, this.pose, loopTime(this.idle, age), 1.0F - moving);
      }

      if (this.walk != null) {
         this.walk.add(m, this.pose, this.walk.length() * frac(position / 16.0F), moving * (1.0F - st.sprint));
      }

      if (this.run != null) {
         this.run.add(m, this.pose, this.run.length() * frac(position / 13.0F), moving * st.sprint);
      }

      if (this.crouch != null && st.crouch > 0.01F) {
         this.crouch.add(m, this.pose, loopTime(this.crouch, age), st.crouch);
      }

      if (this.fall != null && st.air > 0.01F) {
         this.fall.blend(m, this.pose, loopTime(this.fall, age), st.air * (1.0F - st.glide));
      }

      if (this.glide != null && st.glide > 0.01F) {
         this.glide.blend(m, this.pose, loopTime(this.glide, age), st.glide);
      }

      float swimAmount = player.getSwimAmount(partialTick);
      if (this.swim != null && swimAmount > 0.01F) {
         this.swim.blend(m, this.pose, loopTime(this.swim, age), swimAmount);
      }

      if (this.sit != null && riding) {
         this.sit.blend(m, this.pose, 0.0F, 1.0F);
      }

      if (AnimationController.current(player, partialTick, this.clipState)) {
         GeoAnimation previous = this.resolve(this.clipState.previousId, this.clipState.previous);
         if (previous != null && this.clipState.previousWeight > 0.001F) {
            previous.blend(m, this.pose, this.clipState.previousTime, this.clipState.previousWeight);
         }

         GeoAnimation current = this.resolve(this.clipState.id, this.clipState.clip);
         if (current != null) {
            current.blend(m, this.pose, this.clipState.time, this.clipState.weight);
         }
      }

      float yaw = Mth.clamp(netHeadYaw, -75.0F, 75.0F) * (float) (Math.PI / 180.0);
      float pitch = Mth.clamp(headPitch, -60.0F, 60.0F) * (float) (Math.PI / 180.0);
      this.pose.addRotation(this.neck, -pitch * 0.3F, -yaw * 0.3F, 0.0F);
      this.pose.addRotation(this.head, -pitch * 0.7F, -yaw * 0.7F, 0.0F);
      this.attackSwing(player, partialTick, headPitch);
      if (player.hurtTime > 0 && player.deathTime == 0) {
         float h = ((float)player.hurtTime - partialTick) / 10.0F;
         float flinch = Mth.sin(h * (float) Math.PI) * 0.12F * ground;
         this.pose.addRotation(this.body, flinch, 0.0F, 0.0F);
         this.pose.addRotation(this.head, flinch * 1.5F, 0.0F, 0.0F);
      }

      if (this.death != null && player.deathTime > 0) {
         this.death.blend(m, this.pose, (float)player.deathTime + partialTick, 1.0F);
      }

      this.secondary(st, age, ground);
   }

   private void attackSwing(AbstractClientPlayer player, float partialTick, float headPitch) {
      float attack = player.getAttackAnim(partialTick);
      if (!(attack <= 0.0F)) {
         HumanoidArm arm = player.swingingArm == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
         int armBone = arm == HumanoidArm.RIGHT ? this.rightArm : this.leftArm;
         float bodyTwist = Mth.sin(Mth.sqrt(attack) * (float) (Math.PI * 2)) * 0.2F * (arm == HumanoidArm.LEFT ? -1.0F : 1.0F);
         this.pose.addRotation(this.body, 0.0F, -bodyTwist, 0.0F);
         float f = 1.0F - attack;
         f = 1.0F - f * f * f * f;
         float lift = Mth.sin(f * (float) Math.PI) * 1.2F + Mth.sin(attack * (float) Math.PI) * -(headPitch * (float) (Math.PI / 180.0) - 0.7F) * 0.75F;
         this.pose.addRotation(armBone, lift, -bodyTwist * 2.0F, Mth.sin(attack * (float) Math.PI) * -0.4F);
      }
   }

   private void secondary(HybridRenderer.State st, float age, float ground) {
      int n = this.tail.length;

      for (int i = 0; i < n; i++) {
         float f = ((float)i + 1.0F) / (float)n;
         float wave = Mth.sin(age * 0.09F - (float)i * 0.6F) * 5.0F * f;
         this.pose
            .addRotation(
               this.tail[i],
               (st.tailLift * 0.35F * f - (1.0F - ground) * 4.0F) * (float) (Math.PI / 180.0),
               (st.tailYaw * 0.3F * f + wave) * (float) (Math.PI / 180.0),
               Mth.sin(age * 0.07F - (float)i * 0.5F) * 2.0F * f * (float) (Math.PI / 180.0)
            );
      }

      float drag = st.drag * (float) (Math.PI / 180.0);

      for (int i = 0; i < this.whiskers.length; i++) {
         float sway = Mth.sin(age * 0.11F + (float)i * 0.9F) * 3.0F * (float) (Math.PI / 180.0);
         this.pose.addRotation(this.whiskers[i], -drag * 0.35F + sway, 0.0F, sway * 0.6F);
      }

      for (int i = 0; i < this.hair.length; i++) {
         float sway = Mth.sin(age * 0.08F + (float)i * 1.3F) * 2.5F * (float) (Math.PI / 180.0);
         this.pose.addRotation(this.hair[i], -drag * 0.2F + sway, 0.0F, sway);
      }

      for (int i = 0; i < this.beard.length; i++) {
         this.pose.addRotation(this.beard[i], -drag * 0.15F + Mth.sin(age * 0.1F + (float)i) * 1.5F * (float) (Math.PI / 180.0), 0.0F, 0.0F);
      }
   }

   private static float loopTime(GeoAnimation clip, float age) {
      return clip.length() <= 0.0F ? 0.0F : age % clip.length();
   }

   private static float frac(float v) {
      return v - (float)Mth.floor(v);
   }

   private void renderHeld(AbstractClientPlayer player, ItemStack stack, HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffers, int light) {
      int bone = arm == HumanoidArm.RIGHT ? this.rightItem : this.leftItem;
      if (!stack.isEmpty() && bone >= 0 && !this.pose.hidden[bone]) {
         GeoModel.Bone b = this.model.bones[bone];
         boolean left = arm == HumanoidArm.LEFT;
         poseStack.pushPose();
         poseStack.last().pose().set(this.geo.boneMatrix(bone));
         poseStack.last().normal().set(this.geo.boneNormal(bone));
         poseStack.translate(b.pivotX, b.pivotY, b.pivotZ);
         poseStack.scale(16.0F, 16.0F, 16.0F);
         poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
         poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
         poseStack.translate(0.0F, 0.12F, 0.0F);
         Minecraft.getInstance()
            .getEntityRenderDispatcher()
            .getItemInHandRenderer()
            .renderItem(
               player, stack, left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, left, poseStack, buffers, light
            );
         poseStack.popPose();
      }
   }

   @Override
   public boolean renderArm(AbstractClientPlayer player, FruitForm form, HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      if (!this.ensureModel()) {
         return false;
      } else {
         boolean right = arm == HumanoidArm.RIGHT;
         int upper = right ? this.rightArm : this.leftArm;
         int fore = right ? this.rightForearm : this.leftForearm;
         int hand = right ? this.rightHand : this.leftHand;
         if (upper >= 0 && fore >= 0 && hand >= 0) {
            GeoModel.Bone elbow = this.model.bones[fore];
            poseStack.pushPose();
            poseStack.translate((right ? -5.0F : 5.0F) / 16.0F, 0.1875F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            float k = 0.053125F;
            poseStack.scale(k, k, k);
            poseStack.translate(-elbow.pivotX, -elbow.pivotY, -elbow.pivotZ);
            this.fpPose.reset();
            GeoModel.Bone shoulder = this.model.bones[upper];
            this.fpPose.addRotation(upper, -shoulder.rotX, -shoulder.rotY, -shoulder.rotZ);
            this.fpPose.addRotation(fore, -elbow.rotX, -elbow.rotY, -elbow.rotZ);
            this.geo.pose(poseStack.last(), this.fpPose);
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            this.geo.renderBone(vc, elbow, packedLight, OverlayTexture.NO_OVERLAY, -1);
            this.geo.renderBone(vc, this.model.bones[hand], packedLight, OverlayTexture.NO_OVERLAY, -1);
            poseStack.popPose();
            return true;
         } else {
            return false;
         }
      }
   }

   public void clear() {
      this.states.clear();
      HybridAnchors.clear();
      HybridGhosts.clear();
   }

   public void prune(ClientLevel level) {
      HybridAnchors.prune(level);
      this.states.int2ObjectEntrySet().removeIf(e -> {
         Entity entity = level.getEntity(e.getIntKey());
         return entity == null || entity.tickCount - ((HybridRenderer.State)e.getValue()).lastSeen > 200;
      });
   }

   private static final class State {
      float lastAge = Float.NaN;
      float lastBodyYaw;
      int lastSeen;
      float turn;
      float tailYaw;
      float tailYawV;
      float tailLift;
      float tailLiftV;
      float drag;
      float dragV;
      float air;
      float sprint;
      float crouch;
      float glide;
   }
}
