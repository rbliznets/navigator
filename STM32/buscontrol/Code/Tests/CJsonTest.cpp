#include "CJsonTest.h"
#include <cstring>

bool CJsonTest::PreTest()
{
	pars=new CJsonParser();
	ASSERTBOOL(pars != nullptr,"Create CJsonParser");
    return true;
}

bool CJsonTest::Test()
{
	int t1=pars->parse(str1);
	ASSERTBOOL(t1 == 1,"parse");

	std::string str;
	ASSERTBOOL(pars->getString(t1, "str", str),"getString");
	ASSERTBOOL(str == "ups","getString value");
	ASSERTBOOL(!pars->getString(t1, "str5", str),"not getString");

	ASSERTBOOL(pars->getField(t1, "nl"),"getField");

	int t2;
	ASSERTBOOL(pars->getObject(t1, "child", t2),"getObject");

	int x;
	ASSERTBOOL(pars->getInt(t2, "param1", x),"getInt");
	ASSERTBOOL(x == 2,"getInt value");

	bool b=false;
	ASSERTBOOL(pars->getBool(t2, "param2", b),"getBool");
	ASSERTBOOL(b,"getBool value");

	int* data;
	int size;
	ASSERTBOOL(pars->getArrayInt(t2, "ar", data, size),"getArrayInt");
	ASSERTBOOL(size == 5,"getArrayInt size");
	ASSERTEQARRAYUINT8(data,chk,5,"getArrayInt data");
	delete[] data;

	return true;
}


bool CJsonTest::PostTest(bool prev)
{
	if(pars != nullptr)
	{
		delete pars;
		ASSERT("Delete CJsonParser");
	}
	return true;
}

