package commands

import Response
import ServerContainer
import domain.OrganizationType

class CountByType(
) : Command {
    override val name = "count_by_type"
    override val args = listOf("Type")
    override val description = "Подсчитывает количество организаций заданного типа"

    override fun execute(context: ServerContainer, args: List<String>): Response {
        val neatArgument = args[0].uppercase().trim().replace(" ", "_")
        val waitIsItTrue = OrganizationType.entries.any { it.toString() == neatArgument }
        val collectionManager = context.collectionManager

        if (waitIsItTrue) {
            val count = collectionManager.countType(OrganizationType.valueOf(neatArgument))

            return Response.Info("Количество организаций такого типа: $count")
        } else {
            val neatOrganizationTypes = OrganizationType
                .entries
                .toString()
                .replace("_", " ")
            val n = neatOrganizationTypes.length

            return Response.Error(
                "Неправильный тип организации\nДоступные типы для ввода: ${
                    neatOrganizationTypes.substring(
                        1,
                        n - 1
                    )
                }"
            )
        }

    }
}