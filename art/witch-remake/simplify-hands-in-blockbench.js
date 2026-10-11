(()=>{
 for(const id of ['42191b01-5388-7b75-297c-d50828dc708a','a1b270e5-936f-175e-a4be-c799f973a76b']){
  const p=ModelProject.all.find(p=>p.uuid===id);if(!p||!p.select())throw Error('Witch missing');
  Undo.initEdit({elements:Outliner.elements.slice(),outliner:true});
  Outliner.elements.filter(m=>m.name.startsWith('Block finger')||m.name.startsWith('Thumb')).forEach(m=>m.remove());
  for(const m of Outliner.elements.filter(m=>m.name.startsWith('Hand palm'))){
   for(const v of Object.values(m.vertices))if(v[2]<0)v[2]=-1.8;
   m.name=m.name.replace('Hand palm','Block hand');
  }
  Canvas.updateAll();Undo.finishEdit('Use simple Minecraft block hands without separate fingers',{elements:Outliner.elements.slice(),outliner:true});
 }
 return{ok:true};
})()
