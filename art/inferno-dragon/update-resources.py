from pathlib import Path
import json, shutil
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[1]
ASSETS=ROOT/'src/main/resources/assets/royalespells'
messages={
 'item.royalespells.inferno_dragon':('地狱飞龙','Inferno Dragon'),
 'entity.royalespells.inferno_dragon':('地狱飞龙','Inferno Dragon'),
 'troop.royalespells.inferno_dragon':('部署飞行幼龙，以逐步升温的光束攻击单个敌人，可对空对地。','Deploy a flying dragon with a heating single-target beam; attacks air and ground.'),
 'ironspell.royalespells.inferno_dragon':('皇室 · 地狱飞龙','Royale · Inferno Dragon'),
 'ui.royalespells.inferno_health':('召唤生命值：%s','Summon health: %s'),
 'ui.royalespells.inferno_damage':('每 0.4 秒伤害：%s → %s → %s','Damage every 0.4s: %s → %s → %s'),
 'ui.royalespells.inferno_rules':('对空对地；锁定满 2 / 4 秒升温，打断归零。离地悬停 3.5 格；水平射程 4 格，上下射程 6 格；存在 45 秒。','Air and ground. Heat increases after 2 / 4 seconds; interruptions reset heat. Hovers 3.5 blocks above terrain. Horizontal range: 4 blocks; vertical reach: 6. Lifetime: 45s.'),
 'subtitles.royalespells.inferno_dragon_deploy':('地狱飞龙：部署','Inferno Dragon deploys'),
 'subtitles.royalespells.inferno_dragon_wing':('地狱飞龙：振翅','Inferno Dragon flaps'),
 'subtitles.royalespells.inferno_dragon_beam':('地狱飞龙：持续喷射','Inferno Dragon fires'),
 'death.attack.royalespells.inferno_dragon':('%1$s 被 %2$s 的地狱飞龙熔化了','%1$s was melted by %2$s\'s Inferno Dragon'),
}
for index,locale in enumerate(('zh_cn','en_us')):
    path=ASSETS/f'lang/{locale}.json';data=json.loads(path.read_text(encoding='utf-8'));data.update({k:v[index] for k,v in messages.items()})
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
recipe={'type':'minecraft:crafting_shapeless','ingredients':[{'item':x} for x in ['minecraft:paper','minecraft:iron_helmet','minecraft:blaze_rod','minecraft:phantom_membrane','minecraft:copper_ingot']],
 'result':{'id':'royalespells:inferno_dragon','count':1},'neoforge:conditions':[{'type':'neoforge:not','value':{'type':'neoforge:mod_loaded','modid':'irons_spellbooks'}}]}
(ROOT/'src/main/resources/data/royalespells/recipe/inferno_dragon.json').write_text(json.dumps(recipe,indent=2)+'\n')
# Viewer and original ImageGen prompt live alongside this script. Resource
# refreshes must not overwrite the current model tools with an old V1 export.
print('Added native aspect-ratio card, bilingual descriptions, original sound subtitles and conditional standalone recipe.')
