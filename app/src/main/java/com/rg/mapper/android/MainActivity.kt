package com.rg.mapper.android

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rg.mapper.android.model.*
import com.rg.mapper.android.ui.*
import com.rg.mapper.android.util.*
import kotlin.math.hypot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppUI() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppUI() {
    val state = remember { PlanEditorState() }
    val context = LocalContext.current

    // ---------- Pickers ----------
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val bmp = loadBitmapFromUri(context.contentResolver, uri) ?: return@rememberLauncherForActivityResult
        state.background = bmp
        state.bgWidthPx = bmp.width
        state.bgHeightPx = bmp.height
        state.gridCalibrated = false
        state.mode = ToolMode.Calibrate
    }

    val saveProject = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        writeTextToUri(context, uri, state.toProjectJson())
        Toast.makeText(context, "Проект сохранён", Toast.LENGTH_SHORT).show()
    }

    val loadProject = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            val json = readTextFromUri(context, uri)
            state.loadProjectJson(json)
            Toast.makeText(context, "Проект загружен", Toast.LENGTH_SHORT).show()
        } catch (t: Throwable) {
            Toast.makeText(context, "Ошибка загрузки: ${t.message}", Toast.LENGTH_LONG).show()
        }
    }

    val exportJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        writeTextToUri(context, uri, state.toExportJson())
        Toast.makeText(context, "Экспортировано в config.json", Toast.LENGTH_SHORT).show()
    }

    // ---------- Dialog state ----------
    var showHallDialog by remember { mutableStateOf(false) }
    var hallDraft by remember { mutableStateOf<Hall?>(null) }

    var showZoneDialog by remember { mutableStateOf(false) }
    var zoneDraft by remember { mutableStateOf<Zone?>(null) }

    var showAnchorDialog by remember { mutableStateOf(false) }
    var anchorDraft by remember { mutableStateOf<Anchor?>(null) }

    // Калибровка
    var calPointA by remember { mutableStateOf<Offset?>(null) }
    var calPointB by remember { mutableStateOf<Offset?>(null) }
    var showCalibrateDialog by remember { mutableStateOf(false) }
    var lengthCm by remember { mutableStateOf("100.0") }
    var gridStepCm by remember { mutableStateOf("10") }

    fun openEditorFor(sel: Selection) {
        sel.anchorNumber?.let { id ->
            state.anchors.find { it.number == id }?.let { anchorDraft = it; showAnchorDialog = true }
            return
        }
        sel.zoneKey?.let { (hn, zn) ->
            state.zones.firstOrNull { it.hallNumber == hn && it.zoneNum == zn }?.let { zoneDraft = it; showZoneDialog = true }
            return
        }
        sel.hallNumber?.let { hn ->
            state.halls.find { it.number == hn }?.let { hallDraft = it; showHallDialog = true }
        }
    }

    MaterialTheme(colorScheme = lightColorScheme()) {
        // Scaffold без TopAppBar
        Scaffold { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // ---------- Панель 1: Файлы ----------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { pickImage.launch(arrayOf("image/*")) }) {
                        Text("Открыть план")
                    }
                    OutlinedButton(onClick = {
                        loadProject.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                    }) { Text("Загрузить проект") }
                    OutlinedButton(onClick = { saveProject.launch("project.json") }) {
                        Text("Сохранить проект")
                    }
                    OutlinedButton(onClick = { exportJson.launch("config.json") }) {
                        Text("Экспорт JSON")
                    }
                }

                // ---------- Панель 2: Режимы ----------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.mode == ToolMode.Cursor,
                        onClick = { state.mode = ToolMode.Cursor },
                        label = { Text("Курсор") }
                    )
                    FilterChip(
                        selected = state.mode == ToolMode.Calibrate,
                        onClick = { state.mode = ToolMode.Calibrate },
                        label = { Text("Калибровка") }
                    )
                    FilterChip(
                        selected = state.mode == ToolMode.AddHall,
                        onClick = { state.mode = ToolMode.AddHall },
                        label = { Text("Зал") }
                    )
                    FilterChip(
                        selected = state.mode == ToolMode.AddZone,
                        onClick = { state.mode = ToolMode.AddZone },
                        label = { Text("Зона") }
                    )
                    FilterChip(
                        selected = state.mode == ToolMode.AddAnchor,
                        onClick = { state.mode = ToolMode.AddAnchor },
                        label = { Text("Якорь") }
                    )
                }

                // ---------- Холст ----------
                Box(
                    Modifier
                        .fillMaxSize()
                        .zIndex(0f)
                ) {
                    PlanCanvas(
                        state = state,
                        onLongPressEdit = { sel ->
                            if (state.mode == ToolMode.Calibrate) {
                                if (calPointA == null) calPointA = state.tempPointA
                                else { calPointB = state.tempPointA; showCalibrateDialog = true }
                            } else openEditorFor(sel)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // ---- диалоги (оставляем как были) ----
        if (showHallDialog && hallDraft != null) {
            HallDialog(
                visible = true,
                initial = hallDraft!!,
                ppcm = state.pixelPerCm,
                onDismiss = { showHallDialog = false; hallDraft = null },
                onApply = { updated ->
                    val idx = state.halls.indexOfFirst { it.number == hallDraft!!.number }
                    if (idx >= 0) state.halls[idx] = updated
                    showHallDialog = false; hallDraft = null
                }
            )
        }
        if (showZoneDialog && zoneDraft != null) {
            ZoneDialog(
                visible = true,
                initial = zoneDraft!!,
                ppcm = state.pixelPerCm,
                onDismiss = { showZoneDialog = false; zoneDraft = null },
                onApply = { updated ->
                    val idx = state.zones.indexOfFirst {
                        it.hallNumber == zoneDraft!!.hallNumber &&
                                it.zoneNum == zoneDraft!!.zoneNum &&
                                it.type == zoneDraft!!.type
                    }
                    if (idx >= 0) state.zones[idx] = updated
                    showZoneDialog = false; zoneDraft = null
                }
            )
        }
        if (showAnchorDialog && anchorDraft != null) {
            AnchorDialog(
                visible = true,
                initial = anchorDraft!!,
                ppcm = state.pixelPerCm,
                hallForDefault = state.hallAt(
                    Offset(anchorDraft!!.xScenePx, anchorDraft!!.yScenePx)
                )?.number,
                onDismiss = { showAnchorDialog = false; anchorDraft = null },
                onApply = { updated ->
                    val idx = state.anchors.indexOfFirst { it.number == anchorDraft!!.number }
                    if (idx >= 0) state.anchors[idx] = updated
                    showAnchorDialog = false; anchorDraft = null
                }
            )
        }
    }
}
