"""Keep spell icons, translations and legacy recipe conditions aligned with the native registry."""
from pathlib import Path
import json
import re
import shutil

root = Path(__file__).resolve().parents[1]
assets = root / 'src/main/resources/assets/royalespells'
source = (root / 'src/main/java/dev/royalespells/iron/IronSpellProfile.java').read_text(encoding='utf-8')
ids = [name.lower() for name in re.findall(r'^    ([A-Z_]+)\("', source, re.M)]
assert len(ids) == 29
icons = assets / 'textures/gui/spell_icons'
icons.mkdir(parents=True, exist_ok=True)
for name in ids:
    shutil.copyfile(assets / f'textures/card/{name}.png', icons / f'{name}.png')
    recipe = root / f'src/main/resources/data/royalespells/recipe/{name}.json'
    data = json.loads(recipe.read_text(encoding='utf-8'))
    data['neoforge:conditions'] = [{'type': 'neoforge:not', 'value': {'type': 'neoforge:mod_loaded', 'modid': 'irons_spellbooks'}}]
    recipe.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

messages = {
    'itemGroup.royalespells.iron_scrolls': ('皇室法术 · 铁魔法卷轴', 'Royale Spells · Iron Scrolls'),
    'message.royalespells.use_iron_scroll': ('同装铁魔法时，生存施法请使用卷轴或抄写后的法术书；原卡牌供创造模式与片场使用。', 'With Iron\'s installed, use scrolls or an inscribed spellbook in survival. Cards are for creative mode and recording scenes.'),
    'message.royalespells.iron_stunned': ('眩晕或冻结期间无法施法。', 'You cannot cast while stunned or frozen.'),
    'message.royalespells.iron_no_mirror': ('先成功施放一种皇室或铁魔法法术，才能使用镜像。', 'Successfully cast a Royale or Iron\'s spell before using Mirror.'),
    'message.royalespells.iron_mirror_locked': ('原法术被禁用或尚未解锁，无法镜像。', 'The original spell is disabled or has not been learned.'),
    'message.royalespells.iron_finish_recast': ('请先结束原法术的连发，再开始镜像。', 'Finish the original spell\'s recasts before starting a mirrored cast.'),
    'message.royalespells.iron_mirror_mana': ('魔力不足：镜像需要原法术提高一级后的消耗，以及镜像的附加消耗。', 'Not enough mana: Mirror costs the original spell at one higher level plus its surcharge.'),
    'ui.royalespells.effect_power': ('效果强度：%s%%（受等级、法术强度与学派装备影响）', 'Effect strength: %s%% (level, spell power and school equipment)'),
    'ui.royalespells.radius': ('作用半径：%s 格', 'Effect radius: %s blocks'),
    'ui.royalespells.duration': ('持续时间：%s 秒', 'Duration: %s seconds'),
    'ui.royalespells.self_target': ('潜行施法：以自身为中心。', 'Sneak while casting to center the effect on yourself.'),
    'ui.royalespells.hero_recast': ('部署英雄后，30 秒内可免费连施一次，翻滚并恢复一半已损失生命。需抄写到书或武器。', 'After deployment, recast once within 30 seconds to roll and heal half the missing health. Inscribe into a book or weapon.'),
    'effect.royalespells.cloned': ('克隆体', 'Cloned'),
    'ui.royalespells.mirror_all': ('复制上一种成功施放的皇室或铁魔法法术，等级 +1；可临时超过卷轴等级上限。', 'Repeat the last successful Royale or Iron\'s spell at +1 level, including above its scroll level cap.'),
    'ui.royalespells.mirror_mana': ('消耗原法术的魔力，另加 %s 点；镜像升级降低附加消耗。', 'Uses the original spell\'s mana plus %s. Upgrading Mirror reduces the surcharge.'),
    'ui.royalespells.mirror_native': ('保留原施法、连发与解锁条件。镜像不会继续提高后续镜像的等级。', 'Keeps the original casting, recasts and learning rules. Mirrored casts do not stack extra levels.'),
}
for index, locale in enumerate(('zh_cn', 'en_us')):
    path = assets / f'lang/{locale}.json'
    data = json.loads(path.read_text(encoding='utf-8'))
    for name in ids:
        display = data[f'item.royalespells.{name}']
        data[f'ironspell.royalespells.{name}'] = ('皇室 · ' if index == 0 else 'Royale · ') + display
        data[f'death.attack.royalespells.{name}'] = ('%1$s 被 %2$s 的' + display + '击败了') if index == 0 else ('%1$s was defeated by %2$s using ' + display)
    data.update({key: value[index] for key, value in messages.items()})
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print('Generated 29 native icons, 29 conditional recipes, and both spell translations.')
