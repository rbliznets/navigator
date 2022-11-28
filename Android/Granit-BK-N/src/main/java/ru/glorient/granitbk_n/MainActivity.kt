package ru.glorient.granitbk_n

import android.Manifest
import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import ru.glorient.bkn.ServiceManager
import ru.glorient.granitbk_n.accessory.*
import ru.glorient.granitbk_n.avtoinformer.AvtoInformatorFragment
import ru.glorient.granitbk_n.avtoinformer.AvtoInformatorRepository
import ru.glorient.granitbk_n.avtoinformer.AvtoinformerFragmentNoRoute
import ru.glorient.granitbk_n.camera.CameraFragmentOpenGL
import ru.glorient.granitbk_n.camera.opengl.helpers.ContextHelper
import ru.glorient.granitbk_n.dashboard.DashboardFragment
import ru.glorient.granitbk_n.databinding.ActivityMainBinding
import ru.glorient.granitbk_n.video.VideoFullscreenActivity
import java.io.File
import java.util.*
import kotlin.collections.forEach
import kotlin.collections.forEachIndexed
import kotlin.collections.set

class MainActivity : AppCompatActivity(), UpdateListListener {
    // Флаг запущен ли сервис
    var flagService = false

    lateinit var binding: ActivityMainBinding
    private lateinit var progressBar: ProgressBar

    // Флаг нажатия на кнопку назад
    private var exit: Boolean = false

    // Задача для отслеживания спутников (если в течении 2с нет спутников пишет 0)
    private var handler: Handler? = null
    private var runnable: Runnable = object : Runnable {
        override fun run() {
            Log.i(AvtoInformatorFragment.TAG, "postDelayed")
            binding.satellite.text = "0"
        }
    }

    // Менеджер для работы с местоположением
    private var locationManager: LocationManager? = null

    // Листенер для отслеживания местоположения
    private val locationListener: LocationListener =
        object : LocationListener {
            // обязательные функции LocationListener
            override fun onLocationChanged(location: Location) {}
            override fun onProviderDisabled(provider: String) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onStatusChanged(
                provider: String,
                status: Int,
                extras: Bundle
            ) {
            }
        }

    private val updateListListener: UpdateListListener?
        get() = supportFragmentManager.findFragmentByTag("AvtoInformatorFragment")
            .let { it as? UpdateListListener }

    private val updateSpeedListener: UpdateSpeedListener?
        get() = supportFragmentManager.findFragmentByTag("DashboardFragment")
            .let { it as? UpdateSpeedListener }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        ContextHelper.appContext = applicationContext
        // Ставим флаг, чтобы приложение не гасило экран
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val myPhoneStateListener = PhoneCustomStateListener(binding)
        val tel = this.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        tel.listen(myPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)

        // Проверяем все ли даны разрешения
        if (!verifyPermissions()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE)
            Log.d(TAG, "onCreate запрашиваем разрешения")
        } else {
            Log.d(TAG, "onCreate есть все разрешения")
            onCreateActivity()
        }

        // При нажатии на значок загружаем фрагмент Приборная панель
        binding.buttonDashboard.setOnClickListener {
            setSelectedButtonColor(1)
            binding.buttonRoute.isEnabled = false
            binding.buttonAvto.isVisible = false

            val dashboardFragment =
                supportFragmentManager.findFragmentByTag("DashboardFragment")
            val alreadyHasFragment = dashboardFragment != null

            if (!alreadyHasFragment) {
                // Создаем фрагмент автоинформатор
                supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.containerBus,
                        DashboardFragment(), "DashboardFragment"
                    )
                    .addToBackStack("DashboardFragment")
                    .commit()
            } else {
                supportFragmentManager.popBackStack("DashboardFragment", 0)
            }
        }

        // При нажатии на значок загружаем фрагмент Автоинформатор
        binding.avtoInformatorButton.setOnClickListener {
            setSelectedButtonColor(2)
            binding.buttonRoute.isEnabled = true
            binding.buttonAvto.isVisible = true

//            binding.containerBus.removeAllViews()
            if (filePath.isNotEmpty()) {
                Accessory().screenBlock(true, window)
                val fragmentAvtoInformator =
                    supportFragmentManager.findFragmentByTag("AvtoInformatorFragment")
                val alreadyHasFragment = fragmentAvtoInformator != null

                if (!alreadyHasFragment) {
                    // Создаем фрагмент автоинформатор
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.containerBus,
                            AvtoInformatorFragment(), "AvtoInformatorFragment"
                        )
                        .addToBackStack("AvtoInformatorFragment")
                        .commit()
                } else {
                    supportFragmentManager.popBackStack("AvtoInformatorFragment", 0)
                }
                Accessory().screenBlock(false, window)
            } else {
                Accessory().screenBlock(true, window)
                val fragmentAvtoInformerNoRoute =
                    supportFragmentManager.findFragmentByTag("AvtoinformerFragmentNoRoute")
                val alreadyHasFragment = fragmentAvtoInformerNoRoute != null

                if (!alreadyHasFragment) {
                    // Создаем фрагмент автоинформатор
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.containerBus,
                            AvtoinformerFragmentNoRoute(), "AvtoinformerFragmentNoRoute"
                        )
                        .addToBackStack("AvtoinformerFragmentNoRoute")
                        .commit()
                } else {
                    supportFragmentManager.popBackStack("AvtoinformerFragmentNoRoute", 0)
                }
                Accessory().screenBlock(false, window)


//                createTextViewNoRouteList()
//                binding.containerBus.removeAllViews()
//                binding.containerBus.addView(textViewNoList)
                Toast.makeText(this, "Выберите файл маршрута", Toast.LENGTH_SHORT).show()
            }
        }

        // При нажатии на значок загружаем фрагмент Камера
        binding.camera.setOnClickListener {
            // Блочим UI
            Accessory().screenBlock(true, window)
            setSelectedButtonColor(3)
            binding.buttonRoute.isEnabled = false
            binding.buttonAvto.isVisible = false

//            binding.containerBus.removeView(textViewNoList)
//            binding.containerBus.removeAllViews()


            val fragmentCamera = supportFragmentManager.findFragmentByTag("CameraFragment")
            val alreadyHasFragment = fragmentCamera != null

            if (!alreadyHasFragment) {
                Log.d(TAG, "camera true")
                // Создаем фрагмент камера
                supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.containerBus,
                        CameraFragmentOpenGL(), "CameraFragment"
                    )
                    .addToBackStack("CameraFragment")
                    .commit()
            } else {
                Log.d(TAG, "camera false")
                supportFragmentManager.popBackStack("CameraFragment", 0)
            }
            Accessory().screenBlock(false, window)
        }

        // Выбираем маршрут из файлов на устройстве
        binding.buttonRoute.setOnClickListener {
            val currentPath = getExternalFilesDir("/Granit BK-N/Routes")?.path.orEmpty()
            val fileDialog = OpenFileDialog(this, currentPath)
            fileDialog.setFilterJson(".jsonc")
            val drawFolder = resources.getDrawable(R.drawable.ic_folder)
            val drawRoute = resources.getDrawable(R.drawable.ic_route)
            fileDialog.setFolderIcon(drawFolder)
            fileDialog.setFileIcon(drawRoute)
            fileDialog.setOpenDialogListener { fileName ->
                startInformer(fileName)
                model.mInformer.saveDefaultRoute(fileName)
            }

            val dialog = fileDialog.show()
            dialog.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }

        // Настройки
        binding.buttonSetting.setOnClickListener {
            DialogFragmentSetting().showDialog(this)
        }

        // Автоматический/ручной режим
        binding.buttonAvto.setOnClickListener {
            if (flagSelectedButtonAvto) {
                binding.buttonAvto.setBackgroundResource(R.drawable.button_square)
            } else {
                binding.buttonAvto.setBackgroundResource(R.drawable.button_square_select)
            }

            flagSelectedButtonAvto = !flagSelectedButtonAvto

            model.mInformer.toggleGPS()
        }

        // Кнопка SOS
        binding.buttonSos.setOnClickListener {
            val dialog: AlertDialog = AlertDialog.Builder(this)
                .setMessage("Вы действительно хотите отправить команду SOS?")
                .setPositiveButton("Да") { _, _ ->
                    binding.iconSos.visibility = View.VISIBLE
                }
                .setNegativeButton("Отмена") { _, _ ->
                    binding.iconSos.visibility = View.INVISIBLE
                }
                .create()

            dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            dialog.show()
            dialog.window?.decorView?.systemUiVisibility = this.window.decorView.systemUiVisibility
            dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        }

        // Кнопка Видео
        binding.buttonVideo.setOnClickListener {
            Log.d(TAG, "buttonVideo")
            model.mInformer.mScripts.forEachIndexed { index, manualScript ->
                if (manualScript.mName == "Видео") {
                    model.displayManager =
                        this.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                    model.mInformer.play(manualScript.mID)
                    Log.d(TAG, "id = ${manualScript.mID}")
                }
            }
        }
    }

    // При уничтожении активити отписываемся и останавливаем сервисы
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity onDestroy")
        model.displayManager?.unregisterDisplayListener(model.mDisplayListener)
        model.serviceManager.unsubscribe(model.mInformer)
//        model.serviceManager?.unsubscribe(mInformer)
        model.states.forEach {
            if (it.value.state != ServiceManager.ServiceState.STOPED) {
                model.serviceManager.stopService(it.key)
            }
        }
    }

    // Функция запуска информатора (принимает путь файла маршрута)
    private fun startInformer(fileName: String) {
        Accessory().screenBlock(true, window)

        progressBar = createProgressBar()
        binding.containerBus.removeAllViews()
        binding.containerBus.addView(progressBar)
        setSelectedButtonColor(2)

        if (flagService) {
            model.states.forEach {
                Log.d(
                    AvtoInformatorFragment.TAG,
                    "stopService ${it.key.name} ${it.value.state != ServiceManager.ServiceState.STOPED}"
                )
                if (it.value.state != ServiceManager.ServiceState.STOPED) {
                    model.serviceManager.stopService(it.key)
                    model.serviceManager.unsubscribe(model.mInformer)
                }
            }

            Handler(Looper.getMainLooper()).postAtTime({
                Log.d(AvtoInformatorFragment.TAG, "Путь к файлу $fileName")
                filePath = fileName
                init()
            }, SystemClock.uptimeMillis() + 500L)
        }

        Handler(Looper.getMainLooper()).postAtTime({
            startService(fileName)
            flagService = true
        }, SystemClock.uptimeMillis() + 1000L)
    }

    // Создаем директории если их не было
    private fun createDirectory() {
        val routesPath = getExternalFilesDir("/Granit BK-N/Routes")?.path.orEmpty()
        val directoryRoutes = File(routesPath)
        directoryRoutes.mkdirs()
        val reklamaPath =
            getExternalFilesDir("/Granit BK-N/Reklama/Записи с камер")?.path.orEmpty()
        val directoryReklama = File(reklamaPath)
        directoryReklama.mkdirs()
    }

    // Проверка наличия Sim
    private fun isSimSupport() {
        val tm: TelephonyManager =
            this.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        // Возвращает true если сим вставлена
        val isSim = !(tm.getSimState() === TelephonyManager.SIM_STATE_ABSENT)

        if (isSim) {
            binding.iconSim.setImageResource(R.drawable.ic_sim)
        } else {
            binding.iconSim.setImageResource(R.drawable.ic_no_sim)
        }
    }

    // Метод для выбора фрагмента (какой фрагмент подсвечивать выбраным)
    private fun setSelectedButtonColor(flag: Int) {
        when (flag) {
            1 -> {
                binding.buttonDashboard.setBackgroundResource(R.color.buttonSelectColor)
                binding.avtoInformatorButton.setBackgroundResource(R.color.buttonNoSelectColor)
                binding.camera.setBackgroundResource(R.color.buttonNoSelectColor)
            }
            2 -> {
                binding.buttonDashboard.setBackgroundResource(R.color.buttonNoSelectColor)
                binding.avtoInformatorButton.setBackgroundResource(R.color.buttonSelectColor)
                binding.camera.setBackgroundResource(R.color.buttonNoSelectColor)
            }
            3 -> {
                binding.buttonDashboard.setBackgroundResource(R.color.buttonNoSelectColor)
                binding.avtoInformatorButton.setBackgroundResource(R.color.buttonNoSelectColor)
                binding.camera.setBackgroundResource(R.color.buttonSelectColor)
            }
        }
    }

    // Метод который убирает кнопки навигации и делает приложение на весь экран
    private fun setFullScreen() {
        // прячем панель навигации и строку состояния
        val decorView = window.decorView
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        decorView.systemUiVisibility = uiOptions
    }

    // Ловим фокус приложения
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        setFullScreen()
    }

    private fun startService(fileName: String) {
        if (!flagService) {
            Log.d(AvtoInformatorFragment.TAG, "Путь к файлу $fileName")
            filePath = fileName
            init()
        }

        model.states.forEach {
            Log.d(
                AvtoInformatorFragment.TAG,
                "startService ${it.key.name} ${it.value.state == ServiceManager.ServiceState.STOPED}"
            )
            if (it.value.state == ServiceManager.ServiceState.STOPED) {
//                // Временно отключаем старт сервиса EGTS
//                if (it.key.name == "EGTS") {
//                    return@forEach
//                }

                model.serviceManager.startService(
                    it.key,
                    it.value.connection,
                    it.value.payload
                )

                if (it.key.name == "Informer") {

                    val timeDelay = SystemClock.uptimeMillis() + 1000L
                    val mainHandler = Handler(Looper.getMainLooper())
                    mainHandler.postAtTime({
                        binding.containerBus.removeView(progressBar)
                        supportFragmentManager.beginTransaction()
                            .replace(
                                R.id.containerBus,
                                AvtoInformatorFragment.newInstance(""),
                                "AvtoInformatorFragment"
                            )
                            .addToBackStack("AvtoInformatorFragment")
                            .commit()

                        Accessory().screenBlock(false, window)
                    }, timeDelay)
                }
            }
        }
    }

    // Инициализируем ServiceManager
    fun init() {
        model = Model(applicationContext)
        model.serviceManager = ServiceManager(
            mContext = this,
            onStateChange = { serviceType: ServiceManager.ServiceType, serviceState: ServiceManager.ServiceState, connection: String, payload: String ->
                val s = model.states[serviceType]!!.copy()
                s.state = serviceState
                model.states[serviceType] = s
                model.logs.add(0, "$serviceType->$serviceState")
                if (model.logs.size > 50) model.logs.removeRange(49, model.logs.size - 1)
            },
            onRecieve = { serviceType: ServiceManager.ServiceType, jsonObject: JSONObject ->
                model.logs.add(0, "$serviceType->$jsonObject")
                if (model.logs.size > 50) model.logs.removeRange(49, model.logs.size - 1)
                Log.d(serviceType.toString(), jsonObject.toString())
            },
            onSTM32Search = {
                if (it) {
                    model.states[ServiceManager.ServiceType.STM32] =
                        Model.ServiceModel(ServiceManager.ServiceState.STOPED)
                } else {
                    model.states.remove(ServiceManager.ServiceType.STM32)
                }
            },
            onPermissionsRequired = {
// ?????????????????????????????????????????????????????????
            }
        )
        model.serviceManager.mRoutesDir = getExternalFilesDir("/Granit BK-N/Routes")?.path
        model.serviceManager.subscribe(model.mInformer)
    }

    // Функция создания прогресс бара программно
    private fun createProgressBar(): ProgressBar {
        // Создаем прогессбар
        val progressBar = ProgressBar(this)
        // Указываем программно ширину textView
        val params = LinearLayout.LayoutParams(
            200,
            200
        )
        progressBar.layoutParams = params
        progressBar.foregroundGravity = Gravity.CENTER
        return progressBar
    }

    // Дублируем onCreate если пользователь дал разрешения
    private fun onCreateActivity() {
        updateInterface()
        createDirectory()

        CoroutineScope(Dispatchers.Main).launch {
            val result = model.mInformer.getDefaultRouteToSharedPrefs()
            if (result != "") {
                startInformer(result)
            } else {
                // Создаем фрагмент автоинформатор
                supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.containerBus,
                        AvtoinformerFragmentNoRoute(), "AvtoinformerFragmentNoRoute"
                    )
                    .addToBackStack("AvtoinformerFragmentNoRoute")
                    .commit()
            }
        }

        // Инициализируем и считываем настройки с json
        settingsParse = SettingsParse(this)
        init()

        model.start(this)
        secondActivity()
        model.onStart = { secondActivity() }

        setSelectedButtonColor(2)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // получаем местоположения устройства
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        locationManager!!.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            0, 0f, locationListener
        )
        locationManager!!.addGpsStatusListener { event ->
            var satellites = 0
            var satellitesInFix = 0
            for (sat in locationManager!!.getGpsStatus(null)!!.satellites) {
                if (sat.usedInFix()) {
                    satellitesInFix++
                }
                satellites++
            }
            binding.satellite.text = satellitesInFix.toString()

            if (handler != null) {
                handler!!.removeCallbacks(runnable)
            }

            handler = Handler(Looper.getMainLooper())
            handler!!.postAtTime(runnable, SystemClock.uptimeMillis() + 2000L)
        }
    }

    private fun secondActivity() {
        if (flagDisplayVideo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Activity options are used to select the display screen.
                val options = ActivityOptions.makeBasic()
                model.displayManager =
                    this.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                val dis = model.displayManager!!.displays
                // Select the display screen that you want to show the second activity
                options.launchDisplayId = dis[1].displayId
                // To display on the second screen that your intent must be set flag to make
                // single task (combine FLAG_ACTIVITY_CLEAR_TOP and FLAG_ACTIVITY_NEW_TASK)
                // or you also set it in the manifest (see more at the manifest file)
                startActivity(
                    Intent(this@MainActivity, VideoFullscreenActivity::class.java),
                    options.toBundle()
                )
            }
        }

    }

    // Раз в секунду обновляем время, проверяем сим и местоположение
    private fun updateInterface() {
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                binding.time.text = getCurrentTime()
                getLocation()
                isSimSupport()
                isSdCard()
                mainHandler.postDelayed(this, 1000)
            }
        })
    }

    // Проверка наличия SD карты
    private fun isSdCard() {
        val listOfInternalAndExternalStorage = this.getExternalFilesDirs(null)
        if (listOfInternalAndExternalStorage.size >= 2) {
            binding.iconSdcard.setImageResource(R.drawable.ic_sd_card)
        } else {
            binding.iconSdcard.setImageResource(R.drawable.ic_no_sd_card)
        }
    }

    // Отслеживаем принял ли пользователь все разрешения
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionsResult отслеживаем разрешения")
        // проверка по запрашиваемому коду и разрешениям
        if (requestCode == REQUEST_CODE && verifyPermissions()) {
            Log.d(TAG, "onRequestPermissionsResult есть все разрешения")
            onCreateActivity()
        } else {
            // Не все разрешения приняты, запрашиваем еще раз
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // Проверка все ли приняты разрешения
    private fun verifyPermissions(): Boolean {
        // Проверяем, есть ли у нас разрешение камеру
        val isCameraPermissionGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        // Проверяем, есть ли у нас разрешение на запись
        val isWritePermissionGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        // Проверяем, есть ли у нас разрешение на чтение
        val isReadPermissionGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        // Проверяем, есть ли у нас разрешение на получение координат
        val isGeoPermissionGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // Проверяем, есть ли у нас разрешение на запись аудио
        val isRecordAudioPermissionGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        return isCameraPermissionGranted &&
                isWritePermissionGranted &&
                isReadPermissionGranted &&
                isGeoPermissionGranted &&
                isRecordAudioPermissionGranted
    }

    // Получаем время
    fun getCurrentTime(): String? {
        val calendar = Calendar.getInstance()
        val hour = calendar[Calendar.HOUR_OF_DAY]
        val minute = calendar[Calendar.MINUTE]
        val second = calendar[Calendar.SECOND]
        val day = calendar[Calendar.DATE]
        val month = calendar[Calendar.MONTH]
        val year = calendar[Calendar.YEAR]
        return String.format(
            "%02d.%02d.%02d  %02d:%02d:%02d",
            day, month + 1, year, hour, minute, second
        ) // ДД.ММ.ГГГГ ЧЧ:ММ:СС - формат времени
    }

    companion object {
        private const val REQUEST_CODE = 123
        private val PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        const val TAG = "MainActivity"
        lateinit var model: Model
        lateinit var settingsParse: SettingsParse
        var filePath: String = ""
        var flagSelectedButtonAvto = true
        var flagDisplayVideo = true

        lateinit var locationStr: String
    }

    // Прокидываем листенер до фрагмента
    override fun updateList() {
        updateListListener?.updateList()
    }

    // Прокидываем листенер до фрагмента
    override fun messageFromDispatcher(
        message: MutableState<String>,
        mPopupButton: SnapshotStateList<AvtoInformatorRepository.Answer>
    ) {
        updateListListener?.messageFromDispatcher(message, mPopupButton)
    }

    // Делаем реконект к серверу MQTT и EGTS
    override fun reconnectService() {
        var flagMqtt = false
        var flagEgts = false

        CoroutineScope(Dispatchers.Main).launch {
            flagMqtt = applicationContext.let { it1 ->
                AvtoInformatorRepository(it1).getFlagServiceMqttToSharedPrefs()
            }

            flagEgts = applicationContext.let { it1 ->
                AvtoInformatorRepository(it1).getFlagServiceEgtsToSharedPrefs()
            }
        }

        // Останавливаем сервер MQTT
        model.serviceManager.stopService(ServiceManager.ServiceType.MQTT)
        // Останавливаем сервер EGTS
        model.serviceManager.stopService(ServiceManager.ServiceType.EGTS)

        // Инициализируем модель с новыми данными
        init()

        // Запускаем/останавливаем сервер MQTT
        Handler(Looper.getMainLooper()).postAtTime({
            val mqtt = model.states[ServiceManager.ServiceType.MQTT]
            if (flagMqtt) {
                model.serviceManager.startService(
                    ServiceManager.ServiceType.MQTT,
                    mqtt!!.connection,
                    mqtt.payload
                )
            } else {
                model.serviceManager.stopService(ServiceManager.ServiceType.MQTT)
            }
        }, SystemClock.uptimeMillis() + 500L)

        // Запускаем/останавливаем сервер EGTS
        Handler(Looper.getMainLooper()).postAtTime({
            val egts = model.states[ServiceManager.ServiceType.EGTS]
            if (flagEgts) {
                model.serviceManager.startService(
                    ServiceManager.ServiceType.EGTS,
                    egts!!.connection,
                    egts.payload
                )
            } else {
                model.serviceManager.stopService(ServiceManager.ServiceType.EGTS)
            }
        }, SystemClock.uptimeMillis() + 500L)
    }

    // Обрабатываем нажатие кнопки назад
    override fun onBackPressed() {
        if (exit) {
            finish()
        } else {
            Toast.makeText(
                this, "Нажмите еще раз, чтобы выйти",
                Toast.LENGTH_SHORT
            ).show()
            exit = true
            Handler().postDelayed(Runnable { exit = false }, 3 * 1000)
        }
    }

    // Получаем локацию
    fun getLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        LocationServices.getFusedLocationProviderClient(this)
            .lastLocation
            .addOnSuccessListener {
                it?.let {
                    val speed = (it.speed.toInt() * 3600) / 1000
                    val str = resources.getString(R.string.speed, speed.toString())
                    // Изменяем скорость
                    binding.speed.text = str
                    updateSpeedListener?.updateSpeed(speed)

                    locationStr = "${it.latitude}  ${it.longitude}"
                } /*?: Log.d(TAG, resources.getString(R.string.location_absent))*/
            }
            .addOnCanceledListener {
                /*Log.d(TAG, resources.getString(R.string.location_request_was_canceled))*/
            }
            .addOnFailureListener {
                /*Log.d(TAG, resources.getString(R.string.location_request_failed))*/
            }
    }

    // Ловим изменение уровня сигнала сотовой связи
    class PhoneCustomStateListener(val binding: ActivityMainBinding) : PhoneStateListener() {
        var signalSupport = 0
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
            super.onSignalStrengthsChanged(signalStrength)
            signalSupport = signalStrength.getGsmSignalStrength()
//            Log.d(TAG, "------ gsm signal --> $signalSupport")
            if (signalSupport > 30) {
//                Log.d(TAG, "Signal GSM : Good")
                binding.signalNet.setImageResource(R.drawable.ic_good_signal)
            } else if (signalSupport in 21..29) {
                binding.signalNet.setImageResource(R.drawable.ic_middle_signal)
//                Log.d(TAG, "Signal GSM : Avarage")
            } else if (signalSupport in 4..19) {
                binding.signalNet.setImageResource(R.drawable.ic_low_signal)
//                Log.d(TAG, "Signal GSM : Weak")
            } else if (signalSupport < 3) {
                binding.signalNet.setImageResource(R.drawable.ic_no_signal)
//                Log.d(TAG, "Signal GSM : Very weak")
            }
        }
    }
}

class MessageEvent(val serviceFlag: Boolean)