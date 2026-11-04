package com.devilfruits.fruits.yomi_yomi.client.vfx;

import com.devilfruits.fruits.yomi_yomi.Vessel;
import com.devilfruits.fruits.yomi_yomi.client.SkeletonBody;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Post;

public final class Afterimages {
   private static final List<Afterimages.Ghost> GHOSTS = new ArrayList<>();

   public static void add(Entity e, int life, int rgb) {
      if (e.level() != null) {
         float yaw = e instanceof LivingEntity l ? l.yBodyRot : e.getYRot();
         GHOSTS.add(new Afterimages.Ghost(e.getId(), e.position(), yaw, e.level().getGameTime(), life, rgb));
      }
   }

   public static void clear() {
      GHOSTS.clear();
   }

   public static void onRenderPlayer(Post event) {
      if (!GHOSTS.isEmpty()) {
         AbstractClientPlayer p = (AbstractClientPlayer)event.getEntity();
         long now = p.level().getGameTime();
         GHOSTS.removeIf(gx -> now - gx.born > (long)gx.life);
         float partial = event.getPartialTick();
         Vec3 here = new Vec3(Mth.lerp((double)partial, p.xo, p.getX()), Mth.lerp((double)partial, p.yo, p.getY()), Mth.lerp((double)partial, p.zo, p.getZ()));
         PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)event.getRenderer().getModel();
         PoseStack ps = event.getPoseStack();
         boolean skeleton = SkeletonBody.shown(p) == Vessel.SKELETON;
         VertexConsumer vc = null;

         for (Afterimages.Ghost g : GHOSTS) {
            if (g.entity == p.getId()) {
               Vec3 d = g.pos.subtract(here);
               if (!(d.lengthSqr() < 0.09)) {
                  float age = ((float)(now - g.born) + partial) / (float)g.life;
                  int a = Math.round(85.0F * Mth.clamp(1.0F - age, 0.0F, 1.0F));
                  if (a > 2) {
                     ps.pushPose();
                     ps.translate(d.x, d.y, d.z);
                     ps.mulPose(Axis.YP.rotationDegrees(180.0F - g.yaw));
                     ps.scale(-0.9375F, -0.9375F, 0.9375F);
                     ps.translate(0.0F, -1.501F, 0.0F);
                     if (skeleton) {
                        SkeletonBody.renderGhost(model, ps, event.getMultiBufferSource(), a << 24 | g.rgb);
                     } else {
                        if (vc == null) {
                           vc = event.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(event.getRenderer().getTextureLocation(p)));
                        }

                        model.renderToBuffer(ps, vc, 15728880, OverlayTexture.NO_OVERLAY, a << 24 | g.rgb);
                     }

                     ps.popPose();
                  }
               }
            }
         }
      }
   }

   private Afterimages() {
   }

   private static record Ghost(int entity, Vec3 pos, float yaw, long born, int life, int rgb) {
   }
}
