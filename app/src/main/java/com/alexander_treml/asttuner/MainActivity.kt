package com.alexander_treml.asttuner

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.alexander_treml.asttuner.ui.theme.AppTheme
import com.alexander_treml.asttuner.ui.theme.Highlight
import com.alexander_treml.asttuner.ui.theme.backgroundBrush
import com.alexander_treml.asttuner.ui.theme.borderBrush
import com.alexander_treml.asttuner.ui.theme.outlineBrush
import com.alexander_treml.asttuner.ui.theme.outlineBrushPressed
import com.alexander_treml.asttuner.ui.theme.shinyBlackBrush
import com.alexander_treml.asttuner.ui.theme.silverBrush
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.math.min

// Constants
private const val AUTO_DELAY = 500L

private val PADDING = 8.dp
private val BORDER_WIDTH = 4.dp
private val OUTLINE_WIDTH = 2.dp
private val BORDER_RADIUS = 8.dp


// Pre-allocate modifiers
val backgroundMod = Modifier
    .background(brush = backgroundBrush)

val borderMod = backgroundMod
    .border(
        BorderStroke(BORDER_WIDTH, borderBrush),
        shape = RoundedCornerShape(BORDER_RADIUS)
    )
    .padding(BORDER_WIDTH)
    .clip(RoundedCornerShape(BORDER_RADIUS))


val panelMod = backgroundMod
    .padding(PADDING)

val dividerMod = Modifier
    .background(brush = borderBrush)

val mainButtonMod = Modifier
    .padding(PADDING)
    .background(shinyBlackBrush, shape = RoundedCornerShape(BORDER_RADIUS))
    .border(BorderStroke(OUTLINE_WIDTH, outlineBrush), shape = RoundedCornerShape(BORDER_RADIUS))
val mainButtonSelectedMod = Modifier
    .padding(PADDING)
    .background(shinyBlackBrush, shape = RoundedCornerShape(BORDER_RADIUS))
    .border(
        BorderStroke(OUTLINE_WIDTH, outlineBrushPressed),
        shape = RoundedCornerShape(BORDER_RADIUS)
    )
    .background(Highlight, shape = RoundedCornerShape(BORDER_RADIUS))

val sideButtonMod = Modifier
    .padding(PADDING)
    .background(silverBrush, shape = RoundedCornerShape(BORDER_RADIUS))
    .border(BorderStroke(OUTLINE_WIDTH, outlineBrush), shape = RoundedCornerShape(BORDER_RADIUS))
val sideButtonSelectedMod = Modifier
    .padding(PADDING)
    .background(silverBrush, shape = RoundedCornerShape(BORDER_RADIUS))
    .border(
        BorderStroke(OUTLINE_WIDTH, outlineBrushPressed),
        shape = RoundedCornerShape(BORDER_RADIUS)
    )


// TODO save/load/edit functionality
// TODO code cleanup and ui optimization
// TODO adjust splash screen color
// TODO find out how to use theme correctly (text color seems to be chosen automatically if not specified)
// TODO write tests or remove tests and test framework
class MainActivity : ComponentActivity() {
    private val listener = Listener(this)
    private var tuning = Tuning()

    // The target note
    private var selectedNote by mutableStateOf(tuning.notes[0])

    // Auto selection
    private var auto by mutableStateOf(false)
    private var probableNote by mutableStateOf<Note?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainPanel()
        }

        // Only start the listener if we have the necessary permissions
        handlePermissions {
            listener.start()
        }
    }

    @Composable
    fun MainPanel() {
        AppTheme {
            Surface(
                color = MaterialTheme.colorScheme.background
            ) {
                Surface(
                    modifier = borderMod,
                ) {
                    Column {
                        Toolbar(
                            modifier = panelMod
                        )
                        TuningPanel(
                            modifier = panelMod.weight(1f)
                        )
                        HorizontalDivider(
                            thickness = BORDER_WIDTH,
                            color = Color.Transparent,
                            modifier = dividerMod
                        )
                        FrequencyDisplay(
                            modifier = backgroundMod
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun Toolbar(modifier: Modifier = Modifier) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val autoMod =
                if (auto)
                    sideButtonSelectedMod.weight(0.25f)
                else
                    sideButtonMod.weight(0.25f)

            IconButton(
                onClick = { auto = !auto },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor =
                    if (auto)
                        Color.Green
                    else
                        Color.Black
                ),
                modifier = autoMod
            ) {
                Text(text = "A", fontSize = 24.sp, fontWeight = FontWeight(600))
            }

            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                modifier = mainButtonMod.weight(1f)
            ) {
                Text(text = "Six String Standard")
            }

            val context = LocalContext.current
            IconButton(
                onClick = {
                    Toast.makeText(context, "Not implemented yet", Toast.LENGTH_SHORT).show()
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Black
                ),
                modifier = sideButtonMod.weight(0.25f)
            ) {
                Icon(Icons.Filled.Create, contentDescription = "Edit tunings")
            }
        }
    }

    // Create a column of buttons, one for each note in the tuning
    @Composable
    fun TuningPanel(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier,
        ) {
            tuning.notes.forEach { note ->
                NoteButtons(note)
            }
        }
    }

    // Display the listener results
    // Has a debug mode to see the internal processing
    @Composable
    fun FrequencyDisplay(modifier: Modifier = Modifier) {
        val frequency by listener.frequency.collectAsState()
        var debugMode by remember { mutableStateOf(false) }

        val detectedNote = tuning.notes.minBy { it.getDistance(frequency).absoluteValue }
        probableNote =
            if (auto && frequency != 0.0 && detectedNote.getDistance(frequency).absoluteValue < 3) {
                detectedNote
            } else {
                null
            }

        // This will re-launch if probableNote changes
        LaunchedEffect(probableNote) {
            probableNote.let { currentProbableNote ->
                delay(AUTO_DELAY)
                if (currentProbableNote != null && currentProbableNote == probableNote) {
                    selectedNote = currentProbableNote
                }
            }
        }

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
                NeedleDisplay(frequency)
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
    fun NoteButtons(note: Note, modifier: Modifier = Modifier) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val weightedSideButtonMod = sideButtonMod.weight(0.25f)
            val weightedMainButtonMod =
                if (selectedNote == note)
                    mainButtonSelectedMod.weight(1f)
                else
                    mainButtonMod.weight(1f)

            IconButton(
                onClick = { note.shift(-1); if (!auto) selectedNote = note },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Black
                ),
                modifier = weightedSideButtonMod
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Decrease note")
            }

            Button(
                onClick = { selectedNote = note; auto = false },
                modifier = weightedMainButtonMod,
                shape = RoundedCornerShape(BORDER_RADIUS),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
            ) {
                Text(note.name)
            }

            IconButton(
                onClick = { note.shift(1); if (!auto) selectedNote = note },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Black
                ),
                modifier = weightedSideButtonMod
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Increase note")
            }
        }
    }

    @Composable
    fun NeedleDisplay(frequency: Double, modifier: Modifier = Modifier) {
        val lineWidth = 10f
        val needleWidth = 8f
        val padding = 32f

        val tickInset = 0.95f
        val tickLen = 0.05f

        val angle by animateFloatAsState(
            selectedNote
                .getDistance(frequency)
                .toFloat()
                .coerceIn(-1f, 1f) * 90f + 90f,
            label = "Needle Animation"
        )

        Canvas(
            modifier = modifier.aspectRatio(2f)
        ) {
            val offset = lineWidth / 2 + padding
            val radius = min(size.height, size.width / 2f) - offset

            // Draw center line
            drawLine(
                brush = silverBrush,
                start = Offset(size.width / 2, size.height),
                end = Offset(
                    x = size.width / 2,
                    y = size.height - radius
                ),
                strokeWidth = lineWidth
            )

            // Draw ticks
            val tickInner = (radius * (tickInset - tickLen))
            val tickOuter = (radius * tickInset)
            var tickAngle = 0.0
            repeat(19) {
                tickAngle += 0.1f * 90f
                drawLine(
                    brush = silverBrush,
                    start = Offset(
                        x = size.width / 2 + tickInner * kotlin.math.cos(Math.toRadians(180.0 - tickAngle))
                            .toFloat(),
                        y = size.height - tickInner * kotlin.math.sin(Math.toRadians(180.0 - tickAngle))
                            .toFloat()
                    ),
                    end = Offset(
                        x = size.width / 2 + tickOuter * kotlin.math.cos(Math.toRadians(180.0 - tickAngle))
                            .toFloat(),
                        y = size.height - tickOuter * kotlin.math.sin(Math.toRadians(180.0 - tickAngle))
                            .toFloat()
                    ),
                    strokeWidth = lineWidth
                )
            }

            // Draw the needle
            drawLine(
                color = Color.Red,
                start = Offset(size.width / 2, size.height),
                end = Offset(
                    x = size.width / 2 + radius * kotlin.math.cos(Math.toRadians(180.0 - angle))
                        .toFloat(),
                    y = size.height - radius * kotlin.math.sin(Math.toRadians(180.0 - angle))
                        .toFloat()
                ),
                strokeWidth = needleWidth,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color.Black,
                radius = lineWidth,
                center = Offset(size.width / 2, size.height)
            )

            // Draw the dial
            drawArc(
                brush = silverBrush,
                startAngle = 175f,
                sweepAngle = 190f,
                useCenter = false,
                topLeft = Offset(offset, offset),
                size = Size(2 * radius, 2 * radius),
                style = Stroke(lineWidth)
            )
        }
    }

    @Composable
    fun DebugView(frequency: Double, modifier: Modifier = Modifier) {
        val bins by listener.bins.collectAsState()
        val markers by listener.maxima.collectAsState()

        val max = bins.maxOrNull() ?: 1.0

        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                color = Color.White,
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
        MainPanel()
    }
}

