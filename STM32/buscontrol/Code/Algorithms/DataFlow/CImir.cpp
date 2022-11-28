/*!
	\file
	\brief Базовый класс для реализации задачи FreeRTOS.
	\authors Близнец Р.А.
	\version 0.0.0.1
	\date 28.10.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/
*/

#include "CImir.h"
#include <cstdio>
#include <cstring>

CImir::CImir(uint8_t* data, uint16_t size, uint16_t add_size, bool copy)
{
	m_dataSize=size+add_size;
	m_dataEnd=size;
	if(copy)
	{
		m_data=(uint8_t*)pvPortMalloc(m_dataSize);
		std::memcpy(m_data,data,m_dataSize);
	}
	else
	{
		m_data=data;
	}
}

CImir::CImir(uint16_t add_size)
{
	m_dataEnd=0;
	m_dataSize=add_size;
	m_data=(uint8_t*)pvPortMalloc(m_dataSize);
}

CImir::~CImir()
{
	vPortFree(m_data);
}

uint16_t CImir::Find(const char* name)
{
	uint16_t ind=0;
	while(ind < m_dataEnd)
	{
		if(std::strcmp((const char*)&m_data[ind],name) == 0)
		{
			return (std::strlen((const char*)&m_data[ind])+ind+1);
		}
		else
		{
			ind+=(std::strlen((const char*)&m_data[ind])+1);
			switch(m_data[ind])
			{
			case 0:
				ind+=(std::strlen((const char*)&m_data[ind+1])+2);
				break;
			case 1:
				ind+=2;
				break;
			case 2:
				ind+=3;
				break;
			case 3:
			case 4:
				ind+=5;
				break;
			case 254:
				ind+=5+m_data[ind + 1]+m_data[ind + 2] * 256 + m_data[ind + 3] * 256 * 256 + m_data[ind + 4] * 256 * 256 * 256;
				break;
			case 255:
				ind+=1;
				break;
			default:
				return m_dataEnd;
			}
		}
	}
	return m_dataEnd;
}

bool CImir::GetValue(const char* name)
{
	uint16_t ind=Find(name);
	if(ind >= m_dataEnd)
	{
		return false;
	}
	if(m_data[ind] != 255)
	{
		return false;
	}
	return true;
}

bool CImir::GetValue(const char* name, const char*& value)
{
	uint16_t ind=Find(name);
	if(ind >= m_dataEnd)
	{
		return false;
	}
	if(m_data[ind] != 0)
	{
		return false;
	}
	value=(const char*)&m_data[ind+1];
	return true;
}

bool CImir::GetValue(const char* name, uint8_t& value)
{
	uint16_t ind=Find(name);
	if(ind >= m_dataEnd)
	{
		return false;
	}
	if(m_data[ind] != 1)
	{
		return false;
	}
	value=m_data[ind+1];
	return true;
}

bool CImir::GetValue(const char* name, uint16_t& value)
{
	uint16_t ind=Find(name);
	if(ind >= m_dataEnd)
	{
		return false;
	}
	if(m_data[ind] != 2)
	{
		return false;
	}
	value=m_data[ind+1]+m_data[ind+2]*256;
	return true;
}

bool CImir::GetValue(const char* name, uint32_t& value)
{
	uint16_t ind=Find(name);
	if(ind >= m_dataEnd)
	{
		return false;
	}
	if(m_data[ind] != 3)
	{
		return false;
	}
	value=m_data[ind+1]+m_data[ind+2]*256+m_data[ind+3]*256*256+m_data[ind+4]*256*256*256;
	return true;
}

bool CImir::GetValue(const char* name, float& value)
{
	uint16_t ind=Find(name);
	if(ind >= m_dataEnd)
	{
		return false;
	}
	if(m_data[ind] != 4)
	{
		return false;
	}
	std::memcpy(&value,&m_data[ind+1],4);
	return true;
}

bool CImir::GetValue(const char* name, const uint8_t*& value, uint32_t& size)
{
	uint16_t ind=Find(name);
	if(ind >= m_dataEnd)
	{
		return false;
	}
	if(m_data[ind] != 254)
	{
		return false;
	}
	size=m_data[ind+1]+m_data[ind+2]*256+m_data[ind+3]*256*256+m_data[ind+4]*256*256*256;
	value=&m_data[ind+5];
	return true;
}

void CImir::AddSpace(uint32_t size)
{
	size+=m_dataEnd;
	if(size > m_dataSize)
	{
		if((size-m_dataSize) < 128)
		{
			m_dataSize+=128;
		}
		else
		{
			m_dataSize=size;
		}
		uint8_t* data=(uint8_t*)pvPortMalloc(m_dataSize);
		std::memcpy(data, m_data, m_dataEnd);
		vPortFree(m_data);
		m_data=data;
	}
}

void CImir::Trim()
{
	if(m_dataEnd < m_dataSize)
	{
		uint8_t* data=(uint8_t*)pvPortMalloc(m_dataEnd);
		std::memcpy(data, m_data, m_dataEnd);
		vPortFree(m_data);
		m_data=data;
	}
}

void CImir::AddValue(const char* name)
{
	uint32_t sz=std::strlen(name)+1;
	AddSpace(sz+1);
	std::strcpy((char*)&m_data[m_dataEnd],name);
	m_dataEnd+=sz;
	m_data[m_dataEnd]=255;
	m_dataEnd++;
}

void CImir::AddValue(const char* name, const char* value)
{
	uint32_t sz=std::strlen(name)+1;
	uint32_t sz2=std::strlen(value)+1;
	AddSpace(sz+sz2+1);
	std::strcpy((char*)&m_data[m_dataEnd],name);
	m_dataEnd+=sz;
	m_data[m_dataEnd]=0;
	m_dataEnd++;
	std::strcpy((char*)&m_data[m_dataEnd],value);
	m_dataEnd+=sz2;
}

void CImir::AddValue(const char* name, uint8_t value)
{
	uint32_t sz=std::strlen(name)+1;
	AddSpace(sz+2);
	std::strcpy((char*)&m_data[m_dataEnd],name);
	m_dataEnd+=sz;
	m_data[m_dataEnd]=1;
	m_dataEnd++;
	m_data[m_dataEnd]=value;
	m_dataEnd++;
}

void CImir::AddValue(const char* name, uint16_t value)
{
	uint32_t sz=std::strlen(name)+1;
	AddSpace(sz+3);
	std::strcpy((char*)&m_data[m_dataEnd],name);
	m_dataEnd+=sz;
	m_data[m_dataEnd]=2;
	m_dataEnd++;
	m_data[m_dataEnd]=(uint8_t)value;
	m_dataEnd++;
	m_data[m_dataEnd]=(uint8_t)(value >> 8);
	m_dataEnd++;
}

void CImir::AddValue(const char* name, uint32_t value)
{
	uint32_t sz=std::strlen(name)+1;
	AddSpace(sz+5);
	std::strcpy((char*)&m_data[m_dataEnd],name);
	m_dataEnd+=sz;
	m_data[m_dataEnd]=3;
	m_dataEnd++;
	m_data[m_dataEnd]=(uint8_t)value;
	m_dataEnd++;
	m_data[m_dataEnd]=(uint8_t)(value >> 8);
	m_dataEnd++;
	m_data[m_dataEnd]=(uint8_t)(value >> 16);
	m_dataEnd++;
	m_data[m_dataEnd]=(uint8_t)(value >> 24);
	m_dataEnd++;
}

void CImir::AddValue(const char* name, float value)
{
	uint32_t sz=std::strlen(name)+1;
	AddSpace(sz+5);
	std::strcpy((char*)&m_data[m_dataEnd],name);
	m_dataEnd+=sz;
	m_data[m_dataEnd]=4;
	m_dataEnd++;
	std::memcpy(&m_data[m_dataEnd],&value,4);
	m_dataEnd+=4;
}

void CImir::AddValue(const char* name, const uint8_t* value, uint32_t size)
{
	uint32_t sz=std::strlen(name)+1;
	AddSpace(sz+5+size);
	std::strcpy((char*)&m_data[m_dataEnd],name);
	m_dataEnd+=sz;
	m_data[m_dataEnd]=254;
	m_dataEnd++;
	m_data[m_dataEnd]=(uint8_t)size;
	m_dataEnd++;
	m_data[m_dataEnd]=(uint8_t)(size >> 8);
	m_dataEnd++;
	m_data[m_dataEnd]=(uint8_t)(size >> 16);
	m_dataEnd++;
	m_data[m_dataEnd]=(uint8_t)(size >> 24);
	m_dataEnd++;
	std::memcpy(&m_data[m_dataEnd],value,size);
	m_dataEnd+=size;
}

void CImir::AddStruct(const char* name, uint32_t count)
{
	uint32_t sz=std::strlen(name)+1;
	AddSpace(sz+5);
	std::strcpy((char*)&m_data[m_dataEnd],name);
	m_dataEnd+=sz;
	m_data[m_dataEnd]=101;
	m_dataEnd++;
	std::memcpy(&m_data[m_dataEnd],&count,4);
	m_dataEnd+=4;
}

void CImir::AddList(const char* name, uint32_t count)
{
	uint32_t sz=std::strlen(name)+1;
	AddSpace(sz+5);
	std::strcpy((char*)&m_data[m_dataEnd],name);
	m_dataEnd+=sz;
	m_data[m_dataEnd]=100;
	m_dataEnd++;
	std::memcpy(&m_data[m_dataEnd],&count,4);
	m_dataEnd+=4;
}

