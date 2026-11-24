"""Connect from the English 1.20.1 title screen when loader Quick Play is unavailable."""
import importlib.util
from pathlib import Path
import time

spec = importlib.util.spec_from_file_location('fixture_input', Path(__file__).with_name('fixture-input.py'))
ui = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ui)

ui.capture('before-direct-connect')
ui.move(490,352); ui.mouse()
time.sleep(.6)
ui.move(490,665); ui.mouse()
time.sleep(.35)
for char in f'127.0.0.1:{ui.manifest["port"]}':
    if char==':': ui.key(0x10); ui.key(0xba); ui.key(0xba,True); ui.key(0x10,True)
    else:
        key = 0xbe if char == '.' else ord(char)
        ui.key(key); ui.key(key,True)
ui.key(0x0d); ui.key(0x0d,True)
