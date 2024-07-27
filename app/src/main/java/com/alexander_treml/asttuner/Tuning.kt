package com.alexander_treml.asttuner

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Tuning() {
    var notes by mutableStateOf(listOf<Note>())

    init {
        // Standard guitar tuning
        notes = notes.plus(
            arrayOf(
                Note(-5),   // E2
                Note(0),    // A2
                Note(5),    // D3
                Note(10),   // G3
                Note(14),   // B3
                Note(19)    // E4
            )
        )
    }

    fun transposeUp() {
        for (n in notes) {
            n.shift(1)
        }
    }

    fun transposeDown() {
        for (n in notes) {
            n.shift(-1)
        }
    }

    fun addNote() {
        // TODO higher string limit with scrollable tuning panel?
        if (notes.size < 8) {
            val lastNote = notes.lastOrNull() ?: Note(0)
            val interval = Note(0).getDistance(lastNote.frequency).toInt()
            notes = notes.plus(Note(interval))
        }
    }

    fun removeNote() {
        if (notes.size > 1) {
            notes = notes.dropLast(1)
        }
    }
}

