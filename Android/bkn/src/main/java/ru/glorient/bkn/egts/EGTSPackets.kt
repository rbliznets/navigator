package ru.glorient.bkn.egts

import java.util.*

/**
 * Интерфейс подзаписи
 *
 * | **Бит 7-0** | **Тип** | **Тип**** данных **|** Размер, байт** |
 * | --- | --- | --- | --- |
 * | SRT (Subrecord Type) | M | BYTE | 1 |
 * | --- | --- | --- | --- |
 * | SRL (Subrecord Length) | M | USHORT | 2 |
 * | SRD (Subrecord Data) | O | BINARY | 0… 65495 |
 *
 * - SRT – (SubrecordType), тип подзаписи (подтип передаваемых данных в рамках общего набора типов одного Сервиса). Тип 0 – специальный, зарезервирован за подзаписью подтверждения данных для каждого сервиса. Конкретные значения номеров типов подзаписей определяются логикой самого Сервиса. Протокол оговаривает лишь то, что этот номер должен присутствовать, а нулевой идентификатор зарезервирован;
 * - SRL – (SubrecordLength), длина данных в байтах подзаписи в поле SRD;
 * - SRD – (SubrecordData), данные подзаписи. Наполнение данного поля специфично для каждого сочетания идентификатора типа Сервиса и типа подзаписи.
 */
interface EGTSSubPacket : EGTSPacket {
    companion object {
        /**
         * Фабрика объектов подзаписей
         *
         * @param data Массив данных
         * @param offset Смещение в [data]
         * @return Объект класса взависимости от типа плдзаписи
         */
        fun getSR(data: UByteArray, offset: Int = 0): EGTSSubPacket {
            return when(data[offset]){
                EGTS_SR_RECORD_RESPONSE -> EGTSSubRecordResponse(data, offset)
                EGTS_SR_TERM_IDENTITY -> EGTSSubRecordTermIdentity(data, offset)
                EGTS_SR_RESULT_CODE -> EGTSSubRecordResultCode(data, offset)
                EGTS_SR_POS_DATA -> EGTSSubRecordPosData(data, offset)
                else -> EGTSSubRecord(data, offset)
            }
        }

        /**
         * ПОДЗАПИСЬ EGTS\_SR\_RECORD\_RESPONSE
         *
         * | **Бит 7-0** | **Тип** | **Тип**** данных **|** Размер, байт** |
         * | --- | --- | --- | --- |
         * | CRN (Confirmed Record Number) | M | USHORT | 2 |
         * | RST (Record Status) | M | BYTE | 1 |
         *
         * ```
         * Поля подзаписи EGTS\_SR\_RECORD\_RESPONSE:
         * ```
         *
         * - CRN – (ConfirmedRecordNumber), номер подтверждаемой записи (значение поля RN из обрабатываемой записи);
         * - RST – (RecordStatus), статус обработки записи.
         *
         * ```
         * При получении подтверждения Отправителем, он анализирует поле RST подзаписи EGTS\_SR\_ RECORD\_RESPONSE и, в случае получения статуса об успешной обработке, стирает запись из внутреннего хранилища, иначе, в случае ошибки и в зависимости от причины, производит соответствующие действия.Рекомендуется совмещать подтверждение транспортного уровня (тип пакета EGTS\_PT\_RESPONSE) с подзаписями – подтверждениями уровня поддержки услуг EGTS\_SR\_RECORD\_RESPONSE.
         * ```
         */
        const val EGTS_SR_RECORD_RESPONSE: UByte = 0u
        /**
         * ПОДЗАПИСЬ EGTS\_SR\_TERM\_IDENTITY
         *
         * | **Бит 7-0** | **Тип** | **Тип**** данных **|** Размер, байт** |
         * | --- | --- | --- | --- |
         * | TID (Terminal Identifier) | M | UINT | 4 |
         * | Flags | M | BYTE | 1 |
         * | MNE BSE NIDE SSRA LNGCE IMSIE IMEIE HDIDE | | | |
         * | HDID (Home Dispatcher Identifier) | O | USHORT | 2 |
         * | IMEI (International Mobile Equipment Identity) | O | STRING | 15 |
         * | IMSI (International Mobile Subscriber Identity) | O | STRING | 16 |
         * | LNGC (Language Code) | O | STRING | 3 |
         * | NID (Network Identifier) | O | BINARY | 3 |
         * | BS (Buffer Size) | O | USHORT | 2 |
         * | MSISDN (Mobile Station Integrated Services Digital Network Number) | O | STRING | 15 |
         *
         * ```
         * Поля подзаписи EGTS\_SR\_TERM\_IDENTITY:
         * ```
         *
         * - TID – (TerminalIdentifier), уникальный идентификатор, назначаемый при программировании АС. Наличие значения 0 в данном поле означает, что АС не прошел процедуру конфигурирования, или прошел её не полностью. Данный идентификатор назначается оператором и однозначно определяет набор учетных данных АС. TID назначается при инсталляции АС как дополнительного оборудования и передаче оператору учетных данных АС (IMSI, IMEI, serial\_id). В случае использования АС в качестве штатного устройства, TID сообщается оператору автопроизводителем вместе с учетными данными (VIN, IMSI, IMEI);
         * - HDIDE – (HomeDispatcherIdentifierExists), битовый флаг, который определяет наличие поля HDID в подзаписи (если бит равен 1, то поле передаётся, если 0, то не передаётся);
         * - IMEIE – (InternationalMobileEquipmentIdentityExists), битовый флаг, который определяет наличие поля IMEI в подзаписи (если бит равен 1, то поле передаётся, если 0, то не передаётся);
         * - IMSIE – (InternationalMobileSubscriberIdentityExists), битовый флаг, который определяет наличие поля IMSI в подзаписи (если бит равен 1, то поле передаётся, если 0, то не передаётся);
         * - LNGCE – (LanguageCodeExists), битовый флаг, который определяет наличие поля LNGC в подзаписи (если бит равен 1, то поле передаётся, если 0, то не передаётся);
         * - SSRA – битовый флаг предназначен для определения алгоритма использования Сервисов (если бит равен 1, то используется «простой» алгоритм, если 0, то алгоритм «запросов» на использование Cервисов);
         * - NIDE – (NetworkIdentifierExists), битовый флаг определяет наличие поля NID в подзаписи (если бит равен 1, то поле передаётся, если 0, то не передаётся);
         * - BSE – (BufferSizeExists), битовый флаг, определяющий наличие поля BS в подзаписи (если бит равен 1, то поле передаётся, если 0, то не передаётся);
         * - MNE – (MobileNetworkExists), битовый флаг, определяющий наличие поля MSISDN в подзаписи (если бит равен 1, то поле передаётся, если 0, то не передаётся);
         * - HDID – (HomeDispatcherIdentifier), идентификатор «домашней» ТП (подробная учётная информация о терминале хранится на данной ТП);
         * - IMEI – (InternationalMobileEquipmentIdentity), идентификатор мобильного устройства (модема). При невозможности определения данного параметра, АС должна заполнять данное поле значением 0 во всех 15-ти символах;
         * - IMSI – (International Mobile Subscriber Identity), идентификатормобильногоабонента. При невозможности определения данного параметра, АС должна заполнять данное поле значением 0 во всех 16-ти символах;
         * - LNGC – (LanguageCode), код языка, предпочтительного к использованию на стороне АС, по ISO 639-2, например, «rus» – русский;
         * - NID – (NetworkIdentifier), идентификатор сети оператора, в которой зарегистрирована АС на данный момент. Используются 20 младших бит. Представляет пару кодов MCC-MNC (на основе рекомендаций ITU-TE.212). Таблица 8 иллюстрирует структуру поля NID;
         * - BS – (BufferSize), максимальный размер буфера приёма АС в байтах. Размер каждого пакета информации, передаваемого на АС, не должен превышать данного значения. Значение поля BS может принимать различные значения, например 800, 1000, 1024, 2048, 4096 и т.д., и зависит от реализации аппаратной и программной частей конкретной АС;
         * - MSISDN – (Mobile Station Integrated Services Digital Network Number), телефонныйномермобильногоабонента. При невозможности определения данного параметра, устройство должно заполнять данное поле значением 0 во всех 15-ти символах (формат описан в [6]).
         *
         * ```
         * Передача поля HDID определяется настройками АС и целесообразна при возможности подключении АС к ТП, отличной от «домашней», например, при использовании территориально распределённой сети ТП. При использовании только одной «домашней» ТП, передача HDID не требуется.«Простой» алгоритм использования Сервисов, как было отмечено в подразделе 6.2.1, подразумевает, что для АС (авторизуемой ТП) доступны все Сервисы, и в таком режиме АС разрешено сразу отправлять данные для требуемого сервиса. В зависимости от действующих на авторизующей ТП для данной АС разрешений, в ответ на пакет с данными для Сервиса может быть возвращена запись-подтверждение с соответствующим признаком ошибки. В системах с простым распределением прав на использование Сервисов рекомендуется применять, именно, «Простой» алгоритм. Это сокращает объём передаваемого трафика и время, затрачиваемое АС на авторизацию.Алгоритм «запросов» на использование сервисов подразумевает, что перед тем, как использовать тот или иной тип Сервиса (отправлять данные), АС должна получить от ТП информацию о доступных для использования Сервисов. Запрос на использование сервисов может осуществляется как на этапе авторизации, так и после неё. На этапе авторизации запрос на использование того или иного сервиса производится путём добавления подзаписей типа SR\_SERVICE\_INFO и установка бита 7 поля SRVP в значение 1. После процедуры авторизации запрос на использование сервиса может быть осуществлён также при помощи подзаписей SR\_ SERVICE\_INFO.
         * ```
         */
        const val EGTS_SR_TERM_IDENTITY: UByte = 1u
        const val EGTS_SR_MODULE_DATA: UByte = 2u
        const val EGTS_SR_VEHICLE_DATA: UByte = 3u
        const val EGTS_SR_DISPATCHER_IDENTITY: UByte = 5u
        const val EGTS_SR_AUTH_PARAMS: UByte = 6u
        const val EGTS_SR_AUTH_INFO: UByte = 7u
        const val EGTS_SR_SERVICE_INFO: UByte = 8u

        /**
         * ПОДЗАПИСЬ EGTS\_SR\_RESULT\_CODE.
         *
         * | **Бит 7-0** | **Тип** | **Тип**** данных **|** Размер, байт** |
         * | --- | --- | --- | --- |
         * | RCD (Result Code) | M | BYTE | 1 |
         *
         * Поля подзаписи EGTS\_SR\_SERVICE\_INFO:
         *
         * - RCD – (ResultCode), код, определяющий результат выполнения операции авторизации
         */
        const val EGTS_SR_RESULT_CODE: UByte = 9u

        const val EGTS_SR_POS_DATA: UByte = 16u
        const val EGTS_SR_EXT_POS_DATA: UByte = 17u
        const val EGTS_SR_AD_SENSORS_DATA: UByte = 18u
        const val EGTS_SR_COUNTERS_DATA: UByte = 19u
        const val EGTS_SR_STATE_DATA: UByte = 20u
        const val EGTS_SR_LOOPIN_DATA: UByte = 22u
        const val EGTS_SR_ABS_DIG_SENS_DATA: UByte = 23u
        const val EGTS_SR_ABS_AN_SENS_DATA: UByte = 24u
        const val EGTS_SR_ABS_CNTR_DATA: UByte = 25u
        const val EGTS_SR_ABS_LOOPIN_DATA: UByte = 26u
        const val EGTS_SR_LIQUID_LEVEL_SENSOR: UByte = 27u
        const val EGTS_SR_PASSENGERS_COUNTERS: UByte = 28u
    }
}

/**
 * Базовый класс подзаписи
 */
open class EGTSSubRecord : EGTSSubPacket {
    /**
     * SubrecordType, тип подзаписи (подтип передаваемых данных в рамках общего набора типов одного Сервиса).
     *
     * Тип 0 – специальный, зарезервирован за подзаписью подтверждения данных для каждого сервиса.
     * Конкретные значения номеров типов подзаписей определяются логикой самого Сервиса.
     * Протокол оговаривает лишь то, что этот номер должен присутствовать, а нулевой идентификатор зарезервирован.
     */
    var SRT: UByte = 0u
        private set
    /**
     * SubrecordData, данные подзаписи. Наполнение данного поля специфично для каждого сочетания идентификатора типа Сервиса и типа подзаписи.
     */
    var SRD: UByteArray? = null
        protected set

    /**
     * SubrecordLength, длина данных в байтах подзаписи в поле SRD.
     */
    val SRL: UShort
        get() {
            return SRD?.size?.toUShort() ?: 0u
        }

    /**
     * Конструктор для формирования подзаписи на передачу.
     *
     * @param srt SubrecordType
     * @param srd SubrecordData
     */
    constructor(srt: UByte, srd: UByteArray? = null) {
        SRT = srt
        SRD = srd
    }

    /**
     * Конструктор для формирования подзаписи из полученных данных.
     *
     * @param data Массив данных
     * @param offset Смещение в [data]
     */
    constructor(data: UByteArray, offset: Int = 0) {
        var srl = 0
        if ((data.size - offset) > 2) {
            SRT = data[offset]
            srl = convert(data[offset + 1], data[offset + 2]).toInt()

        } else {
            throw EGTSException("EGTSSubRecord parsing failed")
        }
        if (srl > 0) {
            if (srl <= (data.size - offset - 3)) {
                SRD = data.copyOfRange(offset + 3, srl + offset + 3)
            } else {
                throw EGTSException("EGTSSubRecord parsing failed")
            }
        }
    }

    /**
     * Флаг необходимости заново сформировать [SRD].
     */
    protected var needRefresh = false
    /**
     * Виртуальный метод сформирования [SRD].
     */
    protected open fun refreshSRD(){needRefresh = false}

    /**
     * Размер данных под подзапись.
     */
    override val size: Int
        get() {
            var res= 3
            if(needRefresh)refreshSRD()
            val srd = SRD
            return if (srd != null) srd.size + 3 else 3
        }

    /**
     * Скопировать данные подзаписи в массив [data].
     *
     * @param data Массив данных
     * @param offset Смещение в [data]
     * @return Новое смещение в [data]
     */
    override fun copyData(data: UByteArray, offset: Int): Int {
        if ((data.size - offset) >= size) {
            data[offset] = SRT
            val srd = SRD
            if (srd != null) {
                data[offset + 1] = srd.size.toUByte()
                data[offset + 2] = (srd.size ushr 8).toUByte()
                srd.copyInto(data, offset + 3)
            } else {
                data[offset + 1] = 0u
                data[offset + 2] = 0u
            }
        } else {
            throw EGTSException("EGTSSubRecord copping failed")
        }
        return offset + size
    }

    /**
     * Подзапись в виде строки для отладки.
     *
     * @return frendly data
     */
    override fun toString(): String {
        var str = "0x${SRT.toString(16)}"
        if (SRL > 0u) {
            str += "(Size=${SRL}):" + SRD?.toHexString()
        }
        return str
    }

    /**
     * Подзапись в виде строки для отладки с отступами.
     *
     * @param spaces Количество пробелов
     * @return frendly data
     */
    override fun toString(spaces: Int): String {
        var str = " ".repeat(spaces) + toString() + "\n"
        return str
    }
}

class EGTSSubRecordPosData : EGTSSubRecord {
    var NTM: UInt = 0u
        set(value) {
            needRefresh = true
            field =  value
        }
    var LAT: UInt = 0u
        set(value) {
            needRefresh = true
            field =  value
        }
    var LONG: UInt = 0u
        set(value) {
            needRefresh = true
            field =  value
        }
    var FLG: UByte = 0u
        set(value) {
            needRefresh = true
            field =  value
        }
    var SPD: UShort = 0u
        set(value) {
            needRefresh = true
            field =  value
        }
    var DIR: UByte  = 0u
        set(value) {
            needRefresh = true
            field =  value
        }
    var ODM: UByteArray = UByteArray(3)
        set(value) {
            needRefresh = true
            field =  value
        }
    var DIN: UByte = 0u
        set(value) {
            needRefresh = true
            field =  value
        }
    var SRC: UByte = 0u
        set(value) {
            needRefresh = true
            field =  value
        }
    var ALT: UByteArray? = null
        set(value) {
            needRefresh = true
            if(value == null) FLG = FLG and 0x7fu
            else FLG = FLG or 0x80u
            field =  value
        }
    var SRCD: UShort? = null
        set(value) {
            needRefresh = true
            field =  value
        }

    override fun refreshSRD(){
        var size = 21
        if(ALT != null) size += 3
        if(SRCD != null) size += 2
        SRD = UByteArray(size)

        SRD!![0] = NTM.toUByte()
        SRD!![1] = (NTM.toLong() ushr 8).toUByte()
        SRD!![2] = (NTM.toLong() ushr 16).toUByte()
        SRD!![3] = (NTM.toLong() ushr 24).toUByte()
        SRD!![4] = LAT.toUByte()
        SRD!![5] = (LAT.toLong() ushr 8).toUByte()
        SRD!![6] = (LAT.toLong() ushr 16).toUByte()
        SRD!![7] = (LAT.toLong() ushr 24).toUByte()
        SRD!![8] = LONG.toUByte()
        SRD!![9] = (LONG.toLong() ushr 8).toUByte()
        SRD!![10] = (LONG.toLong() ushr 16).toUByte()
        SRD!![11] = (LONG.toLong() ushr 24).toUByte()
        SRD!![12] = FLG
        SRD!![13] = SPD.toUByte()
        SRD!![14] = (SPD.toInt() ushr 8).toUByte()
        SRD!![15] = DIR
        ODM.copyInto(SRD!!,16)
        SRD!![19] = DIN
        SRD!![20] = SRC
        var ind = 21

        if(ALT != null){
            ALT!!.toByteArray().toUByteArray().copyInto(SRD!!,ind)
            ind += +3
        }
        if(SRCD != null){
            SRD!![ind] = SRCD!!.toUByte()
            SRD!![ind+1] = (SRCD!!.toInt() ushr 8).toUByte()
           ind += 2
        }
        needRefresh = false
    }

    constructor(ntm: UInt, lat: UInt, long: UInt,
                flg: UByte = 0u,
                spd: UShort = 0u,
                dir: UByte = 0u,
                odm: UByteArray = UByteArray(3),
                din: UByte = 0u,
                src: UByte = 0u,
                alt: UByteArray? = null,
                secd: UShort? = null) : super (EGTSSubPacket.EGTS_SR_POS_DATA, null) {
        NTM = ntm
        LAT = lat
        LONG = long
        FLG = flg
        SPD = spd
        DIR = dir
        ODM = odm
        DIN = din
        SRC = src
        ALT = alt
        SRCD = secd
    }

    constructor(time: Long, lat: Double, long: Double,
                spd: Float, dir: Float,
                alt: Double? = null,
                odm: Double = 0.0
                ) : super (EGTSSubPacket.EGTS_SR_POS_DATA, null) {
        val c = Calendar.getInstance()
        c.time = Date(time)
        c.add(Calendar.YEAR, -40)
        NTM = (c.timeInMillis/1000).toUInt()
        LAT = ((Math.abs(lat) * 0xffffffff)/90.0).toUInt()
        LONG = ((Math.abs(long) * 0xffffffff)/180.0).toUInt()
        FLG = 1u
        if(lat < 0) FLG = FLG or 0x20u
        if(long < 0) FLG = FLG or 0x40u
        SPD = (spd * 10).toUInt().toUShort()
        val dir1 = dir.toUInt().toUShort()
        if(dir1 > 255u) SPD = SPD or 0x8000u
        DIR = dir1.toUByte()
        val x = (odm * 10).toInt()
        ODM = ubyteArrayOf(x.toUByte(),(x ushr 8).toUByte(),(x ushr 16).toUByte())
        if(alt != null) {
            if (alt < 0) SPD = SPD or 0x4000u
            val alt1 = Math.abs(alt).toUInt()
            ALT = ubyteArrayOf(alt1.toUByte(), (alt1.toInt() ushr 8).toUByte(), (alt1.toInt() ushr 16).toUByte())
         }
    }

    private constructor(srt: UByte, srd: UByteArray? = null) : super(srt, srd)

    constructor(data: UByteArray, offset: Int = 0) : super(data, offset) {
        if (SRT != EGTSSubPacket.EGTS_SR_POS_DATA) throw EGTSException("EGTSSubRecordPosData parsing failed")
        try {
            NTM = convert(SRD!![0],SRD!![1],SRD!![2],SRD!![3])
            LAT = convert(SRD!![4],SRD!![5],SRD!![6],SRD!![7])
            LONG = convert(SRD!![8],SRD!![9],SRD!![10],SRD!![11])
            FLG = SRD!![12]
            SPD = convert(SRD!![13],SRD!![14])
            DIR = SRD!![15]
            ODM = SRD!!.copyOfRange(16,19)
            DIN = SRD!![19]
            SRC = SRD!![20]
            var ind = 21
            if((FLG.toInt() and 0x80) != 0 ){
                ALT = SRD!!.copyOfRange(ind,ind+3)
                ind += 3
            }
        }catch(e: Exception){
            throw EGTSException("EGTSSubRecordPosData parsing failed")
        }
    }

    override fun toString(): String {
        var str = "EGTS_SR_POS_DATA(${FLG.toString(2).padStart(8,'0')}):"
        if((FLG.toInt() and 0x01) == 0)str += "invalid,"
        val c = Calendar.getInstance()
        c.time = Date(NTM.toLong()*1000)
        c.add(Calendar.YEAR, 40)
        str += (c.time).toString()
        str += " "+"%.6f".format(((LONG.toDouble()*180.0)/0xffffffff))
        if((FLG.toInt() and 0x40) == 0)str += "E"
        else str += "W"
        str += " "+"%.6f".format(((LAT.toDouble()*90.0)/0xffffffff))
        if((FLG.toInt() and 0x20) == 0)str += "N"
        else str += "S"
        str += ";SPD="+"%.1f".format((SPD.toInt() and 0x3fff).toDouble()/10)
        val dir = if((SPD.toInt() and 0x8000) == 0) DIR.toInt() else  DIR.toInt() + 0x100
        str += ";DIR=${dir}"
        str += ";ODM="+"%.1f".format(convert(ODM!![0],ODM!![1],ODM!![2],0u).toDouble()/10)
        str += ";DIN=${DIN.toString(2).padStart(8,'0')}"
        str += ";SRC=${SRC}"
        if(ALT != null) {
            if((SPD.toInt() and 0x4000) != 0)str += ";ALT=-${convert(ALT!![0], ALT!![1], ALT!![2], 0u)}"
            else str += ";ALT=${convert(ALT!![0], ALT!![1], ALT!![2], 0u)}"
        }
        if(SRCD != null) str += ";SRCD=${SRCD!!}"
        return str
    }
}

class EGTSSubRecordExtPosData : EGTSSubRecord {
    var VDOP: UShort? = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var HDOP: UShort? = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var PDOP: UShort? = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var SAT: UByte?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var NS: UShort?  = null
        set(value) {
            needRefresh = true
            field =  value
        }

    override fun refreshSRD(){
        var size = 1
        if(VDOP != null) size += 2
        if(HDOP != null) size += 2
        if(PDOP != null) size += 2
        if(SAT != null) size += 1
        if(NS != null) size += 2
        SRD = UByteArray(size)

        SRD!![0] = 0u
        var ind = 1
        if(VDOP != null){
            SRD!![0] = (SRD!![0].toInt() or 0x01).toUByte()
            SRD!![ind] = VDOP!!.toUByte()
            SRD!![ind+1] = (VDOP!!.toInt() ushr 8).toUByte()
            ind += 2
        }
        if(HDOP != null){
            SRD!![0] = (SRD!![0].toInt() or 0x02).toUByte()
            SRD!![ind] = HDOP!!.toUByte()
            SRD!![ind+1] = (HDOP!!.toInt() ushr 8).toUByte()
            ind += 2
        }
        if(PDOP != null){
            SRD!![0] = (SRD!![0].toInt() or 0x04).toUByte()
            SRD!![ind] = PDOP!!.toUByte()
            SRD!![ind+1] = (PDOP!!.toInt() ushr 8).toUByte()
            ind += 2
        }
        if(SAT != null){
            SRD!![0] = (SRD!![0].toInt() or 0x08).toUByte()
            SRD!![ind] = SAT!!
            ind += 1
        }
        if(NS != null){
            SRD!![0] = (SRD!![0].toInt() or 0x10).toUByte()
            SRD!![ind] = NS!!.toUByte()
            SRD!![ind+1] = (NS!!.toInt() ushr 8).toUByte()
            ind += 2
        }
        needRefresh = false
    }

    constructor(vdop: Int? = null,
                hdop: Int? = null,
                pdop: Int? = null,
                sat: Int? = null,
                ns: Int? = null) : super (EGTSSubPacket.EGTS_SR_EXT_POS_DATA, null) {
        VDOP = vdop?.toUShort()
        HDOP = hdop?.toUShort()
        PDOP = pdop?.toUShort()
        SAT = sat?.toUByte()
        NS = ns?.toUShort()
    }

    private constructor(srt: UByte, srd: UByteArray? = null) : super(srt, srd)

    constructor(data: UByteArray, offset: Int = 0) : super(data, offset) {
        if (SRT != EGTSSubPacket.EGTS_SR_EXT_POS_DATA) throw EGTSException("EGTSSubRecordExtPosData parsing failed")
        try {
            var ind = 1
            if((SRD!![0].toInt() and 0x01) != 0 ){
                VDOP = convert(SRD!![ind],SRD!![ind+1])
                ind += 2
            }
            if((SRD!![0].toInt() and 0x02) != 0 ){
                HDOP = convert(SRD!![ind],SRD!![ind+1])
                ind += 2
            }
            if((SRD!![0].toInt() and 0x04) != 0 ){
                PDOP = convert(SRD!![ind],SRD!![ind+1])
                ind += 2
            }
            if((SRD!![0].toInt() and 0x08) != 0 ){
                SAT = SRD!![ind]
                ind += 1
            }
            if((SRD!![0].toInt() and 0x10) != 0 ){
                NS = convert(SRD!![ind],SRD!![ind+1])
            }
        }catch(e: Exception){
            throw EGTSException("EGTSSubRecordExtPosData parsing failed")
        }
    }

    override fun toString(): String {
        var str = "EGTS_SR_EXT_POS_DATA:"

        if(VDOP != null){
            val x = VDOP!!.toDouble()/100.0
            str += " VDOP=${"%.1f".format(x)}"
        }
        if(HDOP != null){
            val x = HDOP!!.toDouble()/100.0
            str += " HDOP=${"%.1f".format(x)}"
        }
        if(PDOP != null){
            val x = PDOP!!.toDouble()/100.0
            str += " PDOP=${"%.1f".format(x)}"
        }
        if(SAT != null)str += " SAT=${SAT}"
        if(NS != null)str += " NS=${NS}"
        return str
    }
}

class EGTSSubRecordADSensorsData : EGTSSubRecord {
    var DOUT: UByte = 0u
        set(value) {
            needRefresh = true
            field =  value
        }
    var ADIO1: UByte?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var ADIO2: UByte?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var ADIO3: UByte?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var ADIO4: UByte?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var ADIO5: UByte?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var ADIO6: UByte?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var ADIO7: UByte?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var ADIO8: UByte?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var ANS1: UInt?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var ANS2: UInt?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var ANS3: UInt?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var ANS4: UInt?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var ANS5: UInt?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var ANS6: UInt?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var ANS7: UInt?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var ANS8: UInt?  = null
        set(value) {
            needRefresh = true
            field =  value
        }

    override fun refreshSRD(){
        var size = 3
        if(ADIO1 != null) size += 1
        if(ADIO2 != null) size += 1
        if(ADIO3 != null) size += 1
        if(ADIO4 != null) size += 1
        if(ADIO5 != null) size += 1
        if(ADIO6 != null) size += 1
        if(ADIO7 != null) size += 1
        if(ADIO8 != null) size += 1
        if(ANS1 != null) size += 3
        if(ANS2 != null) size += 3
        if(ANS3 != null) size += 3
        if(ANS4 != null) size += 3
        if(ANS5 != null) size += 3
        if(ANS6 != null) size += 3
        if(ANS7 != null) size += 3
        if(ANS8 != null) size += 3
        SRD = UByteArray(size)

        SRD!![0] = 0u
        SRD!![1] = DOUT
        SRD!![2] = 0u
        var ind = 3
        if(ADIO1 != null){
            SRD!![0] = (SRD!![0].toInt() or 0x01).toUByte()
            SRD!![ind] = ADIO1!!
            ind += 1
        }
        if(ADIO2 != null){
            SRD!![0] = (SRD!![0].toInt() or 0x02).toUByte()
            SRD!![ind] = ADIO2!!
            ind += 1
        }
        if(ADIO3 != null){
            SRD!![0] = (SRD!![0].toInt() or 0x04).toUByte()
            SRD!![ind] = ADIO3!!
            ind += 1
        }
        if(ADIO4 != null){
            SRD!![0] = (SRD!![0].toInt() or 0x08).toUByte()
            SRD!![ind] = ADIO4!!
            ind += 1
        }
        if(ADIO5 != null){
            SRD!![0] = (SRD!![0].toInt() or 0x10).toUByte()
            SRD!![ind] = ADIO5!!
            ind += 1
        }
        if(ADIO6 != null){
            SRD!![0] = (SRD!![0].toInt() or 0x20).toUByte()
            SRD!![ind] = ADIO6!!
            ind += 1
        }
        if(ADIO7 != null){
            SRD!![0] = (SRD!![0].toInt() or 0x40).toUByte()
            SRD!![ind] = ADIO7!!
            ind += 1
        }
        if(ADIO8 != null){
            SRD!![0] = (SRD!![0].toInt() or 0x80).toUByte()
            SRD!![ind] = ADIO8!!
            ind += 1
        }
        if(ANS1 != null){
            SRD!![2] = (SRD!![2].toInt() or 0x01).toUByte()
            SRD!![ind] = (ANS1!!).toUByte()
            SRD!![ind+1] = (ANS1!! shr 8).toUByte()
            SRD!![ind+2] = (ANS1!! shr 16).toUByte()
            ind += 3
        }
        if(ANS2 != null){
            SRD!![2] = (SRD!![2].toInt() or 0x02).toUByte()
            SRD!![ind] = (ANS2!!).toUByte()
            SRD!![ind+1] = (ANS2!! shr 8).toUByte()
            SRD!![ind+2] = (ANS2!! shr 16).toUByte()
            ind += 3
        }
        if(ANS3 != null){
            SRD!![2] = (SRD!![2].toInt() or 0x04).toUByte()
            SRD!![ind] = (ANS3!!).toUByte()
            SRD!![ind+1] = (ANS3!! shr 8).toUByte()
            SRD!![ind+2] = (ANS3!! shr 16).toUByte()
            ind += 3
        }
        if(ANS4 != null){
            SRD!![2] = (SRD!![2].toInt() or 0x08).toUByte()
            SRD!![ind] = (ANS4!!).toUByte()
            SRD!![ind+1] = (ANS4!! shr 8).toUByte()
            SRD!![ind+2] = (ANS4!! shr 16).toUByte()
            ind += 3
        }
        if(ANS5 != null){
            SRD!![2] = (SRD!![2].toInt() or 0x10).toUByte()
            SRD!![ind] = (ANS5!!).toUByte()
            SRD!![ind+1] = (ANS5!! shr 8).toUByte()
            SRD!![ind+2] = (ANS5!! shr 16).toUByte()
            ind += 3
        }
        if(ANS6 != null){
            SRD!![2] = (SRD!![2].toInt() or 0x20).toUByte()
            SRD!![ind] = (ANS6!!).toUByte()
            SRD!![ind+1] = (ANS6!! shr 8).toUByte()
            SRD!![ind+2] = (ANS6!! shr 16).toUByte()
            ind += 3
        }
        if(ANS7 != null){
            SRD!![2] = (SRD!![2].toInt() or 0x40).toUByte()
            SRD!![ind] = (ANS7!!).toUByte()
            SRD!![ind+1] = (ANS7!! shr 8).toUByte()
            SRD!![ind+2] = (ANS7!! shr 16).toUByte()
            ind += 3
        }
        if(ANS8 != null){
            SRD!![2] = (SRD!![2].toInt() or 0x80).toUByte()
            SRD!![ind] = (ANS8!!).toUByte()
            SRD!![ind+1] = (ANS8!! shr 8).toUByte()
            SRD!![ind+2] = (ANS8!! shr 16).toUByte()
            ind += 3
        }
        needRefresh = false
    }

    constructor(dout: UByte,
                aduio1: UByte? = null,aduio2: UByte? = null,aduio3: UByte? = null,aduio4: UByte? = null,
                aduio5: UByte? = null,aduio6: UByte? = null,aduio7: UByte? = null,aduio8: UByte? = null,
                ans1: UInt? = null,ans2: UInt? = null,ans3: UInt? = null,ans4: UInt? = null,
                ans5: UInt? = null,ans6: UInt? = null,ans7: UInt? = null,ans8: UInt? = null
     ) : super (EGTSSubPacket.EGTS_SR_AD_SENSORS_DATA, null) {
        DOUT = dout
        ADIO1 = aduio1
        ADIO2 = aduio2
        ADIO3 = aduio3
        ADIO4 = aduio4
        ADIO5 = aduio5
        ADIO6 = aduio6
        ADIO7 = aduio7
        ADIO8 = aduio8
        ANS1 = ans1
        ANS2 = ans2
        ANS3 = ans3
        ANS4 = ans4
        ANS5 = ans5
        ANS6 = ans6
        ANS7 = ans7
        ANS8 = ans8
    }

    private constructor(srt: UByte, srd: UByteArray? = null) : super(srt, srd)

    constructor(data: UByteArray, offset: Int = 0) : super(data, offset) {
        if (SRT != EGTSSubPacket.EGTS_SR_AD_SENSORS_DATA) throw EGTSException("EGTSSubRecordADSensorsData parsing failed")
        try {
            var ind = 3
            DOUT = SRD!![1]
            if((SRD!![0].toInt() and 0x01) != 0 ){
                ADIO1 = SRD!![ind]
                ind += 1
            }
            if((SRD!![0].toInt() and 0x02) != 0 ){
                ADIO2 = SRD!![ind]
                ind += 1
            }
            if((SRD!![0].toInt() and 0x04) != 0 ){
                ADIO3 = SRD!![ind]
                ind += 1
            }
            if((SRD!![0].toInt() and 0x08) != 0 ){
                ADIO4 = SRD!![ind]
                ind += 1
            }
            if((SRD!![0].toInt() and 0x10) != 0 ){
                ADIO5 = SRD!![ind]
                ind += 1
            }
            if((SRD!![0].toInt() and 0x20) != 0 ){
                ADIO6 = SRD!![ind]
                ind += 1
            }
            if((SRD!![0].toInt() and 0x40) != 0 ){
                ADIO7 = SRD!![ind]
                ind += 1
            }
            if((SRD!![0].toInt() and 0x80) != 0 ){
                ADIO8 = SRD!![ind]
                ind += 1
            }
            if((SRD!![2].toInt() and 0x01) != 0 ){
                ANS1 = convert(SRD!![ind],SRD!![ind+1],SRD!![ind+2],0u)
                ind += 3
            }
            if((SRD!![2].toInt() and 0x02) != 0 ){
                ANS2 = convert(SRD!![ind],SRD!![ind+1],SRD!![ind+2],0u)
                ind += 3
            }
            if((SRD!![2].toInt() and 0x04) != 0 ){
                ANS3 = convert(SRD!![ind],SRD!![ind+1],SRD!![ind+2],0u)
                ind += 3
            }
            if((SRD!![2].toInt() and 0x08) != 0 ){
                ANS4 = convert(SRD!![ind],SRD!![ind+1],SRD!![ind+2],0u)
                ind += 3
            }
            if((SRD!![2].toInt() and 0x10) != 0 ){
                ANS5 = convert(SRD!![ind],SRD!![ind+1],SRD!![ind+2],0u)
                ind += 3
            }
            if((SRD!![2].toInt() and 0x20) != 0 ){
                ANS6 = convert(SRD!![ind],SRD!![ind+1],SRD!![ind+2],0u)
                ind += 3
            }
            if((SRD!![2].toInt() and 0x40) != 0 ){
                ANS7 = convert(SRD!![ind],SRD!![ind+1],SRD!![ind+2],0u)
                ind += 3
            }
            if((SRD!![2].toInt() and 0x80) != 0 ){
                ANS8 = convert(SRD!![ind],SRD!![ind+1],SRD!![ind+2],0u)
            }
        }catch(e: Exception){
            throw EGTSException("EGTSSubRecordADSensorsData parsing failed")
        }
    }

    override fun toString(): String {
        var str = "EGTS_SR_AD_SENSORS_DATA: ${DOUT.toString(2).padStart(8,'0')}"

        if(ADIO1 != null){
            str += ",ADIO1=${ADIO1!!.toString(2).padStart(8,'0')}"
        }
        if(ADIO2 != null){
            str += ",ADIO2=${ADIO2!!.toString(2).padStart(8,'0')}"
        }
        if(ADIO3 != null){
            str += ",ADIO3=${ADIO3!!.toString(2).padStart(8,'0')}"
        }
        if(ADIO4 != null){
            str += ",ADIO4=${ADIO4!!.toString(2).padStart(8,'0')}"
        }
        if(ADIO5 != null){
            str += ",ADIO5=${ADIO5!!.toString(2).padStart(8,'0')}"
        }
        if(ADIO6 != null){
            str += ",ADIO6=${ADIO6!!.toString(2).padStart(8,'0')}"
        }
        if(ADIO7 != null){
            str += ",ADIO7=${ADIO7!!.toString(2).padStart(8,'0')}"
        }
        if(ADIO8 != null){
            str += ",ADIO8=${ADIO8!!.toString(2).padStart(8,'0')}"
        }
        if(ANS1 != null){
            str += ",ANS1=${ANS1!!.toString()}"
        }
        if(ANS2 != null){
            str += ",ANS2=${ANS2!!.toString()}"
        }
        if(ANS3 != null){
            str += ",ANS3=${ANS3!!.toString()}"
        }
        if(ANS4 != null){
            str += ",ANS4=${ANS4!!.toString()}"
        }
        if(ANS5 != null){
            str += ",ANS5=${ANS5!!.toString()}"
        }
        if(ANS6 != null){
            str += ",ANS6=${ANS6!!.toString()}"
        }
        if(ANS7 != null){
            str += ",ANS7=${ANS7!!.toString()}"
        }
        if(ANS8 != null){
            str += ",ANS8=${ANS8!!.toString()}"
        }
        return str
    }
}

class EGTSSubRecordCountersData : EGTSSubRecord {
    var CN1: UInt?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var CN2: UInt?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var CN3: UInt?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var CN4: UInt?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var CN5: UInt?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var CN6: UInt?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var CN7: UInt?  = null
        set(value) {
            needRefresh = true
            field =  value
        }
    var CN8: UInt?  = null
        set(value) {
            needRefresh = true
            field =  value
        }

    override fun refreshSRD(){
        var size = 1
        if(CN1 != null) size += 3
        if(CN2 != null) size += 3
        if(CN3 != null) size += 3
        if(CN4 != null) size += 3
        if(CN5 != null) size += 3
        if(CN6 != null) size += 3
        if(CN7 != null) size += 3
        if(CN8 != null) size += 3
        SRD = UByteArray(size)

        SRD!![0] = 0u
        var ind = 1
        if(CN1 != null){
            SRD!![0] = (SRD!![0].toInt() or 0x01).toUByte()
            SRD!![ind] = (CN1!!).toUByte()
            SRD!![ind+1] = (CN1!! shr 8).toUByte()
            SRD!![ind+2] = (CN1!! shr 16).toUByte()
            ind += 3
        }
        if(CN2 != null){
            SRD!![0] = (SRD!![0].toInt() or 0x02).toUByte()
            SRD!![ind] = (CN2!!).toUByte()
            SRD!![ind+1] = (CN2!! shr 8).toUByte()
            SRD!![ind+2] = (CN2!! shr 16).toUByte()
            ind += 3
        }
        if(CN3 != null){
            SRD!![0] = (SRD!![0].toInt() or 0x04).toUByte()
            SRD!![ind] = (CN3!!).toUByte()
            SRD!![ind+1] = (CN3!! shr 8).toUByte()
            SRD!![ind+2] = (CN3!! shr 16).toUByte()
            ind += 3
        }
        if(CN4 != null){
            SRD!![0] = (SRD!![0].toInt() or 0x08).toUByte()
            SRD!![ind] = (CN4!!).toUByte()
            SRD!![ind+1] = (CN4!! shr 8).toUByte()
            SRD!![ind+2] = (CN4!! shr 16).toUByte()
            ind += 3
        }
        if(CN5 != null){
            SRD!![0] = (SRD!![0].toInt() or 0x10).toUByte()
            SRD!![ind] = (CN5!!).toUByte()
            SRD!![ind+1] = (CN5!! shr 8).toUByte()
            SRD!![ind+2] = (CN5!! shr 16).toUByte()
            ind += 3
        }
        if(CN6 != null){
            SRD!![0] = (SRD!![0].toInt() or 0x20).toUByte()
            SRD!![ind] = (CN6!!).toUByte()
            SRD!![ind+1] = (CN6!! shr 8).toUByte()
            SRD!![ind+2] = (CN6!! shr 16).toUByte()
            ind += 3
        }
        if(CN7 != null){
            SRD!![0] = (SRD!![0].toInt() or 0x40).toUByte()
            SRD!![ind] = (CN7!!).toUByte()
            SRD!![ind+1] = (CN7!! shr 8).toUByte()
            SRD!![ind+2] = (CN7!! shr 16).toUByte()
            ind += 3
        }
        if(CN8 != null){
            SRD!![0] = (SRD!![0].toInt() or 0x80).toUByte()
            SRD!![ind] = (CN8!!).toUByte()
            SRD!![ind+1] = (CN8!! shr 8).toUByte()
            SRD!![ind+2] = (CN8!! shr 16).toUByte()
            ind += 3
        }
        needRefresh = false
    }

    constructor(cn1: UInt? = null,cn2: UInt? = null,cn3: UInt? = null,cn4: UInt? = null,
                cn5: UInt? = null,cn6: UInt? = null,cn7: UInt? = null,cn8: UInt? = null
    ) : super (EGTSSubPacket.EGTS_SR_COUNTERS_DATA, null) {
        CN1 = cn1
        CN2 = cn2
        CN3 = cn3
        CN4 = cn4
        CN5 = cn5
        CN6 = cn6
        CN7 = cn7
        CN8 = cn8
    }

    private constructor(srt: UByte, srd: UByteArray? = null) : super(srt, srd)

    constructor(data: UByteArray, offset: Int = 0) : super(data, offset) {
        if (SRT != EGTSSubPacket.EGTS_SR_COUNTERS_DATA) throw EGTSException("EGTSSubRecordCountersData parsing failed")
        try {
            var ind = 1
            if((SRD!![0].toInt() and 0x01) != 0 ){
                CN1 = convert(SRD!![ind],SRD!![ind+1],SRD!![ind+2],0u)
                ind += 3
            }
            if((SRD!![0].toInt() and 0x02) != 0 ){
                CN2 = convert(SRD!![ind],SRD!![ind+1],SRD!![ind+2],0u)
                ind += 3
            }
            if((SRD!![0].toInt() and 0x04) != 0 ){
                CN3 = convert(SRD!![ind],SRD!![ind+1],SRD!![ind+2],0u)
                ind += 3
            }
            if((SRD!![0].toInt() and 0x08) != 0 ){
                CN4 = convert(SRD!![ind],SRD!![ind+1],SRD!![ind+2],0u)
                ind += 3
            }
            if((SRD!![0].toInt() and 0x10) != 0 ){
                CN5 = convert(SRD!![ind],SRD!![ind+1],SRD!![ind+2],0u)
                ind += 3
            }
            if((SRD!![0].toInt() and 0x20) != 0 ){
                CN6 = convert(SRD!![ind],SRD!![ind+1],SRD!![ind+2],0u)
                ind += 3
            }
            if((SRD!![0].toInt() and 0x40) != 0 ){
                CN7 = convert(SRD!![ind],SRD!![ind+1],SRD!![ind+2],0u)
                ind += 3
            }
            if((SRD!![0].toInt() and 0x80) != 0 ){
                CN8 = convert(SRD!![ind],SRD!![ind+1],SRD!![ind+2],0u)
            }
        }catch(e: Exception){
            throw EGTSException("EGTSSubRecordCountersData parsing failed")
        }
    }

    override fun toString(): String {
        var str = "EGTS_SR_COUNTERS_DATA:"

        if(CN1 != null){
            str += " CN1=${CN1!!.toString()}"
        }
        if(CN2 != null){
            str += " CN2=${CN2!!.toString()}"
        }
        if(CN3 != null){
            str += " CN3=${CN3!!.toString()}"
        }
        if(CN4 != null){
            str += " CN4=${CN4!!.toString()}"
        }
        if(CN5 != null){
            str += " CN5=${CN5!!.toString()}"
        }
        if(CN6 != null){
            str += " CN6=${CN6!!.toString()}"
        }
        if(CN7 != null){
            str += " CN7=${CN7!!.toString()}"
        }
        if(CN8 != null){
            str += " CN8=${CN8!!.toString()}"
        }
        return str
    }
}

class EGTSSubRecordLiquidLevelSensorData : EGTSSubRecord {
    var FLAG: UByte = 0u
        set(value) {
            needRefresh = true
            field =  value
        }
    var MADDR: UShort = 0u
        set(value) {
            needRefresh = true
            field =  value
        }
    var LLSD: UInt = 0u
        set(value) {
            needRefresh = true
            field =  value
        }

    override fun refreshSRD(){
        var size = 7
        SRD = UByteArray(size)

        SRD!![0] = FLAG
        SRD!![1] = MADDR!!.toUByte()
        SRD!![2] = (MADDR!!.toInt() ushr 8).toUByte()
        SRD!![3] = LLSD!!.toUByte()
        SRD!![4] = (LLSD!!.toInt() ushr 8).toUByte()
        SRD!![5] = (LLSD!!.toInt() ushr 16).toUByte()
        SRD!![6] = (LLSD!!.toInt() ushr 24).toUByte()
        needRefresh = false
    }

    constructor(flag: UByte, maddr: UShort, llsd: UInt) : super (EGTSSubPacket.EGTS_SR_LIQUID_LEVEL_SENSOR, null) {
        FLAG = flag
        MADDR = maddr
        LLSD = llsd
    }

    private constructor(srt: UByte, srd: UByteArray? = null) : super(srt, srd)

    constructor(data: UByteArray, offset: Int = 0) : super(data, offset) {
        if (SRT != EGTSSubPacket.EGTS_SR_LIQUID_LEVEL_SENSOR) throw EGTSException("EGTSSubRecordLiquidLevelSensorData parsing failed")
        try {
            FLAG = SRD!![0]
            MADDR = convert(SRD!![1],SRD!![2])
            LLSD = convert(SRD!![3],SRD!![4],SRD!![5],SRD!![6])
        }catch(e: Exception){
            throw EGTSException("EGTSSubRecordLiquidLevelSensorData parsing failed")
        }
    }

    override fun toString(): String {
        var str = "EGTS_SR_LIQUID_LEVEL_SENSOR(${FLAG.toString(2).padStart(8,'0')}):$MADDR->$LLSD"
        return str
    }
}

class EGTSSubRecordResponse : EGTSSubRecord {
    var CRN: UShort = 0u
        set(value) {
            needRefresh = true
            field =  value
        }
    var RST: UByte = 0u
        private set(value) {
            needRefresh = true
            field =  value
        }

    override fun refreshSRD(){
        SRD = UByteArray(3)

        SRD!![0] = CRN.toUByte()
        SRD!![1] = (CRN.toInt() ushr 8).toUByte()
        SRD!![2] = RST

        needRefresh = false
    }

    constructor(crn: UShort, rst: UByte = 0u) : super (EGTSSubPacket.EGTS_SR_RECORD_RESPONSE, null) {
        CRN = crn
        RST = rst
    }

    private constructor(srt: UByte, srd: UByteArray? = null) : super(srt, srd)

    constructor(data: UByteArray, offset: Int = 0) : super(data, offset) {
        if (SRT != EGTSSubPacket.EGTS_SR_RECORD_RESPONSE) throw EGTSException("EGTSSubRecordResponse parsing failed")
        try {
            CRN = convert(SRD!![0], SRD!![1])
            RST = SRD!![2]
        }catch(e: Exception){
            throw EGTSException("EGTSSubRecordResponse parsing failed")
        }
   }

    override fun toString(): String {
        var str = "EGTS_SR_RECORD_RESPONSE: CRN=${CRN},RST=${RST}"
        return str
    }
}

class EGTSSubRecordTermIdentity : EGTSSubRecord {
    var TID: UInt =0u
        set(value) {
            needRefresh = true
            field =  value
        }
    var FLAGS: UByte =0x10u
        private set(value) {
            needRefresh = true
            field =  value
        }
    var HDID: UShort? = null
        set(value) {
            needRefresh = true
            if(value == null) FLAGS = FLAGS and 0xfeu
            else FLAGS = FLAGS or 0x01u
            field =  value
        }
    var IMEI: String = ""
        set(value) {
            needRefresh = true
            if(value == "") FLAGS = FLAGS and 0xfdu
            else FLAGS = FLAGS or 0x02u
            field =  value
        }
    var IMSI: String = ""
        set(value) {
            needRefresh = true
            if(value == "") FLAGS = FLAGS and 0xfbu
            else FLAGS = FLAGS or 0x04u
            field =  value
        }
    var LNGC: String = ""
        set(value) {
            needRefresh = true
            if(value == "") FLAGS = FLAGS and 0xf7u
            else FLAGS = FLAGS or 0x08u
            field =  value
        }
    var NID: UByteArray? = null
        set(value) {
            needRefresh = true
            if(value == null) FLAGS = FLAGS and 0xdfu
            else FLAGS = FLAGS or 0x20u
            field =  value
        }
    var BS: UShort? = null
        set(value) {
            needRefresh = true
            if(value == null) FLAGS = FLAGS and 0xbfu
            else FLAGS = FLAGS or 0x40u
            field =  value
        }
    var MSISDN: String = ""
        set(value) {
            needRefresh = true
            if(value == "") FLAGS = FLAGS and 0x7fu
            else FLAGS = FLAGS or 0x80u
            field =  value
        }

    override fun refreshSRD(){
        var size = 5
        if(HDID != null) size += 2
        if(IMEI != "") size += 15
        if(IMSI != "") size += 16
        if(LNGC != "") size += 3
        if(NID != null) size += 3
        if(BS != null) size += 2
        if(MSISDN != "") size += 15
        SRD = UByteArray(size)

        SRD!![0] = TID.toUByte()
        SRD!![1] = (TID.toLong() ushr 8).toUByte()
        SRD!![2] = (TID.toLong() ushr 16).toUByte()
        SRD!![3] = (TID.toLong() ushr 24).toUByte()
        SRD!![4] = FLAGS
        var ind = 5

        if(HDID != null){
            SRD!![ind] = HDID!!.toUByte()
            SRD!![ind+1] = (HDID!!.toInt() ushr 8).toUByte()
            ind += 2
        }
        if(IMEI != ""){
            IMEI.toByteArray().toUByteArray().copyInto(SRD!!,ind)
            ind += 15
        }
        if(IMSI != ""){
            IMSI.toByteArray().toUByteArray().copyInto(SRD!!,ind)
            ind += 16
        }
        if(LNGC != ""){
            LNGC.toByteArray().toUByteArray().copyInto(SRD!!,ind)
            ind += 3
        }
        if(NID != null){
            NID!!.copyInto(SRD!!,ind)
            ind += 3
        }
        if(BS != null){
            SRD!![ind] = BS!!.toUByte()
            SRD!![ind+1] = (BS!!.toInt() ushr 8).toUByte()
            ind += 2
        }
        if(MSISDN != ""){
            MSISDN.toByteArray(Charsets.ISO_8859_1).toUByteArray().copyInto(SRD!!)
            ind += 15
        }

        needRefresh = false
    }

    constructor(tid: UInt, ssra: Boolean = true,
                hdid: UShort? = null,
                imei: String = "",
                imsi: String = "",
                lngc: String = "",
                nid: UByteArray? = null,
                bs: UShort? = null,
                msudn: String = "") : super (EGTSSubPacket.EGTS_SR_TERM_IDENTITY, null) {
        TID = tid
        FLAGS = if(ssra) 0x10u else 0x0u
        HDID = hdid
        IMEI = imei
        IMSI = imsi
        LNGC = lngc
        NID = nid
        BS = bs
        MSISDN = msudn
    }

    private constructor(srt: UByte, srd: UByteArray? = null) : super(srt, srd)

    constructor(data: UByteArray, offset: Int = 0) : super(data, offset) {
        if (SRT != EGTSSubPacket.EGTS_SR_TERM_IDENTITY) throw EGTSException("EGTSSubRecordTermIdentity parsing failed")
        try {
            TID = convert(SRD!![0],SRD!![1],SRD!![2],SRD!![3])
            FLAGS = SRD!![4]
            var ind = 5
            if((FLAGS.toInt() and 0x01) != 0 ){
                HDID = convert(data[ind],data[ind+1])
                ind += 2
            }
            if((FLAGS.toInt() and 0x02) != 0 ){
                IMEI = SRD!!.toByteArray().decodeToString(ind,ind+15)
                ind += 15
            }
            if((FLAGS.toInt() and 0x04) != 0 ){
                IMSI = SRD!!.toByteArray().decodeToString(ind,ind+16)
                ind += 16
            }
            if((FLAGS.toInt() and 0x08) != 0 ){
                LNGC = SRD!!.toByteArray().decodeToString(ind,ind+3)
                ind += 3
            }
            if((FLAGS.toInt() and 0x20) != 0 ){
                NID = SRD!!.copyOfRange(ind,ind+3)
                ind += 3
            }
            if((FLAGS.toInt() and 0x40) != 0 ){
                BS = convert(data[ind],data[ind+1])
                ind += 2
            }
            if((FLAGS.toInt() and 0x80) != 0 ){
                MSISDN = SRD!!.toByteArray().decodeToString(ind,ind+15)
                ind += 15
            }
        }catch(e: Exception){
            throw EGTSException("EGTSSubRecordTermIdentity parsing failed")
        }
    }

    override fun toString(): String {
        var str = "EGTS_SR_TERM_IDENTITY(${FLAGS.toString(2).padStart(8,'0')}): TID=${TID}"
        if(HDID != null) str += ",HDID=${HDID}"
        if(IMEI != "") str += ",IMEI=${IMEI}"
        if(IMSI != "") str += ",IMSI=${IMSI}"
        if((FLAGS.toInt() and 0x10) != 0 ) str += ",SSRA"
        if(NID != null) str += ",NID=${NID?.toHexString()}"
        if(BS != null) str += ",BS=${BS}"
        if(MSISDN != "") str += ",MSISDN=${MSISDN}"
        return str
    }
}

class EGTSSubRecordResultCode : EGTSSubRecord {
    var RCD: UByte = 0u
        private set(value) {
            needRefresh = true
            field =  value
        }

    override fun refreshSRD(){
        SRD = UByteArray(1)

        SRD!![0] = RCD

        needRefresh = false
    }

    constructor(rcd: UByte) : super (EGTSSubPacket.EGTS_SR_RESULT_CODE, null) {
        RCD = rcd
    }

    private constructor(srt: UByte, srd: UByteArray? = null) : super(srt, srd)

    constructor(data: UByteArray, offset: Int = 0) : super(data, offset) {
        if (SRT != EGTSSubPacket.EGTS_SR_RESULT_CODE) throw EGTSException("EGTSSubRecordResultCode parsing failed")
        try {
            RCD = SRD!![0]
        }catch(e: Exception){
            throw EGTSException("EGTSSubRecordResultCode parsing failed")
        }
    }

    override fun toString(): String {
        var str = "EGTS_SR_RESULT_CODE: RCD=${RCD}"
        return str
    }
}
