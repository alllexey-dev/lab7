package commands

import Response
import ServerContainer
import domain.Address

class CountLessThanOfficialAddress : Command {
    override val name = "count_less_than_official_address"
    override val args = listOf("Street", "Zip")
    override val description = "Подсчитывает количество организаций чей адрес меньше заданного"

    override fun execute(context: ServerContainer, args: List<String>): Response {
        val collectionManager = context.collectionManager

        val street = args[0]
        val zip = args[1]
        val address = Address(street, zip)

        val count = collectionManager.countLessAddress(address)

        return Response.Info("Количество организаций с меньшим адресом - $count")
    }
}