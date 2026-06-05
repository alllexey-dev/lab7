package domain

import java.time.LocalDate

data class Organization (
    val id: Int,
    var name: String,
    var coordinates: Coordinates,
    var creationDate: LocalDate,
    var annualTurnover: Float,
    var fullName: String,
    var employeesCount: Long?,
    var type: OrganizationType,
    var officialAddress: Address
) : Comparable<Organization> {
    init {
        require(name != "") { "Строка не может быть пустой" }
        require(annualTurnover > 0) { "Значение поля annualTurnover должно быть больше 0" }
        val employeesCount = employeesCount
        if (employeesCount != null ) require(employeesCount > 0) { "Значение поля employeesCount должно быть больше 0" }
    }
    override fun compareTo(other: Organization): Int {
        return this.name.compareTo(other.name)
    }

    override fun toString(): String {

        return "Организация '$name': Id: $id, тип: $type, адрес: $officialAddress, координаты: $coordinates, дата создания: $creationDate, годичная выручка: $annualTurnover, полное название: $fullName, количество сотрудников: $employeesCount,"
    }
}