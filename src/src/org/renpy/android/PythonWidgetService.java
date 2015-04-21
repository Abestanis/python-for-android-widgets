package org.renpy.android;

/*
 * Licensed under the MIT license
 * http://opensource.org/licenses/mit-license.php
 *
 * Copyright 2014 Sebastian Scholz <abestanis.gc@gmail.com> 
 */

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

    @Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(PythonWidgetProvider.TAG, "PythonWidgetUpdateService.onStart is getting called to execute " + intent.getAction() + ".");

        Integer appWidgetId = intent.getIntExtra("widgetId", -1);
		AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(getBaseContext()).getAppWidgetInfo(appWidgetId);
		if (appWidgetInfo == null) {
			Log.i(PythonWidgetProvider.TAG, "Widget " + appWidgetId + " has no initialized provider or the provider is not accessible! Stopping WidgetUpdate service...");
			AlarmManager alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
	        alarmManager.cancel(PendingIntent.getService(getBaseContext(), appWidgetId, intent, 0));
			stopSelf();
			return START_NOT_STICKY;
		}
        String className = appWidgetInfo.provider.getClassName();
        String providerName = appWidgetInfo.label;

        Class<?> providerClass;
        try {
			providerClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			Log.e(PythonWidgetProvider.TAG, "Did not find WidgetProvider " + providerName + " (" + className + "), you might need to reinstall your app!");
			stopSelf();
			return START_NOT_STICKY;
		}

        PythonWidgetProvider providerInstance;
        try {
			providerInstance = (PythonWidgetProvider) providerClass.newInstance();
		} catch (InstantiationException e) {
			Log.e(PythonWidgetProvider.TAG, "Unable to instantiate java proxy provider " + providerName + " for " + className);
			e.printStackTrace();
			stopSelf();
			return START_NOT_STICKY;
		} catch (IllegalAccessException e) {
			Log.e(PythonWidgetProvider.TAG, "Could not instantiate java proxy provider " + providerName + "(" + className + "), have no access!");
			e.printStackTrace();
			stopSelf();
			return START_NOT_STICKY;
		}
		Log.i(PythonWidgetProvider.TAG, "Update Widget " + appWidgetId + " from widgetProvider " + providerName + "(class = " + className + ").");
		
		Intent updateIntent = new Intent();
		updateIntent.setAction(intent.getAction());
		updateIntent.putExtra("widgetId", appWidgetId);
		updateIntent.putExtra("type", intent.getIntExtra("type", 1));
        updateIntent.putExtra("UpdateAction", intent.getStringExtra("UpdateAction"));
		providerInstance.onReceive(getBaseContext(), updateIntent);
        
        Log.d(PythonWidgetProvider.TAG, "Widget update done.");
        this.stopSelf();
		return START_NOT_STICKY;
	}
	
	@Override public IBinder onBind(Intent intent) {return null;}
}
