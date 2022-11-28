package ru.glorient.bkn.egts

import kotlinx.coroutines.*

/**
 * Класс простой отправки записи
 *
 * Отправляется [mRec], ожидается ответ транспорного уровня о доставки в течении 3 сек,
 * затем ожидается сообщение о подверждении зариси в течении 1 сек.
 * Задача осуществляется в отдельной непрерываемой корутине, результат в [onEnd].
 * Можно запускать несколько параллельно.
 *
 * @property  mRec Передаваемая запись
 * @property  mParent Транспорный уровень
 * @property  onEnd Событие на окончание, где res флаг успешности выполнения, msg подробное сообщение о выполнении, data null.
 * @property  mLazy Флаг отложенного запуска
 * @constructor Запускает корутину [mCommandJob] если [mLazy] false
 */
@ExperimentalUnsignedTypes
class EGTSCommand(
    private val mRec: EGTSRecord,
    mParent: EGTSTransportLevel,
    onEnd: (res: Boolean, msg: String, data: Any?) -> Unit,
    mLazy: Boolean = false
) : EGTSCommandJob(mParent,onEnd, mLazy){
    /**
     * Корутина задачи
     */
    override val mCommandJob: Job = GlobalScope.launch(start = CoroutineStart.LAZY){
        mParent.addSubsriber(this@EGTSCommand)
        mRec.RN = mParent.getRN()
        if(send(3000, listOf(mRec)))
        {
            val result = withTimeoutOrNull(1000){
                loop@ while (true){
                    val msg = mChannel.receive()
                    if(msg is EGTSRecord){
                        for(x in msg.RD){
                            if(x is EGTSSubRecordResponse){
                                if(x.CRN == mRec.RN){
                                    mResult = true
                                    if(x.RST.toInt() == 0)onEnd(true,"success", mRec)
                                    else onEnd(false,"RST=${x.RST}", mRec)
                                    break@loop
                                }
                            }
                        }
                    }
                }
            }
            if(result == null)onEnd(false,"timeout2", mRec)
        }else{
            onEnd(false,"timeout1", mRec)
        }

        mParent.removeSubsriber(this@EGTSCommand)
    }

    /**
     * Запуск mCommandJob
     */
    init{
        if(!mLazy)mCommandJob.start()
    }
}