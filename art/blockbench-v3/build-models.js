/* Run through Blockbench MCP risky_eval in a new Generic Model project.
 * Meshes are made inside Blockbench; the saved .bbmodel is the editable source.
 * All coordinates use Blockbench Y-up, -Z front. Runtime export handles Y-down.
 * No raster images are created or altered here: UVs use the ImageGen atlas.
 */
globalThis.RoyaleModelV3 = (() => {
 const TAU=Math.PI*2, A=1254, TILE=A/4;
 let groups=[],meshes=[],tex,flameTex;
 const add=(a,b)=>a.map((v,i)=>v+b[i]);
 const sub=(a,b)=>a.map((v,i)=>v-b[i]);
 const mul=(a,k)=>a.map(v=>v*k);
 const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
 const dot=(a,b)=>a.reduce((s,v,i)=>s+v*b[i],0);
 const unit=a=>mul(a,1/Math.max(.00001,Math.hypot(...a)));
 function group(name,origin,parent=''){let g={name,origin,parent};groups.push(g);return name;}
 function mesh(name,parent,tile,emissive=false){let m={name,parent,tile,emissive,vertices:[],faces:[],uvs:[]};meshes.push(m);return m;}
 function face(m,p,normal,uv){
   if(normal&&dot(cross(sub(p[1],p[0]),sub(p[2],p[0])),normal)<0){p=p.slice().reverse();if(uv)uv=uv.slice().reverse();}
   let ids=p.map(v=>{m.vertices.push(v);return m.vertices.length-1;});
   if(!uv){let n=unit(cross(sub(p[1],p[0]),sub(p[2],p[0]))),axis=n.map(Math.abs).indexOf(Math.max(...n.map(Math.abs)));let axes=axis===0?[2,1]:axis===1?[0,2]:[0,1];uv=p.map(v=>[.14+((v[axes[0]]+12)/24)*.70,.86-((v[axes[1]]+12)/36)*.70]);}
   m.faces.push(ids);m.uvs.push(uv.map(([u,v])=>[((m.tile%4)+Math.max(.025,Math.min(.975,u)))*TILE,(Math.floor(m.tile/4)+Math.max(.025,Math.min(.975,v)))*TILE]));
 }
 function loft(name,parent,rows,tile,sides=12,emissive=false,cap=true){
   let m=mesh(name,parent,tile,emissive),rings=rows.map(([x,y,z,rx,rz])=>Array.from({length:sides},(_,i)=>[x+Math.cos(i*TAU/sides)*rx,y,z+Math.sin(i*TAU/sides)*(rz??rx)]));
   for(let j=0;j<rings.length-1;j++)for(let i=0;i<sides;i++){
     let k=(i+1)%sides,p=[rings[j][i],rings[j+1][i],rings[j+1][k],rings[j][k]],center=mul(add(rows[j].slice(0,3),rows[j+1].slice(0,3)),.5),n=sub(mul(p.reduce((s,v)=>add(s,v),[0,0,0]),.25),center);
     face(m,p,n,[[.06+i/sides*.88,.92-j/(rings.length-1)*.84],[.06+i/sides*.88,.92-(j+1)/(rings.length-1)*.84],[.06+(i+1)/sides*.88,.92-(j+1)/(rings.length-1)*.84],[.06+(i+1)/sides*.88,.92-j/(rings.length-1)*.84]]);
   }
   if(cap)for(let end of [0,rings.length-1])for(let i=0;i<sides;i++)face(m,[rows[end].slice(0,3),rings[end][i],rings[end][(i+1)%sides]],[0,end?1:-1,0]);return m;
 }
 function ellipsoid(name,parent,c,r,tile,glow=false,sides=12,bands=6){
   let rows=[];for(let j=0;j<=bands;j++){let t=-Math.PI/2+j*Math.PI/bands;rows.push([c[0],c[1]+Math.sin(t)*r[1],c[2],Math.max(.025,Math.cos(t)*r[0]),Math.max(.025,Math.cos(t)*r[2])]);}return loft(name,parent,rows,tile,sides,glow);
 }
 function tube(name,parent,points,radii,tile,glow=false,sides=8,depth=1,reference=null){
   let m=mesh(name,parent,tile,glow),rings=points.map((p,i)=>{let d=unit(sub(points[Math.min(i+1,points.length-1)],points[Math.max(i-1,0)]));let u=unit(cross(d,reference||(Math.abs(d[2])<.85?[0,0,1]:[1,0,0]))),v=unit(cross(d,u));return Array.from({length:sides},(_,k)=>add(p,add(mul(u,Math.cos(k*TAU/sides)*radii[i]),mul(v,Math.sin(k*TAU/sides)*radii[i]*depth))));});
   for(let j=0;j<rings.length-1;j++)for(let i=0;i<sides;i++){let k=(i+1)%sides;face(m,[rings[j][i],rings[j+1][i],rings[j+1][k],rings[j][k]],sub(rings[j][i],points[j]));}
   for(let end of [0,rings.length-1])for(let i=0;i<sides;i++)face(m,[points[end],rings[end][i],rings[end][(i+1)%sides]],sub(points[end],points[end?end-1:1]));return m;
 }
 function ellipse(x,y,rx,ry,n=16){return Array.from({length:n},(_,i)=>[x+Math.cos(i*TAU/n)*rx,y+Math.sin(i*TAU/n)*ry]);}
 function block(name,parent,c,r,tile,glow=false){
   let m=mesh(name,parent,tile,glow),[x,y,z]=sub(c,r),[X,Y,Z]=add(c,r);
   for(let [p,n] of [[[[x,y,z],[X,y,z],[X,Y,z],[x,Y,z]],[0,0,-1]],[[[X,y,Z],[x,y,Z],[x,Y,Z],[X,Y,Z]],[0,0,1]],[[[x,y,Z],[x,y,z],[x,Y,z],[x,Y,Z]],[-1,0,0]],[[[X,y,z],[X,y,Z],[X,Y,Z],[X,Y,z]],[1,0,0]],[[[x,y,z],[x,y,Z],[X,y,Z],[X,y,z]],[0,-1,0]],[[[x,Y,Z],[x,Y,z],[X,Y,z],[X,Y,Z]],[0,1,0]]])face(m,p,n);
   return m;
 }
 function squareLoft(name,parent,rows,tile,glow=false,cap=true){
   let m=mesh(name,parent,tile,glow),outline=[[-1,-.7],[-.7,-1],[.7,-1],[1,-.7],[1,.7],[.7,1],[-.7,1],[-1,.7]];
   let rings=rows.map(([x,y,z,rx,rz])=>outline.map(([a,b])=>[x+a*rx,y,z+b*rz]));
   for(let j=0;j<rings.length-1;j++)for(let i=0;i<8;i++){let k=(i+1)%8,p=[rings[j][i],rings[j+1][i],rings[j+1][k],rings[j][k]];face(m,p,sub(mul(p.reduce((s,v)=>add(s,v),[0,0,0]),.25),mul(add(rows[j].slice(0,3),rows[j+1].slice(0,3)),.5)));}
   if(cap)for(let end of [0,rings.length-1])for(let i=0;i<8;i++)face(m,[rows[end].slice(0,3),rings[end][i],rings[end][(i+1)%8]],[0,end?1:-1,0]);
   return m;
 }
 function buildSpirit(kind){
   if(kind==='ice')return;
   const tile={fire:0,electro:2,heal:3}[kind],fire=kind==='fire',heal=kind==='heal';
   group('body',[0,6.75,0]);
   block('voxel_body','body',[0,6.75,0],[4.5,4.5,4.5],tile);
   const Z=-4.515;
   const pixel=(name,x,y,w,h,t=tile,glow=false)=>block(name,'body',[x,y,Z],[w/2,h/2,.016],t,glow);
   for(const s of[-1,1]){
     const x=s*1.82;
     if(fire){
       pixel('ember_eye_edge_'+s,x,8.15,1.95,2.1,4,true);
       pixel('bright_square_eye_'+s,x,8.2,1.38,1.6,14,true).vertices.forEach(v=>v[2]-=.045);
       pixel('coal_brow_'+s,x,9.27,2.3,.48,0).vertices.forEach(v=>v[2]-=.08);
     }else{
       pixel('square_eye_shadow_'+s,x,8.35,2.05,2.05,heal?15:12);
       pixel('flat_bright_eye_'+s,x+s*.08,8.2,1.47,1.52,heal?14:6,true).vertices.forEach(v=>v[2]-=.045);
       pixel('open_eye_corner_'+s,x+s*.85,7.56,.5,.62,tile).vertices.forEach(v=>v[2]-=.075);
     }
   }
   if(!fire){
     const dark=heal?11:12;
     pixel('smile_middle',0,5.05,5.2,1.5,dark);
     pixel('smile_bottom',0,4.2,3.75,.42,dark);
     pixel('smile_corner_l',-2.76,5.56,.36,.6,dark);
     pixel('smile_corner_r',2.76,5.56,.36,.6,dark);
     for(const x of heal?[-1.55]:[-1.8,-.6,.6,1.8])pixel('flat_tooth_'+x,x,5.61,.66,.52,13).vertices.forEach(v=>v[2]-=.042);
     if(!heal)for(const x of[-1.25,0,1.25])pixel('lower_flat_tooth_'+x,x,4.37,.62,.4,13).vertices.forEach(v=>v[2]-=.042);
     if(heal){
       group('tongue',[.7,4.82,-4.59],'body');
       block('flat_pixel_tongue','tongue',[.7,4.0,-4.59],[.91,1.05,.055],7);
       block('stepped_tongue_tip','tongue',[.7,2.95,-4.59],[.61,.22,.055],7);
     }
   }
   for(const s of[-1,1]){
     const arm=group(s<0?'right_arm':'left_arm',[s*4.45,9.0,0],'body');
     block('square_arm_'+s,arm,[s*4.95,6.5,0],[1.05,2.65,1.05],tile);
     const leg=group(s<0?'right_leg':'left_leg',[s*1.65,2.25,0],'body');
     block('square_foot_'+s,leg,[s*1.65,1.15,-.32],[1.05,1.10,1.27],tile);
   }
   group('crown',[0,11.25,0],'body');
   if(fire){
     const sheets=[
       ['front_embers',[[-4.6,1.5,-4.64],[4.6,1.5,-4.64],[4.6,7.1,-4.64],[-4.6,7.1,-4.64]]],
       ['left_wrap',[[-4.64,1.8,4.55],[-4.64,1.8,-4.55],[-4.64,13.7,-4.55],[-4.64,13.7,4.55]]],
       ['right_wrap',[[4.64,2.1,-4.55],[4.64,2.1,4.55],[4.64,13.1,4.55],[4.64,13.1,-4.55]]],
       ['back_wrap',[[4.6,1.9,4.64],[-4.6,1.9,4.64],[-4.6,14.3,4.64],[4.6,14.3,4.64]]],
       ['front_left_flare',[[-5.1,3,-4.7],[-3.2,3,-4.7],[-3.2,12.4,-4.7],[-5.1,12.4,-4.7]]],
       ['front_right_flare',[[3.2,2.7,-4.7],[5.1,2.7,-4.7],[5.1,11.9,-4.7],[3.2,11.9,-4.7]]],
       ['top_flicker',[[-4.6,10.6,.8],[4.6,10.6,.8],[4.6,15.2,.8],[-4.6,15.2,.8]]]
     ];
     for(const [name,q] of sheets){const flame=mesh(name,'body',4,true);flame.flame=true;
       const ids=q.map(v=>{flame.vertices.push(v);return flame.vertices.length-1;}),uv=[[.012,.493],[.488,.493],[.488,.007],[.012,.007]].map(p=>mul(p,A));flame.faces.push(ids,ids.slice().reverse());flame.uvs.push(uv,uv.slice().reverse());}
     for(const s of [-1,1])for(const y of[3.3,5.7,10.6])block('hot_coal_seam_'+s+'_'+y,'body',[s*4.52,y,.7],[.025,.21,1.15],4,true);
   }else if(heal){
     block('golden_curl_base','crown',[.55,11.95,.1],[1.75,.72,1.75],3);
     block('golden_curl_step','crown',[1.15,13.03,.45],[1.10,.6,1.1],3);
     block('golden_curl_tip','crown',[.5,13.85,.65],[.7,.3,.7],3);
   }else{
     squareLoft('violet_tuft_left','crown',[[-2.8,10.85,0,1.42,1.2],[-3.05,12.05,.05,1.14,1.02],[-3.3,13.05,.13,.76,.72],[-3.13,13.65,.18,.32,.4]],2);
     squareLoft('violet_tuft_middle','crown',[[0,10.85,.35,1.56,1.3],[0,12.65,.42,1.19,1.0],[.25,14.02,.55,.76,.65],[.12,14.75,.72,.36,.4]],2);
     squareLoft('violet_tuft_right','crown',[[2.65,10.85,0,1.42,1.2],[2.92,12.0,.08,1.11,.98],[3.25,12.97,.19,.73,.7],[3.27,13.51,.28,.3,.38]],2);
     // Three softened, chamfered tufts; no freestanding lightning ornament.
     // Cheek bolts extend 0.7 px beyond each side of the nine-pixel-wide face.
     for(const s of[-1,1]){
       const arc=group('arc_'+s,[s*2,6.8,-4.63],'body');
       const p=[[.35,6.36],[2.22,6.53],[2.0,6.94],[3.39,6.80],[3.72,7.36],[4.34,7.36],[5.04,8.2],[5.20,7.41],[4.64,6.44],[3.69,6.38],[3.32,5.96],[1.03,5.96]].map(([x,y])=>[s*x,y]);
       const m=mesh('lightning_cheek_moustache_'+s,arc,6,true),z=-4.65,depth=.055;
       const triangles=THREE.ShapeUtils.triangulateShape(p.map(([x,y])=>new THREE.Vector2(x,y)),[]);
       for(const tri of triangles){face(m,tri.map(i=>[...p[i],z-depth]),[0,0,-1]);face(m,tri.map(i=>[...p[i],z+depth]),[0,0,1]);}
       for(let i=0;i<p.length;i++){let a=p[i],b=p[(i+1)%p.length];face(m,[[...a,z-depth],[...b,z-depth],[...b,z+depth],[...a,z+depth]]);}
     }
   }
 }
 function buildStaff(kind){
   group('staff',[0,0,0]);let p='staff',fluid={fire:4,ice:5,electro:6,heal:3}[kind];
   squareLoft('wooden_shaft',p,[[8,-12,8,.6,.6],[8.2,-10,8,.68,.64],[8,0,8,.64,.61],[7.75,9,8.15,.7,.64],[8,18.65,8,.85,.75]],10);
   squareLoft('iron_foot_ferrule',p,[[8,-12.3,8,.6,.6],[8,-12.0,8,.93,.85],[8.2,-10.25,8,.97,.87],[8.2,-9.9,8,.74,.7]],9);
   squareLoft('leather_grip',p,[[8.06,-1.5,8,.77,.75],[7.96,4.5,8.06,.8,.77]],11);
   for(let y of[-1.5,4.5])squareLoft('grip_binding_'+y,p,[[8,y,8,.85,.8],[8,y+.30,8,.85,.8]],15);
   squareLoft('neck_ferrule',p,[[8,16.25,8,1.0,.9],[8,16.55,8,1.15,1.05],[8,18.6,8,1.18,1.1],[8,18.85,8,1.0,.95]],8);
   squareLoft('forged_square_cauldron',p,[[8,18.45,8,1.0,.95],[8,18.9,8,2.3,2.1],[8,20.05,8,3.75,3.4],[8,24.9,8,3.75,3.4],[8,25.7,8,3.85,3.5]],8);
   squareLoft('thick_square_rim',p,[[8,25.5,8,3.85,3.5],[8,25.72,8,4.28,3.85],[8,26.25,8,4.3,3.87],[8,26.55,8,4.02,3.65],[8,26.4,8,3.60,3.22],[8,25.75,8,3.52,3.16]],9,false,false);
   squareLoft('elemental_pool',p,[[8,26.10,8,3.53,3.18],[8,26.3,8,3.53,3.18]],fluid,true);
   squareLoft('lower_iron_binding',p,[[8,20.1,8,3.81,3.46],[8,20.57,8,3.81,3.46]],9,false,false);
   for(let s of [-1,1]){
     let pts=[[-.75,-.75],[-1.1,-.45],[-1.1,.45],[-.75,.75],[.75,.75],[1.1,.45],[1.1,-.45],[.75,-.75],[-.75,-.75]].map(([y,z])=>[8+s*4.13,23.1+y,8+z]);tube('iron_side_loop_'+s,p,pts,pts.map(()=>.25),9,false,4);
     for(let z of[7.4,8.6])block('hinge_rivet_'+s+'_'+z,p,[8+s*3.97,24.35,z],[.22,.3,.25],15);
   }
   let flows=[[[7.15,26.4,4.7],[7.05,25.5,4.30],[7,24.2,4.37],[6.95,22.9,4.22],[7.05,21.4,4.5],[7.12,20.9,4.62]],[[10.3,26.25,5.55],[10.6,25.2,5.07],[10.7,24.1,4.98],[10.62,23.8,5.0]],[[5.6,26.1,6.0],[5.15,25.25,5.5],[5.25,24.2,5.2],[5.05,23.1,5.15],[4.9,22.5,5.2]]];
   flows.forEach((pts,i)=>{let w=i===0?.65:.39;let widths=pts.map((_,j)=>j===pts.length-1?.18:j===pts.length-2?w*1.35:j===1?w*1.2:w*.83);tube('overflow_'+i,p,pts,widths,fluid,true,10,.6);let end=pts[pts.length-2];ellipsoid('rounded_overflow_end_'+i,p,add(end,[0,-.13,-.07]),[w*1.34,w*1.3,w*.5],fluid,true,10,6);});
   ellipsoid('hanging_molten_drop',p,[6.85,19.72,4.45],[.26,.49,.24],fluid,true,8,5);
   let existing=meshes.length;buildSpirit(kind);let mini=meshes.splice(existing).filter(m=>!['right_arm','left_arm','right_leg','left_leg','ice_tail'].includes(m.parent));
   groups=groups.filter(g=>g.name==='staff');
   for(let m of mini){m.name='emerging_'+m.name;m.parent='staff';m.vertices=m.vertices.map(([x,y,z])=>[8+x*.49,24.25+y*.49,7.6+z*.49]);meshes.push(m);}
 }
 function install(kind,staff=false){
   if(kind==='ice'&&!staff)throw new Error('Import MineClash ice with import-mineclash-ice.js instead');
   groups=[];meshes=[];staff?buildStaff(kind):buildSpirit(kind);
   const maps=new Map();for(let g of groups){let bb=new Group({name:g.name,origin:g.origin}).init();if(g.parent)bb.addTo(maps.get(g.parent));maps.set(g.name,bb);}
   for(let m of meshes){let pivot=groups.find(g=>g.name===m.parent).origin;let verts={};m.vertices.forEach((v,i)=>verts['v'+i]=sub(v,pivot));let bb=new Mesh({name:(m.flame?'FLAME_':m.emissive?'EM_':'')+m.name,origin:pivot,vertices:verts});m.faces.forEach((f,i)=>{let keys=f.map(k=>'v'+k),uv={};keys.forEach((k,j)=>uv[k]=m.uvs[i][j]);bb.addFaces(new MeshFace(bb,{vertices:keys,uv,texture:m.flame?flameTex.uuid:tex.uuid}));});bb.addTo(maps.get(m.parent)).init();}
   Project.texture_width=A;Project.texture_height=A;Canvas.updateAll();return {kind,staff,groups:groups.length,meshes:meshes.length,faces:meshes.reduce((n,m)=>n+m.faces.length,0)};
 }
 return {install,setTexture(t,f){tex=t;flameTex=f;},data(){return {groups,meshes};}};
})();
