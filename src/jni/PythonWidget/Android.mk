LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := PythonWidget.c

LOCAL_MODULE := PythonWidget

LOCAL_LDLIBS := -llog -lpython2.7

LOCAL_SHARED_LIBRARIES := sdl $(COMPILED_LIBRARIES)

LOCAL_CFLAGS := $(foreach D, $(APP_SUBDIRS), -I$(LOCAL_PATH)/$(D)) \
				-I$(LOCAL_PATH)/.. \
				-I$(LOCAL_PATH)/../../../build/python-install/include/python2.7

LOCAL_LDFLAGS += -L$(LOCAL_PATH)/../../../build/python-install/lib $(APPLICATION_ADDITIONAL_LDFLAGS)

include $(BUILD_SHARED_LIBRARY)