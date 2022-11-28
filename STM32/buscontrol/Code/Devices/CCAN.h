/*!
	\file
	\brief Класс для управления CAN шиной.
	\authors Близнец Р.А.
	\version 0.0.0.1
	\date 24.12.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/
*/

#if !defined CCAN_H
#define CCAN_H


#include "settings.h"
#include "fdcan.h"
#include "task.h"

/// Класс для управления CAN.
class CCAN
{
protected:
	FDCAN_HandleTypeDef* mCan;
	TaskHandle_t mTaskToNotify=nullptr;	///< Указатель на задачу, ожидающую события.
	uint32_t mNotifyFlag;				///< Флаг для оповещения задачи о приеме пакета.

	FDCAN_RxHeaderTypeDef mRxHeader;
	FDCAN_TxHeaderTypeDef mTxHeader;
	uint8_t mRxData[16];


	/**
	  * @brief  Rx FIFO 0 callback.
	  * @param  hfdcan pointer to an FDCAN_HandleTypeDef structure that contains
	  *         the configuration information for the specified FDCAN.
	  * @param  RxFifo0ITs indicates which Rx FIFO 0 interrupts are signalled.
	  *         This parameter can be any combination of @arg FDCAN_Rx_Fifo0_Interrupts.
	  * @retval None
	  */
	static void HAL_FDCAN_RxFifo0Callback(FDCAN_HandleTypeDef* hfdcan, uint32_t RxFifo0ITs);

	/// Функция для прерывания.
	void rxFifo0Callback(FDCAN_HandleTypeDef* hfdcan, uint32_t RxFifo0ITs);
public:
	/// Едиственный экземпляр класса.
	/*!
	  \return Указатель на CCAN
	*/
	static CCAN* Instance()
	{
		static CCAN theSingleInstance;
		return &theSingleInstance;
	}

	/// Инициализация новыми параметрами.
	/*!
  	  \warning Вызывать только из задачи контролирующей ADC.
	  \param[in] xNotifyBit Номер бита для оповещения задачи  об окончании конвертации.
	  \param[in] period Период опроса в мкс.
	  \sa free()
	*/
	void init(FDCAN_HandleTypeDef* fdcan, uint8_t xNotifyBit);

	/// Освобождение ресурсов.
	/*!
	  \sa init()
	*/
	void free();

	uint8_t* getData(FDCAN_RxHeaderTypeDef*& rxHeader);
	void sendData(uint32_t id, uint8_t* data, uint8_t size);


	/// Флаг работы.
	/*!
	  \return Флаг работы
	*/
	inline bool isBusy()
	{
		return (mTaskToNotify != nullptr);
	};
};

#endif // CCAN_H

