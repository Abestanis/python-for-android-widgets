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


// Python helper func //

PyObject* getallSubclasses(PyObject* cls, int top_level) {
    PyObject* subclasses = PyObject_CallMethod(cls, "__subclasses__", NULL);
    PyObject* lst        = PyList_New(0);
    int       size       = PyList_Size(subclasses);
    int i;
    if (size != 0) {
        for( i = 0; i < size; i++) {
            PyNumber_InPlaceAdd(lst, getallSubclasses(PyList_GetItem(subclasses, i), 0));
        }
    }
    if (top_level != 1) {
        PyList_Append(lst, cls);
    }
    return lst;
}

PyObject* urlencode(PyObject* dict) {
    PyObject* urllib = PyImport_ImportModuleNoBlock("urllib");
    PyObject *key, *value;
    Py_ssize_t pos = 0;
    
    if (urllib == NULL) {
	LOGW("C: Could not import urllib!");
	PyErr_Print();
	Py_RETURN_FALSE;
    }
    while (PyDict_Next(dict, &pos, &key, &value)) {
	if (PyDict_Check(value)) {
	    PyObject* tmp = urlencode(value);
	    if (PyDict_SetItem(dict, key, tmp) == -1) {
                LOGW("C: Failed to save urlencoded item!");
	    }
	} else if (PyList_Check(value)) {
	    int size = PyList_Size(value);
	    if (size != 0) {
		int i;
		for( i = 0; i < size; i++) {
		    PyObject* val = PyList_GetItem(value, i);
		    if (val != NULL && PyDict_Check(val)) {
			PyObject* tmp = urlencode(val);
			if (PyList_SetItem(value, i, tmp) == -1) {
			    LOGW("C: Failed to save urlencoded item!");
			}
		    }
		}
	    }
	}
    }
    PyObject *res = PyObject_CallMethod(urllib, "urlencode", "O", dict);
    if (res == NULL) {
	LOGW("C: Urlencode failed!");
	PyErr_Print();
	Py_RETURN_FALSE;
    }
    return res;
}

// Java jni functions //

static JavaVM *Jvm              = NULL;
PyObject* _androidWidgetsModule = NULL;
static PyObject* _providerList  = NULL;/*
 *
 * example:
 * {
 *    widgetClassName1 = <class>, // Uninitialized provider
 *    widgetClassName2 = { // Initialized provider
 *        "class" = <class>,
 *        provider_id1 = { // To allow subclasses
 *            widgetId1 = <instance>,
 *            ...
 *        }
 *        ...,
 *    },
 *    ...,
 * }
 */
static PyObject* _callbackList  = NULL;
static int INIT_SUCCESS         = 0;
static int PYTHON_INIT 	        = 0;


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

JNIEXPORT void JNICALL Java_org_renpy_android_PythonWidgetProvider_nativeInit(JNIEnv* env, jobject obj, jstring andr_priv, jstring andr_arg, jstring py_path) {
    LOGW("C: Init PythonWidgets...");
    
    const char *android_private  = (*env)->GetStringUTFChars(env, andr_priv, 0);
    const char *android_argument = (*env)->GetStringUTFChars(env, andr_arg,  0);
    const char *python_path      = (*env)->GetStringUTFChars(env, py_path,   0);
    
    setenv("ANDROID_PRIVATE", android_private, 1);
    setenv("ANDROID_ARGUMENT", android_argument, 1);
    setenv("PYTHONOPTIMIZE", "2", 1);
    setenv("PYTHONHOME", android_private, 1);
    setenv("PYTHONPATH", python_path, 1);
    
    
    char *argv[] = {"sdl"};
    
    Py_SetProgramName(argv[0]);
    LOGW("C: Initializing Python...");
    Py_Initialize();
    PYTHON_INIT = 1;
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
    
    // Setting up provider cache
    if (_providerList == NULL) {
        LOGW("C: Initializing provider cache...");
        _providerList = PyDict_New();
    }
    // Setting up callback storage
    if (_callbackList == NULL) {
        LOGW("C: Initializing callback storage...");
        _callbackList = PyDict_New();
    }
    
    // Setting up the needed modules
    LOGW("C: Initializing PythonWidget modules...");
    initPythonWidgets();
    
    //Import the WidgetProvider module
    
    LOGW("C: Importing the WidgetProvider module...");
    
    PyObject *ClassName = PyList_New(0);
    PyList_Append(ClassName, PyString_FromString("WidgetProvider"));
    PyObject *wrmodule = PyImport_ImportModuleEx("WidgetProvider", NULL, NULL, ClassName);
    
    Py_DECREF(&ClassName);
    
    if (wrmodule == NULL) {
        LOGW("C: Unable to import WidgetProvider");
        PyErr_Print();
        return;
    }
    LOGW("C: Imported WidgetProvider successfully.");
    
    // Extract the original Widget class
    if (_androidWidgetsModule == NULL) {
        LOGW("C: The AndroidWidgets module in not initialized!");
        return;
    } else if (PyObject_HasAttrString(_androidWidgetsModule, "Widget") != 1) {
        LOGW("C: The AndroidWidgets module has no Widget class!");
        return;
    }
    LOGW("C: Getting a referencepointer to the Widget class form the AndroidWidget module...");
    PyObject *wClass = PyObject_GetAttrString(_androidWidgetsModule, "Widget");
    if (wClass == NULL) {
        LOGW("C: Failed! Could not find the WidgetProvider class!");
        PyErr_Print();
        return;
    }
    
    // Get all widget classes
    LOGW("Gathering Widgets...");
    PyObject* widgets = getallSubclasses(wClass, 1);
    LOGW("Finished:");
    
    // store them
    int i;
    for (i = 0; i < PyList_GET_SIZE(widgets); i++) {
        PyObject* widgetClass = PyList_GetItem(widgets, i);
        PyObject* name = PyObject_GetAttrString(widgetClass, "__name__");
        LOGW(PyString_AsString(name));
        PyDict_SetItem(_providerList, name, widgetClass);
    }
    
    Py_DECREF(wrmodule);
    INIT_SUCCESS = 1;
    return;
}

JNIEXPORT void JNICALL Java_org_renpy_android_PythonWidgetProvider_nativeEnd(JNIEnv* env, jobject obj) {
    LOGW("C: Cleanup PythonWidgets...");
    LOGW("C: Closing Python...");
    if (_providerList != NULL) {
	PyDict_Clear(_providerList);
    }
    if (_callbackList != NULL) {
	PyDict_Clear(_callbackList);
    }
    if (PYTHON_INIT) {
	Py_Finalize();
    }
    PYTHON_INIT  = 0;
    INIT_SUCCESS = 0;
    return;
}

JNIEXPORT void JNICALL Java_org_renpy_android_PythonWidgetProvider_nativeInitProvider(JNIEnv* env, jobject obj, jint jProviderId, jstring jProviderClass) {
    LOGW("Init Widget Provider...");
    if (!INIT_SUCCESS) {LOGW("C: PythonWidget is not correctly initialized!"); return;}
    PyObject* providerId = Py_BuildValue("i", jProviderId);
    PyObject* jprovider  = PyCapsule_New((void *) &obj, NULL, NULL);
    if (jprovider == NULL) {
        LOGW("C: Failed to convert java provider pointer to python object!");
        PyErr_Print();
        return;
    }
    const char* cProviderClass = (*env)->GetStringUTFChars(env, jProviderClass, 0);
    PyObject* providerClass = PyString_FromString(cProviderClass);
    LOGW(cProviderClass);
    (*env)->ReleaseStringUTFChars(env, jProviderClass, cProviderClass);
    
    // Getting the widget class
    
    LOGW("C: Trying to get the widget class...");
    PyObject* widgetClass = PyDict_GetItem(_providerList, providerClass);
    if (widgetClass == NULL) {
        LOGW("C: Did not found widget class in cache!");
        return;
    }
    if (PyDict_Check(widgetClass)) {
        LOGW("C: Widget class already initialized, this must be a subclass!");
        PyDict_SetItem(widgetClass, providerId, PyDict_New());
        
        // Now get the real class
        widgetClass = PyDict_GetItemString(widgetClass, "class");
    } else {
        // Building data structure
        PyObject* data = PyDict_New();
        PyDict_SetItemString(data, "class", widgetClass);
        PyDict_SetItem(data, providerId, PyDict_New());
        
        // Storing data
        LOGW("C: Storing data in cache...");
        PyDict_SetItem(_providerList, providerClass, data);
    }
    
    // Set the initAction, if it hasn't been set yet
    if ((widgetClass != NULL) && PyObject_HasAttrString(widgetClass, "init_action")) {
        LOGW("Setting widget init action...");
        PythonWidgets_setInitAction(NULL, Py_BuildValue("(OOO)", jprovider, PyObject_GetAttrString(widgetClass, "init_action"), Py_False));
    }
    
    LOGW("C: Done.");
    //PyObject_CallMethod(PyImport_AddModule("__builtin__"), "print", "(s)", "######python print######");
    //PyObject_CallMethod(PyImport_AddModule("__builtin__"), "print", "(O)", _providerList);
    return;
}

JNIEXPORT void JNICALL Java_org_renpy_android_PythonWidgetProvider_nativeEndProvider(JNIEnv* env, jobject obj, jint jProviderId, jstring jProviderClass) {
    LOGW("C: Destroy Widget Provider...");
    if (!INIT_SUCCESS) {LOGW("C: PythonWidget is not correctly initialized!"); return;}
    
    PyObject* providerId = Py_BuildValue("i", jProviderId);
    const char* cProviderClass = (*env)->GetStringUTFChars(env, jProviderClass, 0);
    PyObject* providerClass = PyString_FromString(cProviderClass);
    LOGW(cProviderClass);
    
    if (PyDict_Check(_providerList) && (PyDict_Contains(_providerList, providerClass) == 1)) {
        PyObject* provider = PyDict_GetItem(_providerList, providerClass);
        if (PyDict_Check(provider)) {
            if (PyDict_Size(provider) == 2) { // If this provider is the only one
                if (PyDict_SetItem(_providerList, providerClass, PyDict_GetItemString(PyDict_GetItem(_providerList, providerClass), "class")) == 0) {
                    LOGW("C: Successfully deleted widget provider.");
                } else {
                    LOGW("C: Failed to destroy widget provider!");
                    PyErr_Print();
                }
            } else { // If there are any other subwidgets provider who are still active
                LOGW("C: There is another provider active who needs this widgetClass.");
                if (PyDict_DelItem(provider, providerId) != -1) { // We just delete this provider and leave the other untouched.
                    LOGW("C: Successfully deleted widget provider.");
                } else {
                    LOGW("C: Failed to destroy widget provider!");
                }
            }
        } else {
            LOGW("C: Could not destroy provider because he is not initialized.");
        }
    } else {
        LOGW("C: Unable to destroy widget provider: Unable to find it in cache!");
    }
    //PyObject_CallMethod(PyImport_AddModule("__builtin__"), "print", "(s)", "######python print######");
    //PyObject_CallMethod(PyImport_AddModule("__builtin__"), "print", "(O)", _providerList);
    (*env)->ReleaseStringUTFChars(env, jProviderClass, cProviderClass);
}

JNIEXPORT jboolean JNICALL Java_org_renpy_android_PythonWidgetProvider_nativeInitWidget(JNIEnv* env, jobject obj, jint jProviderId, jstring jProviderClass, jint WidgetId) {
    LOGW("C: Init Widget...");
    if (!INIT_SUCCESS) {LOGW("C: PythonWidget is not correctly initialized!"); return JNI_FALSE;}
    PyEval_InitThreads();
    
    PyObject* jprovider = PyCapsule_New((void *) &obj, NULL, NULL);
    if (jprovider == NULL) {
        LOGW("C: Failed to convert java provider pointer to python object!");
        PyErr_Print();
        return JNI_FALSE;
    }
    PyObject* providerId = Py_BuildValue("i", jProviderId);
    const char* cProviderClass = (*env)->GetStringUTFChars(env, jProviderClass, 0);
    PyObject* providerClass = PyString_FromString(cProviderClass);
    LOGW(cProviderClass);
    (*env)->ReleaseStringUTFChars(env, jProviderClass, cProviderClass);
    PyObject* provider = PyDict_GetItem(_providerList, providerClass);
    
    //PyObject_CallMethod(PyImport_AddModule("__builtin__"), "print", "(s)", "######python print######");
    //PyObject_CallMethod(PyImport_AddModule("__builtin__"), "print", "(O)", _providerList);
    
    if (provider == NULL) {
        LOGW("Failed to extract widget class from cache: Not found!");
        return JNI_FALSE;
    }
    if (PyDict_Contains(provider, providerId) != 1) {
        LOGW("C: The requested provider is not initialized or has no cache!");
        return JNI_FALSE;
    }
    // Get the widget provider class
    PyObject* widgetClass = PyDict_GetItemString(provider, "class");
    if (widgetClass != NULL) {
        // Setting the pointer to the java provider object
        PyObject_SetAttrString(widgetClass, "_provider", jprovider);
        // Making an instance of the provider class
        LOGW("C: Trying to instantiate the widget class...");
        PyObject* widgetInstance = PyObject_Call(widgetClass, Py_BuildValue("(i)", WidgetId), Py_BuildValue("{}"));
        // Deleting the pointer to the java provider object
        PyObject_SetAttrString(widgetClass, "_provider", Py_None);
        if (widgetInstance == NULL) {
            LOGW("C: Failed! Could not create an instance of this widget class!");
            PyErr_Print();
            return JNI_FALSE;
        }
        LOGW("C: Done. Caching instance to cache...");
        if (PyDict_SetItem(PyDict_GetItem(provider, providerId), Py_BuildValue("i", WidgetId), widgetInstance) == -1) {
            LOGW("C: Failed to insert instance in Python cache!");
            PyErr_Print();
            return JNI_FALSE;
        }
        LOGW("Done.");
        return JNI_TRUE;
    }
    LOGW("C: Failed. Our WidgetProvider instance has no attribute 'initWidget'!");
    return JNI_FALSE;
}

JNIEXPORT void JNICALL Java_org_renpy_android_PythonWidgetProvider_nativeUpdateWidget(JNIEnv* env, jobject obj, jint jProviderId, jstring jProviderClass, jint WidgetId, jint UpdateType) {
    LOGW("C: Update Widget...");
    if (!INIT_SUCCESS) {LOGW("C: PythonWidget is not correctly initialized!"); return;}
    PyEval_InitThreads();
    
    PyObject* jprovider = PyCapsule_New((void *) &obj, NULL, NULL);
    if (jprovider == NULL) {
        LOGW("C: Failed to convert java provider pointer to python object!");
        PyErr_Print();
        return;
    }
    PyObject* providerId = Py_BuildValue("i", jProviderId);
    const char* cProviderClass = (*env)->GetStringUTFChars(env, jProviderClass, 0);
    PyObject* providerClass = PyString_FromString(cProviderClass);
    LOGW(cProviderClass);
    (*env)->ReleaseStringUTFChars(env, jProviderClass, cProviderClass);
    PyObject* provider = PyDict_GetItem(_providerList, providerClass);
    
    if (provider == NULL) {
        LOGW("Failed to extract widget class from cache: Not found!");
        return;
    }
    if (PyDict_Contains(provider, providerId) != 1) {
        LOGW("C: The requested provider is not initialized or has no cache!");
        return;
    }
    LOGW("Getting widget instance from cache.");
    PyObject* widgetInstance = PyDict_GetItem(PyDict_GetItem(provider, providerId), Py_BuildValue("i", WidgetId));
    if (widgetInstance == NULL) {
        LOGW("C: Did not found the widgets instance in the cache!");
        return; 
    }
    if (PyObject_HasAttrString(widgetInstance, "updateWidget") == 1) {
        // Setting the pointer to the java provider object
        PyObject_SetAttrString(widgetInstance, "_provider", jprovider);
        PyObject *res = PyObject_Call(PyObject_GetAttrString(widgetInstance, "updateWidget"), Py_BuildValue("()"), Py_BuildValue("{si}", "updateType", UpdateType));
        // Deleting the pointer to the java provider object
        PyObject_SetAttrString(widgetInstance, "_provider", Py_None);
        if (res == NULL) {
            LOGW("C: Failed to execute updateWidget!");
            PyErr_Print();
            return;
        }
        LOGW("C: Successfully executed updateWidget.");
        return;
    }
    LOGW("C: Failed. Our WidgetProvider instance has no attribute 'updateWidget'!");
    return;
}

JNIEXPORT void JNICALL Java_org_renpy_android_PythonWidgetProvider_nativeDestroyWidget(JNIEnv* env, jobject obj, jint jProviderId, jstring jProviderClass, jint WidgetId) {
    LOGW("C: Destroing Widget...");
    if (!INIT_SUCCESS) {LOGW("C: PythonWidget is not correctly initialized!"); return;}
    PyEval_InitThreads();
    
    PyObject* jprovider = PyCapsule_New((void *) &obj, NULL, NULL);
    if (jprovider == NULL) {
        LOGW("C: Failed to convert java provider pointer to python object!");
        PyErr_Print();
        return;
    }
    PyObject* providerId = Py_BuildValue("i", jProviderId);
    const char* cProviderClass = (*env)->GetStringUTFChars(env, jProviderClass, 0);
    PyObject* providerClass = PyString_FromString(cProviderClass);
    LOGW(cProviderClass);
    (*env)->ReleaseStringUTFChars(env, jProviderClass, cProviderClass);
    PyObject* provider = PyDict_GetItem(_providerList, providerClass);
    
    if (provider == NULL) {
        LOGW("Failed to extract widget class from cache: Not found!");
        return;
    }
    if (PyDict_Check(provider) && (PyDict_Contains(provider, providerId) != 1)) {
        LOGW("C: The requested provider is not initialized or has no cache!");
        return;
    }
    PyObject* widgetInstance = PyDict_GetItem(PyDict_GetItem(provider, providerId), Py_BuildValue("i", WidgetId));
    if (widgetInstance == NULL) {
        LOGW("C: Did not found the widget instance in the cache!");
        return; 
    }
    if (PyObject_HasAttrString(widgetInstance, "destroyWidget") == 1) {
        // Setting the pointer to the java provider object
        PyObject_SetAttrString(widgetInstance, "_provider", jprovider);
        PyObject *res = PyObject_Call(PyObject_GetAttrString(widgetInstance, "destroyWidget"), Py_BuildValue("()"), Py_BuildValue("{}"));
        // Deleting the pointer to the java provider object
        PyObject_SetAttrString(widgetInstance, "_provider", Py_None);
        if (res != NULL) {
            LOGW("C: Successfully executed destroyWidget.");
            return;
        }
        LOGW("C: Failed to execute destroyWidget!");
        PyErr_Print();
    } else {
        LOGW("C: Failed. Our WidgetProvider instance has no attribute 'destroyWidget'!");
    }
    PyDict_DelItem(PyDict_GetItem(provider, providerId), Py_BuildValue("i", WidgetId));
    return;
}

JNIEXPORT void JNICALL Java_org_renpy_android_PythonWidgetProvider_nativePythonCallback(JNIEnv* env, jobject obj, jint jProviderId, jstring jProviderClass, jint widgetId, jstring jFunctionId, jint argument) {
    LOGW("C: Calling python callback function...");
    if (!INIT_SUCCESS) {LOGW("C: PythonWidget is not correctly initialized!"); return;}
    PyEval_InitThreads();
    
    const char* cProviderClass = (*env)->GetStringUTFChars(env, jProviderClass, 0);
    const char* cFunctionId    = (*env)->GetStringUTFChars(env, jFunctionId, 0);
    PyObject* jprovider = PyCapsule_New((void *) &obj, NULL, NULL);
    if (jprovider == NULL) {
        LOGW("C: Failed to convert java provider pointer to python object!");
        PyErr_Print();
        return;
    }
    PyObject* providerClass    = PyString_FromString(cProviderClass);
    PyObject* functionId       = PyString_FromString(cFunctionId);
    LOGW(cProviderClass);
    LOGW("C: Function id is:");
    LOGW(cFunctionId);
    (*env)->ReleaseStringUTFChars(env, jProviderClass, cProviderClass);
    (*env)->ReleaseStringUTFChars(env, jFunctionId, cFunctionId);
    PyObject* provider         = PyDict_GetItem(_providerList, providerClass);
    PyObject* providerId       = Py_BuildValue("i", jProviderId);
    PyObject* pyWidgetId       = Py_BuildValue("i", widgetId);
    PyObject* providerCache    = NULL;
    PyObject* res              = NULL;
    PyObject* widgetInstance   = NULL;
    
    if (provider == NULL) {
        LOGW("Failed to extract widget class from cache: Not found!");
        return;
    }
    providerCache = PyDict_GetItem(provider, providerId);
    
    if (providerCache == NULL) {
        LOGW("C: The requested provider is not initialized or has no cache!");
        return;
    }
    
    widgetInstance = PyDict_GetItem(providerCache, pyWidgetId);
    if (widgetInstance == NULL) {
        LOGW("C: The requested provider does not have a widget with the given id!");
        return;
    } else if (PyDict_Check(_callbackList) && (PyDict_Contains(_callbackList, functionId) == 1)) {
        // Setting the pointer to the java provider object
        PyObject_SetAttrString(widgetInstance, "_provider", jprovider);
	if (argument != -1) {
	    // If this is a callback from an item in a list, we must pass down the index
	    res = PyObject_Call(PyDict_GetItem(_callbackList, functionId), Py_BuildValue("()"), Py_BuildValue("{si}", "child_index", argument));
	} else {
	    res = PyObject_Call(PyDict_GetItem(_callbackList, functionId), Py_BuildValue("()"), Py_BuildValue("{}"));
	}
        // Deleting the pointer to the java provider object
        PyObject_SetAttrString(widgetInstance, "_provider", Py_None);
        if (res != NULL) {
            LOGW("C: Successfully executed python callback.");
            return;
        }
        LOGW("C: Failed to execute python callback!");
        PyErr_Print();
    } else {
        LOGW("C: Failed. No callback found!");
    }
    return;
}


// PythonWidgets Module //

static PyObject *PythonWidgets_updateWidget(PyObject *self, PyObject *args) {
    LOGW("C: PythonWidgets_updateWidget is getting called...");
    
    static JNIEnv *env = NULL;
    static jclass *cls = NULL;
    static jmethodID mid = NULL;
    
    
    int widget_Id;
    const char *widgetview;
    PyObject *widgetProvider;
    jobject jwidgetProvider = NULL;
    
    if (!PyArg_ParseTuple(args, "Oiz", &widgetProvider, &widget_Id, &widgetview)) {
        LOGW("C: (updateWidget) Failed to extract the java_provider / the widget Id / the new widget view as a string from the arguments!");
        PyErr_Print();
        Py_RETURN_FALSE;
    }
    
    // Get the java object whose functions we need to call
    if (PyCapsule_CheckExact(widgetProvider)) {
        jobject* temp = (jobject*) PyCapsule_GetPointer(widgetProvider, NULL);
        if (temp == NULL) {
            LOGW("C: Unable to get the java provider from the argument!");
            PyErr_Print();
            Py_RETURN_FALSE;
        }
        jwidgetProvider = *temp;
    } else {
        LOGW("C: Java provider for this widget is not available (Given argument is not an encapsulated pointer)!");
        Py_RETURN_FALSE;
    }
    
    //Debug
    LOGW("C: Passed argument widget_Id is");
    char str[15];
    sprintf(str, "%d", widget_Id);
    LOGW(str);
    //
    
    if (env == NULL) {
        env = GetJNIEnv();
        msgassert(env, "Could not obtain the current environment!");
        cls = (*env)->GetObjectClass(env, jwidgetProvider); // These methods are non static
        msgassert(cls, "Could not get a reference to 'org/renpy/android/PythonWidgetProvider'!");
        mid = (*env)->GetMethodID(env, cls, "updateWidget", "(ILjava/lang/String;)V");
        msgassert(mid, "Could not find the function 'updateWidget' in the Android class!");
    }
    
    LOGW("C: Calling java function updateWidget...");
    (*env)->CallVoidMethod(env, jwidgetProvider, mid, widget_Id, (*env)->NewStringUTF(env, widgetview));
    LOGW("C: back from Java, returning to Python...");
    Py_RETURN_TRUE;
}

static PyObject *PythonWidgets_existWidget(PyObject *self, PyObject *args) {
    LOGW("C: PythonWidgets_existWidget is getting called...");
    
    static JNIEnv *env = NULL;
    static jclass *cls = NULL;
    static jmethodID mid = NULL;
    jobject jwidgetProvider = NULL;
    
    int widget_Id;
    PyObject * widgetProvider;
    
    if (!PyArg_ParseTuple(args, "Oi", &widgetProvider, &widget_Id)) {
        LOGW("C: (existWidget) Failed to extract the java_provider / the widget Id from the arguments!");
        PyErr_Print();
        Py_RETURN_FALSE;
    }
    
    // Get the java object whose functions we need to call
    if (PyCapsule_CheckExact(widgetProvider)) {
        jobject* temp = (jobject*) PyCapsule_GetPointer(widgetProvider, NULL);
        if (temp == NULL) {
            LOGW("C: Unable to get the java provider from the argument!");
            PyErr_Print();
            Py_RETURN_FALSE;
        }
        jwidgetProvider = *temp;
    } else {
        LOGW("C: Java provider for this widget is not available (Given argument is not an encapsulated pointer)!");
        Py_RETURN_FALSE;
    }
    
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
        cls = (*env)->GetObjectClass(env, jwidgetProvider);
        msgassert(cls, "Could not get an instance from class 'org/renpy/android/PythonWidgetProvider'!");
        mid = (*env)->GetMethodID(env, cls, "existWidget", "(I)Z");
        msgassert(mid, "Could not find the function 'existWidget' in the Android class!");
    }
    
    LOGW("C: Calling java function existWidget...");
    jboolean res = (*env)->CallBooleanMethod(env, jwidgetProvider, mid, widget_Id);
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

static PyObject *PythonWidgets_storeWidgetData(PyObject *self, PyObject *args) {
    LOGW("C: PythonWidgets_storeWidgetData is getting called...");
    
    static JNIEnv *env   = NULL;
    jclass *cls   = NULL;
    jmethodID mid = NULL;
    const char* data     = NULL;
    const char* key      = NULL;
    jstring jdata        = NULL;
    jstring jkey         = NULL;
    
    PyObject* data_dict;
    if (!PyArg_ParseTuple(args, "sz", &key, &data)) {
        LOGW("C: (storeWidgetData) Failed to extract the key / the data-string from the arguments!");
        PyErr_Print();
        Py_RETURN_FALSE;
    }
    
    if (env == NULL) {
        LOGW("Trying to get env pointer.");
        env = GetJNIEnv();
        msgassert(env, "Could not obtain the current environment!");
    }
    cls = (*env)->FindClass(env, "org/renpy/android/PythonWidgetProvider");
    msgassert(cls, "Could not get a reference from class 'org/renpy/android/PythonWidgetProvider'!");
    mid = (*env)->GetStaticMethodID(env, cls, "storeWidgetData", "(Ljava/lang/String;Ljava/lang/String;)V");
    msgassert(mid, "Could not find the function 'storeWidgetData' in the PythonWidgetProvider class!");
    
    if (data == NULL) {
        LOGW("C: Given data is None.");
        jdata = NULL;
    } else {
        LOGW(data);
        LOGW("C: Converting C data-string to java string...");
        jdata = (*env)->NewStringUTF(env, data);
        LOGW("C: Done.");
    }
    jkey = (*env)->NewStringUTF(env, key);
    
    LOGW("C: Calling java function storeWidgetData...");
    (*env)->CallStaticVoidMethod(env, cls, mid, jkey, jdata);
    LOGW("C: back from Java, returning to Python...");
    Py_RETURN_TRUE;
}

static PyObject *PythonWidgets_getWidgetData(PyObject *self, PyObject *args) {
    LOGW("C: PythonWidgets_getWidgetData is getting called...");
    
    static JNIEnv *env   = NULL;
    jclass *cls          = NULL;
    jmethodID mid        = NULL;
    const char* key      = NULL;
    jstring jkey         = NULL;
    
    PyObject* data_dict;
    if (!PyArg_ParseTuple(args, "s", &key)) {
        LOGW("C: (getWidgetData) Failed to extract the key from the arguments!");
        PyErr_Print();
        Py_RETURN_FALSE;
    }
    
    if (env == NULL) {
        LOGW("Trying to get env pointer.");
        env = GetJNIEnv();
        msgassert(env, "Could not obtain the current environment!");
    }
    cls = (*env)->FindClass(env, "org/renpy/android/PythonWidgetProvider");
    msgassert(cls, "Could not get a reference from class 'org/renpy/android/PythonWidgetProvider'!");
    mid = (*env)->GetStaticMethodID(env, cls, "getWidgetData", "(Ljava/lang/String;)Ljava/lang/String;");
    msgassert(mid, "Could not find the function 'getWidgetData' in the PythonWidgetProvider class!");
    
    LOGW("C: Converting key c-string to a java-string...");
    jkey = (*env)->NewStringUTF(env, key);
    LOGW("C: Calling java function getWidgetData...");
    jstring res = (*env)->CallStaticObjectMethod(env, cls, mid, jkey);
    LOGW("C: Back from Java, returning to Python...");
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
    PyObject *id         = NULL;
    PyObject *callback   = NULL;
    
    if (!PyArg_ParseTuple(args, "OO", &id, &callback)) {
        LOGW("C: (addOnClick) Could not extract the id / the callback from the given arguments!");
        PyErr_Print();
        Py_RETURN_FALSE;
    }
    
    if (!PyDict_Check(_callbackList)) {
        LOGW("C: Adding callback to callback-list failed: Callback-list is not a dict (maybe uninitialized)!");
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

static PyObject *PythonWidgets_setInitAction(PyObject *self, PyObject *args) {
    LOGW("C: PythonWidgets_setInitAction is getting called...");
    
    static JNIEnv *env    = NULL;
    static jclass *cls    = NULL;
    static jmethodID mid  = NULL;
    PyObject *action      = NULL;
    const char* cAction   = NULL;
    PyObject *withForce   = Py_True;
    PyObject *widgetProvider;
    jobject jwidgetProvider = NULL;
    
    if (!PyArg_ParseTuple(args, "OO|O", &widgetProvider, &action, &withForce)) {
        LOGW("C: (setInitAction) Could not extract the java provider / the action [/ the force argument] from the given arguments!");
        PyErr_Print();
        Py_RETURN_FALSE;
    }
    
    // Get the java object whose functions we need to call
    if (PyCapsule_CheckExact(widgetProvider)) {
        jobject* temp = (jobject*) PyCapsule_GetPointer(widgetProvider, NULL);
        if (temp == NULL) {
            LOGW("C: Unable to get the java provider from the argument!");
            PyErr_Print();
            Py_RETURN_FALSE;
        }
        jwidgetProvider = *temp;
    } else {
        LOGW("C: Java provider for this widget is not available (Given argument is not an encapsulated pointer)!");
        Py_RETURN_FALSE;
    }
    
    if (env == NULL) {
        LOGW("Trying to get env pointer.");
        env = GetJNIEnv();
        msgassert(env, "Could not obtain the current environment!");
        cls = (*env)->GetObjectClass(env, jwidgetProvider);
        msgassert(cls, "Could not get an instance from class 'org/renpy/android/PythonWidgetProvider'!");
        mid = (*env)->GetMethodID(env, cls, "setInitAction", "(Ljava/lang/String;Z)V");
        msgassert(mid, "Could not find the function 'setInitAction' in the Android class!");
    }
    
    if (action == Py_None) {
        cAction = "None";
    } else if (action == Py_True) {
        cAction = "True";
    } else if (PyString_Check(action)) {
        PyObject *tmp = PyString_FromString("argv:");
        PyString_Concat(&tmp, action);
        cAction = PyString_AsString(tmp);
    } else if (PyDict_Check(action)) {
        action = urlencode(action);
        if (action == Py_False) {
            LOGW("C: Urlencoding failed, returning!");
            Py_RETURN_FALSE;
        }
        cAction = PyString_AsString(action);
    } else {
        LOGW("C: Given action is not a str, bool, int");
        Py_RETURN_FALSE;
    }
    LOGW("C: Action:");
    LOGW(cAction);
    
    jboolean force = JNI_TRUE;
    if (withForce == Py_False) {
        force = JNI_FALSE;
    }
    
    LOGW("C: Calling java function setInitAction...");
    (*env)->CallVoidMethod(env, jwidgetProvider, mid, (*env)->NewStringUTF(env, cAction), force);
    LOGW("C: back from Java, returning to Python...");
    
    Py_RETURN_TRUE;
}

static PyObject *PythonWidgets_setSingleUpdate(PyObject *self, PyObject *args) {
    LOGW("C: PythonWidgets_setSingleUpdate is getting called...");
    
    static JNIEnv *env    = NULL;
    static jclass *cls    = NULL;
    static jmethodID mid  = NULL;
    PyObject *widgetProvider;
    jobject jwidgetProvider = NULL;
    int widget_Id = -1;
    int timeDiff  = -1;
    
    if (!PyArg_ParseTuple(args, "Oii", &widgetProvider, &widget_Id, &timeDiff)) {
        LOGW("C: (setSingleUpdate) Could not extract the java provider / the widget Id / the time difference from the given arguments!");
        PyErr_Print();
        Py_RETURN_FALSE;
    }
    
    if (timeDiff <= 0) {
        LOGW("C: [ERROR] Could not set up onetime update, the time difference must be greater than zero!");
        Py_RETURN_FALSE;
    }
    
    // Get the java object whose functions we need to call
    if (PyCapsule_CheckExact(widgetProvider)) {
        jobject* temp = (jobject*) PyCapsule_GetPointer(widgetProvider, NULL);
        if (temp == NULL) {
            LOGW("C: Unable to get the java provider from the argument!");
            PyErr_Print();
            Py_RETURN_FALSE;
        }
        jwidgetProvider = *temp;
    } else {
        LOGW("C: Java provider for this widget is not available (Given argument is not an encapsulated pointer)!");
        Py_RETURN_FALSE;
    }
    
    if (env == NULL) {
        LOGW("Trying to get env pointer.");
        env = GetJNIEnv();
        msgassert(env, "Could not obtain the current environment!");
        cls = (*env)->GetObjectClass(env, jwidgetProvider);
        msgassert(cls, "Could not get an instance from class 'org/renpy/android/PythonWidgetProvider'!");
        mid = (*env)->GetMethodID(env, cls, "setSingleUpdate", "(II)V");
        msgassert(mid, "Could not find the function 'setSingleUpdate' in the Android class!");
    }
    
    LOGW("C: Calling java function setSingleUpdate...");
    (*env)->CallVoidMethod(env, jwidgetProvider, mid, widget_Id, timeDiff);
    LOGW("C: back from Java, returning to Python...");
    
    Py_RETURN_TRUE;
}

static PyObject *PythonWidgets_setPeriodicUpdateFreq(PyObject *self, PyObject *args) {
    LOGW("C: PythonWidgets_setPeriodicUpdateFreq is getting called...");
    
    static JNIEnv *env    = NULL;
    static jclass *cls    = NULL;
    static jmethodID mid  = NULL;
    PyObject *widgetProvider;
    jobject jwidgetProvider = NULL;
    int widget_Id = -1;
    int freq      = -1;
    
    if (!PyArg_ParseTuple(args, "Oii", &widgetProvider, &widget_Id, &freq)) {
        LOGW("C: (setPeriodicUpdateFreq) Could not extract the java provider / the widget Id / the frequency from the given arguments!");
        PyErr_Print();
        Py_RETURN_FALSE;
    }
    
    // Get the java object whose functions we need to call
    if (PyCapsule_CheckExact(widgetProvider)) {
        jobject* temp = (jobject*) PyCapsule_GetPointer(widgetProvider, NULL);
        if (temp == NULL) {
            LOGW("C: Unable to get the java provider from the argument!");
            PyErr_Print();
            Py_RETURN_FALSE;
        }
        jwidgetProvider = *temp;
    } else {
        LOGW("C: Java provider for this widget is not available (Given argument is not an encapsulated pointer)!");
        Py_RETURN_FALSE;
    }
    
    if (env == NULL) {
        LOGW("Trying to get env pointer.");
        env = GetJNIEnv();
        msgassert(env, "Could not obtain the current environment!");
        cls = (*env)->GetObjectClass(env, jwidgetProvider);
        msgassert(cls, "Could not get an instance from class 'org/renpy/android/PythonWidgetProvider'!");
        mid = (*env)->GetMethodID(env, cls, "setPeriodicUpdateFreq", "(II)V");
        msgassert(mid, "Could not find the function 'setPeriodicUpdateFreq' in the Android class!");
    }
    
    LOGW("C: Calling java function setPeriodicUpdateFreq...");
    (*env)->CallVoidMethod(env, jwidgetProvider, mid, widget_Id, freq);
    LOGW("C: back from Java, returning to Python...");
    
    Py_RETURN_TRUE;
}

static PyObject *PythonWidgets_getPeriodicUpdateFreq(PyObject *self, PyObject *args) {
    LOGW("C: PythonWidgets_getPeriodicUpdateFreq is getting called...");
    
    static JNIEnv *env    = NULL;
    static jclass *cls    = NULL;
    static jmethodID mid  = NULL;
    PyObject *widgetProvider;
    jobject jwidgetProvider = NULL;
    int widget_Id = -1;
    int freq      = -1;
    
    if (!PyArg_ParseTuple(args, "Oi", &widgetProvider, &widget_Id)) {
        LOGW("C: (getPeriodicUpdateFreq) Could not extract the java provider / the widget Id from the given arguments!");
        PyErr_Print();
        Py_RETURN_FALSE;
    }
    
    // Get the java object whose functions we need to call
    if (PyCapsule_CheckExact(widgetProvider)) {
        jobject* temp = (jobject*) PyCapsule_GetPointer(widgetProvider, NULL);
        if (temp == NULL) {
            LOGW("C: Unable to get the java provider from the argument!");
            PyErr_Print();
            Py_RETURN_FALSE;
        }
        jwidgetProvider = *temp;
    } else {
        LOGW("C: Java provider for this widget is not available (Given argument is not an encapsulated pointer)!");
        Py_RETURN_FALSE;
    }
    
    if (env == NULL) {
        LOGW("Trying to get env pointer.");
        env = GetJNIEnv();
        msgassert(env, "Could not obtain the current environment!");
        cls = (*env)->GetObjectClass(env, jwidgetProvider);
        msgassert(cls, "Could not get an instance from class 'org/renpy/android/PythonWidgetProvider'!");
        mid = (*env)->GetMethodID(env, cls, "getPeriodicUpdateFreq", "(I)I");
        msgassert(mid, "Could not find the function 'getPeriodicUpdateFreq' in the Android class!");
    }
    
    LOGW("C: Calling java function getPeriodicUpdateFreq...");
    freq = (*env)->CallIntMethod(env, jwidgetProvider, mid, widget_Id);
    LOGW("C: back from Java, returning to Python...");
    
    return Py_BuildValue("i", freq);
}

static PyObject *PythonWidgets_startMainApp(PyObject *self, PyObject *args) {
    LOGW("C: PythonWidgets_startMainApp is getting called...");
    
    static JNIEnv *env      = NULL;
    static jclass *cls      = NULL;
    static jmethodID mid    = NULL;
    PyObject *widgetProvider;
    jobject jwidgetProvider = NULL;
    PyObject *argsList 	    = NULL;
    jobjectArray argv       = NULL;
    
    if (!PyArg_ParseTuple(args, "OO", &widgetProvider, &argsList)) {
        LOGW("C: (startMainApp) Could not extract the java provider or the command line arguments from the given arguments!");
        PyErr_Print();
        Py_RETURN_FALSE;
    }
    
    // Get the java object whose functions we need to call
    if (PyCapsule_CheckExact(widgetProvider)) {
        jobject* temp = (jobject*) PyCapsule_GetPointer(widgetProvider, NULL);
        if (temp == NULL) {
            LOGW("C: Unable to get the java provider from the argument!");
            PyErr_Print();
            Py_RETURN_FALSE;
        }
        jwidgetProvider = *temp;
    } else {
        LOGW("C: Java provider for this widget is not available (Given argument is not an encapsulated pointer)!");
        Py_RETURN_FALSE;
    }
    
    if (env == NULL) {
        LOGW("Trying to get env pointer.");
        env = GetJNIEnv();
        msgassert(env, "Could not obtain the current environment!");
        cls = (*env)->GetObjectClass(env, jwidgetProvider);
        msgassert(cls, "Could not get an instance from class 'org/renpy/android/PythonWidgetProvider'!");
        mid = (*env)->GetMethodID(env, cls, "startMainApp", "([Ljava/lang/String;)V");
        msgassert(mid, "Could not find the function 'startMainApp' in the Android class!");
    }
    
    if (PyList_Check(argsList)) {
	int length = PyList_Size(argsList);
	argv = (jobjectArray) (*env)->NewObjectArray(env, length, (*env)->FindClass(env, "java/lang/String"), (*env)->NewStringUTF(env, ""));
	int i;
	for (i = 0; i < length; i++) {
	    (*env)->SetObjectArrayElement(env, argv, i, (*env)->NewStringUTF(env, PyString_AsString(PyList_GetItem(argsList, i))));
	}
    }
    
    LOGW("C: Calling java function startMainApp...");
    (*env)->CallVoidMethod(env, jwidgetProvider, mid, argv);
    LOGW("C: back from Java, returning to Python...");
    
    Py_RETURN_TRUE;
}

// TODO: Add getWidgetCount
static PyMethodDef module_methods[] = {
    { "updateWidget",          PythonWidgets_updateWidget,          METH_VARARGS, "Updates the widget with the id 'widget_Id' using the view 'widgetview'.\nYou need to call this function in order to make your\nchanges to the widgets graphics visible on the screen." },
    { "existWidget",           PythonWidgets_existWidget,           METH_VARARGS, "Check's if a widget with the id 'widget_Id' was already registered." },
    { "storeWidgetData",       PythonWidgets_storeWidgetData,       METH_VARARGS, "Stores the given dict 'data' in the key 'key' and makes it accessible via getWidgetData." },
    { "getWidgetData",         PythonWidgets_getWidgetData,         METH_VARARGS, "Returns the data stored in the given key 'key' as a urlencoded string that was passed to\n'storeWidgetData' or None, if there is no data stored in that key." },
    { "addOnClick",            PythonWidgets_addOnClickCallb,       METH_VARARGS, "Adds the given callback 'callback' to the identifier 'id'." },
    { "setInitAction",         PythonWidgets_setInitAction,         METH_VARARGS, "Sets the action that occurs before initializion of the widgets." },
    { "setSingleUpdate",       PythonWidgets_setSingleUpdate,       METH_VARARGS, "Initializes a one time update for the widget with the id 'widget_Id' in 'timeDiff' milliseconds." },
    { "setPeriodicUpdateFreq", PythonWidgets_setPeriodicUpdateFreq, METH_VARARGS, "Starts a timer to update the widget with the is 'widget_Id' every 'freq' milliseconds." },
    { "getPeriodicUpdateFreq", PythonWidgets_getPeriodicUpdateFreq, METH_VARARGS, "Gets the frequency in milliseconds a timer currently periodically updates\nthe widget with the id 'widget_Id' or -1, if there is no such timer." },
    { "startMainApp",          PythonWidgets_startMainApp,          METH_VARARGS, "Starts the main app that is associated whith this widget. Passes 'argv' as argumants to the App, if 'argv' is a list of strings." },
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
