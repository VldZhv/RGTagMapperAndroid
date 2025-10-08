package com.rg.mapper.android.util

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.rg.mapper.android.ui.PlanEditorState

fun exportToPdf(ctx: Context, state: PlanEditorState, uri: Uri) {
    val doc = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(1240, 1754, 1).create()
    val page = doc.startPage(pageInfo)
    val c = page.canvas
    c.drawRGB(255,255,255)
    c.drawText("RG Tag Mapper — PDF экспорт (MVP)", 60f, 60f, android.graphics.Paint().apply { textSize = 26f })
    doc.finishPage(page)
    ctx.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
    doc.close()
}
