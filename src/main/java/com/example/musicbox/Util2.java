package com.example.musicbox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Method;
import java.nio.file.Path;

/** 26.x 버전마다 바뀌는 API(setScreen 위치, 폴더 열기)를 안전하게 처리 */
public final class Util2 {
    public static void openScreen(Screen s) {
        Minecraft mc = Minecraft.getInstance();
        try { Minecraft.class.getMethod("setScreen", Screen.class).invoke(mc, s); return; } catch (ReflectiveOperationException ignored) {}
        try { Minecraft.class.getMethod("setScreenAndShow", Screen.class).invoke(mc, s); return; } catch (ReflectiveOperationException ignored) {}
        try {
            Object gui = Minecraft.class.getField("gui").get(mc);
            Method m = gui.getClass().getMethod("setScreen", Screen.class);
            m.invoke(gui, s);
        } catch (ReflectiveOperationException e) {
            MusicBoxClient.LOGGER.error("화면을 열 수 없습니다", e);
        }
    }

    public static void openFolder(Path p) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            String cmd = os.contains("win") ? "explorer" : os.contains("mac") ? "open" : "xdg-open";
            new ProcessBuilder(cmd, p.toAbsolutePath().toString()).start();
        } catch (Exception e) {
            MusicBoxClient.LOGGER.warn("폴더를 열 수 없습니다", e);
        }
    }
}
