"""Reuse the previously verified opt-in framebuffer/audio export for this mod.
Does not touch the real game instance, input devices, or unrelated applications.
"""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
source=root.parent/'mineclash-zappies-addon'
for old,new in [('BattleRecorder','VisualFrameRecorder'),('BattleAudioRecorder','VisualAudioRecorder')]:
    text=(source/f'src/main/java/dev/mineclash/zappies/client/{old}.java').read_text()
    text=text.replace('package dev.mineclash.zappies.client;','package dev.royalespells.client;').replace(old,new).replace('Zappies recording writer','Royale visual recording writer').replace('ZAPPIES_QA_FAILURE','ROYALE_VISUAL_QA_FAILURE')
    (root/f'src/main/java/dev/royalespells/client/{new}.java').write_text(text,encoding='utf-8')
text=(source/'tools/launch-client.py').read_text()
text=text.replace("repo=work/'mineclash-zappies-addon'","repo=work/'royale-spells-iron-1.21.1'")
text=text.replace("('baseline','pack','fixed','combat','battle')","('visual',)")
text=text.replace("mineclash-zappies-neoforge-1.21.1-","royale-spells-neoforge-1.21.1-")
text=text.replace("if mode=='pack' or 'pack' in suffix:","if True:")
text=text.replace("if mode in ('fixed','combat','battle') and p.name.startswith('royale-spells-'):continue","if p.name.startswith(('royale-spells-','mineclash-zappies-')):continue")
start=text.index("    if mode in ('fixed','combat','battle'):")
end=text.index("(run/'options.txt')",start)
text=text[:start]+"    if 'joint' in suffix:\n        addon=work/'mineclash-zappies-addon/dist/mineclash-zappies-neoforge-1.21.1-0.1.0.jar'\n        shutil.copy2(addon,run/'mods'/addon.name)\n"+text[end:]
text=text.replace("params=['-Xmx3G','-Dzappiesaddon.smoke=true']","params=['-Xmx3G','-Droyalespells.visualSmoke=true','-Droyalespells.snowballSmoke=true']")
text=text.replace("'1280' if mode=='battle' else '1600'","'1280'").replace("'800' if mode=='battle' else '1000'","'800'")
text=text.replace("marker='ZAPPIES_QA_COMPLETE'","marker='ROYALE_VISUAL_UPDATE_COMPLETE'")
(root/'tools/launch-visual-update.py').write_text(text,encoding='utf-8')
text=(source/'tools/encode-battle.py').read_text()
text=text.replace("run.name.startswith('run-production-battle')","run.name.startswith('run-production-visual')")
text=text.replace("kind=json.loads((run/'battle-evidence.json').read_text())['opponent']","kind='visual-update'")
text=text.replace("('01-Zappies-vs-MineClash-PEKKA.mp4' if kind=='pekka' else '02-Zappies-vs-RoyaleSpells-InfernoDragon.mp4')","'Royale-Spells-dev5-Snowball-Fireball.mp4'")
text=text.replace("ROOT/'docs/validation'/('battle-'+kind+'.ass')","run/'visual-titles.ass'")
insert="""
def ass_time(t):
    t=max(0,t);return f'{int(t)//3600}:{int(t)//60%60:02}:{int(t)%60:02}.{int(t*100)%100:02}'
scenes=json.loads((run/'visual-scenes.json').read_text())
header='''[Script Info]
ScriptType: v4.00+
PlayResX: 1280
PlayResY: 800
[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Default,Microsoft YaHei,27,&H00FFFFFF,&H000000FF,&H6020150D,&H90000000,0,0,0,0,100,100,0,0,3,1,0,8,35,35,30,1
Style: Footer,Microsoft YaHei,16,&H00DCE5EA,&H000000FF,&H80000000,&H80000000,0,0,0,0,100,100,0,0,1,1,0,2,35,35,20,1
[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
'''
lines=[]
for i,scene in enumerate(scenes):
    begin=(scene['epoch_ms']-start)/1000
    end=(scenes[i+1]['epoch_ms']-start)/1000 if i+1<len(scenes) else duration
    if end>0:lines.append(f"Dialogue: 0,{ass_time(begin)},{ass_time(end)},Default,,0,0,0,,{scene['title']}")
lines.append(f'Dialogue: 0,0:00:00.00,{ass_time(duration)},Footer,,0,0,0,,Royale Spells 1.7.0-dev.5 | 1.21.1 NeoForge | 游戏实录 · 原音效事件混音')
(run/'visual-titles.ass').write_text(header+'\\n'.join(lines)+'\\n',encoding='utf-8')
"""
text=text.replace("ass=str(ROOT/'docs/validation'/('battle-'+kind+'.ass'))", "ass=str(ROOT/'docs/validation'/('battle-'+kind+'.ass'))")
index=text.index('ass=str(')
text=text[:index]+insert+'\n'+text[index:]
(root/'tools/encode-visual-update.py').write_text(text,encoding='utf-8')
