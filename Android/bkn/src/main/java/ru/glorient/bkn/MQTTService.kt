package ru.glorient.bkn

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import org.eclipse.paho.mqttv5.client.*
import org.eclipse.paho.mqttv5.common.*
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import org.json.JSONException
import org.json.JSONObject
import java.lang.Math.random
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence

/**
 * Класс сервиса MQTT.
 */
class MQTTService : GPSService() {
    /**
     * Клиент MQTT.
     */
    private var mMqttClient: MqttAsyncClient? = null
    /**
     * ID транспортного средства.
     */
    private var mId: Int = 0
    /**
     * Корневой топик для сообщений.
     */
    private var mRootTopic = ""
    /**
     * Параметры подключения к серверу.
     */
    private val mMqttOptions = MqttConnectionOptions()
    /**
     * Память для подключения к серверу.
     */
    private var mPersistence = MemoryPersistence()
    /**
     * Флаг остановки процесса отключения от сервера.
     */
    private val mMqttStopping: AtomicBoolean = AtomicBoolean(false)
    /**
     * Флаг логирования сообщений от всех сервисов.
     */
    private var mLog = false

    /**
     * Класс для сохранения в fifo.
     *
     * @property  topic топик MQTT.
     * @property  message json сообщение MQTT.
     */
    private data class Record(val topic: String, val message: String)
    /**
     * Буфер неотправленных сообщений.
     */
    private val mFIFOBufer: Queue<Record> = LinkedList()

    /**
     * Создание сервиса.
     */
    override fun onCreate() {
        super.onCreate()

        mLocationCallback = object : LocationCallback() {
            @SuppressLint("SimpleDateFormat")
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val loc = locationFilter(locationResult) ?: return

                //Log.d(TAG,"$loc")

                val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

                val res = JSONObject("""{"time":"${dateFormat.format(Date(loc.time))}"}""")
                val position = JSONObject("""{"latitude":${loc.latitude},"longitude":${loc.longitude}}""")
                if(loc.accuracy != null){
                    position.put("accuracy", loc.accuracy)
                }
                res.put("position", position)
                if(loc.speed != 0.0f){
                    val speed = JSONObject("""{"value":${loc.speed}}""")
                    if(loc.speedAccuracy != null){
                        speed.put("accuracy", loc.speedAccuracy)
                    }
                    res.put("speed", speed)
                }

                val bearing = JSONObject("""{"value":${loc.bearing}}""")
                if(loc.bearingAccuracy != null){
                    bearing.put("accuracy", loc.bearingAccuracy)
                }
                res.put("bearing", bearing)

                if(loc.altitude != null){
                    val altitude = JSONObject("""{"value":${loc.altitude}}""")
                    if(loc.altitudeAccuracy != null){
                        altitude.put("accuracy", loc.altitudeAccuracy)
                    }
                    res.put("altitude", altitude)
                }

                if(!publish("GPS", res.toString(), 1)){
                    res.put("fifo", mFIFOBufer.size + 1)
                    mFIFOBufer.add(Record("GPS", res.toString()))
                    if(mFIFOBufer.size > FIFO_SIZE) mFIFOBufer.poll()
                }
                newSending()
            }
        }
    }


    /**
     * Запуск сервиса.
     * https://developer.android.com/reference/android/app/Service#onStartCommand(android.content.Intent,%20int,%20int)
     *
     * @param intent интент (параметры запуска)
     * @param flags флаги запуска
     * @param startId параметр для остановки сервиса
     * @return id сервиса
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        start()
        sendMessage("""{"state":"connecting"}""")
        mMqttStopping.set(false)

        mMainJob = GlobalScope.launch{
            val options = intent.getStringExtra("connection")
            if(options != null){
                try {
                    val cn = JSONObject(options)
                    val server = if(cn.has("server")) cn.getString("server") else "romasty.duckdns.org"
                    val port = if(cn.has("port")) cn.getInt("port") else 8883
                    mId = if(cn.has("transport_id")) cn.getInt("transport_id") else (random()*10000).toInt()
                    mRootTopic = if(cn.has("root"))  "${cn.getString("root")}/$mId" else "navigator/$mId"
                    val ssl = if(cn.has("ssl")) cn.getBoolean("ssl") else true
                    if(cn.has("timeout")) mMqttOptions.connectionTimeout = cn.getInt("timeout")
                    mMqttOptions.userName = if(cn.has("user")) cn.getString("user") else "glorient"
                    mMqttOptions.password  = (if(cn.has("password")) cn.getString("password") else "raQNI6dpZZrd44xoXFPz").toByteArray()
                    mMqttOptions.setWill(
                        "$mRootTopic/status",
                        MqttMessage("""{"client":"offline"}""".toByteArray(),1,true, null)
                    )
                    mMqttOptions.keepAliveInterval = 30
                    mMqttOptions.isCleanStart = true
                    mMqttOptions.isAutomaticReconnect = true
                    val serverURI = (if(ssl) "ssl://" else "tcp://") + "$server:$port"
                    mMqttClient = MqttAsyncClient(serverURI, TAG, mPersistence)
                    mMqttClient!!.setCallback(object : MqttCallback {
                        override fun disconnected(disconnectResponse: MqttDisconnectResponse ){
//                                        Log.d(TAG, "Disconnected")
                            if (!mMqttStopping.get()) {
                                sendMessage("""{"state":"connecting"}""")
                            }
                        }
                        override fun mqttErrorOccurred(exception: MqttException ){
                            Log.d(TAG, "mqttErrorOccurred")
                        }

                        override fun messageArrived(topic: String?, message: MqttMessage?) {
                            try {
                                val msg = JSONObject(message.toString())
                                msg.put("topic", topic)
                                when {
                                    topic!!.endsWith("EGTS") -> sendToEGTS(msg.toString())
                                    topic.endsWith("MQTT") -> sendToMQTT(msg.toString())
                                    topic.endsWith("STM32") -> sendToSTM32(msg.toString())
                                    topic.endsWith("Informer") -> sendToInformer(msg.toString())
                                    else -> sendMessage(msg.toString())
                                }
                            } catch (e: JSONException) {
                                sendMessage("""{"warning":"the message string is not json"}""")
                            }
//                            Log.d(TAG, "Receive message: ${message.toString()} from topic: $topic")
                        }


                        override fun connectComplete(reconnect: Boolean, serverURI: String ) {
                            if (!mMqttStopping.get()) {
                                sendMessage("""{"state":"run"}""")
                                subscribe("cmd/#")
                                publish("status", """{"client":"online"}""", 1, true)
                            }
                        }
                        override fun authPacketArrived(reasonCode: Int, properties: MqttProperties) {
                        }

                        override fun deliveryComplete(token: IMqttToken) {
                        }
                    })

                    if(ssl) {
                        /////SSL ENABLED////
                        val caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
                        caKeyStore.load(null, null)
                        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                        tmf.init(caKeyStore)
                        val context = SSLContext.getInstance("TLS")
                        context.init(null, tmf.trustManagers, null)
                        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                            override fun getAcceptedIssuers(): Array<X509Certificate> {
                                return emptyArray()
                            }

                            @SuppressLint("TrustAllX509TrustManager")
                            @Throws(CertificateException::class)
                            override fun checkClientTrusted(
                                chain: Array<X509Certificate>,
                                authType: String
                            ) {
                            }

                            @SuppressLint("TrustAllX509TrustManager")
                            @Throws(CertificateException::class)
                            override fun checkServerTrusted(
                                chain: Array<X509Certificate>,
                                authType: String
                            ) {
                            }
                        })
                        val sslContext = SSLContext.getInstance("SSL")
                        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                        val sslSocketFactory = sslContext.socketFactory
                        mMqttOptions.socketFactory = sslSocketFactory
                        /////SSL ENABLED////
                    }
                    mMqttClient!!.connect(mMqttOptions);

                    val str = intent.getStringExtra("payload")
                    if(str != null) mChannelRx.send(str)
                }catch (e: JSONException){
                    sendMessage("""{"error":"the connection string is not json"}""")
                    mChannelRx.send("""{"cmd":"stop"}""")
                }
            }else{
                sendMessage("""{"error":"the connection string is absent"}""")
                mChannelRx.send("""{"cmd":"stop"}""")
            }
            while(isActive){
                val str=mChannelRx.receive()
                try {
                    val msg = JSONObject(str)
                    if(msg.has("cmd")) {
                        if (msg.getString("cmd") == "stop") {
                            try {
                                publish("status", "", 1, true)
                                mMqttStopping.set(true)
                                if(mLog){
                                    mLog = false
                                    LocalBroadcastManager.getInstance(this@MQTTService).unregisterReceiver(
                                        logBroadcastReceiver
                                    )
                                }
                                mMqttClient?.disconnect(null, object : MqttActionListener {
                                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                                        Log.d(TAG, "Disconnected")
                                        mMqttClient?.close()
                                    }

                                    override fun onFailure(
                                        asyncActionToken: IMqttToken?,
                                        exception: Throwable?
                                    ) {
                                        sendMessage("""{"warning":"Failed to disconnect"}""")
                                    }
                                })
                            } catch (e: MqttException) {
                                e.printStackTrace()
                            }
                            break
                        }
                    }
                    if(msg.has("next")) {
                        Log.d(TAG, "next!!! ${mFIFOBufer.size}")
                        val rec = mFIFOBufer.poll()
                        if(rec != null){
                            if(!publish(rec.topic, rec.message, 1)){
                                mFIFOBufer.add(rec)
                                if(mFIFOBufer.size > FIFO_SIZE) mFIFOBufer.poll()
                            }
                        }
                    }

                    setGPS(msg)
                    publishMessage(msg)
                    setMessage(msg)
                    changeLog(msg)

                }catch (e: JSONException){
                    sendMessage("""{"warning":"the message string is not json"}""")
                }
            }
            stopLocationUpdates()
            delay(350)
            stopSelf(startId)
        }

        return START_STICKY
    }

    /**
     * Отсылка сообщения сервису STM32.
     *
     * @param payload json команда
     */
    private fun sendToSTM32(payload: String) {
        val intent = Intent(STM32Service.toInName)
        intent.putExtra("payload", payload)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        //Log.d(TAG, "sendToSTM32: ${payload}")
    }

    /**
     * Отсылка сообщения в очередь для сервера MQTT.
     *
     * @param payload json команда
     */
    private fun sendToMQTT(payload: String) {
        GlobalScope.launch{mChannelRx.send(payload)}
    }

    /**
     * Отсылка сообщения сервису ЕГТС.
     *
     * @param payload json команда
     */
    private fun sendToEGTS(payload: String) {
        val intent = Intent(EGTSService.toInName)
        intent.putExtra("payload", payload)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    /**
     * Отсылка сообщения сервису автоинформатора.
     *
     * @param payload json команда
     */
    private fun sendToInformer(payload: String) {
        val intent = Intent(InformerService.toInName)
        intent.putExtra("payload", payload)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    /**
     * Приемник широковещательных сообщений от других сервисов для логирования.
     */
    private val logBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val str = intent?.getStringExtra("payload")
            if (str != null) {
                when (intent.action) {
                    EGTSService.toOutName -> {
                        publish("EGTS", str, 0)
                    }
                    STM32Service.toOutName -> {
                        publish("STM32", str, 0)
                    }
                    InformerService.toOutName -> {
                        publish("Informer", str, 0)
                    }
                }
            }
        }
    }

    /**
     * Включение/выключение логирования.
     *
     * @param msg json команда
     */
    private fun changeLog(msg: JSONObject) {
        if(msg.has("log")) {
            if(msg.getString("log") == "on"){
                if(!mLog){
                    val filter = IntentFilter()
                    filter.addAction(EGTSService.toOutName)
                    filter.addAction(STM32Service.toOutName)
                    filter.addAction(InformerService.toOutName)
                    LocalBroadcastManager.getInstance(this).registerReceiver(
                        logBroadcastReceiver,
                        filter
                    )
                    mLog = true
                }
            } else {
                if(mLog){
                    mLog = false
                    LocalBroadcastManager.getInstance(this).unregisterReceiver(logBroadcastReceiver)
                }
            }
        }
    }

    /**
     * Публикация сообщения в корневой топик.
     *
     * @param msg json команда
     */
    private fun publishMessage(msg: JSONObject) {
        if(msg.has("publish")) {
            val dt = msg.getJSONObject("publish")
            val topic = if(dt.has("topic")) dt.getString("topic") else "out"
            val qos = if(dt.has("qos")) dt.getInt("qos") else 1
            val retain = if(dt.has("retained")) dt.getBoolean("retained") else false
            if(dt.has("data")) {
                val data =  dt.getJSONObject("data")
                publish(topic, data.toString(), qos, retain)
            } else {
                publish(topic, "", qos, retain)
            }
        }
    }

    /**
     * Публикация сообщения в произвольный топик.
     *
     * @param msg json команда
     */
    private fun setMessage(msg: JSONObject) {
        if(msg.has("message")) {
            val dt = msg.getJSONObject("message")
            val topic = if(dt.has("topic")) dt.getString("topic") else ""
            if(dt.has("payload")){
                publish(
                    topic,
                    dt.getJSONObject("payload").toString(),
                    1,
                    retained = false,
                    intopic = false
                )
            }
        }
    }


    /**
     * Публикация.
     *
     * @param topic топик публикации
     * @param msg json строка для публикации
     * @param qos параметр доставки (0,1,2)
     * @param retained флаг сохраненного сообщения
     * @param intopic флаг пибликации в корневой топик
     * @return true в случае успеха
     */
    private fun publish(
        topic: String,
        msg: String,
        qos: Int = 1,
        retained: Boolean = false,
        intopic: Boolean = true
    ): Boolean {
        try {
            if((mMqttClient == null) || (!mMqttClient!!.isConnected) || mMqttStopping.get()){
//                sendMessage("{'warning':'Failed to publish'")
                return false
            }
            val message = MqttMessage()
            message.payload = msg.toByteArray()
            message.qos = qos
            message.isRetained = retained
            val tpc = if(intopic) "$mRootTopic/$topic" else "$topic/$mId"
            mMqttClient?.publish(tpc, message, Record(topic, msg), object : MqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    if (mFIFOBufer.isNotEmpty()) {
                        runBlocking { mChannelRx.send("""{"next":null}""") }
                    }
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    sendMessage("""{"warning":"Failed to publish"}""")
                    try {
                        val s = asyncActionToken?.userContext as Record
                        val rec = JSONObject(s.message)
                        if (s.topic == "GPS") {
                            rec.put("fifo", mFIFOBufer.size + 1)
                            mFIFOBufer.add(Record(s.topic, rec.toString()))
                            if (mFIFOBufer.size > FIFO_SIZE) mFIFOBufer.poll()
                            sendMessage("""{"warning":"GPS","fifo":${mFIFOBufer.size}}""")
                        }
                    } catch (e: Exception) {
                        sendMessage("{'warning':'Failed to republish'")
                    }
                }
            })
            return true
        } catch (e: MqttException) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Подписка на топик.
     *
     * @param topic топик публикации
     * @param qos параметр доставки (0,1,2)
     */
    private fun subscribe(topic: String, qos: Int = 1) {
        try {
            mMqttClient?.subscribe("$mRootTopic/$topic", qos, null, object : MqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Subscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    sendMessage("""{"error":"Failed to subscribe $topic"}""")
                    Log.e(TAG, "Failed to subscribe $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    /**
     * Отослать широковещательное сообщение
     *
     * @param str стока сообщения
     */
    override fun sendMessage(str: String) {
        super.sendMessage(str)
        if(mLog){
            publish("MQTT", str, 0)
        }
    }

    /**
     * Получить имя широковещательного передатчика.
     *
     * @return имя широковещательного передатчика.
     */
    override fun getOuputName(): String = toOutName
    /**
     * Получить имя широковещательного приемника.
     *
     * @return имя широковещательного приемника.
     */
    override fun getInputName(): String = toInName

    companion object {
        /**
         * Сформатировать json команду для публикации.
         *
         * @param topic топик публикации
         * @param msg json строка для публикации
         * @param qos параметр доставки (0,1,2)
         * @param retained флаг сохраненного сообщения
         * @return json команда.
         */
        fun formatMQTTMsg(topic: String, msg: JSONObject, qos: Int = 1, retained: Boolean = false): JSONObject{
            return JSONObject("""{"publish":{"topic":"$topic","qos":$qos,"retained":$retained,"data":$msg}}""")
        }

        /**
         * Сформатировать json команду для логирования.
         *
         * @param str json строка для публикации
         * @return json команда.
         */
        fun formatLog(str: String): JSONObject{
            val current = LocalDateTime.now()
            val res = JSONObject("""{"publish":{"topic":"log","qos":0,"data":{"time":"$current"}}}""")
            val data = res.getJSONObject("publish").getJSONObject("data")
            data.put("msg", str)
            return res
        }

        /**
         * Строка для отладки.
         */
        private val TAG = MQTTService::class.java.simpleName
        /**
         * Имя широковещательного канала на передачу.
         */
        const val toOutName = "fromMQTT"
        /**
         * Имя широковещательного канала на прием.
         */
        const val toInName = "toMQTT"
        /**
         * Размер fifo.
         */
        const val FIFO_SIZE = 10000
    }
}