/* Both named projects must already be open. Copies the adapted source head,
 * including its original UV coordinates, into the ice furnace variant. */
ModelProject.all.find(p=>p.name==='Royale - Ice Spirit MineClash V3').select();
const copies=Mesh.all.filter(m=>['body','tooth'].includes(m.parent.name)).map(m=>m.getSaveCopy());
ModelProject.all.find(p=>p.name==='Royale - Furnace Staff ice V3').select();
for(const m of Mesh.all.slice())if(m.name.includes('emerging_'))m.remove();
const iceTex=new Texture({name:'spirit_ice_mineclash.png'}).fromPath('C:/Users/BliBe/Documents/Codex/2026-09-04/new-chat/work/royale-spells-iron-1.21.1/src/main/resources/assets/royalespells/textures/entity/spirit_ice_mineclash.png').add(false);
iceTex.uv_width=iceTex.uv_height=128;
for(const [i,copy] of copies.entries()){
    const data=JSON.parse(JSON.stringify(copy));delete data.uuid;
    data.name='ICE_emerging_'+i;
    data.origin=data.origin.map((v,j)=>v*.52+[8,24.2,7.6][j]);
    for(const v of Object.values(data.vertices))for(let j=0;j<3;j++)v[j]*=.52;
    const m=new Mesh(data).addTo(Group.all[0]).init();
    for(const f of Object.values(m.faces))f.texture=iceTex.uuid;
}
unselectAllElements();Canvas.updateAll();
