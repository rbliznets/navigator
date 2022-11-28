/*!
	\file
	\brief Класс для реализации корневого теста.
	\authors Близнец Р.А.
	\version 1.1.0
	\date 28.10.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/
*/

#if !defined CTESTING
#define CTESTING

/*! \addtogroup unit_test

   	@{
*/

#include "main.h"
#include "CUnitTest.h"
#include <vector>


/////////////////////////////////////////////////
// Классы
/////////////////////////////////////////////////

/// <summary>
/// Менеджер автоматизированного модульного тестирования.
/// </summary>
class CTesting : public CUnitTest
{
	protected:
		/// <summary>
		/// Массив тестов.
		/// </summary>
		std::vector<CUnitTest*> tests;

	public:

	    /// <summary>
	    /// Конструктор.
	    /// </summary>
		CTesting(){};
	    /// <summary>
	    /// Деструктор.
	    /// </summary>
	    /// <remarks>Освобождает ресурсы, в том числе и тесты.</remarks>
		virtual ~CTesting();

	    /// <summary>
	    /// Запуск тестов.
	    /// </summary>
	    /// <param name="ind">отступ (количество пробелов) при выводе результатов</param>
	    /// <returns>успешность прохождения тестирования</returns>
		bool Test(uint16_t ind);

	    /// <summary>
	    /// Добавить тест.
	    /// </summary>
	    /// <param name="tst">добавляемый тест</param>
	    /// <example>
		/// CTesting* t=new CTesting(10);
		/// t->Add(new CLDPCTest());
		/// ...
		/// t->Test(0);
		/// delete t;
	    /// </example>
		void Add(CUnitTest* tst){tests.push_back(tst);};
	    
	    /// <summary>
	    /// Имя теста.
	    /// </summary>
	    /// <returns>возвращает название теста</returns>
		virtual const char* GetName(){return "All Tests";}; 
};
/*! @} */

#endif // CTESTING
