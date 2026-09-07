"""Build original Minecraft-style meshes from the checked visual reference sheet.

The four spirits have different silhouettes/faces, not recoloured vanilla mobs.
Runtime spirit meshes use our existing white material tile; staffs use the
dedicated, unchanged image_gen atlas and a separate OBJ generator. Generated 32px icons are new pixel drawings, not edits of reference art.
"""
from pathlib import Path
import json, math
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/royalespells'

def write(path,obj):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def rgb(hex):return [int(hex[i:i+2],16)/255 for i in (0,2,4)]
def box(x,y,z,w,h,d,color,glow=False):
    return dict(from_=[x,y,z],size=[w,h,d],material=15,flat=True,color=rgb(color),emissive=glow)
def part(name,parent,pivot,boxes,rotation=(0,0,0)):
    for b in boxes:
        if 'from_' in b:b['from']=b.pop('from_')
    return dict(name=name,parent=parent,pivot=pivot,rotation=list(rotation),boxes=boxes)
def shard(x,y,z,w,h,d,color):
    # Four sloped quad faces and a base, with a narrow square tip.
    lower=[(x-w/2,y,z-d/2),(x+w/2,y,z-d/2),(x+w/2,y,z+d/2),(x-w/2,y,z+d/2)]
    upper=[(x-w*.06,y-h,z-d*.06),(x+w*.06,y-h,z-d*.06),(x+w*.06,y-h,z+d*.06),(x-w*.06,y-h,z+d*.06)]
    faces=[]
    for i in range(4):faces.append([n for p in (lower[i],lower[(i+1)%4],upper[(i+1)%4],upper[i]) for n in p])
    faces.extend([[n for p in reversed(lower) for n in p],[n for p in upper for n in p]])
    return dict(faces=faces,material=15,flat=True,color=rgb(color),emissive=False)

def bevel(x,y,z,w,h,d,color,cut=.7,glow=False):
    # Octagonal front and chamfered sides keep a soft silhouette without a sphere mesh.
    ring=[(x+cut,y),(x,y+cut),(x,y+h-cut),(x+cut,y+h),(x+w-cut,y+h),(x+w,y+h-cut),(x+w,y+cut),(x+w-cut,y)]
    front=[(a,b,z) for a,b in ring];back=[(a,b,z+d) for a,b in ring];center=(x+w/2,y+h/2,z);rear=(x+w/2,y+h/2,z+d)
    faces=[]
    for i in range(8):
        j=(i+1)%8
        faces.append([n for p in (center,front[i],front[j],front[j]) for n in p])
        faces.append([n for p in (front[i],back[i],back[j],front[j]) for n in p])
        faces.append([n for p in (rear,back[j],back[i],back[i]) for n in p])
    return dict(faces=faces,material=15,flat=True,color=rgb(color),emissive=glow)

PALETTES={
 'fire':('3B302D','261F20','FFA330','FFFFAD'),
 'ice':('DAF4FA','A3DBED','40BDF4','87DDFB'),
 'electro':('6F3ABB','422378','5421A1','FDEEFF'),
 'heal':('FFCD35','EBA623','AA3B0D','FFEF70')}

def spirit(kind):
    base,shadow,mouth,eye=PALETTES[kind]
    body=[bevel(-3.7,-4,-2.65,7.4,8,5.4,shadow,1.6),bevel(-3.4,-3.85,-3.0,6.8,7.7,4.9,base,1.3)]
    if kind=='fire':
        body += [bevel(-3.4,-2.9,-3.05,6.8,5.8,.12,base,1.15),bevel(-2.7,-3.5,-3.1,5.4,1.5,.12,'AA572B',.5)]
        for x in (-2.7,.55):
            body += [bevel(x-.2,-1.9,-3.17,2.5,2.9,.06,'F56D20',.5,True),bevel(x,-1.65,-3.23,2.1,2.4,.06,eye,.45,True)]
        body += [box(-1.2,2.65,-2.6,2.4,.25,.2,'EA7021',True)]
    else:
        body += [bevel(-2.65,-.05,-3.05,5.3,3.05,.03,'26165C' if kind!='heal' else '862A09',.85)]
        for x in (-2.4,.7):
            body += [bevel(x-.15,-3,-3.08,1.95,2.4,.03,{'ice':'508BE4','electro':'8C3ACF','heal':'DF851C'}[kind],.55)]
            body += [bevel(x+.2,-2.1,-3.12,1.1,1.2,.03,{'ice':'6BE8FF','electro':'FFF5FF','heal':'FFEC65'}[kind],.25,True)]
        if kind=='heal':body += [bevel(1.0,.08,-3.16,1.1,.85,.1,'FFF4A8',.2)]
        else:
            for x in (-1.7,-.55,.6):body += [bevel(x,-.03,-3.1,1,.55,.1,'C2EDFA' if kind=='ice' else 'E7CEFA',.17)]
            for x in (-1.45,-.25,.95):body += [bevel(x,2.25,-3.1,.85,.5,.1,'A1DEEC' if kind=='ice' else 'D5B1EE',.18)]
    parts=[part('body','',[0,18,0],body)]
    for side,sign in [('right',-1),('left',1)]:
        armcolor='67D5FA' if kind=='ice' else shadow if kind=='fire' else base
        parts.append(part(side+'_arm','body',[sign*3.6,-.1,0],[box(-.7,0,-.65,1.4,2.6,1.3,armcolor)],(0,0,sign*-15)))
        parts.append(part(side+'_leg','body',[sign*1.8,3.1,.1],[box(-.8,0,-1.1,1.6,2.6,2.2,armcolor)]))
    crown=[]
    if kind=='ice':
        for x,y,z,w,h,d in [(-1,-3.6,0,2,4.6,1.8),(1.1,-3.5,.4,1.8,3.1,1.7),(-3,-2.6,.3,1.7,2.4,1.6),(3,-2.6,.5,1.3,2.2,1.3)]:crown.append(shard(x,y,z,w,h,d,'46BAEA'))
        parts.append(part('ice_tail','body',[1,3,1.2],[shard(0,0,0,1.8,3,1.7,'63CAF0')],(145,0,20)))
    elif kind=='fire':
        for x,h,z in [(-2.2,3,-.2),(-.3,5,.3),(1.9,3.8,.3)]:
            crown += [shard(x,-3.8,z,1.6,h,1.8,'FF8526'),shard(x,-3.7,z-.4,.8,h*.7,.8,'FFD267')]
        crown += [box(-2.8,-4.2,-.9,5.6,1.3,2.6,'E95319',True)]
    elif kind=='electro':
        crown += [shard(-.4,-3.3,.2,4.2,2.5,3,'854AD6'),box(-.4,-7,.1,.75,2.4,.7,'B9FBFF',True),box(.25,-7.4,.1,1.3,.6,.7,'8AEFFF',True),box(.8,-8.2,.1,.7,1,.7,'CBFFFF',True)]
        # Distinct lightning moustache runs beside the grin, not oversized eyeballs.
        for sign in (-1,1):
            parts.append(part('arc_'+str(sign),'body',[sign*1.4,-.05,-3.22],[box(-.5,-.18,-.1,2.6,.45,.3,'ABF7FF',True),box(1.55,-.6,-.1,1.1,.5,.3,'D9FFFF',True)],(0,0,0 if sign==1 else 180)))
    else:
        parts.append(part('tongue','body',[-.5,1.5,-3],[box(-1,0,-.3,2.3,.65,1.15,'F5806A'),box(-.75,.6,-.2,1.8,.4,.85,'E85F53')],(-16,0,-8)))
        crown += [box(-1.6,-4.15,-.7,3.2,.35,2.5,'FFE96D')]
    parts.append(part('crown','body',[0,0,0],crown))
    write(ASSETS/f'models/troop/spirit_{kind}.json',dict(parts=parts))

def icon(kind):
    base,shade,mouth,eye=PALETTES[kind]
    image=Image.new('RGBA',(32,32),(0,0,0,0));d=ImageDraw.Draw(image)
    d.polygon([(10,7),(21,7),(25,11),(26,22),(21,27),(10,27),(6,23),(6,12)],fill='#'+shade)
    d.rectangle((9,9,22,23),fill='#'+base);d.rectangle((6,15,9,22),fill='#'+base);d.rectangle((23,15,26,22),fill='#'+base)
    if kind=='fire':
        d.polygon([(8,10),(7,4),(11,7),(15,0),(18,6),(22,3),(24,10)],fill='#FF9D30')
        d.rectangle((10,12,13,17),fill='#FFFFB5');d.rectangle((18,12,21,17),fill='#FFFFB5')
    else:
        d.rectangle((11,18,21,23),fill='#'+mouth);d.rectangle((10,11,13,15),fill='#'+eye);d.rectangle((18,11,21,15),fill='#'+eye)
        if kind=='ice':d.polygon([(7,11),(8,4),(12,8),(14,1),(18,8),(22,5),(24,11)],fill='#47C9FF')
        if kind=='electro':
            d.line([(15,8),(17,4),(15,4),(18,0)],fill='#B9FFFF',width=2);d.line([(5,17),(12,18),(12,16)],fill='#B9FFFF',width=2);d.line([(19,16),(20,18),(27,16)],fill='#B9FFFF',width=2)
        if kind=='heal':d.rectangle((18,18,19,19),fill='#FFF6BD');d.rectangle((12,21,16,25),fill='#F68070')
    for x in (10,19):d.rectangle((x,25,x+3,28),fill='#'+base)
    path=ASSETS/f'textures/gui/spell_icons/summon_{kind}_spirit.png';path.parent.mkdir(parents=True,exist_ok=True);image.save(path)

if __name__=='__main__':
    for kind in PALETTES:spirit(kind);icon(kind)
    import runpy
    runpy.run_path(str(ROOT/"art/make-furnace-staff.py"),run_name="__main__")
    write(ROOT/'src/main/resources/data/royalespells/recipe/furnace_staff.json',{'type':'minecraft:crafting_shaped','pattern':[' CF',' RI','R  '],'key':{'C':{'item':'minecraft:cauldron'},'F':{'item':'minecraft:fire_charge'},'R':{'item':'minecraft:blaze_rod'},'I':{'item':'minecraft:iron_ingot'}},'result':{'id':'royalespells:furnace_staff_fire','count':1},'neoforge:conditions':[{'type':'neoforge:mod_loaded','modid':'irons_spellbooks'}]})
    print('Generated four distinct spirit meshes, cauldron staffs, icons and crafting recipe.')
    labels={'fire':('烈焰','Fire'),'ice':('寒冰','Ice'),'electro':('雷电','Electro'),'heal':('治疗','Heal')}
    for lang in ('zh_cn','en_us'):
        path=ASSETS/f'lang/{lang}.json';strings=json.loads(path.read_text(encoding='utf-8'));cn=lang=='zh_cn'
        for kind,(zh,en) in labels.items():
            strings[f'item.royalespells.furnace_staff_{kind}']=f'{zh}熔炉法杖' if cn else f'{en} Furnace Staff'
            strings[f'ironspell.royalespells.summon_{kind}_spirit']=f'召唤{zh}精灵' if cn else f'Summon {en} Spirit'
        strings['entity.royalespells.elemental_spirit']='元素精灵' if cn else 'Elemental Spirit'
        strings['tooltip.royalespells.furnace_staff']='右键召唤绑定精灵；四种元素共享 6 秒基础冷却。' if cn else 'Right-click summons its bound spirit. All elements share a 6s base cooldown.'
        strings['tooltip.royalespells.furnace_attune']='奥术铁砧：火焰弹 / 浮冰 / 紫水晶碎片 / 闪烁的西瓜片' if cn else 'Arcane Anvil: Fire Charge / Packed Ice / Amethyst Shard / Glistering Melon Slice'
        strings['tooltip.royalespells.furnace_reagent']='当前元素媒介：%s' if cn else 'Current elemental reagent: %s'
        strings['ui.royalespells.spirit_rules']='召唤 1 只精灵，存活 20 秒；跳跃命中后消散。每人最多 6 只。' if cn else 'Summon 1 spirit for 20s. Bursts after leaping; at most 6 per player.'
        desc={'fire':('范围伤害 7；不破坏方块。','7 area damage; never destroys blocks.'),'ice':('范围伤害 2.5，冻结 1 秒。','2.5 area damage and a 1s freeze.'),'electro':('每次 2.5 伤害，最多连锁 9 个目标并眩晕 0.5 秒。','2.5 damage per hop; chains to up to 9 targets with a 0.5s stun.'),'heal':('伤害 2，治疗附近友军 4；也会主动寻找受伤友军。','2 damage, restores 4 health to nearby allies; seeks wounded allies too.')}
        for kind,pair in desc.items():strings['ui.royalespells.spirit_'+kind]=pair[0 if cn else 1]+(' 数值随法术强度成长。' if cn else ' Values scale with spell power.')
        write(path,strings)
