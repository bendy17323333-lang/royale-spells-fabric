// SPDX-License-Identifier: MIT
package dev.royalespells.client;

import dev.royalespells.entity.*;
import dev.royalespells.iron.WitchContent;
import dev.royalespells.RoyaleSpells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;
import java.util.*;

/** Explicit opt-in only. Fresh disposable world, actual framebuffers, no user saves. */
public final class WitchWorldSmoke {
    private static final boolean CARDS=Boolean.getBoolean("royalespells.witchCardSmoke");
    private static int stage,ticks,ready,scene;
    private static long started;
    private static UUID witchId;
    private static final Set<String> captured=new LinkedHashSet<>();
    private static String requested;
    private static volatile boolean towerPlaced;
    private static volatile Vec3 roof;
    private static int volumeFrames;
    public static void install(){
        started=System.currentTimeMillis();
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e)->tick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent e)->{
            if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
            var c=Minecraft.getInstance();if(c.level==null)return;
            if(requested!=null){capture(c,requested);requested=null;}
            if(scene>=8&&scene<=10){
                var w=c.level.getEntity(findId(c,witchId));
                if(w instanceof WitchActor witch){
                    if(witch.attackAge()>=8&&witch.attackAge()<=11)capture(c,"left-palm-charge-"+scene);
                    if(witch.attackAge()==14)capture(c,"left-palm-release-"+scene);
                }
                for(var entity:c.level.entitiesForRendering())if(entity instanceof WitchBolt b){
                    var sample=WitchMagicRenderer.visualSample(b);if(sample==null)continue;volumeFrames++;
                    if(sample.impacted()&&b.impactAge()>3)capture(c,"volume-impact-"+scene);
                    else if(!sample.impacted()&&b.age()>2)capture(c,"volume-flight-"+scene);
                }
            }
        });
    }
    private static int findId(Minecraft c,UUID id){if(id!=null)for(var e:c.level.entitiesForRendering())if(e.getUUID().equals(id))return e.getId();return -1;}
    private static void capture(Minecraft c,String name){if(!captured.add(name))return;Screenshot.grab(c.gameDirectory,"witch193-"+name+".png",c.getMainRenderTarget(),m->System.out.println("WITCH193_FRAME "+name+" "+m.getString()));}
    private static void tick(Minecraft c){
        ticks++;c.options.pauseOnLostFocus=false;
        if(System.currentTimeMillis()-started>(stage<2?300000:165000)){System.out.println("WITCH193_TIMEOUT stage="+stage+" ready="+ready);c.stop();return;}
        if(stage==0&&ticks>35&&c.screen instanceof TitleScreen&&c.getOverlay()==null){
            stage=1;c.options.hideGui=true;c.options.renderDistance().set(6);c.options.fov().set(45);c.options.framerateLimit().set(60);
            var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);
            String name="Witch-Neo-193-QA-"+System.currentTimeMillis();
            c.createWorldOpenFlows().createFreshLevel(name,new LevelSettings(name,GameType.SPECTATOR,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(192192L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());
        }else if(stage==1&&c.player!=null&&c.level!=null&&c.getSingleplayerServer()!=null){
            stage=2;started=System.currentTimeMillis();c.getSingleplayerServer().execute(()->{
                var l=world(c);l.setDayTime(6000);
                for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)l.setBlockAndUpdate(new BlockPos(x,79,z),Blocks.SMOOTH_STONE.defaultBlockState());
                model(c,0);
            });
        }else if(stage==2&&c.player!=null){
            ready++;c.options.hideGui=true;if(c.screen instanceof net.minecraft.client.gui.screens.PauseScreen)c.setScreen(null);
            if(ready%50==35&&ready<400)requested=(scene<4?"normal-":"evolved-")+new String[]{"front","angle","side","back"}[scene%4];
            if(ready%50==0&&ready<400){scene=ready/50;int s=scene;c.getSingleplayerServer().execute(()->model(c,s));}
            if(ready==400||ready==490||ready==580){scene=8+(ready-400)/90;int s=scene;c.getSingleplayerServer().execute(()->battle(c,s));}
            if(ready==680&&CARDS){System.out.println("WITCH193_CARD_COMPLETE captures="+captured.size()+" volumeFrames="+volumeFrames+" names="+captured);stage=3;c.stop();return;}
            if(ready==680){scene=11;c.getSingleplayerServer().execute(()->{
                for(var e:com.google.common.collect.ImmutableList.copyOf(world(c).getAllEntities()))if(e instanceof WitchActor||e instanceof WitchBolt||e instanceof AllySkeleton||e.getType()==EntityType.IRON_GOLEM)e.discard();
                c.getSingleplayerServer().getCommands().performPrefixedCommand(c.getSingleplayerServer().createCommandSourceStack(),"place structure royalespells:witch_tower 96 -60 0");
                for(var e:world(c).getAllEntities())if(e instanceof WitchActor){roof=e.position();break;}
                towerPlaced=roof!=null;
                if(roof!=null)camera(c,roof.add(33,7,-39),roof.add(0,-13,0));
                System.out.println("WITCH193_TOWER placed="+towerPlaced+" roof="+roof);
            });}
            if(ready==735)requested="tower-overview";
            if(ready==780&&roof!=null)c.getSingleplayerServer().execute(()->world(c).getEntitiesOfClass(Mob.class,new AABB(roof,roof).inflate(16)).stream().filter(w->w instanceof WitchActor).findFirst().ifPresent(w->inspect(c,w.position().add(0,1.1,0))));
            if(ready==790)requested="tower-resident";
            if(ready==830&&roof!=null)c.getSingleplayerServer().execute(()->{
                world(c).getEntitiesOfClass(WitchSkeleton.class,new AABB(roof,roof).inflate(36)).stream()
                    .filter(s->s.getTags().contains("RoyaleWitchGroundPatrol")).findFirst().ifPresent(s->inspect(c,s.position().add(0,.8,0)));
            });
            if(ready==840)requested="tower-ground-patrol";
            if(ready==850&&roof!=null)c.getSingleplayerServer().execute(()->{
                world(c).getEntitiesOfClass(Mob.class,new AABB(roof,roof).inflate(60),e->e.getType()==EntityType.PILLAGER||e.getType()==EntityType.VINDICATOR).stream()
                    .sorted(java.util.Comparator.comparingDouble(Entity::getY)).anyMatch(s->{
                        // Freeze only the disposable QA actor, so it cannot walk
                        // behind a post between camera placement and capture.
                        if(!inspect(c,s.position().add(0,1,0)))return false;
                        s.setNoAi(true);return true;
                    });
            });
            if(ready==890)requested="tower-captive";
            if(ready==920){
                System.out.println("WITCH193_COMPLETE captures="+captured.size()+" volumeFrames="+volumeFrames+" tower="+towerPlaced+" roof="+roof+" names="+captured);
                stage=3;c.stop();
            }
        }
    }
    private static ServerLevel world(Minecraft c){return c.getSingleplayerServer().overworld();}
    private static boolean inspect(Minecraft c,Vec3 aim){
        Vec3 best=null;double score=-1;
        for(double radius:new double[]{3.2,2,1.6,1.2,.8})for(int i=0;i<16;i++){
            double a=i*Math.PI/8;var eye=aim.add(Math.sin(a)*radius,.45,Math.cos(a)*radius);
            if(!world(c).getBlockState(BlockPos.containing(eye)).isAir())continue;
            if(!world(c).noCollision(new AABB(eye.add(-.2,-.2,-.2),eye.add(.2,.2,.2))))continue;
            var hit=world(c).clip(new ClipContext(eye,aim,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player(c)));
            if(hit.getType()!=HitResult.Type.MISS)continue;
            if(radius>score){score=radius;best=eye;}
        }
        if(best==null)return false;
        // Small prison cells require a wider real camera lens; otherwise the
        // unobstructed close view contains only the captive's chest.
        int fov=(int)net.minecraft.util.Mth.clamp(Math.toDegrees(2*Math.atan(1.35/score)),45,105);
        c.execute(()->c.options.fov().set(fov));
        System.out.println("WITCH193_INSPECT aim="+aim+" radius="+score+" fov="+fov);
        camera(c,best,aim);return true;
    }
    private static ServerPlayer player(Minecraft c){return c.getSingleplayerServer().getPlayerList().getPlayers().getFirst();}
    private static void camera(Minecraft c,Vec3 at,Vec3 toward){
        var d=toward.subtract(at);float yaw=(float)Math.toDegrees(Math.atan2(d.z,d.x))-90,pitch=(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance()));
        player(c).teleportTo(world(c),at.x,at.y-player(c).getEyeHeight(),at.z,yaw,pitch);
    }
    private static void model(Minecraft c,int s){
        for(var e:com.google.common.collect.ImmutableList.copyOf(world(c).getAllEntities()))if(e instanceof WitchActor||e instanceof WitchBolt||e instanceof AllySkeleton||e.getType()==EntityType.IRON_GOLEM)e.discard();
        Mob w=CARDS?(s<4?RoyaleSpells.WITCH:RoyaleSpells.EVOLVED_WITCH).create(world(c)):WitchContent.WITCH.create(world(c));w.moveTo(0,80,0,180,0);w.setYBodyRot(180);w.setYHeadRot(180);w.setNoAi(true);
        CompoundTag n=new CompoundTag();w.saveWithoutId(n);n.putBoolean("WitchEvolved",s>=4);n.putInt("Deploy",0);w.load(n);world(c).addFreshEntity(w);witchId=w.getUUID();
        Vec3[] views={new Vec3(0,81.5,-4.4),new Vec3(3,81.9,-4),new Vec3(4.5,81.5,0),new Vec3(0,81.6,4.4)};camera(c,views[s%4],new Vec3(0,81,0));
    }
    private static void battle(Minecraft c,int s){
        model(c,4);
        var w=(Mob)world(c).getEntity(witchId);var enemy=EntityType.IRON_GOLEM.create(world(c));enemy.setPos(0,80,-4.4);enemy.setNoAi(true);enemy.setInvulnerable(true);world(c).addFreshEntity(enemy);w.setTarget(enemy);if(CARDS&&s==10)((WitchActor)w).summonWave();
        Vec3[] views={new Vec3(4,82,-2),new Vec3(-4,83,-2),new Vec3(0,85,-2)};camera(c,views[s-8],new Vec3(0,81,-2));
    }
    private WitchWorldSmoke(){}
}
