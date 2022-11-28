#include "settings.h"
#include <cstdio>

/*!
    \defgroup Algorithms Алгоритмы
    \brief Алгоритмы непривязанные к «железу».

    \defgroup Devices Устройства
    \brief «Драйверы» устройств.

    \defgroup Tasks Задачи
    \brief Задачи FreeRTOS.

    ![Связь между задачами](tasks.jpg)
		Есть 3 задачи со связями в виде звезды:
	1.	CLogicTask. Реализует основную логику работы модуля.
	2.	CUSBTask. Прием/передача по USB CDC.


*/

#include <new>
#include "Algorithms/Debug/CTrace.h"
#include "Algorithms/Debug/CPrintLog.h"
#include "rng.h"

#ifdef UNIT_TESTS
#include "Algorithms/UnitTest/CTesting.h"
#include "Tests/CSimpleTest.h"
#include "Tests/CJsonTest.h"
#include "Tests/CAdcPinsTest.h"
#include "Tests/CExternalPinsTest.h"
#include "Tests/CCANTest.h"
#else
#include "Tasks/CLogicTask.h"
#include "Tasks/CUSBTask.h"
#include "Tasks/CCANTask.h"
#endif

#ifdef DEBUG
CPrintLog prnt;
#endif

extern "C"
{
	void initTasks();
	void startTests();
	void endUSBTransmit();
	void endUSBRecieve(uint8_t* data, uint32_t size);
	void controlUSB(uint8_t cmd, uint8_t* pbuf, uint16_t length);
}

void initTasks()
{
#ifndef UNIT_TESTS
#ifdef DEBUG
	ADDLOG(&prnt);
#endif
	uint32_t seed=portNVIC_SYSTICK_CURRENT_VALUE_REG;
	if(HAL_RNG_GenerateRandomNumber(&hrng,&seed) != HAL_OK)
	{
		PRINT("HAL_RNG_GenerateRandomNumber failed");
	}
	HAL_RNG_DeInit(&hrng);
	std::srand(seed);

	CLogicTask::Instance()->Init(LOGICTASK_NAME,LOGICTASK_STACKSIZE,LOGICTASK_PRIOR,LOGICTASK_LENGTH);
	CUSBTask::Instance()->Init(USBTASK_NAME,USBTASK_STACKSIZE,USBTASK_PRIOR,USBTASK_LENGTH);
	CCANTask::Instance()->Init(CANTASK_NAME,CANTASK_STACKSIZE,CANTASK_PRIOR,CANTASK_LENGTH);

#endif
}

void startTests()
{
#ifdef UNIT_TESTS
#ifdef DEBUG
	ADDLOG(&prnt);
#endif
	HAL_SuspendTick();

//    CTesting::Detailed=false;
    CTesting* root=new CTesting();
	if(root!=NULL)
	{
//		root->Add(new CSimpleTest());
//		root->Add(new CJsonTest());
//		root->Add(new CAdcPinsTest());
//		root->Add(new CExternalPinsTest());
		root->Add(new CCANTest());

		root->Test(0);
		delete root;
	}
#endif
}

void endUSBTransmit()
{
#ifndef UNIT_TESTS
	CUSBTask::Instance()->endTransmit();
#endif
}

void endUSBRecieve(uint8_t* data, uint32_t size)
{
#ifndef UNIT_TESTS
	CUSBTask::Instance()->endRecieve(data, size);
#endif
}

void controlUSB(uint8_t cmd, uint8_t* pbuf, uint16_t length)
{
#ifndef UNIT_TESTS
	CUSBTask::Instance()->control(cmd, pbuf, length);
#endif
}

void* operator new(std::size_t size)
{
	void* res=pvPortMalloc(size);
	if(!res)throw(std::bad_alloc());
	return res;
}

void operator delete(void* ptr) noexcept
{
	if(ptr != nullptr)vPortFree(ptr);
}

void* operator new[](std::size_t size)
{
	void* res=pvPortMalloc(size);
	if(!res)throw(std::bad_alloc());
	return res;
}

void operator delete[](void* ptr) noexcept
{
	if(ptr != nullptr)vPortFree(ptr);
}

void* operator new  ( std::size_t count, const std::nothrow_t& tag )
{
	return pvPortMalloc(count);
}

void* operator new[]( std::size_t count, const std::nothrow_t& tag )
{
	return pvPortMalloc(count);
}
