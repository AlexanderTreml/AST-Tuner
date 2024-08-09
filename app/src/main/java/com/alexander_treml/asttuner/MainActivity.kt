package com.alexander_treml.asttuner

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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

// Constants
private val PADDING = 8.dp
private val BORDER_WIDTH = 4.dp
private val OUTLINE_WIDTH = 2.dp
private val BORDER_RADIUS = 8.dp

// TODO this is ugly
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

val lowerPanelMod = backgroundMod
    .aspectRatio(2f)
    .clip(RectangleShape)

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


// TODO component previews
// TODO ui optimization
// TODO adjust splash screen color
// TODO find out how to use theme correctly (text color seems to be chosen automatically if not specified)
// TODO write tests or remove tests and test framework
// TODO Deal with Logcat warnings
// TODO haptic feedback
class MainActivity : ComponentActivity() {
    private val listener = Listener(this)
    private val state: AppState by viewModels()

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
                        // TODO fix shader discontinuity between Toolbar and TuningPanel
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
                        LowerPanel(
                            modifier = lowerPanelMod
                        )
                    }
                }
                if (state.selectMode.value) {
                    TuningSelection(
                        modifier = borderMod
                            .fillMaxSize()
                            .background(Color(0xAA000000))
                    )
                }
            }
        }
    }

    // Toolbar with auto mode, tuning selection, and edit mode
    @Composable
    fun Toolbar(modifier: Modifier = Modifier) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val autoMod =
                if (state.autoMode.value)
                    sideButtonSelectedMod.weight(0.25f)
                else
                    sideButtonMod.weight(0.25f)

            val editMod =
                if (state.editMode.value)
                    sideButtonSelectedMod.weight(0.25f)
                else
                    sideButtonMod.weight(0.25f)

            IconButton(
                onClick = { state.autoMode.toggle() },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor =
                    if (state.autoMode.value)
                        Color.Green
                    else
                        Color.Black
                ),
                modifier = autoMod
            ) {
                Text(text = "A", fontSize = 24.sp, fontWeight = FontWeight(600))
            }

            Button(
                onClick = {
                    // TODO check if it is possible to click UI elements behind the selection list
                    if (!state.editMode.value) state.selectMode.value = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                modifier = mainButtonMod.weight(1f)
            ) {
                if (state.editMode.value) {
                    // TODO text field can not be unfocused
                    // TODO text field is so big it shifts the layout
                    TextField(
                        value = state.editedName.value,
                        onValueChange = { name -> state.editedName.value = name },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                        )
                    )
                } else {
                    Text(text = state.tuningName.value)
                }
            }

            IconButton(
                onClick = {
                    if (!state.editMode.value) {
                        state.editMode.value = true
                    } else {
                        showSaveTuningDialog()
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor =
                    if (state.editMode.value)
                        Color.Green
                    else
                        Color.Black
                ),
                modifier = editMod
            ) {
                Icon(Icons.Filled.Build, contentDescription = "Edit tunings")
            }
        }
    }

    // Create buttons corresponding to the tuning
    @Composable
    fun TuningPanel(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
        ) {
            state.tuning.forEachIndexed { i, note ->
                NoteButtons(i, note)
            }
        }
    }

    // A set of buttons for selecting and modifying a note
    @Composable
    fun NoteButtons(index: Int, note: Note, modifier: Modifier = Modifier) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val weightedSideButtonMod = sideButtonMod.weight(0.25f)
            val weightedMainButtonMod =
                if (state.selectedIndex.intValue == index)
                    mainButtonSelectedMod.weight(1f)
                else
                    mainButtonMod.weight(1f)

            IconButton(
                onClick = {
                    state.shiftNote(index, -1)
                    if (!state.autoMode.value) state.selectedIndex.intValue = index
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Black
                ),
                modifier = weightedSideButtonMod
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Decrease note")
            }

            Button(
                onClick = { state.selectedIndex.intValue = index; state.autoMode.value = false },
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
                onClick = {
                    state.shiftNote(index, 1)
                    if (!state.autoMode.value) state.selectedIndex.intValue = index
                },
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

    // Displays the FrequencyDisplay normally, or the EditPanel in edit mode
    @Composable
    fun LowerPanel(modifier: Modifier = Modifier) {
        Box(modifier = modifier) {
            AnimatedVisibility(
                visible = !state.editMode.value,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                FrequencyDisplay(listener, state)
            }
            AnimatedVisibility(
                visible = state.editMode.value,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it }
            ) {
                EditPanel()
            }
        }
    }

    // Buttons for editing tunings
    @Composable
    fun EditPanel(modifier: Modifier = Modifier) {
        Row(modifier = modifier) {
            val weightedSideButtonMod = sideButtonMod
                .weight(0.5f)
                .fillMaxHeight()
            IconButton(
                onClick = { state.transpose(-1) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Black
                ),
                modifier = weightedSideButtonMod
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Transpose down")
            }

            Column(
                modifier = Modifier.weight(1f),
            ) {
                val weightedMainButtonMod = sideButtonMod
                    .weight(0.5f)
                    .fillMaxWidth()
                Button(
                    onClick = { state.addNote() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.Black
                    ),
                    modifier = weightedMainButtonMod
                ) {
                    Text("+", fontSize = 24.sp, fontWeight = FontWeight(600))
                }
                Button(
                    onClick = { state.removeNote() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.Black
                    ),
                    modifier = weightedMainButtonMod
                ) {
                    Text("-", fontSize = 24.sp, fontWeight = FontWeight(600))
                }
            }

            IconButton(
                onClick = { state.transpose(1) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Black
                ),
                modifier = weightedSideButtonMod
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Transpose up")
            }
        }
    }

    // TODO do not show the save dialog if no changes where made
    // TODO better selection list visuals and broader buttons for better experience
    // A scrollable list for selecting tunings
    // Combined clickable is experimental, but considered stable
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun TuningSelection(
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(BORDER_RADIUS))
                .padding(16.dp)
        ) {
            state.tunings.keys
                .toList()
                .sorted()
                .forEach { item ->
                    Text(
                        text = item,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { state.selectTuning(item) },
                                onLongClick = { showDeleteTuningDialog(item) },
                            )
                            .padding(PADDING)
                    )
                    HorizontalDivider()
                }
            Text(
                text = "+",
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { state.addTuning() }
                    .padding(PADDING)
            )
        }
    }

    // Dialogs related to tuning editing
    // TODO dialog theme?
    private fun showDeleteTuningDialog(item: String) {
        AlertDialog
            .Builder(this)
            .setTitle("Delete Tuning?")
            .setMessage("Do you want to delete the tuning \"$item\"")
            .setCancelable(true)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("OK") { dialog, _ ->
                state.deleteTuning(item)
                dialog.dismiss()
            }
            .show()
    }

    private fun showSaveTuningDialog() {
        AlertDialog.Builder(this)
            .setTitle("Save Tuning?")
            .setMessage("Do you want to save the changes made to \"${state.editedName.value}\"")
            .setCancelable(true)
            .setNegativeButton("Discard") { dialog, _ ->
                state.discardEdits()
                dialog.dismiss()
            }
            .setNeutralButton("Keep editing") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Save") { dialog, _ ->
                if (state.tuningName.value != state.editedName.value && state.tunings.contains(state.editedName.value)) {
                    showOverrideDialog()
                } else {
                    state.saveTuning()
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun showOverrideDialog() {
        AlertDialog.Builder(this)
            .setTitle("Override Tuning?")
            .setMessage("This will override an existing tuning. Do you wish to proceed?")
            .setCancelable(true)
            .setNegativeButton("Discard") { dialog, _ ->
                state.discardEdits()
                dialog.dismiss()
            }
            .setNeutralButton("Keep editing") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Override") { dialog, _ ->
                state.saveTuning()
                dialog.dismiss()
            }
            .show()
    }

    // Permission handling
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

    // TODO use onStop/onPause to stop animations etc.

    // Preview
    // TODO preview not working
    @Preview
    @Composable
    fun AppPreview() {
        MainPanel()
    }
}

