package com.faizilham.prototype.versioning

class VersionNumber(versionString: String) : Comparable<VersionNumber> {
    private val parts : List<Int>

    init {
        val parsed = versionString.split('.').map {
            val num = it.toInt()
            if (num < 0) throw NumberFormatException()

            num
        }

        var canonicalLength = parsed.size
        for (i in (canonicalLength - 1) downTo 1) {
            if (parsed[i] > 0) break
            canonicalLength--
        }

        parts = parsed.take(canonicalLength)
    }

    override fun compareTo(other: VersionNumber): Int {
        parts.zip(other.parts).forEach { (a, b) ->
            if (a != b) {
                return a.compareTo(b)
            }
        }

        return parts.size.compareTo(other.parts.size)
    }

    override fun toString() = parts.joinToString(".")
}