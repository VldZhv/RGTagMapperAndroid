@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.rg.mapper.android.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.rg.mapper.android.model.Anchor
import com.rg.mapper.android.model.ExportConfig
import com.rg.mapper.android.model.Hall
import com.rg.mapper.android.model.ProjectSave
import com.rg.mapper.android.model.Zone
import com.rg.mapper.android.model.ZoneType
import com.rg.mapper.android.ui.PlanEditorState
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import java.io.ByteArrayOutputStream
import java.io.IOException

// -------------------------------
// JSON: максимально терпимый
// -------------------------------
val jsonRelaxed: Json = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
    isLenient = true
    allowTrailingComma = true   // ExperimentalSerializationApi
    coerceInputValues = true
}

// -------------------------------
// Desktop-формат .proj (как в RG_Tag_Mapper.py)
// -------------------------------
@Serializable
private data class DZone(
    @JsonNames("zone_num", "zoneNum", "number", "num", "id", "zoneId")
    val zone_num: Int = 0,

    // В десктопе текстовые типы: "Входная зона" | "Выходная зона" | "Переходная"
    @JsonNames("zone_type", "zoneType", "type")
    val zone_type: String = "Входная зона",

    @JsonNames("zone_angle", "angle", "angleDeg")
    val zone_angle: Int = 0,

    // ВНИМАНИЕ: локальные координаты ОТ ВЕРХНЕГО ЛЕВОГО угла зала вниз/вправо!
    @JsonNames("bottom_left_x", "bl_x", "x")
    val bottom_left_x: Float = 0f,

    @JsonNames("bottom_left_y", "bl_y", "y")
    val bottom_left_y: Float = 0f,

    @JsonNames("w_px", "w", "width")
    val w_px: Float = 0f,

    @JsonNames("h_px", "h", "height")
    val h_px: Float = 0f
)

@Serializable
private data class DHall(
    @JsonNames("num", "number", "hall_number")
    val num: Int = 0,
    @JsonNames("name", "title", "label")
    val name: String = "",
    @JsonNames("x_px", "xPx", "x")
    val x_px: Float = 0f,
    @JsonNames("y_px", "yPx", "y")
    val y_px: Float = 0f,
    @JsonNames("w_px", "wPx", "width")
    val w_px: Float = 0f,
    @JsonNames("h_px", "hPx", "height")
    val h_px: Float = 0f,
    @JsonNames("zones", "areas")
    val zones: List<DZone> = emptyList()
)

@Serializable
private data class DAnchor(
    @JsonNames("number", "id")
    val number: Int = 0,
    // Глобальные координаты сцены
    @JsonNames("x", "x_px")
    val x: Float = 0f,
    @JsonNames("y", "y_px")
    val y: Float = 0f,
    // Высота в сантиметрах
    @JsonNames("z", "z_cm")
    val z: Int = 0,
    @JsonNames("main_hall", "mainHall")
    val main_hall: Int? = null,
    @JsonNames("extra_halls", "extraHalls")
    val extra_halls: List<Int> = emptyList(),
    @JsonNames("bound", "is_bound")
    val bound: Boolean? = null
)

@Serializable
private data class DProject(
    val image_data: String = "",
    val pixel_per_cm_x: Float = 1f,
    val pixel_per_cm_y: Float = 1f,
    val grid_step_cm: Float = 20f,
    val lock_halls: Boolean = false,
    val lock_zones: Boolean = false,
    val lock_anchors: Boolean = false,
    val halls: List<DHall> = emptyList(),
    val anchors: List<DAnchor> = emptyList()
)

// -------------------------------
// Bitmap <-> Base64
// -------------------------------
fun Bitmap.toBase64Png(): String {
    val baos = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.PNG, 100, baos)
    val bytes = baos.toByteArray()
    return if (Build.VERSION.SDK_INT >= 26) {
        java.util.Base64.getEncoder().encodeToString(bytes)
    } else {
        android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }
}

fun base64ToBitmap(b64: String): Bitmap? {
    if (b64.isBlank()) return null
    val bytes = if (Build.VERSION.SDK_INT >= 26) {
        java.util.Base64.getDecoder().decode(b64)
    } else {
        android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
    }
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

fun loadBitmapFromUri(resolver: android.content.ContentResolver, uri: Uri): Bitmap? =
    if (Build.VERSION.SDK_INT >= 28)
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri))
    else
        @Suppress("DEPRECATION")
        android.provider.MediaStore.Images.Media.getBitmap(resolver, uri)

// -------------------------------
// Утилиты преобразования типов зон
// -------------------------------
private fun String.toZoneType(): ZoneType = when (this.trim().lowercase()) {
    "входная зона", "входная", "enter" -> ZoneType.ENTER
    "выходная зона", "выходная", "exit" -> ZoneType.EXIT
    "переходная", "bound" -> ZoneType.BOUND
    else -> ZoneType.ENTER
}
private fun ZoneType.toDesktopString(): String = when (this) {
    ZoneType.ENTER -> "Входная зона"
    ZoneType.EXIT  -> "Выходная зона"
    ZoneType.BOUND -> "Переходная"
}

// -------------------------------
// Состояние редактора ↔ сохранение
// -------------------------------
fun PlanEditorState.toProjectSave(): ProjectSave = ProjectSave(
    image_data = background?.toBase64Png() ?: "",
    pixel_per_cm_x = pixelPerCm,
    pixel_per_cm_y = pixelPerCm,
    grid_step_cm = gridStepCm,
    lock_halls = lockHalls,
    lock_zones = lockZones,
    lock_anchors = lockAnchors,
    halls = halls.toList(),
    anchors = anchors.toList(),
    zones = zones.toList()
)

fun PlanEditorState.loadFrom(save: ProjectSave) {
    clearAll()
    background = base64ToBitmap(save.image_data)
    bgWidthPx = background?.width ?: 0
    bgHeightPx = background?.height ?: 0
    pixelPerCm = when {
        save.pixel_per_cm_x > 0f -> save.pixel_per_cm_x
        save.pixel_per_cm_y > 0f -> save.pixel_per_cm_y
        else -> 1f
    }
    gridStepCm = if (save.grid_step_cm > 0f) save.grid_step_cm else 20f
    lockHalls = save.lock_halls
    lockZones = save.lock_zones
    lockAnchors = save.lock_anchors
    halls.addAll(save.halls)
    anchors.addAll(save.anchors)
    zones.addAll(save.zones)
    gridCalibrated = true
}

fun PlanEditorState.toExportJson(): String =
    jsonRelaxed.encodeToString(ExportConfig.serializer(), exportRooms())

// -------------------------------
// ЕДИНЫЙ .proj (полная совместимость с десктопом)
// -------------------------------

/** Сохранить проект в desktop-формате .proj */
fun PlanEditorState.toProjectJson(): String {
    val dHalls = halls.map { h ->
        val zList = zones.filter { it.hallNumber == h.number }.map { z ->
            // ВАЖНО: bottom_left_y «от верха», без инверсий!
            DZone(
                zone_num = z.zoneNum,
                zone_type = z.type.toDesktopString(),
                zone_angle = z.angleDeg,
                bottom_left_x = z.blX_Px,
                bottom_left_y = z.blY_Px,
                w_px = z.wPx,
                h_px = z.hPx
            )
        }
        DHall(
            num = h.number, name = h.name,
            x_px = h.xPx, y_px = h.yPx,
            w_px = h.wPx, h_px = h.hPx,
            zones = zList
        )
    }
    val dAnchors = anchors.map { a ->
        DAnchor(
            number = a.number, x = a.xScenePx, y = a.yScenePx, z = a.zCm,
            main_hall = a.mainHall, extra_halls = a.extraHalls,
            bound = if (a.bound) true else null
        )
    }
    val proj = DProject(
        image_data = background?.toBase64Png() ?: "",
        pixel_per_cm_x = pixelPerCm,
        pixel_per_cm_y = pixelPerCm,
        grid_step_cm = gridStepCm,
        lock_halls = lockHalls,
        lock_zones = lockZones,
        lock_anchors = lockAnchors,
        halls = dHalls,
        anchors = dAnchors
    )
    return jsonRelaxed.encodeToString(DProject.serializer(), proj)
}

/** Загрузить проект из desktop-формата .proj */
fun PlanEditorState.loadProjectJson(json: String) {
    if (json.isBlank()) return

    // 1) Пробуем десктоп-формат
    runCatching { jsonRelaxed.decodeFromString(DProject.serializer(), json) }
        .onSuccess { dp ->
            clearAll()

            background = base64ToBitmap(dp.image_data)
            bgWidthPx = background?.width ?: 0
            bgHeightPx = background?.height ?: 0
            pixelPerCm = if (dp.pixel_per_cm_x > 0f) dp.pixel_per_cm_x else 1f
            gridStepCm = if (dp.grid_step_cm > 0f) dp.grid_step_cm else 20f
            lockHalls = dp.lock_halls
            lockZones = dp.lock_zones
            lockAnchors = dp.lock_anchors

            // Залы
            dp.halls.forEach { dh ->
                halls.add(
                    Hall(
                        number = dh.num, name = dh.name,
                        xPx = dh.x_px, yPx = dh.y_px,
                        wPx = dh.w_px, hPx = dh.h_px
                    )
                )
            }

            // Зоны (ВНИМАНИЕ: bottom_left_y читаем «как есть», от ВЕРХА!)
            dp.halls.forEach { dh ->
                dh.zones.forEach { dz ->
                    zones.add(
                        Zone(
                            hallNumber = dh.num,
                            zoneNum = dz.zone_num,
                            type = dz.zone_type.toZoneType(),
                            angleDeg = dz.zone_angle,
                            blX_Px = dz.bottom_left_x,
                            blY_Px = dz.bottom_left_y,   // <- без инверсии!
                            wPx = dz.w_px,
                            hPx = dz.h_px
                        )
                    )
                }
            }

            // Якоря
            dp.anchors.forEach { da ->
                anchors.add(
                    Anchor(
                        number = da.number,
                        xScenePx = da.x,
                        yScenePx = da.y,
                        zCm = da.z,
                        mainHall = da.main_hall,
                        extraHalls = da.extra_halls,
                        bound = da.bound == true
                    )
                )
            }

            gridCalibrated = true
            return
        }

    // 2) Fallback: наши ранние варианты, если встретятся
    runCatching {
        val legacy = jsonRelaxed.decodeFromString(ProjectSave.serializer(), json)
        loadFrom(legacy)
    }
}

// -------------------------------
// I/O через SAF
// -------------------------------
fun writeTextToUri(ctx: Context, uri: Uri, text: String) {
    ctx.contentResolver.openOutputStream(uri)?.use { os ->
        os.write(text.toByteArray()); os.flush()
    }
}

@Throws(IOException::class)
fun readTextFromUri(ctx: Context, uri: Uri): String {
    val sb = StringBuilder()
    ctx.contentResolver.openInputStream(uri)?.bufferedReader().use { br ->
        if (br == null) return ""
        var line: String?
        while (true) {
            line = br.readLine() ?: break
            sb.append(line).append('\n')
        }
    }
    return sb.toString()
}
