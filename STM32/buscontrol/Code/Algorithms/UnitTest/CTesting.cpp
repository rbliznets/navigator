/*!
	\file
	\brief Класс для реализации корневого теста.
	\authors Близнец Р.А.
	\version 1.1.0
	\date 28.10.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/
*/

#include "CTesting.h"

CTesting::~CTesting()
{
	for(auto x: tests)
	{
		delete x;
	}

	//PrintMem();
}

bool CTesting::Test(uint16_t ind)
{
	int16_t i;
	for(i=0;i<ind;i++)
	{
		indent[i]=' ';
	}
	indent[i]=0;

	int16_t k=0;
	std::printf("%s== Begin %s ==\n",indent,GetName());

	for(auto x: tests)
	{
		StartTimer();
		if(x->Test(ind+1))
		{
			k++;
			std::printf("%s %s PASS (%d)\n",indent,x->GetName(),GetTimer());
		}
		else
		{
			std::printf("%s %s FAIL\n",indent,x->GetName());
		}

		if(Detailed)
		{
			std::printf("\n");
		}
		
	}

	std::printf("%s== Pass:%d. Fail:%d. ==\n",indent,k,tests.size()-k);
	
	return (k==tests.size());
}
 
