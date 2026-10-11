// SPDX-License-Identifier: MIT
package dev.royalespells.client;

import dev.royalespells.RoyaleSpells;
import dev.royalespells.VisualState;
import dev.royalespells.entity.AllySkeleton;
import dev.royalespells.entity.RoyalWitch;
import dev.royalespells.entity.WitchBolt;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Opt-in isolated-world framebuffer QA. Triggers are checked at WorldRenderEvents.END:
 * the captured pixels correspond to the completed world frame, not the preceding
 * frame from END_CLIENT_TICK. This entrypoint never loads an existing player save. */
public final class WitchClientSmoke {
    private static final String WORLD="Witch-1.9.2-QA-remake-"+System.currentTimeMillis();
    private static int stage,ticks,ready,firstAttack=-1,firstSoul=-1,sceneStarted,frameNumber;
    private static volatile UUID witchId,activeWitchId,idleWitchId,targetId;
    private static volatile UUID walkingWitchId;
    private static String battleScene;
    private static final Set<String> captured=new HashSet<>();
    private static final Set<Integer> priorSceneBolts=new HashSet<>();
    private static final LinkedHashSet<String> queued=new LinkedHashSet<>();
    private static final List<Double> frameIntervals=new ArrayList<>();
    private static final Set<Integer> frozenSignatures=new HashSet<>(),frozenAttackAges=new HashSet<>(),otherSignatures=new HashSet<>();
    private static int frozenSamples,otherSamples;
    private static double snowStaffMin=Double.POSITIVE_INFINITY,snowStaffMax=Double.NEGATIVE_INFINITY;
    private static double resumedStaffMin=Double.POSITIVE_INFINITY,resumedStaffMax=Double.NEGATIVE_INFINITY;
    private static long slowFirstTick=-1,slowLastTick=-1,recoveryFirstTick=-1,recoveryLastTick=-1;
    private static int slowLastAge=-1,recoveryLastAge=-1,slowActionSteps,recoveryActionSteps;
    private static long started,lastFrame;
    private static boolean finishRequested,controlsPassed;
    private static float currentPartial;
    private static Vec3 walkStart;
    private static final Map<UUID,MotionTrace> motionTraces=new LinkedHashMap<>();
    private static final StringBuilder motionCsv=new StringBuilder("entity,kind,frame,wallNs,worldTick,partial,x,y,z,shaderTime,impactTime,impacted,serverImpacted,receivedImpactGap\n");
    private static int renderedSamples,clockBackwards,phaseBackwards,nonFiniteSamples,stairEvents,flightPairs,stationaryPairs,positionSpikes,excludedFrameGaps;
    private static int impactPairs,impactClockDiscontinuities,completionSamples;
    private static double maxFlightStep,maxImpactJoin,maxNormalizedSpeed,maxReceivedImpactGap;
    private static final Set<UUID> flightSubjects=new HashSet<>();
    private static final class MotionTrace {
        WitchMagicRenderer.VisualSample previous;
        long wallNs;
        float firstFlightTime=Float.NaN;
        boolean stationary;
        int flightPairs;
    }

    public static void install(){
        started=System.currentTimeMillis();
        ClientTickEvents.END_CLIENT_TICK.register(WitchClientSmoke::tick);
        WorldRenderEvents.END.register(context->frame(Minecraft.getInstance(),context.tickDelta()));
    }
    private static void request(String name){queued.add(name);}
    private static void capture(Minecraft c,String name,String evidence){
        if(!captured.add(name))return;
        System.out.println("WITCH_QA_FRAME_RENDER name="+name+" frame="+frameNumber+" worldTick="+c.level.getGameTime()+" partial="+currentPartial+" "+evidence);
        Screenshot.grab(c.gameDirectory,"witch19-"+name+".png",c.getMainRenderTarget(),message->System.out.println("WITCH_QA_FRAME "+name+" "+message.getString()));
    }
    private static void server(Minecraft c,Runnable action){if(c.getSingleplayerServer()!=null)c.getSingleplayerServer().execute(action);}
    private static void tick(Minecraft c){
        ticks++;c.options.pauseOnLostFocus=false;
        if(System.currentTimeMillis()-started>205000&&stage<3){System.out.println("WITCH_CLIENT_QA_TIMEOUT stage="+stage+" ready="+ready);stage=3;c.stop();return;}
        if(stage==0&&ticks>40&&c.screen instanceof TitleScreen&&c.getOverlay()==null){
            stage=1;c.options.renderDistance().set(5);c.options.simulationDistance().set(5);c.options.framerateLimit().set(60);c.options.fov().set(45);c.options.hideGui=true;
            var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);
            c.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.SPECTATOR,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(810195L,false,false),access->access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        }else if(stage==1&&c.level!=null&&c.player!=null&&c.getSingleplayerServer()!=null){
            stage=2;server(c,()->setup(c));
        }else if(stage==2&&c.level!=null&&c.player!=null){
            ready++;c.options.hideGui=true;
            if(c.screen instanceof net.minecraft.client.gui.screens.PauseScreen)c.setScreen(null);
            if(ready==15)server(c,()->model(c,false,0));
            if(ready==50)request("normal-front");
            if(ready==75)server(c,()->model(c,false,1));
            if(ready==110)request("normal-three-quarter");
            if(ready==135)server(c,()->model(c,false,2));
            if(ready==170)request("normal-back");
            if(ready==195)server(c,()->model(c,true,0));
            if(ready==230)request("evolved-front");
            if(ready==255)server(c,()->model(c,true,1));
            if(ready==290)request("evolved-three-quarter");
            if(ready==315)server(c,()->model(c,true,2));
            if(ready==350)request("evolved-back");
            if(ready==375)beginBattle(c,"front-oblique",0,false);
            if(ready==475)server(c,()->battleEvidence(c,"front-oblique"));
            if(ready==495)beginBattle(c,"high",1,true);
            if(ready==595)server(c,()->battleEvidence(c,"high"));
            if(ready==615)beginBattle(c,"rear",2,true);
            if(ready==715)server(c,()->battleEvidence(c,"rear"));
            if(ready==735){battleScene=null;server(c,()->summon(c,false));}
            if(ready==757)request("summon-opening");
            if(ready==770)server(c,()->holdSkeletons(c));
            if(ready==783)request("summon-four-skeletons");
            if(ready==810)server(c,()->summon(c,true));
            if(ready==848)server(c,()->holdSkeletons(c));
            if(ready==860)request("evolved-first-wave");
            if(ready==875)server(c,()->sacrifice(c));
            if(ready==925)server(c,()->soulEvidence(c));
            if(ready==945)server(c,()->controls(c));
            if(ready==975)request("two-witches-one-attacking");
            if(ready==985)server(c,()->snow(c,true));
            if(ready==1015)request("snow-slow-early");
            if(ready==1070)request("snow-slow-late");
            if(ready==1105)server(c,()->snow(c,false));
            if(ready==1130)request("snow-recovered-early");
            if(ready==1175)server(c,()->wakeOtherWitch(c));
            if(ready==1185)request("two-witches-independent-attacks");
            if(ready==1195)server(c,()->freeze(c,true));
            if(ready==1210)request("freeze-hold-01");
            if(ready==1240)request("freeze-hold-02");
            if(ready==1265)request("freeze-hold-03");
            if(ready==1275)server(c,()->freeze(c,false));
            if(ready==1295)request("freeze-resumed");
            if(ready==1325)beginBattle(c,"close-side",3,true);
            if(ready==1425)server(c,()->battleEvidence(c,"close-side"));
            if(ready==1445)beginBattle(c,"stone-occlusion",4,true);
            if(ready==1545)server(c,()->battleEvidence(c,"stone-occlusion"));
            if(ready==1595){battleScene=null;server(c,()->model(c,false,3));}
            if(ready==1620)request("face-close");
            if(ready==1635)server(c,()->model(c,true,4));
            if(ready==1665)request("profile-and-grip");
            if(ready==1695)server(c,()->walking(c));
            if(ready==1785)finishRequested=true;
        }
    }
    private static void frame(Minecraft c,float partial){
        if(stage!=2||c.level==null||c.player==null)return;
        currentPartial=partial;frameNumber++;
        long now=System.nanoTime();
        if(lastFrame!=0&&ready>=40)frameIntervals.add((now-lastFrame)/1_000_000d);lastFrame=now;
        // Every read below happens after this frame's models and world effects.
        for(String name:List.copyOf(queued))capture(c,name,witchEvidence(c));queued.clear();
        if(battleScene!=null&&ready>=sceneStarted+3&&ready<sceneStarted+108)watchAttack(c,battleScene);
        if(ready>=876&&ready<937)watchSoul(c);
        if(ready>=950)observeControls(c);
        observeVisualFrames(c,now);
        if(ready>=1700&&ready<1780)observeWalk(c);
        if(finishRequested){
            controlsPassed=controlEvidence();
            visualContinuityEvidence(c);
            frameEvidence();
            List<String> missing=new ArrayList<>();
            for(String scene:List.of("front-oblique","high","rear","close-side","stone-occlusion"))for(String part:List.of("windup","release","flight","impact-core","impact-expansion","impact-smoke","impact-embers"))if(!captured.contains(scene+"-"+part))missing.add(scene+"-"+part);
            for(String name:List.of("soul-04","soul-10","soul-17","soul-arrival","heal-flash"))if(!captured.contains(name))missing.add(name);
            for(String name:List.of("normal-front","normal-three-quarter","normal-back","evolved-front","evolved-three-quarter","evolved-back","summon-opening","summon-four-skeletons","evolved-first-wave","two-witches-one-attacking","snow-slow-early","snow-slow-late","snow-recovered-early","two-witches-independent-attacks","freeze-hold-01","freeze-hold-02","freeze-hold-03","freeze-resumed"))if(!captured.contains(name))missing.add(name);
            for(String name:List.of("face-close","profile-and-grip","walk-stride-a","walk-stride-b"))if(!captured.contains(name))missing.add(name);
            System.out.println("WITCH_CLIENT_QA_COMPLETE frames="+captured.size()+" renderedFrames="+frameNumber+" controlsPassed="+controlsPassed+" missing="+missing+" world="+WORLD);
            stage=3;c.stop();
        }
    }
    private static RoyalWitch clientWitch(Minecraft c,UUID id){if(id==null)return null;for(Entity entity:c.level.entitiesForRendering())if(id.equals(entity.getUUID())&&entity instanceof RoyalWitch witch)return witch;return null;}
    private static String witchEvidence(Minecraft c){var e=clientWitch(c,witchId);return e==null?"witch=absent":"witch="+e.getId()+" attackAge="+e.attackAge()+" summonAge="+e.summonAge()+" healFlash="+e.healFlash()+" pose="+FrozenRender.signature(e)+" staffPitch="+boneAngle(e,"staff");}
    private static void beginBattle(Minecraft c,String name,int angle,boolean evolved){
        battleScene=name;sceneStarted=ready;firstAttack=-1;priorSceneBolts.clear();
        // Server cleanup and its remove packets are asynchronous. Never select
        // a still-visible projectile from the preceding battle as this scene's
        // subject; it can disappear before reaching any capture stage.
        for(Entity entity:c.level.entitiesForRendering())if(entity instanceof WitchBolt)priorSceneBolts.add(entity.getId());
        server(c,()->battle(c,angle,evolved));
    }
    private static void watchAttack(Minecraft c,String scene){
        var witch=clientWitch(c,witchId);
        if(witch!=null){
            if(witch.attackAge()>=6&&witch.attackAge()<=8)capture(c,scene+"-windup",witchEvidence(c));
            if(witch.attackAge()>=13&&witch.attackAge()<=14)capture(c,scene+"-release",witchEvidence(c));
        }
        for(Entity entity:c.level.entitiesForRendering())if(entity instanceof WitchBolt b&&b.kind()==WitchBolt.ATTACK){
            if(priorSceneBolts.contains(b.getId()))continue;
            if(firstAttack>=0&&c.level.getEntity(firstAttack)==null&&!captured.contains(scene+"-flight"))firstAttack=-1;
            if(firstAttack<0&&!b.impacted()&&b.age()<=5)firstAttack=b.getId();if(b.getId()!=firstAttack)continue;
            var shown=WitchMagicRenderer.visualSample(b);if(shown==null)continue;
            String evidence="bolt="+b.getId()+" flightAge="+b.age()+" impactAge="+b.impactAge()+" visibleImpactAge="+shown.impactTime()+" serverImpacted="+shown.serverImpacted()+" point="+b.impactPosition();
            if(!shown.impacted()&&b.age()>=2&&b.age()<=4)capture(c,scene+"-flight",evidence);
            // Metadata may precede the final visible flight segment. A capture
            // labelled impact must show the actual submitted explosion volume.
            if(shown.impacted()){
                float age=shown.impactTime();
                if(age>=1&&age<4)capture(c,scene+"-impact-core",evidence);
                if(age>=4&&age<8)capture(c,scene+"-impact-expansion",evidence);
                if(age>=8&&age<13)capture(c,scene+"-impact-smoke",evidence);
                if(age>=13&&age<=16)capture(c,scene+"-impact-embers",evidence);
            }
        }
    }
    private static void watchSoul(Minecraft c){
        for(Entity entity:c.level.entitiesForRendering()){
            if(entity instanceof WitchBolt b&&b.kind()==WitchBolt.HEAL_SOUL){
                if(firstSoul<0)firstSoul=b.getId();if(b.getId()!=firstSoul)continue;
                var shown=WitchMagicRenderer.visualSample(b);if(shown==null)continue;
                String evidence="soul="+b.getId()+" flightAge="+b.age()+" impactAge="+b.impactAge()+" visibleImpactAge="+shown.impactTime()+" serverImpacted="+shown.serverImpacted();
                if(!shown.impacted()){
                    if(b.age()>=4&&b.age()<10)capture(c,"soul-04",evidence);
                    if(b.age()>=10&&b.age()<17)capture(c,"soul-10",evidence);
                    if(b.age()>=17&&b.age()<20)capture(c,"soul-17",evidence);
                }else if(shown.impactTime()>=2)capture(c,"soul-arrival",evidence);
            }
            if(entity instanceof RoyalWitch witch&&witch.healFlash()>=5)capture(c,"heal-flash",witchEvidence(c));
        }
    }
    /** Observe exactly the position/time supplied to this frame's volume draw.
     * Recomputing Entity#getPosition here would test a different code path and
     * could hide a renderer that still advances at 20 Hz. Store every actual
     * sample for an independently inspectable CSV, without per-frame disk I/O. */
    private static void observeVisualFrames(Minecraft c,long now){
        for(Entity entity:c.level.entitiesForRendering())if(entity instanceof WitchBolt bolt){
            var sample=WitchMagicRenderer.visualSample(bolt);if(sample==null)continue;
            var p=sample.position();
            if(p==null||!Double.isFinite(p.x)||!Double.isFinite(p.y)||!Double.isFinite(p.z)||!Float.isFinite(sample.time())||!Float.isFinite(sample.impactTime())||!Double.isFinite(sample.receivedImpactGap())){nonFiniteSamples++;continue;}
            var trace=motionTraces.computeIfAbsent(bolt.getUUID(),id->new MotionTrace());
            if(trace.previous!=null&&trace.previous.frameId()==sample.frameId())continue;
            renderedSamples++;
            if(sample.serverImpacted()&&!sample.impacted())completionSamples++;
            if(sample.receivedImpactGap()>=0)maxReceivedImpactGap=Math.max(maxReceivedImpactGap,sample.receivedImpactGap());
            motionCsv.append(bolt.getUUID()).append(',').append(bolt.kind()).append(',').append(sample.frameId()).append(',').append(now).append(',').append(c.level.getGameTime()).append(',').append(currentPartial).append(',')
                .append(p.x).append(',').append(p.y).append(',').append(p.z).append(',').append(sample.time()).append(',').append(sample.impactTime()).append(',').append(sample.impacted()).append(',').append(sample.serverImpacted()).append(',').append(sample.receivedImpactGap()).append('\n');
            if(!sample.impacted()&&Float.isNaN(trace.firstFlightTime))trace.firstFlightTime=sample.time();
            var old=trace.previous;
            if(old!=null){
                float elapsed=sample.time()-old.time();double step=p.distanceTo(old.position());
                if(elapsed<-.001f)clockBackwards++;
                if(old.impacted()&&(!sample.impacted()||sample.impactTime()+.001f<old.impactTime()))phaseBackwards++;
                if(!old.impacted()&&sample.impacted())maxImpactJoin=Math.max(maxImpactJoin,step);
                double wallMs=(now-trace.wallNs)/1_000_000d;
                boolean adjacent=sample.frameId()==old.frameId()+1;
                // A long stall cannot establish a 20 Hz staircase: there were
                // no intervening rendered frames to observe. Report separately.
                boolean regular=adjacent&&wallMs>=2&&wallMs<=45;
                if(!regular)excludedFrameGaps++;
                // The mist's impact phase must advance on the same rendered
                // clock as its moving noise, rather than wait for 20 Hz packet
                // ages and suddenly catch up on a later frame.
                if(regular&&old.impacted()&&sample.impacted()&&elapsed>.035f){
                    impactPairs++;
                    if(Math.abs((sample.impactTime()-old.impactTime())-elapsed)>.04f)impactClockDiscontinuities++;
                }
                boolean flight=bolt.kind()==WitchBolt.ATTACK&&!old.impacted()&&!sample.impacted()&&old.time()-trace.firstFlightTime>=1;
                if(flight&&regular&&elapsed>.035f){
                    flightPairs++;trace.flightPairs++;flightSubjects.add(bolt.getUUID());
                    boolean stationary=step<.002;
                    if(stationary)stationaryPairs++;
                    if(trace.stationary&&step>WitchBolt.SPEED*.55&&elapsed<.75f)stairEvents++;
                    if(step>WitchBolt.SPEED*.4&&step/elapsed>WitchBolt.SPEED*2.75)positionSpikes++;
                    maxFlightStep=Math.max(maxFlightStep,step);maxNormalizedSpeed=Math.max(maxNormalizedSpeed,step/elapsed);trace.stationary=stationary;
                }else trace.stationary=false;
            }
            trace.previous=sample;trace.wallNs=now;
        }
    }
    private static void visualContinuityEvidence(Minecraft c){
        boolean saved=false;
        try{Files.writeString(c.gameDirectory.toPath().resolve("witch-visual-continuity.csv"),motionCsv,StandardCharsets.UTF_8);saved=true;}
        catch(java.io.IOException ex){System.out.println("WITCH_QA_CONTINUITY_SAVE_FAILED "+ex);}
        double stationaryRatio=stationaryPairs/(double)Math.max(1,flightPairs);
        boolean passed=saved&&renderedSamples>=100&&flightSubjects.size()>=5&&flightPairs>=40&&impactPairs>=100&&clockBackwards==0&&phaseBackwards==0&&nonFiniteSamples==0&&stairEvents==0&&positionSpikes==0&&impactClockDiscontinuities==0&&stationaryRatio<.25;
        System.out.println("WITCH_QA_VISUAL_CONTINUITY passed="+passed+" samples="+renderedSamples+" flightSubjects="+flightSubjects.size()+" flightPairs="+flightPairs+" stationaryPairs="+stationaryPairs+" stationaryRatio="+stationaryRatio+" stairEvents="+stairEvents+" positionSpikes="+positionSpikes+" clockBackwards="+clockBackwards+" phaseBackwards="+phaseBackwards+" impactPairs="+impactPairs+" impactClockDiscontinuities="+impactClockDiscontinuities+" nonFinite="+nonFiniteSamples+" excludedFrameGaps="+excludedFrameGaps+" maxFlightStep="+maxFlightStep+" maxStepPerVisualTick="+maxNormalizedSpeed+" maxFlightToImpactJoin="+maxImpactJoin+" completionSamples="+completionSamples+" maxReceivedImpactGap="+maxReceivedImpactGap+" serverSpeed="+WitchBolt.SPEED+" csv=witch-visual-continuity.csv scope=actual-rendered-frames notNetworkImpairmentTest=true visualQualityRequiresHumanReview=true");
    }
    private static void observeWalk(Minecraft c){
        var e=clientWitch(c,walkingWitchId);if(e==null)return;
        Vec3 at=e.getPosition(currentPartial);if(walkStart==null){walkStart=at;return;}
        double moved=at.distanceTo(walkStart),right=boneAngle(e,"right_leg"),left=boneAngle(e,"left_leg");
        if(moved<.025||!Double.isFinite(right)||!Double.isFinite(left))return;
        String evidence="witch="+e.getId()+" moved="+moved+" rightLeg="+right+" leftLeg="+left+" pose="+FrozenRender.signature(e);
        if(right>.025&&left<-.025)capture(c,"walk-stride-a",evidence);
        if(right<-.025&&left>.025)capture(c,"walk-stride-b",evidence);
    }
    /** The normal renderer shares its model, so after the frame its mutable bones
     * belong to whichever witch rendered last. Inspect FrozenRender's existing
     * per-entity snapshots instead. Reflection is confined to this opt-in QA. */
    private static double boneAngle(RoyalWitch entity,String bone){
        try{
            Field f=FrozenRender.class.getDeclaredField("STATES");f.setAccessible(true);Object state=((Map<?,?>)f.get(null)).get(entity);if(state==null)return Double.NaN;
            Field posesField=state.getClass().getDeclaredField("poses");posesField.setAccessible(true);
            for(var entry:((Map<?,?>)posesField.get(state)).entrySet()){
                Object part=entry.getKey();if(!part.getClass().getName().contains("TroopModel$Part"))continue;
                Field name=part.getClass().getDeclaredField("name");name.setAccessible(true);
                if(bone.equals(name.get(part)))return ((float[])entry.getValue())[0];
            }
        }catch(ReflectiveOperationException ignored){}
        return Double.NaN;
    }
    private static void observeControls(Minecraft c){
        RoyalWitch active=clientWitch(c,activeWitchId),other=clientWitch(c,idleWitchId);if(active==null||other==null)return;
        double pitch=boneAngle(active,"left_arm");
        if(Double.isFinite(pitch)&&ready>=1000&&ready<=1070){snowStaffMin=Math.min(snowStaffMin,pitch);snowStaffMax=Math.max(snowStaffMax,pitch);}
        if(Double.isFinite(pitch)&&ready>=1120&&ready<=1170){resumedStaffMin=Math.min(resumedStaffMin,pitch);resumedStaffMax=Math.max(resumedStaffMax,pitch);}
        long tick=c.level.getGameTime();int age=active.attackAge();
        // Count synchronized attack-clock steps against actual world ticks,
        // not render frames. A frame-rate cap must not masquerade as slowing.
        if(ready>=995&&ready<=1095&&tick!=slowLastTick&&age>0){
            if(slowFirstTick<0)slowFirstTick=tick;
            if(slowLastAge>0)slowActionSteps+=(age-slowLastAge+RoyalWitch.ATTACK_TICKS)%RoyalWitch.ATTACK_TICKS;
            slowLastTick=tick;slowLastAge=age;
        }
        if(ready>=1120&&ready<=1170&&tick!=recoveryLastTick&&age>0){
            if(recoveryFirstTick<0)recoveryFirstTick=tick;
            if(recoveryLastAge>0)recoveryActionSteps+=(age-recoveryLastAge+RoyalWitch.ATTACK_TICKS)%RoyalWitch.ATTACK_TICKS;
            recoveryLastTick=tick;recoveryLastAge=age;
        }
        if(ready>=1210&&ready<=1265&&VisualState.frozen(active)&&FrozenRender.parts(active)>0){frozenSamples++;frozenSignatures.add(FrozenRender.signature(active));frozenAttackAges.add(active.attackAge());}
        if(ready>=1210&&ready<=1265&&!VisualState.frozen(other)&&FrozenRender.parts(other)>0){otherSamples++;otherSignatures.add(FrozenRender.signature(other));}
    }
    private static boolean controlEvidence(){
        double slowSpan=snowStaffMax-snowStaffMin,resumedSpan=resumedStaffMax-resumedStaffMin;
        double slowRate=slowActionSteps/(double)Math.max(1,slowLastTick-slowFirstTick),recoveryRate=recoveryActionSteps/(double)Math.max(1,recoveryLastTick-recoveryFirstTick);
        boolean passed=Double.isFinite(slowSpan)&&slowSpan>.45&&Double.isFinite(resumedSpan)&&resumedSpan>.45
            &&slowLastTick-slowFirstTick>=60&&slowRate>.50&&slowRate<.82
            &&recoveryLastTick-recoveryFirstTick>=30&&recoveryRate>.82&&recoveryRate<1.18
            &&frozenSamples>=20&&frozenSignatures.size()==1&&frozenAttackAges.size()==1&&otherSamples>=20&&otherSignatures.size()>3;
        System.out.println("WITCH_QA_CONTROL_CHECK passed="+passed+" snowStaffSpanRadians="+slowSpan+" recoveredStaffSpanRadians="+resumedSpan+" snowActionStepsPerWorldTick="+slowRate+" recoveredActionStepsPerWorldTick="+recoveryRate+" frozenSamples="+frozenSamples+" frozenPoseVariants="+frozenSignatures.size()+" frozenAttackVariants="+frozenAttackAges.size()+" otherSamples="+otherSamples+" otherPoseVariants="+otherSignatures.size());return passed;
    }
    private static void frameEvidence(){
        if(frameIntervals.isEmpty())return;var sorted=new ArrayList<>(frameIntervals);Collections.sort(sorted);
        double mean=frameIntervals.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        System.out.println("WITCH_QA_FRAME_INTERVALS samples="+sorted.size()+" meanMs="+mean+" p50Ms="+sorted.get(sorted.size()/2)+" p95Ms="+sorted.get((int)((sorted.size()-1)*.95))+" maxMs="+sorted.get(sorted.size()-1)+" includesScreenshotReadbacks=true frameCap=60 scope=isolated-flat-QA-only notAPlayerModpackPerformancePromise=true");
    }
    private static ServerLevel world(Minecraft c){return c.getSingleplayerServer().overworld();}
    private static ServerPlayer player(Minecraft c){return c.getSingleplayerServer().getPlayerList().getPlayers().get(0);}
    private static void setup(Minecraft c){
        var w=world(c);w.setDayTime(6000);w.setWeatherParameters(6000,0,false,false);
        System.out.println("WITCH_QA_RENDER_BASELINE externalShaderMods="+net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods().stream().filter(m->Set.of("iris","oculus","optifabric").contains(m.getMetadata().getId())).map(m->m.getMetadata().getId()).toList()+" vanillaPipeline=true externalBloom=false visualQualityRequiresHumanReview=true");
        for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++){
            w.setBlockAndUpdate(new BlockPos(x,-61,z),Blocks.SMOOTH_STONE.defaultBlockState());
            for(int y=-60;y<-53;y++)w.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
        }
        camera(c,new Vec3(0,-58.25,-3.35),new Vec3(0,-58.65,0));
    }
    private static void clear(Minecraft c){var list=new ArrayList<Entity>();for(Entity e:world(c).getAllEntities())if(!(e instanceof ServerPlayer))list.add(e);list.forEach(Entity::discard);}
    private static void camera(Minecraft c,Vec3 eye,Vec3 at){
        var p=player(c);p.setGameMode(GameType.SPECTATOR);p.getInventory().clearContent();p.inventoryMenu.broadcastChanges();
        Vec3 d=at.subtract(eye);float yaw=(float)(Math.toDegrees(Math.atan2(d.z,d.x))-90),pitch=(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance()));
        p.teleportTo(world(c),eye.x,eye.y-p.getEyeHeight(),eye.z,yaw,pitch);p.setDeltaMovement(Vec3.ZERO);
    }
    private static RoyalWitch witch(Minecraft c,boolean evolved,Vec3 at,boolean quiet){
        var e=RoyaleSpells.WITCH.create(world(c));e.configure(player(c).getUUID(),evolved,11);e.setPos(at);e.setNoAi(true);e.setYRot(180);e.setYBodyRot(180);e.setYHeadRot(180);
        if(quiet){var n=new CompoundTag();e.addAdditionalSaveData(n);n.putInt("Deploy",0);n.putInt("SummonClock",0);n.putBoolean("DeploymentSound",true);e.readAdditionalSaveData(n);}
        world(c).addFreshEntity(e);witchId=e.getUUID();return e;
    }
    private static void model(Minecraft c,boolean evolved,int angle){
        clear(c);witch(c,evolved,new Vec3(0,-60,0),true);
        Vec3 eye=switch(angle){case 1->new Vec3(-2.50,-58.1,-2.60);case 2->new Vec3(0,-58.1,3.35);case 3->new Vec3(0,-58.35,-1.55);case 4->new Vec3(-1.95,-58.55,-.42);default->new Vec3(0,-58.25,-3.35);};camera(c,eye,new Vec3(0,angle==3?-58.38:angle==4?-58.63:-58.65,0));
    }
    private static void walking(Minecraft c){
        clear(c);var e=witch(c,false,new Vec3(0,-60,-2),true);e.setNoAi(false);e.setYRot(0);e.setYBodyRot(0);e.setYHeadRot(0);
        e.setTarget(victim(c,new Vec3(0,-60,10)));walkingWitchId=e.getUUID();walkStart=null;
        camera(c,new Vec3(-4.2,-58.15,-.2),new Vec3(0,-58.95,0));
        System.out.println("WITCH_QA_WALK_SETUP witch="+e.getUUID()+" naturalPathfinding=true targetDistance=12 captureRequiresMovementAndOppositeLegPoses=true");
    }
    private static net.minecraft.world.entity.monster.Zombie victim(Minecraft c,Vec3 at){
        var e=EntityType.ZOMBIE.create(world(c));e.setPos(at);e.setNoAi(true);e.setSilent(true);e.setPersistenceRequired();e.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);e.setHealth(500);world(c).addFreshEntity(e);return e;
    }
    private static void battle(Minecraft c,int angle,boolean evolved){
        clear(c);var e=witch(c,evolved,new Vec3(0,-60,-2),true);e.setYRot(0);e.setYBodyRot(0);e.setYHeadRot(0);
        for(int i=-1;i<=1;i++){var target=victim(c,new Vec3(i*.68,-60,2.2));if(i==0)e.setTarget(target);}
        Vec3 eye=switch(angle){case 1->new Vec3(3.7,-51.0,-.5);case 2->new Vec3(-4.0,-56.9,-5.7);case 3->new Vec3(3.15,-58.45,.5);case 4->new Vec3(3.8,-58.6,.4);default->new Vec3(5.0,-56.9,5.0);};
        if(angle==4){
            // The wall is in the camera's sightline, alongside the actual shot
            // path (x~=0). It masks the impact centre while its upper/lateral
            // edges let the outer density remain visible. No fake impact input.
            for(int y=-60;y<=-59;y++)world(c).setBlockAndUpdate(new BlockPos(1,y,1),Blocks.STONE.defaultBlockState());
            System.out.println("WITCH_QA_OCCLUSION wall=[1,-60..-59,1] camera="+eye+" attackPathX=0 sceneDepthMustHideCentre=true outerVolumeShouldRemainVisible=true visualCheckOnly=true");
        }
        camera(c,eye,angle==4?new Vec3(0,-59,1.8):angle==3?new Vec3(0,-59,.8):new Vec3(0,-59,.1));
    }
    private static void battleEvidence(Minecraft c,String scene){for(Entity e:world(c).getAllEntities())if(e instanceof net.minecraft.world.entity.monster.Zombie z)System.out.println("WITCH_QA_SPLASH_VICTIM scene="+scene+" x="+z.getX()+" health="+z.getHealth()+" / "+z.getMaxHealth());}
    private static void summon(Minecraft c,boolean evolved){clear(c);witch(c,evolved,new Vec3(0,-60,0),false);camera(c,new Vec3(3.45,-57.55,-4.10),new Vec3(-.15,-59.0,.2));}
    private static void holdSkeletons(Minecraft c){for(Entity e:world(c).getAllEntities())if(e instanceof AllySkeleton skeleton){skeleton.setNoAi(true);skeleton.getNavigation().stop();skeleton.setDeltaMovement(Vec3.ZERO);}}
    private static void sacrifice(Minecraft c){
        var w=world(c);if(!(w.getEntity(witchId) instanceof RoyalWitch mother))throw new IllegalStateException("QA mother disappeared");mother.setHealth(Math.max(1,mother.getHealth()-6));
        var id=mother.eligibleSkeletons().stream().findFirst().orElseThrow();if(!(w.getEntity(id) instanceof AllySkeleton child))throw new IllegalStateException("QA first-wave skeleton missing");child.setPos(-2.4,-60,1.3);
        System.out.println("WITCH_QA_SOUL_BEFORE health="+mother.getHealth()+" max="+mother.getMaxHealth()+" skeleton="+child.getUUID());child.hurt(w.damageSources().genericKill(),10000);
    }
    private static void soulEvidence(Minecraft c){if(world(c).getEntity(witchId) instanceof RoyalWitch mother)System.out.println("WITCH_QA_SOUL_AFTER health="+mother.getHealth()+" max="+mother.getMaxHealth()+" healed="+mother.healedSouls()+" remainingFirstWave="+mother.eligibleSkeletons().size());}
    private static void controls(Minecraft c){
        clear(c);var active=witch(c,false,new Vec3(-1.2,-60,-2),true);var idle=witch(c,false,new Vec3(1.2,-60,-2),true);
        var target=victim(c,new Vec3(-1.2,-60,2.2));active.setTarget(target);active.setYRot(0);active.setYBodyRot(0);active.setYHeadRot(0);idle.setYRot(0);idle.setYBodyRot(0);idle.setYHeadRot(0);
        activeWitchId=active.getUUID();idleWitchId=idle.getUUID();targetId=target.getUUID();witchId=activeWitchId;camera(c,new Vec3(5.1,-56.7,3.5),new Vec3(0,-58.8,-.6));
        System.out.println("WITCH_QA_CONTROL_SETUP active="+activeWitchId+" idle="+idleWitchId+" sharedNormalModel=true");
    }
    private static void snow(Minecraft c,boolean enabled){if(world(c).getEntity(activeWitchId) instanceof RoyalWitch e){if(enabled)e.addEffect(new MobEffectInstance(RoyaleSpells.SNOWBOUND,120,0,false,false,true));else e.removeEffect(RoyaleSpells.SNOWBOUND);System.out.println("WITCH_QA_SNOW enabled="+enabled+" attackAge="+e.attackAge()+" serverTick="+world(c).getGameTime());}}
    private static void wakeOtherWitch(Minecraft c){if(world(c).getEntity(idleWitchId) instanceof RoyalWitch e&&world(c).getEntity(targetId) instanceof net.minecraft.world.entity.LivingEntity target)e.setTarget(target);}
    private static void freeze(Minecraft c,boolean enabled){if(world(c).getEntity(activeWitchId) instanceof RoyalWitch e){if(enabled){e.addEffect(new MobEffectInstance(RoyaleSpells.FROZEN,200,0,false,false,true));e.addEffect(new MobEffectInstance(RoyaleSpells.STUN,200,0,false,false,true));}else{e.removeEffect(RoyaleSpells.FROZEN);e.removeEffect(RoyaleSpells.STUN);}System.out.println("WITCH_QA_HARD_FREEZE enabled="+enabled+" attackAge="+e.attackAge()+" serverTick="+world(c).getGameTime());}}
    private WitchClientSmoke(){}
}
