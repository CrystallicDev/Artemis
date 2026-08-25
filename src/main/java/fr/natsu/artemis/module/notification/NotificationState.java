package fr.natsu.artemis.module.notification;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import fr.natsu.artemis.module.nametag.AdventureText.Run;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public final class NotificationState {

    public static final class Notification {

        public final List<List<Run>> titleLines;
        public final List<List<Run>> descriptionLines;
        public final ItemStack itemIcon;
        public final ResourceLocation textureIcon;
        public final long createdAt;
        public final long displayMillis;

        public Notification(List<List<Run>> titleLines, List<List<Run>> descriptionLines,
                            ItemStack itemIcon, ResourceLocation textureIcon,
                            long createdAt, long displayMillis) {
            this.titleLines = titleLines;
            this.descriptionLines = descriptionLines;
            this.itemIcon = itemIcon;
            this.textureIcon = textureIcon;
            this.createdAt = createdAt;
            this.displayMillis = displayMillis;
        }

        public long elapsed(long now) {
            return now - this.createdAt;
        }

        public long remaining(long now) {
            return this.createdAt + this.displayMillis - now;
        }
    }

    private static final List<Notification> ACTIVE = new CopyOnWriteArrayList<>();

    private NotificationState() {
    }

    public static void add(Notification notification) {
        ACTIVE.add(notification);
    }

    public static void remove(Notification notification) {
        ACTIVE.remove(notification);
    }

    public static void resetAll() {
        ACTIVE.clear();
    }

    public static List<Notification> all() {
        return ACTIVE;
    }

    public static boolean isEmpty() {
        return ACTIVE.isEmpty();
    }
}
