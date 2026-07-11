package fr.natsu.artemis.module.limb;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Source de vérité du module Limb : les parties de corps masquées par joueur (bitmask des
 * {@code BodyPart}). Écrit depuis le thread réseau ({@link LimbModule}), lu depuis le rendu
 * ({@code MixinModelPlayer}).
 */
public final class LimbState {

    /** Valeurs {@code BodyPart} (proto). */
    public static final int HEAD = 1;
    public static final int TORSO = 2;
    public static final int LEFT_ARM = 3;
    public static final int RIGHT_ARM = 4;
    public static final int LEFT_LEG = 5;
    public static final int RIGHT_LEG = 6;

    /** Valeurs {@code ArmorPiece} (proto). */
    public static final int HELMET = 1;
    public static final int CHESTPLATE = 2;
    public static final int LEGGINGS = 3;
    public static final int BOOTS = 4;

    private static final Map<UUID, Integer> HIDDEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> HIDDEN_ARMOR = new ConcurrentHashMap<>();

    private LimbState() {
    }

    /** Bit associé à une {@code BodyPart}. */
    public static int bit(int bodyPart) {
        return 1 << bodyPart;
    }

    /** Masque les parties données (ajoute au masque existant). */
    public static void hide(UUID player, int mask) {
        HIDDEN.merge(player, mask, (a, b) -> a | b);
    }

    /** Ré-affiche les parties données ; masque vide -> ré-affiche tout. */
    public static void reset(UUID player, int mask) {
        if (mask == 0) {
            HIDDEN.remove(player);
            return;
        }
        HIDDEN.compute(player, (uuid, current) -> {
            if (current == null) {
                return null;
            }
            int updated = current & ~mask;
            return updated == 0 ? null : updated;
        });
    }

    public static boolean isEmpty() {
        return HIDDEN.isEmpty();
    }

    /** Masque des parties cachées du joueur, ou {@code 0}. */
    public static int maskOrZero(UUID player) {
        Integer mask = HIDDEN.get(player);
        return mask == null ? 0 : mask;
    }

    // ------------------------------------------------------------------
    // Pièces d'armure cachées (masque séparé)
    // ------------------------------------------------------------------

    public static void hideArmor(UUID player, int mask) {
        HIDDEN_ARMOR.merge(player, mask, (a, b) -> a | b);
    }

    public static void resetArmor(UUID player, int mask) {
        if (mask == 0) {
            HIDDEN_ARMOR.remove(player);
            return;
        }
        HIDDEN_ARMOR.compute(player, (uuid, current) -> {
            if (current == null) {
                return null;
            }
            int updated = current & ~mask;
            return updated == 0 ? null : updated;
        });
    }

    /** Masque des pièces d'armure cachées du joueur, ou {@code 0}. */
    public static int armorMaskOrZero(UUID player) {
        Integer mask = HIDDEN_ARMOR.get(player);
        return mask == null ? 0 : mask;
    }
}
