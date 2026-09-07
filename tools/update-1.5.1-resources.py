from pathlib import Path
import json, urllib.request, subprocess, hashlib
r=Path(__file__).resolve().parents[1]; a=r/'src/main/resources/assets/royalespells'
url='https://raw.githubusercontent.com/Henrylq/Clash-Royale-SFX/c2d7d67271113cb9fe3ad896d9d03dd7f49eed52/Cards/Guards/shield_skele_lost_02.ogg'
source=r.parent/'audio/army-1.5/shield_skele_lost_02.ogg'
if not source.exists():source.write_bytes(urllib.request.urlopen(url,timeout=45).read())
dest=a/'sounds/original/shield_skele_lost_02.ogg'
ff=r.parent/'video-audio/vendor/imageio_ffmpeg/binaries/ffmpeg-win-x86_64-v7.1.exe'
subprocess.run([str(ff),'-y','-loglevel','error','-i',str(source),'-ac','1','-c:a','libvorbis','-q:a','5',str(dest)],check=True)
sounds=json.loads((a/'sounds.json').read_text('utf-8'))
sounds['army_shield_break']={'subtitle':'subtitles.royalespells.army_shield_break','sounds':[{'name':'royalespells:original/shield_skele_lost_02','stream':False,'attenuation_distance':32}]}
(a/'sounds.json').write_text(json.dumps(sounds,ensure_ascii=False,indent=2)+'\n','utf-8')
records=json.loads((r/'ARMY-ASSET-SOURCES.json').read_text('utf-8'));records=[e for e in records if e.get('event')!='army_shield_break']
records.append({'event':'army_shield_break','url':url,'note':'Original Clash Royale skeletal shield-loss cue (Guards archive). No separate General shield-break filename is present in the pinned archive.','source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'mono_sha256':hashlib.sha256(dest.read_bytes()).hexdigest()})
(r/'ARMY-ASSET-SOURCES.json').write_text(json.dumps(records,indent=2)+'\n','utf-8')
for lang in ['en_us','zh_cn']:
 p=a/'lang'/f'{lang}.json';data=json.loads(p.read_text('utf-8'));zh=lang=='zh_cn'
 data['item.royalespells.neutral_skeleton_army_spawn_egg']='独立觉醒骷髅军团刷怪蛋' if zh else 'Independent Evolved Skeleton Army Spawn Egg'
 data['message.royalespells.control_cooldown']='冰冻与藤蔓共享冷却，暂时无法施放' if zh else 'Freeze and Vines share a cooldown. Please wait.'
 data['subtitles.royalespells.army_shield_break']='骷髅将军：护盾碎裂' if zh else 'Skeleton General: Shield shatters'
 p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n','utf-8')
(a/'models/item/neutral_skeleton_army_spawn_egg.json').write_text('{"parent":"minecraft:item/template_spawn_egg"}\n','utf-8')
print('Imported original skeleton shield-loss cue, localized independent army egg, and shared control cooldown')
