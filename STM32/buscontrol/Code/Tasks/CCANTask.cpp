/*!
	\file
	\brief Класс для реализации приема/передачи CAN.
	\authors Близнец Р.А.
	\version 0.0.0.1
	\date 24.12.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/

	Один объект на приложение.
*/

#include "CCANTask.h"
#include "../Algorithms/Debug/CTrace.h"
#include "CUSBTask.h"

#include <cstdio>
#include <cstring>
#include <string>

void CCANTask::Run()
{
	uint32_t flags;
	STaskMessage msg;

	vTaskDelay(10);
	CCAN::Instance()->init(&hfdcan1, CANTASK_RX_BIT);
	std::string str;
	FDCAN_RxHeaderTypeDef* rxHeader;
	uint8_t* data;


	for( ;; )
	{
		if(xTaskNotifyWait(0,0xffffffff,&flags,portMAX_DELAY) == pdTRUE)
		{
			if((flags & CANTASK_RXC_FLAG)!=0)
			{
				while((data=CCAN::Instance()->getData(rxHeader)) != nullptr)
				{
					str="{\"can\":{\"id\":"+std::to_string(rxHeader->Identifier);
					int sz=(rxHeader->DataLength >> 16);
					if(sz > 0)
					{
						str+=",\"data\":[";
						int i;
						for(i=0;i<(sz-1);i++)
						{
							str+=std::to_string(data[i])+",";
						}
						str+=std::to_string(data[i]);
					}
					str+="]}}";

					auto dt=AllocNewMsg(&msg,CMD_JSON_DATA,str.length()+1);
					std::strcpy((char*)dt,str.c_str());
					CUSBTask::Instance()->SendMessage(&msg, 0, true);
				}
			}
			if((flags & CANTASK_QUEUE_FLAG)!=0)
			{
				while(GetMessage(&msg))
				{
					//DoMessage(msg);
				}
			}
		}
	}
}



