package com.seb.projecttitancore;

import com.mojang.logging.LogUtils;
import com.seb.projecttitancore.block.TitanCoreBlock;
import com.seb.projecttitancore.blockentity.TitanCoreBlockEntity;
import com.seb.projecttitancore.item.TitanShardItem;
import com.seb.projecttitancore.menu.TitanCoreMenu;
import com.seb.projecttitancore.recipe.TitanCoreRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(ProjectTitanCore.MODID)
public class ProjectTitanCore {
    public static final String MODID = "projecttitancore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, MODID);

    private static DeferredHolder<SoundEvent, SoundEvent> registerSound(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, name)));
    }

    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_TITAN_CORE_IDLE = registerSound("titan_core_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_TITAN_CORE_CRAFTING = registerSound("titan_core_crafting");

    public static final DeferredHolder<RecipeType<?>, RecipeType<TitanCoreRecipe>> TITAN_CORE_RECIPE_TYPE =
            RECIPE_TYPES.register("titan_core", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MODID, "titan_core")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TitanCoreRecipe>> TITAN_CORE_RECIPE_SERIALIZER =
            RECIPE_SERIALIZERS.register("titan_core", TitanCoreRecipe.Serializer::new);

    public static final DeferredBlock<TitanCoreBlock> TITAN_CORE_BLOCK = BLOCKS.register("titan_core",
            () -> new TitanCoreBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    // Glass cage model is not a full cube — without noOcclusion, neighbouring
                    // blocks cull their faces against the Core's full AABB and the translucent
                    // gaps in the cage become x-ray windows into anything below.
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(TitanCoreBlock.CRAFTING) ? 15 : 0)));

    public static final DeferredItem<BlockItem> TITAN_CORE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("titan_core", TITAN_CORE_BLOCK);

    private static BlockBehaviour.Properties holyBrickProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.SAND)
                .strength(2.0f, 6.0f)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    public static final DeferredBlock<Block> HOLY_BRICKS = BLOCKS.register("holy_bricks",
            () -> new Block(holyBrickProps()));
    public static final DeferredItem<BlockItem> HOLY_BRICKS_ITEM = ITEMS.registerSimpleBlockItem("holy_bricks", HOLY_BRICKS);

    public static final DeferredBlock<Block> CHISELED_HOLY_BRICKS = BLOCKS.register("chiseled_holy_bricks",
            () -> new Block(holyBrickProps()));
    public static final DeferredItem<BlockItem> CHISELED_HOLY_BRICKS_ITEM = ITEMS.registerSimpleBlockItem("chiseled_holy_bricks", CHISELED_HOLY_BRICKS);

    public static final DeferredBlock<Block> HOLY_BRICK_PILLAR = BLOCKS.register("holy_brick_pillar",
            () -> new Block(holyBrickProps()));
    public static final DeferredItem<BlockItem> HOLY_BRICK_PILLAR_ITEM = ITEMS.registerSimpleBlockItem("holy_brick_pillar", HOLY_BRICK_PILLAR);

    public static final DeferredBlock<Block> HOLY_BRICK_TILES = BLOCKS.register("holy_brick_tiles",
            () -> new Block(holyBrickProps()));
    public static final DeferredItem<BlockItem> HOLY_BRICK_TILES_ITEM = ITEMS.registerSimpleBlockItem("holy_brick_tiles", HOLY_BRICK_TILES);

    public static final DeferredBlock<Block> GILDED_HOLY_BRICKS = BLOCKS.register("gilded_holy_bricks",
            () -> new Block(holyBrickProps()));
    public static final DeferredItem<BlockItem> GILDED_HOLY_BRICKS_ITEM = ITEMS.registerSimpleBlockItem("gilded_holy_bricks", GILDED_HOLY_BRICKS);

    public static final DeferredBlock<Block> ENGRAVED_HOLY_BRICKS = BLOCKS.register("engraved_holy_bricks",
            () -> new Block(holyBrickProps()));
    public static final DeferredItem<BlockItem> ENGRAVED_HOLY_BRICKS_ITEM = ITEMS.registerSimpleBlockItem("engraved_holy_bricks", ENGRAVED_HOLY_BRICKS);

    public static final DeferredItem<TitanShardItem> MOTE_OF_THE_TITAN = ITEMS.registerItem("mote_of_the_titan", TitanShardItem::new);
    public static final DeferredItem<TitanShardItem> EMBER_OF_THE_TITAN = ITEMS.registerItem("ember_of_the_titan", TitanShardItem::new);
    public static final DeferredItem<TitanShardItem> SPARK_OF_THE_TITAN = ITEMS.registerItem("spark_of_the_titan", TitanShardItem::new);
    public static final DeferredItem<TitanShardItem> PULSE_OF_THE_TITAN = ITEMS.registerItem("pulse_of_the_titan", TitanShardItem::new);
    public static final DeferredItem<TitanShardItem> ECHO_OF_THE_TITAN = ITEMS.registerItem("echo_of_the_titan", TitanShardItem::new);
    public static final DeferredItem<TitanShardItem> WILL_OF_THE_TITAN = ITEMS.registerItem("will_of_the_titan", TitanShardItem::new);
    public static final DeferredItem<TitanShardItem> VOICE_OF_THE_TITAN = ITEMS.registerItem("voice_of_the_titan", TitanShardItem::new);
    public static final DeferredItem<TitanShardItem> SOUL_OF_THE_TITAN = ITEMS.registerItem("soul_of_the_titan", TitanShardItem::new);
    public static final DeferredItem<TitanShardItem> ASCENDANT_SHARD = ITEMS.registerItem("ascendant_shard", TitanShardItem::new);
    public static final DeferredItem<TitanShardItem> HEART_OF_THE_TITAN = ITEMS.registerItem("heart_of_the_titan", TitanShardItem::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TitanCoreBlockEntity>> TITAN_CORE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("titan_core", () -> BlockEntityType.Builder.of(TitanCoreBlockEntity::new, TITAN_CORE_BLOCK.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<TitanCoreMenu>> TITAN_CORE_MENU =
            MENU_TYPES.register("titan_core", () -> new MenuType<>(TitanCoreMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PROJECT_TITAN_TAB = CREATIVE_MODE_TABS.register("project_titan",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.projecttitancore"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> TITAN_CORE_BLOCK_ITEM.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(TITAN_CORE_BLOCK_ITEM.get());
                        output.accept(HOLY_BRICKS_ITEM.get());
                        output.accept(CHISELED_HOLY_BRICKS_ITEM.get());
                        output.accept(HOLY_BRICK_PILLAR_ITEM.get());
                        output.accept(HOLY_BRICK_TILES_ITEM.get());
                        output.accept(GILDED_HOLY_BRICKS_ITEM.get());
                        output.accept(ENGRAVED_HOLY_BRICKS_ITEM.get());
                        output.accept(MOTE_OF_THE_TITAN.get());
                        output.accept(EMBER_OF_THE_TITAN.get());
                        output.accept(SPARK_OF_THE_TITAN.get());
                        output.accept(PULSE_OF_THE_TITAN.get());
                        output.accept(ECHO_OF_THE_TITAN.get());
                        output.accept(WILL_OF_THE_TITAN.get());
                        output.accept(VOICE_OF_THE_TITAN.get());
                        output.accept(SOUL_OF_THE_TITAN.get());
                        output.accept(ASCENDANT_SHARD.get());
                        output.accept(HEART_OF_THE_TITAN.get());
                    })
                    .build());

    public ProjectTitanCore(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        modEventBus.addListener(ProjectTitanCore::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, TITAN_CORE_BLOCK_ENTITY.get(),
                (be, side) -> be.energyStorage);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TITAN_CORE_BLOCK_ENTITY.get(),
                (be, side) -> be.fluidTank);
    }
}
