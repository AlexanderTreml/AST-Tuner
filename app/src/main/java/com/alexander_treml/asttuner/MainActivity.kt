package com.alexander_treml.asttuner

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.alexander_treml.asttuner.ui.theme.AppTheme
import kotlin.math.min

// TODO fix swipe bug that causes button issues
// TODO cleanly handle the transition from double precision in the backend to single precision in the UI
// TODO better color theme (also check color scheme for accessibility)
// TODO reuse modifiers
class MainActivity : ComponentActivity() {
    private val listener = Listener(this)
    private var tuning = Tuning()

    // The target note
    private var selectedNote by mutableStateOf(tuning.notes[0])

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        TuningPanel(
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp))
                        )
                        FrequencyDisplay(modifier = Modifier
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.onBackground,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        }

        // Only start the listener if we have the necessary permissions
        handlePermissions {
            listener.start()
        }
    }

    // TODO allow passing modifiers like here whenever it makes sense
    // Create a column of buttons, one for each note in the tuning
    @Composable
    fun TuningPanel(modifier: Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            tuning.notes.forEach { note ->
                NoteButtons(note, Modifier.padding(8.dp))
            }
        }
    }

    // Display the listener results
    // Has a debug mode to see the internal processing
    @Composable
    fun FrequencyDisplay(modifier: Modifier) {
        val frequency by listener.frequency.collectAsState()
        var debugMode by remember { mutableStateOf(false) }

        Box(
            modifier = modifier
                .aspectRatio(2f)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        if (delta >= 30f) {
                            debugMode = false
                        } else if (delta <= -30f) {
                            debugMode = true
                        }
                    }
                )
        ) {
            if (debugMode) {
                DebugView(frequency)
            } else {
                NeedleDisplay(frequency, arcColor = MaterialTheme.colorScheme.primary)
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

    // A set of buttons for selecting and modifying a note
    @Composable
    fun NoteButtons(note: Note, modifier: Modifier) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = { note.shift(-1); selectedNote = note },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.weight(0.25f)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Decrease Note")
            }

            Button(
                onClick = { selectedNote = note },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        if (selectedNote == note )
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary,
                    contentColor =
                        if (selectedNote == note )
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSecondary
                ),
            ) {
                Text(note.name)
            }

            IconButton(
                onClick = { note.shift(1); selectedNote = note },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.weight(0.25f)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Increase Note")
            }
        }
    }

    @Composable
    fun NeedleDisplay(frequency: Double, arcColor: Color) {
        val angle by animateFloatAsState(
            selectedNote
                .getDistance(frequency)
                .toFloat()
                .coerceIn(-1f, 1f) * 90f + 90f,
            label = "Needle Animation"
        )

        Canvas(
            modifier = Modifier.aspectRatio(2f)
        ) {
            val strokeWidth = 10f
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

            // Draw ticks
            var tickAngle = 0.0
            val tickInset = 0.9f
            // TODO use repeat instead of for with constant values
            repeat(19) {
                tickAngle += 0.1f * 90f
                drawLine(
                    color = arcColor,
                    start = Offset(
                        x = size.width / 2 + (radius * tickInset)  * kotlin.math.cos(Math.toRadians(180.0 - tickAngle)).toFloat(),
                        y = size.height - (radius * tickInset) * kotlin.math.sin(Math.toRadians(180.0 - tickAngle)).toFloat()
                    ),
                    end = Offset(
                        x = size.width / 2 + radius * kotlin.math.cos(Math.toRadians(180.0 - tickAngle)).toFloat(),
                        y = size.height - radius * kotlin.math.sin(Math.toRadians(180.0 - tickAngle)).toFloat()
                    ),
                    strokeWidth = 8f
                )
            }

            // Draw the needle
            drawLine(
                color = Color.Red,
                start = Offset(size.width / 2, size.height),
                end = Offset(
                    x = size.width / 2 + radius * kotlin.math.cos(Math.toRadians(180.0 - angle)).toFloat(),
                    y = size.height - radius * kotlin.math.sin(Math.toRadians(180.0 - angle)).toFloat()
                ),
                strokeWidth = 8f
            )

            // Draw the dial
            drawArc(
                color = arcColor,
                startAngle = 175f,
                sweepAngle = 190f,
                useCenter = false,
                topLeft = Offset(offset, offset),
                size = Size(size.width - strokeWidth, (size.height - strokeWidth) * 2),
                style = Stroke(strokeWidth)
            )
        }
    }

    @Composable
    fun DebugView(frequency: Double) {
        val bins by listener.bins.collectAsState()
        val markers by listener.maxima.collectAsState()

        val max = bins.maxOrNull() ?: 1.0

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "%.2f/%.2f".format(frequency, selectedNote.frequency),
                textAlign = TextAlign.Center
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (bins.isEmpty()) {
                    return@Canvas
                }

                val maxHeight = size.height / 2
                val spacing = size.width / (bins.size - 1)

                // Draw the graph line
                drawPath(
                    path = Path().apply {
                        bins.forEachIndexed { index, point ->
                            val x = index * spacing
                            val y = maxHeight - (point / max) * maxHeight
                            if (index == 0) {
                                moveTo(x, (y).toFloat())
                            } else {
                                lineTo(x, (y).toFloat())
                            }
                        }
                    },
                    color = Color.Red,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Draw vertical lines at the marker positions
                markers.forEach { marker ->
                    val x = marker * spacing

                    drawLine(
                        color = Color.Green,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                }
            }
        }
    }

    private fun handlePermissions(onPermissionGranted: () -> Unit) {
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    onPermissionGranted()
                } else {
                    showPermissionDeniedDialog()
                }
            }

        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                onPermissionGranted()
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

    @Preview(showBackground = true)
    @Composable
    fun AppPreview() {
        AppTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column {
                    TuningPanel(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.onBackground,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp))
                    )
                    FrequencyDisplay(modifier = Modifier
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.onBackground,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

