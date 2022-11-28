LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := ServiceTests

LOCAL_CERTIFICATE := platform

LOCAL_SRC_FILES := ServiceTests-release-unsigned.apk

LOCAL_MODULE_CLASS := APPS
LOCAL_PRIVILEGED_MODULE := true

LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_OVERRIDES_PACKAGES := Home GoogleHome Launcher Launcher2 Launcher3
include $(BUILD_PREBUILT)
