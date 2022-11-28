# Гранит-Навигатор БК-Н
## Структура папок
- Android - ПО под Android
  - bkn - библиотека сервисов
  - ServiceTests - тестовое ПО для библиотеки сервисов (compose)
  - Granit-BK-N - ПО для планшета водителя
- PC - утилиты
  - VisualStudio - утилиты под .net5 (Win10)
    - NavControlLibrary - библиотека общих компонентов
    - ScriptEditor - редактор маршрута
    - MQTTLog - утилита для отслеживания работы планшета водителя через MQTT сервер
    - GPSPlayer - утилита для проигрования GPS логов, записанных MQTTLog. 
    - Setup - проект дистрибутива утилит под .net
- AOSP - Сборка прошивки для навигатора
- Docs - Документация и информационные материалы
  - diagrams - диаграммы и блок-схемы
  - scripts - пример скриптов маршрута
- Servers - ПО для серверов
  - node-red - скрипты node-red (https://nodered.org/)