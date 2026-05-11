package application

import domain.Address
import domain.Organization
import domain.OrganizationRepository
import domain.OrganizationType
import java.time.LocalDate

class CollectionManager(
    private val organizationCollection : ArrayDeque<Organization>,
) : OrganizationRepository {

    private var currentID: Int = if (organizationCollection.isNotEmpty()) organizationCollection.maxOf { it.id } else 0
    private var initDate = LocalDate.now()

    fun generateNewID() : Int{
        return ++currentID
    }

    fun checkID(id: Int) : Boolean {
        return organizationCollection.find {it.id == id } == null
    }

    fun checkFullNameUnique(fullName: String) : Boolean = organizationCollection.any { it.fullName == fullName }

    fun clear() {
        organizationCollection.clear()
        currentID = 0
    }
    fun getCollection() : List<Organization> = organizationCollection.sortedWith(compareBy { it.name }).toList()

    fun countType(type: OrganizationType) : Int = organizationCollection.count {it.type == type}

    fun sumEmployees(): Long = organizationCollection.sumOf { it.employeesCount ?: 0L }

    fun countLessAddress(address: Address): Int = organizationCollection.count { it.officialAddress < address }

    fun removeGreater(organization: Organization) = organizationCollection.removeIf { it > organization}

    fun countGreater(organization: Organization) = organizationCollection.count { it > organization}

    fun removeLower(organization: Organization) = organizationCollection.removeIf { it < organization}

    fun countLower(organization: Organization) = organizationCollection.count { it < organization}

    fun getInitializationDate(): String{
        return if (organizationCollection.isEmpty()) "Коллекция еще не создана"
        else initDate.toString()
    }

    override fun add(organization: Organization) {
        if (organizationCollection.isEmpty()) initDate = LocalDate.now()
        if (!checkFullNameUnique(organization.fullName)) {
            organizationCollection.addLast(organization)
        } else throw IllegalArgumentException("Полное имя организации не уникально.")
    }

    override fun updateById(id: Int, organization: Organization) {
        if (!checkID(id)){
            organizationCollection.removeIf { it.id == id }
            val generatedOrg = Organization(
                id = id,
                name = organization.name,
                coordinates = organization.coordinates,
                creationDate = organization.creationDate,
                annualTurnover = organization.annualTurnover,
                fullName = organization.fullName,
                employeesCount = organization.employeesCount,
                type = organization.type,
                officialAddress = organization.officialAddress
            )
            organizationCollection.addLast(generatedOrg)
        }
    }

    override fun removeById(id: Int) {
        organizationCollection.removeIf{ organization -> organization.id == id }
    }

}