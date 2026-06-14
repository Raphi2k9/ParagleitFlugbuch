package com.example.flugbuch.export

import android.content.Context
import com.example.flugbuch.data.entities.FlightType
import com.example.flugbuch.data.model.FlightModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.floor

/**
 * Schreibt die Flüge eines Schülers als echte .xlsx-Datei – ganz ohne externe
 * Bibliothek. Eine xlsx ist ein ZIP aus OOXML-Teilen; wir erzeugen das nötige
 * Minimum (ein Arbeitsblatt mit Inline-Strings). Öffnet sich direkt in Excel,
 * LibreOffice und Google Sheets.
 */
object FlightExcelExporter {

    private val headers = listOf(
        "Datum", "Gleitschirm", "Dauer (min)", "Flugtyp", "Startplatz", "Landeplatz",
        "Max. Höhe (m)", "Distanz (km)", "Wind", "Bewölkung", "Temp (°C)",
        "Prüfung bestanden", "Unterschrieben von", "Unterschrieben am", "Notizen"
    )

    fun export(context: Context, studentName: String, flights: List<FlightModel>): File {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val safeName = studentName.ifBlank { "Schueler" }.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(dir, "Flugbuch_${safeName}_${System.currentTimeMillis()}.xlsx")
        val dateFmt = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)

        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            zip.writeEntry("[Content_Types].xml", CONTENT_TYPES)
            zip.writeEntry("_rels/.rels", ROOT_RELS)
            zip.writeEntry("xl/workbook.xml", WORKBOOK)
            zip.writeEntry("xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            zip.writeEntry("xl/worksheets/sheet1.xml", sheetXml(flights, dateFmt))
        }
        return file
    }

    // ---- Zeilen/Zellen ---------------------------------------------------------

    private sealed interface Cell
    private data class TextCell(val value: String) : Cell
    private data class NumCell(val value: Double) : Cell

    private fun sheetXml(flights: List<FlightModel>, dateFmt: SimpleDateFormat): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        sb.append(rowXml(1, headers.map { TextCell(it) }))
        flights.forEachIndexed { idx, f ->
            sb.append(rowXml(idx + 2, listOf(
                TextCell(dateFmt.format(Date(f.date))),
                TextCell(f.gliderName),
                NumCell(f.durationMinutes.toDouble()),
                TextCell(flightTypeLabel(f.flightType)),
                TextCell(f.startLocation),
                TextCell(f.landingLocation),
                f.maxAltitude?.let { NumCell(it.toDouble()) } ?: TextCell(""),
                f.distance?.let { NumCell(it) } ?: TextCell(""),
                TextCell(f.windConditions),
                TextCell(f.cloudCover),
                f.temperature?.let { NumCell(it.toDouble()) } ?: TextCell(""),
                TextCell(pruefungLabel(f.pruefungBestanden)),
                TextCell(f.approvedByName ?: ""),
                TextCell(formatSignDate(f.approvedAt)),
                TextCell(oneLine(f.notes))
            )))
        }
        sb.append("""</sheetData></worksheet>""")
        return sb.toString()
    }

    private fun rowXml(rowNum: Int, cells: List<Cell>): String {
        val sb = StringBuilder()
        sb.append("""<row r="$rowNum">""")
        cells.forEachIndexed { i, cell ->
            val ref = colLetter(i) + rowNum
            when (cell) {
                is NumCell -> sb.append("""<c r="$ref"><v>${numStr(cell.value)}</v></c>""")
                is TextCell ->
                    if (cell.value.isEmpty()) sb.append("""<c r="$ref"/>""")
                    else sb.append("""<c r="$ref" t="inlineStr"><is><t xml:space="preserve">${xmlEscape(cell.value)}</t></is></c>""")
            }
        }
        sb.append("""</row>""")
        return sb.toString()
    }

    // ---- Hilfen ----------------------------------------------------------------

    private fun ZipOutputStream.writeEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun flightTypeLabel(typeName: String): String =
        FlightType.entries.find { it.name == typeName }?.displayName ?: typeName

    private fun pruefungLabel(passed: Boolean?): String = when (passed) {
        true -> "Ja"
        false -> "Nein"
        null -> ""
    }

    // ISO-8601 -> dd.MM.yyyy (Datumsteil steht immer am Anfang)
    private fun formatSignDate(iso: String?): String {
        if (iso.isNullOrBlank() || iso.length < 10) return ""
        val parts = iso.substring(0, 10).split("-")
        return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else ""
    }

    private fun oneLine(text: String): String =
        text.replace("\r", " ").replace("\n", " ").trim()

    // <v> braucht Punkt als Dezimaltrennzeichen, unabhängig von der Locale
    private fun numStr(v: Double): String =
        if (v == floor(v) && !v.isInfinite()) v.toLong().toString() else v.toString()

    private fun colLetter(index: Int): String {
        var i = index
        val sb = StringBuilder()
        while (i >= 0) {
            sb.insert(0, ('A' + (i % 26)))
            i = i / 26 - 1
        }
        return sb.toString()
    }

    private fun xmlEscape(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) {
            when (c) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                // Steuerzeichen sind in XML 1.0 unzulässig -> entfernen
                else -> if (c.code >= 0x20 || c == '\t') sb.append(c)
            }
        }
        return sb.toString()
    }

    // ---- statische OOXML-Teile -------------------------------------------------

    private const val CONTENT_TYPES =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""" +
        """<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""" +
        """<Default Extension="xml" ContentType="application/xml"/>""" +
        """<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""" +
        """<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" +
        """</Types>"""

    private const val ROOT_RELS =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
        """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""" +
        """</Relationships>"""

    private const val WORKBOOK =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""" +
        """<sheets><sheet name="Flüge" sheetId="1" r:id="rId1"/></sheets>""" +
        """</workbook>"""

    private const val WORKBOOK_RELS =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
        """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>""" +
        """</Relationships>"""
}
