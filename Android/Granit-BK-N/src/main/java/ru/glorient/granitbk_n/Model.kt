package ru.glorient.granitbk_n

import android.app.Application
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import ru.glorient.bkn.ServiceManager
import ru.glorient.granitbk_n.avtoinformer.AvtoInformatorFragment
import ru.glorient.granitbk_n.avtoinformer.AvtoInformatorRepository

class Model(context: Context){
    data class ServiceModel(var state: ServiceManager.ServiceState, val connection: String = "{}", val payload: String = "")

    var states = mutableStateMapOf<ServiceManager.ServiceType, ServiceModel>()
    var logs = mutableStateListOf<String>()

    var mInformer = AvtoInformatorRepository(context) { _: String, id: Long ->
        if (id > 0) {
            if (!isVideo)
                avid(id)
        }
    }
//    val mMqtt = AvtoInformatorRepository()
    private val settingsParse = MainActivity.settingsParse

    private fun avid(id:Long){
        mInformer.endVideo(id)
    }

    init {
        Log.d(AvtoInformatorFragment.TAG, "server ${settingsParse.server}")
        states[ServiceManager.ServiceType.EGTS] = ServiceModel(ServiceManager.ServiceState.STOPED,
//                "{'server':'10.0.2.2','port':7001,'transport_id':6080}",
//            """{"server":"10.20.9.3","port":7001,"timeout":3000,"transport_id":6080}""",
            """{"server":"${settingsParse.serverEgts}","port":${settingsParse.portEgts},"timeout":${settingsParse.timeoutEgts},"transport_id":${settingsParse.tid}}""",
            ServiceManager.EGTS_START_PAYLOAD
        )
        states[ServiceManager.ServiceType.MQTT] = ServiceModel(ServiceManager.ServiceState.STOPED,
//            """{"server":"romasty.duckdns.org","port":8883,"transport_id":6082,"ssl":true,"user":"glorient","password":"raQNI6dpZZrd44xoXFPz","timeout":3000}""",
            """{"server":"${settingsParse.server}","port":${settingsParse.port},"transport_id":${settingsParse.tid},"ssl":${settingsParse.ssl},"user":"${settingsParse.login}","password":"${settingsParse.password}","timeout":${settingsParse.timeout}}""",
            ServiceManager.MQTT_START_PAYLOAD
        )
        val file = MainActivity.filePath
//        val file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + File.separator.toString() + "398t.jsonc"

        // "autodir":"none|soft|hard" - алгоритм определения вектора движения отключен|без определения точности(для эмулятора)|с определением точности(по умолчанию)
        states[ServiceManager.ServiceType.Informer] = ServiceModel(ServiceManager.ServiceState.STOPED,
            """{"file":"$file"}""",
            ServiceManager.INFO_START_PAYLOAD
        )
    }

    lateinit var serviceManager: ServiceManager

    var onStart: () -> Unit = { }

    var displayManager: DisplayManager? = null
    class DisplayListener(val mModel: Model): DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            Log.i("Model", "onDisplayAdded")
            mModel.changeDisplay()
            MainActivity.flagDisplayVideo = true
        }

        override fun onDisplayChanged(displayId: Int) {
            Log.i("Model", "onDisplayChanged")
            mModel.changeDisplay()
        }

        override fun onDisplayRemoved(displayId: Int) {
            Log.i("Model", "onDisplayRemoved")
            mModel.changeDisplay()
            MainActivity.flagDisplayVideo = false
        }
    }
    val mDisplayListener = DisplayListener(this)
    val isVideo: Boolean
        get() = (displayManager?.displays?.size ?: 0) > 1
    fun changeDisplay(){
//        val flag = isVideo
        MainActivity.flagDisplayVideo = (displayManager?.displays?.size ?: 0) > 1
//        if(!flag and isVideo)onStart()
        if(isVideo)onStart()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun start(cont: AppCompatActivity) {
        if (displayManager == null) {
            displayManager = cont.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            displayManager?.registerDisplayListener( mDisplayListener,null)
            changeDisplay()
        }
    }
}