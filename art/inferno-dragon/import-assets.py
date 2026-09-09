"""Import pinned original card art and base Inferno Dragon cues; no synthetic cues.

Sources remain Supercell's game assets. This script neither executes downloads
nor distributes the upstream archives. Every delivered OGG is fully decoded.
"""
from pathlib import Path
import json, hashlib, urllib.request, urllib.parse, subprocess, re, shutil
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[1]
ASSETS=ROOT/'src/main/resources/assets/royalespells'
CACHE=ROOT.parent/'audio/original-inferno-audio';CACHE.mkdir(parents=True,exist_ok=True)
FFMPEG=ROOT.parent/'video-audio/vendor/imageio_ffmpeg/binaries/ffmpeg-win-x86_64-v7.1.exe'
SFX='c2d7d67271113cb9fe3ad896d9d03dd7f49eed52'
ART='b4530a1043b213ee2baf9c50a3d0d7fae22c2313'
manifest=[]
def fetch(url,name):
    p=CACHE/name
    if not p.exists():
        req=urllib.request.Request(url,headers={'User-Agent':'RoyaleSpells-asset-import'})
        with urllib.request.urlopen(req,timeout=45) as r:p.write_bytes(r.read())
    return p
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
audio=json.loads((ASSETS/'spell_audio.json').read_text())
sounds=json.loads((ASSETS/'sounds.json').read_text())
audio['inferno_dragon']={}
for phase,file,volume in [('deploy','babydragon_deploy_01.ogg',.43),('wing','babydragon_wing_01.ogg',.085),('beam','inferno_dragon_fireloop_01.ogg',.34)]:
    url=f'https://raw.githubusercontent.com/Henrylq/Clash-Royale-SFX/{SFX}/'+urllib.parse.quote('Cards/Inferno Dragon/'+file)
    src=fetch(url,file);dst=ASSETS/'sounds/original'/('inferno_dragon_'+phase+'.ogg');dst.parent.mkdir(parents=True,exist_ok=True)
    probe=subprocess.run([str(FFMPEG),'-hide_banner','-i',str(src),'-f','null','-'],capture_output=True,text=True)
    if probe.returncode:raise RuntimeError(probe.stderr)
    if 'stereo' in probe.stderr:
        subprocess.run([str(FFMPEG),'-y','-i',str(src),'-ac','1','-c:a','libvorbis','-q:a','5',str(dst)],check=True,capture_output=True)
    else:shutil.copyfile(src,dst)
    subprocess.run([str(FFMPEG),'-v','error','-i',str(dst),'-f','null','-'],check=True,capture_output=True)
    event='inferno_dragon_'+phase
    audio['inferno_dragon'][phase]={'event':event,'volume':volume}
    sounds[event]={'subtitle':'subtitles.royalespells.'+event,'sounds':[{'name':'royalespells:original/'+event,'attenuation_distance':64,'preload':True}]}
    manifest.append({'phase':phase,'source_url':url,'source_sha256':digest(src),'output':str(dst.relative_to(ROOT)).replace('\\','/'),'output_sha256':digest(dst),'probe':probe.stderr})
url=f'https://raw.githubusercontent.com/RoyaleAPI/cr-api-assets/{ART}/cards/inferno-dragon.png'
card=fetch(url,'inferno-dragon.png')
assert card.read_bytes()[:8]==b'\x89PNG\r\n\x1a\n'
for rel in ['textures/card/inferno_dragon.png','textures/gui/spell_icons/inferno_dragon.png']:
    shutil.copyfile(card,ASSETS/rel)
manifest.append({'asset':'card','source_url':url,'sha256':digest(card)})
model=json.loads((ASSETS/'models/item/barbarian_hut.json').read_text())
(ASSETS/'models/item/inferno_dragon.json').write_text(json.dumps(model,indent=2)+'\n')
for name,data in [('spell_audio.json',audio),('sounds.json',sounds)]:
    (ASSETS/name).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
(HERE/'asset-sources.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Imported original Inferno Dragon card, deploy, quiet wingbeat and looping beam; decoded all OGG files.')
