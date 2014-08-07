/*
 * PythonWidget.c
 *
 *  Created on: 18.06.2014
 *  Licensed under the MIT license
 *  http://opensource.org/licenses/mit-license.php
 *
 *  Copyright 2014 Sebastian Scholz <abestanis.gc@gmail.com> 
 */

#include "PythonWidget.h"
#include "android/log.h"
#include <string.h>
#include <jni.h>


// The Log system //

#define LOGW(x) __android_log_write(ANDROID_LOG_INFO, "PythonWidgets", (x))

static PyObject *androidembed_wlog(PyObject *self, PyObject *args) {
    char *logstr = NULL;
    if (!PyArg_ParseTuple(args, "s", &logstr)) {
        return NULL;
    }
    LOGW(logstr);
    Py_RETURN_NONE;
}

static PyMethodDef AndroidEmbedwMethods[] = {
    {"log", androidembed_wlog, METH_VARARGS,
     "Widget log on android platform"},
    {NULL, NULL, 0, NULL}
};

PyMODINIT_FUNC initwandroidembed(void) {
    (void) Py_InitModule("androidembed", AndroidEmbedwMethods);
}


// Java jni functions //

PyObject *wrInstance            = NULL;
static JavaVM *Jvm              = NULL;
jobject _PyWidgetClass          = NULL;
PyObject* _androidWidgetsModule = NULL;
static PyObject* _widgetsList   = NULL;
static PyObject* _callbackList  = NULL;
static int INIT_SUCCESS         = 0;


jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    Jvm = vm;
    return JNI_VERSION_1_6;
}

JNIEnv* GetJNIEnv(void) {
    JNIEnv *env;
    LOGW("Calling GetEnv...");
    (*Jvm)->GetEnv(Jvm,(void**) &env, JNI_VERSION_1_6);
    LOGW("Done.");
    return env;
}

JNIEXPORT void JNICALL Java_org_renpy_android_PythonWidgetProvider_nativeinit(JNIEnv* env, jobject obj, jstring andr_priv, jstring andr_arg, jstring py_path) {
    LOGW("C: Init PythonWidgets...");
    
    _PyWidgetClass = obj;
    jboolean iscopy;
    const char *android_private  = (*env)->GetStringUTFChars(env, andr_priv, &iscopy);
    const char *android_argument = (*env)->GetStringUTFChars(env, andr_arg,  &iscopy);
    const char *python_path      = (*env)->GetStringUTFChars(env, py_path,   &iscopy);
    
    setenv("ANDROID_PRIVATE", android_private, 1);
    setenv("ANDROID_ARGUMENT", android_argument, 1);
    setenv("PYTHONOPTIMIZE", "2", 1);
    setenv("PYTHONHOME", android_private, 1);
    setenv("PYTHONPATH", python_path, 1);
    
    
    char *argv[] = {"sdl"};
    
    Py_SetProgramName(argv[0]);
    LOGW("C: Initializing Python...");
    Py_Initialize();
    LOGW("C: Done. Setting args...");
    PySys_SetArgv(1, argv);
    LOGW("C: Done. Init Threads...");
    PyEval_InitThreads();
    
    LOGW("C: Done. Init log service...");
    initwandroidembed();
    
    LOGW("C: Done. Run startup script...");

    /* inject our bootstrap code to redirect python stdin/stdout
     * replace sys.path with our path
     */
    int res = PyRun_SimpleString(
        "import sys, posix\n" \
        "private = posix.environ['ANDROID_PRIVATE']\n" \
        "argument = posix.environ['ANDROID_ARGUMENT']\n" \
        "sys.path[:] = [ \n" \
		"    private + '/lib/python27.zip', \n" \
		"    private + '/lib/python2.7/', \n" \
		"    private + '/lib/python2.7/lib-dynload/', \n" \
		"    private + '/lib/python2.7/site-packages/', \n" \
		"    argument ]\n" \
        "import androidembed\n" \
        "class LogFile(object):\n" \
        "    def __init__(self):\n" \
        "        self.buffer = ''\n" \
        "    def write(self, s):\n" \
        "        s = self.buffer + s\n" \
        "        lines = s.split(\"\\n\")\n" \
        "        for l in lines[:-1]:\n" \
        "            androidembed.log(l)\n" \
        "        self.buffer = lines[-1]\n" \
        "    def flush(self):\n" \
        "        return\n" \
        "sys.stdout = sys.stderr = LogFile()\n" \
        "print 'ANDROID_PRIVATE', private\n" \
		"print 'ANDROID_ARGUMENT', argument\n" \
		"import site; print site.getsitepackages()\n"\
		"print 'Android path', sys.path\n");
    
    if (res == -1) {
        LOGW("C: Startup-script errored!");
        return;
    }
    
    LOGW("C: Startup-Script executed normally.");
    LOGW("C: Changing directory to");
    LOGW(android_argument);
    
    chdir(android_argument);
    
    // Setting up widget cache
    if (_widgetsList == NULL) {
        LOGW("C: Initializing Widget cache...");
        _widgetsList = PyDict_New();
    }
    // Setting up callback storage
    if (_callbackList == NULL) {
        LOGW("C: Initializing callback storage...");
        _callbackList = PyDict_New();
    }
    
    // Setting up ne needet modules
    LOGW("C: Initializing PythonWidget modules...");
    initPythonWidgets();
    
    //Import the WidgetReceiver module
    
    LOGW("C: Importing the WidgetReceiver module...");
    
    PyObject *ClassName = PyList_New(0);
    PyList_Append(ClassName, PyString_FromString("WidgetReceiver"));
    PyObject *wrmodule = PyImport_ImportModuleEx("WidgetReceiver", NULL, NULL, ClassName);
    
    Py_DECREF(&ClassName);
    
    if (wrmodule == NULL) {
        LOGW("C: Unable to import WidgetReceiver");
        PyErr_Print();
        return;
    }
    
    LOGW("C: Imported WidgetReceiver sucessfully.");
    
    //Extract the WidgetReceiver class
    
    if (PyObject_HasAttrString(wrmodule, "WidgetReceiver") == 0) {
        LOGW("C: The imported module WidgetReceiver has no WidgetReceiver class!");
        return;
    }
    
    LOGW("C: Getting a referencepointer to the WidgetReceiver class...");
    PyObject *wrClass = PyObject_GetAttrString(wrmodule, "WidgetReceiver");
    
    Py_DECREF(wrmodule);
    
    if (wrClass == NULL) {
        LOGW("C: Failed! Could not find the WidgetReceiver class!");
        PyErr_Print();
        return;
    }
    LOGW("C: Sucess!");
    
    // Making an instance of this class
    
    LOGW("C: Trying to instanciate WidgetReceiver...");
    
    wrInstance  = PyObject_CallFunction(wrClass, NULL); 
    
    Py_DECREF(wrClass);
    
    if (wrInstance == NULL) {
        LOGW("C: Failed! Could not create an instance of WidgetReceiver class!");
        PyErr_Print();
        
        return;
    }
    LOGW("C: Sucess!");
    _PyWidgetClass = NULL;
    INIT_SUCCESS = 1;
    
    return;
}

JNIEXPORT void JNICALL Java_org_renpy_android_PythonWidgetProvider_nativeend(JNIEnv* env, jobject obj) {
    LOGW("C: Cleanup PythonWidgets...");
    LOGW("C: Closing Python...");
    PyDict_Clear(_widgetsList);
    PyDict_Clear(_callbackList);
    _PyWidgetClass = NULL;
    if (wrInstance != NULL) {
	Py_DECREF(wrInstance);
        wrInstance = NULL;
    }
    Py_Finalize();
    INIT_SUCCESS = 0;
    return;
}

JNIEXPORT jboolean JNICALL Java_org_renpy_android_PythonWidgetProvider_nativeinitWidget(JNIEnv* env, jobject obj, jint WidgetId) {
    LOGW("C: Init Widget...");
    if (!INIT_SUCCESS) {LOGW("C: PythonWidget is not correctly initialized!"); return JNI_FALSE;}
    _PyWidgetClass = obj;
    PyEval_InitThreads();
    if (_androidWidgetsModule != NULL) {// We need to initialize the widget and pass it in
        LOGW("C: Calling Python method 'getWidget'...");
        PyObject *widget = PyObject_CallMethod(_androidWidgetsModule, "getWidget", "i", WidgetId);
        LOGW("C: Done. Caching widget to Python cache...");
        if (PyDict_SetItem(_widgetsList, Py_BuildValue("i", WidgetId), widget) == -1) {
            _PyWidgetClass = NULL;
            LOGW("C: Failed to insert widget in Python cache!");
            PyErr_Print();
            return JNI_FALSE;
        }
        LOGW("Done.");
        if ((wrInstance != NULL) && (PyObject_HasAttrString(wrInstance, "initWidget") == 1)) {
            PyObject *res = PyObject_Call(PyObject_GetAttrString(wrInstance, "initWidget"), Py_BuildValue("()"), Py_BuildValue("{sO}", "widget", widget));//PyDict_GetItem(_widgetsList, Py_BuildValue("i", WidgetId))
            _PyWidgetClass = NULL;
            if (res == NULL) {
                LOGW("C: Failed to execute initWidget!");
                PyErr_Print();
                return JNI_FALSE;
            }
            LOGW("C: Sucessfully executed initWidget.");
            if (PyBool_Check(res) && (res == Py_True)) {
                LOGW("C: Result is true.");
                return JNI_TRUE;
            }
            LOGW("C: Result is false!");
            return JNI_FALSE;
        }
        _PyWidgetClass = NULL;
        LOGW("C: Failed. Our WidgetProvider instance has no attribute 'initWidget'!");
        return JNI_FALSE;
    }
    _PyWidgetClass = NULL;
    LOGW("C: Failed. Haven't initialized the AndroidWidgets module yet!");
    return JNI_FALSE;
}

JNIEXPORT void JNICALL Java_org_renpy_android_PythonWidgetProvider_nativeupdateWidget(JNIEnv* env, jobject obj, jint WidgetId) {
    LOGW("C: Update Widget...");
    if (!INIT_SUCCESS) {LOGW("C: PythonWidget is not correctly initialized!"); return;}
    _PyWidgetClass = obj;
    PyEval_InitThreads();
    if ((wrInstance != NULL) && (PyObject_HasAttrString(wrInstance, "updateWidget") == 1)) {
        LOGW("C: Getting cached widget");
        PyObject* widget = PyDict_GetItem(_widgetsList, Py_BuildValue("i", WidgetId));
        if (widget == NULL) {
           LOGW("C: Did not found the widget in the cache!");
            return; 
        }
        PyObject *res = PyObject_Call(PyObject_GetAttrString(wrInstance, "updateWidget"), Py_BuildValue("()"), Py_BuildValue("{sO}", "widget", widget));
        _PyWidgetClass = NULL;
        if (res == NULL) {
            LOGW("C: Failed to execute updateWidget!");
            PyErr_Print();
            return;
        }
        LOGW("C: Sucessfully executed updateWidget.");
        return;
    }
    _PyWidgetClass = NULL;
    LOGW("C: Failed. Our WidgetProvider instance has no attribute 'updateWidget'!");
    return;
}

JNIEXPORT void JNICALL Java_org_renpy_android_PythonWidgetProvider_nativedestroyWidget(JNIEnv* env, jobject obj, jint WidgetId) {
    LOGW("C: Destroing Widget...");
    if (!INIT_SUCCESS) {LOGW("C: PythonWidget is not correctly initialized!"); return;}
    _PyWidgetClass = obj;
    PyEval_InitThreads();
    //PyDict_DelItem(PyObject *p, PyObject *key)
    if ((wrInstance != NULL) && (PyObject_HasAttrString(wrInstance, "destroyWidget") == 1)) {
        PyObject* widget = PyDict_GetItem(_widgetsList, Py_BuildValue("i", WidgetId));
        if (widget == NULL) {
           LOGW("C: Did not found the widget in the cache!");
            return; 
        }
        PyObject *res = PyObject_Call(PyObject_GetAttrString(wrInstance, "destroyWidget"), Py_BuildValue("()"), Py_BuildValue("{sO}", "widget", widget));
        if (res != NULL) {
            _PyWidgetClass = NULL;
            LOGW("C: Sucessfully executed destroyWidget.");
            return;
        }
        LOGW("C: Failed to execute destroyWidget!");
        PyErr_Print();
    } else {
        LOGW("C: Failed. Our WidgetProvider instance has no attribute 'destroyWidget'!");
    }
    PyDict_DelItem(_widgetsList, Py_BuildValue("i", WidgetId));
    _PyWidgetClass = NULL;
    return;
}

JNIEXPORT void JNICALL Java_org_renpy_android_PythonWidgetProvider_nativePythonCallback(JNIEnv* env, jobject obj, jint WidgetId, jstring jPyFuncId) {
    LOGW("C: Calling python callback function...");
    if (!INIT_SUCCESS) {LOGW("C: PythonWidget is not correctly initialized!"); return;}
    _PyWidgetClass = obj;
    PyEval_InitThreads();
    LOGW("C: Converting given java string to c string...");
    const char* CPyFuncId = (*env)->GetStringUTFChars(env, jPyFuncId, 0);
    LOGW("C: Done. Function id is:");
    LOGW(CPyFuncId);
    LOGW("C: Converting c string to python string...");
    PyObject* PyFuncId = Py_BuildValue("s", CPyFuncId);
    LOGW("C: Done.");
    if (PyDict_Check(_callbackList) && PyDict_Contains(_callbackList, PyFuncId)) {
        PyObject* widget = PyDict_GetItem(_widgetsList, Py_BuildValue("i", WidgetId));
        if (widget != NULL) {
            PyObject *res = PyObject_Call(PyDict_GetItem(_callbackList, PyFuncId), Py_BuildValue("()"), Py_BuildValue("{sO}", "widget", widget));
            if (res != NULL) {
                (*env)->ReleaseStringUTFChars(env, jPyFuncId, CPyFuncId);
                _PyWidgetClass = NULL;
                LOGW("C: Sucessfully executed python callback.");
                return;
            }
            LOGW("C: Failed to execute python callback!");
            PyErr_Print();
        } else {
            LOGW("C: Did not found the widget in the cache!");
        }
    } else {
        LOGW("C: Failed. No callback found for id:");
        LOGW(CPyFuncId);
    }
    (*env)->ReleaseStringUTFChars(env, jPyFuncId, CPyFuncId);
    _PyWidgetClass = NULL;
    return;
}

// PythonWidgets Module //

/* Template:

static PyObject *module_func(PyObject *self, PyObject *args) {
   // Do your stuff here. //
   Py_RETURN_NONE;
}

 Test by:

"C:\Java\jdk1.7.0_51\bin\javap.exe" -s -p "C:\Users\Sebastian\Documents\Android Workspace\PythonActivity\bin\classes\org\renpy\android\PythonWidgetProvider.class"
*/

/*All the Signatures:

public class org.renpy.android.PythonWidgetProvider extends android.appwidget.AppWidgetProvider {
  private static java.lang.String TAG;
    Signature: Ljava/lang/String;
  public static java.lang.String PERIODIC_WIDGET_UPDATE;
    Signature: Ljava/lang/String;
  public static final int DEFAULT_LOADING_LAYOUT;
    Signature: I
  public static final int DEFAULT_ERROR_LAYOUT;
    Signature: I
  private int _updatefreq;
    Signature: I
  private static android.content.Context myContext;
    Signature: Landroid/content/Context;
  private java.util.ArrayList<java.lang.Integer> myWidgets;
    Signature: Ljava/util/ArrayList;
  private java.lang.String _defaultLoadView;
    Signature: Ljava/lang/String;
  private java.lang.String _defaultErrorView;
    Signature: Ljava/lang/String;
  static {};
    Signature: ()V

  public org.renpy.android.PythonWidgetProvider();
    Signature: ()V

  public void onUpdate(android.content.Context, android.appwidget.AppWidgetManager, int[]);
    Signature: (Landroid/content/Context;Landroid/appwidget/AppWidgetManager;[I)V

  public void onAppWidgetOptionsChanged(android.content.Context, android.appwidget.AppWidgetManager, int, android.os.Bundle);
    Signature: (Landroid/content/Context;Landroid/appwidget/AppWidgetManager;ILandroid/os/Bundle;)V

  public void onDeleted(android.content.Context, int[]);
    Signature: (Landroid/content/Context;[I)V

  public void onEnabled(android.content.Context);
    Signature: (Landroid/content/Context;)V

  public void onDisabled(android.content.Context);
    Signature: (Landroid/content/Context;)V

  public void onReceive(android.content.Context, android.content.Intent);
    Signature: (Landroid/content/Context;Landroid/content/Intent;)V

  public void updateWidget(int, java.lang.String);
    Signature: (ILjava/lang/String;)V

  public boolean existWidget(int);
    Signature: (I)Z

  public void setDefaultLoadView(java.lang.String);
    Signature: (Ljava/lang/String;)V

  public java.lang.String getDefaultLoadView();
    Signature: ()Ljava/lang/String;

  public void setDefaultErrorView(java.lang.String);
    Signature: (Ljava/lang/String;)V

  public java.lang.String getDefaultErrorView();
    Signature: ()Ljava/lang/String;

  private void parseViewArgs(android.widget.RemoteViews, java.lang.String, int, java.lang.String);
    Signature: (Landroid/widget/RemoteViews;Ljava/lang/String;ILjava/lang/String;)V

  private android.widget.RemoteViews buildWidgetRemoteViews(int, java.lang.String, int);
    Signature: (ILjava/lang/String;I)Landroid/widget/RemoteViews;

  private android.widget.RemoteViews buildWidgetRemoteViews(int, java.lang.String);
    Signature: (ILjava/lang/String;)Landroid/widget/RemoteViews;

  private void startPythonService();
    Signature: ()V

  private void initWidget(android.content.Context, android.appwidget.AppWidgetManager, int);
    Signature: (Landroid/content/Context;Landroid/appwidget/AppWidgetManager;I)V


  private void updateWidget(android.content.Context, android.appwidget.AppWidgetManager, int);
    Signature: (Landroid/content/Context;Landroid/appwidget/AppWidgetManager;I)V


  public native void nativeinit(java.lang.String, java.lang.String, java.lang.String);
    Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

  public native void nativeend();
    Signature: ()V

  public native boolean nativeinitWidget(int);
    Signature: (I)Z

  public native void nativeupdateWidget(int);
    Signature: (I)V

  public native void nativedestroyWidget(int);
    Signature: (I)V
}

*/

static PyObject *PythonWidgets_updateWidget(PyObject *self, PyObject *args) {
    LOGW("C: PythonWidgets_updateWidget is getting called...");
    
    static JNIEnv *env = NULL;
    static jclass *cls = NULL;
    static jmethodID mid = NULL;
    
    
    int widget_Id;
    const char *widgetview;
    
    PyArg_ParseTuple(args, "iz", &widget_Id, &widgetview);
    // Parse from int and string
    
    //Debug
    LOGW("C: Passed argument widget_Id is");
    char str[15];
    sprintf(str, "%d", widget_Id);
    LOGW(str);
    //
    
    if (env == NULL) {
        env = GetJNIEnv();
        msgassert(env, "Could not obtain the current environment!");
        cls = (*env)->GetObjectClass(env, _PyWidgetClass); // These methods are non static
        msgassert(cls, "Could not get a refference to 'org/renpy/android/PythonWidgetProvider'!");
        mid = (*env)->GetMethodID(env, cls, "updateWidget", "(ILjava/lang/String;)V");
        msgassert(mid, "Could not find the function 'updateWidget' in the Android class!");
    }
    
    LOGW("C: Calling java function updateWidget...");
    (*env)->CallVoidMethod(env, _PyWidgetClass, mid, widget_Id, (*env)->NewStringUTF(env, widgetview));
    LOGW("C: back from Java, returning to Python...");
    Py_RETURN_TRUE;
}

static PyObject *PythonWidgets_existWidget(PyObject *self, PyObject *args) {
    LOGW("C: PythonWidgets_existWidget is getting called...");
    
    static JNIEnv *env = NULL;
    static jclass *cls = NULL;
    static jmethodID mid = NULL;
    
    // Debug
    int widget_Id;
    //
    
    PyArg_ParseTuple(args, "i", &widget_Id);
    
    //Debug
    LOGW("C: Passed argument widget_Id is");
    char str[15];
    sprintf(str, "%d", widget_Id);
    LOGW(str);
    //
    
    if (env == NULL) {
        LOGW("Trying to get env pointer.");
        env = GetJNIEnv();
        msgassert(env, "Could not obtain the current environment!");
        cls = (*env)->GetObjectClass(env, _PyWidgetClass);
        msgassert(cls, "Could not get a instance from class 'org/renpy/android/PythonWidgetProvider'!");
        mid = (*env)->GetMethodID(env, cls, "existWidget", "(I)Z");
        msgassert(mid, "Could not find the function 'existWidget' in the Android class!");
    }
    
    LOGW("C: Calling java function existWidget...");
    jboolean res = (*env)->CallBooleanMethod(env, _PyWidgetClass, mid, widget_Id);
    LOGW("C: back from Java, returning to Python...");
    if (res == JNI_TRUE) {
        LOGW("C: Result is 'True'...");
        Py_RETURN_TRUE;
    }
    else {
        LOGW("C: Result is 'False'...");
        Py_RETURN_FALSE;
    }
}

static PyObject *PythonWidgets_getDfltErrView(PyObject *self, PyObject *args) {
    LOGW("C: PythonWidgets_getDfltErrView is getting called...");
    
    static JNIEnv *env = NULL;
    static jclass *cls = NULL;
    static jmethodID mid = NULL;
    
    if (env == NULL) {
        LOGW("Trying to get env pointer.");
        env = GetJNIEnv();
        msgassert(env, "Could not obtain the current environment!");
        cls = (*env)->GetObjectClass(env, _PyWidgetClass);
        msgassert(cls, "Could not get a instance from class 'org/renpy/android/PythonWidgetProvider'!");
        mid = (*env)->GetMethodID(env, cls, "getDefaultErrorView", "()Ljava/lang/String;");
        msgassert(mid, "Could not find the function 'getDefaultErrorView' in the Android class!");
    }
    
    LOGW("C: Calling java function getDefaultErrorView...");
    jstring res = (*env)->CallObjectMethod(env, _PyWidgetClass, mid);
    LOGW("C: back from Java, returning to Python...");
    if (res == NULL) {
        LOGW("C: Result is None...");
        Py_RETURN_NONE;
    }
    else {
        LOGW("C: Result is a string...");
        LOGW("C: Converting Java string to C string...");
        const char* tmp = (*env)->GetStringUTFChars(env, res, 0);
        LOGW("C: Done. Converting C string to Python string...");
        PyObject* res_str = PyString_FromString(tmp);
        LOGW("C: Done.");
        (*env)->ReleaseStringUTFChars(env, res, tmp);
        return res_str;
    }
}

static PyObject *PythonWidgets_setDfltErrView(PyObject *self, PyObject *args) {
    LOGW("C: PythonWidgets_setDfltErrView is getting called...");
    
    static JNIEnv *env   = NULL;
    static jclass *cls   = NULL;
    static jmethodID mid = NULL;
    const char*   canvas = NULL;
    jstring arg          = NULL;
    
    if (env == NULL) {
        LOGW("Trying to get env pointer.");
        env = GetJNIEnv();
        msgassert(env, "Could not obtain the current environment!");
        cls = (*env)->GetObjectClass(env, _PyWidgetClass);
        msgassert(cls, "Could not get a instance from class 'org/renpy/android/PythonWidgetProvider'!");
        mid = (*env)->GetMethodID(env, cls, "setDefaultErrorView", "(Ljava/lang/String;)V");
        msgassert(mid, "Could not find the function 'setDefaultErrorView' in the Android class!");
    }
    
    if (!PyArg_ParseTuple(args, "z", &canvas)) {
        LOGW("C: (setDefaultErrorView) Failed to extract the canvas_string from the arguments!");
        PyErr_Print();
        Py_RETURN_FALSE;
    }
    
    if (canvas == NULL) {
        LOGW("C: Given default error view is null.");
        arg = NULL;
    } else {
        arg = (*env)->NewStringUTF(env, canvas);
    }
    
    LOGW("C: Calling java function setDefaultErrorView...");
    (*env)->CallVoidMethod(env, _PyWidgetClass, mid, arg);
    LOGW("C: back from Java, returning to Python...");
    Py_RETURN_TRUE;
}

static PyObject *PythonWidgets_getDfltLoadView(PyObject *self, PyObject *args) {
    LOGW("C: PythonWidgets_getDfltLoadView is getting called...");
    
    static JNIEnv *env = NULL;
    static jclass *cls = NULL;
    static jmethodID mid = NULL;
    
    if (env == NULL) {
        LOGW("Trying to get env pointer.");
        env = GetJNIEnv();
        msgassert(env, "Could not obtain the current environment!");
        cls = (*env)->GetObjectClass(env, _PyWidgetClass);
        msgassert(cls, "Could not get a instance from class 'org/renpy/android/PythonWidgetProvider'!");
        mid = (*env)->GetMethodID(env, cls, "getDefaultLoadView", "()Ljava/lang/String;");
        msgassert(mid, "Could not find the function 'getDefaultLoadView' in the Android class!");
    }
    
    LOGW("C: Calling java function getDefaultLoadView...");
    jstring res = (*env)->CallObjectMethod(env, _PyWidgetClass, mid);
    LOGW("C: back from Java, returning to Python...");
    if (res == NULL) {
        LOGW("C: Result is None...");
        Py_RETURN_NONE;
    }
    else {
        LOGW("C: Result is a string...");
        LOGW("C: Converting Java string to C string...");
        const char* tmp = (*env)->GetStringUTFChars(env, res, 0);
        LOGW("C: Done. Converting C string to Python string...");
        PyObject* res_str = PyString_FromString(tmp);
        LOGW("C: Done.");
        (*env)->ReleaseStringUTFChars(env, res, tmp);
        return res_str;
    }
}

static PyObject *PythonWidgets_setDfltLoadView(PyObject *self, PyObject *args) {
    LOGW("C: PythonWidgets_setDfltLoadView is getting called...");
    
    static JNIEnv *env   = NULL;
    static jclass *cls   = NULL;
    static jmethodID mid = NULL;
    const char* canvas   = NULL;
    jstring arg          = NULL;
    
    if (env == NULL) {
        LOGW("Trying to get env pointer.");
        env = GetJNIEnv();
        msgassert(env, "Could not obtain the current environment!");
        cls = (*env)->GetObjectClass(env, _PyWidgetClass);
        msgassert(cls, "Could not get a instance from class 'org/renpy/android/PythonWidgetProvider'!");
        mid = (*env)->GetMethodID(env, cls, "setDefaultLoadView", "(Ljava/lang/String;)V");
        msgassert(mid, "Could not find the function 'setDefaultLoadView' in the Android class!");
    }
    LOGW("C: Converting Python string to c...");
    if (!PyArg_ParseTuple(args, "z", &canvas)) {
        LOGW("C: (setDefaultLoadView) Failed to extract the canvas_string from the arguments!");
        PyErr_Print();
        Py_RETURN_FALSE;
    }
    LOGW("C: Done.");
    if (canvas == NULL) {
        LOGW("C: Given default loading view is null.");
        arg = NULL;
    } else {
        LOGW(canvas);
        LOGW("C: Converting C string to java string...");
        arg = (*env)->NewStringUTF(env, canvas);
        LOGW("C: Done.");
    }
    
    LOGW("C: Calling java function setDefaultLoadView...");
    (*env)->CallVoidMethod(env, _PyWidgetClass, mid, arg);
    LOGW("C: back from Java, returning to Python...");
    Py_RETURN_TRUE;
}

static PyObject *PythonWidgets_getWidgetData(PyObject *self, PyObject *args) {
    LOGW("C: PythonWidgets_getWidgetData is getting called...");
    
    static JNIEnv *env   = NULL;
    static jclass *cls   = NULL;
    static jmethodID mid = NULL;
    const char* canvas   = NULL;
    jstring arg          = NULL;
    
    if (env == NULL) {
        LOGW("Trying to get env pointer.");
        env = GetJNIEnv();
        msgassert(env, "Could not obtain the current environment!");
        cls = (*env)->GetObjectClass(env, _PyWidgetClass);
        msgassert(cls, "Could not get a instance from class 'org/renpy/android/PythonWidgetProvider'!");
        mid = (*env)->GetMethodID(env, cls, "getWidgetData", "()Ljava/lang/String;");
        msgassert(mid, "Could not find the function 'getWidgetData' in the Android class!");
    }
    
    LOGW("C: Calling java function getWidgetData...");
    jstring res = (*env)->CallObjectMethod(env, _PyWidgetClass, mid);
    LOGW("C: back from Java, returning to Python...");
    if (res == NULL) {
        LOGW("C: Result is None...");
        Py_RETURN_NONE;
    }
    else {
        LOGW("C: Result is a string...");
        LOGW("C: Converting Java string to C string...");
        const char* tmp = (*env)->GetStringUTFChars(env, res, 0);
        LOGW("C: Done. Converting C string to Python string...");
        PyObject* res_str = PyString_FromString(tmp);
        LOGW("C: Done.");
        (*env)->ReleaseStringUTFChars(env, res, tmp);
        return res_str;
    }
}

static PyObject *PythonWidgets_addOnClickCallb(PyObject *self, PyObject *args) {
    LOGW("C: PythonWidgets_addOnClickCallb is getting called...");
    
    static JNIEnv *env   = NULL;
    static jclass *cls   = NULL;
    static jmethodID mid = NULL;
    PyObject *id         = NULL;
    PyObject *callback   = NULL;
    
    if (env == NULL) {
        LOGW("Trying to get env pointer.");
        env = GetJNIEnv();
        msgassert(env, "Could not obtain the current environment!");
        cls = (*env)->GetObjectClass(env, _PyWidgetClass);
        msgassert(cls, "Could not get a instance from class 'org/renpy/android/PythonWidgetProvider'!");
        mid = (*env)->GetMethodID(env, cls, "getWidgetData", "()Ljava/lang/String;");
        msgassert(mid, "Could not find the function 'getWidgetData' in the Android class!");
    }
    
    if (!PyArg_ParseTuple(args, "OO", &id, &callback)) {
        LOGW("C: Could not extract the id and the callback from the given arguments!");
        PyErr_Print();
        Py_RETURN_FALSE;
    }
    
    if (!PyDict_Check(_callbackList)) {
        LOGW("C: Adding callback to callback-list failed: Callback-list is not a list (maybe uninitialized)!");
        Py_RETURN_FALSE;
    }
    
    if (PyDict_SetItem(_callbackList, id, callback) == -1) {
        LOGW("C: Adding callback to callback-list failed!");
        PyErr_Print();
        Py_RETURN_FALSE;
    }
    
    LOGW("C: Successfully added callback to callback-list.");
    Py_RETURN_TRUE;
}

static PyMethodDef module_methods[] = {
    { "updateWidget",        PythonWidgets_updateWidget,    METH_VARARGS, "Updates the widget with the id 'widget_Id' using the view 'widgetview'.\nYou need to call this function in order to make your\nchanges to the widgets graphics visible on the screen." },
    { "existWidget",         PythonWidgets_existWidget,     METH_VARARGS, "Check's if a widget with the id 'widget_Id' was allready registered." },
    { "getDefaultErrorView", PythonWidgets_getDfltErrView,  METH_NOARGS,  "Returns the default error view that is shown, if the\ninitialisation of an widget failed or None if None is set." },
    { "setDefaultErrorView", PythonWidgets_setDfltErrView,  METH_VARARGS, "Sets the default error view that is shown, if the\ninitialisation of an widget failed." },
    { "getDefaultLoadView",  PythonWidgets_getDfltLoadView, METH_NOARGS,  "Returns the default loading view that is shown during\nthe initialisation of an widget or None if None is set." },
    { "setDefaultLoadView",  PythonWidgets_setDfltLoadView, METH_VARARGS, "Sets the default loading view that is shown\nduring the initialisation of an widget." },
    { "getWidgetData",       PythonWidgets_getWidgetData,   METH_NOARGS, "Returns the data as a urlencoded string,\nthat was passed to 'storeWidgetData' in the main App or None,\nif there is no data stored." },
    { "addOnClick",          PythonWidgets_addOnClickCallb, METH_VARARGS, "Adds the given callback 'callback' to the identifier 'id'." },
    /*{ "", (PyCFunction)module_func, METH_NOARGS, "" },*/
    { NULL, NULL, 0, NULL }
};

PyMODINIT_FUNC initPythonWidgets(void) {
    (void) Py_InitModule3("PythonWidgets", module_methods, "This module provides interaction between your python code and the Java side.");
    LOGW("C: Importing AndroidWidgets...");
    _androidWidgetsModule = PyImport_ImportModule("AndroidWidgets");
    LOGW("C: Importing done.");
    if (_androidWidgetsModule == NULL) {
        LOGW("C: Importing failed!");
        PyErr_Print();
    }
}
