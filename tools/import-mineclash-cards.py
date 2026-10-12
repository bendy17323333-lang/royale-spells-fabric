"""Import unmodified Supercell card portraits for optional MineClash egg wrappers.
No MineClash model, bytecode or balancing data is copied into this mod.
"""
from pathlib import Path
import hashlib, json, struct, urllib.request

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/royalespells'
PIN = 'b4530a1043b213ee2baf9c50a3d0d7fae22c2313'
CARDS = {
    'knight_zombie': ('knight', '骑士僵尸', 'Knight Zombie'),
    'skeleton': ('skeletons', '骷髅兵', 'Skeleton'),
    'royal_recruits': ('royal-recruits', '皇家卫队', 'Royal Recruit'),
    'ice_spirit': ('ice-spirit', '冰精灵', 'Ice Spirit'),
    'elite_musketeer': ('three-musketeers', '精英火枪手', 'Elite Musketeer'),
    'zappies': ('zappies', '电击车', 'Zappy'),
    'pekka': ('pekka', '皮卡超人', 'P.E.K.K.A'),
    'mini_pekka': ('mini-pekka', '迷你皮卡', 'Mini P.E.K.K.A'),
    'super_mini_pekka': ('super-mini-pekka', '超级迷你皮卡', 'Super Mini P.E.K.K.A'),
    'golem': ('golem', '戈仑石人', 'Golem'),
    'valkyrie': ('valkyrie', '女武神', 'Valkyrie'),
    'monk': ('monk', '圣殿春秋', 'Monk'),
    'mega_knight': ('mega-knight', '超级骑士', 'Mega Knight'),
    'guard': ('guards', '骷髅守卫', 'Guard'),
    'general_gerry': ('skeleton-army-ev1', '骷髅将军', 'General Gerry'),
}

records=[]
model=(ASSETS/'models/item/witch.json').read_bytes()
for entity,(art,zh,en) in CARDS.items():
    url=f'https://raw.githubusercontent.com/RoyaleAPI/cr-api-assets/{PIN}/cards/{art}.png'
    data=urllib.request.urlopen(url,timeout=45).read()
    assert data.startswith(b'\x89PNG\r\n\x1a\n'),url
    item='mineclash_'+entity
    path=ASSETS/f'textures/card/{item}.png'
    path.write_bytes(data)
    (ASSETS/f'models/item/{item}.json').write_bytes(model)
    records.append(dict(item='royalespells:'+item,egg=f'mineclash:{entity}_spawn_egg',
        portrait=art,path=path.relative_to(ROOT).as_posix(),url=url,
        sha256=hashlib.sha256(data).hexdigest(),size=list(struct.unpack('>II',data[16:24])),
        handling='Original PNG bytes. Artwork belongs to Supercell, excluded from MIT. Cosmetic portrait does not change MineClash spawn count or behavior.'))
    for locale,label in [('zh_cn',zh),('en_us',en)]:
        lp=ASSETS/f'lang/{locale}.json';lang=json.loads(lp.read_text(encoding='utf-8'))
        lang['item.royalespells.'+item]='MineClash · '+label
        lp.write_text(json.dumps(lang,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
(ROOT/'MINECLASH-CARD-SOURCES.json').write_text(json.dumps(records,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f'Imported {len(records)} original card portraits; native MineClash behavior unchanged.')
