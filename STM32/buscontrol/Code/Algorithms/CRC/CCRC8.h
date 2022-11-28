/*!
	\file
	\brief Табличный CRC-8.
	\authors Близнец Р.А.
	\version 1.0.0.0
	\date 28.10.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/
*/

#ifndef CCRC8_H
#define CCRC8_H

/*! \defgroup crc8 CRC-8
  	\ingroup crc
  	\brief Табличный алгоритм для CRC-8 (X8+X2+X1+1).

    @{
*/
#include "settings.h"

/// Класс CRC
/*!
 Вычисление и проверка CRC через статические методы.
 */
class CCRC8
{
protected:
	static const uint8_t CRCTable[256]; ///< Таблица для CRC-8 (X8+X2+X1+1).
public:

	/// Проверить CRC-8.
	/*!
	  Полином X8+X2+X1+1
  	  \param[in] data данные.
  	  \param[in] size размер данных.
	  \return true если CRC верно.
	*/
	static bool Check(uint8_t* data, uint16_t size);
	/// Посчитать CRC.
	/*!
 	  Полином X8+X2+X1+1
  	  \param[in] data данные.
  	  \param[in] size размер данных.
 	  \param[out] crc CRC.
	*/
	static void Create(uint8_t* data, uint16_t size,uint8_t* crc);
};
/*! @} */
#endif // CCRC_H

