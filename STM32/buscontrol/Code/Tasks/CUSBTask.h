/*!
	\file
	\brief Класс для реализации передачи и приема данных по USB.
	\authors Близнец Р.А.
	\version 1.0.0.0
	\date 29.10.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/

	Один объект на приложение.
*/

#if !defined CUSBTASK_H
#define CUSBTASK_H

/*!
    \defgroup usb_task Задача для USB
    \ingroup Tasks
    \brief Для для реализации передачи и приема данных по USB.

    Очередь данных на передачу и байт шафтинг, обработка принятых данных.
    	@{
*/

#include "settings.h"

#include "../Algorithms/Task/CBaseTask.h"
#include "../Algorithms/CRC/CCRC16.h"

#include "../data.h"

#define USBTASK_RX_BUF_BIT 			(27)						///< Номер бита уведомления о недоствточном размере буфера приема.
#define USBTASK_RX_BUF_FLAG 		(1 << USBTASK_RX_BUF_BIT)	///< Флаг уведомления  о недоствточном размере буфера приема.
#define USBTASK_RX_OVER_BIT 		(28)						///< Номер бита уведомления об ошибке приема.
#define USBTASK_RX_OVER_FLAG 		(1 << USBTASK_RX_OVER_BIT)	///< Флаг уведомления  об ошибке приема.
#define USBTASK_RX_BIT 				(29)						///< Номер бита уведомления о приеме.
#define USBTASK_RX_FLAG 			(1 << USBTASK_RX_BIT)		///< Флаг уведомления о приеме.
#define USBTASK_TXEND_BIT 			(30)						///< Номер бита уведомления об окончании передачи.
#define USBTASK_TXEND_FLAG 			(1 << USBTASK_TXEND_BIT)	///< Флаг уведомления об окончании передачи.
#define USBTASK_QUEUE_BIT 			(31)						///< Номер бита уведомления о сообщении в очереди.
#define USBTASK_QUEUE_FLAG 			(1 << USBTASK_QUEUE_BIT)	///< Флаг уведомления о сообщении в очереди.


/// Класс для реализации передачи и приема данных по USB.
class CUSBTask : public CBaseTask
{
private:
	bool mFirst=true;			///< Флаг отсутствия подключения.

	bool mOverFalg=false;				///< Флаг пропущенного пакета.
	uint8_t mRxBuffer[USB_BUF_SIZE];	///< Приемный буфер пакета.
	uint32_t mRxBufferSize=0;			///< Размер данных  в приемном буфере.
	uint8_t mTxBuffer[USB_BUF_SIZE];	///< Буфер на передачу.
	bool mWaitTx=false;					///< Флаг ожидания окончания передачи.

	uint8_t mCmdBuffer[USB_BUF_SIZE];	///< Буфер FIFO на прием.
	uint16_t mCurrentIndexRx=0;			///< Текущий индекс буфера FIFO на прием.
	uint32_t mSizeRx;					///< Размер принимаемого пакета.

protected:
	CCRC16 mCRC;	///< Расчет CRC-16.

	/// Функция задачи.
	virtual void Run() override;

	/// Обработка сообщения из очереди.
	/*!
	  \param[in] msg Сообщение.
	*/
	void DoMessage(STaskMessage msg);

	/// Обработка сообщения из приемного буфера.
	void ProccessRx();
	/// Формирование и передача пакета.
	/*!
	  \param[in] data данные.
	  \param[in] size размер данных.
	  \return Размер передавакмых данных
	*/
	uint32_t ProccessTx(uint8_t* data, uint32_t size);

public:
	/// Едиственный экземпляр класса.
	/*!
	  \return Указатель на CUSBTask
	*/
	static CUSBTask* Instance()
	{
		static CUSBTask theSingleInstance;
		return &theSingleInstance;
	}

	/// Послать сообщение в задачу.
	/*!
	  \param[in] msg Указатель на сообщение.
	  \param[in] xTicksToWait Время ожидания в тиках.
	  \param[in] free вернуть память в кучу в случае неудачи.
	  \return true в случае успеха.
	*/
	inline bool SendMessage(STaskMessage* msg,TickType_t xTicksToWait=0, bool free=false)
	{
		return CBaseTask::SendMessage(msg,USBTASK_QUEUE_FLAG,xTicksToWait,free);
	};

	/// Послать сообщение в задачу из прерывания.
	/*!
	  \param[in] msg Указатель на сообщение.
	  \param[out] pxHigherPriorityTaskWoken Флаг переключения задач.
	  \return true в случае успеха.
	*/
	inline bool SendMessageFromISR(STaskMessage* msg,BaseType_t *pxHigherPriorityTaskWoken)
	{
		return CBaseTask::SendMessageFromISR(msg,pxHigherPriorityTaskWoken,USBTASK_QUEUE_FLAG);
	};

	/// Функция окончания передачи.
	/*!
	  Вызывается из прерывания (CDC_TransmitCplt_FS в usbd_cdc_if.c)
	*/
	static void endTransmit();

	/// Функция окончания приема.
	/*!
	  Вызывается из прерывания (CDC_Receive_FS в usbd_cdc_if.c)

	  \param[in] data данные.
	  \param[in] size размер данных.
	*/
	static void endRecieve(uint8_t* data, uint32_t size);

	/// Manage the CDC class requests.
	/*!
	  Вызывается из прерывания (CDC_Control_FS в usbd_cdc_if.c)

	  \param[in] cmd Command code.
	  \param[in] pbuf Buffer containing command data (request parameters).
	  \param[in] length Number of data to be sent (in bytes).
	*/
	static void control(uint8_t cmd, uint8_t* pbuf, uint16_t length);
};
/*! @} */

#endif // CUSBTASK_H

