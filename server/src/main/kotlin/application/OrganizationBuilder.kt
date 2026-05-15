package application


import domain.Address
import domain.Coordinates
import domain.Organization
import domain.OrganizationType
import java.time.LocalDate

fun buildOrganization(data: List<String>): Organization {
    val name = data[0].trim()
    val x = data[1].trim().toFloat()
    val y = data[2].trim().toFloat()
    val turnover = data[3].trim().toFloat()
    val fullName = data[4].trim()
    val empCount = data[5].trim().toLongOrNull()
    val street = data[6].trim()
    val zip = data[7].trim()
    val type = data[8].trim().lowercase()
    val orgType = when (type) {
        "commercial" -> OrganizationType.COMMERCIAL
        "public" -> OrganizationType.PUBLIC
        "government" -> OrganizationType.GOVERNMENT
        "private limited company" -> OrganizationType.PRIVATE_LIMITED_COMPANY
        "open joint stock company" -> OrganizationType.OPEN_JOINT_STOCK_COMPANY
        else -> {
            throw IllegalStateException("Введён некоректный формат типа организации")
        }
    }
    val addr = Address(street ?: "", zip ?: "")
    val coordinates = Coordinates(x, y)
    return Organization(0, name, coordinates, LocalDate.now(), turnover, fullName, empCount,orgType, addr)

}
