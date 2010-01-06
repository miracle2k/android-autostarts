LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := com_elsdoerfer_android_autostarts_NativeTask.c 

LOCAL_C_INCLUDES += $(JNI_H_INCLUDE)

LOCAL_MODULE := libNativeTask

include $(BUILD_SHARED_LIBRARY)
