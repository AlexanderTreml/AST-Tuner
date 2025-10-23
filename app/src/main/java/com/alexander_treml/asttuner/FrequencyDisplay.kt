package com.alexander_treml.asttuner

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.alexander_treml.asttuner.ui.theme.silverBrush
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.math.min

// The delay after which a probable note is selected when automatic note detection is active
private const val AUTO_DELAY = 500L

// Display the listener results and auto-detect notes
// Has a debug mode to see the internal processing
// TODO maybe use Listener/AppState singleton instead of passing it here?
@Composable
fun FrequencyDisplay(listener: Listener, state: AppState, modifier: Modifier = Modifier) {
    val active by listener.active.collectAsState()
    val frequency by listener.frequency.collectAsState()
    val targetNote = state.tuning[state.selectedIndex.intValue]
    var debugMode by remember { mutableStateOf(false) }

    // For slide animation
    var rawOffsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = rawOffsetX,
        animationSpec = spring(),
        label = "Display Animation"
    )

    Box(
        modifier = modifier
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    rawOffsetX += delta
                    rawOffsetX = if (debugMode) {
                        rawOffsetX.coerceIn(0f, 200f)
                    } else {
                        rawOffsetX.coerceIn(-200f, 0f)
                    }

                    if (rawOffsetX >= 100f) {
                        debugMode = false
                        rawOffsetX = 0f
                    } else if (rawOffsetX <= -100f) {
                        debugMode = true
                        rawOffsetX = 0f
                    }
                },
                onDragStopped = {
                    rawOffsetX = 0f
                }
            )
    ) {
        AnimatedVisibility(
            visible = !debugMode,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it }
        ) {
            NeedleDisplay(
                active = active,
                frequency = frequency,
                targetNote = targetNote,
                modifier = Modifier.offset(x = animatedOffsetX.dp)
            )
        }
        AnimatedVisibility(
            visible = debugMode,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it }
        ) {
            DebugView(
                listener = listener,
                targetFrequency = targetNote.frequency,
                modifier = Modifier.offset(x = animatedOffsetX.dp)
            )
        }
    }

    NoteDetector(frequency, state)

    val currentView = LocalView.current
    DisposableEffect(Unit) {
        currentView.keepScreenOn = true
        onDispose {
            currentView.keepScreenOn = false
        }
    }
}

@Composable
fun NoteDetector(frequency: Double, state: AppState) {
    var probableNote by remember { mutableStateOf<Note?>(null) }

    if (state.autoMode.value && frequency != 0.0) {
        val detectedNote = state.tuning.minBy { it.getDistance(frequency).absoluteValue }
        probableNote =
            if (detectedNote.getDistance(frequency).absoluteValue < 3) {
                detectedNote
            } else {
                null
            }
    } else {
        probableNote = null
    }

    // This will re-launch if probableNote changes
    LaunchedEffect(probableNote) {
        delay(AUTO_DELAY)
        if (probableNote != null) {
            state.selectedIndex.intValue = state.tuning.indexOf(probableNote)
        }
    }
}

@Composable
fun NeedleDisplay(active: Boolean, frequency: Double, targetNote: Note, modifier: Modifier = Modifier) {
    val lineWidth = 10f
    val needleWidth = 8f
    val padding = 32f

    val tickInset = 0.95f
    val tickLen = 0.05f

    val angle by animateFloatAsState(
        targetNote
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

        // Draw indicator LED
        val ledRadius = radius * 0.08f
        val ledCenter = Offset(x = offset + radius + (radius / 2) * kotlin.math.cos(Math.toRadians(135.0)).toFloat(), y = offset + radius - (radius / 2) * kotlin.math.sin(Math.toRadians(135.0)).toFloat())
        val ledColor = if (active) Color(0xFFFF0000) else Color(0xFF400000)

        drawCircle(
            color = Color.Black,
            radius = ledRadius,
            center = ledCenter
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    ledColor.copy(alpha = if (active) 1f else 0.5f),
                    ledColor.copy(alpha = 0.0f)
                ),
                center = ledCenter,
                radius = ledRadius
            ),
            radius = ledRadius,
            center = ledCenter
        )

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
        try {
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
        } catch (e: Exception) {
            Log.d("DRAW", angle.toString())
        }

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
fun DebugView(listener: Listener, targetFrequency: Double, modifier: Modifier = Modifier) {
    val frequency by listener.frequency.collectAsState()
    val autocorrelationValues by listener.autocorrelationValues.collectAsState()
    val markers by listener.maxima.collectAsState()

    val max = autocorrelationValues.maxOrNull() ?: 1.0

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            color = Color.White,
            text = "%.2f/%.2f".format(frequency, targetFrequency),
            textAlign = TextAlign.Center
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (autocorrelationValues.isEmpty()) {
                return@Canvas
            }

            val maxHeight = size.height / 2
            val spacing = size.width / (autocorrelationValues.size - 1)

            // Draw the graph line
            drawPath(
                path = Path().apply {
                    autocorrelationValues.forEachIndexed { index, point ->
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

@Preview(widthDp = 200, heightDp = 400)
@Composable
fun NeedleDisplayPreview() {
    NeedleDisplay(active = true, frequency = 440.0, targetNote = Note.new(0))
}