package au.com.benji.robert.navigation

object EquipmentDestination {

    const val ITEM_ID = "itemId"

    const val Route = "equipment/{$ITEM_ID}"

    fun route(itemId: Long): String {
        return "equipment/$itemId"
    }
}