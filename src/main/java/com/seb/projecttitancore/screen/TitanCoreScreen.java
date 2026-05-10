package com.seb.projecttitancore.screen;

import com.seb.projecttitancore.blockentity.TitanCoreBlockEntity;
import com.seb.projecttitancore.menu.TitanCoreMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

public class TitanCoreScreen extends AbstractContainerScreen<TitanCoreMenu> {
    private static final int GUI_WIDTH  = 176;
    private static final int GUI_HEIGHT = 166;

    // Fluid gauge: between input grid (ends x=60) and output slot (x=116).
    // HEIGHT 52 + 1px border on each side = 54px outer, exactly matching the 3x3 input grid.
    private static final int FLUID_BAR_X      = 68;
    private static final int FLUID_BAR_Y      = 17;
    private static final int FLUID_BAR_WIDTH  = 16;
    private static final int FLUID_BAR_HEIGHT = 52;

    // Energy bar: right of output slot (x=132) close to GUI right edge.
    private static final int ENERGY_BAR_X      = 152;
    private static final int ENERGY_BAR_Y      = 17;
    private static final int ENERGY_BAR_WIDTH  = 14;
    private static final int ENERGY_BAR_HEIGHT = 52;

    // Progress arrow: centred in the 30px gap between fluid gauge and output slot
    private static final int ARROW_X      = 89;
    private static final int ARROW_Y      = 35;
    private static final int ARROW_W      = 22;
    private static final int ARROW_H      = 16;

    // Void-fluid button: 8×8, sitting to the right of the fluid gauge with its
    // bottom edge flush with the gauge's bottom edge.
    private static final int VOID_BTN_SIZE = 8;
    private static final int VOID_BTN_X    = FLUID_BAR_X + FLUID_BAR_WIDTH + 2;                    // 86
    // +1 accounts for the gauge's 1px border — visual bottom is at FLUID_BAR_Y + FLUID_BAR_HEIGHT.
    private static final int VOID_BTN_Y    = FLUID_BAR_Y + FLUID_BAR_HEIGHT + 1 - VOID_BTN_SIZE;   // 62

    // Holy palette — see CLAUDE.md "Visual Theme".
    private static final int COLOR_BG          = 0xFFECE4D0; // ivory marble
    private static final int COLOR_FRAME       = 0xFF463612; // dark gold rim — outer GUI frame
    private static final int COLOR_BORDER      = 0xFF8A6620; // gold trim — inner dividers, slot/gauge borders
    private static final int COLOR_SLOT_BG     = 0xFF2A1F08; // dark warm slot well
    private static final int COLOR_OUTPUT_BG   = 0xFF4A3818; // warmer than slot bg, marks the sacred output
    private static final int COLOR_ENERGY_FILL = 0xFFB52218; // RF/FE red — shifted darker to sit closer to the deep-maroon well
    private static final int COLOR_ENERGY_BG   = 0xFF2A0808; // deep maroon well to match
    private static final int COLOR_VOID_X      = 0xFFE53A2F; // bright red for the void-fluid X — kept loud as a cancel cue
    private static final int COLOR_FLUID_BG    = 0xFF1F2A38;
    private static final int COLOR_ARROW_BG    = 0xFF1F1608;
    private static final int COLOR_ARROW_FILL  = 0xFFFFE054;
    private static final int COLOR_LABEL       = 0xFF463612; // dark gold text on ivory bg

    public TitanCoreScreen(TitanCoreMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth  = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // Main background
        g.fill(x, y, x + imageWidth, y + imageHeight, COLOR_BG);

        // Outer border
        drawBorder(g, x, y, imageWidth, imageHeight);

        // 3×3 input slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                drawSlot(g, x + 8 + col * 18, y + 17 + row * 18, COLOR_SLOT_BG);
            }
        }

        // Output slot
        drawSlot(g, x + 116, y + 35, COLOR_OUTPUT_BG);

        // Player inventory (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(g, x + 8 + col * 18, y + 84 + row * 18, COLOR_SLOT_BG);
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            drawSlot(g, x + 8 + col * 18, y + 142, COLOR_SLOT_BG);
        }

        // Separator between machine area and player inventory
        g.fill(x + 7, y + 80, x + imageWidth - 7, y + 81, COLOR_BORDER);

        // --- Progress arrow ---
        int ax = x + ARROW_X;
        int ay = y + ARROW_Y;
        drawArrowShape(g, ax, ay, COLOR_ARROW_BG);
        int progress    = menu.getCraftingProgress();
        int maxProgress = menu.getMaxCraftingProgress();
        if (maxProgress > 0 && progress > 0) {
            int fillW = progress * ARROW_W / maxProgress;
            g.enableScissor(ax, ay, ax + fillW, ay + ARROW_H);
            drawArrowShape(g, ax, ay, COLOR_ARROW_FILL);
            g.disableScissor();
        }

        // --- Fluid gauge ---
        int fx = x + FLUID_BAR_X;
        int fy = y + FLUID_BAR_Y;
        g.fill(fx - 1, fy - 1, fx + FLUID_BAR_WIDTH + 1, fy + FLUID_BAR_HEIGHT + 1, COLOR_BORDER);
        g.fill(fx, fy, fx + FLUID_BAR_WIDTH, fy + FLUID_BAR_HEIGHT, COLOR_FLUID_BG);
        int fluidAmount   = menu.getFluidAmount();
        int fluidCapacity = menu.getFluidCapacity();
        FluidStack fluidStack = getDisplayedFluid();
        if (fluidCapacity > 0 && fluidAmount > 0 && !fluidStack.isEmpty()) {
            int fillH = (int) ((long) fluidAmount * FLUID_BAR_HEIGHT / fluidCapacity);
            renderFluidTiled(g, fx, fy + FLUID_BAR_HEIGHT - fillH,
                    FLUID_BAR_WIDTH, fillH, fluidStack);
        }

        // --- Energy bar ---
        int ex = x + ENERGY_BAR_X;
        int ey = y + ENERGY_BAR_Y;
        g.fill(ex - 1, ey - 1, ex + ENERGY_BAR_WIDTH + 1, ey + ENERGY_BAR_HEIGHT + 1, COLOR_BORDER);
        g.fill(ex, ey, ex + ENERGY_BAR_WIDTH, ey + ENERGY_BAR_HEIGHT, COLOR_ENERGY_BG);
        int energy    = menu.getEnergy();
        int maxEnergy = menu.getMaxEnergy();
        if (maxEnergy > 0 && energy > 0) {
            int fillH = (int) ((long) energy * ENERGY_BAR_HEIGHT / maxEnergy);
            g.fill(ex, ey + ENERGY_BAR_HEIGHT - fillH,
                   ex + ENERGY_BAR_WIDTH, ey + ENERGY_BAR_HEIGHT, COLOR_ENERGY_FILL);
        }

        // --- Void-fluid button: black frame, ivory interior, red X ---
        drawVoidFluidButton(g, x + VOID_BTN_X, y + VOID_BTN_Y);
    }

    private static void drawVoidFluidButton(GuiGraphics g, int bx, int by) {
        g.fill(bx, by, bx + VOID_BTN_SIZE, by + VOID_BTN_SIZE, 0xFF000000);             // black frame
        g.fill(bx + 1, by + 1, bx + VOID_BTN_SIZE - 1, by + VOID_BTN_SIZE - 1, COLOR_BG); // ivory interior (6×6)
        // Red X across the 6×6 interior
        for (int i = 0; i < 6; i++) {
            g.fill(bx + 1 + i, by + 1 + i, bx + 2 + i, by + 2 + i, COLOR_VOID_X); // top-left → bottom-right
            g.fill(bx + 1 + i, by + 6 - i, bx + 2 + i, by + 7 - i, COLOR_VOID_X); // bottom-left → top-right
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 5, COLOR_LABEL, false);
        g.drawString(font, playerInventoryTitle, 8, imageHeight - 94, COLOR_LABEL, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        // Energy bar tooltip
        int ex = leftPos + ENERGY_BAR_X - 1;
        int ey = topPos  + ENERGY_BAR_Y  - 1;
        if (mouseX >= ex && mouseX < ex + ENERGY_BAR_WIDTH + 2
                && mouseY >= ey && mouseY < ey + ENERGY_BAR_HEIGHT + 2) {
            g.renderTooltip(font,
                    Component.literal(menu.getEnergy() + " / " + menu.getMaxEnergy() + " RF"),
                    mouseX, mouseY);
        }

        // Fluid gauge tooltip — fluid name (if any) above the amount/capacity line.
        // Skip when the cursor is over the void-fluid button (overlaps the gauge's bottom-right footprint).
        int fx = leftPos + FLUID_BAR_X - 1;
        int fy = topPos  + FLUID_BAR_Y  - 1;
        if (!isOverVoidButton(mouseX, mouseY)
                && mouseX >= fx && mouseX < fx + FLUID_BAR_WIDTH + 2
                && mouseY >= fy && mouseY < fy + FLUID_BAR_HEIGHT + 2) {
            FluidStack stack = getDisplayedFluid();
            java.util.List<Component> lines = new java.util.ArrayList<>(2);
            if (!stack.isEmpty()) {
                lines.add(stack.getHoverName());
            }
            lines.add(Component.literal(menu.getFluidAmount() + " / " + menu.getFluidCapacity() + " mB"));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }

        // Void-fluid button tooltip
        if (isOverVoidButton(mouseX, mouseY)) {
            g.renderTooltip(font,
                    Component.translatable("tooltip.projecttitancore.void_fluid"),
                    mouseX, mouseY);
        }
    }

    private boolean isOverVoidButton(int mouseX, int mouseY) {
        int bx = leftPos + VOID_BTN_X;
        int by = topPos  + VOID_BTN_Y;
        return mouseX >= bx && mouseX < bx + VOID_BTN_SIZE
                && mouseY >= by && mouseY < by + VOID_BTN_SIZE;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverVoidButton((int) mouseX, (int) mouseY)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryButtonClick(menu.containerId, TitanCoreMenu.BUTTON_VOID_FLUID);
            }
            mc.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Reads the live fluid from the client-side BE (kept in sync via sendBlockUpdated on tank changes). */
    private FluidStack getDisplayedFluid() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return FluidStack.EMPTY;
        BlockEntity be = mc.level.getBlockEntity(menu.getBlockPos());
        return be instanceof TitanCoreBlockEntity tbe ? tbe.fluidTank.getFluid() : FluidStack.EMPTY;
    }

    /**
     * Tiles the fluid's still texture from the bottom up, clipping the topmost partial tile via scissor.
     * Animated sprites (lava, custom mod fluids) animate naturally because the atlas sprite owns the frame.
     */
    private static void renderFluidTiled(GuiGraphics g, int x, int y, int width, int height, FluidStack stack) {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(stack.getFluid());
        ResourceLocation stillTex = ext.getStillTexture(stack);
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(stillTex);
        int tint = ext.getTintColor(stack);
        int alpha = (tint >>> 24) & 0xFF;
        if (alpha == 0) alpha = 0xFF; // some fluids return ARGB with alpha=0 meaning "no tint"
        float r = ((tint >> 16) & 0xFF) / 255f;
        float gn = ((tint >> 8)  & 0xFF) / 255f;
        float b =  (tint        & 0xFF) / 255f;
        g.setColor(r, gn, b, alpha / 255f);

        int yBottom = y + height;
        int xTiles  = (width  + 15) / 16;
        int yTiles  = (height + 15) / 16;
        g.enableScissor(x, y, x + width, yBottom);
        for (int xi = 0; xi < xTiles; xi++) {
            for (int yi = 0; yi < yTiles; yi++) {
                int tileX = x + xi * 16;
                int tileY = yBottom - (yi + 1) * 16;
                g.blit(tileX, tileY, 0, 16, 16, sprite);
            }
        }
        g.disableScissor();
        g.setColor(1f, 1f, 1f, 1f);
    }

    private static void drawSlot(GuiGraphics g, int x, int y, int fillColor) {
        g.fill(x - 1, y - 1, x + 17, y + 17, COLOR_BORDER);
        g.fill(x, y, x + 16, y + 16, fillColor);
    }

    /** Right-pointing arrow (22×16). Body is left 13px; arrowhead tapers right to a 2px tip. */
    private static void drawArrowShape(GuiGraphics g, int ax, int ay, int color) {
        g.fill(ax,    ay + 5, ax + 13, ay + 11, color); // body
        g.fill(ax + 13, ay + 3, ax + 15, ay + 13, color); // head step 1 (10px tall)
        g.fill(ax + 15, ay + 4, ax + 17, ay + 12, color); // head step 2 (8px tall)
        g.fill(ax + 17, ay + 5, ax + 19, ay + 11, color); // head step 3 (6px tall)
        g.fill(ax + 19, ay + 6, ax + 21, ay + 10, color); // head step 4 (4px tall)
        g.fill(ax + 21, ay + 7, ax + 22, ay + 9,  color); // tip (2px tall)
    }

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + 1, COLOR_FRAME);
        g.fill(x, y + h - 1, x + w, y + h, COLOR_FRAME);
        g.fill(x, y, x + 1, y + h, COLOR_FRAME);
        g.fill(x + w - 1, y, x + w, y + h, COLOR_FRAME);
    }
}
