/*!
	\file
	\brief Базовый класс для захвата ресурса задач FreeRTOS.
	\authors Близнец Р.А.
	\version 1.0.0.0
	\date 28.10.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/
*/

#if !defined CLOCK_H
#define CLOCK_H

/*!
    \addtogroup f_tasks

   	@{
*/

#include "FreeRTOS.h"
#include "semphr.h"


/// Базовый класс для захвата ресурса.
class CLock
{
protected:
	SemaphoreHandle_t mMutex; ///< Хэндлер мьютекса.

	/// Инициализация новыми параметрами.
	/*!
	  \param[in] mutex Указатель на на семафор для мьютекса.
	*/
	void Init(SemaphoreHandle_t mutex)
	{
		mMutex=mutex;
	};
	
	/// Захват ресурса.
	void Lock();
	/// Освобождение ресурса.
	void UnLock();
public:
	/// Конструктор класса.
	CLock();
};
/*! @} */

#endif // CLOCK_H

