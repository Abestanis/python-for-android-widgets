package org.renpy.android;

/*
 * Licensed under the MIT license
 * http://opensource.org/licenses/mit-license.php
 *
 * Copyright 2014 Sebastian Scholz <abestanis.gc@gmail.com> 
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.RemoteViews;

@SuppressLint("UseSparseArrays")
public class PythonWidgetProvider extends AppWidgetProvider {

	public  static final String  TAG 				 = "PythonWidgets";
	protected      final String  WIDGET_NAME  		 = null;
    protected      final String  WIDGET_CLASS 		 = null;
    protected 	   final Integer PROVIDER_ID 	     = null;
	public 	static final String  CLOCK_WIDGET_UPDATE = "com.pythonapp.widget.PYTHONWIDGET_CLOCK_WIDGET_UPDATE";
	public 	static final String  WIDGET_INPUT_UPDATE = "com.pythonapp.widget.PYTHONWIDGET_INPUT_WIDGET_UPDATE";
	public 	static final String  WIDGET_INIT_UPDATE  = "com.pythonapp.widget.PYTHONWIDGET_INIT_WIDGET_UPDATE";
	public  static final String  WIDGET_DATA_STORAGE = "PythonWidgetData";
	private static 		 boolean PythonInitialized   = false;
	private static 		 int     _globalNumWidgets   = 0;
	
	private static		 Context myContext;
	public  static 		 Map<Integer, ArrayList<Integer>> data         	  = new HashMap<Integer, ArrayList<Integer>>();
	private static 		 Map<Integer, Map<Integer, Integer>> _updateFreqs = new HashMap<Integer, Map<Integer, Integer>>();
	public  static 		 Map<Integer, String> _initActions                = new HashMap<Integer, String>();
	
	public  static enum UpdateType {
		ONETIME,
		INTERVAL,
		HARD,
	}
	
	/*
	 * Design of the string representation of the widgets views:
	 * 
	 * LayoutName('arg_name': arg_value)[child1(args), child2(args),...]
	 * 
	 * Example:
	 * 
	 * LinearLayout()[TextView('text': '123'), TextView('text': '456')]
	 */
	
	static {
		Log.i(TAG, "Loading Python environment...");
        System.loadLibrary("sdl");
        System.loadLibrary("sdl_image");
        System.loadLibrary("sdl_ttf");
        System.loadLibrary("sdl_mixer");
        System.loadLibrary("python2.7");
        
        // Importing PythonWidget c module
        System.loadLibrary ("PythonWidget");
	}
	
	
 	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		/*This is called to update the App Widget at intervals defined by the updatePeriodMillis attribute in the AppWidgetProviderInfo (see Adding the AppWidgetProviderInfo Metadata above). This method is also called when the user adds the App Widget, so it should perform the essential setup, such as define event handlers for Views and start a temporary Service, if necessary. However, if you have declared a configuration Activity, this method is not called when the user adds the App Widget, but is called for the subsequent updates. It is the responsibility of the configuration Activity to perform the first update when configuration is done. (See Creating an App Widget Configuration Activity below.)
		 */
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		Log.d(TAG, "onUpdate called, args: Context " + context + ", AppWidgetManager " + appWidgetManager + " AppWidgetIDs " + Arrays.toString(appWidgetIds));
		ArrayList<Integer> myWidgets = data.get(getProviderId());
		if (myWidgets == null) {
			Log.w(TAG, "Provider data is not initialized!");
			data.put(getProviderId(), new ArrayList<Integer>());
			myWidgets = data.get(getProviderId());
		}
		Log.d(TAG, "Added Widgets: " + myWidgets.toString());

        for (int appWidgetId : appWidgetIds) {
            //Check if this Widget is new
            if (!myWidgets.contains(appWidgetId)) {
                if (_initActions.get(getProviderId()) == null) {
                    Log.w(TAG, "Widget " + appWidgetId + " not initialized!");
                }
            } else {
                //Else inform the Python Side that an Update occur
                updateWidget(context, appWidgetId, UpdateType.HARD.ordinal());
            }
        }
	}
		
	public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
		/*This is called when the widget is first placed and any time the widget is resized. You can use this callback to show or hide content based on the widget's size ranges. You get the size ranges by calling getAppWidgetOptions(), which returns a Bundle that includes the following:
		
		OPTION_APPWIDGET_MIN_WIDTH  - Contains the lower bound on the current width, in dp units, of a widget instance.
		OPTION_APPWIDGET_MIN_HEIGHT - Contains the lower bound on the current height, in dp units, of a widget instance.
		OPTION_APPWIDGET_MAX_WIDTH  - Contains the upper bound on the current width, in dp units, of a widget instance.
		OPTION_APPWIDGET_MAX_HEIGHT - Contains the upper bound on the current width, in dp units, of a widget instance.
		This callback was introduced in API Level 16 (Android 4.1). If you implement this callback, make sure that your app doesn't depend on it since it won't be called on older devices.
		*/
		//super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
		Log.d(TAG, "onAppWidgetOptionsChanged called, args: Context " + context + ", AppWidgetManager " + appWidgetManager + " AppWidgetID " + appWidgetId + " NewOptions " + newOptions);
	}
	
	public void onDeleted(Context context, int[] appWidgetIds) {
		/*This is called every time an App Widget is deleted from the App Widget host.
		 */
		super.onDeleted(context, appWidgetIds);
		
		Log.d(TAG, "onDeleted called, args: Context " + context + ", AppWidgetIDs " + Arrays.toString(appWidgetIds));
		
		ArrayList<Integer> myWidgets = data.get(getProviderId());
		
		if (myWidgets != null) {
			for (Integer id : appWidgetIds) {
				nativeDestroyWidget(getProviderId(), getWidgetClass(), id);
				myWidgets.remove(id);
				_globalNumWidgets--;
				if (_updateFreqs.containsKey(getProviderId()) && _updateFreqs.get(getProviderId()).containsKey(id)) {
					Log.d(TAG, "Stopping periodic update service for widget " + id + " (if there was one).");
					AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
					alarmManager.cancel(getUpdatePendingIntent(context, id, UpdateType.INTERVAL));
		       		_updateFreqs.get(getProviderId()).remove(id);
				}
			}
		}
		Log.i(TAG, "Number of widgets: " + _globalNumWidgets);
	}
	
	public void onEnabled(Context context) {
		/*This is called when an instance the App Widget is created for the first time. For example, if the user adds two instances of your App Widget, this is only called the first time. If you need to open a new database or perform other setup that only needs to occur once for all App Widget instances, then this is a good place to do it.
		 */
		super.onEnabled(context);
		if (data.containsKey(getProviderId())) {
			Log.w(TAG, "Provider " + getWidgetName() + " already initialized!");
			return;
		}
		Log.d(TAG, "onEnabled called, args: Context " + context);
		
		_updateFreqs.put(getProviderId(), new HashMap<Integer, Integer>());
		_initActions.put(getProviderId(), null);
		
		//Load Python widget provider
		
        Log.d(TAG, "Loading python widget provider for " + getWidgetName() + "...");
        nativeInitProvider(getProviderId(), getWidgetClass());
        
        data.put(getProviderId(), new ArrayList<Integer>());
	}
	
	public void onDisabled(Context context) {
		/*This is called when the last instance of your App Widget is deleted from the App Widget host. This is where you should clean up any work done in onEnabled(Context), such as delete a temporary database.
		 */
		super.onDisabled(context);
		Log.d(TAG, "onDisabled called, args: Context " + context);
		
		//Unload Python widget provider
		
        Log.d(TAG, "Unloading python widget provider for " + getWidgetName() + "...");
        nativeEndProvider(getProviderId(), getWidgetClass());
        
        data.remove(getProviderId());
		_updateFreqs.remove(getProviderId());
		_initActions.remove(getProviderId());
        
        if (_globalNumWidgets <= 0) {
	        Log.i(TAG, "Unloading Python environment...");
			nativeEnd();
			PythonInitialized = false;
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		/*This is called for every broadcast and before each of the above callback methods. You normally don't need to implement this method because the default AppWidgetProvider implementation filters all App Widget broadcasts and calls the above methods as appropriate.
		 */

		myContext = context;
		if (!PythonInitialized) {
			//initializing Python Environment
			PythonInitialized = true;
			Log.d(TAG, "Initializing Python...");
			
			File mPath;
	      	if (context.getResources().getString(context.getResources().getIdentifier("public_version", "string", context.getPackageName())) != null) {
	      	    mPath = new File(Environment.getExternalStorageDirectory(), context.getPackageName());
	      	} else {
	      	    mPath = context.getFilesDir();
	      	}
	      		
	      	// ANDROID_PRIVATE, ANDROID_ARGUMENT
	      	nativeInit(context.getFilesDir().getAbsolutePath(), mPath.getAbsolutePath(), context.getFilesDir().getAbsolutePath() + ":" + mPath.getAbsolutePath() + "/lib");
		}
		
		Log.d(TAG, "onReceive called for " + getWidgetName() + " (" + getWidgetClass() + "), Received intent " + intent);
	    super.onReceive(context, intent);
	    if (CLOCK_WIDGET_UPDATE.equals(intent.getAction())) {
	    	ArrayList<Integer> myWidgets = data.get(getProviderId());
	    	if (myWidgets == null) {
				Log.w(TAG, "Provider data is not initialized!");
				data.put(getProviderId(), new ArrayList<Integer>());
				myWidgets = data.get(getProviderId());
			}
	    	if (myWidgets.isEmpty()) {
	    		Log.w(TAG, "Got periodic widget update, when no widgets were initialized!");
	    		Log.i(TAG, myWidgets.toString());
	    		return;
	    	}
	        Log.d(TAG, "Widget update");
	        int appWidgetId = intent.getIntExtra("widgetId", -1);
	        int updateType = intent.getIntExtra("type", 1);
	        updateWidget(context, appWidgetId, updateType);
	    } else if (WIDGET_INPUT_UPDATE.equals(intent.getAction())) {
	    	Log.i(TAG, "Got Widget_input event.");
	    	String updateAction = intent.getStringExtra("UpdateAction");
	    	if (updateAction == null) {
	    		Log.w(TAG, "Got Widget_input event with no Action set!");
	    	} else {
	    		// Start Python function
	    		Log.i(TAG, "Calling Python function " + updateAction + ".");
	    		Log.i(TAG, intent.getIntExtra("widgetId", -1) + "");
	    		nativePythonCallback(getProviderId(), getWidgetClass(), intent.getIntExtra("widgetId", -1), updateAction, intent.getIntExtra("childNumber", -1));
	    	}
	    } else if (WIDGET_INIT_UPDATE.equals(intent.getAction())) {
	    	Log.i(TAG, "Got Widget_init event.");
	    	Integer appWidgetId = intent.getIntExtra("widgetId", -1);
	    	if (appWidgetId == -1) {
	    		Log.w(TAG, "Got Widget_init event without a widget Id!");
	    	} else {
	    		initWidget(context, appWidgetId);
	    	}
	    }
	}
	
	// Own methods
	
	public String getWidgetName() {
		return WIDGET_NAME;
	}
	
	public String getWidgetClass() {
		return WIDGET_CLASS;
	}
	
	public int getProviderId() {
		return PROVIDER_ID;
	}

	public static void storeWidgetData(String key, String data) {
    	Log.i(TAG, "Storing data " + data + " in key " + key);
    	int mode = Context.MODE_PRIVATE;
    	if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
    		mode |= Context.MODE_MULTI_PROCESS;
    	}
    	SharedPreferences widgetData = myContext.getSharedPreferences(org.renpy.android.PythonWidgetProvider.WIDGET_DATA_STORAGE, mode);
        SharedPreferences.Editor editor = widgetData.edit();
        if (data == null) {
        	editor.remove(key);
        } else {
        	editor.putString(key, data);
        }
        editor.commit();
    }

    @SuppressWarnings("unused")
	public static String getWidgetData(String key) {
		int mode = Context.MODE_PRIVATE;
    	if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
    		mode |= Context.MODE_MULTI_PROCESS;
    	}
		SharedPreferences dataStorage = myContext.getSharedPreferences(WIDGET_DATA_STORAGE, mode);
		return dataStorage.getString(key, null);
	}

	public static void setContext(Context context) {
		myContext = context;
	}

    @SuppressWarnings("unused")
	public void updateWidget(int appWidgetId, String widgetLayout) {
		Log.i(TAG, "Javas updateWidget is getting called on appWidgetId " + appWidgetId);
		
		if (!data.get(getProviderId()).contains(appWidgetId)) {
			Log.w(TAG, "Widget does not exist.");
			return;
		}
		
		Log.i(TAG, "Getting the widgets view...");
		RemoteViews views = buildWidgetRemoteViews(appWidgetId, widgetLayout);
		Log.i(TAG, "View is: " + views.toString() + ", updating widget with views...");
		
		AppWidgetManager.getInstance(myContext).updateAppWidget(appWidgetId, views);
		Log.i(TAG, "Done, leaving function.");
	}
	
	public boolean existWidget(int appWidgetId) {
		Log.i(TAG, "Javas existWidget is getting called on appWidgetId " + appWidgetId);
		ArrayList<Integer> myWidgets = data.get(getProviderId());
		Log.i(TAG, "Result is " + myWidgets.contains(appWidgetId));
		return myWidgets.contains(appWidgetId);
	}

    @SuppressWarnings("unused")
	public void setInitAction(String action, boolean force) {
		if (force || !(_initActions.containsKey(getProviderId())) || (_initActions.get(getProviderId()) == null)) {
			Log.i(TAG, "Setting init action to " + action);
			_initActions.put(getProviderId(), action);
		}
	}

    @SuppressWarnings("unused")
    public void startMainApp(String[] argv) {
        startMainApp(myContext, argv);
    }

    public static void startMainApp(Context context, String[] argv) {
        long id = Calendar.getInstance().getTimeInMillis();
        Intent configIntent = new Intent(context, PythonActivity.class);
        if (argv != null) { configIntent.putExtra("argv", argv); }
        configIntent.putExtra("ConfigActivityId", id);
        configIntent.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
        configIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(configIntent);
    }

    @SuppressWarnings("unused")
	public void setSingleUpdate(int appWidgetId, int timeDiff) {
		// warning: If there previously was a single alarm requested by this appWidgetId, it's canceled by this one.
		Log.d(TAG, "Setting up single update service for widget " + appWidgetId + ", scheduling in " + timeDiff + " ms.");
		AlarmManager alarmManager = (AlarmManager) myContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + timeDiff, getUpdatePendingIntent(myContext, appWidgetId, UpdateType.ONETIME));
	}

    @SuppressWarnings("unused")
	public void setPeriodicUpdateFreq(int appWidgetId, int freq) {
		if (freq <= 0) {freq = -1;}
		_updateFreqs.get(getProviderId()).put(appWidgetId, freq);
	}
	
	public int getPeriodicUpdateFreq(int appWidgetId) {
		if ((!_updateFreqs.containsKey(getProviderId())) || (!_updateFreqs.get(getProviderId()).containsKey(appWidgetId))) {
			return -1;
		}
		return _updateFreqs.get(getProviderId()).get(appWidgetId);
	}
	
	// Private functions
	
	private RemoteViews buildWidgetRemoteViews(int appWidgetId, String widgetView) {
		Log.d(TAG, "(buildWidgetRemoteViews) Widgets View (" + appWidgetId + "): " + widgetView);
        Resources resources = myContext.getResources();
		if (widgetView != null) {
			Log.i(TAG, "Creating widget view!");
			Vector<RmView> viewList = new Vector<RmView>();
			while (widgetView.length() > 0) {
				if (widgetView.startsWith("]")) {
					if (viewList.size() > 1) {
						RmView lastLayout = viewList.get(viewList.size() - 2);
						lastLayout.addView(viewList.remove(viewList.size() - 1));
						viewList.set(viewList.size() - 1, lastLayout);
					}
					widgetView = widgetView.substring(1);
				} else {
					int index = widgetView.indexOf("(");
					RmView currentView = new RmView(myContext, resources, widgetView.substring(0, index), widgetView.substring(index + 1), appWidgetId);
					widgetView = widgetView.substring(index + 1);
					if (currentView.view == null) {
						// An unknown View
						// TODO: Set the view to the default error view?
						break;
					}
					
					widgetView = widgetView.substring(widgetView.indexOf(")") + 1);
					if (widgetView.startsWith("[")) {
						// There are Views to be added to this View
						viewList.add(currentView);
						widgetView = widgetView.substring(1);
					} else if (viewList.isEmpty()) {
						// If this is a flat Layout
						viewList.add(currentView);
					} else {
						// Adding this View to the current highest level view
						RmView tmp = viewList.lastElement();
						tmp.addView(currentView);
						viewList.set(viewList.size() - 1, tmp);
					}
				}
				if (widgetView.startsWith(",")) {
					// Remove ', '
					widgetView = widgetView.substring(2);
				}
			}
			
			if (viewList.size() > 1) {
				Log.w(TAG, "Something went wrong during the build of the widgets views: There are more than one view remaining in the queue: " + viewList);
			}
			if (!viewList.isEmpty()) {
        return viewList.firstElement().view;
    }
    Log.w(TAG, "Something went wrong during the build of the widgets views: There is no view left in the queue: " + viewList);
}
// Let android display an error view
Log.i(TAG, "Setting layout to an error view.");
        return new RemoteViews(myContext.getPackageName(), 0);
        }

private void initWidget(Context context, int appWidgetId) {

        if (existWidget(appWidgetId)) {
        Log.w(TAG, "Widget " + appWidgetId + " from provider " + getWidgetName() + " already initialized!");
        return;
        }

        Log.i(TAG, "Initialize Widget " + appWidgetId + "...");
        data.get(getProviderId()).add(appWidgetId);
        _globalNumWidgets++;

        if (!nativeInitWidget(getProviderId(), getWidgetClass(), appWidgetId)) {
			Log.w(TAG, "Widget initialization failed!");
			data.get(getProviderId()).remove(Integer.valueOf(appWidgetId));
			_globalNumWidgets--;
		} else if (getPeriodicUpdateFreq(appWidgetId) != -1) {
			int freq = getPeriodicUpdateFreq(appWidgetId);
			Log.d(TAG, "Starting periodic update service for widget " + appWidgetId + ", scheduling every " + freq + " ms.");
			AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + freq, freq, getUpdatePendingIntent(context, appWidgetId, UpdateType.INTERVAL));
		}
	}
	
	private void updateWidget(Context context, int appWidgetId, int updateType) {
		Log.i(TAG, "Update Widget " + appWidgetId + " (UpdateType: " + updateType + ")...");
		int tmp_freq = getPeriodicUpdateFreq(appWidgetId);
		nativeUpdateWidget(getProviderId(), getWidgetClass(), appWidgetId, updateType);
		int freq = getPeriodicUpdateFreq(appWidgetId);
		if ((tmp_freq != -1) && freq == -1) {
			Log.d(TAG, "Stopping periodic update service for widget " + appWidgetId + ".");
			AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        	alarmManager.cancel(getUpdatePendingIntent(context, appWidgetId, UpdateType.INTERVAL));
		} else if (tmp_freq != getPeriodicUpdateFreq(appWidgetId)) {
			Log.d(TAG, "Starting/Changing periodic update service for widget " + appWidgetId + ", scheduling every " + freq + " ms.");
			AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        	alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + freq, freq, getUpdatePendingIntent(context, appWidgetId, UpdateType.INTERVAL));
		}
		
	}

    private PendingIntent getUpdatePendingIntent(Context context, Integer appWidgetId, UpdateType updateType) {
        Intent intent = new Intent(context, PythonWidgetService.class);
        intent.setAction(CLOCK_WIDGET_UPDATE);
        intent.putExtra("widgetId", appWidgetId);
        intent.putExtra("type", updateType.ordinal());
        // TODO: Decide if we want multiple updates
        intent.setData(Uri.parse(this.getWidgetName() + "://widget/id/" + appWidgetId + "/start/" + String.valueOf(System.currentTimeMillis()) + "/type/" + updateType.toString()));// + "/id/" + id));
        return PendingIntent.getService(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

	// Native functions

	public native void    nativeInit(String android_private, String android_argument, String python_path);
	public native void    nativeEnd();
	public native void    nativeInitProvider(int providerId, String className);
	public native void    nativeEndProvider(int providerId, String className);
	public native boolean nativeInitWidget(int providerId, String className, int widgetId);
	public native void    nativeUpdateWidget(int providerId, String className, int widgetId, int updateType);
	public native void    nativeDestroyWidget(int providerId, String className, int widgetId);
	public native void 	  nativePythonCallback(int providerId, String className, int widgetId, String methodName, int argument);
}

