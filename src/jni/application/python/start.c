#define PY_SSIZE_T_CLEAN
#include "Python.h"
#ifndef Py_PYTHON_H
    #error Python headers needed to compile C extensions, please install development version of Python.
#else

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <jni.h>
#include "SDL.h"
#include "android/log.h"
#include "jniwrapperstuff.h"

#define LOG(x) __android_log_write(ANDROID_LOG_INFO, "python", (x))
#define msgassert(x, message) { if (!x) { __android_log_print(ANDROID_LOG_ERROR, "android_jni", "Assertion failed: %s. %s:%d", message, __FILE__, __LINE__); abort(); }}

static PyObject *androidembed_log(PyObject *self, PyObject *args) {
    char *logstr = NULL;
    if (!PyArg_ParseTuple(args, "s", &logstr)) {
        return NULL;
    }
    LOG(logstr);
    Py_RETURN_NONE;
}

static PyMethodDef AndroidEmbedMethods[] = {
    {"log", androidembed_log, METH_VARARGS,
     "Log on android platform"},
    {NULL, NULL, 0, NULL}
};

PyMODINIT_FUNC initandroidembed(void) {
    (void) Py_InitModule("androidembed", AndroidEmbedMethods);
}

int file_exists(const char * filename)
{
	FILE *file;
    if (file = fopen(filename, "r")) {
        fclose(file);
        return 1;
    }
    return 0;
}







JNIEnv *SDL_ANDROID_GetJNIEnv(void);

static PyObject *PythonWidgets_storeWidgetData(PyObject *self, PyObject *args) {
    LOG("C: PythonWidgets_storeWidgetData is getting called...");
    
    static JNIEnv *env   = NULL;
    jclass *cls   = NULL;
    jmethodID mid = NULL;
    const char* data     = NULL;
    const char* key      = NULL;
    jstring jdata        = NULL;
    jstring jkey         = NULL;
    
    PyObject* data_dict;
    if (!PyArg_ParseTuple(args, "sz", &key, &data)) {
        LOG("C: (storeWidgetData) Failed to extract the key and / or the data-string from the arguments!");
        PyErr_Print();
        Py_RETURN_FALSE;
    }
    
    if (env == NULL) {
        LOG("Trying to get env pointer.");
        env = SDL_ANDROID_GetJNIEnv();
        msgassert(env, "Could not obtain the current environment!");
    }
    cls = (*env)->FindClass(env, "org/renpy/android/PythonWidgetProvider");
    msgassert(cls, "Could not get a reference from class 'org/renpy/android/PythonWidgetProvider'!");
    mid = (*env)->GetStaticMethodID(env, cls, "storeWidgetData", "(Ljava/lang/String;Ljava/lang/String;)V");
    msgassert(mid, "Could not find the function 'storeWidgetData' in the PythonWidgetProvider class!");
    
    if (data == NULL) {
        LOG("C: Given data is None.");
        jdata = NULL;
    } else {
        LOG(data);
        LOG("C: Converting C data-string to java string...");
        jdata = (*env)->NewStringUTF(env, data);
        LOG("C: Done.");
    }
    jkey = (*env)->NewStringUTF(env, key);
    
    LOG("C: Calling java function storeWidgetData...");
    (*env)->CallStaticVoidMethod(env, cls, mid, jkey, jdata);
    LOG("C: back from Java, returning to Python...");
    Py_RETURN_TRUE;
}

static PyObject *PythonWidgets_getWidgetData(PyObject *self, PyObject *args) {
    LOG("C: PythonWidgets_getWidgetData is getting called...");
    
    static JNIEnv *env   = NULL;
    jclass *cls   = NULL;
    jmethodID mid = NULL;
    const char* key      = NULL;
    jstring jkey         = NULL;
    
    PyObject* data_dict;
    if (!PyArg_ParseTuple(args, "s", &key)) {
        LOG("C: (getWidgetData) Failed to extract the key from the arguments!");
        PyErr_Print();
        Py_RETURN_FALSE;
    }
    
    if (env == NULL) {
        LOG("Trying to get env pointer.");
        env = SDL_ANDROID_GetJNIEnv();
        msgassert(env, "Could not obtain the current environment!");
    }
    cls = (*env)->FindClass(env, "org/renpy/android/PythonWidgetProvider");
    msgassert(cls, "Could not get a reference from class 'org/renpy/android/PythonWidgetProvider'!");
    mid = (*env)->GetStaticMethodID(env, cls, "getWidgetData", "(Ljava/lang/String;)Ljava/lang/String;");
    msgassert(mid, "Could not find the function 'getWidgetData' in the PythonWidgetProvider class!");
    
    LOG("C: Converting key c-string to a java-string...");
    jkey = (*env)->NewStringUTF(env, key);
    LOG("C: Calling java function getWidgetData...");
    jstring res = (*env)->CallStaticObjectMethod(env, cls, mid, jkey);
    LOG("C: Back from Java, returning to Python...");
    if (res == NULL) {
        LOG("C: Result is None...");
        Py_RETURN_NONE;
    }
    else {
        LOG("C: Result is a string...");
        LOG("C: Converting Java string to C string...");
        const char* tmp = (*env)->GetStringUTFChars(env, res, 0);
        LOG("C: Done. Converting C string to Python string...");
        PyObject* res_str = PyString_FromString(tmp);
        LOG("C: Done.");
        (*env)->ReleaseStringUTFChars(env, res, tmp);
        return res_str;
    }
}


static PyObject *PythonWidgets_setConfigResult(PyObject *self, PyObject *args) {
LOG("C: PythonWidgets_setConfigResult is getting called...");
    
    static JNIEnv *env   = NULL;
    jclass *cls   = NULL;
    jmethodID mid = NULL;
    PyObject* result     = Py_False;
    jboolean jresult 	 = JNI_FALSE;
    
    PyArg_ParseTuple(args, "O", &result);
    
    if (env == NULL) {
        LOG("Trying to get env pointer.");
        env = SDL_ANDROID_GetJNIEnv();
        msgassert(env, "Could not obtain the current environment!");
    }
    cls = (*env)->FindClass(env, "org/renpy/android/SDLSurfaceView");
    msgassert(cls, "Could not get a reference from class 'org/renpy/android/SDLSurfaceView'!");
    mid = (*env)->GetStaticMethodID(env, cls, "setConfigResult", "(Z)V");
    msgassert(mid, "Could not find the function 'setConfigResult' in the Android class!");
    
    if (result) {
	jresult = JNI_TRUE;
    }
    
    LOG("C: Calling java function setConfigResult...");
    (*env)->CallStaticVoidMethod(env, cls, mid, jresult);
    LOG("C: back from Java, returning to Python...");
    Py_RETURN_TRUE;
}

static PyMethodDef module_methods[] = {
    { "storeWidgetData", PythonWidgets_storeWidgetData, METH_VARARGS, "Stores the given dict 'data' in the key 'key' and makes it accessible via getWidgetData." },
    { "getWidgetData",   PythonWidgets_getWidgetData,   METH_VARARGS, "Returns the data stored in the given key 'key' as a urlencoded string that was passed to\n'storeWidgetData' or None, if there is no data stored in that key." },
    { "setConfigResult", PythonWidgets_setConfigResult, METH_VARARGS, "Set's the success status of the configuration for a widget done in the main program." },
    { NULL, NULL, 0, NULL }
};



int main(int argc, char **argv) {
    
    char *env_argument = NULL;
    int ret = 0;
    FILE *fd;

    LOG("Initialize Python for Android");
    env_argument = getenv("ANDROID_ARGUMENT");
    setenv("ANDROID_APP_PATH", env_argument, 1);
    LOG("ANDROID_APP_PATH");
    LOG(env_argument);
	//setenv("PYTHONVERBOSE", "2", 1);
    Py_SetProgramName(argv[0]);
    Py_Initialize();
    
    PySys_SetArgv(argc, argv);
    /* ensure threads will work.
     */
    PyEval_InitThreads();

    /* our logging module for android
     */
    initandroidembed();
    
    (void) Py_InitModule3("PythonWidgets", module_methods, "This module provides interaction between your python code and your Widgets.");

    /* inject our bootstrap code to redirect python stdin/stdout
     * replace sys.path with our path
     */
    PyRun_SimpleString(
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
		"import site; print site.getsitepackages()\n"\
		"print 'ANDROID_PRIVATE', private\n" \
		"print 'ANDROID_ARGUMENT', argument\n" \
		"print 'Android path', sys.path\n" \
        "print 'Android kivy bootstrap done. __name__ is', __name__");

    /* run it !
     */
    LOG("Run user program, change dir and execute main.py");
    chdir(env_argument);

	/* search the initial main.py
	 */
	char *main_py = "main.pyo";
	if ( file_exists(main_py) == 0 ) {
		if ( file_exists("main.py") )
			main_py = "main.py";
		else
			main_py = NULL;
	}

	if ( main_py == NULL ) {
		LOG("No main.pyo / main.py found.");
		return -1;
	}

    fd = fopen(main_py, "r");
    if ( fd == NULL ) {
        LOG("Open the main.py(o) failed");
        return -1;
    }

    /* run python !
     */
    ret = PyRun_SimpleFile(fd, main_py);

    if (PyErr_Occurred() != NULL) {
        ret = 1;
        PyErr_Print(); /* This exits with the right code if SystemExit. */
        if (Py_FlushLine())
			PyErr_Clear();
    }

    /* close everything
     */
	Py_Finalize();
    fclose(fd);

    LOG("Python for android ended.");
    return ret;
}

JNIEXPORT void JNICALL JAVA_EXPORT_NAME(PythonService_nativeStart) ( JNIEnv*  env, jobject thiz,
                                                                     jstring j_android_private,
                                                                     jstring j_android_argument,
                                                                     jstring j_python_home,
                                                                     jstring j_python_path,
                                                                     jstring j_arg,
                                                                     jobjectArray j_argv )
{
    jboolean iscopy;
    const char *android_private = (*env)->GetStringUTFChars(env, j_android_private, &iscopy);
    const char *android_argument = (*env)->GetStringUTFChars(env, j_android_argument, &iscopy);
    const char *python_home = (*env)->GetStringUTFChars(env, j_python_home, &iscopy);
    const char *python_path = (*env)->GetStringUTFChars(env, j_python_path, &iscopy);
    const char *arg = (*env)->GetStringUTFChars(env, j_arg, &iscopy);

    setenv("ANDROID_PRIVATE", android_private, 1);
    setenv("ANDROID_ARGUMENT", android_argument, 1);
    setenv("PYTHONOPTIMIZE", "2", 1);
    setenv("PYTHONHOME", python_home, 1);
    setenv("PYTHONPATH", python_path, 1);
    setenv("PYTHON_SERVICE_ARGUMENT", arg, 1);
    
    int i;
    int argc = (*env)->GetArrayLength(env, j_argv) + 1;
    char **argv = malloc(sizeof(char*) * argc);
    argv[0] = (char *) "service";
    for (i = 0; i < argc - 1; i++) {
	jstring str = (jstring) (*env)->GetObjectArrayElement(env, j_argv, i);
	const char *rawStr = (*env)->GetStringUTFChars(env, str, 0);
	argv[i + 1] = strdup(rawStr);
	(*env)->ReleaseStringUTFChars(env, str, rawStr);
    }
    /* ANDROID_ARGUMENT points to service subdir,
     * so main() will run main.py from this dir
     */
    main(argc, argv);
}

#endif
