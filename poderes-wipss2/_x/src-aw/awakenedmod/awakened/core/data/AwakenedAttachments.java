package com.awakenedmod.awakened.core.data;

import com.awakenedmod.awakened.content.brawler.MarkState;
import com.awakenedmod.awakened.content.chronokinetic.ChronoHeld;
import com.awakenedmod.awakened.content.hydro.HydroVisual;
import com.awakenedmod.awakened.content.hydro.TideHookVisual;
import com.awakenedmod.awakened.content.kineticcharger.KineticVisual;
import com.awakenedmod.awakened.core.SlowState;
import com.awakenedmod.awakened.core.glow.GlowState;
import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class AwakenedAttachments {
   private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "awakened");
   public static final Supplier<AttachmentType<PowerData>> POWER_DATA = ATTACHMENT_TYPES.register(
      "power_data", () -> AttachmentType.builder(PowerData::new).serialize(PowerData.MAP_CODEC.codec()).copyOnDeath().build()
   );
   public static final Supplier<AttachmentType<ProgressData>> PROGRESS_DATA = ATTACHMENT_TYPES.register(
      "progress_data", () -> AttachmentType.builder(ProgressData::new).serialize(ProgressData.MAP_CODEC.codec()).copyOnDeath().build()
   );
   public static final Supplier<AttachmentType<SkillData>> SKILL_DATA = ATTACHMENT_TYPES.register(
      "skill_data", () -> AttachmentType.builder(SkillData::new).serialize(SkillData.MAP_CODEC.codec()).copyOnDeath().build()
   );
   public static final Supplier<AttachmentType<QuestData>> QUEST_DATA = ATTACHMENT_TYPES.register(
      "quest_data", () -> AttachmentType.builder(QuestData::new).serialize(QuestData.MAP_CODEC.codec()).copyOnDeath().build()
   );
   public static final Supplier<AttachmentType<RelicData>> RELIC_DATA = ATTACHMENT_TYPES.register(
      "relic_data", () -> AttachmentType.builder(RelicData::new).serialize(RelicData.MAP_CODEC.codec()).sync(RelicData.STREAM_CODEC).copyOnDeath().build()
   );
   public static final Supplier<AttachmentType<SpiderVisual>> SPIDER_VISUAL = ATTACHMENT_TYPES.register(
      "spider_visual", () -> AttachmentType.builder(() -> SpiderVisual.NONE).sync(SpiderVisual.STREAM_CODEC).build()
   );
   public static final Supplier<AttachmentType<ExoVisual>> EXO_VISUAL = ATTACHMENT_TYPES.register(
      "exo_visual", () -> AttachmentType.builder(() -> ExoVisual.STOWED).sync(ExoVisual.STREAM_CODEC).build()
   );
   public static final Supplier<AttachmentType<ExoSkinData>> EXO_SKINS = ATTACHMENT_TYPES.register(
      "exo_skins", () -> AttachmentType.builder(ExoSkinData::new).serialize(ExoSkinData.MAP_CODEC.codec()).sync(ExoSkinData.STREAM_CODEC).copyOnDeath().build()
   );
   public static final Supplier<AttachmentType<AeroVisual>> AERO_VISUAL = ATTACHMENT_TYPES.register(
      "aero_visual", () -> AttachmentType.builder(() -> AeroVisual.NONE).sync(AeroVisual.STREAM_CODEC).build()
   );
   public static final Supplier<AttachmentType<VelocityVisual>> VELOCITY_VISUAL = ATTACHMENT_TYPES.register(
      "velocity_visual", () -> AttachmentType.builder(() -> VelocityVisual.NONE).sync(VelocityVisual.STREAM_CODEC).build()
   );
   public static final Supplier<AttachmentType<PyroVisual>> PYRO_VISUAL = ATTACHMENT_TYPES.register(
      "pyro_visual", () -> AttachmentType.builder(() -> PyroVisual.COLD).sync(PyroVisual.STREAM_CODEC).build()
   );
   public static final Supplier<AttachmentType<ExoGear>> EXO_GEAR = ATTACHMENT_TYPES.register(
      "exo_gear", () -> AttachmentType.builder(ExoGear::new).serialize(ExoGear.MAP_CODEC.codec()).copyOnDeath().build()
   );
   public static final Supplier<AttachmentType<Long>> DISCORD_LAST_SHOWN = ATTACHMENT_TYPES.register(
      "discord_last_shown", () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("at").codec()).build()
   );
   public static final Supplier<AttachmentType<MarkState>> BRAWLER_MARK = ATTACHMENT_TYPES.register(
      "brawler_mark", () -> AttachmentType.builder(() -> MarkState.NONE).sync(MarkState.STREAM_CODEC).build()
   );
   public static final Supplier<AttachmentType<BrawlerVisual>> BRAWLER_VISUAL = ATTACHMENT_TYPES.register(
      "brawler_visual", () -> AttachmentType.builder(() -> BrawlerVisual.IDLE).sync(BrawlerVisual.STREAM_CODEC).build()
   );
   public static final Supplier<AttachmentType<GlowState>> LIMB_GLOW = ATTACHMENT_TYPES.register(
      "limb_glow", () -> AttachmentType.builder(() -> GlowState.NONE).sync(GlowState.STREAM_CODEC).build()
   );
   public static final Supplier<AttachmentType<KineticVisual>> KINETIC_VISUAL = ATTACHMENT_TYPES.register(
      "kinetic_visual", () -> AttachmentType.builder(() -> KineticVisual.NONE).sync(KineticVisual.STREAM_CODEC).build()
   );
   public static final Supplier<AttachmentType<HydroVisual>> HYDRO_VISUAL = ATTACHMENT_TYPES.register(
      "hydro_visual", () -> AttachmentType.builder(() -> HydroVisual.NONE).sync(HydroVisual.STREAM_CODEC).build()
   );
   public static final Supplier<AttachmentType<TideHookVisual>> TIDE_HOOK_VISUAL = ATTACHMENT_TYPES.register(
      "tide_hook_visual", () -> AttachmentType.builder(() -> TideHookVisual.NONE).sync(TideHookVisual.STREAM_CODEC).build()
   );
   public static final Supplier<AttachmentType<SlowState>> SLOW_STATE = ATTACHMENT_TYPES.register(
      "slow_state", () -> AttachmentType.builder(() -> SlowState.NONE).sync(SlowState.STREAM_CODEC).build()
   );
   public static final Supplier<AttachmentType<ChronoHeld>> CHRONO_HELD = ATTACHMENT_TYPES.register(
      "chrono_held", () -> AttachmentType.builder(() -> ChronoHeld.NONE).sync(ChronoHeld.STREAM_CODEC).build()
   );
   public static final Supplier<AttachmentType<Boolean>> FLUID_FOOTING = ATTACHMENT_TYPES.register(
      "fluid_footing", () -> AttachmentType.builder(() -> Boolean.FALSE).sync(ByteBufCodecs.BOOL).build()
   );
   public static final Supplier<AttachmentType<Boolean>> POWER_SUPPRESSED = ATTACHMENT_TYPES.register(
      "power_suppressed", () -> AttachmentType.builder(() -> Boolean.FALSE).sync(ByteBufCodecs.BOOL).build()
   );
   public static final Supplier<AttachmentType<Map<UUID, Float>>> DAMAGE_LEDGER = ATTACHMENT_TYPES.register(
      "damage_ledger", () -> AttachmentType.builder(() -> new HashMap()).build()
   );

   private AwakenedAttachments() {
   }

   public static void register(IEventBus modEventBus) {
      ATTACHMENT_TYPES.register(modEventBus);
   }

   public static PowerData get(Player player) {
      return (PowerData)player.getData(POWER_DATA);
   }

   public static ProgressData progress(Player player) {
      return (ProgressData)player.getData(PROGRESS_DATA);
   }

   public static SkillData skill(Player player) {
      return (SkillData)player.getData(SKILL_DATA);
   }

   public static QuestData quests(Player player) {
      return (QuestData)player.getData(QUEST_DATA);
   }

   public static RelicData relics(Player player) {
      return (RelicData)player.getData(RELIC_DATA);
   }
}
