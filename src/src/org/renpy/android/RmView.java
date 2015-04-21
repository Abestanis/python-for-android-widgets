package org.renpy.android;

/*
 * Licensed under the MIT license
 * http://opensource.org/licenses/mit-license.php
 *
 * Copyright 2014 Sebastian Scholz <abestanis.gc@gmail.com> 
 */

import java.io.File;
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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.RemoteViews;


public class RmView {
	private static final String TAG = "PythonWidgets";
    private static Map<String, int[]> VIEWS;
    public enum Args {
        on_click,
        on_child_click,
        text,
        text_color,
        text_size,
        image_path,
        visibility,
        padding,
        content_description,
        active_child,
        scroll_pos,
        show_next,
        show_prev,
        manage_children,
        orientation,
        gravity,
        chronometer_start,
        alpha {
            @Override
            public String getInputType() { return "float"; }

            @Override
            public String getFunction() { return "setAlpha"; }
        },
        clickable {
            @Override
            public String getInputType() { return "boolean"; }

            @Override
            public String getFunction() { return "setClickable"; }
        },
        allow_stretch {
            @Override
            public String getInputType() { return "boolean"; }

            @Override
            public String getFunction() { return "setAdjustViewBounds"; }
        },
        max_width {
            @Override
            public String getInputType() { return "int"; }

            @Override
            public String getFunction() { return "setMaxWidth"; }
        },
        max_height {
            @Override
            public String getInputType() { return "int"; }

            @Override
            public String getFunction() { return "setMaxHeight"; }
        },
        baseline_aligned {
            @Override
            public String getInputType() { return "boolean"; }

            @Override
            public String getFunction() { return "setBaselineAligned"; }
        },
        baseline_aligned_child {
            @Override
            public String getInputType() { return "int"; }

            @Override
            public String getFunction() { return "setBaselineAlignedChildIndex"; }
        },
        measure_with_largest_child {
            @Override
            public String getInputType() { return "boolean"; }

            @Override
            public String getFunction() { return "setMeasureWithLargestChildEnabled"; }
        },
        virtual_children {
            @Override
            public String getInputType() { return "float"; }

            @Override
            public String getFunction() { return "setWeightSum"; }
        },
        preserve_layout_size {
            @Override
            public String getInputType() { return "boolean"; }

            @Override
            public String getFunction() { return "setMeasureAllChildren"; }
        },
        indeterminate {
            @Override
            public String getInputType() { return "boolean"; }

            @Override
            public String getFunction() { return "setIndeterminate"; }
        },
        max_progress {
            @Override
            public String getInputType() { return "int"; }

            @Override
            public String getFunction() { return "setMax"; }
        },
        progress {
            @Override
            public String getInputType() { return "int"; }

            @Override
            public String getFunction() { return "setProgress"; }
        },
        secondary_progress {
            @Override
            public String getInputType() { return "int"; }

            @Override
            public String getFunction() { return "setSecondaryProgress"; }
        },
        format {
            @Override
            public String getInputType() { return "string"; }

            @Override
            public String getFunction() { return "setFormat"; }
        },
        started {
            @Override
            public String getInputType() { return "boolean"; }

            @Override
            public String getFunction() { return "setStarted"; }
        },
        flip_interval {
            @Override
            public String getInputType() { return "int"; }

            @Override
            public String getFunction() { return "setFlipInterval"; }
        },
        tint {
            @Override
            public String getInputType() { return "color"; }

            @Override
            public String getFunction() { return "setColorFilter"; }
        };

        public String getInputType() { return null; }

        public String getFunction() { return null; }
    }
    private final String    _type;
    private final Context   context;
    private final int       widget_Id;
    private final int       view_Id;
    public  RemoteViews     view                    = null;
    private boolean         manageChildren          = false;
    private boolean         hasChildOnClickListener = false;
    private int             childCount              = 0;
    private final String    packageName;
    private final Resources resources;

    public RmView(Context context, Resources resources, String type, String args, int widget_Id) {
        super();
        Log.i(TAG, "Creating " + type);
        this.context = context;
        this.resources = resources;
        this.packageName = context.getPackageName();
        if (VIEWS == null) { // Init
            VIEWS = new HashMap<String, int[]>();
            VIEWS.put("LinearLayout", getLayoutIds("widget_linearlayout"));
            VIEWS.put("FrameLayout", getLayoutIds("widget_framelayout"));
            VIEWS.put("RelativeLayout", getLayoutIds("widget_relativelayout"));
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                VIEWS.put("GridLayout", getLayoutIds("widget_gridlayout"));
            }
            VIEWS.put("TextView", getLayoutIds("widget_textview"));
            VIEWS.put("AnalogClock", getLayoutIds("widget_analogclock"));
            VIEWS.put("Button", getLayoutIds("widget_button"));
            VIEWS.put("Chronometer", getLayoutIds("widget_chronometer"));
            VIEWS.put("ImageButton", getLayoutIds("widget_imagebutton"));
            VIEWS.put("ImageView", getLayoutIds("widget_imageview"));
            VIEWS.put("ProgressBar", getLayoutIds("widget_progressbar"));
            VIEWS.put("ViewFlipper", getLayoutIds("widget_viewflipper"));
            VIEWS.put("ListView", getLayoutIds("widget_listview")); // TODO: Proper implement this
            VIEWS.put("GridView", getLayoutIds("widget_gridview"));
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
                VIEWS.put("StackView", getLayoutIds("widget_stackview"));
                VIEWS.put("AdapterViewFlipper", getLayoutIds("widget_adapterviewflipper"));
            }
            VIEWS.put("ViewStub", getLayoutIds("widget_viewstub"));
        }
        this._type = type;
        this.widget_Id = widget_Id;
        if (VIEWS.containsKey(this._type)) {
            if (this._type.equals("LinearLayout") && args.contains("orientation=horizontal")) {
                Log.i(TAG, "Setting LinearLayout orientation to horizontal.");
                int[] layoutIds = getLayoutIds("widget_linearlayout_horizontal");
                this.view = new RemoteViews(packageName, layoutIds[0]);
                this.view_Id = layoutIds[1];
            } else {
                this.view = new RemoteViews(packageName, VIEWS.get(this._type)[0]);
                this.view_Id = VIEWS.get(this._type)[1];
            }
            // TODO: Figure out, why I need this
            // It seems like the newly created layouts retain
            // data from their previous equivalents.
            if (this._type.endsWith("Layout")) {
                Log.d(TAG, "Removing all children of " + this._type + ".");
                this.view.removeAllViews(this.view_Id);
			}
			Log.d(TAG, args);
			parseViewArgs(args);
		} else {
			Log.w(TAG, "Got unknown widgetView type " + type);
			this.view_Id = -1;
		}
	}

	public void addView(RemoteViews nestedView, int viewId) {
        if (this.manageChildren) {
            int[] layoutIds = getLayoutIds("widget_weightlayout");
            RemoteViews container = new RemoteViews(this.packageName, layoutIds[0]);
            container.removeAllViews(layoutIds[1]);
            container.addView(layoutIds[1], nestedView);
            nestedView = container;
        }
        if (this.hasChildOnClickListener) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                Log.w(TAG, "Could not set the differentiation intent for child " + childCount + ". Requires at least Android 3.0 (HONEYCOMB).");
            } else {
                Log.d(TAG, "Setting differentiation intent for the " + childCount + ". child.");
                Intent differentiationIntent = new Intent(context, PythonWidgetService.class);
                differentiationIntent.putExtra("childNumber", childCount);
                nestedView.setOnClickFillInIntent(viewId, differentiationIntent);
            }
        }
        this.view.addView(this.view_Id, nestedView);
        childCount++;
	}

	public void addView(RmView child) {
		Log.i(TAG, "Adding " + child._type + " (" + child.toString() + ") to " + this._type + " (" + this.view.toString() + ").");
		this.addView(child.view, child.view_Id);
	}

	private void parseViewArgs(String args_str) {
		if (!args_str.startsWith(")")) {
			String[] args = args_str.substring(0, args_str.indexOf(")")).split("&");
			Log.d(TAG, Arrays.toString(args));
			for (String arg: args) {
                String[] arg_val = arg.split("=");
                // decode
                Log.i(TAG, "Decoding arguments...");
                Log.d(TAG, Arrays.toString(arg_val));
                if (arg_val.length != 2) {
                    Log.w(TAG, "Got argument that does not yield to a key-value-pair while initializing " + this._type + ": " + Arrays.toString(arg_val));
                    continue;
                }
                try {
                    arg_val[0] = URLDecoder.decode(arg_val[0], "UTF-8");
                    arg_val[1] = URLDecoder.decode(arg_val[1], "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Couldn't decode the given arg-value-pair: " + Arrays.toString(arg_val), e);
                    continue;
                }
                Log.d(TAG, Arrays.toString(arg_val));

                switch (Args.valueOf(arg_val[0])) {
                case on_click:
                case on_child_click:
                    Intent inputIntent;
                    PendingIntent pendingIntent;
                    if (arg_val[1].startsWith(".StartActivity")) {
                        String argsString = "";
                        if (arg_val[1].length() > 16) {
                            argsString = arg_val[1].substring(".StartActivity(".length(), arg_val[1].length() - 1);
                        }
                        Log.d(TAG, "argsString: " + argsString);
                        String[] argv = argsString.split("&");
                        for (int i = 0; i < args.length; i++) {
                            try {
                                args[i] = URLDecoder.decode(args[i], "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                                Log.e(TAG, "Couldn't decode argument number " + i + ": " + args[i], e);
                            }
                        }
                        Log.i(TAG, "Setting on_click callback to launch the main App.");
                        inputIntent = new Intent(context, PythonActivity.class);
                        inputIntent.putExtra("argv", argv);
                        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.HONEYCOMB) {
                            inputIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                        }
                        pendingIntent = PendingIntent.getActivity(context, 0, inputIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    } else {
                        if (arg_val[0].equals("on_child_click")) {
                            Log.i(TAG, "Setting on_child_click callback: " + arg_val[1]);
                            hasChildOnClickListener = true;
                        } else {
                            Log.i(TAG, "Setting on_click callback: " + arg_val[1]);
                        }
                        inputIntent = new Intent(context, PythonWidgetService.class);
                        inputIntent.setAction(PythonWidgetProvider.WIDGET_INPUT_UPDATE);
                        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.HONEYCOMB) {
                            inputIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                        }
                        inputIntent.putExtra("UpdateAction", arg_val[1]);
                        inputIntent.putExtra("widgetId", this.widget_Id);
                        inputIntent.setData(Uri.parse(this._type + "://widget/id/" + this.widget_Id + "/input/id/" + arg_val[1]));
                        pendingIntent = PendingIntent.getService(context, this.widget_Id, inputIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    }
                    if (hasChildOnClickListener && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) { // Shut of, Android Lint
                            this.view.setPendingIntentTemplate(this.view_Id, pendingIntent);
                        }
                    } else {
                        this.view.setOnClickPendingIntent(this.view_Id, pendingIntent);
                    }
                    break;
                case text:
                    Log.i(TAG, "Setting TextView text to " + arg_val[1]);
                    this.view.setTextViewText(this.view_Id, arg_val[1]);
                    break;
                case text_color:
                    Log.i(TAG, "Setting Text color to " + arg_val[1]);
                    Integer color = PythonWidgetConfigurationUI.getColor(arg_val[1]);
                    if (color != null) {
                        this.view.setTextColor(this.view_Id, color);
                    }
                    break;
                case text_size:
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        Log.w(TAG, "Could not set the text size. Requires at least Android 4.1 (JELLY BEAN).");
                    } else {
                        Log.i(TAG, "Setting Text size to " + arg_val[1]);
                        try {
                            this.view.setTextViewTextSize(this.view_Id, TypedValue.COMPLEX_UNIT_SP, Integer.valueOf(arg_val[1]));
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Text size could not be decoded as a number!");
                            e.printStackTrace();
                        }
                    }
                    break;
                case image_path:
                    Log.i(TAG, "Setting image source to " + arg_val[1]);
                    Bitmap image = null;
                    File image_file = new File(arg_val[1]);
                    if (image_file.exists() && !image_file.isDirectory()) {
                        image = BitmapFactory.decodeFile(arg_val[1]);
                    }
                    if (image == null) {
                        // The image was not found on this device or it could not be loaded as a bitmap, maybe the given source is an URL?
                        image = downloadImage(arg_val[1]);
                    }
                    if (image != null) {
                        this.view.setImageViewBitmap(this.view_Id, image);
                    } else {
                        Log.e(TAG, "Failed loading the image located at " + arg_val[1] + "!");
                        // TODO: Set to missing Resource image?
                    }
                    break;
                case visibility:
                    Log.i(TAG, "Set visibility to " + arg_val[1]);
                    if (arg_val[1].equalsIgnoreCase("VISIBLE")) {
                        this.view.setViewVisibility(this.view_Id, View.VISIBLE);
                    } else if (arg_val[1].equalsIgnoreCase("INVISIBLE")) {
                        this.view.setViewVisibility(this.view_Id, View.INVISIBLE);
                    } else if (arg_val[1].equalsIgnoreCase("GONE")) {
                        this.view.setViewVisibility(this.view_Id, View.GONE);
                    } else {
                        Log.e(TAG, "Got an invalid visibility: " + arg_val[1]);
                    }
                    break;
                case padding:
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        Log.w(TAG, "Could not set the padding. Requires at least Android 4.1 (JELLY BEAN).");
                    } else {
                        if (arg_val[1].startsWith("(") || arg_val[1].startsWith("[")) {
                            arg_val[1] = arg_val[1].substring(1);
                        }
                        if (arg_val[1].endsWith(")") || arg_val[1].endsWith("]")) {
                            arg_val[1] = arg_val[1].substring(0, arg_val[1].length() - 1);
                        }
                        arg_val[1] = arg_val[1].replaceAll(" ", "");
                        String[] padding = arg_val[1].split(",");
                        try {
                            if (padding.length == 1) {
                                int value = Integer.valueOf(padding[0]);
                                this.view.setViewPadding(this.view_Id, value, value, value, value);
                            } else if (padding.length == 2) {
                                int horizontal = Integer.valueOf(padding[0]);
                                int vertical = Integer.valueOf(padding[1]);
                                this.view.setViewPadding(this.view_Id, horizontal, vertical,
                                                         horizontal, vertical);
                            } else if (padding.length == 4) {
                                this.view.setViewPadding(this.view_Id, Integer.valueOf(padding[0]),
                                                         Integer.valueOf(padding[1]),
                                                         Integer.valueOf(padding[2]),
                                                         Integer.valueOf(padding[3]));
                            } else {
                                Log.w(TAG, "Padding list has an unexpected length: " + padding.length);
                                Log.d(TAG, Arrays.toString(padding));
                            }
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Padding could not be decoded as a number!");
                            e.printStackTrace();
                        }
                    }
                    break;
                case content_description:
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                        Log.w(TAG, "Could not set the content description. Requires at least Android 4.0.3 (ICE CREAM SANDWICH).");
                    } else {
                        Log.i(TAG, "Setting content description of " + this._type + " to " + arg_val[1]);
                        this.view.setContentDescription(this.view_Id, arg_val[1]);
                    }
                    break;
                case active_child:
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
                        Log.w(TAG, "Could not set the active child. Requires at least Android 3.1 (HONEYCOMB).");
                    } else {
                        Log.i(TAG, "Set active child to " + arg_val[1]);
                        try {
                            this.view.setDisplayedChild(this.view_Id, Integer.valueOf(arg_val[1]));
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Active child index could not be decoded as a number!");
                            e.printStackTrace();
                        }
                    }
                    break;
                case scroll_pos:
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                        Log.w(TAG, "Could not set the text size. Requires at least Android 3.0 (HONEYCOMB).");
                    } else {
                        Log.i(TAG, "Set the scroll position to " + arg_val[1]);
                        try {
                            this.view.setScrollPosition(this.view_Id, Integer.valueOf(arg_val[1]));
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "The scroll position could not be decoded as a number!");
                            e.printStackTrace();
                        }
                    }
                    break;
                case show_next:
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                        Log.w(TAG, "Could not set the text size. Requires at least Android 3.0 (HONEYCOMB).");
                    } else {
                        if (arg_val[1].equalsIgnoreCase("True")) {
                            this.view.showNext(this.view_Id);
                        }
                    }
                    break;
                case show_prev:
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                        Log.w(TAG, "Could not set the text size. Requires at least Android 3.0 (HONEYCOMB).");
                    } else {
                        if (arg_val[1].equalsIgnoreCase("True")) {
                            this.view.showPrevious(this.view_Id);
                        }
                    }
                    break;
                case manage_children:
                    Log.i(TAG, "Set manage children to " + arg_val[1]);
                    this.manageChildren = arg_val[1].equalsIgnoreCase("True");
                    break;
                case orientation:
                    break;
                case gravity:
                    Log.i(TAG, "Set gravity to " + arg_val[1]);
                    int gravity = Gravity.NO_GRAVITY;
                    for (String gravityString : arg_val[1].split("\\|")) {
                        try {
                            gravity |= (Integer) Gravity.class.getField(gravityString.toUpperCase()).get(null);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.w(TAG, "Invalid gravity was given to setGravity: " + gravityString);
                        }
                    }
                    Log.v(TAG, this._type);
                    this.view.setInt(this.view_Id, "setGravity", gravity);
                    break;
                case chronometer_start:
                    Log.i(TAG, "Set the chronometer start to " + arg_val[1]);
                    try {
                        this.view.setLong(this.view_Id, "setBase", SystemClock.elapsedRealtime() - Integer.valueOf(arg_val[1]));
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "The chronometer start could not be decoded as a number!");
                        e.printStackTrace();
                    }
                    break;
                case alpha:
                case clickable:
                case allow_stretch:
                case max_width:
                case max_height:
                case baseline_aligned:
                case baseline_aligned_child:
                case measure_with_largest_child:
                case virtual_children:
                case preserve_layout_size:
                case indeterminate:
                case max_progress:
                case progress:
                case secondary_progress:
                case format:
                case started:
                case flip_interval:
                case tint:
                    Log.i(TAG, "Setting " + arg_val[0] + " to " + arg_val[1]);
                    Args argument = Args.valueOf(arg_val[0]);
                    // There is no real documentation for this so we can't check
                    // if the function this argument depends on is available in the current android version.

                    // Handle the argument based on its type
                    if (argument.getInputType().equals("int")) {
                        try {
                            this.view.setInt(this.view_Id, argument.getFunction(), Integer.valueOf(arg_val[1]));
                        } catch (NumberFormatException e) {
                            Log.e(TAG, arg_val[0] + " could not be decoded as a number!");
                            e.printStackTrace();
                        }
                    } else if (argument.getInputType().equals("float")) {
                        try {
                            this.view.setFloat(this.view_Id, argument.getFunction(), Float.valueOf(arg_val[1]));
                        } catch (NumberFormatException e) {
                            Log.e(TAG, arg_val[0] + " could not be decoded as a float!");
                            e.printStackTrace();
                        }
                    } else if (argument.getInputType().equals("boolean")) {
                            this.view.setBoolean(this.view_Id, argument.getFunction(), arg_val[1].equalsIgnoreCase("True"));
                    } else if (argument.getInputType().equals("color")) {
                        Integer clr = PythonWidgetConfigurationUI.getColor(arg_val[1]);
                        if (clr != null) {
                            this.view.setTextColor(this.view_Id, clr);
                        }
                    } else if (argument.getInputType().equals("string")) {
                        this.view.setString(this.view_Id, argument.getFunction(), arg_val[1]);
                    } else if (argument.getInputType() != null) {
                        Log.e(TAG, "RmView.parseViewArgs: Missing implementation for input type: " + argument.getInputType());
                    }
                    break;
                default:
                    Log.w(TAG, "Ignoring an unknown argument while initializing " + this._type + ": " + Arrays.toString(arg_val));
                    break;
                }
                // Unimplemented:
                // - setEmptyView
                // - setLabelFor
                // - setRemoteAdapter
                // - setTextViewCompoundDrawables
                // - setTextViewCompoundDrawablesRelative
            }
		}
	}

	// helper functions

    private int[] getLayoutIds(String name) {
        return new int[] {resources.getIdentifier(name, "layout", packageName), resources.getIdentifier(name, "id", packageName)};
    }

	private InputStream openHttpConnection(String urlString) throws IOException {
        InputStream in = null;
        int response;

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
        InputStream in;
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
