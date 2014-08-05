// Java jni part //

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
 * Method:    nativeinitWidget
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_renpy_android_PythonWidgetProvider_nativeinitWidget
  (JNIEnv *, jobject, jint);

/*
 * Class:     org_renpy_android_PythonWidgetProvider
 * Method:    nativeupdateWidget
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_renpy_android_PythonWidgetProvider_nativeupdateWidget
  (JNIEnv *, jobject, jint);

/*
 * Class:     org_renpy_android_PythonWidgetProvider
 * Method:    nativedestroyWidget
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_renpy_android_PythonWidgetProvider_nativedestroyWidget
  (JNIEnv *, jobject, jint);

/*
 * Class:     org_renpy_android_PythonWidgetProvider
 * Method:    nativePythonCallback
 * Signature: (ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_renpy_android_PythonWidgetProvider_nativePythonCallback
  (JNIEnv *, jobject, jint, jstring);

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


static PyObject *PythonWidgets_updateWidget(PyObject *self, PyObject *args);
static PyObject *PythonWidgets_existWidget(PyObject *self, PyObject *args);
static PyObject *PythonWidgets_getDfltErrView(PyObject *self, PyObject *args);
static PyObject *PythonWidgets_setDfltErrView(PyObject *self, PyObject *args);
static PyObject *PythonWidgets_getDfltLoadView(PyObject *self, PyObject *args);
static PyObject *PythonWidgets_setDfltLoadView(PyObject *self, PyObject *args);
static PyObject *PythonWidgets_getWidgetData(PyObject *self, PyObject *args);

PyMODINIT_FUNC initPythonWidgets(void);

#endif
#endif // PYTHONWIDGETS_H //