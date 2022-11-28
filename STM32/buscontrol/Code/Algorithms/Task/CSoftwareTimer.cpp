/*!
	\file
	\brief Программный таймер под задачи FreeRTOS.
	\authors Близнец Р.А.
	\version 1.0.0.1
	\date 28.10.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/
*/

#include "CSoftwareTimer.h"
#include <cstdio>

/// Обработчик события таймера.
/*!
  \param[in] xTimer Хэндлер таймера FreeRTOS.
*/
extern "C" void vTimerCallback( TimerHandle_t xTimer )
{
	CSoftwareTimer* tm = (CSoftwareTimer*) pvTimerGetTimerID(xTimer);
	tm->Timer();
}

int CSoftwareTimer::Start(uint8_t xNotifyBit, uint32_t period, bool autoRefresh)
{
	if(IsRun())
	{
		if(xTimerStop(mTimerHandle, 0) != pdTRUE)
		{
#ifdef DEBUG
			std::printf("CSoftwareTimer::Start xTimerStop failed\n");
#endif
			return -3;
		}
		vTimerSetReloadMode(mTimerHandle, mAutoRefresh);
		if(xTimerChangePeriod(mTimerHandle, period, 0) == pdTRUE)
		{
			return 0;
		}
		else
		{
#ifdef DEBUG
			std::printf("CSoftwareTimer::Start xTimerChangePeriod failed\n");
#endif
			return -4;
		}
	}
	else
	{
		mTaskToNotify=xTaskGetCurrentTaskHandle();
		mNotifyBit=xNotifyBit;
		mAutoRefresh=autoRefresh;
		mTimerHandle=xTimerCreate("Timer", pdMS_TO_TICKS(period), mAutoRefresh, this, vTimerCallback);
		if(mTimerHandle != NULL)
		{
			if(xTimerStart(mTimerHandle, 0) == pdTRUE)
			{
				return 0;
			}
			else
			{
#ifdef DEBUG
				std::printf("CSoftwareTimer::Start xTimerStart failed\n");
#endif
				return -2;
			}
		}
		else
		{
#ifdef DEBUG
			std::printf("CSoftwareTimer::Start xTimerCreate failed\n");
#endif
			return -1;
		}
	}
}

int CSoftwareTimer::Stop()
{
	if(mTimerHandle != NULL)
	{
		if(xTimerStop(mTimerHandle, 0) == pdTRUE)
		{
			if(xTimerDelete(mTimerHandle, 0) == pdTRUE)
			{
				mTimerHandle=NULL;
				return 0;
			}
			else
			{
#ifdef DEBUG
				std::printf("CSoftwareTimer::Stop xTimerDelete failed\n");
#endif
				return -3;
			}
		}
		else
		{
#ifdef DEBUG
			std::printf("CSoftwareTimer::Stop xTimerStop failed\n");
#endif
			return -2;
		}
	}
	else
	{
#ifdef DEBUG
		std::printf("CSoftwareTimer::Stop mTimerHandle==NULL\n");
#endif
		return -1;
	}
}

void CSoftwareTimer::Timer()
{
	if(!mAutoRefresh)
	{
		if(xTimerDelete(mTimerHandle, 0) == pdTRUE)
		{
			mTimerHandle=NULL;
		}
#ifdef DEBUG
		else
		{
			std::printf("CSoftwareTimer::Timer xTimerDelete failed\n");
		}
#endif
	}
//	std::printf("Timer\n");
	xTaskNotify(mTaskToNotify,(1 << mNotifyBit),eSetBits);
}
