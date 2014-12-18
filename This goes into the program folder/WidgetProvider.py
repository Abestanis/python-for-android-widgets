# Licensed under the MIT license
# http://opensource.org/licenses/mit-license.php

# Copyright 2014 Sebastian Scholz <abestanis.gc@gmail.com> 


# This file is an example for the WidgetProvider.py, which has to be in the programs directory.
# We create 3 Widgets: HelloWorldWidget, HelloWorldWidgetLarge and MyImageWidget

from AndroidWidgets import canvas_to_xml, estimateSize, Canvas, Widget
from urlparse       import parse_qsl
from urllib         import unquote_plus

class HelloWorldWidget(Widget):
    '''This class defines two Widgets which will display
    'Hello World" and launch the main App, if the user
    clicks on them. This widget has no configuration.
    '''
    
    widget_dflt_size = estimateSize(2,2)
    widget_min_size  = estimateSize(1,1)
    preview_image    = 'images\\HelloWorldPreview.jpg'
    initial_layout   = canvas_to_xml('LinearLayout()[TextView(text=Loading...)]')
    # The above string was created by converting the desired CanvasObject-Structure
    # to a string via str().
    # 
    # example:
    # c = Canvas()
    # layout = c.LinearLayout()
    # layout.add(c.TextView(text = 'Loading...'))
    # str(layout)
    resize_mode      = None
    widget_category  = Widget.Both
    sub_widgets      = [
        {
            'widget_name': 'HelloWorldWidgetLarge',
            'widget_dflt_size': estimateSize(4,4),
        }
    ]
    # In sub_widgets we define just one other Widget
    # which will be 4x4 when the user places it on
    # the home screen, instead of 2x2.
    
    num_interval_updates = 0
    num_singletime_updates = 0
    
    def __init__(self, widget_Id):
        '''We set the view of the widget
        to a Textview with the text
        'Hello World' with an on click
        callback which starts our main app.
        '''
        print('Python: Init Widget...')
        super(HelloWorldWidget, self).__init__(widget_Id)
        print('Hello World!')
        print('My widget ID: ', widget_Id)
        
        print('[Debug] Clearing canvas...')
        self.canvas.clear()
        print('[Debug] Done. Creating LinearLayout...')
        layout = self.canvas.LinearLayout()
        print('[Debug] Done. Adding first TextView...')
        # Every view may have an on_click callback, which must be callable
        # or a special action defined in the Widget class like
        # 'Action_StartApp', which starts the main app.
        layout.add(layout.TextView(text = 'Hello World', on_click = self.Action_StartApp))
        print('[Debug] Done. Adding second TextView...')
        layout.add(layout.TextView(text = 'Single-time updates: 0\nInterval updates: 0'))
        print('[Debug] Done. Adding Layout to canvas...')
        self.canvas.add(layout)
        print('[Debug] Done. Updating Widget...')
        # We push our changes to the screen
        self.update_graphics()
        print('[Debug] Done.')
        # We request an update in 5 seconds
        self.schedule_once(5000)
        # We also want an update every 12 seconds (starting in 12 seconds from now on)
        self.schedule_interval(12000)
    
    def updateWidget(self, updateType):
        print('Python: Updating Widget...')
        print('Hello World again!')
        print('Update type is ' + ('OneTime_UpdateType' if updateType == self.OneTime_UpdateType else 'Interval_UpdateType' if updateType == self.Interval_UpdateType else 'Hard_UpdateType'))
        if updateType == self.OneTime_UpdateType:
            self.num_singletime_updates += 1
            if self.num_singletime_updates <= 3:
                self.schedule_once(5000)
                # This means we schedule a onetime update 4 times, including the one time in __init__
        elif updateType == self.Interval_UpdateType:
            self.num_interval_updates += 1
            if self.num_interval_updates == 5:
                self.schedule_interval(6000)
                # If we updated 5 times, change the frequency to 6 seconds
            elif self.num_interval_updates == 10:
                self.schedule_interval(-1)
                # If we updated 10 times, stop it
        layout = self.canvas.children[0] # we get the LinearLayout containing our both TextViews
        layout.remove(1) # We remove the second TextView (The on containing our update timer counts) and readd it wirh the new values
        layout.add(layout.TextView(text = 'Single-time updates: ' + str(self.num_singletime_updates) + '\nInterval updates: ' + str(self.num_interval_updates)))
        self.update_graphics()
        print('Update done!')
    
    def destroyWidget(self):
        print('Python: Destroying Widget...')
        print('Goodby World, will miss you...')
    

class ARandomWidgetClass(Widget):
    '''Another Widget which demonstrates that you don't have to
    name your Widgets like the class, you can also use the
    widget_name variable. This widgets will display an image from
    a list of images, whose paths are given by the main app. If the
    user clicks on the image, the next one is shown. This widget
    will show a configuration, before the widget will actually
    get placed on the homescreen. The settings in the
    'General' section are only available the first
    time a widget of this type is created.
    '''
    
    widget_name = 'MyImageWidget'
    init_action = {
        'title': 'Configuration',
        'save_key': 'ImageWidgetConfig',
        'children': [
            {'type': 'text',         'text': 'General', 'text_color': [100,100,100], 'text_size': 20},
            {'type': 'separator'},
            {'type': 'text',         'text': 'Some information that are only necessary, if there are no other widgets of this type placed on the home screen.'},
            {'type': 'text',         'text': 'Image', 'text_color': [100,100,100], 'text_size': 20},
            {'type': 'separator'},
            {'type': 'switch',       'state': 1, 'desc': 'Auto update', 'hint': 'Check for new images every time the image gets changed.'},
            {'type': 'num_input',    'default': 15, 'text_hint': 'On screen time for the images', 'disallow_negative': True, 'desc': 'Cycle time', 'hint': 'Set how often the widget should cycle trough the images.\nWarning: Setting this to a small number could overload the CPU!', 'hint_text_color': [255,140,0]},
            {'type': 'list_option',  'options': ['As set by profile', 'Creationtime', 'Artist', 'Random'], 'desc': 'Sort by', 'hint': 'Sets the order the images should be displayed'},
            {'type': 'text',         'text': 'Warning', 'text_color': [100,100,100], 'text_size': 20},
            {'type': 'separator'},
            {'type': 'text_input',   'default': 'No images found', 'text_hint': 'e.g. "Got no images"', 'desc': 'No images warning', 'hint': 'The text that should get displayed if no images are given from the app.'},
            {'type': 'color_picker', 'default': [255,200,50,255], 'transparent': True, 'desc': 'Warning color', 'hint': 'Set a text color for the warning.'},
        ]
    }
    
    images      = None
    index       = None
    auto_update = True
    warning     = None
    sorting     = None
    
    
    def __init__(self, widget_Id):
        '''We get the image paths provided by our
        main app, store it in self.images and show
        the first image.
        '''
        print('Python: Init Widget...')
        super(ARandomWidgetClass, self).__init__(widget_Id)
        
        # Setup image storage and index
        self.images   = []
        self.index    = -1
        
        # Get our confiruration results
        config = self.getWidgetData('ImageWidgetConfig').split(',')
        
        # Remove the section 'General', since the information shown there is no longer needed.
        self.init_action['children'] = self.init_action['children'][3:]
        # Now we must notify the java side, that our desired init action has changed.
        self.setInitAction(self.init_action)
        
        # We extract our configuration results and store them (sorting is not implemented in this example!).
        self.auto_update = unquote_plus(config[0]) in ['True', 'true']
        update_time      = int(unquote_plus(config[1]))
        self.sorting     = ['As set by profile', 'Creationtime', 'Artist', 'Random'].index(unquote_plus(config[2]))
        self.warning     = self.canvas.TextView(text = (unquote_plus(config[3]) if config[3] != '' else 'No images found'), text_color = unquote_plus(config[4]), on_click = self.my_callback)
        
        # To change the images on the screen we initialize an interval updater to update us every update_time seconds
        self.schedule_interval(update_time * 1000)
        
        # Finally, we try to displaye the first image
        self.next_image()
    
    def updateWidget(self, updateType):
        '''This is called when its time to show the next
        image.
        '''
        self.next_image()
    
    def my_callback(self):
        '''A custom callback for the on_click event for my
        Widget. It could be named anything or could even
        be a function from an other module.
        '''
        print('Python (my_callback): Got Input!')
        # Display the next image
        self.next_image()
    
    def next_image(self):
        '''Show the next image.'''
        self.canvas.clear()
        if len(self.images) == 0 or self.auto_update:
            # If we have no images or if set by the use, try to update our list
            self.update_images()
        if len(self.images) == 0:
            # If we didn't get any images from the update...
            print('No images')
            # ... set our warning, that we dont have any images to display.
            self.canvas.add(self.warnings)
        else:
            print('Next image')
            # Figure out the next picture we should display from our list.
            if self.index + 1 >= len(self.images):
                self.index = 0
            else:
                self.index += 1
            # Add an ImageWidget to the canvas, displaying our image with an on_click callback to our callback-function.
            self.canvas.add(self.canvas.ImageView(image_path = self.images[self.index], on_click = self.my_callback))
        # Push the changes to the screen.
        self.update_graphics()
    
    def update_images(self):
        '''We try to get the image paths provided
        by our main app.
        '''
        print('[Info ] Getting stored image paths...')
        # We try to get the data (if thers any) stored in the key 'image_paths'.
        paths = self.getWidgetData('image_paths')
        print('[Debug] Returned data: ' + str(paths))
        if type(paths) != str:
            # Well, our main app was not kind enough to deposit any data, so we have nothing to show.
            print('[Info ] No image paths were stored.')
            self.images = []
            return
        # We parse the urlencoded dict back to a normal dict
        paths = dict(parse_qsl(paths))
        
        # This should get us the dict 'paths' if the main app has called storeWidgetData().
        #
        # In this example we want to get image paths which were set by the main app,
        # so we expect sth. like this:
        #
        # paths = {
        #    '0': 'path/to/image1',
        #    '1': 'path/to/image2',
        #    ...
        # }
        
        self.images = []
        i = 0
        while paths.has_key(str(i)):
            # Adding every image path to our cache
            self.images.append(paths[str(i)])
            i += 1
        # You would do the sorting here based on which sorting type the user has configured.
        print("[Info ] Got " + str(i) + " image paths.")
