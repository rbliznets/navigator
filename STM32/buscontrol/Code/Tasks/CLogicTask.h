/*!
	\file
	\brief Класс для реализации основной задачи.
	\authors Близнец Р.А.
	\version 0.0.0.1
	\date 28.10.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/

	Один объект на приложение.
*/

#if !defined CLOGICTASK_H
#define CLOGICTASK_H

/*!
    \defgroup logic_task Основная задача
    \ingroup Tasks
    \brief Для индикации и коммуникации между задачами.

    Обработка команд от базового модуля, коммутация между задачами, светодиодная индикация.
    	@{
*/

#include "settings.h"

#include "../Algorithms/Task/CBaseTask.h"
#include "../Algorithms/DataFlow/CJsonParser.h"

#include "../Algorithms/Task/CSoftwareTimer.h"
#include "../Devices/CExternalPins.h"
#include "../data.h"

//#define LOGICTASK_START_BIT 			(20)						///< Номер бита уведомления об окончании определения режима работы.
//#define LOGICTASK_START_FLAG 			(1 << LOGICTASK_START_BIT)	///< Флаг уведомления об окончании определения режима работы.
#define LOGICTASK_ADC_BIT 				(30)						///< Номер бита уведомления об окончании считывания значений с внешних выводов.
#define LOGICTASK_ADC_FLAG 				(1 << LOGICTASK_ADC_BIT)	///< Флаг уведомления об окончании считывания значений с внешних выводов.
#define LOGICTASK_QUEUE_BIT 			(31)						///< Номер бита уведомления о сообщении в очереди.
#define LOGICTASK_QUEUE_FLAG 			(1 << LOGICTASK_QUEUE_BIT)	///< Флаг уведомления о сообщении в очереди.


/// Класс для реализации задачи FreeRTOS основной логики работы.
class CLogicTask : public CBaseTask
{
private:

protected:
	CSoftwareTimer mTm;
	CJsonParser mParser;

	/// Функция задачи.
	virtual void Run() override;

	/// Обработка сообщения из очереди.
	/*!
	  \param[in] msg Сообщение.
	*/
	void DoMessage(STaskMessage msg);
	void doLeds(CJsonParser* command);

public:
	/// Едиственный экземпляр класса.
	/*!
	  \return Указатель на CLogicTask
	*/
	static CLogicTask* Instance()
	{
		static CLogicTask theSingleInstance;
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
		return CBaseTask::SendMessage(msg,LOGICTASK_QUEUE_FLAG,xTicksToWait,free);
	};

	/// Послать сообщение в задачу из прерывания.
	/*!
	  \param[in] msg Указатель на сообщение.
	  \param[out] pxHigherPriorityTaskWoken Флаг переключения задач.
	  \return true в случае успеха.
	*/
	inline bool SendMessageFromISR(STaskMessage* msg,BaseType_t *pxHigherPriorityTaskWoken)
	{
		return CBaseTask::SendMessageFromISR(msg,pxHigherPriorityTaskWoken,LOGICTASK_QUEUE_FLAG);
	};
};
/*! @} */

#endif // CLOGICTASK_H

