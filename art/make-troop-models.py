"""Build editable articulated geometry, not bitmap images. All materials use the imagegen atlas."""
import json, math, uuid, base64
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'src/main/resources/assets/royalespells/models/troop'
EDIT=ROOT/'art/blockbench'
DEST.mkdir(parents=True,exist_ok=True);EDIT.mkdir(parents=True,exist_ok=True)
SKIN,HAIR,LEATHER,STEEL,BLUE,BONE,BLACK,STONE,CRYSTAL,CLOTH,PALE,WING,WOOD,STRAW,GOLD,WHITE=range(16)
class Rig:
    def __init__(self,name):self.name=name;self.parts=[]
    def part(self,name,parent='',pivot=(0,0,0),rotation=(0,0,0)):
        p=dict(name=name,parent=parent,pivot=list(pivot),rotation=list(rotation),boxes=[]);self.parts.append(p);return p
    def box(self,p,mat,x,y,z,w,h,d):p['boxes'].append(dict(material=mat,from_=[x,y,z],size=[w,h,d]));p['boxes'][-1]['from']=p['boxes'][-1].pop('from_')
    def bevel(self,p,mat,x,y,z,w,h,d,b=.6):
        self.box(p,mat,x+b,y,z+b,w-2*b,h,d-2*b);self.box(p,mat,x,y+b,z+b,w,h-2*b,d-2*b);self.box(p,mat,x+b,y+b,z,w-2*b,h-2*b,d)
    def solid(self,p,color,x,y,z,w,h,d):
        self.box(p,WHITE,x,y,z,w,h,d);p['boxes'][-1].update(flat=True,color=color)
    def loft(self,p,color,rings,cx=0,corner=.28):
        # Connected octagonal sections give an actual silhouette bevel, with no overlapping box seams.
        loops=[]
        for y,w,d,z in rings:
            bx=w*corner;bz=d*corner
            loops.append([[cx+px,y,z+pz] for px,pz in [(-w+bx,-d),(w-bx,-d),(w,-d+bz),(w,d-bz),(w-bx,d),(-w+bx,d),(-w,d-bz),(-w,-d+bz)]])
        faces=[]
        for top,bottom in zip(loops,loops[1:]):
            for i in range(8):faces.append(sum([top[(i+1)%8],top[i],bottom[i],bottom[(i+1)%8]],[]))
        for loop,reverse in [(loops[0],False),(loops[-1],True)]:
            center=[sum(v[j] for v in loop)/8 for j in range(3)]
            for i in range(8):
                a,b=loop[i],loop[(i+1)%8]
                if reverse:a,b=b,a
                faces.append(sum([center,a,b,b],[]))
        p['boxes'].append(dict(material=WHITE,flat=True,color=color,faces=faces))
    def save(self):
        (DEST/(self.name+'.json')).write_text(json.dumps(dict(parts=self.parts),indent=2),encoding='utf8')
        # Blockbench generic project: real mesh, UVs, named pivots, embedded production texture.
        elements=[];groups={};world={};out=[]
        for p in self.parts:
            parent=world.get(p['parent'],[0,0,0]);pos=[parent[i]+p['pivot'][i] for i in range(3)];world[p['name']]=pos
            group=dict(name=p['name'],origin=[pos[0],24-pos[1],pos[2]],rotation=[-p['rotation'][0],p['rotation'][1],-p['rotation'][2]],uuid=str(uuid.uuid5(uuid.NAMESPACE_URL,self.name+p['name'])),children=[])
            groups[p['name']]=group
            (groups[p['parent']]['children'] if p['parent'] else out).append(group)
            for idx,box in enumerate(p['boxes']):
                if 'faces' in box:
                    eid=str(uuid.uuid5(uuid.NAMESPACE_URL,self.name+p['name']+str(idx)));verts={};faces={}
                    for fi,f in enumerate(box['faces']):
                        ids=[]
                        for vi in range(4):
                            xyz=f[vi*3:vi*3+3];key=','.join(str(round(n,5)) for n in xyz)
                            if key not in verts:verts[key]=[pos[0]+xyz[0],24-pos[1]-xyz[1],pos[2]+xyz[2]]
                            if key not in ids:ids.append(key)
                        faces[str(fi)]=dict(vertices=ids,uv={key:[390,390] for key in ids},texture=0)
                    elements.append(dict(type='mesh',name=p['name']+'_'+str(idx),uuid=eid,origin=[0,0,0],vertices=verts,faces=faces,royale_vertex_color=box['color']))
                    group['children'].append(eid);continue
                f=[pos[i]+box['from'][i] for i in range(3)];s=box['size'];mat=box['material'];u=mat%4*128+6;v=mat//4*128+6
                eid=str(uuid.uuid5(uuid.NAMESPACE_URL,self.name+p['name']+str(idx)))
                elements.append(dict(name=p['name']+'_'+str(idx),uuid=eid,from_=[f[0],24-f[1]-s[1],f[2]],to=[f[0]+s[0],24-f[1],f[2]+s[2]],autouv=0,faces={face:dict(uv=[u,v,u+116,v+116],texture=0) for face in ['north','south','east','west','up','down']}))
                elements[-1]['from']=elements[-1].pop('from_');group['children'].append(eid)
        tex=base64.b64encode((ROOT/'src/main/resources/assets/royalespells/textures/entity/troop_materials.png').read_bytes()).decode()
        bb=dict(meta=dict(format_version='4.10',model_format='free',box_uv=False),name=self.name,model_identifier='royalespells:'+self.name,resolution=dict(width=512,height=512),elements=elements,outliner=out,textures=[dict(name='troop_materials.png',id='0',uuid=str(uuid.uuid4()),source='data:image/png;base64,'+tex,uv_width=512,uv_height=512)],animations=[])
        (EDIT/(self.name+'.bbmodel')).write_text(json.dumps(bb,separators=(',',':')),encoding='utf8')
        print(self.name,len(elements),'cuboids',len(self.parts),'joints')

def face(r,head,skin=SKIN,hair=True):
    r.bevel(head,skin,-4,-7.5,-3.4,8,7.5,7,.55)
    # Nearly flush facial surfaces: shallow eye openings, no stacked protruding eye cubes.
    for x in [-2.5,1.0]:
        r.box(head,WHITE,x,-4.55,-3.42,1.5,.65,.025)
        r.box(head,BLUE,x+.5,-4.54,-3.45,.65,.62,.025)
        r.box(head,BLACK,x+.7,-4.48,-3.48,.32,.49,.025)
        r.box(head,HAIR if hair else CLOTH,x-.15,-4.98,-3.46,1.8,.3,.06)
    r.bevel(head,skin,-.55,-3.8,-3.95,1.1,1.6,.6,.15)
    r.box(head,BLACK,-1.65,-1.3,-3.42,3.3,.6,.04)
    r.box(head,WHITE,-1.5,-1.27,-3.47,3,.25,.04)
    if hair:
        r.bevel(head,HAIR,-4.25,-8.1,-3.6,8.5,2.1,7.6,.55)
        for x in [-3.3,-1.9,-.5,.9,2.3]:r.box(head,HAIR,x,-6.9,-3.72,1.2,.8+(x+3.3)*.07,.55)
        for s in [-1,1]:
            tuft=r.part('moustache_'+str(s),'head',(s*.5,-2,-3.95),(0,0,s*-14));r.bevel(tuft,HAIR,-1.6,-.45,-.35,3.2,1.5,1.1,.25)
            r.box(head,HAIR,s*3.5-.25,-6.2,-1.8,.75,3,4)


# Smooth color fields keep pores/stripes out of tiny faces; these are vertex colors, not new bitmap art.
FLESH=[1,.65,.41];CHEEK=[1,.69,.46];YELLOW=[1,.76,.06];GOLD_LIGHT=[1,.84,.11]
BROWN=[.29,.12,.055];BELT=[.59,.13,.07];IRON=[.62,.68,.75];DARK=[.055,.014,.008]

def barbarian_face(r,head):
    # Broad short jaw, rounded cheeks and a golden bowl cut; no eye cubes stacked off the face.
    r.loft(head,FLESH,[(-9.3,3.6,3.3,.25),(-8.5,4.45,3.8,.2),(-4.9,4.5,4.05,0),(-2.3,4.3,4,0),(-.25,3.45,3.8,.2),(.3,2.8,3.25,.25)])
    r.loft(head,YELLOW,[(-10.4,2.9,2.8,.25),(-10,3.95,3.55,.25),(-9.3,4.65,4.15,.25),(-6.85,4.7,4.2,.25)],corner=.35)
    for sign in [-1,1]:
        lock=r.part('sideburn_'+str(sign),'head',(sign*4.18,-6.9,.45),(0,0,-sign*3))
        r.loft(lock,YELLOW,[(0,.59,3.5,0),(4.7,.55,3.05,.1),(5.3,.28,2.3,.2)],corner=.24)
        eye=r.part('eye_'+str(sign),'head',(sign*1.96,-5.35,-4.075),(0,0,-sign*9))
        r.solid(eye,[1,1,1],-.95,-.5,-.027,1.9,1.16,.03)
        r.solid(eye,[.22,.49,.67],-sign*.14-.35,-.38,-.053,.7,.89,.025)
        r.solid(eye,[.025,.035,.05],-sign*.14-.17,-.32,-.075,.34,.75,.021)
        r.solid(eye,[1,1,1],-sign*.14-.17,-.28,-.095,.16,.23,.018)
        brow=r.part('brow_'+str(sign),'head',(sign*2.04,-6.37,-3.91),(0,0,-sign*11))
        r.loft(brow,GOLD_LIGHT,[(-.64,1.87,.5,0),(.28,1.91,.62,0),(.52,1.68,.47,0)],corner=.2)
    # Short broad nose, not a hanging villager nose.
    r.loft(head,CHEEK,[(-5.25,.38,.32,-4.0),(-4.85,.68,.68,-4.08),(-4.15,.88,.72,-4.14),(-3.95,.55,.42,-4.05)],corner=.42)
    # Wide open battle cry framed by the original long horseshoe moustache.
    r.solid(head,DARK,-2.0,-3.22,-4.16,4,2.85,.04)
    r.solid(head,[.5,.08,.035],-1.65,-.92,-4.22,3.3,.47,.04)
    for x in [-1.56,-.77,.02,.81]:
        r.solid(head,[1,1,.94],x,-2.98,-4.25,.74,.48,.04)
        r.solid(head,[1,1,.94],x,-.98,-4.26,.74,.36,.04)
    r.solid(head,[.93,.29,.24],-.88,-1.54,-4.28,1.76,.5,.035)
    r.loft(head,GOLD_LIGHT,[(-3.99,1.75,.41,-4.23),(-3.66,2.28,.52,-4.28),(-3.18,2.33,.42,-4.3)],corner=.33)
    for sign in [-1,1]:
        tuft=r.part('moustache_'+str(sign),'head',(sign*2.23,-3.42,-4.3),(0,0,-sign*7))
        r.loft(tuft,YELLOW,[(0,.53,.45,0),(1.2,.58,.48,0),(3.28,.53,.41,.06),(3.65,.31,.29,.05)],corner=.35)

def barbarian():
    r=Rig('barbarian');body=r.part('body')
    r.loft(body,FLESH,[(0,4.2,2.8,0),(1.2,5.45,3.2,0),(5.1,4.85,3.05,0),(9.35,4.15,2.75,0)])
    for sign in [-1,1]:
        r.loft(body,CHEEK,[(1.1,1.6,.48,-2.97),(2,2.22,.6,-3.08),(4.9,1.85,.35,-3.03)],cx=sign*2.33,corner=.35)
    r.loft(body,BELT,[(9.1,4.5,3.1,0),(10.75,4.7,3.25,0)],corner=.2)
    r.loft(body,IRON,[(9.24,1.05,.27,-3.2),(10.15,1.15,.3,-3.3),(10.83,.67,.25,-3.25)],corner=.35)
    for sign in [-1,1]:r.solid(body,[.78,.8,.82],sign*.72-.12,9.78,-3.65,.24,.24,.12)
    head=r.part('head','body');barbarian_face(r,head)
    for side,sign in [('right',-1),('left',1)]:
        arm=r.part(side+'_arm','body',(sign*6,2,0),(0,0,sign*-4))
        r.loft(arm,FLESH,[(-2.3,1.6,1.95,0),(-1.2,2.65,2.8,0),(2.8,2.5,2.55,0),(5.4,2.02,2.25,0),(9.3,2.0,2.2,0)])
        r.loft(arm,BROWN,[(5.5,2.27,2.5,0),(7.55,2.3,2.55,0)],corner=.2)
        for z in [-1.5,0,1.5]:r.solid(arm,IRON,sign*2.3-.26,6,z-.32,.58,.8,.64)
        r.loft(arm,CHEEK,[(8.1,1.9,2.12,0),(8.7,2.4,2.6,0),(10.8,2.36,2.55,0),(11.45,1.86,2.04,0)],corner=.23)
        # Keep the existing palm pivot/hand depth so the equipped sword stays in the fist.
        leg=r.part(side+'_leg','body',(sign*2.5,11,0))
        r.loft(leg,BROWN,[(0,2.42,2.6,0),(4.4,2.55,2.7,0),(4.7,2.4,2.52,0)],corner=.19)
        r.loft(leg,FLESH,[(4.4,1.96,1.95,0),(6.4,2,2.1,-.1),(9.85,1.6,1.9,0)])
        r.loft(leg,BROWN,[(9.5,1.92,2.07,0),(10.6,2.16,2.42,-.22),(12.7,2.2,2.85,-.35),(13,1.94,2.56,-.35)],corner=.22)
    r.save()

def recruit():
    r=Rig('royal_recruit');body=r.part('body')
    cobalt=[.08,.4,.66];steel=[.39,.49,.54];rim=[.59,.66,.67];oak=[.62,.35,.16]
    r.loft(body,cobalt,[(0,4,2.8,0),(4,4.8,3,0),(10,4.1,2.8,0)])
    r.loft(body,steel,[(.5,3.8,2.85,-.2),(2.4,5,3.55,-.2),(6.8,4.7,3.3,-.2),(8.8,4.1,2.95,-.2)])
    r.loft(body,cobalt,[(8.8,4.45,3.1,0),(10.4,4.5,3.15,0)])
    r.solid(body,[.86,.6,.22],-1.3,8.7,-3.55,2.6,2,.45);r.solid(body,[.16,.25,.27],-.75,9.2,-3.81,1.5,.9,.1)
    r.loft(body,[.19,.24,.27],[(10.2,4.5,3,0),(13.1,5.4,3.3,0)])
    for side in [-1,1]:
        for x in [-3.9,-2.35,-.8,.8,2.35,3.9]:
            r.loft(body,steel,[(10.5,.65,.25,side*3.14),(11.25,.73,.3,side*3.35),(12.25,.63,.28,side*3.4),(12.5,.28,.2,side*3.4)],cx=x)
    head=r.part('head','body')
    # Twelve separate tapered staves, including real seams; this is a bucket, not a metal helmet.
    for i in range(12):
        a=i*math.tau/12;half=math.pi/12-.014;faces=[]
        loops=[]
        for y,rad in [(-9.3,4.05),(-.15,4.75)]:
            loops.append([[math.sin(t)*rr,y,-math.cos(t)*rr] for rr,t in [(rad,a-half),(rad,a+half),(rad-.45,a+half),(rad-.45,a-half)]])
        for j in range(4):faces.append(sum([loops[0][j],loops[0][(j+1)%4],loops[1][(j+1)%4],loops[1][j]],[]))
        faces.extend([sum(loops[0],[]),sum(reversed(loops[1]),[])])
        shade=.92+(i%3)*.055
        head['boxes'].append(dict(material=WHITE,flat=True,color=[c*shade for c in oak],faces=faces))
    r.loft(head,oak,[(-9.3,3.85,3.85,0),(-9,3.85,3.85,0)],corner=.3)
    for y,rad in [(-8.3,4.22),(-1.9,4.72)]:
        for i in range(12):
            band=r.part('bucket_band_'+str(y)+'_'+str(i),'head',(0,0,0),(0,i*30,0))
            r.solid(band,steel,-rad*.269,y,-rad-.08,rad*.538,1.12,.35)
            r.loft(band,rim,[(y+.28,.22,.12,-rad-.49),(y+.7,.22,.12,-rad-.49)],corner=.25)
    # The original conceals the eyes. Only the nose protrudes through the narrow face opening.
    r.solid(head,[.16,.09,.04],-1.38,-4.35,-4.75,2.76,1.15,.18)
    r.loft(head,[.95,.48,.28],[(-4.22,.56,.35,-4.97),(-3.99,1.03,.72,-5.04),(-3.4,1.06,.72,-5.04),(-3.13,.65,.37,-4.96)])
    r.loft(head,steel,[(-9.8,.9,.9,0),(-9.2,1,.95,0)])
    crest=r.part('crest','head')
    r.loft(crest,[.04,.46,.72],[(-9.7,.3,.35,0),(-11.3,.36,.4,.25),(-12.7,.55,.65,.95),(-13.7,.9,1.3,2.05),(-14.1,.92,1.35,2.5)])
    r.loft(crest,[.08,.65,.89],[(-14.3,.78,1.2,2.6),(-14.8,.95,1.5,2.8),(-14.4,1.13,1.75,3.3),(-13.5,1.1,1.65,3.7),(-12.4,.78,1.03,4),(-12.05,.4,.5,4)])
    for side,s in [('right',-1),('left',1)]:
        arm=r.part(side+'_arm','body',(s*5.7,2,0));r.loft(arm,cobalt,[(-1.6,1.5,2.1,0),(0,2.5,2.6,0),(3.1,2.25,2.35,0)])
        r.loft(arm,FLESH,[(3,1.9,2,0),(5.8,1.9,2.1,0),(6.2,1.8,1.9,0)])
        r.loft(arm,steel,[(5.6,2.08,2.28,0),(8.4,2.05,2.25,0),(10.7,2.32,2.4,-.1),(11.3,1.7,2,-.1)])
        leg=r.part(side+'_leg','body',(s*2.4,12,0));r.loft(leg,FLESH,[(0,1.8,1.95,0),(5.8,1.6,1.75,0),(9.7,1.4,1.65,0)])
        r.loft(leg,FLESH,[(9.7,1.8,2.5,-.65),(11.45,1.96,2.9,-.9)])
        r.loft(leg,BROWN,[(11.3,2.1,3.1,-.85),(12,2.1,3.1,-.85)])
        for y,z,d in [(9.7,-.7,1),(10.65,-2.9,.7)]:r.solid(leg,BROWN,-1.95,y,z,3.9,.7,d)
    shield=r.part('shield','left_arm',(1,6,-4),(0,-12,0));r.bevel(shield,WOOD,-4,-5,-.4,8,10,1.5,.7)
    r.bevel(shield,STEEL,-4.5,-5.6,-1.1,9,11.2,.8,.6)
    for i in range(4):r.solid(shield,[c*(.94+i%2*.1) for c in oak],-3.55+i*1.8,-4.55,-1.64,1.7,9.1,.6)
    spear=r.part('spear','right_arm',(-1,8,-2));r.box(spear,WOOD,-.45,-25,-.45,.9,33,.9);r.box(spear,STEEL,-.85,-26,-.85,1.7,2,1.7)
    r.bevel(spear,STEEL,-1.3,-31,-.5,2.6,5,1,.25);r.box(spear,WHITE,-.7,-32.5,-.4,1.4,1.8,.8);r.save()





def hut():
    r=Rig('barbarian_hut');body=r.part('body');r.bevel(body,STONE,-25,20,-22,50,4,44,1)
    # Open front doorway with thick posts and a dark recessed interior.
    r.box(body,BLACK,-18,0,16,36,20,1)
    for x in [-20,17]:
        for z in [-18,15]:r.bevel(body,WOOD,x,-6,z,3,28,3,.5)
    for i in range(8):
        x=-20+i*5;r.box(body,WOOD,x,-3,15,4.8,23,2.7)
        for side in [-1,1]:r.box(body,WOOD,side*18.7,-3,-18+i*4.5,2.7,23,4.2)
    for s in [-1,1]:
        for i in range(2):r.box(body,WOOD,s*12-4+i*4.4,-3,-19,4.1,23,3)
    r.box(body,WOOD,-19,-4,-20,38,5,4)
    for y in [2,15]:
        r.box(body,LEATHER,-20,y,-20.2,40,1.2,.6)
        for x in [-17,-12,12,17]:r.box(body,STEEL,x,y-.1,-20.65,.7,1,.5)
    # Layered roof slopes, uneven thatch fringe, timber ridge and blue war banner.
    for s in [-1,1]:
        roof=r.part('roof_'+str(s),'body',(0,-15,0),(0,0,s*27));r.box(roof,WOOD,min(0,s*27),0,-24,27,2,48)
        for i in range(7):r.box(roof,STRAW,min(0,s*27),-1.2,-24+i*7,27,1.8,6.8)
        for j in range(12):r.box(roof,STRAW,s*26-.8,-.3,-23+j*4,2.5,2.7+(j%3)*.7,3.8)
    r.bevel(body,WOOD,-2,-17,-25,4,3.5,50,.6)
    for z in [-24,22]:r.bevel(body,WOOD,-1,-20,z,2,5,2,.3)
    sign=r.part('sign','body',(0,-6,-22));r.bevel(sign,WOOD,-7,-2,-.5,14,6,1.6,.5)
    r.box(sign,GOLD,-5,-.5,-1.1,10,1.2,.8)
    # Barbarian moustache emblem and studs, no flat text painted on the mesh.
    for s in [-1,1]:
        m=r.part('sign_moustache_'+str(s),'sign',(s*2,.5,-1),(0,0,s*-16));r.bevel(m,HAIR,-2.3,-.5,-.3,4.6,2,1,.25)
    pole=r.part('flagpole','body',(15,-14,8));r.box(pole,WOOD,-.6,-19,-.6,1.2,20,1.2);r.bevel(pole,STEEL,-1,-21,-1,2,2.5,2,.4)
    banner=r.part('banner','flagpole',(0,-18,0));r.box(banner,BLUE,.5,0,-.3,11,8,.6);r.box(banner,GOLD,2,1,-.65,1,6,.5);r.box(banner,GOLD,4.5,2,-.65,4,1,.5);r.box(banner,GOLD,5.5,1,-.65,2,4,.5)
    for s in [-1,1]:
        barrel=r.part('barrel_'+str(s),'body',(s*23,15,-11),(0,0,s*8));r.bevel(barrel,WOOD,-3,-4,-3,6,9,6,.6)
        for y in [-2,2]:r.box(barrel,STEEL,-3.2,y,-3.2,6.4,.7,6.4)
    r.save()

barbarian();recruit();hut()
