package domain

import data.OrganizationTransferData

interface OrganizationRepository {
    @Deprecated("Id is redundant here, use add(org) instead)")
    fun add(organization: Organization, id: Int)
    fun add(organization: Organization)
    fun updateById(id: Int, organization: OrganizationTransferData)
    fun removeById(id: Int)
}