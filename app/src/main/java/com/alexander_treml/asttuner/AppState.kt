package com.alexander_treml.asttuner

import android.app.Application
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// TODO savedStateHandle is recommended with ViewModel, but does not seem to make a difference when reopening the app from the background.
class AppState(
    private val application: Application,
) : AndroidViewModel(application) {
    val autoMode = mutableStateOf(false)
    val editMode = mutableStateOf(false)
    val selectMode = mutableStateOf(false)

    val tunings = mutableStateMapOf(Pair("Six String Standard", Tunings.standard))

    // Current Tuning
    val tuningName = mutableStateOf("Six String Standard")
    val tuning = mutableStateListOf(*(Tunings.standard.toTypedArray()))

    // Index in the tuning of the currently selected note
    val selectedIndex = mutableIntStateOf(0)

    val editedName = mutableStateOf(tuningName.value)

    init {
        viewModelScope.launch {
            val loaded =
            withContext(Dispatchers.IO) {
                Tunings.loadTunings(application.applicationContext)
            }
            if (loaded.isNotEmpty()) {
                tunings.clear()
                tunings.putAll(loaded)
            }
            tuningName.value = tunings.keys.sorted()[0]
            editedName.value = tuningName.value
            tuning.clear()
            tuning.addAll(tunings[tuningName.value]!!)
        }
    }

    fun addNote() {
        // In case you want to have a whole piano tuning!
        if (tuning.size < 88) {
            val lastNote = tuning.last()
            val newNote = Note.new(lastNote.distanceToReference + 5)
            tuning.add(newNote)
        }
    }

    fun removeNote() {
        if (tuning.size > 1) {
            tuning.removeLast()
        }
        if (tuning.size <= selectedIndex.intValue) {
            selectedIndex.intValue = tuning.size - 1
        }
    }

    fun transpose(semitones: Int) {
        tuning.replaceAll { note -> note.shift(semitones) }
    }

    fun shiftNote(index: Int, semitones: Int) {
        tuning[index] = tuning[index].shift(semitones)
    }

    fun selectTuning(name: String) {
        tuningName.value = name
        editedName.value = name
        tuning.clear()
        tuning.addAll(tunings[name]!!)
        selectedIndex.intValue = 0
        selectMode.value = false
    }

    fun addTuning() {
        var name = "New Tuning"
        var n = 2
        while (tunings.contains(name)) {
            name = "New Tuning ($n)"
            n++
        }

        tuningName.value = name
        editedName.value = name
        tuning.clear()
        tuning.addAll(Tunings.standard)
        selectedIndex.intValue = 0

        editMode.value = true
        selectMode.value = false
    }

    fun saveTuning() {
        if (tuningName.value != editedName.value) {
            tunings.remove(tuningName.value)
            tuningName.value = editedName.value
        }

        // This copies the list
        tunings[tuningName.value] = tuning.toList()

        editMode.value = false

        // TODO this is probably an inefficient way to do this
        // Write to disk
        Tunings.saveTunings(application.applicationContext, tunings)
    }

    fun discardEdits() {
        if (tunings.contains(tuningName.value)) {
            // This means an existing tuning was edited. Since tuningName contains the name of the
            // tuning before it was edited, this effectively restores the tuning.
            selectTuning(tuningName.value)
        } else {
            // The tuning was a newly created one. Return to the tuning selection without saving
            selectMode.value = true
        }

        editMode.value = false
    }

    fun deleteTuning(name: String) {
        tunings.remove(name)

        // TODO this is probably an inefficient way to do this
        // Write to disk
        Tunings.saveTunings(application.applicationContext, tunings)
    }
}

fun MutableState<Boolean>.toggle() {
    value = !value
}