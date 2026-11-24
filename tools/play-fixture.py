import argparse
import importlib.util
import json
import pathlib
import time
import re

def load(name, filename):
    spec = importlib.util.spec_from_file_location(name, pathlib.Path(__file__).with_name(filename))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

ui = load('input_fixture', 'fixture-input.py')
rcon = load('rcon_fixture', 'fixture-rcon.py')

ALT, SHIFT, CTRL, Q, R, ESC = 0x12, 0x10, 0x11, 0x51, 0x52, 0x1b
CHEST = (347, 259)
BAG = (347, 394)
HOTBAR = (347, 511)
parser = argparse.ArgumentParser(description='Drive a verified secondary-display client against its vanilla fixture.')
parser.add_argument('--coexist', action='store_true')
parser.add_argument('--durability', action='store_true')
parser.add_argument('--bindings', action='store_true')
parser.add_argument('--extended', action='store_true')
options = parser.parse_args()
results = []
report_name = next((f'{name}.json' for name in ('extended','coexist','durability','bindings') if getattr(options,name)), 'playtest.json')
game_version = ui.manifest['target'].split('-', 1)[1]


def button(xy, *modifiers, right=False):
    ui.move(*xy)
    for key in modifiers: ui.key(key)
    ui.mouse(int(right))
    for key in reversed(modifiers): ui.key(key, True)
    time.sleep(.4)

def press(key, *modifiers):
    for mod in modifiers: ui.key(mod)
    ui.key(key); ui.key(key, True)
    for mod in reversed(modifiers): ui.key(mod, True)
    time.sleep(.4)

def scroll(xy, forward, *modifiers):
    ui.move(*xy)
    for key in modifiers: ui.key(key)
    ui.u.mouse_event(0x800,0,0,120 if forward else -120,0)
    time.sleep(.13)
    for key in reversed(modifiers): ui.key(key, True)
    time.sleep(.4)

def player(slot, item, count=1):
    return f'item replace entity ComfortQA {slot} with {item} {count}'

def container(slot, item, count=1):
    return f'item replace block 0 -60 2 container.{slot} with {item} {count}'

def state(slot, item, count):
    return f'{{Slot: {slot}b, id: "minecraft:{item}", Count: {count}b}}'

def evidence(name, player_want, chest_want, player_exclude=(), chest_exclude=(), predicate=None):
    inventory = server.send('data get entity ComfortQA Inventory')
    chest = server.send('data get block 0 -60 2 Items')
    equipment = {}
    normalized_inventory = normalized(inventory)
    if game_version in ('1.21.11', '26.3'):
        equipment_slots = {'offhand': -106}
        if options.durability or options.extended: equipment_slots.update(feet=100, legs=101, chest=102, head=103)
        for key, slot in equipment_slots.items():
            equipment[key] = server.send(f'data get entity ComfortQA equipment.{key}')
            if 'id: ' in equipment[key]:
                normalized_inventory += normalized(equipment[key].replace('data: {', f'data: {{Slot: {slot}b, ', 1))
    normalized_chest = normalized(chest)
    ok = (all(s in normalized_inventory for s in player_want) and all(s in normalized_chest for s in chest_want)
          and all(s not in normalized_inventory for s in player_exclude) and all(s not in normalized_chest for s in chest_exclude))
    if predicate: ok = ok and predicate(inventory, equipment)
    results.append({'scenario':name,'ok':ok,'inventory':inventory,'chest':chest,
                    'equipment':equipment, 'expected':{'player':player_want,'chest':chest_want,'playerAbsent':player_exclude,'chestAbsent':chest_exclude}})
    print(name, ok, flush=True)
    (ui.output/report_name).write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
    shots = {
        'Alt+Shift+left stashes backpack and preserves hotbar': 'after-stash',
        'Full chest preserves remainder': 'full-chest-remainder',
        'Sort hovered empty container slot and merge stacks': 'after-sort',
        'Shared Shift drag moves both stacks without duplicating items': 'shared-shift-drag',
        'Container filtered drop includes hotbar with Ctrl': 'after-filtered-drop',
        'Placing last main-hand block refills from backpack': 'main-hand-refilled',
        'Last off-hand torch refills with seven spare torches': 'off-hand-refilled',
        'Death protection consumes and refills the off-hand totem': 'off-hand-totem-refilled',
    }
    if name in shots: ui.capture(shots[name])

def normalized(snbt):
    items = []
    for part in re.split(r'\}, \{', snbt):
        slot = re.search(r'Slot: (-?\d+)b', part)
        item = re.search(r'id: "minecraft:([^"]+)"', part)
        count = re.search(r'[Cc]ount: (\d+)b?',part)
        if slot and item and count: items.append(state(int(slot[1]),item[1],int(count[1])))
    return ','.join(items)

def fixture(*commands):
    for command in ('clear ComfortQA', 'data merge block 0 -60 2 {Items:[]}', *commands):
        server.send(command)
    time.sleep(.35)


def damage(item, amount, different_data=False):
    if game_version == '1.20.1':
        extra = ',RepairCost:1' if different_data else ''
        return f'{item}{{Damage:{amount}{extra}}}'
    extra = ',minecraft:repair_cost=1' if different_data else ''
    return f'{item}[minecraft:damage={amount}{extra}]'


def damage_at(inventory, equipment, slot, field='offhand'):
    if field in equipment and (slot == -106 or slot >= 100):
        item = equipment[field]
    else:
        item = next((part for part in re.split(r'\}, \{', inventory) if f'Slot: {slot}b' in part), '')
    value = re.search(r'(?:Damage|"minecraft:damage"): (\d+)', item)
    return int(value[1]) if value else 0



def test_bindings():
    labels = load('game_labels', 'game-labels.py').GameLabels(ui, ui.manifest['gameJar'])
    server.send('tp ComfortQA 0.5 -60 0.5 0 31')
    time.sleep(.65)
    fixture(container(3,'stone',7),container(11,'stone',6),container(17,'dirt',5))
    ui.mouse(1)
    time.sleep(.45)
    if not ui.inventory_panel():
        ui.capture('failed-binding-chest')
        raise RuntimeError('Bindings fixture chest did not open')
    ui.move(*CHEST)
    press(R)
    evidence('Default R leaves the chest unchanged after JSON binds sort to J', [],
             [state(3,'stone',7),state(11,'stone',6),state(17,'dirt',5)])
    press(0x4a)
    evidence('JSON binding J sorts from an empty slot', [],[state(0,'dirt',5),state(1,'stone',13)])
    press(ESC)
    press(ESC)
    for label in ('Options', 'Controls', 'Key Binds'):
        time.sleep(.2)
        xy = labels.find(label)
        if xy is None:
            ui.capture('failed-menu-' + label.replace(' ', '-'))
            raise RuntimeError(f'Could not locate vanilla menu button {label}')
        button(xy)
    sort_label = None
    for _ in range(20):
        sort_label = labels.find('Sort hovered inventory') or labels.find('key.comfortable_items.sort')
        if sort_label: break
        ui.move(670, 545)
        ui.u.mouse_event(0x800,0,0,-720,0)
        time.sleep(.24)
    if not sort_label:
        ui.capture('failed-key-bind-list')
        raise RuntimeError('The vanilla binding list did not display the sort binding')
    _, y = sort_label
    reset = labels.find('Reset', (500, y-15, ui.r.right, y+15))
    if not reset:
        ui.capture('failed-key-bind-position')
        raise RuntimeError('The sort binding row did not contain a Reset button')
    ui.capture('binding-before')
    button((reset[0]-155,y))
    press(0x4b)
    ui.capture('binding-after')
    for _ in range(4):
        back = labels.find('Back to Game')
        if back:
            button(back)
            break
        done = labels.find('Done')
        if not done: break
        button(done)
    config = json.loads((ui.instance/'config/comfortable-items.json').read_text('utf-8'))
    server.send('tp ComfortQA 0.5 -60 0.5 0 31')
    time.sleep(.65)
    fixture(container(3,'stone',7),container(11,'stone',6),container(17,'dirt',5))
    ui.mouse(1)
    time.sleep(.45)
    if not ui.inventory_panel():
        ui.capture('failed-reopen-chest')
        raise RuntimeError('Could not reopen chest after changing key binds')
    ui.move(*CHEST)
    press(0x4a)
    evidence('Old JSON binding J is inactive after changing it in the UI', [],
             [state(3,'stone',7),state(11,'stone',6),state(17,'dirt',5)])
    press(0x4b)
    evidence('Vanilla key bindings set K, persisted it to JSON and sorted from an empty slot', [],
             [state(0,'dirt',5),state(1,'stone',13)], predicate=lambda inv, eq: config['sortKey']=='key.keyboard.k')
    ui.capture('binding-sorted')
    (ui.output/'binding-persistence.json').write_text(json.dumps({'before':'key.keyboard.j','after':config['sortKey']},indent=2),encoding='utf-8')
    test_drop_edges()



def test_drop_edges():
    fixture(player('inventory.0','stone',4),player('inventory.1','dirt',5))
    for modifier, expected, remaining in ((CTRL, (1,1), (3,4)),(SHIFT, (4,5), (0,0))):
        fixture(player('inventory.0','stone',4),player('inventory.1','dirt',5))
        ui.move(*BAG)
        ui.key(modifier)
        ui.u.mouse_event(2,0,0,0,0)
        try:
            time.sleep(.15)
            ui.move(BAG[0]+32,BAG[1])
        finally:
            ui.u.mouse_event(4,0,0,0,0)
            ui.key(modifier,True)
        player_items = [state(9,'stone',remaining[0]),state(10,'dirt',remaining[1])] if remaining[0] else []
        evidence('Ctrl drag moves one per slot' if modifier == CTRL else 'Shift drag moves one complete stack per slot',
                 player_items,[state(0,'stone',expected[0]),state(1,'dirt',expected[1])],
                 () if remaining[0] else ('minecraft:stone','minecraft:dirt'))
    variant = 'stone{RepairCost:1}' if game_version == '1.20.1' else 'stone[minecraft:repair_cost=1]'
    fixture(player('inventory.0','stone',4),player('inventory.1',variant,5),player('hotbar.0','stone',3),container(0,'stone',2))
    ui.move(*CHEST)
    press(Q,ALT)
    evidence('Strict Alt drop keeps an identical ID with different item data',
             [state(10,'stone',5)],[state(0,'stone',2)],['Slot: 9b','Slot: 0b'])
    fixture(player('inventory.0','stone',4),player('inventory.1',variant,5),player('hotbar.0','stone',3),container(0,'stone',2))
    button(CHEST)
    button((720,445),ALT)
    evidence('Outside Alt drops carried plus matching player stacks and retains the distinct variant',
             [state(10,'stone',5)],[],['Slot: 9b','Slot: 0b'],['minecraft:stone'])
    fixture(player('inventory.0','stone',4),player('hotbar.0','stone',3),container(0,'stone',2),container(1,'dirt',9),container(2,'stone',1))
    button(CHEST)
    button((720,445),ALT,SHIFT,CTRL)
    button(CHEST)
    evidence('Outside full chord drops hotbar and backpack matches and returns original carried stack',
             [],[state(0,'stone',2),state(1,'dirt',9),state(2,'stone',1)],['minecraft:stone'])
    press(ESC)
    server.send('kill @e[type=item]')
    fixture(player('hotbar.0','stone',1),player('inventory.0','stone',4))
    press(Q)
    evidence('An ordinary vanilla drop does not refill the selected slot', [state(9,'stone',4)], [], ['Slot: 0b'])
    fixture(player('hotbar.0','stone',3),player('inventory.0','stone',4))
    ui.key(ALT)
    ui.key(Q)
    try:
        time.sleep(.45)
        server.send(player('inventory.0','stone',6))
        for _ in range(6): ui.key(Q)
        evidence('Holding the drop chord executes one round', [state(9,'stone',6)], [], ['Slot: 0b'])
    finally:
        ui.key(Q,True)
        ui.key(ALT,True)


def test_durability():
    fixture(player('inventory.0', damage('wooden_pickaxe', 13)), player('hotbar.0', damage('wooden_pickaxe', 49)))
    evidence('A main-hand tool at ten remaining durability stays in its slot',
             [state(0,'wooden_pickaxe',1),state(9,'wooden_pickaxe',1)], [],
             predicate=lambda inv, eq: damage_at(inv,eq,0) == 49 and damage_at(inv,eq,9) == 13)
    fixture(player('inventory.0', damage('wooden_pickaxe', 50)), player('hotbar.0', damage('wooden_pickaxe', 51)))
    evidence('A spare below ten leaves the original main-hand tool in place',
             [state(0,'wooden_pickaxe',1),state(9,'wooden_pickaxe',1)], [],
             predicate=lambda inv, eq: damage_at(inv,eq,0) == 51 and damage_at(inv,eq,9) == 50)
    fixture(player('inventory.0', damage('wooden_pickaxe', 49)),player('inventory.1', damage('wooden_pickaxe',13)),
            player('inventory.2', damage('wooden_pickaxe', 1, True)),player('hotbar.0',damage('wooden_pickaxe',50)))
    evidence('At nine, choose the strict spare with most durability and store the original',
             [state(0,'wooden_pickaxe',1),state(9,'wooden_pickaxe',1),state(10,'wooden_pickaxe',1),state(11,'wooden_pickaxe',1)], [],
             predicate=lambda inv, eq: [damage_at(inv,eq,i) for i in (0,9,10,11)] == [13,49,50,1])
    fixture(player('inventory.0', damage('wooden_pickaxe',49)),player('inventory.1', damage('wooden_pickaxe',49)),
            player('hotbar.0',damage('wooden_pickaxe',50)))
    evidence('A ten-point spare is eligible and tied spares prefer the first backpack slot',
             [state(0,'wooden_pickaxe',1),state(9,'wooden_pickaxe',1),state(10,'wooden_pickaxe',1)], [],
             predicate=lambda inv, eq: [damage_at(inv,eq,i) for i in (0,9,10)] == [49,50,49])
    fixture(player('inventory.0', damage('shield',13)),player('weapon.offhand',damage('shield',327)))
    evidence('Off-hand shield uses equipment replacement and stores original',
             [state(-106,'shield',1),state(9,'shield',1)], [],
             predicate=lambda inv, eq: damage_at(inv,eq,-106) == 13 and damage_at(inv,eq,9) == 327)
    fixture(player('inventory.0', damage('wooden_pickaxe',13)),player('weapon.offhand',damage('wooden_pickaxe',50)))
    evidence('Off-hand pickaxe uses tool replacement and stores original',
             [state(-106,'wooden_pickaxe',1),state(9,'wooden_pickaxe',1)], [],
             predicate=lambda inv, eq: damage_at(inv,eq,-106) == 13 and damage_at(inv,eq,9) == 50)
    for field,slot,item,worn in [('feet',100,'iron_boots',186),('legs',101,'iron_leggings',216),
                                ('chest',102,'iron_chestplate',231),('head',103,'iron_helmet',156)]:
        fixture(player('inventory.0', damage(item,13)),player('armor.'+field,damage(item,worn)))
        evidence(f'Armor {field} uses the fitting strict spare and stores original',
                 [state(slot,item,1),state(9,item,1)], [],
                 predicate=lambda inv, eq: damage_at(inv,eq,slot,field) == 13 and damage_at(inv,eq,9) == worn)
    pieces = [('feet',100,'iron_boots',194),('legs',101,'iron_leggings',224),
              ('chest',102,'iron_chestplate',239),('head',103,'iron_helmet',164)]
    spares = [player(f'inventory.{i}', damage(item, 1, True)) for i, (_,_,item,_) in enumerate(pieces)]
    worn = [player('armor.'+field, damage(item, amount)) for field,_,item,amount in pieces]
    fixture(*spares, *worn)
    server.send('effect give ComfortQA minecraft:instant_health 1 4 true')
    server.send('damage ComfortQA 8 minecraft:mob_attack')
    time.sleep(.7)
    evidence('Four broken armor pieces refill by ID once vanilla damages them',
             [state(slot,item,1) for _,slot,item,_ in pieces], [],
             [f'Slot: {i}b' for i in range(9,13)],
             predicate=lambda inv, eq: all(damage_at(inv,eq,slot,field) == 1 for field,slot,_,_ in pieces))
    press(0x45)  # Open the player's inventory so the equipped replacements are visible.
    ui.capture('durability')
    press(ESC)


def test_coexistence():
    fixture(player('inventory.0','stone',8),player('inventory.1','dirt',7),player('hotbar.0','diamond',2))
    for _ in range(3):
        if ui.inventory_panel():
            break
        ui.mouse(1)
        time.sleep(.75)
    if not ui.inventory_panel():
        ui.capture('failed-coexistence-chest')
        raise RuntimeError('Coexistence chest did not open')
    scroll(BAG,True)
    evidence('Shared plain wheel transfers one item exactly once',
             [state(9,'stone',7),state(10,'dirt',7),state(0,'diamond',2)],[state(0,'stone',1)])
    fixture(player('inventory.0','stone',4),player('inventory.1','dirt',5))
    ui.move(*BAG)
    ui.key(SHIFT)
    ui.u.mouse_event(2,0,0,0,0)
    try:
        time.sleep(.15)
        for offset in (12, 24, 32, 35, 37):
            ui.move(BAG[0]+offset,BAG[1])
    finally:
        ui.u.mouse_event(4,0,0,0,0)
        ui.key(SHIFT,True)
    evidence('Shared Shift drag moves both stacks without duplicating items',
             [],[state(0,'stone',4),state(1,'dirt',5)],['minecraft:stone','minecraft:dirt'])
    fixture(player('inventory.0','stone',8),player('inventory.1','dirt',7),player('hotbar.0','diamond',2))
    button(BAG, ALT, SHIFT)
    evidence('Shared Alt+Shift click reaches upstream and includes the hotbar', [],
             [state(0,'diamond',2),state(1,'dirt',7),state(2,'stone',8)],
             ['minecraft:stone','minecraft:dirt','minecraft:diamond'])
    fixture(player('inventory.0','stone',3),player('inventory.1','dirt',7),player('hotbar.0','stone',2),container(0,'stone',2))
    button(BAG, ALT, SHIFT, CTRL, right=True)
    evidence('Exclusive full Ctrl chord matches stone and leaves dirt',
             [state(10,'dirt',7)],[state(0,'stone',7)],['minecraft:stone'],['minecraft:dirt'])
    fixture(player('inventory.0','stone',3),player('inventory.1','dirt',7),player('hotbar.0','stone',2),container(0,'stone',2))
    ui.move(*CHEST); press(Q,ALT,SHIFT,CTRL)
    evidence('Exclusive full drop chord keeps unmatched backpack and container items',
             [state(10,'dirt',7)],[state(0,'stone',2)],['minecraft:stone'])
    fixture(container(3,'stone',7),container(11,'stone',6),container(17,'dirt',5))
    ui.move(*CHEST)
    press(R)
    if game_version in ('1.21.1', '1.21.11'):
        evidence('Shared R is reserved for upstream whose sorter is disabled by default', [],
                 [state(3,'stone',7),state(11,'stone',6),state(17,'dirt',5)])
    else:
        evidence('Exclusive R continues to sort from an empty slot', [],[state(0,'dirt',5),state(1,'stone',13)])
    ui.capture('coexistence')
    press(ESC)
    fixture(player('hotbar.0','stone',1),player('inventory.0','stone',4))
    server.send('kill @e[type=item]')
    server.send('setblock 0 -60 1 air')
    server.send('setblock 0 -60 2 air')
    server.send('tp ComfortQA 0.5 -60 0.5 0 58')
    time.sleep(.35); button((492,375),right=True); time.sleep(.45)
    evidence('Exclusive refill remains available with upstream loaded',[state(0,'stone',4)],[],['Slot: 9b'])


with rcon.Rcon() as server:
    player_list = server.send('list')
    if 'ComfortQA' not in player_list: raise RuntimeError('Test client has not joined the vanilla server')
    server.send('gamerule doMobSpawning false')
    # A trader persisted by the fixture server can intercept the chest interaction.
    for entity in ('wandering_trader', 'trader_llama'):
        server.send(f'execute positioned 0 -60 2 run kill @e[type=minecraft:{entity},distance=..16]')
    server.send('kill @e[type=item]')
    server.send('setblock 0 -60 1 air')
    server.send('setblock 0 -60 2 air')
    server.send('setblock 0 -60 2 chest')
    server.send('tp ComfortQA 0.5 -60 0.5 0 31')
    if options.durability or options.coexist or options.bindings or options.extended:
        if options.durability or options.extended: test_durability()
        if options.coexist: test_coexistence()
        if options.bindings or options.extended: test_bindings()
        raise SystemExit(0 if results and all(case['ok'] for case in results) else 1)
    fixture(player('inventory.0','stone',8),player('inventory.1','dirt',7),player('hotbar.0','diamond',2))
    ui.mouse(1)
    time.sleep(.45)
    ui.capture('container-before')
    if not ui.inventory_panel(): raise RuntimeError('Chest was not opened at the expected coordinates')
    button(BAG, ALT, SHIFT)
    evidence('Alt+Shift+left stashes backpack and preserves hotbar', [state(0,'diamond',2)],
        [state(0,'stone',8),state(1,'dirt',7)], ['minecraft:stone','minecraft:dirt'])
    button(CHEST, ALT, SHIFT)
    evidence('Alt+Shift+left retrieves into backpack', [state(9,'stone',8),state(10,'dirt',7),state(0,'diamond',2)],[],(),['minecraft:stone','minecraft:dirt'])
    fixture(player('inventory.0','stone',3), player('inventory.1','dirt',5), player('hotbar.0','stone',2), container(0,'stone',2))
    button(BAG, ALT, SHIFT, right=True)
    evidence('Matching store excludes hotbar', [state(10,'dirt',5),state(0,'stone',2)],[state(0,'stone',5)],[state(9,'stone',3)],['minecraft:dirt'])
    button(BAG, ALT, SHIFT, CTRL, right=True)
    evidence('Matching store includes hotbar with Ctrl', [state(10,'dirt',5)], [state(0,'stone',7)],[state(0,'stone',2)],['minecraft:dirt'])
    fixture(player('inventory.0','stone',3), container(0,'stone',2), container(1,'dirt',4))
    button(CHEST, ALT, SHIFT, right=True)
    evidence('Matching retrieve uses backpack snapshot', [state(9,'stone',5)],[state(1,'dirt',4)],['minecraft:dirt'],['minecraft:stone'])
    fixture(player('hotbar.0','stone',1), container(0,'stone',2), container(1,'dirt',4))
    button(CHEST, ALT, SHIFT, CTRL, right=True)
    evidence('Matching retrieve includes hotbar with Ctrl', [state(0,'stone',3)],[state(1,'dirt',4)],['minecraft:dirt'],['minecraft:stone'])
    fixture(player('hotbar.0','diamond',2), container(0,'stone',2))
    button(BAG, ALT, SHIFT, CTRL)
    evidence('All store includes hotbar with Ctrl', [], [state(0,'stone',2),state(1,'diamond',2)],['minecraft:diamond'])
    button(CHEST, ALT, SHIFT, CTRL)
    evidence('All retrieve includes hotbar in search range', [state(9,'stone',2),state(10,'diamond',2)], [],(),['minecraft:diamond','minecraft:stone'])
    fixture(player('inventory.0','stone',3),player('inventory.2','stone',4),player('hotbar.0','stone',1),container(0,'stone',2))
    scroll(BAG,False,ALT)
    evidence('Alt wheel backward retrieves all matching into hovered side', [state(9,'stone',5),state(11,'stone',4),state(0,'stone',1)],[],(),['minecraft:stone'])
    scroll(BAG,True,ALT)
    evidence('Alt wheel forward moves all matching out', [],[state(0,'stone',10)],['minecraft:stone'])
    fixture(player('inventory.0','stone',8), *(container(slot,'dirt',64) for slot in range(1,27)),container(0,'stone',62))
    button(BAG, ALT, SHIFT)
    evidence('Full chest preserves remainder', [state(9,'stone',6)],[state(0,'stone',64),state(26,'dirt',64)])
    fixture(player('inventory.0','stone',3),player('inventory.1','stone',5),player('inventory.2','dirt',8),container(2,'stone',4))
    button(BAG)
    button(CHEST, SHIFT)
    evidence('Shift placing a carried stack in an empty slot brings matching stacks from the opposite side',
             [state(11,'dirt',8)],[state(0,'stone',8),state(2,'stone',4)],['minecraft:stone'],['minecraft:dirt'])
    fixture(player('hotbar.0','diamond',3),container(3,'stone',7),container(11,'stone',6),container(17,'dirt',5))
    ui.move(*CHEST); press(R)
    evidence('Sort hovered empty container slot and merge stacks',[state(0,'diamond',3)],[state(0,'dirt',5),state(1,'stone',13)])
    fixture(player('hotbar.0','diamond',3),player('inventory.2','stone',7),player('inventory.9','stone',6),player('inventory.10','dirt',5))
    ui.move(*BAG); press(R)
    evidence('Sort hovered empty backpack slot with hotbar preserved',[state(0,'diamond',3),state(9,'dirt',5),state(10,'stone',13)],[])
    fixture(player('inventory.0','stone',4),player('hotbar.0','stone',3),container(0,'stone',2),container(1,'dirt',9))
    ui.move(*CHEST); press(Q,ALT,SHIFT)
    evidence('Container filtered drop from backpack keeps chest and hotbar',[state(0,'stone',3)], [state(0,'stone',2),state(1,'dirt',9)],[state(9,'stone',4)])
    ui.move(*CHEST); press(Q,ALT,SHIFT,CTRL)
    evidence('Container filtered drop includes hotbar with Ctrl',[], [state(0,'stone',2),state(1,'dirt',9)],['minecraft:stone'])
    fixture(player('inventory.0','stone',4),player('hotbar.0','stone',3),container(0,'stone',2),container(1,'dirt',9))
    ui.move(*CHEST); press(Q,ALT)
    evidence('Alt+drop uses hovered sample and drops player stacks only', [],[state(0,'stone',2),state(1,'dirt',9)],['minecraft:stone'])
    ui.move(*CHEST); press(Q,SHIFT)
    evidence('Shift+drop throws hovered container stack',[],[state(1,'dirt',9)],(),['minecraft:stone'])
    fixture(player('inventory.0','stone',4),player('hotbar.0','stone',3),container(0,'dirt',2),container(1,'stone',9))
    button(CHEST)
    button((720,445), ALT, SHIFT)
    evidence('Outside Alt+Shift keeps carried stack and drops filtered backpack',[state(0,'stone',3)],[state(1,'stone',9)],[state(9,'stone',4)],['minecraft:dirt'])
    button(CHEST)
    evidence('Carried stack can be placed back after outside operation',[state(0,'stone',3)],[state(0,'dirt',2),state(1,'stone',9)])
    fixture(player('inventory.0','stone',4),player('hotbar.0','stone',3),container(0,'stone',2),container(1,'dirt',9))
    button(CHEST); button((720,445), SHIFT)
    evidence('Outside Shift drops carried plus matching player stacks',[],[state(1,'dirt',9)],['minecraft:stone'],['minecraft:stone'])
    ui.capture('container-after')
    press(ESC)
    fixture(player('hotbar.0','stone',3),player('inventory.0','stone',4),player('inventory.1','dirt',7))
    press(Q,ALT)
    evidence('World Alt+drop drops all matching stacks and does not refill',[state(10,'dirt',7)],[],['minecraft:stone'])
    fixture(player('hotbar.0','stone',3),player('inventory.0','stone',4),player('inventory.1','dirt',7))
    press(Q,ALT,SHIFT)
    evidence('World Alt+Shift+drop leaves hotbar',[state(0,'stone',3)],[],['Slot: 9b','Slot: 10b'])
    press(Q,ALT,SHIFT,CTRL)
    evidence('World Ctrl+Alt+Shift+drop includes hotbar',[],[],['minecraft:stone'])
    fixture(player('hotbar.0','stone',1),player('inventory.0','stone',4))
    server.send('kill @e[type=item]')
    time.sleep(.4); button((492,375),right=True)
    evidence('World use on chest opens chest without automatic refill',[state(0,'stone',1),state(9,'stone',4)],[])
    press(ESC)
    server.send('setblock 0 -60 1 air')
    server.send('setblock 0 -60 2 air')
    server.send('tp ComfortQA 0.5 -60 0.5 0 58')
    time.sleep(.35); button((492,375),right=True); time.sleep(.45)
    evidence('Placing last main-hand block refills from backpack',[state(0,'stone',4)],[],['Slot: 9b'])

    fixture(player('weapon.offhand','torch',1),player('inventory.0','torch',7))
    server.send('setblock 0 -60 1 air')
    server.send('tp ComfortQA 0.5 -60 0.5 0 58')
    time.sleep(.35); button((492,375),right=True); time.sleep(.55)
    evidence('Last off-hand torch refills with seven spare torches',[state(-106,'torch',7)],[],['Slot: 9b'])
    fixture(player('weapon.offhand','totem_of_undying'), player('inventory.0','totem_of_undying'))
    server.send('damage ComfortQA 100 minecraft:generic')
    time.sleep(.75)
    evidence('Death protection consumes and refills the off-hand totem',
             [state(-106,'totem_of_undying',1)],[],[state(9,'totem_of_undying',1)])
    server.send('effect clear ComfortQA')
    server.send('effect give ComfortQA instant_health 1 4 true')
    fixture(player('hotbar.0','totem_of_undying'), player('weapon.offhand','totem_of_undying'),
            player('inventory.0','totem_of_undying'))
    server.send('damage ComfortQA 100 minecraft:generic')
    time.sleep(.75)
    evidence('Death protection consumes the main-hand totem first and refills it',
             [state(0,'totem_of_undying',1),state(-106,'totem_of_undying',1)],[],[state(9,'totem_of_undying',1)])
    server.send('effect clear ComfortQA')
    server.send('effect give ComfortQA instant_health 1 4 true')
    fixture(player('weapon.offhand','torch',4),player('inventory.0','torch',7))
    press(0x46)
    evidence('Manual off-hand swap does not refill',[state(0,'torch',4),state(9,'torch',7)],[],['Slot: -106b'])
    damage = '{Damage:58}' if game_version == '1.20.1' else '[minecraft:damage=58]'
    fixture(player('hotbar.0','wooden_pickaxe'+damage),player('inventory.0','wooden_pickaxe'),player('inventory.1','stone_pickaxe'))
    server.send('kill @e[type=item]')
    server.send('setblock 0 -60 1 air')
    server.send('setblock 0 -60 2 stone')
    server.send('tp ComfortQA 0.5 -60 0.5 0 31')
    time.sleep(.45)
    ui.u.mouse_event(2,0,0,0,0); time.sleep(1.7); ui.u.mouse_event(4,0,0,0,0)
    time.sleep(.45)
    evidence('A broken wooden pickaxe refills by ID',[state(0,'wooden_pickaxe',1),state(10,'stone_pickaxe',1)],[],['Slot: 9b'])
    ui.capture('world-after')

if not all(case["ok"] for case in results): raise SystemExit(1)
