package ru.glorient.servicemanager.compose

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONObject
import ru.glorient.bkn.IServiceListiner
import ru.glorient.bkn.ServiceManager
import ru.glorient.bkn.informer.AutoDirMode
import ru.glorient.bkn.informer.ManualScript
import ru.glorient.bkn.informer.RouteItem


/**
 * Класс подписчика [ServiceManager] для сервиса автоинформирования.
 *
 * Реализует Model Viewer для compose.
 */
class InformerDecorator(val onVideo: (file: String, id: Long) -> Unit = { _: String, _: Long -> }) : IServiceListiner {
    /**
     * Родительский [ServiceManager].
     */
    override var mServiceManager: ServiceManager? = null
    /**
     * Флаг установленного соединения.
     */
    var isConnected = mutableStateOf(false)

    /**
     * Название маршрута.
     */
    var mName = mutableStateOf("Маршрут")
    /**
     * Флаг прямого направления вектора движения.
     */
    var isForward = mutableStateOf(true)
    /**
     * Флаг включения GPS тригерров.
     */
    var isGPS = mutableStateOf(true)
    /**
     * Флаг аытоматического определения направления вектора движения.
     */
    var isAutoDir = mutableStateOf(true)
    /**
     * Флаг кольцевого маршрута.
     */
    var isCircle = mutableStateOf(false)
    /**
     * Список остановок текущего вектора движения.
     */
    var mRoute = mutableStateListOf<RouteItem>()
    /**
     * Список скриптов для водителя.
     */
    var mScripts = mutableStateListOf<ManualScript>()

    /**
     * Последняя посещенная остановка.
     */
    var mStationID = mutableStateOf(-1)

    /**
     * Получено сообщение от сервиса.
     *
     * @param tp сервис
     * @param data сообщение
     */
    override fun onRecieve(tp: ServiceManager.ServiceType, data: JSONObject){
        if(tp == ServiceManager.ServiceType.Informer){
//            Log.d("InformerDecorator",data.toString())
            if(data.has("route")){
                mRoute.clear()
                mStationID.value = -1
                val dt = data.getJSONArray("route")
                for(i in 0 until dt.length()){
                    val item = RouteItem(dt.getJSONObject(i))
                    mRoute.add(item)
                }
            }
            if(data.has("name")) mName.value = data.getString("name")
            if(data.has("forward")) isForward.value = data.getBoolean("forward")
            if(data.has("circle")) isCircle.value = data.getBoolean("circle")

            if(data.has("scripts")){
                mScripts.clear()
                val dt = data.getJSONArray("scripts")
                for(i in 0 until dt.length()){
                    val s = ManualScript(dt.getJSONObject(i))
                    if(s.mMenu) mScripts.add(s)
                }
            }

            if(data.has("station")){
                mStationID.value = data.getInt("station")

                var r1 = mRoute.firstOrNull { (it.mID != mStationID.value) && (it.mNextID == 1) }
                if (r1 != null){
                    val itm = r1.copy()
                    itm.mNextID = 0
                    val index = mRoute.indexOf(r1)
                    mRoute.remove(r1)
                    mRoute.add(index, itm)
                }

                r1 = mRoute.firstOrNull { (it.mID == mStationID.value) && (it.mNextID == 0) }
                if (r1 != null){
                    val itm = r1.copy()
                    itm.mNextID = 1
                    val index = mRoute.indexOf(r1)
                    mRoute.remove(r1)
                    mRoute.add(index, itm)
                }
            }

            if(data.has("display")) {
                val display = data.getJSONObject("display")
                var str: String
                if (display.has("id")) {
                    val ar = display.getJSONArray("id")
                    if(ar.length() > 0){
                        str = "["
                        for(i in 0 until ar.length()-1){
                            str+=ar.getInt(i).toString()+","
                        }
                        str+=ar.getInt(ar.length() - 1).toString()+"]:"
                    }
                }
//                if (display.has("text")) str += display.getString("text")
//                mText.value = str
            }

            if (data.has("gpsenable")) {
                isGPS.value = data.getBoolean("gpsenable")
            }

            if (data.has("autodir")) {
                isAutoDir.value = data.getString("autodir") != "none"
            }

            if (data.has("video")) {
                val video = data.getJSONObject("video")
                onVideo(video.getString("file"),video.getLong("id"))
            }
        }
    }

    /**
     * Посылка окончания проигрования видеофайла.
     *
     * @param id индификатор файла
     */
    fun endVideo(id: Long){
        mServiceManager?.sendMessage(
            ServiceManager.ServiceType.Informer,
            JSONObject("""{"endvideo":${id}}""")
        )
        //Log.i("endVideo", "${id}")
    }
    /**
     * Изменение состояния сервиса.
     *
     * @param tp сервис
     * @param state состояние
     */
    override fun onStateChange(
        tp: ServiceManager.ServiceType,
        state: ServiceManager.ServiceState,
        connection: String,
        payload: String
    ){
        if(tp == ServiceManager.ServiceType.Informer){
            isConnected.value = (state == ServiceManager.ServiceState.RUN)
        }
    }

    /**
     * Запустить скрипт.
     *
     * @param id номер скрипта
     */
    fun play(id: Int){
        mServiceManager?.sendMessage(
            ServiceManager.ServiceType.Informer,
            JSONObject("""{"play":[${id}]}""")
        )
    }

    /**
     * Переключить направление движения.
     */
    fun toggleDir(){
        mServiceManager?.sendMessage(
            ServiceManager.ServiceType.Informer,
            JSONObject("""{"dirrection":"toggle"}""")
        )
    }

    /**
     * Переключить определение остановок по GPS.
     */
    fun toggleGPS(){
        mServiceManager?.sendMessage(
            ServiceManager.ServiceType.Informer,
            JSONObject("""{"gpstoggle":"toggle"}""")
        )
    }

    /**
     * Установить режим определения вектора движения.
     *
     * @param dir режим
     */
    fun setAutoDir(dir: AutoDirMode) {
        val str = when(dir){
            AutoDirMode.HARD -> "hard"
            AutoDirMode.SOFT -> "soft"
            else -> "none"
        }
        mServiceManager?.sendMessage(
            ServiceManager.ServiceType.Informer,
            JSONObject("""{"autodir":"$str"}""")
        )
    }

}

/**
 * Отобразить панель с режимами информатора.
 *
 * @param informer ссалка на объект [InformerDecorator]
 */
@Composable
fun RouteControl(informer: InformerDecorator) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly){
        Column(
            modifier = Modifier.alignBy(FirstBaseline),
            horizontalAlignment = Alignment.CenterHorizontally,
        ){
            Text(
                text = informer.mName.value,
                modifier = Modifier
                    .padding(2.dp)
                    .align(alignment = Alignment.CenterHorizontally),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if(informer.isCircle.value) {
                Text(
                    text = "circle",
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }else{
                Text(
                    text = "bidir",
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        Row(horizontalArrangement = Arrangement.End) {
            if(informer.isForward.value) {
                Button(
                    modifier = Modifier.padding(2.dp),
                    onClick = {
                        informer.toggleDir()
                    }
                ){
                    Text(text = "-->", modifier = Modifier.padding(2.dp))
                }
            }else{
                Button(
                    modifier = Modifier.padding(2.dp),
                    onClick = {
                        informer.toggleDir()
                    }
                ){
                    Text(text = "<--", modifier = Modifier.padding(2.dp))
                }
            }
            if(informer.isAutoDir.value) {
                Button(
                    modifier = Modifier.padding(2.dp),
                    onClick = {
                        informer.setAutoDir(AutoDirMode.NONE)
                    }
                ){
                    Text(text = "AD", modifier = Modifier.padding(2.dp))
                }
            }else{
                Button(
                    modifier = Modifier.padding(2.dp),
                    onClick = {
                        informer.setAutoDir(AutoDirMode.SOFT)
                    }
                ){
                    Text(text = "MD", modifier = Modifier.padding(2.dp))
                }
            }
            if(informer.isGPS.value) {
                Button(
                    modifier = Modifier.padding(2.dp),
                    onClick = {
                        informer.toggleGPS()
                    }
                ){
                    Text(text = "AS", modifier = Modifier.padding(2.dp))
                }
            }else{
                Button(
                    modifier = Modifier.padding(2.dp),
                    onClick = {
                        informer.toggleGPS()
                    }
                ){
                    Text(text = "MS", modifier = Modifier.padding(2.dp))
                }
            }
        }
    }
}

/**
 * Отобразить панель с остановками.
 *
 * @param informer ссалка на объект [InformerDecorator]
 */
@Composable
fun RouteList(informer: InformerDecorator) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val lastState = mutableStateOf(0)
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize())  {
        items(informer.mRoute.size){ ind ->
            val item = informer.mRoute[ind]

            if (!informer.isGPS.value) {
                Row(modifier = Modifier.clickable(onClick = { if (item.mEnable) informer.play(item.mID) })) {
                    if (item.mNextID == 1) {
                        Text(
                            text = item.mName,
                            modifier = Modifier
                                .padding(5.dp)
                                .background(Color.LightGray)
                                .fillMaxWidth(),
                            color = Color.Red,
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center
                        )
                        if (lastState.value != ind){
                            lastState.value = ind
                        }
                    } else {
                        Text(
                            text = item.mName,
                            modifier = Modifier
                                .padding(5.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }else{
                Row{
                    if (item.mNextID == 1) {
                        Text(
                            text = item.mName,
                            modifier = Modifier
                                .padding(5.dp)
                                .background(Color.LightGray)
                                .fillMaxWidth(),
                            color = Color.Red,
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center
                        )
                        if (lastState.value != ind){
                            lastState.value = ind
                        }
                    } else {
                        Text(
                            text = item.mName,
                            modifier = Modifier
                                .padding(5.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

        }

        coroutineScope.launch {
            // Animate scroll to the first item
            listState.animateScrollToItem(index = lastState.value)
        }
    }
}

/**
 * Отобразить панель со скриптами для водителя.
 *
 * @param informer ссалка на объект [InformerDecorator]
 */
@Composable
fun ScriptList(informer: InformerDecorator) {
    LazyRow {
        items(informer.mScripts.size) { index ->
            Button(
                onClick = {
                    informer.play(informer.mScripts[index].mID)
                },
                modifier = Modifier.padding(2.dp)
            ){
                Text(text = informer.mScripts[index].mName)
            }
        }
    }
}

/**
 * Экран с интеграцией всех визуальных компонентов.
 *
 * @param informer ссалка на объект [InformerDecorator]
 */
@Composable
fun InformerScreen(informer: InformerDecorator) {
    if(informer.isConnected.value) {
        Column(Modifier.fillMaxWidth()) {
            RouteControl(informer)
            ScriptList(informer)
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(2.dp)
                    .background(Color.Transparent)
                    .border(
                        border = BorderStroke(
                            2.dp,
                            Color.Black
                        )
                    )
                    .padding(
                        start = if (Dp.Unspecified != Dp.Unspecified) Dp.Unspecified else 5.dp,
                        top = if (Dp.Unspecified != Dp.Unspecified) Dp.Unspecified else 5.dp,
                        end = if (Dp.Unspecified != Dp.Unspecified) Dp.Unspecified else 5.dp,
                        bottom = if (Dp.Unspecified != Dp.Unspecified) Dp.Unspecified else 5.dp
                    ),
                contentAlignment = Alignment.TopCenter
            ) {
                RouteList(informer)
            }
        }
    }
}

/**
 * Compose Preview.
 */
@Preview
@Composable
fun InformerScreenPreview() {
    val informer = InformerDecorator()
    informer.isConnected.value = true
    informer.mScripts.add(ManualScript(101, true, "Script1"))
    informer.mScripts.add(ManualScript(102, false, "Script2"))
    informer.mRoute.add(RouteItem(1, "Остановка1", 0, true))
    informer.mRoute.add(RouteItem(2, "Остановка2", 1, true))
    informer.mRoute.add(RouteItem(3, "Остановка3", 0, true))
    informer.mStationID.value = 2
    MaterialTheme{
        // A surface container using the 'background' color from the theme
        Surface(color = MaterialTheme.colors.background) {
             InformerScreen(informer)
        }
    }
}