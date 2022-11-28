/*!
	\file
	\brief Класс для управления выводами внешнего раъъёма.
	\authors Близнец Р.А.
	\version 1.0.0.0
	\date 21.12.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/
*/

#if !defined CEXTERNALPINS_H
#define CEXTERNALPINS_H


#include "settings.h"
#include "task.h"
#include "CAdcPins.h"
#include <string>
#include "../Algorithms/DataFlow/CJsonParser.h"

/// Функционал внешнего вывода.
enum EEXT_PIN_TYPE
{
	EEXT_PIN_NONE,	///< Вывод не задействован.
	EEXT_PIN_ADC,	///< Вывод АЦП.
	EEXT_PIN_IN,	///< Цифровой вход.
	EEXT_PIN_OUT	///< Цифровой выход.
};

/// Класс для управления выводами внешнего раъъёма.
struct SExternalPin
{
	EEXT_PIN_TYPE type;	///< Тип вывода.
	int sensitivity;	///< Чувствительность вывода АЦП. Разность 12-битного прошлого и текущего значения при котором создаётся сообщение.
	uint16_t index;		///< Индекс текущего значения АЦП в массиве от CAdcPins.
	uint16_t adc;		///< Прошлое значение АЦП или состояния входа.
};

/// Класс для управления выводами внешнего раъъёма.
/*!
 * Периодическое считывание последовательности каналов АЦП и цифровых выводов.
*/
class CExternalPins
{
private:
	uint16_t mTempSens;	   ///< Чувствительность АЦП температурного датчика.

protected:
	SExternalPin mPins[8];	///< Массив состояний выводов IO1..IO8.
	uint16_t mAdcSize=4;	///< Количество отслеживаемых каналов АЦП.
	uint16_t mTemp=0;		///< Прошлое значение АЦП температурного датчика.
	uint16_t mVp=0;			///< Прошлое значение АЦП питания STM.
	uint16_t mVext=0;		///< Прошлое значение АЦП внешнего питания.
	uint16_t mVbat=0;		///< Прошлое значение АЦП питания SC66.

	/// Настройка вывода.
	/*!
	  \param[in] json Парсер JSON.
	  \param[in] io Токен JSON для анализа.
	  \param[in] pin Вывод IO1..IO8.
	*/
	void setPin(CJsonParser* json, int io, EEXTERNAL_PINS pin);
	/// Установка входного вывода.
	/*!
	  \param[in] pin Вывод IO1..IO8.
	  \sa setOutput()
	*/
	void setInput(EEXTERNAL_PINS pin);
	/// Установка выходного вывода.
	/*!
	  \param[in] pin Вывод IO1..IO8.
	  \param[in] value значение вывода 0,1.
	  \sa setInput()
	*/
	void setOutput(EEXTERNAL_PINS pin, int value);
	/// Задать выходное значение.
	/*!
	  \param[in] pin Вывод IO1..IO8.
	  \param[in] value значение вывода 0,1.
	*/
	void setValue(EEXTERNAL_PINS pin, int value);
	/// Прочитать входное значение.
	/*!
	  \param[in] pin Вывод IO1..IO8.
	  \return значение вывода 0,1. Если вывод не утановлен как входной, то 2.
	*/
	int getValue(EEXTERNAL_PINS pin);

	/// Конструктор.
	CExternalPins();
public:
	/// Едиственный экземпляр класса.
	/*!
	  \return Указатель на CExternalPins
	*/
	static CExternalPins* Instance()
	{
		static CExternalPins theSingleInstance;
		return &theSingleInstance;
	}

	/// Команды JSON установки и контроля.
	/*!
  	  \warning Вызывать только из задачи контролирующей ADC.
	  \param[in] json Парсер JSON.
	  \param[in] root Корневой токен.
	  \param[in] xNotifyBit Номер бита для оповещения задачи  об окончании конвертации.
	  \sa stop(),start(),update()

	Установка выводов
	{
		"ext_pins":{
			"settings":{
				"period":10000, 		# Период опроса в мкс. Если поле отсутсвует, то опрос останавливается.
				"IO1":{					# Номер вывода. Если вывод не задан, то он деактивируется.
					"type":"adc",		# Тип вывода: АЦП.
					"time":2,			# Время коннвериации в циклах 0:2.5, 1:6.5, 2:12.5, 3:24.5, 4:47.5, 5:92.5, 6:247.5, 7:640.5.
					"sensitivity":20	# Чувствительность вывода АЦП.
				},
				"IO2":{					# Номер вывода. Если вывод не задан, то он деактивируется.
					"type":"in"			# Тип вывода: вход.
				},
				"IO3":{					# Номер вывода. Если вывод не задан, то он деактивируется.
					"type":"out",		# Тип вывода: выход.
					"value":1			# Значение выхода 0,1.
				}
			}
		}
	}

	Установка выходов
	{
		"ext_pins":{
			"pins":{
				"IO3":0					# Номер вывода и его значение 0,1.
			}
		}
	}
	*/
	void command(CJsonParser* json, int root, uint8_t xNotifyBit);

	/// Обработка входных знвчений.
	/*!
	 * Вызывается из задвчи по собвтию заданому в command (xNotifyBit).
	 *
	  \param[out] json Строка с текущеми изменениями.
	  \return true если есть событие.
	  \sa stop(),start(),command()
	*/
	bool update(std::string& msg);

	/// Начать измерения без внешних выводов.
	/*!
  	  \warning Вызывать только из задачи контролирующей ADC.
	  \param[in] xNotifyBit Номер бита для оповещения задачи  об окончании конвертации.
	  \param[in] period Период опроса в мкс.
	  \sa update(),stop(),command()
	*/
	void start(uint8_t xNotifyBit, uint32_t period=100000);
	/// Остановить измерения.
	/*!
	 * Вместо деструктора
	  \sa update(),start(),command()
	*/
	void stop();

	/// Текушее значение внешнего напряжения.
	/*!
	  \return значение внешнего напряжения
	*/
	float getPowerVoltage();
};

#endif // CEXTERNALPINS_H

