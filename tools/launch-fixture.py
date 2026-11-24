"""Launch a built mod JAR in a fresh Windows fixture using installed launcher profiles."""
from pathlib import Path
import argparse, ctypes, ctypes.wintypes as wt, hashlib, json, os, re, subprocess, time, shutil

PORTS = {'1.20.1':36921,'1.21.1':36931,'1.21.11':36941,'26.3':36951}
PROFILES = {
    'fabric-1.20.1':'1.20.1-Fabric',
    'forge-1.20.1':'1.20.1-Forge_47.4.23',
    'fabric-1.21.1':'1.21.1-Fabric 0.19.5',
    'neoforge-1.21.1':'1.21.1-NeoForge_21.1.265',
    'fabric-1.21.11':'1.21.11-Fabric_0.18.4',
    'neoforge-1.21.11':'neoforge-21.11.48',
    'fabric-26.3':'26.3-Fabric_0.19.5',
    'neoforge-26.3':'26.3-NeoForge_26.3.0.115',
}

def include(rules):
    allowed = not rules
    for rule in rules or []:
        if rule.get('features'): continue
        system = rule.get('os',{})
        if system.get('name','windows') != 'windows' or system.get('arch','x86_64') not in ('x86_64','amd64'): continue
        allowed = rule['action']=='allow'
    return allowed

def coordinate_path(name):
    group, artifact, version, *classifier = name.split(':')
    suffix = '-' + classifier[0] if classifier else ''
    return f'{group.replace(".", "/")}/{artifact}/{version}/{artifact}-{version}{suffix}.jar'

def flatten(items):
    for item in items:
        if isinstance(item,str): yield item.strip()
        elif include(item['rules']):
            value=item['value']
            yield from value if isinstance(value,list) else (value,)

def native_window(pid):
    user32=ctypes.windll.user32
    user32.SetProcessDPIAware()
    found=[]
    callback=ctypes.WINFUNCTYPE(wt.BOOL,wt.HWND,wt.LPARAM)
    @callback
    def collect(hwnd, unused):
        owner=wt.DWORD()
        user32.GetWindowThreadProcessId(hwnd,ctypes.byref(owner))
        if owner.value==pid and user32.IsWindowVisible(hwnd):
            title=ctypes.create_unicode_buffer(200)
            user32.GetWindowTextW(hwnd,title,len(title))
            if 'Minecraft' in title.value: found.append(hwnd)
        return True
    user32.EnumWindows(collect,0)
    if len(found)==1: return found[0]
    return None

def launch(target, jar, mc_dir, instance, extra_mods, loopback_port, boot_override=None, assets=None):
    game=target.split('-',1)[1]
    profile=mc_dir/'versions'/PROFILES[target]
    basename=profile.name
    data=json.loads((profile/(basename+'.json')).read_bytes())
    game_profile = profile
    if data.get('inheritsFrom'):
        game_profile = mc_dir/'versions'/data['inheritsFrom']
        base = json.loads((game_profile/(game_profile.name+'.json')).read_bytes())
        args = {kind: [*base['arguments'][kind],*data['arguments'].get(kind,[])] for kind in ('game','jvm')}
        data = {**base,**data,'libraries':[*data['libraries'],*base['libraries']], 'arguments':args}

    library_root=mc_dir/'libraries'
    classpath=[]
    for entry in data['libraries']:
        if not include(entry.get('rules')): continue
        artifact=entry.get('downloads',{}).get('artifact',{}).get('path') or coordinate_path(entry['name'])
        path=library_root/artifact
        if 'natives' in entry and not entry.get('downloads',{}).get('artifact') and not path.is_file(): continue
        if not path.is_file(): raise FileNotFoundError(path)
        classpath.append(str(path))
    game_jar=game_profile/(game_profile.name+'.jar')
    if not game_jar.is_file(): raise FileNotFoundError(game_jar)
    classpath.append(str(game_jar))
    classpath = list(dict.fromkeys(classpath))
    instance.mkdir(parents=True)
    mods=instance/'mods'
    mods.mkdir()
    for mod in [jar,*extra_mods]: shutil.copy2(mod,mods/mod.name)
    options = {'guiScale':2,'fullscreen':'false','pauseOnLostFocus':'false','renderDistance':4,
               'simulationDistance':5,'soundCategory_master':'0.0','onboardAccessibility':'false','lang':'en_us','skipMultiplayerWarning':'true','tutorialStep':'none','maxFps':40}
    (instance/'options.txt').write_text(''.join(f'{k}:{v}\n' for k,v in options.items()),encoding='ascii')
    natives = profile/(basename+'-natives')
    if natives.exists(): shutil.copytree(natives,instance/'natives')
    else: (instance/'natives').mkdir()
    if boot_override:
        config=instance/'config'; config.mkdir()
        (config/'comfortable-items.json').write_text(json.dumps(boot_override),encoding='utf-8')
    values = {'natives_directory':str(instance/'natives'), 'launcher_name':'ComfortableItemsQA', 'launcher_version':'1',
              'classpath':os.pathsep.join(classpath),'library_directory':str(library_root),'classpath_separator':os.pathsep,
              'auth_player_name':'ComfortQA','version_name':basename,'game_directory':str(instance),'assets_root':str(assets or mc_dir/'assets'),
              'assets_index_name':data['assetIndex']['id'],'auth_uuid':'cf54ba8751604378b831d091dbc4f7282' if game=='1.21.1' else 'cf54ba87-5160-4378-b831-d091dbc4f7282',
              'auth_access_token':'0','clientid':'','auth_xuid':'','user_type':'legacy','version_type':'release'}
    def interpolate(text):
        return re.sub(r'\$\{([^}]+)\}',lambda match:values[match[1]],text)
    game_args = list(map(interpolate, flatten(data['arguments']['game'])))
    for option in ('--uuid', '--clientId', '--xuid'):
        if option in game_args:
            index = game_args.index(option)
            del game_args[index:index + 2]
    args=['-Xmx1200M','-Dfile.encoding=UTF-8',*map(interpolate,flatten(data['arguments']['jvm'])),data['mainClass'],
          *game_args,'--width','984','--height','751','--quickPlayMultiplayer',f'127.0.0.1:{loopback_port}']
    arg_path=instance/'launch.args'
    arg_path.write_text(''.join('"'+arg.replace('\\','/').replace('"','\\"')+'"\n' for arg in args),encoding='utf-8')
    log_path=instance/'console.log'
    java_major=data['javaVersion']['majorVersion']
    with log_path.open('wb') as log:
        process=subprocess.Popen([f'C:/Program Files/Java/jdk-{java_major}/bin/java.exe','@'+str(arg_path)],cwd=instance,
            stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
    pid=process.pid
    manifest={'target':target,'profile':basename,'java':java_major,'port':loopback_port,'pid':pid,'gameJar':str(game_jar),
              'mods':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in mods.glob('*.jar')}}
    (instance/'launch.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
    print(json.dumps(manifest),flush=True)
    deadline=time.monotonic()+60
    while time.monotonic()<deadline:
        if process.poll() is not None: raise RuntimeError(f'Client exited with {process.returncode}, see {log_path}')
        window=native_window(pid)
        if window:
            ctypes.windll.user32.MoveWindow(window,-1800,85,1000,790,True)
            return
        time.sleep(.1)
    raise TimeoutError(f'Client window did not appear, see {log_path}')

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('target',choices=PROFILES)
    p.add_argument('jar',type=Path)
    p.add_argument('instance',type=Path)
    p.add_argument('--mc-dir',type=Path,required=True)
    p.add_argument('--with-mod',type=Path,action='append',default=[])
    p.add_argument('--boot-config',type=json.loads)
    p.add_argument('--assets-dir',type=Path)
    a=p.parse_args()
    launch(a.target,a.jar.resolve(),a.mc_dir.resolve(),a.instance.resolve(),a.with_mod,PORTS[a.target.split('-',1)[1]],a.boot_config,a.assets_dir)
