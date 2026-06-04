package application


import data.OrganizationTransferData
import domain.Address
import domain.Coordinates
import domain.Organization
import domain.OrganizationType
import java.time.LocalDate

fun buildOrganization(data: List<String>): OrganizationTransferData {
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
    return OrganizationTransferData(
        name,
        Coordinates(x, y),
        LocalDate.now(),
        turnover, fullName,
        empCount,
        orgType,
        Address(street, zip)
    )
}

fun convertOrganizationFromTransferData(id: Int, data: OrganizationTransferData): Organization {
    return Organization(
        id,
        data.name,
        data.coordinates,
        data.creationDate,
        data.annualTurnover,
        data.fullName,
        data.employeesCount,
        data.type,
        data.officialAddress
    )
}
