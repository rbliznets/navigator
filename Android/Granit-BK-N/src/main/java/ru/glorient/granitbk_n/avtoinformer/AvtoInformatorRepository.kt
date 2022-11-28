package ru.glorient.granitbk_n.avtoinformer

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ru.glorient.bkn.IServiceListiner
import ru.glorient.bkn.ServiceManager
import ru.glorient.granitbk_n.accessory.UpdateListListener
import ru.glorient.bkn.informer.ManualScript
import ru.glorient.bkn.informer.RouteItem

class AvtoInformatorRepository(
    private val context: Context,
    val onVideo: (file: String, id: Long) -> Unit = { _: String, _: Long -> }
) : IServiceListiner {

    /**
     * Класс ответа на сообщение.
     *
     * @property mText ответ
     * @property mTopic топик куда отсылать ответ
     */
    data class Answer(val mText: String, val mTopic: String)

    var isConnected = false
    override var mServiceManager: ServiceManager? = null
    var mRoute = mutableListOf<RouteItem>()
    var mScripts = mutableListOf<ManualScript>()
    var mStationID = 0
    private val listStops = mutableListOf<Stop>()
    var indexStop = 0

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
    var mQueue = mutableStateListOf<JSONObject>()


    // Кастуем Main Activity к интерфейсу
    private val updateListListener: UpdateListListener?
        get() = mServiceManager?.mContext?.let { it as? UpdateListListener }

    private val sharedPrefs by lazy {
        context.getSharedPreferences("granit_bkn_shared_prefs", Context.MODE_PRIVATE)
    }

    // Оповещаем об изменении списка
    fun updateList(
        onStopsFetched: (ind: Int, stops: List<Stop>) -> Unit
    ) {
        onStopsFetched(indexStop, listStops)
    }

    // Здесь ловим изменение состояния движения
    override fun onRecieve(tp: ServiceManager.ServiceType, data: JSONObject) {
        if (tp == ServiceManager.ServiceType.Informer) {
//            Log.d(AvtoInformatorFragment.TAG, "AvtoInformatorRepository onReceive")
            if (data.has("route")) {
                mRoute.clear()
                listStops.clear()
                val dt = data.getJSONArray("route")
                for (i in 0..dt.length() - 1) {
                    val item = RouteItem(dt.getJSONObject(i))
                    if (item.mID == mStationID) {
                        item.mNextID = 1
                    }
                    mRoute.add(item)
                    val defaultStop = Stop.DefaultStop(
//                        Random.nextLong(),
                        item.mID,
                        item.mName,
                        ""
                    )
                    listStops.add(defaultStop)
                }
                updateListListener?.updateList()
                if(mServiceManager?.mRoutesDownloadPath != "") {
                    saveDefaultRoute(mServiceManager?.mRoutesDownloadPath.orEmpty())
                }
                Log.d(AvtoInformatorFragment.TAG, "AvtoInformatorRepository Получили маршрут")
            }

//            if (data.has("name")) {
//                mName = data.getString("name")
//
//            }
//            if (data.has("forward"))
//                isForward = data.getBoolean("forward")
//            if (data.has("circle"))
//                isCircle = data.getBoolean("circle")
//
            if (data.has("scripts")) {
                mScripts.clear()
                val dt = data.getJSONArray("scripts")
                for (i in 0..dt.length() - 1) {
                    mScripts.add(ManualScript(dt.getJSONObject(i)))
                }
            }
//
            if (data.has("station")) {
//                Log.d(AvtoInformatorFragment.TAG, "mRoute $mRoute")
                Log.d(AvtoInformatorFragment.TAG, "station $data")
                mStationID = data.getInt("station")

                var r1 = mRoute.firstOrNull { (it.mID != mStationID) && (it.mNextID == 1) }
                if (r1 != null) {
                    val itm = r1.copy()
                    itm.mNextID = 0
                    val index = mRoute.indexOf(r1)
                    mRoute.remove(r1)
                    mRoute.add(index, itm)

                    listStops.removeAt(index)
                    val defaultStop = Stop.DefaultStop(
                        r1.mID,
                        r1.mName,
                        ""
                    )
                    listStops.add(index, defaultStop)
                }

                r1 = mRoute.firstOrNull { (it.mID == mStationID) && (it.mNextID == 0) }
                if (r1 != null) {
                    val itm = r1.copy()
                    itm.mNextID = 1
                    val index = mRoute.indexOf(r1)
                    mRoute.remove(r1)
                    mRoute.add(index, itm)
                    indexStop = index

                    listStops.removeAt(index)
                    val nextStop = Stop.NextStop(
                        r1.mID,
                        r1.mName,
                        ""
                    )
                    listStops.add(index, nextStop)
                }

                updateListListener?.updateList()

                updateList() { _, _ ->
                    indexStop
                    listStops
                }
                Log.d(AvtoInformatorFragment.TAG, "mRoute $mRoute")
            }
//
//            if(data.has("display")) {
//                val display = data.getJSONObject("display")
//                var str = ""
//                if (display.has("id")) {
//                    val ar = display.getJSONArray("id")
//                    if(ar.length() > 0){
//                        str = "["
//                        for(i in 0 until ar.length()-1){
//                            str+=ar.getInt(i).toString()+","
//                        }
//                        str+=ar.getInt(ar.length()-1).toString()+"]:"
//                    }
//                }
//                if (display.has("text")) str += display.getString("text")
//                mText = str
//            }

            if (data.has("video")) {
                val video = data.getJSONObject("video")
                onVideo(video.getString("file"),video.getLong("id"))
            }
        }
        if (tp == ServiceManager.ServiceType.MQTT) {
            if (data.has("query")) {

                val q = data.getJSONObject("query")
                if (!q.has("text")) return
                if (isPopup.value) {
                    mQueue.add(q)
                } else {
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
    fun popupWindow(q: JSONObject) {
        if (q.has("answers")) {
            val ans = q.getJSONArray("answers")
            for (i in 0 until ans.length()) {
                val itm = ans.getJSONObject(i)
                if (itm.has("text") && itm.has("topic")) {
                    mPopupButton.add(Answer(itm.getString("text"), itm.getString("topic")))
                }
            }
        }
        if (mPopupButton.isEmpty()) mPopupButton.add(Answer("Прочитано", ""))
        mPopupText.value = q.getString("text")
        isPopup.value = true

        updateListListener?.messageFromDispatcher(
            mPopupText,
            mPopupButton)
    }

    override fun onStateChange(
        tp: ServiceManager.ServiceType,
        state: ServiceManager.ServiceState,
        connection: String,
        payload: String
    ) {
        if (tp == ServiceManager.ServiceType.Informer) {
            isConnected = (state == ServiceManager.ServiceState.RUN)
        }
    }

    fun toggleGPS() {
        mServiceManager?.sendMessage(
            ServiceManager.ServiceType.Informer,
            JSONObject("""{"gpstoggle":"toggle"}""")
        )

    }

    // Записываем путь дефолтного маршрута в память
    fun saveDefaultRoute(routePath: String) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                sharedPrefs.edit()
                    .putString("defaultRoute", routePath)
                    .apply()
            } catch (t: Throwable) {}
        }
    }

    // Считываем из памяти путь дефолтного маршрута
    suspend fun getDefaultRouteToSharedPrefs(): String {
        return withContext(Dispatchers.Default) {
            try {
                sharedPrefs.getString("defaultRoute", null).orEmpty()
            } catch (t: Throwable) {
                ""
            }
        }
    }

    // Считываем из памяти путь дефолтного маршрута
    suspend fun getFlagServiceMqttToSharedPrefs(): Boolean {
        return withContext(Dispatchers.Default) {
            try {
                sharedPrefs.getBoolean("serviceMqtt", false)
            } catch (t: Throwable) {
                false
            }
        }
    }

    // Считываем из памяти путь дефолтного маршрута
    suspend fun getFlagServiceEgtsToSharedPrefs(): Boolean {
        return withContext(Dispatchers.Default) {
            try {
                sharedPrefs.getBoolean("serviceEgts", false)
            } catch (t: Throwable) {
                false
            }
        }
    }

    /**
     * Посылка окончания проигрования видеофайла.
     *
     * @param id индификатор файла
     */
    fun endVideo(id: Long){
        mServiceManager?.sendMessage(
            ServiceManager.ServiceType.Informer,
            JSONObject("""{"endvideo":${id}}""")
        )
        //Log.i("endVideo", "${id}")
    }

    /**
     * Запустить скрипт.
     *
     * @param id номер скрипта
     */
    fun play(id: Int){
        mServiceManager?.sendMessage(
            ServiceManager.ServiceType.Informer,
            JSONObject("""{"play":[${id}]}""")
        )
    }
}