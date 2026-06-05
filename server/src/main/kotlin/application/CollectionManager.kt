package application

import data.OrganizationTransferData
import domain.Address
import domain.Organization
import domain.OrganizationRepository
import domain.OrganizationType
import java.time.LocalDate

class CollectionManager : OrganizationRepository {

    private var organizationCollection: ArrayDeque<Organization> = ArrayDeque()
    private var initDate = LocalDate.now()

    fun checkFullNameUnique(fullName: String): Boolean = organizationCollection.any { it.fullName == fullName }


    fun uploadCollection(collection: List<Organization>){
        organizationCollection = ArrayDeque(collection)
    }

    fun getCollection(): List<Organization> = organizationCollection.sortedWith(compareBy { it.name }).toList()

    fun countType(type: OrganizationType): Int = organizationCollection.count { it.type == type }

    fun sumEmployees(): Long = organizationCollection.sumOf { it.employeesCount ?: 0L }

    fun countLessAddress(address: Address): Int = organizationCollection.count { it.officialAddress < address }

    fun countGreater(organization: Organization) = organizationCollection.count { it > organization }

    fun countLower(organization: Organization) = organizationCollection.count { it < organization }

    fun getInitializationDate(): String {
        return if (organizationCollection.isEmpty()) "Коллекция еще не создана"
        else initDate.toString()
    }

    override fun add(organization: Organization){
        if (organizationCollection.isEmpty()) initDate = LocalDate.now()
        if (!checkFullNameUnique(organization.fullName)) {
            organizationCollection.addLast(organization)
        } else throw IllegalArgumentException("Полное имя организации не уникально.")
    }

    @Deprecated("Id is redundant here, use add(Organization) instead)")
    override fun add(organization: Organization, id: Int) {
        if (organizationCollection.isEmpty()) initDate = LocalDate.now()
        if (!checkFullNameUnique(organization.fullName)) {
            organizationCollection.addLast(organization)
        } else throw IllegalArgumentException("Полное имя организации не уникально.")
    }

    override fun updateById(id: Int, organization: OrganizationTransferData) {
        organizationCollection.find { it.id == id }?.let {
            it.type = organization.type
            it.fullName = organization.fullName
            it.employeesCount = organization.employeesCount
            it.officialAddress = organization.officialAddress
            it.coordinates = organization.coordinates
            it.name = organization.name
            it.annualTurnover = organization.annualTurnover
        }

    }

    override fun removeById(id: Int) {
        organizationCollection.removeIf { organization -> organization.id == id }
    }

}