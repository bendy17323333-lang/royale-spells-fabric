"""Export the MCP-authored .bbmodel files; never constructs replacement geometry.

Blockbench = Y-up, absolute bone pivots, vertices local to mesh origin.
TroopModel = Y-down from 24, relative bone pivots, four vertices per face.
Existing troop materials remain unchanged. New meshes carry their exact face UVs.
Minecraft item OBJ uses the same Y-up coordinates, expressed in blocks.
"""
from pathlib import Path
import json, math, hashlib

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[1]
ASSETS=ROOT/'src/main/resources/assets/royalespells'
KINDS=('fire','ice','electro','heal')

def write(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,ensure_ascii=False,separators=(',',':'))+'\n',encoding='utf-8')

def read(name):
    p=HERE/(name+'.bbmodel'); b=json.loads(p.read_text(encoding='utf-8'))
    assert b['resolution']['width']>0 and b['resolution']['height']>0
    return b

def face_uv(b,f,ids):
    t=b['textures'][f['texture']]
    w=t.get('uv_width',b['resolution']['width']);h=t.get('uv_height',b['resolution']['height'])
    return [[f['uv'][i][0]/w,f['uv'][i][1]/h] for i in ids]

def nondegenerate(points):
    a=[points[1][i]-points[0][i] for i in range(3)];c=[points[2][i]-points[0][i] for i in range(3)]
    return sum((a[(i+1)%3]*c[(i+2)%3]-a[(i+2)%3]*c[(i+1)%3])**2 for i in range(3))>1e-12

def spirit(kind):
    b=read('spirit_'+kind);groups={g['uuid']:g for g in b['groups']};elems={e['uuid']:e for e in b['elements']};parts=[]
    def walk(nodes,parent=None):
        for node in nodes:
            if isinstance(node,str):continue
            g=groups[node['uuid']];p=g['origin'];o=parent['origin'] if parent else [0,24,0]
            part={'name':g['name'],'parent':parent['name'] if parent else '', 'pivot':[p[0]-o[0],o[1]-p[1],p[2]-o[2]],'rotation':[-g['rotation'][0],g['rotation'][1],-g['rotation'][2]],'boxes':[]}
            for child in node['children']:
                if not isinstance(child,str):continue
                e=elems[child];assert e['type']=='mesh';assert e['rotation']==[0,0,0]
                xyz=e['vertices'];origin=e['origin'];faces=[];uvs=[]
                for f in e['faces'].values():
                    ids=f['vertices'][::-1]
                    if len(ids)==3:ids=ids+[ids[-1]]
                    assert len(ids)==4
                    points=[[xyz[i][0]+origin[0]-p[0],p[1]-xyz[i][1]-origin[1],xyz[i][2]+origin[2]-p[2]] for i in ids]
                    assert all(math.isfinite(x) for v in points for x in v)
                    if not nondegenerate(points):continue
                    faces.append([round(x,6) for v in points for x in v])
                    uv=face_uv(b,f,ids);assert all(0<=x<=1 for v in uv for x in v)
                    uvs.append([round(x,7) for v in uv for x in v])
                part['boxes'].append({'faces':faces,'uvs':uvs,'material':0,'emissive':e['name'].startswith(('EM_','FLAME_')),'flame':e['name'].startswith('FLAME_')})
            parts.append(part);walk([c for c in node['children'] if not isinstance(c,str)],g)
    walk(b['outliner']);assert len(parts)>=6
    write(ASSETS/f'models/troop/spirit_{kind}.json',{'parts':parts})
    return sum(len(p['boxes']) for p in parts),sum(len(m['faces']) for p in parts for m in p['boxes'])

def staff(kind):
    b=read('furnace_staff_'+kind);lines=['# Exported from MCP-authored Blockbench model','mtllib furnace_staff.mtl'];n=0;faces=0
    for e in b['elements']:
        assert e['type']=='mesh' and e['rotation']==[0,0,0]
        flame=e['name'].startswith('FLAME_');lines+=['o '+e['name'],'usemtl '+('flame' if flame else 'ice' if e['name'].startswith('ICE_') else 'glow' if e['name'].startswith('EM_') else 'steel')]
        for f in e['faces'].values():
            if not nondegenerate([e['vertices'][i] for i in f['vertices']]):continue
            ids=[]
            for key in f['vertices']:
                v=[(e['vertices'][key][i]+e['origin'][i])/16 for i in range(3)];uv=[x*(2 if flame else 1) for x in face_uv(b,f,[key])[0]]
                n+=1;ids.append(f'{n}/{n}');lines+=['v '+' '.join(f'{x:.7f}' for x in v),'vt '+' '.join(f'{x:.7f}' for x in uv)]
            lines.append('f '+' '.join(ids));faces+=1
    (ASSETS/f'models/item/furnace_staff_{kind}.obj').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    modelpath=ASSETS/f'models/item/furnace_staff_{kind}.json';model=json.loads(modelpath.read_text());model['textures']['particle']='royalespells:entity/spirit_materials';model['render_type']='minecraft:cutout';model['flip_v']=False
    model['display']['gui']['scale']=[.36]*3
    model['display']['firstperson_righthand']['scale']=[.38]*3;model['display']['firstperson_lefthand']['scale']=[.38]*3
    write(modelpath,model);return len(b['elements']),faces

if __name__=='__main__':
    report={}
    for k in KINDS:report['spirit_'+k]=dict(zip(('meshes','faces'),spirit(k)))
    for k in KINDS:report['furnace_staff_'+k]=dict(zip(('meshes','faces'),staff(k)))
    (ASSETS/'models/item/furnace_staff.mtl').write_text('newmtl steel\nKa 0 0 0\nKd 1 1 1\nmap_Kd royalespells:entity/spirit_materials\n\nnewmtl glow\nKa 1 1 1\nKd 1 1 1\nmap_Kd royalespells:entity/spirit_materials\n\nnewmtl flame\nKa 1 1 1\nKd 1 1 1\nmap_Kd royalespells:entity/spirit_flame\n',encoding='utf-8')
    with (ASSETS/'models/item/furnace_staff.mtl').open('a',encoding='utf-8') as f:f.write('\nnewmtl ice\nKa 0 0 0\nKd 1 1 1\nmap_Kd royalespells:entity/spirit_ice_mineclash\n')
    for k in report:report[k]['source_sha256']=hashlib.sha256((HERE/(k+'.bbmodel')).read_bytes()).hexdigest()
    write(HERE/'export-report.json',report);print(json.dumps(report,indent=2))
