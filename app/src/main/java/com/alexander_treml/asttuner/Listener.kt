package com.alexander_treml.asttuner

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.collection.IntList
import androidx.collection.MutableIntList
import androidx.compose.animation.core.animateRectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import kotlin.math.exp
import kotlin.math.log
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

private const val SAMPLE_RATE = 44100

class Listener () : ViewModel() {
    // Output value
    val frequency = MutableStateFlow(0.0)

    // Debugging values
    val bins: MutableStateFlow<List<Double>> = MutableStateFlow(emptyList())
    val maxima: MutableStateFlow<List<Int>> = MutableStateFlow(emptyList())

    private val bufferSize = getBufferSize()

    private val fft: FastFourierTransformer = FastFourierTransformer(DftNormalization.STANDARD)
    private var audioRecord: AudioRecord? = null

    // TODO listening threshold (display if the signal amplitude is too low)
    // TODO code cleanup
    fun startListening() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!initializeAudioRecord() || audioRecord == null) return@launch

            val audioBuffer = ShortArray(bufferSize)
            val fftBuffer = DoubleArray(bufferSize)

            audioRecord?.startRecording()

            try {
                while (true) {
                    audioRecord?.read(audioBuffer, 0, bufferSize, AudioRecord.READ_BLOCKING)

                    // Convert audioBuffer to fftBuffer
                    for (i in audioBuffer.indices) {
                        fftBuffer[i] = audioBuffer[i].toDouble()
                    }

                    val magnitudes = autocorrelation(fftBuffer)

                    bins.value = magnitudes.sliceArray(0..bufferSize / 2).toList()
                    // Find maxima in the first section of the autocorrelation
                    maxima.value = getLocalMaxima(magnitudes.sliceArray(0..bufferSize / 2))

                    val maxIndex = maxima.value.maxByOrNull { magnitudes[it] } ?: continue

                    val a = magnitudes[maxIndex - 1]
                    val b = magnitudes[maxIndex]
                    val c = magnitudes[maxIndex + 1]

                    val peakEstimate = if ((a - 2 * b + c) != 0.0) {
                        (1.0 / 2.0) * (a - c) / (a - 2 * b + c) + maxIndex
                    } else {
                        0.0 + maxIndex
                    }

                    frequency.value = SAMPLE_RATE / peakEstimate
                }
            } finally {
                audioRecord?.stop()
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
            AudioFormat.ENCODING_PCM_16BIT)

        var result = min.takeHighestOneBit()

        if (min.takeLowestOneBit() != result) {
            result *= 2
        }

        Log.d("Config", "Chose buffer size $result")
        return result
    }

    private suspend fun initializeAudioRecord(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
                true
            } catch (e: SecurityException) {
                // TODO handle this more carefully (display error or something)
                false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecord?.release()
    }
}