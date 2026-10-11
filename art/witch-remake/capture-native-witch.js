(async()=>{
 const root='C:/Users/BliBe/Documents/Codex/2026-09-04/new-chat/work/royale-spells-fabric-1.20.1-standalone/';
 const ids={witch:'42191b01-5388-7b75-297c-d50828dc708a',witch_evolution:'a1b270e5-936f-175e-a4be-c799f973a76b'};
 const view=Preview.all.find(v=>v.id==='mcp_offscreen_royale_witch_191');if(!view)throw Error('Missing private viewport');
 const base={right_arm:[26,0,-12],right_forearm:[38,0,0],staff:[-64,0,10],left_arm:[8,0,18],left_forearm:[42,0,0]};
 const views={front:[0,21,-76],side:[-76,21,0],back:[0,21,76],'three-quarter':[43,33,-64]};
 const receipt=[];
 for(const [kind,id] of Object.entries(ids)){
  const p=ModelProject.all.find(p=>p.uuid===id);if(!p||!p.select())throw Error('Missing '+kind);
  const gs=Object.fromEntries(Group.all.map(g=>[g.name,g]));
  Undo.initEdit({elements:Outliner.elements.slice(),animations:Animation.all.slice()});
  for(const m of Outliner.elements)if(m.name.startsWith('Eye light inner facet'))m.name='EM_'+m.name;
  if(!Animation.all.length){
   const animators={};
   for(const [name,angles] of Object.entries(base))animators[gs[name].uuid]={name,type:'bone',keyframes:[0,1,2].map(t=>({channel:'rotation',time:t,interpolation:'catmullrom',data_points:[{x:String(angles[0]+(name==='left_forearm'&&t===1?2:0)),y:String(angles[1]),z:String(angles[2]-(name==='staff'?10:0))}]}))};
   new Animation({name:'animation.'+kind+'.idle_reference',loop:'loop',length:2,animators}).add();
  }
  Undo.finishEdit('Store articulated idle reference and modest eye glow',{elements:Outliner.elements.slice(),animations:Animation.all.slice()});
  Blockbench.writeFile(root+'art/blockbench/'+kind+'.bbmodel',{content:Codecs.project.compile()});
  const old=Group.all.map(g=>[g,g.rotation.slice()]);
  try{
   for(const [name,angles]of Object.entries(base))gs[name].rotation=angles.slice();
   Canvas.updateAll();
   for(const [angle,pos] of Object.entries(views)){
    view.loadAnglePreset({position:pos,target:[0,16,0],projection:'perspective',fov:35});view.render();
    const path=root+'art/witch-remake/blockbench-final-'+kind+'-'+angle+'.png';
    await new Promise(r=>view.screenshot({width:1000,height:1000},data=>{Blockbench.writeFile(path,{savetype:'image',content:data});r();}));
    receipt.push({kind,angle,path});
   }
  }finally{for(const[g,r]of old)g.rotation=r;Canvas.updateAll();}
 }
 return{captures:receipt};
})()
