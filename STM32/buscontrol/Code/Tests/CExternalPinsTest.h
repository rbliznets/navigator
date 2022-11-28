
#if !defined CEXTERNALPINSTEST_H
#define CEXTERNALPINSTEST_H

#include "../Algorithms/UnitTest/CUnitTest.h"
#include "../Devices/CExternalPins.h"
#include "../Algorithms/DataFlow/CJsonParser.h"

class CExternalPinsTest : public CUnitTest
{
	CJsonParser* pars=nullptr;
	CExternalPins* dev=nullptr;

	const char* str1="{\"ext_pins\":{\"settings\":{\"period\":10000,\"IO1\":{\"type\":\"adc\",\"time\":6,\"sensitivity\":5},\"IO2\":{\"type\":\"in\"},\"IO3\":{\"type\":\"out\",\"value\":1}}}}";
	const char* str2="{\"ext_pins\":{\"pins\":{\"IO3\":0}}}";
	const char* str3="{\"ext_pins\":{\"settings\":{}}}";

protected:
	virtual bool PreTest();
	virtual bool Test();
	virtual bool PostTest(bool prev);
public:
	virtual inline const char* GetName(){return "ExternalPins Test";};
};

#endif // CEXTERNALPINSTEST_H
