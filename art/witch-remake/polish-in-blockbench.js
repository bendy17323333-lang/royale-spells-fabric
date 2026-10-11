(()=>{
 const project=ModelProject.all.find(p=>p.uuid==='42191b01-5388-7b75-297c-d50828dc708a');
 if(!project||!project.select())throw Error('Missing isolated Witch');
 const elements=Outliner.elements.slice();Undo.initEdit({elements,groups:Group.all.slice(),outliner:true,uv_only:false});
 let triangles=0;
 for(const m of elements){
  for(const face of Object.values(m.faces)){
   const seen=new Set();face.vertices=face.vertices.filter(k=>{const id=m.vertices[k].join(',');if(seen.has(id))return false;seen.add(id);return true;});
   if(face.vertices.length===3)triangles++;
  }
 }
 for(const m of elements){
  if(m.name.startsWith('Face - small'))for(const v of Object.values(m.vertices))if(v[1]<23.4)v[1]=22.55+(v[1]-22.05)*.63;
  if(/Slanted magical eye|Upper violet eyelid|Eye light inner facet/.test(m.name)){
   const sign=m.name.endsWith('-1')?-1:1;
   for(const v of Object.values(m.vertices)){v[1]-=(Math.abs(v[0])-1.5)*.13;v[1]+=.15;}
  }
  if(m.name==='Small reserved mouth')for(const v of Object.values(m.vertices)){v[0]*=.68;v[1]=23.1+(v[1]-23.05)*.52-v[0]*.065;}
  const bounds=[0,1,2].map(i=>[Math.min(...Object.values(m.vertices).map(v=>v[i])),Math.max(...Object.values(m.vertices).map(v=>v[i]))]);
  for(const face of Object.values(m.faces)){
   const samples=face.vertices.map(k=>face.uv[k]);
   if(samples.every(uv=>uv[0]===samples[0][0]&&uv[1]===samples[0][1]))continue;
   const matX=Math.floor(samples[0][0]/32)*32,matY=Math.floor(samples[0][1]/32)*32;
   const norm=face.getNormal(true),axis=norm.map(Math.abs).indexOf(Math.max(...norm.map(Math.abs)));
   const uAxis=axis===0?2:0,vAxis=axis===1?2:1;
   const w=bounds[uAxis][1]-bounds[uAxis][0],h=bounds[vAxis][1]-bounds[vAxis][0];
   const scale=Math.min(2,25/Math.max(1,w,h));
   for(const key of face.vertices){const v=m.vertices[key];face.uv[key]=[matX+4+(v[uAxis]-bounds[uAxis][0])*scale,matY+4+(bounds[vAxis][1]-v[vAxis])*scale];}
  }
 }
 Canvas.updateAll();Undo.finishEdit('Polish compact facial proportions, coherent pixel UV and restored staff triangles',{elements,groups:Group.all.slice(),outliner:true});
 return {project:Project.uuid,triangles,meshes:elements.length};
})()
