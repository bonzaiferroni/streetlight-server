package streetlight.server.utils

fun printToFile(content: String, filename: String) {
    val file = java.io.File(filename)
    file.writeText(content)
}