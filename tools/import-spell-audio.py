"""Fetch pinned original game sounds, preserving sources and downmixing stereo for 3D audio."""
from pathlib import Path
import argparse, concurrent.futures, hashlib, json, shutil, struct, subprocess, urllib.parse, urllib.request

repo=Path(__file__).resolve().parents[1]
work=repo.parent
commit='c2d7d67271113cb9fe3ad896d9d03dd7f49eed52'
index=work/'audio'/('classified-sfx-tree-'+commit+'.json')
index.parent.mkdir(parents=True,exist_ok=True)
if not index.exists():
 request=urllib.request.Request('https://api.github.com/repos/Henrylq/Clash-Royale-SFX/git/trees/'+commit+'?recursive=1',headers={'User-Agent':'RoyaleSpellsAssetImport'})
 index.write_bytes(urllib.request.urlopen(request).read())
tree=json.loads(index.read_text(encoding='utf-8'))['tree']
available={Path(e['path']).name:e['path'] for e in tree if e['path'].endswith('.ogg') and e['path'].startswith('Cards/')}
def cue(file,volume=.7):return {'file':file,'volume':volume}
profiles={
 'arrows':{'deploy':cue('archer_queen_attack_02',.5),'strike':cue('arrow_norm_impact_06',.7)},
 'fireball':{'deploy':cue('fire_ball_02',.65),'impact':cue('fire_ball_explo_02',.85)},
 'zap':{'deploy':cue('zap_02',.7)},
 'zap_evolution':{'deploy':cue('zap_02',.7),'strike':cue('evo_zap_01',.7)},
 'lightning':{'deploy':cue('lightning_02',.9)},
 'rocket':{'deploy':cue('rocket_launch_02v2',.75),'impact':cue('rocket_hit_01v2',1)},
 'poison':{'deploy':cue('poison_spell_01',.65)},
 'freeze':{'deploy':cue('freeze_04',.75)},
 'rage':{'deploy':cue('rage_spell_01',.65)},
 'the_log':{'deploy':cue('log_vocal_01',.65),'travel':cue('logroll01',.45),'hit':cue('log_impact_01',.55)},
 'tornado':{'deploy':cue('royale_tornado_01',.7)},
 'earthquake':{'deploy':cue('scrollearthquake01',.7),'strike':cue('boulder_impact_01',.3)},
 'giant_snowball':{'deploy':cue('throw_snowball_01',.6),'impact':cue('snowball_impact_01',.8)},
 'giant_snowball_evolution':{'deploy':cue('evo_snowball_deploy_01',.65),'impact':cue('evo_snowball_land_01',.75),'capture':cue('evo_snowball_capture_01',.5)},
 'goblin_barrel':{'deploy':cue('barrel_drawback_14',.7),'impact':cue('barrel_explosion_02',.7)},
 'goblin_barrel_evolution':{'deploy':cue('evo_gob_barrel_dep_01',.7),'impact':cue('barrel_explosion_02',.7)},
 'barbarian_barrel':{'deploy':cue('barbarrel_01',.7),'impact':cue('log_impact_01',.65)},
 'barbarian_barrel_hero':{'deploy':cue('card_hero_barbarian_barrel_deploy',.7),'reroll':cue('card_hero_barbarian_barrel_ability_activated',.65),'impact':cue('card_hero_barbarian_barrel_crash',.75),'hit':cue('card_hero_barbarian_barrel_hit',.5)},
 'royal_delivery':{'deploy':cue('royal_recruit_whistle_01',.7),'impact':cue('barrel_explosion_02',.7),'summon':cue('royal_recruit_deploy_01',.65)},
 'graveyard':{'deploy':dict(file='cemetary_deploy_01',volume=1,event='graveyard_deploy')},
 'clone':{'deploy':cue('clone_spell_01',.65)},
 'mirror':{'deploy':cue('mirror_02_dl',.65)},
 'goblin_curse':{'deploy':cue('goblin_curse_spell_02',.6),'transform':cue('gob_spell_transformation_01',.65),'end':cue('gob_spell_end_01',.5)},
 'void':{'deploy':cue('dark_void_spell_01',.7)},
 'vines':{'deploy':cue('card_epic_vines_cast',.65),'hit':cue('card_epic_vines_debuff',.45),'end':cue('card_epic_vines_end',.5)},
 'heal':{'deploy':cue('heal_magic_03',.65)},
 'warmth':{'deploy':cue('heal_magic_03',.5)},
 'party_rocket':{'deploy':cue('rocket_launch_02v2',.65),'impact':cue('rocket_hit_01v2',.65),'party':cue('gob_party_hut_deploy_01',.5)},
 'barbarian_hut':{'deploy':cue('building_place_01',.65),'end':cue('boulder_impact_01',.55)}
}
assets=repo/'src/main/resources/assets/royalespells'
cache=work/'audio/original-spell-audio';cache.mkdir(exist_ok=True)
dest=assets/'sounds/original';dest.mkdir(exist_ok=True)
bundled=work/'video-audio/vendor/imageio_ffmpeg/binaries/ffmpeg-win-x86_64-v7.1.exe'
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--ffmpeg',default=str(bundled) if bundled.exists() else shutil.which('ffmpeg'))
options=parser.parse_args()
if not options.ffmpeg:parser.error('Install FFmpeg or pass --ffmpeg <executable> for stereo-to-mono conversion.')
ffmpeg=options.ffmpeg
def fetch(stem):
 name=stem+'.ogg';path=available[name];url=f'https://raw.githubusercontent.com/Henrylq/Clash-Royale-SFX/{commit}/'+urllib.parse.quote(path)
 original=cache/name
 if not original.exists():original.write_bytes(urllib.request.urlopen(url).read())
 data=original.read_bytes();assert data.startswith(b'OggS');marker=data.index(b'\x01vorbis');channels=data[marker+11]
 target=dest/name
 if channels>1:
  subprocess.run([str(ffmpeg),'-hide_banner','-loglevel','error','-y','-i',str(original),'-ac','1','-c:a','libvorbis','-q:a','5',str(target)],check=True)
 else:shutil.copy2(original,target)
 return {'file':name,'classification':path,'url':url,'original_sha256':hashlib.sha256(data).hexdigest(),'installed_sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'processing':'stereo to mono for positional playback; no time or pitch change' if channels>1 else 'original bytes unchanged'}
files=sorted({c['file'] for p in profiles.values() for c in p.values()})
with concurrent.futures.ThreadPoolExecutor(max_workers=5) as pool:records=list(pool.map(fetch,files))
sounds=json.loads((assets/'sounds.json').read_text(encoding='utf-8'))
for spell,phases in profiles.items():
 for phase,c in phases.items():
  event=c.setdefault('event','spell_'+spell+'_'+phase)
  if event=='graveyard_deploy':continue
  sounds[event]={'subtitle':'item.royalespells.'+spell,'sounds':[{'name':'royalespells:original/'+c['file'],'attenuation_distance':20,'preload':True}]}
(assets/'sounds.json').write_text(json.dumps(sounds,indent=2)+'\n',encoding='utf-8')
(assets/'spell_audio.json').write_text(json.dumps(profiles,indent=2)+'\n',encoding='utf-8')
manifest={'source_commit':commit,'profiles':profiles,'files':records,'notes':{
 'warmth':'No separately classified Warmth clip found in the public archives; uses the original heal_magic_03 game asset as a documented substitute.',
 'party_rocket':'Rocket launch/impact plus the original Party Hut celebration clip; not claimed as an independently verified Party Rocket-only track.',
 'heal':'Original heal_magic_03 is currently classified under Heal Spirit.',
 'barbarian_hut':'Uses the original shared building placement sound, plus existing original Barbarian voice assets.',
 'graveyard':'Existing graveyard_deploy event and original 1.0.1 audio are retained.'}}
(repo/'SPELL-AUDIO-SOURCES.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('IMPORTED',len(files),'original assets;',len(profiles),'card profiles;',sum(len(p) for p in profiles.values()),'cues')
