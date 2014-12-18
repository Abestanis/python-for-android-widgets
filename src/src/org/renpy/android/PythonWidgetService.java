package org.renpy.android;

/*
 * Licensed under the MIT license
 * http://opensource.org/licenses/mit-license.php
 *
 * Copyright 2014 Sebastian Scholz <abestanis.gc@gmail.com> 
 */

import org.renpy.android.PythonWidgetProvider;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class PythonWidgetService extends Service {
	
	static private String TAG = PythonWidgetProvider.TAG;
	private PythonWidgetProvider providerInstance = null;
	private Integer  appWidgetId                  = -1;
	private String   className 					  = null;
	private String   providerName 				  = null;
	private Class<?> providerClass 				  = null;
	
	@Override  
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "PythonWidgetUpdateSercice.onStart is getting called.");
		
		appWidgetId	= intent.getIntExtra("widgetId", -1);
		AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(getBaseContext()).getAppWidgetInfo(appWidgetId);
		if (appWidgetInfo == null) {
			Log.w(TAG, "Given widget " + appWidgetId + " has no initialized provider or the provider is not accessible! Stopping WidgetUpdate service...");
			AlarmManager alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
	        alarmManager.cancel(PendingIntent.getService(getBaseContext(), appWidgetId, intent, 0));
			stopSelf();
			return START_NOT_STICKY;
		}
        className    = appWidgetInfo.provider.getClassName();
        providerName = appWidgetInfo.label;
        
		try {
			providerClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			Log.e(TAG, "Did not find WidgetProvider " + providerName + " (" + className + "), you might need to reinstall your app!");
			stopSelf();
			return START_NOT_STICKY;
		}
		
		try {
			providerInstance = (PythonWidgetProvider) providerClass.newInstance();
		} catch (InstantiationException e) {
			Log.e(TAG, "Unable to instantiate java proxy provider " + providerName + " for " + className);
			e.printStackTrace();
			stopSelf();
			return START_NOT_STICKY;
		} catch (IllegalAccessException e) {
			Log.e(TAG, "Could not instantiate java proxy provider " + providerName + "(" + className + "), have no access!");
			e.printStackTrace();
			stopSelf();
			return START_NOT_STICKY;
		}
		Log.i(TAG, "Update Widget " + appWidgetId + " from widgetprovider " + providerName + "(class = " + className + ").");
		
		Intent updateIntent = new Intent();
		updateIntent.setAction(PythonWidgetProvider.CLOCK_WIDGET_UPDATE);
		updateIntent.putExtra("widgetId", appWidgetId);
		updateIntent.putExtra("type", intent.getIntExtra("type", 1));
		providerInstance.onReceive(getBaseContext(), updateIntent);
        
        Log.d(TAG, "Widget update done.");
        this.stopSelf();
		return START_NOT_STICKY;
	}
	
	@Override public IBinder onBind(Intent intent) {return null;}
}