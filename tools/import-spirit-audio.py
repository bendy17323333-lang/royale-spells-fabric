"""Import original per-spirit cues from a pinned classified game-audio archive.

Preserves all other spell profiles. Mono audio is necessary for Minecraft's
positional attenuation; no trimming, speed changes or pitch shifting are used.
"""
from pathlib import Path
import concurrent.futures, hashlib, json, subprocess, urllib.parse, urllib.request

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/royalespells'
COMMIT='c2d7d67271113cb9fe3ad896d9d03dd7f49eed52'
FFMPEG=ROOT.parent/'video-audio/vendor/imageio_ffmpeg/binaries/ffmpeg-win-x86_64-v7.1.exe'
CACHE=ROOT.parent/'audio/original-spirit-audio'
VOLUMES={'deploy':.46,'jump':.38,'impact':.48,'step':.075}
PROFILES={
 'fire': {'deploy':['fire_spirit_deploy_01','fire_spirit_deploy_02'], 'jump':['fire_spirit_attack_01'], 'impact':['sound_fireball_hit_01'], 'step':['fire_spirit_step_sfx_01']},
 'ice': {'deploy':['ice_sp_deploy_01','ice_sp_deploy_02'], 'jump':['fire_spirit_attack_01'], 'impact':['ice_mage_impact_02'], 'step':['fire_spirit_step_sfx_01']},
 'electro': {'deploy':['electro_spirit_dep_01'], 'jump':['elec_spirit_atk_vo_01','elec_spirit_atk_vo_02'], 'impact':['tesla_zap_01'], 'step':['elec_spirit_step_01','elec_spirit_step_02']},
 'heal': {'deploy':['heal_spirit_deploy_vo_01_dl'], 'jump':['fire_spirit_step_vo_01','fire_spirit_step_vo_02','fire_spirit_step_vo_03'], 'impact':['heal_magic_03'], 'step':['fire_spirit_step_sfx_01']}
}

def import_clip(key):
 kind,stem=key
 path=f'Cards/{kind.title()} Spirit/{stem}.ogg'
 url=f'https://raw.githubusercontent.com/Henrylq/Clash-Royale-SFX/{COMMIT}/'+urllib.parse.quote(path)
 source=CACHE/kind/(stem+'.ogg');source.parent.mkdir(parents=True,exist_ok=True)
 if not source.exists():
  request=urllib.request.Request(url,headers={'User-Agent':'RoyaleSpellsAssetImport'})
  source.write_bytes(urllib.request.urlopen(request,timeout=30).read())
 data=source.read_bytes();assert data.startswith(b'OggS')
 channels=data[data.index(b'\x01vorbis')+11]
 destination=ASSETS/'sounds/spirits'/kind/(stem+'.ogg');destination.parent.mkdir(parents=True,exist_ok=True)
 if channels==1:destination.write_bytes(data)
 else:subprocess.run([str(FFMPEG),'-hide_banner','-loglevel','error','-y','-i',str(source),'-ac','1','-c:a','libvorbis','-q:a','5',str(destination)],check=True)
 # Decode the entire clip as well as checking its container/header.
 subprocess.run([str(FFMPEG),'-hide_banner','-loglevel','error','-i',str(destination),'-f','null','-'],check=True)
 mono=destination.read_bytes();assert mono[mono.index(b'\x01vorbis')+11]==1
 return dict(path=destination.relative_to(ROOT).as_posix(),classification=path,url=url,original_sha256=hashlib.sha256(data).hexdigest(),sha256=hashlib.sha256(mono).hexdigest(),bytes=len(mono),channels=1,processing='Original bytes' if channels==1 else 'Stereo to mono only; no speed/pitch/trim change')

def main():
 jobs=[(kind,stem) for kind,phases in PROFILES.items() for stem in sorted({s for stems in phases.values() for s in stems})]
 with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:records=list(pool.map(import_clip,jobs))
 sounds=json.loads((ASSETS/'sounds.json').read_text(encoding='utf-8'))
 profiles=json.loads((ASSETS/'spell_audio.json').read_text(encoding='utf-8'))
 for kind,phases in PROFILES.items():
  profiles['spirit_'+kind]={}
  for phase,stems in phases.items():
   event=f'spirit_{kind}_{phase}'
   sounds[event]=dict(subtitle='subtitle.royalespells.'+event,sounds=[dict(name=f'royalespells:spirits/{kind}/{s}',attenuation_distance=64,preload=True) for s in stems])
   profiles['spirit_'+kind][phase]=dict(event=event,volume=.28 if kind=='electro' and phase=='impact' else VOLUMES[phase])
 for filename,data in [('sounds.json',sounds),('spell_audio.json',profiles)]:
  (ASSETS/filename).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 for lang in ['zh_cn','en_us']:
  path=ASSETS/'lang'/f'{lang}.json';data=json.loads(path.read_text(encoding='utf-8'))
  data['ui.royalespells.spirit_damage']='单体伤害：%s（随法术/召唤强度提升）' if lang=='zh_cn' else 'Damage per target: %s (scales with spell/summon power)'
  for kind in PROFILES:
   name={'fire':'烈焰精灵','ice':'冰雪精灵','electro':'电击精灵','heal':'治疗精灵'}[kind] if lang=='zh_cn' else kind.title()+' Spirit'
   for phase in VOLUMES:
    action=({'deploy':'登场','jump':'跳扑','impact':'命中','step':'脚步'} if lang=='zh_cn' else {'deploy':'deploys','jump':'leaps','impact':'hits','step':'steps'})[phase]
    data[f'subtitle.royalespells.spirit_{kind}_{phase}']=name+'：'+action if lang=='zh_cn' else name+' '+action
  path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 manifest=dict(source_commit=COMMIT,source_repository='https://github.com/Henrylq/Clash-Royale-SFX',original_rights='Clash Royale / Supercell',profiles=PROFILES,files=records,notes={
  'shared_cues':'Ice uses the fire-named attack and footstep files classified under Ice Spirit in the game-audio archive. Heal uses the shared spirit movement voices classified under Heal Spirit for its leap; no invented exclusive heal attack recording is claimed.',
  'timing':'Deploy once after spawn, not on reload; leap once on commitment; impact on actual burst/each electro chain link; quiet steps at least 10 ticks apart, never attack sounds used as footsteps.',
  'range':'64-block fixed server delivery and positional mono attenuation, with 0.9 global spell volume scaling.',
  'omitted':'dummy_01 is not used; no dedicated spirit death recording was identified in these classified folders.'})
 (ROOT/'SPIRIT-AUDIO-SOURCES.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print(f'Imported and fully decoded {len(records)} mono clips for 16 original spirit sound events.')

if __name__=='__main__':main()
