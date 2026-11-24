package com.example.musicbox;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public final class TrackLibrary {
    public static final Path DIR = FabricLoader.getInstance().getGameDir().resolve("musicbox");
    private static final Set<String> EXT = Set.of("ogg", "mp3", "wav", "aiff", "au");
    public static volatile List<Track> vanilla = List.of();
    public static volatile List<Track> custom = List.of();

    public static void refresh() {
        List<Track> v = new ArrayList<>();
        var rm = Minecraft.getInstance().getResourceManager();
        for (String base : new String[]{"sounds/music", "sounds/records"}) {
            for (Identifier id : rm.listResources(base, i -> i.getPath().endsWith(".ogg")).keySet()) {
                String p = id.getPath();
                v.add(new Track(p.substring("sounds/".length(), p.length() - 4), null, id));
            }
        }
        v.sort(Comparator.comparing(Track::name));
        vanilla = v;

        List<Track> c = new ArrayList<>();
        try (Stream<Path> s = Files.walk(DIR, 3)) {
            s.filter(Files::isRegularFile).forEach(f -> {
                String n = f.getFileName().toString();
                int dot = n.lastIndexOf('.');
                if (dot > 0 && EXT.contains(n.substring(dot + 1).toLowerCase(Locale.ROOT)))
                    c.add(new Track(n.substring(0, dot), f, null));
            });
        } catch (Exception ignored) {}
        c.sort(Comparator.comparing(t -> t.name().toLowerCase(Locale.ROOT)));
        custom = c;
    }

    /** 같은 목록의 다음 곡 (끝이면 처음으로). 곡이 1개면 null. */
    public static Track next(Track cur) {
        List<Track> l = cur.path() != null ? custom : vanilla;
        if (l.size() < 2) return null;
        int i = l.indexOf(cur);
        return l.get((i + 1) % l.size());
    }
}
