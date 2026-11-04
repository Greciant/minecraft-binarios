package com.devilfruits.fruits.yomi_yomi.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.model.geo.GeoModel;
import com.devilfruits.client.model.geo.GeoPose;
import com.devilfruits.client.model.geo.GeoRenderer;
import com.devilfruits.client.model.geo.GeoResources;
import com.devilfruits.fruits.yomi_yomi.YomiItems;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.fruits.yomi_yomi.ability.YomiTiming;
import com.devilfruits.fruits.yomi_yomi.item.SoulCaneItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderLivingEvent.Post;
import net.neoforged.neoforge.client.event.RenderLivingEvent.Pre;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class SoulCaneRenderer extends BlockEntityWithoutLevelRenderer {
   private static final ResourceLocation MODEL = YomiYomi.id("soul_cane");
   private static final ResourceLocation TEXTURE = DevilFruits.id("textures/entity/yomi_yomi/soul_cane.png");
   private static final float GRIP = 32.0F;
   private static final float MOUTH = 29.5F;
   private static final float HAND_SCALE = 0.04375F;
   private static final Quaternionf TURN = new Quaternionf().rotateX((float) (Math.PI / 15)).rotateY(-0.4886922F).rotateZ((float) (Math.PI * 3.0 / 4.0));
   private static final float FILL = 0.96F;
   private static final Map<Integer, Long> FUBUKI = new HashMap<>();
   private static final float NOTO_OUT = 16.0F;
   private static final Quaternionf BLADE_REST = new Quaternionf().rotationZ((float) (Math.PI * 3.0 / 4.0));
   private static final Quaternionf BLADE_NOTO = new Quaternionf().rotationY(-1.4137167F).rotateZ(1.1170107F);
   private static final Quaternionf SCABBARD_REST = new Quaternionf().rotationZ((float) (-Math.PI * 4.0 / 9.0));
   private static final Quaternionf SCABBARD_NOTO = new Quaternionf().rotationY(1.3002703F).rotateZ(-0.94247776F);
   @Nullable
   private static Entity drawing;
   @Nullable
   private static EntityModel<?> drawingModel;
   private static final float ALIGN = 80.0F;
   private static final float CROOK_DOWN = 180.0F;
   private static final float STICK_TURN = 215.0F;
   private static final float SCABBARD_TURN = -80.0F;
   private static SoulCaneRenderer instance;
   private int generation = -1;
   @Nullable
   private GeoModel model;
   private GeoRenderer geo;
   private GeoPose pose;
   private final float[][] fits = new float[SoulCaneRenderer.Variant.values().length][];

   public static SoulCaneRenderer get() {
      if (instance == null) {
         Minecraft mc = Minecraft.getInstance();
         instance = new SoulCaneRenderer(mc);
      }

      return instance;
   }

   private SoulCaneRenderer(Minecraft mc) {
      super(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
   }

   public static void fubuki(int entityId) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         FUBUKI.put(entityId, mc.level.getGameTime() - 6L);
      }
   }

   public static void clear() {
      FUBUKI.clear();
      drawing = null;
   }

   private static float fubukiTime(@Nullable Entity e) {
      Minecraft mc = Minecraft.getInstance();
      if (e != null && mc.level != null) {
         Long start = FUBUKI.get(e.getId());
         if (start == null) {
            return -1.0F;
         } else {
            float t = (float)(mc.level.getGameTime() - start) + mc.getTimer().getGameTimeDeltaPartialTick(false);
            if (t > 42.0F) {
               FUBUKI.remove(e.getId());
               return -1.0F;
            } else {
               return t;
            }
         }
      } else {
         return -1.0F;
      }
   }

   private static float smooth(float k) {
      float c = Mth.clamp(k, 0.0F, 1.0F);
      return c * c * (3.0F - 2.0F * c);
   }

   public static void onRenderLiving(Pre<?, ?> event) {
      drawing = event.getEntity();
      drawingModel = event.getRenderer().getModel();
   }

   @Nullable
   public static Entity drawingEntity() {
      return drawing;
   }

   public static void onRenderLivingPost(Post<?, ?> event) {
      drawing = null;
      drawingModel = null;
   }

   private static float thrust(ItemDisplayContext context) {
      if ((context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
         && drawingModel instanceof HumanoidModel<?> humanoid) {
         boolean right = context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
         float raised = -(right ? humanoid.rightArm.xRot : humanoid.leftArm.xRot) * (180.0F / (float)Math.PI);
         return Mth.clamp((raised - 35.0F) / 50.0F, 0.0F, 1.0F);
      } else {
         return 0.0F;
      }
   }

   public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
      if (this.ready()) {
         boolean scabbard = stack.is((Item)YomiItems.SCABBARD.get());
         SoulCaneRenderer.Variant variant = scabbard
            ? SoulCaneRenderer.Variant.SCABBARD
            : (SoulCaneItem.isDrawn(stack) ? SoulCaneRenderer.Variant.BLADE : SoulCaneRenderer.Variant.CANE);
         boolean thirdPerson = context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
         boolean inHand = thirdPerson || context.firstPerson();
         float fubuki = context.firstPerson() ? fubukiTime(Minecraft.getInstance().player) : (thirdPerson ? fubukiTime(drawing) : -1.0F);
         if (fubuki >= 14.0F && (variant == SoulCaneRenderer.Variant.BLADE || variant == SoulCaneRenderer.Variant.SCABBARD)) {
            if (!context.firstPerson()) {
               this.noto(variant, fubuki, ps, buffers, light, overlay);
               return;
            }

            if (fubuki >= 27.0F && fubuki < 35.0F) {
               variant = variant == SoulCaneRenderer.Variant.BLADE ? SoulCaneRenderer.Variant.NONE : SoulCaneRenderer.Variant.CANE;
            }
         }

         if (variant != SoulCaneRenderer.Variant.NONE) {
            this.hide(variant);
            ps.pushPose();
            if (context == ItemDisplayContext.GUI) {
               float[] fit = this.fit(variant);
               ps.translate(0.5F, 0.5F, 0.5F);
               ps.scale(fit[3], fit[3], fit[3]);
               ps.translate(-fit[0], -fit[1], -fit[2]);
               ps.mulPose(TURN);
            } else if (variant != SoulCaneRenderer.Variant.SCABBARD && (!scabbard || variant != SoulCaneRenderer.Variant.CANE)) {
               boolean stick = variant == SoulCaneRenderer.Variant.CANE && inHand;
               float align = variant == SoulCaneRenderer.Variant.BLADE ? thrust(context) : 0.0F;
               align = align * align * (3.0F - 2.0F * align);
               ps.translate(0.25F, 0.25F, 0.5F);
               ps.mulPose(Axis.ZP.rotationDegrees(stick ? 215.0F : 135.0F + 80.0F * align));
               ps.scale(0.04375F, 0.04375F, 0.04375F);
               ps.translate(0.0F, -32.0F, 0.0F);
               if (inHand && !stick) {
                  ps.mulPose(Axis.YP.rotationDegrees(180.0F));
               }
            } else {
               ps.translate(0.25F, 0.25F, 0.5F);
               ps.mulPose(Axis.ZP.rotationDegrees(-80.0F));
               ps.scale(0.04375F, 0.04375F, 0.04375F);
               ps.translate(0.0F, -29.5F, 0.0F);
               if (variant == SoulCaneRenderer.Variant.CANE) {
                  ps.mulPose(Axis.YP.rotationDegrees(180.0F));
               }
            }

            this.geo.pose(ps.last(), this.pose);
            this.geo.render(buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), this.pose, light, overlay, -1);
            ps.popPose();
         }
      }
   }

   private void noto(SoulCaneRenderer.Variant variant, float t, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
      boolean sliding = t >= 17.0F && t < 37.0F;
      Quaternionf turn = new Quaternionf();
      if (variant == SoulCaneRenderer.Variant.BLADE) {
         if (!sliding) {
            float k = t < 17.0F ? smooth((t - 14.0F) / 3.0F) : 1.0F - smooth((t - 37.0F) / 3.0F);
            BLADE_REST.slerp(BLADE_NOTO, k, turn);
            this.hide(SoulCaneRenderer.Variant.BLADE);
            this.draw(ps, buffers, light, overlay, turn, 32.0F, true);
         }
      } else {
         float k = t < 17.0F ? smooth((t - 14.0F) / 3.0F) : (t < 37.0F ? 1.0F : 1.0F - smooth((t - 37.0F) / 3.0F));
         SCABBARD_REST.slerp(SCABBARD_NOTO, k, turn);
         if (sliding) {
            this.pose.reset();
            float out = (1.0F - YomiTiming.notoInsert(t)) * 16.0F;
            int blade = this.model.bone("blade");
            int handle = this.model.bone("handle");
            if (blade >= 0) {
               this.pose.addPosition(blade, 0.0F, out, 0.0F);
            }

            if (handle >= 0) {
               this.pose.addPosition(handle, 0.0F, out, 0.0F);
            }
         } else {
            this.hide(SoulCaneRenderer.Variant.SCABBARD);
         }

         this.draw(ps, buffers, light, overlay, turn, 29.5F, sliding);
      }
   }

   private void draw(PoseStack ps, MultiBufferSource buffers, int light, int overlay, Quaternionf turn, float origin, boolean handle) {
      ps.pushPose();
      ps.translate(0.25F, 0.25F, 0.5F);
      ps.mulPose(turn);
      ps.scale(0.04375F, 0.04375F, 0.04375F);
      ps.translate(0.0F, -origin, 0.0F);
      if (handle) {
         ps.mulPose(Axis.YP.rotationDegrees(180.0F));
      }

      this.geo.pose(ps.last(), this.pose);
      this.geo.render(buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), this.pose, light, overlay, -1);
      ps.popPose();
   }

   private boolean ready() {
      int gen = GeoResources.MODELS.generation();
      if (gen != this.generation) {
         this.generation = gen;
         this.model = GeoResources.model(MODEL);
         Arrays.fill(this.fits, null);
         if (this.model != null) {
            this.geo = new GeoRenderer(this.model);
            this.pose = new GeoPose(this.model);
         }
      }

      return this.model != null;
   }

   private void hide(SoulCaneRenderer.Variant variant) {
      this.pose.reset();
      int scabbard = this.model.bone("scabbard");
      int blade = this.model.bone("blade");
      int handle = this.model.bone("handle");
      if (variant == SoulCaneRenderer.Variant.BLADE && scabbard >= 0) {
         this.pose.hidden[scabbard] = true;
      } else if (variant == SoulCaneRenderer.Variant.SCABBARD) {
         if (blade >= 0) {
            this.pose.hidden[blade] = true;
         }

         if (handle >= 0) {
            this.pose.hidden[handle] = true;
         }
      }
   }

   private float[] fit(SoulCaneRenderer.Variant variant) {
      float[] fit = this.fits[variant.ordinal()];
      if (fit != null) {
         return fit;
      } else {
         float[] min = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
         float[] max = new float[]{-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
         Vector3f p = new Vector3f();

         for (GeoModel.Bone bone : this.model.bones) {
            if (!this.pose.hidden[bone.index]) {
               for (int v = 0; v < bone.mesh.length; v += 8) {
                  TURN.transform(p.set(bone.mesh[v], bone.mesh[v + 1], bone.mesh[v + 2]));
                  min[0] = Math.min(min[0], p.x);
                  min[1] = Math.min(min[1], p.y);
                  min[2] = Math.min(min[2], p.z);
                  max[0] = Math.max(max[0], p.x);
                  max[1] = Math.max(max[1], p.y);
                  max[2] = Math.max(max[2], p.z);
               }
            }
         }

         float size = Math.max(1.0F, Math.max(max[0] - min[0], max[1] - min[1]));
         fit = new float[]{(min[0] + max[0]) / 2.0F, (min[1] + max[1]) / 2.0F, (min[2] + max[2]) / 2.0F, 0.96F / size};
         this.fits[variant.ordinal()] = fit;
         return fit;
      }
   }

   private static enum Variant {
      CANE,
      BLADE,
      SCABBARD,
      NONE;
   }
}
