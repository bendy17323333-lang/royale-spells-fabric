"""Run after the pinned audio importers: independent 64-block falloff and timed Void strikes."""
from pathlib import Path
import json
repo=Path(__file__).resolve().parents[1];assets=repo/'src/main/resources/assets/royalespells'
profiles=json.loads((assets/'spell_audio.json').read_text(encoding='utf-8'))
# The archive contains Void's continuous field ambience; use the original electric impact
# as an independent cue for each beam instead of replaying the six-second ambience.
profiles['void']['strike']={'file':'zap_02','volume':.7,'event':'spell_void_strike'}
sounds=json.loads((assets/'sounds.json').read_text(encoding='utf-8'))
sounds['spell_void_strike']={'subtitle':'subtitles.royalespells.void_strike','sounds':[{'name':'royalespells:original/zap_02','preload':True}]}
for profile in profiles.values():
 for cue in profile.values():
  for sound in sounds[cue['event']]['sounds']:sound['attenuation_distance']=64
for name in ['army_deploy','army_horn']:
 for sound in sounds[name]['sounds']:sound['attenuation_distance']=64;sound['volume']=.9
for filename,data in [('spell_audio.json',profiles),('sounds.json',sounds)]:
 (assets/filename).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
for locale,text in [('zh_cn','虚空：能量劈击'),('en_us','Void energy strikes')]:
 p=assets/'lang'/f'{locale}.json';data=json.loads(p.read_text(encoding='utf-8'));data['subtitles.royalespells.void_strike']=text;p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
provenance=repo/'SPELL-AUDIO-SOURCES.json';data=json.loads(provenance.read_text(encoding='utf-8'));data['profiles']=profiles
data['audio_update_1_5']={'network_radius':64,'attenuation_distance':64,'runtime_volume_scale':.9,'void_strikes_ticks':[16,40,64],'void_impact':'Original archived zap_02.ogg electric impact, independently triggered; the archive has no separate Void impact recording.'}
provenance.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Updated',sum(len(p) for p in profiles.values()),'spell cues and two army horn cues')
