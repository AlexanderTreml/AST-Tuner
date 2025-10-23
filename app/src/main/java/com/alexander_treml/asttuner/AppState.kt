package com.alexander_treml.asttuner

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppState(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("app_state", Context.MODE_PRIVATE)

    // Persistent values
    val autoMode = mutableStateOf(
        savedStateHandle["autoMode"] ?: prefs.getBoolean("autoMode", false)
    )
    val tuningName = mutableStateOf( // Name of selected tuning
        savedStateHandle["tuningName"] ?: prefs.getString("tuningName", "Six String Standard")!!
    )

    val editMode = mutableStateOf(savedStateHandle["editMode"] ?: false)
    val selectMode = mutableStateOf(savedStateHandle["selectMode"] ?: false)
    val tunings = mutableStateMapOf(Pair("Six String Standard", Tunings.standard))
    val tuning = mutableStateListOf(*(Tunings.standard.toTypedArray()))
    val selectedIndex = mutableIntStateOf(savedStateHandle["selectedIndex"] ?: 0)
    val editedName = mutableStateOf(savedStateHandle["editedName"] ?: tuningName.value)

    init {
        // Observe all mutable states and update SavedStateHandle
        observeState(autoMode, "autoMode", persist = true)
        observeState(tuningName, "tuningName", persist = true)
        observeState(editMode, "editMode")
        observeState(selectMode, "selectMode")
        observeState(selectedIndex, "selectedIndex")
        observeState(editedName, "editedName")

        // Load tunings
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                Tunings.loadTunings(application.applicationContext)
            }
            if (loaded.isNotEmpty()) {
                tunings.clear()
                tunings.putAll(loaded)
            }

            if (!tunings.containsKey(tuningName.value))
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

        // Write to disk
        Tunings.saveTunings(application.applicationContext, tunings)
    }

    // Generic observer to save both to SavedStateHandle and optionally SharedPreferences
    private fun <T> observeState(state: MutableState<T>, key: String, persist: Boolean = false) {
        viewModelScope.launch {
            snapshotFlow { state.value }.collect { value ->
                savedStateHandle[key] = value
                if (persist) saveToPrefs(key, value)
            }
        }
    }

    private fun <T> saveToPrefs(key: String, value: T) {
        with(prefs.edit()) {
            when (value) {
                is Boolean -> putBoolean(key, value)
                is String -> putString(key, value)
                is Int -> putInt(key, value)
            }
            apply()
        }
    }
}

fun MutableState<Boolean>.toggle() {
    value = !value
}