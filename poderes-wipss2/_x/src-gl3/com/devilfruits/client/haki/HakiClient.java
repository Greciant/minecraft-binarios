package com.devilfruits.client.haki;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.logia.LogiaRenderState;
import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.CameraEffects;
import com.devilfruits.client.render.vfx.ShockwaveEffect;
import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.core.haki.HakiFx;
import com.devilfruits.core.haki.HakiManager;
import com.devilfruits.core.haki.HakiParts;
import com.devilfruits.core.network.FxPayload;
import com.devilfruits.core.network.LogiaHitPayload;
import com.devilfruits.registry.DFParticles;
import com.devilfruits.registry.DFSounds;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(
   modid = "devilfruits",
   value = {Dist.CLIENT}
)
public final class HakiClient {
   public static void init(IEventBus modBus, ModContainer container) {
      container.registerConfig(Type.CLIENT, HakiClientConfig.SPEC, "devilfruits-haki-client.toml");
      modBus.addListener(HakiClient::addLayers);
      modBus.addListener(e -> HakiParticleProviders.register(e));
      modBus.addListener(e -> e.registerAbove(VanillaGuiLayers.HOTBAR, DevilFruits.id("haki"), HakiHud::render));
      ClientFxRegistry.register(HakiFx.COAT, HakiClient::coat);
      ClientFxRegistry.register(HakiFx.UNCOAT, (level, entity, p) -> SoundDesign.play(p.pos(), DFSounds.HAKI_UNCOAT, 0.6F, 1.0F));
      ClientFxRegistry.register(HakiFx.FLICKER, HakiClient::flicker);
      ClientFxRegistry.register(HakiFx.IMPACT, HakiClient::impact);
      ClientFxRegistry.register(HakiFx.CLASH, HakiClient::clash);
      ClientFxRegistry.register(HakiFx.RYUO, HakiClient::ryuo);
      ClientFxRegistry.register(HakiFx.LEVEL_UP, (level, entity, p) -> ripple(p.pos(), 3.5F, false));
      ClientFxRegistry.register(HakiFx.UNLOCK, (level, entity, p) -> ripple(p.pos(), p.power() > 1.5F ? 6.0F : 4.5F, true));
      LogiaRenderState.solidLimbs = p -> limbs(HakiClientState.shown(p));
      LogiaRenderState.hakiContact = HakiClient::contact;
      AutoTest.addScript(DevilFruits.id("haki"), new HakiAutotest());
   }

   private static void addLayers(AddLayers event) {
      for (Model model : event.getSkins()) {
         if (event.getSkin(model) instanceof LivingEntityRenderer<?, ?> renderer) {
            renderer.addLayer(new HakiCoatLayer((RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>)renderer));
         }
      }
   }

   @SubscribeEvent
   public static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      if (level == null) {
         HakiClientState.clear();
         HakiSkinCoat.clear();
      } else if (!mc.isPaused()) {
         HakiClientState.tick(level);
         HakiSkinCoat.tick();
         boolean embers = HakiClientConfig.QUALITY.get() == HakiClientConfig.Quality.HIGH;
         RandomSource r = Fx.random();

         for (AbstractClientPlayer player : level.players()) {
            int mask = HakiManager.mask(player);
            if (mask != 0 && !player.isInvisible()) {
               boolean ownFirstPerson = player == mc.player && mc.options.getCameraType().isFirstPerson();
               int charge = HakiManager.data(player).ryuoCharge();

               for (int side = 0; side < 2; side++) {
                  boolean right = side == 0;
                  if (HakiParts.fist(mask, right)) {
                     Vec3 hand = hand(player, right, ownFirstPerson);
                     if (embers && r.nextInt(charge > 0 ? 1 : 5) == 0) {
                        Fx.spawn(
                           (ParticleOptions)DFParticles.HAKI_EMBER.get(),
                           hand.x + Fx.spread(0.12),
                           hand.y + Fx.spread(0.1),
                           hand.z + Fx.spread(0.12),
                           Fx.spread(0.01),
                           0.02,
                           Fx.spread(0.01)
                        );
                     }

                     if (charge > 0 && right == (player.getMainArm() == HumanoidArm.RIGHT) && player.tickCount % 5 == 0) {
                        Vec3 to = hand.add(Fx.spread(0.9), Fx.spread(0.6), Fx.spread(0.9));
                        VfxManager.add(new BlackLightningEffect(hand, to, 3, 0.05F, 1, r.nextLong()));
                     }
                  }
               }
            }
         }
      }
   }

   private static void coat(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      SoundDesign.play(
         p.pos().add(0.0, 1.0, 0.0),
         DFSounds.HAKI_COAT,
         0.8F,
         0.95F + Fx.random().nextFloat() * 0.1F,
         SoundDesign.layer(DFSounds.HAKI_LAYER_ANVIL, 0.25F, 1.7F)
      );
      if (entity instanceof Player player) {
         for (int side = 0; side < 2; side++) {
            if (HakiParts.fist(p.extra(), side == 0)) {
               Vec3 hand = hand(player, side == 0, false);

               for (int i = 0; i < Fx.count(5); i++) {
                  Fx.spawn(
                     (ParticleOptions)DFParticles.HAKI_EMBER.get(),
                     hand.x + Fx.spread(0.2),
                     hand.y + Fx.spread(0.2),
                     hand.z + Fx.spread(0.2),
                     Fx.spread(0.03),
                     0.04,
                     Fx.spread(0.03)
                  );
               }
            }
         }
      }
   }

   private static void flicker(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (entity != null) {
         HakiClientState.flicker(entity.getId());
      }

      SoundDesign.play(p.pos().add(0.0, 1.0, 0.0), DFSounds.HAKI_FLICKER, 0.7F, 1.0F);
      RandomSource r = Fx.random();

      for (int i = 0; i < Fx.count(8); i++) {
         Fx.spawn(
            (ParticleOptions)DFParticles.HAKI_SHARD.get(),
            p.pos().x + Fx.spread(0.5),
            p.pos().y + 0.6 + r.nextDouble() * 0.8,
            p.pos().z + Fx.spread(0.5),
            Fx.spread(0.06),
            0.05,
            Fx.spread(0.06)
         );
      }
   }

   private static void impact(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      Vec3 at = p.pos();
      Vec3 dir = p.dir();
      RandomSource r = Fx.random();
      float strength = Mth.clamp(p.power() / 12.0F, 0.3F, 1.4F);
      VfxManager.add(new ImpactFlashEffect(at, 0.35F + 0.25F * strength, r.nextFloat()));

      for (int i = 0; i < Fx.count(6 + (int)(strength * 6.0F)); i++) {
         double s = 0.12 + r.nextDouble() * 0.2;
         Fx.spawn(
            i % 3 == 0 ? (ParticleOptions)DFParticles.HAKI_SPARK.get() : (ParticleOptions)DFParticles.HAKI_SHARD.get(),
            at.x,
            at.y,
            at.z,
            dir.x * s + Fx.spread(0.12),
            dir.y * s + 0.08 + Fx.spread(0.08),
            dir.z * s + Fx.spread(0.12)
         );
      }

      SoundDesign.play(at, DFSounds.HAKI_HIT, 0.8F + 0.2F * strength, 0.9F + r.nextFloat() * 0.15F, SoundDesign.layer(DFSounds.HAKI_LAYER_ANVIL, 0.35F, 1.5F));
      shakeNear(at, 0.06F + 0.08F * strength, 8);
   }

   private static void clash(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      Vec3 at = p.pos();
      RandomSource r = Fx.random();
      float strength = Mth.clamp(p.power(), 0.3F, 1.0F);
      int bolts = 4 + (int)(strength * 4.0F);

      for (int i = 0; i < bolts; i++) {
         Vec3 to = at.add(Fx.spread(2.4 + (double)(strength * 2.0F)), Fx.spread(1.6 + (double)strength), Fx.spread(2.4 + (double)(strength * 2.0F)));
         VfxManager.add(new BlackLightningEffect(at, to, 5 + r.nextInt(4), 0.07F + 0.05F * strength, 2, r.nextLong()));
      }

      VfxManager.add(new ImpactFlashEffect(at, 0.7F + 0.5F * strength, r.nextFloat()));
      VfxManager.add(
         new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, at, Vec3.ZERO, 1.6F + strength * 2.0F, 0.0F, 8, 2, 0.8F).tint(21, 21, 29).shell(242, 242, 242)
      );

      for (int i = 0; i < Fx.count(14); i++) {
         Fx.spawn(
            i % 2 == 0 ? (ParticleOptions)DFParticles.HAKI_SPARK.get() : (ParticleOptions)DFParticles.HAKI_SHARD.get(),
            at.x,
            at.y,
            at.z,
            Fx.spread(0.4),
            Fx.spread(0.3) + 0.1,
            Fx.spread(0.4)
         );
      }

      SoundDesign.play(
         at,
         DFSounds.HAKI_CLASH,
         1.0F,
         0.9F + r.nextFloat() * 0.15F,
         SoundDesign.layer(DFSounds.HAKI_LAYER_THUNDER, 0.5F, 1.4F),
         SoundDesign.layer(DFSounds.HAKI_LAYER_ANVIL, 0.5F, 0.9F)
      );
      shakeNear(at, 0.35F + 0.35F * strength, 24);
   }

   private static void ryuo(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      Vec3 at = p.pos();
      Vec3 dir = p.dir().lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : p.dir().normalize();
      float charge = Mth.clamp(p.power(), 0.2F, 1.0F);
      RandomSource r = Fx.random();
      Vec3 through = at.add(dir.scale(1.6 + 1.6 * (double)charge));
      VfxManager.add(new BlackLightningEffect(at.subtract(dir.scale(0.4)), through, 7, 0.1F + 0.06F * charge, 3, r.nextLong()));

      for (int i = 0; i < 2 + (int)(charge * 4.0F); i++) {
         Vec3 to = through.add(Fx.spread(1.6), Fx.spread(1.2), Fx.spread(1.6));
         VfxManager.add(new BlackLightningEffect(at, to, 5 + r.nextInt(3), 0.06F, 1, r.nextLong()));
      }

      VfxManager.add(new ImpactFlashEffect(at, 0.8F + 0.5F * charge, r.nextFloat()));
      VfxManager.add(
         new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, at, Vec3.ZERO, 1.4F + charge * 2.2F, 0.0F, 9, 3, 0.9F).tint(11, 11, 16).shell(242, 242, 242)
      );

      for (int i = 0; i < Fx.count(18); i++) {
         double s = 0.2 + r.nextDouble() * 0.3;
         Fx.spawn(
            i % 3 == 0 ? (ParticleOptions)DFParticles.HAKI_SPARK.get() : (ParticleOptions)DFParticles.HAKI_SHARD.get(),
            at.x,
            at.y,
            at.z,
            dir.x * s + Fx.spread(0.15),
            dir.y * s + Fx.spread(0.15) + 0.06,
            dir.z * s + Fx.spread(0.15)
         );
      }

      SoundDesign.play(
         at,
         DFSounds.HAKI_RYUO,
         1.0F,
         0.9F + r.nextFloat() * 0.1F,
         SoundDesign.layer(DFSounds.HAKI_LAYER_SMASH, 0.8F, 0.9F),
         SoundDesign.layer(DFSounds.HAKI_HIT, 0.7F, 0.6F)
      );
      if (entity == Minecraft.getInstance().player) {
         CameraEffects.fovPunch(0.06F + 0.05F * charge);
      }

      shakeNear(at, 0.3F + 0.35F * charge, 20);
   }

   private static void ripple(Vec3 at, float radius, boolean awaken) {
      VfxManager.add(
         new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, at.add(0.0, 0.1, 0.0), Vec3.ZERO, radius, 0.0F, 14, 2, 0.6F).tint(21, 21, 29).shell(242, 242, 242)
      );
      RandomSource r = Fx.random();

      for (int i = 0; i < Fx.count(awaken ? 24 : 12); i++) {
         double a = r.nextDouble() * Math.PI * 2.0;
         Fx.spawn(
            (ParticleOptions)DFParticles.HAKI_EMBER.get(),
            at.x + Math.cos(a) * 0.6,
            at.y + r.nextDouble() * 1.8,
            at.z + Math.sin(a) * 0.6,
            Math.cos(a) * 0.04,
            0.03,
            Math.sin(a) * 0.04
         );
      }

      SoundDesign.play(at.add(0.0, 1.0, 0.0), awaken ? DFSounds.HAKI_UNLOCK : DFSounds.HAKI_LEVEL_UP, 0.9F, 1.0F);
   }

   private static void contact(Player player, LogiaHitPayload.Hit hit) {
      VfxManager.add(new ImpactFlashEffect(hit.pos(), 0.45F, Fx.random().nextFloat()));
      double h = (hit.pos().y - player.getY()) / Math.max(0.1, (double)player.getBbHeight());
      float yaw = player.yBodyRot * (float) (Math.PI / 180.0);
      double side = (hit.pos().x - player.getX()) * (double)(-Mth.cos(yaw)) + (hit.pos().z - player.getZ()) * (double)(-Mth.sin(yaw));
      int part = h > 0.78 ? 128 : (h > 0.42 ? (Math.abs(side) > 0.2 ? (side > 0.0 ? 4 : 8) : 64) : (side > 0.0 ? 16 : 32));
      HakiClientState.contact(player.getId(), part, 8);
      SoundDesign.play(hit.pos(), DFSounds.HAKI_HIT, 0.8F, 1.05F, SoundDesign.layer(DFSounds.HAKI_LAYER_ANVIL, 0.3F, 1.6F));
   }

   private static void shakeNear(Vec3 at, float strength, int radius) {
      float scale = ((Double)HakiClientConfig.SHAKE.get()).floatValue();
      if (scale > 0.0F) {
         CameraEffects.shakeAt(at, strength * scale, (double)radius);
      }
   }

   static int limbs(int mask) {
      int limbs = 0;
      if ((mask & 128) != 0) {
         limbs |= 1;
      }

      if ((mask & 64) != 0) {
         limbs |= 2;
      }

      if ((mask & 5) != 0) {
         limbs |= 4;
      }

      if ((mask & 10) != 0) {
         limbs |= 8;
      }

      if ((mask & 16) != 0) {
         limbs |= 16;
      }

      if ((mask & 32) != 0) {
         limbs |= 32;
      }

      return limbs;
   }

   static Vec3 hand(Player player, boolean right, boolean ownFirstPerson) {
      float yaw = player.yBodyRot * (float) (Math.PI / 180.0);
      double rx = (double)(-Mth.cos(yaw));
      double rz = (double)(-Mth.sin(yaw));
      double fx = (double)(-Mth.sin(yaw));
      double fz = (double)Mth.cos(yaw);
      double s = right ? 0.36 : -0.36;
      double up = ownFirstPerson ? 1.25 : (double)player.getBbHeight() * 0.42;
      double ahead = ownFirstPerson ? 0.45 : 0.08;
      return new Vec3(player.getX() + rx * s + fx * ahead, player.getY() + up, player.getZ() + rz * s + fz * ahead);
   }

   private HakiClient() {
   }
}
