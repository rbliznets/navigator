/*!
	\file
	\brief Базовый класс для реализации задачи FreeRTOS.
	\authors Близнец Р.А.
	\version 1.0.0.0
	\date 28.10.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/
*/

#include "CBaseTask.h"
#include <cstdio>
#include <cstring>

void CBaseTask::vTask( void *pvParameters )
{
	((CBaseTask*)pvParameters)->Run();
#ifdef DEBUG
	std::printf("Task exit\n");
#endif
#if (INCLUDE_vTaskDelete == 1)
	vQueueDelete(((CBaseTask*)pvParameters)->mTaskQueue);
	vTaskDelete(NULL);
#else
	for(;;);
#endif
}

void CBaseTask::Init(const char * name,unsigned short usStack, UBaseType_t uxPriority, UBaseType_t queueLength)
{
#ifdef useEXT_CHECK
	configASSERT(uxPriority <=  configMAX_PRIORITIES);
	configASSERT(usStack >=  configMINIMAL_STACK_SIZE);
	configASSERT(std::strlen(name) <  configMAX_TASK_NAME_LEN);
#endif
	mTaskQueue=xQueueCreate( queueLength, sizeof(STaskMessage) );
	xTaskCreate( vTask, name, usStack, this, uxPriority, &mTaskHandle );
	mInit=true;
}

bool CBaseTask::SendMessage(STaskMessage* msg,uint32_t nFlag, TickType_t xTicksToWait, bool free)
{
#ifdef useEXT_CHECK
	configASSERT(msg !=  NULL);
#endif
	if(xQueueSend(mTaskQueue,msg,xTicksToWait)==pdPASS)
	{
		if(nFlag != 0)
		{
			return(xTaskNotify(mTaskHandle,nFlag,eSetBits) == pdPASS);
		}
		else return true;
	}
	else
	{
		if(free)vPortFree(msg->msgBody);
#ifdef DEBUG
		std::printf("%s:SendMessage %d failed.\n",pcTaskGetName(mTaskHandle),msg->msgID);
#endif
		return false;
	}
}

bool CBaseTask::SendMessageFront(STaskMessage* msg,uint32_t nFlag, TickType_t xTicksToWait, bool free)
{
#ifdef useEXT_CHECK
	configASSERT(msg !=  NULL);
#endif
	if(xQueueSendToFront(mTaskQueue,msg,xTicksToWait)==pdPASS)
	{
		if(nFlag != 0)
		{
			return(xTaskNotify(mTaskHandle,nFlag,eSetBits) == pdPASS);
		}
		else return true;
	}
	else
	{
		if(free)vPortFree(msg->msgBody);
#ifdef DEBUG
		std::printf("%s:SendMessage %d failed.\n",pcTaskGetName(mTaskHandle),msg->msgID);
#endif
		return false;
	}
}

bool CBaseTask::SendMessageFromISR(STaskMessage* msg, BaseType_t *pxHigherPriorityTaskWoken,uint32_t nFlag)
{
#ifdef useEXT_CHECK
	configASSERT(msg !=  NULL);
#endif
	if(xQueueSendFromISR(mTaskQueue,msg,pxHigherPriorityTaskWoken)==pdPASS)
	{
		if(nFlag != 0)
		{
			return(xTaskNotifyFromISR(mTaskHandle,nFlag,eSetBits,pxHigherPriorityTaskWoken) == pdPASS);
		}
		else return true;
	}
	else return false;
}

bool CBaseTask::GetMessage(STaskMessage* msg, TickType_t xTicksToWait)
{
#ifdef useEXT_CHECK
	configASSERT(msg !=  NULL);
#endif
	return (xQueueReceive(mTaskQueue,msg,xTicksToWait)== pdTRUE);
}

uint8_t* CBaseTask::AllocNewMsg(STaskMessage* msg, uint16_t cmd, uint16_t size)
{
#ifdef useEXT_CHECK
	configASSERT(msg !=  NULL);
	configASSERT(size >  0);
#endif
	msg->msgID=cmd;
	msg->shortParam=size;
	msg->msgBody=pvPortMalloc(msg->shortParam);
	return (uint8_t*)msg->msgBody;
}

