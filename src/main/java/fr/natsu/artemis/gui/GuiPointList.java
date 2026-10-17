package fr.natsu.artemis.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import fr.natsu.artemis.module.marker.MarkerState;
import fr.natsu.artemis.module.waypoint.WaypointState;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

public final class GuiPointList extends GuiScreen {

    private static final int LIST_TOP = 38;
    private static final int LIST_BOTTOM_MARGIN = 34;
    private static final int ROW_HEIGHT = 24;
    private static final int ROW_MARGIN = 20;
    private static final int REMOVE_WIDTH = 54;
    private static final int REMOVE_HEIGHT = 16;

    private final GuiScreen parent;
    private final List<Entry> entries = new ArrayList<Entry>();
    private int scroll;

    private static final class Entry {
        final boolean waypoint;
        final String key;
        final String label;
        final String details;
        final int color;
        final boolean hidden;

        Entry(boolean waypoint, String key, String label, String details, int color, boolean hidden) {
            this.waypoint = waypoint;
            this.key = key;
            this.label = label;
            this.details = details;
            this.color = color;
            this.hidden = hidden;
        }
    }

    public GuiPointList(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height - 26, 200, 20,
            I18n.format("artemis.list.back")));
        this.refresh();
    }

    @Override
    public void updateScreen() {
        this.refresh();
    }

    private void refresh() {
        this.entries.clear();
        EntityPlayer player = this.mc.thePlayer;

        List<Entry> waypoints = new ArrayList<Entry>();
        for (WaypointState.Waypoint waypoint : WaypointState.all()) {
            waypoints.add(new Entry(true, waypoint.name, waypoint.name,
                position(player, waypoint.x + 0.5D, waypoint.y + 0.5D, waypoint.z + 0.5D),
                waypoint.colorArgb, waypoint.hidden));
        }
        Collections.sort(waypoints, byLabel());

        List<Entry> markers = new ArrayList<Entry>();
        for (MarkerState.Marker marker : MarkerState.all()) {
            String label = marker.ownerName == null || marker.ownerName.isEmpty()
                ? marker.id : marker.ownerName;
            markers.add(new Entry(false, marker.id, label,
                position(player, marker.x, marker.y, marker.z), marker.iconArgb, false));
        }
        Collections.sort(markers, byLabel());

        this.entries.addAll(waypoints);
        this.entries.addAll(markers);
        this.scroll = this.clampScroll(this.scroll);
    }

    private static Comparator<Entry> byLabel() {
        return new Comparator<Entry>() {
            @Override
            public int compare(Entry left, Entry right) {
                return left.label.compareToIgnoreCase(right.label);
            }
        };
    }

    private static String position(EntityPlayer player, double x, double y, double z) {
        String coordinates = Math.round(x) + ", " + Math.round(y) + ", " + Math.round(z);
        if (player == null) {
            return coordinates;
        }
        return coordinates + "  -  " + Math.round(player.getDistance(x, y, z)) + "m";
    }

    private int listBottom() {
        return this.height - LIST_BOTTOM_MARGIN;
    }

    private int clampScroll(int value) {
        int viewport = this.listBottom() - LIST_TOP;
        int content = this.entries.size() * ROW_HEIGHT;
        int max = Math.max(0, content - viewport);
        return Math.max(0, Math.min(max, value));
    }

    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            this.scroll = this.clampScroll(this.scroll - Integer.signum(wheel) * ROW_HEIGHT);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, I18n.format("artemis.list.title"),
            this.width / 2, 12, 0xFFFFFF);

        if (this.entries.isEmpty()) {
            this.drawCenteredString(this.fontRendererObj, I18n.format("artemis.list.empty"),
                this.width / 2, this.height / 2 - 4, 0xFFAAAAAA);
            super.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }

        this.drawCenteredString(this.fontRendererObj, I18n.format("artemis.list.hint"),
            this.width / 2, 24, 0xFFAAAAAA);

        int bottom = this.listBottom();
        ScaledResolution resolution = new ScaledResolution(this.mc);
        int factor = resolution.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(0, (this.height - bottom) * factor, this.width * factor,
            (bottom - LIST_TOP) * factor);

        for (int i = 0; i < this.entries.size(); i++) {
            int rowTop = LIST_TOP + i * ROW_HEIGHT - this.scroll;
            if (rowTop > bottom || rowTop + ROW_HEIGHT < LIST_TOP) {
                continue;
            }
            this.drawRow(this.entries.get(i), rowTop, mouseX, mouseY);
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawRow(Entry entry, int rowTop, int mouseX, int mouseY) {
        int left = ROW_MARGIN;
        int right = this.width - ROW_MARGIN;
        drawRect(left, rowTop, right, rowTop + ROW_HEIGHT - 2, 0x40FFFFFF);
        drawRect(left, rowTop, left + 3, rowTop + ROW_HEIGHT - 2, 0xFF000000 | entry.color);

        String label = entry.hidden
            ? entry.label + " " + I18n.format("artemis.list.hidden") : entry.label;
        String kind = I18n.format(entry.waypoint ? "artemis.list.waypoint" : "artemis.list.marker");
        this.fontRendererObj.drawStringWithShadow(label, left + 8, rowTop + 3, 0xFFFFFFFF);
        this.fontRendererObj.drawStringWithShadow(kind + "  " + entry.details,
            left + 8, rowTop + 13, 0xFFAAAAAA);

        int buttonLeft = right - REMOVE_WIDTH - 4;
        int buttonTop = rowTop + (ROW_HEIGHT - 2 - REMOVE_HEIGHT) / 2;
        boolean hovered = this.isOverRemove(mouseX, mouseY, buttonLeft, buttonTop);
        drawRect(buttonLeft, buttonTop, buttonLeft + REMOVE_WIDTH, buttonTop + REMOVE_HEIGHT,
            hovered ? 0xFFB03030 : 0xFF702020);
        String remove = I18n.format("artemis.list.remove");
        this.fontRendererObj.drawStringWithShadow(remove,
            buttonLeft + (REMOVE_WIDTH - this.fontRendererObj.getStringWidth(remove)) / 2,
            buttonTop + 4, 0xFFFFFFFF);
    }

    private boolean isOverRemove(int mouseX, int mouseY, int buttonLeft, int buttonTop) {
        return mouseX >= buttonLeft && mouseX <= buttonLeft + REMOVE_WIDTH
            && mouseY >= buttonTop && mouseY <= buttonTop + REMOVE_HEIGHT
            && mouseY >= LIST_TOP && mouseY <= this.listBottom();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        if (mouseButton == 0) {
            for (int i = 0; i < this.entries.size(); i++) {
                int rowTop = LIST_TOP + i * ROW_HEIGHT - this.scroll;
                int buttonLeft = this.width - ROW_MARGIN - REMOVE_WIDTH - 4;
                int buttonTop = rowTop + (ROW_HEIGHT - 2 - REMOVE_HEIGHT) / 2;
                if (this.isOverRemove(mouseX, mouseY, buttonLeft, buttonTop)) {
                    Entry entry = this.entries.get(i);
                    if (entry.waypoint) {
                        WaypointState.remove(entry.key);
                    } else {
                        MarkerState.remove(entry.key);
                    }
                    this.mc.getSoundHandler().playSound(PositionedSoundRecord.create(
                        new ResourceLocation("gui.button.press"), 1.0F));
                    this.refresh();
                    return;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            this.mc.displayGuiScreen(this.parent);
        }
    }
}
