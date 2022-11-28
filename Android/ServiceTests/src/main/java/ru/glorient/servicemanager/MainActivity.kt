package ru.glorient.servicemanager

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.setContent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.*
import org.json.JSONObject
import ru.glorient.bkn.ServiceManager
import ru.glorient.servicemanager.compose.*
import ru.glorient.servicemanager.ui.ServiceManagerTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.content.BroadcastReceiver
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter


class StartMyActivityAtBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            val activityIntent = Intent(context, MainActivity::class.java)
            activityIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            context.startActivity(activityIntent)
        }
    }
}

class Model(act: Application): AndroidViewModel(act) {
    data class ServiceModel(
        var state: ServiceManager.ServiceState,
        val connection: String = "{}",
        val payload: String = ""
    )

    var states = mutableStateMapOf<ServiceManager.ServiceType, ServiceModel>()
    var logs = mutableStateListOf<String>()

    val mInformer = InformerDecorator { _: String, id: Long ->
        if (id > 0) {
            if (!isVideo.value) avid(id)
        }
    }
    private fun avid(id: Long){
        mInformer.endVideo(id)
    }
    val mMqtt = MQTTDecorator()
    val mEgts = EGTSDecorator()

    var onStart: () -> Unit = { }

    init {
        states[ServiceManager.ServiceType.EGTS] = ServiceModel(
            ServiceManager.ServiceState.STOPED,
//            """{"server":"10.0.2.2","port":7001,"timeout":3000,"transport_id":6080}""",
//            """{"server":"195.96.165.50","port":20000,"timeout":3000,"transport_id":6080}""",
            """{"server":"95.163.85.129","port":7005,"timeout":3000,"transport_id":6080}""",
   //"""{"GPS":{"run":"on","interval":1500,"fastestInterval":1000, "filter":{"type":1,"minInterval":3000,"dutyCycle":10,"sensors":"off"}}}"""
            ServiceManager.EGTS_START_PAYLOAD
        )
        states[ServiceManager.ServiceType.MQTT] = ServiceModel(
            ServiceManager.ServiceState.STOPED,
//                """{"server":"195.96.165.38","port":1884,"transport_id":6080,"ssl":false,"user":"glorient","password":"raQNI6dpZZrd44xoXFPz","timeout":3000}""",
            """{"server":"romasty.duckdns.org","port":8883,"transport_id":6080,"ssl":true,"user":"glorient","password":"raQNI6dpZZrd44xoXFPz","timeout":3000}""",
//            """{"GPS":{"run":"on","interval":1500,"fastestInterval":1000,"filter":{"type":2,"minInterval":60000,"sensors":"off"}},"log":"on"}"""
//            """{"GPS":{"run":"on","interval":3000,"fastestInterval":2000, "filter":{"type":0,"minInterval":60000}},"log":"on"}"""
            ServiceManager.MQTT_START_PAYLOAD
        )
        val file = act.baseContext.getExternalFilesDir("routes")?.path + File.separator.toString() + "398t.jsonc"
        // "autodir":"none|soft|hard" - алгоритм определения вектора движения отключен|без определения точности(для эмулятора)|с определением точности(по умолчанию)
        states[ServiceManager.ServiceType.Informer] = ServiceModel(
            ServiceManager.ServiceState.STOPED,
            """{"file":"$file"}""",
//            """{"GPS":{"run":"on","interval":1500,"fastestInterval":1000},"autodir":"soft"}"""
            ServiceManager.INFO_START_PAYLOAD
        )
    }

    var serviceManager: ServiceManager? = null
    var displayManager: DisplayManager? = null
    private class DisplayListiner(val mModel: Model): DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            Log.i("Model", "onDisplayAdded")
            mModel.changeDisplay()
        }

        override fun onDisplayChanged(displayId: Int) {
            Log.i("Model", "onDisplayChanged")
            mModel.changeDisplay()
        }

        override fun onDisplayRemoved(displayId: Int) {
            Log.i("Model", "onDisplayRemoved")
            mModel.changeDisplay()
        }
    }
    private val mDisplayListiner = DisplayListiner(this)
    var isVideo = mutableStateOf(false)
    fun changeDisplay(){
        val flag = isVideo.value
        isVideo.value = (displayManager?.displays?.size ?: 0) > 1
        if(!flag and isVideo.value)onStart()
    }

    @SuppressLint("SimpleDateFormat")
    @RequiresApi(Build.VERSION_CODES.Q)
    fun start(
        cont: AppCompatActivity,
        permissionsRequired: ((perms: Array<String>) -> Unit)? = null
    ) {
        if (displayManager == null) {
            displayManager = cont.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            displayManager?.registerDisplayListener(mDisplayListiner, null)
            changeDisplay()
        }

        if (serviceManager == null) {
            serviceManager = ServiceManager(
                mContext = cont,
                onStateChange = { serviceType: ServiceManager.ServiceType, serviceState: ServiceManager.ServiceState, _: String, _: String ->
                    val s = states[serviceType]!!.copy()
                    s.state = serviceState
                    states[serviceType] = s
                    val sdf = SimpleDateFormat("HH:mm:ss")
                    logs.add(0, "(${sdf.format(Date())}) $serviceType->$serviceState")
                    if (logs.size > 50) logs.removeRange(49, logs.size - 1)
                },
                onRecieve = { serviceType: ServiceManager.ServiceType, jsonObject: JSONObject ->
                    val sdf = SimpleDateFormat("HH:mm:ss")
                    logs.add(0, "(${sdf.format(Date())}) $serviceType->$jsonObject")
                    if (logs.size > 50) logs.removeRange(49, logs.size - 1)
                    Log.d(serviceType.toString(), jsonObject.toString())
                },
                onSTM32Search = {
                    if (it) {
                        states[ServiceManager.ServiceType.STM32] =
                            ServiceModel(ServiceManager.ServiceState.STOPED)
                    } else {
                        states.remove(ServiceManager.ServiceType.STM32)
                    }
                },
                onPermissionsRequired = permissionsRequired
            )

            serviceManager?.subscribe(mInformer)
            serviceManager?.subscribe(mMqtt)
            serviceManager?.subscribe(mEgts)

            val pInfo: PackageInfo? = cont.packageManager?.getPackageInfo("ru.glorient.servicemanager", 0)
            logs.add("ru.glorient.servicemanager: ${pInfo?.versionName}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCleared() {
        super.onCleared()
        displayManager?.unregisterDisplayListener(mDisplayListiner)
        serviceManager?.unsubscribe(mMqtt)
        serviceManager?.unsubscribe(mInformer)
        states.forEach{
            if(it.value.state != ServiceManager.ServiceState.STOPED) {
                serviceManager?.stopService(it.key)
            }
        }
//        Log.i("Model", "Model destroyed!")
    }
}

class MainActivity : AppCompatActivity() {
    private val requestMultiplePermissions =  registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        permissions.entries.forEach {
            Log.e("DEBUG", "${it.key} = ${it.value}")
        }
    }

    private lateinit var vmodel: Model


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

//        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
//        val deviceSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)
//        deviceSensors.forEach{
//            Log.d("Sensor","${it.toString()}")
//        }

//        val suProcess=Runtime.getRuntime().exec("su")
//        val os = DataOutputStream(suProcess.getOutputStream())
//        val osRes = DataInputStream(suProcess.getInputStream())
//
//        if (null != os && null != osRes)
//        {
//            // Getting the id of the current user to check if this is root
//            os.writeBytes("id\n");
//            os.flush();
//
//            val currUid = osRes.readLine()
//            var exitSu = false;
//            if (null == currUid)
//            {
//                exitSu = false;
//                Log.d("ROOT", "Can't get root access or denied by user");
//            }
//            else if (true == currUid.contains("uid=0"))
//            {
//                exitSu = true;
//                Log.d("ROOT", "Root access granted");
//            }
//            else
//            {
//                exitSu = true;
//                Log.d("ROOT", "Root access rejected: " + currUid);
//            }
//
//            if (exitSu)
//            {
//                os.writeBytes("exit\n");
//                os.flush();
//            }
//        }

        if (this.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
            Log.d("TAG", "This is a system application")
        } else {
            Log.d("TAG", "This is not a system application")
        }

        vmodel = ViewModelProvider(this).get(Model::class.java)
        vmodel.start(this) { requestMultiplePermissions.launch(it) }

        setContent {
            ServiceManagerTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Column(Modifier.fillMaxWidth()) {
                        ControlScreen(vmodel)
                        InformerScreen(vmodel.mInformer)
                        if(!vmodel.mInformer.isConnected.value) {
                            EGTSScreen(vmodel.mEgts)
                            if(!vmodel.mEgts.isConnected.value) {
                                LogList(vmodel)
                            }
                        }
                        QueryPopup(vmodel.mMqtt)
                    }
                }
            }
        }

        secondActivity()
        vmodel.onStart = { secondActivity() }
    }

    private fun secondActivity(){
        if(vmodel.isVideo.value){
            // Activity options are used to select the display screen.
            val options = ActivityOptions.makeBasic()
            val dis = vmodel.displayManager!!.displays
            // Select the display screen that you want to show the second activity
            options.launchDisplayId = dis[1].displayId
            // To display on the second screen that your intent must be set flag to make
            // single task (combine FLAG_ACTIVITY_CLEAR_TOP and FLAG_ACTIVITY_NEW_TASK)
            // or you also set it in the manifest (see more at the manifest file)
            startActivity(
                Intent(this@MainActivity, VideoFullscreenActivity::class.java),
//                    Intent(this@MainActivity, VideoActivity::class.java),
                options.toBundle()
            )
        }

    }
}

@Composable
fun LogList(model: Model) {
    LazyColumn {
        items(model.logs.size) { index ->
            Text(text = model.logs[index], modifier = Modifier.padding(2.dp))
        }
    }
}

@Composable
fun ControlScreen(model: Model) {
    Column(Modifier.fillMaxWidth()) {
        model.states.forEach{
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${it.key}: ${it.value.state}",
                    modifier = Modifier.padding(10.dp),
                    textAlign = TextAlign.Center
                )
                if(it.value.state == ServiceManager.ServiceState.STOPED) {
                    Button(
                        modifier = Modifier.padding(0.dp, 0.dp, 6.dp, 0.dp),
                        onClick = {
                            model.serviceManager?.startService(
                                it.key,
                                it.value.connection,
                                it.value.payload
                            )
                        }

                    ) {
                        Text(text = "Start")
                    }
                }else{
                    Button(
                        modifier = Modifier.padding(0.dp, 0.dp, 6.dp, 0.dp),
                        onClick = {
                            model.serviceManager?.stopService(it.key)
                        }
                    ) {
                        Text(text = "Stop")
                    }
                }

            }
        }
    }
}
