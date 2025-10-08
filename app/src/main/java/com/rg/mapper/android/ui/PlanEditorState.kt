package com.rg.mapper.android.ui

import android.graphics.Bitmap
import androidx.compose.runtime.*
import com.rg.mapper.android.model.*
import kotlin.math.*

enum class ToolMode { Cursor, Calibrate }

data class Selection(
    val hallNumber: Int? = null,
    val zoneKey: Pair<Int, Int>? = null,
    val anchorNumber: Int? = null
)

class PlanEditorState {
    var background by mutableStateOf<Bitmap?>(null)
    var bgWidthPx by mutableStateOf(0)
    var bgHeightPx by mutableStateOf(0)

    var pixelPerCm by mutableStateOf(1f)
    var gridStepCm by mutableStateOf(20f)
    var gridCalibrated by mutableStateOf(false)

    var halls = mutableStateListOf<Hall>()
    var zones = mutableStateListOf<Zone>()
    var anchors = mutableStateListOf<Anchor>()

    var lockHalls by mutableStateOf(false)
    var lockZones by mutableStateOf(false)
    var lockAnchors by mutableStateOf(false)

    var mode by mutableStateOf(ToolMode.Cursor)
    var selection by mutableStateOf(Selection())

    var tempPointA by mutableStateOf<androidx.compose.ui.geometry.Offset?>(null)

    fun clearAll() {
        halls.clear(); zones.clear(); anchors.clear()
        selection = Selection(); tempPointA = null
    }

    fun hallAt(scene: androidx.compose.ui.geometry.Offset): Hall? =
        halls.lastOrNull { scene.x >= it.xPx && scene.x <= it.xPx + it.wPx && scene.y >= it.yPx && scene.y <= it.yPx + it.hPx }

    fun anchorAt(scene: androidx.compose.ui.geometry.Offset, radiusPx: Float = 12f): Anchor? =
        anchors.lastOrNull { hypot(scene.x - it.xScenePx, scene.y - it.yScenePx) <= radiusPx }

    fun zonesInHall(hn: Int) = zones.filter { it.hallNumber == hn }

    fun zoneAt(scene: androidx.compose.ui.geometry.Offset): Zone? {
        val zcandidates = zones.mapNotNull { z ->
            val hall = halls.find { it.number == z.hallNumber } ?: return@mapNotNull null
            if (!pointInsideZone(scene, z, hall)) null else z
        }
        return zcandidates.lastOrNull()
    }

    private fun pointInsideZone(p: androidx.compose.ui.geometry.Offset, z: Zone, h: Hall): Boolean {
        val localX = p.x - h.xPx
        val localY = p.y - h.yPx
        val ox = localX - z.blX_Px
        val oy = (h.hPx - localY) - z.blY_Px
        val ang = Math.toRadians(z.angleDeg.toDouble())
        val cos = cos(ang).toFloat(); val sin = sin(ang).toFloat()
        val rx =  cos * ox + sin * oy
        val ry = -sin * ox + cos * oy
        return rx >= 0 && rx <= z.wPx && ry >= 0 && ry <= z.hPx
    }

    fun exportRooms(): ExportConfig {
        val ppcm = pixelPerCm
        fun round1(v: Float) = (kotlin.math.round(v * 10f) / 10f)

        val rooms = halls.map { h ->
            val anchorsForHall = anchors.filter { a -> (a.mainHall == h.number) || (h.number in a.extraHalls) }
            val exportAnchors = anchorsForHall.map { a ->
                val localX = a.xScenePx - h.xPx
                val localY = a.yScenePx - h.yPx
                val xm = (localX) / (ppcm * 100f)
                val ym = (h.hPx - localY) / (ppcm * 100f)
                val zM = (a.zCm / 100f)
                ExportAnchor(
                    id = a.number,
                    x = round1(xm),
                    y = round1(ym),
                    z = round1(zM),
                    bound = if (a.bound) true else null
                )
            }

            val zonesForHall = zones.filter { it.hallNumber == h.number }
            val byNum = zonesForHall.groupBy { it.zoneNum }
            val exportZones = byNum.map { (num, list) ->
                var enter = ExportRect(0f, 0f, 0f, 0f, 0f)
                var exit  = ExportRect(0f, 0f, 0f, 0f, 0f)
                var bound: Boolean? = null
                list.forEach { z ->
                    val rect = ExportRect(
                        x = round1(z.blX_Px / (ppcm * 100f)),
                        y = round1((h.hPx - z.blY_Px) / (ppcm * 100f)),
                        w = round1(z.wPx / (ppcm * 100f)),
                        h = round1(z.hPx / (ppcm * 100f)),
                        angle = z.angleDeg.toFloat()
                    )
                    when (z.type) {
                        ZoneType.ENTER -> enter = rect
                        ZoneType.EXIT  -> exit = rect
                        ZoneType.BOUND -> { enter = rect; bound = true }
                    }
                }
                ExportZone(num = num, enter = enter, exit = exit, bound = bound)
            }

            ExportRoom(
                num = h.number,
                anchors = exportAnchors,
                zones = exportZones
            )
        }
        return ExportConfig(rooms)
    }
}
