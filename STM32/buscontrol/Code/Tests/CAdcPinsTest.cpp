#include "CAdcPinsTest.h"
#include "task.h"
#include "../Algorithms/Debug/CTrace.h"

bool CAdcPinsTest::PreTest()
{
	dev=CAdcPins::Instance();
	ASSERTBOOL(dev!=nullptr,"Create");

    return true;
}

bool CAdcPinsTest::Test()
{
    STARTTIMER();
    dev->addChannel(EEXTERNAL_IO1, ADC_SAMPLETIME_247CYCLES_5);
	dev->init(1,15000);
	uint32_t flag=0;
	for(int i=0; i < 100; i++)
	{
		xTaskNotifyWait(0,(1 << 1),&flag,100);
	}
    STOPTIMER("1.5 sec");
	TRACEDATA("ADC",dev->getData(),dev->getSize(),false);
    STARTTIMER();
    dev->addChannel(EEXTERNAL_IO2, ADC_SAMPLETIME_247CYCLES_5);
	for(int i=0; i < 10; i++)
	{
		xTaskNotifyWait(0,(1 << 1),&flag,100);
	}
    STOPTIMER("150 msec");
	TRACEDATA("ADC",dev->getData(),dev->getSize(),false);
    STARTTIMER();
    dev->clearChannels();
	for(int i=0; i < 10; i++)
	{
		xTaskNotifyWait(0,(1 << 1),&flag,100);
	}
    STOPTIMER("150 msec");
	TRACEDATA("ADC",dev->getData(),dev->getSize(),false);
	return true;
}


bool CAdcPinsTest::PostTest(bool prev)
{
	if(dev != nullptr)
	{
		dev->free();
	    ASSERT("Delete");
	}
	return true;
}

