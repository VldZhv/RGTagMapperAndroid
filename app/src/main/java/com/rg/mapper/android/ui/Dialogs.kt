package com.rg.mapper.android.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rg.mapper.android.model.*
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.*   // Column, Row, Spacer


@Composable
fun HallDialog(
    visible: Boolean,
    initial: Hall,
    ppcm: Float,
    onDismiss: () -> Unit,
    onApply: (Hall) -> Unit
) {
    if (!visible) return
    var number by remember { mutableStateOf(initial.number.toString()) }
    var name by remember { mutableStateOf(initial.name) }
    var w by remember { mutableStateOf(String.format("%.1f", initial.wPx / (ppcm * 100f))) }
    var h by remember { mutableStateOf(String.format("%.1f", initial.hPx / (ppcm * 100f))) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Параметры зала") },
        text = {
            Column {
                OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Номер зала") })
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") })
                OutlinedTextField(value = w, onValueChange = { w = it }, label = { Text("Ширина (м)") })
                OutlinedTextField(value = h, onValueChange = { h = it }, label = { Text("Высота (м)") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val n = number.toIntOrNull() ?: initial.number
                val wm = w.toFloatOrNull()?.coerceAtLeast(0.1f) ?: (initial.wPx / (ppcm * 100f))
                val hm = h.toFloatOrNull()?.coerceAtLeast(0.1f) ?: (initial.hPx / (ppcm * 100f))
                val wp = wm * ppcm * 100f
                val hp = hm * ppcm * 100f
                onApply(initial.copy(number = n, name = name, wPx = wp, hPx = hp))
            }) { Text("Сохранить") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun AnchorDialog(
    visible: Boolean,
    initial: Anchor,
    ppcm: Float,
    hallForDefault: Int?,
    onDismiss: () -> Unit,
    onApply: (Anchor) -> Unit
) {
    if (!visible) return
    var number by remember { mutableStateOf(initial.number.toString()) }
    var z by remember { mutableStateOf(String.format("%.1f", initial.zCm / 100f)) }
    var extras by remember { mutableStateOf(initial.extraHalls.joinToString(",")) }
    var bound by remember { mutableStateOf(initial.bound) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Параметры якоря") },
        text = {
            Column {
                OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Номер якоря") })
                OutlinedTextField(value = z, onValueChange = { z = it }, label = { Text("Координата Z (м)") })
                OutlinedTextField(value = extras, onValueChange = { extras = it }, label = { Text("Доп. залы (через запятую)") })
                Row { Checkbox(checked = bound, onCheckedChange = { bound = it }); Text("Переходный") }
            }
        },
        confirmButton = {
            Button(onClick = {
                val n = number.toIntOrNull() ?: initial.number
                val zCm = ((z.toFloatOrNull() ?: (initial.zCm / 100f)) * 100f).roundToInt()
                val ex = extras.split(',').mapNotNull { it.trim().toIntOrNull() }
                onApply(initial.copy(number = n, zCm = zCm, extraHalls = ex, bound = bound,
                    mainHall = initial.mainHall ?: hallForDefault))
            }) { Text("Сохранить") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun ZoneDialog(
    visible: Boolean,
    initial: Zone,
    ppcm: Float,
    onDismiss: () -> Unit,
    onApply: (Zone) -> Unit
) {
    if (!visible) return
    var number by remember { mutableStateOf(initial.zoneNum.toString()) }
    var type by remember { mutableStateOf(initial.type) }
    var angle by remember { mutableStateOf(initial.angleDeg.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Параметры зоны") },
        text = {
            Column {
                OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Номер зоны") })
                Row {
                    FilterChip(selected = type == ZoneType.ENTER, onClick = { type = ZoneType.ENTER }, label = { Text("Входная") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = type == ZoneType.EXIT, onClick = { type = ZoneType.EXIT }, label = { Text("Выходная") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = type == ZoneType.BOUND, onClick = { type = ZoneType.BOUND }, label = { Text("Переходная") })
                }
                OutlinedTextField(value = angle, onValueChange = { angle = it }, label = { Text("Угол (°)") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val n = number.toIntOrNull() ?: initial.zoneNum
                val ang = angle.toIntOrNull()?.coerceIn(-90, 90) ?: initial.angleDeg
                onApply(initial.copy(zoneNum = n, type = type, angleDeg = ang))
            }) { Text("Сохранить") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
