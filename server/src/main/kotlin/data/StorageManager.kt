package data

import ServerContainer
import domain.Organization
import nl.adaptivity.xmlutil.serialization.XML
import java.io.*
import java.util.*
import kotlin.collections.ArrayDeque

class StorageManager(
    val container: ServerContainer,
    val defaultPath: String = ""
) {
    fun downloadCollection(fileName: String = defaultPath): ArrayDeque<Organization> {
        if (fileName.isEmpty()) return ArrayDeque()
        val file = File(fileName)
        if (!file.exists()) {
            println("Файл '$fileName' не найден, коллекция организация не была подгружена :P"); return ArrayDeque()
        }
        val sc = Scanner(file)

        val sb = StringBuilder()



        while (sc.hasNextLine()) {
            sb.append(sc.nextLine())
        }

        val res = sb.toString()

        val decode = XML.decodeFromString(OrganizationsContainer.serializer(), res)
        val collection = decode
            .organizations
            .map(OrganizationDTO::toDomain)
            .toList()



        return ArrayDeque(collection)
    }

    fun uploadCollection(collection: ArrayDeque<Organization> = ArrayDeque(container.collectionManager.getCollection()), fileName: String = defaultPath) {
        var file: File
        file = if (fileName.isBlank()) File(defaultPath)
        else File(fileName)
        val list = collection
            .map(OrganizationDTO::toDto)
            .toList()
        val container = OrganizationsContainer(list)
        val xml = XML { indentString = "    " }
        val content = xml.encodeToString(OrganizationsContainer.serializer(), container)
        val fileWriter = FileWriter(file)

        fileWriter.write(content)
        println("Коллекция успешно сохранена в файл $fileName")
        fileWriter.close()
    }
}