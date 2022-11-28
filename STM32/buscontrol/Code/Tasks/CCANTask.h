/*!
	\file
	\brief Класс для реализации приема/передачи CAN.
	\authors Близнец Р.А.
	\version 0.0.0.1
	\date 24.12.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/

	Один объект на приложение.
*/

#if !defined CCANTASK_H
#define CLOGICTASK_H


#include "settings.h"

#include "../Algorithms/Task/CBaseTask.h"
#include "../Devices/CCAN.h"
#include "../data.h"

#define CANTASK_RX_BIT 				(30)
#define CANTASK_RXC_FLAG 				(1 << CANTASK_RX_BIT)
#define CANTASK_QUEUE_BIT 			(31)						///< Номер бита уведомления о сообщении в очереди.
#define CANTASK_QUEUE_FLAG 			(1 << CANTASK_QUEUE_BIT)	///< Флаг уведомления о сообщении в очереди.


/// Класс для реализации приема/передачи CAN.
class CCANTask : public CBaseTask
{
private:

protected:

	/// Функция задачи.
	virtual void Run() override;

public:
	/// Едиственный экземпляр класса.
	/*!
	  \return Указатель на CCANTask
	*/
	static CCANTask* Instance()
	{
		static CCANTask theSingleInstance;
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
		return CBaseTask::SendMessage(msg,CANTASK_QUEUE_FLAG,xTicksToWait,free);
	};

	/// Послать сообщение в задачу из прерывания.
	/*!
	  \param[in] msg Указатель на сообщение.
	  \param[out] pxHigherPriorityTaskWoken Флаг переключения задач.
	  \return true в случае успеха.
	*/
	inline bool SendMessageFromISR(STaskMessage* msg,BaseType_t *pxHigherPriorityTaskWoken)
	{
		return CBaseTask::SendMessageFromISR(msg,pxHigherPriorityTaskWoken,CANTASK_QUEUE_FLAG);
	};
};

#endif // CCANTASK_H

