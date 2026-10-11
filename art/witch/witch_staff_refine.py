"""Rebuild only the Witch staff head, after the legacy +6 Y shortening pass.

All coordinates are in the existing staff joint's model space (Y points down).
The hand joint, shaft, bindings and muzzle centre at Y=-17 remain unchanged.
Eye sockets and the open mouth are actual empty spaces bounded by solid rims;
no dark rectangle, transparent picture or unlit plane stands in for the skull.
The caller owns atlas UV allocation, serialization and preview generation.
"""
from math import sqrt


def _quad(*points):
    return [float(c) for point in points for c in point]


def _triangle(a, b, c):
    # The shared mesh format is quad-only; the last coincident corner makes a
    # triangle without adding a zero-area first triangle or changing normals.
    return _quad(a, b, c, c)


def _area(poly):
    return sum(a[0]*b[1]-b[0]*a[1]
               for a, b in zip(poly, poly[1:]+poly[:1]))


def _prism(r, name, poly, front, back, mat=3):
    """Closed convex silhouette, with independent front/back and edge faces."""
    poly = list(poly)
    if _area(poly) < 0:
        poly.reverse()
    faces = []
    if len(poly) == 4:
        faces.append(_quad(*[(x, y, front) for x, y in reversed(poly)]))
        faces.append(_quad(*[(x, y, back) for x, y in poly]))
    else:
        # These silhouettes are convex; an n-2 fan needs no extra centre
        # vertex and avoids subdividing tiny teeth into redundant faces.
        for i in range(1, len(poly)-1):
            a, b, c = poly[0], poly[i], poly[i+1]
            faces.append(_triangle((*a, front), (*c, front), (*b, front)))
            faces.append(_triangle((*a, back), (*b, back), (*c, back)))
    for a, b in zip(poly, poly[1:]+poly[:1]):
        faces.append(_quad((*a, front), (*b, front), (*b, back), (*a, back)))
    r.solid('staff', name, faces, mat, uv_scale=.5)


def _socket(r, name, outer, inner, front=-2.08, back=-.82):
    """Four-sided slanted orbital rim; the middle is deliberately not filled."""
    outer, inner = list(outer), list(inner)
    if _area(outer) < 0:
        outer.reverse()
        inner.reverse()
    rim, edge = [], []
    for i in range(4):
        j = (i+1) % 4
        a, b, c, d = outer[i], outer[j], inner[i], inner[j]
        rim.append(_quad((*a, front), (*c, front), (*d, front), (*b, front)))
        rim.append(_quad((*b, back), (*d, back), (*c, back), (*a, back)))
        edge.append(_quad((*a, front), (*b, front), (*b, back), (*a, back)))
        edge.append(_quad((*d, front), (*c, front), (*c, back), (*d, back)))
    r.solid('staff', name+'_rim', rim, 3, uv_scale=.45)
    r.solid('staff', name+'_recess_wall', edge, 15, uv_scale=.4)


def _cross(a, b):
    return (a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2],
            a[0]*b[1]-a[1]*b[0])


def _unit(v):
    length = sqrt(sum(c*c for c in v))
    return tuple(c/length for c in v)


def _beam(r, name, a, b, width0, width1, depth, mat):
    """A tapered rectangular solid in 3D, not an axis-aligned connecting box."""
    axis = _unit(tuple(y-x for x, y in zip(a, b)))
    reference = (0., 0., 1.) if abs(axis[2]) < .8 else (0., 1., 0.)
    side = _unit(_cross(axis, reference))
    up = _unit(_cross(side, axis))
    rings = []
    for origin, width in [(a, width0), (b, width1)]:
        rings.append([tuple(origin[k] + sx*width*.5*side[k] + sy*depth*.5*up[k]
                            for k in range(3))
                      for sx, sy in [(-1,-1), (1,-1), (1,1), (-1,1)]])
    p, q = rings
    faces = [_quad(*p), _quad(*reversed(q))]
    faces += [_quad(p[i], q[i], q[(i+1)%4], p[(i+1)%4]) for i in range(4)]
    r.solid('staff', name, faces, mat, uv_scale=.45)


def _mirror(poly, side):
    return [(side*x, y) for x, y in poly]


def _crystal(r):
    r.part('staff_crystal', 'staff', (0, -17, -.82))
    top, bottom = (0, -1.00, 0), (0, .94, 0)
    ring = [(-1.00, 0, 0), (0, 0, -.82), (1.00, 0, 0), (0, 0, .82)]
    facets, highlight = [], []
    for i in range(4):
        a, b = ring[i], ring[(i+1)%4]
        upper = _triangle(top, a, b)
        lower = _triangle(bottom, b, a)
        if i == 1:
            highlight.append(upper)
        else:
            facets.append(upper)
        facets.append(lower)
    r.solid('staff_crystal', 'suspended_soul_crystal', facets, 9, True, uv_scale=.3)
    r.solid('staff_crystal', 'soul_crystal_lit_facet', highlight, 10, True, uv_scale=.25)


def _evolved(r):
    # The cranium stops above the eyes. Its chamfered outline is distinct from
    # the cheeks and leaves both eye apertures completely open from the front.
    _prism(r, 'skull_vault', [(-2.42,-20.18),(-2.13,-21.39),(-1.22,-21.84),
                            (1.22,-21.84),(2.13,-21.39),(2.42,-20.18)],
           -1.73, 1.06)
    _prism(r, 'skull_occiput', [(-2.08,-20.55),(2.08,-20.55),
                              (1.72,-18.16),(-1.72,-18.16)], .91, 1.51, 15)
    for side, label in [(-1,'left'), (1,'right')]:
        # Build the left contour then reflect: rim widths vary deliberately,
        # making the brow heavy and the inferior orbital edge slim.
        outer = _mirror([(.30,-20.39),(2.39,-20.10),(2.19,-18.49),(.43,-18.66)], side)
        inner = _mirror([(.70,-20.04),(2.04,-19.86),(1.93,-18.91),(.77,-19.02)], side)
        _socket(r, 'orbit_'+label, outer, inner)
        # A dark lining sits deep inside the real opening, behind the bone
        # tunnel. Using the grey-bone cell here flattened the eye cavity in
        # the in-game front view. This is not a black decal on the face.
        _prism(r, 'orbit_deep_lining_'+label, inner, .77, .91, 8)
        _prism(r, 'zygomatic_'+label,
               _mirror([(1.87,-19.03),(2.44,-19.00),(2.17,-17.99),(1.50,-18.27)], side),
               -1.76, .60)
        _beam(r, 'temple_'+label, (side*2.16,-19.74,.43),
              (side*1.94,-18.18,.91), .55, .38, .55, 15)
        # Narrow curved mandibular arms emerge from actual side hinges. They
        # stay behind the crystal so the oral opening is visible in 3/4 views.
        _beam(r, 'mandible_ramus_'+label, (side*1.87,-18.18,1.02),
              (side*1.62,-16.12,.22), .43, .51, .55, 15)
        _beam(r, 'mandible_angle_'+label, (side*1.62,-16.12,.22),
              (side*1.37,-15.66,-1.62), .56, .64, .67, 3)
        _beam(r, 'mandible_chin_turn_'+label, (side*1.37,-15.66,-1.62),
              (side*.81,-15.60,-2.09), .58, .62, .62, 3)
    _prism(r, 'nasal_bridge', [(-.27,-20.43),(.27,-20.43),(.41,-18.83),(-.41,-18.83)],
           -2.23, -1.00)
    # Paired alae leave a small real triangular nasal gap above the palate.
    for side, label in [(-1,'left'), (1,'right')]:
        _prism(r, 'nasal_ala_'+label,
               _mirror([(.12,-18.96),(.35,-19.05),(.58,-18.36),(.29,-18.32)], side),
               -2.20, -1.45, 15)
    _prism(r, 'upper_palate', [(-1.59,-18.49),(1.59,-18.49),
                             (1.35,-17.98),(-1.35,-17.98)], -2.07, -.83)
    _prism(r, 'mandible_chin', [(-.94,-15.99),(.94,-15.99),
                              (.77,-15.21),(-.77,-15.21)], -2.16, -1.47)
    # Maintain the physical connection to the unchanged gilt collar after
    # raising the jaw. The support is behind the open mouth, below the gem.
    r.box('staff', 'skull_rear_socket_support', (-.60,-15.43,.24),
          (1.20,.74,1.00), 14, uv_scale=.4)
    # Two broad incisors and two canines read as teeth at game distance, while
    # the negative gaps prevent the old solid rectangular "ladder" appearance.
    for side, label in [(-1,'left'), (1,'right')]:
        _prism(r, 'upper_incisor_'+label,
               _mirror([(.07,-18.17),(.64,-18.17),(.61,-17.52),(.11,-17.48)], side),
               -2.12, -1.52)
        _prism(r, 'upper_canine_'+label,
               _mirror([(.91,-18.27),(1.38,-18.27),(1.12,-17.47)], side),
               -2.01, -1.50, 15)
        _prism(r, 'lower_incisor_'+label,
               _mirror([(.06,-16.42),(.52,-16.42),(.57,-15.91),(.07,-15.91)], side),
               -2.08, -1.55)
        _prism(r, 'lower_canine_'+label,
               _mirror([(.83,-15.97),(1.22,-15.95),(.97,-16.44)], side),
               -1.94, -1.44, 15)
    _crystal(r)


def _ordinary(r):
    # Short, forward-tapering beast skull held by two curling gold antlers,
    # as in the user's block-model reference. No evolution human jaw/gem.
    _prism(r, 'beast_forehead', [(-2.02,-18.92),(-1.72,-20.33),
                               (-.96,-20.73),(.96,-20.73),
                               (1.72,-20.33),(2.02,-18.92)], -1.70, 1.27)
    _prism(r, 'beast_rear', [(-1.8,-19.65),(1.8,-19.65),
                           (1.46,-17.61),(-1.46,-17.61)], .74, 1.47, 15)
    for side, label in [(-1,'left'),(1,'right')]:
        outer = _mirror([(.24,-19.15),(1.98,-19.23),(1.78,-17.92),(.55,-18.04)], side)
        inner = _mirror([(.66,-18.86),(1.64,-18.91),(1.49,-18.27),(.78,-18.34)], side)
        _socket(r, 'beast_orbit_'+label, outer, inner, -1.91, -.78)
        _prism(r, 'beast_cheek_'+label,
               _mirror([(1.46,-18.61),(1.97,-18.25),(1.40,-16.48),(.81,-16.89)], side),
               -1.74, .81)
        # Each horn follows a four-bend taper. The forward end hooks inward,
        # so it cradles the bone rather than becoming a straight antenna.
        points = [(side*1.23,-15.39,.19),(side*2.63,-17.30,.19),
                  (side*3.06,-19.05,.06),(side*2.61,-20.47,-.12),
                  (side*1.98,-20.92,-.34)]
        widths = [1.01,1.16,.96,.66,.21]
        for i in range(len(points)-1):
            _beam(r, 'beast_gilt_horn_'+label+'_'+str(i), points[i], points[i+1],
                  widths[i], widths[i+1], .97-i*.13, 4 if i < 3 else 14)
    _prism(r, 'beast_nasal_keel', [(-.61,-19.07),(.61,-19.07),
                                 (.96,-16.64),(-.96,-16.64)], -2.19, -.78)
    _prism(r, 'beast_muzzle', [(-.97,-17.07),(.97,-17.07),
                             (.68,-15.71),(-.68,-15.71)], -2.36, -.81)
    for side, label in [(-1,'left'),(1,'right')]:
        _prism(r, 'beast_nostril_ridge_'+label,
               _mirror([(.29,-16.91),(.58,-16.98),(.46,-16.04),(.20,-16.08)], side),
               -2.40, -2.13, 15)
    # The small gilt bridge attaches the skull firmly to the preserved collar.
    _beam(r, 'beast_lower_gilt_cradle', (0,-15.73,.30), (0,-14.70,.30),
          1.52,1.22,1.30,14)


def refine_staff(r, evo):
    """Replace the shortened old staff head in place; safe to call repeatedly."""
    if 'staff' not in r.parts:
        raise ValueError('refine_staff requires the existing staff hand joint')
    descendants = {'staff'}
    changed = True
    while changed:
        changed = False
        for name, part in list(r.parts.items()):
            if name not in descendants and part['parent'] in descendants:
                descendants.add(name)
                changed = True
    for name in descendants - {'staff'}:
        del r.parts[name]
    keep = {'staff_shaft','pommel','head_collar','head_gilt_band'}
    r.parts['staff']['boxes'] = [b for b in r.parts['staff']['boxes']
                               if b['name'] in keep or b['name'].startswith('staff_binding_')]
    (_evolved if evo else _ordinary)(r)
