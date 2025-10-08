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
    val pxPerMeter = (ppcm.takeIf { it > 0f } ?: 1f) * 100f
    var number by remember { mutableStateOf(initial.number.toString()) }
    var name by remember { mutableStateOf(initial.name) }
    var x by remember { mutableStateOf(String.format("%.2f", initial.xPx / pxPerMeter)) }
    var y by remember { mutableStateOf(String.format("%.2f", initial.yPx / pxPerMeter)) }
    var w by remember { mutableStateOf(String.format("%.2f", initial.wPx / pxPerMeter)) }
    var h by remember { mutableStateOf(String.format("%.2f", initial.hPx / pxPerMeter)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Параметры зала") },
        text = {
            Column {
                OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Номер зала") })
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") })
                OutlinedTextField(value = x, onValueChange = { x = it }, label = { Text("Смещение X (м)") })
                OutlinedTextField(value = y, onValueChange = { y = it }, label = { Text("Смещение Y (м)") })
                OutlinedTextField(value = w, onValueChange = { w = it }, label = { Text("Ширина (м)") })
                OutlinedTextField(value = h, onValueChange = { h = it }, label = { Text("Высота (м)") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val n = number.toIntOrNull() ?: initial.number
                val xm = x.toFloatOrNull()?.coerceAtLeast(0f) ?: (initial.xPx / pxPerMeter)
                val ym = y.toFloatOrNull()?.coerceAtLeast(0f) ?: (initial.yPx / pxPerMeter)
                val wm = w.toFloatOrNull()?.coerceAtLeast(0.1f) ?: (initial.wPx / pxPerMeter)
                val hm = h.toFloatOrNull()?.coerceAtLeast(0.1f) ?: (initial.hPx / pxPerMeter)
                val xp = xm * pxPerMeter
                val yp = ym * pxPerMeter
                val wp = wm * pxPerMeter
                val hp = hm * pxPerMeter
                onApply(initial.copy(number = n, name = name, xPx = xp, yPx = yp, wPx = wp, hPx = hp))
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
    val pxPerMeter = (ppcm.takeIf { it > 0f } ?: 1f) * 100f
    var number by remember { mutableStateOf(initial.number.toString()) }
    var z by remember { mutableStateOf(String.format("%.1f", initial.zCm / 100f)) }
    var x by remember { mutableStateOf(String.format("%.2f", initial.xScenePx / pxPerMeter)) }
    var y by remember { mutableStateOf(String.format("%.2f", initial.yScenePx / pxPerMeter)) }
    var extras by remember { mutableStateOf(initial.extraHalls.joinToString(",")) }
    var bound by remember { mutableStateOf(initial.bound) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Параметры якоря") },
        text = {
            Column {
                OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Номер якоря") })
                OutlinedTextField(value = x, onValueChange = { x = it }, label = { Text("Координата X (м)") })
                OutlinedTextField(value = y, onValueChange = { y = it }, label = { Text("Координата Y (м)") })
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
                val xm = x.toFloatOrNull() ?: (initial.xScenePx / pxPerMeter)
                val ym = y.toFloatOrNull() ?: (initial.yScenePx / pxPerMeter)
                onApply(
                    initial.copy(
                        number = n,
                        xScenePx = xm * pxPerMeter,
                        yScenePx = ym * pxPerMeter,
                        zCm = zCm,
                        extraHalls = ex,
                        bound = bound,
                        mainHall = initial.mainHall ?: hallForDefault
                    )
                )
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
    val pxPerMeter = (ppcm.takeIf { it > 0f } ?: 1f) * 100f
    var number by remember { mutableStateOf(initial.zoneNum.toString()) }
    var type by remember { mutableStateOf(initial.type) }
    var angle by remember { mutableStateOf(initial.angleDeg.toString()) }
    var x by remember { mutableStateOf(String.format("%.2f", initial.blX_Px / pxPerMeter)) }
    var y by remember { mutableStateOf(String.format("%.2f", initial.blY_Px / pxPerMeter)) }
    var w by remember { mutableStateOf(String.format("%.2f", initial.wPx / pxPerMeter)) }
    var h by remember { mutableStateOf(String.format("%.2f", initial.hPx / pxPerMeter)) }

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
                OutlinedTextField(value = x, onValueChange = { x = it }, label = { Text("Смещение X (м)") })
                OutlinedTextField(value = y, onValueChange = { y = it }, label = { Text("Смещение Y от верха (м)") })
                OutlinedTextField(value = w, onValueChange = { w = it }, label = { Text("Ширина (м)") })
                OutlinedTextField(value = h, onValueChange = { h = it }, label = { Text("Высота (м)") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val n = number.toIntOrNull() ?: initial.zoneNum
                val ang = angle.toIntOrNull()?.coerceIn(-90, 90) ?: initial.angleDeg
                val xm = x.toFloatOrNull()?.coerceAtLeast(0f) ?: (initial.blX_Px / pxPerMeter)
                val ym = y.toFloatOrNull()?.coerceAtLeast(0f) ?: (initial.blY_Px / pxPerMeter)
                val wm = w.toFloatOrNull()?.coerceAtLeast(0.1f) ?: (initial.wPx / pxPerMeter)
                val hm = h.toFloatOrNull()?.coerceAtLeast(0.1f) ?: (initial.hPx / pxPerMeter)
                onApply(
                    initial.copy(
                        zoneNum = n,
                        type = type,
                        angleDeg = ang,
                        blX_Px = xm * pxPerMeter,
                        blY_Px = ym * pxPerMeter,
                        wPx = wm * pxPerMeter,
                        hPx = hm * pxPerMeter
                    )
                )
            }) { Text("Сохранить") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
