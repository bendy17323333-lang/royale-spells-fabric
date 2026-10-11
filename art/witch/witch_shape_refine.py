"""Sculpted, faceted Witch silhouette. All cloth panels are closed thick solids.
The face is an ImageGen albedo on a shallow cheek/nose/jaw mesh, not a square plane.
"""
import math

def quads(r,part,name,verts,faces,mat,**kw):
    r.solid(part,name,[[q for i in face for q in verts[i]] for face in faces],mat,**kw)

def loft(r,part,name,rings,mat):
    n=len(rings[0]); faces=[]
    for a,b in zip(rings,rings[1:]):
        for i in range(n):
            j=(i+1)%n;faces.append([*a[j],*a[i],*b[i],*b[j]])
    for ring,reverse in [(rings[0],False),(rings[-1],True)]:
        c=[sum(v[k] for v in ring)/n for k in range(3)]
        for i in range(n):
            a,b=ring[i],ring[(i+1)%n]
            faces.append([*c,*(b if reverse else a),*(a if reverse else b),*c])
    r.solid(part,name,faces,mat)

def oval(y,rx,rz,z=0,x=0):
    return [[x+rx*a,y,z+rz*b] for a,b in [(-.65,-1),(.65,-1),(1,-.58),(1,.58),(.65,1),(-.65,1),(-1,.58),(-1,-.58)]]

def bevel(r,part,name,pos,size,mat,b=.22):
    x,y,z=pos;w,h,d=size;cx=x+w/2;cz=z+d/2
    loft(r,part,name,[oval(y,w/2-b,d/2-b,cz,cx),oval(y+b,w/2,d/2,cz,cx),
        oval(y+h-b,w/2,d/2,cz,cx),oval(y+h,w/2-b,d/2-b,cz,cx)],mat)

def panel(r,part,name,width,height,mat,depth=.24,flare=1.3,curve=.45):
    # Five transverse vertices form one large fold plus two quieter side folds.
    # The fold deepens down the skirt, instead of merely painting a pleat stripe.
    levels=[0,.22,.55,.83,1];xs=[-1,-.48,0,.46,1];front=[]
    for v in levels:
        ring=[]
        for i,u in enumerate(xs):
            y=height*v+(max(0,v-.83)/.17)*([-.4,.35,.9,-.1,-.65][i])
            z=-curve*v*v-([0,.20,.56,.17,0][i])*(.55+.9*v)
            ring.append([u*width/2*(1+(flare-1)*v),y,z])
        front.append(ring)
    faces=[]
    for j in range(len(front)-1):
        for i in range(4):
            a,b,c,d=front[j][i],front[j][i+1],front[j+1][i+1],front[j+1][i]
            faces.append([*b,*a,*d,*c])
            faces.append([*([a[0],a[1],a[2]+depth]),*([b[0],b[1],b[2]+depth]),
                *([c[0],c[1],c[2]+depth]),*([d[0],d[1],d[2]+depth])])
    boundary=front[0]+[row[-1] for row in front[1:]]+list(reversed(front[-1][:-1]))+[row[0] for row in reversed(front[1:-1])]
    for a,b in zip(boundary,boundary[1:]+boundary[:1]):faces.append([*a,*b,b[0],b[1],b[2]+depth,a[0],a[1],a[2]+depth])
    r.solid(part,name,faces,mat)

def curved_tube(r,part,name,points,radii,mat,sides=6):
    rings=[]
    for p,rad in zip(points,radii):
        rings.append([[p[0]+math.cos(i*math.tau/sides)*rad,p[1],p[2]+math.sin(i*math.tau/sides)*rad*.8] for i in range(sides)])
    loft(r,part,name,rings,mat)

def refine_shape(r,evo):
    # Replace the full head and old crown. Use an open shell round a faceted face.
    r.parts['head']['boxes']=[]
    for n in list(r.parts):
        if n.startswith('horn_'):del r.parts[n]
    widths=[2.95,3.24,3.30,3.23,3.12,2.93,2.55,2.10]
    ys=[-6.6,-5.3,-4.15,-3.25,-2.4,-1.5,-.5,.45]
    xs=[-1,-.75,-.45,-.16,0,.16,.45,.75,1]
    rows=[];uv=[]
    for y,w in zip(ys,widths):
        row=[];tex=[];v=(y+6.6)/7.05
        for x in xs:
            z=-3.65+1.18*abs(x)**3
            # Nose bridge and tip are part of the continuous mesh, not a box.
            z-=.43*math.exp(-((x/.19)**2+((v-.60)/.14)**2))
            z+=.11*math.exp(-(((abs(x)-.5)/.25)**2+((v-.43)/.10)**2))
            z-=.10*math.exp(-(((abs(x)-.58)/.22)**2+((v-.62)/.13)**2))
            row.append([x*w,y,z]);tex.append([(x+1)/2,v])
        rows.append(row);uv.append(tex)
    faces=[];uvs=[]
    for j in range(len(rows)-1):
        for i in range(len(xs)-1):
            ids=[(j,i+1),(j,i),(j+1,i),(j+1,i+1)]
            faces.append([q for a,b in ids for q in rows[a][b]])
            uvs.append([q for a,b in ids for q in uv[a][b]])
    r.solid('head','sculpted_face',faces,7)
    r.parts['head']['boxes'][-1].update(uvs=uvs,face_texture=True)
    # Side skull and chin stay behind the textured surface; none protrude as eyes.
    loft(r,'head','face_side_planes',[oval(-6.6,2.94,2.48,.12),oval(-4,3.24,2.5,.15),
        oval(-1.5,2.92,2.25,.13),oval(.46,2.08,1.67,.10)],7)
    # Remove the front faces of this backing volume; the sculpted albedo closes it.
    backing=r.parts['head']['boxes'][-1]
    keep=[(f,u) for f,u in zip(backing['faces'],backing['uvs']) if not all(f[k]<-.9 for k in [2,5,8,11])]
    backing['faces']=[f for f,u in keep];backing['uvs']=[u for f,u in keep];backing['color']=[.83,.86,1]
    # A crown-shaped hood cross section, with depth changes down its back.
    outer=[[-3.1,1.7],[-4.05,-1.6],[-4.15,-4.9],[-3.4,-7.0],[-1.5,-8.5],[1.5,-8.5],[3.4,-7.0],[4.15,-4.9],[4.05,-1.6],[3.1,1.7]]
    inner=[[-2.55,.95],[-3.40,-1.7],[-3.43,-4.8],[-2.85,-6.4],[-1.35,-7.30],[1.35,-7.30],[2.85,-6.4],[3.43,-4.8],[3.40,-1.7],[2.55,.95]]
    hf=[];rim=[]
    for i in range(len(outer)-1):
        a,b=outer[i],outer[i+1];c,d=inner[i],inner[i+1]
        hf.append([*a,-3.18,*b,-3.18,b[0]*.92,b[1]+.55,3.0,a[0]*.92,a[1]+.55,3.0])
        rim.append([*b,-3.19,*a,-3.19,*c,-3.72,*d,-3.72])
    r.solid('head','hood_sculpted_shell',hf,0)
    r.solid('head','hood_opening_rim',rim,4 if evo else 12)
    # Back folds are a shaped cap and descending cowl, not an upright box.
    hb=[]
    # Include the lower closing edge: omitting it left a triangular bare-skin
    # hole in the back cap despite the front opening looking correct.
    for i in range(len(outer)):
        a,b=outer[i],outer[(i+1)%len(outer)]
        hb.append([0,-3.1,3.8,a[0]*.92,a[1]+.55,3.0,b[0]*.92,b[1]+.55,3.0,0,-3.1,3.8])
    r.solid('head','hood_rear_facets',hb,0)
    r.part('hood_cowl','body',(0,.8,2.9),(12,0,0))
    panel(r,'hood_cowl','hood_drape',5.7,5.0,0,.36,.80,.45)
    # Broad side locks, not six disconnected comb teeth across the forehead.
    # Evolution's frontlet needs clear forehead space, as in its original art.
    fringe=[(-2.95,1.15,1.65),(1.90,1.05,1.90)] if evo else [(-2.90,1.55,1.30),(-1.40,2.55,1.05),(1.25,1.50,1.70)]
    for i,(x,w,h) in enumerate(fringe):
        r.taper('head','fringe_'+str(i),(x,x+w),(x+.12,x+w-.08),-6.6,-6.6+h,-3.71,.30,0)
    # Real neck beneath the chin, articulated shoulder layer and fitted dark chest.
    r.parts['body']['boxes']=[]
    bevel(r,'body','neck',(-1.25,-.15,-.8),(2.5,2.6,2.6),7,.2)
    loft(r,'body','fitted_bodice',[oval(1.3,3.65,2.20,.1),oval(4.1,3.45,2.25,.1),
        oval(7.4,2.85,1.8,.05),oval(10,3.35,2.12,.12)],8 if evo else 1)
    # Chamfered chest plates have seams and small warm highlights, not noisy gold.
    r.taper('body','chest_front',(-3.05,3.05),(-2.45,2.45),2.05,8.4,-2.18,.25,5 if evo else 1)
    r.parts['body']['boxes'][-1]['color']=[.43,.43,.48] if evo else [1,1,1]
    r.box('body','collar_seam',(-2.05,1.55,-2.20),(4.1,.42,.35),4 if evo else 12)
    bevel(r,'body','belt',(-3.35,9.05,-2.3),(6.7,1.12,4.85),14,.12)
    r.part('waist_gem','body',(0,9.55,-2.55),(0,0,45))
    bevel(r,'waist_gem','waist_setting',(-.73,-.73,-.18),(1.46,1.46,.58),4,.15)
    bevel(r,'waist_gem','waist_inlay',(-.46,-.46,-.27),(.92,.92,.25),2,.08)
    for n in ['lapel_-1','lapel_1']:
        if n in r.parts:r.parts[n]['boxes']=[]
    for side in [-1,1]:
        suf='right' if side<0 else 'left'
        shoulder=r.part('mantle_'+suf,'body',(side*2.3,1.65,.15),(0,0,side*13))
        bevel(r,shoulder,'mantle_'+suf,(-1.15,-.3,-2.1),(2.8,.55,4.8),4 if evo else 0,.12)
        arm=suf+'_arm';r.parts[arm]['pivot']=[side*4.15,2.15,.1];r.parts[arm]['boxes']=[]
        bevel(r,arm,'upper_arm_'+suf,(-1.35,.0,-1.32),(2.7,4.45,2.64),7,.28)
        if not evo:
            bevel(r,arm,'pauldron_'+suf,(-1.72,-.6,-1.7),(3.44,2.0,3.4),4,.42)
            bevel(r,arm,'pauldron_inner_'+suf,(-1.52,1,-1.5),(3.04,.65,3.0),14,.16)
        fore=suf+'_forearm';r.parts[fore]['boxes']=[]
        bevel(r,fore,'vambrace_'+suf,(-1.28,0,-1.2),(2.56,3.62,2.45),1 if evo else 5,.24)
        r.parts[fore]['boxes'][-1]['color']=[.65,.90,1.35] if evo else [.55,.60,.68]
        for j,y in enumerate([.05,2.96]):bevel(r,fore,'cuff_'+suf+str(j),(-1.40,y,-1.32),(2.8,.57,2.68),4,.14)
        # Palm and separate curled phalanges grip the brown pole, leaving gaps.
        bevel(r,fore,'palm_'+suf,(-1.03,3.55,-.9),(2.06,1.65,1.85),7,.20)
        for finger in range(3):
            x=-.92+finger*.62
            bevel(r,fore,'finger_'+suf+str(finger),(x,3.95,-1.82),(.49,.92,1.35),7,.12)
        r.part('thumb_'+suf,fore,(-1.07 if side<0 else 1.07,3.67,-1.05),(10,0,side*28))
        bevel(r,'thumb_'+suf,'thumb_'+suf,(-.30,0,-.75),(.60,1.14,1.1),7,.12)
        # Two layers of folded front cloth plus an outward side fold.
        robe='robe_'+suf;r.parts[robe]['boxes']=[];r.parts[robe]['pivot']=[side*1.72,10,-2.1]
        r.parts[robe]['rotation']=[0,side*5,side*3]
        panel(r,robe,'front_fold_'+suf,2.55,12.2,2,.30,1.45,.55)
        trim=r.part('robe_trim_'+suf,robe,(side*1.12,.1,-.08))
        panel(r,trim,'edge_braid_'+suf,.17,11.6,4 if evo else 12,.14,1.1,.55)
        q='robe_side_'+suf;r.parts[q]['boxes']=[];r.parts[q]['pivot']=[side*3.25,10,.15];r.parts[q]['rotation']=[0,side*-60,side*-5]
        panel(r,q,'side_fold_'+suf,2.95,12.55,0,.32,1.3,.60)
        leg=suf+'_leg';r.parts[leg]['boxes']=[]
        bevel(r,leg,'stocking_'+suf,(-1,0,-.85),(2,8.6,2.15),1,.2)
        bevel(r,leg,'boot_'+suf,(-1.08,8,-1.73),(2.16,2.85,3.3),13,.24)
        r.box(leg,'sole_'+suf,(-1.06,10.72,-1.7),(2.12,.23,3.25),8)
    loft(r,'body','inner_petticoat',[oval(10,2.95,1.8,.25),oval(15,3.25,2.05,.3),oval(21.9,3.85,2.3,.4)],1)
    # Rear cloak split into independently moving lower panels with shaped tips.
    r.parts['cape']['boxes']=[];r.parts['cape']['pivot']=[0,2.1,2.45]
    panel(r,'cape','cape_shoulders',6.65,7.5,0,.33,1.12,-.2)
    r.parts['cape_tail']['boxes']=[];r.parts['cape_tail']['pivot']=[0,7.4,.1]
    panel(r,'cape_tail','cape_central',4.4,13.4,0,.32,1.28,-.65)
    for side in [-1,1]:
        cp=r.part('cape_ribbon_'+str(side),'cape',(side*2.9,5.2,.12),(0,side*-12,side*-5))
        panel(r,cp,'cloak_tip_'+str(side),1.7,15.1,0,.26,1.2,-.7)
        seam=r.part('cape_gold_'+str(side),cp,(side*.77,0,-.04))
        panel(r,seam,'cloak_seam_'+str(side),.16,14.8,4 if evo else 12,.12,1,-.7)
    if evo:
        # Real side-rooted horns curve out first, then turn upward; no forehead beam.
        for s in [-1,1]:
            curved_tube(r,'head','gilded_horn_'+str(s),[[s*3.5,-5.7,-.7],[s*4.9,-6.6,-.55],
                [s*5.45,-7.7,-.3],[s*5.2,-8.8,-.1],[s*4.5,-10.0,.1]], [1.03,.97,.74,.47,.045],4)
        # A small beast-skull frontlet with oblique negative eye spaces.
        r.part('crown_skull','head',(0,-7.2,-3.85))
        bevel(r,'crown_skull','crown_forehead',(-1.25,-1.2,-.3),(2.5,.65,.85),4,.17)
        for s in [-1,1]:
            p=r.part('crown_brow_'+str(s),'crown_skull',(s*.78,-.58,-.05),(0,0,s*24))
            bevel(r,p,'brow_'+str(s),(-.75,-.15,-.38),(1.5,.35,.65),4,.1)
            r.segment('crown_skull','crown_cheek_'+str(s),(s*1.32,-.40,0),(s*.44,.24,0),.30,.60,4)
        r.taper('crown_skull','crown_beak',(-.34,.34),(-.09,.09),-.68,.8,-.54,.66,14)
        # Broaden only the small beast-skull frontlet, keeping the side horns
        # and hood proportions intact. Both sockets remain actual openings.
        for name,p in r.parts.items():
            if name=='crown_skull' or p['parent']=='crown_skull':
                if name!='crown_skull':p['pivot'][0]*=1.4
                for b in p['boxes']:
                    for face in b['faces']:
                        for index in [0,3,6,9]:face[index]*=1.4
    # Subtle skin hue matching for sides/neck/hands; face itself uses its own image.
    for p in r.parts.values():
        for b in p['boxes']:
            if b['material']==7 and not b.get('face_texture'):b['color']=[.82,.86,1.0]
