package com.seb.projecttitancore;

import com.mojang.logging.LogUtils;
import com.seb.projecttitancore.block.TitanCoreBlock;
import com.seb.projecttitancore.blockentity.TitanCoreBlockEntity;
import com.seb.projecttitancore.item.TrophyItem;
import com.seb.projecttitancore.menu.TitanCoreMenu;
import com.seb.projecttitancore.recipe.TitanCoreRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
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

    public static final DeferredHolder<RecipeType<?>, RecipeType<TitanCoreRecipe>> TITAN_CORE_RECIPE_TYPE =
            RECIPE_TYPES.register("titan_core", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MODID, "titan_core")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TitanCoreRecipe>> TITAN_CORE_RECIPE_SERIALIZER =
            RECIPE_SERIALIZERS.register("titan_core", TitanCoreRecipe.Serializer::new);

    public static final DeferredBlock<TitanCoreBlock> TITAN_CORE_BLOCK = BLOCKS.register("titan_core",
            () -> new TitanCoreBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .sound(SoundType.METAL)));

    public static final DeferredItem<BlockItem> TITAN_CORE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("titan_core", TITAN_CORE_BLOCK);

    public static final DeferredItem<TrophyItem> TROPHY_TIER_1 = ITEMS.registerItem("trophy_tier_1", TrophyItem::new);

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
                        output.accept(TROPHY_TIER_1.get());
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
        modEventBus.addListener(ProjectTitanCore::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, TITAN_CORE_BLOCK_ENTITY.get(),
                (be, side) -> be.energyStorage);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TITAN_CORE_BLOCK_ENTITY.get(),
                (be, side) -> be.fluidTank);
    }
}
