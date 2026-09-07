"""Import original Skeleton Army evolution cues and native card art from pinned archives."""
from pathlib import Path
import json,hashlib,urllib.request,urllib.parse,subprocess
r=Path(__file__).resolve().parents[1];w=r.parent
pin='c2d7d67271113cb9fe3ad896d9d03dd7f49eed52'
tree=json.loads((w/'audio/classified-sfx-tree-current.json').read_text(encoding='utf-8'))['tree']
paths={Path(e['path']).stem:e['path'] for e in tree if e['path'].startswith('Cards/') and e['path'].endswith('.ogg')}
cues={'army_deploy':'card_evolution_skeleton_army_deploy','army_horn':'card_evolution_skeleton_army_change_horn','army_change':'card_evolution_skeleton_army_change','army_death':'card_evolution_skeleton_army_death_vo','skeleton_step':'skeleton_step_02','skeleton_attack':'skeleton_atk_03','skeleton_death':'npc_die_02'}
assets=r/'src/main/resources/assets/royalespells';cache=w/'audio/army-1.5';cache.mkdir(parents=True,exist_ok=True)
sounds=json.loads((assets/'sounds.json').read_text(encoding='utf-8'));records=[]
ff=w/'video-audio/vendor/imageio_ffmpeg/binaries/ffmpeg-win-x86_64-v7.1.exe'
for event,name in cues.items():
 url=f'https://raw.githubusercontent.com/Henrylq/Clash-Royale-SFX/{pin}/'+urllib.parse.quote(paths[name]);source=cache/(name+'.ogg')
 if not source.exists():source.write_bytes(urllib.request.urlopen(url,timeout=45).read())
 dst=assets/'sounds/original'/source.name
 subprocess.run([str(ff),'-y','-loglevel','error','-i',str(source),'-ac','1','-c:a','libvorbis','-q:a','5',str(dst)],check=True)
 sounds[event]={'subtitle':'subtitles.royalespells.'+event,'sounds':[{'name':'royalespells:original/'+name,'stream':False}]}
 records.append({'event':event,'url':url,'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'mono_sha256':hashlib.sha256(dst.read_bytes()).hexdigest()})
(assets/'sounds.json').write_text(json.dumps(sounds,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
cardpin='b4530a1043b213ee2baf9c50a3d0d7fae22c2313';url=f'https://raw.githubusercontent.com/RoyaleAPI/cr-api-assets/{cardpin}/cards/skeleton-army-ev1.png'
card=urllib.request.urlopen(url,timeout=45).read();assert card.startswith(b'\x89PNG');(assets/'textures/gui/spell_icons/skeleton_army_evolution.png').write_bytes(card)
records.append({'card_url':url,'sha256':hashlib.sha256(card).hexdigest()})
(r/'ARMY-ASSET-SOURCES.json').write_text(json.dumps(records,indent=2)+'\n',encoding='utf-8')
print('Imported',len(cues),'original cues and original evolved Skeleton Army icon')
