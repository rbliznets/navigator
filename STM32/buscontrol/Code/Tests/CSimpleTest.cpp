#include "CSimpleTest.h"
#include "task.h"

bool CSimpleTest::PreTest()
{
    ASSERT("PreTest");

    return true;
}

bool CSimpleTest::Test()
{
    STARTTIMER();
    vTaskDelay(pdMS_TO_TICKS(2));
    STOPTIMER("Delay");

//    uint8_t* p=(uint8_t*)pvPortMalloc(100);
    uint8_t* p=new uint8_t[16];
//    delete[] p;

	return true;
}


bool CSimpleTest::PostTest(bool prev)
{
    ASSERT("PostTest");
//    PrintMem();
	return true;
}

