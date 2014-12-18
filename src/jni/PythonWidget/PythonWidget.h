/*
 * PythonWidget.h
 *
 *  Created on: 18.06.2014
 *  Licensed under the MIT license
 *  http://opensource.org/licenses/mit-license.php
 *
 *  Copyright 2014 Sebastian Scholz <abestanis.gc@gmail.com> 
 */


// Java jni part //

/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_renpy_android_PythonWidgetProvider */

#ifndef _Included_org_renpy_android_PythonWidgetProvider
#define _Included_org_renpy_android_PythonWidgetProvider
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_renpy_android_PythonWidgetProvider
 * Method:    nativeinit
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_renpy_android_PythonWidgetProvider_nativeinit
  (JNIEnv *, jobject, jstring, jstring, jstring);

/*
 * Class:     org_renpy_android_PythonWidgetProvider
 * Method:    nativeend
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_renpy_android_PythonWidgetProvider_nativeend
  (JNIEnv *, jobject);

/*
 * Class:     org_renpy_android_PythonWidgetProvider
 * Method:    nativeinitProvider
 * Signature: (ILjava/lang/String;)
 */
JNIEXPORT void JNICALL Java_org_renpy_android_PythonWidgetProvider_nativeinitProvider
  (JNIEnv *, jobject, jint, jstring);

/*
 * Class:     org_renpy_android_PythonWidgetProvider
 * Method:    nativeendProvider
 * Signature: (ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_renpy_android_PythonWidgetProvider_nativeendProvider
  (JNIEnv *, jobject, jint, jstring);

/*
 * Class:     org_renpy_android_PythonWidgetProvider
 * Method:    nativeinitWidget
 * Signature: (ILjava/lang/String;I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_renpy_android_PythonWidgetProvider_nativeinitWidget
  (JNIEnv *, jobject, jint, jstring, jint);

/*
 * Class:     org_renpy_android_PythonWidgetProvider
 * Method:    nativeupdateWidget
 * Signature: (ILjava/lang/String;II)V
 */
JNIEXPORT void JNICALL Java_org_renpy_android_PythonWidgetProvider_nativeupdateWidget
  (JNIEnv *, jobject, jint, jstring, jint, jint);

/*
 * Class:     org_renpy_android_PythonWidgetProvider
 * Method:    nativedestroyWidget
 * Signature: (ILjava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_org_renpy_android_PythonWidgetProvider_nativedestroyWidget
  (JNIEnv *, jobject, jint, jstring, jint);

/*
 * Class:     org_renpy_android_PythonWidgetProvider
 * Method:    nativePythonCallback
 * Signature: (ILjava/lang/String;ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_renpy_android_PythonWidgetProvider_nativePythonCallback
  (JNIEnv *, jobject, jint, jstring, jint, jstring);

#ifdef __cplusplus
}
#endif
#endif


// The PythonWidget Module //

#ifndef PYTHONWIDGETS_H
#define PYTHONWIDGETS_H

#define PY_SSIZE_T_CLEAN
#include "Python.h"
#ifndef Py_PYTHON_H
    #error Python headers needed to compile C extensions, please install development version of Python.
#else

#define msgassert(x, message) { if (!x) { __android_log_print(ANDROID_LOG_ERROR, "android_jni", "Assertion failed: %s. %s:%d", message, __FILE__, __LINE__); abort(); }}


static PyObject *PythonWidgets_updateWidget(         PyObject *self, PyObject *args);
static PyObject *PythonWidgets_existWidget(          PyObject *self, PyObject *args);
static PyObject *PythonWidgets_storeWidgetData(      PyObject *self, PyObject *args);
static PyObject *PythonWidgets_getWidgetData(        PyObject *self, PyObject *args);
static PyObject *PythonWidgets_addOnClickCallb(      PyObject *self, PyObject *args);
static PyObject *PythonWidgets_setInitAction(        PyObject *self, PyObject *args);
static PyObject *PythonWidgets_setSingleUpdate(      PyObject *self, PyObject *args);
static PyObject *PythonWidgets_setPeriodicUpdateFreq(PyObject *self, PyObject *args);
static PyObject *PythonWidgets_getPeriodicUpdateFreq(PyObject *self, PyObject *args);

PyMODINIT_FUNC initPythonWidgets(void);

#endif
#endif // PYTHONWIDGETS_H //