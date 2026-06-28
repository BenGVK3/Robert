package au.com.benji.robert.screens.shack

import au.com.benji.robert.models.EquipmentCategory

data class AddEquipmentState(

    val category: EquipmentCategory = EquipmentCategory.RADIO,

    val manufacturer: String = "",

    val model: String = "",

    val nickname: String = "",

    val serialNumber: String = "",

    val notes: String = "",

    val imagePath: String = ""
)