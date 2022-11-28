package ru.glorient.granitbk_n.avtoinformer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONObject
import ru.glorient.bkn.ServiceManager
import ru.glorient.granitbk_n.MainActivity

class AvtoInformatorViewModel : ViewModel() {

    private val repository = MainActivity.model.mInformer

    private val stopLiveData = MutableLiveData<List<Stop>>()

    val stops: LiveData<List<Stop>>
        get() = stopLiveData

    val serviceManager = repository.mServiceManager

    // Получаем маршрут и возвращаем индекс текущей остановки
    fun requestStops() : Int {
        var ind = 0
        repository.updateList() { index, stopsList ->
            ind = index
            stopLiveData.postValue(stopsList)
        }
        return ind
    }

    /**
     * Отсылка ответа на сообщение.
     *
     * @param answer ответ
     * @param topic топик куда отсылать ответ
     */
    fun answerQuery(answer: String, topic: String)
    {
        repository.isPopup.value = false
        if(answer != "Прочитано") {
            val payload = """{"answer":"$answer"}"""
            serviceManager?.sendMessage(
                ServiceManager.ServiceType.MQTT,
                JSONObject("""{"message":{"topic":"$topic/${repository.mPopupText.value}","payload":$payload}}""")
            )
        }
        repository.mPopupButton.clear()
        repository.mPopupText.value=""
        if(!repository.mQueue.isEmpty()){
            val q = repository.mQueue.first()
            repository.mQueue.remove(q)
            repository.popupWindow(q)
        }
    }
}