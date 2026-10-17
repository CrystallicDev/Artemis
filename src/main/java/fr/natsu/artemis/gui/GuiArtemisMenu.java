package fr.natsu.artemis.gui;

import fr.natsu.artemis.module.marker.MarkerState;
import fr.natsu.artemis.module.waypoint.WaypointState;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

public final class GuiArtemisMenu extends GuiScreen {

    private GuiButton pointsButton;
    private int shownCount = -1;

    @Override
    public void initGui() {
        this.buttonList.clear();
        int x = this.width / 2 - 100;
        int y = this.height / 4 + 24;
        this.buttonList.add(new GuiButton(0, x, y, 200, 20, I18n.format("artemis.menu.hud")));
        this.shownCount = count();
        this.pointsButton = new GuiButton(1, x, y + 24, 200, 20,
            I18n.format("artemis.menu.points", this.shownCount));
        this.buttonList.add(this.pointsButton);
        this.buttonList.add(new GuiButton(2, x, this.height - 28, 200, 20,
            I18n.format("artemis.config.done")));
    }

    @Override
    public void updateScreen() {
        int current = count();
        if (current != this.shownCount) {
            this.shownCount = current;
            this.pointsButton.displayString = I18n.format("artemis.menu.points", current);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, I18n.format("artemis.menu.title"),
            this.width / 2, this.height / 4, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                this.mc.displayGuiScreen(new GuiHudConfig(this));
                break;
            case 1:
                this.mc.displayGuiScreen(new GuiPointList(this));
                break;
            case 2:
                this.mc.displayGuiScreen(null);
                break;
            default:
                break;
        }
    }

    private static int count() {
        return WaypointState.all().size() + MarkerState.all().size();
    }
}
