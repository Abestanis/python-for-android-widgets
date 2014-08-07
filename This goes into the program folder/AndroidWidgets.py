# Licensed under the MIT license
# http://opensource.org/licenses/mit-license.php

# Copyright 2014 Sebastian Scholz <abestanis.gc@gmail.com> 


print('[Debug] Trying to import my module...')
import PythonWidgets
print('[Debug] Success!')
from urllib   import urlencode
from types    import MethodType
from urlparse import parse_qsl

class CanvasBase(object):
    '''This is the base class for every canvas Object.'''
    
    children = None
    _repr = None
    _args = None
    
    # All special actions for the on_click callback
    Action_StartApp = '.StartActivity'
    
    # All possible canvas objects
    _canvasObjects = {
        #-- Layouts: --#
        'LinearLayout':       {},
        'FrameLayout':        {},
        'RelativeLayout':     {},
        #'GridLayout':         {},
        
        #--  Views:  --#
        'TextView':           {
            'setText': lambda self, text: self._addOption('text', text)
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
        print('Init CanvasBase...')
        self.children = []
        self._args     = ''
        if canvas:
            print('Adding children given to __init__...')
            if type(canvas) == str:
                print('children given via string, decoding...')
                canvas_stack = [Canvas()]
                par_index = canvas.find('(')
                while par_index != -1:
                    print('Canvas: ' + canvas)
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
                    print('New canvas = ' + canvas[par_end + 1:])
                    canvas = canvas[par_end + 1:]
                    if canvas.startswith('['):
                        print('canvas_stack += 1')
                        canvas_stack.append(view)
                    else:
                        print('Adding view to top-level view...')
                        canvas_stack[-1].add(view)
                    if canvas.startswith(']'):
                        print('canvas_stack -= 1')
                        if len(canvas_stack) > 1:
                            print('Adding top view to next layer...')
                            canvas_stack[-2].add(canvas_stack[-1])
                            canvas_stack.pop()
                    canvas = canvas[1:]
                    par_index = canvas.find('(')
                canvas = canvas_stack[0]
            self.add(canvas)
    
    def __getattr__(self, name):
        print('Proxy: ' + str(name))
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
        view from.'''
        if type(self._repr) == str:
            return self._repr + '(' + (self._args if type(self._args) == str else '') + ')' + (str(self.children) if len(self.children) > 0 else '')
        if len(self.children) == 1:
            # This CanvasObject must be a pure canvas
            return str(self.children[0])
        return ''
    
    def __call__(self, **kwargs):
        '''This is used to initialize a CanvasObject arguments
        all at once.'''
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
        value 'value' to the stored arguments self._args.'''
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
        given. Returns True on success, False on failure.'''
        if not isinstance(child, CanvasBase):
            print('[ERROR] Could not add child (' + str(child) + ' to the canvas because it is not a canvas object!')
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
        Clears all children from this CanvasObject.'''
        print('Clearing canvas...')
        self.children = []
    
    def remove(self, arg):
        '''>>> remove(index or CanvasObject) -> CanvasObject or True
        Removes the given CanvasObject 'arg' or the CanvasObject at the
        index 'arg' from from the children list.'''
        
        if type(arg) == int:
            print('Removing child from index' + str(arg))
            return self.children.pop(arg)
        else:
            print('Removing ' + str(arg))
            self.children.remove(arg)
            return True
    

class CanvasObject(CanvasBase):
    '''This class represents an object which can be added to an
    canvas and represents a view from the view-tree of a widget.'''
    
    
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
    the android home screen.'''
    
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
        Pushes the current canvas to the screen.'''
        print('Metod update of Widget ' + str(self.widget_Id) + ' is getting called.')
        print('[[[----------- Canvas: -----------]]]\n' + str(self.canvas))
        PythonWidgets.updateWidget(self.widget_Id, str(self.canvas))
        print('Metod update of Widget ' + str(self.widget_Id) + ' called sucessfully.')
    

def getWidget(widget_ID):
    '''>>> getWidget(widget_ID) -> widget or None
    This function returns an instance that represents
    the widget with the id 'widget_ID' or None,
    if no widget with the given id exists.'''
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
    or if it throws an error.'''
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
    given, a predefined default canvas is used.'''
    if canvas and (not isinstance(canvas, CanvasBase)):
        print('[ERROR] Could not set the default error view because the given argument (' + str(canvas) + ' is not a Canvas!')
        return False
    print('Setting the default error view to ' + str(canvas))
    return PythonWidgets.setDefaultErrorView(str(canvas) if canvas else canvas)

def getDefaultLoad():
    '''>>> getDefaultLoad() -> canvas or None
    Sets the default view that is shown during
    the initialisation of a widget, before the
    first call to widget.update().'''
    err_view = PythonWidgets.getDefaultLoadView()
    if err_view == None:
        return None
    else:
        return Canvas(err_view)

def setDefaultLoad(canvas = None):
    '''>>> setDefaultLoad([canvas]) -> success
    Sets the default view that is shown during
    the initialisation of a widget, before the
    first call to widget.update(). If no canvas
    is given, a predefined default canvas is used.'''
    if canvas and (not isinstance(canvas, CanvasBase)):
        print('[ERROR] Could not set the default loading view because the given argument (' + str(canvas) + ' is not a Canvas!')
        return False
    print('Setting the default loading view to ' + str(canvas))
    return PythonWidgets.setDefaultLoadView(str(canvas) if canvas else canvas)
    

def getWidgetData():
    '''>>> getWidgetData() -> dict or None
    Returns the dict, that was passed to
    'storeWidgetData' in the main App or
    None, if there is no data stored.'''
    print('Calling PythonWidgets.getWidgetData...')
    data = PythonWidgets.getWidgetData()
    if type(data) == str:
        return dict(parse_qsl(data))
    return None
    