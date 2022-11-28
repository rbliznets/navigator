/*!
	\file
	\brief Класс для управления CAN шиной.
	\authors Близнец Р.А.
	\version 0.0.0.1
	\date 24.12.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/
*/

#include "CCAN.h"
#include "../Algorithms/Debug/CTrace.h"
#include <cstring>

__attribute__((long_call, section(".RamFunc"))) void CCAN::HAL_FDCAN_RxFifo0Callback(FDCAN_HandleTypeDef* hfdcan, uint32_t RxFifo0ITs)
{
	CCAN::Instance()->rxFifo0Callback(hfdcan, RxFifo0ITs);
}

__attribute__((long_call, section(".RamFunc"))) void CCAN::rxFifo0Callback(FDCAN_HandleTypeDef* hfdcan, uint32_t RxFifo0ITs)
{
	if(hfdcan == mCan)
	{
	    /* Retrieve Rx messages from RX FIFO0 */
//	    if (HAL_FDCAN_GetRxMessage(hfdcan, FDCAN_RX_FIFO0, &RxHeader, RxData) != HAL_OK)
//	    {
//	      Error_Handler();
//	    }

		BaseType_t xHigherPriorityTaskWoken=pdFALSE;
		xTaskNotifyFromISR(mTaskToNotify,mNotifyFlag,eSetBits,&xHigherPriorityTaskWoken);
		portYIELD_FROM_ISR( xHigherPriorityTaskWoken );
	}
}


void CCAN::init(FDCAN_HandleTypeDef* fdcan, uint8_t xNotifyBit)
{
	free();

	mTxHeader.BitRateSwitch=FDCAN_BRS_OFF;
	mTxHeader.ErrorStateIndicator=FDCAN_ESI_PASSIVE;
	mTxHeader.FDFormat=FDCAN_CLASSIC_CAN;
	mTxHeader.IdType = FDCAN_STANDARD_ID;
	mTxHeader.MessageMarker = 0x22;
	mTxHeader.TxEventFifoControl=FDCAN_NO_TX_EVENTS;
	mTxHeader.TxFrameType=FDCAN_DATA_FRAME;

	mCan=fdcan;
	mTaskToNotify=xTaskGetCurrentTaskHandle();
	mNotifyFlag=(1 << xNotifyBit);

	HAL_FDCAN_RegisterRxFifo0Callback(mCan,HAL_FDCAN_RxFifo0Callback);
	HAL_FDCAN_ActivateNotification(mCan, FDCAN_IT_RX_FIFO0_NEW_MESSAGE, 0);
	HAL_FDCAN_Start(mCan);
}

void CCAN::free()
{
	if(mTaskToNotify != nullptr)
	{
		HAL_FDCAN_Stop(mCan);
		HAL_FDCAN_DeactivateNotification(mCan, FDCAN_IT_RX_FIFO0_NEW_MESSAGE);
		HAL_FDCAN_UnRegisterRxFifo0Callback(mCan);
		mTaskToNotify=nullptr;
	}
}

uint8_t* CCAN::getData(FDCAN_RxHeaderTypeDef*& rxHeader)
{
	if (HAL_FDCAN_GetRxMessage(mCan, FDCAN_RX_FIFO0, &mRxHeader, mRxData) != HAL_OK)
	{
		return nullptr;
	}
	else
	{
		rxHeader=&mRxHeader;
		return mRxData;
	}
}

void CCAN::sendData(uint32_t id, uint8_t* data, uint8_t size)
{
	mTxHeader.DataLength = size << 16;
	mTxHeader.Identifier = id;
	HAL_FDCAN_AddMessageToTxFifoQ(mCan, &mTxHeader, data);
}

