
#if !defined CJSONTEST_H
#define Json

#include "../Algorithms/UnitTest/CUnitTest.h"
#include "../Algorithms/DataFlow/CJsonParser.h"

class CJsonTest : public CUnitTest
{
protected:
	const char* str1="{\"str\":\"ups\",\"nl\":null,\"child\":{\"param1\":2,\"param2\":true,\"ar\":[1,2,3,4,5]}}";
	const int chk[5]={1,2,3,4,5};

	CJsonParser* pars=nullptr;

	virtual bool PreTest();
	virtual bool Test();
	virtual bool PostTest(bool prev);
public:
	virtual inline const char* GetName(){return "Json Test";};
};

#endif // CJSONTEST_H
