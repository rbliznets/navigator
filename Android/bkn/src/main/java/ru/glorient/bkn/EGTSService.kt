package ru.glorient.bkn

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.GnssStatus
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONException
import org.json.JSONObject
import ru.glorient.bkn.egts.*
import java.util.*


open class EGTSService : GPSService() {

    private var mEgts: EGTSTransportLevel? = null
    private val mId = EGTSId()

    protected var mSatellites: Int? = null
    protected var m3D: Int = 1
    protected var mPDOP: Int? = null
    protected var mHDOP: Int? = null
    protected var mVDOP: Int? = null

    protected var mOdometer: Double = 0.0
    protected var mDOUT: UByte? = null
    protected var mGasLevel: Double? = null
    protected var mCN1: UInt? = null

    protected val mFIFOBufer: Queue<EGTSRecord> = LinkedList()

    /**
     * Callback for GnssStatus events.
     */
    private var mGnssStatusCallback: GnssStatus.Callback = object : GnssStatus.Callback(){
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            super.onSatelliteStatusChanged(status)
            var cnt = 0
            for( index in 0 until status.satelliteCount){
                if(status.usedInFix(index))cnt++
            }
            mSatellites = cnt
            //Log.d(TAG, "onSatelliteStatusChanged: $cnt:${status.getSatelliteCount()}")
        }
    }
    private var mOnNmeaMessageListener: OnNmeaMessageListener =
        OnNmeaMessageListener { message, _ ->
            val str = message.split('$').lastOrNull { it.contains("GSA,") }
            if(str != null){
                val ar = str.split(',')
                if(ar.size > 17){
                    m3D = ar[2].toIntOrNull() ?: 1
                    var x = ar[15].toFloatOrNull() ?: 0f
                    mPDOP = (x*10).toInt()
                    x = ar[16].toFloatOrNull() ?: 0f
                    mHDOP = (x*10).toInt()
                    x = ar[17].toFloatOrNull() ?: 0f
                    mVDOP = (x*10).toInt()
                }
                //Log.d(TAG, "$message")
            }
        }

    @SuppressLint("HardwareIds")
    override fun onCreate() {
        super.onCreate()

        try{
            val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            mId.mIMEI=telephonyManager.imei
            mId.mIMSI=telephonyManager.subscriberId
        }catch (e: Exception){
            Log.e(TAG, "EGTSService init:${e.message}")
        }
        try {
            val subsManager =
                getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val subsList = subsManager.getActiveSubscriptionInfoList()
            if (subsList != null) {
                mId.mICCID = subsList[0].iccId
            }
        }catch (e: Exception){
            Log.e(TAG, "EGTSService init:${e.message}")
        }
        Log.d(TAG, "$mId")

        mSatellites = null
        m3D = 1
        mPDOP = null
        mVDOP = null
        mHDOP = null
        mFIFOBufer.clear()
        try{
            val locationManager =  getSystemService(LOCATION_SERVICE) as LocationManager
            if(!locationManager.registerGnssStatusCallback(mGnssStatusCallback, null)){
                Log.e(TAG, "EGTSService init: registerGnssStatusCallback failed")
            }
            if(!locationManager.addNmeaListener( mOnNmeaMessageListener, null)){
                Log.e(TAG, "EGTSService init: addNmeaListener failed")
            }
        }catch (e: Exception){
            Log.e(TAG, "EGTSService init:${e.message}")
        }

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val loc = locationFilter(locationResult) ?: return

                val rec = EGTSRecord(
                    1u,
                    0x01u,
                    mId.mID,
                    null,
                    null,
                    EGTSRecord.SERVICE.EGTS_TELEDATA_SERVICE,
                    EGTSRecord.SERVICE.EGTS_TELEDATA_SERVICE
                )

                val sr1=EGTSSubRecordPosData(
                    loc.time,
                    loc.latitude,
                    loc.longitude,
                    loc.speed,
                    loc.bearing,
                    loc.altitude,
                    mOdometer
                )
                var flags = sr1.FLG
                when(m3D){
                    1 -> flags = (flags.toInt() and 0xfe).toUByte()
                    3 -> flags = (flags.toInt() or 0x02).toUByte()
                }
                sr1.FLG = flags
                rec.RD.add(sr1)

                rec.RD.add(EGTSSubRecordExtPosData(mVDOP, mHDOP, mPDOP, mSatellites))
                if(mDOUT != null)rec.RD.add(EGTSSubRecordADSensorsData(mDOUT!!))
                if(mCN1 != null)rec.RD.add(EGTSSubRecordCountersData(mCN1))
                if(mGasLevel != null)rec.RD.add(EGTSSubRecordLiquidLevelSensorData(LIQUID_LEVEL_SENSOR_FLAG, LIQUID_LEVEL_SENSOR_MADDR,(mGasLevel!!*10).toUInt()))

                if((mEgts == null) || (!mEgts!!.isRun)){
                    sr1.FLG = (sr1.FLG.toInt() or 0x08).toUByte()
                    mFIFOBufer.add(rec)
                    if(mFIFOBufer.size > FIFO_SIZE) mFIFOBufer.poll()
                    //Log.d(TAG, "${mFIFOBufer.size}->${rec.toString(2)}")
                } else {
                    EGTSCommand(
                        rec,
                        mEgts!!,
                        { b: Boolean, s: String, o: Any? ->
                            if (!b){
                                val rec = o as EGTSRecord
                                val sr1 = rec.RD[0] as EGTSSubRecordPosData
                                //sr1.SPD = 20u//@@@@
                                sr1.FLG = (sr1.FLG.toInt() or 0x08).toUByte()
                                mFIFOBufer.add(rec)
                                if(mFIFOBufer.size > FIFO_SIZE) mFIFOBufer.poll()
                                //Log.d(TAG, "${mFIFOBufer.size}->${rec.toString(2)}")
                                sendMessage("""{"warning":"GPS ${s}","fifo":${mFIFOBufer.size}}""")
                            }else{
                                //Log.d(TAG, "?${mFIFOBufer.size}->${(o as EGTSRecord).toString(2)}")
                                if(mFIFOBufer.isNotEmpty()){
                                    runBlocking { mChannelRx.send("""{"next":null}""") }
                                }
                            }
                        }
                    )
                }
                newSending()
            }
        }

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        start()
        sendMessage("""{"state":"connecting"}""")

        mMainJob= GlobalScope.launch {
            val options = intent.getStringExtra("connection")
            if(options != null){
                try {
                    val cn = JSONObject(options)
                    mId.mID = if(cn.has("transport_id")) cn.getInt("transport_id").toUInt() else (Math.random() *10000).toUInt()
                    mEgts = EGTSTransportLevel(
                        if (cn.has("server")) cn.getString("server") else "10.0.2.2",
                        if (cn.has("port")) cn.getInt("port") else 7001,
                        if (cn.has("timeout")) cn.getInt("timeout") else 3000,
                        mId,
                        { msg -> sendMessage("""{"error":"$msg"}""") },
                        { on ->
                            if (on) sendMessage("""{"state":"run"}""")
                            else sendMessage("""{"state":"connecting"}""")
                        })
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
//                Log.d(TAG, str)
                try {
                    val msg = JSONObject(str)
                    if(msg.has("cmd")) {
                        if (msg.getString("cmd") == "stop") {
                            mEgts?.close()
                            break
                        }
                    }
                    if(msg.has("next")) {
                        //Log.d(TAG,"next!!! ${mFIFOBufer.size}")
                        val rec = mFIFOBufer.poll()
                        if(rec != null){
                            EGTSCommand(
                                rec,
                                mEgts!!,
                                { b: Boolean, s: String, o: Any? ->
                                    if (!b){
                                        val rec = o as EGTSRecord
                                        mFIFOBufer.add(rec)
                                        if(mFIFOBufer.size > FIFO_SIZE) mFIFOBufer.poll()
                                        //Log.d(TAG, "${mFIFOBufer.size}->${rec.toString(2)}")
                                        sendMessage("""{"warning":"GPS $s","fifo":${mFIFOBufer.size}}""")
                                    }else{
                                        sendMessage("""{"info":"from fifo","fifo":${mFIFOBufer.size}}""")
                                        //Log.d(TAG, "!${mFIFOBufer.size}->${(o as EGTSRecord).toString(2)}")
                                        if(mFIFOBufer.isNotEmpty()){
                                            runBlocking { mChannelRx.send("""{"next":null}""") }
                                        }
                                    }
                                })
                        }
                    }

                    setGPS(msg)
                    setSensors(msg)

                }catch (e: JSONException){
                    sendMessage("""{"warning":"the message string is not json (${e})"}""")
                }
            }
            stopLocationUpdates()

            try{
                val locationManager =  getSystemService(LOCATION_SERVICE) as LocationManager
                locationManager.unregisterGnssStatusCallback(mGnssStatusCallback)
                locationManager.removeNmeaListener(mOnNmeaMessageListener)
            }catch (e: Exception){
                Log.e(TAG, "EGTSService init:${e.message}")
            }

            stopSelf(startId)
        }
        return START_STICKY
    }

    private fun setSensors(msg: JSONObject) {
        if(msg.has("sensors")) {
            val dt = msg.getJSONObject("sensors")
            if(dt.has("cn1")){
                mCN1 = dt.getInt("cn1").toUInt()
            }
            if(dt.has("odometer")){
                mOdometer = dt.getDouble("odometer")
            }
            if(dt.has("dout")){
                mDOUT = dt.getInt("dout").toUByte()
            }
            if(dt.has("gas")){
                mGasLevel = dt.getDouble("gas")
            }
        }
    }

    override fun getOuputName(): String = toOutName
    override fun getInputName(): String = toInName

    companion object {
        private val TAG = EGTSService::class.java.simpleName
        val toOutName = "fromEGTS"
        val toInName = "toEGTS"
        val FIFO_SIZE = 10000

        val LIQUID_LEVEL_SENSOR_FLAG: UByte = 32u
        val LIQUID_LEVEL_SENSOR_MADDR: UShort = 3u
    }

}