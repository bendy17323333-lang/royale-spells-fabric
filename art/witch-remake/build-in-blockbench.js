(() => {
  const project = ModelProject.all.find(p => p.uuid === '42191b01-5388-7b75-297c-d50828dc708a');
  if (!project || !project.select()) throw new Error('The isolated Witch rebuild project is unavailable');
  if (Outliner.elements.length) throw new Error('Refuse to replace a non-empty model; preserve the previous pass first');
  const created = [], groups = {}, textures = [];
  Undo.initEdit({elements: [], outliner: true, textures: [], animations: []});
  Project.texture_width = Project.texture_height = 128;
  const canvas = document.createElement('canvas'); canvas.width = canvas.height = 128;
  const ctx = canvas.getContext('2d'); ctx.imageSmoothingEnabled = false;
  const palette = [
    ['#6042bc','#7254d0','#4c3499'], ['#342457','#403069','#2a1c46'],
    ['#262739','#363a51','#1a1d2c'], ['#814e9d','#985fb2','#663e82'],
    ['#b98b3b','#e0b75e','#886124'], ['#66402e','#80523a','#472c25'],
    ['#d2c9b5','#ebe3d0','#b1a68f'], ['#c6b7d0','#d9cce0','#ae9dba'],
    ['#23202e','#2e293c','#181721'], ['#684179','#805591','#523364'],
    ['#855dd9','#a780eb','#6143ab'], ['#dbcce4','#e9dcf0','#c2b0ce'],
    ['#443768','#594878','#34294f'], ['#dfb44c','#f0d477','#b68a32'],
    ['#bd36de','#ed75ef','#8224aa'], ['#c6b7d0','#d9cce0','#ae9dba']
  ];
  for (let i=0;i<16;i++) {
    const x=i%4*32, y=Math.floor(i/4)*32, c=palette[i];
    ctx.fillStyle=c[0];ctx.fillRect(x,y,32,32);
    if(i!==15){
      ctx.fillStyle=c[1];ctx.fillRect(x+2,y+2,28,2);ctx.fillRect(x+2,y+4,2,22);
      ctx.fillStyle=c[2];ctx.fillRect(x+28,y+6,2,24);ctx.fillRect(x+5,y+28,23,2);
      if([0,1,3,9,10,12].includes(i)){
        ctx.fillStyle=c[2];ctx.fillRect(x+21,y+6,2,17);ctx.fillRect(x+9,y+22,2,6);
        ctx.fillStyle=c[1];ctx.fillRect(x+7,y+9,2,10);ctx.fillRect(x+15,y+18,3,2);
      }
      if(i===5){ctx.fillStyle=c[2];ctx.fillRect(x+7,y+5,2,19);ctx.fillRect(x+18,y+13,2,15);}
      if(i===6){ctx.fillStyle=c[1];ctx.fillRect(x+6,y+5,15,12);ctx.fillStyle=c[2];ctx.fillRect(x+21,y+20,4,4);}
    }
  }
  const fx=96,fy=96;
  ctx.fillStyle='#b7a6c3';ctx.fillRect(fx,fy+3,3,26);ctx.fillRect(fx+29,fy+3,3,26);
  ctx.fillStyle='#d6c8df';ctx.fillRect(fx+4,fy+3,24,6);
  ctx.fillStyle='#564067';ctx.fillRect(fx+5,fy+10,8,2);ctx.fillRect(fx+19,fy+10,8,2);
  ctx.fillStyle='#8f289f';ctx.fillRect(fx+5,fy+12,8,5);ctx.fillRect(fx+19,fy+12,8,5);
  ctx.fillStyle='#ee66dd';ctx.fillRect(fx+6,fy+12,6,3);ctx.fillRect(fx+20,fy+12,6,3);
  ctx.fillStyle='#ffb0ed';ctx.fillRect(fx+7,fy+12,3,1);ctx.fillRect(fx+21,fy+12,3,1);
  ctx.fillStyle='#af9dbb';ctx.fillRect(fx+15,fy+18,2,1);
  ctx.fillStyle='#6b427a';ctx.fillRect(fx+13,fy+24,6,1);ctx.fillRect(fx+15,fy+25,3,1);
  const texture = new Texture({name:'witch_materials.png',width:128,height:128,uv_width:128,uv_height:128});
  texture.fromDataURL(canvas.toDataURL('image/png')).add(false);textures.push(texture);
  const texId=texture.uuid;
  function group(name,parent,origin) {
    const g=new Group({name,origin,rotation:[0,0,0]});g.addTo(parent?groups[parent]:'root').init();groups[name]=g;return g;
  }
  group('body','',[0,12,0]);group('head','body',[0,24,0]);
  group('right_arm','body',[-5.15,22.6,0]);group('left_arm','body',[5.15,22.6,0]);
  group('right_forearm','right_arm',[-5.15,16.6,0]);group('left_forearm','left_arm',[5.15,16.6,0]);
  group('staff','right_forearm',[-5.15,12.2,-1.45]);
  group('staff_crystal','staff',[-5.15,29.2,-1.45]);
  group('right_leg','body',[-1.75,12,0]);group('left_leg','body',[1.75,12,0]);
  group('robe_right','body',[-1.7,12.8,-2.55]);group('robe_left','body',[1.7,12.8,-2.55]);
  group('robe_side_right','body',[-2.9,12.8,0]);group('robe_side_left','body',[2.9,12.8,0]);
  group('cape','body',[0,22.7,2.55]);group('cape_tail','cape',[0,10,3.0]);
  group('cape_ribbon_-1','body',[-2.65,23,-2.9]);group('cape_ribbon_1','body',[2.65,23,-2.9]);
  function solid(part,name,polys,mat,facePaint=false) {
    const mesh=new Mesh({name,vertices:{},faces:{},origin:[0,0,0]});
    const vertices={};let k=0;const center=[0,0,0];let n=0;
    for(const poly of polys)for(const p of poly){for(let j=0;j<3;j++)center[j]+=p[j];n++;}
    center.forEach((v,i)=>center[i]=v/n);
    for(const points0 of polys){
      let pts=points0.slice(),keys=[];const a=pts[0],b=pts[1],c=pts[2];
      const ab=b.map((v,i)=>v-a[i]),ac=c.map((v,i)=>v-a[i]);
      const norm=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]];
      const mid=[0,1,2].map(i=>pts.reduce((s,p)=>s+p[i],0)/pts.length);
      if(norm.reduce((s,v,i)=>s+v*(mid[i]-center[i]),0)<0)pts.reverse();
      for(const p of pts){let key='v'+k++;vertices[key]=p;keys.push(key);}
      const tileX=mat%4*32, tileY=Math.floor(mat/4)*32,uv={};
      const w=Math.min(26,Math.max(2,Math.hypot(...pts[1].map((v,i)=>v-pts[0][i]))*3));
      const h=Math.min(26,Math.max(2,Math.hypot(...pts[2].map((v,i)=>v-pts[1][i]))*3));
      keys.forEach((key,i)=>uv[key]=[tileX+3+(i===0||i===3?0:w),tileY+3+(i>=2?h:0)]);
      if(facePaint&&pts.every(p=>Math.abs(p[2]+3.48)<.0001)){
        keys.forEach((key,i)=>uv[key]=[96+(pts[i][0]+3.15)/6.3*32,96+(30.65-pts[i][1])/6.2*32]);
      }
      mesh.vertices=vertices;
      mesh.addFaces(new MeshFace(mesh,{vertices:keys,uv,texture:texId}));
    }
    mesh.addTo(groups[part]).init();created.push(mesh);return mesh;
  }
  function box(part,name,x,y,z,w,h,d,mat){const X=x+w,Y=y+h,Z=z+d;
    return solid(part,name,[[[x,y,z],[X,y,z],[X,Y,z],[x,Y,z]],[[X,y,Z],[x,y,Z],[x,Y,Z],[X,Y,Z]],
      [[x,y,Z],[x,y,z],[x,Y,z],[x,Y,Z]],[[X,y,z],[X,y,Z],[X,Y,Z],[X,Y,z]],
      [[x,Y,z],[X,Y,z],[X,Y,Z],[x,Y,Z]],[[x,y,Z],[X,y,Z],[X,y,z],[x,y,z]]],mat);}
  function bevel(part,name,x,y,z,w,h,d,c,mat,facePaint=false){
    const ring=[[x+c,y],[x+w-c,y],[x+w,y+c],[x+w,y+h-c],[x+w-c,y+h],[x+c,y+h],[x,y+h-c],[x,y+c]];
    const back=ring.map(p=>[p[0],p[1],z+d]),front=ring.map(p=>[p[0],p[1],z]),polys=[];
    for(let i=1;i<7;i++){polys.push([front[0],front[i],front[i+1]]);polys.push([back[0],back[i+1],back[i]]);}
    for(let i=0;i<8;i++)polys.push([front[i],front[(i+1)%8],back[(i+1)%8],back[i]]);
    return solid(part,name,polys,mat,facePaint);
  }
  function panel(part,name,top,bottom,yt,yb,z,d,mat){const[a,b]=top,[c,e]=bottom;
    return solid(part,name,[[[a,yt,z],[b,yt,z],[e,yb,z],[c,yb,z]],[[b,yt,z+d],[a,yt,z+d],[c,yb,z+d],[e,yb,z+d]],
      [[a,yt,z+d],[a,yt,z],[c,yb,z],[c,yb,z+d]],[[b,yt,z],[b,yt,z+d],[e,yb,z+d],[e,yb,z]],
      [[a,yt,z],[a,yt,z+d],[b,yt,z+d],[b,yt,z]],[[c,yb,z],[e,yb,z],[e,yb,z+d],[c,yb,z+d]]],mat);}
  function beam(part,name,a,b,r,mat){const v=new THREE.Vector3(...b).sub(new THREE.Vector3(...a)).normalize();
    const u=new THREE.Vector3(0,0,1);if(Math.abs(v.dot(u))>.96)u.set(1,0,0);u.cross(v).normalize();const w=v.clone().cross(u).normalize();
    const rings=[a,b].map(pt=>Array.from({length:6},(_,i)=>new THREE.Vector3(...pt).addScaledVector(u,Math.cos(i*Math.PI/3)*r).addScaledVector(w,Math.sin(i*Math.PI/3)*r).toArray()));
    const polys=[];for(let i=0;i<6;i++)polys.push([rings[0][i],rings[0][(i+1)%6],rings[1][(i+1)%6],rings[1][i]]);
    for(let i=1;i<5;i++){polys.push([rings[0][0],rings[0][i],rings[0][i+1]]);polys.push([rings[1][0],rings[1][i+1],rings[1][i]]);}return solid(part,name,polys,mat);}
  function gem(part,name,c,r,mat){const[x,y,z]=c;return solid(part,name,[[[x,y+r,z],[x-r*.7,y,z],[x,y,z-r*.7]],[[x,y+r,z],[x,y,z-r*.7],[x+r*.7,y,z]],[[x,y+r,z],[x+r*.7,y,z],[x,y,z+r*.7]],[[x,y+r,z],[x,y,z+r*.7],[x-r*.7,y,z]],[[x,y-r,z],[x,y,z-r*.7],[x-r*.7,y,z]],[[x,y-r,z],[x+r*.7,y,z],[x,y,z-r*.7]],[[x,y-r,z],[x,y,z+r*.7],[x+r*.7,y,z]],[[x,y-r,z],[x-r*.7,y,z],[x,y,z+r*.7]]],mat);}
  bevel('head','Face - broad short beveled pixel face',-3.15,24.45,-3.48,6.3,6.2,4.85,.42,15,true);
  bevel('head','Hood back - closed square cloth shell',-4.45,23.75,1.35,8.9,9.2,2.7,.55,0);
  bevel('head','Hood roof - shallow folded cap',-4.45,31.35,-3.95,8.9,1.6,6.75,.42,0);
  bevel('head','Hood roof secondary fold',-3.55,32.83,-2.8,7.1,.45,5.6,.18,10);
  for(const s of [-1,1]){
    const x=s<0?-4.5:3.18;
    bevel('head','Hood side '+s,x,23.9,-3.85,1.32,7.75,5.4,.3,0);
    panel('head','Hood cheek fold '+s,[s<0?-4.15:3.15,s<0?-3.15:4.15],[s<0?-3.9:3.0,s<0?-3.0:3.9],28.6,23.35,-3.96,.44,10);
  }
  panel('head','Central pointed brow fold',[-1.35,1.35],[-.3,.3],31.55,29.4,-3.98,.48,0);
  box('body','Neck inside hood',-1.65,23,-1.15,3.3,1.9,2.8,7);
  bevel('body','Fitted dark blue breastplate',-3.25,14,-2.3,6.5,9.6,4.8,.32,2);
  panel('body','Upper collar silver V left',[-2.5,-.05],[-1.05,-.05],23.65,21.5,-2.46,.35,11);
  panel('body','Upper collar silver V right',[.05,2.5],[.05,1.05],23.65,21.5,-2.46,.35,11);
  bevel('body','Waist skirt base',-3.05,10.7,-2.05,6.1,4.6,4.25,.3,3);
  box('body','Gold belt',-3.2,13.7,-2.38,6.4,1.1,4.7,4);
  bevel('body','Small skull buckle',-.85,13.18,-2.78,1.7,1.35,.65,.2,6);
  box('body','Buckle left eye',-.6,13.85,-2.83,.4,.35,.05,8);box('body','Buckle right eye',.2,13.85,-2.83,.4,.35,.05,8);
  for(const s of [-1,1]){
    const side=s<0?'right':'left',x=s*5.15;
    bevel(side+'_arm','Shoulder upper armour '+side,x-1.75,21.65,-1.95,3.5,1.9,4.15,.3,4);
    bevel(side+'_arm','Shoulder raised gold plate '+side,x-1.35,23.15,-1.65,2.7,.85,3.45,.24,13);
    box(side+'_arm','Bare upper arm '+side,x-1.25,16.2,-1.25,2.5,5.85,2.7,7);
    bevel(side+'_forearm','Blue gauntlet '+side,x-1.4,12.65,-1.5,2.8,3.8,3.05,.18,12);
    box(side+'_forearm','Gold cuff upper '+side,x-1.48,15.85,-1.58,2.96,.66,3.21,4);
    box(side+'_forearm','Gold cuff lower '+side,x-1.48,12.3,-1.58,2.96,.66,3.21,4);
    bevel(side+'_forearm','Hand palm '+side,x-1.17,10.9,-1.18,2.34,1.64,2.45,.19,7);
    for(let i=0;i<3;i++)box(side+'_forearm','Block finger '+side+' '+i,x-.94+i*.65,10.69,-1.4,.52,.72,.63,7);
    box(side+'_forearm','Thumb '+side,x+(s<0?1:-1.63),11.1,-1.32,.64,1.02,1.17,7);
    box(side+'_leg','Stocking '+side,s*1.65-1.26,1.1,-1.3,2.52,11,2.7,1);
    bevel(side+'_leg','Boot '+side,s*1.65-1.42,0,-2,2.84,2.3,3.75,.22,8);
  }
  panel('robe_right','Front robe right',[-2.9,-.1],[-4.0,-.1],13.2,2.15,-2.7,.56,3);
  panel('robe_left','Front robe left',[.1,2.9],[.1,3.7],13.2,2.75,-2.7,.56,3);
  panel('robe_right','Right folded border',[-2.95,-2.35],[-4.05,-3.36],13.35,1.9,-2.84,.36,10);
  panel('robe_left','Left folded border',[2.25,2.95],[3.05,3.81],13.35,2.5,-2.84,.36,0);
  for(const s of [-1,1]){
    const part=s<0?'robe_side_right':'robe_side_left';
    panel(part,'Outer hanging cloth '+s,[s<0?-3.28:2.55,s<0?-2.55:3.28],[s<0?-4.65:3.78,s<0?-3.78:4.65],14.6,1.18,-1.5,4.0,0);
    panel('cape_ribbon_'+s,'Hood front long fold '+s,[s<0?-3.18:1.78,s<0?-1.78:3.18],[s<0?-4.06:2.54,s<0?-2.54:4.06],23.2,s<0?4.7:6.2,-2.89,.47,0);
  }
  panel('cape','Back cape shoulder drape',[-3.4,3.4],[-3.62,3.62],23.5,9.6,2.42,.65,1);
  panel('cape_tail','Back split cape right',[-3.62,-.12],[-4.22,-.72],10,1.05,2.54,.58,0);
  panel('cape_tail','Back split cape left',[.12,3.62],[.64,4.04],10,1.75,2.54,.58,0);
  const sx=-5.15,sz=-1.45;
  beam('staff','Wood staff shaft',[sx,2.25,sz],[sx,28.2,sz],.45,5);
  for(const y of [5.7,9.2,12.1,17.2,23.4,26.7])bevel('staff','Staff wrap '+y,sx-.56,y,sz-.56,1.12,.72,1.12,.15,y===26.7?4:5);
  bevel('staff','Gold skull saddle',sx-1.75,27.0,sz-1.5,3.5,1.55,3,.28,4);
  bevel('staff','Ram skull cranium',sx-2.8,28.3,sz-2.18,5.6,5.45,4.5,.8,6);
  bevel('staff','Ram skull snout',sx-1.66,27.8,sz-2.66,3.32,2.95,2.42,.45,6);
  for(const s of [-1,1]){
    bevel('staff','Ram eye hollow '+s,sx+s*1.38-.7,30.04,sz-2.24,1.4,1.6,.07,.2,8);
    beam('staff','Ram horn rising '+s,[sx+s*2.35,32.5,sz],[sx+s*3.5,33.75,sz+.2],.72,4);
    beam('staff','Ram horn curl '+s,[sx+s*3.5,33.75,sz+.2],[sx+s*4.35,32.2,sz+.05],.67,4);
    beam('staff','Ram horn lower '+s,[sx+s*4.35,32.2,sz+.05],[sx+s*3.9,30.85,sz-.8],.53,13);
    beam('staff','Ram horn tip '+s,[sx+s*3.9,30.85,sz-.8],[sx+s*3.25,31.1,sz-1.2],.3,4);
    box('staff','Ram fang '+s,sx+s*.85-.3,27.0,sz-2.4,.6,1.4,.7,6);
  }
  box('staff','Ram nose hollow',sx-.4,28.85,sz-2.72,.8,.7,.07,8);
  Canvas.updateAll();
  Undo.finishEdit('Build Witch from native meshes and hand-painted pixel atlas',{elements:created,outliner:true,textures,animations:[]});
  Project.name='Royale Spells - Witch 1.9.1';
  return {project:Project.uuid,name:Project.name,meshes:created.length,groups:Object.keys(groups).length,texture:[128,128],nativeUndo:Undo.history.length};
})()
