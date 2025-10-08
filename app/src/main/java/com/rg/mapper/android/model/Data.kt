@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.rg.mapper.android.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
enum class ZoneType(val human: String) {
    ENTER("Входная зона"),
    EXIT("Выходная зона"),
    BOUND("Переходная")
}

@Serializable
data class Hall(
    @JsonNames("number", "num", "hall_number")
    val number: Int = 0,

    @JsonNames("name", "title", "label")
    val name: String = "",

    // Координаты и размеры в пикселях сцены (десктоп мог писать x/y/width/height или x_px/…)
    @JsonNames("xPx", "x_px", "x")
    val xPx: Float = 0f,

    @JsonNames("yPx", "y_px", "y")
    val yPx: Float = 0f,

    @JsonNames("wPx", "w_px", "width", "w")
    val wPx: Float = 0f,

    @JsonNames("hPx", "h_px", "height", "h")
    val hPx: Float = 0f
)

@Serializable
data class Zone(
    @JsonNames("hallNumber", "hall_number", "room", "hall")
    val hallNumber: Int = 0,

    @JsonNames("zoneNum", "number", "num", "zone_number")
    val zoneNum: Int = 0,

    // В десктопном файле могло быть "type": "ENTER"/"EXIT"/"BOUND" или "enter/exit"
    @JsonNames("type", "zoneType")
    val type: ZoneType = ZoneType.ENTER,

    @JsonNames("angleDeg", "angle_deg", "angle", "rotation")
    val angleDeg: Int = 0,

    // Локальные координаты зоны относительно низа-зала (у нас blX/blY снизу, у десктопа могли быть x/y сверху)
    @JsonNames("blX_Px", "bl_x_px", "x_px", "x")
    val blX_Px: Float = 0f,

    @JsonNames("blY_Px", "bl_y_px", "y_px", "y")
    val blY_Px: Float = 0f,

    @JsonNames("wPx", "w_px", "width", "w")
    val wPx: Float = 0f,

    @JsonNames("hPx", "h_px", "height", "h")
    val hPx: Float = 0f
)

@Serializable
data class Anchor(
    @JsonNames("number", "id", "anchor_number")
    val number: Int = 0,

    @JsonNames("xScenePx", "x_scene_px", "x_px", "x")
    val xScenePx: Float = 0f,

    @JsonNames("yScenePx", "y_scene_px", "y_px", "y")
    val yScenePx: Float = 0f,

    @JsonNames("zCm", "z_cm", "z", "height_cm")
    val zCm: Int = 0,

    @JsonNames("mainHall", "main_hall", "hall", "room")
    val mainHall: Int? = null,

    @JsonNames("extraHalls", "extra_halls", "halls")
    val extraHalls: List<Int> = emptyList(),

    @JsonNames("bound", "is_bound")
    val bound: Boolean = false
)

@Serializable
data class ProjectSave(
    // В десктопе может не быть image_data — оставим пустым
    @JsonNames("image_data", "imageData", "background_base64")
    val image_data: String = "",

    @JsonNames("pixel_per_cm_x", "ppcm_x", "pixelPerCmX", "pixel_per_cm")
    val pixel_per_cm_x: Float = 1f,

    @JsonNames("pixel_per_cm_y", "ppcm_y", "pixelPerCmY")
    val pixel_per_cm_y: Float = 1f,

    @JsonNames("grid_step_cm", "gridStepCm", "grid_step")
    val grid_step_cm: Float = 20f,

    @JsonNames("lock_halls") val lock_halls: Boolean = false,
    @JsonNames("lock_zones") val lock_zones: Boolean = false,
    @JsonNames("lock_anchors") val lock_anchors: Boolean = false,

    @JsonNames("halls")   val halls: List<Hall> = emptyList(),
    @JsonNames("anchors") val anchors: List<Anchor> = emptyList(),
    @JsonNames("zones")   val zones: List<Zone> = emptyList()
)

/* ---------- Экспорт в прошивку (как было) ---------- */

@Serializable data class ExportAnchor(val id: Int, val x: Float, val y: Float, val z: Float, val bound: Boolean? = null)
@Serializable data class ExportRect(val x: Float, val y: Float, val w: Float, val h: Float, val angle: Float)
@Serializable data class ExportZone(val num: Int, val enter: ExportRect, val exit: ExportRect, val bound: Boolean? = null)
@Serializable data class ExportRoom(val num: Int, val anchors: List<ExportAnchor>, val zones: List<ExportZone>)
@Serializable data class ExportConfig(val rooms: List<ExportRoom>)
