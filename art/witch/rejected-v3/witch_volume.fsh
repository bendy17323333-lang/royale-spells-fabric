#version 150
// SPDX-License-Identifier: MIT
// Continuous world-local participating medium. The rendered proxy is closed
// geometry, not a view-aligned image. All color emerges from density samples.
uniform sampler2D SceneDepth;
uniform mat4 InverseProjection;
uniform mat4 ViewToVolume;
uniform vec2 TargetSize;
uniform vec3 BoundsMin;
uniform vec3 BoundsMax;
uniform vec4 Params; // mode, time in ticks, normalized effect age, opacity
uniform vec4 Shape;  // physical radius, ray steps, evolution, tail point count
uniform vec4 Tail0;
uniform vec4 Tail1;
uniform vec4 Tail2;
uniform vec4 Tail3;
uniform vec4 Tail4;
uniform vec4 Tail5;
uniform vec4 Tail6;
in vec3 localPosition;
out vec4 fragColor;

float hash3(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yxz + 33.33);
    return fract((p.x + p.y) * p.z);
}
float densityNoise(vec3 p) {
    vec3 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);
    return mix(mix(mix(hash3(i),hash3(i+vec3(1,0,0)),f.x),
                   mix(hash3(i+vec3(0,1,0)),hash3(i+vec3(1,1,0)),f.x),f.y),
               mix(mix(hash3(i+vec3(0,0,1)),hash3(i+vec3(1,0,1)),f.x),
                   mix(hash3(i+vec3(0,1,1)),hash3(i+vec3(1,1,1)),f.x),f.y),f.z);
}
float turbulence(vec3 p,float t) {
    // Two coherent scales advect at different velocities; neither is random
    // per frame, so soft folds flow instead of flickering or boiling apart.
    return 0.69*densityNoise(p*2.9+vec3(t*.11,-t*.17,t*.06))
         + 0.31*densityNoise(p*6.4+vec3(-t*.08,t*.07,t*.12));
}
vec3 bend(vec3 p,float t) {
    return p + .17*vec3(sin(p.y*2.1+p.z*.7+t*.31),
                       sin(p.z*2.3-p.x*.8-t*.23),
                       sin(p.x*1.7+p.y*.9+t*.19));
}
vec4 tailPoint(int i) {
    if(i==0)return Tail0;if(i==1)return Tail1;if(i==2)return Tail2;
    if(i==3)return Tail3;if(i==4)return Tail4;if(i==5)return Tail5;return Tail6;
}

// density, thermal/core illumination, tail illumination, mint-heal factor
vec4 medium(vec3 p) {
    float mode=Params.x,t=Params.y*.1,age=Params.z,r=Shape.x;
    float density=0.0,glow=0.0,tailLight=0.0,mint=0.0;
    if(mode<.5 || (mode>1.5&&mode<2.5)) {
        vec3 q=p/max(.07,r);
        vec3 warped=bend(q,t);
        float d=length(warped);
        float n=turbulence(warped,t);
        float edge=1.0-smoothstep(.60,1.39,d);
        float cloud=exp(-d*d*1.9)*edge*smoothstep(.16,.68,n);
        float core=exp(-dot(q,q)*15.0);
        density=cloud*3.0+core*8.0;
        glow=exp(-dot(q,q)*3.6);
        // A swept field joins every historical segment into one soft wake.
        // Radius and intensity taper along the entire wake, not per particle.
        float wake=0.0;
        float wakeNoise=.47+.75*turbulence(p*2.4,t);
        for(int i=0;i<6;i++) {
            if(float(i+1)>=Shape.w)break;
            vec4 a=tailPoint(i),b=tailPoint(i+1);vec3 ab=b.xyz-a.xyz;
            float u=clamp(dot(p-a.xyz,ab)/max(dot(ab,ab),.00001),0.0,1.0);
            vec3 center=mix(a.xyz,b.xyz,u);
            float along=(float(i)+u)/max(1.0,Shape.w-1.0);
            // Asymmetric rising curl, fixed to trail age rather than camera.
            center+=vec3(sin(along*4.1-t*.29)*.055*along,along*along*.16,
                         cos(along*3.3+t*.21)*.07*along);
            float width=mix(a.w,b.w,u);
            vec3 off=(p-center)/max(width,.025);
            off+=vec3(sin(off.y*1.2+t*.23),sin(off.z*1.8-t*.13),sin(off.x*1.1+t*.19))*.13;
            float radial=length(off);
            float envelope=exp(-radial*radial*2.0)*(1.0-smoothstep(.7,1.6,radial));
            wake=max(wake,envelope*pow(1.0-along,.7)*wakeNoise);
        }
        density+=wake*2.5;tailLight=wake*.16;
    } else if(mode<1.5) {
        // An expanding filled cloud, never a hollow ring or a set of petals.
        float opened=1.0-exp(-age*5.0);
        float spread=.19+r*opened*.83;
        vec3 q=(p-vec3(age*.09,age*.22,-age*.06))/spread;
        q.y*=1.15;
        vec3 w=bend(q,t+age*1.7);
        w+=vec3(.07*sin(q.z*3.1+age*4.0),.11*sin(q.x*2.7-age*3.0),.06*cos(q.y*3.0));
        float d=length(w),n=turbulence(w+vec3(age*.7,-age*.9,age*.3),t);
        float cloud=exp(-d*d*1.4)*(1.0-smoothstep(.77,1.53,d))*smoothstep(.13,.69,n);
        float fade=pow(max(0.0,1.0-age),1.35);
        float coreSize=.12+.19*opened;
        float core=exp(-dot(p,p)/(coreSize*coreSize))*exp(-age*7.0);
        density=(cloud*3.7*fade+core*9.0);
        glow=clamp(core*1.6+exp(-dot(q,q)*2.4)*exp(-age*2.5)*.52,0.0,1.0);
    } else if(mode<3.5) {
        vec3 q=p/max(.08,r*(.7+Params.z*.7));
        float d=length(bend(q,t));
        density=exp(-d*d*3.0)*(1.0-smoothstep(.7,1.4,d))*5.0*pow(1.0-age,1.5);
        glow=exp(-dot(q,q)*5.0);mint=glow;
    } else {
        vec3 q=(p-vec3(.04,.13+age*.08,-.03))/vec3(r,.25,r);
        q=bend(q,t);float d=length(q);
        float n=turbulence(q+vec3(0,-age*.8,0),t);
        density=exp(-d*d*1.5)*(1.0-smoothstep(.6,1.45,d))*smoothstep(.15,.65,n)*2.8;
        glow=exp(-dot(q,q)*5.0)*.25;
    }
    return vec4(density*Params.w,glow,tailLight,mint);
}

void main() {
    vec3 camera=(ViewToVolume*vec4(0,0,0,1)).xyz;
    vec3 ray=normalize(localPosition-camera);
    vec3 inverseRay=1.0/(ray+vec3(.0000001));
    vec3 a=(BoundsMin-camera)*inverseRay,b=(BoundsMax-camera)*inverseRay;
    vec3 enter=min(a,b),leave=max(a,b);
    float nearT=max(0.0,max(enter.x,max(enter.y,enter.z)));
    float farT=min(leave.x,min(leave.y,leave.z));
    vec2 uv=gl_FragCoord.xy/TargetSize;
    float sceneDepth=texture(SceneDepth,uv).r;
    vec4 view=InverseProjection*vec4(uv*2.0-1.0,sceneDepth*2.0-1.0,1.0);
    view/=view.w;
    vec3 scene=(ViewToVolume*view).xyz;
    float sceneT=dot(scene-camera,ray);
    farT=min(farT,sceneT);
    if(farT<=nearT)discard;
    int count=int(Shape.y);
    float stepLength=(farT-nearT)/float(count);
    // Stable sub-step dither removes slice bands without animated grain.
    float jitter=fract(dot(gl_FragCoord.xy,vec2(.754877666,.569840296)));
    vec3 accumulated=vec3(0);float transmittance=1.0;
    for(int i=0;i<40;i++) {
        if(i>=count||transmittance<.015)break;
        float at=nearT+(float(i)+.18+.64*jitter)*stepLength;
        vec3 p=camera+ray*at;
        vec4 field=medium(p);
        // Soft particle/terrain intersection in actual world-distance units.
        float density=field.x*smoothstep(0.0,.055,sceneT-at);
        if(density<.001)continue;
        float alpha=1.0-exp(-density*stepLength*4.3);
        // Outskirts absorb to dark plum, inner medium scatters saturated pink.
        // Bounded non-additive colors preserve hue without bloom or whiteout.
        float warm=clamp(field.y*1.32+field.z,0.0,1.0);
        vec3 color=mix(vec3(.18,.025,.30),vec3(.67,.15,.80),sqrt(warm));
        color=mix(color,vec3(.97,.57,.94),pow(warm,3.2));
        color=mix(color,vec3(.70,.94,.69),field.w*.8);
        // Broad directional scattering makes the fog depth readable without
        // turning it into a shaded opaque solid surface.
        color*=.91+.09*clamp(p.y/max(Shape.x,.1)+.5,0.0,1.0);
        accumulated+=transmittance*alpha*color;
        transmittance*=1.0-alpha;
    }
    float opacity=1.0-transmittance;
    if(opacity<.003)discard;
    fragColor=vec4(accumulated/max(opacity,.0001),opacity);
}
