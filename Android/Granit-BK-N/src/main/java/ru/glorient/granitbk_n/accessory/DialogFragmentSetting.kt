package ru.glorient.granitbk_n.accessory

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import ru.glorient.granitbk_n.BuildConfig
import ru.glorient.granitbk_n.MainActivity
import ru.glorient.granitbk_n.R
import ru.glorient.granitbk_n.avtoinformer.AvtoInformatorRepository
import java.io.File

// Диалог для чтения и записи настроек из/в json
class DialogFragmentSetting : DialogFragment(), DialogVerificationListener {

    private lateinit var version: TextView
    private lateinit var transportId: TextView
    private lateinit var tabHost: TabHost
    private lateinit var containerLock: LinearLayout
    private lateinit var buttonLock: ImageView
    private lateinit var textViewButtonLock: TextView

    private lateinit var switchMqtt: SwitchCompat
    private lateinit var switchEgts: SwitchCompat

    // Поля для вкладки MQTT
    private lateinit var server: EditText
    private lateinit var port: EditText
    private lateinit var ssl: CheckBox
    private lateinit var user: EditText
    private lateinit var password: EditText
    private lateinit var timeout: EditText
    private lateinit var textInputLayout: TextInputLayout

    // Поля для вкладки EGTS
    private lateinit var serverEgts: EditText
    private lateinit var portEgts: EditText
    private lateinit var timeoutEgts: EditText

    private val updateListListener: UpdateListListener?
        get() = activity as UpdateListListener?

    private val sharedPrefs by lazy {
        context?.getSharedPreferences("granit_bkn_shared_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alertDialog = requireActivity().let {
            val myBuilder = AlertDialog.Builder(it)

            // Парсим настройки при открытии диалога
            MainActivity.settingsParse = SettingsParse(requireContext())
            val settingsParse = MainActivity.settingsParse

            // Инфлэйтим вьюху с настройками
            val view = requireActivity()
                .layoutInflater
                .inflate(R.layout.dialog_layout_setting, null)

            version = view.findViewById(R.id.version) as TextView
            transportId = view.findViewById(R.id.editDialogSettingTransportId) as TextView

            // Настраиваем вкладки
            tabHost = view.findViewById(R.id.tabHost) as TabHost
            tabHost.setup()
            setupTab(getString(R.string.setting_general_tab), R.id.setting_general)
            setupTab(getString(R.string.setting_mqtt), R.id.setting_mqtt)
            setupTab(getString(R.string.setting_egts), R.id.setting_egts)

            buttonLock = view.findViewById(R.id.buttonLock) as ImageView
            textViewButtonLock = view.findViewById(R.id.textViewButtonLock) as TextView
            containerLock = view.findViewById(R.id.containerLock) as LinearLayout
            textInputLayout = view.findViewById(R.id.textInputLayout) as TextInputLayout

            // Находим вью общих настроек
            switchMqtt = view.findViewById(R.id.mqttService) as SwitchCompat
            switchEgts = view.findViewById(R.id.egtsService) as SwitchCompat

            // Находим вью настроек MQTT
            server = view.findViewById(R.id.editDialogSettingServer) as EditText
            port = view.findViewById(R.id.editDialogSettingPort) as EditText
            ssl = view.findViewById(R.id.dialog_setting_ssl) as CheckBox
            user = view.findViewById(R.id.editDialogSettingUser) as EditText
            password = view.findViewById(R.id.editDialogSettingPassword) as EditText
            timeout = view.findViewById(R.id.editDialogSettingTimeout) as EditText

            // Находим вью настроек EGTS
            serverEgts = view.findViewById(R.id.editDialogSettingServerEgts) as EditText
            portEgts = view.findViewById(R.id.editDialogSettingPortEgts) as EditText
            timeoutEgts = view.findViewById(R.id.editDialogSettingTimeoutEgts) as EditText

            CoroutineScope(Dispatchers.Main).launch {
                switchMqtt.isChecked =
                    context?.let { it1 ->
                        AvtoInformatorRepository(it1).getFlagServiceMqttToSharedPrefs()
                    }
                        ?: false

                switchEgts.isChecked =
                    context?.let { it1 ->
                        AvtoInformatorRepository(it1).getFlagServiceEgtsToSharedPrefs()
                    }
                        ?: false
            }

            // Заполняем поля из json для MQTT
            server.setText(settingsParse.server)
            port.setText(settingsParse.port.toString())
            ssl.isChecked = settingsParse.ssl
            user.setText(settingsParse.login)
            password.setText(settingsParse.password)
            timeout.setText(settingsParse.timeout.toString())

            // Заполняем поля из json для EGTS
            serverEgts.setText(settingsParse.serverEgts)
            portEgts.setText(settingsParse.portEgts.toString())
            timeoutEgts.setText(settingsParse.timeoutEgts.toString())

            // Добавляем фильтры в EditText
            port.filters = arrayOf(InputFilterMinMax("1", "65535"))
            timeout.filters = arrayOf(InputFilterMinMax("1", "2147483647"))

            // Добавляем фильтры в EditText для EGTS
            portEgts.filters = arrayOf(InputFilterMinMax("1", "65535"))
            timeoutEgts.filters = arrayOf(InputFilterMinMax("1", "2147483647"))

            // Обрабатываем нажатие на иконку замка
            containerLock.setOnClickListener { viewLock ->
                // Открываем диалог с авторизацией
                LoginFragment().showDialog(this)
            }

            // Убираем значок просмотра пароля, пока пользователь не получил доступ
            textInputLayout.isPasswordVisibilityToggleEnabled = false
            // Пишем версию ПО и TID
            version.text = "1.0.1 от ${BuildConfig.VERSION_NAME}"
            transportId.text = settingsParse.tid.toString()

            // Блокируем поля от ввода
            verificationViewVisible(false)

            myBuilder
                .setTitle(requireActivity().resources.getString(R.string.dialog_setting_title))
                .setView(view)
                .setPositiveButton(
                    requireActivity()
                        .resources
                        .getString(R.string.ok)
                ) { _, _ ->
                    // При нажатии ок сохраняем новый файл json в память
                    val filePathFolder = requireContext()
                        .getExternalFilesDir("/Granit BK-N")?.path.orEmpty()
                    val folder = File(filePathFolder)
                    val file = File(folder, "settings.jsonc")
                    if (file.exists()) {
                        val str = StringBuilder()

                        try {
                            file.forEachLine {
                                str.append(it.substringBeforeLast("//").trim())
                            }
                            createFileSetting(file.absolutePath, str.toString())
                        } catch (throwable: Throwable) {
                            Log.d("mylog", "ошибка - $throwable")
                        }
                    } else {
                        file.createNewFile()
                        createFileSetting(file.absolutePath, "")
                    }

                    // Изменяем параметры MQTT
                    settingsParse.server = server.text.toString()
                    settingsParse.port = port.text.toString().toInt()
                    settingsParse.ssl = ssl.isChecked
                    settingsParse.login = user.text.toString()
                    settingsParse.password = password.text.toString()
                    settingsParse.timeout = timeout.text.toString().toInt()

                    // Изменяем параметры EGTS
                    settingsParse.serverEgts = serverEgts.text.toString()
                    settingsParse.portEgts = portEgts.text.toString().toInt()
                    settingsParse.timeoutEgts = timeoutEgts.text.toString().toInt()

                    saveFlagService(switchMqtt.isChecked, switchEgts.isChecked)

                    // Переподключаем MQTT
                    updateListListener?.reconnectService()
                }
                .setNegativeButton(
                    requireActivity()
                        .resources
                        .getString(R.string.cancel)
                ) { _, _ -> }
                .create()
        }

        alertDialog.window?.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

        return alertDialog
    }

    // Устанавливаем вкладки
    private fun setupTab(title: String, id: Int) {
        val spec = tabHost.newTabSpec(title)
        spec.setContent(id)
        spec.setIndicator(title)
        tabHost.addTab(spec)
    }

    // Создаем файл json с настройками если его нет
    private fun createFileSetting(filePath: String, str: String) {
        val file = File(filePath)
        val settings: JSONObject = if (str != "") {
            JSONObject(str)
        } else {
            JSONObject()
        }

        val mqtt = JSONObject()
        mqtt.put("server", server.text.toString())
        mqtt.put("port", port.text.toString().toInt())
        mqtt.put("user", user.text.toString())
        mqtt.put("password", password.text.toString())
        mqtt.put("timeout", timeout.text.toString().toInt())
        mqtt.put("ssl", ssl.isChecked)

        val egts = JSONObject()
        egts.put("server", serverEgts.text.toString())
        egts.put("port", portEgts.text.toString().toInt())
        egts.put("timeout", timeoutEgts.text.toString().toInt())

        settings.put("mqtt", mqtt)
        settings.put("egts", egts)

        try {
            val outputStream = file.outputStream()
            outputStream.use {
                it.write(settings.toString().toByteArray())
            }
        } catch (throwable: Throwable) {
            Log.d("mylog", "ошибка - $throwable")
        }
    }

    // Показываем диалог
    fun showDialog(activity: AppCompatActivity) {
        show(activity.supportFragmentManager, null)

        fragmentManager?.executePendingTransactions()

        dialog?.window?.decorView?.systemUiVisibility =
            activity.window.decorView.systemUiVisibility

        // Make the dialogs window focusable again.
        dialog?.window?.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )
    }

    override fun onResume() {
        super.onResume()
        // прячем панель навигации и строку состояния
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        dialog?.window?.decorView?.systemUiVisibility = uiOptions

//        dialog?.getWindow()?.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

        // Задаем размер диалога
        val window = dialog?.window
        val size = Point()
        val display = window?.windowManager?.defaultDisplay
        display?.getSize(size)
        val width = size.x
        window?.setLayout((width * 0.85).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        window?.setGravity(Gravity.CENTER)
    }

    // Функция блокировки/разблокировки полей ввода
    private fun verificationViewVisible(flag: Boolean) {
        switchMqtt.isEnabled = flag
        switchEgts.isEnabled = flag

        server.isEnabled = flag
        port.isEnabled = flag
        ssl.isEnabled = flag
        user.isEnabled = flag
        password.isEnabled = flag
        timeout.isEnabled = flag

        serverEgts.isEnabled = flag
        portEgts.isEnabled = flag
        timeoutEgts.isEnabled = flag
    }

    // Записываем флаги сервисов в память
    fun saveFlagService(flagMqtt: Boolean, flagEgts: Boolean) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                sharedPrefs?.edit()
                    ?.putBoolean("serviceMqtt", flagMqtt)
                    ?.putBoolean("serviceEgts", flagEgts)
                    ?.apply()
            } catch (t: Throwable) {
            }
        }
    }

    // Ловим событие авторизации пользователя
    override fun successfulVerification() {
        verificationViewVisible(true)

        // Меняем картинку и текст
        buttonLock.setImageResource(R.drawable.ic_lock_open)
        textViewButtonLock.setText(R.string.access_open)
        textViewButtonLock.setTextColor(Color.GREEN)

        textInputLayout.isPasswordVisibilityToggleEnabled = true
    }
}