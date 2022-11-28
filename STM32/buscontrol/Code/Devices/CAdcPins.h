/*!
	\file
	\brief Класс для управления аналоговыми выводами внешнего раъъёма.
	\authors Близнец Р.А.
	\version 1.0.0.0
	\date 17.12.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/
*/

#if !defined CADCPINS_H
#define CADCPINS_H


#include "settings.h"
#include "adc.h"
#include "tim.h"
#include "task.h"

/// Внешние выводы.
enum EEXTERNAL_PINS
{
	EEXTERNAL_IO1=0,
	EEXTERNAL_IO2=1,
	EEXTERNAL_IO3=2,
	EEXTERNAL_IO4=3,
	EEXTERNAL_IO5=4,
	EEXTERNAL_IO6=5,
	EEXTERNAL_IO7=6,
	EEXTERNAL_IO8=7
};

/// Класс для управления аналоговыми выводами внешнего раъъёма.
/*!
 * Периодическое считывание последовательности каналов АЦП через DMA.
 * ADCPINS_ADC АЦП для считывания
 * ADCPINS_TIM таймер периода считывания
*/
class CAdcPins
{
protected:
	TaskHandle_t mTaskToNotify=nullptr;	///< Указатель на задачу, ожидающую события.
	uint32_t mNotifyFlag;				///< Флаг для оповещения задачи об окончании конвертации.

	uint16_t mAdcData[12];				///< Данные АЦП.
	uint16_t mDmaData[12];				///< Данные для DMA.
	uint16_t mAdcSize=4;				///< Количество каналов АЦП в считывавемой последовательности (не более 12).
	/**
	  * @brief  Conversion complete callback in non blocking mode
	  * @param  hadc : ADC handle
	  * @note   This example shows a simple way to report end of conversion
	  *         and get conversion result. You can add your own implementation.
	  * @retval None
	  */
	static void HAL_ADC_ConvCpltCallback(ADC_HandleTypeDef *hadc);
//	static void HAL_ADC_ErrorCallback(ADC_HandleTypeDef *hadc);

	/// Функция для прерывания окончания считывания.
	void convCpltCallback();
public:
	/// Едиственный экземпляр класса.
	/*!
	  \return Указатель на CAdcPins
	*/
	static CAdcPins* Instance()
	{
		static CAdcPins theSingleInstance;
		return &theSingleInstance;
	}

	/// Инициализация новыми параметрами.
	/*!
  	  \warning Вызывать только из задачи контролирующей ADC.
	  \param[in] xNotifyBit Номер бита для оповещения задачи  об окончании конвертации.
	  \param[in] period Период опроса в мкс.
	  \sa free()
	*/
	void init(uint8_t xNotifyBit, uint32_t period=10000);

	/// Освобождение ресурсов.
	/*!
	  \sa init()
	*/
	void free();

	/// Добавить канал для измерения.
	/*!
	  \param[in] pin Номер внешнего вывода.
	  \param[in] samplingTime Циклы на конвертацию от ADC_SAMPLETIME_2CYCLES_5 до ADC_SAMPLETIME_640CYCLES_5.
	  \return Количество каналов АЦП в считывавемой последовательности
	  \sa clearChannels()
	*/
	uint16_t addChannel(EEXTERNAL_PINS pin, uint32_t samplingTime);
	/// Убрать внешние каналы для измерения.
	/*!
	  \return Количество каналов АЦП в считывавемой последовательности
	  \sa clearChannels()
	*/
	uint16_t clearChannels();

	/// Флаг занятой АЦП.
	/*!
	  \return Флаг занятой АЦП
	*/
	inline bool isBusy()
	{
		return (mTaskToNotify != nullptr);
	};
	/// Количество каналов АЦП в считывавемой последовательности.
	/*!
	  \return Количество каналов АЦП в считывавемой последовательности
	*/
	inline uint16_t getSize()
	{
		return mAdcSize;
	};
	/// Указатель на данные АЦП.
	/*!
	  \return Указатель на данные АЦП
	*/
	inline uint16_t* getData()
	{
		return mAdcData;
	};
};

#endif // CADCPINS_H

