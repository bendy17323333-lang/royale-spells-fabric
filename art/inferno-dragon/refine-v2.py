from pathlib import Path

root=Path(__file__).resolve().parent
source=Path(r'C:/Users/BliBe/Documents/Codex/2026-09-04/new-chat/outputs/inferno-dragon-v1/build-inferno.js').read_text(encoding='utf-8')
source=source.replace('88bfdb46-3f5c-daad-bbed-090927071ae3','814e9d37-8885-977c-0e19-e6d9cd45344c').replace('Royale - Inferno Dragon prototype 2026-09-07','Royale - Inferno Dragon V2 integration')
source=source.replace("group('wing_left',[5.1,18.7,5.1],'body');group('wing_right',[-5.1,18.7,5.1],'body');", "group('wing_mount_left',[5.15,15.5,1.6],'body');group('wing_left',[5.15,15.5,1.6],'wing_mount_left');group('wing_mount_right',[-5.15,15.5,1.6],'body');group('wing_right',[-5.15,15.5,1.6],'wing_mount_right');")
helper=r'''
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
    const corners=[[s*6.65,24.0,-9.9],[0,26.45,-11.0],[0,31.0,-8.15],[s*5.8,29.3,-7.0]],at=(u,v)=>add(mul(add(mul(corners[0],1-u),mul(corners[1],u)),1-v),mul(add(mul(corners[3],1-u),mul(corners[2],u)),v));
    const us=[0,.37,.59,1],vs=[0,.40,.78,1],normal=[s*.17,.38,-1];
    for(let u=0;u<3;u++)for(let v=0;v<3;v++)if(u!==1||v!==1)solid('sloping_visor_'+s+'_'+u+'_'+v,'head',[at(us[u],vs[v]),at(us[u+1],vs[v]),at(us[u+1],vs[v+1]),at(us[u],vs[v+1])],normal,.52,mat.steel);
    for(let [a,b] of [[at(0,0),at(1,0)],[at(.37,.40),at(.37,.78)],[at(.37,.78),at(.59,.78)],[at(.59,.40),at(.59,.78)],[at(.37,.40),at(.59,.40)]])beam('visor_beveled_rim_'+s+'_'+Math.round(a[0]*100)+'_'+Math.round(a[1]*100),'head',a,b,.20,.23,mat.edge);
    solid('helmet_crown_facet_'+s,'head',[[0,31.0,-8.15],[0,29.5,2.2],[s*6.3,27.8,2.2],[s*5.8,29.3,-7.0]],[s*.18,1,.08],.55,mat.steel);
    solid('helmet_open_cheek_panel_'+s,'head',[[s*6.63,19.75,1.6],[s*6.63,19.75,-6.7],[s*6.65,24,-9.9],[s*5.8,29.3,-7.0],[s*6.3,27.8,2.2]],[s,0,0],.58,mat.steel);
    beam('helmet_rake_'+s,'head',[s*6.65,24,-9.9],[s*5.8,29.3,-7.0],.22,.23,mat.edge);
    for(let z of[-4.9,.6])block('helmet_cheek_rivet_'+s+'_'+z,'head',[s*6.76,25.0,z],[.11,.22,.22],mat.edge);
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
'''
source=source.replace('  function gear(parent,s){',helper+'\n  function gear(parent,s){')
source=source.replace("[0,22.4,-4.1],[6.35,4.6,5.8]", "[0,21.6,-4.1],[5.9,3.8,5.8]")
start=source.index("      front('eye_shadow_")
end=source.index("      block('green_cheek_",start)
source=source[:start]+source[end:]
source=source.replace("    block('open_mouth_interior'", "    eyeBand();\n    block('open_mouth_interior'",1)
start=source.index("    chamferBox('helmet_top'")
end=source.index("    for(let s of[-1,1]){\n      const arm=",start)
source=source[:start]+r'''
    for(let s of[-1,1]){
      visor(s);gear('head',s);
      solid('deep_jaw_side_'+s,'jaw',[[s*5.50,17.7,-7.4],[s*5.50,16.0,-12.9],[s*5.1,16.2,-14.05],[s*5.5,11.8,-13.3],[s*5.50,12.0,-7.5]],[s,0,0],.72,mat.steel);
      const teeth=[[0,14.4,-15.15],[s*.9,13.65,-15.06],[s*2.6,16.0,-14.76],[s*3.65,14.40,-14.39],[s*5.1,16.2,-14.05],[s*5.5,11.8,-13.3],[s*3.2,10.6,-14.5],[0,10.20,-15.18]];
      solid('thick_castellated_chin_'+s,'jaw',teeth,[s*.2,-.08,-1],.90,mat.steel);
      for(let i=0;i<4;i++)beam('chin_tooth_bevel_'+s+'_'+i,'jaw',teeth[i],teeth[i+1],.21,.23,mat.edge);
      front('chin_plate_rivet_'+s,'jaw',s*3.5,12.30,-14.85,.42,.42,mat.iron);
    }
    solid('chin_heavy_base','jaw',[[-5.5,11.8,-13.3],[0,10.2,-15.18],[5.5,11.8,-13.3],[4.9,11.9,-7.5],[-4.9,11.9,-7.5]],[0,-1,0],.72,mat.iron);
''' +source[end:]
source=source.replace("[1.22,1.07,.84],mat.skin)","[1.22,1.07,.84],mat.copper)")
start=source.index('      const points=[[s*2.8,27.36,8.1]')
end=source.index('      for(let y of[16.7',start)
source=source[:start]+source[end:]
start=source.index("    for(let s of[-1,1]){\n      const g=s===1?'wing_left'")
end=source.index('\n  }\n  function install',start)
source=source[:start]+r'''
    const endHead=[3.9,29.3,-1.5],offset=sub(endHead,[0,20,-2]),endBody=add([0,20,-2],new THREE.Vector3(...offset).applyAxisAngle(new THREE.Vector3(1,0,0),22*Math.PI/180).toArray());
    const pipe=[[0,27.45,8.38],[.25,29.9,8.2],[1.9,31.45,6.3],[3.8,30.85,3.7],endBody];
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
''' +source[end:]
source=source.replace('const rest={body:[-35,0,0],head:[22,0,0],','const rest={body:[-35,0,0],head:[22,0,0],wing_mount_left:[35,0,0],wing_mount_right:[35,0,0],')
(root/'build-model.js').write_text(source,encoding='utf-8')
print('V2: raised sloping visor, thick castellated chin, one hose, body-rooted two-sided wings, gentle continuous brow.')
