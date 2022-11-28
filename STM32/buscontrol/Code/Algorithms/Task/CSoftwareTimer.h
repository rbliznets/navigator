/*!
	\file
	\brief Программный таймер под задачи FreeRTOS.
	\authors Близнец Р.А.
	\version 1.0.0.1
	\date 28.10.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/
*/

#if !defined CSOFTWARETIMER_H
#define CSOFTWARETIMER_H

/*!
    \defgroup soft_timer Таймер
    \ingroup freertos
    \brief Таймер FreeRTOS.

   	@{
*/

#include "FreeRTOS.h"
#include "task.h"
#include "timers.h"

/// Программный таймер под задачи FreeRTOS.
class CSoftwareTimer
{
protected:
	TimerHandle_t mTimerHandle=NULL; 	///< Хэндлер таймера FreeRTOS.
	TaskHandle_t mTaskToNotify; 		///< Указатель на задачу, ожидающую события от таймера.
	uint8_t mNotifyBit;					///< Номер бита для оповещения задачи о событии таймера (не более 31).
	bool mAutoRefresh;					///< Флаг автозагрузки таймера.

public:
	/// Запуск таймера.
	/*!
  	  \warning Вызывать только из задачи FreeRTOS.
	  \param[in] xNotifyBit Номер бита для оповещения задачи о событии таймера.
	  \param[in] period Период в милисекундах.
	  \param[in] autoRefresh Флаг автозагрузки таймера. Если false, то таймер запускается один раз.
	  \return 0 - в случае успеха.
	  \sa Stop()
	*/
	int Start(uint8_t xNotifyBit, uint32_t period, bool autoRefresh=false);
	/// Остановка таймера.
	/*!
	  \return 0 - в случае успеха.
	  \sa Start()
	*/
	int Stop();

	/// Сосотояние таймера.
	/*!
	  \return Сосотояние таймера.
	*/
	bool IsRun()
	{
		return mTimerHandle != NULL;
	};

	/// Функция, вызываемая по событию в таймере.
	void Timer();
};
/*! @} */

#endif // CSOFTWARETIMER_H

