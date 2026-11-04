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
import com.devilfruits.core.body.BodyFrames;
import com.devilfruits.core.body.SerpentBody;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.haki.HakiFormParts;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuClientConfig;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.fruits.uo_uo_seiryu.entity.SeiryuDragonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import org.joml.FrustumIntersection;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class DragonRenderer implements FormRenderers.FormRenderer, HakiCoatable {
   public static final ResourceLocation HEAD_MODEL = UoUoSeiryu.id("dragon_head");
   public static final ResourceLocation PARTS_MODEL = UoUoSeiryu.id("dragon_parts");
   private static final ResourceLocation BODY_TEXTURE = DevilFruits.id("textures/entity/uo_uo_seiryu/dragon_body.png");
   private static final ResourceLocation HEAD_TEXTURE = DevilFruits.id("textures/entity/uo_uo_seiryu/dragon_head.png");
   private static final ResourceLocation HEAD_EMISSIVE = DevilFruits.id("textures/entity/uo_uo_seiryu/dragon_head_emissive.png");
   private static final ResourceLocation PARTS_TEXTURE = DevilFruits.id("textures/entity/uo_uo_seiryu/dragon_parts.png");
   private static final String CLIP_PREFIX = "uo_uo_seiryu/dragon/";
   private static final float HEAD_SCALE = 0.09375F;
   private static final float PART_SCALE = 0.08125F;
   private static final float TILE = 6.0F;
   private static final float WIDTH = 1.05F;
   private static final float HEIGHT = 0.95F;
   private static final List<SeiryuDragonEntity> SHELLS = new ArrayList<>();
   public static int debugCopies;
   private final BodyFrames frames = new BodyFrames(160);
   private final int[] light = new int[160];
   private final boolean[] visible = new boolean[160];
   private final Matrix4f matrix = new Matrix4f();
   private final Matrix3f normalMatrix = new Matrix3f();
   private final Vector3f pos = new Vector3f();
   private final Vector3f nrm = new Vector3f();
   private final MutableBlockPos blockPos = new MutableBlockPos();
   private final PoseStack partStack = new PoseStack();
   private final FrustumIntersection cull = new FrustumIntersection();
   private final Matrix4f cullMatrix = new Matrix4f();
   private int generation = -1;
   @Nullable
   private GeoRenderer head;
   private GeoPose headPose;
   @Nullable
   private GeoRenderer parts;
   private GeoPose partsPose;
   private int tuft;
   private int legFront;
   private int legBack;
   private int tailTip;
   private final AnimationController.ClipState clipState = new AnimationController.ClipState();
   private int[] whiskers;
   private static final int MAX_SECTIONS = 160;
   private static final int COAT_BODY = 0;
   private static final int COAT_TAIL = 1;
   private static final int COAT_FRONT = 2;
   private static final int COAT_BACK = 3;
   private static final int COAT_HEAD = 4;
   private final Int2ObjectOpenHashMap<boolean[]> coatParts = new Int2ObjectOpenHashMap();
   private static final int[] CAP_ORDER = new int[]{0, 3, 2, 1};

   @Override
   public int coatSupport() {
      return HakiFormParts.support(UoUoSeiryu.ID, "dragon");
   }

   public static void track(SeiryuDragonEntity shell) {
      if (!SHELLS.contains(shell)) {
         SHELLS.add(shell);
      }
   }

   public static void untrack(SeiryuDragonEntity shell) {
      SHELLS.remove(shell);
   }

   public static void clear() {
      SHELLS.clear();
   }

   @Nullable
   public static SeiryuDragonEntity shellOf(Entity owner) {
      for (int i = 0; i < SHELLS.size(); i++) {
         SeiryuDragonEntity shell = SHELLS.get(i);
         if (shell.ownerId() == owner.getId() && !shell.isRemoved()) {
            return shell;
         }
      }

      return null;
   }

   public static List<SeiryuDragonEntity> shells() {
      return SHELLS;
   }

   public static int count() {
      return SHELLS.size();
   }

   @Override
   public boolean render(AbstractClientPlayer player, FruitForm form, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      FormRenderers.renderNameTag(player, partialTick, poseStack, buffers, packedLight);
      return true;
   }

   @Override
   public boolean renderFirstPerson(
      AbstractClientPlayer player,
      FruitForm form,
      InteractionHand hand,
      ItemStack stack,
      float partialTick,
      float swingProgress,
      float equipProgress,
      PoseStack poseStack,
      MultiBufferSource buffers,
      int packedLight
   ) {
      return true;
   }

   @Override
   public boolean renderArm(AbstractClientPlayer player, FruitForm form, HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      return true;
   }

   private boolean ensureModels() {
      int gen = GeoResources.MODELS.generation();
      if (gen != this.generation) {
         this.generation = gen;
         GeoModel headModel = GeoResources.model(HEAD_MODEL);
         GeoModel partsModel = GeoResources.model(PARTS_MODEL);
         this.head = headModel == null ? null : new GeoRenderer(headModel);
         this.headPose = headModel == null ? null : new GeoPose(headModel);
         this.parts = partsModel == null ? null : new GeoRenderer(partsModel);
         this.partsPose = partsModel == null ? null : new GeoPose(partsModel);
         if (headModel != null) {
            List<Integer> w = new ArrayList<>();

            for (GeoModel.Bone b : headModel.bones) {
               if (b.name.startsWith("whisker_")) {
                  w.add(b.index);
               }
            }

            this.whiskers = w.stream().mapToInt(Integer::intValue).toArray();
         }

         if (partsModel != null) {
            this.tuft = partsModel.bone("tuft");
            this.legFront = partsModel.bone("leg_front");
            this.legBack = partsModel.bone("leg_back");
            this.tailTip = partsModel.bone("tail_tip");
         }
      }

      return this.head != null && this.parts != null;
   }

   public void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_ENTITIES && !SHELLS.isEmpty()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null && this.ensureModels()) {
            float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            Vec3 cam = event.getCamera().getPosition();
            this.cull.set(this.cullMatrix.set(event.getProjectionMatrix()).mul(event.getModelViewMatrix()));
            PoseStack poseStack = event.getPoseStack();
            BufferSource buffers = mc.renderBuffers().bufferSource();

            for (int i = 0; i < SHELLS.size(); i++) {
               SeiryuDragonEntity shell = SHELLS.get(i);
               if (!shell.isRemoved() && shell.level() == mc.level) {
                  Player owner = shell.owner();
                  SerpentBody body = shell.body();
                  if (owner != null && body != null && !owner.isInvisibleTo(mc.player)) {
                     this.drawDragon(mc, owner, body, partial, cam, poseStack, buffers);

                     for (int k = 1; k <= debugCopies; k++) {
                        Vec3 shifted = cam.subtract((double)(k % 4 * 28 - 42), (double)(8 + k / 4 * 14), (double)(36 + k % 3 * 10));
                        this.drawDragon(mc, owner, body, partial, shifted, poseStack, buffers);
                     }
                  }
               }
            }

            buffers.endBatch();
         }
      }
   }

   private void drawDragon(Minecraft mc, Player owner, SerpentBody body, float partial, Vec3 cam, PoseStack poseStack, BufferSource buffers) {
      double hx = Mth.lerp((double)partial, owner.xo, owner.getX());
      double hy = Mth.lerp((double)partial, owner.yo, owner.getY());
      double hz = Mth.lerp((double)partial, owner.zo, owner.getZ());
      double dist = Math.sqrt(cam.distanceToSqr(hx, hy, hz));
      int sections = sections(dist, body.length);
      body.sample(this.frames, sections, partial);
      int n = this.frames.count;
      double minX = Double.MAX_VALUE;
      double minY = Double.MAX_VALUE;
      double minZ = Double.MAX_VALUE;
      double maxX = -Double.MAX_VALUE;
      double maxY = -Double.MAX_VALUE;
      double maxZ = -Double.MAX_VALUE;

      for (int i = 0; i < n; i++) {
         minX = Math.min(minX, this.frames.x[i]);
         minY = Math.min(minY, this.frames.y[i]);
         minZ = Math.min(minZ, this.frames.z[i]);
         maxX = Math.max(maxX, this.frames.x[i]);
         maxY = Math.max(maxY, this.frames.y[i]);
         maxZ = Math.max(maxZ, this.frames.z[i]);
      }

      float pad = body.thickness * 2.5F;
      boolean firstPerson = owner == mc.player && mc.options.getCameraType().isFirstPerson();
      boolean headVisible = this.visible(cam, hx - 4.0, hy - 2.0, hz - 4.0, hx + 4.0, hy + 6.0, hz + 4.0);
      if (headVisible
         || this.visible(cam, minX - (double)pad, minY - (double)pad, minZ - (double)pad, maxX + (double)pad, maxY + (double)pad, maxZ + (double)pad)) {
         int overlay = LivingEntityRenderer.getOverlayCoords(owner, 0.0F);

         for (int i = 0; i < n; i++) {
            double r = (double)this.frames.radius[i] * 1.6;
            double clear = firstPerson ? 3.0 : (double)this.frames.radius[i] + 2.5;
            boolean near = cam.distanceToSqr(this.frames.x[i], this.frames.y[i], this.frames.z[i]) < clear * clear;
            this.visible[i] = !near
               && this.visible(
                  cam, this.frames.x[i] - r, this.frames.y[i] - r, this.frames.z[i] - r, this.frames.x[i] + r, this.frames.y[i] + r, this.frames.z[i] + r
               );
            this.blockPos.set(this.frames.x[i], this.frames.y[i], this.frames.z[i]);
            this.light[i] = LevelRenderer.getLightColor(mc.level, this.blockPos);
         }

         Matrix4f view = poseStack.last().pose();
         Matrix3f viewNormal = poseStack.last().normal();
         this.drawTube(buffers.getBuffer(RenderType.entityCutout(BODY_TEXTURE)), view, viewNormal, cam, overlay, 0, n);
         if (dist < 160.0) {
            this.drawDetails(buffers, poseStack, cam, body, overlay, dist < 64.0);
         }

         int coat = HakiClientState.blinkedOut(owner) ? 0 : HakiClientState.shown(owner);
         CoatGeometry coated = coat != 0 && dist < 160.0 ? HakiCoatRenderer.begin() : null;
         if (!firstPerson && headVisible) {
            this.drawHead(owner, partial, hx, hy, hz, cam, body, poseStack, buffers, overlay, coated, coat);
         }

         if (coated != null) {
            this.drawCoat(coated, coat, owner, partial, poseStack, cam, body, buffers, overlay, dist);
         }
      }
   }

   private boolean visible(Vec3 cam, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
      return this.cull
         .testAab((float)(minX - cam.x), (float)(minY - cam.y), (float)(minZ - cam.z), (float)(maxX - cam.x), (float)(maxY - cam.y), (float)(maxZ - cam.z));
   }

   private static int sections(double dist, float length) {
      int base = Math.min(160, Math.round((float)baseSections() * Math.max(1.0F, length / 60.0F)));
      if (dist > 128.0) {
         return Math.max(16, base / 2);
      } else {
         return dist > 64.0 ? Math.max(16, base * 3 / 4) : base;
      }
   }

   private static int baseSections() {
      return switch ((SeiryuClientConfig.Detail)SeiryuClientConfig.DRAGON_DETAIL.get()) {
         case LOW -> 32;
         case MEDIUM -> 48;
         case HIGH -> 64;
         case AUTO -> {
            switch ((CoreClientConfig.VfxQuality)CoreClientConfig.VFX_QUALITY.get()) {
               case LOW:
                  yield 32;
               case MEDIUM:
                  yield 48;
               case HIGH:
                  yield 64;
               default:
                  throw new MatchException(null, null);
            }
         }
      };
   }

   private void drawTube(VertexConsumer vc, Matrix4f view, Matrix3f viewNormal, Vec3 cam, int overlay, int from, int to) {
      int n = this.frames.count;
      BodyFrames f = this.frames;

      for (int i = Math.max(0, from); i + 1 < Math.min(n, to); i++) {
         if (this.visible[i] || this.visible[i + 1]) {
            float v0 = f.distance[i] / 6.0F;
            float v1 = f.distance[i + 1] / 6.0F;
            this.face(vc, view, viewNormal, cam, i, 0, 1, 0.0F, 0.25F, v0, v1, overlay, f.ux, f.uy, f.uz, 1.0F);
            this.face(vc, view, viewNormal, cam, i, 1, 2, 0.25F, 0.5F, v0, v1, overlay, f.sx, f.sy, f.sz, 1.0F);
            this.face(vc, view, viewNormal, cam, i, 2, 3, 0.5F, 0.75F, v0, v1, overlay, f.ux, f.uy, f.uz, -1.0F);
            this.face(vc, view, viewNormal, cam, i, 3, 0, 0.75F, 1.0F, v0, v1, overlay, f.sx, f.sy, f.sz, -1.0F);
         }
      }

      if (to >= n) {
         this.cap(vc, view, viewNormal, cam, n - 1, overlay);
      }
   }

   private void drawCoat(
      CoatGeometry g,
      int coat,
      Player owner,
      float partial,
      PoseStack poseStack,
      Vec3 cam,
      SerpentBody body,
      MultiBufferSource buffers,
      int overlay,
      double dist
   ) {
      int n = this.frames.count;
      Matrix4f view = poseStack.last().pose();
      Matrix3f viewNormal = poseStack.last().normal();
      boolean[] parts = this.coatParts(coat);
      if (parts[0]) {
         this.drawTube(g, view, viewNormal, cam, overlay, 0, n);
      } else if (parts[1]) {
         this.drawTube(g, view, viewNormal, cam, overlay, n - Math.max(2, n / 5), n);
      }

      if (parts[2]) {
         this.legs(g, poseStack, cam, this.section(body.length * 0.16F), this.legFront, overlay);
      }

      if (parts[3]) {
         this.legs(g, poseStack, cam, this.section(body.length * 0.58F), this.legBack, overlay);
      }

      int e = n - 1;
      if (parts[1] && (this.visible[e] || this.visible[Math.max(0, e - 1)])) {
         this.place(poseStack, cam, e, 0.0F, 0.0F, 1.0F, false);
         this.part(g, this.tailTip, this.light[e], overlay);
      }

      HakiCoatRenderer.passes(
         buffers,
         g,
         HakiRenderTypes.COAT_TILE,
         dist < 64.0 ? HakiRenderTypes.OUTLINE_TILE : null,
         HakiClientState.progress(owner, partial),
         this.light[Math.min(n - 1, n / 2)],
         0.0F,
         0.06F
      );
   }

   private boolean[] coatParts(int mask) {
      return (boolean[])this.coatParts
         .computeIfAbsent(
            mask,
            m -> {
               List<String> names = HakiFormParts.parts(UoUoSeiryu.ID, "dragon", m);
               return new boolean[]{
                  names.contains("body"),
                  names.contains("tail"),
                  names.contains("leg_front"),
                  names.contains("leg_back"),
                  names.contains("head") || names.contains("jaw")
               };
            }
         );
   }

   private void face(
      VertexConsumer vc,
      Matrix4f view,
      Matrix3f viewNormal,
      Vec3 cam,
      int i,
      int a,
      int b,
      float u0,
      float u1,
      float v0,
      float v1,
      int overlay,
      float[] nx,
      float[] ny,
      float[] nz,
      float sign
   ) {
      this.vertex(vc, view, viewNormal, cam, i, a, u0, v0, overlay, nx, ny, nz, sign);
      this.vertex(vc, view, viewNormal, cam, i + 1, a, u0, v1, overlay, nx, ny, nz, sign);
      this.vertex(vc, view, viewNormal, cam, i + 1, b, u1, v1, overlay, nx, ny, nz, sign);
      this.vertex(vc, view, viewNormal, cam, i, b, u1, v0, overlay, nx, ny, nz, sign);
   }

   private void vertex(
      VertexConsumer vc,
      Matrix4f view,
      Matrix3f viewNormal,
      Vec3 cam,
      int i,
      int corner,
      float u,
      float v,
      int overlay,
      float[] nx,
      float[] ny,
      float[] nz,
      float sign
   ) {
      BodyFrames f = this.frames;
      float w = f.radius[i] * 1.05F;
      float h = f.radius[i] * 0.95F;
      float su = corner != 0 && corner != 1 ? -h : h;
      float ss = corner != 1 && corner != 2 ? -w : w;
      float x = (float)(f.x[i] - cam.x) + f.ux[i] * su + f.sx[i] * ss;
      float y = (float)(f.y[i] - cam.y) + f.uy[i] * su + f.sy[i] * ss;
      float z = (float)(f.z[i] - cam.z) + f.uz[i] * su + f.sz[i] * ss;
      view.transformPosition(x, y, z, this.pos);
      viewNormal.transform(nx[i] * sign, ny[i] * sign, nz[i] * sign, this.nrm);
      vc.addVertex(this.pos.x, this.pos.y, this.pos.z, -1, u, v, overlay, this.light[i], this.nrm.x, this.nrm.y, this.nrm.z);
   }

   private void cap(VertexConsumer vc, Matrix4f view, Matrix3f viewNormal, Vec3 cam, int i, int overlay) {
      BodyFrames f = this.frames;

      for (int k = 0; k < 4; k++) {
         int c = CAP_ORDER[k];
         float w = f.radius[i] * 1.05F;
         float h = f.radius[i] * 0.95F;
         float su = c != 0 && c != 1 ? -h : h;
         float ss = c != 1 && c != 2 ? -w : w;
         float x = (float)(f.x[i] - cam.x) + f.ux[i] * su + f.sx[i] * ss;
         float y = (float)(f.y[i] - cam.y) + f.uy[i] * su + f.sy[i] * ss;
         float z = (float)(f.z[i] - cam.z) + f.uz[i] * su + f.sz[i] * ss;
         view.transformPosition(x, y, z, this.pos);
         viewNormal.transform(-f.tx[i], -f.ty[i], -f.tz[i], this.nrm);
         vc.addVertex(
            this.pos.x, this.pos.y, this.pos.z, -1, 0.5F + (ss > 0.0F ? 0.01F : 0.0F), 0.0F, overlay, this.light[i], this.nrm.x, this.nrm.y, this.nrm.z
         );
      }
   }

   private void drawDetails(MultiBufferSource buffers, PoseStack poseStack, Vec3 cam, SerpentBody body, int overlay, boolean near) {
      VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(PARTS_TEXTURE));
      BodyFrames f = this.frames;
      int n = f.count;
      float length = body.length;
      float step = near ? 2.2F : 4.4F;

      for (float d = length * 0.03F; d < length * 0.9F; d += step) {
         int i = this.section(d);
         if (this.visible[i]) {
            this.place(poseStack, cam, i, 0.0F, 0.874F, 1.0F, false);
            this.part(vc, this.tuft, this.light[i], overlay);
         }
      }

      this.legs(vc, poseStack, cam, this.section(length * 0.16F), this.legFront, overlay);
      this.legs(vc, poseStack, cam, this.section(length * 0.58F), this.legBack, overlay);
      int e = n - 1;
      if (this.visible[e] || this.visible[Math.max(0, e - 1)]) {
         this.place(poseStack, cam, e, 0.0F, 0.0F, 1.0F, false);
         this.part(vc, this.tailTip, this.light[e], overlay);
      }
   }

   private void legs(VertexConsumer vc, PoseStack poseStack, Vec3 cam, int i, int bone, int overlay) {
      if (this.visible[i]) {
         this.place(poseStack, cam, i, 0.84F, -0.33249998F, 1.0F, false);
         this.part(vc, bone, this.light[i], overlay);
         this.place(poseStack, cam, i, -0.84F, -0.33249998F, 1.0F, true);
         this.part(vc, bone, this.light[i], overlay);
      }
   }

   private int section(float distance) {
      BodyFrames f = this.frames;
      float span = f.distance[f.count - 1] - f.distance[0];
      return Mth.clamp(Math.round(distance / span * (float)(f.count - 1)), 0, f.count - 1);
   }

   private void place(PoseStack poseStack, Vec3 cam, int i, float side, float up, float scale, boolean mirror) {
      BodyFrames f = this.frames;
      float r = f.radius[i];
      float x = (float)(f.x[i] - cam.x) + (f.ux[i] * up + f.sx[i] * side) * r;
      float y = (float)(f.y[i] - cam.y) + (f.uy[i] * up + f.sy[i] * side) * r;
      float z = (float)(f.z[i] - cam.z) + (f.uz[i] * up + f.sz[i] * side) * r;
      float s = 0.08125F * scale * Mth.clamp(r / 1.3F, 0.35F, 1.1F);
      this.matrix.set(f.sx[i], f.sy[i], f.sz[i], 0.0F, f.ux[i], f.uy[i], f.uz[i], 0.0F, -f.tx[i], -f.ty[i], -f.tz[i], 0.0F, x, y, z, 1.0F);
      Pose pose = this.partStack.last();
      pose.pose().set(poseStack.last().pose()).mul(this.matrix).scale(mirror ? -s : s, s, s);
      pose.normal().set(poseStack.last().normal()).mul(this.normalMatrix.set(this.matrix));
      if (mirror) {
         pose.normal().scale(-1.0F, 1.0F, 1.0F);
      }

      this.partsPose.reset();
      this.parts.pose(pose, this.partsPose);
   }

   private void part(VertexConsumer vc, int bone, int light, int overlay) {
      if (bone >= 0) {
         this.parts.renderBone(vc, this.parts.model().bones[bone], light, overlay, -1);
      }
   }

   private void drawHead(
      Player owner,
      float partial,
      double hx,
      double hy,
      double hz,
      Vec3 cam,
      SerpentBody body,
      PoseStack poseStack,
      MultiBufferSource buffers,
      int overlay,
      @Nullable CoatGeometry coated,
      int coat
   ) {
      float yaw = Mth.rotLerp(partial, owner.yRotO, owner.getYRot());
      float pitch = owner.getViewXRot(partial);
      poseStack.pushPose();
      poseStack.translate(hx - cam.x, hy - cam.y + (double)body.anchorHeight, hz - cam.z);
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
      poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.clamp(pitch, -70.0F, 70.0F)));
      poseStack.scale(0.09375F, 0.09375F, 0.09375F);
      this.headPose.reset();
      if (AnimationController.current(owner, partial, this.clipState)) {
         this.addClip(this.clipState.clip, this.clipState.time, this.clipState.weight);
         this.addClip(this.clipState.previous, this.clipState.previousTime, this.clipState.previousWeight);
      }

      float age = (float)owner.tickCount + partial;

      for (int i = 0; i < this.whiskers.length; i++) {
         float sway = Mth.sin(age * 0.09F + (float)i * 0.8F) * 0.12F;
         this.headPose.addRotation(this.whiskers[i], sway * 0.5F, sway, 0.0F);
      }

      this.head.pose(poseStack.last(), this.headPose);
      this.blockPos.set(hx, hy + (double)body.anchorHeight, hz);
      int light = LevelRenderer.getLightColor(owner.level(), this.blockPos);
      this.head.render(buffers.getBuffer(RenderType.entityCutoutNoCull(HEAD_TEXTURE)), this.headPose, light, overlay, -1);
      GeoModel.Bone root = this.head.model().bones[0];
      this.head.renderBone(buffers.getBuffer(RenderType.eyes(HEAD_EMISSIVE)), root, light, OverlayTexture.NO_OVERLAY, -1);
      if (coated != null && this.coatParts(coat)[4]) {
         HakiCoatRenderer.geo(coated, this.head, this.headPose, UoUoSeiryu.ID, "dragon", coat, light);
      }

      poseStack.popPose();
   }

   private void addClip(@Nullable AnimationClip clip, float time, float weight) {
      if (clip instanceof GeoAnimation g && g.id.getPath().startsWith("uo_uo_seiryu/dragon/")) {
         g.add(this.head.model(), this.headPose, time, weight);
      }
   }
}
