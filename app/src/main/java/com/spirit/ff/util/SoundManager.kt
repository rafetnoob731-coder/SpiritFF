package com.spirit.ff.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

class SoundManager(context: Context) {
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 95)
    private val handler = Handler(Looper.getMainLooper())

    fun playActivate() {
        tone.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 80)
        handler.postDelayed({ tone.startTone(ToneGenerator.TONE_PROP_BEEP, 100) }, 110)
    }

    fun playDeactivate() {
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
    }

    fun release() = tone.release()
}
