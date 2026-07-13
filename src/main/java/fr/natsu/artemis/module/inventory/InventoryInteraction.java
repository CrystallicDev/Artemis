package fr.natsu.artemis.module.inventory;

import java.net.URI;

import fr.natsu.artemis.Artemis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Lunar's Inventory module: the server tags items with NBT ({@code tag.lunar.*}) to drive client-side
 * interaction inside GUIs.
 *
 * <p>Supported keys: {@code unclickable} (bool), {@code openUrl} (string), {@code runCommand}
 * (string), {@code suggestCommand} (string), {@code copyToClipboard} (string),
 * {@code hideItemTooltip} (bool), {@code hideSlotHighlight} (bool).</p>
 */
public final class InventoryInteraction {

    private static final String LUNAR = "lunar";

    private InventoryInteraction() {
    }

    private static NBTTagCompound lunar(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return null;
        }
        NBTTagCompound tag = stack.getTagCompound();
        return tag.hasKey(LUNAR) ? tag.getCompoundTag(LUNAR) : null;
    }

    public static boolean hideTooltip(ItemStack stack) {
        NBTTagCompound lunar = lunar(stack);
        return lunar != null && lunar.getBoolean("hideItemTooltip");
    }

    public static boolean hideHighlight(ItemStack stack) {
        NBTTagCompound lunar = lunar(stack);
        return lunar != null && lunar.getBoolean("hideSlotHighlight");
    }

    /**
     * Runs the click action bound to the item, if any.
     *
     * @return {@code true} if the vanilla click should be cancelled (an action fired or the item is unclickable)
     */
    public static boolean handleClick(ItemStack stack) {
        NBTTagCompound lunar = lunar(stack);
        if (lunar == null) {
            return false;
        }

        Minecraft mc = Minecraft.getMinecraft();
        boolean acted = false;

        if (lunar.hasKey("copyToClipboard")) {
            GuiScreen.setClipboardString(lunar.getString("copyToClipboard"));
            acted = true;
        }
        if (lunar.hasKey("runCommand") && mc.thePlayer != null) {
            mc.thePlayer.sendChatMessage(lunar.getString("runCommand"));
            acted = true;
        }
        if (lunar.hasKey("suggestCommand")) {
            mc.displayGuiScreen(new GuiChat(lunar.getString("suggestCommand")));
            acted = true;
        }
        if (lunar.hasKey("openUrl")) {
            openUrl(mc, lunar.getString("openUrl"));
            acted = true;
        }

        return acted || lunar.getBoolean("unclickable");
    }

    /** Opens a URL, honoring the "chat links" privacy setting. */
    private static void openUrl(Minecraft mc, String url) {
        if (!mc.gameSettings.chatLinks) {
            return;
        }
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return;
            }
            Class<?> desktop = Class.forName("java.awt.Desktop");
            Object instance = desktop.getMethod("getDesktop").invoke(null);
            desktop.getMethod("browse", URI.class).invoke(instance, uri);
        } catch (Throwable t) {
            Artemis.LOGGER.warn("[Inventory] could not open URL: {}", t.toString());
        }
    }
}
