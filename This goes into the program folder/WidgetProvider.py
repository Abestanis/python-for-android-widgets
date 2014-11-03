# Licensed under the MIT license
# http://opensource.org/licenses/mit-license.php

# Copyright 2014 Sebastian Scholz <abestanis.gc@gmail.com> 


# This file is an example for the WidgetProvider.py, which has to be in the programs directory.
# We create 3 Widgets: HelloWorldWidget, HelloWorldWidgetLarge and MyImageWidget

from AndroidWidgets import *
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
    
    
    
    def __init__(self, *args, **kwargs):
        print('Python: Init class...')
        print('Hello World!')
        setDefaultError(Canvas().TextView(text = 'An error occoured!'))
        print('Default Error:')
        print(getDefaultError())
    
    def initWidget(self, *args, **kwargs):
        print('Python: Init Widget...')
        if 'widget' not in kwargs:
            print('[ERROR] No widget passed in and thus can not get initialized!')
            return False # Widget will not be created
        widget = kwargs.get('widget')
        # This Widget is an instance of the ExternalWidget class and has
        # a widget id (widget.widget_Id),
        # a canvas, which is empty at creation (widget.canvas) and
        # an update function, which pushes all changes made to the widgets canvas to the screen.
        print('My widget ID: ', widget.widget_Id)
        
        print('[Debug] Clearing canvas...')
        widget.canvas.clear()
        print('[Debug] Done. Creating LinearLayout...')
        layout = widget.canvas.LinearLayout()
        print('[Debug] Done. Adding TextView...')
        # Every view may have an on_click callback, which must be callable
        # or a special action defined in the Widget class like
        # 'Action_StartApp', which starts the main app.
        layout.add(layout.TextView(text = 'Hello World', on_click = self.Action_StartApp))
        print('[Debug] Done. Adding Layout to canvas...')
        widget.canvas.add(layout)
        print('[Debug] Done. Updating Widget...')
        # We push our changes to the screen
        widget.update()
        print('[Debug] Done.')
        return True
    
    def updateWidget(self, *args, **kwargs):
        print('Python: Updating Widget...')
        print('Hello World again!')
    
    def destroyWidget(self, *args, **kwargs):
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
    'General settings' section are only available the first
    time a widget of this type is created.
    '''
    
    widget_name = 'MyImageWidget'
    init_action = {
        'title': 'Configuration',
        'save_key': 'ImageWidgetConfig',
        'children': [
            {'type': 'text',         'text': 'General settings', 'text_color': [100,100,100], 'text_size': 20},
            {'type': 'separator'},
            {'type': 'switch',       'state': 1, 'text_on': 'Enabled', 'text_off': 'Disabled', 'desc': 'Auto update', 'hint': 'Check for new images every time the image gets changed.'},
            {'type': 'num_input',    'default': 15, 'text_hint': 'On screen time for the images', 'disallow_negative': True, 'desc': 'Cycle time', 'hint': 'Set how often the widget should cycle trough the images.\nWarning: Setting this to a small number could overload the CPU!', 'hint_text_color': [255,140,0]},
            {'type': 'text',         'text': 'Image displaying', 'text_color': [100,100,100], 'text_size': 20},
            {'type': 'separator'},
            {'type': 'list_option',  'options': ['As set by profile', 'Creationtime', 'Artist', 'Random'], 'desc': 'Sort by', 'hint': 'Sets the order the images should be displayed'},
            {'type': 'text',         'text': 'No images', 'text_color': [100,100,100], 'text_size': 20},
            {'type': 'separator'},
            {'type': 'text_input',   'default': 'No images found', 'text_hint': 'e.g. "Got no images"', 'desc': 'No images warning', 'hint': 'The text that should get displayed if no images are given from the app.'},
            {'type': 'color_picker', 'default': [255,200,50,255], 'transparent': True, 'desc': 'Warning color', 'hint': 'Set a text color for the warning.'},
        ]
    }
    
    images      = None
    indexes     = None
    auto_update = True
    update_time = 15
    warnings    = None
    sorting     = None
    
    
    def __init__(self, *args, **kwargs):
        '''We get the image paths provided by our
        main app and store it in self.images.
        '''
        # setup storage
        self.images   = []
        self.indexes  = {}
        self.warnings = {}
        self.sorting  = {}
        
        self.update_images()
    
    def initWidget(self, *args, **kwargs):
        '''Show the first image.'''
        print('Python: Init Widget...')
        if 'widget' not in kwargs:
            print('[ERROR] No widget passed in and thus can not get initialized!')
            return False # Widget will not be created
        widget = kwargs.get('widget')
        self.indexes[widget.widget_Id] = -1
        
        # Get the confiruration results
        config = getWidgetData('ImageWidgetConfig').split(',')
        
        if len(self.init_action['children']) == 11: # If w haven't done this yet:
            # Remove the section 'General settings', since its values are global for all MyImageWidgets.
            self.init_action['children'] = self.init_action['children'][4:]
            # Now we must notify the java side, that our desired init action has changed.
            setInitAction(self.init_action)
            # We extract the global values...
            self.auto_update = unquote_plus(config[0]) in ['True', 'true']
            self.update_time = unquote_plus(config[1])
            # ... and remove them from the list, so that they are out of the way.
            config = config[2:]
        
        # We extract our configuration results and store them (sorting is not implemented in this example!).
        self.sorting[widget.widget_Id]  = ['As set by profile', 'Creationtime', 'Artist', 'Random'].index(unquote_plus(config[0]))
        self.warnings[widget.widget_Id] = widget.canvas.TextView(text = (unquote_plus(config[1]) if config[1] != '' else 'No images found'), text_color = unquote_plus(config[2]), on_click = self.my_callback)
        
        # Finally, we try to displaye the first image
        self.next_image(widget)
        return True
    
    def my_callback(self, widget):
        '''A custom callback for the on_click event for my
        Widget. It could be named anything or could even
        be a function from an other module.
        '''
        print('Python (my_callback): Got Input!')
        # Display the next image
        self.next_image(widget)
    
    def next_image(self, widget):
        '''Show the next image.'''
        widget.canvas.clear()
        if len(self.images) == 0 or self.auto_update:
            # If we have no images or if set by the use, try to update our list
            self.update_images()
        if len(self.images) == 0:
            # If we didn't get any images from the update...
            print('No images')
            # ... set our warning, that we dont have any images to display.
            widget.canvas.add(self.warnings[widget.widget_Id])
        else:
            print('Next image')
            # Figure out the next picture we should display from our list.
            if self.indexes[widget.widget_Id] + 1 >= len(self.images):
                self.indexes[widget.widget_Id] = 0
            else:
                self.indexes[widget.widget_Id] += 1
            # Add an ImageWidget to the canvas, displaying our image with an on_click callback to our callback-function.
            widget.canvas.add(widget.canvas.ImageView(image_path = self.images[self.indexes[widget.widget_Id]], on_click = self.my_callback))
        # Push the changes to the screen.
        widget.update()
    
    def update_images(self):
        '''We try to get the image paths provided
        by our main app.
        '''
        print('[Info ] Getting stored image paths...')
        # We try to get the data (if thers any) stored in the key 'image_paths'.
        paths = getWidgetData('image_paths')
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
        index = 0
        while paths.has_key(str(index)):
            # Adding every image path to our cache
            self.images.append(paths[str(index)])
            index += 1
        # You would do the sorting here based on which sorting type the user has configured.
        print("Got " + str(index) + " image paths.")
