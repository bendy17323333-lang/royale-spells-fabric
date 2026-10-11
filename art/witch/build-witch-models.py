"""Author a single articulated mesh source for Minecraft and editable Blockbench.

No bitmap is painted by this script: witch_materials.png is the unchanged
ImageGen output. Normalized face UVs are shared exactly by the two exports.
Minecraft uses Y down, feet at 24; Blockbench export reflects Y about 24.
"""
from pathlib import Path
import base64, json, math, struct, uuid

ROOT = Path(__file__).resolve().parents[2]
OUT = Path(__file__).resolve().parent
ASSETS = ROOT / 'src/main/resources/assets/royalespells'
TEXTURE = ASSETS / 'textures/entity/witch_materials.png'
TW, TH = struct.unpack('>II', TEXTURE.read_bytes()[16:24])

def ident(name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'royalespells:witch/' + name))

class Rig:
    def __init__(self, name):
        self.name, self.parts = name, {}
    def part(self, name, parent='', pivot=(0,0,0), rotation=(0,0,0)):
        self.parts[name] = dict(name=name,parent=parent,pivot=list(pivot),rotation=list(rotation),boxes=[])
        return name
    def box(self, part, name, pos, size, mat, emissive=False, uv_scale=1):
        x,y,z=pos; X,Y,Z=[a+b for a,b in zip(pos,size)]
        faces=[[X,y,z,x,y,z,x,Y,z,X,Y,z], [x,y,Z,X,y,Z,X,Y,Z,x,Y,Z],
               [x,y,z,x,y,Z,x,Y,Z,x,Y,z], [X,y,Z,X,y,z,X,Y,z,X,Y,Z],
               [x,y,z,X,y,z,X,y,Z,x,y,Z], [x,Y,Z,X,Y,Z,X,Y,z,x,Y,z]]
        self.solid(part,name,faces,mat,emissive,uv_scale)
    def solid(self, part, name, faces, mat, emissive=False, uv_scale=1):
        u=(mat%4)*.25+.012; v=(mat//4)*.25+.012
        # Material grain has a consistent physical size. Mapping an entire tile
        # onto every small finger/metal edge was the rejected version's noise.
        uvs=[]
        for f in faces:
            w=math.dist(f[:3],f[3:6]);h=math.dist(f[3:6],f[6:9])
            su=min(.226,max(.012,w*.022))*uv_scale;sv=min(.226,max(.012,h*.022))*uv_scale
            uvs.append([u+su,v,u,v,u,v+sv,u+su,v+sv])
        self.parts[part]['boxes'].append(dict(name=name,material=mat,faces=faces,
            uvs=uvs,emissive=emissive))
    def taper(self,part,name,top,bottom,y0,y1,z,depth,mat):
        # Four-sided cloth panel, with real thickness and a sloping silhouette.
        a,b=top; c,d=bottom; Z=z+depth
        self.solid(part,name,[[b,y0,z,a,y0,z,c,y1,z,d,y1,z],
            [a,y0,Z,b,y0,Z,d,y1,Z,c,y1,Z], [a,y0,z,a,y0,Z,c,y1,Z,c,y1,z],
            [b,y0,Z,b,y0,z,d,y1,z,d,y1,Z], [a,y0,z,b,y0,z,b,y0,Z,a,y0,Z],
            [c,y1,Z,d,y1,Z,d,y1,z,c,y1,z]],mat)
    def segment(self, part,name,start,end,width,depth,mat):
        # Independent rigid segment so bends remain crisp and Minecraft-like.
        dx,dy,dz=[b-a for a,b in zip(start,end)]; length=math.sqrt(dx*dx+dy*dy+dz*dz)
        p=self.part(name,part,start,(0,0,-math.degrees(math.atan2(dx,dy))))
        self.box(p,name,(-width/2,0,-depth/2),(width,length,depth),mat)
    def save(self):
        (ASSETS/'models/troop'/f'{self.name}.json').write_text(json.dumps({'parts':list(self.parts.values())},indent=2))
        offsets={}
        def offset(n):
            if n not in offsets:
                p=self.parts[n]; base=offset(p['parent']) if p['parent'] else [0,0,0]
                offsets[n]=[a+b for a,b in zip(base,p['pivot'])]
            return offsets[n]
        groups={}; elements=[]
        for n,p in self.parts.items():
            off=offset(n)
            groups[n]={'name':n,'uuid':ident(self.name+'/'+n),'origin':[off[0],24-off[1],off[2]],
                'rotation':[-p['rotation'][0],p['rotation'][1],-p['rotation'][2]],'children':[],'export':True,'isOpen':True}
            for mesh in p['boxes']:
                vertices={}; faces={}
                for j,(face,uv) in enumerate(zip(mesh['faces'],mesh['uvs'])):
                    keys=[]; tex={}
                    for k in range(4):
                        key=f'v{j}_{k}'; keys.append(key)
                        vertices[key]=[face[k*3]+off[0],24-face[k*3+1]-off[1],face[k*3+2]+off[2]]
                        tex[key]=[uv[k*2]*TW,uv[k*2+1]*TH]
                    faces[str(j)]={'vertices':list(reversed(keys)),'uv':tex,'texture':1 if mesh.get('face_texture') else 0}
                eid=ident(self.name+'/'+n+'/'+mesh['name'])
                elements.append({'type':'mesh','name':mesh['name'],'uuid':eid,'origin':[0,0,0],
                    'vertices':vertices,'faces':faces,'visibility':True,'export':True,'autouv':0,'color':5})
                groups[n]['children'].append(eid)
        roots=[]
        for n,p in self.parts.items():
            if p['parent']:groups[p['parent']]['children'].append(groups[n])
            else:roots.append(groups[n])
        animations=[]
        def anim(name,length,loop,tracks):
            animators={}
            for bone,keys in tracks.items():
                animators[ident(self.name+'/'+bone)]={'name':bone,'type':'bone','keyframes':[
                    {'channel':'rotation','data_points':[{'x':-r[0],'y':r[1],'z':-r[2]}],
                     'uuid':ident(self.name+name+bone+str(t)),'time':t,'interpolation':'catmullrom'} for t,r in keys]}
            animations.append({'name':'animation.witch.'+name,'uuid':ident(self.name+'/'+name),
                'length':length,'loop':'loop' if loop else 'once','animators':animators})
        anim('idle',3,True,{'right_arm':[(0,(-12,0,-6)),(3,(-12,0,-6))],
            'right_forearm':[(0,(-8,0,0)),(3,(-8,0,0))],'staff':[(0,(20,0,0)),(3,(20,0,0))],
            'cape':[(0,(7,0,0)),(1.5,(10,2,0)),(3,(7,0,0))],
            'left_arm':[(0,(-8,0,-5)),(1.5,(-10,0,-7)),(3,(-8,0,-5))]})
        anim('walk',1,True,{'right_leg':[(0,(20,0,0)),(.5,(-20,0,0)),(1,(20,0,0))],
            'left_leg':[(0,(-20,0,0)),(.5,(20,0,0)),(1,(-20,0,0))],
            'robe_right':[(0,(12,0,-3)),(.5,(-8,0,-3)),(1,(12,0,-3))],
            'robe_left':[(0,(-8,0,3)),(.5,(12,0,3)),(1,(-8,0,3))]})
        anim('cast',1.1,False,{'right_arm':[(0,(-12,0,-6)),(.4,(-22,0,-5)),(.7,(-28,0,-5)),(1.1,(-12,0,-6))],
            'right_forearm':[(0,(-8,0,0)),(.4,(-1,0,0)),(.7,(3,0,0)),(1.1,(-8,0,0))],
            'staff':[(0,(20,0,0)),(.4,(48,0,0)),(.7,(70,0,0)),(1.1,(20,0,0))],
            'left_arm':[(0,(-8,0,-5)),(.4,(-32,0,-17)),(.7,(-38,0,-12)),(1.1,(-8,0,-5))]})
        anim('summon',1.2,False,{'right_arm':[(0,(-12,0,-6)),(.4,(-32,0,15)),(.8,(-38,0,12)),(1.2,(-12,0,-6))],
            'left_arm':[(0,(-8,0,-5)),(.4,(-65,0,-40)),(.8,(-70,0,-36)),(1.2,(-8,0,-5))]})
        native={'meta':{'format_version':'4.10','model_format':'free','box_uv':False},'name':self.name,
            'resolution':{'width':TW,'height':TH},'elements':elements,'outliner':roots,'animations':animations,
            'textures':[{'path':str(TEXTURE),'name':'witch_materials.png','id':'0','uuid':ident('texture'),
                'width':TW,'height':TH,'uv_width':TW,'uv_height':TH,'particle':False,'visible':True,
                'source':'data:image/png;base64,'+base64.b64encode(TEXTURE.read_bytes()).decode()}]}
        face_tex=ASSETS/'textures/entity/witch_face.png'
        native['textures'].append({'path':str(face_tex),'name':'witch_face.png','id':'1','uuid':ident('face_texture'),
            'width':TW,'height':TH,'uv_width':TW,'uv_height':TH,'particle':False,'visible':True,
            'source':'data:image/png;base64,'+base64.b64encode(face_tex.read_bytes()).decode()})
        (OUT/f'{self.name}.bbmodel').write_text(json.dumps(native),encoding='utf-8')
        print(self.name,len(self.parts),'bones',len(elements),'meshes')

def make(evo):
    r=Rig('witch_evolution' if evo else 'witch')
    r.part('body'); r.part('head','body',(0,0,0))
    r.box('body','fitted_robe',(-3.2,1,-2.1),(6.4,9.5,4.6),1)
    r.taper('body','waisted_overdress',(-3.8,3.8),(-2.8,2.8),1,10,-2.5,.55,0)
    r.box('body','collar_shadow',(-2,0,-2.5),(4,2,.7),8)
    for side in [-1,1]:
        n='lapel_'+str(side);r.part(n,'body',(side*1.9,1,-2.65),(0,0,side*-14))
        r.box(n,n,(-.55,0,-.18),(1.1,7,.4),12)
    r.box('body','collar_clasp',(-.6,2,-3.1),(1.2,1.3,.55),4)
    r.box('body','waistband',(-3.25,9,-2.35),(6.5,1.1,5.0),13)
    r.box('body','belt_buckle',(-.65,9.1,-2.65),(1.3,.9,.45),4)
    # Hood is a shell with an actual recessed opening, not a solid face slab.
    r.box('head','face',(-3.05,-6.4,-3.3),(6.1,6.4,5.5),7)
    r.box('head','hood_top',(-4.15,-8.3,-3.55),(8.3,1.35,7.75),0)
    r.box('head','hood_crown',(-3.15,-8.85,-2.8),(6.3,.6,5.9),0)
    r.box('head','hood_back',(-4.1,-7,-.4),(8.2,7.8,4.75),1 if evo else 0)
    for side in [-1,1]:
        x=-4.25 if side<0 else 3.2
        r.box('head','hood_side_'+str(side),(x,-7,-3.65),(1.05,8.3,6.85),0)
        r.box('head','hood_cheek_trim_'+str(side),(x,-5.1,-3.91),(1.05,5.9,.32),4 if evo else 12)
        r.box('head','hood_lower_tab_'+str(side),(side*3.35-.45,.4,-2.3),(.9,2.9,3.9),0)
    r.box('head','hood_front_rim',(-4.22,-7.03,-3.86),(8.44,.85,.35),4 if evo else 0)
    # Asymmetric fringe, flat eyes and a restrained pixel mouth.
    for i,(x,w,h) in enumerate([(-3.1,1.15,2.25),(-1.98,.98,2.8),(-1.03,1.05,1.85),(.0,1.3,2.2),(1.27,1.02,2.8),(2.26,.85,3.45)]):
        r.box('head','bang_'+str(i),(x,-6.3,-3.58),(w,h,.48),0)
    for side in [-1,1]:
        x=side*1.55
        r.box('head','eye_shadow_'+str(side),(x-.72,-3.83,-3.35),(1.44,1.15,.095),8,uv_scale=.18)
        r.box('head','eye_white_'+str(side),(x-.6,-3.7,-3.46),(1.2,.86,.065),10 if evo else 11,evo,uv_scale=.15)
        r.box('head','iris_'+str(side),(x-.26,-3.73,-3.54),(.55,.9,.06),10,True,uv_scale=.35)
        r.box('head','eye_glint_'+str(side),(x-.2,-3.71,-3.60),(.18,.26,.035),11,True,uv_scale=.12)
    r.box('head','nose',(-.34,-2.63,-3.52),(.68,.7,.45),7,uv_scale=.25)
    r.box('head','mouth',(-.5,-1.4,-3.35),(1,.17,.065),2,uv_scale=.2)
    r.box('head','chin',(-2.55,-.7,-3.15),(5.1,.9,1.8),7)
    # Segmented cape and split hem expose feet without a large square body.
    r.part('cape','body',(0,2,2.5))
    r.taper('cape','cape_upper',(-3.9,3.9),(-3.7,3.7),0,10,.1,.6,1)
    r.part('cape_tail','cape',(0,10,.4))
    r.taper('cape_tail','cape_lower',(-3.7,3.7),(-5.0,4.3),0,9,.1,.65,1)
    for i,(x,y,h) in enumerate([(-4.4,7,3.3),(-2.7,7,2.3),(.4,7,4), (2.8,7,2.9)]):
        r.box('cape_tail','cape_fray_'+str(i),(x,y,.12),(1.05,h,.6),2)
    r.taper('body','underskirt',(-2.9,2.9),(-4.2,4.2),10,22,-1.5,3.8,8)
    for side in [-1,1]:
        suffix='right' if side<0 else 'left'
        p=r.part('robe_'+suffix,'body',(side*1.6,10,-2.5),(0,0,side*4))
        r.taper(p,'front_robe_'+suffix,(-1.45,1.45),(-1.8,1.8),0,11,-.1,.6,0)
        r.box(p,'vertical_trim_'+suffix,(side*.85-.22,.2,-.27),(.44,10.6,.3),12)
        r.taper(p,'inner_split_'+suffix,(-.75,.75),(-.8,.65),1.4,12.3,.55,.4,2)
        q=r.part('robe_side_'+suffix,'body',(side*3,10,.9),(0,0,side*-7))
        r.taper(q,'side_skirt_'+suffix,(-.65,.65),(-1.2,1.2),0,12.5,-1.4,3.2,2)
        r.box(q,'hem_tatter_'+suffix,(-.65,11.4,-1.45),(.9,1.8,.55),0)
        leg=r.part(suffix+'_leg','body',(side*1.65,13,.3))
        r.box(leg,'stocking_'+suffix,(-1.05,0,-.9),(2.1,8.5,2.1),1)
        r.box(leg,'boot_'+suffix,(-1.15,8.2,-1.9),(2.3,2.6,3.5),13)
        r.box(leg,'boot_edge_'+suffix,(-1.17,10.5,-1.95),(2.34,.45,3.55),8)
        arm=r.part(suffix+'_arm','body',(side*4.0,2,.0))
        r.box(arm,'upper_arm_'+suffix,(-1.35,0,-1.35),(2.7,4.6,2.7),7)
        r.box(arm,'pauldron_base_'+suffix,(-1.8,-.7,-1.75),(3.6,2.55,3.5),14)
        r.box(arm,'pauldron_gilt_'+suffix,(-1.9,-.8,-1.86),(3.8,.65,3.72),4)
        r.box(arm,'pauldron_face_'+suffix,(-1.6,-.1,-1.87),(3.2,1.6,.3),4)
        fore=r.part(suffix+'_forearm',arm,(0,4.45,0))
        r.box(fore,'forearm_'+suffix,(-1.15,0,-1.1),(2.3,3.65,2.2),7)
        r.box(fore,'wrist_band_'+suffix,(-1.35,2.55,-1.35),(2.7,.95,2.7),4)
        r.box(fore,'hand_'+suffix,(-1.15,3.45,-1.25),(2.3,1.7,2.6),7)
        r.box(fore,'thumb_'+suffix,(-.55,3.6,-1.65),(1.1,1,.55),7)
    r.part('staff','right_forearm',(0,4.15,-1.5),(0,0,-10))
    r.box('staff','staff_shaft',(-.58,-20,-.58),(1.16,29,1.16),6)
    for i,y in enumerate([-16,-11,-6,1,7]):r.box('staff','staff_binding_'+str(i),(-.72,y,-.72),(1.44,.8,1.44),13)
    r.box('staff','pommel',(-.85,8.3,-.85),(1.7,1.1,1.7),4)
    r.box('staff','head_collar',(-1.4,-20.4,-1.4),(2.8,1.45,2.8),14)
    r.box('staff','head_gilt_band',(-1.6,-21.05,-1.6),(3.2,.85,3.2),4)
    r.box('staff','bone_core',(-2.25,-25.5,-2.1),(4.5,4.4,4.2),3)
    r.box('staff','bone_cap',(-1.6,-26.1,-1.55),(3.2,.65,3.1),15)
    r.box('staff','bone_lower',(-1.65,-21.55,-1.55),(3.3,.85,3.1),3)
    for side in [-1,1]:
        r.segment('staff','gold_claw_'+str(side),(side*1.4,-20.5,0),(side*2.8,-24,0),.9,1.7,4)
        r.segment('staff','gold_claw_tip_'+str(side),(side*2.8,-24,0),(side*2.45,-26.6,0),.8,1.5,4)
    r.box('staff','front_mount',(-.5,-26.6,-2.5),(1,2.6,.8),4)
    r.box('staff','front_rune',(-.45,-24.3,-2.16),(.9,1.4,.1),9,True,uv_scale=.45)
    if evo:
        r.box('head','crown_brow',(-2.9,-7.9,-4.03),(5.8,.9,.55),4)
        r.box('head','little_beast_skull',(-1.15,-9.15,-4.3),(2.3,1.6,1.15),4)
        r.box('head','skull_muzzle',(-.65,-8.2,-4.48),(1.3,.8,.35),14)
        for side in [-1,1]:
            r.box('head','crown_socket_'+str(side),(side*.55-.24,-8.8,-4.53),(.48,.43,.12),8,uv_scale=.2)
            r.segment('head','horn_root_'+str(side),(side*1.0,-8.5,-3.8),(side*3.3,-9.2,-3.8),1.15,1.1,4)
            r.segment('head','horn_curve_'+str(side),(side*3.3,-9.2,-3.8),(side*4.0,-11.0,-3.8),.95,.9,4)
            r.segment('head','horn_tip_'+str(side),(side*4.0,-11.0,-3.8),(side*3.7,-12.1,-3.8),.58,.6,14)
        r.box('body','evolution_clasp',(-.5,2.2,-3.67),(1,1,.3),9,True,uv_scale=.3)
        # The evolution has a separate open-jawed skull around a suspended gem.
        # Keep the head genuinely hollow: the crystal is not painted over a cube.
        r.parts['staff']['boxes']=[b for b in r.parts['staff']['boxes'] if b['name'] not in
            ['bone_core','bone_cap','bone_lower','front_mount','front_rune']]
        for n in [n for n in r.parts if n.startswith('gold_claw')]:del r.parts[n]
        r.box('staff','skull_cranium',(-2.55,-27.1,-1.8),(5.1,2.15,3.8),3)
        r.box('staff','skull_crown',(-1.8,-27.65,-1.3),(3.6,.6,2.8),15)
        r.box('staff','skull_rear',(-2.05,-25.3,1.0),(4.1,4.1,1.0),3)
        for side in [-1,1]:
            r.box('staff','skull_orbit_'+str(side),(side*1.25-.62,-25.65,-1.91),(1.24,1.0,.18),8,uv_scale=.25)
            r.box('staff','skull_cheek_'+str(side),(side*2.0-.35,-25.1,-1.6),(.7,1.5,2.6),3)
            r.box('staff','jaw_hinge_'+str(side),(side*1.8-.32,-23.7,.7),(.64,3.1,.9),3)
        r.box('staff','lower_jaw',(-2.05,-21.25,-2.05),(4.1,.8,3.5),3)
        for i in range(4):
            r.box('staff','upper_tooth_'+str(i),(-1.65+i*.88,-24.96,-1.86),(.55,.64,.6),15)
            r.box('staff','lower_tooth_'+str(i),(-1.65+i*.88,-21.74,-2.0),(.55,.64,.6),15)
        r.part('staff_crystal','staff',(0,-23.1,-.8),(0,0,45))
        r.box('staff_crystal','soul_crystal',(-.77,-.77,-.75),(1.54,1.54,1.5),9,True)
        r.box('staff_crystal','soul_crystal_core',(-.4,-.4,-.80),(.8,.8,.09),10,True,uv_scale=.5)
    # Shorten the head section while retaining a long, usable brown handle.
    # Holding joint remains exactly at the palm; the muzzle is checked at cast release.
    for b in r.parts['staff']['boxes']:
        if b['name']=='staff_shaft':
            for f in b['faces']:
                for k in [1,4,7,10]:
                    if f[k]<0:f[k]+=6
        elif b['name'].startswith('staff_binding') or b['name']=='pommel':continue
        else:
            for f in b['faces']:
                for k in [1,4,7,10]:f[k]+=6
    for n,p in r.parts.items():
        if p['parent']=='staff' and n!='staff':p['pivot'][1]+=6
    from witch_shape_refine import refine_shape
    from witch_staff_refine import refine_staff
    refine_shape(r,evo)
    refine_staff(r,evo)
    r.save()

if __name__=='__main__':
    OUT.mkdir(parents=True,exist_ok=True)
    make(False);make(True)
