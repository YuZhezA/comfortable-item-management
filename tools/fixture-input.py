import ctypes as c
import ctypes.wintypes as w
import json
import os
import sys
import time
from pathlib import Path
from PIL import ImageGrab

instance=Path(os.environ['COMFORT_QA_INSTANCE'])
manifest=json.loads((instance/'launch.json').read_text('utf-8'))
output=instance/'evidence'
output.mkdir(exist_ok=True)
u=c.windll.user32
u.SetProcessDPIAware()
handles=[]
@c.WINFUNCTYPE(w.BOOL,w.HWND,w.LPARAM)
def collect(hwnd, extra):
    pid=w.DWORD()
    u.GetWindowThreadProcessId(hwnd,c.byref(pid))
    if pid.value==manifest['pid'] and u.IsWindowVisible(hwnd):
        title=c.create_unicode_buffer(200)
        u.GetWindowTextW(hwnd,title,len(title))
        if 'Minecraft' in title.value: handles.append(hwnd)
    return True

u.EnumWindows(collect,0)
if len(handles)!=1: raise RuntimeError(f'Expected one verified Minecraft window, found {len(handles)}')
hwnd=handles[0]
if u.GetForegroundWindow()!=hwnd:
    u.keybd_event(0x12,0,0,0); u.keybd_event(0x12,0,2,0)
    u.SetForegroundWindow(hwnd)
    time.sleep(.4)
if u.GetForegroundWindow()!=hwnd: raise RuntimeError('Minecraft test window did not take focus')
r=w.RECT()
u.GetClientRect(hwnd,c.byref(r))
point=w.POINT(0,0);u.ClientToScreen(hwnd,c.byref(point))
if (r.right,r.bottom)!=(984,751) or point.x>=0:
    raise RuntimeError(f'Unexpected test client rectangle {(point.x,point.y,r.right,r.bottom)}')

def move(x,y):
    u.SetCursorPos(point.x+round(float(x)),point.y+round(float(y)))
    time.sleep(.14)

def mouse(button=0):
    down,up=[(2,4),(8,16)][button]
    u.mouse_event(down,0,0,0,0); time.sleep(.08); u.mouse_event(up,0,0,0,0)
    time.sleep(.18)

def key(k,release=False):
    u.keybd_event(k,0,2 if release else 0,0)
    time.sleep(.06)

def capture(name):
    path=output/(name+'.png')
    ImageGrab.grab(bbox=(point.x,point.y,point.x+r.right,point.y+r.bottom),all_screens=True).save(path)
    print(path, (point.x,point.y,r.right,r.bottom),flush=True)


def inventory_panel():
    image=ImageGrab.grab(bbox=(point.x,point.y,point.x+r.right,point.y+r.bottom),all_screens=True)
    # Gray interior samples above the title and inside the empty lower row.
    return all(max(pixel)-min(pixel) <= 4 and 100 < pixel[0] < 245 for pixel in
               (image.getpixel((333,215)),image.getpixel((633,310))))
