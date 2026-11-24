"""Regression checks for drag transfers using the real client and vanilla server."""

import importlib.util
import json
from pathlib import Path
import re
import time


BAG = ((347, 394), (383, 394))
HOTBAR = ((347, 510), (383, 510))
STORAGE = ((347, 261), (383, 261))
CREATIVE_BAG = ((329, 363), (365, 363))
CREATIVE_HOTBAR = ((329, 479), (365, 479))
BLOCK_POS = '0 -60 2'
PLAYER = 'ComfortQA'
CONTENTS = {9: ('stone', 4), 10: ('dirt', 5)}
MOVED = {0: CONTENTS[9], 1: CONTENTS[10]}


def load(filename):
    spec = importlib.util.spec_from_file_location(filename, Path(__file__).with_name(filename + '.py'))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def press(ui, key):
    ui.key(key)
    ui.key(key, True)
    time.sleep(.4)


def drag(ui, *positions):
    ui.move(*positions[0])
    ui.key(0x10)
    ui.u.mouse_event(2, 0, 0, 0, 0)
    try:
        time.sleep(.15)
        for position in positions[1:]:
            ui.move(*position)
    finally:
        ui.u.mouse_event(4, 0, 0, 0, 0)
        ui.key(0x10, True)
    time.sleep(.45)


def read_slots(response):
    if response.rstrip() == 'Found no elements matching Items':
        return {}
    slots = {}
    for stack in re.findall(r'\{[^{}]+\}', response):
        slot = re.search(r'Slot: (-?\d+)b?', stack)
        item = re.search(r'id: "minecraft:([a-z_]+)"', stack)
        count = re.search(r'(?:Count|count): (\d+)', stack)
        if not (slot and item and count):
            raise ValueError(f'Unexpected stack from vanilla server: {stack}')
        slots[int(slot[1])] = (item[1], int(count[1]))
    if not slots and not response.rstrip().endswith('[]'):
        raise ValueError(f'Unexpected inventory response from vanilla server: {response}')
    return slots


def seed(server, stacks):
    server.send(f'clear {PLAYER}')
    for slot, (item, count) in stacks.items():
        target = f'hotbar.{slot}' if slot < 9 else f'inventory.{slot - 9}'
        response = server.send(f'item replace entity {PLAYER} {target} with minecraft:{item} {count}')
        if 'Replaced' not in response:
            raise RuntimeError(response)
    time.sleep(.35)


def inventory_panel(ui):
    image = ui.ImageGrab.grab(bbox=(ui.point.x, ui.point.y, ui.point.x + ui.r.right, ui.point.y + ui.r.bottom),
                              all_screens=True)
    return all(image.getpixel(position)[:3] == (198, 198, 198) for position in ((645, 231), (650, 358)))


def open_block(ui, server, block):
    server.send('setblock 0 -60 1 air')
    server.send(f'setblock {BLOCK_POS} air')
    server.send(f'setblock {BLOCK_POS} minecraft:{block}')
    server.send(f'execute positioned {BLOCK_POS} run kill @e[type=minecraft:item,distance=..8]')
    server.send(f'tp {PLAYER} 0.5 -60 0.5 0 31')
    time.sleep(.6)
    ui.mouse(1)
    time.sleep(.4)
    if not inventory_panel(ui):
        ui.capture(f'failed-open-{block}')
        raise RuntimeError(f'Failed to open {block}')


def run(ui, server):
    results = []

    def check(name, expected, block_expected=None):
        inventory = server.send(f'data get entity {PLAYER} Inventory')
        storage = server.send(f'data get block {BLOCK_POS} Items') if block_expected is not None else None
        ok = read_slots(inventory) == expected and (storage is None or read_slots(storage) == block_expected)
        results.append({'scenario': name, 'ok': ok, 'inventory': inventory, 'storage': storage,
                        'expectedInventory': expected, 'expectedStorage': block_expected})
        print(f'{name}: {ok}', flush=True)
        (ui.output / 'drag-regression.json').write_text(json.dumps(results, indent=2), encoding='utf-8')
        ui.move(715, 400)
        ui.capture(name)

    server.send(f'gamemode survival {PLAYER}')
    time.sleep(.4)
    seed(server, CONTENTS)
    for _ in range(3):
        if inventory_panel(ui):
            break
        press(ui, 0x45)
    if not inventory_panel(ui):
        ui.capture('failed-open-inventory')
        raise RuntimeError('Failed to open survival inventory')
    drag(ui, *BAG, HOTBAR[1])
    check('survival-inventory-to-hotbar', MOVED)
    drag(ui, *HOTBAR)
    check('survival-hotbar-to-inventory', CONTENTS)
    full_hotbar = {slot: ('cobblestone', 64) for slot in range(9)}
    seed(server, CONTENTS | full_hotbar)
    drag(ui, *BAG)
    check('survival-full-hotbar-keeps-items', CONTENTS | full_hotbar)
    press(ui, 0x45)

    seed(server, CONTENTS)
    open_block(ui, server, 'crafting_table')
    drag(ui, *BAG)
    check('survival-workbench-to-hotbar', MOVED)
    drag(ui, *HOTBAR)
    check('survival-workbench-to-inventory', CONTENTS)
    press(ui, 0x45)

    seed(server, CONTENTS | {11: ('white_shulker_box', 1)})
    open_block(ui, server, 'shulker_box')
    drag(ui, *BAG, (419, 394))
    check('shulker-storage-and-nesting-restriction', {11: ('white_shulker_box', 1)}, MOVED)
    drag(ui, *STORAGE)
    check('shulker-storage-to-inventory', CONTENTS | {11: ('white_shulker_box', 1)}, {})
    press(ui, 0x45)

    server.send(f'gamemode creative {PLAYER}')
    time.sleep(.4)
    seed(server, CONTENTS)
    press(ui, 0x45)
    ui.move(659, 535)
    ui.mouse()
    drag(ui, *CREATIVE_BAG, CREATIVE_HOTBAR[1])
    check('creative-inventory-to-hotbar', MOVED)
    drag(ui, *CREATIVE_HOTBAR)
    check('creative-hotbar-to-inventory', CONTENTS)
    press(ui, 0x45)

    seed(server, CONTENTS)
    open_block(ui, server, 'crafting_table')
    drag(ui, *BAG)
    check('creative-workbench-to-hotbar', MOVED)
    press(ui, 0x45)

    seed(server, CONTENTS)
    open_block(ui, server, 'chest')
    drag(ui, *BAG)
    check('creative-chest-stash', {}, MOVED)
    drag(ui, *STORAGE)
    check('creative-chest-retrieve', CONTENTS, {})
    press(ui, 0x45)
    return all(result['ok'] for result in results)


if __name__ == '__main__':
    ui = load('fixture-input')
    with load('fixture-rcon').Rcon() as server:
        if PLAYER not in server.send('list'):
            raise RuntimeError('Test client has not joined the vanilla server')
        try:
            passed = run(ui, server)
        finally:
            server.send(f'gamemode survival {PLAYER}')
            server.send(f'setblock {BLOCK_POS} minecraft:chest')
    raise SystemExit(0 if passed else 1)
