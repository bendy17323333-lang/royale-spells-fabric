(()=>{
 const p=ModelProject.all.find(p=>p.uuid==='42191b01-5388-7b75-297c-d50828dc708a');
 if(!p||!p.select())throw Error('Missing isolated Witch');
 const es=Outliner.elements.slice(),head=Group.all.find(g=>g.name==='head'),tex=Texture.all[0];
 Undo.initEdit({elements:es,outliner:true});
 for(const m of es){
  if(m.name.startsWith('Face -'))for(const v of Object.values(m.vertices))if(v[1]<22)v[1]=22+(v[1]-22)*.57;
  if(m.name==='Small reserved mouth')for(const v of Object.values(m.vertices))v[1]=21.45+(v[1]-20.82)*.8;
  if(m.name==='Hood front cloth brow with central point')for(const v of Object.values(m.vertices))if(v[1]<25)v[1]-=(25-v[1])*.23;
 }
 const poly=[[-2.8,20.0],[-2.3,19.4],[0,19.12],[2.3,19.4],[2.8,20.0],[2.73,21.35],[1.8,20.93],[0,20.7],[-1.8,20.93],[-2.73,21.35]];
 const vv=poly.map(([x,y])=>[x,y,-3.19]);vv.push(...vv.map(v=>[v[0],v[1],v[2]+.5]));
 const ff=[],n=poly.length;
 for(const t of THREE.ShapeUtils.triangulateShape(poly.map(q=>new THREE.Vector2(...q)),[])){ff.push([t[0],t[2],t[1]]);ff.push(t.map(i=>i+n));}
 for(let i=0;i<n;i++)ff.push([i,(i+1)%n,(i+1)%n+n,i+n]);
 const m=new Mesh({name:'Folded hood throat lining',origin:[0,0,0],vertices:{},faces:{}});let j=0;
 for(const f of ff){const ks=[],uv={};for(const i of f){const k='v'+j++;m.vertices[k]=vv[i].slice();ks.push(k);uv[k]=[37+(vv[i][0]+2.8)*2,4+(21.4-vv[i][1])*3];}m.addFaces(new MeshFace(m,{vertices:ks,uv,texture:tex.uuid}));}m.addTo(head).init();
 Canvas.updateAll();Undo.finishEdit('Shorten visible lower face and nest it inside the hood',{elements:Outliner.elements.slice(),outliner:true});
 return{meshes:Outliner.elements.length};
})()
