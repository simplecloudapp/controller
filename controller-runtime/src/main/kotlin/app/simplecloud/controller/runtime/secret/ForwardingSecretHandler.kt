package app.simplecloud.controller.runtime.secret

import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class ForwardingSecretHandler(
    path: Path
) {
    private val secret: String
    init {
        val secretFile = path.toFile()
        if(!secretFile.exists()) {
            Files.createDirectories(secretFile.parentFile.toPath())
            Files.createFile(secretFile.toPath())
            val writer = FileWriter(secretFile)
            writer.write(UUID.randomUUID().toString())
            writer.close()
        }
        val reader = FileReader(secretFile)
        secret = reader.readText()
        reader.close()
    }
    fun getSecret(): String {
        return secret
    }
}