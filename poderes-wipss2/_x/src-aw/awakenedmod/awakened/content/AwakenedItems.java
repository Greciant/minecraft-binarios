package com.awakenedmod.awakened.content;

import com.awakenedmod.awakened.content.exosavant.ExoArmorPieceItem;
import com.awakenedmod.awakened.content.exosavant.ExoCurioItem;
import com.awakenedmod.awakened.content.hydro.VesselItem;
import com.awakenedmod.awakened.content.item.ArbiterSummonsItem;
import com.awakenedmod.awakened.content.item.ArchEvokerStaffItem;
import com.awakenedmod.awakened.content.item.ExosavantWarhammerItem;
import com.awakenedmod.awakened.content.item.IaiBladeItem;
import com.awakenedmod.awakened.content.item.PrismItem;
import com.awakenedmod.awakened.content.item.ReaperScytheItem;
import com.awakenedmod.awakened.content.item.VerdictChainItem;
import com.awakenedmod.awakened.content.reaper.SoulVesselItem;
import com.awakenedmod.awakened.content.relic.RelicBeltItem;
import com.awakenedmod.awakened.content.relic.RelicItem;
import com.awakenedmod.awakened.content.relic.RelicItems;
import com.awakenedmod.awakened.content.scaler.ContainmentJarItem;
import com.awakenedmod.awakened.content.shadow.ShadowBladeItem;
import com.awakenedmod.awakened.content.spider.WebGauntletsItem;
import com.awakenedmod.awakened.content.sun.BlankSunAxeItem;
import com.awakenedmod.awakened.content.velocity.EnergyBarItem;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.CatalystConfig;
import com.awakenedmod.awakened.core.relic.Relic;
import com.awakenedmod.awakened.core.relic.RelicRegistry;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Items;

public final class AwakenedItems {
   private static final Items ITEMS = DeferredRegister.createItems("awakened");
   private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "awakened");
   public static final DeferredItem<CatalystItem> CATALYST = ITEMS.registerItem(
      "catalyst", properties -> new CatalystItem(properties.stacksTo(1).rarity(Rarity.EPIC).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true))
   );
   public static final DeferredItem<FocusedCatalystItem> FOCUSED_CATALYST = focusedCatalyst("focused_catalyst", CatalystConfig.FOCUSED);
   public static final DeferredItem<FocusedCatalystItem> HONED_CATALYST = focusedCatalyst("honed_catalyst", CatalystConfig.HONED);
   public static final DeferredItem<FocusedCatalystItem> REFINED_CATALYST = focusedCatalyst("refined_catalyst", CatalystConfig.REFINED);
   public static final DeferredItem<FocusedCatalystItem> PRECISE_CATALYST = focusedCatalyst("precise_catalyst", CatalystConfig.PRECISE);
   public static final DeferredItem<FocusedCatalystItem> PERFECT_CATALYST = focusedCatalyst("perfect_catalyst", CatalystConfig.PERFECT);
   public static final DeferredItem<ShadowBladeItem> SHADOW_BLADE = ITEMS.registerItem(
      "shadow_blade",
      properties -> new ShadowBladeItem(
            properties.stacksTo(1)
               .rarity(Rarity.EPIC)
               .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 3.0F, -2.4F))
               .component(DataComponents.TOOL, SwordItem.createToolProperties())
               .component(DataComponents.UNBREAKABLE, new Unbreakable(false))
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.shadow_blade.tooltip"))))
         )
   );
   public static final DeferredItem<ArchEvokerStaffItem> ARCHEVOKER_STAFF = ITEMS.registerItem(
      "archevoker_staff",
      properties -> new ArchEvokerStaffItem(
            properties.stacksTo(1)
               .rarity(Rarity.EPIC)
               .component(DataComponents.UNBREAKABLE, new Unbreakable(false))
               .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.archevoker_staff.tooltip"))))
         )
   );
   public static final DeferredItem<Item> SUN_AXE = ITEMS.registerItem(
      "sun_axe",
      properties -> new Item(
            properties.stacksTo(1)
               .rarity(Rarity.EPIC)
               .attributes(DiggerItem.createAttributes(Tiers.NETHERITE, 6.0F, -2.8F))
               .component(DataComponents.TOOL, Tiers.NETHERITE.createToolProperties(BlockTags.MINEABLE_WITH_AXE))
               .component(DataComponents.UNBREAKABLE, new Unbreakable(false))
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.sun_axe.tooltip"))))
         )
   );
   public static final DeferredItem<Item> DORMANT_CORE = ITEMS.registerItem(
      "dormant_core",
      properties -> new Item(
            properties.stacksTo(16)
               .rarity(Rarity.EPIC)
               .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.dormant_core.tooltip"))))
         )
   );
   public static final DeferredItem<Item> SUN_AXE_HEAD = ITEMS.registerItem(
      "sun_axe_head",
      properties -> new Item(
            properties.rarity(Rarity.RARE).component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.sun_axe_head.tooltip"))))
         )
   );
   public static final DeferredItem<Item> SUN_AXE_HANDLE = ITEMS.registerItem(
      "sun_axe_handle",
      properties -> new Item(
            properties.rarity(Rarity.RARE)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.sun_axe_handle.tooltip"))))
         )
   );
   public static final DeferredItem<BlankSunAxeItem> BLANK_SUN_AXE = ITEMS.registerItem(
      "blank_sun_axe",
      properties -> new BlankSunAxeItem(
            properties.stacksTo(1)
               .rarity(Rarity.UNCOMMON)
               .attributes(DiggerItem.createAttributes(Tiers.WOOD, 6.0F, -3.2F))
               .component(DataComponents.TOOL, Tiers.WOOD.createToolProperties(BlockTags.MINEABLE_WITH_AXE))
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.blank_sun_axe.tooltip"))))
         )
   );
   public static final DeferredItem<Item> CORDON_CORE = ITEMS.registerItem(
      "cordon_core",
      properties -> new Item(
            properties.stacksTo(16)
               .rarity(Rarity.EPIC)
               .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.cordon_core.tooltip"))))
         )
   );
   public static final DeferredItem<Item> CORDON_SPEAR = ITEMS.registerItem(
      "cordon_spear",
      properties -> new Item(
            properties.attributes(SwordItem.createAttributes(Tiers.NETHERITE, 0.5F, -2.8F))
               .component(DataComponents.TOOL, SwordItem.createToolProperties())
               .fireResistant()
               .rarity(Rarity.RARE)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.cordon_spear.tooltip"))))
         )
   );
   public static final DeferredItem<Item> NUKE_CHARGE = ITEMS.registerItem("nuke_charge", Item::new);
   public static final DeferredItem<Item> RAW_ADAMANTIUM = ITEMS.registerItem("raw_adamantium", properties -> new Item(properties.rarity(Rarity.RARE)));
   public static final DeferredItem<Item> ADAMANTIUM_INGOT = ITEMS.registerItem(
      "adamantium_ingot",
      properties -> new Item(
            properties.rarity(Rarity.RARE)
               .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.adamantium_ingot.tooltip"))))
         )
   );
   public static final DeferredItem<BlockItem> ADAMANTIUM_ORE = blockItem("adamantium_ore", AwakenedBlocks.ADAMANTIUM_ORE);
   public static final DeferredItem<RelicItem> RELIC = ITEMS.registerItem(
      "relic", properties -> new RelicItem(properties.stacksTo(16).rarity(Rarity.EPIC).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true))
   );
   public static final DeferredItem<TrackerItem> TRACKER = ITEMS.registerItem(
      "tracker",
      properties -> new TrackerItem(
            properties.stacksTo(1)
               .rarity(Rarity.UNCOMMON)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.tracker.tooltip"))))
         )
   );
   public static final DeferredItem<ArbiterSummonsItem> ARBITER_SUMMONS = ITEMS.registerItem(
      "arbiter_summons",
      properties -> new ArbiterSummonsItem(
            properties.stacksTo(16)
               .rarity(Rarity.EPIC)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.arbiter_summons.tooltip"))))
         )
   );
   public static final DeferredItem<WebGauntletsItem> WEB_GAUNTLETS = ITEMS.registerItem(
      "web_gauntlets",
      properties -> new WebGauntletsItem(
            properties.stacksTo(1)
               .rarity(Rarity.UNCOMMON)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.web_gauntlets.tooltip"))))
         )
   );
   public static final DeferredItem<RelicBeltItem> RELIC_BELT = ITEMS.registerItem(
      "relic_belt",
      properties -> new RelicBeltItem(
            properties.stacksTo(1)
               .rarity(Rarity.RARE)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.relic_belt.tooltip"))))
         )
   );
   public static final DeferredItem<ContainmentJarItem> CONTAINMENT_JAR = ITEMS.registerItem(
      "containment_jar",
      properties -> new ContainmentJarItem(
            properties.stacksTo(1)
               .rarity(Rarity.UNCOMMON)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.containment_jar.tooltip"))))
         )
   );
   public static final DeferredItem<PrismItem> PRISM = ITEMS.registerItem("prism", properties -> new PrismItem(properties.stacksTo(16).rarity(Rarity.UNCOMMON)));
   public static final DeferredItem<EnergyBarItem> ENERGY_BAR = ITEMS.registerItem(
      "energy_bar",
      properties -> new EnergyBarItem(
            EnergyBarItem.food(properties).component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.energy_bar.tooltip"))))
         )
   );
   public static final DeferredItem<BlockItem> INFUSION_TANK = blockItem(
      "infusion_tank",
      AwakenedBlocks.INFUSION_TANK,
      properties -> properties.rarity(Rarity.RARE)
            .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.infusion_tank.tooltip"))))
   );
   public static final DeferredItem<IaiBladeItem> IAI_BLADE = ITEMS.registerItem(
      "iai_blade",
      properties -> new IaiBladeItem(
            properties.stacksTo(1)
               .rarity(Rarity.EPIC)
               .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 2.5F, -2.2F))
               .component(DataComponents.TOOL, SwordItem.createToolProperties())
         )
   );
   public static final DeferredItem<VerdictChainItem> VERDICT_CHAIN = ITEMS.registerItem(
      "verdict_chain",
      properties -> new VerdictChainItem(
            properties.stacksTo(1)
               .rarity(Rarity.EPIC)
               .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 1.5F, -2.6F))
               .component(DataComponents.TOOL, SwordItem.createToolProperties())
               .attributes(verdictChainAttributes())
         )
   );
   public static final DeferredItem<ReaperScytheItem> REAPER_SCYTHE = ITEMS.registerItem(
      "reaper_scythe",
      properties -> new ReaperScytheItem(
            properties.stacksTo(1)
               .rarity(Rarity.EPIC)
               .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 4.0F, -2.8F))
               .component(DataComponents.TOOL, SwordItem.createToolProperties())
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.reaper_scythe.tooltip"))))
         )
   );
   public static final DeferredItem<Item> SCYTHE_EDGE = ITEMS.registerItem(
      "scythe_edge",
      properties -> new Item(
            properties.rarity(Rarity.RARE).component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.scythe_edge.tooltip"))))
         )
   );
   public static final DeferredItem<ExosavantWarhammerItem> EXOSAVANT_WARHAMMER = ITEMS.registerItem(
      "exosavant_warhammer",
      properties -> new ExosavantWarhammerItem(
            properties.stacksTo(1)
               .rarity(Rarity.EPIC)
               .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 5.0F, -3.4F))
               .component(DataComponents.TOOL, SwordItem.createToolProperties())
               .component(DataComponents.UNBREAKABLE, new Unbreakable(false))
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.exosavant_warhammer.tooltip"))))
         )
   );
   public static final DeferredItem<Item> EXOSAVANT_GAUNTLETS = exosavantPiece("exosavant_gauntlets", Rarity.UNCOMMON, false);
   public static final DeferredItem<Item> EXOSAVANT_BOOTS = exosavantArmor("exosavant_boots", Rarity.UNCOMMON, EquipmentSlot.FEET);
   public static final DeferredItem<Item> EXOSAVANT_CHESTPLATE = exosavantArmor("exosavant_chestplate", Rarity.RARE, EquipmentSlot.CHEST);
   public static final DeferredItem<Item> EXOSAVANT_LEGGINGS = exosavantArmor("exosavant_leggings", Rarity.RARE, EquipmentSlot.LEGS);
   public static final DeferredItem<Item> EXOSAVANT_HELMET = exosavantArmor("exosavant_helmet", Rarity.RARE, EquipmentSlot.HEAD);
   public static final DeferredItem<Item> EXOSAVANT_CHIP = exosavantPiece("exosavant_chip", Rarity.EPIC, true);
   public static final DeferredItem<SoulVesselItem> SOUL_VESSEL = ITEMS.registerItem(
      "soul_vessel",
      properties -> new SoulVesselItem(
            properties.stacksTo(16)
               .rarity(Rarity.UNCOMMON)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.soul_vessel.tooltip")))),
            false
         )
   );
   public static final DeferredItem<SoulVesselItem> SOUL_CORE = ITEMS.registerItem(
      "soul_core",
      properties -> new SoulVesselItem(
            properties.stacksTo(16)
               .rarity(Rarity.EPIC)
               .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.soul_core.tooltip")))),
            true
         )
   );
   public static final DeferredItem<BlockItem> SIEGE_ALTAR = blockItem(
      "siege_altar",
      AwakenedBlocks.SIEGE_ALTAR,
      properties -> properties.rarity(Rarity.UNCOMMON)
            .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.siege_altar.tooltip"))))
   );
   public static final DeferredItem<BlockItem> CHALLENGE_POST = blockItem(
      "challenge_post",
      AwakenedBlocks.CHALLENGE_POST,
      properties -> properties.rarity(Rarity.UNCOMMON)
            .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened.challenge_post.tooltip"))))
   );
   public static final DeferredItem<VesselItem> WATER_FLASK = ITEMS.registerItem(
      "water_flask", properties -> new VesselItem(properties.stacksTo(1).rarity(Rarity.COMMON), 4, 1)
   );
   public static final DeferredItem<VesselItem> WATER_CANTEEN = ITEMS.registerItem(
      "water_canteen", properties -> new VesselItem(properties.stacksTo(1).rarity(Rarity.UNCOMMON), 10, 2)
   );
   public static final DeferredItem<VesselItem> WATER_RESERVOIR = ITEMS.registerItem(
      "water_reservoir", properties -> new VesselItem(properties.stacksTo(1).rarity(Rarity.RARE), 20, 3)
   );
   public static final DeferredItem<VesselItem> TIDAL_VESSEL = ITEMS.registerItem(
      "tidal_vessel", properties -> new VesselItem(properties.stacksTo(1).rarity(Rarity.EPIC), 32, 4)
   );
   public static final DeferredItem<VesselItem> RESERVOIR_CORE = ITEMS.registerItem(
      "reservoir_core",
      properties -> new VesselItem(properties.stacksTo(1).rarity(Rarity.EPIC).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true), 100, 5)
   );
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register(
      "main",
      () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.awakened"))
            .icon(() -> new ItemStack((ItemLike)CATALYST.get()))
            .displayItems((params, output) -> {
               output.accept(new ItemStack((ItemLike)CATALYST.get()));
               output.accept(new ItemStack((ItemLike)FOCUSED_CATALYST.get()));
               output.accept(new ItemStack((ItemLike)HONED_CATALYST.get()));
               output.accept(new ItemStack((ItemLike)REFINED_CATALYST.get()));
               output.accept(new ItemStack((ItemLike)PRECISE_CATALYST.get()));
               output.accept(new ItemStack((ItemLike)PERFECT_CATALYST.get()));
               output.accept(new ItemStack((ItemLike)SHADOW_BLADE.get()));
               output.accept(new ItemStack((ItemLike)ARCHEVOKER_STAFF.get()));
               output.accept(new ItemStack((ItemLike)SUN_AXE.get()));
               output.accept(new ItemStack((ItemLike)BLANK_SUN_AXE.get()));
               output.accept(new ItemStack((ItemLike)DORMANT_CORE.get()));
               output.accept(new ItemStack((ItemLike)SUN_AXE_HEAD.get()));
               output.accept(new ItemStack((ItemLike)SUN_AXE_HANDLE.get()));
               output.accept(new ItemStack((ItemLike)CORDON_SPEAR.get()));
               output.accept(new ItemStack((ItemLike)CORDON_CORE.get()));
               output.accept(new ItemStack((ItemLike)TRACKER.get()));
               output.accept(new ItemStack((ItemLike)ARBITER_SUMMONS.get()));
               output.accept(new ItemStack((ItemLike)WEB_GAUNTLETS.get()));
               output.accept(new ItemStack((ItemLike)RELIC_BELT.get()));
               output.accept(new ItemStack((ItemLike)CONTAINMENT_JAR.get()));
               output.accept(new ItemStack((ItemLike)RAW_ADAMANTIUM.get()));
               output.accept(new ItemStack((ItemLike)ADAMANTIUM_INGOT.get()));
               output.accept(new ItemStack((ItemLike)ADAMANTIUM_ORE.get()));
               output.accept(new ItemStack((ItemLike)INFUSION_TANK.get()));
               output.accept(new ItemStack((ItemLike)SIEGE_ALTAR.get()));
               output.accept(new ItemStack((ItemLike)CHALLENGE_POST.get()));
               output.accept(new ItemStack((ItemLike)IAI_BLADE.get()));
               output.accept(new ItemStack((ItemLike)VERDICT_CHAIN.get()));
               output.accept(new ItemStack((ItemLike)REAPER_SCYTHE.get()));
               output.accept(new ItemStack((ItemLike)SCYTHE_EDGE.get()));
               output.accept(new ItemStack((ItemLike)EXOSAVANT_GAUNTLETS.get()));
               output.accept(new ItemStack((ItemLike)EXOSAVANT_BOOTS.get()));
               output.accept(new ItemStack((ItemLike)EXOSAVANT_CHESTPLATE.get()));
               output.accept(new ItemStack((ItemLike)EXOSAVANT_LEGGINGS.get()));
               output.accept(new ItemStack((ItemLike)EXOSAVANT_HELMET.get()));
               output.accept(new ItemStack((ItemLike)EXOSAVANT_CHIP.get()));
               output.accept(new ItemStack((ItemLike)SOUL_VESSEL.get()));
               output.accept(new ItemStack((ItemLike)SOUL_CORE.get()));

               for (DyeColor dye : DyeColor.values()) {
                  output.accept(PrismItem.of(dye));
               }

               output.accept(new ItemStack((ItemLike)ENERGY_BAR.get()));
               output.accept(((VesselItem)WATER_FLASK.get()).filled());
               output.accept(((VesselItem)WATER_CANTEEN.get()).filled());
               output.accept(((VesselItem)WATER_RESERVOIR.get()).filled());
               output.accept(((VesselItem)TIDAL_VESSEL.get()).filled());
               output.accept(((VesselItem)RESERVOIR_CORE.get()).filled());

               for (Relic relic : RelicRegistry.all()) {
                  output.accept(RelicItem.of(relic.id()));
               }
            })
            .build()
   );

   private static DeferredItem<FocusedCatalystItem> focusedCatalyst(String id, CatalystConfig.Tier tier) {
      return ITEMS.registerItem(
         id,
         properties -> new FocusedCatalystItem(
               properties.stacksTo(1).rarity(Rarity.EPIC).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true), tier::picks, tier::xpCost
            )
      );
   }

   private static DeferredItem<BlockItem> blockItem(String id, Supplier<? extends Block> block, UnaryOperator<Properties> extras) {
      return ITEMS.registerItem(id, properties -> {
         Properties named = extras.apply(properties);
         return new BlockItem(block.get(), named);
      });
   }

   private static DeferredItem<BlockItem> blockItem(String id, Supplier<? extends Block> block) {
      return blockItem(id, block, UnaryOperator.identity());
   }

   private static ItemAttributeModifiers verdictChainAttributes() {
      Operation add = Operation.ADD_VALUE;
      EquipmentSlotGroup mainhand = EquipmentSlotGroup.MAINHAND;
      return ItemAttributeModifiers.builder()
         .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 5.5, add), mainhand)
         .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.6, add), mainhand)
         .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(AwId.of("verdict_chain_reach"), 1.0, add), mainhand)
         .build();
   }

   private static DeferredItem<Item> exosavantPiece(String id, Rarity rarity, boolean glint) {
      return ITEMS.registerItem(
         id,
         properties -> {
            properties.stacksTo(1)
               .rarity(rarity)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened." + id + ".tooltip"))));
            if (glint) {
               properties.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            }

            return new ExoCurioItem(properties);
         }
      );
   }

   private static DeferredItem<Item> exosavantArmor(String id, Rarity rarity, EquipmentSlot slot) {
      return ITEMS.registerItem(
         id,
         properties -> {
            properties.stacksTo(1)
               .rarity(rarity)
               .attributes(exoArmorAttributes(slot))
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.awakened." + id + ".tooltip"))));
            return new ExoArmorPieceItem(properties, slot);
         }
      );
   }

   private static ItemAttributeModifiers exoArmorAttributes(EquipmentSlot slot) {
      int armor;
      EquipmentSlotGroup group;
      String idPath;
      switch (slot) {
         case HEAD:
            armor = 3;
            group = EquipmentSlotGroup.HEAD;
            idPath = "exo_armor_head";
            break;
         case CHEST:
            armor = 8;
            group = EquipmentSlotGroup.CHEST;
            idPath = "exo_armor_chest";
            break;
         case LEGS:
            armor = 6;
            group = EquipmentSlotGroup.LEGS;
            idPath = "exo_armor_legs";
            break;
         case FEET:
            armor = 3;
            group = EquipmentSlotGroup.FEET;
            idPath = "exo_armor_feet";
            break;
         default:
            armor = 0;
            group = EquipmentSlotGroup.ARMOR;
            idPath = "exo_armor_other";
      }

      ResourceLocation modId = AwId.of(idPath);
      Operation add = Operation.ADD_VALUE;
      return ItemAttributeModifiers.builder()
         .add(Attributes.ARMOR, new AttributeModifier(modId, (double)armor, add), group)
         .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(modId, 3.0, add), group)
         .add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(modId, 0.1, add), group)
         .build();
   }

   private AwakenedItems() {
   }

   public static void register(IEventBus modEventBus) {
      RelicItems.registerAll(ITEMS);
      ITEMS.register(modEventBus);
      TABS.register(modEventBus);
      modEventBus.addListener(AwakenedItems::addToCreativeTabs);
   }

   private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
      if (event.getTabKey() == CreativeModeTabs.COMBAT) {
         event.accept(SHADOW_BLADE);
         event.accept(SUN_AXE);
         event.accept(BLANK_SUN_AXE);
         event.accept(CORDON_SPEAR);
         event.accept(IAI_BLADE);
         event.accept(VERDICT_CHAIN);
         event.accept(REAPER_SCYTHE);
         event.accept(EXOSAVANT_GAUNTLETS);
         event.accept(EXOSAVANT_BOOTS);
         event.accept(EXOSAVANT_CHESTPLATE);
         event.accept(EXOSAVANT_LEGGINGS);
         event.accept(EXOSAVANT_HELMET);
         event.accept(EXOSAVANT_CHIP);
      } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
         event.accept(CATALYST);
         event.accept(FOCUSED_CATALYST);
         event.accept(HONED_CATALYST);
         event.accept(REFINED_CATALYST);
         event.accept(PRECISE_CATALYST);
         event.accept(PERFECT_CATALYST);
         event.accept(TRACKER);
         event.accept(ARBITER_SUMMONS);
         event.accept(WEB_GAUNTLETS);
         event.accept(RELIC_BELT);
         event.accept(CONTAINMENT_JAR);
         event.accept(SOUL_VESSEL);
         event.accept(SOUL_CORE);

         for (DyeColor dye : DyeColor.values()) {
            event.accept(PrismItem.of(dye));
         }

         event.accept(((VesselItem)WATER_FLASK.get()).filled());
         event.accept(((VesselItem)WATER_CANTEEN.get()).filled());
         event.accept(((VesselItem)WATER_RESERVOIR.get()).filled());
         event.accept(((VesselItem)TIDAL_VESSEL.get()).filled());
         event.accept(((VesselItem)RESERVOIR_CORE.get()).filled());

         for (Relic relic : RelicRegistry.all()) {
            event.accept(RelicItem.of(relic.id()));
         }
      } else if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
         event.accept(ENERGY_BAR);
      } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
         event.accept(RAW_ADAMANTIUM);
         event.accept(ADAMANTIUM_INGOT);
         event.accept(CORDON_CORE);
         event.accept(DORMANT_CORE);
         event.accept(SUN_AXE_HEAD);
         event.accept(SUN_AXE_HANDLE);
         event.accept(SCYTHE_EDGE);
      } else if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
         event.accept(ADAMANTIUM_ORE);
      } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
         event.accept(INFUSION_TANK);
         event.accept(SIEGE_ALTAR);
         event.accept(CHALLENGE_POST);
      }
   }
}
