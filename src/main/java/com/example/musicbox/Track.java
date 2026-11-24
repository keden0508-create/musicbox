package com.example.musicbox;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** path != null: 내 파일, 아니면 마인크래프트 리소스(id). */
public record Track(String name, Path path, Identifier id) {
    public String ext() {
        String s = (path != null ? path.getFileName().toString() : id.getPath()).toLowerCase(Locale.ROOT);
        return s.substring(s.lastIndexOf('.') + 1);
    }
    public InputStream open() throws IOException {
        if (path != null) return Files.newInputStream(path);
        return Minecraft.getInstance().getResourceManager().getResource(id)
                .orElseThrow(() -> new IOException("missing " + id)).open();
    }
}
