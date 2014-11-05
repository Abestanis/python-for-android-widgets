# Licensed under the MIT license
# http://opensource.org/licenses/mit-license.php

# Copyright 2014 Sebastian Scholz <abestanis.gc@gmail.com> 


print('[Debug] Trying to import my module...')
PythonWidgets = None
try:
    import PythonWidgets
except Exception, msg:
    from os import environ
    if 'ANDROID_ARGUMENT' in environ:
        print('[ERROR] Failed to import PythonWidgets: %(msg)s' % {'msg': msg})
    else:
        class t(object):
            '''Fake class, so this can be used even if
            PythonWidgets is not present.'''
            def __getattr__(self, name):
                if name in ['getWidgetData']:
                    return lambda *args, **kwargs: None
                else:
                    return lambda *args, **kwargs: True
        PythonWidgets = t()
else:
    print('[Debug] Success!')
from urllib   import urlencode
from types    import MethodType
from urlparse import parse_qsl


class Widget(object):
    '''A generic class that all Widgets must inherit from.'''
    
    ### constants ###
    
    Category_Homescreen = 1
    Category_Lockscreen = 2
    
    Resize_Horizontal   = 1
    Resize_Vertical     = 2
    
    Both                = 3
    
    Action_StartApp     = '.StartActivity'
    # Starts the main app
    
    ### defaults ###
    
    widget_name         = None
    # None or string
    # 
    # The name of this widget. If name is
    # None, the class name is used instead.
    
    widget_dflt_size    = (40, 40)
    # tuple or list of two int
    # 
    # The size of the widget when the
    # user first places the widget on
    # the homescreen. Defaults to 1x1.
    
    widget_min_size     = (40, 40)
    # tuple or list of two int
    # 
    # The minimal size the widget can
    # be resized to on the homescreen.
    # Defaults to 1x1.
    # 
    # Should not be more than
    # estimateSize(4,4)
    
    
    preview_image       = None
    # None or string
    # 
    # Path to the image which will be
    # displayed for the widget at the
    # widget section of the launcher.
    # If no image is given the app
    # icon is used instead.
    
    hard_update         = 0
    # 0 or greater than 1800000
    # 
    # This defines how often the
    # widget.on_update method should
    # get called in milliseconds. If
    # the value is 0, no update will
    # be performed.
    #
    # Minimal value: 1800000 (30 min)
    #
    # Warning: use this with caution,
    # the device will wake up to
    # deliver this update if it was
    # previously sleeping, thus
    # draining the battery. It is
    # recommended to not update more
    # than once per hour with this
    # method.
    
    initial_layout      = None
    # None or string
    #
    # A string containing the layout
    # shown by the widget before
    # widget.initWidget was called
    # in a xml format. You can
    # create your layout by passing
    # the desired
    # CanvasObject-Structure to the
    # canvas_to_xml function or
    # alternatively the string
    # version of the Structure
    # you get via
    # str(CanvasObjectStructure).
    #
    # If no string is provided a
    # progressbar is shown as
    # default.
    #
    # example:
    # 
    # initial_layout = canvas_to_xml(
    #     'LinearLayout()[TextView(text=Loading...)]'
    # )
    
    
    initial_lock_layout = None
    # None or string
    #
    # Same as initial_layout, but
    # for the lockscreen.
    
    resize_mode         = Both
    # Widget.Both,
    # Widget.Resize_Horizontal,
    # Widget.Resize_Vertical or
    # None
    # 
    # Describes how the widget can
    # be resized.
    
    
    widget_category     = Category_Homescreen
    # Widget.Category_Homescreen,
    # Widget.Category_Lockscreen
    # or Widget.Both
    # 
    # Defines whether the widget
    # may be on the homescreen,
    # the lockscreen or on both.
    
    init_action         = None
    # None, True, string or dict
    # 
    # Defines the configuration-
    # action which should be
    # performed before the
    # Widget gets instantiated.
    #
    # * If it's set to None,
    #   no action will be performed.
    # * If set to True, the main
    #   app will get launched with
    #   the argument '--show_config'
    #   followed by the widgetId,
    #   the widgetClassName and
    #   the widgetName. The main App
    #   must call
    #   PythonWidgets.setConfigResult(True)
    #   to indicate that the configuration
    #   was successfull, otherwise the
    #   widget won't be created.
    # * If init_action is a string,
    #   it behaves exactly like it
    #   would be True, but the
    #   given string is added to
    #   the command arguments.
    # * If init_action is a dict,
    #   a simple config is launched
    #   whose UI is defined by this
    #   dict. The result of the
    #   configuration is stored in
    #   a string of the following
    #   format:
    #   'urlencoded_option_1,urlencoded_option_2,...'
    #   and can be accessed by the
    #   widget via getWidgetData(key)
    #   with 'key' being either
    #   the string located in
    #   dict['save_key'] or it is
    #   widgetClassName + widgetId
    #   + '_config'.
    #   All possibilities for the
    #   dict are listed below:
    #
    #   {
    #       'title': String # Window title; optional
    #       'background': [r,b,g,a] # the color of the window background; a is optional; r, b, g, a must be integers between 0 and 255; optional
    #       'save_key': String # The key where the config result will get saved; optional
    #       'children': [ # The UI elements.
    #                       Each UI element besides 'separator' and 'text' may have a description ('desc': String) and a hint ('hint': String) as well as 'desc_text_size' and 'desc_text_color', the same goes for the hint.
    #                       Hints wont get displayed, if there is no description.
    #                       The hints text size is half the normal text size by default. If you don't want this, just set 'hint_text_size' to 'default'.
    #
    #           {'type': 'separator'},
    #           # Draws a separator line between the previous element and the next.
    #           # Will not deposit any value in the result!
    #
    #           {'type': 'text', 'text': String, 'text_size': Integer, 'text_color': [r,g,b,a]},
    #           # Displays text.
    #           # Arguments:
    #           #   text:       The text to display
    #           #   text_size:  The text size, optional
    #           #   text_color: The text color, a is optional, optional
    #           # Will not deposit any value in the result!
    #
    #           {'type': 'togglebutton', 'text_on': String, 'text_off': String, 'state': 1/'on'/0/'off'},
    #           # A button with two states, on and off
    #           # Arguments:
    #           #   text_on:  The text that should be displayed, if the buttons state is on,  optional
    #           #   text_off: The text that should be displayed, if the buttons state is off, optional
    #           #   state:    The initial state of the togglebutton, may be either 'on', 1, 'off' or 0, optional, defaults to 1
    #           # Will deposit its state as one of the strings 'true' or 'false'
    #
    #           {'type': 'switch'},
    #           # A switch with two states, on and off, Warning: Switches require android version 4.0 (ICE_CREAM_SANDWICH) or higher, a togglebutton is used instead
    #           # Arguments:
    #           #   see togglebutton
    #           # Will deposit its state the same as a togglebutton.
    #
    #           {'type': 'seek_bar', 'default': Integer, 'min': Integer, 'max': Integer},
    #           # A horizontal seekbar.
    #           # Arguments:
    #           #   default: The initial value of the seekbar, optional, defaults to 0
    #           #   min:     The minimal allowed value,        optional, defaults to 0
    #           #   max:     The maximal allowed value,        optional, defaults to 100
    #           # Will deposit its value as a number.
    #
    #           {'type': 'text_input', 'default': String, 'text_hint': String, 'password': True/False, 'multiline': True/False},
    #           # A textinput field
    #           # Arguments:
    #           #   default:   The text the textinput should hold when building the config UI
    #           #   text_hint: A hint text, that will get displayed, if the textinput is empty, optional, defaults to ''
    #           #   password:  If set to true, dots will be shown instead of the input,         optional, defaults to False
    #           #   multiline: If set to True, allows the user to type in multiple lines,       optional, defaults to False
    #           # Will deposit its value as a string.
    #
    #           {'type': 'num_input', 'default': String, 'text_hint': String, 'password': True/False, 'multiline': True/False, 'decimal': True/False, 'disallow_negative': True/False, 'min': Integer, 'max': Integer},
    #           # Same as text_input, but only allows numbers, setting the virtual keyboard layout appropriate
    #           # Arguments:
    #           #   default, text_hint, password and multiline are the same as in text_input
    #           #   !!! Warning: The password flag only works for android version 3.0 (HONEYCOMB) or higher !!!
    #           #   decimal:            If set to True, allows a decimal point ('.') in the number-string, optional, defaults to False
    #           #   disallow_negative:  Disallows negative numbers as an input,                            optional, defaults to False
    #           #   !!! Warning: The above parameter will not work on android versions below 3.0 (HONEYCOMB) and will default to True. This means you can't input negative numbers on android versions prior to 3.0. Use the seebar to for those versions !!!
    #           #   min:                The minimal allowed number,                                        optional, no limit by default
    #           #   max:                The maximal allowed number,                                        optional, no limit by default
    #           # Will deposit its value as a number.
    #
    #           {'type': 'mail_input', 'default': String, 'text_hint': String, 'password': True/False, 'multiline': True/False},
    #           # Same as text_input, setting the virtual keyboard layout for mail input
    #           # Arguments:
    #           #   Same as text_input
    #           # Will deposit its value the same as the text_input.
    #
    #           {'type': 'time_input', 'default': time, 'format': String, 'is24h': True/False},
    #           # An input for hours and minutes
    #           # Arguments:
    #           #   default: The initial time to show, can be a timestamp (e.g. via time.time()) or a string in the following pattern 'hh:mm',        optional, defaults to the current time
    #           #   format:  The format the time should get displayed,                                                                                optional, defaults to 'hh:mm a' (' a' depending on is24h)
    #           #   !!! Warning: Will affect the way the time value will get stored in the result !!!
    #           #   ... Info:    For format possibilities see 'http://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html' ...
    #           #   is24h:   Controls weather the user should input hours from 0 to 24 or from 0 to 12 with am and pm, influences the default format, optional, defaults to True
    #           # Will deposit its value as a string in the format given or 'hh:mm a'.
    #
    #           {'type': 'date_input', 'default': time, 'format': String},
    #           # An input for dates; day, month, year
    #           # Arguments:
    #           #   default: Same as time_input, but the string is expected in the format 'dd.MM.yyyy'
    #           #   format:  Same as time_input, but defaults to 'dd.MM.yyyy'
    #           # Will deposit its value as a string in the format given or 'dd.MM.yyyy'.
    #
    #           {'type': 'web_input', 'default': String, 'text_hint': String, 'password': True/False, 'multiline': True/False},
    #           # Same as text_input, setting the virtual keyboard layout appropriate
    #           # Arguments:
    #           #   Same as text_input
    #           # Will deposit its value the same as the text_input.
    #
    #           {'type': 'list_option', 'default': String, 'options': list of strings, 'multioption': True/False},
    #           # Lets the user choose from an list of options
    #           # Arguments:
    #           #   default:     The initial option to be picked, may or may not be in 'options', optional, defaults to the first element of 'options'
    #           #   options:     The list of options the user can choose of
    #           #   multioption: If set to True, the user is able to choose multiple options,     optional, defaults to False
    #           # Will deposit its value as the chosen option string, or the chosen strings separated by ', '
    #
    #           {'type': 'color_picker', 'default': [r,g,b,a], 'transparent': True/False},
    #           # Lets the user pick a color
    #           # Arguments:
    #           #   default:     The initial color as a list of [r,g,b,a], r, g, b and a are numbers from 0 to 255, a is optional and defaults to 255, optional, defaults to [0,0,0,255] (Black)
    #           #   transparent: If set to True allows the user to chose the alpha channel, allowing him to set the color to fully transparent,        optional, defaults to False
    #           # Will deposit its value as a hexadecimal number (e.g. ff000000 for a nontransparent Black).
    #       ]
    #   }
    
    sub_widgets         = []
    # list of dicts
    # 
    # Describes widgets, that use
    # the same Widget-class, but
    # have different values.
    # Usefully to create smaller
    # and larger variants of the
    # same widget.
    # 
    # example:
    #
    # class MyWidget(Widget):
    #     widget_dflt_size = estimateSize(2,2)
    #     sub_widgets      = [
    #         {
    #             'widget_name':      'MyWidgetLarge',
    #             'widget_dflt_size': estimateSize(4,4)
    #         },
    #         {
    #             'widget_name':      'MyWidgetSmall',
    #             'widget_dflt_size': estimateSize(1,1)
    #         }
    #     ]
    
    def __init__(self, *args, **kwargs):
        '''This function will initialize the given widget 'widget'
        and will set it's visual appearance. If this function
        throws an error, the initialisation is considered as failed
        and the widget will not be created.
        '''
        pass
    
    def updateWidget(self, *args, **kwargs):
        '''This function gets called every time the widget receives
        an update.
        '''
        pass
    
    def destroyWidget(self, *args, **kwargs):
        '''This function is called if the user deletes a widget from
        the home screen.
        '''
        pass


def estimateSize(*args):
    '''>>> estimateSize(num_cells) -> (dp)
    or
    >>> estimateSize(num_cells_x, num_cells_y) -> (dp, dp)
    Roughly convert from numbers of grid-cells
    on the homescreen to dp, using the
    formula 70 * num_cells - 30 given from
    'http://developer.android.com/guide/practices/ui_guidelines/widget_design.html#cellstable'.
    '''
    if len(args) == 1:
        return 70 * args[0] - 30
    if len(args) == 2:
        return (70 * args[0] - 30, 70 * args[1] - 30)
    else:
        return -1
    

def canvas_to_xml(canvas):
    '''>>> canvas_to_xml(canvas/canvas_str) -> xml_string
    Converts the given Canvas or Canvas-string 'canvas'
    in a string containing an xml representation.
    '''
    if type(canvas) == str:
        canvas = Canvas(canvas)
    alias = {
        'image_path': 'src'
    }
    forbidden = [
        'UrlImageView'
    ]
    print('[Debug] Converting ' + str(canvas) + ' to xml...')
    
    def children_to_xml(children):
        xml = ''
        
        for child in children:
            name = child._repr
            if name in forbidden:
                print('[ERROR]' + name + ' is not allowed in xml files!')
                continue
            xml += '<' + name + ' xmlns:android="http://schemas.android.com/apk/res/android"\n'
            
            # parameters
            
            size_width  = False
            size_height = False
            
            for key, val in parse_qsl(child._args):
                if key == 'on_click':
                    print('[Warn ] "on_click" events are not supported in xml files.')
                    continue
                size_width  = key == 'width'  or size_width
                size_height = key == 'height' or size_height
                if key == 'image_path': # The image needs to be in the resource folder
                    val = '{{% "' + val + '" %}}'
                else:
                    val = '"' + val + '"'
                key = alias.get(key, key)
                
                xml += 'android:' + key + '=' + val + '\n'
            if not size_width:
                xml += 'android:layout_width="wrap_content"\n'
            if not size_height:
                xml += 'android:layout_height="wrap_content"\n'
            
            # children
            
            if len(child.children) != 0:
                xml += '>\n\n'
                xml += children_to_xml(child.children)
                xml += '</' + name + '>\n\n'
            else:
                xml += '/>\n\n'
        return xml
    
    
    return children_to_xml(canvas.children)


class CanvasBase(object):
    '''This is the base class for every canvas Object.'''
    
    children = None
    _repr = None
    _args = None
    
    # All possible canvas objects
    _canvasObjects = {
        #-- Layouts: --#
        'LinearLayout':       {},
        'FrameLayout':        {},
        'RelativeLayout':     {},
        #'GridLayout':         {},
        
        #--  Views:  --#
        'TextView':           {
            'setText':      lambda self, text:  self._addOption('text', text),
            'setTextColor': lambda self, color: self._addOption('text_color', color),
        },
        'AnalogClock':        {},
        'Button':             {},
        'Chronometer':        {},
        'ImageButton':        {},
        'ImageView':          {
            'setImagePath': lambda self, source_path: self._addOption('image_path', source_path)
        },
        'ProgressBar':        {},
        'ViewFlipper':        {},
        'ListView':           {},
        'GridView':           {},
        #'StackView':          {},
        #'AdapterViewFlipper': {},
        'ViewStub':           {},
        
        'UrlImageView':       {
            'setImageUrl': lambda self, url: self._addOption('image_url', url)
        }
    }
    
    def __init__(self, canvas = None):
        self.children = []
        self._args     = ''
        if canvas:
            print('Adding children given to __init__...')
            if type(canvas) == str:
                print('children given via string, decoding...')
                canvas_stack = [Canvas()]
                par_index = canvas.find('(')
                while par_index != -1:
                    print('---> Canvas: ' + canvas)
                    view = canvas[:par_index]
                    if not view in self._canvasObjects:
                        print('[ERROR] Got unknown view object ' + view)
                        canvas_stack = [Canvas()]
                        break
                    view = CanvasObject(view)
                    par_end = canvas.find(')', par_index)
                    if not par_index + 1 == par_end:
                        # Parameter
                        print('Parameter: ' + canvas[par_index + 1:par_end])
                        view._args = canvas[par_index + 1:par_end]
                    canvas = canvas[par_end + 1:]
                    if canvas.startswith('['):
                        canvas_stack.append(view)
                        canvas = canvas[1:]
                    else:
                        canvas_stack[-1].add(view)
                    if canvas[:2] == ', ':
                        canvas = canvas[2:]
                    while canvas.startswith(']'):
                        if len(canvas_stack) > 1:
                            canvas_stack[-2].add(canvas_stack[-1])
                            canvas_stack.pop()
                        canvas = canvas[1:]
                    par_index = canvas.find('(')
                canvas = canvas_stack[0]
            self.add(canvas)
    
    def __getattr__(self, name):
        if name in self._canvasObjects.keys():
            # Generate a new Canvas Object
            new = CanvasObject(name)
            for m_name, method in self._canvasObjects[name].iteritems():
                setattr(new, m_name, MethodType(method, new, new.__class__))
            return new
        else:
            # default
            raise AttributeError
    
    def __repr__(self):
        '''Returns a representation of this CanvasObject and of all it's
        children that the java side can interpret and form the widgets
        view from.
        '''
        if type(self._repr) == str:
            return self._repr + '(' + (self._args if type(self._args) == str else '') + ')' + (str(self.children) if len(self.children) > 0 else '')
        if len(self.children) == 1:
            # This CanvasObject must be a pure canvas
            return str(self.children[0])
        return ''
    
    def __call__(self, **kwargs):
        '''This is used to initialize a CanvasObject arguments
        all at once.
        '''
        for key, value in kwargs.iteritems():
            if key == 'on_click' and callable(value):
                PythonWidgets.addOnClick(str(id(self)), value)
                kwargs[key] = id(self)
            elif callable(value):
                kwargs[key] = value.__name__
        self._args = urlencode(kwargs)
        return self
    
    def _addOption(self, name, value):
        '''Used to add one argument with the name 'name' and the
        value 'value' to the stored arguments self._args.
        '''
        print('Adding option ' + name + ': ' + str(value))
        print(self._args, urlencode({name: ''}))
        index = self._args.find(urlencode({name: ''}))
        if index != -1:
            end = self._args.find('&', index)
            if end == -1:
                end = len(self._args)
            tmp = self._args[:max(index - 1, 0)]
            self._args = tmp + self._args[end + 1:]
        if self._args != '':
            self._args = self._args + '&'
        if name == 'on_click' and callable(value):
            PythonWidgets.addOnClick(str(id(self)), value)
            value = id(self)
        if callable(value):
            value = value.__name__
        self._args = self._args + urlencode({name: value})
        return True
    
    def add(self, child, index = None):
        '''>>> add(child[, index]) -> True or False
        Adds the CanvasObject 'child' at the given
        index 'index' or at the end, if no index was
        given. Returns True on success, False on failure.
        '''
        if not isinstance(child, CanvasBase):
            print('[ERROR] Could not add child (' + str(child) + ') to the canvas because it is not a canvas object!')
            return False
        if not self._repr and len(self.children) > 0:
            print('[ERROR] Can not add more than one root widget to canvas (Could not add ' + str(child) + ')!')
            return False
        print('Adding child "' + str(child) + '" to canvas...')
        if not child._repr:
            print('Given child is a Canvas...')
            while not child._repr:
                if len(child.children) == 0:
                    # No need to add an empty canvas
                    print('[Debug] Given canvas is empty.')
                    return True
                child = child.children[0]
        if index and type(index) != int:
            print('[Warn ] addView was called with a non numeric index: ' + str(index))
            return False
        elif index:
            print('Adding it to ' + str(index))
            self.children.insert(index, child)
        else:
            print('Appending it to the end.')
            self.children.append(child)
        return True
    
    def clear(self):
        '''>>> clear()
        Clears all children from this CanvasObject.
        '''
        print('Clearing canvas...')
        self.children = []
    
    def remove(self, arg):
        '''>>> remove(index or CanvasObject) -> CanvasObject or True
        Removes the given CanvasObject 'arg' or the CanvasObject at the
        index 'arg' from from the children list.
        '''
        
        if type(arg) == int:
            print('Removing child from index' + str(arg))
            return self.children.pop(arg)
        else:
            print('Removing ' + str(arg))
            self.children.remove(arg)
            return True
    

class CanvasObject(CanvasBase):
    '''This class represents an object which can be added to an
    canvas and represents a view from the view-tree of a widget.
    '''
    
    def __init__(self, rep):
        '''Initializes the CanvasObject and sets its representation 'rep',
        which indicates what kind of CanvasObject this one is.'''
        print('Init CanvasObject (' + str(rep) +')...')
        super(CanvasObject, self).__init__()
        self._repr = rep
    
    def setOnClickListener(self, callback):
        '''>>> setOnClickListener(callback) -> True or False
        Setts the callback for this CanvasObject for an
        on_click event.'''
        return self._addOption('on_click', callback)
    

class Canvas(CanvasBase):
    '''A representation of the empty canvas.'''
    
    def __init__(self, canvas = None):
        print('Init Canvas...')
        super(Canvas, self).__init__(canvas)
        self._repr = False
    

class ExternalWidget(object):
    '''Every instance of this class represents an existing widget on
    the android home screen.
    '''
    
    widget_Id = -1
    canvas    = None
    
    def __init__(self, widget_Id):
        print('Init external widget.')
        self.widget_Id = widget_Id
        print('Creating own canvas...')
        self.canvas    = Canvas()
        print('Canvas created sucessfully.')
    
    def update(self):
        '''>>> update()
        Pushes the current canvas to the screen.
        '''
        print('Method update of Widget ' + str(self.widget_Id) + ' is getting called.')
        print('[[[----------- Canvas: -----------]]]\n' + str(self.canvas))
        PythonWidgets.updateWidget(self.widget_Id, str(self.canvas))
        print('Method update of Widget ' + str(self.widget_Id) + ' called sucessfully.')
    

def getWidget(widget_ID):
    '''>>> getWidget(widget_ID) -> widget or None
    This function returns an instance that represents
    the widget with the id 'widget_ID' or None,
    if no widget with the given id exists.
    '''
    print('getWidget is called, widget_ID is ' + str(widget_ID))
    if PythonWidgets.existWidget(widget_ID): # See if our widget_Id is in the list
        print('Widget Id exists, creating ext widget...')
        ExtWidget = ExternalWidget(widget_ID)
        print('Done creating ext widget, returning it')
        return ExtWidget
    print('Widget Id does not exist!')
    return None

def getDefaultError():
    '''>>> getDefaultError() -> canvas or None
    Returns the default view that is shown, if
    the initialisation of a widget failed; if
    the initWidget function did not return True
    or if it throws an error.
    '''
    err_view = PythonWidgets.getDefaultErrorView()
    if err_view == None:
        return None
    else:
        return Canvas(err_view)

def setDefaultError(canvas = None):
    '''>>> setDefaultError([canvas]) -> success
    Sets the default view that is shown, if
    the initialisation of a widget failed; if
    the initWidget function did not return True
    or if it throws an error. If no canvas is
    given, a predefined default canvas is used.
    '''
    if canvas and (not isinstance(canvas, CanvasBase)):
        print('[ERROR] Could not set the default error view because the given argument (' + str(canvas) + ' is not a Canvas!')
        return False
    print('Setting the default error view to ' + str(canvas))
    return PythonWidgets.setDefaultErrorView(str(canvas) if canvas else canvas)    

def getWidgetData(key):
    '''>>> getWidgetData(key) -> string or None
    Returns the string, that was stored in the
    key 'key' via 'storeWidgetData' or None, if
    there is no data stored in that key.
    '''
    print('Calling PythonWidgets.getWidgetData...')
    data = PythonWidgets.getWidgetData(key)
    return data or None # Ignoring occoured errors (False --> None)

def setWidgetData(key, data):
    '''>>> setWidgetData(key, data) -> success
    Stores 'data' in key 'key', to access it
    later via getWidgetData. 'key' is eighter
    the string to store or None, to remove the
    data previously stored in the key.
    '''
    print('Calling PythonWidgets.storeWidgetData...')
    return PythonWidgets.storeWidgetData(key, data)

def setInitAction(new_action):
    '''>>> getWidgetData() -> success
    Setts the action, that shoul be performed, if
    an widget from the current provider is created
    by the user.
    '''
    print('Calling PythonWidgets.setInitAction...')
    return PythonWidgets.setInitAction(new_action)
