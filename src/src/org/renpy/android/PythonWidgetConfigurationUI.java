package org.renpy.android;

/*
 * Licensed under the MIT license
 * http://opensource.org/licenses/mit-license.php
 *
 * Copyright 2014 Sebastian Scholz <abestanis.gc@gmail.com> 
 */

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;
import android.graphics.LinearGradient;

public class PythonWidgetConfigurationUI {
	
	private static final String TAG = "PythonWidgets";
	private static final Integer CONFIG_ITEM = 222;
	
	interface OnResultListener {
	    void onDone(String key, String result);
	    void onCancel();
	}
	
	static public class MaxWidthLinearLayout extends LinearLayout {

		private int _maxWidth 		   = Integer.MAX_VALUE;
		private float _maxWidthPercent = 1f;
		private DisplayMetrics screen  = null;
		private Rect layoutArea 	   = new Rect();

		public MaxWidthLinearLayout(Context context) {
		    super(context);
		    screen = context.getResources().getDisplayMetrics();
		}
		
		public MaxWidthLinearLayout(Context context, AttributeSet attr) {
		    super(context, attr);
		    screen = context.getResources().getDisplayMetrics();
		}
		
		public void setMaxWidth(int width) {
			_maxWidth = width;
			_maxWidthPercent = 1f;
		}
		
		public int getMaxWidth() {
			return _maxWidth;
		}
		
		public void setMaxWidthPercentage(float percentage) {
			_maxWidthPercent = Math.min(1f, Math.max(0f, percentage));
			_maxWidth = Integer.MAX_VALUE;
		}
		
		public float getMaxWidthPercentage() {
			return _maxWidthPercent;
		}
		
		protected TouchDelegate getTouchDelegate(View child) {
			return new TouchDelegate(layoutArea, child);
		}
		
		
		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		    int maxWidth      = (int) Math.min(_maxWidth, (screen.widthPixels * _maxWidthPercent));
		    int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
	        if (maxWidth > 0 && maxWidth < measuredWidth) {
	            int measureMode = MeasureSpec.getMode(widthMeasureSpec);
	            widthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth, measureMode);
	        }
	        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	        if (getChildCount() == 1) {
				View child = getChildAt(0);
				
                // Extend the touch area
                layoutArea.right  = getWidth();
                layoutArea.left   = -getWidth();
                layoutArea.bottom = getHeight();
                layoutArea.top	 = -getHeight();
				
                TouchDelegate touchDelegate = getTouchDelegate(child);
                
                // Sets the TouchDelegate on the parent view, such that touches 
                // within the touch delegate bounds are routed to the child.
                setTouchDelegate(touchDelegate);
			}
		}
	}
	
	static public class Sepperator extends View {
		Integer _color = null;
		Paint paint    = new Paint();
		Drawable _draw = new ListView(getContext()).getDivider();
		
		public Sepperator(Context context) {
			super(context);
		}
		
		public void setColor(Integer color) {
			_color = color;
		}
		
		public Integer getColor() {
			return _color;
		}
		
		@Override
		protected void onDraw(Canvas canvas) {
			if (_color != null) {
				paint.setColor(this.getColor());
				canvas.drawRect(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom(), paint);
			} else {
				_draw.setBounds(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
				_draw.draw(canvas);
			}
		}
	}
	
	static public class ColorPicker extends View {
		private float[] _hsv   = {0.0f, 0.0f, 0.0f};
		private Integer _alpha = null;
		private Paint   paint  = new Paint();
		private Integer focus  = -1;
		private boolean _allowAlpha = true;
		private OnColorChangeListener _listener;
		private Rect    colorRect;
		private RectF   colorIndecator;
		private Shader  colorShader;
		private Rect    lightnessRect;
		private Shader  constLightnessShader;
		private Shader  lightnessShader;
		private Rect    alphaRect;
		private RectF   alphaIndecator;
		private Shader  alphaShader;
		private Shader  checkerBoardShader;
		
		private final static int[] colors = {
			Color.RED,
			Color.MAGENTA,
			Color.BLUE,
			Color.CYAN,
			Color.GREEN,
			Color.YELLOW,
			Color.RED,
		};
		
		interface OnColorChangeListener {
		    void onColorChange(ColorPicker instance, float[] hsv, Integer alpha);
		}
		
		
		public ColorPicker(Context context, int color, boolean allowAlpha) {
			super(context);
			_allowAlpha = allowAlpha;
			setColor(color);
			setPadding(20, 20, 20, 20);
			createCheckerBoardShader();
			if (_allowAlpha) {
				setMinimumWidth(110);
				setMinimumHeight(100);
			} else {
				setMinimumWidth(70);
				setMinimumHeight(60);
			}
		}
		
		public ColorPicker(Context context, int color, boolean allowAlpha, OnColorChangeListener listener) {
			super(context);
			_allowAlpha = allowAlpha;
			setColor(color);
			setPadding(20, 20, 20, 20);
			createCheckerBoardShader();
			if (_allowAlpha) {
				setMinimumWidth(110);
				setMinimumHeight(100);
			} else {
				setMinimumWidth(70);
				setMinimumHeight(60);
			}
			_listener = listener;
		}
		
		public ColorPicker(Context context, int color, int alpha) {
			super(context);
			setColor(color);
			setColorAlpha(alpha);
			setPadding(20, 20, 20, 20);
			createCheckerBoardShader();
			setMinimumWidth(110);
			setMinimumHeight(100);
		}
		
		public ColorPicker(Context context, int color, int alpha, OnColorChangeListener listener) {
			super(context);
			setColor(color);
			setColorAlpha(alpha);
			setPadding(20, 20, 20, 20);
			createCheckerBoardShader();
			setMinimumWidth(110);
			setMinimumHeight(100);
			_listener = listener;
		}
		
		public Integer getColor() {
			return Color.HSVToColor(_alpha, _hsv);
		}
		
		public Integer getColorAlpha() {
			return _alpha;
		}
		
		public void setColor(int alpha, float[] hsv) {
			if (_allowAlpha) {
				_alpha = alpha;
			} else {
				_alpha = 255;
			}
			_hsv   = hsv;
			if (lightnessRect != null) {
				updateLightnessShader();
				invalidate();
			}
			dispatchCallback();
		}
		
		public void setColor(float[] hsv) {
			if (_alpha == null) {
				_alpha = 255;
			}
			_hsv   = hsv;
			if (lightnessRect != null) {
				updateLightnessShader();
				invalidate();
			}
			dispatchCallback();
		}
		
		public void setColor(Integer color) {
			if (_allowAlpha) {
				_alpha = Color.alpha(color);
			} else {
				_alpha = 255;
			}
			Color.colorToHSV(color, _hsv);
			if (lightnessRect != null) {
				updateLightnessShader();
				invalidate();
			}
			dispatchCallback();
		}
		
		public void setColorAlpha(Integer alpha) {
			if (_allowAlpha) {
				_alpha = alpha;
				if (alphaRect != null) {
					invalidate();
				}
				dispatchCallback();
			} else {
				_alpha = 255;
			}
		}
		
		
		protected void updateLightnessShader() {
			lightnessShader = new LinearGradient(lightnessRect.left, 0, lightnessRect.right, 0, Color.WHITE, Color.HSVToColor(new float[] {_hsv[0], 1.0f, 1.0f}), TileMode.CLAMP);
			updateAlphaShader();
		}
		
		protected void updateAlphaShader() {
			if (_allowAlpha) {
				alphaShader = new LinearGradient(alphaRect.left, 0, alphaRect.right, 0, Color.HSVToColor(_hsv), Color.TRANSPARENT, TileMode.CLAMP);
			}
		}
		
		protected void createCheckerBoardShader() {
			if (!_allowAlpha) {
				return;
			}
		    checkerBoardShader = new BitmapShader(createCheckerBoard(), BitmapShader.TileMode.REPEAT, BitmapShader.TileMode.REPEAT);
		}
		
		protected void dispatchCallback() {
			if (_listener != null) {
				_listener.onColorChange(this, _hsv, _alpha);
			}
		}
		
		
		@Override
		public boolean onTouchEvent(MotionEvent input) {
			float x = input.getX();
			float y = input.getY();
			
			if (focus == 0 || (focus == -1 && colorRect.contains((int) x, (int) y))) {
				y = Math.min(colorRect.bottom, Math.max(colorRect.top, y));
				focus = 0;
				y -= colorRect.top;
				_hsv[0] = 360 - (y / (colorRect.height() - 1)) * 360;
				updateLightnessShader();
			} else if (focus == 1 || (focus == -1 && lightnessRect.contains((int) x, (int) y))) {
				y = Math.min(lightnessRect.bottom, Math.max(lightnessRect.top, y));
				x = Math.min(lightnessRect.right, Math.max(lightnessRect.left, x));
				focus = 1;
				x -= lightnessRect.left;
				y -= lightnessRect.top;
				_hsv[1] = x / (lightnessRect.width() - 1);
				_hsv[2] = 1.0f - (y / (lightnessRect.height() - 1));
				updateAlphaShader();
			} else if (_allowAlpha && (focus == 2 || (focus == -1 && alphaRect.contains((int) x, (int) y)))) {
				x = Math.min(alphaRect.right, Math.max(alphaRect.left, x));
				focus = 2;
				x -= alphaRect.left;
				_alpha = (int) (255 - (x / (alphaRect.width() - 1)) * 255);
			} else {
				if (input.getAction() != MotionEvent.ACTION_MOVE) {
					focus = -1;
				}
				return false;
			}
			if (input.getAction() == MotionEvent.ACTION_UP) {
				focus = -1;
			}
			invalidate();
			dispatchCallback();
			return true;
		}
		
		@Override
		protected void onSizeChanged(int width, int height, int oldw, int oldh) {
			int alphaHeight = 0;
			if (_allowAlpha) {
				alphaHeight = 30;
			}
			colorRect = new Rect(width - getPaddingRight() - 40, getPaddingTop(), width - getPaddingRight(), height - getPaddingBottom() - alphaHeight - 10);
			colorIndecator = new RectF(colorRect.left - 1, colorRect.top - 2, colorRect.right + 1, colorRect.top + 3);
			colorShader = new LinearGradient(0, colorRect.top, 0, colorRect.bottom, colors, null, TileMode.CLAMP);
			
			lightnessRect = new Rect(getPaddingLeft(), getPaddingTop(), width - getPaddingRight() - 50, height - getPaddingBottom() - alphaHeight - 10);
			constLightnessShader = new LinearGradient(0, lightnessRect.top, 0, lightnessRect.bottom, Color.TRANSPARENT, Color.BLACK, TileMode.CLAMP);
			
			if (_allowAlpha) {
				alphaRect = new Rect(getPaddingLeft(), height - getPaddingBottom() - alphaHeight, width - getPaddingRight(), height - getPaddingBottom());
				alphaIndecator = new RectF(alphaRect.left - 2, alphaRect.top - 1, alphaRect.left + 3, alphaRect.bottom + 1);
			}
			updateLightnessShader();
		}
		
		@Override
		protected void onDraw(Canvas canvas) {
			// Draw the rectangles
			paint.setStyle(Paint.Style.FILL);
			paint.setAntiAlias(false);
			paint.setStrokeWidth(0);
			paint.setShader(colorShader);
			canvas.drawRect(colorRect, paint);
			paint.setShader(lightnessShader);
			canvas.drawRect(lightnessRect, paint);
			paint.setShader(constLightnessShader);
			canvas.drawRect(lightnessRect, paint);
			if (_allowAlpha) {
				paint.setShader(checkerBoardShader);
				canvas.drawRect(alphaRect, paint);
				paint.setShader(alphaShader);
				canvas.drawRect(alphaRect, paint);
			}
			
			// Draw a border around the lightnessRect
			paint.setShader(null);
			paint.setStyle(Paint.Style.STROKE);
			paint.setColor(Color.DKGRAY);
			canvas.drawRect(lightnessRect.left, lightnessRect.top, lightnessRect.right + 1, lightnessRect.bottom + 1, paint);
			
			// Draw the indicators
			paint.setAntiAlias(true);
			paint.setStrokeWidth(2);
			paint.setColor(0xff222222);
			
			int colorX = (int) (((colorRect.height() - 1) / 360.0f) * (360 - _hsv[0]));
			colorIndecator.top    = colorRect.top - 2 + colorX;
			colorIndecator.bottom = colorRect.top + 3 + colorX;
			canvas.drawRoundRect(colorIndecator, colorIndecator.width(), 1, paint);
			
			if (_allowAlpha) {
				int alphaY = (int) (((alphaRect.width() - 1) / 255.0f) * (255 - _alpha));
				alphaIndecator.left   = alphaRect.left - 2 + alphaY;
				alphaIndecator.right  = alphaRect.left + 3 + alphaY;
				canvas.drawRoundRect(alphaIndecator, 1, alphaIndecator.height(), paint);
			}
			
			int saturation = (int) ((lightnessRect.width() - 1) * _hsv[1]);
			int lightness  = (int) ((lightnessRect.height() - 1) * (1.0f - _hsv[2]));
			paint.setColor(Color.WHITE);
			canvas.drawCircle(lightnessRect.left + 1 + saturation, lightnessRect.top + 1 + lightness, 6.5f, paint);
			paint.setColor(Color.BLACK);
			canvas.drawCircle(lightnessRect.left + 1 + saturation, lightnessRect.top + 1 + lightness, 4.5f, paint);
		}
	}
	
	static public class ColorView extends TextView {
		private Integer _color = null;
		private Paint   paint  = new Paint();
		static private Shader checkerBoardShader;
		
		public ColorView(Context context, int color) {
			super(context);
			setColor(color);
			if (checkerBoardShader == null) {
				createCheckerBoardShader();
			}
		}
		
		public Integer getColor() {
			return _color;
		}
		
		public void setColor(Integer color) {
			_color = color;
			invalidate();
		}
		static protected void createCheckerBoardShader() {
			checkerBoardShader = new BitmapShader(createCheckerBoard(), BitmapShader.TileMode.REPEAT, BitmapShader.TileMode.REPEAT);
		}
		
		@Override
		protected void onDraw(Canvas canvas) {
			paint.setStyle(Paint.Style.FILL);
			paint.setColor(Color.WHITE);
			paint.setShader(checkerBoardShader);
			canvas.drawRect(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom(), paint);
			paint.setShader(null);
			paint.setColor(_color);
			canvas.drawRect(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom(), paint);
			super.onDraw(canvas);
		}
	}
	
	static public class TextSeekBar extends RelativeLayout {
		private SeekBar _skbar;
		final private TextView _progress;
		final private TextView _min;
		final private TextView _max;
		
		public TextSeekBar(Context context, Integer value, Integer min, Integer max) {
			super(context);
			
			if (min == null) {
				min = 0;
			}
			if (max == null) {
				max = 100;
			}
			if (max < min) {
				Log.w(TAG, "SeekBar got invalid min and max values (min: " + min + ", max: " + max + "), max must be greater or equal to min! Setting max to min.");
				max = min;
			}
			
			_skbar 	  = new SeekBar(context);
			_progress = new TextView(context);
			_min 	  = new TextView(context);
			_max 	  = new TextView(context);
			
			_progress.setId(1);
			_min.setId(2);
			_max.setId(3);
			
			LayoutParams skbarLayoutParams 	  = new LayoutParams(LayoutParams.WRAP_CONTENT,  LayoutParams.MATCH_PARENT);
			LayoutParams progressLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
			LayoutParams minLayoutParams 	  = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
			LayoutParams maxLayoutParams 	  = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
			
			skbarLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			skbarLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			skbarLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			skbarLayoutParams.addRule(RelativeLayout.BELOW, _progress.getId());
			skbarLayoutParams.addRule(RelativeLayout.BELOW, _min.getId());
			skbarLayoutParams.addRule(RelativeLayout.BELOW, _max.getId());
			minLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			minLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			progressLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			progressLayoutParams.addRule(RelativeLayout.RIGHT_OF, _min.getId());
			progressLayoutParams.addRule(RelativeLayout.LEFT_OF, _max.getId());
			maxLayoutParams.addRule(ALIGN_PARENT_TOP);
			maxLayoutParams.addRule(ALIGN_PARENT_RIGHT);
						
			_skbar.setLayoutParams(skbarLayoutParams);
			_progress.setLayoutParams(progressLayoutParams);
			_min.setLayoutParams(minLayoutParams);
			_max.setLayoutParams(maxLayoutParams);
			
			_min.setText(String.valueOf(min));
			_max.setText(String.valueOf(max));
			_progress.setText(_min.getText());
			_progress.setGravity(Gravity.CENTER);
			_skbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {}
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {}
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					_progress.setText(String.valueOf(progress + Integer.valueOf(_min.getText().toString())));
				}
			});
			_skbar.setProgress(value - min);
			_skbar.setMax(max - min);
			
			this.addView(_min);
			this.addView(_progress);
			this.addView(_max);
			this.addView(_skbar);
		}
		
		public Integer getProgress() {
			return Integer.valueOf(_progress.getText().toString());
		}	
	}	
	
	static public class EntryAdapter extends ArrayAdapter<View> {
	    private ArrayList<View> childs;
	    public EntryAdapter(Context context, ArrayList<View> items) {
	        super(context, 0, items);
	        this.childs = items;
	    }
	    
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	        return childs.get(position);
	    }
	}
	
	
	static public Object getdefault(Map<?,?> map, Object key) {
		return getdefault(map, key, null);
	}
	
	static public Object getdefault(Map<?,?> map, Object key, Object dflt) {
		if (map.containsKey(key)) {
			return map.get(key);
		} else {
			return dflt;
		}
	}
	
	static public int getColor(String color_string) {
		if (color_string.startsWith("(") || color_string.startsWith("[")) {
			color_string = color_string.substring(1);
		}
		if (color_string.endsWith(")") || color_string.endsWith("]")) {
			color_string = color_string.substring(0, color_string.length() - 1);
		}
		color_string = color_string.replaceAll(" ", "");
		String[] colors = color_string.split(",");
		Integer color = null;
		if (colors.length == 1) {
			try {
				color = Color.parseColor(colors[0]);
			} catch (IllegalArgumentException e) {
				try {
					color = Color.parseColor("#" + colors[0]);
				} catch (IllegalArgumentException e2) {
					Log.w(TAG, colors[0] + " could not be parsed to an color,");
				}
			}
		} else if (colors.length == 3) {
			color = Color.rgb(Integer.valueOf(colors[0]), Integer.valueOf(colors[1]), Integer.valueOf(colors[2]));
		} else if (colors.length == 4) {
			color = Color.argb(Integer.valueOf(colors[3]), Integer.valueOf(colors[0]), Integer.valueOf(colors[1]), Integer.valueOf(colors[2]));
		}
		
		if (color == null){
			Log.w(TAG, "Got unknown color format: " + color_string);
		}
		return color;
	}
	
	static public Calendar getTime(String time_string) {
		Calendar time = new GregorianCalendar();
		time_string = time_string.trim();
		if (time_string.matches("-?\\d+(\\.\\d+)?")) {// timestamp via time.time()
			String[] timeparts = time_string.split("\\.");
			try {
				time.setTimeInMillis(Long.parseLong(timeparts[0]) * 1000 + Long.parseLong(timeparts[1]));
			} catch (NumberFormatException  e) {
				Log.w(TAG, "Given default time '" + time_string + "' for date input couldn't be parsed to a time from python timestamp.");
				e.printStackTrace();
				time.set(1970, 0, 0, 0, 0, 0);
			}
		} else {// string time in format 'dd.mm.yyyy hh:mm:ss' or 'dd.mm.yyyy' or 'hh:mm:ss'
			String[] timeparts = time_string.split(" ");
			String year, month, day, hour, min, sec;
			month = day = hour = min = sec = "0";
			year = "1970";
			if (timeparts.length == 2) {// We have 'dd.mm.yyyy hh:mm:ss' or 'hh:mm:ss dd.mm.yyyy'
				if (timeparts[0].contains(":") && timeparts[1].contains("\\.")) {// We have 'hh:mm:ss dd.mm.yyyy'
					String[] tmp = timeparts[0].split(":");
					hour = tmp[0];
					min  = tmp[1];
					if (tmp.length == 3) {
						sec = tmp[2];
					}
					tmp = timeparts[1].split("\\.");
					if (tmp.length == 3) {
						day   = tmp[0];
						month = tmp[1];
						year  = tmp[2];
					} else {
						month = tmp[0];
						year  = tmp[1];
					}
				} else if (timeparts[0].contains("\\.") && timeparts[1].contains(":")) {// We have 'dd.mm.yyyy hh:mm:ss'
					String[] tmp = timeparts[1].split(":");
					hour = tmp[0];
					min  = tmp[1];
					if (tmp.length == 3) {
						sec = tmp[2];
					}
					tmp = timeparts[0].split("\\.");
					if (tmp.length == 3) {
						day   = tmp[0];
						month = tmp[1];
						year  = tmp[2];
					} else {
						month = tmp[0];
						year  = tmp[1];
					}
				} else {
					Log.w(TAG, "Given default time '" + time_string + "' for date input couldn't be parsed to a time.");
				}
			} else if (timeparts.length == 1) {// We have 'hh:mm:ss' or 'dd.mm.yyyy'
				if (time_string.contains(":")) {// We have 'hh:mm:ss'
					String[] tmp = time_string.split(":");
					hour = tmp[0];
					min  = tmp[1];
					if (tmp.length == 3) {
						sec = tmp[2];
					}
				} else if (time_string.contains(".")) {// We have 'dd.mm.yyyy'
					String[] tmp = time_string.split("\\.");
					if (tmp.length == 3) {
						day   = tmp[0];
						month = tmp[1];
						year  = tmp[2];
					} else {
						month = tmp[0];
						year  = tmp[1];
					}
				} else {
					Log.w(TAG, "Given default time '" + time_string + "' for date input couldn't be parsed to a time.");
				}
			} else {
				Log.w(TAG, "Given default time '" + time_string + "' for date input couldn't be parsed to a time.");
			}
			try {
				time.set(Integer.parseInt(year), Math.max(Integer.parseInt(month) - 1, 0), Integer.parseInt(day), Integer.parseInt(hour), Integer.parseInt(min), Integer.parseInt(sec));
			} catch (NumberFormatException  e) {
				Log.w(TAG, "Given default time '" + time_string + "' for date input couldn't be parsed to a time, at least one value is not an integer.");
				e.printStackTrace();
				time.set(1970, 0, 0, 0, 0, 0);
			}
		}
		return time;
	}
	
	static public String String_repeat(Integer n, String s) {
		if (n <= 0) {
			return "";
		}
		return new String(new char[n]).replace("\0", s);
	}
	
	static public Bitmap createCheckerBoard() {
		int blockSize = 5;
	    Bitmap bitmap = Bitmap.createBitmap(blockSize * 2, blockSize * 2, Bitmap.Config.ARGB_8888);

	    Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
	    fill.setStyle(Paint.Style.FILL);
	    fill.setColor(Color.WHITE);
	    
	    Canvas canvas = new Canvas(bitmap);
	    canvas.drawRect(0, 0, blockSize * 2, blockSize * 2, fill);
	    fill.setColor(0x22000000);
	    Rect rect = new Rect(0, 0, blockSize, blockSize);
	    canvas.drawRect(rect, fill);
	    rect.offset(blockSize, blockSize);
	    canvas.drawRect(rect, fill);
	    
	    return bitmap;
	}
	
	static public void setConfigUI(String ui_description, final Activity targetActivity, final OnResultListener resultListener) {
/* 
x = {
'title': 'test',
'children': [
    "Test1",
    {'type': 'switch', 'state': 0, 'desc': 'Visibility'},
    {'type': 'text', 'desc': 'Lets explain visibility!', 'text_color': [100,100,100]},
    {'type': 'separator'},
    {'type': 'text_input',   'default': 'Bob', 'text_hint': 'Your name, e.g. Tom...', 'hint': 'Set your playername.\nPS: Use with care!', 'desc': 'Name'},
    {'type': 'date_input',   'default': '21.10.2014'},
    {'type': 'date_input',   'default': '100.15.0'},
    {'type': 'date_input',   'default': 1414059291.549, 'format': 'yyyy-MM-dd'},
    {'type': 'time_input',   'default': '10:45', 'is24h': False},
    {'type': 'time_input',   'default': '33:5'},
    {'type': 'time_input',   'default': 1414059291.549, 'format': 'HH \'hours\', mm \'minutes and\' ss \'seconds\''},
    {'type': 'num_input',    'default': 20,    'text_hint': 'Your age...',            'hint': 'Set your age.\nWarning: You can\'t play this game if your not 18 or older!', 'desc': 'Age', 'min': 3, 'max': 100},
    {'type': 'list_option',  'default': 'Choose an option', 'options': ['Human', 'Elf', 'Dwarf'], 'hint': 'Set your playertype.', 'desc': 'Playertype'},
    {'type': 'color_picker', 'default': [100,100,100], 'transparent': True, 'hint': 'Set your color.', 'desc': 'Team color'},
    {'type': 'seek_bar',     'default': 20, 'max': 25, 'hint': 'Idk...', 'hint_text_size': 'default', 'desc': 'Some thing'},
]
}

def do(d):
    from urllib import urlencode
    for name in d.keys():
            val = d[name]
            if type(val) == list:
                    for i in range(0, len(val)):
                            val[i] = do(val[i]) if type(val[i]) == dict else val[i]
            elif type(val) == dict:
                    val = urlencode(val)
            d[name] = val
    return urlencode(d)

do(x)

*/

		//ui_description = "children=%5B%27Test1%27%2C+%27state%3D0%26type%3Dswitch%26desc%3DVisibility%27%2C+%27text_color%3D%255B100%252C%2B100%252C%2B100%255D%26type%3Dtext%26desc%3DLets%2Bexplain%2Bvisibility%2521%27%2C+%27type%3Dseparator%27%2C+%27default%3DBob%26text_hint%3DYour%2Bname%252C%2Be.g.%2BTom...%26desc%3DName%26type%3Dtext_input%26hint%3DSet%2Byour%2Bplayername.%250APS%253A%2BUse%2Bwith%2Bcare%2521%27%2C+%27default%3D21.10.2014%26type%3Ddate_input%27%2C+%27default%3D100.15.0%26type%3Ddate_input%27%2C+%27default%3D1414059291.55%26type%3Ddate_input%26format%3Dyyyy-MM-dd%27%2C+%27default%3D10%253A45%26is24h%3DFalse%26type%3Dtime_input%27%2C+%27default%3D33%253A5%26type%3Dtime_input%27%2C+%27default%3D1414059291.55%26type%3Dtime_input%26format%3DHH%2B%2527Stunden%2527%252C%2Bmm%2B%2527Minuten%2Bund%2527%2Bss%2B%2527Sekunden%2527%27%2C+%27text_hint%3DYour%2Bage...%26min%3D3%26default%3D20%26max%3D100%26hint%3DSet%2Byour%2Bage.%250AWarning%253A%2BYou%2Bcan%2527t%2Bplay%2Bthis%2Bgame%2Bif%2Byour%2Bnot%2B18%2Bor%2Bolder%2521%26type%3Dnum_input%26desc%3DAge%27%2C+%27default%3DChoose%2Ban%2Boption%26desc%3DPlayertype%26type%3Dlist_option%26options%3D%255B%2527Human%2527%252C%2B%2527Elf%2527%252C%2B%2527Dwarf%2527%255D%26hint%3DSet%2Byour%2Bplayertype.%27%2C+%27default%3D%255B100%252C%2B100%252C%2B100%255D%26desc%3DTeam%2Bcolor%26type%3Dcolor_picker%26transparent%3DTrue%26hint%3DSet%2Byour%2Bcolor.%27%2C+%27hint_text_size%3Ddefault%26default%3D20%26max%3D25%26hint%3DIdk...%26type%3Dseek_bar%26desc%3DSome%2Bthing%27%5D&title=test";
		Context context = targetActivity.getBaseContext();
		String[] opts = ui_description.split("&");
		Map<String, String> options = new HashMap<String, String>();
		for (int i = 0; i < opts.length; i++) {
			String[] tmp = opts[i].split("=");
			try {
				options.put(URLDecoder.decode(tmp[0], "UTF-8"), URLDecoder.decode(tmp[1], "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, "Couldn't decode the given option: " + opts[i].toString(), e);
				e.printStackTrace();
				continue;
			}
		}
		if (options.containsKey("children")) { // Parse the children data
			
			ArrayList<Map<String, String>> children_data = new ArrayList<Map<String, String>>();
			
			for (String child: options.get("children").substring(2, options.get("children").length() - 2).split("', '")) {
				Map<String, String> cld = new HashMap<String, String>();
				for (String option: child.split("&")) {
					String[] tmp = option.split("=");
					if (tmp.length == 2) {
						try {
							cld.put(URLDecoder.decode(tmp[0], "UTF-8"), URLDecoder.decode(tmp[1], "UTF-8"));
						} catch (UnsupportedEncodingException e) {
							Log.e(TAG, "Couldn't decode the given child element option: " + child.toString(), e);
							e.printStackTrace();
							continue;
						}
					}
				}
				children_data.add(cld);
			}
			Log.d(TAG, "Got " + children_data.size() + " children.");
			
			if (options.containsKey("title")) {
				targetActivity.setTitle(options.get("title"));
			}
			final String configSaveKey = (String) getdefault(options, "save_key");
			
			
			// Creating Views and Layouts //
			RelativeLayout layout 		   = new RelativeLayout(context);
			LinearLayout btn_container	   = new LinearLayout(context);
			Button cancel_btn 			   = new Button(context);
			Button done_btn				   = new Button(context);
			ListView container 			   = new ListView(context);
			final ArrayList<View> elements = new ArrayList<View>();
			
			// Setting their attributes //
			if (options.containsKey("background")) {
				Integer backgroundColor = getColor(options.get("background"));
				if (backgroundColor != null) {
					layout.setBackgroundColor(backgroundColor);
				} else {
					Log.w(TAG, "Could not set backgroundcolor for the configuration UI!");
				}
			}
			btn_container.setId(1);
			btn_container.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			container.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			RelativeLayout.LayoutParams lp1 = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			lp1.addRule(RelativeLayout.ABOVE, btn_container.getId());
			lp1.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			lp2.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			cancel_btn.setText("Cancel");
			cancel_btn.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1f));
			cancel_btn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					resultListener.onCancel();
				}
			});
			done_btn.setText("Done");
			done_btn.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1f));
			done_btn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String result = "";
					for (View item : elements) {
						if (item.getClass().equals(LinearLayout.class)) {
							LinearLayout layout = (LinearLayout) item;
							View configItem = null;
							for (int i = 0; i < layout.getChildCount(); i++) {
								if (configItem == null) {
									if (layout.getChildAt(i).getTag() == CONFIG_ITEM) {
										configItem = layout.getChildAt(i);
									} else if (MaxWidthLinearLayout.class.isAssignableFrom(layout.getChildAt(i).getClass())) {
										MaxWidthLinearLayout innerLayout = (MaxWidthLinearLayout) layout.getChildAt(i);
										for (int index = 0; index < innerLayout.getChildCount(); index++) {
											if (configItem == null) {
												if (innerLayout.getChildAt(index).getTag() == CONFIG_ITEM) {
													configItem = innerLayout.getChildAt(index);
												}
											}
										}
									}
								}
							}
							if (configItem == null) {
								Log.w(TAG, "Was unable to extract the view witch holds the config value from " + item.toString() + "!");
								continue;
							}
							try {
								if (ColorView.class.isAssignableFrom(configItem.getClass())) {
									result += URLEncoder.encode(
											Integer.toHexString(((ColorView) configItem).getColor()),
											"UTF-8");
								} else if (CompoundButton.class.isAssignableFrom(configItem.getClass())) {
									result += URLEncoder.encode(
											String.valueOf(((CompoundButton) configItem).isChecked()),
											"UTF-8");
								} else if (TextSeekBar.class.isAssignableFrom(configItem.getClass())) {
									result += URLEncoder.encode(
											String.valueOf(((TextSeekBar) configItem).getProgress()),
											"UTF-8");
								} else if (TextView.class.isAssignableFrom(configItem.getClass())) {
									result += URLEncoder.encode(
											((TextView) configItem).getText().toString(),
											"UTF-8");
								} else {
									Log.w(TAG, "Could not ectract value from " + configItem.toString() + ", unexpected class!");
									continue;
								}
								result += ",";
							} catch (UnsupportedEncodingException e) {
								Log.w(TAG, "Could not ectract value from " + configItem.toString() + ", urlencoding failed!");
								e.printStackTrace();
							}
						}
					}
					resultListener.onDone(configSaveKey, result.substring(0, result.length() - 1));
				}
			});
			
			// Building the actual UI
			
			for (Map<String,String> item : children_data) {
				View new_item = buildItem(context, targetActivity, item);
				if (new_item != null) {
					elements.add(new_item);
				}
			}
			EntryAdapter adapter = new EntryAdapter(context, elements);
			container.setAdapter(adapter);
			
			// Adding our views //
			btn_container.addView(cancel_btn);
			btn_container.addView(done_btn);
			layout.addView(container, lp1);
			layout.addView(btn_container, lp2);
			
			targetActivity.setTheme(android.R.style.Theme);
			targetActivity.setContentView(layout);
		} else {
			Log.w(TAG, "Got no children to display as a configuration UI!");
			targetActivity.finish();
		}
	}
	
	@SuppressLint("NewApi")
	static protected View buildItem(Context context, Activity uiActivity, Map<String, String> item_description) {
		View item = null;
		Log.d(TAG, "Proccessing " + item_description.toString());
		if (!item_description.containsKey("type")) {
			Log.w(TAG, "Got child without a type!");
			return null;
		}
		String type = item_description.get("type");
		if (type.equals("switch") && (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH || uiActivity.getApplicationInfo().targetSdkVersion <= 10)) {
			Log.w(TAG, "Displaying a togglebutton insteat of a switch, because a switch requires android version 4.0 (ICE_CREAM_SANDWICH) or higher.");
			type = "togglebutton";
		}
		if (type.equals("separator")) {
			item = new Sepperator(context);
            item.setLayoutParams(new android.widget.AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, 2));
		} else if (type.equals("text")) {
			TextView text = new TextView(context);
			text.setPadding(20, 0, 20, 0);
			if (item_description.containsKey("text")) {
				text.setText(item_description.get("text"));
			} else {
				Log.w(TAG, "Got a text item with no text! Intended?");
			}
			if (item_description.containsKey("text_size")) {
				try {
					text.setTextSize(Integer.valueOf(item_description.get("text_size")));
				} catch (NumberFormatException e) {
					Log.w(TAG, "Could not set the textsize because '" + item_description.get("text_size") + "' is not a number!");
					e.printStackTrace();
				}
			}
			if (item_description.containsKey("text_color")) {
				Integer color = getColor(item_description.get("text_color"));
				if (color != null){
					text.setTextColor(color);
				}
			}
			item = text;
		} else if (type.equals("togglebutton")) {
			ToggleButton tglbtn = new ToggleButton(context);
			
			if (item_description.containsKey("text_on")) {
				tglbtn.setTextOn(item_description.get("text_on"));
			}
			
			if (item_description.containsKey("text_off")) {
				tglbtn.setTextOff(item_description.get("text_off"));
			}
			
			tglbtn.setChecked(getdefault(item_description, "state", "1").equals("1") || ((String) getdefault(item_description, "state", "on")).equalsIgnoreCase("on"));
			
			item = getOptionView(context, tglbtn, item_description);
		} else if (type.equals("switch")) {
			Switch swtch = new Switch(context);
			
			if (item_description.containsKey("text_on")) {
				swtch.setTextOn(item_description.get("text_on"));
			}
			
			if (item_description.containsKey("text_off")) {
				swtch.setTextOff(item_description.get("text_off"));
			}
			
			swtch.setChecked(!(getdefault(item_description, "state", "1").equals("0") || ((String) getdefault(item_description, "state", "1")).equalsIgnoreCase("off")));
			
			item = getOptionView(context, swtch, item_description);
		} else if (type.equals("seek_bar")) {
			String tmpMin = (String) getdefault(item_description, "min");
			Integer min = null;
			if (tmpMin != null) {
				min = Integer.valueOf(tmpMin);
			}
			String tmpMax = (String) getdefault(item_description, "max");
			Integer max = null;
			if (tmpMax != null) {
				max = Integer.valueOf(tmpMax);
			}
			TextSeekBar skbar = new TextSeekBar(context,
												Integer.valueOf((String) getdefault(item_description, "default", "0")),
												min,
												max);
			item = getOptionView(context, skbar, item_description);
		} else if (type.equals("text_input") || type.equals("num_input") || type.equals("mail_input") || type.equals("time_input") || type.equals("date_input") || type.equals("web_input")) {
			final TextView txt = new TextView(context);
			txt.setPadding(20, 0, 20, 0);
			Dialog popup = null;
			if (type.equals("date_input")) {
				final Calendar time;
				if (item_description.containsKey("default")) {
					time = getTime(item_description.get("default"));
				} else {
					time = new GregorianCalendar();
					time.setTime(new Date());
				}
				final SimpleDateFormat format = new SimpleDateFormat((String) getdefault(item_description, "format", "dd.MM.yyyy"), Locale.US);
				final DatePickerDialog.OnDateSetListener datePickerListener = new DatePickerDialog.OnDateSetListener() {
					@Override
					public void onDateSet(DatePicker view, int year, int month, int day) {
						time.set(year, month, day);
						txt.setText(format.format(time.getTime()));
				    }
				};
				popup = new DatePickerDialog(uiActivity, datePickerListener, time.get(Calendar.YEAR) - 1, time.get(Calendar.MONTH) + 3, time.get(Calendar.DAY_OF_YEAR));
				txt.setText(format.format(time.getTime()));
			} else if (type.equals("time_input")) {
				final Calendar time;
				if (item_description.containsKey("default")) {
					time = getTime(item_description.get("default"));
				} else {
					time = new GregorianCalendar();
					time.setTime(new Date());
				}
				String dfltFormat = "HH:mm";
				if (!getdefault(item_description, "is24h", "true").toString().equalsIgnoreCase("true")) {
					dfltFormat = "hh:mm a";
				}
				final SimpleDateFormat format = new SimpleDateFormat((String) getdefault(item_description, "format", dfltFormat), Locale.US);
				final TimePickerDialog.OnTimeSetListener timePickerListener = new TimePickerDialog.OnTimeSetListener() {
					@Override
					public void onTimeSet(TimePicker view, int hour, int minute) {
				        time.set(Calendar.HOUR_OF_DAY, hour);
				        time.set(Calendar.MINUTE, minute);
						txt.setText(format.format(time.getTime()));
				    }
				};
				popup = new TimePickerDialog(uiActivity, timePickerListener, time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE), getdefault(item_description, "is24h", "true").toString().equalsIgnoreCase("true"));
				txt.setText(format.format(time.getTime()));
			} else {
				txt.setText((CharSequence) getdefault(item_description, "default", ""));
				AlertDialog.Builder builder = new AlertDialog.Builder(uiActivity);
				final EditText input = new EditText(context);
				if (item_description.containsKey("text_hint")) {
					input.setHint(item_description.get("text_hint"));
				}
				input.setText(txt.getText());
				int inputtype = InputType.TYPE_CLASS_TEXT;
				if (type.startsWith("num")) {
					inputtype = InputType.TYPE_CLASS_NUMBER;
					if (getdefault(item_description, "decimal", "False").toString().equalsIgnoreCase("True")) {// allow decimal numbers
						inputtype |= InputType.TYPE_NUMBER_FLAG_DECIMAL;
					}
					if (getdefault(item_description, "disallow_negative", "False").toString().equalsIgnoreCase("False")) {// allow also negatives
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
							inputtype |= InputType.TYPE_NUMBER_VARIATION_NORMAL;
						} else {
							Log.w(TAG, "Could not disallow negative numbers in num_input, only available for android version 3.0 (HONEYCOMB) or higher.");
						}
					}
					if (item_description.containsKey("min") || item_description.containsKey("max")) {
						Long max_bound = null;
						Long min_bound = null;
						try {
							if (item_description.containsKey("max")) {
								max_bound = Long.valueOf(item_description.get("max"));
							}
						} catch (NumberFormatException e) {
							Log.w(TAG, "Could not convert a max value for a num_input to a number, ignoring it!");
							e.printStackTrace();
							max_bound = null;
						}
						try {
							if (item_description.containsKey("min")) {
								min_bound = Long.valueOf(item_description.get("min"));
							}
						} catch (NumberFormatException e) {
							Log.w(TAG, "Could not convert a min value for a num_input to a number, ignoring it!");
							e.printStackTrace();
							min_bound = null;
						}
						if (max_bound != null || min_bound != null) {
							final Long tmp_max = max_bound;
							final Long tmp_min = min_bound;
							input.addTextChangedListener(new TextWatcher() {
								@Override
								public void onTextChanged(CharSequence s, int start, int before, int count) {}
								
								@Override
								public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
								
								@Override
								public void afterTextChanged(Editable s) {
									if (s.length() == 0) {
										input.setText("0");
										input.setSelection(0, 1);
									} else {
										try {
											Long input_num = Long.valueOf(s.toString());
											if (tmp_min != null && input_num < tmp_min) {
												input.setText(String.valueOf(tmp_min));
												input.setSelection(input.getText().length());
											} else if (tmp_max != null &&  input_num > tmp_max) {
												input.setText(String.valueOf(tmp_max));
												input.setSelection(input.getText().length());
											}
										} catch (NumberFormatException e) {
											if (s.charAt(0) == '-') {
												if (tmp_min != null) {
													input.setText(String.valueOf(tmp_min));
													input.setSelection(input.getText().length());
												}
											} else if (tmp_max != null){
												input.setText(String.valueOf(tmp_max));
												input.setSelection(input.getText().length());
											}
										}
									}
								}
							});
						}
					}
				} else if (type.startsWith("mail")) {
					inputtype = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
				} else if (type.startsWith("web")) {
					inputtype = InputType.TYPE_TEXT_VARIATION_URI;
				}
				if (getdefault(item_description, "password", "False").toString().equalsIgnoreCase("True")) {// password
					if (type.startsWith("num")) {
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
							inputtype |= InputType.TYPE_NUMBER_VARIATION_PASSWORD;
						} else {
							Log.w(TAG, "Could not set the num_input to be hidden like a password, for nummerical inputs they're only available for android version 3.0 (HONEYCOMB) or higher.");
						}
					} else {
						inputtype |= InputType.TYPE_TEXT_VARIATION_PASSWORD;
					}
				}
				if (getdefault(item_description, "multiline", "False").toString().equalsIgnoreCase("True")) {// allow multiline
					inputtype |= InputType.TYPE_TEXT_FLAG_MULTI_LINE;
				}
				input.setInputType(inputtype);
				txt.setInputType(inputtype | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
			    builder.setView(input);
			    // Add action buttons
			    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			               @Override
			               public void onClick(DialogInterface dialog, int id) {
			            	   txt.setText(input.getText().toString());
			               }
			           });
			    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			               public void onClick(DialogInterface dialog, int id) {
			            	   input.setText(txt.getText());
			               }
			           });
			    popup = builder.create();
			}
			if (popup == null) {
				Log.w(TAG, "Unable to create popup for (" + type + ") " + item_description.toString());
				return null;
			}
			if (item_description.containsKey("desc")) {
				popup.setTitle(item_description.get("desc"));
			}
			final Dialog tmp = popup;
			txt.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					tmp.show();
				}
			});
			item = getOptionView(context, txt, item_description);
		} else if (type.equals("list_option")) {
			final TextView txt = new TextView(context);
			Dialog popup = null;
			AlertDialog.Builder builder = new AlertDialog.Builder(uiActivity);
			if (!item_description.containsKey("options")) {
				Log.w(TAG, "Got list option with no options!");
				return null;
			}
			final String[] options =  item_description.get("options").substring(2, item_description.get("options").length() - 2).split("', '");
			txt.setText((CharSequence) getdefault(item_description, "default", options[0]));
			if (getdefault(item_description, "multioption", "False").toString().equalsIgnoreCase("True") || getdefault(item_description, "multioption", "0").equals("1")) {
				final ArrayList<Boolean> opt_state = new ArrayList<Boolean>(Collections.nCopies(options.length, false));
				final String dflt = (String) txt.getText();
				final ArrayList<Boolean> last_state = new ArrayList<Boolean>(opt_state);
				builder.setMultiChoiceItems(options, null, new DialogInterface.OnMultiChoiceClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int index, boolean isChecked) {
						if (index <= opt_state.size()) {
							opt_state.set(index, isChecked);
						}
					}
				});
				builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						for (int i = 0; i < opt_state.size(); i++) {
							((AlertDialog) dialog).getListView().setItemChecked(i, last_state.get(i));
							opt_state.set(i, last_state.get(i));
						}
					}
				});
				builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						String text = "";
						for (int i = 0; i < options.length; i++) {
							if (opt_state.get(i)) {
								text += ", " + options[i];
							}
							last_state.set(i, opt_state.get(i));
						}
						if (text.length() <= 2) {
							text = ", " + dflt;
						}
						txt.setText(text.substring(2));
					}
				});
				 popup = builder.create();
			} else {
				builder.setItems(options, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int index) {
					txt.setText(options[index]);
					}
				});
				popup = builder.create();
			}
			if (popup == null) {
				Log.w(TAG, "Unable to create popup for (" + type + ") " + item_description.toString());
				return null;
			}
			if (item_description.containsKey("desc")) {
				popup.setTitle(item_description.get("desc"));
			}
			final Dialog tmp = popup;
			txt.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					tmp.show();
				}
			});
			item = getOptionView(context, txt, item_description);
		} else if (type.equals("color_picker")) {
			Integer color = getColor((String) getdefault(item_description, "default", "[0, 0, 0, 255]"));
			final ColorView clr_btn = new ColorView(context, color) {
				@Override
				public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
					super.onMeasure(heightMeasureSpec, heightMeasureSpec);
				}
			};
			AlertDialog.Builder builder = new AlertDialog.Builder(uiActivity);
			AlertDialog.Builder colorEditPopupBuilder = new AlertDialog.Builder(uiActivity);
			final RelativeLayout container = new RelativeLayout(context);
			final ColorView hexColorView = new ColorView(context, color);
			final ColorView oldColorView = new ColorView(context, color);
			final EditText colorEditor = new EditText(context);
			Sepperator sep1 = new Sepperator(context);
			Sepperator sep2 = new Sepperator(context);
			final ColorPicker clrPicker = new ColorPicker(context, color, getdefault(item_description, "transparent", "false").toString().equalsIgnoreCase("true"), new ColorPicker.OnColorChangeListener() {
				@Override
				public void onColorChange(ColorPicker instance, float[] hsv, Integer alpha) {
					String color_str = Integer.toHexString(instance.getColor());
					hexColorView.setText(String_repeat(8 - color_str.length(), "0") + color_str);
					if ((hsv[2] <= 0.5f || (hsv[1] > 0.5f && !(hsv[0] < 210 && hsv[0] > 30))) && alpha > 128) {
						hexColorView.setTextColor(Color.LTGRAY);
					} else {
						hexColorView.setTextColor(Color.DKGRAY);
					}
					hexColorView.setColor(instance.getColor());
				}
			});
			RelativeLayout.LayoutParams colorPickerLayout = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			RelativeLayout.LayoutParams hexColorLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			RelativeLayout.LayoutParams oldColorLayout = new RelativeLayout.LayoutParams(40 + 20, LayoutParams.WRAP_CONTENT);
			RelativeLayout.LayoutParams sepLayout1 = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, 2);
			RelativeLayout.LayoutParams sepLayout2 = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, 2);
			
			hexColorView.setId(1);
			oldColorView.setId(2);
			clrPicker.setId(3);
			sep2.setId(4);
			sep1.setId(5);
			
			sepLayout1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			sepLayout1.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			sepLayout1.addRule(RelativeLayout.ABOVE, hexColorView.getId());
			sepLayout2.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			sepLayout2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			sepLayout2.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			colorPickerLayout.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			colorPickerLayout.addRule(RelativeLayout.ABOVE, sep1.getId());
			hexColorLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			hexColorLayout.addRule(RelativeLayout.RIGHT_OF, oldColorView.getId());
			hexColorLayout.addRule(RelativeLayout.ABOVE, sep2.getId());
			oldColorLayout.addRule(RelativeLayout.ALIGN_TOP, hexColorView.getId());
			oldColorLayout.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			
			colorEditPopupBuilder.setView(colorEditor);
			final Dialog colorEditpopup = colorEditPopupBuilder.create();
			colorEditor.setOnEditorActionListener(new EditText.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					Log.i(TAG, "I am called!");
					try { 
						clrPicker.setColor(Color.parseColor(v.getText().toString()));
						// userInput is a valid color
					} catch (IllegalArgumentException e1) {
						try {
							Log.i(TAG, "#" + String_repeat(8 - v.getText().length(), "0") + v.getText().toString());
							clrPicker.setColor(Color.parseColor("#" + String_repeat(8 - v.getText().length(), "0") + v.getText().toString()));
							// userInput is a valid color
						} catch (IllegalArgumentException e2) {
							// userInput is not a valid color
							e2.printStackTrace();
							Log.i(TAG, "Voiding given color " + v.getText() + " from userinput, because it could not be converted to a color.");
						}
					}
					colorEditpopup.dismiss();
					return true;
				}
			});
			colorEditor.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			hexColorView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					colorEditor.setText(((TextView) v).getText());
					colorEditpopup.show();
				}
			});
			hexColorView.setGravity(Gravity.CENTER);
			hexColorView.setText(Integer.toHexString(color));
			hexColorView.setColor(color);
			hexColorView.setPadding(5, 0, 20, 0);
			oldColorView.setColor(color);
			oldColorView.setPadding(20, 0, 0, 0);
			sep1.setPadding(20, 0, 20, 0);
			sep2.setPadding(20, 0, 20, 0);
			clr_btn.setColor(color);
			clr_btn.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
			
			container.addView(clrPicker,    colorPickerLayout);
			container.addView(hexColorView, hexColorLayout);
			container.addView(oldColorView, oldColorLayout);
			container.addView(sep1, sepLayout1);
			container.addView(sep2, sepLayout2);
			
			builder.setView(container);
			builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {}
			});
			builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					clr_btn.setColor(clrPicker.getColor());
				}
			});
			Dialog popup = builder.create();
			
			if (popup == null) {
				Log.w(TAG, "Unable to create popup for (" + type + ") " + item_description.toString());
				return null;
			}
			if (item_description.containsKey("desc")) {
				popup.setTitle(item_description.get("desc"));
			}
			final Dialog tmp = popup;
			clr_btn.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					clrPicker.setColor(clr_btn.getColor());
					oldColorView.setColor(clr_btn.getColor());
					tmp.show();
				}
			});
			item = getOptionView(context, clr_btn, item_description);
		} else {
			Log.w(TAG, "Got unknown option type: " + String.valueOf(type));
			return null;
		}
		return item;
	}
	
	static protected ViewGroup getOptionView(Context context, View second_view, Map<String, String> option_data) {
    	LinearLayout container 			   = new LinearLayout(context);
    	LinearLayout textcontainer 		   = new LinearLayout(context);
    	MaxWidthLinearLayout viewcontainer = new MaxWidthLinearLayout(context);
    	second_view.setTag(CONFIG_ITEM);
		if (option_data.containsKey("desc")) {
			textcontainer.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
			
			TextView title = new TextView(context);
			title.setText(option_data.get("desc"));
			title.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			
			if (option_data.containsKey("desc_text_color")) {
				Integer color = getColor(option_data.get("desc_text_color"));
				if (color != null) {
					title.setTextColor(color);
				} else {
					Log.w(TAG, "Could not set the desc textcolor because '" + option_data.get("desc_text_color") + "' is not a color!");
				}
			}
			if (option_data.containsKey("desc_text_size")) {
				try {
					title.setTextSize(Integer.valueOf(option_data.get("desc_text_size")));
				} catch (NumberFormatException e) {
					Log.w(TAG, "Could not set the desc textsize because '" + option_data.get("desc_text_size") + "' is not a number!");
					e.printStackTrace();
					title.setTextSize(title.getTextSize() * 0.5f);
				}
			}
			textcontainer.setOrientation(LinearLayout.VERTICAL);
			textcontainer.setPadding(20, 0, 20, 0);
			viewcontainer.setMaxWidthPercentage(0.75f);
			viewcontainer.setPadding(0, 0, 20, 0);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	        params.weight  = 0f;
	        params.gravity = Gravity.CENTER_VERTICAL;
			viewcontainer.setLayoutParams(params);
			viewcontainer.setMinimumWidth(60);
			textcontainer.addView(title);
			viewcontainer.addView(second_view);
			
			if (option_data.containsKey("hint")) {
				TextView hint = new TextView(context);
				hint.setText(option_data.get("hint"));
				hint.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				if (option_data.containsKey("hint_text_color")) {
					Integer color = getColor(option_data.get("hint_text_color"));
					if (color != null) {
						hint.setTextColor(color);
					} else {
						Log.w(TAG, "Could not set the hint textcolor because '" + option_data.get("hint_text_color") + "' is not a color!");
					}
				}
				if (option_data.containsKey("hint_text_size")) {
					if (!option_data.get("hint_text_size").equals("default")) {
						try {
							hint.setTextSize(Integer.valueOf(option_data.get("hint_text_size")));
						} catch (NumberFormatException e) {
							Log.w(TAG, "Could not set the hint textsize because '" + option_data.get("hint_text_size") + "' is not a number!");
							e.printStackTrace();
							hint.setTextSize(hint.getTextSize() * 0.5f);
						}
					}
				} else {
					hint.setTextSize(hint.getTextSize() * 0.5f);
				}
				textcontainer.addView(hint);
			}
			
			container.addView(textcontainer);
			container.addView(viewcontainer);
			
			return container;
		} else {
			container.addView(second_view);
			return container;
		}
	}
}