package com.example.musicbox;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class MusicBoxClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("musicbox_client");
    private static KeyMapping open;

    @Override
    public void onInitializeClient() {
        try {
            Files.createDirectories(TrackLibrary.DIR);
            Path readme = TrackLibrary.DIR.resolve("여기에 노래 넣기.txt");
            if (Files.notExists(readme)) {
                Files.writeString(readme, "이 폴더에 mp3 / ogg / wav 파일을 넣으세요.\n"
                        + "게임에서 M 키(또는 타이틀·일시정지 화면의 '♪ 음악' 버튼) → '내 파일' 탭 → 새로고침.\n"
                        + "하위 폴더(최대 3단계)도 인식합니다. 이 음악은 내 화면에서만 들립니다.\n");
            }
        } catch (Exception ignored) {}
        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("musicbox_client", "main"));
        open = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.musicbox_client.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, cat));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (open.consumeClick()) {
                TrackLibrary.refresh();
                mc.setScreen(new MusicScreen());
            }
            // 내 음악 재생 중에는 바닐라 BGM이 끼어들지 않게 함
            if (AudioPlayer.isActive()) mc.getMusicManager().stopPlaying();
        });
    }
}
