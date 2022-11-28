
#if !defined CCANTEST_H
#define CCANTEST_H

#include "../Algorithms/UnitTest/CUnitTest.h"
#include "../Devices/CCAN.h"

class CCANTest : public CUnitTest
{
	CCAN* dev=nullptr;
protected:
	virtual bool PreTest();
	virtual bool Test();
	virtual bool PostTest(bool prev);
public:
	virtual inline const char* GetName(){return "CCAN Test";};
};

#endif // CCANTEST_H
