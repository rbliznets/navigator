/*!
	\file
	\brief Класс для управления аналоговыми выводами внешнего раъъёма.
	\authors Близнец Р.А.
	\version 1.0.0.0
	\date 17.12.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/
*/

#include "CAdcPins.h"
#include "../Algorithms/Debug/CTrace.h"
#include <cstring>

__attribute__((long_call, section(".RamFunc"))) void CAdcPins::HAL_ADC_ConvCpltCallback(ADC_HandleTypeDef *hadc)
{
	CAdcPins::Instance()->convCpltCallback();
}

__attribute__((long_call, section(".RamFunc"))) void CAdcPins::convCpltCallback()
{
	std::memcpy(mAdcData,mDmaData,mAdcSize*sizeof(uint16_t));
	BaseType_t xHigherPriorityTaskWoken=pdFALSE;
	xTaskNotifyFromISR(mTaskToNotify,mNotifyFlag,eSetBits,&xHigherPriorityTaskWoken);
	portYIELD_FROM_ISR( xHigherPriorityTaskWoken );
}

//__attribute__((long_call, section(".RamFunc"))) void CAdcPins::HAL_ADC_ErrorCallback(ADC_HandleTypeDef *hadc)
//{
//	HAL_ADC_Stop_DMA(ADCPINS_ADC);
//	HAL_ADC_Start_DMA(ADCPINS_ADC, (uint32_t*)CAdcPins::Instance()->mDmaData, CAdcPins::Instance()->mAdcSize);
//}

uint16_t CAdcPins::addChannel(EEXTERNAL_PINS pin, uint32_t samplingTime)
{
	if(mAdcSize < 12)
	{
		bool b=isBusy();
		ADC_ChannelConfTypeDef sConfig;

		sConfig.SamplingTime = samplingTime;
		sConfig.SingleDiff = ADC_SINGLE_ENDED;
		sConfig.OffsetNumber = ADC_OFFSET_NONE;
		sConfig.Offset = 0;
		switch(mAdcSize)
		{
		case 4:
			sConfig.Rank = ADC_REGULAR_RANK_5;
			break;
		case 5:
			sConfig.Rank = ADC_REGULAR_RANK_6;
			break;
		case 6:
			sConfig.Rank = ADC_REGULAR_RANK_7;
			break;
		case 7:
			sConfig.Rank = ADC_REGULAR_RANK_8;
			break;
		case 8:
			sConfig.Rank = ADC_REGULAR_RANK_9;
			break;
		case 9:
			sConfig.Rank = ADC_REGULAR_RANK_10;
			break;
		case 10:
			sConfig.Rank = ADC_REGULAR_RANK_11;
			break;
		case 11:
			sConfig.Rank = ADC_REGULAR_RANK_12;
			break;
		}
		switch(pin)
		{
		case EEXTERNAL_IO1:
			sConfig.Channel = ADC_CHANNEL_1;
			break;
		case EEXTERNAL_IO2:
			sConfig.Channel = ADC_CHANNEL_2;
			break;
		case EEXTERNAL_IO3:
			sConfig.Channel = ADC_CHANNEL_3;
			break;
		case EEXTERNAL_IO4:
			sConfig.Channel = ADC_CHANNEL_4;
			break;
		case EEXTERNAL_IO5:
			sConfig.Channel = ADC_CHANNEL_5;
			break;
		case EEXTERNAL_IO6:
			sConfig.Channel = ADC_CHANNEL_6;
			break;
		case EEXTERNAL_IO7:
			sConfig.Channel = ADC_CHANNEL_7;
			break;
		case EEXTERNAL_IO8:
			sConfig.Channel = ADC_CHANNEL_8;
			break;
		}

		if(b)HAL_ADC_Stop_DMA(ADCPINS_ADC);
		mAdcSize++;
	    MODIFY_REG(ADCPINS_ADC->Instance->SQR1, ADC_SQR1_L, (mAdcSize - (uint8_t)1));
	    HAL_ADC_ConfigChannel(ADCPINS_ADC, &sConfig);
		if(b)HAL_ADC_Start_DMA(ADCPINS_ADC, (uint32_t*)mDmaData, mAdcSize);
	}
	return mAdcSize;
}

uint16_t CAdcPins::clearChannels()
{
	bool b=isBusy();
	if(b)HAL_ADC_Stop_DMA(ADCPINS_ADC);
	mAdcSize=4;
    MODIFY_REG(ADCPINS_ADC->Instance->SQR1, ADC_SQR1_L, (mAdcSize - (uint8_t)1));
	if(b)HAL_ADC_Start_DMA(ADCPINS_ADC, (uint32_t*)mDmaData, mAdcSize);
	return mAdcSize;
}

void CAdcPins::init(uint8_t xNotifyBit, uint32_t period)
{
	free();

	if(HAL_ADCEx_Calibration_Start(ADCPINS_ADC, ADC_SINGLE_ENDED) != HAL_OK)
	{
		TRACE("CAdcPins: ADC Calibration failed",8430,false);
	}

	mTaskToNotify=xTaskGetCurrentTaskHandle();
	mNotifyFlag=(1 << xNotifyBit);
	ADCPINS_TIM->Instance->ARR=period;
	HAL_ADC_Stop_DMA(ADCPINS_ADC);

	if(HAL_ADC_RegisterCallback(ADCPINS_ADC,HAL_ADC_CONVERSION_COMPLETE_CB_ID,HAL_ADC_ConvCpltCallback) != HAL_OK)
	{
		TRACE("CAdcPins: ADC RegisterCallback failed",8431,false);
	}
//	if(HAL_ADC_RegisterCallback(ADCPINS_ADC,HAL_ADC_ERROR_CB_ID,HAL_ADC_ErrorCallback) != HAL_OK)
//	{
//		TRACE("CAdcPins: ADC RegisterCallback failed",8431,false);
//	}
	if (HAL_TIM_Base_Start(ADCPINS_TIM) != HAL_OK)
	{
		/* Counter enable error */
		TRACE("CAdcPins: ADC Start_TIM failed",8433,false);
	}
	if(HAL_ADC_Start_DMA(ADCPINS_ADC, (uint32_t*)mDmaData, mAdcSize) != HAL_OK)
	{
	    /* ADC conversion start error */
		TRACE("CAdcPins: ADC Start_DMA failed",8432,false);
	}
}

void CAdcPins::free()
{
	if(mTaskToNotify != nullptr)
	{
		HAL_ADC_Stop_DMA(ADCPINS_ADC);
		HAL_TIM_Base_Stop(ADCPINS_TIM);
		HAL_ADC_UnRegisterCallback(ADCPINS_ADC,HAL_ADC_CONVERSION_COMPLETE_CB_ID);
		mTaskToNotify=nullptr;
	}
}
