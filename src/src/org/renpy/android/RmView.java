package org.renpy.android;

/*
 * Licensed under the MIT license
 * http://opensource.org/licenses/mit-license.php
 *
 * Copyright 2014 Sebastian Scholz <abestanis.gc@gmail.com> 
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.test.Instagram_Feed.R;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;
import android.widget.RemoteViews;


public class RmView {
	private static final String TAG = "PythonWidgets";
	private final String _type;
	private final int widget_Id;
	private final Context context;
	private static final Map<String, int[]> VIEWS;
	static {
        VIEWS = new HashMap<String, int[]>();
        VIEWS.put("LinearLayout", 		new int[] {R.layout.widget_linearlayout,   R.id.widget_linearlayout});
        VIEWS.put("FrameLayout", 		new int[] {R.layout.widget_framelayout,    R.id.widget_framelayout});
        VIEWS.put("RelativeLayout", 	new int[] {R.layout.widget_relativelayout, R.id.widget_relativelayout});
        //VIEWS.put("GridLayout", 		new int[] {, }); Requires higher API level
        VIEWS.put("TextView", 	  		new int[] {R.layout.widget_textview, 	   R.id.widget_textview});
        VIEWS.put("AnalogClock", 		new int[] {R.layout.widget_analogclock,    R.id.widget_analogclock});
        VIEWS.put("Button", 			new int[] {R.layout.widget_button, 		   R.id.widget_button});
        VIEWS.put("Chronometer", 		new int[] {R.layout.widget_chronometer,    R.id.widget_chronometer});
        VIEWS.put("ImageButton", 		new int[] {R.layout.widget_imagebutton,    R.id.widget_imagebutton});
        VIEWS.put("ImageView", 			new int[] {R.layout.widget_imageview, 	   R.id.widget_imageview});
        VIEWS.put("ProgressBar", 		new int[] {R.layout.widget_progressbar,    R.id.widget_progressbar});
        VIEWS.put("ViewFlipper", 		new int[] {R.layout.widget_viewflipper,    R.id.widget_viewflipper});
        VIEWS.put("ListView", 			new int[] {R.layout.widget_listview, 	   R.id.widget_listview});
        VIEWS.put("GridView", 			new int[] {R.layout.widget_gridview, 	   R.id.widget_gridview});
        //VIEWS.put("StackView", 		  new int[] {, });
        //VIEWS.put("AdapterViewFlipper", new int[] {, });
        VIEWS.put("ViewStub", 			new int[] {R.layout.widget_viewstub, 	   R.id.widget_viewstub});
        
        
        // additional
        
        VIEWS.put("UrlImageView", 		new int[] {R.layout.widget_imageview, 	   R.id.widget_imageview});
        
	}
	private static enum Args {
		text,
		on_click,
		image_path,
		image_url,
		text_color,
		visibility
	}
	public RemoteViews view = null;
	public final int view_Id;

	public RmView(Context context, String packageName, String type, String args, int widget_Id, Object _class) {
		super();
		Log.i(TAG, "Creating " + type);
		this.context = context;
		this._type = type;
		this.widget_Id = widget_Id;
		if (VIEWS.containsKey(this._type)) {
			this.view = new RemoteViews(packageName, VIEWS.get(this._type)[0]);
			this.view_Id = VIEWS.get(this._type)[1];
			
			// TODO: Figure out, why I need this
			// It seems like the newly created layouts retain
			// data from their previous equivalents.
			if (this._type.endsWith("Layout")) {
				Log.d(TAG, "Removing all children of " + this._type + ".");
				this.view.removeAllViews(this.view_Id);
			}
			
			Log.i(TAG, args);
			parseViewArgs(args, _class);
		} else {
			Log.w(TAG, "Got unknown widgetView type " + type);
			this.view_Id = -1;
		}
	}
	
	void addView(RemoteViews nestedView) {
		Log.i(TAG, "Adding view (" + nestedView.toString() + ") to " + this._type + " (" + this.view.toString() + ").");
		this.view.addView(this.view_Id, nestedView);
	}
	
	void addView(RmView child) {
		Log.i(TAG, "Adding " + child._type + " (" + child.toString() + ") to " + this._type + " (" + this.view.toString() + ").");
		this.view.addView(this.view_Id, child.view);
	}

	
	private void parseViewArgs(String args_str, Object _class) {
		if (!args_str.startsWith(")")) {
			String[] args = args_str.substring(0, args_str.indexOf(")")).split("&");
			Log.i(TAG, Arrays.toString(args));
			for (String arg: args) {
				String[] arg_val = arg.split("=");
				// urldecode
				Log.i(TAG, "Decoding arguments...");
				Log.i(TAG, Arrays.toString(arg_val));
				if (arg_val.length != 2) {
					Log.w(TAG, "Got argument that does not yield to a key-value-paire while initializing " + this._type + ": " + Arrays.toString(arg_val));
					continue;
				}
				try {
					arg_val[0] = URLDecoder.decode(arg_val[0], "UTF-8");
					arg_val[1] = URLDecoder.decode(arg_val[1], "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					Log.e(TAG, "Couldn't decode the given arg-value-pair: " + arg_val.toString(), e);
					continue;
				}
				Log.i(TAG, Arrays.toString(arg_val));
				
				
				switch (Args.valueOf(arg_val[0])) {
				case text:
					Log.i(TAG, "Setting TextView text to " + arg_val[1]);
					this.view.setTextViewText(this.view_Id, arg_val[1]);
					break;
				case on_click:
					Intent inputIntent = null;
					PendingIntent pendingIntent = null;
					if (arg_val[1].equals(".StartActivity")) {
						Log.i(TAG, "Setting on_click callback to launch the main App."); // TODO: If the main App is still open in cache, it is not able to open main.py
						inputIntent = new Intent(context, PythonActivity.class);
			            pendingIntent = PendingIntent.getActivity(context, 0, inputIntent, PendingIntent.FLAG_UPDATE_CURRENT);
					} else {
						Log.i(TAG, "Setting on_click callback: " + arg_val[1]);
						inputIntent = new Intent(context, _class.getClass());
						try {
							inputIntent.setAction((String) _class.getClass().getField("WIDGET_INPUT_UPDATE").get(_class));
						} catch (NoSuchFieldException e) {
							Log.e(TAG, "Given object " + _class + " has no field called 'WIDGET_INPUT_UPDATE'!");
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalArgumentException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						inputIntent.putExtra("UpdateAction", arg_val[1]);
						inputIntent.putExtra("WidgetId",     this.widget_Id);
						pendingIntent = PendingIntent.getBroadcast(context, 0, inputIntent, PendingIntent.FLAG_UPDATE_CURRENT);
					}
					this.view.setOnClickPendingIntent(this.view_Id, pendingIntent);
					break;
				case image_path:
					Log.i(TAG, "Setting image source to " + arg_val[1]);
					Bitmap image = BitmapFactory.decodeFile(arg_val[1]);
					if (image != null) {
						this.view.setImageViewBitmap(this.view_Id, image);
					} else {
						Log.e(TAG, "Failed loading the image located at the given path!");
						// TODO: Set to missing Resource image?
					}
					break;
				case image_url:
					Log.i(TAG, "Setting image url_source to " + arg_val[1]);
					Bitmap url_image = downloadImage(arg_val[1]);
					if (url_image != null) {
						this.view.setImageViewBitmap(this.view_Id, url_image);
					} else {
						Log.e(TAG, "Failed loading the image located at the given path!");
						// TODO: Set to missing Resource image?
					}
					break;
				case text_color:
					Log.i(TAG, "Setting Text color to " + arg_val[1]);
					if (arg_val[1].startsWith("(") || arg_val[1].startsWith("[")) {
						arg_val[1] = arg_val[1].substring(1);
					}
					if (arg_val[1].endsWith(")") || arg_val[1].endsWith("]")) {
						arg_val[1] = arg_val[1].substring(0, arg_val[1].length() - 1);
					}
					arg_val[1] = arg_val[1].replaceAll(" ", "");
					Log.i(TAG, arg_val[1]);
					String[] colors = arg_val[1].split(",");
					Integer color = null;
					if (colors.length == 1) {
						Log.i(TAG, "Color is " + colors[0]);
						color = Integer.valueOf(colors[0]);
					} else if (colors.length == 3) {
						Log.i(TAG, "Extracted RGB: " + Arrays.toString(colors));
						color = Color.rgb(Integer.valueOf(colors[0]), Integer.valueOf(colors[1]), Integer.valueOf(colors[2]));
					} else if (colors.length == 4) {
						Log.i(TAG, "Extracted RGBA: " + Arrays.toString(colors));
						color = Color.argb(Integer.valueOf(colors[3]), Integer.valueOf(colors[0]), Integer.valueOf(colors[1]), Integer.valueOf(colors[2]));
					}
					
					if (color == null){
						Log.w(TAG, "Got unknown color format: " + arg_val[1]);
						continue;
					}
					this.view.setTextColor(this.view_Id, color);
					this.view.setViewVisibility(this.view_Id, 4);
					break;
				case visibility:
					Log.i(TAG, "Set visibility to " + arg_val[1]);
					this.view.setViewVisibility(this.view_Id, Integer.valueOf(arg_val[1]));
					break;
//				case Argument:
//					break;
				default:
					Log.w(TAG, "Got an unknown argument while initializing " + this._type + ": " + Arrays.toString(arg_val));
					break;
				}
			}
		}
	}
	
	
	// Some additional stuff
	
	private InputStream openHttpConnection(String urlString) throws IOException {
        InputStream in = null;
        int response = -1;

        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();

        if (!(conn instanceof HttpURLConnection))
            throw new IOException("Not an HTTP connection");

        try {
            HttpURLConnection httpConn = (HttpURLConnection) conn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setConnectTimeout(5000);
            httpConn.setRequestMethod("GET");
            httpConn.connect();
            response = httpConn.getResponseCode();
            if (response == HttpURLConnection.HTTP_OK) {
                in = httpConn.getInputStream();
            }
        } catch (Exception ex) {
            throw new IOException("Error connecting");
        }
        return in;
    }

    private Bitmap downloadImage(String URL) {
    	// TODO: First, look for cache, then for connection
        Bitmap bitmap = null;
        InputStream in = null;
        try {
            in = openHttpConnection(URL);
            bitmap = BitmapFactory.decodeStream(in);
            in.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return bitmap;
    }
	
}
