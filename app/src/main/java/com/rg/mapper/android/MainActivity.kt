package com.rg.mapper.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rg.mapper.android.model.Anchor
import com.rg.mapper.android.model.Hall
import com.rg.mapper.android.model.Zone
import com.rg.mapper.android.ui.AnchorDialog
import com.rg.mapper.android.ui.HallDialog
import com.rg.mapper.android.ui.PlanCanvas
import com.rg.mapper.android.ui.PlanEditorState
import com.rg.mapper.android.ui.Selection
import com.rg.mapper.android.ui.ToolMode
import com.rg.mapper.android.ui.ZoneDialog
import com.rg.mapper.android.util.loadProjectJson
import com.rg.mapper.android.R
import java.io.BufferedReader
import java.io.InputStreamReader

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

    LaunchedEffect(Unit) {
        if (state.halls.isNotEmpty() || state.zones.isNotEmpty() || state.anchors.isNotEmpty()) return@LaunchedEffect
        runCatching {
            context.resources.openRawResource(R.raw.default_project).use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            }
        }.onSuccess { json ->
            state.loadProjectJson(json)
        }.onFailure { error ->
            Toast.makeText(context, "Не удалось загрузить проект: ${error.message}", Toast.LENGTH_LONG).show()
        }
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
                Text(
                    text = "Готовый проект загружен автоматически. Долгий тап по объекту открывает его параметры.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )

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
