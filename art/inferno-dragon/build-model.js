globalThis.InfernoPrototype = (() => {
  const projectId = '814e9d37-8885-977c-0e19-e6d9cd45344c';
  const add=(a,b)=>a.map((v,i)=>v+b[i]);
  const sub=(a,b)=>a.map((v,i)=>v-b[i]);
  const mul=(a,k)=>a.map(v=>v*k);
  const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
  const dot=(a,b)=>a.reduce((s,v,i)=>s+v*b[i],0);
  const unit=a=>mul(a,1/Math.max(1e-9,Math.hypot(...a)));
  const groups=[],meshes=[];
  let atlas=null;
  const mat={skin:0,shade:1,belly:2,iron:3,steel:4,edge:5,copper:6,wood:7,brass:8,ember:9,dark:10,tooth:11,core:12,wing:13,claw:14,eye:15};
  function group(name,origin,parent=''){groups.push({name,origin,parent});return name;}
  function mesh(name,parent,tile,emissive=false){const m={name,parent,tile,emissive,vertices:[],faces:[],uvs:[]};meshes.push(m);return m;}
  function face(m,p,normal,uv){
    if(dot(cross(sub(p[1],p[0]),sub(p[2],p[0])),normal)<0){p=p.slice().reverse();if(uv)uv=uv.slice().reverse();}
    const n=unit(normal),axis=n.map(Math.abs).indexOf(Math.max(...n.map(Math.abs))),axes=axis===0?[2,1]:axis===1?[0,2]:[0,1];
    if(!uv){const lo=axes.map(i=>Math.min(...p.map(v=>v[i])));uv=p.map(v=>[.12+(v[axes[0]]-lo[0])/22,.87-(v[axes[1]]-lo[1])/22]);}
    m.faces.push(p.map(v=>{m.vertices.push(v);return m.vertices.length-1;}));m.uvs.push(uv.map(p=>p.map(v=>Math.max(.05,Math.min(.95,v)))));
  }
  function block(name,parent,c,r,tile,rotation=null,glow=false){
    const m=mesh(name,parent,tile,glow),[x,y,z]=sub(c,r),[X,Y,Z]=add(c,r);
    const q=(p)=>{if(!rotation)return p;const e=new THREE.Euler(...rotation.map(v=>v*Math.PI/180),'ZYX');return new THREE.Vector3(...sub(p,c)).applyEuler(e).add(new THREE.Vector3(...c)).toArray();};
    const qn=n=>rotation?new THREE.Vector3(...n).applyEuler(new THREE.Euler(...rotation.map(v=>v*Math.PI/180),'ZYX')).toArray():n;
    for(const [p,n] of [[[[x,y,z],[X,y,z],[X,Y,z],[x,Y,z]],[0,0,-1]],[[[X,y,Z],[x,y,Z],[x,Y,Z],[X,Y,Z]],[0,0,1]],[[[x,y,Z],[x,y,z],[x,Y,z],[x,Y,Z]],[-1,0,0]],[[[X,y,z],[X,y,Z],[X,Y,Z],[X,Y,z]],[1,0,0]],[[[x,y,z],[x,y,Z],[X,y,Z],[X,y,z]],[0,-1,0]],[[[x,Y,Z],[x,Y,z],[X,Y,z],[X,Y,Z]],[0,1,0]]])face(m,p.map(q),qn(n));
    return m;
  }
  function beam(name,parent,a,b,w,d,tile){
    const c=mul(add(a,b),.5),v=unit(sub(b,a)),u=unit(cross(v,Math.abs(v[2])>.9?[0,1,0]:[0,0,1])),z=unit(cross(u,v));
    const m=mesh(name,parent,tile),point=(p,s,t)=>add(p,add(mul(u,s*w/2),mul(z,t*d/2)));
    const low=[point(a,-1,-1),point(a,1,-1),point(a,1,1),point(a,-1,1)],high=[point(b,-1,-1),point(b,1,-1),point(b,1,1),point(b,-1,1)];
    for(let i=0;i<4;i++){let j=(i+1)%4,p=[low[i],high[i],high[j],low[j]];face(m,p,sub(mul(p.reduce(add,[0,0,0]),.25),c));}
    face(m,low,mul(v,-1));face(m,high,v);return m;
  }
  function chamferBox(name,parent,c,r,tile,cut=.7){
    const m=mesh(name,parent,tile),[x,y,z]=c,[rx,ry,rz]=r;
    const outline=[[-rx,-rz+cut],[-rx+cut,-rz],[rx-cut,-rz],[rx,-rz+cut],[rx,rz-cut],[rx-cut,rz],[-rx+cut,rz],[-rx,rz-cut]];
    const rings=[y-ry,y+ry].map(Y=>outline.map(([X,Z])=>[x+X,Y,z+Z]));
    for(let i=0;i<8;i++){let j=(i+1)%8,p=[rings[0][i],rings[1][i],rings[1][j],rings[0][j]];face(m,p,sub(mul(p.reduce(add,[0,0,0]),.25),c));}
    for(let end=0;end<2;end++)for(let i=1;i<7;i++)face(m,[rings[end][0],rings[end][i],rings[end][i+1]],[0,end?1:-1,0]);return m;
  }
  function front(name,parent,x,y,z,w,h,tile,glow=false){return block(name,parent,[x,y,z],[w/2,h/2,.018],tile,null,glow);}
  function bodyHull(){
    const rows=[[6.7,3.5,3.15,3.40,1.0],[8.3,3.3,4.60,4.80,1.1],[11.6,2.3,5.65,6.0,1.3],[15.2,.9,5.45,5.70,1.0],[17.6,-.3,4.20,4.60,.7],[18.7,-1.5,3.30,3.30,.6]];
    const rings=rows.map(([y,z,xr,zr,c])=>[[-xr,-zr+c],[-xr+c,-zr],[xr-c,-zr],[xr,-zr+c],[xr,zr-c],[xr-c,zr],[-xr+c,zr],[-xr,zr-c]].map(([x,dz])=>[x,y,z+dz]));
    const m=mesh('faceted_chest_abdomen_and_haunches','body',mat.skin);
    for(let row=0;row<rings.length-1;row++)for(let i=0;i<8;i++){
      const j=(i+1)%8,p=[rings[row][i],rings[row+1][i],rings[row+1][j],rings[row][j]],c=[0,(rows[row][0]+rows[row+1][0])/2,(rows[row][1]+rows[row+1][1])/2];
      face(m,p,sub(mul(p.reduce(add,[0,0,0]),.25),c));
    }
    for(let end of[0,rings.length-1])for(let i=1;i<7;i++)face(m,[rings[end][0],rings[end][i],rings[end][i+1]],[0,end?1:-1,0]);
    const b=mesh('segmented_belly_scutes','body',mat.belly);
    for(let j=0;j<rows.length-2;j++){
      const a=rows[j],c=rows[j+1],w=a[2]*.55,W=c[2]*.57;
      face(b,[[-w,a[0]+.12,a[1]-a[3]-.07],[w,a[0]+.12,a[1]-a[3]-.07],[W,c[0]-.12,c[1]-c[3]-.08],[-W,c[0]-.12,c[1]-c[3]-.08]],[0,-.2,-1]);
    }
  }
  function panel(name,parent,points,depth,tile){
    const m=mesh(name,parent,tile),indices=THREE.ShapeUtils.triangulateShape(points.map(p=>new THREE.Vector2(p[0],p[1])),[]);
    const frontPts=points.map(p=>add(p,[0,0,-depth/2])),backPts=points.map(p=>add(p,[0,0,depth/2]));
    for(let f of indices){face(m,f.map(i=>frontPts[i]),[0,0,-1]);face(m,f.map(i=>backPts[i]),[0,0,1]);}
    for(let i=0;i<points.length;i++){let j=(i+1)%points.length,p=[frontPts[i],backPts[i],backPts[j],frontPts[j]];face(m,p,[points[j][1]-points[i][1],points[i][0]-points[j][0],0]);}return m;
  }

  function solid(name,parent,p,n,depth,tile,backTile=tile){
    n=unit(n);const ax=n.map(Math.abs).indexOf(Math.max(...n.map(Math.abs))),axes=ax===0?[1,2]:ax===1?[0,2]:[0,1];
    const ids=THREE.ShapeUtils.triangulateShape(p.map(v=>new THREE.Vector2(v[axes[0]],v[axes[1]])),[]),back=p.map(v=>sub(v,mul(n,depth)));
    const m=mesh(name,parent,tile),bm=backTile===tile?m:mesh(name+'_red_underside',parent,backTile);
    const low=axes.map(a=>Math.min(...p.map(v=>v[a]))),uv=p.map(v=>[.12+(v[axes[0]]-low[0])/22,.87-(v[axes[1]]-low[1])/22]);
    for(let f of ids){face(m,f.map(i=>p[i]),n,f.map(i=>uv[i]));face(bm,f.map(i=>back[i]),mul(n,-1),f.map(i=>uv[i]));}
    for(let i=0;i<p.length;i++){let j=(i+1)%p.length;face(m,[p[i],back[i],back[j],p[j]],cross(sub(p[j],p[i]),n));}
    return m;
  }
  function visor(s){
    // The raised visor is a separate thin plate. Its upper rim must never
    // determine the height or bulk of the shell sitting against the scalp.
    const corners=[[s*6.4,24.0,-10.0],[0,26.25,-11.0],[0,29.15,-8.15],[s*5.65,27.65,-7.0]],at=(u,v)=>add(mul(add(mul(corners[0],1-u),mul(corners[1],u)),1-v),mul(add(mul(corners[3],1-u),mul(corners[2],u)),v));
    const us=[0,.37,.59,1],vs=[0,.40,.78,1],normal=[s*.17,.38,-1];
    for(let u=0;u<3;u++)for(let v=0;v<3;v++)if(u!==1||v!==1)solid('sloping_visor_'+s+'_'+u+'_'+v,'head',[at(us[u],vs[v]),at(us[u+1],vs[v]),at(us[u+1],vs[v+1]),at(us[u],vs[v+1])],normal,.30,mat.steel);
    for(let [a,b] of [[at(0,0),at(1,0)],[at(.37,.40),at(.37,.78)],[at(.37,.78),at(.59,.78)],[at(.59,.40),at(.59,.78)],[at(.37,.40),at(.59,.40)]])beam('visor_beveled_rim_'+s+'_'+Math.round(a[0]*100)+'_'+Math.round(a[1]*100),'head',a,b,.20,.23,mat.edge);
    solid('helmet_crown_facet_'+s,'head',[[0,26.0,-8.15],[0,26.0,1.94],[s*6.12,25.65,1.94],[s*6.05,25.65,-7.0]],[s*.06,1,0],.26,mat.steel);
    solid('helmet_open_cheek_panel_'+s,'head',[[s*6.12,19.75,1.75],[s*6.12,19.75,-6.7],[s*6.20,24,-9.9],[s*6.05,25.65,-7.0],[s*6.12,25.65,1.94]],[s,0,0],.26,mat.steel);
    solid('helmet_back_panel_'+s,'head',[[0,19.75,1.94],[s*6.12,19.75,1.94],[s*6.12,25.65,1.94],[0,26.0,1.94]],[0,0,1],.22,mat.iron);
    beam('visor_hinge_arm_'+s,'head',[s*6.48,19.15,-7.35],corners[0],.26,.30,mat.iron);
    beam('helmet_rake_'+s,'head',corners[0],corners[3],.18,.20,mat.edge);
    for(let z of[-4.9,.6])block('helmet_cheek_rivet_'+s+'_'+z,'head',[s*6.20,24.85,z],[.08,.18,.18],mat.edge);
  }
  function eyeBand(){
    const xs=[-5.95,-3.4,0,3.4,5.95],zs=[-9.90,-10.40,-10.66,-10.40,-9.90],top=x=>25.97-Math.abs(x)*.32;
    for(let i=0;i<4;i++)solid('continuous_brow_volume_'+i,'head',[[xs[i],21.90,zs[i]],[xs[i+1],21.90,zs[i+1]],[xs[i+1],top(xs[i+1]),zs[i+1]],[xs[i],top(xs[i]),zs[i]]],[0,0,-1],.75,mat.skin);
    const zAt=x=>-10.4+(Math.abs(x)-3.4)*.175;
    const patch=(name,s,x,y,w,h,t,layer)=>{const m=mesh(name+s,'head',t),p=[[x-w/2,y-h/2],[x+w/2,y-h/2],[x+w/2,y+h/2],[x-w/2,y+h/2]].map(([X,Y])=>[X,Y,zAt(X)-layer]);face(m,p,[s*.17,0,-1]);};
    for(let s of[-1,1]){
      patch('soft_eye_socket_',s,s*4.02,23.48,3.12,3.18,mat.shade,.025);
      patch('warm_eye_white_',s,s*4.02,23.50,2.88,2.95,mat.tooth,.055);
      patch('large_gold_iris_',s,s*3.89,23.43,2.20,2.43,mat.eye,.085);
      patch('friendly_pupil_',s,s*3.83,23.41,1.21,1.78,mat.dark,.115);
      patch('pupil_spark_',s,s*3.83-.25,23.99,.51,.53,mat.tooth,.145);

    }
  }

  function gear(parent,s){
    const x=s*6.48,y=19.15,z=-7.35;
    block('cheek_gear_hub_'+s,parent,[x,y,z],[.30,1.38,1.38],mat.steel);
    for(const [dy,dz] of [[0,-1.65],[0,1.65],[-1.65,0],[1.65,0]])block('cheek_gear_tooth_'+s+'_'+dy+'_'+dz,parent,[x,y+dy,z+dz],[.37,.52,.52],mat.edge);
    block('cheek_axle_'+s,parent,[x+s*.4,y,z],[.14,.65,.65],mat.iron);
    block('cheek_pin_'+s,parent,[x+s*.55,y,z],[.07,.26,.26],mat.edge);
  }
  function build(){
    group('hover',[0,13,2]);group('body',[0,13,2],'hover');group('head',[0,20,-2],'body');group('jaw',[0,19.15,-7.35],'head');
    group('wing_mount_left',[5.15,15.5,1.6],'body');group('wing_left',[5.15,15.5,1.6],'wing_mount_left');group('wing_mount_right',[-5.15,15.5,1.6],'body');group('wing_right',[-5.15,15.5,1.6],'wing_mount_right');
    group('arm_left',[5.0,15,-4.2],'body');group('arm_right',[-5.0,15,-4.2],'body');
    group('leg_left',[3.4,8,3.5],'body');group('leg_right',[-3.4,8,3.5],'body');
    group('tail_1',[0,10.7,7.0],'body');group('tail_2',[0,11.6,13.0],'tail_1');group('tank',[0,20,8],'body');
    bodyHull();
    chamferBox('broad_baby_dragon_head','head',[0,21.6,-4.1],[5.9,3.8,5.8],mat.skin,.55);
    chamferBox('broad_square_muzzle','head',[0,19.8,-10.6],[5.45,2.03,3.80],mat.skin,.48);
    front('muzzle_lower_lip','head',0,18.12,-14.43,9.75,.48,mat.shade);
    for(let s of[-1,1]){
      front('nostril_'+s,'head',s*2.85,20.2,-14.44,1.12,1.03,mat.dark);
      front('nostril_corner_'+s,'head',s*2.85+s*.43,20.66,-14.445,.35,.27,mat.shade);
      block('green_cheek_'+s,'head',[s*5.7,18.4,-8.9],[.66,1.58,1.7],mat.skin);
    }
    eyeBand();
    block('open_mouth_interior','head',[0,16.65,-8.9],[4.9,1.2,.13],mat.dark);
    front('inner_ember_glow','head',0,16.7,-9.12,3.0,1.55,mat.ember,true);
    front('inner_ember_core','head',0,16.55,-9.18,1.30,.73,mat.core,true);
    chamferBox('lower_jaw','jaw',[0,14.93,-10.13],[4.90,.92,3.34],mat.skin,.42);
    block('lower_mouth_floor','jaw',[0,15.88,-10.10],[4.43,.055,2.7],mat.copper);
    for(let [i,x] of[-3.6,3.6].entries())block('upper_square_tooth_'+i,'head',[x,17.34,-12.93],[.52,.67,.61],mat.tooth);
    for(let [i,x] of[-3.35,0,3.35].entries())block('lower_square_tooth_'+i,'jaw',[x,16.15,-12.39],[.45,.52,.55],mat.tooth);

    for(let s of[-1,1]){
      visor(s);gear('head',s);
      solid('deep_jaw_side_'+s,'jaw',[[s*5.50,17.7,-7.4],[s*5.50,16.0,-12.9],[s*5.1,16.2,-14.05],[s*5.5,11.8,-13.3],[s*5.50,12.0,-7.5]],[s,0,0],.72,mat.steel);
      const teeth=[[0,14.4,-15.15],[s*.9,13.65,-15.06],[s*2.6,16.0,-14.76],[s*3.65,14.40,-14.39],[s*5.1,16.2,-14.05],[s*5.5,11.8,-13.3],[s*3.2,10.6,-14.5],[0,10.20,-15.18]];
      solid('thick_castellated_chin_'+s,'jaw',teeth,[s*.2,-.08,-1],.90,mat.steel);
      for(let i=0;i<4;i++)beam('chin_tooth_bevel_'+s+'_'+i,'jaw',teeth[i],teeth[i+1],.21,.23,mat.edge);
      front('chin_plate_rivet_'+s,'jaw',s*3.5,12.30,-14.85,.42,.42,mat.iron);
    }
    solid('chin_heavy_base','jaw',[[-5.5,11.8,-13.3],[0,10.2,-15.18],[5.5,11.8,-13.3],[4.9,11.9,-7.5],[-4.9,11.9,-7.5]],[0,-1,0],.72,mat.iron);
    for(let s of[-1,1]){
      const arm=s===1?'arm_left':'arm_right',leg=s===1?'leg_left':'leg_right';
      block('short_upper_arm_'+s,arm,[s*6.06,14.45,-4.9],[1.18,1.77,1.55],mat.skin,[0,0,s*24]);
      block('forearm_'+s,arm,[s*6.9,13.02,-6.4],[1.27,1.30,1.48],mat.skin,[12,0,-s*9]);
      block('small_hand_'+s,arm,[s*7.1,13.1,-7.46],[1.22,1.07,.84],mat.copper);
      for(let k=-1;k<=1;k++)block('finger_claw_'+s+'_'+k,arm,[s*7.1+k*.64,12.87,-8.30],[.21,.36,.46],mat.claw,[12,0,0]);
      block('folded_hind_thigh_'+s,leg,[s*3.85,6.40,4.32],[1.9,2.5,2.00],mat.skin,[-24,0,s*6]);
      block('hind_foot_'+s,leg,[s*3.96,4.70,2.63],[1.88,.90,2.32],mat.skin,[9,0,s*5]);
      for(let k=-1;k<=1;k++)block('hind_toe_'+s+'_'+k,leg,[s*3.96+k*1.05,4.62,.26],[.35,.43,.52],mat.claw,[9,0,s*5]);
    }
    beam('tail_base','tail_1',[0,10.5,7.0],[0,10.7,13.1],4.0,3.8,mat.skin);
    beam('tail_tip','tail_2',[0,10.7,12.2],[0,13.25,18.7],2.6,2.4,mat.skin);
    beam('tail_last','tail_2',[0,13.2,18.1],[0,15.0,21.0],1.20,1.15,mat.skin);
    for(const [i,y,z,w] of[[0,12.50,10.3,1.5],[1,13.00,13.5,1.1],[2,14.62,17.4,.8]])block('tail_dorsal_spike_'+i,i===0?'tail_1':'tail_2',[0,y,z],[w/2,.65,.62],mat.claw,[-22,0,0]);
    chamferBox('barrel_wooden_reservoir','tank',[0,21.4,8.38],[4.45,5.55,3.68],mat.wood,.82);
    chamferBox('barrel_upper_hoop','tank',[0,26.29,8.38],[4.72,.55,3.93],mat.steel,.88);
    chamferBox('barrel_lower_hoop','tank',[0,16.65,8.38],[4.72,.66,3.93],mat.steel,.88);
    chamferBox('barrel_middle_hoop','tank',[0,20.65,8.38],[4.65,.49,3.88],mat.steel,.86);
    block('barrel_lid','tank',[0,27.04,8.38],[3.45,.22,2.8],mat.wood);
    for(let x of[-2.72,-.91,.91,2.72])block('barrel_back_plank_seam_'+x,'tank',[x,22.4,12.084],[.075,3.25,.024],mat.copper);
    for(let s of[-1,1]){
      block('tank_flame_grille_'+s,'tank',[s*3.89,23.32,10.75],[.51,1.10,.72],mat.iron);
      block('tank_glowing_slit_'+s,'tank',[s*4.413,23.32,10.75],[.025,.76,.36],mat.ember,null,true);
      block('tank_mount_'+s,'tank',[s*4.78,18.6,5.78],[.55,2.0,.8],mat.copper);
      for(let y of[16.7,20.65,26.29])block('hoop_rivet_'+s+'_'+y,'tank',[s*4.74,y,8.2],[.13,.23,.23],mat.edge);
    }

    const endHead=[3.9,26.05,-1.5],offset=sub(endHead,[0,20,-2]),endBody=add([0,20,-2],new THREE.Vector3(...offset).applyAxisAngle(new THREE.Vector3(1,0,0),22*Math.PI/180).toArray());
    const pipe=[[0,27.45,8.38],[.25,29.9,8.2],[1.9,30.85,6.3],[3.8,29.85,3.7],endBody];
    for(let i=0;i<pipe.length-1;i++)beam('single_brass_fuel_hose_'+i,'tank',pipe[i],pipe[i+1],1.43,1.43,mat.brass);
    block('single_tank_center_socket','tank',[0,27.30,8.38],[1.14,.37,1.14],mat.iron);
    block('right_helmet_hose_socket','head',endHead,[1.03,.38,.86],mat.iron,[0,0,-12]);
    for(let s of[-1,1]){
      const g=s===1?'wing_left':'wing_right',p=[[5.15,15.5,1.6],[9.0,15.6,.7],[17.0,15.5,3.4],[18.5,15.4,5.7],[15.8,15.45,5.1],[14.6,15.45,9.1],[11.6,15.5,7.0],[8.9,15.55,10.2],[6.65,15.5,6.3]].map(([x,y,z])=>[s*x,y,z]);
      solid('wing_membrane_'+s,g,p,[0,1,0],.20,mat.wing,mat.copper);
      beam('wing_body_socket_'+s,'body',[s*4.55,15.20,1.6],[s*5.15,15.5,1.6],1.8,1.8,mat.skin);
      beam('wing_upper_arm_'+s,g,p[0],p[1],1.02,.93,mat.skin);
      beam('wing_fore_edge_'+s,g,p[1],p[2],.82,.76,mat.skin);
      beam('wing_tip_'+s,g,p[2],p[3],.58,.62,mat.skin);
      beam('wing_middle_rib_'+s,g,p[1],p[5],.55,.52,mat.shade);
      beam('wing_inner_rib_'+s,g,p[0],p[7],.55,.52,mat.skin);
      block('wing_thumb_'+s,g,[s*9.0,15.65,.13],[.47,.47,.75],mat.claw,[0,s*12,0]);
    }

  }
  function install(texture,replace=false){
    const project=ModelProject.all.find(p=>p.uuid===projectId);if(!project)throw Error('Prototype project missing');project.select();
    if(Project.elements.length){if(!replace||Project.name!=='Royale - Inferno Dragon V2 integration')throw Error('Prototype is not empty; refusing to touch existing model');Outliner.root.slice().forEach(n=>n.remove());}
    atlas=texture;atlas.uv_width=atlas.width;atlas.uv_height=atlas.height;build();const map=new Map();
    for(let g of groups){const bb=new Group({name:g.name,origin:g.origin}).init();if(g.parent)bb.addTo(map.get(g.parent));map.set(g.name,bb);}
    for(let m of meshes){const pivot=groups.find(g=>g.name===m.parent).origin,vertices={};m.vertices.forEach((v,i)=>vertices['v'+i]=sub(v,pivot));const bb=new Mesh({name:(m.emissive?'EM_':'')+m.name,origin:pivot,vertices});
      m.faces.forEach((f,i)=>{const keys=f.map(k=>'v'+k),uv={};keys.forEach((k,j)=>uv[k]=[(m.tile%4+m.uvs[i][j][0])*atlas.width/4,(Math.floor(m.tile/4)+m.uvs[i][j][1])*atlas.height/4]);bb.addFaces(new MeshFace(bb,{vertices:keys,uv,texture:atlas.uuid}));});bb.addTo(map.get(m.parent)).init();
    }
    const rest={body:[-35,0,0],head:[22,0,0],wing_mount_left:[35,0,0],wing_mount_right:[35,0,0],leg_left:[18,0,0],leg_right:[18,0,0],arm_left:[12,0,0],arm_right:[12,0,0]};
    for(const [name,rotation] of Object.entries(rest)){map.get(name).rotation.splice(0,map.get(name).rotation.length,...rotation);groups.find(g=>g.name===name).rotation=rotation;}
    Project.texture_width=atlas.width;Project.texture_height=atlas.height;Canvas.updateAll();unselectAll();
    return {project:Project.uuid,groups:groups.length,meshes:meshes.length,faces:meshes.reduce((n,m)=>n+m.faces.length,0),width:atlas.width,height:atlas.height};
  }
  return {install,data:()=>({groups,meshes}),geometry:()=>{groups.length=0;meshes.length=0;build();return {groups,meshes};},projectId};
})();
