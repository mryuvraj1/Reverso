package dev.reverso

internal data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val prerelease: String? = null,
)

internal fun parseSemanticVersion(value: String): SemanticVersion? {
    val match = Regex("^[\\s\\-vV]*(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?$")
        .matchEntire(value.trim()) ?: return null
    return SemanticVersion(
        match.groupValues[1].toIntOrNull() ?: return null,
        match.groupValues[2].toIntOrNull() ?: return null,
        match.groupValues[3].toIntOrNull() ?: return null,
        match.groupValues[4].ifEmpty { null },
    )
}

internal fun compareSemanticVersions(candidate: String, installed: String): Int {
    val left = parseSemanticVersion(candidate) ?: return 0
    val right = parseSemanticVersion(installed) ?: return 0
    compareValuesBy(left, right, SemanticVersion::major, SemanticVersion::minor, SemanticVersion::patch)
        .takeIf { it != 0 }?.let { return it }
    if (left.prerelease == null) return if (right.prerelease == null) 0 else 1
    if (right.prerelease == null) return -1

    val leftParts = left.prerelease.split('.')
    val rightParts = right.prerelease.split('.')
    for (index in 0 until maxOf(leftParts.size, rightParts.size)) {
        if (index >= leftParts.size) return -1
        if (index >= rightParts.size) return 1
        val a = leftParts[index]
        val b = rightParts[index]
        val aNumber = a.toIntOrNull()
        val bNumber = b.toIntOrNull()
        val compared = when {
            aNumber != null && bNumber != null -> aNumber.compareTo(bNumber)
            aNumber != null -> -1
            bNumber != null -> 1
            else -> a.compareTo(b)
        }
        if (compared != 0) return compared
    }
    return 0
}
