package com.devilfruits.fruits.uo_uo_seiryu.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.AwakeningCapture;
import com.devilfruits.client.form.FormMovements;
import com.devilfruits.client.render.FormRenderers;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuEntities;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuFx;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.fruits.uo_uo_seiryu.client.dev.AscensionCapture;
import com.devilfruits.fruits.uo_uo_seiryu.client.dev.CataclysmCapture;
import com.devilfruits.fruits.uo_uo_seiryu.client.dev.DragonSyncCheck;
import com.devilfruits.fruits.uo_uo_seiryu.client.dev.HellfireCaptureScript;
import com.devilfruits.fruits.uo_uo_seiryu.client.dev.JumpCapture;
import com.devilfruits.fruits.uo_uo_seiryu.client.dev.RushCapture;
import com.devilfruits.fruits.uo_uo_seiryu.client.dev.SeiryuAutotest;
import com.devilfruits.fruits.uo_uo_seiryu.client.dev.SeiryuGpoAutotest;
import com.devilfruits.fruits.uo_uo_seiryu.client.dev.ShoulderCapture;
import com.devilfruits.fruits.uo_uo_seiryu.client.dev.SoundCheck;
import com.devilfruits.fruits.uo_uo_seiryu.client.dev.StageCapture;
import com.devilfruits.fruits.uo_uo_seiryu.client.dev.StressCapture;
import com.devilfruits.fruits.uo_uo_seiryu.client.dev.TailSlamCapture;
import com.devilfruits.fruits.uo_uo_seiryu.client.dev.ThunderfallCapture;
import com.devilfruits.fruits.uo_uo_seiryu.client.dev.TransformCapture;
import com.devilfruits.fruits.uo_uo_seiryu.client.dev.VeilCapture;
import com.devilfruits.fruits.uo_uo_seiryu.client.particle.SeiryuParticleProviders;
import com.devilfruits.fruits.uo_uo_seiryu.client.render.DragonRenderer;
import com.devilfruits.fruits.uo_uo_seiryu.client.render.HybridRenderer;
import com.devilfruits.fruits.uo_uo_seiryu.client.render.SeiryuHumanLayer;
import com.devilfruits.fruits.uo_uo_seiryu.entity.SeiryuDragonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.common.NeoForge;

public final class SeiryuClient {
   private static final HybridRenderer HYBRID = new HybridRenderer();
   private static final DragonRenderer DRAGON = new DragonRenderer();

   public static void init(IEventBus modBus) {
      SeiryuClientFx.register();
      HybridFx.register(modBus);
      FormRenderers.register(UoUoSeiryu.id("hybrid"), HYBRID);
      FormRenderers.register(UoUoSeiryu.id("dragon"), DRAGON);
      FormMovements.register(UoUoSeiryu.fruit().form("dragon").id(), new DragonMovement());
      SeiryuDragonEntity.localPlayerId = () -> {
         LocalPlayer player = Minecraft.getInstance().player;
         return player != null ? player.getId() : Integer.MIN_VALUE;
      };
      NeoForge.EVENT_BUS.addListener(DRAGON::onRenderLevel);
      NeoForge.EVENT_BUS.addListener(event -> {
         if (event.getLevel().isClientSide() && event.getEntity() instanceof SeiryuDragonEntity shell) {
            DragonRenderer.track(shell);
         }
      });
      NeoForge.EVENT_BUS.addListener(event -> {
         if (event.getLevel().isClientSide() && event.getEntity() instanceof SeiryuDragonEntity shell) {
            DragonRenderer.untrack(shell);
         }
      });
      NeoForge.EVENT_BUS.addListener(event -> {
         HYBRID.clear();
         DragonRenderer.clear();
      });
      NeoForge.EVENT_BUS.addListener(event -> {
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            HybridFx.tick(level);
            if (!Minecraft.getInstance().isPaused()) {
               SeiryuClientFx.tickDragons(level);
            }

            if (level.getGameTime() % 100L == 0L) {
               HYBRID.prune(level);
            }
         }
      });
      modBus.addListener(SeiryuParticleProviders::register);
      modBus.addListener(SeiryuClient::registerRenderers);
      modBus.addListener(SeiryuClient::registerLayerDefinitions);
      modBus.addListener(SeiryuClient::addLayers);
      AutoTest.addScript(UoUoSeiryu.ID, new SeiryuAutotest());
      AutoTest.addScript(DevilFruits.id("seiryu_gpo"), new SeiryuGpoAutotest());
      AutoTest.addScript(DevilFruits.id("vfx_hellfire_ray"), new HellfireCaptureScript());
      AutoTest.addScript(DevilFruits.id("vfx_thunderclad_veil"), new VeilCapture());
      AutoTest.addScript(DevilFruits.id("vfx_thunderclaw_rush"), new RushCapture());
      AutoTest.addScript(DevilFruits.id("vfx_draconic_thunderfall"), new ThunderfallCapture());
      AutoTest.addScript(DevilFruits.id("vfx_draconic_ascension"), new AscensionCapture());
      AutoTest.addScript(DevilFruits.id("vfx_draconic_cataclysm"), new CataclysmCapture());
      AutoTest.addScript(DevilFruits.id("vfx_draconic_tail_slam"), new TailSlamCapture());
      AutoTest.addScript(DevilFruits.id("vfx_transformation"), new TransformCapture());

      for (boolean look : new boolean[]{false, true}) {
         AutoTest.addScript(
            DevilFruits.id("vfx_awaken_seiryu" + (look ? "_look" : "")),
            new AwakeningCapture(
               UoUoSeiryu.ID,
               look,
               new AwakeningCapture.Take("vfx_aws_", "seiryu awakening", SeiryuFx.AWAKEN, 7.0, 140.0F, 0.0F, 64, new int[]{6, 10, 11, 13, 16, 22, 34})
            )
         );
      }

      AutoTest.addScript(DevilFruits.id("vfx_hybrid_jump"), new JumpCapture(false));
      AutoTest.addScript(DevilFruits.id("vfx_hybrid_jump_look"), new JumpCapture(true));
      AutoTest.addScript(DevilFruits.id("vfx_shoulder"), new ShoulderCapture());
      AutoTest.addScript(DevilFruits.id("vfx_dragon_sync"), new DragonSyncCheck());
      AutoTest.addScript(DevilFruits.id("vfx_seiryu_sound"), new SoundCheck());
      AutoTest.addScript(DevilFruits.id("vfx_stress"), new StressCapture());

      for (String[] v : new String[][]{
         {"human", null},
         {"human_hakke", "vfx_hb_"},
         {"human_breath", "vfx_hbr_"},
         {"human_cloud", "vfx_hfc_"},
         {"human_dash", "vfx_hdd_"},
         {"hybrid_rest", null},
         {"hybrid_hakke", "vfx_yb_"},
         {"hybrid_breath", "vfx_ybr_"},
         {"dragon", null},
         {"dragon_dbr", "vfx_dbr_"},
         {"dragon_dbb", "vfx_dbb_"},
         {"dragon_dtw", "vfx_dtw_"},
         {"dragon_dst", "vfx_dst_"},
         {"dragon_dtl", "vfx_dtl_"},
         {"dragon_dro", "vfx_dro_"},
         {"dragon_dtf", "vfx_dtf_"}
      }) {
         String stage = v[0].startsWith("human") ? "human" : (v[0].startsWith("dragon") ? "dragon" : "hybrid");
         AutoTest.addScript(DevilFruits.id("vfx_" + v[0]), new StageCapture(stage, v[1]));
      }
   }

   private static void registerRenderers(RegisterRenderers event) {
      event.registerEntityRenderer(SeiryuEntities.DRAGON_BODY.get(), NoopRenderer::new);
   }

   private static void registerLayerDefinitions(RegisterLayerDefinitions event) {
      event.registerLayerDefinition(SeiryuHumanLayer.LAYER, SeiryuHumanLayer::createLayer);
   }

   private static void addLayers(AddLayers event) {
      for (Model model : event.getSkins()) {
         if (event.getSkin(model) instanceof PlayerRenderer renderer) {
            renderer.addLayer(new SeiryuHumanLayer(renderer, event.getEntityModels().bakeLayer(SeiryuHumanLayer.LAYER), model == Model.SLIM));
         }
      }
   }

   private SeiryuClient() {
   }
}
