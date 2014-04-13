LOCAL_PATH := $(call my-dir)
 
include $(CLEAR_VARS)
 
LOCAL_MODULE    := bt_detect2
LOCAL_SRC_FILES := bt_detect2.c
 
include $(BUILD_SHARED_LIBRARY)
