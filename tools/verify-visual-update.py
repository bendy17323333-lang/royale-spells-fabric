"""Validate samples from the actual client, not a mock rendering clock.

Usage: python tools/verify-visual-update.py
Requires the solo4 and joint3 opt-in recordings made for dev.5.
"""
from pathlib import Path
from collections import defaultdict
import hashlib, json, statistics, zipfile

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/validation-dev5'
OUT.mkdir(exist_ok=True)
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
report = {}
for mode in ['solo4', 'joint3']:
    run = ROOT / ('run-production-visual' + mode)
    data = json.loads((run/'visual-observations.json').read_text(encoding='utf-8'))
    groups = defaultdict(list)
    for row in data:
        if row['kind'] != 'state' and row['tick'] < 220:
            groups[(row['kind'], row['id'])].append(row)
    cases = []
    for (kind, ident), rows in groups.items():
        if len(rows) < 30:
            continue
        ratios = defaultdict(list)
        for a, b in zip(rows, rows[1:]):
            if a['blue'] != b['blue'] or a['paused'] != b['paused']:
                continue  # A state change may occur between the two samples.
            di, dl = b['input'] - a['input'], b['local'] - a['local']
            assert di >= -.001 and dl >= -.001, 'Animation time moved backwards'
            if b['paused']:
                ratios['paused'].append(dl)
                assert abs(dl) < .0001, 'Electrical pause must hold local time'
            elif di > .1:
                key = 'slow' if b['blue'] else 'normal'
                ratios[key].append(dl/di)
                assert abs(dl/di - (.65 if b['blue'] else 1)) < .001
        affected = any(r['blue'] for r in rows)
        assert not rows[-1]['blue'], 'Snowball tint must expire'
        assert len(ratios['normal']) > 100
        if affected:
            assert len(ratios['slow']) >= 40 and len(ratios['paused']) >= 5
        else:
            assert all(abs(r['input'] - r['local']) < .0001 for r in rows)
        cases.append({'renderer': kind, 'entity': ident, 'affected': affected,
                      'samples': len(rows), 'measurements': {
                          k: {'count': len(v), 'min': min(v), 'median': statistics.median(v), 'max': max(v)}
                          for k, v in ratios.items() if v}})
    assert len(cases) == 4
    for kind in ['vanilla', 'gecko']:
        assert sorted(c['affected'] for c in cases if c['renderer'] == kind) == [False, True]
    # Blue-on-release is observed in two actual directions. The 'paused' sample
    # is electrical pause only; capture's separate STUN is tested server-side.
    states = [r for r in data if r['kind'] == 'state']
    for begin, end, cast in [(220, 380, 260), (380, 525, 412)]:
        scene = [r for r in states if begin < r['tick'] < end]
        assert any(r['blue'] and not r['paused'] for r in scene)
        first_blue = min(r['tick'] for r in scene if r['blue'])
        assert cast + 42 <= first_blue <= cast + 48
        assert len({r['id'] for r in scene if r['blue']}) == 3
        assert all(not r['blue'] for r in scene if r['tick'] > end-10)
    jars = list((run/'mods').glob('royale-spells-*.jar'))
    assert len(jars) == 1
    report[mode] = {'jar_sha256': sha(jars[0]), 'cases': cases,
                    'evolved_blue_after_release_two_directions': 'PASS',
                    'data_sha256': sha(run/'visual-observations.json')}

# The final test-fixture repair must not alter the gameplay/render classes
# already validated in the final solo recording.
current = ROOT/'build/libs/royale-spells-neoforge-1.21.1-1.7.0-dev.5.jar'
recorded = next((ROOT/'run-production-visualsolo4/mods').glob('royale-spells-*.jar'))
with zipfile.ZipFile(current) as a, zipfile.ZipFile(recorded) as b:
    changed = [n for n in sorted(set(a.namelist()) | set(b.namelist()))
               if (a.read(n) if n in a.namelist() else None) != (b.read(n) if n in b.namelist() else None)]
assert all(n.startswith('dev/royalespells/test/') or n == 'data/royalespells/structure/empty.nbt'
           for n in changed), changed
report['final_jar'] = {'sha256': sha(current), 'changes_since_recording': changed,
                       'gameplay_and_client_code_identical': True}
(OUT/'client-verification.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
print('PASS: both real clients, four independent entities each; 65% / 100% / pause / recovery; final runtime bytes unchanged')
