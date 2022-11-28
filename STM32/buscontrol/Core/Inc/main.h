/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file           : main.h
  * @brief          : Header for main.c file.
  *                   This file contains the common defines of the application.
  ******************************************************************************
  * @attention
  *
  * <h2><center>&copy; Copyright (c) 2021 STMicroelectronics.
  * All rights reserved.</center></h2>
  *
  * This software component is licensed by ST under BSD 3-Clause license,
  * the "License"; You may not use this file except in compliance with the
  * License. You may obtain a copy of the License at:
  *                        opensource.org/licenses/BSD-3-Clause
  *
  ******************************************************************************
  */
/* USER CODE END Header */

/* Define to prevent recursive inclusion -------------------------------------*/
#ifndef __MAIN_H
#define __MAIN_H

#ifdef __cplusplus
extern "C" {
#endif

/* Includes ------------------------------------------------------------------*/
#include "stm32g4xx_hal.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */

/* USER CODE END Includes */

/* Exported types ------------------------------------------------------------*/
/* USER CODE BEGIN ET */

/* USER CODE END ET */

/* Exported constants --------------------------------------------------------*/
/* USER CODE BEGIN EC */

/* USER CODE END EC */

/* Exported macro ------------------------------------------------------------*/
/* USER CODE BEGIN EM */

/* USER CODE END EM */

/* Exported functions prototypes ---------------------------------------------*/
void Error_Handler(void);

/* USER CODE BEGIN EFP */

/* USER CODE END EFP */

/* Private defines -----------------------------------------------------------*/
#define PWRKEY_Pin GPIO_PIN_4
#define PWRKEY_GPIO_Port GPIOE
#define RESET_LAN_Pin GPIO_PIN_5
#define RESET_LAN_GPIO_Port GPIOE
#define KEY_ON_Pin GPIO_PIN_3
#define KEY_ON_GPIO_Port GPIOE
#define CMUTE_Pin GPIO_PIN_1
#define CMUTE_GPIO_Port GPIOE
#define HS_DET_Pin GPIO_PIN_14
#define HS_DET_GPIO_Port GPIOC
#define LED1_Pin GPIO_PIN_6
#define LED1_GPIO_Port GPIOE
#define AMP_GAIN_Pin GPIO_PIN_2
#define AMP_GAIN_GPIO_Port GPIOE
#define RESET_Pin GPIO_PIN_0
#define RESET_GPIO_Port GPIOE
#define SHD_Pin GPIO_PIN_15
#define SHD_GPIO_Port GPIOC
#define LED_EXT_Pin GPIO_PIN_13
#define LED_EXT_GPIO_Port GPIOC
#define FAULT1_Pin GPIO_PIN_6
#define FAULT1_GPIO_Port GPIOC
#define FAULT2_Pin GPIO_PIN_7
#define FAULT2_GPIO_Port GPIOC
#define LED2_Pin GPIO_PIN_7
#define LED2_GPIO_Port GPIOE
#define DIO5_Pin GPIO_PIN_12
#define DIO5_GPIO_Port GPIOE
#define FAULT3_Pin GPIO_PIN_4
#define FAULT3_GPIO_Port GPIOA
#define DIO1_Pin GPIO_PIN_8
#define DIO1_GPIO_Port GPIOE
#define DIO2_Pin GPIO_PIN_9
#define DIO2_GPIO_Port GPIOE
#define DIO8_Pin GPIO_PIN_15
#define DIO8_GPIO_Port GPIOE
#define LCD_INT_Pin GPIO_PIN_5
#define LCD_INT_GPIO_Port GPIOA
#define LCD_EN_Pin GPIO_PIN_6
#define LCD_EN_GPIO_Port GPIOA
#define IGN_DET_Pin GPIO_PIN_5
#define IGN_DET_GPIO_Port GPIOC
#define DIO4_Pin GPIO_PIN_11
#define DIO4_GPIO_Port GPIOE
#define DIO7_Pin GPIO_PIN_14
#define DIO7_GPIO_Port GPIOE
#define SC_READY_Pin GPIO_PIN_4
#define SC_READY_GPIO_Port GPIOC
#define DIO3_Pin GPIO_PIN_10
#define DIO3_GPIO_Port GPIOE
#define DIO6_Pin GPIO_PIN_13
#define DIO6_GPIO_Port GPIOE
/* USER CODE BEGIN Private defines */

/* USER CODE END Private defines */

#ifdef __cplusplus
}
#endif

#endif /* __MAIN_H */
