/*!
	\file
	\brief Базовый класс для захвата ресурса задач FreeRTOS.
	\authors Близнец Р.А.
	\version 1.0.0.0
	\date 28.10.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/
*/

#include "CLock.h"

CLock::CLock()
{
}

void CLock::Lock()
{
	if(mMutex != NULL)xSemaphoreTake(mMutex, portMAX_DELAY);
}

void CLock::UnLock()
{
	if(mMutex != NULL)xSemaphoreGive(mMutex);
}
