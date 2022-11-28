#include "CExternalPinsTest.h"
#include "task.h"
#include "../Algorithms/Debug/CTrace.h"

bool CExternalPinsTest::PreTest()
{
	pars=new CJsonParser();
	ASSERTBOOL(pars != nullptr,"Create CJsonParser");
	dev=CExternalPins::Instance();
	ASSERTBOOL(dev!=nullptr,"Create");

    return true;
}

bool CExternalPinsTest::Test()
{
	std::string json;
	int root=pars->parse(str1);
	dev->command(pars, root, 1);
    STARTTIMER();
	uint32_t flag=0;
	for(int i=0; i < 100; i++)
	{
		xTaskNotifyWait(0,(1 << 1),&flag,100);
		if(dev->update(json))
		{
			TRACE(json.c_str(),0,false);
		}
	}
    STOPTIMER("1 sec");

    STARTTIMER();
    root=pars->parse(str2);
	dev->command(pars, root, 1);
	for(int i=0; i < 100; i++)
	{
		xTaskNotifyWait(0,(1 << 1),&flag,100);
		if(dev->update(json))
		{
			TRACE(json.c_str(),0,false);
		}
	}
    STOPTIMER("1 sec");

//	for(;;)
//	{
//		xTaskNotifyWait(0,(1 << 1),&flag,100);
//		if(dev->update(json))
//		{
//			TRACE(json.c_str(),0,false);
//		}
//	}

    root=pars->parse(str3);
	dev->command(pars, root, 1);
	return true;
}


bool CExternalPinsTest::PostTest(bool prev)
{
	if(dev != nullptr)
	{
		dev->stop();
	    ASSERT("Delete");
	}
	if(pars != nullptr)
	{
		delete pars;
		ASSERT("Delete CJsonParser");
	}
	return true;
}

