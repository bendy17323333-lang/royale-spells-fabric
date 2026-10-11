(()=>{
 const root='C:/Users/BliBe/Documents/Codex/2026-09-04/new-chat/work/';
 const receipt=[];
 for(const kind of ['witch','witch_evolution']){
  const p=ModelProject.all.find(p=>p.name===kind&&p.save_path.includes('royale-spells-fabric-1.20.1-standalone'));
  if(!p||!p.select())throw Error('Missing isolated '+kind);
  if(Outliner.elements.some(m=>m.name==='Tailored dimensional bodice'))throw Error('Already remodeled');
  const gs=Object.fromEntries(Group.all.map(g=>[g.name,g])),tex=Texture.all[0],evo=kind==='witch_evolution';
  Undo.initEdit({elements:Outliner.elements.slice(),outliner:true,animations:Animation.all.slice()});
  const remapY=y=>y<=10.56?y*1.17424:y<=19.8?y+1.84+(y-10.56)*.058:y+2.376;
  const touched=new WeakSet();
  for(const m of Outliner.elements){
   for(const v of Object.values(m.vertices||{}))if(!touched.has(v)){touched.add(v);v[1]=remapY(v[1]);}
   m.origin[1]=remapY(m.origin[1]);
  }
  for(const g of Group.all)g.origin[1]=remapY(g.origin[1]);
  const body=Outliner.elements.find(m=>m.name==='Fitted dark blue breastplate');
  const bodyFace=Object.values(body.faces)[0],bodyUv=Object.values(bodyFace.uv).map(a=>a.slice());
  body.remove();
  const mesh=(name,vertices,faces,parent,rect)=>{
   const m=new Mesh({name,origin:[0,0,0],vertices:{},faces:{}});let n=0;
   for(const face of faces){const keys=[],uv={};for(let i=0;i<face.length;i++){const k='v'+n++;m.vertices[k]=vertices[face[i]].slice();keys.push(k);uv[k]=[rect[0]+(i===1||i===2?rect[2]:0),rect[1]+(i>=2?rect[3]:0)];}m.addFaces(new MeshFace(m,{vertices:keys,uv,texture:tex.uuid}));}
   m.addTo(parent).init();return m;
  };
  const rings=[
   {y:14.1,w:2.9,z:2.1,b:2.2},
   {y:16.5,w:2.72,z:2.2,b:2.25},
   {y:19.25,w:3.2,z:3.25,b:2.4},
   {y:20.6,w:3.24,z:3.60,b:2.45},
   {y:22.05,w:2.87,z:2.5,b:2.2}];
  const verts=[];
  for(const r of rings)verts.push(...[[-r.w,r.y,-r.z*.70],[-r.w*.49,r.y,-r.z],[0,r.y,-r.z*.92],[r.w*.49,r.y,-r.z],[r.w,r.y,-r.z*.70],[r.w,r.y,r.b*.62],[r.w*.65,r.y,r.b],[-r.w*.65,r.y,r.b],[-r.w,r.y,r.b*.62]]);
  const faces=[],n=9;
  for(let k=0;k<rings.length-1;k++)for(let j=0;j<n;j++)faces.push([k*n+j,k*n+(j+1)%n,(k+1)*n+(j+1)%n,(k+1)*n+j]);
  for(let j=1;j<n-1;j++){faces.push([0,j+1,j]);faces.push([(rings.length-1)*n,(rings.length-1)*n+j,(rings.length-1)*n+j+1]);}
  const br=[Math.min(...bodyUv.map(p=>p[0])),Math.min(...bodyUv.map(p=>p[1])),6,6];
  mesh('Tailored dimensional bodice',verts,faces,gs.body,br);
  const nv=[],nr=[{y:21.7,x:1.35,z:1.5},{y:23.3,x:1.2,z:1.25},{y:24.15,x:1.35,z:1.4}];
  for(const r of nr)for(let i=0;i<8;i++){const a=(i+.5)*Math.PI/4;nv.push([Math.cos(a)*r.x,r.y,Math.sin(a)*r.z-.48]);}
  const nf=[];for(let k=0;k<2;k++)for(let j=0;j<8;j++)nf.push([k*8+j,k*8+(j+1)%8,(k+1)*8+(j+1)%8,(k+1)*8+j]);
  nf.push([16,17,18,19],[16,19,20,21],[16,21,22,23]);
  const face=Outliner.elements.find(m=>m.name.startsWith('Face -')),fu=Object.values(Object.values(face.faces)[0].uv);
  mesh('Neck joined beneath hood and collar',nv,nf,gs.body,[Math.min(...fu.map(p=>p[0])),Math.min(...fu.map(p=>p[1])),3,4]);
  for(const m of Outliner.elements)if(m.name.startsWith('Upper collar silver V'))for(const v of Object.values(m.vertices))v[2]-=.2;
  if(evo){
   for(const m of Outliner.elements.slice())if(/^(Stocking|Boot) /.test(m.name))m.remove();
   const vs=[],fs=[],segments=10;
   for(const [y,w,d] of [[2.25,4.32,2.95],[7.6,3.78,2.45],[13.15,2.95,2.15]])for(let j=0;j<segments;j++){const a=j*Math.PI*2/segments;vs.push([Math.sin(a)*w,y+(y<3?.33*Math.sin(j*2.1):0),Math.cos(a)*d]);}
   for(let k=0;k<2;k++)for(let j=0;j<segments;j++)fs.push([k*segments+j,k*segments+(j+1)%segments,(k+1)*segments+(j+1)%segments,(k+1)*segments+j]);
   const inner=vs.map(([x,y,z])=>[x*.92,y+.08,z*.91]);vs.push(...inner);
   for(let k=0;k<2;k++)for(let j=0;j<segments;j++)fs.push([30+k*segments+j,30+(k+1)*segments+j,30+(k+1)*segments+(j+1)%segments,30+k*segments+(j+1)%segments]);
   for(let j=0;j<segments;j++)fs.push([j,30+j,30+(j+1)%segments,(j+1)%segments]);
   mesh('Hollow suspended robe lining - no legs',vs,fs,gs.body,[48,65,8,12]);
   const seen=new WeakSet();
   for(const m of Outliner.elements)if(/robe|tail|lappet/i.test(m.name)&&!m.name.startsWith('Hollow'))for(const v of Object.values(m.vertices||{}))if(!seen.has(v)){seen.add(v);if(v[1]<8)v[1]=2.15+v[1]*.73;}
  }
  for(const a of Animation.all.slice())a.remove();
  const base={right_arm:[14,0,-7],right_forearm:[24,0,0],staff:[-38,0,-10],left_arm:[10,0,15],left_forearm:[22,0,0]};
  const animators={};
  for(const [name,angle]of Object.entries(base))animators[gs[name].uuid]={name,type:'bone',keyframes:[0,1,2].map(t=>({channel:'rotation',time:t,interpolation:'catmullrom',data_points:[{x:String(angle[0]),y:String(angle[1]),z:String(angle[2])}]}))};
  new Animation({name:'animation.'+kind+'.idle_reference',loop:'loop',length:2,animators}).add();
  Undo.finishEdit('Lengthen legs, connect neck, shape clothed torso and float evolved robe',{elements:Outliner.elements.slice(),outliner:true,animations:Animation.all.slice()});Canvas.updateAll();
  Blockbench.writeFile(root+'royale-spells-fabric-1.20.1-standalone/art/blockbench/'+kind+'.bbmodel',{content:Codecs.project.compile()});
  receipt.push({name:kind,id:p.uuid,meshes:Outliner.elements.length,neck:true,legs:Outliner.elements.filter(e=>/^(Stocking|Boot) /.test(e.name)).length,hoodTop:Math.max(...Outliner.elements.filter(m=>m.name.startsWith('Hood')).flatMap(m=>Object.values(m.vertices).map(v=>v[1])))});
 }
 return receipt;
})()
