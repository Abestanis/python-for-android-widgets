package org.renpy.android;

/*
 * Licensed under the MIT license
 * http://opensource.org/licenses/mit-license.php
 *
 * Copyright 2014 Sebastian Scholz <abestanis.gc@gmail.com> 
 */

import java.util.Calendar;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;

public class PythonWidgetConfigurationActivity extends Activity {
	
	private static final String TAG 			  = "PythonWidgets";
	private PythonWidgetProvider providerInstance = null;
	private Integer  appWidgetId                  = -1;
	private Integer  providerId                   = -1;
	private String   className 					  = null;
	private String   providerName 				  = null;
	private Class<?> providerClass 				  = null;
	private long 	 id 						  = -1;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	setResult(RESULT_CANCELED);
        super.onCreate(savedInstanceState);
    	if (savedInstanceState != null) {
    		// Returning from App
    		className    = savedInstanceState.getString("className");
    		providerName = savedInstanceState.getString("providerName");
    		setProviderData();
    		appWidgetId  = savedInstanceState.getInt("appWidgetId");
    		providerId   = savedInstanceState.getInt("providerId");
    		id 			 = savedInstanceState.getLong("id");
    		SharedPreferences configState = getBaseContext().getSharedPreferences("PythonConfigState", 0);
    		boolean result = Boolean.valueOf(configState.getString(String.valueOf(id), "false"));
    		SharedPreferences.Editor editor = configState.edit();
            editor.remove(String.valueOf(id));
            editor.commit();
            onConfigResult(result);
    		return;
    	}
    	
    	// Getting all the information we need
    	
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getBaseContext());
        className     		       		  = appWidgetManager.getAppWidgetInfo(appWidgetId).provider.getClassName();
        providerName  		   			  = appWidgetManager.getAppWidgetInfo(appWidgetId).label;
        setProviderData();
		
        if (providerClass == null) {
			finish();
        	return;
        }
        providerId = providerInstance.getProviderId();
		
		Log.i(TAG, "Configure Widget " + appWidgetId + " from widgetprovider " + providerName + "(class = " + className + ", id = " + providerId + ").");
		
		// Provider might or might not be already initialized at this point
		// This ensures that the Provider is loaded:
		
		if (!PythonWidgetProvider._initActions.containsKey(providerId) || PythonWidgetProvider._initActions.get(providerId) == null) {
			
			// Call the on_enabled function from the appWidgetProvider
			Intent enableIntent = new Intent();
			enableIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_ENABLED);
			
			providerInstance.onReceive(getBaseContext(), enableIntent);
			
			if (!PythonWidgetProvider._initActions.containsKey(providerId) || PythonWidgetProvider._initActions.get(providerId) == null) {
				Log.e(TAG, "Widgetprovider " + providerName + " doesn't declare an initialistation action!");
				finish();
				return;
			}
		}
		
		// Get the action to perform at widget initialization
		
		String action = PythonWidgetProvider._initActions.get(providerId);
		
		// Execute the given initialization action
		
		if (action.equals("None")) {
			// No configuration activity, just initialize the widget
			initWidget();
		} else if (action.equals("True") || action.startsWith("argv:")) {
			// Launch the configuration in the main app
			// We can't use "--show_config" because kivy throws an error if it finds an argument which it does not recognize, if it starts with an '-'.
			// TODO: Work on a solution.
			if (action.startsWith("argv:")) {
				// Extract command line arguments
				startMainApp(new String[] {"--show_config", String.valueOf(appWidgetId), providerInstance.getWidgetClass(), providerName, action.substring(5)});
			} else {
				startMainApp(new String[] {"--show_config", String.valueOf(appWidgetId), providerInstance.getWidgetClass(), providerName});
			}
		} else {
			// Display simple configuration options
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
			final String tmp_appWidgetId  	  = String.valueOf(appWidgetId);
			final String tmp_provideClassName = providerInstance.getWidgetClass();
			PythonWidgetConfigurationUI.setConfigUI(action, this, new PythonWidgetConfigurationUI.OnResultListener() {
				@Override
				public void onDone(String key, String result) {
					if (key == null) {
						PythonWidgetProvider.storeWidgetData(tmp_provideClassName + tmp_appWidgetId + "_config", result);
					} else {
						PythonWidgetProvider.storeWidgetData(key, result);
					}
					initWidget();
				}
				@Override
				public void onCancel() {
					finish();
				}
			});
		}
    }
    
    protected void onSaveInstanceState(Bundle outState) {
    	outState.putInt("appWidgetId",     appWidgetId);
    	outState.putInt("providerId",      providerId);
    	outState.putString("className",    className);
    	outState.putString("providerName", providerName);
    	outState.putLong("id",		       id);
    	super.onSaveInstanceState(outState);
    }
    
    private void setProviderData() {
		try {
			providerClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			Log.e(TAG, "Did not find WidgetProvider " + providerName + " (" + className + "), you might need to reinstall your app!");
			return;
		}
		
		try {
			providerInstance = (PythonWidgetProvider) providerClass.newInstance();
		} catch (InstantiationException e) {
			Log.e(TAG, "Unable to instantiate java proxy provider " + providerName + " for " + className);
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			Log.e(TAG, "Could not instantiate java proxy provider " + providerName + "(" + className + "), have no access!");
			e.printStackTrace();
		}
    }
    
    private void initWidget() {
    	Log.i(TAG, "Initializing widget...");
    	Intent initIntent = new Intent();
		initIntent.setAction(PythonWidgetProvider.WIDGET_INIT_UPDATE);
		initIntent.putExtra("WidgetId", appWidgetId);
		
		providerInstance.onReceive(getBaseContext(), initIntent);
		
		if (PythonWidgetProvider.data.get(providerId).contains(appWidgetId)) {
			Log.i(TAG, "Widget init was successfull!");
			setResult(RESULT_OK);
		} else {
			Log.i(TAG, "Removing widget from homescreen!");
			setResult(RESULT_CANCELED);
		}
		finish();
		return;
    }
    
    public void onConfigResult(boolean resultOK) {
		if (resultOK) {
			initWidget();
        } else {
        	Log.d(TAG, "Config trough app has failed!");
        	setResult(RESULT_CANCELED);
    		finish();
        }
        return;
	}
    
    private void startMainApp(String[] argv) {
    	id = Calendar.getInstance().getTimeInMillis();
    	Intent configIntent = new Intent(getBaseContext(), PythonActivity.class);
		configIntent.putExtra("argv", argv);
		configIntent.putExtra("ConfigActivityId", id);
		configIntent.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
		startActivity(configIntent);
	}
}