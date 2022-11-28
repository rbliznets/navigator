/*!
	\file
	\brief Класс для реализации передачи и приема данных по USB.
	\authors Близнец Р.А.
	\version 1.0.0.0
	\date 29.10.2021
	\copyright (c) Copyright 2021, ООО "Глобал Ориент", Москва, Россия, http://www.glorient.ru/

	Один объект на приложение.
*/

#include "CUSBTask.h"
#include "../Algorithms/Debug/CTrace.h"
#include "usbd_cdc_if.h"

#include <cstdio>
#include <cstring>
#include "CLogicTask.h"

void CUSBTask::control(uint8_t cmd, uint8_t* pbuf, uint16_t length)
{
//	printf("0x%x:%d\n",cmd,length);
	CUSBTask* par;

	switch(cmd)
	{
    case CDC_SEND_ENCAPSULATED_COMMAND:
    	break;
    case CDC_GET_ENCAPSULATED_RESPONSE:
    	break;
    case CDC_SET_COMM_FEATURE:
    	break;
    case CDC_GET_COMM_FEATURE:
    	break;
    case CDC_CLEAR_COMM_FEATURE:
    	break;

  /*******************************************************************************/
  /* Line Coding Structure                                                       */
  /*-----------------------------------------------------------------------------*/
  /* Offset | Field       | Size | Value  | Description                          */
  /* 0      | dwDTERate   |   4  | Number |Data terminal rate, in bits per second*/
  /* 4      | bCharFormat |   1  | Number | Stop bits                            */
  /*                                        0 - 1 Stop bit                       */
  /*                                        1 - 1.5 Stop bits                    */
  /*                                        2 - 2 Stop bits                      */
  /* 5      | bParityType |  1   | Number | Parity                               */
  /*                                        0 - None                             */
  /*                                        1 - Odd                              */
  /*                                        2 - Even                             */
  /*                                        3 - Mark                             */
  /*                                        4 - Space                            */
  /* 6      | bDataBits  |   1   | Number Data bits (5, 6, 7, 8 or 16).          */
  /*******************************************************************************/
    case CDC_SET_LINE_CODING:
    	par=(CUSBTask*)CUSBTask::Instance();
    	par->mFirst=false;
    	break;
    case CDC_GET_LINE_CODING:
    	break;
    case CDC_SET_CONTROL_LINE_STATE:
    	break;
    case CDC_SEND_BREAK:
    	break;
   default:
	   break;
   }
}

void CUSBTask::endTransmit()
{
	CUSBTask* par=(CUSBTask*)CUSBTask::Instance();
	if(par->mInit){
		BaseType_t xHigherPriorityTaskWoken=pdFALSE;
		xTaskNotifyFromISR(par->mTaskHandle,USBTASK_TXEND_FLAG,eSetBits,&xHigherPriorityTaskWoken);
		portYIELD_FROM_ISR( xHigherPriorityTaskWoken );
	}
}

void CUSBTask::endRecieve(uint8_t* data, uint32_t size)
{
#ifdef useEXT_CHECK
	configASSERT(size <= USB_BUF_SIZE);
#endif
	CUSBTask* par=(CUSBTask*)CUSBTask::Instance();
	if(par->mInit)
	{
		BaseType_t xHigherPriorityTaskWoken=pdFALSE;

		for (uint32_t i = 0; i < size; i++)
		{
			switch(par->mCurrentIndexRx)
			{
			case 0:
				if(data[i] == 0xf6)
				{
					par->mCmdBuffer[par->mCurrentIndexRx]=data[i];
					par->mCurrentIndexRx++;
				}
				break;
			case 1:
				if(data[i] == 0xb8)
				{
					par->mCmdBuffer[par->mCurrentIndexRx]=data[i];
					par->mCurrentIndexRx++;
				}
				else par->mCurrentIndexRx=0;
				break;
			case 2:
				if(data[i] == 0xaa)
				{
					par->mCmdBuffer[par->mCurrentIndexRx]=data[i];
					par->mCurrentIndexRx++;
				}
				else par->mCurrentIndexRx=0;
				break;
			case 3:
				if(data[i] == 0x18)
				{
					par->mCmdBuffer[par->mCurrentIndexRx]=data[i];
					par->mCurrentIndexRx++;
				}
				else par->mCurrentIndexRx=0;
				break;
			case 4:
				par->mCmdBuffer[par->mCurrentIndexRx]=data[i];
				par->mCurrentIndexRx++;
				par->mSizeRx=data[i];
				break;
			case 5:
				par->mCmdBuffer[par->mCurrentIndexRx]=data[i];
				par->mCurrentIndexRx++;
				par->mSizeRx+=data[i]*256;
				break;
			case 6:
				par->mCmdBuffer[par->mCurrentIndexRx]=data[i];
				par->mCurrentIndexRx++;
				par->mSizeRx+=data[i]*256*256;
				break;
			case 7:
				par->mCmdBuffer[par->mCurrentIndexRx]=data[i];
				par->mSizeRx+=data[i]*256*256*256+10;
				if(par->mSizeRx > USB_BUF_SIZE)
				{
					par->mCurrentIndexRx=0;
					xTaskNotifyFromISR(par->mTaskHandle,USBTASK_RX_BUF_FLAG,eSetBits,&xHigherPriorityTaskWoken);
				}
				else
				{
					par->mCurrentIndexRx++;
				}
				break;
			default:
				if(par->mCurrentIndexRx < (par->mSizeRx-1))
				{
					par->mCmdBuffer[par->mCurrentIndexRx]=data[i];
					par->mCurrentIndexRx++;
				}
				else
				{
					par->mCmdBuffer[par->mCurrentIndexRx]=data[i];
					if(par->mOverFalg){
						xTaskNotifyFromISR(par->mTaskHandle,USBTASK_RX_OVER_FLAG,eSetBits,&xHigherPriorityTaskWoken);
					}else{
						par->mOverFalg=true;
						std::memcpy(par->mRxBuffer,par->mCmdBuffer,par->mSizeRx);
						par->mRxBufferSize=par->mSizeRx;
						xTaskNotifyFromISR(par->mTaskHandle,USBTASK_RX_FLAG,eSetBits,&xHigherPriorityTaskWoken);
					}
					par->mCurrentIndexRx=0;
				}
				break;
			}
		}
		portYIELD_FROM_ISR( xHigherPriorityTaskWoken );
	}
}

void CUSBTask::Run()
{
	uint32_t flags;
	STaskMessage msg;


	for( ;; )
	{
		if(xTaskNotifyWait(0,0xffffffff,&flags,portMAX_DELAY) == pdTRUE)
		{
			if((flags & USBTASK_RX_FLAG)!=0)
			{
				ProccessRx();
			}
			if((flags & USBTASK_RX_OVER_FLAG)!=0)
			{
				TRACE("USBTASK_RX_OVER_FLAG",0,false);
			}
			if((flags & USBTASK_RX_BUF_FLAG)!=0)
			{
				TRACE("USBTASK_RX_BUF_FLAG",mSizeRx,false);
			}
			if((flags & USBTASK_TXEND_FLAG)!=0)
			{
				mWaitTx=false;
				while((!mWaitTx) && GetMessage(&msg))
				{
					DoMessage(msg);
				}
			}
			if((flags & USBTASK_QUEUE_FLAG)!=0)
			{
				while((!mWaitTx) && GetMessage(&msg))
				{
					DoMessage(msg);
				}
			}
		}
	}
}

void CUSBTask::ProccessRx()
{
	STaskMessage tmsg;
	uint8_t* dt;

	if(mCRC.Check(mRxBuffer,mRxBufferSize))
	{
		dt=AllocNewMsg(&tmsg,CMD_JSON_DATA,mRxBufferSize-9);
		std::memcpy(dt,&mRxBuffer[8],mRxBufferSize-10);
		dt[mRxBufferSize-10]=0;
		CLogicTask::Instance()->SendMessage(&tmsg, 0, true);
	}
	else
	{
		TRACE("CUSBTask::ProccessRx: crc failed",mRxBufferSize,false);
	}

	portENTER_CRITICAL();
	mOverFalg=false;
	portEXIT_CRITICAL();
}

uint32_t CUSBTask::ProccessTx(uint8_t* data, uint32_t size)
{
	if(mFirst)
	{
		return 0;
	}
	if(size > (USB_BUF_SIZE-10))
	{
		TRACE("UCUSBTask::ProccessTx: buf overflow",size,false);
		return 0;
	}

	mTxBuffer[0]=0xf6;
	mTxBuffer[1]=0xb8;
	mTxBuffer[2]=0xaa;
	mTxBuffer[3]=0x18;
	std::memcpy(&mTxBuffer[4],&size,4);
	std::memcpy(&mTxBuffer[8],data,size);
	mCRC.Create(mTxBuffer, size+8, (uint16_t*)&mTxBuffer[size+8]);

	mWaitTx=true;
	uint8_t res=CDC_Transmit_FS(mTxBuffer,size+10);
	if(USBD_OK != res){
#ifdef DEBUG
		std::printf("CDC_Transmit_FS failed %d: %d\n", res, size+10);
#endif
		mWaitTx=false;
		return 0;
	}
	return size+10;
}

void CUSBTask::DoMessage(STaskMessage msg)
{
	switch(msg.msgID)
	{
	case CMD_JSON_DATA:
#ifdef DEBUG
//		std::printf("(%d)%s\n", msg.shortParam, (const char*)msg.msgBody);
#endif
		ProccessTx((uint8_t*)msg.msgBody,msg.shortParam);
		vPortFree(msg.msgBody);
		break;
	default:
#ifdef DEBUG
		std::printf("CUSBTask unknown message: %d(%d)\n", msg.msgID, msg.shortParam);
#endif
		break;
	}
}

