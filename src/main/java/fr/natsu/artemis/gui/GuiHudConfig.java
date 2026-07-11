package fr.natsu.artemis.gui;

import java.util.ArrayList;
import java.util.List;

import fr.natsu.artemis.ArtemisConfig;
import fr.natsu.artemis.render.CooldownRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

/**
 * Écran de configuration des HUD Artemis : déplace à la souris les éléments (cooldowns, inventaire),
 * choisit le sens des cooldowns et active/désactive le HUD d'inventaire. Sauvegardé à la fermeture.
 */
public final class GuiHudConfig extends GuiScreen {

    private final List<HudElement> elements = new ArrayList<>();
    private HudElement dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    private GuiButton directionButton;

    public GuiHudConfig() {
        this.elements.add(cooldownElement());
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.directionButton = new GuiButton(2, this.width / 2 - 100, this.height - 52, 200, 20, directionLabel());
        this.buttonList.add(this.directionButton);
        this.buttonList.add(new GuiButton(0, this.width / 2 - 152, this.height - 28, 150, 20,
            I18n.format("artemis.config.done")));
        this.buttonList.add(new GuiButton(1, this.width / 2 + 2, this.height - 28, 150, 20,
            I18n.format("artemis.config.reset")));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, I18n.format("artemis.config.title"), this.width / 2, 12, 0xFFFFFF);
        this.drawCenteredString(this.fontRendererObj, I18n.format("artemis.config.hint"), this.width / 2, 24, 0xFFAAAAAA);

        for (HudElement element : this.elements) {
            int cx = Math.round(element.getX() * this.width);
            int cy = Math.round(element.getY() * this.height);
            int frame = element == this.dragging ? 0x804CAF50 : 0x40FFFFFF;
            drawRect(cx - element.halfWidth(), cy - element.halfHeight(),
                cx + element.halfWidth(), cy + element.halfHeight(), frame);
            element.renderPreview(cx, cy);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        if (mouseButton == 0) {
            for (HudElement element : this.elements) {
                int cx = Math.round(element.getX() * this.width);
                int cy = Math.round(element.getY() * this.height);
                if (Math.abs(mouseX - cx) <= element.halfWidth() && Math.abs(mouseY - cy) <= element.halfHeight()) {
                    this.dragging = element;
                    this.dragOffsetX = mouseX - cx;
                    this.dragOffsetY = mouseY - cy;
                    break;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (this.dragging != null) {
            this.dragging.setX(clamp((mouseX - this.dragOffsetX) / (float) this.width));
            this.dragging.setY(clamp((mouseY - this.dragOffsetY) / (float) this.height));
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        this.dragging = null;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                this.mc.displayGuiScreen(null);
                break;
            case 1:
                ArtemisConfig.cooldownX = ArtemisConfig.DEFAULT_COOLDOWN_X;
                ArtemisConfig.cooldownY = ArtemisConfig.DEFAULT_COOLDOWN_Y;
                ArtemisConfig.cooldownDirection = ArtemisConfig.CooldownDirection.RIGHT;
                this.directionButton.displayString = directionLabel();
                break;
            case 2:
                ArtemisConfig.cooldownDirection = ArtemisConfig.cooldownDirection.next();
                this.directionButton.displayString = directionLabel();
                break;
            default:
                break;
        }
    }

    @Override
    public void onGuiClosed() {
        ArtemisConfig.save();
    }

    private static String directionLabel() {
        return I18n.format("artemis.config.direction",
            I18n.format(ArtemisConfig.cooldownDirection.translationKey()));
    }

    private static float clamp(float value) {
        return Math.max(0.02F, Math.min(0.98F, value));
    }

    private static HudElement cooldownElement() {
        return new HudElement() {
            @Override public String name() { return "cooldown"; }
            @Override public float getX() { return ArtemisConfig.cooldownX; }
            @Override public void setX(float value) { ArtemisConfig.cooldownX = value; }
            @Override public float getY() { return ArtemisConfig.cooldownY; }
            @Override public void setY(float value) { ArtemisConfig.cooldownY = value; }
            @Override public int halfWidth() { return 16; }
            @Override public int halfHeight() { return 16; }
            @Override public void renderPreview(int centerX, int centerY) {
                for (int i = 0; i < 3; i++) {
                    int[] pos = CooldownRenderer.slotPosition(centerX, centerY, i);
                    CooldownRenderer.renderPreview(pos[0], pos[1]);
                }
            }
        };
    }

}
