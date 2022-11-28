package ru.glorient.bkn

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.timer

/**
 * Соединить 2 JSONObject
 *
 * @param json добовляемый JSONObject
 */
fun JSONObject.add(json: JSONObject) {
    val keys = json.keys()
    keys.forEach {
        put(it, json[it])
    }
}

/**
 * Родительский класс сервиса
 *
 * Управление сервиса через широковещательный канал [mMessageReceiver] json командой в "payload".
 * Результат выдаётся в широковещательный канал.
 * Сихронизация корутины [mMainJob] через канал [mChannelRx]
 *
 * @param length максимальное количество сообщений в очереди [mChannelRx]
 */
abstract class ParentService(length: Int = 100) : Service() {
    /**
     * Канал входящих сообщений из [mMessageReceiver]
     */
    protected val mChannelRx = Channel<String>(length)
    /**
     * Основная корутина
     */
    protected var mMainJob: Job? = null

    /**
     * Приемник широковещательных сообщений
     */
    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val str = intent.getStringExtra("payload")
            if (str != null) runBlocking { mChannelRx.send(str) }
        }
    }

    /**
     * Отослать широковещательное сообщение
     *
     * @param str стока сообщения
     */
    protected open fun sendMessage(str: String) {
        val intent = Intent(getOuputName())
        intent.putExtra("payload", str)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    /**
     * Начало работы
     *
     * Запускать в начале [onStartCommand].
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    protected fun start(){
        runBlocking { mMainJob?.cancelAndJoin() }
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(
                    mMessageReceiver,
                    IntentFilter(getInputName())
                )

        val pendingIntent: PendingIntent =
            Intent(this, ParentService::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        val notification: Notification = Notification.Builder(this, "services")
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    /**
     * Завершение работы сервиса
     */
     override fun onDestroy() {
        runBlocking {
            sendMessage("""{"state":"stopped"}""")
            mChannelRx.send("""{"cmd":"stop"}""")
            mMainJob?.join()
            while (!mChannelRx.isEmpty) mChannelRx.receive()
            mMainJob = null
        }
        stopForeground(true)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver)
        super.onDestroy()
    }

    /**
     * Заглушка.
     *
     * Данный механизм не используется.
     */
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    /**
     * Получить имя широковещательного передатчика.
     *
     * @return имя широковещательного передатчика.
     */
    abstract fun getOuputName(): String
    /**
     * Получить имя широковещательного приемника.
     *
     * @return имя широковещательного приемника.
     */
    abstract fun getInputName(): String
}

/**
 * Родительский класс сервиса, использующего данные локации.
 *
 * Настройка секрвиса локации:
 * {
 *  'GPS':{
 *      'run':'on',		        <- on|off включить/выключить передачу
 *      'interval':3000,	    <- интервал передачи в мсек
 *      'fastestInterval':1000,	<- минимальный интервал передачи в мсек
 *      'filter':{	            <- настройки дополнительных параметров
 *          'type':1,           <- алгоритм фильтпации (0 - нет, 1 - определение стоянки, 2 - определение стоянки и курса). По умолчанию 1.
 *          'minInterval':3000, <- Для типа 1: минимальное время между сообщениями. Для типа 2: максимальное время между сообщениями. По умолчанию 0(выключено).
 *          'dutyCycle':10,     <- Для типа 1: максимальное время между сообщениями на стоянке minInterval*dutyCycle. По умолчанию 1.
 *          'sensors':'on'     <- on/off: использование акселерометра и магнитометра. По умолчанию on.
 *      }
 * }
 *
 * @param length максимальное количество сообщений в очереди
 */
abstract class GPSService(length: Int = 100) : ParentService(length) {
    /**
     * Provides access to the Fused Location Provider API.
     */
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    /**
     * Provides access to the Location Settings API.
     */
    private var mSettingsClient: SettingsClient? = null
    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private var mLocationRequest: LocationRequest? = null
    /**
     * Callback for Location events.
     */
    protected var mLocationCallback: LocationCallback? = null
    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private var mLocationSettingsRequest: LocationSettingsRequest? = null
    /**
     * Интервал фильтра в мс для посылки в сеть.
     */
    private var mMinInterval: Long = 0
    /**
     * Тип втльтра.
     */
    private var mFilter = 1

    /**
     * Флаг синхронизации процесса подключения к сервису локаций.
     */
    private val mGpsEnableWait: AtomicBoolean = AtomicBoolean(false)
    /**
     * Флаг разрешения работы.
     */
    private var mGpsAccess = false
    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    private fun startLocationUpdates() {
        if(mGpsAccess) {
            mGpsEnableWait.set(true)
            mLocationSettingsRequest?.let {
                mSettingsClient!!.checkLocationSettings(it)
                    .addOnSuccessListener {
                        if (ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED) {
                            sendMessage("""{"error":"GPS is failed"}""")
                            mGpsEnableWait.set(false)
                        } else {
                            try {
                                if(mLocationCallback != null) {
                                    mLocationRequest?.let { it1 ->
                                        Looper.myLooper()?.let { it2 ->
                                            mFusedLocationClient!!.requestLocationUpdates(
                                                it1,
                                                mLocationCallback!!,
                                                it2
                                            )
                                        }
                                    }
                                }
                                mGpsEnableWait.set(false)
                            }catch (e: Exception){
                                mGpsAccess = false
                                mGpsEnableWait.set(false)
                                sendMessage("""{"error":"Location is failed"}""")
                            }
                        }
                    }
                    .addOnFailureListener {
                        mGpsAccess = false
                        mGpsEnableWait.set(false)
                        sendMessage("""{"error":"Starting of Location is failed"}""")
                    }
            }

            if(mSensors) {
                mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
                mAccel?.also { s ->
                    mSensorManager.registerListener(
                        mSensorEventListener,
                        s,
                        SensorManager.SENSOR_DELAY_NORMAL
                    )
                }
                val sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
                sensor?.also { s ->
                    mSensorManager.registerListener(
                        mSensorEventListener,
                        s,
                        SensorManager.SENSOR_DELAY_NORMAL
                    )
                    mCompass = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
                    mCompass?.also { s1 ->
                        mSensorManager.registerListener(
                            mSensorEventListener,
                            s1,
                            SensorManager.SENSOR_DELAY_NORMAL
                        )
                    }
                }
            }

            while (mGpsEnableWait.get()) {
            }
        }
    }
    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected fun stopLocationUpdates() {
        if(mGpsAccess){
            if(mLocationCallback != null) {
                mFusedLocationClient!!.removeLocationUpdates(mLocationCallback!!)
            }

            mSensorManager.unregisterListener(mSensorEventListener)
        }
    }

    /**
     * Создание сервиса.
     */
    override fun onCreate() {
        super.onCreate()

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        mGpsAccess=(PermissionChecker.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PermissionChecker.PERMISSION_GRANTED)

        if(mGpsAccess) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            mSettingsClient = LocationServices.getSettingsClient(this)

            mLocationRequest = LocationRequest.create()?.apply {
                interval = 10000
                fastestInterval = 5000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            val builder = LocationSettingsRequest.Builder()
            builder.addLocationRequest(mLocationRequest!!)
            mLocationSettingsRequest = builder.build()
        }
    }

    /**
     * Последняя позиция.
     */
    private var mLastLocation: Location? = null

    /**
     * Таймер для отчета времени.
     *
     * Для обхода засыпания GPS модуля время запросов от LocationService устанавливается в 1-3 сек,
     * а таймер нужен для большего переода отсылки пакетов.
     */
    private var mTimer: Timer? = null
    /**
     * Флаг срабатывания таймера.
     */
    private val mTimerEnd = AtomicBoolean(true)
    /**
     * Счетчик таймера.
     */
    private val mTimerCount = AtomicLong(0L)
    /**
     * Скважность для отсылки сообщения во время стоянки.
     */
    private var mDutyCycle = 1

    /**
     * Обновить таймер после посылки сообщения.
     */
    protected fun newSending(){
        if (mMinInterval > 0L){
            mTimer?.cancel()
            mTimerEnd.set(false)
            mTimerCount.set(0L)
            mTimer = timer(
                initialDelay = mMinInterval,
                period = mMinInterval,
                action = {
                    mTimerEnd.set(true)
                    mTimerCount.incrementAndGet()
                }
            )
         }
    }

    /**
     * Вектор ускорения гравитации.
     */
    private val mGravity = FloatArray(3)
    /**
     * Курс по магнитометру.
     */
    private var mCourse = 0
    /**
     * Коррекция курса между магнитометром и GPS.
     */
    private var mCourseCorrection: Int? = null

    /**
     * Обработка сообщений от сенсоров.
     */
    private var mSensorEventListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            val str = event?.sensor?.name
            if (str == "Linear Acceleration") {
                var x = 0.0
                event.values?.forEach { x += it * it }
                if (x > MOTION_LEVEL_DETECTOR){
                    mMotionDetect = true
                    //Log.d("Sensor", str!!+" $x")
                }
            }else if(str?.contains("Magnetometer") == true) {
                val r = FloatArray(9)
                val i = FloatArray(9)
                if(SensorManager.getRotationMatrix(r, i, mGravity, event.values)){
                    val res = FloatArray(3)
                    SensorManager.getOrientation(r, res)
                    var course = (((res[0]+Math.PI)/(2*Math.PI))*360).toInt()
                    course -= 90
                    if(course < 0) course+=360
                    mCourse = course
                    //Log.d("Sensor","course $mCourse")
                }
            }else if(str?.contains("Gravity") == true) {
                event.values?.copyInto(mGravity, 0, 0, 3)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            //Log.d("Sensor","${sensor?.name}: $accuracy")
        }
    }

    /**
     * Флаг стоянки.
     */
    private var mOldLocation = false
    /**
     * Флаг исполбзования дополнительных сенсоров.
     */
    private var mSensors = true
    /**
     * Сервис сенсоров.
     */
    private lateinit var mSensorManager: SensorManager
    /**
     * Сенсор ускорения.
     */
    private var mAccel: Sensor? = null
    /**
     * Сенсор магнитного поля.
     */
    private var mCompass: Sensor? = null
    /**
     * Флаг определения срабатывания сенсора ускорения.
     */
    protected var mMotionDetect = false
        get() = if(mAccel != null) field else true

    /**
     * Обновленная локация.
     *
     * @property  time Время в мсек с 01.01.1970.
     * @property  latitude широта.
     * @property  longitude долгота.
     * @property  accuracy точность позиции в метрах (сигма) если есть.
     * @property  speed скорость в км/ч.
     * @property  speedAccuracy точность скорости в км/ч (сигма) если есть.
     * @property  bearing курс в градусах.
     * @property  bearingAccuracy точность курса в градусах (сигма) если есть.
     * @property  altitude высота в метрах если есть.
     * @property  altitudeAccuracy точность высоты в метрах (сигма) если есть.
     */
    protected data class CorrectedLocation(
        val time: Long,
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float?,
        val speed: Float,
        val speedAccuracy: Float?,
        val bearing: Float,
        val bearingAccuracy: Float?,
        val altitude: Double?,
        val altitudeAccuracy: Float?
    )

    /**
     * Обработка данных от LocationService
     *
     * @param locationResult текущии координаты
     * @return новые данные для передачи серверу, если они есть.
     */
    protected fun locationFilter(locationResult: LocationResult): CorrectedLocation? {
        return when(mFilter){
            1 -> locationFilter1(locationResult)
            2 -> locationFilter2(locationResult)
            else -> locationFilter0(locationResult)
        }
    }

    /**
     * Обработка данных от LocationService.
     *
     * @param locationResult текущии координаты
     * @return новые данные для передачи серверу, если они есть.
     */
    private fun locationFilter0(locationResult: LocationResult): CorrectedLocation {
        val currentLocation = locationResult.lastLocation

        if(currentLocation.hasBearingAccuracy() and (mCourseCorrection == null))mCourseCorrection = currentLocation.bearing.toInt()-mCourse
        val bearing = if(currentLocation.hasBearingAccuracy())currentLocation.bearing else {
            if(mCompass!=null) {
                if(mCourseCorrection != null) {
                    if ((mCourse + mCourseCorrection!!) < 0) (mCourse + mCourseCorrection!! + 360).toFloat()
                    else if ((mCourse + mCourseCorrection!!) > 360) (mCourse + mCourseCorrection!! - 360).toFloat()
                    else (mCourse + mCourseCorrection!!).toFloat()
                }else  currentLocation.bearing
            }else currentLocation.bearing
        }

        return CorrectedLocation(
            currentLocation.time,
            currentLocation.latitude,
            currentLocation.longitude,
            if (currentLocation.hasAccuracy()) currentLocation.accuracy else null,
            if (currentLocation.hasSpeed()) currentLocation.speed * 3.6f else 0.0F,
            if (currentLocation.hasSpeedAccuracy()) currentLocation.speedAccuracyMetersPerSecond * 3.6f else null,
            bearing,
            if (currentLocation.hasBearingAccuracy()) currentLocation.bearingAccuracyDegrees else null,
            if (currentLocation.hasAltitude()) currentLocation.altitude else null,
            if (currentLocation.hasVerticalAccuracy()) currentLocation.verticalAccuracyMeters else null
        )
    }

    /**
     * Обработка данных от LocationService по алгоритму оптимизированному под ЕГТС.
     *
     * @param locationResult текущии координаты
     * @return новые данные для передачи серверу, если они есть.
     */
    private fun locationFilter1(locationResult: LocationResult): CorrectedLocation?{
        val currentLocation = locationResult.lastLocation
        var  moving = true
        if(mLastLocation == null) mLastLocation = currentLocation
        else moving = currentLocation.distanceTo(mLastLocation!!) > (currentLocation.accuracy + mLastLocation!!.accuracy)

        if(currentLocation.hasBearingAccuracy())mCourseCorrection= currentLocation.bearing.toInt()-mCourse

        if(!mTimerEnd.get()) return null

        val alt: Double? = if(currentLocation.hasVerticalAccuracy()) currentLocation.altitude else null
        var speed = 0.0
        if(currentLocation.hasSpeedAccuracy()){
            if(currentLocation.speed > (3* currentLocation.speedAccuracyMetersPerSecond)){
                if(!mOldLocation or mMotionDetect) {
                    speed = currentLocation.speed * 3.6 //to km per hour
                    mLastLocation = currentLocation
                }
            }
        }else{
            if(!mOldLocation or mMotionDetect) {
                speed = currentLocation.speed * 3.6 //to km per hour
                mLastLocation = currentLocation
            }
        }

        //Log.d(TAG,"$speed $mOldLocation $mMotionDetect")
        mMotionDetect = false
        if(speed == 0.0){
            mOldLocation = true
            if(currentLocation.hasAccuracy()){
                if(mLastLocation!!.hasAccuracy()){
                    if((currentLocation.accuracy < mLastLocation!!.accuracy) or moving){
//                        Log.d(
//                            "accuracy",
//                            "${currentLocation.accuracy} < ${mLastLocation!!.accuracy},$moving"
//                        )
                        mLastLocation = currentLocation
                        moving = true
                    }
                }else{
                    mLastLocation = currentLocation
                    moving = true
                }
            }
        }else{
            mOldLocation = false
        }
        mLastLocation!!.time = currentLocation.time

        if(moving){
            //Log.d(TAG, "move ${mTimerCount.get()}")
        }else{
            //Log.d(TAG, "stop ${mTimerCount.get()}")
            if((mMinInterval > 0L) and (mTimerCount.get() < mDutyCycle) and !mMotionDetect){
                return null
            }
        }

        val bearing = if(currentLocation.hasBearingAccuracy())mLastLocation!!.bearing else {
            if(mCompass!=null) {
                if(mCourseCorrection != null) {
                    if ((mCourse + mCourseCorrection!!) < 0) (mCourse + mCourseCorrection!! + 360).toFloat()
                    else if ((mCourse + mCourseCorrection!!) > 360) (mCourse + mCourseCorrection!! - 360).toFloat()
                    else (mCourse + mCourseCorrection!!).toFloat()
                }else  mCourse.toFloat()
            }else mLastLocation!!.bearing
        }

        return CorrectedLocation(
            mLastLocation!!.time,
            mLastLocation!!.latitude,
            mLastLocation!!.longitude,
            if (mLastLocation!!.hasAccuracy()) mLastLocation!!.accuracy else null,
            speed.toFloat(),
            if (mLastLocation!!.hasSpeedAccuracy()) mLastLocation!!.speedAccuracyMetersPerSecond * 3.6f else null,
            bearing,
            if (mLastLocation!!.hasBearingAccuracy()) mLastLocation!!.bearingAccuracyDegrees else null,
            alt,
            if (mLastLocation!!.hasVerticalAccuracy()) mLastLocation!!.verticalAccuracyMeters else null,
        )
    }

    /**
     * Обработка данных от LocationService по алгоритму оптимизированному под количество передаваемых точек.
     *
     * @param locationResult текущии координаты
     * @return новые данные для передачи серверу, если они есть.
     */
    private fun locationFilter2(locationResult: LocationResult): CorrectedLocation?{
        val currentLocation = locationResult.lastLocation

        if((!currentLocation.hasSpeedAccuracy()) or (!currentLocation.hasBearingAccuracy()) or ( currentLocation.speed < currentLocation.speedAccuracyMetersPerSecond) ){
            currentLocation.speed = 0.0F
        }

        if(mLastLocation == null) mLastLocation = currentLocation
        else {
            if (mCourseCorrection == null) {
                if (currentLocation.hasBearingAccuracy()) mCourseCorrection =
                    currentLocation.bearing.toInt() - mCourse
            } else if (currentLocation.hasBearingAccuracy() and (currentLocation.speed > 4.1F)) mCourseCorrection =
                currentLocation.bearing.toInt() - mCourse

            if (mCompass != null) {
                if ((mCourseCorrection != null) && (!currentLocation.hasBearingAccuracy())) {
                    currentLocation.bearing =
                        if ((mCourse + mCourseCorrection!!) < 0) (mCourse + mCourseCorrection!! + 360).toFloat()
                        else if ((mCourse + mCourseCorrection!!) > 360) (mCourse + mCourseCorrection!! - 360).toFloat()
                        else (mCourse + mCourseCorrection!!).toFloat()
                }
            }
            val d = mLastLocation!!.distanceTo(currentLocation)
            var b = mLastLocation!!.bearingTo(currentLocation)
            if (b < 0) b += 360
            val angle = anglesDelta(currentLocation.bearing, b)

            if (mTimerEnd.get()) {
                mLastLocation = currentLocation
            } else if (((mLastLocation!!.speed == 0.0f) and (currentLocation.speed != 0.0f)) or ((currentLocation.speed == 0.0f) && (mLastLocation!!.speed != 0.0f))) {
                mLastLocation = currentLocation
            } else if ((mLastLocation!!.speed == 0.0f) and (currentLocation.speed == 0.0f)) {
                mLastLocation = if (currentLocation.hasAccuracy() and mLastLocation!!.hasAccuracy()) {
                    if (d > currentLocation.accuracy + mLastLocation!!.accuracy) {
                        currentLocation
                    } else return null
                } else if (currentLocation.hasAccuracy()) {
                    currentLocation
                } else return null
            } else if (angle < MOTION_ANGLE_DETECTOR) {
                return null
            } else if (currentLocation.accuracy < d) {
                mLastLocation = currentLocation
            } else return null
        }

        return CorrectedLocation(
            mLastLocation!!.time,
            mLastLocation!!.latitude,
            mLastLocation!!.longitude,
            if (mLastLocation!!.hasAccuracy()) mLastLocation!!.accuracy else null,
            mLastLocation!!.speed * 3.6f,
            if (mLastLocation!!.hasSpeedAccuracy()) mLastLocation!!.speedAccuracyMetersPerSecond * 3.6f else null,
            mLastLocation!!.bearing,
            if (mLastLocation!!.hasBearingAccuracy()) mLastLocation!!.bearingAccuracyDegrees else null,
            if (mLastLocation!!.hasVerticalAccuracy()) mLastLocation!!.altitude else null,
            if (mLastLocation!!.hasVerticalAccuracy()) mLastLocation!!.verticalAccuracyMeters else null,
        )
    }

    /**
     * Вычисление разницы между углами.
     *
     * @param a первый угол в градусах
     * @param b второй угол в градусах
     * @return модуль разности в градусах
     */
    private fun anglesDelta(a: Float, b: Float): Float{
        val r1: Float
        val r2: Float
        if( a > b ){
            r1 = a - b
            r2 = b-a+360
        }else{
            r1 = b - a
            r2 = a - b+360
        }
        return if(r1>r2) r2 else r1
    }

    /**
     * Обработка команды подключения к сервису лакации.
     *
     * @param msg сообщение от широковещательного приемника
     */
    protected fun setGPS(msg: JSONObject) {
        if (msg.has("GPS")) {
            if(mGpsAccess) {
                val gps = msg.getJSONObject("GPS")
                stopLocationUpdates()

                mLocationRequest = LocationRequest.create()?.apply {
                    if (gps.has("interval")) {
                        interval = gps.getLong("interval")
                    } else interval = 30000
                    if (gps.has("fastestInterval")) {
                        fastestInterval = gps.getLong("fastestInterval")
                    } else fastestInterval = 2500
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                }

                mTimer?.cancel()
                mTimerEnd.set(true)
                mDutyCycle = 1
                mFilter = 1
                mSensors = true
                mMinInterval = 0
                if (gps.has("filter")) {
                    val filter = gps.getJSONObject("filter")
                    if(filter.has("type")){
                        mFilter = filter.getInt("type")
                    }
                    if (filter.has("minInterval")) {
                        mMinInterval = filter.getLong("minInterval")
                        if (filter.has("dutyCycle")) {
                            mDutyCycle = filter.getInt("dutyCycle")
                        }
                    }
                    if (filter.has("sensors")) {
                        mSensors = filter.getString("sensors") == "on"
                    }
                }

                val builder = LocationSettingsRequest.Builder()
                builder.addLocationRequest(mLocationRequest!!)
                mLocationSettingsRequest = builder.build()

                if (gps.has("run")) {
                    if (gps.getString("run") == "off") return
                }
                mLastLocation = null
                startLocationUpdates()
            } else sendMessage("""{"error":"GPS access denied"}""")
        }
    }

    companion object {
        /**
         * Уровень определения движения по датчику ускорения (m^2/s)^2.
         */
        const val MOTION_LEVEL_DETECTOR: Double = 0.45
        /**
         * Уровень определения изменения курсв в градусах.
         */
        const val MOTION_ANGLE_DETECTOR: Float = 5.0F
    }
}
