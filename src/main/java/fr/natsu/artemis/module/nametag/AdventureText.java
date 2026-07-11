package fr.natsu.artemis.module.nametag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Parseur minimal de composants texte « adventure » (format JSON de Minecraft 1.7+/1.20+) vers des
 * runs colorés rendables en 1.8.9.
 *
 * <p>Le FontRenderer 1.8.9 ne comprend pas le hex dans les chaînes, mais accepte une couleur RGB
 * pleine en argument de {@code drawString}. On aplati donc l'arbre JSON en une liste de
 * {@link Run} (texte + couleur RGB + codes de style §), héritant le style du parent vers les
 * enfants.</p>
 */
public final class AdventureText {

    private static final Map<String, Integer> NAMED_COLORS = new HashMap<>();

    static {
        NAMED_COLORS.put("black", 0x000000);
        NAMED_COLORS.put("dark_blue", 0x0000AA);
        NAMED_COLORS.put("dark_green", 0x00AA00);
        NAMED_COLORS.put("dark_aqua", 0x00AAAA);
        NAMED_COLORS.put("dark_red", 0xAA0000);
        NAMED_COLORS.put("dark_purple", 0xAA00AA);
        NAMED_COLORS.put("gold", 0xFFAA00);
        NAMED_COLORS.put("gray", 0xAAAAAA);
        NAMED_COLORS.put("dark_gray", 0x555555);
        NAMED_COLORS.put("blue", 0x5555FF);
        NAMED_COLORS.put("green", 0x55FF55);
        NAMED_COLORS.put("aqua", 0x55FFFF);
        NAMED_COLORS.put("red", 0xFF5555);
        NAMED_COLORS.put("light_purple", 0xFF55FF);
        NAMED_COLORS.put("yellow", 0xFFFF55);
        NAMED_COLORS.put("white", 0xFFFFFF);
    }

    private AdventureText() {
    }

    /** Un fragment de texte homogène : les octets à dessiner (styles § inclus) et sa couleur RGB. */
    public static final class Run {
        /** Texte préfixé de ses codes de style (§l, §o, …), prêt pour {@code drawString}. */
        public final String display;
        /** Couleur RGB (0xRRGGBB) ; l'alpha est ajouté par le FontRenderer. */
        public final int rgb;

        Run(String display, int rgb) {
            this.display = display;
            this.rgb = rgb;
        }
    }

    /** Style courant propagé pendant le parcours de l'arbre. */
    private static final class Style {
        int rgb = 0xFFFFFF;
        boolean bold;
        boolean italic;
        boolean underlined;
        boolean strikethrough;
        boolean obfuscated;

        Style copy() {
            Style s = new Style();
            s.rgb = this.rgb;
            s.bold = this.bold;
            s.italic = this.italic;
            s.underlined = this.underlined;
            s.strikethrough = this.strikethrough;
            s.obfuscated = this.obfuscated;
            return s;
        }

        String codes() {
            StringBuilder sb = new StringBuilder();
            if (this.obfuscated) {
                sb.append("§k");
            }
            if (this.bold) {
                sb.append("§l");
            }
            if (this.strikethrough) {
                sb.append("§m");
            }
            if (this.underlined) {
                sb.append("§n");
            }
            if (this.italic) {
                sb.append("§o");
            }
            return sb.toString();
        }
    }

    /**
     * Parse un composant JSON en liste de runs. Renvoie une liste avec le texte brut en secours si le
     * JSON est invalide.
     *
     * @param json le composant JSON adventure
     * @return les runs, jamais {@code null}
     */
    public static List<Run> parse(String json) {
        List<Run> runs = new ArrayList<>();
        try {
            visit(new JsonParser().parse(json), new Style(), runs);
        } catch (Exception e) {
            runs.clear();
            runs.add(new Run(json, 0xFFFFFF));
        }
        return runs.isEmpty() ? Collections.<Run>emptyList() : runs;
    }

    private static void visit(JsonElement element, Style inherited, List<Run> out) {
        if (element.isJsonPrimitive()) {
            String text = element.getAsString();
            if (!text.isEmpty()) {
                out.add(new Run(inherited.codes() + text, inherited.rgb));
            }
            return;
        }

        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                visit(child, inherited, out);
            }
            return;
        }

        if (!element.isJsonObject()) {
            return;
        }

        JsonObject obj = element.getAsJsonObject();
        Style style = inherited.copy();

        if (obj.has("color")) {
            Integer rgb = resolveColor(obj.get("color").getAsString());
            if (rgb != null) {
                style.rgb = rgb;
            }
        }
        if (obj.has("bold")) {
            style.bold = obj.get("bold").getAsBoolean();
        }
        if (obj.has("italic")) {
            style.italic = obj.get("italic").getAsBoolean();
        }
        if (obj.has("underlined")) {
            style.underlined = obj.get("underlined").getAsBoolean();
        }
        if (obj.has("strikethrough")) {
            style.strikethrough = obj.get("strikethrough").getAsBoolean();
        }
        if (obj.has("obfuscated")) {
            style.obfuscated = obj.get("obfuscated").getAsBoolean();
        }

        if (obj.has("text")) {
            String text = obj.get("text").getAsString();
            if (!text.isEmpty()) {
                out.add(new Run(style.codes() + text, style.rgb));
            }
        }

        if (obj.has("extra")) {
            JsonArray extra = obj.get("extra").getAsJsonArray();
            for (JsonElement child : extra) {
                visit(child, style, out);
            }
        }
    }

    /** Résout une couleur adventure (hex {@code #RRGGBB} ou nom) en RGB, ou {@code null} si inconnue. */
    private static Integer resolveColor(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (value.charAt(0) == '#') {
            try {
                return Integer.parseInt(value.substring(1), 16) & 0xFFFFFF;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return NAMED_COLORS.get(value.toLowerCase());
    }
}
