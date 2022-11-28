# Установка на VM Ubuntu 20.04 (minimum)

Инструкция написана на основе документов с Quectel с адаптацией и проверкой:
>        FTP URL：202.111.194.162
>        Account：SC66_O_A10_r008
>        Passwd：123456 

***Необходимо: диск не менее 0.5Tb, виртуальной памяти не менее 10Gb.***

Необходимые дополнительные пакеты:
- sudo apt install git-core gnupg flex bison gperf build-essential zip curl zlib1g-dev libc6-dev
- sudo apt install lib32ncurses5-dev x11proto-core-dev libx11-dev lib32z-dev libgl1-mesa-dev 
- sudo apt install g++-multilib 
- sudo apt install mingw-w64
- sudo apt install tofrodos python-markdown libxml2-utils xsltproc libssl-dev
- sudo apt install default-jdk ccache android-tools-adb lzop pngcrush
- sudo apt install libtinfo5 libncurses5
- sudo apt install libncurses5:i386
- sudo snap install git-repo

## Начальная инициализация
1. Создать папку android
2. Запустить в ней Terminal
3. repo init -u https://source.codeaurora.org/quic/la/platform/manifest.git -b release -m LA.UM.8.2.1.r1-04200-sdm660.0.xml --repo-url=git://codeaurora.org/tools/repo.git --repo-branch=caf-stable
4. repo sync -cdj16 --no-tags (*время выполнения 1-4 суток* :))
5. Скопировать SC66_Android10.0_Quectel_SDK_r008_20200604.tar.gz в домашнюю папку.
6.  mkdir SC66_Android10.0_Quectel_SDK_r008_20200604
7.  tar -zxvf  SC66_Android10.0_Quectel_SDK_r008_20200604.tar.gz -C SC66_Android10.0_Quectel_SDK_r008_20200604
8.  cp  -rf  SC66_Android10.0_Quectel_SDK_r008_20200604/*   android/ 

## Сборка
1. Запустить Terminal в папке android
2. source build/envsetup.sh
3. lunch sdm660_64-eng
4. export _JAVA_OPTIONS="-Xmx4g"
5. ./build.sh dist -j6
6. Если не собирается возможно нужно обновить sdk api (make api-stubs-docs-update-current-api). Еще можно ппопробовать собирать по отдельности (make boot и т.д.) и в одном потоке.

## Прошивка
Прошивка делается под Windows 10 в несколько шагов. Нужно установить qpst.win.2.7_installer_00495.1 c ftp сервера. Также должен быть установлен Python2.7 для запуска скриптов из SC66_Android10.0_r008_Unpacking_Tool_20200423.
1. В файле build_sdm660.bat (папка SC66_Android10.0_r008_Unpacking_Tool_20200423) прописываем пути (COMPILE_TOOLS_PATH,PYTHON_PATH,PYTHONPATH).
2. В дирректорию SC66_Android10.0_r008_Unpacking_Tool_20200423\LINUX\android\out\target\product\sdm660_64 из Android/out/target/product/sdm660_64 с VM переписываем: abl.elf, boot.img, dtbo.img, mdtp.img, metadata.img, persist.img, recovery.img, userdata.img.
3. В дирректорию  SC66_Android10.0_r008_Unpacking_Tool_20200423\LINUX\android\out\target\product\sdm660_64\obj\KERNEL_OBJ из Android/out/target/product/sdm660_64/obj/KERNEL_OBJ с VM переписываем: vmlinux
4. В дирректорию  SC66_Android10.0_r008_Unpacking_Tool_20200423\LINUX\android\out\target\product\sdm660_64\vendor\firmware из Android/out/target/product/sdm660_64/vendor/firmware с VM переписываем: a512_zap.elf
5. В дирректорию SC66_Android10.0_r008_Unpacking_Tool_20200423\LINUX\android\out\target\product\sdm660_64 из Android/out/dist с VM переписываем: super.img.
6. В дирректорию SC66_Android10.0_r008_Unpacking_Tool_20200423\LINUX\android\out\target\product\sdm660_64 из Android/out/dist/sdm660_64-img-eng.romasty.zip с VM распаковываем vbmeta.img, vbmeta_system.img.
7. Запускаем build_sdm660.bat
8. Запускаем QFIL (qpst.win.2.7_installer_00495.1). Tools->Flat Meta Build. Content XML выбираем  SC66_Android10.0_r008_Unpacking_Tool_20200423\contents.xml; Flat Build Path папка в которую будет собрана окончательная прошивка, например, C:\SC66; Storage type должен быть emmc. Жмем OK.
9. Подключаем kit. Если он "корпич", то выключатель в положение "BOOT".
10. Выбираем порт, Programmer Path:C:\SC66\emmc\prog_emmc_ufs_firehose_Sdm660_ddr.elf. Load XML...
11. Жмем Download.

