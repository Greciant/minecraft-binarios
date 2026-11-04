package com.devilfruits.mobs.pirate;

import com.devilfruits.DevilFruits;
import com.devilfruits.mobs.pirate.entity.MusketBall;
import com.devilfruits.mobs.pirate.entity.PirateCaptain;
import com.devilfruits.mobs.pirate.entity.PirateDeckhand;
import com.devilfruits.mobs.pirate.entity.PirateFighter;
import com.devilfruits.mobs.pirate.entity.PirateGunner;
import com.devilfruits.mobs.pirate.item.FlintlockItem;
import com.devilfruits.registry.DFCreativeTabs;
import com.devilfruits.registry.DFDataComponents;
import com.devilfruits.registry.DFEntities;
import com.devilfruits.registry.DFItems;
import com.devilfruits.registry.DFParticles;
import com.devilfruits.registry.DFSounds;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class Pirates {
   public static final Supplier<EntityType<PirateDeckhand>> DECKHAND = DFEntities.ENTITIES
      .register(
         "pirate/deckhand",
         () -> Builder.of(PirateDeckhand::new, MobCategory.MONSTER)
               .sized(0.6F, 1.95F)
               .passengerAttachments(new float[]{2.0F})
               .ridingOffset(-0.6F)
               .clientTrackingRange(8)
               .build("pirate/deckhand")
      );
   public static final Supplier<EntityType<PirateGunner>> GUNNER = DFEntities.ENTITIES
      .register(
         "pirate/gunner",
         () -> Builder.of(PirateGunner::new, MobCategory.MONSTER)
               .sized(0.6F, 1.95F)
               .passengerAttachments(new float[]{2.0F})
               .ridingOffset(-0.6F)
               .clientTrackingRange(8)
               .build("pirate/gunner")
      );
   public static final Supplier<EntityType<PirateCaptain>> CAPTAIN = DFEntities.ENTITIES
      .register(
         "pirate/captain",
         () -> Builder.of(PirateCaptain::new, MobCategory.MONSTER)
               .sized(0.6F, 1.95F)
               .passengerAttachments(new float[]{2.0F})
               .ridingOffset(-0.6F)
               .clientTrackingRange(10)
               .build("pirate/captain")
      );
   public static final Supplier<EntityType<MusketBall>> MUSKET_BALL_ENTITY = DFEntities.ENTITIES
      .register(
         "pirate/musket_ball",
         () -> Builder.of(MusketBall::new, MobCategory.MISC).sized(0.15F, 0.15F).clientTrackingRange(6).updateInterval(1).build("pirate/musket_ball")
      );
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> GUN_LOADED = DFDataComponents.COMPONENTS
      .registerComponentType("pirate/gun_loaded", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
   public static final TagKey<BannerPattern> JOLLY_ROGER_PATTERN = TagKey.create(Registries.BANNER_PATTERN, id("pattern_item/jolly_roger"));
   public static final DeferredItem<SwordItem> CUTLASS = DFItems.ITEMS
      .register("pirate/cutlass", () -> new SwordItem(Tiers.IRON, new Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.2F))));
   public static final DeferredItem<SwordItem> CAPTAIN_CUTLASS = DFItems.ITEMS
      .register(
         "pirate/captain_cutlass",
         () -> new SwordItem(
               Tiers.DIAMOND,
               new Properties()
                  .rarity(Rarity.RARE)
                  .attributes(
                     ItemAttributeModifiers.builder()
                        .add(
                           Attributes.ATTACK_DAMAGE,
                           new AttributeModifier(ResourceLocation.withDefaultNamespace("base_attack_damage"), 8.0, Operation.ADD_VALUE),
                           EquipmentSlotGroup.MAINHAND
                        )
                        .add(
                           Attributes.ATTACK_SPEED,
                           new AttributeModifier(ResourceLocation.withDefaultNamespace("base_attack_speed"), -2.9, Operation.ADD_VALUE),
                           EquipmentSlotGroup.MAINHAND
                        )
                        .add(
                           Attributes.ATTACK_KNOCKBACK,
                           new AttributeModifier(id("captain_cutlass_knockback"), 0.6, Operation.ADD_VALUE),
                           EquipmentSlotGroup.MAINHAND
                        )
                        .build()
                  )
            )
      );
   public static final DeferredItem<FlintlockItem> FLINTLOCK_PISTOL = DFItems.ITEMS
      .register("pirate/flintlock_pistol", () -> new FlintlockItem(false, new Properties().stacksTo(1).durability(320)));
   public static final DeferredItem<FlintlockItem> MUSKET = DFItems.ITEMS
      .register("pirate/musket", () -> new FlintlockItem(true, new Properties().stacksTo(1).durability(480)));
   public static final DeferredItem<Item> MUSKET_BALL = DFItems.ITEMS.register("pirate/musket_ball", () -> new Item(new Properties()));
   public static final Supplier<SimpleParticleType> MUZZLE_FLASH = DFParticles.PARTICLES.register("pirate/muzzle_flash", () -> new SimpleParticleType(true));
   public static final DeferredItem<BannerPatternItem> JOLLY_ROGER = DFItems.ITEMS
      .register("pirate/jolly_roger_banner_pattern", () -> new BannerPatternItem(JOLLY_ROGER_PATTERN, new Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
   public static final DeferredItem<DeferredSpawnEggItem> DECKHAND_EGG = DFItems.ITEMS
      .register("pirate/deckhand_spawn_egg", () -> new DeferredSpawnEggItem(DECKHAND, 8007210, 15260868, new Properties()));
   public static final DeferredItem<DeferredSpawnEggItem> GUNNER_EGG = DFItems.ITEMS
      .register("pirate/gunner_spawn_egg", () -> new DeferredSpawnEggItem(GUNNER, 3029586, 12095560, new Properties()));
   public static final DeferredItem<DeferredSpawnEggItem> CAPTAIN_EGG = DFItems.ITEMS
      .register("pirate/captain_spawn_egg", () -> new DeferredSpawnEggItem(CAPTAIN, 9051164, 14725184, new Properties()));
   public static final Supplier<SoundEvent> DECKHAND_AMBIENT = DFSounds.register("pirates.deckhand.ambient");
   public static final Supplier<SoundEvent> DECKHAND_HURT = DFSounds.register("pirates.deckhand.hurt");
   public static final Supplier<SoundEvent> DECKHAND_DEATH = DFSounds.register("pirates.deckhand.death");
   public static final Supplier<SoundEvent> GUNNER_AMBIENT = DFSounds.register("pirates.gunner.ambient");
   public static final Supplier<SoundEvent> GUNNER_HURT = DFSounds.register("pirates.gunner.hurt");
   public static final Supplier<SoundEvent> GUNNER_DEATH = DFSounds.register("pirates.gunner.death");
   public static final Supplier<SoundEvent> CAPTAIN_AMBIENT = DFSounds.register("pirates.captain.ambient");
   public static final Supplier<SoundEvent> CAPTAIN_HURT = DFSounds.register("pirates.captain.hurt");
   public static final Supplier<SoundEvent> CAPTAIN_DEATH = DFSounds.register("pirates.captain.death");
   public static final Supplier<SoundEvent> CUTLASS_SWING = DFSounds.register("pirates.cutlass.swing");
   public static final Supplier<SoundEvent> CUTLASS_HIT = DFSounds.register("pirates.cutlass.hit");
   public static final Supplier<SoundEvent> PISTOL_FIRE = DFSounds.register("pirates.pistol.fire");
   public static final Supplier<SoundEvent> MUSKET_FIRE = DFSounds.register("pirates.musket.fire");
   public static final Supplier<SoundEvent> GUN_BOOM = DFSounds.register("pirates.gun.boom");
   public static final Supplier<SoundEvent> GUN_CLICK = DFSounds.register("pirates.gun.click");
   public static final Supplier<SoundEvent> GUN_POUR = DFSounds.register("pirates.gun.pour");
   public static final Supplier<SoundEvent> GUN_RAM = DFSounds.register("pirates.gun.ram");
   public static final Supplier<SoundEvent> BALL_HIT = DFSounds.register("pirates.ball.hit");
   public static final Supplier<SoundEvent> BARGE = DFSounds.register("pirates.deckhand.barge");
   public static final Supplier<SoundEvent> TAUNT = DFSounds.register("pirates.deckhand.taunt");
   public static final Supplier<SoundEvent> RALLY = DFSounds.register("pirates.captain.rally");
   public static final Supplier<SoundEvent> SLAM = DFSounds.register("pirates.captain.slam");
   public static final Supplier<SoundEvent> ENRAGE = DFSounds.register("pirates.captain.enrage");
   public static final Supplier<SoundEvent> HEAVY_STEP = DFSounds.register("pirates.captain.step");

   public static ResourceLocation id(String path) {
      return DevilFruits.id("pirate/" + path);
   }

   private static void registerPayloads(RegisterPayloadHandlersEvent event) {
      event.registrar("11").playToServer(FireGunPayload.TYPE, FireGunPayload.STREAM_CODEC, (payload, ctx) -> {
         if (ctx.player() instanceof ServerPlayer player) {
            FireGunPayload.handle(player);
         }
      });
   }

   public static void init(IEventBus modBus, ModContainer container) {
      modBus.addListener(Pirates::registerPayloads);
      container.registerConfig(Type.COMMON, PirateConfig.SPEC, "devilfruits-pirates-common.toml");

      for (Supplier<? extends Item> item : List.of(
         CUTLASS, CAPTAIN_CUTLASS, FLINTLOCK_PISTOL, MUSKET, MUSKET_BALL, JOLLY_ROGER, DECKHAND_EGG, GUNNER_EGG, CAPTAIN_EGG
      )) {
         DFCreativeTabs.addExtra(item);
      }

      modBus.addListener(Pirates::attributes);
      modBus.addListener(Pirates::spawnPlacements);
      modBus.addListener(Pirates::creativeTabs);
   }

   private static void attributes(EntityAttributeCreationEvent event) {
      event.put(DECKHAND.get(), PirateDeckhand.createAttributes().build());
      event.put(GUNNER.get(), PirateGunner.createAttributes().build());
      event.put(CAPTAIN.get(), PirateCaptain.createAttributes().build());
   }

   private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
      event.register(
         DECKHAND.get(),
         SpawnPlacementTypes.ON_GROUND,
         Types.MOTION_BLOCKING_NO_LEAVES,
         PirateFighter::checkPirateSpawnRules,
         net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE
      );
      event.register(
         GUNNER.get(),
         SpawnPlacementTypes.ON_GROUND,
         Types.MOTION_BLOCKING_NO_LEAVES,
         PirateFighter::checkPirateSpawnRules,
         net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE
      );
      event.register(
         CAPTAIN.get(),
         SpawnPlacementTypes.ON_GROUND,
         Types.MOTION_BLOCKING_NO_LEAVES,
         PirateFighter::checkPirateSpawnRules,
         net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE
      );
   }

   private static void creativeTabs(BuildCreativeModeTabContentsEvent event) {
      if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
         event.accept((ItemLike)DECKHAND_EGG.get());
         event.accept((ItemLike)GUNNER_EGG.get());
         event.accept((ItemLike)CAPTAIN_EGG.get());
      }
   }

   private Pirates() {
   }
}
