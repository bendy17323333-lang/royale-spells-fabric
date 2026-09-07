"""Read actual client frames to check contour visibility; never modify screenshots."""
from pathlib import Path
from PIL import Image
import argparse
import json

parser = argparse.ArgumentParser()
parser.add_argument('run', type=Path, help='Isolated production preview run directory')
args = parser.parse_args()
log = (args.run / 'console.log').read_text(encoding='utf-8', errors='replace')
assert 'ROYALE_TARGET_PREVIEW_COMPLETE scenes=14 depthAudits=14 hideGui=true emptyHand=true' in log
rows = []
for line in log.splitlines():
    if not line.startswith('ROYALE_TARGET_FRAME '):
        continue
    name = line.split()[1]
    assert 'vanillaCrosshair=true' in line
    with Image.open(args.run / 'screenshots' / f'target-{name}.png') as source:
        image = source.convert('RGB')
        pixels = image.load()
        # Exclude hotbar/held item. Detect missing viewport contours; Java verifies
        # radius, planarity and full scene-depth preservation for every scene.
        lit = []
        for y in range(25, int(image.height * .8)):
            for x in range(int(image.width * .08), int(image.width * .84)):
                r, g, b = pixels[x, y]
                bright = b > 190 and g > 160 and g > r * 1.25 and b > r * 1.3
                # Rolling paths are mostly behind the staircase: the ghost pass
                # blends cyan into brown blocks, so its pixels are deliberately dim.
                ghost = name in ('log-over-steps', 'barrel-over-steps') and y >= image.height*.5 and x < image.width*.68 and r < 140 and g > r+12 and b > r+12
                if bright or ghost:
                    lit.append((x, y))
        assert len(lit) >= 80, (name, 'contour absent from viewport', len(lit))
        xs, ys = zip(*lit)
        rows.append(dict(scene=name, colored_pixels=len(lit), bounds=[min(xs), min(ys), max(xs)+1, max(ys)+1]))
assert len(rows) == 14, ('missing screenshot checks', len(rows))
(args.run / 'target-pixel-check.json').write_text(json.dumps(rows, indent=2), encoding='utf-8')
print('TARGET_FRAMEBUFFER_CHECK_PASSED', len(rows), 'visible contours; no added GUI reticle')
