package com.example.einkarcade.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

fun playSound() {
    val sampleRate = 44100
    val durationMs = 180

    val numSamples = sampleRate * durationMs / 1000
    val samples = ShortArray(numSamples)

    // "Pebble in water" = short, soft transient + quickly damped low tone + a tiny bit of filtered noise.
    // We approximate this with:
    //  - a fast attack, medium-fast decay envelope
    //  - a pitch drop (like a plop)
    //  - a lightly noisy component to mimic splash texture
    val startFreqHz = 520.0
    val endFreqHz = 140.0
    val noiseLevel = 0.10
    val volume = 0.22

    // Single-note "coin" layer (brief, clean, no trill)
    val coinFreqHz = 1600.0
    val coinDurationMs = 145
    val coinVolume = 0.30

    // Bright coin ding — triangle wave with hard attack (distinct timbre)
    val brightCoinFreqHz = 3200.0
    val brightCoinDurationMs = 120   // audible, not transient-only
    val brightCoinVolume = 0.35

    // Low plop layer (water drop body)
    val plopFreqHz = 220.0
    val plopDurationMs = 260
    val plopVolume = 0.88

    // Simple one-pole low-pass for the noise (keeps it from sounding like static)
    var lp = 0.0
    val lpAlpha = 0.08

    // Phase accumulator for the sine (smooth, "water" rather than buzzy)
    var phase = 0.0
    val twoPi = 2.0 * Math.PI

    for (i in 0 until numSamples) {
        val t = i / sampleRate.toDouble()
        val p = i.toDouble() / (numSamples - 1).toDouble()

        // Fast attack + damped decay (pebble "plop" then vanish)
        val attack = 0.008
        val decay = Math.exp(-7.5 * p)
        val envelope = if (t < attack) (t / attack) else decay

        // Pitch glides downward quickly, then settles
        val freq = startFreqHz + (endFreqHz - startFreqHz) * (1.0 - Math.exp(-10.0 * p))

        phase += twoPi * freq / sampleRate
        if (phase > twoPi) phase -= twoPi

        val tone = Math.sin(phase)

        // Splash texture: low-passed noise with an even quicker decay than the tone
        val rawNoise = (Math.random() * 2.0 - 1.0)
        lp += lpAlpha * (rawNoise - lp)
        val splashEnv = Math.exp(-14.0 * p)
        val splash = lp * splashEnv

        // Plop: low sine with fast attack and early fade
        val plopSamples = sampleRate * plopDurationMs / 1000
        val plopTone = if (i < plopSamples) {
            val plopPhase = twoPi * plopFreqHz * t
            val norm = i.toDouble() / plopSamples

            // Quick attack, medium-fast decay so it fades under the coin
            val env = Math.exp(-5.5 * norm)
            Math.sin(plopPhase) * env * plopVolume
        } else {
            0.0
        }

        // Coin tone: short, clean sine blip with fast decay
        val coinSamples = sampleRate * coinDurationMs / 1000
        val coinEnv = if (i < coinSamples) {
            Math.exp(-12.0 * (i.toDouble() / coinSamples))
        } else {
            0.0
        }
        val coinTone = Math.sin(twoPi * coinFreqHz * t) * coinEnv * coinVolume

        // Bright coin ding: triangle wave with hard transient
        val brightCoinSamples = sampleRate * brightCoinDurationMs / 1000
        val brightCoinTone = if (i < brightCoinSamples) {
            val localPhase = (t * brightCoinFreqHz) % 1.0
            val triangle =
                if (localPhase < 0.5)
                    4.0 * localPhase - 1.0
                else
                    3.0 - 4.0 * localPhase

            // Very fast attack, fast decay (metallic "ping")
            val sustainPoint = 0.35
            val norm = i.toDouble() / brightCoinSamples
            val env = if (norm < sustainPoint) {
                1.0
            } else {
                Math.exp(-6.0 * ((norm - sustainPoint) / (1.0 - sustainPoint)))
            }
            triangle * env * brightCoinVolume
        } else {
            0.0
        }

        val mixed =
            ((tone * 0.95 + splash * noiseLevel) * envelope * volume) +
            plopTone +
            coinTone +
            brightCoinTone

        samples[i] = (mixed * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }

    val track = AudioTrack(
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build(),
        AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build(),
        samples.size * 2,
        AudioTrack.MODE_STATIC,
        AudioManager.AUDIO_SESSION_ID_GENERATE
    )

    track.write(samples, 0, samples.size)
    track.play()

    // Avoid leaking AudioTrack instances when called repeatedly.
    Thread {
        try {
            Thread.sleep((durationMs + 50).toLong())
        } catch (_: InterruptedException) {
        }
        try {
            track.stop()
        } catch (_: IllegalStateException) {
        }
        track.release()
    }.start()
}
