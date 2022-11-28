#include "CCANTest.h"
#include "task.h"
#include "../Algorithms/Debug/CTrace.h"

bool CCANTest::PreTest()
{
	dev=CCAN::Instance();
	ASSERTBOOL(dev!=nullptr,"Create");

    return true;
}

bool CCANTest::Test()
{
	dev->init(&hfdcan1,1);
	uint32_t flag=0;
	FDCAN_RxHeaderTypeDef* rxHeader;
	uint8_t* data;
	xTaskNotifyWait(0,(1 << 1),&flag,portMAX_DELAY);
	data=dev->getData(rxHeader);
	if(data != nullptr)
	{
		int z=(rxHeader->DataLength >> 16);
		TRACEDATA("Rx",data,z,false);
	}
	dev->sendData(32, data, 1);
	vTaskDelay(1);
	return true;
}


bool CCANTest::PostTest(bool prev)
{
	if(dev != nullptr)
	{
		dev->free();
	    ASSERT("Delete");
	}
	return true;
}

