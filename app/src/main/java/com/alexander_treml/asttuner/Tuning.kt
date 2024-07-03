package com.alexander_treml.asttuner

class Tuning(numStrings: Int = 6) {
    val notes: Array<Note>

    init {
        require(numStrings > 0) { "Number of strings must be positive" }

        notes = Array(numStrings) { Note() }

        // Standard Tuning
        if (numStrings == 6) {
            notes[0].shift(-5)  // E2
            notes[1].shift(0)   // A2
            notes[2].shift(5)   // D3
            notes[3].shift(10)  // G3
            notes[4].shift(14)  // B3
            notes[5].shift(19)  // E4
        }
    }
}

