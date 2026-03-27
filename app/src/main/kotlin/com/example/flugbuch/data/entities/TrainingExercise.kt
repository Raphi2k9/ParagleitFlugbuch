package com.example.flugbuch.data.entities

import androidx.annotation.StringRes
import com.example.flugbuch.R

enum class TrainingExercise(
    val displayName: String,   // German name kept for compatibility
    @StringRes val labelRes: Int
) {
    RUECKWAERTSAUFZIEHEN("Rückwärtsaufziehen", R.string.exercise_reverse_launch),
    GESCHWINDIGKEIT("Geschwindigkeit", R.string.exercise_speed),
    KURVENTECHNIK("Kurventechnik", R.string.exercise_turn_technique),
    KURVE_180("Kurve 180 Grad", R.string.exercise_turn_180),
    VOLLKREIS("Vollkreis", R.string.exercise_full_circle),
    KURVEN_FLACH("Kurven flach", R.string.exercise_flat_turns),
    KURVEN_MAESSIG("Kurven mäßig", R.string.exercise_moderate_turns),
    LANDEEINTEILUNG("Landeeinteilung", R.string.exercise_landing_approach),
    LANDUNG_IN_MARKIERUNG("Landung in Markierung", R.string.exercise_precision_landing),
    BESCHLEUNIGT_FLIEGEN("Beschleunigt Fliegen", R.string.exercise_accelerated),
    SEITLICHER_KLAPPER("Seitlicher Klapper 30%", R.string.exercise_side_tuck),
    LEITLINIENACHT("Leitlinienacht (35 Sekunden)", R.string.exercise_figure8_brakes),
    HANGACHT("Hangacht", R.string.exercise_slope_eight),
    OHRENANLEGEN("Ohrenanlegen", R.string.exercise_ear_folding),
    OHRENANLEGEN_BESCHLEUNIGT("Ohrenanlegen Beschleunigt", R.string.exercise_ear_folding_acc),
    NICKEN_STABILISIEREN("Nicken und Stabilisieren", R.string.exercise_pitch_stabilize),
    ROLLEN_STABILISIEREN("Rollen und Stabilisieren", R.string.exercise_roll_stabilize),
    GROUNDHANDLEN("Groundhandlen", R.string.exercise_groundhandling);

    companion object {
        fun fromStorageString(value: String?): Set<TrainingExercise> {
            if (value.isNullOrBlank()) return emptySet()
            return value.split(",")
                .mapNotNull { name -> entries.find { it.name == name.trim() } }
                .toSet()
        }

        fun toStorageString(exercises: Set<TrainingExercise>): String =
            exercises.joinToString(",") { it.name }
    }
}
