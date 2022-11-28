package ru.glorient.servicemanager.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import ru.glorient.bkn.IServiceListiner
import ru.glorient.bkn.ServiceManager

class EGTSDecorator : IServiceListiner {
    /**
     * Родительский [ServiceManager].
     */
    override var mServiceManager: ServiceManager? = null
    /**
     * Флаг установленного соединения.
     */
    var isConnected = mutableStateOf(false)


    /**
     * Получено сообщение от сервиса.
     *
     * @param tp сервис
     * @param data сообщение
     */
    override fun onRecieve(tp: ServiceManager.ServiceType, data: JSONObject){
        if(tp == ServiceManager.ServiceType.EGTS){
        }
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
        if(tp == ServiceManager.ServiceType.EGTS){
            isConnected.value = (state == ServiceManager.ServiceState.RUN)
        }
    }

    fun setCounter(cn: Int){
        mServiceManager?.sendMessage(
            ServiceManager.ServiceType.EGTS,
            JSONObject("""{"sensors":{"cn1":$cn}}""")
        )
    }

    fun setGasLevel(level: Double){
        mServiceManager?.sendMessage(
            ServiceManager.ServiceType.EGTS,
            JSONObject("""{"sensors":{"gas":$level}}""")
        )
    }

    fun setOdometer(value: Double){
        mServiceManager?.sendMessage(
            ServiceManager.ServiceType.EGTS,
            JSONObject("""{"sensors":{"odometer":$value}}""")
        )
    }

    fun setDout(value: Int){
        mServiceManager?.sendMessage(
            ServiceManager.ServiceType.EGTS,
            JSONObject("""{"sensors":{"dout":$value}}""")
        )
    }

}

@Composable
fun EGTSScreen(decorator: EGTSDecorator) {
    val odometer = remember {mutableStateOf("120000")}
    val cn = remember {mutableStateOf("1234")}
    val gas = remember {mutableStateOf("10.1")}
    val dout = remember {mutableStateOf("16")}
    if(decorator.isConnected.value) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = odometer.value,
                    onValueChange = { odometer.value = it },
                    label = { Text("Odometer") }
                )
                Button(
                    modifier = Modifier.padding(10.dp, 0.dp, 0.dp, 2.dp),
                    onClick = {
                        decorator.setOdometer(odometer.value.toDouble())
                    }

                ) {
                    Text(text = "Set")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
                ) {
                TextField(
                    value = cn.value,
                    onValueChange = { cn.value = it },
                    label = { Text("Counter") }
                )
                Button(
                    modifier = Modifier.padding(10.dp, 0.dp, 0.dp, 2.dp),
                    onClick = {
                        decorator.setCounter(cn.value.toInt())
                    }

                ) {
                    Text(text = "Set")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = gas.value,
                    onValueChange = { gas.value = it },
                    label = { Text("GasLevel") }
                )
                Button(
                    modifier = Modifier.padding(10.dp, 0.dp, 0.dp, 2.dp),
                    onClick = {
                        decorator.setGasLevel(gas.value.toDouble())
                    }

                ) {
                    Text(text = "Set")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = dout.value,
                    onValueChange = { dout.value = it },
                    label = { Text("Pins") }
                )
                Button(
                    modifier = Modifier.padding(10.dp, 0.dp, 0.dp, 2.dp),
                    onClick = {
                        decorator.setDout(dout.value.toInt())
                    }

                ) {
                    Text(text = "Set")
                }
            }
        }
    }
}

/**
 * Compose Preview.
 */
@Preview
@Composable
fun EGTSScreenPreview() {
    val decorator = EGTSDecorator()
    decorator.isConnected.value = true
    MaterialTheme{
        // A surface container using the 'background' color from the theme
        Surface(color = MaterialTheme.colors.background) {
            EGTSScreen(decorator)
        }
    }
}