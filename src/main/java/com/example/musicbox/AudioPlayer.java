package com.example.musicbox;

import javazoom.jl.decoder.*;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/** 클라이언트 전용 오디오 재생. 서버로는 아무것도 전송되지 않음. */
public final class AudioPlayer {
    private static final AtomicInteger GEN = new AtomicInteger();
    public static volatile Track current;
    public static volatile boolean paused, loop;
    public static volatile float volume = 1f;

    public static boolean isActive() { return current != null; }

    public static void play(Track t) {
        int g = GEN.incrementAndGet();
        paused = false;
        current = t;
        Thread th = new Thread(() -> run(t, g), "musicbox-player");
        th.setDaemon(true);
        th.start();
    }

    public static void stop() {
        GEN.incrementAndGet();
        current = null;
        paused = false;
    }

    private static void run(Track first, int g) {
        Track cur = first;
        try {
            while (GEN.get() == g) {
                try { playOnce(cur, g); }
                catch (Exception e) { MusicBoxClient.LOGGER.error("재생 실패: {}", cur.name(), e); break; }
                if (GEN.get() != g) return;
                if (loop) continue;
                Track n = TrackLibrary.next(cur);
                if (n == null) break;
                cur = n;
                current = cur;
            }
        } finally {
            if (GEN.get() == g) current = null;
        }
    }

    private static void playOnce(Track t, int g) throws Exception {
        try (InputStream in = new BufferedInputStream(t.open())) {
            switch (t.ext()) {
                case "ogg" -> ogg(in, g);
                case "mp3" -> mp3(in, g);
                default -> wav(in, g);
            }
        }
    }

    private static float effectiveVolume() {
        var o = Minecraft.getInstance().options;
        return volume
                * o.getSoundSourceOptionInstance(SoundSource.MUSIC).get().floatValue()
                * o.getSoundSourceOptionInstance(SoundSource.MASTER).get().floatValue();
    }

    private static final class Sink implements AutoCloseable {
        final int g;
        final SourceDataLine l;
        Sink(int g, float rate, int ch) throws LineUnavailableException {
            this.g = g;
            AudioFormat f = new AudioFormat(rate, 16, ch, true, false);
            l = AudioSystem.getSourceDataLine(f);
            l.open(f, (int) (rate * 0.15) * ch * 2);
            l.start();
        }
        /** @return false 이면 중단됨 */
        boolean write(byte[] b, int len) {
            float v = effectiveVolume();
            for (int i = 0; i + 1 < len; i += 2) {
                short s = (short) ((b[i] & 0xff) | (b[i + 1] << 8));
                s = (short) Math.max(-32768, Math.min(32767, Math.round(s * v)));
                b[i] = (byte) s;
                b[i + 1] = (byte) (s >> 8);
            }
            int off = 0;
            while (off < len) {
                if (GEN.get() != g) return false;
                if (paused) {
                    if (l.isRunning()) l.stop();
                    try { Thread.sleep(40); } catch (InterruptedException e) { return false; }
                    continue;
                }
                if (!l.isRunning()) l.start();
                int n = l.write(b, off, Math.min(len - off, 2048));
                if (n <= 0) { try { Thread.sleep(5); } catch (InterruptedException e) { return false; } }
                off += Math.max(n, 0);
            }
            return true;
        }
        void finish() { if (GEN.get() == g) l.drain(); }
        @Override public void close() { l.stop(); l.close(); }
    }

    private static void ogg(InputStream in, int g) throws Exception {
        byte[] data = in.readAllBytes();
        ByteBuffer buf = MemoryUtil.memAlloc(data.length);
        long handle = 0;
        ShortBuffer pcm = null;
        STBVorbisInfo info = null;
        try {
            buf.put(data).flip();
            try (MemoryStack st = MemoryStack.stackPush()) {
                IntBuffer err = st.mallocInt(1);
                handle = STBVorbis.stb_vorbis_open_memory(buf, err, null);
                if (handle == 0L) throw new IOException("ogg 열기 실패 (" + err.get(0) + ")");
            }
            info = STBVorbisInfo.malloc();
            STBVorbis.stb_vorbis_get_info(handle, info);
            int ch = info.channels();
            int rate = info.sample_rate();
            pcm = MemoryUtil.memAllocShort(4096 * ch);
            try (Sink s = new Sink(g, rate, ch)) {
                byte[] chunk = new byte[4096 * ch * 2];
                while (true) {
                    pcm.clear();
                    int n = STBVorbis.stb_vorbis_get_samples_short_interleaved(handle, ch, pcm);
                    if (n <= 0) break;
                    int total = n * ch;
                    for (int i = 0; i < total; i++) {
                        short v = pcm.get(i);
                        chunk[2 * i] = (byte) v;
                        chunk[2 * i + 1] = (byte) (v >> 8);
                    }
                    if (!s.write(chunk, total * 2)) return;
                }
                s.finish();
            }
        } finally {
            if (handle != 0L) STBVorbis.stb_vorbis_close(handle);
            if (info != null) info.free();
            if (pcm != null) MemoryUtil.memFree(pcm);
            MemoryUtil.memFree(buf);
        }
    }

    private static void mp3(InputStream in, int g) throws Exception {
        Bitstream bs = new Bitstream(in);
        Decoder dec = new Decoder();
        Sink s = null;
        try {
            Header h;
            while ((h = bs.readFrame()) != null) {
                SampleBuffer out = (SampleBuffer) dec.decodeFrame(h, bs);
                if (s == null) s = new Sink(g, out.getSampleFrequency(), out.getChannelCount());
                short[] sb = out.getBuffer();
                int len = out.getBufferLength();
                byte[] b = new byte[len * 2];
                for (int i = 0; i < len; i++) { b[2 * i] = (byte) sb[i]; b[2 * i + 1] = (byte) (sb[i] >> 8); }
                if (!s.write(b, b.length)) return;
                bs.closeFrame();
            }
            if (s != null) s.finish();
        } finally {
            if (s != null) s.close();
            try { bs.close(); } catch (Exception ignored) {}
        }
    }

    private static void wav(InputStream in, int g) throws Exception {
        try (AudioInputStream a = AudioSystem.getAudioInputStream(in)) {
            AudioFormat f = a.getFormat();
            AudioFormat t = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, f.getSampleRate(), 16,
                    f.getChannels(), f.getChannels() * 2, f.getSampleRate(), false);
            try (AudioInputStream p = AudioSystem.getAudioInputStream(t, a);
                 Sink s = new Sink(g, t.getSampleRate(), t.getChannels())) {
                byte[] b = new byte[8192];
                int n;
                while ((n = p.read(b)) > 0) { if (!s.write(b, n)) return; }
                s.finish();
            }
        }
    }
}
