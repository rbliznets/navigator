# Установка OpenGApps AOSP based build system на VM Ubuntu 14.04

Инструкция написана на основе https://github.com/opengapps/aosp_build для установленого aosp с инсртукции от производителя:

1. Устанавливаем git-lfs. Из репозитория Ubuntu работает как-то непонятно, поэтому curl -s https://packagecloud.io/install/repositories/github/git-lfs/script.deb.sh | sudo bash
2. sudo apt-get install git-lfs
3. Часть репозиториев лежит на gitlabs c TLS 1.2. Поэтому нужен более новый curl. В /etc/apt/sources.list добавляем deb http://security.ubuntu.com/ubuntu xenial-security main и deb http://cz.archive.ubuntu.com/ubuntu xenial main universe
4. apt-get update && apt-get install curl
5. Убираем из /etc/apt/sources.list всё что добавили (иначе следующий upgrade может убить систему).
6. Добавляем в конец файла android/.repo/manifest.xml перед последним тэгом:
> <remote name="opengapps" fetch="https://github.com/opengapps/"  /\>
> <remote name="opengapps-gitlab" fetch="https://gitlab.opengapps.org/opengapps/"  /\>
> <project path="vendor/opengapps/build" name="aosp_build" revision="master" remote="opengapps" /\>
> <project path="vendor/opengapps/sources/all" name="all" clone-depth="1" revision="master" remote="opengapps-gitlab" /\>
> <project path="vendor/opengapps/sources/arm" name="arm" clone-depth="1" revision="master" remote="opengapps-gitlab" /\>
> <project path="vendor/opengapps/sources/arm64" name="arm64" clone-depth="1" revision="master" remote="opengapps-gitlab" /\>
> <project path="vendor/opengapps/sources/x86" name="x86" clone-depth="1" revision="master" remote="opengapps-gitlab" /\>
> <project path="vendor/opengapps/sources/x86_64" name="x86_64" clone-depth="1" revision="master" remote="opengapps-gitlab" /\>
7. В папке android/.repo/manifests комитим git
8. Переходим в android
9. git lfs install 
10. repo sync -j12
11. Добовляем в конец android/device/qcom/sdm660_64/sdm660_64.mk
> GAPPS_VARIANT := mini
> $(call inherit-product, vendor/opengapps/build/opengapps-packages.mk)
12. Собираем систему с очисткой (см. скрипт b). 


