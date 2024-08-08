package com.alexander_treml.asttuner

import android.os.Parcel
import android.os.Parcelable
import kotlin.math.log
import kotlin.math.pow

// Reference tone A2
private const val REFERENCE = 110.0

// Constant for calculating frequency
private val ROOT: Double = 2.0.pow(1.0 / 12.0)

// Limits
private const val MIN_DIST = -24
private const val MAX_DIST = 65

class Note private constructor(val distanceToReference: Int) {
    val name = generateName()
    val frequency = ROOT.pow(distanceToReference.toDouble()) * REFERENCE

    // Return a new note shifted by semitones
    fun shift(semitones: Int): Note = new(distanceToReference + semitones)

    // Returns difference in semitones
    fun getDistance(frequency: Double): Double =
        log(frequency / REFERENCE, ROOT) - distanceToReference

    private fun generateName(): String {
        // There are 12 semi-tones in an octave.
        // Shift by 9 because our reference is A, but octaves begin at C
        val octave = 2 + Math.floorDiv(this.distanceToReference + 9, 12)

        // Offset within octave from A
        val remainder = Math.floorMod(this.distanceToReference, 12)
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

    companion object {
        fun new(distanceToReference: Int): Note =
            Note(distanceToReference.coerceIn(MIN_DIST, MAX_DIST))
    }
}