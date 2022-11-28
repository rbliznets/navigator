package ru.glorient.granitbk_n.accessory

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import ru.glorient.granitbk_n.avtoinformer.AvtoInformatorRepository

// Интерфейс для обновления списка в автоинформаторе и отправка/прием сообщений от диспетчера
interface UpdateListListener {
    fun updateList()
    fun messageFromDispatcher(
        message: MutableState<String>,
        mPopupButton: SnapshotStateList<AvtoInformatorRepository.Answer>
    )
    fun reconnectService()
}