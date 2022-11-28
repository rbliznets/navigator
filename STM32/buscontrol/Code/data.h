/*!
	\file
	\brief Структуры.
	\authors Близнец Р.А.
	\version 0.0.0.1
	\date 28.10.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/

	Опции условной компеляции, начтройки статических буферов и привязки классов устройств к железу.
*/

#ifndef DATA_H_
#define DATA_H_

/*! \defgroup struct Структуры
  	\brief Общие структуры.
    @{
*/

#include "main.h"
#include "FreeRTOS.h"


/// Структура для измерения реального времени в режиме отладки.
struct STimeSpan
{
	uint32_t Ticks;			///< Количество тиков FreeRTOS.
	uint32_t SysTimerCount; ///< Количество тактов системного таймера.
};

#define MAGIC_CALIBRATE 0xDEADBEEF  ///< Данные для проверки корректного заголовка калибровочных данных.
#define CALIBRATE_KSIZE 4  			///< Количество калибровочных коэффициентов.

/// Структура заголовка калибровочных данных
struct SCalibrateHeader
{
	uint32_t magic; 				///< Поле для проверки корректного заголовка ккалибровочных данных. Должно быть MAGIC_CALIBRATE.
	uint16_t crc16;					///< Контрольная сумма набора регистров.
	uint16_t size;					///<Размер данных.
	uint32_t sn; 					///< Серийный номер.
	float kADC[CALIBRATE_KSIZE];	///< Коэфициенты для пересчета значений АЦП.
};

/*! @} */

#endif /* DATA_H_ */
