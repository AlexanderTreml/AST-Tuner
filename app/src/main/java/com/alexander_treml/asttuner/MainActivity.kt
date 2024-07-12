package com.alexander_treml.asttuner

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.alexander_treml.asttuner.ui.theme.ASTTunerTheme
import kotlin.math.min

// TODO UI previews
// TODO cleanly handle the transition from double precision in the backend to single precision in the UI
// TODO use the theme (color scheme: https://material-foundation.github.io/material-theme-builder/)
class MainActivity : ComponentActivity() {
    private lateinit var listener: Listener
    private lateinit var tuning: Tuning

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listener = Listener()
        tuning = Tuning()

        // The target note
        // TODO use "by" syntax?
        val selectedNote = mutableStateOf(tuning.notes[0])

        setContent {
            ASTTunerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column {
                        TuningPanel(
                            notes = tuning.notes,
                            selectedNote = selectedNote,
                            modifier = Modifier.weight(1f)
                        )
                        FrequencyDisplay(listener, selectedNote)
                    }
                }
            }
        }

        // Only start the listener routine if we have the necessary permissions
        handlePermissions()
    }

    private fun handlePermissions() {
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    listener.startListening()
                } else {
                    showPermissionDeniedDialog()
                }
            }

        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                listener.startListening()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.RECORD_AUDIO
            ) -> {
                showPermissionDeniedDialog()
            }

            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.RECORD_AUDIO
                )
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Missing Permission")
            .setMessage("The app needs the permission to record audio. Please grant the permission in the settings.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}


// Create a column of buttons, one for each note in the tuning
@Composable
fun TuningPanel(notes: Array<Note>, selectedNote: MutableState<Note>, modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        notes.forEach { note ->
            NoteButtons(note, selectedNote)
        }
    }
}


// A set of buttons for selecting and modifying a note
@Composable
fun NoteButtons(note: Note, selectedNote: MutableState<Note>) {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = { note.shift(-1) },
            modifier = Modifier.weight(0.25f)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Decrease Note")
        }

        Button(
            onClick = { selectedNote.value = note },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedNote.value == note ) Color.Green else Color.Cyan,
            ),
        ) {
            Text(note.name)
        }

        IconButton(
            onClick = { note.shift(1) },
            modifier = Modifier.weight(0.25f)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Increase Note")
        }
    }
}


// Display the listener results
// Has a debug mode to see the internal processing
@Composable
fun FrequencyDisplay(listener: Listener, selectedNote: MutableState<Note>) {
    val frequency by listener.frequency.collectAsState()

    val bins by listener.bins.collectAsState()
    val markers by listener.maxima.collectAsState()

    var debugMode by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .aspectRatio(2f)
            .draggable(
            orientation = Orientation.Horizontal,
            state = rememberDraggableState { delta ->
                if (delta >= 30f) {
                    debugMode = false
                } else if (delta <= -30f) {
                    debugMode = true
                }
            },
        )
    ) {
        if (debugMode) {
            DebugView(frequency, selectedNote.value.frequency, bins, bins.maxOrNull() ?: 1.0, markers)
        } else {
            NeedleDisplay(frequency, selectedNote.value)
        }
    }

    val currentView = LocalView.current
    DisposableEffect(Unit) {
        currentView.keepScreenOn = true
        onDispose {
            currentView.keepScreenOn = false
        }
    }
}

@Composable
fun NeedleDisplay(frequency: Double, targetNote: Note) {
    val angle by animateFloatAsState(
        targetNote
            .getDistance(frequency)
            .toFloat()
            .coerceIn(-1f, 1f) * 90f + 90f,
        label = "Needle Animation"
    )

    Canvas(
        modifier = Modifier
            .aspectRatio(2f)
            .fillMaxSize()
    ) {
        drawDisplay(angle)
    }
}

@Composable
fun DebugView(frequency: Double, targetFrequency: Double, points: List<Double>, max: Double, markers: List<Int>) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "%.2f/%.2f".format(frequency, targetFrequency),
            textAlign = TextAlign.Center
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (points.isEmpty()) {
                return@Canvas
            }

            val maxHeight = size.height / 2
            val spacing = size.width / (points.size - 1)

            // Draw the graph line
            drawPath(
                path = Path().apply {
                    points.forEachIndexed { index, point ->
                        val x = index * spacing
                        val y = maxHeight - (point / max) * maxHeight
                        if (index == 0) {
                            moveTo(x, (y).toFloat())
                        } else {
                            lineTo(x, (y).toFloat())
                        }
                    }
                },
                color = Color.Blue,
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw vertical lines at the marker positions
            markers.forEach { marker ->
                val x = marker * spacing

                drawLine(
                    color = Color.Red,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
            }
        }
    }
}

fun DrawScope.drawDisplay(angle: Float) {
    val strokeWidth = 20f
    val offset = strokeWidth / 2
    val radius = min(size.height, size.width / 2f)

    // Draw center line
    drawLine(
        color = Color.Green,
        start = Offset(size.width / 2, size.height),
        end = Offset(
            x = size.width / 2,
            y = size.height - radius
        ),
        strokeWidth = 4f
    )

    // Draw the needle
    drawLine(
        color = Color.Red,
        start = Offset(size.width / 2, size.height),
        end = Offset(
            x = size.width / 2 + radius * kotlin.math.cos(Math.toRadians((180.0 - angle.toDouble()))).toFloat(),
            y = size.height - radius * kotlin.math.sin(Math.toRadians((180.0 - angle.toDouble()))).toFloat()
        ),
        strokeWidth = 8f
    )

    // Draw the dial
    drawArc(
        color = Color.Gray,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(offset, offset),
        size = Size(size.width - strokeWidth, (size.height - strokeWidth) * 2),
        style = Stroke(strokeWidth)
    )
}