package com.example.musicbox;

import net.minecraft.Util;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class MusicScreen extends Screen {
    private static boolean customTab;
    private static int pageV, pageC;
    private static final int ROWS = 6, W = 240;
    private StringWidget now;

    private final Screen parent;

    public MusicScreen() { this(null); }

    public MusicScreen(Screen parent) {
        super(Component.literal("Music Box"));
        this.parent = parent;
    }

    @Override
    public void onClose() { this.minecraft.setScreen(parent); }

    @Override
    protected void init() {
        int left = width / 2 - W / 2, y = 6;
        addRenderableWidget(new StringWidget(left, y, W, 12, Component.literal("Music Box"), font).alignCenter());
        y = 20;
        Button b1 = Button.builder(Component.literal("마크 기본 곡"), b -> { customTab = false; rebuildWidgets(); })
                .bounds(left, y, W / 2 - 1, 20).build();
        Button b2 = Button.builder(Component.literal("내 파일"), b -> { customTab = true; rebuildWidgets(); })
                .bounds(left + W / 2 + 1, y, W / 2 - 1, 20).build();
        b1.active = customTab;
        b2.active = !customTab;
        addRenderableWidget(b1);
        addRenderableWidget(b2);

        List<Track> list = customTab ? TrackLibrary.custom : TrackLibrary.vanilla;
        int page = customTab ? pageC : pageV;
        int pages = Math.max(1, (list.size() + ROWS - 1) / ROWS);
        page = Math.min(page, pages - 1);
        if (customTab) pageC = page; else pageV = page;

        y = 44;
        if (list.isEmpty()) {
            addRenderableWidget(new StringWidget(left, y + 20, W, 12,
                    Component.literal(customTab ? "musicbox 폴더에 mp3/ogg/wav를 넣고 새로고침" : "곡을 찾지 못했습니다"), font).alignCenter());
        }
        for (int i = page * ROWS; i < Math.min(list.size(), (page + 1) * ROWS); i++) {
            Track t = list.get(i);
            String name = font.plainSubstrByWidth(t.name(), W - 16);
            String label = (t.equals(AudioPlayer.current) ? "▶ " : "") + name;
            addRenderableWidget(Button.builder(Component.literal(label), b -> { AudioPlayer.play(t); rebuildWidgets(); })
                    .bounds(left, y, W, 20).build());
            y += 21;
        }
        y = 44 + ROWS * 21;
        final int fp = page;
        Button prev = Button.builder(Component.literal("<"), b -> { setPage(fp - 1); rebuildWidgets(); }).bounds(left, y, 40, 20).build();
        Button next = Button.builder(Component.literal(">"), b -> { setPage(fp + 1); rebuildWidgets(); }).bounds(left + W - 40, y, 40, 20).build();
        prev.active = page > 0;
        next.active = page < pages - 1;
        addRenderableWidget(prev);
        addRenderableWidget(next);
        addRenderableWidget(new StringWidget(left + 40, y + 6, W - 80, 12, Component.literal((page + 1) + " / " + pages), font).alignCenter());

        y += 24;
        int bw = W / 3 - 1;
        addRenderableWidget(Button.builder(Component.literal("정지"), b -> { AudioPlayer.stop(); rebuildWidgets(); })
                .bounds(left, y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.literal(AudioPlayer.paused ? "재개" : "일시정지"),
                b -> { AudioPlayer.paused = !AudioPlayer.paused; rebuildWidgets(); }).bounds(left + bw + 1, y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.literal(AudioPlayer.loop ? "반복: 켜짐" : "반복: 꺼짐"),
                b -> { AudioPlayer.loop = !AudioPlayer.loop; rebuildWidgets(); }).bounds(left + 2 * (bw + 1), y, bw, 20).build());

        y += 22;
        addRenderableWidget(Button.builder(Component.literal("폴더 열기"), b -> Util.getPlatform().openPath(TrackLibrary.DIR))
                .bounds(left, y, W / 2 - 1, 20).build());
        addRenderableWidget(Button.builder(Component.literal("새로고침"), b -> { TrackLibrary.refresh(); rebuildWidgets(); })
                .bounds(left + W / 2 + 1, y, W / 2 - 1, 20).build());

        y += 22;
        addRenderableWidget(new VolSlider(left, y, W, 20));
        y += 24;
        now = new StringWidget(left, y, W, 12, Component.empty(), font).alignCenter();
        addRenderableWidget(now);
    }

    private void setPage(int p) { if (customTab) pageC = Math.max(0, p); else pageV = Math.max(0, p); }

    @Override
    public void tick() {
        super.tick();
        if (now != null) {
            Track t = AudioPlayer.current;
            now.setMessage(Component.literal(t == null ? "재생 중인 곡 없음" : "재생 중: " + font.plainSubstrByWidth(t.name(), W - 70)));
        }
    }

    private static class VolSlider extends AbstractSliderButton {
        VolSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty(), AudioPlayer.volume);
            updateMessage();
        }
        @Override protected void updateMessage() { setMessage(Component.literal("볼륨: " + Math.round(value * 100) + "%")); }
        @Override protected void applyValue() { AudioPlayer.volume = (float) value; }
    }
}
