
#if !defined CADCPINSTEST_H
#define CADCPINSTEST_H

#include "../Algorithms/UnitTest/CUnitTest.h"
#include "../Devices/CAdcPins.h"

class CAdcPinsTest : public CUnitTest
{
	CAdcPins* dev=nullptr;
protected:
	virtual bool PreTest();
	virtual bool Test();
	virtual bool PostTest(bool prev);
public:
	virtual inline const char* GetName(){return "AdcPins Test";};
};

#endif // CADCPINSTEST_H
