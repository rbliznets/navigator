/*!
	\file
	\brief Базовый класс для реализации задачи FreeRTOS.
	\authors Близнец Р.А.
	\version 1.0.0.0
	\date 28.10.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/
*/

#if !defined CBASETASK_H
#define CBASETASK_H

/*! \defgroup freertos FreeRTOS
  	\ingroup Algorithms
  	\brief Алгоритмы, связанные с FreeRTOS.
*/

/*!
    \defgroup f_tasks Задачи
  	\ingroup freertos
    \brief Данный модуль предназначен для кода задач FreeRTOS.

   	@{
*/

#include "FreeRTOS.h"
#include "task.h"
#include "queue.h"

/// Структура сообщения между задачами.
struct STaskMessage
{
	uint16_t msgID;			///< Тип сообщения.
	uint16_t shortParam;	///< Параметр команды.
	union
	{
		struct
		{
			uint16_t param1;	///< Параметр сообщения.
			uint16_t param2;	///< Параметр сообщения.
		};
		uint32_t paramID;	///< Параметр сообщения.
		void* msgBody;		///< Указатель на тело сообщение.
	};
};

/// Базовый абстрактный класс для реализации задачи FreeRTOS.
class CBaseTask
{
protected:
	bool mInit=false; 		  ///< Флаг инициализации.
	TaskHandle_t mTaskHandle; ///< Хэндлер задачи FreeRTOS.
	QueueHandle_t mTaskQueue; ///< Приемная очередь сообщений.

	/// Функция задачи FreeRTOS.
	/*!
	  \param[in] pvParameters Параметр (указатель на объект CBaseTask).
	*/
	static void vTask( void *pvParameters );

	/// Функция задачи для переопределения в потомках.
	virtual void Run()=0;

	/// Получить сообщение из очереди.
	/*!
	  \param[out] msg Указатель на сообщение.
	  \param[in] xTicksToWait Время ожидания в тиках.
	  \return true в случае успеха.
	*/
	bool GetMessage(STaskMessage* msg, TickType_t xTicksToWait=0);
public:
	/// Начальная инициализация.
	/*!
	  \param[in] name Имя задачи длиной не более configMAX_TASK_NAME_LEN.
	  \param[in] usStack Размер стека в двойных словах (4 байта).
	  \param[in] uxPriority Приоритет. Не более configMAX_PRIORITIES.
	  \param[in] queueLength Максимальная длина очереди сообщений.
	*/
	virtual void Init(const char * name,unsigned short usStack, UBaseType_t uxPriority, UBaseType_t queueLength);

	/// Послать сообщение в задачу.
	/*!
	  \param[in] msg Указатель на сообщение.
	  \param[in] nFlag Флаг сообщения о не пустой очереди для задачи. Если 0, то механизм Notify не используется.
	  \param[in] xTicksToWait Время ожидания в тиках.
	  \param[in] free вернуть память в кучу в случае неудачи.
	  \return true в случае успеха.
	*/
	bool SendMessage(STaskMessage* msg, uint32_t nFlag=0, TickType_t xTicksToWait=0, bool free=false);
	/// Послать сообщение в задачу в начало очереди.
	/*!
	  \param[in] msg Указатель на сообщение.
	  \param[in] nFlag Флаг сообщения о не пустой очереди для задачи. Если 0, то механизм Notify не используется.
	  \param[in] xTicksToWait Время ожидания в тиках.
	  \param[in] free вернуть память в кучу в случае неудачи.
	  \return true в случае успеха.
	*/
	bool SendMessageFront(STaskMessage* msg, uint32_t nFlag=0, TickType_t xTicksToWait=0, bool free=false);
	/// Послать сообщение в задачу из прерывания.
	/*!
	  \param[in] msg Указатель на сообщение.
	  \param[out] pxHigherPriorityTaskWoken Флаг переключения задач.
	  \param[in] nFlag Флаг сообщения о не пустой очереди для задачи. Если 0, то механизм Notify не используется.
	  \return true в случае успеха.
	*/
	bool SendMessageFromISR(STaskMessage* msg, BaseType_t *pxHigherPriorityTaskWoken,uint32_t nFlag=0);

	/// Выделить память сообщению.
	/*!
	  \param[in] msg Указатель на сообщение.
	  \param[in] cmd Номер команды.
	  \param[in] size Размер выделяемой памяти.
	  \return указатель на выделеную память.
	*/
	uint8_t* AllocNewMsg(STaskMessage* msg, uint16_t cmd, uint16_t size);
};
/*! @} */

#endif // CBASETASK_H

