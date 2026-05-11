package util

import java.io.File

class PropertiesParser {
    companion object {
        fun getPropertiesFromFile(path: String): Map<String, String> {
            val propertiesFile = File(path)
            println(propertiesFile.absolutePath)

            if (!propertiesFile.exists()) throw Error("env file should be specified")

            val map = propertiesFile
                .readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { it.split("=") }
                .filter { it.size == 2 }
                .associate { (k, v) -> k to v }

            return map
        }
    }
}