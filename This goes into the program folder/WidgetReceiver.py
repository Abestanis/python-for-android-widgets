# This file is an example for the WidgetReceiver.py, which has to be in the programs directory

import AndroidWidgets

class WidgetReceiver(object):
    '''This class is the central interface between the python code
    and the widgets. All events occurring for the widget will get
    passed down to the corresponding functions in this class.'''
    
    def __init__(self, *args, **kwargs):
        '''This function is called whenever a widget gets initialized and
        no other widgets are present and before 'initWidget' is called.
        You can use this function to get some data required for all your
        widgets or to set the default loading and error view.'''
        print('Python: Init class...')
        # Getting the current default loading view:
        # If res == None, the hardcoded default view
        # is currently set, which is default.
        res = AndroidWidgets.getDefaultLoad()
        if not res:
            print('Setting my own default loading view!')
            # Getting a blank canvas,
            canvas = AndroidWidgets.Canvas()
            # populating it with a TextView
            canvas.add(canvas.TextView(text = 'This Widget is currently under construction...'))
            # and setting it to the default loading view
            AndroidWidgets.setDefaultLoad(canvas)
            # The same could be done for the default error view
    
    def initWidget(self, *args, **kwargs):
        '''This function will initialize the given widget 'widget'
        and will set it's visual appearance. If this function
        returns False, the initialisation is considered as failed
        and an error view is displayed.'''
        # ... Because nobody likes invisible widgets on the home screen.
        print('Python: Init Widget...')
        if 'widget' not in kwargs:
            print('[ERROR] No widget passed in and thus can not get initialized!')
            return False # Widget will show a 'failed' message and wont get updated
        widget = kwargs.get('widget')
        # This Widget is a instance of the ExternalWidget class and has
        # a widget id (widget.widget_Id),
        # a canvas, which is empty at creation (widget.canvas) and
        # a update function, which pushes all changes made to the widgets canvas to the screen.
        print('My widget ID: ', widget.widget_Id)
        
        print('[Debug] Clearing canvas...')
        widget.canvas.clear()
        print('[Debug] Done. Creating LinearLayout...')
        layout = widget.canvas.LinearLayout()
        print('[Debug] Done. Adding TextViews...')
        # Every view may have an on_click callback, which must be callable
        # or a special action defined in the CanvasBase class like
        # 'Action_StartApp', which starts the main app.
        layout.add(layout.TextView(text = '[Launch Python callback]', on_click = self.onInput))
        layout.add(layout.TextView(text = '[Launch Main App]', on_click = layout.Action_StartApp))
        
        print('[Debug] Done. Getting stored Widget data')
        data = AndroidWidgets.getWidgetData()
        # This returns the dict 'data' if the main app has called storeWidgetData(data),
        # otherwise it returns None
        #
        # Warning: Every value in the data-dict was converted to a string
        #
        # In this example we want to display an image which was set by the main app,
        # so we expect sth. like this:
        #
        # data = {
        #    'image_path': 'path/to/image'
        #}
        #
        # If we cant obtain the path, we are setting it to a default (... + '\images\test.jpg').
        source = None
        if type(data) == dict:
            print('[Debug] Returned data: ' + str(data))
            source = data.get('image_path', None)
            if source == None:
                print('[Debug] Given data has no entry "image_path"!')
        else:
            print('[Debug] Got no data (' + str(data) + '), setting image to test.jpg')
        if source == None:
            from os.path import dirname, realpath, sep
            source = dirname(realpath(__file__)) + sep + 'images' + sep + 'test.jpg'
        print('[Debug] Image source is ' + source)
        
        print('[Debug] Done. Adding ImageView...')
        # Creating a ImageView which shows the image at the given path
        layout.add(layout.ImageView(image_path = source))
        print('[Debug] Done. Adding Layout to canvas...')
        widget.canvas.add(layout)
        print('[Debug] Done. Updating Widget...')
        # We push our changes to the screen
        widget.update()
        print('[Debug] Done.')
        return True
    
    def updateWidget(self, *args, **kwargs):
        '''This function gets called every time the android AlarmManager
        schedules an update.'''# Which is currently hardcoded to 15 seconds.
        print('Python: Updating Widget...')
    
    def destroyWidget(self, *args, **kwargs):
        '''This function is called if the user deletes a widget from
        the home screen.'''
        print('Python: Destroying Widget...')
    
    def onInput(self, *args, **kwargs):
        '''A custom callback for the on_click event for one of
        the TextViews. It could be named anything ore could even
        be a function from an other module.'''
        print('Python (onInput): Got Input!')
    