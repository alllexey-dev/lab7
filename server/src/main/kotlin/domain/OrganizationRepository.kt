package domain

interface OrganizationRepository {
    fun add(organization: Organization, id: Int)
    fun updateById(id: Int, organization: Organization)
    fun removeById(id: Int)
}