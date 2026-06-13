package com.example.flugbuch.igc

import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.*

// ---------------------------------------------------------------------------
// Model
// ---------------------------------------------------------------------------

data class Coords(val lat: Double, val lng: Double)

data class TrackPoint(
    val lat: Double,
    val lng: Double,
    val altBaro: Int,
    val altGPS: Int,
    val time: LocalTime,
)

data class IgcFlight(
    val date: LocalDate,
    val glider: String?,
    val pilot: String?,
    val durationSeconds: Long,
    val maxAltitudeMeters: Int,
    val launchCoords: Coords,
    val landingCoords: Coords,
    val trackDistanceKm: Double,
    val straightLineDistanceKm: Double,
    val trackPoints: List<TrackPoint>,
)

// ---------------------------------------------------------------------------
// Parser
// ---------------------------------------------------------------------------

/**
 * Parses the raw text content of an IGC flight-recorder file and returns an
 * [IgcFlight] with header metadata and derived track statistics.
 *
 * IGC format reference: https://www.fai.org/sites/default/files/igc_fr_spec_2020-11-25_with_addenda_1.pdf
 *
 * @throws IllegalArgumentException if a mandatory record (HFDTE, at least one
 *   valid B record) is missing or cannot be parsed.
 */
fun parseIgc(fileContent: String): IgcFlight {
    var date: LocalDate? = null
    var glider: String? = null
    var pilot: String? = null
    val trackPoints = mutableListOf<TrackPoint>()

    for (rawLine in fileContent.lineSequence()) {
        val line = rawLine.trimEnd()          // keep leading chars; only strip trailing CR/LF
        if (line.isEmpty()) continue

        when (line[0]) {
            'H' -> when {
                // ---- Date -------------------------------------------------------
                // HFDTE DDMMYY          (traditional)
                // HFDTEDATE:DDMMYY,FF   (post-2000 spec with flight number)
                line.length >= 5 && line.substring(1, 5).equals("FDTE", ignoreCase = true) -> {
                    if (date == null) date = parseDateRecord(line)
                }

                // ---- Glider type ------------------------------------------------
                // HFGTYGLIDERTYPE:Advance Omega XC3
                line.length > 16 && line.substring(1, 16).equals("FGTYGLIDERTYPE:", ignoreCase = true) -> {
                    glider = line.substring(16).trim().nullIfBlank()
                }

                // ---- Pilot ------------------------------------------------------
                // HFPLTPILOTINCHARGE:John Doe   (IGC spec)
                // HFPLTPILOT:John Doe            (some loggers omit "INCHARGE")
                line.length > 19 && line.substring(1, 19).equals("FPLTPILOTINCHARGE:", ignoreCase = true) -> {
                    pilot = line.substring(19).trim().nullIfBlank()
                }
                line.length > 11 && line.substring(1, 11).equals("FPLTPILOT:", ignoreCase = true) -> {
                    if (pilot == null) pilot = line.substring(11).trim().nullIfBlank()
                }
            }

            'B' -> {
                // B records are ≥35 chars when compact; some loggers add spaces.
                // Length check after stripping is done inside parseBRecord.
                parseBRecord(line)?.let { trackPoints.add(it) }
            }
        }
    }

    requireNotNull(date) { "No valid HFDTE date record found in IGC file" }
    require(trackPoints.isNotEmpty()) { "No valid (A-flag) B records found in IGC file" }

    val launch = Coords(trackPoints.first().lat, trackPoints.first().lng)
    val landing = Coords(trackPoints.last().lat, trackPoints.last().lng)

    // Logger ohne Barometer schreiben 0 ins Baro-Feld – dann GPS-Höhe nutzen
    val maxBaro = trackPoints.maxOf { it.altBaro }
    val maxAltitude = if (maxBaro > 0) maxBaro else trackPoints.maxOf { it.altGPS }

    return IgcFlight(
        date = date,
        glider = glider,
        pilot = pilot,
        durationSeconds = calculateDurationSeconds(trackPoints),
        maxAltitudeMeters = maxAltitude,
        launchCoords = launch,
        landingCoords = landing,
        trackDistanceKm = calculateTrackDistance(trackPoints),
        straightLineDistanceKm = haversineKm(launch, landing),
        trackPoints = trackPoints,
    )
}

// ---------------------------------------------------------------------------
// Record parsers
// ---------------------------------------------------------------------------

/**
 * Parses HFDTE records in both legacy and current formats:
 *   HFDTEDDMMYY
 *   HFDTE DDMMYY        (space variant)
 *   HFDTEDATE:DDMMYY,FF
 */
private fun parseDateRecord(line: String): LocalDate? = runCatching {
    // Locate the 6-digit DDMMYY block
    val sixDigits: String = if (line.contains("DATE:", ignoreCase = true)) {
        line.substringAfter(":", "")
            .substringBefore(",")
            .trim()
    } else {
        // Skip "HFDTE" (5 chars) and any optional space
        line.drop(5).trimStart().take(6)
    }
    if (sixDigits.length < 6 || !sixDigits.all { it.isDigit() }) return null

    val day   = sixDigits.substring(0, 2).toInt()
    val month = sixDigits.substring(2, 4).toInt()
    val yy    = sixDigits.substring(4, 6).toInt()
    // IGC epoch: yy ≥ 70 → 19xx, yy < 70 → 20xx
    val year  = if (yy >= 70) 1900 + yy else 2000 + yy

    LocalDate.of(year, month, day)
}.getOrNull()

/**
 * Parses a single B record and returns a [TrackPoint], or `null` when the
 * record is marked invalid (V-flag) or cannot be decoded.
 */
private fun parseBRecord(rawLine: String): TrackPoint? = runCatching {
    val line = rawLine.replace(" ", "")   // some loggers insert spaces between fields
    if (line.length < 35) return null
    val validity = line[24]
    if (validity != 'A') return null   // skip V (invalid) fixes

    val hh = line.substring(1, 3).toInt()
    val mm = line.substring(3, 5).toInt()
    val ss = line.substring(5, 7).toInt()

    val lat     = parseLatitude(line.substring(7, 15))
    val lng     = parseLongitude(line.substring(15, 24))
    val altBaro = line.substring(25, 30).toInt()
    val altGPS  = line.substring(30, 35).toInt()

    TrackPoint(lat, lng, altBaro, altGPS, LocalTime.of(hh, mm, ss))
}.getOrNull()

// ---------------------------------------------------------------------------
// Coordinate helpers
// ---------------------------------------------------------------------------

/**
 * Converts IGC latitude field `DDMMmmmN` (8 chars) to decimal degrees.
 *
 * DD  = integer degrees (2 digits)
 * MM  = integer minutes (2 digits)
 * mmm = decimal minutes in thousandths (3 digits), i.e. full minutes = MM.mmm
 * N/S = hemisphere
 */
private fun parseLatitude(s: String): Double {
    require(s.length == 8)
    val deg      = s.substring(0, 2).toDouble()
    val minInt   = s.substring(2, 4).toDouble()
    val minDec   = s.substring(4, 7).toDouble()          // thousandths of a minute
    val decimal  = deg + (minInt + minDec / 1000.0) / 60.0
    return if (s[7] == 'S') -decimal else decimal
}

/**
 * Converts IGC longitude field `DDDMMmmmE` (9 chars) to decimal degrees.
 *
 * DDD = integer degrees (3 digits)
 * MM  = integer minutes (2 digits)
 * mmm = decimal minutes in thousandths (3 digits)
 * E/W = hemisphere
 */
private fun parseLongitude(s: String): Double {
    require(s.length == 9)
    val deg      = s.substring(0, 3).toDouble()
    val minInt   = s.substring(3, 5).toDouble()
    val minDec   = s.substring(5, 8).toDouble()
    val decimal  = deg + (minInt + minDec / 1000.0) / 60.0
    return if (s[8] == 'W') -decimal else decimal
}

// ---------------------------------------------------------------------------
// Distance & duration calculations
// ---------------------------------------------------------------------------

/**
 * Calculates flight duration in seconds from the first to the last track point.
 * Handles flights that cross midnight UTC by adding 86 400 s when the raw
 * difference is negative.
 */
private fun calculateDurationSeconds(points: List<TrackPoint>): Long {
    val startSec = points.first().time.toSecondOfDay().toLong()
    val endSec   = points.last().time.toSecondOfDay().toLong()
    val diff     = endSec - startSec
    return if (diff >= 0) diff else diff + 86_400L
}

/** Sum of haversine distances between every pair of consecutive track points. */
private fun calculateTrackDistance(points: List<TrackPoint>): Double {
    var totalKm = 0.0
    for (i in 1 until points.size) {
        totalKm += haversineKm(
            Coords(points[i - 1].lat, points[i - 1].lng),
            Coords(points[i].lat,     points[i].lng),
        )
    }
    return totalKm
}

/**
 * Haversine great-circle distance between two coordinate pairs, in kilometres.
 *
 * Uses the mean Earth radius of 6 371 km (WGS-84 approximation sufficient for
 * short-range paragliding tracks).
 */
private fun haversineKm(a: Coords, b: Coords): Double {
    val R    = 6_371.0
    val dLat = (b.lat - a.lat).toRadians()
    val dLng = (b.lng - a.lng).toRadians()
    val lat1 = a.lat.toRadians()
    val lat2 = b.lat.toRadians()
    val h    = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
    return 2 * R * asin(sqrt(h))
}

// ---------------------------------------------------------------------------
// Extensions
// ---------------------------------------------------------------------------

private fun Double.toRadians(): Double = Math.toRadians(this)
private fun String.nullIfBlank(): String? = ifBlank { null }
