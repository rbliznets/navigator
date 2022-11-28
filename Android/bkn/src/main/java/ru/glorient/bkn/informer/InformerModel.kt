package ru.glorient.bkn.informer

import android.content.Context
import android.location.Location
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ticker
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.math.abs

/**
 * Класс модели информатора
 *
 * Реализует логику работы информатора
 *
 * @param file файл json
 * @param context контекст для работы информатора
 * @property  onText функция на событие отображение текста, где s сам текст, а l список номеров дисплеев.
 * @property  onVideo функция на событие показа вмдео, где file полный путь к видеофайлу, а id его индификатор.
 * @property  onChangeStation функция на событие определения остановки, где stationID id скрипта привязанного к остановке.
 * @property  onChangeDirrection функция на событие изменения вектора движения.
 * @property  onError функция на ошибку в работе, где er ее описание.
 */
@ExperimentalCoroutinesApi
class InformerModel(
    file: File,
    context: Context,
    val onText: (s: String, l: List<Int>) -> Unit = { _: String, _: List<Int> -> },
    val onVideo: (file: String, id: Long) -> Unit = { _: String, _: Long -> },
    val onMqtt: (topic: String, payload: JSONObject) -> Unit = { _: String, _: JSONObject -> },
    val onChangeStation: (stationID: Int) -> Unit = { },
    val onChangeDirrection: () -> Unit = { },
    val onError: (er: JSONObject) -> Unit = { }
) {
    /**
     * Папка json файла
     */
    private val mPath: String = file.absolutePath.substringBeforeLast("/")+"/"
    /**
     * Текущий вектор движения
     */
    var mDirrection = RouteDir.FORWARD
    /**
     * Режим автоопределения вектора движения
     */
    var mAutoDirrection = AutoDirMode.HARD
    /**
     * Список остановок по прямому вектору движения
     */
    private var mRouteForward = mutableListOf<RouteItem>()
    /**
     * Список остановок по обратному вектору движения
     */
    private var mRouteBack = mutableListOf<RouteItem>()
    /**
     * Список скиптов для ручного запуска не входящих в списки остановок
     */
    private var mManual = mutableListOf<ManualScript>()
    /**
     * Тип маршрута
     */
    private var mRouteType: RouteType = RouteType.BIDIR
    /**
     * Название маршрута
     */
    private var mRouteName = "Маршрут"
    /**
     * Флаг работы тригеров по локации
     */
    var mGPSTriggerEnable = true

    /**
     * Карта скриптов отображения текста
     */
    private var mTextTasks = mutableMapOf<Int,TextTask>()
    /**
     * Очередь скриптов отображения текста
     */
    private val mTextTaskQueue = TextTaskQueue(onText)

    /**
     * Карта скриптов проигрования аудиофайлов
     */
    private var mAudioTasks = mutableMapOf<Int,AudioTask>()
    /**
     * Очередь скриптов проигрования аудиофайлов
     */
    private val mAudioTaskQueue = AudioTaskQueue(context)

    /**
     * Карта скриптов проигрования видиофайлов
     */
    private var mVideoTasks = mutableMapOf<Int,VideoTask>()
    /**
     * Очередь скриптов проигрования видиофайлов
     */
    private val mVideoTaskQueue = VideoTaskQueue(onVideo)

    /**
     * Карта скриптов MQTT
     */
    private var mMqttTasks = mutableMapOf<Int,MqttTask>()
    /**
     * Очередь скриптов MQTT
     */
    private val mMqttTaskQueue = MqttTaskQueue(onMqtt)

    /**
     * Карта триггеров для запуска вручную
     */
    private var mManualTriggers = mutableMapOf<Int, ExtrnalTrigger>()
    /**
     * Карта триггеров для запуска по местоположению
     */
    private var mGPSTriggers = mutableMapOf<Int, GPSTrigger>()
    /**
     * Карта триггеров для запуска вручную
     */
    private var mTimerTriggers = mutableListOf<TimerTrigger>()
    /**
     * id скрипта для запуска при смене вектора напрвления
     */
    private var mChangeDirID: Int? = null

    /**
     * Канал для формирования 1сек тика
     */
    @ObsoleteCoroutinesApi
    val mTickerChannel = ticker(delayMillis = 1000, initialDelayMillis = 1000)
    /**
     * Корутина для запуска скриптов по таймеру
     */
    @ExperimentalCoroutinesApi
    @ObsoleteCoroutinesApi
    val mTimerJob = GlobalScope.launch {
        val delTriggerList = mutableListOf<Int>()
        for(x in mTickerChannel){
           val time = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())
           mTimerTriggers.forEach {
               if(it.mNext == null){
                   if(!it.findOutNextDate(time))delTriggerList.add(it.mID)
               }else{
                   if(it.mNext!!.before(time)){
                       it.mNext = null
                       runBlocking {
                           if (mTextTasks.containsKey(it.mID))
                               mTextTaskQueue.add(mTextTasks[it.mID]!!.getTaskCopy(mDirrection))
                           if (mAudioTasks.containsKey(it.mID))
                               mAudioTaskQueue.add(mAudioTasks[it.mID]!!.getTaskCopy(mDirrection))
                           if (mVideoTasks.containsKey(it.mID))
                               mVideoTaskQueue.add(mVideoTasks[it.mID]!!.getTaskCopy(mDirrection))
                           if (mMqttTasks.containsKey(it.mID))
                               mMqttTaskQueue.add(mMqttTasks[it.mID]!!.getTaskCopy(mDirrection))
                       }
                   }
               }
           }
           mTimerTriggers.removeAll { it.mID in delTriggerList }
           delTriggerList.clear()
        }
    }

    fun endVideo(id: Long){
        mVideoTaskQueue.stopEvent(id)
    }

    /**
     * Функция остановки корутины для запуска скриптов по таймеру
     */
    @ObsoleteCoroutinesApi
    @ExperimentalCoroutinesApi
    suspend fun deInit(){
        mTickerChannel.cancel()
        mTimerJob.cancelAndJoin()
    }

    /**
     * Конструктор
     *
     * Парсит json файл и в случае удачи инициализурует все триггеры и очереди скриптов.
     */
    init{
        val str = StringBuilder()
        file.forEachLine {
            str.append(it.substringBeforeLast("//").trim())
        }
        val check = ScriptErrorFinder.findID(str.toString())
        if(check == 0) {
            val route = JSONObject(str.toString())
            if (route.has("version")) {
                if (route.getInt("version") > 1){
                    onError(JSONObject("""{"script":"${file.absoluteFile}","text":"Route has version ${route.getInt("version")}>1"}"""))
                } else {
                    if (route.has("name")) mRouteName = route.getString("name")
                    if (route.has("type")) mRouteType = if (route.getString("type") == "circle") RouteType.CIRCLE else RouteType.BIDIR
                    if (route.has("sequences")) {
                        val sequences = route.getJSONArray("sequences")
                        for (index in 0 until sequences.length()) {
                            val script = sequences.getJSONObject(index)
                            var id: Int
                            if (script.has("id")) {
                                id = script.getInt("id")
                            } else {
                                onError(JSONObject("""{"script":"${file.absoluteFile}","text":"parser: script does not has id"}"""))
                                break
                            }
                            try {
                                var nextStop = -1
                                var dir = RouteDir.ANY
                                if (script.has("stop")){
                                    val stop = script.getJSONObject("stop")
                                    nextStop = stop.getInt("nextstop")
                                    dir = when(stop.getString("route")){
                                        "forward"->RouteDir.FORWARD
                                        "back"->RouteDir.BACKWARD
                                        "backward"->RouteDir.BACKWARD
                                        else -> throw Exception("stop route failed")
                                    }
                                }
                                val name = if (script.has("name")) script.getString("name") else "script_$id"
                                val priority = if (script.has("mode")) TaskPriority(script.getJSONObject("mode")) else TaskPriority()
                                var external = true
                                var menu = false
                                if (script.has("trigger")) {
                                    val trigger = script.getJSONObject("trigger")
                                    if (trigger.has("external")) {
                                        val ext = trigger.getJSONObject("external")
                                        if(ext.has("enable")) {
                                            if (ext.getString("enable") == "off") external = false
                                        }
                                        if(ext.has("menu")) {
                                            if (ext.getString("menu") == "on") menu = true
                                        }
                                    }
                                    if (trigger.has("changedir")) {
                                        if (trigger.getString("changedir") == "on") mChangeDirID = id
                                    }
                                    if (trigger.has("gps")) {
                                        val gps = GPSTrigger(trigger.getJSONObject("gps"),dir)
                                        mGPSTriggers[id] = gps
                                    }
                                    if (trigger.has("time")) {
                                        val tx = trigger.getJSONObject("time")
                                        if (tx.has("cronlike")) {
                                            val tar = tx.getJSONArray("cronlike")
                                            for (i in 0 until tar.length()) {
                                                mTimerTriggers.add(TimerTrigger(id, tar.getString(i)))
                                            }
                                        }
                                    }
                                }
                                if (script.has("texts")) {
                                    mTextTasks[id] = TextTask(id, priority, script.getJSONArray("texts"),dir)
                                }
                                if (script.has("audio")) {
                                    mAudioTasks[id] = AudioTask(id, priority, mPath, script.getJSONArray("audio"),dir)
                                }
                                if (script.has("video")) {
                                    mVideoTasks[id] = VideoTask(id, priority, mPath, script.getJSONArray("video"),dir)
                                }
                                if (script.has("mqtt")) {
                                    mMqttTasks[id] = MqttTask(id, priority, script.getJSONArray("mqtt"),dir)
                                }

                                if (nextStop > -1) {
                                    if(dir == RouteDir.FORWARD) mRouteForward.add(RouteItem(id, name, nextStop, external))
                                    else  mRouteBack.add(RouteItem(id, name, nextStop, external))
                                } else if (external) {
                                    mManual.add(ManualScript(id, menu, name))
                                }
                                if (mManualTriggers.containsKey(id)) throw Exception("ID does not unique")
                                mManualTriggers[id] = ExtrnalTrigger(external,menu)
                            } catch (e: Exception) {
                                onError(JSONObject("""{"script":"${file.absoluteFile}","text":"parser(id=$id)->$e"}"""))
                                break
                            }
                        }
                    } else {
                        onError(JSONObject("""{"script":"${file.absoluteFile}","text":"Route does not has sequences"}"""))
                    }
                    mRouteForward=sortRoute(mRouteForward)
                    mRouteBack=sortRoute(mRouteBack)
                    playChangeDirScript()
                }
            } else {
                onError(JSONObject("""{"script":"${file.absoluteFile}","text":"Route does not has version"}"""))
            }
        }else{
            when(check){
                -1 -> onError(JSONObject("""{"script":"${file.absoluteFile}","text":"Scrip checking failed: check []"}"""))
                -2 -> onError(JSONObject("""{"script":"${file.absoluteFile}","text":"Scrip checking failed: check {}"}"""))
                -3 -> onError(JSONObject("""{"script":"${file.absoluteFile}","text":"Scrip checking failed: check id"}"""))
                -4 -> onError(JSONObject("""{"script":"${file.absoluteFile}","text":"Scrip checking failed: check id"}"""))
                else -> onError(JSONObject("""{"script":"${file.absoluteFile}","text":"Scrip checking failed: check id=$check"}"""))
            }
        }
    }

    /**
     * Сортировка списка остановок
     *
     * @param route изначальный список остановок
     * @return отсортированный список остановок
     */
    private fun sortRoute(route: MutableList<RouteItem>): MutableList<RouteItem> {
        val res = mutableListOf<RouteItem>()
        var next = 0
        var rt = route.filter { it.mNextID == next }
        while(rt.isNotEmpty()){
            if(rt.size > 1) throw Exception("Route stops error (mNextID=$next is not unique)")
            next = rt[0].mID
            res.add(0,rt[0])
            rt = route.filter { it.mNextID == next }
        }
        if(route.size != res.size) throw Exception("Route stops error (extra items ${route.size}!=${res.size})")
        return res
    }

    /**
     * Получить список остановок для текущего вектора движения
     *
     * @return список скриптов остановок в виде json
     */
    fun getRoute(): JSONObject{
        val res = JSONObject()
        val route = JSONArray()
        if(mDirrection == RouteDir.FORWARD) {
            mRouteForward.forEach {
                route.put(it.getJSON())
            }
        }else{
            mRouteBack.forEach {
                route.put(it.getJSON())
            }
        }
        res.put("route", route)
        res.put("name",mRouteName)
        res.put("forward",mDirrection == RouteDir.FORWARD)
        res.put("circle",mRouteType == RouteType.CIRCLE)
        return res
    }

    /**
     * Получить список скриптов для активирования вручную
     *
     * @return список скриптов в виде json
     */
    fun getScripts(): JSONObject{
        val res = JSONObject()
        val scripts = JSONArray()
        mManual.forEach {
            scripts.put(it.getJSON())
        }
        res.put("scripts", scripts)
        return res
    }

    /**
     * Функция изменения местоположения
     *
     * Определяются срабатывание триггеров GPS
     *
     * @param location локация
     */
    @ExperimentalCoroutinesApi
    fun changeLocation(location: Location) {
        if(mGPSTriggerEnable && location.hasAccuracy() && (location.accuracy < 100.0)) {
            mGPSTriggers.forEach { (id, trig) ->
                if((trig.mDir == RouteDir.ANY) || (trig.mDir == mDirrection)) {
                    val loc = Location("trig")
                    loc.longitude = trig.mLongitude
                    loc.latitude = trig.mLatitude
                    if (trig.mPrior != 0f) {
                        if (trig.mIn == 0) {
                            if (location.distanceTo(loc) < trig.mPrior) {
                                trig.mIn = 1
                                GlobalScope.launch {
                                    if (mTextTasks.containsKey(id)) {
                                        val task = mTextTasks[id]!!.getTaskCopy(mDirrection).getTaskCopy(GPSTriggerMode.PRIOR)
                                        if (task.mScriptList.isNotEmpty()) {
                                            task.mID += 20000
                                            mTextTaskQueue.add(task)
                                        }
                                    }
                                    if (mAudioTasks.containsKey(id)) {
                                        val task = mAudioTasks[id]!!.getTaskCopy(mDirrection).getTaskCopy(GPSTriggerMode.PRIOR)
                                        if (task.mScriptList.isNotEmpty()) {
                                            task.mID += 20000
                                            mAudioTaskQueue.add(task)
                                        }
                                    }
                                    if (mVideoTasks.containsKey(id)) {
                                        val task = mVideoTasks[id]!!.getTaskCopy(mDirrection).getTaskCopy(GPSTriggerMode.PRIOR)
                                        if (task.mScriptList.isNotEmpty()) {
                                            task.mID += 20000
                                            mVideoTaskQueue.add(task)
                                        }
                                    }
                                    if (mMqttTasks.containsKey(id)) {
                                        val task = mMqttTasks[id]!!.getTaskCopy(mDirrection).getTaskCopy(GPSTriggerMode.PRIOR)
                                        if (task.mScriptList.isNotEmpty()) {
                                            task.mID += 20000
                                            mMqttTaskQueue.add(task)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (trig.mIn < 2) {
                        if (location.distanceTo(loc) < trig.mRadius) {
                            trig.mIn = 2
                            if ((mAutoDirrection != AutoDirMode.NONE) && (trig.mBearing != null) && location.hasBearing()) {
                                when (mAutoDirrection) {
                                    AutoDirMode.SOFT -> {
                                        if (location.hasSpeed() && (location.speed > 1) && location.hasBearing()) {
                                            var x = abs(trig.mBearing!! - location.bearing)
                                            if (x > 180) x = 360 - x

                                            if (x < 60) {
                                                if (mDirrection != RouteDir.FORWARD) {
                                                    changeDirrection(RouteDir.FORWARD)
                                                }
                                            } else if (x > 120) {
                                                if (mDirrection != RouteDir.BACKWARD) {
                                                    changeDirrection(RouteDir.BACKWARD)
                                                }
                                            }
                                        }
                                    }
                                    AutoDirMode.HARD -> {
                                        if (location.hasBearingAccuracy() && location.hasBearing()) {
                                            var x = abs(trig.mBearing!! - location.bearing)
                                            if (x > 180) x = 360 - x

                                            if (x < 60) {
                                                if (mDirrection != RouteDir.FORWARD) {
                                                    changeDirrection(RouteDir.FORWARD)
                                                }
                                            } else if (x > 120) {
                                                if (mDirrection != RouteDir.BACKWARD) {
                                                    changeDirrection(RouteDir.BACKWARD)
                                                }
                                            }
                                        }
                                    }
                                    else -> {}
                                }
                            }
                            if ((mDirrection == RouteDir.FORWARD) && (mRouteForward.any { it.mID == id })) onChangeStation(id)
                            else if ((mDirrection == RouteDir.BACKWARD) && (mRouteBack.any { it.mID == id })) onChangeStation(id)
                            val dir = mDirrection
                            GlobalScope.launch {
                                if (mTextTasks.containsKey(id)) {
                                    var task: TextTask? = null
                                    if (trig.mPrior == 0f) task = mTextTasks[id]!!.getTaskCopy(dir).getTaskCopy(GPSTriggerMode.PRIOR)
                                    task = task?.addScriptList(mTextTasks[id]!!.getTaskCopy(dir).getTaskCopy(GPSTriggerMode.NONE))
                                            ?: mTextTasks[id]!!.getTaskCopy(dir).getTaskCopy(GPSTriggerMode.NONE)
                                    if (trig.mPost == 0f) {
                                        task = task.addScriptList(mTextTasks[id]!!.getTaskCopy(dir).getTaskCopy(GPSTriggerMode.POST))
                                    }
                                    if (trig.mDelay != 0) {
                                        task.mID = id + 10000
                                        mTextTasks[id + 10000] = task
                                        mTimerTriggers.add(TimerTrigger(id + 10000, trig.mDelay.toLong()))
                                    } else {
                                        task.mID += 30000
                                        mTextTaskQueue.add(task)
                                    }
                                }
                                if (mAudioTasks.containsKey(id)) {
                                    var task: AudioTask? = null
                                    if (trig.mPrior == 0f) task = mAudioTasks[id]!!.getTaskCopy(dir).getTaskCopy(GPSTriggerMode.PRIOR)
                                    task = task?.addScriptList(mAudioTasks[id]!!.getTaskCopy(dir).getTaskCopy(GPSTriggerMode.NONE))
                                        ?: mAudioTasks[id]!!.getTaskCopy(dir).getTaskCopy(GPSTriggerMode.NONE)
                                    if (trig.mPost == 0f) {
                                        task = task.addScriptList(mAudioTasks[id]!!.getTaskCopy(dir).getTaskCopy(GPSTriggerMode.POST))
                                    }
                                    if (trig.mDelay != 0) {
                                        task.mID = id + 10000
                                        mAudioTasks[id + 10000] = task
                                        mTimerTriggers.add(TimerTrigger(id + 10000, trig.mDelay.toLong()))
                                    } else {
                                        task.mID += 30000
                                        mAudioTaskQueue.add(task)
                                    }
                                }
                                if (mVideoTasks.containsKey(id)) {
                                    var task: VideoTask? = null
                                    if (trig.mPrior == 0f) task = mVideoTasks[id]!!.getTaskCopy(dir).getTaskCopy(GPSTriggerMode.PRIOR)
                                    task = task?.addScriptList(mVideoTasks[id]!!.getTaskCopy(dir).getTaskCopy(GPSTriggerMode.NONE))
                                        ?: mVideoTasks[id]!!.getTaskCopy(dir).getTaskCopy(GPSTriggerMode.NONE)
                                    if (trig.mPost == 0f) {
                                        task = task.addScriptList(mVideoTasks[id]!!.getTaskCopy(dir).getTaskCopy(GPSTriggerMode.POST))
                                    }
                                    if (trig.mDelay != 0) {
                                        task.mID = id + 10000
                                        mVideoTasks[id + 10000] = task
                                        mTimerTriggers.add(TimerTrigger(id + 10000, trig.mDelay.toLong()))
                                    } else {
                                        task.mID += 30000
                                        mVideoTaskQueue.add(task)
                                    }
                                }
                                if (mMqttTasks.containsKey(id)) {
                                    var task: MqttTask? = null
                                    if (trig.mPrior == 0f) task = mMqttTasks[id]!!.getTaskCopy(dir).getTaskCopy(GPSTriggerMode.PRIOR)
                                    task = task?.addScriptList(mMqttTasks[id]!!.getTaskCopy(dir).getTaskCopy(GPSTriggerMode.NONE))
                                            ?: mMqttTasks[id]!!.getTaskCopy(dir).getTaskCopy(GPSTriggerMode.NONE)
                                    if (trig.mPost == 0f) {
                                        task = task.addScriptList(mMqttTasks[id]!!.getTaskCopy(dir).getTaskCopy(GPSTriggerMode.POST))
                                    }
                                    if (trig.mDelay != 0) {
                                        task.mID = id + 10000
                                        mMqttTasks[id + 10000] = task
                                        mTimerTriggers.add(TimerTrigger(id + 10000, trig.mDelay.toLong()))
                                    } else {
                                        task.mID += 30000
                                        mMqttTaskQueue.add(task)
                                    }
                                }
                            }
                            if (trig.mPrior == 0f) {
                                if ((mAutoDirrection != AutoDirMode.NONE) && (trig.mBearing != null) && location.hasBearing()) {
                                    when (mAutoDirrection) {
                                        AutoDirMode.SOFT -> {
                                            if (location.hasSpeed() && (location.speed > 1) && location.hasBearing()) {
                                                var x = abs(trig.mBearing!! - location.bearing)
                                                if (x > 180) x = 360 - x

                                                if (x < 60) {
                                                    if (mDirrection != RouteDir.FORWARD) {
                                                        changeDirrection(RouteDir.FORWARD)
                                                    }
                                                } else if (x > 120) {
                                                    if (mDirrection != RouteDir.BACKWARD) {
                                                        changeDirrection(RouteDir.BACKWARD)
                                                    }
                                                }
                                            }
                                        }
                                        AutoDirMode.HARD -> {
                                            if (location.hasBearingAccuracy() && location.hasBearing()) {
                                                var x = abs(trig.mBearing!! - location.bearing)
                                                if (x > 180) x = 360 - x

                                                if (x < 60) {
                                                    if (mDirrection != RouteDir.FORWARD) {
                                                        changeDirrection(RouteDir.FORWARD)
                                                    }
                                                } else if (x > 120) {
                                                    if (mDirrection != RouteDir.BACKWARD) {
                                                        changeDirrection(RouteDir.BACKWARD)
                                                    }
                                                }
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }
                    if (trig.mPost != 0f) {
                        if (trig.mIn == 2) {
                            if (location.distanceTo(loc) > trig.mPost) {
                                trig.mIn = 0

                                GlobalScope.launch {
                                    if (mTextTasks.containsKey(id)) {
                                        val task = mTextTasks[id]!!.getTaskCopy(mDirrection).getTaskCopy(GPSTriggerMode.POST)
                                        if (task.mScriptList.isNotEmpty()) {
                                            task.mID += 40000
                                            mTextTaskQueue.add(task)
                                        }
                                    }
                                    if (mAudioTasks.containsKey(id)) {
                                        val task = mAudioTasks[id]!!.getTaskCopy(mDirrection).getTaskCopy(GPSTriggerMode.POST)
                                        if (task.mScriptList.isNotEmpty()) {
                                            task.mID += 40000
                                            mAudioTaskQueue.add(task)
                                        }
                                    }
                                    if (mVideoTasks.containsKey(id)) {
                                        val task = mVideoTasks[id]!!.getTaskCopy(mDirrection).getTaskCopy(GPSTriggerMode.POST)
                                        if (task.mScriptList.isNotEmpty()) {
                                            task.mID += 40000
                                            mVideoTaskQueue.add(task)
                                        }
                                    }
                                    if (mMqttTasks.containsKey(id)) {
                                        val task = mMqttTasks[id]!!.getTaskCopy(mDirrection).getTaskCopy(GPSTriggerMode.POST)
                                        if (task.mScriptList.isNotEmpty()) {
                                            task.mID += 40000
                                            mMqttTaskQueue.add(task)
                                        }
                                    }
                                }

                                if (mRouteType == RouteType.BIDIR) {
                                    when (mDirrection) {
                                        RouteDir.FORWARD -> {
                                            if (mRouteForward.last().mID == id) changeDirrection(RouteDir.BACKWARD)
                                        }
                                        RouteDir.BACKWARD -> {
                                            if (mRouteBack.last().mID == id) changeDirrection(RouteDir.FORWARD)
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }else{
                        if (trig.mIn == 2) {
                            if (location.distanceTo(loc) > trig.mRadius) {
                                trig.mIn = 0

                                if (mRouteType == RouteType.BIDIR) {
                                    when (mDirrection) {
                                        RouteDir.FORWARD -> {
                                            if (mRouteForward.last().mID == id) changeDirrection(RouteDir.BACKWARD)
                                        }
                                        RouteDir.BACKWARD -> {
                                            if (mRouteBack.last().mID == id) changeDirrection(RouteDir.FORWARD)
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            mGPSTriggers.forEach { (_, trig) ->
                trig.mIn = 0
            }
        }
    }

    /**
     * Функция изменения вектора движения
     *
     * @param dir вектор движения
     */
    @ExperimentalCoroutinesApi
    fun changeDirrection(dir: RouteDir) {
        if(mDirrection != dir) {
            mDirrection = dir
            mGPSTriggers.forEach { (_, trig) ->
                trig.mIn = 0
            }
            playChangeDirScript()
            onChangeDirrection()
        }
    }

    /**
     * Функция триггера изменения вектора движения
     */
    @ExperimentalCoroutinesApi
    private fun playChangeDirScript() {
        if (mChangeDirID != null) {
            if (mTextTasks.containsKey(mChangeDirID)) {
                runBlocking { mTextTaskQueue.add(mTextTasks[mChangeDirID]!!.getTaskCopy(mDirrection)) }
            }
            if (mAudioTasks.containsKey(mChangeDirID)) {
                runBlocking { mAudioTaskQueue.add(mAudioTasks[mChangeDirID]!!.getTaskCopy(mDirrection)) }
            }
            if (mVideoTasks.containsKey(mChangeDirID)) {
                runBlocking { mVideoTaskQueue.add(mVideoTasks[mChangeDirID]!!.getTaskCopy(mDirrection)) }
            }
            if (mMqttTasks.containsKey(mChangeDirID)) {
                runBlocking { mMqttTaskQueue.add(mMqttTasks[mChangeDirID]!!.getTaskCopy(mDirrection)) }
            }
        }
    }

    /**
     * Функция запуска скрипта в ручную
     *
     * @param id id скрипта
     */
    @ExperimentalCoroutinesApi
    suspend fun playScript(id: Int) {
        if(mManualTriggers.containsKey(id) && mManualTriggers[id]!!.mEnable)
        {
            if(mTextTasks.containsKey(id)) {
                mTextTaskQueue.add(mTextTasks[id]!!.getTaskCopy(mDirrection))
            }
            if(mAudioTasks.containsKey(id)) {
                mAudioTaskQueue.add(mAudioTasks[id]!!.getTaskCopy(mDirrection))
            }
            if(mVideoTasks.containsKey(id)) {
                mVideoTaskQueue.add(mVideoTasks[id]!!.getTaskCopy(mDirrection))
            }
            if(mMqttTasks.containsKey(id)) {
                mMqttTaskQueue.add(mMqttTasks[id]!!.getTaskCopy(mDirrection))
            }
            if(mRouteForward.any { it.mID == id }) onChangeStation(id)
            else if(mRouteBack.any { it.mID == id }) onChangeStation(id)
        }
    }

    /**
     * Получить режим афвтоопределения вектора движения
     *
     * @return режим афвтоопределения вектора движения в виде json
     */
    fun getAutoDir(): JSONObject{
        val str = when(mAutoDirrection){
            AutoDirMode.HARD -> "hard"
            AutoDirMode.SOFT -> "soft"
            else -> "none"
        }
        return JSONObject("""{"autodir":"$str"}""")
    }
}