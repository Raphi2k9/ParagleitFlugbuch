package com.example.flugbuch.data.entities

enum class TrainingExercise(val displayName: String) {
    RUECKWAERTSAUFZIEHEN("Rückwärtsaufziehen"),
    GESCHWINDIGKEIT("Geschwindigkeit"),
    KURVENTECHNIK("Kurventechnik"),
    KURVE_180("Kurve 180 Grad"),
    VOLLKREIS("Vollkreis"),
    KURVEN_FLACH("Kurven flach"),
    KURVEN_MAESSIG("Kurven mäßig"),
    LANDEEINTEILUNG("Landeeinteilung"),
    LANDUNG_IN_MARKIERUNG("Landung in Markierung"),
    BESCHLEUNIGT_FLIEGEN("Beschleunigt Fliegen"),
    SEITLICHER_KLAPPER("Seitlicher Klapper 30%"),
    LEITLINIENACHT("Leitlinienacht (35 Sekunden)"),
    HANGACHT("Hangacht"),
    OHRENANLEGEN("Ohrenanlegen"),
    OHRENANLEGEN_BESCHLEUNIGT("Ohrenanlegen Beschleunigt"),
    NICKEN_STABILISIEREN("Nicken und Stabilisieren"),
    ROLLEN_STABILISIEREN("Rollen und Stabilisieren"),
    GROUNDHANDLEN("Groundhandlen");

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
