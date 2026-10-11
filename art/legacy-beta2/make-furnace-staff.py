"""UV-mapped faceted cauldron staff. Uses the unchanged image_gen atlas, no vanilla textures.

NeoForge's built-in OBJ loader keeps curved/faceted silhouettes outside the
axis-aligned cube restriction. Geometry is deterministic; bitmap artwork is not
generated, recoloured, cropped or edited by this script.
"""
from pathlib import Path
import math,json

B=Path(__file__).resolve().parents[1]/'src/main/resources/assets/royalespells'
TAU=math.tau

class Mesh:
    def __init__(self):self.lines=['# Furnace staff - dedicated image_gen atlas','mtllib furnace_staff.mtl'];self.count=0
    def face(self,points,tile,uv=None,glow=False):
        self.lines.append('usemtl '+('glow' if glow else 'steel'))
        ids=[]
        if uv is None:uv=[(.02,.02),(.02,.98),(.98,.98),(.98,.02)][:len(points)]
        for p,(u,v) in zip(points,uv):
            self.count+=1;ids.append(f'{self.count}/{self.count}')
            self.lines.append('v '+' '.join(f'{n/16:.7f}' for n in p))
            self.lines.append(f'vt {(tile%4+u)/4:.7f} {(tile//4+v)/4:.7f}')
        self.lines.append('f '+' '.join(ids))
    def lathe(self,rings,tile,sides=12,cx=8,cz=8,glow=False,inside=False):
        # Rings: (height,radius). UV is continuous vertically across each panel.
        low=min(y for y,r in rings);height=max(.1,max(y for y,r in rings)-low)
        for j in range(len(rings)-1):
            y,r=rings[j];Y,R=rings[j+1]
            for i in range(sides):
                a=i*TAU/sides;b=(i+1)*TAU/sides
                points=[(cx+r*math.cos(a),y,cz+r*math.sin(a)),(cx+R*math.cos(a),Y,cz+R*math.sin(a)),(cx+R*math.cos(b),Y,cz+R*math.sin(b)),(cx+r*math.cos(b),y,cz+r*math.sin(b))]
                u=.06+(i%4)*.2;uv=[(u, .94-(y-low)/height*.88),(u,.94-(Y-low)/height*.88),(u+.18,.94-(Y-low)/height*.88),(u+.18,.94-(y-low)/height*.88)]
                if inside:points.reverse();uv.reverse()
                self.face(points,tile,uv,glow)
    def disk(self,y,r,tile,cx=8,cz=8,glow=False,sides=12):
        for i in range(sides):
            a=i*TAU/sides;b=(i+1)*TAU/sides
            self.face([(cx,y,cz),(cx+r*math.cos(b),y,cz+r*math.sin(b)),(cx+r*math.cos(a),y,cz+r*math.sin(a))],tile,[(.5,.5),(.5+.46*math.cos(b),.5+.46*math.sin(b)),(.5+.46*math.cos(a),.5+.46*math.sin(a))],glow)
    def bubble(self,x,y,z,r,tile):
        self.lathe([(y-r*.55,r*.65),(y,r),(y+r*.65,r*.7),(y+r*.85,.05)],tile,8,x,z,True)

def build(kind,index):
    m=Mesh();liquid=8+index
    # Long, narrow wooden handle, modest forge collar, no broad plunger base.
    m.lathe([(-12,.57),(-10,.7),(2,.76),(12,.82),(18,.68)],4,8)
    m.disk(18,.68,12)
    m.lathe([(-12.3,.64),(-12,.9),(-9.9,.9),(-9.6,.73)],0,8)
    m.lathe([(1,.83),(5.8,.83)],5,8)
    m.lathe([(15.7,.84),(16,.99),(17.5,.99),(17.9,1.27)],2,8)
    # Bowl body: narrow curved bottom, deep belly, rolled open rim.
    m.lathe([(17.8,1.27),(18.4,2.15),(19.6,3.12),(21,3.55),(24.8,3.65)],0)
    m.lathe([(24.6,3.68),(24.95,3.87),(25.5,3.87),(25.75,3.56),(25.4,3.24)],2)
    m.lathe([(23.3,2.94),(25.4,3.24)],7,inside=True)
    m.disk(24.35,3.15,liquid,glow=True)
    # Soot-tarnished band just below the bright rim, with visible small rivets.
    m.lathe([(23.6,3.67),(24.55,3.67)],6)
    for angle in (-TAU/4,-TAU/12,TAU/4):
        x=8+3.72*math.cos(angle);z=8+3.72*math.sin(angle);tangent=(-math.sin(angle),math.cos(angle));w=.48
        m.face(list(reversed([(x+tangent[0]*w,22.8,z+tangent[1]*w),(x+tangent[0]*w,23.65,z+tangent[1]*w),(x-tangent[0]*w,23.65,z-tangent[1]*w),(x-tangent[0]*w,22.8,z-tangent[1]*w)])),3)
    # Thick syrup drips follow the curved bowl. They end in a rounded bulb,
    # rather than identical rectangular stripes floating in front of the cup.
    def surface(y):
        profile=[(17.8,1.27),(18.4,2.15),(19.6,3.12),(21,3.55),(24.6,3.65),(24.95,3.87),(25.8,3.87)]
        for (a,r),(b,R) in zip(profile,profile[1:]):
            if a<=y<=b:return r+(R-r)*(y-a)/(b-a)+.14
        return 4.0
    for angle,length,width in [(-1.42,6.6,1.05),(-2.45,3.8,.55),(-.55,4.8,.55)]:
        bottom=25.3-length
        heights=[y for y in (25.7,25.2,24.7,24.2,23.5,22.5,21.5,21,20.5,20,19.6,19.2) if y>bottom+.4]+[bottom+.35,bottom]
        rows=[(y,width*(.2 if y==bottom else .95 if y==bottom+.35 else .7+.12*math.sin(y*2)),surface(y)) for y in heights]
        for j in range(len(rows)-1):
            y,w,r=rows[j];Y,W,R=rows[j+1];points=[]
            for h,span,rad,sign in [(y,w,r,-1),(Y,W,R,-1),(Y,W,R,1),(y,w,r,1)]:
                a=angle+sign*span/rad/2;points.append((8+rad*math.cos(a),h,8+rad*math.sin(a)))
            m.face(list(reversed(points)),liquid,glow=True)
    for x,y,z,r in [(6.6,24.55,7.4,.53),(9.2,24.5,9,.34),(8.9,24.55,6.2,.22)]:m.bubble(x,y,z,r,liquid)
    # A small iron loop on the side gives the head the original cauldron silhouette.
    for i in range(8):
        a=i*TAU/8;b=(i+1)*TAU/8
        points=[(11.9,22+1.12*math.cos(a),8+1.12*math.sin(a)),(11.9,22+.7*math.cos(a),8+.7*math.sin(a)),(11.9,22+.7*math.cos(b),8+.7*math.sin(b)),(11.9,22+1.12*math.cos(b),8+1.12*math.sin(b))]
        m.face(points,1);m.face(list(reversed([(x+.25,y,z) for x,y,z in points])),0)
    dest=B/'models/item';dest.mkdir(parents=True,exist_ok=True)
    (dest/f'furnace_staff_{kind}.obj').write_text('\n'.join(m.lines)+'\n',encoding='utf-8')
    display={
        'gui':{'rotation':[15,-35,-15],'translation':[0,0,0],'scale':[.43]*3},
        'thirdperson_righthand':{'rotation':[75,0,0],'translation':[0,2.5,1],'scale':[.7]*3},
        'thirdperson_lefthand':{'rotation':[75,0,0],'translation':[0,2.5,1],'scale':[.7]*3},
        'firstperson_righthand':{'rotation':[15,170,-8],'translation':[0,-1,-1],'scale':[.42]*3},
        'firstperson_lefthand':{'rotation':[15,-170,8],'translation':[0,-1,-1],'scale':[.42]*3},
        'ground':{'translation':[0,4,0],'scale':[.3]*3},'fixed':{'scale':[.43]*3}}
    model={'loader':'neoforge:obj','model':f'royalespells:models/item/furnace_staff_{kind}.obj','automatic_culling':False,'shade_quads':True,'flip_v':False,'emissive_ambient':True,'textures':{'particle':'royalespells:item/furnace_staff_atlas'},'display':display}
    (dest/f'furnace_staff_{kind}.json').write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
    print(kind,'vertices',m.count,'faces',sum(s.startswith('f ') for s in m.lines))

if __name__=='__main__':
    dest=B/'models/item';dest.mkdir(parents=True,exist_ok=True)
    (dest/'furnace_staff.mtl').write_text('newmtl steel\nKa 0 0 0\nKd 1 1 1\nmap_Kd royalespells:item/furnace_staff_atlas\n\nnewmtl glow\nKa 1 1 1\nKd 1 1 1\nmap_Kd royalespells:item/furnace_staff_atlas\n',encoding='utf-8')
    for i,kind in enumerate(('fire','ice','electro','heal')):build(kind,i)
