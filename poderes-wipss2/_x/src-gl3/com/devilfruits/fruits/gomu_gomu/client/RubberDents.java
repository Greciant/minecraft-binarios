package com.devilfruits.fruits.gomu_gomu.client;

import com.devilfruits.client.limb.LimbSpec;
import com.devilfruits.client.limb.SkinSize;
import com.devilfruits.client.limb.TexelBoxMesh;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.core.limb.LimbPath;
import com.devilfruits.core.limb.LimbUv;
import com.devilfruits.fruits.gomu_gomu.GomuNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Post;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class RubberDents {
   private static final float DEPTH = 3.0F;
   private static final float GROW_TICKS = 3.0F;
   private static final float SPRING_TICKS = 10.0F;
   private static final float SIGMA = 1.7F;
   private static final int PARTS = 6;
   private static final List<RubberDents.Dent> DENTS = new ArrayList<>();
   private static final boolean[] SAVED = new boolean[12];
   private static final boolean[] HIDDEN = new boolean[6];
   private static PlayerModel<?> hiddenOn;
   private static final TexelBoxMesh MESH = new TexelBoxMesh();
   private static final LimbPath PATH = new LimbPath();
   private static final LimbSpec SPEC = new LimbSpec();
   private static final float[] SEGMENT = new float[4];
   private static final Vector3f N0 = new Vector3f(1.0F, 0.0F, 0.0F);
   private static final Vector3f V = new Vector3f();
   private static final Vector3f Q = new Vector3f();
   private static final Quaternionf ROT = new Quaternionf();
   private static final Matrix4f TO_PX = new Matrix4f();
   private static RubberDents.Dent shaping;
   private static float shapeDepth;
   private static float shapeRipple;
   private static float shapeFront;
   private static final TexelBoxMesh.VertexShaper SHAPER = new TexelBoxMesh.VertexShaper() {
      private float dx;
      private float dy;
      private float dz;
      private float push;

      @Override
      public void shape(int face, float s, Vector3f pos) {
         RubberDents.Dent d = RubberDents.shaping;
         this.dx = pos.x - d.hit.x;
         this.dy = pos.y - d.hit.y;
         this.dz = pos.z - d.hit.z;
         float r2 = this.dx * this.dx + this.dy * this.dy + this.dz * this.dz;
         this.push = RubberDents.shapeDepth * (float)Math.exp((double)(-r2 / 5.78F));
         if (RubberDents.shapeRipple > 0.0F) {
            float r = (float)Math.sqrt((double)r2);
            if (r < RubberDents.shapeFront) {
               this.push = this.push
                  + RubberDents.shapeRipple * Mth.sin(2.2F * (r - RubberDents.shapeFront)) * (float)Math.exp((double)(-(RubberDents.shapeFront - r) * 0.6F));
            }
         }

         pos.x = pos.x + d.in.x * this.push;
         pos.y = pos.y + d.in.y * this.push;
         pos.z = pos.z + d.in.z * this.push;
      }

      @Override
      public float light(Vector3f normal) {
         if (Math.abs(this.push) < 0.02F) {
            return 1.0F;
         } else {
            float along = this.dx * normal.x + this.dy * normal.y + this.dz * normal.z;
            float tx = this.dx - along * normal.x;
            float ty = this.dy - along * normal.y;
            float tz = this.dz - along * normal.z;
            float slope = this.push / 2.89F;
            normal.sub(tx * slope, ty * slope, tz * slope).normalize();
            return 1.0F - 0.42F * Mth.clamp(this.push / 3.0F, 0.0F, 1.0F);
         }
      }
   };

   public static void init(IEventBus modBus) {
      modBus.addListener(RubberDents::addLayers);
      NeoForge.EVENT_BUS.addListener(EventPriority.LOW, RubberDents::onRenderPlayerPre);
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, RubberDents::onRenderPlayerPost);
      NeoForge.EVENT_BUS.addListener(e -> tick());
      NeoForge.EVENT_BUS.addListener(e -> DENTS.clear());
   }

   public static void accept(GomuNetwork.BulletAbsorbPayload payload) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         DENTS.add(new RubberDents.Dent(payload));
      }
   }

   private static void tick() {
      Minecraft mc = Minecraft.getInstance();
      if (!DENTS.isEmpty() && mc.level != null) {
         float now = (float)mc.level.getGameTime();
         DENTS.removeIf(d -> {
            Entity entity = mc.level.getEntity(d.entity);
            if (entity != null && !d.over(now)) {
               if (!d.ejected && now >= (float)(d.start + (long)d.hold)) {
                  d.ejected = true;
                  Vec3 at = entity.position().add(d.offset);
                  ShockRing.at(at, d.dir).radius(0.04F, 0.5F).life(6).strength(0.35F).rim(0.35F, 16773598).spawn();
                  mc.level.addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, -d.dir.x * 0.05, -d.dir.y * 0.05, -d.dir.z * 0.05);
               }

               return false;
            } else {
               return true;
            }
         });
      }
   }

   private static void addLayers(AddLayers event) {
      for (Model skin : event.getSkins()) {
         if (event.getSkin(skin) instanceof PlayerRenderer r) {
            r.addLayer(new RubberDents.Layer(r));
         }
      }
   }

   private static void onRenderPlayerPre(Pre event) {
      if (!DENTS.isEmpty()) {
         Arrays.fill(HIDDEN, false);
         int id = event.getEntity().getId();
         boolean any = false;

         for (RubberDents.Dent d : DENTS) {
            any |= d.entity == id && d.part >= 0;
         }

         if (any) {
            PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)event.getRenderer().getModel();
            hiddenOn = model;

            for (int i = 0; i < 6; i++) {
               SAVED[i * 2] = part(model, i, false).visible;
               SAVED[i * 2 + 1] = part(model, i, true).visible;
            }

            for (RubberDents.Dent d : DENTS) {
               if (d.entity == id && d.part >= 0 && part(model, d.part, false).visible) {
                  HIDDEN[d.part] = true;
               }
            }

            for (int i = 0; i < 6; i++) {
               if (HIDDEN[i]) {
                  part(model, i, false).visible = false;
                  part(model, i, true).visible = false;
               }
            }
         }
      }
   }

   private static void onRenderPlayerPost(Post event) {
      if (hiddenOn == event.getRenderer().getModel()) {
         for (int i = 0; i < 6; i++) {
            part(hiddenOn, i, false).visible = SAVED[i * 2];
            part(hiddenOn, i, true).visible = SAVED[i * 2 + 1];
         }

         hiddenOn = null;
      }
   }

   static ModelPart part(PlayerModel<?> m, int i, boolean overlay) {
      return switch (i) {
         case 0 -> overlay ? m.hat : m.head;
         case 1 -> overlay ? m.jacket : m.body;
         case 2 -> overlay ? m.rightSleeve : m.rightArm;
         case 3 -> overlay ? m.leftSleeve : m.leftArm;
         case 4 -> overlay ? m.rightPants : m.rightLeg;
         default -> overlay ? m.leftPants : m.leftLeg;
      };
   }

   private static PlayerModelPart overlayPart(int i) {
      return switch (i) {
         case 0 -> PlayerModelPart.HAT;
         case 1 -> PlayerModelPart.JACKET;
         case 2 -> PlayerModelPart.RIGHT_SLEEVE;
         case 3 -> PlayerModelPart.LEFT_SLEEVE;
         case 4 -> PlayerModelPart.RIGHT_PANTS_LEG;
         default -> PlayerModelPart.LEFT_PANTS_LEG;
      };
   }

   @Nullable
   static LimbUv.Layout layout(int i, boolean slim, int w, int h, boolean overlay) {
      LimbUv.Layer layer = overlay ? LimbUv.Layer.OVERLAY : LimbUv.Layer.SKIN;
      LimbUv.Model model = slim ? LimbUv.Model.SLIM : LimbUv.Model.CLASSIC;
      float scale = (float)w / 64.0F;

      return switch (i) {
         case 0 -> new LimbUv.Layout(w, h, scale, overlay ? 32 : 0, 0, 8, 8, -4.0F, -8.0F, -4.0F, 0.0F, 0.0F, 0.0F, overlay ? 0.5F : 0.0F, false);
         case 1 -> overlay && LimbUv.legacy(w, h)
         ? null
         : new LimbUv.Layout(w, h, scale, 16, overlay ? 32 : 16, 8, 4, -4.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, overlay ? 0.25F : 0.0F, false);
         case 2 -> LimbUv.resolve(model, LimbUv.Side.RIGHT, LimbUv.Limb.ARM, w, h, layer);
         case 3 -> LimbUv.resolve(model, LimbUv.Side.LEFT, LimbUv.Limb.ARM, w, h, layer);
         case 4 -> LimbUv.resolve(model, LimbUv.Side.RIGHT, LimbUv.Limb.LEG, w, h, layer);
         default -> LimbUv.resolve(model, LimbUv.Side.LEFT, LimbUv.Limb.LEG, w, h, layer);
      };
   }

   static float height(int i) {
      return i == 0 ? 8.0F : 12.0F;
   }

   static float depth(float t, int hold) {
      if (t < 0.0F) {
         return 0.0F;
      } else if (t < (float)hold) {
         float g = Mth.clamp(t / 3.0F, 0.0F, 1.0F);
         return 3.0F * g * g * (3.0F - 2.0F * g);
      } else {
         float u = t - (float)hold;
         return 3.0F * (float)(Math.exp((double)(-u) * 0.45) * Math.cos((double)u * 0.75));
      }
   }

   private static void resolve(RubberDents.Dent d, PoseStack pose, PlayerModel<?> model, AbstractClientPlayer player, float partial) {
      Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
      Vec3 at = player.getPosition(partial).add(d.offset).subtract(cam);
      TO_PX.set(pose.last().pose()).scale(0.0625F).invert();
      Vector3f local = TO_PX.transformPosition((float)at.x, (float)at.y, (float)at.z, new Vector3f());
      Vector3f way = TO_PX.transformDirection((float)d.dir.x, (float)d.dir.y, (float)d.dir.z, new Vector3f()).normalize();
      boolean slim = player.getSkin().model() == Model.SLIM;
      int best = -1;
      float bestDist = Float.MAX_VALUE;

      for (int i = 0; i < 6; i++) {
         ModelPart part = part(model, i, false);
         LimbUv.Layout box = layout(i, slim, 64, 64, false);
         toPart(part, local, Q);
         float dist = outside(box, height(i), Q);
         if (dist < bestDist) {
            bestDist = dist;
            best = i;
         }
      }

      ModelPart part = part(model, best, false);
      toPart(part, local, d.hit);
      snap(layout(best, slim, 64, 64, false), height(best), d.hit);
      ROT.rotationZYX(part.zRot, part.yRot, part.xRot).conjugate().transform(way, d.in);
      d.part = best;
   }

   private static void toPart(ModelPart part, Vector3f p, Vector3f out) {
      out.set(p).sub(part.x, part.y, part.z);
      ROT.rotationZYX(part.zRot, part.yRot, part.xRot).conjugate().transform(out);
   }

   private static float outside(LimbUv.Layout b, float h, Vector3f q) {
      float dx = Math.max(Math.max(b.minX() - q.x, 0.0F), q.x - (b.minX() + (float)b.width()));
      float dy = Math.max(Math.max(b.minY() - q.y, 0.0F), q.y - (b.minY() + h));
      float dz = Math.max(Math.max(b.minZ() - q.z, 0.0F), q.z - (b.minZ() + (float)b.depth()));
      return dx * dx + dy * dy + dz * dz;
   }

   private static void snap(LimbUv.Layout b, float h, Vector3f q) {
      float x0 = b.minX();
      float x1 = x0 + (float)b.width();
      float y0 = b.minY();
      float y1 = y0 + h;
      float z0 = b.minZ();
      float z1 = z0 + (float)b.depth();
      q.set(Mth.clamp(q.x, x0, x1), Mth.clamp(q.y, y0, y1), Mth.clamp(q.z, z0, z1));
      float[] gaps = new float[]{q.x - x0, x1 - q.x, q.y - y0, y1 - q.y, q.z - z0, z1 - q.z};
      int nearest = 0;

      for (int k = 1; k < 6; k++) {
         if (gaps[k] < gaps[nearest]) {
            nearest = k;
         }
      }

      switch (nearest) {
         case 0:
            q.x = x0;
            break;
         case 1:
            q.x = x1;
            break;
         case 2:
            q.y = y0;
            break;
         case 3:
            q.y = y1;
            break;
         case 4:
            q.z = z0;
            break;
         default:
            q.z = z1;
      }
   }

   private static void draw(
      RubberDents.Dent d,
      PoseStack pose,
      MultiBufferSource buffers,
      int light,
      AbstractClientPlayer player,
      PlayerModel<?> model,
      ResourceLocation texture,
      float now
   ) {
      int[] size = SkinSize.of(texture);
      boolean slim = player.getSkin().model() == Model.SLIM;
      LimbUv.Layout skin = layout(d.part, slim, size[0], size[1], false);
      LimbUv.Layout over = player.isModelPartShown(overlayPart(d.part)) ? layout(d.part, slim, size[0], size[1], true) : null;
      if (skin != null) {
         float t = now - (float)d.start;
         shaping = d;
         shapeDepth = depth(t, d.hold);
         float since = t < (float)d.hold ? t : t - (float)d.hold;
         shapeRipple = since < 8.0F ? 0.45F * (float)Math.exp((double)(-since * 0.35F)) : 0.0F;
         shapeFront = 0.9F * since + 0.5F;
         float h = height(d.part);
         pose.pushPose();
         part(model, d.part, false).translateAndRotate(pose);
         pose.scale(0.0625F, 0.0625F, 0.0625F);
         int overlayCoords = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
         sweep(skin, h, light);
         MESH.write(pose.last(), buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), skin, overlayCoords, -1, SHAPER, true);
         if (over != null) {
            sweep(over, h, light);
            MESH.write(pose.last(), buffers.getBuffer(RenderType.entityTranslucent(texture)), over, overlayCoords, -1, SHAPER, true);
         }

         pose.popPose();
      }
   }

   private static void sweep(LimbUv.Layout box, float h, int light) {
      float cx = box.minX() + (float)box.width() * 0.5F;
      float cz = box.minZ() + (float)box.depth() * 0.5F;
      PATH.reset().add(cx, box.minY(), cz).add(cx, box.minY() + h, cz).startDirection(0.0F, 1.0F, 0.0F).endDirection(0.0F, 1.0F, 0.0F).build();
      SEGMENT[0] = 0.0F;
      SEGMENT[1] = h;
      SEGMENT[2] = 0.0F;
      SEGMENT[3] = h;
      MESH.sweep(PATH, SEGMENT, 1, 1.0F, N0, SPEC, light, light, light);
   }

   private RubberDents() {
   }

   static final class Dent {
      final int entity;
      final Vec3 offset;
      final Vec3 dir;
      final long start;
      final int hold;
      int part = -1;
      final Vector3f hit = new Vector3f();
      final Vector3f in = new Vector3f();
      boolean ejected;

      Dent(GomuNetwork.BulletAbsorbPayload p) {
         this.entity = p.entity();
         this.offset = p.offset();
         this.dir = p.dir();
         this.start = p.start();
         this.hold = p.hold();
      }

      boolean over(float now) {
         return now > (float)(this.start + (long)this.hold) + 10.0F;
      }
   }

   static final class Layer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
      Layer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
         super(parent);
      }

      public void render(
         PoseStack pose,
         MultiBufferSource buffers,
         int light,
         AbstractClientPlayer player,
         float limbSwing,
         float limbSwingAmount,
         float partialTick,
         float ageInTicks,
         float netHeadYaw,
         float headPitch
      ) {
         if (!RubberDents.DENTS.isEmpty() && !player.isInvisible()) {
            PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)this.getParentModel();
            float now = (float)player.level().getGameTime() + partialTick;
            ResourceLocation texture = this.getTextureLocation(player);
            int drawn = 0;

            for (int i = RubberDents.DENTS.size() - 1; i >= 0; i--) {
               RubberDents.Dent d = RubberDents.DENTS.get(i);
               if (d.entity == player.getId()) {
                  if (d.part < 0) {
                     RubberDents.resolve(d, pose, model, player, partialTick);
                  } else if (RubberDents.hiddenOn == model && RubberDents.HIDDEN[d.part] && (drawn & 1 << d.part) == 0) {
                     drawn |= 1 << d.part;
                     RubberDents.draw(d, pose, buffers, light, player, model, texture, now);
                  }
               }
            }
         }
      }
   }
}
