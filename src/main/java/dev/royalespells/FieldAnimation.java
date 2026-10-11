package dev.royalespells;

/** Deterministic timing shared by field geometry, motes and visual checks. */
public final class FieldAnimation {
    private static float clamp(float x){return Math.max(0,Math.min(1,x));}
    private static float smooth(float x){x=clamp(x);return x*x*(3-2*x);}
    public static float opening(Spell spell,float time){float ticks=spell==Spell.RAGE?4:spell==Spell.VOID?10:12;float x=clamp(time/ticks);return 1-(1-x)*(1-x)*(1-x);}
    public static float opacity(Spell spell,float time,int duration){float out=spell==Spell.GRAVEYARD?18:spell==Spell.RAGE?12:14;return smooth(time/3)*smooth((duration-time)/out);}
    public static float radius(Spell spell,float time,int duration){float closing=spell==Spell.VOID?smooth((time-duration+14)/14):0;return opening(spell,time)*(1-.28f*closing);}
    private FieldAnimation(){}
}
