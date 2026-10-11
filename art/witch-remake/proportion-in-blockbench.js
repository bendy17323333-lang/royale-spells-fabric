(async()=>{
 const p=ModelProject.all.find(p=>p.uuid==='42191b01-5388-7b75-297c-d50828dc708a');
 if(!p||!p.select())throw Error('Missing isolated Witch');
 const elements=Outliner.elements.slice(),groups=Group.all.slice(),tex=Texture.all[0];
 Undo.initEdit({elements,groups,outliner:true,textures:[tex],bitmap:true});
 const yBody=y=>y<12.3?y:12.3+(y-12.3)*.88;
 const headY=y=>yBody(21.15)-1.15+(y-21.15)*.97;
 function adjust(v,part){
   if(part==='staff'||part==='staff_crystal'){v[0]-=.38;return;}
   if(part==='head'){v[0]*=.91;v[1]=headY(v[1]);v[2]*=.94;return;}
   v[1]=yBody(v[1]);
   if(/arm/.test(part))v[0]+=Math.sign(v[0])*.38;
   else if(part.startsWith('robe')||part.startsWith('cape'))v[0]*=1.07;
 }
 for(const m of elements){const seen=new WeakSet();for(const v of Object.values(m.vertices))if(!seen.has(v)){seen.add(v);adjust(v,m.parent.name);}}
 for(const g of groups)adjust(g.origin,g.name);
 // Keep the face nested inside cloth; the long exposed neck was visually
 // separating the head from the shoulders despite the short head itself.
 const neck=elements.find(m=>m.name==='Neck inside hood');
 for(const v of Object.values(neck.vertices))v[1]=Math.min(v[1],20.05);
 // Shorten one front strip and sweep both out after the waist. Broad folds,
 // not parallel rails running the whole length of the dress.
 for(const m of elements.filter(m=>m.name.startsWith('Draped hood front lappet'))){
   const s=m.name.endsWith('-1')?-1:1;
   for(const v of Object.values(m.vertices)){
     if(v[1]<12){v[0]+=s*(12-v[1])*.07;v[2]+=(12-v[1])*.105;}
     if(s===1&&v[1]<10)v[1]=10+(v[1]-10)*.67;
   }
 }
 // Wrap the shoulder plate down the arm instead of a horizontal gold lid.
 for(const m of elements.filter(m=>/Shoulder upper armour|Shoulder raised gold/.test(m.name))){
   const sign=m.parent.name==='right_arm'?-1:1;
   for(const v of Object.values(m.vertices))v[1]-=Math.max(0,sign*v[0]-5.2)*.34;
 }
 // The old ram skull remains intact. A crooked wood shaft has a deliberate
 // two-bend centreline; grip and head attachment keep their exact anchors.
 const shaft=elements.find(m=>m.name==='Restored staff_shaft');
 if(shaft){
   const g=shaft.parent,tx=tex.uuid;shaft.remove();
   const cx=g.origin[0],cz=g.origin[2],ys=[1.736,8.8,10.736,16.2,21,24.736];
   const offsets=[-.14,.21,0,-.25,.12,0],vertices={},faces={};
   for(let j=0;j<ys.length;j++)for(let k=0;k<4;k++){
     const a=Math.PI/4+k*Math.PI/2+(j%2)*.08;
     vertices['v'+j+'_'+k]=[cx+offsets[j]+Math.cos(a)*.73,ys[j],cz+Math.sin(a)*.73];
   }
   const m=new Mesh({name:'Crooked dark wooden staff shaft',vertices,faces:{},origin:[0,0,0]});
   for(let j=0;j<ys.length-1;j++)for(let k=0;k<4;k++){
     const ks=['v'+j+'_'+k,'v'+j+'_'+((k+1)%4),'v'+(j+1)+'_'+((k+1)%4),'v'+(j+1)+'_'+k];
     const uv={};ks.forEach((q,i)=>uv[q]=[36+(i===1||i===2?5:0),37+(i>=2?Math.min(23,(ys[j+1]-ys[j])*2):0)]);
     m.addFaces(new MeshFace(m,{vertices:ks,uv,texture:tx}));
   }
   m.addTo(g).init();
 }
 tex.edit(canvas=>{
   const c=canvas.getContext('2d');
   // Deliberate sparse cloth pixels at a consistent density, no airbrushing.
   for(const [tile,lo,hi] of [[0,'#463087','#6145ad'],[1,'#30234e','#453363'],[3,'#4b2b61','#684179']]){
     const ox=tile%4*32,oy=Math.floor(tile/4)*32;
     c.fillStyle=lo;for(const [x,y,w,h] of [[7,8,2,3],[17,12,3,2],[10,22,2,4],[24,23,2,2],[21,6,2,3]])c.fillRect(ox+x,oy+y,w,h);
     c.fillStyle=hi;for(const [x,y,w,h] of [[10,6,3,2],[20,9,2,4],[6,20,2,3],[16,25,4,1]])c.fillRect(ox+x,oy+y,w,h);
   }
 },{no_undo:true});
 Canvas.updateAll();Undo.finishEdit('Correct head-to-body silhouette, conceal neck, shorten torso and shape cloth',{elements:Outliner.elements.slice(),groups:Group.all.slice(),outliner:true,textures:[tex],bitmap:true});
 return {project:Project.uuid,meshes:Outliner.elements.length};
})()
