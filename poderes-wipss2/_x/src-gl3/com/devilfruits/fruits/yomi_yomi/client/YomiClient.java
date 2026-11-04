package com.devilfruits.fruits.yomi_yomi.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.animation.AnimationController;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.form.FormMovements;
import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.render.BodyModels;
import com.devilfruits.client.render.FormRenderers;
import com.devilfruits.client.render.SkinOverrides;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.fruits.yomi_yomi.Vessel;
import com.devilfruits.fruits.yomi_yomi.YomiEntities;
import com.devilfruits.fruits.yomi_yomi.YomiFx;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.fruits.yomi_yomi.client.dev.YomiAutotest;
import com.devilfruits.fruits.yomi_yomi.client.dev.YomiCapture;
import com.devilfruits.fruits.yomi_yomi.soul.SoulProjection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

public final class YomiClient {
   private static final Vector3f GREEN = new Vector3f(0.49F, 1.0F, 0.23F);
   private static final Vector3f HOT = new Vector3f(0.78F, 1.0F, 0.42F);

   public static void init(IEventBus modBus) {
      modBus.addListener(e -> e.registerEntityRenderer(YomiEntities.PLAYER_CORPSE.get(), PlayerCorpseRenderer::new));
      modBus.addListener(e -> e.registerAbove(VanillaGuiLayers.CROSSHAIR, YomiYomi.id("status"), YomiHud::render));
      FormRenderers.register(YomiYomi.id("soul"), new SoulRenderer());
      modBus.addListener(SoulBodyShader::onRegister);
      SkinOverrides.register(VesselSkins::skin);
      BodyModels.register(SkeletonBody::render);
      FormRenderers.register(YomiYomi.id("skeleton_body"), new SkeletonBody());
      WaterRunClient run = new WaterRunClient();
      FormMovements.register(YomiYomi.id("form/" + Vessel.SKELETON.formName()), run);
      FormMovements.register(YomiYomi.id("form/soul_king"), run);
      FormMovements.register(YomiYomi.id("form/soul"), new FormMovements.FormMovement() {
         @Override
         public void tick(LocalPlayer player, FruitForm form, boolean boosting) {
            if (this.held(player)) {
               player.setDeltaMovement(Vec3.ZERO);
            }
         }

         private boolean held(LocalPlayer player) {
            return FormFlags.has(player, YomiYomi.ID, YomiYomi.POSSESSING) || FormFlags.has(player, YomiYomi.ID, SoulProjection.RECOILING);
         }

         @Override
         public void input(LocalPlayer player, FruitForm form, Input input) {
            if (FormFlags.has(player, YomiYomi.ID, SoulProjection.PROJECTING)) {
               player.noPhysics = true;
               player.setOnGround(false);
               player.getAbilities().flying = true;
            }

            if (this.held(player)) {
               input.forwardImpulse = 0.0F;
               input.leftImpulse = 0.0F;
               input.jumping = false;
               input.shiftKeyDown = false;
            }
         }
      });
      AnimationController.addPoseLayer(run);
      NeoForge.EVENT_BUS.addListener(WaterRunClient::onLevelTick);
      NeoForge.EVENT_BUS.addListener(WaterRunClient::onRenderPlayer);
      NeoForge.EVENT_BUS.addListener(e -> WaterRunClient.onRenderStage(e));
      NeoForge.EVENT_BUS.addListener(e -> {
         if (e.getLevel().isClientSide()) {
            VesselSkins.tick();
         }
      });
      NeoForge.EVENT_BUS.addListener(e -> {
         VesselSkins.clear();
         WaterRunClient.clear();
         SoulRenderer.clear();
         SoulClient.clear();
      });
      ClientFxRegistry.register(YomiFx.QUIET, (level, entity, p) -> {
      });
      ClientFxRegistry.register(YomiFx.SOUL_DEPART, (level, entity, p) -> souls(level, p.pos(), 40, 1.6));
      ClientFxRegistry.register(YomiFx.SOUL_FADE, (level, entity, p) -> souls(level, p.pos(), 30, 0.8));
      ClientFxRegistry.register(YomiFx.SOUL_RISE, (level, entity, p) -> {
         if (entity != null) {
            SoulRenderer.play(entity.getId(), "emerge");
            souls(level, entity.position().add(0.0, 1.0, 0.0), 24, 0.6);
         }
      });
      ClientFxRegistry.register(YomiFx.POSSESS_DIVE, (level, entity, p) -> {
         if (entity != null) {
            SoulRenderer.play(entity.getId(), "possess");
            if (entity == Minecraft.getInstance().player) {
               YomiHud.dim(0.65F, 60);
            }
         }

         souls(level, p.pos(), 18, 0.3);
      });
      ClientFxRegistry.register(
         YomiFx.POSSESS,
         (level, entity, p) -> {
            souls(level, p.pos(), 50, 0.4);
            RandomSource r = level.getRandom();

            for (int i = 0; i < 36; i++) {
               double a = (double)i * Math.PI * 2.0 / 36.0;
               level.addParticle(
                  new DustParticleOptions(i % 2 == 0 ? GREEN : HOT, 1.4F),
                  p.pos().x + Math.cos(a) * 0.3,
                  p.pos().y + 0.1,
                  p.pos().z + Math.sin(a) * 0.3,
                  Math.cos(a) * 0.25,
                  0.02 + r.nextDouble() * 0.04,
                  Math.sin(a) * 0.25
               );
            }
         }
      );
      BladeClient.init(modBus);
      SoulClient.init(modBus);
      SoulKingClient.init(modBus);
      AutoTest.addScript(YomiYomi.ID, new YomiAutotest());
      AutoTest.addScript(DevilFruits.id("vfx_yomi_m4"), new YomiCapture("vfx_yomi4_"));
      AutoTest.addScript(DevilFruits.id("vfx_yomi_fubuki"), new YomiCapture("vfx_yomif_", YomiCapture.Stage.FUBUKI));
      AutoTest.addScript(DevilFruits.id("vfx_yomi_m5"), new YomiCapture("vfx_yomi5_", YomiCapture.Stage.M5));
      AutoTest.addScript(DevilFruits.id("vfx_yomi_polka"), new YomiCapture("vfx_yomip_", YomiCapture.Stage.POLKA));
      AutoTest.addScript(DevilFruits.id("vfx_yomi_m6"), new YomiCapture("vfx_yomi6_", YomiCapture.Stage.KING));
      AutoTest.addScript(DevilFruits.id("vfx_yomi_m6_look"), new YomiCapture("vfx_yomi6l_", YomiCapture.Stage.KING_LOOK));
      AutoTest.addScript(DevilFruits.id("vfx_yomi_cane"), new YomiCapture("vfx_yomic_", YomiCapture.Stage.CANE));
      AutoTest.addScript(DevilFruits.id("vfx_yomi_ice"), new YomiCapture("vfx_yomii_", YomiCapture.Stage.ICE));
      AutoTest.addScript(DevilFruits.id("vfx_yomi_run"), new YomiCapture("vfx_yomir_", YomiCapture.Stage.RUN));
      AutoTest.addScript(DevilFruits.id("vfx_yomi_water"), new YomiCapture("vfx_yomiw_", YomiCapture.Stage.WATER));
   }

   private static void souls(ClientLevel level, Vec3 at, int count, double rise) {
      RandomSource r = level.getRandom();

      for (int i = 0; i < count; i++) {
         level.addParticle(
            new DustParticleOptions(i % 3 == 0 ? HOT : GREEN, 0.8F + r.nextFloat() * 0.8F),
            at.x + r.nextGaussian() * 0.4,
            at.y + 0.2 + r.nextDouble(),
            at.z + r.nextGaussian() * 0.4,
            r.nextGaussian() * 0.02,
            0.02 + r.nextDouble() * 0.06 * rise,
            r.nextGaussian() * 0.02
         );
      }
   }

   public static void openVesselMenu(int corpse) {
      Minecraft.getInstance().setScreen(new VesselScreen(corpse));
   }

   private YomiClient() {
   }
}
