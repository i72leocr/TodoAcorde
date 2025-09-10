#include <jni.h>
#include "kiss_fft.h"
#include <cmath>
#include <vector>

extern "C"
JNIEXPORT void JNICALL
Java_com_todoacorde_todoacorde_FFT_performFFT(JNIEnv *env, jclass clazz, jfloatArray signal, jfloatArray real, jfloatArray imag) {
    int n = env->GetArrayLength(signal);
    jfloat* signalArray = env->GetFloatArrayElements(signal, nullptr);
    jfloat* realArray = env->GetFloatArrayElements(real, nullptr);
    jfloat* imagArray = env->GetFloatArrayElements(imag, nullptr);

    kiss_fft_cfg cfg = kiss_fft_alloc(n, 0, nullptr, nullptr);

    std::vector<kiss_fft_cpx> in(n);
    std::vector<kiss_fft_cpx> out(n);

    for (int i = 0; i < n; i++) {
        in[i].r = signalArray[i];
        in[i].i = 0.0;
    }

    kiss_fft(cfg, in.data(), out.data());

    for (int i = 0; i < n; i++) {
        realArray[i] = out[i].r;
        imagArray[i] = out[i].i;
    }

    kiss_fft_free(cfg);
    env->ReleaseFloatArrayElements(signal, signalArray, 0);
    env->ReleaseFloatArrayElements(real, realArray, 0);
    env->ReleaseFloatArrayElements(imag, imagArray, 0);
}