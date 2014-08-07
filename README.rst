Python for Android
==================

Python for android is a project to create your own Python distribution
including the modules you want, and create an apk including python, libs, and
your application.

- Website: http://python-for-android.rtfd.org/
- Forum: https://groups.google.com/forum/?hl=fr#!forum/python-android
- Mailing list: python-android@googlegroups.com


Global overview
---------------

#. Download Android NDK, SDK
 
 * NDK: http://dl.google.com/android/ndk/android-ndk-r8c-linux-x86.tar.bz2
 
 * More details at: http://developer.android.com/tools/sdk/ndk/index.html
 
 * SDK: http://dl.google.com/android/android-sdk_r21.0.1-linux.tgz
 
 * More details at:http://developer.android.com/sdk/index.html

#. Launch "android", and download latest Android platform, here API 14, which would be Android 4.0

#. Export some environment variables::

    export ANDROIDSDK="/path/to/android/android-sdk-linux_86"
    export ANDROIDNDK="/path/to/android/android-ndk-r8c"
    export ANDROIDNDKVER=r8c
    export ANDROIDAPI=14

 (Of course correct the paths mentioned in ANDROIDSDK and ANDROIDNDK)

#. Clone python-for-android::

    git clone git://github.com/kivy/python-for-android

#. Build a distribution with OpenSSL module, PIL and Kivy::

    cd python-for-android
    ./distribute.sh -m "openssl pil kivy"

#. Go to your fresh distribution, build the APK of your application::

    cd dist/default
    ./build.py --package org.test.touchtracer --name touchtracer \
    --version 1.0 --dir ~/code/kivy/examples/demo/touchtracer debug

#. Install the debug apk to your device::

    adb install bin/touchtracer-1.0-debug.apk

#. Enjoy.


Troubleshooting
---------------

if you get the following message:

    Android NDK: Host 'awk' tool is outdated. Please define HOST_AWK to point to Gawk or Nawk !

a solution is to remove the "awk" binary in the android ndk distribution

    rm $ANDROIDNDK/prebuilt/linux-x86/bin/awk


Widgets
-------

For now, your application will provide one widget type, whether you want more or none at all.
This will be changeable in the future.

If you want to interact with your widgets, you need a 'WidgetReceiver.py' file in your program directory which should be structured as follows:

.. code-block:: python

  class WidgetReceiver(object):
      '''This class is the central interface between the python code
      and the widgets. All events occurring for the widget will get
      passed down to the corresponding functions in this class.'''
      
      def __init__(self):
          '''This function is called whenever a widget gets initialized,
          no other widgets are present and before 'initWidget' is called.
          You can use this function to get some data required for all your
          widgets or to set the default loading and error view.'''
          pass
      
      def initWidget(self, widget):
          '''This function will initialize the given widget 'widget'
          and will set it's visual appearance. If this function
          returns False, the initialisation is considered as failed
          and an error view is displayed.'''
          return True
      
      def updateWidget(self, widget:
          '''This function gets called every time the android AlarmManager
          schedules an update.'''
          pass
      
      def destroyWidget(self, widget):
          '''This function is called if the user deletes a widget from
          the home screen.'''
          pass

An example is provided `here`_.


Every widget has an id and a canvas. The canvas is used to define the widgets look.
Just add a CanvasObject (aka. view) to the canvas and push the change to the screen:

.. code-block:: python

  widget.canvas.add(view)
  widget.update() # Don't forget this!

Due to android `limitations`_, only a few view types are allowed on the canvas:

- Layouts
 - LinearLayout
 - FrameLayout
 - RelativeLayout
 - GridLayout
- Views
 - TextView
 - AnalogClock
 - Button
 - Chronometer
 - ImageButton
 - ImageView
 - ProgressBar
 - ViewFlipper
 - ListView
 - GridView
 - StackView
 - AdapterViewFlipper
 - ViewStub

You can get a new CanvasObject from every other CanvasObject or a canvas itself:

.. code-block:: python

  textview1 = widget.canvas.TextView(text = 'Hello world!')
  textview2 = textview1.TextView(text = 'How are you?')


For more information about the canvas system look at `AndroidWidgets.py`_.


.. _here: https://github.com/Abestanis/python-for-android-widgets/blob/master/This%20goes%20into%20the%20program%20folder/WidgetReceiver.py
.. _limitations: http://developer.android.com/guide/topics/appwidgets/index.html#CreatingLayout
.. _AndroidWidgets.py: https://github.com/Abestanis/python-for-android-widgets/blob/master/This%20goes%20into%20the%20program%20folder/AndroidWidgets.py
