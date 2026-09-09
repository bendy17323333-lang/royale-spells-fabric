"""Export the actual Blockbench meshes, exact UVs and articulated rest pose.

The eye planes follow a continuous, shallow brow; there are no separate eyeballs.
Y reflection changes winding and the signs of X/Z rotations, not bone parenting.
"""
from pathlib import Path
import json, math, hashlib, shutil

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[1]
ASSETS=ROOT/'src/main/resources/assets/royalespells'
b=json.loads((HERE/'inferno_dragon.bbmodel').read_text(encoding='utf-8'))
groups={g['uuid']:g for g in b['groups']}
elements={e['uuid']:e for e in b['elements']}
parts=[]
def walk(nodes,parent=None):
    for node in nodes:
        if isinstance(node,str):continue
        g=groups[node['uuid']];p=g['origin'];o=parent['origin'] if parent else [0,24,0]
        part={'name':g['name'],'parent':parent['name'] if parent else '',
              'pivot':[p[0]-o[0],o[1]-p[1],p[2]-o[2]],
              'rotation':[-g['rotation'][0],g['rotation'][1],-g['rotation'][2]],'boxes':[]}
        for child in node['children']:
            if not isinstance(child,str):continue
            e=elements[child];assert e['type']=='mesh' and e['rotation']==[0,0,0]
            faces=[];uvs=[]
            for f in e['faces'].values():
                ids=f['vertices'][::-1]
                if len(ids)==3:ids=ids+[ids[-1]]
                assert len(ids)==4
                xyz=e['vertices'];origin=e['origin']
                pts=[[xyz[i][0]+origin[0]-p[0],p[1]-xyz[i][1]-origin[1],xyz[i][2]+origin[2]-p[2]] for i in ids]
                a=[pts[1][i]-pts[0][i] for i in range(3)];c=[pts[2][i]-pts[0][i] for i in range(3)]
                assert sum((a[(i+1)%3]*c[(i+2)%3]-a[(i+2)%3]*c[(i+1)%3])**2 for i in range(3))>1e-12
                tex=b['textures'][f['texture']];w=tex['uv_width'];h=tex['uv_height']
                uv=[[f['uv'][i][0]/w,f['uv'][i][1]/h] for i in ids]
                assert all(math.isfinite(x) for v in pts for x in v)
                assert all(0<=x<=1 for v in uv for x in v)
                faces.append([round(x,6) for v in pts for x in v]);uvs.append([round(x,7) for v in uv for x in v])
            part['boxes'].append({'faces':faces,'uvs':uvs,'material':0,'emissive':e['name'].startswith('EM_')})
        parts.append(part);walk([n for n in node['children'] if not isinstance(n,str)],g)
walk(b['outliner'])
path=ASSETS/'models/troop/inferno_dragon.json'
path.parent.mkdir(parents=True,exist_ok=True)
path.write_text(json.dumps({'parts':parts},separators=(',',':'))+'\n',encoding='utf-8')
shutil.copyfile(HERE/'inferno-materials.png',ASSETS/'textures/entity/inferno_dragon.png')
report={'source_sha256':hashlib.sha256((HERE/'inferno_dragon.bbmodel').read_bytes()).hexdigest(),
        'parts':len(parts),'meshes':len(elements),'faces':sum(len(e['faces']) for e in elements.values()),
        'runtime_sha256':hashlib.sha256(path.read_bytes()).hexdigest()}
(HERE/'export-report.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,indent=2))
