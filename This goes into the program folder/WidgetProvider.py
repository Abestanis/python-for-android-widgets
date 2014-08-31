# Licensed under the MIT license
# http://opensource.org/licenses/mit-license.php

# Copyright 2014 Sebastian Scholz <abestanis.gc@gmail.com> 


# This file is an example for the WidgetProvider.py, which has to be in the programs directory.
# We create 3 Widgets: HelloWorldWidget, HelloWorldWidgetLarge and MyImageWidget

from AndroidWidgets import *

class HelloWorldWidget(Widget):
    '''This class defines two Widgets which will display
    'Hello World" and launch the main App, if the user
    clicks on them.'''
    
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
    init_action      = True
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
    
    def initWidget(self, *args, **kwargs):
        print('Python: Init Widget...')
        if 'widget' not in kwargs:
            print('[ERROR] No widget passed in and thus can not get initialized!')
            return False # Widget will show a 'failed' message and wont get updated
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
    a list of images, whose paths are given by the main app. If
    the user clicks on the image, the next one is shown.'''
    
    widget_name = 'MyImageWidget'
    
    images = None
    index  = -1
    
    def __init__(self, *args, **kwargs):
        '''We get the image paths provided by our
        main app and store it in self.images'''
        self.update_images()
    
    def initWidget(self, *args, **kwargs):
        '''Show the first image.'''
        print('Python: Init Widget...')
        if 'widget' not in kwargs:
            print('[ERROR] No widget passed in and thus can not get initialized!')
            return False # Widget will show a 'failed' message and wont get updated
        widget = kwargs.get('widget')
        self.next_image(widget)
        return True
    
    def my_callback(self, widget):
        '''A custom callback for the on_click event for my
        Widget. It could be named anything or could even
        be a function from an other module.'''
        print('Python (my_callback): Got Input!')
        self.next_image(widget)
    
    def next_image(self, widget):
        '''Show the next image.'''
        widget.canvas.clear()
        if len(self.images) == 0:
            # If we have no images, try to update our list
            self.update_images()
        if len(self.images) == 0:
            # If we didn't get any images
            print('No images')
            widget.canvas.add(widget.canvas.TextView(text = 'No images found!', on_click = self.my_callback))
        else:
            print('Next image')
            if self.index + 1 == len(self.images):
                self.index = 0
            else:
                self.index += 1
            widget.canvas.add(widget.canvas.ImageView(image_path = self.images[self.index], on_click = self.my_callback))
        widget.update()
    
    def update_images(self):
        '''We try to get the image paths provided
        by our main app.'''
        print('[Info] Getting stored Widget data')
        data = getWidgetData()
        # This returns the dict 'data' if the main app has called storeWidgetData(data),
        # otherwise it returns None
        #
        # Warning: Every value in the data-dict was converted to a string
        #
        # In this example we want to get image paths which were set by the main app,
        # so we expect sth. like this:
        #
        # data = {
        #    'image_paths': '["path/to/image1", "path/to/image2"]'
        #}
        
        if type(data) == dict:
            print('[Debug] Returned data: ' + str(data))
            paths = data.get('image_paths', None)
            if paths == None:
                print('[Debug] Given data has no entry "image_paths"!')
                self.images = []
            else:
                paths = paths[1:-1]
                paths.replace('"', '')
                self.images = paths.split(', ')
        else:
            print('[Debug] Got no data (' + str(data) + ').')
            self.images = []