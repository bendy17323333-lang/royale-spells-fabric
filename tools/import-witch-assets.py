"""Import pinned, original Witch assets; mono conversion preserves pitch/speed.

Run with --ffmpeg PATH. Only witch/evolved_witch sound events are added.
Third-party art and recordings remain excluded from this project's MIT grant.
"""
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import argparse, hashlib, json, re, shutil, struct, subprocess, urllib.request

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources/assets/royalespells'
RAW = ROOT / 'art/witch/original-audio'
RAW.mkdir(parents=True, exist_ok=True)
parser = argparse.ArgumentParser()
parser.add_argument('--ffmpeg', required=True)
args = parser.parse_args()
CARD_REV = 'b4530a1043b213ee2baf9c50a3d0d7fae22c2313'
AUDIO_REV = 'c2d7d67271113cb9fe3ad896d9d03dd7f49eed52'
CARD_BASE = f'https://raw.githubusercontent.com/RoyaleAPI/cr-api-assets/{CARD_REV}/cards/'
AUDIO_BASE = f'https://raw.githubusercontent.com/Henrylq/Clash-Royale-SFX/{AUDIO_REV}/Cards/Witch/'
audio_names = [
    'summon_witch_01', 'witch_attack_02', 'witch_summon_02', 'npc_die_02',
    'card_evolution_witch_attack', 'card_evolution_witch_attack_hit',
    'card_evolution_witch_deploy_sfx', 'card_evolution_witch_deploy_vo_01',
    'card_evolution_witch_deploy_vo_02', 'card_evolution_witch_healing',
    'card_evolution_witch_skill_skeleton_01', 'card_evolution_witch_skill_skeleton_02',
]
def sha(data): return hashlib.sha256(data).hexdigest()
def get(url):
    req = urllib.request.Request(url, headers={'User-Agent':'RoyaleSpells-AssetImport/1.9'})
    return urllib.request.urlopen(req, timeout=45).read()
def fetch_audio(name):
    url = AUDIO_BASE + name + '.ogg'
    raw = RAW / (name + '.ogg')
    if not raw.exists(): raw.write_bytes(get(url))
    data = raw.read_bytes()
    start = data.index(b'\x01vorbis')
    channels = data[start + 11]
    rate = struct.unpack_from('<I', data, start + 12)[0]
    target = RES / 'sounds/witch' / raw.name
    target.parent.mkdir(parents=True, exist_ok=True)
    if channels == 1:
        shutil.copyfile(raw, target)
        operation = 'verbatim original mono OGG'
    else:
        subprocess.run([args.ffmpeg,'-hide_banner','-loglevel','error','-y','-i',str(raw),
                        '-ac','1','-c:a','libvorbis','-q:a','5',str(target)], check=True)
        operation = 'stereo to mono Vorbis q5 only; no pitch, speed, trim or loudness change'
    check = subprocess.run([args.ffmpeg,'-hide_banner','-i',str(target),'-f','null','-'],
                           capture_output=True, text=True)
    if check.returncode != 0: raise RuntimeError(check.stderr)
    duration = re.search(r'Duration:\s*([\d:.]+)', check.stderr)
    return {'kind':'audio','path':target.relative_to(ROOT).as_posix(),'source_url':url,
            'original_sha256':sha(data),'sha256':sha(target.read_bytes()),
            'original_channels':channels,'channels':1,'sample_rate':rate,
            'duration':duration.group(1) if duration else None,'operation':operation}

records = []
for source_name, item_id in [('witch','witch'),('witch-ev1','witch_evolution')]:
    url = CARD_BASE + source_name + '.png'
    data = get(url)
    target = RES/'textures/card'/f'{item_id}.png'
    target.write_bytes(data)
    width, height = struct.unpack_from('>II', data,16)
    records.append({'kind':'card','path':target.relative_to(ROOT).as_posix(),
                    'source_url':url,'sha256':sha(data),'width':width,'height':height,
                    'operation':'original bytes; not cropped, recolored or resized'})
with ThreadPoolExecutor(max_workers=4) as pool:
    records += list(pool.map(fetch_audio, audio_names))

events = {
 'witch_deploy':['summon_witch_01'],
 'witch_attack':['witch_attack_02'],
 'witch_impact':['card_evolution_witch_attack_hit'],
 'witch_summon':['witch_summon_02'],
 'witch_heal':['card_evolution_witch_healing'],
 'witch_death':['npc_die_02'],
 'evolved_witch_deploy':['card_evolution_witch_deploy_sfx'],
 'evolved_witch_deploy_voice':['card_evolution_witch_deploy_vo_01','card_evolution_witch_deploy_vo_02'],
 'evolved_witch_attack':['card_evolution_witch_attack'],
 'evolved_witch_impact':['card_evolution_witch_attack_hit'],
 'evolved_witch_summon':['card_evolution_witch_skill_skeleton_01','card_evolution_witch_skill_skeleton_02'],
}
sounds_path=RES/'sounds.json'
sounds=json.loads(sounds_path.read_text(encoding='utf-8-sig'))
for event,names in events.items():
    sounds[event]={'subtitle':f'subtitles.royalespells.{event}',
                   'sounds':[{'name':'royalespells:witch/'+name,'preload':True,'attenuation_distance':48,
                              **({'volume':.55} if event=='witch_impact' else {})} for name in names]}
sounds_path.write_text(json.dumps(sounds,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
manifest={'version':'1.9.0','retrieved_on':'2026-10-10','rights':'Supercell; third-party assets excluded from MIT',
          'card_repository_revision':CARD_REV,'audio_repository_revision':AUDIO_REV,
          'audio_notes':['Original archive contains no separately labelled normal Witch impact; normal impact intentionally reuses the original evolved Witch impact at reduced runtime gain (0.55).',
                         'npc_die_02 is a shared game death cue, not a Witch-exclusive voice.',
                         'No dedicated hurt voice was found; no replacement Minecraft witch/zombie sound is substituted.',
                         'Deployment sound and voice are distinct events; a random sound list is not used to pretend simultaneous layering.'],
          'events':events,'files':records}
(ROOT/'WITCH-ASSET-SOURCES.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'cards':2,'audio':len(audio_names),'events':len(events),'validated':True},ensure_ascii=False))
