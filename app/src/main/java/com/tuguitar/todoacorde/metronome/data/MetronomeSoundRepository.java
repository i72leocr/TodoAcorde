package com.tuguitar.todoacorde.metronome.data;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import com.tuguitar.todoacorde.R;
import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;

/** Data layer: manages sound loading and playback for the metronome. */
public class MetronomeSoundRepository {
    private SoundPool soundPool;
    private final int tickSoundId;
    private final int tickUpSoundId;
    private boolean soundLoaded = false;

    @Inject
    public MetronomeSoundRepository(@ApplicationContext Context context) {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build();
        tickSoundId = soundPool.load(context, R.raw.tick, 1);
        tickUpSoundId = soundPool.load(context, R.raw.tick_up, 1);
        soundPool.setOnLoadCompleteListener((pool, id, status) -> {
            if (status == 0) {
                soundLoaded = true;
            }
        });
    }

    public void playTick(boolean accent) {
        if (!soundLoaded) return;
        int soundId = accent ? tickUpSoundId : tickSoundId;
        soundPool.play(soundId, 1f, 1f, 0, 0, 1f);
    }

    public void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        soundLoaded = false;
    }
}
