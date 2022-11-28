package ru.glorient.bkn.informer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.json.JSONArray
import org.json.JSONObject

/**
 * Класс шага скрипта отсылки сообщения в MQTT сервер
 *
 * @property  mTopic Топик, куда отсылается сообщение
 * @property  mPayload Сообщение в виде JSON структуры
 * @property  mGPS признак выполнения в зависимости от режима триггера GPS
 * @property  mRoute признак выполнения в зависимости от вектора движения
 */
data class MQTTStep(
        var mTopic: String = "",
        var mPayload: JSONObject = JSONObject(),
        var mGPS: GPSTriggerMode = GPSTriggerMode.NONE,
        var mRoute: RouteDir = RouteDir.ANY
) {
    /**
     * Конструктор из JSON
     *
     * @param  json Описание данных в JSON
     * @param  dir Вектор напровления остановки
     */
    constructor(json: JSONObject, dir: RouteDir? = null) : this() {
        if(json.has("topic")){
            mTopic = json.getString("topic")
        }
        if(json.has("payload")){
            mPayload = json.getJSONObject("payload")
        }
        if(json.has("gps")){
            mGPS = when(json.getString("gps")){
                "prior" -> GPSTriggerMode.PRIOR
                "post" -> GPSTriggerMode.POST
                else -> GPSTriggerMode.NONE
            }
        }
        if((dir==null) || (dir == RouteDir.ANY)){
            if(json.has("route")){
                mRoute = when(json.getString("route")){
                    "forward" -> RouteDir.FORWARD
                    "back" -> RouteDir.BACKWARD
                    "backward" -> RouteDir.BACKWARD
                    else -> RouteDir.ANY
                }
            }
        }else mRoute=dir
    }
}

/**
 * Класс задачи скрипта MQTT
 *
 * @property  mID id скрипта
 * @property  mTaskPriority приоретет скрипта
 * @param  json JSON массив шагов скрипта
 * @param  dir Вектор напровления остановки
 */
class MqttTask(var mID: Int, private val mTaskPriority: TaskPriority, json: JSONArray? = null, dir: RouteDir? = null) {
    var mScriptList = mutableListOf<MQTTStep>()
    init{
        if(json != null) {
            for (index in 0 until json.length()) {
                val script = json.getJSONObject(index)
                if (script.has("payload")) {
                    mScriptList.add(MQTTStep(script, dir))
                }
            }
        }
    }

    /**
     * Получить задачу с фильтрацией по вектору направления
     *
     * @param dir вектор направления
     * @return копия задачи с отфильтрованными шагами
     */
    fun getTaskCopy(dir: RouteDir): MqttTask{
        val res = MqttTask(mID, mTaskPriority)
        res.mScriptList = mScriptList
                .filter { (it.mRoute == RouteDir.ANY) || (it.mRoute == dir) }
                .toMutableList()
        return res
    }

    /**
     * Получить задачу с фильтрацией по режиму GPS
     *
     * @param gps режим сраьатывания GPS
     * @return копия задачи с отфильтрованными шагами
     */
    fun getTaskCopy(gps: GPSTriggerMode): MqttTask{
        val res = MqttTask(mID, mTaskPriority)
        res.mScriptList = mScriptList
                .filter { (it.mGPS == gps) }
                .toMutableList()
        return res
    }

    /**
     * Соединить шаги задач
     *
     * @param task задача с присоединяемыми шагами
     * @return копия задачи с шагами 2-х задач
     */
    fun addScriptList(task: MqttTask): MqttTask{
        val res = MqttTask(mID, mTaskPriority)
        res.mScriptList = mScriptList.toMutableList()
        res.mScriptList.addAll(task.mScriptList)
        return res
    }

    /**
     * Строковое отображение объекта
     *
     * @return строка для отладки
     */
    override fun toString(): String {
        var res = "$mTaskPriority|"
        mScriptList.forEach{
            res += "$it|"
        }
        return res
    }
}

/**
 * Класс очереди задач MQTT
 *
 * Так как сообщения сразу отсылаются, то класс сделан для однотипности.
 * Буферезацию осуществляет базовый класс через канал.
 *
 * @property  onMqtt функция на событие отсылки сообщения, где topic топик MQTT, а payload само сообщение.
 */
open class MqttTaskQueue(val onMqtt: (topic: String, payload: JSONObject) -> Unit = { _: String, _: JSONObject -> }) : TaskQueue(100) {
    /**
     * Функция на обработку сообщений корутине
     *
     * @param msg сообщение из канала
     */
    override suspend fun analize(msg: Any){
        when (msg) {
            is MqttTask -> {
                addMqttTask(msg)
            }
        }
    }

    /**
     * Функция добавления скрипта в очередь
     *
     * @param task скрипт
     */
    private fun addMqttTask(task: MqttTask) {
        task.mScriptList.forEach {
            onMqtt(it.mTopic, it.mPayload)
        }
    }

    /**
     * Метод добавления скрипта
     *
     * @param script скрипт
     */
    @ExperimentalCoroutinesApi
    suspend fun add(script: MqttTask){
        if(script.mScriptList.isNotEmpty()) {
            if (!mChannel.isClosedForSend) mChannel.send(script)
        }
    }
}