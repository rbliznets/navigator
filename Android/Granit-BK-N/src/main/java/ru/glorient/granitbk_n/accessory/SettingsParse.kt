package ru.glorient.granitbk_n.accessory

import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import ru.glorient.granitbk_n.R
import java.io.*
import kotlin.random.Random

// Парсинг файла настроек
class SettingsParse(val context: Context) {

    // Путь к папке
    private val filePath = context.getExternalFilesDir("/Granit BK-N")?.path.orEmpty()
    private val folder = File(filePath)

    var tid = 0

    var server = ""
    var port = 0
    var login = ""
    var password = ""
    var timeout = 0
    var ssl = false

    var serverEgts = ""
    var portEgts = 0
    var timeoutEgts = 0

    init {
        try {
            // Получаем настройки серверов
            val file = File(folder, "settings.jsonc")
            val str = StringBuilder()
            file.forEachLine {
                str.append(it.substringBeforeLast("//").trim())
            }

            val settings = JSONObject(str.toString())
            if (settings.has("mqtt")) {
                val mqtt = settings.getJSONObject("mqtt")

                if (mqtt.has("server")) server = mqtt.getString("server")
                if (mqtt.has("port")) port = mqtt.getInt("port")
                if (mqtt.has("user")) login = mqtt.getString("user")
                if (mqtt.has("password")) password = mqtt.getString("password")
                if (mqtt.has("timeout")) timeout = mqtt.getInt("timeout")
                if (mqtt.has("ssl")) ssl = mqtt.getBoolean("ssl")
            }
            if (settings.has("egts")) {
                val egts = settings.getJSONObject("egts")

                if (egts.has("server")) serverEgts = egts.getString("server")
                if (egts.has("port")) portEgts = egts.getInt("port")
                if (egts.has("timeout")) timeoutEgts = egts.getInt("timeout")
            }
        } catch (e: FileNotFoundException) {
            Toast.makeText(
                context,
                "Файл настроек не найден",
                Toast.LENGTH_SHORT
            ).show()
        }

        try {
            // Получаем TID
            val fileTid = File(folder, "TID.jsonc")
            val strTid = StringBuilder()
            fileTid.forEachLine {
                strTid.append(it.substringBeforeLast("//").trim())
            }
            val settingsTid = JSONObject(strTid.toString())
            if (settingsTid.has("tid")) tid = settingsTid.getInt("tid")
        } catch (e: FileNotFoundException) {
            createFileTid()
        }
    }

    // Создаем файл json c TID если его нет
    private fun createFileTid() {
        folder.mkdirs()
        val file = File(folder, "TID.jsonc")
//        file.createNewFile()

        val tid = JSONObject()
        tid.put("tid", Random.nextInt(9999))

        try {
            val outputStream = file.outputStream()
            outputStream.use {
                it.write(tid.toString().toByteArray())
            }
        } catch (throwable: Throwable) {
            Log.d("mylog", "ошибка - $throwable")
        }
    }
}