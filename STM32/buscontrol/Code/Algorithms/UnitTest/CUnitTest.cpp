/*!
	\file
	\brief Базовый класс для модульного тестирования.
	\authors Близнец Р.А.
	\version 1.1.0
	\date 28.10.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/
*/

#include "CUnitTest.h"
#include "task.h"
#include <cstdio>

bool CUnitTest::Detailed=true;
int32_t CUnitTest::CPUFrequency=configCPU_CLOCK_HZ/1000000;

bool CUnitTest::Test(uint16_t ind)
{
	m_time.SysTimerCount=0;
	m_time.Ticks=0;

	int16_t i;
	for(i=0;i<ind;i++)
	{
		indent[i]=' ';
	}
	indent[i]=0;

	bool res=false;

	StartMem();
	res = PreTest();
	if(res)
	{
		res = Test();
	}
	res&=PostTest(res);

	StopMem();

	return res;
} 

bool CUnitTest::AssertBool(bool value, const char* message)
{
	if(value)
	{
		PrintPass(message);
		return true;
	}
	else
	{
		PrintFail(message);
		return false;
	}
}

bool CUnitTest::AssertEqArray(int16_t* ar1, int16_t* ar2, uint16_t size, const char* message)
{
	int16_t i;
	for(i=0;i<size;i++)
	{
		if(ar1[i]!=ar2[i])
		{
			break;
		}
	}

	if(i==size)
	{
		PrintPass(message);
		return true;
	}
	else
	{
		PrintFail(message);
		return false;
	}
}

bool CUnitTest::AssertEqArray(char* ar1, char* ar2, uint16_t size, const char* message)
{
	int16_t i;
	for(i=0;i<size;i++)
	{
		if(ar1[i]!=ar2[i])
		{
			break;
		}
	}

	if(i==size)
	{
		PrintPass(message);
		return true;
	}
	else
	{
		PrintFail(message);
		return false;
	}
}

int16_t CUnitTest::GetEqArray(uint16_t* ar1, uint16_t* ar2, uint16_t size)
{
	int16_t i;
	int16_t res=0;
	for(i=0;i<size;i++)
	{
		if(ar1[i]!=ar2[i])
		{
			uint16_t k=0x8000;
			for(int16_t j=0;j<16;j++)
			{
				if((ar1[i]&k)!=(ar2[i]&k))
				{
					res++;
				}
				k=k>>1;
			}
		}
	}
	return res;
}

void CUnitTest::PrintMem()
{
    std::printf("totalSize:%d\n",configTOTAL_HEAP_SIZE);
    std::printf("totalFreeSize:%d\n",xPortGetFreeHeapSize());
    std::printf("minimumFreeSize:%d\n",xPortGetMinimumEverFreeHeapSize());
    std::printf("stackUsedSize:%lu\n",uxTaskGetStackHighWaterMark( xTaskGetCurrentTaskHandle() ));
} 

void CUnitTest::StartMem()
{
//	if((uxTaskGetStackHighWaterMark( mTaskHandle )<64) || (xPortGetFreeHeapSize()<256))
//		std::printf("CLogicTask stack %d, mem %d:%d\n",uxTaskGetStackHighWaterMark( mTaskHandle ),xPortGetFreeHeapSize(),xPortGetMinimumEverFreeHeapSize());

    startMem=xPortGetFreeHeapSize();
} 

void CUnitTest::StopMem()
{
	size_t mem=xPortGetFreeHeapSize();
	if(mem != startMem)
	{
		std::printf("Start:%d,%d\n",startMem, configTOTAL_HEAP_SIZE);
		std::printf("Stop:%d,%d\n", mem, configTOTAL_HEAP_SIZE);
		std::printf("Leak:%d\n",startMem-mem);
	    std::printf("minimumFreeSize:%d\n",xPortGetMinimumEverFreeHeapSize());
	}
}

void CUnitTest::StartTimer()
{
#if (configUSE_TICKLESS_IDLE == 0)
	portENTER_CRITICAL();
	m_time.SysTimerCount=portNVIC_SYSTICK_CURRENT_VALUE_REG;
	m_time.Ticks=xTaskGetTickCount();
	portEXIT_CRITICAL();
#endif
}

uint64_t CUnitTest::GetTimer()
{
	uint64_t res=0;
#if (configUSE_TICKLESS_IDLE == 0)
	STimeSpan time;

	portENTER_CRITICAL();
	time.SysTimerCount=portNVIC_SYSTICK_CURRENT_VALUE_REG;
	time.Ticks=xTaskGetTickCount();
	portEXIT_CRITICAL();

	if(time.Ticks > m_time.Ticks)
	{
		res=(time.Ticks-m_time.Ticks-1)*((1000/configTICK_RATE_HZ)*1000);
		res+=((configCPU_CLOCK_HZ/configTICK_RATE_HZ)-time.SysTimerCount+m_time.SysTimerCount)/(configCPU_CLOCK_HZ/1000000);
	}
	else if(time.Ticks == m_time.Ticks)
	{
		if(m_time.SysTimerCount > time.SysTimerCount)
		{
			res=(m_time.SysTimerCount-time.SysTimerCount)/(configCPU_CLOCK_HZ/1000000);
		}
		else
		{
			res=((configCPU_CLOCK_HZ/configTICK_RATE_HZ)-time.SysTimerCount+m_time.SysTimerCount)/(configCPU_CLOCK_HZ/1000000);
		}
	}
	else
	{
		res=((0x100000000L-m_time.Ticks)+time.Ticks-1)*(1000/configTICK_RATE_HZ)*1000;
		res+=((configCPU_CLOCK_HZ/configTICK_RATE_HZ)-time.SysTimerCount+m_time.SysTimerCount)/(configCPU_CLOCK_HZ/1000000);
	}
#endif
	return res;
}

void CUnitTest::StopTimer(const char* message)
{
	if(!Detailed)return;

	uint64_t res=GetTimer();
	std::printf("%s->%s",indent,message);

	uint32_t x=res/1000;
	std::printf(":%lu.",x);
	x=res-(x*1000L);
	if(x>99)std::printf("%lu msec\n",x);
	else if(x>9)std::printf("0%lu msec\n",x);
	else std::printf("00%lu msec\n",x);
}
