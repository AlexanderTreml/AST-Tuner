package com.alexander_treml.asttuner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.log
import kotlin.math.pow

// Reference tone A2
private const val REFERENCE = 110.0

// Constant for calculating frequency
private val ROOT: Double = 2.0.pow(1.0 / 12.0)

// Limits
private const val MIN_DIST = -24
private const val MAX_DIST = 65

class Note(private var distanceToReference: Int) {
    constructor() : this(0)

    var name by mutableStateOf(getName(distanceToReference))
        private set
    var frequency by mutableDoubleStateOf(ROOT.pow(distanceToReference.toDouble()) * REFERENCE)
        private set

    init {
        distanceToReference = distanceToReference.coerceIn(MIN_DIST, MAX_DIST)
    }

    fun shift(semitones: Int) {
        distanceToReference += semitones
        distanceToReference = distanceToReference.coerceIn(MIN_DIST, MAX_DIST)
        name = getName(distanceToReference)
        frequency = ROOT.pow(distanceToReference.toDouble()) * REFERENCE
    }

    // Returns difference in semitones
    fun getDistance(frequency: Double): Double {
        return log(frequency / REFERENCE, ROOT) - distanceToReference
    }

    private fun getName(distanceToReference: Int): String {
        // There are 12 semi-tones in an octave. Shift by 9 because out reference is A, but octaves begin at C
        val octave = 2 + Math.floorDiv(distanceToReference + 9, 12)

        // Offset in octave from A. Wraps around at offset 3 = C.
        val remainder = Math.floorMod(distanceToReference, 12)
        return when (remainder) {
            0 -> "A$octave"
            1 -> "A$octave#"
            2 -> "B$octave"
            3 -> "C$octave"
            4 -> "C$octave#"
            5 -> "D$octave"
            6 -> "D$octave#"
            7 -> "E$octave"
            8 -> "F$octave"
            9 -> "F$octave#"
            10 -> "G$octave"
            11 -> "G$octave#"
            else -> "THIS SHOULD NOT HAPPEN"
        }
    }

    override fun toString(): String = name
}