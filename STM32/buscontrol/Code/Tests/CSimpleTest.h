
#if !defined CSIMPLETEST_H
#define CSIMPLETEST_H

#include "../Algorithms/UnitTest/CUnitTest.h"

class CSimpleTest : public CUnitTest
{
	protected:
		virtual bool PreTest();
		virtual bool Test();
		virtual bool PostTest(bool prev);
	public:
		virtual inline const char* GetName(){return "Simple Test";};
};

#endif // CSIMPLETEST_H
