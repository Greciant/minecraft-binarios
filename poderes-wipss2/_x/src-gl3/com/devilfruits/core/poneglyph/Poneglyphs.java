package com.devilfruits.core.poneglyph;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.seastone.SeastoneGear;
import com.devilfruits.core.trigger.DevilFruitTrigger;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFBlocks;
import com.devilfruits.registry.DFCreativeTabs;
import com.devilfruits.registry.DFDataComponents;
import com.devilfruits.registry.DFItems;
import com.devilfruits.registry.DFLootModifiers;
import com.devilfruits.registry.DFParticles;
import com.devilfruits.registry.DFSounds;
import com.devilfruits.registry.DFTriggers;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Poneglyphs {
   public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "devilfruits");
   public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "devilfruits");
   public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, "devilfruits");
   public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, "devilfruits");
   public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, "devilfruits");
   public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS = DeferredRegister.create(
      Registries.PLACEMENT_MODIFIER_TYPE, "devilfruits"
   );
   public static final TagKey<Block> UNBREAKABLE = TagKey.create(Registries.BLOCK, DevilFruits.id("poneglyph_unbreakable"));
   public static final TagKey<Structure> RUINS_TAG = TagKey.create(Registries.STRUCTURE, DevilFruits.id("poneglyph_ruins"));
   public static final TagKey<Biome> HAS_RUINS = TagKey.create(Registries.BIOME, DevilFruits.id("has_structure/poneglyph_ruins"));
   public static final DeferredBlock<Block> PONEGLYPH_STONE = DFBlocks.BLOCKS.register("poneglyph_stone", () -> new Block(ancientStone()));
   public static final DeferredBlock<PoneglyphFaceBlock> PONEGLYPH_FACE = DFBlocks.BLOCKS
      .register("poneglyph_face", () -> new PoneglyphFaceBlock(ancientStone().lightLevel(s -> 2)));
   public static final DeferredBlock<DecipherDeskBlock> DECIPHER_DESK = DFBlocks.BLOCKS
      .register(
         "decipher_desk",
         () -> new DecipherDeskBlock(Properties.of().mapColor(MapColor.WOOD).strength(2.5F, 6.0F).sound(SoundType.WOOD).lightLevel(s -> 7).noOcclusion())
      );
   public static final Supplier<BlockEntityType<PoneglyphFaceBlockEntity>> PONEGLYPH_FACE_BE = BLOCK_ENTITIES.register(
      "poneglyph_face", () -> Builder.of(PoneglyphFaceBlockEntity::new, new Block[]{(Block)PONEGLYPH_FACE.get()}).build(null)
   );
   public static final Supplier<BlockEntityType<DecipherDeskBlockEntity>> DECIPHER_DESK_BE = BLOCK_ENTITIES.register(
      "decipher_desk", () -> Builder.of(DecipherDeskBlockEntity::new, new Block[]{(Block)DECIPHER_DESK.get()}).build(null)
   );
   public static final Supplier<MenuType<DecipherDeskMenu>> DECIPHER_DESK_MENU = MENUS.register(
      "decipher_desk", () -> IMenuTypeExtension.create(DecipherDeskMenu::new)
   );
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlueprintData.Rubbing>> RUBBING = DFDataComponents.COMPONENTS
      .registerComponentType("rubbing", b -> b.persistent(BlueprintData.Rubbing.CODEC).networkSynchronized(BlueprintData.Rubbing.STREAM_CODEC));
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlueprintData.Encrypted>> ENCRYPTED = DFDataComponents.COMPONENTS
      .registerComponentType("encrypted_blueprint", b -> b.persistent(BlueprintData.Encrypted.CODEC).networkSynchronized(BlueprintData.Encrypted.STREAM_CODEC));
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlueprintData.Deciphered>> DECIPHERED = DFDataComponents.COMPONENTS
      .registerComponentType(
         "deciphered_blueprint", b -> b.persistent(BlueprintData.Deciphered.CODEC).networkSynchronized(BlueprintData.Deciphered.STREAM_CODEC)
      );
   public static final Supplier<AttachmentType<PoneglyphPlayerData.PlayerKnowledge>> KNOWLEDGE = DFAttachments.ATTACHMENTS
      .register(
         "knowledge",
         () -> AttachmentType.builder(() -> PoneglyphPlayerData.PlayerKnowledge.EMPTY)
               .serialize(PoneglyphPlayerData.PlayerKnowledge.CODEC)
               .copyOnDeath()
               .sync((holder, to) -> holder == to, PoneglyphPlayerData.PlayerKnowledge.STREAM_CODEC)
               .build()
      );
   public static final Supplier<AttachmentType<PoneglyphPlayerData.PoneglyphLiteracy>> LITERACY = DFAttachments.ATTACHMENTS
      .register(
         "poneglyph_literacy",
         () -> AttachmentType.builder(() -> PoneglyphPlayerData.PoneglyphLiteracy.EMPTY)
               .serialize(PoneglyphPlayerData.PoneglyphLiteracy.CODEC)
               .copyOnDeath()
               .sync((holder, to) -> holder == to, PoneglyphPlayerData.PoneglyphLiteracy.STREAM_CODEC)
               .build()
      );
   public static final Supplier<AttachmentType<PoneglyphPlayerData.RubbingProgress>> RUBBING_PROGRESS = DFAttachments.ATTACHMENTS
      .register(
         "rubbing_progress",
         () -> AttachmentType.builder(() -> PoneglyphPlayerData.RubbingProgress.EMPTY)
               .serialize(PoneglyphPlayerData.RubbingProgress.CODEC)
               .copyOnDeath()
               .sync((holder, to) -> holder == to, PoneglyphPlayerData.RubbingProgress.STREAM_CODEC)
               .build()
      );
   public static final DeferredItem<BlockItem> PONEGLYPH_STONE_ITEM = DFItems.ITEMS
      .registerSimpleBlockItem(PONEGLYPH_STONE, new net.minecraft.world.item.Item.Properties().rarity(Rarity.EPIC));
   public static final DeferredItem<BlockItem> PONEGLYPH_FACE_ITEM = DFItems.ITEMS
      .registerSimpleBlockItem(PONEGLYPH_FACE, new net.minecraft.world.item.Item.Properties().rarity(Rarity.EPIC));
   public static final DeferredItem<BlockItem> DECIPHER_DESK_ITEM = DFItems.ITEMS
      .registerSimpleBlockItem(DECIPHER_DESK, new net.minecraft.world.item.Item.Properties().rarity(Rarity.UNCOMMON));
   public static final DeferredItem<PoneglyphItems.Rubbing> RUBBING_ITEM = DFItems.ITEMS
      .register("poneglyph_rubbing", () -> new PoneglyphItems.Rubbing(new net.minecraft.world.item.Item.Properties().stacksTo(16)));
   public static final DeferredItem<PoneglyphItems.Encrypted> ENCRYPTED_BLUEPRINT = DFItems.ITEMS
      .register("encrypted_blueprint", () -> new PoneglyphItems.Encrypted(new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
   public static final DeferredItem<PoneglyphItems.Deciphered> DECIPHERED_BLUEPRINT = DFItems.ITEMS
      .register("deciphered_blueprint", () -> new PoneglyphItems.Deciphered(new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
   public static final DeferredItem<PoneglyphItems.Primer> PRIMER = DFItems.ITEMS
      .register("ancient_script_primer", () -> new PoneglyphItems.Primer(new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
   public static final Supplier<RecipeSerializer<BlueprintAssemblyRecipe>> BLUEPRINT_ASSEMBLY = RECIPE_SERIALIZERS.register(
      "blueprint_assembly", () -> new SimpleCraftingRecipeSerializer(BlueprintAssemblyRecipe::new)
   );
   public static final Supplier<RecipeSerializer<KnowledgeRecipes.Shaped>> KNOWLEDGE_SHAPED = RECIPE_SERIALIZERS.register(
      "knowledge_shaped", KnowledgeRecipes.Shaped.Serializer::new
   );
   public static final Supplier<RecipeSerializer<KnowledgeRecipes.Shapeless>> KNOWLEDGE_SHAPELESS = RECIPE_SERIALIZERS.register(
      "knowledge_shapeless", KnowledgeRecipes.Shapeless.Serializer::new
   );
   public static final Supplier<StructureType<PoneglyphRuins>> RUINS = STRUCTURE_TYPES.register("poneglyph_ruins", () -> () -> PoneglyphRuins.CODEC);
   public static final Supplier<StructurePieceType> RUINS_PIECE = STRUCTURE_PIECES.register("poneglyph_ruins", () -> PoneglyphRuins.Piece::new);
   public static final Supplier<PlacementModifierType<ConfigGatePlacement>> CONFIG_GATE = PLACEMENT_MODIFIERS.register(
      "config_gate", () -> () -> ConfigGatePlacement.CODEC
   );
   public static final Supplier<MapCodec<PrimerLootModifier>> PRIMER_LOOT = DFLootModifiers.SERIALIZERS.register("add_primer", () -> PrimerLootModifier.CODEC);
   public static final Supplier<SoundEvent> SOUND_RUB = DFSounds.register("poneglyph.rub");
   public static final Supplier<SoundEvent> SOUND_RUB_DONE = DFSounds.register("poneglyph.rub_done");
   public static final Supplier<SoundEvent> SOUND_CORRECT = DFSounds.register("poneglyph.desk.correct");
   public static final Supplier<SoundEvent> SOUND_WRONG = DFSounds.register("poneglyph.desk.wrong");
   public static final Supplier<SoundEvent> SOUND_SUCCESS = DFSounds.register("poneglyph.desk.success");
   public static final Supplier<SoundEvent> SOUND_PAGE = DFSounds.register("poneglyph.desk.page");
   public static final Supplier<SoundEvent> SOUND_SEAL = DFSounds.register("poneglyph.desk.seal");
   public static final Supplier<SoundEvent> SOUND_LEARN = DFSounds.register("poneglyph.learn");
   public static final Supplier<SoundEvent> SOUND_SMUDGE = DFSounds.register("poneglyph.smudge");
   public static final Supplier<SimpleParticleType> GLYPH_PARTICLE = DFParticles.PARTICLES.register("poneglyph/glyph", () -> new SimpleParticleType(false));
   public static final Supplier<PlayerTrigger> FIRST_RUBBING = DFTriggers.TRIGGERS.register("poneglyph_rubbing", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> DECIPHERED_TRIGGER = DFTriggers.TRIGGERS.register("poneglyph_deciphered", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> LITERACY_MAX = DFTriggers.TRIGGERS.register("poneglyph_literacy_max", PlayerTrigger::new);
   public static final Supplier<DevilFruitTrigger> KNOWLEDGE_LEARNED = DFTriggers.TRIGGERS.register("knowledge_learned", DevilFruitTrigger::new);

   private static Properties ancientStone() {
      return Properties.of()
         .mapColor(MapColor.TERRACOTTA_LIGHT_BLUE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .strength(-1.0F, 3600000.0F)
         .noLootTable()
         .pushReaction(PushReaction.BLOCK)
         .sound(SoundType.DEEPSLATE_TILES)
         .isValidSpawn((s, l, p, e) -> false);
   }

   public static void init(IEventBus modBus, ModContainer container) {
      BLOCK_ENTITIES.register(modBus);
      MENUS.register(modBus);
      RECIPE_SERIALIZERS.register(modBus);
      STRUCTURE_TYPES.register(modBus);
      STRUCTURE_PIECES.register(modBus);
      PLACEMENT_MODIFIERS.register(modBus);
      SeastoneGear.init(modBus);
      container.registerConfig(Type.COMMON, PoneglyphConfig.SPEC, "devilfruits-poneglyph-common.toml");

      for (Supplier<? extends Item> item : List.of(DECIPHER_DESK_ITEM, PRIMER, PONEGLYPH_STONE_ITEM, PONEGLYPH_FACE_ITEM)) {
         DFCreativeTabs.addExtra(item);
      }
   }

   private Poneglyphs() {
   }
}
