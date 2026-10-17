package fr.natsu.artemis.module.chat;

import java.lang.reflect.Method;

import net.minecraft.client.gui.FontRenderer;

public final class LunarFontBridge {

    private static Method getHolder;
    private static Method setHolder;
    private static boolean resolved;

    private LunarFontBridge() {
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        for (Method method : FontRenderer.class.getMethods()) {
            if (method.getName().equals("lunar$getColorHolder") && method.getParameterTypes().length == 0) {
                getHolder = method;
            } else if (method.getName().equals("lunar$setColorHolder") && method.getParameterTypes().length == 1) {
                setHolder = method;
            }
        }
    }

    public static Object current(FontRenderer font) {
        resolve();
        if (getHolder == null || setHolder == null) {
            return null;
        }
        try {
            return getHolder.invoke(font);
        } catch (Throwable t) {
            return null;
        }
    }

    public static int alphaOf(Object holder, float x, float y, int fallback) {
        if (holder == null) {
            return fallback;
        }
        try {
            Method apply = holder.getClass().getMethod("apply", Object.class);
            apply.setAccessible(true);
            Object value = apply.invoke(holder, Float.valueOf(x + y));
            return value instanceof Integer ? ((Integer) value) >>> 24 : fallback;
        } catch (Throwable t) {
            return fallback;
        }
    }

    public static void suspend(FontRenderer font) {
        try {
            setHolder.invoke(font, new Object[] {null});
        } catch (Throwable ignored) {
        }
    }

    public static void restore(FontRenderer font, Object holder) {
        if (holder == null) {
            return;
        }
        try {
            setHolder.invoke(font, holder);
        } catch (Throwable ignored) {
        }
    }
}
