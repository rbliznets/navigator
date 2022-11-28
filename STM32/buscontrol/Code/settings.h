/*!
	\file
	\brief Настройки паоаметров.
	\authors Близнец Р.А.
	\version 0.0.0.1
	\date 28.10.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/

	Опции условной компеляции, начтройки статических буферов и привязки классов устройств к железу.
*/

#ifndef SETTINGS_H_
#define SETTINGS_H_

/*! \defgroup timer Таймер
  	\ingroup Devices
  	\brief Устройства связанные с таймерами.
*/

/*! \defgroup settings Настройки
  	\brief Настройки ПО.
    @{
*/

#include "main.h"
#include "FreeRTOS.h"
//#include "data.h"

/*
 * Параметры условной компиляции
 */
#ifdef DEBUG
//#define UNIT_TESTS		///< Режим модульных тестов.
//#define useEXT_CHECK	///< Дополнительная проверка параметров.
#endif

#define FAST_CODE __attribute__((long_call, section(".fast_code"))) ///< Размещение функции в RAM.
#define portNVIC_SYSTICK_CURRENT_VALUE_REG	( * ( ( volatile uint32_t * ) 0xe000e018 ) ) ///< Адрес счетчика системного таймера.

/*
 * Настройки устройств
 */
#define LED1	LED1_GPIO_Port, LED1_Pin
#define LED2	LED2_GPIO_Port, LED2_Pin

#define USB_BUF_SIZE 2048

#define JSON_MIN_TOKEN_SIZE 30	///< Начальное максимальное значение токенов в JSON строке.

#define ADCPINS_ADC			(&hadc1)	///< ADC для внешних пинов.
#define ADCPINS_TIM			(&htim2)	///< Таймер считывания ADC для внешних пинов.

/*
 * Параметры задач
 */
#define LOGICTASK_NAME 			"Logic" ///< Имя задачи CLogicTask для отладки.
#define LOGICTASK_STACKSIZE 	(1024) 	///< Размер стека задачи CLogicTask в словах.
#define LOGICTASK_PRIOR 		(1) 	///< Приоритет задачи CLogicTask.
#define LOGICTASK_LENGTH 		(30) 	///< Длина приемной очереди задачи CLogicTask.

#define USBTASK_NAME 			"USB" 	///< Имя задачи CUSBTask для отладки.
#define USBTASK_STACKSIZE 		(512) 	///< Размер стека задачи CUSBTask в словах.
#define USBTASK_PRIOR 			(3) 	///< Приоритет задачи CUSBTask.
#define USBTASK_LENGTH 			(30) 	///< Длина приемной очереди задачи CUSBTask.

#define CANTASK_NAME 			"CAN" 	///< Имя задачи CCANTask для отладки.
#define CANTASK_STACKSIZE 		(512) 	///< Размер стека задачи CCANTask в словах.
#define CANTASK_PRIOR 			(4) 	///< Приоритет задачи CCANTask.
#define CANTASK_LENGTH 			(10) 	///< Длина приемной очереди задачи CCANTask.

/*
 * Номера команд для межзадачного обмена
 */
#define CMD_JSON_DATA		(10)	///< Номер команды пакета данных USB.


/*! @} */

#endif /* SETTINGS_H_ */
