/*!
	\file
	\brief Класс для управления выводами внешнего раъъёма.
	\authors Близнец Р.А.
	\version 1.0.0.0
	\date 21.12.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/
*/

#include "CExternalPins.h"
#include "../Algorithms/Debug/CTrace.h"
#include <cstring>

void CExternalPins::stop()
{
	CAdcPins::Instance()->free();
}

void CExternalPins::start(uint8_t xNotifyBit, uint32_t period)
{
	CAdcPins::Instance()->free();
	CAdcPins::Instance()->init(xNotifyBit, period);
}

void CExternalPins::command(CJsonParser* json, int root, uint8_t xNotifyBit)
{
	int ext;
	if(json->getObject(root, "ext_pins", ext))
	{
		int set;
		if(json->getObject(ext, "settings", set))
		{
			int period;
			CAdcPins::Instance()->free();
			CAdcPins::Instance()->clearChannels();
			mAdcSize=4;
			mTemp=0;
			mVp=0;
			mVext=0;
			mVbat=0;

			if(json->getInt(set, "period", period))
			{
				int io;
				if(json->getObject(set, "IO1", io))
				{
					setPin(json, io,EEXTERNAL_IO1);
				}
				else
				{
					setInput(EEXTERNAL_IO1);
					mPins[EEXTERNAL_IO1].type=EEXT_PIN_NONE;
				}
				if(json->getObject(set, "IO2", io))
				{
					setPin(json, io,EEXTERNAL_IO2);
				}
				else
				{
					setInput(EEXTERNAL_IO2);
					mPins[EEXTERNAL_IO2].type=EEXT_PIN_NONE;
				}
				if(json->getObject(set, "IO3", io))
				{
					setPin(json, io,EEXTERNAL_IO3);
				}
				else
				{
					setInput(EEXTERNAL_IO3);
					mPins[EEXTERNAL_IO3].type=EEXT_PIN_NONE;
				}
				if(json->getObject(set, "IO4", io))
				{
					setPin(json, io,EEXTERNAL_IO4);
				}
				else
				{
					setInput(EEXTERNAL_IO4);
					mPins[EEXTERNAL_IO4].type=EEXT_PIN_NONE;
				}
				if(json->getObject(set, "IO5", io))
				{
					setPin(json, io,EEXTERNAL_IO5);
				}
				else
				{
					setInput(EEXTERNAL_IO5);
					mPins[EEXTERNAL_IO5].type=EEXT_PIN_NONE;
				}
				if(json->getObject(set, "IO6", io))
				{
					setPin(json, io,EEXTERNAL_IO6);
				}
				else
				{
					setInput(EEXTERNAL_IO6);
					mPins[EEXTERNAL_IO6].type=EEXT_PIN_NONE;
				}
				if(json->getObject(set, "IO7", io))
				{
					setPin(json, io,EEXTERNAL_IO7);
				}
				else
				{
					setInput(EEXTERNAL_IO7);
					mPins[EEXTERNAL_IO7].type=EEXT_PIN_NONE;
				}
				if(json->getObject(set, "IO8", io))
				{
					setPin(json, io,EEXTERNAL_IO8);
				}
				else
				{
					setInput(EEXTERNAL_IO8);
					mPins[EEXTERNAL_IO8].type=EEXT_PIN_NONE;
				}
				CAdcPins::Instance()->init(xNotifyBit, period);
			}
		}
		if(json->getObject(ext, "pins", set))
		{
			int io;
			if(json->getInt(set, "IO1", io))
			{
				setValue(EEXTERNAL_IO1,io);
			}
			if(json->getInt(set, "IO2", io))
			{
				setValue(EEXTERNAL_IO2,io);
			}
			if(json->getInt(set, "IO3", io))
			{
				setValue(EEXTERNAL_IO3,io);
			}
			if(json->getInt(set, "IO4", io))
			{
				setValue(EEXTERNAL_IO4,io);
			}
			if(json->getInt(set, "IO5", io))
			{
				setValue(EEXTERNAL_IO5,io);
			}
			if(json->getInt(set, "IO6", io))
			{
				setValue(EEXTERNAL_IO6,io);
			}
			if(json->getInt(set, "IO7", io))
			{
				setValue(EEXTERNAL_IO7,io);
			}
			if(json->getInt(set, "IO8", io))
			{
				setValue(EEXTERNAL_IO8,io);
			}
		}
	}
}

void CExternalPins::setValue(EEXTERNAL_PINS pin, int value)
{
	if(mPins[pin].type == EEXT_PIN_OUT)
	{
		switch(pin)
		{
		case EEXTERNAL_IO1:
			HAL_GPIO_WritePin(GPIOE, DIO1_Pin, (GPIO_PinState)value);
			break;
		case EEXTERNAL_IO2:
			HAL_GPIO_WritePin(GPIOE, DIO2_Pin, (GPIO_PinState)value);
			break;
		case EEXTERNAL_IO3:
			HAL_GPIO_WritePin(GPIOE, DIO3_Pin, (GPIO_PinState)value);
			break;
		case EEXTERNAL_IO4:
			HAL_GPIO_WritePin(GPIOE, DIO4_Pin, (GPIO_PinState)value);
			break;
		case EEXTERNAL_IO5:
			HAL_GPIO_WritePin(GPIOE, DIO5_Pin, (GPIO_PinState)value);
			break;
		case EEXTERNAL_IO6:
			HAL_GPIO_WritePin(GPIOE, DIO6_Pin, (GPIO_PinState)value);
			break;
		case EEXTERNAL_IO7:
			HAL_GPIO_WritePin(GPIOE, DIO7_Pin, (GPIO_PinState)value);
			break;
		case EEXTERNAL_IO8:
			HAL_GPIO_WritePin(GPIOE, DIO8_Pin, (GPIO_PinState)value);
			break;
		}
	}
}

int CExternalPins::getValue(EEXTERNAL_PINS pin)
{
	if(mPins[pin].type == EEXT_PIN_IN)
	{
		switch(pin)
		{
		case EEXTERNAL_IO1:
			return HAL_GPIO_ReadPin(GPIOE, DIO1_Pin);
		case EEXTERNAL_IO2:
			return HAL_GPIO_ReadPin(GPIOE, DIO2_Pin);
		case EEXTERNAL_IO3:
			return HAL_GPIO_ReadPin(GPIOE, DIO3_Pin);
		case EEXTERNAL_IO4:
			return HAL_GPIO_ReadPin(GPIOE, DIO4_Pin);
		case EEXTERNAL_IO5:
			return HAL_GPIO_ReadPin(GPIOE, DIO5_Pin);
		case EEXTERNAL_IO6:
			return HAL_GPIO_ReadPin(GPIOE, DIO6_Pin);
		case EEXTERNAL_IO7:
			return HAL_GPIO_ReadPin(GPIOE, DIO7_Pin);
		case EEXTERNAL_IO8:
			return HAL_GPIO_ReadPin(GPIOE, DIO8_Pin);
		}
	}
	return 2;
}

void CExternalPins::setPin(CJsonParser* json, int io, EEXTERNAL_PINS pin)
{
	std::string str;
	if(json->getString(io, "type", str))
	{
		if(str == "adc")
		{
			mPins[pin].sensitivity=1;
			json->getInt(io, "sensitivity", mPins[pin].sensitivity);
			mPins[pin].type=EEXT_PIN_ADC;
			int tp=0;
			uint32_t s;
			json->getInt(io, "type", tp);
			switch(tp)
			{
			case 1:
				s=ADC_SAMPLETIME_6CYCLES_5;
				break;
			case 2:
				s=ADC_SAMPLETIME_12CYCLES_5;
				break;
			case 3:
				s=ADC_SAMPLETIME_24CYCLES_5;
				break;
			case 4:
				s=ADC_SAMPLETIME_47CYCLES_5;
				break;
			case 5:
				s=ADC_SAMPLETIME_92CYCLES_5;
				break;
			case 6:
				s=ADC_SAMPLETIME_247CYCLES_5;
				break;
			case 7:
				s=ADC_SAMPLETIME_640CYCLES_5;
				break;
			default:
				s=ADC_SAMPLETIME_2CYCLES_5;
				break;
			}
			mAdcSize=CAdcPins::Instance()->addChannel(pin, s);
			mPins[pin].index=mAdcSize-1;
			mPins[pin].adc=0;
		}
		else if(str == "in")
		{
			setInput(pin);
			mPins[pin].type=EEXT_PIN_IN;
			mPins[pin].adc=2;
		}
		else if(str == "out")
		{
			mPins[pin].type=EEXT_PIN_OUT;
			int val=0;
			json->getInt(io, "value", val);
			setOutput(pin,val);
		}
	}
}

void CExternalPins::setOutput(EEXTERNAL_PINS pin, int value)
{
	GPIO_InitTypeDef GPIO_InitStruct = {0};
	switch(pin)
	{
	case EEXTERNAL_IO1:
		GPIO_InitStruct.Pin = DIO1_Pin;
		HAL_GPIO_WritePin(GPIOE, DIO1_Pin, (GPIO_PinState)value);
		break;
	case EEXTERNAL_IO2:
		GPIO_InitStruct.Pin = DIO2_Pin;
		HAL_GPIO_WritePin(GPIOE, DIO2_Pin, (GPIO_PinState)value);
		break;
	case EEXTERNAL_IO3:
		GPIO_InitStruct.Pin = DIO3_Pin;
		HAL_GPIO_WritePin(GPIOE, DIO3_Pin, (GPIO_PinState)value);
		break;
	case EEXTERNAL_IO4:
		GPIO_InitStruct.Pin = DIO4_Pin;
		HAL_GPIO_WritePin(GPIOE, DIO4_Pin, (GPIO_PinState)value);
		break;
	case EEXTERNAL_IO5:
		GPIO_InitStruct.Pin = DIO5_Pin;
		HAL_GPIO_WritePin(GPIOE, DIO5_Pin, (GPIO_PinState)value);
		break;
	case EEXTERNAL_IO6:
		GPIO_InitStruct.Pin = DIO6_Pin;
		HAL_GPIO_WritePin(GPIOE, DIO6_Pin, (GPIO_PinState)value);
		break;
	case EEXTERNAL_IO7:
		GPIO_InitStruct.Pin = DIO7_Pin;
		HAL_GPIO_WritePin(GPIOE, DIO7_Pin, (GPIO_PinState)value);
		break;
	case EEXTERNAL_IO8:
		GPIO_InitStruct.Pin = DIO8_Pin;
		HAL_GPIO_WritePin(GPIOE, DIO8_Pin, (GPIO_PinState)value);
		break;
	}
	GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
	GPIO_InitStruct.Pull = GPIO_PULLUP;
	HAL_GPIO_Init(GPIOE, &GPIO_InitStruct);
}

void CExternalPins::setInput(EEXTERNAL_PINS pin)
{
	GPIO_InitTypeDef GPIO_InitStruct = {0};
	switch(pin)
	{
	case EEXTERNAL_IO1:
		GPIO_InitStruct.Pin = DIO1_Pin;
		break;
	case EEXTERNAL_IO2:
		GPIO_InitStruct.Pin = DIO2_Pin;
		break;
	case EEXTERNAL_IO3:
		GPIO_InitStruct.Pin = DIO3_Pin;
		break;
	case EEXTERNAL_IO4:
		GPIO_InitStruct.Pin = DIO4_Pin;
		break;
	case EEXTERNAL_IO5:
		GPIO_InitStruct.Pin = DIO5_Pin;
		break;
	case EEXTERNAL_IO6:
		GPIO_InitStruct.Pin = DIO6_Pin;
		break;
	case EEXTERNAL_IO7:
		GPIO_InitStruct.Pin = DIO7_Pin;
		break;
	case EEXTERNAL_IO8:
		GPIO_InitStruct.Pin = DIO8_Pin;
		break;
	}
	GPIO_InitStruct.Mode = GPIO_MODE_INPUT;
	GPIO_InitStruct.Pull = GPIO_PULLUP;
	HAL_GPIO_Init(GPIOE, &GPIO_InitStruct);
}

#define TS_CAL1 (*((uint16_t*)0x1FFF75A8)) ///< Калибровочные значения для 30 градусов про Vref=3.
#define TS_CAL2 (*((uint16_t*)0x1FFF75CA)) ///< Калибровочные значения для 110 градусов про Vref=3.
#define TS_CAL1_TEMP (30) 				   ///< 30 градусов.
#define TS_CAL2_TEMP (110)				   ///< 110 градусов.

CExternalPins::CExternalPins()
{
	mTempSens=(TS_CAL2-TS_CAL1)/(TS_CAL2_TEMP-TS_CAL1_TEMP);
	for(auto& x:mPins)x.type=EEXT_PIN_NONE;
}

float CExternalPins::getPowerVoltage()
{
	return mVext*0.007075;
}

bool CExternalPins::update(std::string& msg)
{
	bool res=false;
	int x;
	float f;
	uint16_t* data=CAdcPins::Instance()->getData();
	if(abs((int)mTemp - data[0]) >= mTempSens)
	{
		mTemp = data[0];
		f=(((float)mTemp-TS_CAL1)*(TS_CAL2_TEMP-TS_CAL1_TEMP)*0.966667)/(TS_CAL2-TS_CAL1)+30;

		if(!res)
		{
			res=true;
			msg="{\"ext_pins\":{\"temperature\":{\"value\":"+std::to_string(f)+",\"adc\":"+std::to_string(mTemp)+"}";
		}
	}

	if(abs((int)mVp - data[1]) >= 47)
	{
		mVp = data[1];
		f=mVp*0.002124;

		if(!res)
		{
			res=true;
			msg="{\"ext_pins\":{\"Vp\":{\"value\":"+std::to_string(f)+",\"adc\":"+std::to_string(mVp)+"}";
		}
		else
		{
			msg+=",\"Vp\":{\"value\":"+std::to_string(f)+",\"adc\":"+std::to_string(mVp)+"}";
		}
	}

	if(abs((int)mVext - data[2]) >= 14)
	{
		mVext = data[2];
		f=mVext*0.007075;

		if(!res)
		{
			res=true;
			msg="{\"ext_pins\":{\"Vext\":{\"value\":"+std::to_string(f)+",\"adc\":"+std::to_string(mVext)+"}";
		}
		else
		{
			msg+=",\"Vext\":{\"value\":"+std::to_string(f)+",\"adc\":"+std::to_string(mVext)+"}";
		}
	}

	if(abs((int)mVbat - data[3]) >= 14)
	{
		mVbat = data[3];
		f=mVbat*0.007075;

		if(!res)
		{
			res=true;
			msg="{\"ext_pins\":{\"Vbat\":{\"value\":"+std::to_string(f)+",\"adc\":"+std::to_string(mVbat)+"}";
		}
		else
		{
			msg+=",\"Vbat\":{\"value\":"+std::to_string(f)+",\"adc\":"+std::to_string(mVbat)+"}";
		}
	}

	for(int i=0; i < 8; i++)
	{
		if(mPins[i].type == EEXT_PIN_ADC)
		{
			if(abs((int)mPins[i].adc - data[mPins[i].index]) >= mPins[i].sensitivity)
			{
				mPins[i].adc = data[mPins[i].index];
				f=mPins[i].adc*0.00708;

				if(!res)
				{
					res=true;
					msg="{\"ext_pins\":{\"VIO"+std::to_string(i+1)+"\":{\"value\":"+std::to_string(f)+",\"adc\":"+std::to_string(mPins[i].adc)+"}";
				}
				else
				{
					msg+=",\"VIO"+std::to_string(i+1)+"\":{\"value\":"+std::to_string(f)+",\"adc\":"+std::to_string(mPins[i].adc)+"}";
				}
			}
		}
		else if(mPins[i].type == EEXT_PIN_IN)
		{
			x=getValue((EEXTERNAL_PINS)i);
			if(mPins[i].adc != x)
			{
				mPins[i].adc = x;
				if(!res)
				{
					res=true;
					msg="{\"ext_pins\":{\"IO"+std::to_string(i+1)+"\":"+std::to_string(mPins[i].adc);
				}
				else
				{
					msg+=",\"IO"+std::to_string(i+1)+"\":"+std::to_string(mPins[i].adc);
				}
			}
		}
	}

	if(res)
	{
		msg+="}}";
	}
	return res;
}
