"""Record byte provenance and packaged-client evidence; never edit raster images.

Run from any directory after export-runtime.py, Gradle build, and the packaged
spirit smoke client. Missing or mismatched inputs fail rather than manufacturing
successful test output. Documentation describes the separate visual review.
"""
from pathlib import Path
import hashlib
import json
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = '1.6.0-beta.3'
RUN = ROOT / 'run-production-spirit-beta3-voxel'
EVIDENCE = ROOT / 'docs/beta3'
ASSETS = ROOT / 'src/main/resources/assets/royalespells'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def relative(path):
    return path.relative_to(ROOT).as_posix()


def dump(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def record(path):
    return {'path': relative(path), 'sha256': sha(path), 'bytes': path.stat().st_size}


def main():
    jar = ROOT / f'build/libs/royale-spells-neoforge-1.21.1-{VERSION}.jar'
    installed = RUN / 'mods' / jar.name
    assert sha(jar) == sha(installed), 'Tested JAR differs from current build'
    log = (RUN / 'console.log').read_text(encoding='utf-8', errors='replace')
    assert 'ROYALE_SPIRIT_CLIENT_COMPLETE models=4 leapFrames=10' in log
    assert 'ROYALE_SPIRIT_NATIVE_LIVE_HIT health=92.3' in log
    assert '2048x1024x4 minecraft:textures/atlas/blocks.png-atlas' in log
    assert 'Missing furnace sprite source' not in log
    assert 'Unable to prepare furnace sprite' not in log
    assert not any('Missing textures' in line and 'royalespells' in line for line in log.splitlines())

    EVIDENCE.mkdir(parents=True, exist_ok=True)
    (EVIDENCE / 'screenshots').mkdir(exist_ok=True)
    images = sorted((RUN / 'screenshots').glob('*.png'))
    assert len(images) == 16
    for path in images:
        shutil.copy2(path, EVIDENCE / 'screenshots' / path.name)
    shutil.copy2(RUN / 'console.log', EVIDENCE / 'client.log')
    shutil.copy2(ROOT / 'build-beta3-voxel.log', EVIDENCE / 'build.log')
    evidence = {
        'version': VERSION,
        'scope': 'Local packaged client; no CurseForge install or GitHub publication.',
        'jar': record(jar),
        'sources_jar': record(jar.with_name(jar.stem + '-sources.jar')),
        'runtime': {'minecraft': '1.21.1', 'neoforge': '21.1.249', 'java': '21.0.12',
                    'exit_code': 0, 'graphics': 'no shader pack', 'run_directory': relative(RUN)},
        'mods': [record(p) for p in sorted((RUN / 'mods').glob('*.jar'))],
        'markers': [s for s in log.splitlines() if s.startswith('ROYALE_SPIRIT_')],
        'screenshots': [record(EVIDENCE / 'screenshots' / p.name) for p in images],
        'manual_visual_review': ['four cube silhouettes and flat faces', 'four staff textures',
                                 'fire alpha', 'third-person grip', 'inventory', 'live jump', 'ice night mask'],
        'prior_tests_not_rerun': {'standalone': 59, 'iron': 118, 'source': 'VALIDATION-1.6.0-beta.2.md'},
        'limits': ['No user aesthetic acceptance claimed', 'No full user modpack or multiplayer/shader validation'],
    }
    dump(EVIDENCE / 'test-results.json', evidence)

    sources_path = ROOT / 'SPIRIT-ASSET-SOURCES.json'
    sources = json.loads(sources_path.read_text(encoding='utf-8'))
    sources['version'] = VERSION
    sources['purpose'] = ('The references array contains reference-only Clash Royale artwork, not bundled in the JAR. '
                          'Current generated textures and user-authorized MineClash ice assets are recorded separately below.')
    sources['created_assets'] = ('Eight actual Blockbench projects exported to runtime JSON/OBJ. '
                                 'Fire/electro/heal and staff use ImageGen pixel material maps; '
                                 'ice reuses the supplied MineClash geometry, diffuse texture and glow mask.')
    sources['staff_texture']['status'] = 'Superseded Beta 2 texture; excluded from the block/item atlas in Beta 3.'
    sources['current_generated_assets'] = []
    for name, generated in [
        ('spirit_materials.png', 'exec-bd642dfc-f2df-40eb-9320-4fb61b1034f5.png'),
        ('spirit_flame.png', 'exec-98352c00-1a0e-46a7-8e26-23e5557abdf3.png'),
    ]:
        path = ASSETS / 'textures/entity' / name
        origin = Path.home() / '.codex/generated_images/01a07009-6629-7c60-9206-dde4ddc77def' / generated
        if origin.exists():
            assert sha(origin) == sha(path), 'Generated PNG was changed'
        sources['current_generated_assets'].append(record(path) | {
            'generator': 'built-in image_gen', 'generation_file': generated, 'size': [1254, 1254],
            'prompt_file': 'art/blockbench-v3/PIXEL-FINAL-PROMPTS.md',
            'handling': 'Original PNG bytes; only in-memory nearest-neighbor atlas upload adapts size.'})

    reference = ROOT / 'art/mineclash-reference'
    original_meta = json.loads((reference / 'SOURCE.json').read_text(encoding='utf-8'))
    source_jar = Path(original_meta['source_jar'])
    assert sha(source_jar) == original_meta['sha256']
    ice = []
    with zipfile.ZipFile(source_jar) as z:
        for raw, runtime in [
            ('ice_spirit.png', 'spirit_ice_mineclash.png'),
            ('ice_spirit_glowmask.png', 'spirit_ice_mineclash_glowmask.png'),
        ]:
            archive_path = 'assets/mineclash/textures/entity/' + raw
            dest = ASSETS / 'textures/entity' / runtime
            assert z.read(archive_path) == dest.read_bytes()
            ice.append(record(dest) | {'source_entry': archive_path, 'handling': 'Byte-identical original texture'})
        raw_geo = reference / 'assets/mineclash/geo/ice_spirit.geo.json'
        assert z.read('assets/mineclash/geo/ice_spirit.geo.json') == raw_geo.read_bytes()
        ice.append(record(raw_geo) | {'handling': 'Original source; runtime geometry adapted via Blockbench, uniform 0.75 scale'})
    sources['mineclash_ice'] = {
        'source_archive': source_jar.name, 'source_archive_sha256': original_meta['sha256'],
        'authors': ['LiziYowo', 'YangXuKun', '4y4u', 'AX_ZHANG', 'LieNiaoBiBai'],
        'declared_license': 'All Rights Reserved',
        'authorization': 'User supplied archive and explicitly authorized ice model reference or reuse for future merge.',
        'runtime_code_merged': False, 'full_animation_port': False, 'files': ice,
    }
    report = json.loads((ROOT / 'art/blockbench-v3/export-report.json').read_text(encoding='utf-8'))
    models = []
    for name, metrics in report.items():
        model = ROOT / 'art/blockbench-v3' / (name + '.bbmodel')
        assert metrics['source_sha256'] == sha(model), 'Runtime export stale: ' + name
        doc = json.loads(model.read_text(encoding='utf-8'))
        assert all(t.get('source', '').startswith('data:') for t in doc['textures']), 'Textures must be embedded'
        models.append(record(model) | metrics)
    sources['blockbench_projects'] = models
    sources['icons'] = [record(ASSETS / f'textures/gui/spell_icons/summon_{e}_spirit.png') |
                        {'method': 'Actual final Blockbench model rendered at 128x128 with transparency'}
                        for e in ['fire', 'ice', 'electro', 'heal']]
    with zipfile.ZipFile(jar) as z:
        packaged = [ASSETS / 'models/troop' / f'spirit_{e}.json' for e in ['fire', 'ice', 'electro', 'heal']]
        packaged += [ASSETS / 'models/item' / f'furnace_staff_{e}.obj' for e in ['fire', 'ice', 'electro', 'heal']]
        packaged += [ASSETS / 'textures/entity' / n for n in ['spirit_materials.png', 'spirit_flame.png',
                                                               'spirit_ice_mineclash.png', 'spirit_ice_mineclash_glowmask.png']]
        packaged += [ASSETS / 'model_credits.txt', ROOT / 'src/main/resources/assets/minecraft/atlases/blocks.json']
        for path in packaged:
            assert z.read(path.relative_to(ROOT / 'src/main/resources').as_posix()) == path.read_bytes(), 'JAR resource differs'
        assert 'dev/royalespells/client/FurnaceSpriteSource.class' in z.namelist()
    dump(sources_path, sources)
    print('BETA3_ASSETS_AND_EVIDENCE_OK models=8 screenshots=16 jar=' + sha(jar))


if __name__ == '__main__':
    main()
