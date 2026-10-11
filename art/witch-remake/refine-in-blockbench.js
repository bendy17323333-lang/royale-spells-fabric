(async()=>{
 const p=ModelProject.all.find(p=>p.uuid==='42191b01-5388-7b75-297c-d50828dc708a');
 if(!p||!p.select())throw Error('Missing isolated Witch project');
 const root='C:/Users/BliBe/Documents/Codex/2026-09-04/new-chat/work/royale-spells-fabric-1.20.1-standalone/art/witch-remake/';
 const staffData=await new Promise(resolve=>Blockbench.read([root+'witch-legacy-staff.json'],{readtype:'text'},f=>resolve(JSON.parse(f[0].content))));
 const oldElements=Outliner.elements.slice(), tex=Texture.all[0], created=[];
 const gs=Object.fromEntries(Group.all.map(g=>[g.name,g]));
 Undo.initEdit({elements:oldElements,groups:Group.all.slice(),outliner:true,textures:[tex],bitmap:true});
 const removeNames=['Face -','Hood side','Hood cheek','Central pointed','Hood front long','Front robe','Right folded','Left folded','Outer hanging','Back split','Small skull buckle','Buckle left','Buckle right'];
 oldElements.filter(m=>removeNames.some(n=>m.name.startsWith(n))).forEach(m=>m.remove());
 const staffGroup=gs.staff;
 function removeContents(g){g.children.slice().forEach(c=>{if(c instanceof Group){removeContents(c);c.remove();}else c.remove();});}
 removeContents(staffGroup);
 const tx=tex.uuid;
 function mesh(name,part,verts,faces,mat,flat=false){
  const m=new Mesh({name,vertices:{},faces:{},origin:[0,0,0]});let vi=0;
  for(const poly0 of faces){const poly=poly0.map(i=>verts[i]),keys=[],uv={};
   for(const point of poly){const key='v'+vi++;m.vertices[key]=point.slice();keys.push(key);}
   const u=mat%4*32,v=Math.floor(mat/4)*32;
   keys.forEach((key,i)=>uv[key]=flat?[u+16,v+16]:[u+5+(i===0||i===3?0:21),v+5+(i>=2?21:0)]);
   m.addFaces(new MeshFace(m,{vertices:keys,uv,texture:tx}));
  }
  m.addTo(gs[part]).init();created.push(m);return m;
 }
 function slab(part,name,poly,z,depth,mat,flat=false){
  const vv=poly.map(([x,y])=>[x,y,typeof z==='function'?z(x,y):z]);const n=poly.length;
  vv.push(...vv.map(v=>[v[0],v[1],v[2]+depth]));
  const triangles=THREE.ShapeUtils.triangulateShape(poly.map(q=>new THREE.Vector2(...q)),[]),ff=[];
  for(const t of triangles){ff.push([t[0],t[2],t[1]]);ff.push(t.map(i=>i+n));}
  for(let i=0;i<n;i++)ff.push([i,(i+1)%n,(i+1)%n+n,i+n]);
  return mesh(name,part,vv,ff,mat,flat);
 }
 function box(part,name,x,y,z,w,h,d,mat){return slab(part,name,[[x,y],[x+w,y],[x+w,y+h],[x,y+h]],z,d,mat);}
 slab('head','Face - small broad chamfered opening',[[-2.7,24.9],[2.7,24.9],[3.18,25.4],[3.18,28.6],[2.82,29.52],[-2.82,29.52],[-3.18,28.6],[-3.18,25.4]],-3.47,3.9,7,true);
 slab('head','Hood front cloth brow with central point',[[-4.36,29.1],[-3.07,29.1],[-2.8,30.0],[-1.1,29.72],[0,28.62],[1.1,29.72],[2.8,30.0],[3.07,29.1],[4.36,29.1],[4.36,31.45],[3.7,32.7],[-3.7,32.7],[-4.36,31.45]],-4.02,.68,0);
 for(const s of [-1,1]){
  const poly=[[3.15,31.1],[4.4,31.45],[4.67,28.1],[4.6,24.6],[3.55,23.15],[2.95,23.55],[3.17,25.5],[3.1,28.65]].map(([x,y])=>[x*s,y]);
  slab('head','Hood draped side cloth '+s,poly,(x,y)=>-3.98+Math.max(0,26-y)*.28,5.15,0);
  const rim=[[3.09,29.1],[3.39,29.04],[3.43,25.55],[3.23,24.15],[2.97,23.5],[3.1,25.55]].map(([x,y])=>[x*s,y]);
  slab('head','Hood folded inner seam '+s,rim,(x,y)=>-4.08+Math.max(0,26-y)*.28,.24,10);
  const eye=[[.57,27.5],[2.55,27.98],[2.46,27.42],[1.94,27.18],[.76,27.12]].map(([x,y])=>[x*s,y]);
  slab('head','Slanted magical eye '+s,eye,-3.50,.026,14,true);
  const lid=[[.5,27.54],[2.59,28.1],[2.55,27.94],[.58,27.4]].map(([x,y])=>[x*s,y]);
  slab('head','Upper violet eyelid '+s,lid,-3.523,.02,12,true);
  const spark=[[1.17,27.53],[2.23,27.82],[2.05,27.52],[1.26,27.34]].map(([x,y])=>[x*s,y]);
  slab('head','Eye light inner facet '+s,spark,-3.534,.02,15,true);
  const folds=[[1.88,23.95],[3.25,23.85],[3.20,19.8],[3.25,15.2],[3.55,10.65],[4.64,s<0?4.0:5.25],[3.63,s<0?3.1:4.9],[2.55,9.9],[2.23,14.9],[2.36,19.6]].map(([x,y])=>[x*s,y]);
  slab('cape_ribbon_'+s,'Draped hood front lappet '+s,folds,(x,y)=>-2.88-Math.max(0,17-y)*.028,.48,0);
  const shoulder=s<0?'right_arm':'left_arm',cx=s*5.15;
  slab(shoulder,'Gold shoulder skull '+s,[[cx-1.06,22.16],[cx-.75,21.4],[cx-.6,20.91],[cx+.58,20.91],[cx+.75,21.4],[cx+1.06,22.16],[cx+.66,22.8],[cx-.66,22.8]],-2.09,.45,13);
  for(const k of [-1,1])slab(shoulder,'Shoulder skull recess '+s+' '+k,[[cx+k*.45-.3,21.74],[cx+k*.45+.3,21.74],[cx+k*.45+.23,22.13],[cx+k*.45-.3,22.22]],-2.105,.018,8,true);
  slab(shoulder,'Shoulder skull nose '+s,[[cx-.17,21.46],[cx+.17,21.46],[cx,21.72]],-2.12,.016,4,true);
 }
 slab('head','Small reserved mouth',[[-.52,25.92],[.51,25.98],[.40,25.82],[-.40,25.78]],-3.50,.028,9,true);
 slab('robe_right','Wine robe split front right',[[-2.97,13.4],[-.15,13.4],[-.32,5.2],[-.63,2.6],[-1.36,3.05],[-1.83,1.2],[-2.43,2.4],[-3.6,1.7],[-4.07,3.35],[-3.54,8.5]],(x,y)=>-2.47-(13.4-y)*.054,.55,3);
 slab('robe_left','Wine robe split front left',[[.15,13.4],[2.97,13.4],[3.58,8.5],[4.4,3.1],[3.61,2.3],[3.08,3.1],[2.18,1.6],[1.55,2.7],[.71,2.5],[.38,5.8]],(x,y)=>-2.5-(13.4-y)*.047,.55,9);
 for(const s of [-1,1]){
  const poly=[[2.78,14.05],[3.35,13.95],[4.48,9],[5.06,5.35],[4.82,2.3],[4.25,2.8],[3.72,.86],[3.19,2.3],[3.53,6.85]].map(([x,y])=>[x*s,y]);
  slab(s<0?'robe_side_right':'robe_side_left','Flaring side robe '+s,poly,(x,y)=>-1.4+(14-y)*.035,3.2,0);
 }
 slab('cape_tail','Cape split right torn tail',[[-3.62,10.1],[-.12,10.1],[-.48,5.7],[-.83,.75],[-1.72,1.92],[-2.34,.96],[-3.01,2.21],[-4.66,1.37],[-4.29,5.4]],(x,y)=>2.5+(10-y)*.13,.53,1);
 slab('cape_tail','Cape split left swept tail',[[.12,10.1],[3.62,10.1],[4.12,6.8],[4.6,3.1],[5.0,2.37],[3.75,1.8],[2.8,2.75],[2.0,1.37],[1.2,2.73],[.64,1.95]],(x,y)=>2.5+(10-y)*.14,.53,0);
 slab('body','Belt skull sculpted crown',[[-.94,14.61],[-.69,14.98],[.69,14.98],[.94,14.61],[.8,13.82],[.45,13.54],[-.45,13.54],[-.8,13.82]],-2.79,.55,13);
 for(const s of [-1,1]){
  slab('body','Belt skull socket '+s,[[s*.4-.28,14.04],[s*.4+.28,14.04],[s*.4+.23,14.45],[s*.4-.29,14.49]],-2.807,.014,8,true);
  slab('body','Belt skull tooth '+s,[[s*.3-.17,13.75],[s*.3+.17,13.75],[s*.3+.14,13.14],[s*.3-.13,13.34]],-2.83,.23,13);
 }
 const mapY=y=>y>=23.75?y-2.85:y*.88;
 const mappedVertices=new WeakSet();
 Outliner.elements.forEach(m=>{for(const v of Object.values(m.vertices))if(!mappedVertices.has(v)){mappedVertices.add(v);v[1]=mapY(v[1]);}});
 Group.all.forEach(g=>g.origin[1]=mapY(g.origin[1]));
 const originalOrigin=staffData.root.origin, newOrigin=staffGroup.origin;
 const delta=newOrigin.map((x,i)=>x-originalOrigin[i]);
 const materialMap={3:6,4:4,6:5,8:8,9:14,10:15,13:5,14:13,15:6};
 const legacyById=Object.fromEntries(staffData.elements.map(m=>[m.uuid,m]));
 function restoreChildren(source,target){
  for(const c of source.children){
   if(typeof c==='string'){
    const old=legacyById[c],data=JSON.parse(JSON.stringify(old));delete data.uuid;
    data.name='Restored '+data.name;
    Object.values(data.vertices).forEach(v=>v.forEach((x,i)=>v[i]=x+delta[i]));
    const material=materialMap[staffData.materials[old.name].material]??6;
    for(const face of Object.values(data.faces)){
     face.texture=tx;
     const u=material%4*32,v=Math.floor(material/4)*32;
     face.vertices.forEach((key,i)=>face.uv[key]=[u+5+(i===0||i===3?0:21),v+5+(i>=2?21:0)]);
     if(face.vertices.length===4&&data.vertices[face.vertices[2]].every((x,i)=>x===data.vertices[face.vertices[3]][i]))face.vertices.pop();
    }
    const m=new Mesh(data).addTo(target).init();created.push(m);
   }else{
    const g=new Group({name:c.name,origin:c.origin.map((x,i)=>x+delta[i]),rotation:c.rotation||[0,0,0]}).addTo(target).init();gs[g.name]=g;restoreChildren(c,g);
   }
  }
 }
 staffGroup.rotation=[0,0,10];restoreChildren(staffData.root,staffGroup);
 tex.edit(c=>{
   const t=c.getContext('2d');
   t.fillStyle='#d0bfd7';t.fillRect(96,32,32,32);
   t.fillStyle='#d0bfd7';t.fillRect(96,96,32,32);
   t.fillStyle='#ffd0f6';t.fillRect(101,101,22,22);
 },{no_undo:true});
 Canvas.updateAll();
 Undo.finishEdit('Rebuild hood opening, slanted eyes, compact silhouette and torn cloth; restore original staff',{elements:Outliner.elements.slice(),groups:Group.all.slice(),outliner:true,textures:[tex],bitmap:true});
 return {project:Project.uuid,elements:Outliner.elements.length,groups:Group.all.length,restoredStaff:staffData.elements.length,undo:Undo.history.length};
})()
