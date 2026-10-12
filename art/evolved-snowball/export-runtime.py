"""Export the saved Blockbench mesh; never regenerate replacement geometry.

Snow faces, narrow bevels and crystals retain individual plane normals.
Runtime samples Minecraft snow/white concrete without copying Mojang textures.
"""
from pathlib import Path
import collections, hashlib, json, math
HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[1]
SOURCE=HERE/'evolved_snowball.bbmodel'
OUT=ROOT/'src/main/resources/assets/royalespells/models/effects/evolved_snowball.json'
COLORS={'snow':0xe7f3f6,'snow_soft':0xdcecf1,'snow_shadow':0xcbdfe9,'gem_dark':0x482164,'gem_mid':0x6a298e,'gem_light':0x9353bd,'gem_edge':0xb484d6,'gem_top':0x8043a7}
def normal(p):
    a=[p[1][i]-p[0][i] for i in range(3)];b=[p[2][i]-p[0][i] for i in range(3)]
    return [a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]
def unit(n):
    d=math.sqrt(sum(x*x for x in n));assert d>1e-10,'Degenerate face'
    return [round(x/d,7) for x in n]
def flat(points):return [round(c,7) for p in points for c in p]
b=json.loads(SOURCE.read_text(encoding='utf-8'))
output={'source':'art/evolved-snowball/evolved_snowball.bbmodel','snow':[],'crystals':[]}
vertices=faces=0
for e in b['elements']:
    assert e['type']=='mesh' and e['rotation']==[0,0,0]
    snow=e['name'].startswith('SNOW_')
    points={k:[(v[i]+e['origin'][i])/16 for i in range(3)] for k,v in e['vertices'].items()}
    edges=collections.Counter()
    for face in e['faces'].values():
        ids=face['vertices'];n=normal([points[k] for k in ids]);unit(n)
        for a,c in zip(ids,ids[1:]+ids[:1]):edges[tuple(sorted((a,c)))]+=1
    assert set(edges.values())=={2},f'Open or non-manifold mesh: {e["name"]}'
    for face in e['faces'].values():
        ids=face['vertices'];assert 3<=len(ids)<=4
        xyz=[points[k] for k in ids]
        normals=[unit(normal(xyz))]*len(ids)
        uv=[[x/16 for x in face['uv'][k]] for k in ids]
        if len(ids)==3:xyz.append(xyz[-1]);normals.append(normals[-1]);uv.append(uv[-1])
        texture=b['textures'][face['texture']]['name'].removesuffix('.png');assert texture in COLORS,texture
        output['snow' if snow else 'crystals'].append({'positions':flat(xyz),'normals':flat(normals),'uv':flat(uv),'color':COLORS[texture]})
        faces+=1
    vertices+=len(points)
OUT.parent.mkdir(parents=True,exist_ok=True)
OUT.write_text(json.dumps(output,separators=(',',':'))+'\n',encoding='utf-8',newline='\n')
report={'source':SOURCE.name,'sourceSha256':hashlib.sha256(SOURCE.read_bytes()).hexdigest(),'runtime':OUT.relative_to(ROOT).as_posix(),'runtimeSha256':hashlib.sha256(OUT.read_bytes()).hexdigest(),'meshes':len(b['elements']),'vertices':vertices,'faces':faces,'closedManifoldMeshes':True,'normals':'flat square snow faces, narrow bevels and crystal facets','axis':'Blockbench Y-up, one block = 16 units'}
(HERE/'export-report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
print(json.dumps(report,indent=2))
