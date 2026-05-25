package com.guessinggame.util;

import javax.sound.sampled.*;
import java.net.URL;

/**
 * One-shot WAV sound effect player.
 * Each call runs on its own daemon thread so it never blocks the UI.
 */
public final class SoundPlayer {

    private SoundPlayer() {}

    /**
     * Plays a WAV file from the /audio/ resource folder once.
     * Silently does nothing if the file is missing or the system has no audio.
     *
     * @param fileName e.g. "nice.wav" or "fail.wav"
     */
    public static void play(String fileName) {
        Thread t = new Thread(() -> {
            try {
                URL url = SoundPlayer.class.getResource("/audio/" + fileName);
                if (url == null) return;

                AudioInputStream ais = AudioSystem.getAudioInputStream(url);
                Clip clip = AudioSystem.getClip();
                clip.open(ais);

                // Auto-close the clip as soon as playback stops
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });

                clip.start();
            } catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }
}
