package org.renpy.android;

/*
 * Licensed under the MIT license
 * http://opensource.org/licenses/mit-license.php
 *
 * Copyright 2014 Sebastian Scholz <abestanis.gc@gmail.com> 
 */

/*import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.app.Activity;
*/
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.test.Instagram_Feed.R;
import org.renpy.android.WidgetView;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.RemoteViews;

public class PythonWidgetProvider extends AppWidgetProvider {
	
    /*
	static Context context;

    /* Deliver some data to someone else
    */
	/*
    static void send(String mimeType, String filename, String subject, String text, String chooser_title) {
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType(mimeType);
        /** tryied with String [] emails, but hard to code the whole C/Cython part.
          if (emails != null)
          emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, emails);
         **//*
        if (subject != null)
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        if (text != null)
            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);
        if (filename != null)
            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+ filename));
        if (chooser_title == null)
            chooser_title = "Send mail";
        context.startActivity(Intent.createChooser(emailIntent, chooser_title));
    }
    */
	
	private static final String TAG 				   = "PythonWidgets";
	public 	static final String PERIODIC_WIDGET_UPDATE = "com.pythonapp.widget.PYTHONWIDGET_CLOCK_WIDGET_UPDATE";
	public 	static final String WIDGET_INPUT_UPDATE    = "com.pythonapp.widget.PYTHONWIDGET_INPUT_WIDGET_UPDATE";
	public  static final String WIDGET_DATA_STORAGE    = "PythonWidgetData";
	public 	static final int 	DEFAULT_LOADING_LAYOUT = R.layout.widget_loading;
	public 	static final int 	DEFAULT_ERROR_LAYOUT   = R.layout.widget_textview; // TODO: Replace with error view
	private 			 int 	_updatefreq 		   = 15000; // TODO: Make this accessible from the python side
	private static 		 Context myContext;
	private static		 ArrayList<Integer> myWidgets  = new ArrayList<Integer>();
	private static		 String _defaultLoadView  	   = null;
	private static		 String _defaultErrorView 	   = null;
	
	/*
	 * Design of the string representation of the widgets views:
	 * 
	 * Layoutname('arg_name': arg_value)[child1(args), child2(args),...]
	 * 
	 * Example:
	 * 
	 * LinearLayout()[TextView('text': '123'), TextView('text': '456')]
	 */
	
	
 	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		/*This is called to update the App Widget at intervals defined by the updatePeriodMillis attribute in the AppWidgetProviderInfo (see Adding the AppWidgetProviderInfo Metadata above). This method is also called when the user adds the App Widget, so it should perform the essential setup, such as define event handlers for Views and start a temporary Service, if necessary. However, if you have declared a configuration Activity, this method is not called when the user adds the App Widget, but is called for the subsequent updates. It is the responsibility of the configuration Activity to perform the first update when configuration is done. (See Creating an App Widget Configuration Activity below.)
		 */
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		Log.d(TAG, "onUpdate called, args: Context " + context + ", AppWidgetManager " + appWidgetManager + " AppWidgetIDs " + Arrays.toString(appWidgetIds));
		Log.d(TAG, "Added Widgets: " + myWidgets.toString());
		final int N = appWidgetIds.length;
		
		for (int i=0; i<N; i++) {
			//Check if this Widget is new
			if (!myWidgets.contains(appWidgetIds[i])) {
				initWidget(context, appWidgetManager, appWidgetIds[i]);
			} else {
				//Else inform the Python Side that an Update occur
				updateWidget(context, appWidgetManager, appWidgetIds[i]);
			};
		}
	}
		
	public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
		/*This is called when the widget is first placed and any time the widget is resized. You can use this callback to show or hide content based on the widget's size ranges. You get the size ranges by calling getAppWidgetOptions(), which returns a Bundle that includes the following:
		
		OPTION_APPWIDGET_MIN_WIDTH—Contains the lower bound on the current width, in dp units, of a widget instance.
		OPTION_APPWIDGET_MIN_HEIGHT—Contains the lower bound on the current height, in dp units, of a widget instance.
		OPTION_APPWIDGET_MAX_WIDTH—Contains the upper bound on the current width, in dp units, of a widget instance.
		OPTION_APPWIDGET_MAX_HEIGHT—Contains the upper bound on the current width, in dp units, of a widget instance.
		This callback was introduced in API Level 16 (Android 4.1). If you implement this callback, make sure that your app doesn't depend on it since it won't be called on older devices.
		*/
		//super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
		Log.d(TAG, "onAppWidgetOptionsChanged called, args: Context " + context + ", AppWidgetManager " + appWidgetManager + " AppWidgetID " + appWidgetId + " NewOptions " + newOptions);
		//If it is and there are multiple Widgets for this Provider, launch a choose Widget Action
		//Inform the Python side that a Widget was added
	}
	
	public void onDeleted(Context context, int[] appWidgetIds) {
		/*This is called every time an App Widget is deleted from the App Widget host.
		 */
		super.onDeleted(context, appWidgetIds);
		
		Log.d(TAG, "onDeleted called, args: Context " + context + ", AppWidgetIDs " + Arrays.toString(appWidgetIds));
		
		List<int[]> WidgetIds = Arrays.asList(appWidgetIds);
		
		for (Integer widgetId : myWidgets) {
			if (!WidgetIds.contains(widgetId)) {
				nativedestroyWidget(widgetId);
				myWidgets.remove(widgetId);
			};
		}
	}
	
	public void onEnabled(Context context) {
		/*This is called when an instance the App Widget is created for the first time. For example, if the user adds two instances of your App Widget, this is only called the first time. If you need to open a new database or perform other setup that only needs to occur once for all App Widget instances, then this is a good place to do it.
		 */
		super.onEnabled(context);
		Log.d(TAG, "onEnabled called, args: Context " + context);
		Log.i(TAG, "Starting WidgetUpdate service...");
		
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.SECOND, 1);
        alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), _updatefreq, PendingIntent.getBroadcast(context, 0, new Intent(PERIODIC_WIDGET_UPDATE), PendingIntent.FLAG_UPDATE_CURRENT));
		
        Log.i(TAG, "Loading Python environment...");
        
        // I don't know why but this seems to be necessary
        System.loadLibrary("sdl");
        System.loadLibrary("sdl_image");
        System.loadLibrary("sdl_ttf");
        System.loadLibrary("sdl_mixer");
        System.loadLibrary("python2.7");
        
        // Importing PythonWidget c module
        System.loadLibrary ("PythonWidget");
        
        // Doing the same as the main thread (I hope...)
        
		File mPath;
		if (context.getResources().getString(context.getResources().getIdentifier("public_version", "string", context.getPackageName())) != null) {
	        mPath = new File(Environment.getExternalStorageDirectory(), context.getPackageName());
	    } else {
	        mPath = context.getFilesDir();
	    }
		
		// ANDROID_PRIVATE, ANDROID_ARGUMENT
		nativeinit(context.getFilesDir().getAbsolutePath(), mPath.getAbsolutePath(), context.getFilesDir().getAbsolutePath() + ":" + mPath.getAbsolutePath() + "/lib");
		
        //(Check if Python instance is Running)
	}
	
	public void onDisabled(Context context) {
		/*This is called when the last instance of your App Widget is deleted from the App Widget host. This is where you should clean up any work done in onEnabled(Context), such as delete a temporary database.
		 */
		super.onDisabled(context);
		Log.d(TAG, "onDisabled called, args: Context " + context);
		
		Log.i(TAG, "Stopping WidgetUpdate service...");
		
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(PendingIntent.getBroadcast(context, 0, new Intent(PERIODIC_WIDGET_UPDATE), PendingIntent.FLAG_UPDATE_CURRENT));
		//End the Python Service
        
        Log.i(TAG, "Unloading Python environment...");
        nativeend();
	}	
	
	@Override
	public void onReceive(Context context, Intent intent) {
		/*This is called for every broadcast and before each of the above callback methods. You normally don't need to implement this method because the default AppWidgetProvider implementation filters all App Widget broadcasts and calls the above methods as appropriate.
		 */
		
		myContext = context;
		Log.d(TAG, "onRecive called, Received intent " + intent);
	    super.onReceive(context, intent);
	    if (PERIODIC_WIDGET_UPDATE.equals(intent.getAction())) {
	    	if (myWidgets.isEmpty()) {
	    		Log.w(TAG, "Got Preriodic widget update, when no widgets were initialized!");
	    		Log.i(TAG, myWidgets.toString());
	    		return;
	    	}
	        Log.d(TAG, "Widget update");
	        // Get the widget manager and Id's for this widget provider, then call the shared
	        // clock update method.
	        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), getClass().getName());
	        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
	        int ids[] = appWidgetManager.getAppWidgetIds(thisAppWidget);
	        for (int appWidgetID: ids) {
	            updateWidget(context, appWidgetManager, appWidgetID);
	        }
	    } else if (WIDGET_INPUT_UPDATE.equals(intent.getAction())) {
	    	Log.i(TAG, "Got Widget_input event.");
	    	String updateAction = intent.getStringExtra("UpdateAction");
	    	if (updateAction == null) {
	    		Log.w(TAG, "Got Widget_input event with no Action set!");
	    	} else {
	    		// Start Python function
	    		Log.i(TAG, "Calling Python function " + updateAction + ".");
	    		nativePythonCallback(intent.getIntExtra("WidgetId", -1), updateAction);
	    	}
	    }
	}
	
	// Own methods
	
	public void updateWidget(int appWidgetId, String widgetlayout) {
		Log.i(TAG, "Javas updateWidget is getting called on appWidgetId " + appWidgetId);
		
		if (!myWidgets.contains(appWidgetId)) {
			Log.w(TAG, "Widget does not exist.");
			return;
		}
		
		Log.i(TAG, "Getting the widgets view...");
		RemoteViews views = buildWidgetRemoteViews(appWidgetId, widgetlayout);
		Log.i(TAG, "View is: " + views.toString() + ", updating widget with views...");
		
		AppWidgetManager.getInstance(myContext).updateAppWidget(appWidgetId, views);
		Log.i(TAG, "Done, leaving function.");
	}
	
	public boolean existWidget(int appWidgetId) {
		Log.i(TAG, "Javas existWidget is getting called on appWidgetId " + appWidgetId);
		Log.i(TAG, "Result is " + myWidgets.contains(appWidgetId));
		return myWidgets.contains(appWidgetId);
	}
	
	public void setDefaultLoadView(String defaultview) {
		Log.i(TAG, "Setting default loading view: " + defaultview);
		_defaultLoadView = defaultview;
	}
	
	public String getDefaultLoadView() {
		Log.i(TAG, "Returning default loading view: " + _defaultLoadView);
		return _defaultLoadView;
	}
	
	public void setDefaultErrorView(String defaultview) {
		Log.i(TAG, "Setting default error view: " + defaultview);
		_defaultErrorView = defaultview;
	}
	
	public String getDefaultErrorView() {
		Log.i(TAG, "Returning default error view: " + _defaultErrorView);
		return _defaultErrorView;
	}
	
	public static Context getWidgetContext() {
		return myContext;
	}
	
	public String getWidgetData() {
		SharedPreferences dataStorage = myContext.getSharedPreferences(WIDGET_DATA_STORAGE, 0);
		String data = dataStorage.getString("data", null);
		return data;
	}
	
	// Private functions
	
	private RemoteViews buildWidgetRemoteViews(int appWidgetId, String widgetview, int defaultLayout) {
		Log.d(TAG, "(buildWidgetRemoteViews) Widgets View (" + appWidgetId + "): " + widgetview);
		
		if (widgetview == null) {
			// Set widgetview to the user defined layout
			Log.i(TAG, "Widget has no view, setting userdefined default if given...");
			switch (defaultLayout) {
			case DEFAULT_LOADING_LAYOUT:
				Log.i(TAG, "Setting view to userdefined loading view.");
				widgetview = _defaultLoadView;
				break;
			case DEFAULT_ERROR_LAYOUT:
				Log.i(TAG, "Setting view to userdefined error view.");
				widgetview = _defaultErrorView;
				break;
			}
		}
		
		
//		String packageName = myContext.getPackageName();
//		RemoteViews currentView = new RemoteViews(packageName, R.layout.widget_linearlayout);
//		currentView.removeAllViews(R.id.widget_linearlayout);
//		RemoteViews currentView2 = new RemoteViews(packageName, R.layout.widget_textview);
//		currentView2.setTextViewText(R.id.widget_textview, String.valueOf(Calendar.getInstance().getTimeInMillis()));
//		currentView.addView(R.id.widget_linearlayout, currentView2);
//		if (true) {return currentView;}
		
		
//		String packageName = myContext.getPackageName();
//		RemoteViews currentView2 = new RemoteViews(packageName, R.layout.widget_textview);
//		currentView2.setTextViewText(R.id.widget_textview, String.valueOf(Calendar.getInstance().getTimeInMillis()));
//		if (true) {return currentView2;}
		
		
//		String packageName = myContext.getPackageName();
//		RemoteViews currentView = new RemoteViews(packageName, R.layout.widget_linearlayout);
//		RemoteViews currentView2 = new RemoteViews(packageName, R.layout.widget_textview);
//		currentView2.setTextViewText(R.id.widget_textview, String.valueOf(Calendar.getInstance().getTimeInMillis()));
//		currentView.addView(R.id.widget_linearlayout, currentView2);
//		if (true) {return currentView;}
		
		
//		String packageName = myContext.getPackageName();
//		Vector<RemoteViews> viewList = new Vector<RemoteViews>();
//		RemoteViews currentView = new RemoteViews(packageName, R.layout.widget_linearlayout);
//		viewList.add(currentView);
//		RemoteViews currentView2 = new RemoteViews(packageName, R.layout.widget_textview);
//		currentView2.setTextViewText(R.id.widget_textview, String.valueOf(Calendar.getInstance().getTimeInMillis()));
//		RemoteViews tmp = viewList.lastElement();
//		tmp.addView(R.id.widget_linearlayout, currentView2);
//		viewList.set(viewList.size() - 1, tmp);
//		if (viewList.size() > 1) {
//			RemoteViews lastLayout = viewList.get(viewList.size() - 2);
//			lastLayout.addView(R.id.widget_linearlayout, viewList.remove(viewList.size() - 1));
//			viewList.set(viewList.size() - 1, lastLayout);
//		}
//		if (true) {return viewList.firstElement();}
		
		
//		String packageName = myContext.getPackageName();
//		Vector<WidgetView> viewList = new Vector<WidgetView>();
//		WidgetView currentView = new WidgetView(packageName, "LinearLayout", ")", appWidgetId);
//		viewList.add(currentView);
//		WidgetView currentView2 = new WidgetView(packageName, "TextView", "text=".concat(String.valueOf(Calendar.getInstance().getTimeInMillis())).concat(")"), appWidgetId);
//		WidgetView tmp = viewList.lastElement();
//		tmp.addView(currentView2);
//		viewList.set(viewList.size() - 1, tmp);
//		if (viewList.size() > 1) {
//			WidgetView lastLayout = viewList.get(viewList.size() - 2);
//			lastLayout.addView(viewList.remove(viewList.size() - 1));
//			viewList.set(viewList.size() - 1, lastLayout);
//		}
//		if (true) {return viewList.firstElement().view;}
		
		if (widgetview != null) {
			Log.i(TAG, "Creating widget view!");
			String packageName = myContext.getPackageName();
			Vector<WidgetView> viewList = new Vector<WidgetView>();
			while (widgetview.length() > 0) {
				if (widgetview.startsWith("]")) {
					if (viewList.size() > 1) {
						WidgetView lastLayout = viewList.get(viewList.size() - 2);
						lastLayout.addView(viewList.remove(viewList.size() - 1));
						viewList.set(viewList.size() - 1, lastLayout);
					}
					widgetview = widgetview.substring(1);
				} else {
					int index = widgetview.indexOf("(");
					WidgetView currentView = new WidgetView(packageName, widgetview.substring(0, index), widgetview.substring(index + 1), appWidgetId);
					widgetview = widgetview.substring(index + 1);
					if (currentView.view == null) {
						// An unknown View
						// TODO: Set the view to the default error view 
						break;
					}
					
					widgetview = widgetview.substring(widgetview.indexOf(")") + 1);
					if (widgetview.startsWith("[")) {
						// There are Views to be added to this View
						viewList.add(currentView);
						widgetview = widgetview.substring(1);
					} else if (viewList.isEmpty()) {
						// If this is a flat Layout
						viewList.add(currentView);
					} else {
						// Adding this View to the current highest level view
						WidgetView tmp = viewList.lastElement();
						tmp.addView(currentView);
						viewList.set(viewList.size() - 1, tmp);
					}
				}
				if (widgetview.startsWith(",")) {
					// Remove ', '
					widgetview = widgetview.substring(2);
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
		// Set view to the hard-coded defaults cause the user has not defined a default layout
		Log.i(TAG, "Setting layout to a hardcoded default.");
		return new RemoteViews(myContext.getPackageName(), defaultLayout);
	}
	
	private RemoteViews buildWidgetRemoteViews(int appWidgetId, String widgetview) {
		return buildWidgetRemoteViews(appWidgetId, widgetview, DEFAULT_ERROR_LAYOUT);
	}
	
	private void startPythonService() {
		Log.i(TAG, "Starting the Python Service.");
	}
	
	private void initWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
		Log.i(TAG, "Initialize Widget " + appWidgetId + "...");
		Log.i(TAG, "Setting loading view for Widget " + appWidgetId + "!");
		
		myWidgets.add(appWidgetId);
		
		RemoteViews views = buildWidgetRemoteViews(appWidgetId, null, DEFAULT_LOADING_LAYOUT);
		
		Log.i(TAG, "Got Widget View.");
		// Tell the AppWidgetManager to perform an update on the current app widget
		appWidgetManager.updateAppWidget(appWidgetId, views);
		if (!nativeinitWidget(appWidgetId)) {
			Log.w(TAG, "Python initialisation failed, setting default error View");
			views = buildWidgetRemoteViews(appWidgetId, null);
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}
	
	private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
		Log.i(TAG, "Update Widget " + appWidgetId + "...");
		nativeupdateWidget(appWidgetId);
	}

	// Native functions
	
	public native void    nativeinit(String android_private, String android_argument, String python_path);
	public native void    nativeend();
	public native boolean nativeinitWidget(int WidgetId);
	public native void    nativeupdateWidget(int WidgetId);
	public native void    nativedestroyWidget(int WidgetId);
	public native void 	  nativePythonCallback(int WidgetId, String methodname);
	
}

