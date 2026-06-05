package data

import domain.Address
import domain.Coordinates
import domain.OrganizationType
import java.time.LocalDate

data class OrganizationTransferData(
    val name: String,
    val coordinates: Coordinates,
    val creationDate: LocalDate,
    val annualTurnover: Float,
    val fullName: String,
    val employeesCount: Long?,
    val type: OrganizationType,
    val officialAddress: Address
)
