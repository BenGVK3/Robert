package au.com.benji.robert.screens.shack

import androidx.lifecycle.ViewModel
import au.com.benji.robert.database.ShackEntity
import au.com.benji.robert.repository.ShackRepository

class AddEquipmentViewModel(
    private val repository: ShackRepository
) : ViewModel() {

    suspend fun saveEquipment(
        category: String,
        manufacturer: String,
        model: String,
        nickname: String,
        serialNumber: String = "",
        notes: String = "",
        imagePath: String = ""
    ) {
        repository.addEquipment(
            ShackEntity(
                category = category,
                manufacturer = manufacturer.trim(),
                model = model.trim(),
                nickname = nickname.trim(),
                serialNumber = serialNumber.trim(),
                notes = notes.trim(),
                imagePath = imagePath
            )
        )
    }
}
