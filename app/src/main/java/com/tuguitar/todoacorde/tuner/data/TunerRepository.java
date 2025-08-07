package com.tuguitar.todoacorde.tuner.data;

import android.content.Context;
import com.tuguitar.todoacorde.tuner.domain.PitchDetectorCallback;
import javax.inject.Inject;

/** Data layer: handles pitch detection hardware/service */
public class TunerRepository {
    private PitchDetector pitchDetector;

    @Inject
    public TunerRepository() { }

    public void startDetection(PitchDetectorCallback callback, Context context) {
        if (pitchDetector == null) {
            pitchDetector = new PitchDetector(callback, context);
        }
        pitchDetector.startDetection();
    }

    public void stopDetection() {
        if (pitchDetector != null) {
            pitchDetector.stopDetection();
        }
    }
}
