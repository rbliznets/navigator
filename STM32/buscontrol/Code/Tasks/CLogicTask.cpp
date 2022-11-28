/*!
	\file
	\brief Класс для реализации основной задачи.
	\authors Близнец Р.А.
	\version 0.0.0.1
	\date 28.10.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/

	Один объект на приложение.
*/

#include "CLogicTask.h"
#include "../Algorithms/Debug/CTrace.h"
#include "CUSBTask.h"

#include <cstdio>
#include <cstring>
#include <string>

void CLogicTask::Run()
{
#ifdef DEBUG
	std::string str="Navigator-BKN build on ";
	str=str+__DATE__+" "+__TIME__;
	LOG(str);
#endif
	uint32_t flags;
	STaskMessage msg;
	HAL_GPIO_WritePin(LED1, GPIO_PIN_SET);
	HAL_GPIO_WritePin(LED2, GPIO_PIN_RESET);

	HAL_GPIO_WritePin(KEY_ON_GPIO_Port, KEY_ON_Pin, GPIO_PIN_SET);
	vTaskDelay(pdMS_TO_TICKS(10));
	HAL_GPIO_WritePin(RESET_LAN_GPIO_Port,RESET_LAN_Pin, GPIO_PIN_SET);
	HAL_GPIO_WritePin(AMP_GAIN_GPIO_Port, AMP_GAIN_Pin, GPIO_PIN_RESET); // 1 for 24 Volts, 0 for 12 Volts
	HAL_GPIO_WritePin(RESET_GPIO_Port, RESET_Pin, GPIO_PIN_SET);

	HAL_GPIO_WritePin(PWRKEY_GPIO_Port, PWRKEY_Pin, GPIO_PIN_SET);
	vTaskDelay(pdMS_TO_TICKS(100));
	HAL_GPIO_WritePin(PWRKEY_GPIO_Port, PWRKEY_Pin, GPIO_PIN_RESET);
	HAL_GPIO_WritePin(CMUTE_GPIO_Port, CMUTE_Pin, GPIO_PIN_SET);

//	mTm.Start(LOGICTASK_START_BIT, 5000, true);
//	const char* cstr="{\"cmd\":4}";
	bool bFirst=true;
	CExternalPins::Instance()->start(LOGICTASK_ADC_BIT);
	std::string sExPins;

	for( ;; )
	{
		if(xTaskNotifyWait(0,0xffffffff,&flags,portMAX_DELAY) == pdTRUE)
		{
			if((flags & LOGICTASK_ADC_FLAG)!=0)
			{
				if(CExternalPins::Instance()->update(sExPins))
				{
					HAL_GPIO_TogglePin(LED1);
					if(bFirst)
					{
						bFirst=false;
						#ifdef DEBUG
								std::printf("V=%.2fV\n", CExternalPins::Instance()->getPowerVoltage());
						#endif
						if(CExternalPins::Instance()->getPowerVoltage() > 18.0)
						{
							HAL_GPIO_WritePin(AMP_GAIN_GPIO_Port, AMP_GAIN_Pin, GPIO_PIN_SET); // 1 for 24 Volts, 0 for 12 Volts
						}
					}
					auto dt=AllocNewMsg(&msg,CMD_JSON_DATA,sExPins.length()+1);
					//sExPins.copy((char*)dt,sExPins.length());
					std::strcpy((char*)dt,sExPins.c_str());
					CUSBTask::Instance()->SendMessage(&msg, 0, true);
				}
			}
			if((flags & LOGICTASK_QUEUE_FLAG)!=0)
			{
				while(GetMessage(&msg))
				{
					DoMessage(msg);
				}
			}
		}
	}
}


void CLogicTask::DoMessage(STaskMessage msg)
{
	STaskMessage tmsg;
	uint8_t* dt;
	int x;

	switch(msg.msgID)
	{
	case CMD_JSON_DATA:
#ifdef DEBUG
//		std::printf("%d:%s\n", msg.shortParam, (const char*)msg.msgBody);
#endif
		x=mParser.parse((const char*)msg.msgBody);
		CExternalPins::Instance()->command(&mParser, x, LOGICTASK_ADC_BIT);
		doLeds(&mParser);
		vPortFree(msg.msgBody);
		break;
	default:
#ifdef DEBUG
		std::printf("CLogicTask unknown message: %d(%d)\n", msg.msgID, msg.shortParam);
#endif
		break;
	}
}

void CLogicTask::doLeds(CJsonParser* command)
{
	int leds;
	if(command->getObject(1, "leds", leds))
	{
		std::string str;
		if(command->getString(leds, "red", str))
		{
			if(str == "on")
			{
				HAL_GPIO_WritePin(LED1, GPIO_PIN_SET);
			}
			else if(str == "off")
			{
				HAL_GPIO_WritePin(LED1, GPIO_PIN_RESET);
			}
		}
		if(command->getString(leds, "green", str))
		{
			if(str == "on")
			{
				HAL_GPIO_WritePin(LED2, GPIO_PIN_SET);
			}
			else if(str == "off")
			{
				HAL_GPIO_WritePin(LED2, GPIO_PIN_RESET);
			}
		}
	}
}

