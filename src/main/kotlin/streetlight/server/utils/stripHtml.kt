package streetlight.server.utils

fun String.stripHtml(): String {
    val sb = StringBuilder(length)
    var inTag = false
    var inEntity = false
    val entity = StringBuilder()

    for (char in this) {
        when {
            char == '<' -> inTag = true
            char == '>' -> inTag = false
            inTag -> {}
            char == '&' -> {
                inEntity = true
                entity.clear()
            }
            inEntity && char == ';' -> {
                inEntity = false
                sb.append(decodeEntity(entity.toString()))
            }
            inEntity -> entity.append(char)
            else -> sb.append(char)
        }
    }

    // Unterminated entity — flush raw
    if (inEntity) {
        sb.append('&')
        sb.append(entity)
    }

    return sb.toString()
}

private fun decodeEntity(entity: String): String = when {
    entity.startsWith("#x", ignoreCase = true) ->
        entity.substring(2).toIntOrNull(16)
            ?.let { String(intArrayOf(it), 0, 1) } ?: "&$entity;"
    entity.startsWith("#") ->
        entity.substring(1).toIntOrNull()
            ?.let { String(intArrayOf(it), 0, 1) } ?: "&$entity;"
    else -> NAMED_ENTITIES[entity] ?: "&$entity;"
}

private val NAMED_ENTITIES = mapOf(
    "amp" to "&",
    "lt" to "<",
    "gt" to ">",
    "quot" to "\"",
    "apos" to "'",
    "nbsp" to "\u00A0",
    "ndash" to "\u2013",
    "mdash" to "\u2014",
    "laquo" to "\u00AB",
    "raquo" to "\u00BB",
    "copy" to "\u00A9",
    "reg" to "\u00AE",
    "trade" to "\u2122",
    "hellip" to "\u2026",
)