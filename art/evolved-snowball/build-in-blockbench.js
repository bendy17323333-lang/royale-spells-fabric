(() => {
const p=ModelProject.all.find(p=>p.uuid==='0e326031-21f3-aaa2-1c2a-55d25a50a8d3');
if(!p||!p.select())throw new Error('Snowball project missing');

const elements=[],textures=[];
Undo.initEdit({elements:Outliner.elements.slice(),outliner:true,textures:Texture.all.slice()});
Outliner.elements.slice().forEach(e=>e.remove());Group.all.slice().forEach(g=>g.remove());Texture.all.slice().forEach(t=>t.remove());
Project.texture_width=Project.texture_height=16;
const colors={snow:'#e7f3f6',snow_soft:'#dcecf1',snow_shadow:'#cbdfe9',gem_dark:'#482164',gem_mid:'#6a298e',gem_light:'#9353bd',gem_edge:'#b484d6',gem_top:'#8043a7'};
for(const [name,color] of Object.entries(colors)){
const c=document.createElement('canvas');c.width=c.height=16;const ctx=c.getContext('2d');ctx.fillStyle=color;ctx.fillRect(0,0,16,16);
const t=new Texture({name:name+'.png',width:16,height:16,uv_width:16,uv_height:16});
t.fromDataURL(c.toDataURL()).add(false);textures.push(t);
}
const tex=name=>textures.find(t=>t.name===name+'.png').uuid;
const group=new Group({name:'snowball',origin:[0,0,0]}).addTo('root').init();
const norm=v=>{const l=Math.hypot(...v);return v.map(x=>x/l)};
const dot=(a,b)=>a.reduce((s,x,i)=>s+x*b[i],0);
const add=(a,b)=>a.map((x,i)=>x+b[i]);
const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
function make(name,vertices,polys,materials,uvs){
const m=new Mesh({name,vertices,faces:{},origin:[0,0,0]});
for(let k=0;k<polys.length;k++){const keys=polys[k];const uv={};keys.forEach((v,i)=>uv[v]=uvs?uvs[k][i]:[[1,1],[15,1],[15,15],[1,15]][i]);
m.addFaces(new MeshFace(m,{vertices:keys,texture:tex(Array.isArray(materials)?materials[k]:materials),uv}));}
m.addTo(group).init();elements.push(m);return m;
}
function block(name,center,half,bevel){
const vertices={},polys=[],index=new Map();
function f(points){const a=points[0],b=points[1],c=points[2],n=cross(b.map((x,i)=>x-a[i]),c.map((x,i)=>x-a[i]));
if(dot(n,a)<0)points.reverse();
const ids=points.map(pt=>{const world=add(pt,center),key=world.map(x=>x.toFixed(5)).join(',');if(!index.has(key)){const id='v'+index.size;index.set(key,id);vertices[id]=world;}return index.get(key);});
polys.push(ids);}
for(let axis=0;axis<3;axis++)for(const sign of [-1,1]){
const a=(axis+1)%3,b=(axis+2)%3;
f([[-1,-1],[1,-1],[1,1],[-1,1]].map(pair=>{const p=[0,0,0];p[axis]=sign*half[axis];p[a]=pair[0]*(half[a]-bevel);p[b]=pair[1]*(half[b]-bevel);return p;}));}
for(let a=0;a<3;a++)for(let b=a+1;b<3;b++)for(const s of [-1,1])for(const t of [-1,1]){
const c=3-a-b;
const point=(edge,sign)=>{const p=[0,0,0];p[a]=s*(half[a]-(edge?bevel:0));p[b]=t*(half[b]-(edge?0:bevel));p[c]=sign*(half[c]-bevel);return p;};
f([point(0,-1),point(1,-1),point(1,1),point(0,1)]);}
for(const x of [-1,1])for(const y of [-1,1])for(const z of [-1,1]){
const s=[x,y,z];f([0,1,2].map(axis=>half.map((v,i)=>s[i]*(v-(i===axis?0:bevel)))));}
make(name,vertices,polys,'snow');
}
block('SNOW_block_body',[0,0,0],[7.7,7.5,7.7],.42);
block('SNOW_top_drift_left',[-2.3,7.50,-2.0],[4.6,.38,4.9],.18);
block('SNOW_top_drift_right',[3.0,7.45,3.35],[4.15,.31,3.55],.14);
block('SNOW_lower_front_lip',[-3.05,-5.55,7.65],[3.35,1.25,.22],.12);
block('SNOW_right_packed_edge',[7.65,4.5,-3.7],[.22,2.15,2.5],.12);
const gems=[
{d:[0,0,1],c:[.65,1.7,7.6],w:2.3,h:1.45,spin:.23},
{d:[0,0,1],c:[-5.0,-3.3,7.6],w:1.45,h:1.03,spin:.47},
{d:[1,0,0],c:[7.6,3.25,-.6],w:2.15,h:1.30,spin:.17},
{d:[1,0,0],c:[7.6,-4.0,4.4],w:1.25,h:.94,spin:.45},
{d:[-1,0,0],c:[-7.6,.6,1.0],w:1.85,h:1.2,spin:.32},
{d:[0,0,-1],c:[3.25,-2.7,-7.6],w:2.0,h:1.25,spin:.15},
{d:[0,0,-1],c:[-4.1,3.8,-7.6],w:1.4,h:1.0,spin:.51},
{d:[0,1,0],c:[1.15,7.7,-1.75],w:2.15,h:1.25,spin:.31},
{d:[0,-1,0],c:[-1.7,-7.5,2.3],w:1.6,h:1.0,spin:.15}];
gems.forEach((g,idx)=>{
const n=norm(g.d),u=norm(cross(n,Math.abs(n[1])<.9?[0,1,0]:[1,0,0])),v=cross(n,u),center=g.c.map((x,i)=>x-n[i]*.50);
const vertices={},polys=[],mats=[],count=6;
const contour=[1,.96,.84,1.04,.89,.95],profiles=[[-.38,1],[g.h*.42,.96],[g.h,.55]];
for(let j=0;j<3;j++)for(let i=0;i<count;i++){const a=(i/count*Math.PI*2)+g.spin,r=g.w*contour[i]*profiles[j][1],offset=j===2?.2:0;
vertices['v'+j+'_'+i]=center.map((x,k)=>x+n[k]*profiles[j][0]+u[k]*(Math.cos(a)*r+offset)+v[k]*Math.sin(a)*r*.84);}
const vkey=(j,i)=>'v'+j+'_'+((i+count)%count);
const push=(ids,mat)=>{const a=vertices[ids[0]],b=vertices[ids[1]],c=vertices[ids[2]],fn=cross(b.map((x,k)=>x-a[k]),c.map((x,k)=>x-a[k])),fc=ids.map(id=>vertices[id]).reduce((a,b)=>add(a,b),[0,0,0]).map(x=>x/ids.length);
if(dot(fn,fc.map((x,k)=>x-center[k]))<0)ids.reverse();polys.push(ids);mats.push(mat);};
for(let j=0;j<2;j++)for(let i=0;i<count;i++)push([vkey(j,i),vkey(j,i+1),vkey(j+1,i+1),vkey(j+1,i)],j===0?'gem_dark':['gem_light','gem_mid','gem_dark','gem_mid','gem_light','gem_edge'][(i+idx)%6]);
vertices.cap=center.map((x,k)=>x+n[k]*(g.h+.12)+u[k]*.12);
for(let i=0;i<count;i++)push([vkey(2,i),vkey(2,i+1),'cap'],['gem_top','gem_light','gem_top','gem_mid','gem_top','gem_edge'][(i+idx)%6]);
vertices.base=center.map((x,k)=>x-n[k]*.38);
for(let i=0;i<count;i++)push([vkey(0,i+1),vkey(0,i),'base'],'gem_dark');
make('CRYSTAL_embedded_'+(idx+1),vertices,polys,mats);
});
Undo.finishEdit('Rebuild square snow block with shallow drifts and buried crystals',{elements,textures,outliner:true});
unselectAllElements();Canvas.updateAll();
return {project:Project.uuid,meshes:elements.length,faces:elements.reduce((n,m)=>n+Object.keys(m.faces).length,0),textures:textures.map(t=>t.name)};
})()
