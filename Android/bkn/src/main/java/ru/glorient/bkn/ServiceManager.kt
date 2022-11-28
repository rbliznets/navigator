package ru.glorient.bkn

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import java.util.zip.ZipInputStream

/**
 * Интерфейс подписчика [ServiceManager].
 *
 * Для структуризации обработки сообщений с разными сервисами
 */
interface IServiceListiner{
    /**
     * Родительский [ServiceManager].
     */
    var mServiceManager: ServiceManager?

    /**
     * Получено сообщение от сервиса.
     *
     * @param tp сервис
     * @param data сообщение
     */
    fun onRecieve(tp: ServiceManager.ServiceType, data: JSONObject)
    /**
     * Изменение состояния сервиса.
     *
     * @param tp сервис
     * @param state состояние
     * @param connection строка соединения
     * @param payload строка первой команды
     */
    fun onStateChange(
        tp: ServiceManager.ServiceType,
        state: ServiceManager.ServiceState,
        connection: String,
        payload: String
    )
}

/**
 * Менеджер сервисов.
 *
 * Запускает и кониролирует сервисы. Позволяет взаимодествовать приложению через подписку к ним.
 * Желательно вместо [onStateChange] и [onRecieve] использовать декораторы наследуемые от [IServiceListiner].
 *
 * @property  mContext Контекст для воспроизведения media.
 * @property  onStateChange функция на событие изменения состояния сервиса, где serviceType сервис, а serviceState его состояние (connection строка соединения, payload строка первой команды).
 * @property  onRecieve функция на получение сообщения от сервиса, где serviceType сервис, а s JSON данные.
 * @property  onSTM32Search функция на изменение подключения STM32 по USB. enable - флаг подключения.
 * @property  onPermissionsRequired запрос на разрешения для основного activity. perms - список требуемых разрешений.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class ServiceManager(
    val mContext: AppCompatActivity,
    val onStateChange: (serviceType: ServiceType, serviceState: ServiceState, connection: String, payload: String) -> Unit = { _: ServiceType, _: ServiceState, _: String, _: String -> },
    val onRecieve: (serviceType: ServiceType, s: JSONObject) -> Unit = { _: ServiceType, _: JSONObject -> },
    val onSTM32Search: (enable: Boolean) -> Unit = { _: Boolean -> },
    val onPermissionsRequired: ((perms: Array<String>) -> Unit)? = null
) {
    /**
     * Список требуемых разрешений
     */
    private var permissionsRequired = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_PHONE_STATE
    )

    /**
     * Типы сервисов
     */
    enum class ServiceType{
        /**
         * Клиент EGTS
         */
        EGTS,
        /**
         * Клиент MQTT
         */
        MQTT,
        /**
         * Сервис внешних устройств через STM32
         */
        STM32,
        /**
         * Информатор
         */
        Informer
    }
    /**
     * Состояние сервиса
     */
    enum class ServiceState {
        /**
         * Запускается
         */
        STARTING,
        /**
         * Соединение
         */
        CONNECTING,
        /**
         * Рабочее состояние
         */
        RUN,
        /**
         * Остановка
         */
        STOPPING,
        /**
         * Сервис остановлен
         */
        STOPED
    }

    /**
     * Сервис загрузки файлов
     */
    private val mDownloadManager = mContext.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
    /**
     * Индификатор загружаемого файла
     */
    private var mDownloadID: Long? = null
    /**
     * Путь куда загружается файл
     */
    private var mDownloadFile: File? = null
    /**
     * Параметры старта сервиса Informer
     */
    private var mDownloadStart: JSONObject? = null

    /**
     * Папка для маршпутов.
     */
    var mRoutesDir = mContext.getExternalFilesDir("routes")?.path

    /**
     * Путь к загруженному файлу маршрута.
     */
    var mRoutesDownloadPath: String = ""

    /**
     * Постановка в очередь загрузки
     *
     * @param msg сообщение от MQTT
     */
    private fun startDownload(msg: JSONObject) {
        val download = msg.getJSONObject("download")
        mDownloadStart = if(download.has("start")){
            download.getJSONObject("start")
        }else{
            null
        }
        if (download.has("zip")) {
            val url = download.getString("zip")
            val fileName = url.substring(url.lastIndexOf('/') + 1)
            mDownloadFile = File(mRoutesDir + File.separator.toString() + fileName)
            val request = DownloadManager.Request(Uri.parse(url))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED) // Visibility of the download Notification
                .setDestinationUri(Uri.fromFile(mDownloadFile)) // Uri of the destination file
                .setTitle(fileName) // Title of the Download Notification
                .setDescription("Загрузка маршрута") // Description of the Download Notification
                .setRequiresCharging(false) // Set if charging is required to begin the download
                .setAllowedOverMetered(true) // Set if download is allowed on Mobile network
                .setAllowedOverRoaming(true) // Set if download is allowed on roaming network
            mDownloadID = mDownloadManager.enqueue(request)
            sendMessage(
                ServiceType.MQTT, MQTTService.formatMQTTMsg(
                    "download",
                    JSONObject("""{"file":"${mDownloadFile?.absolutePath}","status":"started"}""")
                )
            )
        } else {
            if(mDownloadStart?.has("file") == true){
                val file = mRoutesDir + File.separator.toString() + mDownloadStart!!.getString("file")
                val payload = if(mDownloadStart!!.has("payload")) mDownloadStart!!.getJSONObject("payload").toString() else ""
                startService(ServiceType.Informer, """{"file":"$file"}""", payload)
            }
        }
    }

    /**
     * Приемник сообщений от DownloadManager.
     */
    private val mDownloadManagerBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(mDownloadID != null) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (mDownloadID == id){
                    mDownloadID = null
                    try {
                        val zin = ZipInputStream(FileInputStream(mDownloadFile))
                        var entry = zin.nextEntry
                        while(entry != null){
                            val name = entry.name
                            val buf = ByteArray(131172)
                            val size = entry.size  // получим его размер в байтах
                            //Log.d(TAG, "$name -> $size")
                            if(size > 0){
                                val fout = FileOutputStream(mRoutesDir + File.separator.toString() + name)
                                while(true){
                                    val l = zin.read(buf, 0, 131172)
                                    if(l == -1)break
                                    fout.write(buf, 0, l)
                                }
                                fout.flush()
                                zin.closeEntry()
                                fout.close()
                            }else{
                                val folder = File(mRoutesDir + File.separator.toString() + name)
                                if(!folder.exists()){
                                    folder.mkdirs()
                                }
                            }

                            entry = zin.nextEntry
                        }
                        Log.d(TAG, "Unzip OK")
                        if(mDownloadStart != null){
                            if(mDownloadStart?.has("file") == true){
                                val file = mRoutesDir + File.separator.toString() + mDownloadStart!!.getString(
                                    "file"
                                )
                                val payload = if(mDownloadStart!!.has("payload")) mDownloadStart!!.getJSONObject(
                                    "payload"
                                ).toString() else ""
                                startService(ServiceType.Informer, """{"file":"$file"}""", payload)
                                mRoutesDownloadPath = file
                            }
                        }
                        sendMessage(
                            ServiceType.MQTT, MQTTService.formatMQTTMsg(
                                "download", JSONObject(
                                    """{"file":"${mDownloadFile?.absolutePath}","status":"success"}"""
                                )
                            )
                        )
                    }catch (ex: Exception){
                        Log.w(TAG, "Unzip failed ${ex.message}")
                        sendMessage(
                            ServiceType.MQTT, MQTTService.formatMQTTMsg(
                                "download", JSONObject(
                                    """{"file":"${mDownloadFile?.absolutePath}","status":"failed"}"""
                                )
                            )
                        )
                   }
                   mDownloadFile?.delete()
                }
            }
        }
    }

    /**
     * Список подписчиков
     */
    private var mListiners = mutableListOf<IServiceListiner>()

    /**
     * Подписатся
     *
     * @param ls декоратор
     */
    fun subscribe(ls: IServiceListiner){
        if(ls.mServiceManager == null) {
            ls.mServiceManager = this
            mListiners.add(ls)
        }
    }
    /**
     * Отписатся
     *
     * @param ls декоратор
     */
    fun unsubscribe(ls: IServiceListiner){
        if(mListiners.remove(ls)) {
            ls.mServiceManager = null
        }
    }

    /**
     * Событие на изменение сервиса
     *
     * @param tp сервис
     * @param state состояние
     * @param connection строка соединения
     * @param payload строка первой командф
     */
    private fun stateChange(
        tp: ServiceType,
        state: ServiceState,
        connection: String,
        payload: String
    ){
        onStateChange(tp, state, connection, payload)
        mListiners.forEach {
            it.onStateChange(tp, state, connection, payload)
        }
    }
    /**
     * Событие на получение данных от сервиса
     *
     * @param tp сервис
     * @param data JSON данные
     */
    private fun recieve(tp: ServiceType, data: JSONObject){
        onRecieve(tp, data)
        mListiners.forEach {
            it.onRecieve(tp, data)
        }
    }

    /**
     * USB менеджер
     */
    private val mUsbManager: UsbManager =  mContext.getSystemService(AppCompatActivity.USB_SERVICE) as UsbManager
    /**
     * Флаг подключения STM32
     */
    var mSTM32Enable = false
        private set

    /**
     * Приемник сообщений при подключении и отключении устройств по USB.
     */
    private val mUsbBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action!! == "permission") {
                val granted: Boolean = intent.extras!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                if (granted) {
                    mSTM32Enable = true
                    onSTM32Search(mSTM32Enable)
                } else {
                    Log.w(TAG, "USB permission not granted")
                }
            } else if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                if(!mSTM32Enable)checkUSB()
            } else if (intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                if(mSTM32Enable)checkUSB()
            }
        }
    }

    /**
     * Конструктор.
     * Инициализация USB и запрос разрешений.
     */
    init{
        val chan =  NotificationChannel(
            "services",
            "Service Manager channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = (getSystemService(mContext, NotificationManager::class.java) as NotificationManager)
        manager.createNotificationChannel(chan)

        var filter = IntentFilter()
        filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        mContext.registerReceiver(mDownloadManagerBroadcastReceiver, filter)

        filter = IntentFilter()
        filter.addAction("permission")
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        mContext.registerReceiver(mUsbBroadcastReceiver, filter)
        checkUSB()

        val lst = mutableListOf<String>()
        for(s in permissionsRequired){
            if(ContextCompat.checkSelfPermission(mContext, s) != PackageManager.PERMISSION_GRANTED){
                if(!shouldShowRequestPermissionRationale(mContext, s)) lst.add(s)
            }
        }
        if(lst.isNotEmpty()){
            if(onPermissionsRequired != null){
                onPermissionsRequired!!(lst.toTypedArray())
            } else {
                Log.e(TAG, "Need permission: $lst")
            }
        }

//        val deviceId = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * Данные сервиса.
     *
     * @property  mState состояние сервиса.
     * @property  mMessageReceiver приемник сообщений от сервиса.
     * @property  mConnection JSON строка параметров включения сервиса.
     * @property  mPayload JSON строка первой команды после включения сервиса.
     */
    private data class Service(
        var mState: ServiceState,
        val mMessageReceiver: BroadcastReceiver,
        var mConnection: String = "",
        var mPayload: String = ""
    )

    /**
     * Карта сервисов.
     */
    private val mStateMap = mutableMapOf(
        ServiceType.EGTS to Service(ServiceState.STOPED, object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val str = intent.getStringExtra("payload")
                if (str != null) {
                    try {
                        val msg = JSONObject(str)
                        if (msg.has("state")) {
                            when (msg.getString("state")) {
                                "connecting" -> setState(
                                    ServiceType.EGTS,
                                    ServiceState.CONNECTING
                                )
                                "run" -> setState(ServiceType.EGTS, ServiceState.RUN)
                                else -> setState(ServiceType.EGTS, ServiceState.STOPED)
                            }
                            msg.remove("state")
                            if (msg.length() == 0) return
                        }

                        recieve(ServiceType.EGTS, msg)

                    } catch (e: JSONException) {
                        recieve(
                            ServiceType.EGTS,
                            JSONObject("""{"warning":"the message string is not json"}""")
                        )
                        Log.w(TAG, e.toString())
                    }
                }
            }
        }),
        ServiceType.MQTT to Service(ServiceState.STOPED, object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val str = intent.getStringExtra("payload")
                if (str != null) {
                    try {
                        val msg = JSONObject(str)
                        if (msg.has("state")) {
                            when (msg.getString("state")) {
                                "connecting" -> setState(
                                    ServiceType.MQTT,
                                    ServiceState.CONNECTING
                                )
                                "run" -> {
                                    setState(ServiceType.MQTT, ServiceState.RUN)
                                    sendState()
                                }
                                else -> setState(ServiceType.MQTT, ServiceState.STOPED)
                            }
                            msg.remove("state")
                            if (msg.length() == 0) return
                        }
                        if (msg.has("download")) {
                            startDownload(msg)
                            msg.remove("download")
                            if (msg.length() == 0) return
                        }
                        if (msg.has("service")) {
                            val srv = msg.getJSONObject("service")
                            if (srv.has("EGTS")) {
                                val srv2 = srv.getJSONObject("EGTS")
                                if (srv2.has("run")) {
                                    if (srv2.getString("run") == "on") {
                                        val con =
                                            if (srv2.has("connection")) srv2.getJSONObject(
                                                "connection"
                                            ).toString() else ""
                                        val dt =
                                            if (srv2.has("payload")) srv2.getJSONObject("payload")
                                                .toString() else ""
                                        startService(ServiceType.EGTS, con, dt)
                                    } else {
                                        stopService(ServiceType.EGTS)
                                    }
                                }
                            }
                            if (srv.has("Informer")) {
                                val srv2 = srv.getJSONObject("Informer")
                                if (srv2.has("run")) {
                                    if (srv2.getString("run") == "on") {
                                        val con =
                                            if (srv2.has("connection")) srv2.getJSONObject(
                                                "connection"
                                            ).toString() else ""
                                        val dt =
                                            if (srv2.has("payload")) srv2.getJSONObject("payload")
                                                .toString() else ""
                                        startService(ServiceType.Informer, con, dt)
                                    } else {
                                        stopService(ServiceType.Informer)
                                    }
                                }
                            }
                            if (srv.has("STM32")) {
                                val srv2 = srv.getJSONObject("STM32")
                                if (srv2.has("run")) {
                                    if (srv2.getString("run") == "on") {
                                        val con =
                                            if (srv2.has("connection")) srv2.getJSONObject(
                                                "connection"
                                            ).toString() else ""
                                        val dt =
                                            if (srv2.has("payload")) srv2.getJSONObject("payload")
                                                .toString() else ""
                                        startService(ServiceType.STM32, con, dt)
                                    } else {
                                        stopService(ServiceType.STM32)
                                    }
                                }
                            }
                            if (srv.has("MQTT")) {
                                val srv2 = srv.getJSONObject("MQTT")
                                if (srv2.has("run")) {
                                    if (srv2.getString("run") == "off") {
                                        stopService(ServiceType.MQTT)
                                    }
                                }
                            }
                            msg.remove("service")
                            if (msg.length() == 0) return
                        }

                        recieve(ServiceType.MQTT, msg)

                    } catch (e: JSONException) {
                        recieve(
                            ServiceType.MQTT,
                            JSONObject("""{"warning":"the message string is not json"}""")
                        )
                        Log.w(TAG, e.toString())
                    }
                }
            }
        }),
        ServiceType.STM32 to Service(ServiceState.STOPED, object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val str = intent.getStringExtra("payload")
                if (str != null) {
                    try {
                        val msg = JSONObject(str)
                        if (msg.has("state")) {
                            when (msg.getString("state")) {
                                "connecting" -> setState(
                                    ServiceType.STM32,
                                    ServiceState.CONNECTING
                                )
                                "run" -> setState(ServiceType.STM32, ServiceState.RUN)
                                else -> setState(ServiceType.STM32, ServiceState.STOPED)
                            }
                            msg.remove("state")
                            if (msg.length() == 0) return
                        }

                        recieve(ServiceType.STM32, msg)

                    } catch (e: JSONException) {
                        recieve(
                            ServiceType.STM32,
                            JSONObject("""{"warning":"the message string is not json"}""")
                        )
                        Log.w(TAG, e.toString())
                    }
                }
            }
        }),
        ServiceType.Informer to Service(ServiceState.STOPED, object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val str = intent.getStringExtra("payload")
                if (str != null) {
                    try {
                        val msg = JSONObject(str)
                        if (msg.has("state")) {
                            when (msg.getString("state")) {
                                "connecting" -> setState(
                                    ServiceType.Informer,
                                    ServiceState.CONNECTING
                                )
                                "run" -> setState(ServiceType.Informer, ServiceState.RUN)
                                else -> setState(ServiceType.Informer, ServiceState.STOPED)
                            }
                            msg.remove("state")
                            if (msg.length() == 0) return
                        }

                        recieve(ServiceType.Informer, msg)

                    } catch (e: JSONException) {
                        recieve(
                            ServiceType.Informer,
                            JSONObject("""{"warning":"the message string is not json"}""")
                        )
                        Log.w(TAG, e.toString())
                    }
                }
            }
        })
    )

    /**
     * Стандартная функция для логирования в MQTT.
     *
     * @param s строка для логирования
     */
    fun log(s: String) {
        sendMessage(ServiceType.MQTT, MQTTService.formatLog(s))
    }

    /**
     * Поссылка состояний в MQTT.
     */
    private fun sendState() {
        mStateMap.forEach{
            sendServiceState(it.key, it.value)
        }
    }

    /**
     * Очистка состояний в MQTT.
     */
    private fun clearState() {
        mStateMap.forEach{
            sendMessage(
                ServiceType.MQTT,
                JSONObject("""{"publish":{"topic":"services/${it.key}","retained":true}}""")
            )
        }
    }

    /**
     * Поссылка состояния в MQTT.
     *
     * @param type тип сервиса
     * @param service данные сервиса
     */
    private fun sendServiceState(type: ServiceType, service: Service) {
        val data = JSONObject("""{"state":"${service.mState}"}""")
        if ((service.mState != ServiceState.STOPED) && (service.mConnection != ""))
            data.put("connection", JSONObject(service.mConnection))
        val msg = MQTTService.formatMQTTMsg("services/${type}", data, 1, true)
        sendMessage(ServiceType.MQTT, msg)
    }

    /**
     * Получить состояние сервиса.
     *
     * @param service тип сервиса
     * @return состояние сервиса
     */
    private fun getState(service: ServiceType) = mStateMap[service]?.mState
    /**
     * Обновление состояния сервиса.
     *
     * @param service тип сервиса
     * @param state состояние сервиса
     */
    private fun setState(service: ServiceType, state: ServiceState){
        mStateMap[service]?.mState = state
        if(service != ServiceType.MQTT){
            sendServiceState(service, mStateMap[service]!!)
        }
        if(state == ServiceState.STARTING) {
            when(service) {
                ServiceType.EGTS -> LocalBroadcastManager.getInstance(mContext).registerReceiver(
                    mStateMap[service]!!.mMessageReceiver,
                    IntentFilter(EGTSService.toOutName)
                )
                ServiceType.MQTT -> LocalBroadcastManager.getInstance(mContext).registerReceiver(
                    mStateMap[service]!!.mMessageReceiver,
                    IntentFilter(MQTTService.toOutName)
                )
                ServiceType.STM32 -> LocalBroadcastManager.getInstance(mContext).registerReceiver(
                    mStateMap[service]!!.mMessageReceiver,
                    IntentFilter(STM32Service.toOutName)
                )
                ServiceType.Informer -> LocalBroadcastManager.getInstance(mContext)
                    .registerReceiver(
                        mStateMap[service]!!.mMessageReceiver,
                        IntentFilter(InformerService.toOutName)
                    )
                else -> {}
            }
        }else if(state == ServiceState.STOPED){
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mStateMap[service]!!.mMessageReceiver)
       }
       stateChange(service, state, mStateMap[service]!!.mConnection, mStateMap[service]!!.mPayload)
    }

    /**
     * Запустить сервис.
     *
     * @param service тип сервиса
     * @param connection параметры соединение
     * @param payload первое сообщение сервису
     */
    fun startService(service: ServiceType, connection: String = "", payload: String = ""){
//        stopService(service)
//        while (getState(service) != ServiceState.STOPED) {}
        mStateMap[service]?.mConnection = connection
        mStateMap[service]?.mPayload = payload
        when(service) {
            ServiceType.EGTS -> {
                setState(service, ServiceState.STARTING)
                Intent(mContext, EGTSService::class.java).also {
                    if (connection != "") it.putExtra("connection", connection)
                    if (payload != "") it.putExtra("payload", payload)
                    mContext.startService(it)
                }
            }
            ServiceType.MQTT -> {
                setState(service, ServiceState.STARTING)
                Intent(mContext, MQTTService::class.java).also {
                    if (connection != "") it.putExtra("connection", connection)
                    if (payload != "") it.putExtra("payload", payload)
                    mContext.startService(it)
                }
            }
            ServiceType.STM32 -> {
                if (mSTM32Enable) {
                    setState(service, ServiceState.STARTING)
                    Intent(mContext, STM32Service::class.java).also {
                        it.putExtra("connection", STM32CONNECTION)
                        mContext.startService(it)
                    }
                }
            }
            ServiceType.Informer -> {
                setState(service, ServiceState.STARTING)
                Intent(mContext, InformerService::class.java).also {
                    if (connection != "") it.putExtra("connection", connection)
                    if (payload != "") it.putExtra("payload", payload)
                    mContext.startService(it)
                }
            }
        }
    }

    /**
     * Послать сообщение сервису.
     *
     * @param service тип сервиса
     * @param msg сообщение
     */
    fun sendMessage(service: ServiceType, msg: JSONObject): Boolean {
        if(getState(service) == ServiceState.STOPED) return false
        val intent: Intent = when(service){
            ServiceType.EGTS -> Intent(EGTSService.toInName)
            ServiceType.MQTT -> Intent(MQTTService.toInName)
            ServiceType.STM32 -> Intent(STM32Service.toInName)
            ServiceType.Informer -> Intent(InformerService.toInName)
            else -> return false
        }
        intent.putExtra("payload", msg.toString())
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent)
        return true
    }

    /**
     * Остановить сервис.
     *
     * @param service тип сервиса
     */
    fun stopService(service: ServiceType){
        while(getState(service) == ServiceState.STARTING){}
        if((getState(service) != ServiceState.STOPPING) && (getState(service) != ServiceState.STOPED)){
            when(service) {
                ServiceType.EGTS -> {
                    setState(service, ServiceState.STOPPING)
                    Intent(mContext, EGTSService::class.java).also { mContext.stopService(it) }
                }
                ServiceType.MQTT -> {
                    clearState()
                    setState(service, ServiceState.STOPPING)
                    Intent(mContext, MQTTService::class.java).also { mContext.stopService(it) }
                }
                ServiceType.STM32 -> {
                    setState(service, ServiceState.STOPPING)
                    Intent(mContext, STM32Service::class.java).also { mContext.stopService(it) }
                }
                ServiceType.Informer -> {
                    setState(service, ServiceState.STOPPING)
                    Intent(mContext, InformerService::class.java).also { mContext.stopService(it) }
                }
                else -> {}
            }
        }
    }

    /**
     * Проверить подключение STM32 по USB.
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun checkUSB() {
        val usbDevices: HashMap<String, UsbDevice>? = mUsbManager.deviceList
        if (!usbDevices?.isEmpty()!!) {
            var keep = true
            usbDevices.forEach{ entry ->
                val deviceVendorId: Int = entry.value.vendorId
                val deviceProductId: Int = entry.value.productId
                if ((deviceVendorId == STM32_VENDOR_ID) && (deviceProductId == STM32_PRODUCT_ID)) {
                    val intent: PendingIntent = PendingIntent.getBroadcast(
                        mContext,
                        0,
                        Intent("permission"),
                        0
                    )
                    Log.i(TAG, "USB: $deviceVendorId:$deviceProductId STM32")
//                    if(!mUsbManager.hasPermission(entry.value)) {
                        mUsbManager.requestPermission(entry.value, intent)
                        keep = false
//                    }else{
//                        mSTM32Enable = true
//                        onSTM32Search(mSTM32Enable)
//                    }
                } else {
                    Log.i(TAG, "USB: $deviceVendorId:$deviceProductId")
                }
            }
            if (keep) {
                if(mSTM32Enable){
                    mSTM32Enable = false
                    onSTM32Search(mSTM32Enable)
                }
            }
        } else {
            Log.i(TAG, "no usb device connected")
            if(mSTM32Enable){
                mSTM32Enable = false
                onSTM32Search(mSTM32Enable)
            }
        }
    }

    companion object{
        /**
         * Vendor STM32.
         */
        private const val STM32_VENDOR_ID = 1155
        /**
         * Product STM32.
         */
        private const val STM32_PRODUCT_ID = 22336
        /**
         * Строка соединения с сервисом STM32.
         */
        private const val STM32CONNECTION = """{"vendorid":$STM32_VENDOR_ID,"productid":$STM32_PRODUCT_ID}"""
        /**
         * Строка для отладки.
         */
        private val TAG = ServiceManager::class.java.simpleName

        /**
         * Команда по умолчанию после запуска mqtt сервиса.
         */
        const val MQTT_START_PAYLOAD = """{"GPS":{"run":"on","interval":3000,"fastestInterval":1000, "filter":{"type":2,"minInterval":60000}},"log":"on"}"""
        /**
         * Команда по умолчанию после запуска ЕГТС сервиса.
         */
        const val EGTS_START_PAYLOAD = """{"GPS":{"run":"on","interval":1500,"fastestInterval":1000, "filter":{"type":1,"minInterval":3000,"dutyCycle":10}}}"""
        /**
         * Команда по умолчанию после запуска информационного сервиса.
         */
        const val INFO_START_PAYLOAD = """{"GPS":{"run":"on","interval":1500,"fastestInterval":1000}}"""
    }

}