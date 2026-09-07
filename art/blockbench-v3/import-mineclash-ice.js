/* Run in Blockbench after importing the user-supplied ice_spirit.geo.json and texture.
 * Blockbench performs the cube-to-mesh UV conversion; we bake cube rest rotations,
 * retain articulated bones, and uniformly fit the existing spirit entity size.
 * No texture pixels are changed. The original GEO and animation remain in art/mineclash-reference.
 */
Formats.free.convertTo();
unselectAllElements();
for (const c of Cube.all) c.markAsSelected(true);
BarItems.convert_to_mesh.onClick();
const scale = 0.75, groundOffset = 0.03556 * scale;
for (const m of Mesh.all) {
    const rotation = new THREE.Euler(...m.rotation.map(Math.degToRad), 'XYZ');
    for (const v of Object.values(m.vertices)) {
        const p = new THREE.Vector3(...v).applyEuler(rotation);
        v.splice(0, 3, p.x * scale, p.y * scale, p.z * scale);
    }
    m.origin = m.origin.map(x => x * scale);
    m.origin[1] += groundOffset;
    m.rotation = [0, 0, 0];
    for (const f of Object.values(m.faces)) f.vertices = f.getSortedVertices();
}
const names = {all:'root', rightarm:'right_arm', leftarm:'left_arm', rightleg:'right_leg', leftleg:'left_leg'};
for (const g of Group.all) {
    g.origin = g.origin.map(x => x * scale);
    g.origin[1] += groundOffset;
    g.name = names[g.name] || g.name;
}
Texture.all[0].fromPath('C:/Users/BliBe/Documents/Codex/2026-09-04/new-chat/work/royale-spells-iron-1.21.1/src/main/resources/assets/royalespells/textures/entity/spirit_ice_mineclash.png');
Project.name = 'Royale - Ice Spirit MineClash V3';
Project.texture_width = Project.texture_height = 128;
Project.save_path = 'C:/Users/BliBe/Documents/Codex/2026-09-04/new-chat/work/royale-spells-iron-1.21.1/art/blockbench-v3/spirit_ice.bbmodel';
unselectAllElements();
Canvas.updateAll();
