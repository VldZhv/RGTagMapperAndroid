package com.rg.mapper.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.rg.mapper.android.model.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

@Composable
fun PlanCanvas(
    state: PlanEditorState,
    onLongPressEdit: (Selection) -> Unit,
    modifier: Modifier = Modifier
) {
    // сохраняем масштаб и сдвиг между конфигурациями
    val offsetSaver: Saver<Offset, List<Float>> = remember {
        Saver(
            save = { listOf(it.x, it.y) },
            restore = { list -> Offset(list[0], list[1]) }
        )
    }
    var scale by rememberSaveable { mutableStateOf(1f) }
    var trans by rememberSaveable(stateSaver = offsetSaver) { mutableStateOf(Offset.Zero) }

    fun toScene(p: Offset): Offset =
        Offset((p.x - trans.x) / scale, (p.y - trans.y) / scale)

    // drag state
    var dragging by remember { mutableStateOf(false) }
    var dragEntity by remember { mutableStateOf<Selection?>(null) }
    var isPanning by remember { mutableStateOf(false) }

    // цвета
    val gridColor = Color(0x22000000)
    val hallStroke = Color(0xFF0000FF)
    val hallFill = Color(0x330000FF)
    val enterColor = Color(0x80008000)   // зелёный
    val exitColor  = Color(0x80800080)   // фиолетовый
    val boundColor = enterColor          // при желании задайте отдельный цвет
    val anchorColor = Color(0xFFFF0000)

    Canvas(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .clipToBounds()

            // Тапы / длинный тап
            .pointerInput(state.mode, state.halls, state.zones, state.anchors, scale, trans) {
                detectTapGestures(
                    onLongPress = { pos ->
                        val scene = toScene(pos)
                        if (state.mode != ToolMode.Calibrate) {
                            state.anchorAt(scene)?.let {
                                state.selection = Selection(anchorNumber = it.number)
                                onLongPressEdit(state.selection); return@detectTapGestures
                            }
                            state.zoneAt(scene)?.let { z ->
                                state.selection = Selection(hallNumber = z.hallNumber, zoneKey = z.hallNumber to z.zoneNum)
                                onLongPressEdit(state.selection); return@detectTapGestures
                            }
                            state.hallAt(scene)?.let { h ->
                                state.selection = Selection(hallNumber = h.number)
                                onLongPressEdit(state.selection)
                            }
                        }
                    },
                    onTap = { pos ->
                        val scene = toScene(pos)
                        when (state.mode) {
                            ToolMode.Calibrate -> {
                                if (state.tempPointA == null) state.tempPointA = scene
                                else { state.tempPointA = scene; onLongPressEdit(Selection()) }
                            }
                            ToolMode.AddHall -> {
                                if (!state.gridCalibrated) return@detectTapGestures
                                if (state.tempPointA == null) state.tempPointA = scene
                                else {
                                    val h = state.addHallByPoints(state.tempPointA!!, scene)
                                    state.selection = Selection(hallNumber = h.number)
                                    onLongPressEdit(state.selection)
                                    state.tempPointA = null
                                }
                            }
                            ToolMode.AddZone -> {
                                val hall = state.hallAt(scene) ?: return@detectTapGestures
                                if (state.tempPointA == null) {
                                    state.tempPointA = scene
                                } else {
                                    val a = state.tempPointA!!
                                    if (state.hallAt(scene)?.number == hall.number) {
                                        val blX = min(a.x, scene.x) - hall.xPx
                                        val blY = max(a.y, scene.y) - hall.yPx // bottom_left_y от ВЕРХА
                                        val w = abs(scene.x - a.x)
                                        val h = abs(scene.y - a.y)
                                        val z = Zone(
                                            hallNumber = hall.number,
                                            zoneNum = state.nextZoneNumber(hall.number),
                                            type = ZoneType.ENTER,
                                            angleDeg = 0,
                                            blX_Px = blX,
                                            blY_Px = blY,
                                            wPx = w,
                                            hPx = h
                                        )
                                        state.zones.add(z)
                                        state.selection = Selection(hallNumber = hall.number, zoneKey = hall.number to z.zoneNum)
                                        onLongPressEdit(state.selection)
                                    }
                                    state.tempPointA = null
                                }
                            }
                            ToolMode.AddAnchor -> {
                                val hall = state.hallAt(scene) ?: return@detectTapGestures
                                val a = Anchor(
                                    number = state.nextAnchorNumber(),
                                    xScenePx = scene.x,
                                    yScenePx = scene.y,
                                    zCm = 0,
                                    mainHall = hall.number
                                )
                                state.anchors.add(a)
                                state.selection = Selection(anchorNumber = a.number)
                                onLongPressEdit(state.selection)
                            }
                            else -> Unit
                        }
                    }
                )
            }

            // Drag: если начали на объекте в режиме Курсор — двигаем объект; иначе — панорамируем холст
            .pointerInput(state.mode, state.halls, state.zones, state.anchors, scale, trans) {
                detectDragGestures(
                    onDragStart = { pos ->
                        isPanning = true
                        dragging = false
                        dragEntity = null

                        if (state.mode == ToolMode.Cursor) {
                            val scene = toScene(pos)
                            state.anchorAt(scene)?.takeIf { !state.lockAnchors }?.let {
                                dragging = true; isPanning = false
                                dragEntity = Selection(anchorNumber = it.number); return@detectDragGestures
                            }
                            state.zoneAt(scene)?.takeIf { !state.lockZones }?.let { z ->
                                dragging = true; isPanning = false
                                dragEntity = Selection(hallNumber = z.hallNumber, zoneKey = z.hallNumber to z.zoneNum); return@detectDragGestures
                            }
                            state.hallAt(scene)?.takeIf { !state.lockHalls }?.let { h ->
                                dragging = true; isPanning = false
                                dragEntity = Selection(hallNumber = h.number); return@detectDragGestures
                            }
                        }
                    },
                    onDrag = { change, drag ->
                        if (isPanning) {
                            change.consume()
                            trans += drag // экранные пиксели
                            return@detectDragGestures
                        }
                        if (!dragging || dragEntity == null) return@detectDragGestures
                        change.consume()

                        // Перетаскивание объектов — в координатах сцены
                        val deltaScene = Offset(drag.x / scale, drag.y / scale)
                        val stepPx = state.pixelPerCm * state.gridStepCm
                        fun snap(v: Float) = if (stepPx > 0f) round(v / stepPx) * stepPx else v

                        dragEntity?.let { sel ->
                            sel.anchorNumber?.let { id ->
                                val i = state.anchors.indexOfFirst { it.number == id }
                                if (i >= 0) {
                                    val a = state.anchors[i]
                                    state.anchors[i] = a.copy(
                                        xScenePx = snap(a.xScenePx + deltaScene.x),
                                        yScenePx = snap(a.yScenePx + deltaScene.y)
                                    )
                                }
                            }
                            sel.hallNumber?.let { hn ->
                                if (sel.zoneKey == null) {
                                    val i = state.halls.indexOfFirst { it.number == hn }
                                    if (i >= 0) {
                                        val h = state.halls[i]
                                        state.halls[i] = h.copy(
                                            xPx = snap(h.xPx + deltaScene.x),
                                            yPx = snap(h.yPx + deltaScene.y)
                                        )
                                    }
                                } else {
                                    val (hallN, zoneN) = sel.zoneKey
                                    val i = state.zones.indexOfFirst { it.hallNumber == hallN && it.zoneNum == zoneN }
                                    if (i >= 0) {
                                        val z = state.zones[i]
                                        state.zones[i] = z.copy(
                                            blX_Px = snap(z.blX_Px + deltaScene.x),
                                            blY_Px = snap(z.blY_Px + deltaScene.y) // ось Y вниз (от верха)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    onDragEnd = { dragging = false; isPanning = false; dragEntity = null },
                    onDragCancel = { dragging = false; isPanning = false; dragEntity = null }
                )
            }

            // Щипок (масштабирование вокруг точки касания) + двухпальцевый pan
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = scale
                    val newScale = (oldScale * zoom).coerceIn(0.2f, 8f)
                    val scaleChange = newScale / oldScale

                    // сначала двухпальцевый pan в экранных пикселях
                    val prePanTrans = trans + pan

                    // фиксация фокуса под пальцем:
                    // t' = (t + pan) + (centroid - (t + pan)) * (1 - scaleChange)
                    trans = prePanTrans + (centroid - prePanTrans) * (1 - scaleChange)

                    // затем обновляем масштаб
                    scale = newScale
                }
            }
    ) {
        // --- сцена: применяем экранные трансформации ---
        withTransform({
            translate(trans.x, trans.y)
            scale(scale, scale)
        }) {
            // фон
            state.background?.let { drawImage(it.asImageBitmap()) }

            // сетка
            if (state.gridCalibrated) {
                val stepPx = state.pixelPerCm * state.gridStepCm
                if (stepPx > 2f) {
                    val w = size.width / scale
                    val h = size.height / scale
                    val maxX = max(w, state.bgWidthPx.toFloat())
                    val maxY = max(h, state.bgHeightPx.toFloat())

                    var x = 0f
                    while (x <= maxX) {
                        drawLine(gridColor, Offset(x, 0f), Offset(x, maxY), 1f, StrokeCap.Butt)
                        x += stepPx
                    }
                    var y = 0f
                    while (y <= maxY) {
                        drawLine(gridColor, Offset(0f, y), Offset(maxX, y), 1f, StrokeCap.Butt)
                        y += stepPx
                    }
                }
            }

            // залы
            state.halls.forEach { h ->
                drawRect(hallFill, topLeft = Offset(h.xPx, h.yPx), size = Size(h.wPx, h.hPx))
                drawRect(hallStroke, topLeft = Offset(h.xPx, h.yPx), size = Size(h.wPx, h.hPx), style = Stroke(2f))
                drawIntoCanvas {
                    it.nativeCanvas.drawText(
                        h.number.toString(),
                        h.xPx + 6f,
                        h.yPx + h.hPx - 6f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.BLUE
                            textSize = 28f
                            isFakeBoldText = true
                        }
                    )
                }
            }

            // зоны: перенос в (hall.x + blX, hall.y + blY), затем rotate(-angle) вокруг (0,0)
            state.zones.forEach { z ->
                val hall = state.halls.find { it.number == z.hallNumber } ?: return@forEach
                val zoneColor = when (z.type) {
                    ZoneType.ENTER -> enterColor
                    ZoneType.EXIT  -> exitColor
                    ZoneType.BOUND -> boundColor
                }
                withTransform({
                    translate(hall.xPx + z.blX_Px, hall.yPx + z.blY_Px)
                    // поворот как в десктопе (Qt setRotation(-angle))
                    rotate(degrees = -z.angleDeg.toFloat(), pivot = Offset.Zero)
                }) {
                    drawRect(
                        color = zoneColor.copy(alpha = 0.35f),
                        topLeft = Offset(0f, -z.hPx),
                        size = Size(z.wPx, z.hPx)
                    )
                    drawRect(
                        color = zoneColor,
                        topLeft = Offset(0f, -z.hPx),
                        size = Size(z.wPx, z.hPx),
                        style = Stroke(width = 2f)
                    )
                    drawIntoCanvas {
                        it.nativeCanvas.drawText(
                            z.zoneNum.toString(),
                            6f,
                            -6f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GREEN
                                textSize = 26f
                                isFakeBoldText = true
                            }
                        )
                    }
                }
            }

            // якоря
            state.anchors.forEach { a ->
                drawCircle(color = anchorColor, radius = 5f, center = Offset(a.xScenePx, a.yScenePx))
                drawIntoCanvas {
                    it.nativeCanvas.drawText(
                        a.number.toString(),
                        a.xScenePx + 8f,
                        a.yScenePx - 8f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.RED
                            textSize = 26f
                            isFakeBoldText = true
                        }
                    )
                }
            }
        }

        // подсказка
        val hint = when (state.mode) {
            ToolMode.Calibrate -> "Калибровка: два тапа — 2 точки"
            ToolMode.AddHall   -> "Добавление зала: два тапа — противоположные углы"
            ToolMode.AddZone   -> "Добавление зоны: два тапа внутри зала"
            ToolMode.AddAnchor -> "Добавление якоря: тап по залу"
            else               -> "Панорама: свайп одним пальцем; Зум: щипок; Долгий тап — редактировать"
        }
        drawIntoCanvas {
            it.nativeCanvas.drawText(
                hint,
                12f,
                28f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 28f
                }
            )
        }
    }
}
