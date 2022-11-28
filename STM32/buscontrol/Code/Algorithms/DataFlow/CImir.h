/*!
	\file
	\brief Базовый класс для реализации задачи FreeRTOS.
	\authors Близнец Р.А.
	\version 0.0.0.1
	\date 28.10.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/
*/

#if !defined CIMIR_H
#define CIMIR_H

/*! \defgroup imir Imir
  	\ingroup Algorithms
  	\brief Структура именованных данных.

   	@{
*/

#include "settings.h"

/// Класс для реализации структуры именованных данных.
/*
 * Реализация без списков.
 */
class CImir
{
protected:
	uint8_t* m_data; 	 ///< Массив данных под структуру.
	uint16_t m_dataSize; ///< Размер массива данных под структуру.
	uint16_t m_dataEnd;  ///< Размер структуры.

	/// Найти поле.
	/*!
	  \param[in] name Название поля.
	  \return индекс в массиве данных m_data на тип поля. Если равно размеру структуры m_dataEnd, то поле не найдено.
	*/
	uint16_t Find(const char* name);
	/// Увеличить размер массива данных.
	/*!
	 * При необходимости увеличивает размер массива данных под структуру.
	 *
	  \param[in] size дополнительное пространство в байтах.
	*/
	void AddSpace(uint32_t size);

public:
	/// Конструктор.
	/*!
	 *  Для парсинга принятой структуры.
	 *
	  \param[in] data данные.
	  \param[in] size размер данных в байтах.
	  \param[in] add_size дополнительное пространство в байтах.
	  \param[in] copy флаг копирования данных.
	*/
	CImir(uint8_t* data, uint16_t size, uint16_t add_size=0, bool copy=true);
	/// Конструктор.
	/*!
	 *  Для создоваемой структуры.
	 *
	  \param[in] add_size дополнительное пространство в байтах.
	*/
	CImir(uint16_t add_size=512);
	/// Деструктор.
	~CImir();

	/// Убирает дополнительное пространство под структуру.
	void Trim();
	/// Получить данные.
	/*!
	  \param[out] size размер данных.
	  \return ссылка на массив данных под структуру.
	*/
	inline uint8_t* GetData(uint16_t& size)
	{
		size=m_dataEnd;
		return m_data;
	};

	/// Прочитать пустое поле.
	/*!
	  \param[in] name название поля.
	  \return true если поле существует.
	*/
	bool GetValue(const char* name);
	/// Прочитать строковое поле.
	/*!
	  \param[in] name название поля.
	  \param[out] value значение поля.
	  \return true если поле существует.
	*/
	bool GetValue(const char* name, const char*& value);
	/// Прочитать байтовое поле.
	/*!
	  \param[in] name название поля.
	  \param[out] value значение поля.
	  \return true если поле существует.
	*/
	bool GetValue(const char* name, uint8_t& value);
	/// Прочитать двухбайтовое поле.
	/*!
	  \param[in] name название поля.
	  \param[out] value значение поля.
	  \return true если поле существует.
	*/
	bool GetValue(const char* name, uint16_t& value);
	/// Прочитать 32-х разрядное поле.
	/*!
	  \param[in] name название поля.
	  \param[out] value значение поля.
	  \return true если поле существует.
	*/
	bool GetValue(const char* name, uint32_t& value);
	/// Прочитать поле с плавающей точкой.
	/*!
	  \param[in] name название поля.
	  \param[out] value значение поля.
	  \return true если поле существует.
	*/
	bool GetValue(const char* name, float& value);
	/// Прочитать поле с массивом данных.
	/*!
	  \param[in] name название поля.
	  \param[out] value ссылка на массив.
	  \param[out] size размер массива.
	  \return true если поле существует.
	*/
	bool GetValue(const char* name, const uint8_t*& value, uint32_t& size);

	/// Добавить пустое поле.
	/*!
	  \param[in] name название поля.
	*/
	void AddValue(const char* name);
	/// Добавить строковое поле.
	/*!
	  \param[in] name название поля.
	  \param[in] value значение.
	*/
	void AddValue(const char* name, const char* value);
	/// Добавить байтовое поле.
	/*!
	  \param[in] name название поля.
	  \param[in] value значение.
	*/
	void AddValue(const char* name, uint8_t value);
	/// Добавить двухбайтовое поле.
	/*!
	  \param[in] name название поля.
	  \param[in] value значение.
	*/
	void AddValue(const char* name, uint16_t value);
	/// Добавить 32-х разрядное поле.
	/*!
	  \param[in] name название поля.
	  \param[in] value значение.
	*/
	void AddValue(const char* name, uint32_t value);
	/// Добавить поле с плавающей точкой.
	/*!
	  \param[in] name название поля.
	  \param[in] value значение.
	*/
	void AddValue(const char* name, float value);
	/// Добавить поле с массивом данных.
	/*!
	  \param[in] name название поля.
	  \param[in] value указатель на массив.
	  \param[in] size размер массива.
	*/
	void AddValue(const char* name, const uint8_t* value, uint32_t size);

	/// Добавить поле со структурой.
	/*!
	  \param[in] name название поля.
	  \param[in] count количество элементов.
	  \warning Следующие count добавлений в поле пойдут в структуру.
	*/
	void AddStruct(const char* name, uint32_t count);
	/// Добавить поле со списком.
	/*!
	  \param[in] name название поля.
	  \param[in] count количество элементов.
	  \warning Следующие count добавлений в поле пойдут в список.
	*/
	void AddList(const char* name, uint32_t count);

};
/*! @} */

#endif // CIMIR_H

