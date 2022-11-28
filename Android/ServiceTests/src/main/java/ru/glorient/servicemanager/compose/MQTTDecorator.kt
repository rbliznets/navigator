package ru.glorient.servicemanager.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import org.json.JSONObject
import ru.glorient.bkn.IServiceListiner
import ru.glorient.bkn.ServiceManager

/**
 * Класс ответа на сообщение.
 *
 * @property mText ответ
 * @property mTopic топик куда отсылать ответ
 */
data class Answer(val mText: String, val mTopic: String)

/**
 * Класс подписчика [ServiceManager] для сервиса MQTT.
 *
 * Реализует Model Viewer для compose.
 */
class MQTTDecorator : IServiceListiner {
    /**
     * Родительский [ServiceManager].
     */
    override var mServiceManager: ServiceManager? = null
    /**
     * Флаг установленного соединения.
     */
    private var isConnected = mutableStateOf(false)

    /**
     * Флаг отображения полученного сообщения.
     */
    var isPopup = mutableStateOf(false)
    /**
     * Полученное сообщение для отображения.
     */
    var mPopupText = mutableStateOf("")
    /**
     * Список ответов.
     */
    var mPopupButton = mutableStateListOf<Answer>()
    /**
     * Очередь полученных сообщений.
     */
    private var mQueue = mutableStateListOf<JSONObject>()

    /**
     * Изменение состояния сервиса.
     *
     * @param tp сервис
     * @param state состояние
     */
    override fun onStateChange(
        tp: ServiceManager.ServiceType,
        state: ServiceManager.ServiceState,
        connection: String,
        payload: String
    ){
        if(tp == ServiceManager.ServiceType.Informer){
            isConnected.value = (state == ServiceManager.ServiceState.RUN)
        }
    }

    /**
     * Получено сообщение от сервиса.
     *
     * @param tp сервис
     * @param data сообщение
     */
    override fun onRecieve(tp: ServiceManager.ServiceType, data: JSONObject){
        if(tp == ServiceManager.ServiceType.MQTT) {
            if (data.has("query")) {
                val q = data.getJSONObject("query")
                if(!q.has("text"))return
                if(isPopup.value){
                    mQueue.add(q)
                }else{
                    popupWindow(q)
                }
            }
        }
    }

    /**
     * Формирование отобращения полученного сообщения.
     *
     * @param q сообщение в JSON формате от сервиса.
     */
    private fun popupWindow(q: JSONObject) {
        if (q.has("answers")) {
            val ans = q.getJSONArray("answers")
            for(i in 0 until ans.length()){
                val itm = ans.getJSONObject(i)
                if(itm.has("text") && itm.has("topic"))
                {
                    mPopupButton.add(Answer(itm.getString("text"),itm.getString("topic")))
                }
            }
         }
        if(mPopupButton.isEmpty()) mPopupButton.add(Answer("Прочитано",""))
        mPopupText.value=q.getString("text")
        isPopup.value = true
    }

    /**
     * Отсылка ответа на сообщение.
     *
     * @param answer ответ
     * @param topic топик куда отсылать ответ
     */
    fun answerQuery(answer: String, topic: String)
    {
        isPopup.value = false
        if(answer != "Прочитано") {
            val payload = """{"answer":"$answer"}"""
            mServiceManager?.sendMessage(ServiceManager.ServiceType.MQTT,
                    JSONObject("""{"message":{"topic":"$topic/${mPopupText.value}","payload":$payload}}"""))
        }
        mPopupButton.clear()
        mPopupText.value=""
        if(!mQueue.isEmpty()){
            val q = mQueue.first()
            mQueue.remove(q)
            popupWindow(q)
        }
    }
}

/**
 * Отобразить popup окно для полученного сообщения.
 *
 * @param mqtt ссалка на объект [MQTTDecorator]
 */
@Composable
fun QueryPopup(mqtt: MQTTDecorator){
    if(mqtt.isPopup.value) {
        Popup(alignment = Alignment.Center) {
            // Draw a rectangle shape with rounded corners inside the popup
            Column(
                    modifier = Modifier.fillMaxSize(0.7f)
                            .wrapContentHeight()
                            .background(Color.White)
                            .border(3.dp, Color.Red)
                            .padding(10.dp),
                    verticalArrangement = Arrangement.Center
            ) {
                Text(
                        modifier = Modifier.padding(4.dp,4.dp,4.dp,10.dp),
                        text = mqtt.mPopupText.value,
                        textAlign = TextAlign.Center
                )
                Row(
                        modifier = Modifier.fillMaxWidth().padding(0.dp,0.dp,0.dp,10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                ) {
                    mqtt.mPopupButton.forEach{
                        Button(
                                modifier = Modifier.padding(2.dp),
                                onClick = {
                                    mqtt.answerQuery(it.mText,it.mTopic)
                                }
                        ) {
                            Text(text = it.mText)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compose Preview.
 */
@Preview
@Composable
fun QueryPopupPreview() {
    val mqtt = MQTTDecorator()
    mqtt.isPopup.value = true
    mqtt.mPopupText.value = "Вопрос?"
    mqtt.mPopupButton.add(Answer("Да","answer"))
    mqtt.mPopupButton.add(Answer("Нет","answer"))
    MaterialTheme{
        // A surface container using the 'background' color from the theme
        Surface(color = MaterialTheme.colors.background) {
            QueryPopup(mqtt)
        }
    }
}