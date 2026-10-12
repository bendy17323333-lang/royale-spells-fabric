"""Restore Void's original single-target impact layer, with auditable inputs.

Original game path (Cr_Unpacked 59ef8701): DarkMagicAOE maxUnits=1 ->
PaoloFirstEffectEverStrong -> 'Fire ball explo' -> fire_ball_explo_02.ogg,
plus Paolo_zap_effect3 -> sound_spell_zap -> zap_02.ogg.
Minecraft sounds.json alternatives are RANDOM choices, not layers, so the two
unaltered-pitch source samples are mixed offline into one mono cue.
"""
from pathlib import Path
import argparse, hashlib, json, subprocess

parser=argparse.ArgumentParser();parser.add_argument('--ffmpeg',required=True);args=parser.parse_args()
root=Path(__file__).resolve().parents[1];a=root/'src/main/resources/assets/royalespells';s=a/'sounds/original'
zap=s/'zap_02.ogg';blast=s/'fire_ball_explo_02.ogg';output=s/'void_single_strike.ogg'
# Keep the original 80:55 sound-event volume relationship. Headroom is constant,
# never compression or normalization which changes the impact envelope.
subprocess.run([args.ffmpeg,'-hide_banner','-y','-i',str(zap),'-i',str(blast),
    '-filter_complex','[0:a]volume=0.45[z];[1:a]volume=0.309375[b];[z][b]amix=inputs=2:normalize=0:duration=longest',
    '-ac','1','-ar','48000','-c:a','libvorbis','-q:a','6',str(output)],check=True,capture_output=True)
profiles=json.loads((a/'spell_audio.json').read_text(encoding='utf-8'))
profiles['void']['strike_single']={'file':'void_single_strike','volume':.7,'event':'spell_void_strike_single'}
sounds=json.loads((a/'sounds.json').read_text(encoding='utf-8'))
sounds['spell_void_strike_single']={'subtitle':'subtitles.royalespells.void_strike_single','sounds':[
    {'name':'royalespells:original/void_single_strike','attenuation_distance':64,'preload':True}]}
for filename,data in [('spell_audio.json',profiles),('sounds.json',sounds)]:
    (a/filename).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
for locale,label in [('en_us','Void: concentrated impact'),('zh_cn','虚空：单体重击')]:
    p=a/f'lang/{locale}.json';data=json.loads(p.read_text(encoding='utf-8'))
    data['subtitles.royalespells.void_strike_single']=label
    p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
source=root/'SPELL-AUDIO-SOURCES.json';data=json.loads(source.read_text(encoding='utf-8'));data['profiles']=profiles
data['audio_update_beta5']={
    'original_mapping_commit':'59ef870142cef9b9dbf24ee07a05d3fd272b0e01',
    'mapping_sources':['https://github.com/Lee-The-Tree/Cr_Unpacked/blob/59ef870142cef9b9dbf24ee07a05d3fd272b0e01/Unpacked_Assets/'+p for p in ['toml/area/area_effect_objects.toml','csv/effects/effects.csv','csv/sounds/sounds.csv']],
    'single_target':'PaoloFirstEffectEverStrong adds Fire ball explo; Paolo_zap_effect3 also plays sound_spell_zap.',
    'group':'Keep the original zap_02 impact; no added blast layer.',
    'empty':'No impact event. Independent field ambience still plays on deployment.',
    'processing':'Offline mono mix: zap 0.45 + fireball explosion 0.309375, original speed/pitch; Vorbis q6 48 kHz. No generated substitute audio.',
    'files':[{'path':p.relative_to(root).as_posix(),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in [zap,blast,output]],
    'rights':'Supercell original audio and derivative mix excluded from MIT.'}
source.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Void single-target layered original cue imported.')
