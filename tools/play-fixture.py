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
options = parser.parse_args()
results = []
report_name = 'coexist.json' if options.coexist else 'durability.json' if options.durability else 'playtest.json'
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
        if options.durability: equipment_slots.update(feet=100, legs=101, chest=102, head=103)
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
    ui.capture('durability')


def test_coexistence():
    fixture(player('inventory.0','stone',8),player('inventory.1','dirt',7),player('hotbar.0','diamond',2))
    ui.mouse(1)
    time.sleep(.45)
    if not ui.inventory_panel(): raise RuntimeError('Coexistence chest did not open')
    scroll(BAG,True)
    evidence('Shared plain wheel transfers one item exactly once',
             [state(9,'stone',7),state(10,'dirt',7),state(0,'diamond',2)],[state(0,'stone',1)])
    fixture(player('inventory.0','stone',8),player('inventory.1','dirt',7),player('hotbar.0','diamond',2))
    button(BAG, ALT, SHIFT)
    evidence('Shared Alt+Shift click reaches upstream bulk transfer', [],[state(0,'stone',8),state(1,'dirt',7)],
             ['minecraft:stone','minecraft:dirt'])
    fixture(player('inventory.0','stone',3),player('inventory.1','dirt',7),player('hotbar.0','stone',2),container(0,'stone',2))
    button(BAG, ALT, SHIFT, CTRL, right=True)
    evidence('Exclusive full Ctrl chord matches stone and leaves dirt',
             [state(10,'dirt',7)],[state(0,'stone',7)],['minecraft:stone'],['minecraft:dirt'])
    fixture(player('inventory.0','stone',3),player('inventory.1','dirt',7),player('hotbar.0','stone',2),container(0,'stone',2))
    ui.move(*CHEST); press(Q,ALT,SHIFT,CTRL)
    evidence('Exclusive full drop chord keeps unmatched backpack and container items',
             [state(10,'dirt',7)],[state(0,'stone',2)],['minecraft:stone'])
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
    server.send('kill @e[type=item]')
    server.send('setblock 0 -60 1 air')
    server.send('setblock 0 -60 2 air')
    server.send('setblock 0 -60 2 chest')
    server.send('tp ComfortQA 0.5 -60 0.5 0 31')
    if options.durability or options.coexist:
        if options.durability: test_durability()
        if options.coexist: test_coexistence()
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
