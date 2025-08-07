package com.tuguitar.todoacorde.tuner.domain;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

/** Presentation layer ViewModel for TunerFragment. */
@HiltViewModel
public class TunerViewModel extends ViewModel implements TunerManager.TuningUpdateListener {
    private final TunerManager tunerManager;
    private final MutableLiveData<TuningResult> tuningResult = new MutableLiveData<>();

    @Inject
    public TunerViewModel(TunerManager tunerManager) {
        this.tunerManager = tunerManager;
    }

    public LiveData<TuningResult> getTuningResult() {
        return tuningResult;
    }

    public void startTuning(Context context) {
        tunerManager.start(context, this);
    }

    public void stopTuning() {
        tunerManager.stop();
    }

    public void setTargetNote(String note) {
        tunerManager.setTargetNote(note);
    }

    @Override
    public void onTuningUpdate(TuningResult result) {
        // Called from a background thread by TunerManager
        tuningResult.postValue(result);
    }
}
