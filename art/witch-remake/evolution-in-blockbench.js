(async()=>{
 const p=ModelProject.all.find(p=>p.uuid==='a1b270e5-936f-175e-a4be-c799f973a76b');
 if(!p||!p.select())throw Error('Missing isolated evolved Witch');
 const root='C:/Users/BliBe/Documents/Codex/2026-09-04/new-chat/work/royale-spells-fabric-1.20.1-standalone/art/witch-remake/';
 const src=await new Promise(r=>Blockbench.read([root+'witch_evolution-legacy-staff.json'],{readtype:'text'},f=>r(JSON.parse(f[0].content))));
 const tex=Texture.all[0],gs=Object.fromEntries(Group.all.map(g=>[g.name,g])),old=Outliner.elements.slice();
 Undo.initEdit({elements:old,groups:Group.all.slice(),outliner:true});
 function rem(g){g.children.slice().forEach(c=>{if(c instanceof Group){rem(c);c.remove();}else c.remove();});}
 rem(gs.staff);
 const delta=gs.staff.origin.map((x,i)=>x-src.root.origin[i]),byId=Object.fromEntries(src.elements.map(m=>[m.uuid,m]));
 const mapping={3:6,4:4,6:5,8:8,9:14,10:15,13:5,14:13,15:6};
 function restore(source,target){for(const c of source.children){if(typeof c==='string'){
   const old=byId[c],d=JSON.parse(JSON.stringify(old));delete d.uuid;
   d.name=(src.materials[old.name].emissive?'EM_':'Restored ')+d.name;
   Object.values(d.vertices).forEach(v=>v.forEach((x,i)=>v[i]=x+delta[i]));
   const mat=mapping[src.materials[old.name].material]??6;
   for(const f of Object.values(d.faces)){const seen=new Set();f.vertices=f.vertices.filter(k=>{const id=d.vertices[k].join(',');if(seen.has(id))return false;seen.add(id);return true;});f.texture=tex.uuid;}
   const m=new Mesh(d).addTo(target).init();
   const b=[0,1,2].map(i=>[Math.min(...Object.values(m.vertices).map(v=>v[i])),Math.max(...Object.values(m.vertices).map(v=>v[i]))]);
   for(const f of Object.values(m.faces)){const norm=f.getNormal(true),axis=norm.map(Math.abs).indexOf(Math.max(...norm.map(Math.abs))),u=axis===0?2:0,v=axis===1?2:1,scale=Math.min(2,25/Math.max(1,b[u][1]-b[u][0],b[v][1]-b[v][0]));for(const k of f.vertices)f.uv[k]=[mat%4*32+4+(m.vertices[k][u]-b[u][0])*scale,Math.floor(mat/4)*32+4+(b[v][1]-m.vertices[k][v])*scale];}
 }else{const g=new Group({name:c.name,origin:c.origin.map((x,i)=>x+delta[i]),rotation:c.rotation||[0,0,0]}).addTo(target).init();restore(c,g);}}}
 restore(src.root,gs.staff);
 // Normal Witch keeps the unadorned hood. Only evolution receives gold edging
 // and the canonical open horned bone frontlet.
 for(const m of Outliner.elements){
  if(m.name.startsWith('Hood folded inner seam'))for(const f of Object.values(m.faces))for(const k of f.vertices)f.uv[k]=[16,48];
  if(m.name.startsWith('Hood ')||m.name.startsWith('Back cape'))for(const f of Object.values(m.faces))for(const k of f.vertices){const uv=f.uv[k];if(Math.floor(uv[0]/32)===0&&Math.floor(uv[1]/32)===0)uv[0]+=32;}
 }
 function slab(name,poly,z,d,mat){
  const pts=poly.map(([x,y])=>[x,y,z]),n=pts.length;pts.push(...pts.map(v=>[v[0],v[1],v[2]+d]));const faces=[];
  for(const t of THREE.ShapeUtils.triangulateShape(poly.map(q=>new THREE.Vector2(...q)),[])){faces.push([t[0],t[2],t[1]]);faces.push(t.map(i=>i+n));}
  for(let i=0;i<n;i++)faces.push([i,(i+1)%n,(i+1)%n+n,i+n]);
  const m=new Mesh({name,origin:[0,0,0],vertices:{},faces:{}});let j=0;
  for(const f of faces){const keys=[],uv={};for(const i of f){const k='v'+j++;m.vertices[k]=pts[i].slice();keys.push(k);uv[k]=[mat%4*32+7+(pts[i][0]+5)*1.7,Math.floor(mat/4)*32+5+(29-pts[i][1])*2];}m.addFaces(new MeshFace(m,{vertices:keys,uv,texture:tex.uuid}));}m.addTo(gs.head).init();return m;
 }
 slab('Evolved bone frontlet',[[-1.15,26.45],[-.8,25.4],[-.36,25.2],[0,24.25],[.36,25.2],[.8,25.4],[1.15,26.45],[.62,26.96],[-.62,26.96]],-4.02,.6,4);
 for(const s of [-1,1]){
  slab('Evolved swept horn '+s,[[.74,26.55],[2.0,26.85],[3.5,27.13],[4.35,27.72],[4.0,28.28],[3.38,28.51],[3.87,28.45],[4.7,28.08],[4.89,27.43],[4.32,26.91],[3.16,26.48],[1.17,26.05]].map(([x,y])=>[x*s,y]),-3.73,.72,4);
  slab('Evolved dark bone socket '+s,[[.2,26.14],[.56,26.66],[.9,26.27],[.71,25.9]].map(([x,y])=>[x*s,y]),-4.035,.02,8);
 }
 for(const m of Outliner.elements)if(m.name.startsWith('Eye light inner facet'))m.name='EM_'+m.name;
 Canvas.updateAll();Undo.finishEdit('Adapt rebuilt Witch to canonical evolution hood and skull staff',{elements:Outliner.elements.slice(),groups:Group.all.slice(),outliner:true});
 return{project:Project.uuid,elements:Outliner.elements.length};
})()
