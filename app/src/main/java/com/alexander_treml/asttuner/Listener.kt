package com.alexander_treml.asttuner

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

private const val SAMPLE_RATE = 44100
private const val ACTIVATION_THRESHOLD = 1000.0

// How much a frequency bin can be below the maximum value to be selected. At 1 the maximum bin is always selected.
// Lower values: better detection when multiple strings are ringing out (the algorithm will detect the most prominent frequency, instead of a common denominator)
// Higher values: better distortion tolerance (the algorithm will detect the fundamental instead of harmonics)
private const val DISTORTION_TOLERANCE = 0.9

class Listener(owner: LifecycleOwner) : DefaultLifecycleObserver {
    init {
        owner.lifecycle.addObserver(this)
    }

    // Output values
    val frequency = MutableStateFlow(0.0)
    val active = MutableStateFlow(false)

    // Debugging values
    val autocorrelationValues: MutableStateFlow<List<Double>> = MutableStateFlow(emptyList())
    val maxima: MutableStateFlow<List<Int>> = MutableStateFlow(emptyList())

    private val bufferSize = getBufferSize()
    private val audioBuffer = ShortArray(bufferSize)
    private val fftBuffer = DoubleArray(bufferSize)

    private val listenerScope = CoroutineScope(Dispatchers.IO)
    private val fft: FastFourierTransformer = FastFourierTransformer(DftNormalization.STANDARD)
    private var audioRecord: AudioRecord? = null

    // TODO code cleanup and optimization
    // TODO pause when in edit mode or selecting a tuning
    fun start() {
        listenerScope.launch(Dispatchers.IO) {
            // TODO inform the user that something went wrong
            if (!initializeAudioRecord()) return@launch

            while (true) {
                audioRecord?.read(audioBuffer, 0, bufferSize, AudioRecord.READ_BLOCKING)

                // Convert PCM16 to Double
                for (i in audioBuffer.indices) {
                    fftBuffer[i] = audioBuffer[i].toDouble()
                }

                val magnitudes = autocorrelation(fftBuffer).sliceArray(0..bufferSize / 2)

                autocorrelationValues.value = magnitudes.toList()
                // Find maxima in the first section of the autocorrelation
                maxima.value = getLocalMaxima(magnitudes)

                val maxIndex = maxima.value.maxByOrNull { magnitudes[it] } ?: -1

                active.value = maxIndex != -1 && magnitudes[maxIndex] >= ACTIVATION_THRESHOLD
                if (!active.value) {
                    frequency.value = 0.0
                    continue
                }

                val maxVal = magnitudes[maxIndex]
                val estIdx = maxima.value.firstOrNull { magnitudes[it] >= DISTORTION_TOLERANCE * maxVal } ?: -1
                if (estIdx == -1) {
                    continue
                }

                val a = magnitudes[estIdx - 1]
                val b = magnitudes[estIdx]
                val c = magnitudes[estIdx + 1]

                val peakEstimate = if ((a - 2 * b + c) != 0.0) {
                    (1.0 / 2.0) * (a - c) / (a - 2 * b + c) + estIdx
                } else {
                    0.0 + estIdx
                }

                frequency.value = SAMPLE_RATE / peakEstimate
            }
        }
    }

    // TODO maybe only return one half (in place?)
    private fun autocorrelation(array: DoubleArray): DoubleArray {
        // Pad with zeros
        val padded = array.copyOf(array.size * 2)

        // Transform forward
        val fftResult = fft.transform(padded, TransformType.FORWARD)

        // Remove DC component and take the square absolute value
        val magnitudes = DoubleArray(array.size * 2)
        for (i in 1..<fftResult.size) {
            magnitudes[i] = fftResult[i].abs().pow(2)
        }

        // Transform inverse
        val ifftResult = fft.transform(magnitudes, TransformType.INVERSE)

        // Get real valued result and normalize
        val result = DoubleArray(array.size * 2)
        result[0] = ifftResult[0].real * 2.0 / bufferSize
        for (i in 1 until ifftResult.size) {
            result[i] = ifftResult[i].real * 2.0 / bufferSize
        }

        return result
    }

    private fun getLocalMaxima(array: DoubleArray): List<Int> {
        val result = ArrayList<Int>()

        for (i in 1 until array.size - 1) {
            if (array[i - 1] <= array[i] && array[i] > array[i + 1]) {
                result.add(i)
            }
        }

        return result
    }

    private fun getBufferSize(): Int {
        val min = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        var result = min.takeHighestOneBit()

        if (min.takeLowestOneBit() != result) {
            result *= 2
        }

        Log.d("Listener", "Chose buffer size $result")
        return result
    }

    private fun initializeAudioRecord(): Boolean {
        return try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            audioRecord!!.startRecording()
            true
        } catch (_: SecurityException) {
            false
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        Log.d("Listener", "Paused...")
        audioRecord?.stop()
    }

    override fun onResume(owner: LifecycleOwner) {
        Log.d("Listener", "Resumed...")
        audioRecord?.startRecording()
    }
}