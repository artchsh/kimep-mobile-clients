package kz.kimep.mobile.ui

import androidx.compose.ui.graphics.Color

/** "F2026" -> "Fall 2026", "SU1/2026" -> "Summer 2026", "S2026" -> "Spring 2026". */
fun semesterLabel(code: String): String {
    val trimmed = code.trim()
    if (trimmed.isEmpty()) return code
    val prefix = trimmed.takeWhile { it.isLetter() }
    val year = Regex("\\d{4}").find(trimmed)?.value ?: trimmed.filter { it.isDigit() }
    val name = when {
        prefix.startsWith("SU", ignoreCase = true) -> "Summer"
        prefix.startsWith("F", ignoreCase = true) -> "Fall"
        prefix.startsWith("S", ignoreCase = true) -> "Spring"
        else -> prefix
    }
    return if (year.isNotEmpty()) "$name $year" else trimmed
}

/** Returns (background, foreground) for a letter grade badge. */
fun gradeColors(grade: String): Pair<Color, Color> {
    val value = grade.trim().uppercase()
    return when (value.firstOrNull()) {
        'A' -> Color(0xFFD3F2DA) to Color(0xFF11542B)
        'B' -> Color(0xFFD6E4FF) to Color(0xFF123E8C)
        'C' -> Color(0xFFFFEAC2) to Color(0xFF7A4E00)
        'D' -> Color(0xFFFFDCC7) to Color(0xFF8F3D00)
        'F' -> Color(0xFFFFDAD6) to Color(0xFFB3261E)
        else -> Color(0xFFE1E2EC) to Color(0xFF3A3D45)
    }
}
