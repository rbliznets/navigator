/*!
	\file
	\brief Базовый класс для модульного тестирования.
	\authors Близнец Р.А.
	\version 1.1.0
	\date 28.10.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/
*/

#if !defined CUNITTEST
#define CUNITTEST

/*! \defgroup unit_test Модульное тестирование
  	\ingroup Algorithms
  	\brief Классы для модульного тестирования.

   	@{
*/

/////////////////////////////////////////////////
// 	Раздел #include
////////////////////////////////////////////////
#include "settings.h"
#include <cstdio>
#include "data.h"

/////////////////////////////////////////////////
// 	Раздел #define
////////////////////////////////////////////////

/// <summary>
/// Подтверждение прохождения шага тестирования.
/// </summary>
#define ASSERT(S) AssertBool(true,S)
/// <summary>
/// Ошибка прохождения шага тестирования.
/// </summary>
#define FAILED(S) AssertBool(false,S)
/// <summary>
/// Подтверждение прохождения шага тестирования по условию.
/// </summary>
#define ASSERTBOOL(V,S) if( !AssertBool(V,S) )return false
/// <summary>
/// Подтверждение прохождения шага тестирования при равенстве матриц типа int16.
/// </summary>
#define ASSERTEQARRAYINT16(V1,V2,SZ,S) if( !AssertEqArray(V1,V2,SZ,S) )return false
/// <summary>
/// Подтверждение прохождения шага тестирования при равенстве матриц типа uint16.
/// </summary>
#define ASSERTEQARRAYUINT16(V1,V2,SZ,S) if( !AssertEqArray((int16_t*)V1,(int16_t*)V2,SZ,S) )return false
/// <summary>
/// Подтверждение прохождения шага тестирования при равенстве матриц типа char.
/// </summary>
#define ASSERTEQARRAYCHAR(V1,V2,SZ,S) if( !AssertEqArray(V1,V2,SZ,S) )return false
/// <summary>
/// Подтверждение прохождения шага тестирования при равенстве матриц типа uint8_t.
/// </summary>
#define ASSERTEQARRAYUINT8(V1,V2,SZ,S) if( !AssertEqArray((char*)V1,(char*)V2,SZ,S) )return false
/// <summary>
/// Подтверждение прохождения шага тестирования при равенстве матриц типа int32.
/// </summary>
#define ASSERTEQARRAYINT32(V1,V2,SZ,S) if( !AssertEqArray((int16_t*)V1,(int16_t*)V2,2*SZ,S) )return false
/// <summary>
/// Подтверждение прохождения шага тестирования при равенстве матриц типа uint32.
/// </summary>
#define ASSERTEQARRAYUINT32(V1,V2,SZ,S) if( !AssertEqArray((int16_t*)V1,(int16_t*)V2,2*SZ,S) )return false

/// <summary>
/// Начало отчета таймера.
/// </summary>
#define STARTTIMER() StartTimer();
/// <summary>
/// Вывести текущее значение таймера со сообщением.
/// </summary>
#define STOPTIMER(S) StopTimer(S);


/////////////////////////////////////////////////
// Классы
/////////////////////////////////////////////////

/// <summary>
/// Базовый класс для модульного тестирования.
/// </summary>
class CUnitTest 
{
	protected:
		/// <summary>
		/// Время запуска теста.
		/// </summary>
		STimeSpan m_time;

		/// <summary>
		/// Отступ (строка пробелов).
		/// </summary>
		char indent[17];

	    /// <summary>
	    /// Проверка на логическую функцию.
	    /// </summary>
	    /// <param name="value">Булево значение</param>
	    /// <param name="message">Описание проверки</param>
	    /// <returns>успешность проверки</returns>
		bool AssertBool(bool value, const char* message);
		
	    /// <summary>
	    /// Проверка на индентичность массивов.
	    /// </summary>
	    /// <param name="ar1">первый массив</param>
	    /// <param name="ar2">второй массив</param>
	    /// <param name="size">размер проверяемых массивов</param>
	    /// <param name="message">Описание проверки</param>
	    /// <returns>успешность проверки</returns>
		bool AssertEqArray(int16_t* ar1, int16_t* ar2, uint16_t size, const char* message);
		
	    /// <summary>
	    /// Проверка на индентичность массивов.
	    /// </summary>
	    /// <param name="ar1">первый массив</param>
	    /// <param name="ar2">второй массив</param>
	    /// <param name="size">размер проверяемых массивов</param>
	    /// <param name="message">Описание проверки</param>
	    /// <returns>успешность проверки</returns>
		bool AssertEqArray(char* ar1, char* ar2, uint16_t size, const char* message);

	    /// <summary>
	    /// Различие массивов.
	    /// </summary>
	    /// <param name="ar1">первый массив</param>
	    /// <param name="ar2">второй массив</param>
	    /// <param name="size">размер проверяемых массивов</param>
	    /// <returns>количество неодинаковых бит</returns>
		int16_t GetEqArray(uint16_t* ar1, uint16_t* ar2, uint16_t size);

	    /// <summary>
	    /// Вывод положительного результата прохождения шага теста.
	    /// </summary>
	    /// <param name="message">описание шага теста</param>
		inline void PrintPass(const char* message){if(Detailed)std::printf("%s->%s PASS\n",indent,message);};

	    /// <summary>
	    /// Вывод отрицательного результата прохождения шага теста.
	    /// </summary>
	    /// <param name="message">описание шага теста</param>
		inline void PrintFail(const char* message){if(Detailed)std::printf("%s->%s FAIL\n",indent,message);};
	    
	    /// <summary>
	    /// Инициализация теста.
	    /// </summary>
	    /// <returns>успешность инициализации</returns>
		virtual bool PreTest(){return true;}; 
	    
	    /// <summary>
	    /// Запуск теста.
	    /// </summary>
	    /// <returns>успешность теста</returns>
		virtual bool Test(){return false;}; 
	    
	    /// <summary>
	    /// Закрытие теста.
	    /// </summary>
	    /// <param name="prev">флаг успещного прохождения теста</param>
	    /// <returns>успешность зыкрытия теста</returns>
		virtual bool PostTest(bool prev){return true;};

	    /// <summary>
	    /// Сообщение о текущем состоянии кучи.
	    /// </summary>
		void PrintMem(); 

	    /// <summary>
	    /// Информация о состоянии кучи перед началом теста.
	    /// </summary>
		size_t startMem;
	    /// <summary>
	    /// Считывание информации о состоянии кучи перед началом теста.
	    /// </summary>
		void StartMem();
	    /// <summary>
	    /// Считывание информации о состоянии кучи после теста.
	    /// </summary>
	    /// <remarks>
	    /// При наличии разницы с состоянием перед тестом выводит сообщение.
	    /// </remarks>
		void StopMem();

	    /// <summary>
	    /// Начало отчета таймера.
	    /// </summary>
		void StartTimer();
	    /// <summary>
	    /// Текущее значение таймера.
	    /// </summary>
	    /// <returns>Текущее значение таймера</returns>
		uint64_t GetTimer();
	    /// <summary>
	    /// Вывести текущее значение таймера со сообщением.
	    /// </summary>
	    /// <param name="message">сообщение</param>
		void StopTimer(const char* message);

	public:
	    /// <summary>
	    /// Режим вывода подробностей при прохождении теста.
	    /// </summary>
		static bool Detailed;

	    /// <summary>
	    /// Частота процессора в МГц.
	    /// </summary>
		static int32_t CPUFrequency;

	    /// <summary>
	    /// Запуск теста.
	    /// </summary>
	    /// <param name="ind">отступ (количество пробелов) при выводе результатов</param>
	    /// <returns>успешность теста</returns>
		virtual bool Test(uint16_t ind);
	    
	    /// <summary>
	    /// Имя теста.
	    /// </summary>
	    /// <returns>возвращает название теста</returns>
		virtual const char* GetName(){return "Abstract Test";}; 
};
/*! @} */

#endif // CUNITTEST
