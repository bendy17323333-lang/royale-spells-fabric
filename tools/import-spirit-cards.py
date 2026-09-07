"""Copy original card PNG bytes; IronCardUi reads their native aspect ratio."""
from pathlib import Path
import hashlib, json, struct, urllib.request

root=Path(__file__).resolve().parents[1]
pin='b4530a1043b213ee2baf9c50a3d0d7fae22c2313'
records=[]
for kind in ('fire','ice','electro','heal'):
    url=f'https://raw.githubusercontent.com/RoyaleAPI/cr-api-assets/{pin}/cards/{kind}-spirit.png'
    data=urllib.request.urlopen(url,timeout=45).read()
    assert data.startswith(b'\x89PNG\r\n\x1a\n')
    size=struct.unpack('>II',data[16:24])
    path=root/f'src/main/resources/assets/royalespells/textures/gui/spell_icons/summon_{kind}_spirit.png'
    path.write_bytes(data)
    records.append(dict(element=kind,path=path.relative_to(root).as_posix(),url=url,sha256=hashlib.sha256(data).hexdigest(),size=list(size),handling='Original PNG bytes, no crop, resize or added border. Original artwork belongs to Supercell.'))
(root/'SPIRIT-CARD-SOURCES.json').write_text(json.dumps(records,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(records,indent=2))
