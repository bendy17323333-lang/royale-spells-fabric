from pathlib import Path
import json
root=Path(__file__).resolve().parents[2]
n=root/'mineclash-zappies-addon'
b=root/'royale-spells-iron-1.21.1'
for p,extra in [(n/'src/main/resources/zappiesaddon.mixins.json',['pause.mixin.PauseIronStartMixin','mixin.MobGoalAccess']),(b/'src/main/resources/royalespells-electrical.mixins.json',['PauseIronStartMixin'])]:
    d=json.loads(p.read_text())
    for v in extra:
        if v not in d['mixins']:d['mixins'].append(v)
    p.write_text(json.dumps(d,indent=2)+'\n')
for lang,label in [('zh_cn','电击眩晕'),('en_us','Electrical Stun')]:
    p=b/f'src/main/resources/assets/royalespells/lang/{lang}.json'
    d=json.loads(p.read_text(encoding='utf-8'))
    d['effect.royalespells.electrical_stun']=label
    p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
for relative in ['SpellEngine.java','iron/RoyaleIronSpell.java','iron/MirrorIronSpell.java','iron/SpiritSpell.java','entity/ArmySkeleton.java','entity/ElementalSpirit.java']:
    p=b/'src/main/java/dev/royalespells'/relative
    s=p.read_text(encoding='utf-8')
    import re
    s=re.sub(r'(?<![\w.])(?:(player|caster)\.)?hasEffect\(RoyaleSpells.STUN\)',lambda m:'('+m[0]+'||dev.royalespells.pause.ElectricPause.active('+ (m[1] or 'this')+'))',s)
    p.write_text(s,encoding='utf-8')
for name in ['InfernoDragonAudio.java','InfernoDragonRenderer.java']:
    p=b/'src/main/java/dev/royalespells/client'/name
    s=p.read_text(encoding='utf-8')
    if name=='InfernoDragonAudio.java':
        s=s.replace('!VisualState.frozen(dragon)','!VisualState.frozen(dragon)&&!dev.royalespells.pause.ElectricPause.active(dragon)')
    else:s=s.replace('VisualState.frozen(e)||','VisualState.frozen(e)||dev.royalespells.pause.ElectricPause.active(e)||')
    p.write_text(s,encoding='utf-8')
p=b/'src/main/java/dev/royalespells/client/FrozenRender.java'
s=p.read_text().replace('frozen=VisualState.frozen(e);','frozen=VisualState.frozen(e)||dev.royalespells.pause.ElectricPause.active(e);')
p.write_text(s)
print('Electrical integration wired')
